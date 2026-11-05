---
last_mapped_commit: 3db9ba3d13768e0c9b05d70867d81a71845a1d8e
last_mapped_at: 2026-11-05
---
<Codebase Structure>
**Analysis Date:** 2026-11-05

## Directory Layout

```
xeno-gfx/
├── app/                                    # Main Android application module
│   ├── build.gradle.kts                    # App-level build config, plugins, and dependencies
│   ├── proguard-rules.pro                  # R8/Proguard code obfuscation & shrinking rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml         # App manifest declaring permissions and components
│       │   ├── java/com/xeno/gfx/
│       │   │   ├── XenoGfxApp.kt           # Custom Application class (notification channels)
│       │   │   ├── core/                   # Core business logic, hardware telemetry, optimizer
│       │   │   │   ├── detector/           # Usage stats polling and foreground game detection
│       │   │   │   │   └── ForegroundGameDetector.kt
│       │   │   │   ├── model/              # Domain models and telemetry data structures
│       │   │   │   │   ├── DeviceMetrics.kt
│       │   │   │   │   ├── HudConfig.kt
│       │   │   │   │   └── TrackedGame.kt
│       │   │   │   ├── optimizer/          # Process eviction and whitelist management
│       │   │   │   │   ├── AppWhitelist.kt
│       │   │   │   └── RamOptimizer.kt
│       │   │   │   ├── permission/         # Special permission checkers and intent builders
│       │   │   │   │   └── PermissionHelper.kt
│       │   │   │   └── stats/              # Real-time hardware telemetry and FPS probes
│       │   │   │       ├── FpsSampler.kt
│       │   │   │       └── StatsCollector.kt
│       │   │   ├── service/                # Foreground service and floating window lifecycle
│       │   │   │   ├── ComposeLifecycleOwner.kt
│       │   │   │   └── HudOverlayService.kt
│       │   │   └── ui/                     # User interface layer (Jetpack Compose)
│       │   │       ├── MainActivity.kt     # Launcher Activity
│       │   │       ├── dashboard/          # Main dashboard screen and ViewModel
│       │   │       │   ├── DashboardScreen.kt
│       │   │       │   └── DashboardViewModel.kt
│       │   │       ├── overlay/            # Floating HUD overlay composable
│       │   │       │   └── HudOverlayView.kt
│       │   │       └── theme/              # Cyberpunk dark theme styling
│       │   │           ├── Color.kt
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   └── res/                        # Android resources (XML drawables, strings, themes)
│       │       ├── drawable/
│       │       ├── mipmap-anydpi-v26/
│       │       └── values/
│       └── test/java/com/xeno/gfx/core/    # Unit tests
│           ├── detector/
│           │   └── TrackedGamesFilterTest.kt
│           ├── model/
│           │   └── DeviceMetricsTest.kt
│           └── optimizer/
│               └── AppWhitelistTest.kt
├── gradle/
│   ├── libs.versions.toml                  # Version catalog (dependencies, plugins)
│   └── wrapper/                            # Gradle wrapper binaries and properties
├── build.gradle.kts                        # Root build script
├── settings.gradle.kts                     # Gradle repository and module settings
├── gradle.properties                       # JVM arguments and Android build properties
└── README.md                               # Project documentation and roadmap
```

## Directory Purposes

**`app/src/main/java/com/xeno/gfx/core/`:**
- Purpose: Houses framework-independent domain logic, hardware diagnostic collectors, process optimization routines, and data models.
- Contains: Pure Kotlin classes, coroutine polling loops, and Android system service wrappers.
- Key files: `StatsCollector.kt`, `FpsSampler.kt`, `RamOptimizer.kt`, `AppWhitelist.kt`, `ForegroundGameDetector.kt`.

**`app/src/main/java/com/xeno/gfx/service/`:**
- Purpose: Implements Android Background/Foreground Services hosting the floating HUD window outside the activity lifecycle.
- Contains: `Service` implementations and lifecycle adapter bridges for Jetpack Compose.
- Key files: `HudOverlayService.kt`, `ComposeLifecycleOwner.kt`.

**`app/src/main/java/com/xeno/gfx/ui/`:**
- Purpose: Jetpack Compose presentation layer comprising the configuration dashboard, floating HUD views, and Material 3 theming.
- Contains: Composable functions, Activity entry points, and Android ViewModels.
- Key files: `MainActivity.kt`, `DashboardViewModel.kt`, `DashboardScreen.kt`, `HudOverlayView.kt`.

