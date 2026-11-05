# Feature Landscape: Android Gaming Performance Optimizer & BGMI 30 FPS Turbo

**Domain:** Android Gaming Performance Optimization & Floating Hardware Telemetry (3GB–4GB Low-RAM Devices)  
**Researched:** 2026-11-05  
**Target OS:** Android 8.0 (API 26) through Android 15 (API 35)  
**Target Games:** Battlegrounds Mobile India (BGMI / PUBG Mobile variants), Free Fire, Call of Duty Mobile  

---

## Executive Feature Overview

Modern mobile game boosters fall into two distinct camps: OEM-integrated suites (Samsung Game Booster, Xiaomi Game Turbo) that leverage privileged platform permissions, and third-party Play Store utilities (PGT+ Pro GFX, Game Booster 4x Faster) that operate within standard Android user-space sandboxes.

For low-RAM devices (3GB and 4GB RAM), aiming for 60 FPS or unlocked frame rates is often a trap: budget SoCs (e.g., MediaTek Helio G35/G85/G88, Qualcomm Snapdragon 665/680) suffer rapid thermal saturation within 5–10 minutes, triggering severe hardware thermal throttling down to 12–18 FPS accompanied by erratic frame pacing. Furthermore, low-memory budget phones constantly thrash swap/zRAM and invoke Android’s kernel Low Memory Killer (`lmkd`), resulting in micro-stutters during intense game moments (e.g., BGMI hot-drops).

**Xeno GFX** differentiates itself by offering a **rock-solid, stutter-free 30 FPS target** (33.33ms uniform frame time) achieved through:
1. **Calibrated cadence telemetry** that avoids the misleading "fake 60 FPS" display refresh rate pitfall without requiring root.
2. **Decoupled foreground continuous background RAM suppression** that keeps free memory above the LMK threshold.
3. **Emergency thermal protection** that intervenes before thermal throttling destroys frame pacing.
4. **An ultra-compact, non-obstructive minimalist FPS badge**.
5. **Potato graphic configuration presets (`Active.sav`)** tailored specifically to 30 FPS budget performance.

---

## Table Stakes

Features users expect in any legitimate modern game booster. Missing any of these causes immediate uninstallation or negative store reviews.

| Feature | Why Expected | Complexity | Implementation Notes |
|---------|--------------|------------|----------------------|
| **One-Tap Turbo Game Launcher** | Centralized hub aggregating installed competitive games (`com.pubg.imobile`, `com.tencent.ig`, `com.dts.freefireth`); users expect to launch their game with optimization pre-applied. | Low | Requires `<queries>` elements in `AndroidManifest.xml` on Android 11+ (API 30+) to resolve launcher activities and installed game packages without runtime security filtering. |
| **Pre-Launch RAM Purge & Memory Reclaim** | Users expect immediate physical RAM clearance before entering a memory-heavy game like BGMI. | Medium | Calls `ActivityManager.killBackgroundProcesses()` on unwhitelisted packages, triggers `System.gc()`, and measures delta in `ActivityManager.MemoryInfo.availMem`. Reports transparent MB recovered rather than misleading arbitrary process numbers. |
| **Real-Time Floating Telemetry HUD** | Users demand live visibility into device performance (FPS, available RAM %, battery temperature, and network ping) while playing full-screen. | Medium | Rendered via Android `WindowManager` with `TYPE_APPLICATION_OVERLAY`. Requires `android.permission.SYSTEM_ALERT_WINDOW`. Must handle screen rotation (portrait dashboard to landscape game) without clipping off-screen. |
| **Non-Intrusive Touch & Drag Controls** | Floating overlay must be movable anywhere on screen and must never block in-game shooting, aiming, or mini-map controls. | Medium | Must use touch slop thresholding (`ViewConfiguration.getScaledTouchSlop`) or Compose `pointerInput(detectDragGestures)` so taps/drags do not starve child clickables or steal touches from the game surface underneath. |
| **Persistent Foreground Notification & Quick Actions** | Android 8.0+ enforces persistent notifications for background services; users expect quick buttons ('Purge RAM Now', 'Toggle FPS HUD', 'Exit Turbo') without switching out of the game. | Low | Hosted inside `HudOverlayService` foreground lifecycle using `NotificationCompat.Builder` with pending intent actions and `FOREGROUND_SERVICE_SPECIAL_USE` type declaration for Android 14+. |
| **Automated Game Launch Detection (Auto-HUD)** | Automatically activates overlay and starts turbo suppression when a tracked game is brought to the foreground, without requiring manual booster app startup. | Medium | Uses `UsageStatsManager.queryEvents()` with `UsageEvents.Event.ACTIVITY_RESUMED`. Window query timestamps must advance monotonically every cycle (`lastQueryTimestamp = now`) to prevent unbounded CPU polling leaks. |
| **System, Launcher & Keyboard IME Protection** | Aggressive RAM cleaning must never kill critical Android OS components, default system home launchers, or active virtual keyboards (Gboard/SwiftKey), which would cause device freezes. | Low | Implemented via `AppWhitelist` filtering `ApplicationInfo.FLAG_SYSTEM`, default home launcher resolved via `Intent.ACTION_MAIN / CATEGORY_HOME`, and active IME package via `Settings.Secure.DEFAULT_INPUT_METHOD`. |
| **Persistent User Configuration & Storage** | Enabled meters, tracked games, HUD opacity, and custom toggles must survive app death and device reboots. | Low | Backed by Jetpack DataStore Preferences (`androidx.datastore:datastore-preferences`). Immediate reactive flow binding into `DashboardUiState`. |

