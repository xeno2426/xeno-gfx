package com.xeno.gfx.core.repository

import android.content.Context
import com.xeno.gfx.core.model.DeviceMetrics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    // Minimal mock Context for unit test execution
    private val mockContext: Context = object : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getSystemService(name: String): Any? = null
    }

    @Test
    fun `test latency probe emits elapsed milliseconds when probe succeeds`() = runTest(testDispatcher) {
        val repository = TelemetryRepository(
            context = mockContext,
            scope = backgroundScope,
            ioDispatcher = testDispatcher,
            pingProbe = { 42L }
        )

        val emissions = mutableListOf<Long?>()
        val job = backgroundScope.launch {
            repository.latencyFlow.take(2).toList(emissions)
        }

        advanceTimeBy(100L)
        runCurrent()

        assertEquals(2, emissions.size)
        assertNull(emissions[0])
        assertEquals(42L, emissions[1])
    }

    @Test
    fun `test latency probe emits null on timeout or network error`() = runTest(testDispatcher) {
        var callCount = 0
        val repository = TelemetryRepository(
            context = mockContext,
            scope = backgroundScope,
            ioDispatcher = testDispatcher,
            pingProbe = {
                callCount++
                if (callCount == 1) {
                    throw SocketTimeoutException("Connect timed out")
                } else {
                    throw IOException("Network unreachable")
                }
            }
        )

        val emissions = mutableListOf<Long?>()
        val job = backgroundScope.launch {
            repository.latencyFlow.take(2).toList(emissions)
        }

        advanceTimeBy(100L)
        runCurrent()

        assertEquals(2, emissions.size)
        assertNull(emissions[0])
        assertNull(emissions[1])
    }

    @Test
    fun `test hardware and latency flow combination updates DeviceMetrics`() = runTest(testDispatcher) {
        val repository = TelemetryRepository(
            context = mockContext,
            scope = backgroundScope,
            ioDispatcher = testDispatcher,
            pingProbe = { 35L }
        )

        val collected = mutableListOf<DeviceMetrics>()
        backgroundScope.launch {
            repository.metrics.collect {
                collected.add(it)
            }
        }

        advanceTimeBy(1100L)
        runCurrent()

        assertTrue("Metrics should be collected once subscribed", collected.isNotEmpty())
        val latest = collected.last()
        assertEquals(30, latest.calibratedFps)
        assertEquals(60, latest.rawRefreshRate)
    }
}
