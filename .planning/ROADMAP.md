# Roadmap: Xeno GFX

## Milestones

- 🚧 **v1.0 MVP Release** — Phases 1–6 (Targeting rock-solid 30 FPS gaming on memory-constrained devices)

## Phases

- [x] **Phase 1: Telemetry Consolidation & 30 FPS Meter Calibration** - Consolidate telemetry collection and calibrate frame cadence to an honest 30 FPS baseline
- [ ] **Phase 2: Jetpack DataStore Settings Persistence** - Provide transactional persistence for user settings, HUD coordinates, and preferences
- [ ] **Phase 3: Persistent Foreground Service & Game Session Decoupling** - Decouple session management and aggressive process suppression into a persistent foreground service
- [ ] **Phase 4: Floating HUD Evolution & Minimalist FPS Badge** - Implement stealth FPS badge, touch-slop dragging, and orientation bounds clamping
- [ ] **Phase 5: BGMI 30 FPS GFX Config Engine** - Build SAF storage bridge with atomic backup and optimized 30 FPS potato graphics presets
- [ ] **Phase 6: Emergency Thermal Protection & System Hardening** - Implement thermal listener, load-shedding cool-down, and strict system whitelist

## Phase Details

### Phase 1: Telemetry Consolidation & 30 FPS Meter Calibration
**Goal:** Consolidate telemetry collection and calibrate frame cadence to an honest 30 FPS baseline with non-blocking socket ping.
**Mode:** mvp
**Depends on:** Nothing (first phase)
**Requirements:** FPS-01, FPS-04
**Success Criteria** (what must be TRUE):
  1. User observes the FPS counter accurately report a calibrated 30 FPS target (clamped against a 33.33ms VSYNC cadence baseline) instead of misleading 60 FPS panel refresh rates.
  2. User observes network ping latency updating every 5 seconds without UI stutter, color-coded for instant health assessment (Green <60ms, Yellow 60-120ms, Red >120ms).
  3. Dashboard and overlay telemetry streams update smoothly from a unified `TelemetryRepository` with CPU overhead measured under 1% on low-end devices.
**Plans:** 2 plans
- [x] 01-01-PLAN.md — Core Engine & Pacing Calibration (DeviceMetrics, FpsSampler, Unit Tests)
- [x] 01-02-PLAN.md — Telemetry Repository Consolidation, Decoupled Socket Latency Probe & UI Integration (TelemetryRepository, DashboardViewModel, HudOverlayService, DashboardScreen, HudOverlayView)
**UI hint**: yes

### Phase 2: Jetpack DataStore Settings Persistence
**Goal:** Provide transactional, non-blocking persistence for user settings, HUD coordinates, display modes, and turbo preferences across app restarts and reboots.
**Mode:** mvp
**Depends on:** Phase 1
**Requirements:** GFX-01
**Success Criteria** (what must be TRUE):
  1. User-configured settings (tracked game package selections, custom whitelists, turbo toggles, and HUD preferences) are immediately retained after force-stopping and reopening the application.
  2. Floating HUD on-screen coordinates and display mode state survive device reboots without reverting to default presets.
  3. Preference reads and writes occur asynchronously via Jetpack DataStore without causing main-thread frame drops or UI hiccups.
**Plans**: TBD
**UI hint**: yes

### Phase 3: Persistent Foreground Service & Game Session Decoupling
**Goal:** Decouple session management and aggressive process suppression into a persistent foreground service with notification controls.
**Mode:** mvp
**Depends on:** Phase 2
**Requirements:** TURBO-01, TURBO-02, TURBO-03, TURBO-04, TURBO-05, TURBO-06, THERM-04
**Success Criteria** (what must be TRUE):
  1. User can trigger a pre-launch RAM purge from the dashboard and immediately observe the verified memory delta freed, or launch games directly with turbo pre-applied.
  2. Background process suppression and game detection remain active in `HudOverlayService` when the dashboard is minimized or destroyed during gameplay.
  3. User can enable Full Throttle continuous 30-second RAM suppression during gameplay, which enforces strict System & Keyboard IME-only whitelist protection even on low battery (<15%).
  4. User can control turbo features directly from the persistent ongoing notification using quick actions ('Purge RAM Now', 'Toggle FPS HUD', 'Exit Turbo').
**Plans**: TBD
**UI hint**: yes

### Phase 4: Floating HUD Evolution & Minimalist FPS Badge
**Goal:** Deliver a distraction-free, customizable floating HUD with an ultra-compact stealth badge and smooth screen dragging.
**Mode:** mvp
**Depends on:** Phase 3
**Requirements:** FPS-02, FPS-03, FPS-05
**Success Criteria** (what must be TRUE):
  1. User can switch the floating HUD into a minimalist "Stealth" FPS badge (<50dp width, transparent) showing only the live FPS counter during gameplay.
  2. User can tap the minimalist badge to smoothly expand it into the full diagnostic card (showing RAM %, thermals, and ping) and tap again to collapse it.
  3. User can drag the floating overlay anywhere across the display with slop-aware touch handling, with automatic clamping preventing it from clipping off-screen during portrait-to-landscape rotations.
**Plans**: TBD
**UI hint**: yes

### Phase 5: BGMI 30 FPS GFX Config Engine
**Goal:** Safely configure BGMI graphics to 30 FPS potato presets with automated atomic backups and anti-cheat disclosures.
**Mode:** mvp
**Depends on:** Phase 2
**Requirements:** GFX-02, GFX-03, GFX-04
**Success Criteria** (what must be TRUE):
  1. User can apply optimized 30 FPS potato graphics presets (low shadow bias, 540p/720p resolution scaling) to lock stable BGMI frame pacing.
  2. Modifying game configuration automatically creates a verified rollback backup (`Active.sav.bak`) via Storage Access Framework before any file changes occur.
  3. User can review clear in-app graphics recommendations and anti-cheat compliance disclosures explaining how configurations maintain account safety.
**Plans**: TBD
**UI hint**: yes

### Phase 6: Emergency Thermal Protection & System Hardening
**Goal:** Protect low-end hardware from thermal throttling via emergency HUD alerts and automatic telemetry load-shedding.
**Mode:** mvp
**Depends on:** Phase 3, Phase 4
**Requirements:** THERM-01, THERM-02, THERM-03
**Success Criteria** (what must be TRUE):
  1. User can view real-time battery and SoC thermal status across Android 8 through Android 15 via native thermal listeners and battery fallback.
  2. Floating HUD displays a pulsating amber or red thermal alert badge whenever device temperature exceeds 42°C or enters severe throttling status.
  3. System automatically sheds telemetry CPU load (stretching polling to 5s and pausing socket pings) when thermal alerts trigger, preventing telemetry from exacerbating device heat.
**Plans**: TBD
**UI hint**: yes

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Telemetry Consolidation & 30 FPS Meter Calibration | 2/2 | Complete | 2026-11-06 |
| 2. Jetpack DataStore Settings Persistence | 0/0 | Not started | - |
| 3. Persistent Foreground Service & Game Session Decoupling | 0/0 | Not started | - |
| 4. Floating HUD Evolution & Minimalist FPS Badge | 0/0 | Not started | - |
| 5. BGMI 30 FPS GFX Config Engine | 0/0 | Not started | - |
| 6. Emergency Thermal Protection & System Hardening | 0/0 | Not started | - |
