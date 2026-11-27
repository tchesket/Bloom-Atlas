# 3D Rendering & Anatomy Architecture Guide

This document is a plain-English map of **what draws what**, **which variables control which colors and shapes**, and **the subtle OpenGL gotchas** inside Bloom Atlas's procedural 3D engine.

If you want to tweak a curve, adjust a skin/mucosa tone, or debug a seam without reading 6,000 lines of Kotlin and GLSL first, start here.

---

## 1. World Coordinate System

All meshes share a single right-handed world space (model matrix is identity):

| Axis | Direction | Reference Landmarks |
| :--- | :--- | :--- |
| **`+X` / `-X`** | Viewer's right / left | `x = 0` is the sagittal midline; `±0.64` is the vulva patch horizontal radius |
| **`+Y` / `-Y`** | Superior (head) / Inferior (feet) | Vulva center `y ≈ -0.06`, anus `y ≈ -0.78` (Beta v1) / `-0.86` (Legacy), breasts `y ≈ 3.45`, neck `y ≈ 5.15–5.75`, head `y ≈ 6.35` |
| **`+Z` / `-Z`** | Anterior (toward camera) / Posterior (toward exam cushion) | Anterior pelvic surface sits near `z ≈ 0.0–0.45`; exam cushion sits behind negative `Z` |

---

## 2. How the Scene Is Layered (Draw Order)

The full body is **not** one single mesh. Several procedural meshes overlap intentionally in depth, and knowing **which mesh owns which pixels** is essential:

| Draw Order | Mesh Field (`VulvaAnatomySceneMesh`) | Built / Updated In | Material Pass | What It Renders |
| :---: | :--- | :--- | :---: | :--- |
| 1 | `blossomDrapeMeshes` | `BlossomBodyBuilder.kt` | **Pass B** (`1.0`) | Matte charcoal-plum clinical exam cushion |
| 2 | `blossomBodyMesh` | `BlossomSdfBodyBuilder.kt` | **Pass A** (`0.0`) | Marching Cubes SDF torso, hips, buttocks, thighs, calves, feet & neck — with an oval cutout at the pelvis (`Seam D`) |
| 3 | `blossomVulvaPatchMesh` | `BlossomSdfBodyBuilder.kt` + `VulvaGeometryBuilder.kt` | **Pass A** (`0.0`) | High-res `92 × 116` parametric grid projected directly onto the SDF body surface across the oval `rx = 0.64, ry = 1.02` centered at `y = -0.06`. Renders mons pubis, labia majora, vestibule floor, perineum, raphe, and anus |
| 4 | `glansMesh` | `VulvaGeometryBuilder.kt` (`updateGlansMesh`) | **Pass A** (`0.0`) | 3D rounded clitoral glans tucked under the clitoral hood |
| 5 | `labiaHoodArchMesh` | `VulvaGeometryBuilder.kt` (`updateContinuousLabiaHoodArch`) | **Pass A** (`0.0`) | **Separate sculpted 3D mesh** sitting over the inner vulva oval! Renders the continuous horseshoe clitoral hood, frenulum, and both inner labia (labia minora) folds |
| 6 | `blossomBreastsMesh` | `BlossomSdfBodyBuilder.kt` | **Pass A** (`0.0`) | Elastic breasts + areola/nipple pigmentation (sized by `breastSize`) |
| 7 | `blossomHeadMesh` + `blossomArmMeshes` | `BlossomUpperBodyBuilder.kt` | **Pass A** (`0.0`) | Sculpted head/face/lips/eyelids + posed IK arms and hands |
| 8 | `blossomNailMeshes` | `BlossomUpperBodyBuilder.kt` | **Pass E** (`4.0`) | Translucent pink keratin fingernails & toenails (drawn with `glPolygonOffset(-2, -2)`) |
| 9 | `blossomEyeMeshes` | `BlossomUpperBodyBuilder.kt` | **Pass C** (`2.0`) | Sclera, slate-blue iris, pupil, refracted iris caustics, wet tear-line meniscus & corneal catchlight |
| 10 | `blossomEyelashMeshes` *(v27.0)* | `BlossomUpperBodyBuilder.kt` (`buildBlossomEyelashesMesh`) | **Pass G** (`6.0`) | Alpha-feathered, blink- & head-turn-synchronized 3D upper & lower eyelash fans (`GL_BLEND`, `glDepthMask(false)`, toggled by `betaRealismEyesAndLashes`) |
| 11 | `blossomHairMesh` | `BlossomUpperBodyBuilder.kt` | **Pass D** (`3.0`) | Layered bob hairstyle (with strand normal detail, v28.0 right-of-center scalp part line, macro lock clump valleys & dual-lobe Kajiya-Kay R+TRT sheen) |
| 12 | `blossomHairWispMeshes` *(v27.0–v28.0)* | `BlossomUpperBodyBuilder.kt` (`buildBlossomHairWispsMesh`) | **Pass F** (`5.0`) | 62 sculpted multi-tier volumetric lock cards & backlit flyaway strands (`6,944` verts, jagged needle tips, Kajiya-Kay R+TRT specular, `GL_BLEND`, `glDepthMask(false)`, toggled by `betaRealismLayeredHair`) |

