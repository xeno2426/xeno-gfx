<!-- GSD:project-start source:PROJECT.md -->

## Project

**Xeno GFX**

Xeno GFX is an Android gaming performance optimizer and real-time floating HUD utility engineered specifically for low-RAM devices and stable 30 FPS gameplay. It provides aggressive continuous background process suppression, calibrated FPS and hardware telemetry monitoring, emergency thermal protection, and BGMI graphic configuration presets to eliminate frame drops and stutter.

**Core Value:** Deliver a guaranteed, stutter-free 30 FPS gaming experience on memory-constrained devices by aggressively eliminating background resource hogs and maintaining accurate real-time telemetry.

### Constraints

- **Compatibility**: Android API 26+ (minSdk 26, targetSdk 35) — Must support Android 8.0 through Android 15
- **Permissions**: Must function within `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, and `KILL_BACKGROUND_PROCESSES` without requiring root
- **Performance**: Overlay telemetry and background polling must consume negligible CPU (<1%) to avoid introducing micro-stutters
- **Memory**: Optimizer app memory footprint must remain under 30MB RAM

<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

## Languages

- Kotlin 2.0.21 - Android application logic, Jetpack Compose UI components, foreground services, coroutines, and hardware metrics telemetry (`app/src/main/java/com/xeno/gfx/`)
- Kotlin DSL (Gradle KTS) - Build configuration, dependency catalog integration, and Android compilation rules (`build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`)
- XML - Android manifest declaration, permissions, launcher icons, theme styles, and localization string resources (`app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values/colors.xml`)
- YAML - Automated CI/CD build and GitHub Release workflow definitions (`.github/workflows/release.yml`)

## Runtime

- Android Runtime (ART) / Dalvik VM on Android 8.0+ (API Level 26+)
- Minimum SDK: API 26 (Android 8.0 Oreo) (`app/build.gradle.kts:13`)
- Target SDK: API 35 (Android 15 Vanilla Ice Cream) (`app/build.gradle.kts:14`)
- Compile SDK: API 35 (`app/build.gradle.kts:9`)
- Java / JVM Compatibility: Java 21 (`JavaVersion.VERSION_21` in `app/build.gradle.kts:40-41`)
- Gradle 8.11.1 via Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties:3`)
- Dependency Catalog: TOML-based Gradle Version Catalog (`gradle/libs.versions.toml`)
- Lockfile: Not present (fixed dependency version pinning via version catalog)

## Frameworks

- AndroidX / Jetpack Suite:
- Kotlinx Coroutines (`1.9.0`):
- JUnit 4 (`4.13.2`) - Unit testing runner (`app/src/test/java/com/xeno/gfx/`)
- AndroidX Test JUnit (`1.2.1`) - Android instrumentation test runner extension (`app/build.gradle.kts:77`)
- Espresso Core (`3.6.1`) - Android UI test automation framework (`app/build.gradle.kts:78`)
- Kotlinx Coroutines Test (`1.9.0`) - Coroutine test dispatchers and test schedulers (`app/build.gradle.kts:76`)
- Android Gradle Plugin (AGP `8.7.2`) (`gradle/libs.versions.toml:2,41`)
- Kotlin Android Gradle Plugin (`2.0.21`) (`gradle/libs.versions.toml:3,42`)
- Jetpack Compose Compiler Plugin (`2.0.21`) (`gradle/libs.versions.toml:3,43`)
- R8 / ProGuard (`getDefaultProguardFile("proguard-android-optimize.txt")`, `app/proguard-rules.pro`) - Code shrinking and optimization enabled for release builds (`isMinifyEnabled = true` in `app/build.gradle.kts:26`)

## Key Dependencies

- `androidx.compose.material3:material3` (`1.3.1`) - Material Design 3 design system components, dark theme tokens, cards, buttons, switches, and iconography (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt`)
- `androidx.compose.ui:ui` & `androidx.compose.ui:ui-graphics` (`BOM 2024.11.00`) - Core layout primitives, custom draw modifiers, and color space modeling (`app/src/main/java/com/xeno/gfx/ui/theme/Color.kt`)
- `androidx.activity:activity-compose` (`1.9.3`) - Integration between `ComponentActivity` and Compose hierarchy via `setContent` (`app/src/main/java/com/xeno/gfx/ui/MainActivity.kt:20`)
- `androidx.lifecycle:lifecycle-service` (`2.8.7`) - Architecture component lifecycle support for Android background services (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`)
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` (`1.9.0`) - Main thread and IO thread pool coroutine dispatchers (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`)
- `com.android.application` (`8.7.2`) - Standard build plugin for packaging Android `.apk` binaries
- `actions/setup-java@v4` & `gradle/actions/setup-gradle@v4` - Automated CI build environment setup in GitHub Actions (`.github/workflows/release.yml:27-34`)
- `softprops/action-gh-release@v2` - Automated GitHub Release publishing for standalone APK artifacts (`.github/workflows/release.yml:49-68`)

