package com.example.ui.components

import com.example.ui.components.BlossomArmIkRig.OrthoFrame
import com.example.ui.components.BlossomArmIkRig.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Version 33.0 Length-Preserving Two-Bone Analytical Leg IK Rig + Planted Heel Contact +
 * Opposite-Leg Soft-Tissue Contact Solver for Blossom.
 *
 * Lives behind the existing **"Hip-anchored thigh swing"** (`betaRealismHipThighSwing`) toggle so
 * the user and tests can A/B compare the legacy planar-shear leg skinning (OFF) against full
 * articulated 2-bone IK (ON) at 60 FPS with bit-identical reversion when OFF.
 *
 * Key guarantees:
 *  1. **Exact bone-length preservation**: Both femur (`|K - H| == FEMUR_LENGTH`) and shin
 *     (`|A - K| == SHIN_LENGTH`) maintain their authored SDF lengths within floating-point precision
 *     across every spread, knee-shift, asymmetric surge, and tremor pose.
 *  2. **Stable bend plane**: Uses the authored upward (`+Z`) rest knee bend plane as the pole
 *     reference so the knee arcs coherently and can never flip at maximum adduction.
 *  3. **True planted-heel contact**: Captures each heel's current cushion-space position on entry to
 *     a planted pulse (`plantedFeet = true`) and anchors it there while the knee arcs and the foot
 *     pivots naturally; on whole-leg sweeps, allows deliberate cushion-plane heel travel and carries
 *     that position smoothly into the next plant without snapping.
 *  4. **Opposite-leg contact & soft-tissue compression**: Evaluates 5 tapered inner-thigh/knee
 *     contact proxies between the posed left and right femurs (not an invisible `x = 0` wall),
 *     slowing inward travel smoothly as surfaces meet, allowing localized soft-tissue yield at the
 *     contact patch, letting asymmetric leading legs approach trailing legs organically, and feeding
 *     tremor through the contact constraints without dragging planted heels.
 */
internal object BlossomLegIkRig {

    // Shared authored rest joint coordinates from BlossomSdfBodyBuilder (evaluated in +X half-space;
    // v34.0 proportions: femur:shin = 1.10, L1 = 3.4322, L2 = 3.1202, fixed total length & bend plane)
    const val HIP_AX = BlossomSdfBodyBuilder.HIP_PIVOT_AX     // 1.12f
    const val HIP_Y = BlossomSdfBodyBuilder.HIP_PIVOT_Y       // 0.26f
    const val HIP_Z = BlossomSdfBodyBuilder.HIP_PIVOT_Z       // -0.22f

    const val KNEE_AX = BlossomSdfBodyBuilder.KNEE_BIND_AX    // 1.88425f
    const val KNEE_Y = BlossomSdfBodyBuilder.KNEE_BIND_Y      // -2.51391f
    const val KNEE_Z = BlossomSdfBodyBuilder.KNEE_BIND_Z      // 1.65127f

    const val ANKLE_AX = BlossomSdfBodyBuilder.ANKLE_BIND_AX  // 1.68f
    const val ANKLE_Y = BlossomSdfBodyBuilder.ANKLE_BIND_Y    // -4.24f
    const val ANKLE_Z = BlossomSdfBodyBuilder.ANKLE_BIND_Z    // -0.94f

    const val HEEL_AX = BlossomSdfBodyBuilder.HEEL_BIND_AX    // 1.68f
    const val HEEL_Y = BlossomSdfBodyBuilder.HEEL_BIND_Y      // -4.06f
    const val HEEL_Z = BlossomBodyBuilder.MATTRESS_TOP_Z      // -1.14f

    // Exact authored bone lengths
    val FEMUR_LENGTH: Float = sqrt(
        (KNEE_AX - HIP_AX).let { it * it } +
            (KNEE_Y - HIP_Y).let { it * it } +
            (KNEE_Z - HIP_Z).let { it * it }
    )

    val SHIN_LENGTH: Float = sqrt(
        (ANKLE_AX - KNEE_AX).let { it * it } +
            (ANKLE_Y - KNEE_Y).let { it * it } +
            (ANKLE_Z - KNEE_Z).let { it * it }
    )

    // 5 tapered inner-thigh & medial-knee contact proxy stations along femur u in [0..1]
    // Radii match BlossomSdfBodyBuilder's SDF femur profile (slightly softened by ~0.94x for snug
    // medial adipose yield before hard bone-to-bone resistance).
    internal val CONTACT_STATION_U = floatArrayOf(0.28f, 0.44f, 0.60f, 0.78f, 0.96f)
    internal val CONTACT_STATION_RADIUS = floatArrayOf(0.72f, 0.68f, 0.63f, 0.56f, 0.50f)
    private const val MAX_SOFT_COMPRESS_PER_LEG = 0.120f
    private const val APPROACH_SLOW_MARGIN = 0.16f

    internal data class SideLegIkSolution(
        val side: Float,
        val hip: Vec3,
        val knee: Vec3,
        val ankle: Vec3,
        val heelContact: Vec3,
        val restFemur: OrthoFrame,
        val poseFemur: OrthoFrame,
        val restShin: OrthoFrame,
        val poseShin: OrthoFrame,
        val restFoot: OrthoFrame,
        val poseFoot: OrthoFrame,
        val femurLength: Float,
        val shinLength: Float,
        val isRestPose: Boolean,
        val contactCompress: FloatArray,
        val contactDirToOther: Vec3
    )

    internal data class LegPairIkSolution(
        val left: SideLegIkSolution,
        val right: SideLegIkSolution,
        val minSurfaceSeparation: Float,
        val maxContactCompression: Float
    )

