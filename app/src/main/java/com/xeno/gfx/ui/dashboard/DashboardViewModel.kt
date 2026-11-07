package com.xeno.gfx.ui.dashboard

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xeno.gfx.core.detector.ForegroundGameDetector
import com.xeno.gfx.core.gfx.ApplyResult
import com.xeno.gfx.core.gfx.GfxConfig
import com.xeno.gfx.core.gfx.GfxFileManager
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.model.TrackedGame
import com.xeno.gfx.core.optimizer.CleanResult
import com.xeno.gfx.core.optimizer.RamOptimizer
import com.xeno.gfx.core.permission.PermissionHelper
import com.xeno.gfx.core.repository.SettingsRepository
import com.xeno.gfx.core.repository.TelemetryRepository
import com.xeno.gfx.core.stats.GameSessionStats
import com.xeno.gfx.core.stats.SessionTracker
import com.xeno.gfx.service.HudOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val hasOverlayPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasSafPermission: Boolean = false,
    val safTreeUri: Uri? = null,
    val isHudActive: Boolean = false,
    val isAutoHudEnabled: Boolean = false,
    val isContinuousSuppressionEnabled: Boolean = true,
    val isCleaningInProgress: Boolean = false,
    val lastCleanResult: CleanResult? = null,
    val previewMetrics: DeviceMetrics = DeviceMetrics(),
    val installedApps: List<TrackedGame> = emptyList(),
    val trackedGamePackages: Set<String> = emptySet(),
    val isBgmiInstalled: Boolean = false,
    val selectedGfxConfig: GfxConfig = GfxConfig.POTATO_ZERO_LAG_30,
    val availablePresets: List<GfxConfig> = listOf(
        GfxConfig.POTATO_ZERO_LAG_30,
        GfxConfig.SMOOTH_60_FPS,
        GfxConfig.BALANCED_40_FPS,
        GfxConfig.COMPETITIVE_90_FPS
    ),
    val gfxApplyResult: ApplyResult? = null,
    val lastCompletedSession: GameSessionStats? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val telemetryRepository = TelemetryRepository.getInstance(context)
    private val settingsRepository = SettingsRepository.getInstance(context)
    private val gameDetector = ForegroundGameDetector(context)
    val ramOptimizer = RamOptimizer(context)
    val sessionTracker = SessionTracker()

    companion object {
        const val BGMI_PACKAGE_NAME = "com.pubg.imobile"
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Collect live preview metrics from demand-driven TelemetryRepository
        viewModelScope.launch {
            telemetryRepository.metrics.collect { metrics ->
                _uiState.value = _uiState.value.copy(previewMetrics = metrics)
                sessionTracker.recordMetrics(
                    fps = metrics.calibratedFps,
                    tempCelsius = metrics.batteryTempCelsius,
                    targetFps = _uiState.value.selectedGfxConfig.fpsTarget.targetFps
                )
            }
        }

        // Collect persistent user settings from SettingsRepository
        viewModelScope.launch {
            settingsRepository.userSettings.collect { settings ->
                val parsedSafUri = settings.safTreeUriString?.let { Uri.parse(it) }
                val hasSaf = GfxFileManager.hasAccess(context, parsedSafUri)

                val matchedPreset = _uiState.value.availablePresets.find {
                    it.presetName == settings.selectedPresetName
                } ?: GfxConfig.POTATO_ZERO_LAG_30

                _uiState.value = _uiState.value.copy(
                    isContinuousSuppressionEnabled = settings.isContinuousSuppressionEnabled,
                    isAutoHudEnabled = settings.isAutoHudEnabled,
                    trackedGamePackages = settings.trackedGamePackages,
                    safTreeUri = parsedSafUri,
                    hasSafPermission = hasSaf,
                    selectedGfxConfig = matchedPreset
                )

                gameDetector.setTrackedGames(settings.trackedGamePackages)
                if (settings.isAutoHudEnabled && PermissionHelper.hasUsageStatsPermission(context)) {
                    gameDetector.startMonitoring(viewModelScope)
                } else {
                    gameDetector.stopMonitoring()
                }
            }
        }

        // Configure auto-HUD game detection listeners
        gameDetector.onGameLaunched = { packageName ->
            if (_uiState.value.isAutoHudEnabled && PermissionHelper.hasOverlayPermission(context)) {
                HudOverlayService.start(context)
                _uiState.value = _uiState.value.copy(isHudActive = true)
            }

            sessionTracker.startSession(packageName)

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
            val completed = sessionTracker.stopSession()
            if (completed != null) {
                _uiState.value = _uiState.value.copy(lastCompletedSession = completed)
            }
        }

        refreshPermissions()
        loadInstalledLauncherApps()
    }

    fun onResume() {
        refreshPermissions()
    }

    fun onPause() {
        // Demand-driven telemetry pauses automatically via WhileSubscribed(5000L)
    }

    fun refreshPermissions() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
        val hasUsage = PermissionHelper.hasUsageStatsPermission(context)
        val isBgmi = isPackageInstalled(BGMI_PACKAGE_NAME)
        val hasSaf = GfxFileManager.hasAccess(context, _uiState.value.safTreeUri)

        _uiState.value = _uiState.value.copy(
            hasOverlayPermission = hasOverlay,
            hasUsageStatsPermission = hasUsage,
            hasSafPermission = hasSaf,
            isBgmiInstalled = isBgmi
        )
    }

    fun onSafUriGranted(uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {
            // Some ROMs do not require explicit flag grant
        }

        viewModelScope.launch {
            settingsRepository.saveSafTreeUri(uri.toString())
            _uiState.value = _uiState.value.copy(
                safTreeUri = uri,
                hasSafPermission = true
            )
        }
    }

    fun selectPreset(config: GfxConfig) {
        _uiState.value = _uiState.value.copy(selectedGfxConfig = config)
        viewModelScope.launch {
            settingsRepository.saveSelectedPreset(config.presetName)
        }
    }

    fun updateGfxConfig(update: (GfxConfig) -> GfxConfig) {
        val updated = update(_uiState.value.selectedGfxConfig)
        _uiState.value = _uiState.value.copy(selectedGfxConfig = updated)
    }

    fun applyGfxOnly() {
        val result = GfxFileManager.applyGfxConfig(
            context = context,
            config = _uiState.value.selectedGfxConfig,
            treeUri = _uiState.value.safTreeUri
        )
        _uiState.value = _uiState.value.copy(gfxApplyResult = result)
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

    /**
     * Executes the Turbo Launch Pipeline:
     * 1. Aggressively purges RAM
     * 2. Injects UE4 UserCustom.ini & Active.sav GFX presets
     * 3. Starts floating HUD overlay service
     * 4. Starts continuous background suppression
     * 5. Launches BGMI (com.pubg.imobile / SplashActivity)
     */
    fun launchBgmiWithTurbo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaningInProgress = true)

            // 1. RAM Purge
            val cleanResult = ramOptimizer.purgeMemory(BGMI_PACKAGE_NAME)

            // 2. Apply GFX Configuration
            val gfxResult = GfxFileManager.applyGfxConfig(
                context = context,
                config = _uiState.value.selectedGfxConfig,
                treeUri = _uiState.value.safTreeUri
            )

            _uiState.value = _uiState.value.copy(
                lastCleanResult = cleanResult,
                gfxApplyResult = gfxResult,
                isCleaningInProgress = false
            )

            // 3. Start Session Tracker
            sessionTracker.startSession(BGMI_PACKAGE_NAME, cleanResult.freedMb)

            // 4. Start HUD if permission granted
            if (PermissionHelper.hasOverlayPermission(context)) {
                HudOverlayService.start(context)
                _uiState.value = _uiState.value.copy(isHudActive = true)
            }

            // 5. Start continuous suppression
            if (_uiState.value.isContinuousSuppressionEnabled) {
                ramOptimizer.startContinuousSuppression(viewModelScope, BGMI_PACKAGE_NAME)
            }

            // 6. Launch BGMI
            val launchIntent = context.packageManager.getLaunchIntentForPackage(BGMI_PACKAGE_NAME)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        }
    }

    fun toggleContinuousSuppression(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isContinuousSuppressionEnabled = enabled)
        viewModelScope.launch {
            settingsRepository.setContinuousSuppression(enabled)
        }
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
        viewModelScope.launch {
            settingsRepository.setAutoHud(enabled)
        }
        if (enabled && PermissionHelper.hasUsageStatsPermission(context)) {
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
        viewModelScope.launch {
            settingsRepository.updateTrackedGames(current)
        }
    }

    fun dismissSessionSummary() {
        _uiState.value = _uiState.value.copy(lastCompletedSession = null)
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
                installedApps = games
            )

            // If persisted trackedGamePackages in DataStore is empty, seed with first 3 discovered games
            if (_uiState.value.trackedGamePackages.isEmpty() && games.isNotEmpty()) {
                val seedPackages = games.take(3).map { it.packageName }.toSet()
                settingsRepository.updateTrackedGames(seedPackages)
            }
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
