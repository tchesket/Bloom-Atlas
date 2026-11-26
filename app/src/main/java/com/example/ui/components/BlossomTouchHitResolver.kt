package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal enum class TouchTargetZone {
    NONE,
    VULVA,
    LEFT_BREAST,
    RIGHT_BREAST,
    HEAD,
    LEFT_LEG,
    RIGHT_LEG,
    LEFT_GLUTE,
    RIGHT_GLUTE
}

internal data class ProjectedVulvaFrame(
    val center: Offset,
    val dirX: Offset,
    val dirY: Offset,
    val exactRx: Float,
    val exactRy: Float,
    val hitRx: Float,
    val hitRy: Float
) {
    /**
     * Maps any normalized screen point into local vulva coordinates `(vulvaNx, vulvaNy)` in `[0..1]`,
     * where `(0.5, 0.5)` is vulva center, `0.14..0.86` spans left-to-right outer labia, and
     * `0.08..0.92` spans mons pubis to perineum—accurate at any zoom, pan, or 3/4 camera angle.
     */
    fun screenToLocalNorm(screenPt: Offset): Offset {
        val relX = screenPt.x - center.x
        val relY = screenPt.y - center.y
        val u = (relX * dirX.x + relY * dirX.y) / exactRx.coerceAtLeast(0.015f)
        val v = (relX * dirY.x + relY * dirY.y) / exactRy.coerceAtLeast(0.020f)
        return Offset(
            x = (0.5f + u * 0.36f).coerceIn(0.04f, 0.96f),
            y = (0.5f + v * 0.42f).coerceIn(0.04f, 0.96f)
        )
    }

    /**
     * Converts a normalized screen drag delta into local vulva-relative drag delta `(dx, dy)`
     * so opening labia or lifting the hood feels consistent across Macro, Exam, 3/4, and Full Body views.
     */
    fun screenDeltaToLocalDelta(rawDx: Float, rawDy: Float): Offset {
        val projX = rawDx * dirX.x + rawDy * dirX.y
        val projY = rawDx * dirY.x + rawDy * dirY.y
        val spanX = (exactRx * 2.0f).coerceAtLeast(0.09f)
        val spanY = (exactRy * 2.0f).coerceAtLeast(0.11f)
        return Offset(
            x = projX / (spanX * 1.45f),
            y = projY / (spanY * 1.45f)
        )
    }
}

internal data class TouchHitResult(
    val zone: TouchTargetZone,
    val vulvaLocalNorm: Offset? = null,
    val vulvaFrame: ProjectedVulvaFrame? = null
)

internal data class ProjectedScreenPoint(
    val x: Float,
    val y: Float,
    val depth: Float
) {
    fun toOffset(): Offset = Offset(x, y)
}

/**
 * Pure-Kotlin unclamped 3D camera projector matching [VulvaGlTextureView.computeMatrices] exactly.
 * Never clamps off-screen points into `0.05..0.95` (preventing ghost hitboxes along screen edges)
 * and runs deterministically in both runtime and local JVM unit tests without `android.opengl.Matrix`.
 */
