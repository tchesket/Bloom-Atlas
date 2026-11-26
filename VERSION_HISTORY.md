# Bloom Atlas — Version History

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
