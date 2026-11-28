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
    val rightGluteY: Float = 0f,
    /**
     * When true, reverts all Beta v1 anatomical & visual refinements (perineum, buttocks,
     * vulva seam A–F, inner labia tone/sheen, raphe, neck seam, and strand-detail hair)
     * back to the original pre-Beta v1 look.
     */
    val legacyVulvaMode: Boolean = false,
    val betaShortPerineum: Boolean = !legacyVulvaMode,
    val betaSoftButtockTable: Boolean = !legacyVulvaMode,
    val betaSmoothVulvaSeam: Boolean = !legacyVulvaMode,
    val betaBlendVulvaSeamColor: Boolean = !legacyVulvaMode,
    val betaSoftLabiaInteriorEdge: Boolean = !legacyVulvaMode,
    val betaSmoothBodyHoleCutoff: Boolean = !legacyVulvaMode,
    val betaSoftArchEdge: Boolean = !legacyVulvaMode,
    val betaWarmTransitionZone: Boolean = !legacyVulvaMode,
    val betaRefineInnerLabia: Boolean = !legacyVulvaMode,
    val betaPerinealRaphe: Boolean = !legacyVulvaMode,
    val betaSmoothNeckSeam: Boolean = !legacyVulvaMode,
    val betaSoftHair: Boolean = !legacyVulvaMode,
    /**
     * Vulva Beta v2 refinements — now baked into the live baseline (default ON whenever
     * legacyVulvaMode is false, with optimal 30% vestibular concavity). Explicit false flags
     * remain supported for historical Version 13.0 regression tests.
     */
    val betaV2SeamOnlyPolyOffset: Boolean = !legacyVulvaMode,
    val betaV2NonInvertingLabiaFold: Boolean = !legacyVulvaMode,
    val betaV2AlignedSulcus: Boolean = !legacyVulvaMode,
    val betaV2NaturalPelvicFinish: Boolean = !legacyVulvaMode,
    val betaV2BalancedUpperVulvaDepth: Boolean = !legacyVulvaMode,
    val betaV2BalancedUpperVulvaDepthWithCrest: Boolean = !legacyVulvaMode,
    val betaV2AdjustableVestibularConcavity: Boolean = !legacyVulvaMode,
    val betaV2VestibularConcavity: Float = 0.30f,
    /**
     * Vulva Beta v3 anatomical realism pass (6 independent toggles + adjustable strength,
     * all OFF by default so the baked Beta v2 baseline stays bit-for-bit identical) plus
     * proportional Breast Size Boost slider (0%..+50% above most petite).
     */
    val betaV3OrganicContinuity: Boolean = false,
    val betaV3ContinuityStrength: Float = 0.50f,
    val betaV3SoftPigmentation: Boolean = false,
    val betaV3PigmentStrength: Float = 0.50f,
    val betaV3AnatomicalAsymmetry: Boolean = false,
    val betaV3AsymmetryStrength: Float = 0.50f,
    val betaV3RegionalSurfaceDetail: Boolean = false,
    val betaV3SurfaceStrength: Float = 0.50f,
    val betaV3LabialVestibuleBlend: Boolean = false,
    val betaV3LabialBlendStrength: Float = 0.70f,
    val betaV3IntricateLabiaFolds: Boolean = false,
    val betaV3LabiaFoldsStrength: Float = 0.70f,
    /**
     * Vulva Beta v3 Option 7 (Medial Labial Root Color Continuity): strictly localized material
     * continuity at the labia minora–vestibule root (u -> 0) to eliminate the pale cutaneous bleed
     * band without altering geometry, normals, lighting, or free-edge pigmentation. OFF by default.
     */
    val betaV3MedialRootContinuity: Boolean = false,
    val betaV3MedialRootStrength: Float = 0.75f,
    val betaBreastSizeBoost: Float = 0f,
    /**
     * Lifelike Beta (Tab 2 & Tab 3) micro-behavior parameters (all 0f / false by default in
     * VulvaDeformationState so baseline head/body geometry & shading stay bit-identical).
     */
    val eyeGazeX: Float = 0f,
    val eyeGazeY: Float = 0f,
    val headRollOffset: Float = 0f,
    val faceWarmth: Float = 0f,
    val faceBrow: Float = 0f,
    val faceLipSoft: Float = 0f,
    val betaLifeDeepBreathsSighs: Boolean = false,
    val shoulderRelaxL: Float = 0f,
    val shoulderRelaxR: Float = 0f,
    // Tab 3 (Lifelike+) live parameters (strictly 0f when disabled)
    val eyeBlinkRightOffset: Float = 0f,
    val eyelidGazeTrack: Float = 0f,
    val swallowPhase: Float = 0f,
    val nostrilBreath: Float = 0f,
    val livingWarmth: Float = 0f,
    // Peak exertion & expressive micro-kinematics (all strictly 0f at rest for bit-identical baseline)
    val leftKneeShift: Float = 0f,
    val rightKneeShift: Float = 0f,
    val leftToeWiggle: Float = 0f,
    val rightToeWiggle: Float = 0f,
    val leftArmShift: Float = 0f,
    val rightArmShift: Float = 0f,
    val leftFingerCurl: Float = 0f,
    val rightFingerCurl: Float = 0f,
    val mouthOpen: Float = 0f,
    /**
     * Vulva Beta v3 Option 8 (v28.0 Clitoral Hood–Mons Skin Blend): smoothly harmonizes the dorsal
     * and outer clitoral hood coloration & pigmentation into the natural surrounding mons pubis skin
     * while preserving the delicate glistening inner preputial edge and clitoral glans. OFF by default.
     */
    val betaV3HoodMonsBlend: Boolean = false,
    val betaV3HoodMonsStrength: Float = 0.75f,
    /**
     * Vulva Beta v3 Option 9 (v29.0 Upper Hood–Mons Color Blend): softly blends the triangular
     * rose-brown upper hood & anterior commissure patch (center + shoulders) immediately above the
     * clitoral glans and below the pubic hair into adjacent mons skin, tracking hoodRetract while
     * explicitly excluding the glans, inner preputial rim, and vestibular floor, and preserving
     * micro-relief, fold shadow AO, and wetness. OFF by default.
     */
    val betaV3HoodMonsUpperBlend: Boolean = false,
    val betaV3HoodMonsUpperStrength: Float = 0.75f,
    // v27.0–v30.0 Photorealistic Visual Realism Beta toggles (all default false so baseline stays bit-identical)
    val betaRealismLayeredHair: Boolean = false,
    val betaRealismEyesAndLashes: Boolean = false,
    val betaRealismContactShadows: Boolean = false,
    val betaRealismDynamicCreases: Boolean = false,
    val betaRealismFacialMicro: Boolean = false,
    val betaRealismSurfaceMicro: Boolean = false,
    val betaRealismHipThighSwing: Boolean = false,
    // v33.0–v35.0 Independent Vulva Beta sliders (0..1; sheen/roundness 0f default here so baseline tests stay bit-identical)
    val betaV3InnerTissueSheenStrength: Float = 0f,
    val betaV3IntroitusRoundness: Float = 0f,
    // v35.0 Peak wetness intensity multiplier (0..1; scales automatic peak viscous clear-coat & subtle vestibule trickle)
    val betaV3PeakWetnessIntensity: Float = 1.0f,
    // v33.0 Dynamic physiological arousal wetness (0..1; ramps in pre-event, peaks at 1.0, decays over ~30s)
    val arousalWetness: Float = 0f,
    // v33.0 2-bone leg IK tremor & planted-heel contact signals (0f / false at rest)
    val leftLegTremor: Float = 0f,
    val rightLegTremor: Float = 0f,
    val leftFootPlanted: Boolean = false,
    val rightFootPlanted: Boolean = false
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

    // Version counters for GPU VBO/IBO dirty uploads + world-space AABB for exact conservative frustum culling
    @Volatile
    var vertexVersion: Int = 1
        private set

    @Volatile
    var indexVersion: Int = 1
        private set

    var minX: Float = -10f
        private set
    var maxX: Float = 10f
        private set
    var minY: Float = -10f
        private set
    var maxY: Float = 10f
        private set
    var minZ: Float = -10f
        private set
    var maxZ: Float = 10f
        private set

    fun commitVertices() {
        var loX = Float.POSITIVE_INFINITY
        var hiX = Float.NEGATIVE_INFINITY
        var loY = Float.POSITIVE_INFINITY
        var hiY = Float.NEGATIVE_INFINITY
        var loZ = Float.POSITIVE_INFINITY
        var hiZ = Float.NEGATIVE_INFINITY
        val data = vertexData
        var i = 0
        val limit = vertexCount * FLOATS_PER_VERTEX
        while (i < limit) {
            val x = data[i]
            val y = data[i + 1]
            val z = data[i + 2]
            if (x < loX) loX = x
            if (x > hiX) hiX = x
            if (y < loY) loY = y
            if (y > hiY) hiY = y
            if (z < loZ) loZ = z
            if (z > hiZ) hiZ = z
            i += FLOATS_PER_VERTEX
        }
        if (loX <= hiX) {
            minX = loX; maxX = hiX
            minY = loY; maxY = hiY
            minZ = loZ; maxZ = hiZ
        }
        vertexBuffer.position(0)
        vertexBuffer.put(data)
        vertexBuffer.position(0)
        vertexVersion++
    }

    fun setIndices(indices: ShortArray) {
        indexBuffer.position(0)
        indexBuffer.put(indices)
        indexBuffer.position(0)
        indexVersion++
    }

    companion object {
        const val FLOATS_PER_VERTEX = 10
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
    }
}

internal class VulvaSurfaceSample(
    var dz: Float = 0f,
    var mucosa: Float = 0f,
    var wetness: Float = 0f,
    var ao: Float = 1f,
    var pigment: Float = 0f
)

class VulvaAnatomySceneMesh {
    // 1A. Default Mode: Ultra-High-Resolution Organically Contoured Curved Pelvic Vignette (center-clustered ~0.5mm!)
    private val pelvisCols = 116
    private val pelvisRows = 132
    val pelvisMesh = DynamicGlMesh(pelvisCols * pelvisRows, (pelvisCols - 1) * (pelvisRows - 1) * 6)

    // 1B. Beta Mode: Blossom's Full Head-to-Toe Unified SDF Body + Toes + Sculpted Palms + Fingers + 20 Nails + Live Breasts + Head/Eyes/Hair + Ultra-HD Vulva Patch
    val blossomBodyMesh: DynamicGlMesh
    // Unposed bind-pose Seam D pelvic opening coordinates per vertex of blossomBodyMesh:
    // vec4(bindX / PATCH_RX, (bindY - PATCH_CENTER_Y) / PATCH_RY, bindZ, bindNz).
    // Decoupled from leg/arm/head/glute posing so inward-squeezed thighs NEVER trigger Seam D discard.
    internal val blossomBodyBindHoleData: FloatArray
    val blossomBodyBindHoleBuffer: FloatBuffer
    var blossomBodyBindHoleVersion: Int = 1
        private set
    val blossomToesMesh: DynamicGlMesh
    val blossomPalmsMesh: DynamicGlMesh
    val blossomHandsMesh: DynamicGlMesh
    val blossomNailsMesh: DynamicGlMesh
    internal val blossomLegSkinCache: BlossomSdfBodyBuilder.LegSkinCache
    internal val blossomGluteSkinCache: BlossomSdfBodyBuilder.GluteSkinCache
    internal val blossomPelvisBetaCache: BlossomSdfBodyBuilder.PelvisBetaCache
    internal val blossomArmSkinCache: BlossomArmIkRig.ArmSkinCache
    internal val blossomHeadRigCache: BlossomUpperBodyBuilder.HeadRigCache
    internal val blossomHeadNeckHairBetaCache: BlossomUpperBodyBuilder.HeadNeckHairBetaCache
    private var lastAppliedLeftLegSpread = Float.NaN
    private var lastAppliedRightLegSpread = Float.NaN
    private var lastAppliedLeftKneeShift = Float.NaN
    private var lastAppliedRightKneeShift = Float.NaN
    private var lastAppliedLeftToeWiggle = Float.NaN
    private var lastAppliedRightToeWiggle = Float.NaN
    private var lastAppliedBetaRealismHipThighSwing: Boolean? = null
    private var lastAppliedLeftLegTremor = Float.NaN
    private var lastAppliedRightLegTremor = Float.NaN
    private var lastAppliedLeftFootPlanted: Boolean? = null
    private var lastAppliedRightFootPlanted: Boolean? = null
    internal val blossomLegHeelTracker = BlossomLegIkRig.PlantedHeelTracker()
    private var lastAppliedLeftGluteX = Float.NaN
    private var lastAppliedLeftGluteY = Float.NaN
    private var lastAppliedRightGluteX = Float.NaN
    private var lastAppliedRightGluteY = Float.NaN
    private var lastAppliedSelfSpreadPose = Float.NaN
    private var lastAppliedArmLeftOuter = Float.NaN
    private var lastAppliedArmRightOuter = Float.NaN
    private var lastAppliedLeftArmShift = Float.NaN
    private var lastAppliedRightArmShift = Float.NaN
    private var lastAppliedLeftFingerCurl = Float.NaN
    private var lastAppliedRightFingerCurl = Float.NaN
    private var lastAppliedHeadTurn = Float.NaN
    private var lastAppliedHeadPitch = Float.NaN
    private var lastAppliedEyeBlink = Float.NaN
    private var lastAppliedEyeBlinkRightOffset = Float.NaN
    private var lastAppliedEyelidGazeTrack = Float.NaN
    private var lastAppliedEyeGazeX = Float.NaN
    private var lastAppliedEyeGazeY = Float.NaN
    private var lastAppliedHeadRollOffset = Float.NaN
    private var lastAppliedFaceWarmth = Float.NaN
    private var lastAppliedFaceBrow = Float.NaN
    private var lastAppliedFaceLipSoft = Float.NaN
    private var lastAppliedSwallowPhase = Float.NaN
    private var lastAppliedNostrilBreath = Float.NaN
    private var lastAppliedMouthOpen = Float.NaN
    private var lastAppliedBreastLeftX = Float.NaN
    private var lastAppliedBreastLeftY = Float.NaN
    private var lastAppliedBreastRightX = Float.NaN
    private var lastAppliedBreastRightY = Float.NaN
    private var lastAppliedBreastSize = Float.NaN
    private var lastAppliedBreastSizeBoost = Float.NaN
    private var lastVulvaLeftOuter = Float.NaN
    private var lastVulvaRightOuter = Float.NaN
    private var lastVulvaLeftInner = Float.NaN
    private var lastVulvaRightInner = Float.NaN
    private var lastVulvaHoodRetract = Float.NaN
    private var lastVulvaGluteLX = Float.NaN
    private var lastVulvaGluteLY = Float.NaN
    private var lastVulvaGluteRX = Float.NaN
    private var lastVulvaGluteRY = Float.NaN
    private var lastVulvaLithotomy: Boolean? = null
    private var lastVulvaShortPerineum = false
    private var lastVulvaSmoothSeam = false
    private var lastVulvaBlendSeamColor = false
    private var lastVulvaSoftInterior = false
    private var lastVulvaSoftArchEdge = false
    private var lastVulvaRaphe = false
    private var lastVulvaBetaV2SeamOnlyPolyOffset = false
    private var lastVulvaBetaV2NonInvertingLabiaFold = false
    private var lastVulvaBetaV2AlignedSulcus = false
    private var lastVulvaBetaV2BalancedUpperVulvaDepth = false
    private var lastVulvaBetaV2BalancedUpperVulvaDepthWithCrest = false
    private var lastVulvaBetaV2AdjustableVestibularConcavity = false
    private var lastVulvaBetaV2VestibularConcavity = Float.NaN
    private var lastVulvaBetaV3OrganicContinuity = false
    private var lastVulvaBetaV3ContinuityStrength = Float.NaN
    private var lastVulvaBetaV3AnatomicalAsymmetry = false
    private var lastVulvaBetaV3AsymmetryStrength = Float.NaN
    private var lastVulvaBetaV3RegionalSurfaceDetail = false
    private var lastVulvaBetaV3SurfaceStrength = Float.NaN
    private var lastVulvaBetaV3LabialVestibuleBlend = false
    private var lastVulvaBetaV3LabialBlendStrength = Float.NaN
    private var lastVulvaBetaV3IntricateLabiaFolds = false
    private var lastVulvaBetaV3LabiaFoldsStrength = Float.NaN
    private var lastVulvaBetaV3MedialRootContinuity = false
    private var lastVulvaBetaV3MedialRootStrength = Float.NaN
    private var lastVulvaBetaV3HoodMonsBlend = false
    private var lastVulvaBetaV3HoodMonsStrength = Float.NaN
    private var lastVulvaBetaV3HoodMonsUpperBlend = false
    private var lastVulvaBetaV3HoodMonsUpperStrength = Float.NaN
    private var lastVulvaBetaV3IntroitusRoundness = Float.NaN
    private var lastBetaShortPerineum = false
    private var lastBetaSoftButtockTable = false
    private var lastBetaSmoothVulvaSeam = false
    private var lastBetaBlendVulvaSeamColor = false
    private var lastBetaSmoothNeckSeam = false
    private var lastBetaSoftHair = false
    private val blossomBreastsCache: BlossomUpperBodyBuilder.BlossomBreastsCache
    val blossomBreastsMesh = DynamicGlMesh(
        BlossomUpperBodyBuilder.BREAST_COLS * BlossomUpperBodyBuilder.BREAST_ROWS * 2,
        (BlossomUpperBodyBuilder.BREAST_COLS - 1) * (BlossomUpperBodyBuilder.BREAST_ROWS - 1) * 6 * 2
    )
    val blossomHeadMesh: DynamicGlMesh
    val blossomEyesMesh: DynamicGlMesh
    val blossomHairMesh: DynamicGlMesh
    val blossomHairWispsMesh: DynamicGlMesh
    val blossomEyelashesMesh: DynamicGlMesh
    private val blossomAnteriorLut = BlossomSdfBodyBuilder.buildAnteriorSurfaceLut()
    private var syncedAnteriorLut: BlossomSdfBodyBuilder.AnteriorSurfaceLut? = null
    private var syncedLutShortPerineum = false
    private var syncedLutSoftButtock = false
    private val baselineVulvaPatchCache = BlossomSdfBodyBuilder.buildVulvaPatchCache()
    private val blossomVulvaPatchCache = BlossomSdfBodyBuilder.BlossomVulvaPatchCache(
        BlossomSdfBodyBuilder.VULVA_PATCH_COLS,
        BlossomSdfBodyBuilder.VULVA_PATCH_ROWS
    ).apply { copyFrom(baselineVulvaPatchCache) }
    val blossomVulvaPatchMesh = DynamicGlMesh(
        BlossomSdfBodyBuilder.VULVA_PATCH_COLS * BlossomSdfBodyBuilder.VULVA_PATCH_ROWS,
        (BlossomSdfBodyBuilder.VULVA_PATCH_COLS - 1) * (BlossomSdfBodyBuilder.VULVA_PATCH_ROWS - 1) * 6
    )
    // Vulva Beta v2 Toggle 1: strictly disjoint static index partitions of blossomVulvaPatchMesh
    // separating interior triangles (max edgeR < 0.88, drawn with polygon offset OFF) from the
    // outer seam ring (max edgeR >= 0.88, drawn with Seam A polygon offset ON).
    val vulvaPatchInteriorIndexBuffer: ShortBuffer
    val vulvaPatchSeamIndexBuffer: ShortBuffer
    val vulvaPatchInteriorIndexCount: Int
    val vulvaPatchSeamIndexCount: Int
    val examCushionMesh: DynamicGlMesh