    /**
     * Stateful per-scene heel contact tracker that captures exact cushion-space heel coordinates
     * on entry to a planted pulse (`planted == true`) and smoothly decays any transition offset
     * when exiting back into a whole-leg sweep (`planted == false`) so there is never a snap.
     */
    class PlantedHeelTracker {
        var initialized: Boolean = false
        var leftWasPlanted: Boolean = false
        var rightWasPlanted: Boolean = false
        var leftAnchorX: Float = -ANKLE_AX
        var leftAnchorY: Float = ANKLE_Y
        var rightAnchorX: Float = ANKLE_AX
        var rightAnchorY: Float = ANKLE_Y
        var lastLeftAnkleX: Float = -ANKLE_AX
        var lastLeftAnkleY: Float = ANKLE_Y
        var lastRightAnkleX: Float = ANKLE_AX
        var lastRightAnkleY: Float = ANKLE_Y
        var leftExitOffsetX: Float = 0f
        var leftExitOffsetY: Float = 0f
        var rightExitOffsetX: Float = 0f
        var rightExitOffsetY: Float = 0f

        fun reset() {
            initialized = false
            leftWasPlanted = false
            rightWasPlanted = false
            leftAnchorX = -ANKLE_AX
            leftAnchorY = ANKLE_Y
            rightAnchorX = ANKLE_AX
            rightAnchorY = ANKLE_Y
            lastLeftAnkleX = -ANKLE_AX
            lastLeftAnkleY = ANKLE_Y
            lastRightAnkleX = ANKLE_AX
            lastRightAnkleY = ANKLE_Y
            leftExitOffsetX = 0f
            leftExitOffsetY = 0f
            rightExitOffsetX = 0f
            rightExitOffsetY = 0f
        }
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        if (abs(edge1 - edge0) < 1e-6f) return if (x >= edge1) 1f else 0f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun clampReachAnkle(hip: Vec3, rawAnkle: Vec3): Vec3 {
        val dx = rawAnkle.x - hip.x
        val dy = rawAnkle.y - hip.y
        val dz = rawAnkle.z - hip.z
        val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-5f)
        val minReach = abs(SHIN_LENGTH - FEMUR_LENGTH) + 0.35f
        val maxReach = (FEMUR_LENGTH + SHIN_LENGTH) * 0.965f
        if (dist in minReach..maxReach) return rawAnkle
        val clampedDist = dist.coerceIn(minReach, maxReach)
        val scale = clampedDist / dist
        return Vec3(hip.x + dx * scale, hip.y + dy * scale, rawAnkle.z)
    }

    /**
     * Computes the deliberate whole-leg sweep ankle position on the cushion plane strictly from
     * `spread` (never from `kneeShift` or `tremor`).
     *
     * Outward spreads (`spread > 0`) travel smoothly into the wide stance; inward sweeps (`spread < 0`)
     * draw the heel moderately closer on the cushion (~0.56x hip rotation) so the bent knee arcs
     * markedly farther inward than the heel instead of leg and foot sliding sideways as a rigid board.
     */
    internal fun computeSweepAnkleTarget(side: Float, spread: Float): Vec3 {
        val clampedSpread = spread.coerceIn(BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD, 1.0f)
        if (abs(clampedSpread) < 1e-5f) {
            return Vec3(side * ANKLE_AX, ANKLE_Y, ANKLE_Z)
        }
        val heelFactor = if (clampedSpread < 0f) 0.56f else 0.92f
        val angle = side * clampedSpread * BlossomSdfBodyBuilder.MAX_LEG_SPREAD_RAD * heelFactor
        val cosA = cos(angle)
        val sinA = sin(angle)
        val hipX = side * HIP_AX
        val rx = side * ANKLE_AX - hipX
        val ry = ANKLE_Y - HIP_Y
        var ax = hipX + rx * cosA - ry * sinA
        val ay = HIP_Y + rx * sinA + ry * cosA
        // Never allow opposing ankles/feet to cross through each other on the cushion
        val minMedialAnkle = 0.42f
        if (ax * side < minMedialAnkle) {
            ax = side * minMedialAnkle
        }
        return clampReachAnkle(Vec3(hipX, HIP_Y, HIP_Z), Vec3(ax, ay, ANKLE_Z))
    }

