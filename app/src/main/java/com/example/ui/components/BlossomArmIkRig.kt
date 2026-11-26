package com.example.ui.components

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
 * Real-time 60 FPS 2-Bone Analytical Inverse Kinematics (IK) & Articulated Hand/Finger Skinning Rig
 * for Blossom v5.0 ("Ask Blossom to Spread" self-assistance pose).
 *
 * Guarantees:
 *  - Exact bone-length preservation (|E - S| == L1, |W - E| == L2) across the entire 0.0 -> 1.0 arc
 *  - Progressive anatomical forearm pronation twist from elbow to wrist (zero candy-wrapper pinch)
 *  - Disjoint vertex membership from [BlossomSdfBodyBuilder.LegSkinCache] (legs & arms animate independently)
 *  - Multi-joint MCP / PIP / DIP phalanx flexion on all 10 sculpted fingers + 10 locked 3D fingernails
 *  - Strictly right-handed orthonormal joint frames (det = +1) preserving outward CCW winding on both sides
 */
internal object BlossomArmIkRig {

    /**
     * Version 19.0 calibrated self-spread targets: aligned with the default 40% vulva elastic
     * resistance spread cap (`vulvaElasticMaxSpread(0.40f) == 0.60f`) so Blossom's spreading
     * animation matches manual tissue elasticity and never overspreads past the 40% limit.
     */
    const val SELF_SPREAD_OUTER_TARGET = 0.56f
    const val SELF_SPREAD_INNER_TARGET = 0.60f

    // Exact rest arm joint anchors matching BlossomSdfBodyBuilder.evaluateBodySdf()
    const val SHOULDER_AX = 1.34f
    const val SHOULDER_Y = 4.40f
    val SHOULDER_Z: Float = BlossomBodyBuilder.mattressSurfaceZ(4.54f) + 0.52f

    const val ELBOW_AX = 2.26f
    const val ELBOW_Y = 2.28f
    val ELBOW_Z: Float = BlossomBodyBuilder.mattressSurfaceZ(2.28f) + 0.36f

    const val WRIST_AX = 2.34f
    const val WRIST_Y = 0.42f
    val WRIST_Z: Float = BlossomBodyBuilder.mattressSurfaceZ(0.42f) + 0.195f

    const val PALM_TIP_AX = 2.41f
    const val PALM_TIP_Y = -0.45f
    val PALM_TIP_Z: Float = BlossomBodyBuilder.mattressSurfaceZ(0.00f) + 0.185f

    val UPPER_ARM_LEN: Float = dist3(
        SHOULDER_AX, SHOULDER_Y, SHOULDER_Z,
        ELBOW_AX, ELBOW_Y, ELBOW_Z
    )
    val FOREARM_LEN: Float = dist3(
        ELBOW_AX, ELBOW_Y, ELBOW_Z,
        WRIST_AX, WRIST_Y, WRIST_Z
    )

    internal data class Vec3(val x: Float, val y: Float, val z: Float) {
        operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
        operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
        operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
        fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
        fun cross(o: Vec3): Vec3 = Vec3(
            y * o.z - z * o.y,
            z * o.x - x * o.z,
            x * o.y - y * o.x
        )
        fun len(): Float = sqrt(x * x + y * y + z * z)
        fun normalized(): Vec3 {
            val l = len()
            return if (l > 1e-6f) Vec3(x / l, y / l, z / l) else Vec3(0f, 1f, 0f)
        }
    }

    internal data class OrthoFrame(
        val origin: Vec3,
        val axisX: Vec3,
        val axisY: Vec3,
        val axisZ: Vec3
    ) {
        /** Expresses a world point in local frame coordinates. */
        fun worldToLocalPoint(px: Float, py: Float, pz: Float): Vec3 {
            val dx = px - origin.x
            val dy = py - origin.y
            val dz = pz - origin.z
            return Vec3(
                dx * axisX.x + dy * axisX.y + dz * axisX.z,
                dx * axisY.x + dy * axisY.y + dz * axisY.z,
                dx * axisZ.x + dy * axisZ.y + dz * axisZ.z
            )
        }

        /** Maps a local point back to world coordinates. */
        fun localToWorldPoint(lx: Float, ly: Float, lz: Float): Vec3 = Vec3(
            origin.x + axisX.x * lx + axisY.x * ly + axisZ.x * lz,
            origin.y + axisX.y * lx + axisY.y * ly + axisZ.y * lz,
            origin.z + axisX.z * lx + axisY.z * ly + axisZ.z * lz
        )

        /** Expresses a world normal vector in local frame coordinates. */
        fun worldToLocalDir(nx: Float, ny: Float, nz: Float): Vec3 = Vec3(
            nx * axisX.x + ny * axisX.y + nz * axisX.z,
            nx * axisY.x + ny * axisY.y + nz * axisY.z,
            nx * axisZ.x + ny * axisZ.y + nz * axisZ.z
        )

        /** Maps a local direction vector back to world coordinates. */
        fun localToWorldDir(lx: Float, ly: Float, lz: Float): Vec3 = Vec3(
            axisX.x * lx + axisY.x * ly + axisZ.x * lz,
            axisX.y * lx + axisY.y * ly + axisZ.y * lz,
            axisX.z * lx + axisY.z * ly + axisZ.z * lz
        )

        /** Rotates axisX & axisZ around axisY by angleRad (anatomical forearm pronation/supination). */
        fun rotatedAroundY(angleRad: Float): OrthoFrame {
            if (abs(angleRad) < 1e-5f) return this
            val c = cos(angleRad)
            val s = sin(angleRad)
            val rx = axisX * c + axisZ * s
            val rz = axisZ * c - axisX * s
            return OrthoFrame(origin, rx, axisY, rz)
        }
    }

