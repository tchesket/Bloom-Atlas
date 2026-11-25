# Bloom Atlas (Beta v1)

**Bloom Atlas** is an educational Android application built with Kotlin, Jetpack Compose, Material 3, and a custom real-time OpenGL ES 2.0 procedural 3D anatomy engine.

---

## Features

- **Interactive 3D Anatomy Explorer (`Blossom Beta v1`)**
  - Procedural full-body reclining anatomical model rendered in real time via a custom `TextureView` + EGL14 / OpenGL ES 2.0 pipeline (no external 3D asset files required).
  - **Touch Tissue mode**: multi-touch elastic deformation of outer/inner labia, clitoral hood retraction, breasts, glutes, positional leg spread, head turn/nod, and an animated IK self-spread pose (*"Ask Blossom to Spread"*).
  - **Orbit 3D mode**: drag to orbit, pinch to zoom from full-body to macro close-up, and two-finger pan head-to-toe, plus quick camera angle presets.
  - **Real-time appearance controls**: skin tone presets, hair color swatches, breast size slider, pubic hair density slider, studio Exam Light vs. raking Side Light, and sensory haptics.
  - **Physiological idle animations**: subtle breathing displacement on the chest/abdomen (in vertex shader space) and natural randomized eyelid blinking.
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
        ├── InteractiveVulva3DCanvas.kt      # Compose UI wrapper, touch/orbit gestures, sliders & Legacy toggle
        ├── VulvaGlRenderer.kt               # EGL14 render thread, GLES 2.0 draw loop & GLSL shaders (Pass A–E)
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

- **Beta v1.1 — Golden Baseline Checkpoint (2026-11-25)** *(Safe reversion point)*
  - **Rounded finger & toe tips**: true 5-ring hemispherical rounded pads on all 10 fingers and 10 toes with flush fingernails/toenails.
  - **Refined supine breast profile**: continuous teardrop curvature from upper chest wall through soft lower fold, eliminating cylindrical side walls while preserving front look, cleavage, areolae/nipples, and size scaling. *(Future optional note: largest breast-size settings could be fine-tuned slightly further later, if desired.)*
  - **Realistic thoracic ribcage breathing**: full ribcage expansion (anterior, subtle pump-handle superior, and bucket-handle lateral) synchronized with abdominal breathing so breasts ride coherently on the expanding chest wall with zero Z-telescoping.
  - **Includes all Beta v1 features & fixes**: promoted vulva/seam/neck/hair refinements (with buried *Advanced rendering → Legacy vulva* toggle) and GPU VBO/IBO caching + frustum culling to prevent HWUI fence timeouts.