## Configuration

- Android SDK path configured locally via `local.properties` (`sdk.dir=/home/codespace/android-sdk`)
- Gradle daemon memory allocation, character encoding, and AndroidX options configured in `gradle.properties` (`org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `kotlin.code.style=official`)
- Android platform permissions and service properties declared in `app/src/main/AndroidManifest.xml`:
- Root build script: `build.gradle.kts`
- Project settings script: `settings.gradle.kts`
- Application module build script: `app/build.gradle.kts`
- Version Catalog: `gradle/libs.versions.toml`
- Wrapper properties: `gradle/wrapper/gradle-wrapper.properties`
- ProGuard rules: `app/proguard-rules.pro`

## Platform Requirements

- JDK: Java 21 (Temurin / OpenJDK 21+)
- Android SDK: Platform API 35 (Android 15) with Build-Tools
- Build System: Gradle 8.11.1 (invoked via `./gradlew`)
- Host OS: Linux, macOS, or Windows
- Target Platform: Android 8.0 (API Level 26, `Oreo`) or higher
- Target Device Profiles: Low-RAM hardware (4GB RAM devices optimized for BGMI/PUBG Mobile) up to high-end devices
- Target Architectures: Multi-architecture APK (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`)
- Privileged Permissions Required: `Settings.canDrawOverlays` (SYSTEM_ALERT_WINDOW) and `AppOpsManager.OPSTR_GET_USAGE_STATS` (Usage Access)

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

## Naming Patterns

- Kotlin classes and objects: PascalCase matching declaration (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt`, `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`, `app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt`).
- Jetpack Compose UI screens/views: PascalCase matching the UI component (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt`, `app/src/main/java/com/xeno/gfx/ui/overlay/HudOverlayView.kt`).
- Design system and theme files: PascalCase nouns (`app/src/main/java/com/xeno/gfx/ui/theme/Color.kt`, `app/src/main/java/com/xeno/gfx/ui/theme/Theme.kt`, `app/src/main/java/com/xeno/gfx/ui/theme/Type.kt`).
- Unit test files: PascalCase suffixed with `Test.kt` matching the target class (`app/src/test/java/com/xeno/gfx/core/optimizer/AppWhitelistTest.kt`, `app/src/test/java/com/xeno/gfx/core/model/DeviceMetricsTest.kt`).
- Android XML resources: lowercase snake_case (`app/src/main/res/values/strings.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`).
- Gradle build scripts: lowercase with extensions (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`).
- Standard member functions and utility methods: camelCase starting with an imperative verb (`purgeMemory()`, `startSampling()`, `checkForegroundApp()`, `refreshPermissions()`, `isProtected()`).
- Jetpack Compose `@Composable` functions: PascalCase nouns or descriptive widget names (`DashboardScreen`, `BgmiRamTurboCard`, `HeaderSection`, `PermissionCard`, `TelemetryPreviewCard`, `HudOverlayView`, `MetricRow`).
- Test functions: Backtick enclosed plain-English descriptive phrases (`fun \`test critical system packages are whitelisted\`()`, `fun \`test BGMI package variants are recognized\`()`).
- Callback and event lambda properties: camelCase with `on` prefix (`onGameLaunched`, `onGameExited`, `onFpsCalculated`, `onCloseClick`, `onCleanNow`).
- Local variables and parameters: camelCase (`packageName`, `ioDispatcher`, `targetGamePackage`, `userWhitelist`, `beforeAvailMb`).
- Private mutable StateFlow backing properties: camelCase prefixed with an underscore (`_uiState`, `_isSuppressionActive`, `_lastCleanResult`, `_metrics`).
- Public immutable StateFlow exposures: camelCase without underscore (`uiState`, `isSuppressionActive`, `lastCleanResult`, `metrics`).
- Constants in `companion object` or `object`: UPPER_SNAKE_CASE (`HUD_NOTIFICATION_CHANNEL_ID`, `ACTION_START`, `ACTION_STOP`, `NOTIFICATION_ID`, `BGMI_PACKAGE_NAME`).
- Constant sets: UPPER_SNAKE_CASE (`CRITICAL_SYSTEM_PACKAGES`, `SUPPORTED_BGMI_VARIANTS`).
- Design system color tokens: PascalCase (`CyberCyan`, `NeonGreen`, `AmberWarning`, `NeonRed`, `DarkBackground`, `DarkSurface`, `HudPillBackground`).
- Classes and Objects: PascalCase (`XenoGfxApp`, `ForegroundGameDetector`, `RamOptimizer`, `AppWhitelist`, `PermissionHelper`, `StatsCollector`, `FpsSampler`, `ComposeLifecycleOwner`, `HudOverlayService`, `MainActivity`, `DashboardViewModel`).
- Data models and UI state holders: PascalCase (`DeviceMetrics`, `HudConfig`, `TrackedGame`, `DashboardUiState`, `CleanResult`).
- Enums: PascalCase enum name (`ThermalStatus`) with UPPER_SNAKE_CASE values (`NORMAL`, `LIGHT`, `MODERATE`, `SEVERE`, `CRITICAL`, `EMERGENCY`, `SHUTDOWN`, `UNKNOWN`).

