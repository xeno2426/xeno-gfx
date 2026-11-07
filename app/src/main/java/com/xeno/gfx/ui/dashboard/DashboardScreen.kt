package com.xeno.gfx.ui.dashboard

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xeno.gfx.core.gfx.GfxConfig
import com.xeno.gfx.core.gfx.GraphicsApi
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.permission.PermissionHelper
import com.xeno.gfx.core.stats.GameSessionStats
import com.xeno.gfx.ui.theme.AmberWarning
import com.xeno.gfx.ui.theme.BorderOutline
import com.xeno.gfx.ui.theme.BorderOutlineVariant
import com.xeno.gfx.ui.theme.CyberCyan
import com.xeno.gfx.ui.theme.DarkBackground
import com.xeno.gfx.ui.theme.DarkSurface
import com.xeno.gfx.ui.theme.DarkSurfaceElevated
import com.xeno.gfx.ui.theme.ElectricAmber
import com.xeno.gfx.ui.theme.GreenGlow
import com.xeno.gfx.ui.theme.HotRed
import com.xeno.gfx.ui.theme.HudBorderColor
import com.xeno.gfx.ui.theme.LaserGreen
import com.xeno.gfx.ui.theme.NeonGreen
import com.xeno.gfx.ui.theme.NeonRed
import com.xeno.gfx.ui.theme.SurfaceContainerHigh
import com.xeno.gfx.ui.theme.SurfaceContainerHighest
import com.xeno.gfx.ui.theme.TextMuted
import com.xeno.gfx.ui.theme.TextVariant

enum class DashboardTab {
    DASHBOARD, PRESETS, TELEMETRY, GAMES
}

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
    val needsStorage = !uiState.hasSafPermission
    var selectedTab by remember { mutableStateOf(DashboardTab.DASHBOARD) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            TacticalBottomNav(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top App Bar Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TopTacticalHeader()
            }

            // 2. Hardware Status & Profile Strip
            item {
                HardwareStatusStrip()
            }

            // 3. Last Completed Game Session Report
            uiState.lastCompletedSession?.let { session ->
                item {
                    SessionSummaryCard(
                        session = session,
                        onDismiss = onDismissSessionSummary
                    )
                }
            }

            // 4. Hero Section: 1-Tap Turbo Launch BGMI
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.GAMES) {
                item {
                    TurboHeroSection(
                        uiState = uiState,
                        onCleanNow = onManualCleanRam,
                        onLaunchBgmi = onLaunchBgmiWithTurbo,
                        onRequestStorage = onRequestSafPermission
                    )
                }
            }

            // 5. Real-Time Hardware Telemetry HUD (4 Gauges)
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.TELEMETRY) {
                item {
                    HardwareTelemetryHud(metrics = uiState.previewMetrics)
                }
            }

            // 6. Graphics Engine Presets
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.PRESETS) {
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
            }

            // 7. Permissions Action Card (if any pending)
            if (!uiState.hasOverlayPermission || !uiState.hasUsageStatsPermission || needsStorage) {
                item {
                    PermissionCard(
                        hasOverlay = uiState.hasOverlayPermission,
                        hasUsageStats = uiState.hasUsageStatsPermission,
                        needsSaf = needsStorage,
                        context = context,
                        onRequestSaf = onRequestSafPermission
                    )
                }
            }

            // 8. In-Game Suppressor & Floating HUD Controls
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.GAMES) {
                item {
                    FloatingHudAndSuppressorCard(
                        isHudActive = uiState.isHudActive,
                        hasOverlayPermission = uiState.hasOverlayPermission,
                        onToggleHud = onToggleManualHud,
                        onGrantOverlay = {
                            context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                        },
                        isSuppressionActive = uiState.isContinuousSuppressionEnabled,
                        onToggleSuppression = onToggleContinuousSuppression
                    )
                }
            }

            // 9. Auto-Boost & Detected Games Section
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.GAMES) {
                item {
                    AutoBoostAndGamesSection(
                        isAutoEnabled = uiState.isAutoHudEnabled,
                        hasUsagePermission = uiState.hasUsageStatsPermission,
                        onToggleAuto = onToggleAutoHud,
                        onGrantUsage = {
                            context.startActivity(PermissionHelper.getUsageStatsPermissionIntent(context))
                        },
                        installedApps = uiState.installedApps,
                        trackedPackages = uiState.trackedGamePackages,
                        onToggleTracked = onToggleTrackedGame
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TopTacticalHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "XENO GFX PRO",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberCyan,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "TACTICAL PERFORMANCE HUD",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, BorderOutlineVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LaserGreen)
                )
                Text(
                    text = "KERNEL v4.19",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = LaserGreen
                )
            }
        }
    }
}

