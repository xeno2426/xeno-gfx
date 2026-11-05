package com.xeno.gfx.core.model

/**
 * Snapshot of real-time device hardware and network performance metrics.
 */
data class DeviceMetrics(
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val ramPercent: Int = 0,
    val batteryLevel: Int = -1,
    val batteryTempCelsius: Float = 0.0f,
    val isCharging: Boolean = false,
    val refreshRateHz: Int = 60,
    val currentFps: Int = 60,
    val networkLatencyMs: Long? = null,
    val thermalStatus: ThermalStatus = ThermalStatus.NORMAL
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