## Code Style

- Language: Kotlin 2.0.21 on JVM target 21 (`sourceCompatibility = JavaVersion.VERSION_21` in `app/build.gradle.kts`).
- Indentation: 4 spaces for Kotlin code, 4 spaces for Gradle Kotlin scripts (`build.gradle.kts`), 4 spaces for XML resources.
- Semicolons: Never used in Kotlin source code.
- Line width: Kept under 120 characters where practical.
- Parameter formatting: Multi-line function signatures and Composable calls use trailing commas or newline breaks before parentheses.
- Configured via Android Gradle Plugin 8.7.2 (`com.android.application`).
- Compiler warning suppressions applied with specific annotations where necessary:

## Import Organization

- Gradle Version Catalog (`gradle/libs.versions.toml`) is used exclusively for library and plugin dependencies.
- No Gradle or Kotlin source path aliases; relative and fully-qualified package names are used throughout.

## Error Handling

- **Specific Exception Swallowing with Recovery:**
- **Socket and Resource Safety with `.use { ... }`:**
- **Null Safety and Safe Calls:**
- **Value Clamping:**

## Logging

- Background services use targeted `e.printStackTrace()` when window attachment fails, followed by clean service termination:
- Observability is driven reactively by publishing events and status updates to `StateFlow` streams (`_uiState`, `_metrics`, `_lastCleanResult`) rather than spamming logcat.

## Comments

- Use class-level KDoc to describe domain responsibility and hardware interactions (e.g., VSYNC frame pacing, memory eviction).
- Use numbered section comments for multi-phase logic gates (e.g., whitelist checking tiers in `AppWhitelist.isProtected`).
- Avoid commenting obvious language constructs; allow expressive property and function names to carry intent.
- Applied on public core classes and key suspending operations:

## Function Design

- Core business logic functions are kept compact (typically 10–35 lines).
- Composable functions are broken down into granular items (`DashboardScreen` delegates to `BgmiRamTurboCard`, `PermissionCard`, `TelemetryPreviewCard`, `HudControlButton`, `AutoHudSection`, `GameRow`).
- Default argument values are utilized extensively to allow simple caller invocation while preserving dependency injection and testability:
- Dispatchers are injected into constructors with defaults (`ioDispatcher: CoroutineDispatcher = Dispatchers.IO` in `ForegroundGameDetector.kt`, `RamOptimizer.kt`, `StatsCollector.kt`).
- Callback lambdas for UI events are passed as parameters rather than passing whole ViewModels down the composable tree.
- Non-nullable return types with sensible empty/zero fallbacks preferred where state is continuous (`Triple<Long, Long, Int>`, `CleanResult`, `DeviceMetrics`).
- Nullable types (`Long?`, `String?`) explicitly used for values that may genuinely be absent (such as network latency when offline or home launcher package on non-standard ROMs).

## Module Design

- Single Gradle module (`:app`) structured into clear domain packages:
- Unidirectional Data Flow (UDF) via Android Architecture Components:
- Stateless singletons implemented as Kotlin `object` for pure helpers (`AppWhitelist`, `PermissionHelper`).

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## System Overview

