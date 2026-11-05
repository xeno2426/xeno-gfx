<Architecture Patterns>
**Domain:** Android Gaming Performance Optimizer & Real-Time Telemetry HUD
**Researched:** 2026-11-05

## Recommended Architecture

The target architecture transitions Xeno GFX from an Activity-centric model (where background tasks erroneously depended on `MainActivity` and `DashboardViewModel`) into a **Service-Centric Reactive Architecture**. The core runtime execution lives in a persistent Foreground Service (`HudOverlayService` / `GameSessionManager`), while UI surfaces (`DashboardActivity` and floating WindowManager `ComposeView`) act as purely reactive subscribers to application-scoped repositories.

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   PRESENTATION LAYER                                   │
├──────────────────────────────────────────┬─────────────────────────────────────────────┤
│        Dashboard Activity & Compose      │          Floating Window Overlay HUD        │
│    `com.xeno.gfx.ui.MainActivity`        │      `com.xeno.gfx.ui.overlay.HudOverlayView`│
│ `com.xeno.gfx.ui.dashboard.DashboardScreen`│  `com.xeno.gfx.ui.overlay.MinimalFpsBadge`   │
│ `com.xeno.gfx.ui.dashboard.DashboardVM`  │      `WindowManager` (TYPE_APPLICATION_OVERLAY)│
└────────────────────┬─────────────────────┴──────────────────────┬──────────────────────┘
                     │                                            │
                     │ observes StateFlow                         │ observes StateFlow
                     ▼                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        SERVICE & SESSION ORCHESTRATION LAYER                           │
├────────────────────────────────────────────────────────────────────────────────────────┤
│                           `service.HudOverlayService`                                  │
│             (Android Foreground Service, `specialUse` FGS Subtype)                     │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                            `core.session.GameSessionManager`                     │  │
│  │   - ServiceScope Coroutine Lifecycle (Survives Activity Minimization)             │  │
│  │   - Continuous 30s RAM Suppression Ticker (`RamOptimizer`)                       │  │
│  │   - Foreground Game Transition Monitor (`ForegroundGameDetector`)                 │  │
│  │   - Emergency Thermal Protection Guard (`TelemetryRepository` listener)          │  │
│  │   - Ongoing Notification Quick Actions (Purge RAM, Toggle HUD, Stop)             │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                      `service.ComposeLifecycleOwner`                             │  │
│  │   - Synthetic LifecycleOwner + ViewModelStoreOwner + SavedStateRegistryOwner     │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────┬─────────────────────┬──────────────────────┬──────────────────────┘
                     │                     │                      │
                     ▼                     ▼                      ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                             REACTIVE REPOSITORIES (SINGLETON)                          │
├────────────────────────────┬─────────────────────────────┬─────────────────────────────┤
│   `TelemetryRepository`    │    `SettingsRepository`     │    `GfxConfigRepository`    │
│  - Single Telemetry Source │  - Jetpack DataStore Prefs  │  - UE4 `Active.sav` Parser  │
│  - Demand-Driven Sampling  │  - Reactive `UserSettings`  │  - SAF DocumentFile Bridge  │
│  - FPS 30-Cap Calibration  │  - Tracked Games & Whitelist│  - 30 FPS Optimization     │
│  - Async Latency Pinger    │  - HUD Styles & Coordinates │  - Atomic Backup & Restore  │
└──────────────┬─────────────┴─────────────┬───────────────┴──────────────┬──────────────┘
               │                           │                              │
               ▼                           ▼                              ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                         CORE ENGINE & OS HARDWARE PROBES                               │
