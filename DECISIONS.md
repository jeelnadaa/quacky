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
## ADR 009: Geometric Solvers & Estimator Modeling
- **Decision:** Area & Volume Calculator provides pure Kotlin mathematical models for 10 2D shapes (including the Shoelace polygon formula) and 8 3D shapes, coupled with unit conversions across metric and imperial systems and practical material estimators.
- **Rationale:** Guarantees deterministic geometry and material estimation with input validation (e.g. triangle inequality, positive dimensions) preventing runtime crashes.

## ADR 010: AR Ruler Architecture & 3D Screen Projection
- **Decision:** Sceneform is deprecated, so AR Ruler couples ARCore plane tracking with an OpenGL ES 2.0 camera background renderer (`ArRenderer`) and a Jetpack Compose overlay. 3D world anchors are projected onto 2D viewport coordinates via pure column-major View-Projection matrix transforms. Floating distance labels automatically face the camera and render in Satoshi typography.
- **Rationale:** Fulfills Section 7.3 specifications with zero heavy external rendering engines, supports all 4 measurement modes (Distance, Path, Height, Angle), and allows seamless interoperability by sending measured dimensions directly to Area & Volume Calculator.

## ADR 011: App Shortcuts & External Intent Ingestion
- **Decision:** Declare static launcher shortcuts for frequently accessed tools (Barcode Scanner, Screen Ruler, Color Picker, Image Compressor) in `shortcuts.xml` and support runtime launch intents in `MainActivity` with `initialToolId`.
- **Rationale:** Minimizes friction for time-sensitive utilities while keeping the single-activity architecture clean.

## ADR 012: Offline Integrity & Automated Manifest Verification
- **Decision:** Automated JVM unit test (`OfflineManifestTest`) inspects `AndroidManifest.xml` during every test run to guarantee zero declarations of `android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, or related network permissions.
- **Rationale:** Guarantees regression-free adherence to the 100% offline privacy guarantee.

## ADR 013: Multi-Measurement AR Ruler Sessions
- **Decision:** Support concurrent in-session measurements (up to 20 measurements per session) with independent modes (Distance, Path, Height, Angle). Distinguish finished measurements using numbered chips and a calibrated 5-tone gray palette, with active/selected measurements drawn in solid white. Support tick finish and auto-finish settings, expandable session drawer with copy-all/multi-delete, and cross-tool dimension export into Area & Volume Calculator.
- **Rationale:** Greatly enhances real-world measurement workflows (e.g. measuring entire rooms) while keeping memory within strict mobile constraints.

## ADR 014: Document Scanner & Perspective Quad Rectification
- **Decision:** Document Scanner operates 100% on-device using native Android 2D graphics matrices (`Matrix.setPolyToPoly`) to perform perspective homography without external OpenCV binaries. Implements Microsoft Lens-inspired document enhancement filters (Magic Color, B&W binarization, Grayscale) via `ColorMatrix`, multi-page sequencing, and native A4 PDF generation via `PdfDocument`.
- **Rationale:** Delivers high-performance, lightweight document scanning with zero cloud dependencies and zero privacy risk.
