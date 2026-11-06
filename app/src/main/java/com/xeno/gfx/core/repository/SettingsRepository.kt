package com.xeno.gfx.core.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xeno.gfx.core.model.UserSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "xeno_gfx_settings")

/**
 * Application repository managing transactional, non-blocking persistence of user preferences
 * and HUD configurations via Jetpack Preferences DataStore.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    object SettingsKeys {
        val KEY_CONTINUOUS_SUPPRESSION = booleanPreferencesKey("continuous_suppression_enabled")
        val KEY_AUTO_HUD = booleanPreferencesKey("auto_hud_enabled")
        val KEY_HUD_X = intPreferencesKey("hud_coordinate_x")
        val KEY_HUD_Y = intPreferencesKey("hud_coordinate_y")
        val KEY_TRACKED_GAMES = stringSetPreferencesKey("tracked_game_packages")
        val KEY_CUSTOM_WHITELIST = stringSetPreferencesKey("user_custom_whitelist")
        val KEY_COMPACT_HUD_MODE = booleanPreferencesKey("compact_hud_mode")
    }

    /**
     * Reactive stream of current [UserSettings]. Emits fallback defaults on fresh installs
     * or when catching an [IOException] during disk read operations.
     */
    val userSettings: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                isContinuousSuppressionEnabled = preferences[SettingsKeys.KEY_CONTINUOUS_SUPPRESSION] ?: true,
                isAutoHudEnabled = preferences[SettingsKeys.KEY_AUTO_HUD] ?: false,
                hudCoordinates = Pair(
                    preferences[SettingsKeys.KEY_HUD_X] ?: 20,
                    preferences[SettingsKeys.KEY_HUD_Y] ?: 100
                ),
                trackedGamePackages = preferences[SettingsKeys.KEY_TRACKED_GAMES] ?: emptySet(),
                userCustomWhitelist = preferences[SettingsKeys.KEY_CUSTOM_WHITELIST] ?: emptySet(),
                isCompactHudMode = preferences[SettingsKeys.KEY_COMPACT_HUD_MODE] ?: false
            )
        }
        .flowOn(ioDispatcher)

    /**
     * Updates the continuous background RAM suppression toggle.
     */
    suspend fun setContinuousSuppression(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.KEY_CONTINUOUS_SUPPRESSION] = enabled
        }
    }

    /**
     * Updates the automatic floating HUD game detection trigger toggle.
     */
    suspend fun setAutoHud(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.KEY_AUTO_HUD] = enabled
        }
    }

    /**
     * Updates the persisted floating HUD on-screen (x, y) coordinates.
     */
    suspend fun saveHudCoordinates(x: Int, y: Int) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.KEY_HUD_X] = x
            preferences[SettingsKeys.KEY_HUD_Y] = y
        }
    }

    /**
     * Replaces the set of package names tracked for automatic HUD overlay triggers.
     */
    suspend fun updateTrackedGames(packages: Set<String>) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.KEY_TRACKED_GAMES] = packages
        }
    }

    /**
     * Replaces the set of user-whitelisted package names excluded from background suppression.
     */
    suspend fun updateCustomWhitelist(packages: Set<String>) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.KEY_CUSTOM_WHITELIST] = packages
        }
    }

    /**
     * Updates the HUD display mode toggle (full diagnostic pill vs minimalist compact badge).
     */
    suspend fun setCompactHudMode(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.KEY_COMPACT_HUD_MODE] = enabled
        }
    }

    /**
     * Resets all stored preferences to defaults.
     */
    suspend fun resetToDefaults() = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(
                    dataStore = context.applicationContext.settingsDataStore
                ).also { instance = it }
            }
        }

        fun resetInstanceForTesting() {
            instance = null
        }
    }
}
