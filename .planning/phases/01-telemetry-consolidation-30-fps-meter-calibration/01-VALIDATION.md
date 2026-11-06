---
phase: "01"
slug: "telemetry-consolidation-30-fps-meter-calibration"
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-11-06"
---

# Phase 01 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (4.13.2) + kotlinx-coroutines-test (1.9.0) |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~5 seconds |

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
| 01-01-01 | 01 | 0 | FPS-01, FPS-04 | — | N/A | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 01-01-02 | 01 | 1 | FPS-01 | — | Coerce FPS 0..30 | unit | `./gradlew :app:testDebugUnitTest --tests "com.xeno.gfx.core.stats.FpsSamplerTest"` | ❌ W0 | ⬜ pending |
| 01-02-01 | 02 | 1 | FPS-04 | — | Safe socket timeout | unit | `./gradlew :app:testDebugUnitTest --tests "com.xeno.gfx.core.repository.TelemetryRepositoryTest"` | ❌ W0 | ⬜ pending |
| 01-02-02 | 02 | 2 | FPS-01, FPS-04 | — | Safe receiver unregister | unit | `./gradlew :app:testDebugUnitTest` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/xeno/gfx/core/stats/FpsSamplerTest.kt` — Stubs and unit tests verifying 30 FPS calibration formulas, dropped VSYNC pulse detection, and EMA smoothing.
- [ ] `app/src/test/java/com/xeno/gfx/core/repository/TelemetryRepositoryTest.kt` — Unit tests verifying decoupled latency probe, demand-driven `WhileSubscribed` flow behavior, and timeout handling.
- [ ] Update `app/src/test/java/com/xeno/gfx/core/model/DeviceMetricsTest.kt` — Assertions for new semantic properties (`calibratedFps`, `rawRefreshRate`, `latencyMs`).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Visual HUD color thresholds on live display | FPS-04 | UI visual inspection on Android device | Verify ping displays in green (<60ms), yellow (60-120ms), and red (>120ms) on dashboard and overlay. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 10s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending 2026-11-06
