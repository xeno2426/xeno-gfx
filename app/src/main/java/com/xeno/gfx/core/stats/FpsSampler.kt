package com.xeno.gfx.core.stats

import android.os.Handler
import android.os.Looper
import android.view.Choreographer

/**
 * Real-time frame rate sampler measuring compositor VSYNC cadence for in-game monitoring.
 */
class FpsSampler(
    private var maxRefreshRate: Int = 60,
    private val onFpsCalculated: (Int) -> Unit
) : Choreographer.FrameCallback {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var choreographer: Choreographer? = null
    private var isRunning = false

    private var frameCount = 0
    private var lastIntervalTimeNanos = 0L

    fun setMaxRefreshRate(rate: Int) {
        maxRefreshRate = rate.coerceAtLeast(30)
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        if (Looper.myLooper() == Looper.getMainLooper()) {
            initChoreographer()
        } else {
            mainHandler.post { initChoreographer() }
        }
    }

    private fun initChoreographer() {
        choreographer = Choreographer.getInstance()
        lastIntervalTimeNanos = System.nanoTime()
        frameCount = 0
        choreographer?.postFrameCallback(this)
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false

        if (Looper.myLooper() == Looper.getMainLooper()) {
            teardownChoreographer()
        } else {
            mainHandler.post { teardownChoreographer() }
        }
    }

    private fun teardownChoreographer() {
        choreographer?.removeFrameCallback(this)
        choreographer = null
        frameCount = 0
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        frameCount++
        val elapsedNanos = frameTimeNanos - lastIntervalTimeNanos
        val elapsedMs = elapsedNanos / 1_000_000L

        if (elapsedMs >= 1000L) {
            val calculatedFps = ((frameCount.toDouble() / elapsedMs.toDouble()) * 1000.0).toInt()
                .coerceIn(1, maxRefreshRate)
            onFpsCalculated(calculatedFps)

            frameCount = 0
            lastIntervalTimeNanos = frameTimeNanos
        }

        if (isRunning) {
            choreographer?.postFrameCallback(this)
        }
    }
}
