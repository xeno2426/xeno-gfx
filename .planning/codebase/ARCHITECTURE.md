---
last_mapped_commit: 3db9ba3d13768e0c9b05d70867d81a71845a1d8e
last_mapped_at: 2026-11-05
---
<!-- refreshed: 2026-11-05 -->
<Architecture>
**Analysis Date:** 2026-11-05

## System Overview

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   Presentation Layer                                   │
├──────────────────────────────────────────┬─────────────────────────────────────────────┤
│         Dashboard Activity & UI          │            Floating Window Overlay          │
│       `app/.../ui/MainActivity.kt`       │      `app/.../service/HudOverlayService.kt` │
│    `app/.../ui/dashboard/DashboardScreen` │      `app/.../ui/overlay/HudOverlayView.kt` │
└────────────────────┬─────────────────────┴──────────────────────┬──────────────────────┘
                     │                                            │
                     ▼                                            ▼
┌──────────────────────────────────────────┐  ┌──────────────────────────────────────────┐
│             State Management             │  │        Service Lifecycle Bridge          │
│ `app/.../ui/dashboard/DashboardViewModel`│  │ `app/.../service/ComposeLifecycleOwner`  │
│       (StateFlow<DashboardUiState>)      │  │ (LifecycleOwner + ViewModelStoreOwner)   │
└────────────────────┬─────────────────────┘  └───────────────────┬──────────────────────┘
                     │                                            │
                     ▼                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              Core Services & Hardware Probes                           │
├───────────────────────────────┬─────────────────────────────┬──────────────────────────┤
│        Telemetry Engine       │       Process Optimizer     │    Foreground Detector   │
│ `app/.../core/stats/StatsCol` │ `app/.../core/optimizer/`   │ `app/.../core/detector/` │
│ `app/.../core/stats/FpsSample`│ `RamOptimizer.kt`           │ `ForegroundGameDetector` │
│                               │ `AppWhitelist.kt`           │                          │
└───────────────┬───────────────┴──────────────┬──────────────┴────────────┬─────────────┘
                │                              │                           │
                ▼                              ▼                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                Android OS Subsystems                                   │
│  `Choreographer` (VSYNC)  │  `ActivityManager` (Memory/Kill) │ `UsageStatsManager`    │
│  `DisplayManager` (Hz)    │  `BatteryManager` (Temp/Charge)  │ `WindowManager` (HUD)   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `XenoGfxApp` | Application entry point; initializes low-priority notification channels for foreground services | `app/src/main/java/com/xeno/gfx/XenoGfxApp.kt` |
| `MainActivity` | Launcher activity hosting Compose theme and delegating events to `DashboardViewModel` | `app/src/main/java/com/xeno/gfx/ui/MainActivity.kt` |
| `DashboardViewModel` | State machine coordinating telemetry sampling, app listing, game detection callbacks, and RAM purges | `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt` |
| `DashboardScreen` | Compose UI rendering BGMI turbo card, telemetry cards, permission status, and game toggles | `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt` |
| `HudOverlayService` | Foreground service managing floating window lifecycle, touch drag physics, and Compose hosting | `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt` |
| `ComposeLifecycleOwner` | Synthetic lifecycle, ViewModelStore, and SavedStateRegistry owner required by Compose in a Service | `app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt` |
| `HudOverlayView` | Collapsible floating HUD composable displaying real-time FPS, RAM, thermal status, and ping | `app/src/main/java/com/xeno/gfx/ui/overlay/HudOverlayView.kt` |
| `StatsCollector` | Periodic metrics sampler polling RAM, battery temperature, display refresh rate, and ping | `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt` |
| `FpsSampler` | Frame rate calculator using Android `Choreographer` VSYNC callbacks on the Main Looper | `app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt` |
| `ForegroundGameDetector` | Background polling loop checking `UsageEvents` for `ACTIVITY_RESUMED` events | `app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt` |
| `RamOptimizer` | Memory reclaim engine invoking `killBackgroundProcesses` on non-whitelisted apps | `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt` |
| `AppWhitelist` | Policy engine protecting critical system processes, current launcher, active IME, and game variants | `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt` |
| `PermissionHelper` | Helper utility checking and requesting `SYSTEM_ALERT_WINDOW` and `PACKAGE_USAGE_STATS` | `app/src/main/java/com/xeno/gfx/core/permission/PermissionHelper.kt` |
| `DeviceMetrics` | Domain snapshot model capturing real-time device hardware telemetry and thermal state | `app/src/main/java/com/xeno/gfx/core/model/DeviceMetrics.kt` |
| `HudConfig` | Data model defining HUD visual configuration, update interval, and enabled meters | `app/src/main/java/com/xeno/gfx/core/model/HudConfig.kt` |
| `TrackedGame` | Model representing an installed application tracked for automatic HUD overlay triggers | `app/src/main/java/com/xeno/gfx/core/model/TrackedGame.kt` |

