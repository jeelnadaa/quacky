# AGENT PROMPT: Rebuild "Quacky Surfer" as a real 3D endless runner (keep the current UI style)

You are a senior Android + real-time 3D engineer. Quacky (Kotlin, Jetpack Compose, dark monochrome UI, Satoshi font, offline, no INTERNET permission) contains a game tool, **Quacky Surfer**. The current version fakes 3D with flat sprites on a fake-perspective plane and has many problems (section 1).

**Forget the current game implementation.** Do not reuse its renderer, sprite logic, spawner, collision or camera code, and do not patch it. Delete it and **rebuild the game from this document**. All numbers, rules and algorithms are specified, so implement them instead of inventing your own. Where this document says **verify**, check the exact API names in the current docs and adapt names only, not the intent.

**Keep (the user likes this UI):** the Quacky tool shell (top bar with back arrow, title "Quacky Surfer", pin, help (?), overflow), the dark monochrome look, Satoshi typography, the HUD layout (score left, breadcrumb pill, distance, pause), the rounded dark "CRASHED!" card with white "Play Again" button, the "Breadcrumbs" yellow accent. Everything inside the game viewport becomes real 3D.

**Hard rules:** fully offline (no network, no analytics, no INTERNET permission). No per-frame allocations in the game loop. All gameplay numbers in one file `SurferTuning.kt`.

## 1. What is wrong today (found by watching the screen recording) and where each fix is

| # | Problem seen in the video | Fix |
|---|---|---|
| 1 | The duck is a flat, blurry 2D sprite that faces the camera. It looks like a sticker, does not look like it is running, and has no run, jump or slide animation. | Real 3D duck model with animations (sections 2, 6) |
| 2 | Obstacles (train, barrier, overhead duct) and coins are flat sprites. Coins are blurry yellow glow discs that overlap objects. | 3D props + emissive coins with real bloom (sections 2, 5) |
| 3 | The track floats in a black void: hard cut at the bottom edge, abrupt dark band at the horizon, no walls, rails, lamps or buildings, no sense of speed. | Environment chunks, fog, lights (sections 2, 5, 8) |
| 4 | The game only uses the middle ~55% of the screen; large dead black areas above and below. | Full-bleed 3D surface behind the HUD (section 12) |
| 5 | The camera skews: when the duck changes lane the whole track tilts and the vanishing point swings, while the horizon band stays fixed. Disorienting. | Fixed-yaw, no-roll chase camera with a smoothed X follow (section 5.3) |
| 6 | Coins are placed on top of barriers and trains or inside obstacles, and stray coins appear below the track in the void after they pass the player. | Spawner rules + validator + culling (section 8) |
| 7 | Possible unfair patterns (blocked rows) and a train appears right at the start (9 m at 1 s). | Fair pattern templates, safe-path validator, calm first 45 m (section 8) |
| 8 | No ready/countdown: the run starts instantly. | Ready + 3-2-1 countdown (section 10) |
| 9 | Lane changes look like teleports; no lean, no jump/slide pose, no crash animation (instant stop). | Tweened lane change + lean, jump/slide poses, tumble crash (sections 6, 10) |
| 10 | Stray thin white vertical lines beside the duck (debug markers or hitbox lines) and an offset blob shadow. | Remove all debug drawing in release; proper blob shadow under the duck (section 5.5) |
| 11 | The crash text says "overhead duct" but the obstacle looks like a gate; the currency is "Breadcrumbs" but the pickup looks like a generic coin; the unlabeled gamepad icon in the HUD is unclear. | Consistent naming and art; remove the gamepad icon (sections 2, 12) |
| 12 | Score logic is unclear (20 points per coin only; distance separate). | Defined scoring (section 9) |

## 2. Provided 3D assets (use them as they are)

Folder `quacky_surfer_assets/` (attached next to this prompt). Copy `models/*.glb` to `app/src/main/assets/surfer/`. Do not edit the GLBs by hand. They were generated and validated with the Khronos glTF validator (0 errors). The generator `tools/make_assets.py` (Python 3 + numpy) can regenerate them if a number must change. `previews/` shows how they look. `ANIMATIONS.md` lists the clips.