    internal data class SideArmIkSolution(
        val side: Float,
        val shoulder: Vec3,
        val elbow: Vec3,
        val wrist: Vec3,
        val restUpper: OrthoFrame,
        val poseUpper: OrthoFrame,
        val restForearm: OrthoFrame,
        val poseForearm: OrthoFrame,
        val restHand: OrthoFrame,
        val poseHand: OrthoFrame,
        val wristTwistRad: Float,
        val fingerCurlWeight: Float,
        val poseProgress: Float,
        val fingerMicroCurl: Float = 0f,
        val isActivePose: Boolean = false
    )

    internal data class FingerFlexPose(
        val mcpFlexRad: Float,
        val pipFlexRad: Float,
        val dipFlexRad: Float,
        val splayRad: Float,
        val rollRad: Float = 0f
    )

    class ArmSkinCache(
        val bodyVertIndices: IntArray,
        val bodyBasePosNorm: FloatArray,
        val bodySides: FloatArray,
        val wTorso: FloatArray,
        val wUpper: FloatArray,
        val wForearm: FloatArray,
        val wHand: FloatArray,
        val hForearm: FloatArray
    )

    /**
     * Precomputes smooth shoulder/elbow/wrist skinning weights over Blossom's unified SDF body mesh.
     * Strictly excludes torso interior, breasts, vulva, buttocks, and ALL leg vertices so [ArmSkinCache]
     * and [BlossomSdfBodyBuilder.LegSkinCache] are 100% disjoint!
     */
    fun buildArmSkinCache(bodyMesh: DynamicGlMesh): ArmSkinCache {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val nVerts = bodyMesh.vertexCount

        val idxBuf = IntArray(nVerts)
        val sideBuf = FloatArray(nVerts)
        val wTorsoBuf = FloatArray(nVerts)
        val wUpperBuf = FloatArray(nVerts)
        val wForearmBuf = FloatArray(nVerts)
        val wHandBuf = FloatArray(nVerts)
        val hForBuf = FloatArray(nVerts)
        var count = 0

        for (i in 0 until nVerts) {
            val base = i * stride
            val x = bData[base]
            val y = bData[base + 1]
            val z = bData[base + 2]

            val ax = abs(x)
            // Quick bounding box rejection (well inside the smooth SDF zero-weight zone)
            if (ax < 0.96f || y < -0.85f || y > 5.04f) continue
            // Guarantee 100% disjoint membership from LegSkinCache (legs sit in y <= 0.44 and ax <= 1.86 or z >= -0.35)
            if (y <= 0.44f && (ax <= 1.86f || z >= -0.35f)) continue

            // Continuous SDF ownership weight (ZERO hard spatial cutoffs -> ZERO arm-to-table skin spikes!)
            val armEnable = BlossomSdfBodyBuilder.evaluateArmVsTorsoWeight(x, y, z)
            if (armEnable <= 0.001f) continue

            // Normalized parameter along forearm (E -> W) for progressive anatomical pronation twist
            val (hFa, _) = closestSegmentParamAndDist(
                ax, y, z,
                ELBOW_AX, ELBOW_Y, ELBOW_Z,
                WRIST_AX, WRIST_Y, WRIST_Z
            )

            // Smooth multi-bone weights along the Shoulder -> Elbow -> Wrist -> Palm chain
            // Elbow transition centered around ELBOW_Y (blended over +-0.34, strictly disjoint from shoulder)
            val toForearm = smoothStep(ELBOW_Y + 0.34f, ELBOW_Y - 0.34f, y)
            // Wrist transition centered around WRIST_Y (blended over +-0.28 for silky volume-preserving bend)
            val toHand = smoothStep(WRIST_Y + 0.28f, WRIST_Y - 0.28f, y)

            val rawHand = toHand
            val rawForearm = toForearm * (1f - toHand)
            val rawUpper = 1f - toForearm

            val wT = 1f - armEnable
            val wU = rawUpper * armEnable
            val wF = rawForearm * armEnable
            val wH = rawHand * armEnable
            val sumW = (wT + wU + wF + wH).coerceAtLeast(1e-5f)

            idxBuf[count] = i
            sideBuf[count] = if (x >= 0f) 1f else -1f
            wTorsoBuf[count] = wT / sumW
            wUpperBuf[count] = wU / sumW
            wForearmBuf[count] = wF / sumW
            wHandBuf[count] = wH / sumW
            hForBuf[count] = if (toHand > 0.99f) 1f else hFa
            count++
        }

        val indices = idxBuf.copyOf(count)
        val basePosNorm = FloatArray(count * 6)
        for (k in 0 until count) {
            val src = indices[k] * stride
            val dst = k * 6
            basePosNorm[dst] = bData[src]
            basePosNorm[dst + 1] = bData[src + 1]
            basePosNorm[dst + 2] = bData[src + 2]
            basePosNorm[dst + 3] = bData[src + 3]
            basePosNorm[dst + 4] = bData[src + 4]
            basePosNorm[dst + 5] = bData[src + 5]
        }

        return ArmSkinCache(
            bodyVertIndices = indices,
            bodyBasePosNorm = basePosNorm,
            bodySides = sideBuf.copyOf(count),
            wTorso = wTorsoBuf.copyOf(count),
            wUpper = wUpperBuf.copyOf(count),
            wForearm = wForearmBuf.copyOf(count),
            wHand = wHandBuf.copyOf(count),
            hForearm = hForBuf.copyOf(count)
        )
    }