## Pattern Overview

**Overall:** Android MVVM + Service-Hosted Floating Compose UI

**Key Characteristics:**
- **Decoupled Service Overlay Architecture:** Unlike standard Android applications where UI is strictly bound to an `Activity`, the primary runtime monitor lives in a Foreground Service (`HudOverlayService`) rendering a `ComposeView` into Android `WindowManager` with `TYPE_APPLICATION_OVERLAY`.
- **Unidirectional Data Flow (UDF):** Hardware sensors update reactive Kotlin `StateFlow<DeviceMetrics>` streams consumed by Jetpack Compose via `collectAsState()`.
- **Safe Process Pruning Hierarchy:** Process termination uses a whitelist checking system services, active keyboards (IMEs), home launcher activities, and supported game package variants before invoking `ActivityManager.killBackgroundProcesses()`.

## Layers

**Presentation Layer (Activity & Composable Screens):**
- Purpose: Render user controls, configuration dashboard, and telemetry previews.
- Location: `app/src/main/java/com/xeno/gfx/ui/`
- Contains: `MainActivity.kt`, `DashboardScreen.kt`, `HudOverlayView.kt`, theme files (`Theme.kt`, `Color.kt`, `Type.kt`).
- Depends on: `DashboardViewModel`, `DeviceMetrics`, `HudConfig`, `PermissionHelper`.
- Used by: Android launcher, user interaction.

**State & Orchestration Layer (ViewModel):**
- Purpose: Maintain UI state, schedule background telemetry loops, listen for game transitions, coordinate RAM optimization tasks.
- Location: `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`
- Contains: `DashboardViewModel`, `DashboardUiState`.
- Depends on: `StatsCollector`, `ForegroundGameDetector`, `RamOptimizer`, `PermissionHelper`, `HudOverlayService`.
- Used by: `MainActivity`, `DashboardScreen`.

**Background Service & Overlay Layer:**
- Purpose: Host persistent floating HUD on top of full-screen games while handling OS lifecycle requirements and touch dragging.
- Location: `app/src/main/java/com/xeno/gfx/service/`
- Contains: `HudOverlayService.kt`, `ComposeLifecycleOwner.kt`.
- Depends on: `ComposeView`, `WindowManager`, `StatsCollector`, `HudOverlayView`, `XenoGfxApp`.
- Used by: `DashboardViewModel`, auto-game detector intents.

**Core Services & Hardware Telemetry Layer:**
- Purpose: Interface with low-level Android system services (`ActivityManager`, `UsageStatsManager`, `PowerManager`, `DisplayManager`, `Choreographer`).
- Location: `app/src/main/java/com/xeno/gfx/core/`
- Contains: `stats/` (`StatsCollector`, `FpsSampler`), `optimizer/` (`RamOptimizer`, `AppWhitelist`), `detector/` (`ForegroundGameDetector`), `permission/` (`PermissionHelper`).
- Depends on: Android framework APIs, Kotlin Coroutines, Kotlin Flow.
- Used by: `DashboardViewModel`, `HudOverlayService`.

**Domain Models Layer:**
- Purpose: Immutable data structures and entity definitions.
- Location: `app/src/main/java/com/xeno/gfx/core/model/`
- Contains: `DeviceMetrics.kt`, `HudConfig.kt`, `TrackedGame.kt`.
- Depends on: Pure Kotlin data classes (plus Android `Drawable` for app icons).
- Used by: All layers.

## Data Flow

### Primary Request Path (Telemetry Monitoring)

1. `StatsCollector.startSampling()` launches a polling loop on `Dispatchers.IO` and starts `FpsSampler` (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:49`).
2. `FpsSampler.doFrame()` calculates compositor frame count every 1000ms via `Choreographer.postFrameCallback` on the Main Looper (`app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt:61-79`).
3. `StatsCollector.sampleMetrics()` queries `ActivityManager` for RAM, `BatteryManager` for temp/level, `DisplayManager` for Hz, and opens a short-lived socket to `8.8.8.8:53` for network ping (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:67-88`).
4. `StatsCollector._metrics.value` emits a new `DeviceMetrics` instance into `StateFlow<DeviceMetrics>` (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:55`).
5. `HudOverlayService` or `DashboardViewModel` collects `metrics` via `collectAsState()` and recomposes `HudOverlayView` or `DashboardScreen` (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:130-137`).