**Conventions:** meters, +Y up. The **duck faces −Z** (away from the camera, no rotation needed). Trains, barriers and ducts have their front toward **+Z** (toward the player). Colors are PBR with low metallic values (no reflection map is needed).

| File | What | Origin | Size (x × y × z, m) |
|---|---|---|---|
| `quacky_duck.glb` | The Quacky duck (white, orange hard hat with yellow headlamp, cyan headband with two trailing tails). 3.3k triangles, 8 clips | feet center, on the ground | ≈ 0.75 × 1.06 × 1.0 (standing, with wings) |
| `train.glb` | Dark train car, blue windshield and side windows, yellow stripe, headlights | front-bottom-center; body extends toward −Z | 1.48 × 2.84 × 11.0 |
| `barrier.glb` | Low hazard barrier (yellow with black diagonal stripes, orange beacons) | bottom-center | 1.56 × 0.89 × 0.32 |
| `duct.glb` | Overhead duct on two posts: metal body, red cap, hazard strip, yellow sign with a down chevron | bottom-center | 1.9 × 1.74 × 0.56; duct body spans y 0.82-1.32 |
| `breadcrumb.glb` | Golden breadcrumb coin, upright, facing ±Z, with a `spin` clip | center of the coin | 0.56 × 0.56 × 0.08 |
| `blob_shadow.glb` | Flat translucent dark disc for the duck's ground shadow | ground center | 1.0 × 1.25 (x × z) |
| `env_chunk_a/b/c.glb` | 24 m track chunk: dark floor, lane lines, tile lines, side walls with yellow light strips, pillars with lamps, night skyline | near-end center on the ground; extends toward −Z by 24 m | 70 × ~25 × 24 |

**Duck clips** (names exact): `rest` (0 s, restores the bind pose), `run` (0.5 s loop), `idle` (1.6 s loop), `jump` (0.7 s), `slide` (0.7 s: eases in 0-0.2 s, hold pose 0.2-0.5 s, eases out 0.5-0.7 s), `lean_left` / `lean_right` (0.3 s, only the `LeanPivot` node), `crash` (0.9 s, ends lying on its back, hold the last pose). Coin clip: `spin` (1.2 s loop).

**Colliders (gameplay boxes; the visual models are slightly larger, this is intentional for forgiving collisions):**

| Object | Collider (AABB, relative to origin) |
|---|---|
| Player standing / jumping | x ±0.28, y 0 to 0.95, z ±0.25 |
| Player sliding | x ±0.28, y 0 to 0.62, z ±0.25 |
| Train | x ±0.70, y 0 to 2.80, z from −11.0 to 0.0 |
| Barrier | x ±0.75, y 0 to 0.80, z ±0.16 (must be jumped) |
| Overhead duct | bar: x ±0.92, y 0.82 to 1.32, z ±0.28 (must be slid under); posts: x from 0.80 to 0.92 and −0.92 to −0.80, y 0 to 1.74 |
| Breadcrumb | sphere radius 0.38 at y 0.75 (ground) or y 1.45 (high) |

## 3. Tech stack (3D engine)

Use **Google Filament** (Apache-2.0, offline, made for Android): `filament-android`, `gltfio-android`, `filament-utils-android` (**verify** latest stable versions on Maven Central). It renders glTF/GLB with PBR lighting, node animations, shadows, bloom, fog and MSAA. Add its ProGuard keep rules. Keep ABI splits.

- Host the 3D view in a `SurfaceView` (`GameSurfaceView`) inside `AndroidView`, **full-bleed behind the Compose HUD**. Drive frames with `Choreographer`. Use Filament's `UiHelper` for the surface lifecycle.
- If the Filament engine cannot be created (device lacks OpenGL ES 3.0), show the standard missing-requirement message: "Your phone can't run the 3D engine this game needs." No fallback renderer.
- Lifecycle: pause the loop on `ON_PAUSE` (and auto-pause the game), resume on `ON_RESUME`, destroy engine, assets, textures and entities on dispose, handle surface re-creation without crashing.