**`app/src/test/java/com/xeno/gfx/core/`:**
- Purpose: Unit tests validating business rules, data model constraints, and whitelist protection logic.
- Contains: JUnit 4 test classes.
- Key files: `AppWhitelistTest.kt`, `TrackedGamesFilterTest.kt`, `DeviceMetricsTest.kt`.

**`gradle/`:**
- Purpose: Gradle build automation wrappers and modern version catalog definitions.
- Contains: Version catalog (`libs.versions.toml`) and wrapper properties/JAR.
- Key files: `gradle/libs.versions.toml`.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/xeno/gfx/XenoGfxApp.kt`: Application subclass initializing notification channels.
- `app/src/main/java/com/xeno/gfx/ui/MainActivity.kt`: Launcher Activity entry point for user navigation.
- `app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`: Service entry point for the floating performance HUD overlay.

**Configuration:**
- `app/build.gradle.kts`: Application Gradle build settings, SDK targets (minSdk 26, targetSdk 35), and Compose configurations.
- `gradle/libs.versions.toml`: Centralized dependency versions catalog.
- `app/src/main/AndroidManifest.xml`: Android system permissions, component declarations, and foreground service types.

**Core Logic:**
- `app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`: Primary hardware and network telemetry collection loop.
- `app/src/main/java/com/xeno/gfx/core/stats/FpsSampler.kt`: Choreographer VSYNC framerate calculator.
- `app/src/main/java/com/xeno/gfx/core/optimizer/RamOptimizer.kt`: Background process killer and memory reclamation routine.
- `app/src/main/java/com/xeno/gfx/core/optimizer/AppWhitelist.kt`: Package protection rules for system, launcher, and games.
- `app/src/main/java/com/xeno/gfx/core/detector/ForegroundGameDetector.kt`: Game launch detection via UsageStatsManager.

**Testing:**
- `app/src/test/java/com/xeno/gfx/core/optimizer/AppWhitelistTest.kt`: Tests critical package whitelist rules.
- `app/src/test/java/com/xeno/gfx/core/detector/TrackedGamesFilterTest.kt`: Tests tracked game set operations.
- `app/src/test/java/com/xeno/gfx/core/model/DeviceMetricsTest.kt`: Tests metric bounds and conversions.

## Naming Conventions

**Files:**
- Kotlin classes and objects: PascalCase matching the class/object name (e.g., `ForegroundGameDetector.kt`, `RamOptimizer.kt`).
- Composable UI components: PascalCase with descriptive suffixes (e.g., `DashboardScreen.kt`, `HudOverlayView.kt`).
- Unit tests: PascalCase ending with `Test` (e.g., `AppWhitelistTest.kt`, `DeviceMetricsTest.kt`).
- Gradle build scripts: camelCase with `.gradle.kts` extension (e.g., `build.gradle.kts`, `settings.gradle.kts`).
- Android XML resources: snake_case (e.g., `ic_launcher.xml`, `colors.xml`, `strings.xml`).

**Directories:**
- Package directories: Lowercase, single-word or dot-separated namespace components (e.g., `com/xeno/gfx/core/optimizer`).
- Resource directories: Lowercase, hyphen-delimited configuration qualifiers (e.g., `mipmap-anydpi-v26`, `values`).

## Where to Add New Code

**New Feature (e.g. GFX Graphic Config Profiles):**
- Primary code: Create feature package under `app/src/main/java/com/xeno/gfx/core/gfx/` (e.g. `GfxConfigManager.kt`).
- UI controls: Add composables under `app/src/main/java/com/xeno/gfx/ui/gfx/` and integrate into `DashboardScreen.kt`.
- Tests: Add unit tests under `app/src/test/java/com/xeno/gfx/core/gfx/`.

**New Component/Module (e.g. Battery Health / Thermal Throttling Manager):**
- Implementation: Add to `app/src/main/java/com/xeno/gfx/core/stats/` or `app/src/main/java/com/xeno/gfx/core/thermal/`.
- Wire into `StatsCollector.kt` or `DashboardViewModel.kt`.

**Utilities:**
- Shared helpers: Add to `app/src/main/java/com/xeno/gfx/core/util/` or expand `PermissionHelper.kt` under `core/permission/`.

## Special Directories

**`.planning/`:**
- Purpose: Stores GSD architecture maps, project roadmaps, and execution plans.
- Generated: Semi-automated by GSD workflow tools.
- Committed: Yes.

**`app/build/`:**
- Purpose: Android build outputs, intermediate DEX files, compiled class files, and APKs.
- Generated: Yes (by Gradle).
- Committed: No (ignored via `.gitignore`).

---

*Structure analysis: 2026-11-05*