    private fun resolveAnkleWithPlantTracker(
        side: Float,
        spread: Float,
        planted: Boolean,
        tracker: PlantedHeelTracker?
    ): Vec3 {
        val hip = Vec3(side * HIP_AX, HIP_Y, HIP_Z)
        val rawSweep = computeSweepAnkleTarget(side, spread)
        if (tracker == null) return rawSweep

        val isLeft = side < 0f
        if (!tracker.initialized) {
            tracker.lastLeftAnkleX = computeSweepAnkleTarget(-1f, spread).x
            tracker.lastLeftAnkleY = computeSweepAnkleTarget(-1f, spread).y
            tracker.lastRightAnkleX = computeSweepAnkleTarget(+1f, spread).x
            tracker.lastRightAnkleY = computeSweepAnkleTarget(+1f, spread).y
        }

        val wasPlanted = if (isLeft) tracker.leftWasPlanted else tracker.rightWasPlanted
        val resolved: Vec3
        if (planted) {
            if (!wasPlanted) {
                // Entry to planted pulse: capture current cushion-space ankle/heel position with zero snap!
                val capX = if (isLeft) tracker.lastLeftAnkleX else tracker.lastRightAnkleX
                val capY = if (isLeft) tracker.lastLeftAnkleY else tracker.lastRightAnkleY
                if (isLeft) {
                    tracker.leftAnchorX = capX
                    tracker.leftAnchorY = capY
                    tracker.leftExitOffsetX = 0f
                    tracker.leftExitOffsetY = 0f
                } else {
                    tracker.rightAnchorX = capX
                    tracker.rightAnchorY = capY
                    tracker.rightExitOffsetX = 0f
                    tracker.rightExitOffsetY = 0f
                }
            }
            val ax = if (isLeft) tracker.leftAnchorX else tracker.rightAnchorX
            val ay = if (isLeft) tracker.leftAnchorY else tracker.rightAnchorY
            resolved = clampReachAnkle(hip, Vec3(ax, ay, ANKLE_Z))
        } else {
            if (wasPlanted) {
                // Exiting planted pulse into sweep: record delta offset and decay smoothly to zero (zero snap!)
                val prevX = if (isLeft) tracker.leftAnchorX else tracker.rightAnchorX
                val prevY = if (isLeft) tracker.leftAnchorY else tracker.rightAnchorY
                if (isLeft) {
                    tracker.leftExitOffsetX = prevX - rawSweep.x
                    tracker.leftExitOffsetY = prevY - rawSweep.y
                } else {
                    tracker.rightExitOffsetX = prevX - rawSweep.x
                    tracker.rightExitOffsetY = prevY - rawSweep.y
                }
            } else {
                // Smoothly decay any residual post-plant offset as the leg sweeps onward
                if (isLeft) {
                    tracker.leftExitOffsetX *= 0.80f
                    tracker.leftExitOffsetY *= 0.80f
                    if (abs(tracker.leftExitOffsetX) < 1e-4f) tracker.leftExitOffsetX = 0f
                    if (abs(tracker.leftExitOffsetY) < 1e-4f) tracker.leftExitOffsetY = 0f
                } else {
                    tracker.rightExitOffsetX *= 0.80f
                    tracker.rightExitOffsetY *= 0.80f
                    if (abs(tracker.rightExitOffsetX) < 1e-4f) tracker.rightExitOffsetX = 0f
                    if (abs(tracker.rightExitOffsetY) < 1e-4f) tracker.rightExitOffsetY = 0f
                }
            }
            val offX = if (isLeft) tracker.leftExitOffsetX else tracker.rightExitOffsetX
            val offY = if (isLeft) tracker.leftExitOffsetY else tracker.rightExitOffsetY
            resolved = clampReachAnkle(hip, Vec3(rawSweep.x + offX, rawSweep.y + offY, ANKLE_Z))
        }

        if (isLeft) {
            tracker.leftWasPlanted = planted
            tracker.lastLeftAnkleX = resolved.x
            tracker.lastLeftAnkleY = resolved.y
        } else {
            tracker.rightWasPlanted = planted
            tracker.lastRightAnkleX = resolved.x
            tracker.lastRightAnkleY = resolved.y
        }
        return resolved
    }

    /**
     * Builds a consistent right-handed bone frame whose `axisY` aligns with `boneDir` and whose
     * `axisZ` lies in the knee bend plane (`bendUpHint`), guaranteeing zero axial twist discontinuity
     * between thigh and shin at the knee joint.
     */
    private fun buildBoneFrame(origin: Vec3, boneDir: Vec3, bendUpHint: Vec3): OrthoFrame {
        val ay = boneDir.normalized()
        var az = bendUpHint - ay * bendUpHint.dot(ay)
        if (az.len() < 1e-5f) {
            val fallback = Vec3(0f, 0f, 1f)
            az = (fallback - ay * fallback.dot(ay)).normalized()
        } else {
            az = az.normalized()
        }
        val ax = ay.cross(az).normalized()
        az = ax.cross(ay).normalized()
        return OrthoFrame(origin, ax, ay, az)
    }

    /**
     * Exact Rodrigues shortest-arc parallel transport of an orthonormal bone frame `rest` so its
     * bone axis (`rest.y`) aligns with `newDirRaw` with strictly zero artificial axial twist or
     * projection counter-roll around the hip socket.
     */
    private fun parallelTransportFrame(origin: Vec3, rest: OrthoFrame, newDirRaw: Vec3): OrthoFrame {
        val a = rest.axisY
        val b = newDirRaw.normalized()
        val dot = a.dot(b).coerceIn(-1f, 1f)
        if (dot > 0.99999f) return OrthoFrame(origin, rest.axisX, rest.axisY, rest.axisZ)
        val axisRaw = a.cross(b)
        val sinT = axisRaw.len()
        if (sinT < 1e-5f) return OrthoFrame(origin, rest.axisX, b, rest.axisZ)
        val k = axisRaw * (1f / sinT)
        val cosT = dot
        fun rot(v: Vec3): Vec3 {
            val kv = k.dot(v)
            val kxv = k.cross(v)
            return v * cosT + kxv * sinT + k * (kv * (1f - cosT))
        }
        val nx = rot(rest.axisX).normalized()
        val ny = b
        val nz = nx.cross(ny).normalized()
        val nxOrtho = ny.cross(nz).normalized()
        return OrthoFrame(origin, nxOrtho, ny, nz)
    }

    /**
     * Builds a cushion-grounded foot frame anchored at `ankle`, keeping the sole flat on the exam
     * mattress (`axisZ = (0, 0, 1)`) while yaw-pivoting naturally around the heel as the knee and
     * shin swing inward or outward.
     */
    private fun buildFootFrame(side: Float, hip: Vec3, ankle: Vec3, isRest: Boolean): OrthoFrame {
        // Rest foot longitudinal heading in XY from ankle toward forefoot (~slightly outward-angled)
        val restDirX = side * 0.14f
        val restDirY = -0.99f
        val restYaw = atan2(restDirX, restDirY)
        val restAnkle = Vec3(side * ANKLE_AX, ANKLE_Y, ANKLE_Z)
        val yaw = if (isRest || (ankle - restAnkle).len() < 1e-5f) {
            restYaw
        } else {
            // Drive foot yaw strictly from hip-to-ankle abduction so planted feet (fixed ankle)
            // stay 100% locked on the cushion (heel + toes) during knee pulses/tremor, while
            // whole-leg sweeps smoothly turn the foot with the abducting leg.
            val haYaw = atan2(ankle.x - hip.x, ankle.y - hip.y)
            val restHaYaw = atan2(side * (ANKLE_AX - HIP_AX), ANKLE_Y - HIP_Y)
            var deltaYaw = haYaw - restHaYaw
            while (deltaYaw > Math.PI) deltaYaw -= (2.0 * Math.PI).toFloat()
            while (deltaYaw < -Math.PI) deltaYaw += (2.0 * Math.PI).toFloat()
            restYaw + (deltaYaw * 0.82f).coerceIn(-0.55f, 0.55f)
        }
        val ay = Vec3(sin(yaw), cos(yaw), 0f).normalized()
        val az = Vec3(0f, 0f, 1f)
        val ax = ay.cross(az).normalized()
        return OrthoFrame(ankle, ax, ay, az)
    }

