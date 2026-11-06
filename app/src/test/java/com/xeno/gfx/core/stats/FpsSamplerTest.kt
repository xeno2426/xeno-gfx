package com.xeno.gfx.core.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FpsSamplerTest {

    @Test
    fun `test undropped 60Hz vsync calculates locked 30 FPS`() {
        var reportedFps = -1
        var reportedRefreshRate = -1
        val sampler = FpsSampler(maxRefreshRate = 60) { fps, rate ->
            reportedFps = fps
            reportedRefreshRate = rate
        }

        val vsyncStepNanos = 16_666_666L // ~60 FPS interval
        var currentTimeNanos = 1_000_000_000L

        // Feed frames spanning over 1000ms
        for (i in 0..61) {
            sampler.processFrame(currentTimeNanos)
            currentTimeNanos += vsyncStepNanos
        }

        assertEquals(30, reportedFps)
        assertEquals(60, reportedRefreshRate)
        assertEquals(30, sampler.latestCalibratedFps)
    }

    @Test
    fun `test dropped pulses calculate proportional frame rate drop`() {
        var reportedFps = -1
        val sampler = FpsSampler(maxRefreshRate = 60) { fps ->
            reportedFps = fps
        }

        // Simulate 50ms intervals instead of 16.6ms intervals (2 dropped pulses per frame)
        // 20 frames * 50ms = 1000ms. In each 50ms frame, 2 pulses are dropped.
        // Total dropped pulses = 20 * 2 = 40.
        // Delivery ratio = 1 - 40/60 = 20/60 = 0.333 -> ~10 FPS.
        var currentTimeNanos = 1_000_000_000L
        val interval50msNanos = 50_000_000L

        for (i in 0..21) {
            sampler.processFrame(currentTimeNanos)
            currentTimeNanos += interval50msNanos
        }

        assertTrue("Reported FPS should reflect drop: $reportedFps", reportedFps in 9..11)
        assertTrue("Reported FPS should be clamped within 0..30", reportedFps in 0..30)
    }

    @Test
    fun `test severe frame delivery freeze clamps strictly to 0 FPS`() {
        var reportedFps = -1
        val sampler = FpsSampler(maxRefreshRate = 60) { fps ->
            reportedFps = fps
        }

        // Simulate initial frame then a huge 2-second stall
        var currentTimeNanos = 1_000_000_000L
        sampler.processFrame(currentTimeNanos)

        currentTimeNanos += 2_000_000_000L // 2 second freeze
        sampler.processFrame(currentTimeNanos)

        assertEquals(0, reportedFps)
        assertEquals(0, sampler.latestCalibratedFps)
    }

    @Test
    fun `test exponential moving average smoothing prevents erratic jitter`() {
        val calculatedHistory = mutableListOf<Int>()
        val sampler = FpsSampler(maxRefreshRate = 60) { fps ->
            calculatedHistory.add(fps)
        }

        var currentTimeNanos = 1_000_000_000L
        val normalStepNanos = 16_666_666L

        // Window 1: Solid 30 FPS
        for (i in 0..61) {
            sampler.processFrame(currentTimeNanos)
            currentTimeNanos += normalStepNanos
        }
        assertEquals(30, calculatedHistory.last())

        // Window 2: Sudden drop where instant FPS is 10
        // EMA: smoothed = (0.2 * 10) + (0.8 * 30) = 2 + 24 = 26 FPS
        val droppedStepNanos = 50_000_000L
        for (i in 0..21) {
            sampler.processFrame(currentTimeNanos)
            currentTimeNanos += droppedStepNanos
        }

        assertEquals(2, calculatedHistory.size)
        assertEquals(26, calculatedHistory[1]) // Smoothed down to 26 rather than jumping immediately to 10

        // Window 3: Sustained drop where instant FPS is still 10
        // EMA: smoothed = (0.2 * 10) + (0.8 * 26) = 2 + 20.8 = 22.8 -> 23 FPS
        for (i in 0..21) {
            sampler.processFrame(currentTimeNanos)
            currentTimeNanos += droppedStepNanos
        }

        assertEquals(3, calculatedHistory.size)
        assertEquals(23, calculatedHistory[2]) // Further smoothed down to 23
    }
}
