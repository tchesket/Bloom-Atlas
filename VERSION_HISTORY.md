# Bloom Atlas — Version History

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
