# Quacky Surfer 3D assets

- `models/*.glb`: copy to `app/src/main/assets/surfer/` (glTF 2.0, validated with the Khronos glTF validator, 0 errors/warnings).
- `ANIMATIONS.md`: clip names and durations (names are exact).
- `previews/`: software-rendered previews (flat shaded; in-engine look will be smoother and lit with PBR).
- `tools/make_assets.py`: regenerates every model. Needs Python 3 + numpy. Usage: `python3 make_assets.py out_dir`.
- Conventions: meters, +Y up. Duck faces -Z. Trains/barrier/duct fronts face +Z. Duck is animated by node transforms (no skins).
