# Project State

## Project Reference

- **Project:** Xeno GFX
- **Core Value:** Deliver a guaranteed, stutter-free 30 FPS gaming experience on memory-constrained devices by aggressively eliminating background resource hogs and maintaining accurate real-time telemetry.
- **Current Milestone:** v1.0 MVP Release
- **Project Mode:** mvp

## Current Position

- **Phase:** Phase 1: Telemetry Consolidation & 30 FPS Meter Calibration
- **Phase Status:** Ready to plan
- **Overall Progress:** 0/6 phases completed (0%)
- **Active Plans:** None

## Performance Metrics

- **Velocity:** Not yet tracked
- **Cycle Time:** Not yet tracked
- **Verification Rate:** Not yet tracked

## Accumulated Context

### Key Decisions

- **Capped & Calibrated FPS Meter:** VSYNC cadence clamped against a 33.33ms baseline to eliminate misleading 60 FPS readings without requiring root or Shizuku.
- **Decoupled Service-Centric Core:** Session management and process suppression host inside `HudOverlayService` on `ServiceScope` so optimization survives `MainActivity` minimization/destruction.
- **Aggressive 30s RAM Killer (Full Throttle):** Periodic RAM eviction during active gameplay to prevent Android low-memory killer (LMK) frame drops.
- **System & Input Only Whitelist:** Strict protection exclusively for OS services, system launcher, and active keyboard IME to maximize memory freed for games.
- **Minimalist "Stealth" FPS Badge:** Ultra-compact (<50dp width) transparent floating counter for zero-distraction competitive gaming.
- **Jetpack Preferences DataStore:** Transactional, non-blocking disk persistence replacing in-memory state and eliminating `fsync` main-thread stutters.
- **BGMI 30 FPS GFX Config Engine:** Safe potato graphics preset generation with Storage Access Framework atomic backups (`Active.sav.bak`) and anti-cheat compliance disclosures.
- **Thermal Load-Shedding:** Prevent thermal inversion by throttling telemetry polling to 5s and pausing socket pings rather than running heavy CPU purges during thermal spikes.

### Blockers & Impediments

- None currently.

### Open Todos

- Ready to plan Phase 1 (`/gsd-plan-phase 1`).

## Session Continuity

- **Last Session:** 2026-11-05 (Project initialization & roadmap creation)
- **Current Focus:** Phase 1 planning & implementation