├────────────────────────────┬─────────────────────────────┬─────────────────────────────┤
│   `core.stats.FpsSampler`  │  `core.optimizer.RamOptim`  │  `core.detector.ForegrGame` │
│  - Choreographer VSYNC     │  - `ActivityManager.kill`   │  - `UsageStatsManager`      │
│  - Frame Cadence Clamp     │  - Strict IME/System Filter │  - `ACTIVITY_RESUMED` Poll  │
│  - Target 30 FPS Normalizer│  - Kernel LMK reclamation   │  - Tracked Package Watcher  │
└────────────────────────────┴─────────────────────────────┴─────────────────────────────┘
```

---

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|----------------|-------------------|
| `HudOverlayService` | Persistent Android Foreground Service holding process priority (`specialUse`). Hosts the floating `ComposeView` window and persistent notification. | Android OS, `WindowManager`, `ComposeLifecycleOwner`, `GameSessionManager` |
| `GameSessionManager` | Orchestrates the active game lifecycle: runs continuous 30s RAM suppression, monitors foreground app transitions, and responds to emergency thermal spikes. Bound to `ServiceScope`. | `HudOverlayService`, `ForegroundGameDetector`, `RamOptimizer`, `TelemetryRepository`, `SettingsRepository` |
| `TelemetryRepository` | Consolidated single source of truth for all hardware diagnostics. Coordinates demand-driven sampling, calibrated 30 FPS frame estimation, battery thermals, RAM, and async network ping. | `FpsSampler`, `HardwareMetricsSampler`, `SettingsRepository`, `DashboardViewModel`, `HudOverlayService` |
| `SettingsRepository` | Manages persistent user preferences using Jetpack Preferences DataStore. Stores tracked packages, whitelist, HUD mode, coordinates, and GFX presets. | Jetpack DataStore, `DashboardViewModel`, `GameSessionManager`, `HudOverlayService` |
| `GfxConfigRepository` | High-level GFX management. Coordinates UE4 `Active.sav` configuration binary parsing, 30 FPS preset injection, checksum recalculation, backup, and restore. | `SafStorageManager`, `ActiveSavPatcher`, `SettingsRepository`, `DashboardViewModel` |
| `ActiveSavPatcher` | Low-level binary codec for Unreal Engine 4 save game format. Edits graphics level, frame rates, and disables dynamic shadows while preserving struct offsets. | `GfxConfigRepository` |
| `SafStorageManager` | Storage Access Framework (SAF) bridge using `DocumentFile` to read and write to game sandboxes on Android 11+ (API 30+) without root. | Android OS (`Intent.ACTION_OPEN_DOCUMENT_TREE`), `GfxConfigRepository` |
| `RamOptimizer` | Executes memory reclamation via `ActivityManager.killBackgroundProcesses()`. Enforces strict System & Keyboard IME-only whitelist protection. | `ActivityManager`, `AppWhitelist`, `GameSessionManager` |
| `ForegroundGameDetector` | Background polling loop inspecting `UsageEvents` for `ACTIVITY_RESUMED` events of tracked packages. | `UsageStatsManager`, `GameSessionManager` |
| `DashboardViewModel` | MVVM presentation coordinator for `MainActivity`. Binds repository flows to `DashboardUiState`. Dispatches user actions (manual purge, preset change, HUD toggle). | `SettingsRepository`, `TelemetryRepository`, `GfxConfigRepository`, `HudOverlayService` |
| `HudOverlayView` / `MinimalFpsBadge` | Jetpack Compose composable views rendered directly into `WindowManager`. Display real-time calibrated telemetry or ultra-small transparent 30 FPS badge. | `ComposeView`, `TelemetryRepository`, `SettingsRepository` |

---

### Data Flow

```text
Telemetry Data Flow:
[Choreographer / OS Sensors] ──> [TelemetryRepository] ──(StateFlow<DeviceMetrics>)──> [DashboardViewModel]
                                                                                └──> [HudOverlayView / Mini Badge]

User Preferences Flow:
[DashboardScreen / Overlay] ──> [SettingsRepository (DataStore)] ──(Flow<UserSettings>)──> [GameSessionManager]
                                                                                   └──> [HudOverlayService]

Game Session & Suppression Flow:
[UsageStatsManager] ──> [ForegroundGameDetector] ──(onGameLaunched)──> [GameSessionManager]
                                                                              │
                                       ┌──────────────────────────────────────┴──────────────────────────────────────┐
                                       ▼                                                                             ▼
                        [RamOptimizer.purgeMemory]                                                    [HudOverlayService.showOverlay]
                                       │                                                                             │
                        [30s Continuous Suppression Loop]                                             [ComposeView (Minimalist Badge)]
