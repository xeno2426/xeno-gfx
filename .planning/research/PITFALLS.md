# Domain Pitfalls: Android Gaming Performance Optimizer & BGMI 30 FPS Turbo

**Domain:** Android Gaming Optimizer, Real-Time HUD Overlay, & GFX Config Management  
**Target Environment:** Android 8.0 (API 26) through Android 15 (API 35) on Low-RAM (3GB/4GB) Devices  
**Researched:** 2026-11-05  
**Overall Confidence:** HIGH (Verified against official Android API 26-35 documentation, Android 14/15 behavioral restrictions, AOSP lmkd/binder mechanics, and Krafton anti-cheat policies)

---

## Executive Summary

Engineering a high-performance gaming optimizer and real-time floating HUD for memory-constrained Android devices without root or Shizuku requires navigating severe OS sandbox restrictions and vendor-specific edge cases across Android 8 through Android 15. The most dangerous traps in this domain involve implementing features that appear standard on older Android releases or rooted platforms, but behave destructively or fail completely on modern Android builds.

Specifically, five foundational design pitfalls threaten project viability:
1. **Android 14+ (API 34/35) Total Ineffectiveness of Cross-App `killBackgroundProcesses`:** Starting in Android 14, the OS restricts `ActivityManager.killBackgroundProcesses()` exclusively to the caller's own package. Passing third-party package names fails silently, completely breaking non-root RAM purgers.
2. **Cyclic Restart Thrashing from 30s Periodic Process Eviction:** On Android 8–13 where cross-app killing functions, terminating background cached apps every 30 seconds triggers immediate OS-level process recreation (Zygote fork, ART JIT warmup, SQLite initialization), causing severe CPU micro-stuttering and disk thrashing during gameplay—the exact opposite of a gaming turbo.
3. **Scoped Storage Total Lockdown of `Android/data` & Krafton Anti-Cheat 10-Year Bans:** Standard non-root applications cannot read or write to `Android/data/com.pubg.imobile/` on Android 11–15 via File APIs or SAF. Furthermore, modifying the binary `Active.sav` config file triggers Krafton's server-side client integrity checks, resulting in permanent 10-year player bans.
4. **Choreographer VSYNC Telemetry vs. Game Render Truth:** Third-party non-root apps cannot hook another app's render thread or SurfaceFlinger transactions. Choreographer frame callbacks in an overlay service measure the overlay window's own display VSYNC cadence (reporting a fake 60/90/120 FPS), completely masking in-game frame drops.
5. **Thermal Protection Inversion:** Executing a CPU-intensive background RAM purge when device temperatures spike pushes an already overheated SoC past thermal limits, triggering severe hardware throttling and potential system shutdown.

Addressing these pitfalls early in the roadmap ensures Xeno GFX delivers genuine, measurable frame pacing and memory headroom without risking user bans, app crashes, or OS-level process termination.

---

## Critical Pitfalls

Mistakes that cause system-level failure, feature invalidation, user account bans, or architectural rewrites.

---

### Pitfall 1: Android 14+ (API 34/35) Deprecation of Cross-App `killBackgroundProcesses`

- **What goes wrong:** Calling `ActivityManager.killBackgroundProcesses(packageName)` with any package name other than `com.xeno.gfx` fails silently on Android 14 and Android 15. The "Full Throttle 30s RAM Killer" and "Purge RAM Now" features reclaim zero external memory on modern devices.
- **Why it happens:** In Android 14 (API 34), Google instituted a global OS-level security and stability policy: apps can only kill background processes within their own package. Calling this method for external applications is logged as `W/ActivityManager: Invalid packageName: <target>` and dropped by `ActivityManagerService`. This restriction applies to all apps running on Android 14+, regardless of `targetSdkVersion`.
- **Consequences:** The app displays deceptive reports (e.g. "Purged 74 background apps") while actual available RAM does not increase. Users and reviewers quickly identify that the memory cleaner does not work, resulting in 1-star reviews and potential Google Play Store suspensions for deceptive behavior.
- **Warning signs:**
  - Testing on Android 14 (API 34) or Android 15 (API 35) shows `W/ActivityManager: Invalid packageName:` in logcat.
  - `ActivityManager.MemoryInfo.availMem` before and after `purgeMemory()` is identical (0 MB delta).
