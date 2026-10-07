# Quacky AR Ruler Accuracy Report

## 1. Test Methodology & Protocol
This accuracy benchmark report evaluates the rebuilt Quacky AR Ruler measurement engine across real-world surfaces and camera distances.

### Test Environment
- **Device Model:** [Device Model, e.g. Pixel 8 Pro]
- **ARCore Version:** [e.g. 1.45.0]
- **Hardware Depth / ToF Sensor:** [Yes / No]
- **Lighting Level:** [Well-lit, ~300-500 lux / Ambient / Low Light]
- **Reference Standard:** Precision steel tape measure (± 0.5 mm)

---

## 2. Accuracy Targets (from Engineering Specification)
- **Median Absolute Error:** ≤ 1.0% (or ≤ 5 mm for distances under 0.5 m)
- **90th Percentile Error:** ≤ 2.0%
- **Without Depth API:** ≤ 2.0% median on planar surfaces, honest lower confidence on non-planar surfaces.

---

## 3. Measured Results Matrix

Each cell represents 5 repeated measurements of a calibrated reference length.

| Surface Type | 0.5 m Distance Error (%) | 1.0 m Distance Error (%) | 2.0 m Distance Error (%) | 4.0 m Distance Error (%) | Primary Hit Source | Confidence Level |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Front Wall** (Matte paint) | | | | | FUSED / PLANE | High |
| **Floor** (Wood / Tile) | | | | | PLANE | High |
| **Ceiling** (Matte) | | | | | PLANE / DEPTH_FIT | High / Med |
| **Laptop** (Top / Screen) | | | | | DEPTH_FIT | High / Med |
| **Sofa** (Fabric / Curved) | | | | | DEPTH_FIT | Medium |
| **Cupboard Front** | | | | | FUSED / PLANE | High |
| **Fridge** (Stainless / Gloss) | | | | | PLANE + EDGE | Medium |
| **Window Frame** (Wood / Metal) | | | | | EDGE SNAP | High / Med |

---

## 4. Summary Statistics
- **Overall Median Absolute Error:** `%`
- **Overall 90th Percentile Error:** `%`
- **Target Met:** `[YES / NO]`

---

## 5. Notes & Observations
- Surface Regularization (PlaneFitter) impact:
- Object Edge Snapping stability:
- Parallax lateral motion gating effectiveness:
- Drift over 20+ second sessions:
