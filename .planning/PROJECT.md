# Xeno GFX

## What This Is

Xeno GFX is an Android gaming performance optimizer and real-time floating HUD utility engineered specifically for low-RAM devices and stable 30 FPS gameplay. It provides aggressive continuous background process suppression, calibrated FPS and hardware telemetry monitoring, emergency thermal protection, and BGMI graphic configuration presets to eliminate frame drops and stutter.

## Core Value

Deliver a guaranteed, stutter-free 30 FPS gaming experience on memory-constrained devices by aggressively eliminating background resource hogs and maintaining accurate real-time telemetry.

## Requirements

### Validated

<!-- Inferred from existing codebase implementation -->

- ✓ Floating WindowManager overlay HUD service (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`)
- ✓ Android VSYNC compositor cadence sampling via Choreographer (`app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt`)
- ✓ Telemetry polling for RAM footprint, battery temperature, and network ping (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`)
- ✓ Background game launch detection via UsageStatsManager (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt`)
- ✓ Manual and automated memory purge via `ActivityManager.killBackgroundProcesses` (`app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`)
- ✓ Cyberpunk-styled Material 3 Compose dashboard interface (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt`)
- ✓ System service, keyboard IME, and launcher whitelist protection (`app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt`)

### Active

- [ ] Calibrate and cap FPS meter to accurately reflect target 30 FPS device display and rendering capability
- [ ] Decouple `ForegroundGameDetector` and continuous suppression from Activity `viewModelScope` so they survive app minimization
- [ ] Full Game Turbo Suite: Aggressive continuous background killer running every 30s during gameplay (Full Throttle mode)
- [ ] Strict System & Input-only app protection during RAM purge to maximize available gaming memory
- [ ] Minimalist floating FPS overlay badge (ultra-small transparent live FPS counter)
- [ ] Emergency thermal cool-down: notify user and execute background RAM purge when device temperature spikes
- [ ] Persistent ongoing notification with quick actions ('Purge RAM Now', 'Toggle FPS HUD', 'Exit Turbo')
- [ ] Jetpack DataStore integration for instant auto-save and persistent settings across reboots
- [ ] BGMI 30 FPS GFX Config Tool with optimized graphic presets (`Active.sav` / config profiles)
- [ ] Consolidate telemetry collection into a single repository/source of truth (eliminate duplicate sockets & sensors)
- [ ] Complete codebase hardening across HUD customization flags, error handling, and memory leaks

### Out of Scope

- Root / Shizuku dependency — Keep the app 100% non-root, relying on standard Android user permissions and foreground services
- Cloud user accounts / Remote backend — Standalone local on-device utility only
- Complex social app whitelisting — User selected system-only protection to maximize available RAM for gaming performance

## Context

- Target ecosystem: Low-RAM Android devices (3GB/4GB RAM) running BGMI, Free Fire, COD Mobile, and PUBG variants
- Runtime environment: Android 8.0+ (API 26) through Android 15 (API 35) on Java 21 with Jetpack Compose
- Prior known issue: FPS meter shows "fake" 60 FPS because Choreographer measures the overlay window's display VSYNC rate rather than the game's actual frame cadence
- Prior architectural defect: Background suppression and game detection are cancelled when the user enters the game because `viewModelScope` terminates when `MainActivity` is paused/killed

## Constraints

- **Compatibility**: Android API 26+ (minSdk 26, targetSdk 35) — Must support Android 8.0 through Android 15
- **Permissions**: Must function within `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, and `KILL_BACKGROUND_PROCESSES` without requiring root
- **Performance**: Overlay telemetry and background polling must consume negligible CPU (<1%) to avoid introducing micro-stutters
- **Memory**: Optimizer app memory footprint must remain under 30MB RAM

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Cap & Calibrate FPS Meter | Display VSYNC currently misleads users; calibrating to device render targets provides realistic frame rates without root | — Pending |
| Aggressive 30s RAM Killer (Full Throttle) | Freeing maximum memory every 30 seconds prevents Android LMK from dropping game frames, even on low battery | — Pending |
| System & Input Only Whitelist | Protect only Android OS, launcher, and active IME; sacrifice background apps for maximum game memory | — Pending |
| Minimalist FPS Badge | Ultra-small transparent badge keeps screen clear for competitive gaming visibility | — Pending |
| Persistent DataStore Storage | Replace zero-persistence in-memory state with Jetpack DataStore preferences | — Pending |
| GFX Config Modifier | Directly configure BGMI active graphics presets for smooth 30 FPS performance | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-11-05 after initialization*
