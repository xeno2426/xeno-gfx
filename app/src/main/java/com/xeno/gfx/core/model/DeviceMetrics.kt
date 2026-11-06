package com.xeno.gfx.core.model

/**
 * Snapshot of real-time device hardware and network performance metrics.
 *
 * D-03 & D-06: Distinguishes calibrated gaming FPS (0..30 target) from physical
 * display compositor refresh rate (e.g. 60Hz, 90Hz, 120Hz) while maintaining
 * backward-compatible accessors for legacy callers.
 */
data class DeviceMetrics(
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val ramPercent: Int = 0,
    val batteryLevel: Int = -1,
    val batteryTempCelsius: Float = 0.0f,
    val isCharging: Boolean = false,
    val thermalStatus: ThermalStatus = ThermalStatus.NORMAL,
    val calibratedFps: Int = 30,
    val rawRefreshRate: Int = 60,
    val latencyMs: Long? = null,
    val currentFps: Int = calibratedFps,
    val refreshRateHz: Int = rawRefreshRate,
    val networkLatencyMs: Long? = latencyMs
) {
    enum class ThermalStatus {
        NORMAL,
        LIGHT,
        MODERATE,
        SEVERE,
        CRITICAL,
        EMERGENCY,
        SHUTDOWN,
        UNKNOWN
    }
}