- **Prevention strategy:**
  - Acknowledge OS constraints directly in architecture: on API 34+, non-root cross-app killing is impossible through public APIs.
  - Guard the process termination loop:
    ```kotlin
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        // Android 8.0 - 13: Execute guarded killBackgroundProcesses on LRU cached apps
        activityManager.killBackgroundProcesses(targetPackage)
    } else {
        // Android 14+: Trim internal application memory & invoke ART GC
        context.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        System.gc()
    }
    ```
  - Shift optimizer strategy on Android 12+ (API 31+) to system-supported game APIs like `GameManager.setGameMode(GameManager.GAME_MODE_PERFORMANCE)` where supported.
  - Calculate `freedMb` strictly from delta in `ActivityManager.MemoryInfo.availMem`. If `freedMb <= 0`, accurately report "RAM Optimized (Cached apps settled)" rather than inflating terminated app counts.
- **Which phase should address it:** RAM Suppression & Process Management Phase.

---

### Pitfall 2: Scoped Storage Lockdown of `Android/data` & Krafton Anti-Cheat 10-Year Bans

- **What goes wrong:** Attempting to modify or replace `Active.sav` in `/storage/emulated/0/Android/data/com.pubg.imobile/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/` fails on Android 11–15 with `Access Denied` / `FileNotFoundException`. Even if bypassed via Shizuku or PC transfer, modifying `Active.sav` triggers Krafton's anti-cheat integrity check, permanently banning user accounts for 10 years.
- **Why it happens:**
  1. *OS Restriction:* Android 11 introduced Scoped Storage. Android 13, 14, and 15 progressively eliminated all Storage Access Framework (SAF) tree picker bypasses in `DocumentsUI`. Third-party apps cannot obtain read or write URIs to `Android/data` or `Android/obb` subfolders. `MANAGE_EXTERNAL_STORAGE` strictly excludes other packages' directories under `Android/data`.
  2. *Anti-Cheat Policy:* BGMI / PUBG Mobile uses Tencent ACE and Krafton security modules. Client configuration files like `Active.sav` contain binary serialized Unreal Engine `USaveGame` properties. Server-side hash and checksum validation detects unauthorized edits (e.g. 90/120 FPS unlocks, custom rendering scales, or modified shadow parameters). Krafton enforces a strict zero-tolerance policy against local file tampering.
- **Consequences:** 100% feature failure on standard Android 11–15 non-root devices. For users running older Android versions or manual workarounds, their primary game accounts suffer 10-year bans, creating catastrophic liability and brand destruction for Xeno GFX.
- **Warning signs:**
  - `File.canWrite()` returns `false` on target `Active.sav` paths.
  - SAF `ACTION_OPEN_DOCUMENT_TREE` rejects user selection with "To protect your privacy, choose another folder".
  - Game client reports "Data changed. Please login again" or delivers a 3650-day suspension notice upon logging into game servers.
- **Prevention strategy:**
  - **Eliminate automatic raw file injection into `Android/data` from the non-root roadmap.**
  - Reframe the GFX Tool feature:
    1. Provide safe, verified graphics setting advice and optimal device profiles that users set through the official in-game graphics menu.
    2. Offer exportable configuration files to public directories (e.g., `Downloads/XenoGFX/`) strictly for advanced users utilizing external ADB/PC tooling, with mandatory high-visibility warnings regarding Krafton's anti-cheat terms and ban risks.
    3. Never claim "100% Non-Root One-Click Active.sav Injector" on Android 11–15, as this is technically impossible without privileged file exploits.
- **Which phase should address it:** GFX Configuration & Storage Access Phase.

---

### Pitfall 3: Cyclic Process Restart Thrashing from 30s Periodic Eviction

