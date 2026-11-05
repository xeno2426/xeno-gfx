---
last_mapped_commit: 3db9ba3d13768e0c9b05d70867d81a71845a1d8e
last_mapped_at: 2026-11-05
---
# Codebase Concerns

**Analysis Date:** 2026-11-05

## Tech Debt

**Foreground Game Detection Tied to Activity ViewModel Lifecycle:**
- Issue: `ForegroundGameDetector` and `RamOptimizer.startContinuousSuppression()` are owned by and launched in `DashboardViewModel` using `viewModelScope`.
- Files: `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`, `app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt`
- Impact: When a user launches BGMI or any game, `MainActivity` moves to the background. On memory-constrained devices (especially 4GB RAM devices targeted by the app), Android aggressively terminates background activities. When `MainActivity` is destroyed, `viewModelScope` is cancelled. Game detection immediately halts, continuous background suppression stops, and the app fails to detect when the game exits or if another game starts.
- Fix approach: Decouple `ForegroundGameDetector` from `DashboardViewModel`. Run detection inside an independent background service or coordinate directly through `HudOverlayService` (which is already an Android Foreground Service with a persistent notification).

**Dual Independent Telemetry Collectors Violating Single Source of Truth:**
- Issue: Both `DashboardViewModel` and `HudOverlayService` independently instantiate separate instances of `StatsCollector` (`statsCollector = StatsCollector(context)`).
- Files: `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`, `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`, `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`
- Impact: When the user views the dashboard while the HUD overlay is active, both instances sample hardware telemetry concurrently. This results in duplicate TCP socket connections every second, duplicate broadcast receivers querying battery state, and duplicate Choreographer frame callbacks, creating unnecessary system overhead and battery drain.
- Fix approach: Create a centralized repository or singleton service (`StatsRepository`) injected via Dependency Injection or service binding to share a single reactive `StateFlow<DeviceMetrics>` stream across both the dashboard UI and the overlay service.