internal class BlossomCameraProjector(
    cam: VulvaCamera3D,
    aspect: Float
) {
    private val safeAspect = aspect.coerceAtLeast(0.1f)
    private val focal = (1.0 / tan(Math.toRadians(20.0))).toFloat() // fovy = 40 deg

    private val eyeX: Float
    private val eyeY: Float
    private val eyeZ: Float

    private val rightX: Float
    private val rightY: Float
    private val rightZ: Float

    private val upX: Float
    private val upY: Float
    private val upZ: Float

    private val fwdX: Float
    private val fwdY: Float
    private val fwdZ: Float

    init {
        val radius = 2.82f / cam.zoom.coerceIn(0.10f, 1.95f)
        val yawRad = Math.toRadians(cam.yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(cam.pitchDeg.toDouble()).toFloat()

        eyeX = radius * cos(pitchRad) * sin(yawRad)
        eyeY = cam.targetY + radius * sin(pitchRad)
        eyeZ = cam.targetZ + radius * cos(pitchRad) * cos(yawRad)

        val fx = 0f - eyeX
        val fy = cam.targetY - eyeY
        val fz = cam.targetZ - eyeZ
        val fLen = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-6f)
        fwdX = fx / fLen
        fwdY = fy / fLen
        fwdZ = fz / fLen

        // right = normalize(fwd x worldUp(0, 1, 0)) = (-fwdZ, 0, fwdX)
        val rx = -fwdZ
        val rz = fwdX
        val rLen = sqrt(rx * rx + rz * rz).coerceAtLeast(1e-6f)
        rightX = rx / rLen
        rightY = 0f
        rightZ = rz / rLen

        // camUp = right x fwd
        upX = rightY * fwdZ - rightZ * fwdY
        upY = rightZ * fwdX - rightX * fwdZ
        upZ = rightX * fwdY - rightY * fwdX
    }

    /**
     * Projects a 3D world point to unclamped normalized screen space `(0..1, 0..1)`.
     * Returns `null` if the point lies behind or on the camera near plane.
     */
    fun projectUnclamped(wx: Float, wy: Float, wz: Float): ProjectedScreenPoint? {
        val dx = wx - eyeX
        val dy = wy - eyeY
        val dz = wz - eyeZ

        val depth = dx * fwdX + dy * fwdY + dz * fwdZ
        if (depth <= 0.12f) return null

        val vx = dx * rightX + dy * rightY + dz * rightZ
        val vy = dx * upX + dy * upY + dz * upZ

        val ndcX = (vx * focal / safeAspect) / depth
        val ndcY = (vy * focal) / depth
        return ProjectedScreenPoint(
            x = (ndcX + 1f) * 0.5f,
            y = (1f - ndcY) * 0.5f,
            depth = depth
        )
    }

    fun projectUnclamped(worldPoint: FloatArray): ProjectedScreenPoint? =
        projectUnclamped(worldPoint[0], worldPoint[1], worldPoint[2])

    fun projectRadiusX(worldRadius: Float, depth: Float): Float =
        (worldRadius * focal) / (2f * depth.coerceAtLeast(0.18f) * safeAspect)

    fun projectRadiusY(worldRadius: Float, depth: Float): Float =
        (worldRadius * focal) / (2f * depth.coerceAtLeast(0.18f))
}

/**
 * Resolves 2D screen touches into snug, 3D-projected anatomical hit zones (Vulva, Breasts, Legs, or NONE).
 * Non-interactive regions (arms, hands, torso, face, exam table, background) cleanly resolve to [TouchTargetZone.NONE].
 */
internal object BlossomTouchHitResolver {