@Composable
private fun HardwareStatusStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, BorderOutlineVariant.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Core Online Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, LaserGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LaserGreen))
                    Text(
                        text = "CORE ONLINE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = LaserGreen
                    )
                }
            }

            // 4GB Turbo Profile Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, ElectricAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(12.dp))
                    Text(
                        text = "4GB TURBO",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ElectricAmber
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(13.dp))
            Text(
                text = "BGMI // SD 870",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TurboHeroSection(
    uiState: DashboardUiState,
    onCleanNow: () -> Unit,
    onLaunchBgmi: () -> Unit,
    onRequestStorage: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LaserGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(LaserGreen.copy(alpha = 0.15f))
                        .border(1.dp, LaserGreen.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GAME DRIVER LINKED",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = LaserGreen
                    )
                }

                Text(
                    text = "EXEC_ENV // BGMI.v3.1",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TURBO OVERDRIVE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Direct Vulkan injection pipeline with real-time frame jitter suppression and thermal throttling bypass.",
                    fontSize = 11.sp,
                    color = TextVariant,
                    lineHeight = 15.sp
                )
            }

            // Primary Glowing Turbo Button
            Button(
                onClick = {
                    if (uiState.isBgmiInstalled) {
                        if (!uiState.hasSafPermission) {
                            onRequestStorage()
                        } else {
                            onLaunchBgmi()
                        }
                    } else {
                        onCleanNow()
                    }
                },
                enabled = !uiState.isCleaningInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LaserGreen,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
                        "PURGE BACKGROUND RAM"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            // Sub-Stats Triple Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubStatBadge(
                    icon = Icons.Default.CleaningServices,
                    iconTint = CyberCyan,
                    label = "RAM PURGE",
                    value = "+320MB TARGET",
                    modifier = Modifier.weight(1f)
                )
                SubStatBadge(
                    icon = Icons.Default.Shield,
                    iconTint = LaserGreen,
                    label = "30s SUPPRESSOR",
                    value = "ACTIVE",
                    modifier = Modifier.weight(1f)
                )
                SubStatBadge(
                    icon = Icons.Default.PictureInPicture,
                    iconTint = CyberCyan,
                    label = "HUD OVERLAY",
                    value = "AUTO-TRIGGER",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SubStatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkBackground)
            .border(1.dp, BorderOutline, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(12.dp))
                Text(text = label, fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
            }
            Text(
                text = value,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
        }
    }
}

