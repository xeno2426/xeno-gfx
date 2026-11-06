package com.xeno.gfx.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMetricsTest {

    @Test
    fun `test default metrics state`() {
        val metrics = DeviceMetrics()
        assertEquals(0L, metrics.ramUsedMb)
        assertEquals(0L, metrics.ramTotalMb)
        assertEquals(0, metrics.ramPercent)
        assertEquals(30, metrics.calibratedFps)
        assertEquals(60, metrics.rawRefreshRate)
        assertNull(metrics.latencyMs)
        assertEquals(DeviceMetrics.ThermalStatus.NORMAL, metrics.thermalStatus)

        // Backward compatibility assertions
        assertEquals(30, metrics.currentFps)
        assertEquals(60, metrics.refreshRateHz)
        assertNull(metrics.networkLatencyMs)
    }

    @Test
    fun `test semantic calibration fields map cleanly to legacy accessors`() {
        val metrics = DeviceMetrics(
            calibratedFps = 28,
            rawRefreshRate = 90,
            latencyMs = 45L
        )
        assertEquals(28, metrics.calibratedFps)
        assertEquals(28, metrics.currentFps)
        assertEquals(90, metrics.rawRefreshRate)
        assertEquals(90, metrics.refreshRateHz)
        assertEquals(45L, metrics.latencyMs)
        assertEquals(45L, metrics.networkLatencyMs)
    }

    @Test
    fun `test ram percentage bounds`() {
        val totalMb = 8192L
        val usedMb = 6144L
        val percent = ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt()
        assertEquals(75, percent)
        assertTrue(percent in 0..100)
    }

    @Test
    fun `test battery temperature formatting`() {
        val rawTempFromIntent = 385 // 38.5 C in tenths
        val tempCelsius = rawTempFromIntent / 10.0f
        assertEquals(38.5f, tempCelsius, 0.01f)
    }
}
