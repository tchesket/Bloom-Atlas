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
 * High-resolution upper-body anatomical & facial geometry builders for Blossom v4.1:
 *  1. 10 Proportionate Relaxed 3D Fingers & Thumbs (exact outward CCW winding on both hands)
 *  2. Live-Deformable Soft Rounded Medium Breasts (exact outward CCW winding -> exact skin-tone
 *     match with body, natural supine teardrop contour, soft feathered areolae, rounded 3D nipples)
 *  3. Sculptural Realistic Feminine Head & Face (96x88 bottom-to-top outward CCW mesh -> seamless
 *     skin lighting & neck join, calm serene almond blue eyes, soft eyebrows, dainty nose & lips)
 *  4. Glossy Natural Sapphire-Blue 3D Eyes (calm relaxed gaze, upper eyelids grazing upper iris)
 *  5. Stylish Layered Dark-Brown / Soft-Black Pixie Haircut (dimensional crown + 24 sweeping locks)
 */
internal object BlossomUpperBodyBuilder {

    // Classical adult female head (1.14x scale -> naturally balanced shoulder-to-head oval proportion) resting elevated on ergonomic pillow
    const val HEAD_CENTER_Y = 6.42f
    val HEAD_CENTER_Z: Float = BlossomBodyBuilder.mattressSurfaceZ(HEAD_CENTER_Y) + 1.25f
    private const val HEAD_SCALE = 1.14f
    private const val HEAD_PITCH_RAD = 0.09f
    private val COS_PITCH = cos(HEAD_PITCH_RAD)
    private val SIN_PITCH = sin(HEAD_PITCH_RAD)

    // Natural closer-set chest patch with soft cleavage valley (|x| >= 0.065),
    // staying strictly inside the ribcage away from upper arms (|x| <= 1.35), and starting cleanly below the collarbone (y <= 4.53)
    const val BREAST_CENTER_AX = 0.59f
    const val BREAST_CENTER_Y = 3.68f
    const val BREAST_FOOTPRINT_R = 0.76f
    private const val BREAST_MEDIAL_R = 0.525f
    private const val BREAST_LATERAL_R = 0.76f
    private const val BREAST_VERTICAL_R = 0.85f
    const val BREAST_COLS = 64
    const val BREAST_ROWS = 60

    private fun headLocalToWorld(lx: Float, ly: Float, lz: Float): FloatArray {
        val sx = lx * HEAD_SCALE
        val sy = ly * HEAD_SCALE
        val sz = lz * HEAD_SCALE
        val wy = HEAD_CENTER_Y + sy * COS_PITCH - sz * SIN_PITCH
        val wz = HEAD_CENTER_Z + sy * SIN_PITCH + sz * COS_PITCH
        return floatArrayOf(sx, wy, wz)
    }

    private fun headWorldToLocal(wx: Float, wy: Float, wz: Float): FloatArray {
        val dy = wy - HEAD_CENTER_Y
        val dz = wz - HEAD_CENTER_Z
        val sy = dy * COS_PITCH + dz * SIN_PITCH
        val sz = -dy * SIN_PITCH + dz * COS_PITCH
        return floatArrayOf(wx / HEAD_SCALE, sy / HEAD_SCALE, sz / HEAD_SCALE)
    }

    private fun headNormalToWorld(nx: Float, ny: Float, nz: Float): FloatArray {
        val wy = ny * COS_PITCH - nz * SIN_PITCH
        val wz = ny * SIN_PITCH + nz * COS_PITCH
        val len = sqrt(nx * nx + wy * wy + wz * wz).coerceAtLeast(1e-5f)
        return floatArrayOf(nx / len, wy / len, wz / len)
    }

    internal data class BlossomFingerSpec(
        val rootAx: Float,
        val rootY: Float,
        val rootZ: Float,
        val length: Float,
        val rLat: Float,
        val rZ: Float,
        val yawOut: Float,
        val archLift: Float,
        val tipCurlDown: Float,
        val isThumb: Boolean = false
    )

    internal val BLOSSOM_FINGER_SPECS: List<BlossomFingerSpec> by lazy {
        val zPalm = BlossomBodyBuilder.mattressSurfaceZ(0.00f) + 0.185f
        // Proportionate adult feminine hands relative to forearms with soft, relaxed natural finger posture (zero claw arch!)
        listOf(
            BlossomFingerSpec(rootAx = 2.165f, rootY = 0.11f, rootZ = zPalm - 0.012f, length = 0.595f, rLat = 0.077f, rZ = 0.066f, yawOut = -0.42f, archLift = 0.008f, tipCurlDown = 0.009f, isThumb = true),
            BlossomFingerSpec(rootAx = 2.230f, rootY = -0.460f, rootZ = zPalm + 0.007f, length = 0.620f, rLat = 0.074f, rZ = 0.064f, yawOut = -0.020f, archLift = 0.011f, tipCurlDown = 0.012f),
            BlossomFingerSpec(rootAx = 2.370f, rootY = -0.498f, rootZ = zPalm + 0.010f, length = 0.690f, rLat = 0.076f, rZ = 0.066f, yawOut = 0.005f, archLift = 0.013f, tipCurlDown = 0.015f),
            BlossomFingerSpec(rootAx = 2.510f, rootY = -0.465f, rootZ = zPalm + 0.007f, length = 0.615f, rLat = 0.072f, rZ = 0.062f, yawOut = 0.028f, archLift = 0.011f, tipCurlDown = 0.014f),
            BlossomFingerSpec(rootAx = 2.638f, rootY = -0.410f, rootZ = zPalm + 0.002f, length = 0.495f, rLat = 0.065f, rZ = 0.056f, yawOut = 0.055f, archLift = 0.009f, tipCurlDown = 0.012f)
        )
    }

    const val PALM_COLS = 26
    const val PALM_ROWS = 22
    const val FINGER_COLS = 16
    const val FINGER_ROWS = 18
    const val NAIL_COLS = 10
    const val NAIL_ROWS = 8

    internal data class ArticulatedFingerSpine(
        val cx: Float,
        val cy: Float,
        val cz: Float,
        val tanX: Float,
        val tanY: Float,
        val tanZ: Float,
        val latX: Float,
        val latY: Float,
        val latZ: Float,
        val dorsX: Float,
        val dorsY: Float,
        val dorsZ: Float
    )

    /**
     * Evaluates a realistic hinged phalanx skeleton in rest hand space:
     *  - Fingers: 3 straight rigid phalanx bones meeting at crisp MCP (s=0.12), PIP (s=0.48), and DIP (s=0.75) knuckles
     *  - Thumb: 2 straight rigid phalanx bones (CMC/MCP @ s=0.14, IP @ s=0.58) with true opposable pronation roll (~42-56 deg)
     *    so the thumbpad opposes naturally toward the fingers and the thumbnail sits dorso-medially!
     */
    internal fun evaluateArticulatedFingerSpine(
        spec: BlossomFingerSpec,
        side: Float,
        s: Float,
        flex: BlossomArmIkRig.FingerFlexPose
    ): ArticulatedFingerSpine {
        val effectiveYaw = spec.yawOut + flex.splayRad
        val fwdX = side * sin(effectiveYaw)
        val fwdY = -cos(effectiveYaw)
        val baseLatX = side * cos(effectiveYaw)
        val baseLatY = sin(effectiveYaw)

        // Thumb naturally pronates (opposes) around its long axis so its pad faces inward toward the index finger
        val thumbRoll = if (spec.isThumb) flex.rollRad else 0f
        // Moderate opposition flexion plane so thumb curves gently anteriorly/medially without folding sideways into palm
        val flexPlaneRoll = if (spec.isThumb) thumbRoll * 0.28f else 0f
        val cFP = cos(flexPlaneRoll)
        val sFP = sin(flexPlaneRoll)
        // Flexion "down" direction in rest hand space (for fingers: (0,0,-1); for thumb: tilted medially-inward!)
        val flexDownX = baseLatX * sFP
        val flexDownY = baseLatY * sFP
        val flexDownZ = -cFP
        // Orthogonal dorsal axis in the flexion plane
        val flexUpX = -flexDownX
        val flexUpY = -flexDownY
        val flexUpZ = -flexDownZ
        // Orthogonal lateral axis perpendicular to the flexion plane
        val flexLatX = baseLatX * cFP
        val flexLatY = baseLatY * cFP
        val flexLatZ = sFP

        fun jointPitchAt(u: Float): Float {
            return if (spec.isThumb) {
                val cmcW = smoothStep(0.08f, 0.22f, u)
                val ipW = smoothStep(0.51f, 0.65f, u)
                flex.mcpFlexRad * cmcW + flex.pipFlexRad * ipW
            } else {
                // Crisp narrow hinge windows -> straight rigid proximal, middle, & distal phalanx bones!
                val mcpW = smoothStep(0.07f, 0.17f, u)
                val pipW = smoothStep(0.43f, 0.53f, u)
                val dipW = smoothStep(0.70f, 0.80f, u)
                flex.mcpFlexRad * mcpW + flex.pipFlexRad * pipW + flex.dipFlexRad * dipW
            }
        }

        // Integrate rigid-phalanx spine with fine steps so crisp hinges stay accurate
        val alongRoot = if (spec.isThumb) -0.105f else -0.125f
        val totalLen = spec.length - alongRoot
        val steps = 24
        val ds = s / steps

        var cx = side * spec.rootAx + fwdX * alongRoot
        var cy = spec.rootY + fwdY * alongRoot
        var cz = spec.rootZ
        for (k in 0 until steps) {
            val sMid = (k + 0.5f) * ds
            val pitch = jointPitchAt(sMid)
            val segLen = totalLen * ds
            val cosP = cos(pitch)
            val sinP = sin(pitch)
            cx += (fwdX * cosP + flexDownX * sinP) * segLen
            cy += (fwdY * cosP + flexDownY * sinP) * segLen
            cz += (flexDownZ * sinP) * segLen
        }

        // Exact orthonormal (tan, lat, dors) frame at parameter s
        val pitchAtS = jointPitchAt(s)
        val cosP = cos(pitchAtS)
        val sinP = sin(pitchAtS)

        val tanX = fwdX * cosP + flexDownX * sinP
        val tanY = fwdY * cosP + flexDownY * sinP
        val tanZ = flexDownZ * sinP

        val unrolledDorsX = flexUpX * cosP + fwdX * sinP
        val unrolledDorsY = flexUpY * cosP + fwdY * sinP
        val unrolledDorsZ = flexUpZ * cosP

        // Remaining axial pronation roll on the thumb so thumbnail faces dorso-medially and pad opposes the fingers
        val remRoll = thumbRoll - flexPlaneRoll
        val cR = cos(remRoll)
        val sR = sin(remRoll)
        val latX = flexLatX * cR + unrolledDorsX * sR
        val latY = flexLatY * cR + unrolledDorsY * sR
        val latZ = flexLatZ * cR + unrolledDorsZ * sR
        val dorsX = unrolledDorsX * cR - flexLatX * sR
        val dorsY = unrolledDorsY * cR - flexLatY * sR
        val dorsZ = unrolledDorsZ * cR - flexLatZ * sR

        val restArchScale = 1f - smoothStep(0f, 0.45f, abs(flex.pipFlexRad) + abs(flex.mcpFlexRad)) * 0.72f
        val restArchZ = (spec.archLift * sin(s * PI.toFloat()) - spec.tipCurlDown * smoothStep(0.36f, 1.0f, s)) * restArchScale
        val medialCurve = if (spec.isThumb) {
            0.018f * smoothStep(0.28f, 1.0f, s)
        } else {
            -0.010f * smoothStep(0.32f, 1.0f, s)
        }

        cx += baseLatX * medialCurve
        cy += baseLatY * medialCurve
        cz += restArchZ

        return ArticulatedFingerSpine(
            cx = cx, cy = cy, cz = cz,
            tanX = tanX, tanY = tanY, tanZ = tanZ,
            latX = latX, latY = latY, latZ = latZ,
            dorsX = dorsX, dorsY = dorsY, dorsZ = dorsZ
        )
    }

    private fun fingerTipStartS(isThumb: Boolean): Float = if (isThumb) 0.905f else 0.915f

    internal fun fingerRowParameterS(r: Int, rows: Int, isThumb: Boolean): Float {
        val tipStart = fingerTipStartS(isThumb)
        val shaftLastRow = rows - 6 // 5 smooth hemisphere cap rings across the rounded fingertip
        if (r <= shaftLastRow) {
            return (r.toFloat() / shaftLastRow) * tipStart
        }
        val capAnglesDeg = floatArrayOf(20f, 40f, 60f, 78f, 90f)
        val deg = capAnglesDeg[(r - shaftLastRow - 1).coerceIn(0, capAnglesDeg.lastIndex)]
        val rad = deg * (PI.toFloat() / 180f)
        return tipStart + sin(rad) * (1f - tipStart)
    }

    internal fun fingerRadiusProfile(s: Float, isThumb: Boolean = false): Pair<Float, Float> {
        // True plump hemispherical rounded human fingertip dome (never pointy/cone-tipped!)
        val tipStart = fingerTipStartS(isThumb)
        val tipAngle: Float
        val tipScale: Float
        if (s <= tipStart) {
            tipAngle = 0f
            tipScale = 1f
        } else {
            val uCap = ((s - tipStart) / (1f - tipStart)).coerceIn(0f, 1f)
            tipAngle = kotlin.math.asin(uCap)
            tipScale = sqrt((1f - uCap * uCap).coerceAtLeast(0f)).coerceAtLeast(0.012f)
        }

        val rootFlare = if (isThumb) {
            1f + 0.24f * (1f - smoothStep(0f, 0.30f, s))
        } else {
            1f + 0.16f * (1f - smoothStep(0f, 0.22f, s))
        }
        val distalTaper = 1f - (if (isThumb) 0.10f else 0.12f) * smoothStep(0.14f, 0.88f, s)
        val jointProfile = if (isThumb) {
            // Single sculpted thumb IP knuckle at s=0.58 + fleshy distal thumbpad
            val ipJoint = 1f + 0.082f * exp(-((s - 0.58f) / 0.068f).let { it * it })
            val padFull = 1f + 0.060f * exp(-((s - 0.81f) / 0.11f).let { it * it })
            ipJoint * padFull
        } else {
            // Distinct anatomical PIP (s=0.48) & DIP (s=0.75) knuckles + plump rounded distal fingertip pad
            val pipJoint = 1f + 0.092f * exp(-((s - 0.48f) / 0.058f).let { it * it })
            val dipJoint = 1f + 0.060f * exp(-((s - 0.75f) / 0.052f).let { it * it })
            val padFull = 1f + 0.056f * exp(-((s - 0.86f) / 0.09f).let { it * it })
            pipJoint * dipJoint * padFull
        }
        return Pair(rootFlare * distalTaper * jointProfile * tipScale, tipAngle)
    }

    // -------------------------------------------------------------------------
    // 1A. HIGH-RESOLUTION SCULPTED PARAMETRIC PALMS (26x22 PER HAND, OUTWARD CCW)
    //     Replaces coarse voxelized SDF palm lumps with flat anatomical dorsum,
    //     4 delicate MCP knuckles, concave palmar cup, thenar/hypothenar pads,
    //     and seamless wrist-collar blending!
    // -------------------------------------------------------------------------
    private class PalmRestCache(
        val restData: FloatArray,
        val sides: FloatArray,
        val wristBlend: FloatArray
    )

    private val palmRestCache: PalmRestCache by lazy { buildPalmRestCache() }

    private fun buildPalmRestCache(): PalmRestCache {
        val cols = PALM_COLS
        val rows = PALM_ROWS
        val vertsPerPalm = cols * rows
        val totalVerts = vertsPerPalm * 2
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val restData = FloatArray(totalVerts * stride)
        val sideArr = FloatArray(totalVerts)
        val wristBlendArr = FloatArray(totalVerts)

        val zWrist = BlossomBodyBuilder.mattressSurfaceZ(0.42f) + 0.195f
        val zPalm = BlossomBodyBuilder.mattressSurfaceZ(0.00f) + 0.185f
        val floorZ = BlossomBodyBuilder.MATTRESS_TOP_Z + 0.015f
        val sides = floatArrayOf(-1f, +1f)
        val sideBuf = FloatArray(vertsPerPalm * stride)

        for ((handIdx, side) in sides.withIndex()) {
            val baseVert = handIdx * vertsPerPalm
            for (r in 0 until rows) {
                val s = r.toFloat() / (rows - 1)
                val capT = smoothStep(0.84f, 1.0f, s)
                val capAngle = capT * (PI.toFloat() * 0.5f)
                val capScale = cos(capAngle).coerceAtLeast(0.03f)
                val wristTuck = 0.96f + 0.04f * smoothStep(0.0f, 0.16f, s)

                val cxAx = 2.345f + 0.095f * smoothStep(0.0f, 0.85f, s)
                val cyBase = 0.48f - s * 1.035f
                val czBase = zWrist * (1f - s) + (zPalm + 0.004f) * s

                // Proportionate adult feminine palm silhouette (wrist locked flush to forearm -> fuller natural metacarpal fan -> rounded knuckle web)
                val wristPinch = -0.006f * exp(-((s - 0.12f) / 0.10f).let { it * it })
                val halfW = (0.226f + wristPinch + 0.088f * smoothStep(0.12f, 0.66f, s) - 0.024f * smoothStep(0.72f, 0.94f, s)) * wristTuck * capScale
                val halfZ = (0.148f - 0.018f * smoothStep(0.06f, 0.44f, s) - 0.012f * smoothStep(0.56f, 0.88f, s)) * wristTuck * capScale
                val flatMix = smoothStep(0.16f, 0.46f, s)

                for (c in 0 until cols) {
                    val u = c.toFloat() / (cols - 1)
                    val phi = u * (2f * PI.toFloat())
                    val cosPhi = cos(phi)
                    val sinPhi = sin(phi)

                    val cSign = if (cosPhi >= 0f) 1f else -1f
                    val sSign = if (sinPhi >= 0f) 1f else -1f
                    val sqW = cSign * Math.pow(abs(cosPhi).toDouble().coerceAtLeast(1e-5), 0.90).toFloat()
                    val sqZ = sSign * Math.pow(abs(sinPhi).toDouble().coerceAtLeast(1e-5), 0.91).toFloat()
                    val latShape = cosPhi * (1f - flatMix) + sqW * flatMix
                    val zShape = sinPhi * (1f - flatMix) + sqZ * flatMix

                    var localLat = halfW * latShape
                    var localZ = halfZ * zShape
                    val palmEnv = (1f - capT) * smoothStep(0.04f, 0.22f, s)

                    // 1. Natural curved distal MCP knuckle arch aligned cleanly with index-middle-ring-pinky roots (zero pinky notch!)
                    val archDist = cosPhi + 0.18f
                    val knuckleSlantY = (0.018f * archDist * archDist + 0.014f * cosPhi + 0.022f * smoothStep(0.25f, 0.95f, cosPhi)) *
                        smoothStep(0.46f, 0.94f, s)

                    // 2. Sculpted fleshy Thenar Eminence + smooth 1st interosseous (thumb-to-index) web bridge
                    val thenarS = exp(-((s - 0.36f) / 0.19f).let { it * it })
                    val thenarRad = smoothStep(-0.08f, -0.82f, cosPhi)
                    val thenarPalmar = smoothStep(0.10f, -0.78f, sinPhi)
                    val thumbWebS = exp(-((s - 0.62f) / 0.17f).let { it * it })
                    val thumbWebRad = smoothStep(-0.32f, -0.92f, cosPhi)
                    val thumbWebBridge = thumbWebS * thumbWebRad * palmEnv
                    localLat -= (0.032f * thenarS * thenarRad + 0.020f * thumbWebS * thumbWebRad) * palmEnv
                    localZ -= 0.026f * thenarS * thenarRad * thenarPalmar * palmEnv
                    // Gentle dorsal hollow over the 1st dorsal interosseous web between thumb and index metacarpals
                    localZ -= 0.0042f * thumbWebBridge * smoothStep(0.12f, 0.85f, sinPhi)

                    // 3. Sculpted Hypothenar heel pad (ulnar-palmar border) & distal metacarpal ball
                    val hypoS = exp(-((s - 0.40f) / 0.22f).let { it * it })
                    val hypoUlnar = smoothStep(0.18f, 0.84f, cosPhi) * smoothStep(0.05f, -0.75f, sinPhi)
                    val distalBall = exp(-((s - 0.78f) / 0.11f).let { it * it }) * smoothStep(-0.10f, -0.82f, sinPhi) *
                        (1f - smoothStep(0.75f, 1.0f, abs(cosPhi)))
                    localZ -= (0.016f * hypoS * hypoUlnar + 0.015f * distalBall) * palmEnv

                    // 4. Subtle concave palmar hollow framed between thenar, hypothenar, and metacarpal ball
                    val palmCup = exp(-((s - 0.56f) / 0.18f).let { it * it }) *
                        exp(-(cosPhi / 0.46f).let { it * it }) *
                        smoothStep(-0.25f, -0.88f, sinPhi)
                    localZ += 0.009f * palmCup * palmEnv

                    // 5. Sleek smooth back of hand (dorsum) with 4 delicate MCP knuckle hills, scalloped interdigital valleys & subtle extensor tendons
                    val dorsMask = smoothStep(0.22f, 0.88f, sinPhi) * palmEnv
                    var interdigitalValley = 0f
                    if (dorsMask > 0.001f) {
                        localZ -= 0.007f * dorsMask * smoothStep(0.22f, 0.72f, s)
                        val kCenters = floatArrayOf(-0.62f, -0.18f, 0.24f, 0.64f)
                        val vCenters = floatArrayOf(-0.40f, 0.03f, 0.44f)
                        val kRowEnv = exp(-((s - 0.78f) / 0.088f).let { it * it })
                        val valleyEnv = exp(-((s - 0.82f) / 0.095f).let { it * it })
                        val tendonSpan = smoothBell(s, 0.26f, 0.78f, 0.10f)
                        var knuckleLift = 0f
                        var tendonLift = 0f
                        for (kc in kCenters) {
                            val d = (cosPhi - kc) / 0.165f
                            val g = exp(-(d * d))
                            knuckleLift += g * kRowEnv
                            tendonLift += exp(-((cosPhi - kc) / 0.080f).let { it * it }) * tendonSpan
                        }
                        for (vc in vCenters) {
                            val dv = (cosPhi - vc) / 0.115f
                            interdigitalValley += exp(-(dv * dv)) * valleyEnv
                        }
                        localZ += (0.0095f * knuckleLift + 0.0030f * tendonLift - 0.0036f * interdigitalValley) * dorsMask
                    }

                    val px = side * (cxAx + localLat)
                    val py = cyBase + knuckleSlantY
                    var pz = czBase + localZ
                    if (pz < floorZ) pz = floorZ

                    val palmAo = (1f - 0.14f * palmCup - 0.08f * smoothStep(0.20f, 0.90f, -sinPhi) -
                        0.065f * interdigitalValley * dorsMask - 0.045f * thumbWebBridge * smoothStep(0.05f, 0.80f, sinPhi)).coerceIn(0.72f, 1f)
                    val palmPigment = (0.04f + 0.038f * smoothStep(0.30f, 0.90f, sinPhi) * smoothStep(0.65f, 0.86f, s)).coerceIn(0f, 0.20f)

                    val lIdx = (r * cols + c) * stride
                    sideBuf[lIdx] = px
                    sideBuf[lIdx + 1] = py
                    sideBuf[lIdx + 2] = pz
                    sideBuf[lIdx + 6] = 0.0f // skin
                    sideBuf[lIdx + 7] = 0.042f // satin skin sheen
                    sideBuf[lIdx + 8] = palmAo
                    sideBuf[lIdx + 9] = palmPigment

                    val globalVert = baseVert + r * cols + c
                    sideArr[globalVert] = side
                    // Match exact wrist blend window of BlossomArmIkRig (WRIST_Y +- 0.28) so wrist collar NEVER separates!
                    wristBlendArr[globalVert] = smoothStep(BlossomArmIkRig.WRIST_Y + 0.28f, BlossomArmIkRig.WRIST_Y - 0.28f, py)
                }
            }

            // Compute smooth wrapped normals (dP/dc x dP/dr is outward for side=+1, inward for side=-1)
            val flipSign = if (side < 0f) -1f else 1f
            for (r in 0 until rows) {
                val rPrev = max(0, r - 1)
                val rNext = min(rows - 1, r + 1)
                val s = r.toFloat() / (rows - 1)
                val wristNormBlend = 1f - smoothStep(0.0f, 0.24f, s)
                for (c in 0 until cols) {
                    val cPrev = if (c == 0) cols - 2 else c - 1
                    val cNext = if (c == cols - 1) 1 else c + 1
                    val iL = (r * cols + cPrev) * stride
                    val iR = (r * cols + cNext) * stride
                    val iD = (rPrev * cols + c) * stride
                    val iU = (rNext * cols + c) * stride

                    val tx = sideBuf[iR] - sideBuf[iL]
                    val ty = sideBuf[iR + 1] - sideBuf[iL + 1]
                    val tz = sideBuf[iR + 2] - sideBuf[iL + 2]
                    val bx = sideBuf[iU] - sideBuf[iD]
                    val by = sideBuf[iU + 1] - sideBuf[iD + 1]
                    val bz = sideBuf[iU + 2] - sideBuf[iD + 2]

                    var nx = (ty * bz - tz * by) * flipSign
                    var ny = (tz * bx - tx * bz) * flipSign
                    var nz = (tx * by - ty * bx) * flipSign
                    val len = sqrt(nx * nx + ny * ny + nz * nz)
                    if (len > 1e-5f) {
                        nx /= len; ny /= len; nz /= len
                    } else {
                        nx = 0f; ny = -1f; nz = 0f
                    }

                    val lIdx = (r * cols + c) * stride
                    if (wristNormBlend > 0.001f) {
                        val sdfN = BlossomSdfBodyBuilder.evaluateBodyNormal(sideBuf[lIdx], sideBuf[lIdx + 1], sideBuf[lIdx + 2], eps = 0.018f)
                        val mx = nx * (1f - wristNormBlend) + sdfN[0] * wristNormBlend
                        val my = ny * (1f - wristNormBlend) + sdfN[1] * wristNormBlend
                        val mz = nz * (1f - wristNormBlend) + sdfN[2] * wristNormBlend
                        val mLen = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                        nx = mx / mLen; ny = my / mLen; nz = mz / mLen
                    }
                    sideBuf[lIdx + 3] = nx
                    sideBuf[lIdx + 4] = ny
                    sideBuf[lIdx + 5] = nz
                }
            }

            System.arraycopy(sideBuf, 0, restData, baseVert * stride, sideBuf.size)
        }

        return PalmRestCache(restData = restData, sides = sideArr, wristBlend = wristBlendArr)
    }