---

## Differentiators

Features that set Xeno GFX apart from generic "snake-oil" boosters, specifically engineered for budget 3GB/4GB devices targeting 30 FPS stability.

| Feature | Value Proposition | Complexity | Implementation Notes |
|---------|-------------------|------------|----------------------|
| **Target 30 FPS Calibrated Cadence Meter** | Non-root `Choreographer.FrameCallback` normally reports 60Hz display refresh rate (giving fake 60 FPS even when the game is lagging at 20 FPS). Calibrating and clamping to the 30 FPS target (33.33ms baseline) measures actual compositor VSYNC intervals and dropped tick cadences to deliver an honest 30 FPS stability gauge. | Medium | Analyzes frame interval deltas (`frameTimeNanos - lastFrameTimeNanos`). When compositor ticks slip past 33.33ms, logs frame pacing jank and calculates true delivered cadence relative to the 30 FPS cap. |
| **Minimalist "Stealth" Floating FPS Badge** | Competitive mobile gamers hate bulky HUD boxes covering their mini-map, kill-feed, or crosshair. An ultra-compact pill/badge (<50dp width, transparent background, single live FPS number) provides zero visual distraction. | Medium | Two-state overlay view: Compact Badge (single glowing FPS text pill) and Expanded Dashboard (RAM %, Battery Temp, Ping, Quick Actions). Single tap toggles between states; long press enables free dragging and edge-dock snapping. |
| **Decoupled Continuous Background Suppression ("Full Throttle Mode")** | Moving game detection and the 30-second background process suppression loop out of the Activity `viewModelScope` into a robust Android Foreground Service (`HudOverlayService`) ensures optimization survives `MainActivity` death when memory is tight. | High | Operates inside `HudOverlayService` on `Dispatchers.IO`. On Android 8–13, periodically flushes non-whitelisted cached background processes. On Android 14+ (where `killBackgroundProcesses` is restricted to calling package), triggers local trim and garbage collection and notifies user of background memory pressure. |
| **Emergency Thermal Spike Cool-Down & Alert** | When budget chipsets exceed 42°C–45°C, CPU/GPU thermal governors aggressively throttle clocks, halving frame rates. Proactive notification and automatic background RAM flushing alerts the player before unrecoverable thermal throttling occurs. | Low | Monitored through `BatteryManager.EXTRA_TEMPERATURE` in registered sticky intent receiver. Triggers high-priority HUD visual warning (amber/red badge glow) and executes an immediate memory purge. |
| **BGMI 30 FPS Potato Graphics Preset Generator (`Active.sav`)** | Budget devices overheat trying to render high-resolution shadows, foliage, and high-LOD textures. Providing direct, optimized 30 FPS graphic profiles (`Active.sav` with 540p/720p scaling, low shadow bias, zero motion blur) guarantees sustained 30 FPS on low-end hardware. | High | Generates or writes UE4 configuration files. Handles Android 11–15 Scoped Storage boundaries gracefully via Storage Access Framework (SAF) document tree or clear user export guidance. |
| **Low-Overhead Consolidated Telemetry Repository** | Eliminates duplicate hardware samplers. Replaces dual polling loops and independent Choreographer listeners in `DashboardViewModel` and `HudOverlayService` with a single shared `StateFlow<DeviceMetrics>` repository. | Medium | `StatsRepository` singleton. Throttles background sampling rate to 2000ms when overlay is collapsed/badge mode, dropping optimizer CPU utilization to <0.8% and memory consumption to <25MB. |
| **Decoupled Asynchronous Network Latency Probe** | Eliminates 1-second synchronous blocking sockets that freeze the telemetry thread and prevents continuous mobile radio high-power state (`DCH`) battery drain. | Low | Uses decoupled non-blocking coroutine probe to check gateway/DNS latency every 5–10 seconds, color-coding ping: Green (<60ms), Yellow (60–120ms), Red (>120ms). |
| **Strict System & Input-Only Aggressive Policy** | Unlike generic boosters that protect dozens of social apps, Xeno GFX protects *only* the core OS, IME, and launcher. Dormant social messengers and background sync workers are evicted to hand 100% of physical RAM to BGMI. | Low | Clean exclusion list: protects `android`, `com.android.systemui`, current launcher package, active keyboard IME package, and target game package. |

