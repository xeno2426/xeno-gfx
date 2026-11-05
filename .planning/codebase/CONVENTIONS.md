---
last_mapped_commit: 3db9ba3d13768e0c9b05d70867d81a71845a1d8e
last_mapped_at: 2026-11-05
---
# Coding Conventions
**Analysis Date:** 2026-11-05

## Naming Patterns

**Files:**
- Kotlin classes and objects: PascalCase matching declaration (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt`, `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`, `app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt`).
- Jetpack Compose UI screens/views: PascalCase matching the UI component (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt`, `app/src/main/java/com/xeno/gfx/ui/overlay/HudOverlayView.kt`).
- Design system and theme files: PascalCase nouns (`app/src/main/java/com/xeno/gfx/ui/theme/Color.kt`, `app/src/main/java/com/xeno/gfx/ui/theme/Theme.kt`, `app/src/main/java/com/xeno/gfx/ui/theme/Type.kt`).
- Unit test files: PascalCase suffixed with `Test.kt` matching the target class (`app/src/test/java/com/xeno/gfx/core/optimizer/AppWhitelistTest.kt`, `app/src/test/java/com/xeno/gfx/core/model/DeviceMetricsTest.kt`).
- Android XML resources: lowercase snake_case (`app/src/main/res/values/strings.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`).
- Gradle build scripts: lowercase with extensions (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`).

**Functions:**
- Standard member functions and utility methods: camelCase starting with an imperative verb (`purgeMemory()`, `startSampling()`, `checkForegroundApp()`, `refreshPermissions()`, `isProtected()`).
- Jetpack Compose `@Composable` functions: PascalCase nouns or descriptive widget names (`DashboardScreen`, `BgmiRamTurboCard`, `HeaderSection`, `PermissionCard`, `TelemetryPreviewCard`, `HudOverlayView`, `MetricRow`).
- Test functions: Backtick enclosed plain-English descriptive phrases (`fun \`test critical system packages are whitelisted\`()`, `fun \`test BGMI package variants are recognized\`()`).
- Callback and event lambda properties: camelCase with `on` prefix (`onGameLaunched`, `onGameExited`, `onFpsCalculated`, `onCloseClick`, `onCleanNow`).

**Variables:**
- Local variables and parameters: camelCase (`packageName`, `ioDispatcher`, `targetGamePackage`, `userWhitelist`, `beforeAvailMb`).
- Private mutable StateFlow backing properties: camelCase prefixed with an underscore (`_uiState`, `_isSuppressionActive`, `_lastCleanResult`, `_metrics`).
- Public immutable StateFlow exposures: camelCase without underscore (`uiState`, `isSuppressionActive`, `lastCleanResult`, `metrics`).
- Constants in `companion object` or `object`: UPPER_SNAKE_CASE (`HUD_NOTIFICATION_CHANNEL_ID`, `ACTION_START`, `ACTION_STOP`, `NOTIFICATION_ID`, `BGMI_PACKAGE_NAME`).
- Constant sets: UPPER_SNAKE_CASE (`CRITICAL_SYSTEM_PACKAGES`, `SUPPORTED_BGMI_VARIANTS`).
- Design system color tokens: PascalCase (`CyberCyan`, `NeonGreen`, `AmberWarning`, `NeonRed`, `DarkBackground`, `DarkSurface`, `HudPillBackground`).

**Types:**
- Classes and Objects: PascalCase (`XenoGfxApp`, `ForegroundGameDetector`, `RamOptimizer`, `AppWhitelist`, `PermissionHelper`, `StatsCollector`, `FpsSampler`, `ComposeLifecycleOwner`, `HudOverlayService`, `MainActivity`, `DashboardViewModel`).
- Data models and UI state holders: PascalCase (`DeviceMetrics`, `HudConfig`, `TrackedGame`, `DashboardUiState`, `CleanResult`).
- Enums: PascalCase enum name (`ThermalStatus`) with UPPER_SNAKE_CASE values (`NORMAL`, `LIGHT`, `MODERATE`, `SEVERE`, `CRITICAL`, `EMERGENCY`, `SHUTDOWN`, `UNKNOWN`).

## Code Style

**Formatting:**
- Language: Kotlin 2.0.21 on JVM target 21 (`sourceCompatibility = JavaVersion.VERSION_21` in `app/build.gradle.kts`).
- Indentation: 4 spaces for Kotlin code, 4 spaces for Gradle Kotlin scripts (`build.gradle.kts`), 4 spaces for XML resources.
- Semicolons: Never used in Kotlin source code.
- Line width: Kept under 120 characters where practical.
- Parameter formatting: Multi-line function signatures and Composable calls use trailing commas or newline breaks before parentheses.

**Linting:**
- Configured via Android Gradle Plugin 8.7.2 (`com.android.application`).
- Compiler warning suppressions applied with specific annotations where necessary:
  - `@SuppressLint("ClickableViewAccessibility")` on custom touch listeners in `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`.
  - `@Suppress("DEPRECATION")` on legacy Android window flags in `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`.
  - Kotlin 1.9+ unused lambda/exception syntax (`catch (_: Exception)`) to prevent unused parameter warnings.

## Import Organization

**Order:**
1. Package declaration (`package com.xeno.gfx...`)
2. Android platform SDK imports (`android.*`, `android.app.*`, `android.content.*`, `android.os.*`, `android.view.*`)
3. AndroidX and Jetpack Compose imports (`androidx.activity.*`, `androidx.compose.*`, `androidx.core.*`, `androidx.lifecycle.*`)
4. KotlinX Coroutines imports (`kotlinx.coroutines.*`)
5. Internal project imports (`com.xeno.gfx.*`)
6. Java standard library imports (`java.net.*`)
7. Kotlin standard library imports (`kotlin.math.*`)

*Example from `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`:*
```kotlin
package com.xeno.gfx.core.stats

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import com.xeno.gfx.core.model.DeviceMetrics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
```

**Path Aliases:**
- Gradle Version Catalog (`gradle/libs.versions.toml`) is used exclusively for library and plugin dependencies.
- No Gradle or Kotlin source path aliases; relative and fully-qualified package names are used throughout.

## Error Handling

**Patterns:**
- **Specific Exception Swallowing with Recovery:**
  Use `try ... catch (_: SpecificException)` returning a fallback value when optional or permission-dependent system data cannot be queried.
  ```kotlin
  // From app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt
  val events: UsageEvents? = try {
      usageStatsManager.queryEvents(lastQueryTimestamp, now)
  } catch (_: SecurityException) {
      null
  }
  ```
- **Socket and Resource Safety with `.use { ... }`:**
  Auto-close network sockets or file descriptors safely in resource blocks.
  ```kotlin
  // From app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt
  private fun sampleNetworkLatency(): Long? {
      return try {
          val startTime = System.currentTimeMillis()
          Socket().use { socket ->
              socket.connect(InetSocketAddress("8.8.8.8", 53), 500)
          }
          System.currentTimeMillis() - startTime
      } catch (_: Exception) {
          null
      }
  }
  ```
- **Null Safety and Safe Calls:**
  Use safe calls (`?.`) and Elvis operators (`?:`) for system services that may be null or return null references on specific OEM skins.
  ```kotlin
  // From app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return null
  val enabledImes = imm.enabledInputMethodList
  return enabledImes.firstOrNull()?.packageName
  ```
- **Value Clamping:**
  Use `.coerceAtLeast()`, `.coerceIn()`, and `.coerceAtMost()` to guarantee numeric metrics never violate bounds:
  ```kotlin
  val freedMb = (afterAvailMb - beforeAvailMb).coerceAtLeast(0L)
  val calculatedFps = ((frameCount.toDouble() / elapsedMs.toDouble()) * 1000.0).toInt().coerceIn(1, maxRefreshRate)
  ```

## Logging

**Framework:** Standard Android runtime / `Throwable.printStackTrace()`

**Patterns:**
- Background services use targeted `e.printStackTrace()` when window attachment fails, followed by clean service termination:
  ```kotlin
  // From app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt
  try {
      windowManager.addView(composeView, params)
      overlayView = composeView
  } catch (e: Exception) {
      e.printStackTrace()
      stopSelf()
  }
  ```
