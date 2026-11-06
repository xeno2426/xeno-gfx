# Phase 1: Telemetry Consolidation & 30 FPS Meter Calibration - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-11-06
**Phase:** 1-Telemetry Consolidation & 30 FPS Meter Calibration
**Areas discussed:** 30 FPS Calibration Algorithm, Telemetry Repository Architecture, Asynchronous Latency Probe, Metrics Compatibility

---

## 30 FPS Calibration Algorithm

| Option | Description | Selected |
|--------|-------------|----------|
| Mathematical Jitter Loss with 1000ms EMA | Anchor target interval to 33.33ms, calculate dropped VSYNC pulses, and smooth with Exponential Moving Average (alpha = 0.2) | ✓ |
| Direct Frame Cadence Binning | Strict bucket tracking of 33.33ms presentation windows | |
| Hard Floor Clamping | Raw Choreographer frequency divided by refresh rate ratio | |

**User's choice:** Mathematical Jitter Loss with 1000ms EMA (Auto-mode recommended selection)
**Notes:** Eliminates the "fake 60 FPS" flaw on 60Hz/90Hz/120Hz panels without requiring root or Shizuku permissions.

---

## Telemetry Repository Architecture

| Option | Description | Selected |
|--------|-------------|----------|
| Application-Scoped Singleton with WhileSubscribed | Single source of truth using Kotlin SharingStarted.WhileSubscribed(5000L) to eliminate dual polling loops | ✓ |
| Eager Process Singleton | Runs continuously from app boot regardless of UI observers | |
| Foreground Service-Bound | Tied exclusively to Service lifecycle and accessed via Binder IPC | |

**User's choice:** Application-Scoped Singleton with WhileSubscribed (Auto-mode recommended selection)
**Notes:** Resolves dual sensor instantiation in DashboardViewModel and HudOverlayService, cutting CPU overhead to <1%.

---

## Asynchronous Latency Probe

| Option | Description | Selected |
|--------|-------------|----------|
| Decoupled 5-Second Background Coroutine | Non-blocking TCP connection to 8.8.8.8:53 with 500ms timeout every 5s on Dispatchers.IO | ✓ |
| Synchronous 1-Second Polling | Checked synchronously inside primary 1000ms metrics loop | |
| ICMP Process Exec | Native ping process execution | |

**User's choice:** Decoupled 5-Second Background Coroutine (Auto-mode recommended selection)
**Notes:** Prevents 500ms network timeout hitching in the hardware telemetry loop and avoids cellular radio battery drain.

---

## the agent's Discretion

- Choice of zero-allocation circular buffer vs primitive array for frame interval deltas.
- Method for hardware refresh rate detection across Android 8 through 15.

## Deferred Ideas

- Advanced per-game historical frame drop logging (v2).
- Dynamic custom FPS target caps (45 FPS / 60 FPS) for high-end hardware (v2).
