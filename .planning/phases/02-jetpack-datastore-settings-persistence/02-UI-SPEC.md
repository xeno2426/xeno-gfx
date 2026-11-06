---
phase: "02"
title: "UI Design Contract: Jetpack DataStore Settings Persistence"
status: "draft"
created: "2026-11-06"
tool: "none"
framework: "Jetpack Compose 1.3.1 (Material 3)"
---

# UI Design Contract: Phase 02 Jetpack DataStore Settings Persistence

## 1. Executive Overview

### Phase Boundary & Visual Goals
Phase 02 delivers transactional, non-blocking persistence for user settings, HUD coordinates, display modes, and turbo preferences across app restarts and reboots via Jetpack Preferences DataStore (`androidx.datastore:datastore-preferences:1.1.3`). 

The visual and interaction contract governs how persistent settings (continuous suppression toggle, auto-HUD toggle, tracked battle royale game packages, user whitelist, and floating HUD on-screen coordinates) are hydrated, visually rendered, and asynchronously persisted on both the main dashboard (`DashboardScreen`) and the floating overlay (`HudOverlayService` / `HudOverlayView`).

### Core Design Objectives
1. **Flicker-Free Cold-Boot Hydration:** Align initial in-memory UI state models (`DashboardUiState`) exactly with DataStore defaults (`isContinuousSuppressionEnabled = true`, `isAutoHudEnabled = false`, HUD position `x = 20, y = 100`) so that toggles and switches do not suffer visual popping or animated jitter during cold startup.
2. **Smooth, Non-Blocking Mutation Feedback:** Execute all preference mutations asynchronously on `Dispatchers.IO` via Kotlin coroutines without blocking the Android Main Looper, ensuring 0% dropped frames during user interactions.
3. **Decoupled Drag Physics & Atomic Release Persistence:** Keep active touch dragging (`MotionEvent.ACTION_MOVE`) 100% in-memory with zero disk I/O; persist final clamped HUD coordinates exclusively on touch release (`MotionEvent.ACTION_UP`) with automatic screen bounds clamping (`0 <= x <= screenWidth - hudWidth`, `0 <= y <= screenHeight - hudHeight`).
4. **Resilient Silent Fallback:** Catch disk I/O errors (`IOException`) gracefully with `emptyPreferences()`, ensuring user interactions never throw unhandled exceptions or present disruptive error dialogs.
5. **High-Contrast Cyberpunk Visual Consistency:** Maintain the established 60-30-10 color distribution, 8-point spatial grid (multiples of 4dp), monospace tabular digits for all coordinates and telemetry metrics, and 44dp minimum touch targets.

### Traceability
- **Requirements Covered:** `GFX-01` (User settings persist across app closes and device reboots via Jetpack Preferences DataStore).
- **Implementation Decisions Enforced:**
  - `D-01`: Jetpack Preferences DataStore (`datastore-preferences:1.1.3`) backing.
  - `D-02`: Native `stringSetPreferencesKey` for `tracked_game_packages` and `user_custom_whitelist`.
  - `D-03`: Asynchronous coordinate persistence on `ACTION_UP` via `Dispatchers.IO`.
  - `D-04`: Default settings baseline (`suppression = true`, `autoHud = false`, `x = 20, y = 100`, empty whitelist).
  - `D-05` & `D-06`: Application-scoped `SettingsRepository` singleton exposing reactive `Flow<UserSettings>`.
  - `D-07`: `IOException` resilience with graceful `emptyPreferences()` fallback.
  - `D-08`: Asynchronous non-blocking disk operations on `Dispatchers.IO`.
  - `D-09`: Strict physical display boundary clamping on save and restore.
  - `D-10`: Reactive synchronization with `ForegroundGameDetector` for immediate listener updates.

---

## 2. Design System & Tooling State