- **What goes wrong:** Running a continuous background RAM killer every 30 seconds induces severe micro-stuttering, sudden frame time spikes (jank), and rapid battery drain in BGMI instead of boosting performance.
- **Why it happens:** When `killBackgroundProcesses` is executed on Android 8–13, it terminates processes in the `CACHED` state. However, background applications frequently have registered broadcast receivers (FCM push notifications, network changes), sticky services (`START_STICKY`), or scheduled tasks (`JobScheduler` / `WorkManager`). The Android `system_server` immediately forks a new process from Zygote to resurrect these components. Each cold restart consumes heavy CPU bursts (JIT compilation, class loading, SQLite verification, IPC). Repeating this cycle across 30–50 apps every 30 seconds keeps CPU utilization pegged at 100% and saturates flash memory I/O.
- **Consequences:** In-game framerate drops violently every 30 seconds as the kernel scheduler preempts the game's render thread to handle background app restarts. Battery temperature rises rapidly.
- **Warning signs:**
  - CPU profiler shows recurring utilization spikes every 30 seconds synchronous with `startContinuousSuppression`.
  - Android logcat displays a rapid cascade of `ActivityManager: Start proc ... for restart` immediately after `killBackgroundProcesses` calls.
  - User experiences rhythmic frame drops every 30 seconds during active matches.
- **Prevention strategy:**
  - **Abolish blind periodic 30-second kill loops.**
  - Replace with a **Single Pre-Launch Purge**: execute memory optimization once immediately before or upon detecting game launch, then cease active eviction.
  - Implement **Threshold-Driven Eviction**: during gameplay, only initiate cleanup if physical available RAM drops below critical levels (e.g. `availMem < totalMem * 0.12`).
  - Never kill apps possessing active audio playback (`AudioManager.isMusicActive()`), foreground services, or active notification listeners.
- **Which phase should address it:** RAM Suppression & Process Management Phase.

---

### Pitfall 4: Choreographer False Telemetry Artifacts & In-Game Stutter Invisibility

- **What goes wrong:** The HUD overlay displays a smooth 60 FPS counter (or 90/120 FPS on high-refresh displays) while the game running underneath is lagging at 18–22 FPS.
- **Why it happens:** `Choreographer.getInstance().postFrameCallback()` runs exclusively on the calling app's UI thread (`com.xeno.gfx`). It receives VSYNC pulses dispatched to the floating overlay window. Because the overlay is a small Compose window consuming minimal resources, it easily satisfies every VSYNC tick. Due to Android application sandboxing (`uid` isolation and SELinux policies), a third-party application without root or `android.permission.DUMP` (granted via ADB) cannot access another app's render thread, SurfaceFlinger frame release timings, or Vulkan swapchain cadence.
- **Consequences:** The FPS counter is completely uncalibrated to game performance. Gamers immediately recognize the meter is inaccurate when obvious visual stutter occurs while the HUD claims a stable 60 FPS.
- **Warning signs:**
  - Injecting artificial sleep or heavy rendering load into a test game does not change the FPS counter.
  - The FPS counter on a 90Hz or 120Hz display shows 90 or 120 FPS even when BGMI is configured to 30 FPS mode.
- **Prevention strategy:**
  - **Accurate Telemetry Semantics:** Clearly label the overlay readout in the UI as `"Display Refresh (Hz)"` or `"Compositor Cadence"` rather than `"Game Render FPS"`.
  - **Calibrated 30 FPS Pacing Metric:** For target 30 FPS calibration on 60Hz/90Hz/120Hz displays, calculate compositor frame-pacing jitter by analyzing frame callback timestamp intervals (`frameTimeNanos - lastFrameTimeNanos`). Quantify frame delivery variance and dropped compositor pulses rather than presenting a fabricated game framerate.
  - **Throttling & Power Preservation:** Do not run Choreographer callbacks at 120Hz when the HUD is collapsed or when live FPS display is toggled off in `HudConfig`. Pause the callback loop immediately when the overlay is hidden.
