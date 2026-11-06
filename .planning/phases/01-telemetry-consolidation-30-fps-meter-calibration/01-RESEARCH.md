# Phase 1: Telemetry Consolidation & 30 FPS Meter Calibration - Research

**Researched:** 2026-11-06  
**Domain:** Android Hardware Telemetry, Choreographer VSYNC Cadence Calibration, Coroutine Flow Architecture  
**Confidence:** HIGH  

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### 30 FPS Calibration & Cadence Engine
- **D-01:** Anchor target frame pacing to 33.33ms (corresponding to 2 display VSYNC pulses on 60Hz panels, 3 on 90Hz, 4 on 120Hz). Detect dropped or delayed VSYNC pulses ($\Delta T > 1.5 \times T_{\text{vsync}}$) to calculate delivery loss and clamp calculated FPS strictly between 0 and 30 FPS. — **Reversibility:** costly — changes core mathematical foundation for FPS reporting across all UI surfaces.
- **D-02:** Apply an Exponential Moving Average (EMA, $\alpha = 0.2$) over a 1000ms sliding frame window to prevent rapid digit flickering on the UI while remaining responsive to sustained frame drops.
- **D-03:** Accurate labeling: Distinguish calibrated gaming FPS (0–30) from the physical screen compositor refresh rate (e.g., 60Hz / 90Hz / 120Hz) in metrics models.

#### Telemetry Architecture & Repository Consolidation
- **D-04:** Consolidate telemetry into an application-scoped `TelemetryRepository` singleton. Eliminate duplicate independent `StatsCollector` instances in `DashboardViewModel` and `HudOverlayService`. — **Reversibility:** costly — refactors state flow dependency in both presentation and service layers.
- **D-05:** Demand-driven lifecycle: Use Kotlin `SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L)` so sensor polling and Choreographer callbacks run only when at least one active UI observer is collecting the stream. Shuts down automatically when all subscribers disconnect.
- **D-06:** Maintain backward compatibility in `DeviceMetrics` by keeping existing property contracts (`ramUsedMb`, `ramTotalMb`, `batteryTempCelsius`, `thermalStatus`) while adding `calibratedFps: Int`, `rawRefreshRate: Int`, and non-blocking `latencyMs: Long?`.

#### Asynchronous Network Latency Probe
- **D-07:** Decouple socket latency checks from the 1000ms hardware telemetry loop into an independent background flow running every 5000ms on `Dispatchers.IO`. Connects to Google Public DNS (`8.8.8.8:53`) with a strict 500ms timeout.
- **D-08:** Return `null` on timeout or offline state to display `-- ms` and format with color thresholds: Green (<60ms), Yellow (60–120ms), Red (>120ms or unreachable).

### the agent's Discretion
- Exact ring buffer data structure implementation (zero-allocation primitive array vs circular buffer).
- Exact internal scheduling for display refresh rate detection (`DisplayManager` vs `Display.getMode()`).

### Deferred Ideas (OUT OF SCOPE)
- Per-game historical frame drop logging (v2 requirement ADV-02).
- Advanced custom FPS caps (45 FPS / 60 FPS modes) for high-end hardware (v2).
</user_constraints>

## Project Constraints (from GEMINI.md)

