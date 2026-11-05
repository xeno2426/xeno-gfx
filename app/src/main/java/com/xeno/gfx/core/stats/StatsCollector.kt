package com.xeno.gfx.core.stats

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import com.xeno.gfx.core.model.DeviceMetrics
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
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Periodically samples device hardware metrics and network latency.
 */
class StatsCollector(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val _metrics = MutableStateFlow(DeviceMetrics())
    val metrics: StateFlow<DeviceMetrics> = _metrics.asStateFlow()

    private var samplingJob: Job? = null
    private var latestFps: Int = 60

    private val fpsSampler = FpsSampler(maxRefreshRate = 60) { measuredFps ->
        latestFps = measuredFps
        _metrics.value = _metrics.value.copy(currentFps = measuredFps)
    }

    fun startSampling(scope: CoroutineScope, intervalMs: Long = 1000L) {
        stopSampling()
        fpsSampler.start()
        samplingJob = scope.launch(ioDispatcher) {
            while (isActive) {
                val snapshot = sampleMetrics()
                _metrics.value = snapshot
                delay(intervalMs)
            }
        }
    }

    fun stopSampling() {
        fpsSampler.stop()
        samplingJob?.cancel()
        samplingJob = null
    }

    suspend fun sampleMetrics(): DeviceMetrics = withContext(ioDispatcher) {
        val (ramUsedMb, ramTotalMb, ramPercent) = sampleRam()
        val (batteryLevel, batteryTemp, isCharging) = sampleBattery()
        val refreshRate = sampleRefreshRate()
        val latency = sampleNetworkLatency()
        val thermalStatus = sampleThermalStatus()

        fpsSampler.setMaxRefreshRate(refreshRate)

        DeviceMetrics(
            ramUsedMb = ramUsedMb,
            ramTotalMb = ramTotalMb,
            ramPercent = ramPercent,
            batteryLevel = batteryLevel,
            batteryTempCelsius = batteryTemp,
            isCharging = isCharging,
            refreshRateHz = refreshRate,
            currentFps = latestFps,
            networkLatencyMs = latency,
            thermalStatus = thermalStatus
        )
    }

    private fun sampleRam(): Triple<Long, Long, Int> {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        val availableMb = memoryInfo.availMem / (1024 * 1024)
        val usedMb = (totalMb - availableMb).coerceAtLeast(0L)
        val percent = if (totalMb > 0) ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt() else 0

        return Triple(usedMb, totalMb, percent)
    }

    private fun sampleBattery(): Triple<Int, Float, Boolean> {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent: Intent? = context.registerReceiver(null, intentFilter)

        if (batteryIntent == null) {
            return Triple(-1, 0.0f, false)
        }

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPercent = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt()
        } else {
            -1
        }

        val rawTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = rawTemp / 10.0f

        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return Triple(batteryPercent, tempCelsius, isCharging)
    }

    private fun sampleRefreshRate(): Int {
        return try {
            val defaultDisplay = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            val rate = defaultDisplay?.refreshRate ?: 60f
            rate.toInt().coerceAtLeast(30)
        } catch (_: Exception) {
            60
        }
    }

    private fun sampleNetworkLatency(): Long? {
        return try {
            val startTime = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 500)
            }
            System.currentTimeMillis() - startTime
        } catch (_: Exception) {
            null
        }
    }

    private fun sampleThermalStatus(): DeviceMetrics.ThermalStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            return when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> DeviceMetrics.ThermalStatus.NORMAL
                PowerManager.THERMAL_STATUS_LIGHT -> DeviceMetrics.ThermalStatus.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> DeviceMetrics.ThermalStatus.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> DeviceMetrics.ThermalStatus.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL -> DeviceMetrics.ThermalStatus.CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY -> DeviceMetrics.ThermalStatus.EMERGENCY
                PowerManager.THERMAL_STATUS_SHUTDOWN -> DeviceMetrics.ThermalStatus.SHUTDOWN
                else -> DeviceMetrics.ThermalStatus.UNKNOWN
            }
        }
        return DeviceMetrics.ThermalStatus.NORMAL
    }
}
