package com.example.ui.components

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Unified Smooth-Min Signed Distance Field (SDF) + Surface Nets 3D lower-body generator for Blossom.
 *
 * Architectural purpose:
 *  - Fuses the reclining torso (lower ribcage, hourglass waist, rounded abdomen, navel, hips),
 *    dual-hemisphere buttocks, two-bone bent legs (thighs, sculpted knees, calves, slender ankles),
 *    and flat-resting sculpted feet into ONE unbroken watertight manifold using cubic polynomial
 *    smooth-min blending (`smin`).
 *  - Eliminates all separate-mesh Z-buffer intersection seams across the hips, inguinal folds,
 *    gluteal-to-posterior-thigh junction, and ankles.
 *  - Pairs with an ultra-high-resolution (128x144, ~0.5mm center spacing) deformable anterior
 *    vulva/perineum/anus patch projected directly onto the exact SDF=0 surface with matched
 *    analytical SDF gradient normals for a completely invisible skin transition.
 */
internal object BlossomSdfBodyBuilder {

    const val VULVA_PATCH_COLS = 128
    const val VULVA_PATCH_ROWS = 144

    // Elliptical patch footprint on the anterior pelvis (covers mons +0.72 through lower gluteal-cleft anus -0.86 at 100% detail!)
    const val PATCH_CENTER_Y = -0.06f
    const val PATCH_RX = 0.64f
    const val PATCH_RY = 1.02f

    // Seam A (Geometry) constants — betaSmoothVulvaSeam
    const val SEAM_A_COLLAR_RECESS_START = 0.70f
    const val SEAM_A_COLLAR_RECESS_END = 0.84f
    const val SEAM_A_COLLAR_RECESS_DEPTH = 0.088f
    const val SEAM_A_PERIANAL_RECESS_DEPTH = 0.042f
    const val SEAM_A_BODY_SMOOTH_INNER = 0.84f
    const val SEAM_A_BODY_SMOOTH_FADE_START = 1.12f
    const val SEAM_A_BODY_SMOOTH_OUTER = 1.26f
    const val SEAM_A_BODY_SMOOTH_PASSES = 2
    const val SEAM_A_BODY_SMOOTH_LAMBDA = 0.42f
    const val SEAM_A_POS_FEATHER_START = 0.58f
    const val SEAM_A_POS_FEATHER_END = 0.84f
    const val SEAM_A_ANUS_PROTECT_FADE_START = 0.84f
    const val SEAM_A_ANUS_PROTECT_FADE_END = 0.95f
    const val SEAM_A_MESH_POS_BLEND_START = 0.80f
    const val SEAM_A_MESH_POS_BLEND_END = 0.96f
    const val SEAM_A_NORMAL_BLEND_START = 0.56f
    const val SEAM_A_NORMAL_BLEND_END = 0.84f
    const val SEAM_A_MESH_NORMAL_BLEND_START = 0.80f
    const val SEAM_A_MESH_NORMAL_BLEND_END = 0.96f
    const val SEAM_TARGET_MIN_EDGE_R = 0.70f
    const val SEAM_TRI_MIN_NZ_SUM = 0.02f
    const val SEAM_RAY_MAX_DIST = 0.065f

    // Seam B (Color) constants — betaBlendVulvaSeamColor (widens fade across outer ~40% of patch radius)
    const val SEAM_B_MAT_FEATHER_START = 0.56f
    const val SEAM_B_MAT_FEATHER_END = 0.96f
    const val SEAM_B_ANUS_MAT_FADE_START = 0.84f
    const val SEAM_B_ANUS_MAT_FADE_END = 0.95f
    const val SEAM_B_BODY_MAT_BLEND_START = 0.72f
    const val SEAM_B_BODY_MAT_BLEND_END = 0.96f
    const val BODY_BASE_WETNESS = 0.055f

    private const val MIN_X = -2.96f
    private const val MAX_X = 2.96f
    private const val MIN_Y = -5.92f
    private const val MAX_Y = 6.32f
    private const val MIN_Z = -2.45f
    private const val MAX_Z = 2.40f
    private const val STEP = 0.082f

    private const val LUT_SIZE = 65
    private const val LUT_MIN_X = -0.72f
    private const val LUT_MAX_X = 0.72f
    private const val LUT_MIN_Y = -1.22f
    private const val LUT_MAX_Y = 1.08f

    class AnteriorSurfaceLut(
        val size: Int,
        val zValues: FloatArray,
        val aoValues: FloatArray,
        val pigmentValues: FloatArray
    ) {
        private fun sampleBilinear(table: FloatArray, x: Float, y: Float): Float {
            val u = ((x - LUT_MIN_X) / (LUT_MAX_X - LUT_MIN_X)).coerceIn(0f, 1f) * (size - 1)
            val v = ((y - LUT_MIN_Y) / (LUT_MAX_Y - LUT_MIN_Y)).coerceIn(0f, 1f) * (size - 1)
            val ix0 = u.toInt().coerceIn(0, size - 2)
            val iy0 = v.toInt().coerceIn(0, size - 2)
            val fx = u - ix0
            val fy = v - iy0
            val r0 = iy0 * size + ix0
            val r1 = (iy0 + 1) * size + ix0
            val v0 = table[r0] + (table[r0 + 1] - table[r0]) * fx
            val v1 = table[r1] + (table[r1 + 1] - table[r1]) * fx
            return v0 + (v1 - v0) * fy
        }

        fun sampleZ(x: Float, y: Float): Float = sampleBilinear(zValues, x, y)
        fun sampleAo(x: Float, y: Float): Float = sampleBilinear(aoValues, x, y)
        fun samplePigment(x: Float, y: Float): Float = sampleBilinear(pigmentValues, x, y)
    }

    fun buildAnteriorSurfaceLut(
        betaShortPerineum: Boolean = false,
        betaSoftButtockTable: Boolean = false
    ): AnteriorSurfaceLut {
        val count = LUT_SIZE * LUT_SIZE
        val zTable = FloatArray(count)
        val aoTable = FloatArray(count)
        val pigmentTable = FloatArray(count)
        for (j in 0 until LUT_SIZE) {
            val y = LUT_MIN_Y + (j.toFloat() / (LUT_SIZE - 1)) * (LUT_MAX_Y - LUT_MIN_Y)
            for (i in 0 until LUT_SIZE) {
                val x = LUT_MIN_X + (i.toFloat() / (LUT_SIZE - 1)) * (LUT_MAX_X - LUT_MIN_X)
                val s = sampleBodySurfacePoint(
                    x = x,
                    y = y,
                    betaShortPerineum = betaShortPerineum,
                    betaSoftButtockTable = betaSoftButtockTable
                )
                val idx = j * LUT_SIZE + i
                zTable[idx] = s.z
                aoTable[idx] = s.ao
                pigmentTable[idx] = s.pigment
            }
        }
        return AnteriorSurfaceLut(LUT_SIZE, zTable, aoTable, pigmentTable)
    }

    fun evaluateVulvaPatchMatMask(
        x: Float,
        y: Float,
        betaShortPerineum: Boolean = false,
        betaBlendVulvaSeamColor: Boolean = false
    ): Float {
        val ex = x / PATCH_RX
        val ey = (y - PATCH_CENTER_Y) / PATCH_RY
        val edgeR = sqrt(ex * ex + ey * ey).coerceIn(0f, 1f)
        val anusCenterY = if (betaShortPerineum) -0.782f else -0.86f
        val anusProtect = exp(-((x / 0.17f).let { it * it } + ((y - anusCenterY) / 0.16f).let { it * it }))
        val matFeather = if (betaBlendVulvaSeamColor) {
            1f - quinticStep(SEAM_B_MAT_FEATHER_START, SEAM_B_MAT_FEATHER_END, edgeR)
        } else {
            1f - smoothStep(0.62f, 0.90f, edgeR)
        }
        val anusMatProtect = if (betaBlendVulvaSeamColor) {
            anusProtect * (1f - quinticStep(SEAM_B_ANUS_MAT_FADE_START, SEAM_B_ANUS_MAT_FADE_END, edgeR))
        } else {
            anusProtect
        }
        return (matFeather + (1f - matFeather) * anusMatProtect * 0.82f).coerceIn(0f, 1f)
    }

    class BlossomVulvaPatchCache(
        val cols: Int,
        val rows: Int
    ) {
        val count = cols * rows
        val baseX = FloatArray(count)
        val baseY = FloatArray(count)
        val baseZ = FloatArray(count)
        val baseNx = FloatArray(count)
        val baseNy = FloatArray(count)
        val baseNz = FloatArray(count)
        val baseAo = FloatArray(count)
        val basePigment = FloatArray(count)
        val edgeR = FloatArray(count)

        // Exact body-triangle 3D seam targets populated when Seam A (betaSmoothVulvaSeam) or Seam B (betaBlendVulvaSeamColor) is active
        val seamMeshX = FloatArray(count)
        val seamMeshY = FloatArray(count)
        val seamMeshZ = FloatArray(count)
        val seamMeshNx = FloatArray(count)
        val seamMeshNy = FloatArray(count)
        val seamMeshNz = FloatArray(count)
        val seamMeshAo = FloatArray(count)
        val seamMeshPigment = FloatArray(count)
        val hasSeamMatch = BooleanArray(count)

        fun copyFrom(src: BlossomVulvaPatchCache) {
            System.arraycopy(src.baseX, 0, baseX, 0, count)
            System.arraycopy(src.baseY, 0, baseY, 0, count)
            System.arraycopy(src.baseZ, 0, baseZ, 0, count)
            System.arraycopy(src.baseNx, 0, baseNx, 0, count)
            System.arraycopy(src.baseNy, 0, baseNy, 0, count)
            System.arraycopy(src.baseNz, 0, baseNz, 0, count)
            System.arraycopy(src.baseAo, 0, baseAo, 0, count)
            System.arraycopy(src.basePigment, 0, basePigment, 0, count)
            System.arraycopy(src.edgeR, 0, edgeR, 0, count)
            System.arraycopy(src.seamMeshX, 0, seamMeshX, 0, count)
            System.arraycopy(src.seamMeshY, 0, seamMeshY, 0, count)
            System.arraycopy(src.seamMeshZ, 0, seamMeshZ, 0, count)
            System.arraycopy(src.seamMeshNx, 0, seamMeshNx, 0, count)
            System.arraycopy(src.seamMeshNy, 0, seamMeshNy, 0, count)
            System.arraycopy(src.seamMeshNz, 0, seamMeshNz, 0, count)
            System.arraycopy(src.seamMeshAo, 0, seamMeshAo, 0, count)
            System.arraycopy(src.seamMeshPigment, 0, seamMeshPigment, 0, count)
            System.arraycopy(src.hasSeamMatch, 0, hasSeamMatch, 0, count)
        }
    }

    data class SdfSurfaceSample(
        val z: Float,
        val nx: Float,
        val ny: Float,
        val nz: Float,
        val ao: Float,
        val pigment: Float
    )

    fun mattressSurfaceZ(y: Float): Float = BlossomBodyBuilder.mattressSurfaceZ(y)

