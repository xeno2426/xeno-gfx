---
last_mapped_commit: 3db9ba3d13768e0c9b05d70867d81a71845a1d8e
last_mapped_at: 2026-11-05
---
# Technology Stack
**Analysis Date:** 2026-11-05

## Languages

**Primary:**
- Kotlin 2.0.21 - Android application logic, Jetpack Compose UI components, foreground services, coroutines, and hardware metrics telemetry (`app/src/main/java/com/xeno/gfx/`)

**Secondary:**
- Kotlin DSL (Gradle KTS) - Build configuration, dependency catalog integration, and Android compilation rules (`build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`)
- XML - Android manifest declaration, permissions, launcher icons, theme styles, and localization string resources (`app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values/colors.xml`)
- YAML - Automated CI/CD build and GitHub Release workflow definitions (`.github/workflows/release.yml`)

## Runtime

**Environment:**
- Android Runtime (ART) / Dalvik VM on Android 8.0+ (API Level 26+)
- Minimum SDK: API 26 (Android 8.0 Oreo) (`app/build.gradle.kts:13`)
- Target SDK: API 35 (Android 15 Vanilla Ice Cream) (`app/build.gradle.kts:14`)
- Compile SDK: API 35 (`app/build.gradle.kts:9`)
- Java / JVM Compatibility: Java 21 (`JavaVersion.VERSION_21` in `app/build.gradle.kts:40-41`)

**Package Manager:**
- Gradle 8.11.1 via Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties:3`)
- Dependency Catalog: TOML-based Gradle Version Catalog (`gradle/libs.versions.toml`)
- Lockfile: Not present (fixed dependency version pinning via version catalog)

## Frameworks

**Core:**
- AndroidX / Jetpack Suite:
  - Jetpack Compose (BOM `2024.11.00`, Material 3 `1.3.1`, Activity Compose `1.9.3`) - Modern declarative reactive UI framework powering both the main dashboard app and the floating WindowManager HUD overlay (`app/src/main/java/com/xeno/gfx/ui/`)
  - Jetpack Lifecycle (`2.8.7`) - `lifecycle-runtime-ktx`, `lifecycle-service`, `lifecycle-viewmodel-compose` providing lifecycle management, ViewModel state hoisting, and custom service lifecycle integration (`app/src/main/java/com/xeno/gfx/service/ComposeLifecycleOwner.kt`)
  - AndroidX Core KTX (`1.15.0`) - Kotlin extensions for foundational Android platform APIs (`app/build.gradle.kts:56`)
- Kotlinx Coroutines (`1.9.0`):
  - `kotlinx-coroutines-core` & `kotlinx-coroutines-android` - Asynchronous background operations, periodic timer sampling, RAM suppression loops, and reactive state management via `StateFlow` (`app/build.gradle.kts:71-72`)

**Testing:**
- JUnit 4 (`4.13.2`) - Unit testing runner (`app/src/test/java/com/xeno/gfx/`)
- AndroidX Test JUnit (`1.2.1`) - Android instrumentation test runner extension (`app/build.gradle.kts:77`)
- Espresso Core (`3.6.1`) - Android UI test automation framework (`app/build.gradle.kts:78`)
- Kotlinx Coroutines Test (`1.9.0`) - Coroutine test dispatchers and test schedulers (`app/build.gradle.kts:76`)

**Build/Dev:**
- Android Gradle Plugin (AGP `8.7.2`) (`gradle/libs.versions.toml:2,41`)
- Kotlin Android Gradle Plugin (`2.0.21`) (`gradle/libs.versions.toml:3,42`)
- Jetpack Compose Compiler Plugin (`2.0.21`) (`gradle/libs.versions.toml:3,43`)
- R8 / ProGuard (`getDefaultProguardFile("proguard-android-optimize.txt")`, `app/proguard-rules.pro`) - Code shrinking and optimization enabled for release builds (`isMinifyEnabled = true` in `app/build.gradle.kts:26`)

## Key Dependencies

**Critical:**
- `androidx.compose.material3:material3` (`1.3.1`) - Material Design 3 design system components, dark theme tokens, cards, buttons, switches, and iconography (`app/src/main/java/com/xeno/gfx/ui/dashboard/DashboardScreen.kt`)
- `androidx.compose.ui:ui` & `androidx.compose.ui:ui-graphics` (`BOM 2024.11.00`) - Core layout primitives, custom draw modifiers, and color space modeling (`app/src/main/java/com/xeno/gfx/ui/theme/Color.kt`)
- `androidx.activity:activity-compose` (`1.9.3`) - Integration between `ComponentActivity` and Compose hierarchy via `setContent` (`app/src/main/java/com/xeno/gfx/ui/MainActivity.kt:20`)
- `androidx.lifecycle:lifecycle-service` (`2.8.7`) - Architecture component lifecycle support for Android background services (`app/src/main/java/com/xeno/gfx/service/HudOverlayService.kt`)
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` (`1.9.0`) - Main thread and IO thread pool coroutine dispatchers (`app/src/main/java/com/xeno/gfx/core/stats/StatsCollector.kt`)