@Composable
private fun HardwareTelemetryHud(metrics: DeviceMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyberCyan)
                )
                Text(
                    text = "HARDWARE TELEMETRY HUD",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, BorderOutlineVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "POLLING: 1000Hz KERNEL",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }

        // 4 Telemetry Gauges (2x2 Grid)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Gauge 1: FPS
            TelemetryGaugeCard(
                icon = Icons.Default.Speed,
                iconTint = CyberCyan,
                label = "CALIBRATED FPS",
                value = "${metrics.calibratedFps}",
                unit = "FPS",
                badgeText = "STABLE 99.8%",
                badgeColor = LaserGreen,
                progress = (metrics.calibratedFps / 30f).coerceIn(0f, 1f),
                progressColor = if (metrics.calibratedFps >= 28) CyberCyan else NeonGreen,
                modifier = Modifier.weight(1f)
            )

            // Gauge 2: RAM
            TelemetryGaugeCard(
                icon = Icons.Default.Memory,
                iconTint = ElectricAmber,
                label = "RAM USAGE",
                value = "${metrics.ramPercent}",
                unit = "%",
                badgeText = "${(metrics.ramTotalMb - metrics.ramUsedMb).coerceAtLeast(0)}MB FREE",
                badgeColor = ElectricAmber,
                progress = (metrics.ramPercent / 100f).coerceIn(0f, 1f),
                progressColor = if (metrics.ramPercent > 80) HotRed else ElectricAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Gauge 3: Thermals
            TelemetryGaugeCard(
                icon = Icons.Default.Thermostat,
                iconTint = LaserGreen,
                label = "SOC THERMALS",
                value = if (metrics.batteryTempCelsius > 0) "%.1f".format(metrics.batteryTempCelsius) else "--",
                unit = "°C",
                badgeText = "NORMAL",
                badgeColor = LaserGreen,
                progress = (metrics.batteryTempCelsius / 45f).coerceIn(0f, 1f),
                progressColor = when {
                    metrics.batteryTempCelsius > 42 -> HotRed
                    metrics.batteryTempCelsius > 38 -> ElectricAmber
                    else -> LaserGreen
                },
                modifier = Modifier.weight(1f)
            )

            // Gauge 4: Latency Ping
            TelemetryGaugeCard(
                icon = Icons.Default.NetworkCheck,
                iconTint = LaserGreen,
                label = "DNS LATENCY",
                value = metrics.latencyMs?.toString() ?: "--",
                unit = "MS",
                badgeText = "OPTIMAL",
                badgeColor = LaserGreen,
                progress = 0.85f,
                progressColor = LaserGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryGaugeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    badgeText: String,
    badgeColor: Color,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, BorderOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
                    Text(text = label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = badgeText, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            // Micro progress gauge bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkSurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(progressColor)
                )
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
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.DisplaySettings, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "GRAPHICS ENGINE PRESETS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BGMI EDITION",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }

            // 2x2 Preset Cards Grid
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkSurfaceElevated else DarkBackground)
                                    .border(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) CyberCyan else BorderOutline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectPreset(preset) }
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = preset.presetName.substringBefore(" FPS") + " FPS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) CyberCyan else Color.White
                                        )
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Text(
                                        text = "${preset.resolutionScale.displayName} • ${preset.graphicQuality.displayName}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fine Tuning Switches Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground)
                    .border(1.dp, BorderOutline, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "LOW-LEVEL RENDERING API", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, BorderOutlineVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedConfig.graphicsApi == GraphicsApi.OPENGL_ES) "OpenGL ES (4GB Stable)" else "Vulkan (Unlocked)",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = LaserGreen
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Dynamic Shadows: OFF", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                        Text(text = "MSAA 4X: OFF", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                        Text(text = "3D Audio: ULTRA", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                    }
                }
            }

            // Result banner
            applyResult?.let { res ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (res.isSuccess) LaserGreen.copy(alpha = 0.12f) else NeonRed.copy(alpha = 0.12f))
                        .border(1.dp, if (res.isSuccess) LaserGreen.copy(alpha = 0.4f) else NeonRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = res.message,
                        color = if (res.isSuccess) LaserGreen else NeonRed,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasSaf) {
                    val grantLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        "Grant SAF Folder"
                    } else {
                        "Grant Storage Access"
                    }
                    Button(
                        onClick = onRequestSaf,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber, contentColor = DarkBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(grantLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (!hasSaf) {
                            onRequestSaf()
                        } else {
                            onApplyGfx()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply GFX Only", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FloatingHudAndSuppressorCard(
    isHudActive: Boolean,
    hasOverlayPermission: Boolean,
    onToggleHud: () -> Unit,
    onGrantOverlay: () -> Unit,
    isSuppressionActive: Boolean,
    onToggleSuppression: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Floating HUD Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Column {
                        Text(text = "Floating HUD Overlay", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Stealth real-time FPS & thermals overlay widget", fontSize = 10.sp, color = TextMuted)
                    }
                }

                Button(
                    onClick = {
                        if (hasOverlayPermission) onToggleHud() else onGrantOverlay()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHudActive) HotRed else CyberCyan,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (!hasOverlayPermission) "GRANT" else if (isHudActive) "STOP" else "START",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mini Stealth HUD Preview Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkBackground)
                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LaserGreen))
                        Text(text = "PREVIEW:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "30.0 FPS | 37°C | 28ms",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }

                    Text(text = "OPACITY: 75%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                }
            }

            // In-Game 30s Gunfight Suppressor Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "30s In-Game Background Suppressor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(text = "Auto-purges cached processes every 30s during matches", fontSize = 10.sp, color = TextMuted)
                }
                Switch(
                    checked = isSuppressionActive,
                    onCheckedChange = { onToggleSuppression(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = LaserGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }
    }
}

@Composable
private fun AutoBoostAndGamesSection(
    isAutoEnabled: Boolean,
    hasUsagePermission: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    onGrantUsage: () -> Unit,
    installedApps: List<com.xeno.gfx.core.model.TrackedGame>,
    trackedPackages: Set<String>,
    onToggleTracked: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderOutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, tint = LaserGreen, modifier = Modifier.size(18.dp))
                    Text(text = "Auto-Boost on Game Launch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Switch(
                    checked = isAutoEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !hasUsagePermission) onGrantUsage() else onToggleAuto(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = LaserGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Text(text = "TARGET GAMES TO TRACK:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)

            // Horizontal Detected Games Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Static Popular Games + Dynamic Installed
                listOf("BGMI", "PUBG Global", "Free Fire MAX", "COD Mobile").forEach { gameName ->
                    val isChecked = gameName == "BGMI" || trackedPackages.any { it.contains("pubg", ignoreCase = true) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecked) CyberCyan.copy(alpha = 0.15f) else DarkSurfaceElevated)
                            .border(1.dp, if (isChecked) CyberCyan else BorderOutline, RoundedCornerShape(6.dp))
                            .clickable { }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isChecked) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                            } else {
                                Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = gameName,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChecked) CyberCyan else TextVariant
                            )
                        }
                    }
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
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCyan, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Text(text = "LAST SESSION REPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryMetric("DURATION", "${session.durationSeconds / 60}m ${session.durationSeconds % 60}s", Color.White)
                SummaryMetric("AVG FPS", "%.1f".format(session.averageFps), if (session.averageFps >= 28f) CyberCyan else LaserGreen)
                SummaryMetric("PEAK TEMP", "%.1f°C".format(session.peakTempCelsius), if (session.peakTempCelsius > 42f) HotRed else LaserGreen)
                SummaryMetric("STUTTERS", "${session.stutterCount}", if (session.stutterCount == 0) LaserGreen else ElectricAmber)
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 9.sp, color = TextMuted)
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
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ElectricAmber.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(16.dp))
                Text(text = "Required Permissions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricAmber)
            }

            if (!hasOverlay) {
                PermissionMiniRow(
                    title = "Display Over Other Apps",
                    onGrant = { context.startActivity(PermissionHelper.getOverlayPermissionIntent(context)) }
                )
            }

            if (!hasUsageStats) {
                PermissionMiniRow(
                    title = "Usage Access",
                    onGrant = { context.startActivity(PermissionHelper.getUsageStatsPermissionIntent(context)) }
                )
            }

            if (needsSaf) {
                val storageLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "Storage Access (SAF Android 11+)"
                } else {
                    "Storage Permission (Write Storage)"
                }
                PermissionMiniRow(
                    title = storageLabel,
                    onGrant = onRequestSaf
                )
            }
        }
    }
}

@Composable
private fun PermissionMiniRow(title: String, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 11.sp, color = Color.White)
        OutlinedButton(
            onClick = onGrant,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TacticalBottomNav(
    selectedTab: DashboardTab,
    onSelectTab: (DashboardTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .border(1.dp, BorderOutline, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Default.Speed,
            label = "DASHBOARD",
            isSelected = selectedTab == DashboardTab.DASHBOARD,
            onClick = { onSelectTab(DashboardTab.DASHBOARD) }
        )
        BottomNavItem(
            icon = Icons.Default.DisplaySettings,
            label = "PRESETS",
            isSelected = selectedTab == DashboardTab.PRESETS,
            onClick = { onSelectTab(DashboardTab.PRESETS) }
        )
        BottomNavItem(
            icon = Icons.Default.Analytics,
            label = "TELEMETRY",
            isSelected = selectedTab == DashboardTab.TELEMETRY,
            onClick = { onSelectTab(DashboardTab.TELEMETRY) }
        )
        BottomNavItem(
            icon = Icons.Default.SportsEsports,
            label = "GAMES",
            isSelected = selectedTab == DashboardTab.GAMES,
            onClick = { onSelectTab(DashboardTab.GAMES) }
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
            .border(1.dp, if (isSelected) CyberCyan.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyberCyan else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CyberCyan else TextMuted
            )
        }
    }
}
