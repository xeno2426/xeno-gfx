package com.xeno.gfx.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.model.HudConfig
import com.xeno.gfx.ui.theme.ChampagneGold
import com.xeno.gfx.ui.theme.HudBorderColor
import com.xeno.gfx.ui.theme.HudCardBackground
import com.xeno.gfx.ui.theme.HudPillBackground
import com.xeno.gfx.ui.theme.MutedCrimson
import com.xeno.gfx.ui.theme.MutedEmerald
import com.xeno.gfx.ui.theme.PlatinumIce
import com.xeno.gfx.ui.theme.SapphireMist
import com.xeno.gfx.ui.theme.SubduedAmber
import com.xeno.gfx.ui.theme.TextMuted
import com.xeno.gfx.ui.theme.TextVariant

/**
 * Collapsible floating HUD composable displaying real-time FPS, RAM, thermal status, and ping.
 *
 * Implements Stitch Titanium Telemetry Studio Design System:
 * - Monotonic 4dp/8dp grid spacing with 44dp minimum touch targets for HUD action icons.
 * - Monospace tabular digits for live numerical telemetry to eliminate frame-rate jitter.
 * - Non-neon luxury palette: MutedEmerald (>=28 FPS), ChampagneGold (24..27), SubduedAmber (18..23), MutedCrimson (<18).
 * - Frosted smoked glass substrate with hairline specular highlights.
 * - Decoupled ping display with graceful "-- ms" / "Offline / Timeout" fallback.
 */
@Composable
fun HudOverlayView(
    metrics: DeviceMetrics,
    config: HudConfig,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val fpsColor = when {
        metrics.calibratedFps >= 28 -> MutedEmerald
        metrics.calibratedFps >= 24 -> ChampagneGold
        metrics.calibratedFps >= 18 -> SubduedAmber
        else -> MutedCrimson
    }

    val latencyColor = when {
        metrics.latencyMs == null -> TextMuted
        metrics.latencyMs < 60L -> MutedEmerald
        metrics.latencyMs < 120L -> SubduedAmber
        else -> MutedCrimson
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isExpanded) HudCardBackground else HudPillBackground)
            .border(1.dp, HudBorderColor, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header / Pill Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                // Live indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MutedEmerald)
                )

                // Compact summary metrics: Calibrated 30 FPS target
                Text(
                    text = "${metrics.calibratedFps} FPS",
                    color = fpsColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${metrics.ramPercent}%",
                    color = if (metrics.ramPercent > 80) SubduedAmber else PlatinumIce,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                if (metrics.batteryTempCelsius > 0) {
                    Text(
                        text = "%.1f°C".format(metrics.batteryTempCelsius),
                        color = if (metrics.batteryTempCelsius > 42) MutedCrimson else TextVariant,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Decoupled Ping indicator
                Text(
                    text = metrics.latencyMs?.let { "${it}ms" } ?: "-- ms",
                    color = latencyColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Expand/Collapse Toggle with 44dp minimum ergonomic touch bounds
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand HUD",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close button with 44dp minimum ergonomic touch bounds
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onCloseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close HUD",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanded Detail Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                ) {
                    MetricRow(
                        label = "RAM Usage",
                        value = "${metrics.ramUsedMb}MB / ${metrics.ramTotalMb}MB (${metrics.ramPercent}%)",
                        valueColor = if (metrics.ramPercent > 85) MutedCrimson else PlatinumIce
                    )

                    MetricRow(
                        label = "Battery",
                        value = "${metrics.batteryLevel}%" + if (metrics.isCharging) " (⚡)" else "",
                        valueColor = if (metrics.batteryLevel < 20) SubduedAmber else ChampagneGold
                    )

                    MetricRow(
                        label = "Battery Temp",
                        value = "%.1f °C".format(metrics.batteryTempCelsius),
                        valueColor = when {
                            metrics.batteryTempCelsius > 42 -> MutedCrimson
                            metrics.batteryTempCelsius > 38 -> SubduedAmber
                            else -> MutedEmerald
                        }
                    )

                    MetricRow(
                        label = "Thermal Status",
                        value = metrics.thermalStatus.name,
                        valueColor = when (metrics.thermalStatus) {
                            DeviceMetrics.ThermalStatus.NORMAL -> MutedEmerald
                            DeviceMetrics.ThermalStatus.LIGHT,
                            DeviceMetrics.ThermalStatus.MODERATE -> SubduedAmber
                            else -> MutedCrimson
                        }
                    )

                    MetricRow(
                        label = "Live Gaming FPS",
                        value = "${metrics.calibratedFps} FPS (30 FPS Target)",
                        valueColor = fpsColor
                    )

                    MetricRow(
                        label = "Panel Refresh Rate",
                        value = "${metrics.rawRefreshRate} Hz (Compositor)",
                        valueColor = TextMuted
                    )

                    MetricRow(
                        label = "Ping (Google DNS)",
                        value = metrics.latencyMs?.let { "${it} ms" } ?: "Offline / Timeout",
                        valueColor = latencyColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            fontFamily = FontFamily.Default
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
