package com.xeno.gfx.ui.dashboard

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.permission.PermissionHelper
import com.xeno.gfx.ui.theme.AmberWarning
import com.xeno.gfx.ui.theme.CyberCyan
import com.xeno.gfx.ui.theme.DarkBackground
import com.xeno.gfx.ui.theme.DarkSurface
import com.xeno.gfx.ui.theme.DarkSurfaceElevated
import com.xeno.gfx.ui.theme.HudBorderColor
import com.xeno.gfx.ui.theme.NeonGreen
import com.xeno.gfx.ui.theme.NeonRed
import com.xeno.gfx.ui.theme.TextMuted

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onToggleManualHud: () -> Unit,
    onToggleAutoHud: (Boolean) -> Unit,
    onToggleTrackedGame: (String) -> Unit,
    onManualCleanRam: () -> Unit,
    onLaunchBgmiWithTurbo: () -> Unit,
    onToggleContinuousSuppression: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeaderSection()
            }

            // Dedicated BGMI RAM Turbo Card (4GB Optimization)
            item {
                BgmiRamTurboCard(
                    uiState = uiState,
                    onCleanNow = onManualCleanRam,
                    onLaunchBgmi = onLaunchBgmiWithTurbo,
                    onToggleContinuous = onToggleContinuousSuppression
                )
            }

            // Permissions Card (if any missing)
            if (!uiState.hasOverlayPermission || !uiState.hasUsageStatsPermission) {
                item {
                    PermissionCard(
                        hasOverlay = uiState.hasOverlayPermission,
                        hasUsageStats = uiState.hasUsageStatsPermission,
                        context = context
                    )
                }
            }

            // Live Telemetry Preview Card
            item {
                TelemetryPreviewCard(metrics = uiState.previewMetrics)
            }

            // HUD Master Control Button
            item {
                HudControlButton(
                    isHudActive = uiState.isHudActive,
                    hasOverlayPermission = uiState.hasOverlayPermission,
                    onToggle = onToggleManualHud,
                    onGrantOverlay = {
                        context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                    }
                )
            }

            // Auto-Launch Configuration Section
            item {
                AutoHudSection(
                    isAutoEnabled = uiState.isAutoHudEnabled,
                    hasUsagePermission = uiState.hasUsageStatsPermission,
                    onToggleAuto = onToggleAutoHud,
                    onGrantUsage = {
                        context.startActivity(PermissionHelper.getUsageStatsPermissionIntent(context))
                    }
                )
            }

            // Tracked Game Items
            items(uiState.installedApps, key = { it.packageName }) { game ->
                val isTracked = uiState.trackedGamePackages.contains(game.packageName)
                GameRow(
                    appName = game.appName,
                    packageName = game.packageName,
                    isTracked = isTracked,
                    onToggle = { onToggleTrackedGame(game.packageName) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BgmiRamTurboCard(
    uiState: DashboardUiState,
    onCleanNow: () -> Unit,
    onLaunchBgmi: () -> Unit,
    onToggleContinuous: (Boolean) -> Unit
) {
    val totalRam = uiState.previewMetrics.ramTotalMb
    val usedRam = uiState.previewMetrics.ramUsedMb
    val freeRam = (totalRam - usedRam).coerceAtLeast(0L)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "BGMI 4GB RAM TURBO",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Targeting 3GB RAM headroom for BGMI",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${freeRam}MB FREE",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Clean Results Feedback (if any)
            uiState.lastCleanResult?.let { result ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan.copy(alpha = 0.12f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ Purged ${result.terminatedAppsCount} background apps • Recovered +${result.freedMb}MB RAM",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Primary Launch / Clean Action Button
            Button(
                onClick = {
                    if (uiState.isBgmiInstalled) {
                        onLaunchBgmi()
                    } else {
                        onCleanNow()
                    }
                },
                enabled = !uiState.isCleaningInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isBgmiInstalled) Icons.Default.RocketLaunch else Icons.Default.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isCleaningInProgress) {
                        "PURGING BACKGROUND APPS..."
                    } else if (uiState.isBgmiInstalled) {
                        "PURGE RAM & LAUNCH BGMI"
                    } else {
                        "PURGE BACKGROUND APPS NOW"
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // In-Game Background Killer Switch (every 30s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "In-Game Background Suppressor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Auto-kills apps every 30s while in gunfights to eliminate stutter",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = uiState.isContinuousSuppressionEnabled,
                    onCheckedChange = { onToggleContinuous(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, CyberCyan, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        Column {
            Text(
                text = "XENO GFX",
                style = MaterialTheme.typography.titleLarge,
                color = CyberCyan,
                letterSpacing = 2.sp
            )
            Text(
                text = "Pro Gaming Performance Monitor",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PermissionCard(
    hasOverlay: Boolean,
    hasUsageStats: Boolean,
    context: Context
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Required Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberWarning
                )
            }

            if (!hasOverlay) {
                PermissionRow(
                    title = "Display Over Other Apps",
                    desc = "Required to render the floating HUD overlay while gaming.",
                    onGrant = {
                        context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                    }
                )
            }

            if (!hasUsageStats) {
                PermissionRow(
                    title = "Usage Access",
                    desc = "Required to detect when games are launched and automatically display the HUD.",
                    onGrant = {
                        context.startActivity(PermissionHelper.getUsageStatsPermissionIntent(context))
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    desc: String,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onGrant,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
        ) {
            Text("Grant", fontSize = 12.sp)
        }
    }
}

@Composable
private fun TelemetryPreviewCard(metrics: DeviceMetrics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HudBorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "DEVICE TELEMETRY",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    label = "30 FPS TARGET",
                    value = "${metrics.calibratedFps} FPS",
                    color = when {
                        metrics.calibratedFps >= 28 -> CyberCyan
                        metrics.calibratedFps >= 24 -> NeonGreen
                        metrics.calibratedFps >= 18 -> AmberWarning
                        else -> NeonRed
                    }
                )
                MetricItem(
                    label = "RAM USAGE",
                    value = "${metrics.ramPercent}%",
                    color = if (metrics.ramPercent > 80) AmberWarning else NeonGreen
                )
                MetricItem(
                    label = "BATTERY TEMP",
                    value = if (metrics.batteryTempCelsius > 0) "%.1f°C".format(metrics.batteryTempCelsius) else "--",
                    color = when {
                        metrics.batteryTempCelsius > 42 -> NeonRed
                        metrics.batteryTempCelsius > 38 -> AmberWarning
                        else -> NeonGreen
                    }
                )
                MetricItem(
                    label = "PING (DNS)",
                    value = metrics.latencyMs?.let { "${it}ms" } ?: "-- ms",
                    color = when {
                        metrics.latencyMs == null -> TextMuted
                        metrics.latencyMs < 60 -> NeonGreen
                        metrics.latencyMs < 120 -> AmberWarning
                        else -> NeonRed
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
private fun HudControlButton(
    isHudActive: Boolean,
    hasOverlayPermission: Boolean,
    onToggle: () -> Unit,
    onGrantOverlay: () -> Unit
) {
    Button(
        onClick = {
            if (hasOverlayPermission) {
                onToggle()
            } else {
                onGrantOverlay()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHudActive) NeonRed else CyberCyan,
            contentColor = DarkBackground
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(
            imageVector = if (isHudActive) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (!hasOverlayPermission) {
                "GRANT OVERLAY PERMISSION"
            } else if (isHudActive) {
                "STOP FLOATING HUD"
            } else {
                "LAUNCH FLOATING HUD"
            },
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun AutoHudSection(
    isAutoEnabled: Boolean,
    hasUsagePermission: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    onGrantUsage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-HUD on Game Launch",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Automatically activates HUD overlay when any checked game is opened",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Switch(
                checked = isAutoEnabled,
                onCheckedChange = { checked ->
                    if (checked && !hasUsagePermission) {
                        onGrantUsage()
                    } else {
                        onToggleAuto(checked)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBackground,
                    checkedTrackColor = CyberCyan,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceElevated
                )
            )
        }

        Text(
            text = "SELECT GAMES TO TRACK",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun GameRow(
    appName: String,
    packageName: String,
    isTracked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Checkbox(
            checked = isTracked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CyberCyan,
                checkmarkColor = DarkBackground,
                uncheckedColor = TextMuted
            )
        )
    }
}
