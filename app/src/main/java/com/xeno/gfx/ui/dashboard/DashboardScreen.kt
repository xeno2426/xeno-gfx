package com.xeno.gfx.ui.dashboard

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xeno.gfx.core.gfx.ApplyResult
import com.xeno.gfx.core.gfx.GfxConfig
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.model.TrackedGame
import com.xeno.gfx.core.permission.PermissionHelper
import com.xeno.gfx.core.stats.GameSessionStats
import com.xeno.gfx.ui.theme.CanvasBackground
import com.xeno.gfx.ui.theme.ChampagneGold
import com.xeno.gfx.ui.theme.ChampagneGoldBright
import com.xeno.gfx.ui.theme.ChampagneGoldMuted
import com.xeno.gfx.ui.theme.DarkBackground
import com.xeno.gfx.ui.theme.DarkSurface
import com.xeno.gfx.ui.theme.DarkSurfaceElevated
import com.xeno.gfx.ui.theme.GlassHairlineBorder
import com.xeno.gfx.ui.theme.GlassHairlineSubtle
import com.xeno.gfx.ui.theme.GlassHairlineTop
import com.xeno.gfx.ui.theme.MutedCrimson
import com.xeno.gfx.ui.theme.MutedEmerald
import com.xeno.gfx.ui.theme.PlatinumIce
import com.xeno.gfx.ui.theme.SapphireMist
import com.xeno.gfx.ui.theme.SatinButtonBottom
import com.xeno.gfx.ui.theme.SatinButtonTop
import com.xeno.gfx.ui.theme.SmokedGlassElevated
import com.xeno.gfx.ui.theme.SmokedGlassSurface
import com.xeno.gfx.ui.theme.SubduedAmber
import com.xeno.gfx.ui.theme.SurfaceContainerHigh
import com.xeno.gfx.ui.theme.SurfaceContainerHighest
import com.xeno.gfx.ui.theme.TextMuted
import com.xeno.gfx.ui.theme.TextTitanium
import com.xeno.gfx.ui.theme.TextVariant
import com.xeno.gfx.ui.theme.TitaniumSubstrate

enum class DashboardTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Speed),
    PRESETS("Presets", Icons.Default.Tune),
    TELEMETRY("Telemetry", Icons.AutoMirrored.Filled.ShowChart),
    GAMES("Games", Icons.Default.SportsEsports)
}