### Auto-HUD Game Launch Flow

1. User opens a tracked game (e.g., `com.pubg.imobile`).
2. `ForegroundGameDetector.checkForegroundApp()` detects `UsageEvents.Event.ACTIVITY_RESUMED` (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt:90`).
3. Detector fires callback `onGameLaunched(packageName)` (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt:115`).
4. `DashboardViewModel` triggers `HudOverlayService.start(context)` if overlay permission is granted (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:60-63`).
5. `DashboardViewModel` invokes `ramOptimizer.purgeMemory(packageName)` and starts `ramOptimizer.startContinuousSuppression(viewModelScope, packageName)` (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:66-72`).
6. `HudOverlayService.onStartCommand()` puts the service in foreground with `startForeground()`, attaches `ComposeView` to `WindowManager`, and begins sampling (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:82-87`).

### Manual RAM Purge & Turbo Flow

1. User clicks "PURGE RAM & LAUNCH BGMI" on `DashboardScreen` (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt:251-286`).
2. Handler invokes `DashboardViewModel.launchBgmiWithTurbo()` (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:118`).
3. `RamOptimizer.purgeMemory()` queries all installed apps via `PackageManager`, evaluates `AppWhitelist.isProtected()`, and calls `ActivityManager.killBackgroundProcesses()` on unwhitelisted packages (`app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt:53-71`).
4. Optimizer triggers `System.gc()`, pauses 200ms for kernel LMKD reclamation, computes `freedMb`, and returns `CleanResult` (`app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt:74-90`).
5. Intent `HudOverlayService.start(context)` is dispatched and BGMI launch intent is executed via `context.startActivity()` (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:129-143`).

**State Management:**
- UI state is represented as immutable data class `DashboardUiState` managed inside `DashboardViewModel` via private `MutableStateFlow` and exposed as read-only `StateFlow`.
- Real-time hardware telemetry is managed inside `StatsCollector` via `StateFlow<DeviceMetrics>`.
- Overlay drag coordinates and expansion states are maintained locally in Compose using `remember { mutableStateOf(...) }` inside `HudOverlayView` and window layout params in `HudOverlayService`.

## Key Abstractions

**`ComposeLifecycleOwner`:**
- Purpose: Provides a bridge allowing Jetpack Compose to run inside an Android `Service` without an underlying `Activity` or `Fragment`.
- Examples: `app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt`
- Pattern: Adapter / Composite Lifecycle Registry pattern implementing `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`.

**`AppWhitelist`:**
- Purpose: Safeguards device stability during aggressive background RAM purge routines.
- Examples: `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt`
- Pattern: Policy / Specification pattern filtering packages by category (system core, launcher, keyboard IME, target game, user custom whitelist).

**`DeviceMetrics`:**
- Purpose: Normalized hardware state snapshot encapsulating CPU/GPU compositor frame rate, memory footprint, battery thermals, and latency.
- Examples: `app/src/main/java/com/xeno/gfx/core/model/DeviceMetrics.kt`
- Pattern: Value Object / Snapshot pattern.

## Entry Points

**`XenoGfxApp`:**
- Location: `app/src/main/java/com/xeno/gfx/XenoGfxApp.kt`
- Triggers: Android system application startup.
- Responsibilities: Creates the persistent notification channel (`xeno_gfx_hud_channel`) required for foreground services on Android 8.0+ (API 26+).

**`MainActivity`:**
- Location: `app/src/main/java/com/xeno/gfx/ui/MainActivity.kt`
- Triggers: User tapping app icon from system launcher (`android.intent.action.MAIN`).
- Responsibilities: Hosts `DashboardScreen` composable, handles activity resume/pause lifecycle hooks, initiates permission requests.

**`HudOverlayService`:**
- Location: `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`
- Triggers: Explicit service intent with `ACTION_START` or `ACTION_STOP` sent by `DashboardViewModel` or notification actions.
- Responsibilities: Promotes process to foreground service (`startForeground`), registers floating overlay with `WindowManager`, starts metrics sampling, binds drag gesture handlers.

## Architectural Constraints

