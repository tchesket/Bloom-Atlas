# Bloom Atlas — Version History

## Version 24.0 (`versionCode = 24`)
**Date:** 2026-11-26

- **Refined Post-Squeeze Peak Leg Pulses (`InteractiveVulva3DCanvas.kt`)**
  - Preserved the dramatic initial thigh-squeeze slam (`-0.84f` spread / `-1.12f` knee squeeze, `260ms` clinch hold) and its timing against Blossom's head dip and side tilt while refining only the subsequent leg pulses during the 16-second peak episode.
  - Replaced the uniform symmetric post-squeeze leg drift loops with a curated asymmetric pulse sequence (`buildLaterPeakLegPulses`) featuring smaller partial reopenings, unequal left/right travel distances, staggered return timing (`120–395ms` lead/lag between knees), and two distinct strong later surges (breaking any predictable even fade).
  - Added a dedicated final easing phase (`PEAK_LEG_FINAL_EASE_MS = 2400ms`) that fades out leg tremor and smoothly eases both legs and knees directly into the resting pose (`0f`) with a subtle trailing-knee stagger right as the episode finishes, remaining 100% compatible with the active "Ask Blossom to Spread" (`selfSpreadPose = 1f`) arm/hand IK pose.

---

## Version 23.0 (`versionCode = 23`)
**Date:** 2026-11-26

- **Beta Option #7 Promoted to Default ON (`BlossomViewportSessionState.kt`)**
  - Enabled **Vestibule-Labial Patch Alignment** (`betaV3MedialRootContinuity`) by default at 75% strength while keeping it independently toggleable in Beta Lab.
- **Heart Rate HUD Test Trigger Button (`InteractiveVulva3DCanvas.kt`)**
  - Added a compact **"Trigger 136"** button directly underneath the optional top-left Heart Rate HUD pill that immediately sets heart rate to `136 BPM` and clears refractory/cooldown timers so the full peak sequence can be tested on demand.
- **Refined 8s Pre-Event & 16s Peak Sequence Choreography (`InteractiveVulva3DCanvas.kt`, `BlossomSdfBodyBuilder.kt`, `BlossomUpperBodyBuilder.kt`, `BlossomArmIkRig.kt`, `VulvaGeometryBuilder.kt`)**
  - **Extended Timings & Natural Pacing:** Extended the pre-event build-up to **8 seconds** (autonomous `136 -> 152 BPM` climb + ramping continuous vibration) and the main peak event to **16 seconds**, while slowing peak head movements by **60%** (`2.5×` durations) and leg movements by **40%** (`1.67×` durations).
  - **Desynchronized, Restless Pre-Event Legs:** Replaced the synchronized back-and-forth sway with independently phased, higher-frequency restless leg and knee movements that draw closer together early and trend progressively wider into the wide stance at `t = 8s`.
  - **Tighter Thigh-Squeeze Slam & Planted-Foot Knee Shifting:** Added a dedicated thigh/knee lateral rig with a C1 soft-tissue midline barrier (feet remain 100% planted while knees and upper legs shift) so the initial slam genuinely squeezes her thighs and knees flush together without interpenetration, and the 16-second event naturally mixes planted-foot knee shifting with whole-leg sweeps and shaking.
  - **Mouth Opening & Subtle Extremity Realism:** Added a precomputed anatomical mouth-opening morph (lips parting, mandible drop, recessed darkened oral cavity) that opens right at the start of the 16-second event with organic breath modulation, alongside subtle independent toe wiggling (with synced toenails), subtle finger curling (with synced fingernails), and gentle length-preserving 2-bone arm shifting.

---

## Version 22.0 (`versionCode = 22`)
**Date:** 2026-11-26

