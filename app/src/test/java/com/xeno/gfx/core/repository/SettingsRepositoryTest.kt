package com.xeno.gfx.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.xeno.gfx.core.model.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createTestRepository(testScope: TestScope): SettingsRepository {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { tempFolder.newFile("test_settings_${System.nanoTime()}.preferences_pb") }
        )
        return SettingsRepository(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `test default settings emitted on fresh datastore`() = runTest {
        val repository = createTestRepository(this)
        val settings = repository.userSettings.first()

        assertTrue("Continuous suppression should be enabled by default per D-04", settings.isContinuousSuppressionEnabled)
        assertFalse("Auto HUD should be disabled by default per D-04", settings.isAutoHudEnabled)
        assertEquals("Default HUD coordinates should be (20, 100) per D-04", Pair(20, 100), settings.hudCoordinates)
        assertTrue("Tracked games should be empty by default", settings.trackedGamePackages.isEmpty())
        assertTrue("User custom whitelist should be empty by default", settings.userCustomWhitelist.isEmpty())
        assertFalse("Compact HUD mode should be false by default", settings.isCompactHudMode)
    }

    @Test
    fun `test hud coordinates persistence`() = runTest {
        val repository = createTestRepository(this)

        repository.saveHudCoordinates(75, 220)
        val updated = repository.userSettings.first()

        assertEquals(Pair(75, 220), updated.hudCoordinates)
    }

    @Test
    fun `test continuous suppression toggle persistence`() = runTest {
        val repository = createTestRepository(this)

        repository.setContinuousSuppression(false)
        var settings = repository.userSettings.first()
        assertFalse(settings.isContinuousSuppressionEnabled)

        repository.setContinuousSuppression(true)
        settings = repository.userSettings.first()
        assertTrue(settings.isContinuousSuppressionEnabled)
    }

    @Test
    fun `test auto hud toggle persistence`() = runTest {
        val repository = createTestRepository(this)

        repository.setAutoHud(true)
        val settings = repository.userSettings.first()
        assertTrue(settings.isAutoHudEnabled)
    }

    @Test
    fun `test tracked games set atomic persistence`() = runTest {
        val repository = createTestRepository(this)
        val targetPackages = setOf("com.pubg.imobile", "com.dts.freefireth")

        repository.updateTrackedGames(targetPackages)
        val settings = repository.userSettings.first()

        assertEquals(targetPackages, settings.trackedGamePackages)
    }

    @Test
    fun `test user custom whitelist atomic persistence`() = runTest {
        val repository = createTestRepository(this)
        val whitelistPackages = setOf("com.discord", "org.telegram.messenger")

        repository.updateCustomWhitelist(whitelistPackages)
        val settings = repository.userSettings.first()

        assertEquals(whitelistPackages, settings.userCustomWhitelist)
    }

    @Test
    fun `test compact hud mode toggle persistence`() = runTest {
        val repository = createTestRepository(this)

        repository.setCompactHudMode(true)
        val settings = repository.userSettings.first()
        assertTrue(settings.isCompactHudMode)
    }

    @Test
    fun `test reset to defaults restores initial values`() = runTest {
        val repository = createTestRepository(this)

        repository.saveHudCoordinates(300, 400)
        repository.setContinuousSuppression(false)
        repository.updateTrackedGames(setOf("com.test.game"))

        repository.resetToDefaults()
        val settings = repository.userSettings.first()

        assertEquals(Pair(20, 100), settings.hudCoordinates)
        assertTrue(settings.isContinuousSuppressionEnabled)
        assertTrue(settings.trackedGamePackages.isEmpty())
    }

    @Test
    fun `test io exception gracefully falls back to default settings`() = runTest {
        val failingDataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow {
                throw IOException("Simulated disk read failure")
            }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                throw IOException("Simulated disk write failure")
            }
        }

        val repository = SettingsRepository(
            dataStore = failingDataStore,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val settings = repository.userSettings.first()
        assertEquals(UserSettings(), settings)
    }
}