```text

```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `XenoGfxApp` | Application entry point; initializes low-priority notification channels for foreground services | `app/src/main/java/com/xeno/gfx/XenoGfxApp.kt` |
| `MainActivity` | Launcher activity hosting Compose theme and delegating events to `DashboardViewModel` | `app/src/main/java/com/xeno/gfx/ui/MainActivity.kt` |
| `DashboardViewModel` | State machine coordinating telemetry sampling, app listing, game detection callbacks, and RAM purges | `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt` |
| `DashboardScreen` | Compose UI rendering BGMI turbo card, telemetry cards, permission status, and game toggles | `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt` |
| `HudOverlayService` | Foreground service managing floating window lifecycle, touch drag physics, and Compose hosting | `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt` |
| `ComposeLifecycleOwner` | Synthetic lifecycle, ViewModelStore, and SavedStateRegistry owner required by Compose in a Service | `app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt` |
| `HudOverlayView` | Collapsible floating HUD composable displaying real-time FPS, RAM, thermal status, and ping | `app/src/main/java/com/xeno/gfx/ui/overlay/HudOverlayView.kt` |
| `StatsCollector` | Periodic metrics sampler polling RAM, battery temperature, display refresh rate, and ping | `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt` |
| `FpsSampler` | Frame rate calculator using Android `Choreographer` VSYNC callbacks on the Main Looper | `app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt` |
| `ForegroundGameDetector` | Background polling loop checking `UsageEvents` for `ACTIVITY_RESUMED` events | `app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt` |
| `RamOptimizer` | Memory reclaim engine invoking `killBackgroundProcesses` on non-whitelisted apps | `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt` |
| `AppWhitelist` | Policy engine protecting critical system processes, current launcher, active IME, and game variants | `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt` |
| `PermissionHelper` | Helper utility checking and requesting `SYSTEM_ALERT_WINDOW` and `PACKAGE_USAGE_STATS` | `app/src/main/java/com/xeno/gfx/core/permission/PermissionHelper.kt` |
| `DeviceMetrics` | Domain snapshot model capturing real-time device hardware telemetry and thermal state | `app/src/main/java/com/xeno/gfx/core/model/DeviceMetrics.kt` |
| `HudConfig` | Data model defining HUD visual configuration, update interval, and enabled meters | `app/src/main/java/com/xeno/gfx/core/model/HudConfig.kt` |
| `TrackedGame` | Model representing an installed application tracked for automatic HUD overlay triggers | `app/src/main/java/com/xeno/gfx/core/model/TrackedGame.kt` |

## Pattern Overview

- **Decoupled Service Overlay Architecture:** Unlike standard Android applications where UI is strictly bound to an `Activity`, the primary runtime monitor lives in a Foreground Service (`HudOverlayService`) rendering a `ComposeView` into Android `WindowManager` with `TYPE_APPLICATION_OVERLAY`.
- **Unidirectional Data Flow (UDF):** Hardware sensors update reactive Kotlin `StateFlow<DeviceMetrics>` streams consumed by Jetpack Compose via `collectAsState()`.
- **Safe Process Pruning Hierarchy:** Process termination uses a whitelist checking system services, active keyboards (IMEs), home launcher activities, and supported game package variants before invoking `ActivityManager.killBackgroundProcesses()`.

## Layers

- Purpose: Render user controls, configuration dashboard, and telemetry previews.
- Location: `app/src/main/java/com/xeno/gfx/ui/`
- Contains: `MainActivity.kt`, `DashboardScreen.kt`, `HudOverlayView.kt`, theme files (`Theme.kt`, `Color.kt`, `Type.kt`).
- Depends on: `DashboardViewModel`, `DeviceMetrics`, `HudConfig`, `PermissionHelper`.
- Used by: Android launcher, user interaction.
- Purpose: Maintain UI state, schedule background telemetry loops, listen for game transitions, coordinate RAM optimization tasks.
- Location: `app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardViewModel.kt`
- Contains: `DashboardViewModel`, `DashboardUiState`.
- Depends on: `StatsCollector`, `ForegroundGameDetector`, `RamOptimizer`, `PermissionHelper`, `HudOverlayService`.
- Used by: `MainActivity`, `DashboardScreen`.
- Purpose: Host persistent floating HUD on top of full-screen games while handling OS lifecycle requirements and touch dragging.
- Location: `app/src/main/java/com/xeno/gfx/service/`
- Contains: `HudOverlayService.kt`, `ComposeLifecycleOwner.kt`.
- Depends on: `ComposeView`, `WindowManager`, `StatsCollector`, `HudOverlayView`, `XenoGfxApp`.
- Used by: `DashboardViewModel`, auto-game detector intents.
- Purpose: Interface with low-level Android system services (`ActivityManager`, `UsageStatsManager`, `PowerManager`, `DisplayManager`, `Choreographer`).
- Location: `app/src/main/java/com/xeno/gfx/core/`
- Contains: `stats/` (`StatsCollector`, `FpsSampler`), `optimizer/` (`RamOptimizer`, `AppWhitelist`), `detector/` (`ForegroundGameDetector`), `permission/` (`PermissionHelper`).
- Depends on: Android framework APIs, Kotlin Coroutines, Kotlin Flow.
- Used by: `DashboardViewModel`, `HudOverlayService`.
- Purpose: Immutable data structures and entity definitions.
- Location: `app/src/main/java/com/xeno/gfx/core/model/`
- Contains: `DeviceMetrics.kt`, `HudConfig.kt`, `TrackedGame.kt`.
- Depends on: Pure Kotlin data classes (plus Android `Drawable` for app icons).
- Used by: All layers.