- **Replaced Beta Option #7 — Vestibule-Labial Patch Alignment (`VulvaGeometryBuilder.kt`, `VulvaGlRenderer.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Root-cause audit confirmed the persistent pale, body-skin-colored bands at the inner labia–vestibule junction were literally the exposed underlying `blossomVulvaPatchMesh` showing cutaneous skin coloration (`mucosa = 0.14–0.42`) because `vestEllipse` was narrower and out of phase with `labiaHoodArchMesh`'s actual attachment centerline (including spread, asymmetry, and organic meander) while deep `innerTuck` and `innerRootAlpha` exposed it (and v21.0's `rootRimBand` mask was mathematically subsumed by `vestibuleColorMask`, producing `0.0` change on the patch).
  - Replaced Option #7 (still OFF by default, `75%` slider default with toggle memory) to align the underlying vulva-patch mucosal/wetness/AO/pigment footprint directly to `labiaHoodArchMesh`'s exact attachment centerline (feathering cleanly under the opaque arch core with zero bleed onto outer labia majora, mons, or perineum), sync `labiaHoodArchMesh` ground height to the active beta anterior surface LUT, tighten medial `innerTuck`, and smoothly fade arch-only outer-skirt shading so both medial and outer boundaries merge seamlessly.
- **Redesigned 136 BPM 3-Phase Peak Exertion Sequence (`InteractiveVulva3DCanvas.kt`, `AnatomyHapticsEngine.kt`, `BlossomSdfBodyBuilder.kt`, `VulvaGlRenderer.kt`)**
  - **136 BPM Trigger & 7-Second Pre-Event Delay:** Triggers at **136 BPM** (down from 140 BPM) with a 7-second build-up before the main event: the Heart Rate HUD pill turns **emerald green and slowly pulsates** (when visible), heart rate autonomously climbs steadily from **136 -> 152 BPM** regardless of user touch, continuous phone vibration ramps smoothly from low to high intensity, and her legs sway slowly side-to-side with growing amplitude (animating closer together than the manual `-0.30f` limit even from max placement) before opening into a wide stance at `t = 7s`.
  - **12-Second Dramatic Peak Event:** Extended from 10s to **12 seconds** with much more dramatic irregular movements across legs, head, breathing, and blinking: legs slam tightly shut (`-0.94f`, verified zero midline interpenetration) -> slowly part open to default (`0f`) over ~1 second -> shake independently AND sweep side-to-side asymmetrically; eyelids close for **4 full seconds** before opening into rapid irregular peak blinking; head throws deeply back with wider erratic turns/rolls and heavy irregular breathing; and vibration switches to high-intensity, high-frequency, slightly irregular pulsing for the full 12 seconds (with safe cancellation on exit/pause/dispose).
  - **125 BPM Post-Peak Cooldown Floor:** At the end of the 12 seconds, triggers 2–3 powerful recovery sighs and fast heart-rate recovery that slows down **only until reaching 125 BPM** (holding cleanly at 125 BPM rather than plunging straight back to rest).

---

## Version 21.0 (`versionCode = 21`)
**Date:** 2026-11-26

- **Beta Option #7 — Medial Labial Root Color Continuity (`VulvaGeometryBuilder.kt`, `VulvaGlRenderer.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Added an independent, OFF-by-default Beta toggle with an adjustable strength slider (`75%` default when enabled) strictly targeting the pale cutaneous tone band at the labia minora–vestibule root junction (`u -> 0`).
  - Maintains warm mucosal continuity in `targetMucosa`/`targetWetness`, prevents `innerRootAlpha` from exposing lighter underlying patch skin, preserves mucosal patch coloration along the immediate attachment rim, and unlocks `vestBaseMask` at lower mucosal weights without modifying geometry, normals, lighting, or free-edge pigmentation.
- **Peak 140 BPM Exertion Sequence & Post-Peak Recovery (`InteractiveVulva3DCanvas.kt`, `VulvaGlRenderer.kt`, `BlossomUpperBodyBuilder.kt`)**
  - Automatically triggers a coordinated ~10-second peak exertion sequence when heart rate reaches **140 BPM**: very fast, heavy, irregular breathing; rapid blinking (mostly double blinks) with two randomly placed 1–2s sustained eye closures; legs quickly closing shut then trembling erratically and independently; and head looking straight down (~1.1s) before leaning all the way back (`headPitch = -1.0f`) with quick, small erratic side-to-side movements.
  - Followed immediately by **2–3 powerful, slow recovery sighs**, **2× heart-rate cooldown speed for 6 seconds** returning smoothly to normal recovery rate afterward, and seamless return of head, legs, and blinking to resting behavior.
