# Phase 02: Jetpack DataStore Settings Persistence - Research

**Researched:** 2026-11-06  
**Domain:** Android Jetpack Preferences DataStore, Reactive State Streams, Thread-Safe Offline Persistence  
**Confidence:** HIGH  

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Back persistent storage with Jetpack Preferences DataStore (`androidx.datastore:datastore-preferences:1.1.3`). Lightweight, no protobuf schema compilation, zero SQLite overhead, minimal memory footprint (<2MB).
- **D-02:** Use native `stringSetPreferencesKey` for set collections (`tracked_game_packages` and `user_custom_whitelist`) ensuring atomic updates without manual serialization.
- **D-03:** Store floating HUD on-screen coordinates (`KEY_HUD_X`, `KEY_HUD_Y`) on touch release (`MotionEvent.ACTION_UP`) asynchronously on `Dispatchers.IO` to eliminate I/O overhead during active dragging.
- **D-04:** Default settings state on fresh install: continuous suppression enabled (`true`), auto-HUD trigger disabled (`false` until usage permissions granted), HUD position `(x=20, y=100)`, empty custom whitelist.
- **D-05:** Encapsulate persistence in an application-scoped singleton `SettingsRepository.getInstance(context)` providing immutable `UserSettings` snapshot data models.
- **D-06:** Expose reactive `val userSettings: Flow<UserSettings>` to `DashboardViewModel` and `HudOverlayService`, updating UI state reactively without manual reload calls.
- **D-07:** Resilient error handling: Catch `IOException` during DataStore disk reads and fall back gracefully to `emptyPreferences()` per Android best practices.
- **D-08:** Execute all disk mutations asynchronously on `Dispatchers.IO` via non-blocking suspending functions (`updateTrackedGames`, `setContinuousSuppression`, `saveHudCoordinates`, `resetToDefaults`).
- **D-09:** Clamp restored and saved HUD coordinates to physical display bounds (`0 <= x <= screenWidth - hudWidth`, `0 <= y <= screenHeight - hudHeight`) to prevent clipping across orientation changes.
- **D-10:** Synchronize tracked package selections reactively with `ForegroundGameDetector` so game launch listeners immediately observe user modifications.

### the agent's Discretion
- Exact naming of preference keys within `SettingsKeys` companion object.
- Integration test harnessing for DataStore using temporary test preferences data files.

### Deferred Ideas (OUT OF SCOPE)
- Cloud settings synchronization or Google Drive backups (strictly 100% offline).
- Per-game custom graphics configuration profiles (Phase 5).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|---|---|---|
| **GFX-01** | User settings (tracked games, whitelist, HUD mode, coordinates, turbo toggles) persist across app closes and device reboots via Jetpack Preferences DataStore. | Full architectural specification of `SettingsRepository`, `UserSettings` data model, `SettingsKeys`, `Context.settingsDataStore` extension, graceful `IOException` fallback, `ACTION_UP` asynchronous position persistence, display boundary clamping, and JVM unit test harness using `PreferenceDataStoreFactory.create`. |
</phase_requirements>

## Summary

Phase 02 transitions Xeno GFX from volatile in-memory state in `DashboardViewModel` (`_uiState`) and `HudOverlayService` into transactional, asynchronous disk persistence powered by Jetpack Preferences DataStore (`androidx.datastore:datastore-preferences:1.1.3` [VERIFIED: google maven / official docs]). DataStore completely eliminates the main-thread blocking (`fsync`) and thread-safety pitfalls inherent in legacy Android `SharedPreferences`, directly upholding the project's zero-stutter mandate on memory-constrained 3GB and 4GB RAM devices.

The persistence architecture centers around an application-scoped singleton, `SettingsRepository`, which manages the underlying `DataStore<Preferences>` file (`"xeno_gfx_settings.preferences_pb"`). The repository exposes an immutable, reactive `Flow<UserSettings>` that feeds directly into `DashboardViewModel` and `HudOverlayService`. Mutations—including toggling continuous RAM suppression, toggling auto-HUD on game launch, modifying tracked game package sets, adjusting the custom whitelist, and updating floating HUD coordinates—are executed exclusively on `Dispatchers.IO` via suspending functions. Active dragging in `HudOverlayService` remains 100% in-memory, committing coordinate updates strictly on `MotionEvent.ACTION_UP` after clamping against physical screen boundaries.