    /**
     * Solves exact 2-bone analytical IK + natural 3-phase wrist choreography for one arm (`side = -1` left, `+1` right).
     *
     * Phases:
     *  - `0.00 -> 0.45`: Lift wrists softly off the exam mattress and arc cleanly over anterior hips (+Z)
     *  - `0.45 -> 0.68`: Glide wrists inward above upper groin while orienting palms & articulating fingers onto outer labia
     *  - `0.68 -> 1.00`: Draw fingertips smoothly outward in lockstep with outer & inner labial spreading
     */
    fun solveSideArmIk(
        side: Float,
        poseProgress: Float,
        liveOuterSpread: Float,
        armMicroShift: Float = 0f,
        fingerMicroCurl: Float = 0f
    ): SideArmIkSolution {
        val p = poseProgress.coerceIn(0f, 1f)
        val microArm = armMicroShift.coerceIn(-1f, 1f)
        val microFinger = fingerMicroCurl.coerceIn(0f, 1f)

        val s0 = Vec3(side * SHOULDER_AX, SHOULDER_Y, SHOULDER_Z)
        val e0 = Vec3(side * ELBOW_AX, ELBOW_Y, ELBOW_Z)
        val w0 = Vec3(side * WRIST_AX, WRIST_Y, WRIST_Z)
        val palmTip0 = Vec3(side * PALM_TIP_AX, PALM_TIP_Y, PALM_TIP_Z)

        val restUpper = buildBoneFrame(s0, e0, Vec3(0f, 0f, 1f))
        val restForearm = buildBoneFrame(e0, w0, Vec3(0f, 0f, 1f))
        val restHand = buildBoneFrame(w0, palmTip0, Vec3(0f, 0f, 1f))

        if (p <= 0.0001f && abs(microArm) <= 1e-4f && microFinger <= 1e-4f) {
            return SideArmIkSolution(
                side = side,
                shoulder = s0,
                elbow = e0,
                wrist = w0,
                restUpper = restUpper,
                poseUpper = restUpper,
                restForearm = restForearm,
                poseForearm = restForearm,
                restHand = restHand,
                poseHand = restHand,
                wristTwistRad = 0f,
                fingerCurlWeight = 0f,
                poseProgress = 0f,
                fingerMicroCurl = 0f,
                isActivePose = false
            )
        }

        // Rest-side subtle arm & finger micro-movement (strictly 2-bone length-preserving & above mattress!)
        if (p <= 0.0001f) {
            val shoulder = Vec3(
                s0.x + side * 0.014f * microArm,
                s0.y + 0.032f * abs(microArm),
                s0.z + 0.026f * abs(microArm)
            )
            var wrist = Vec3(
                w0.x - side * (0.075f * max(0f, microArm) - 0.055f * max(0f, -microArm)),
                w0.y + 0.145f * microArm,
                w0.z + 0.058f * abs(microArm) + 0.024f * microFinger
            )
            val maxReach = (UPPER_ARM_LEN + FOREARM_LEN) * 0.992f
            val minReach = abs(UPPER_ARM_LEN - FOREARM_LEN) + 0.32f
            val swVec = wrist - shoulder
            val swDistRaw = swVec.len().coerceAtLeast(1e-5f)
            val swDist = swDistRaw.coerceIn(minReach, maxReach)
            if (abs(swDist - swDistRaw) > 1e-5f) {
                wrist = shoulder + swVec * (swDist / swDistRaw)
            }
            val swDir = (wrist - shoulder) * (1f / swDist)
            val restDir = (w0 - s0).normalized()
            val restElbowOffset = (e0 - s0) - restDir * (e0 - s0).dot(restDir)
            val poleRest = restElbowOffset.normalized()
            var polePerp = poleRest - swDir * poleRest.dot(swDir)
            if (polePerp.len() < 1e-4f) {
                polePerp = Vec3(side * 0.85f, 0f, -0.52f) - swDir * (Vec3(side * 0.85f, 0f, -0.52f).dot(swDir))
            }
            polePerp = polePerp.normalized()
            val a = (UPPER_ARM_LEN * UPPER_ARM_LEN - FOREARM_LEN * FOREARM_LEN + swDist * swDist) / (2f * swDist)
            val h = sqrt((UPPER_ARM_LEN * UPPER_ARM_LEN - a * a).coerceAtLeast(0f))
            val elbow = shoulder + swDir * a + polePerp * h

            val poseUpper = buildBoneFrame(shoulder, elbow, Vec3(0f, 0f, 1f))
            val poseForearm = buildBoneFrame(elbow, wrist, Vec3(0f, 0f, 1f))
            val subtleHandDir = Vec3(
                restHand.axisY.x - side * 0.06f * microArm,
                restHand.axisY.y,
                restHand.axisY.z + 0.05f * abs(microArm) + 0.04f * microFinger
            ).normalized()
            val subtleDorsal = Vec3(
                -side * (0.14f * microArm + 0.08f * microFinger),
                0.04f * microArm,
                1.0f
            ).normalized()
            val poseHand = buildFrameFromYAndZHint(wrist, subtleHandDir, subtleDorsal)
            val wristTwist = (0.16f * microArm + 0.08f * microFinger).coerceIn(-0.35f, 0.35f)
            return SideArmIkSolution(
                side = side,
                shoulder = shoulder,
                elbow = elbow,
                wrist = wrist,
                restUpper = restUpper,
                poseUpper = poseUpper,
                restForearm = restForearm,
                poseForearm = poseForearm,
                restHand = restHand,
                poseHand = poseHand,
                wristTwistRad = wristTwist,
                fingerCurlWeight = 0f,
                poseProgress = 0f,
                fingerMicroCurl = microFinger,
                isActivePose = true
            )
        }

        // 1. Subtle natural scapular protraction + gentle lateral clearance past fuller breasts
        val reachT = smoothStep(0.05f, 0.65f, p)
        val microFade = (1f - p * 0.82f).coerceIn(0.18f, 1f)
        val shoulder = Vec3(
            s0.x + side * (0.078f * reachT + 0.012f * microArm * microFade),
            s0.y - 0.17f * reachT + 0.024f * abs(microArm) * microFade,
            s0.z + 0.24f * reachT + 0.020f * abs(microArm) * microFade
        )

        // 2. Cubic Bezier wrist trajectory calibrated for lengthened anatomical feminine arms & hands
        val uArc = smoothStep(0.0f, 0.68f, p)
        val b0 = w0
        val b1 = Vec3(side * 2.42f, 0.82f, 0.18f)
        val b2 = Vec3(side * 1.66f, 1.48f, 1.10f)
        val b3 = Vec3(side * 0.80f, 1.24f, 1.07f)
        val wristArc = cubicBezier(b0, b1, b2, b3, uArc)

        // 3. Synchronized lateral spread glide (Phase 3: 0.68 -> 1.00) calibrated to v19.0 40% elastic spread
        val spreadT = smoothStep(0.68f, 1.00f, p)
        val manualAdjust = if (p > 0.85f) {
            ((liveOuterSpread - SELF_SPREAD_OUTER_TARGET).coerceIn(-0.22f, 0.26f) * 0.16f) * smoothStep(0.85f, 1.0f, p)
        } else {
            0f
        }
        val lateralGlide = spreadT * 0.114f + manualAdjust

        var wrist = Vec3(
            wristArc.x + side * lateralGlide,
            wristArc.y - 0.01f * spreadT,
            wristArc.z + 0.080f * spreadT
        )

        // Clamp wrist strictly within reachable 2-bone envelope (exact bone lengths preserved!)
        val maxReach = (UPPER_ARM_LEN + FOREARM_LEN) * 0.985f
        val minReach = abs(UPPER_ARM_LEN - FOREARM_LEN) + 0.32f
        val swVec = wrist - shoulder
        val swDistRaw = swVec.len().coerceAtLeast(1e-5f)
        val swDist = swDistRaw.coerceIn(minReach, maxReach)
        if (abs(swDist - swDistRaw) > 1e-5f) {
            wrist = shoulder + swVec * (swDist / swDistRaw)
        }
        val swDir = (wrist - shoulder) * (1f / swDist)

        // 4. Analytical Law-of-Cosines Elbow Position — elbows rest relaxed DOWN toward the exam mattress (-Z)
        val restDir = (w0 - s0).normalized()
        val restElbowOffset = (e0 - s0) - restDir * (e0 - s0).dot(restDir)
        val poleRest = restElbowOffset.normalized()
        // Wider lateral elbow sweep clears full/large breasts cleanly while keeping elbows softly resting toward exam table
        val polePose = Vec3(side * 0.66f, -0.08f, -0.75f).normalized()
        val poleBlend = lerpVec(poleRest, polePose, smoothStep(0.0f, 0.72f, p)).normalized()
        var polePerp = poleBlend - swDir * poleBlend.dot(swDir)
        if (polePerp.len() < 1e-4f) {
            polePerp = Vec3(side * 0.5f, 0f, -0.86f) - swDir * (Vec3(side * 0.5f, 0f, -0.86f).dot(swDir))
        }
        polePerp = polePerp.normalized()

        val a = (UPPER_ARM_LEN * UPPER_ARM_LEN - FOREARM_LEN * FOREARM_LEN + swDist * swDist) / (2f * swDist)
        val h = sqrt((UPPER_ARM_LEN * UPPER_ARM_LEN - a * a).coerceAtLeast(0f))
        val elbow = shoulder + swDir * a + polePerp * h

        // 5. Orthonormal Bone & Hand Frames + Exact Progressive Forearm Pronation Twist
        val poseUpper = buildBoneFrame(shoulder, elbow, Vec3(0f, 0f, 1f))
        val poseForearm = buildBoneFrame(elbow, wrist, Vec3(0f, 0f, 1f))

        // Hand continues gracefully from wrist onto the outer labia majora with smooth orthonormal wrist blend
        val labialTarget = Vec3(
            side * (0.28f + lateralGlide),
            -0.06f,
            0.445f + 0.055f * spreadT
        )
        val orientT = smoothStep(0.12f, 0.64f, p)
        val targetHandY = (labialTarget - wrist).normalized()
        val poseHandY = lerpVec(restHand.axisY, targetHandY, orientT).normalized()
        // Natural medial dorsal tilt (-side) lifts the curled ulnar/ring/pinky loop cleanly ABOVE the rising anterior thighs
        // while aligning index & middle pads flush onto the outer labia majora and resting the thumb softly on upper groin
        val dorsalHint = lerpVec(
            Vec3(0f, 0f, 1f),
            Vec3(-side * (0.72f + 0.05f * spreadT), 0.18f, 0.67f).normalized(),
            orientT
        ).normalized()
        val poseHand = buildFrameFromYAndZHint(wrist, poseHandY, dorsalHint)

        // Measure smooth forearm twist angle aligning forearm dorsal normal with hand dorsal normal at the wrist
        val targetZOnForearm = (poseHand.axisZ - poseForearm.axisY * poseHand.axisZ.dot(poseForearm.axisY)).normalized()
        val cosTwist = poseForearm.axisZ.dot(targetZOnForearm).coerceIn(-1f, 1f)
        val sinTwist = (-poseForearm.axisX.dot(targetZOnForearm)).coerceIn(-1f, 1f)
        val wristTwistRad = (atan2(sinTwist, cosTwist)).coerceIn(-1.25f, 1.25f) * smoothStep(0.05f, 0.55f, p)

        val fingerCurlWeight = smoothStep(0.28f, 0.68f, p)

        return SideArmIkSolution(
            side = side,
            shoulder = shoulder,
            elbow = elbow,
            wrist = wrist,
            restUpper = restUpper,
            poseUpper = poseUpper,
            restForearm = restForearm,
            poseForearm = poseForearm,
            restHand = restHand,
            poseHand = poseHand,
            wristTwistRad = wristTwistRad,
            fingerCurlWeight = fingerCurlWeight,
            poseProgress = p,
            fingerMicroCurl = microFinger * (1f - p * 0.78f).coerceIn(0.15f, 1f),
            isActivePose = true
        )
    }

