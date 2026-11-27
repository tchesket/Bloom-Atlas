# Bloom Atlas (Version 29.0)

**Bloom Atlas** is an educational Android application built with Kotlin, Jetpack Compose, Material 3, and a custom real-time OpenGL ES 2.0 procedural 3D anatomy engine.

---

## Features

- **Interactive 3D Anatomy Explorer (`Blossom v29.0`)**
  - Procedural full-body reclining anatomical model rendered in real time via a custom `TextureView` + EGL14 / OpenGL ES 2.0 pipeline (no external 3D asset files required).
  - **Touch Tissue mode**: multi-touch elastic deformation of outer/inner labia (default 50% elastic resistance, adjustable up to 60%), clitoral hood retraction, breasts, glutes, positional leg spread, head turn/nod/lean-back, and an elasticity-aligned animated IK self-spread pose (*"Ask Blossom to Spread"*).
  - **Orbit 3D mode**: drag to orbit, pinch to zoom from full-body to macro close-up, and two-finger pan head-to-toe, plus quick camera angle presets.
  - **Real-time appearance, Visual Realism & Beta Lab controls**: compact, translucent, scroll-locked always-on-top 3-tab **Beta Lab** (`Vulva`, `Life`, `Life+`) with toggle value memory, Vulva Options #8 (*Hood–mons skin blend*) and #9 (*Upper hood–mons color blend*) with independent strength sliders, 6 head- & leg-locked photorealistic visual realism toggles (*Layered hair & Kajiya-Kay*, *3D lashes & iris depth*, *Cushion contact shadows*, *Dynamic creases & landmarks*, *Lip, brow & freckle detail*, *Areola, nail & dermal detail*), skin tone presets, hair color swatches, breast size & boost sliders, pubic hair density slider, studio Exam Light vs. raking Side Light, and sensory haptics.
  - **Dedicated Settings screen**: DataStore-persisted *"Show Extra Beta Options"* toggle (cleanly hiding advanced v18–v25 tuning controls while keeping all realism defaults active), tactile haptics, reading lens, and app version info.
  - **Scroll-persistent physiological animations**: chest/abdomen ribcage breathing, micro-saccades, asymmetric (8–19ms) and double blinking, gaze anticipation, subtle swallowing, postural settling, living warmth, 3× slower sympathetic cardio-respiratory heart-rate dynamics, and a coordinated 136 BPM 3-phase peak sequence (8s restless pre-event -> 16s asymmetric peak choreography with deeper 1/3-frequency max-amplitude haptics -> 125 BPM cooldown floor) — all preserved across scrolling and tab switches via `BlossomViewportSessionState`.
  - **Legacy compatibility option**: the pre-Beta v1 look (original vulva, seam, neck, and hair rendering) is preserved behind a collapsed-by-default **Advanced rendering → Legacy vulva** switch at the bottom of the 3D explorer panel.
- **Guided Learning & Tools**
  - Illustrated anatomy guides, sensitivity & structure breakdowns, cycle wheel explorer, diversity studies, Settings, and curated educational resources.

---

## Project Layout

```text
app/src/main/java/com/example/
├── MainActivity.kt                          # App entry point, edge-to-edge setup
├── data/                                    # Anatomy models, repository & educational content
└── ui/
    ├── screens/
    │   ├── AnatomyScreen.kt                 # Primary 3D/2D anatomy explorer host
    │   ├── LearnScreen.kt                   # Educational modules
    │   ├── InteractiveExplorersScreens.kt   # Cycle & interactive learning tools
    │   ├── DiversityFeedScreen.kt           # Anatomical diversity gallery
    │   ├── ArticleAndToolsScreens.kt        # Articles & reference tools
    │   ├── SettingsScreen.kt                # App settings, Extra Beta Options & haptics preferences
    │   └── ResourcesScreen.kt               # Curated external links
    └── components/
        ├── InteractiveVulva3DCanvas.kt      # Compose UI wrapper, touch/orbit gestures, sliders & floating Beta Lab
        ├── BlossomViewportSessionState.kt   # Scroll- & tab-persistent animation, pose, camera & Beta Lab state
        ├── VulvaGlRenderer.kt               # EGL14 render thread, GLES 2.0 draw loop, heart-rate model & GLSL shaders
        ├── VulvaGeometryBuilder.kt          # VulvaDeformationState, VulvaAnatomySceneMesh, glans & labiaHoodArchMesh
        ├── BlossomSdfBodyBuilder.kt         # Marching Cubes SDF torso/legs/pelvis + ultra-HD vulva patch projection
        ├── BlossomUpperBodyBuilder.kt       # Head, eyes, 3D eyelashes, bob hair + wisps, posed arms/hands & 3D nails
        ├── BlossomBodyBuilder.kt            # Mattress surface curve, clinical drape cushion & milestone tag
        ├── BlossomArmIkRig.kt               # Analytical arm/hand IK solver for resting & self-spread poses
        ├── BlossomTouchHitResolver.kt       # 3D screen-space ray/projection hit-testing across body zones
        └── AnatomyHapticsEngine.kt          # Tiered tactile feedback engine

docs/
└── 3D_RENDERING_ARCHITECTURE.md             # Deep-dive map of meshes, shader channels, seams & "where to edit X"
```