**Primary recommendation:** Integrate `androidx.datastore:datastore-preferences:1.1.3` via `gradle/libs.versions.toml`, implement `SettingsRepository` with an injectable `DataStore<Preferences>` constructor for instantaneous JVM unit testing via `PreferenceDataStoreFactory.create` and JUnit 4 `TemporaryFolder`, bind `SettingsRepository.userSettings` directly into `DashboardViewModel`'s state flow, and asynchronously commit clamped coordinates from `HudOverlayService` on touch release.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|---|---|---|---|
| **Preferences Disk Storage** | Storage / Data Layer (`SettingsRepository`) | Disk Filesystem (`Context.filesDir/datastore`) | Encapsulates all DataStore read/write operations, key definitions, and serialization behind an immutable interface. |
| **Settings Reactive State** | Domain / Repository Layer (`SettingsRepository`) | Presentation Layer (`DashboardViewModel`) | Exposes `Flow<UserSettings>` with `catch` error recovery, mapped cleanly to Compose UI state. |
| **Dashboard UI Binding** | Presentation Layer (`DashboardViewModel`) | Compose UI (`DashboardScreen`) | Binds `userSettings` into `DashboardUiState`, dispatching user toggle events asynchronously. |
| **HUD Coordinates Persistence** | Foreground Service (`HudOverlayService`) | Storage Layer (`SettingsRepository`) | Manages window layout params during active drag; commits clamped coordinates on `ACTION_UP` via coroutines on `Dispatchers.IO`. |
| **Tracked Games Sync** | Core Detector (`ForegroundGameDetector`) | Presentation Layer (`DashboardViewModel`) | Receives updated tracked package sets reactively so background polling immediately monitors newly selected titles. |
| **Whitelist Enforcement** | Core Optimizer (`AppWhitelist`) | Storage Layer (`SettingsRepository`) | Reads persisted `userCustomWhitelist` to exempt user-approved background utilities from aggressive eviction. |

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---|---|---|---|
| `androidx.datastore:datastore-preferences` | `1.1.3` [VERIFIED: google maven / official docs] | Key-value disk persistence using Kotlin Coroutines & Flow | Official Android Jetpack standard replacing `SharedPreferences`; transactional, non-blocking, asynchronous disk I/O, no runtime protobuf tooling required. |
| `androidx.datastore:datastore-preferences-core` | `1.1.3` [VERIFIED: google maven / official docs] | Core primitives, key types (`stringSetPreferencesKey`, `booleanPreferencesKey`), and factory APIs | Transitive dependency providing platform-agnostic preferences machinery and `PreferenceDataStoreFactory`. |

### Supporting
| Library | Version | Purpose | When to Use |
|---|---|---|---|
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `1.9.0` [VERIFIED: in-repo gradle/libs.versions.toml:9] | Suspending functions, coroutine builders, and Flow operators | Already integrated; manages async repository operations and thread dispatching. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.9.0` [VERIFIED: in-repo gradle/libs.versions.toml:9] | Android Looper dispatchers (`Dispatchers.Main`) | Ensures safe UI updates and lifecycle-aware coroutine execution. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.9.0` [VERIFIED: in-repo gradle/libs.versions.toml:9] | Test dispatchers (`StandardTestDispatcher`, `runTest`) | Unit testing `SettingsRepository` with virtual time advancement. |
| `junit:junit` | `4.13.2` [VERIFIED: in-repo gradle/libs.versions.toml:10] | Unit testing runner and rules (`TemporaryFolder`) | Isolating test DataStore files on the host JVM during `./gradlew testDebugUnitTest`. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|---|---|---|
| `datastore-preferences` | `SharedPreferences` | **Rejected:** `SharedPreferences.apply()` runs `fsync` on the Main Looper during Activity/Service pause, inducing UI micro-stutters. Lacks Flow reactivity and type safety. |
| `datastore-preferences` | `Room` / SQLite | **Rejected:** Extreme overhead (>15MB APK footprint, SQLite connection pools, schema migrations) for storing ~6 scalar key-value settings. Overkill for low-RAM devices. |
| `datastore-preferences` | Proto DataStore | **Rejected:** Requires Protobuf Gradle plugin (`com.google.protobuf`), `.proto` schema definitions, and code generation overhead for simple configuration toggles. |