    fun buildBlossomPalmsMesh(): DynamicGlMesh {
        val cols = PALM_COLS
        val rows = PALM_ROWS
        val vertsPerPalm = cols * rows
        val trisPerPalm = (cols - 1) * (rows - 1) * 6
        val mesh = DynamicGlMesh(vertsPerPalm * 2, trisPerPalm * 2)
        val indices = ShortArray(trisPerPalm * 2)
        val sides = floatArrayOf(-1f, +1f)
        var idxOut = 0
        for ((handIdx, side) in sides.withIndex()) {
            val baseVert = handIdx * vertsPerPalm
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
        }
        mesh.setIndices(indices)
        val leftRest = BlossomArmIkRig.solveSideArmIk(-1f, 0f, 0f)
        val rightRest = BlossomArmIkRig.solveSideArmIk(+1f, 0f, 0f)
        updateArticulatedPalmsMesh(mesh, leftRest, rightRest)
        return mesh
    }

    /**
     * Real-time 60 FPS update of Blossom's sculpted high-resolution parametric palms,
     * seamlessly matching the volume-preserving wrist blend of [BlossomArmIkRig].
     */
    fun updateArticulatedPalmsMesh(
        palmsMesh: DynamicGlMesh,
        leftSol: BlossomArmIkRig.SideArmIkSolution,
        rightSol: BlossomArmIkRig.SideArmIkSolution
    ) {
        val cache = palmRestCache
        val src = cache.restData
        val dst = palmsMesh.vertexData
        val sides = cache.sides
        val wHand = cache.wristBlend
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val totalVerts = palmsMesh.vertexCount

        if (leftSol.poseProgress <= 0.0001f && rightSol.poseProgress <= 0.0001f) {
            System.arraycopy(src, 0, dst, 0, src.size)
            palmsMesh.commitVertices()
            return
        }

        val leftTwistedForearm = leftSol.poseForearm.rotatedAroundY(leftSol.wristTwistRad)
        val rightTwistedForearm = rightSol.poseForearm.rotatedAroundY(rightSol.wristTwistRad)

        for (i in 0 until totalVerts) {
            val base = i * stride
            val px = src[base]; val py = src[base + 1]; val pz = src[base + 2]
            val nx = src[base + 3]; val ny = src[base + 4]; val nz = src[base + 5]
            val sol = if (sides[i] < 0f) leftSol else rightSol
            val twistedForearm = if (sides[i] < 0f) leftTwistedForearm else rightTwistedForearm
            val tHand = wHand[i]

            if (sol.poseProgress <= 0.0001f) {
                dst[base] = px; dst[base + 1] = py; dst[base + 2] = pz
                dst[base + 3] = nx; dst[base + 4] = ny; dst[base + 5] = nz
            } else if (tHand < 0.999f) {
                val (posW, normW) = BlossomArmIkRig.applyBlendedJointTransform(
                    px, py, pz, nx, ny, nz,
                    restPivot = sol.restHand.origin,
                    posePivot = sol.wrist,
                    restA = sol.restForearm,
                    poseA = twistedForearm,
                    restB = sol.restHand,
                    poseB = sol.poseHand,
                    t = tHand
                )
                val nLen = normW.len().coerceAtLeast(1e-5f)
                dst[base] = posW.x; dst[base + 1] = posW.y; dst[base + 2] = posW.z
                dst[base + 3] = normW.x / nLen; dst[base + 4] = normW.y / nLen; dst[base + 5] = normW.z / nLen
            } else {
                val lp = sol.restHand.worldToLocalPoint(px, py, pz)
                val wp = sol.poseHand.localToWorldPoint(lp.x, lp.y, lp.z)
                val ln = sol.restHand.worldToLocalDir(nx, ny, nz)
                val wn = sol.poseHand.localToWorldDir(ln.x, ln.y, ln.z)
                val nLen = wn.len().coerceAtLeast(1e-5f)
                dst[base] = wp.x; dst[base + 1] = wp.y; dst[base + 2] = wp.z
                dst[base + 3] = wn.x / nLen; dst[base + 4] = wn.y / nLen; dst[base + 5] = wn.z / nLen
            }
            dst[base + 6] = src[base + 6]
            dst[base + 7] = src[base + 7]
            dst[base + 8] = src[base + 8]
            dst[base + 9] = src[base + 9]
        }
        palmsMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // 1B. 10 PROPORTIONATE ARTICULATED 3D FINGERS & THUMBS (OUTWARD CCW BOTH HANDS)
    // -------------------------------------------------------------------------
    fun buildBlossomHandsMesh(): DynamicGlMesh {
        val fingerSpecs = BLOSSOM_FINGER_SPECS
        val cols = FINGER_COLS
        val rows = FINGER_ROWS
        val vertsPerFinger = cols * rows
        val trisPerFinger = (cols - 1) * (rows - 1) * 6
        val totalFingers = fingerSpecs.size * 2

        val mesh = DynamicGlMesh(vertsPerFinger * totalFingers, trisPerFinger * totalFingers)
        val indices = ShortArray(trisPerFinger * totalFingers)

        var fingerIdx = 0
        var idxOut = 0
        val sides = floatArrayOf(-1f, +1f)
        for (side in sides) {
            for (specIdx in fingerSpecs.indices) {
                val baseVert = fingerIdx * vertsPerFinger
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
                fingerIdx++
            }
        }
        mesh.setIndices(indices)

        // Populate resting hand geometry via the shared articulated solver
        val leftRest = BlossomArmIkRig.solveSideArmIk(-1f, 0f, 0f)
        val rightRest = BlossomArmIkRig.solveSideArmIk(+1f, 0f, 0f)
        updateArticulatedHandsInternal(mesh, leftRest, rightRest)
        return mesh
    }

    /**
     * Real-time 60 FPS update of all 10 multi-joint articulated fingers and 10 matching glossy
     * 3D fingernails driven by Blossom's 2-bone arm IK solution.
     */
    fun updateArticulatedHandsAndFingernails(
        handsMesh: DynamicGlMesh,
        nailsMesh: DynamicGlMesh,
        leftSol: BlossomArmIkRig.SideArmIkSolution,
        rightSol: BlossomArmIkRig.SideArmIkSolution
    ) {
        updateArticulatedHandsInternal(handsMesh, leftSol, rightSol)
        updateArticulatedFingernailsInternal(nailsMesh, leftSol, rightSol)
    }

    private fun updateArticulatedHandsInternal(
        mesh: DynamicGlMesh,
        leftSol: BlossomArmIkRig.SideArmIkSolution,
        rightSol: BlossomArmIkRig.SideArmIkSolution
    ) {
        val floorZ = BlossomBodyBuilder.MATTRESS_TOP_Z + 0.015f
        val fingerSpecs = BLOSSOM_FINGER_SPECS
        val cols = FINGER_COLS
        val rows = FINGER_ROWS
        val vertsPerFinger = cols * rows
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val sides = floatArrayOf(-1f, +1f)

        var fingerIdx = 0
        for (side in sides) {
            val sol = if (side < 0f) leftSol else rightSol
            val isResting = sol.poseProgress <= 0.0001f
            val floorClampW = 1f - smoothStep(0f, 0.10f, sol.poseProgress)

            for (specIdx in fingerSpecs.indices) {
                val spec = fingerSpecs[specIdx]
                val baseVert = fingerIdx * vertsPerFinger
                val flex = BlossomArmIkRig.fingerFlexForIndex(specIdx, sol.fingerCurlWeight)

                for (r in 0 until rows) {
                    val s = fingerRowParameterS(r, rows, spec.isThumb)
                    val (radScale, tipAngle) = fingerRadiusProfile(s, spec.isThumb)
                    val spine = evaluateArticulatedFingerSpine(spec, side, s, flex)
                    val radialW = cos(tipAngle)
                    val fwdW = sin(tipAngle)
                    val rootBlend = 1f - smoothStep(0f, 0.24f, s)

                    // Sculpted anatomical dorsal knuckle contours aligned to exact rigid hinge centers
                    val pipCrease = if (spec.isThumb) {
                        exp(-((s - 0.58f) / 0.038f).let { it * it })
                    } else {
                        exp(-((s - 0.48f) / 0.036f).let { it * it })
                    }
                    val dipCrease = if (spec.isThumb) {
                        0f
                    } else {
                        exp(-((s - 0.75f) / 0.032f).let { it * it })
                    }

                    for (c in 0 until cols) {
                        val u = c.toFloat() / (cols - 1)
                        val phi = u * (2f * PI.toFloat())
                        val cosPhi = cos(phi)
                        val sinPhi = sin(phi)

                        val nailMask = smoothBell(s, 0.78f, 0.95f, 0.04f) * smoothStep(0.45f, 0.92f, sinPhi) *
                            (1f - smoothStep(0.55f, 0.88f, abs(cosPhi)))
                        val nailIndent = -0.0028f * nailMask

                        // Subtle anatomical D/squircle phalanx cross-section (gently flatter lateral/dorsal planes + plump palmar fingertip pads)
                        val sectionW = smoothStep(0.08f, 0.24f, s) * (1f - smoothStep(0.84f, 0.96f, s))
                        val cSign = if (cosPhi >= 0f) 1f else -1f
                        val sqCos = cSign * Math.pow(abs(cosPhi).toDouble().coerceAtLeast(1e-5), 0.92).toFloat()
                        val shapedCos = cosPhi * (1f - 0.36f * sectionW) + sqCos * (0.36f * sectionW)
                        val shaftFlat = 1f - 0.045f * sectionW * (1f - smoothStep(0.72f, 0.86f, s))

                        val localLat = spec.rLat * radScale * shapedCos
                        var localDors = spec.rZ * radScale * sinPhi + nailIndent
                        if (sinPhi < 0f) {
                            // Softer fleshy pad on distal fingertip, slightly flatter palmar surface on proximal phalanx
                            val padPlump = 0.89f + 0.16f * smoothStep(0.70f, 0.94f, s)
                            localDors *= padPlump
                        } else {
                            // Gently flattened dorsal phalanx shaft + distinct dorsal bony knuckle prominence at PIP/IP & DIP
                            val bendBoost = 1f + 0.55f * (abs(flex.pipFlexRad) / 1.4f).coerceIn(0f, 1f)
                            localDors = localDors * shaftFlat + (0.0056f * pipCrease * bendBoost + 0.0035f * dipCrease * bendBoost) * sinPhi
                        }

                        var px = spine.cx + spine.latX * localLat + spine.dorsX * localDors
                        var py = spine.cy + spine.latY * localLat + spine.dorsY * localDors
                        var pz = spine.cz + spine.latZ * localLat + spine.dorsZ * localDors
                        if (floorClampW > 0.001f && pz < floorZ) {
                            pz = pz + (floorZ - pz) * floorClampW
                        }

                        var nx = spine.latX * (cosPhi * radialW / spec.rLat) +
                            spine.dorsX * (sinPhi * radialW / spec.rZ) +
                            spine.tanX * (fwdW / spec.rLat)
                        var ny = spine.latY * (cosPhi * radialW / spec.rLat) +
                            spine.dorsY * (sinPhi * radialW / spec.rZ) +
                            spine.tanY * (fwdW / spec.rLat)
                        var nz = spine.latZ * (cosPhi * radialW / spec.rLat) +
                            spine.dorsZ * (sinPhi * radialW / spec.rZ) +
                            spine.tanZ * (fwdW / spec.rLat)
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen

                        // Seamless normal fusion into unified SDF palm at knuckle root
                        if (rootBlend > 0.001f) {
                            val sdfNorm = BlossomSdfBodyBuilder.evaluateBodyNormal(px, py, pz, eps = 0.016f)
                            val mx = nx * (1f - rootBlend) + sdfNorm[0] * rootBlend
                            val my = ny * (1f - rootBlend) + sdfNorm[1] * rootBlend
                            val mz = nz * (1f - rootBlend) + sdfNorm[2] * rootBlend
                            val mLen = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                            nx = mx / mLen; ny = my / mLen; nz = mz / mLen
                        }

                        // Map cleanly from rest hand frame into live posed 2-bone IK hand frame!
                        if (!isResting) {
                            val lp = sol.restHand.worldToLocalPoint(px, py, pz)
                            val wp = sol.poseHand.localToWorldPoint(lp.x, lp.y, lp.z)
                            val ln = sol.restHand.worldToLocalDir(nx, ny, nz)
                            val wn = sol.poseHand.localToWorldDir(ln.x, ln.y, ln.z)
                            val wnLen = wn.len().coerceAtLeast(1e-5f)
                            px = wp.x; py = wp.y; pz = wp.z
                            nx = wn.x / wnLen; ny = wn.y / wnLen; nz = wn.z / wnLen
                        }

                        val interdigitalAo = 1f - abs(cosPhi) * 0.22f * (1f - smoothStep(0.08f, 0.62f, s))
                        val padAo = 1f - smoothStep(0.10f, 0.85f, -sinPhi) * 0.26f
                        val knuckleAo = 1f - (0.10f * pipCrease + 0.08f * dipCrease) * smoothStep(0.22f, 0.90f, sinPhi) -
                            (0.08f * pipCrease + 0.06f * dipCrease) * smoothStep(0.22f, 0.85f, -sinPhi)
                        val ao = (interdigitalAo * padAo * knuckleAo).coerceIn(0.40f, 1f)
                        val wetness = 0.052f
                        val pigment = (0.05f + smoothStep(0.68f, 0.96f, s) * 0.085f +
                            (pipCrease * 0.06f + dipCrease * 0.045f) * smoothStep(0.2f, 0.9f, sinPhi)).coerceIn(0f, 1f)

                        val vIdx = (baseVert + r * cols + c) * stride
                        data[vIdx] = px; data[vIdx + 1] = py; data[vIdx + 2] = pz
                        data[vIdx + 3] = nx; data[vIdx + 4] = ny; data[vIdx + 5] = nz
                        data[vIdx + 6] = nailMask * 0.16f
                        data[vIdx + 7] = wetness
                        data[vIdx + 8] = ao
                        data[vIdx + 9] = pigment
                    }
                }
                fingerIdx++
            }
        }
        mesh.commitVertices()
    }

    private fun updateArticulatedFingernailsInternal(
        nailsMesh: DynamicGlMesh,
        leftSol: BlossomArmIkRig.SideArmIkSolution,
        rightSol: BlossomArmIkRig.SideArmIkSolution
    ) {
        val nailCols = NAIL_COLS
        val nailRows = NAIL_ROWS
        val vertsPerNail = nailCols * nailRows
        val fingerSpecs = BLOSSOM_FINGER_SPECS
        val data = nailsMesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val sides = floatArrayOf(-1f, +1f)

        var nailIdx = 0
        for (side in sides) {
            val sol = if (side < 0f) leftSol else rightSol
            val isResting = sol.poseProgress <= 0.0001f

            for (specIdx in fingerSpecs.indices) {
                val spec = fingerSpecs[specIdx]
                val baseVert = nailIdx * vertsPerNail
                val flex = BlossomArmIkRig.fingerFlexForIndex(specIdx, sol.fingerCurlWeight)

                for (r in 0 until nailRows) {
                    val v = r.toFloat() / (nailRows - 1)
                    // Natural short oval fingernails hugging flush onto the distal nailbed (ZERO pointy claw overhang!)
                    val s = if (spec.isThumb) 0.76f + v * 0.185f else 0.775f + v * 0.175f
                    val (radScale, tipAngle) = fingerRadiusProfile(s, spec.isThumb)
                    val spine = evaluateArticulatedFingerSpine(spec, side, s, flex)

                    val vCentered = v * 2f - 1f
                    val almondWidth = sqrt((1f - vCentered * vCentered * 0.34f).coerceAtLeast(0.32f))
                    val nailArcHalf = if (spec.isThumb) 0.36f * PI.toFloat() else 0.31f * PI.toFloat()
                    val radialW = cos(tipAngle * 0.60f)
                    val fwdW = sin(tipAngle * 0.60f)

                    for (c in 0 until nailCols) {
                        val u = c.toFloat() / (nailCols - 1)
                        val uCentered = u * 2f - 1f
                        val phi = (PI.toFloat() * 0.5f) - uCentered * nailArcHalf * almondWidth
                        val cosPhi = cos(phi)
                        val sinPhi = sin(phi)

                        var nx = spine.latX * (cosPhi * radialW / spec.rLat) +
                            spine.dorsX * (sinPhi * radialW / spec.rZ) +
                            spine.tanX * (fwdW / spec.rLat)
                        var ny = spine.latY * (cosPhi * radialW / spec.rLat) +
                            spine.dorsY * (sinPhi * radialW / spec.rZ) +
                            spine.tanY * (fwdW / spec.rLat)
                        var nz = spine.latZ * (cosPhi * radialW / spec.rLat) +
                            spine.dorsZ * (sinPhi * radialW / spec.rZ) +
                            spine.tanZ * (fwdW / spec.rLat)
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen

                        val sectionW = smoothStep(0.08f, 0.24f, s) * (1f - smoothStep(0.84f, 0.96f, s))
                        val cSign = if (cosPhi >= 0f) 1f else -1f
                        val sqCos = cSign * Math.pow(abs(cosPhi).toDouble().coerceAtLeast(1e-5), 0.92).toFloat()
                        val shapedCos = cosPhi * (1f - 0.36f * sectionW) + sqCos * (0.36f * sectionW)
                        val shaftFlat = 1f - 0.045f * sectionW * (1f - smoothStep(0.72f, 0.86f, s))

                        val edgeBevel = sin(v * PI.toFloat()).coerceAtLeast(0.15f) * cos(uCentered * PI.toFloat() * 0.46f)
                        val plateLift = 0.0058f * (0.48f + 0.52f * edgeBevel)
                        val localLat = spec.rLat * radScale * shapedCos
                        val localDors = spec.rZ * radScale * sinPhi * shaftFlat

                        var px = spine.cx + spine.latX * localLat + spine.dorsX * localDors + nx * plateLift
                        var py = spine.cy + spine.latY * localLat + spine.dorsY * localDors + ny * plateLift
                        var pz = spine.cz + spine.latZ * localLat + spine.dorsZ * localDors + nz * plateLift

                        if (!isResting) {
                            val lp = sol.restHand.worldToLocalPoint(px, py, pz)
                            val wp = sol.poseHand.localToWorldPoint(lp.x, lp.y, lp.z)
                            val ln = sol.restHand.worldToLocalDir(nx, ny, nz)
                            val wn = sol.poseHand.localToWorldDir(ln.x, ln.y, ln.z)
                            val wnLen = wn.len().coerceAtLeast(1e-5f)
                            px = wp.x; py = wp.y; pz = wp.z
                            nx = wn.x / wnLen; ny = wn.y / wnLen; nz = wn.z / wnLen
                        }

                        val nailAo = (0.86f + 0.14f * cos(uCentered * PI.toFloat() * 0.45f) -
                            0.14f * (1f - smoothStep(0f, 0.20f, v))).coerceIn(0.62f, 1f)

                        val vIdx = (baseVert + r * nailCols + c) * stride
                        data[vIdx] = px; data[vIdx + 1] = py; data[vIdx + 2] = pz
                        data[vIdx + 3] = nx; data[vIdx + 4] = ny; data[vIdx + 5] = nz
                        data[vIdx + 6] = v
                        data[vIdx + 7] = u
                        data[vIdx + 8] = nailAo
                        data[vIdx + 9] = 0.0f // fingernail
                    }
                }
                nailIdx++
            }
        }
        nailsMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // 1B. 20 SCULPTED 3D KERATIN FINGERNAILS (10) & TOENAILS (10) — OUTWARD CCW!
    //     Returns Pair(nailsMesh, toeNailStartVert) so fingernails track arms & toenails track legs!
    // -------------------------------------------------------------------------
    fun buildBlossomNailsMesh(): Pair<DynamicGlMesh, Int> {
        val nailCols = NAIL_COLS
        val nailRows = NAIL_ROWS
        val vertsPerNail = nailCols * nailRows
        val trisPerNail = (nailCols - 1) * (nailRows - 1) * 6

        val fingerSpecs = BLOSSOM_FINGER_SPECS
        val toeSpecs = BlossomSdfBodyBuilder.BLOSSOM_TOE_SPECS
        val totalNails = (fingerSpecs.size + toeSpecs.size) * 2
        val toeNailStartVert = fingerSpecs.size * 2 * vertsPerNail

        val mesh = DynamicGlMesh(vertsPerNail * totalNails, trisPerNail * totalNails)
        val indices = ShortArray(trisPerNail * totalNails)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val sides = floatArrayOf(-1f, +1f)

        var nailIdx = 0
        var idxOut = 0

        fun emitNailIndices(baseVert: Int, side: Float) {
            for (r in 0 until nailRows - 1) {
                for (c in 0 until nailCols - 1) {
                    val i0 = (baseVert + r * nailCols + c).toShort()
                    val i1 = (baseVert + r * nailCols + c + 1).toShort()
                    val i2 = (baseVert + (r + 1) * nailCols + c).toShort()
                    val i3 = (baseVert + (r + 1) * nailCols + c + 1).toShort()
                    // d/dc = +lat, d/dr = +fwd -> side=+1 needs fwd x lat (+Z); side=-1 needs lat x fwd (+Z)
                    if (side > 0f) {
                        indices[idxOut++] = i0; indices[idxOut++] = i2; indices[idxOut++] = i1
                        indices[idxOut++] = i1; indices[idxOut++] = i2; indices[idxOut++] = i3
                    } else {
                        indices[idxOut++] = i0; indices[idxOut++] = i1; indices[idxOut++] = i2
                        indices[idxOut++] = i1; indices[idxOut++] = i3; indices[idxOut++] = i2
                    }
                }
            }
        }

        // Part 1: Reserve index topology for 10 Curved Almond Fingernails (vertices filled via articulated solver below)
        for (side in sides) {
            for (specIdx in fingerSpecs.indices) {
                emitNailIndices(nailIdx * vertsPerNail, side)
                nailIdx++
            }
        }
        val leftRest = BlossomArmIkRig.solveSideArmIk(-1f, 0f, 0f)
        val rightRest = BlossomArmIkRig.solveSideArmIk(+1f, 0f, 0f)
        updateArticulatedFingernailsInternal(mesh, leftRest, rightRest)

        // Part 2: 10 Sculpted Natural Flush Toenails seated cleanly on upgraded distal toes
        for (side in sides) {
            for ((specIdx, spec) in toeSpecs.withIndex()) {
                val baseVert = nailIdx * vertsPerNail
                val isHallux = specIdx == 0
                emitNailIndices(baseVert, side)

                val fwdX = side * sin(spec.yawOut)
                val fwdY = -cos(spec.yawOut)
                val latX = side * cos(spec.yawOut)
                val latY = sin(spec.yawOut)

                for (r in 0 until nailRows) {
                    val v = r.toFloat() / (nailRows - 1)
                    // Natural short broad toenails sitting flush on distal toe dorsal bed
                    val s = if (isHallux) 0.61f + v * 0.29f else 0.64f + v * 0.25f

                    // Match shared hemispherical rounded toe profile so toenails stay 100% flush with distal tips
                    val (radScale, tipAngle) = BlossomSdfBodyBuilder.toeRadiusProfile(s, spec, isHallux)

                    val along = -0.095f + s * (spec.length + 0.095f)
                    val curlZ = spec.archLift * sin(s * PI.toFloat()) -
                        (if (isHallux) 0.018f else 0.030f) * smoothStep(0.42f, 0.94f, s)
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

                    val vCentered = v * 2f - 1f
                    // Real toenails are gently squared-oval (especially hallux) rather than narrow points
                    val toePlateWidth = sqrt((1f - vCentered * vCentered * (if (isHallux) 0.16f else 0.24f)).coerceAtLeast(0.45f))
                    val toeArcHalf = if (isHallux) 0.36f * PI.toFloat() else 0.30f * PI.toFloat()

                    for (c in 0 until nailCols) {
                        val u = c.toFloat() / (nailCols - 1)
                        val uCentered = u * 2f - 1f
                        val phi = (PI.toFloat() * 0.5f) - uCentered * toeArcHalf * toePlateWidth
                        val cosP = cos(phi)
                        val sinP = sin(phi)

                        val radialW = cos(tipAngle * 0.50f)
                        val fwdW = sin(tipAngle * 0.50f)
                        var nx = latX * (cosP * radialW / spec.rLat) + fwdX * (fwdW / spec.rLat)
                        var ny = latY * (cosP * radialW / spec.rLat) + fwdY * (fwdW / spec.rLat)
                        var nz = sinP * radialW / spec.rZ
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen

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

                        val edgeBevel = sin(v * PI.toFloat()).coerceAtLeast(0.18f) * cos(uCentered * PI.toFloat() * 0.45f)
                        val plateLift = 0.0056f * (0.50f + 0.50f * edgeBevel)
                        val localLat = spec.rLat * radScale * shapedCosT * halluxBroad
                        val localZ = spec.rZ * radScale * sinP * dorsalToeFlat

                        val px = cx + latX * localLat + nx * plateLift
                        val py = cy + latY * localLat + ny * plateLift
                        val pz = cz + localZ + nz * plateLift

                        val nailAo = (0.85f + 0.15f * cos(uCentered * PI.toFloat() * 0.45f) -
                            0.14f * (1f - smoothStep(0f, 0.22f, v))).coerceIn(0.60f, 1f)

                        val vIdx = (baseVert + r * nailCols + c) * stride
                        data[vIdx] = px; data[vIdx + 1] = py; data[vIdx + 2] = pz
                        data[vIdx + 3] = nx; data[vIdx + 4] = ny; data[vIdx + 5] = nz
                        data[vIdx + 6] = v
                        data[vIdx + 7] = u
                        data[vIdx + 8] = nailAo
                        data[vIdx + 9] = 1.0f // toenail
                    }
                }
                nailIdx++
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return Pair(mesh, toeNailStartVert)
    }

    // -------------------------------------------------------------------------
    // 2. LIVE-DEFORMABLE SOFT ROUNDED MEDIUM BREASTS (EXACT OUTWARD CCW WINDING!)
    // -------------------------------------------------------------------------
    class BreastSideCache(val side: Float, val cols: Int, val rows: Int) {
        val count = cols * rows
        val baseX = FloatArray(count)
        val baseY = FloatArray(count)
        val baseZ = FloatArray(count)
        val baseNx = FloatArray(count)
        val baseNy = FloatArray(count)
        val baseNz = FloatArray(count)
        val baseAo = FloatArray(count)
        val basePigment = FloatArray(count)
        val localEx = FloatArray(count)
        val localEy = FloatArray(count)
        val edgeR = FloatArray(count)
    }

    class BlossomBreastsCache(
        val left: BreastSideCache,
        val right: BreastSideCache
    )

    fun buildBreastsCache(): BlossomBreastsCache {
        fun sampleSide(side: Float): BreastSideCache {
            val cache = BreastSideCache(side, BREAST_COLS, BREAST_ROWS)
            val cx = side * BREAST_CENTER_AX
            val cy = BREAST_CENTER_Y

            for (r in 0 until BREAST_ROWS) {
                val vRaw = (r.toFloat() / (BREAST_ROWS - 1)) * 2f - 1f
                // Tight radial center clustering (~1mm vertex spacing across areola & nipple papilla!)
                val v = 0.46f * vRaw + 0.54f * vRaw * vRaw * vRaw
                for (c in 0 until BREAST_COLS) {
                    val uRaw = (c.toFloat() / (BREAST_COLS - 1)) * 2f - 1f
                    val u = 0.46f * uRaw + 0.54f * uRaw * uRaw * uRaw

                    val ex = u * sqrt((1f - 0.5f * v * v).coerceAtLeast(0.0001f))
                    val ey = v * sqrt((1f - 0.5f * u * u).coerceAtLeast(0.0001f))
                    val edgeR = sqrt(ex * ex + ey * ey).coerceIn(0f, 1f)

                    // Unwarped C1-continuous medial-to-lateral footprint mapped smoothly onto the arm-free ribcage
                    // (preserves exact unpinched v6.2 breast geometry while eliminating all armpit arm-snapping artifacts!)
                    val latBlend = smoothStep(-0.35f, 0.35f, ex * side)
                    val rxHere = BREAST_MEDIAL_R * (1f - latBlend) + BREAST_LATERAL_R * latBlend
                    val xRaw = cx + ex * rxHere
                    val y = cy + ey * BREAST_VERTICAL_R
                    val sample = BlossomSdfBodyBuilder.sampleTorsoChestWallWrapped(xRaw, y)

                    val idx = r * BREAST_COLS + c
                    cache.baseX[idx] = sample.x
                    cache.baseY[idx] = y
                    cache.baseZ[idx] = sample.z
                    cache.baseNx[idx] = sample.nx
                    cache.baseNy[idx] = sample.ny
                    cache.baseNz[idx] = sample.nz
                    cache.baseAo[idx] = sample.ao
                    cache.basePigment[idx] = sample.pigment
                    cache.localEx[idx] = ex
                    cache.localEy[idx] = ey
                    cache.edgeR[idx] = edgeR
                }
            }
            return cache
        }
        return BlossomBreastsCache(left = sampleSide(-1f), right = sampleSide(+1f))
    }

    /**
     * Exact CCW outward winding (i0, i1, i2), (i1, i3, i2) so gl_FrontFacing == true across
     * the entire breast surface and lighting/skin color matches the torso 100% seamlessly!
     */
    fun buildBreastsIndices(): ShortArray {
        val cols = BREAST_COLS
        val rows = BREAST_ROWS
        val vertsPerSide = cols * rows
        val trisPerSide = (cols - 1) * (rows - 1) * 6
        val indices = ShortArray(trisPerSide * 2)
        var out = 0
        for (sideIdx in 0..1) {
            val base = sideIdx * vertsPerSide
            for (r in 0 until rows - 1) {
                for (c in 0 until cols - 1) {
                    val i0 = (base + r * cols + c).toShort()
                    val i1 = (base + r * cols + c + 1).toShort()
                    val i2 = (base + (r + 1) * cols + c).toShort()
                    val i3 = (base + (r + 1) * cols + c + 1).toShort()
                    indices[out++] = i0; indices[out++] = i1; indices[out++] = i2
                    indices[out++] = i1; indices[out++] = i3; indices[out++] = i2
                }
            }
        }
        return indices
    }

    /**
     * Real-time 60 FPS update of Blossom's soft anatomical breasts with true 3D (X, Y, AND Z) volume scaling:
     *  - Preserves the natural medium teardrop silhouette at default `breastSize = 0.50`
     *  - Expands width, upper/lower vertical fullness, roundness, and natural sternum cleavage at Large (`1.00`)
     *    instead of stretching into a pointy Z-only cone
     *  - Gently tucks lateral breast tissue medially when Blossom brings her arms forward in the spread pose
     *    so large breasts clear upper arms cleanly and naturally deepen cleavage!
     */
    fun updateBlossomBreastsMesh(
        mesh: DynamicGlMesh,
        cache: BlossomBreastsCache,
        state: VulvaDeformationState,
        computeNormals: (FloatArray, Int, Int, Boolean, Boolean) -> Unit
    ) {
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val sideBuf = FloatArray(cache.left.count * stride)

        val sizeNorm = state.breastSize.coerceIn(0f, 1f)
        val largeT = smoothStep(0.45f, 1.0f, sizeNorm)
        val armSqueeze = state.selfSpreadPose.coerceIn(0f, 1f) * smoothStep(0.24f, 1.0f, sizeNorm)

        // Compute local supine reclining gravity factor directly from Blossom's exam-table torso slope at breast level
        val epsY = 0.05f
        val reclineSlope = (BlossomBodyBuilder.mattressSurfaceZ(BREAST_CENTER_Y + epsY) -
            BlossomBodyBuilder.mattressSurfaceZ(BREAST_CENTER_Y - epsY)) / (2f * epsY)
        val supineCos = (1f / sqrt(1f + reclineSlope * reclineSlope)).coerceIn(0f, 1f)
        // Larger breasts have greater soft-tissue mobility under supine gravity; arm-spread naturally yields tissue inward
        val gravityMobility = supineCos * (0.22f + 0.78f * sizeNorm) * (1f - 0.65f * armSqueeze)

        // True 3D volume-proportional scaling: X/Y base footprint AND Z projection grow together harmoniously!
        val supineFlattenZ = 1f - 0.13f * gravityMobility
        val supineBroaden = 1f / sqrt(supineFlattenZ)
        // Beta slider (betaBreastSizeBoost): scales breast size 0%..+50% at every setting except the most petite (sizeNorm <= 0.08)
        val boostFrac = state.betaBreastSizeBoost.coerceIn(0f, 0.50f)
        val effectiveBoost = boostFrac * smoothStep(0.08f, 0.28f, sizeNorm)
        val boostXY = 1f + effectiveBoost * 0.56f
        val boostZ = 1f + effectiveBoost * 1.03f
        val sizeBloom = ((sizeNorm - 0.45f) / 0.55f).coerceIn(0f, 1f)
        val footprintScaleX = (0.72f + sizeNorm * 0.48f) * (0.95f + 0.05f * supineBroaden) * boostXY
        val footprintScaleY = (0.72f + sizeNorm * 0.48f) * (0.95f + 0.05f * supineBroaden) * boostXY
        val moundZScale = (0.60f + sizeNorm * 0.52f) * supineFlattenZ * boostZ

        // Gentle outer seam-guarantee collar envelope (reaches exact zero cleanly by edgeR <= 0.925)
        // while letting the continuous cos^p teardrop dome govern the entire organic side profile!
        val collarStart = (0.50f + 0.18f * sizeNorm + effectiveBoost * 0.14f).coerceAtMost(0.75f)
        val collarEnd = (0.84f + 0.06f * sizeNorm + effectiveBoost * 0.06f).coerceAtMost(0.925f)
        val collarPow = (1.35f - 0.20f * sizeNorm).toDouble()

        fun updateSide(sideCache: BreastSideCache, shiftX: Float, shiftY: Float, sideOffsetVerts: Int) {
            val side = sideCache.side
            val sx = shiftX.coerceIn(-1f, 1f) * (0.20f + 0.08f * sizeNorm)
            val sy = shiftY.coerceIn(-1f, 1f) * (0.18f + 0.07f * sizeNorm)
            val medialGather = (-sx * side).coerceIn(-0.14f, 0.22f)
            val liftFullness = (sy * 0.15f).coerceIn(-0.04f, 0.05f)
            val manualMedialT = (medialGather / 0.22f).coerceIn(0f, 1f)

            // Natural closer-set sit with realistic lateral supine settle as size increases
            val apexEx = side * (-0.018f + 0.148f * gravityMobility - 0.062f * armSqueeze - 0.065f * manualMedialT)
            val apexEy = -(0.085f + 0.055f * sizeNorm)
            val latBlendApex = smoothStep(-0.35f, 0.35f, apexEx * side)
            val rxWorldAtApex = BREAST_MEDIAL_R * (1f - latBlendApex) + BREAST_LATERAL_R * latBlendApex
            val areolaLatComp = 1f / (1f + 0.06f * gravityMobility * largeT)

            for (i in 0 until sideCache.count) {
                val ex = sideCache.localEx[i]
                val ey = sideCache.localEy[i]
                val edgeR = sideCache.edgeR[i]

                // Smooth C2 zero-slope chest-wall collar falloff reaching exact zero cleanly before edgeR = 0.90
                val collarLinear = 1f - smoothStep(collarStart, collarEnd, edgeR)
                val collarFade = Math.pow(collarLinear.toDouble(), collarPow).toFloat()
                val softShiftW = (1f - smoothStep(0.0f, 0.86f, edgeR)).let { t -> t * t * (3f - 2f * t) }

                val dxRel = ex - apexEx
                val dyRel = ey - apexEy
                val dxLatBlend = smoothStep(-0.36f, 0.36f, dxRel * side)
                val dyUpBlend = smoothStep(-0.38f, 0.38f, dyRel)
                val medialSpread = (0.88f + 0.14f * sizeNorm + 0.14f * (armSqueeze + manualMedialT)) * footprintScaleX
                val lateralSpread = (0.90f + 0.14f * gravityMobility - 0.09f * armSqueeze) * footprintScaleX
                val upperSpread = (1.14f + 0.10f * sizeNorm) * footprintScaleY
                val lowerSpread = (0.86f + 0.14f * sizeNorm) * footprintScaleY

                val spreadX = medialSpread * (1f - dxLatBlend) + lateralSpread * dxLatBlend
                val spreadY = lowerSpread * (1f - dyUpBlend) + upperSpread * dyUpBlend
                val dxA = dxRel / spreadX
                val dyA = dyRel / spreadY
                val rawRDome = sqrt(dxA * dxA + dyA * dyA)
                val rDome = (rawRDome + 0.07f * smoothStep(0.68f, 0.90f, edgeR)).coerceIn(0f, 1f)

                // Organic supine teardrop dome (cos^p has zero slope at both crown AND chest-wall base!):
                //  - Upper pole (dyRel > 0): long graceful concave-to-convex pectoral sweep seamlessly out of chest wall
                //  - Lower pole (dyRel < 0): fuller soft convex teardrop roll curving gently into inframammary crease
                //  - Eliminates flat-topped cylinder plateau and steep vertical perimeter cliffs in side profile!
                val cosDome = cos(rDome * (PI.toFloat() * 0.5f)).coerceAtLeast(0f)
                val upperPow = (2.05f - 0.12f * sizeNorm).toDouble()
                val lowerPow = (1.46f - 0.16f * sizeNorm).toDouble()
                val upperRamp = Math.pow(cosDome.toDouble(), upperPow).toFloat()
                val lowerFull = Math.pow(cosDome.toDouble(), lowerPow).toFloat()
                val lowerHalfT = smoothStep(0.32f, -0.34f, dyRel)
                val roundedDome = (upperRamp * (1f - lowerHalfT) + lowerFull * lowerHalfT) * collarFade

                // Subtle anatomical upper-outer pectoral bridge (Tail of Spence) so breasts grow organically from the chest wall
                val tailOfSpence = 0.034f * moundZScale *
                    smoothStep(-0.08f, 0.48f, ex * side) * smoothStep(0.02f, 0.52f, ey) *
                    (1f - smoothStep(0.60f, 0.87f, edgeR)) * collarFade

                // Gentle upper-pole softness, natural lower teardrop cushion, and smooth supine lateral fullness
                // (peaking smoothly inside the dome so outer walls never bulge into a cylindrical shelf)
                val upperSlopeSoften = 1f - (0.08f + 0.05f * gravityMobility) * smoothStep(0.14f, 0.86f, ey)
                val medialSupineSlope = 1f - (0.14f * gravityMobility * (1f - armSqueeze)) * smoothStep(0.12f, -0.56f, ex * side)
                val lowerPlush = 1f + (0.04f + 0.07f * sizeNorm) * smoothStep(-0.02f, -0.44f, ey) * (1f - smoothStep(0.46f, 0.82f, edgeR))
                val lateralGravityFull = 1f + (0.04f + 0.14f * gravityMobility) * smoothStep(0.00f, 0.42f, ex * side) * (1f - smoothStep(0.48f, 0.84f, edgeR))
                val medialCompressFull = 1f + (0.14f * (armSqueeze + manualMedialT)) *
                    smoothStep(0.14f, -0.58f, ex * side) * (1f - smoothStep(0.52f, 0.84f, edgeR))

                val moundHeight = (0.485f * moundZScale + medialGather * 0.16f + liftFullness) *
                    roundedDome * upperSlopeSoften * medialSupineSlope * lowerPlush * lateralGravityFull * medialCompressFull + tailOfSpence

                // Richer, more visible natural v6.3 areola & sculpted 3D nipple papilla on soft teardrop breasts (apex-centered circular coords)
                val relWx = dxRel * rxWorldAtApex * areolaLatComp
                val relWy = dyRel * BREAST_VERTICAL_R
                val distApexWorld = sqrt(relWx * relWx + relWy * relWy)
                val ang = kotlin.math.atan2(relWy, relWx)
                val organicEdge = 1f + 0.032f * sin(ang * 3f + side * 1.1f) +
                    0.018f * cos(ang * 5f - side * 0.7f)
                val distOrg = sqrt((relWx * 0.98f).let { it * it } + (relWy * 1.02f).let { it * it }) / organicEdge

                val areolaScale = 0.86f + 0.24f * sizeNorm
                val areolaMask = (1f - smoothStep(0.094f * areolaScale, 0.164f * areolaScale, distOrg)) * collarFade
                val areolaCushion = 0.0052f * (1f - smoothStep(0.024f, 0.152f * areolaScale, distOrg)) * collarFade
                val basalGroove = -0.0032f * exp(-((distApexWorld - 0.044f) / 0.013f).let { it * it }) * collarFade

                val nipR = 0.038f
                val nipT = (distApexWorld / nipR).coerceIn(0f, 1.4f)
                val nippleDome = if (nipT < 1f) {
                    val roundedCrown = Math.pow((1.0 - Math.pow(nipT.toDouble(), 2.4)), 0.56).toFloat()
                    0.035f * roundedCrown * collarFade
                } else {
                    0f
                }
                val nippleMask = (1f - smoothStep(0.026f, 0.044f, distApexWorld)) * collarFade

                // Smooth micro-lift through the transition band then invisible tuck beneath the ribcage at edgeR 0.89..1.00
                val collarBias = 0.0028f * smoothStep(0.58f, 0.78f, edgeR) * (1f - smoothStep(0.85f, 0.91f, edgeR)) -
                    0.0068f * smoothStep(0.89f, 1.0f, edgeR)
                val totalLift = moundHeight + areolaCushion + basalGroove + nippleDome + collarBias

                // True tangential X/Y radial tissue expansion as size grows toward Full (wider cleavage/ribcage + fuller lower drape)
                val bloomMask = roundedDome * (1f - smoothStep(0.62f, 0.88f, edgeR))
                val bloomX = ex * 0.155f * sizeBloom * bloomMask
                val bloomY = (ey * 0.115f - 0.072f * smoothStep(0.22f, -0.68f, ey)) * sizeBloom * bloomMask

                // Moderate natural lateral settle & gentle lower drape (keeps breasts seated comfortably closer while redistributing fuller sizes outward)
                val lateralTuckMask = smoothStep(-0.15f, 0.75f, ex * side) * roundedDome
                val supineLateralShiftX = side * (0.006f + 0.076f * gravityMobility) * roundedDome * (0.45f + 0.55f * smoothStep(-0.25f, 0.55f, ex * side))
                val armTuckX = -side * (0.145f * armSqueeze * lateralTuckMask + 0.065f * armSqueeze * roundedDome)
                val lowerGravityY = -(0.018f + 0.026f * sizeNorm) * roundedDome * (0.45f + 0.55f * smoothStep(0.15f, -0.70f, ey))

                // Follow local curved ribcage normal cleanly with subtle natural outward orientation
                val latDrapeNorm = (0.035f + 0.085f * gravityMobility - 0.075f * armSqueeze - 0.07f * manualMedialT).coerceIn(-0.04f, 0.15f)
                val nxB = sideCache.baseNx[i] * 0.42f + side * latDrapeNorm * softShiftW
                val nyB = sideCache.baseNy[i] * 0.28f - (0.006f + 0.014f * sizeNorm) * softShiftW
                val nzB = (sideCache.baseNz[i] * 0.58f + 0.42f).coerceAtLeast(0.52f)
                val dirLen = sqrt(nxB * nxB + nyB * nyB + nzB * nzB).coerceAtLeast(1e-5f)

                var px = sideCache.baseX[i] + (nxB / dirLen) * totalLift + sx * softShiftW + bloomX + supineLateralShiftX + armTuckX
                val py = sideCache.baseY[i] + (nyB / dirLen) * totalLift + sy * softShiftW + bloomY + lowerGravityY
                var pz = sideCache.baseZ[i] + (nzB / dirLen) * totalLift +
                    (medialGather * 0.06f - abs(sx) * 0.025f) * softShiftW +
                    0.038f * armSqueeze * lateralTuckMask

                // Preserve clean non-crossing midline valley even when fuller or pushed inward manually / by arm-spread
                val signedPx = px * side
                val minMidGap = 0.052f
                if (signedPx < 0.115f && roundedDome > 0.01f) {
                    val delta = signedPx - minMidGap
                    val clamped = minMidGap + 0.5f * (delta + sqrt(delta * delta + 0.0014f))
                    px = side * clamped
                    val pressDepth = (0.115f - signedPx).coerceAtLeast(0f)
                    pz += pressDepth * 0.048f * roundedDome
                }

                // Pure torso-matched AO across the breast & chest-wall boundary (zero dark smeared under-breast band!)
                val nippleRimAo = exp(-((distApexWorld - 0.042f) / 0.014f).let { it * it }) * 0.13f * collarFade
                val ao = (sideCache.baseAo[i] * (1f - nippleRimAo)).coerceIn(0.24f, 1f)

                // Exact torso skin wetness match (0.055f) at outer collar, darker matte natural v6.3 areola & nipple finish
                val mucosa = (areolaMask * 0.14f + nippleMask * 0.10f).coerceIn(0f, 0.26f)
                val wetness = (0.055f - areolaMask * 0.028f - nippleMask * 0.006f).coerceIn(0.020f, 0.055f)
                val pigment = (sideCache.basePigment[i] + areolaMask * 0.56f + nippleMask * 0.34f).coerceIn(0f, 0.94f)

                val idx = i * stride
                sideBuf[idx] = px; sideBuf[idx + 1] = py; sideBuf[idx + 2] = pz
                sideBuf[idx + 6] = mucosa
                sideBuf[idx + 7] = wetness
                sideBuf[idx + 8] = ao
                sideBuf[idx + 9] = pigment
            }

            computeNormals(sideBuf, sideCache.cols, sideCache.rows, false, false)

            // Smooth normal convergence into the analytical ribcage normals BEFORE the outer skirt tucks in (0.60..0.88)
            for (i in 0 until sideCache.count) {
                val edgeR = sideCache.edgeR[i]
                val nBlend = smoothStep(0.60f, 0.88f, edgeR)
                val idx = i * stride
                if (nBlend > 0.001f) {
                    val mx = sideBuf[idx + 3] * (1f - nBlend) + sideCache.baseNx[i] * nBlend
                    val my = sideBuf[idx + 4] * (1f - nBlend) + sideCache.baseNy[i] * nBlend
                    val mz = sideBuf[idx + 5] * (1f - nBlend) + sideCache.baseNz[i] * nBlend
                    val len = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                    sideBuf[idx + 3] = mx / len
                    sideBuf[idx + 4] = my / len
                    sideBuf[idx + 5] = mz / len
                }
            }

            System.arraycopy(sideBuf, 0, data, sideOffsetVerts * stride, sideBuf.size)
        }

        updateSide(cache.left, state.leftBreastX, state.leftBreastY, 0)
        updateSide(cache.right, state.rightBreastX, state.rightBreastY, cache.left.count)
        mesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // 3. SCULPTURAL REALISTIC FEMININE HEAD & FACE (120x108, OUTWARD CCW!)
    //    theta runs BOTTOM-TO-TOP (PI -> 0) so d/dc=+X, d/dr=+Y => +Z OUTWARD!
    // -------------------------------------------------------------------------
    private const val HEAD_COLS = 120
    private const val HEAD_ROWS = 108

    fun buildBlossomHeadMesh(
        buildIndices: (Int, Int) -> ShortArray,
        computeNormals: (FloatArray, Int, Int, Boolean, Boolean) -> Unit
    ): DynamicGlMesh {
        val cols = HEAD_COLS
        val rows = HEAD_ROWS
        val mesh = DynamicGlMesh(cols * rows, (cols - 1) * (rows - 1) * 6)
        mesh.setIndices(buildIndices(cols, rows))

        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        for (r in 0 until rows) {
            // Run v from 0 (neck/chin bottom, theta = PI) UP to 1 (crown top, theta = 0) -> strictly +Y!
            val vRaw = 1f - (r.toFloat() / (rows - 1))
            val vCentered = vRaw * 2f - 1f
            val vCluster = 0.66f * vCentered + 0.34f * vCentered * vCentered * vCentered
            val theta = ((vCluster + 1f) * 0.5f).coerceIn(0.004f, 0.996f) * PI.toFloat()
            val sinT = sin(theta)
            val cosT = cos(theta)

            for (c in 0 until cols) {
                // Cluster horizontal resolution tightly across anterior face (nose, eyes & lips ~4mm vertex spacing!)
                val uRaw = (c.toFloat() / (cols - 1)) * 2f - 1f
                val uCluster = 0.52f * uRaw + 0.48f * uRaw * uRaw * uRaw
                val phi = uCluster * PI.toFloat()
                val sinP = sin(phi)
                val cosP = cos(phi)

                // Realistic adult feminine oval skull & bone structure (orthognathic profile, gentle gonial corner & smooth chin-neck transition!)
                val yScale = 0.885f + 0.070f * smoothStep(-0.35f, 0.35f, cosT)
                val rawY = cosT * yScale
                val frontFace = smoothStep(-0.12f, 0.48f, cosP)

                val lowerFaceT = smoothStep(0.06f, -0.76f, rawY)
                // Gently defined adult female gonial jaw corner beneath the ears transitioning smoothly into oval lower face
                val jawAngle = 1f + 0.046f * exp(-((rawY + 0.43f) / 0.13f).let { it * it }) *
                    smoothStep(0.42f, 0.85f, abs(sinP)) * smoothStep(-0.35f, 0.25f, cosP)
                val jawTaper = 1f - 0.365f * lowerFaceT * lowerFaceT * (0.48f + 0.52f * frontFace)
                val chinRound = 1f - 0.125f * smoothStep(-0.52f, -0.82f, rawY) * frontFace
                val templeHollow = 1f - 0.056f * exp(-((rawY - 0.22f) / 0.24f).let { it * it }) * smoothStep(0.36f, 0.86f, abs(sinP))
                val cheekPlaneSlim = 1f - 0.026f * exp(-((rawY + 0.28f) / 0.22f).let { it * it }) * smoothStep(0.38f, 0.84f, abs(sinP)) * frontFace
                val craniumBackFull = 1f + 0.07f * exp(-((rawY - 0.16f) / 0.48f).let { it * it }) * smoothStep(0.10f, 0.80f, -cosP)

                var lx = sinT * sinP * 0.642f * jawAngle * jawTaper * chinRound * templeHollow * cheekPlaneSlim
                var ly = rawY
                var lz = sinT * cosP * 0.745f * craniumBackFull

                // Orthognathic anterior facial scaffold counteracting spherical sin(theta) Z-collapse in midface, maxilla & chin!
                if (cosP > 0f) {
                    val axPre = abs(lx)
                    val antPlaneW = smoothStep(0.04f, 0.60f, cosP)
                    val foreheadPlane = -0.016f * smoothStep(0.28f, 0.72f, ly) * cosP * cosP
                    // Structural maxillary/dental arch supporting upper & lower lips smoothly
                    val maxillaArchZ = 0.036f * exp(-((axPre / 0.32f).let { it * it } + ((ly + 0.40f) / 0.21f).let { it * it })) * antPlaneW
                    // Orthognathic mandibular/chin forward carrier with smooth submental underside fade (zero cliff from low views!)
                    val chinOrthoZ = 0.156f * smoothStep(-0.24f, -0.66f, rawY) *
                        (1f - smoothStep(-0.68f, -0.87f, rawY)) * exp(-((axPre / 0.42f).let { it * it })) * antPlaneW
                    // Sculpted mandibular body border curving smoothly from gonial corner forward into the chin
                    val jawLineY = -0.68f + 0.24f * smoothStep(0.08f, 0.46f, axPre)
                    val mandibleBody = 0.022f * exp(-((ly - jawLineY) / 0.075f).let { it * it }) *
                        smoothStep(0.56f, 0.14f, axPre) * smoothStep(-0.08f, 0.44f, cosP)
                    // 3D malar/zygomatic arch support wrapping naturally onto the oblique 3/4 cheek plane (preserves soft cheek fullness!)
                    val malarArch = 0.026f * exp(-(((axPre - 0.375f) / 0.165f).let { it * it } + ((ly + 0.055f) / 0.140f).let { it * it })) *
                        smoothStep(0.04f, 0.50f, cosP)
                    lx += (if (sinP >= 0f) 1f else -1f) * (mandibleBody * 0.40f + malarArch * 0.65f)
                    lz += foreheadPlane + maxillaArchZ + chinOrthoZ + mandibleBody * 0.80f + malarArch * 0.78f
                }

                // Smooth C2 submental floor mask easing gradually beneath chin pogonion into the neck cylinder (zero pinched jaw shelf!)
                val submentalLin = smoothStep(0.835f * PI.toFloat(), 0.985f * PI.toFloat(), theta)
                val submentalT = submentalLin * submentalLin

                val ax = abs(lx)
                var dzFace = 0f
                var faceMucosa = 0f
                var faceWetness = 0.030f
                var faceAo = 1f
                var facePigment = 0.008f

                if (cosP > 0.12f && submentalT < 0.45f) {
                    val fW = smoothStep(0.12f, 0.52f, cosP) * (1f - submentalT)

                    // 1. Realistic 3D Feminine Facial Planes (natural cheek fullness, gentle infraorbital-to-malar light flow, defined adult chin)
                    val zygomaCheek = 0.034f * exp(-(((ax - 0.365f) / 0.165f).let { it * it } + ((ly + 0.062f) / 0.140f).let { it * it }))
                    val malarLightShelf = 0.0055f * exp(-(((ax - 0.305f) / 0.120f).let { it * it } + ((ly + 0.075f) / 0.085f).let { it * it }))
                    val subMalarRoll = -0.0042f * exp(-(((ax - 0.432f) / 0.115f).let { it * it } + ((ly + 0.175f) / 0.110f).let { it * it }))
                    val cheekSoftFull = 0.014f * exp(-(((ax - 0.295f) / 0.155f).let { it * it } + ((ly + 0.215f) / 0.145f).let { it * it }))
                    val buccalSoftHollow = -0.009f * exp(-(((ax - 0.335f) / 0.130f).let { it * it } + ((ly + 0.360f) / 0.135f).let { it * it }))
                    val chinForm = 0.044f * exp(-((ax / 0.148f).let { it * it } + ((ly + 0.690f) / 0.095f).let { it * it }))
                    val jawRimDef = 0.018f * exp(-(((ax - 0.265f) / 0.140f).let { it * it } + ((ly + 0.595f) / 0.088f).let { it * it }))
                    val browArchY = 0.172f + 0.034f * exp(-((ax - 0.332f) / 0.105f).let { it * it }) - 0.040f * smoothStep(0.345f, 0.455f, ax)
                    val browBone = 0.026f * exp(-(((ax - 0.258f) / 0.205f).let { it * it } + ((ly - browArchY) / 0.070f).let { it * it }))
                    val glabellaBridge = 0.016f * exp(-((ax / 0.095f).let { it * it } + ((ly - 0.145f) / 0.072f).let { it * it }))
                    val tearTrough = -0.008f * exp(-(((ax - 0.248f) / 0.120f).let { it * it } + ((ly + 0.056f) / 0.040f).let { it * it }))
                    val nasolabialSoft = 0.007f * exp(-(((ax - 0.215f) / 0.082f).let { it * it } + ((ly + 0.325f) / 0.115f).let { it * it }))
                    dzFace += (zygomaCheek + malarLightShelf + subMalarRoll + cheekSoftFull + buccalSoftHollow +
                        chinForm + jawRimDef + browBone + glabellaBridge + tearTrough + nasolabialSoft) * fW

                    // 2. Classical Rule-of-5-Eyes Proportions with contoured eyelid rim wrap & inner-canthus tear-duct pocket
                    val exEye = (ax - 0.256f) / 0.142f
                    val eyEye = (ly - 0.036f) / 0.088f
                    val orbitR2 = exEye * exEye * 0.54f + eyEye * eyEye * 0.48f
                    val orbitHollow = -0.072f * exp(-orbitR2 * 1.48f)

                    val eyeU = ((ax - 0.256f) / 0.126f).coerceIn(-1.25f, 1.25f)
                    val eyeSpan = (1f - eyeU * eyeU).coerceAtLeast(0f)
                    val upperLidY = 0.036f + (0.056f + 0.008f * eyeU) * sqrt(eyeSpan)
                    val lowerLidY = 0.036f - (0.036f - 0.006f * eyeU) * sqrt(eyeSpan)
                    val insideAperture = if (abs(eyeU) < 0.985f) {
                        smoothStep(lowerLidY - 0.0065f, lowerLidY + 0.0065f, ly) *
                            (1f - smoothStep(upperLidY - 0.0065f, upperLidY + 0.0065f, ly))
                    } else 0f

                    val apertureRecess = -0.122f * insideAperture
                    val lidSpanW = smoothStep(1.04f, 0.58f, abs(eyeU))
                    // Sculpted upper & lower eyelid margin thickness wrapping organically over the eyeball curvature
                    val upperLidRim = 0.0075f * exp(-((ly - (upperLidY + 0.0075f)) / 0.0095f).let { it * it }) * lidSpanW
                    val lowerLidRim = 0.0055f * exp(-((ly - (lowerLidY - 0.0065f)) / 0.0088f).let { it * it }) * lidSpanW
                    val upperLidFold = 0.034f * exp(-((ly - (upperLidY + 0.018f)) / 0.018f).let { it * it }) * smoothStep(1.06f, 0.62f, abs(eyeU))
                    val upperCrease = -0.018f * exp(-((ly - (upperLidY + 0.042f)) / 0.014f).let { it * it }) * smoothStep(1.02f, 0.52f, abs(eyeU))
                    val lowerLidMargin = 0.019f * exp(-((ly - (lowerLidY - 0.014f)) / 0.015f).let { it * it }) * smoothStep(1.04f, 0.60f, abs(eyeU))
                    // Subtle medial inner-canthus (tear-duct / caruncle) nook for natural inner eye corner depth
                    val innerCanthusMask = exp(-(((ax - 0.138f) / 0.022f).let { it * it } + ((ly - 0.032f) / 0.018f).let { it * it }))
                    val innerCanthusPocket = -0.013f * innerCanthusMask
                    dzFace += (orbitHollow + apertureRecess + upperLidRim + lowerLidRim + upperLidFold +
                        upperCrease + lowerLidMargin + innerCanthusPocket) * fW

                    // Natural tapered eyebrows spanning inner-to-outer eye (strictly below lash pigment threshold -> ZERO dark dots!)
                    val lashThick = 0.0092f + 0.0032f * smoothStep(-0.25f, 0.88f, eyeU)
                    val lashLine = exp(-((ly - (upperLidY + 0.003f)) / lashThick).let { it * it }) * smoothStep(1.04f, 0.66f, abs(eyeU))
                    val lowerLash = exp(-((ly - (lowerLidY - 0.002f)) / 0.0065f).let { it * it }) * smoothStep(0.92f, 0.42f, abs(eyeU)) * 0.24f
                    val browThickness = 0.0205f * (1f - 0.42f * smoothStep(0.310f, 0.450f, ax))
                    val browMask = exp(-((ly - browArchY) / browThickness).let { it * it }) *
                        smoothStep(0.098f, 0.148f, ax) * (1f - smoothStep(0.388f, 0.456f, ax))

                    // 3. True 3D Classical Feminine Nose (restrained columella, angled infra-tip undercut & clean alar wings!)
                    val nasionDip = -0.012f * exp(-((ax / 0.068f).let { it * it } + ((ly - 0.068f) / 0.046f).let { it * it }))
                    val bridgeW = 0.060f + 0.016f * smoothStep(0.068f, -0.21f, ly)
                    val bridgeSpan = smoothBell(ly, -0.228f, 0.075f, 0.062f)
                    val bridgeProfile = 0.032f + 0.056f * smoothStep(0.068f, -0.21f, ly)
                    val supratipBreak = -0.004f * exp(-((ax / 0.068f).let { it * it } + ((ly + 0.175f) / 0.038f).let { it * it }))
                    val noseBridge = bridgeProfile * exp(-((ax / bridgeW).let { it * it })) * bridgeSpan
                    val noseTip = 0.072f * exp(-((ax / 0.084f).let { it * it } + ((ly + 0.216f) / 0.065f).let { it * it }))
                    val noseAla = 0.040f * exp(-(((ax - 0.090f) / 0.048f).let { it * it } + ((ly + 0.244f) / 0.042f).let { it * it }))
                    val alarCreaseMask = exp(-(((ax - 0.132f) / 0.024f).let { it * it } + ((ly + 0.246f) / 0.046f).let { it * it }))
                    val alarCrease = -0.0135f * alarCreaseMask
                    val nostrilCut = -0.018f * exp(-(((ax - 0.054f) / 0.027f).let { it * it } + ((ly + 0.268f) / 0.023f).let { it * it }))
                    // Anatomical columella pillar + angled infra-tip underside + subtle nostril sill (stronger 3/4 & lower-view depth!)
                    val columella = 0.013f * exp(-((ax / 0.023f).let { it * it } + ((ly + 0.264f) / 0.024f).let { it * it }))
                    val infraTipUndercut = -0.0085f * exp(-((ax / 0.074f).let { it * it } + ((ly + 0.278f) / 0.022f).let { it * it }))
                    val nostrilSill = 0.0052f * exp(-(((ax - 0.056f) / 0.032f).let { it * it } + ((ly + 0.288f) / 0.014f).let { it * it }))
                    val philtrumDimple = -0.009f * exp(-((ax / 0.032f).let { it * it })) * smoothBell(ly, -0.390f, -0.290f, 0.030f)
                    val philtralCols = 0.006f * exp(-(((ax - 0.044f) / 0.020f).let { it * it })) * smoothBell(ly, -0.394f, -0.296f, 0.026f)
                    dzFace += (nasionDip + noseBridge + supratipBreak + noseTip + noseAla + alarCrease + nostrilCut +
                        columella + infraTipUndercut + nostrilSill + philtrumDimple + philtralCols) * fW

                    // 4. Classical Proportioned Natural Feminine Lips (delicate vermilion ridge & softly tucked corners, ZERO enlargement!)
                    val lipU = (ax / 0.222f).coerceIn(0f, 1.35f)
                    val lipSpan = smoothStep(1.04f, 0.48f, lipU)
                    val gentleCornerCurve = 0.015f * lipU * lipU
                    val cupidDip = -0.009f * exp(-((ax / 0.032f).let { it * it })) +
                        0.009f * exp(-(((ax - 0.054f) / 0.034f).let { it * it }))
                    val lipPartY = -0.440f + gentleCornerCurve
                    val upperLipY = -0.408f + cupidDip + gentleCornerCurve * 0.30f
                    val lowerLipY = -0.470f + gentleCornerCurve * 0.55f

                    val upperLipVol = 0.025f * exp(-((ly - upperLipY) / 0.024f).let { it * it }) * lipSpan
                    val upperTubercle = 0.005f * exp(-((ax / 0.042f).let { it * it } + ((ly + 0.418f) / 0.018f).let { it * it }))
                    val lowerLipVol = 0.030f * exp(-((ly - lowerLipY) / 0.029f).let { it * it }) * smoothStep(0.95f, 0.36f, lipU)
                    val upperVermilionRidge = 0.0036f * exp(-((ly - (upperLipY + 0.017f)) / 0.0075f).let { it * it }) * smoothStep(0.92f, 0.36f, lipU)
                    val lowerVermilionRidge = 0.0026f * exp(-((ly - (lowerLipY - 0.021f)) / 0.0085f).let { it * it }) * smoothStep(0.86f, 0.32f, lipU)
                    val lipPartGroove = -0.012f * exp(-((ly - lipPartY) / 0.0095f).let { it * it }) * smoothStep(0.98f, 0.48f, lipU)
                    val cornerDimpleMask = exp(-(((ax - 0.224f) / 0.025f).let { it * it } + ((ly + 0.425f) / 0.023f).let { it * it }))
                    val cornerDimple = -0.013f * cornerDimpleMask
                    val mentolabialDip = -0.018f * exp(-((ax / 0.145f).let { it * it } + ((ly + 0.576f) / 0.048f).let { it * it }))
                    dzFace += (upperLipVol + upperTubercle + lowerLipVol + upperVermilionRidge + lowerVermilionRidge +
                        lipPartGroove + cornerDimple + mentolabialDip) * fW

                    // Natural muted lip rose tightly aligned to the sculpted vermilion border + clean skin-matched cheeks
                    val rawLipMask = ((exp(-((ly - upperLipY) / 0.022f).let { it * it }) +
                        exp(-((ly - lowerLipY) / 0.026f).let { it * it })) * lipSpan).coerceIn(0f, 1f)
                    val lipColorMask = smoothStep(0.12f, 0.72f, rawLipMask)
                    val cheekWarmth = 0.011f * exp(-(((ax - 0.360f) / 0.175f).let { it * it } + ((ly + 0.08f) / 0.150f).let { it * it }))

                    faceMucosa = (lipColorMask * 0.44f + cheekWarmth + innerCanthusMask * 0.12f).coerceIn(0f, 0.48f)
                    faceWetness = (0.028f + lipColorMask * 0.040f).coerceIn(0.024f, 0.072f)
                    val nostrilAo = exp(-(((ax - 0.054f) / 0.030f).let { it * it } + ((ly + 0.266f) / 0.025f).let { it * it })) * 0.26f
                    val alarAo = alarCreaseMask * 0.095f
                    val lipLineAo = exp(-((ly - lipPartY) / 0.0095f).let { it * it }) * lipSpan * 0.25f
                    val cornerAo = cornerDimpleMask * 0.12f
                    val canthusAo = innerCanthusMask * 0.11f
                    val creaseAo = exp(-((ly - (upperLidY + 0.042f)) / 0.015f).let { it * it }) * smoothStep(1.00f, 0.52f, abs(eyeU)) * 0.20f
                    val orbitShadowAo = exp(-orbitR2 * 1.90f) * 0.06f
                    val browDarkAo = browMask * 0.28f
                    val lashDarkAo = (lashLine * 0.62f + lowerLash * 0.24f).coerceIn(0f, 0.66f)
                    faceAo = (1f - nostrilAo - alarAo - lipLineAo - cornerAo - canthusAo - creaseAo - orbitShadowAo - browDarkAo - lashDarkAo).coerceIn(0.24f, 1f)
                    // Clean separation: brows stay in 0.08..0.54 (hair-matched) while lashes reach 0.72..0.92 (dark lashes)!
                    val browPigment = (browMask * 0.50f).coerceAtMost(0.52f)
                    val lashPigment = lashLine * 0.88f + lowerLash * 0.36f
                    facePigment = (0.008f + max(browPigment, lashPigment) + lipColorMask * 0.06f).coerceIn(0f, 0.96f)
                }

                // 5. Delicate Sculpted Ears on lateral temples
                val earRegion = smoothStep(0.72f, 0.96f, abs(sinP)) * smoothBell(ly, -0.24f, 0.12f, 0.08f) * (1f - submentalT)
                if (earRegion > 0.001f) {
                    val earLat = 0.056f * earRegion * exp(-((cosP + 0.16f) / 0.28f).let { it * it })
                    lx += (if (sinP >= 0f) 1f else -1f) * earLat
                    lz -= 0.018f * earRegion
                    faceAo *= (1f - 0.16f * earRegion * exp(-((cosP + 0.10f) / 0.18f).let { it * it }))
                }

                // Soft natural ambient occlusion gently transitioning under the jaw onto the neck
                if (submentalT > 0.001f) {
                    faceAo *= (1f - 0.14f * submentalT)
                }

                lz += dzFace

                val worldPt = headLocalToWorld(lx, ly, lz)
                // Smoothly curve submental underside directly into Blossom's exact world-space SDF neck cylinder
                // (continuous gentle chin-to-neck sweep from low/extreme angles while tucking row 0 strictly inside neck R < 0.24!)
                if (submentalT > 0.001f) {
                    val zNeckBase = BlossomBodyBuilder.mattressSurfaceZ(4.86f) + 0.58f
                    val zNeckTop = BlossomBodyBuilder.mattressSurfaceZ(5.78f) + 0.80f
                    val targetWy = 5.66f - 0.26f * smoothStep(0.86f * PI.toFloat(), 0.996f * PI.toFloat(), theta)
                    val neckT = ((targetWy - 4.84f) / 0.94f).coerceIn(0f, 1f)
                    val neckAxisZ = zNeckBase + (zNeckTop - zNeckBase) * neckT + 0.018f * sin(neckT * PI.toFloat())
                    val targetRad = 0.305f - 0.120f * smoothStep(0.875f * PI.toFloat(), 0.996f * PI.toFloat(), theta)
                    val targetWx = sinP * targetRad
                    val targetWz = neckAxisZ + cosP * targetRad
                    worldPt[0] = worldPt[0] * (1f - submentalT) + targetWx * submentalT
                    worldPt[1] = worldPt[1] * (1f - submentalT) + targetWy * submentalT
                    worldPt[2] = worldPt[2] * (1f - submentalT) + targetWz * submentalT
                }

                val idx = (r * cols + c) * stride
                data[idx] = worldPt[0]
                data[idx + 1] = worldPt[1]
                data[idx + 2] = worldPt[2]
                data[idx + 6] = faceMucosa
                data[idx + 7] = faceWetness
                data[idx + 8] = faceAo
                data[idx + 9] = facePigment
            }
        }

        // Bottom-to-top v parameterization means tx x bx = (+X) x (+Y) = +Z OUTWARD!
        computeNormals(data, cols, rows, false, true)

        // Smoothly blend the tucked submental/neck-socket collar into SDF neck normals so low-angle shading stays clean & crease-free
        for (r in 0 until rows) {
            val vRaw = 1f - (r.toFloat() / (rows - 1))
            val vCentered = vRaw * 2f - 1f
            val vCluster = 0.66f * vCentered + 0.34f * vCentered * vCentered * vCentered
            val theta = ((vCluster + 1f) * 0.5f).coerceIn(0.004f, 0.996f) * PI.toFloat()
            val nBlend = smoothStep(0.89f * PI.toFloat(), 0.985f * PI.toFloat(), theta)
            if (nBlend > 0.001f) {
                for (c in 0 until cols) {
                    val idx = (r * cols + c) * stride
                    val sdfNorm = BlossomSdfBodyBuilder.evaluateBodyNormal(data[idx], data[idx + 1], data[idx + 2], eps = 0.018f)
                    val mx = data[idx + 3] * (1f - nBlend) + sdfNorm[0] * nBlend
                    val my = data[idx + 4] * (1f - nBlend) + sdfNorm[1] * nBlend
                    val mz = data[idx + 5] * (1f - nBlend) + sdfNorm[2] * nBlend
                    val len = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                    data[idx + 3] = mx / len
                    data[idx + 4] = my / len
                    data[idx + 5] = mz / len
                }
            }
        }

        mesh.commitVertices()
        return mesh
    }

    // -------------------------------------------------------------------------
    // 4. NATURAL DEEPER-SET SAPPHIRE-BLUE 3D EYES (SEATED CALMLY BEHIND EYELIDS)
    // -------------------------------------------------------------------------
    fun buildBlossomEyesMesh(): DynamicGlMesh {
        val cols = 28
        val rows = 20
        val vertsPerEye = cols * rows
        val trisPerEye = (cols - 1) * (rows - 1) * 6

        val mesh = DynamicGlMesh(vertsPerEye * 2, trisPerEye * 2)
        val indices = ShortArray(trisPerEye * 2)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        val eyeRadius = 0.136f
        val eyeCenterY = 0.038f
        val eyeCenterZ = 0.528f
        val sides = floatArrayOf(-1f, +1f)

        var outIdx = 0
        for ((eyeIndex, side) in sides.withIndex()) {
            val baseVert = eyeIndex * vertsPerEye
            for (r in 0 until rows - 1) {
                for (c in 0 until cols - 1) {
                    val i0 = (baseVert + r * cols + c).toShort()
                    val i1 = (baseVert + r * cols + c + 1).toShort()
                    val i2 = (baseVert + (r + 1) * cols + c).toShort()
                    val i3 = (baseVert + (r + 1) * cols + c + 1).toShort()
                    // Verified strictly CCW outward: d/dr(+X) x d/dc(+Y) = +Z
                    indices[outIdx++] = i0; indices[outIdx++] = i2; indices[outIdx++] = i1
                    indices[outIdx++] = i1; indices[outIdx++] = i2; indices[outIdx++] = i3
                }
            }

            val cx = side * 0.256f
            for (r in 0 until rows) {
                val v = r.toFloat() / (rows - 1)
                val alpha = v * (0.66f * PI.toFloat())
                val sinA = sin(alpha)
                val cosA = cos(alpha)

                for (c in 0 until cols) {
                    val u = c.toFloat() / (cols - 1)
                    val phi = u * (2f * PI.toFloat())
                    val cosP = cos(phi)
                    val sinP = sin(phi)

                    val lnx = sinA * cosP
                    val lny = sinA * sinP
                    val lnz = cosA

                    val lx = cx + lnx * eyeRadius
                    val ly = eyeCenterY + lny * eyeRadius
                    val lz = eyeCenterZ + lnz * eyeRadius

                    val wPt = headLocalToWorld(lx, ly, lz)
                    val wN = headNormalToWorld(lnx, lny, lnz)

                    val irisR = sinA
                    // Soft natural upper eyelid cast shadow + lower-lid contact shadow + medial/lateral corner-canthus shadow
                    val upperLidShadow = 1f - smoothStep(-0.08f, 0.44f, lny) * 0.44f
                    val lowerLidShadow = 1f - smoothStep(-0.18f, -0.58f, lny) * 0.16f
                    val cornerCanthusShadow = 1f - smoothStep(0.36f, 0.84f, abs(lnx)) * 0.24f
                    val eyeLidAo = (upperLidShadow * lowerLidShadow * cornerCanthusShadow).coerceIn(0.38f, 1f)

                    val idx = (baseVert + r * cols + c) * stride
                    data[idx] = wPt[0]; data[idx + 1] = wPt[1]; data[idx + 2] = wPt[2]
                    data[idx + 3] = wN[0]; data[idx + 4] = wN[1]; data[idx + 5] = wN[2]
                    data[idx + 6] = irisR
                    data[idx + 7] = phi / (2f * PI.toFloat())
                    data[idx + 8] = eyeLidAo
                    data[idx + 9] = lny * 0.5f + 0.5f
                }
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return mesh
    }

    // -------------------------------------------------------------------------
    // 5. SOFT CHIN-TO-COLLARBONE LAYERED DARK-BROWN / SOFT-BLACK BOB (CAP + 30 LOCKS)
    // -------------------------------------------------------------------------
    fun buildBlossomHairMesh(betaSoftHair: Boolean = false): DynamicGlMesh {
        val capCols = 72
        val capRows = 48
        val capVerts = capCols * capRows
        val capIndices = (capCols - 1) * (capRows - 1) * 6

        data class LockSpec(
            val p0: FloatArray,
            val p1: FloatArray,
            val p2: FloatArray,
            val halfW: Float,
            val thick: Float,
            val shadeBias: Float
        )

        // Exact original clean side-swept fringe silhouette, overall shape, length & part preserved in BOTH baseline and Beta 6!
        val locks = listOf(
            // 4 Sleek close-hugging feathered temple & curtain-bang strands curving cleanly along hairline (zero slab edges!)
            LockSpec(floatArrayOf(0.22f, 0.86f, 0.36f), floatArrayOf(-0.16f, 0.69f, 0.60f), floatArrayOf(-0.62f, 0.22f, 0.38f), 0.145f, 0.018f, 0.18f),
            LockSpec(floatArrayOf(0.16f, 0.88f, 0.38f), floatArrayOf(-0.32f, 0.58f, 0.54f), floatArrayOf(-0.68f, 0.02f, 0.28f), 0.140f, 0.018f, 0.14f),
            LockSpec(floatArrayOf(0.26f, 0.85f, 0.36f), floatArrayOf(0.52f, 0.54f, 0.45f), floatArrayOf(0.68f, 0.02f, 0.28f), 0.140f, 0.018f, 0.16f),
            LockSpec(floatArrayOf(0.10f, 0.92f, 0.30f), floatArrayOf(-0.36f, 0.72f, 0.49f), floatArrayOf(-0.70f, 0.14f, 0.26f), 0.142f, 0.019f, 0.22f),

            // 10 Smooth face-framing layered bob side locks hugging temples, cheekbones & jawline along a cohesive curved cut line
            LockSpec(floatArrayOf(-0.34f, 0.78f, 0.30f), floatArrayOf(-0.72f, 0.16f, 0.29f), floatArrayOf(-0.70f, -0.92f, 0.15f), 0.132f, 0.028f, 0.16f),
            LockSpec(floatArrayOf(-0.42f, 0.72f, 0.16f), floatArrayOf(-0.77f, 0.02f, 0.14f), floatArrayOf(-0.76f, -1.04f, -0.02f), 0.142f, 0.030f, 0.12f),
            LockSpec(floatArrayOf(-0.38f, 0.76f, 0.23f), floatArrayOf(-0.74f, 0.08f, 0.22f), floatArrayOf(-0.68f, -0.96f, 0.09f), 0.124f, 0.026f, 0.22f),
            LockSpec(floatArrayOf(-0.26f, 0.86f, 0.12f), floatArrayOf(-0.75f, 0.22f, 0.15f), floatArrayOf(-0.79f, -1.08f, -0.10f), 0.138f, 0.030f, 0.18f),
            LockSpec(floatArrayOf(-0.46f, 0.60f, 0.02f), floatArrayOf(-0.79f, -0.14f, -0.04f), floatArrayOf(-0.82f, -1.18f, -0.22f), 0.146f, 0.032f, 0.10f),

            LockSpec(floatArrayOf(0.32f, 0.78f, 0.30f), floatArrayOf(0.72f, 0.16f, 0.29f), floatArrayOf(0.70f, -0.92f, 0.15f), 0.132f, 0.028f, 0.16f),
            LockSpec(floatArrayOf(0.40f, 0.72f, 0.16f), floatArrayOf(0.77f, 0.02f, 0.14f), floatArrayOf(0.76f, -1.04f, -0.02f), 0.142f, 0.030f, 0.12f),
            LockSpec(floatArrayOf(0.30f, 0.76f, 0.34f), floatArrayOf(0.68f, 0.20f, 0.34f), floatArrayOf(0.68f, -0.94f, 0.18f), 0.124f, 0.026f, 0.22f),
            LockSpec(floatArrayOf(0.26f, 0.86f, 0.14f), floatArrayOf(0.75f, 0.22f, 0.16f), floatArrayOf(0.79f, -1.08f, -0.08f), 0.138f, 0.030f, 0.18f),
            LockSpec(floatArrayOf(0.46f, 0.60f, 0.02f), floatArrayOf(0.79f, -0.14f, -0.04f), floatArrayOf(0.82f, -1.18f, -0.22f), 0.146f, 0.032f, 0.10f),

            // 10 Cleanly aligned nape & pillow-draping lower bob locks tapering along an even, softly layered chin-to-nape arc
            LockSpec(floatArrayOf(-0.56f, 0.26f, -0.18f), floatArrayOf(-0.88f, -0.44f, -0.26f), floatArrayOf(-0.96f, -1.28f, -0.34f), 0.162f, 0.034f, 0.14f),
            LockSpec(floatArrayOf(-0.48f, 0.14f, -0.34f), floatArrayOf(-0.82f, -0.58f, -0.42f), floatArrayOf(-0.98f, -1.34f, -0.46f), 0.168f, 0.034f, 0.08f),
            LockSpec(floatArrayOf(-0.34f, 0.02f, -0.48f), floatArrayOf(-0.68f, -0.68f, -0.54f), floatArrayOf(-0.86f, -1.38f, -0.54f), 0.164f, 0.032f, 0.12f),
            LockSpec(floatArrayOf(-0.54f, 0.42f, -0.08f), floatArrayOf(-0.86f, -0.26f, -0.18f), floatArrayOf(-0.92f, -1.24f, -0.28f), 0.154f, 0.032f, 0.20f),
            LockSpec(floatArrayOf(-0.62f, 0.04f, 0.04f), floatArrayOf(-0.79f, -0.56f, 0.02f), floatArrayOf(-0.82f, -1.22f, -0.14f), 0.138f, 0.030f, 0.18f),

            LockSpec(floatArrayOf(0.56f, 0.26f, -0.18f), floatArrayOf(0.88f, -0.44f, -0.26f), floatArrayOf(0.96f, -1.28f, -0.34f), 0.162f, 0.034f, 0.14f),
            LockSpec(floatArrayOf(0.48f, 0.14f, -0.34f), floatArrayOf(0.82f, -0.58f, -0.42f), floatArrayOf(0.98f, -1.34f, -0.46f), 0.168f, 0.034f, 0.08f),
            LockSpec(floatArrayOf(0.34f, 0.02f, -0.48f), floatArrayOf(0.68f, -0.68f, -0.54f), floatArrayOf(0.86f, -1.38f, -0.54f), 0.164f, 0.032f, 0.12f),
            LockSpec(floatArrayOf(0.54f, 0.42f, -0.08f), floatArrayOf(0.86f, -0.26f, -0.18f), floatArrayOf(0.92f, -1.24f, -0.28f), 0.154f, 0.032f, 0.20f),
            LockSpec(floatArrayOf(0.62f, 0.04f, 0.04f), floatArrayOf(0.79f, -0.56f, 0.02f), floatArrayOf(0.82f, -1.22f, -0.14f), 0.138f, 0.030f, 0.18f),

            // 6 Layered dimensional crown volume locks (gives natural glossy silhouette from every angle)
            LockSpec(floatArrayOf(0.14f, 1.01f, 0.08f), floatArrayOf(-0.22f, 0.92f, 0.40f), floatArrayOf(-0.58f, 0.54f, 0.44f), 0.155f, 0.038f, 0.24f),
            LockSpec(floatArrayOf(0.24f, 0.99f, 0.02f), floatArrayOf(0.56f, 0.78f, 0.24f), floatArrayOf(0.76f, 0.30f, 0.20f), 0.152f, 0.036f, 0.18f),
            LockSpec(floatArrayOf(-0.08f, 1.01f, -0.08f), floatArrayOf(-0.52f, 0.76f, 0.16f), floatArrayOf(-0.76f, 0.20f, 0.10f), 0.152f, 0.036f, 0.14f),
            LockSpec(floatArrayOf(0.06f, 1.04f, -0.14f), floatArrayOf(-0.36f, 0.90f, -0.04f), floatArrayOf(-0.72f, 0.40f, -0.12f), 0.155f, 0.036f, 0.20f),
            LockSpec(floatArrayOf(0.12f, 1.02f, -0.16f), floatArrayOf(0.48f, 0.84f, -0.04f), floatArrayOf(0.75f, 0.34f, -0.12f), 0.152f, 0.035f, 0.16f),
            LockSpec(floatArrayOf(-0.02f, 1.04f, 0.14f), floatArrayOf(0.28f, 0.94f, 0.38f), floatArrayOf(0.60f, 0.56f, 0.40f), 0.148f, 0.035f, 0.22f)
        )

        val lockCols = 11
        val lockRows = 18
        val vertsPerLock = lockCols * lockRows
        val trisPerLock = (lockCols - 1) * (lockRows - 1) * 6

        val totalVerts = capVerts + locks.size * vertsPerLock
        val totalIndices = capIndices + locks.size * trisPerLock
        val mesh = DynamicGlMesh(totalVerts, totalIndices)
        val indices = ShortArray(totalIndices)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        var idxOut = 0
        for (r in 0 until capRows - 1) {
            for (c in 0 until capCols - 1) {
                val i0 = (r * capCols + c).toShort()
                val i1 = (r * capCols + c + 1).toShort()
                val i2 = ((r + 1) * capCols + c).toShort()
                val i3 = ((r + 1) * capCols + c + 1).toShort()
                indices[idxOut++] = i0; indices[idxOut++] = i2; indices[idxOut++] = i1
                indices[idxOut++] = i1; indices[idxOut++] = i2; indices[idxOut++] = i3
            }
        }

        // Part A: Layered Bob Crown & Clean Smooth-Hem Nape Volume Cap (exact silhouette preserved!)
        for (r in 0 until capRows) {
            val v = r.toFloat() / (capRows - 1)
            for (c in 0 until capCols) {
                val u = (c.toFloat() / (capCols - 1)) * 2f - 1f
                val phi = u * PI.toFloat()
                val sinP = sin(phi)
                val cosP = cos(phi)

                val frontMask = smoothStep(-0.25f, 0.72f, cosP)
                val backMask = smoothStep(0.05f, 0.85f, -cosP)
                val sideMask = smoothStep(0.28f, 0.88f, abs(sinP))
                val partLift = 0.052f * smoothStep(-0.20f, 0.56f, sinP) * frontMask
                val maxTheta = 0.63f * PI.toFloat() -
                    (0.265f + partLift) * PI.toFloat() * frontMask +
                    0.22f * PI.toFloat() * backMask +
                    0.12f * PI.toFloat() * sideMask * (1f - frontMask)

                val theta = (0.008f + v * (maxTheta / PI.toFloat())).coerceAtMost(0.85f) * PI.toFloat()
                val sinT = sin(theta)
                val cosT = cos(theta)

                val crownVolume = 0.112f * (1f - smoothStep(0.26f, 0.90f, v)) + 0.042f * smoothStep(0.18f, 0.82f, v)
                val edgeTaper = 1f - 0.94f * smoothStep(0.74f, 1.0f, v)
                val strandRib = 0.0042f * sin(phi * 20f + v * 4.0f) + 0.0024f * cos(phi * 36f - v * 5.6f)
                val loft = (crownVolume * edgeTaper + strandRib * (1f - smoothStep(0.78f, 1.0f, v))).coerceAtLeast(0.003f)

                val craniumBack = 1f + 0.09f * backMask * sinT
                val napeBlend = smoothStep(-0.12f, 0.65f, -cosP)
                val pillowFan = 0.135f * smoothStep(0.60f, 0.96f, v) * napeBlend * (1f - 0.25f * smoothStep(0.88f, 1.0f, v))
                val lx = sinT * sinP * (0.682f + loft + pillowFan)
                val ly = cosT * (0.975f + loft * 0.90f) - 0.18f * smoothStep(0.66f, 1.0f, v) * napeBlend
                val lz = sinT * cosP * (0.778f * craniumBack + loft)

                val wPt = headLocalToWorld(lx, ly, lz)
                // Beta 6 adds finer strand-direction normal detail along the cap without altering silhouette
                val strandNormRib = if (betaSoftHair) {
                    0.056f * cos(phi * 28f + v * 3.8f) + 0.036f * sin(phi * 54f - v * 4.6f)
                } else {
                    0.042f * cos(phi * 20f + v * 4.0f)
                }
                val lnx = sinT * sinP + strandNormRib * cosP
                val lny = cosT
                val lnz = sinT * cosP - strandNormRib * sinP
                val wN = headNormalToWorld(lnx, lny, lnz)

                val strandShade = if (betaSoftHair) {
                    (0.5f + 0.32f * sin(phi * 28f + v * 3.8f) + 0.18f * cos(phi * 54f - v * 4.6f)).coerceIn(0f, 1f)
                } else {
                    0.5f + 0.5f * sin(phi * 26f + v * 5.0f)
                }
                val ao = (0.78f + 0.22f * (1f - smoothStep(0.62f, 1.0f, v)) * (0.78f + 0.22f * strandShade)).coerceIn(0.45f, 1f)

                val idx = (r * capCols + c) * stride
                data[idx] = wPt[0]; data[idx + 1] = wPt[1]; data[idx + 2] = wPt[2]
                data[idx + 3] = wN[0]; data[idx + 4] = wN[1]; data[idx + 5] = wN[2]
                data[idx + 6] = v
                data[idx + 7] = strandShade
                data[idx + 8] = ao
                data[idx + 9] = 0.08f
            }
        }

        // Part B: 30 Sleek Feathered Layered Bob Hair Locks (verified outward CCW winding!)
        for ((lockIdx, lock) in locks.withIndex()) {
            val baseVert = capVerts + lockIdx * vertsPerLock
            for (r in 0 until lockRows - 1) {
                for (c in 0 until lockCols - 1) {
                    val i0 = (baseVert + r * lockCols + c).toShort()
                    val i1 = (baseVert + r * lockCols + c + 1).toShort()
                    val i2 = (baseVert + (r + 1) * lockCols + c).toShort()
                    val i3 = (baseVert + (r + 1) * lockCols + c + 1).toShort()
                    // B = T x O => d/dc(B) x d/dr(T) = B x T = +O (OUTWARD CCW!)
                    indices[idxOut++] = i0; indices[idxOut++] = i1; indices[idxOut++] = i2
                    indices[idxOut++] = i1; indices[idxOut++] = i3; indices[idxOut++] = i2
                }
            }

            // Deterministic per-lock subtle strand color/highlight variation (strictly Beta 6)
            val lockColorJitter = if (betaSoftHair) {
                0.075f * sin(lockIdx * 2.17f + 0.6f) + 0.040f * cos(lockIdx * 3.91f)
            } else 0f

            for (r in 0 until lockRows) {
                val t = r.toFloat() / (lockRows - 1)
                val omt = 1f - t
                val cx = omt * omt * lock.p0[0] + 2f * omt * t * lock.p1[0] + t * t * lock.p2[0]
                val cy = omt * omt * lock.p0[1] + 2f * omt * t * lock.p1[1] + t * t * lock.p2[1]
                val cz = omt * omt * lock.p0[2] + 2f * omt * t * lock.p1[2] + t * t * lock.p2[2]

                var tx = 2f * omt * (lock.p1[0] - lock.p0[0]) + 2f * t * (lock.p2[0] - lock.p1[0])
                var ty = 2f * omt * (lock.p1[1] - lock.p0[1]) + 2f * t * (lock.p2[1] - lock.p1[1])
                var tz = 2f * omt * (lock.p1[2] - lock.p0[2]) + 2f * t * (lock.p2[2] - lock.p1[2])
                val tLen = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(1e-5f)
                tx /= tLen; ty /= tLen; tz /= tLen

                val drapeW = smoothStep(-0.15f, -0.85f, cy)
                var ox = cx * (1f - 0.45f * drapeW)
                var oy = (cy - 0.12f) * (1f - drapeW) + 0.06f * drapeW
                var oz = cz * (1f - drapeW) + 0.92f * drapeW
                val oDotT = ox * tx + oy * ty + oz * tz
                ox -= tx * oDotT; oy -= ty * oDotT; oz -= tz * oDotT
                val oLen = sqrt(ox * ox + oy * oy + oz * oz).coerceAtLeast(1e-5f)
                ox /= oLen; oy /= oLen; oz /= oLen

                val bx = ty * oz - tz * oy
                val by = tz * ox - tx * oz
                val bz = tx * oy - ty * ox

                // Fine silky feathered width & thickness tapering smoothly to soft points (Beta 6 adds slightly finer tapered tips)
                val rootEnv = 0.62f + 0.38f * smoothStep(0.0f, 0.18f, t)
                val tipWidthFade = if (betaSoftHair) {
                    1f - 0.91f * smoothStep(0.58f, 1.0f, t)
                } else {
                    1f - 0.86f * smoothStep(0.62f, 1.0f, t)
                }
                val curW = lock.halfW * 1.06f * rootEnv * (0.86f + 0.16f * sin(t * PI.toFloat())) * tipWidthFade
                val tipThickFade = if (betaSoftHair) {
                    1f - 0.90f * smoothStep(0.56f, 1.0f, t)
                } else {
                    1f - 0.85f * smoothStep(0.58f, 1.0f, t)
                }
                val curThick = lock.thick * 0.40f * rootEnv * (0.45f + 0.55f * sin(t * PI.toFloat())) * tipThickFade
                val tipZone = smoothStep(0.70f, 1.0f, t)

                for (c in 0 until lockCols) {
                    val u = (c.toFloat() / (lockCols - 1)) * 2f - 1f
                    val arch = cos(u * (PI.toFloat() * 0.5f))
                    // Beta 6: subtle irregular strand-end taper strictly at the lock tips (preserves exact overall shape & length!)
                    val tipIrregular = if (betaSoftHair && tipZone > 0.001f) {
                        (0.022f * sin(u * 4.8f + lockIdx * 1.73f) + 0.012f * cos(u * 9.1f - lockIdx * 2.41f)) * tipZone
                    } else 0f
                    val lx = cx + bx * (u * curW) + ox * (arch * curThick) + tx * tipIrregular
                    val ly = cy + by * (u * curW) + oy * (arch * curThick) + ty * tipIrregular
                    val lz = cz + bz * (u * curW) + oz * (arch * curThick) + tz * tipIrregular

                    // Beta 6: fine cross-strand normal ripple aligned cleanly along each lock's flow direction
                    val strandRipple = if (betaSoftHair) {
                        (0.14f * sin(u * 13.5f + lockIdx * 1.3f) + 0.07f * cos(u * 23.0f - lockIdx * 2.1f)) * arch
                    } else 0f
                    val lnx = ox * (0.85f * arch + 0.15f) + bx * (u * 0.52f + strandRipple)
                    val lny = oy * (0.85f * arch + 0.15f) + by * (u * 0.52f + strandRipple)
                    val lnz = oz * (0.85f * arch + 0.15f) + bz * (u * 0.52f + strandRipple)

                    val wPt = headLocalToWorld(lx, ly, lz)
                    val minHairZ = BlossomBodyBuilder.mattressSurfaceZ(wPt[1]) + 0.03f
                    if (wPt[2] < minHairZ) wPt[2] = minHairZ
                    val wN = headNormalToWorld(lnx, lny, lnz)

                    val strandRibVal = if (betaSoftHair) {
                        (0.5f + 0.34f * cos(u * PI.toFloat()) + 0.16f * sin(u * 13.5f + lockIdx * 1.3f)).coerceIn(0f, 1f)
                    } else {
                        0.5f + 0.5f * cos(u * PI.toFloat())
                    }
                    val idx = (baseVert + r * lockCols + c) * stride
                    data[idx] = wPt[0]; data[idx + 1] = wPt[1]; data[idx + 2] = wPt[2]
                    data[idx + 3] = wN[0]; data[idx + 4] = wN[1]; data[idx + 5] = wN[2]
                    data[idx + 6] = t
                    data[idx + 7] = strandRibVal
                    data[idx + 8] = (0.72f + 0.28f * arch).coerceIn(0.55f, 1f)
                    data[idx + 9] = (lock.shadeBias + lockColorJitter).coerceIn(0.02f, 0.42f)
                }
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return mesh
    }

    // -------------------------------------------------------------------------
    // 6. PERSISTENT HEAD TURN + SUBTLE DOWNWARD NOD + NATURAL EYELID BLINK RIG
    //    Disjoint from ArmSkinCache (ax >= 1.12) & LegSkinCache (y <= 0.44)!
    // -------------------------------------------------------------------------
    class HeadRigCache(
        val headRestPosNorm: FloatArray,
        val eyesRestPosNorm: FloatArray,
        val hairRestPosNorm: FloatArray,
        val neckBodyIndices: IntArray,
        val neckRestPosNorm: FloatArray,
        val neckWeights: FloatArray,
        val blinkVertIndices: IntArray,
        val blinkClosedPosNorm: FloatArray,
        val blinkVertIsRight: BooleanArray,
        val smileMorphIndices: IntArray,
        val smileMorphDeltaPosNorm: FloatArray,
        val browMorphIndices: IntArray,
        val browMorphDeltaPosNorm: FloatArray,
        val lipSoftMorphIndices: IntArray,
        val lipSoftMorphDeltaPosNorm: FloatArray,
        val upperLidLiftIndices: IntArray,
        val upperLidLiftIsRight: BooleanArray,
        val upperLidLiftDeltaPosNorm: FloatArray,
        val swallowJawIndices: IntArray,
        val swallowJawDeltaPosNorm: FloatArray,
        val nostrilMorphIndices: IntArray,
        val nostrilMorphDeltaPosNorm: FloatArray,
        val throatNeckLocalIndices: IntArray,
        val throatNeckY: FloatArray,
        val throatNeckWeights: FloatArray
    )

    fun buildHeadRigCache(
        bodyMesh: DynamicGlMesh,
        headMesh: DynamicGlMesh,
        eyesMesh: DynamicGlMesh,
        hairMesh: DynamicGlMesh
    ): HeadRigCache {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        fun copyPosNorm(mesh: DynamicGlMesh): FloatArray {
            val src = mesh.vertexData
            val out = FloatArray(mesh.vertexCount * 6)
            for (i in 0 until mesh.vertexCount) {
                val s = i * stride
                val d = i * 6
                out[d] = src[s]; out[d + 1] = src[s + 1]; out[d + 2] = src[s + 2]
                out[d + 3] = src[s + 3]; out[d + 4] = src[s + 4]; out[d + 5] = src[s + 5]
            }
            return out
        }

        val headRest = copyPosNorm(headMesh)

        // Precompute sparse upper-eyelid blink targets in world space wrapping cleanly over both eyeballs
        // with strictly monotonic vertical row order (zero fold-over, zero eyeball clipping!).
        val eyeRadius = 0.136f
        val eyeWrapR = eyeRadius + 0.016f
        val eyeCenterY = 0.038f
        val eyeCenterZ = 0.528f
        val blinkIdxBuf = IntArray(headMesh.vertexCount)
        val blinkPosBuf = FloatArray(headMesh.vertexCount * 6)
        val blinkRightBuf = BooleanArray(headMesh.vertexCount)
        var blinkCount = 0

        val lidLiftIdxBuf = IntArray(headMesh.vertexCount)
        val lidLiftRightBuf = BooleanArray(headMesh.vertexCount)
        val lidLiftDeltaBuf = FloatArray(headMesh.vertexCount * 6)
        var lidLiftCount = 0

        fun localDeltaToWorldPre(dlx: Float, dly: Float, dlz: Float): FloatArray {
            val sx = dlx * HEAD_SCALE
            val sy = dly * HEAD_SCALE
            val sz = dlz * HEAD_SCALE
            return floatArrayOf(sx, sy * COS_PITCH - sz * SIN_PITCH, sy * SIN_PITCH + sz * COS_PITCH)
        }

        for (i in 0 until headMesh.vertexCount) {
            val s = i * 6
            val local = headWorldToLocal(headRest[s], headRest[s + 1], headRest[s + 2])
            val lx = local[0]
            val ly = local[1]
            val lz = local[2]
            if (lz < 0.30f) continue

            val side = if (lx >= 0f) 1f else -1f
            val ax = abs(lx)
            val eyeU = (ax - 0.256f) / 0.126f
            if (abs(eyeU) >= 1.04f) continue

            val clampedU = eyeU.coerceIn(-1f, 1f)
            val eyeSpan = (1f - clampedU * clampedU).coerceAtLeast(0f)
            val upperLidY = 0.036f + (0.056f + 0.008f * clampedU) * sqrt(eyeSpan)
            val lowerLidY = 0.036f - (0.036f - 0.006f * clampedU) * sqrt(eyeSpan)
            val lidTop = upperLidY + 0.066f
            val lidBot = lowerLidY + 0.002f
            if (ly <= lidBot || ly >= lidTop) continue

            val horizW = smoothStep(1.04f, 0.68f, abs(eyeU))
            if (horizW <= 0.002f) continue

            val targetLy: Float
            var targetLz = lz
            var useSphereNormalW = 0f

            if (ly >= upperLidY) {
                val tAbove = ((ly - upperLidY) / (lidTop - upperLidY).coerceAtLeast(1e-4f)).coerceIn(0f, 1f)
                val seamY = lowerLidY + 0.004f
                val closedLy = seamY + tAbove * (lidTop - seamY)
                targetLy = ly * (1f - horizW) + closedLy * horizW

                val dxEye = lx - side * 0.256f
                val dyEye = targetLy - eyeCenterY
                val rad2 = dxEye * dxEye + dyEye * dyEye
                val wrapW = horizW * (1f - smoothStep(0.68f, 1.0f, tAbove))
                if (rad2 < eyeWrapR * eyeWrapR && wrapW > 0.001f) {
                    val sphereZ = eyeCenterZ + sqrt(eyeWrapR * eyeWrapR - rad2)
                    val blendedZ = lz * (1f - wrapW) + sphereZ * wrapW
                    targetLz = max(lz, blendedZ)
                    useSphereNormalW = wrapW
                }
            } else {
                // Recessed inner-aperture vertices compress smoothly behind the lower-lid closure seam
                val tBelow = ((ly - lidBot) / (upperLidY - lidBot).coerceAtLeast(1e-4f)).coerceIn(0f, 1f)
                val closedLy = lidBot + tBelow * 0.002f
                targetLy = ly * (1f - horizW) + closedLy * horizW
            }

            val closedW = headLocalToWorld(lx, targetLy, targetLz)
            var cnx = headRest[s + 3]
            var cny = headRest[s + 4]
            var cnz = headRest[s + 5]
            if (useSphereNormalW > 0.001f) {
                val dxEye = lx - side * 0.256f
                val dyEye = targetLy - eyeCenterY
                val dzEye = (targetLz - eyeCenterZ).coerceAtLeast(0.04f)
                val sphereWorldN = headNormalToWorld(dxEye, dyEye, dzEye)
                val blendN = useSphereNormalW * 0.78f
                cnx = cnx * (1f - blendN) + sphereWorldN[0] * blendN
                cny = cny * (1f - blendN) + sphereWorldN[1] * blendN
                cnz = cnz * (1f - blendN) + sphereWorldN[2] * blendN
                val nLen = sqrt(cnx * cnx + cny * cny + cnz * cnz).coerceAtLeast(1e-5f)
                cnx /= nLen; cny /= nLen; cnz /= nLen
            }

            blinkIdxBuf[blinkCount] = i
            blinkRightBuf[blinkCount] = lx >= 0f
            val d = blinkCount * 6
            blinkPosBuf[d] = closedW[0]; blinkPosBuf[d + 1] = closedW[1]; blinkPosBuf[d + 2] = closedW[2]
            blinkPosBuf[d + 3] = cnx; blinkPosBuf[d + 4] = cny; blinkPosBuf[d + 5] = cnz
            blinkCount++

            // Subtle upper-eyelid tracking lift for gentle upward gaze (zero fold-over, fades smoothly at rim & crease)
            if (ly >= upperLidY) {
                val tAbove = ((ly - upperLidY) / (lidTop - upperLidY).coerceAtLeast(1e-4f)).coerceIn(0f, 1f)
                val liftEnv = horizW * (1f - smoothStep(0.18f, 0.96f, tAbove)) * smoothStep(0.0f, 0.14f, tAbove + 0.08f)
                if (liftEnv > 0.01f) {
                    val dlyUp = 0.0082f * liftEnv
                    val dlzBack = -0.0024f * liftEnv
                    val dwUp = localDeltaToWorldPre(0f, dlyUp, dlzBack)
                    val dnwUp = localDeltaToWorldPre(0f, 0.03f * liftEnv, 0f)
                    lidLiftIdxBuf[lidLiftCount] = i
                    lidLiftRightBuf[lidLiftCount] = lx >= 0f
                    val ld = lidLiftCount * 6
                    lidLiftDeltaBuf[ld] = dwUp[0]; lidLiftDeltaBuf[ld + 1] = dwUp[1]; lidLiftDeltaBuf[ld + 2] = dwUp[2]
                    lidLiftDeltaBuf[ld + 3] = dnwUp[0]; lidLiftDeltaBuf[ld + 4] = dnwUp[1]; lidLiftDeltaBuf[ld + 5] = dnwUp[2]
                    lidLiftCount++
                }
            }
        }

        // Capture upper cervical neck vertices in blossomBodyMesh (strictly ax < 0.58, y >= 4.92 -> 100% disjoint from arms & legs)
        val bData = bodyMesh.vertexData
        val nVerts = bodyMesh.vertexCount
        val idxBuf = IntArray(nVerts)
        val wBuf = FloatArray(nVerts)
        var count = 0
        for (i in 0 until nVerts) {
            val base = i * stride
            val ax = abs(bData[base])
            val y = bData[base + 1]
            if (y in 4.92f..6.05f && ax < 0.58f) {
                val w = smoothStep(4.92f, 5.72f, y) * (1f - smoothStep(0.38f, 0.58f, ax))
                if (w > 0.002f) {
                    idxBuf[count] = i
                    wBuf[count] = w
                    count++
                }
            }
        }
        val neckIndices = idxBuf.copyOf(count)
        val neckWeights = wBuf.copyOf(count)
        val neckRest = FloatArray(count * 6)
        val throatLocalIdxBuf = IntArray(count)
        val throatYBuf = FloatArray(count)
        val throatWBuf = FloatArray(count)
        var throatCount = 0
        for (k in 0 until count) {
            val s = neckIndices[k] * stride
            val d = k * 6
            val nxPos = bData[s]
            val nyPos = bData[s + 1]
            val nzPos = bData[s + 2]
            val nnx = bData[s + 3]
            val nny = bData[s + 4]
            val nnz = bData[s + 5]
            neckRest[d] = nxPos; neckRest[d + 1] = nyPos; neckRest[d + 2] = nzPos
            neckRest[d + 3] = nnx; neckRest[d + 4] = nny; neckRest[d + 5] = nnz
            // Capture anterior mid-throat vertices strictly for subtle swallowing wave (0 on breasts/back/face)
            val axT = abs(nxPos)
            if (nyPos in 5.04f..5.74f && axT < 0.24f && nnz > 0.35f) {
                val tw = (1f - smoothStep(0.06f, 0.24f, axT)) *
                    smoothStep(0.35f, 0.72f, nnz) *
                    smoothStep(5.04f, 5.14f, nyPos) *
                    (1f - smoothStep(5.64f, 5.74f, nyPos))
                if (tw > 0.005f) {
                    throatLocalIdxBuf[throatCount] = k
                    throatYBuf[throatCount] = nyPos
                    throatWBuf[throatCount] = tw
                    throatCount++
                }
            }
        }

        // Precompute sparse subtle facial expression morph targets (all strictly localized to anterior face
        // above jaw rim y > 5.81 -> 100% disjoint from neck seam beta and zero overhead when weights are 0f!)
        val smileIdxBuf = IntArray(headMesh.vertexCount)
        val smileDeltaBuf = FloatArray(headMesh.vertexCount * 6)
        var smileCount = 0

        val browIdxBuf = IntArray(headMesh.vertexCount)
        val browDeltaBuf = FloatArray(headMesh.vertexCount * 6)
        var browCount = 0

        val lipSoftIdxBuf = IntArray(headMesh.vertexCount)
        val lipSoftDeltaBuf = FloatArray(headMesh.vertexCount * 6)
        var lipSoftCount = 0

        val swallowJawIdxBuf = IntArray(headMesh.vertexCount)
        val swallowJawDeltaBuf = FloatArray(headMesh.vertexCount * 6)
        var swallowJawCount = 0

        val nostrilIdxBuf = IntArray(headMesh.vertexCount)
        val nostrilDeltaBuf = FloatArray(headMesh.vertexCount * 6)
        var nostrilCount = 0

        fun localDeltaToWorld(dlx: Float, dly: Float, dlz: Float): FloatArray {
            val sx = dlx * HEAD_SCALE
            val sy = dly * HEAD_SCALE
            val sz = dlz * HEAD_SCALE
            return floatArrayOf(sx, sy * COS_PITCH - sz * SIN_PITCH, sy * SIN_PITCH + sz * COS_PITCH)
        }

        for (i in 0 until headMesh.vertexCount) {
            val s = i * 6
            val wy = headRest[s + 1]
            if (wy <= 5.81f) continue
            val local = headWorldToLocal(headRest[s], wy, headRest[s + 2])
            val lx = local[0]
            val ly = local[1]
            val lz = local[2]
            if (lz < 0.28f) continue
            val ax = abs(lx)
            val side = if (lx >= 0f) 1f else -1f

            // 1. Subtle serene micro-smile warmth (gentle mouth-corner lift, soft cheek apple rise, delicate eye-corner warmth)
            if (ly in -0.58f..0.06f && ax < 0.48f) {
                val cornerW = exp(-(((ax - 0.222f) / 0.088f).let { it * it } + ((ly + 0.432f) / 0.065f).let { it * it }))
                val midLipEase = exp(-((ax / 0.14f).let { it * it } + ((ly + 0.440f) / 0.048f).let { it * it })) * 0.22f
                val cheekW = exp(-(((ax - 0.315f) / 0.130f).let { it * it } + ((ly + 0.155f) / 0.115f).let { it * it }))
                val lowerEyeWarm = exp(-(((ax - 0.275f) / 0.095f).let { it * it } + ((ly + 0.014f) / 0.024f).let { it * it })) *
                    smoothStep(0.015f, -0.008f, ly)
                val dlx = side * (0.0085f * cornerW + 0.0040f * cheekW)
                val dly = 0.0175f * cornerW + 0.0035f * midLipEase + 0.0090f * cheekW + 0.0048f * lowerEyeWarm
                val dlz = -0.0065f * cornerW + 0.0055f * cheekW + 0.0020f * lowerEyeWarm
                if (abs(dlx) + abs(dly) + abs(dlz) > 1e-4f) {
                    val dw = localDeltaToWorld(dlx, dly, dlz)
                    val dnw = localDeltaToWorld(0f, 0.06f * cheekW - 0.05f * cornerW, 0.04f * cheekW)
                    smileIdxBuf[smileCount] = i
                    val d = smileCount * 6
                    smileDeltaBuf[d] = dw[0]; smileDeltaBuf[d + 1] = dw[1]; smileDeltaBuf[d + 2] = dw[2]
                    smileDeltaBuf[d + 3] = dnw[0]; smileDeltaBuf[d + 4] = dnw[1]; smileDeltaBuf[d + 5] = dnw[2]
                    smileCount++
                }
            }

            // 2. Subtle expressive brow lift / gentle thoughtful soften (strictly above upper eyelid crease!)
            if (ly in 0.115f..0.34f && ax < 0.48f) {
                val browArchY = 0.172f + 0.034f * exp(-((ax - 0.332f) / 0.105f).let { it * it }) - 0.040f * smoothStep(0.345f, 0.455f, ax)
                val browEnv = exp(-((ly - browArchY) / 0.058f).let { it * it }) *
                    smoothStep(0.04f, 0.13f, ax) * (1f - smoothStep(0.39f, 0.48f, ax)) *
                    smoothStep(0.115f, 0.145f, ly)
                val innerBrowAccent = exp(-(((ax - 0.165f) / 0.085f).let { it * it })) * browEnv
                val dlx = side * (-0.0025f * innerBrowAccent)
                val dly = 0.0155f * browEnv + 0.0045f * innerBrowAccent
                val dlz = 0.0032f * browEnv
                if (abs(dlx) + abs(dly) + abs(dlz) > 1e-4f) {
                    val dw = localDeltaToWorld(dlx, dly, dlz)
                    val dnw = localDeltaToWorld(0f, -0.05f * browEnv, 0.02f * browEnv)
                    browIdxBuf[browCount] = i
                    val d = browCount * 6
                    browDeltaBuf[d] = dw[0]; browDeltaBuf[d + 1] = dw[1]; browDeltaBuf[d + 2] = dw[2]
                    browDeltaBuf[d + 3] = dnw[0]; browDeltaBuf[d + 4] = dnw[1]; browDeltaBuf[d + 5] = dnw[2]
                    browCount++
                }
            }

            // 3. Ultra-subtle relaxed breath-part / lip ease on lower lip & chin cushion
            if (ly in -0.56f..-0.40f && ax < 0.22f) {
                val lowerLipEase = exp(-((ax / 0.145f).let { it * it } + ((ly + 0.472f) / 0.044f).let { it * it })) *
                    smoothStep(-0.415f, -0.446f, ly)
                val dly = -0.0072f * lowerLipEase
                val dlz = 0.0038f * lowerLipEase
                if (abs(dly) + abs(dlz) > 1e-4f) {
                    val dw = localDeltaToWorld(0f, dly, dlz)
                    val dnw = localDeltaToWorld(0f, -0.04f * lowerLipEase, 0.02f * lowerLipEase)
                    lipSoftIdxBuf[lipSoftCount] = i
                    val d = lipSoftCount * 6
                    lipSoftDeltaBuf[d] = dw[0]; lipSoftDeltaBuf[d + 1] = dw[1]; lipSoftDeltaBuf[d + 2] = dw[2]
                    lipSoftDeltaBuf[d + 3] = dnw[0]; lipSoftDeltaBuf[d + 4] = dnw[1]; lipSoftDeltaBuf[d + 5] = dnw[2]
                    lipSoftCount++
                }
            }

            // 4. Tab 3 Lifelike+: Ultra-subtle swallow jaw/chin ease & quiet lip seal (strictly wy > 5.81)
            if (ly in -0.68f..-0.40f && ax < 0.28f) {
                val chinEaseW = exp(-((ax / 0.165f).let { it * it } + ((ly + 0.565f) / 0.080f).let { it * it }))
                val lipSealW = exp(-((ax / 0.140f).let { it * it } + ((ly + 0.468f) / 0.036f).let { it * it }))
                val dlyS = 0.0042f * lipSealW - 0.0052f * chinEaseW
                val dlzS = -0.0050f * chinEaseW - 0.0018f * lipSealW
                if (abs(dlyS) + abs(dlzS) > 1e-4f) {
                    val dw = localDeltaToWorld(0f, dlyS, dlzS)
                    val dnw = localDeltaToWorld(0f, -0.03f * chinEaseW, 0.02f * chinEaseW)
                    swallowJawIdxBuf[swallowJawCount] = i
                    val d = swallowJawCount * 6
                    swallowJawDeltaBuf[d] = dw[0]; swallowJawDeltaBuf[d + 1] = dw[1]; swallowJawDeltaBuf[d + 2] = dw[2]
                    swallowJawDeltaBuf[d + 3] = dnw[0]; swallowJawDeltaBuf[d + 4] = dnw[1]; swallowJawDeltaBuf[d + 5] = dnw[2]
                    swallowJawCount++
                }
            }

            // 5. Tab 3 Lifelike+: Whisper-soft nasal alar wing flare synced with breathing (noseAla centered at ax=0.090, ly=-0.244)
            if (ly in -0.31f..-0.17f && ax in 0.032f..0.165f && lz > 0.38f) {
                val alarW = exp(-(((ax - 0.092f) / 0.046f).let { it * it } + ((ly + 0.244f) / 0.042f).let { it * it })) *
                    smoothStep(0.032f, 0.056f, ax)
                val dlxN = side * 0.0054f * alarW
                val dlyN = 0.0022f * alarW
                val dlzN = 0.0020f * alarW
                if (abs(dlxN) + abs(dlyN) + abs(dlzN) > 1e-4f) {
                    val dw = localDeltaToWorld(dlxN, dlyN, dlzN)
                    val dnw = localDeltaToWorld(side * 0.035f * alarW, 0.015f * alarW, 0f)
                    nostrilIdxBuf[nostrilCount] = i
                    val d = nostrilCount * 6
                    nostrilDeltaBuf[d] = dw[0]; nostrilDeltaBuf[d + 1] = dw[1]; nostrilDeltaBuf[d + 2] = dw[2]
                    nostrilDeltaBuf[d + 3] = dnw[0]; nostrilDeltaBuf[d + 4] = dnw[1]; nostrilDeltaBuf[d + 5] = dnw[2]
                    nostrilCount++
                }
            }
        }

        return HeadRigCache(
            headRestPosNorm = headRest,
            eyesRestPosNorm = copyPosNorm(eyesMesh),
            hairRestPosNorm = copyPosNorm(hairMesh),
            neckBodyIndices = neckIndices,
            neckRestPosNorm = neckRest,
            neckWeights = neckWeights,
            blinkVertIndices = blinkIdxBuf.copyOf(blinkCount),
            blinkClosedPosNorm = blinkPosBuf.copyOf(blinkCount * 6),
            blinkVertIsRight = blinkRightBuf.copyOf(blinkCount),
            smileMorphIndices = smileIdxBuf.copyOf(smileCount),
            smileMorphDeltaPosNorm = smileDeltaBuf.copyOf(smileCount * 6),
            browMorphIndices = browIdxBuf.copyOf(browCount),
            browMorphDeltaPosNorm = browDeltaBuf.copyOf(browCount * 6),
            lipSoftMorphIndices = lipSoftIdxBuf.copyOf(lipSoftCount),
            lipSoftMorphDeltaPosNorm = lipSoftDeltaBuf.copyOf(lipSoftCount * 6),
            upperLidLiftIndices = lidLiftIdxBuf.copyOf(lidLiftCount),
            upperLidLiftIsRight = lidLiftRightBuf.copyOf(lidLiftCount),
            upperLidLiftDeltaPosNorm = lidLiftDeltaBuf.copyOf(lidLiftCount * 6),
            swallowJawIndices = swallowJawIdxBuf.copyOf(swallowJawCount),
            swallowJawDeltaPosNorm = swallowJawDeltaBuf.copyOf(swallowJawCount * 6),
            nostrilMorphIndices = nostrilIdxBuf.copyOf(nostrilCount),
            nostrilMorphDeltaPosNorm = nostrilDeltaBuf.copyOf(nostrilCount * 6),
            throatNeckLocalIndices = throatLocalIdxBuf.copyOf(throatCount),
            throatNeckY = throatYBuf.copyOf(throatCount),
            throatNeckWeights = throatWBuf.copyOf(throatCount)
        )
    }

    /**
     * Smoothly rotates Blossom's head, eyes, layered hair, and upper neck side-to-side (`headTurn` in `-1f..+1f`),
     * nods gently downward (`headPitch` in `0f..1f` -> 0..12 deg chin-tuck nod), smoothly blinks upper eyelids
     * (`eyeBlink` in `0f..1f`), and optionally applies Lifelike Beta eye microsaccades (`eyeGazeX/Y`), subtle
     * pillow roll (`headRollOffset`), and delicate facial micro-expressions (`faceWarmth`, `faceBrow`, `faceLipSoft`).
     */
    fun applyHeadTurn(
        bodyMesh: DynamicGlMesh,
        headMesh: DynamicGlMesh,
        eyesMesh: DynamicGlMesh,
        hairMesh: DynamicGlMesh,
        cache: HeadRigCache,
        headTurn: Float,
        headPitch: Float = 0f,
        eyeBlink: Float = 0f,
        eyeGazeX: Float = 0f,
        eyeGazeY: Float = 0f,
        headRollOffset: Float = 0f,
        faceWarmth: Float = 0f,
        faceBrow: Float = 0f,
        faceLipSoft: Float = 0f,
        eyeBlinkRightOffset: Float = 0f,
        eyelidGazeTrack: Float = 0f,
        swallowPhase: Float = 0f,
        nostrilBreath: Float = 0f
    ) {
        val turn = headTurn.coerceIn(-1f, 1f)
        val pitch = headPitch.coerceIn(-1f, 1f)
        val blinkLBase = eyeBlink.coerceIn(0f, 1f)
        val blinkRBase = (blinkLBase + eyeBlinkRightOffset).coerceIn(0f, 1f)
        val gazeX = eyeGazeX.coerceIn(-1f, 1f)
        val gazeY = eyeGazeY.coerceIn(-1f, 1f)
        val lidTrackW = eyelidGazeTrack.coerceIn(0f, 1f)
        val downTrackClosure = if (lidTrackW > 1e-4f && gazeY < 0f) {
            (-gazeY * 0.28f * lidTrackW).coerceIn(0f, 0.30f)
        } else {
            0f
        }
        val upTrackLift = if (lidTrackW > 1e-4f && gazeY > 0f) {
            (gazeY * 0.90f * lidTrackW).coerceIn(0f, 1f)
        } else {
            0f
        }
        val effBlinkL = if (downTrackClosure > 1e-4f) {
            (blinkLBase + downTrackClosure * (1f - blinkLBase)).coerceIn(0f, 1f)
        } else {
            blinkLBase
        }
        val effBlinkR = if (downTrackClosure > 1e-4f) {
            (blinkRBase + downTrackClosure * (1f - blinkRBase)).coerceIn(0f, 1f)
        } else {
            blinkRBase
        }
        val rollExtra = headRollOffset.coerceIn(-0.12f, 0.12f)
        val warmth = faceWarmth.coerceIn(0f, 1f)
        val brow = faceBrow.coerceIn(-1f, 1f)
        val lipSoft = faceLipSoft.coerceIn(0f, 1f)
        val swallowP = swallowPhase.coerceIn(0f, 1f)
        val hasSwallow = swallowP > 0.001f && swallowP < 0.999f
        val swallowJawW = if (hasSwallow) {
            sin(smoothStep(0.04f, 0.62f, swallowP) * PI.toFloat()) * 0.85f
        } else {
            0f
        }
        val nostrilW = nostrilBreath.coerceIn(0f, 1f)
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val pivotY = HEAD_CENTER_Y - 0.18f
        val pivotZ = HEAD_CENTER_Z - 0.22f

        val hasFacialMorph = warmth > 1e-4f || abs(brow) > 1e-4f || lipSoft > 1e-4f ||
            swallowJawW > 1e-4f || nostrilW > 1e-4f
        val hasBlink = (effBlinkL > 1e-4f || effBlinkR > 1e-4f) && cache.blinkVertIndices.isNotEmpty()
        val hasLidLift = upTrackLift > 1e-4f && cache.upperLidLiftIndices.isNotEmpty()
        // Build rest-space head buffer with live eyelid blink, tracking & optional Lifelike micro-expressions before cervical rotation
        val headWork = if (hasBlink || hasLidLift || hasFacialMorph) {
            val buf = cache.headRestPosNorm.copyOf()
            if (hasFacialMorph) {
                fun applySparseMorph(indices: IntArray, deltas: FloatArray, weight: Float) {
                    if (abs(weight) <= 1e-4f || indices.isEmpty()) return
                    for (k in indices.indices) {
                        val h = indices[k] * 6
                        val d = k * 6
                        buf[h] += deltas[d] * weight
                        buf[h + 1] += deltas[d + 1] * weight
                        buf[h + 2] += deltas[d + 2] * weight
                        var nx = buf[h + 3] + deltas[d + 3] * weight
                        var ny = buf[h + 4] + deltas[d + 4] * weight
                        var nz = buf[h + 5] + deltas[d + 5] * weight
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        buf[h + 3] = nx / nLen
                        buf[h + 4] = ny / nLen
                        buf[h + 5] = nz / nLen
                    }
                }
                applySparseMorph(cache.smileMorphIndices, cache.smileMorphDeltaPosNorm, warmth)
                applySparseMorph(cache.browMorphIndices, cache.browMorphDeltaPosNorm, brow)
                applySparseMorph(cache.lipSoftMorphIndices, cache.lipSoftMorphDeltaPosNorm, lipSoft)
                applySparseMorph(cache.swallowJawIndices, cache.swallowJawDeltaPosNorm, swallowJawW)
                applySparseMorph(cache.nostrilMorphIndices, cache.nostrilMorphDeltaPosNorm, nostrilW)
            }
            if (hasLidLift) {
                val lIdx = cache.upperLidLiftIndices
                val lRight = cache.upperLidLiftIsRight
                val lDelta = cache.upperLidLiftDeltaPosNorm
                for (k in lIdx.indices) {
                    val sideBlink = if (lRight[k]) effBlinkR else effBlinkL
                    val w = upTrackLift * (1f - sideBlink)
                    if (w <= 1e-4f) continue
                    val h = lIdx[k] * 6
                    val d = k * 6
                    buf[h] += lDelta[d] * w
                    buf[h + 1] += lDelta[d + 1] * w
                    buf[h + 2] += lDelta[d + 2] * w
                    var nx = buf[h + 3] + lDelta[d + 3] * w
                    var ny = buf[h + 4] + lDelta[d + 4] * w
                    var nz = buf[h + 5] + lDelta[d + 5] * w
                    val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                    buf[h + 3] = nx / nLen
                    buf[h + 4] = ny / nLen
                    buf[h + 5] = nz / nLen
                }
            }
            if (hasBlink) {
                val bIdx = cache.blinkVertIndices
                val bRight = cache.blinkVertIsRight
                val bClosed = cache.blinkClosedPosNorm
                for (k in bIdx.indices) {
                    val vBlink = if (bRight[k]) effBlinkR else effBlinkL
                    if (vBlink <= 1e-4f) continue
                    val h = bIdx[k] * 6
                    val b = k * 6
                    buf[h] = buf[h] + (bClosed[b] - buf[h]) * vBlink
                    buf[h + 1] = buf[h + 1] + (bClosed[b + 1] - buf[h + 1]) * vBlink
                    buf[h + 2] = buf[h + 2] + (bClosed[b + 2] - buf[h + 2]) * vBlink
                    var nx = buf[h + 3] + (bClosed[b + 3] - buf[h + 3]) * vBlink
                    var ny = buf[h + 4] + (bClosed[b + 4] - buf[h + 4]) * vBlink
                    var nz = buf[h + 5] + (bClosed[b + 5] - buf[h + 5]) * vBlink
                    val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                    buf[h + 3] = nx / nLen
                    buf[h + 4] = ny / nLen
                    buf[h + 5] = nz / nLen
                }
            }
            buf
        } else {
            cache.headRestPosNorm
        }

        // Build optional rest-space anterior throat swallow wave buffer (zero copy & bit-identical when swallowPhase == 0f)
        val neckWork = if (hasSwallow && cache.throatNeckLocalIndices.isNotEmpty()) {
            val buf = cache.neckRestPosNorm.copyOf()
            val riseT = smoothStep(0f, 0.34f, swallowP)
            val fallT = smoothStep(0.32f, 0.86f, swallowP)
            val crestY = 5.30f + 0.22f * riseT - 0.42f * fallT
            val swallowEnv = smoothStep(0f, 0.14f, swallowP) * (1f - smoothStep(0.78f, 1.0f, swallowP))
            val dirVel = if (swallowP < 0.34f) 1.0f else -0.68f
            val tIdx = cache.throatNeckLocalIndices
            val tY = cache.throatNeckY
            val tW = cache.throatNeckWeights
            for (t in tIdx.indices) {
                val dyCrest = (tY[t] - crestY) / 0.105f
                val wave = exp(-(dyCrest * dyCrest)) * tW[t] * swallowEnv
                if (wave <= 1e-4f) continue
                val d = tIdx[t] * 6
                buf[d + 1] += dirVel * 0.0065f * wave
                buf[d + 2] += 0.0145f * wave
                val nx = buf[d + 3]
                val ny = buf[d + 4] - dyCrest * 0.08f * wave
                val nz = buf[d + 5]
                val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                buf[d + 3] = nx / nLen
                buf[d + 4] = ny / nLen
                buf[d + 5] = nz / nLen
            }
            buf
        } else {
            cache.neckRestPosNorm
        }

        // Build rest-space eyes buffer with optional Lifelike eye microsaccade/gaze rotation around each eyeball center
        val hasEyeGaze = abs(gazeX) > 1e-4f || abs(gazeY) > 1e-4f
        val eyesWork = if (hasEyeGaze && eyesMesh.vertexCount > 0) {
            val src = cache.eyesRestPosNorm
            val buf = src.copyOf()
            val halfVerts = eyesMesh.vertexCount / 2
            val gazeYaw = gazeX * 0.165f // up to +/- 9.5 deg subtle natural gaze shift
            val gazePitch = -gazeY * 0.115f // up to +/- 6.6 deg vertical gaze shift
            val cyG = cos(gazeYaw); val syG = sin(gazeYaw)
            val cpG = cos(gazePitch); val spG = sin(gazePitch)
            val eyeCenterY = 0.038f
            val eyeCenterZ = 0.528f
            for (i in 0 until eyesMesh.vertexCount) {
                val side = if (i < halfVerts) -1f else 1f
                val eyeCx = side * 0.256f
                val s = i * 6
                // Convert rest world position into head-local eyeball coordinates
                val lPt = headWorldToLocal(src[s], src[s + 1], src[s + 2])
                val rx = lPt[0] - eyeCx
                val ry = lPt[1] - eyeCenterY
                val rz = lPt[2] - eyeCenterZ
                // Pitch around local +X, then yaw around local +Y
                val y0 = ry * cpG - rz * spG
                val z0 = ry * spG + rz * cpG
                val x1 = rx * cyG + z0 * syG
                val z1 = -rx * syG + z0 * cyG
                val wPt = headLocalToWorld(eyeCx + x1, eyeCenterY + y0, eyeCenterZ + z1)

                // Rotate rest world normal in head-local frame
                val wnx = src[s + 3]; val wny = src[s + 4]; val wnz = src[s + 5]
                val lny = wny * COS_PITCH + wnz * SIN_PITCH
                val lnz = -wny * SIN_PITCH + wnz * COS_PITCH
                val ny0 = lny * cpG - lnz * spG
                val nz0 = lny * spG + lnz * cpG
                val nx1 = wnx * cyG + nz0 * syG
                val nz1 = -wnx * syG + nz0 * cyG
                val wN = headNormalToWorld(nx1, ny0, nz1)

                buf[s] = wPt[0]; buf[s + 1] = wPt[1]; buf[s + 2] = wPt[2]
                buf[s + 3] = wN[0]; buf[s + 4] = wN[1]; buf[s + 5] = wN[2]
            }
            buf
        } else {
            cache.eyesRestPosNorm
        }

        fun restoreMesh(mesh: DynamicGlMesh, rest: FloatArray) {
            val dst = mesh.vertexData
            for (i in 0 until mesh.vertexCount) {
                val s = i * 6
                val d = i * stride
                dst[d] = rest[s]; dst[d + 1] = rest[s + 1]; dst[d + 2] = rest[s + 2]
                dst[d + 3] = rest[s + 3]; dst[d + 4] = rest[s + 4]; dst[d + 5] = rest[s + 5]
            }
            mesh.commitVertices()
        }

        if (abs(turn) < 1e-4f && abs(pitch) < 1e-4f && abs(rollExtra) < 1e-4f) {
            restoreMesh(headMesh, headWork)
            restoreMesh(eyesMesh, eyesWork)
            restoreMesh(hairMesh, cache.hairRestPosNorm)
            val bData = bodyMesh.vertexData
            for (k in cache.neckBodyIndices.indices) {
                val s = k * 6
                val d = cache.neckBodyIndices[k] * stride
                bData[d] = neckWork[s]
                bData[d + 1] = neckWork[s + 1]
                bData[d + 2] = neckWork[s + 2]
                bData[d + 3] = neckWork[s + 3]
                bData[d + 4] = neckWork[s + 4]
                bData[d + 5] = neckWork[s + 5]
            }
            bodyMesh.commitVertices()
            return
        }

        // +pitch (0..1): 0..12 deg downward chin-tuck nod; -pitch (-1..0): 0..-19.5 deg backward head tilt
        val fullPitch = if (pitch >= 0f) pitch * 0.21f else pitch * 0.34f
        val fullYaw = turn * 0.488f // +/- 28 deg natural comfortable cervical rotation
        val fullRoll = -turn * 0.068f + rollExtra // +/- 3.9 deg gentle headrest pillow roll + optional subtle tilt

        fun rotatePointAndNormal(
            px: Float, py: Float, pz: Float,
            nx: Float, ny: Float, nz: Float,
            pitchRad: Float, yaw: Float, roll: Float,
            out: FloatArray
        ) {
            val cp = cos(pitchRad); val sp = sin(pitchRad)
            val cy = cos(yaw); val sy = sin(yaw)
            val cr = cos(roll); val sr = sin(roll)
            val rx = px
            val ry = py - pivotY
            val rz = pz - pivotZ

            // 1. Subtle downward nod around +X (+pitch moves anterior +Z face downward -Y)
            val x0 = rx
            val y0 = ry * cp - rz * sp
            val z0 = ry * sp + rz * cp

            // 2. Yaw around +Y (+yaw turns face toward +X), then subtle roll around +Z
            val x1 = x0 * cy + z0 * sy
            val y1 = y0
            val z1 = -x0 * sy + z0 * cy

            val x2 = x1 * cr - y1 * sr
            val y2 = x1 * sr + y1 * cr
            val z2 = z1

            val nx0 = nx
            val ny0 = ny * cp - nz * sp
            val nz0 = ny * sp + nz * cp

            val nx1 = nx0 * cy + nz0 * sy
            val ny1 = ny0
            val nz1 = -nx0 * sy + nz0 * cy

            var nx2 = nx1 * cr - ny1 * sr
            var ny2 = nx1 * sr + ny1 * cr
            var nz2 = nz1
            val nLen = sqrt(nx2 * nx2 + ny2 * ny2 + nz2 * nz2).coerceAtLeast(1e-5f)
            nx2 /= nLen; ny2 /= nLen; nz2 /= nLen

            out[0] = x2
            out[1] = pivotY + y2
            out[2] = pivotZ + z2
            out[3] = nx2
            out[4] = ny2
            out[5] = nz2
        }

        val tmp = FloatArray(6)

        fun applyRigid(mesh: DynamicGlMesh, rest: FloatArray) {
            val dst = mesh.vertexData
            for (i in 0 until mesh.vertexCount) {
                val s = i * 6
                val d = i * stride
                rotatePointAndNormal(
                    rest[s], rest[s + 1], rest[s + 2],
                    rest[s + 3], rest[s + 4], rest[s + 5],
                    fullPitch, fullYaw, fullRoll, tmp
                )
                dst[d] = tmp[0]; dst[d + 1] = tmp[1]; dst[d + 2] = tmp[2]
                dst[d + 3] = tmp[3]; dst[d + 4] = tmp[4]; dst[d + 5] = tmp[5]
            }
            mesh.commitVertices()
        }

        applyRigid(headMesh, headWork)
        applyRigid(eyesMesh, eyesWork)

        // Hair follows full head turn & nod on crown/face-framing locks and softens gently on lower pillow-resting tips
        val hDst = hairMesh.vertexData
        val hRest = cache.hairRestPosNorm
        for (i in 0 until hairMesh.vertexCount) {
            val s = i * 6
            val d = i * stride
            val restY = hRest[s + 1]
            val hairW = 0.52f + 0.48f * smoothStep(5.05f, 5.85f, restY)
            rotatePointAndNormal(
                hRest[s], restY, hRest[s + 2],
                hRest[s + 3], hRest[s + 4], hRest[s + 5],
                fullPitch * hairW, fullYaw * hairW, fullRoll * hairW, tmp
            )
            val minHairZ = BlossomBodyBuilder.mattressSurfaceZ(tmp[1]) + 0.03f
            hDst[d] = tmp[0]
            hDst[d + 1] = tmp[1]
            hDst[d + 2] = if (tmp[2] < minHairZ) minHairZ else tmp[2]
            hDst[d + 3] = tmp[3]
            hDst[d + 4] = tmp[4]
            hDst[d + 5] = tmp[5]
        }
        hairMesh.commitVertices()

        // Smooth cervical twist & nod across upper neck vertices in blossomBodyMesh
        val bData = bodyMesh.vertexData
        val nRest = neckWork
        for (k in cache.neckBodyIndices.indices) {
            val s = k * 6
            val d = cache.neckBodyIndices[k] * stride
            val w = cache.neckWeights[k]
            rotatePointAndNormal(
                nRest[s], nRest[s + 1], nRest[s + 2],
                nRest[s + 3], nRest[s + 4], nRest[s + 5],
                fullPitch * w * 0.55f, fullYaw * w * 0.72f, fullRoll * w * 0.65f, tmp
            )
            bData[d] = tmp[0]; bData[d + 1] = tmp[1]; bData[d + 2] = tmp[2]
            bData[d + 3] = tmp[3]; bData[d + 4] = tmp[4]; bData[d + 5] = tmp[5]
        }
        bodyMesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // 7. TEMPORARY INDEPENDENT BETA REFINEMENTS (NECK SEAM & SOFTENED HAIR)
    //    Stores exact immutable baseline backups so each toggle can be cleanly undone!
    // -------------------------------------------------------------------------
    class HeadNeckHairBetaCache(
        val baseHeadVertexData: FloatArray,
        val neckBodyIndices: IntArray,
        val baseNeckBodyData: FloatArray,
        val baseHairVertexData: FloatArray
    ) {
        var betaHeadVertexData: FloatArray? = null
        var betaNeckBodyData: FloatArray? = null
        var betaHairVertexData: FloatArray? = null
    }

    fun buildHeadNeckHairBetaCache(
        bodyMesh: DynamicGlMesh,
        headMesh: DynamicGlMesh,
        hairMesh: DynamicGlMesh
    ): HeadNeckHairBetaCache {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val idxBuf = IntArray(bodyMesh.vertexCount)
        var count = 0
        for (i in 0 until bodyMesh.vertexCount) {
            val base = i * stride
            val ax = abs(bData[base])
            val y = bData[base + 1]
            // Superset of HeadRigCache (y in 4.92..6.05) extended down to lower neck base (y >= 4.66, ax < 0.62)
            // so Beta 5 can feather neck AO & normals ~2x farther below the jaw without touching breasts, chest, face, eyes or hair!
            if (y in 4.66f..6.05f && ax < 0.62f) {
                idxBuf[count++] = i
            }
        }
        val neckIndices = idxBuf.copyOf(count)
        val baseNeckData = FloatArray(count * stride)
        for (k in 0 until count) {
            System.arraycopy(bData, neckIndices[k] * stride, baseNeckData, k * stride, stride)
        }
        return HeadNeckHairBetaCache(
            baseHeadVertexData = headMesh.vertexData.copyOf(),
            neckBodyIndices = neckIndices,
            baseNeckBodyData = baseNeckData,
            baseHairVertexData = hairMesh.vertexData.copyOf()
        )
    }

    /**
     * Beta 5 (Smooth Neck Seam): widens the jaw-to-neck feather ~2x across the submental underside
     * (`theta >= 0.765*PI`, `wy <= 5.86f`) and down the full neck (`y in 4.68..6.05`, `ax < 0.60`),
     * smoothly matching AO, wetness, pigment & normals so the pale band under the chin disappears.
     * Eyes, face above jaw rim (`y > 5.88`), hair, and body/breasts below neck stay 100% untouched.
     */
    fun applyNeckSeamBeta(
        bodyMesh: DynamicGlMesh,
        headMesh: DynamicGlMesh,
        headRigCache: HeadRigCache,
        betaCache: HeadNeckHairBetaCache,
        enabled: Boolean
    ) {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        if (enabled && (betaCache.betaHeadVertexData == null || betaCache.betaNeckBodyData == null)) {
            val headCopy = betaCache.baseHeadVertexData.copyOf()
            val cols = HEAD_COLS
            val rows = HEAD_ROWS
            if (headMesh.vertexCount == cols * rows) {
                // Step 1: Feather submental AO, wetness, pigment & normals smoothly from jaw rim right into SDF neck
                for (r in 0 until rows) {
                    val vRaw = 1f - (r.toFloat() / (rows - 1))
                    val vCentered = vRaw * 2f - 1f
                    val vCluster = 0.66f * vCentered + 0.34f * vCentered * vCentered * vCentered
                    val theta = ((vCluster + 1f) * 0.5f).coerceIn(0.004f, 0.996f) * PI.toFloat()
                    if (theta < 0.765f * PI.toFloat()) continue

                    val thetaMat = smoothStep(0.765f * PI.toFloat(), 0.915f * PI.toFloat(), theta)
                    val thetaNorm = smoothStep(0.770f * PI.toFloat(), 0.925f * PI.toFloat(), theta)
                    val filletBell = sin(smoothStep(0.78f * PI.toFloat(), 0.96f * PI.toFloat(), theta) * PI.toFloat())

                    for (c in 0 until cols) {
                        val idx = (r * cols + c) * stride
                        val wx = headCopy[idx]
                        var wy = headCopy[idx + 1]
                        val wz = headCopy[idx + 2]
                        if (wy > 5.86f) continue

                        val baseNy = headCopy[idx + 4]
                        val downMask = smoothStep(0.18f, -0.28f, baseNy)
                        val yGuard = 1f - smoothStep(5.74f, 5.86f, wy)
                        val matBlend = (thetaMat * (0.45f + 0.55f * downMask) * yGuard).coerceIn(0f, 1f)
                        val normBlend = (thetaNorm * (0.40f + 0.60f * downMask) * yGuard).coerceIn(0f, 1f)

                        // Gentle downward fillet ease strictly under the jaw (zero change to face above jaw!)
                        if (filletBell > 0.001f && yGuard > 0.001f) {
                            wy -= 0.036f * filletBell * yGuard * smoothStep(-0.15f, 0.55f, headCopy[idx + 5])
                            headCopy[idx + 1] = wy
                        }

                        var nx = headCopy[idx + 3]
                        var ny = headCopy[idx + 4]
                        var nz = headCopy[idx + 5]
                        if (normBlend > 0.001f) {
                            val sdfNorm = BlossomSdfBodyBuilder.evaluateBodyNormal(wx, wy, wz, eps = 0.026f)
                            nx = nx * (1f - normBlend) + sdfNorm[0] * normBlend
                            ny = ny * (1f - normBlend) + sdfNorm[1] * normBlend
                            nz = nz * (1f - normBlend) + sdfNorm[2] * normBlend
                            val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                            nx /= nLen; ny /= nLen; nz /= nLen
                            headCopy[idx + 3] = nx
                            headCopy[idx + 4] = ny
                            headCopy[idx + 5] = nz
                        }

                        if (matBlend > 0.001f) {
                            val (sdfAo, sdfPigment) = BlossomSdfBodyBuilder.evaluateBodyMaterial(wx, wy, wz, nx, ny, nz)
                            // Match the softened upper-neck AO curve seamlessly (eliminates bright pale band under chin!)
                            val softNeckAo = (sdfAo + (0.875f - sdfAo).coerceAtLeast(0f) * 0.48f).coerceIn(0.79f, 0.91f)
                            headCopy[idx + 7] = headCopy[idx + 7] * (1f - matBlend) + 0.055f * matBlend
                            headCopy[idx + 8] = headCopy[idx + 8] * (1f - matBlend) + softNeckAo * matBlend
                            headCopy[idx + 9] = headCopy[idx + 9] * (1f - matBlend) + sdfPigment * matBlend
                        }
                    }
                }
            }
            betaCache.betaHeadVertexData = headCopy

            // Step 2: Feather under-chin AO & neck normals ~2x farther down the neck (y = 4.68..5.92)
            val neckCopy = betaCache.baseNeckBodyData.copyOf()
            for (k in betaCache.neckBodyIndices.indices) {
                val d = k * stride
                val ax = abs(neckCopy[d])
                val y = neckCopy[d + 1]
                val neckFade = smoothStep(4.70f, 5.38f, y) * (1f - smoothStep(0.38f, 0.60f, ax))
                if (neckFade > 0.001f) {
                    val baseAo = neckCopy[d + 8]
                    val liftedAo = baseAo + (0.90f - baseAo).coerceAtLeast(0f) * 0.56f * neckFade
                    neckCopy[d + 8] = liftedAo.coerceIn(0.14f, 1f)

                    // Smooth, wide anterior neck normal feather flowing seamlessly out from under the chin
                    val nz = neckCopy[d + 5]
                    val wideNormalEase = smoothStep(4.92f, 5.62f, y) * (1f - smoothStep(5.76f, 6.02f, y)) *
                        smoothStep(0.10f, 0.68f, nz) * (1f - smoothStep(0.38f, 0.60f, ax))
                    if (wideNormalEase > 0.001f) {
                        val nx = neckCopy[d + 3]
                        val ny = neckCopy[d + 4] - 0.085f * wideNormalEase
                        val nzz = nz
                        val nLen = sqrt(nx * nx + ny * ny + nzz * nzz).coerceAtLeast(1e-5f)
                        neckCopy[d + 3] = nx / nLen
                        neckCopy[d + 4] = ny / nLen
                        neckCopy[d + 5] = nzz / nLen
                    }
                }
            }
            betaCache.betaNeckBodyData = neckCopy
        }

        val srcHead = if (enabled) betaCache.betaHeadVertexData!! else betaCache.baseHeadVertexData
        System.arraycopy(srcHead, 0, headMesh.vertexData, 0, srcHead.size)
        headMesh.commitVertices()

        val srcNeck = if (enabled) betaCache.betaNeckBodyData!! else betaCache.baseNeckBodyData
        val bData = bodyMesh.vertexData
        for (k in betaCache.neckBodyIndices.indices) {
            System.arraycopy(srcNeck, k * stride, bData, betaCache.neckBodyIndices[k] * stride, stride)
        }
        bodyMesh.commitVertices()

        // Keep HeadRigCache rest pose in sync so head turn, nod & blink compose seamlessly
        for (i in 0 until headMesh.vertexCount) {
            val s = i * stride
            val d = i * 6
            headRigCache.headRestPosNorm[d] = srcHead[s]
            headRigCache.headRestPosNorm[d + 1] = srcHead[s + 1]
            headRigCache.headRestPosNorm[d + 2] = srcHead[s + 2]
            headRigCache.headRestPosNorm[d + 3] = srcHead[s + 3]
            headRigCache.headRestPosNorm[d + 4] = srcHead[s + 4]
            headRigCache.headRestPosNorm[d + 5] = srcHead[s + 5]
        }
        for (k in headRigCache.neckBodyIndices.indices) {
            val s = headRigCache.neckBodyIndices[k] * stride
            val d = k * 6
            headRigCache.neckRestPosNorm[d] = bData[s]
            headRigCache.neckRestPosNorm[d + 1] = bData[s + 1]
            headRigCache.neckRestPosNorm[d + 2] = bData[s + 2]
            headRigCache.neckRestPosNorm[d + 3] = bData[s + 3]
            headRigCache.neckRestPosNorm[d + 4] = bData[s + 4]
            headRigCache.neckRestPosNorm[d + 5] = bData[s + 5]
        }
    }

    /**
     * Beta 6 (Softened Hair): swaps Blossom's hair vertex buffer cleanly between the exact
     * immutable baseline hair mesh and the lazily-cached softened fringe/temple/nape variant.
     */
    fun applyHairBeta(
        hairMesh: DynamicGlMesh,
        headRigCache: HeadRigCache,
        betaCache: HeadNeckHairBetaCache,
        enabled: Boolean
    ) {
        if (enabled && betaCache.betaHairVertexData == null) {
            betaCache.betaHairVertexData = buildBlossomHairMesh(betaSoftHair = true).vertexData.copyOf()
        }
        val src = if (enabled) betaCache.betaHairVertexData!! else betaCache.baseHairVertexData
        System.arraycopy(src, 0, hairMesh.vertexData, 0, src.size)
        hairMesh.commitVertices()

        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        for (i in 0 until hairMesh.vertexCount) {
            val s = i * stride
            val d = i * 6
            headRigCache.hairRestPosNorm[d] = src[s]
            headRigCache.hairRestPosNorm[d + 1] = src[s + 1]
            headRigCache.hairRestPosNorm[d + 2] = src[s + 2]
            headRigCache.hairRestPosNorm[d + 3] = src[s + 3]
            headRigCache.hairRestPosNorm[d + 4] = src[s + 4]
            headRigCache.hairRestPosNorm[d + 5] = src[s + 5]
        }
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun smoothBell(x: Float, low: Float, high: Float, feather: Float): Float {
        return smoothStep(low - feather, low + feather, x) * (1f - smoothStep(high - feather, high + feather, x))
    }
}