---

## Building & Testing

This project uses standard Android Gradle (Kotlin DSL).

```bash
# Build debug APK
gradle :app:assembleDebug

# Run all JVM & Robolectric unit tests (geometry, winding, seam, IK, hit-testing)
gradle :app:testDebugUnitTest
```

> **Note for contributors:** All 3D geometry invariants (finite coordinates, unit normals, outward CCW triangle winding, flush vulva-to-body seam alignment, Beta v1 default & Legacy Vulva round-trip restoration) are verified in `app/src/test/java/com/example/BlossomGeometryUnitTest.kt`. Always run `:app:testDebugUnitTest` after editing mesh builders or deformation math.

---

## Deep-Dive Documentation

If you are modifying the 3D anatomy, tweaking colors/shading, or trying to figure out which mesh draws which pixel, start here:

👉 **[`docs/3D_RENDERING_ARCHITECTURE.md`](docs/3D_RENDERING_ARCHITECTURE.md)**

---

## Version Checkpoints

- **Version 29.0 (2026-11-26)**
  - **Head-Locked Facial & Hair Textures (`uInvHeadMatrix`)**: Exact inverse rigid head transform keeps freckles, eyebrows, lip texture, hairline shadow, iris fibers, and hair strands locked to Blossom's head across all head turns/nods.
  - **Refined Base Hair, Lips & Visual Realism**: Softened parted forehead hairline and unified cohesive curved bob silhouette, dialed back hair specular/rim glare, enriched base lips into natural muted rose, refined subtle non-periodic lip texture & softer freckles/brows, and retired *Skin micro-detail & SSS* and *Studio rim & cushion bounce*.
  - **Vulva Beta Option #9 — Upper Hood–Mons Color Blend (`betaV3HoodMonsUpperBlend`)**: OFF-by-default toggle + slider in Tab 0 (`Vulva`) blending the triangular upper hood–mons patch and shoulders seamlessly into mons skin across `blossomVulvaPatchMesh` and `labiaHoodArchMesh` while preserving the glans, inner preputial rim, relief, AO, and wetness.
  - **Bilateral Upper-Inner-Thigh Fold Fix**: Replaced 2D `(x, y)` `ovalR` thigh pinning in `buildLegSkinCache()` with a 3D pelvic collar guard freeing forward inner-thigh tissue during hard peak squeezes, paired with bind-pose depth-guarded pelvic shading masks (`pelvicCoord` + `pelvicBindZGuard`) in Pass A.

- **Version 28.0 (2026-11-26)**
  - **Leg-Locked Dynamic Creases (`betaRealismDynamicCreases`)**: Evaluates thigh/hip-flexion creases and kneecaps in bind-pose `aBodyBindHole` coordinates so they move 100% locked to Blossom's thighs during leg spreads and HR peak squeezes.
  - **Vulva Beta Option #8 — Hood–Mons Skin Blend (`betaV3HoodMonsBlend`)**: OFF-by-default toggle + slider in Tab 0 (`Vulva`) that softly harmonizes dorsal clitoral hood pigmentation into surrounding mons pubis skin while preserving the inner edge and glans.
  - **Upgraded SSS & 62-Lock Kajiya-Kay Hair**: Visibly richer hemoglobin subsurface scattering & backlit rim translucency (`betaRealismSkinDetail`), and 62 sculpted multi-tier volumetric lock cards & backlit flyaways with natural scalp part line and dual-lobe Kajiya-Kay (R + TRT) specular (`betaRealismLayeredHair`).
  - **3 New Visual Realism Toggles (OFF by default in `Life+`)**: *Lip, brow & freckle detail*, *Studio rim & cushion bounce*, and *Areola, nail & dermal detail*.

