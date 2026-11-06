---
phase: "02"
slug: "jetpack-datastore-settings-persistence"
status: draft
nyquist_compliant: true
wave_0_complete: false
created: "2026-11-06"
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (4.13.2) + kotlinx-coroutines-test (1.9.0) |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.xeno.gfx.core.repository.SettingsRepositoryTest"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~6 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 10 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 0 | GFX-01 | — | N/A (Scaffolding & Data Model) | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | GFX-01 | — | IOException fallback to defaults | unit | `./gradlew :app:testDebugUnitTest --tests "com.xeno.gfx.core.repository.SettingsRepositoryTest"` | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | GFX-01 | — | Atomic set mutation | unit | `./gradlew :app:testDebugUnitTest` | ✅ | ⬜ pending |
| 02-02-02 | 02 | 2 | GFX-01 | — | Clamped coordinate persistence | unit | `./gradlew :app:testDebugUnitTest` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/xeno/gfx/core/repository/SettingsRepositoryTest.kt` — Unit test suite verifying default settings values on fresh store, coordinate saving and reading, string set mutation (add/remove tracked games and whitelist), and graceful `IOException` fallback.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Coordinate restoration on device reboot/relaunch | GFX-01 | Requires Android OS process kill or reboot | Drag floating HUD overlay to custom coordinates `(x, y)`, force-close app, relaunch overlay, and verify HUD appears at same position. |
| Boundary clamping on orientation change | GFX-01 | Live display orientation change | Drag overlay to bottom-right corner in portrait, rotate to landscape, verify overlay remains fully on-screen without clipping. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 10s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** verified 2026-11-06
