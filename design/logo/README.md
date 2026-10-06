# Quacky Brand & Logo System

## 1. Mascot Concept
The Quacky mascot is a minimalist, geometric duck embodying the app's philosophy:
- **Quiet & Clean:** Stripped of unnecessary decoration, gradients, shadows, or bright colors.
- **Grayscale Only:** Rendered in `#F2F2F2` (white-primary) on `#0A0A0A` (background-dark).
- **Geometric Construction:** Built from basic ellipses and circles. The head is a circle, the body an elongated ellipse smoothly sweeping into a gentle upturned tail, and a rounded geometric beak.
- **Expression:** Friendly, intelligent, and calm.

## 2. Clear Space Rule
- **Minimum Clear Space:** Equal to half the head diameter ($0.5 \times D_{head}$, approximately $36\text{dp}$ on a $108\text{dp}$ canvas).
- No UI elements, typography, or edges should invade this perimeter.

## 3. Minimum Sizing & Legibility
- **Adaptive Launcher Icon:** Safe zone within central 66dp of a 108dp canvas (`ic_launcher_foreground.xml`).
- **In-App Navigation / Empty State Mark:** Down to 24dp $\times$ 24dp silhouette without loss of silhouette distinction.
- **Themed Icon:** Supported via `ic_launcher_monochrome.xml` for Android 13+ Material You themed icons.

## 4. Deliverables
1. `quacky_mark.svg`: 512×512 standalone mark.
2. `quacky_wordmark.svg`: Scaled mark alongside vectorized "Quacky" wordmark.
3. `ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_launcher_monochrome.xml`: Adaptive icon layers.
4. `ic_launcher_512.png`: 512×512 full-bleed Play Store icon.