- **Version 27.0 (2026-11-26)** *(User-marked Good Version)*
  - **5 Photorealistic Visual Realism Upgrades (all independently toggleable in `Life+` Beta Lab, OFF by default):**
    - *Layered hair wisps*: 26 alpha-feathered baby-hair, face-framing, crown-flyaway & pillow-fanned ribbons (**Pass F**), Marschner secondary hair sheen, and forehead/temple hairline contact shadow.
    - *Skin micro-detail & SSS*: close-up facial/body micro-grain, regional capillary undertones (nose tip, ears, shoulders, elbows, knees), and thin-tissue rim SSS (strictly outside vulvar mucosa).
    - *3D lashes & iris depth*: blink- and head-turn-synchronized 3D upper/lower eyelash fans (**Pass G**), refracted opposite-side iris caustics, collarette stroma detail, and wet lower-lid tear-line meniscus.
    - *Cushion contact shadows*: pose-aware soft grounding shadows beneath head, back, glutes, arms (`selfSpreadPose`), and moving calves/heels (`leftLegSpread`/`rightLegSpread`), plus linen weight ripples and posterior skin contact AO.
    - *Dynamic creases & landmarks*: `headTurn`-linked contralateral neck SCM tendon tension, leg-spread/knee-squeeze hip-flexion creases, clavicles, linea alba, kneecaps, and inner-elbow creases.

- **Version 26.0 (2026-11-26)** *(User-marked Good Version)*
  - Added dedicated **Settings** screen with persistent *"Show Extra Beta Options"* toggle, lowered climax vibration cadence to ~1/3 frequency at peak hardware amplitude (`248–255`), and fixed closed-eyelid blue-iris bleed-through by wrapping recessed aperture vertices cleanly over the eyeball sphere.

- **Version 22.0 – 25.0 (2026-11-26)**
  - Promoted Vestibule-Labial Patch Alignment (Option #7), added the 136 BPM 3-phase peak choreography (8s restless pre-event -> 16s asymmetric peak with planted-foot knee shifting & mouth opening -> 125 BPM cooldown), and fixed the Seam D inner-thigh cutout via static bind-pose `aBodyBindHole` coordinates.

- **★ Version 19.0 — Significant Pre-Graduation Milestone (2026-11-26)** *(Pre-baking candidate)*
  - **Promoted Beta Lab defaults**: Vulva options 1–6 (50% / 50% / 85% / 36% / 75% / 65%), vulva elastic resistance at 40% (new 60% max), breast size boost at 45%, all 6 Life behaviors, and all 7 Life+ realism behaviors ON by default (Heart Rate HUD stays OFF by default).
  - **Elasticity-synced self-spread IK**: Blossom's *"Ask Blossom to Spread"* animation caps at the 40% elastic limit (`0.60` inner / `0.56` outer) with matched fingertip glide.
  - **3× slower heart-rate rise**: Gradual sympathetic cardiac climb across continuous touch and tap impulses while preserving resting BPM, ceilings, and recovery.
  - **Scroll-persistent viewport & toggle memory**: Viewport animations, poses, breathing phase, and heart rate persist when scrolling away/back; toggling any Beta option OFF and back ON restores its last slider value.
  - **Compact translucent Beta Lab**: 60% height (`200dp`), ~30% narrower (`222dp`), higher translucency (`0.72` alpha).
  - See [`VERSION_HISTORY.md`](VERSION_HISTORY.md) for the complete changelog.

- **Beta v1.1 — Golden Baseline Checkpoint (2026-11-25)** *(Safe reversion point)*
  - **Rounded finger & toe tips**: true 5-ring hemispherical rounded pads on all 10 fingers and 10 toes with flush fingernails/toenails.
  - **Refined supine breast profile**: continuous teardrop curvature from upper chest wall through soft lower fold, eliminating cylindrical side walls while preserving front look, cleavage, areolae/nipples, and size scaling.
  - **Realistic thoracic ribcage breathing**: full ribcage expansion (anterior, subtle pump-handle superior, and bucket-handle lateral) synchronized with abdominal breathing so breasts ride coherently on the expanding chest wall with zero Z-telescoping.
  - **Includes all Beta v1 features & fixes**: promoted vulva/seam/neck/hair refinements (with buried *Advanced rendering → Legacy vulva* toggle) and GPU VBO/IBO caching + frustum culling to prevent HWUI fence timeouts.

