# Requirements: Xeno GFX

**Defined:** 2026-11-05
**Core Value:** Deliver a guaranteed, stutter-free 30 FPS gaming experience on memory-constrained devices by aggressively eliminating background resource hogs and maintaining accurate real-time telemetry.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### FPS Telemetry & Display

- [ ] **FPS-01**: User can view a calibrated 30 FPS frame rate on the floating overlay calculated from VSYNC intervals against a 33.33ms target (eliminating fake 60 FPS readings).
- [ ] **FPS-02**: User can switch floating HUD into a minimalist "Stealth" FPS badge (<50dp width, transparent) showing only the live FPS counter.
- [ ] **FPS-03**: User can tap the minimalist FPS badge to toggle into the expanded diagnostic card (displaying RAM %, thermals, and ping).
- [ ] **FPS-04**: User can view network ping latency probed via non-blocking asynchronous socket every 5 seconds with color-coded status (Green <60ms, Yellow 60-120ms, Red >120ms).
- [ ] **FPS-05**: User can drag the floating overlay to any screen position with touch-slop handling and automatic landscape orientation bounds clamping.

### Game Turbo & Process Suppression

- [ ] **TURBO-01**: User can trigger a manual pre-launch RAM purge that calculates and displays the verified memory delta freed.
- [ ] **TURBO-02**: User can launch BGMI or supported battle royale games directly from the dashboard with turbo optimization pre-applied.
- [ ] **TURBO-03**: Continuous background process suppression and game detection run inside `HudOverlayService` so they survive `MainActivity` minimization/destruction.
- [ ] **TURBO-04**: User can enable Full Throttle continuous 30-second RAM suppression while inside an active game session.
- [ ] **TURBO-05**: RAM suppression enforces strict System & Keyboard IME-only whitelist protection, protecting OS, launcher, and IME while evicting background processes.
- [ ] **TURBO-06**: User can control turbo features via persistent notification quick actions ('Purge RAM Now', 'Toggle FPS HUD', 'Exit Turbo').

### Thermal Protection & Hardware Guard

- [ ] **THERM-01**: User can view real-time battery and SoC thermal status via `PowerManager.OnThermalStatusChangedListener` (API 29+) with battery temperature fallback (API 26-28).
- [ ] **THERM-02**: Floating HUD displays a high-visibility pulsating amber/red warning badge when device temperature exceeds 42°C or enters severe throttling status.
- [ ] **THERM-03**: System automatically sheds telemetry CPU load (expands polling interval to 5s and pauses pings) during thermal spikes to avoid CPU heat generation.
- [ ] **THERM-04**: Full throttle suppression continues operating even during low battery conditions (<15%).

### Storage & GFX Configuration

- [ ] **GFX-01**: User settings (tracked games, whitelist, HUD mode, coordinates, turbo toggles) persist across app closes and device reboots via Jetpack Preferences DataStore.
- [ ] **GFX-02**: User can apply BGMI 30 FPS Potato Graphics presets (low shadow bias, 540p/720p resolution scaling) to lock stable 30 FPS.
- [ ] **GFX-03**: Storage Access Framework (`SafStorageManager`) creates an atomic backup (`Active.sav.bak`) before modifying game configuration files.
- [ ] **GFX-04**: User can view in-app graphics recommendations and anti-cheat compliance disclosures.

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Advanced Customization

- **ADV-01**: User can create custom hex GFX graphics presets beyond the built-in 30 FPS profiles.
- **ADV-02**: User can configure per-game individual suppression rules and custom intervals.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Root / Magisk / KernelSU Dependency | 98% of target low-end users do not root; root trips SafetyNet/Play Integrity and voids warranty |
| Shizuku Wireless Debugging Bridge | High onboarding friction; requires Wi-Fi and resets on reboot |
| In-Game Process Memory Injection (Frida / GameGuardian) | Triggers Krafton anti-cheat integrity checks, resulting in 10-year player bans |
| Fake CPU Overclock / 200% Turbo Sliders | Deceptive snake-oil; Android kernel governors cannot be changed by non-root apps |
| Cloud User Accounts & Remote Analytics | Adds network latency, privacy risks, and APK bloat; app is 100% offline |
| In-Game Screen Recording / Floating Browser | Heavy RAM (>80MB) and hardware video encoder overhead causes frame drops on 3GB/4GB devices |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| FPS-01 | Phase 1 | Pending |
| FPS-02 | Phase 4 | Pending |
| FPS-03 | Phase 4 | Pending |
| FPS-04 | Phase 1 | Pending |
| FPS-05 | Phase 4 | Pending |
| TURBO-01 | Phase 3 | Pending |
| TURBO-02 | Phase 3 | Pending |
| TURBO-03 | Phase 3 | Pending |
| TURBO-04 | Phase 3 | Pending |
| TURBO-05 | Phase 3 | Pending |
| TURBO-06 | Phase 3 | Pending |
| THERM-01 | Phase 6 | Pending |
| THERM-02 | Phase 6 | Pending |
| THERM-03 | Phase 6 | Pending |
| THERM-04 | Phase 3 | Pending |
| GFX-01 | Phase 2 | Pending |
| GFX-02 | Phase 5 | Pending |
| GFX-03 | Phase 5 | Pending |
| GFX-04 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 19 total
- Mapped to phases: 19
- Unmapped: 0 ✓

---
*Requirements defined: 2026-11-05*
*Last updated: 2026-11-05 after initial definition*
