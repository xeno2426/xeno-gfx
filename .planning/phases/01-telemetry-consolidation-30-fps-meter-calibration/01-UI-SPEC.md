---
phase: "01"
title: "UI Design Contract: Telemetry Consolidation & 30 FPS Meter Calibration"
status: "draft"
created: "2026-11-06"
tool: "none"
framework: "Jetpack Compose 1.3.1 (Material 3)"
---

# UI Design Contract: Phase 01 Telemetry Consolidation & 30 FPS Meter Calibration

## 1. Executive Overview

### Phase Boundary & Visual Goals
Phase 01 consolidates hardware telemetry monitoring and calibrates frame rate calculations to an honest, stutter-free 30 FPS target ceiling (anchored against a 33.33ms VSYNC cadence baseline). The visual contract governs how telemetry metrics (calibrated FPS, panel refresh rate, RAM usage, battery thermals, and network latency) are presented on both the main dashboard (`DashboardScreen`) and the floating overlay (`HudOverlayView`).

### Core Design Objectives
1. **Accurate 30 FPS Visual Calibration:** Distinguish calibrated gaming frame rate (0–30 FPS) from physical screen compositor refresh rate (60Hz / 90Hz / 120Hz) with clear labeling and calibrated color thresholds.
2. **Smooth, Non-Flickering Numerical Readouts:** Enforce monospace tabular digits and Exponential Moving Average (EMA, $\alpha = 0.2$) smoothing to eliminate distracting rapid visual jitter during fast-paced battle royale gaming.
3. **Decoupled Asynchronous Ping Indicators:** Present non-blocking network socket latency probed every 5000ms with intuitive color coding (Green <60ms, Yellow 60–120ms, Red >120ms) and clean fallback (`-- ms`) on offline or timeout states.
4. **Zero-Distraction Cyberpunk Aesthetic:** Maintain high-contrast dark theme surfaces with focused accent illumination (`CyberCyan`) and instant semantic status cues (`NeonGreen`, `AmberWarning`, `NeonRed`).

### Traceability
- **Requirements Covered:** `FPS-01` (Calibrated 30 FPS meter), `FPS-04` (Asynchronous socket ping probe)
- **User Decisions Enforced:** `D-01` (33.33ms VSYNC anchoring & 0–30 FPS clamping), `D-02` (EMA $\alpha = 0.2$ smoothing), `D-03` (Distinct FPS vs Hz labeling), `D-04` / `D-05` (Demand-driven telemetry repository), `D-06` (`DeviceMetrics` compatibility), `D-07` / `D-08` (5s socket ping to `8.8.8.8:53` with color mapping).

---

## 2. Design System & Tooling State

### Design System Configuration
- **Tool:** `none` (Native Android Jetpack Compose Material 3)
- **Component Framework:** `androidx.compose.material3:material3@1.3.1`
- **Compose UI Foundation:** `androidx.compose.ui:ui` (BOM `2024.11.00`)
- **Theme Definition:** `com.xeno.gfx.ui.theme.Theme.kt` (`XenoGfxTheme`)
- **Preset Automation / shadcn Gate:** Not applicable (`Tool: none` — Android Kotlin Jetpack Compose runtime).
- **Third-Party Registries:** None. All UI components are native Jetpack Compose primitives.

### Component Inventory Provenance
Could not enumerate: Native Android Jetpack Compose project without shadcn CLI (Tool: none).

---

## 3. Component Inventory (Jetpack Compose Material 3)

The following non-exhaustive table catalogs the foundational Compose primitives used across the Phase 01 telemetry surfaces:

| Component | Jetpack Compose Import | Phase 01 Telemetry Usage | Custom Styling / Modifiers Applied |
|---|---|---|---|
| `Scaffold` | `androidx.compose.material3.Scaffold` | Top-level screen container for `DashboardScreen` | `containerColor = DarkBackground`, `fillMaxSize()` |
| `Card` | `androidx.compose.material3.Card` | Telemetry preview card & BGMI Turbo container | `colors = CardDefaults.cardColors(containerColor = DarkSurface)`, `shape = RoundedCornerShape(16.dp)`, `border = 1.dp HudBorderColor` |
| `Box` | `androidx.compose.foundation.layout.Box` | Floating HUD overlay container, active live dot, badge pills | `clip(RoundedCornerShape(16.dp))`, `background(HudPillBackground / HudCardBackground)`, `border(1.dp, HudBorderColor)` |
| `Row` | `androidx.compose.foundation.layout.Row` | Metric alignment in pill bar, telemetry cards, and metric rows | `verticalAlignment = Alignment.CenterVertically`, `horizontalArrangement = Arrangement.SpaceBetween` |
| `Column` | `androidx.compose.foundation.layout.Column` | Vertical stack for preview metrics and expanded diagnostic list | `horizontalAlignment = Alignment.CenterHorizontally`, `verticalArrangement = Arrangement.spacedBy(6.dp)` |
| `Text` | `androidx.compose.material3.Text` | Telemetry numerical values, unit labels, titles | `fontFamily = FontFamily.Monospace` for numbers, `fontFamily = FontFamily.Default` for labels |
| `Icon` | `androidx.compose.material3.Icon` | Status icons, expand/collapse arrows, close dismiss button | `size = 16.dp` to `26.dp`, `tint = CyberCyan / TextMuted / AmberWarning` |
| `Button` | `androidx.compose.material3.Button` | Master HUD control button (`LAUNCH / STOP FLOATING HUD`) | `colors = ButtonDefaults.buttonColors(...)`, `shape = RoundedCornerShape(12.dp)`, `height = 52.dp` |
| `AnimatedVisibility` | `androidx.compose.animation.AnimatedVisibility` | Accordion expand/collapse transition for HUD diagnostic card | `enter = fadeIn()`, `exit = fadeOut()` |

---

## 4. Spacing & Layout Scale (8-Point Grid)

All layouts strictly adhere to an 8-point spatial grid using multiples of 4dp. Arbitrary paddings and unscaled values are prohibited.

### Spacing Scale Tokens
| Token | Value (dp) | Allowed Applications |
|---|---|---|
| `space-xxs` | `4.dp` | Inner padding for compact badges, spacing between icon and close target, micro-separators |
| `space-xs` | `8.dp` | Gap between icon and text, compact padding around floating HUD pill, element inner margin |
| `space-sm` | `12.dp` | Vertical arrangement between telemetry card rows, header icon-to-title gap |
| `space-md` | `16.dp` | Standard container padding inside cards, horizontal screen margins (`DashboardScreen`) |
| `space-lg` | `24.dp` | Vertical rhythm separation between major dashboard sections |
| `space-xl` | `32.dp` | Bottom screen scroll clearance and primary action group separation |

### Interactive Touch Target & Dimension Exceptions
The following dimension exceptions are explicitly approved to ensure touch accessibility and gaming ergonomics:
- **Button Height (`52.dp`):** Master HUD control button (`LAUNCH FLOATING HUD`) uses `52.dp` height for high-confidence tapping.
- **Turbo Button Height (`48.dp`):** Pre-launch clean action button uses `48.dp` height conforming to Android accessibility guidelines.
- **HUD Icon Tap Bounds (`44.dp` × `44.dp` minimum):** Floating HUD expand (`KeyboardArrowUp`/`Down`) and dismiss (`Close`) icons render visual vectors at 16–18dp but MUST declare a minimum clickable touch box of 44dp × 44dp via `Modifier.padding(8.dp).clickable { ... }` to avoid missed taps while gaming.
- **Live Status Indicator Dot (`8.dp` × `8.dp`):** Non-interactive circular dot inside HUD pill indicating active Choreographer VSYNC sampling.

---

## 5. Typography Hierarchy

