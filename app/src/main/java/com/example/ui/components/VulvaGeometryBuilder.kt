package com.example.ui.components

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-resolution procedural 3D anatomical mesh generator featuring:
 *  1. Default Mode: organically contoured curved pelvic vignette (zero square borders)
 *  2. Beta Legs Mode: unified continuous lithotomy-posture 3D body sculpt (lower belly,
 *     sweeping inner/upper thighs, rounded buttocks, perineal body, and radially folded anus)
 *  3. Recessed Concave Vestibule: eliminates outward glossy bulging; frames a delicate
 *     urethral meatus papilla, vertical-oval vaginal orifice with hymenal rugae, frenulum
 *     ridges, and smooth posterior fourchette
 *  4. Single Continuous Labia Minora + Clitoral Hood Arch Manifold: merges left labium minus,
 *     continuous gothic-arch clitoral prepuce hood, and right labium minus into one unbroken
 *     living fold anchored flush to the exact underlying body surface height (zero floating
 *     hood gap, zero top seams, zero bottom poke-through slivers)
 *  5. Smooth Analytic-Normal Clitoral Glans: snugly tucked inside the continuous hood arch
 *     with its glistening pearl tip visibly peeking at rest and fully revealed as the arch
 *     gently rolls back on upward drag.
 *
 * Vertex layout (10 floats = 40 bytes):
 *   0..2 : position (x, y, z)
 *   3..5 : smooth 3D surface normal (nx, ny, nz)
 *   6    : mucosa factor (0 = cutaneous skin, 1 = moist inner mucosa)
 *   7    : wetness / gloss factor (0 = satin skin, 1 = glistening mucosa/glans)
 *   8    : ambient occlusion factor (0 = deep recessed cavity, 1 = exposed)
 *   9    : natural pigment & crease factor (0 = base skin, >0 = deeper natural rim pigment)
 */
data class VulvaDeformationState(
    val leftOuter: Float = 0f,
    val rightOuter: Float = 0f,
    val leftInner: Float = 0f,
    val rightInner: Float = 0f,
    val hoodRetract: Float = 0f,
    val leftBreastX: Float = 0f,
    val leftBreastY: Float = 0f,
    val rightBreastX: Float = 0f,
    val rightBreastY: Float = 0f,
    val breastSize: Float = 0.50f,
    val leftLegSpread: Float = 0f,
    val rightLegSpread: Float = 0f,
    val selfSpreadPose: Float = 0f,
    val headTurn: Float = 0f,
    val headPitch: Float = 0f,
    val eyeBlink: Float = 0f,
    val leftGluteX: Float = 0f,
    val leftGluteY: Float = 0f,
    val rightGluteX: Float = 0f,
    val rightGluteY: Float = 0f
)

