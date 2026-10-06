# Architectural Decisions Record (DECISIONS.md)

## ADR 001: Offline-First Zero Network Policy
- **Decision:** No `INTERNET` permission is declared anywhere in `AndroidManifest.xml`.
- **Rationale:** Strict adherence to privacy promises. All tools (scanning, generation, metadata removal, compression, mathematical solvers) operate exclusively on-device.

## ADR 002: Missing Hardware Honesty (Section 5A)
- **Decision:** Hardware gating happens strictly at runtime upon tool launch via `DeviceCapabilities` and `RequirementChecker`.
- **Rationale:** Never hide or dim tools on Home or Search, but never offer degraded or "fake" fallback modes. If the device genuinely lacks a back camera or ARCore, inform the user plainly with `MissingRequirementScreen`.

## ADR 003: Typeface Strategy & Build Resilience
- **Decision:** Satoshi is configured as the default design typography, falling back to `FontFamily.SansSerif` if font binaries are not yet populated into `res/font/`.
- **Rationale:** Adheres to section 3.3 to guarantee that missing font assets do not fail builds while preserving precise token scaling (Display 34sp, Title 22sp, Section 13sp, Body 15sp, Caption 12sp).

## ADR 004: Monochromatic Dark Aesthetic
- **Decision:** Strict palette tokens (`#0A0A0A`, `#121212`, `#1A1A1A`, `#2A2A2A`, `#F2F2F2`, `#9A9A9A`, `#5E5E5E`, `#FFFFFF`).
- **Rationale:** Content is the only element allowed to feature color (camera feed, picked color swatches, user imagery, and sparse `#B3261E` destructive confirmation).

## ADR 005: ML Kit Bundled Model
- **Decision:** Utilize `com.google.mlkit:barcode-scanning` bundled artifact rather than the Play Services download variant.
- **Rationale:** Guarantees barcode scanning works offline immediately upon initial app launch, even in airplane mode.

## ADR 006: Brand Vector Asset Pipeline
- **Decision:** The Quacky mascot is hand-authored as a geometric SVG vector (`quacky_mark.svg`), adaptive vector drawables (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_launcher_monochrome.xml`), and a native Compose Canvas composable (`QuackyMark`).
- **Rationale:** Guarantees crisp resolution at any scale, support for Android 13+ themed icons, and ultra-lightweight rendering across splash, empty states, and Easter egg animations.

## ADR 007: Lossless Metadata Stripping & Zero-Value Privacy History
- **Decision:** JPEG metadata is removed at the byte level by stripping APP1 (EXIF/XMP) and APP13 (IPTC) markers without re-encoding DCT coefficients, preserving pixel quality. Orientation rotation is applied to pixels only when orientation tags would otherwise be lost. History entries strictly log file names and category summaries—never metadata values.
- **Rationale:** Adheres strictly to Sections 7.7 and 8 privacy specifications while preserving image fidelity.

## ADR 008: Memory-Safe Compression & Binary Search Sizing
- **Decision:** Target-file-size compression utilizes an iterative binary search on quality, followed by proportional dimension downscaling if needed. PDF compression processes pages sequentially using `PdfRenderer` and `PdfDocument` with immediate bitmap recycling to prevent out-of-memory errors on large documents.
- **Rationale:** Adheres to Section 5A.5 runtime honesty and prevents OOM crashes on memory-constrained devices.