    /**
     * Evaluates the exact signed distance (negative inside, positive outside) to Blossom's
     * single unified reclining full body (feet, legs, buttocks, pelvis, abdomen, ribcage,
     * feminine shoulders, reclining arms & palms, and slender neck).
     */
    fun evaluateBodySdf(
        x: Float,
        y: Float,
        z: Float,
        betaShortPerineum: Boolean = false,
        betaSoftButtockTable: Boolean = false
    ): Float {
        val ax = abs(x)

        // ---------------------------------------------------------------------
        // 1. CONTINUOUS RECLINING TORSO (PELVIS -> ABDOMEN -> WAIST -> RIBCAGE -> SHOULDERS)
        // ---------------------------------------------------------------------
        val yc = y.coerceIn(-1.04f, 4.98f)
        val matY = mattressSurfaceZ(yc)
        val upperFill = smoothStep(0.62f, 3.20f, yc)
        // Smooth organic posterior roll across the perineum into the gluteal cleft (zero protruding pouch!)
        val perinealRollZ = -0.34f * smoothStep(-0.34f, -1.04f, yc)
        // Gentle upper-thoracic settling onto exam table while preserving true bony ribcage elevation
        val thoraxSettle = -0.14f * smoothStep(3.35f, 4.92f, yc)
        val zSpine = (matY + 0.56f) + 0.14f * upperFill + perinealRollZ + thoraxSettle

        val rxTorso = torsoRxAt(yc)

        // Realistic supine AP depth: soft lower abdomen settling into slender waist, rising cleanly into bony ribcage!
        val lowerBellyFull = 0.18f * exp(-((yc - 1.38f) / 0.86f).let { it * it })
        val waistDepthDip = -0.045f * exp(-((yc - 2.48f) / 0.56f).let { it * it })
        val thoraxArchDepth = 0.175f * exp(-((yc - 3.68f) / 0.88f).let { it * it })
        val upperChestTaper = -0.22f * smoothStep(4.08f, 4.98f, yc)
        val pelvicDepthTaper = 1f - 0.30f * smoothStep(-0.30f, -1.04f, yc)
        val rzTorso = ((0.62f + lowerBellyFull + waistDepthDip + thoraxArchDepth + upperChestTaper) * pelvicDepthTaper).coerceAtLeast(0.32f)

        // Rounded capsule-capped super-elliptic torso column
        val capR = 0.16f
        val dRadTorso = sdEllipse2D(ax, z - zSpine, (rxTorso - capR).coerceAtLeast(0.14f), (rzTorso - capR).coerceAtLeast(0.14f))
        val dyTorso = max(y - 4.98f, -1.04f - y)
        var dTorso = sqrt(max(dRadTorso, 0f).let { it * it } + max(dyTorso, 0f).let { it * it }) +
            min(max(dRadTorso, dyTorso), 0f) - capR

        // Asymmetric anterior supine soft-tissue contours (costal arch, soft hypogastrium, oblique/iliac flow, calibrated clavicles)
        if (y > 0.15f && y < 5.14f && ax < 1.65f) {
            val frontW = smoothStep(-0.08f, 0.34f, z - zSpine)
            if (frontW > 0.001f) {
                val navelDx = x / 0.075f
                val navelDy = (y - 2.02f) / 0.095f
                val navelR2 = navelDx * navelDx + navelDy * navelDy
                val navelDimple = 0.056f * exp(-navelR2 * 2.0f)
                val navelHood = -0.018f * exp(-((x / 0.105f).let { it * it } + ((y - 2.13f) / 0.056f).let { it * it }))
                val lineaAlba = 0.018f * exp(-((x / 0.085f).let { it * it })) * smoothBell(y, 1.35f, 3.05f, 0.26f)
                // Soft lower-abdominal (hypogastric) feminine fullness + subtle semilunar oblique border + smooth iliac flow
                val hypogastricSoft = -0.022f * exp(-((ax / 0.56f).let { it * it } + ((y - 1.32f) / 0.42f).let { it * it }))
                val semilunarSoft = 0.016f * exp(-(((ax - 0.36f) / 0.16f).let { it * it })) * smoothBell(y, 1.55f, 2.75f, 0.25f)
                val iliacCrest = -0.044f * exp(-(((ax - 1.12f) / 0.38f).let { it * it } + ((y - 1.06f) / 0.48f).let { it * it }))
                // Anatomical costal arch (inverted-V lower rib border from xiphoid down to 10th rib) + subtle epigastric settling
                val costalY = 3.24f - 0.52f * smoothStep(0.08f, 0.96f, ax)
                val costalArch = -0.026f * exp(-((y - costalY) / 0.16f).let { it * it }) * smoothBell(ax, 0.14f, 1.04f, 0.18f)
                val epigastricDip = 0.022f * exp(-((x / 0.24f).let { it * it } + ((y - 2.94f) / 0.24f).let { it * it }))
                // Calibrated sternum midline, graceful S-curved feminine collarbones, infraclavicular hollow & subtle upper pectoral plane above breasts
                val sternalHollow = 0.022f * exp(-((x / 0.15f).let { it * it })) * smoothBell(y, 3.32f, 4.62f, 0.24f)
                val clavY = 4.74f - 0.055f * smoothStep(0.26f, 1.16f, ax) + 0.022f * sin(ax * 2.6f)
                val clavicleRidge = -0.036f * exp(-((y - clavY) / 0.080f).let { it * it }) * smoothBell(ax, 0.16f, 1.18f, 0.16f)
                val supraClavHollow = 0.034f * exp(-((y - (clavY + 0.15f)) / 0.110f).let { it * it }) * smoothBell(ax, 0.24f, 1.04f, 0.18f)
                val infraClavHollow = 0.020f * exp(-((y - (clavY - 0.155f)) / 0.115f).let { it * it }) * smoothBell(ax, 0.26f, 1.06f, 0.18f)
                val pecSweepY = 4.26f + 0.20f * smoothStep(0.22f, 1.16f, ax)
                val pecUpperPlane = -0.017f * exp(-((y - pecSweepY) / 0.165f).let { it * it }) * smoothBell(ax, 0.22f, 1.18f, 0.18f)
                val deltoPecGroove = 0.014f * exp(-(((ax - 1.16f) / 0.10f).let { it * it } + ((y - 4.48f) / 0.19f).let { it * it }))
                val sternalNotch = 0.038f * exp(-((x / 0.14f).let { it * it } + ((y - 4.82f) / 0.11f).let { it * it }))
                dTorso += (navelDimple + navelHood + lineaAlba + hypogastricSoft + semilunarSoft + iliacCrest +
                    costalArch + epigastricDip + sternalHollow + clavicleRidge + supraClavHollow +
                    infraClavHollow + pecUpperPlane + deltoPecGroove + sternalNotch) * frontW
            }
            val backW = smoothStep(0.10f, 0.48f, zSpine - z)
            if (backW > 0.001f) {
                val spineGroove = 0.030f * exp(-((x / 0.15f).let { it * it })) * smoothStep(0.25f, 4.65f, y)
                dTorso += spineGroove * backW
            }
        }

        // ---------------------------------------------------------------------
        // 2. DUAL-HEMISPHERE GLUTEAL MASS (SEAMLESSLY FUSED WITH PELVIS & THIGHS)
        // ---------------------------------------------------------------------
        val dGlute = sdEllipsoid(
            px = ax - 0.78f,
            py = y - (-0.54f),
            pz = z - (-0.65f),
            rx = 0.88f,
            ry = 0.66f,
            rz = 0.56f
        )
        // Natural gluteal cleft cradling the lower anus (-0.86 baseline, or -0.782 with betaShortPerineum: ~20% shorter)
        val cleftShiftY = if (betaShortPerineum) 0.078f else 0f
        val cleftMask = smoothStep(-0.42f + cleftShiftY, -0.88f + cleftShiftY, y) *
            (1f - smoothStep(0.16f, 0.42f, z + 0.42f))
        val cleftIndent = 0.095f * exp(-((x / 0.105f).let { it * it })) * cleftMask
        var dPelvis = smin(dTorso, dGlute, 0.38f) + cleftIndent

        // Beta 2: Subtle anatomical infragluteal fold under each buttock cheek (posterior-inferior only)
        if (betaSoftButtockTable && y in -1.34f..-0.64f && ax in 0.16f..1.38f && z < -0.10f) {
            val foldY = -1.03f + 0.11f * smoothStep(0.28f, 1.18f, ax)
            val foldBand = exp(-((y - foldY) / 0.082f).let { it * it })
            val foldLat = smoothBell(ax, 0.30f, 1.14f, 0.16f)
            val foldPost = 1f - smoothStep(-0.48f, -0.12f, z)
            val foldAboveMat = smoothStep(mattressSurfaceZ(y) + 0.03f, mattressSurfaceZ(y) + 0.15f, z)
            dPelvis += 0.039f * foldBand * foldLat * foldPost * foldAboveMat
        }

        // ---------------------------------------------------------------------
        // 3. TWO-BONE SCULPTED LEGS (SYMMETRICALLY EVALUATED IN +X HALF-SPACE)
        // ---------------------------------------------------------------------
        // Hip core H sits cleanly beside lower mons (+0.26), femur sweeps smoothly to crisp knee K,
        // straight tibia descends through shapely calf to slender ankle A & grounded foot!
        val hx = 1.12f; val hy = 0.26f; val hz = -0.22f
        val kx = 1.80f; val ky = -1.86f; val kz = 1.74f
        val axJ = 1.68f; val ayJ = -4.24f; val azJ = -0.94f

        // 3A. Femur (Hip -> Knee) with sleek convex organic thigh profile & subtle musculature
        val fdx = kx - hx; val fdy = ky - hy; val fdz = kz - hz
        val fLen2 = fdx * fdx + fdy * fdy + fdz * fdz
        val hFem = (((ax - hx) * fdx + (y - hy) * fdy + (z - hz) * fdz) / fLen2).coerceIn(0f, 1f)
        val fpx = ax - (hx + fdx * hFem)
        val fpy = y - (hy + fdy * hFem)
        val fpz = z - (hz + fdz * hFem)
        val fDist = sqrt(fpx * fpx + fpy * fpy + fpz * fpz)
        val femDrop = smoothStep(0.14f, 1.0f, hFem)
        var rFem = 0.72f - 0.26f * femDrop * femDrop
        // Subtle anterior quadriceps fullness & smooth, sleek posterior hamstring contour (zero lump!)
        val midThighW = smoothBell(hFem, 0.14f, 0.78f, 0.16f)
        if (midThighW > 0.001f && fDist > 1e-4f) {
            val antFactor = (fpz / fDist).coerceIn(-1f, 1f)
            rFem += max(0f, antFactor) * 0.036f * midThighW + max(0f, -antFactor) * 0.022f * midThighW
        }
        val dFemur = fDist - rFem

        // 3B. Sleek infragluteal saddle strictly bridging buttock cheek into upper hamstring (zero mid-thigh protrusion!)
        val dGluteBridge = sdCapsule(
            ax, y, z,
            0.96f, -0.44f, -0.58f,
            1.28f, -0.66f, 0.06f,
            0.54f, 0.46f
        )

        // 3C. Crisp anatomical knee (patella dome + subtle descending patellar tendon taper & parapatellar softening)
        val dPatella = sdEllipsoid(
            px = ax - 1.80f,
            py = y - (-1.90f),
            pz = z - 1.91f,
            rx = 0.24f,
            ry = 0.26f,
            rz = 0.22f
        )
        val dPatellarTendon = sdCapsule(
            ax, y, z,
            1.80f, -1.98f, 1.84f,
            1.79f, -2.38f, 1.52f,
            0.155f, 0.135f
        )

        // 3D. Tibia + sleek bone-aligned gastrocnemius calf contour tapering smoothly to slender ankle (zero pointy bump!)
        val tdx = axJ - kx; val tdy = ayJ - ky; val tdz = azJ - kz
        val tLen2 = tdx * tdx + tdy * tdy + tdz * tdz
        val hTib = (((ax - kx) * tdx + (y - ky) * tdy + (z - kz) * tdz) / tLen2).coerceIn(0f, 1f)
        val tpx = ax - (kx + tdx * hTib)
        val tpy = y - (ky + tdy * hTib)
        val tpz = z - (kz + tdz * hTib)
        val tDist = sqrt(tpx * tpx + tpy * tpy + tpz * tpz)
        val calfBelly = exp(-((hTib - 0.34f) / 0.24f).let { it * it })
        var rTib = 0.44f - 0.205f * smoothStep(0.14f, 0.96f, hTib) + 0.042f * calfBelly
        if (calfBelly > 0.001f && tDist > 1e-4f) {
            // Posterior unit direction perpendicular to the diagonal tibia bone (smooth, elongated gastrocnemius curve)
            val postFactor = ((tpy * 0.747f - tpz * 0.664f) / tDist).coerceIn(-1f, 1f)
            rTib += max(0f, postFactor) * 0.062f * calfBelly
        }
        val dTibia = tDist - rTib

        val dUpperLeg = smin(dFemur, dGluteBridge, 0.24f)
        var dLeg = smin(dUpperLeg, dTibia, 0.16f)
        dLeg = smin(dLeg, dPatella, 0.12f)
        dLeg = smin(dLeg, dPatellarTendon, 0.13f)
        // Subtle parapatellar soft depressions flanking the kneecap (sized >= 1.5x STEP so Surface Nets stays silky smooth)
        if (y in -2.35f..-1.55f && z > 1.55f) {
            val paraMed = exp(-(((ax - 1.56f) / 0.14f).let { it * it } + ((y + 1.94f) / 0.18f).let { it * it }))
            val paraLat = exp(-(((ax - 2.04f) / 0.14f).let { it * it } + ((y + 1.94f) / 0.18f).let { it * it }))
            dLeg += 0.017f * (paraMed + paraLat) * smoothStep(1.58f, 1.88f, z)
        }

        // ---------------------------------------------------------------------
        // 4. ANATOMICAL SCULPTED FEET (MALLEOLI + ACHILLES -> SLEEK INSTEP -> ARCH -> OBLIQUE METATARSAL BALL)
        //    Pairs seamlessly with the 10 individually sculpted 3D toes in buildBlossomToesMesh()!
        // ---------------------------------------------------------------------
        // Delicate, anatomically distinct feminine malleoli (medial slightly higher & anterior; lateral slightly lower & posterior)
        val dMedialMalleolus = sdEllipsoid(
            px = ax - 1.565f,
            py = y - (-4.16f),
            pz = z - (-0.855f),
            rx = 0.118f,
            ry = 0.134f,
            rz = 0.118f
        )
        val dLateralMalleolus = sdEllipsoid(
            px = ax - 1.795f,
            py = y - (-4.26f),
            pz = z - (-0.895f),
            rx = 0.114f,
            ry = 0.130f,
            rz = 0.114f
        )
        // Subtle posterior Achilles tendon column anchoring calf cleanly into the heel
        val dAchilles = sdCapsule(
            ax, y, z,
            1.68f, -3.55f, -0.44f,
            1.68f, -4.10f, -0.96f,
            0.125f, 0.115f
        )
        val dHeel = sdEllipsoid(
            px = ax - 1.68f,
            py = y - (-4.06f),
            pz = z - (-0.97f),
            rx = 0.190f,
            ry = 0.210f,
            rz = 0.174f
        )
        // Graceful convex feminine dorsal instep tapering smoothly from ankle crease through midfoot (zero dorsal hollow!)
        val dInstep = sdCapsule(
            ax, y, z,
            1.68f, -4.22f, -0.89f,
            1.81f, -4.88f, -0.972f,
            0.194f, 0.178f
        )
        // Central dorsal metatarsal bridge filling rays 2-4 cleanly so the top of the foot is smoothly convex with zero dimple
        val dCenterMetatarsal = sdCapsule(
            ax, y, z,
            1.76f, -4.56f, -0.955f,
            1.90f, -5.14f, -1.022f,
            0.168f, 0.132f
        )
        // Natural medial & lateral metatarsal rays fanning organically to a graceful oblique toe line
        val dMedialRay = sdCapsule(
            ax, y, z,
            1.71f, -4.64f, -0.985f,
            1.70f, -5.22f, -1.032f,
            0.156f, 0.126f
        )
        val dLateralRay = sdCapsule(
            ax, y, z,
            1.84f, -4.60f, -0.990f,
            2.08f, -5.04f, -1.045f,
            0.146f, 0.108f
        )
        // Transverse metatarsal ball bridging hallux base cleanly to 5th-toe base
        val dMetatarsalPad = sdCapsule(
            ax, y, z,
            1.70f, -5.21f, -1.032f,
            2.08f, -5.03f, -1.045f,
            0.124f, 0.104f
        )
        // Graceful medial longitudinal foot arch underneath (preserves grounded lateral plantar border!)
        val dArchCut = sdEllipsoid(
            px = ax - 1.555f,
            py = y - (-4.68f),
            pz = z - (-1.20f),
            rx = 0.195f,
            ry = 0.36f,
            rz = 0.122f
        )
        var dFoot = smin(dHeel, dInstep, 0.11f)
        dFoot = smin(dFoot, dAchilles, 0.11f)
        dFoot = smin(dFoot, dMedialMalleolus, 0.095f)
        dFoot = smin(dFoot, dLateralMalleolus, 0.095f)
        val dForefootRays = smin(smin(dMedialRay, dCenterMetatarsal, 0.14f), dLateralRay, 0.14f)
        dFoot = smin(dFoot, dForefootRays, 0.13f)
        dFoot = smin(dFoot, dMetatarsalPad, 0.115f)
        dFoot = smax(dFoot, -dArchCut, 0.060f)

        // Seamless ankle-to-foot fusion + gentle retromalleolar hollows flanking the Achilles tendon
        var dFullLimb = smin(dLeg, dFoot, 0.125f)
        if (y in -4.32f..-3.65f && z < -0.58f) {
            val retroMed = exp(-(((ax - 1.535f) / 0.115f).let { it * it } + ((y + 3.96f) / 0.19f).let { it * it } + ((z + 0.84f) / 0.16f).let { it * it }))
            val retroLat = exp(-(((ax - 1.825f) / 0.115f).let { it * it } + ((y + 3.98f) / 0.19f).let { it * it } + ((z + 0.86f) / 0.16f).let { it * it }))
            dFullLimb += 0.019f * (retroMed + retroLat)
        }

        // ---------------------------------------------------------------------
        // 5. GRACEFUL FEMININE SHOULDERS, FULLER ANATOMICAL ARMS, OVAL WRISTS, SCULPTED PALMS & NECK
        //    Natural bicep/forearm fullness proportional to thighs & torso, smooth wrist-to-palm arch, ZERO hip webbing!
        // ---------------------------------------------------------------------
        val zShoulder = mattressSurfaceZ(4.54f) + 0.52f
        val zElbow = mattressSurfaceZ(2.28f) + 0.36f
        val zWrist = mattressSurfaceZ(0.42f) + 0.195f
        val zPalm = mattressSurfaceZ(0.00f) + 0.185f

        val zNeckBase = mattressSurfaceZ(4.86f) + 0.58f
        val zNeckTop = mattressSurfaceZ(5.78f) + 0.80f

        // Smooth diagonal feminine trapezius curving softly from lower neck into rounded acromion
        val dTrapezius = sdCapsule(
            ax, y, z,
            0.30f, 5.10f, zNeckBase - 0.10f,
            1.28f, 4.54f, zShoulder - 0.05f,
            0.25f, 0.27f
        )
        // Anatomical anterior axillary fold (pectoralis-major tendon bridge linking upper chest & Tail of Spence into anterior deltoid)
        val dPecAxilla = sdCapsule(
            ax, y, z,
            0.84f, 4.28f, zShoulder + 0.20f,
            1.34f, 4.45f, zShoulder + 0.05f,
            0.19f, 0.18f
        )
        // Low-profile soft feminine deltoid harmonized organically with ribcage & anterior axillary fold (NO pauldron bulge!)
        val dDeltoid = sdEllipsoid(
            px = ax - 1.29f,
            py = y - 4.50f,
            pz = z - zShoulder,
            rx = 0.26f,
            ry = 0.36f,
            rz = 0.30f
        )
        // Anatomically proportionate upper arm with natural open armpit-to-ribcage gap below axilla (ZERO ribcage webbing!)
        val uax0 = 1.34f; val uay0 = 4.40f; val uaz0 = zShoulder
        val uax1 = 2.26f; val uay1 = 2.28f; val uaz1 = zElbow
        val uadx = uax1 - uax0; val uady = uay1 - uay0; val uadz = uaz1 - uaz0
        val uaLen2 = uadx * uadx + uady * uady + uadz * uadz
        val hUpperArm = (((ax - uax0) * uadx + (y - uay0) * uady + (z - uaz0) * uadz) / uaLen2).coerceIn(0f, 1f)
        val uapx = ax - (uax0 + uadx * hUpperArm)
        val uapy = y - (uay0 + uady * hUpperArm)
        val uapz = z - (uaz0 + uadz * hUpperArm)
        val uaDist = sqrt(uapx * uapx + uapy * uapy + uapz * uapz)
        val bicepFullness = exp(-((hUpperArm - 0.44f) / 0.27f).let { it * it })
        val rUpperArm = 0.338f - 0.030f * smoothStep(0.32f, 1.0f, hUpperArm) + 0.040f * bicepFullness
        val dUpperArm = uaDist - rUpperArm

        // Natural feminine forearm: smoothly narrower than upper arm below elbow, tapering gradually to a balanced oval wrist (zero Popeye bulge or skinny pinch!)
        val fax0 = uax1; val fay0 = uay1; val faz0 = uaz1
        val fax1 = 2.34f; val fay1 = 0.42f; val faz1 = zWrist
        val fadx = fax1 - fax0; val fady = fay1 - fay0; val fadz = faz1 - faz0
        val faLen2 = fadx * fadx + fady * fady + fadz * fadz
        val hForearm = (((ax - fax0) * fadx + (y - fay0) * fady + (z - faz0) * fadz) / faLen2).coerceIn(0f, 1f)
        val fapx = ax - (fax0 + fadx * hForearm)
        val fapy = y - (fay0 + fady * hForearm)
        val fapz = z - (faz0 + fadz * hForearm)
        val faDist = sqrt(fapx * fapx + fapy * fapy + fapz * fapz)
        val forearmBelly = exp(-((hForearm - 0.20f) / 0.22f).let { it * it })
        val rForearmBase = 0.308f - 0.080f * smoothStep(0.08f, 1.0f, hForearm) + 0.022f * forearmBelly
        val distalOvalW = smoothStep(0.38f, 1.0f, hForearm)
        val rForearm = if (distalOvalW > 0.001f && faDist > 1e-4f) {
            val zComp = abs(fapz / faDist)
            rForearmBase * (1f - 0.32f * distalOvalW * zComp * zComp)
        } else {
            rForearmBase
        }
        val dForearm = faDist - rForearm

        // Balanced oval wrist & compact interior carpal core (high-res sculpted parametric palm mesh wraps flush outside with zero voxel blobbiness!)
        val dWristBridge = sdEllipsoid(
            px = ax - 2.365f,
            py = y - 0.26f,
            pz = z - (zWrist * 0.5f + zPalm * 0.5f),
            rx = 0.190f,
            ry = 0.195f,
            rz = 0.130f
        )
        val dPalmCore = sdEllipsoid(
            px = ax - 2.415f,
            py = y - (-0.02f),
            pz = z - zPalm,
            rx = 0.185f,
            ry = 0.245f,
            rz = 0.116f
        )
        // Subtle posterior elbow olecranon landmark + faint anterior cubital fossa crease (preserves v6.0/v6.1 arm widths!)
        val dOlecranon = sdEllipsoid(
            px = ax - 2.28f,
            py = y - 2.26f,
            pz = z - (zElbow - 0.15f),
            rx = 0.155f,
            ry = 0.180f,
            rz = 0.145f
        )
        val dPalm = smin(dPalmCore, dWristBridge, 0.085f)
        var dArmChain = smin(
            smin(dDeltoid, dUpperArm, 0.24f),
            smin(dForearm, dPalm, 0.090f),
            0.12f
        )
        dArmChain = smin(dArmChain, dOlecranon, 0.14f)
        if (y in 2.02f..2.58f && ax > 1.92f && z > zElbow + 0.06f) {
            val cubitalDip = exp(-(((ax - 2.24f) / 0.19f).let { it * it } + ((y - 2.30f) / 0.13f).let { it * it })) *
                smoothStep(zElbow + 0.08f, zElbow + 0.28f, z)
            dArmChain += 0.015f * cubitalDip
        }
        val dShoulderArm = smin(dTrapezius, dArmChain, 0.24f)

        // Classical-length graceful feminine cervical neck with subtle mid-neck taper & lordosis flowing smoothly into skull base
        val neckTClamp = ((y - 4.84f) / 0.94f).coerceIn(0f, 1f)
        val neckWaist = -0.022f * sin(neckTClamp * PI.toFloat())
        val neckLordosisZ = 0.018f * sin(neckTClamp * PI.toFloat())
        var dNeck = sdCapsule(
            ax, y, z - neckLordosisZ,
            0f, 4.84f, zNeckBase,
            0f, 5.78f, zNeckTop,
            0.355f, 0.335f
        ) - neckWaist
        // Calibrated subtle sternocleidomastoid (SCM) V-contour framing the anterior throat gently (resolved cleanly at STEP=0.082)
        if (y in 4.78f..5.68f && ax < 0.54f) {
            val scmTargetX = 0.11f + 0.21f * neckTClamp
            val scmRidge = -0.020f * exp(-((ax - scmTargetX) / 0.085f).let { it * it }) *
                smoothBell(y, 4.88f, 5.56f, 0.12f) * smoothStep(-0.08f, 0.22f, z - (zNeckBase + (zNeckTop - zNeckBase) * neckTClamp))
            dNeck += scmRidge
        }

        // ---------------------------------------------------------------------
        // 6. UNIFIED SEAMLESS FULL-BODY FUSION + EXAM MATTRESS SOFT CONTACT
        // ---------------------------------------------------------------------
        var dBody = smin(dPelvis, dFullLimb, 0.42f)
        // Smooth pectoralis/anterior axillary fold anchored cleanly to upper chest
        dBody = smin(dBody, dPecAxilla, 0.18f)
        // Smooth shoulder/axilla fusion at true anatomical armpit (y=3.88..4.42 ONLY); crisp open separation along lateral ribcage & waist!
        val kArmTorso = 0.22f * smoothStep(3.88f, 4.42f, y) + 0.002f
        dBody = smin(dBody, dShoulderArm, kArmTorso)
        dBody = smin(dBody, dNeck, 0.17f)

        // Softly flatten posterior torso, buttocks, arms, palms & foot soles onto the reclined exam mattress
        val matPlane = (mattressSurfaceZ(y) + 0.005f) - z
        val matBlendK = if (betaSoftButtockTable && y in -1.42f..0.24f && ax < 1.72f) {
            val buttockTableZone = smoothBell(y, -1.06f, -0.08f, 0.34f) * (1f - smoothStep(1.22f, 1.72f, ax))
            0.12f + 0.18f * buttockTableZone
        } else {
            0.12f
        }
        dBody = smax(dBody, matPlane, matBlendK)

        return dBody
    }

    /**
     * Exact half-width of Blossom's continuous torso/ribcage cross-section at vertical coordinate [y].
     */
    fun torsoRxAt(y: Float): Float {
        val yc = y.coerceIn(-1.04f, 4.98f)
        val hipFlare = 0.405f * exp(-((yc - 0.18f) / 1.08f).let { it * it })
        val iliacFull = 0.125f * exp(-((yc - 0.96f) / 0.62f).let { it * it })
        val waistPinch = -0.275f * exp(-((yc - 2.44f) / 0.66f).let { it * it })
        val lowerRibTaper = -0.130f * smoothStep(2.50f, 3.20f, yc) * (1f - smoothStep(3.65f, 4.18f, yc))
        val ribCage = 0.065f * exp(-((yc - 4.02f) / 0.62f).let { it * it })
        val shoulderSpan = 0.05f * exp(-((yc - 4.46f) / 0.44f).let { it * it })
        val shoulderTopTaper = 1f - 0.70f * smoothStep(4.54f, 4.98f, yc)
        val pelvicBaseTaper = 1f - 0.44f * smoothStep(-0.22f, -1.04f, yc)
        return ((1.36f + hipFlare + iliacFull + waistPinch + lowerRibTaper + ribCage + shoulderSpan) *
            pelvicBaseTaper * shoulderTopTaper).coerceAtLeast(0.36f)
    }

    /**
     * Evaluates ONLY Blossom's ribcage/torso + anterior axillary fold SDF (strictly excluding arms),
     * ensuring breast patch projection stays smoothly anchored to the chest wall and NEVER snaps
     * across the open armpit gap onto the inner upper arm!
     */
    fun evaluateTorsoChestSdf(x: Float, y: Float, z: Float): Float {
        val ax = abs(x)
        val yc = y.coerceIn(-1.04f, 4.98f)
        val matY = mattressSurfaceZ(yc)
        val upperFill = smoothStep(0.62f, 3.20f, yc)
        val perinealRollZ = -0.34f * smoothStep(-0.34f, -1.04f, yc)
        val thoraxSettle = -0.14f * smoothStep(3.35f, 4.92f, yc)
        val zSpine = (matY + 0.56f) + 0.14f * upperFill + perinealRollZ + thoraxSettle

        val rxTorso = torsoRxAt(yc)
        val lowerBellyFull = 0.18f * exp(-((yc - 1.38f) / 0.86f).let { it * it })
        val waistDepthDip = -0.045f * exp(-((yc - 2.48f) / 0.56f).let { it * it })
        val thoraxArchDepth = 0.175f * exp(-((yc - 3.68f) / 0.88f).let { it * it })
        val upperChestTaper = -0.22f * smoothStep(4.08f, 4.98f, yc)
        val pelvicDepthTaper = 1f - 0.30f * smoothStep(-0.30f, -1.04f, yc)
        val rzTorso = ((0.62f + lowerBellyFull + waistDepthDip + thoraxArchDepth + upperChestTaper) * pelvicDepthTaper).coerceAtLeast(0.32f)

        val capR = 0.16f
        val dRadTorso = sdEllipse2D(ax, z - zSpine, (rxTorso - capR).coerceAtLeast(0.14f), (rzTorso - capR).coerceAtLeast(0.14f))
        val dyTorso = max(y - 4.98f, -1.04f - y)
        var dTorso = sqrt(max(dRadTorso, 0f).let { it * it } + max(dyTorso, 0f).let { it * it }) +
            min(max(dRadTorso, dyTorso), 0f) - capR

        if (y > 0.15f && y < 5.14f && ax < 1.65f) {
            val frontW = smoothStep(-0.08f, 0.34f, z - zSpine)
            if (frontW > 0.001f) {
                val costalY = 3.24f - 0.52f * smoothStep(0.08f, 0.96f, ax)
                val costalArch = -0.026f * exp(-((y - costalY) / 0.16f).let { it * it }) * smoothBell(ax, 0.14f, 1.04f, 0.18f)
                val epigastricDip = 0.022f * exp(-((x / 0.24f).let { it * it } + ((y - 2.94f) / 0.24f).let { it * it }))
                val sternalHollow = 0.022f * exp(-((x / 0.15f).let { it * it })) * smoothBell(y, 3.32f, 4.62f, 0.24f)
                val clavY = 4.74f - 0.055f * smoothStep(0.26f, 1.16f, ax) + 0.022f * sin(ax * 2.6f)
                val clavicleRidge = -0.036f * exp(-((y - clavY) / 0.080f).let { it * it }) * smoothBell(ax, 0.16f, 1.18f, 0.16f)
                val supraClavHollow = 0.034f * exp(-((y - (clavY + 0.15f)) / 0.110f).let { it * it }) * smoothBell(ax, 0.24f, 1.04f, 0.18f)
                val infraClavHollow = 0.020f * exp(-((y - (clavY - 0.155f)) / 0.115f).let { it * it }) * smoothBell(ax, 0.26f, 1.06f, 0.18f)
                val pecSweepY = 4.26f + 0.20f * smoothStep(0.22f, 1.16f, ax)
                val pecUpperPlane = -0.017f * exp(-((y - pecSweepY) / 0.165f).let { it * it }) * smoothBell(ax, 0.22f, 1.18f, 0.18f)
                val deltoPecGroove = 0.014f * exp(-(((ax - 1.16f) / 0.10f).let { it * it } + ((y - 4.48f) / 0.19f).let { it * it }))
                val sternalNotch = 0.038f * exp(-((x / 0.14f).let { it * it } + ((y - 4.82f) / 0.11f).let { it * it }))
                dTorso += (costalArch + epigastricDip + sternalHollow + clavicleRidge + supraClavHollow +
                    infraClavHollow + pecUpperPlane + deltoPecGroove + sternalNotch) * frontW
            }
        }

        val zShoulder = mattressSurfaceZ(4.54f) + 0.52f
        val dPecAxilla = sdCapsule(
            ax, y, z,
            0.84f, 4.28f, zShoulder + 0.20f,
            1.34f, 4.45f, zShoulder + 0.05f,
            0.19f, 0.18f
        )
        var dChest = smin(dTorso, dPecAxilla, 0.18f)
        val matPlane = (mattressSurfaceZ(y) + 0.005f) - z
        dChest = smax(dChest, matPlane, 0.12f)
        return dChest
    }

