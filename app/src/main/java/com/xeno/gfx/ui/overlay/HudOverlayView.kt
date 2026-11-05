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
import com.xeno.gfx.ui.theme.AmberWarning
import com.xeno.gfx.ui.theme.CyberCyan
import com.xeno.gfx.ui.theme.HudBorderColor
import com.xeno.gfx.ui.theme.HudCardBackground
import com.xeno.gfx.ui.theme.HudPillBackground
import com.xeno.gfx.ui.theme.NeonGreen
import com.xeno.gfx.ui.theme.NeonRed
import com.xeno.gfx.ui.theme.TextMuted

@Composable
fun HudOverlayView(
    metrics: DeviceMetrics,
    config: HudConfig,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val fpsColor = when {
        metrics.currentFps >= 55 -> CyberCyan
        metrics.currentFps >= 40 -> NeonGreen
        metrics.currentFps >= 25 -> AmberWarning
        else -> NeonRed
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
                        .background(CyberCyan)
                )

                // Compact summary metrics: Live FPS
                Text(
                    text = "${metrics.currentFps} FPS",
                    color = fpsColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${metrics.ramPercent}%",
                    color = if (metrics.ramPercent > 80) AmberWarning else NeonGreen,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                if (metrics.batteryTempCelsius > 0) {
                    Text(
                        text = "%.1f°C".format(metrics.batteryTempCelsius),
                        color = if (metrics.batteryTempCelsius > 42) NeonRed else TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (metrics.networkLatencyMs != null) {
                    val latencyColor = when {
                        metrics.networkLatencyMs < 60 -> NeonGreen
                        metrics.networkLatencyMs < 120 -> AmberWarning
                        else -> NeonRed
                    }
                    Text(
                        text = "${metrics.networkLatencyMs}ms",
                        color = latencyColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Expand/Collapse Toggle
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand HUD",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { isExpanded = !isExpanded }
                )

                // Close button
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close HUD",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCloseClick() }
                )
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
                        valueColor = if (metrics.ramPercent > 85) NeonRed else NeonGreen
                    )

                    MetricRow(
                        label = "Battery",
                        value = "${metrics.batteryLevel}%" + if (metrics.isCharging) " (⚡)" else "",
                        valueColor = if (metrics.batteryLevel < 20) AmberWarning else CyberCyan
                    )

                    MetricRow(
                        label = "Battery Temp",
                        value = "%.1f °C".format(metrics.batteryTempCelsius),
                        valueColor = when {
                            metrics.batteryTempCelsius > 42 -> NeonRed
                            metrics.batteryTempCelsius > 38 -> AmberWarning
                            else -> NeonGreen
                        }
                    )

                    MetricRow(
                        label = "Thermal Status",
                        value = metrics.thermalStatus.name,
                        valueColor = when (metrics.thermalStatus) {
                            DeviceMetrics.ThermalStatus.NORMAL -> NeonGreen
                            DeviceMetrics.ThermalStatus.LIGHT,
                            DeviceMetrics.ThermalStatus.MODERATE -> AmberWarning
                            else -> NeonRed
                        }
                    )

                    MetricRow(
                        label = "Live FPS",
                        value = "${metrics.currentFps} FPS",
                        valueColor = fpsColor
                    )

                    MetricRow(
                        label = "Refresh Rate",
                        value = "${metrics.refreshRateHz} Hz",
                        valueColor = TextMuted
                    )

                    MetricRow(
                        label = "Ping (8.8.8.8)",
                        value = metrics.networkLatencyMs?.let { "${it} ms" } ?: "Offline / Timeout",
                        valueColor = when {
                            metrics.networkLatencyMs == null -> TextMuted
                            metrics.networkLatencyMs < 60 -> NeonGreen
                            metrics.networkLatencyMs < 120 -> AmberWarning
                            else -> NeonRed
                        }
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