**Installation:**
Add to `gradle/libs.versions.toml`:
```toml
[versions]
datastore = "1.1.3"

[libraries]
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```
Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.androidx.datastore.preferences)
}
```

---

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---|---|---|---|---|---|---|
| `androidx.datastore:datastore-preferences:1.1.3` | Google Maven / Maven Central | 4+ yrs (1.1.3 Feb 2025) | 100M+ installs | `https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:datastore/` | `[OK]` | Approved |
| `androidx.datastore:datastore-preferences-core:1.1.3` | Google Maven / Maven Central | 4+ yrs | Transitive core | `https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:datastore/` | `[OK]` | Approved |

**Packages removed due to [SLOP] verdict:** None.  
**Packages flagged as suspicious [SUS]:** None.  
*Both libraries are official Google AndroidX components verified on Google Maven (`dl.google.com/dl/android/maven2`).*

---

## Architecture Patterns

### System Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                                  Android System                                   |
|   App Starts / Cold Boot                  Device Screen (Touch Drag Events)       |
+-----------------------------------------------------------------------------------+
                  |                                           |
                  |                                           | (MotionEvent.ACTION_UP)
                  v                                           v
+------------------------------------+   +------------------------------------------+
|         DashboardViewModel         |   |            HudOverlayService             |
|   - Collects userSettings Flow     |   |   - Restores (x, y) on showOverlay()     |
|   - Mutates toggles on IO scope    |   |   - Clamps coordinates to display bounds |
|   - Updates DashboardUiState       |   |   - Invokes saveHudCoordinates() on UP   |
+------------------------------------+   +------------------------------------------+
                  |                                           |
                  | (Suspending Mutation Calls)               | (saveHudCoordinates)
                  +---------------------+---------------------+
                                        |
                                        v
+-----------------------------------------------------------------------------------+
|                        SettingsRepository (Singleton)                             |
|   - Exposes: val userSettings: Flow<UserSettings>                                 |
|   - Error handling: .catch { if (it is IOException) emit(emptyPreferences()) }    |
|   - Suspending functions: setContinuousSuppression(), setAutoHud(),               |
|     updateTrackedGames(), updateCustomWhitelist(), saveHudCoordinates()           |
+-----------------------------------------------------------------------------------+
                                        |
                                        | (DataStore.edit { ... } on Dispatchers.IO)
                                        v
+-----------------------------------------------------------------------------------+
|                  Jetpack Preferences DataStore (Single Process)                   |
|   File: /data/user/0/com.xeno.gfx/files/datastore/xeno_gfx_settings.preferences_pb|
+-----------------------------------------------------------------------------------+
                                        |
                                        | (Emits updated Preferences snapshot)
                                        v
                       [ Reactive Flow updates down to UI ]
```

### Recommended Project Structure
```
app/src/main/java/com/xeno/gfx/
├── core/
│   ├── model/
│   │   ├── DeviceMetrics.kt         # Hardware telemetry model
│   │   ├── HudConfig.kt             # Existing HUD configuration model
│   │   ├── TrackedGame.kt           # Game entity definition
│   │   └── UserSettings.kt          # [NEW] Immutable snapshot of persistent preferences
│   └── repository/
│       ├── TelemetryRepository.kt   # Phase 1 demand-driven telemetry
│       └── SettingsRepository.kt    # [NEW] Application-scoped DataStore persistence singleton
├── service/
│   └── HudOverlayService.kt         # Window overlay service, coordinate restoration & touch up persistence
└── ui/
    └── dashboard/
        ├── DashboardViewModel.kt    # Binds userSettings Flow into DashboardUiState
        └── DashboardScreen.kt       # Renders controls according to persisted state
```

### Pattern 1: Application-Scoped Singleton with Testable Constructor
**What:** Create `SettingsRepository` with an internal constructor accepting `DataStore<Preferences>`, and a companion `getInstance(context)` factory using `preferencesDataStore` property delegate.  
**When to use:** Ensures production code shares exactly one DataStore file handle while unit tests can instantiate `SettingsRepository` with isolated temporary files via `PreferenceDataStoreFactory.create`.  
**Example:**
```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/datastore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xeno_gfx_settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(
                    dataStore = (context.applicationContext ?: context).dataStore
                ).also { instance = it }
            }
        }
    }
}
```

### Pattern 2: Immutable Settings Snapshot Model
**What:** Expose a clean, immutable Kotlin data class `UserSettings` rather than leaking raw AndroidX `Preferences` or keys to ViewModel and Service layers.  
**When to use:** Protects presentation layers from DataStore implementation details and provides type safety with default values.  
**Example:**
```kotlin
data class UserSettings(
    val isContinuousSuppressionEnabled: Boolean = true,
    val isAutoHudEnabled: Boolean = false,
    val hudCoordinates: Pair<Int, Int> = Pair(20, 100),
    val trackedGamePackages: Set<String> = emptySet(),
    val userCustomWhitelist: Set<String> = emptySet(),
    val isCompactHudMode: Boolean = false
)
```