- **Refined Asymmetric Blinking, Default Elasticity & Beta Lab Ergonomics (`InteractiveVulva3DCanvas.kt`)**
  - Tightened left/right asymmetric blink offset by 50% (`8–19ms`, down from `16–38ms`).
  - Raised default vulva elastic resistance to **50%** (`0.50f`, max `60%`).
  - Scaled Beta Lab overlay text ~20% smaller and added nested scroll/fling isolation so scrolling to the end of the Beta Lab menu stops cleanly and never scrolls the parent page.

---

## Version 19.0 (`versionCode = 19`, ★ Significant Pre-Graduation Milestone)
**Date:** 2026-11-26

> **Milestone Status:** Candidate pre-graduation configuration prior to baking Beta Lab options into the permanent production baseline. All options remain independently toggleable in Beta Lab while launching with tuned production-candidate defaults.

- **Promoted Beta Lab Defaults (Vulva, Life, and Life+)**
  - **Vulva Tab (all 8 options enabled by default, preserving last slider values on toggle OFF/ON):**
    - Option 1 (*Organic tissue continuity*): **ON @ 50%** (3× scaled)
    - Option 2 (*Soft spatial pigmentation*): **ON @ 50%** (3× scaled)
    - Option 3 (*Subtle anatomical asymmetry*): **ON @ 85%** (3× scaled)
    - Option 4 (*Regional curvature & material*): **ON @ 36%** (3× scaled)
    - Option 5 (*Labial-vestibule color blend*): **ON @ 75%**
    - Option 6 (*Intricate labial folds & border*): **ON @ 65%**
    - *Vulva elastic resistance*: **ON @ 40%** (expanded maximum range to **60%**)
    - *Breast size boost*: **ON @ 45%** (range 0–50%, most petite setting unchanged)
  - **Life Tab:** All 6 subtle micro-behaviors enabled by default.
  - **Life+ Tab:** All 7 next-gen realism behaviors (*Eyelid tracking*, *Asymmetric blinking*, *Gaze anticipation*, *Subtle swallowing*, *Postural settling*, *Breath-linked face softness*, *Subtle capillary warmth*) enabled by default; *Show heart rate (BPM)* HUD overlay remains **OFF** by default.
