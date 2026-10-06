package app.quacky.feature.arruler.presentation

import android.annotation.SuppressLint
import android.content.Context
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
                val hasSurface = planes.any { it.trackingState == TrackingState.TRACKING }

                // Center hit test for reticle
                val centerHits = frame.hitTest(width / 2f, height / 2f)
                val reticleHitPose = centerHits.firstOrNull { hit ->
                    val trackable = hit.trackable
                    trackable is Plane && trackable.trackingState == TrackingState.TRACKING
                }?.hitPose

                val reticleHit = reticleHitPose?.let {
                    Vector3(it.tx(), it.ty(), it.tz())
                }

                onFrameUpdated(arSession, frame, viewMatrix, projMatrix, hasSurface, reticleHit, width, height)
            } else {
                onFrameUpdated(arSession, frame, viewMatrix, projMatrix, false, null, width, height)
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

    fun performHitTest(x: Float, y: Float): Vector3? {
        val frame = latestFrame ?: return null
        return try {
            val hits = frame.hitTest(x, y)
            val hitPose = hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is Plane && trackable.trackingState == TrackingState.TRACKING
            }?.hitPose
            hitPose?.let { Vector3(it.tx(), it.ty(), it.tz()) }
        } catch (_: Exception) {
            null
        }
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
