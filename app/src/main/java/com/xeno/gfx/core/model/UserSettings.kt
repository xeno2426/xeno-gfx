package com.xeno.gfx.core.model

/**
 * Immutable snapshot representation of persisted user settings and HUD configurations.
 *
 * @property isContinuousSuppressionEnabled Continuous background RAM process suppression toggle.
 * @property isAutoHudEnabled Automatic floating HUD launch on detected game resume.
 * @property hudCoordinates Persisted (x, y) display coordinates for floating HUD window.
 * @property trackedGamePackages Set of package names monitored for automatic HUD overlay triggers.
 * @property userCustomWhitelist Set of user-protected package names excluded from RAM kill routines.
 * @property isCompactHudMode Minimalist stealth FPS badge display mode toggle.
 */
data class UserSettings(
    val isContinuousSuppressionEnabled: Boolean = true,
    val isAutoHudEnabled: Boolean = false,
    val hudCoordinates: Pair<Int, Int> = Pair(20, 100),
    val trackedGamePackages: Set<String> = emptySet(),
    val userCustomWhitelist: Set<String> = emptySet(),
    val isCompactHudMode: Boolean = false
)