    /**
     * Returns realistic hinged phalanx angles per digit (`0 = Opposable Thumb .. 4 = Pinky`):
     *  - Thumb: naturally abducted AWAY from index (`splayRad = -0.14f`) with gentle anatomical opposition roll
     *    so it forms a clean open thumb-index V-web and rests softly atop the upper groin/thigh!
     *  - Index & Middle: gentle anatomical 3-phalanx spreading arch onto the outer labia majora
     *  - Ring & Pinky: crisp ~158-165 deg 3-bone knuckle tuck snugly into palm (NEVER >180 deg backwards J-hook!)
     *  - Optional `microCurl` (`0f..1f`) adds delicate organic finger curling/gripping flexion across all 5 digits.
     */
    fun fingerFlexForIndex(fingerIndex: Int, curlWeight: Float, microCurl: Float = 0f): FingerFlexPose {
        val w = curlWeight.coerceIn(0f, 1f)
        val m = microCurl.coerceIn(0f, 1f)
        return when (fingerIndex) {
            0 -> FingerFlexPose(
                mcpFlexRad = 0.12f * w + 0.16f * m,
                pipFlexRad = 0.18f * w + 0.22f * m,
                dipFlexRad = 0f,
                splayRad = -0.14f * w - 0.05f * m,
                rollRad = 0.46f + 0.16f * w + 0.08f * m
            )
            1 -> FingerFlexPose(
                mcpFlexRad = 0.08f * w + 0.24f * m,
                pipFlexRad = 0.18f * w + 0.34f * m,
                dipFlexRad = 0.10f * w + 0.16f * m,
                splayRad = 0.01f * w + 0.03f * m
            )
            2 -> FingerFlexPose(
                mcpFlexRad = 0.06f * w + 0.30f * m,
                pipFlexRad = 0.16f * w + 0.40f * m,
                dipFlexRad = 0.10f * w + 0.20f * m,
                splayRad = -0.02f * w
            )
            3 -> FingerFlexPose(
                mcpFlexRad = (1.18f * w + 0.34f * m).coerceAtMost(1.30f),
                pipFlexRad = (1.35f * w + 0.44f * m).coerceAtMost(1.45f),
                dipFlexRad = (0.36f * w + 0.22f * m).coerceAtMost(0.45f),
                splayRad = -0.05f * w - 0.02f * m
            )
            else -> FingerFlexPose(
                mcpFlexRad = (1.24f * w + 0.38f * m).coerceAtMost(1.34f),
                pipFlexRad = (1.38f * w + 0.46f * m).coerceAtMost(1.48f),
                dipFlexRad = (0.38f * w + 0.24f * m).coerceAtMost(0.46f),
                splayRad = -0.07f * w - 0.03f * m
            )
        }
    }