**Missing Data Persistence Layer (Zero Local Storage):**
- Issue: Although `README.md` documents "Local Storage: Room + DataStore" and "DI: Hilt", neither Room, DataStore, nor Hilt is declared in `gradle/libs.versions.toml` or `app/build.gradle.kts`.
- Files: `README.md`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`
- Impact: All user settings (auto-HUD enabled toggle, continuous suppression toggle, tracked game selections, whitelisted applications) are stored exclusively in in-memory state (`DashboardUiState`). Every time the app process terminates, all configuration is wiped.
- Fix approach: Add `androidx-datastore-preferences` to `gradle/libs.versions.toml` and persist HUD configurations, continuous suppression state, and user-selected package sets across process restarts.

**Ignored Configuration Model in HUD View:**
- Issue: `HudConfig` defines user customization flags (`isFpsMeterEnabled`, `isRamMeterEnabled`, `isBatteryMeterEnabled`, `isTempMeterEnabled`, `isLatencyMeterEnabled`, `isCompactMode`, `opacity`), but `HudOverlayView.kt` does not read or apply any of these flags.
- Files: `app/src/main/java/com/xeno/gfx/core/model/HudConfig.kt`, `app/src/main/java/com/xeno/gfx/ui/overlay/HudOverlayView.kt`, `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`
- Impact: The overlay view renders a rigid hardcoded UI layout regardless of user configuration, making `HudConfig` dead configuration code.
- Fix approach: Refactor `HudOverlayView` to conditionally render telemetry pills and detail rows according to the flags in `HudConfig`, and bind the configuration to DataStore preferences.

**Orphan Manifest Permissions:**
- Issue: Manifest declares `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />`, but there is no `BroadcastReceiver` registered in `AndroidManifest.xml` or code to listen for device boot.
- Files: `app/src/main/AndroidManifest.xml`
- Impact: Unnecessary permissions requested from the user without providing any boot-recovery functionality; triggers Play Store review scrutiny.
- Fix approach: Either implement a `BootReceiver` that restores the foreground service or auto-detection if enabled by the user, or remove `RECEIVE_BOOT_COMPLETED` from `AndroidManifest.xml`.

---

## Known Bugs

**Compose View Touch Event Starvation via Raw OnTouchListener:**
- Symptoms: The expand/collapse button and close button on the floating HUD overlay are unresponsive or fail to register clicks consistently.
- Files: `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`, `app/src/main/java/com/xeno/gfx/ui/overlay/HudOverlayView.kt`
- Trigger: Touching or tapping any interactive Compose element (such as the collapse chevron icon or close button) in the floating overlay.
- Cause: In `HudOverlayService.kt` (lines 147-175), `composeView.setOnTouchListener` returns `true` for `ACTION_DOWN`, `ACTION_MOVE`, and `ACTION_UP`. In Android's view hierarchy, returning `true` from an `OnTouchListener` prevents `View.onTouchEvent(event)` from ever being called. Compose relies on `onTouchEvent` for pointer input dispatch. Returning `true` starves Compose pointer input. Calling `composeView.performClick()` only triggers view-level click listeners, not internal Compose clickables.
- Workaround: Stop the HUD service exclusively via the persistent system notification's "Stop Monitor" action.
- Fix: Replace the raw `View.OnTouchListener` drag logic with Compose-native dragging (`Modifier.pointerInput` with `detectDragGestures`), or modify `OnTouchListener` to only consume events when drag displacement exceeds `ViewConfiguration.get(context).scaledTouchSlop`.

**Misleading Real-time FPS Telemetry via Host Process Choreographer:**
- Symptoms: The "Live FPS" meter in the HUD overlay does not reflect in-game frame drops or lag spikes in BGMI; it reports the compositor refresh rate (e.g. 60 FPS) even when the game is dropping frames.
- Files: `app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt`, `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`
- Trigger: Running any demanding 3D game with the overlay active.
- Cause: `FpsSampler` calls `Choreographer.getInstance().postFrameCallback(...)`. In Android, `Choreographer` callbacks run on the calling app's UI thread (`com.xeno.gfx`), measuring only the overlay window's own VSYNC scheduling cadence. Due to Android application sandboxing, third-party apps cannot measure the render loop or frame times of another application's process without elevated permissions (root, Shizuku, or `dumpsys gfxinfo` via adb).
- Workaround: Clarify in the UI that the meter shows display refresh synchronization rather than native game rendering pipeline FPS, or integrate Shizuku / ADB shell `dumpsys SurfaceFlinger` / `dumpsys gfxinfo` for true per-app frame pacing.

**Missing Package Visibility Queries on Android 11+ (API 30+):**
- Symptoms: BGMI status is detected as "Not Installed" (`isBgmiInstalled = false`) even when installed, and `installedApps` list in the dashboard is incomplete on modern devices.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`
- Trigger: Running the application on Android 11 through Android 15 (target SDK is 35).
- Cause: Since Android 11 (API 30), package visibility filtering prevents applications from seeing other installed applications unless explicitly declared in `<queries>` inside `AndroidManifest.xml` or holding `QUERY_ALL_PACKAGES`. Neither `<queries>` nor `QUERY_ALL_PACKAGES` is present in `AndroidManifest.xml`.
- Workaround: Manually start the game from the launcher and toggle the HUD manually.
- Fix: Add explicit `<queries>` tags in `app/src/main/AndroidManifest.xml` for supported game packages (`com.pubg.imobile`, `com.tencent.ig`, etc.) and launcher intent queries (`<intent><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent>`).

**Unbounded UsageEvents Query Window Accumulation:**
- Symptoms: High CPU usage, battery drain, and eventual frame drops in `ForegroundGameDetector` the longer a game session lasts.
- Files: `app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt`
- Trigger: Playing a game continuously for an extended duration without switching activities.
- Cause: In `ForegroundGameDetector.kt`, `lastQueryTimestamp` is only updated when an `ACTIVITY_RESUMED` event is parsed (`lastQueryTimestamp = latestTimestamp`). If a user remains in the same game, no new `ACTIVITY_RESUMED` events are generated. Consequently, `latestTimestamp` is never updated, and `lastQueryTimestamp` remains stuck in the past. Every 1.5 seconds, `usageStatsManager.queryEvents(lastQueryTimestamp, now)` queries an ever-expanding time window (1 minute, 10 minutes, 1 hour), repeatedly parsing past events on every iteration.
- Workaround: None without restarting the app.
- Fix: Advance `lastQueryTimestamp = now` at the end of every polling cycle regardless of whether new resume events were found.