Typography utilizes Android Jetpack Compose Material 3 tokens. To maintain a strict visual hierarchy, exactly 4 font sizes and 2 font weights are declared. All dynamic numeric metrics MUST use monospace font formatting to prevent digit jitter during 1000ms updates.

### Typography Scale
| Style Token | Size (sp) | Weight | Line Height | Ratio | Font Family | Role / Purpose |
|---|---|---|---|---|---|---|
| `titleLarge` | `22.sp` | Bold (700) | `28.sp` | ~1.27 | Default (Roboto) | Screen branding header ("XENO GFX") |
| `titleMedium` | `16.sp` | Bold (700) | `24.sp` | 1.50 | Default (Roboto) | Card section headers ("DEVICE TELEMETRY", "Required Permissions") |
| `bodyMedium` | `14.sp` | Normal (400) | `20.sp` | ~1.43 | Default (Roboto) | Descriptive subtitles, permission explanations, helper text |
| `labelSmall` | `11.sp` | Bold (700) | `16.sp` | ~1.45 | Monospace | Live numeric metrics (FPS, RAM %, Temp, Latency, Hz) |

### Weights Contract
- Exactly **2 weights** declared:
  1. `FontWeight.Normal` (`400`): Body text, labels, descriptions, and static captions.
  2. `FontWeight.Bold` (`700`): Headers, CTA button titles, and live numeric values.

### Numerical Jitter Prevention Contract
- All live numbers (FPS counter, RAM percentage, battery temperature, ping latency, and refresh rate) MUST explicitly set `fontFamily = FontFamily.Monospace`.
- Tabular monospace numbers guarantee consistent character bounding boxes, preventing horizontal layout reflows when values transition (e.g. from `29 FPS` to `30 FPS` or `59ms` to `102ms`).

---

## 6. Color Contract (60-30-10 Split & Semantic Mapping)

The color system enforces a dark cyberpunk palette optimized for AMOLED battery conservation and minimal glare during nighttime gameplay.

```
+--------------------------------------------------------------+
| 60% Dominant Base: DarkBackground (#0D1117)                 |
|     Surface Containers: DarkSurface (#161B22)                |
|     Floating Overlays: HudPillBackground (#CC0D1117)         |
+--------------------------------------------------------------+
| 30% Secondary Structure:                                     |
|     Text & Borders: DarkOnBackground (#E6EDF3),               |
|     TextMuted (#8B949E), HudBorderColor (#4000E5FF)          |
+--------------------------------------------------------------+
| 10% Accent (CyberCyan #00E5FF) - Strictly Reserved List       |
+--------------------------------------------------------------+
```

### 60% Dominant Surface Colors
- `DarkBackground` (`0xFF0D1117`): Scaffold root canvas and full-screen container background.
- `DarkSurface` (`0xFF161B22`): Elevated cards (`TelemetryPreviewCard`, `BgmiRamTurboCard`).
- `DarkSurfaceElevated` (`0xFF21262D`): Monospace badge backgrounds, secondary button surfaces.
- `HudPillBackground` (`0xCC0D1117`): Floating overlay compact pill background (80% opacity dark tint).
- `HudCardBackground` (`0xEB161B22`): Floating overlay expanded diagnostic card (92% opacity dark tint).

### 30% Secondary Structure & Typography Colors
- `DarkOnBackground` (`0xFFE6EDF3`): Primary high-contrast headings and value text.
- `DarkOnSurface` (`0xFFC9D1D9`): Secondary body descriptions and static labels.
- `TextMuted` (`0xFF8B949E`): Subtitles, units ("FPS", "ms", "Hz"), and offline fallback (`"-- ms"`).
- `HudBorderColor` (`0x4000E5FF`): Translucent CyberCyan border (25% opacity) applied to all cards and overlay containers.

