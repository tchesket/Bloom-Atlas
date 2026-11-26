# Bloom Atlas (Version 19.0 — Pre-Graduation Milestone)

**Bloom Atlas** is an educational Android application built with Kotlin, Jetpack Compose, Material 3, and a custom real-time OpenGL ES 2.0 procedural 3D anatomy engine.

---

## Features

- **Interactive 3D Anatomy Explorer (`Blossom v19.0`)**
  - Procedural full-body reclining anatomical model rendered in real time via a custom `TextureView` + EGL14 / OpenGL ES 2.0 pipeline (no external 3D asset files required).
  - **Touch Tissue mode**: multi-touch elastic deformation of outer/inner labia (with adjustable soft-tissue elastic resistance up to 60%), clitoral hood retraction, breasts, glutes, positional leg spread, head turn/nod, and an elasticity-aligned animated IK self-spread pose (*"Ask Blossom to Spread"*).
  - **Orbit 3D mode**: drag to orbit, pinch to zoom from full-body to macro close-up, and two-finger pan head-to-toe, plus quick camera angle presets.
  - **Real-time appearance & Beta Lab controls**: compact, translucent always-on-top 3-tab **Beta Lab** (`Vulva`, `Life`, `Life+`) with toggle value memory, skin tone presets, hair color swatches, breast size & boost sliders, pubic hair density slider, studio Exam Light vs. raking Side Light, and sensory haptics.
  - **Scroll-persistent physiological animations**: chest/abdomen ribcage breathing, micro-saccades, asymmetric/double blinking, gaze anticipation, subtle swallowing, postural settling, living warmth, and 3× slower sympathetic cardio-respiratory heart-rate dynamics — all preserved across scrolling and tab switches via `BlossomViewportSessionState`.
  - **Legacy compatibility option**: the pre-Beta v1 look (original vulva, seam, neck, and hair rendering) is preserved behind a collapsed-by-default **Advanced rendering → Legacy vulva** switch at the bottom of the 3D explorer panel.
- **Guided Learning & Tools**
  - Illustrated anatomy guides, sensitivity & structure breakdowns, cycle wheel explorer, diversity studies, and curated educational resources.

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
    │   └── ResourcesScreen.kt               # Curated external links
    └── components/
        ├── InteractiveVulva3DCanvas.kt      # Compose UI wrapper, touch/orbit gestures, sliders & floating Beta Lab
        ├── BlossomViewportSessionState.kt   # Scroll- & tab-persistent animation, pose, camera & Beta Lab state (v19.0)
        ├── VulvaGlRenderer.kt               # EGL14 render thread, GLES 2.0 draw loop, heart-rate model & GLSL shaders
        ├── VulvaGeometryBuilder.kt          # VulvaDeformationState, VulvaAnatomySceneMesh, glans & labiaHoodArchMesh
        ├── BlossomSdfBodyBuilder.kt         # Marching Cubes SDF torso/legs/pelvis + ultra-HD vulva patch projection
        ├── BlossomUpperBodyBuilder.kt       # Head, eyelids/eyes, layered bob hair, posed arms/hands & 3D nails
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

