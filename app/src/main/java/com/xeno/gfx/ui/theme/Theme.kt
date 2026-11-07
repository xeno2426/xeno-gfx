package com.xeno.gfx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = CanvasBackground,
    secondary = SapphireMist,
    onSecondary = CanvasBackground,
    tertiary = MutedEmerald,
    background = CanvasBackground,
    onBackground = DarkOnBackground,
    surface = TitaniumSubstrate,
    onSurface = DarkOnSurface,
    surfaceVariant = SmokedGlassSurface,
    onSurfaceVariant = TextVariant,
    error = MutedCrimson
)

@Composable
fun XenoGfxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