**Infrastructure:**
- `com.android.application` (`8.7.2`) - Standard build plugin for packaging Android `.apk` binaries
- `actions/setup-java@v4` & `gradle/actions/setup-gradle@v4` - Automated CI build environment setup in GitHub Actions (`.github/workflows/release.yml:27-34`)
- `softprops/action-gh-release@v2` - Automated GitHub Release publishing for standalone APK artifacts (`.github/workflows/release.yml:49-68`)

## Configuration

**Environment:**
- Android SDK path configured locally via `local.properties` (`sdk.dir=/home/codespace/android-sdk`)
- Gradle daemon memory allocation, character encoding, and AndroidX options configured in `gradle.properties` (`org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `kotlin.code.style=official`)
- Android platform permissions and service properties declared in `app/src/main/AndroidManifest.xml`:
  - `android.permission.SYSTEM_ALERT_WINDOW` (floating HUD overlay)
  - `android.permission.PACKAGE_USAGE_STATS` (game foreground detection)
  - `android.permission.KILL_BACKGROUND_PROCESSES` (RAM optimizer turbo purge)
  - `android.permission.FOREGROUND_SERVICE` & `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` (continuous HUD background service)
  - `android.permission.INTERNET` & `android.permission.ACCESS_NETWORK_STATE` (network latency ping)
  - `android.permission.POST_NOTIFICATIONS` (Android 13+ HUD notification)
  - `android.permission.RECEIVE_BOOT_COMPLETED` (boot recovery capability)

**Build:**
- Root build script: `build.gradle.kts`
- Project settings script: `settings.gradle.kts`
- Application module build script: `app/build.gradle.kts`
- Version Catalog: `gradle/libs.versions.toml`
- Wrapper properties: `gradle/wrapper/gradle-wrapper.properties`
- ProGuard rules: `app/proguard-rules.pro`

## Platform Requirements

**Development:**
- JDK: Java 21 (Temurin / OpenJDK 21+)
- Android SDK: Platform API 35 (Android 15) with Build-Tools
- Build System: Gradle 8.11.1 (invoked via `./gradlew`)
- Host OS: Linux, macOS, or Windows

**Production:**
- Target Platform: Android 8.0 (API Level 26, `Oreo`) or higher
- Target Device Profiles: Low-RAM hardware (4GB RAM devices optimized for BGMI/PUBG Mobile) up to high-end devices
- Target Architectures: Multi-architecture APK (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`)
- Privileged Permissions Required: `Settings.canDrawOverlays` (SYSTEM_ALERT_WINDOW) and `AppOpsManager.OPSTR_GET_USAGE_STATS` (Usage Access)

---

*Stack analysis: 2026-11-05*