    fun resolveTouchZone(
        rawScreenNorm: Offset,
        cam: VulvaCamera3D,
        aspect: Float,
        deform: VulvaDeformationState,
        showFullBody: Boolean
    ): TouchHitResult {
        val projector = BlossomCameraProjector(cam, aspect)

        // 1. PRIORITY 1 — Snug 3D-Projected Vulva Ellipse (+ modest finger-friendly margin)
        val vulvaFrame = buildProjectedVulvaFrame(projector, deform)
        if (vulvaFrame != null &&
            vulvaFrame.center.x in -0.35f..1.35f &&
            vulvaFrame.center.y in -0.35f..1.35f
        ) {
            val relX = rawScreenNorm.x - vulvaFrame.center.x
            val relY = rawScreenNorm.y - vulvaFrame.center.y
            val uHit = (relX * vulvaFrame.dirX.x + relY * vulvaFrame.dirX.y) / vulvaFrame.hitRx
            val vHit = (relX * vulvaFrame.dirY.x + relY * vulvaFrame.dirY.y) / vulvaFrame.hitRy
            if (uHit * uHit + vHit * vHit <= 1.0f) {
                return TouchHitResult(
                    zone = TouchTargetZone.VULVA,
                    vulvaLocalNorm = vulvaFrame.screenToLocalNorm(rawScreenNorm),
                    vulvaFrame = vulvaFrame
                )
            }
        }

        if (!showFullBody) {
            return TouchHitResult(zone = TouchTargetZone.NONE)
        }

        // 2. PRIORITY 2 — Snug 3D-Projected Left & Right Breasts (matches raised mound, isolated from upper arms!)
        val sizeNorm = deform.breastSize.coerceIn(0f, 1f)
        val boostFrac = deform.betaBreastSizeBoost.coerceIn(0f, 0.50f)
        val effectiveBoost = if (boostFrac > 0.0001f) {
            val t = ((sizeNorm - 0.08f) / 0.20f).coerceIn(0f, 1f)
            boostFrac * t * t * (3f - 2f * t)
        } else 0f
        val worldBreastR = (0.50f + 0.12f * sizeNorm) * (1f + 0.52f * effectiveBoost)
        val breastZ = BlossomBodyBuilder.mattressSurfaceZ(BlossomUpperBodyBuilder.BREAST_CENTER_Y) +
            1.48f + 0.24f * sizeNorm + 0.46f * effectiveBoost
        val supineShiftX = 0.04f * sizeNorm
        val leftBreastW = floatArrayOf(
            -(BlossomUpperBodyBuilder.BREAST_CENTER_AX + supineShiftX) + deform.leftBreastX * 0.22f,
            BlossomUpperBodyBuilder.BREAST_CENTER_Y + deform.leftBreastY * 0.20f,
            breastZ
        )
        val rightBreastW = floatArrayOf(
            (BlossomUpperBodyBuilder.BREAST_CENTER_AX + supineShiftX) + deform.rightBreastX * 0.22f,
            BlossomUpperBodyBuilder.BREAST_CENTER_Y + deform.rightBreastY * 0.20f,
            breastZ
        )

        val leftBreastDist = evaluateBreastEllipseDist(rawScreenNorm, projector, leftBreastW, worldBreastR)
        val rightBreastDist = evaluateBreastEllipseDist(rawScreenNorm, projector, rightBreastW, worldBreastR)
        if (leftBreastDist <= 1.0f || rightBreastDist <= 1.0f) {
            val zone = if (leftBreastDist <= rightBreastDist) {
                TouchTargetZone.LEFT_BREAST
            } else {
                TouchTargetZone.RIGHT_BREAST
            }
            return TouchHitResult(zone = zone)
        }

        // 3. PRIORITY 3 — Snug 3D-Projected Head & Face Ellipse (allows persistent side-to-side face turning!)
        val headDist = evaluateHeadEllipseDist(rawScreenNorm, projector)
        if (headDist <= 1.0f) {
            return TouchHitResult(zone = TouchTargetZone.HEAD)
        }

        // 4 & 5. Snug 3D-Projected Thigh/Calf Capsules & Posterior Gluteal Cheeks (isolated from hands, arms, vulva & edges!)
        val leftLegDist = evaluateLegCapsuleDist(rawScreenNorm, projector, side = -1f, spread = deform.leftLegSpread)
        val rightLegDist = evaluateLegCapsuleDist(rawScreenNorm, projector, side = +1f, spread = deform.rightLegSpread)
        val bestLegDist = minOf(leftLegDist, rightLegDist)

        // Left & right buttock cheeks: primary medial cheeks directly left/right of her anus + outer cheeks in 3/4 views
        val leftPerianalGluteW = floatArrayOf(-0.64f, -0.90f, -0.32f)
        val rightPerianalGluteW = floatArrayOf(0.64f, -0.90f, -0.32f)
        val leftOuterGluteW = floatArrayOf(-1.14f, -0.74f, -0.78f)
        val rightOuterGluteW = floatArrayOf(1.14f, -0.74f, -0.78f)
        val leftGluteDist = minOf(
            evaluateGluteEllipseDist(rawScreenNorm, projector, leftPerianalGluteW, worldRadius = 0.40f),
            evaluateGluteEllipseDist(rawScreenNorm, projector, leftOuterGluteW, worldRadius = 0.40f)
        )
        val rightGluteDist = minOf(
            evaluateGluteEllipseDist(rawScreenNorm, projector, rightPerianalGluteW, worldRadius = 0.40f),
            evaluateGluteEllipseDist(rawScreenNorm, projector, rightOuterGluteW, worldRadius = 0.40f)
        )
        val bestGluteDist = minOf(leftGluteDist, rightGluteDist)

        if (bestGluteDist <= 1.0f && (bestLegDist > 1.0f || bestGluteDist < bestLegDist * 0.90f)) {
            val zone = if (leftGluteDist <= rightGluteDist) {
                TouchTargetZone.LEFT_GLUTE
            } else {
                TouchTargetZone.RIGHT_GLUTE
            }
            return TouchHitResult(zone = zone)
        }
        if (bestLegDist <= 1.0f) {
            val zone = if (leftLegDist <= rightLegDist) {
                TouchTargetZone.LEFT_LEG
            } else {
                TouchTargetZone.RIGHT_LEG
            }
            return TouchHitResult(zone = zone)
        }

        // 6. Everything else (arms, hands, waist, belly, shoulders, neck, mattress, background) -> NONE!
        return TouchHitResult(zone = TouchTargetZone.NONE)
    }

