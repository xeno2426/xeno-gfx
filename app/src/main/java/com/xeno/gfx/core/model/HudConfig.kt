package com.xeno.gfx.core.model

/**
 * User configuration for HUD appearance and update frequency.
 */
data class HudConfig(
    val updateIntervalMs: Long = 1000L,
    val isFpsMeterEnabled: Boolean = true,
    val isRamMeterEnabled: Boolean = true,
    val isBatteryMeterEnabled: Boolean = true,
    val isTempMeterEnabled: Boolean = true,
    val isLatencyMeterEnabled: Boolean = true,
    val isCompactMode: Boolean = false,
    val opacity: Float = 0.85f
)