- **Elasticity-Aligned Self-Spread IK (`BlossomArmIkRig.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Calibrated Blossom's *"Ask Blossom to Spread"* animation (`SELF_SPREAD_OUTER_TARGET = 0.56f`, `SELF_SPREAD_INNER_TARGET = 0.60f`) and lateral hand glide (`0.114f`) to respect the default **40% elastic resistance** spread limit (`maxSpread = 0.60f`), and dynamically cap to tighter limits if elasticity is raised up to 60%.
- **3× Slower Physiological Heart-Rate Rise (`VulvaGlRenderer.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Introduced `HEART_RATE_RISE_RATE_DIVISOR = 3.0f` across both continuous sympathetic climb (`stepHeartRateBpm`) and tap impulse bumps (`registerPhysiologicalStimulus`), preserving resting BPM (66), sensitivity-weighted ceilings (up to 142 BPM), and smooth parasympathetic recovery.
- **Scroll- & Tab-Persistent 3D Viewport State (`BlossomViewportSessionState.kt`, `AtlasViewModel.kt`, `AnatomyScreen.kt`)**
  - Hoisted viewport session state into `AtlasViewModel` so scrolling the 3D viewport out of view in `LazyColumn` (or switching tabs) never resets Blossom's pose, spreading animation, camera angle, breathing cycle phase, heart rate, or Beta Lab settings.
- **Toggle Slider Value Memory & Compact Translucent Beta Lab Overlay (`InteractiveVulva3DCanvas.kt`)**
  - Every Beta Lab slider option now retains its last configured value when toggled OFF and restores that exact value when toggled back ON (including after *All off*).
  - Resized the floating Beta Lab panel to **60% height (`200.dp`)** and **~30% narrower width (`222.dp`)** with increased surface translucency (`alpha = 0.72f`) so Blossom remains clearly visible while adjusting controls live.

---

## Versions 14.0 – 18.0 (`versionCode = 14..18`, Beta v2 → Beta v3 & Lifelike Evolution)
**Date:** 2026-11-25

- Baked Vulva Beta v2 geometry/concavity corrections into the live baseline (with optimal 30% vestibular concavity).
- Introduced the always-on-top 3-tab floating **Beta Lab** (`Vulva`, `Life`, `Life+`), physiological cardio-respiratory heart-rate coupling, Beta v3 anatomical realism options 1–6 (with 3× scaling on options 1–4, labial-vestibule color blending, and intricate wavy folds), proportional 0–50% breast size boost, and vulva soft-tissue elastic resistance.

---

## Version 13.0 (`versionCode = 13`, Beta v1.1 — Golden Baseline Checkpoint)
**Date:** 2026-11-25

- **Supine Breast Profile Refinement (`BlossomUpperBodyBuilder.kt`)**
  - Replaced the flat-topped/steep-walled perimeter profile in `updateBlossomBreastsMesh` with a continuous organic `cos^p` supine teardrop curve (graceful concave-to-convex upper pectoral sweep, natural soft lower-pole teardrop roll, and refined lateral/lower fullness), eliminating the cylindrical side-profile silhouette while preserving front aesthetics, cleavage, areolae/nipples, seam convergence, and size scaling.
- **Realistic Thoracic Ribcage Breathing (`VulvaGlRenderer.kt`)**
  - Replaced the world-Z / per-vertex `nrm.z` thoracic breathing mask in `VERTEX_SHADER` with a cushion-relative ribcage depth mask (`zAboveMat = pos.z - mattressZ(pos.y)`) that saturates at the anterior chest wall beneath the breasts.
  - Enabled cohesive ribcage expansion (anterior `+Z`, subtle pump-handle `+Y`, and bucket-handle lateral `±X`) synchronized in lockstep with the existing abdominal breathing so breasts ride naturally on the expanding chest wall with zero Z-telescoping.
- **Rounded Finger & Toe Tips (`BlossomUpperBodyBuilder.kt`, `BlossomSdfBodyBuilder.kt`)**
  - Replaced the linear-`s` ogive tip taper on all 10 fingers and 10 toes with true 5-ring hemispherical rounded tip domes (`fingerRadiusProfile` / `toeRadiusProfile`) while keeping fingernails and toenails flush and preserving mesh vertex counts and outward CCW winding.
- **Verification**
  - Added automated invariants in `BlossomGeometryUnitTest.kt` covering rounded digit dome curvature, non-cylindrical supine breast side-profile ratios, and zero-telescoping ribcage breathing (`gradle :app:testDebugUnitTest` green).

---

## Version 12.0 (`versionCode = 12`, Beta v1)
**Date:** 2026-11-25

- **Promoted Beta Vulva & Seam Refinements to Standard Defaults**
  - Made all 12 calibrated anatomical/rendering refinements (Seam A–F, inner labia tone/blotch refinement, neck/hair polish, perineum/buttock table softness) standard by default (`legacyVulvaMode = false`), removed temporary beta/diagnostic toggles, and preserved the original look behind a collapsed-by-default **Advanced rendering → Legacy vulva** toggle.
- **OpenGL Rendering Stability & Performance (`VulvaGlRenderer.kt`)**
  - Resolved `E/HWUI : EglManager::fenceWait` timeouts by adding GPU VBO/IBO caching, conservative clip-space AABB frustum culling, split Pass-1/Pass-2 arch index buffers, background render-thread priority, `glFinish()` before `eglSwapBuffers`, and frustum-aware breathing/blinking cadence.
- **Documentation**
  - Added `README.md` and `docs/3D_RENDERING_ARCHITECTURE.md` documenting meshes, shader channels, seams, and extension points.
