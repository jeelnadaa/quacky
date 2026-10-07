package app.quacky.feature.arruler.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import app.quacky.feature.arruler.engine.ArSessionConfigurator
import app.quacky.feature.arruler.engine.ConfidenceReason
import app.quacky.feature.arruler.engine.ConfidenceScorer
import app.quacky.feature.arruler.engine.DepthSampler
import app.quacky.feature.arruler.engine.EdgeSnapper
import app.quacky.feature.arruler.engine.LiveFrameState
import app.quacky.feature.arruler.engine.LiveMeasurementController
import app.quacky.feature.arruler.engine.MeasureTuning
import app.quacky.feature.arruler.engine.MeasurementMode
import app.quacky.feature.arruler.engine.PlaneIntersectionSnapper
import app.quacky.feature.arruler.engine.PointConfidence
import app.quacky.feature.arruler.engine.PointStabilizer
import app.quacky.feature.arruler.engine.ReferenceItem
import app.quacky.feature.arruler.engine.RulerUnit
import app.quacky.feature.arruler.engine.ScaleCheckResult
import app.quacky.feature.arruler.engine.ScaleChecker
import app.quacky.feature.arruler.engine.SurfaceHit
import app.quacky.feature.arruler.engine.SurfaceResolver
import app.quacky.feature.arruler.engine.TrackingFailureReasonEnum
import app.quacky.feature.arruler.engine.TrackingMonitor
import app.quacky.feature.arruler.engine.Vec3