> **Key insight:** If you look head-on at the vulva, the outer mons, labia majora, central vestibule floor, and perineum come from **`blossomVulvaPatchMesh`**, whereas the raised inner labia folds and clitoral hood arch come from **`labiaHoodArchMesh`**. Editing `evaluateVulvaDetail()` changes the patch; editing `updateContinuousLabiaHoodArch()` changes the inner labia & hood!

---

## 3. Vertex Buffer Layout

Every `DynamicGlMesh` uses an interleaved **10-float (40-byte)** stride (`DynamicGlMesh.FLOATS_PER_VERTEX = 10`):

| Offset | GLSL Attribute | Components | Meaning in Pass A (Skin & Mucosa) |
| :---: | :--- | :--- | :--- |
| `0..2` | `aPos` | `x, y, z` | World-space rest position (breathing offset is applied in the vertex shader to `vWorldPos`, while procedural textures sample undisplaced `vRestPos` so pores & pubic hair never swim) |
| `3..5` | `aNormal` | `nx, ny, nz` | Outward unit surface normal |
| `6` | `aMat.x` | `mucosa` (`0..1`) | `0` = outer skin, `1` = pink/vascular mucosal tissue (also used for facial lips) |
| `7` | `aMat.y` | `wetness` (`0..1`) | Controls broad specular exponent & crisp secondary wet mucosal highlight |
| `8` | `aMat.z` | `ao` (`0.06..1.0`) | Baked ambient occlusion (crevices, cleft, under-hood, under-chin, anus) |
| `9` | `aMat.w` | `pigment` (`0..1`) | Multi-purpose melanin/pigmentation mask: labial rim & majora warmth in pelvis, areola/nipple on chest (`y ≈ 2.65..5.4`), eyebrows/eyelashes on head (`y > 5.4`) |

**Auxiliary per-vertex attributes:**
- `aArchAlpha` (`labiaHoodArchAlphaBuffer`, 1 float/vertex): Used exclusively by `labiaHoodArchMesh` to feather the outer ~20% skirt alpha from `1.0` (core) down to `0.0` (perimeter).
- `aBodyBindHole` (`blossomBodyBindHoleBuffer`, 4 floats/vertex, v25.0–v28.0): Static unposed bind-pose coordinates `(bindX / 0.64, (bindY + 0.06) / 1.02, bindZ, bindNz)` bound unconditionally for `blossomBodyMesh`. Serves two purposes: (1) keeps Seam D's pelvic oval discard strictly anchored to the unposed pelvis during strong inward thigh squeezes, and (2) lets v28.0 Pass A reconstruct `bindPos` so dynamic thigh/hip-flexion creases, kneecaps, and leg skin undertones move 100% locked to the posed thighs.

---

## 4. Fragment Shader Overview (`VulvaGlRenderer.kt`)

All rendering goes through one unified GLSL ES 2.0 shader program branched by `uMaterialType`:

- **Pass A (`uMaterialType < 0.5`) — Living Skin & Mucosa**
  - 3-point studio lighting (`Lkey`, `Lfill`, `Lrim`) with toggleable Exam Overhead vs. Raking Side Light (`uLightMode`).
  - **RGB Pre-Integrated Subsurface Scattering (SSS)**: red light wraps deeper across shadow terminators than green/blue (`sssWrapKey`, `sssWrapFill`), plus thin-tissue translucency (`thinTransmit`, `grazingTrans`) on high-mucosa folds.
  - **Multi-scale 3D procedural skin detail** (`skinDetailHeight` in `vRestPos`): pores, fine labial striae, close-up areola creping (`dryPigmentMask`), and close-up pelvic dermal relief (`pelvicSkinZone`).
  - **Colored Hemoglobin AO Bounce** (`coloredAo`): powers AO with `(0.76, 1.00, 1.18)` so crevices warm toward blood-red rather than turning muddy gray.
  - **Procedural eyebrows, eyelashes & pubic hair**: guarded by fast-path branch checks so non-hair fragments skip all 3D noise evaluations.
  - **v28.0 Packed Realism Uniform `uBetaRealismFlags3`**: `x` = Option #8 dorsal clitoral hood–mons skin blend (`betaV3HoodMonsBlend * betaV3HoodMonsStrength`), `y` = facial micro-detail (vertical lip creases/pillow sheen, feathered brows, subtle freckles), `z` = 3-point studio rim & warm cushion bounce lighting, `w` = areola relief, sub-dermal vein hints, and Pass E longitudinal keratin nail ridges + lunula.
  - **Filmic Reinhard tone mapper**: `lit / (lit + 0.76)` with gentle saturation preservation.
- **Pass B (`uMaterialType > 0.5`)** — Matte clinical linen weave (`blossomDrapeMeshes`), plus v27.0 pose-aware body/arm/leg contact shadows and weight-tension ripples (`uBetaRealismFlags1.w`).
- **Pass C (`uMaterialType > 1.5`)** — Anatomical eyes with radial iris fibers, limbal ring, corneal bulge specular (`blossomEyeMeshes`), plus v27.0 collarette ring, refracted opposite-side iris caustic, caruncle warmth, and wet tear-line meniscus (`uBetaRealismFlags1.z`).
- **Pass D (`uMaterialType > 2.5`)** — Anisotropic layered bob hair (`blossomHairMesh`) with v28.0 scalp part line, macro clump valleys, and dual-lobe Kajiya-Kay (primary R + pigment-tinted TRT) sheen (`uBetaRealismFlags1.x`).
- **Pass E (`uMaterialType > 3.5`)** — Translucent rosy-pink keratin fingernails & toenails (`blossomNailMeshes`), enhanced in v28.0 with longitudinal keratin micro-ridges, brighter lunula, and crisp hydrated crown sheen (`uBetaRealismFlags3.w`).
- **Pass F (`uMaterialType > 4.5`, v27.0–v28.0)** — 62 alpha-feathered volumetric lock cards & sunlit flyaway halo ribbons with staggered jagged tips and Kajiya-Kay R+TRT specular (`blossomHairWispMeshes`, gated by `betaRealismLayeredHair`).
- **Pass G (`uMaterialType > 5.5`, v27.0)** — Alpha-feathered 3D upper & lower eyelash fans synchronized with blinks and head turns (`blossomEyelashMeshes`, gated by `betaRealismEyesAndLashes`).

---

## 5. Beta v1 Refinements & How the Vulva Seam Works

In **Beta v1**, all 12 visual/anatomical refinements are **ON by default**. Toggling **Advanced rendering → Legacy vulva** (`legacyVulvaMode = true` in `VulvaDeformationState`) turns all 12 OFF at once to restore the exact pre-Beta v1 look.

