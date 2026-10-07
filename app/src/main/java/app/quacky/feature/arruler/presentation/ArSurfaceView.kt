package app.quacky.feature.arruler.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import app.quacky.feature.arruler.domain.Vector3

@SuppressLint("ViewConstructor")
class ArSurfaceView(
    context: Context,
    private val onFrameUpdated: (
        session: Session,
        frame: Frame,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        hasSurface: Boolean,
        reticleHit: Vector3?,
        featurePoints: List<Vector3>,
        width: Int,
        height: Int
    ) -> Unit,
    private val onTap: (worldPos: Vector3, screenX: Float, screenY: Float) -> Unit
) : GLSurfaceView(context) {

    private val renderer: ArRenderer
    private var session: Session? = null
    private var latestFrame: Frame? = null

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true

        renderer = ArRenderer { arSession, frame, width, height ->
            latestFrame = frame
            val camera = frame.camera

            if (camera.trackingState == TrackingState.TRACKING) {
                camera.getViewMatrix(viewMatrix, 0)
                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)

                val planes = arSession.getAllTrackables(Plane::class.java)
                val hasTrackingPlane = planes.any { it.trackingState == TrackingState.TRACKING }

                // Multi-tier Center hit test for reticle
                val reticleHit = queryBestHit(frame, width / 2f, height / 2f)
                val hasSurface = hasTrackingPlane || (reticleHit != null)

                // Extract point cloud feature points for visible surface tracking dots
                val featureList = mutableListOf<Vector3>()
                try {
                    val pointCloud = frame.acquirePointCloud()
                    val pointsBuffer = pointCloud.points
                    val totalPoints = pointsBuffer.remaining() / 4
                    val sampleStep = (totalPoints / 25).coerceAtLeast(1)
                    val maxPoints = 25
                    var taken = 0
                    var i = 0
                    while (i < totalPoints && taken < maxPoints) {
                        val px = pointsBuffer.get(i * 4)
                        val py = pointsBuffer.get(i * 4 + 1)
                        val pz = pointsBuffer.get(i * 4 + 2)
                        val conf = pointsBuffer.get(i * 4 + 3)
                        if (conf > 0.35f) {
                            featureList.add(Vector3(px, py, pz))
                            taken++
                        }
                        i += sampleStep
                    }
                    pointCloud.close()
                } catch (_: Exception) {}

                onFrameUpdated(
                    arSession,
                    frame,
                    viewMatrix,
                    projMatrix,
                    hasSurface,
                    reticleHit,
                    featureList,
                    width,
                    height
                )
            } else {
                onFrameUpdated(
                    arSession,
                    frame,
                    viewMatrix,
                    projMatrix,
                    false,
                    null,
                    emptyList(),
                    width,
                    height
                )
            }
        }

        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun initSession(): Boolean {
        if (session != null) return true
        return try {
            val s = Session(context)
            val config = Config(s).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                if (s.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    depthMode = Config.DepthMode.AUTOMATIC
                }
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            }
            s.configure(config)
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
        } catch (_: Exception) {}
    }

    fun capturePhoto(onCaptured: (Bitmap) -> Unit) {
        renderer.captureNextFrame { bitmap ->
            post {
                onCaptured(bitmap)
            }
        }
    }

    private fun queryBestHit(frame: Frame, x: Float, y: Float): Vector3? {
        return try {
            val hits = frame.hitTest(x, y)

            // Tier 1: Inside exact plane polygon
            hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is Plane && trackable.trackingState == TrackingState.TRACKING && trackable.isPoseInPolygon(hit.hitPose)
            }?.hitPose?.let { return Vector3(it.tx(), it.ty(), it.tz()) }

            // Tier 2: Inside plane extents
            hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is Plane && trackable.trackingState == TrackingState.TRACKING && trackable.isPoseInExtents(hit.hitPose)
            }?.hitPose?.let { return Vector3(it.tx(), it.ty(), it.tz()) }

            // Tier 3: Any active tracking plane
            hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is Plane && trackable.trackingState == TrackingState.TRACKING
            }?.hitPose?.let { return Vector3(it.tx(), it.ty(), it.tz()) }

            // Tier 4: DepthPoint (accurate 3D depth)
            hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is com.google.ar.core.DepthPoint && trackable.trackingState == TrackingState.TRACKING
            }?.hitPose?.let { return Vector3(it.tx(), it.ty(), it.tz()) }

            // Tier 5: Point with estimated surface normal
            hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is com.google.ar.core.Point && trackable.trackingState == TrackingState.TRACKING &&
                    trackable.orientationMode == com.google.ar.core.Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
            }?.hitPose?.let { return Vector3(it.tx(), it.ty(), it.tz()) }

            // Tier 6: Instant placement point
            try {
                val instantHits = frame.hitTestInstantPlacement(x, y, 2.0f)
                instantHits.firstOrNull { it.trackable.trackingState == TrackingState.TRACKING }
                    ?.hitPose?.let { return Vector3(it.tx(), it.ty(), it.tz()) }
            } catch (_: Exception) {}

            // Tier 7: Any tracked feature point
            hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is com.google.ar.core.Point && trackable.trackingState == TrackingState.TRACKING
            }?.hitPose?.let { Vector3(it.tx(), it.ty(), it.tz()) }
        } catch (_: Exception) {
            null
        }
    }

    fun performHitTest(x: Float, y: Float): Vector3? {
        val frame = latestFrame ?: return null
        return queryBestHit(frame, x, y)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val hit = performHitTest(event.x, event.y)
            if (hit != null) {
                onTap(hit, event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