Skeleton to follow (**verify** names against the Filament `gltf-viewer` sample and current docs):
```kotlin
val engine = Engine.create()
val renderer = engine.createRenderer()
val scene = engine.createScene()
val view = engine.createView().also { it.scene = scene }
val cameraEntity = EntityManager.get().create()
val camera = engine.createCamera(cameraEntity).also { view.camera = it }
val assetLoader = AssetLoader(engine, UbershaderProvider(engine), EntityManager.get())
val resourceLoader = ResourceLoader(engine)
// load once, then make N instances for pooled objects
val asset = assetLoader.createInstancedAsset(bytes, instancesArray)   // instances for coins/obstacles
resourceLoader.loadResources(asset)
val animator = asset.instance.animator    // per-instance animator for the duck
animator.applyAnimation(clipIndex, timeSeconds); animator.updateBoneMatrices()
```
Update transforms in one batch per frame with `TransformManager` (`openLocalTransformTransaction()` / `commitLocalTransformTransaction()`). Add/remove entities from the scene instead of moving them far away.

## 4. World, coordinates and the loop

- **Axes:** +X right, +Y up, forward = −Z. The player stays at z = 0. The **world scrolls toward the camera (+Z)**; objects have a distance-ahead `d` and `z = −d` relative to the player. Distance traveled `dist` (m) accumulates `speed × dt`.
- **Lanes:** 3 lanes at x = −1.6, 0, +1.6 (lane width 1.6 m; track half-width 2.4 m).
- **Fixed timestep:** 1/120 s with an accumulator (max 5 steps per frame). Render uses interpolation for the player and camera. Never tie gameplay to frame rate.
- **Chunks:** a ring of 8 env chunks (24 m each, cycle a/b/c randomly, never the same one twice in a row), placed back-to-back along −Z. Recycle a chunk to the far end when its near edge is more than 10 m behind the camera.
- **Pools (preallocated, no creation during play):** 140 breadcrumbs, 14 barriers, 14 ducts, 10 trains, 1 duck, 1 blob shadow. Objects are removed from the scene when more than 8 m behind the camera (**this also fixes stray coins in the void**) and returned to the pool.
- **Speed:** `speed(t) = min(22, 10 + 0.07·t)` m/s with `t` = seconds since the countdown ended. Reset every run.

## 5. Scene, camera, lights and look (keeps the dark monochrome style)

### 5.1 Palette and mood
Night rail-yard: near-black background `#0A0A0A`, charcoal floor, white duck, yellow accents (breadcrumbs, light strips), blue train windows. No colorful gradients.

### 5.2 Lights and post-processing (**verify** option class names)
- One **sun** directional light: color (1.0, 0.97, 0.92), intensity ≈ 60 000 lux, direction (−0.35, −0.9, −0.25), casts shadows on High quality only.
- **Ambient:** an `IndirectLight` built from a 1-band spherical harmonic with color (0.30, 0.32, 0.38) and intensity ≈ 25 000, so the dark side of objects stays readable. No reflection texture is needed (materials are low-metallic).
- **Tone mapping:** ACES. **Bloom** (strength ≈ 0.35, threshold on, 6 levels) so emissive coins, light strips, windows and headlights glow for real (this replaces the fake blurry halo sprites). **Anti-aliasing:** MSAA 4× on High, FXAA on Medium, none on Low. **Fog:** color `#0A0A0A`, starts at 25 m, fully opaque by ~110 m, so the horizon dissolves smoothly instead of ending in a hard band. Set the camera far plane to 140 m.
- Background/clear color `#0A0A0A`.

### 5.3 Camera (fixes the skewing track)
- Perspective, vertical FOV **58° at speed 10 m/s**, easing to **66° at 22 m/s** (speed feel). Near 0.1, far 140. Ensure horizontal FOV ≥ 54° on wide screens (tablets/landscape) by adjusting vertical FOV.
- Position: `x = player.x × 0.45` (critically-damped spring, ω = 10), `y = 2.55`, `z = +4.3`. **Look-at** target: `x = camera.x` (same X, so **zero yaw**), `y = 0.9`, `z = −7.0`. **No roll, no yaw**, only slight pitch down (~8°). The track must never visibly skew or tilt.
- On crash: camera shake for 0.25 s (amplitude 0.06 m, decaying), no roll.
- During the ready screen: camera slightly lower (y 2.0) and closer (z 3.4) with a 1 s ease into the play position when the countdown ends.

### 5.4 Environment and sense of speed
Chunks give pillars every 6 m, light strips and skyline, so motion is obvious. Also: FOV kick with speed (above), coin spin, slight bob of the camera y (±0.02 m at the run cycle rate), and duck run playback rate = `clamp(speed / 10, 0.9, 1.8)`.

### 5.5 Shadow
High quality: real sun shadow for the duck only. Medium/Low: the `blob_shadow.glb` disc under the duck (scale 1.0 grounded; shrinks to 0.6 and fades at max jump height). Never draw debug lines or hitboxes in release builds (**the stray white lines in the old version**).

### 5.6 Quality tiers (auto)
`High` (shadows, bloom, MSAA 4×), `Medium` (bloom, FXAA, blob shadow), `Low` (no bloom, no AA, blob shadow, render scale 0.75 via dynamic resolution). Start on High. If the average frame time over 2 s exceeds 20 ms, drop one tier (never go back up in the same run). Setting in the overflow menu: Auto / High / Medium / Low.

## 6. Player (`PlayerController`)

**State machine:** `GROUNDED_RUN`, `JUMPING`, `SLIDING`, `CRASHED`, `IDLE_READY`. Lane changes are an overlay (they can happen in any moving state).

**Numbers (`SurferTuning`):**
- Lane change: 0.12 s, smoothstep from current x to the target lane x. Plays `lean_left` / `lean_right` at the same time (overlay on `LeanPivot`). A new lane input during a change is buffered and applied when the change is ≥ 60% done.
- Jump: `v0 = 9.5 m/s`, `g = 28 m/s²` (apex ≈ 1.61 m, air time ≈ 0.68 s). Playing `jump` clip over the real air time. **Fast-fall:** swipe down in the air sets `vy = −16`, and the duck slides right after landing.
- Slide: duration 0.65 s. Swipe up during a slide cancels it and jumps after 80 ms. Collider switches to the sliding box for the whole slide.
- Input buffering: remember the latest jump/slide/lane input for 120 ms and apply it as soon as it is legal.
- Landing: tiny squash is optional; just resume `run`.

**Animation application each frame (nodes-only animation, no skins):**
1. `animator.applyAnimation(restIndex, 0f)` to reset every animated node.
2. Apply the base clip at its time (`run` looping with the playback rate above, `jump`, `slide` with the hold mapping, `crash`, or `idle`).
3. Apply the lean clip on top, if active (it only touches `LeanPivot`, so it never conflicts with the base clip).
4. `animator.updateBoneMatrices()`.
Slide time mapping: map elapsed slide time `e ∈ [0, 0.65]` to clip time: `0-0.2 s → 0-0.2`, hold `0.2-0.45 s → 0.2-0.5`, `0.45-0.65 s → 0.5-0.7`.

The crash plays `crash` once and holds the last pose; the duck stays where it hit.

## 7. Input

- **Swipes:** left/right changes lane, up jumps, down slides/fast-falls. Trigger **as soon as** the drag passes 36 dp (or velocity > 900 dp/s) on the dominant axis, once per touch, without waiting for finger-up.
- Exclude the system back gesture on screen edges while playing: `Modifier.systemGestureExclusion()` over the game area (the platform caps this; use the allowed height).
- Arrow keys and WASD for emulator/keyboard testing (debug and release is fine).
- HUD buttons (pause, top bar) consume their own touches so they never trigger swipes.
- Haptics: very light tick on lane change (rate-limited), light tick on breadcrumb pickup (max 8/s), heavy click on crash. Respect the Settings toggle.

## 8. Spawner: fair patterns (`RowPlanner`)

### 8.1 Rows
The track ahead is generated as **rows**, each a 3-character string, nearest lane left to right (L C R). Rows are spaced by `rowGap = max(7.0 m, speed × 0.55 s)` along the road. Spawn rows until 110 m ahead of the player.