    /**
     * Applies 60 FPS smooth 2-bone IK + progressive forearm pronation skinning to Blossom's arms,
     * forearms, wrists, and sculpted high-res palms, then updates all 10 articulated fingers and
     * 10 tracking 3D fingernails in [handsMesh] and [nailsMesh].
     */
    fun applyArmAndHandPose(
        bodyMesh: DynamicGlMesh,
        palmsMesh: DynamicGlMesh,
        handsMesh: DynamicGlMesh,
        nailsMesh: DynamicGlMesh,
        cache: ArmSkinCache,
        poseProgress: Float,
        leftOuterSpread: Float,
        rightOuterSpread: Float,
        leftArmMicroShift: Float = 0f,
        rightArmMicroShift: Float = 0f,
        leftFingerMicroCurl: Float = 0f,
        rightFingerMicroCurl: Float = 0f
    ) {
        val leftSol = solveSideArmIk(-1f, poseProgress, leftOuterSpread, leftArmMicroShift, leftFingerMicroCurl)
        val rightSol = solveSideArmIk(+1f, poseProgress, rightOuterSpread, rightArmMicroShift, rightFingerMicroCurl)

        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val bIdx = cache.bodyVertIndices
        val bBase = cache.bodyBasePosNorm
        val bSides = cache.bodySides
        val wT = cache.wTorso
        val wU = cache.wUpper
        val wF = cache.wForearm
        val wH = cache.wHand
        val hFor = cache.hForearm

        for (k in bIdx.indices) {
            val sol = if (bSides[k] < 0f) leftSol else rightSol
            val src = k * 6
            val px = bBase[src]; val py = bBase[src + 1]; val pz = bBase[src + 2]
            val nx = bBase[src + 3]; val ny = bBase[src + 4]; val nz = bBase[src + 5]

            if (!sol.isActivePose) {
                val dst = bIdx[k] * stride
                bData[dst] = px; bData[dst + 1] = py; bData[dst + 2] = pz
                bData[dst + 3] = nx; bData[dst + 4] = ny; bData[dst + 5] = nz
                continue
            }

            val wt = wT[k]
            val wu = wU[k]
            val wf = wF[k]
            val wh = wH[k]

            val armPos: Vec3
            val armNorm: Vec3

            if (wf > 0.0001f && wh > 0.0001f) {
                // Volume-preserving orthonormal wrist joint blend around w0 -> wrist (ZERO wrist pinch or kink!)
                val twistedForearm = sol.poseForearm.rotatedAroundY(sol.wristTwistRad * hFor[k])
                val tBlend = wh / (wf + wh)
                val (posW, normW) = applyBlendedJointTransform(
                    px, py, pz, nx, ny, nz,
                    restPivot = sol.restHand.origin,
                    posePivot = sol.wrist,
                    restA = sol.restForearm,
                    poseA = twistedForearm,
                    restB = sol.restHand,
                    poseB = sol.poseHand,
                    t = tBlend
                )
                armPos = posW
                armNorm = normW
            } else if (wu > 0.0001f && wf > 0.0001f) {
                // Volume-preserving orthonormal elbow joint blend around e0 -> elbow (ZERO elbow collapse!)
                val twistedForearm = sol.poseForearm.rotatedAroundY(sol.wristTwistRad * hFor[k])
                val tBlend = wf / (wu + wf)
                val (posE, normE) = applyBlendedJointTransform(
                    px, py, pz, nx, ny, nz,
                    restPivot = sol.restForearm.origin,
                    posePivot = sol.elbow,
                    restA = sol.restUpper,
                    poseA = sol.poseUpper,
                    restB = sol.restForearm,
                    poseB = twistedForearm,
                    t = tBlend
                )
                armPos = posE
                armNorm = normE
            } else if (wh >= wf && wh >= wu) {
                val lp = sol.restHand.worldToLocalPoint(px, py, pz)
                val ln = sol.restHand.worldToLocalDir(nx, ny, nz)
                armPos = sol.poseHand.localToWorldPoint(lp.x, lp.y, lp.z)
                armNorm = sol.poseHand.localToWorldDir(ln.x, ln.y, ln.z)
            } else if (wf >= wu) {
                val twistedForearm = sol.poseForearm.rotatedAroundY(sol.wristTwistRad * hFor[k])
                val lp = sol.restForearm.worldToLocalPoint(px, py, pz)
                val ln = sol.restForearm.worldToLocalDir(nx, ny, nz)
                armPos = twistedForearm.localToWorldPoint(lp.x, lp.y, lp.z)
                armNorm = twistedForearm.localToWorldDir(ln.x, ln.y, ln.z)
            } else {
                val lp = sol.restUpper.worldToLocalPoint(px, py, pz)
                val ln = sol.restUpper.worldToLocalDir(nx, ny, nz)
                armPos = sol.poseUpper.localToWorldPoint(lp.x, lp.y, lp.z)
                armNorm = sol.poseUpper.localToWorldDir(ln.x, ln.y, ln.z)
            }

            val wArm = 1f - wt
            val outPos = if (wt > 0.0001f) {
                Vec3(px * wt + armPos.x * wArm, py * wt + armPos.y * wArm, pz * wt + armPos.z * wArm)
            } else {
                armPos
            }
            val outNorm = if (wt > 0.0001f) {
                Vec3(nx * wt + armNorm.x * wArm, ny * wt + armNorm.y * wArm, nz * wt + armNorm.z * wArm)
            } else {
                armNorm
            }

            val nLen = outNorm.len().coerceAtLeast(1e-5f)
            val dst = bIdx[k] * stride
            bData[dst] = outPos.x; bData[dst + 1] = outPos.y; bData[dst + 2] = outPos.z
            bData[dst + 3] = outNorm.x / nLen; bData[dst + 4] = outNorm.y / nLen; bData[dst + 5] = outNorm.z / nLen
        }
        bodyMesh.commitVertices()

        // Update sculpted high-res parametric palms, all 10 multi-joint articulated fingers & 10 matching glossy 3D fingernails
        BlossomUpperBodyBuilder.updateArticulatedPalmsMesh(
            palmsMesh = palmsMesh,
            leftSol = leftSol,
            rightSol = rightSol
        )
        BlossomUpperBodyBuilder.updateArticulatedHandsAndFingernails(
            handsMesh = handsMesh,
            nailsMesh = nailsMesh,
            leftSol = leftSol,
            rightSol = rightSol
        )
    }