    fun buildProjectedVulvaFrame(
        projector: BlossomCameraProjector,
        deform: VulvaDeformationState
    ): ProjectedVulvaFrame? {
        val leftHalfW = 0.44f + deform.leftOuter.coerceIn(0f, 1f) * 0.16f
        val rightHalfW = 0.44f + deform.rightOuter.coerceIn(0f, 1f) * 0.16f
        val centerP = projector.projectUnclamped(0f, 0.04f, 0.12f) ?: return null
        val leftP = projector.projectUnclamped(-leftHalfW, 0.02f, 0.14f) ?: return null
        val rightP = projector.projectUnclamped(rightHalfW, 0.02f, 0.14f) ?: return null
        val topP = projector.projectUnclamped(0f, 0.74f, 0.24f) ?: return null
        val botP = projector.projectUnclamped(0f, -0.68f, 0.04f) ?: return null

        val axX = (rightP.x - leftP.x) * 0.5f
        val axY = (rightP.y - leftP.y) * 0.5f
        val axLen = hypot(axX, axY)
        val dirX = if (axLen > 1e-4f) Offset(axX / axLen, axY / axLen) else Offset(1f, 0f)

        val ayX = (botP.x - topP.x) * 0.5f
        val ayY = (botP.y - topP.y) * 0.5f
        val ayLen = hypot(ayX, ayY)
        val dirY = if (ayLen > 1e-4f) Offset(ayX / ayLen, ayY / ayLen) else Offset(0f, 1f)

        val exactRx = axLen.coerceAtLeast(projector.projectRadiusX(0.42f, centerP.depth))
        val exactRy = ayLen.coerceAtLeast(projector.projectRadiusY(0.68f, centerP.depth))

        // Snug anatomical boundary + small finger-friendly cushion (~14% + 0.020 screen buffer)
        val hitRx = (exactRx * 1.14f + 0.020f).coerceAtLeast(0.036f)
        val hitRy = (exactRy * 1.12f + 0.022f).coerceAtLeast(0.044f)

        return ProjectedVulvaFrame(
            center = centerP.toOffset(),
            dirX = dirX,
            dirY = dirY,
            exactRx = exactRx,
            exactRy = exactRy,
            hitRx = hitRx,
            hitRy = hitRy
        )
    }

    private fun evaluateBreastEllipseDist(
        touch: Offset,
        projector: BlossomCameraProjector,
        worldCenter: FloatArray,
        worldRadius: Float
    ): Float {
        val p = projector.projectUnclamped(worldCenter) ?: return Float.POSITIVE_INFINITY
        if (p.x !in -0.25f..1.25f || p.y !in -0.25f..1.25f) return Float.POSITIVE_INFINITY
        val rx = (projector.projectRadiusX(worldRadius, p.depth) + 0.006f).coerceIn(0.026f, 0.26f)
        val ry = (projector.projectRadiusY(worldRadius, p.depth) + 0.006f).coerceIn(0.026f, 0.26f)
        return hypot((touch.x - p.x) / rx, (touch.y - p.y) / ry)
    }