    // 2. High-Resolution Pearl 3D Clitoral Glans (exact analytic outward normals — zero apex singularity)
    private val glansCols = 36
    private val glansRows = 30
    val glansMesh = DynamicGlMesh(glansCols * glansRows, (glansCols - 1) * (glansRows - 1) * 6)

    // 3. ULTRA-HIGH-RES CONTINUOUS LABIA MINORA + CLITORAL HOOD ARCH MANIFOLD (48x216 = 10,368 vertices!)
    //    labiaHoodArchCcwIndexBuffer supplies true outward CCW triangle winding (used in Beta v1 mode)
    //    so gl_FrontFacing == true across the entire arch; labiaHoodArchMesh.indexBuffer preserves legacy CW winding.
    internal val archCols = 48
    internal val archRows = 216
    private val archTotalIndices = (archCols - 1) * (archRows - 1) * 6
    val labiaHoodArchMesh = DynamicGlMesh(archCols * archRows, archTotalIndices)
    private val labiaHoodArchCcwIndices: ShortArray = buildGridIndices(archCols, archRows, flipWinding = true)
    val labiaHoodArchCcwIndexBuffer: ShortBuffer = ByteBuffer
        .allocateDirect(archTotalIndices * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
    // Exact Pass-1 (opaque core) and Pass-2 (feathered ~20% outer skirt) triangle subsets of the CCW arch
    // mesh so Pass 2 skips ~75% of triangles whose fragments would all be discarded (100% bit-identical output).
    private val labiaHoodArchCoreIndices = ShortArray(archTotalIndices)
    private val labiaHoodArchSkirtIndices = ShortArray(archTotalIndices)
    val labiaHoodArchCoreIndexBuffer: ShortBuffer = ByteBuffer
        .allocateDirect(archTotalIndices * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
    val labiaHoodArchSkirtIndexBuffer: ShortBuffer = ByteBuffer
        .allocateDirect(archTotalIndices * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
    var labiaHoodArchCoreIndexCount: Int = archTotalIndices
        private set
    var labiaHoodArchSkirtIndexCount: Int = 0
        private set
    var labiaHoodArchSplitVersion: Int = 1
        private set
    internal val labiaHoodArchAlphaData = FloatArray(archCols * archRows) { 1f }
    val labiaHoodArchAlphaBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(archCols * archRows * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    var labiaHoodArchAlphaVersion: Int = 1
        private set

    // Reusable per-frame scratch storage (eliminates ~40k heap allocations per interactive drag frame)
    private val scratchDetailSample = VulvaSurfaceSample()
    private val archCx = FloatArray(archRows)
    private val archCy = FloatArray(archRows)
    private val archOx = FloatArray(archRows)
    private val archOy = FloatArray(archRows)
    private val archTheta = FloatArray(archRows)
    private val archFwd = FloatArray(archRows)
    private val archBwd = FloatArray(archRows)
    private val archSmoothTmp = FloatArray(archRows)

    val coreMeshes: List<DynamicGlMesh> = listOf(
        pelvisMesh,
        glansMesh,
        labiaHoodArchMesh
    )

    val blossomSkinMeshes: List<DynamicGlMesh>
    val blossomEyeMeshes: List<DynamicGlMesh>
    val blossomEyelashMeshes: List<DynamicGlMesh>
    val blossomHairMeshes: List<DynamicGlMesh>
    val blossomHairWispMeshes: List<DynamicGlMesh>
    val blossomNailMeshes: List<DynamicGlMesh>
    val blossomDrapeMeshes: List<DynamicGlMesh>

    init {
        pelvisMesh.setIndices(buildGridIndices(pelvisCols, pelvisRows))
        val patchIndices = buildGridIndices(
            BlossomSdfBodyBuilder.VULVA_PATCH_COLS,
            BlossomSdfBodyBuilder.VULVA_PATCH_ROWS
        )
        blossomVulvaPatchMesh.setIndices(patchIndices)
        val patchEdgeR = baselineVulvaPatchCache.edgeR
        val interiorTemp = ShortArray(patchIndices.size)
        val seamTemp = ShortArray(patchIndices.size)
        var interiorCount = 0
        var seamCount = 0
        var pt = 0
        while (pt < patchIndices.size) {
            val i0 = patchIndices[pt]
            val i1 = patchIndices[pt + 1]
            val i2 = patchIndices[pt + 2]
            val maxEdgeR = max(
                patchEdgeR[i0.toInt() and 0xFFFF],
                max(patchEdgeR[i1.toInt() and 0xFFFF], patchEdgeR[i2.toInt() and 0xFFFF])
            )
            if (maxEdgeR < VULVA_PATCH_SEAM_OFFSET_EDGE_R) {
                interiorTemp[interiorCount++] = i0
                interiorTemp[interiorCount++] = i1
                interiorTemp[interiorCount++] = i2
            } else {
                seamTemp[seamCount++] = i0
                seamTemp[seamCount++] = i1
                seamTemp[seamCount++] = i2
            }
            pt += 3
        }
        vulvaPatchInteriorIndexCount = interiorCount
        vulvaPatchSeamIndexCount = seamCount
        vulvaPatchInteriorIndexBuffer = ByteBuffer
            .allocateDirect(max(1, interiorCount) * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(interiorTemp, 0, interiorCount)
                position(0)
            }
        vulvaPatchSeamIndexBuffer = ByteBuffer
            .allocateDirect(max(1, seamCount) * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(seamTemp, 0, seamCount)
                position(0)
            }
        blossomBreastsMesh.setIndices(BlossomUpperBodyBuilder.buildBreastsIndices())
        glansMesh.setIndices(buildGridIndices(glansCols, glansRows))
        labiaHoodArchMesh.setIndices(buildGridIndices(archCols, archRows))
        labiaHoodArchCcwIndexBuffer.position(0)
        labiaHoodArchCcwIndexBuffer.put(labiaHoodArchCcwIndices)
        labiaHoodArchCcwIndexBuffer.position(0)

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
        blossomPelvisBetaCache = BlossomSdfBodyBuilder.buildPelvisBetaCache(blossomBodyMesh)
        blossomBodyBindHoleData = FloatArray(blossomBodyMesh.vertexCount * 4)
        blossomBodyBindHoleBuffer = ByteBuffer
            .allocateDirect(blossomBodyBindHoleData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        syncBodyBindHoleFromMesh(onlyAnteriorPelvisCollar = false)
        blossomArmSkinCache = BlossomArmIkRig.buildArmSkinCache(blossomBodyMesh)
        blossomBreastsCache = BlossomUpperBodyBuilder.buildBreastsCache()
        blossomHeadMesh = BlossomUpperBodyBuilder.buildBlossomHeadMesh(
            buildIndices = ::buildGridIndices,
            computeNormals = ::computeGridNormals
        )
        blossomEyesMesh = BlossomUpperBodyBuilder.buildBlossomEyesMesh()
        blossomHairMesh = BlossomUpperBodyBuilder.buildBlossomHairMesh()
        blossomHairWispsMesh = BlossomUpperBodyBuilder.buildBlossomHairWispsMesh()
        blossomEyelashesMesh = BlossomUpperBodyBuilder.buildBlossomEyelashesMesh(blossomHeadMesh)
        blossomHeadRigCache = BlossomUpperBodyBuilder.buildHeadRigCache(
            bodyMesh = blossomBodyMesh,
            headMesh = blossomHeadMesh,
            eyesMesh = blossomEyesMesh,
            hairMesh = blossomHairMesh,
            hairWispsMesh = blossomHairWispsMesh,
            eyelashesMesh = blossomEyelashesMesh
        )
        blossomHeadNeckHairBetaCache = BlossomUpperBodyBuilder.buildHeadNeckHairBetaCache(
            bodyMesh = blossomBodyMesh,
            headMesh = blossomHeadMesh,
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
        blossomEyelashMeshes = listOf(blossomEyelashesMesh)
        blossomHairMeshes = listOf(blossomHairMesh)
        blossomHairWispMeshes = listOf(blossomHairWispsMesh)
        blossomNailMeshes = listOf(blossomNailsMesh)
        blossomDrapeMeshes = listOf(examCushionMesh)

        // Initialize directly into default Full-Body Beta v1 state (standalone pelvisMesh builds lazily if toggled)
        update(VulvaDeformationState(), includeLithotomyBody = true)
    }

    private fun syncBodyBindHoleFromMesh(onlyAnteriorPelvisCollar: Boolean) {
        val bData = blossomBodyMesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val invRx = 1f / BlossomSdfBodyBuilder.PATCH_RX
        val centerY = BlossomSdfBodyBuilder.PATCH_CENTER_Y
        val invRy = 1f / BlossomSdfBodyBuilder.PATCH_RY

        if (!onlyAnteriorPelvisCollar) {
            for (v in 0 until blossomBodyMesh.vertexCount) {
                val src = v * stride
                val dst = v * 4
                blossomBodyBindHoleData[dst] = bData[src] * invRx
                blossomBodyBindHoleData[dst + 1] = (bData[src + 1] - centerY) * invRy
                blossomBodyBindHoleData[dst + 2] = bData[src + 2]
                blossomBodyBindHoleData[dst + 3] = bData[src + 5]
            }
        } else {
            val indices = blossomPelvisBetaCache.bodyVertIndices
            val isAnt = blossomPelvisBetaCache.isAnteriorCollar
            for (k in indices.indices) {
                if (!isAnt[k]) continue
                val v = indices[k]
                val src = v * stride
                val dst = v * 4
                blossomBodyBindHoleData[dst] = bData[src] * invRx
                blossomBodyBindHoleData[dst + 1] = (bData[src + 1] - centerY) * invRy
                blossomBodyBindHoleData[dst + 2] = bData[src + 2]
                blossomBodyBindHoleData[dst + 3] = bData[src + 5]
            }
        }
        blossomBodyBindHoleBuffer.position(0)
        blossomBodyBindHoleBuffer.put(blossomBodyBindHoleData)
        blossomBodyBindHoleBuffer.position(0)
        blossomBodyBindHoleVersion++
    }

    fun update(state: VulvaDeformationState, includeLithotomyBody: Boolean = false) {
        if (includeLithotomyBody) {
            val betaChanged = state.betaShortPerineum != lastBetaShortPerineum ||
                state.betaSoftButtockTable != lastBetaSoftButtockTable ||
                state.betaSmoothVulvaSeam != lastBetaSmoothVulvaSeam ||
                state.betaBlendVulvaSeamColor != lastBetaBlendVulvaSeamColor
            if (betaChanged) {
                BlossomSdfBodyBuilder.applyPelvisBetaRefinements(
                    bodyMesh = blossomBodyMesh,
                    pelvisCache = blossomPelvisBetaCache,
                    legCache = blossomLegSkinCache,
                    gluteCache = blossomGluteSkinCache,
                    patchCache = blossomVulvaPatchCache,
                    baselinePatchCache = baselineVulvaPatchCache,
                    betaShortPerineum = state.betaShortPerineum,
                    betaSoftButtockTable = state.betaSoftButtockTable,
                    betaSmoothVulvaSeam = state.betaSmoothVulvaSeam,
                    betaBlendVulvaSeamColor = state.betaBlendVulvaSeamColor
                )
                lastBetaShortPerineum = state.betaShortPerineum
                lastBetaSoftButtockTable = state.betaSoftButtockTable
                lastBetaSmoothVulvaSeam = state.betaSmoothVulvaSeam
                lastBetaBlendVulvaSeamColor = state.betaBlendVulvaSeamColor
                // Sync bind-pose Seam D coordinates strictly for the unposed anterior pelvic rim
                // (never touches moving leg/arm/head vertices; runs before applyLegSpread re-poses legs)
                syncBodyBindHoleFromMesh(onlyAnteriorPelvisCollar = true)
                // Force leg+glute re-composition if legs or glutes are currently posed
                lastAppliedLeftGluteX = Float.NaN
            }

            if (state.betaSmoothNeckSeam != lastBetaSmoothNeckSeam) {
                BlossomUpperBodyBuilder.applyNeckSeamBeta(
                    bodyMesh = blossomBodyMesh,
                    headMesh = blossomHeadMesh,
                    headRigCache = blossomHeadRigCache,
                    betaCache = blossomHeadNeckHairBetaCache,
                    enabled = state.betaSmoothNeckSeam
                )
                lastBetaSmoothNeckSeam = state.betaSmoothNeckSeam
                lastAppliedHeadTurn = Float.NaN
            }
            if (state.betaSoftHair != lastBetaSoftHair) {
                BlossomUpperBodyBuilder.applyHairBeta(
                    hairMesh = blossomHairMesh,
                    headRigCache = blossomHeadRigCache,
                    betaCache = blossomHeadNeckHairBetaCache,
                    enabled = state.betaSoftHair
                )
                lastBetaSoftHair = state.betaSoftHair
                lastAppliedHeadTurn = Float.NaN
            }

            val vulvaNeedsUpdate = betaChanged ||
                lastVulvaLithotomy != true ||
                state.leftOuter != lastVulvaLeftOuter ||
                state.rightOuter != lastVulvaRightOuter ||
                state.leftInner != lastVulvaLeftInner ||
                state.rightInner != lastVulvaRightInner ||
                state.hoodRetract != lastVulvaHoodRetract ||
                state.leftGluteX != lastVulvaGluteLX ||
                state.leftGluteY != lastVulvaGluteLY ||
                state.rightGluteX != lastVulvaGluteRX ||
                state.rightGluteY != lastVulvaGluteRY ||
                state.betaShortPerineum != lastVulvaShortPerineum ||
                state.betaSmoothVulvaSeam != lastVulvaSmoothSeam ||
                state.betaBlendVulvaSeamColor != lastVulvaBlendSeamColor ||
                state.betaSoftLabiaInteriorEdge != lastVulvaSoftInterior ||
                state.betaSoftArchEdge != lastVulvaSoftArchEdge ||
                state.betaPerinealRaphe != lastVulvaRaphe ||
                state.betaV2SeamOnlyPolyOffset != lastVulvaBetaV2SeamOnlyPolyOffset ||
                state.betaV2NonInvertingLabiaFold != lastVulvaBetaV2NonInvertingLabiaFold ||
                state.betaV2AlignedSulcus != lastVulvaBetaV2AlignedSulcus ||
                state.betaV2BalancedUpperVulvaDepth != lastVulvaBetaV2BalancedUpperVulvaDepth ||
                state.betaV2BalancedUpperVulvaDepthWithCrest != lastVulvaBetaV2BalancedUpperVulvaDepthWithCrest ||
                state.betaV2AdjustableVestibularConcavity != lastVulvaBetaV2AdjustableVestibularConcavity ||
                (state.betaV2AdjustableVestibularConcavity &&
                    state.betaV2VestibularConcavity != lastVulvaBetaV2VestibularConcavity) ||
                state.betaV3OrganicContinuity != lastVulvaBetaV3OrganicContinuity ||
                (state.betaV3OrganicContinuity &&
                    state.betaV3ContinuityStrength != lastVulvaBetaV3ContinuityStrength) ||
                state.betaV3AnatomicalAsymmetry != lastVulvaBetaV3AnatomicalAsymmetry ||
                (state.betaV3AnatomicalAsymmetry &&
                    state.betaV3AsymmetryStrength != lastVulvaBetaV3AsymmetryStrength) ||
                state.betaV3RegionalSurfaceDetail != lastVulvaBetaV3RegionalSurfaceDetail ||
                (state.betaV3RegionalSurfaceDetail &&
                    state.betaV3SurfaceStrength != lastVulvaBetaV3SurfaceStrength) ||
                state.betaV3LabialVestibuleBlend != lastVulvaBetaV3LabialVestibuleBlend ||
                (state.betaV3LabialVestibuleBlend &&
                    state.betaV3LabialBlendStrength != lastVulvaBetaV3LabialBlendStrength) ||
                state.betaV3IntricateLabiaFolds != lastVulvaBetaV3IntricateLabiaFolds ||
                (state.betaV3IntricateLabiaFolds &&
                    state.betaV3LabiaFoldsStrength != lastVulvaBetaV3LabiaFoldsStrength) ||
                state.betaV3MedialRootContinuity != lastVulvaBetaV3MedialRootContinuity ||
                (state.betaV3MedialRootContinuity &&
                    state.betaV3MedialRootStrength != lastVulvaBetaV3MedialRootStrength) ||
                state.betaV3HoodMonsBlend != lastVulvaBetaV3HoodMonsBlend ||
                (state.betaV3HoodMonsBlend &&
                    state.betaV3HoodMonsStrength != lastVulvaBetaV3HoodMonsStrength) ||
                state.betaV3HoodMonsUpperBlend != lastVulvaBetaV3HoodMonsUpperBlend ||
                (state.betaV3HoodMonsUpperBlend &&
                    state.betaV3HoodMonsUpperStrength != lastVulvaBetaV3HoodMonsUpperStrength) ||
                state.betaV3IntroitusRoundness != lastVulvaBetaV3IntroitusRoundness
            if (vulvaNeedsUpdate) {
                updateBlossomVulvaPatch(state)
                updateClitoralGlans(state, includeLithotomy = true)
                updateContinuousLabiaHoodArch(state, includeLithotomy = true)
                markVulvaClean(state, includeLithotomy = true)
            }

            val breastsChanged = state.leftBreastX != lastAppliedBreastLeftX ||
                state.leftBreastY != lastAppliedBreastLeftY ||
                state.rightBreastX != lastAppliedBreastRightX ||
                state.rightBreastY != lastAppliedBreastRightY ||
                state.breastSize != lastAppliedBreastSize ||
                state.betaBreastSizeBoost != lastAppliedBreastSizeBoost
            if (breastsChanged) {
                updateBlossomBreasts(state)
                lastAppliedBreastLeftX = state.leftBreastX
                lastAppliedBreastLeftY = state.leftBreastY
                lastAppliedBreastRightX = state.rightBreastX
                lastAppliedBreastRightY = state.rightBreastY
                lastAppliedBreastSize = state.breastSize
                lastAppliedBreastSizeBoost = state.betaBreastSizeBoost
            }
            val legsSpreadChanged = state.leftLegSpread != lastAppliedLeftLegSpread ||
                state.rightLegSpread != lastAppliedRightLegSpread
            val kneeShiftChanged = state.leftKneeShift != lastAppliedLeftKneeShift ||
                state.rightKneeShift != lastAppliedRightKneeShift
            val toesChanged = state.leftToeWiggle != lastAppliedLeftToeWiggle ||
                state.rightToeWiggle != lastAppliedRightToeWiggle
            val hipThighSwingChanged = state.betaRealismHipThighSwing != lastAppliedBetaRealismHipThighSwing
            val tremorChanged = state.leftLegTremor != lastAppliedLeftLegTremor ||
                state.rightLegTremor != lastAppliedRightLegTremor
            val footPlantedChanged = state.leftFootPlanted != lastAppliedLeftFootPlanted ||
                state.rightFootPlanted != lastAppliedRightFootPlanted
            val legsChanged = legsSpreadChanged || kneeShiftChanged || toesChanged ||
                hipThighSwingChanged || tremorChanged || footPlantedChanged
            val glutesChanged = state.leftGluteX != lastAppliedLeftGluteX ||
                state.leftGluteY != lastAppliedLeftGluteY ||
                state.rightGluteX != lastAppliedRightGluteX ||
                state.rightGluteY != lastAppliedRightGluteY
            if (legsChanged || glutesChanged) {
                val legsAtExactRest = abs(state.leftLegSpread) < 1e-4f &&
                    abs(state.rightLegSpread) < 1e-4f &&
                    abs(state.leftKneeShift) < 1e-4f &&
                    abs(state.rightKneeShift) < 1e-4f &&
                    abs(state.leftLegTremor) < 1e-4f &&
                    abs(state.rightLegTremor) < 1e-4f &&
                    !state.leftFootPlanted &&
                    !state.rightFootPlanted
                if (hipThighSwingChanged || legsAtExactRest) {
                    blossomLegHeelTracker.reset()
                }
                BlossomSdfBodyBuilder.applyLegSpread(
                    bodyMesh = blossomBodyMesh,
                    toesMesh = blossomToesMesh,
                    nailsMesh = blossomNailsMesh,
                    cache = blossomLegSkinCache,
                    leftLegSpread = state.leftLegSpread,
                    rightLegSpread = state.rightLegSpread,
                    // v33.0: Always sync toes & toenails whenever ANY leg/knee/IK/tremor/plant input changes
                    includeToesAndNails = legsChanged,
                    commitBody = false,
                    leftKneeShift = state.leftKneeShift,
                    rightKneeShift = state.rightKneeShift,
                    leftToeWiggle = state.leftToeWiggle,
                    rightToeWiggle = state.rightToeWiggle,
                    useHipAnchoredThighSwing = state.betaRealismHipThighSwing,
                    leftLegTremor = state.leftLegTremor,
                    rightLegTremor = state.rightLegTremor,
                    leftFootPlanted = state.leftFootPlanted,
                    rightFootPlanted = state.rightFootPlanted,
                    heelTracker = blossomLegHeelTracker
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
                lastAppliedLeftKneeShift = state.leftKneeShift
                lastAppliedRightKneeShift = state.rightKneeShift
                lastAppliedLeftToeWiggle = state.leftToeWiggle
                lastAppliedRightToeWiggle = state.rightToeWiggle
                lastAppliedBetaRealismHipThighSwing = state.betaRealismHipThighSwing
                lastAppliedLeftLegTremor = state.leftLegTremor
                lastAppliedRightLegTremor = state.rightLegTremor
                lastAppliedLeftFootPlanted = state.leftFootPlanted
                lastAppliedRightFootPlanted = state.rightFootPlanted
                lastAppliedLeftGluteX = state.leftGluteX
                lastAppliedLeftGluteY = state.leftGluteY
                lastAppliedRightGluteX = state.rightGluteX
                lastAppliedRightGluteY = state.rightGluteY
            }
            val armPoseNeedsUpdate = state.selfSpreadPose != lastAppliedSelfSpreadPose ||
                state.leftArmShift != lastAppliedLeftArmShift ||
                state.rightArmShift != lastAppliedRightArmShift ||
                state.leftFingerCurl != lastAppliedLeftFingerCurl ||
                state.rightFingerCurl != lastAppliedRightFingerCurl ||
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
                    rightOuterSpread = state.rightOuter,
                    leftArmMicroShift = state.leftArmShift,
                    rightArmMicroShift = state.rightArmShift,
                    leftFingerMicroCurl = state.leftFingerCurl,
                    rightFingerMicroCurl = state.rightFingerCurl
                )
                lastAppliedSelfSpreadPose = state.selfSpreadPose
                lastAppliedArmLeftOuter = state.leftOuter
                lastAppliedArmRightOuter = state.rightOuter
                lastAppliedLeftArmShift = state.leftArmShift
                lastAppliedRightArmShift = state.rightArmShift
                lastAppliedLeftFingerCurl = state.leftFingerCurl
                lastAppliedRightFingerCurl = state.rightFingerCurl
            }
            if (state.headTurn != lastAppliedHeadTurn ||
                state.headPitch != lastAppliedHeadPitch ||
                state.eyeBlink != lastAppliedEyeBlink ||
                state.eyeBlinkRightOffset != lastAppliedEyeBlinkRightOffset ||
                state.eyelidGazeTrack != lastAppliedEyelidGazeTrack ||
                state.eyeGazeX != lastAppliedEyeGazeX ||
                state.eyeGazeY != lastAppliedEyeGazeY ||
                state.headRollOffset != lastAppliedHeadRollOffset ||
                state.faceWarmth != lastAppliedFaceWarmth ||
                state.faceBrow != lastAppliedFaceBrow ||
                state.faceLipSoft != lastAppliedFaceLipSoft ||
                state.swallowPhase != lastAppliedSwallowPhase ||
                state.nostrilBreath != lastAppliedNostrilBreath ||
                state.mouthOpen != lastAppliedMouthOpen
            ) {
                BlossomUpperBodyBuilder.applyHeadTurn(
                    bodyMesh = blossomBodyMesh,
                    headMesh = blossomHeadMesh,
                    eyesMesh = blossomEyesMesh,
                    hairMesh = blossomHairMesh,
                    cache = blossomHeadRigCache,
                    headTurn = state.headTurn,
                    headPitch = state.headPitch,
                    eyeBlink = state.eyeBlink,
                    eyeGazeX = state.eyeGazeX,
                    eyeGazeY = state.eyeGazeY,
                    headRollOffset = state.headRollOffset,
                    faceWarmth = state.faceWarmth,
                    faceBrow = state.faceBrow,
                    faceLipSoft = state.faceLipSoft,
                    eyeBlinkRightOffset = state.eyeBlinkRightOffset,
                    eyelidGazeTrack = state.eyelidGazeTrack,
                    swallowPhase = state.swallowPhase,
                    nostrilBreath = state.nostrilBreath,
                    mouthOpen = state.mouthOpen,
                    hairWispsMesh = blossomHairWispsMesh,
                    eyelashesMesh = blossomEyelashesMesh
                )
                lastAppliedHeadTurn = state.headTurn
                lastAppliedHeadPitch = state.headPitch
                lastAppliedEyeBlink = state.eyeBlink
                lastAppliedEyeBlinkRightOffset = state.eyeBlinkRightOffset
                lastAppliedEyelidGazeTrack = state.eyelidGazeTrack
                lastAppliedEyeGazeX = state.eyeGazeX
                lastAppliedEyeGazeY = state.eyeGazeY
                lastAppliedHeadRollOffset = state.headRollOffset
                lastAppliedFaceWarmth = state.faceWarmth
                lastAppliedFaceBrow = state.faceBrow
                lastAppliedFaceLipSoft = state.faceLipSoft
                lastAppliedSwallowPhase = state.swallowPhase
                lastAppliedNostrilBreath = state.nostrilBreath
                lastAppliedMouthOpen = state.mouthOpen
            }
        } else {
            val pelvisVulvaNeedsUpdate = lastVulvaLithotomy != false ||
                state.leftOuter != lastVulvaLeftOuter ||
                state.rightOuter != lastVulvaRightOuter ||
                state.leftInner != lastVulvaLeftInner ||
                state.rightInner != lastVulvaRightInner ||
                state.hoodRetract != lastVulvaHoodRetract ||
                state.betaShortPerineum != lastVulvaShortPerineum ||
                state.betaSoftLabiaInteriorEdge != lastVulvaSoftInterior ||
                state.betaSoftArchEdge != lastVulvaSoftArchEdge ||
                state.betaPerinealRaphe != lastVulvaRaphe ||
                state.betaV2SeamOnlyPolyOffset != lastVulvaBetaV2SeamOnlyPolyOffset ||
                state.betaV2NonInvertingLabiaFold != lastVulvaBetaV2NonInvertingLabiaFold ||
                state.betaV2AlignedSulcus != lastVulvaBetaV2AlignedSulcus ||
                state.betaV2BalancedUpperVulvaDepth != lastVulvaBetaV2BalancedUpperVulvaDepth ||
                state.betaV2BalancedUpperVulvaDepthWithCrest != lastVulvaBetaV2BalancedUpperVulvaDepthWithCrest ||
                state.betaV2AdjustableVestibularConcavity != lastVulvaBetaV2AdjustableVestibularConcavity ||
                (state.betaV2AdjustableVestibularConcavity &&
                    state.betaV2VestibularConcavity != lastVulvaBetaV2VestibularConcavity) ||
                state.betaV3OrganicContinuity != lastVulvaBetaV3OrganicContinuity ||
                (state.betaV3OrganicContinuity &&
                    state.betaV3ContinuityStrength != lastVulvaBetaV3ContinuityStrength) ||
                state.betaV3AnatomicalAsymmetry != lastVulvaBetaV3AnatomicalAsymmetry ||
                (state.betaV3AnatomicalAsymmetry &&
                    state.betaV3AsymmetryStrength != lastVulvaBetaV3AsymmetryStrength) ||
                state.betaV3RegionalSurfaceDetail != lastVulvaBetaV3RegionalSurfaceDetail ||
                (state.betaV3RegionalSurfaceDetail &&
                    state.betaV3SurfaceStrength != lastVulvaBetaV3SurfaceStrength) ||
                state.betaV3LabialVestibuleBlend != lastVulvaBetaV3LabialVestibuleBlend ||
                (state.betaV3LabialVestibuleBlend &&
                    state.betaV3LabialBlendStrength != lastVulvaBetaV3LabialBlendStrength) ||
                state.betaV3IntricateLabiaFolds != lastVulvaBetaV3IntricateLabiaFolds ||
                (state.betaV3IntricateLabiaFolds &&
                    state.betaV3LabiaFoldsStrength != lastVulvaBetaV3LabiaFoldsStrength) ||
                state.betaV3MedialRootContinuity != lastVulvaBetaV3MedialRootContinuity ||
                (state.betaV3MedialRootContinuity &&
                    state.betaV3MedialRootStrength != lastVulvaBetaV3MedialRootStrength) ||
                state.betaV3HoodMonsBlend != lastVulvaBetaV3HoodMonsBlend ||
                (state.betaV3HoodMonsBlend &&
                    state.betaV3HoodMonsStrength != lastVulvaBetaV3HoodMonsStrength) ||
                state.betaV3HoodMonsUpperBlend != lastVulvaBetaV3HoodMonsUpperBlend ||
                (state.betaV3HoodMonsUpperBlend &&
                    state.betaV3HoodMonsUpperStrength != lastVulvaBetaV3HoodMonsUpperStrength) ||
                state.betaV3IntroitusRoundness != lastVulvaBetaV3IntroitusRoundness
            if (pelvisVulvaNeedsUpdate) {
                updateContouredPelvis(state)
                updateClitoralGlans(state, includeLithotomy = false)
                updateContinuousLabiaHoodArch(state, includeLithotomy = false)
                markVulvaClean(state, includeLithotomy = false)
            }
        }
    }

    private fun markVulvaClean(state: VulvaDeformationState, includeLithotomy: Boolean) {
        lastVulvaLithotomy = includeLithotomy
        lastVulvaLeftOuter = state.leftOuter
        lastVulvaRightOuter = state.rightOuter
        lastVulvaLeftInner = state.leftInner
        lastVulvaRightInner = state.rightInner
        lastVulvaHoodRetract = state.hoodRetract
        lastVulvaGluteLX = state.leftGluteX
        lastVulvaGluteLY = state.leftGluteY
        lastVulvaGluteRX = state.rightGluteX
        lastVulvaGluteRY = state.rightGluteY
        lastVulvaShortPerineum = state.betaShortPerineum
        lastVulvaSmoothSeam = state.betaSmoothVulvaSeam
        lastVulvaBlendSeamColor = state.betaBlendVulvaSeamColor
        lastVulvaSoftInterior = state.betaSoftLabiaInteriorEdge
        lastVulvaSoftArchEdge = state.betaSoftArchEdge
        lastVulvaRaphe = state.betaPerinealRaphe
        lastVulvaBetaV2SeamOnlyPolyOffset = state.betaV2SeamOnlyPolyOffset
        lastVulvaBetaV2NonInvertingLabiaFold = state.betaV2NonInvertingLabiaFold
        lastVulvaBetaV2AlignedSulcus = state.betaV2AlignedSulcus
        lastVulvaBetaV2BalancedUpperVulvaDepth = state.betaV2BalancedUpperVulvaDepth
        lastVulvaBetaV2BalancedUpperVulvaDepthWithCrest = state.betaV2BalancedUpperVulvaDepthWithCrest
        lastVulvaBetaV2AdjustableVestibularConcavity = state.betaV2AdjustableVestibularConcavity
        lastVulvaBetaV2VestibularConcavity = state.betaV2VestibularConcavity
        lastVulvaBetaV3OrganicContinuity = state.betaV3OrganicContinuity
        lastVulvaBetaV3ContinuityStrength = state.betaV3ContinuityStrength
        lastVulvaBetaV3AnatomicalAsymmetry = state.betaV3AnatomicalAsymmetry
        lastVulvaBetaV3AsymmetryStrength = state.betaV3AsymmetryStrength
        lastVulvaBetaV3RegionalSurfaceDetail = state.betaV3RegionalSurfaceDetail
        lastVulvaBetaV3SurfaceStrength = state.betaV3SurfaceStrength
        lastVulvaBetaV3LabialVestibuleBlend = state.betaV3LabialVestibuleBlend
        lastVulvaBetaV3LabialBlendStrength = state.betaV3LabialBlendStrength
        lastVulvaBetaV3IntricateLabiaFolds = state.betaV3IntricateLabiaFolds
        lastVulvaBetaV3LabiaFoldsStrength = state.betaV3LabiaFoldsStrength
        lastVulvaBetaV3MedialRootContinuity = state.betaV3MedialRootContinuity
        lastVulvaBetaV3MedialRootStrength = state.betaV3MedialRootStrength
        lastVulvaBetaV3HoodMonsBlend = state.betaV3HoodMonsBlend
        lastVulvaBetaV3HoodMonsStrength = state.betaV3HoodMonsStrength
        lastVulvaBetaV3HoodMonsUpperBlend = state.betaV3HoodMonsUpperBlend
        lastVulvaBetaV3HoodMonsUpperStrength = state.betaV3HoodMonsUpperStrength
        lastVulvaBetaV3IntroitusRoundness = state.betaV3IntroitusRoundness
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
    private fun resolveAnteriorLut(s: VulvaDeformationState): BlossomSdfBodyBuilder.AnteriorSurfaceLut {
        if (!s.betaV3MedialRootContinuity || (!s.betaShortPerineum && !s.betaSoftButtockTable)) {
            return blossomAnteriorLut
        }
        val cached = syncedAnteriorLut
        if (cached != null &&
            syncedLutShortPerineum == s.betaShortPerineum &&
            syncedLutSoftButtock == s.betaSoftButtockTable
        ) {
            return cached
        }
        val rebuilt = BlossomSdfBodyBuilder.buildAnteriorSurfaceLut(
            betaShortPerineum = s.betaShortPerineum,
            betaSoftButtockTable = s.betaSoftButtockTable
        )
        syncedAnteriorLut = rebuilt
        syncedLutShortPerineum = s.betaShortPerineum
        syncedLutSoftButtock = s.betaSoftButtockTable
        return rebuilt
    }

    private fun evaluateCentralBaseCurveZ(
        x: Float,
        y: Float,
        includeLithotomy: Boolean,
        lut: BlossomSdfBodyBuilder.AnteriorSurfaceLut = blossomAnteriorLut
    ): Float {
        return if (includeLithotomy) {
            lut.sampleZ(x, y)
        } else {
            0.09f - 0.28f * x * x - 0.22f * max(0f, -y - 0.54f).let { it * it } -
                0.12f * max(0f, y - 0.72f).let { it * it }
        }
    }

    private fun sampleBodySurfaceZ(x: Float, y: Float, s: VulvaDeformationState, includeLithotomy: Boolean): Float {
        val baseZ = evaluateCentralBaseCurveZ(x, y, includeLithotomy, resolveAnteriorLut(s))
        val detail = evaluateVulvaDetail(x, y, s, scratchDetailSample)
        return baseZ + detail.dz
    }

    internal fun sampleBodySurfaceZForTest(x: Float, y: Float, s: VulvaDeformationState, includeLithotomy: Boolean = true): Float {
        return sampleBodySurfaceZ(x, y, s, includeLithotomy)
    }

    internal fun evaluateVulvaDetailForTest(x: Float, y: Float, s: VulvaDeformationState): FloatArray {
        val sample = evaluateVulvaDetail(x, y, s, VulvaSurfaceSample())
        return floatArrayOf(sample.dz, sample.mucosa, sample.wetness, sample.ao, sample.pigment)
    }

    internal fun getVulvaPatchEdgeRForTest(): FloatArray = baselineVulvaPatchCache.edgeR.copyOf()

    // -------------------------------------------------------------------------
    // SHARED ANATOMICAL VULVA CORE (TEARDROP OUTER LABIA, CONCAVE VESTIBULE, ANUS)
    // -------------------------------------------------------------------------
    private fun evaluateVulvaDetail(
        x: Float,
        y: Float,
        s: VulvaDeformationState,
        out: VulvaSurfaceSample = scratchDetailSample
    ): VulvaSurfaceSample {
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

        // Beta v3 sliders 1..4 are amplified 3x (0% = off, 100% = 3.0x) for clear visual expressiveness,
        // gated cleanly to exact 0 before ovalR = 0.89 so the outer body-patch seam (edgeR >= 0.90) stays untouched.
        val v3OvalX = x / BlossomSdfBodyBuilder.PATCH_RX
        val v3OvalY = (y - BlossomSdfBodyBuilder.PATCH_CENTER_Y) / BlossomSdfBodyBuilder.PATCH_RY
        val v3OvalR = sqrt(v3OvalX * v3OvalX + v3OvalY * v3OvalY)
        val v3PatchMask = 1f - smoothStep(0.72f, 0.89f, v3OvalR)
        val contS = if (s.betaV3OrganicContinuity) s.betaV3ContinuityStrength.coerceIn(0f, 1f) * 3.0f * v3PatchMask else 0f
        val asymS = if (s.betaV3AnatomicalAsymmetry) s.betaV3AsymmetryStrength.coerceIn(0f, 1f) * 3.0f * v3PatchMask else 0f
        val surfS = if (s.betaV3RegionalSurfaceDetail) s.betaV3SurfaceStrength.coerceIn(0f, 1f) * 3.0f * v3PatchMask else 0f
        val foldS = if (s.betaV3IntricateLabiaFolds) s.betaV3LabiaFoldsStrength.coerceIn(0f, 1f) * v3PatchMask else 0f
        val medRootS = if (s.betaV3MedialRootContinuity) s.betaV3MedialRootStrength.coerceIn(0f, 1f) * v3PatchMask else 0f
        val hoodMonsS = if (s.betaV3HoodMonsBlend) s.betaV3HoodMonsStrength.coerceIn(0f, 1f) * v3PatchMask else 0f
        val hoodMonsUpperS = if (s.betaV3HoodMonsUpperBlend) s.betaV3HoodMonsUpperStrength.coerceIn(0f, 1f) * v3PatchMask else 0f

        // Teardrop outer labia majora crests framing the continuous inner arch naturally
        // Beta v3 Toggle 3 (Subtle Anatomical Asymmetry): organic left/right fullness & longitudinal peak offset
        val restCrestX = 0.098f + 0.132f * almondCurve
        val leftCrestAsym = if (asymS > 1e-5f) {
            asymS * (0.011f * almondCurve + 0.006f * sin(y * 3.8f + 0.55f) * cleftSpan)
        } else 0f
        val rightCrestAsym = if (asymS > 1e-5f) {
            asymS * (-0.008f * almondCurve + 0.005f * sin(y * 4.3f - 1.15f) * cleftSpan)
        } else 0f
        val leftCrestX = -(restCrestX + leftSpread * almondCurve + leftCrestAsym)
        val rightCrestX = restCrestX + rightSpread * almondCurve + rightCrestAsym

        // Curved inguinal groin creases tightly framing the outer labia majora into the upper thighs
        val groinT = ((y + 0.66f) / 1.54f).coerceIn(0f, 1f)
        val groinBaseX = 0.34f + 0.22f * groinT * groinT + 0.04f * groinT
        val groinAsymL = if (asymS > 1e-5f) asymS * (0.012f * sin(y * 3.1f + 0.7f) + 0.007f) * cleftSpan else 0f
        val groinAsymR = if (asymS > 1e-5f) asymS * (0.011f * cos(y * 3.5f - 0.9f) - 0.005f) * cleftSpan else 0f
        val leftGroinX = -(groinBaseX + leftSpread * 0.22f * cleftSpan + groinAsymL)
        val rightGroinX = groinBaseX + rightSpread * 0.22f * cleftSpan + groinAsymR

        var dz = 0f

        // Gentle, anatomically flush Mons Pubis pad sloping seamlessly from lower belly into anterior commissure (zero shelf!)
        val mons = 0.048f * exp(-((x / 0.42f).let { it * it } + ((y - 0.56f) / 0.25f).let { it * it }))
        dz += mons

        // Soft curved inguinal groin creases harmonizing with the unified SDF thigh-pelvis transition
        val groinWeight = smoothBell(y, -0.60f, 0.62f, 0.22f)
        val groinSigma = 0.075f + contS * 0.018f
        val leftGroinDip = exp(-((x - leftGroinX) / groinSigma).let { it * it })
        val rightGroinDip = exp(-((x - rightGroinX) / groinSigma).let { it * it })
        val groinDepth = (0.032f - contS * 0.005f).coerceAtLeast(0.012f)
        dz -= (leftGroinDip + rightGroinDip) * groinDepth * groinWeight

        // Recessed Concave Vestibular Basin (tightly framed inside inner labia — eliminates muddy outer halo!)
        // Beta v3 Toggle 6 (Intricate Labia Folds & Organic Border): breaks up the smooth elliptical
        // labial-vestibular boundary into wavy, irregular, left/right-asymmetric anatomical curves!
        val borderWaveMask = smoothBell(y, -0.44f, 0.32f, 0.12f) * foldS
        val leftBorderWave = if (borderWaveMask > 1e-5f) {
            val wWarp = 0.75f * sin(y * 9.5f + 0.6f)
            (0.0135f * sin(y * 16.4f + 0.85f + wWarp) +
                0.0085f * cos(y * 31.2f - 1.40f - wWarp * 0.6f) +
                0.0046f * sin(y * 54.0f + 2.15f)) * borderWaveMask
        } else 0f
        val rightBorderWave = if (borderWaveMask > 1e-5f) {
            val wWarp = 0.72f * cos(y * 10.3f - 1.1f)
            (0.0125f * sin(y * 18.1f + 2.55f + wWarp) +
                0.0090f * cos(y * 34.5f + 0.75f - wWarp * 0.7f) +
                0.0048f * sin(y * 58.5f - 1.35f)) * borderWaveMask
        } else 0f
        val vestLeftEdge = -(0.044f + s.leftInner * 0.155f + s.leftOuter * 0.058f + leftBorderWave).coerceAtLeast(0.028f)
        val vestRightEdge = (0.044f + s.rightInner * 0.155f + s.rightOuter * 0.058f + rightBorderWave).coerceAtLeast(0.028f)
        val vestCenter = (vestLeftEdge + vestRightEdge) * 0.5f
        val vestHalfW = ((vestRightEdge - vestLeftEdge) * 0.5f).coerceAtLeast(0.038f)
        val normVestX = (x - vestCenter) / vestHalfW
        val normVestY = (y - (-0.04f)) / 0.41f
        val vestEllipse = normVestX * normVestX + normVestY * normVestY
        val insideVestibule = (1f - smoothStep(0.38f, 1.06f, vestEllipse)).coerceIn(0f, 1f)

        // Soft, anatomically proportioned teardrop Outer Labia Majora cushions nestled naturally between the thighs
        val fullnessCurve = (0.24f + 0.78f * almondCurve) * smoothBell(y, -0.54f, 0.52f, 0.14f)
        val asymBlendW = (0.32f * asymS).coerceAtMost(0.95f)
        val fullnessL = if (asymS > 1e-5f) {
            val yL = y - 0.028f * asymS
            val tL = ((yL - (-0.54f)) / 1.06f).coerceIn(0f, 1f)
            val almL = sin(tL * PI.toFloat()).coerceIn(0f, 1f)
            fullnessCurve * (1f - asymBlendW) + (0.24f + 0.78f * almL) * smoothBell(yL, -0.54f, 0.52f, 0.14f) * asymBlendW
        } else fullnessCurve
        val fullnessR = if (asymS > 1e-5f) {
            val yR = y + 0.022f * asymS
            val tR = ((yR - (-0.54f)) / 1.06f).coerceIn(0f, 1f)
            val almR = sin(tR * PI.toFloat()).coerceIn(0f, 1f)
            fullnessCurve * (1f - asymBlendW) + (0.24f + 0.78f * almR) * smoothBell(yR, -0.54f, 0.52f, 0.14f) * asymBlendW
        } else fullnessCurve
        val leftWidth = (0.100f + 0.044f * almondCurve) + s.leftOuter * 0.022f + contS * 0.014f + asymS * 0.009f
        val rightWidth = ((0.100f + 0.044f * almondCurve) + s.rightOuter * 0.022f + contS * 0.014f - asymS * 0.006f).coerceAtLeast(0.068f)

        val lDist = abs((x - leftCrestX) / leftWidth)
        val rDist = abs((x - rightCrestX) / rightWidth)
        val softVestBridge = if (contS > 1e-5f) {
            val wideVest = (1f - smoothStep(0.22f, 1.38f, vestEllipse)).coerceIn(0f, 1f)
            val bridgeMix = (contS * 0.34f).coerceIn(0f, 0.92f)
            insideVestibule * (1f - bridgeMix) + wideVest * bridgeMix
        } else insideVestibule
        val vestibuleSuppress = 1f - softVestBridge * (0.88f - (contS * 0.08f).coerceAtMost(0.24f))
        val leftCushion = exp(-Math.pow(lDist.toDouble(), 1.92).toFloat()) *
            (0.108f - s.leftOuter * 0.012f) * (1f + asymS * 0.068f) * fullnessL * vestibuleSuppress
        val rightCushion = exp(-Math.pow(rDist.toDouble(), 1.92).toFloat()) *
            (0.108f - s.rightOuter * 0.012f) * (1f - asymS * 0.052f).coerceAtLeast(0.76f) * fullnessR * vestibuleSuppress

        // Soft anterior & posterior labial commissures framing the cleft smoothly at top and bottom
        val antCommSigmaX = 0.16f + contS * 0.055f
        val antCommSigmaY = 0.12f + contS * 0.055f
        val postCommSigmaX = 0.14f + contS * 0.045f
        val postCommSigmaY = 0.09f + contS * 0.040f
        val commVestSuppress = (0.75f - (contS * 0.12f).coerceAtMost(0.36f))
        val antCommissure = (0.022f + contS * 0.009f) * exp(-((x / antCommSigmaX).let { it * it } + ((y - (0.46f + contS * 0.014f)) / antCommSigmaY).let { it * it })) *
            (1f - softVestBridge * commVestSuppress)
        val postCommissure = (0.018f + contS * 0.007f) * exp(-((x / postCommSigmaX).let { it * it } + ((y + (0.50f + contS * 0.010f)) / postCommSigmaY).let { it * it })) *
            (1f - softVestBridge * commVestSuppress)

        // Delicate longitudinal skin folds along outer labia majora (flatten organically under spreading tension)
        val asymPhaseL = asymS * 1.15f
        val asymPhaseR = asymS * 2.42f
        val outerCreaseL = (0.0048f * sin((x - leftCrestX) * 62f + y * 5.4f + asymPhaseL) +
            0.0022f * cos((x - leftCrestX) * 112f - y * 8.2f - asymPhaseL * 0.7f)) *
            exp(-lDist * lDist * 1.45f) * fullnessL * (1f - s.leftOuter * 0.52f)
        val outerCreaseR = (0.0048f * sin((x - rightCrestX) * 62f - y * 5.4f + asymPhaseR) +
            0.0022f * cos((x - rightCrestX) * 112f + y * 8.2f + asymPhaseR * 0.6f)) *
            exp(-rDist * rDist * 1.45f) * fullnessR * (1f - s.rightOuter * 0.52f)
        // Beta v3 Toggle 4 (Regional Curvature & Material Differentiation): multi-scale cutaneous
        // pillowing on labia majora + soft mons-to-commissure roll (3x scaled; zero change when surfS == 0f)
        val majoraRegionalRelief = if (surfS > 1e-5f) {
            val lSubtleRoll = (0.0042f * sin(y * 14.5f + (x - leftCrestX) * 28f + 0.6f) +
                0.0026f * cos(y * 27.0f - (x - leftCrestX) * 44f)) *
                exp(-lDist * lDist * 1.20f) * fullnessL * (1f - s.leftOuter * 0.45f)
            val rSubtleRoll = (0.0042f * sin(y * 15.8f - (x - rightCrestX) * 26f + 2.1f) +
                0.0026f * cos(y * 29.0f + (x - rightCrestX) * 42f)) *
                exp(-rDist * rDist * 1.20f) * fullnessR * (1f - s.rightOuter * 0.45f)
            val monsRoll = 0.0065f * exp(-((x / 0.28f).let { it * it } + ((y - 0.44f) / 0.16f).let { it * it })) *
                (1f - insideVestibule * 0.85f)
            (lSubtleRoll + rSubtleRoll + monsRoll) * surfS
        } else 0f
        dz += leftCushion + rightCushion + antCommissure + postCommissure + outerCreaseL + outerCreaseR + majoraRegionalRelief

        // Truly Concave Vestibular Basin: smooth inward bowl with zero harsh cliff edge.
        // Vulva Beta v2 Toggle 5 (Step 1 only) & Toggle 6 (Step 1 + Step 2): smoothly tapers
        // superior vestibular floor depth above the urethral meatus (y > +0.08f).
        // Vulva Beta v2 Toggle 7 (betaV2AdjustableVestibularConcavity): reduces uniform vestibular
        // basin concavity & softens the steep perimeter wall transition while adding delicate
        // anatomical soft-tissue contouring — WITHOUT reducing vaginal canal (introitus) depth!
        val concaveBowl = (1f - vestEllipse.coerceIn(0f, 1f))
        val baseCleftRecess = (0.098f + 0.032f * cavityOpen) * insideVestibule + 0.032f * concaveBowl
        val basinRecess: Float
        val softVestContour: Float
        if (s.betaV2AdjustableVestibularConcavity) {
            val c = s.betaV2VestibularConcavity.coerceIn(0f, 1f)
            val reduce = 1f - c
            if (reduce > 1e-6f) {
                val softVestWall = (1f - smoothStep(0.16f, 1.36f, vestEllipse)).coerceIn(0f, 1f)
                val rimWall = insideVestibule * (1f - 0.68f * reduce) + softVestWall * (0.68f * reduce)
                val depthScale = 0.22f + 0.78f * c
                basinRecess = ((0.098f + 0.032f * cavityOpen) * rimWall + 0.032f * concaveBowl * c) * depthScale
                val periUrethralPillow = exp(
                    -(((x - vestCenter * 0.35f) / 0.062f).let { it * it } +
                        ((y - 0.04f) / 0.135f).let { it * it })
                )
                val lateralSoftPad = exp(
                    -(((abs(x - vestCenter) - vestHalfW * 0.56f) / (vestHalfW * 0.36f).coerceAtLeast(0.025f)).let { it * it })
                ) * smoothBell(y, -0.34f, 0.22f, 0.09f)
                softVestContour = (0.010f * periUrethralPillow + 0.007f * lateralSoftPad) * reduce * insideVestibule
            } else {
                basinRecess = baseCleftRecess
                softVestContour = 0f
            }
        } else {
            basinRecess = baseCleftRecess
            softVestContour = 0f
        }
        val useUpperFloorTaper = s.betaV2BalancedUpperVulvaDepth || s.betaV2BalancedUpperVulvaDepthWithCrest
        val upperTaperMul = if (useUpperFloorTaper) 1f - 0.56f * smoothStep(0.08f, 0.34f, y) else 1f
        val cleftRecess = basinRecess * upperTaperMul
        val savedBasinDepthForIntroitus = ((baseCleftRecess * upperTaperMul) - cleftRecess).coerceAtLeast(0f)
        dz -= cleftRecess

        // Interlabial sulcus crease gently separating inner labia arch from outer labia majora
        // Vulva Beta v2 Toggle 3 (betaV2AlignedSulcus): genuinely independent sulcus alignment sitting
        // cleanly between the inner labia root and outer labia majora crest with a softer, broader profile.
        val sulcusLeftCenter: Float
        val sulcusRightCenter: Float
        val sulcusSigma: Float
        val sulcusDepthGain: Float
        val sulcusAoGain: Float
        if (s.betaV2AlignedSulcus) {
            val vestYScale = 0.30f + 0.70f * almondCurve
            sulcusLeftCenter = (vestLeftEdge * vestYScale) * 0.36f + leftCrestX * 0.64f
            sulcusRightCenter = (vestRightEdge * vestYScale) * 0.36f + rightCrestX * 0.64f
            sulcusSigma = 0.042f + contS * 0.013f
            sulcusDepthGain = 0.017f * (1f - (contS * 0.24f).coerceAtMost(0.75f))
            sulcusAoGain = 0.14f * (1f - (contS * 0.18f).coerceAtMost(0.65f))
        } else {
            sulcusLeftCenter = vestLeftEdge - 0.034f
            sulcusRightCenter = vestRightEdge + 0.034f
            sulcusSigma = 0.028f + contS * 0.010f
            sulcusDepthGain = 0.024f * (1f - (contS * 0.22f).coerceAtMost(0.72f))
            sulcusAoGain = 0.21f * (1f - (contS * 0.16f).coerceAtMost(0.62f))
        }
        val sulcusEndTaper = if (contS > 1e-5f) {
            val midTaper = 0.42f + 0.58f * smoothBell(y, -0.38f, 0.30f, 0.14f)
            1f - (contS * 0.34f).coerceAtMost(0.95f) * (1f - midTaper)
        } else 1f
        val sulcusL = exp(-((x - sulcusLeftCenter) / sulcusSigma).let { it * it }) * cleftSpan * sulcusEndTaper
        val sulcusR = exp(-((x - sulcusRightCenter) / sulcusSigma).let { it * it }) * cleftSpan * sulcusEndTaper
        dz -= (sulcusL + sulcusR) * sulcusDepthGain

        // Delicate bilateral Clitoral Frenulum V-folds curving down from underside of glans toward inner labia
        val frenSpan = smoothBell(y, 0.10f, 0.23f, 0.04f)
        val frenXTarget = 0.014f + (0.24f - y).coerceAtLeast(0f) * 0.34f
        val frenAsymX = if (asymS > 1e-5f) (if (x < 0f) 0.0025f else -0.0020f) * asymS else 0f
        val frenRidge = (exp(-((abs(x) - (frenXTarget + frenAsymX)) / 0.014f).let { it * it })) * frenSpan * insideVestibule
        dz += frenRidge * (0.017f + surfS * 0.0035f)

        // Dainty, anatomically accurate Urethral Meatus papilla & tiny dimple at (0, +0.055)
        val uDx = (x - vestCenter * 0.35f) / 0.026f
        val uDy = (y - 0.055f) / 0.030f
        val uR2 = uDx * uDx + uDy * uDy
        val urethralCushion = 0.018f * exp(-uR2 * 1.6f) * insideVestibule
        val urethralDimple = 0.030f * exp(-uR2 * 16.0f) * insideVestibule
        dz += urethralCushion - urethralDimple

        // Crisp Vaginal Orifice + Scalloped Hymenal Rim + Interior Rugae
        // v34.0 Independent Vulva Beta Slider ("Rounder vaginal opening", 0..100%, default 0%):
        // Smoothly reshapes the vertical-oval slit toward a natural circular opening inside the
        // exposed vestibule (clamped safely to vestHalfW * 0.78f so it never clips inner labia or
        // touches the urethral meatus at y=+0.055; 0% is bit-identical to baseline).
        val introY = -0.21f
        val baseIntroRx = 0.024f + cavityOpen * 0.068f
        val baseIntroRy = 0.118f + cavityOpen * 0.056f
        val roundS = s.betaV3IntroitusRoundness.coerceIn(0f, 1f) * (1f - smoothStep(0.14f, 0.22f, y))
        val introRx: Float
        val introRy: Float
        if (roundS > 1e-5f) {
            val geoR = sqrt(baseIntroRx * baseIntroRy) * 1.03f
            val maxSafeRx = (vestHalfW * 0.78f).coerceAtLeast(baseIntroRx)
            val targetRx = min(geoR, maxSafeRx)
            val targetRy = max(targetRx * 1.05f, min(baseIntroRy, max(geoR * 1.06f, 0.062f)))
            introRx = baseIntroRx + (targetRx - baseIntroRx) * roundS
            introRy = baseIntroRy + (targetRy - baseIntroRy) * roundS
        } else {
            introRx = baseIntroRx
            introRy = baseIntroRy
        }
        val iDx = (x - vestCenter * 0.45f) / introRx
        val iDy = (y - introY) / introRy
        val iR2 = iDx * iDx + iDy * iDy
        val iAngle = atan2(iDy, iDx)
        val coronaWave = 1f + 0.12f * sin(iAngle * 5f + 0.5f) + 0.06f * cos(iAngle * 9f - 0.4f)
        val introEnvelope = exp(-(iR2 / (coronaWave * coronaWave)) * 1.75f)
        // Compensate vaginal canal depth by savedBasinDepthForIntroitus so reducing vestibular concavity
        // never shallows the vaginal orifice itself!
        val introDepth = ((0.135f + 0.24f * cavityOpen) + savedBasinDepthForIntroitus) * introEnvelope
        val iR = sqrt(iR2)
        val coronaRing = exp(-((iR - 1.04f) / 0.19f).let { it * it }) *
            (0.018f + 0.010f * sin(iAngle * 6f + 0.3f)) * insideVestibule * (0.40f + 0.60f * cavityOpen)
        val rugae = if (iR2 < 0.88f) {
            0.008f * sin(y * 62f + x * 12f) * (1f - iR2) * (0.35f + 0.65f * cavityOpen)
        } else 0f
        // Beta v3 Toggle 4 (3x scaled): Hart's-line contour nuance & vestibular micro-fold pillowing
        val vestRegionalNuance = if (surfS > 1e-5f) {
            val hartsRing = exp(-((sqrt(vestEllipse) - 0.78f) / 0.20f).let { it * it }) *
                smoothBell(y, -0.40f, 0.18f, 0.10f) * 0.0045f
            val vestMicroFold = (0.0022f * sin(y * 38f + x * 18f) + 0.0014f * cos(x * 46f - y * 24f)) *
                insideVestibule * (1f - introEnvelope) * (0.45f + 0.55f * cavityOpen)
            (hartsRing + vestMicroFold) * surfS
        } else 0f
        // Beta v3 Toggle 6: subtle organic scalloped folds where inner labia base meets the vestibule rim
        val vestBorderFolds = if (foldS > 1e-5f) {
            val rimBand = exp(-((sqrt(vestEllipse) - 0.86f) / 0.24f).let { it * it }) *
                smoothBell(y, -0.42f, 0.28f, 0.10f) * (1f - introEnvelope)
            val asymX = x * 1.13f + (if (x < 0f) 0.45f else -0.62f)
            val organicPlicae = 0.0052f * sin(y * 34f + asymX * 26f + 1.1f * sin(y * 13f)) +
                0.0031f * cos(y * 61f - asymX * 38f + 0.7f)
            organicPlicae * rimBand * foldS
        } else 0f
        dz += coronaRing - introDepth + rugae +
            (softVestContour + vestRegionalNuance + vestBorderFolds) * (1f - introEnvelope)

        // Fossa Navicularis & Smooth Posterior Fourchette right below vaginal orifice
        val fossaDx = x / 0.065f
        val fossaDy = (y - (-0.40f)) / 0.044f
        dz -= 0.015f * exp(-(fossaDx * fossaDx + fossaDy * fossaDy)) * insideVestibule

        // Smooth Perineal Body bridging posterior fourchette (-0.48) down to lower gluteal-cleft anus (-0.86, or -0.782 in Beta 1: ~20% shorter)
        // Smoothly gated below -0.51f so the entire vulva, fourchette & labia/hood arch remain 100% untouched!
        val anusShiftY = if (s.betaShortPerineum) 0.078f * smoothStep(-0.51f, -0.60f, y) else 0f
        val perineumCenterY = -0.64f + anusShiftY * 0.5f
        val perineumRy = 0.15f - anusShiftY * 0.30f
        val perineumBulge = 0.022f * exp(-((x / 0.20f).let { it * it } + ((y - perineumCenterY) / perineumRy).let { it * it }))
        val rapheGroove = 0.009f * exp(-((x / 0.024f).let { it * it })) * smoothBell(y, -0.78f + anusShiftY, -0.50f, 0.06f)
        // Beta 4 (Faint Perineal Raphe): thin, soft midline ridge strictly gated below -0.51f so vulva stays 100% untouched
        val rapheGate = if (s.betaPerinealRaphe) smoothStep(-0.51f, -0.56f, y) else 0f
        val rapheSpan = if (rapheGate > 0.001f) {
            smoothBell(y, -0.77f + anusShiftY, -0.525f, 0.042f) * rapheGate
        } else 0f
        val rapheDelta = if (rapheGate > 0.001f) {
            val ridge = 0.0076f * exp(-((x / 0.015f).let { it * it })) * rapheSpan
            val flank = 0.0021f * exp(-(((abs(x) - 0.024f) / 0.012f).let { it * it })) * rapheSpan
            (rapheGroove * rapheGate) + ridge - flank
        } else 0f
        dz += perineumBulge - rapheGroove + rapheDelta

        // Anatomically Sculpted Anus & Radial Perianal Skin Folds nestled lower between the buttocks
        val anusY = -0.86f + anusShiftY
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
        // Seam C (betaSoftLabiaInteriorEdge): widens outer-edge color/mucosa/AO/wetness falloff ~2x
        // while keeping interior colors, shape & labia geometry (dz) 100% identical.
        val vestibuleColorMask = if (s.betaSoftLabiaInteriorEdge) {
            (1f - smoothStep(SEAM_C_VEST_ELLIPSE_START, SEAM_C_VEST_ELLIPSE_END, vestEllipse)).coerceIn(0f, 1f)
        } else {
            insideVestibule
        }
        // Beta v3 Option 7 (v22.0): align the underlying vulva-patch vestibular mucosal footprint
        // directly to labiaHoodArchMesh's actual attachment centerline (including spread, asymmetry &
        // organic meander) so the exposed patch medial to and beneath the inner labia root is always
        // continuous wet vestibule mucosa rather than pale cutaneous body skin (0 dz change, 0 outer majora bleed).
        val effectiveVestibuleColorMask: Float
        if (medRootS > 1e-5f) {
            val apexY = 0.292f + s.hoodRetract * 0.088f
            val bottomY = -0.448f
            val ySpan = apexY - bottomY
            val yNorm = ((y - bottomY) / ySpan).coerceIn(0f, 1f)
            val cosArc = (2f * yNorm - 1f).coerceIn(-1f, 1f)
            val absS = kotlin.math.acos(cosArc.toDouble()).toFloat() / PI.toFloat()
            val sinArc = sqrt((1f - cosArc * cosArc).coerceAtLeast(0f))
            val isLeftSide = x < 0f
            val innerOpenSide = if (isLeftSide) s.leftInner else s.rightInner
            val outerOpenSide = if (isLeftSide) s.leftOuter else s.rightOuter
            val hoodMaskA = exp(-(absS / 0.17f).let { it * it })
            val sideSpreadMaskA = smoothStep(0.08f, 0.36f, absS) * smoothStep(1.0f, 0.70f, absS)
            val asymArchS = if (s.betaV3AnatomicalAsymmetry) s.betaV3AsymmetryStrength.coerceIn(0f, 1f) * 3.0f else 0f
            val foldArchS = if (s.betaV3IntricateLabiaFolds) s.betaV3LabiaFoldsStrength.coerceIn(0f, 1f) else 0f
            val archWidthAsym = if (asymArchS > 1e-5f) {
                val sideBias = if (isLeftSide) 0.0048f else -0.0036f
                asymArchS * sideBias * smoothStep(0.10f, 0.42f, absS) * smoothStep(1.0f, 0.72f, absS)
            } else 0f
            val organicMeander = if (foldArchS > 1e-5f && absS > 0.02f) {
                val mPhase = if (isLeftSide) 0.75f else 2.95f
                val mMask = smoothStep(0.12f, 0.34f, absS) * smoothStep(0.96f, 0.68f, absS)
                (0.0085f * sin(absS * 12.8f + mPhase) + 0.0048f * cos(absS * 23.4f - mPhase * 1.2f)) * mMask * foldArchS
            } else 0f
            val baseHalfW = (0.036f + 0.036f * sinArc + hoodMaskA * s.hoodRetract * 0.016f + archWidthAsym + organicMeander)
                .coerceAtLeast(0.022f)
            val dynamicSpr = (innerOpenSide * 0.145f + outerOpenSide * 0.058f) * sideSpreadMaskA
            val archCenterAbsX = sinArc * (baseHalfW + dynamicSpr) + (1f - sinArc) * 0.006f * smoothStep(0.75f, 1.0f, absS)
            val yAlignGate = smoothStep(bottomY - 0.015f, bottomY + 0.055f, y) *
                (1f - smoothStep(0.205f, 0.285f, y))
            val labiaAlignMask = (1f - smoothStep(archCenterAbsX + 0.004f, archCenterAbsX + 0.044f, abs(x))) * yAlignGate
            val alignedTarget = max(vestibuleColorMask, labiaAlignMask)
            effectiveVestibuleColorMask = (vestibuleColorMask + (alignedTarget - vestibuleColorMask) * medRootS).coerceIn(0f, 1f)
        } else {
            effectiveVestibuleColorMask = vestibuleColorMask
        }
        val warmInnerLabiaMajora = if (s.betaSoftLabiaInteriorEdge) {
            val warmY = smoothStep(SEAM_C_WARM_MAJORA_Y_OUTER_LO, -0.36f, y) *
                (1f - smoothStep(0.30f, SEAM_C_WARM_MAJORA_Y_OUTER_HI, y))
            smoothStep(SEAM_C_WARM_MAJORA_OUTER_X, 0.08f, abs(x)) * warmY
        } else {
            smoothStep(0.38f, 0.08f, abs(x)) * smoothBell(y, -0.52f, 0.46f, 0.16f)
        }
        val anusMucosaHint = exp(-aR2 * 3.2f) * 0.24f
        var mucosa = (effectiveVestibuleColorMask * 1.05f + warmInnerLabiaMajora * 0.16f + anusMucosaHint).coerceIn(0f, 1f)
        val wetness = (effectiveVestibuleColorMask * (0.34f + 0.28f * cavityOpen) +
            exp(-uR2 * 1.8f) * 0.28f * effectiveVestibuleColorMask +
            exp(-iR2 * 1.05f) * 0.34f +
            exp(-aR2 * 4.0f) * 0.10f).coerceIn(0f, 0.86f)

        val closedCleftDarkening = (1f - cavityOpen * 0.72f) * effectiveVestibuleColorMask * 0.42f
        val introAo = exp(-iR2 * 1.55f) * 0.84f
        val urethralAo = exp(-uR2 * 12.0f) * 0.44f * effectiveVestibuleColorMask
        val anusCenterAo = exp(-aR2 * 4.4f) * 0.66f + (0.5f - 0.5f * cos(aAngle * 11f + 0.4f)) * radialFoldMask * 0.20f
        // Softer groin & sulcus occlusion eliminates any dark outer halo ring around the pelvic patch
        val groinColorWeight = if (s.betaSoftLabiaInteriorEdge) {
            smoothStep(SEAM_C_GROIN_Y_OUTER_LO, -0.38f, y) * (1f - smoothStep(0.40f, SEAM_C_GROIN_Y_OUTER_HI, y))
        } else {
            groinWeight
        }
        val leftGroinColorDip = if (s.betaSoftLabiaInteriorEdge && x < leftGroinX) {
            exp(-((x - leftGroinX) / SEAM_C_GROIN_COLOR_OUTER_SIGMA).let { it * it })
        } else {
            leftGroinDip
        }
        val rightGroinColorDip = if (s.betaSoftLabiaInteriorEdge && x > rightGroinX) {
            exp(-((x - rightGroinX) / SEAM_C_GROIN_COLOR_OUTER_SIGMA).let { it * it })
        } else {
            rightGroinDip
        }
        val groinAo = (leftGroinColorDip + rightGroinColorDip) * 0.17f * groinColorWeight

        val sulcusAo = if (s.betaSoftLabiaInteriorEdge) {
            val cleftColorSpan = smoothStep(SEAM_C_SULCUS_Y_OUTER_LO, -0.38f, y) *
                (1f - smoothStep(0.32f, SEAM_C_SULCUS_Y_OUTER_HI, y))
            val sLeftSigma = if (x < sulcusLeftCenter) max(SEAM_C_SULCUS_COLOR_OUTER_SIGMA, sulcusSigma) else sulcusSigma
            val sRightSigma = if (x > sulcusRightCenter) max(SEAM_C_SULCUS_COLOR_OUTER_SIGMA, sulcusSigma) else sulcusSigma
            val sL = exp(-((x - sulcusLeftCenter) / sLeftSigma).let { it * it }) * cleftColorSpan
            val sR = exp(-((x - sulcusRightCenter) / sRightSigma).let { it * it }) * cleftColorSpan
            (sL + sR) * sulcusAoGain
        } else {
            (sulcusL + sulcusR) * sulcusAoGain
        }
        val rapheFlankAo = if (rapheSpan > 0.001f) {
            0.036f * exp(-(((abs(x) - 0.024f) / 0.013f).let { it * it })) * rapheSpan
        } else 0f
        val ao = (1f - closedCleftDarkening - introAo - urethralAo - anusCenterAo - groinAo - sulcusAo - rapheFlankAo).coerceIn(0.08f, 1f)

        // Slightly richer warm cutaneous pigmentation across labia majora cushions fading gently outward into thigh skin
        val pigmentFullnessCurve = if (s.betaSoftLabiaInteriorEdge) {
            val wideBell = smoothStep(SEAM_C_FULLNESS_Y_OUTER_LO, -0.40f, y) *
                (1f - smoothStep(0.38f, SEAM_C_FULLNESS_Y_OUTER_HI, y))
            (0.24f + 0.78f * almondCurve) * wideBell
        } else {
            fullnessCurve
        }
        val majoraBodyPigment = if (s.betaSoftLabiaInteriorEdge) {
            val lExp = if (x < leftCrestX) SEAM_C_MAJORA_PIG_OUTER_EXP else 0.72f
            val rExp = if (x > rightCrestX) SEAM_C_MAJORA_PIG_OUTER_EXP else 0.72f
            (exp(-lDist * lDist * lExp) + exp(-rDist * rDist * rExp)) * pigmentFullnessCurve * (1f - effectiveVestibuleColorMask * 0.65f)
        } else {
            (exp(-lDist * lDist * 0.72f) + exp(-rDist * rDist * 0.72f)) * fullnessCurve * (1f - max(insideVestibule, effectiveVestibuleColorMask) * 0.65f)
        }
        val medialMargin = if (s.betaSoftLabiaInteriorEdge) {
            val mLeftCenter = leftCrestX * 0.62f
            val mRightCenter = rightCrestX * 0.62f
            val mLeftSigma = if (x < mLeftCenter) SEAM_C_MEDIAL_MARGIN_OUTER_SIGMA else 0.10f
            val mRightSigma = if (x > mRightCenter) SEAM_C_MEDIAL_MARGIN_OUTER_SIGMA else 0.10f
            (exp(-((x - mLeftCenter) / mLeftSigma).let { it * it }) +
                exp(-((x - mRightCenter) / mRightSigma).let { it * it })) * pigmentFullnessCurve
        } else {
            (exp(-((x - leftCrestX * 0.62f) / 0.10f).let { it * it }) +
                exp(-((x - rightCrestX * 0.62f) / 0.10f).let { it * it })) * fullnessCurve
        }
        val perianalPigment = exp(-aR2 * 1.15f) * 0.62f +
            exp(-((x / 0.14f).let { it * it } + ((y + 0.84f - anusShiftY) / 0.18f).let { it * it })) * 0.20f
        val raphePigment = if (rapheSpan > 0.001f) {
            0.130f * exp(-((x / 0.0185f).let { it * it })) * rapheSpan
        } else 0f
        var pigment = (majoraBodyPigment * 0.22f +
            medialMargin * 0.28f +
            (leftGroinColorDip + rightGroinColorDip) * 0.09f * groinColorWeight +
            perianalPigment +
            raphePigment).coerceIn(0f, 1f)
        // Beta v3 Option 8 (v28.0): soften excess anterior-commissure pigmentation right around the
        // clitoral hood landing apron so the hood transitions organically into natural mons pubis skin.
        if (hoodMonsS > 1e-5f) {
            val hoodApronY = smoothStep(0.16f, 0.28f, y) * (1f - smoothStep(0.46f, 0.62f, y))
            val hoodApronX = 1f - smoothStep(0.085f, 0.185f, abs(x))
            val hoodApron = hoodApronY * hoodApronX * (1f - smoothStep(0.32f, 0.72f, mucosa))
            pigment *= (1f - hoodApron * hoodMonsS * 0.78f)
        }
        // Beta v3 Option 9 (v29.0 Upper Hood–Mons Color Blend):
        // Softly feathers the rose-brown triangular upper-hood & anterior-commissure island (center +
        // two shoulders) immediately above the glans and below the pubic hair into adjacent mons skin,
        // even where vestibule mucosa is high (~0.81). Anchored to hoodApexY so it follows hoodRetract,
        // strictly above the vestibular floor, and preserves dz, wetness, and AO fold shadow identically.
        if (hoodMonsUpperS > 1e-5f) {
            val hoodApexY = 0.292f + s.hoodRetract * 0.088f
            val spreadExpand = avgOuter.coerceIn(0f, 1f) * 0.045f
            val upperYLow = smoothStep(hoodApexY - 0.020f, hoodApexY + 0.055f, y)
            val upperYHigh = 1f - smoothStep(0.47f + s.hoodRetract * 0.04f, 0.63f + s.hoodRetract * 0.04f, y)
            val yTriNorm = ((y - hoodApexY) / (0.24f + s.hoodRetract * 0.03f)).coerceIn(0f, 1f)
            val halfWInner = 0.115f + spreadExpand - 0.035f * yTriNorm
            val halfWOuter = 0.215f + spreadExpand - 0.050f * yTriNorm
            val upperXMask = 1f - smoothStep(halfWInner, halfWOuter, abs(x))
            val upperHoodMask = (upperYLow * upperYHigh * upperXMask).coerceIn(0f, 1f)
            if (upperHoodMask > 1e-5f) {
                val blendW = upperHoodMask * hoodMonsUpperS
                mucosa *= (1f - blendW * 0.90f)
                pigment *= (1f - blendW * 0.86f)
            }
        }

        out.dz = dz
        out.mucosa = mucosa
        out.wetness = wetness
        out.ao = ao
        out.pigment = pigment
        return out
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
                val cleftShiftY = if (s.betaShortPerineum) 0.078f else 0f
                val innerThighLift = smoothStep(0.42f, 0.82f, abs(x)) * 0.15f * smoothBell(y, -0.68f, 0.70f, 0.34f)
                val buttockHint = 0.095f * exp(-(((abs(x) - 0.34f) / 0.28f).let { it * it } + ((y + 0.94f) / 0.26f).let { it * it }))
                val glutealCleft = 0.105f * exp(-((x / 0.075f).let { it * it })) * smoothStep(-0.82f + cleftShiftY, -1.10f + cleftShiftY, y)
                val rimRoll = -0.32f * edgeR * edgeR * edgeR * edgeR

                val detail = evaluateVulvaDetail(x, y, s)
                val z = baseCurve + innerThighLift + buttockHint - glutealCleft + rimRoll + detail.dz

                // Gentle gradual vignette falloff without a dark halo ring or hard rim
                val rimShadow = 1f - 0.28f * smoothStep(0.80f, 1.0f, edgeR)
                val cleftShadow = 1f - 0.34f * exp(-((x / 0.085f).let { it * it })) * smoothStep(-0.82f + cleftShiftY, -1.08f + cleftShiftY, y)
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
            detailEvaluator = { x, y -> evaluateVulvaDetail(x, y, s, scratchDetailSample) },
            computeNormals = ::computeGridNormals,
            leftGluteX = s.leftGluteX,
            leftGluteY = s.leftGluteY,
            rightGluteX = s.rightGluteX,
            rightGluteY = s.rightGluteY,
            betaShortPerineum = s.betaShortPerineum,
            betaSmoothVulvaSeam = s.betaSmoothVulvaSeam,
            betaBlendVulvaSeamColor = s.betaBlendVulvaSeamColor
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
        val cx = archCx
        val cy = archCy
        val ox = archOx
        val oy = archOy
        val apexY = 0.292f + s.hoodRetract * 0.088f
        val bottomY = -0.448f
        val ySpan = apexY - bottomY

        // Beta v3 sliders 1, 3, 4 are amplified 3x (0% = off, 100% = 3.0x); options 5 & 6 use direct 0..1 strength.
        val contS = if (s.betaV3OrganicContinuity) s.betaV3ContinuityStrength.coerceIn(0f, 1f) * 3.0f else 0f
        val asymS = if (s.betaV3AnatomicalAsymmetry) s.betaV3AsymmetryStrength.coerceIn(0f, 1f) * 3.0f else 0f
        val surfS = if (s.betaV3RegionalSurfaceDetail) s.betaV3SurfaceStrength.coerceIn(0f, 1f) * 3.0f else 0f
        val blendS = if (s.betaV3LabialVestibuleBlend) s.betaV3LabialBlendStrength.coerceIn(0f, 1f) else 0f
        val foldS = if (s.betaV3IntricateLabiaFolds) s.betaV3LabiaFoldsStrength.coerceIn(0f, 1f) else 0f
        val medRootS = if (s.betaV3MedialRootContinuity) s.betaV3MedialRootStrength.coerceIn(0f, 1f) else 0f
        val hoodMonsS = if (s.betaV3HoodMonsBlend) s.betaV3HoodMonsStrength.coerceIn(0f, 1f) else 0f
        val hoodMonsUpperS = if (s.betaV3HoodMonsUpperBlend) s.betaV3HoodMonsUpperStrength.coerceIn(0f, 1f) else 0f

        for (r in 0 until archRows) {
            val arcS = (r.toFloat() / (archRows - 1)) * 2f - 1f
            val absS = abs(arcS)
            val signS = if (arcS < 0f) -1f else 1f
            val isLeftCenter = arcS < 0f
            val innerOpenSide = if (isLeftCenter) s.leftInner else s.rightInner
            val outerOpenSide = if (isLeftCenter) s.leftOuter else s.rightOuter

            val hoodMask = exp(-(arcS / 0.17f).let { it * it })
            val cosArc = cos(absS * PI.toFloat())
            val yNorm = (cosArc + 1f) * 0.5f
            cy[r] = bottomY + yNorm * ySpan

            // Narrow delicate arch at top & gracefully converging ribbon tips at bottom posterior fourchette
            val sinArc = sin(absS * PI.toFloat()).coerceAtLeast(0f)
            val sideSpreadMask = smoothStep(0.08f, 0.36f, absS) * smoothStep(1.0f, 0.70f, absS)
            val archWidthAsym = if (asymS > 1e-5f) {
                val sideBias = if (isLeftCenter) 0.0048f else -0.0036f
                asymS * sideBias * smoothStep(0.10f, 0.42f, absS) * smoothStep(1.0f, 0.72f, absS)
            } else 0f
            // Beta v3 Toggle 6: gentle organic centerline meander breaking up the smooth geometric oval curve
            // (strictly 0 at hood apex mid and converging tips so midline & convexity stay defect-free)
            val organicMeander = if (foldS > 1e-5f && absS > 0.02f) {
                val mPhase = if (isLeftCenter) 0.75f else 2.95f
                val mMask = smoothStep(0.12f, 0.34f, absS) * smoothStep(0.96f, 0.68f, absS)
                (0.0085f * sin(absS * 12.8f + mPhase) + 0.0048f * cos(absS * 23.4f - mPhase * 1.2f)) * mMask * foldS
            } else 0f
            val baseHalfWidth = (0.036f + 0.036f * sinArc + hoodMask * s.hoodRetract * 0.016f + archWidthAsym + organicMeander)
                .coerceAtLeast(0.022f)
            val dynamicSpread = (innerOpenSide * 0.145f + outerOpenSide * 0.058f) * sideSpreadMask
            cx[r] = signS * (sinArc * (baseHalfWidth + dynamicSpread) + (1f - sinArc) * 0.006f * smoothStep(0.75f, 1.0f, absS))
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

        if (s.betaV2NonInvertingLabiaFold) {
            // Unwrap outward normal angle theta(r) continuously from the hood apex outward and project
            // onto a smooth monotonically non-increasing (convex outward) ray fan. This guarantees
            // -theta'(r) >= 0 so spread-induced S-bends in sideSpreadMask never cause adjacent rows
            // of the expanded outer skirt to cross over each other and invert CCW triangle winding.
            val theta = archTheta
            val mid = archRows / 2
            theta[mid] = atan2(oy[mid], ox[mid])
            val twoPi = (2.0 * PI).toFloat()
            val piF = PI.toFloat()
            for (r in mid + 1 until archRows) {
                var a = atan2(oy[r], ox[r])
                while (a - theta[r - 1] > piF) a -= twoPi
                while (a - theta[r - 1] < -piF) a += twoPi
                theta[r] = a
            }
            for (r in mid - 1 downTo 0) {
                var a = atan2(oy[r], ox[r])
                while (a - theta[r + 1] > piF) a -= twoPi
                while (a - theta[r + 1] < -piF) a += twoPi
                theta[r] = a
            }
            // Bidirectional non-increasing envelope average (preserves symmetry & minimal deviation)
            val fwd = archFwd
            val bwd = archBwd
            fwd[0] = theta[0]
            for (r in 1 until archRows) {
                fwd[r] = min(fwd[r - 1], theta[r])
            }
            bwd[archRows - 1] = theta[archRows - 1]
            for (r in archRows - 2 downTo 0) {
                bwd[r] = max(bwd[r + 1], theta[r])
            }
            for (r in 0 until archRows) {
                theta[r] = (fwd[r] + bwd[r]) * 0.5f
            }
            // Two monotonicity-preserving 3-tap smoothing passes to eliminate sharp curvature corners
            val smoothTmp = archSmoothTmp
            repeat(2) {
                smoothTmp[0] = theta[0]
                smoothTmp[archRows - 1] = theta[archRows - 1]
                for (r in 1 until archRows - 1) {
                    smoothTmp[r] = 0.25f * theta[r - 1] + 0.50f * theta[r] + 0.25f * theta[r + 1]
                }
                smoothTmp.copyInto(theta)
            }
            for (r in 0 until archRows) {
                ox[r] = cos(theta[r])
                oy[r] = sin(theta[r])
            }
        }

        val glansCenterY = 0.254f
        val glansBaseZ = if (s.betaV2NonInvertingLabiaFold) {
            sampleBodySurfaceZ(0f, glansCenterY, s, includeLithotomy) - 0.008f
        } else {
            0f
        }
        val glansRx = 0.034f + s.hoodRetract * 0.004f
        val glansRy = 0.045f + s.hoodRetract * 0.005f
        val glansRz = 0.042f

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
            val fourchetteEnd = if (isLeft) {
                (0.52f - asymS * 0.045f).coerceAtLeast(0.36f)
            } else {
                (0.59f + asymS * 0.040f).coerceAtMost(0.76f)
            }
            val fourchetteTaper = smoothStep(1.0f, fourchetteEnd, absS)
            val labiaMidArch = sin(absS * PI.toFloat()).coerceIn(0f, 1f) * fourchetteTaper
            // Subtly offset upper-mid labial lobe fullness between left and right sides
            val lobePeakS = if (isLeft) (0.34f - asymS * 0.035f).coerceAtLeast(0.22f) else (0.40f + asymS * 0.035f).coerceAtMost(0.54f)
            val upperLobeFullness = 0.78f + (0.33f + (if (isLeft) 0.045f else -0.035f) * asymS).coerceIn(0.16f, 0.54f) *
                exp(-((absS - lobePeakS) / 0.22f).let { it * it })

            val wrinkleTension = (1f - innerOpen * 0.40f).coerceIn(0.42f, 1f)
            val asymPhase = if (isLeft) 0.62f else 2.84f
            val sideHeightAsym = if (isLeft) {
                (1.055f + asymS * 0.075f).coerceAtMost(1.32f)
            } else {
                (0.945f - asymS * 0.065f).coerceAtLeast(0.70f)
            }
            val sideThickPow = if (isLeft) {
                (0.74 - asymS * 0.04).coerceAtLeast(0.56)
            } else {
                (0.83 + asymS * 0.04).coerceAtMost(1.02)
            }

            // Non-periodic multi-harmonic irregular labial scalloping & soft ruffling (breaks up uniform parallel pleats!)
            val phaseWarp = 0.55f * sin(absS * 7.3f + asymPhase * 0.8f)
            val extraScallop = if (foldS > 1e-5f) {
                val fPhase = if (isLeft) 1.15f else 3.72f
                (0.0115f * sin(absS * 19.3f + fPhase + 0.7f * cos(absS * 8.1f)) +
                    0.0072f * cos(absS * 34.8f - fPhase * 1.3f) +
                    0.0044f * sin(absS * 57.5f + fPhase * 0.9f)) * labiaMidArch * labiaMask * foldS
            } else 0f
            val labiaLobes = (0.019f * sin(absS * 11.3f + asymPhase + phaseWarp) +
                0.012f * cos(absS * 21.7f - asymPhase * 1.18f - phaseWarp * 0.7f) +
                0.0068f * sin(absS * 37.4f + asymPhase * 1.52f + phaseWarp * 1.1f) * wrinkleTension +
                0.0032f * cos(absS * 61.9f - asymPhase * 0.85f) * wrinkleTension) * labiaMidArch * labiaMask + extraScallop

            // Vulva Beta v2 Toggle 6 (Step 1 + Step 2): conservative dorsal clitoral hood crest fullness
            // (+0.014f * hoodMask) layered cleanly on top of Step 1 without touching labia petals or landings.
            val hoodCrestBoost = if (s.betaV2BalancedUpperVulvaDepthWithCrest) 0.014f else 0f
            val hoodCrestHeight = (0.042f + hoodCrestBoost + 0.009f * s.hoodRetract) * hoodMask
            val labiaCrestHeightBase = ((0.055f + 0.020f * innerOpen) * (0.28f + 0.72f * labiaMidArch) * upperLobeFullness * sideHeightAsym +
                labiaLobes * (0.72f + surfS * 0.14f))
            val labiaCrestFloor = if (asymS > 1e-5f || surfS > 1e-5f || foldS > 1e-5f) {
                0.016f * (0.28f + 0.72f * labiaMidArch)
            } else {
                Float.NEGATIVE_INFINITY
            }
            val labiaCrestHeight = max(labiaCrestHeightBase, labiaCrestFloor) * labiaMask * fourchetteTaper
            val totalHeight = (hoodCrestHeight + labiaCrestHeight).coerceAtLeast(0.001f)

            val crestOutwardShift = (0.026f + innerOpen * 0.058f + outerOpen * 0.024f) * labiaMidArch * labiaMask -
                (0.024f - s.hoodRetract * 0.036f) * hoodMask

            // Organic irregular ruffled edge flutter along the rolled labial free margin
            val crestWaveGain = (1f + asymS * (if (isLeft) 0.18f else -0.10f)).coerceIn(0.62f, 1.60f) *
                (1f + foldS * 0.58f)
            val crestWave = (0.0125f * sin(absS * 17.6f + asymPhase + phaseWarp * 1.15f) +
                0.0068f * cos(absS * 33.4f - asymPhase * 1.1f) +
                0.0030f * sin(absS * 58.7f + asymPhase * 1.65f)) *
                labiaMidArch * labiaMask * wrinkleTension * crestWaveGain

            val foldShift = 1.45f * sin(absS * 9.4f + asymPhase) + 0.65f * cos(absS * 16.2f - asymPhase)
            val hoodDorsalArcGain = (0.0085f + surfS * 0.0022f) * exp(-((arcS / 0.085f).let { it * it })) * hoodMask
            val rimOrganicMod = 0.86f + 0.14f * sin(absS * 14.2f + asymPhase)
            val crestU = if (isLeft) 0.37f else 0.35f
            val rolledShoulderGain = (if (isLeft) 0.0122f else 0.0102f) * labiaMask * labiaMidArch

            // Beta v3 Toggle 6: wavy, irregular, asymmetric medial border width where inner labia meet the vestibule
            val medialBorderWave = if (foldS > 1e-5f) {
                val bPhase = if (isLeft) 0.92f else 3.38f
                val bWarp = 0.65f * sin(absS * 8.4f + bPhase)
                (0.0105f * sin(absS * 15.6f + bPhase + bWarp) +
                    0.0062f * cos(absS * 29.8f - bPhase * 1.15f) +
                    0.0032f * sin(absS * 51.2f + bPhase * 0.7f)) * labiaMidArch * labiaMask * foldS
            } else 0f
            val innerInsetWidth = (0.024f * labiaMidArch * labiaMask + 0.020f * hoodMask + medialBorderWave)
                .coerceAtLeast(0.010f * labiaMidArch * labiaMask + 0.018f * hoodMask)
            val rawCrestPos = crestOutwardShift + crestWave
            // Vulva Beta v2 Toggle 2 (betaV2NonInvertingLabiaFold):
            // 1. Inner-wall monotonicity guard: L'(t) > 0 on [0, crestU] requires C > -(2/pi)*I ~ -0.6366*I.
            val effectiveCrestPos = if (s.betaV2NonInvertingLabiaFold) {
                max(rawCrestPos, -innerInsetWidth * 0.52f)
            } else {
                rawCrestPos
            }
            // 2. Narrower, delicately tapered skirt that expands with exact pose-dependent crest displacement
            //    and satisfies the derived non-folding floor W >= C + 1.145 * R + clearance margin.
            val baseOuterSkirtWidth = ((0.072f + 0.038f * labiaMidArch) * (0.35f + 0.65f * fourchetteTaper)) * labiaMask +
                (0.132f - s.hoodRetract * 0.025f) * hoodMask
            val outerSkirtWidth = if (s.betaV2NonInvertingLabiaFold) {
                val dynamicCrestShift = (innerOpen * 0.058f + outerOpen * 0.024f) * labiaMidArch * labiaMask
                val nonFoldFloor = effectiveCrestPos + 1.145f * rolledShoulderGain +
                    0.026f * labiaMidArch * labiaMask + 0.004f * hoodMask + 0.002f
                max(baseOuterSkirtWidth + dynamicCrestShift, nonFoldFloor)
            } else {
                baseOuterSkirtWidth
            }
            // 3. C1-smooth strictly monotone midline-crossing guard for lateralOffset < 0 so neither
            //    the tight-curvature hood inner margin nor lower labial root crosses x = 0.
            val oxAbs = abs(ox[r])
            val maxNegLatX = if (s.betaV2NonInvertingLabiaFold && oxAbs > 1e-4f) {
                (abs(cx[r]) * 0.82f) / oxAbs
            } else {
                Float.POSITIVE_INFINITY
            }

            for (c in 0 until archCols) {
                val u = c.toFloat() / (archCols - 1)
                val lateralOffset: Float
                val heightFrac: Float

                if (u <= crestU) {
                    val t = u / crestU
                    val sCurve = sin(t * (PI.toFloat() * 0.5f))
                    // Subtle left/right thickness asymmetry along plump rolled medial face
                    lateralOffset = (-innerInsetWidth) * (1f - t) + effectiveCrestPos * sCurve
                    heightFrac = Math.pow(sCurve.coerceAtLeast(0f).toDouble(), sideThickPow).toFloat()
                } else {
                    val t = (u - crestU) / (1f - crestU)
                    val ease = t * t * (3f - 2f * t)
                    // Soft rounded lateral shoulder before landing cleanly into the interlabial sulcus
                    val rolledShoulder = rolledShoulderGain * sin(t * PI.toFloat()) * (1f - t)
                    lateralOffset = effectiveCrestPos * (1f - ease) + outerSkirtWidth * ease + rolledShoulder
                    // Ultra-smooth C2 zero-slope tangent landing (cos^2.65): dissolves invisibly into body skin!
                    val cosT = cos(t * (PI.toFloat() * 0.5f)).coerceAtLeast(0f)
                    heightFrac = Math.pow(cosT.toDouble(), 2.65).toFloat()
                }

                // Irregular organic ruffle micro-folds along the labia minora petals (non-uniform spacing)
                val radialFoldWave = sin(absS * 44.5f + foldShift + u * 5.2f + asymPhase)
                val fineRippleWave = cos(absS * 83.0f - foldShift * 0.7f - u * 4.6f + asymPhase * 0.7f)
                val labiaFold = (0.0052f * radialFoldWave + 0.0026f * fineRippleWave) *
                    heightFrac * labiaMidArch * labiaMask * wrinkleTension

                // Beta v3 Toggle 6 (Intricate Inner-Labia Folds): rich multi-scale crisscrossing oblique
                // & branched mucosal folds sculpted across the inner labial face (u in 0.14..0.64)
                val innerTangleMask = smoothStep(0.14f, 0.26f, u) * (1f - smoothStep(0.36f, 0.64f, u)) *
                    labiaMidArch * labiaMask * foldS
                var innerTangleWave = 0f
                val innerTangleZ = if (innerTangleMask > 1e-5f) {
                    val tPhase = if (isLeft) 0.48f else 2.71f
                    val uWarp = u * 15.5f + 1.45f * sin(absS * 14.2f + tPhase)
                    val sWarp = absS * 38.0f + 1.85f * cos(u * 12.8f - tPhase * 0.9f)
                    val ridge1 = sin(sWarp + uWarp * 1.15f + tPhase)
                    val ridge2 = cos(absS * 66.0f - u * 23.5f + 1.1f * sin(sWarp * 0.45f) - tPhase * 1.25f)
                    val lobule = sin(absS * 24.5f + tPhase * 1.6f) * cos(u * 14.2f + 0.8f * sin(absS * 19f))
                    // Soft ridged folding (broad convex cushions separated by delicate crinkled creases)
                    val ridged = (1f - abs(ridge1)) * 0.55f + ridge2 * 0.30f + lobule * 0.35f - 0.12f
                    innerTangleWave = ridged
                    val reliefZ = (0.0082f * (ridged + 0.56f).coerceAtLeast(0f) + 0.0030f * abs(ridge2))
                    reliefZ * innerTangleMask * (0.28f + 0.72f * heightFrac)
                } else 0f

                // Subtle dorsal prepuce ridge & soft transverse retraction folds over the clitoral hood
                val hoodDorsalRidge = hoodDorsalArcGain * exp(-((u - 0.52f) / 0.20f).let { it * it })
                val hoodRollFold = s.hoodRetract * hoodMask * (
                    0.0105f * exp(-((u - 0.52f) / 0.10f).let { it * it }) +
                        0.0058f * exp(-((u - 0.72f) / 0.095f).let { it * it })
                    )

                val lateralOffsetX = if (s.betaV2NonInvertingLabiaFold && lateralOffset < 0f && maxNegLatX.isFinite()) {
                    if (maxNegLatX > 1e-6f) {
                        (maxNegLatX * lateralOffset) / (maxNegLatX - lateralOffset)
                    } else {
                        0f
                    }
                } else {
                    lateralOffset
                }
                val px = cx[r] + ox[r] * lateralOffsetX
                val py = cy[r] + oy[r] * lateralOffset

                // EXACT FLUSH ANCHORING + EXACT UNDERLYING MATERIAL SAMPLING (zero allocation)!
                val activeLut = resolveAnteriorLut(s)
                val groundMat = evaluateVulvaDetail(px, py, s, scratchDetailSample)
                val groundZ = evaluateCentralBaseCurveZ(px, py, includeLithotomy, activeLut) + groundMat.dz

                // Micro-tuck only at extreme outer boundary AFTER normal & material already match ground 100%
                // Beta v3 Option 7 (v22.0): tighten medial innerTuck to a crisp micro-tuck right at u = 0..0.065
                // so u = 0 stays strictly below patchZ (zero floating edge) while u >= 0.04 rises cleanly without
                // exposing a deep underlying patch shelf or letting the lower patch poke through the arch.
                val baseInnerTuck = -0.014f * (1f - smoothStep(0.0f, 0.16f, u))
                val innerTuck = if (medRootS > 1e-5f) {
                    val cleanInnerTuck = -0.0038f * (1f - smoothStep(0.0f, 0.065f, u))
                    baseInnerTuck * (1f - medRootS) + cleanInnerTuck * medRootS
                } else {
                    baseInnerTuck
                }
                val outerTuck = -0.0034f * smoothStep(0.91f, 1.0f, u)
                val rawTipSink = -0.012f * (1f - fourchetteTaper)
                val tipSink = if (s.betaV2SeamOnlyPolyOffset) {
                    // Vulva Beta v2 Toggle 1: keep opaque lower labial tips above groundZ and only tuck
                    // once fourchetteTaper has already faded near the posterior fourchette.
                    rawTipSink * smoothStep(0.32f, 0.0f, fourchetteTaper)
                } else {
                    rawTipSink
                }
                var pz = groundZ + totalHeight * heightFrac + labiaFold + innerTangleZ + hoodDorsalRidge + hoodRollFold +
                    innerTuck + outerTuck + tipSink

                // Vulva Beta v2 Toggle 2: prevent hood inner margin from slicing through the glans ellipsoid
                if (s.betaV2NonInvertingLabiaFold && hoodMask > 0.02f) {
                    val gU = px / (glansRx * 1.14f)
                    val gV = (py - glansCenterY) / (glansRy * 1.14f)
                    val gR2 = gU * gU + gV * gV
                    if (gR2 < 1.0f) {
                        val glansSurfZ = glansBaseZ + glansRz * sqrt((1f - gR2).coerceAtLeast(0f))
                        val clearanceZ = glansSurfZ + 0.0035f
                        if (pz < clearanceZ) {
                            val clearBlend = (1f - smoothStep(0.78f, 1.0f, gR2)) * smoothStep(0.02f, 0.14f, hoodMask)
                            pz += (clearanceZ - pz) * clearBlend
                        }
                    }
                }

                // Silky anatomical mucocutaneous color gradient:
                //  - Inner medial face (u < 0.34): glistening warm rose-coral mucosa
                //  - Rolled fleshy free margin (u ~ 0.35): richer natural darker pigmentation tapering softly along the scalloped rim
                //  - Outer hood & outer skirt (u -> 0.84): 100% EXACT lerp into underlying body skin material -> ZERO border!
                val innerFaceMask = 1f - smoothStep(0.24f, 0.50f, u)
                val hoodOuterSkin = hoodMask * smoothStep(0.20f, 0.58f, u)
                // Beta v3 Option 8 (v28.0): expand natural mons-skin continuity over the dorsal & outer
                // clitoral hood while keeping the delicate inner preputial edge (u < 0.16) naturally rosy.
                val hoodDorsalSkinW = if (hoodMonsS > 1e-5f) {
                    (hoodMask * smoothStep(0.11f, 0.34f, u) * hoodMonsS).coerceIn(0f, 1f)
                } else 0f
                // Beta v3 Option 9 (v29.0): matching upper-hood & anterior-commissure color blend on the
                // dorsal arch side of the arch-to-patch transition (follows apexY/hoodRetract, strictly
                // preserves inner preputial rim u < 0.16, lower labia, wetness, and fold shadow AO).
                val hoodUpperDorsalW = if (hoodMonsUpperS > 1e-5f) {
                    val upperArchY = smoothStep(apexY - 0.030f, apexY + 0.045f, py)
                    val dorsalRimGuard = smoothStep(0.16f, 0.36f, u)
                    (hoodMask * upperArchY * dorsalRimGuard * hoodMonsUpperS).coerceIn(0f, 1f)
                } else 0f
                val combinedHoodColorW = max(hoodDorsalSkinW, hoodUpperDorsalW)

                val archMucosa = ((0.90f * innerFaceMask + 0.40f * (1f - innerFaceMask) * (1f - smoothStep(0.34f, 0.74f, u))) *
                    (1f - hoodOuterSkin * 0.96f) * (1f - combinedHoodColorW * 0.90f)).coerceIn(0f, 1f)
                // Beta v3 Toggle 6: richer wetness pooling in inner labial fold troughs & softer sheen on crests
                val tangleWetDelta = if (innerTangleMask > 1e-5f) {
                    (-innerTangleWave * 0.22f + 0.08f) * innerTangleMask
                } else 0f
                val archWetness = (((0.64f * innerFaceMask + 0.20f * heightFrac) * (1f - hoodOuterSkin * 0.92f) *
                    (1f - hoodDorsalSkinW * 0.86f) + 0.055f) + tangleWetDelta).coerceIn(0.055f, 0.88f)

                val foldCrevice = (0.5f - 0.5f * radialFoldWave) * heightFrac * labiaMask * wrinkleTension
                val freeEdgeRim = exp(-((u - crestU) / 0.15f).let { it * it })
                val tanglePigDelta = if (innerTangleMask > 1e-5f) {
                    (0.09f * innerTangleWave + 0.06f * sin(absS * 21.0f + u * 9.5f + asymPhase)) * innerTangleMask
                } else 0f
                val rawArchPigment = (0.12f + 0.44f * freeEdgeRim * rimOrganicMod * labiaMask +
                    0.16f * freeEdgeRim * hoodMask + foldCrevice * 0.12f + tanglePigDelta)
                    .coerceIn(0f, 1f)
                val archPigment = (rawArchPigment * (1f - combinedHoodColorW * 0.88f)).coerceIn(0f, 1f)

                // Beta v3 Toggle 5 (Soft Labial-Vestibule Color Blend) & Option 7: ease the dark rootShadow band
                // at the medial labial base so it merges into the aligned vestibule floor without a harsh line!
                val rootShadowEase = max(blendS, medRootS * 0.88f)
                val rootShadow = (1f - smoothStep(0.0f, 0.22f, u)) * 0.26f * (1f - 0.86f * rootShadowEase * labiaMask) +
                    smoothStep(0.62f, 0.84f, u) * 0.06f * labiaMask
                val tangleAoDrop = if (innerTangleMask > 1e-5f) {
                    ((-innerTangleWave).coerceAtLeast(0f) * 0.14f) * innerTangleMask
                } else 0f
                val baseArchAo = (0.95f - rootShadow - foldCrevice * 0.11f - tangleAoDrop + 0.06f * heightFrac).coerceIn(0.46f, 1f)
                val archAo = (baseArchAo + (0.98f - baseArchAo) * hoodDorsalSkinW * 0.42f).coerceIn(0.46f, 1f)

                // Reach 100% exact underlying body material BEFORE u = 0.84 so outer skirt transition is invisible!
                // Beta v3 Toggle 1 (Organic Tissue Continuity, 3x scaled): softens base landing transition smoothly
                // without affecting the inner mucosal core or skirt monotonicity.
                val seamStart = ((0.42f - 0.14f * hoodMask) - contS * 0.075f).coerceAtLeast(0.14f)
                val seamBlend = if (s.betaSoftArchEdge) {
                    smoothStep(seamStart, SEAM_E_ARCH_COLOR_BLEND_END, u)
                } else {
                    smoothStep(seamStart, 0.84f, u)
                }
                val tipBlend = 1f - fourchetteTaper
                // Beta v3 Toggle 5 & Option 7: smoothly blend the medial labial base (u -> 0) directly into the
                // exact underlying (now labia-aligned!) vulvar vestibule material (mucosa, wetness, AO, pigment)!
                val innerBaseBlend = if (blendS > 1e-5f) {
                    (1f - smoothStep(0.0f, crestU * (0.68f + 0.30f * blendS), u)) * labiaMask * (0.94f * blendS)
                } else 0f
                val alignedRootBlend = if (medRootS > 1e-5f) {
                    (1f - smoothStep(0.0f, crestU * 0.72f, u)) * labiaMask * (0.92f * medRootS)
                } else 0f
                val medialBaseBlend = max(innerBaseBlend, alignedRootBlend)
                val bodyLerp = if (s.betaSoftArchEdge) {
                    max(max(seamBlend, tipBlend), medialBaseBlend).coerceIn(0f, 1f)
                } else {
                    max(max(seamBlend, tipBlend * 0.90f), medialBaseBlend).coerceIn(0f, 1f)
                }

                // Seam E (betaSoftArchEdge): match exact surrounding vulva-patch material (baseAo, basePigment,
                // BODY_BASE_WETNESS & matMask) and fade outer ~20% skirt alpha cleanly to 0 before depth tuck!
                val targetMucosa: Float
                val targetWetness: Float
                val targetAo: Float
                val targetPigment: Float
                val archAlpha: Float
                if (s.betaSoftArchEdge) {
                    if (includeLithotomy) {
                        val patchMatMask = BlossomSdfBodyBuilder.evaluateVulvaPatchMatMask(
                            x = px,
                            y = py,
                            betaShortPerineum = s.betaShortPerineum,
                            betaBlendVulvaSeamColor = s.betaBlendVulvaSeamColor
                        )
                        val patchBaseAo = activeLut.sampleAo(px, py)
                        val patchBasePigment = activeLut.samplePigment(px, py)
                        targetMucosa = (groundMat.mucosa * patchMatMask).coerceIn(0f, 1f)
                        targetWetness = (BlossomSdfBodyBuilder.BODY_BASE_WETNESS + groundMat.wetness * patchMatMask).coerceIn(0f, 1f)
                        val patchDetailAo = 1f - (1f - groundMat.ao) * patchMatMask
                        targetAo = (patchBaseAo * patchDetailAo).coerceIn(0.06f, 1f)
                        targetPigment = (patchBasePigment + groundMat.pigment * patchMatMask).coerceIn(0f, 1f)
                    } else {
                        targetMucosa = groundMat.mucosa
                        targetWetness = groundMat.wetness
                        targetAo = groundMat.ao
                        targetPigment = groundMat.pigment
                    }
                    val uAlpha = 1f - smoothStep(SEAM_E_ARCH_ALPHA_U_START, SEAM_E_ARCH_ALPHA_U_END, u)
                    // Beta v3 Toggle 5: softly feather only the extreme buried medial root (u < 0.115) so the
                    // medial labial base dissolves seamlessly into the underlying mucosal vestibule floor!
                    val innerRootAlpha = if (blendS > 1e-5f) {
                        val medFeather = smoothStep(0.0f, 0.115f, u)
                        1f - (1f - medFeather) * blendS * labiaMask
                    } else 1f
                    val tipGate = 1f - smoothStep(0.25f, 0.78f, fourchetteTaper)
                    val elevAbovePatch = pz - groundZ
                    val tipElevAlpha = 1f - tipGate * (1f - smoothStep(SEAM_E_ARCH_TIP_ELEV_START, SEAM_E_ARCH_TIP_ELEV_END, elevAbovePatch))
                    archAlpha = (uAlpha * innerRootAlpha * tipElevAlpha).coerceIn(0f, 1f)
                } else {
                    targetMucosa = groundMat.mucosa
                    targetWetness = groundMat.wetness
                    targetAo = groundMat.ao
                    targetPigment = groundMat.pigment
                    archAlpha = 1f
                }

                val finalMucosa = archMucosa * (1f - bodyLerp) + targetMucosa * bodyLerp
                val finalWetness = archWetness * (1f - bodyLerp) + targetWetness * bodyLerp
                val finalAo = archAo * (1f - bodyLerp) + targetAo * bodyLerp
                val finalPigment = archPigment * (1f - bodyLerp) + targetPigment * bodyLerp

                labiaHoodArchAlphaData[r * archCols + c] = archAlpha

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
        labiaHoodArchAlphaBuffer.position(0)
        labiaHoodArchAlphaBuffer.put(labiaHoodArchAlphaData)
        labiaHoodArchAlphaBuffer.position(0)
        labiaHoodArchAlphaVersion++

        // Partition CCW arch triangles into Pass-1 opaque core (any vertex >= 0.995) and Pass-2 feathered skirt
        // (any vertex < 0.995 and any vertex >= 0.004) to avoid rasterizing ~75% discarded triangles in Pass 2.
        var coreCount = 0
        var skirtCount = 0
        val ccw = labiaHoodArchCcwIndices
        val alphas = labiaHoodArchAlphaData
        var t = 0
        while (t < archTotalIndices) {
            val i0 = ccw[t]
            val i1 = ccw[t + 1]
            val i2 = ccw[t + 2]
            val a0 = alphas[i0.toInt() and 0xFFFF]
            val a1 = alphas[i1.toInt() and 0xFFFF]
            val a2 = alphas[i2.toInt() and 0xFFFF]
            if (a0 >= 0.995f || a1 >= 0.995f || a2 >= 0.995f) {
                labiaHoodArchCoreIndices[coreCount++] = i0
                labiaHoodArchCoreIndices[coreCount++] = i1
                labiaHoodArchCoreIndices[coreCount++] = i2
            }
            if ((a0 < 0.995f || a1 < 0.995f || a2 < 0.995f) &&
                (a0 >= 0.004f || a1 >= 0.004f || a2 >= 0.004f)
            ) {
                labiaHoodArchSkirtIndices[skirtCount++] = i0
                labiaHoodArchSkirtIndices[skirtCount++] = i1
                labiaHoodArchSkirtIndices[skirtCount++] = i2
            }
            t += 3
        }
        labiaHoodArchCoreIndexCount = coreCount
        labiaHoodArchSkirtIndexCount = skirtCount
        labiaHoodArchCoreIndexBuffer.position(0)
        labiaHoodArchCoreIndexBuffer.put(labiaHoodArchCoreIndices, 0, coreCount)
        labiaHoodArchCoreIndexBuffer.position(0)
        labiaHoodArchSkirtIndexBuffer.position(0)
        labiaHoodArchSkirtIndexBuffer.put(labiaHoodArchSkirtIndices, 0, skirtCount)
        labiaHoodArchSkirtIndexBuffer.position(0)
        labiaHoodArchSplitVersion++
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

    private fun buildGridIndices(cols: Int, rows: Int, flipWinding: Boolean = false): ShortArray {
        val out = ShortArray((cols - 1) * (rows - 1) * 6)
        var k = 0
        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val i0 = (r * cols + c).toShort()
                val i1 = (r * cols + c + 1).toShort()
                val i2 = ((r + 1) * cols + c).toShort()
                val i3 = ((r + 1) * cols + c + 1).toShort()
                if (flipWinding) {
                    out[k++] = i0
                    out[k++] = i2
                    out[k++] = i1
                    out[k++] = i1
                    out[k++] = i2
                    out[k++] = i3
                } else {
                    out[k++] = i0
                    out[k++] = i1
                    out[k++] = i2
                    out[k++] = i1
                    out[k++] = i3
                    out[k++] = i2
                }
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
        // Seam C (betaSoftLabiaInteriorEdge) constants: ~2x wider outer-edge falloff around labia interior
        internal const val SEAM_C_VEST_ELLIPSE_START = 0.38f
        internal const val SEAM_C_VEST_ELLIPSE_END = 1.74f
        internal const val SEAM_C_WARM_MAJORA_OUTER_X = 0.64f
        internal const val SEAM_C_WARM_MAJORA_Y_OUTER_LO = -0.96f
        internal const val SEAM_C_WARM_MAJORA_Y_OUTER_HI = 0.90f
        internal const val SEAM_C_MAJORA_PIG_OUTER_EXP = 0.18f
        internal const val SEAM_C_MEDIAL_MARGIN_OUTER_SIGMA = 0.20f
        internal const val SEAM_C_FULLNESS_Y_OUTER_LO = -0.96f
        internal const val SEAM_C_FULLNESS_Y_OUTER_HI = 0.94f
        internal const val SEAM_C_GROIN_COLOR_OUTER_SIGMA = 0.150f
        internal const val SEAM_C_GROIN_Y_OUTER_LO = -1.26f
        internal const val SEAM_C_GROIN_Y_OUTER_HI = 1.28f
        internal const val SEAM_C_SULCUS_COLOR_OUTER_SIGMA = 0.056f
        internal const val SEAM_C_SULCUS_Y_OUTER_LO = -0.94f
        internal const val SEAM_C_SULCUS_Y_OUTER_HI = 0.88f

        // Seam E (betaSoftArchEdge) constants: fade labiaHoodArchMesh outer ~20% skirt alpha to 0 + match patch color
        internal const val SEAM_E_ARCH_ALPHA_U_START = 0.71f
        internal const val SEAM_E_ARCH_ALPHA_U_END = 0.91f
        internal const val SEAM_E_ARCH_COLOR_BLEND_END = 0.80f
        internal const val SEAM_E_ARCH_TIP_ELEV_START = 0.0010f
        internal const val SEAM_E_ARCH_TIP_ELEV_END = 0.0060f

        // Vulva Beta v2 Toggle 1: partition vulva patch triangles at edgeR = 0.88 (leaves 0.05 guard band
        // inside Seam D's ovalR = 0.93 cutoff and stays well outside labiaHoodArchMesh/glansMesh <= 0.72).
        internal const val VULVA_PATCH_SEAM_OFFSET_EDGE_R = 0.88f

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