Cell codes: `.` empty, `c` ground breadcrumb, `h` high breadcrumb (y 1.45), `B` barrier (jump), `D` overhead duct (slide), `T` train front (a train then occupies 11 m of road ahead of its front, in that lane).

### 8.2 Templates (rows are listed nearest first)
| id | rows | weight tiers |
|---|---|---|
| `coin_center` | `.c.` `.c.` `.c.` `.c.` `.c.` | all |
| `coin_left` | `c..` `c..` `c..` `c..` | all |
| `coin_right` | `..c` `..c` `..c` `..c` | all |
| `coin_zigzag` | `c..` `.c.` `..c` `.c.` `c..` | all |
| `coin_stairs` | `c..` `c..` `.c.` `.c.` `..c` `..c` | all |
| `barrier_center` | `...` `.B.` `...` | all |
| `barrier_pair` | `B..` `..B` `...` | after 150 m |
| `slalom` | `B..` `.B.` `..B` `.B.` `B..` | after 150 m |
| `duct_center` | `.D.` `.c.` `...` | all |
| `duct_pair` | `D..` `..D` `.c.` | after 150 m |
| `train_left_coins` | `Tc.` `.c.` `.c.` `...` | after 150 m |
| `train_right_coins` | `.cT` `.c.` `.c.` `...` | after 150 m |
| `train_center_sides` | `.T.` `c.c` `c.c` `...` | after 150 m |
| `two_trains_gap` | `T.T` `.c.` `.c.` `...` | after 400 m |
| `barrier_wall` | `BBB` `...` `c.c` | after 400 m |
| `duct_wall` | `DDD` `...` `.c.` | after 400 m |
| `train_barrier_mix` | `T.B` `...` `.c.` | after 400 m |
| `duct_then_barrier` | `.D.` `...` `B..` `.c.` | after 400 m |

Add an automatic **coin arc** over every `B`: three high breadcrumbs (`h`) in the same lane at −1.2 m, 0, +1.2 m along the road, at heights 1.0, 1.45, 1.0 (rewards jumping). Breadcrumbs spacing in lines is 1.3 m along the road.

Selection: weighted random from the allowed set for the current `dist`. **The first 45 m contain only coin templates** (calm start). Never place a train closer than 25 m to the start.

### 8.3 Placement rules (cells are skipped if they would break these)
- A breadcrumb is never placed inside or touching an obstacle collider, including a lane that is covered by a train's 11 m range. (`h` over a `B` is allowed; `h`/`c` over `T` or `D` is not.)
- Obstacles are never placed on top of each other.

### 8.4 Fairness validator (`RowPlanner.validate`) — mandatory
Treat the road as a list of rows with a distance. For each row compute each lane's state: `FREE`, `B`, `D`, or `TRAIN` (a train in that lane is blocking every row whose distance lies within its 11 m range).
- A row is **passable** if at least one lane is `FREE`.
- A row with no `FREE` lane is passable **only if** all its non-`TRAIN` lanes are `B` (jump row) **or** all are `D` (slide row), and at least one such lane exists. Mixed `B`+`D`, or all lanes `TRAIN`, is **invalid**.
- Keep a set of lanes `R` where the player could be after the previous row (start `{0,1,2}`). For each row, the allowed lanes are `FREE` lanes (plus `B` lanes for a jump row, `D` lanes for a slide row). The new `R` is the allowed lanes reachable from `R` with a lane shift of at most `1` if the time gap to the previous row is under 0.5 s, otherwise at most `2`. If the new `R` is ever empty, the pattern is **invalid**.
- Two action rows (jump or slide) must never be consecutive: keep at least one row with a `FREE` lane between them.
Generate the next template, validate it **together with the pending rows already queued and the last emitted row**; retry up to 8 times with a different template; fall back to `coin_center`. Unit-test with 10 000 generated rows at different speeds: zero invalid sequences.

## 9. Scoring, breadcrumbs and persistence