---

## Anti-Features

Features to **deliberately NOT build**, preventing security risks, platform violations, ban risks, or architectural degradation.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Root / Magisk / KernelSU Dependency** | 98% of target budget users do not root their phones. Rooting voids warranties, trips SafetyNet / Play Integrity (preventing banking app usage), and flags anti-cheat engines in games like BGMI. | Build 100% within standard Android SDK user permissions (`SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, Foreground Services). |
| **Shizuku / Wireless Debugging Bridge Requirement** | High onboarding friction. Requires connecting to Wi-Fi to start wireless ADB pairing; resets on every phone reboot. Causes high abandonment rate among non-technical mobile gamers. | Rely strictly on standard Android user permissions. Clarify FPS meter calibration in UI rather than forcing ADB `dumpsys SurfaceFlinger`. |
| **Fake "CPU Overclock / 200% Turbo" Sliders** | Deceptive snake-oil. Android kernel frequency governors cannot be altered by third-party apps without kernel root. Deceptive claims violate Google Play Developer Policies and destroy user trust. | Deliver honest, measurable optimizations: RAM cache eviction, thermal throttling prevention, 33.33ms frame pacing calibration, and graphics preset downscaling. |
| **Cloud User Accounts & Remote Analytics** | Introduces unnecessary network latency, privacy concerns, server infrastructure costs, and APK bloat. Budget gamers often play on metered mobile data. | 100% offline, standalone architecture. Store all preferences locally using Jetpack DataStore. Zero telemetry transmissions to remote servers. |
| **Aggressive Interstitial or Video Ads on Game Launch** | Common nuisance in low-quality game boosters. Showing an ad right before a game match consumes 80–120MB of RAM, negating the entire RAM purge and causing the game to stutter upon launch. | Zero launch interruptions. Direct, instant one-tap launch into the game. |
| **Direct Process Memory Injection (e.g. Frida / GameGuardian)** | Modifying game memory or injecting code into `libUE4.so` triggers Krafton / Tencent anti-cheat engines, leading to instant 10-year account bans. | Restrict game configuration exclusively to standard offline configuration files (`Active.sav` / graphics presets) and OS environment optimization. |
| **Indiscriminate Process Slaughter on Android 14+** | In Android 14+ (API 34), `killBackgroundProcesses` ignores external packages. Blindly looping over packages causes log spam and wastes CPU cycles. Furthermore, killing active audio playback (Spotify, Discord voice) ruins gaming communication. | Check `AudioManager.isMusicActive()` or media session status before evicting apps. Adapt cleanup logic gracefully for API 34+. |
| **Resource-Heavy Secondary Features (In-Game Video Recording / Overlay Browser)** | Screen recording or floating web browsers require hardware video encoders and 80MB+ RAM, causing budget 3GB/4GB devices to immediately drop frames in BGMI. | Keep Xeno GFX strictly focused on lightweight telemetry (<25MB RAM, <1% CPU) and process suppression. |

---

## Feature Dependencies

```
┌────────────────────────────────────────────────────────┐
│               Android System Permissions               │
│  • SYSTEM_ALERT_WINDOW  • PACKAGE_USAGE_STATS          │
└───────────────┬────────────────────────┬───────────────┘
                │                        │
                ▼                        ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│     WindowManager Overlay    │ │   ForegroundGameDetector     │
│   (HudOverlayService FGS)    │ │ (UsageEvents ACTIVITY_RESUME)│
└───────────────┬──────────────┘ └──────────────┬───────────────┘
                │                               │
                ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│ Minimalist FPS Badge / HUD   │ │ Auto-Launch & Auto-Suppress  │
└───────────────┬──────────────┘ └──────────────┬───────────────┘
                │                               │
                │        ┌──────────────────────┘
                ▼        ▼
┌────────────────────────────────────────────────────────┐
│               StatsRepository (Singleton)              │
│  • Calibrated 30 FPS Cadence Frame Tracker             │
│  • Battery Thermals & Thermal Spike Alert (>42°C)      │
│  • Asynchronous Gateway Network Ping Probe             │
│  • Available RAM & Memory Pressure Monitor             │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────┐
│               RamOptimizer & AppWhitelist              │
│  • Pre-Launch RAM Flush & MB Reclaim Calculation       │
│  • Full Throttle Continuous 30s Suppression Loop       │
│  • System, Launcher, & IME Protection                  │
└────────────────────────────────────────────────────────┘

┌──────────────────────────────┐ ┌──────────────────────────────┐
│  Jetpack DataStore Storage   │ │   BGMI 30 FPS GFX Tool       │
│  • HUD customization flags   │ │   • Potato Graphic Presets   │
│  • Custom whitelist set      │ │   • Active.sav config writer │
│  • Auto-HUD tracked games    │ │   • Scoped Storage SAF bridge│
└──────────────────────────────┘ └──────────────────────────────┘
```

---

## MVP Recommendation & Roadmap Phasing

### Phase 1: Core Foundation & Telemetry Engine (Table Stakes + Key Calibration)
- **Deliverables:**
  - Consolidate telemetry into a single application-scoped `StatsRepository` (SharedFlow / StateFlow).
  - Implement Calibrated 30 FPS Cadence meter (frame delta timing against 33.33ms baseline).
  - Fix Compose touch event starvation in overlay (`Modifier.pointerInput` with touch slop drag).
  - Asynchronous non-blocking network ping probe (5s interval).
- **Rationale:** Without accurate, low-overhead telemetry, the booster cannot measure or prove its performance impact.

### Phase 2: Decoupled Service Architecture & Game Turbo Suite
- **Deliverables:**
  - Move `ForegroundGameDetector` and continuous suppression into `HudOverlayService` lifecycle (independent of `MainActivity`).
  - Implement Full Throttle continuous 30-second RAM suppression loop.
  - Implement strict System & IME protection (`AppWhitelist`) with media playback check.
  - Fix monotonic timestamp advancing in `ForegroundGameDetector` to eliminate unbounded CPU loops.
- **Rationale:** Ensures game detection and memory management continue working seamlessly when the game is full-screen and the launcher activity is destroyed by the OS.

### Phase 3: Minimalist FPS Badge & UI Polish
- **Deliverables:**
  - Build two-state floating overlay: Minimalist FPS Badge (compact glowing pill <50dp) and Expanded HUD.
  - Implement screen orientation listener and display bounds clamping for landscape game transitions.
  - Add Persistent Notification Quick Actions ('Purge RAM Now', 'Toggle FPS HUD', 'Exit Turbo').
  - Jetpack DataStore Preferences integration for instant settings persistence across reboots.
- **Rationale:** Gives competitive gamers an unobtrusive, zero-distraction HUD and persistent controls.

### Phase 4: Emergency Thermal Protection & BGMI 30 FPS GFX Tool
- **Deliverables:**
  - Emergency Thermal Spike Monitor (visual alert on HUD + auto-purge when temp >42°C).
  - BGMI 30 FPS Potato Graphics preset generator (`Active.sav` / configuration modifier).
  - Scoped Storage SAF integration and clear export workflow for Android 11–15.
- **Rationale:** Directly addresses the root causes of frame drops on budget hardware: GPU/CPU thermal throttling and oversized in-game texture/shadow overhead.

### Features Deferred from MVP
- **Custom Hex Graphic Preset Editor:** Defer to post-MVP. Risk of user error corrupting game data; static tested presets are safer.
- **Audio Voice Changer / Floating Browser:** Defer indefinitely. Violates target memory footprint (<30MB) and distracts from core 30 FPS optimization value.

---

## Sources & Verification

| Topic / Feature | Source | Confidence | Verification Notes |
|-----------------|--------|------------|--------------------|
| `ActivityManager.killBackgroundProcesses` Restrictions | Android 14 Behavior Changes (developer.android.com) | HIGH | Verified: In API 34+, calling `killBackgroundProcesses` on third-party packages is ignored by the OS (`Invalid packageName`). Must adapt suppression strategy. |
| Non-Root Choreographer Limitations | Android View System & Choreographer Documentation | HIGH | Verified: `Choreographer.postFrameCallback` runs on the calling process Looper, measuring overlay VSYNC cadence rather than game surface buffer swaps. Cadence calibration required. |
| Scoped Storage in Android 11–15 | Android Storage Access Framework & Scoped Storage Guidelines | HIGH | Verified: Direct file access to `Android/data/com.pubg.imobile/` is restricted on modern Android. Must use SAF tree picker or file export guide. |
| Low-RAM Android LMK & Memory Pressure | Android Core OS Low Memory Killer Daemon (`lmkd`) Docs | HIGH | Verified: Continuous RAM clearing before memory pressure crosses high watermarks prevents kernel LMK from terminating foreground gaming activities. |
| Competitive Mobile Booster Patterns | Tear-down of Samsung Game Booster, Xiaomi Game Turbo, PGT+ Pro GFX | MEDIUM | Verified via competitive analysis: Minimalist overlays, battery thermals, and one-tap pre-launch memory clearing represent universal user expectations. |