- **Which phase should address it:** Display Cadence & FPS Calibration Phase.

---

### Pitfall 5: Thermal Inversion: Triggering Heavy CPU RAM Purges During Thermal Spikes

- **What goes wrong:** When the device reaches critical thermal throttling levels, Xeno GFX triggers an "emergency RAM purge", causing SoC temperature to spike higher and pushing the device into aggressive CPU throttling or emergency thermal shutdown.
- **Why it happens:** Iterating through package lists, querying `PackageManager`, executing synchronous Binder IPCs, and triggering background app restarts generates significant CPU workload. When silicon die temperature is already hovering near the throttling threshold (75°C–80°C), executing synthetic CPU-heavy tasks adds thermal energy faster than the phone's passive cooling can dissipate it.
- **Consequences:** Game frame rates drop from 30 FPS to single digits as the thermal governor slashes CPU/GPU frequencies by 50%–70% to prevent hardware damage.
- **Warning signs:**
  - `PowerManager.currentThermalStatus` escalates from `THERMAL_STATUS_MODERATE` directly to `THERMAL_STATUS_CRITICAL` immediately after an emergency thermal purge.
  - Device backplate temperature spikes sharply during cool-down routine execution.
- **Prevention strategy:**
  - **Decouple thermal protection from RAM purging.** Emergency thermal handling must reduce system load, never increase it.
  - On thermal warnings (`THERMAL_STATUS_MODERATE` or `SEVERE`):
    1. Drop HUD telemetry polling frequency from 1000ms to 5000ms.
    2. Pause non-essential background tasks, including latency pinging and Choreographer sampling.
    3. Notify the player with a lightweight HUD indicator to temporarily lower screen brightness or game graphic settings.
    4. Never invoke `purgeMemory()` during an active high-temperature thermal state.
- **Which phase should address it:** Telemetry Architecture & Thermal Monitoring Phase.

---

## Moderate Pitfalls

Mistakes that lead to battery drain, service termination by OEM task killers, or edge-case runtime crashes.

---

### Pitfall 6: Battery Temperature Measurement Lag vs. SoC Thermal Throttling

- **What goes wrong:** In-game thermal stutter and frame drops occur 5 to 10 minutes before the app's thermal HUD indicates high temperature.
- **Why it happens:** `BatteryManager.EXTRA_TEMPERATURE` measures the temperature of the lithium-ion battery pouch. The battery has substantial thermal mass and is physically separated from the SoC. Under heavy 3D gaming load, the CPU/GPU silicon heats up in 15–30 seconds, while heat takes 5–10 minutes of continuous conduction to raise the battery temperature sensor.
- **Consequences:** The app fails to warn the user about thermal throttling before performance collapses.
- **Warning signs:** Device CPU clocks throttle while HUD battery temperature displays 34°C–36°C (safe range).
- **Prevention strategy:**
  - Prioritize `PowerManager.currentThermalStatus` (available on Android 10+ / API 29+) as the primary thermal health metric. It directly reflects system kernel thermal zones and throttling policies.
  - Register `PowerManager.OnThermalStatusChangedListener` to receive immediate OS thermal status transitions (`LIGHT`, `MODERATE`, `SEVERE`, `CRITICAL`).
  - Treat battery temperature solely as a secondary battery safety metric rather than an instantaneous gaming thermal throttle indicator.
- **Which phase should address it:** Telemetry Architecture & Thermal Monitoring Phase.

---

### Pitfall 7: Synchronous 1-Second Binder IPC Overhead in Telemetry Collection