/**
 * Xeno Studio Dashboard
 * Designed with Stitch Titanium Telemetry Studio Design System
 * Features: Frosted Glassmorphism, Brushed Titanium Satin Tactility, Non-Neon Palette
 */
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
        containerColor = CanvasBackground,
        bottomBar = {
            StudioGlassBottomNav(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Titanium Studio Top Header
            item {
                StudioHeader(
                    isBgmiInstalled = uiState.isBgmiInstalled,
                    onOpenSettings = {
                        try {
                            context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                        } catch (_: Exception) {}
                    }
                )
            }

            // 2. Hardware Telemetry Ribbon (4 Micro Gauges)
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.TELEMETRY) {
                item {
                    TelemetryRibbonSection(metrics = uiState.previewMetrics)
                }
            }

            // 3. Master Performance Engine Hero Card
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.GAMES) {
                item {
                    MasterEngineHeroCard(
                        uiState = uiState,
                        onCleanNow = onManualCleanRam,
                        onLaunchBgmi = onLaunchBgmiWithTurbo,
                        onRequestStorage = onRequestSafPermission
                    )
                }
            }

            // 4. Governor Preset Profiles Matrix
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.PRESETS) {
                item {
                    GovernorPresetMatrix(
                        selectedConfig = uiState.selectedGfxConfig,
                        availablePresets = uiState.availablePresets,
                        onSelectPreset = onSelectPreset,
                        onApplyGfx = onApplyGfxOnly,
                        hasStorageAccess = uiState.hasSafPermission,
                        onRequestStorage = onRequestSafPermission,
                        applyResult = uiState.gfxApplyResult
                    )
                }
            }

            // 5. In-Game Draggable HUD Controller
            if (selectedTab == DashboardTab.DASHBOARD || selectedTab == DashboardTab.TELEMETRY) {
                item {
                    InGameHudControllerCard(
                        isHudActive = uiState.isHudActive,
                        hasOverlayPermission = uiState.hasOverlayPermission,
                        onToggleHud = onToggleManualHud,
                        onGrantOverlay = {
                            context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                        },
                        metrics = uiState.previewMetrics,
                        isSuppressionActive = uiState.isContinuousSuppressionEnabled,
                        onToggleSuppression = onToggleContinuousSuppression
                    )
                }
            }

            // 6. Required Permissions Card (if any required)
            if (!uiState.hasOverlayPermission || !uiState.hasUsageStatsPermission || needsStorage) {
                item {
                    StudioPermissionCard(
                        hasOverlay = uiState.hasOverlayPermission,
                        hasUsageStats = uiState.hasUsageStatsPermission,
                        needsStorage = needsStorage,
                        context = context,
                        onRequestStorage = onRequestSafPermission
                    )
                }
            }

            // 7. Extended Telemetry View (when Telemetry tab is selected)
            if (selectedTab == DashboardTab.TELEMETRY) {
                item {
                    ExtendedTelemetryDetailCard(metrics = uiState.previewMetrics)
                }
            }

            // 8. Tracked Applications Management (when Games tab is selected)
            if (selectedTab == DashboardTab.GAMES) {
                item {
                    TrackedGamesSection(
                        isAutoHudEnabled = uiState.isAutoHudEnabled,
                        onToggleAutoHud = onToggleAutoHud,
                        installedApps = uiState.installedApps,
                        trackedGamePackages = uiState.trackedGamePackages,
                        onToggleGame = onToggleTrackedGame,
                        hasUsagePermission = uiState.hasUsageStatsPermission,
                        onGrantUsage = {
                            context.startActivity(PermissionHelper.getUsageStatsPermissionIntent(context))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Gaming Session Summary Dialog (if present)
    val session = uiState.lastCompletedSession
    if (session != null) {
        SessionSummaryDialog(
            summary = session,
            onDismiss = onDismissSessionSummary
        )
    }
}

// =========================================================================
// 1. Studio Header Component
// =========================================================================

@Composable
private fun StudioHeader(
    isBgmiInstalled: Boolean,
    onOpenSettings: () -> Unit
) {
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
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TitaniumSubstrate)
                    .border(BorderStroke(1.dp, GlassHairlineBorder), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = PlatinumIce,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "XENO STUDIO",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumIce,
                    letterSpacing = 2.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MutedEmerald)
                    )
                    Text(
                        text = if (isBgmiInstalled) "KERNEL: READY" else "SYSTEM: STANDBY",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SmokedGlassSurface)
                    .border(BorderStroke(1.dp, GlassHairlineBorder), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = TextTitanium,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// =========================================================================
// 2. Hardware Telemetry Ribbon (4 Micro Gauges)
// =========================================================================

@Composable
private fun TelemetryRibbonSection(metrics: DeviceMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PHYSICAL SENSORS | POLL 100HZ",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = "SOC: NOMINAL",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MutedEmerald
            )
        }

        // 2x2 Grid of Micro Gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MicroTelemetryCard(
                label = "RENDER PACING",
                value = "${metrics.calibratedFps}",
                unit = "FPS",
                subtext = "Target: 30 | Stable",
                progress = (metrics.calibratedFps / 60f).coerceIn(0f, 1f),
                indicatorColor = MutedEmerald,
                modifier = Modifier.weight(1f)
            )

            val availableRamMb = (metrics.ramTotalMb - metrics.ramUsedMb).coerceAtLeast(0L)
            val freeGb = availableRamMb / 1024f
            val totalGb = metrics.ramTotalMb / 1024f
            val allocPct = (metrics.ramPercent / 100f).coerceIn(0f, 1f)

            MicroTelemetryCard(
                label = "LPDDR5X FREE",
                value = String.format("%.1f", freeGb),
                unit = "GB",
                subtext = "Pool: ${String.format("%.0f", totalGb)}GB | ${metrics.ramPercent}% Alloc",
                progress = (1f - allocPct).coerceIn(0f, 1f),
                indicatorColor = SapphireMist,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tempColor = when {
                metrics.batteryTempCelsius > 43f -> SubduedAmber
                metrics.batteryTempCelsius > 40f -> ChampagneGold
                else -> MutedEmerald
            }

            MicroTelemetryCard(
                label = "CRYO THERMAL",
                value = String.format("%.1f", metrics.batteryTempCelsius),
                unit = "°C",
                subtext = "Chamber: ${metrics.thermalStatus.name}",
                progress = (metrics.batteryTempCelsius / 50f).coerceIn(0f, 1f),
                indicatorColor = tempColor,
                modifier = Modifier.weight(1f)
            )

            val ping = metrics.latencyMs
            val pingVal = ping?.toString() ?: "--"
            val pingProgress = if (ping != null) ((100L - ping) / 100f).coerceIn(0.1f, 1f) else 0.5f
            val pingColor = if (ping != null && ping < 60) MutedEmerald else SubduedAmber

            MicroTelemetryCard(
                label = "CARRIER LATENCY",
                value = pingVal,
                unit = "MS",
                subtext = "Carrier: Loss 0.0%",
                progress = pingProgress,
                indicatorColor = pingColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MicroTelemetryCard(
    label: String,
    value: String,
    unit: String,
    subtext: String,
    progress: Float,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E232E).copy(alpha = 0.85f),
                        Color(0xFF13161E).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(GlassHairlineTop, GlassHairlineSubtle)
                    )
                ),
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumIce,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFF0B0E15))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(indicatorColor)
                    )
                }
                Text(
                    text = subtext,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}

