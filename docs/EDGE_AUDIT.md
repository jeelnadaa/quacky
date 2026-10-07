# Edge Detection Audit Report: Root Causes of Over-Crop

## 1. Executive Summary
Quacky's document scanner previously exhibited an over-crop of approximately 1 inch (or 10-30+ screen/image pixels depending on perspective and resolution) on every side of captured documents. This audit identifies the exact architectural and mathematical causes in the previous implementation and details the fixes implemented in the new pipeline.

---

## 2. Root Cause Findings

### Cause 1: CameraX Coordinate Mapping Mismatch (`FILL_CENTER` vs. Direct Canvas Scaling)
- **File / Location:** `CameraPreview.kt:85-91`, `DocumentScannerScreen.kt:430-435`
- **Issue:** `PreviewView` was initialized with `ScaleType.FILL_CENTER` while `ImageAnalysis` had an unconstrained resolution and aspect ratio (often 16:9 on modern sensors). In `DocumentScannerScreen`, the detected quad coordinates `(x, y)` in `[0, 1]` were mapped directly as:
  ```kotlin
  Offset(q.topLeft.x * size.width, q.topLeft.y * size.height)
  ```
- **Consequence:** Because `FILL_CENTER` crops either the top/bottom or left/right of the sensor buffer to fill the screen viewport, treating normalized buffer coordinates as normalized screen coordinates resulted in a severe aspect ratio stretching and offset error. The overlay polygon appeared shifted outward and distorted by 10-15% of the frame dimension.
- **Fix:** Enforce uniform 4:3 aspect ratio across `Preview`, `ImageAnalysis` (target 1280x960), and `ImageCapture` using CameraX `ResolutionSelector` and a shared `ViewPort` bound in a `UseCaseGroup`. Coordinate transformations between Analysis buffer, Preview screen, and Still capture are now mediated by a dedicated `CoordinateMapper` utilizing CameraX transforms.

---

### Cause 2: Naive Inward Gradient Scanning at Low Resolution Without Sub-Pixel Edge Verification
- **File / Location:** `LiveEdgeDetector.kt:40-100`
- **Issue:** The previous detector downsampled frames to a coarse 64×48 thumbnail grid and used a threshold scan inward from the frame borders:
  ```kotlin
  val sampleW = 64
  val sampleH = 48
  ```
  A 1-pixel error on a 64-wide grid translates to `1280 / 64 = 20` pixels on the camera stream and `4000 / 64 = 62.5` pixels on a 12 MP still! Furthermore, scanning inward from the outside caused the scan to halt at the outer table shadow, document drop-shadow, or table edge rather than the true paper boundary.
- **Fix:** Operate candidate generation at 640 px long edge with OpenCV Canny, Otsu segmentation, background distance in Lab, and Line Segment Detection (LSD). High-resolution sub-pixel refinement is executed at 1280x960 (live) and full resolution (capture) using normal-profile derivative scans directed **from inside outward**, guaranteeing selection of the actual paper edge.

---

### Cause 3: Fallback Quadrilateral Extrusion & Bounding Slack
- **File / Location:** `LiveEdgeDetector.kt:150-180`
- **Issue:** When edge lines were not cleanly four-sided, the detector formed rectangular clamped polygons with arbitrary inset/outset bounds.
- **Fix:** Replaced with convex polygon candidate scoring, epsilon-swept `approxPolyDP`, area-minimizing vertex reduction to 4 corners, and strict geometric plausibility scoring.

---

### Cause 4: Warp Transformation Margin & Safety Offset
- **File / Location:** `DocumentProcessor.kt:35-65`
- **Issue:** During perspective warping, coordinates were being approximated with float rect bounds rather than sub-pixel floating-point homography matrices.
- **Fix:** Perspective transformation now applies double-precision `getPerspectiveTransform` directly onto the sub-pixel refined paper corners without adding any margin or dilation.
