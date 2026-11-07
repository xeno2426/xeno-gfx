package com.xeno.gfx.core.stats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class GameSessionStats(
    val gamePackage: String,
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long? = null,
    val durationSeconds: Long = 0,
    val averageFps: Float = 0f,
    val minFps: Int = 0,
    val maxFps: Int = 0,
    val stutterCount: Int = 0,
    val peakTempCelsius: Float = 0f,
    val ramFreedMb: Long = 0
)

/**
 * Tracks real-time telemetry metrics during an active gaming session, inspired by
 * Game Booster Plus session analytics architecture.
 */
class SessionTracker {

    private val fpsSamples = mutableListOf<Int>()
    private var peakTemp: Float = 0f
    private var initialRamFreed: Long = 0
    private var currentGamePackage: String = ""
    private var sessionStartTime: Long = 0
    private var stutters: Int = 0

    private val _currentSession = MutableStateFlow<GameSessionStats?>(null)
    val currentSession: StateFlow<GameSessionStats?> = _currentSession.asStateFlow()

    private val _lastCompletedSession = MutableStateFlow<GameSessionStats?>(null)
    val lastCompletedSession: StateFlow<GameSessionStats?> = _lastCompletedSession.asStateFlow()

    fun startSession(gamePackage: String, ramFreedMb: Long = 0) {
        synchronized(this) {
            currentGamePackage = gamePackage
            sessionStartTime = System.currentTimeMillis()
            initialRamFreed = ramFreedMb
            peakTemp = 0f
            stutters = 0
            fpsSamples.clear()

            _currentSession.value = GameSessionStats(
                gamePackage = gamePackage,
                startTimeMs = sessionStartTime,
                ramFreedMb = ramFreedMb
            )
        }
    }

    fun recordMetrics(fps: Int, tempCelsius: Float, targetFps: Int = 30) {
        synchronized(this) {
            if (_currentSession.value == null) return

            if (fps > 0) {
                fpsSamples.add(fps)
                if (fps < (targetFps - 3)) {
                    stutters++
                }
            }

            if (tempCelsius > peakTemp) {
                peakTemp = tempCelsius
            }

            val elapsedSec = (System.currentTimeMillis() - sessionStartTime) / 1000
            val avg = if (fpsSamples.isNotEmpty()) fpsSamples.average().toFloat() else 0f
            val min = fpsSamples.minOrNull() ?: 0
            val max = fpsSamples.maxOrNull() ?: 0

            _currentSession.value = GameSessionStats(
                gamePackage = currentGamePackage,
                startTimeMs = sessionStartTime,
                durationSeconds = elapsedSec,
                averageFps = avg,
                minFps = min,
                maxFps = max,
                stutterCount = stutters,
                peakTempCelsius = peakTemp,
                ramFreedMb = initialRamFreed
            )
        }
    }

    fun stopSession(): GameSessionStats? {
        synchronized(this) {
            val session = _currentSession.value ?: return null
            val now = System.currentTimeMillis()
            val totalSeconds = (now - sessionStartTime) / 1000
            val avg = if (fpsSamples.isNotEmpty()) fpsSamples.average().toFloat() else 0f
            val min = fpsSamples.minOrNull() ?: 0
            val max = fpsSamples.maxOrNull() ?: 0

            val completed = session.copy(
                endTimeMs = now,
                durationSeconds = totalSeconds,
                averageFps = avg,
                minFps = min,
                maxFps = max,
                stutterCount = stutters,
                peakTempCelsius = peakTemp
            )

            _lastCompletedSession.value = completed
            _currentSession.value = null
            return completed
        }
    }
}
