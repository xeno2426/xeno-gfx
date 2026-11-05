---
last_mapped_commit: 3db9ba3d13768e0c9b05d70867d81a71845a1d8e
last_mapped_at: 2026-11-05
---
# External Integrations
**Analysis Date:** 2026-11-05

## APIs & External Services

**Network Latency Probing:**
- Google Public DNS (`8.8.8.8:53`) - Used for real-time network latency / ping measurement during gaming sessions
  - SDK/Client: Java Standard Socket (`java.net.Socket`, `java.net.InetSocketAddress`)
  - Auth: None (public DNS port 53 raw socket probe)
  - Implementation: `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:141-144`

**External App Integration (Games & Launchers):**
- BGMI & PUBG Mobile Family (`com.pubg.imobile`, `com.tencent.ig`, `com.tencent.iglite`, `com.pubg.krmobile`, `com.vng.pubgmobile`)
  - Purpose: Targeted game launch acceleration, memory purge whitelist exception, and continuous background suppression target
  - Integration: Android Intent Launcher (`packageManager.getLaunchIntentForPackage`) and package identity checking
  - Files: `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt:21-27`, `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt:139-143`
- System Launcher & Active IME:
  - Purpose: Dynamically resolved to protect the user's home screen launcher and virtual keyboard from being killed during RAM cleaning cycles
  - Integration: Android `Intent(Intent.ACTION_MAIN, CATEGORY_HOME)` via `PackageManager` and `InputMethodManager.enabledInputMethodList`
  - Files: `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt:48-70`

**Android OS System Subsystems:**
- `ActivityManager`: Process memory statistics (`getMemoryInfo`) and background process termination (`killBackgroundProcesses`) (`app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt:65`)
- `UsageStatsManager`: Polling system activity events (`UsageEvents.Event.ACTIVITY_RESUMED`) to trigger HUD overlay automatically upon game launch (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt:77-96`)
- `WindowManager`: Hosting floating overlay window views over other applications (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`) (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:106`)
- `Choreographer`: Frame cadence measurement via `FrameCallback` for live FPS estimation (`app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt:13,41`)
- `BatteryManager` / `PowerManager`: Broadcast receiver for thermal status (`currentThermalStatus`), battery percentage, charging state, and battery temperature (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:103,152`)
- `DisplayManager`: Hardware display refresh rate query in Hz (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:130`)

## Data Storage

**Databases:**
- None detected (in-memory state management via `StateFlow` and Android system providers; Room + DataStore is mentioned in `README.md` as planned for future per-game profiles and session history, but not yet implemented in dependencies or source code)
  - Connection: Not applicable
  - Client: Not applicable

**File Storage:**
- Local filesystem only (`local.properties` for build configuration; Android internal storage and shared preferences used implicitly by Android OS subsystems)

**Caching:**
- None (metrics are sampled in-memory at 1000ms intervals; no persistent offline disk caching)

## Authentication & Identity

**Auth Provider:**
- None / Not applicable
  - Implementation: Standalone local utility application without user authentication, login screens, or cloud user accounts

## Monitoring & Observability

**Error Tracking:**
- None (crash reporting services such as Firebase Crashlytics or Sentry are not integrated; exceptions are handled via standard try/catch blocks with fallback defaults in `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:180` and `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:145`)

**Logs:**
- Android Logcat / standard stderr (no external log shipping or centralized log aggregation)

## CI/CD & Deployment

**Hosting:**
- GitHub Releases (`.github/workflows/release.yml`) - Standalone APK artifact distribution (`xeno-gfx.apk`, `xenp-gfx.apk`)

**CI Pipeline:**
- GitHub Actions (`.github/workflows/release.yml`)
  - Triggers on git tags matching `v*` and manual triggers via `workflow_dispatch`
  - Runs `./gradlew assembleDebug --stacktrace` on `ubuntu-latest` with JDK 21 Temurin
  - Releases build outputs using `softprops/action-gh-release@v2`

## Environment Configuration

**Required env vars:**
- `GITHUB_TOKEN`: Required in CI runner for GitHub Actions release deployment (`.github/workflows/release.yml:69`)
- `sdk.dir`: Required in `local.properties` for local Gradle builds pointing to the Android SDK installation path (`local.properties:1`)

**Secrets location:**
- GitHub repository secrets (`secrets.GITHUB_TOKEN`)
- Local development secrets/configs: None (no API keys, tokens, or backend credentials required for application functionality)

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

---

*Integration audit: 2026-11-05*