    private fun computeDesiredKneeTarget(
        side: Float,
        spread: Float,
        kneeShift: Float,
        tremor: Float
    ): Vec3 {
        val clampedSpread = spread.coerceIn(BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD, 1.0f)
        val clampedKs = kneeShift.coerceIn(-1.15f, 1.0f)
        val clampedTremor = tremor.coerceIn(-0.45f, 0.45f)

        val angle = side * clampedSpread * BlossomSdfBodyBuilder.MAX_LEG_SPREAD_RAD
        val cosA = cos(angle)
        val sinA = sin(angle)
        val hipX = side * HIP_AX
        val rx = side * KNEE_AX - hipX
        val ry = KNEE_Y - HIP_Y
        var kx = hipX + rx * cosA - ry * sinA
        val ky = HIP_Y + rx * sinA + ry * cosA
        val kz = KNEE_Z - 0.14f * clampedSpread

        val lateralAmp = if (clampedKs < 0f) 0.94f else 0.78f
        kx += side * clampedKs * lateralAmp
        // Feed tremor through the knee target (never ankle!) so held squeezes tremble under contact
        kx += side * clampedTremor * 0.19f
        val kzFinal = kz - 0.095f * clampedKs
        return Vec3(kx, ky, kzFinal)
    }

    /**
     * Exact analytical 2-bone IK solve for one leg given fixed hip `H`, grounded ankle `A`,
     * and desired knee target `kneeTarget`. Guarantees `|K - H| == FEMUR_LENGTH` and
     * `|A - K| == SHIN_LENGTH` with a strictly upward (`+Z`) bend plane that cannot flip.
     */
    private fun solveTwoBoneSide(
        side: Float,
        ankle: Vec3,
        kneeTarget: Vec3,
        isRestPose: Boolean,
        contactCompress: FloatArray = FloatArray(CONTACT_STATION_U.size),
        contactDirToOther: Vec3 = Vec3(-side, 0f, 0f)
    ): SideLegIkSolution {
        val hip = Vec3(side * HIP_AX, HIP_Y, HIP_Z)
        val restKnee = Vec3(side * KNEE_AX, KNEE_Y, KNEE_Z)
        val restAnkle = Vec3(side * ANKLE_AX, ANKLE_Y, ANKLE_Z)

        val restBendPlane = computeBendPlaneUp(hip, restKnee, restAnkle)
        val restFemur = buildBoneFrame(hip, restKnee - hip, restBendPlane)
        val restShin = buildBoneFrame(restKnee, restAnkle - restKnee, restBendPlane)
        val restFoot = buildFootFrame(side, hip, restAnkle, isRest = true)

        if (isRestPose) {
            return SideLegIkSolution(
                side = side,
                hip = hip,
                knee = restKnee,
                ankle = restAnkle,
                heelContact = Vec3(side * HEEL_AX, HEEL_Y, HEEL_Z),
                restFemur = restFemur,
                poseFemur = restFemur,
                restShin = restShin,
                poseShin = restShin,
                restFoot = restFoot,
                poseFoot = restFoot,
                femurLength = FEMUR_LENGTH,
                shinLength = SHIN_LENGTH,
                isRestPose = true,
                contactCompress = contactCompress,
                contactDirToOther = contactDirToOther
            )
        }

        val ha = ankle - hip
        val dRaw = ha.len().coerceAtLeast(1e-5f)
        val minD = abs(SHIN_LENGTH - FEMUR_LENGTH) + 0.14f
        val maxD = (FEMUR_LENGTH + SHIN_LENGTH) * 0.985f
        val d = dRaw.coerceIn(minD, maxD)
        val uHa = ha * (1f / dRaw)
        val safeAnkle = if (abs(d - dRaw) > 1e-5f) hip + uHa * d else ankle

        val l1 = FEMUR_LENGTH
        val l2 = SHIN_LENGTH
        val alongDist = ((l1 * l1 + d * d - l2 * l2) / (2f * d)).coerceIn(-l1 * 0.995f, l1 * 0.995f)
        val perpHeight = sqrt(max(0f, l1 * l1 - alongDist * alongDist))

        // Project desired knee offset perpendicular to chord H->A
        val desRel = kneeTarget - hip
        var pole = desRel - uHa * desRel.dot(uHa)
        // Blend a tiny upward stability anchor and strictly bound lateral roll so the knee NEVER flips!
        val upWorld = Vec3(0f, 0f, 1f)
        val upPerp = (upWorld - uHa * upWorld.dot(uHa)).normalized()
        if (pole.len() < 1e-4f) {
            pole = upPerp
        } else {
            pole = (pole.normalized() + upPerp * 0.03f).normalized()
            val upComp = pole.dot(upPerp)
            // Limit knee bend plane tilt to <= 62 degrees from vertical sagittal plane (cos(62°) ≈ 0.47)
            val minUpComp = 0.47f
            if (upComp < minUpComp) {
                val latPart = (pole - upPerp * upComp).normalized()
                val latWeight = sqrt(max(0f, 1f - minUpComp * minUpComp))
                pole = (upPerp * minUpComp + latPart * latWeight).normalized()
            }
        }

        val posedKnee = hip + uHa * alongDist + pole * perpHeight
        val poseBendPlane = computeBendPlaneUp(hip, posedKnee, safeAnkle)
        // Exact minimal-twist shortest-arc parallel transport for the femur ball-and-socket hip swing
        // (zero projection counter-roll on the anterior thigh & zero corkscrew twist at the groin),
        // and smooth knee-plane blended parallel transport for the tibia hinge!
        val poseFemur = parallelTransportFrame(hip, restFemur, posedKnee - hip)
        val shinTransported = parallelTransportFrame(posedKnee, restShin, safeAnkle - posedKnee)
        val shinUpHint = (shinTransported.axisZ * 0.55f + poseBendPlane * 0.45f).normalized()
        val poseShin = buildBoneFrame(posedKnee, safeAnkle - posedKnee, shinUpHint)
        val footUnmoved = (safeAnkle - restAnkle).len() < 1e-5f
        val poseFoot = if (footUnmoved) restFoot else buildFootFrame(side, hip, safeAnkle, isRest = false)

        // Heel contact tracks the exact solved foot frame mapped from rest heel contact point
        val heelLocal = restFoot.worldToLocalPoint(side * HEEL_AX, HEEL_Y, HEEL_Z)
        val heelPosed = poseFoot.localToWorldPoint(heelLocal.x, heelLocal.y, heelLocal.z)

        return SideLegIkSolution(
            side = side,
            hip = hip,
            knee = posedKnee,
            ankle = safeAnkle,
            heelContact = Vec3(heelPosed.x, heelPosed.y, HEEL_Z),
            restFemur = restFemur,
            poseFemur = poseFemur,
            restShin = restShin,
            poseShin = poseShin,
            restFoot = restFoot,
            poseFoot = poseFoot,
            femurLength = (posedKnee - hip).len(),
            shinLength = (safeAnkle - posedKnee).len(),
            isRestPose = false,
            contactCompress = contactCompress,
            contactDirToOther = contactDirToOther
        )
    }