- Observability is driven reactively by publishing events and status updates to `StateFlow` streams (`_uiState`, `_metrics`, `_lastCleanResult`) rather than spamming logcat.

## Comments

**When to Comment:**
- Use class-level KDoc to describe domain responsibility and hardware interactions (e.g., VSYNC frame pacing, memory eviction).
- Use numbered section comments for multi-phase logic gates (e.g., whitelist checking tiers in `AppWhitelist.isProtected`).
- Avoid commenting obvious language constructs; allow expressive property and function names to carry intent.

**KDoc/Documentation:**
- Applied on public core classes and key suspending operations:
  ```kotlin
  /**
   * High-performance RAM optimizer designed to maximize available memory for gaming.
   */
  class RamOptimizer( ... ) {
      /**
       * Executes a deep RAM purge, evicting non-whitelisted background/cached processes.
       */
      suspend fun purgeMemory( ... ): CleanResult
  }
  ```

## Function Design

**Size:**
- Core business logic functions are kept compact (typically 10–35 lines).
- Composable functions are broken down into granular items (`DashboardScreen` delegates to `BgmiRamTurboCard`, `PermissionCard`, `TelemetryPreviewCard`, `HudControlButton`, `AutoHudSection`, `GameRow`).

**Parameters:**
- Default argument values are utilized extensively to allow simple caller invocation while preserving dependency injection and testability:
  ```kotlin
  suspend fun purgeMemory(
      targetGamePackage: String = "com.pubg.imobile",
      userWhitelist: Set<String> = emptySet()
  ): CleanResult
  ```
- Dispatchers are injected into constructors with defaults (`ioDispatcher: CoroutineDispatcher = Dispatchers.IO` in `ForegroundGameDetector.kt`, `RamOptimizer.kt`, `StatsCollector.kt`).
- Callback lambdas for UI events are passed as parameters rather than passing whole ViewModels down the composable tree.

**Return Values:**
- Non-nullable return types with sensible empty/zero fallbacks preferred where state is continuous (`Triple<Long, Long, Int>`, `CleanResult`, `DeviceMetrics`).
- Nullable types (`Long?`, `String?`) explicitly used for values that may genuinely be absent (such as network latency when offline or home launcher package on non-standard ROMs).

## Module Design

**Exports & Packaging:**
- Single Gradle module (`:app`) structured into clear domain packages:
  - `com.xeno.gfx.core.detector` — Background app and game detection via UsageStats.
  - `com.xeno.gfx.core.model` — Immutable data definitions (`DeviceMetrics`, `HudConfig`, `TrackedGame`).
  - `com.xeno.gfx.core.optimizer` — Memory eviction logic and system whitelists (`RamOptimizer`, `AppWhitelist`).
  - `com.xeno.gfx.core.permission` — Android runtime permission checks and settings intents (`PermissionHelper`).
  - `com.xeno.gfx.core.stats` — Choreographer frame pacing and device metric collectors (`FpsSampler`, `StatsCollector`).
  - `com.xeno.gfx.service` — Background service and WindowManager Compose lifecycle integration (`HudOverlayService`, `ComposeLifecycleOwner`).
  - `com.xeno.gfx.ui` — UI host activity (`MainActivity`).
  - `com.xeno.gfx.ui.dashboard` — Screen UI and ViewModel (`DashboardScreen`, `DashboardViewModel`).
  - `com.xeno.gfx.ui.overlay` — Floating overlay composable (`HudOverlayView`).
  - `com.xeno.gfx.ui.theme` — Theme, colors, typography definitions (`Theme`, `Color`, `Type`).

**State Management & Reactive Architecture:**
- Unidirectional Data Flow (UDF) via Android Architecture Components:
  - ViewModels hold private `MutableStateFlow` and expose read-only `StateFlow`.
  - Jetpack Compose UI observes state via `collectAsState()`.
  - Event triggers flow upstream via lambdas (`onToggleManualHud`, `onManualCleanRam`).
- Stateless singletons implemented as Kotlin `object` for pure helpers (`AppWhitelist`, `PermissionHelper`).

---

*Convention analysis: 2026-11-05*
