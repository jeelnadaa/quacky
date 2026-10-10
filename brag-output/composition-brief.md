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

## Storyboard & Layout Architecture
- **Layout Structure**:
  - Asymmetric split screen: Phone mockup vertically centered in the right third of the 1920x1080 canvas (right: 170px, width: 384px).
  - Kinetic typography vertically centered in the left column (left: 140px, width: 820px, >= 120px margin from canvas edge).
  - Maximum 2 lines on screen at once, 2-4 words per line, 68px to 94px bold typography in pure white.
  - Safe area: Nothing within 90px of any canvas edge.
- **Motion & Camera System**:
  - Continuous camera push-in & ambient background grid parallax drift throughout 20s.
  - 3D spring entrance (`back.out(1.3)`) with perspective tilt settling to flat.
  - Shared-element scale morphs between app screens.
  - Feature emphasis beats: AR Ruler 1.84m chip zoom + spatial caliper callout; Document Scanner homography quad zoom + callout.
  - Outro hold: >= 1.5s confident hold on Quacky logo, tagline, and offline badge.

## Audio & Waveform Synchronization
- Narration generated as individual phrase WAV clips via Kokoro-82M TTS and measured for exact waveform onsets:
  - Phrase 1 (0.60s): "Meet Quacky." (onset: 34ms)
  - Phrase 2 (1.95s): "A pure offline utility suite for Android." (onset: 30ms)
  - Phrase 3 (5.20s): "Measure real spaces" (onset: 36ms)
  - Phrase 4 (6.85s): "with precision AR spatial math." (onset: 54ms)
  - Phrase 5 (9.80s): "Scan & crop documents." (onset: 32ms)
  - Phrase 6 (11.65s): "completely on-device." (onset: 74ms)
  - Phrase 7 (14.20s): "Zero network permissions." (onset: 51ms)
  - Phrase 8 (16.05s): "Your phone already has everything it needs." (onset: 55ms)
- Background music sidechain-ducked by ~12 dB during speech intervals with 300ms fades.
- Precision UI audio cues:
  - Tap SFX (`click_001.ogg`) at 4.40s and 13.40s
  - Scene morph SFX (`drop_001.ogg`) at 4.75s and 9.35s
  - AR caliper lock SFX at 7.25s
  - Outro impact resolve (`impactSoft_medium_000.ogg`) at 13.85s
- Measured audio/subtitle offset across test frames: < 25ms (well within +/-100ms tolerance).