### Pattern 3: Resilient Stream Error Handling with Graceful Fallback
**What:** Intercept read `IOException` using Kotlin Flow `.catch` operator, emitting `emptyPreferences()` to enable safe defaults without crashing.  
**When to use:** Mandatory per Decision D-07 and official Android documentation. Prevents crashes from disk corruption or unreadable storage.  
**Example:**
```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/datastore#preferences-read
val userSettings: Flow<UserSettings> = dataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
    .map { preferences ->
        mapPreferencesToUserSettings(preferences)
    }
```

### Pattern 4: Atomic Touch-Up Coordinate Persistence with Boundary Clamping
**What:** Handle `MotionEvent.ACTION_MOVE` purely in-memory (`windowManager.updateViewLayout`), and persist the final clamped coordinates in `MotionEvent.ACTION_UP` via a background coroutine on `Dispatchers.IO`.  
**When to use:** Ensures 60/120Hz drag fluidity without disk I/O lag, and guarantees HUD stays fully visible on-screen regardless of device screen size or rotation.  
**Example:**
```kotlin
// Inside HudOverlayService onTouchListener
MotionEvent.ACTION_UP -> {
    // 1. Clamp to display bounds per D-09
    val displayMetrics = resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels
    val hudWidth = composeView.width.coerceAtLeast(100)
    val hudHeight = composeView.height.coerceAtLeast(100)

    val clampedX = p.x.coerceIn(0, screenWidth - hudWidth)
    val clampedY = p.y.coerceIn(0, screenHeight - hudHeight)

    if (clampedX != p.x || clampedY != p.y) {
        p.x = clampedX
        p.y = clampedY
        windowManager.updateViewLayout(composeView, p)
    }

    // 2. Persist asynchronously per D-03 & D-08
    serviceScope.launch(Dispatchers.IO) {
        settingsRepository.saveHudCoordinates(clampedX, clampedY)
    }
    true
}
```

### Anti-Patterns to Avoid
- **Calling `runBlocking` on DataStore on Android Main Looper:** Freezes the UI thread, introduces ANR risk, and violates the zero-stutter project mandate.
- **Instantiating Multiple DataStores for the Same File:** Calling `PreferenceDataStoreFactory.create` or `preferencesDataStore` multiple times with the same filename in the same process causes `IllegalStateException: There are multiple DataStores active for the same file`.
- **Saving Coordinates on `MotionEvent.ACTION_MOVE`:** Spawns dozens of disk writes per second while the user is actively dragging the overlay, destroying battery life and dropping frame pacing.
- **Directly Mutating `Preferences[setKey]`:** Attempting `preferences[KEY_TRACKED_GAMES].add("com.pkg")` fails because DataStore sets are immutable. A new `Set<String>` instance must be assigned.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---|---|---|---|
| Key-value persistence | Custom JSON serialization to file | `androidx.datastore:datastore-preferences` | Custom file serialization lacks atomic file replacement, transactional concurrency locks, and memory caching, leading to race conditions and truncated files during unexpected process termination. |
| Settings change observation | Custom listener callbacks or broadcast intents | Kotlin `Flow<UserSettings>` | Flow natively supports thread switching, backpressure handling, lifecycle cancellation, and unidirectional data flow. |
| Set serialization | Manual CSV or delimiter string splitting | `stringSetPreferencesKey` | Delimiter parsing breaks on package names containing special characters and requires manual escaping. `stringSetPreferencesKey` is natively serialized via Protocol Buffers. |
| Screen bounds calculation | Hardcoded 1080x2400 screen bounds | `DisplayMetrics` / `WindowMetrics` clamping | Device display resolutions vary wildly from 720p budget handsets to foldable screens. Clamping must derive dynamically from system metrics. |

---

## Runtime State Inventory

> Greenfield migration: Volatile in-memory ViewModel defaults are migrated to persistent disk storage.

| Category | Items Found | Action Required |
|---|---|---|
| **Stored data** | None — fresh DataStore file `"xeno_gfx_settings.preferences_pb"` will be created on first write in app's private files directory. | Initial read falls back to default values specified in Decision D-04. |
| **Live service config** | None. | N/A — local on-device settings only. |
| **OS-registered state** | None. | N/A. |
| **Secrets/env vars** | None. | N/A. |
| **Build artifacts** | In-memory `_uiState` properties in `DashboardViewModel` (`isContinuousSuppressionEnabled`, `isAutoHudEnabled`, `trackedGamePackages`). | Migrate state initialization to observe `SettingsRepository.userSettings` Flow directly. |