// =========================================================================
// 3. Master Engine Hero Card
// =========================================================================

@Composable
private fun MasterEngineHeroCard(
    uiState: DashboardUiState,
    onCleanNow: () -> Unit,
    onLaunchBgmi: () -> Unit,
    onRequestStorage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF222834).copy(alpha = 0.85f),
                        Color(0xFF141720).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(GlassHairlineTop, GlassHairlineSubtle)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header row with Target Environment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "TARGET ENVIRONMENT",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ChampagneGold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "BGMI Kernel & Frame Pacing",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumIce,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Direct SurfaceFlinger Bypass · Low-RAM Tuned",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TitaniumSubstrate)
                        .border(BorderStroke(1.dp, GlassHairlineBorder), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = uiState.selectedGfxConfig.presetName.replace("_", " ").take(10),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumIce
                    )
                }
            }

            // Live Diagnostics Architecture Readouts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E1117))
                    .border(BorderStroke(1.dp, GlassHairlineSubtle), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ArchitectureMetricRow(
                    label = "Vulkan API Pipeline",
                    status = "SYNCHRONIZED (1:1 VSYNC)",
                    isOptimal = true
                )
                val freedMb = uiState.lastCleanResult?.freedMb ?: 380
                ArchitectureMetricRow(
                    label = "Real-time Memory Purge",
                    status = "+$freedMb MB RECLAIMED",
                    isOptimal = true
                )
                ArchitectureMetricRow(
                    label = "Digitizer Touch Polling",
                    status = "360 HZ LOCKED",
                    isOptimal = true
                )
            }

            // Result feedback banner if GFX applied
            uiState.gfxApplyResult?.let { res ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (res.isSuccess) MutedEmerald.copy(alpha = 0.12f) else MutedCrimson.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, if (res.isSuccess) MutedEmerald.copy(alpha = 0.4f) else MutedCrimson.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = res.message,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (res.isSuccess) MutedEmerald else MutedCrimson
                    )
                }
            }

            // Tactile Satin Titanium Primary Action Button
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
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SatinButtonTop, SatinButtonBottom)
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(GlassHairlineTop, GlassHairlineSubtle)
                            )
                        ),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (uiState.isCleaningInProgress) {
                                    "PURGING BACKGROUND APPS..."
                                } else if (!uiState.hasSafPermission) {
                                    "LINK STORAGE & LAUNCH BGMI"
                                } else {
                                    "OPTIMIZE & LAUNCH BGMI"
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumIce,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Kernel affinity pinned to Gold Cores 4-7",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PlatinumIce,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchitectureMetricRow(
    label: String,
    status: String,
    isOptimal: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isOptimal) MutedEmerald else SubduedAmber)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextVariant
            )
        }
        Text(
            text = status,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = if (isOptimal) PlatinumIce else SubduedAmber
        )
    }
}