**Inaccurate Process Termination Count Reporting:**
- Symptoms: Dashboard reports that dozens of background apps were "purged" (e.g. "Purged 74 background apps") even on a newly booted device where those apps were never running.
- Files: `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`
- Trigger: Tapping "PURGE RAM & LAUNCH BGMI" or running continuous suppression.
- Cause: `purgeMemory()` iterates over every package returned by `packageManager.getInstalledApplications()` and calls `activityManager?.killBackgroundProcesses(pkg)`. It increments `terminatedCount++` merely because `killBackgroundProcesses` did not throw an exception. `killBackgroundProcesses` is an asynchronous void method that executes regardless of whether the app was running or cached.
- Workaround: None in current code.
- Fix: Inspect running processes via `activityManager.runningAppProcesses` before and after invocation, or report recovered megabytes rather than misleading process counts.

**Arbitrary Alpha Selection of Default Tracked Games:**
- Symptoms: On first launch, the app arbitrarily selects random apps (e.g., banking or utility apps) as games to track with Auto-HUD.
- Files: `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`
- Trigger: Fresh app installation.
- Cause: Line 209 in `DashboardViewModel.kt` sets `trackedGamePackages = games.take(3).map { it.packageName }.toSet()`, which takes the first 3 apps alphabetically from all launcher apps.
- Workaround: Manually uncheck the first 3 apps in the dashboard list.
- Fix: Match packages against `AppWhitelist.SUPPORTED_BGMI_VARIANTS` or `ApplicationInfo.CATEGORY_GAME` (`appInfo.category == ApplicationInfo.CATEGORY_GAME`) instead of blindly taking the first 3 items.

---

## Security Considerations

**Unprotected OEM System Processes During RAM Purge:**
- Risk: System instability, telephony interruptions, or audio glitches during background suppression.
- Files: `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt`, `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`
- Current mitigation: A hardcoded list of 9 Google/AOSP package names and packages starting with `com.android.systemui` are whitelisted.
- Recommendations: Check `(appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0` to protect all system and pre-installed OEM apps (Samsung `com.sec.*`, Xiaomi `com.miui.*`, Oppo/Realme `com.coloros.*`, Vivo `com.vivo.*`). Add audio playback and media session checks so music services (Spotify, YouTube Music) and voice chat apps (Discord) are not terminated during gameplay.

**Unencrypted Cleartext Network Probe to Hardcoded External IP:**
- Risk: Cleartext network leakage and failure on restricted/IPv6 networks.
- Files: `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`
- Current mitigation: None. Opens a raw TCP socket to `8.8.8.8:53` every second.
- Recommendations: Hardcoding external DNS IP `8.8.8.8` triggers network inspection flags in enterprise environments, violates zero-telemetry expectations, and fails on IPv6-only cellular networks without 464XLAT. Replace with ICMP ping via `Runtime.getRuntime().exec("ping -c 1 ...")` or an HTTP/HTTPS head request to user-configurable endpoints, or allow users to toggle latency monitoring off.

**Unminified Debug Builds Shipped as Release Binaries:**
- Risk: Exposure of internal symbols, reverse-engineering vulnerability, and oversized distribution APKs.
- Files: `.github/workflows/release.yml`, `app/build.gradle.kts`
- Current mitigation: `isMinifyEnabled = true` is configured in `buildTypes.release`.
- Recommendations: The CI workflow (`.github/workflows/release.yml`) builds `./gradlew assembleDebug` and renames `app-debug.apk` to `xeno-gfx.apk` for release tags. The shipped APKs are debuggable (`isDebuggable = true`), signed with debug keys, and lack R8 optimization. Change CI to build `./gradlew assembleRelease` with valid production signing keys.

