# Bloom Atlas — Version History

## Version 30.0 (`versionCode = 30`)
**Date:** 2026-11-26

- **Eliminated Under-Jaw Hair Cap Shell Flaps (`BlossomUpperBodyBuilder.kt`)**
  - Fixed `buildBlossomHairMesh()` Part A (base hair cap) where lateral `maxTheta` wrapped past the head equator (`0.75–0.85 * PI`), causing `sin(theta)` to shrink inward underneath Blossom's jawline and create two concave shell flaps beside her neck.
  - Restricted deep downward cap wrap strictly to the rear occiput/nape (`rearNapeMask = smoothStep(0.50, 0.90, -cosP)`), trimmed lateral sides cleanly behind the ears (`lateralJawTrim`), added an outward rear-nape pillow drape profile (`drapeSinX`) with mattress floor clamping, and preserved both the arched parted forehead hairline and outward CCW winding.
- **Restored Visible Head-Locked Freckles & Balanced Lip Micro-Detail (`VulvaGlRenderer.kt`)**
  - Tuned `betaRealismFacialMicro` (*Lip, brow & freckle detail*) in Pass A so head-locked freckles across the nose bridge and upper cheeks are clearly visible and natural (~68% of v28 strength) rather than washed out, and moderately boosted organic non-periodic lip texture, crease AO, center rose warmth, and hydrated lower-lip satin sheen without reintroducing barcode stripes.
