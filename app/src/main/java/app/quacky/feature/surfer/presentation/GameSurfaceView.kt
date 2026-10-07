package app.quacky.feature.surfer.presentation

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.Choreographer
import com.google.android.filament.android.UiHelper
import kotlin.math.abs

/**
 * SurfaceView hosting Google Filament 3D engine for Quacky Surfer.
 * Handles surface lifecycle, Choreographer 60 FPS frame callback,
 * and high-responsiveness touch swipe gestures (36 dp threshold or 900 dp/s velocity).
 */
class GameSurfaceView(
    context: Context,
    val renderer3d: Game3dRenderer,
    private val onSwipeLeft: () -> Unit = { renderer3d.requestMoveLeft() },
    private val onSwipeRight: () -> Unit = { renderer3d.requestMoveRight() },
    private val onSwipeUp: () -> Unit = { renderer3d.requestJump() },
    private val onSwipeDown: () -> Unit = { renderer3d.requestSlide() }
) : SurfaceView(context) {

    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                renderer3d.setSurface(surface)
            }

            override fun onDetachedFromSurface() {
                renderer3d.destroySurface()
            }

            override fun onResized(width: Int, height: Int) {
                renderer3d.setViewport(width, height)
            }
        }
    }

    private var isRendering = false
    private var lastFrameNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRendering) return
            if (lastFrameNanos != 0L) {
                val dt = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                renderer3d.renderFrame(dt)
            }
            lastFrameNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // Touch Swipe Tracking
    private var startX = 0f
    private var startY = 0f
    private var startTimeMs = 0L
    private var swipeHandled = false

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        uiHelper.attachTo(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startRendering()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRendering()
        uiHelper.detach()
    }

    fun startRendering() {
        if (!isRendering) {
            isRendering = true
            lastFrameNanos = 0L
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stopRendering() {
        if (isRendering) {
            isRendering = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                startTimeMs = SystemClock.uptimeMillis()
                swipeHandled = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!swipeHandled) {
                    val dx = event.x - startX
                    val dy = event.y - startY
                    val dtSeconds = (SystemClock.uptimeMillis() - startTimeMs) / 1000f

                    val density = resources.displayMetrics.density
                    val thresholdPx = 36f * density
                    val velThresholdPx = 900f * density

                    val vx = if (dtSeconds > 0) abs(dx) / dtSeconds else 0f
                    val vy = if (dtSeconds > 0) abs(dy) / dtSeconds else 0f

                    if (abs(dx) >= thresholdPx || abs(dy) >= thresholdPx || vx >= velThresholdPx || vy >= velThresholdPx) {
                        if (abs(dx) > abs(dy)) {
                            if (dx > 0) onSwipeRight() else onSwipeLeft()
                        } else {
                            if (dy < 0) onSwipeUp() else onSwipeDown()
                        }
                        swipeHandled = true
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                swipeHandled = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                onSwipeLeft()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                onSwipeRight()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_SPACE -> {
                onSwipeUp()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                onSwipeDown()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