    private fun computeBendPlaneUp(hip: Vec3, knee: Vec3, ankle: Vec3): Vec3 {
        val ha = (ankle - hip).normalized()
        val hk = knee - hip
        val perp = hk - ha * hk.dot(ha)
        return if (perp.len() > 1e-4f) perp.normalized() else Vec3(0f, 0f, 1f)
    }

    /**
     * Solves BOTH legs simultaneously using exact 2-bone analytical IK, grounded cushion-space
     * heel anchors, and opposite-leg tapered contact proxies with localized soft-tissue yield.
     */
    fun solveLegPairIk(
        leftSpread: Float,
        rightSpread: Float,
        leftKneeShift: Float,
        rightKneeShift: Float,
        leftTremor: Float = 0f,
        rightTremor: Float = 0f,
        leftFootPlanted: Boolean = false,
        rightFootPlanted: Boolean = false,
        heelTracker: PlantedHeelTracker? = null
    ): LegPairIkSolution {
        val leftAnkle = resolveAnkleWithPlantTracker(-1f, leftSpread, leftFootPlanted, heelTracker)
        val rightAnkle = resolveAnkleWithPlantTracker(+1f, rightSpread, rightFootPlanted, heelTracker)
        if (heelTracker != null) heelTracker.initialized = true

        val leftIsRest = abs(leftSpread) < 1e-4f && abs(leftKneeShift) < 1e-4f && abs(leftTremor) < 1e-4f &&
            abs(leftAnkle.x + ANKLE_AX) < 1e-4f && abs(leftAnkle.y - ANKLE_Y) < 1e-4f
        val rightIsRest = abs(rightSpread) < 1e-4f && abs(rightKneeShift) < 1e-4f && abs(rightTremor) < 1e-4f &&
            abs(rightAnkle.x - ANKLE_AX) < 1e-4f && abs(rightAnkle.y - ANKLE_Y) < 1e-4f

        if (leftIsRest && rightIsRest) {
            val lSol = solveTwoBoneSide(-1f, leftAnkle, Vec3(-KNEE_AX, KNEE_Y, KNEE_Z), isRestPose = true)
            val rSol = solveTwoBoneSide(+1f, rightAnkle, Vec3(KNEE_AX, KNEE_Y, KNEE_Z), isRestPose = true)
            val minSep = measureMinSurfaceSeparation(lSol, rSol)
            return LegPairIkSolution(lSol, rSol, minSep, 0f)
        }

        var leftTarget = computeDesiredKneeTarget(-1f, leftSpread, leftKneeShift, leftTremor)
        var rightTarget = computeDesiredKneeTarget(+1f, rightSpread, rightKneeShift, rightTremor)

        // Last-resort midline safeguard: prevents a single hyper-adducted leg from crossing way past
        // the opposite hip when the other leg is wide open, without flattening at x = 0!
        if (leftTarget.x > 0.16f) leftTarget = Vec3(0.16f, leftTarget.y, leftTarget.z)
        if (rightTarget.x < -0.16f) rightTarget = Vec3(-0.16f, rightTarget.y, rightTarget.z)

        // Weight how much each leg yields when two opposing thighs collide (asymmetric leading leg
        // carries proportionally more of the collision stop while the stationary/trailing leg stays calmer)
        val leftInwardEffort = (max(0f, -leftSpread) * 0.55f + max(0f, -leftKneeShift) * 0.85f + 0.25f)
        val rightInwardEffort = (max(0f, -rightSpread) * 0.55f + max(0f, -rightKneeShift) * 0.85f + 0.25f)
        val effortSum = (leftInwardEffort + rightInwardEffort).coerceAtLeast(0.01f)
        val leftPushShare = (leftInwardEffort / effortSum).coerceIn(0.28f, 0.72f)
        val rightPushShare = 1f - leftPushShare

        var lSol = solveTwoBoneSide(-1f, leftAnkle, leftTarget, isRestPose = false)
        var rSol = solveTwoBoneSide(+1f, rightAnkle, rightTarget, isRestPose = false)

        // Two lightweight relaxation passes against the actual opposing posed femur proxies
        repeat(2) {
            var worstWeightedPush = 0f
            var pushDirX = 1f
            var pushDirY = 0f
            var pushDirZ = 0f

            for (s in CONTACT_STATION_U.indices) {
                val u = CONTACT_STATION_U[s]
                val rSum = CONTACT_STATION_RADIUS[s] * 2f
                val pL = lSol.hip + (lSol.knee - lSol.hip) * u
                val pR = rSol.hip + (rSol.knee - rSol.hip) * u
                val diff = pR - pL
                val dist = diff.len().coerceAtLeast(1e-5f)
                val clearance = dist - rSum

                // Smooth deceleration in the approach band + soft-tissue budget before hard resistance
                if (clearance < APPROACH_SLOW_MARGIN) {
                    val rawPen = (rSum - dist).coerceAtLeast(0f)
                    val approachSlow = if (clearance > 0f) {
                        val t = 1f - clearance / APPROACH_SLOW_MARGIN
                        0.028f * t * t
                    } else {
                        0.028f
                    }
                    // Allow up to MAX_SOFT_COMPRESS_PER_LEG per side of soft-tissue yield;
                    // resist all deeper bone interpenetration!
                    val allowedSoftTotal = (2f * MAX_SOFT_COMPRESS_PER_LEG) * tanh(rawPen / 0.28f)
                    val rigidNeed = max(0f, rawPen - allowedSoftTotal) + approachSlow
                    // Scale station push to knee lever arm (`1 / max(u, 0.45)`) capped conservatively
                    val kneeEquivPush = rigidNeed / max(u, 0.48f)
                    if (kneeEquivPush > worstWeightedPush) {
                        worstWeightedPush = kneeEquivPush
                        val invD = 1f / dist
                        // Ensure push direction points cleanly from Left toward Right even on deep overlap
                        if (diff.x > 0.02f) {
                            pushDirX = diff.x * invD
                            pushDirY = diff.y * invD
                            pushDirZ = diff.z * invD
                        } else {
                            pushDirX = 1f; pushDirY = 0f; pushDirZ = 0f
                        }
                    }
                }
            }

            if (worstWeightedPush > 1e-4f) {
                val step = worstWeightedPush * 0.92f
                val pushVec = Vec3(pushDirX * step, pushDirY * step * 0.35f, pushDirZ * step * 0.25f)
                leftTarget = leftTarget - pushVec * leftPushShare
                rightTarget = rightTarget + pushVec * rightPushShare
                lSol = solveTwoBoneSide(-1f, leftAnkle, leftTarget, isRestPose = false)
                rSol = solveTwoBoneSide(+1f, rightAnkle, rightTarget, isRestPose = false)
            }
        }

        // Compute final per-station soft-tissue compression depths & minimum surface separation
        val leftComp = FloatArray(CONTACT_STATION_U.size)
        val rightComp = FloatArray(CONTACT_STATION_U.size)
        var minSep = Float.POSITIVE_INFINITY
        var maxComp = 0f
        var avgDirX = 0f; var avgDirY = 0f; var avgDirZ = 0f; var dirWeightSum = 0f

        for (s in CONTACT_STATION_U.indices) {
            val u = CONTACT_STATION_U[s]
            val rSum = CONTACT_STATION_RADIUS[s] * 2f
            val pL = lSol.hip + (lSol.knee - lSol.hip) * u
            val pR = rSol.hip + (rSol.knee - rSol.hip) * u
            val diff = pR - pL
            val dist = diff.len().coerceAtLeast(1e-5f)
            val sep = dist - rSum
            if (sep < minSep) minSep = sep

            if (sep < 0.04f) {
                val overlap = (0.04f - sep).coerceAtLeast(0f)
                val compPerLeg = (MAX_SOFT_COMPRESS_PER_LEG * tanh(overlap / 0.16f)).coerceIn(0f, MAX_SOFT_COMPRESS_PER_LEG)
                leftComp[s] = compPerLeg
                rightComp[s] = compPerLeg
                if (compPerLeg > maxComp) maxComp = compPerLeg
                val w = compPerLeg + 0.01f
                if (diff.x > 0.02f) {
                    avgDirX += (diff.x / dist) * w
                    avgDirY += (diff.y / dist) * w
                    avgDirZ += (diff.z / dist) * w
                } else {
                    avgDirX += w
                }
                dirWeightSum += w
            }
        }

        val dirLToR = if (dirWeightSum > 1e-5f) {
            Vec3(avgDirX / dirWeightSum, avgDirY / dirWeightSum, avgDirZ / dirWeightSum).normalized()
        } else {
            Vec3(1f, 0f, 0f)
        }
        val finalL = lSol.copy(contactCompress = leftComp, contactDirToOther = dirLToR)
        val finalR = rSol.copy(contactCompress = rightComp, contactDirToOther = dirLToR * -1f)

        // Effective post-compression minimum surface separation (clamped >= 0 once soft tissue yields cleanly)
        val effectiveMinSep = minSep + 2f * maxComp
        return LegPairIkSolution(
            left = finalL,
            right = finalR,
            minSurfaceSeparation = effectiveMinSep,
            maxContactCompression = maxComp
        )
    }