@SuppressLint("ViewConstructor")
class ArSurfaceView(
    context: Context,
    private val onFrameStateUpdated: (
        state: LiveFrameState,
        latestHit: SurfaceHit?,
        pointConfidence: PointConfidence?,
        scaleFactor: Double
    ) -> Unit
) : GLSurfaceView(context) {

    private val renderer: ArRenderer
    private var session: Session? = null

    // Engine units
    private val trackingMonitor = TrackingMonitor()
    private val depthSampler = DepthSampler()
    private val surfaceResolver = SurfaceResolver()
    private val planeSnapper = PlaneIntersectionSnapper()
    private val edgeSnapper = EdgeSnapper()
    private val pointStabilizer = PointStabilizer()
    private val confidenceScorer = ConfidenceScorer()
    private val scaleChecker = ScaleChecker()
    val liveController = LiveMeasurementController()

    // Mode and settings
    var currentMode: MeasurementMode = MeasurementMode.DISTANCE
    var currentUnit: RulerUnit = RulerUnit.CENTIMETERS
    var isSnappingEnabled: Boolean = true

    // Placed anchors
    private val placedAnchors = mutableListOf<Anchor>()
    private val anchorConfidences = mutableListOf<Float>()
    private val anchorReasons = mutableListOf<Set<ConfidenceReason>>()

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val cameraRayDir = FloatArray(3)

    private var latestHit: SurfaceHit? = null
    private var latestConfidence: PointConfidence? = null

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true

        renderer = ArRenderer { arSession, frame, width, height ->
            processFrame(arSession, frame, width.toFloat(), height.toFloat())
        }

        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun initSession(): Boolean {
        if (session != null) return true
        return try {
            val s = Session(context)
            ArSessionConfigurator.configureSession(s)
            session = s
            renderer.session = s
            true
        } catch (_: Exception) {
            false
        }
    }

    fun resumeSession() {
        try {
            session?.resume()
            onResume()
        } catch (_: Exception) {}
    }

    fun pauseSession() {
        try {
            onPause()
            session?.pause()
        } catch (_: Exception) {}
    }

    fun destroySession() {
        try {
            session?.close()
            session = null
            renderer.session = null
            trackingMonitor.reset()
            pointStabilizer.reset()
            liveController.reset()
            scaleChecker.reset()
            placedAnchors.forEach { it.detach() }
            placedAnchors.clear()
            anchorConfidences.clear()
            anchorReasons.clear()
        } catch (_: Exception) {}
    }

    private fun processFrame(arSession: Session, frame: Frame, width: Float, height: Float) {
        val camera = frame.camera
        val timestampMs = System.currentTimeMillis()
        val isTracking = camera.trackingState == TrackingState.TRACKING

        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)

        val camPose = camera.pose
        val camPos = Vec3(camPose.tx().toDouble(), camPose.ty().toDouble(), camPose.tz().toDouble())

        // Camera look direction from pose rotation: sensor -Z axis
        val dir = camPose.zAxis // Z axis
        val rayDir = Vec3(-dir[0].toDouble(), -dir[1].toDouble(), -dir[2].toDouble()).normalized()

        val failureReason = when (camera.trackingFailureReason) {
            TrackingFailureReason.BAD_STATE -> TrackingFailureReasonEnum.BAD_STATE
            TrackingFailureReason.INSUFFICIENT_LIGHT -> TrackingFailureReasonEnum.INSUFFICIENT_LIGHT
            TrackingFailureReason.EXCESSIVE_MOTION -> TrackingFailureReasonEnum.EXCESSIVE_MOTION
            TrackingFailureReason.INSUFFICIENT_FEATURES -> TrackingFailureReasonEnum.INSUFFICIENT_FEATURES
            TrackingFailureReason.CAMERA_UNAVAILABLE -> TrackingFailureReasonEnum.CAMERA_UNAVAILABLE
            else -> TrackingFailureReasonEnum.NONE
        }

        // Synchronize anchor positions into live controller
        if (placedAnchors.isNotEmpty()) {
            val updatedAnchorPoints = mutableListOf<Vec3>()
            for (anchor in placedAnchors) {
                val p = anchor.pose
                updatedAnchorPoints.add(Vec3(p.tx().toDouble(), p.ty().toDouble(), p.tz().toDouble()))
            }
            // Update points in controller
            liveController.reset()
            updatedAnchorPoints.forEach { liveController.addPoint(it) }
        }

        // 1. Depth Sampling
        val viewCenterX = width / 2f
        val viewCenterY = height / 2f
        val depthResult = depthSampler.sampleWindow(
            frame = frame,
            camera = camera,
            viewX = viewCenterX,
            viewY = viewCenterY,
            estimatedDistanceMeters = latestHit?.distanceFromCamera
        )

        // 2. Surface Resolver
        val arCoreHits = try { frame.hitTest(viewCenterX, viewCenterY) } catch (_: Exception) { emptyList() }
        val resolvedHit = surfaceResolver.resolve(
            cameraPosition = camPos,
            rayDirection = rayDir,
            depthSamples = depthResult.samples,
            isDepthReady = depthResult.isDepthReady,
            depthMeanConfidence = depthResult.meanConfidence,
            arCoreHits = arCoreHits
        )

        latestHit = resolvedHit

        // 3. Snapping
        var snappedPoint: Vec3? = null
        var snapLabel: String? = null
        if (isSnappingEnabled && resolvedHit != null) {
            val planes = arSession.getAllTrackables(Plane::class.java).filter { it.trackingState == TrackingState.TRACKING }
            val planeSnap = planeSnapper.checkSnap(resolvedHit.worldPoint, camPos, rayDir, planes)
            if (planeSnap != null) {
                snappedPoint = planeSnap.snappedPoint
                snapLabel = planeSnap.label
            } else {
                val edgeSnap = edgeSnapper.checkEdgeSnap(resolvedHit, depthResult.samples, camPos)
                if (edgeSnap.isSnapped) {
                    snappedPoint = edgeSnap.snappedPoint
                    snapLabel = "Edge"
                }
            }
        }

        // 4. Point Stabilization
        val effectiveHit = if (snappedPoint != null && resolvedHit != null) {
            resolvedHit.copy(worldPoint = snappedPoint)
        } else resolvedHit

        val reticleState = pointStabilizer.update(effectiveHit, timestampMs)
        val stabilizedResult = pointStabilizer.getStabilizedPoint()

        // 5. Tracking Monitor
        val trackingStatus = trackingMonitor.update(
            timestampNs = frame.timestamp,
            isTracking = isTracking,
            failureReason = failureReason,
            tx = camPose.tx(),
            ty = camPose.ty(),
            tz = camPose.tz(),
            qx = camPose.qx(),
            qy = camPose.qy(),
            qz = camPose.qz(),
            qw = camPose.qw(),
            isDepthReady = depthResult.isDepthReady,
            targetDistanceMeters = resolvedHit?.distanceFromCamera
        )

        // 6. Confidence Scoring
        if (resolvedHit != null) {
            latestConfidence = confidenceScorer.scorePoint(
                isTracking = isTracking,
                depthQuality = depthResult.meanConfidence,
                fitQuality = if (resolvedHit.source == app.quacky.feature.arruler.engine.HitSource.FUSED) 0.9f else 0.7f,
                distanceMeters = resolvedHit.distanceFromCamera,
                jitterMeters = stabilizedResult.jitterMeters,
                hasSourceAgreement = resolvedHit.source == app.quacky.feature.arruler.engine.HitSource.FUSED,
                hasDisagreement = resolvedHit.reasons.contains(ConfidenceReason.SURFACES_DISAGREED),
                parallaxRatio = trackingStatus.parallaxProgress
            )
        } else {
            latestConfidence = null
        }

        // 7. Live Measurement Frame State
        val frameState = liveController.updateFrame(
            mode = currentMode,
            unit = currentUnit,
            scaleFactor = scaleChecker.currentScaleFactor,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            cameraPosition = camPos,
            screenWidth = width,
            screenHeight = height,
            currentHit = effectiveHit,
            reticleState = reticleState,
            trackingStatus = trackingStatus,
            snappedPoint = snappedPoint,
            snapLabel = snapLabel,
            timestampMs = timestampMs
        )

        post {
            onFrameStateUpdated(
                frameState,
                resolvedHit,
                latestConfidence,
                scaleChecker.currentScaleFactor
            )
        }
    }

    /**
     * Places a point at the current stabilized aim point and creates an ARCore Anchor.
     */
    fun placePoint(): Boolean {
        val s = session ?: return false
        val stab = pointStabilizer.getStabilizedPoint()
        val hit = latestHit

        val targetPoint = if (stab.isLocked || stab.sampleCount >= 3) {
            stab.point
        } else {
            hit?.worldPoint ?: return false
        }

        val normal = hit?.normal ?: Vec3.UP
        // Normal-aligned rotation for anchor
        val pose = Pose.makeTranslation(
            targetPoint.x.toFloat(),
            targetPoint.y.toFloat(),
            targetPoint.z.toFloat()
        )

        return try {
            val anchor = s.createAnchor(pose)
            placedAnchors.add(anchor)
            liveController.addPoint(targetPoint)
            latestConfidence?.let { anchorConfidences.add(it.score) }
            latestHit?.let { anchorReasons.add(it.reasons) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun undoPoint(): Boolean {
        if (placedAnchors.isNotEmpty()) {
            val last = placedAnchors.removeAt(placedAnchors.size - 1)
            last.detach()
            if (anchorConfidences.isNotEmpty()) anchorConfidences.removeAt(anchorConfidences.size - 1)
            if (anchorReasons.isNotEmpty()) anchorReasons.removeAt(anchorReasons.size - 1)
            return liveController.removeLastPoint()
        }
        return false
    }

    fun clearPoints() {
        placedAnchors.forEach { it.detach() }
        placedAnchors.clear()
        anchorConfidences.clear()
        anchorReasons.clear()
        liveController.reset()
        pointStabilizer.reset()
    }

    fun calibrateScale(item: ReferenceItem, customLengthMeters: Double = 0.0): ScaleCheckResult {
        val refLength = if (item == ReferenceItem.CUSTOM) customLengthMeters else item.lengthMeters
        val measured = liveController.currentPoints.let { pts ->
            if (pts.size >= 2) (pts[1] - pts[0]).length() else 0.0
        }
        return scaleChecker.calibrate(refLength, measured)
    }

    fun capturePhoto(onCaptured: (Bitmap) -> Unit) {
        renderer.captureNextFrame { bitmap ->
            post {
                onCaptured(bitmap)
            }
        }
    }
}
