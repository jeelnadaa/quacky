# Quacky

> **Pure offline utility suite for Android.**  
> 15+ focused tools. Zero network permissions. Zero ads. Monochromatic dark design.

---

## 🦆 Philosophy & Principles

- **100% Offline by Design**: `android.permission.INTERNET` is strictly prohibited. Everything runs on-device—scanning, processing, image compression, metadata scrubbing, and geometric solvers.
- **Hardware Honesty (Section 5A)**: If hardware (e.g. Back Camera, ARCore) is unavailable, Quacky states it clearly and plainly with an explanation, never hiding tools or faking readings.
- **Monochromatic Dark Aesthetic**: Strict palette (`#0A0A0A`, `#121212`, `#1A1A1A`, `#2A2A2A`, `#F2F2F2`, `#9A9A9A`, `#5E5E5E`). Content is the only element allowed to feature color (camera feed, swatches, user imagery).
- **Zero Privacy Footprint**: History records timestamps and tool summaries, but never logs private user metadata, passwords, or locations. All data lives in local Room SQLite storage and can be wiped with one tap.

---

## 🛠️ Tool Suite

| # | Tool | Highlights |
|---|---|---|
| 1 | **Text Counter** | Real-time words, characters, sentences, paragraphs, reading & speaking time, word frequency, case converter. Supports `ACTION_PROCESS_TEXT` and `ACTION_SEND`. |
| 2 | **Date Calculator** | Date differences in days/weeks/months/years, add/subtract duration, countdown timer, business days exclusion. |
| 3 | **Dice Roller** | D4, D6, D8, D10, D12, D20, D100, custom sides. Haptic feedback, shake-to-roll, roll history with sum and statistics. |
| 4 | **Coin Flip** | Fair 3D-styled animated coin, heads/tails streak tracker, bias simulation test. |
| 5 | **Random Number Generator** | Min/max bounds, count, distinct toggle, sorting, seed reproducibility. |
| 6 | **Picker Wheel** | Custom weighted items, angular deceleration physics, sound/haptic ticks, winner celebration. |
| 7 | **Team Splitter** | Custom roster input, split by team count or team size, balance options, easy clipboard export. |
| 8 | **On-Screen Ruler** | Calibrated millimeter & inch ruler with dual calipers, calibration wizard against credit card or physical coin. |
| 9 | **QR & Barcode Scanner** | Offline ML Kit bundled model, instant format parsing (URL, Wi-Fi, vCard, Email, SMS, UPI, Geo), flash toggle, clipboard copy. |
| 10 | **Barcode Generator** | Generate QR, EAN-13, EAN-8, UPC-A, Code 128, Code 39, Aztec, Data Matrix, PDF417. Export to PNG/SVG with custom padding. |
| 11 | **Document Scanner** | Microsoft Lens-style offline document scanner with 4-corner perspective quad cropping, Magic Color / B&W / Grayscale enhancement filters, multi-page scanning, and native PDF generation. |
| 12 | **Color Picker & Palette Studio** | Camera eyedropper with crosshair, image picker, HEX/RGB/HSL sliders, WCAG contrast validator, 5-shade palette export. |
| 13 | **Photo Metadata Viewer & Stripper** | EXIF/XMP/IPTC inspection, GPS map preview, lossless byte-level marker stripping without re-encoding DCT coefficients. |
| 14 | **Image Compressor** | Target file size or percentage slider, iterative binary search sizing, format conversion (JPEG, WebP, PNG). |
| 15 | **PDF Compressor** | Sequential page downscaling and compression via native `PdfRenderer` and `PdfDocument` with immediate memory recycling. |
| 16 | **Area & Volume Calculator** | 10 2D shapes (with Shoelace polygon coordinate solver), 8 3D shapes, metric/imperial unit converter, paint/flooring/concrete material estimators. Pulls dimensions directly from AR Ruler sessions. |
| 17 | **AR Ruler** | Multi-measurement AR sessions (up to 20 per session, e.g. M1, M2). Numbered chips with distinct gray tones, per-measurement mode (Distance, Path, Height, Angle), tick finish, auto-finish setting, expandable bottom sheet with copy-all/multi-delete, grouped history sessions, and direct export to Area & Volume. |

---

## 🎨 Design System

- **Typography**: Satoshi (`FontFamily.SansSerif` build-resilient fallback).
- **Brand Identity**: Custom vector geometric duck mascot (`QuackyMark`), adaptive launcher icons with Android 13+ themed icon support (`ic_launcher_monochrome`).
- **Custom Tips & Guides**: Every single tool includes a custom Canvas-rendered how-to guide sheet with step-by-step vector illustrations.
- **Easter Egg**: 7 taps on the version number in Settings or a 2-second long-press on `QuackyMark` unlocks the developer letter with animated eye-blink mascot and feedback shortcuts.

---

## 🚀 Building & Testing

For detailed step-by-step instructions across Windows, macOS, and Linux, see the [Build Guide (BUILD.md)](file:///d:/quacky-app/BUILD.md).

### Quick Start:

```bash
# Run all unit tests (including strict offline manifest assertion)
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```