    private fun evaluateHeadEllipseDist(
        touch: Offset,
        projector: BlossomCameraProjector
    ): Float {
        val faceCenter = floatArrayOf(
            0f,
            BlossomUpperBodyBuilder.HEAD_CENTER_Y - 0.04f,
            BlossomUpperBodyBuilder.HEAD_CENTER_Z + 0.48f
        )
        val p = projector.projectUnclamped(faceCenter) ?: return Float.POSITIVE_INFINITY
        if (p.x !in -0.25f..1.25f || p.y !in -0.25f..1.25f) return Float.POSITIVE_INFINITY
        val rx = (projector.projectRadiusX(0.76f, p.depth) + 0.010f).coerceIn(0.032f, 0.34f)
        val ry = (projector.projectRadiusY(0.94f, p.depth) + 0.010f).coerceIn(0.038f, 0.42f)
        return hypot((touch.x - p.x) / rx, (touch.y - p.y) / ry)
    }

    private fun evaluateGluteEllipseDist(
        touch: Offset,
        projector: BlossomCameraProjector,
        worldCenter: FloatArray,
        worldRadius: Float = 0.40f
    ): Float {
        val p = projector.projectUnclamped(worldCenter) ?: return Float.POSITIVE_INFINITY
        if (p.x !in -0.15f..1.15f || p.y !in -0.15f..1.15f) return Float.POSITIVE_INFINITY
        val rx = (projector.projectRadiusX(worldRadius, p.depth) + 0.004f).coerceIn(0.024f, 0.22f)
        val ry = (projector.projectRadiusY(worldRadius, p.depth) + 0.004f).coerceIn(0.024f, 0.22f)
        return hypot((touch.x - p.x) / rx, (touch.y - p.y) / ry)
    }

    private fun evaluateLegCapsuleDist(
        touch: Offset,
        projector: BlossomCameraProjector,
        side: Float,
        spread: Float
    ): Float {
        // Upper thigh start sits cleanly below/lateral to vulva & hips so hands, arms & labia never collide
        val thighStart = BlossomSdfBodyBuilder.transformLegPoint(side * 1.32f, -0.48f, 0.44f, side, spread, 0.65f)
        val knee = BlossomSdfBodyBuilder.transformLegPoint(side * 1.80f, -1.86f, 1.74f, side, spread, 1.0f)
        val ankleFoot = BlossomSdfBodyBuilder.transformLegPoint(side * 1.74f, -4.60f, -0.96f, side, spread, 1.0f)

        val dThigh = evaluateProjectedSegmentDist(
            touch = touch,
            projector = projector,
            worldA = thighStart,
            worldB = knee,
            radiusA = 0.52f,
            radiusB = 0.44f
        )
        val dShin = evaluateProjectedSegmentDist(
            touch = touch,
            projector = projector,
            worldA = knee,
            worldB = ankleFoot,
            radiusA = 0.42f,
            radiusB = 0.26f
        )
        return minOf(dThigh, dShin)
    }

    private fun evaluateProjectedSegmentDist(
        touch: Offset,
        projector: BlossomCameraProjector,
        worldA: FloatArray,
        worldB: FloatArray,
        radiusA: Float,
        radiusB: Float
    ): Float {
        val pA = projector.projectUnclamped(worldA) ?: return Float.POSITIVE_INFINITY
        val pB = projector.projectUnclamped(worldB) ?: return Float.POSITIVE_INFINITY
        if ((pA.x < -0.35f && pB.x < -0.35f) || (pA.x > 1.35f && pB.x > 1.35f) ||
            (pA.y < -0.35f && pB.y < -0.35f) || (pA.y > 1.35f && pB.y > 1.35f)
        ) {
            return Float.POSITIVE_INFINITY
        }

        val abX = pB.x - pA.x
        val abY = pB.y - pA.y
        val abLen2 = abX * abX + abY * abY
        val t = if (abLen2 > 1e-6f) {
            (((touch.x - pA.x) * abX + (touch.y - pA.y) * abY) / abLen2).coerceIn(0f, 1f)
        } else {
            0f
        }
        val cx = pA.x + abX * t
        val cy = pA.y + abY * t
        val depth = pA.depth + (pB.depth - pA.depth) * t
        val rWorld = radiusA + (radiusB - radiusA) * t
        val rx = (projector.projectRadiusX(rWorld, depth) * 1.04f + 0.010f).coerceIn(0.026f, 0.24f)
        val ry = (projector.projectRadiusY(rWorld, depth) * 1.04f + 0.010f).coerceIn(0.026f, 0.24f)
        return hypot((touch.x - cx) / rx, (touch.y - cy) / ry)
    }
}
