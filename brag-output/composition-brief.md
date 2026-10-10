# Hyperframes Composition Brief: Quacky

## Objective
Create a short, polished launch video for Quacky, the pure offline utility suite for Android.

## Output
- Composition directory: `brag-output/composition/`
- Rendered video: `brag-output/brag.mp4`
- Format: landscape — 1920x1080
- Duration: 22 seconds

## Source Material
- Project root: `d:\quacky-app`
- Primary files read: `README.md`, `DECISIONS.md`, `app/src/main/res/drawable/ic_launcher_monochrome.xml`, `Theme.kt`, `Color.kt`, `Type.kt`
- Product name: Quacky
- Tagline / strongest claim: "android.permission.INTERNET is strictly prohibited. 15+ focused tools. Zero network permissions. Zero ads."
- Key UI / visual moments to recreate:
  - Vector geometric duck mascot (`QuackyMark`)
  - Strict monochromatic 5-tone dark palette
  - Manifest inspection pill (`android.permission.INTERNET: NOT FOUND`)
  - AR Ruler 3D spatial coordinate projection (`Distance: 1.84 m`)
  - Document Scanner perspective homography quad cropping
  - Byte-level lossless EXIF scrubbing badge
- Copy that must appear verbatim:
  - "What if your ruler didn't track your location?"
  - "android.permission.INTERNET: NOT FOUND"
  - "15+ offline Android utilities. Zero network permissions."
  - "Because a calculator has no business talking to the cloud."
  - "Quacky"

## Creative Direction
- Tone preset: polished / deadpan
- Creative direction: "minimalist, calm, confident. Monochromatic black-and-white aesthetic with lots of whitespace, clean Satoshi, slow precise motion, subtle fades and gentle easing, no flashy effects. Understated Apple-keynote feel with a dry touch of wit."
- Interpretation: Generous negative space, deep `#0A0A0A` background, pure white and silver typography, slow precise 0.8s fades with smooth easing (`power2.out`), zero neon colors, zero aggressive movement.
- Angle: Brag about what the app refuses to do: zero network calls, zero tracking, backed by unit tests verifying the app cannot talk to the internet.
- Hook: "What if your ruler didn't track your location?"
- Outro / punchline: "Because a calculator has no business talking to the cloud. Quacky. Just utilities."
- Avoid:
  - Generic SaaS language
  - Abstract colorful filler visuals
  - Flashy zooms, camera shakes, or strobes
  - Invented claims not grounded in the repository

## Visual Identity
- Background: `#0A0A0A`
- Cards / Surfaces: `#121212` (elevated: `#1A1A1A`)
- Outline: `#2A2A2A`
- Text: `#F2F2F2` (secondary: `#9A9A9A`, tertiary: `#5E5E5E`)
- Accent: `#FFFFFF`
- Display font: Satoshi (with fallback to modern geometric grotesque sans-serif)
- Body font: Satoshi (with fallback)
- Visual references from the project:
  - Geometric duck mascot vector from `ic_launcher_monochrome.xml`
  - Strict monochromatic tokens from `Color.kt`
  - Android Manifest offline badge from `DECISIONS.md` ADR 001/ADR 012

## Storyboard
Use the storyboard in `brag-output/brag-plan.md` as the creative contract.

Scene summary:
1. **The Question** — 4.5s (0.0s – 4.5s) — Geometric duck mascot fades in; headline "What if your ruler didn't track your location?" settled at 1.0s; subtext "15+ offline Android utilities. Zero network permissions." settled at 1.8s.
2. **The Manifest** — 5.5s (4.5s – 10.0s) — Clean keynote inspection card with `android.permission.INTERNET: NOT FOUND`; three metric columns: 15+ Tools / 0 Network Requests / 0.0 KB Sent to Cloud. Strong cue lock at 8.74s.
3. **The Engine** — 6.0s (10.0s – 16.0s) — Three sleek monochromatic cards: AR Ruler (3D projection 1.84m), Document Scanner (perspective quad cropping), Lossless EXIF Stripper (byte-level APP1 marker removal). Beat lock at 13.11s.
4. **The Outro** — 6.0s (16.0s – 22.0s) — Minimalist duck mark and Satoshi title `Quacky`; punchline "Because a calculator has no business talking to the cloud." settled at 17.5s (strong cue 17.47s); pill "100% Offline · Zero Ads" settled at 18.8s; peaceful hold through 22.0s.

## Audio
- Audio role: Sparse professional accents over an elegant, understated ambient bed.
- Music: `assets/music/happy-beats-business-moves-vol-12-by-ende-dot-app.mp3`
- Music treatment: Base volume 0.20, ducked to 0.12 during voiceover (0.5s–20.2s), soft fade-out in final 1.5s.
- Voiceover: `assets/voiceover.wav` on track 3, volume 1.0, data-start="0.5" data-duration="19.7".
- Music cue guidance: Preset cues at 8.74s, 13.11s, 17.47s.
- SFX files copied:
  - `assets/sfx/interface/drop_001.ogg`
  - `assets/sfx/interface/click_001.ogg`
  - `assets/sfx/impact/impactSoft_medium_000.ogg`
- Audio-reactive treatment: Subtle luminance on card outlines responding smoothly to music RMS.

## Hyperframes Instructions
- Composition root: `<div id="root" data-composition-id="root" data-width="1920" data-height="1080">`
- Set timeline with GSAP paused: true, registered on `window.__timelines["root"]`.
- Add `class="clip"` to timed scene elements with appropriate `data-start` and `data-duration`.
- Ensure all text has strong WCAG contrast (all text `#F2F2F2` or `#FFFFFF` on `#0A0A0A` or `#121212`, secondary text `#9A9A9A` strictly compliant with large/bold sizing).
- Check with `npx hyperframes check` before rendering.