---

## Common Pitfalls

### Pitfall 1: Calling `runBlocking` or `.first()` on the Main Thread
**What goes wrong:** The Android UI thread blocks waiting for disk I/O, leading to skipped frames, jank, or Application Not Responding (ANR) warnings.  
**Why it happens:** Developers attempt to retrieve persistent preferences synchronously during Activity or Service initialization.  
**How to avoid:** Collect `Flow<UserSettings>` asynchronously in `viewModelScope` or `serviceScope` using Kotlin coroutines. UI components should render safe default values immediately during cold boot without waiting for disk response.  
**Warning signs:** Choreographer logs: `Skipped 12 frames! The application may be doing too much work on its main thread.`

### Pitfall 2: Multi-Instance DataStore Conflicts
**What goes wrong:** Runtime crash: `IllegalStateException: There are multiple DataStores active for the same file: /data/user/0/com.xeno.gfx/files/datastore/xeno_gfx_settings.preferences_pb`.  
**Why it happens:** `preferencesDataStore(name = "xeno_gfx_settings")` or `PreferenceDataStoreFactory.create` is invoked in multiple classes instead of sharing a single application-scoped singleton.  
**How to avoid:** Define the `preferencesDataStore` delegate as a top-level property on `Context` in a single file (`SettingsRepository.kt`) and expose only the `SettingsRepository.getInstance(context)` singleton.  
**Warning signs:** Unit tests or multi-service transitions crash with `IllegalStateException`.

### Pitfall 3: Coordinates Slipping Off-Screen on Display Rotation
**What goes wrong:** A user places the HUD at coordinate `(x=950, y=1800)` in portrait. When BGMI launches in landscape `(width=2400, height=1080)`, the HUD is drawn off-screen or unreachable.  
**Why it happens:** Absolute pixel coordinates from one orientation exceed the viewport boundaries of another orientation.  
**How to avoid:** Implement strict clamping in `HudOverlayService.showOverlay()` and on `ACTION_UP` per Decision D-09:
`p.x = targetX.coerceIn(0, maxOf(0, screenWidth - hudWidth))`
`p.y = targetY.coerceIn(0, maxOf(0, screenHeight - hudHeight))`  
**Warning signs:** HUD overlay becomes invisible after rotating device or launching a landscape game.

### Pitfall 4: Swallowing Schema or ClassCast Exceptions in Error Handler
**What goes wrong:** A developer changes a preference key's type (e.g. from `Int` to `String`), and the application silently returns defaults forever instead of surfacing the development error.  
**Why it happens:** Catching generic `Exception` or `Throwable` inside `dataStore.data.catch { ... }` instead of exclusively checking `if (exception is IOException)`.  
**How to avoid:** Strictly inspect `if (exception is IOException) emit(emptyPreferences()) else throw exception` per official Android recommendations.  
**Warning signs:** Persistent preferences appear to reset silently after code modifications without any logcat warning.

---

## Code Examples

### 1. `SettingsKeys.kt` & Discrete Key Contracts
```kotlin
// Verified in-repo discrete keys contract
package com.xeno.gfx.core.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object SettingsKeys {
    val KEY_CONTINUOUS_SUPPRESSION = booleanPreferencesKey("continuous_suppression_enabled")
    val KEY_AUTO_HUD = booleanPreferencesKey("auto_hud_enabled")
    val KEY_HUD_X = intPreferencesKey("hud_coordinate_x")
    val KEY_HUD_Y = intPreferencesKey("hud_coordinate_y")
    val KEY_TRACKED_GAMES = stringSetPreferencesKey("tracked_game_packages")
    val KEY_CUSTOM_WHITELIST = stringSetPreferencesKey("user_custom_whitelist")
    val KEY_COMPACT_HUD_MODE = booleanPreferencesKey("compact_hud_mode")
}
```

### 2. `UserSettings.kt` Snapshot Model
```kotlin
package com.xeno.gfx.core.model

/**
 * Immutable snapshot of all persisted user preferences.
 * Default values match Decision D-04 verbatim.
 */
data class UserSettings(
    val isContinuousSuppressionEnabled: Boolean = true,
    val isAutoHudEnabled: Boolean = false,
    val hudCoordinates: Pair<Int, Int> = Pair(20, 100),
    val trackedGamePackages: Set<String> = emptySet(),
    val userCustomWhitelist: Set<String> = emptySet(),
    val isCompactHudMode: Boolean = false
)
```

