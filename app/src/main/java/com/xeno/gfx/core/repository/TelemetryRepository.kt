package com.xeno.gfx.core.repository

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
import com.xeno.gfx.core.stats.FpsSampler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Application-scoped singleton repository providing unified, demand-driven hardware
 * and network telemetry StateFlow streams per D-04 and D-05.
 *
 * Implements:
 * - D-04: Single consolidated repository eliminating dual independent polling loops.
 * - D-05: Demand-driven lifecycle via WhileSubscribed(5000L). Shuts down sensor polling
 *         and Choreographer callbacks when no active subscribers exist (<1% CPU).
 * - D-07: Decoupled 5000ms asynchronous socket latency probing to Google DNS (8.8.8.8:53)
 *         with 500ms timeout on Dispatchers.IO.
 * - D-08: Graceful fallback to null ("-- ms") on network timeout or offline state.
 */
class TelemetryRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pingProbe: (suspend () -> Long?)? = null,
    private val fpsSamplerProvider: ((onMetrics: (Int, Int) -> Unit) -> FpsSampler)? = null
) {
    private val appContext = context.applicationContext ?: context
    private val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    @Volatile
    private var latestFps: Int = 30

    @Volatile
    private var latestRefreshRate: Int = 60

    private val fpsSampler: FpsSampler = fpsSamplerProvider?.invoke { fps, rate ->
        latestFps = fps
        latestRefreshRate = rate
    } ?: FpsSampler(maxRefreshRate = 60) { fps, rate ->
        latestFps = fps
        latestRefreshRate = rate
    }

    /**
     * Decoupled 5000ms network latency probe flow on Dispatchers.IO per D-07.
     */
    val latencyFlow: Flow<Long?> = flow {
        // Emit initial null to avoid blocking hardware telemetry while waiting for first ping
        emit(null)
        while (currentCoroutineContext().isActive) {
            val latency = try {
                if (pingProbe != null) {
                    pingProbe.invoke()
                } else {
                    probeSocketLatency()
                }
            } catch (_: SocketTimeoutException) {
                null
            } catch (_: IOException) {
                null
            } catch (_: Exception) {
                null
            }
            emit(latency)
            delay(5000L)
        }
    }.flowOn(ioDispatcher)

    /**
     * 1000ms hardware telemetry flow on Dispatchers.IO per D-05.
     */
    val hardwareFlow: Flow<DeviceMetrics> = flow {
        while (currentCoroutineContext().isActive) {
            val (ramUsedMb, ramTotalMb, ramPercent) = sampleRam()
            val (batteryLevel, batteryTemp, isCharging) = sampleBattery()
            val refreshRate = sampleRefreshRate()
            val thermalStatus = sampleThermalStatus()

            fpsSampler.setMaxRefreshRate(refreshRate)

            val snapshot = DeviceMetrics(
                ramUsedMb = ramUsedMb,
                ramTotalMb = ramTotalMb,
                ramPercent = ramPercent,
                batteryLevel = batteryLevel,
                batteryTempCelsius = batteryTemp,
                isCharging = isCharging,
                thermalStatus = thermalStatus,
                calibratedFps = latestFps,
                rawRefreshRate = latestRefreshRate.coerceAtLeast(refreshRate),
                latencyMs = null
            )
            emit(snapshot)
            delay(1000L)
        }
    }.onStart {
        fpsSampler.start()
    }.onCompletion {
        fpsSampler.stop()
    }.flowOn(ioDispatcher)

    /**
     * Consolidated demand-driven StateFlow combining hardware telemetry and decoupled ping per D-04 & D-05.
     */
    val metrics: StateFlow<DeviceMetrics> = hardwareFlow
        .combine(latencyFlow) { hardware, latency ->
            hardware.copy(
                latencyMs = latency,
                networkLatencyMs = latency
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
            initialValue = DeviceMetrics()
        )

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
        val batteryIntent: Intent? = try {
            appContext.registerReceiver(null, intentFilter)
        } catch (_: Exception) {
            null
        }

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
        val tempCelsius = (rawTemp / 10.0f).coerceIn(0.0f, 80.0f)

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

    private fun probeSocketLatency(): Long? {
        val startTime = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 500)
            }
            System.currentTimeMillis() - startTime
        } catch (_: SocketTimeoutException) {
            null
        } catch (_: IOException) {
            null
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

    companion object {
        @Volatile
        private var instance: TelemetryRepository? = null

        fun getInstance(context: Context): TelemetryRepository {
            return instance ?: synchronized(this) {
                instance ?: TelemetryRepository(context.applicationContext ?: context).also {
                    instance = it
                }
            }
        }
    }
}