### 10% Accent Color (`CyberCyan` — `0xFF00E5FF`)
The primary brand accent `CyberCyan` is **strictly reserved** for the following 5 interface elements:
1. **Calibrated 30 FPS Optimal Status:** Rendered on the FPS counter when delivering 28–30 FPS (target frame pacing locked).
2. **Overlay Active Status Dot:** Pulsing 8dp circular indicator on the floating HUD pill signaling active VSYNC callbacks.
3. **Screen Branding Header:** Top-level "XENO GFX" title and speedometer icon outline.
4. **Master HUD Launch Button:** Primary container background for `LAUNCH FLOATING HUD`.
5. **Telemetry Section Category Label:** Overline text `"DEVICE TELEMETRY"`.

### Semantic Color Thresholds (Calibrated 30 FPS & Hardware Status)
Semantic colors provide immediate at-a-glance health status. Calibrated 30 FPS thresholds replace legacy 60 FPS thresholds:

| Metric | Optimal (`CyberCyan` / `NeonGreen`) | Warning (`AmberWarning`) | Critical / Error (`NeonRed`) |
|---|---|---|---|
| **Calibrated FPS (0–30)** | **28–30 FPS** (`CyberCyan`)<br>24–27 FPS (`NeonGreen`) | **18–23 FPS** (`AmberWarning`)<br>(Noticeable VSYNC loss) | **<18 FPS** (`NeonRed`)<br>(Severe stutter / unplayable) |
| **Ping Latency (8.8.8.8)** | **<60 ms** (`NeonGreen`)<br>(Tournament grade) | **60–120 ms** (`AmberWarning`)<br>(Moderate latency) | **>120 ms** or Timeout (`NeonRed`)<br>(Desync risk / unreachable) |
| **RAM Usage** | **<80%** (`NeonGreen`) | **80–85%** (`AmberWarning`) | **>85%** (`NeonRed`) (LMK kill risk) |
| **Battery Temperature** | **<38.0°C** (`NeonGreen`) | **38.1°C – 42.0°C** (`AmberWarning`) | **>42.0°C** (`NeonRed`) (Thermal throttle) |
| **Thermal Status Enum** | `NORMAL` (`NeonGreen`) | `LIGHT` / `MODERATE` (`AmberWarning`) | `SEVERE` / `CRITICAL` (`NeonRed`) |

*Hex Values:* `NeonGreen = 0xFF00E676`, `AmberWarning = 0xFFFFD600`, `NeonRed = 0xFFFF1744`.

---

## 7. Copywriting Contract

All copy must be crisp, technical, actionable, and unambiguous. Ambiguous phrases like "Checking...", "Okay", or "Good" are prohibited.

### Primary Action Call-to-Actions (CTAs)
All primary interactive buttons follow strict **[Verb] + [Noun]** formatting:
- `"LAUNCH FLOATING HUD"`: Starts `HudOverlayService` and binds overlay to `WindowManager`.
- `"STOP FLOATING HUD"`: Stops `HudOverlayService` and removes overlay window.
- `"PURGE RAM & LAUNCH BGMI"`: Executes pre-launch RAM purge and launches target battle royale app.
- `"PURGE BACKGROUND APPS NOW"`: Executes manual RAM optimization when no target game is installed.
- `"GRANT PERMISSION"`: Deep-links to Android system settings for required permission.

### Telemetry Display Labels (Per Decision D-03)
To eliminate confusion between the game's actual frame rate and the screen's hardware refresh rate, labels must explicitly differentiate them:

#### Dashboard Preview Card (`DashboardScreen.kt`)
- Live FPS Metric:
  - Top Label: `"30 FPS TARGET"`
  - Display Value: `"${calibratedFps} FPS"`
- RAM Metric:
  - Top Label: `"RAM USAGE"`
  - Display Value: `"${ramPercent}%"`
- Battery Temperature Metric:
  - Top Label: `"BATTERY TEMP"`
  - Display Value: `"${batteryTempCelsius}°C"` (or `"--"` if uninitialized)
- Network Latency Metric:
  - Top Label: `"PING (DNS)"`
  - Display Value: `"${latencyMs}ms"` (or `"-- ms"` if null)