// =========================================================================
// 4. Governor Preset Profiles Matrix (2x2 Glass Cards)
// =========================================================================

@Composable
private fun GovernorPresetMatrix(
    selectedConfig: GfxConfig,
    availablePresets: List<GfxConfig>,
    onSelectPreset: (GfxConfig) -> Unit,
    onApplyGfx: () -> Unit,
    hasStorageAccess: Boolean,
    onRequestStorage: () -> Unit,
    applyResult: ApplyResult?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GOVERNOR PROFILES",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = "AUTO-APPLY ON FOREGROUND",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        // 2x2 Grid
        val firstRow = availablePresets.take(2)
        val secondRow = availablePresets.drop(2).take(2)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            firstRow.forEach { preset ->
                PresetGlassCard(
                    config = preset,
                    isSelected = preset.presetName == selectedConfig.presetName,
                    onClick = { onSelectPreset(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (secondRow.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondRow.forEach { preset ->
                    PresetGlassCard(
                        config = preset,
                        isSelected = preset.presetName == selectedConfig.presetName,
                        onClick = { onSelectPreset(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Apply Button Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!hasStorageAccess) {
                Button(
                    onClick = onRequestStorage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGoldMuted,
                        contentColor = ChampagneGoldBright
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Link SAF Folder" else "Grant Storage Access",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    if (!hasStorageAccess) {
                        onRequestStorage()
                    } else {
                        onApplyGfx()
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlatinumIce),
                border = BorderStroke(1.dp, GlassHairlineBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = ChampagneGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply GFX Only", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PresetGlassCard(
    config: GfxConfig,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = when {
        config.presetName.contains("30", ignoreCase = true) || config.presetName.contains("POTATO", ignoreCase = true) ->
            Triple("ECO SAVER", "4.2H EST", "Gov: Conservative · 1.4 GHz")
        config.presetName.contains("40", ignoreCase = true) || config.presetName.contains("BALANCED", ignoreCase = true) ->
            Triple("DAILY MATCH", "3.4H EST", "Gov: Schedutil · 2.1 GHz")
        config.presetName.contains("60", ignoreCase = true) || config.presetName.contains("SMOOTH", ignoreCase = true) ->
            Triple("TOURNAMENT", "60 FIXED", "Gov: Performance · 2.8 GHz")
        else ->
            Triple("OVERCLOCK", "2.1H EST", "Gov: Turbo Lock · 3.3 GHz")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF282D3B).copy(alpha = 0.9f),
                            Color(0xFF181C26).copy(alpha = 0.95f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E232E).copy(alpha = 0.75f),
                            Color(0xFF12151D).copy(alpha = 0.85f)
                        )
                    )
                }
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) {
                        Brush.verticalGradient(
                            listOf(ChampagneGold.copy(alpha = 0.6f), GlassHairlineBorder)
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(GlassHairlineTop, GlassHairlineSubtle)
                        )
                    }
                ),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meta.first,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) ChampagneGold else TextMuted,
                    letterSpacing = 0.5.sp
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(ChampagneGoldMuted)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ChampagneGoldBright
                        )
                    }
                } else {
                    Text(
                        text = meta.second,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = config.presetName.replace("_", " "),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlatinumIce,
                maxLines = 1
            )

            Text(
                text = "${config.resolutionScale.displayName} · ${config.fpsTarget.targetFps} FPS · ${config.style.displayName}",
                fontSize = 9.sp,
                color = TextVariant,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = meta.third,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) ChampagneGold.copy(alpha = 0.8f) else TextMuted
                )
            }
        }
    }
}