- **Score** = `floor(distance in meters) + 20 × breadcrumbs collected this run`. Show the score live (large, left), breadcrumbs in the yellow pill, distance in meters.
- Pickup: when the duck's box overlaps a breadcrumb sphere, play a 0.12 s scale pop (1 → 1.5) then remove it; pulse the HUD pill; light haptic.
- Persist with DataStore: `bestScore`, `bestDistance`, `breadcrumbWallet` (total, no shop in this version). If the old game stored values, **migrate** them once.
- Each finished run is saved to Quacky's **history** (same history system as the other tools): title `Run · 37 m · 80 pts`, subtitle with breadcrumbs and date, payload with score/distance/breadcrumbs/seed. Tapping a history entry opens the game screen's result card read-only. "Best score" filter works like favorites.

## 10. Game states and screens

1. **Ready:** duck plays `idle`, camera in ready position, text "Swipe to move" with a tiny arrow row on the first run ever (and after "Reset tips"), then a **3-2-1 countdown** (1 s each, large white numerals with Satoshi, light haptic per tick). The world is stationary during the countdown.
2. **Running:** gameplay as above.
3. **Paused:** pause button, app background, or incoming overlay. Dim overlay with Resume and Quit. Resuming gives a 1 s "get ready" slow-in (speed ramps from 0 to current).
4. **Crashing (0.9 s):** world speed eases to 0 over 0.4 s, `crash` plays, camera shake, heavy haptic. Input is ignored.
5. **Game over card** (keep the existing visual style): title "CRASHED!", a reason line that matches the obstacle (train: "Hit a train! Dodge to the side."; barrier: "Hit a barrier! Swipe up to jump."; duct: "Hit an overhead duct! Swipe down to slide."), then Score, Breadcrumbs (+N, yellow), Distance, **Best** and a "NEW BEST" chip when applicable, **Play Again** (white button) and a text button "Exit". Appears after the crash animation. Play Again restarts within 0.3 s with no flash of old objects (clear pools first).

## 11. Audio

Keep the existing audio assets and logic if present, but rebind them to the new events (pickup, jump, slide, lane change, crash, button). Respect audio focus and the sound toggle in the overflow menu. If there is no audio code, add none in this task.

## 12. HUD and layout (keep the style, fix the wasted space)

- The 3D surface is **edge-to-edge** (draw behind status/navigation bars; use window insets only for HUD padding). The top bar (back, "Quacky Surfer", pin, help, overflow) overlays the 3D view with a transparent-to-dark scrim so text stays readable.
- Row under the top bar: score (large, left), breadcrumb pill (yellow dot + count), distance in small `textSecondary`, pause button (right). **Remove the unexplained gamepad icon.**
- Overflow menu: Sound, Haptics, Graphics quality, Show swipe hints, Reset tips, Reset best score (confirm).
- Help (?) opens the **how-to guide** (section 7C style: stepper with a visual per step): 1 swipe left/right to change lane, 2 swipe up to jump (barriers), 3 swipe down to slide (overhead ducts), 4 collect breadcrumbs, 5 trains: change lanes. The visuals are simple Canvas animations (finger swipe on a phone outline with the duck), consistent with the other tools.
- Pin works like other tools; the tool appears in the Random/Games grouping decided by the existing registry (keep its current category).

## 13. Performance and quality budgets

- 60 fps on a mid-range phone on Medium; High may drop to 45+. Frame time budget: game update ≤ 2 ms, render submission ≤ 4 ms CPU.
- At most ~150 draw calls (each prop is 1-3 primitives; chunks are 6-8). No allocations in the loop (preallocated float arrays/matrices, no boxing, no lambdas created per frame). Monitor GC in debug builds and fail the smoke test if a GC pause > 10 ms occurs during play.
- Memory under 250 MB. Release every Filament object on dispose.
- Cold load of all assets under 600 ms on a mid-range phone (load GLBs on a background thread, then create instances on the render thread).

## 14. Debug tools (debug builds only)

Overlay with fps, frame ms, entity counts, speed, quality tier. Toggles: god mode (no collisions), slow-motion (×0.25), show colliders (draw AABB wireframes using a debug-only line renderable), force a template by id, seed the spawner. **None of this may exist in release.**

