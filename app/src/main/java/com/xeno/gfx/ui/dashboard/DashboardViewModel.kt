package com.xeno.gfx.ui.dashboard

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xeno.gfx.core.detector.ForegroundGameDetector
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.model.TrackedGame
import com.xeno.gfx.core.optimizer.CleanResult
import com.xeno.gfx.core.optimizer.RamOptimizer
import com.xeno.gfx.core.permission.PermissionHelper
import com.xeno.gfx.core.repository.TelemetryRepository
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
    val isContinuousSuppressionEnabled: Boolean = true,
    val isCleaningInProgress: Boolean = false,
    val lastCleanResult: CleanResult? = null,
    val previewMetrics: DeviceMetrics = DeviceMetrics(),
    val installedApps: List<TrackedGame> = emptyList(),
    val trackedGamePackages: Set<String> = emptySet(),
    val isBgmiInstalled: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val telemetryRepository = TelemetryRepository.getInstance(context)
    private val gameDetector = ForegroundGameDetector(context)
    val ramOptimizer = RamOptimizer(context)

    companion object {
        const val BGMI_PACKAGE_NAME = "com.pubg.imobile"
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Collect live preview metrics from demand-driven TelemetryRepository per D-04 & D-05
        viewModelScope.launch {
            telemetryRepository.metrics.collect { metrics ->
                _uiState.value = _uiState.value.copy(previewMetrics = metrics)
            }
        }

        // Configure auto-HUD game detection listeners
        gameDetector.onGameLaunched = { packageName ->
            if (_uiState.value.isAutoHudEnabled && PermissionHelper.hasOverlayPermission(context)) {
                HudOverlayService.start(context)
                _uiState.value = _uiState.value.copy(isHudActive = true)
            }

            // Automatic RAM purge & suppression when game is launched
            viewModelScope.launch {
                val cleanResult = ramOptimizer.purgeMemory(packageName)
                _uiState.value = _uiState.value.copy(lastCleanResult = cleanResult)
                if (_uiState.value.isContinuousSuppressionEnabled) {
                    ramOptimizer.startContinuousSuppression(viewModelScope, packageName)
                }
            }
        }

        gameDetector.onGameExited = { _ ->
            if (_uiState.value.isAutoHudEnabled) {
                HudOverlayService.stop(context)
                _uiState.value = _uiState.value.copy(isHudActive = false)
            }
            ramOptimizer.stopContinuousSuppression()
        }

        refreshPermissions()
        loadInstalledLauncherApps()
    }

    fun onResume() {
        refreshPermissions()
    }

    fun onPause() {
        // Telemetry sampling automatically pauses via WhileSubscribed(5000L) per D-05
    }

    fun refreshPermissions() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val hasUsage = PermissionHelper.hasUsageStatsPermission(context)
        val isBgmi = isPackageInstalled(BGMI_PACKAGE_NAME)
        _uiState.value = _uiState.value.copy(
            hasOverlayPermission = hasOverlay,
            hasUsageStatsPermission = hasUsage,
            isBgmiInstalled = isBgmi
        )
    }

    fun manualCleanRam() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaningInProgress = true)
            val result = ramOptimizer.purgeMemory(BGMI_PACKAGE_NAME)
            _uiState.value = _uiState.value.copy(
                lastCleanResult = result,
                isCleaningInProgress = false
            )
        }
    }

    fun launchBgmiWithTurbo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaningInProgress = true)
            val result = ramOptimizer.purgeMemory(BGMI_PACKAGE_NAME)
            _uiState.value = _uiState.value.copy(
                lastCleanResult = result,
                isCleaningInProgress = false
            )

            // Start HUD if permission granted
            if (PermissionHelper.hasOverlayPermission(context)) {
                HudOverlayService.start(context)
                _uiState.value = _uiState.value.copy(isHudActive = true)
            }

            // Start continuous suppression
            if (_uiState.value.isContinuousSuppressionEnabled) {
                ramOptimizer.startContinuousSuppression(viewModelScope, BGMI_PACKAGE_NAME)
            }

            // Launch BGMI
            val launchIntent = context.packageManager.getLaunchIntentForPackage(BGMI_PACKAGE_NAME)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        }
    }

    fun toggleContinuousSuppression(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isContinuousSuppressionEnabled = enabled)
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

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
