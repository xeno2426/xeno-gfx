# Phase 1: Telemetry Consolidation & 30 FPS Meter Calibration - Context

**Gathered:** 2026-11-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Consolidate hardware and network telemetry collection into an application-scoped `TelemetryRepository` and calibrate the frame rate calculation engine to an honest, stutter-free 30 FPS target (33.33ms VSYNC cadence baseline) with decoupled, non-blocking asynchronous socket latency probing.

Delivers requirements:
- **FPS-01**: User can view a calibrated 30 FPS frame rate on the floating overlay calculated from VSYNC intervals against a 33.33ms target (eliminating fake 60 FPS readings).
- **FPS-04**: User can view network ping latency probed via non-blocking asynchronous socket every 5 seconds with color-coded status (Green <60ms, Yellow 60-120ms, Red >120ms).
</domain>

<decisions>
## Implementation Decisions

### 30 FPS Calibration & Cadence Engine
- **D-01:** Anchor target frame pacing to 33.33ms (corresponding to 2 display VSYNC pulses on 60Hz panels, 3 on 90Hz, 4 on 120Hz). Detect dropped or delayed VSYNC pulses ($\Delta T > 1.5 \times T_{\text{vsync}}$) to calculate delivery loss and clamp calculated FPS strictly between 0 and 30 FPS. — **Reversibility:** costly — changes core mathematical foundation for FPS reporting across all UI surfaces.
- **D-02:** Apply an Exponential Moving Average (EMA, $\alpha = 0.2$) over a 1000ms sliding frame window to prevent rapid digit flickering on the UI while remaining responsive to sustained frame drops.
- **D-03:** Accurate labeling: Distinguish calibrated gaming FPS (0–30) from the physical screen compositor refresh rate (e.g., 60Hz / 90Hz / 120Hz) in metrics models.

### Telemetry Architecture & Repository Consolidation
- **D-04:** Consolidate telemetry into an application-scoped `TelemetryRepository` singleton. Eliminate duplicate independent `StatsCollector` instances in `DashboardViewModel` and `HudOverlayService`. — **Reversibility:** costly — refactors state flow dependency in both presentation and service layers.
- **D-05:** Demand-driven lifecycle: Use Kotlin `SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L)` so sensor polling and Choreographer callbacks run only when at least one active UI observer is collecting the stream. Shuts down automatically when all subscribers disconnect.
- **D-06:** Maintain backward compatibility in `DeviceMetrics` by keeping existing property contracts (`ramUsedMb`, `ramTotalMb`, `batteryTempCelsius`, `thermalStatus`) while adding `calibratedFps: Int`, `rawRefreshRate: Int`, and non-blocking `latencyMs: Long?`.

### Asynchronous Network Latency Probe
- **D-07:** Decouple socket latency checks from the 1000ms hardware telemetry loop into an independent background flow running every 5000ms on `Dispatchers.IO`. Connects to Google Public DNS (`8.8.8.8:53`) with a strict 500ms timeout.
- **D-08:** Return `null` on timeout or offline state to display `-- ms` and format with color thresholds: Green (<60ms), Yellow (60–120ms), Red (>120ms or unreachable).

### the agent's Discretion
- Exact ring buffer data structure implementation (zero-allocation primitive array vs circular buffer).
- Exact internal scheduling for display refresh rate detection (`DisplayManager` vs `Display.getMode()`).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project & Requirements
- `.planning/PROJECT.md` — Project goals, low-RAM constraints, and core 30 FPS value proposition
- `.planning/REQUIREMENTS.md` — Acceptance definitions for FPS-01 and FPS-04

### Technical Architecture & Research
- `.planning/research/STACK.md` § 2 — Frame Timing & 30 FPS Calibration Engine mathematical formulas and dependencies
- `.planning/research/ARCHITECTURE.md` § TelemetryRepository — Demand-driven repository pattern and state flow architecture
- `.planning/research/PITFALLS.md` § Pitfall 4, 7, 9 — Choreographer false telemetry artifacts, synchronous Binder IPCs, and cellular radio wakeups

### Codebase Conventions
- `.planning/codebase/CONVENTIONS.md` — Kotlin coroutines, naming patterns, and error handling rules
- `.planning/codebase/ARCHITECTURE.md` — Existing layered system overview
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt`: Existing `Choreographer.FrameCallback` logic to be refactored into the calibrated 30 FPS pacing engine.
- `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`: Existing RAM and battery metrics sampling logic to be extracted into `TelemetryRepository`.
- `app/src/main/java/com/xeno/gfx/core/model/DeviceMetrics.kt`: Core snapshot model to be enriched with calibrated metrics.

### Established Patterns
- Unidirectional Data Flow via Kotlin `StateFlow<DeviceMetrics>`.
- Coroutine dispatcher injection (`ioDispatcher = Dispatchers.IO`) for testability.
- Safe resource management with `.use { ... }` for network sockets.

### Integration Points
- `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`: Replace `StatsCollector` instance with `TelemetryRepository.metrics`.
- `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`: Replace `StatsCollector` instance with `TelemetryRepository.metrics`.
</code_context>

<specifics>
## Specific Ideas

- Ensure CPU overhead of the new telemetry repository remains under 1% on 3GB/4GB budget devices so the monitoring utility never introduces micro-stutters into the game.
- Maintain seamless display of live FPS on both the main cyberpunk dashboard and the floating overlay window.
</specifics>

<deferred>
## Deferred Ideas

- Per-game historical frame drop logging (v2 requirement ADV-02).
- Advanced custom FPS caps (45 FPS / 60 FPS modes) for high-end hardware (v2).
</deferred>

---

*Phase: 01-telemetry-consolidation-30-fps-meter-calibration*
*Context gathered: 2026-11-06*