#### Floating Overlay Pill Bar (`HudOverlayView.kt` — Compact Mode)
Format: `[Status Dot]  [30 FPS]  [RAM%]  [Temp°C]  [Ping ms]  [Expand Arrow]  [Close]`
- Calibrated FPS: `"${calibratedFps} FPS"` (e.g. `"30 FPS"`)
- RAM Usage: `"${ramPercent}%"` (e.g. `"68%"`)
- Battery Temp: `"%.1f°C".format(batteryTempCelsius)` (e.g. `"36.4°C"`)
- Ping Latency: `"${latencyMs}ms"` (e.g. `"42ms"`) or `"-- ms"`

#### Floating Overlay Diagnostic Card (`HudOverlayView.kt` — Expanded Mode)
- Row 1: Label: `"Live Gaming FPS"` | Value: `"${calibratedFps} FPS (30 FPS Target)"`
- Row 2: Label: `"Panel Refresh Rate"` | Value: `"${rawRefreshRate} Hz (Compositor)"`
- Row 3: Label: `"Ping (Google DNS)"` | Value: `"${latencyMs} ms"` (or `"Offline / Timeout"`)
- Row 4: Label: `"RAM Usage"` | Value: `"${ramUsedMb}MB / ${ramTotalMb}MB (${ramPercent}%)"`
- Row 5: Label: `"Battery"` | Value: `"${batteryLevel}%" + (if (isCharging) " (⚡ Charging)" else "")`
- Row 6: Label: `"Battery Temp"` | Value: `"%.1f °C".format(batteryTempCelsius)`
- Row 7: Label: `"Thermal Status"` | Value: `"${thermalStatus.name}"`

### Empty, Loading & In-Progress States
- **Network Latency Initializing / Offline:** `"-- ms"` (Compact pill & dashboard); `"Offline / Timeout"` (Expanded card).
- **Battery Sensor Initializing:** `"--"`
- **RAM Optimization In-Progress:** `"PURGING BACKGROUND APPS..."` (Button text with disabled click state).

### Error States & Next-Step Guidance
- **Network Ping Timeout (>500ms):** Value renders `"-- ms"` in `NeonRed` (or `TextMuted`). User guidance in expanded card: `"Offline / Timeout (Check Wi-Fi or Mobile Data)"`.
- **Missing Overlay Permission:** Problem + Next Step: `"Display Over Other Apps: Required to render the floating HUD overlay while gaming. Tap Grant."`
- **Missing Usage Stats Permission:** Problem + Next Step: `"Usage Access: Required to detect when games are launched and automatically display the HUD. Tap Grant."`

### Destructive Actions & Confirmation Policy
- **Stopping Floating HUD:** Tapping `STOP FLOATING HUD` or the HUD close `Icon(Icons.Default.Close)` terminates the service **immediately without a confirmation dialog**. In-game utilities must never interrupt gameplay with blocking modal alerts.
- **Manual RAM Purge:** Tapping `PURGE BACKGROUND APPS NOW` triggers immediate eviction, displaying an inline recovery badge upon completion: `"⚡ Purged {terminatedAppsCount} background apps • Recovered +{freedMb}MB RAM"`.

---

## 8. State & Interaction Contracts

The telemetry interface transitions across distinct operational states managed by `TelemetryRepository` and Jetpack Compose state streams:

```
[Stopped / Idle] 
       │ (Observer connects via collectAsState)
       ▼
[Active Sampling (Demand-Driven)] ──► [VSYNC Pulse Loss] ──► [Calibrated Drop (18-23 FPS)]
       │ (Every 5000ms IO flow)                                     │
       ▼                                                           ▼
[Socket Ping 8.8.8.8:53]                                   [Color: AmberWarning]
       ├─► Success (<60ms)  ──► Green
       ├─► Moderate (60-120ms) ──► Yellow
       └─► Timeout (>500ms) ──► "-- ms" (Red/Muted)
       │
       ▼ (All observers disconnect)
[Unsubscribed Teardown (0% CPU)]
```

