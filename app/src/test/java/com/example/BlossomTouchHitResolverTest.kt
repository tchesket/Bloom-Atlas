package com.example

import androidx.compose.ui.geometry.Offset
import com.example.ui.components.BlossomBodyBuilder
import com.example.ui.components.BlossomCameraProjector
import com.example.ui.components.BlossomSdfBodyBuilder
import com.example.ui.components.BlossomTouchHitResolver
import com.example.ui.components.BlossomUpperBodyBuilder
import com.example.ui.components.TouchTargetZone
import com.example.ui.components.VulvaCamera3D
import com.example.ui.components.VulvaDeformationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlossomTouchHitResolverTest {

    private val aspect = 0.86f
    private val restDeform = VulvaDeformationState()

    private val examCam = VulvaCamera3D(yawDeg = 0f, pitchDeg = 5f, zoom = 1.02f, targetY = 0.02f, targetZ = 0.08f)
    private val fullBodyCam = VulvaCamera3D(yawDeg = 0f, pitchDeg = 13f, zoom = 0.140f, targetY = 1.05f, targetZ = -0.62f)
    private val chestFaceCam = VulvaCamera3D(yawDeg = 0f, pitchDeg = 6f, zoom = 0.37f, targetY = 4.54f, targetZ = -0.82f)
    private val threeQuarterLeftCam = VulvaCamera3D(yawDeg = -46f, pitchDeg = 17f, zoom = 0.160f, targetY = 1.05f, targetZ = -0.62f)

    @Test
    fun fullBodyViewIsolatesVulvaBreastsAndLegsFromArmsTorsoFaceAndBackground() {
        val projector = BlossomCameraProjector(fullBodyCam, aspect)

        fun project(x: Float, y: Float, z: Float): Offset {
            val pt = projector.projectUnclamped(x, y, z)
            assertNotNull("Expected world point ($x, $y, $z) to project in front of camera", pt)
            return pt!!.toOffset()
        }

        // 1. Direct touches on non-interactive regions MUST resolve to NONE (zero accidental vulva stretching!)
        val leftUpperArm = project(-1.60f, 3.55f, BlossomBodyBuilder.mattressSurfaceZ(3.55f) + 0.42f)
        val rightForearm = project(2.05f, 1.80f, BlossomBodyBuilder.mattressSurfaceZ(1.80f) + 0.28f)
        val leftWrist = project(-2.24f, 0.82f, BlossomBodyBuilder.mattressSurfaceZ(0.82f) + 0.20f)
        val rightHand = project(2.32f, 0.36f, BlossomBodyBuilder.mattressSurfaceZ(0.36f) + 0.18f)
        val navel = project(0f, 2.02f, BlossomBodyBuilder.mattressSurfaceZ(2.02f) + 1.48f)
        val face = project(0f, BlossomUpperBodyBuilder.HEAD_CENTER_Y, BlossomUpperBodyBuilder.HEAD_CENTER_Z + 0.72f)
        val backgroundCorner = Offset(0.08f, 0.08f)

        assertEquals("Touching left upper arm must be ignored", TouchTargetZone.NONE, resolve(leftUpperArm, fullBodyCam))
        assertEquals("Touching right forearm must be ignored", TouchTargetZone.NONE, resolve(rightForearm, fullBodyCam))
        assertEquals("Touching left wrist must be ignored", TouchTargetZone.NONE, resolve(leftWrist, fullBodyCam))
        assertEquals("Touching right hand must be ignored", TouchTargetZone.NONE, resolve(rightHand, fullBodyCam))
        assertEquals("Touching abdomen/navel must be ignored", TouchTargetZone.NONE, resolve(navel, fullBodyCam))
        assertEquals("Touching face must resolve to HEAD for side-to-side turning", TouchTargetZone.HEAD, resolve(face, fullBodyCam))
        assertEquals("Touching dark background must be ignored", TouchTargetZone.NONE, resolve(backgroundCorner, fullBodyCam))

        // 2. Direct touches on interactive zones MUST resolve accurately
        val vulvaCenter = project(0f, 0.04f, 0.12f)
        val breastZ = BlossomBodyBuilder.mattressSurfaceZ(BlossomUpperBodyBuilder.BREAST_CENTER_Y) + 1.54f
        val leftBreast = project(-BlossomUpperBodyBuilder.BREAST_CENTER_AX, BlossomUpperBodyBuilder.BREAST_CENTER_Y, breastZ)
        val rightBreast = project(BlossomUpperBodyBuilder.BREAST_CENTER_AX, BlossomUpperBodyBuilder.BREAST_CENTER_Y, breastZ)
        val leftKnee = project(-1.80f, -1.86f, 1.74f)
        val rightCalf = project(1.76f, -3.20f, 0.35f)

        val vulvaHit = BlossomTouchHitResolver.resolveTouchZone(vulvaCenter, fullBodyCam, aspect, restDeform, showFullBody = true)
        assertEquals("Direct vulva center touch must resolve to VULVA", TouchTargetZone.VULVA, vulvaHit.zone)
        val vulvaLocal = vulvaHit.vulvaLocalNorm
        assertNotNull("Vulva hit must provide local normalized coordinates", vulvaLocal)
        assertTrue("Vulva center local X should be near 0.5", vulvaLocal!!.x in 0.44f..0.56f)
        assertTrue("Vulva center local Y should be near 0.5", vulvaLocal.y in 0.44f..0.56f)

        assertEquals("Direct left breast touch must resolve to LEFT_BREAST", TouchTargetZone.LEFT_BREAST, resolve(leftBreast, fullBodyCam))
        assertEquals("Direct right breast touch must resolve to RIGHT_BREAST", TouchTargetZone.RIGHT_BREAST, resolve(rightBreast, fullBodyCam))
        assertEquals("Direct left knee touch must resolve to LEFT_LEG", TouchTargetZone.LEFT_LEG, resolve(leftKnee, fullBodyCam))
        assertEquals("Direct right calf touch must resolve to RIGHT_LEG", TouchTargetZone.RIGHT_LEG, resolve(rightCalf, fullBodyCam))
    }

    @Test
    fun examViewAndChestFaceViewEliminateOffScreenGhostHitboxes() {
        // In Exam View, top corners must NOT hit off-screen clamped breasts, and far outer edges must NOT stretch vulva
        assertEquals("Exam View top-left edge must not hit off-screen breast", TouchTargetZone.NONE, resolve(Offset(0.05f, 0.05f), examCam))
        assertEquals("Exam View top-right edge must not hit off-screen breast", TouchTargetZone.NONE, resolve(Offset(0.95f, 0.05f), examCam))
        assertEquals("Exam View far left edge must not stretch vulva", TouchTargetZone.NONE, resolve(Offset(0.04f, 0.50f), examCam))
        assertEquals("Exam View far right edge must not stretch vulva", TouchTargetZone.NONE, resolve(Offset(0.96f, 0.50f), examCam))

        // Direct center and snug labial taps in Exam View MUST resolve to VULVA
        assertEquals("Exam View center must hit VULVA", TouchTargetZone.VULVA, resolve(Offset(0.50f, 0.50f), examCam))
        val leftLabium = BlossomTouchHitResolver.resolveTouchZone(Offset(0.36f, 0.52f), examCam, aspect, restDeform, showFullBody = true)
        assertEquals("Exam View left labium must hit VULVA", TouchTargetZone.VULVA, leftLabium.zone)
        assertTrue("Left labium local X must be < 0.45", leftLabium.vulvaLocalNorm!!.x < 0.45f)

        // In Chest & Face view, vulva is off-screen below—touching bottom edge or arms must NOT hit vulva!
        val chestProjector = BlossomCameraProjector(chestFaceCam, aspect)
        val rightUpperArm = chestProjector.projectUnclamped(1.62f, 3.60f, BlossomBodyBuilder.mattressSurfaceZ(3.60f) + 0.42f)!!.toOffset()
        val breastZ = BlossomBodyBuilder.mattressSurfaceZ(BlossomUpperBodyBuilder.BREAST_CENTER_Y) + 1.54f
        val rightBreast = chestProjector.projectUnclamped(BlossomUpperBodyBuilder.BREAST_CENTER_AX, BlossomUpperBodyBuilder.BREAST_CENTER_Y, breastZ)!!.toOffset()
        val faceInChestView = chestProjector.projectUnclamped(0f, BlossomUpperBodyBuilder.HEAD_CENTER_Y, BlossomUpperBodyBuilder.HEAD_CENTER_Z + 0.72f)!!.toOffset()

        assertEquals("Chest & Face bottom edge must not hit off-screen vulva", TouchTargetZone.NONE, resolve(Offset(0.50f, 0.96f), chestFaceCam))
        assertEquals("Chest & Face arm touch must be ignored", TouchTargetZone.NONE, resolve(rightUpperArm, chestFaceCam))
        assertEquals("Chest & Face right breast touch must hit RIGHT_BREAST", TouchTargetZone.RIGHT_BREAST, resolve(rightBreast, chestFaceCam))
        assertEquals("Chest & Face face touch must hit HEAD", TouchTargetZone.HEAD, resolve(faceInChestView, chestFaceCam))
    }

    @Test
    fun threeQuarterRotationAndSpreadLegsKeepHitboxesAligned() {
        val spreadDeform = VulvaDeformationState(leftLegSpread = 0.85f, rightLegSpread = 0.85f)
        val projector = BlossomCameraProjector(threeQuarterLeftCam, aspect)

        val spreadRightKneeW = BlossomSdfBodyBuilder.transformLegPoint(1.80f, -1.86f, 1.74f, +1f, 0.85f)
        val spreadRightKneeS = projector.projectUnclamped(spreadRightKneeW)!!.toOffset()
        val vulvaS = projector.projectUnclamped(0f, 0.04f, 0.12f)!!.toOffset()
        val rightArmS = projector.projectUnclamped(2.05f, 1.80f, BlossomBodyBuilder.mattressSurfaceZ(1.80f) + 0.28f)!!.toOffset()

        assertEquals(
            "3/4 rotated spread right knee must still resolve to RIGHT_LEG",
            TouchTargetZone.RIGHT_LEG,
            BlossomTouchHitResolver.resolveTouchZone(spreadRightKneeS, threeQuarterLeftCam, aspect, spreadDeform, showFullBody = true).zone
        )
        assertEquals(
            "3/4 rotated vulva center must resolve to VULVA",
            TouchTargetZone.VULVA,
            BlossomTouchHitResolver.resolveTouchZone(vulvaS, threeQuarterLeftCam, aspect, spreadDeform, showFullBody = true).zone
        )
        assertEquals(
            "3/4 rotated arm touch must still resolve to NONE",
            TouchTargetZone.NONE,
            BlossomTouchHitResolver.resolveTouchZone(rightArmS, threeQuarterLeftCam, aspect, spreadDeform, showFullBody = true).zone
        )

        // Touching her exposed near-side buttock cheek in left & right 3/4 views resolves cleanly to LEFT_GLUTE / RIGHT_GLUTE
        val leftGluteS = projector.projectUnclamped(-1.14f, -0.74f, -0.82f)!!.toOffset()
        assertEquals(
            "Left 3/4 rotated exposed left buttock touch must resolve to LEFT_GLUTE",
            TouchTargetZone.LEFT_GLUTE,
            BlossomTouchHitResolver.resolveTouchZone(leftGluteS, threeQuarterLeftCam, aspect, spreadDeform, showFullBody = true).zone
        )
        val threeQuarterRightCam = threeQuarterLeftCam.copy(yawDeg = +46f)
        val rightProjector = BlossomCameraProjector(threeQuarterRightCam, aspect)
        val rightGluteS = rightProjector.projectUnclamped(1.14f, -0.74f, -0.82f)!!.toOffset()
        assertEquals(
            "Right 3/4 rotated exposed right buttock touch must resolve to RIGHT_GLUTE",
            TouchTargetZone.RIGHT_GLUTE,
            BlossomTouchHitResolver.resolveTouchZone(rightGluteS, threeQuarterRightCam, aspect, spreadDeform, showFullBody = true).zone
        )

        // Front Exam view: touching directly to the left and right of her anus resolves cleanly to LEFT_GLUTE / RIGHT_GLUTE
        val examProjector = BlossomCameraProjector(examCam, aspect)
        val leftPerianalS = examProjector.projectUnclamped(-0.62f, -0.90f, -0.32f)!!.toOffset()
        val rightPerianalS = examProjector.projectUnclamped(0.62f, -0.90f, -0.32f)!!.toOffset()
        assertEquals(
            "Front view touch directly left of anus must resolve to LEFT_GLUTE",
            TouchTargetZone.LEFT_GLUTE,
            resolve(leftPerianalS, examCam)
        )
        assertEquals(
            "Front view touch directly right of anus must resolve to RIGHT_GLUTE",
            TouchTargetZone.RIGHT_GLUTE,
            resolve(rightPerianalS, examCam)
        )
    }

    private fun resolve(pt: Offset, cam: VulvaCamera3D): TouchTargetZone =
        BlossomTouchHitResolver.resolveTouchZone(pt, cam, aspect, restDeform, showFullBody = true).zone
}
