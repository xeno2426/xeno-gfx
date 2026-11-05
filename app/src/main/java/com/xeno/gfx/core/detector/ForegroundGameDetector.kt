package com.xeno.gfx.core.detector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Detects when a tracked game enters or exits the foreground by polling UsageEvents.
 */
class ForegroundGameDetector(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private var pollingJob: Job? = null
    private var lastQueryTimestamp: Long = 0L
    private var currentForegroundPackage: String? = null

    // Set of package names to monitor
    private val trackedPackages = mutableSetOf<String>()

    // Listener callbacks
    var onGameLaunched: ((packageName: String) -> Unit)? = null
    var onGameExited: ((packageName: String) -> Unit)? = null

    fun setTrackedGames(packageNames: Set<String>) {
        synchronized(trackedPackages) {
            trackedPackages.clear()
            trackedPackages.addAll(packageNames)
        }
    }

    fun addTrackedGame(packageName: String) {
        synchronized(trackedPackages) {
            trackedPackages.add(packageName)
        }
    }

    fun removeTrackedGame(packageName: String) {
        synchronized(trackedPackages) {
            trackedPackages.remove(packageName)
        }
    }

    fun startMonitoring(scope: CoroutineScope, pollIntervalMs: Long = 1500L) {
        stopMonitoring()
        lastQueryTimestamp = System.currentTimeMillis() - 10000L

        pollingJob = scope.launch(ioDispatcher) {
            while (isActive) {
                checkForegroundApp()
                delay(pollIntervalMs)
            }
        }
    }

    fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
        currentForegroundPackage = null
    }

    private suspend fun checkForegroundApp() = withContext(ioDispatcher) {
        if (usageStatsManager == null) return@withContext

        val now = System.currentTimeMillis()
        val events: UsageEvents? = try {
            usageStatsManager.queryEvents(lastQueryTimestamp, now)
        } catch (_: SecurityException) {
            null
        }

        if (events == null) return@withContext

        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = lastQueryTimestamp

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestPackage = event.packageName
                if (event.timeStamp > latestTimestamp) {
                    latestTimestamp = event.timeStamp
                }
            }
        }

        lastQueryTimestamp = latestTimestamp

        if (latestPackage != null && latestPackage != currentForegroundPackage) {
            val previousPackage = currentForegroundPackage
            currentForegroundPackage = latestPackage

            val isTrackedNow: Boolean
            val wasTrackedPrevious: Boolean
            synchronized(trackedPackages) {
                isTrackedNow = trackedPackages.contains(latestPackage)
                wasTrackedPrevious = previousPackage != null && trackedPackages.contains(previousPackage)
            }

            if (wasTrackedPrevious && previousPackage != null) {
                onGameExited?.invoke(previousPackage)
            }
            if (isTrackedNow) {
                onGameLaunched?.invoke(latestPackage)
            }
        }
    }
}