### 3. `SettingsRepository.kt` Production Architecture
```kotlin
package com.xeno.gfx.core.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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

// Top-level single instance delegate per D-01 & D-05
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "xeno_gfx_settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val userSettings: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserSettings(
                isContinuousSuppressionEnabled = prefs[SettingsKeys.KEY_CONTINUOUS_SUPPRESSION] ?: true,
                isAutoHudEnabled = prefs[SettingsKeys.KEY_AUTO_HUD] ?: false,
                hudCoordinates = Pair(
                    prefs[SettingsKeys.KEY_HUD_X] ?: 20,
                    prefs[SettingsKeys.KEY_HUD_Y] ?: 100
                ),
                trackedGamePackages = prefs[SettingsKeys.KEY_TRACKED_GAMES] ?: emptySet(),
                userCustomWhitelist = prefs[SettingsKeys.KEY_CUSTOM_WHITELIST] ?: emptySet(),
                isCompactHudMode = prefs[SettingsKeys.KEY_COMPACT_HUD_MODE] ?: false
            )
        }
        .flowOn(ioDispatcher)

    suspend fun setContinuousSuppression(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.KEY_CONTINUOUS_SUPPRESSION] = enabled
        }
    }

    suspend fun setAutoHud(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.KEY_AUTO_HUD] = enabled
        }
    }

    suspend fun saveHudCoordinates(x: Int, y: Int) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.KEY_HUD_X] = x
            prefs[SettingsKeys.KEY_HUD_Y] = y
        }
    }

    suspend fun updateTrackedGames(packages: Set<String>) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.KEY_TRACKED_GAMES] = packages
        }
    }

    suspend fun updateCustomWhitelist(packages: Set<String>) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.KEY_CUSTOM_WHITELIST] = packages
        }
    }

    suspend fun setCompactHudMode(enabled: Boolean) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.KEY_COMPACT_HUD_MODE] = enabled
        }
    }

    suspend fun resetToDefaults() = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(
                    dataStore = (context.applicationContext ?: context).settingsDataStore
                ).also { instance = it }
            }
        }
    }
}
```

### 4. JUnit 4 JVM Unit Test Harness Pattern
```kotlin
package com.xeno.gfx.core.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val testFile = tempFolder.newFile("test_settings.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        repository = SettingsRepository(
            dataStore = testDataStore,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `test default settings match specifications`() = testScope.runTest {
        val settings = repository.userSettings.first()
        assertTrue(settings.isContinuousSuppressionEnabled)
        assertFalse(settings.isAutoHudEnabled)
        assertEquals(Pair(20, 100), settings.hudCoordinates)
        assertTrue(settings.trackedGamePackages.isEmpty())
        assertTrue(settings.userCustomWhitelist.isEmpty())
    }

    @Test
    fun `test save and read hud coordinates`() = testScope.runTest {
        repository.saveHudCoordinates(45, 120)
        val settings = repository.userSettings.first()
        assertEquals(Pair(45, 120), settings.hudCoordinates)
    }

    @Test
    fun `test update tracked game packages atomically`() = testScope.runTest {
        val games = setOf("com.pubg.imobile", "com.tencent.ig")
        repository.updateTrackedGames(games)
        val settings = repository.userSettings.first()
        assertEquals(games, settings.trackedGamePackages)
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|---|---|---|---|
| `SharedPreferences` | `Jetpack Preferences DataStore` | Android Jetpack 1.0 (2021) / 1.1 (2024) | Replaced synchronous blocking disk I/O and runtime parsing errors with coroutine Flow-based thread-safe persistence. |
| Protobuf DataStore | Preferences DataStore | Android Jetpack 1.0+ | Avoided protobuf compiler plugin setup, schema maintenance, and runtime code generation for simple app settings. |
| In-memory ViewModel state | Reactive DataStore Repository | Modern Android Architecture (UDF) | Settings reliably survive process death, OS low-memory kills, and device reboots without manual bundle saving. |

---

## In-Repo Discrete Values Verbatim Reference

For strict checkability and adherence to the In-Repo Value Provenance Rule, the following source-of-truth definitions were opened and verified this session:

1. **`HudConfig` Definition:**
```kotlin
data class HudConfig(
    val updateIntervalMs: Long = 1000L,
    val isFpsMeterEnabled: Boolean = true,
    val isRamMeterEnabled: Boolean = true,
    val isBatteryMeterEnabled: Boolean = true,
    val isTempMeterEnabled: Boolean = true,
    val isLatencyMeterEnabled: Boolean = true,
    val isCompactMode: Boolean = false,
    val opacity: Float = 0.85f
)
```
*[VERIFIED: app/src/main/java/com/xeno/gfx/core/model/HudConfig.kt:6-15]*

2. **`DashboardUiState` Definition:**
```kotlin
data class DashboardUiState(
    val hasOverlayPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val isHudActive: Boolean = false,
    val isAutoHudEnabled: Boolean = false,
    val isContinuousSuppressionEnabled: Boolean = true,
    val isCleaningInProgress: Boolean = false,
    val lastCleanResult: CleanResult? = null,
    val previewMetrics: DeviceMetrics = DeviceMetrics(),
    val installedApps: List<TrackedGame> = emptyList(),
    val trackedGamePackages: Set<String> = emptySet(),
    val isBgmiInstalled: Boolean = false
)
```
*[VERIFIED: app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:22-34]*

3. **`DashboardViewModel` BGMI Package Constant:**
```kotlin
    companion object {
        const val BGMI_PACKAGE_NAME = "com.pubg.imobile"
    }