    /**
     * Evaluates the world-space fingertip pad position of any finger (`0 = Thumb .. 4 = Pinky`)
     * for unit verification and anatomical contact checks.
     */
    fun computeFingertipWorldPosition(
        side: Float,
        fingerIndex: Int,
        poseProgress: Float,
        liveOuterSpread: Float
    ): Vec3 {
        val sol = solveSideArmIk(side, poseProgress, liveOuterSpread)
        val spec = BlossomUpperBodyBuilder.BLOSSOM_FINGER_SPECS[fingerIndex]
        val flex = fingerFlexForIndex(fingerIndex, sol.fingerCurlWeight)
        val spine = BlossomUpperBodyBuilder.evaluateArticulatedFingerSpine(spec, side, 0.92f, flex)
        val localPt = sol.restHand.worldToLocalPoint(spine.cx, spine.cy, spine.cz)
        return sol.poseHand.localToWorldPoint(localPt.x, localPt.y, localPt.z)
    }

    private fun buildBoneFrame(start: Vec3, end: Vec3, zHint: Vec3): OrthoFrame {
        val axisY = (end - start).normalized()
        return buildFrameFromYAndZHint(start, axisY, zHint)
    }

    private fun buildFrameFromYAndZHint(origin: Vec3, axisY: Vec3, zHint: Vec3): OrthoFrame {
        var zProj = zHint - axisY * zHint.dot(axisY)
        if (zProj.len() < 1e-4f) {
            val fallback = if (abs(axisY.z) < 0.9f) Vec3(0f, 0f, 1f) else Vec3(1f, 0f, 0f)
            zProj = fallback - axisY * fallback.dot(axisY)
        }
        val axisZ = zProj.normalized()
        val axisX = axisY.cross(axisZ).normalized()
        return OrthoFrame(origin, axisX, axisY, axisZ)
    }