- **Threading:**
  - Compositor frame tracking (`Choreographer.FrameCallback`) requires execution on `Looper.getMainLooper()`.
  - Android IPC and blocking operations (such as `Socket.connect` for latency ping, `UsageStatsManager.queryEvents`, and `packageManager.getInstalledApplications`) must run on `Dispatchers.IO`.
  - All Compose UI updates must occur on `Dispatchers.Main`.
- **Global State:**
  - `AppWhitelist` (`app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt`): Static singleton object with immutable sets for `CRITICAL_SYSTEM_PACKAGES` and `SUPPORTED_BGMI_VARIANTS`.
  - `PermissionHelper` (`app/src/main/java/com/xeno/gfx/core/permission/PermissionHelper.kt`): Stateless utility object.
- **Circular Imports:** None detected. Architecture follows a clean unidirectional dependency graph from UI -> Service/ViewModel -> Core -> Model.
- **Service Type Declaration:** Android 14+ (API 34) and Android 15 (API 35) require `foregroundServiceType="specialUse"` with `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" .../>` declared in `AndroidManifest.xml`.
- **Window Overlay Permissions:** Adding views with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` throws a runtime `BadTokenException` if `Settings.canDrawOverlays(context)` is false.

## Anti-Patterns

### Redundant Dual Telemetry Collectors

**What happens:** Both `DashboardViewModel` (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:39`) and `HudOverlayService` (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:61`) instantiate their own independent `StatsCollector` instances.
**Why it's wrong:** When both the dashboard and the overlay are active, two separate coroutine jobs query memory info and create TCP sockets to `8.8.8.8:53` every second. Furthermore, two separate `FpsSampler` instances attach to `Choreographer`.
**Do this instead:** Extract `StatsCollector` into a shared repository or application-scoped singleton (or inject via Hilt per `README.md`) so that a single telemetry stream is shared across all consumers via `SharedFlow` or `StateFlow`.

### Hardcoded Game Package References

**What happens:** Target package `"com.pubg.imobile"` is hardcoded as default parameter in `RamOptimizer.purgeMemory` (`app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt:48`) and referenced directly in `DashboardViewModel` (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:44`).
**Why it's wrong:** Tightly couples the optimization pipeline to BGMI instead of supporting dynamic selection or custom games selected by the user in `trackedGamePackages`.
**Do this instead:** Pass the active foreground package dynamically from `ForegroundGameDetector` or user configuration, using `AppWhitelist.SUPPORTED_BGMI_VARIANTS` as defaults rather than a static parameter.

### Synchronous Socket Ping in Polling Loop

**What happens:** `StatsCollector.sampleNetworkLatency()` creates a blocking `Socket()` connection to `8.8.8.8:53` with a 500ms timeout synchronously inside `sampleMetrics()` (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:141-144`).
**Why it's wrong:** If network connectivity is slow or dropped, this blocks the entire 1000ms sampling coroutine for up to 500ms, causing jitter in RAM and thermal update intervals.
**Do this instead:** Decouple latency sampling into an independent concurrent coroutine job or use Android `NetworkCapabilities` / asynchronous ICMP ping.

## Error Handling

**Strategy:** Defensive containment. Low-level system service calls that may throw `SecurityException` (due to missing or revoked permissions) or IPC exceptions are guarded with try-catch blocks that return fallback defaults.

**Patterns:**
- Permission exception silencing: Catching `SecurityException` when querying `UsageStatsManager` (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt:78`) or executing `killBackgroundProcesses` (`app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt:67`).
- Network error recovery: Catching generic `Exception` in socket latency check and returning `null` to represent offline status (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:145-147`).
- Window manager failure recovery: Catching `Exception` when adding overlay view and calling `stopSelf()` if the overlay cannot be displayed (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:180-183`).

## Cross-Cutting Concerns

**Logging:**
- Current logging relies on native `e.printStackTrace()` in `HudOverlayService`.
- Standard Android `Log.d` / `Log.e` or a structured logging library (e.g. JakeWharton Timber) is recommended for production builds.

**Validation & Permissions:**
- Managed centrally by `PermissionHelper` (`app/src/main/java/com/xeno/gfx/core/permission/PermissionHelper.kt`).
- Dual check for `Settings.canDrawOverlays` (System Alert Window) and `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS)` (Usage Access).
- Deep-links directly to Android OS system management settings via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` and `Settings.ACTION_USAGE_ACCESS_SETTINGS`.

**Authentication:**
- Not applicable. Local client-side utility running fully on-device without cloud user authentication.

---

*Architecture analysis: 2026-11-05*