```
*[VERIFIED: app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:43-45]*

4. **`HudOverlayService` Notification & Action Constants and Hardcoded Position:**
```kotlin
    companion object {
        const val ACTION_START = "com.xeno.gfx.action.START_HUD"
        const val ACTION_STOP = "com.xeno.gfx.action.STOP_HUD"
        private const val NOTIFICATION_ID = 2026
```
*[VERIFIED: app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:35-38]*
```kotlin
            x = 80
            y = 120
```
*[VERIFIED: app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:120-121]*

5. **`AppWhitelist` Package Sets:**
```kotlin
    val CRITICAL_SYSTEM_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.providers.telephony"
    )

    val SUPPORTED_BGMI_VARIANTS = setOf(
        "com.pubg.imobile",     // BGMI (Battlegrounds Mobile India)
        "com.tencent.ig",       // PUBG Mobile Global
        "com.tencent.iglite",   // PUBG Mobile Lite
        "com.pubg.krmobile",    // PUBG Korea
        "com.vng.pubgmobile"    // PUBG Vietnam
    )
```
*[VERIFIED: app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt:9-27]*

6. **Default Settings Specification from CONTEXT.md:**
```markdown
- **D-04:** Default settings state on fresh install: continuous suppression enabled (`true`), auto-HUD trigger disabled (`false` until usage permissions granted), HUD position `(x=20, y=100)`, empty custom whitelist.
```
*[VERIFIED: .planning/phases/02-jetpack-datastore-settings-persistence/02-CONTEXT.md:22-23]*

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|---|---|---|
| A1 | Jetpack Preferences DataStore 1.1.3 resolves seamlessly without transitively breaking Kotlin 2.0.21 or AndroidX Core 1.15.0. | Standard Stack | Low; DataStore 1.1.3 is fully compatible with Kotlin 2.0 and AGP 8.7+. If Gradle conflicts occur, 1.1.2 can be used as fallback. |

*All other claims and implementation patterns are verified against official Android documentation and codebase source files.*

---

## Open Questions (RESOLVED)

1. **Should initial installed launcher games seed `trackedGamePackages` if DataStore is empty?**
   - *RESOLVED:* Adopted 3-game auto-seed on empty DataStore per `02-02-PLAN.md` Task 1. When `loadInstalledLauncherApps` discovers games, if DataStore's `trackedGamePackages` is empty, seed with `games.take(3).map { it.packageName }.toSet()` and persist it immediately via `settingsRepository.updateTrackedGames`.
   - *What we know:* Currently `DashboardViewModel` seeds `games.take(3).map { it.packageName }.toSet()` if not specified [VERIFIED: app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:208].
   - *Recommendation:* If DataStore's `trackedGamePackages` is empty on first boot, initialize it with the first 3 detected games so users immediately experience auto-HUD without manual setup, then persist that choice so subsequent modifications stick.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|---|---|---|---|---|
| Java / JDK | Kotlin / Gradle compilation | ✓ | Java 21 (Temurin OpenJDK) | — |
| Android SDK Platform | Compilation target (API 35) | ✓ | API 35 (Android 15) | — |
| Gradle Wrapper | Build execution | ✓ | Gradle 8.11.1 | — |
| Google Maven Repository | Resolving `androidx.datastore` | ✓ | Online repository | Local maven cache / mirror |

---

## Validation Architecture

### Test Framework
| Property | Value |
|---|---|
| Framework | JUnit 4 (`4.13.2`) + `kotlinx-coroutines-test` (`1.9.0`) |
| Config file | `app/build.gradle.kts` [VERIFIED: app/build.gradle.kts:78-80] |
| Quick run command | `./gradlew testDebugUnitTest --tests com.xeno.gfx.core.repository.SettingsRepositoryTest` |
| Full suite command | `./gradlew testDebugUnitTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|---|---|---|---|---|
| **GFX-01** | Default preferences state initialized accurately | unit | `./gradlew testDebugUnitTest --tests com.xeno.gfx.core.repository.SettingsRepositoryTest.test_default_settings` | ❌ Wave 0 Gap |
| **GFX-01** | HUD coordinates persist and restore across sessions | unit | `./gradlew testDebugUnitTest --tests com.xeno.gfx.core.repository.SettingsRepositoryTest.test_save_and_read_hud_coordinates` | ❌ Wave 0 Gap |
| **GFX-01** | Tracked game packages and custom whitelist persist atomically | unit | `./gradlew testDebugUnitTest --tests com.xeno.gfx.core.repository.SettingsRepositoryTest.test_update_tracked_game_packages` | ❌ Wave 0 Gap |
| **GFX-01** | `IOException` caught gracefully falling back to defaults | unit | `./gradlew testDebugUnitTest --tests com.xeno.gfx.core.repository.SettingsRepositoryTest.test_io_exception_fallback` | ❌ Wave 0 Gap |