    /**
     * Applies a strictly volume-preserving orthonormal rotation blend between two adjacent bones
     * meeting at `restPivot -> posePivot`. Eliminates classic LBS joint pinch & candy-wrapper collapse!
     */
    internal fun applyBlendedJointTransform(
        px: Float, py: Float, pz: Float,
        nx: Float, ny: Float, nz: Float,
        restPivot: Vec3,
        posePivot: Vec3,
        restA: OrthoFrame,
        poseA: OrthoFrame,
        restB: OrthoFrame,
        poseB: OrthoFrame,
        t: Float
    ): Pair<Vec3, Vec3> {
        // Delta-rotation columns for bone A and bone B (each maps world basis axes into posed world space)
        fun rotDir(rest: OrthoFrame, pose: OrthoFrame, vx: Float, vy: Float, vz: Float): Vec3 {
            val l = rest.worldToLocalDir(vx, vy, vz)
            return pose.localToWorldDir(l.x, l.y, l.z)
        }
        val ax = lerpVec(rotDir(restA, poseA, 1f, 0f, 0f), rotDir(restB, poseB, 1f, 0f, 0f), t).normalized()
        val ayRaw = lerpVec(rotDir(restA, poseA, 0f, 1f, 0f), rotDir(restB, poseB, 0f, 1f, 0f), t)
        val ay = (ayRaw - ax * ayRaw.dot(ax)).normalized()
        val az = ax.cross(ay).normalized()

        val rx = px - restPivot.x
        val ry = py - restPivot.y
        val rz = pz - restPivot.z
        val outPos = Vec3(
            posePivot.x + ax.x * rx + ay.x * ry + az.x * rz,
            posePivot.y + ax.y * rx + ay.y * ry + az.y * rz,
            posePivot.z + ax.z * rx + ay.z * ry + az.z * rz
        )
        val outNorm = Vec3(
            ax.x * nx + ay.x * ny + az.x * nz,
            ax.y * nx + ay.y * ny + az.y * nz,
            ax.z * nx + ay.z * ny + az.z * nz
        )
        return Pair(outPos, outNorm)
    }