class DynamicGlMesh(
    val vertexCount: Int,
    val indexCount: Int
) {
    val vertexData: FloatArray = FloatArray(vertexCount * FLOATS_PER_VERTEX)
    val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    val indexBuffer: ShortBuffer = ByteBuffer.allocateDirect(indexCount * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()

    fun commitVertices() {
        vertexBuffer.position(0)
        vertexBuffer.put(vertexData)
        vertexBuffer.position(0)
    }

    fun setIndices(indices: ShortArray) {
        indexBuffer.position(0)
        indexBuffer.put(indices)
        indexBuffer.position(0)
    }

    companion object {
        const val FLOATS_PER_VERTEX = 10
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
    }
}

internal data class VulvaSurfaceSample(
    val dz: Float,
    val mucosa: Float,
    val wetness: Float,
    val ao: Float,
    val pigment: Float
)

class VulvaAnatomySceneMesh {
    // 1A. Default Mode: Ultra-High-Resolution Organically Contoured Curved Pelvic Vignette (center-clustered ~0.5mm!)
    private val pelvisCols = 116
    private val pelvisRows = 132
    val pelvisMesh = DynamicGlMesh(pelvisCols * pelvisRows, (pelvisCols - 1) * (pelvisRows - 1) * 6)

    // 1B. Beta Mode: Blossom's Full Head-to-Toe Unified SDF Body + Toes + Sculpted Palms + Fingers + 20 Nails + Live Breasts + Head/Eyes/Hair + Ultra-HD Vulva Patch
    val blossomBodyMesh: DynamicGlMesh
    val blossomToesMesh: DynamicGlMesh
    val blossomPalmsMesh: DynamicGlMesh
    val blossomHandsMesh: DynamicGlMesh
    val blossomNailsMesh: DynamicGlMesh
    internal val blossomLegSkinCache: BlossomSdfBodyBuilder.LegSkinCache
    internal val blossomGluteSkinCache: BlossomSdfBodyBuilder.GluteSkinCache
    internal val blossomArmSkinCache: BlossomArmIkRig.ArmSkinCache
    internal val blossomHeadRigCache: BlossomUpperBodyBuilder.HeadRigCache
    private var lastAppliedLeftLegSpread = Float.NaN
    private var lastAppliedRightLegSpread = Float.NaN
    private var lastAppliedLeftGluteX = Float.NaN
    private var lastAppliedLeftGluteY = Float.NaN
    private var lastAppliedRightGluteX = Float.NaN
    private var lastAppliedRightGluteY = Float.NaN
    private var lastAppliedSelfSpreadPose = Float.NaN
    private var lastAppliedArmLeftOuter = Float.NaN
    private var lastAppliedArmRightOuter = Float.NaN
    private var lastAppliedHeadTurn = Float.NaN
    private var lastAppliedHeadPitch = Float.NaN
    private var lastAppliedEyeBlink = Float.NaN
    private val blossomBreastsCache: BlossomUpperBodyBuilder.BlossomBreastsCache
    val blossomBreastsMesh = DynamicGlMesh(
        BlossomUpperBodyBuilder.BREAST_COLS * BlossomUpperBodyBuilder.BREAST_ROWS * 2,
        (BlossomUpperBodyBuilder.BREAST_COLS - 1) * (BlossomUpperBodyBuilder.BREAST_ROWS - 1) * 6 * 2
    )
    val blossomHeadMesh: DynamicGlMesh
    val blossomEyesMesh: DynamicGlMesh
    val blossomHairMesh: DynamicGlMesh
    private val blossomAnteriorLut = BlossomSdfBodyBuilder.buildAnteriorSurfaceLut()
    private val blossomVulvaPatchCache = BlossomSdfBodyBuilder.buildVulvaPatchCache()
    val blossomVulvaPatchMesh = DynamicGlMesh(
        BlossomSdfBodyBuilder.VULVA_PATCH_COLS * BlossomSdfBodyBuilder.VULVA_PATCH_ROWS,
        (BlossomSdfBodyBuilder.VULVA_PATCH_COLS - 1) * (BlossomSdfBodyBuilder.VULVA_PATCH_ROWS - 1) * 6
    )
    val examCushionMesh: DynamicGlMesh

    // 2. High-Resolution Pearl 3D Clitoral Glans (exact analytic outward normals — zero apex singularity)
    private val glansCols = 36
    private val glansRows = 30
    val glansMesh = DynamicGlMesh(glansCols * glansRows, (glansCols - 1) * (glansRows - 1) * 6)

    // 3. ULTRA-HIGH-RES CONTINUOUS LABIA MINORA + CLITORAL HOOD ARCH MANIFOLD (48x216 = 10,368 vertices!)
    private val archCols = 48
    private val archRows = 216
    val labiaHoodArchMesh = DynamicGlMesh(archCols * archRows, (archCols - 1) * (archRows - 1) * 6)

    val coreMeshes: List<DynamicGlMesh> = listOf(
        pelvisMesh,
        glansMesh,
        labiaHoodArchMesh
    )

    val blossomSkinMeshes: List<DynamicGlMesh>
    val blossomEyeMeshes: List<DynamicGlMesh>
    val blossomHairMeshes: List<DynamicGlMesh>
    val blossomNailMeshes: List<DynamicGlMesh>
    val blossomDrapeMeshes: List<DynamicGlMesh>

    init {
        pelvisMesh.setIndices(buildGridIndices(pelvisCols, pelvisRows))
        blossomVulvaPatchMesh.setIndices(
            buildGridIndices(BlossomSdfBodyBuilder.VULVA_PATCH_COLS, BlossomSdfBodyBuilder.VULVA_PATCH_ROWS)
        )
        blossomBreastsMesh.setIndices(BlossomUpperBodyBuilder.buildBreastsIndices())
        glansMesh.setIndices(buildGridIndices(glansCols, glansRows))
        labiaHoodArchMesh.setIndices(buildGridIndices(archCols, archRows))

        // Build Blossom's full head-to-toe unified watertight SDF body, limbs, sculpted palms, fingers, 20 nails, breasts, head, eyes, hair & exam cushion ONCE
        blossomBodyMesh = BlossomSdfBodyBuilder.buildUnifiedBodyMesh()
        blossomToesMesh = BlossomSdfBodyBuilder.buildBlossomToesMesh()
        blossomPalmsMesh = BlossomUpperBodyBuilder.buildBlossomPalmsMesh()
        blossomHandsMesh = BlossomUpperBodyBuilder.buildBlossomHandsMesh()
        val (nailsMesh, toeNailStartVert) = BlossomUpperBodyBuilder.buildBlossomNailsMesh()
        blossomNailsMesh = nailsMesh
        blossomLegSkinCache = BlossomSdfBodyBuilder.buildLegSkinCache(
            bodyMesh = blossomBodyMesh,
            toesMesh = blossomToesMesh,
            nailsMesh = blossomNailsMesh,
            toeNailStartVert = toeNailStartVert
        )
        blossomGluteSkinCache = BlossomSdfBodyBuilder.buildGluteSkinCache(
            bodyMesh = blossomBodyMesh,
            legCache = blossomLegSkinCache
        )
        blossomArmSkinCache = BlossomArmIkRig.buildArmSkinCache(blossomBodyMesh)
        blossomBreastsCache = BlossomUpperBodyBuilder.buildBreastsCache()
        blossomHeadMesh = BlossomUpperBodyBuilder.buildBlossomHeadMesh(
            buildIndices = ::buildGridIndices,
            computeNormals = ::computeGridNormals
        )
        blossomEyesMesh = BlossomUpperBodyBuilder.buildBlossomEyesMesh()
        blossomHairMesh = BlossomUpperBodyBuilder.buildBlossomHairMesh()
        blossomHeadRigCache = BlossomUpperBodyBuilder.buildHeadRigCache(
            bodyMesh = blossomBodyMesh,
            headMesh = blossomHeadMesh,
            eyesMesh = blossomEyesMesh,
            hairMesh = blossomHairMesh
        )
        examCushionMesh = BlossomBodyBuilder.buildExamCushionMesh(
            buildIndices = ::buildGridIndices,
            computeNormals = ::computeGridNormals
        )

        blossomSkinMeshes = listOf(
            blossomBodyMesh,
            blossomToesMesh,
            blossomPalmsMesh,
            blossomHandsMesh,
            blossomBreastsMesh,
            blossomHeadMesh,
            blossomVulvaPatchMesh,
            glansMesh,
            labiaHoodArchMesh
        )
        blossomEyeMeshes = listOf(blossomEyesMesh)
        blossomHairMeshes = listOf(blossomHairMesh)
        blossomNailMeshes = listOf(blossomNailsMesh)
        blossomDrapeMeshes = listOf(examCushionMesh)

        val resting = VulvaDeformationState()
        update(resting, includeLithotomyBody = false)
        updateBlossomVulvaPatch(resting)
        updateBlossomBreasts(resting)
    }

    fun update(state: VulvaDeformationState, includeLithotomyBody: Boolean = false) {
        if (includeLithotomyBody) {
            updateBlossomVulvaPatch(state)
            updateBlossomBreasts(state)
            val legsChanged = state.leftLegSpread != lastAppliedLeftLegSpread ||
                state.rightLegSpread != lastAppliedRightLegSpread
            val glutesChanged = state.leftGluteX != lastAppliedLeftGluteX ||
                state.leftGluteY != lastAppliedLeftGluteY ||
                state.rightGluteX != lastAppliedRightGluteX ||
                state.rightGluteY != lastAppliedRightGluteY
            if (legsChanged || glutesChanged) {
                BlossomSdfBodyBuilder.applyLegSpread(
                    bodyMesh = blossomBodyMesh,
                    toesMesh = blossomToesMesh,
                    nailsMesh = blossomNailsMesh,
                    cache = blossomLegSkinCache,
                    leftLegSpread = state.leftLegSpread,
                    rightLegSpread = state.rightLegSpread,
                    includeToesAndNails = legsChanged,
                    commitBody = false
                )
                BlossomSdfBodyBuilder.applyGluteElasticity(
                    bodyMesh = blossomBodyMesh,
                    cache = blossomGluteSkinCache,
                    leftGluteX = state.leftGluteX,
                    leftGluteY = state.leftGluteY,
                    rightGluteX = state.rightGluteX,
                    rightGluteY = state.rightGluteY
                )
                lastAppliedLeftLegSpread = state.leftLegSpread
                lastAppliedRightLegSpread = state.rightLegSpread
                lastAppliedLeftGluteX = state.leftGluteX
                lastAppliedLeftGluteY = state.leftGluteY
                lastAppliedRightGluteX = state.rightGluteX
                lastAppliedRightGluteY = state.rightGluteY
            }
            val armPoseNeedsUpdate = state.selfSpreadPose != lastAppliedSelfSpreadPose ||
                (state.selfSpreadPose > 0.001f &&
                    (state.leftOuter != lastAppliedArmLeftOuter || state.rightOuter != lastAppliedArmRightOuter))
            if (armPoseNeedsUpdate) {
                BlossomArmIkRig.applyArmAndHandPose(
                    bodyMesh = blossomBodyMesh,
                    palmsMesh = blossomPalmsMesh,
                    handsMesh = blossomHandsMesh,
                    nailsMesh = blossomNailsMesh,
                    cache = blossomArmSkinCache,
                    poseProgress = state.selfSpreadPose,
                    leftOuterSpread = state.leftOuter,
                    rightOuterSpread = state.rightOuter
                )
                lastAppliedSelfSpreadPose = state.selfSpreadPose
                lastAppliedArmLeftOuter = state.leftOuter
                lastAppliedArmRightOuter = state.rightOuter
            }
            if (state.headTurn != lastAppliedHeadTurn ||
                state.headPitch != lastAppliedHeadPitch ||
                state.eyeBlink != lastAppliedEyeBlink
            ) {
                BlossomUpperBodyBuilder.applyHeadTurn(
                    bodyMesh = blossomBodyMesh,
                    headMesh = blossomHeadMesh,
                    eyesMesh = blossomEyesMesh,
                    hairMesh = blossomHairMesh,
                    cache = blossomHeadRigCache,
                    headTurn = state.headTurn,
                    headPitch = state.headPitch,
                    eyeBlink = state.eyeBlink
                )
                lastAppliedHeadTurn = state.headTurn
                lastAppliedHeadPitch = state.headPitch
                lastAppliedEyeBlink = state.eyeBlink
            }
        } else {
            updateContouredPelvis(state)
        }
        updateClitoralGlans(state, includeLithotomyBody)
        updateContinuousLabiaHoodArch(state, includeLithotomyBody)
    }

    private fun updateBlossomBreasts(s: VulvaDeformationState) {
        BlossomUpperBodyBuilder.updateBlossomBreastsMesh(
            mesh = blossomBreastsMesh,
            cache = blossomBreastsCache,
            state = s,
            computeNormals = ::computeGridNormals
        )
    }

    // -------------------------------------------------------------------------
    // SHARED EXACT CENTRAL BODY SURFACE HEIGHT (FLUSH ROOT ANCHORING!)
    // -------------------------------------------------------------------------
    private fun evaluateCentralBaseCurveZ(x: Float, y: Float, includeLithotomy: Boolean): Float {
        return if (includeLithotomy) {
            blossomAnteriorLut.sampleZ(x, y)
        } else {
            0.09f - 0.28f * x * x - 0.22f * max(0f, -y - 0.54f).let { it * it } -
                0.12f * max(0f, y - 0.72f).let { it * it }
        }
    }

    private fun sampleBodySurface(x: Float, y: Float, s: VulvaDeformationState, includeLithotomy: Boolean): Pair<Float, VulvaSurfaceSample> {
        val baseZ = evaluateCentralBaseCurveZ(x, y, includeLithotomy)
        val detail = evaluateVulvaDetail(x, y, s)
        return Pair(baseZ + detail.dz, detail)
    }

    private fun sampleBodySurfaceZ(x: Float, y: Float, s: VulvaDeformationState, includeLithotomy: Boolean): Float {
        return sampleBodySurface(x, y, s, includeLithotomy).first
    }

    // -------------------------------------------------------------------------
    // SHARED ANATOMICAL VULVA CORE (TEARDROP OUTER LABIA, CONCAVE VESTIBULE, ANUS)
    // -------------------------------------------------------------------------
    private fun evaluateVulvaDetail(x: Float, y: Float, s: VulvaDeformationState): VulvaSurfaceSample {
        val avgOuter = (s.leftOuter + s.rightOuter) * 0.5f
        val avgInner = (s.leftInner + s.rightInner) * 0.5f
        val cavityOpen = (avgInner * 0.72f + avgOuter * 0.28f).coerceIn(0f, 1f)

        // Organic teardrop longitudinal profile: tapers smoothly into mons above & perineum below
        val tArch = ((y - (-0.54f)) / 1.06f).coerceIn(0f, 1f)
        val almondCurve = sin(tArch * PI.toFloat()).let { sVal ->
            val skewed = sin(Math.pow(tArch.toDouble(), 0.85).toFloat() * PI.toFloat())
            (0.45f * sVal + 0.55f * skewed).coerceIn(0f, 1f)
        }
        val cleftSpan = smoothBell(y, -0.52f, 0.46f, 0.14f)

        val leftSpread = s.leftOuter * 0.22f + s.leftInner * 0.048f
        val rightSpread = s.rightOuter * 0.22f + s.rightInner * 0.048f

        // Teardrop outer labia majora crests framing the continuous inner arch naturally
        val restCrestX = 0.098f + 0.132f * almondCurve
        val leftCrestX = -(restCrestX + leftSpread * almondCurve)
        val rightCrestX = restCrestX + rightSpread * almondCurve

        // Curved inguinal groin creases tightly framing the outer labia majora into the upper thighs
        val groinT = ((y + 0.66f) / 1.54f).coerceIn(0f, 1f)
        val groinBaseX = 0.34f + 0.22f * groinT * groinT + 0.04f * groinT
        val leftGroinX = -(groinBaseX + leftSpread * 0.22f * cleftSpan)
        val rightGroinX = groinBaseX + rightSpread * 0.22f * cleftSpan

        var dz = 0f

        // Gentle, anatomically flush Mons Pubis pad sloping seamlessly from lower belly into anterior commissure (zero shelf!)
        val mons = 0.048f * exp(-((x / 0.42f).let { it * it } + ((y - 0.56f) / 0.25f).let { it * it }))
        dz += mons

        // Soft curved inguinal groin creases harmonizing with the unified SDF thigh-pelvis transition
        val groinWeight = smoothBell(y, -0.60f, 0.62f, 0.22f)
        val leftGroinDip = exp(-((x - leftGroinX) / 0.075f).let { it * it })
        val rightGroinDip = exp(-((x - rightGroinX) / 0.075f).let { it * it })
        dz -= (leftGroinDip + rightGroinDip) * 0.032f * groinWeight

        // Recessed Concave Vestibular Basin (tightly framed inside inner labia — eliminates muddy outer halo!)
        val vestLeftEdge = -(0.044f + s.leftInner * 0.155f + s.leftOuter * 0.058f)
        val vestRightEdge = 0.044f + s.rightInner * 0.155f + s.rightOuter * 0.058f
        val vestCenter = (vestLeftEdge + vestRightEdge) * 0.5f
        val vestHalfW = ((vestRightEdge - vestLeftEdge) * 0.5f).coerceAtLeast(0.038f)
        val normVestX = (x - vestCenter) / vestHalfW
        val normVestY = (y - (-0.04f)) / 0.41f
        val vestEllipse = normVestX * normVestX + normVestY * normVestY
        val insideVestibule = (1f - smoothStep(0.38f, 1.06f, vestEllipse)).coerceIn(0f, 1f)

        // Soft, anatomically proportioned teardrop Outer Labia Majora cushions nestled naturally between the thighs
        val fullnessCurve = (0.24f + 0.78f * almondCurve) * smoothBell(y, -0.54f, 0.52f, 0.14f)
        val leftWidth = (0.100f + 0.044f * almondCurve) + s.leftOuter * 0.022f
        val rightWidth = (0.100f + 0.044f * almondCurve) + s.rightOuter * 0.022f

        val lDist = abs((x - leftCrestX) / leftWidth)
        val rDist = abs((x - rightCrestX) / rightWidth)
        val vestibuleSuppress = 1f - insideVestibule * 0.88f
        val leftCushion = exp(-Math.pow(lDist.toDouble(), 1.92).toFloat()) *
            (0.108f - s.leftOuter * 0.012f) * fullnessCurve * vestibuleSuppress
        val rightCushion = exp(-Math.pow(rDist.toDouble(), 1.92).toFloat()) *
            (0.108f - s.rightOuter * 0.012f) * fullnessCurve * vestibuleSuppress

        // Soft anterior & posterior labial commissures framing the cleft smoothly at top and bottom
        val antCommissure = 0.022f * exp(-((x / 0.16f).let { it * it } + ((y - 0.46f) / 0.12f).let { it * it })) *
            (1f - insideVestibule * 0.75f)
        val postCommissure = 0.018f * exp(-((x / 0.14f).let { it * it } + ((y + 0.50f) / 0.09f).let { it * it })) *
            (1f - insideVestibule * 0.75f)

        // Delicate longitudinal skin folds along outer labia majora (flatten organically under spreading tension)
        val outerCreaseL = (0.0048f * sin((x - leftCrestX) * 62f + y * 5.4f) +
            0.0022f * cos((x - leftCrestX) * 112f - y * 8.2f)) *
            exp(-lDist * lDist * 1.45f) * fullnessCurve * (1f - s.leftOuter * 0.52f)
        val outerCreaseR = (0.0048f * sin((x - rightCrestX) * 62f - y * 5.4f) +
            0.0022f * cos((x - rightCrestX) * 112f + y * 8.2f)) *
            exp(-rDist * rDist * 1.45f) * fullnessCurve * (1f - s.rightOuter * 0.52f)
        dz += leftCushion + rightCushion + antCommissure + postCommissure + outerCreaseL + outerCreaseR

        // Truly Concave Vestibular Basin: smooth inward bowl with zero harsh cliff edge
        val concaveBowl = (1f - vestEllipse.coerceIn(0f, 1f))
        val cleftRecess = (0.098f + 0.032f * cavityOpen) * insideVestibule + 0.032f * concaveBowl
        dz -= cleftRecess

        // Interlabial sulcus crease gently separating inner labia arch from outer labia majora
        val sulcusL = exp(-((x - (vestLeftEdge - 0.034f)) / 0.028f).let { it * it }) * cleftSpan
        val sulcusR = exp(-((x - (vestRightEdge + 0.034f)) / 0.028f).let { it * it }) * cleftSpan
        dz -= (sulcusL + sulcusR) * 0.024f

        // Delicate bilateral Clitoral Frenulum V-folds curving down from underside of glans toward inner labia
        val frenSpan = smoothBell(y, 0.10f, 0.23f, 0.04f)
        val frenXTarget = 0.014f + (0.24f - y).coerceAtLeast(0f) * 0.34f
        val frenRidge = (exp(-((abs(x) - frenXTarget) / 0.014f).let { it * it })) * frenSpan * insideVestibule
        dz += frenRidge * 0.017f

        // Dainty, anatomically accurate Urethral Meatus papilla & tiny dimple at (0, +0.055)
        val uDx = (x - vestCenter * 0.35f) / 0.026f
        val uDy = (y - 0.055f) / 0.030f
        val uR2 = uDx * uDx + uDy * uDy
        val urethralCushion = 0.018f * exp(-uR2 * 1.6f) * insideVestibule
        val urethralDimple = 0.030f * exp(-uR2 * 16.0f) * insideVestibule
        dz += urethralCushion - urethralDimple

        // Crisp Vertical-Oval Vaginal Orifice + Scalloped Hymenal Rim + Interior Rugae
        val introY = -0.21f
        val introRx = 0.024f + cavityOpen * 0.068f
        val introRy = 0.118f + cavityOpen * 0.056f
        val iDx = (x - vestCenter * 0.45f) / introRx
        val iDy = (y - introY) / introRy
        val iR2 = iDx * iDx + iDy * iDy
        val iAngle = atan2(iDy, iDx)
        val coronaWave = 1f + 0.12f * sin(iAngle * 5f + 0.5f) + 0.06f * cos(iAngle * 9f - 0.4f)
        val introDepth = (0.135f + 0.24f * cavityOpen) * exp(-(iR2 / (coronaWave * coronaWave)) * 1.75f)
        val coronaRing = exp(-((sqrt(iR2) - 1.04f) / 0.19f).let { it * it }) *
            (0.018f + 0.010f * sin(iAngle * 6f + 0.3f)) * insideVestibule * (0.40f + 0.60f * cavityOpen)
        val rugae = if (iR2 < 0.88f) {
            0.008f * sin(y * 62f + x * 12f) * (1f - iR2) * (0.35f + 0.65f * cavityOpen)
        } else 0f
        dz += coronaRing - introDepth + rugae

        // Fossa Navicularis & Smooth Posterior Fourchette right below vaginal orifice
        val fossaDx = x / 0.065f
        val fossaDy = (y - (-0.40f)) / 0.044f
        dz -= 0.015f * exp(-(fossaDx * fossaDx + fossaDy * fossaDy)) * insideVestibule

        // Smooth Perineal Body bridging posterior fourchette (-0.48) down to lower gluteal-cleft anus (-0.86)
        val perineumBulge = 0.022f * exp(-((x / 0.20f).let { it * it } + ((y + 0.64f) / 0.15f).let { it * it }))
        val rapheGroove = 0.009f * exp(-((x / 0.024f).let { it * it })) * smoothBell(y, -0.78f, -0.50f, 0.06f)
        dz += perineumBulge - rapheGroove

        // Anatomically Sculpted Anus & Radial Perianal Skin Folds nestled lower between the buttocks at (0, -0.86)
        val anusY = -0.86f
        val aDx = x / 0.068f
        val aDy = (y - anusY) / 0.080f
        val aR2 = aDx * aDx + aDy * aDy
        val aR = sqrt(aR2)
        val aAngle = atan2(aDy, aDx)
        val anusRecess = 0.086f * exp(-aR2 * 3.8f)
        val radialFoldMask = exp(-((aR - 0.58f) / 0.48f).let { it * it }) * smoothStep(0.10f, 0.42f, aR)
        val radialFolds = (0.012f * cos(aAngle * 11f + 0.4f) + 0.007f * sin(aAngle * 7f - 0.8f)) * radialFoldMask
        val perianalRim = 0.014f * exp(-((aR - 0.72f) / 0.36f).let { it * it })
        dz += perianalRim + radialFolds - anusRecess

        // Subtle organic living-tissue asymmetry
        dz += 0.0038f * sin(y * 5.8f + x * 2.4f) + 0.0020f * cos(x * 9.0f - y * 4.2f)

        // Balanced living-mucosa & warm vascular transition across inner slopes of outer labia majora
        // Eliminates stark jump from pale skin to pink inner lips!
        val warmInnerLabiaMajora = smoothStep(0.38f, 0.08f, abs(x)) * smoothBell(y, -0.52f, 0.46f, 0.16f)
        val anusMucosaHint = exp(-aR2 * 3.2f) * 0.24f
        val mucosa = (insideVestibule * 1.05f + warmInnerLabiaMajora * 0.16f + anusMucosaHint).coerceIn(0f, 1f)
        val wetness = (insideVestibule * (0.34f + 0.28f * cavityOpen) +
            exp(-uR2 * 1.8f) * 0.28f * insideVestibule +
            exp(-iR2 * 1.05f) * 0.34f +
            exp(-aR2 * 4.0f) * 0.10f).coerceIn(0f, 0.86f)

        val closedCleftDarkening = (1f - cavityOpen * 0.72f) * insideVestibule * 0.42f
        val introAo = exp(-iR2 * 1.55f) * 0.84f
        val urethralAo = exp(-uR2 * 12.0f) * 0.44f * insideVestibule
        val anusCenterAo = exp(-aR2 * 4.4f) * 0.66f + (0.5f - 0.5f * cos(aAngle * 11f + 0.4f)) * radialFoldMask * 0.20f
        // Softer groin & sulcus occlusion eliminates any dark outer halo ring around the pelvic patch
        val groinAo = (leftGroinDip + rightGroinDip) * 0.17f * groinWeight
        val sulcusAo = (sulcusL + sulcusR) * 0.21f
        val ao = (1f - closedCleftDarkening - introAo - urethralAo - anusCenterAo - groinAo - sulcusAo).coerceIn(0.08f, 1f)

        // Slightly richer warm cutaneous pigmentation across labia majora cushions fading gently outward into thigh skin
        val majoraBodyPigment = (exp(-lDist * lDist * 0.72f) + exp(-rDist * rDist * 0.72f)) * fullnessCurve * (1f - insideVestibule * 0.65f)
        val medialMargin = (exp(-((x - leftCrestX * 0.62f) / 0.10f).let { it * it }) +
            exp(-((x - rightCrestX * 0.62f) / 0.10f).let { it * it })) * fullnessCurve
        val perianalPigment = exp(-aR2 * 1.15f) * 0.62f +
            exp(-((x / 0.14f).let { it * it } + ((y + 0.84f) / 0.18f).let { it * it })) * 0.20f
        val pigment = (majoraBodyPigment * 0.22f +
            medialMargin * 0.28f +
            (leftGroinDip + rightGroinDip) * 0.09f * groinWeight +
            perianalPigment).coerceIn(0f, 1f)

        return VulvaSurfaceSample(dz, mucosa, wetness, ao, pigment)
    }

    // -------------------------------------------------------------------------
    // 1A. DEFAULT MODE: ULTRA-HIGH-RES ORGANICALLY CONTOURED PELVIC VIGNETTE
    // -------------------------------------------------------------------------
    private fun updateContouredPelvis(s: VulvaDeformationState) {
        val data = pelvisMesh.vertexData

        for (r in 0 until pelvisRows) {
            val vRaw = (r.toFloat() / (pelvisRows - 1)) * 2f - 1f
            val v = 0.60f * vRaw + 0.40f * vRaw * vRaw * vRaw
            for (c in 0 until pelvisCols) {
                val uRaw = (c.toFloat() / (pelvisCols - 1)) * 2f - 1f
                // High vertex density clustered across the vulvar lips & vestibule (~0.5mm resolution!)
                val u = 0.46f * uRaw + 0.54f * uRaw * uRaw * uRaw

                val ex = u * sqrt((1f - 0.5f * v * v).coerceAtLeast(0.0001f))
                val ey = v * sqrt((1f - 0.5f * u * u).coerceAtLeast(0.0001f))
                val edgeR = sqrt(ex * ex + ey * ey).coerceIn(0f, 1f)

                val x = ex * 0.96f
                val y = ey * 1.14f

                val baseCurve = evaluateCentralBaseCurveZ(x, y, includeLithotomy = false)
                val innerThighLift = smoothStep(0.42f, 0.82f, abs(x)) * 0.15f * smoothBell(y, -0.68f, 0.70f, 0.34f)
                val buttockHint = 0.095f * exp(-(((abs(x) - 0.34f) / 0.28f).let { it * it } + ((y + 0.94f) / 0.26f).let { it * it }))
                val glutealCleft = 0.105f * exp(-((x / 0.075f).let { it * it })) * smoothStep(-0.82f, -1.10f, y)
                val rimRoll = -0.32f * edgeR * edgeR * edgeR * edgeR

                val detail = evaluateVulvaDetail(x, y, s)
                val z = baseCurve + innerThighLift + buttockHint - glutealCleft + rimRoll + detail.dz

                // Gentle gradual vignette falloff without a dark halo ring or hard rim
                val rimShadow = 1f - 0.28f * smoothStep(0.80f, 1.0f, edgeR)
                val cleftShadow = 1f - 0.34f * exp(-((x / 0.085f).let { it * it })) * smoothStep(-0.82f, -1.08f, y)
                val finalAo = (detail.ao * rimShadow * cleftShadow).coerceIn(0.06f, 1f)

                val idx = (r * pelvisCols + c) * DynamicGlMesh.FLOATS_PER_VERTEX
                data[idx] = x
                data[idx + 1] = y
                data[idx + 2] = z
                data[idx + 6] = detail.mucosa
                data[idx + 7] = detail.wetness
                data[idx + 8] = finalAo
                data[idx + 9] = detail.pigment
            }
        }

        computeGridNormals(data, pelvisCols, pelvisRows, flipNormals = false)
        pelvisMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // 1B. BETA MODE: ULTRA-HD FLUSH-PROJECTED ANTERIOR VULVA PATCH ON UNIFIED SDF BODY
    // -------------------------------------------------------------------------
    private fun updateBlossomVulvaPatch(s: VulvaDeformationState) {
        BlossomSdfBodyBuilder.updateBlossomVulvaPatch(
            mesh = blossomVulvaPatchMesh,
            cache = blossomVulvaPatchCache,
            detailEvaluator = { x, y -> evaluateVulvaDetail(x, y, s) },
            computeNormals = ::computeGridNormals,
            leftGluteX = s.leftGluteX,
            leftGluteY = s.leftGluteY,
            rightGluteX = s.rightGluteX,
            rightGluteY = s.rightGluteY
        )
    }

    // -------------------------------------------------------------------------
    // 2. SNUGLY TUCKED PEARL 3D CLITORAL GLANS (EXACT ANALYTIC NORMALS)
    // -------------------------------------------------------------------------
    private fun updateClitoralGlans(s: VulvaDeformationState, includeLithotomy: Boolean) {
        val data = glansMesh.vertexData
        val centerY = 0.254f
        // Anchored directly onto the exact recessed upper vestibule floor so there is ZERO gap!
        val floorZ = sampleBodySurfaceZ(0f, centerY, s, includeLithotomy)
        val baseZ = floorZ - 0.008f
        val rx = 0.034f + s.hoodRetract * 0.004f
        val ry = 0.045f + s.hoodRetract * 0.005f
        val rz = 0.042f

        for (r in 0 until glansRows) {
            val v = r.toFloat() / (glansRows - 1)
            val theta = v * (PI.toFloat() * 0.60f)
            val sinT = sin(theta)
            val cosT = cos(theta)

            for (c in 0 until glansCols) {
                val u = c.toFloat() / (glansCols - 1)
                val phi = u * (2f * PI.toFloat())
                val cosP = cos(phi)
                val sinP = sin(phi)

                val taper = 1f - 0.14f * max(0f, -sinP) * sinT
                val coronaRim = 1f + 0.042f * exp(-((v - 0.72f) / 0.15f).let { it * it })
                val x = rx * sinT * cosP * taper * coronaRim
                val y = centerY + ry * sinT * sinP * coronaRim
                val z = baseZ + rz * cosT

                // Exact smooth analytic outward ellipsoid normal (N = (0, 0, +1) at apex — zero center dot!)
                var nx = (sinT * cosP) / rx
                var ny = (sinT * sinP) / ry
                var nz = cosT / rz
                val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
                nx /= nLen
                ny /= nLen
                nz /= nLen

                // Soft shadow cast by the tight continuous hood arch above (lifts cleanly as arch rolls back!)
                val hoodArchRimY = 0.268f + s.hoodRetract * 0.095f
                val underHood = smoothStep(hoodArchRimY - 0.024f, hoodArchRimY + 0.028f, y)
                val ao = (1f - underHood * (0.58f * (1f - s.hoodRetract * 0.82f))).coerceIn(0.32f, 1f)

                val idx = (r * glansCols + c) * DynamicGlMesh.FLOATS_PER_VERTEX
                data[idx] = x
                data[idx + 1] = y
                data[idx + 2] = z
                data[idx + 3] = nx
                data[idx + 4] = ny
                data[idx + 5] = nz
                data[idx + 6] = 1.0f // pure glistening mucosa
                data[idx + 7] = 0.94f // lustrous pearl highlight
                data[idx + 8] = ao
                data[idx + 9] = 0.12f
            }
        }

        glansMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // 3. SINGLE CONTINUOUS LABIA MINORA + CLITORAL HOOD ARCH MANIFOLD
    //    s = -1.0 (left posterior fourchette) -> 0.0 (clitoral hood gothic arch)
    //    -> +1.0 (right posterior fourchette), anchored 100% flush to body Z!
    // -------------------------------------------------------------------------
    private fun updateContinuousLabiaHoodArch(s: VulvaDeformationState, includeLithotomy: Boolean) {
        val data = labiaHoodArchMesh.vertexData

        // Horseshoe centerline C(arcS) from bottom-left (-1) -> pointed gothic hood arch apex (0) -> bottom-right (+1)
        fun archCenterPoint(arcS: Float): FloatArray {
            val absS = abs(arcS)
            val signS = if (arcS < 0f) -1f else 1f
            val innerOpenSide = if (arcS < 0f) s.leftInner else s.rightInner
            val outerOpenSide = if (arcS < 0f) s.leftOuter else s.rightOuter

            val hoodMask = exp(-(arcS / 0.17f).let { it * it })
            val apexY = 0.292f + s.hoodRetract * 0.088f
            val bottomY = -0.448f

            val cosArc = cos(absS * PI.toFloat())
            val yNorm = (cosArc + 1f) * 0.5f
            val y = bottomY + yNorm * (apexY - bottomY)

            // Narrow delicate arch at top & gracefully converging ribbon tips at bottom posterior fourchette
            val sinArc = sin(absS * PI.toFloat()).coerceAtLeast(0f)
            val sideSpreadMask = smoothStep(0.08f, 0.36f, absS) * smoothStep(1.0f, 0.70f, absS)
            val baseHalfWidth = 0.036f + 0.036f * sinArc + hoodMask * s.hoodRetract * 0.016f
            val dynamicSpread = (innerOpenSide * 0.145f + outerOpenSide * 0.058f) * sideSpreadMask
            val x = signS * (sinArc * (baseHalfWidth + dynamicSpread) + (1f - sinArc) * 0.006f * smoothStep(0.75f, 1.0f, absS))

            return floatArrayOf(x, y)
        }

        val cx = FloatArray(archRows)
        val cy = FloatArray(archRows)
        val ox = FloatArray(archRows)
        val oy = FloatArray(archRows)

        for (r in 0 until archRows) {
            val arcS = (r.toFloat() / (archRows - 1)) * 2f - 1f
            val pt = archCenterPoint(arcS)
            cx[r] = pt[0]
            cy[r] = pt[1]
        }

        for (r in 0 until archRows) {
            val rPrev = max(0, r - 1)
            val rNext = min(archRows - 1, r + 1)
            var tx = cx[rNext] - cx[rPrev]
            var ty = cy[rNext] - cy[rPrev]
            val tLen = sqrt(tx * tx + ty * ty).coerceAtLeast(1e-5f)
            tx /= tLen
            ty /= tLen
            ox[r] = -ty
            oy[r] = tx
        }

        for (r in 0 until archRows) {
            val arcS = (r.toFloat() / (archRows - 1)) * 2f - 1f
            val absS = abs(arcS)
            val isLeft = arcS < 0f
            val innerOpen = if (isLeft) s.leftInner else s.rightInner
            val outerOpen = if (isLeft) s.leftOuter else s.rightOuter

            // Tighter, more delicate anatomical clitoral hood apex & subtly asymmetric posterior fourchette taper
            val hoodMask = exp(-(arcS / 0.175f).let { it * it })
            val labiaMask = 1f - hoodMask
            // Subtle organic left/right length asymmetry (left petal extends slightly lower & fuller than right)
            val fourchetteEnd = if (isLeft) 0.52f else 0.59f
            val fourchetteTaper = smoothStep(1.0f, fourchetteEnd, absS)
            val labiaMidArch = sin(absS * PI.toFloat()).coerceIn(0f, 1f) * fourchetteTaper
            // Subtly offset upper-mid labial lobe fullness between left and right sides
            val lobePeakS = if (isLeft) 0.34f else 0.40f
            val upperLobeFullness = 0.78f + 0.33f * exp(-((absS - lobePeakS) / 0.22f).let { it * it })

            val wrinkleTension = (1f - innerOpen * 0.40f).coerceIn(0.42f, 1f)
            val asymPhase = if (isLeft) 0.62f else 2.84f
            val sideHeightAsym = if (isLeft) 1.055f else 0.945f
            val sideThickPow = if (isLeft) 0.74 else 0.83

            // Non-periodic multi-harmonic irregular labial scalloping & soft ruffling (breaks up uniform parallel pleats!)
            val phaseWarp = 0.55f * sin(absS * 7.3f + asymPhase * 0.8f)
            val labiaLobes = (0.019f * sin(absS * 11.3f + asymPhase + phaseWarp) +
                0.012f * cos(absS * 21.7f - asymPhase * 1.18f - phaseWarp * 0.7f) +
                0.0068f * sin(absS * 37.4f + asymPhase * 1.52f + phaseWarp * 1.1f) * wrinkleTension +
                0.0032f * cos(absS * 61.9f - asymPhase * 0.85f) * wrinkleTension) * labiaMidArch * labiaMask

            // Narrower, delicately tapered skirt that blends invisibly into surrounding interlabial sulcus
            val outerSkirtWidth = ((0.072f + 0.038f * labiaMidArch) * (0.35f + 0.65f * fourchetteTaper)) * labiaMask +
                (0.132f - s.hoodRetract * 0.025f) * hoodMask
            val innerInsetWidth = 0.024f * labiaMidArch * labiaMask + 0.020f * hoodMask

            val hoodCrestHeight = (0.042f + 0.009f * s.hoodRetract) * hoodMask
            val labiaCrestHeight = ((0.055f + 0.020f * innerOpen) * (0.28f + 0.72f * labiaMidArch) * upperLobeFullness * sideHeightAsym +
                labiaLobes * 0.72f) * labiaMask * fourchetteTaper
            val totalHeight = (hoodCrestHeight + labiaCrestHeight).coerceAtLeast(0.001f)

            val crestOutwardShift = (0.026f + innerOpen * 0.058f + outerOpen * 0.024f) * labiaMidArch * labiaMask -
                (0.024f - s.hoodRetract * 0.036f) * hoodMask

            // Organic irregular ruffled edge flutter along the rolled labial free margin
            val crestWave = (0.0125f * sin(absS * 17.6f + asymPhase + phaseWarp * 1.15f) +
                0.0068f * cos(absS * 33.4f - asymPhase * 1.1f) +
                0.0030f * sin(absS * 58.7f + asymPhase * 1.65f)) *
                labiaMidArch * labiaMask * wrinkleTension

            for (c in 0 until archCols) {
                val u = c.toFloat() / (archCols - 1)
                val crestU = if (isLeft) 0.37f else 0.35f
                val lateralOffset: Float
                val heightFrac: Float

                if (u <= crestU) {
                    val t = u / crestU
                    val sCurve = sin(t * (PI.toFloat() * 0.5f))
                    // Subtle left/right thickness asymmetry along plump rolled medial face
                    lateralOffset = (-innerInsetWidth) * (1f - t) + (crestOutwardShift + crestWave) * sCurve
                    heightFrac = Math.pow(sCurve.coerceAtLeast(0f).toDouble(), sideThickPow).toFloat()
                } else {
                    val t = (u - crestU) / (1f - crestU)
                    val ease = t * t * (3f - 2f * t)
                    // Soft rounded lateral shoulder before landing cleanly into the interlabial sulcus
                    val rolledShoulder = (if (isLeft) 0.0122f else 0.0102f) * sin(t * PI.toFloat()) * (1f - t) * labiaMask * labiaMidArch
                    lateralOffset = (crestOutwardShift + crestWave) * (1f - ease) + outerSkirtWidth * ease + rolledShoulder
                    // Ultra-smooth C2 zero-slope tangent landing (cos^2.65): dissolves invisibly into body skin!
                    val cosT = cos(t * (PI.toFloat() * 0.5f)).coerceAtLeast(0f)
                    heightFrac = Math.pow(cosT.toDouble(), 2.65).toFloat()
                }

                // Irregular organic ruffle micro-folds along the labia minora petals (non-uniform spacing)
                val foldShift = 1.45f * sin(absS * 9.4f + asymPhase) + 0.65f * cos(absS * 16.2f - asymPhase)
                val radialFoldWave = sin(absS * 44.5f + foldShift + u * 5.2f + asymPhase)
                val fineRippleWave = cos(absS * 83.0f - foldShift * 0.7f - u * 4.6f + asymPhase * 0.7f)
                val labiaFold = (0.0052f * radialFoldWave + 0.0026f * fineRippleWave) *
                    heightFrac * labiaMidArch * labiaMask * wrinkleTension
                // Subtle dorsal prepuce ridge & soft transverse retraction folds over the clitoral hood
                val hoodDorsalRidge = 0.0085f * exp(-((arcS / 0.085f).let { it * it })) *
                    exp(-((u - 0.52f) / 0.20f).let { it * it }) * hoodMask
                val hoodRollFold = s.hoodRetract * hoodMask * (
                    0.0105f * exp(-((u - 0.52f) / 0.10f).let { it * it }) +
                        0.0058f * exp(-((u - 0.72f) / 0.095f).let { it * it })
                    )

                val px = cx[r] + ox[r] * lateralOffset
                val py = cy[r] + oy[r] * lateralOffset

                // EXACT FLUSH ANCHORING + EXACT UNDERLYING MATERIAL SAMPLING!
                val (groundZ, groundMat) = sampleBodySurface(px, py, s, includeLithotomy)

                // Micro-tuck only at extreme outer boundary AFTER normal & material already match ground 100%
                val innerTuck = -0.014f * (1f - smoothStep(0.0f, 0.16f, u))
                val outerTuck = -0.0034f * smoothStep(0.91f, 1.0f, u)
                val tipSink = -0.012f * (1f - fourchetteTaper)
                val pz = groundZ + totalHeight * heightFrac + labiaFold + hoodDorsalRidge + hoodRollFold +
                    innerTuck + outerTuck + tipSink

                // Silky anatomical mucocutaneous color gradient:
                //  - Inner medial face (u < 0.34): glistening warm rose-coral mucosa
                //  - Rolled fleshy free margin (u ~ 0.35): richer natural darker pigmentation tapering softly along the scalloped rim
                //  - Outer hood & outer skirt (u -> 0.84): 100% EXACT lerp into underlying body skin material -> ZERO border!
                val innerFaceMask = 1f - smoothStep(0.24f, 0.50f, u)
                val hoodOuterSkin = hoodMask * smoothStep(0.20f, 0.58f, u)

                val archMucosa = ((0.90f * innerFaceMask + 0.40f * (1f - innerFaceMask) * (1f - smoothStep(0.34f, 0.74f, u))) *
                    (1f - hoodOuterSkin * 0.96f)).coerceIn(0f, 1f)
                val archWetness = ((0.64f * innerFaceMask + 0.20f * heightFrac) * (1f - hoodOuterSkin * 0.92f) +
                    0.055f).coerceIn(0.055f, 0.80f)

                val foldCrevice = (0.5f - 0.5f * radialFoldWave) * heightFrac * labiaMask * wrinkleTension
                val freeEdgeRim = exp(-((u - crestU) / 0.15f).let { it * it })
                val rimOrganicMod = 0.86f + 0.14f * sin(absS * 14.2f + asymPhase)
                val archPigment = (0.12f + 0.44f * freeEdgeRim * rimOrganicMod * labiaMask +
                    0.16f * freeEdgeRim * hoodMask + foldCrevice * 0.12f)
                    .coerceIn(0f, 1f)

                val rootShadow = (1f - smoothStep(0.0f, 0.22f, u)) * 0.26f +
                    smoothStep(0.62f, 0.84f, u) * 0.06f * labiaMask
                val archAo = (0.95f - rootShadow - foldCrevice * 0.11f + 0.06f * heightFrac).coerceIn(0.50f, 1f)

                // Reach 100% exact underlying body material BEFORE u = 0.84 so outer skirt transition is invisible!
                val seamStart = 0.42f - 0.14f * hoodMask
                val seamBlend = smoothStep(seamStart, 0.84f, u)
                val tipBlend = 1f - fourchetteTaper
                val bodyLerp = max(seamBlend, tipBlend * 0.90f).coerceIn(0f, 1f)

                val finalMucosa = archMucosa * (1f - bodyLerp) + groundMat.mucosa * bodyLerp
                val finalWetness = archWetness * (1f - bodyLerp) + groundMat.wetness * bodyLerp
                val finalAo = archAo * (1f - bodyLerp) + groundMat.ao * bodyLerp
                val finalPigment = archPigment * (1f - bodyLerp) + groundMat.pigment * bodyLerp

                val idx = (r * archCols + c) * DynamicGlMesh.FLOATS_PER_VERTEX
                data[idx] = px
                data[idx + 1] = py
                data[idx + 2] = pz
                data[idx + 6] = finalMucosa
                data[idx + 7] = finalWetness
                data[idx + 8] = finalAo
                data[idx + 9] = finalPigment
            }
        }

        // Verified mathematically: O(s) x T_arch(s) = -Z, so flipNormals = true gives strictly +Z outward normals everywhere!
        computeGridNormals(data, archCols, archRows, flipNormals = true)

        // Exact Outer-Skirt & Fourchette-Tip Normal Matching: smoothly match outer boundary (u >= 0.68)
        // and converging posterior fourchette tips (absS >= 0.84) directly to the underlying body normal!
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val eps = 0.012f
        for (r in 0 until archRows) {
            val absS = abs((r.toFloat() / (archRows - 1)) * 2f - 1f)
            val tipNormalBlend = smoothStep(0.84f, 1.0f, absS)
            for (c in 0 until archCols) {
                val u = c.toFloat() / (archCols - 1)
                val normalBlend = max(smoothStep(0.68f, 0.90f, u), tipNormalBlend)
                if (normalBlend > 0.001f) {
                    val idx = (r * archCols + c) * stride
                    val px = data[idx]
                    val py = data[idx + 1]
                    val dzDx = (sampleBodySurfaceZ(px + eps, py, s, includeLithotomy) -
                        sampleBodySurfaceZ(px - eps, py, s, includeLithotomy)) / (2f * eps)
                    val dzDy = (sampleBodySurfaceZ(px, py + eps, s, includeLithotomy) -
                        sampleBodySurfaceZ(px, py - eps, s, includeLithotomy)) / (2f * eps)
                    var gx = -dzDx
                    var gy = -dzDy
                    var gz = 1f
                    val gLen = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(1e-6f)
                    gx /= gLen
                    gy /= gLen
                    gz /= gLen

                    var mx = data[idx + 3] * (1f - normalBlend) + gx * normalBlend
                    var my = data[idx + 4] * (1f - normalBlend) + gy * normalBlend
                    var mz = data[idx + 5] * (1f - normalBlend) + gz * normalBlend
                    val mLen = sqrt(mx * mx + my * my + mz * mz)
                    if (mLen > 1e-5f) {
                        data[idx + 3] = mx / mLen
                        data[idx + 4] = my / mLen
                        data[idx + 5] = mz / mLen
                    } else {
                        data[idx + 3] = gx
                        data[idx + 4] = gy
                        data[idx + 5] = gz
                    }
                }
            }
        }

        labiaHoodArchMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // GEOMETRY MATH HELPERS
    // -------------------------------------------------------------------------
    private fun computeGridNormals(
        data: FloatArray,
        cols: Int,
        rows: Int,
        flipNormals: Boolean,
        wrapU: Boolean = false
    ) {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val sign = if (flipNormals) -1f else 1f

        for (r in 0 until rows) {
            val rPrev = max(0, r - 1)
            val rNext = min(rows - 1, r + 1)
            for (c in 0 until cols) {
                val cPrev = if (wrapU && c == 0) cols - 2 else max(0, c - 1)
                val cNext = if (wrapU && c == cols - 1) 1 else min(cols - 1, c + 1)

                val iL = (r * cols + cPrev) * stride
                val iR = (r * cols + cNext) * stride
                val iD = (rPrev * cols + c) * stride
                val iU = (rNext * cols + c) * stride

                val tx = data[iR] - data[iL]
                val ty = data[iR + 1] - data[iL + 1]
                val tz = data[iR + 2] - data[iL + 2]

                val bx = data[iU] - data[iD]
                val by = data[iU + 1] - data[iD + 1]
                val bz = data[iU + 2] - data[iD + 2]

                var nx = (ty * bz - tz * by) * sign
                var ny = (tz * bx - tx * bz) * sign
                var nz = (tx * by - ty * bx) * sign

                val len = sqrt(nx * nx + ny * ny + nz * nz)
                if (len > 1e-5f) {
                    nx /= len
                    ny /= len
                    nz /= len
                } else {
                    nx = 0f
                    ny = 0f
                    nz = 1f
                }

                val idx = (r * cols + c) * stride
                data[idx + 3] = nx
                data[idx + 4] = ny
                data[idx + 5] = nz
            }
        }
    }

    private fun buildGridIndices(cols: Int, rows: Int): ShortArray {
        val out = ShortArray((cols - 1) * (rows - 1) * 6)
        var k = 0
        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val i0 = (r * cols + c).toShort()
                val i1 = (r * cols + c + 1).toShort()
                val i2 = ((r + 1) * cols + c).toShort()
                val i3 = ((r + 1) * cols + c + 1).toShort()
                out[k++] = i0
                out[k++] = i1
                out[k++] = i2
                out[k++] = i1
                out[k++] = i3
                out[k++] = i2
            }
        }
        return out
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun smoothBell(x: Float, start: Float, end: Float, feather: Float): Float {
        return smoothStep(start - feather, start + feather, x) *
            (1f - smoothStep(end - feather, end + feather, x))
    }

    companion object {
        fun computeLandmark3D(partId: String, s: VulvaDeformationState): FloatArray {
            return when (partId) {
                "ext_mons" -> floatArrayOf(0f, 0.72f, 0.28f)
                "ext_hood" -> floatArrayOf(0f, 0.34f + s.hoodRetract * 0.08f, 0.17f)
                "ext_glans" -> floatArrayOf(0f, 0.254f, 0.11f)
                "ext_labia_majora" -> floatArrayOf(-(0.21f + s.leftOuter * 0.20f), 0.02f, 0.22f)
                "ext_labia_minora" -> floatArrayOf(0.08f + s.rightInner * 0.17f + s.rightOuter * 0.05f, -0.05f, 0.13f)
                "ext_urethra" -> floatArrayOf(0f, 0.055f, -0.04f)
                "ext_introitus" -> floatArrayOf(0f, -0.21f, -0.10f)
                "ext_perineum" -> floatArrayOf(0f, -0.64f, 0.04f)
                else -> floatArrayOf(0f, 0f, 0.12f)
            }
        }
    }
}
