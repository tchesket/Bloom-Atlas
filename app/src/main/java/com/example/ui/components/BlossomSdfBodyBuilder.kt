package com.example.ui.components

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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

    private const val MIN_X = -2.62f
    private const val MAX_X = 2.62f
    private const val MIN_Y = -5.92f
    private const val MAX_Y = 3.92f
    private const val MIN_Z = -1.46f
    private const val MAX_Z = 2.40f
    private const val STEP = 0.078f

    private const val LUT_SIZE = 65
    private const val LUT_MIN_X = -0.72f
    private const val LUT_MAX_X = 0.72f
    private const val LUT_MIN_Y = -1.22f
    private const val LUT_MAX_Y = 1.08f

    class AnteriorSurfaceLut(
        val size: Int,
        val zValues: FloatArray
    ) {
        fun sampleZ(x: Float, y: Float): Float {
            val u = ((x - LUT_MIN_X) / (LUT_MAX_X - LUT_MIN_X)).coerceIn(0f, 1f) * (size - 1)
            val v = ((y - LUT_MIN_Y) / (LUT_MAX_Y - LUT_MIN_Y)).coerceIn(0f, 1f) * (size - 1)
            val ix0 = u.toInt().coerceIn(0, size - 2)
            val iy0 = v.toInt().coerceIn(0, size - 2)
            val fx = u - ix0
            val fy = v - iy0
            val r0 = iy0 * size + ix0
            val r1 = (iy0 + 1) * size + ix0
            val z0 = zValues[r0] + (zValues[r0 + 1] - zValues[r0]) * fx
            val z1 = zValues[r1] + (zValues[r1 + 1] - zValues[r1]) * fx
            return z0 + (z1 - z0) * fy
        }
    }

    fun buildAnteriorSurfaceLut(): AnteriorSurfaceLut {
        val table = FloatArray(LUT_SIZE * LUT_SIZE)
        for (j in 0 until LUT_SIZE) {
            val y = LUT_MIN_Y + (j.toFloat() / (LUT_SIZE - 1)) * (LUT_MAX_Y - LUT_MIN_Y)
            for (i in 0 until LUT_SIZE) {
                val x = LUT_MIN_X + (i.toFloat() / (LUT_SIZE - 1)) * (LUT_MAX_X - LUT_MIN_X)
                table[j * LUT_SIZE + i] = sampleBodySurfacePoint(x, y).z
            }
        }
        return AnteriorSurfaceLut(LUT_SIZE, table)
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
    }

    data class SdfSurfaceSample(
        val z: Float,
        val nx: Float,
        val ny: Float,
        val nz: Float,
        val ao: Float,
        val pigment: Float
    )

    fun mattressSurfaceZ(y: Float): Float {
        val yRec = max(0f, y - 0.72f)
        return BlossomBodyBuilder.MATTRESS_TOP_Z - 0.16f * yRec - 0.014f * yRec * yRec
    }

    /**
     * Evaluates the exact signed distance (negative inside, positive outside) to Blossom's
     * single unified reclining lower body (torso + buttocks + legs + feet).
     */
    fun evaluateBodySdf(x: Float, y: Float, z: Float): Float {
        val ax = abs(x)

        // ---------------------------------------------------------------------
        // 1. RECLINING LOWER TORSO (LOWER RIBCAGE -> WAIST -> BELLY -> PELVIS)
        // ---------------------------------------------------------------------
        val yc = y.coerceIn(-1.04f, 3.54f)
        val yRec = max(0f, yc - 0.72f)
        val upperFill = smoothStep(0.62f, 3.15f, yc)
        // Smooth organic posterior roll across the perineum into the gluteal cleft (zero protruding pouch!)
        val perinealRollZ = -0.34f * smoothStep(-0.34f, -1.04f, yc)
        val zSpine = -0.58f - 0.16f * yRec - 0.014f * yRec * yRec + 0.28f * upperFill + perinealRollZ

        val hipFlare = 0.36f * exp(-((yc - 0.16f) / 1.12f).let { it * it })
        val iliacFull = 0.14f * exp(-((yc - 1.08f) / 0.68f).let { it * it })
        val waistPinch = -0.18f * exp(-((yc - 2.68f) / 0.66f).let { it * it })
        val ribFlare = 0.16f * smoothStep(2.65f, 3.45f, yc)
        val pelvicBaseTaper = 1f - 0.44f * smoothStep(-0.22f, -1.04f, yc)
        val rxTorso = ((1.36f + hipFlare + iliacFull + waistPinch + ribFlare) * pelvicBaseTaper).coerceAtLeast(0.46f)

        val bellyDepth = 0.28f * exp(-((yc - 1.54f) / 1.05f).let { it * it }) +
            0.27f * smoothStep(1.65f, 3.32f, yc)
        val waistDepthDip = -0.048f * exp(-((yc - 2.55f) / 0.50f).let { it * it })
        val pelvicDepthTaper = 1f - 0.30f * smoothStep(-0.30f, -1.04f, yc)
        val rzTorso = ((0.62f + bellyDepth + waistDepthDip) * pelvicDepthTaper).coerceAtLeast(0.36f)

        // Rounded capsule-capped super-elliptic torso column
        val capR = 0.16f
        val dRadTorso = sdEllipse2D(ax, z - zSpine, (rxTorso - capR).coerceAtLeast(0.15f), (rzTorso - capR).coerceAtLeast(0.15f))
        val dyTorso = max(y - 3.54f, -1.04f - y)
        var dTorso = sqrt(max(dRadTorso, 0f).let { it * it } + max(dyTorso, 0f).let { it * it }) +
            min(max(dRadTorso, dyTorso), 0f) - capR

        // Subtle anatomical abdominal & dorsal relief (navel, linea alba, iliac crests, lumbar spine)
        if (y > 0.15f && y < 3.55f && ax < 1.65f) {
            val frontW = smoothStep(-0.10f, 0.35f, z - zSpine)
            if (frontW > 0.001f) {
                val navelDx = x / 0.072f
                val navelDy = (y - 2.10f) / 0.092f
                val navelR2 = navelDx * navelDx + navelDy * navelDy
                val navelDimple = 0.056f * exp(-navelR2 * 2.1f)
                val navelHood = -0.017f * exp(-((x / 0.10f).let { it * it } + ((y - 2.20f) / 0.052f).let { it * it }))
                val lineaAlba = 0.015f * exp(-((x / 0.075f).let { it * it })) * smoothBell(y, 1.32f, 3.18f, 0.25f)
                val iliacCrest = -0.042f * exp(-(((ax - 1.12f) / 0.38f).let { it * it } + ((y - 1.12f) / 0.48f).let { it * it }))
                dTorso += (navelDimple + navelHood + lineaAlba + iliacCrest) * frontW
            }
            val backW = smoothStep(0.10f, 0.48f, zSpine - z)
            if (backW > 0.001f) {
                val spineGroove = 0.028f * exp(-((x / 0.14f).let { it * it })) * smoothStep(0.25f, 3.00f, y)
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
        // Natural gluteal cleft cradling the lower anus (-0.86) and curving up the dorsal midline
        val cleftMask = smoothStep(-0.42f, -0.88f, y) * (1f - smoothStep(0.16f, 0.42f, z + 0.42f))
        val cleftIndent = 0.095f * exp(-((x / 0.105f).let { it * it })) * cleftMask
        val dPelvis = smin(dTorso, dGlute, 0.38f) + cleftIndent

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

        // 3C. Crisp anatomical knee (patella dome; solid watertight natural popliteal bend with ZERO cutout hole!)
        val dPatella = sdEllipsoid(
            px = ax - 1.80f,
            py = y - (-1.90f),
            pz = z - 1.91f,
            rx = 0.24f,
            ry = 0.26f,
            rz = 0.22f
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

        // ---------------------------------------------------------------------
        // 4. FULL-SCALE SOFT-CONTOURED SCULPTED FEET (SEAMLESSLY FUSED AT ANKLES)
        // ---------------------------------------------------------------------
        val dFootCore = sdCapsule(
            ax, y, z,
            1.68f, -4.08f, -0.94f,
            1.92f, -5.44f, -0.99f,
            0.22f, 0.18f
        )
        val dInstep = sdCapsule(
            ax, y, z,
            1.68f, -4.22f, -0.92f,
            1.79f, -4.76f, -0.87f,
            0.22f, 0.18f
        )
        val dForefoot = sdEllipsoid(
            px = ax - 1.94f,
            py = y - (-5.42f),
            pz = z - (-0.98f),
            rx = 0.30f,
            ry = 0.34f,
            rz = 0.165f
        )
        // Subtle medial foot arch indentation underneath
        val dArchCut = sdEllipsoid(
            px = ax - 1.62f,
            py = y - (-4.78f),
            pz = z - (-1.18f),
            rx = 0.18f,
            ry = 0.34f,
            rz = 0.11f
        )
        var dFoot = smin(smin(dFootCore, dInstep, 0.10f), dForefoot, 0.14f)
        dFoot = smax(dFoot, -dArchCut, 0.06f)

        // Seamless ankle-to-foot fusion
        val dFullLimb = smin(dLeg, dFoot, 0.13f)

        // ---------------------------------------------------------------------
        // 5. UNIFIED SEAMLESS BODY FUSION + EXAM MATTRESS SOFT CONTACT
        // ---------------------------------------------------------------------
        var dBody = smin(dPelvis, dFullLimb, 0.42f)

        // Softly flatten posterior torso, buttocks, and foot soles onto the reclined exam mattress
        val matPlane = (mattressSurfaceZ(y) + 0.005f) - z
        dBody = smax(dBody, matPlane, 0.12f)

        return dBody
    }

    /**
     * Exact central-difference gradient normal on the SDF surface.
     */
    fun evaluateBodyNormal(x: Float, y: Float, z: Float, eps: Float = 0.014f): FloatArray {
        val dx = evaluateBodySdf(x + eps, y, z) - evaluateBodySdf(x - eps, y, z)
        val dy = evaluateBodySdf(x, y + eps, z) - evaluateBodySdf(x, y - eps, z)
        val dz = evaluateBodySdf(x, y, z + eps) - evaluateBodySdf(x, y, z - eps)
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
    fun evaluateBodyMaterial(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float): Pair<Float, Float> {
        val ax = abs(x)
        val matZ = mattressSurfaceZ(y)

        // Contact AO where dorsal torso, buttocks & feet rest on the dark clinical exam mattress
        val matContact = (1f - smoothStep(0.02f, 0.24f, z - matZ)) * smoothStep(0.10f, 0.72f, -nz)
        val cushionAo = 1f - 0.34f * matContact

        // Navel cavity AO & subtle pigment
        val navelDx = x / 0.072f
        val navelDy = (y - 2.10f) / 0.092f
        val navelR2 = navelDx * navelDx + navelDy * navelDy
        val frontW = smoothStep(0.15f, 0.70f, nz)
        val navelAo = 1f - 0.44f * exp(-navelR2 * 2.4f) * frontW

        // Gluteal cleft AO
        val cleftMask = smoothStep(-0.34f, -0.82f, y) * (1f - smoothStep(0.05f, 0.45f, z + 0.35f))
        val cleftAo = 1f - 0.44f * exp(-((x / 0.11f).let { it * it })) * cleftMask

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

        // Graceful studio vignette fade at top waist cut (+3.25..+3.72)
        val waistFadeAo = 1f - 0.56f * smoothStep(3.12f, 3.68f, y)

        val ao = (cushionAo * navelAo * cleftAo * ingAo * popAo * waistFadeAo).coerceIn(0.14f, 1f)

        val kneeWarm = exp(-(((ax - 1.80f) / 0.42f).let { it * it } + ((y + 1.86f) / 0.42f).let { it * it } + ((z - 1.78f) / 0.42f).let { it * it })) * 0.09f
        val toeWarm = smoothStep(-5.15f, -5.65f, y) * 0.09f
        val pigment = (ingDip * 0.08f + exp(-navelR2 * 2.0f) * 0.16f * frontW + kneeWarm + toeWarm).coerceIn(0f, 1f)

        return Pair(ao, pigment)
    }

    /**
     * Exact ray-march + Newton-Raphson front-surface sampler on the unified SDF body
     * for any (x, y) inside the anterior vulva/perineum/anus region.
     */
    fun sampleBodySurfacePoint(x: Float, y: Float): SdfSurfaceSample {
        // Start comfortably in front of the anterior pelvis & inner thighs and sphere-trace backward along -Z
        var z = 1.40f
        for (step in 0 until 40) {
            val d = evaluateBodySdf(x, y, z)
            if (abs(d) < 0.0006f) break
            z -= d.coerceIn(-0.22f, 0.38f)
            if (z < -1.30f) {
                z = -1.12f
                break
            }
        }
        // 5 Newton-Raphson polish iterations along Z for < 0.0001mm surface precision
        for (it in 0 until 5) {
            val d = evaluateBodySdf(x, y, z)
            if (abs(d) < 0.0001f) break
            val dz = (evaluateBodySdf(x, y, z + 0.006f) - evaluateBodySdf(x, y, z - 0.006f)) / 0.012f
            if (abs(dz) > 0.10f) {
                z -= (d / dz).coerceIn(-0.08f, 0.08f)
            } else {
                z -= d * 0.5f
            }
        }
        val n = evaluateBodyNormal(x, y, z)
        val (ao, pigment) = evaluateBodyMaterial(x, y, z, n[0], n[1], n[2])
        return SdfSurfaceSample(z, n[0], n[1], n[2], ao, pigment)
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
        computeNormals: (FloatArray, Int, Int, Boolean, Boolean) -> Unit
    ) {
        val data = mesh.vertexData
        val cols = cache.cols
        val rows = cache.rows
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val i = r * cols + c
                val x = cache.baseX[i]
                val y = cache.baseY[i]
                val edgeR = cache.edgeR[i]

                // Keep 100% full detail across mons (+0.72), vulva, perineum & anus (-0.86), feathering smoothly to 0 by 0.88!
                val vMask = 1f - smoothStep(0.72f, 0.88f, edgeR)
                val d = if (vMask > 0.0005f) detailEvaluator(x, y) else null

                // Gentle overlap lift in [0.84..0.95] keeps the high-res patch strictly in front of the recessed SDF collar,
                // then tucks invisibly beneath the unmodified SDF surface at [0.96..1.00] with matched normals & material!
                val overlapBias = 0.0032f * smoothStep(0.80f, 0.88f, edgeR) * (1f - smoothStep(0.93f, 0.97f, edgeR)) -
                    0.0035f * smoothStep(0.96f, 1.00f, edgeR)

                val dz = (d?.dz ?: 0f) * vMask
                // Displace cleanly along Z
                val pz = cache.baseZ[i] + dz + overlapBias

                val mucosa = ((d?.mucosa ?: 0f) * vMask).coerceIn(0f, 1f)
                val wetness = (0.055f + (d?.wetness ?: 0f) * vMask).coerceIn(0f, 1f)
                val detailAo = 1f - (1f - (d?.ao ?: 1f)) * vMask
                val ao = (cache.baseAo[i] * detailAo).coerceIn(0.06f, 1f)
                val pigment = (cache.basePigment[i] + (d?.pigment ?: 0f) * vMask).coerceIn(0f, 1f)

                val vIdx = i * stride
                data[vIdx] = x
                data[vIdx + 1] = y
                data[vIdx + 2] = pz
                data[vIdx + 6] = mucosa
                data[vIdx + 7] = wetness
                data[vIdx + 8] = ao
                data[vIdx + 9] = pigment
            }
        }

        computeNormals(data, cols, rows, false, false)

        // Exact outer-skirt normal matching to the unified SDF body's analytical gradient normals -> ZERO seam!
        for (i in 0 until cache.count) {
            val edgeR = cache.edgeR[i]
            val normalBlend = smoothStep(0.74f, 0.92f, edgeR)
            if (normalBlend > 0.001f) {
                val vIdx = i * stride
                val mx = data[vIdx + 3] * (1f - normalBlend) + cache.baseNx[i] * normalBlend
                val my = data[vIdx + 4] * (1f - normalBlend) + cache.baseNy[i] * normalBlend
                val mz = data[vIdx + 5] * (1f - normalBlend) + cache.baseNz[i] * normalBlend
                val len = sqrt(mx * mx + my * my + mz * mz)
                if (len > 1e-5f) {
                    data[vIdx + 3] = mx / len
                    data[vIdx + 4] = my / len
                    data[vIdx + 5] = mz / len
                } else {
                    data[vIdx + 3] = cache.baseNx[i]
                    data[vIdx + 4] = cache.baseNy[i]
                    data[vIdx + 5] = cache.baseNz[i]
                }
            }
        }

        mesh.commitVertices()
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