- **New Beta Toggle — Hip-Anchored Thigh Swing (`BlossomSdfBodyBuilder.kt`, `VulvaGeometryBuilder.kt`, `BlossomViewportSessionState.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Added an OFF-by-default *"Hip-anchored thigh swing"* (`betaRealismHipThighSwing`) toggle in Tab 2 (`Life+`) under *Visual realism (v27–v30 Beta)*, backed by dual zero-rebuild weight arrays in `LegSkinCache` so toggling ON/OFF switches live at 60 FPS and reverts bit-identically when OFF.
  - Distributes both hip-socket spread rotation and planted-knee lateral displacement linearly along the femur about the authored hip pivot (`HIP_PIVOT_AX = 1.12, HIP_PIVOT_Y = 0.26, HIP_PIVOT_Z = -0.22`) with a soft pelvic attachment and bounded medial-to-femur transition (`lateralFemurFactor = smoothStep(0.76, 1.44, ax)`), raising quarter-thigh travel across wide-to-first-squeeze from `3.3%` → `20.9%` on the femur centerline (`5.6%` → `26.0%` outer thigh, `7.2%` → `22.1%` anterior thigh) and mid-thigh from `45.6%` → `49.9%` while preserving exact knee travel (`1.9849` → `1.9849`), planted feet, v29 3D pelvic collar pinning, Seam D, and zero upper-inner-thigh foldover across hard squeezes, asymmetric returns, and manual spreads.

---

## Version 29.0 (`versionCode = 29`)
**Date:** 2026-11-26

- **Head-Locked Facial & Hair Procedural Textures (`BlossomUpperBodyBuilder.kt`, `VulvaGlRenderer.kt`)**
  - Added `BlossomUpperBodyBuilder.computeInverseHeadMatrix()` computing the exact column-major 4×4 inverse of `applyHeadTurn`'s rigid head transform (`T(pivot) * Rz * Ry * Rx * T(-pivot)`), uploaded per-draw as `uInvHeadMatrix` for head, hair, hair-wisps, eye, and eyelash meshes.
  - Evaluates `vRestPos = (uModel * (uInvHeadMatrix * vec4(aPos, 1.0))).xyz` in `VERTEX_SHADER` (and switched legacy Pass D hair and Pass C iris noise from `vWorldPos` to `vRestPos`), completely eliminating world-space texture swimming on freckles, eyebrows, lip detail, hairline shadows, iris fibers, and hair strands whenever Blossom's head turns or nods.
- **Improved Base Hair Silhouette, Reduced Hair Reflectivity & Refined Base + Beta Facial Detail (`BlossomUpperBodyBuilder.kt`, `VulvaGlRenderer.kt`)**
  - **Base Hair Geometry**: Replaced the ruler-straight horizontal forehead bangs edge in `buildBlossomHairMesh()` with a softly arched right-of-center parted hairline, and reshaped the 20 side and nape locks into a wider, overlapping curved bob curtain with gentler hem taper so the bob no longer breaks into spiky triangular wedges beside her chin. Tightened `blossomHairWispsMesh` Tiers 3–5 to hug the cohesive bob silhouette.
  - **Restrained Hair Sheen**: Significantly dialed back Pass D & Pass F specular and rim-halo reflectivity and replaced white-tinted specular highlights with warm hair-toned sheen so hair looks natural and silky rather than metallic/silvery.
  - **Natural Base Lips & Refined `betaRealismFacialMicro`**: Enriched base lips (both `faceMucosa` in `buildBlossomHeadMesh` and `facialLipTone` in Pass A) from pale beige to soft, healthy muted rose. Replaced periodic vertical barcode lip stripes with ultra-subtle organic non-periodic micro-texture, dialed freckles back ~65% into soft organic clusters, and softened eyebrow hair strokes.
- **Removed Retired Visual Realism Toggles (`VulvaGeometryBuilder.kt`, `BlossomViewportSessionState.kt`, `VulvaGlRenderer.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Completely removed `betaRealismSkinDetail` (*Skin micro-detail & SSS*) and `betaRealismStudioLighting` (*Studio rim & cushion bounce*) from state, Beta Lab UI, renderer uniforms, shaders, and tests.
- **New Vulva Beta Option #9 — Upper Hood–Mons Color Blend (`VulvaGeometryBuilder.kt`, `VulvaGlRenderer.kt`, `BlossomViewportSessionState.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Added an independent **OFF-by-default** toggle + 0–100% strength slider (`betaV3HoodMonsUpperBlend` / `betaV3HoodMonsUpperStrength`, `75%` default, always visible in Tab 0 `Vulva` right below Option #8).
  - Targets the triangular rose-brown patch (center + both lateral shoulders) immediately above the exposed glans and below the pubic hair, anchored to hood/anterior commissure landmarks so it tracks `hoodRetract`. Smoothly blends base albedo across both `blossomVulvaPatchMesh` and dorsal `labiaHoodArchMesh` toward surrounding mons skin across all skin tones without suppressing high-mucosa upper patch vertices, while strictly excluding `glansMesh`, the inner preputial rim, and the lower vestibular floor, and preserving micro-relief, fold AO, and wetness.
- **Fixed Bilateral Upper-Inner-Thigh Fold & Bind-Pose Pelvic Shading Guard During Peak Leg Animation (`BlossomSdfBodyBuilder.kt`, `VulvaGlRenderer.kt`)**
  - Replaced the 2D `(x, y)` `ovalR` exclusion and `patchGuard` in `BlossomSdfBodyBuilder.buildLegSkinCache()` with a 3D anterior pelvic collar distance (`collarSurfaceZ = 0.56`, `collarDzNorm = max(0, z - collarSurfaceZ) / 0.30`, `collarDist3d = sqrt(ovalR^2 + collarDzNorm^2)`), pinning actual pelvic collar vertices (`collarDist3d <= 1.05`) and feathering smoothly across the groin crease (`1.05..1.68`, with gentler proximal femur ramps `femRamp 0.10..0.54` and `thighRamp 0.18..0.94`) while freeing forward inner-thigh vertices (e.g. `(±0.808, -0.410, 0.959)`) consistently for both spread (`w`) and planted-knee (`kw`) weights — eliminating bilateral upper-inner-thigh mesh foldover/inversion during hard inward squeezes with zero Seam D bridge leakage.
  - Updated Pass A `pelvicSkinZone`, `pelvicRegionMask`, and `pelvicFinishMask` to evaluate in unposed bind-pose coordinates (`pelvicCoord`) with a bind-depth guard (`pelvicBindZGuard = 1.0 - smoothstep(0.42, 0.68, bindPos.z)` on `blossomBodyMesh`), preventing squeezed forward thighs from triggering pelvic-region shading transitions or `hipCreaseZone` attenuation bands.

---

## Version 28.0 (`versionCode = 28`)
**Date:** 2026-11-26

- **Fixed Leg-Locked Dynamic Thigh/Hip Creases & Kneecaps (`VulvaGlRenderer.kt`)**
  - Fixed a confirmed coordinate-space issue in `betaRealismDynamicCreases`: because `BlossomSdfBodyBuilder.applyLegSpread()` writes already-posed thigh coordinates into `blossomBodyMesh`, evaluating inguinal/hip-flexion creases and patella landmarks in `vRestPos` left them stationary in world space while Blossom's legs swept and squeezed during the HR peak animation.
  - Bound `bodyBindHoleVboId` (`aBodyBindHole`) unconditionally for `blossomBodyMesh` (while keeping Seam D pelvic hole discard strictly gated by `uBetaSmoothBodyHole`) and reconstructed true unposed bind-pose coordinates `bindPos` in Pass A so hip-flexion creases, kneecap (patella) contours (centered at true rest knee `(1.80, -1.86, 1.78)`), and leg skin undertones now track 100% locked to her moving thighs and knees.
- **New Vulva Beta Option #8 — Clitoral Hood–Mons Skin Blend (`VulvaGeometryBuilder.kt`, `VulvaGlRenderer.kt`, `BlossomViewportSessionState.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Added an independent **OFF-by-default** toggle + strength slider (`betaV3HoodMonsBlend` / `betaV3HoodMonsStrength`, remembers `75%` default) directly visible in Tab 0 (`Vulva`) even when *"Show Extra Beta Options"* is unchecked.
  - Smoothly harmonizes dorsal/outer clitoral hood pigmentation, mucosal tint, and Seam F shade darkening into surrounding natural mons pubis skin across all skin-tone presets, while guarding the glistening inner preputial edge, lower inner labia, and clitoral glans.
- **Significantly Upgraded Subsurface Scattering (`betaRealismSkinDetail`) & Spectacular Layered Hair (`betaRealismLayeredHair`)**
  - **Subsurface Scattering (`VulvaGlRenderer.kt`)**: Replaced the overly subtle v27.0 SSS nudge with visibly stronger red-channel RGB wrap, skin-adaptive coral-hemoglobin shadow-terminator scattering, backlit + grazing thin-tissue translucency (ears, nose tip, hands/fingers, curved body rims), warmer cavity AO bounce exponents, and subtle pore specular breakup — strictly masked outside the vulva via `bodyRealismMask`.
  - **Spectacular Layered Hair (`BlossomUpperBodyBuilder.kt`, `VulvaGlRenderer.kt`)**: Expanded `blossomHairWispsMesh` from 26 sparse ribbons to **62 sculpted, S-waved multi-tier volumetric lock cards & backlit flyaway strands** (`6,944` verts across crown root-lift, curtain bangs/baby hairs, staggered cheekbone/jaw face-framing locks, pillow-fanned nape locks, and airy sunlit flyaway halos; verified outward CCW winding & mattress floor clamp), added a natural right-of-center **scalp part line** with root shadow and deep macro lock-clump valleys on `blossomHairMesh`, and upgraded both Pass D and Pass F to **Kajiya-Kay dual-lobe anisotropic specular (`sin(T, H)`)** with a sharp root-shifted neutral-gold primary R-lobe, a glowing pigment-tinted tip-shifted TRT secondary lobe, and staggered needle-tapered strand tips.
- **3 New Photorealistic Visual Realism Beta Toggles in `Life+` (All OFF by Default, Packed in `uBetaRealismFlags3`)**
  1. **Lip, Brow & Freckle Detail (`betaRealismFacialMicro`)** — Fine vertical lip micro-creases with hydrated lower-lip pillow sheen, feathered directional arched eyebrow hairs, and delicate nose-bridge/cheekbone sun-kissed freckles & cheek bloom.
  2. **Studio Rim & Cushion Bounce Lighting (`betaRealismStudioLighting`)** — Warm linen mattress-to-skin upward bounce illumination on shadowed undersides, cool overhead studio softbox fill/sheen, sculpting warm rim kicker on skin and hair, and gentle filmic vibrancy lift.
  3. **Areola, Nail & Subtle Dermal Detail (`betaRealismSurfaceMicro`)** — Soft areolar Montgomery-tubercle relief bumps, whisper-soft sub-dermal cool-teal vein hints on temples/upper chest/inner forearms, and Pass E longitudinal keratin nail micro-ridges with brighter lunula and crisp hydrated crown sheen.
- **Unit Tests & Architecture Docs (`BlossomGeometryUnitTest.kt`, `README.md`, `docs/3D_RENDERING_ARCHITECTURE.md`)**
  - Added 3 unit tests covering the 62-lock upgraded hair wisps mesh (6,944 verts, volumetric/airy encoding, outward CCW winding, floor clamp, head-turn tracking), Option #8 dorsal hood–mons blending (preserving glans and lower inner labia + bit-identical revert), and v28.0 OFF-by-default toggle & slider memory invariants.

---

## Version 27.0 (`versionCode = 27`)
**Date:** 2026-11-26

- **5 New Photorealistic Visual Realism Upgrades (All Behind Independent Beta Toggles in `Life+`, OFF by Default)**
  - All 5 new upgrades default **OFF** in both `BlossomViewportSessionState` and `VulvaDeformationState()` (preserving v26.0 visuals until opted in) and appear directly in the `Life+` Beta Lab tab (above *"Show heart rate (BPM)"*, even when *"Show Extra Beta Options"* is unchecked) so each can be enabled and A/B tested independently:
  1. **Layered Hair Wisps & Flyaway Strands (`betaRealismLayeredHair`)** — Added `blossomHairWispsMesh` (`BlossomUpperBodyBuilder.buildBlossomHairWispsMesh`, 26 curved alpha-feathered ribbons covering forehead/temple baby hairs, cheekbone/jawline face-framing wisps, crown backlit flyaway halo arches, and pillow-fanned nape tips) rendered in **Pass F** (`uMaterialType = 5.0`), paired with Marschner-inspired secondary transmittance sheen on `blossomHairMesh` and a soft warm forehead/temple hairline contact shadow on skin. Synchronized with cervical head turn/pitch and clamped cleanly above `mattressSurfaceZ`.
  2. **Regional Skin Micro-Texture & Subsurface Depth (`betaRealismSkinDetail`)** — Added distance-faded close-up dermal micro-grain (velvety facial pores vs. fine body micro-lines), subtle regional capillary undertones over bony/vascular prominences (nose tip, ear rims, shoulders, elbows, kneecaps) plus cool-olive inner-thigh complexity, and enhanced thin-tissue ear/nose/hand rim SSS + velvet peach-fuzz graze. Strictly masked out of vulvar mucosa and pelvic interior (`bodyRealismMask`) so the perfected vulva appearance remains untouched.
  3. **3D Curved Eyelashes, Wet Tear Line & Deep Iris Caustics (`betaRealismEyesAndLashes`)** — Added `blossomEyelashesMesh` (`BlossomUpperBodyBuilder.buildBlossomEyelashesMesh`, 4 upper/lower lash fans anchored flush to `blossomHeadMesh`'s sculpted eyelid margins and rendered in **Pass G** `uMaterialType = 6.0`), precomputed open/closed blink targets in `HeadRigCache` (tracking asymmetric blinking, upward-gaze lid lift, and head rotation without eyeball penetration), plus collarette ring, radial crypt contrast, opposite-side refracted corneal iris caustics, inner-canthus caruncle warmth, and a crisp lower-lid wet tear-line meniscus in **Pass C**.
  4. **Soft Contact Shadows & Cushion Weight Grounding (`betaRealismContactShadows`)** — Added pose-aware soft grounding shadows across **Pass B** (`examCushionMesh`) beneath the head, hair, shoulders, back, glutes, resting arms (fading smoothly as `selfSpreadPose` lifts Blossom's hands), and moving calves/heels (tracking `leftLegSpread`/`rightLegSpread`), plus subtle radial weight-tension linen micro-creases and warm posterior body-to-mattress clearance AO (`mattressZFrag`) on downward-facing skin.
  5. **Dynamic Skin Creasing & Anatomical Landmarks (`betaRealismDynamicCreases`)** — Added live contralateral sternocleidomastoid (SCM) neck tendon tension driven by `headTurn`, dynamic inguinal/hip-flexion compression creases driven by `leftLegSpread`/`rightLegSpread` and `leftKneeShift`/`rightKneeShift`, and delicate suprasternal notch, clavicle hollow, abdominal linea alba, kneecap (patella) contour rim, and inner-elbow crease shading.
- **Shader & Test Hardening (`VulvaGlRenderer.kt`, `BlossomGeometryUnitTest.kt`, `README.md`, `docs/3D_RENDERING_ARCHITECTURE.md`)**
  - Packed all v27.0 shader flags and pose scalars into 3 compact `vec4` uniforms (`uBetaRealismFlags1`, `uBetaRealismFlags2`, `uBetaRealismPose`) and used a GLSL ES 1.00–safe `sq()` helper across all Gaussian terms to avoid negative-base `pow()` UB on mobile GPUs.
  - Added 3 unit tests covering layered hair wisp & 3D eyelash mesh validity, outward CCW winding, mattress floor clamping, blink/head-turn synchronization without eyeball penetration, and toggle defaults.

---

## Version 26.0 (`versionCode = 26`)
**Date:** 2026-11-26

- **New Settings Page & "Show Extra Beta Options" Preference (`SettingsScreen.kt`, `PreferencesRepository.kt`, `AtlasViewModel.kt`, `MainActivity.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Added a dedicated **Settings** tab (`AtlasTab.SETTINGS`) with a DataStore-persisted **"Show Extra Beta Options"** checkbox (default OFF), plus Tactile Haptics, Reading Lens, and version info controls.
  - Preserved the 3D viewport's hamburger menu and its three tabs (`Vulva`, `Life`, `Life+`), keeping all active realism defaults enabled under the hood while hiding extra Beta Lab toggles/sliders (and `"All off"`) when unchecked, and always keeping **"Show heart rate (BPM)"** visible in the `Life+` tab.
- **Deeper, Lower-Frequency Climax Vibration Pattern (`InteractiveVulva3DCanvas.kt`)**
  - Refactored the 16-second peak episode vibration generator into `buildPeakVibrationChunk(...)`, slowing pulse frequency to ~1/3 the previous rapid cadence (~3× period: `84–159ms` ON pulses and `44–264ms` irregular gaps) and driving every pulse at peak hardware amplitude (`248–255/255`) so phone vibration motors reach full mechanical spin-up on each beat.
- **Fixed Blue Irises Visible When Eyes Should Be Closed (`BlossomUpperBodyBuilder.kt`, `InteractiveVulva3DCanvas.kt`)**
  - Fixed a confirmed eyelid-closure geometry bug in `buildHeadRigCache()`: previously, recessed inner-aperture head rows (`ly < upperLidY`) compressed downward without wrapping forward over the eyeball sphere (`targetLz` remained recessed `-0.122` behind the eyeball), causing the bridging strip from the upper eyelid to slice through the eyeball and expose the lower half of the blue irises during full closure (`eyeBlink = 1.0f`).
  - Updated closed-eyelid targets to wrap all upper-lid and inner-aperture rows monotonically onto a smooth `eyeRadius + 0.024f` cover shell across the full aperture width, and guarded the idle blink `LaunchedEffect` on `peakEpisodeActive` so in-flight idle blinks can never reopen her eyes mid-peak.

---

## Version 25.0 (`versionCode = 25`)
**Date:** 2026-11-26

- **Fixed Inner-Thigh Seam D Cutout During Strong Inward Squeezes (`VulvaGeometryBuilder.kt`, `VulvaGlRenderer.kt`)**
  - Isolated root cause in **Seam D** (`uBetaSmoothBodyHole`): `BlossomSdfBodyBuilder.applyLegSpread()` writes posed thigh coordinates into `blossomBodyMesh`, so `vRestPos = (uModel * vec4(aPos, 1.0)).xyz` evaluated the pelvic oval discard (`holeOvalR < 0.93 && z > -0.92 && nz > 0.04`) against already-posed inner-thigh vertices as they swung across the pelvis, punching a sharply bounded oval window through the thigh.
  - Added a dedicated static per-vertex bind-pose attribute (`aBodyBindHole` / `vBodyBindHole` = `vec4(bindX / 0.64, (bindY + 0.06) / 1.02, bindZ, bindNz)`) backed by a dedicated GPU VBO on `VulvaGlTextureView`, initialized from unposed bind-pose body geometry and synced strictly for anterior pelvic collar vertices on pelvis beta toggles (never mutated by leg, arm, head, or glute posing).
  - Preserved the exact smooth Seam D pelvic opening and vulvar patch edge at rest and in the active "Ask Blossom to Spread" pose (front and oblique angles), as well as the full v24.0 asymmetric leg choreography, squeeze amplitude, timing, and camera range.

---

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
