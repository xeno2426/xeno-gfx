package com.xeno.gfx.ui.dashboard

import android.content.Context
import android.os.Build
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.xeno.gfx.core.gfx.GfxConfig
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.permission.PermissionHelper
import com.xeno.gfx.core.stats.GameSessionStats
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
    onRequestSafPermission: () -> Unit,
    onSelectPreset: (GfxConfig) -> Unit,
    onApplyGfxOnly: () -> Unit,
    onDismissSessionSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val needsSaf = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !uiState.hasSafPermission

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

            // Session Summary Card (if session just ended)
            uiState.lastCompletedSession?.let { session ->
                item {
                    SessionSummaryCard(
                        session = session,
                        onDismiss = onDismissSessionSummary
                    )
                }
            }

            // Dedicated BGMI RAM Turbo & Launch Card
            item {
                BgmiRamTurboCard(
                    uiState = uiState,
                    onCleanNow = onManualCleanRam,
                    onLaunchBgmi = onLaunchBgmiWithTurbo,
                    onToggleContinuous = onToggleContinuousSuppression
                )
            }

            // GFX Presets & Graphic Engine Card
            item {
                GfxPresetSelectorCard(
                    selectedConfig = uiState.selectedGfxConfig,
                    availablePresets = uiState.availablePresets,
                    onSelectPreset = onSelectPreset,
                    onApplyGfx = onApplyGfxOnly,
                    hasSaf = uiState.hasSafPermission,
                    onRequestSaf = onRequestSafPermission,
                    applyResult = uiState.gfxApplyResult
                )
            }

            // Permissions Card (if any missing)
            if (!uiState.hasOverlayPermission || !uiState.hasUsageStatsPermission || needsSaf) {
                item {
                    PermissionCard(
                        hasOverlay = uiState.hasOverlayPermission,
                        hasUsageStats = uiState.hasUsageStatsPermission,
                        needsSaf = needsSaf,
                        context = context,
                        onRequestSaf = onRequestSafPermission
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
private fun GfxPresetSelectorCard(
    selectedConfig: GfxConfig,
    availablePresets: List<GfxConfig>,
    onSelectPreset: (GfxConfig) -> Unit,
    onApplyGfx: () -> Unit,
    hasSaf: Boolean,
    onRequestSaf: () -> Unit,
    applyResult: com.xeno.gfx.core.gfx.ApplyResult?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BGMI GRAPHIC CONFIG",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (hasSaf) NeonGreen.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f))
                            .border(1.dp, if (hasSaf) NeonGreen else AmberWarning, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (hasSaf) "SAF OK" else "SAF NEEDED",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSaf) NeonGreen else AmberWarning
                        )
                    }
                }
            }

            Text(
                text = "Hardware-tuned Unreal Engine 4 CVars with 0x79 encryption & Active.sav FPS unlock.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            // Preset Chips Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availablePresets.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = preset.presetName == selectedConfig.presetName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CyberCyan.copy(alpha = 0.18f) else DarkSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberCyan else HudBorderColor,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectPreset(preset) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = preset.presetName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CyberCyan else Color.White
                                    )
                                    Text(
                                        text = "${preset.fpsTarget.displayName} • ${preset.resolutionScale.displayName}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Preset Spec Specs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ACTIVE PRESET PARAMETERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Graphics: ${selectedConfig.graphicQuality.displayName}",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Target: ${selectedConfig.fpsTarget.targetFps} FPS",
                            fontSize = 11.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "API: ${if (selectedConfig.graphicsApi == com.xeno.gfx.core.gfx.GraphicsApi.OPENGL_ES) "OpenGL" else "Vulkan"}",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Shadows: ${if (selectedConfig.shadowsEnabled) "ON" else "OFF"}",
                            fontSize = 11.sp,
                            color = if (selectedConfig.shadowsEnabled) AmberWarning else TextMuted
                        )
                        Text(
                            text = "MSAA: ${if (selectedConfig.antiAliasingEnabled) "ON" else "OFF"}",
                            fontSize = 11.sp,
                            color = if (selectedConfig.antiAliasingEnabled) CyberCyan else TextMuted
                        )
                        Text(
                            text = "Audio: ${selectedConfig.soundQuality.displayName.substringBefore(" ")}",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Feedback Message
            applyResult?.let { res ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (res.isSuccess) NeonGreen.copy(alpha = 0.12f) else NeonRed.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            if (res.isSuccess) NeonGreen.copy(alpha = 0.4f) else NeonRed.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = res.message,
                        color = if (res.isSuccess) NeonGreen else NeonRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Row: Apply GFX & SAF Grant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasSaf) {
                    Button(
                        onClick = onRequestSaf,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = DarkBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Storage Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onApplyGfx,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply GFX Only", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(
    session: GameSessionStats,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCyan, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAST SESSION REPORT",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    label = "DURATION",
                    value = formatDuration(session.durationSeconds),
                    color = Color.White
                )
                MetricItem(
                    label = "AVG FPS",
                    value = "%.1f".format(session.averageFps),
                    color = if (session.averageFps >= 28f) CyberCyan else NeonGreen
                )
                MetricItem(
                    label = "PEAK TEMP",
                    value = "%.1f°C".format(session.peakTempCelsius),
                    color = if (session.peakTempCelsius > 42f) NeonRed else NeonGreen
                )
                MetricItem(
                    label = "STUTTERS",
                    value = "${session.stutterCount}",
                    color = if (session.stutterCount == 0) NeonGreen else AmberWarning
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s}s"
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
                        "TURBO LAUNCH BGMI"
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
    needsSaf: Boolean,
    context: Context,
    onRequestSaf: () -> Unit
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

            if (needsSaf) {
                PermissionRow(
                    title = "Storage Access (Android 11+)",
                    desc = "Required to configure BGMI graphic presets into Android/data.",
                    onGrant = onRequestSaf
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
                    label = "CALIBRATED FPS",
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
