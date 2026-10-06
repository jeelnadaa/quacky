# AGENT PROMPT: Build "Quacky" — an ad-free, offline Android utility app

You are a senior Android engineer. Build a complete, production-quality Android app described below. Read the whole prompt first, then plan, then build in the order given in section 13. Ask me nothing unless something is truly blocking; make sensible decisions and note them in a `DECISIONS.md` file.

---

## 1. Product summary

A single Android app bundling everyday utility tools that are normally scattered across ad-filled apps. Core promises:

- **No ads, no account, no analytics, no tracking, no network calls.** Fully offline. Do not add the INTERNET permission.
- **Minimalist, dark, monochromatic UI.**
- **History everywhere it makes sense**, stored locally.
- **Tools are organized into categories, searchable, and pinnable.**
- Permissions are requested **only at the moment a tool needs them**, with a short in-app explanation first.
- **Honest by design, no false hope.** If a phone lacks the hardware or system support a tool genuinely needs (camera, flash, ARCore, etc.), that tool does **not** run on that phone. No fake, degraded, or "sort of works" modes. The app says so plainly and explains why. See section 5A (this is a hard rule).

App name: **Quacky** (always styled "Quacky" with a capital Q; keep it in a single string resource `app_name`). Mascot: a minimalist duck. Package name suggestion: `app.quacky`.

### Developer placeholders (fill these in before building, or leave as-is and I will replace them)

Use these as constants in one file (`DeveloperInfo.kt`) and in `strings.xml`, never hardcode them elsewhere:

- `DEVELOPER_NAME` = "{{YOUR NAME}}"
- `DEVELOPER_EMAIL` = "{{YOUR EMAIL}}"

---

## 2. Tech stack (use these unless there is a strong reason not to)

- Language: **Kotlin**, UI: **Jetpack Compose** with Material 3 (heavily customized to the design system below)
- Min SDK 26, target/compile SDK = latest stable
- Architecture: **MVVM + unidirectional data flow**, one ViewModel per tool, repository layer for history
- DI: **Hilt**
- Navigation: **Navigation Compose** (type-safe routes)
- Persistence: **Room** (history, favorites, saved items), **DataStore** (settings, pinned tools, tool order)
- Camera: **CameraX**
- Barcode scanning: **ML Kit Barcode Scanning, bundled model** (`com.google.mlkit:barcode-scanning`, NOT the Play-services-downloaded variant) so it works offline from first launch
- QR generation: **ZXing core** (`com.google.zxing:core`)
- AR: **ARCore** (Sceneform is deprecated; render with Compose + OpenGL/Filament or a lightweight custom overlay). Mark ARCore as `optional` in the manifest and show an honest missing-requirement message on unsupported devices (no fallback modes; see 5A)
- EXIF: **androidx.exifinterface**
- PDF: **android.graphics.pdf.PdfRenderer** and **PdfDocument**
- Image decoding: **Coil** (local files only)
- Testing: JUnit + Turbine for ViewModels, Compose UI tests for key flows

No Firebase, no Google Analytics, no ad SDKs, no crash-reporting SDKs that phone home.

---

## 3. Design system

### 3.1 Principles
Minimal. Quiet. Lots of whitespace. No gradients, no shadows, no decorative illustrations. Hierarchy comes from type size/weight and thin dividers, not color.

### 3.2 Color (monochromatic dark, grayscale only)
Define as theme tokens; never hardcode hex in screens.

| Token | Hex | Use |
|---|---|---|
| `background` | `#0A0A0A` | App background |
| `surface` | `#121212` | Cards, sheets |
| `surfaceElevated` | `#1A1A1A` | Dialogs, pressed state |
| `outline` | `#2A2A2A` | 1dp borders, dividers |
| `textPrimary` | `#F2F2F2` | Primary text |
| `textSecondary` | `#9A9A9A` | Secondary text |
| `textTertiary` | `#5E5E5E` | Hints, disabled |
| `accent` | `#FFFFFF` | Primary buttons (white fill, black text), selected states |

- The **only** non-gray colors allowed in the entire app are those that are *content*: the color picker's picked colors, camera previews, user images, and a muted destructive indicator (`#B3261E`-ish) used sparingly for delete confirmations.
- Force dark theme always (no light theme in v1), but build the theme so a light theme can be added later.
- Status bar and navigation bar: transparent, edge-to-edge, light icons.

### 3.3 Typography: Satoshi
- **Satoshi is the default and only app typeface.** Fonts are from Fontshare (free for commercial use). Place the files in `app/src/main/res/font/`: `satoshi_light.otf`, `satoshi_regular.otf`, `satoshi_medium.otf`, `satoshi_bold.otf`, `satoshi_black.otf` (or `.ttf` equivalents).
- If the font files are not present in the repo, **do not fail the build**: create the `FontFamily` definition, add a `TODO` and a `res/font/README.md` telling me exactly which files to download and where to put them, and temporarily fall back to `FontFamily.SansSerif`.
- Build a complete Material 3 `Typography` using Satoshi. Suggested scale: Display 34/Bold, Title 22/Bold, Section 13/Medium (uppercase, +1sp letter-spacing, `textSecondary`), Body 15/Regular, Caption 12/Regular. Tabular numerals for any number-heavy UI (rulers, counters, results) if the font supports it.

### 3.4 Components and shape
- Corner radius: 14dp cards, 12dp buttons/inputs, full-pill for chips.
- 1dp `outline` borders instead of elevation.
- Icons: Material Symbols **Rounded, weight 300**, monochrome, 24dp. No colored icons.
- Spacing grid: 4dp base; screen horizontal padding 20dp.
- Buttons: Primary = white fill/black text; Secondary = transparent with 1dp outline; Text button = `textSecondary`.
- Motion: subtle only (150–250ms fades and shared-axis transitions). Haptic feedback on key actions (copy, spin, flip, pin).
- Empty states: one line of text + one small icon, no illustrations.
- Support font scaling up to 130% and TalkBack labels on every control.

---

## 4. App structure and navigation

### 4.1 Screens
1. **Home**
2. **Search** (inline on Home, expands to full-screen overlay)
3. **Category screen** (list of tools in that category)
4. **Tool screens** (one per tool, section 7+)
5. **Global History** (all tools, filterable)
6. **Settings**

Bottom navigation with 3 items: **Home**, **History**, **Settings**. Keep it minimal, icon + label, no indicator pill color (selected = white, unselected = `textTertiary`).

### 4.2 Home screen layout (top to bottom)
1. Large greeting-free header: app name left, nothing else.
2. **Search bar** (pill, `surface`, placeholder "Search tools"). Tapping it focuses and shows results live.
3. **Pinned** section: horizontally scrollable row (or 2-column grid if >4) of pinned tool tiles. If nothing is pinned, show a one-line hint: "Long-press any tool to pin it."
4. **Recent** section: last 4 used tools (auto, from usage log). Hide section if empty.
5. **Categories** section: vertical list of category rows (icon, name, tool count, chevron). Tapping opens the Category screen.
6. An optional toggle in Settings: "Show all tools as a flat grid on Home" for users who prefer no categories.

### 4.3 Categories (fixed taxonomy; tools must live in exactly one)
| Category | Tools |
|---|---|
| **Scan & Generate** | QR & Barcode Scanner, QR & Barcode Generator |
| **Measure** | AR Ruler, On-Screen Ruler, Area & Volume Calculator |
| **Color & Image** | Color Picker, Photo Metadata Viewer & Remover, Image & PDF Compressor |
| **Text & Date** | Text Counter, Date Calculator |
| **Random** | Dice, Coin Flip, Random Number, Picker Wheel, Team Splitter |

(Scanner and Generator are shown as two separate tools in navigation but share one history table and one screen host with two tabs, so users can reach either quickly.)

Define tools in a single **`ToolRegistry`**: a list of `ToolDefinition(id, nameRes, descriptionRes, category, iconRes, keywords: List<String>, route, requirements: Set<Requirement>)`, where `Requirement` is an enum (e.g., `BACK_CAMERA`, `ARCORE`, `VALID_DISPLAY_METRICS`) resolved by the capability checker in section 5A. Home, search, categories, pins, and recents all read from this registry. Adding a tool must require editing only the registry plus the tool's own package.

### 4.4 Search
- Live, as-you-type, case-insensitive, matches against tool name, description, and `keywords` (e.g., "ruler" should find AR Ruler and On-Screen Ruler; "exif", "privacy", "gps" should find Metadata Remover; "random", "pick", "lottery" should find Random tools; "hex", "eyedropper" finds Color Picker; "age", "days between" finds Date Calculator; "wifi", "upi" finds QR Generator).
- Light fuzzy matching: tolerate one typo for words longer than 4 characters, and rank prefix matches first.
- Results show tool icon, name, category label; each result supports long-press to pin.
- Empty-query state shows Recent searches (last 5, clearable) and Recent tools.
- No results state: "No tool found for '…'".

