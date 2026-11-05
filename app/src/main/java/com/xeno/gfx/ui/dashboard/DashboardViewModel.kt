package com.xeno.gfx.ui.dashboard

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xeno.gfx.core.detector.ForegroundGameDetector
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.model.TrackedGame
import com.xeno.gfx.core.permission.PermissionHelper
import com.xeno.gfx.core.stats.StatsCollector
import com.xeno.gfx.service.HudOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val hasOverlayPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val isHudActive: Boolean = false,
    val isAutoHudEnabled: Boolean = false,
    val previewMetrics: DeviceMetrics = DeviceMetrics(),
    val installedApps: List<TrackedGame> = emptyList(),
    val trackedGamePackages: Set<String> = emptySet()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val statsCollector = StatsCollector(context)
    private val gameDetector = ForegroundGameDetector(context)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Collect live preview metrics
        viewModelScope.launch {
            statsCollector.metrics.collect { metrics ->
                _uiState.value = _uiState.value.copy(previewMetrics = metrics)
            }
        }

        // Configure auto-HUD game detection listeners
        gameDetector.onGameLaunched = { _ ->
            if (_uiState.value.isAutoHudEnabled && PermissionHelper.hasOverlayPermission(context)) {
                HudOverlayService.start(context)
                _uiState.value = _uiState.value.copy(isHudActive = true)
            }
        }

        gameDetector.onGameExited = { _ ->
            if (_uiState.value.isAutoHudEnabled) {
                HudOverlayService.stop(context)
                _uiState.value = _uiState.value.copy(isHudActive = false)
            }
        }

        refreshPermissions()
        loadInstalledLauncherApps()
    }

    fun onResume() {
        refreshPermissions()
        statsCollector.startSampling(viewModelScope, 1000L)
    }

    fun onPause() {
        statsCollector.stopSampling()
    }

    fun refreshPermissions() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val hasUsage = PermissionHelper.hasUsageStatsPermission(context)
        _uiState.value = _uiState.value.copy(
            hasOverlayPermission = hasOverlay,
            hasUsageStatsPermission = hasUsage
        )
    }

    fun toggleManualHud() {
        val nextState = !_uiState.value.isHudActive
        if (nextState) {
            HudOverlayService.start(context)
        } else {
            HudOverlayService.stop(context)
        }
        _uiState.value = _uiState.value.copy(isHudActive = nextState)
    }

    fun toggleAutoHud(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoHudEnabled = enabled)
        if (enabled) {
            gameDetector.setTrackedGames(_uiState.value.trackedGamePackages)
            gameDetector.startMonitoring(viewModelScope)
        } else {
            gameDetector.stopMonitoring()
        }
    }

    fun toggleTrackedGame(packageName: String) {
        val current = _uiState.value.trackedGamePackages.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _uiState.value = _uiState.value.copy(trackedGamePackages = current)
        gameDetector.setTrackedGames(current)
    }

    private fun loadInstalledLauncherApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val myPackage = context.packageName

            val games = resolveInfos
                .mapNotNull { it.activityInfo?.applicationInfo }
                .filter { it.packageName != myPackage }
                .distinctBy { it.packageName }
                .map { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                    TrackedGame(
                        packageName = appInfo.packageName,
                        appName = label,
                        icon = icon
                    )
                }
                .sortedBy { it.appName.lowercase() }

            _uiState.value = _uiState.value.copy(
                installedApps = games,
                // Default tracked packages: initial empty or games
                trackedGamePackages = games.take(3).map { it.packageName }.toSet()
            )
            gameDetector.setTrackedGames(_uiState.value.trackedGamePackages)
        }
    }
}