## Data Flow

### Primary Request Path (Telemetry Monitoring)

### Auto-HUD Game Launch Flow

### Manual RAM Purge & Turbo Flow

- UI state is represented as immutable data class `DashboardUiState` managed inside `DashboardViewModel` via private `MutableStateFlow` and exposed as read-only `StateFlow`.
- Real-time hardware telemetry is managed inside `StatsCollector` via `StateFlow<DeviceMetrics>`.
- Overlay drag coordinates and expansion states are maintained locally in Compose using `remember { mutableStateOf(...) }` inside `HudOverlayView` and window layout params in `HudOverlayService`.

## Key Abstractions

- Purpose: Provides a bridge allowing Jetpack Compose to run inside an Android `Service` without an underlying `Activity` or `Fragment`.
- Examples: `app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt`
- Pattern: Adapter / Composite Lifecycle Registry pattern implementing `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`.
- Purpose: Safeguards device stability during aggressive background RAM purge routines.
- Examples: `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt`
- Pattern: Policy / Specification pattern filtering packages by category (system core, launcher, keyboard IME, target game, user custom whitelist).
- Purpose: Normalized hardware state snapshot encapsulating CPU/GPU compositor frame rate, memory footprint, battery thermals, and latency.
- Examples: `app/src/main/java/com/xeno/gfx/core/model/DeviceMetrics.kt`
- Pattern: Value Object / Snapshot pattern.

## Entry Points

- Location: `app/src/main/java/com/xeno/gfx/XenoGfxApp.kt`
- Triggers: Android system application startup.
- Responsibilities: Creates the persistent notification channel (`xeno_gfx_hud_channel`) required for foreground services on Android 8.0+ (API 26+).
- Location: `app/src/main/java/com/xeno/gfx/ui/MainActivity.kt`
- Triggers: User tapping app icon from system launcher (`android.intent.action.MAIN`).
- Responsibilities: Hosts `DashboardScreen` composable, handles activity resume/pause lifecycle hooks, initiates permission requests.
- Location: `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`
- Triggers: Explicit service intent with `ACTION_START` or `ACTION_STOP` sent by `DashboardViewModel` or notification actions.
- Responsibilities: Promotes process to foreground service (`startForeground`), registers floating overlay with `WindowManager`, starts metrics sampling, binds drag gesture handlers.

## Architectural Constraints

- **Threading:**
- **Global State:**
- **Circular Imports:** None detected. Architecture follows a clean unidirectional dependency graph from UI -> Service/ViewModel -> Core -> Model.
- **Service Type Declaration:** Android 14+ (API 34) and Android 15 (API 35) require `foregroundServiceType="specialUse"` with `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" .../>` declared in `AndroidManifest.xml`.
- **Window Overlay Permissions:** Adding views with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` throws a runtime `BadTokenException` if `Settings.canDrawOverlays(context)` is false.

## Anti-Patterns

### Redundant Dual Telemetry Collectors

### Hardcoded Game Package References

### Synchronous Socket Ping in Polling Loop

## Error Handling

- Permission exception silencing: Catching `SecurityException` when querying `UsageStatsManager` (`app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt:78`) or executing `killBackgroundProcesses` (`app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt:67`).
- Network error recovery: Catching generic `Exception` in socket latency check and returning `null` to represent offline status (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt:145-147`).
- Window manager failure recovery: Catching `Exception` when adding overlay view and calling `stopSelf()` if the overlay cannot be displayed (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt:180-183`).

## Cross-Cutting Concerns

- Current logging relies on native `e.printStackTrace()` in `HudOverlayService`.
- Standard Android `Log.d` / `Log.e` or a structured logging library (e.g. JakeWharton Timber) is recommended for production builds.
- Managed centrally by `PermissionHelper` (`app/src/main/java/com/xeno/gfx/core/permission/PermissionHelper.kt`).
- Dual check for `Settings.canDrawOverlays` (System Alert Window) and `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS)` (Usage Access).
- Deep-links directly to Android OS system management settings via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` and `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- Not applicable. Local client-side utility running fully on-device without cloud user authentication.

<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.agents/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-fast` for a trivial task inline, with no subagents and no PLAN.md
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