| Refinement | Flag in `VulvaDeformationState` | Where It Lives | What It Does |
| :--- | :--- | :--- | :--- |
| **1. Shorter Perineum (-20%)** | `betaShortPerineum` | `BlossomSdfBodyBuilder.kt`, `VulvaGeometryBuilder.kt` | Moves the anus & lower gluteal cleft up ~20% (`+0.08` Y) toward the posterior fourchette |
| **2. Soft Buttock & Fold** | `betaSoftButtockTable` | `BlossomSdfBodyBuilder.kt` (`evaluateBodySdf`, `applyPelvisBetaRefinements`) | Rounds buttock-to-cushion contact and adds a subtle infragluteal under-fold |
| **3A. Seam A: Flush Rim Geometry** | `betaSmoothVulvaSeam` | `BlossomSdfBodyBuilder.kt` (`updateBlossomVulvaPatch`), `VulvaGlRenderer.kt` (`SEAM_A_PATCH_POLY_OFFSET_*`) | Snaps the vulva patch perimeter flush onto rendered body triangles, blends normals to match, and applies a slight negative polygon offset to prevent Z-fighting |
| **3B. Seam B: Wide Rim Color Fade** | `betaBlendVulvaSeamColor` | `BlossomSdfBodyBuilder.kt` (`updateBlossomVulvaPatch`) | Fades patch `mucosa`, `wetness`, `ao`, and `pigment` over the outer 40% (`ovalR` `0.60..1.00`) to match the exact surrounding body skin |
| **3C. Seam C: Soft Labia Interior Edge** | `betaSoftLabiaInteriorEdge` | `VulvaGeometryBuilder.kt` (`evaluateVulvaDetail`) | Widens `medialMargin`, `warmInnerLabiaMajora`, `majoraBodyPigment`, and mucosa/AO falloffs ~2× so the patch interior blends softly into surrounding skin |
| **3D. Seam D: Smooth Body Hole Cutoff** | `betaSmoothBodyHoleCutoff` | `VulvaGlRenderer.kt` (`uBetaSmoothBodyHole`, `SEAM_D_HOLE_OVAL_R = 0.93f`) | Clips the anterior body mesh opening per-fragment along a smooth mathematical `ovalR` ellipse instead of coarse Marching Cubes quad stair-steps |
| **3E. Seam E: Soft Arch Edge** | `betaSoftArchEdge` | `VulvaGeometryBuilder.kt` (`updateContinuousLabiaHoodArch`), `VulvaGlRenderer.kt` (`uArchAlphaPass`) | Feathers the outer ~20% skirt of `labiaHoodArchMesh` via `labiaHoodArchAlphaBuffer` using a **2-pass draw** (Pass 1 opaque core writing depth; Pass 2 alpha-blended skirt with `glDepthMask(false)`) and blends skirt vertex materials to match the underlying patch |
| **3F. Seam F: Labia Arch Winding & Shade** | `betaWarmTransitionZone` | `VulvaGeometryBuilder.kt` (`labiaHoodArchCcwIndexBuffer`), `VulvaGlRenderer.kt` (`seamFShadeTarget`, `archWarmMask`) | Fixes the inverted CW triangle winding on `labiaHoodArchMesh` with an outward CCW index buffer, deepens the newly front-lit tissue by ~1 warm skin shade (`seamFShadeTarget`), and keeps the outer skirt transition warm |
| **3G. Inner Labia Tone & Sheen** | `betaRefineInnerLabia` | `VulvaGlRenderer.kt` (`labiaInnerMask`, `broadExp`/`wetKeyExp`, `warmRoseBrown`) | Softens pale specular/noise blotches across the inner labia (`labiaInnerMask`) while preserving wet sheen, and adds a subtle warm rose-brown tint strongest along the rolled free edges |
| **4. Faint Perineal Raphe** | `betaPerinealRaphe` | `VulvaGeometryBuilder.kt` (`evaluateVulvaDetail`) | Adds a delicate midline cutaneous ridge between the fourchette and anus |
| **5. Smooth Neck Seam** | `betaSmoothNeckSeam` | `BlossomUpperBodyBuilder.kt`, `BlossomSdfBodyBuilder.kt`, `VulvaGlRenderer.kt` (`neckSkinBlend`, `submentalFeatherZone`) | Doubles the jaw-to-neck AO, normal, and tone transition feathering so the head joins the neck smoothly |
| **6. Strand-Detail Hair** | `betaSoftHair` | `BlossomUpperBodyBuilder.kt`, `VulvaGlRenderer.kt` (`Pass D` `uBetaSoftHair`) | Preserves the bob silhouette while adding per-strand normal perturbation, lowlight/highlight lock variation, dual anisotropic sheen, and subtly tapered irregular tips |

---

## 6. Critical OpenGL Gotchas (Learned the Hard Way!)

1. **Inverted triangle winding turns a mesh dark/gray in `Pass A`!**
   - The fragment shader has `if (!gl_FrontFacing) N = -N;` (required for thin two-sided folds).
   - If a mesh's index buffer is wound **clockwise (CW)** when viewed from the front, OpenGL flags every front pixel as `!gl_FrontFacing` and **flips the vertex normal backwards**, killing direct key lighting and making the mesh look dark gray regardless of albedo tweaks!
   - `labiaHoodArchMesh` originally had CW winding (`indexBuffer`). Beta v1 uses `labiaHoodArchCcwIndexBuffer` (outward CCW) whenever `betaWarmTransitionZone` is active.
   - `BlossomGeometryUnitTest.kt` has `assertOutwardWindingMatchesNormals(...)` specifically to catch this automatically.

