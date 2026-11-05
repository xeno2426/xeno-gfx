package com.xeno.gfx.core.model

import android.graphics.drawable.Drawable

/**
 * Representation of an installed app designated as a game for auto-HUD tracking.
 */
data class TrackedGame(
    val packageName: String,
    val appName: String,
    val isAutoHudEnabled: Boolean = true,
    @Transient val icon: Drawable? = null
)
