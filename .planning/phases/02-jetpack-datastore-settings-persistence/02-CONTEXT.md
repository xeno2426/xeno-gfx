# Phase 2: Jetpack DataStore Settings Persistence - Context

**Gathered:** 2026-11-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Provide transactional, non-blocking disk persistence for user configurations, floating HUD coordinates, display states, and turbo preferences across application restarts and device reboots via Jetpack Preferences DataStore.

Delivers requirement:
- **GFX-01**: User settings (tracked games, whitelist, HUD mode, coordinates, turbo toggles) persist across app closes and device reboots via Jetpack Preferences DataStore.
</domain>

<decisions>
## Implementation Decisions

### Storage Schema & Key Contracts
- **D-01:** Back persistent storage with Jetpack Preferences DataStore (`androidx.datastore:datastore-preferences:1.1.3`). Lightweight, no protobuf schema compilation, zero SQLite overhead, minimal memory footprint (<2MB).
- **D-02:** Use native `stringSetPreferencesKey` for set collections (`tracked_game_packages` and `user_custom_whitelist`) ensuring atomic updates without manual serialization.
- **D-03:** Store floating HUD on-screen coordinates (`KEY_HUD_X`, `KEY_HUD_Y`) on touch release (`MotionEvent.ACTION_UP`) asynchronously on `Dispatchers.IO` to eliminate I/O overhead during active dragging.
- **D-04:** Default settings state on fresh install: continuous suppression enabled (`true`), auto-HUD trigger disabled (`false` until usage permissions granted), HUD position `(x=20, y=100)`, empty custom whitelist.

### Reactive State Flow & Presentation Binding
- **D-05:** Encapsulate persistence in an application-scoped singleton `SettingsRepository.getInstance(context)` providing immutable `UserSettings` snapshot data models.
- **D-06:** Expose reactive `val userSettings: Flow<UserSettings>` to `DashboardViewModel` and `HudOverlayService`, updating UI state reactively without manual reload calls.
- **D-07:** Resilient error handling: Catch `IOException` during DataStore disk reads and fall back gracefully to `emptyPreferences()` per Android best practices.
- **D-08:** Execute all disk mutations asynchronously on `Dispatchers.IO` via non-blocking suspending functions (`updateTrackedGames`, `setContinuousSuppression`, `saveHudCoordinates`, `resetToDefaults`).

### HUD Coordinate Integration & Bounds Safety
- **D-09:** Clamp restored and saved HUD coordinates to physical display bounds (`0 <= x <= screenWidth - hudWidth`, `0 <= y <= screenHeight - hudHeight`) to prevent clipping across orientation changes.
- **D-10:** Synchronize tracked package selections reactively with `ForegroundGameDetector` so game launch listeners immediately observe user modifications.

### the agent's Discretion
- Exact naming of preference keys within `SettingsKeys` companion object.
- Integration test harnessing for DataStore using temporary test preferences data files.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/xeno/gfx/core/model/HudConfig.kt`: Existing HUD configuration model to be integrated with persistent preferences.
- `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`: Currently manages in-memory `_uiState` (`isContinuousSuppressionEnabled`, `isAutoHudEnabled`, `trackedGamePackages`).
- `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`: Handles window drag coordinates in `setOnTouchListener` (`MotionEvent.ACTION_UP`).

### Established Patterns
- Application-scoped singleton repository pattern (`TelemetryRepository.getInstance(context)` established in Phase 1).
- Unidirectional Data Flow via Kotlin `Flow` and `StateFlow`.
- Safe exception handling with recovery fallbacks.

### Integration Points
- `gradle/libs.versions.toml` & `app/build.gradle.kts`: Add `androidx.datastore:datastore-preferences:1.1.3`.
- `DashboardViewModel.kt`: Bind `SettingsRepository.userSettings` directly into `DashboardUiState`.
- `HudOverlayService.kt`: Load initial HUD coordinates from `SettingsRepository` and persist final position on touch up.
</code_context>

<specifics>
## Specific Ideas

- Ensure DataStore reads and writes never block the Android Main thread, maintaining the project's zero-stutter mandate.
- All settings must survive complete OS force-stop and device reboot.
</specifics>

<deferred>
## Deferred Ideas

- Cloud settings synchronization or Google Drive backups (strictly 100% offline).
- Per-game custom graphics configuration profiles (Phase 5).
</deferred>
