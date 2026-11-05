---
last_mapped_commit: 3db9ba3d13768e0c9b05d70867d81a71845a1d8e
last_mapped_at: 2026-11-05
---
# Testing Patterns
**Analysis Date:** 2026-11-05

## Test Framework

**Runner:**
- JUnit 4 (`junit:junit:4.13.2`) for local JVM unit tests.
- Config: `app/build.gradle.kts` via `testImplementation(libs.junit)`.
- Instrumented Runner: `androidx.test.runner.AndroidJUnitRunner` with AndroidX JUnit (`androidx.test.ext:junit:1.2.1`) and Espresso (`androidx.test.espresso:espresso-core:3.6.1`).

**Assertion Library:**
- `org.junit.Assert` (`assertEquals`, `assertTrue`, `assertFalse`).

**Run Commands:**
```bash
./gradlew testDebugUnitTest       # Run all JVM unit tests
./gradlew test --continuous       # Continuous watch mode
./gradlew connectedAndroidTest    # Run instrumented tests on connected device/emulator
```

## Test File Organization

**Location:**
- Unit tests are located in separate test source sets mirroring main source code: `app/src/test/java/com/xeno/gfx/...`.
- Instrumented tests are configured for: `app/src/androidTest/java/com/xeno/gfx/...`.

**Naming:**
- Suffix matching target class: `<TargetClass>Test.kt` (e.g., `AppWhitelistTest.kt`, `DeviceMetricsTest.kt`, `TrackedGamesFilterTest.kt`).

**Structure:**
```text
app/src/test/java/com/xeno/gfx/
└── core/
    ├── detector/
    │   └── TrackedGamesFilterTest.kt
    ├── model/
    │   └── DeviceMetricsTest.kt
    └── optimizer/
        └── AppWhitelistTest.kt
```

## Test Structure

**Suite Organization:**
```kotlin
package com.xeno.gfx.core.optimizer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppWhitelistTest {

    @Test
    fun `test critical system packages are whitelisted`() {
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("android"))
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("com.android.systemui"))
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("com.google.android.gms"))
        assertTrue(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains("com.android.phone"))
    }

    @Test
    fun `test BGMI package variants are recognized`() {
        assertTrue(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains("com.pubg.imobile"))
        assertTrue(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains("com.tencent.ig"))
        assertTrue(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains("com.tencent.iglite"))
    }

    @Test
    fun `test non-whitelisted apps can be terminated`() {
        val testApp = "com.instagram.android"
        assertFalse(AppWhitelist.CRITICAL_SYSTEM_PACKAGES.contains(testApp))
        assertFalse(AppWhitelist.SUPPORTED_BGMI_VARIANTS.contains(testApp))
    }
}
```

**Patterns:**
- **Test Method Naming:** Backtick-quoted full English sentences (`fun \`test default metrics state\`()`).
- **Setup Pattern:** In-line state initialization or default parameter constructors. No heavy `@Before` setup currently required for existing pure logic tests.
- **Assertion Pattern:**
  - Strict equality: `assertEquals(0L, metrics.ramUsedMb)`.
  - Boolean verification: `assertTrue(percent in 0..100)`.
  - Floating-point assertions with explicit delta tolerance:
    ```kotlin
    val rawTempFromIntent = 385 // 38.5 C in tenths
    val tempCelsius = rawTempFromIntent / 10.0f
    assertEquals(38.5f, tempCelsius, 0.01f)
    ```

## Mocking

**Framework:**
- No external mocking library (e.g., MockK, Mockito) is included in `gradle/libs.versions.toml`.
- Coroutine testing support is provided by `kotlinx-coroutines-test:1.9.0` via `testImplementation(libs.kotlinx.coroutines.test)`.

**Patterns:**
- Architecture uses dispatcher injection to facilitate future coroutine testing:
  ```kotlin
  class RamOptimizer(
      private val context: Context,
      private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
  )
  ```
  In tests, `StandardTestDispatcher()` or `UnconfinedTestDispatcher()` can be passed to control execution pacing.

**What to Mock:**
- Android framework services (`ActivityManager`, `UsageStatsManager`, `PackageManager`, `WindowManager`, `PowerManager`, `DisplayManager`) when testing higher-level repository or coordinator classes.
- Network sockets (`Socket`) and battery intent receivers.

**What NOT to Mock:**
- Data models (`DeviceMetrics`, `HudConfig`, `TrackedGame`).
- Mathematical calculation helpers (RAM percentage formulas, FPS counters).
- In-memory collection filters (`AppWhitelist.CRITICAL_SYSTEM_PACKAGES`).

## Fixtures and Factories

**Test Data:**
- Data classes have default constructor arguments which function as lightweight test fixtures:
  ```kotlin
  val metrics = DeviceMetrics()
  assertEquals(0L, metrics.ramUsedMb)
  assertEquals(DeviceMetrics.ThermalStatus.NORMAL, metrics.thermalStatus)
  ```
- Local fixtures are instantiated directly in test bodies:
  ```kotlin
  val tracked = mutableSetOf("com.miHoYo.GenshinImpact", "com.activision.callofduty.shooter")
  ```

**Location:**
- Defined inline within individual test classes (`app/src/test/java/com/xeno/gfx/...`).

## Coverage

**Requirements:**
- None enforced in Gradle build scripts or CI pipelines.

**View Coverage:**
- Test execution reports are generated by Gradle at:
  `app/build/reports/tests/testDebugUnitTest/index.html`
- Test XML logs output to:
  `app/build/test-results/testDebugUnitTest/`

## Test Types

**Unit Tests:**
- Focus on pure business logic, calculations, package filtering, and model invariants in `app/src/test/java/com/xeno/gfx/core/`.
- 100% JVM-based, running in milliseconds without device emulators.

**Integration Tests:**
- Configured in Gradle (`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`), targeting device-specific features (e.g., WindowManager overlay drawing, foreground service notifications).

**E2E / UI Tests:**
- Espresso dependency (`androidx.test.espresso:espresso-core:3.6.1`) and Compose BOM testing library (`androidx.compose.ui:ui-test-junit4`) are configured in Gradle, though no automated UI tests are checked in yet. Compose previews (`@Preview`) provide visual inspection.

## Common Patterns

**Boundary and Conversion Testing:**
```kotlin
@Test
fun `test ram percentage bounds`() {
    val totalMb = 8192L
    val usedMb = 6144L
    val percent = ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt()
    assertEquals(75, percent)
    assertTrue(percent in 0..100)
}
```

**Collection Mutation and Set Operations:**
```kotlin
@Test
fun `test tracked games set operations`() {
    val tracked = mutableSetOf("com.miHoYo.GenshinImpact", "com.activision.callofduty.shooter")

    assertTrue(tracked.contains("com.miHoYo.GenshinImpact"))
    assertFalse(tracked.contains("com.google.android.youtube"))

    tracked.add("com.epicgames.portal")
    assertTrue(tracked.contains("com.epicgames.portal"))

    tracked.remove("com.miHoYo.GenshinImpact")
    assertFalse(tracked.contains("com.miHoYo.GenshinImpact"))
}
```

---

*Testing analysis: 2026-11-05*