- **What goes wrong:** The telemetry sampling loop consumes measurable CPU and battery, competing with the game for resources on 3GB RAM devices.
- **Why it happens:** In `StatsCollector.kt`, `context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))` and `activityManager.getMemoryInfo()` are called every 1000ms inside a continuous coroutine loop. Calling `registerReceiver(null, ...)` performs a synchronous Binder IPC transaction to `system_server` on every tick.
- **Consequences:** Constant context switches and Binder IPC overhead cause micro-stutters on budget chipsets (e.g. MediaTek Helio P35/G35).
- **Warning signs:** Perfetto / Systrace records show recurring `binder:reply` transactions initiated by `com.xeno.gfx` every 1000ms.
- **Prevention strategy:**
  - Register an event-driven `BroadcastReceiver` for `ACTION_BATTERY_CHANGED` once when monitoring begins; update cached battery state asynchronously when the system broadcasts updates.
  - Query `getMemoryInfo()` at relaxed intervals (e.g. every 3000ms–5000ms) during gameplay, or only refresh RAM metrics when the user actively opens the expanded HUD view.
- **Which phase should address it:** Telemetry Architecture & Thermal Monitoring Phase.

---

### Pitfall 8: Missing `RECEIVER_NOT_EXPORTED` Flag on Android 14+ (API 34/35)

- **What goes wrong:** Registering dynamic broadcast receivers crashes the app immediately on Android 14+ with `SecurityException: One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified`.
- **Why it happens:** Android 14 introduced strict enforcement requiring apps targeting API 34+ to declare whether dynamically registered broadcast receivers are exported to other apps or internal.
- **Consequences:** Immediate crash when starting telemetry collection or service monitoring on Android 14/15 devices.
- **Warning signs:** Crash log: `java.lang.SecurityException: com.xeno.gfx: One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified for a receiver if it cannot be null`.
- **Prevention strategy:**
  - Always use `ContextCompat.registerReceiver` with the appropriate export flag:
    ```kotlin
    ContextCompat.registerReceiver(
        context,
        batteryReceiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED
    )
    ```
- **Which phase should address it:** Telemetry Architecture & Thermal Monitoring Phase.

---

### Pitfall 9: Radio Wakeup & IPv6 Network Latency Failures via 1-Second TCP Sockets

- **What goes wrong:** The network latency ping consumes excessive cellular power and reports `null` (or fails) on IPv6-only mobile carrier networks.
- **Why it happens:** `StatsCollector.kt` attempts to connect a raw TCP `Socket` to `8.8.8.8:53` every 1000ms. Opening TCP sockets every second locks the cellular modem into the high-power active state (`DCH`), draining battery 3x faster. Furthermore, on IPv6-only cellular networks without 464XLAT or in regions blocking Google Public DNS (port 53), the hardcoded IPv4 address `8.8.8.8` fails with network unreachable.
- **Consequences:** Rapid battery drain; latency displays `null` or 999ms on modern 5G/4G cellular connections.
- **Prevention strategy:**
  - Increase ping sampling interval to 5000ms–10000ms.
  - Implement DNS fallback using hostnames or query active network ping times via `ConnectivityManager.getNetworkCapabilities(network)`.
  - Provide an instant toggle in `HudConfig` (`isLatencyMeterEnabled`) to allow users to disable network probing entirely.
- **Which phase should address it:** Telemetry Architecture & Thermal Monitoring Phase.

---

### Pitfall 10: Aggressive OEM Task Killers Evicting `HudOverlayService` Mid-Game

- **What goes wrong:** On Xiaomi (HyperOS/MIUI), OnePlus/Oppo (ColorOS), Samsung (OneUI), and Transsion devices, the floating HUD overlay and game detector vanish 2 to 5 minutes into a match.
- **Why it happens:** Proprietary OEM power management daemons (e.g. Xiaomi `miui.powerkeeper`, Samsung `SmartManager`) aggressively kill third-party foreground services during gaming sessions to free memory for the game, especially if the service lacks proper foreground service declarations or consumes recurring CPU.
- **Consequences:** The optimizer terminates silently mid-match; users lose overlay telemetry and must exit the game to re-launch Xeno GFX.
- **Warning signs:** Logcat shows `am_kill` with reason `excessive cpu` or process termination by OEM power management packages.
- **Prevention strategy:**
  - Declare correct Android 14 `foregroundServiceType` in `AndroidManifest.xml` (e.g. `specialUse` with clear functional description or `dataSync`).
  - Provide explicit, step-by-step user guidance on the dashboard to grant "Autostart" and set battery optimization to "No restrictions" on OEM ROMs (MIUI, ColorOS, OneUI).
  - Check `PowerManager.isIgnoringBatteryOptimizations(packageName)` and prompt user when unexempted.
  - Keep idle background CPU utilization of `HudOverlayService` strictly under 0.5%.