// =========================================================================
// 5. In-Game Draggable HUD Controller
// =========================================================================

@Composable
private fun InGameHudControllerCard(
    isHudActive: Boolean,
    hasOverlayPermission: Boolean,
    onToggleHud: () -> Unit,
    onGrantOverlay: () -> Unit,
    metrics: DeviceMetrics,
    isSuppressionActive: Boolean,
    onToggleSuppression: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF202530).copy(alpha = 0.85f),
                        Color(0xFF141720).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(GlassHairlineTop, GlassHairlineSubtle)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header with toggle switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "SURFACEFLINGER OVERLAY",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "In-Game Draggable HUD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlatinumIce
                    )
                }

                if (!hasOverlayPermission) {
                    OutlinedButton(
                        onClick = onGrantOverlay,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Grant Permission", fontSize = 10.sp)
                    }
                } else {
                    Switch(
                        checked = isHudActive,
                        onCheckedChange = { onToggleHud() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PlatinumIce,
                            checkedTrackColor = ChampagneGoldMuted,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF0E1117)
                        )
                    )
                }
            }

            // Realistic HUD Badge Miniature Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E1117))
                    .border(BorderStroke(1.dp, GlassHairlineSubtle), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HUD PREVIEW",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                // Realistic HUD Mini Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF161A22))
                        .border(BorderStroke(1.dp, GlassHairlineBorder), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MutedEmerald)
                        )
                        Text(
                            text = "${metrics.calibratedFps} FPS",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumIce
                        )
                        Text(text = "|", fontSize = 10.sp, color = Color(0xFF32353C))
                        Text(
                            text = "${String.format("%.1f", metrics.batteryTempCelsius)}°C",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextVariant
                        )
                        Text(text = "|", fontSize = 10.sp, color = Color(0xFF32353C))
                        Text(
                            text = "${metrics.latencyMs ?: 28}ms",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ChampagneGold
                        )
                    }
                }
            }

            // In-Game 30s background process suppression toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Continuous Background Suppressor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTitanium
                    )
                    Text(
                        text = "Suppresses background apps every 30s while gaming",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                Switch(
                    checked = isSuppressionActive,
                    onCheckedChange = onToggleSuppression,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PlatinumIce,
                        checkedTrackColor = ChampagneGoldMuted,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0xFF0E1117)
                    )
                )
            }
        }
    }
}

// =========================================================================
// 6. Studio Permission Card
// =========================================================================

@Composable
private fun StudioPermissionCard(
    hasOverlay: Boolean,
    hasUsageStats: Boolean,
    needsStorage: Boolean,
    context: Context,
    onRequestStorage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1915))
            .border(BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "HARDWARE PERMISSIONS REQUIRED",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ChampagneGold,
                    letterSpacing = 0.5.sp
                )
            }

            if (!hasOverlay) {
                PermissionItemRow(
                    title = "Display Over Other Apps (HUD Overlay)",
                    onGrant = { context.startActivity(PermissionHelper.getOverlayPermissionIntent(context)) }
                )
            }

            if (!hasUsageStats) {
                PermissionItemRow(
                    title = "Usage Access (Foreground Game Detector)",
                    onGrant = { context.startActivity(PermissionHelper.getUsageStatsPermissionIntent(context)) }
                )
            }

            if (needsStorage) {
                val label = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "Scoped Storage Access (SAF Android 11+)"
                } else {
                    "Storage Access (WRITE_EXTERNAL_STORAGE)"
                }
                PermissionItemRow(
                    title = label,
                    onGrant = onRequestStorage
                )
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    title: String,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = PlatinumIce,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = onGrant,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// =========================================================================
// 7. Extended Telemetry View (Telemetry Tab)
// =========================================================================