---

## Performance Bottlenecks

**Periodic Full-Device Package Scan and Kill Loop:**
- Problem: Background app suppression loop creates CPU spikes and micro-stutters during gaming.
- Files: `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`
- Cause: `startContinuousSuppression` runs every 30 seconds. On each tick, it calls `packageManager.getInstalledApplications(PackageManager.GET_META_DATA)`, which issues a heavy Binder IPC transaction querying hundreds of apps, resolves launcher and IME packages, and calls `killBackgroundProcesses` across all non-whitelisted apps.
- Improvement path: Cache installed app metadata upon app start rather than re-querying `PackageManager` every 30 seconds. Furthermore, on Android 14+ (API 34), repeatedly killing background processes causes apps with registered receivers or job schedulers to immediately restart, consuming CPU cycles and actively harming gaming performance. Suppress only once before launching the game, or allow user-triggered manual purges.

**Continuous Radio Wakeup via 1-Second TCP Latency Ping:**
- Problem: Severe battery drain and cellular radio state lock.
- Files: `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`
- Cause: `sampleNetworkLatency()` opens and closes a new `Socket` to `8.8.8.8:53` every 1000ms. Mobile network interfaces stay in high-power active mode (`DCH` state) when traffic occurs every second, preventing the modem from entering low-power idle states.
- Improvement path: Increase the ping interval to 5000ms or 10000ms, or measure latency over existing connections rather than opening fresh TCP handshakes every second.

**Continuous Binder IPC via Battery Intent Registration:**
- Problem: Unnecessary IPC roundtrips on every telemetry cycle.
- Files: `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`
- Cause: `context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))` is called every single second.
- Improvement path: Register a persistent `BroadcastReceiver` for `ACTION_BATTERY_CHANGED` once when monitoring starts, store battery state in memory, and unregister when monitoring stops.

**Choreographer FrameCallback Loop Overhead:**
- Problem: Continuous UI thread wakeups at display refresh rate.
- Files: `app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt`
- Cause: `FpsSampler` re-posts `choreographer.postFrameCallback(this)` on every frame (60–120Hz). Even when the HUD view does not need recomposition, the UI thread must dispatch callbacks every 8–16ms.
- Improvement path: Sample frame rates only when the HUD is visible and rendering, or throttle callback scheduling.

---

## Fragile Areas

**Floating Overlay WindowManager Lifecycle & Window Tokens:**
- Files: `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`, `app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt`
- Why fragile: Hosting a `ComposeView` directly in `WindowManager` outside an `Activity` requires manual simulation of `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`. If `windowManager.addView()` is called when the overlay permission is revoked or in background restrictions on custom OEM ROMs (MIUI/HyperOS, ColorOS), it throws fatal exceptions (`BadTokenException`, `SecurityException`).
- Safe modification: Wrap `windowManager.addView` and `windowManager.updateViewLayout` in comprehensive error handlers, and check `Settings.canDrawOverlays(context)` immediately before any window manipulation.
- Test coverage: Zero automated UI or instrumentation tests for service window management or ComposeLifecycleOwner.

**Overlay Dragging and Orientation Handling:**
- Files: `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`
- Why fragile: `windowLayoutParams` coordinates (`x = 80, y = 120`) use fixed pixel offsets. When games rotate between portrait and landscape (BGMI runs strictly in landscape), the HUD overlay coordinates can be thrown off-screen or overlap notch/camera cutouts.
- Safe modification: Clamp coordinates to display metrics using `WindowMetrics` (or `Display.getRealMetrics`), and register an orientation change listener to reposition the HUD within visible screen boundaries upon rotation.
- Test coverage: Gaps: No tests covering screen rotation or bounds clamping.

**Repository Bloat with Binary APKs:**
- Files: `xeno-gfx.apk`, `xenp-gfx.apk`
- Why fragile: Two 16.5MB pre-built APK binaries are committed directly to the git repository root.
- Safe modification: Remove both APKs from git history using `git rm`, add `*.apk` to `.gitignore` enforcement, and rely strictly on GitHub Actions release artifacts for binary distribution.