2. **Two-pass alpha is required for feathered overlays (`Seam E`)**
   - Simply enabling `GL_BLEND` with `glDepthMask(true)` on a self-overlapping 3D fold causes sorting artifacts; disabling depth writes globally causes back folds to bleed through front folds.
   - `VulvaGlRenderer.kt` draws `labiaHoodArchMesh` twice: **Pass 1** (`uArchAlphaPass = 1.0`) draws only opaque core pixels (`archAlpha >= 0.995`) with depth writes ON; **Pass 2** (`uArchAlphaPass = 2.0`) draws only feathered skirt pixels (`0.004 <= archAlpha < 0.995`) with `GL_BLEND` ON and `glDepthMask(false)`.

3. **Dirty-flag mesh caching in `VulvaAnatomySceneMesh.update()`**
   - Idle animations (eye blinks, head turns, leg spreads, arm IK) call `scene.update(...)` frequently.
   - `VulvaAnatomySceneMesh` tracks `lastVulvaStateKey` and `lastBreastsStateKey` so the ~24k-vertex vulva meshes and breast mesh are **only rebuilt when their own deformation inputs actually change**, eliminating unnecessary per-frame CPU rebuilds.

4. **Procedural detail samples `vRestPos`, not `vWorldPos`**
   - Breathing displaces torso/breast vertices in the vertex shader. All 3D noise (skin pores, pubic hair, areola detail, neck masks) samples undisplaced `vRestPos` so surface details stay pinned to the skin.

---

## 7. Quick Cheat Sheet: "Where Do I Edit X?"

| I want to change… | File | Symbol / Variable |
| :--- | :--- | :--- |
| Skin tone palette presets | `InteractiveVulvaCanvas.kt` | `VulvaToneStyles` (`skinMid`, `skinShadow`, `mucosaMid`, `vestibuleDeep`) |
| Hair color presets | `VulvaGlRenderer.kt` | `BlossomHairStyles` |
| Outer labia majora / mons / vestibule / anus / perineal raphe shape & vertex materials | `VulvaGeometryBuilder.kt` | `VulvaAnatomySceneMesh.evaluateVulvaDetail(...)` |
| Inner labia (labia minora) & clitoral hood 3D folds + outer skirt alpha | `VulvaGeometryBuilder.kt` | `VulvaAnatomySceneMesh.updateContinuousLabiaHoodArch(...)` |
| Clitoral glans size / position / sheen | `VulvaGeometryBuilder.kt` | `VulvaAnatomySceneMesh.updateGlansMesh(...)` |
| Inner labia color warmth, 1-shade darkening, or wet specular highlight | `VulvaGlRenderer.kt` | `FRAGMENT_SHADER` → `seamFShadeTarget`, `warmRoseBrown`, `labiaInnerMask`, `broadExp`, `wetKeyExp` |
| Vulva-to-body hole size or seam blending | `BlossomSdfBodyBuilder.kt` & `VulvaGlRenderer.kt` | `PATCH_RX`, `PATCH_RY`, `PATCH_CENTER_Y`, `SEAM_D_HOLE_OVAL_R`, `updateBlossomVulvaPatch(...)` |
| Body proportions, hips, thighs, buttocks, perineum SDF | `BlossomSdfBodyBuilder.kt` | `evaluateBodySdf(...)` & `applyPelvisBetaRefinements(...)` |
| Pubic hair shape, curl & density | `VulvaGlRenderer.kt` | `FRAGMENT_SHADER` → `monsTopArch`, `cleftArchY`, `hairCoord`, `hairCoverage` |
| Head, face, eyes, blinking, hair silhouette, arms & nails | `BlossomUpperBodyBuilder.kt` | `buildHeadMesh`, `buildHairMesh`, `updateEyeBlink`, `updatePosedArms` |
| Self-spread arm/hand IK pose & targets | `BlossomArmIkRig.kt` | `SELF_SPREAD_OUTER_TARGET`, `SELF_SPREAD_INNER_TARGET`, `solveArmPose` |
| Camera presets & touch-zone routing | `InteractiveVulva3DCanvas.kt` & `BlossomTouchHitResolver.kt` | `CameraAnglePreset`, `BlossomTouchHitResolver.resolveTouchZone` |
| Legacy Vulva compatibility toggle UI | `InteractiveVulva3DCanvas.kt` | `legacyVulvaMode`, `testTag("toggle_legacy_vulva")` |