### 4.5 Pinning
- Long-press any tool tile (Home, Category, Search result) → bottom sheet: **Pin / Unpin**, **Open**, **How to use** (opens the tool's guide, 7C). Also a pin icon toggle inside each tool's top bar.
- Pinned tools are stored in DataStore as an ordered list.
- Pinned section supports **drag-to-reorder** (long-press and drag in an "Edit pinned" mode).
- Max pinned: unlimited, but show a gentle note past 8.
- Provide **Android app shortcuts**: long-press the launcher icon shows up to 4 shortcuts = the first 4 pinned tools (dynamic shortcuts, updated when pins change).
- Optional (stretch): a Glance home-screen widget showing up to 4 pinned tools as one-tap launchers.

### 4.6 History architecture (shared)
- Room database `quacky.db` with one generic table `history_entry`:
  `id, toolId, type, title, subtitle, payloadJson, thumbnailPath?, createdAt, isFavorite`
  plus per-tool tables only where structured querying is needed (e.g., saved palettes).
- Global History screen: list grouped by day, **filter chips by category / tool**, search within history, favorites filter, swipe-to-delete with Undo snackbar, multi-select delete, "Clear all" with confirmation.
- Tapping an entry opens the originating tool pre-filled with that entry.
- Each tool also shows its own **History** entry point (clock icon in top bar → bottom sheet or screen filtered to that tool).
- Settings: history on/off per tool, auto-delete after (never / 7 / 30 / 90 days), max entries per tool (default 500).
- Never store images inside the DB; store small thumbnails in app-private storage and reference by path. Delete thumbnails when entries are deleted.

### 4.7 Tool screen conventions
Every tool screen has the same top bar: back arrow, tool name, then actions: **pin toggle**, **history (clock)**, **help (?) for the how-to guide (7C)**, **overflow (about / reset)**. Result areas always provide **Copy**, **Share**, and **Save to history** (auto-save on by default where it makes sense, controlled by Settings).

---

## 5. Permissions policy

- Declared: `CAMERA` (optional feature), `VIBRATE`. For AR: `<uses-feature android:name="android.hardware.camera.ar" android:required="false"/>` and ARCore `<meta-data android:name="com.google.ar.core" android:value="optional"/>`.
- **No INTERNET, no location, no microphone, no broad storage permission.** Use the system **Photo Picker** (`PickVisualMedia`) and Storage Access Framework for files. Save outputs with MediaStore (scoped storage).
- Camera permission flow: tool opens → if not granted, show a minimal in-tool rationale screen ("Camera is used on-device only. Nothing is uploaded.") with a Grant button; if permanently denied, show a button to open app settings. This permission flow applies only to tools whose capability check passed (5A); a permission denial by the user is different from a phone lacking a camera. For a denied permission, tools whose core does not need the camera (for example Color Picker with Gallery) stay usable; camera-only tools simply wait for the user to grant it.

---

## 5A. MISSING-SENSOR POLICY (no false hope)

**Principle:** Quacky never gives false hope. **All tools appear normally everywhere** (Home, categories, search, pins, history): no dimming, no "not supported" badges, no hiding. The check happens **only when the user tries to use a tool**. If the phone doesn't have the sensor or system support that tool genuinely needs, the app shows a clear message saying so, and the tool does not run. No fallback modes, no workarounds, no "try anyway" button, no substitute tool suggestions.

### 5A.1 Capability checker
Create `core/capability/` with an injectable, fakeable `DeviceCapabilities` interface exposing at least:
- `hasBackCamera`, `hasFlashUnit`, `hasAccelerometer`, `hasVibrator`
- `arCoreStatus`: `SupportedAndReady`, `SupportedNeedsInstallOrUpdate`, `Unsupported`, `Checking` (use `ArCoreApk.checkAvailability`; treat `Checking` as "not ready yet" and re-check)
- `displayMetricsPlausible` (see 7.4)
- `canHandle(intent)`: whether any installed app can handle an intent (use `PackageManager` with a narrow `<queries>` block in the manifest for only the intents Quacky uses; **do not** request `QUERY_ALL_PACKAGES`)

Each tool in the `ToolRegistry` declares its `requirements`. A `RequirementChecker` returns `Ready` or `Missing(requirement)` for a tool.

### 5A.2 Which tools need what

| Tool | Requires | If missing |
|---|---|---|
| QR & Barcode Scanner | Back camera | Message screen (5A.3) |
| QR & Barcode Generator | nothing | always works |
| AR Ruler | Back camera + ARCore-supported device + Google Play Services for AR installed and current | Message screen. If ARCore is supported but not installed/outdated, the message includes one button that hands off to the system/Play Store flow (done by Android, not by Quacky); re-check when the user returns |
| On-Screen Ruler | Plausible display metrics, or a completed calibration | See 7.4 |
| Area & Volume Calculator | nothing | always works (manual input) |
| Color Picker | nothing for the Gallery source; back camera for the Camera source | Tapping **Camera** shows the message; Gallery keeps working |
| Photo Metadata Viewer & Remover | nothing | always works |
| Image & PDF Compressor | nothing | runtime limits only (5A.5) |
| Text Counter, Date Calculator | nothing | always works |
| Dice, Coin Flip, Random Number, Picker Wheel, Team Splitter | nothing | Shake-to-roll/flip appears only if an accelerometer exists; haptics are skipped silently if there is no vibrator |

### 5A.3 What the user sees
- The check runs **when the tool is opened**, before any permission request, camera start, or AR session. Never request a permission for a tool that can't run.
- If a requirement is missing, show a `MissingRequirementScreen` (a simple centered message in the normal dark minimal style, with the tool's top bar and a Back button):
  - A plain sentence naming the missing thing and the tool, for example:
    - "Your phone doesn't have a back camera, which the QR Scanner needs."
    - "Your phone doesn't support Google ARCore, which the AR Ruler needs to measure."
    - "Your phone doesn't have a flash, so the torch isn't available." (action-level, see 5A.4)
  - Nothing else: no alternatives, no "continue anyway", no apologetic filler. Exception: the AR "needs install/update" case with its single setup button.
- Keep all message strings in `strings.xml`, one per requirement type.
- Pinning, search, history and shortcuts behave normally for these tools.

### 5A.4 Buttons that can't work are not shown
Inside a tool, show an action only if the phone can really perform it: torch (needs flash), zoom (needs camera zoom support), Wi-Fi connect, Open in UPI app, Add to contacts, open map, compose email/SMS, dial, open calendar (need an app that can handle the intent), shake detection (needs accelerometer), "Use AR measurement" in Area & Volume (only if AR is available and measurements exist). If an intent unexpectedly fails, catch `ActivityNotFoundException` and show one honest line ("No app on this phone can open this."), never a crash.

### 5A.5 Runtime honesty
- Unreadable file type: "This phone can't open this file type." Never output an empty or corrupt file.
- Not enough memory for a heavy job (large PDFs/images, low-RAM phones): estimate first; if unsafe, say "This file is too large to process on this phone."
- Compression: never claim "saved X%" unless the new file is really smaller; otherwise say so and keep the original.
- Metadata removal: always verify the output and show the verified result; say upfront if a format can't be cleaned reliably.
- Never show success, progress, or results that weren't actually produced.

### 5A.6 Accuracy honesty
AR measurements are labeled estimates (7.3). The On-Screen Ruler shows Calibrated / Not calibrated (7.4). Color Picker notes that live camera colors depend on lighting. Don't use words like "precise" or "accurate" in UI copy for anything that depends on sensors or calibration.

### 5A.7 Manifest and docs
Declare camera, flash, accelerometer and AR features as `required="false"` so the app installs on as many phones as possible; the in-app check decides what runs. The README lists each tool's requirements honestly.

### 5A.8 Tests
Provide fake `DeviceCapabilities` profiles (no camera, no flash, no accelerometer, ARCore unsupported / needs install / ready, implausible display metrics). Unit-test `RequirementChecker` for every tool against every profile, and UI-test that opening a tool with a missing requirement shows the message and never triggers a permission request or camera/AR initialization.

---

## 6. Settings screen

Sections: **Appearance** (haptics on/off, font scale respects system), **History** (per-tool toggles, auto-delete, max entries, "Clear all history"), **Home** (flat grid vs categories, show Recents), **Help & Tips** (show tips on first open, reset all tips, browse all how-to guides, see 7C), **Data** (export all history as JSON via SAF, import JSON), **About** (duck logo, version number (hidden Easter egg trigger, see 7B), "No ads · No tracking · Offline" statement, open-source licenses list, and the "Developer's note" row once the Easter egg has been found).

---

## 7. TOOL SPECS

Each tool lives in its own feature package (`feature/<tool>/` with `ui`, `viewmodel`, `domain`, `data`). Write unit tests for all domain logic.

---

### 7.1 QR & Barcode Scanner

**Purpose:** Fast, ad-free scanning of QR codes and common barcodes.

**Requirements**
- Live camera scan with CameraX + ML Kit (bundled). Support all ML Kit formats (QR, Data Matrix, Aztec, PDF417, EAN-8/13, UPC-A/E, Code 39/93/128, ITF, Codabar).
- Minimal viewfinder: a thin white rounded-corner frame, rest of the preview dimmed. Controls: torch toggle (only if the phone has a flash unit), pinch/slider zoom (only if supported), **gallery import** (scan a code from a saved image), format filter (All / QR only / Barcodes only), continuous-scan mode (batch scan multiple codes into a list) vs single-scan mode.
- On detection: vibrate, freeze frame briefly, show a **result bottom sheet** with the parsed type and contextual actions. Parse and act on:
  - **URL** → Open, Copy, Share (show the full URL clearly before opening; flag `http://` as non-secure)
  - **Wi-Fi** (`WIFI:T:...;S:...;P:...;;`) → show SSID/security, **Copy password**, **Connect** (use `WifiNetworkSuggestion`/`ACTION_WIFI_ADD_NETWORKS` on supported API levels)
  - **UPI** (`upi://pay?...`) → show payee, VPA, amount, note; **Open in UPI app** via intent
  - **Contact** (vCard/MeCard) → **Add to contacts** via `ContactsContract` insert intent (no contacts permission needed)
  - **SMS / phone / email / geo / calendar event** → matching intent actions
  - **Product barcodes** → show number, format, Copy; "Search the web" opens the browser (user-initiated only)
  - **Plain text** → Copy, Share
- Always show the raw content in an expandable "Raw data" section.
- **Requirement:** a back camera (see 5A). On phones without one, opening the scanner shows the missing-requirement message.
- Show contextual action buttons **only if this phone can actually perform them** (5A.4). For example, if no installed app can handle a UPI link, show the parsed details with Copy only, not a dead "Open in UPI app" button.
- Auto-save every scan to history (toggle in Settings). History entry stores type, raw value, format, timestamp, favorite flag.
- **Tool history screen**: filter by type (URL, Wi-Fi, UPI, Contact, Product, Text), search, favorites, swipe delete, tap to reopen the result sheet, export selected as CSV/text.
- Safety: never auto-open links; always require a tap.

**Acceptance:** scans a printed QR in <1s in normal light, works in airplane mode, works from a gallery image, and all listed content types route to the right action.

---

### 7.2 QR & Barcode Generator

**Requirements**
- Input type selector (chips): **Text, URL, Wi-Fi, Contact (vCard), Email, SMS, Phone, UPI, Location, Calendar event**. Each has its own tiny form with validation (e.g., UPI: VPA, name, amount, note; Wi-Fi: SSID, password, security WPA/WEP/None, hidden toggle).
- Output formats: QR (default), plus Code 128, Code 39, EAN-13, EAN-8, UPC-A, Data Matrix, PDF417, Aztec. Validate input per format (e.g., EAN-13 digits/checksum; auto-calc the check digit) and show clear inline errors.
- **Live preview** that updates as you type (debounced).
- Customization (kept minimal): error-correction level (L/M/Q/H), size, margin (quiet zone), foreground/background colors (default black on white; warn if contrast is too low to scan), optional center logo for QR (auto-bumps EC to H).
- Export: **PNG, SVG, PDF**, Save to gallery (MediaStore), Share, Copy content.
- History: auto-save every generated code with a thumbnail and its settings; tapping reopens it fully editable; favorites; "Duplicate and edit".
- Templates: "Save as template" for repeating codes (e.g., home Wi-Fi), shown at the top of the generator.

**Acceptance:** generated codes scan correctly with the app's own scanner and with a second device's scanner; exports are crisp at 2000px.

---

### 7.3 AR Ruler

**Requirements**
- ARCore plane detection with a minimal reticle (small white ring that sticks to detected surfaces; turns dim when tracking is poor, with a one-line hint "Move phone slowly").
- Modes (segmented control): **Distance** (2 points), **Multi-point path** (polyline with running total), **Height** (measure from floor to a point; prompt user to first tap the floor), **Angle** (3 points).
- Tap to place points; draggable points to adjust; undo last point; clear all. Floating distance labels along each segment in white text on a small dark pill; always face the camera.
- Units: cm, mm, m, in, ft+in; remembered between sessions. Large readout panel at the bottom with current/total value and a **Copy** button.
- **Capture** button saves a screenshot of the AR view with the measurements overlaid to the gallery, and adds a history entry.
- Show a one-time accuracy note: "AR measurements are estimates, typically within 1–3% on flat, well-lit surfaces."
- **History:** each saved measurement stores: label (editable), value(s), unit, mode, screenshot thumbnail, timestamp. Users can rename, favorite, and **send a measurement to Area & Volume Calculator** as an input.
- **Requirements (5A):** back camera, an ARCore-supported device, and Google Play Services for AR installed and current. Check with `ArCoreApk.checkAvailability` as soon as the tool is opened.
  - **Unsupported device:** show the missing-requirement message with the plain reason ("This phone doesn't support Google ARCore, which AR measuring needs."). **Do not offer manual entry, a lookalike ruler, or any "try anyway" path.**
  - **Supported but needs install/update:** `NeedsSetup` with a single button that hands off to the system/Play Store flow; after returning, re-check and open the tool only when `SupportedAndReady`.
  - **Tracking problems at runtime** (poor light, featureless surfaces): keep the tool open, show the one-line hint, and do not display a measurement value until tracking is stable. Never show a number that was produced without a valid tracked plane.

**Acceptance:** two-point measurements of a known 30cm object are within ~±1cm in good conditions; app never crashes if ARCore is missing or outdated (prompt install/update gracefully, using the offline-safe Play Services AR flow).

---

### 7.4 On-Screen Physical Ruler

**Requirements**
- Full-screen ruler with **cm/mm** along one edge and **inches (1/16 subdivisions)** along the other, using `DisplayMetrics.xdpi/ydpi` for true physical scale.
- **Calibration:** many devices report wrong DPI. Provide a Calibrate flow with two options: (a) **Credit card method**: user lines a standard card (85.60 × 53.98 mm) against an on-screen outline and adjusts with a slider; (b) **Known-length method**: user enters the real-world length of a line shown on screen. Store a per-device calibration factor in DataStore. Show "Calibrated / Not calibrated" status.
- **Honesty about scale:** treat the display metrics as `Implausible` if `xdpi`/`ydpi` are outside 120–700 or differ from each other by more than 8%. If implausible, the ruler is **locked until the user completes calibration**. If plausible but never calibrated, the ruler works but shows a persistent "Not calibrated, may be off by a few mm" badge with a Calibrate button. After calibration, show "Calibrated".
- Orientation: portrait shows a vertical ruler; landscape shows a horizontal ruler; a toggle flips which edge is zero (left/right-handed use).
- Draggable **measure markers** (two movable lines) giving a live distance readout in the chosen unit between them. Large tabular numerals.
- Optional tools in the same screen: **protractor** mode is out of scope for v1; **grid/mm paper** mode (toggle) is in.
- Keep screen awake while open. No history needed here, except: "Save measurement" button that stores the marker distance and a label.

**Acceptance:** after calibration, a 10cm line on screen measures 10cm (±1mm) with a physical ruler.

---

### 7.5 Area & Volume Calculator

**Requirements**
- Two top-level tabs: **Area** and **Volume**. Inside each, shape chips.
  - **Area shapes:** rectangle, square, triangle (base-height, three sides via Heron, or SAS), circle, semicircle, trapezoid, parallelogram, ellipse, ring/annulus, regular polygon, **irregular polygon** (enter or tap coordinates/corners on a canvas, using the shoelace formula).
  - **Volume shapes:** cube, cuboid, cylinder, cone, sphere, hemisphere, pyramid (rect base), prism (triangular), capsule.
- Each shape: labeled diagram (simple monochrome line drawing, drawn with Compose Canvas), numeric fields with unit dropdowns per field, live results, and a **results card** with area/perimeter (or volume/surface area) in the chosen output unit, plus a "Show in all units" expander (mm², cm², m², in², ft², yd², acre, hectare / mL, L, m³, in³, ft³, gallon).
- **Import from AR Ruler:** button "Use measurement from AR Ruler" that pulls a saved length into the focused field. **Show this button only if AR Ruler is available on this phone and at least one saved measurement exists**; otherwise do not show it at all.
- **Estimators** (separate "Estimate" tab, each with clear editable assumptions):
  - **Paint:** wall dimensions, doors/windows to subtract, coats, coverage per litre (default 10 m²/L) → litres needed.
  - **Tiles/flooring:** room dimensions, tile size, gap, wastage % (default 10%) → count and boxes.
  - **Concrete:** slab L × W × thickness → m³, plus bags equivalent.
  - **Water tank:** shape + dimensions → litres.
- History: every calculation saved with shape, inputs, units, and results; reopen and edit; duplicate; favorites. Group history by Area / Volume / Estimates.
- All math in `BigDecimal` or carefully rounded doubles; display with sensible precision (user setting: 0–6 decimals). Validate negative/zero/impossible inputs (e.g., triangle inequality) with inline errors, never crashes.

---

### 7.6 Color Picker  (precise pin-pointing is a core requirement)

**Purpose:** pick exact colors from the live camera or any image with pixel-level precision, then copy, name, and save them.

**Sources (top segmented control):** **Camera** | **Gallery image**. If the phone has no back camera, tapping the Camera segment shows the missing-camera message, and Gallery remains fully functional. Camera-only features (live loupe on the preview, Freeze) are not shown without a camera.

**Pin-point precision (must-have, treat as the heart of this tool)**
- A **crosshair reticle** (thin white lines with a hollow center showing the exact sampled pixel) is placed on the image/preview. The user can **tap anywhere to move it** and **drag it** to fine-tune.
- While dragging, show a **magnifier loupe** offset above the finger (so the finger doesn't cover it): a circular 8× zoomed view with a pixel grid, the center pixel outlined, and the live HEX value beneath. Use nearest-neighbor scaling so individual pixels are visible.
- **Fine-adjust nudge controls:** four small arrow buttons (and volume-key support) to move the reticle by exactly **1 pixel** per tap, long-press to repeat.
- **Sampling size selector:** Point (1×1), 3×3 average, 5×5 average, 11×11 average. Default 3×3 for camera (reduces noise), Point for gallery images. Show the selected sample area as a small square on the reticle.
- **Freeze/Lock:** in camera mode a **Freeze** button captures the current frame so the user can pin-point calmly on a still image (and zoom/pan it with pinch and drag, with the reticle staying locked to image coordinates, not screen coordinates). Un-freeze returns to live.
- **Zoom and pan** on gallery images and frozen frames (up to 20×). The reticle coordinates are stored in image-pixel space.
- **Multi-pin mode:** user can drop up to **8 pins** (numbered markers) on one image, each with its own sampled color, shown in a strip along the bottom. Tap a pin in the strip to select/move it; long-press to delete. This is how palettes are built from specific spots.
- Show the pixel coordinates (x, y) of the active pin and the image size.
- Accuracy rules: sample from the **full-resolution bitmap/frame**, not the downscaled preview; correctly handle EXIF rotation, color space (convert Display P3/wide-gamut to sRGB), and camera YUV→RGB conversion. Disable auto-white-balance lock warnings: show a small note that live camera colors depend on lighting.

**Color readout panel (bottom sheet, collapsible)**
- Large swatch of the picked color (this is where color is allowed in the UI).
- Values with one-tap copy each: **HEX, RGB, HSL, HSV, CMYK**, and also **Android ARGB int**, **Compose `Color(0xFF…)`**, **CSS rgb()**.
- **Nearest color name** (offline lookup against a bundled list of ~1,500 named colors using CIELAB ΔE distance), shown with the match distance.
- **Shades and tints** strip (±5 steps) and **complementary / analogous / triadic / split-complementary** harmonies; tapping any swatch makes it the active color.
- **Contrast checker:** choose a second color (from pins, palette, or manual) and show WCAG ratio plus AA/AAA pass/fail for normal and large text.

**Palettes**
- "Extract palette" button: auto-generates 5–8 dominant colors from the image (median-cut or k-means in a background coroutine), displayed as chips; tapping one drops a pin where that color most appears if possible.
- Save current pins as a named **Palette**. Palettes screen: list, rename, delete, reorder colors, export as **PNG swatch image, JSON, CSS variables, Android `colors.xml`, Tailwind config snippet** via Share/Save.

**History**
- Every picked color (when the user taps **Save**, or auto-save toggle) goes to history with HEX, thumbnail of the source image crop around the pin, timestamp, and optional label.
- History view: grid of swatches, filter by favorites, search by HEX or name, tap to reopen in the picker, multi-select → "Make palette".

**Acceptance:** picking the same pixel in a gallery image at Point sampling returns the exact HEX that a desktop image editor shows (±0 per channel for sRGB images); the loupe tracks the finger with no visible lag; reticle nudges by exactly 1 image pixel.

---

### 7.7 Photo Metadata Viewer & Remover

**Requirements**
- Pick one or many images via Photo Picker (also accept "Share to app" intent and "Open with" for images).
- **Viewer:** grouped, readable sections: **Location** (lat/long, altitude; with a clear red "Contains GPS location" warning chip, and *no map tile loading*, show coordinates only, with an optional "Open in maps app" action), **Device** (make, model, software, lens), **Capture** (date/time, exposure, ISO, aperture, focal length, flash), **Image** (dimensions, orientation, color space, file size, format), **Other tags** (raw list, searchable).
- **Privacy score** chip per image: "Clean" / "Contains metadata" / "Contains location".
- **Remover:**
  - Choices: **Remove all metadata**, **Remove only location**, or **Custom** (checkbox per group).
  - For JPEG, remove EXIF/XMP/IPTC segments **without re-encoding pixels** where possible (lossless strip); preserve orientation by applying rotation to pixels only when the orientation tag would otherwise be lost, and tell the user. For PNG/WebP/HEIC, strip ancillary metadata chunks; if not feasible losslessly, re-encode at max quality and say so.
  - **Batch mode:** process many images; show progress; results list with before/after metadata counts.
  - Output: save **copies** to `Pictures/Quacky/Clean/` via MediaStore (never overwrite originals unless the user explicitly enables "Replace original", which uses the system write-request dialog). Share directly after cleaning.
- **History:** log of cleaned images (filename, what was removed, timestamp, thumbnail). No metadata *values* are stored in history (privacy), only the categories removed.
- Be upfront about format limits: if a file type cannot be cleaned reliably on this phone, say so before processing (5A.5).
- Verify after cleaning by re-reading the output file and showing "Verified: no GPS data found."

---

### 7.8 Image & PDF Compressor

**Requirements**
- Pick **JPG, PNG, WebP images** (single or batch) and **PDFs**.
- **Image compression:**
  - Modes: **Quality slider (1–100)**, **Target file size** ("under 200 KB": iteratively binary-search quality, then downscale dimensions if needed), and presets (Small / Balanced / High / Custom).
  - Options: resize by percent or max width/height, output format convert (JPG/PNG/WebP), strip metadata toggle (default on), keep original dimensions toggle.
  - PNG: lossy palette quantization option plus lossless re-encode; warn that PNG with transparency can't become JPG without a background color (let user pick white/black).
  - Live **before/after preview** with a draggable split slider and estimated output size; actual size shown after processing. Zoom to inspect.
- **PDF compression:**
  - Render each page with `PdfRenderer` at a chosen DPI (72 / 100 / 150 / 200), re-encode to JPEG at the chosen quality, and rebuild with `PdfDocument`. Show a clear note: "Text becomes image-based (not selectable) when compressed this way", and offer a lighter mode that only keeps the original if no size reduction is achieved.
  - Show page count, original size, estimated new size, progress per page, cancel button. Process off the main thread with a foreground-safe coroutine and avoid OOM (stream pages one at a time, recycle bitmaps).
- Results screen: original vs new size, percent saved, **Save** (MediaStore to `Pictures/Quacky/` or `Documents/Quacky/`) and **Share**. Never overwrite the original.
- **Honest limits (5A.5):** before starting, estimate memory needs; if the phone cannot process the file safely, stop with a clear message instead of crashing or producing a partial file. If the result is not actually smaller, say so and keep the original; never display "saved X%" for a larger file.
- **History:** each job logs filename, original/new size, percent saved, settings used, thumbnail; "Reuse settings" action to apply the same preset to a new file.

---

### 7.9 Text Counter

**Requirements**
- Large text area (paste, type, or **import .txt** via SAF; also accepts text shared from other apps via `ACTION_SEND` and `ACTION_PROCESS_TEXT`).
- **Live stats** (update as you type, debounced for very large text): characters **with spaces**, characters **without spaces**, words, sentences, paragraphs, lines, plus **reading time** (238 wpm) and **speaking time** (150 wpm).
- **Count options (toggles/chips):**
  - Count spaces: on/off
  - Count line breaks: on/off
  - Count punctuation: on/off
  - Count numbers/digits: on/off
  - Case sensitive: on/off
  - Ignore emojis; count emoji as 1 character (grapheme-cluster-aware counting, so use `BreakIterator`/ICU grapheme segmentation, not `String.length`)
- **Find and count specific item:** search field with modes: **Word** (whole-word match), **Letter/character**, **Phrase/substring**, optional **Regex** (advanced, wrapped in try/catch with a timeout). Shows total occurrences, highlights all matches in the text, with prev/next navigation and a per-match position.
- **Word frequency:** list of top words with counts and percentage, with options to ignore common stopwords (English list bundled), minimum word length, and sort by count/alphabetical. Also **letter frequency** bar chart (simple monochrome bars).
- **Character-limit presets:** X/Twitter (280), Instagram bio (150), Instagram caption (2,200), SMS (160 / 70 for Unicode, shows segment count), Meta description (160), SEO title (60), LinkedIn post (3,000), custom. Show a thin progress bar and remaining count; turns from `textPrimary` to the destructive tone only when exceeded.
- Utility buttons: Clear, Copy, Paste, Undo/Redo.
- **History:** "Save text" button stores snippets (title = first 40 chars, stats snapshot, timestamp); history list with search, rename, reopen, and delete. Autosave a draft so text isn't lost when leaving the screen.

**Acceptance:** counts match a reference implementation (Word-style) on a test corpus including Unicode, emojis, multiple spaces, tabs, and Windows line endings.

---

### 7.10 Date Calculator

**Requirements** (use `java.time`; handle leap years and month-end edge cases)
- Tabs:
  1. **Age:** from date of birth to today (or a chosen date) → years, months, days; total months/weeks/days/hours; **next birthday** countdown and the weekday it falls on.
  2. **Difference:** between two dates → years/months/days, total days/weeks; toggle **include end date**; toggle **business days only** (Mon–Fri, with an optional custom weekend and an editable holiday list the user can add dates to).
  3. **Add / Subtract:** start date ± years/months/weeks/days (and business days) → resulting date with weekday.
  4. **Day info:** for any date → weekday, day of year, week number (ISO), quarter, leap year yes/no, days remaining in year.
- Material-minimal date pickers plus quick **typed entry** (DD/MM/YYYY, respects locale format setting) with validation; "Today" shortcut chip.
- Each result has Copy and Share (formatted summary text).
- **History:** saves each calculation with its type, inputs, and result; reopen/edit; pin common ones ("Days until <label>") as **Saved countdowns** (user-named events with a live days-remaining display shown at the top of the tool).

---

### 7.11 Dice

- Choose **1–10 dice** and sides: d4, d6, d8, d10, d12, d20, d100, or **custom sides (2–1000)**. Optional modifier (+/−N) and "sum" display.
- Tap or **shake the phone** (shown only if the phone has an accelerometer; toggleable) to roll. Smooth minimal animation (numbers flicker then settle; no 3D). Haptic tick on settle.
- Show each die value and the total. Support **advantage/disadvantage** for a single d20.
- Use `SecureRandom`. Roll history list (last 100) with expression (e.g., "3d6+2 → 4, 2, 6 +2 = 14"), timestamp, and "Roll again."
- Preset save: "Save roll" (e.g., "Attack: 1d20+5") shown as quick chips.

### 7.12 Coin Flip

- Single or multiple coins (1–10), tap to flip (or shake to flip, only if the phone has an accelerometer). Minimal flat 2D flip animation, heads/tails labels (custom labels optional, e.g., "Yes/No").
- Counters for session totals (heads/tails count and percentage). History of flips with timestamps; "Reset session" button; history persists.

### 7.13 Random Number Generator

- Inputs: **Minimum**, **Maximum** (inclusive), **How many numbers** (default 1; **2** supported and highlighted as a quick option; up to 1000), **Allow duplicates** toggle (when off, validate that count ≤ range size), **Integer / Decimal** mode (decimals with chosen precision), **Sort results** toggle.
- **Seed (optional):** text/number input. When provided, results are **deterministic and reproducible**: same seed + same settings → same output sequence. Implement with a documented, stable algorithm (e.g., `SplittableRandom`/xoshiro256** seeded via SplitMix64 from the seed string's 64-bit hash) and put the algorithm name in the "About" sheet so users can reproduce it elsewhere. Without a seed, use `SecureRandom`. Show a "Seeded / Unseeded" badge on results.
- Validate: min ≤ max, ranges within Long limits, friendly inline errors.
- Result card with large numerals, Copy all / Copy as CSV, "Generate again", and "Use next in sequence" for seeded mode (advances the sequence index and shows it).
- **History:** each generation logs settings (including seed), results, and timestamp; tapping re-runs with the same settings (and for seeded runs reproduces exactly).

### 7.14 Picker Wheel

- Create a wheel by entering options (one per line or add chips); support up to 100 options. Optional **weights** per option, **colors are grayscale shades** to keep with the theme (alternating tones with thin separators).
- Spin by tap/swipe; physics-based ease-out deceleration (3–6s, randomized), haptic ticks as segments pass the pointer, final result in a bottom sheet with **"Remove this option and spin again"** (elimination mode) and **"Spin again."**
- Options: spin duration, sound off by default (no audio permission needed; use system haptics), toggle to avoid repeating the last winner.
- **Saved wheels:** name and save wheels (e.g., "Lunch spots"); list screen with rename, duplicate, delete, pin to Home as a shortcut.
- **History:** log of spins per wheel (winner, timestamp); stats per wheel (how often each option won).

### 7.15 Team Splitter

- Input names: type one per line, add chips, or **paste a list**; import from a text file. Detect and warn on duplicate names.
- Split by **number of teams** or **team size**; handle uneven splits explicitly (e.g., 11 people into 3 teams → 4/4/3) and show it.
- Options: **balance by skill** (optional per-name 1–5 rating; use snake draft or greedy balancing so team totals are as even as possible), **keep together** (pairs that must be on the same team), **keep apart** (pairs that must be separated), and **exclude names** (toggle a person out for this round without deleting them).
- **Shuffle** button for a new random split (with a subtle shuffling animation), optional **seed** for reproducibility (same as RNG tool). Custom team names (default: Team 1, Team 2, or editable color-free labels).
- Output: team cards (minimal lists), **Copy as text**, **Share**, and **Save as image**.
- **Saved groups:** save the name list as a group ("Cricket Sunday") to reload instantly.
- **History:** each split stored (groups, teams, seed, timestamp); reopen to view or re-shuffle with the same group.

---

## 7A. BRAND: Quacky logo and app icon

Create the complete visual identity yourself, as hand-authored vector files (you cannot rely on an image generator). Do the logo **before** the UI polish so it can be used in the splash screen, About screen, and Easter egg.

**Concept:** a minimalist duck mascot that matches the dark monochrome design system: friendly, geometric, instantly recognizable at 48px.

**Design rules**
- Built from a few simple geometric shapes only (circle/ellipse body, smaller circle head, one rounded triangle beak, one small round eye). Rounded corners, no outlines thinner than the rest, no gradients, no shadows, no text inside the icon.
- **Grayscale only.** Duck in `#F2F2F2` (white-ish) on a `#0A0A0A` background, with the eye and beak detail cut out as negative space (or using `#5E5E5E` for the beak so it reads as a subtle tone, not a color).
- It should feel calm and clever, not cartoonish or childish. Think "Mailchimp-simple", not "kids' game".
- Must stay legible when shrunk to 24px, as a single-color silhouette, and in the Android 13 themed (monochrome) icon.
- Optionally hide a tiny detail that rewards a close look (for example, the negative space between the beak and head subtly forming a small "Q"). Only do this if it stays clean.

**Deliverables (create all of these)**
1. `design/logo/quacky_mark.svg`: the duck mark alone, 512×512 viewBox, clean paths.
2. `design/logo/quacky_wordmark.svg`: the mark plus the word "Quacky" set in Satoshi Bold (convert text to paths so the SVG renders without the font).
3. **Adaptive launcher icon** (API 26+): `ic_launcher_foreground.xml` (VectorDrawable, duck inside the 66dp safe zone of the 108dp canvas), `ic_launcher_background.xml` (solid `#0A0A0A`), and `ic_launcher_monochrome.xml` (single-color silhouette for Android 13+ themed icons). Wire up `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.
4. **Splash screen** using the AndroidX SplashScreen API: dark background, centered duck mark, short fade; no long animation (under 600ms).
5. **Play Store icon** `design/logo/ic_launcher_512.png` (512×512, full-bleed, no rounded corners applied) generated from the SVG (use a small script or Gradle task; document how).
6. An in-app `QuackyMark` composable (vector-based) used in: splash, About screen, empty states (small, `textTertiary`), and the Easter egg.
7. A short `design/logo/README.md` explaining the construction, clear-space rule (minimum margin = half the head diameter), and minimum size.

Show me a rendered preview of the final icon (screenshot or PNG) when you finish this step, and include it in the README.

---

## 7B. EASTER EGG: "A note from the developer"

**Trigger (hidden):** On the Settings → About screen, tap the **version number** 7 times (like Android's developer-options gesture). Starting from the 3rd tap, show a tiny toast/snackbar countdown in Quacky's voice: "4 more quacks…", "3 more quacks…", … "1 more quack…". Each tap gives a light haptic tick. On the 7th tap, play a distinctive **double-pulse "quack" haptic** and open the Easter egg screen. Also add a **second hidden trigger**: long-press the duck logo on the About screen for 2 seconds.

**Screen design (full-screen, same dark monochrome system; no extra permissions; no audio)**
- Slow fade-in from black. The `QuackyMark` at the top does a tiny, charming idle animation (a gentle head bob or a single blink every ~4 seconds; subtle and cheap on battery; stop animating when the screen is not visible).
- A heading in Satoshi Bold: **"You found the quack."**
- Below it, the developer's note, split into short paragraphs that fade in one after another (staggered 300ms). Users can tap to skip the animation. The text must be selectable and respect font scaling.
- A footer with developer details and two actions.

**Note content** (store in `strings.xml` so it can be edited; use the placeholders; keep my first-person voice; you may lightly polish grammar but keep the meaning and tone honest and plain):

> Hi, I'm {{YOUR NAME}}, and I made Quacky.
>
> I got tired of needing a QR scanner, a ruler, a color picker or a file compressor and ending up in an app that was half ads, half permission requests. The tools were simple, but the experience was not.
>
> So I built the app I wanted for myself: all the small everyday tools in one place, clean and quiet.
>
> Quacky works **fully offline**. It doesn't even have permission to access the internet. There are no ads, no accounts, no analytics, and no tracking. Nothing you scan, measure, pick, count or compress ever leaves your phone. Your history is stored only on your device, and you can delete it any time.
>
> If your phone is missing something a tool needs, like a camera or AR support, Quacky will tell you plainly instead of pretending it works. I'd rather be honest than give you false hope.
>
> I built it myself, and I'm still improving it. If something is broken, missing, or you just have an idea for a tool, I'd genuinely like to hear from you.
>
> Thanks for using it. Quack.
>
> — {{YOUR NAME}}

**Footer block:**
- "Developed by {{YOUR NAME}}"
- Email: `{{YOUR EMAIL}}` as a tappable row.
  - Tap → opens an email composer through an `ACTION_SENDTO` `mailto:` intent with a prefilled subject "Quacky feedback" and a body that includes the app version and Android version (show the user exactly what will be added; no device identifiers). This is user-initiated only.
  - Long-press → copies the email address to the clipboard with a "Copied" confirmation.
- A small pill-shaped line: "No ads · No tracking · 100% offline".
- A "Close" text button and system back both exit. Persist a flag `easter_egg_found = true` in DataStore. After it's found, show a small, unobtrusive "Developer's note" row in About so the user can open it again without the 7 taps.

**Rules**
- Do not collect, log, or store anything about who opened this screen.
- This is the only place the developer's email appears in the app (and the README / store listing).
- Add a UI test that taps the version 7 times and asserts the screen opens, and a test that confirms the `mailto:` intent is built correctly.

---

## 7C. HOW-TO TIPS (every tool gets a guide: text + visual)

**Goal:** a first-time user should understand every tool within seconds, without reading documentation. Every tool has a **How to use** guide made of short steps, and **each step pairs one plain-language text tip with one small visual tip**.

### 7C.1 Where guides appear
1. **Help (?) icon** in every tool's top bar (next to pin and history) opens the guide any time.
2. **First open:** the first time a user opens a tool, show its guide automatically once (as a bottom sheet, 60-70% height, swipeable). Buttons: **Got it** and **Skip**. Store `tip_seen_<toolId>` in DataStore. Do not show it on the missing-requirement message screen (5A); show it once the tool actually runs.
3. **Pin / long-press menu → "How to use"** opens the same guide from Home, Category, and Search.
4. **Settings → Help & Tips:** toggle "Show tips on first open", button "Reset all tips", and a list **"All how-to guides"** (every tool, grouped by category) so users can read any guide without opening the tool.
5. **Inline info tips (ⓘ):** tiny info icons beside non-obvious controls open a small popover with 1-2 plain sentences (and a mini visual where noted in 7C.4). Tap anywhere to dismiss; also reachable by long-pressing the control.

### 7C.2 Guide format
- A horizontally swipeable **stepper**: step dots at the top, "Step 2 of 4" label, **Back / Next** buttons, **Done** on the last step, and a Skip text button.
- Each step has: a **visual area** (16:10, `surface` card with a 1dp outline), a **bold title** (max ~6 words), and **1-2 short sentences** of body text (max ~25 words). Plain language, no jargon, second person ("Tap…", "Drag…"). Where jargon is unavoidable (HEX, EXIF, seed), explain it in the same sentence.
- The last step of each guide is a **"Good to know"** tip when useful (privacy, accuracy, limits).
- Text must be selectable, scale with system font size, and be fully available to TalkBack. Each visual has a `contentDescription` that describes what the animation shows.

### 7C.3 Visual tips (how to build them)
- Visuals are **hand-built Compose Canvas animations**: simple, monochrome (white and gray strokes on `surface`), looping every 2-4 seconds. **No video, GIF, network image, or heavy animation library**, so the app stays small and fully offline.
- Build a small reusable kit in `core/tips/`: `PhoneFrame` (rounded phone outline with optional screen content), `FingerTap` (ripple on tap), `FingerDrag` (path with finger dot), `PinchGesture`, `Arrow`, `HighlightRing` (pulsing ring around the element being explained), `Callout` (tiny label), `SwipeHint`. Compose each tool's visuals from these primitives so they stay consistent and cheap.
- Respect the system "Remove animations" / reduce-motion setting: show a static final frame instead.
- Animations pause when the sheet is not visible. Keep them under ~1ms of CPU per frame; no allocations in draw loops.
- Model: `TipStep(visual: TipVisual, titleRes, bodyRes, a11yRes)` and `ToolGuide(toolId, steps)`; a `GuideRegistry` maps `toolId` to its guide. All text lives in `strings.xml`.
- Add a debug-only "Guide gallery" screen (not in release) that lists every guide for quick review.

### 7C.4 Guide content per tool
Use the following as the starting copy and visuals (edit wording for clarity if needed, keep the meaning and the one-visual-per-step rule).

**QR & Barcode Scanner**
1. *Point at the code.* Visual: phone showing a camera view with corner brackets and a QR code; a scan line sweeps once; the brackets pulse once in white. Text: "Hold your camera over a QR code or barcode. It scans by itself, no button to press."
2. *Dark or far away?* Visual: torch icon glows, then a pinch gesture enlarges the code. Text: "Tap the torch for light. Pinch the screen to zoom in."
3. *Scan from an image.* Visual: gallery icon is tapped, a picture with a QR code slides in and gets scanned. Text: "Have a screenshot or saved photo? Tap the gallery icon and choose it."
4. *Choose what to do.* Visual: result sheet slides up with action rows highlighted one by one (Copy, Open, Connect). Text: "After scanning, pick an action: open a link, copy text, join Wi-Fi, or save a contact."
5. *Good to know.* Visual: a link row with a lock/hand icon. Text: "Links never open on their own. You always see the full address first."

**QR & Barcode Generator**
1. *Pick the type.* Visual: chips (Text, Link, Wi-Fi, Contact, UPI) highlight in turn. Text: "Choose what the code is for. Each type gives you the right fields."
2. *Fill in details.* Visual: a field fills with typed text while the QR on the right redraws live. Text: "Type your details. The code updates as you type."
3. *Keep it scannable.* Visual: two code samples, dark-on-light with a check, light-on-light with a cross. Text: "Use a dark code on a light background. Low contrast codes may not scan."
4. *Save or share.* Visual: export sheet with PNG, SVG, PDF rows highlighted. Text: "Save as PNG, SVG or PDF, or share it. Tap Save as template to reuse it later."
- Inline ⓘ: **Error correction** ("Higher levels still scan if the code is dirty or has a logo on it, but make the code denser.").

**AR Ruler**
1. *Find a surface.* Visual: phone sways slowly, a dotted grid appears on a floor/table plane. Text: "Move your phone slowly over a surface until dots appear. Good light helps."
2. *Place the points.* Visual: finger taps start point, phone moves, finger taps end point, a line with a distance label appears. Text: "Tap where you want to start, then tap where you want to end."
3. *Pick a mode.* Visual: four mini icons (Distance, Path, Height, Angle) highlight in turn. Text: "Switch modes at the bottom to measure a path with many points, a height, or an angle."
4. *Measuring height.* Visual: finger taps the floor first (ring), then the top of a door. Text: "For height, tap the floor first, then tap the top."
5. *Good to know.* Visual: phone with a soft "±" wobble around the number. Text: "AR gives a good estimate, not a perfect measurement. Plain, well-lit, non-shiny surfaces work best."

**On-Screen Ruler**
1. *Line it up.* Visual: a small object placed against the ruler on the screen, its edge aligned with 0. Text: "Lay your object flat on the screen with one end at 0."
2. *Calibrate once.* Visual: a bank card outline on screen, a slider adjusts the outline until it matches the card. Text: "For the most accurate scale, calibrate once using any bank card."
3. *Use the markers.* Visual: two lines dragged apart, the readout updates. Text: "Drag the two markers to measure the gap between them."
4. *Left-handed?* Visual: ruler flips side. Text: "Tap the flip icon to move 0 to the other edge."
- Inline ⓘ: **Calibrated badge** ("Phones report their screen size differently. Calibrating makes sure 1 cm on screen is 1 cm in real life.").

**Area & Volume Calculator**
1. *Choose Area or Volume.* Visual: the two tabs toggle, shape chips slide by. Text: "Pick Area for flat surfaces and Volume for 3D shapes, then choose the shape."
2. *Enter the measurements.* Visual: a labeled rectangle where side "a" highlights while the matching field is highlighted. Text: "Each field matches a labeled side in the picture. Choose a unit for each one."
3. *Read the results.* Visual: result card expands into the "all units" list. Text: "See the answer instantly. Tap Show all units to see it in other units."
4. *Estimate materials.* Visual: a wall rectangle with a door cut out, then a "litres of paint" number appears. Text: "Use the Estimate tab to work out paint, tiles, concrete or water tank size."
5. *(Only if AR is available on this phone)* *Use your AR measurement.* Visual: ruler icon drops a number into a field. Text: "Tap Use AR measurement to fill a field from a saved AR Ruler result."

**Color Picker**
1. *Choose a source.* Visual: segmented control toggles Camera and Gallery. Text: "Pick colors live with the camera, or open a photo from your gallery."
2. *Place the pin exactly.* Visual: finger drags the crosshair; a magnifier circle above shows zoomed pixels with the center pixel outlined. Text: "Tap to place the crosshair, then drag it. The magnifier shows exactly which pixel you are on."
3. *Nudge by one pixel.* Visual: arrow buttons tap and the crosshair moves one pixel with a coordinate readout changing by 1. Text: "Use the small arrows to move the pin one pixel at a time."
4. *Zoom in.* Visual: pinch gesture zooms into an image while the crosshair stays on the same spot. Text: "Pinch to zoom for tiny details. The pin stays locked to the same spot on the image."
5. *Point or average?* Visual: a single highlighted pixel vs a 3×3 block of pixels averaged. Text: "Point reads one pixel. Average blends nearby pixels, which is steadier for camera colors."
6. *Freeze the camera.* Visual: shutter freezes the live view into a still. Text: "Tap Freeze to hold the picture still, then take your time placing the pin."
7. *Build a palette.* Visual: pins numbered 1-3 on an image, their swatches fill a strip below. Text: "Drop up to 8 pins and save them together as a palette."
8. *Copy the value.* Visual: swatch with HEX/RGB/HSL rows, a copy icon taps. Text: "Tap any value to copy it. Open Contrast to check if two colors are readable together."
- Inline ⓘ: **Sampling size** (with the Point vs Average mini visual), **HEX/RGB/HSL/CMYK** (one line each), **Contrast ratio** ("Higher is easier to read. 4.5 or more passes for normal text.").
- Good to know: "Camera colors change with lighting. Colors from photos in your gallery are exact."

**Photo Metadata Viewer & Remover**
1. *Pick photos.* Visual: grid of photos, a few get checkmarks. Text: "Choose one or many photos."
2. *See what's hidden.* Visual: photo with data tags (location pin, camera, date) unfolding from it. Text: "Photos can secretly store where and when they were taken and which device took them. This is called metadata."
3. *Choose what to remove.* Visual: three chips (All, Only location, Custom) with a location chip being crossed out. Text: "Remove everything, only the location, or pick exactly what to remove."
4. *Save a clean copy.* Visual: original stays, a second copy appears with a shield check. Text: "Your original photo is never changed. A clean copy is saved separately, then checked to confirm the data is gone."

**Image & PDF Compressor**
1. *Pick a file.* Visual: image and PDF icons drop into the tool. Text: "Choose JPG, PNG, WebP images, or a PDF."
2. *Choose how to shrink it.* Visual: a slider moves, and a second mode "Target size" with "under 200 KB" typed. Text: "Drag the quality slider, or type a target size like 200 KB and let the app find the settings."
3. *Compare before and after.* Visual: split-slider dragging across an image with sizes shown on each side. Text: "Drag the divider to compare quality. The new file size is shown right away."
4. *Save it.* Visual: result card with "saved 68%" and Save/Share buttons. Text: "Save or share the smaller file. Your original is kept."
- Good to know (PDF): "Compressed PDFs turn pages into images, so text may no longer be selectable."

**Text Counter**
1. *Type or paste.* Visual: text appears in a box while counters tick up. Text: "Type, paste, or import a text file. Counts update live."
2. *Choose what to count.* Visual: toggles (spaces, line breaks, punctuation, case) flip and the character count changes. Text: "Turn options on or off to count with or without spaces and more."
3. *Find a word or letter.* Visual: a search field, matches highlight in the text with a "5 found" badge. Text: "Search for a word, a letter, or a phrase to count how many times it appears."
4. *Check a limit.* Visual: a progress bar fills toward a "280" X/Twitter limit. Text: "Pick a preset like X, Instagram or SMS to see how many characters you have left."

**Date Calculator**
1. *Choose a calculation.* Visual: four tabs (Age, Difference, Add/Subtract, Day info) highlight in turn. Text: "Pick what you want to find out."
2. *Pick the dates.* Visual: a calendar opens and a date is selected; the "Today" chip taps. Text: "Choose dates from the calendar or type them in."
3. *Read the answer.* Visual: result shows "25 years, 3 months, 4 days" and "next birthday in 51 days". Text: "Results are shown in years, months and days, plus totals."
4. *Save a countdown.* Visual: an event name with "42 days left" pins to the top. Text: "Save an important date and see how many days are left, any time you open this tool."
- Inline ⓘ: **Include end date** ("Counts the last day too, so Mon to Wed becomes 3 days instead of 2.").

**Dice**
1. *Set up your dice.* Visual: steppers for number of dice and a dropdown of d4-d20. Text: "Choose how many dice and how many sides each has."
2. *Roll.* Visual: finger taps and the numbers flicker then settle; a phone shake icon alongside (only if shake is available). Text: "Tap Roll or shake your phone."
3. *Add a modifier.* Visual: "3d6 +2" builds up and the total is shown. Text: "Add a bonus or penalty. The total includes it."
4. *Save favorites.* Visual: "Attack 1d20+5" becomes a chip. Text: "Save a roll you use often and roll it again with one tap."

**Coin Flip**
1. *Flip.* Visual: coin flips in 2D and lands on heads/tails. Text: "Tap the coin to flip it" (add "or shake your phone" only if available).
2. *Use your own labels.* Visual: labels change from Heads/Tails to Yes/No. Text: "Rename the sides, like Yes and No, to settle a decision."
3. *Track results.* Visual: counters Heads 6, Tails 4 update. Text: "See how many times each side came up. Reset any time."

**Random Number Generator**
1. *Set the range.* Visual: Min 1 and Max 100 fields highlighted on a number line. Text: "Enter the smallest and largest number you want, both included."
2. *How many?* Visual: a count stepper at 1 then 2, results appear. Text: "Choose how many numbers to generate. Pick 2 for a quick pair."
3. *Repeats or not.* Visual: two rows, one with repeated numbers, one with all different. Text: "Turn off Allow duplicates to get all different numbers."
4. *Use a seed (optional).* Visual: same seed typed twice, same three numbers appear both times. Text: "A seed is a starting code. Using the same seed and settings always gives the same numbers, which is useful for fair, repeatable draws."
- Good to know: "No seed means truly random every time."

**Picker Wheel**
1. *Add options.* Visual: names get added one by one and slices appear on the wheel. Text: "Type your options, one per line. The wheel updates as you add them."
2. *Spin.* Visual: finger flicks the wheel, it slows and stops at the pointer. Text: "Tap Spin or flick the wheel. The slice under the pointer wins."
3. *Spin again without repeats.* Visual: winner slice fades from the wheel. Text: "Tap Remove and spin again to eliminate winners one by one."
4. *Save your wheel.* Visual: name field "Lunch spots" saves; wheel appears in a list. Text: "Save wheels you reuse, like lunch spots or chores."
- Inline ⓘ: **Weights** ("A bigger weight makes an option more likely to win.").

**Team Splitter**
1. *Add names.* Visual: names typed and pasted, chips appear. Text: "Type names, paste a list, or load a saved group."
2. *Choose teams or team size.* Visual: stepper switches between "3 teams" and "4 per team"; an uneven split of 4/4/3 is shown. Text: "Pick how many teams, or how many people per team. Uneven groups are split as evenly as possible."
3. *Shuffle.* Visual: names reshuffle between team cards. Text: "Tap Shuffle for a new random split."
4. *Fine-tune.* Visual: two names link with a chain ("keep together") and two separate with a gap ("keep apart"); star ratings balance totals. Text: "Keep friends together, keep others apart, or add skill ratings to balance the teams."
5. *Share.* Visual: teams card with Copy/Share/Image icons. Text: "Copy the teams as text or save them as an image."

### 7C.5 Quality rules for tips
- Every tool must have a guide with at least 3 steps; every step must have both a text tip and a visual tip.
- Visuals must match the real UI (same icons, labels, and layout as the tool), so users recognize them. Reuse the tool's real components in the mini preview where possible.
- No step should assume prior knowledge of terms like HEX, EXIF, seed or ARCore without a one-line explanation.
- Tests: a UI test per tool that opens the guide, swipes through every step, and confirms the text and content descriptions exist; a test that the first-open guide shows exactly once; a test that "Reset all tips" shows it again; a test that guides never show on the missing-requirement screen.

---

## 8. Data model summary (Room)

- `history_entry` (generic, see 4.6)
- `saved_palette` + `palette_color`
- `saved_wheel` + `wheel_option`
- `saved_group` + `group_member`
- `saved_countdown`
- `saved_template` (QR templates)
- `search_history`
- `tool_usage` (toolId, lastUsedAt, count) for Recents only; stays local

Use Room migrations from v1; export/import in Settings must round-trip all of these.

---

## 9. Performance, quality, and reliability

- Cold start < 1.5s on a mid-range device; no heavy work in `Application.onCreate`.
- All image/PDF/bitmap work on `Dispatchers.Default/IO`; downsample large images for preview; respect memory (check `ActivityManager.isLowRamDevice`).
- Handle process death and configuration changes (rotate, dark/light system changes, split-screen) for every tool; use `SavedStateHandle` for in-progress input.
- Strict null-safety and sealed UI states (`Loading / Content / Empty / Error`).
- No crashes on permission denial, missing ARCore, corrupt files, or huge inputs.
- R8/ProGuard enabled for release; keep ML Kit/ARCore rules correct. Target APK/AAB size as small as reasonable (use ABI splits).
- Accessibility: contrast ≥ 4.5:1 for text, 48dp touch targets, content descriptions, logical focus order.
- Localization-ready: all strings in `strings.xml` (English only for v1), locale-aware number/date formatting.

---

## 10. Privacy statement (put on About screen and in README)

"This app has no ads, no accounts, no analytics, and no internet access. All processing happens on your device. Your history is stored locally and can be cleared at any time."

---

## 11. Project structure

```
app/
  core/            (design system, theme, components, navigation, utils, registry, capability/, tips/)
  data/            (Room, DataStore, repositories)
  feature/
    home/ search/ history/ settings/ about/ (about + easter egg) help/ (all how-to guides) brand/ (QuackyMark, splash)
    qrscanner/ qrgenerator/
    arruler/ screenruler/ areavolume/
    colorpicker/ metadata/ compressor/
    textcounter/ datecalc/
    dice/ coinflip/ randomnumber/ pickerwheel/ teamsplitter/
```

Reusable shared components to build once: `ToolScaffold` (standard top bar), `ToolTile`, `CategoryRow`, `HistorySheet`, `PermissionGate` (camera rationale), `ResultCard`, `CopyButton`, `UnitDropdown`, `SectionLabel`, `EmptyState`, `ConfirmDialog`, `MissingRequirementScreen`, `HowToSheet`, `TipVisuals` (Canvas kit), `InfoTip`, and a shared `CameraPreview` composable (used by Scanner, Color Picker, and AR fallbacks).

---

## 12. Definition of done (global)

- App builds with `./gradlew assembleDebug` and `./gradlew assembleRelease` with zero warnings that matter.
- Every tool in section 7 is reachable from Home, Category, and Search, can be pinned, and writes to history where specified.
- Manifest contains **no INTERNET permission** (add a unit/lint check that fails the build if it appears).
- Unit tests pass for: text counting, date math, area/volume math, seeded RNG determinism, team balancing, nearest-color-name, color conversions, target-size compression search, QR payload parsing/generation, and the capability checker against fake device profiles (5A.8).
- The logo deliverables in 7A exist, the adaptive and themed icons render correctly on a launcher, and the Easter egg in 7B opens after 7 taps on the version number.
- Opening any tool whose requirement is missing shows the plain message from 5A.3, with no permission prompts, no fallback modes, and no tool is dimmed, hidden or badged anywhere else. No button is shown that cannot work on the current phone.
- Every tool has a how-to guide per 7C (text + visual on every step), reachable from the (?) icon, the first-open sheet, the long-press menu, and Settings → Help & Tips, with inline ⓘ tips where listed.
- Provide `README.md` (setup, font instructions, architecture, how to add a new tool via `ToolRegistry`) and `DECISIONS.md`.

---

## 13. Build order (do these in order, committing after each step)

1. Project setup, theme, Satoshi font loading with fallback, design system components, **then the Quacky logo, launcher icons and splash screen (section 7A)**.
2. `ToolRegistry`, **capability checker and missing-requirement message screen (section 5A)**, navigation, Home (categories, pinned, recents), Search, pin logic, DataStore.
3. Room database, generic history repository, Global History screen, Settings, and the tips framework (`HowToSheet`, visual kit, first-open logic from section 7C).
4. Text Counter, Date Calculator (pure logic, fast wins; establishes tool conventions).
5. Random tools: Dice, Coin Flip, RNG, Picker Wheel, Team Splitter.
6. On-Screen Ruler.
7. Shared `CameraPreview` + `PermissionGate`; QR & Barcode Scanner; QR & Barcode Generator.
8. Color Picker (including the full pin-point system: reticle, loupe, nudge, freeze, zoom, multi-pin, sampling sizes, palettes).
9. Photo Metadata Viewer & Remover.
10. Image & PDF Compressor.
11. Area & Volume Calculator (with estimators).
12. AR Ruler (with the strict availability gating from 5A, no fallback), then wire "send to Area & Volume".
13. About screen and the Easter egg (section 7B), app shortcuts, optional widget, polish, accessibility pass, performance pass, tests, README/DECISIONS.

For every tool built in steps 4-12, add its how-to guide and inline tips from section 7C in the same step; a tool is not done without its guide.

After each numbered step: run the app, verify against that tool's acceptance notes, fix issues, and summarize what's done and what's next before continuing.

---

## 14. Out of scope for v1 (do NOT build)

Document scanner, OCR, unit converter, password generator, level/compass, decibel meter, light theme, cloud sync, accounts, in-app purchases, any networked feature, and any fallback, degraded, or "fake" mode for tools whose hardware requirements are not met.