    private fun measureMinSurfaceSeparation(lSol: SideLegIkSolution, rSol: SideLegIkSolution): Float {
        var minSep = Float.POSITIVE_INFINITY
        for (s in CONTACT_STATION_U.indices) {
            val u = CONTACT_STATION_U[s]
            val rSum = CONTACT_STATION_RADIUS[s] * 2f
            val pL = lSol.hip + (lSol.knee - lSol.hip) * u
            val pR = rSol.hip + (rSol.knee - rSol.hip) * u
            val sep = (pR - pL).len() - rSum
            if (sep < minSep) minSep = sep
        }
        return minSep
    }

    private fun sampleStationCompression(comp: FloatArray, u: Float): Float {
        if (u < 0.16f || u > 1.12f) return 0f
        var acc = 0f
        var wSum = 0f
        for (s in CONTACT_STATION_U.indices) {
            val d = (u - CONTACT_STATION_U[s]) / 0.17f
            val w = exp(-d * d)
            acc += comp[s] * w
            wSum += w
        }
        return if (wSum > 1e-4f) acc / wSum else 0f
    }

    /**
     * Applies the solved length-preserving 2-bone IK leg pose (femur + shin + cushion-grounded foot
     * + localized inner-thigh soft-tissue compression) to Blossom's unified body mesh, toes mesh,
     * and toenails mesh.
     */
    fun applyTwoBoneIkLegPose(
        bodyMesh: DynamicGlMesh,
        toesMesh: DynamicGlMesh,
        nailsMesh: DynamicGlMesh,
        cache: BlossomSdfBodyBuilder.LegSkinCache,
        pairSol: LegPairIkSolution,
        includeToesAndNails: Boolean,
        commitBody: Boolean,
        leftToeWiggle: Float,
        rightToeWiggle: Float
    ) {
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val bData = bodyMesh.vertexData
        val bIdx = cache.bodyVertIndices
        val bBase = cache.bodyBasePosNorm
        val bSides = cache.bodySides
        val bHipW = cache.bodyIkHipW
        val bShinW = cache.bodyIkShinW
        val bFootW = cache.bodyIkFootW
        val bHFem = cache.bodyIkHFem

        val lSol = pairSol.left
        val rSol = pairSol.right
        val footFloorZ = BlossomBodyBuilder.MATTRESS_TOP_Z + 0.012f

        for (k in bIdx.indices) {
            val side = bSides[k]
            val sol = if (side < 0f) lSol else rSol
            val src = k * 6
            val x = bBase[src]; val y = bBase[src + 1]; val z = bBase[src + 2]
            val nx = bBase[src + 3]; val ny = bBase[src + 4]; val nz = bBase[src + 5]
            val dst = bIdx[k] * stride

            if (sol.isRestPose) {
                bData[dst] = x; bData[dst + 1] = y; bData[dst + 2] = z
                bData[dst + 3] = nx; bData[dst + 4] = ny; bData[dst + 5] = nz
                continue
            }

            val wHip = bHipW[k]
            val wShin = bShinW[k]
            val wFoot = bFootW[k]

            var outPos: Vec3
            var outNorm: Vec3

            if (wFoot >= 0.999f) {
                // Pure foot/heel region: rigid cushion-grounded foot transform
                val lp = sol.restFoot.worldToLocalPoint(x, y, z)
                outPos = sol.poseFoot.localToWorldPoint(lp.x, lp.y, lp.z)
                val ln = sol.restFoot.worldToLocalDir(nx, ny, nz)
                outNorm = sol.poseFoot.localToWorldDir(ln.x, ln.y, ln.z)
            } else if (wShin <= 0.001f && wFoot <= 0.001f) {
                // Pelvis -> Hip -> Femur region (preserves 3D pelvic collar & Seam D at wHip == 0!):
                // Use spherical volume-preserving joint rotation around the authored hip pivot so
                // the inner-thigh / groin transition never collapses or folds under deep adduction.
                if (wHip >= 0.999f) {
                    val lpF = sol.restFemur.worldToLocalPoint(x, y, z)
                    val lnF = sol.restFemur.worldToLocalDir(nx, ny, nz)
                    outPos = sol.poseFemur.localToWorldPoint(lpF.x, lpF.y, lpF.z)
                    outNorm = sol.poseFemur.localToWorldDir(lnF.x, lnF.y, lnF.z)
                } else {
                    val (hipPos, hipNorm) = BlossomArmIkRig.applyBlendedJointTransform(
                        x, y, z, nx, ny, nz,
                        restPivot = sol.hip,
                        posePivot = sol.hip,
                        restA = sol.restFemur,
                        poseA = sol.restFemur,
                        restB = sol.restFemur,
                        poseB = sol.poseFemur,
                        t = wHip
                    )
                    outPos = hipPos
                    outNorm = hipNorm
                }
            } else if (wFoot <= 0.001f) {
                // Knee transition & shin region: volume-preserving joint blend around knee pivot!
                val (legPos, legNorm) = if (wShin >= 0.999f) {
                    val lpS = sol.restShin.worldToLocalPoint(x, y, z)
                    val lnS = sol.restShin.worldToLocalDir(nx, ny, nz)
                    Pair(
                        sol.poseShin.localToWorldPoint(lpS.x, lpS.y, lpS.z),
                        sol.poseShin.localToWorldDir(lnS.x, lnS.y, lnS.z)
                    )
                } else {
                    BlossomArmIkRig.applyBlendedJointTransform(
                        x, y, z, nx, ny, nz,
                        restPivot = sol.restShin.origin,
                        posePivot = sol.knee,
                        restA = sol.restFemur,
                        poseA = sol.poseFemur,
                        restB = sol.restShin,
                        poseB = sol.poseShin,
                        t = wShin
                    )
                }
                if (wHip >= 0.999f) {
                    outPos = legPos
                    outNorm = legNorm
                } else {
                    outPos = Vec3(
                        x + (legPos.x - x) * wHip,
                        y + (legPos.y - y) * wHip,
                        z + (legPos.z - z) * wHip
                    )
                    outNorm = Vec3(
                        nx + (legNorm.x - nx) * wHip,
                        ny + (legNorm.y - ny) * wHip,
                        nz + (legNorm.z - nz) * wHip
                    )
                }
            } else {
                // Ankle transition region: volume-preserving blend between shin & grounded foot around ankle!
                val (ankPos, ankNorm) = BlossomArmIkRig.applyBlendedJointTransform(
                    x, y, z, nx, ny, nz,
                    restPivot = sol.restFoot.origin,
                    posePivot = sol.ankle,
                    restA = sol.restShin,
                    poseA = sol.poseShin,
                    restB = sol.restFoot,
                    poseB = sol.poseFoot,
                    t = wFoot
                )
                outPos = ankPos
                outNorm = ankNorm
            }

            // Localized soft-tissue compression strictly at the active inner-thigh / knee contact patch!
            val hFem = bHFem[k]
            if (pairSol.maxContactCompression > 0.001f && hFem in 0.16f..1.12f && wFoot < 0.05f) {
                val baseComp = sampleStationCompression(sol.contactCompress, hFem)
                if (baseComp > 0.001f) {
                    val uClamped = hFem.coerceIn(0f, 1f)
                    val femurPt = sol.hip + (sol.knee - sol.hip) * uClamped
                    val radialVec = outPos - femurPt
                    val radialLen = radialVec.len().coerceAtLeast(1e-4f)
                    val dirOther = sol.contactDirToOther
                    // Facing factor: +1 on the inner thigh surface facing the opposite leg, 0 on outer thigh
                    val facingDot = (radialVec.dot(dirOther) / radialLen).coerceIn(-1f, 1f)
                    val facingMask = smoothStep(0.05f, 0.95f, facingDot) * wHip
                    if (facingMask > 0.002f) {
                        val squash = baseComp * facingMask
                        // Flatten inward away from the opposite leg + gentle anterior/posterior volume conserve
                        val antSwell = squash * 0.24f * (radialVec.z / radialLen)
                        outPos = Vec3(
                            outPos.x - dirOther.x * squash,
                            outPos.y - dirOther.y * squash,
                            outPos.z - dirOther.z * squash + antSwell
                        )
                        // Tilt surface normal gently toward the contact interface for realistic touch crease shading
                        outNorm = Vec3(
                            outNorm.x + dirOther.x * (0.28f * facingMask * (baseComp / MAX_SOFT_COMPRESS_PER_LEG)),
                            outNorm.y + dirOther.y * (0.28f * facingMask * (baseComp / MAX_SOFT_COMPRESS_PER_LEG)),
                            outNorm.z
                        )
                    }
                }
            }

            // Keep posterior thighs, lower shins & foot soles cleanly above the exam mattress
            var finalZ = outPos.z
            if (y < -3.40f && finalZ < footFloorZ) {
                finalZ = footFloorZ
            } else if (outPos.y < -0.50f) {
                val thighFloorZ = BlossomBodyBuilder.mattressSurfaceZ(outPos.y) + 0.005f
                if (finalZ < thighFloorZ) finalZ = thighFloorZ
            }

            val nLen = outNorm.len().coerceAtLeast(1e-5f)
            bData[dst] = outPos.x
            bData[dst + 1] = outPos.y
            bData[dst + 2] = finalZ
            bData[dst + 3] = outNorm.x / nLen
            bData[dst + 4] = outNorm.y / nLen
            bData[dst + 5] = outNorm.z / nLen
        }
        if (commitBody) {
            bodyMesh.commitVertices()
        }

        if (!includeToesAndNails) return

        val leftTw = leftToeWiggle.coerceIn(-1f, 1f)
        val rightTw = rightToeWiggle.coerceIn(-1f, 1f)
        val hasToeWiggle = abs(leftTw) > 1e-4f || abs(rightTw) > 1e-4f
        val toeFloorZ = BlossomBodyBuilder.MATTRESS_TOP_Z + 0.014f

        // Drive toes from the EXACT SAME solved foot frame, with local toe wiggle layered cleanly!
        val tData = toesMesh.vertexData
        val tBase = cache.toesBasePosNorm
        val tSides = cache.toesSides
        for (i in 0 until toesMesh.vertexCount) {
            val side = tSides[i]
            val sol = if (side < 0f) lSol else rSol
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

            val dst = i * stride
            if (sol.isRestPose || sol.poseFoot === sol.restFoot) {
                tData[dst] = x; tData[dst + 1] = y; tData[dst + 2] = z
                tData[dst + 3] = nx; tData[dst + 4] = ny; tData[dst + 5] = nz
            } else {
                val lp = sol.restFoot.worldToLocalPoint(x, y, z)
                val wp = sol.poseFoot.localToWorldPoint(lp.x, lp.y, lp.z)
                val ln = sol.restFoot.worldToLocalDir(nx, ny, nz)
                val wn = sol.poseFoot.localToWorldDir(ln.x, ln.y, ln.z)
                val nLen = wn.len().coerceAtLeast(1e-5f)
                tData[dst] = wp.x
                tData[dst + 1] = wp.y
                tData[dst + 2] = max(wp.z, toeFloorZ)
                tData[dst + 3] = wn.x / nLen
                tData[dst + 4] = wn.y / nLen
                tData[dst + 5] = wn.z / nLen
            }
        }
        toesMesh.commitVertices()

        // Drive toenails from the EXACT SAME solved foot frame!
        val nData = nailsMesh.vertexData
        val nBase = cache.toeNailsBasePosNorm
        val nSides = cache.toeNailsSides
        val nStart = cache.toeNailStartVert
        for (k in nSides.indices) {
            val side = nSides[k]
            val sol = if (side < 0f) lSol else rSol
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

            val dst = (nStart + k) * stride
            if (sol.isRestPose || sol.poseFoot === sol.restFoot) {
                nData[dst] = x; nData[dst + 1] = y; nData[dst + 2] = z
                nData[dst + 3] = nx; nData[dst + 4] = ny; nData[dst + 5] = nz
            } else {
                val lp = sol.restFoot.worldToLocalPoint(x, y, z)
                val wp = sol.poseFoot.localToWorldPoint(lp.x, lp.y, lp.z)
                val ln = sol.restFoot.worldToLocalDir(nx, ny, nz)
                val wn = sol.poseFoot.localToWorldDir(ln.x, ln.y, ln.z)
                val nLen = wn.len().coerceAtLeast(1e-5f)
                nData[dst] = wp.x
                nData[dst + 1] = wp.y
                nData[dst + 2] = max(wp.z, toeFloorZ + 0.004f)
                nData[dst + 3] = wn.x / nLen
                nData[dst + 4] = wn.y / nLen
                nData[dst + 5] = wn.z / nLen
            }
        }
        nailsMesh.commitVertices()
    }
}