### Design System Configuration
- **Tool:** `none` (Native Android Jetpack Compose Material 3)
- **Component Framework:** `androidx.compose.material3:material3@1.3.1`
- **Compose UI Foundation:** `androidx.compose.ui:ui` (BOM `2024.11.00`)
- **Theme Definition:** `com.xeno.gfx.ui.theme.Theme.kt` (`XenoGfxTheme`)
- **Persistence Foundation:** `androidx.datastore:datastore-preferences:1.1.3`
- **Preset Automation / shadcn Gate:** Not applicable (`Tool: none` — Android Kotlin Jetpack Compose runtime).
- **Third-Party Registries:** None. All UI components are native Jetpack Compose primitives.

### Component Inventory Provenance
Could not enumerate: Native Android Jetpack Compose project without shadcn CLI (Tool: none).

---

## 3. Component Inventory (Jetpack Compose Material 3)

The following non-exhaustive catalog defines the foundational Compose primitives used across the Phase 02 settings and configuration surfaces:

| Component | Jetpack Compose Import | Phase 02 Settings Usage | Custom Styling / Modifiers Applied |
|---|---|---|---|
| `Scaffold` | `androidx.compose.material3.Scaffold` | Top-level screen container for `DashboardScreen` | `containerColor = DarkBackground`, `fillMaxSize()` |
| `Card` | `androidx.compose.material3.Card` | BGMI Turbo container, Telemetry preview, and Permissions container | `colors = CardDefaults.cardColors(containerColor = DarkSurface)`, `shape = RoundedCornerShape(16.dp)`, `border = 1.dp NeonGreen / HudBorderColor` |
| `Switch` | `androidx.compose.material3.Switch` | Continuous suppression toggle & Auto-HUD on game launch toggle | `checkedTrackColor = NeonGreen / CyberCyan`, `uncheckedTrackColor = DarkSurfaceElevated`, `checkedThumbColor = DarkBackground` |
| `Checkbox` | `androidx.compose.material3.Checkbox` | Selection checkbox for tracked games list items | `checkedColor = CyberCyan`, `checkmarkColor = DarkBackground`, `uncheckedColor = TextMuted` |
| `Button` | `androidx.compose.material3.Button` | Master HUD control button (`LAUNCH / STOP FLOATING HUD`) and pre-launch turbo button | `shape = RoundedCornerShape(12.dp)`, `height = 52.dp / 48.dp`, `colors = ButtonDefaults.buttonColors(...)` |
| `OutlinedButton` | `androidx.compose.material3.OutlinedButton` | Permission grant actions and secondary configuration actions | `border = BorderStroke(1.dp, CyberCyan)`, `colors = ButtonDefaults.outlinedButtonColors(...)` |
| `LazyColumn` | `androidx.compose.foundation.lazy.LazyColumn` | Scrollable list container holding cards, sections, and installed game rows | `modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)`, `verticalArrangement = Arrangement.spacedBy(16.dp)` |
| `Box` | `androidx.compose.foundation.layout.Box` | Floating HUD overlay container, badge pills, and icon wrappers | `clip(RoundedCornerShape(16.dp))`, `background(HudPillBackground / DarkSurfaceElevated)`, `border(1.dp, HudBorderColor)` |
| `Row` | `androidx.compose.foundation.layout.Row` | Horizontal arrangement for switch controls, game items, and header | `verticalAlignment = Alignment.CenterVertically`, `horizontalArrangement = Arrangement.SpaceBetween` |
| `Column` | `androidx.compose.foundation.layout.Column` | Vertical stack for settings descriptions, metric items, and cards | `verticalArrangement = Arrangement.spacedBy(12.dp / 8.dp)` |
| `Text` | `androidx.compose.material3.Text` | Titles, descriptions, package names, coordinates, and status feedback | `fontFamily = FontFamily.Monospace` for values & coordinates, `fontFamily = FontFamily.Default` for body/labels |
| `Icon` | `androidx.compose.material3.Icon` | Setting item icons, rocket, speed, security, and close vector assets | `size = 18.dp` to `26.dp`, `tint = CyberCyan / NeonGreen / AmberWarning / TextMuted` |

---

## 4. Spacing & Layout Scale (8-Point Grid)

All settings, cards, rows, and overlay positions strictly adhere to an 8-point spatial grid using multiples of 4dp. Arbitrary paddings and unscaled dimensional offsets are prohibited.