    /**
     * Evaluates a smooth, C1-continuous `[0, 1]` arm-skinning ownership weight comparing Blossom's
     * exact arm-chain SDF (`deltoid + upperArm + forearm + palm`) against her torso/ribcage/trapezius SDF.
     *
     * Eliminates hard geometric cutoffs across the medial-posterior upper arm so ZERO upper-arm
     * vertices are left pinned to the exam table when lifting the arms, while keeping the torso,
     * breasts, neck, and legs 100% anchored.
     */
    /**
     * Identifies strictly vertices belonging to Blossom's resting distal forearm, wrist, or
     * palm core below `y <= 0.44f` (anatomically located at `ax >= 2.10, z <= -0.77, y >= -0.34`),
     * sitting cleanly across the ~0.35-unit air gap from the outer upper thigh/hip/glute
     * (`ax <= 1.72` whenever `z < -0.66`). Shared identically by [buildLegSkinCache],
     * [evaluateArmVsTorsoWeight], and [BlossomArmIkRig.buildArmSkinCache] so arm and leg
     * ownership stay 100% disjoint with ZERO clipping of outer upper-thigh skin.
     */
    internal fun isRestingHandOrDistalForearm(ax: Float, y: Float, z: Float): Boolean =
        y in -0.50f..0.44f && ax > 2.04f && z < -0.66f

    fun evaluateArmVsTorsoWeight(x: Float, y: Float, z: Float): Float {
        val ax = abs(x)
        if (ax < 0.96f || y < -0.85f || y > 5.04f) return 0f
        // Strictly disjoint from LegSkinCache via the shared resting-hand corridor
        if (y <= 0.44f && !isRestingHandOrDistalForearm(ax, y, z)) return 0f

        // Exact Torso + Anterior Axillary Fold + Trapezius SDF (matching evaluateBodySdf)
        val yc = y.coerceIn(-1.04f, 4.98f)
        val matY = mattressSurfaceZ(yc)
        val upperFill = smoothStep(0.62f, 3.20f, yc)
        val perinealRollZ = -0.34f * smoothStep(-0.34f, -1.04f, yc)
        val thoraxSettle = -0.14f * smoothStep(3.35f, 4.92f, yc)
        val zSpine = (matY + 0.56f) + 0.14f * upperFill + perinealRollZ + thoraxSettle

        val rxTorso = torsoRxAt(yc)

        val lowerBellyFull = 0.18f * exp(-((yc - 1.38f) / 0.86f).let { it * it })
        val waistDepthDip = -0.045f * exp(-((yc - 2.48f) / 0.56f).let { it * it })
        val thoraxArchDepth = 0.175f * exp(-((yc - 3.68f) / 0.88f).let { it * it })
        val upperChestTaper = -0.22f * smoothStep(4.08f, 4.98f, yc)
        val pelvicDepthTaper = 1f - 0.30f * smoothStep(-0.30f, -1.04f, yc)
        val rzTorso = ((0.62f + lowerBellyFull + waistDepthDip + thoraxArchDepth + upperChestTaper) * pelvicDepthTaper).coerceAtLeast(0.32f)

        val capR = 0.16f
        val dRadTorso = sdEllipse2D(ax, z - zSpine, (rxTorso - capR).coerceAtLeast(0.14f), (rzTorso - capR).coerceAtLeast(0.14f))
        val dyTorso = max(y - 4.98f, -1.04f - y)
        val dTorso = sqrt(max(dRadTorso, 0f).let { it * it } + max(dyTorso, 0f).let { it * it }) +
            min(max(dRadTorso, dyTorso), 0f) - capR

        val zShoulder = mattressSurfaceZ(4.54f) + 0.52f
        val zElbow = mattressSurfaceZ(2.28f) + 0.36f
        val zWrist = mattressSurfaceZ(0.42f) + 0.195f
        val zPalm = mattressSurfaceZ(0.00f) + 0.185f
        val zNeckBase = mattressSurfaceZ(4.86f) + 0.58f

        val dTrapezius = sdCapsule(
            ax, y, z,
            0.30f, 5.10f, zNeckBase - 0.10f,
            1.28f, 4.54f, zShoulder - 0.05f,
            0.25f, 0.27f
        )
        val dPecAxilla = sdCapsule(
            ax, y, z,
            0.84f, 4.28f, zShoulder + 0.20f,
            1.34f, 4.45f, zShoulder + 0.05f,
            0.19f, 0.18f
        )
        val dDeltoid = sdEllipsoid(
            px = ax - 1.29f,
            py = y - 4.50f,
            pz = z - zShoulder,
            rx = 0.26f,
            ry = 0.36f,
            rz = 0.30f
        )

        val uax0 = 1.34f; val uay0 = 4.40f; val uaz0 = zShoulder
        val uax1 = 2.26f; val uay1 = 2.28f; val uaz1 = zElbow
        val uadx = uax1 - uax0; val uady = uay1 - uay0; val uadz = uaz1 - uaz0
        val uaLen2 = uadx * uadx + uady * uady + uadz * uadz
        val hUpperArm = (((ax - uax0) * uadx + (y - uay0) * uady + (z - uaz0) * uadz) / uaLen2).coerceIn(0f, 1f)
        val uapx = ax - (uax0 + uadx * hUpperArm)
        val uapy = y - (uay0 + uady * hUpperArm)
        val uapz = z - (uaz0 + uadz * hUpperArm)
        val uaDist = sqrt(uapx * uapx + uapy * uapy + uapz * uapz)
        val bicepFullness = exp(-((hUpperArm - 0.44f) / 0.27f).let { it * it })
        val rUpperArm = 0.338f - 0.030f * smoothStep(0.32f, 1.0f, hUpperArm) + 0.040f * bicepFullness
        val dUpperArm = uaDist - rUpperArm

        val fax0 = uax1; val fay0 = uay1; val faz0 = uaz1
        val fax1 = 2.34f; val fay1 = 0.42f; val faz1 = zWrist
        val fadx = fax1 - fax0; val fady = fay1 - fay0; val fadz = faz1 - faz0
        val faLen2 = fadx * fadx + fady * fady + fadz * fadz
        val hForearm = (((ax - fax0) * fadx + (y - fay0) * fady + (z - faz0) * fadz) / faLen2).coerceIn(0f, 1f)
        val fapx = ax - (fax0 + fadx * hForearm)
        val fapy = y - (fay0 + fady * hForearm)
        val fapz = z - (faz0 + fadz * hForearm)
        val faDist = sqrt(fapx * fapx + fapy * fapy + fapz * fapz)
        val forearmBelly = exp(-((hForearm - 0.20f) / 0.22f).let { it * it })
        val rForearmBase = 0.308f - 0.080f * smoothStep(0.08f, 1.0f, hForearm) + 0.022f * forearmBelly
        val distalOvalW = smoothStep(0.38f, 1.0f, hForearm)
        val rForearm = if (distalOvalW > 0.001f && faDist > 1e-4f) {
            val zComp = abs(fapz / faDist)
            rForearmBase * (1f - 0.32f * distalOvalW * zComp * zComp)
        } else {
            rForearmBase
        }
        val dForearm = faDist - rForearm

        val dWristBridge = sdEllipsoid(
            px = ax - 2.365f,
            py = y - 0.26f,
            pz = z - (zWrist * 0.5f + zPalm * 0.5f),
            rx = 0.190f,
            ry = 0.195f,
            rz = 0.130f
        )
        val dPalmCore = sdEllipsoid(
            px = ax - 2.415f,
            py = y - (-0.02f),
            pz = z - zPalm,
            rx = 0.185f,
            ry = 0.245f,
            rz = 0.116f
        )
        val dOlecranon = sdEllipsoid(
            px = ax - 2.28f,
            py = y - 2.26f,
            pz = z - (zElbow - 0.15f),
            rx = 0.155f,
            ry = 0.180f,
            rz = 0.145f
        )
        val dPalm = smin(dPalmCore, dWristBridge, 0.085f)
        val dArmChain = smin(
            smin(
                smin(dDeltoid, dUpperArm, 0.24f),
                smin(dForearm, dPalm, 0.090f),
                0.12f
            ),
            dOlecranon,
            0.14f
        )

        val dTorsoAnchor = min(min(dTorso, dTrapezius), dPecAxilla)
        // Smooth SDF ownership transition across true axilla/shoulder crease (ZERO discontinuous cliff & ZERO ribcage-arm stretch!)
        val sdfWeight = smoothStep(+0.34f, -0.34f, dArmChain - dTorsoAnchor)
        val trapGuard = 1f - smoothStep(4.62f, 5.00f, y) * (1f - smoothStep(1.26f, 1.56f, ax))
        val farFade = 1f - smoothStep(0.30f, 0.60f, dArmChain)
        return (sdfWeight * trapGuard * farFade).coerceIn(0f, 1f)
    }

    /**
     * Exact central-difference gradient normal on the SDF surface.
     */
    fun evaluateBodyNormal(
        x: Float,
        y: Float,
        z: Float,
        eps: Float = 0.014f,
        betaShortPerineum: Boolean = false,
        betaSoftButtockTable: Boolean = false
    ): FloatArray {
        val dx = evaluateBodySdf(x + eps, y, z, betaShortPerineum, betaSoftButtockTable) -
            evaluateBodySdf(x - eps, y, z, betaShortPerineum, betaSoftButtockTable)
        val dy = evaluateBodySdf(x, y + eps, z, betaShortPerineum, betaSoftButtockTable) -
            evaluateBodySdf(x, y - eps, z, betaShortPerineum, betaSoftButtockTable)
        val dz = evaluateBodySdf(x, y, z + eps, betaShortPerineum, betaSoftButtockTable) -
            evaluateBodySdf(x, y, z - eps, betaShortPerineum, betaSoftButtockTable)
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        return if (len > 1e-6f) {
            floatArrayOf(dx / len, dy / len, dz / len)
        } else {
            floatArrayOf(0f, 0f, 1f)
        }
    }

    /**
     * Evaluates consistent anatomical ambient occlusion and subtle skin pigment across the
     * unified SDF body so BOTH the Surface Nets mesh and the Ultra-HD vulva patch match 100%.
     */
    fun evaluateBodyMaterial(
        x: Float,
        y: Float,
        z: Float,
        nx: Float,
        ny: Float,
        nz: Float,
        betaShortPerineum: Boolean = false,
        betaSoftButtockTable: Boolean = false
    ): Pair<Float, Float> {
        val ax = abs(x)
        val matZ = mattressSurfaceZ(y)

        // Contact AO where dorsal torso, buttocks & feet rest on the dark clinical exam mattress
        val buttockTableZone = if (betaSoftButtockTable && y in -1.42f..0.24f && ax < 1.72f) {
            smoothBell(y, -1.06f, -0.08f, 0.34f) * (1f - smoothStep(1.22f, 1.72f, ax))
        } else 0f
        val matAoSpan = 0.24f + 0.10f * buttockTableZone
        val matContact = (1f - smoothStep(0.02f, matAoSpan, z - matZ)) * smoothStep(0.10f, 0.72f, -nz)
        val cushionAo = 1f - (0.34f - 0.05f * buttockTableZone) * matContact

        // Navel cavity AO & subtle pigment
        val navelDx = x / 0.072f
        val navelDy = (y - 2.10f) / 0.092f
        val navelR2 = navelDx * navelDx + navelDy * navelDy
        val frontW = smoothStep(0.15f, 0.70f, nz)
        val navelAo = 1f - 0.44f * exp(-navelR2 * 2.4f) * frontW

        // Gluteal cleft AO
        val cleftShiftY = if (betaShortPerineum) 0.078f else 0f
        val cleftMask = smoothStep(-0.34f + cleftShiftY, -0.82f + cleftShiftY, y) *
            (1f - smoothStep(0.05f, 0.45f, z + 0.35f))
        val cleftAo = 1f - 0.44f * exp(-((x / 0.11f).let { it * it })) * cleftMask

        // Beta 2: Subtle soft AO along the infragluteal fold under each buttock
        val infraglutealAo = if (betaSoftButtockTable && y in -1.34f..-0.64f && ax in 0.16f..1.38f && z < -0.10f) {
            val foldY = -1.03f + 0.11f * smoothStep(0.28f, 1.18f, ax)
            val foldBand = exp(-((y - foldY) / 0.088f).let { it * it })
            val foldLat = smoothBell(ax, 0.30f, 1.14f, 0.16f)
            val foldPost = 1f - smoothStep(-0.48f, -0.12f, z)
            val foldAboveMat = smoothStep(matZ + 0.03f, matZ + 0.15f, z)
            1f - 0.17f * foldBand * foldLat * foldPost * foldAboveMat
        } else 1f

        // Subtle oblique inguinal crease tint & AO
        val ingLineY = 0.36f + (ax - 0.40f) * 0.74f
        val ingDip = exp(-((y - ingLineY) / 0.15f).let { it * it }) * smoothBell(ax, 0.42f, 1.24f, 0.24f) * frontW
        val ingAo = 1f - 0.08f * ingDip

        // Back-of-knee popliteal crease AO
        val popDx = (ax - 1.76f) / 0.36f
        val popDy = (y - (-1.82f)) / 0.34f
        val popDz = (z - 1.28f) / 0.36f
        val popMask = exp(-(popDx * popDx + popDy * popDy + popDz * popDz)) * smoothStep(0.05f, 0.65f, -nz)
        val popAo = 1f - 0.26f * popMask

        // Subtle upper-body & joint anatomical AO (axillary folds, clavicles, suprasternal notch, under-chin, true v6.1 elbow, knee & ankle hollows)
        val axillaAo = 1f - 0.20f * exp(-(((ax - 1.18f) / 0.24f).let { it * it } + ((y - 4.28f) / 0.32f).let { it * it }))
        val notchAo = 1f - 0.15f * exp(-((x / 0.14f).let { it * it } + ((y - 4.82f) / 0.11f).let { it * it })) * frontW
        val supraClavAo = 1f - 0.10f * exp(-(((ax - 0.62f) / 0.34f).let { it * it } + ((y - 4.86f) / 0.10f).let { it * it })) * frontW
        val underChinAo = 1f - 0.26f * exp(-((x / 0.38f).let { it * it } + ((y - 5.44f) / 0.22f).let { it * it })) * frontW
        // Aligned to exact v6.1 anterior cubital crease (ax=2.26, y=2.28)
        val cubitalLineY = 2.28f - (ax - 2.26f) * 0.12f
        val elbowAo = 1f - 0.15f * exp(-(((ax - 2.26f) / 0.24f).let { it * it } + ((y - cubitalLineY) / 0.15f).let { it * it })) * frontW
        // Gentle parapatellar & retromalleolar joint contour AO
        val paraKneeAo = 1f - 0.065f * (
            exp(-(((ax - 1.56f) / 0.16f).let { it * it } + ((y + 1.94f) / 0.22f).let { it * it })) +
                exp(-(((ax - 2.04f) / 0.16f).let { it * it } + ((y + 1.94f) / 0.22f).let { it * it }))
            ) * smoothStep(1.52f, 1.86f, z)
        val ankleHollowAo = 1f - 0.10f * (
            exp(-(((ax - 1.535f) / 0.13f).let { it * it } + ((y + 3.96f) / 0.22f).let { it * it })) +
                exp(-(((ax - 1.825f) / 0.13f).let { it * it } + ((y + 3.98f) / 0.22f).let { it * it }))
            ) * (1f - smoothStep(-0.92f, -0.56f, z))

        val ao = (cushionAo * navelAo * cleftAo * infraglutealAo * ingAo * popAo * axillaAo * notchAo * supraClavAo *
            underChinAo * elbowAo * paraKneeAo * ankleHollowAo).coerceIn(0.14f, 1f)

        val kneeWarm = exp(-(((ax - 1.80f) / 0.42f).let { it * it } + ((y + 1.86f) / 0.42f).let { it * it } + ((z - 1.78f) / 0.42f).let { it * it })) * 0.09f
        val shoulderWarm = exp(-(((ax - 1.32f) / 0.40f).let { it * it } + ((y - 4.50f) / 0.40f).let { it * it })) * 0.05f
        val elbowWarm = exp(-(((ax - 2.28f) / 0.30f).let { it * it } + ((y - 2.26f) / 0.30f).let { it * it })) * 0.055f * (1f - frontW * 0.55f)
        val ankleWarm = exp(-(((ax - 1.68f) / 0.34f).let { it * it } + ((y + 4.18f) / 0.32f).let { it * it })) * 0.045f
        val toeWarm = smoothStep(-5.15f, -5.65f, y) * 0.09f
        val pigment = (ingDip * 0.08f + exp(-navelR2 * 2.0f) * 0.16f * frontW +
            kneeWarm + shoulderWarm + elbowWarm + ankleWarm + toeWarm).coerceIn(0f, 1f)

        return Pair(ao, pigment)
    }

    /**
     * Exact ray-march + Newton-Raphson front-surface sampler on the unified SDF body
     * for any (x, y) inside the anterior vulva/perineum/anus region.
     */
    fun sampleBodySurfacePoint(
        x: Float,
        y: Float,
        betaShortPerineum: Boolean = false,
        betaSoftButtockTable: Boolean = false
    ): SdfSurfaceSample {
        // Start comfortably in front of the anterior pelvis & inner thighs and sphere-trace backward along -Z
        var z = 1.40f
        for (step in 0 until 40) {
            val d = evaluateBodySdf(x, y, z, betaShortPerineum, betaSoftButtockTable)
            if (abs(d) < 0.0006f) break
            z -= d.coerceIn(-0.22f, 0.38f)
            if (z < -1.30f) {
                z = -1.12f
                break
            }
        }
        // 5 Newton-Raphson polish iterations along Z for < 0.0001mm surface precision
        for (it in 0 until 5) {
            val d = evaluateBodySdf(x, y, z, betaShortPerineum, betaSoftButtockTable)
            if (abs(d) < 0.0001f) break
            val dz = (evaluateBodySdf(x, y, z + 0.006f, betaShortPerineum, betaSoftButtockTable) -
                evaluateBodySdf(x, y, z - 0.006f, betaShortPerineum, betaSoftButtockTable)) / 0.012f
            if (abs(dz) > 0.10f) {
                z -= (d / dz).coerceIn(-0.08f, 0.08f)
            } else {
                z -= d * 0.5f
            }
        }
        val n = evaluateBodyNormal(
            x = x,
            y = y,
            z = z,
            betaShortPerineum = betaShortPerineum,
            betaSoftButtockTable = betaSoftButtockTable
        )
        val (ao, pigment) = evaluateBodyMaterial(
            x = x,
            y = y,
            z = z,
            nx = n[0],
            ny = n[1],
            nz = n[2],
            betaShortPerineum = betaShortPerineum,
            betaSoftButtockTable = betaSoftButtockTable
        )
        return SdfSurfaceSample(z, n[0], n[1], n[2], ao, pigment)
    }