---

## Scaling Limits

**Installed Applications Scalability:**
- Current capacity: Tested with small application sets in unit tests.
- Limit: Devices with 300+ installed applications experience noticeable UI freezes and coroutine latency in `loadInstalledLauncherApps()` and `purgeMemory()`.
- Scaling path: Move app loading to pagination, offload icon loading to Coil / asynchronous drawable loaders, and avoid full metadata scans during RAM cleanup.

**UsageEvents Query Interval:**
- Current capacity: 1500ms polling rate with a fixed lookback window.
- Limit: If the polling loop falls behind or if event queries lag, querying usage stats stalls the IO dispatcher.
- Scaling path: Implement an adaptive polling interval (e.g. 3000ms while idle, 1500ms when game launch is imminent).

---

## Dependencies at Risk

**Android Gradle Plugin & Gradle Daemon Compatibility:**
- Risk: AGP version `8.7.2` with Kotlin `2.0.21` requires Java 21 runtime.
- Files: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Impact: Developers or CI runners on Java 17 fail during build configuration.
- Migration plan: Maintain Java 21 toolchain specification in `build.gradle.kts` and document minimum JDK requirement in `README.md`.

**Lack of Modern Dependency Injection:**
- Risk: Direct instantiation of services and managers (`StatsCollector(context)`, `RamOptimizer(context)`) creates tight coupling.
- Files: `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`, `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`
- Impact: Testing and swapping telemetry backends is difficult; leads to memory leaks if services hold activity or context references.
- Migration plan: Introduce Hilt or Koin to inject singletons (`Context`, `StatsCollector`, `RamOptimizer`).

---

## Missing Critical Features

**Per-Game Graphics Tuning (GFX Tool Functionality):**
- Problem: Despite being named "Xeno GFX", there is no graphics profile configuration, resolution scaling, unlocker for 90 FPS/120 FPS modes, or configuration file modifier (e.g. Unreal Engine `UserCustom.ini` or `Active.sav` tuning for BGMI).
- Blocks: The app currently acts only as a device telemetry overlay and RAM cleaner, missing core GFX optimization tools.

**Persistent User Configuration and Custom Whitelist:**
- Problem: No UI exists for users to customize the whitelist (e.g., preserving Spotify, Discord, or WhatsApp during RAM cleanup), and all settings reset upon app exit.
- Blocks: Users cannot prevent essential background apps from being terminated when using the RAM Turbo feature.

**Background Auto-Detection Foreground Service:**
- Problem: Auto-HUD detection is disabled as soon as `MainActivity` is closed because it runs in `viewModelScope`.
- Blocks: True automatic HUD detection when launching games from the home screen without having Xeno GFX actively open in the foreground.

---

## Test Coverage Gaps

**ForegroundGameDetector Logic:**
- What's not tested: `checkForegroundApp()`, event timestamp tracking, and callback invocations (`onGameLaunched`, `onGameExited`).
- Files: `app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt`
- Risk: Game detection silently fails or accumulates unbounded query windows.
- Priority: High

**RamOptimizer Process Eviction Logic:**
- What's not tested: `purgeMemory()`, `startContinuousSuppression()`, memory calculation edge cases, and whitelist integration with actual contexts.
- Files: `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`
- Risk: Killing critical system apps or throwing uncaught security exceptions on customized Android ROMs.
- Priority: High

**StatsCollector Hardware Telemetry:**
- What's not tested: Battery status parsing, thermal status mapping, network timeout handling, and refresh rate fallback.
- Files: `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`, `app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt`
- Risk: Telemetry crashes on devices lacking thermal sensors or running on IPv6 networks.
- Priority: Medium

**Overlay Touch & UI Interaction:**
- What's not tested: ComposeView touch listeners, dragging calculation, expand/collapse toggling, and close button actions.
- Files: `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`, `app/src/main/java/com/xeno/gfx/ui/overlay/HudOverlayView.kt`
- Risk: Unresponsive overlay controls for end users.
- Priority: High

---

*Concerns audit: 2026-11-05*