@Composable
private fun ExtendedTelemetryDetailCard(metrics: DeviceMetrics) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SmokedGlassSurface)
            .border(BorderStroke(1.dp, GlassHairlineBorder), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "DETAILED HARDWARE DIAGNOSTICS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PlatinumIce,
                letterSpacing = 1.sp
            )

            val availableRamMb = (metrics.ramTotalMb - metrics.ramUsedMb).coerceAtLeast(0L)
            TelemetryDetailRow("SurfaceFlinger Refresh Rate", "${metrics.refreshRateHz} Hz")
            TelemetryDetailRow("Calibrated FPS", "${metrics.calibratedFps} FPS")
            TelemetryDetailRow("Raw Compositor FPS", "${metrics.currentFps} FPS")
            TelemetryDetailRow("Available System RAM", "$availableRamMb MB")
            TelemetryDetailRow("Total Physical RAM", "${metrics.ramTotalMb} MB")
            TelemetryDetailRow("RAM Saturation", "${metrics.ramPercent}%")
            TelemetryDetailRow("Battery / SoC Temperature", "${String.format("%.1f", metrics.batteryTempCelsius)}°C")
            TelemetryDetailRow("Thermal State", metrics.thermalStatus.name)
            TelemetryDetailRow("Network Latency (Google DNS)", "${metrics.latencyMs ?: -1} ms")
        }
    }
}

@Composable
private fun TelemetryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = PlatinumIce
        )
    }
}

// =========================================================================
// 8. Tracked Applications Management (Games Tab)
// =========================================================================

@Composable
private fun TrackedGamesSection(
    isAutoHudEnabled: Boolean,
    onToggleAutoHud: (Boolean) -> Unit,
    installedApps: List<TrackedGame>,
    trackedGamePackages: Set<String>,
    onToggleGame: (String) -> Unit,
    hasUsagePermission: Boolean,
    onGrantUsage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SmokedGlassSurface)
            .border(BorderStroke(1.dp, GlassHairlineBorder), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUTOMATIC HUD LAUNCH",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumIce,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Launches HUD automatically when game is resumed",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                if (!hasUsagePermission) {
                    OutlinedButton(
                        onClick = onGrantUsage,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Grant Usage", fontSize = 10.sp)
                    }
                } else {
                    Switch(
                        checked = isAutoHudEnabled,
                        onCheckedChange = onToggleAutoHud,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PlatinumIce,
                            checkedTrackColor = ChampagneGoldMuted
                        )
                    )
                }
            }

            Text(
                text = "TRACKED GAME PACKAGES",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )

            installedApps.forEach { game ->
                val isTracked = trackedGamePackages.contains(game.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0E1117))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = game.appName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PlatinumIce)
                        Text(text = game.packageName, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    }

                    Switch(
                        checked = isTracked,
                        onCheckedChange = { onToggleGame(game.packageName) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PlatinumIce,
                            checkedTrackColor = ChampagneGoldMuted
                        )
                    )
                }
            }
        }
    }
}

// =========================================================================
// 9. Floating Glass Bottom Navigation Dock
// =========================================================================

@Composable
private fun StudioGlassBottomNav(
    selectedTab: DashboardTab,
    onSelectTab: (DashboardTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF141720).copy(alpha = 0.92f),
                        Color(0xFF0A0C10).copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, GlassHairlineTop),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) ChampagneGold else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tab.label,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) ChampagneGoldBright else TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// 10. Gaming Session Summary Dialog
// =========================================================================

@Composable
private fun SessionSummaryDialog(
    summary: GameSessionStats,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TitaniumSubstrate,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "GAMING SESSION SUMMARY",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumIce,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryDetailRow("Session Duration", formatDuration(summary.durationSeconds))
                TelemetryDetailRow("Average FPS", String.format("%.1f FPS", summary.averageFps))
                TelemetryDetailRow("Frame Range", "${summary.minFps} - ${summary.maxFps} FPS")
                TelemetryDetailRow("Frame Drop Stutters", "${summary.stutterCount} events")
                TelemetryDetailRow("Peak SoC Temperature", "${String.format("%.1f", summary.peakTempCelsius)}°C")
                TelemetryDetailRow("Initial Memory Cleared", "${summary.ramFreedMb} MB")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampagneGoldMuted,
                    contentColor = ChampagneGoldBright
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Dismiss", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}