    /**
     * Exact ray-march + Newton-Raphson front-surface sampler strictly on Blossom's ribcage/chest wall
     * (excluding the arm chain so breast patch edges never snap onto the inner upper arm across the armpit!).
     */
    fun sampleTorsoChestSurfacePoint(x: Float, y: Float): SdfSurfaceSample {
        var z = 1.40f
        for (step in 0 until 40) {
            val d = evaluateTorsoChestSdf(x, y, z)
            if (abs(d) < 0.0006f) break
            z -= d.coerceIn(-0.22f, 0.38f)
            if (z < -1.80f) {
                z = mattressSurfaceZ(y) + 0.04f
                break
            }
        }
        for (it in 0 until 5) {
            val d = evaluateTorsoChestSdf(x, y, z)
            if (abs(d) < 0.0001f) break
            val dz = (evaluateTorsoChestSdf(x, y, z + 0.006f) - evaluateTorsoChestSdf(x, y, z - 0.006f)) / 0.012f
            if (abs(dz) > 0.10f) {
                z -= (d / dz).coerceIn(-0.08f, 0.08f)
            } else {
                z -= d * 0.5f
            }
        }
        val eps = 0.014f
        val dx = evaluateTorsoChestSdf(x + eps, y, z) - evaluateTorsoChestSdf(x - eps, y, z)
        val dy = evaluateTorsoChestSdf(x, y + eps, z) - evaluateTorsoChestSdf(x, y - eps, z)
        val dz = evaluateTorsoChestSdf(x, y, z + eps) - evaluateTorsoChestSdf(x, y, z - eps)
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-6f)
        val nx = dx / len
        val ny = dy / len
        val nz = dz / len
        val (ao, pigment) = evaluateBodyMaterial(x, y, z, nx, ny, nz)
        return SdfSurfaceSample(z, nx, ny, nz, ao, pigment)
    }

    data class WrappedChestSample(
        val x: Float,
        val y: Float,
        val z: Float,
        val nx: Float,
        val ny: Float,
        val nz: Float,
        val ao: Float,
        val pigment: Float
    )

    /**
     * Smooth C1 radial-elliptical projection of unwarped breast patch coordinates `(xRaw, y)`
     * strictly onto Blossom's arm-free torso/ribcage SDF (`evaluateTorsoChestSdf`):
     *  - Exact 1:1 `(x == xRaw)` across the entire breast interior (`|xRaw| <= 0.78 * rxTorso`)
     *    so the breast dome keeps its exact unpinched, unwarped anatomical shape.
     *  - Smooth C1 wrap around the curved lateral ribcage for the outer collar (`|xRaw| > 0.78 * rxTorso`),
     *    eliminating all armpit arm-snapping triangles AND hard lateral clamp ridges!
     */
    fun sampleTorsoChestWallWrapped(xRaw: Float, y: Float): WrappedChestSample {
        val yc = y.coerceIn(-1.04f, 4.98f)
        val matY = mattressSurfaceZ(yc)
        val upperFill = smoothStep(0.62f, 3.20f, yc)
        val perinealRollZ = -0.34f * smoothStep(-0.34f, -1.04f, yc)
        val thoraxSettle = -0.14f * smoothStep(3.35f, 4.92f, yc)
        val zSpine = (matY + 0.56f) + 0.14f * upperFill + perinealRollZ + thoraxSettle

        val rxTorso = torsoRxAt(yc)
        val lowerBellyFull = 0.18f * exp(-((yc - 1.38f) / 0.86f).let { it * it })
        val waistDepthDip = -0.045f * exp(-((yc - 2.48f) / 0.56f).let { it * it })
        val thoraxArchDepth = 0.175f * exp(-((yc - 3.68f) / 0.88f).let { it * it })
        val upperChestTaper = -0.22f * smoothStep(4.08f, 4.98f, yc)
        val pelvicDepthTaper = 1f - 0.30f * smoothStep(-0.30f, -1.04f, yc)
        val rzTorso = ((0.62f + lowerBellyFull + waistDepthDip + thoraxArchDepth + upperChestTaper) * pelvicDepthTaper).coerceAtLeast(0.32f)

        val side = if (xRaw >= 0f) 1f else -1f
        val u = (abs(xRaw) / rxTorso.coerceAtLeast(0.36f)).coerceAtLeast(0f)
        val uSplit = 0.78f
        val theta = if (u <= uSplit) {
            asin(u)
        } else {
            val theta0 = asin(uSplit)
            val slope0 = 1f / sqrt(1f - uSplit * uSplit)
            val thetaMax = 1.22f
            val room = thetaMax - theta0
            theta0 + room * tanh((u - uSplit) * (slope0 / room))
        }

        // Elliptical ray from (0, y, zSpine) passing directly through (side * rx * sin(theta), y, zSpine + rz * cos(theta))
        val dirX = side * rxTorso * sin(theta)
        val dirZ = rzTorso * cos(theta)
        val dirLen = sqrt(dirX * dirX + dirZ * dirZ).coerceAtLeast(1e-5f)
        val ux = dirX / dirLen
        val uz = dirZ / dirLen

        var r = max(rxTorso, rzTorso) + 0.65f
        for (step in 0 until 42) {
            val px = r * ux
            val pz = zSpine + r * uz
            val d = evaluateTorsoChestSdf(px, y, pz)
            if (abs(d) < 0.0005f) break
            r = (r - d.coerceIn(-0.18f, 0.34f)).coerceAtLeast(0.08f)
        }
        for (it in 0 until 5) {
            val px = r * ux
            val pz = zSpine + r * uz
            val d = evaluateTorsoChestSdf(px, y, pz)
            if (abs(d) < 0.0001f) break
            val dr = (evaluateTorsoChestSdf((r + 0.006f) * ux, y, zSpine + (r + 0.006f) * uz) -
                evaluateTorsoChestSdf((r - 0.006f) * ux, y, zSpine + (r - 0.006f) * uz)) / 0.012f
            if (abs(dr) > 0.10f) {
                r = (r - (d / dr).coerceIn(-0.08f, 0.08f)).coerceAtLeast(0.08f)
            } else {
                r = (r - d * 0.5f).coerceAtLeast(0.08f)
            }
        }

        val px = r * ux
        val pz = zSpine + r * uz
        val eps = 0.014f
        val dx = evaluateTorsoChestSdf(px + eps, y, pz) - evaluateTorsoChestSdf(px - eps, y, pz)
        val dy = evaluateTorsoChestSdf(px, y + eps, pz) - evaluateTorsoChestSdf(px, y - eps, pz)
        val dz = evaluateTorsoChestSdf(px, y, pz + eps) - evaluateTorsoChestSdf(px, y, pz - eps)
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-6f)
        val nx = dx / len
        val ny = dy / len
        val nz = dz / len
        val (ao, pigment) = evaluateBodyMaterial(px, y, pz, nx, ny, nz)
        return WrappedChestSample(px, y, pz, nx, ny, nz, ao, pigment)
    }

    /**
     * Extracts Blossom's single continuous watertight 3D lower body using two-pass narrow-band
     * Surface Nets + tangential Laplacian smoothing + exact SDF projection & gradient normals.
     */
    fun buildUnifiedBodyMesh(): DynamicGlMesh {
        val nx = ((MAX_X - MIN_X) / STEP).toInt()
        val ny = ((MAX_Y - MIN_Y) / STEP).toInt()
        val nz = ((MAX_Z - MIN_Z) / STEP).toInt()
        val gx = nx + 1
        val gy = ny + 1
        val gz = nz + 1

        // Pass 1: 2x coarse grid evaluation to skip empty air/deep interior quickly
        val cStep = STEP * 2f
        val cnx = (nx + 1) / 2 + 1
        val cny = (ny + 1) / 2 + 1
        val cnz = (nz + 1) / 2 + 1
        val coarse = FloatArray(cnx * cny * cnz)
        for (ck in 0 until cnz) {
            val pz = MIN_Z + ck * cStep
            val kOff = ck * cny * cnx
            for (cj in 0 until cny) {
                val py = MIN_Y + cj * cStep
                val jOff = kOff + cj * cnx
                for (ci in 0 until cnx) {
                    val px = MIN_X + ci * cStep
                    coarse[jOff + ci] = evaluateBodySdf(px, py, pz)
                }
            }
        }

        // Pass 2: Evaluate fine SDF only within narrow band around surface (|coarse| < 0.44)
        val grid = FloatArray(gx * gy * gz)
        val bandThreshold = cStep * 2.4f
        for (k in 0 until gz) {
            val pz = MIN_Z + k * STEP
            val ck = (k / 2).coerceAtMost(cnz - 1)
            val ck1 = min(ck + 1, cnz - 1)
            val kOff = k * gy * gx
            for (j in 0 until gy) {
                val py = MIN_Y + j * STEP
                val cj = (j / 2).coerceAtMost(cny - 1)
                val cj1 = min(cj + 1, cny - 1)
                val jOff = kOff + j * gx
                for (i in 0 until gx) {
                    val ci = (i / 2).coerceAtMost(cnx - 1)
                    val ci1 = min(ci + 1, cnx - 1)
                    val cVal = coarse[(ck * cny + cj) * cnx + ci]
                    val nearSurface = abs(cVal) < bandThreshold ||
                        abs(coarse[(ck * cny + cj) * cnx + ci1]) < bandThreshold ||
                        abs(coarse[(ck * cny + cj1) * cnx + ci]) < bandThreshold ||
                        abs(coarse[(ck1 * cny + cj) * cnx + ci]) < bandThreshold
                    grid[jOff + i] = if (nearSurface) {
                        evaluateBodySdf(MIN_X + i * STEP, py, pz)
                    } else {
                        cVal
                    }
                }
            }
        }

        fun gIdx(i: Int, j: Int, k: Int): Int = (k * gy + j) * gx + i
        fun cIdx(i: Int, j: Int, k: Int): Int = (k * ny + j) * nx + i

        // Step 3: Emit 1 Surface Nets vertex per sign-changing cell
        val cellVert = IntArray(nx * ny * nz) { -1 }
        val vxList = FloatArrayList(12000)
        val vyList = FloatArrayList(12000)
        val vzList = FloatArrayList(12000)

        val edgeA = intArrayOf(0, 2, 4, 6, 0, 1, 4, 5, 0, 1, 2, 3)
        val edgeB = intArrayOf(1, 3, 5, 7, 2, 3, 6, 7, 4, 5, 6, 7)
        val cornerDx = intArrayOf(0, 1, 0, 1, 0, 1, 0, 1)
        val cornerDy = intArrayOf(0, 0, 1, 1, 0, 0, 1, 1)
        val cornerDz = intArrayOf(0, 0, 0, 0, 1, 1, 1, 1)
        val cv = FloatArray(8)

        for (k in 0 until nz) {
            val z0 = MIN_Z + k * STEP
            for (j in 0 until ny) {
                val y0 = MIN_Y + j * STEP
                for (i in 0 until nx) {
                    val v0 = grid[gIdx(i, j, k)]
                    val v1 = grid[gIdx(i + 1, j, k)]
                    val v2 = grid[gIdx(i, j + 1, k)]
                    val v3 = grid[gIdx(i + 1, j + 1, k)]
                    val v4 = grid[gIdx(i, j, k + 1)]
                    val v5 = grid[gIdx(i + 1, j, k + 1)]
                    val v6 = grid[gIdx(i, j + 1, k + 1)]
                    val v7 = grid[gIdx(i + 1, j + 1, k + 1)]

                    var insideCount = 0
                    if (v0 < 0f) insideCount++
                    if (v1 < 0f) insideCount++
                    if (v2 < 0f) insideCount++
                    if (v3 < 0f) insideCount++
                    if (v4 < 0f) insideCount++
                    if (v5 < 0f) insideCount++
                    if (v6 < 0f) insideCount++
                    if (v7 < 0f) insideCount++
                    if (insideCount == 0 || insideCount == 8) continue

                    cv[0] = v0; cv[1] = v1; cv[2] = v2; cv[3] = v3
                    cv[4] = v4; cv[5] = v5; cv[6] = v6; cv[7] = v7

                    var sx = 0f
                    var sy = 0f
                    var sz = 0f
                    var crossCount = 0
                    for (e in 0 until 12) {
                        val a = edgeA[e]
                        val b = edgeB[e]
                        val da = cv[a]
                        val db = cv[b]
                        if ((da < 0f) != (db < 0f)) {
                            val t = (da / (da - db)).coerceIn(0.05f, 0.95f)
                            sx += cornerDx[a] + (cornerDx[b] - cornerDx[a]) * t
                            sy += cornerDy[a] + (cornerDy[b] - cornerDy[a]) * t
                            sz += cornerDz[a] + (cornerDz[b] - cornerDz[a]) * t
                            crossCount++
                        }
                    }
                    val invC = 1f / crossCount.coerceAtLeast(1)
                    val x0 = MIN_X + i * STEP
                    val vId = vxList.size
                    vxList.add(x0 + sx * invC * STEP)
                    vyList.add(y0 + sy * invC * STEP)
                    vzList.add(z0 + sz * invC * STEP)
                    cellVert[cIdx(i, j, k)] = vId
                }
            }
        }

        val vCount = vxList.size
        val vx = vxList.toTrimmedArray()
        val vy = vyList.toTrimmedArray()
        val vz = vzList.toTrimmedArray()

        // Step 4: Collect consistently-oriented outward quads around every sign-changing interior edge
        val quadList = IntArrayList(vCount * 4)

        // X-axis interior edges: (i, j, k) -> (i+1, j, k)
        for (k in 1 until nz) {
            for (j in 1 until ny) {
                for (i in 0 until nx) {
                    val d0 = grid[gIdx(i, j, k)]
                    val d1 = grid[gIdx(i + 1, j, k)]
                    if ((d0 < 0f) == (d1 < 0f)) continue
                    val q00 = cellVert[cIdx(i, j - 1, k - 1)]
                    val q10 = cellVert[cIdx(i, j, k - 1)]
                    val q11 = cellVert[cIdx(i, j, k)]
                    val q01 = cellVert[cIdx(i, j - 1, k)]
                    if (q00 < 0 || q10 < 0 || q11 < 0 || q01 < 0) continue
                    if (d0 < 0f) {
                        quadList.add4(q00, q10, q11, q01)
                    } else {
                        quadList.add4(q00, q01, q11, q10)
                    }
                }
            }
        }

        // Y-axis interior edges: (i, j, k) -> (i, j+1, k)
        for (k in 1 until nz) {
            for (j in 0 until ny) {
                for (i in 1 until nx) {
                    val d0 = grid[gIdx(i, j, k)]
                    val d1 = grid[gIdx(i, j + 1, k)]
                    if ((d0 < 0f) == (d1 < 0f)) continue
                    val q00 = cellVert[cIdx(i - 1, j, k - 1)]
                    val q01 = cellVert[cIdx(i - 1, j, k)]
                    val q11 = cellVert[cIdx(i, j, k)]
                    val q10 = cellVert[cIdx(i, j, k - 1)]
                    if (q00 < 0 || q01 < 0 || q11 < 0 || q10 < 0) continue
                    if (d0 < 0f) {
                        quadList.add4(q00, q01, q11, q10)
                    } else {
                        quadList.add4(q00, q10, q11, q01)
                    }
                }
            }
        }

        // Z-axis interior edges: (i, j, k) -> (i, j, k+1)
        for (k in 0 until nz) {
            for (j in 1 until ny) {
                for (i in 1 until nx) {
                    val d0 = grid[gIdx(i, j, k)]
                    val d1 = grid[gIdx(i, j, k + 1)]
                    if ((d0 < 0f) == (d1 < 0f)) continue
                    val q00 = cellVert[cIdx(i - 1, j - 1, k)]
                    val q10 = cellVert[cIdx(i, j - 1, k)]
                    val q11 = cellVert[cIdx(i, j, k)]
                    val q01 = cellVert[cIdx(i - 1, j, k)]
                    if (q00 < 0 || q10 < 0 || q11 < 0 || q01 < 0) continue
                    if (d0 < 0f) {
                        quadList.add4(q00, q10, q11, q01)
                    } else {
                        quadList.add4(q00, q01, q11, q10)
                    }
                }
            }
        }

        val quadCount = quadList.size / 4
        val quads = quadList.data

        // Step 5: Two passes of neighbor Laplacian smoothing + exact SDF=0 Newton projection!
        // Removes every trace of voxel stair-stepping while keeping exact sculpted anatomical shape!
        val accX = FloatArray(vCount)
        val accY = FloatArray(vCount)
        val accZ = FloatArray(vCount)
        val accN = IntArray(vCount)

        repeat(2) {
            accX.fill(0f)
            accY.fill(0f)
            accZ.fill(0f)
            accN.fill(0)
            for (q in 0 until quadCount) {
                val base = q * 4
                val a = quads[base]
                val b = quads[base + 1]
                val c = quads[base + 2]
                val d = quads[base + 3]

                accX[a] += vx[b] + vx[d]; accY[a] += vy[b] + vy[d]; accZ[a] += vz[b] + vz[d]; accN[a] += 2
                accX[b] += vx[a] + vx[c]; accY[b] += vy[a] + vy[c]; accZ[b] += vz[a] + vz[c]; accN[b] += 2
                accX[c] += vx[b] + vx[d]; accY[c] += vy[b] + vy[d]; accZ[c] += vz[b] + vz[d]; accN[c] += 2
                accX[d] += vx[c] + vx[a]; accY[d] += vy[c] + vy[a]; accZ[d] += vz[c] + vz[a]; accN[d] += 2
            }
            for (v in 0 until vCount) {
                val cnt = accN[v]
                if (cnt > 0) {
                    val inv = 1f / cnt
                    var px = vx[v] * 0.55f + (accX[v] * inv) * 0.45f
                    var py = vy[v] * 0.55f + (accY[v] * inv) * 0.45f
                    var pz = vz[v] * 0.55f + (accZ[v] * inv) * 0.45f
                    // Project back onto exact SDF = 0 isosurface
                    val dVal = evaluateBodySdf(px, py, pz)
                    val norm = evaluateBodyNormal(px, py, pz, eps = 0.016f)
                    val stepBack = dVal.coerceIn(-0.06f, 0.06f)
                    px -= norm[0] * stepBack
                    py -= norm[1] * stepBack
                    pz -= norm[2] * stepBack
                    vx[v] = px
                    vy[v] = py
                    vz[v] = pz
                }
            }
        }

        // Step 6: Compute exact analytical SDF normals + body materials and filter interior vulva-core quads
        val vNx = FloatArray(vCount)
        val vNy = FloatArray(vCount)
        val vNz = FloatArray(vCount)
        val insideVulvaCore = BooleanArray(vCount)

        for (v in 0 until vCount) {
            val px = vx[v]
            val py = vy[v]
            val pz = vz[v]
            val norm = evaluateBodyNormal(px, py, pz, eps = 0.014f)
            vNx[v] = norm[0]
            vNy[v] = norm[1]
            vNz[v] = norm[2]

            val ovalR = sqrt((px / PATCH_RX).let { it * it } + ((py - PATCH_CENTER_Y) / PATCH_RY).let { it * it })
            // Smoothly recess the SDF body collar behind the Ultra-HD vulva patch across ovalR < 0.94 so
            // ZERO coarse SDF voxel blocks can EVER poke through negative groin/vestibule/anus dips!
            val isAnteriorFace = pz > -0.85f && norm[2] > 0.08f
            if (isAnteriorFace && ovalR < 0.94f) {
                val collarRecess = 0.090f * (1f - smoothStep(0.76f, 0.94f, ovalR))
                vx[v] = px - norm[0] * collarRecess
                vy[v] = py - norm[1] * collarRecess
                vz[v] = pz - norm[2] * collarRecess
            }
            // Omit quads strictly inside the recessed vulva & perianal core
            insideVulvaCore[v] = ovalR < 0.80f && isAnteriorFace
        }

        val triIndices = ShortArrayList(quadCount * 6)
        for (q in 0 until quadCount) {
            val base = q * 4
            val a = quads[base]
            val b = quads[base + 1]
            val c = quads[base + 2]
            val d = quads[base + 3]
            if (insideVulvaCore[a] && insideVulvaCore[b] && insideVulvaCore[c] && insideVulvaCore[d]) {
                continue
            }
            // Split along shorter diagonal for cleaner organic shading
            val dAC = sqDist(vx[a], vy[a], vz[a], vx[c], vy[c], vz[c])
            val dBD = sqDist(vx[b], vy[b], vz[b], vx[d], vy[d], vz[d])
            if (dAC <= dBD) {
                triIndices.add6(a, b, c, a, c, d)
            } else {
                triIndices.add6(a, b, d, b, c, d)
            }
        }

        val mesh = DynamicGlMesh(vCount, triIndices.size)
        mesh.setIndices(triIndices.toTrimmedArray())

        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        for (v in 0 until vCount) {
            val px = vx[v]
            val py = vy[v]
            val pz = vz[v]
            val nxV = vNx[v]
            val nyV = vNy[v]
            val nzV = vNz[v]
            val (ao, pigment) = evaluateBodyMaterial(px, py, pz, nxV, nyV, nzV)

            val idx = v * stride
            data[idx] = px
            data[idx + 1] = py
            data[idx + 2] = pz
            data[idx + 3] = nxV
            data[idx + 4] = nyV
            data[idx + 5] = nzV
            data[idx + 6] = 0f
            data[idx + 7] = 0.055f
            data[idx + 8] = ao
            data[idx + 9] = pigment
        }

        mesh.commitVertices()
        return mesh
    }

    internal data class BlossomToeSpec(
        val rootAx: Float,
        val rootY: Float,
        val rootZ: Float,
        val length: Float,
        val rLat: Float,
        val rZ: Float,
        val yawOut: Float,
        val archLift: Float
    )

    // Gracefully clustered feminine Egyptian toe cascade (natural hallux -> gently curled lesser toes, ZERO splayed pegs!)
    internal val BLOSSOM_TOE_SPECS: List<BlossomToeSpec> = listOf(
        BlossomToeSpec(rootAx = 1.676f, rootY = -5.22f, rootZ = -1.034f, length = 0.405f, rLat = 0.077f, rZ = 0.063f, yawOut = 0.085f, archLift = 0.012f),
        BlossomToeSpec(rootAx = 1.812f, rootY = -5.19f, rootZ = -1.040f, length = 0.375f, rLat = 0.049f, rZ = 0.049f, yawOut = 0.105f, archLift = 0.024f),
        BlossomToeSpec(rootAx = 1.918f, rootY = -5.14f, rootZ = -1.044f, length = 0.320f, rLat = 0.046f, rZ = 0.046f, yawOut = 0.115f, archLift = 0.025f),
        BlossomToeSpec(rootAx = 2.018f, rootY = -5.08f, rootZ = -1.048f, length = 0.268f, rLat = 0.043f, rZ = 0.043f, yawOut = 0.095f, archLift = 0.022f),
        BlossomToeSpec(rootAx = 2.114f, rootY = -5.01f, rootZ = -1.052f, length = 0.215f, rLat = 0.040f, rZ = 0.039f, yawOut = 0.045f, archLift = 0.017f)
    )

    internal fun toeTipStartS(spec: BlossomToeSpec, isHallux: Boolean): Float {
        val totalLen = spec.length + 0.095f
        // Slightly oblate blunt-hemispherical human toe pad (never pointy!)
        val tipCapLen = spec.rLat * (if (isHallux) 0.90f else 0.95f)
        return (1f - tipCapLen / totalLen).coerceIn(0.80f, 0.90f)
    }

    internal fun toeRowParameterS(r: Int, rows: Int, spec: BlossomToeSpec, isHallux: Boolean): Float {
        val tipStart = toeTipStartS(spec, isHallux)
        val shaftLastRow = rows - 6 // 5 smooth hemisphere cap rings across the rounded tip
        if (r <= shaftLastRow) {
            return (r.toFloat() / shaftLastRow) * tipStart
        }
        val capAnglesDeg = floatArrayOf(20f, 40f, 60f, 78f, 90f)
        val deg = capAnglesDeg[(r - shaftLastRow - 1).coerceIn(0, capAnglesDeg.lastIndex)]
        val rad = deg * (PI.toFloat() / 180f)
        return tipStart + sin(rad) * (1f - tipStart)
    }

    internal fun toeRadiusProfile(s: Float, spec: BlossomToeSpec, isHallux: Boolean): Pair<Float, Float> {
        val tipStart = toeTipStartS(spec, isHallux)
        val tipAngle: Float
        val tipScale: Float
        if (s <= tipStart) {
            tipAngle = 0f
            tipScale = 1f
        } else {
            val uCap = ((s - tipStart) / (1f - tipStart)).coerceIn(0f, 1f)
            tipAngle = asin(uCap)
            tipScale = sqrt((1f - uCap * uCap).coerceAtLeast(0f)).coerceAtLeast(0.015f)
        }
        val rootFlare = 1f + (if (isHallux) 0.16f else 0.12f) * (1f - smoothStep(0.0f, 0.22f, s))
        val phalanxNeck = 1f - 0.085f * exp(-((s - 0.34f) / 0.15f).let { it * it })
        val ipKnuckle = 1f + (if (isHallux) 0.055f else 0.072f) * exp(-((s - 0.50f) / 0.09f).let { it * it })
        val distalPad = 1f + (if (isHallux) 0.085f else 0.115f) * exp(-((s - 0.76f) / 0.14f).let { it * it })
        return Pair(rootFlare * phalanxNeck * ipKnuckle * distalPad * tipScale, tipAngle)
    }

    /**
     * Builds all 10 individually sculpted anatomical 3D toes (5 per foot: Hallux through 5th toe)
     * in a single lightweight [DynamicGlMesh], seamlessly rooted along the oblique metatarsal line
     * with realistic interphalangeal knuckles, plump rounded toe tips, and recessed dorsal nailbeds.
     */
    fun buildBlossomToesMesh(): DynamicGlMesh {
        val toeSpecs = BLOSSOM_TOE_SPECS

        val cols = 16
        val rows = 14
        val vertsPerToe = cols * rows
        val trisPerToe = (cols - 1) * (rows - 1) * 6
        val totalToes = toeSpecs.size * 2

        val mesh = DynamicGlMesh(vertsPerToe * totalToes, trisPerToe * totalToes)
        val indices = ShortArray(trisPerToe * totalToes)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val floorZ = BlossomBodyBuilder.MATTRESS_TOP_Z + 0.012f

        var toeIndex = 0
        var idxOut = 0
        val sides = floatArrayOf(-1f, +1f)

        for (side in sides) {
            for ((specIdx, spec) in toeSpecs.withIndex()) {
                val baseVert = toeIndex * vertsPerToe
                val isHallux = specIdx == 0

                // Emit strictly outward CCW grid indices for both left (+1) and right (-1) feet
                for (r in 0 until rows - 1) {
                    for (c in 0 until cols - 1) {
                        val i0 = (baseVert + r * cols + c).toShort()
                        val i1 = (baseVert + r * cols + c + 1).toShort()
                        val i2 = (baseVert + (r + 1) * cols + c).toShort()
                        val i3 = (baseVert + (r + 1) * cols + c + 1).toShort()
                        if (side > 0f) {
                            indices[idxOut++] = i0; indices[idxOut++] = i1; indices[idxOut++] = i2
                            indices[idxOut++] = i1; indices[idxOut++] = i3; indices[idxOut++] = i2
                        } else {
                            indices[idxOut++] = i0; indices[idxOut++] = i2; indices[idxOut++] = i1
                            indices[idxOut++] = i1; indices[idxOut++] = i2; indices[idxOut++] = i3
                        }
                    }
                }

                // Forward & lateral unit vectors in world space for this toe
                val fwdX = side * sin(spec.yawOut)
                val fwdY = -cos(spec.yawOut)
                val latX = side * cos(spec.yawOut)
                val latY = sin(spec.yawOut)

                for (r in 0 until rows) {
                    val s = toeRowParameterS(r, rows, spec, isHallux)

                    // True plump hemispherical rounded toe tip dome + sculpted IP knuckle & toe pad
                    val (radScale, tipAngle) = toeRadiusProfile(s, spec, isHallux)

                    val along = -0.095f + s * (spec.length + 0.095f)
                    // Natural gentle plantar toe arch: lesser toes curve slightly downward at the IP joint onto plump soft toe pads
                    val curlZ = spec.archLift * sin(s * PI.toFloat()) -
                        (if (isHallux) 0.018f else 0.030f) * smoothStep(0.42f, 0.94f, s)
                    // Subtle natural medial nestling of lesser toes (4th & 5th tuck gently toward hallux)
                    val medialCurve = -0.022f * smoothStep(0.28f, 1.0f, s) * when (specIdx) {
                        0 -> 0.10f
                        1 -> 0.32f
                        2 -> 0.58f
                        3 -> 0.85f
                        else -> 1.15f
                    }

                    val cx = side * spec.rootAx + fwdX * along + latX * medialCurve
                    val cy = spec.rootY + fwdY * along + latY * medialCurve
                    val cz = spec.rootZ + curlZ

                    for (c in 0 until cols) {
                        val u = c.toFloat() / (cols - 1)
                        val phi = u * (2f * PI.toFloat())
                        val cosP = cos(phi)
                        val sinP = sin(phi)

                        // Subtle dorsal nail plate indentation & anatomical IP joint contour (s in 0.62..0.90)
                        val nailMask = smoothBell(s, 0.62f, 0.90f, 0.05f) * smoothStep(0.45f, 0.92f, sinP) *
                            (1f - smoothStep(0.55f, 0.88f, abs(cosP)))
                        val nailIndent = -0.0040f * nailMask
                        val ipBand = exp(-((s - 0.50f) / 0.072f).let { it * it })
                        val knuckleBump = (if (isHallux) 0.0034f else 0.0046f) * ipBand * smoothStep(0.20f, 0.90f, sinP)

                        // Anatomical toe cross-section: broader, gently flatter hallux & subtly side-flattened lesser toes (matches toenails!)
                        val toeSectionW = smoothStep(0.10f, 0.28f, s) * (1f - smoothStep(0.82f, 0.94f, s))
                        val cSignT = if (cosP >= 0f) 1f else -1f
                        val sqCosT = cSignT * Math.pow(abs(cosP).toDouble().coerceAtLeast(1e-5), 0.90).toFloat()
                        val shapedCosT = cosP * (1f - 0.34f * toeSectionW) + sqCosT * (0.34f * toeSectionW)
                        val halluxBroad = if (isHallux) 1f + 0.055f * smoothStep(0.55f, 0.85f, s) else 1f
                        val dorsalToeFlat = if (isHallux) {
                            1f - 0.060f * smoothStep(0.45f, 0.84f, s)
                        } else {
                            1f - 0.035f * toeSectionW
                        }

                        val localLat = spec.rLat * radScale * shapedCosT * halluxBroad
                        var localZ = spec.rZ * radScale * sinP * (if (sinP >= 0f) dorsalToeFlat else 1f) + nailIndent + knuckleBump
                        // Soft plantar toe pad resting gently against the exam cushion
                        if (sinP < 0f) {
                            localZ *= (0.82f + 0.11f * smoothStep(0.60f, 0.90f, s))
                        }

                        val px = cx + latX * localLat
                        val py = cy + latY * localLat
                        val pz = (cz + localZ).coerceAtLeast(floorZ)

                        // Exact analytic outward hemispherical dome normal (smoothly rotates to pure forward at tip apex — zero pinch!)
                        val radialW = cos(tipAngle)
                        val fwdW = sin(tipAngle)
                        var nx = latX * (cosP * radialW / spec.rLat) + fwdX * (fwdW / spec.rLat)
                        var ny = latY * (cosP * radialW / spec.rLat) + fwdY * (fwdW / spec.rLat)
                        var nz = sinP * radialW / spec.rZ
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen

                        val rootBlend = 1f - smoothStep(0.0f, 0.28f, s)
                        if (rootBlend > 0.001f) {
                            val sdfNorm = evaluateBodyNormal(px, py, pz, eps = 0.016f)
                            val mx = nx * (1f - rootBlend) + sdfNorm[0] * rootBlend
                            val my = ny * (1f - rootBlend) + sdfNorm[1] * rootBlend
                            val mz = nz * (1f - rootBlend) + sdfNorm[2] * rootBlend
                            val mLen = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                            nx = mx / mLen; ny = my / mLen; nz = mz / mLen
                        }

                        // Soft interdigital crevice shadow + subtle IP knuckle crease AO + mattress contact shadow
                        val interdigitalAo = 1f - abs(cosP) * 0.25f * (1f - smoothStep(0.10f, 0.62f, s))
                        val ipCreaseAo = 1f - 0.075f * ipBand * smoothStep(0.25f, 0.90f, sinP)
                        val soleAo = 1f - smoothStep(0.10f, 0.80f, -sinP) * 0.34f
                        val ao = (interdigitalAo * ipCreaseAo * soleAo).coerceIn(0.36f, 1f)
                        val wetness = (0.052f + nailMask * 0.22f).coerceIn(0f, 0.45f)
                        val pigment = (0.08f + smoothStep(0.60f, 0.96f, s) * 0.085f +
                            ipBand * 0.045f * smoothStep(0.25f, 0.90f, sinP) + nailMask * 0.14f).coerceIn(0f, 1f)

                        val vIdx = (baseVert + r * cols + c) * stride
                        data[vIdx] = px
                        data[vIdx + 1] = py
                        data[vIdx + 2] = pz
                        data[vIdx + 3] = nx
                        data[vIdx + 4] = ny
                        data[vIdx + 5] = nz
                        data[vIdx + 6] = nailMask * 0.16f // gentle natural rosy nail tint
                        data[vIdx + 7] = wetness
                        data[vIdx + 8] = ao
                        data[vIdx + 9] = pigment
                    }
                }
                toeIndex++
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return mesh
    }

    /**
     * Precomputes the ultra-high-resolution (128x144) clustered elliptical anterior pelvic patch
     * projected with <0.0001mm precision onto Blossom's exact unified SDF body surface.
     */
    fun buildVulvaPatchCache(): BlossomVulvaPatchCache {
        val cols = VULVA_PATCH_COLS
        val rows = VULVA_PATCH_ROWS
        val cache = BlossomVulvaPatchCache(cols, rows)

        for (r in 0 until rows) {
            val vRaw = (r.toFloat() / (rows - 1)) * 2f - 1f
            // Non-linear vertical concentration over labia, clitoral hood, vestibule & anus
            val v = 0.56f * vRaw + 0.44f * vRaw * vRaw * vRaw
            for (c in 0 until cols) {
                val uRaw = (c.toFloat() / (cols - 1)) * 2f - 1f
                // Strong horizontal concentration across labia majora, sulcus & inner vestibule (~0.5mm spacing!)
                val u = 0.42f * uRaw + 0.58f * uRaw * uRaw * uRaw

                val ex = u * sqrt((1f - 0.5f * v * v).coerceAtLeast(0.0001f))
                val ey = v * sqrt((1f - 0.5f * u * u).coerceAtLeast(0.0001f))
                val edgeR = sqrt(ex * ex + ey * ey).coerceIn(0f, 1f)

                val x = ex * PATCH_RX
                val y = PATCH_CENTER_Y + ey * PATCH_RY
                val sample = sampleBodySurfacePoint(x, y)

                val idx = r * cols + c
                cache.baseX[idx] = x
                cache.baseY[idx] = y
                cache.baseZ[idx] = sample.z
                cache.baseNx[idx] = sample.nx
                cache.baseNy[idx] = sample.ny
                cache.baseNz[idx] = sample.nz
                cache.baseAo[idx] = sample.ao
                cache.basePigment[idx] = sample.pigment
                cache.edgeR[idx] = edgeR
            }
        }
        return cache
    }

    /**
     * Real-time 60 FPS update of Blossom's Ultra-HD deformable vulva patch with zero-seam
     * normal, height, AO, and pigment matching onto the unified SDF body.
     */
    fun updateBlossomVulvaPatch(
        mesh: DynamicGlMesh,
        cache: BlossomVulvaPatchCache,
        detailEvaluator: (x: Float, y: Float) -> VulvaSurfaceSample,
        computeNormals: (FloatArray, Int, Int, Boolean, Boolean) -> Unit,
        leftGluteX: Float = 0f,
        leftGluteY: Float = 0f,
        rightGluteX: Float = 0f,
        rightGluteY: Float = 0f,
        betaShortPerineum: Boolean = false,
        betaSmoothVulvaSeam: Boolean = false,
        betaBlendVulvaSeamColor: Boolean = false
    ) {
        val data = mesh.vertexData
        val cols = cache.cols
        val rows = cache.rows
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val lx = leftGluteX.coerceIn(-1f, 1f)
        val ly = leftGluteY.coerceIn(-1f, 1f)
        val rx = rightGluteX.coerceIn(-1f, 1f)
        val ry = rightGluteY.coerceIn(-1f, 1f)
        val hasGluteShift = abs(lx) > 1e-4f || abs(ly) > 1e-4f || abs(rx) > 1e-4f || abs(ry) > 1e-4f
        val anusCenterY = if (betaShortPerineum) -0.782f else -0.86f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val i = r * cols + c
                val x = cache.baseX[i]
                val y = cache.baseY[i]
                val edgeR = cache.edgeR[i]

                // Protect crisp perianal detail at (0, anusCenterY) while feathering outer perimeter cleanly
                val anusProtect = exp(-((x / 0.17f).let { it * it } + ((y - anusCenterY) / 0.16f).let { it * it }))
                val posFeather = if (betaSmoothVulvaSeam) {
                    1f - quinticStep(SEAM_A_POS_FEATHER_START, SEAM_A_POS_FEATHER_END, edgeR)
                } else {
                    1f - smoothStep(0.68f, 0.92f, edgeR)
                }
                val anusPosProtect = if (betaSmoothVulvaSeam) {
                    anusProtect * (1f - quinticStep(SEAM_A_ANUS_PROTECT_FADE_START, SEAM_A_ANUS_PROTECT_FADE_END, edgeR))
                } else {
                    anusProtect
                }
                val vMask = (posFeather + (1f - posFeather) * anusPosProtect * 0.75f).coerceIn(0f, 1f)

                // Seam B (Color) is completely decoupled from Seam A (Geometry):
                // Widens pigment, mucosa, AO & wetness fade across outer ~40% of patch radius [0.56..0.96]
                val matFeather = if (betaBlendVulvaSeamColor) {
                    1f - quinticStep(SEAM_B_MAT_FEATHER_START, SEAM_B_MAT_FEATHER_END, edgeR)
                } else {
                    1f - smoothStep(0.62f, 0.90f, edgeR)
                }
                val anusMatProtect = if (betaBlendVulvaSeamColor) {
                    anusProtect * (1f - quinticStep(SEAM_B_ANUS_MAT_FADE_START, SEAM_B_ANUS_MAT_FADE_END, edgeR))
                } else {
                    anusProtect
                }
                val matMask = (matFeather + (1f - matFeather) * anusMatProtect * 0.82f).coerceIn(0f, 1f)
                val d = if (vMask > 1e-6f || matMask > 1e-6f) detailEvaluator(x, y) else null

                val dz = (d?.dz ?: 0f) * vMask
                var px = x
                var py = y
                var baseZ = cache.baseZ[i]
                var baseAo = cache.baseAo[i]
                var basePigment = cache.basePigment[i]

                val pz: Float
                if (betaSmoothVulvaSeam) {
                    // Seam A (Geometry): Blend full 3D (px, py, pz) smoothly onto exact rendered body triangle surface
                    // across [0.80..0.96] with ZERO step/dive at the outermost ring -> flush rim even from grazing right-rim views!
                    if (edgeR >= SEAM_A_MESH_POS_BLEND_START && cache.hasSeamMatch[i]) {
                        val seamBlend = quinticStep(SEAM_A_MESH_POS_BLEND_START, SEAM_A_MESH_POS_BLEND_END, edgeR)
                        px = x * (1f - seamBlend) + cache.seamMeshX[i] * seamBlend
                        py = y * (1f - seamBlend) + cache.seamMeshY[i] * seamBlend
                        baseZ = baseZ * (1f - seamBlend) + cache.seamMeshZ[i] * seamBlend
                    }
                    pz = baseZ + dz
                } else {
                    // Baseline exact behavior
                    val overlapBias = 0.0012f * smoothStep(0.82f, 0.90f, edgeR) * (1f - smoothStep(0.93f, 0.96f, edgeR)) -
                        0.0024f * smoothStep(0.95f, 1.00f, edgeR)
                    pz = baseZ + dz + overlapBias
                }

                // Seam B (Color): Match surrounding body triangle's exact interpolated AO & pigment at the outer edge
                if (betaBlendVulvaSeamColor && edgeR >= SEAM_B_BODY_MAT_BLEND_START && cache.hasSeamMatch[i]) {
                    val matSeamBlend = quinticStep(SEAM_B_BODY_MAT_BLEND_START, SEAM_B_BODY_MAT_BLEND_END, edgeR)
                    baseAo = baseAo * (1f - matSeamBlend) + cache.seamMeshAo[i] * matSeamBlend
                    basePigment = basePigment * (1f - matSeamBlend) + cache.seamMeshPigment[i] * matSeamBlend
                }

                // Seamlessly apply matching gluteal soft-tissue yield on the perianal buttock cheeks left & right of the anus
                var finalPz = pz
                if (hasGluteShift && y < -0.34f) {
                    val gw = gluteElasticWeight(x, y, baseZ)
                    if (gw > 0.002f) {
                        val side = if (x >= 0f) 1f else -1f
                        val gx = if (side < 0f) lx else rx
                        val gy = if (side < 0f) ly else ry
                        val compressMag = (abs(gx) + abs(gy)).coerceIn(0f, 1.4f)
                        px += gx * 0.068f * gw
                        py += gy * 0.064f * gw
                        val gdz = (-0.015f * compressMag + 0.020f * (-gx * side)) * gw
                        finalPz = max(finalPz + gdz, mattressSurfaceZ(py) + 0.008f)
                    }
                }

                val mucosa = ((d?.mucosa ?: 0f) * matMask).coerceIn(0f, 1f)
                val wetness = (BODY_BASE_WETNESS + (d?.wetness ?: 0f) * matMask).coerceIn(0f, 1f)
                val detailAo = 1f - (1f - (d?.ao ?: 1f)) * matMask
                val ao = (baseAo * detailAo).coerceIn(0.06f, 1f)
                val pigment = (basePigment + (d?.pigment ?: 0f) * matMask).coerceIn(0f, 1f)

                val vIdx = i * stride
                data[vIdx] = px
                data[vIdx + 1] = py
                data[vIdx + 2] = finalPz
                data[vIdx + 6] = mucosa
                data[vIdx + 7] = wetness
                data[vIdx + 8] = ao
                data[vIdx + 9] = pigment
            }
        }

        computeNormals(data, cols, rows, false, false)

        // Wide, smooth outer-skirt normal matching to the unified SDF body's normals -> ZERO seam!
        for (i in 0 until cache.count) {
            val edgeR = cache.edgeR[i]
            val bx = cache.baseX[i]
            val by = cache.baseY[i]
            val anusProtect = exp(-((bx / 0.16f).let { it * it } + ((by - anusCenterY) / 0.15f).let { it * it }))
            val normalBlend = if (betaSmoothVulvaSeam) {
                val anusNormProtect = anusProtect * (1f - quinticStep(SEAM_A_ANUS_PROTECT_FADE_START, SEAM_A_ANUS_PROTECT_FADE_END, edgeR))
                (quinticStep(SEAM_A_NORMAL_BLEND_START, SEAM_A_NORMAL_BLEND_END, edgeR) * (1f - anusNormProtect * 0.85f)).coerceIn(0f, 1f)
            } else {
                (smoothStep(0.66f, 0.93f, edgeR) * (1f - anusProtect * 0.82f)).coerceIn(0f, 1f)
            }
            if (normalBlend > 0.001f) {
                val vIdx = i * stride
                var bnx = cache.baseNx[i]
                var bny = cache.baseNy[i]
                var bnz = cache.baseNz[i]
                if (betaSmoothVulvaSeam && edgeR >= SEAM_A_MESH_NORMAL_BLEND_START && cache.hasSeamMatch[i]) {
                    val seamNBlend = quinticStep(SEAM_A_MESH_NORMAL_BLEND_START, SEAM_A_MESH_NORMAL_BLEND_END, edgeR)
                    bnx = bnx * (1f - seamNBlend) + cache.seamMeshNx[i] * seamNBlend
                    bny = bny * (1f - seamNBlend) + cache.seamMeshNy[i] * seamNBlend
                    bnz = bnz * (1f - seamNBlend) + cache.seamMeshNz[i] * seamNBlend
                    val sLen = sqrt(bnx * bnx + bny * bny + bnz * bnz).coerceAtLeast(1e-5f)
                    bnx /= sLen; bny /= sLen; bnz /= sLen
                }
                if (hasGluteShift && by < -0.34f) {
                    val gw = gluteElasticWeight(bx, by, cache.baseZ[i])
                    if (gw > 0.002f) {
                        val side = if (bx >= 0f) 1f else -1f
                        val gx = if (side < 0f) lx else rx
                        val gy = if (side < 0f) ly else ry
                        bnx += gx * 0.08f * gw
                        bny += gy * 0.08f * gw
                        val bLen = sqrt(bnx * bnx + bny * bny + bnz * bnz).coerceAtLeast(1e-5f)
                        bnx /= bLen; bny /= bLen; bnz /= bLen
                    }
                }
                val mx = data[vIdx + 3] * (1f - normalBlend) + bnx * normalBlend
                val my = data[vIdx + 4] * (1f - normalBlend) + bny * normalBlend
                val mz = data[vIdx + 5] * (1f - normalBlend) + bnz * normalBlend
                val len = sqrt(mx * mx + my * my + mz * mz)
                if (len > 1e-5f) {
                    data[vIdx + 3] = mx / len
                    data[vIdx + 4] = my / len
                    data[vIdx + 5] = mz / len
                } else {
                    data[vIdx + 3] = bnx
                    data[vIdx + 4] = bny
                    data[vIdx + 5] = bnz
                }
            }
        }

        mesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // LOCALIZED REVERSIBLE PELVIS/BUTTOCK/SEAM BETA CACHE (INSTANT 60 FPS TOGGLE)
    // -------------------------------------------------------------------------
    class PelvisBetaCache(
        val bodyVertIndices: IntArray,
        val baselineVertData: FloatArray,   // 8 floats/vert: x,y,z,nx,ny,nz,ao,pigment (exact backup!)
        val unrecessedPos: FloatArray,      // 3 floats/vert: exact SDF=0 position prior to collar recess
        val ovalR: FloatArray,
        val isAnteriorCollar: BooleanArray,
        val inButtockZone: BooleanArray,
        val smoothNeighborOffsets: IntArray, // CSR offsets (length = bodyVertIndices.size + 1) for un-recessed rim smoothing
        val smoothNeighborVerts: IntArray    // Body vertex indices of 1-ring neighbors for Seam A rim smoothing
    )

    fun buildPelvisBetaCache(bodyMesh: DynamicGlMesh): PelvisBetaCache {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val vCount = bodyMesh.vertexCount

        val idxBuf = IntArray(vCount)
        val ovalBuf = FloatArray(vCount)
        val antBuf = BooleanArray(vCount)
        val buttockBuf = BooleanArray(vCount)
        var count = 0

        for (i in 0 until vCount) {
            val base = i * stride
            val x = bData[base]
            val y = bData[base + 1]
            val z = bData[base + 2]
            val nz = bData[base + 5]
            val ax = abs(x)

            val rOval = sqrt((x / PATCH_RX).let { it * it } + ((y - PATCH_CENTER_Y) / PATCH_RY).let { it * it })
            val isAnt = z > -0.92f && nz > 0.04f && rOval < 1.30f
            val inButtock = y in -1.44f..0.26f && ax < 1.74f && z < 0.30f

            if (isAnt || inButtock) {
                idxBuf[count] = i
                ovalBuf[count] = rOval
                antBuf[count] = isAnt
                buttockBuf[count] = inButtock
                count++
            }
        }

        val indices = idxBuf.copyOf(count)
        val ovalR = ovalBuf.copyOf(count)
        val isAntCollar = antBuf.copyOf(count)
        val inButtock = buttockBuf.copyOf(count)
        val baseline = FloatArray(count * 8)
        val unrecessed = FloatArray(count * 3)

        val vertToCache = IntArray(vCount) { -1 }
        for (k in 0 until count) {
            vertToCache[indices[k]] = k
            val src = indices[k] * stride
            val x = bData[src]; val y = bData[src + 1]; val z = bData[src + 2]
            val nx = bData[src + 3]; val ny = bData[src + 4]; val nz = bData[src + 5]
            val ao = bData[src + 8]; val pig = bData[src + 9]

            val bOff = k * 8
            baseline[bOff] = x; baseline[bOff + 1] = y; baseline[bOff + 2] = z
            baseline[bOff + 3] = nx; baseline[bOff + 4] = ny; baseline[bOff + 5] = nz
            baseline[bOff + 6] = ao; baseline[bOff + 7] = pig

            var ux = x; var uy = y; var uz = z
            if (isAntCollar[k] && ovalR[k] < 0.94f && z > -0.85f && nz > 0.08f) {
                val oldRecess = 0.090f * (1f - smoothStep(0.76f, 0.94f, ovalR[k]))
                ux += nx * oldRecess
                uy += ny * oldRecess
                uz += nz * oldRecess
                // 2 Newton polish steps back onto baseline SDF=0
                for (it in 0 until 2) {
                    val d = evaluateBodySdf(ux, uy, uz)
                    if (abs(d) < 0.0003f) break
                    val n = evaluateBodyNormal(ux, uy, uz, eps = 0.014f)
                    val step = d.coerceIn(-0.04f, 0.04f)
                    ux -= n[0] * step; uy -= n[1] * step; uz -= n[2] * step
                }
            }
            val uOff = k * 3
            unrecessed[uOff] = ux; unrecessed[uOff + 1] = uy; unrecessed[uOff + 2] = uz
        }

        // Precompute 1-ring neighbors (excluding deeply recessed inner vertices) for anterior rim smoothing
        val maxDeg = 12
        val nbMat = IntArray(count * maxDeg)
        val nbDeg = IntArray(count)
        fun addNeighbor(cacheK: Int, nbBodyVert: Int) {
            if (!isAntCollar[cacheK]) return
            if (ovalR[cacheK] < SEAM_A_BODY_SMOOTH_INNER || ovalR[cacheK] >= SEAM_A_BODY_SMOOTH_OUTER) return
            // Skip recessed inner neighbors so collar recess never drags rim vertices inward
            val nbK = vertToCache[nbBodyVert]
            if (nbK >= 0 && isAntCollar[nbK] && ovalR[nbK] < SEAM_A_BODY_SMOOTH_INNER) return
            val deg = nbDeg[cacheK]
            val rowOff = cacheK * maxDeg
            for (d in 0 until deg) {
                if (nbMat[rowOff + d] == nbBodyVert) return
            }
            if (deg < maxDeg) {
                nbMat[rowOff + deg] = nbBodyVert
                nbDeg[cacheK] = deg + 1
            }
        }
        val idxBufMesh = bodyMesh.indexBuffer
        val triCount = bodyMesh.indexCount / 3
        idxBufMesh.position(0)
        for (t in 0 until triCount) {
            val a = idxBufMesh.get().toInt() and 0xFFFF
            val b = idxBufMesh.get().toInt() and 0xFFFF
            val c = idxBufMesh.get().toInt() and 0xFFFF
            val ka = vertToCache[a]
            val kb = vertToCache[b]
            val kc = vertToCache[c]
            if (ka >= 0) { addNeighbor(ka, b); addNeighbor(ka, c) }
            if (kb >= 0) { addNeighbor(kb, a); addNeighbor(kb, c) }
            if (kc >= 0) { addNeighbor(kc, a); addNeighbor(kc, b) }
        }
        idxBufMesh.position(0)

        val nbOffsets = IntArray(count + 1)
        var totalNb = 0
        for (k in 0 until count) {
            nbOffsets[k] = totalNb
            totalNb += nbDeg[k]
        }
        nbOffsets[count] = totalNb
        val nbVerts = IntArray(totalNb)
        for (k in 0 until count) {
            val deg = nbDeg[k]
            if (deg > 0) {
                System.arraycopy(nbMat, k * maxDeg, nbVerts, nbOffsets[k], deg)
            }
        }

        return PelvisBetaCache(
            bodyVertIndices = indices,
            baselineVertData = baseline,
            unrecessedPos = unrecessed,
            ovalR = ovalR,
            isAnteriorCollar = isAntCollar,
            inButtockZone = inButtock,
            smoothNeighborOffsets = nbOffsets,
            smoothNeighborVerts = nbVerts
        )
    }

    /**
     * Applies any independent combination of the pelvis/buttock/seam Beta refinements cleanly and reversibly
     * to ONLY the pelvic/buttock/patch-seam region of Blossom's body mesh and vulva patch cache,
     * keeping legs, vulva design, and everything above the pelvis 100% untouched.
     * Restores the exact immutable baseline backup whenever all toggles are OFF.
     */
    fun applyPelvisBetaRefinements(
        bodyMesh: DynamicGlMesh,
        pelvisCache: PelvisBetaCache,
        legCache: LegSkinCache,
        gluteCache: GluteSkinCache,
        patchCache: BlossomVulvaPatchCache,
        baselinePatchCache: BlossomVulvaPatchCache,
        betaShortPerineum: Boolean,
        betaSoftButtockTable: Boolean,
        betaSmoothVulvaSeam: Boolean,
        betaBlendVulvaSeamColor: Boolean = false
    ) {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val indices = pelvisCache.bodyVertIndices
        val baseline = pelvisCache.baselineVertData
        val unrecessed = pelvisCache.unrecessedPos
        val ovalRs = pelvisCache.ovalR
        val isAnts = pelvisCache.isAnteriorCollar
        val inButtocks = pelvisCache.inButtockZone

        val bodyChanged = betaShortPerineum || betaSoftButtockTable || betaSmoothVulvaSeam
        val sdfChanged = betaShortPerineum || betaSoftButtockTable
        val anusCenterY = if (betaShortPerineum) -0.782f else -0.86f

        // 1. Update ONLY pelvic/buttock/collar vertices of blossomBodyMesh
        for (k in indices.indices) {
            val dst = indices[k] * stride
            val bOff = k * 8
            val touchesButtock = sdfChanged && inButtocks[k]
            val touchesSeam = betaSmoothVulvaSeam && isAnts[k]
            if (!bodyChanged || (!touchesButtock && !touchesSeam)) {
                bData[dst] = baseline[bOff]
                bData[dst + 1] = baseline[bOff + 1]
                bData[dst + 2] = baseline[bOff + 2]
                bData[dst + 3] = baseline[bOff + 3]
                bData[dst + 4] = baseline[bOff + 4]
                bData[dst + 5] = baseline[bOff + 5]
                bData[dst + 8] = baseline[bOff + 6]
                bData[dst + 9] = baseline[bOff + 7]
                continue
            }

            val uOff = k * 3
            var px = unrecessed[uOff]
            var py = unrecessed[uOff + 1]
            var pz = unrecessed[uOff + 2]
            var nx = baseline[bOff + 3]
            var ny = baseline[bOff + 4]
            var nz = baseline[bOff + 5]
            var ao = baseline[bOff + 6]
            var pig = baseline[bOff + 7]

            if (sdfChanged && inButtocks[k]) {
                // Smooth damped projection onto toggled pelvic/buttock SDF
                for (it in 0 until 4) {
                    val dVal = evaluateBodySdf(px, py, pz, betaShortPerineum, betaSoftButtockTable)
                    if (abs(dVal) < 0.0005f) break
                    val norm = evaluateBodyNormal(px, py, pz, eps = 0.015f, betaShortPerineum, betaSoftButtockTable)
                    val step = dVal.coerceIn(-0.07f, 0.07f)
                    px -= norm[0] * step
                    py -= norm[1] * step
                    pz -= norm[2] * step
                }
                val finalNorm = evaluateBodyNormal(px, py, pz, eps = 0.014f, betaShortPerineum, betaSoftButtockTable)
                nx = finalNorm[0]; ny = finalNorm[1]; nz = finalNorm[2]
                val mat = evaluateBodyMaterial(px, py, pz, nx, ny, nz, betaShortPerineum, betaSoftButtockTable)
                ao = mat.first; pig = mat.second
            }

            if (isAnts[k]) {
                val rOval = ovalRs[k]
                if (betaSmoothVulvaSeam) {
                    // Seam A: Pull inner recess strictly inside [0.70..0.84] so EVERY body triangle
                    // touching the outer rim (edgeR >= 0.86) has all 3 vertices 100% un-recessed on SDF=0!
                    val baseRecess = if (rOval < SEAM_A_COLLAR_RECESS_END) {
                        SEAM_A_COLLAR_RECESS_DEPTH * (1f - quinticStep(SEAM_A_COLLAR_RECESS_START, SEAM_A_COLLAR_RECESS_END, rOval))
                    } else 0f
                    val perianalClear = if (rOval < 0.90f && abs(px) < 0.22f && py < -0.64f) {
                        SEAM_A_PERIANAL_RECESS_DEPTH * exp(-((px / 0.13f).let { it * it } + ((py - anusCenterY) / 0.12f).let { it * it }))
                    } else 0f
                    val recess = max(baseRecess, perianalClear)
                    if (recess > 1e-5f) {
                        px -= nx * recess; py -= ny * recess; pz -= nz * recess
                    } else if (!sdfChanged && rOval < SEAM_A_BODY_SMOOTH_OUTER) {
                        val dVal = evaluateBodySdf(px, py, pz)
                        if (abs(dVal) > 0.0002f) {
                            val step = dVal.coerceIn(-0.02f, 0.02f)
                            px -= nx * step; py -= ny * step; pz -= nz * step
                        }
                        val n = evaluateBodyNormal(px, py, pz, eps = 0.014f)
                        nx = n[0]; ny = n[1]; nz = n[2]
                        val mat = evaluateBodyMaterial(px, py, pz, nx, ny, nz)
                        ao = mat.first; pig = mat.second
                    }
                } else if (rOval < 0.94f && baseline[bOff + 2] > -0.85f && baseline[bOff + 5] > 0.08f) {
                    val recess = 0.090f * (1f - smoothStep(0.76f, 0.94f, rOval))
                    px -= nx * recess; py -= ny * recess; pz -= nz * recess
                }
            }

            bData[dst] = px
            bData[dst + 1] = py
            bData[dst + 2] = pz
            bData[dst + 3] = nx
            bData[dst + 4] = ny
            bData[dst + 5] = nz
            bData[dst + 8] = ao
            bData[dst + 9] = pig
        }

        // 1B. Seam A (Geometry): 2 local Laplacian smoothing + exact SDF=0 projection passes across the
        // un-recessed rim & immediately surrounding skin band [0.84..1.26] to eliminate Surface Nets ripple shading!
        if (betaSmoothVulvaSeam) {
            val nbOffsets = pelvisCache.smoothNeighborOffsets
            val nbVerts = pelvisCache.smoothNeighborVerts
            val tmpX = FloatArray(indices.size)
            val tmpY = FloatArray(indices.size)
            val tmpZ = FloatArray(indices.size)
            val touched = BooleanArray(indices.size)

            repeat(SEAM_A_BODY_SMOOTH_PASSES) {
                for (k in indices.indices) {
                    val start = nbOffsets[k]
                    val end = nbOffsets[k + 1]
                    if (end <= start) {
                        touched[k] = false
                        continue
                    }
                    val rOval = ovalRs[k]
                    val inRamp = quinticStep(SEAM_A_BODY_SMOOTH_INNER, 0.90f, rOval)
                    val outRamp = 1f - quinticStep(SEAM_A_BODY_SMOOTH_FADE_START, SEAM_A_BODY_SMOOTH_OUTER, rOval)
                    val w = SEAM_A_BODY_SMOOTH_LAMBDA * inRamp * outRamp
                    if (w < 1e-4f) {
                        touched[k] = false
                        continue
                    }
                    var sx = 0f; var sy = 0f; var sz = 0f
                    for (e in start until end) {
                        val nbOff = nbVerts[e] * stride
                        sx += bData[nbOff]
                        sy += bData[nbOff + 1]
                        sz += bData[nbOff + 2]
                    }
                    val invDeg = 1f / (end - start)
                    val dst = indices[k] * stride
                    var px = bData[dst] * (1f - w) + (sx * invDeg) * w
                    var py = bData[dst + 1] * (1f - w) + (sy * invDeg) * w
                    var pz = bData[dst + 2] * (1f - w) + (sz * invDeg) * w

                    // Project smoothly back onto exact SDF=0 isosurface
                    for (it in 0 until 2) {
                        val dVal = evaluateBodySdf(px, py, pz, betaShortPerineum, betaSoftButtockTable)
                        if (abs(dVal) < 0.0002f) break
                        val nrm = evaluateBodyNormal(px, py, pz, eps = 0.014f, betaShortPerineum, betaSoftButtockTable)
                        val step = dVal.coerceIn(-0.04f, 0.04f)
                        px -= nrm[0] * step; py -= nrm[1] * step; pz -= nrm[2] * step
                    }
                    tmpX[k] = px; tmpY[k] = py; tmpZ[k] = pz
                    touched[k] = true
                }
                for (k in indices.indices) {
                    if (!touched[k]) continue
                    val dst = indices[k] * stride
                    val px = tmpX[k]; val py = tmpY[k]; val pz = tmpZ[k]
                    val nrm = evaluateBodyNormal(px, py, pz, eps = 0.014f, betaShortPerineum, betaSoftButtockTable)
                    val mat = evaluateBodyMaterial(px, py, pz, nrm[0], nrm[1], nrm[2], betaShortPerineum, betaSoftButtockTable)
                    bData[dst] = px
                    bData[dst + 1] = py
                    bData[dst + 2] = pz
                    bData[dst + 3] = nrm[0]
                    bData[dst + 4] = nrm[1]
                    bData[dst + 5] = nrm[2]
                    bData[dst + 8] = mat.first
                    bData[dst + 9] = mat.second
                }
            }
        }

        // Sync affected rest positions into leg & glute skin caches so live leg-spread & glute elasticity compose seamlessly
        val gluteIdx = gluteCache.bodyVertIndices
        val gluteRest = gluteCache.bodyRestPosNorm
        for (k in gluteIdx.indices) {
            val src = gluteIdx[k] * stride
            val dst = k * 6
            gluteRest[dst] = bData[src]; gluteRest[dst + 1] = bData[src + 1]; gluteRest[dst + 2] = bData[src + 2]
            gluteRest[dst + 3] = bData[src + 3]; gluteRest[dst + 4] = bData[src + 4]; gluteRest[dst + 5] = bData[src + 5]
        }
        val legIdx = legCache.bodyVertIndices
        val legBase = legCache.bodyBasePosNorm
        for (k in legIdx.indices) {
            val vIdx = legIdx[k]
            val src = vIdx * stride
            if (bData[src + 1] > -1.50f) {
                val dst = k * 6
                legBase[dst] = bData[src]; legBase[dst + 1] = bData[src + 1]; legBase[dst + 2] = bData[src + 2]
                legBase[dst + 3] = bData[src + 3]; legBase[dst + 4] = bData[src + 4]; legBase[dst + 5] = bData[src + 5]
            }
        }

        bodyMesh.commitVertices()

        // 2. Refresh vulva patch base cache (instant copy when OFF, localized lower-region resample when SDF changed)
        patchCache.copyFrom(baselinePatchCache)
        if (sdfChanged) {
            for (i in 0 until patchCache.count) {
                val py = patchCache.baseY[i]
                if (py < 0.24f) {
                    val sample = sampleBodySurfacePoint(
                        x = patchCache.baseX[i],
                        y = py,
                        betaShortPerineum = betaShortPerineum,
                        betaSoftButtockTable = betaSoftButtockTable
                    )
                    patchCache.baseZ[i] = sample.z
                    patchCache.baseNx[i] = sample.nx
                    patchCache.baseNy[i] = sample.ny
                    patchCache.baseNz[i] = sample.nz
                    patchCache.baseAo[i] = sample.ao
                    patchCache.basePigment[i] = sample.pigment
                }
            }
        }

        // 3. Populate exact 3D barycentric body-triangle seam targets whenever Seam A (Geometry) or Seam B (Color) is active
        if (betaSmoothVulvaSeam || betaBlendVulvaSeamColor) {
            populateVulvaPatchSeamTargets(bodyMesh, patchCache)
        }
    }

    /**
     * Exact 3D Möller–Trumbore normal-ray intersection + closest-triangle barycentric fallback against
     * the actual rendered anterior seam-collar triangles of [bodyMesh] for every outer patch vertex
     * (`edgeR >= SEAM_TARGET_MIN_EDGE_R`). Guarantees 100% perimeter coverage including steeply sloped
     * right/left inner-thigh rims with identical 3D positions, normals, AO, and pigment!
     */
    private fun populateVulvaPatchSeamTargets(
        bodyMesh: DynamicGlMesh,
        patchCache: BlossomVulvaPatchCache
    ) {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val idxBuf = bodyMesh.indexBuffer
        val triCount = bodyMesh.indexCount / 3

        val boxMinX = -PATCH_RX - 0.18f
        val boxMaxX = PATCH_RX + 0.18f
        val boxMinY = PATCH_CENTER_Y - PATCH_RY - 0.18f
        val boxMaxY = PATCH_CENTER_Y + PATCH_RY + 0.18f

        val triA = IntArray(triCount)
        val triB = IntArray(triCount)
        val triC = IntArray(triCount)
        val triMinX = FloatArray(triCount)
        val triMaxX = FloatArray(triCount)
        val triMinY = FloatArray(triCount)
        val triMaxY = FloatArray(triCount)
        val triMinZ = FloatArray(triCount)
        val triMaxZ = FloatArray(triCount)
        var selCount = 0

        idxBuf.position(0)
        for (t in 0 until triCount) {
            val a = idxBuf.get().toInt() and 0xFFFF
            val b = idxBuf.get().toInt() and 0xFFFF
            val c = idxBuf.get().toInt() and 0xFFFF

            val ao = a * stride; val bo = b * stride; val co = c * stride
            val az = bData[ao + 2]; val bz = bData[bo + 2]; val cz = bData[co + 2]
            if (az < -1.05f && bz < -1.05f && cz < -1.05f) continue
            val anz = bData[ao + 5]; val bnz = bData[bo + 5]; val cnz = bData[co + 5]
            if (anz + bnz + cnz < SEAM_TRI_MIN_NZ_SUM) continue

            val ax = bData[ao]; val ay = bData[ao + 1]
            val bx = bData[bo]; val by = bData[bo + 1]
            val cx = bData[co]; val cy = bData[co + 1]

            val minX = min(ax, min(bx, cx))
            val maxX = max(ax, max(bx, cx))
            val minY = min(ay, min(by, cy))
            val maxY = max(ay, max(by, cy))
            if (maxX < boxMinX || minX > boxMaxX || maxY < boxMinY || minY > boxMaxY) continue

            triA[selCount] = a; triB[selCount] = b; triC[selCount] = c
            triMinX[selCount] = minX - 0.06f
            triMaxX[selCount] = maxX + 0.06f
            triMinY[selCount] = minY - 0.06f
            triMaxY[selCount] = maxY + 0.06f
            triMinZ[selCount] = min(az, min(bz, cz)) - 0.08f
            triMaxZ[selCount] = max(az, max(bz, cz)) + 0.08f
            selCount++
        }
        idxBuf.position(0)

        // Lightweight 8x8 spatial hash over patch XY footprint for sub-millisecond toggle latency
        val gridN = 8
        val invSpanX = gridN / (boxMaxX - boxMinX)
        val invSpanY = gridN / (boxMaxY - boxMinY)
        val cellCounts = IntArray(gridN * gridN)
        for (t in 0 until selCount) {
            val ix0 = ((triMinX[t] - boxMinX) * invSpanX).toInt().coerceIn(0, gridN - 1)
            val ix1 = ((triMaxX[t] - boxMinX) * invSpanX).toInt().coerceIn(0, gridN - 1)
            val iy0 = ((triMinY[t] - boxMinY) * invSpanY).toInt().coerceIn(0, gridN - 1)
            val iy1 = ((triMaxY[t] - boxMinY) * invSpanY).toInt().coerceIn(0, gridN - 1)
            for (iy in iy0..iy1) {
                val rowOff = iy * gridN
                for (ix in ix0..ix1) cellCounts[rowOff + ix]++
            }
        }
        val cellOffsets = IntArray(gridN * gridN + 1)
        var totalRefs = 0
        for (c in 0 until gridN * gridN) {
            cellOffsets[c] = totalRefs
            totalRefs += cellCounts[c]
            cellCounts[c] = 0
        }
        cellOffsets[gridN * gridN] = totalRefs
        val cellTris = IntArray(totalRefs)
        for (t in 0 until selCount) {
            val ix0 = ((triMinX[t] - boxMinX) * invSpanX).toInt().coerceIn(0, gridN - 1)
            val ix1 = ((triMaxX[t] - boxMinX) * invSpanX).toInt().coerceIn(0, gridN - 1)
            val iy0 = ((triMinY[t] - boxMinY) * invSpanY).toInt().coerceIn(0, gridN - 1)
            val iy1 = ((triMaxY[t] - boxMinY) * invSpanY).toInt().coerceIn(0, gridN - 1)
            for (iy in iy0..iy1) {
                val rowOff = iy * gridN
                for (ix in ix0..ix1) {
                    val cell = rowOff + ix
                    cellTris[cellOffsets[cell] + cellCounts[cell]++] = t
                }
            }
        }

        val baryOut = FloatArray(3)
        for (i in 0 until patchCache.count) {
            val edgeR = patchCache.edgeR[i]
            if (edgeR < SEAM_TARGET_MIN_EDGE_R) {
                patchCache.hasSeamMatch[i] = false
                continue
            }
            val ox = patchCache.baseX[i]
            val oy = patchCache.baseY[i]
            val oz = patchCache.baseZ[i]
            val dx = patchCache.baseNx[i]
            val dy = patchCache.baseNy[i]
            val dz = patchCache.baseNz[i]

            val ix = ((ox - boxMinX) * invSpanX).toInt().coerceIn(0, gridN - 1)
            val iy = ((oy - boxMinY) * invSpanY).toInt().coerceIn(0, gridN - 1)
            val cell = iy * gridN + ix
            val cStart = cellOffsets[cell]
            val cEnd = cellOffsets[cell + 1]

            var bestRayAbsT = Float.POSITIVE_INFINITY
            var rayFound = false
            var bestX = ox; var bestY = oy; var bestZ = oz
            var bestNx = dx; var bestNy = dy; var bestNz = dz
            var bestAo = patchCache.baseAo[i]; var bestPig = patchCache.basePigment[i]

            // Pass 1: Exact Möller–Trumbore ray-triangle intersection along surface normal D
            for (ci in cStart until cEnd) {
                val t = cellTris[ci]
                if (ox < triMinX[t] || ox > triMaxX[t] || oy < triMinY[t] || oy > triMaxY[t] || oz < triMinZ[t] || oz > triMaxZ[t]) continue

                val ao = triA[t] * stride
                val bo = triB[t] * stride
                val co = triC[t] * stride
                val x0 = bData[ao]; val y0 = bData[ao + 1]; val z0 = bData[ao + 2]
                val e1x = bData[bo] - x0; val e1y = bData[bo + 1] - y0; val e1z = bData[bo + 2] - z0
                val e2x = bData[co] - x0; val e2y = bData[co + 1] - y0; val e2z = bData[co + 2] - z0

                val pvx = dy * e2z - dz * e2y
                val pvy = dz * e2x - dx * e2z
                val pvz = dx * e2y - dy * e2x
                val det = e1x * pvx + e1y * pvy + e1z * pvz
                if (abs(det) < 1e-7f) continue

                val invDet = 1f / det
                val tvx = ox - x0; val tvy = oy - y0; val tvz = oz - z0
                val u = (tvx * pvx + tvy * pvy + tvz * pvz) * invDet
                if (u < -0.02f || u > 1.02f) continue

                val qvx = tvy * e1z - tvz * e1y
                val qvy = tvz * e1x - tvx * e1z
                val qvz = tvx * e1y - tvy * e1x
                val v = (dx * qvx + dy * qvy + dz * qvz) * invDet
                val w = 1f - u - v
                if (v < -0.02f || w < -0.02f) continue

                val rayT = (e2x * qvx + e2y * qvy + e2z * qvz) * invDet
                val absT = abs(rayT)
                if (absT < SEAM_RAY_MAX_DIST && absT < bestRayAbsT) {
                    val cu = u.coerceIn(0f, 1f)
                    val cv = v.coerceIn(0f, 1f)
                    val cw = w.coerceIn(0f, 1f)
                    val wSum = (cu + cv + cw).coerceAtLeast(1e-6f)
                    val nw0 = cw / wSum; val nw1 = cu / wSum; val nw2 = cv / wSum

                    bestRayAbsT = absT
                    bestX = nw0 * x0 + nw1 * bData[bo] + nw2 * bData[co]
                    bestY = nw0 * y0 + nw1 * bData[bo + 1] + nw2 * bData[co + 1]
                    bestZ = nw0 * z0 + nw1 * bData[bo + 2] + nw2 * bData[co + 2]
                    var tnx = nw0 * bData[ao + 3] + nw1 * bData[bo + 3] + nw2 * bData[co + 3]
                    var tny = nw0 * bData[ao + 4] + nw1 * bData[bo + 4] + nw2 * bData[co + 4]
                    var tnz = nw0 * bData[ao + 5] + nw1 * bData[bo + 5] + nw2 * bData[co + 5]
                    val nLen = sqrt(tnx * tnx + tny * tny + tnz * tnz).coerceAtLeast(1e-5f)
                    tnx /= nLen; tny /= nLen; tnz /= nLen
                    bestNx = tnx; bestNy = tny; bestNz = tnz
                    bestAo = nw0 * bData[ao + 8] + nw1 * bData[bo + 8] + nw2 * bData[co + 8]
                    bestPig = nw0 * bData[ao + 9] + nw1 * bData[bo + 9] + nw2 * bData[co + 9]
                    rayFound = true
                }
            }

            // Pass 2: Guaranteed closest-triangle 3D barycentric projection fallback (0% dropouts on steep rims!)
            if (!rayFound) {
                var bestDistSq = Float.POSITIVE_INFINITY
                val useCell = cEnd > cStart
                val iterCount = if (useCell) cEnd - cStart else selCount
                for (k in 0 until iterCount) {
                    val t = if (useCell) cellTris[cStart + k] else k
                    val ao = triA[t] * stride
                    val bo = triB[t] * stride
                    val co = triC[t] * stride
                    val distSq = closestPointOnTriangleBarycentric(
                        ox, oy, oz,
                        bData[ao], bData[ao + 1], bData[ao + 2],
                        bData[bo], bData[bo + 1], bData[bo + 2],
                        bData[co], bData[co + 1], bData[co + 2],
                        baryOut
                    )
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq
                        val nw0 = baryOut[0]; val nw1 = baryOut[1]; val nw2 = baryOut[2]
                        bestX = nw0 * bData[ao] + nw1 * bData[bo] + nw2 * bData[co]
                        bestY = nw0 * bData[ao + 1] + nw1 * bData[bo + 1] + nw2 * bData[co + 1]
                        bestZ = nw0 * bData[ao + 2] + nw1 * bData[bo + 2] + nw2 * bData[co + 2]
                        var tnx = nw0 * bData[ao + 3] + nw1 * bData[bo + 3] + nw2 * bData[co + 3]
                        var tny = nw0 * bData[ao + 4] + nw1 * bData[bo + 4] + nw2 * bData[co + 4]
                        var tnz = nw0 * bData[ao + 5] + nw1 * bData[bo + 5] + nw2 * bData[co + 5]
                        val nLen = sqrt(tnx * tnx + tny * tny + tnz * tnz).coerceAtLeast(1e-5f)
                        tnx /= nLen; tny /= nLen; tnz /= nLen
                        bestNx = tnx; bestNy = tny; bestNz = tnz
                        bestAo = nw0 * bData[ao + 8] + nw1 * bData[bo + 8] + nw2 * bData[co + 8]
                        bestPig = nw0 * bData[ao + 9] + nw1 * bData[bo + 9] + nw2 * bData[co + 9]
                        rayFound = true
                    }
                }
            }

            if (rayFound) {
                patchCache.seamMeshX[i] = bestX
                patchCache.seamMeshY[i] = bestY
                patchCache.seamMeshZ[i] = bestZ
                patchCache.seamMeshNx[i] = bestNx
                patchCache.seamMeshNy[i] = bestNy
                patchCache.seamMeshNz[i] = bestNz
                patchCache.seamMeshAo[i] = bestAo
                patchCache.seamMeshPigment[i] = bestPig
                patchCache.hasSeamMatch[i] = true
            } else {
                patchCache.hasSeamMatch[i] = false
            }
        }
    }

    /**
     * Exact closest point on 3D triangle (A, B, C) to point P; writes normalized barycentric weights
     * (wA, wB, wC) into [baryOut] and returns squared Euclidean distance.
     */
    private fun closestPointOnTriangleBarycentric(
        px: Float, py: Float, pz: Float,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        baryOut: FloatArray
    ): Float {
        val abx = bx - ax; val aby = by - ay; val abz = bz - az
        val acx = cx - ax; val acy = cy - ay; val acz = cz - az
        val apx = px - ax; val apy = py - ay; val apz = pz - az

        val d1 = abx * apx + aby * apy + abz * apz
        val d2 = acx * apx + acy * apy + acz * apz
        if (d1 <= 0f && d2 <= 0f) {
            baryOut[0] = 1f; baryOut[1] = 0f; baryOut[2] = 0f
            return apx * apx + apy * apy + apz * apz
        }

        val bpx = px - bx; val bpy = py - by; val bpz = pz - bz
        val d3 = abx * bpx + aby * bpy + abz * bpz
        val d4 = acx * bpx + acy * bpy + acz * bpz
        if (d3 >= 0f && d4 <= d3) {
            baryOut[0] = 0f; baryOut[1] = 1f; baryOut[2] = 0f
            return bpx * bpx + bpy * bpy + bpz * bpz
        }

        val vc = d1 * d4 - d3 * d2
        if (vc <= 0f && d1 >= 0f && d3 <= 0f) {
            val v = d1 / (d1 - d3).coerceAtLeast(1e-8f)
            baryOut[0] = 1f - v; baryOut[1] = v; baryOut[2] = 0f
            val qx = ax + abx * v - px; val qy = ay + aby * v - py; val qz = az + abz * v - pz
            return qx * qx + qy * qy + qz * qz
        }

        val cpx = px - cx; val cpy = py - cy; val cpz = pz - cz
        val d5 = abx * cpx + aby * cpy + abz * cpz
        val d6 = acx * cpx + acy * cpy + acz * cpz
        if (d6 >= 0f && d5 <= d6) {
            baryOut[0] = 0f; baryOut[1] = 0f; baryOut[2] = 1f
            return cpx * cpx + cpy * cpy + cpz * cpz
        }

        val vb = d5 * d2 - d1 * d6
        if (vb <= 0f && d2 >= 0f && d6 <= 0f) {
            val w = d2 / (d2 - d6).coerceAtLeast(1e-8f)
            baryOut[0] = 1f - w; baryOut[1] = 0f; baryOut[2] = w
            val qx = ax + acx * w - px; val qy = ay + acy * w - py; val qz = az + acz * w - pz
            return qx * qx + qy * qy + qz * qz
        }

        val va = d3 * d6 - d5 * d4
        if (va <= 0f && (d4 - d3) >= 0f && (d5 - d6) >= 0f) {
            val w = (d4 - d3) / ((d4 - d3) + (d5 - d6)).coerceAtLeast(1e-8f)
            baryOut[0] = 0f; baryOut[1] = 1f - w; baryOut[2] = w
            val qx = bx + (cx - bx) * w - px; val qy = by + (cy - by) * w - py; val qz = bz + (cz - bz) * w - pz
            return qx * qx + qy * qy + qz * qz
        }

        val denom = 1f / (va + vb + vc).coerceAtLeast(1e-8f)
        val v = vb * denom
        val w = vc * denom
        val u = 1f - v - w
        baryOut[0] = u; baryOut[1] = v; baryOut[2] = w
        val qx = ax + abx * v + acx * w - px
        val qy = ay + aby * v + acy * w - py
        val qz = az + abz * v + acz * w - pz
        return qx * qx + qy * qy + qz * qz
    }

    // -------------------------------------------------------------------------
    // 60 FPS HIP-SOCKET ARTICULATED LEG-SPREAD SKINNING (BODY + TOES + TOENAILS)
    // -------------------------------------------------------------------------
    const val HIP_PIVOT_AX = 1.12f
    const val HIP_PIVOT_Y = 0.26f
    const val HIP_PIVOT_Z = -0.22f
    const val MAX_LEG_SPREAD_RAD = 0.25f
    const val MANUAL_MIN_LEG_SPREAD = -0.30f
    const val PEAK_MIN_LEG_SPREAD = -1.00f

    class LegSkinCache(
        val bodyVertIndices: IntArray,
        val bodyBasePosNorm: FloatArray,
        val bodyWeights: FloatArray,
        val bodySides: FloatArray,
        val bodyKneeShiftWeights: FloatArray,
        val bodyKneeNormalTilt: FloatArray,
        val bodyHipSwingWeights: FloatArray,
        val bodyHipSwingKneeShiftWeights: FloatArray,
        val bodyHipSwingKneeNormalTilt: FloatArray,
        val toesBasePosNorm: FloatArray,
        val toesSides: FloatArray,
        val toeNailStartVert: Int,
        val toeNailsBasePosNorm: FloatArray,
        val toeNailsSides: FloatArray
    )

    fun buildLegSkinCache(
        bodyMesh: DynamicGlMesh,
        toesMesh: DynamicGlMesh,
        nailsMesh: DynamicGlMesh,
        toeNailStartVert: Int
    ): LegSkinCache {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData

        val idxBuf = IntArray(bodyMesh.vertexCount)
        val weightBuf = FloatArray(bodyMesh.vertexCount)
        val sideBuf = FloatArray(bodyMesh.vertexCount)
        val kneeWeightBuf = FloatArray(bodyMesh.vertexCount)
        val kneeTiltBuf = FloatArray(bodyMesh.vertexCount)
        val hipSwingWeightBuf = FloatArray(bodyMesh.vertexCount)
        val hipSwingKneeWeightBuf = FloatArray(bodyMesh.vertexCount)
        val hipSwingKneeTiltBuf = FloatArray(bodyMesh.vertexCount)
        var count = 0

        // Femur bone vector in +X half-space (Hip -> Knee)
        val fdx = 1.80f - HIP_PIVOT_AX
        val fdy = -1.86f - HIP_PIVOT_Y
        val fdz = 1.74f - HIP_PIVOT_Z
        val fLen2 = fdx * fdx + fdy * fdy + fdz * fdz

        for (i in 0 until bodyMesh.vertexCount) {
            val base = i * stride
            val x = bData[base]
            val y = bData[base + 1]
            val z = bData[base + 2]
            if (y > 0.44f) continue

            val ax = abs(x)
            // Keep resting forearm, wrist & palm strictly stationary without slicing outer upper-thigh skin!
            if (isRestingHandOrDistalForearm(ax, y, z)) continue

            // 3D anterior pelvic collar guard (v29.0): keeps actual anterior pelvic-collar attachment
            // vertices 100% pinned (zero vulva-patch seam drift or Seam D bridge leakage) and feathers
            // neighboring pelvic-collar vertices gently across the inguinal crease, while freeing
            // forward inner-thigh tissue once clear of the anterior pelvic surface in depth (+Z).
            // Applied identically to both spread (w) and planted-knee (kw) weights so strong inward
            // squeezes never fold the upper inner thigh.
            val ovalR = sqrt((x / PATCH_RX).let { it * it } + ((y - PATCH_CENTER_Y) / PATCH_RY).let { it * it })
            val collarSurfaceZ = 0.56f
            val collarDzNorm = max(0f, z - collarSurfaceZ) / 0.30f
            val collarDist3d = sqrt(ovalR * ovalR + collarDzNorm * collarDzNorm)
            if (collarDist3d <= 1.05f) continue

            val hFem = ((ax - HIP_PIVOT_AX) * fdx + (y - HIP_PIVOT_Y) * fdy + (z - HIP_PIVOT_Z) * fdz) / fLen2
            val patchGuard = smoothStep(1.05f, 1.68f, collarDist3d)
            val groinYGuard = smoothStep(0.38f, -0.58f, y)
            val femRamp = smoothStep(0.10f, 0.54f, hFem)
            val lowerLimbFull = smoothStep(-1.05f, -1.65f, y)
            val w = max(femRamp * groinYGuard, lowerLimbFull) * patchGuard

            // v30.0 Beta ("Hip-anchored thigh swing"): distributes both spread rotation and planted-knee
            // lateral travel linearly along the femur from the authored hip pivot (hFem=0) to the knee
            // (hFem=1) with a soft pelvis attachment, eliminating the apparent low mid-thigh hinge while
            // keeping the exact same knee travel (w=1, kw=1 at knee), planted feet, 3D pelvic collar guard,
            // and blending smoothly across |x| = 0.76..1.44 (bounded d(kw)/dx << 0.80 to prevent any
            // horizontal foldover) from proven fold-free medial groin weights to full hip-anchored swing.
            val lateralFemurFactor = smoothStep(0.76f, 1.44f, ax)
            val hipSwingPelvisGuard = smoothStep(0.42f, -0.14f, y)
            val hipSwingFemRamp = smoothStep(-0.04f, 0.34f, hFem)
            val wHipSwingFull = max(hipSwingFemRamp * hipSwingPelvisGuard, lowerLimbFull) * patchGuard
            val wHipSwing = w + (wHipSwingFull - w) * lateralFemurFactor

            if (w > 0.001f || wHipSwing > 0.001f) {
                // Planted-foot knee/upper-leg shift weight: 0 at pelvis & vulva, rises smoothly to 1.0 at knee,
                // and tapers cleanly back to 0.0 above ankle/foot (y <= -3.85) so feet stay 100% planted!
                val ankleAnchor = smoothStep(-3.86f, -2.02f, y)
                val thighRamp = smoothStep(0.18f, 0.94f, hFem) * groinYGuard * patchGuard
                val kw = if (w > 0.001f) (thighRamp * ankleAnchor).coerceIn(0f, 1f) else 0f
                val tiltDir = (smoothStep(-2.12f, -1.58f, y) * 2f - 1f) * kw

                val femLinear = max(hFem.coerceIn(0f, 1f), smoothStep(-1.45f, -2.05f, y))
                val hipRootEase = smoothStep(0.02f, 0.22f, hFem)
                val kwHipSwingFull = (femLinear * (0.20f + 0.80f * hipRootEase) * hipSwingPelvisGuard * patchGuard * ankleAnchor).coerceIn(0f, 1f)
                val kwHipSwing = if (wHipSwing > 0.001f) {
                    (kw + (kwHipSwingFull - kw) * lateralFemurFactor).coerceIn(0f, 1f)
                } else 0f
                val tiltDirHipSwing = (smoothStep(-2.12f, -1.58f, y) * 2f - 1f) * kwHipSwing

                idxBuf[count] = i
                weightBuf[count] = if (w > 0.001f) w.coerceIn(0f, 1f) else 0f
                sideBuf[count] = if (x >= 0f) 1f else -1f
                kneeWeightBuf[count] = kw
                kneeTiltBuf[count] = tiltDir
                hipSwingWeightBuf[count] = if (wHipSwing > 0.001f) wHipSwing.coerceIn(0f, 1f) else 0f
                hipSwingKneeWeightBuf[count] = kwHipSwing
                hipSwingKneeTiltBuf[count] = tiltDirHipSwing
                count++
            }
        }

        val bodyIndices = idxBuf.copyOf(count)
        val bodyWeights = weightBuf.copyOf(count)
        val bodySides = sideBuf.copyOf(count)
        val bodyKneeWeights = kneeWeightBuf.copyOf(count)
        val bodyKneeTilt = kneeTiltBuf.copyOf(count)
        val bodyHipSwingWeights = hipSwingWeightBuf.copyOf(count)
        val bodyHipSwingKneeWeights = hipSwingKneeWeightBuf.copyOf(count)
        val bodyHipSwingKneeTilt = hipSwingKneeTiltBuf.copyOf(count)
        val bodyBase = FloatArray(count * 6)
        for (k in 0 until count) {
            val src = bodyIndices[k] * stride
            val dst = k * 6
            bodyBase[dst] = bData[src]; bodyBase[dst + 1] = bData[src + 1]; bodyBase[dst + 2] = bData[src + 2]
            bodyBase[dst + 3] = bData[src + 3]; bodyBase[dst + 4] = bData[src + 4]; bodyBase[dst + 5] = bData[src + 5]
        }

        val tData = toesMesh.vertexData
        val toesBase = FloatArray(toesMesh.vertexCount * 6)
        val toesSides = FloatArray(toesMesh.vertexCount)
        for (i in 0 until toesMesh.vertexCount) {
            val src = i * stride
            val dst = i * 6
            toesBase[dst] = tData[src]; toesBase[dst + 1] = tData[src + 1]; toesBase[dst + 2] = tData[src + 2]
            toesBase[dst + 3] = tData[src + 3]; toesBase[dst + 4] = tData[src + 4]; toesBase[dst + 5] = tData[src + 5]
            toesSides[i] = if (tData[src] >= 0f) 1f else -1f
        }

        val nData = nailsMesh.vertexData
        val toeNailCount = (nailsMesh.vertexCount - toeNailStartVert).coerceAtLeast(0)
        val toeNailsBase = FloatArray(toeNailCount * 6)
        val toeNailsSides = FloatArray(toeNailCount)
        for (k in 0 until toeNailCount) {
            val src = (toeNailStartVert + k) * stride
            val dst = k * 6
            toeNailsBase[dst] = nData[src]; toeNailsBase[dst + 1] = nData[src + 1]; toeNailsBase[dst + 2] = nData[src + 2]
            toeNailsBase[dst + 3] = nData[src + 3]; toeNailsBase[dst + 4] = nData[src + 4]; toeNailsBase[dst + 5] = nData[src + 5]
            toeNailsSides[k] = if (nData[src] >= 0f) 1f else -1f
        }

        return LegSkinCache(
            bodyVertIndices = bodyIndices,
            bodyBasePosNorm = bodyBase,
            bodyWeights = bodyWeights,
            bodySides = bodySides,
            bodyKneeShiftWeights = bodyKneeWeights,
            bodyKneeNormalTilt = bodyKneeTilt,
            bodyHipSwingWeights = bodyHipSwingWeights,
            bodyHipSwingKneeShiftWeights = bodyHipSwingKneeWeights,
            bodyHipSwingKneeNormalTilt = bodyHipSwingKneeTilt,
            toesBasePosNorm = toesBase,
            toesSides = toesSides,
            toeNailStartVert = toeNailStartVert,
            toeNailsBasePosNorm = toeNailsBase,
            toeNailsSides = toeNailsSides
        )
    }

    /**
     * Evaluates the exact world-space position of any leg point after interactive hip-socket abduction.
     */
    fun transformLegPoint(x: Float, y: Float, z: Float, side: Float, spread: Float, weight: Float = 1f): FloatArray {
        val clampedSpread = spread.coerceIn(PEAK_MIN_LEG_SPREAD, 1.0f)
        val angle = side * clampedSpread * MAX_LEG_SPREAD_RAD * weight
        val cosA = cos(angle)
        val sinA = sin(angle)
        val pivotX = side * HIP_PIVOT_AX
        val rx = x - pivotX
        val ry = y - HIP_PIVOT_Y
        val nx = pivotX + rx * cosA - ry * sinA
        val ny = HIP_PIVOT_Y + rx * sinA + ry * cosA
        val kneeDrop = -0.14f * clampedSpread * weight * smoothStep(-0.55f, 1.50f, z)
        return floatArrayOf(nx, ny, z + kneeDrop)
    }

    fun applyLegSpread(
        bodyMesh: DynamicGlMesh,
        toesMesh: DynamicGlMesh,
        nailsMesh: DynamicGlMesh,
        cache: LegSkinCache,
        leftLegSpread: Float,
        rightLegSpread: Float,
        includeToesAndNails: Boolean = true,
        commitBody: Boolean = true,
        leftKneeShift: Float = 0f,
        rightKneeShift: Float = 0f,
        leftToeWiggle: Float = 0f,
        rightToeWiggle: Float = 0f,
        useHipAnchoredThighSwing: Boolean = false
    ) {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val leftS = leftLegSpread.coerceIn(PEAK_MIN_LEG_SPREAD, 1.0f)
        val rightS = rightLegSpread.coerceIn(PEAK_MIN_LEG_SPREAD, 1.0f)
        val leftKs = leftKneeShift.coerceIn(-1.15f, 1.0f)
        val rightKs = rightKneeShift.coerceIn(-1.15f, 1.0f)
        val hasKneeShift = abs(leftKs) > 1e-4f || abs(rightKs) > 1e-4f
        val leftTw = leftToeWiggle.coerceIn(-1f, 1f)
        val rightTw = rightToeWiggle.coerceIn(-1f, 1f)
        val hasToeWiggle = abs(leftTw) > 1e-4f || abs(rightTw) > 1e-4f

        val bData = bodyMesh.vertexData
        val bIdx = cache.bodyVertIndices
        val bBase = cache.bodyBasePosNorm
        val bWeights = if (useHipAnchoredThighSwing) cache.bodyHipSwingWeights else cache.bodyWeights
        val bSides = cache.bodySides
        val bKneeW = if (useHipAnchoredThighSwing) cache.bodyHipSwingKneeShiftWeights else cache.bodyKneeShiftWeights
        val bKneeTilt = if (useHipAnchoredThighSwing) cache.bodyHipSwingKneeNormalTilt else cache.bodyKneeNormalTilt
        for (k in bIdx.indices) {
            val side = bSides[k]
            val spread = if (side < 0f) leftS else rightS
            val w = bWeights[k]
            val angle = side * spread * MAX_LEG_SPREAD_RAD * w
            val cosA = cos(angle)
            val sinA = sin(angle)
            val pivotX = side * HIP_PIVOT_AX

            val src = k * 6
            val x = bBase[src]; val y = bBase[src + 1]; val z = bBase[src + 2]
            val nx = bBase[src + 3]; val ny = bBase[src + 4]; val nz = bBase[src + 5]

            val rx = x - pivotX
            val ry = y - HIP_PIVOT_Y
            val kneeDrop = -0.14f * spread * w * smoothStep(-0.55f, 1.50f, z)

            var outX = pivotX + rx * cosA - ry * sinA
            var outY = HIP_PIVOT_Y + rx * sinA + ry * cosA
            var outZ = z + kneeDrop
            var outNx = nx * cosA - ny * sinA
            var outNy = nx * sinA + ny * cosA
            var outNz = nz

            if (hasKneeShift) {
                val kw = bKneeW[k]
                if (kw > 0.0005f) {
                    val ks = if (side < 0f) leftKs else rightKs
                    if (abs(ks) > 1e-4f) {
                        val lateralAmp = if (ks < 0f) 0.94f else 0.78f
                        var shiftedX = outX + side * ks * lateralAmp * kw
                        // Soft anatomical midline contact barrier so squeezed thighs/knees press snugly together with ZERO interpenetration
                        val signedX = shiftedX * side
                        if (signedX < 0.055f) {
                            val compressed = 0.026f + 0.029f * exp(((signedX - 0.055f) / 0.065f).coerceAtLeast(-12f))
                            shiftedX = side * compressed
                        }
                        outX = shiftedX
                        outZ += -0.095f * ks * kw * smoothStep(0.05f, 1.55f, z)

                        val tiltAngle = side * ks * 0.21f * bKneeTilt[k]
                        if (abs(tiltAngle) > 1e-4f) {
                            val ct = cos(tiltAngle)
                            val st = sin(tiltAngle)
                            val rnx = outNx * ct - outNy * st
                            val rny = outNx * st + outNy * ct
                            outNx = rnx
                            outNy = rny
                        }
                    }
                }
            }

            val dst = bIdx[k] * stride
            bData[dst] = outX
            bData[dst + 1] = outY
            bData[dst + 2] = outZ
            bData[dst + 3] = outNx
            bData[dst + 4] = outNy
            bData[dst + 5] = outNz
        }
        if (commitBody) {
            bodyMesh.commitVertices()
        }

        if (!includeToesAndNails) return

        val toeFloorZ = BlossomBodyBuilder.MATTRESS_TOP_Z + 0.014f
        val tData = toesMesh.vertexData
        val tBase = cache.toesBasePosNorm
        val tSides = cache.toesSides
        for (i in 0 until toesMesh.vertexCount) {
            val side = tSides[i]
            val spread = if (side < 0f) leftS else rightS
            val angle = side * spread * MAX_LEG_SPREAD_RAD
            val cosA = cos(angle)
            val sinA = sin(angle)
            val pivotX = side * HIP_PIVOT_AX

            val src = i * 6
            var x = tBase[src]; var y = tBase[src + 1]; var z = tBase[src + 2]
            val nx = tBase[src + 3]; val ny = tBase[src + 4]; val nz = tBase[src + 5]

            if (hasToeWiggle) {
                val tw = if (side < 0f) leftTw else rightTw
                if (abs(tw) > 1e-4f) {
                    val toeS = smoothStep(-5.06f, -5.56f, y)
                    if (toeS > 0.001f) {
                        val toeWave = 0.72f + 0.36f * cos(((abs(x) - 1.62f) * 6.5f) - tw * 1.1f)
                        val liftZ = tw * 0.044f * toeS * toeWave
                        val curlY = max(0f, -tw) * 0.022f * toeS * toeWave
                        y += curlY
                        z = max(z + liftZ, toeFloorZ)
                    }
                }
            }

            val rx = x - pivotX
            val ry = y - HIP_PIVOT_Y

            val dst = i * stride
            tData[dst] = pivotX + rx * cosA - ry * sinA
            tData[dst + 1] = HIP_PIVOT_Y + rx * sinA + ry * cosA
            tData[dst + 2] = z
            tData[dst + 3] = nx * cosA - ny * sinA
            tData[dst + 4] = nx * sinA + ny * cosA
            tData[dst + 5] = nz
        }
        toesMesh.commitVertices()

        val nData = nailsMesh.vertexData
        val nBase = cache.toeNailsBasePosNorm
        val nSides = cache.toeNailsSides
        val nStart = cache.toeNailStartVert
        for (k in nSides.indices) {
            val side = nSides[k]
            val spread = if (side < 0f) leftS else rightS
            val angle = side * spread * MAX_LEG_SPREAD_RAD
            val cosA = cos(angle)
            val sinA = sin(angle)
            val pivotX = side * HIP_PIVOT_AX

            val src = k * 6
            var x = nBase[src]; var y = nBase[src + 1]; var z = nBase[src + 2]
            val nx = nBase[src + 3]; val ny = nBase[src + 4]; val nz = nBase[src + 5]

            if (hasToeWiggle) {
                val tw = if (side < 0f) leftTw else rightTw
                if (abs(tw) > 1e-4f) {
                    val toeS = smoothStep(-5.06f, -5.56f, y)
                    if (toeS > 0.001f) {
                        val toeWave = 0.72f + 0.36f * cos(((abs(x) - 1.62f) * 6.5f) - tw * 1.1f)
                        val liftZ = tw * 0.044f * toeS * toeWave
                        val curlY = max(0f, -tw) * 0.022f * toeS * toeWave
                        y += curlY
                        z = max(z + liftZ, toeFloorZ + 0.004f)
                    }
                }
            }

            val rx = x - pivotX
            val ry = y - HIP_PIVOT_Y

            val dst = (nStart + k) * stride
            nData[dst] = pivotX + rx * cosA - ry * sinA
            nData[dst + 1] = HIP_PIVOT_Y + rx * sinA + ry * cosA
            nData[dst + 2] = z
            nData[dst + 3] = nx * cosA - ny * sinA
            nData[dst + 4] = nx * sinA + ny * cosA
            nData[dst + 5] = nz
        }
        nailsMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // SUBTLE ELASTIC GLUTEAL SOFT-TISSUE INTERACTIVITY (COMPOSED WITH LEG SPREAD)
    // -------------------------------------------------------------------------
    class GluteSkinCache(
        val bodyVertIndices: IntArray,
        val bodyRestPosNorm: FloatArray,
        val bodyWeights: FloatArray,
        val bodySides: FloatArray,
        val isInLegCache: BooleanArray
    )

    /**
     * Shared C1-continuous soft-tissue elasticity weight for Blossom's left and right buttocks,
     * covering BOTH the fleshy medial-inferior cheeks directly to the left and right of her anus
     * AND the outer-posterior buttock cheeks, while keeping the anus opening (`|x| < 0.14`),
     * vulva/labia (`y > -0.34`), and resting mattress contact cleanly anchored.
     */
    fun gluteElasticWeight(x: Float, y: Float, z: Float): Float {
        if (y !in -1.36f..-0.34f) return 0f
        val ax = abs(x)
        if (ax !in 0.14f..1.74f) return 0f
        if (y > -0.72f && ax > 1.78f && z < -0.35f) return 0f

        val midlineGuard = smoothStep(0.14f, 0.34f, ax)
        val upperGuard = smoothStep(-0.34f, -0.62f, y)
        val frontGuard = smoothStep(0.28f, -0.06f, z)
        val matZ = mattressSurfaceZ(y)
        val matGuard = smoothStep(matZ + 0.01f, matZ + 0.12f, z)
        val guard = midlineGuard * upperGuard * frontGuard * matGuard
        if (guard <= 0.002f) return 0f

        // Primary lobe: fleshy medial-inferior buttock cheek directly left & right of her anus (-0.86)
        val mx = (ax - 0.68f) / 0.48f
        val my = (y + 0.88f) / 0.44f
        val mz = (z + 0.34f) / 0.48f
        val bellMedial = exp(-(mx * mx + my * my + mz * mz) * 1.10f)

        // Secondary lobe: outer-posterior buttock cheek visible from 3/4 angles
        val ox = (ax - 1.12f) / 0.58f
        val oy = (y + 0.72f) / 0.52f
        val oz = (z + 0.74f) / 0.50f
        val bellOuter = exp(-(ox * ox + oy * oy + oz * oz) * 1.05f)

        val bell = 1f - (1f - bellMedial) * (1f - bellOuter)
        return (bell * guard).coerceIn(0f, 1f)
    }

    fun buildGluteSkinCache(
        bodyMesh: DynamicGlMesh,
        legCache: LegSkinCache
    ): GluteSkinCache {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val legMember = BooleanArray(bodyMesh.vertexCount)
        for (idx in legCache.bodyVertIndices) {
            if (idx in legMember.indices) legMember[idx] = true
        }

        val idxBuf = IntArray(bodyMesh.vertexCount)
        val weightBuf = FloatArray(bodyMesh.vertexCount)
        val sideBuf = FloatArray(bodyMesh.vertexCount)
        val inLegBuf = BooleanArray(bodyMesh.vertexCount)
        var count = 0

        for (i in 0 until bodyMesh.vertexCount) {
            val base = i * stride
            val x = bData[base]
            val y = bData[base + 1]
            val z = bData[base + 2]
            val w = gluteElasticWeight(x, y, z)
            if (w > 0.005f) {
                idxBuf[count] = i
                weightBuf[count] = w
                sideBuf[count] = if (x >= 0f) 1f else -1f
                inLegBuf[count] = legMember[i]
                count++
            }
        }

        val indices = idxBuf.copyOf(count)
        val rest = FloatArray(count * 6)
        for (k in 0 until count) {
            val src = indices[k] * stride
            val dst = k * 6
            rest[dst] = bData[src]; rest[dst + 1] = bData[src + 1]; rest[dst + 2] = bData[src + 2]
            rest[dst + 3] = bData[src + 3]; rest[dst + 4] = bData[src + 4]; rest[dst + 5] = bData[src + 5]
        }

        return GluteSkinCache(
            bodyVertIndices = indices,
            bodyRestPosNorm = rest,
            bodyWeights = weightBuf.copyOf(count),
            bodySides = sideBuf.copyOf(count),
            isInLegCache = inLegBuf.copyOf(count)
        )
    }

    /**
     * Applies very slight, volume-preserving elastic soft-tissue yield to Blossom's left and right buttocks.
     * Must be invoked immediately after [applyLegSpread] so leg-shared vertices compose cleanly.
     */
    fun applyGluteElasticity(
        bodyMesh: DynamicGlMesh,
        cache: GluteSkinCache,
        leftGluteX: Float,
        leftGluteY: Float,
        rightGluteX: Float,
        rightGluteY: Float
    ) {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val lx = leftGluteX.coerceIn(-1f, 1f)
        val ly = leftGluteY.coerceIn(-1f, 1f)
        val rx = rightGluteX.coerceIn(-1f, 1f)
        val ry = rightGluteY.coerceIn(-1f, 1f)

        val indices = cache.bodyVertIndices
        val rest = cache.bodyRestPosNorm
        val weights = cache.bodyWeights
        val sides = cache.bodySides
        val inLeg = cache.isInLegCache

        for (k in indices.indices) {
            val dst = indices[k] * stride
            val baseX: Float
            val baseY: Float
            val baseZ: Float
            val baseNx: Float
            val baseNy: Float
            val baseNz: Float
            if (inLeg[k]) {
                baseX = bData[dst]; baseY = bData[dst + 1]; baseZ = bData[dst + 2]
                baseNx = bData[dst + 3]; baseNy = bData[dst + 4]; baseNz = bData[dst + 5]
            } else {
                val src = k * 6
                baseX = rest[src]; baseY = rest[src + 1]; baseZ = rest[src + 2]
                baseNx = rest[src + 3]; baseNy = rest[src + 4]; baseNz = rest[src + 5]
            }

            val side = sides[k]
            val gx = if (side < 0f) lx else rx
            val gy = if (side < 0f) ly else ry
            val w = weights[k]

            if (abs(gx) < 1e-4f && abs(gy) < 1e-4f) {
                bData[dst] = baseX; bData[dst + 1] = baseY; bData[dst + 2] = baseZ
                bData[dst + 3] = baseNx; bData[dst + 4] = baseNy; bData[dst + 5] = baseNz
                continue
            }

            // Very slight, realistic soft-tissue displacement + gentle volume-preserving cushion swell
            val dx = gx * 0.068f * w
            val dy = gy * 0.064f * w
            val compressMag = (abs(gx) + abs(gy)).coerceIn(0f, 1.4f)
            val dz = (-0.015f * compressMag + 0.020f * (-gx * side)) * w

            val newY = baseY + dy
            val minZ = mattressSurfaceZ(newY) + 0.008f
            bData[dst] = baseX + dx
            bData[dst + 1] = newY
            bData[dst + 2] = max(baseZ + dz, minZ)

            var nnx = baseNx + gx * 0.08f * w
            var nny = baseNy + gy * 0.08f * w
            var nnz = baseNz
            val nLen = sqrt(nnx * nnx + nny * nny + nnz * nnz).coerceAtLeast(1e-5f)
            nnx /= nLen; nny /= nLen; nnz /= nLen
            bData[dst + 3] = nnx
            bData[dst + 4] = nny
            bData[dst + 5] = nnz
        }
        bodyMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // SDF PRIMITIVES & FAST PRIMITIVE COLLECTIONS
    // -------------------------------------------------------------------------
    private fun smin(a: Float, b: Float, k: Float): Float {
        val h = (k - abs(a - b)).coerceAtLeast(0f) / k
        return min(a, b) - h * h * h * k * (1f / 6f)
    }

    private fun smax(a: Float, b: Float, k: Float): Float = -smin(-a, -b, k)

    private fun sdEllipse2D(px: Float, py: Float, rx: Float, ry: Float): Float {
        val k0x = px / rx
        val k0y = py / ry
        val k0 = sqrt(k0x * k0x + k0y * k0y)
        if (k0 < 1e-6f) return -min(rx, ry)
        val k1x = px / (rx * rx)
        val k1y = py / (ry * ry)
        val k1 = sqrt(k1x * k1x + k1y * k1y)
        return k0 * (k0 - 1f) / k1
    }

    private fun sdEllipsoid(px: Float, py: Float, pz: Float, rx: Float, ry: Float, rz: Float): Float {
        val k0x = px / rx
        val k0y = py / ry
        val k0z = pz / rz
        val k0 = sqrt(k0x * k0x + k0y * k0y + k0z * k0z)
        if (k0 < 1e-6f) return -min(rx, min(ry, rz))
        val k1x = px / (rx * rx)
        val k1y = py / (ry * ry)
        val k1z = pz / (rz * rz)
        val k1 = sqrt(k1x * k1x + k1y * k1y + k1z * k1z)
        return k0 * (k0 - 1f) / k1
    }

    private fun sdCapsule(
        px: Float, py: Float, pz: Float,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        rA: Float, rB: Float
    ): Float {
        val pax = px - ax; val pay = py - ay; val paz = pz - az
        val bax = bx - ax; val bay = by - ay; val baz = bz - az
        val ba2 = bax * bax + bay * bay + baz * baz
        val h = ((pax * bax + pay * bay + paz * baz) / ba2).coerceIn(0f, 1f)
        val dx = pax - bax * h
        val dy = pay - bay * h
        val dz = paz - baz * h
        val r = rA + (rB - rA) * h
        return sqrt(dx * dx + dy * dy + dz * dz) - r
    }

    private fun sqDist(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        val dx = ax - bx; val dy = ay - by; val dz = az - bz
        return dx * dx + dy * dy + dz * dz
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun quinticStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * t * (t * (t * 6f - 15f) + 10f)
    }

    private fun smoothBell(x: Float, start: Float, end: Float, feather: Float): Float {
        return smoothStep(start - feather, start + feather, x) *
            (1f - smoothStep(end - feather, end + feather, x))
    }

    private class FloatArrayList(initialCapacity: Int) {
        var data = FloatArray(initialCapacity)
        var size = 0
        fun add(v: Float) {
            if (size == data.size) data = data.copyOf(data.size * 2)
            data[size++] = v
        }
        fun toTrimmedArray(): FloatArray = data.copyOf(size)
    }

    private class IntArrayList(initialCapacity: Int) {
        var data = IntArray(initialCapacity)
        var size = 0
        fun add4(a: Int, b: Int, c: Int, d: Int) {
            if (size + 4 > data.size) data = data.copyOf(data.size * 2)
            data[size++] = a
            data[size++] = b
            data[size++] = c
            data[size++] = d
        }
    }

    private class ShortArrayList(initialCapacity: Int) {
        var data = ShortArray(initialCapacity)
        var size = 0
        fun add6(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) {
            if (size + 6 > data.size) data = data.copyOf(data.size * 2)
            data[size++] = a.toShort()
            data[size++] = b.toShort()
            data[size++] = c.toShort()
            data[size++] = d.toShort()
            data[size++] = e.toShort()
            data[size++] = f.toShort()
        }
        fun toTrimmedArray(): ShortArray = data.copyOf(size)
    }
}