- **Which phase should address it:** Service Architecture & Game Detection Phase.

---

## Minor Pitfalls

Edge cases, UI touch conflicts, and display orientation quirks.

---

### Pitfall 11: Touch Event Starvation in WindowManager Compose View

- **What goes wrong:** Expand/collapse chevron and close buttons on the floating HUD overlay do not respond to taps, or require multiple aggressive presses.
- **Why it happens:** `composeView.setOnTouchListener` in `HudOverlayService.kt` returns `true` for `ACTION_DOWN`, `ACTION_MOVE`, and `ACTION_UP`. In Android view dispatching, returning `true` prevents `View.onTouchEvent(event)` from being called. Jetpack Compose relies on `onTouchEvent` to feed its internal pointer input subsystem. Starving Compose of touch events breaks all internal `Modifier.clickable` handlers.
- **Prevention strategy:**
  - Distinguish drag gestures from tap clicks using `ViewConfiguration.get(context).scaledTouchSlop`:
    ```kotlin
    var isDragging = false
    composeView.setOnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX; startY = event.rawY
                isDragging = false
                false // Allow Compose to see ACTION_DOWN
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.rawX - startX)
                val dy = abs(event.rawY - startY)
                if (dx > touchSlop || dy > touchSlop) {
                    isDragging = true
                    updateWindowCoordinates(event.rawX, event.rawY)
                    true
                } else false
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    view.performClick()
                    false // Pass through to Compose clickable
                } else true
            }
            else -> false
        }
    }
    ```
- **Which phase should address it:** Display Cadence & FPS Calibration Phase (Overlay UI).

---

### Pitfall 12: Screen Rotation Clipping in Landscape Gaming

- **What goes wrong:** When transitioning from portrait launcher to landscape BGMI, the floating HUD gets trapped off-screen or rendered behind the display notch/camera cutout.
- **Why it happens:** `WindowManager.LayoutParams` coordinates (`x = 80, y = 120`) use absolute pixel offsets. When screen orientation changes, screen width and height invert, causing coordinates valid in portrait to fall outside landscape visible bounds.
- **Prevention strategy:**
  - Register a display rotation listener via `DisplayManager.DisplayListener` or `OrientationEventListener`.
  - Clamp window coordinates to current active display metrics:
    ```kotlin
    val windowMetrics = windowManager.maximumWindowMetrics
    val screenWidth = windowMetrics.bounds.width()
    val screenHeight = windowMetrics.bounds.height()
    params.x = params.x.coerceIn(0, screenWidth - view.width)
    params.y = params.y.coerceIn(0, screenHeight - view.height)
    windowManager.updateViewLayout(view, params)
    ```
- **Which phase should address it:** Display Cadence & FPS Calibration Phase.

---

### Pitfall 13: Binary Save File Deserialization Crashes Across Game Variants

- **What goes wrong:** Attempting to parse or generate `Active.sav` across different PUBG releases (BGMI `com.pubg.imobile`, Global `com.tencent.ig`, KR `com.pubg.krmobile`, Lite `com.tencent.iglite`) results in corrupted save games or game startup crashes.
- **Why it happens:** Different game editions run on modified forks of Unreal Engine 4. The binary serialization format of `Active.sav` differs in header magic bytes, engine version tags, and property map schemas between editions.
- **Prevention strategy:**
  - If parsing binary saves, implement strict header magic checks and CRC32 payload verification.
  - Maintain isolated schema definitions per package name; never apply an active profile generated for Global to BGMI.
- **Which phase should address it:** GFX Configuration & Storage Access Phase.

---

## Phase-Specific Warnings & Roadmap Implications

