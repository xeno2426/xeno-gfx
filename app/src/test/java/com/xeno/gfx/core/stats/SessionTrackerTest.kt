package com.xeno.gfx.core.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTrackerTest {

    @Test
    fun `test session lifecycle records metrics and calculates averages correctly`() {
        val tracker = SessionTracker()
        assertNull(tracker.currentSession.value)

        // 1. Start Session
        tracker.startSession("com.pubg.imobile", ramFreedMb = 250L)
        val session1 = tracker.currentSession.value
        assertNotNull(session1)
        assertEquals("com.pubg.imobile", session1?.gamePackage)
        assertEquals(250L, session1?.ramFreedMb)

        // 2. Record telemetry samples
        tracker.recordMetrics(fps = 30, tempCelsius = 36.5f, targetFps = 30)
        tracker.recordMetrics(fps = 29, tempCelsius = 37.0f, targetFps = 30)
        tracker.recordMetrics(fps = 25, tempCelsius = 38.2f, targetFps = 30) // Stutter (25 < 30-3=27)

        val updated = tracker.currentSession.value
        assertNotNull(updated)
        assertEquals(38.2f, updated!!.peakTempCelsius, 0.01f)
        assertEquals(25, updated.minFps)
        assertEquals(30, updated.maxFps)
        assertEquals(1, updated.stutterCount)
        assertEquals(28.0f, updated.averageFps, 0.1f)

        // 3. Stop Session
        val completed = tracker.stopSession()
        assertNotNull(completed)
        assertNull(tracker.currentSession.value)
        assertEquals(completed, tracker.lastCompletedSession.value)
        assertEquals("com.pubg.imobile", completed?.gamePackage)
    }
}
