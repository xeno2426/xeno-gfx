package com.xeno.gfx.core.stats

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Calibrated frame rate calculation engine measuring compositor VSYNC delivery loss
 * anchored to an honest 30 FPS target ceiling (33.33ms VSYNC cadence baseline).
 *
 * Implements:
 * - D-01: 33.33ms target frame interval, dropped VSYNC detection (delta > 1.5 * vsync), 0..30 clamp.
 * - D-02: Exponential Moving Average (EMA, alpha = 0.2) smoothing over 1000ms calculation windows.
 * - D-03: Distinct reporting of calibrated gaming FPS and physical panel refresh rate.
 * - T-01-03: Zero-allocation primitive operations on the Main Looper to prevent ART GC pauses.
 */
class FpsSampler(
    private var maxRefreshRate: Int = 60,
    private val onMetricsCalculated: (fps: Int, refreshRate: Int) -> Unit
) : Choreographer.FrameCallback {

    constructor(
        maxRefreshRate: Int = 60,
        onFpsCalculated: (Int) -> Unit
    ) : this(
        maxRefreshRate = maxRefreshRate,
        onMetricsCalculated = { fps, _ -> onFpsCalculated(fps) }
    )

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var choreographer: Choreographer? = null
    private var isRunning = false

    // Timing and cadence tracking primitives (zero-allocation)
    private var lastFrameTimeNanos = 0L
    private var lastIntervalTimeNanos = 0L
    private var accumulatedDroppedPulses = 0
    private var smoothedFps = -1.0

    // Calibrated properties
    val latestCalibratedFps: Int
        get() = if (smoothedFps >= 0.0) smoothedFps.roundToInt().coerceIn(0, 30) else 30

    fun setMaxRefreshRate(rate: Int) {
        maxRefreshRate = rate.coerceAtLeast(30)
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        resetState()

        if (Looper.myLooper() == Looper.getMainLooper()) {
            initChoreographer()
        } else {
            mainHandler.post { initChoreographer() }
        }
    }

    private fun initChoreographer() {
        choreographer = Choreographer.getInstance()
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
        resetState()
    }

    private fun resetState() {
        lastFrameTimeNanos = 0L
        lastIntervalTimeNanos = 0L
        accumulatedDroppedPulses = 0
        smoothedFps = -1.0
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        processFrame(frameTimeNanos)

        if (isRunning) {
            choreographer?.postFrameCallback(this)
        }
    }

    /**
     * Core zero-allocation mathematical processing loop for each VSYNC frame timestamp.
     * Exposed for deterministic unit testing without Android Looper / Choreographer dependencies.
     */
    fun processFrame(frameTimeNanos: Long) {
        if (lastFrameTimeNanos == 0L || lastIntervalTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
            lastIntervalTimeNanos = frameTimeNanos
            return
        }

        val deltaNanos = frameTimeNanos - lastFrameTimeNanos
        lastFrameTimeNanos = frameTimeNanos

        val vsyncIntervalNanos = 1_000_000_000L / maxRefreshRate
        val droppedThresholdNanos = (vsyncIntervalNanos * 1.5).toLong()

        if (deltaNanos > droppedThresholdNanos) {
            val pulsesElapsed = ((deltaNanos + (vsyncIntervalNanos * 0.5)).toLong() / vsyncIntervalNanos).toInt()
            val dropped = max(0, pulsesElapsed - 1)
            accumulatedDroppedPulses += dropped
        }

        val windowElapsedNanos = frameTimeNanos - lastIntervalTimeNanos
        if (windowElapsedNanos >= 1_000_000_000L) {
            val deliveryRatio = max(0.0, 1.0 - (accumulatedDroppedPulses.toDouble() / maxRefreshRate.toDouble()))
            val instantFps = 30.0 * deliveryRatio

            smoothedFps = if (smoothedFps < 0.0) {
                instantFps
            } else {
                (0.2 * instantFps) + (0.8 * smoothedFps)
            }

            val finalFps = smoothedFps.roundToInt().coerceIn(0, 30)
            onMetricsCalculated(finalFps, maxRefreshRate)

            accumulatedDroppedPulses = 0
            lastIntervalTimeNanos = frameTimeNanos
        }
    }
}