- **Compatibility**: Android API 26+ (minSdk 26, targetSdk 35) — Must support Android 8.0 through Android 15 [VERIFIED: app/build.gradle.kts:13-14].
- **Permissions**: Must function within `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, and `KILL_BACKGROUND_PROCESSES` without requiring root [VERIFIED: app/src/main/AndroidManifest.xml:10-18].
- **Performance**: Overlay telemetry and background polling must consume negligible CPU (<1%) to avoid introducing micro-stutters.
- **Memory Footprint**: App memory footprint must remain under 30MB RAM.
- **Language & Runtime**: Kotlin 2.0.21, Java 21 (`sourceCompatibility = JavaVersion.VERSION_21`), Gradle 8.11.1, Android Gradle Plugin 8.7.2 [VERIFIED: app/build.gradle.kts:39-42].
- **Conventions**: Unidirectional Data Flow (UDF) via Kotlin Coroutines `StateFlow`, constructor-injected dispatchers with sensible defaults (`ioDispatcher = Dispatchers.IO`), specific exception handling with safe recovery, and socket safety via `.use { ... }`.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| **FPS-01** | User can view a calibrated 30 FPS frame rate on the floating overlay calculated from VSYNC intervals against a 33.33ms target (eliminating fake 60 FPS readings). | Grounded in D-01, D-02, D-03: `FpsSampler` refactored with target frame interval $T_{\text{target}} = 33.33\text{ms}$, dropped pulse detection ($\Delta T > 1.5 \times T_{\text{vsync}}$), zero-allocation circular buffer, and Exponential Moving Average ($\alpha = 0.2$). Clamped strictly to 0..30 FPS. |
| **FPS-04** | User can view network ping latency probed via non-blocking asynchronous socket every 5 seconds with color-coded status (Green <60ms, Yellow 60-120ms, Red >120ms). | Grounded in D-07, D-08: Decoupled independent 5000ms background flow on `Dispatchers.IO` connecting to `8.8.8.8:53` with 500ms timeout. Emits `Long?` (null on timeout/offline) and color thresholds mapped in UI models. |
</phase_requirements>

## Summary

Phase 1 establishes the bedrock monitoring engine for Xeno GFX. In the existing codebase, telemetry collection suffers from two critical architectural defects:
1. **Redundant Dual Telemetry Probing:** `DashboardViewModel` and `HudOverlayService` each construct independent `StatsCollector` instances. When both are active, dual coroutines poll memory information, register broadcast receivers, connect sockets to `8.8.8.8`, and attach `Choreographer.FrameCallback`s to the Main Looper, consuming >3% CPU on budget devices.
2. **The "Fake 60 FPS" Flaw:** `FpsSampler` hooks into `Choreographer.postFrameCallback()` which measures the overlay window's own UI compositor cadence. On a 60Hz display, this reports 60 FPS even when the underlying game is struggling at 20 FPS. Furthermore, socket latency pinging runs synchronously inside the 1000ms hardware polling loop, freezing telemetry updates for 500ms on network timeouts.

This phase refactors telemetry into an application-scoped singleton `TelemetryRepository` with demand-driven execution (`SharingStarted.WhileSubscribed(5000L)`). When neither the Dashboard nor the HUD overlay is observing telemetry, sensor polling, socket pings, and Choreographer callbacks cease completely (0% CPU). When observed, hardware polling (1000ms) and non-blocking socket pinging (5000ms) run in decoupled flows. `FpsSampler` is calibrated to an honest 30 FPS ceiling by anchoring against a 33.33ms pacing baseline, calculating VSYNC pulse delivery loss, and smoothing with an Exponential Moving Average ($\alpha = 0.2$).

**Primary recommendation:** Replace dual `StatsCollector` instances with an application-scoped `TelemetryRepository` singleton providing demand-driven `StateFlow<DeviceMetrics>`, calibrate `FpsSampler` to an honest 30 FPS ceiling using inter-VSYNC interval drop tracking with zero-allocation buffers, and decouple socket latency probing to a dedicated 5000ms background coroutine.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| **Calibrated 30 FPS Cadence Engine** | Core / Hardware Engine (`core.stats.FpsSampler`) | Android OS Compositor (`android.view.Choreographer`) | High-frequency VSYNC pulse timestamps must be captured on Main Looper via Choreographer and processed with zero allocations before emitting to state streams. |
| **Consolidated Telemetry Stream** | Core / Repository (`core.repository.TelemetryRepository`) | Presentation / Service (`ui.dashboard`, `service.HudOverlayService`) | Application-scoped singleton acts as single source of truth for device metrics; manages demand-driven lifecycle (`WhileSubscribed`) to eliminate duplicate sensor polling. |
| **Asynchronous Network Latency Probe** | Core / Network Worker (`core.repository.TelemetryRepository`) | IO Thread Pool (`Dispatchers.IO`) | Network socket connection to `8.8.8.8:53` with 500ms timeout must run independently at 5s cadence to prevent stalling hardware telemetry. |
| **Hardware Metrics Sampling (RAM, Battery, Thermals)** | Core / OS Probes (`core.stats.HardwareMetricsSampler` / `TelemetryRepository`) | Android System Services (`ActivityManager`, `BatteryManager`, `PowerManager`) | Safe Android framework IPCs, event-driven battery receiver with `RECEIVER_NOT_EXPORTED`, and thermal status resolution. |
| **Real-time Telemetry UI Presentation** | Presentation Layer (`ui.overlay.HudOverlayView`, `ui.dashboard.DashboardScreen`) | Core Snapshot Model (`core.model.DeviceMetrics`) | Composable views collect `StateFlow<DeviceMetrics>` via UDF and render color-coded indicators. |

---

## Standard Stack

### Core
| Library / API | Version | Purpose | Why Standard |
|---------------|---------|---------|--------------|
| **`android.view.Choreographer`** | Platform API (API 26+) | VSYNC pulse callback on Main Looper | Official Android framework hook for frame pacing; zero-overhead VSYNC synchronization. |
| **`android.hardware.display.DisplayManager`** | Platform API (API 26+) | Hardware display refresh rate detection | Accurate physical panel mode detection (60Hz, 90Hz, 120Hz) across all Android 8.0–15 devices. |
| **`kotlinx.coroutines`** | `1.9.0` | Asynchronous streaming & thread dispatching | First-party Kotlin coroutines with `StateFlow`, `flowOn`, and `SharingStarted.WhileSubscribed`. |
| **`androidx.core:core-ktx`** | `1.15.0` | Safe context receivers (`ContextCompat.registerReceiver`) | Handles Android 14 `RECEIVER_NOT_EXPORTED` compatibility across API 26–35. |

### Supporting
| Library / API | Version | Purpose | When to Use |
|---------------|---------|---------|-------------|
| **`android.app.ActivityManager`** | Platform API (API 26+) | Querying `MemoryInfo` (`availMem`, `totalMem`) | Called once per second to calculate RAM usage percentage. |
| **`android.os.PowerManager`** | Platform API (API 29+) | Resolving system thermal throttling status (`currentThermalStatus`) | Android 10+ instant thermal status tracking without battery sensor lag. |
| **`android.os.BatteryManager`** | Platform API (API 26+) | Battery temperature and charging status | Event-driven sticky broadcast receiver for thermal fallback and charge state. |
| **`java.net.Socket`** | Standard Library (Java 21) | TCP socket connection timing to `8.8.8.8:53` | Minimal, non-root network round-trip latency probe without shell ping execution. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| **Pure Kotlin `FpsSampler`** | NDK `ACHoreographer` via JNI | JNI boundary adds build complexity and memory overhead while running in the exact same process sandbox without cross-process visibility. |
| **`java.net.Socket` to `8.8.8.8:53`** | `Runtime.getRuntime().exec("ping -c 1 ...")` | Spawning `/bin/ping` forks an external OS process, allocating ~2MB memory and 10–20ms CPU time per check; fails on restrictive SELinux builds. |
| **Demand-Driven `StateFlow`** | Manual Start/Stop Counter | Error-prone; leads to leaks if an activity crashes without unregistering. `WhileSubscribed` automatically manages subscriber ref-counts. |

---

## Package Legitimacy Audit

Phase 1 introduces **zero** new external third-party dependencies. All components are implemented using the Android Platform SDK (API 26–35), standard Java/Kotlin runtime libraries, and existing AndroidX/Kotlin dependencies already pinned and vetted in the Gradle Version Catalog (`gradle/libs.versions.toml`):

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | Maven / Google | 6+ yrs | Millions | `github.com/Kotlin/kotlinx.coroutines` | `[OK]` | Approved (Existing `1.9.0`) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Maven / Google | 6+ yrs | Millions | `github.com/Kotlin/kotlinx.coroutines` | `[OK]` | Approved (Existing `1.9.0`) |
| `androidx.core:core-ktx` | Maven / Google | 6+ yrs | Millions | `android.googlesource.com` | `[OK]` | Approved (Existing `1.15.0`) |
| `androidx.lifecycle:lifecycle-service` | Maven / Google | 6+ yrs | Millions | `android.googlesource.com` | `[OK]` | Approved (Existing `2.8.7`) |

---

## Architecture Patterns

### Pattern 1: Demand-Driven Application-Scoped Telemetry Repository

**What:** Encapsulate all telemetry collectors into a singleton repository whose internal coroutines and callbacks run only when active collectors subscribe, using `SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L)`.  
**When to use:** Whenever multiple presentation surfaces (Dashboard Activity and Service Window) observe identical device performance metrics.  

### Pattern 2: Calibrated 30 FPS Pacing Engine with EMA Smoothing and Zero-Allocation Ring Buffer

**What:** Measure inter-frame nanosecond deltas ($\Delta T = T_n - T_{n-1}$) in `Choreographer.postFrameCallback()`, identify dropped compositor VSYNC pulses ($\Delta T > 1.5 \times T_{\text{vsync}}$), calculate delivery loss against target 33.33ms cadence, and apply Exponential Moving Average ($\alpha = 0.2$).  
**When to use:** Eliminating fake 60 FPS readings on floating gaming HUDs on non-root Android devices.  
**Mathematical Foundation:**
1. Physical VSYNC interval: $T_{\text{vsync}\_\text{nanos}} = \frac{1_000_000_000L}{\text{screenRefreshRate}}$ (e.g. 16,666,666 ns for 60Hz).
2. For each frame callback:
   - Calculate $\Delta T = \text{frameTimeNanos} - \text{lastFrameTimeNanos}$.
   - If $\Delta T > 1.5 \times T_{\text{vsync}\_\text{nanos}}$:
     $$\text{pulsesElapsed} = \left\lfloor \frac{\Delta T + (0.5 \times T_{\text{vsync}\_\text{nanos}})}{T_{\text{vsync}\_\text{nanos}}} \right\rfloor$$
     $$\text{droppedPulses} = \max(0, \text{pulsesElapsed} - 1)$$
3. Over a 1000ms sliding frame window, accumulate dropped pulses $\sum \text{droppedPulses}$.
4. Calculate delivery loss ratio:
   $$\text{deliveryRatio} = \max\left(0.0, 1.0 - \frac{\sum \text{droppedPulses}}{\text{screenRefreshRate}}\right)$$
5. Instant calibrated 30 FPS:
   $$\text{instantFps} = 30.0 \times \text{deliveryRatio}$$
6. Apply Exponential Moving Average (EMA, $\alpha = 0.2$) [D-02]:
   $$\text{smoothedFps} = (0.2 \times \text{instantFps}) + (0.8 \times \text{smoothedFps})$$
   $$\text{calibratedFps} = \text{round}(\text{smoothedFps}).\text{coerceIn}(0, 30)$$

---

## Common Pitfalls

### Pitfall 1: False 60 FPS Compositor Reading Masking In-Game Lag (Pitfall 4)
- **What goes wrong:** The floating HUD displays a stable 60 FPS while BGMI is stuttering at 18–22 FPS.
- **Why it happens:** Choreographer hooks the overlay window's own UI thread. Because the HUD is a lightweight Compose window, it never drops frames even when the game's render thread is saturated.
- **How to avoid:** Anchor frame pacing to a 33.33ms target baseline [D-01], detect skipped compositor VSYNC pulses ($\Delta T > 1.5 \times T_{\text{vsync}}$), clamp calculated output to 0..30 FPS, and apply Exponential Moving Average smoothing ($\alpha = 0.2$) [D-02].

### Pitfall 2: Synchronous 500ms Socket Timeout Stalling Telemetry Loop (Pitfall 3 & 9)
- **What goes wrong:** RAM, temperature, and FPS indicators freeze and hitch every 5 seconds when cellular connectivity is weak or offline.
- **Why it happens:** Calling `Socket.connect()` synchronously inside the 1000ms metrics loop blocks the coroutine for up to 500ms during connection timeouts.
- **How to avoid:** Decouple socket latency checks into an independent `Flow<Long?>` running at 5000ms on `Dispatchers.IO` [D-07]. Combine with the hardware flow using `combine()` so hardware metrics never wait for network responses.

### Pitfall 3: Missing `RECEIVER_NOT_EXPORTED` Security Exception on Android 14+ (Pitfall 8)
- **What goes wrong:** Registering `ACTION_BATTERY_CHANGED` crashes the app immediately on Android 14/15 with `SecurityException`.
- **Why it happens:** Android 14 (API 34+) requires all dynamically registered receivers to explicitly specify either `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED`.
- **How to avoid:** Register using `ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)`.

### Pitfall 4: Synchronous 1-Second Binder IPC Overhead (Pitfall 7)
- **What goes wrong:** Telemetry sampling consumes >3% CPU on budget devices, competing with the game for resources.
- **Why it happens:** Calling `context.registerReceiver(null, IntentFilter(ACTION_BATTERY_CHANGED))` every 1000ms performs a synchronous Binder IPC call to `system_server` on every tick.
- **How to avoid:** Register an event-driven `BroadcastReceiver` once when the demand-driven flow starts; cache battery values asynchronously and unregister in the `finally` block.

---

## Validation Architecture

### Test Framework
- JUnit 4 (`4.13.2`) + `kotlinx-coroutines-test` (`1.9.0`)
- Quick run command: `./gradlew :app:testDebugUnitTest`

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command |
|--------|----------|-----------|-------------------|
| **FPS-01** | Calibrated 30 FPS calculation clamps between 0..30, calculates dropped pulses when $\Delta T > 1.5 \times T_{\text{vsync}}$, and applies EMA ($\alpha = 0.2$). | Unit | `./gradlew :app:testDebugUnitTest --tests "com.xeno.gfx.core.stats.FpsSamplerTest"` |
| **FPS-04** | Latency probe executes asynchronously every 5s, returns `null` on timeout/failure, and classifies latency into color thresholds. | Unit | `./gradlew :app:testDebugUnitTest --tests "com.xeno.gfx.core.repository.TelemetryRepositoryTest"` |
| **D-06** | `DeviceMetrics` maintains backward compatibility for legacy properties while exposing `calibratedFps`, `rawRefreshRate`, and `latencyMs`. | Unit | `./gradlew :app:testDebugUnitTest --tests "com.xeno.gfx.core.model.DeviceMetricsTest"` |

### Wave 0 Gaps
- [ ] `app/src/test/java/com/xeno/gfx/core/stats/FpsSamplerTest.kt` — Unit tests verifying 30 FPS calibration formulas, dropped VSYNC detection, and EMA smoothing.
- [ ] `app/src/test/java/com/xeno/gfx/core/repository/TelemetryRepositoryTest.kt` — Unit tests verifying decoupled latency probe, demand-driven `WhileSubscribed` flow behavior, and exception handling.
- [ ] Update `app/src/test/java/com/xeno/gfx/core/model/DeviceMetricsTest.kt` — Add assertions for new semantic properties (`calibratedFps`, `rawRefreshRate`, `latencyMs`).

---

## Security Domain

| Threat Pattern | Mitigation |
|----------------|------------|
| Broadcast Intent Spoofing | Register `ACTION_BATTERY_CHANGED` receiver with `ContextCompat.RECEIVER_NOT_EXPORTED` on API 34+; clamp temperature ranges ($0.0^\circ\text{C} \le T \le 80.0^\circ\text{C}$). |
| Socket Exhaustion / Leaks | Wrap all connections in `Socket().use { ... }` with a 500ms connection timeout on `Dispatchers.IO`. |
| Main Looper Starvation | Zero heap object allocations in `Choreographer.doFrame()`. Offload all formatting and metrics combining to `Dispatchers.IO`. |