```

#### 1. Telemetry Monitoring Flow (Consolidated Single Repository)
1. When either `DashboardActivity` is visible or `HudOverlayService` is active, they subscribe to `TelemetryRepository.metrics`.
2. `TelemetryRepository` uses an atomic subscriber counter or `SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000)`. When subscriber count increases from 0 to 1, sampling begins.
3. Every 1000ms:
   - `ActivityManager.getMemoryInfo()` retrieves RAM stats.
   - `BatteryManager` broadcast receiver supplies battery level and temperature.
   - `PowerManager.currentThermalStatus` supplies Android Q+ thermal status.
   - Calibrated `FpsSampler` collects VSYNC intervals on `Looper.getMainLooper()`, applying the 30 FPS target cap filter to prevent false 60 FPS reporting.
4. Concurrently in a decoupled background worker (every 5000ms):
   - Non-blocking socket connect to `8.8.8.8:53` runs on `Dispatchers.IO` with a 500ms timeout, updating `latencyMs` without stalling the primary telemetry loop.
5. Emits an updated immutable `DeviceMetrics` snapshot down `StateFlow<DeviceMetrics>`.
6. Both `DashboardViewModel` (via `collectAsStateWithLifecycle`) and `HudOverlayService` (via Compose `collectAsState`) render the identical metrics state with zero duplicate sensor overhead.

#### 2. Persistent Game Turbo & Continuous Suppression Flow
1. User enables Auto-HUD or taps "LAUNCH BGMI WITH TURBO".
2. `HudOverlayService` starts in the foreground (`startForeground`) with an ongoing notification declaring quick actions (`Purge RAM Now`, `Toggle HUD`, `Exit Turbo`).
3. `GameSessionManager` (residing in `HudOverlayService`) takes ownership of `ForegroundGameDetector` and launches a persistent monitoring coroutine on `ServiceScope`.
4. User enters BGMI (`com.pubg.imobile`). `MainActivity` is paused or stopped, but `HudOverlayService` remains running at OOM adj score 200 (Foreground Service).
5. `ForegroundGameDetector` detects `ACTIVITY_RESUMED` for `com.pubg.imobile`.
6. `GameSessionManager` triggers:
   - Step A: `RamOptimizer.purgeMemory(targetGamePackage, strictWhitelist)` to reclaim background memory before game initialization.
   - Step B: Launches continuous 30-second suppression ticker: every 30 seconds, background processes are purged to prevent Android LMK from dropping game frames.
   - Step C: Updates notification text: "BGMI Turbo Active • 30 FPS Target Locked".
   - Step D: Checks `SettingsRepository.hudStyle`: if `MINIMAL_FPS_BADGE`, renders ultra-compact transparent badge; if `FULL_HUD`, renders cyberpunk diagnostic card.
7. Thermal Guard Trigger: If `TelemetryRepository` emits a battery temperature exceeding threshold (e.g. >45.0°C) or thermal status `SEVERE`, `GameSessionManager` posts an emergency notification and triggers an immediate RAM purge to shed background CPU load.
8. When user exits BGMI back to launcher: `ForegroundGameDetector` detects exit, cancels the 30s suppression ticker, hides the overlay (if auto-mode), and returns the service to idle standby.

#### 3. GFX Configuration & 30 FPS Preset Injection Flow
1. User selects "Smooth + 30 FPS Lockdown" in the GFX tool section of `DashboardScreen`.
2. `DashboardViewModel` dispatches selection to `GfxConfigRepository.applyPreset(presetId)`.
3. `GfxConfigRepository` verifies SAF directory permission via `SafStorageManager.hasPermission()`.
   - If missing, fires one-time SAF directory picker intent (`ACTION_OPEN_DOCUMENT_TREE`) targeting the game's `SaveGames` folder.
4. Once URI is resolved:
   - `SafStorageManager` opens input stream on `Active.sav`.
   - `GfxConfigRepository` creates an atomic backup copy `Active.sav.bak`.
   - `ActiveSavPatcher` parses the UE4 binary payload, finds property blocks (`FPSLevel`, `BattleFPS`, `ShadowQuality`, `ResolutionQuality`), updates them to target 30 FPS values, and recalculates the trailing checksum.
   - Writes the modified byte buffer back through `SafStorageManager.openOutputStream()`.
5. `SettingsRepository` saves the active preset state in Jetpack DataStore. UI updates to reflect active 30 FPS configuration.

---

## Patterns to Follow

### Pattern 1: Demand-Driven Single-Instance Telemetry Repository
**What:** Consolidate all hardware sampling into a single application-scoped repository where polling loops run only when there are active UI or service consumers, using Kotlin `SharingStarted.WhileSubscribed`.
**When:** Whenever multiple UI or background consumers require access to hardware sensors, Choreographer frame callbacks, or network latency probes.
**Example:**
```kotlin
package com.xeno.gfx.core.repository