### Spacing Scale Tokens
| Token | Value (dp) | Allowed Settings & Overlay Applications |
|---|---|---|
| `space-xxs` | `4.dp` | Inner padding for badges, gap between dot and text, micro-spacing in status pills |
| `space-xs` | `8.dp` | Padding around game list items, icon-to-label gaps, internal switch-to-label spacing |
| `space-sm` | `12.dp` | Vertical spacing between items inside settings cards, header icon-to-title gap |
| `space-md` | `16.dp` | Standard padding inside `Card` containers, horizontal screen margins on `DashboardScreen` |
| `space-lg` | `24.dp` | Vertical rhythm separation between major dashboard settings sections |
| `space-xl` | `32.dp` | Bottom scroll clearance below the game list to ensure comfortable reaching |

### Interactive Touch Target & Dimension Exceptions
The following dimension exceptions are explicitly approved to guarantee tactile precision and accessibility:
- **Master HUD Button Height (`52.dp`):** High-confidence touch target for launching or stopping the floating monitor overlay.
- **Turbo Clean Button Height (`48.dp`):** Full-width pre-launch RAM purge button meeting Android standard 48dp minimum touch recommendation.
- **Settings Switch Row Touch Bounds (`48.dp` minimum height):** Both `In-Game Background Suppressor` and `Auto-HUD on Game Launch` rows declare a minimum container height of 48dp, allowing users to tap either the label or the switch to toggle state.
- **Game List Row Touch Bounds (`48.dp` minimum height):** Each `GameRow` provides a full-width clickable area with `48.dp` minimum vertical height, ensuring effortless game selection without missed taps.
- **Floating HUD Icon Touch Bounds (`44.dp` × `44.dp` minimum):** Close (`Close`) and expand/collapse (`KeyboardArrowUp`/`Down`) icons declare a minimum clickable hit-box of 44dp × 44dp via `Modifier.size(44.dp).clickable { ... }`.
- **Default Floating HUD Initial Coordinates:** `x = 20.dp`, `y = 100.dp` from display top-left corner per Decision `D-04`.

---

## 5. Typography Hierarchy

Typography utilizes Jetpack Compose Material 3 tokens. To enforce clear visual hierarchy, exactly 4 font sizes and 2 font weights are declared. Tabular monospace digits are mandatory for all persisted coordinates and numerical feedback.

### Typography Scale
| Style Token | Size (sp) | Weight | Line Height | Ratio | Font Family | Role / Persistence Application |
|---|---|---|---|---|---|---|
| `titleLarge` | `22.sp` | Bold (700) | `28.sp` | ~1.27 | Default (Roboto) | App branding header ("XENO GFX") |
| `titleMedium` | `16.sp` | Bold (700) | `24.sp` | 1.50 | Default (Roboto) | Settings section titles ("BGMI 4GB RAM TURBO", "Auto-HUD on Game Launch") |
| `bodyMedium` | `14.sp` | Normal (400) | `20.sp` | ~1.43 | Default (Roboto) | Setting control titles, game app names, permission row titles |
| `bodySmall` | `11.sp` | Normal (400) | `16.sp` | ~1.45 | Default (Roboto) | Explanatory helper copy, package identifiers (`com.pubg.imobile`) |
| `labelSmall` | `11.sp` | Bold (700) | `16.sp` | ~1.45 | Monospace | Category overlines ("SELECT GAMES TO TRACK"), live coordinates, badge counters |

### Weights Contract
- Exactly **2 weights** declared:
  1. `FontWeight.Normal` (`400`): Explanatory captions, game package names, subtitles, helper text.
  2. `FontWeight.Bold` (`700`): Section headers, button text, category overlines, and live numerical indicators.

### Monospace Tabular Digits Contract
- All numerical metrics, RAM counters, package counts, and coordinate pairs (`X: 20, Y: 100`) MUST explicitly set `fontFamily = FontFamily.Monospace`.
- Guarantees fixed-width character glyphs, eliminating horizontal text wobble and layout reflow during live updates.

---

## 6. Color Contract (60-30-10 Split & Semantic Mapping)

The color contract enforces a dark cyberpunk palette designed for minimal eye strain and maximum legibility during nighttime gaming.
