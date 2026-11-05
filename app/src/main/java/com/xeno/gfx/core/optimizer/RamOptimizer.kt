package com.xeno.gfx.core.optimizer

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CleanResult(
    val beforeAvailMb: Long,
    val afterAvailMb: Long,
    val freedMb: Long,
    val terminatedAppsCount: Int
)

/**
 * High-performance RAM optimizer designed to maximize available memory for gaming.
 */
class RamOptimizer(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val packageManager = context.packageManager

    private var suppressionJob: Job? = null

    private val _isSuppressionActive = MutableStateFlow(false)
    val isSuppressionActive: StateFlow<Boolean> = _isSuppressionActive.asStateFlow()

    private val _lastCleanResult = MutableStateFlow<CleanResult?>(null)
    val lastCleanResult: StateFlow<CleanResult?> = _lastCleanResult.asStateFlow()

    /**
     * Executes a deep RAM purge, evicting non-whitelisted background/cached processes.
     */
    suspend fun purgeMemory(
        targetGamePackage: String = "com.pubg.imobile",
        userWhitelist: Set<String> = emptySet()
    ): CleanResult = withContext(ioDispatcher) {
        val beforeAvailMb = getAvailableMemoryMb()

        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList<ApplicationInfo>()
        }

        var terminatedCount = 0

        for (app in installedApps) {
            val pkg = app.packageName
            if (!AppWhitelist.isProtected(pkg, context, targetGamePackage, userWhitelist)) {
                try {
                    activityManager?.killBackgroundProcesses(pkg)
                    terminatedCount++
                } catch (_: SecurityException) {
                    // Ignored if permission revoked
                }
            }
        }

        // Suggest garbage collection to reclaim local heap
        System.gc()

        // Wait brief moment for kernel lmkd / page cache to settle
        delay(200)

        val afterAvailMb = getAvailableMemoryMb()
        val freedMb = (afterAvailMb - beforeAvailMb).coerceAtLeast(0L)

        val result = CleanResult(
            beforeAvailMb = beforeAvailMb,
            afterAvailMb = afterAvailMb,
            freedMb = freedMb,
            terminatedAppsCount = terminatedCount
        )

        _lastCleanResult.value = result
        result
    }

    /**
     * Starts a continuous background suppression loop (default every 30s) while the game is active.
     */
    fun startContinuousSuppression(
        scope: CoroutineScope,
        targetGamePackage: String = "com.pubg.imobile",
        userWhitelist: Set<String> = emptySet(),
        intervalMs: Long = 30000L
    ) {
        stopContinuousSuppression()
        _isSuppressionActive.value = true

        suppressionJob = scope.launch(ioDispatcher) {
            while (isActive) {
                delay(intervalMs)
                if (isActive) {
                    purgeMemory(targetGamePackage, userWhitelist)
                }
            }
        }
    }

    /**
     * Stops continuous suppression when the game exits.
     */
    fun stopContinuousSuppression() {
        suppressionJob?.cancel()
        suppressionJob = null
        _isSuppressionActive.value = false
    }

    fun getAvailableMemoryMb(): Long {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    fun getTotalMemoryMb(): Long {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }
}