| Phase Topic | Critical Pitfalls to Avoid | Severity | Actionable Mitigation |
|---|---|---|---|
| **Phase: Service Architecture & Game Detection** | OEM task killer service termination; Android 14 `foregroundServiceType` missing; Game detection tied to activity lifecycle. | **CRITICAL** | Declare `specialUse` foreground service type; decouple detector from `viewModelScope` into persistent Foreground Service; guide users through OEM battery saver exemptions. |
| **Phase: RAM Suppression & Process Management** | Android 14+ silent failure of `killBackgroundProcesses`; Cyclic restart thrashing every 30 seconds; Terminating OEM audio/voice services. | **CRITICAL** | Transition from continuous 30s kill loop to single pre-game purge; guard Android 14+ against no-op cross-app calls; verify `FLAG_SYSTEM` protection in whitelist; calculate real memory deltas. |
| **Phase: Telemetry Architecture & Thermal Monitoring** | Thermal cool-down via RAM purge (thermal inversion); Missing Android 14 broadcast export flags; Battery temperature delay vs SoC heat; 1-second Binder polling. | **CRITICAL** | Never run RAM purge on thermal warning; replace 1s battery polling with registered receiver; use `PowerManager.OnThermalStatusChangedListener` (API 29+); use `RECEIVER_NOT_EXPORTED`. |
| **Phase: Display Cadence & FPS Calibration** | Reporting fake 60 FPS via overlay Choreographer; High looper wakeup overhead at 120Hz; WindowManager Compose touch event starvation; Screen rotation clipping. | **CRITICAL** | Label telemetry accurately as "Display Cadence"; calculate frame pacing variance/jitter for 30 FPS calibration; pause Choreographer when minimized; implement slop-aware touch dispatcher. |
| **Phase: GFX Configuration & Storage Access** | Android 11–15 Scoped Storage lockdown of `Android/data`; Krafton 10-year anti-cheat account bans; Binary save file corruption. | **CRITICAL** | Exclude raw non-root `Active.sav` injection into `Android/data`; provide compliant graphic optimization advice and exportable profiles with prominent anti-cheat risk disclosures. |
| **Phase: Data Persistence & Production Hardening** | In-memory settings loss across reboots; Shipping debuggable unsigned binaries in release workflows. | **MODERATE** | Implement Jetpack DataStore preferences for persistent configuration; configure release signing and R8 minification in release CI. |

---

## Sources & Evidence

- **Android 14 Behavior Changes (System Resource Management & Task Killers):** [Official Android 14 Documentation](https://developer.android.com/about/versions/14/behavior-changes-all#kill-background-processes) - Confirms `ActivityManager.killBackgroundProcesses()` is restricted to own package on Android 14+.
- **Android Storage Access Framework & Scoped Storage Restrictions:** [Android Storage Use Cases and Best Practices](https://developer.android.com/training/data-storage) & Google Issue Tracker SAF restrictions on `Android/data` (Android 11–15).
- **Android Dynamic Performance Framework & Thermal Management:** [Android ADPF Documentation](https://developer.android.com/games/optimize/adpf) - Verifies `getThermalHeadroom()` and `PowerManager.OnThermalStatusChangedListener` APIs for proactive thermal management.
- **Unreal Engine SaveGame Architecture:** [Unreal Engine Documentation: Saving and Loading Games](https://docs.unrealengine.com/5.0/en-US/saving-and-loading-your-game-in-unreal-engine/) - Structure of `FArchive` binary serialization in `.sav` files.
- **Krafton Battlegrounds Mobile India Terms of Conduct & Anti-Cheat Policies:** [BGMI Rules of Conduct](https://www.battlegroundsmobileindia.com/) - Prohibits third-party file manipulation and specifies 10-year account bans for tampering with game client data.
- **Android Broadcast Receiver Security (API 34+):** [Android 14 Context-registered receivers](https://developer.android.com/about/versions/14/behavior-changes-14#runtime-receivers-exported) - Requires `RECEIVER_EXPORTED` / `RECEIVER_NOT_EXPORTED`.