### State Matrix
| State | Trigger / Condition | Visual Representation | Color / Token | Next Action / Recovery |
|---|---|---|---|---|
| **Idle / Unsubscribed** | App closed, HUD overlay closed | Zero background polling, 0% CPU consumption | N/A | Subscribes automatically when Dashboard or HUD opens |
| **Active Telemetry (Optimal)** | Frame pacing interval $\le 33.33\text{ms}$, Latency $<60\text{ms}$ | Live status dot solid, `30 FPS` displayed | `CyberCyan` for FPS, `NeonGreen` for Ping | Continuous 1s metrics update, 5s ping update |
| **Frame Cadence Drop** | VSYNC pulse delay $\Delta T > 1.5 \times T_{\text{vsync}}$ (Delivery loss) | EMA-smoothed FPS drops to `20–25 FPS` | `AmberWarning` (Warning) | Optimizer engine assesses memory pressure |
| **Severe Frame Stutter** | Sustained delivery loss, calculated FPS $<18$ | FPS counter drops to `<18 FPS` | `NeonRed` (Critical) | User prompted to purge RAM or enable Turbo |
| **Ping Latency Spike** | Socket round-trip takes $60\text{ms}–120\text{ms}$ | Latency text updates to `85ms` | `AmberWarning` | Auto-reprobes on next 5s tick |
| **Ping Socket Timeout** | Socket fails to connect within $500\text{ms}$ or network offline | Displays `"-- ms"` (pill) or `"Offline / Timeout"` | `TextMuted` (pill), `NeonRed` (expanded) | Auto-retries silently every 5000ms on `Dispatchers.IO` |
| **HUD Accordion Expand** | User taps chevron arrow on floating HUD pill | Smooth `fadeIn()` accordion expansion revealing full diagnostics card | `HudCardBackground` with `HudBorderColor` border | Tapping chevron collapses back to compact pill |
| **HUD Dismissal** | User taps 'X' icon or `STOP FLOATING HUD` | Immediate fade out and window removal from `WindowManager` | Clean dismiss | Re-activatable from Dashboard button |

---

## 9. Verification & Acceptance Criteria

Downstream verification agents (`gsd-ui-checker`, `gsd-planner`, `gsd-executor`) must validate this UI contract against the following checklist:

1. **[Dimension 1: Spacing]** All spacers, margins, and paddings in `DashboardScreen.kt` and `HudOverlayView.kt` use 4dp/8dp multiples (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`, `32.dp`). Touch targets for HUD icons are at least 44dp × 44dp.
2. **[Dimension 2: Typography]** Exactly 4 font sizes (`22sp`, `16sp`, `14sp`, `11sp`) and 2 font weights (`Normal 400`, `Bold 700`) are used. All live numerical telemetry values use `FontFamily.Monospace`.
3. **[Dimension 3: Color Contract]** 60-30-10 distribution is strictly enforced. `CyberCyan` is restricted to the 5 approved accent elements. Calibrated 30 FPS thresholds are color-coded (Cyan >=28, Green >=24, Yellow >=18, Red <18).
4. **[Dimension 4: Copywriting]** Primary CTAs use [Verb] + [Noun] format (`LAUNCH FLOATING HUD`, `PURGE RAM & LAUNCH BGMI`). Offline ping displays `"-- ms"` without crashing. Live gaming FPS is labeled distinctly from panel refresh rate.
5. **[Dimension 5: Component Inventory]** Tool is declared as `none` with Jetpack Compose Material 3 baseline cataloged.
6. **[Dimension 6: Interaction & States]** Latency socket timeout (500ms) is completely non-blocking and never hitches hardware telemetry updates.
7. **[Dimension 7: Traceability]** Requirements `FPS-01` and `FPS-04` are fully satisfied by the visual specifications and state transitions.