### Sampling Rate
- **Per task commit:** `./gradlew testDebugUnitTest --tests com.xeno.gfx.core.repository.SettingsRepositoryTest`
- **Per wave merge:** `./gradlew testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/java/com/xeno/gfx/core/repository/SettingsRepositoryTest.kt` — covers GFX-01 persistence behaviors.
- [ ] Dependency addition in `gradle/libs.versions.toml` (`androidx-datastore-preferences = "1.1.3"`).

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---|---|---|
| V5 Input Validation | Yes | Coordinates must be clamped within physical display bounds (`0 <= x <= screenWidth - hudWidth`, `0 <= y <= screenHeight - hudHeight`). Package names validated against Android package format. |
| V8 Data Protection at Rest | Yes | Jetpack Preferences DataStore stores files in app's private sandbox directory (`/data/user/0/com.xeno.gfx/files/datastore/`) with `0600` permissions, preventing inter-app snooping on non-rooted devices. |
| V13 API and Service Security | Yes | Overlay service must strictly guard against `BadTokenException` and verify `Settings.canDrawOverlays` before rendering. |

### Known Threat Patterns for Android DataStore

| Pattern | STRIDE | Standard Mitigation |
|---|---|---|
| Corrupted or malformed preferences file on disk | Denial of Service | Intercept `IOException` in Flow `.catch` block and fall back to `emptyPreferences()` per Decision D-07. |
| Injection of malicious or arbitrary package strings | Tampering | Whitelist validation in `AppWhitelist`; only valid launcher/game packages stored. |
| Main-thread file locking / UI stutter | Denial of Service (Quality) | Perform all disk mutations strictly on `Dispatchers.IO` using suspending functions. |

---

## Sources

### Primary (HIGH confidence)
- [Official Android DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore) - Read & write patterns, `IOException` handling with `emptyPreferences()`, `PreferenceDataStoreFactory` testing.
- [Google Maven Repository](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/) - Verified release versions up to `1.1.3`.
- In-Repo Source Files:
  - `app/src/main/java/com/xeno/gfx/core/model/HudConfig.kt`
  - `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`
  - `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`
  - `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt`
  - `.planning/phases/02-jetpack-datastore-settings-persistence/02-CONTEXT.md`
  - `.planning/phases/02-jetpack-datastore-settings-persistence/02-UI-SPEC.md`

### Secondary (MEDIUM confidence)
- Android Developers Guide on Preferences Testing using `PreferenceDataStoreFactory.create` and JUnit 4 `TemporaryFolder`.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - `androidx.datastore:datastore-preferences:1.1.3` is the official Google Jetpack solution.
- Architecture: HIGH - Singleton repository exposing `Flow<UserSettings>` matches existing `TelemetryRepository` pattern.
- Pitfalls: HIGH - Well-documented Android DataStore edge cases (runBlocking, multiple instances, coordinate clamping).

**Research date:** 2026-11-06  
**Valid until:** 2026-12-06 (30 days)