    private fun cubicBezier(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, t: Float): Vec3 {
        val u = 1f - t
        val b0 = u * u * u
        val b1 = 3f * u * u * t
        val b2 = 3f * u * t * t
        val b3 = t * t * t
        return Vec3(
            p0.x * b0 + p1.x * b1 + p2.x * b2 + p3.x * b3,
            p0.y * b0 + p1.y * b1 + p2.y * b2 + p3.y * b3,
            p0.z * b0 + p1.z * b1 + p2.z * b2 + p3.z * b3
        )
    }

    private fun lerpVec(a: Vec3, b: Vec3, t: Float): Vec3 = Vec3(
        a.x + (b.x - a.x) * t,
        a.y + (b.y - a.y) * t,
        a.z + (b.z - a.z) * t
    )

    private fun closestSegmentParamAndDist(
        px: Float, py: Float, pz: Float,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float
    ): Pair<Float, Float> {
        val bax = bx - ax; val bay = by - ay; val baz = bz - az
        val ba2 = (bax * bax + bay * bay + baz * baz).coerceAtLeast(1e-6f)
        val h = (((px - ax) * bax + (py - ay) * bay + (pz - az) * baz) / ba2).coerceIn(0f, 1f)
        val dx = px - (ax + bax * h)
        val dy = py - (ay + bay * h)
        val dz = pz - (az + baz * h)
        return Pair(h, sqrt(dx * dx + dy * dy + dz * dz))
    }

    private fun dist3(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        val dx = ax - bx; val dy = ay - by; val dz = az - bz
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