## 15. Tests

- **Unit (JVM):** `RowPlanner` fairness (10 000 sequences at speeds 10, 16, 22 m/s, zero invalid); coin/obstacle overlap rule; jump physics (apex 1.61 ± 0.02 m, air time 0.68 ± 0.02 s); slide/duct clearance (sliding box passes the duct, standing box does not, jumping does not); barrier clearance (a jump started 0.2 s before clears it, one started 0.05 s late does not); collision AABB tests; speed curve; scoring; pool recycling (no entity leaks over 5 000 spawns); lane-change smoothstep; swipe classifier.
- **Instrumented:** launch the game, run a scripted 30-second bot (random valid swipes), assert no crash, frame-time p95 < 20 ms on the test device, no stray entities left in the scene after a crash and restart, and the Filament engine is destroyed on exit.
- **Visual checklist on a real device** (record a screen video and compare with section 16).

## 16. Acceptance (each item maps to the problems in section 1)

1. The duck is a lit 3D model seen from behind/above, runs with a visible leg and wing cycle, leans on lane changes, jumps with tucked feet and raised wings, slides low under ducts, and tumbles onto its back when crashing. (1, 9)
2. Trains, barriers, ducts and breadcrumbs are 3D, lit, and cast no fake halos. Breadcrumbs spin and glow via bloom. (2, 11)
3. The track has walls, light strips, pillars and a skyline, fades into fog at distance, and shows clear sense of speed. No hard edges at the top or bottom of the track. (3)
4. The 3D view fills the whole screen behind the HUD; no dead black areas. (4)
5. During lane changes the track and horizon do not tilt, skew or swing. (5)
6. No breadcrumb overlaps an obstacle (except high arcs over barriers), nothing appears in the void after passing the player, and a 5-minute bot run never produces an unfair row. (6, 7)
7. A 3-2-1 countdown precedes every run; the first 45 m contain only breadcrumbs. (7, 8)
8. No white debug lines anywhere in release builds; the duck has a soft shadow. (10)
9. The crash card text matches the obstacle that was hit ("train", "barrier", "overhead duct"), the card shows Best and "NEW BEST", and the gamepad icon is gone. (11)
10. Score = distance + 20 per breadcrumb, shown consistently in HUD, card and history. (12)
11. Runs are saved in Quacky's history, best score persists across restarts, and the game works fully offline in airplane mode.
12. On a phone without OpenGL ES 3.0 the honest missing-requirement message appears instead of the game.

## 17. Build order

1. Remove the old game rendering/sprites/spawner/collision code (keep the tool shell, route, registry entry, HUD composables you will restyle).
2. Add Filament, `GameSurfaceView`, lifecycle, engine/scene/camera/lights/fog/bloom, load `env_chunk_*.glb` and show a scrolling environment with the camera from section 5.3.
3. Load the duck, play `idle` and `run`, add the `rest` + base + lean animation application, blob shadow, quality tiers.
4. Input (swipes + keys), `PlayerController` physics, lane changes, jump, slide, animation mapping.
5. Pools, props (train, barrier, duct, breadcrumb), colliders, collisions, crash sequence.
6. `RowPlanner` templates, spawner, validator, unit tests.
7. Game states (ready/countdown/running/paused/crashed/game over), HUD restyle, full-bleed layout, scoring, persistence, history entries.
8. Haptics, audio rebind, help guide, overflow menu, graphics quality setting.
9. Performance pass (allocations, GC, frame time), instrumented bot test, remove/guard debug tools, README section for the game.
After each step run on a real device, compare against section 16, and summarize before continuing.

## 18. Do not

- Do not reuse any old sprite/fake-perspective rendering or old spawner logic.
- Do not use 2D billboards or glow-disc sprites for coins or props.
- Do not roll or yaw the camera with lane changes.
- Do not add network features, ads, analytics, or an in-app shop in this task.
- Do not modify the provided GLB files by hand; regenerate with `tools/make_assets.py` if a dimension must change.
- Do not leave debug drawing, god mode, or logs enabled in release builds.