import android.content.Context
import com.xeno.gfx.core.model.DeviceMetrics
import com.xeno.gfx.core.stats.FpsSampler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetSocketAddress
import java.net.Socket

class TelemetryRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val fpsSampler = FpsSampler(maxRefreshRate = 30) // Calibrated to 30 FPS target

    private val latencyFlow: Flow<Long?> = flow {
        while (currentCoroutineContext().isActive) {
            val ping = measureSocketPing()
            emit(ping)
            delay(5000L) // Slower poll for network avoids battery & CPU drain
        }
    }.flowOn(ioDispatcher)

    val metrics: StateFlow<DeviceMetrics> = flow {
        fpsSampler.start()
        try {
            while (currentCoroutineContext().isActive) {
                val snapshot = readHardwareSnapshot()
                emit(snapshot)
                delay(1000L)
            }
        } finally {
            fpsSampler.stop()
        }
    }
    .flowOn(ioDispatcher)
    .stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        initialValue = DeviceMetrics()
    )

    private fun measureSocketPing(): Long? = try {
        val start = System.currentTimeMillis()
        Socket().use { it.connect(InetSocketAddress("8.8.8.8", 53), 500) }
        System.currentTimeMillis() - start
    } catch (_: Exception) { null }

    private fun readHardwareSnapshot(): DeviceMetrics {
        // Sample RAM, battery temp, thermal status, calibrated FPS
        return DeviceMetrics(currentFps = fpsSampler.latestCalibratedFps)
    }
}
```

### Pattern 2: Service-Bound Session Manager (Decoupled from ViewModel)
**What:** Move the active game lifecycle, continuous suppression loops, and UsageStats polling into a persistent `GameSessionManager` tied to `HudOverlayService`'s `ServiceScope`.
**When:** Whenever background tasks must outlive Activity destruction or minimization when launching external games.
**Example:**
```kotlin
package com.xeno.gfx.core.session

import com.xeno.gfx.core.detector.ForegroundGameDetector
import com.xeno.gfx.core.optimizer.RamOptimizer
import com.xeno.gfx.core.repository.SettingsRepository
import kotlinx.coroutines.*

class GameSessionManager(
    private val serviceScope: CoroutineScope,
    private val detector: ForegroundGameDetector,
    private val ramOptimizer: RamOptimizer,
    private val settingsRepository: SettingsRepository
) {
    private var suppressionJob: Job? = null

    fun startSessionManagement() {
        detector.onGameLaunched = { packageName ->
            serviceScope.launch {
                // Immediate purge
                ramOptimizer.purgeMemory(packageName)
                // Start 30s continuous suppression ticker
                startContinuousSuppression(packageName)
            }
        }
        detector.onGameExited = {
            stopContinuousSuppression()
        }
        detector.startMonitoring(serviceScope)
    }

    private fun startContinuousSuppression(packageName: String) {
        suppressionJob?.cancel()
        suppressionJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30_000L) // Aggressive 30s interval for Full Throttle Mode
                if (isActive) {
                    ramOptimizer.purgeMemory(packageName)
                }
            }
        }
    }

    fun stopContinuousSuppression() {
        suppressionJob?.cancel()
        suppressionJob = null
    }

    fun cleanup() {
        stopContinuousSuppression()
        detector.stopMonitoring()
    }
}
```

### Pattern 3: Atomic Storage Access Framework Binary Patching with Rollback
**What:** Read/write game configuration files through SAF `DocumentFile` using atomic streams and creating `.bak` files prior to modification.
**When:** Applying GFX settings to game directories restricted by Android 11+ scoped storage.
**Example:**
```kotlin
package com.xeno.gfx.core.gfx

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream

class GfxConfigEngine(private val context: Context) {
    suspend fun apply30FpsPreset(treeUri: Uri, presetBytes: ByteArray): Result<Unit> = runCatching {
        val rootDir = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Cannot access storage tree")
        val activeSavFile = rootDir.findFile("Active.sav")
            ?: throw NoSuchElementException("Active.sav not found")

        // 1. Create atomic backup if not exists
        if (rootDir.findFile("Active.sav.bak") == null) {
            val backup = rootDir.createFile("application/octet-stream", "Active.sav.bak")
                ?: throw IllegalStateException("Failed to create backup")
            context.contentResolver.openInputStream(activeSavFile.uri)?.use { input ->
                context.contentResolver.openOutputStream(backup.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
        }

        // 2. Write patched payload
        context.contentResolver.openOutputStream(activeSavFile.uri, "wt")?.use { output ->
            output.write(presetBytes)
            output.flush()
        } ?: throw IllegalStateException("Cannot write to Active.sav")
    }
}
```

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: ViewModel-Scoped Background Execution
**What happens:** Launching background loops (such as `ramOptimizer.startContinuousSuppression` or `gameDetector.startMonitoring`) inside `viewModelScope`.
**Why it's bad:** As soon as the user opens BGMI or navigates to another app, `MainActivity` enters `onStop()` / `onDestroy()`. Under low-RAM conditions (3GB/4GB), Android aggressively kills cached activities. When `DashboardViewModel.onCleared()` fires, `viewModelScope` is cancelled. Continuous suppression and auto-HUD silently cease working exactly when the user is playing the game.
**Instead:** Move all persistent execution to `GameSessionManager` inside `HudOverlayService`, which runs as a prioritized Foreground Service. `DashboardViewModel` should only bind to StateFlows for UI display.

### Anti-Pattern 2: Multi-Instance Telemetry Probing
**What happens:** Creating independent `StatsCollector` instances in `DashboardViewModel` and `HudOverlayService`.
**Why it's bad:** Multiple coroutines poll system files, multiple sockets ping `8.8.8.8`, and multiple `Choreographer.FrameCallback`s register with Android's compositor. This consumes measurable CPU cycles (>3%), defeating the primary objective of freeing CPU/RAM for low-end gaming devices.
**Instead:** Centralize telemetry in an application-scoped `TelemetryRepository` that shares a single multicast `StateFlow<DeviceMetrics>` and uses `WhileSubscribed` reference counting.

### Anti-Pattern 3: Synchronous Socket Ping in Metrics Loop
**What happens:** Calling `Socket().connect(InetSocketAddress("8.8.8.8", 53), 500)` synchronously inside the 1000ms hardware sampling loop.
**Why it's bad:** If the mobile network is congested or drops packets, the 500ms socket timeout delays the entire coroutine. RAM, temperature, and FPS counters visibly hitch and lag every time a ping occurs.
**Instead:** Decouple network latency sampling into a separate independent Flow running at a relaxed cadence (5-10s) on `Dispatchers.IO`.

### Anti-Pattern 4: Direct File Path Access under Scoped Storage
**What happens:** Trying to use `java.io.File("/storage/emulated/0/Android/data/com.pubg.imobile/...")` on Android 11+ (API 30+).
**Why it's bad:** Throws `AccessControlException` or silent failures. Android 11+ strictly isolates `/Android/data` from standard file APIs without root.
**Instead:** Leverage Android Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) with `DocumentFile` and persisted URI permissions.

---

## Suggested Build Order & Roadmap Implications

The dependency graph between components dictates a clear 6-phase implementation roadmap. Downstream features depend directly on foundation layers:

```text
[Phase 1: Telemetry & FPS Calibration] 
       │
       ▼
[Phase 2: DataStore Settings Persistence] ──┐
       │                                    │
       ▼                                    ▼
[Phase 3: Service-Centric Session Manager]  [Phase 5: GFX Config Engine]
       │                                    │
       ▼                                    │
[Phase 4: Minimalist HUD & Compose Overlay] │
       │                                    │
       └─────────────────┬──────────────────┘
                         ▼
           [Phase 6: Hardening & Thermal Guard]
```

### Phase 1: Telemetry Consolidation & 30 FPS Meter Calibration
- **Build First:** Foundation for all monitoring.
- **Deliverables:**
  - Create `TelemetryRepository` singleton. Eliminate dual `StatsCollector` in ViewModel and Service.
  - Decouple network ping into an asynchronous 5s worker.
  - Calibrate `FpsSampler`: Add a 30 FPS target clamping algorithm and frame interval smoothing to eliminate fake 60 FPS compositor reporting.
- **Dependencies:** None (uses existing Android system APIs).

### Phase 2: Jetpack DataStore Persistence Layer
- **Build Second:** Foundation for state management.
- **Deliverables:**
  - Introduce `androidx.datastore:datastore-preferences` dependency.
  - Create `SettingsRepository` with `Flow<UserSettings>`.
  - Persist tracked game packages, continuous suppression toggles, HUD configuration, and custom whitelist.
- **Dependencies:** Phase 1 (for HUD config models).

### Phase 3: Persistent Foreground Service & Game Session Decoupling
- **Build Third:** Solves the core lifecycle defect.
- **Deliverables:**
  - Create `GameSessionManager` living inside `HudOverlayService`'s `ServiceScope`.
  - Move `ForegroundGameDetector` and `RamOptimizer` 30s continuous suppression loop out of `DashboardViewModel`.
  - Implement strict System & Keyboard IME-only whitelist filtering in `AppWhitelist`.
  - Add ongoing persistent notification quick actions (`Purge RAM Now`, `Toggle HUD`, `Exit Turbo`).
- **Dependencies:** Phase 1 (Telemetry), Phase 2 (Settings).

### Phase 4: HUD Evolution & Minimalist FPS Badge
- **Build Fourth:** User-facing overlay presentation.
- **Deliverables:**
  - Implement `MinimalFpsBadge` composable (ultra-small, semi-transparent, minimal screen footprint).
  - Add dynamic switching between Cyberpunk full HUD and Minimalist Badge based on `SettingsRepository`.
  - Persist WindowManager overlay coordinates (X, Y) to DataStore on touch release.
- **Dependencies:** Phase 2 (Settings), Phase 3 (Service host).

### Phase 5: BGMI 30 FPS GFX Config Engine
- **Build Fifth:** Graphic configuration utility.
- **Deliverables:**
  - Create `SafStorageManager` for Storage Access Framework URI permissions.
  - Build `ActiveSavPatcher` to parse and modify UE4 binary configs for 30 FPS lock, shadow elimination, and texture scaling.
  - Create 30 FPS presets: "Smooth 30 FPS Low-RAM", "Balanced 30 FPS", "Anti-Stutter Lockdown".
  - Integrate preset selection into `DashboardScreen`.
- **Dependencies:** Phase 2 (DataStore for active preset persistence).

### Phase 6: Emergency Thermal Protection & System Hardening
- **Build Sixth:** Safety and resilience layer.
- **Deliverables:**
  - Hook `PowerManager.OnThermalStatusChangedListener` and battery temperature threshold watcher in `GameSessionManager`.
  - Automated emergency RAM purge and user notification when thermals exceed critical limits.
  - Full codebase hardening: WindowManager view attachment leak guards, try-catch safety on all IPC boundaries, Proguard rules verification.
- **Dependencies:** All previous phases.

---

## Scalability & Resource Constraints

| Hardware & OS Tier | Memory Footprint Target | CPU Overhead Target | Architectural Adaptation |
|--------------------|-------------------------|---------------------|--------------------------|
| **Tier 1: 3GB RAM / Android 8.0 - 10 (API 26-29)** | <20 MB | <0.8% | Ultra-aggressive background process pruning. Skip deep reflection; rely on standard `killBackgroundProcesses`. Slower ping polling (8s). Enforce Minimalist FPS Badge by default to minimize GPU Compose overhead. |
| **Tier 2: 4GB RAM / Android 11 - 13 (API 30-33)** | <28 MB | <1.0% | Standard 30s continuous suppression ticker. Use Storage Access Framework (`DocumentFile`) for GFX configuration injection. Full Cyberpunk HUD or Minimalist Badge supported. |
| **Tier 3: 6GB+ RAM / Android 14 - 15 (API 34-35)** | <35 MB | <1.2% | Explicit `specialUse` Foreground Service Subtype declared with system property metadata. Battery saver exemption handling and `POST_NOTIFICATIONS` runtime permission checks. |

---

## Sources

- [HIGH] Android Developer Guides: *Guide to App Architecture (Repository Pattern & UDF)* — `developer.android.com/topic/architecture`
- [HIGH] Android Developer Guides: *Foreground Service Types & Policies in Android 14/15* — `developer.android.com/about/versions/14/changes/fgs-types-required`
- [HIGH] Android Developer Guides: *DataStore Preferences Implementation Guide* — `developer.android.com/topic/libraries/architecture/datastore`
- [MEDIUM] Unreal Engine 4 Community Documentation: *Active.sav binary structure and property serialization in mobile builds*
- [HIGH] Android Framework APIs: `android.view.Choreographer`, `android.app.ActivityManager`, `android.app.usage.UsageStatsManager`
