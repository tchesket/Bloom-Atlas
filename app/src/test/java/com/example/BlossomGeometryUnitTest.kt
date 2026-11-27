package com.example

import com.example.ui.components.BlossomArmIkRig
import com.example.ui.components.BlossomBodyBuilder
import com.example.ui.components.BlossomSdfBodyBuilder
import com.example.ui.components.BlossomUpperBodyBuilder
import com.example.ui.components.BlossomViewportSessionState
import com.example.ui.components.DynamicGlMesh
import com.example.ui.components.VULVA_ELASTIC_DEFAULT_RESISTANCE
import com.example.ui.components.VULVA_ELASTIC_MAX_RESISTANCE
import com.example.ui.components.vulvaElasticHoodMax
import com.example.ui.components.vulvaElasticMaxSpread
import com.example.ui.components.vulvaElasticPullScale
import com.example.ui.components.vulvaElasticSpringStiffness
import com.example.ui.components.vulvaElasticStep
import com.example.ui.components.VulvaAnatomySceneMesh
import com.example.ui.components.VulvaDeformationState
import com.example.ui.components.VulvaGlTextureView
import com.example.ui.components.computeBlinkIntervalRangeMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class BlossomGeometryUnitTest {

    @Test
    fun blossomMeshesHaveValidFiniteVerticesAndUnitNormals() {
        val scene = VulvaAnatomySceneMesh()
        val states = listOf(
            VulvaDeformationState(),
            VulvaDeformationState(
                leftOuter = 1f,
                rightOuter = 1f,
                leftInner = 1f,
                rightInner = 1f,
                hoodRetract = 1f,
                leftBreastX = 0.8f,
                leftBreastY = 0.6f,
                rightBreastX = -0.7f,
                rightBreastY = -0.5f,
                breastSize = 0.85f,
                leftLegSpread = 0.80f,
                rightLegSpread = 0.80f
            )
        )

        states.forEach { state ->
            scene.update(state, includeLithotomyBody = false)
            scene.coreMeshes.forEach { assertValidMesh(it) }

            scene.update(state, includeLithotomyBody = true)
            scene.blossomSkinMeshes.forEach { assertValidMesh(it) }
            scene.blossomNailMeshes.forEach { assertValidMesh(it) }
            scene.blossomEyeMeshes.forEach { assertValidMesh(it) }
            scene.blossomHairMeshes.forEach { assertValidMesh(it) }
            scene.blossomDrapeMeshes.forEach { assertValidMesh(it) }
        }
    }

    @Test
    fun unifiedFullBodyHasTrueClassicalProportionsOutwardWindingAndInteractiveBreasts() {
        val scene = VulvaAnatomySceneMesh()
        scene.update(VulvaDeformationState(), includeLithotomyBody = true)

        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val body = scene.blossomBodyMesh

        // 0. Ensure unified full-body SDF mesh fits comfortably inside GL_UNSIGNED_SHORT (65,535)
        assertTrue(
            "Unified SDF full body vertex count should be in 10000..55000, got ${body.vertexCount}",
            body.vertexCount in 10000..55000
        )

        var hipMaxX = 0f
        var shoulderMaxX = 0f
        var armMaxX = 0f
        var bodyMaxY = Float.NEGATIVE_INFINITY
        var bodyMinY = Float.POSITIVE_INFINITY
        var kneeMaxZ = Float.NEGATIVE_INFINITY
        var footMinZ = Float.POSITIVE_INFINITY
        var bellyMinZ = Float.POSITIVE_INFINITY
        var bellyMaxZ = Float.NEGATIVE_INFINITY

        val bData = body.vertexData
        for (i in 0 until body.vertexCount) {
            val x = abs(bData[i * stride])
            val y = bData[i * stride + 1]
            val z = bData[i * stride + 2]

            if (y in -0.6f..0.8f && x < 1.80f && x > hipMaxX) hipMaxX = x
            if (y in 4.35f..4.75f && x > shoulderMaxX) shoulderMaxX = x
            if (y in 0.2f..5.0f && x > armMaxX) armMaxX = x
            if (y > bodyMaxY) bodyMaxY = y
            if (y < bodyMinY) bodyMinY = y
            if (y in -2.5f..-1.2f && z > kneeMaxZ) kneeMaxZ = z
            if (y < -4.2f && z < footMinZ) footMinZ = z
            if (y in 1.20f..2.50f && x < 0.40f) {
                if (z < bellyMinZ) bellyMinZ = z
                if (z > bellyMaxZ) bellyMaxZ = z
            }
        }

        // 1. Verify realistic classical 1:1 head-to-toe proportions (graceful feminine shoulders, ZERO pauldrons!)
        assertTrue("Hip half-width should be realistic 1:1 scale (> 1.55), got $hipMaxX", hipMaxX > 1.55f)
        assertTrue("Feminine shoulder half-width should stay graceful & non-pauldron (1.48..1.74), got $shoulderMaxX", shoulderMaxX in 1.48f..1.74f)
        assertTrue("Arms should extend naturally outside torso (armMaxX > 2.05), got $armMaxX", armMaxX > 2.05f)
        assertTrue("Unified SDF body should extend seamlessly up through natural-length neck (maxY > 5.75), got $bodyMaxY", bodyMaxY > 5.75f)
        assertTrue("Feet should extend naturally to full leg length (minY < -5.20), got $bodyMinY", bodyMinY < -5.20f)
        assertTrue("Reclining abdomen should have full rounded non-flattened depth (> 1.35), got ${bellyMaxZ - bellyMinZ}", (bellyMaxZ - bellyMinZ) > 1.35f)
        assertTrue("Bent knees should rise clearly upward (+Z > 1.95), got $kneeMaxZ", kneeMaxZ > 1.95f)
        assertTrue("Foot soles should rest flush on exam mattress (minZ in -1.20..-1.06), got $footMinZ", footMinZ in -1.20f..-1.06f)

        // 1B. Verify Blossom's 10 individually sculpted 3D toes
        val toes = scene.blossomToesMesh
        val tData = toes.vertexData
        var toesMinY = Float.POSITIVE_INFINITY
        var toesMinZ = Float.POSITIVE_INFINITY
        var toesMaxAbsX = 0f
        for (i in 0 until toes.vertexCount) {
            val ax = abs(tData[i * stride])
            val y = tData[i * stride + 1]
            val z = tData[i * stride + 2]
            if (y < toesMinY) toesMinY = y
            if (z < toesMinZ) toesMinZ = z
            if (ax > toesMaxAbsX) toesMaxAbsX = ax
        }
        assertTrue("Toes mesh should contain 10 sculpted toes (2240 verts), got ${toes.vertexCount}", toes.vertexCount == 2240)
        assertTrue("Hallux tips should extend naturally past metatarsal ball (minY < -5.50), got $toesMinY", toesMinY < -5.50f)
        assertTrue("Toe pads should rest gently on exam mattress (minZ in -1.16..-1.08), got $toesMinZ", toesMinZ in -1.16f..-1.08f)
        assertTrue("Lateral pinky toes should fan naturally outside ankle width (maxAbsX > 2.15), got $toesMaxAbsX", toesMaxAbsX > 2.15f)

        // 1C. Verify Blossom's 10 proportionate relaxed 3D fingers & thumbs
        val hands = scene.blossomHandsMesh
        val hData = hands.vertexData
        var handsMinY = Float.POSITIVE_INFINITY
        var handsMaxAbsX = 0f
        for (i in 0 until hands.vertexCount) {
            val ax = abs(hData[i * stride])
            val y = hData[i * stride + 1]
            if (y < handsMinY) handsMinY = y
            if (ax > handsMaxAbsX) handsMaxAbsX = ax
        }
        assertTrue("Hands mesh should contain 10 sculpted articulated fingers (2880 verts), got ${hands.vertexCount}", hands.vertexCount == 2880)
        assertTrue("Middle fingertips should reach naturally beside upper-to-mid thighs (handsMinY < -0.90), got $handsMinY", handsMinY < -0.90f)
        assertTrue("Relaxed hands should rest cleanly beside hips without clipping (handsMaxAbsX > 2.40), got $handsMaxAbsX", handsMaxAbsX > 2.40f)

        // Verify clean air-gap between hip and wrist/palm (ZERO hip-merge!), smooth wrist-to-palm arch, & realistic arm fullness
        val hipHandGapSdf = BlossomSdfBodyBuilder.evaluateBodySdf(1.88f, 0.45f, -0.95f)
        assertTrue("Air gap between hip and palm must remain open outside body (SDF > 0.04), got $hipHandGapSdf", hipHandGapSdf > 0.04f)
        val wristTopZ = BlossomSdfBodyBuilder.sampleBodySurfacePoint(2.34f, 0.42f).z
        val palmTopZ = BlossomSdfBodyBuilder.sampleBodySurfacePoint(2.42f, -0.02f).z
        assertTrue(
            "Wrist-to-palm dorsal transition must be smooth with no wrist bump or pancake drop (|wristTopZ=$wristTopZ - palmTopZ=$palmTopZ| < 0.06)",
            abs(wristTopZ - palmTopZ) < 0.06f
        )
        // Measure upper-arm fullness (y ~ 3.55), proximal forearm (y ~ 1.90), mid-forearm (y ~ 1.35), wrist (y ~ 0.42), & palm width
        var midUpperArmMinX = Float.POSITIVE_INFINITY
        var midUpperArmMaxX = Float.NEGATIVE_INFINITY
        var proxForearmMinX = Float.POSITIVE_INFINITY
        var proxForearmMaxX = Float.NEGATIVE_INFINITY
        var midForearmMinX = Float.POSITIVE_INFINITY
        var midForearmMaxX = Float.NEGATIVE_INFINITY
        var wristMinX = Float.POSITIVE_INFINITY
        var wristMaxX = Float.NEGATIVE_INFINITY
        for (i in 0 until body.vertexCount) {
            val ax = abs(bData[i * stride])
            val y = bData[i * stride + 1]
            if (y in 3.45f..3.65f && ax > 1.32f) {
                if (ax < midUpperArmMinX) midUpperArmMinX = ax
                if (ax > midUpperArmMaxX) midUpperArmMaxX = ax
            }
            if (y in 1.80f..1.98f && ax > 1.65f) {
                if (ax < proxForearmMinX) proxForearmMinX = ax
                if (ax > proxForearmMaxX) proxForearmMaxX = ax
            }
            if (y in 1.25f..1.45f && ax > 1.65f) {
                if (ax < midForearmMinX) midForearmMinX = ax
                if (ax > midForearmMaxX) midForearmMaxX = ax
            }
            if (y in 0.36f..0.48f && ax > 1.95f) {
                if (ax < wristMinX) wristMinX = ax
                if (ax > wristMaxX) wristMaxX = ax
            }
        }
        val midUpperArmWidth = midUpperArmMaxX - midUpperArmMinX
        val proxForearmWidth = proxForearmMaxX - proxForearmMinX
        val midForearmWidth = midForearmMaxX - midForearmMinX
        val wristWidth = wristMaxX - wristMinX
        var palmMinX = Float.POSITIVE_INFINITY
        var palmMaxX = Float.NEGATIVE_INFINITY
        val palmData = scene.blossomPalmsMesh.vertexData
        for (i in 0 until scene.blossomPalmsMesh.vertexCount) {
            val ax = abs(palmData[i * stride])
            val y = palmData[i * stride + 1]
            if (y in -0.28f..0.02f) {
                if (ax < palmMinX) palmMinX = ax
                if (ax > palmMaxX) palmMaxX = ax
            }
        }
        val palmWidth = palmMaxX - palmMinX
        assertTrue(
            "Proximal forearm ($proxForearmWidth) must be strictly slimmer than mid upper arm ($midUpperArmWidth) with zero Popeye bulge",
            proxForearmWidth < midUpperArmWidth - 0.04f
        )
        assertTrue(
            "Mid-forearm must have realistic non-spindly width (> 0.56), got $midForearmWidth ($midForearmMinX..$midForearmMaxX)",
            midForearmWidth > 0.56f
        )
        assertTrue(
            "Wrist must stay naturally proportioned and non-pinched (width > 0.42), got $wristWidth ($wristMinX..$wristMaxX)",
            wristWidth > 0.42f
        )
        assertTrue(
            "Palm width ($palmWidth) must be anatomically fuller than wrist ($wristWidth) and >= 0.52",
            palmWidth >= 0.52f && palmWidth > wristWidth * 1.12f
        )
        assertTrue(
            "Upper arm bicep contour must have healthy feminine fullness (outer ax > 1.94), got $midUpperArmMaxX",
            midUpperArmMaxX > 1.94f
        )

        // Verify 20 sculpted 3D fingernails (10) & toenails (10)
        val nails = scene.blossomNailsMesh
        assertTrue("Nails mesh should contain 20 sculpted nails (1600 verts), got ${nails.vertexCount}", nails.vertexCount == 1600)

        // 1D. Verify sculpted Head, Blue Eyes, and Longer Layered Bob span classical proportions, crisp chin & head ABOVE collarbone!
        var headMaxY = Float.NEGATIVE_INFINITY
        var headMinY = Float.POSITIVE_INFINITY
        var headMaxAbsX = 0f
        var faceFrontMaxZ = Float.NEGATIVE_INFINITY
        var chinFrontMaxZ = Float.NEGATIVE_INFINITY
        val headData = scene.blossomHeadMesh.vertexData
        for (i in 0 until scene.blossomHeadMesh.vertexCount) {
            val ax = abs(headData[i * stride])
            val y = headData[i * stride + 1]
            val z = headData[i * stride + 2]
            if (y > headMaxY) headMaxY = y
            if (y < headMinY) headMinY = y
            if (y > 5.80f && ax > headMaxAbsX) headMaxAbsX = ax
            if (ax < 0.35f && y > 5.80f && z > faceFrontMaxZ) faceFrontMaxZ = z
            if (ax < 0.24f && y in 5.28f..5.65f && z > chinFrontMaxZ) chinFrontMaxZ = z
        }
        var collarboneFrontMaxZ = Float.NEGATIVE_INFINITY
        var throatFrontMaxZ = Float.NEGATIVE_INFINITY
        for (i in 0 until body.vertexCount) {
            val ax = abs(bData[i * stride])
            val y = bData[i * stride + 1]
            val z = bData[i * stride + 2]
            if (ax < 0.85f && y in 4.55f..4.95f && z > collarboneFrontMaxZ) {
                collarboneFrontMaxZ = z
            }
            if (ax < 0.28f && y in 4.95f..5.38f && z > throatFrontMaxZ) {
                throatFrontMaxZ = z
            }
        }
        var hairMinY = Float.POSITIVE_INFINITY
        val hairData = scene.blossomHairMesh.vertexData
        for (i in 0 until scene.blossomHairMesh.vertexCount) {
            val y = hairData[i * stride + 1]
            if (y < hairMinY) hairMinY = y
        }
        val headWidth = headMaxAbsX * 2f
        assertTrue("Head width should match classical feminine proportion (1.45..1.80), got $headWidth", headWidth in 1.45f..1.80f)
        assertTrue("Head crown should reach classical adult proportion (headMaxY in 7.30..7.75), got $headMaxY", headMaxY in 7.30f..7.75f)
        assertTrue("Head submental socket should overlap cleanly onto slender SDF neck (headMinY < 5.65), got $headMinY", headMinY < 5.65f)
        assertTrue(
            "Pillow-elevated face front must sit cleanly ABOVE collarbone/neck (faceFrontMaxZ=$faceFrontMaxZ > collarboneFrontMaxZ=$collarboneFrontMaxZ + 0.45)",
            faceFrontMaxZ > collarboneFrontMaxZ + 0.45f
        )
        assertTrue(
            "Chin must project crisply ahead of anterior throat with zero neck-melt (chinFrontMaxZ=$chinFrontMaxZ > throatFrontMaxZ=$throatFrontMaxZ + 0.25)",
            chinFrontMaxZ > throatFrontMaxZ + 0.25f
        )
        assertTrue("Eyes mesh should have crisp resolution (> 1000 verts)", scene.blossomEyesMesh.vertexCount > 1000)
        assertTrue("Longer layered bob hair mesh should have cap + 34 locks (> 5000 verts)", scene.blossomHairMesh.vertexCount > 5000)
        assertTrue("Longer bob hair should drape past chin toward collarbones (hairMinY < 5.15), got $hairMinY", hairMinY < 5.15f)

        // 1E. Verify STRICTLY OUTWARD CCW WINDING (prevents inverted-normal dark shading!) across ALL sculpted meshes
        assertOutwardWindingMatchesNormals("blossomBreastsMesh", scene.blossomBreastsMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh", scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomEyesMesh", scene.blossomEyesMesh)
        assertOutwardWindingMatchesNormals("blossomPalmsMesh", scene.blossomPalmsMesh)
        assertOutwardWindingMatchesNormals("blossomHandsMesh", scene.blossomHandsMesh)
        assertOutwardWindingMatchesNormals("blossomToesMesh", scene.blossomToesMesh)
        assertOutwardWindingMatchesNormals("blossomNailsMesh", scene.blossomNailsMesh)
        assertOutwardWindingMatchesNormals("blossomHairMesh", scene.blossomHairMesh)

        // 1F. Verify interactive breasts stay locked at outer ribcage collar, deform when dragged & scale cleanly with breastSize
        val bCols = BlossomUpperBodyBuilder.BREAST_COLS
        val bRows = BlossomUpperBodyBuilder.BREAST_ROWS
        val leftCollarIdx = ((bRows / 2) * bCols + 0) * stride
        val leftApexIdx = ((bRows / 2) * bCols + (bCols / 2)) * stride
        val restCollarX = scene.blossomBreastsMesh.vertexData[leftCollarIdx]
        val restApexX = scene.blossomBreastsMesh.vertexData[leftApexIdx]
        val restApexZ = scene.blossomBreastsMesh.vertexData[leftApexIdx + 2]
        scene.update(VulvaDeformationState(leftBreastX = 1.0f, leftBreastY = 0.8f), includeLithotomyBody = true)
        val shiftedCollarX = scene.blossomBreastsMesh.vertexData[leftCollarIdx]
        val shiftedApexX = scene.blossomBreastsMesh.vertexData[leftApexIdx]
        assertTrue(
            "Breast outer collar must stay locked flush to ribcage (< 0.001), got ${abs(shiftedCollarX - restCollarX)}",
            abs(shiftedCollarX - restCollarX) < 0.001f
        )
        assertTrue(
            "Left breast apex should deform smoothly when dragged (deltaX > 0.14), got ${shiftedApexX - restApexX}",
            (shiftedApexX - restApexX) > 0.14f
        )
        scene.update(VulvaDeformationState(breastSize = 1.0f), includeLithotomyBody = true)
        val fullApexZ = scene.blossomBreastsMesh.vertexData[leftApexIdx + 2]
        // Verify supine gravity redistributes full breasts laterally toward axillae without crossing x=0, while preserving volume!
        val bVertsPerSide = bCols * bRows
        var fullLeftMaxX = Float.NEGATIVE_INFINITY
        var fullLeftApexWorldX = 0f
        var fullLeftMaxZ = Float.NEGATIVE_INFINITY
        for (i in 0 until bVertsPerSide) {
            val px = scene.blossomBreastsMesh.vertexData[i * stride]
            val pz = scene.blossomBreastsMesh.vertexData[i * stride + 2]
            if (px > fullLeftMaxX) fullLeftMaxX = px
            if (pz > fullLeftMaxZ) {
                fullLeftMaxZ = pz
                fullLeftApexWorldX = px
            }
        }
        assertTrue(
            "Left full breast must stay strictly on left chest without crossing x=0 (fullLeftMaxX <= -0.003), got $fullLeftMaxX",
            fullLeftMaxX <= -0.003f
        )
        scene.update(VulvaDeformationState(breastSize = 0.0f), includeLithotomyBody = true)
        val petiteApexZ = scene.blossomBreastsMesh.vertexData[leftApexIdx + 2]
        var petiteLeftApexWorldX = 0f
        var petiteLeftMaxZ = Float.NEGATIVE_INFINITY
        for (i in 0 until bVertsPerSide) {
            val px = scene.blossomBreastsMesh.vertexData[i * stride]
            val pz = scene.blossomBreastsMesh.vertexData[i * stride + 2]
            if (pz > petiteLeftMaxZ) {
                petiteLeftMaxZ = pz
                petiteLeftApexWorldX = px
            }
        }
        assertTrue(
            "Larger supine breasts must redistribute more laterally toward axillae (|fullApexX|=${abs(fullLeftApexWorldX)} > |petiteApexX|=${abs(petiteLeftApexWorldX)})",
            abs(fullLeftApexWorldX) > abs(petiteLeftApexWorldX) + 0.04f
        )
        assertTrue(
            "Breast size slider must visibly scale volume while staying proportionate & non-spherical (fullApexZ=$fullApexZ > restApexZ=$restApexZ > petiteApexZ=$petiteApexZ)",
            fullApexZ > restApexZ + 0.045f && restApexZ > petiteApexZ + 0.055f
        )

        // Verify supine torso AP depth: bony ribcage height above mattress must exceed settled waist height!
        val waistSurfaceZ = BlossomSdfBodyBuilder.sampleBodySurfacePoint(0f, 2.48f).z - BlossomBodyBuilder.mattressSurfaceZ(2.48f)
        val ribSurfaceZ = BlossomSdfBodyBuilder.sampleBodySurfacePoint(0f, 3.68f).z - BlossomBodyBuilder.mattressSurfaceZ(3.68f)
        assertTrue(
            "Reclining ribcage depth ($ribSurfaceZ) must exceed softly settled waist depth ($waistSurfaceZ)",
            ribSurfaceZ > waistSurfaceZ + 0.06f
        )

        // Verify orthognathic chin projection (no recessed chin relative to lips!)
        var lipFrontZ = Float.NEGATIVE_INFINITY
        for (i in 0 until scene.blossomHeadMesh.vertexCount) {
            val ax = abs(headData[i * stride])
            val y = headData[i * stride + 1]
            val z = headData[i * stride + 2]
            if (ax < 0.14f && y in 5.82f..6.04f && z > lipFrontZ) lipFrontZ = z
        }
        assertTrue(
            "Orthognathic chin ($chinFrontMaxZ) must project cleanly near lower lip ($lipFrontZ, gap < 0.14), got gap=${lipFrontZ - chinFrontMaxZ}",
            (lipFrontZ - chinFrontMaxZ) < 0.14f
        )

        // 1G. Verify interactive side-to-side leg spread moves knees & toes outward while keeping vulva patch perimeter locked
        var restKneeMaxX = 0f
        for (i in 0 until body.vertexCount) {
            if (bData[i * stride + 1] in -2.2f..-1.5f) {
                val ax = abs(bData[i * stride])
                if (ax > restKneeMaxX) restKneeMaxX = ax
            }
        }
        scene.update(VulvaDeformationState(leftLegSpread = 1.0f, rightLegSpread = 1.0f), includeLithotomyBody = true)
        var spreadKneeMaxX = 0f
        for (i in 0 until body.vertexCount) {
            if (bData[i * stride + 1] in -2.2f..-1.4f) {
                val ax = abs(bData[i * stride])
                if (ax > spreadKneeMaxX) spreadKneeMaxX = ax
            }
        }
        assertTrue(
            "Spreading legs must move knees cleanly wider apart (spreadKneeMaxX=$spreadKneeMaxX > restKneeMaxX=$restKneeMaxX + 0.35)",
            spreadKneeMaxX > restKneeMaxX + 0.35f
        )

        // 1H. Verify interactive side-to-side headTurn rotates face & eyes smoothly and restores exact rest identity
        val restHeadCopy = scene.blossomHeadMesh.vertexData.copyOf()
        val restEyesCopy = scene.blossomEyesMesh.vertexData.copyOf()
        val restHairCopy = scene.blossomHairMesh.vertexData.copyOf()
        scene.update(VulvaDeformationState(headTurn = 1.0f), includeLithotomyBody = true)
        var turnedNoseX = 0f
        var maxNoseZ = Float.NEGATIVE_INFINITY
        for (i in 0 until scene.blossomHeadMesh.vertexCount) {
            val z = scene.blossomHeadMesh.vertexData[i * stride + 2]
            if (z > maxNoseZ) {
                maxNoseZ = z
                turnedNoseX = scene.blossomHeadMesh.vertexData[i * stride]
            }
        }
        assertTrue("Turning head right (headTurn=+1) must shift anterior face toward +X (> 0.20), got $turnedNoseX", turnedNoseX > 0.20f)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(turned)", scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomEyesMesh(turned)", scene.blossomEyesMesh)
        assertOutwardWindingMatchesNormals("blossomHairMesh(turned)", scene.blossomHairMesh)

        scene.update(VulvaDeformationState(headTurn = 0f), includeLithotomyBody = true)
        var maxHeadDiff = 0f
        for (i in restHeadCopy.indices) {
            val d = abs(scene.blossomHeadMesh.vertexData[i] - restHeadCopy[i])
            if (d > maxHeadDiff) maxHeadDiff = d
        }
        var maxEyesDiff = 0f
        for (i in restEyesCopy.indices) {
            val d = abs(scene.blossomEyesMesh.vertexData[i] - restEyesCopy[i])
            if (d > maxEyesDiff) maxEyesDiff = d
        }
        var maxHairDiff = 0f
        for (i in restHairCopy.indices) {
            val d = abs(scene.blossomHairMesh.vertexData[i] - restHairCopy[i])
            if (d > maxHairDiff) maxHairDiff = d
        }
        assertTrue("Head rest identity must hold (< 1e-4), got $maxHeadDiff", maxHeadDiff < 1e-4f)
        assertTrue("Eyes rest identity must hold (< 1e-4), got $maxEyesDiff", maxEyesDiff < 1e-4f)
        assertTrue("Hair rest identity must hold (< 1e-4), got $maxHairDiff", maxHairDiff < 1e-4f)

        // 2. Verify Ultra-HD Vulva Patch outer perimeter sits flush (< 0.025) on the exact unified SDF=0 body surface
        val patch = scene.blossomVulvaPatchMesh
        val pData = patch.vertexData
        var maxPerimeterSdfError = 0f
        for (i in 0 until patch.vertexCount) {
            val x = pData[i * stride]
            val y = pData[i * stride + 1]
            val z = pData[i * stride + 2]
            val ovalR = sqrt(
                (x / BlossomSdfBodyBuilder.PATCH_RX).let { it * it } +
                    ((y - BlossomSdfBodyBuilder.PATCH_CENTER_Y) / BlossomSdfBodyBuilder.PATCH_RY).let { it * it }
            )
            if (ovalR >= 0.94f) {
                val err = abs(
                    BlossomSdfBodyBuilder.evaluateBodySdf(
                        x, y, z,
                        betaShortPerineum = true,
                        betaSoftButtockTable = true
                    )
                )
                if (err > maxPerimeterSdfError) maxPerimeterSdfError = err
            }
        }
        assertTrue(
            "Vulva patch outer perimeter must sit flush on unified SDF body (< 0.025), got $maxPerimeterSdfError",
            maxPerimeterSdfError < 0.025f
        )

        var vulvaMaxZ = Float.NEGATIVE_INFINITY
        for (i in 0 until patch.vertexCount) {
            val ax = abs(pData[i * stride])
            val y = pData[i * stride + 1]
            val z = pData[i * stride + 2]
            if (ax <= 0.30f && y in -0.55f..0.75f && z > vulvaMaxZ) {
                vulvaMaxZ = z
            }
        }
        assertTrue(
            "Mons pubis and outer labia should sit anatomically flush beneath/inline with abdomen profile (vulvaMaxZ <= bellyMaxZ + 0.02), got vulvaMaxZ=$vulvaMaxZ vs bellyMaxZ=$bellyMaxZ",
            vulvaMaxZ <= bellyMaxZ + 0.02f
        )

        var archMaxZ = Float.NEGATIVE_INFINITY
        val aData = scene.labiaHoodArchMesh.vertexData
        for (i in 0 until scene.labiaHoodArchMesh.vertexCount) {
            val z = aData[i * stride + 2]
            if (z > archMaxZ) archMaxZ = z
        }
        assertTrue(
            "Clitoral hood and inner labia should stay tucked within pelvic profile (archMaxZ <= bellyMaxZ + 0.025 with baked Beta v2 depth/crest), got archMaxZ=$archMaxZ vs bellyMaxZ=$bellyMaxZ",
            archMaxZ <= bellyMaxZ + 0.025f
        )

        // 2B. Verify knee bend is 100% solid & watertight with zero back-of-knee hole
        val kneeCoreSdf = BlossomSdfBodyBuilder.evaluateBodySdf(1.80f, -1.86f, 1.56f)
        val kneeBackSdf = BlossomSdfBodyBuilder.evaluateBodySdf(1.78f, -1.86f, 1.38f)
        assertTrue("Knee interior must be solid (SDF < -0.20), got $kneeCoreSdf", kneeCoreSdf < -0.20f)
        assertTrue("Posterior knee joint must remain watertight with no hole (SDF < -0.05), got $kneeBackSdf", kneeBackSdf < -0.05f)

        // 3. Verify exam mattress stays cleanly behind anterior vulva midline
        val cushion = scene.examCushionMesh
        val cData = cushion.vertexData
        for (i in 0 until cushion.vertexCount) {
            val x = cData[i * stride]
            val y = cData[i * stride + 1]
            val z = cData[i * stride + 2]
            if (x in -0.55f..0.55f && y in -0.80f..0.70f) {
                assertTrue("Cushion must stay behind vulva midline (z < -0.68), got $z at ($x, $y)", z < -0.68f)
            }
        }
    }

    @Test
    fun blossomV62RefinementInvariantsHold() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        // 1. Verify large breast (breastSize = 1.0) fades smoothly near outer collar without a steep cliff
        scene.update(VulvaDeformationState(breastSize = 1.0f), includeLithotomyBody = true)
        val breastsCache = BlossomUpperBodyBuilder.buildBreastsCache()
        val bData = scene.blossomBreastsMesh.vertexData
        var maxOuterCollarLift = 0f
        for (i in 0 until breastsCache.left.count) {
            val edgeR = breastsCache.left.edgeR[i]
            if (edgeR >= 0.86f) {
                val liftZ = bData[i * stride + 2] - breastsCache.left.baseZ[i]
                if (liftZ > maxOuterCollarLift) maxOuterCollarLift = liftZ
            }
        }
        assertTrue(
            "Large breast (size=1.0) outer collar (edgeR>=0.86) must blend smoothly into chest wall (< 0.09), got $maxOuterCollarLift",
            maxOuterCollarLift < 0.09f
        )

        // 2. Verify elbow cubital crease AO sits at the true v6.1 elbow (ax=2.26, y=2.28), not legacy (1.80, 2.70)
        val zElbowSurface = BlossomBodyBuilder.mattressSurfaceZ(2.28f) + 0.66f
        val (trueElbowAo, _) = BlossomSdfBodyBuilder.evaluateBodyMaterial(2.26f, 2.28f, zElbowSurface, 0f, 0f, 1f)
        val (legacyElbowAo, _) = BlossomSdfBodyBuilder.evaluateBodyMaterial(1.80f, 2.70f, zElbowSurface, 0f, 0f, 1f)
        assertTrue(
            "True v6.1 elbow crease AO ($trueElbowAo) must be darker than legacy off-arm spot ($legacyElbowAo)",
            trueElbowAo < 0.90f && legacyElbowAo > 0.95f
        )

        // 3. Verify eye mesh carries natural corner-canthus & lower-lid contact AO while keeping iris center bright
        val eyeData = scene.blossomEyesMesh.vertexData
        var minEyeAo = 1f
        var maxEyeAo = 0f
        for (i in 0 until scene.blossomEyesMesh.vertexCount) {
            val ao = eyeData[i * stride + 8]
            if (ao < minEyeAo) minEyeAo = ao
            if (ao > maxEyeAo) maxEyeAo = ao
        }
        assertTrue(
            "Eye sclera/canthus AO should span natural shadow range (min < 0.72, max > 0.92), got $minEyeAo..$maxEyeAo",
            minEyeAo < 0.72f && maxEyeAo > 0.92f
        )

        // 4. Verify all refined meshes stay strictly valid and outward-wound at breastSize=1.0 + spread pose
        scene.update(VulvaDeformationState(breastSize = 1.0f, selfSpreadPose = 1.0f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomBreastsMesh)
        assertValidMesh(scene.blossomPalmsMesh)
        assertValidMesh(scene.blossomHandsMesh)
        assertValidMesh(scene.blossomNailsMesh)
        assertValidMesh(scene.blossomToesMesh)
        assertOutwardWindingMatchesNormals("blossomBreastsMesh(large+spread)", scene.blossomBreastsMesh)
        assertOutwardWindingMatchesNormals("blossomPalmsMesh(spread)", scene.blossomPalmsMesh)
        assertOutwardWindingMatchesNormals("blossomHandsMesh(spread)", scene.blossomHandsMesh)
        assertOutwardWindingMatchesNormals("blossomNailsMesh(spread)", scene.blossomNailsMesh)
    }

    @Test
    fun blossomV63FixesAndInteractivityInvariantsHold() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        // 1. Breast footprint, sternal gap, collarbone clearance & torso seam/material match
        val breastsCache = BlossomUpperBodyBuilder.buildBreastsCache()
        for (sideCache in listOf(breastsCache.left, breastsCache.right)) {
            var minBaseNz = Float.POSITIVE_INFINITY
            var minAxillaNz = Float.POSITIVE_INFINITY
            var maxAbsX = 0f
            var minAbsX = Float.POSITIVE_INFINITY
            var maxBaseY = Float.NEGATIVE_INFINITY
            for (i in 0 until sideCache.count) {
                val ax = abs(sideCache.baseX[i])
                val y = sideCache.baseY[i]
                val nz = sideCache.baseNz[i]
                if (ax > maxAbsX) maxAbsX = ax
                if (ax < minAbsX) minAbsX = ax
                if (y > maxBaseY) maxBaseY = y
                if (nz < minBaseNz) minBaseNz = nz
                if (y in 3.0f..4.65f && nz < minAxillaNz) minAxillaNz = nz
            }
            assertTrue("Breast patch must stay cleanly on its own side of midline (minAbsX >= 0.06), got $minAbsX", minAbsX >= 0.06f)
            assertTrue("Breast patch outer edge must stay inside ribcage away from upper arms (maxAbsX <= 1.38), got $maxAbsX", maxAbsX <= 1.38f)
            assertTrue("Breast upper start must sit cleanly below collarbone (maxBaseY <= 4.55), got $maxBaseY", maxBaseY <= 4.55f)
            assertTrue("Breast base patch must remain forward-facing everywhere (minBaseNz > 0.25), got $minBaseNz", minBaseNz > 0.25f)
            assertTrue("Breast base patch across armpit/ribcage band must sit cleanly on anterior/antero-lateral chest wall (minAxillaNz > 0.25), got $minAxillaNz", minAxillaNz > 0.25f)
        }
        // Verify clear sternal gap & torso-matched wetness/AO even at maximum "Full" (breastSize = 1.0)
        scene.update(VulvaDeformationState(breastSize = 1.0f), includeLithotomyBody = true)
        val fullData = scene.blossomBreastsMesh.vertexData
        var minFullAbsX = Float.POSITIVE_INFINITY
        var maxCollarWetnessDiff = 0f
        var maxLowerAoDiff = 0f
        for (i in 0 until breastsCache.left.count) {
            val b = i * stride
            val ax = abs(fullData[b])
            if (ax < minFullAbsX) minFullAbsX = ax
            if (breastsCache.left.edgeR[i] >= 0.88f) {
                val wDiff = abs(fullData[b + 7] - 0.055f)
                if (wDiff > maxCollarWetnessDiff) maxCollarWetnessDiff = wDiff
            }
            if (breastsCache.left.localEy[i] < -0.58f) {
                val aoDiff = abs(fullData[b + 8] - breastsCache.left.baseAo[i])
                if (aoDiff > maxLowerAoDiff) maxLowerAoDiff = aoDiff
            }
        }
        assertTrue("Clean midline valley must persist even at Full breastSize=1.0 (minFullAbsX >= 0.05), got $minFullAbsX", minFullAbsX >= 0.05f)
        assertTrue("Breast boundary wetness must match torso skin 0.055 (< 1e-3), got $maxCollarWetnessDiff", maxCollarWetnessDiff < 1e-3f)
        assertTrue("No dark smeared under-breast AO band (< 1e-3 diff from baseAo), got $maxLowerAoDiff", maxLowerAoDiff < 1e-3f)
        scene.update(VulvaDeformationState(), includeLithotomyBody = true)

        // 2. Hollow neck collar fix: bottom rim of blossomHeadMesh (row 0) sits strictly INSIDE the solid SDF neck cylinder!
        val headCols = 120
        val headData = scene.blossomHeadMesh.vertexData
        val zNeckBase = BlossomBodyBuilder.mattressSurfaceZ(4.86f) + 0.58f
        val zNeckTop = BlossomBodyBuilder.mattressSurfaceZ(5.78f) + 0.80f
        var maxBottomRimRadialOffset = 0f
        for (c in 0 until headCols) {
            val idx = c * stride
            val wx = headData[idx]
            val wy = headData[idx + 1]
            val wz = headData[idx + 2]
            val neckT = ((wy - 4.84f) / 0.94f).coerceIn(0f, 1f)
            val neckAxisZ = zNeckBase + (zNeckTop - zNeckBase) * neckT + 0.018f * kotlin.math.sin(neckT * kotlin.math.PI.toFloat())
            val radial = sqrt(wx * wx + (wz - neckAxisZ) * (wz - neckAxisZ))
            if (radial > maxBottomRimRadialOffset) maxBottomRimRadialOffset = radial
        }
        assertTrue(
            "Head bottom neck rim must tuck strictly inside SDF neck cylinder (< 0.24 vs neck R ~0.335) with ZERO hollow collar, got $maxBottomRimRadialOffset",
            maxBottomRimRadialOffset < 0.24f
        )

        // 3. Natural blinking: eyeBlink = 1f closes upper eyelids smoothly with outward winding & restores exact rest identity at 0f
        assertTrue("HeadRigCache should capture upper eyelid blink vertices (> 80)", scene.blossomHeadRigCache.blinkVertIndices.size > 80)
        val restHeadCopy = scene.blossomHeadMesh.vertexData.copyOf()
        scene.update(VulvaDeformationState(eyeBlink = 1.0f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(blink=1)", scene.blossomHeadMesh)
        var maxLidDropY = 0f
        for (vertIdx in scene.blossomHeadRigCache.blinkVertIndices) {
            val dropY = restHeadCopy[vertIdx * stride + 1] - scene.blossomHeadMesh.vertexData[vertIdx * stride + 1]
            if (dropY > maxLidDropY) maxLidDropY = dropY
        }
        assertTrue("Full blink must smoothly lower upper eyelids over eyeballs (dropY > 0.06), got $maxLidDropY", maxLidDropY > 0.06f)

        // 4. Head pitch (v21.0): +pitch nods downward (+1f), -pitch tilts head backward (-1f), and 0f restores exact rest identity
        scene.update(VulvaDeformationState(headPitch = -0.8f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertValidMesh(scene.blossomEyesMesh)
        assertValidMesh(scene.blossomHairMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(leanBack)", scene.blossomHeadMesh)
        var leanNoseIdx = 0
        var leanRestMaxZ = Float.NEGATIVE_INFINITY
        for (i in 0 until scene.blossomHeadMesh.vertexCount) {
            val z = restHeadCopy[i * stride + 2]
            if (z > leanRestMaxZ) {
                leanRestMaxZ = z
                leanNoseIdx = i
            }
        }
        val leanedNoseY = scene.blossomHeadMesh.vertexData[leanNoseIdx * stride + 1]
        assertTrue(
            "Negative headPitch=-0.8 must smoothly lean anterior face backward (+Y): leanedNoseY=$leanedNoseY > restNoseY=${restHeadCopy[leanNoseIdx * stride + 1]} + 0.12",
            leanedNoseY > restHeadCopy[leanNoseIdx * stride + 1] + 0.12f
        )
        scene.update(VulvaDeformationState(headPitch = 0f), includeLithotomyBody = true)
        var maxZeroPitchDiff = 0f
        for (i in restHeadCopy.indices) {
            val d = abs(scene.blossomHeadMesh.vertexData[i] - restHeadCopy[i])
            if (d > maxZeroPitchDiff) maxZeroPitchDiff = d
        }
        assertTrue("Returning headPitch to 0f must restore exact rest head (< 1e-4), got $maxZeroPitchDiff", maxZeroPitchDiff < 1e-4f)

        scene.update(VulvaDeformationState(headPitch = 1.0f, headTurn = 0.5f, eyeBlink = 0.6f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertValidMesh(scene.blossomEyesMesh)
        assertValidMesh(scene.blossomHairMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(nod+turn+blink)", scene.blossomHeadMesh)
        var noseVertIdx = 0
        var restMaxZ = Float.NEGATIVE_INFINITY
        for (i in 0 until scene.blossomHeadMesh.vertexCount) {
            val z = restHeadCopy[i * stride + 2]
            if (z > restMaxZ) {
                restMaxZ = z
                noseVertIdx = i
            }
        }
        val restNoseY = restHeadCopy[noseVertIdx * stride + 1]
        val noddedNoseY = scene.blossomHeadMesh.vertexData[noseVertIdx * stride + 1]
        assertTrue(
            "Downward headPitch=1 must gently lower anterior face (noddedNoseY=$noddedNoseY < restNoseY=$restNoseY - 0.12)",
            noddedNoseY < restNoseY - 0.12f
        )

        // 5. Subtle elastic glute interactivity: gently deforms posterior glutes, stays restrained, and composes cleanly with legSpread
        assertTrue("GluteSkinCache should capture gluteal vertices (> 100)", scene.blossomGluteSkinCache.bodyVertIndices.size > 100)
        scene.update(VulvaDeformationState(leftLegSpread = 0.5f, rightLegSpread = 0.5f), includeLithotomyBody = true)
        val spreadOnlyBody = scene.blossomBodyMesh.vertexData.copyOf()
        val spreadOnlyPatch = scene.blossomVulvaPatchMesh.vertexData.copyOf()
        scene.update(
            VulvaDeformationState(
                leftLegSpread = 0.5f,
                rightLegSpread = 0.5f,
                leftGluteX = -1.0f,
                leftGluteY = 0.8f,
                rightGluteX = 1.0f,
                rightGluteY = -0.6f
            ),
            includeLithotomyBody = true
        )
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        var maxGluteDelta = 0f
        for (idx in scene.blossomGluteSkinCache.bodyVertIndices) {
            val b = idx * stride
            val dx = scene.blossomBodyMesh.vertexData[b] - spreadOnlyBody[b]
            val dy = scene.blossomBodyMesh.vertexData[b + 1] - spreadOnlyBody[b + 1]
            val dz = scene.blossomBodyMesh.vertexData[b + 2] - spreadOnlyBody[b + 2]
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (d > maxGluteDelta) maxGluteDelta = d
        }
        assertTrue(
            "Glute elasticity must be perceptible yet very slight (0.03..0.12), got $maxGluteDelta",
            maxGluteDelta in 0.03f..0.12f
        )
        var maxPerianalPatchDelta = 0f
        var maxAnusCenterDelta = 0f
        for (i in 0 until scene.blossomVulvaPatchMesh.vertexCount) {
            val b = i * stride
            val rx = spreadOnlyPatch[b]
            val ry = spreadOnlyPatch[b + 1]
            val dx = scene.blossomVulvaPatchMesh.vertexData[b] - rx
            val dy = scene.blossomVulvaPatchMesh.vertexData[b + 1] - ry
            val dz = scene.blossomVulvaPatchMesh.vertexData[b + 2] - spreadOnlyPatch[b + 2]
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (ry < -0.55f && abs(rx) > 0.25f && d > maxPerianalPatchDelta) maxPerianalPatchDelta = d
            if (abs(rx) < 0.12f && d > maxAnusCenterDelta) maxAnusCenterDelta = d
        }
        assertTrue("Perianal cheeks left/right of anus must subtly flex (> 0.015), got $maxPerianalPatchDelta", maxPerianalPatchDelta > 0.015f)
        assertTrue("Anus midline must stay anchored (< 1e-4), got $maxAnusCenterDelta", maxAnusCenterDelta < 1e-4f)

        // Releasing glutes returns body cleanly to spreadOnlyBody
        scene.update(VulvaDeformationState(leftLegSpread = 0.5f, rightLegSpread = 0.5f), includeLithotomyBody = true)
        var maxGluteResetErr = 0f
        for (i in spreadOnlyBody.indices) {
            val d = abs(scene.blossomBodyMesh.vertexData[i] - spreadOnlyBody[i])
            if (d > maxGluteResetErr) maxGluteResetErr = d
        }
        assertTrue("Releasing glute elasticity must restore exact leg-spread geometry (< 1e-4), got $maxGluteResetErr", maxGluteResetErr < 1e-4f)
    }

    @Test
    fun blossomPhysiologyBreathingBlinkAndVulvaPolishInvariantsHold() {
        // 1. Periodic breathing cadence & amplitude: calm baseline (~14 bpm) subtly quickens & deepens with touch
        val calmHz = VulvaGlTextureView.breathFrequencyHz(0f)
        val breastHz = VulvaGlTextureView.breathFrequencyHz(0.55f)
        val clitoralHz = VulvaGlTextureView.breathFrequencyHz(1.0f)
        assertTrue("Calm breathing should be near ~14 breaths/min (0.20..0.28 Hz), got $calmHz", calmHz in 0.20f..0.28f)
        assertTrue("Touch stimulus must smoothly increase breathing frequency ($calmHz < $breastHz < $clitoralHz)", calmHz < breastHz && breastHz < clitoralHz)
        assertTrue("Elevated breathing should stay gentle & natural (<= 0.42 Hz), got $clitoralHz", clitoralHz <= 0.42f)

        var maxCalmChest = 0f
        var maxCalmAbdomen = 0f
        var maxTouchChest = 0f
        var maxTouchAbdomen = 0f
        var phaseDiffSum = 0f
        for (k in 0 until 64) {
            val phase = k / 64f
            val (calmChest, calmAbdomen) = VulvaGlTextureView.evaluateBreathingDisplacement(phase, 0f)
            val (touchChest, touchAbdomen) = VulvaGlTextureView.evaluateBreathingDisplacement(phase, 1f)
            assertTrue("Breathing displacement must remain non-negative and bounded", calmChest in 0f..0.09f && calmAbdomen in 0f..0.11f)
            assertTrue("Touch breathing displacement must remain gentle and bounded", touchChest in 0f..0.12f && touchAbdomen in 0f..0.14f)
            if (calmChest > maxCalmChest) maxCalmChest = calmChest
            if (calmAbdomen > maxCalmAbdomen) maxCalmAbdomen = calmAbdomen
            if (touchChest > maxTouchChest) maxTouchChest = touchChest
            if (touchAbdomen > maxTouchAbdomen) maxTouchAbdomen = touchAbdomen
            phaseDiffSum += abs(calmChest - calmAbdomen)
        }
        assertTrue("Calm breathing must produce subtle visible chest & abdomen rise (> 0.02)", maxCalmChest > 0.02f && maxCalmAbdomen > 0.025f)
        assertTrue("Touch should subtly deepen breathing amplitude ($maxTouchChest > $maxCalmChest, $maxTouchAbdomen > $maxCalmAbdomen)", maxTouchChest > maxCalmChest && maxTouchAbdomen > maxCalmAbdomen)
        assertTrue("Abdomen and chest waveforms should carry natural phase offset", phaseDiffSum > 0.10f)

        // 2. Touch-responsive blinking cadence: calm rest -> breast/vulva -> clitoris fastest
        val calmBlink = computeBlinkIntervalRangeMs(0f)
        val breastBlink = computeBlinkIntervalRangeMs(0.55f)
        val clitoralBlink = computeBlinkIntervalRangeMs(1.0f)
        assertTrue("Resting blink interval should be relaxed (first >= 3000ms), got $calmBlink", calmBlink.first >= 3000L)
        assertTrue("Breast/vulva touch should shorten blink interval vs calm ($breastBlink vs $calmBlink)", breastBlink.first < calmBlink.first && breastBlink.last < calmBlink.last)
        assertTrue("Clitoral touch should shorten blink interval further ($clitoralBlink vs $breastBlink)", clitoralBlink.first < breastBlink.first && clitoralBlink.last < breastBlink.last)
        assertTrue("Clitoral blink interval must stay natural (>= 900ms), got $clitoralBlink", clitoralBlink.first >= 900L)

        // 3. Subtle vulva polish: organic L/R labia minora asymmetry, preserved resting peek & softened vignette perimeter
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        scene.update(VulvaDeformationState(), includeLithotomyBody = false)
        assertValidMesh(scene.pelvisMesh)
        assertValidMesh(scene.labiaHoodArchMesh)

        // Vignette outer rim must not have a harsh dark halo ring (AO stays softly feathered > 0.62)
        val pelvData = scene.pelvisMesh.vertexData
        var minPerimeterAo = 1f
        for (i in 0 until scene.pelvisMesh.vertexCount) {
            val x = pelvData[i * stride]
            val y = pelvData[i * stride + 1]
            val ao = pelvData[i * stride + 8]
            val r = sqrt((x / 1.40f).let { it * it } + ((y + 0.06f) / 1.80f).let { it * it })
            if (r >= 0.92f && ao < minPerimeterAo) minPerimeterAo = ao
        }
        assertTrue("Vignette perimeter AO should avoid dark halo ring (>= 0.60), got $minPerimeterAo", minPerimeterAo >= 0.60f)

        // Labia minora left vs right sides must exhibit subtle organic asymmetry (non-mirror-identical)
        val archData = scene.labiaHoodArchMesh.vertexData
        var leftLabiaMaxZ = Float.NEGATIVE_INFINITY
        var rightLabiaMaxZ = Float.NEGATIVE_INFINITY
        var leftLabiaMinY = Float.POSITIVE_INFINITY
        var rightLabiaMinY = Float.POSITIVE_INFINITY
        for (i in 0 until scene.labiaHoodArchMesh.vertexCount) {
            val x = archData[i * stride]
            val y = archData[i * stride + 1]
            val z = archData[i * stride + 2]
            if (y in -0.58f..0.12f) {
                if (x < -0.02f) {
                    if (z > leftLabiaMaxZ) leftLabiaMaxZ = z
                    if (y < leftLabiaMinY) leftLabiaMinY = y
                } else if (x > 0.02f) {
                    if (z > rightLabiaMaxZ) rightLabiaMaxZ = z
                    if (y < rightLabiaMinY) rightLabiaMinY = y
                }
            }
        }
        val zAsym = abs(leftLabiaMaxZ - rightLabiaMaxZ)
        val yAsym = abs(leftLabiaMinY - rightLabiaMinY)
        assertTrue(
            "Labia minora should have subtle organic L/R asymmetry (zAsym=$zAsym or yAsym=$yAsym > 0.004) without exaggeration (< 0.12)",
            (zAsym > 0.004f || yAsym > 0.004f) && zAsym < 0.12f && yAsym < 0.12f
        )

        // 4. Reverted v6.3 breasts remain valid and cleanly outward-wound
        scene.update(VulvaDeformationState(breastSize = 0.50f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomBreastsMesh)
        assertOutwardWindingMatchesNormals("blossomBreastsMesh(medium)", scene.blossomBreastsMesh)
    }

    @Test
    fun betaPelvicRefinementsAreIndependentLocalizedAndReversible() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        // Capture exact legacy baseline (legacyVulvaMode = true -> all 12 refinements OFF)
        val legacyBase = VulvaDeformationState(legacyVulvaMode = true)
        scene.update(legacyBase, includeLithotomyBody = true)
        val baseBody = scene.blossomBodyMesh.vertexData.copyOf()
        val basePatch = scene.blossomVulvaPatchMesh.vertexData.copyOf()
        val baseArch = scene.labiaHoodArchMesh.vertexData.copyOf()
        val baseGlans = scene.glansMesh.vertexData.copyOf()

        fun findAnusMinY(patchData: FloatArray): Float {
            var minAo = Float.POSITIVE_INFINITY
            var anusY = 0f
            val count = patchData.size / stride
            for (i in 0 until count) {
                val x = patchData[i * stride]
                val y = patchData[i * stride + 1]
                val ao = patchData[i * stride + 8]
                if (abs(x) < 0.04f && y in -0.96f..-0.68f && ao < minAo) {
                    minAo = ao
                    anusY = y
                }
            }
            return anusY
        }

        val baselineAnusY = findAnusMinY(basePatch)
        assertTrue("Baseline anus should be centered near -0.86, got $baselineAnusY", baselineAnusY in -0.89f..-0.83f)

        // 1. Beta Toggle 1 ONLY: Shorter perineum (~20%) moves anus upward without altering vulva/legs/upper body
        scene.update(legacyBase.copy(betaShortPerineum = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        val beta1AnusY = findAnusMinY(scene.blossomVulvaPatchMesh.vertexData)
        val shiftY = beta1AnusY - baselineAnusY
        assertTrue(
            "Beta 1 should move anus upward by ~0.078 (~20% of 0.40 fourchette-anus span), got shift=$shiftY (base=$baselineAnusY, beta1=$beta1AnusY)",
            shiftY in 0.062f..0.095f
        )
        // Vulva glans & labia arch must stay 100% bitwise untouched!
        for (i in baseGlans.indices) {
            assertTrue("Beta 1 must not alter clitoral glans", abs(scene.glansMesh.vertexData[i] - baseGlans[i]) < 1e-6f)
        }
        for (i in baseArch.indices) {
            assertTrue("Beta 1 must not alter labia/hood arch", abs(scene.labiaHoodArchMesh.vertexData[i] - baseArch[i]) < 1e-6f)
        }

        // 2. Beta Toggle 2 ONLY: Soft buttock-table contact & subtle infragluteal fold (localized strictly to buttocks)
        scene.update(legacyBase.copy(betaSoftButtockTable = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        var buttockMaxDelta = 0f
        var nonPelvisMaxDelta = 0f
        val beta2Body = scene.blossomBodyMesh.vertexData
        for (v in 0 until scene.blossomBodyMesh.vertexCount) {
            val base = v * stride
            val y = baseBody[base + 1]
            val dx = beta2Body[base] - baseBody[base]
            val dy = beta2Body[base + 1] - baseBody[base + 1]
            val dz = beta2Body[base + 2] - baseBody[base + 2]
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (y in -1.35f..0.15f && abs(baseBody[base]) < 1.65f) {
                if (d > buttockMaxDelta) buttockMaxDelta = d
            } else if (y > 0.65f || y < -1.75f) {
                if (d > nonPelvisMaxDelta) nonPelvisMaxDelta = d
            }
        }
        assertTrue("Beta 2 should visibly soften buttock-table contact / add subtle fold (maxDelta > 0.008), got $buttockMaxDelta", buttockMaxDelta > 0.008f)
        assertTrue("Beta 2 must not alter legs or upper body above pelvis (nonPelvisMaxDelta < 1e-6), got $nonPelvisMaxDelta", nonPelvisMaxDelta < 1e-6f)

        // 3. Beta Toggle 3 ONLY: Smooth vulva patch seam keeps valid normals & outward CCW winding
        scene.update(legacyBase.copy(betaSmoothVulvaSeam = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        assertOutwardWindingMatchesNormals("blossomVulvaPatchMesh(betaSmoothSeam)", scene.blossomVulvaPatchMesh)

        // 4. All 3 ON together -> valid meshes, then Legacy mode -> 100% exact baseline backup restoration!
        scene.update(
            legacyBase.copy(
                betaShortPerineum = true,
                betaSoftButtockTable = true,
                betaSmoothVulvaSeam = true
            ),
            includeLithotomyBody = true
        )
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        assertOutwardWindingMatchesNormals("blossomVulvaPatchMesh(all3Beta)", scene.blossomVulvaPatchMesh)

        scene.update(legacyBase, includeLithotomyBody = true)
        var maxRestoreBodyDiff = 0f
        for (i in baseBody.indices) {
            val diff = abs(scene.blossomBodyMesh.vertexData[i] - baseBody[i])
            if (diff > maxRestoreBodyDiff) maxRestoreBodyDiff = diff
        }
        var maxRestorePatchDiff = 0f
        for (i in basePatch.indices) {
            val diff = abs(scene.blossomVulvaPatchMesh.vertexData[i] - basePatch[i])
            if (diff > maxRestorePatchDiff) maxRestorePatchDiff = diff
        }
        assertTrue("Legacy mode must restore exact baseline body mesh (< 1e-5), got $maxRestoreBodyDiff", maxRestoreBodyDiff < 1e-5f)
        assertTrue("Legacy mode must restore exact baseline vulva patch (< 1e-5), got $maxRestorePatchDiff", maxRestorePatchDiff < 1e-5f)
    }

    @Test
    fun betaVulvaSeamAGeometryAndSeamBColorAreIndependentFlushAndExact() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val cols = BlossomSdfBodyBuilder.VULVA_PATCH_COLS
        val rows = BlossomSdfBodyBuilder.VULVA_PATCH_ROWS

        val legacyBase = VulvaDeformationState(legacyVulvaMode = true)
        scene.update(legacyBase, includeLithotomyBody = true)
        val baseBody = scene.blossomBodyMesh.vertexData.copyOf()
        val basePatch = scene.blossomVulvaPatchMesh.vertexData.copyOf()

        // Helper: brute-force exact closest point & interpolated normal/AO/pigment on rendered anterior body triangles
        fun sampleNearestRenderedBodyTri(
            bodyMesh: DynamicGlMesh,
            px: Float,
            py: Float,
            pz: Float
        ): FloatArray {
            val bData = bodyMesh.vertexData
            val idxBuf = bodyMesh.indexBuffer
            val triCount = bodyMesh.indexCount / 3
            var bestDistSq = Float.POSITIVE_INFINITY
            var bestNx = 0f; var bestNy = 0f; var bestNz = 1f
            var bestAo = 1f; var bestPig = 0f
            idxBuf.position(0)
            for (t in 0 until triCount) {
                val a = idxBuf.get().toInt() and 0xFFFF
                val b = idxBuf.get().toInt() and 0xFFFF
                val c = idxBuf.get().toInt() and 0xFFFF
                val ao = a * stride; val bo = b * stride; val co = c * stride
                if (bData[ao + 5] + bData[bo + 5] + bData[co + 5] < 0.02f) continue
                val ax = bData[ao]; val ay = bData[ao + 1]; val az = bData[ao + 2]
                val bx = bData[bo]; val by = bData[bo + 1]; val bz = bData[bo + 2]
                val cx = bData[co]; val cy = bData[co + 1]; val cz = bData[co + 2]
                // Fast bounding-sphere reject around triangle vertex A
                val dxA = px - ax; val dyA = py - ay; val dzA = pz - az
                if (dxA * dxA + dyA * dyA + dzA * dzA > 0.12f) continue

                // Closest barycentric point on triangle (A,B,C)
                val abx = bx - ax; val aby = by - ay; val abz = bz - az
                val acx = cx - ax; val acy = cy - ay; val acz = cz - az
                val apx = px - ax; val apy = py - ay; val apz = pz - az
                val d1 = abx * apx + aby * apy + abz * apz
                val d2 = acx * apx + acy * apy + acz * apz
                var w0: Float; var w1: Float; var w2: Float
                if (d1 <= 0f && d2 <= 0f) {
                    w0 = 1f; w1 = 0f; w2 = 0f
                } else {
                    val bpx = px - bx; val bpy = py - by; val bpz = pz - bz
                    val d3 = abx * bpx + aby * bpy + abz * bpz
                    val d4 = acx * bpx + acy * bpy + acz * bpz
                    if (d3 >= 0f && d4 <= d3) {
                        w0 = 0f; w1 = 1f; w2 = 0f
                    } else {
                        val vc = d1 * d4 - d3 * d2
                        if (vc <= 0f && d1 >= 0f && d3 <= 0f) {
                            val v = d1 / (d1 - d3).coerceAtLeast(1e-8f)
                            w0 = 1f - v; w1 = v; w2 = 0f
                        } else {
                            val cpx = px - cx; val cpy = py - cy; val cpz = pz - cz
                            val d5 = abx * cpx + aby * cpy + abz * cpz
                            val d6 = acx * cpx + acy * cpy + acz * cpz
                            if (d6 >= 0f && d5 <= d6) {
                                w0 = 0f; w1 = 0f; w2 = 1f
                            } else {
                                val vb = d5 * d2 - d1 * d6
                                if (vb <= 0f && d2 >= 0f && d6 <= 0f) {
                                    val w = d2 / (d2 - d6).coerceAtLeast(1e-8f)
                                    w0 = 1f - w; w1 = 0f; w2 = w
                                } else {
                                    val va = d3 * d6 - d5 * d4
                                    if (va <= 0f && (d4 - d3) >= 0f && (d5 - d6) >= 0f) {
                                        val w = (d4 - d3) / ((d4 - d3) + (d5 - d6)).coerceAtLeast(1e-8f)
                                        w0 = 0f; w1 = 1f - w; w2 = w
                                    } else {
                                        val denom = 1f / (va + vb + vc).coerceAtLeast(1e-8f)
                                        w1 = vb * denom; w2 = vc * denom; w0 = 1f - w1 - w2
                                    }
                                }
                            }
                        }
                    }
                }
                val qx = w0 * ax + w1 * bx + w2 * cx - px
                val qy = w0 * ay + w1 * by + w2 * cy - py
                val qz = w0 * az + w1 * bz + w2 * cz - pz
                val dSq = qx * qx + qy * qy + qz * qz
                if (dSq < bestDistSq) {
                    bestDistSq = dSq
                    var nx = w0 * bData[ao + 3] + w1 * bData[bo + 3] + w2 * bData[co + 3]
                    var ny = w0 * bData[ao + 4] + w1 * bData[bo + 4] + w2 * bData[co + 4]
                    var nz = w0 * bData[ao + 5] + w1 * bData[bo + 5] + w2 * bData[co + 5]
                    val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
                    bestNx = nx / nLen; bestNy = ny / nLen; bestNz = nz / nLen
                    bestAo = w0 * bData[ao + 8] + w1 * bData[bo + 8] + w2 * bData[co + 8]
                    bestPig = w0 * bData[ao + 9] + w1 * bData[bo + 9] + w2 * bData[co + 9]
                }
            }
            idxBuf.position(0)
            return floatArrayOf(sqrt(bestDistSq), bestNx, bestNy, bestNz, bestAo, bestPig)
        }

        // 1. SEAM A ONLY (Geometry): Outermost ring sits strictly flush with rendered body triangles & identical normals
        scene.update(legacyBase.copy(betaSmoothVulvaSeam = true, betaBlendVulvaSeamColor = false), includeLithotomyBody = true)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        assertOutwardWindingMatchesNormals("blossomVulvaPatchMesh(seamA)", scene.blossomVulvaPatchMesh)

        val seamAPatch = scene.blossomVulvaPatchMesh.vertexData
        var maxRimDist = 0f
        var minRimNormalDot = 1f
        var maxRightRimDist = 0f
        var maxInteriorDiffA = 0f
        var maxMatDiffA = 0f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val i = r * cols + c
                val b = i * stride
                val bx = basePatch[b]
                val by = basePatch[b + 1]
                val ovalR = sqrt((bx / BlossomSdfBodyBuilder.PATCH_RX).let { it * it } +
                    ((by - BlossomSdfBodyBuilder.PATCH_CENTER_Y) / BlossomSdfBodyBuilder.PATCH_RY).let { it * it })
                // Seam A must NOT alter interior (labia, vestibule, clitoral hood)
                if (ovalR <= 0.54f) {
                    for (k in 0 until stride) {
                        val d = abs(seamAPatch[b + k] - basePatch[b + k])
                        if (d > maxInteriorDiffA) maxInteriorDiffA = d
                    }
                }
                // Seam A must NOT alter material channels (mucosa, wetness, ao, pigment belong to Seam B)
                for (k in 6..9) {
                    val d = abs(seamAPatch[b + k] - basePatch[b + k])
                    if (d > maxMatDiffA) maxMatDiffA = d
                }
                val isRim = r == 0 || r == rows - 1 || c == 0 || c == cols - 1
                if (isRim && ((r + c) % 4 == 0)) {
                    val px = seamAPatch[b]; val py = seamAPatch[b + 1]; val pz = seamAPatch[b + 2]
                    val near = sampleNearestRenderedBodyTri(scene.blossomBodyMesh, px, py, pz)
                    val dist = near[0]
                    val dot = seamAPatch[b + 3] * near[1] + seamAPatch[b + 4] * near[2] + seamAPatch[b + 5] * near[3]
                    if (dist > maxRimDist) maxRimDist = dist
                    if (dot < minRimNormalDot) minRimNormalDot = dot
                    if (c == cols - 1 && dist > maxRightRimDist) maxRightRimDist = dist
                }
            }
        }
        assertTrue("Seam A outermost ring must sit flush on body triangles (< 0.001), got maxRimDist=$maxRimDist (rightRim=$maxRightRimDist)", maxRimDist < 0.001f)
        assertTrue("Seam A outermost ring normals must match body triangles (dot > 0.999), got minDot=$minRimNormalDot", minRimNormalDot > 0.999f)
        assertTrue("Seam A must not alter interior vulva (< 1e-6), got $maxInteriorDiffA", maxInteriorDiffA < 1e-6f)
        assertTrue("Seam A must not alter Seam B material channels (< 1e-6), got $maxMatDiffA", maxMatDiffA < 1e-6f)

        // 2. SEAM B ONLY (Color): Geometry untouched, interior untouched, outer 40% widens fade & rim equals exact body skin
        scene.update(legacyBase.copy(betaSmoothVulvaSeam = false, betaBlendVulvaSeamColor = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        val seamBPatch = scene.blossomVulvaPatchMesh.vertexData
        var maxGeomDiffB = 0f
        var maxInteriorDiffB = 0f
        var maxOuterBandMatChange = 0f
        var maxRimMucosa = 0f
        var maxRimWetnessDiff = 0f
        var maxRimAoDiff = 0f
        var maxRimPigDiff = 0f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val i = r * cols + c
                val b = i * stride
                // Seam B must NOT alter geometry or normals
                for (k in 0..5) {
                    val d = abs(seamBPatch[b + k] - basePatch[b + k])
                    if (d > maxGeomDiffB) maxGeomDiffB = d
                }
                val bx = basePatch[b]; val by = basePatch[b + 1]
                val ovalR = sqrt((bx / BlossomSdfBodyBuilder.PATCH_RX).let { it * it } +
                    ((by - BlossomSdfBodyBuilder.PATCH_CENTER_Y) / BlossomSdfBodyBuilder.PATCH_RY).let { it * it })
                if (ovalR <= 0.54f) {
                    for (k in 0 until stride) {
                        val d = abs(seamBPatch[b + k] - basePatch[b + k])
                        if (d > maxInteriorDiffB) maxInteriorDiffB = d
                    }
                } else if (ovalR in 0.60f..0.92f) {
                    val dMat = abs(seamBPatch[b + 8] - basePatch[b + 8]) + abs(seamBPatch[b + 9] - basePatch[b + 9])
                    if (dMat > maxOuterBandMatChange) maxOuterBandMatChange = dMat
                }
                val isRim = r == 0 || r == rows - 1 || c == 0 || c == cols - 1
                if (isRim) {
                    if (abs(seamBPatch[b + 6]) > maxRimMucosa) maxRimMucosa = abs(seamBPatch[b + 6])
                    val wDiff = abs(seamBPatch[b + 7] - BlossomSdfBodyBuilder.BODY_BASE_WETNESS)
                    if (wDiff > maxRimWetnessDiff) maxRimWetnessDiff = wDiff
                    if ((r + c) % 4 == 0) {
                        val near = sampleNearestRenderedBodyTri(scene.blossomBodyMesh, seamBPatch[b], seamBPatch[b + 1], seamBPatch[b + 2])
                        val aoDiff = abs(seamBPatch[b + 8] - near[4])
                        val pigDiff = abs(seamBPatch[b + 9] - near[5])
                        if (aoDiff > maxRimAoDiff) maxRimAoDiff = aoDiff
                        if (pigDiff > maxRimPigDiff) maxRimPigDiff = pigDiff
                    }
                }
            }
        }
        assertTrue("Seam B must not alter patch geometry or normals (< 1e-6), got $maxGeomDiffB", maxGeomDiffB < 1e-6f)
        assertTrue("Seam B must not alter interior vulva (< 1e-6), got $maxInteriorDiffB", maxInteriorDiffB < 1e-6f)
        assertTrue("Seam B should widen material fade across outer 40% (> 0.015), got $maxOuterBandMatChange", maxOuterBandMatChange > 0.015f)
        assertTrue("Seam B rim mucosa must equal body 0.0 (< 1e-6), got $maxRimMucosa", maxRimMucosa < 1e-6f)
        assertTrue("Seam B rim wetness must equal body 0.055 (< 1e-6), got $maxRimWetnessDiff", maxRimWetnessDiff < 1e-6f)
        assertTrue("Seam B rim AO must match surrounding body skin (< 0.005), got $maxRimAoDiff", maxRimAoDiff < 0.005f)
        assertTrue("Seam B rim pigment must match surrounding body skin (< 0.005), got $maxRimPigDiff", maxRimPigDiff < 0.005f)

        // 3. BOTH Seam A + Seam B ON together -> exact flush rim + exact body material match, then OFF -> exact baseline!
        scene.update(legacyBase.copy(betaSmoothVulvaSeam = true, betaBlendVulvaSeamColor = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        assertOutwardWindingMatchesNormals("blossomVulvaPatchMesh(seamA+seamB)", scene.blossomVulvaPatchMesh)

        scene.update(legacyBase, includeLithotomyBody = true)
        var maxRestoreBody = 0f
        for (i in baseBody.indices) {
            val d = abs(scene.blossomBodyMesh.vertexData[i] - baseBody[i])
            if (d > maxRestoreBody) maxRestoreBody = d
        }
        var maxRestorePatch = 0f
        for (i in basePatch.indices) {
            val d = abs(scene.blossomVulvaPatchMesh.vertexData[i] - basePatch[i])
            if (d > maxRestorePatch) maxRestorePatch = d
        }
        assertTrue("Disabling Seam A & B must restore exact baseline body (< 1e-5), got $maxRestoreBody", maxRestoreBody < 1e-5f)
        assertTrue("Disabling Seam A & B must restore exact baseline patch (< 1e-5), got $maxRestorePatch", maxRestorePatch < 1e-5f)
    }

    @Test
    fun betaSeamCInteriorFalloffAndSeamDSmoothHoleInvariantsHold() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val legacyBase = VulvaDeformationState(legacyVulvaMode = true)
        scene.update(legacyBase, includeLithotomyBody = true)
        val baseBody = scene.blossomBodyMesh.vertexData.copyOf()
        val basePatch = scene.blossomVulvaPatchMesh.vertexData.copyOf()
        val baseArch = scene.labiaHoodArchMesh.vertexData.copyOf()
        val baseGlans = scene.glansMesh.vertexData.copyOf()

        // 1. Seam C ONLY: geometry & interior colors unchanged, outer-edge labia falloff widened
        scene.update(legacyBase.copy(betaSoftLabiaInteriorEdge = true), includeLithotomyBody = true)
        val seamCPatch = scene.blossomVulvaPatchMesh.vertexData
        var maxGeomDiffC = 0f
        var maxInteriorColorDiffC = 0f
        var maxOuterBandColorChangeC = 0f
        for (i in 0 until scene.blossomVulvaPatchMesh.vertexCount) {
            val b = i * stride
            for (k in 0..5) {
                val d = abs(seamCPatch[b + k] - basePatch[b + k])
                if (d > maxGeomDiffC) maxGeomDiffC = d
            }
            val x = basePatch[b]
            val y = basePatch[b + 1]
            if (abs(x) <= 0.020f && y in -0.20f..0.12f) {
                for (k in 6..9) {
                    val d = abs(seamCPatch[b + k] - basePatch[b + k])
                    if (d > maxInteriorColorDiffC) maxInteriorColorDiffC = d
                }
            } else if (abs(x) in 0.25f..0.52f && y in -0.58f..0.52f) {
                val dMat = abs(seamCPatch[b + 6] - basePatch[b + 6]) +
                    abs(seamCPatch[b + 8] - basePatch[b + 8]) +
                    abs(seamCPatch[b + 9] - basePatch[b + 9])
                if (dMat > maxOuterBandColorChangeC) maxOuterBandColorChangeC = dMat
            }
        }
        var maxArchGeomDiffC = 0f
        for (v in 0 until scene.labiaHoodArchMesh.vertexCount) {
            val b = v * stride
            for (k in 0..5) {
                val d = abs(scene.labiaHoodArchMesh.vertexData[b + k] - baseArch[b + k])
                if (d > maxArchGeomDiffC) maxArchGeomDiffC = d
            }
        }
        var maxGlansDiffC = 0f
        for (i in baseGlans.indices) {
            val d = abs(scene.glansMesh.vertexData[i] - baseGlans[i])
            if (d > maxGlansDiffC) maxGlansDiffC = d
        }
        assertTrue("Seam C must not alter patch geometry or normals (< 1e-6), got $maxGeomDiffC", maxGeomDiffC < 1e-6f)
        assertTrue("Seam C must not alter labia/hood arch geometry or normals (< 1e-6), got $maxArchGeomDiffC", maxArchGeomDiffC < 1e-6f)
        assertTrue("Seam C must not alter clitoral glans (< 1e-6), got $maxGlansDiffC", maxGlansDiffC < 1e-6f)
        assertTrue("Seam C must preserve interior vestibule colors (< 1e-6), got $maxInteriorColorDiffC", maxInteriorColorDiffC < 1e-6f)
        assertTrue("Seam C must widen outer-edge labia color falloff (> 0.01), got $maxOuterBandColorChangeC", maxOuterBandColorChangeC > 0.01f)

        // 2. Seam D ONLY: smooth per-fragment ovalR cutoff strictly outside quad-omission zone (0.80) and inside patch rim (1.00)
        assertTrue(
            "Seam D hole threshold must sit cleanly in (0.82, 0.96), got ${VulvaGlTextureView.SEAM_D_HOLE_OVAL_R}",
            VulvaGlTextureView.SEAM_D_HOLE_OVAL_R in 0.82f..0.96f
        )
        scene.update(legacyBase.copy(betaSmoothBodyHoleCutoff = true), includeLithotomyBody = true)
        var maxBodyDiffD = 0f
        for (i in baseBody.indices) {
            val d = abs(scene.blossomBodyMesh.vertexData[i] - baseBody[i])
            if (d > maxBodyDiffD) maxBodyDiffD = d
        }
        var maxPatchDiffD = 0f
        for (i in basePatch.indices) {
            val d = abs(scene.blossomVulvaPatchMesh.vertexData[i] - basePatch[i])
            if (d > maxPatchDiffD) maxPatchDiffD = d
        }
        assertTrue("Seam D must not corrupt body vertex data (< 1e-6), got $maxBodyDiffD", maxBodyDiffD < 1e-6f)
        assertTrue("Seam D must not alter patch vertex data (< 1e-6), got $maxPatchDiffD", maxPatchDiffD < 1e-6f)

        // 3. Disabling Seam C & D restores exact baseline
        scene.update(legacyBase, includeLithotomyBody = true)
        var maxRestorePatch = 0f
        for (i in basePatch.indices) {
            val d = abs(scene.blossomVulvaPatchMesh.vertexData[i] - basePatch[i])
            if (d > maxRestorePatch) maxRestorePatch = d
        }
        assertTrue("Disabling Seam C & D must restore exact baseline (< 1e-6), got $maxRestorePatch", maxRestorePatch < 1e-6f)
    }

    @Test
    fun betaRapheNeckAndHairRefinementsAreIndependentLocalizedAndReversible() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val legacyBase = VulvaDeformationState(legacyVulvaMode = true)
        scene.update(legacyBase, includeLithotomyBody = true)

        val baseBody = scene.blossomBodyMesh.vertexData.copyOf()
        val basePatch = scene.blossomVulvaPatchMesh.vertexData.copyOf()
        val baseGlans = scene.glansMesh.vertexData.copyOf()
        val baseArch = scene.labiaHoodArchMesh.vertexData.copyOf()
        val baseHead = scene.blossomHeadMesh.vertexData.copyOf()
        val baseHair = scene.blossomHairMesh.vertexData.copyOf()

        // 1. Beta Toggle 4 ONLY: Faint Perineal Raphe adds subtle midline ridge between vulva & anus without touching vulva/head/hair
        scene.update(legacyBase.copy(betaPerinealRaphe = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        var maxMidlineRapheLift = 0f
        val pBeta4 = scene.blossomVulvaPatchMesh.vertexData
        for (i in 0 until scene.blossomVulvaPatchMesh.vertexCount) {
            val b = i * stride
            val x = pBeta4[b]
            val y = pBeta4[b + 1]
            if (abs(x) < 0.015f && y in -0.76f..-0.54f) {
                val lift = pBeta4[b + 2] - basePatch[b + 2]
                if (lift > maxMidlineRapheLift) maxMidlineRapheLift = lift
            }
        }
        assertTrue(
            "Beta 4 should raise a subtle midline perineal raphe ridge (0.003..0.025), got $maxMidlineRapheLift",
            maxMidlineRapheLift in 0.003f..0.025f
        )
        for (i in baseGlans.indices) {
            assertTrue("Beta 4 must not alter clitoral glans", abs(scene.glansMesh.vertexData[i] - baseGlans[i]) < 1e-6f)
        }
        for (i in baseArch.indices) {
            assertTrue("Beta 4 must not alter labia/hood arch", abs(scene.labiaHoodArchMesh.vertexData[i] - baseArch[i]) < 1e-6f)
        }
        for (i in baseHead.indices) {
            assertTrue("Beta 4 must not alter head mesh", abs(scene.blossomHeadMesh.vertexData[i] - baseHead[i]) < 1e-6f)
        }
        for (i in baseHair.indices) {
            assertTrue("Beta 4 must not alter hair mesh", abs(scene.blossomHairMesh.vertexData[i] - baseHair[i]) < 1e-6f)
        }

        // 2. Beta Toggle 5 ONLY: Wide Smooth Neck Seam (~2x feather down neck) leaves face (y > 5.88), eyes, hair & torso/breasts untouched!
        val baseEyes = scene.blossomEyeMeshes.map { it.vertexData.copyOf() }
        scene.update(legacyBase.copy(betaSmoothNeckSeam = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertValidMesh(scene.blossomBodyMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(betaSmoothNeckSeam)", scene.blossomHeadMesh)

        var maxSubmentalAoChange = 0f
        var maxUpperFaceDiff = 0f
        val headBeta5 = scene.blossomHeadMesh.vertexData
        for (v in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = v * stride
            val baseY = baseHead[b + 1]
            if (baseY > 5.88f) {
                for (k in 0 until stride) {
                    val d = abs(headBeta5[b + k] - baseHead[b + k])
                    if (d > maxUpperFaceDiff) maxUpperFaceDiff = d
                }
            } else if (baseY < 5.72f) {
                val aoDiff = abs(headBeta5[b + 8] - baseHead[b + 8])
                if (aoDiff > maxSubmentalAoChange) maxSubmentalAoChange = aoDiff
            }
        }
        assertTrue("Beta 5 should blend submental AO smoothly across jaw-neck join (> 0.02), got $maxSubmentalAoChange", maxSubmentalAoChange > 0.02f)
        assertTrue("Beta 5 must not touch face above jaw rim (y > 5.88 diff < 1e-6), got $maxUpperFaceDiff", maxUpperFaceDiff < 1e-6f)
        scene.blossomEyeMeshes.forEachIndexed { idx, eyeMesh ->
            val baseEye = baseEyes[idx]
            for (i in baseEye.indices) {
                assertTrue("Beta 5 must not alter eye mesh $idx", abs(eyeMesh.vertexData[i] - baseEye[i]) < 1e-6f)
            }
        }
        for (i in baseHair.indices) {
            assertTrue("Beta 5 must not alter hair", abs(scene.blossomHairMesh.vertexData[i] - baseHair[i]) < 1e-6f)
        }

        var maxBelowNeckBodyDiff = 0f
        val bodyBeta5 = scene.blossomBodyMesh.vertexData
        for (v in 0 until scene.blossomBodyMesh.vertexCount) {
            val b = v * stride
            if (baseBody[b + 1] < 4.60f || abs(baseBody[b]) > 0.65f) {
                for (k in 0 until stride) {
                    val d = abs(bodyBeta5[b + k] - baseBody[b + k])
                    if (d > maxBelowNeckBodyDiff) maxBelowNeckBodyDiff = d
                }
            }
        }
        assertTrue("Beta 5 must not touch torso/breasts/shoulders outside neck (diff < 1e-6), got $maxBelowNeckBodyDiff", maxBelowNeckBodyDiff < 1e-6f)

        // 3. Beta Toggle 6 ONLY: Strand-Detail Hair preserves exact original silhouette/part/length while adding strand normals, shade variation & subtle tapered tips
        scene.update(legacyBase.copy(betaSoftHair = true), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHairMesh)
        assertOutwardWindingMatchesNormals("blossomHairMesh(betaSoftHair)", scene.blossomHairMesh)
        var beta6HairMinY = Float.POSITIVE_INFINITY
        var beta6MaxPosDiff = 0f
        var beta6MaxNormalDiff = 0f
        var beta6MaxShadeDiff = 0f
        val hairBeta6 = scene.blossomHairMesh.vertexData
        for (v in 0 until scene.blossomHairMesh.vertexCount) {
            val b = v * stride
            val y = hairBeta6[b + 1]
            if (y < beta6HairMinY) beta6HairMinY = y
            val posD = sqrt(
                (hairBeta6[b] - baseHair[b]) * (hairBeta6[b] - baseHair[b]) +
                    (hairBeta6[b + 1] - baseHair[b + 1]) * (hairBeta6[b + 1] - baseHair[b + 1]) +
                    (hairBeta6[b + 2] - baseHair[b + 2]) * (hairBeta6[b + 2] - baseHair[b + 2])
            )
            if (posD > beta6MaxPosDiff) beta6MaxPosDiff = posD
            val nrmD = sqrt(
                (hairBeta6[b + 3] - baseHair[b + 3]) * (hairBeta6[b + 3] - baseHair[b + 3]) +
                    (hairBeta6[b + 4] - baseHair[b + 4]) * (hairBeta6[b + 4] - baseHair[b + 4]) +
                    (hairBeta6[b + 5] - baseHair[b + 5]) * (hairBeta6[b + 5] - baseHair[b + 5])
            )
            if (nrmD > beta6MaxNormalDiff) beta6MaxNormalDiff = nrmD
            val shadeD = abs(hairBeta6[b + 9] - baseHair[b + 9])
            if (shadeD > beta6MaxShadeDiff) beta6MaxShadeDiff = shadeD
        }
        assertTrue("Beta 6 hair should preserve bob length (minY < 5.15), got $beta6HairMinY", beta6HairMinY < 5.15f)
        assertTrue("Beta 6 hair should preserve overall silhouette with subtle tapered irregular tips (0.005..0.065), got $beta6MaxPosDiff", beta6MaxPosDiff in 0.005f..0.065f)
        assertTrue("Beta 6 hair should add strand-direction normal detail (> 0.02), got $beta6MaxNormalDiff", beta6MaxNormalDiff > 0.02f)
        assertTrue("Beta 6 hair should add subtle per-strand tonal variation (> 0.015), got $beta6MaxShadeDiff", beta6MaxShadeDiff > 0.015f)

        // 4. All 6 Beta toggles ON together + head turn -> valid meshes, then ALL OFF -> exact baseline restoration across all meshes!
        scene.update(
            legacyBase.copy(
                headTurn = 0.6f,
                betaShortPerineum = true,
                betaSoftButtockTable = true,
                betaSmoothVulvaSeam = true,
                betaPerinealRaphe = true,
                betaSmoothNeckSeam = true,
                betaSoftHair = true
            ),
            includeLithotomyBody = true
        )
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        assertValidMesh(scene.blossomHeadMesh)
        assertValidMesh(scene.blossomHairMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(all6Beta+turn)", scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHairMesh(all6Beta+turn)", scene.blossomHairMesh)

        scene.update(legacyBase, includeLithotomyBody = true)
        fun maxDiff(a: FloatArray, b: FloatArray): Float {
            var m = 0f
            for (i in a.indices) {
                val d = abs(a[i] - b[i])
                if (d > m) m = d
            }
            return m
        }
        assertTrue("Resetting all 6 Beta toggles must restore exact baseline body (< 1e-5)", maxDiff(scene.blossomBodyMesh.vertexData, baseBody) < 1e-5f)
        assertTrue("Resetting all 6 Beta toggles must restore exact baseline patch (< 1e-5)", maxDiff(scene.blossomVulvaPatchMesh.vertexData, basePatch) < 1e-5f)
        assertTrue("Resetting all 6 Beta toggles must restore exact baseline head (< 1e-5)", maxDiff(scene.blossomHeadMesh.vertexData, baseHead) < 1e-5f)
        assertTrue("Resetting all 6 Beta toggles must restore exact baseline hair (< 1e-5)", maxDiff(scene.blossomHairMesh.vertexData, baseHair) < 1e-5f)
    }

    @Test
    fun seamFFixesLabiaHoodArchOutwardCcwWindingWithoutAlteringGeometryOrSeamE() {
        val scene = VulvaAnatomySceneMesh()
        scene.update(
            VulvaDeformationState(legacyVulvaMode = true, betaSoftArchEdge = true, betaWarmTransitionZone = false),
            includeLithotomyBody = true
        )
        val arch = scene.labiaHoodArchMesh
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val baseVerts = arch.vertexData.copyOf()
        val baseAlpha = FloatArray(arch.vertexCount).also { dst ->
            scene.labiaHoodArchAlphaBuffer.position(0)
            scene.labiaHoodArchAlphaBuffer.get(dst)
            scene.labiaHoodArchAlphaBuffer.position(0)
        }

        fun alignedTriangleRatio(idxBuf: java.nio.ShortBuffer): Float {
            val data = arch.vertexData
            idxBuf.position(0)
            var aligned = 0
            var total = 0
            var t = 0
            while (t + 2 < arch.indexCount) {
                val i0 = idxBuf.get(t).toInt() and 0xFFFF
                val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
                val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
                t += 3
                val b0 = i0 * stride
                val b1 = i1 * stride
                val b2 = i2 * stride
                val e1x = data[b1] - data[b0]
                val e1y = data[b1 + 1] - data[b0 + 1]
                val e1z = data[b1 + 2] - data[b0 + 2]
                val e2x = data[b2] - data[b0]
                val e2y = data[b2 + 1] - data[b0 + 1]
                val e2z = data[b2 + 2] - data[b0 + 2]
                val fx = e1y * e2z - e1z * e2y
                val fy = e1z * e2x - e1x * e2z
                val fz = e1x * e2y - e1y * e2x
                val fLen = sqrt(fx * fx + fy * fy + fz * fz)
                if (fLen < 1e-7f) continue
                val nx = (data[b0 + 3] + data[b1 + 3] + data[b2 + 3]) / 3f
                val ny = (data[b0 + 4] + data[b1 + 4] + data[b2 + 4]) / 3f
                val nz = (data[b0 + 5] + data[b1 + 5] + data[b2 + 5]) / 3f
                total++
                if ((fx * nx + fy * ny + fz * nz) / fLen > 0f) aligned++
            }
            return if (total > 0) aligned.toFloat() / total else 0f
        }

        val baselineRatio = alignedTriangleRatio(arch.indexBuffer)
        val seamFRatio = alignedTriangleRatio(scene.labiaHoodArchCcwIndexBuffer)
        assertTrue(
            "Baseline labiaHoodArchMesh.indexBuffer had inverted CW winding (< 5%), got ${(baselineRatio * 100f).toInt()}%",
            baselineRatio < 0.05f
        )
        assertTrue(
            "Seam F labiaHoodArchCcwIndexBuffer must have outward CCW winding matching normals (> 98%), got ${(seamFRatio * 100f).toInt()}%",
            seamFRatio > 0.98f
        )

        // Enabling Seam F must leave arch vertex geometry, normals, materials & Seam E alpha identical
        scene.update(
            VulvaDeformationState(legacyVulvaMode = true, betaSoftArchEdge = true, betaWarmTransitionZone = true),
            includeLithotomyBody = true
        )
        for (i in baseVerts.indices) {
            assertTrue("Seam F must not alter labiaHoodArchMesh vertex data", abs(arch.vertexData[i] - baseVerts[i]) < 1e-6f)
        }
        val seamFAlpha = FloatArray(arch.vertexCount).also { dst ->
            scene.labiaHoodArchAlphaBuffer.position(0)
            scene.labiaHoodArchAlphaBuffer.get(dst)
            scene.labiaHoodArchAlphaBuffer.position(0)
        }
        for (i in baseAlpha.indices) {
            assertTrue("Seam F must not alter Seam E alpha feathering", abs(seamFAlpha[i] - baseAlpha[i]) < 1e-6f)
        }
    }

    @Test
    fun betaV1DefaultAndLegacyVulvaToggleInvariantsHold() {
        val betaV1 = VulvaDeformationState()
        assertTrue("Default state should have legacyVulvaMode = false", !betaV1.legacyVulvaMode)
        assertTrue(
            "Default Beta v1 state must enable all 12 refinements",
            betaV1.betaShortPerineum &&
                betaV1.betaSoftButtockTable &&
                betaV1.betaSmoothVulvaSeam &&
                betaV1.betaBlendVulvaSeamColor &&
                betaV1.betaSoftLabiaInteriorEdge &&
                betaV1.betaSmoothBodyHoleCutoff &&
                betaV1.betaSoftArchEdge &&
                betaV1.betaWarmTransitionZone &&
                betaV1.betaRefineInnerLabia &&
                betaV1.betaPerinealRaphe &&
                betaV1.betaSmoothNeckSeam &&
                betaV1.betaSoftHair
        )

        val legacy = VulvaDeformationState(legacyVulvaMode = true)
        assertTrue("Legacy state should have legacyVulvaMode = true", legacy.legacyVulvaMode)
        assertTrue(
            "Legacy Vulva mode must disable all 12 Beta v1 refinements",
            !legacy.betaShortPerineum &&
                !legacy.betaSoftButtockTable &&
                !legacy.betaSmoothVulvaSeam &&
                !legacy.betaBlendVulvaSeamColor &&
                !legacy.betaSoftLabiaInteriorEdge &&
                !legacy.betaSmoothBodyHoleCutoff &&
                !legacy.betaSoftArchEdge &&
                !legacy.betaWarmTransitionZone &&
                !legacy.betaRefineInnerLabia &&
                !legacy.betaPerinealRaphe &&
                !legacy.betaSmoothNeckSeam &&
                !legacy.betaSoftHair
        )

        val scene = VulvaAnatomySceneMesh()
        scene.update(betaV1, includeLithotomyBody = true)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        assertValidMesh(scene.labiaHoodArchMesh)
        assertValidMesh(scene.glansMesh)
        assertValidMesh(scene.blossomHeadMesh)
        assertValidMesh(scene.blossomHairMesh)
        val betaV1Body = scene.blossomBodyMesh.vertexData.copyOf()
        val betaV1Patch = scene.blossomVulvaPatchMesh.vertexData.copyOf()
        val betaV1Arch = scene.labiaHoodArchMesh.vertexData.copyOf()

        // Non-vulva updates (eye blink, head turn) must hit the vulva dirty-flag fast path with bitwise-identical meshes
        scene.update(betaV1.copy(eyeBlink = 0.85f, headTurn = 0.35f), includeLithotomyBody = true)
        for (i in betaV1Patch.indices) {
            assertTrue("Vulva patch must remain identical across eyeBlink/headTurn", scene.blossomVulvaPatchMesh.vertexData[i] == betaV1Patch[i])
        }
        for (i in betaV1Arch.indices) {
            assertTrue("Labia arch must remain identical across eyeBlink/headTurn", scene.labiaHoodArchMesh.vertexData[i] == betaV1Arch[i])
        }

        // Switching to Legacy Vulva mode and back to Beta v1 restores exact Beta v1 geometry
        scene.update(legacy, includeLithotomyBody = true)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        scene.update(betaV1, includeLithotomyBody = true)
        var maxRestoreDiff = 0f
        for (i in betaV1Body.indices) {
            val d = abs(scene.blossomBodyMesh.vertexData[i] - betaV1Body[i])
            if (d > maxRestoreDiff) maxRestoreDiff = d
        }
        for (i in betaV1Patch.indices) {
            val d = abs(scene.blossomVulvaPatchMesh.vertexData[i] - betaV1Patch[i])
            if (d > maxRestoreDiff) maxRestoreDiff = d
        }
        assertTrue("Switching Legacy -> Beta v1 must restore exact Beta v1 meshes (< 1e-5), got $maxRestoreDiff", maxRestoreDiff < 1e-5f)
    }

    @Test
    fun roundedDigitTipsNaturalBreastProfileAndRibcageBreathingInvariantsHold() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        scene.update(VulvaDeformationState(breastSize = 0.50f), includeLithotomyBody = true)

        // 1. Rounded finger & toe tips (true plump hemispherical domes, never pointy cones)
        for (isThumb in listOf(false, true)) {
            val (baseRad, _) = BlossomUpperBodyBuilder.fingerRadiusProfile(0.88f, isThumb)
            val (r97, _) = BlossomUpperBodyBuilder.fingerRadiusProfile(0.97f, isThumb)
            val (r99, _) = BlossomUpperBodyBuilder.fingerRadiusProfile(0.99f, isThumb)
            assertTrue(
                "Fingertip (isThumb=$isThumb) at s=0.97 must stay plump & rounded (> 0.65x shaft), got ${r97 / baseRad}",
                r97 / baseRad > 0.65f
            )
            assertTrue(
                "Fingertip (isThumb=$isThumb) at s=0.99 must curve bluntly (> 0.35x shaft), got ${r99 / baseRad}",
                r99 / baseRad > 0.35f
            )
        }
        BlossomSdfBodyBuilder.BLOSSOM_TOE_SPECS.forEachIndexed { idx, spec ->
            val isHallux = idx == 0
            val sShaft = BlossomSdfBodyBuilder.toeTipStartS(spec, isHallux)
            val (baseRad, _) = BlossomSdfBodyBuilder.toeRadiusProfile(sShaft, spec, isHallux)
            val sDome40 = BlossomSdfBodyBuilder.toeRowParameterS(10, 14, spec, isHallux)
            val sDome60 = BlossomSdfBodyBuilder.toeRowParameterS(11, 14, spec, isHallux)
            val (rad40, _) = BlossomSdfBodyBuilder.toeRadiusProfile(sDome40, spec, isHallux)
            val (rad60, _) = BlossomSdfBodyBuilder.toeRadiusProfile(sDome60, spec, isHallux)
            assertTrue(
                "Toe#$idx at 40-deg dome ring must stay plump (> 0.65x shaft), got ${rad40 / baseRad}",
                rad40 / baseRad > 0.65f
            )
            assertTrue(
                "Toe#$idx at 60-deg dome ring must round bluntly (> 0.40x shaft), got ${rad60 / baseRad}",
                rad60 / baseRad > 0.40f
            )
        }
        assertValidMesh(scene.blossomHandsMesh)
        assertValidMesh(scene.blossomToesMesh)
        assertValidMesh(scene.blossomNailsMesh)
        assertOutwardWindingMatchesNormals("blossomHandsMesh(rounded)", scene.blossomHandsMesh)
        assertOutwardWindingMatchesNormals("blossomToesMesh(rounded)", scene.blossomToesMesh)
        assertOutwardWindingMatchesNormals("blossomNailsMesh(rounded)", scene.blossomNailsMesh)

        // 2. Lifelike non-cylindrical supine breast side profile (continuous slope, no flat top + steep cliff)
        val bCache = BlossomUpperBodyBuilder.buildBreastsCache()
        val bData = scene.blossomBreastsMesh.vertexData
        var peakLift = 0f
        var upperSlopeLift = -1f
        var lowerSlopeLift = -1f
        var bestUpperDist = Float.POSITIVE_INFINITY
        var bestLowerDist = Float.POSITIVE_INFINITY
        for (i in 0 until bCache.left.count) {
            val lift = bData[i * stride + 2] - bCache.left.baseZ[i]
            if (lift > peakLift) peakLift = lift
            val ex = bCache.left.localEx[i]
            val ey = bCache.left.localEy[i]
            val dUpper = abs(ex + 0.06f) + abs(ey - 0.45f)
            if (dUpper < bestUpperDist) {
                bestUpperDist = dUpper
                upperSlopeLift = lift
            }
            val dLower = abs(ex + 0.06f) + abs(ey + 0.50f)
            if (dLower < bestLowerDist) {
                bestLowerDist = dLower
                lowerSlopeLift = lift
            }
        }
        val upperRatio = upperSlopeLift / peakLift
        val lowerRatio = lowerSlopeLift / peakLift
        assertTrue(
            "Upper breast slope (ey=+0.45) must sweep gently out of chest (0.20..0.62 of peak), got $upperRatio",
            upperRatio in 0.20f..0.62f
        )
        assertTrue(
            "Lower breast slope (ey=-0.50) must curve naturally instead of forming a flat-topped cylinder (0.42..0.78 of peak), got $lowerRatio",
            lowerRatio in 0.42f..0.78f
        )
        assertTrue(
            "Lower pole ($lowerRatio) should remain naturally fuller than upper pectoral slope ($upperRatio)",
            lowerRatio > upperRatio + 0.08f
        )
        for (sz in listOf(0.0f, 0.50f, 1.0f)) {
            scene.update(VulvaDeformationState(breastSize = sz), includeLithotomyBody = true)
            assertValidMesh(scene.blossomBreastsMesh)
            assertOutwardWindingMatchesNormals("blossomBreastsMesh(size=$sz)", scene.blossomBreastsMesh)
        }
        scene.update(VulvaDeformationState(breastSize = 0.50f), includeLithotomyBody = true)

        // 3. Thoracic ribcage breathing: chest expands in lockstep with abdomen & ZERO breast Z-telescoping!
        val chestAmp = 0.072f
        val abdAmp = 0.096f
        val sternum = BlossomSdfBodyBuilder.sampleTorsoChestWallWrapped(0f, BlossomUpperBodyBuilder.BREAST_CENTER_Y)
        val sternumOff = VulvaGlTextureView.evaluateVertexBreathingOffset(
            sternum.x, BlossomUpperBodyBuilder.BREAST_CENTER_Y, sternum.z,
            sternum.nx, sternum.ny, sternum.nz, chestAmp, abdAmp
        )
        assertTrue(
            "Anterior sternum/chest wall must visibly rise with lungs (dZ >= 0.065), got ${sternumOff[2]}",
            sternumOff[2] >= 0.065f
        )
        val latRib = BlossomSdfBodyBuilder.sampleTorsoChestWallWrapped(1.15f, BlossomUpperBodyBuilder.BREAST_CENTER_Y)
        val latRibOff = VulvaGlTextureView.evaluateVertexBreathingOffset(
            latRib.x, BlossomUpperBodyBuilder.BREAST_CENTER_Y, latRib.z,
            latRib.nx, latRib.ny, latRib.nz, chestAmp, abdAmp
        )
        assertTrue(
            "Lateral ribcage must expand outward (bucket-handle dX >= 0.015) and forward (dZ >= 0.055), got dX=${latRibOff[0]}, dZ=${latRibOff[2]}",
            latRibOff[0] >= 0.015f && latRibOff[2] >= 0.055f
        )
        // Verify breast apex and underlying chest wall move in exact rigid lockstep in Z (zero telescoping!)
        val breastBase = BlossomSdfBodyBuilder.sampleTorsoChestWallWrapped(
            BlossomUpperBodyBuilder.BREAST_CENTER_AX,
            BlossomUpperBodyBuilder.BREAST_CENTER_Y
        )
        val baseOff = VulvaGlTextureView.evaluateVertexBreathingOffset(
            breastBase.x, BlossomUpperBodyBuilder.BREAST_CENTER_Y, breastBase.z,
            breastBase.nx, breastBase.ny, breastBase.nz, chestAmp, abdAmp
        )
        val apexOff = VulvaGlTextureView.evaluateVertexBreathingOffset(
            breastBase.x, BlossomUpperBodyBuilder.BREAST_CENTER_Y, breastBase.z + 0.38f,
            0f, 0f, 1f, chestAmp, abdAmp
        )
        val flankOff = VulvaGlTextureView.evaluateVertexBreathingOffset(
            breastBase.x, BlossomUpperBodyBuilder.BREAST_CENTER_Y, breastBase.z + 0.16f,
            0.78f, 0.25f, 0.56f, chestAmp, abdAmp
        )
        assertTrue(
            "Breast apex must ride rigidly with chest wall beneath it (|dZ_apex - dZ_base| < 0.002), got ${abs(apexOff[2] - baseOff[2])}",
            abs(apexOff[2] - baseOff[2]) < 0.002f
        )
        assertTrue(
            "Sloped breast flanks must lift identically to breast apex (|dZ_apex - dZ_flank| < 0.002), got ${abs(apexOff[2] - flankOff[2])}",
            abs(apexOff[2] - flankOff[2]) < 0.002f
        )
        // Back pinned to exam table, arms/head/vulva undisturbed, stomach still rises cleanly
        val backZ = BlossomBodyBuilder.mattressSurfaceZ(BlossomUpperBodyBuilder.BREAST_CENTER_Y) + 0.05f
        val backOff = VulvaGlTextureView.evaluateVertexBreathingOffset(0f, BlossomUpperBodyBuilder.BREAST_CENTER_Y, backZ, 0f, 0f, -1f, chestAmp, abdAmp)
        val armOff = VulvaGlTextureView.evaluateVertexBreathingOffset(1.75f, 3.50f, BlossomBodyBuilder.mattressSurfaceZ(3.50f) + 0.50f, 0f, 0f, 1f, chestAmp, abdAmp)
        val vulvaOff = VulvaGlTextureView.evaluateVertexBreathingOffset(0f, 0f, 0.20f, 0f, 0f, 1f, chestAmp, abdAmp)
        val stomach = BlossomSdfBodyBuilder.sampleBodySurfacePoint(0f, 1.90f)
        val stomachOff = VulvaGlTextureView.evaluateVertexBreathingOffset(0f, 1.90f, stomach.z, 0f, 0f, 1f, chestAmp, abdAmp)
        assertTrue("Back on cushion must stay pinned (dZ < 0.001), got ${backOff[2]}", abs(backOff[2]) < 0.001f)
        assertTrue("Resting upper arm must remain undisturbed (< 0.001), got ${armOff[2]}", abs(armOff[2]) < 0.001f)
        assertTrue("Vulva must remain undisturbed (< 0.001), got ${vulvaOff[2]}", abs(vulvaOff[2]) < 0.001f)
        assertTrue("Stomach breathing must remain visibly smooth (dZ >= 0.085), got ${stomachOff[2]}", stomachOff[2] >= 0.085f)
    }

    private data class VulvaArchDiagnosticReport(
        val poseName: String,
        val totalTriangles: Int,
        val degenerateTriangles: Int,
        val windingInvertedCcwCount: Int,
        val outerSkirtNonMonotonicSteps: Int,
        val innerWallNonMonotonicSteps: Int,
        val midlineCrossingVertices: Int,
        val glansInterpenetratingVertices: Int,
        val opaqueArchBelowPatchVertices: Int,
        val visibleArchBelowPatchVertices: Int,
        val maxArchOvalR: Float
    )

    private fun inspectLabiaHoodArchDiagnostics(
        scene: VulvaAnatomySceneMesh,
        state: VulvaDeformationState,
        poseName: String
    ): VulvaArchDiagnosticReport {
        val data = scene.labiaHoodArchMesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val cols = scene.archCols
        val rows = scene.archRows
        val alphas = scene.labiaHoodArchAlphaData
        val ccwBuf = scene.labiaHoodArchCcwIndexBuffer
        ccwBuf.position(0)

        var totalTris = 0
        var degenTris = 0
        var invertedWinding = 0
        val idxCount = scene.labiaHoodArchMesh.indexCount
        var t = 0
        while (t + 2 < idxCount) {
            val i0 = ccwBuf.get(t).toInt() and 0xFFFF
            val i1 = ccwBuf.get(t + 1).toInt() and 0xFFFF
            val i2 = ccwBuf.get(t + 2).toInt() and 0xFFFF
            t += 3
            totalTris++

            val b0 = i0 * stride
            val b1 = i1 * stride
            val b2 = i2 * stride
            val e1x = data[b1] - data[b0]
            val e1y = data[b1 + 1] - data[b0 + 1]
            val e1z = data[b1 + 2] - data[b0 + 2]
            val e2x = data[b2] - data[b0]
            val e2y = data[b2 + 1] - data[b0 + 1]
            val e2z = data[b2 + 2] - data[b0 + 2]
            val fx = e1y * e2z - e1z * e2y
            val fy = e1z * e2x - e1x * e2z
            val fz = e1x * e2y - e1y * e2x
            val fLen = sqrt(fx * fx + fy * fy + fz * fz)
            if (fLen < 1e-8f) {
                degenTris++
                continue
            }
            // In the XY anatomical front view, an outward-wound CCW triangle on the arch manifold
            // has positive geometric Z-component (fz > 0) unless the surface folded back over itself.
            if (fz < -1e-7f) {
                invertedWinding++
            }
        }

        // Compute outward row normal O(r) from the centerline (c = 0..cols-1) to verify lateral monotonicity
        val glansCenterY = 0.254f
        val glansBaseZ = scene.sampleBodySurfaceZForTest(0f, glansCenterY, state, includeLithotomy = true) - 0.008f
        val glansRx = 0.034f + state.hoodRetract * 0.004f
        val glansRy = 0.045f + state.hoodRetract * 0.005f
        val glansRz = 0.042f

        var outerNonMono = 0
        var innerNonMono = 0
        var midlineCross = 0
        var glansClip = 0
        var opaqueBelowPatch = 0
        var visibleBelowPatch = 0
        var maxArchOvalR = 0f

        for (r in 0 until rows) {
            val arcS = (r.toFloat() / (rows - 1)) * 2f - 1f
            val absS = abs(arcS)
            val signS = if (arcS < 0f) -1f else 1f
            val isLeft = arcS < 0f
            val crestU = if (isLeft) 0.37f else 0.35f

            // Estimate row outward direction from c = 0 -> c = cols-1 (guaranteed radially outward)
            val bStart = (r * cols) * stride
            val bEnd = (r * cols + cols - 1) * stride
            var ox = data[bEnd] - data[bStart]
            var oy = data[bEnd + 1] - data[bStart + 1]
            val oLen = sqrt(ox * ox + oy * oy).coerceAtLeast(1e-6f)
            ox /= oLen
            oy /= oLen

            for (c in 0 until cols) {
                val u = c.toFloat() / (cols - 1)
                val vIdx = r * cols + c
                val b = vIdx * stride
                val px = data[b]
                val py = data[b + 1]
                val pz = data[b + 2]
                val alpha = alphas[vIdx]

                val ovalR = sqrt((px / 0.64f) * (px / 0.64f) + ((py + 0.06f) / 1.02f) * ((py + 0.06f) / 1.02f))
                if (ovalR > maxArchOvalR) maxArchOvalR = ovalR

                if (c > 0) {
                    val bPrev = (vIdx - 1) * stride
                    val dpx = px - data[bPrev]
                    val dpy = py - data[bPrev + 1]
                    val stepOut = dpx * ox + dpy * oy
                    if (stepOut < -1e-5f) {
                        val uPrev = (c - 1).toFloat() / (cols - 1)
                        if (uPrev >= crestU) {
                            outerNonMono++
                        } else {
                            innerNonMono++
                        }
                    }
                }

                if (absS >= 0.06f && px * signS < -0.0005f) {
                    midlineCross++
                }

                if (alpha > 0.05f && pz > glansBaseZ) {
                    val gx = px / glansRx
                    val gy = (py - glansCenterY) / glansRy
                    val gz = (pz - glansBaseZ) / glansRz
                    if (gx * gx + gy * gy + gz * gz < 0.98f) {
                        glansClip++
                    }
                }

                if (alpha > 0.05f && u >= 0.16f && u < 0.90f) {
                    val groundZ = scene.sampleBodySurfaceZForTest(px, py, state, includeLithotomy = true)
                    if (pz < groundZ - 0.0005f) {
                        visibleBelowPatch++
                        if (alpha >= 0.995f) {
                            opaqueBelowPatch++
                        }
                    }
                }
            }
        }

        return VulvaArchDiagnosticReport(
            poseName = poseName,
            totalTriangles = totalTris,
            degenerateTriangles = degenTris,
            windingInvertedCcwCount = invertedWinding,
            outerSkirtNonMonotonicSteps = outerNonMono,
            innerWallNonMonotonicSteps = innerNonMono,
            midlineCrossingVertices = midlineCross,
            glansInterpenetratingVertices = glansClip,
            opaqueArchBelowPatchVertices = opaqueBelowPatch,
            visibleArchBelowPatchVertices = visibleBelowPatch,
            maxArchOvalR = maxArchOvalR
        )
    }

    private fun v13BaselineState(
        leftOuter: Float = 0f,
        rightOuter: Float = 0f,
        leftInner: Float = 0f,
        rightInner: Float = 0f,
        hoodRetract: Float = 0f
    ): VulvaDeformationState = VulvaDeformationState(
        leftOuter = leftOuter,
        rightOuter = rightOuter,
        leftInner = leftInner,
        rightInner = rightInner,
        hoodRetract = hoodRetract,
        betaV2SeamOnlyPolyOffset = false,
        betaV2NonInvertingLabiaFold = false,
        betaV2AlignedSulcus = false,
        betaV2NaturalPelvicFinish = false,
        betaV2BalancedUpperVulvaDepth = false,
        betaV2BalancedUpperVulvaDepthWithCrest = false,
        betaV2AdjustableVestibularConcavity = false
    )

    @Test
    fun vulvaBetaV2DiagnosticsAndIndependentToggleInvariantsHold() {
        val scene = VulvaAnatomySceneMesh()

        // 1. Live defaults check: all 7 Beta v2 refinements are now baked live ON (30% vestibular concavity),
        // while all new Beta v3 + breast boost + Lifelike channels default cleanly OFF/0.
        val defaultState = VulvaDeformationState()
        assertTrue(defaultState.betaV2SeamOnlyPolyOffset)
        assertTrue(defaultState.betaV2NonInvertingLabiaFold)
        assertTrue(defaultState.betaV2AlignedSulcus)
        assertTrue(defaultState.betaV2NaturalPelvicFinish)
        assertTrue(defaultState.betaV2BalancedUpperVulvaDepth)
        assertTrue(defaultState.betaV2BalancedUpperVulvaDepthWithCrest)
        assertTrue(defaultState.betaV2AdjustableVestibularConcavity)
        assertEquals(0.30f, defaultState.betaV2VestibularConcavity, 1e-6f)
        assertFalse(defaultState.betaV3OrganicContinuity)
        assertFalse(defaultState.betaV3SoftPigmentation)
        assertFalse(defaultState.betaV3AnatomicalAsymmetry)
        assertFalse(defaultState.betaV3RegionalSurfaceDetail)
        assertFalse(defaultState.betaV3LabialVestibuleBlend)
        assertFalse(defaultState.betaV3IntricateLabiaFolds)
        assertEquals(0f, defaultState.betaBreastSizeBoost, 0f)
        assertFalse(defaultState.betaLifeDeepBreathsSighs)
        assertEquals(0f, defaultState.eyeGazeX, 0f)
        assertEquals(0f, defaultState.eyeGazeY, 0f)
        assertEquals(0f, defaultState.headRollOffset, 0f)
        assertEquals(0f, defaultState.faceWarmth, 0f)
        assertEquals(0f, defaultState.faceBrow, 0f)
        assertEquals(0f, defaultState.faceLipSoft, 0f)
        assertEquals(0f, defaultState.shoulderRelaxL, 0f)
        assertEquals(0f, defaultState.shoulderRelaxR, 0f)

        // Historical v13.0 baseline bit-identity round-trip check
        val v13State = v13BaselineState()
        scene.update(v13State, includeLithotomyBody = true)
        val baselineArchCopy = scene.labiaHoodArchMesh.vertexData.copyOf()
        val baselinePatchCopy = scene.blossomVulvaPatchMesh.vertexData.copyOf()
        val baselineGlansCopy = scene.glansMesh.vertexData.copyOf()
        val baselineAlphaCopy = scene.labiaHoodArchAlphaData.copyOf()

        // Toggle all 7 Beta v2 switches ON, then back OFF, and verify exact 0.0f bit-identity
        scene.update(
            v13State.copy(
                betaV2SeamOnlyPolyOffset = true,
                betaV2NonInvertingLabiaFold = true,
                betaV2AlignedSulcus = true,
                betaV2NaturalPelvicFinish = true,
                betaV2BalancedUpperVulvaDepth = true,
                betaV2BalancedUpperVulvaDepthWithCrest = true,
                betaV2AdjustableVestibularConcavity = true,
                betaV2VestibularConcavity = 0.25f
            ),
            includeLithotomyBody = true
        )
        scene.update(v13State, includeLithotomyBody = true)
        assertTrue("Arch vertices must revert bit-for-bit to v13.0 when Beta v2 toggles are OFF", baselineArchCopy.contentEquals(scene.labiaHoodArchMesh.vertexData))
        assertTrue("Vulva patch vertices must revert bit-for-bit to v13.0 when Beta v2 toggles are OFF", baselinePatchCopy.contentEquals(scene.blossomVulvaPatchMesh.vertexData))
        assertTrue("Glans vertices must revert bit-for-bit to v13.0 when Beta v2 toggles are OFF", baselineGlansCopy.contentEquals(scene.glansMesh.vertexData))
        assertTrue("Arch alpha buffer must revert bit-for-bit to v13.0 when Beta v2 toggles are OFF", baselineAlphaCopy.contentEquals(scene.labiaHoodArchAlphaData))

        // 2. Toggle 1: verify exact disjoint vulva-patch interior/seam triangle partition & safety margin
        val edgeR = scene.getVulvaPatchEdgeRForTest()
        val totalPatchIndices = scene.blossomVulvaPatchMesh.indexCount
        assertEquals(
            "Interior + seam index counts must partition blossomVulvaPatchMesh exactly",
            totalPatchIndices,
            scene.vulvaPatchInteriorIndexCount + scene.vulvaPatchSeamIndexCount
        )
        assertTrue("Interior partition must be non-empty", scene.vulvaPatchInteriorIndexCount > 0)
        assertTrue("Seam partition must be non-empty", scene.vulvaPatchSeamIndexCount > 0)
        scene.vulvaPatchInteriorIndexBuffer.position(0)
        var i = 0
        while (i + 2 < scene.vulvaPatchInteriorIndexCount) {
            val v0 = scene.vulvaPatchInteriorIndexBuffer.get(i).toInt() and 0xFFFF
            val v1 = scene.vulvaPatchInteriorIndexBuffer.get(i + 1).toInt() and 0xFFFF
            val v2 = scene.vulvaPatchInteriorIndexBuffer.get(i + 2).toInt() and 0xFFFF
            val maxR = max(edgeR[v0], max(edgeR[v1], edgeR[v2]))
            assertTrue("Interior triangle maxEdgeR must be < 0.88f, got $maxR", maxR < VulvaAnatomySceneMesh.VULVA_PATCH_SEAM_OFFSET_EDGE_R)
            i += 3
        }
        scene.vulvaPatchSeamIndexBuffer.position(0)
        var s = 0
        while (s + 2 < scene.vulvaPatchSeamIndexCount) {
            val v0 = scene.vulvaPatchSeamIndexBuffer.get(s).toInt() and 0xFFFF
            val v1 = scene.vulvaPatchSeamIndexBuffer.get(s + 1).toInt() and 0xFFFF
            val v2 = scene.vulvaPatchSeamIndexBuffer.get(s + 2).toInt() and 0xFFFF
            val maxR = max(edgeR[v0], max(edgeR[v1], edgeR[v2]))
            assertTrue("Seam triangle maxEdgeR must be >= 0.88f, got $maxR", maxR >= VulvaAnatomySceneMesh.VULVA_PATCH_SEAM_OFFSET_EDGE_R)
            s += 3
        }

        // 3. Automated geometry diagnostics across 5 critical poses: Baseline (v13.0) vs Toggle 1 & Toggle 2
        val testPoses = listOf(
            "Rest(0/0)" to v13BaselineState(),
            "SelfSpread(0.74/0.78)" to v13BaselineState(
                leftOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                rightOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                leftInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                rightInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET
            ),
            "FullSpread(1.0/1.0)" to v13BaselineState(
                leftOuter = 1.0f, rightOuter = 1.0f, leftInner = 1.0f, rightInner = 1.0f
            ),
            "AsymSpread(L1/R0)" to v13BaselineState(
                leftOuter = 1.0f, leftInner = 1.0f, rightOuter = 0.0f, rightInner = 0.0f
            ),
            "HoodRetract(1.0)" to v13BaselineState(hoodRetract = 1.0f),
            "FullSpread+Hood(1.0)" to v13BaselineState(
                leftOuter = 1.0f, rightOuter = 1.0f, leftInner = 1.0f, rightInner = 1.0f, hoodRetract = 1.0f
            )
        )

        var baselineSpreadOuterInversions = 0
        var baselineMidlineCrossingsTotal = 0
        var baselineGlansClipsTotal = 0

        for ((poseName, basePose) in testPoses) {
            // A. Baseline (v13.0) diagnostic measurement
            scene.update(basePose, includeLithotomyBody = true)
            val baseRep = inspectLabiaHoodArchDiagnostics(scene, basePose, "$poseName [v13.0]")
            println("DIAG $baseRep")
            if (poseName.startsWith("SelfSpread") || poseName.startsWith("FullSpread")) {
                baselineSpreadOuterInversions += baseRep.outerSkirtNonMonotonicSteps
            }
            baselineMidlineCrossingsTotal += baseRep.midlineCrossingVertices
            baselineGlansClipsTotal += baseRep.glansInterpenetratingVertices

            // Arch stays strictly inside ovalR < 0.85 even at full spread, validating the edgeR = 0.88 seam split!
            assertTrue(
                "$poseName baseline arch maxOvalR must stay safely inside 0.88 seam ring, got ${baseRep.maxArchOvalR}",
                baseRep.maxArchOvalR < 0.85f
            )

            // B. Toggle 1 ONLY (betaV2SeamOnlyPolyOffset = true)
            val t1Pose = basePose.copy(betaV2SeamOnlyPolyOffset = true)
            scene.update(t1Pose, includeLithotomyBody = true)
            val t1Rep = inspectLabiaHoodArchDiagnostics(scene, t1Pose, "$poseName [Toggle1]")
            println("DIAG $t1Rep")
            assertTrue(
                "$poseName baseline must exhibit visible tipSink below patch (> 0), got ${baseRep.visibleArchBelowPatchVertices}",
                baseRep.visibleArchBelowPatchVertices > 0
            )
            assertEquals(
                "$poseName Toggle 1 must keep all visible arch vertices (alpha > 0.05, u in 0.16..0.90) above vulva patch",
                0,
                t1Rep.visibleArchBelowPatchVertices
            )
            assertEquals(
                "$poseName Toggle 1 must keep all opaque arch vertices above vulva patch",
                0,
                t1Rep.opaqueArchBelowPatchVertices
            )

            // C. Toggle 2 ONLY (betaV2NonInvertingLabiaFold = true)
            val t2Pose = basePose.copy(betaV2NonInvertingLabiaFold = true)
            scene.update(t2Pose, includeLithotomyBody = true)
            val t2Rep = inspectLabiaHoodArchDiagnostics(scene, t2Pose, "$poseName [Toggle2]")
            println("DIAG $t2Rep")
            assertValidMesh(scene.labiaHoodArchMesh)
            assertEquals("$poseName Toggle 2 must have 0 outer-skirt fold-over steps", 0, t2Rep.outerSkirtNonMonotonicSteps)
            assertEquals("$poseName Toggle 2 must have 0 inner-wall fold-over steps", 0, t2Rep.innerWallNonMonotonicSteps)
            assertEquals("$poseName Toggle 2 must have 0 CCW triangle winding inversions", 0, t2Rep.windingInvertedCcwCount)
            assertEquals("$poseName Toggle 2 must have 0 midline crossing vertices", 0, t2Rep.midlineCrossingVertices)
            assertEquals("$poseName Toggle 2 must have 0 glans interpenetrating vertices", 0, t2Rep.glansInterpenetratingVertices)
            assertTrue(
                "$poseName Toggle 2 arch maxOvalR must remain safely inside 0.88 seam ring, got ${t2Rep.maxArchOvalR}",
                t2Rep.maxArchOvalR < 0.85f
            )

            // D. Toggle 1 + Toggle 2 combined
            val t12Pose = basePose.copy(betaV2SeamOnlyPolyOffset = true, betaV2NonInvertingLabiaFold = true)
            scene.update(t12Pose, includeLithotomyBody = true)
            val t12Rep = inspectLabiaHoodArchDiagnostics(scene, t12Pose, "$poseName [Toggle1+2]")
            assertEquals("$poseName Toggle 1+2 must have 0 CCW winding inversions", 0, t12Rep.windingInvertedCcwCount)
            assertEquals("$poseName Toggle 1+2 must have 0 opaque arch vertices below patch", 0, t12Rep.opaqueArchBelowPatchVertices)
        }

        // Confirm the diagnostic detector genuinely detects the baseline defects we diagnosed!
        assertTrue(
            "Detector must actually catch baseline outer-skirt fold-over in spread poses (got $baselineSpreadOuterInversions)",
            baselineSpreadOuterInversions > 0
        )
        assertTrue(
            "Detector must actually catch baseline midline crossing vertices (got $baselineMidlineCrossingsTotal)",
            baselineMidlineCrossingsTotal > 0
        )
        assertTrue(
            "Detector must actually catch baseline glans interpenetration (got $baselineGlansClipsTotal)",
            baselineGlansClipsTotal > 0
        )

        // 4. Toggle 3 (betaV2AlignedSulcus): verify outward alignment & complete independence from Toggle 2
        // Uses historical wide spread (0.74/0.78) calibrated to the fixed x=0.242 / x=0.350 probe coordinates
        val spreadBase = v13BaselineState(
            leftOuter = 0.74f,
            rightOuter = 0.74f,
            leftInner = 0.78f,
            rightInner = 0.78f
        )
        val spreadSulcusOnly = spreadBase.copy(betaV2AlignedSulcus = true, betaV2NonInvertingLabiaFold = false)
        val spreadSulcusAndFold = spreadBase.copy(betaV2AlignedSulcus = true, betaV2NonInvertingLabiaFold = true)
        // Old sulcus sat directly under arch base at x = 0.242; aligned sulcus shifts outward to ~0.350 (between minora & majora crest 0.430)
        val oldPosBase = scene.evaluateVulvaDetailForTest(0.242f, 0.0f, spreadBase)
        val oldPosAligned = scene.evaluateVulvaDetailForTest(0.242f, 0.0f, spreadSulcusOnly)
        val newPosBase = scene.evaluateVulvaDetailForTest(0.350f, 0.0f, spreadBase)
        val newPosAligned = scene.evaluateVulvaDetailForTest(0.350f, 0.0f, spreadSulcusOnly)
        val newPosAlignedWithT2 = scene.evaluateVulvaDetailForTest(0.350f, 0.0f, spreadSulcusAndFold)
        assertTrue(
            "Toggle 3 must relieve the recessed trough directly under the arch base at x=0.242 (dz ${oldPosAligned[0]} > ${oldPosBase[0]}, ao ${oldPosAligned[3]} > ${oldPosBase[3]})",
            oldPosAligned[0] > oldPosBase[0] + 0.010f && oldPosAligned[3] > oldPosBase[3] + 0.05f
        )
        assertTrue(
            "Toggle 3 must place a gentle groove cleanly at the interlabial space x=0.350 (dz ${newPosAligned[0]} < ${newPosBase[0]})",
            newPosAligned[0] < newPosBase[0] - 0.008f
        )
        assertEquals(
            "Toggle 3 sulcus evaluation must be 100% independent of Toggle 2 (identical dz)",
            newPosAligned[0], newPosAlignedWithT2[0], 1e-6f
        )
        assertEquals(
            "Toggle 3 sulcus evaluation must be 100% independent of Toggle 2 (identical ao)",
            newPosAligned[3], newPosAlignedWithT2[3], 1e-6f
        )
    }

    @Test
    fun vulvaBetaV2Toggle5BalancedUpperDepthDiagnosticsAndInvariantsHold() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val edgeR = scene.getVulvaPatchEdgeRForTest()

        fun countInvertedCcwTriangles(
            mesh: DynamicGlMesh,
            vertexEdgeR: FloatArray? = null,
            maxEdgeR: Float = Float.POSITIVE_INFINITY,
            minY: Float = Float.NEGATIVE_INFINITY
        ): Int {
            val data = mesh.vertexData
            val idxBuf = mesh.indexBuffer
            idxBuf.position(0)
            var inverted = 0
            var t = 0
            while (t + 2 < mesh.indexCount) {
                val i0 = idxBuf.get(t).toInt() and 0xFFFF
                val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
                val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
                t += 3
                if (vertexEdgeR != null) {
                    val triMaxR = max(vertexEdgeR[i0], max(vertexEdgeR[i1], vertexEdgeR[i2]))
                    if (triMaxR >= maxEdgeR) continue
                }
                val b0 = i0 * stride
                val b1 = i1 * stride
                val b2 = i2 * stride
                val cy = (data[b0 + 1] + data[b1 + 1] + data[b2 + 1]) / 3f
                if (cy < minY) continue
                val e1x = data[b1] - data[b0]; val e1y = data[b1 + 1] - data[b0 + 1]; val e1z = data[b1 + 2] - data[b0 + 2]
                val e2x = data[b2] - data[b0]; val e2y = data[b2 + 1] - data[b0 + 1]; val e2z = data[b2 + 2] - data[b0 + 2]
                val fx = e1y * e2z - e1z * e2y
                val fy = e1z * e2x - e1x * e2z
                val fz = e1x * e2y - e1y * e2x
                val fLen = sqrt(fx * fx + fy * fy + fz * fz)
                if (fLen < 1e-7f) continue
                val nx = (data[b0 + 3] + data[b1 + 3] + data[b2 + 3]) / 3f
                val ny = (data[b0 + 4] + data[b1 + 4] + data[b2 + 4]) / 3f
                val nz = (data[b0 + 5] + data[b1 + 5] + data[b2 + 5]) / 3f
                if ((fx * nx + fy * ny + fz * nz) / fLen < 0f) inverted++
            }
            return inverted
        }

        fun countGeometricFlippedHeightfieldTriangles(
            mesh: DynamicGlMesh,
            vertexEdgeR: FloatArray,
            maxEdgeR: Float
        ): Int {
            val data = mesh.vertexData
            val idxBuf = mesh.indexBuffer
            idxBuf.position(0)
            var flipped = 0
            var t = 0
            while (t + 2 < mesh.indexCount) {
                val i0 = idxBuf.get(t).toInt() and 0xFFFF
                val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
                val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
                t += 3
                val triMaxR = max(vertexEdgeR[i0], max(vertexEdgeR[i1], vertexEdgeR[i2]))
                if (triMaxR >= maxEdgeR) continue
                val b0 = i0 * stride
                val b1 = i1 * stride
                val b2 = i2 * stride
                val e1x = data[b1] - data[b0]; val e1y = data[b1 + 1] - data[b0 + 1]
                val e2x = data[b2] - data[b0]; val e2y = data[b2 + 1] - data[b0 + 1]
                val fz = e1x * e2y - e1y * e2x
                if (fz <= 0f) flipped++
            }
            return flipped
        }

        fun maxMeshZ(mesh: DynamicGlMesh): Float {
            val data = mesh.vertexData
            var best = Float.NEGATIVE_INFINITY
            for (v in 0 until mesh.vertexCount) {
                val z = data[v * stride + 2]
                if (z > best) best = z
            }
            return best
        }

        fun hoodCrestZ(archMesh: DynamicGlMesh): Float {
            val data = archMesh.vertexData
            val midRow = scene.archRows / 2
            // Sample the dorsal clitoral hood crest (u = crestU ~ 0.36) rather than the outer skirt landing on the commissure
            val crestCol = (0.36f * (scene.archCols - 1)).toInt()
            val v = midRow * scene.archCols + crestCol
            return data[v * stride + 2]
        }

        val testPoses = listOf(
            "Rest(0/0)" to v13BaselineState(),
            "SelfSpread(0.74/0.78)" to v13BaselineState(
                leftOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                rightOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                leftInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                rightInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET
            ),
            "FullSpread(1.0/1.0)" to v13BaselineState(
                leftOuter = 1.0f, rightOuter = 1.0f, leftInner = 1.0f, rightInner = 1.0f
            ),
            "AsymSpread(L1/R0)" to v13BaselineState(
                leftOuter = 1.0f, leftInner = 1.0f, rightOuter = 0.0f, rightInner = 0.0f
            ),
            "HoodRetract(1.0)" to v13BaselineState(hoodRetract = 1.0f),
            "FullSpread+Hood(1.0)" to v13BaselineState(
                leftOuter = 1.0f, rightOuter = 1.0f, leftInner = 1.0f, rightInner = 1.0f, hoodRetract = 1.0f
            )
        )

        val baseSdfAtGlans = BlossomSdfBodyBuilder.sampleBodySurfacePoint(0f, 0.254f).z

        for ((poseName, basePose) in testPoses) {
            // Baseline (v13.0)
            scene.update(basePose, includeLithotomyBody = true)
            val baseBodyData = scene.blossomBodyMesh.vertexData.copyOf()
            val basePatchData = scene.blossomVulvaPatchMesh.vertexData.copyOf()
            val baseGlansData = scene.glansMesh.vertexData.copyOf()
            val baseArchData = scene.labiaHoodArchMesh.vertexData.copyOf()
            val basePatchInvAll = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh)
            val basePatchInvInterior = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f)
            val basePatchInvUpper = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f, minY = 0.08f)
            val basePatchGeomFlip = countGeometricFlippedHeightfieldTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f)
            val baseGlansInv = countInvertedCcwTriangles(scene.glansMesh)
            val baseFloorGlans = scene.sampleBodySurfaceZForTest(0f, 0.254f, basePose, includeLithotomy = true)
            val baseGlansTipZ = maxMeshZ(scene.glansMesh)
            val baseHoodApexZ = hoodCrestZ(scene.labiaHoodArchMesh)
            val baseCommissureZ = scene.sampleBodySurfaceZForTest(0f, 0.46f, basePose, includeLithotomy = true)
            val majoraSampleX = 0.19f + basePose.rightOuter * 0.16f
            val baseMajoraZ = scene.sampleBodySurfaceZForTest(majoraSampleX, 0.25f, basePose, includeLithotomy = true)

            // A. Toggle 5 ONLY (betaV2BalancedUpperVulvaDepth = true, Toggles 1-4 OFF)
            val t5OnlyPose = basePose.copy(betaV2BalancedUpperVulvaDepth = true)
            scene.update(t5OnlyPose, includeLithotomyBody = true)
            assertValidMesh(scene.blossomVulvaPatchMesh)
            assertValidMesh(scene.glansMesh)
            assertValidMesh(scene.labiaHoodArchMesh)

            val t5BodyMaxDiff = (baseBodyData.indices).maxOf { abs(scene.blossomBodyMesh.vertexData[it] - baseBodyData[it]) }
            assertEquals("$poseName Toggle 5 must leave blossomBodyMesh bitwise untouched", 0f, t5BodyMaxDiff, 0f)

            var maxOuterSeamPosDiff = 0f
            var maxLowerVestPosDiff = 0f
            var maxPatchMatDiff = 0f
            val t5PatchData = scene.blossomVulvaPatchMesh.vertexData.copyOf()
            val t5ArchData = scene.labiaHoodArchMesh.vertexData.copyOf()
            for (v in 0 until scene.blossomVulvaPatchMesh.vertexCount) {
                val b = v * stride
                val y = basePatchData[b + 1]
                val posDiff = max(
                    abs(t5PatchData[b] - basePatchData[b]),
                    max(abs(t5PatchData[b + 1] - basePatchData[b + 1]), abs(t5PatchData[b + 2] - basePatchData[b + 2]))
                )
                if (edgeR[v] >= 0.58f && posDiff > maxOuterSeamPosDiff) maxOuterSeamPosDiff = posDiff
                if (y <= 0.08f && posDiff > maxLowerVestPosDiff) maxLowerVestPosDiff = posDiff
                for (m in 6..9) {
                    val md = abs(t5PatchData[b + m] - basePatchData[b + m])
                    if (md > maxPatchMatDiff) maxPatchMatDiff = md
                }
            }
            var maxGlansMatDiff = 0f
            for (v in 0 until scene.glansMesh.vertexCount) {
                val b = v * stride
                for (m in 6..9) {
                    val md = abs(scene.glansMesh.vertexData[b + m] - baseGlansData[b + m])
                    if (md > maxGlansMatDiff) maxGlansMatDiff = md
                }
            }
            var maxArchMatDiff = 0f
            for (v in 0 until scene.labiaHoodArchMesh.vertexCount) {
                val b = v * stride
                for (m in 6..9) {
                    val md = abs(scene.labiaHoodArchMesh.vertexData[b + m] - baseArchData[b + m])
                    if (md > maxArchMatDiff) maxArchMatDiff = md
                }
            }

            assertEquals("$poseName Toggle 5 must leave outer patch seam (edgeR >= 0.58) bitwise untouched", 0f, maxOuterSeamPosDiff, 0f)
            assertEquals("$poseName Toggle 5 must leave lower/mid vulva (y <= 0.08) bitwise untouched", 0f, maxLowerVestPosDiff, 0f)
            assertEquals("$poseName Toggle 5 must leave all patch mucosa/wetness/ao/pigment bitwise untouched", 0f, maxPatchMatDiff, 0f)
            assertEquals("$poseName Toggle 5 must leave all glans mucosa/wetness/ao/pigment bitwise untouched", 0f, maxGlansMatDiff, 0f)
            assertEquals("$poseName Toggle 5 must leave all arch mucosa/wetness/ao/pigment bitwise untouched", 0f, maxArchMatDiff, 0f)

            val t5PatchInvAll = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh)
            val t5PatchInvInterior = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f)
            val t5PatchInvUpper = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f, minY = 0.08f)
            val t5PatchGeomFlip = countGeometricFlippedHeightfieldTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f)
            val t5GlansInv = countInvertedCcwTriangles(scene.glansMesh)

            val t5FloorGlans = scene.sampleBodySurfaceZForTest(0f, 0.254f, t5OnlyPose, includeLithotomy = true)
            val t5GlansTipZ = maxMeshZ(scene.glansMesh)
            val t5HoodApexZ = hoodCrestZ(scene.labiaHoodArchMesh)
            val t5CommissureZ = scene.sampleBodySurfaceZForTest(0f, 0.46f, t5OnlyPose, includeLithotomy = true)
            val t5MajoraZ = scene.sampleBodySurfaceZForTest(majoraSampleX, 0.25f, t5OnlyPose, includeLithotomy = true)

            // Midline smoothness check across y in [0.08, 0.34]
            var maxBaseSlope = 0f
            var maxT5Slope = 0f
            val dyStep = 0.005f
            var yProbe = 0.08f
            while (yProbe < 0.34f) {
                val bz0 = scene.sampleBodySurfaceZForTest(0f, yProbe, basePose, includeLithotomy = true)
                val bz1 = scene.sampleBodySurfaceZForTest(0f, yProbe + dyStep, basePose, includeLithotomy = true)
                val tz0 = scene.sampleBodySurfaceZForTest(0f, yProbe, t5OnlyPose, includeLithotomy = true)
                val tz1 = scene.sampleBodySurfaceZForTest(0f, yProbe + dyStep, t5OnlyPose, includeLithotomy = true)
                val bs = abs((bz1 - bz0) / dyStep)
                val ts = abs((tz1 - tz0) / dyStep)
                if (bs > maxBaseSlope) maxBaseSlope = bs
                if (ts > maxT5Slope) maxT5Slope = ts
                yProbe += dyStep
            }

            // B. All 5 Beta v2 toggles ON together
            val all5Pose = basePose.copy(
                betaV2SeamOnlyPolyOffset = true,
                betaV2NonInvertingLabiaFold = true,
                betaV2AlignedSulcus = true,
                betaV2NaturalPelvicFinish = true,
                betaV2BalancedUpperVulvaDepth = true
            )
            scene.update(all5Pose, includeLithotomyBody = true)
            assertValidMesh(scene.blossomVulvaPatchMesh)
            assertValidMesh(scene.glansMesh)
            assertValidMesh(scene.labiaHoodArchMesh)
            val all5ArchRep = inspectLabiaHoodArchDiagnostics(scene, all5Pose, "$poseName [All5BetaV2]")
            val all5PatchInvAll = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh)
            val all5PatchInvInterior = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f)
            val all5PatchInvUpper = countInvertedCcwTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f, minY = 0.08f)
            val all5PatchGeomFlip = countGeometricFlippedHeightfieldTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f)
            val all5GlansInv = countInvertedCcwTriangles(scene.glansMesh)
            val all5FloorGlans = scene.sampleBodySurfaceZForTest(0f, 0.254f, all5Pose, includeLithotomy = true)
            val all5GlansTipZ = maxMeshZ(scene.glansMesh)
            val all5HoodApexZ = hoodCrestZ(scene.labiaHoodArchMesh)

            val glansLift = t5FloorGlans - baseFloorGlans
            println(
                (
                    "T5_DIAG pose=$poseName sdfGlansZ=%.4f baseFloor=%.4f->t5Floor=%.4f(lift=%+.4f) " +
                        "glansTip=%.4f->%.4f hoodApex=%.4f->%.4f(all5=%.4f) majoraZ=%.4f commZ=%.4f " +
                        "maxSlope=%.3f->%.3f patchGeomFlip=%d/%d/%d patchInvUpper=%d/%d/%d " +
                        "patchInvInt=%d/%d/%d patchInvAll=%d/%d/%d glansInv=%d/%d " +
                        "archAll5Inv=%d foldOut=%d foldIn=%d midCross=%d glansClip=%d belowVis=%d maxOvalR=%.4f"
                    ).format(
                        baseSdfAtGlans, baseFloorGlans, t5FloorGlans, glansLift,
                        baseGlansTipZ, t5GlansTipZ, baseHoodApexZ, t5HoodApexZ, all5HoodApexZ,
                        baseMajoraZ, baseCommissureZ,
                        maxBaseSlope, maxT5Slope,
                        basePatchGeomFlip, t5PatchGeomFlip, all5PatchGeomFlip,
                        basePatchInvUpper, t5PatchInvUpper, all5PatchInvUpper,
                        basePatchInvInterior, t5PatchInvInterior, all5PatchInvInterior,
                        basePatchInvAll, t5PatchInvAll, all5PatchInvAll,
                        t5GlansInv, all5GlansInv,
                        all5ArchRep.windingInvertedCcwCount, all5ArchRep.outerSkirtNonMonotonicSteps,
                        all5ArchRep.innerWallNonMonotonicSteps, all5ArchRep.midlineCrossingVertices,
                        all5ArchRep.glansInterpenetratingVertices, all5ArchRep.visibleArchBelowPatchVertices,
                        all5ArchRep.maxArchOvalR
                    )
            )

            // Winding & normal-alignment invariants
            assertEquals("$poseName Toggle 5 patch interior geometric XY winding flips (fz <= 0)", 0, t5PatchGeomFlip)
            assertEquals("$poseName All-5 patch interior geometric XY winding flips (fz <= 0)", 0, all5PatchGeomFlip)
            assertEquals("$poseName Toggle 5 upper vulva (y > 0.08, edgeR < 0.58) face-vs-normal inversions", 0, t5PatchInvUpper)
            assertEquals("$poseName All-5 upper vulva (y > 0.08, edgeR < 0.58) face-vs-normal inversions", 0, all5PatchInvUpper)
            assertTrue(
                "$poseName Toggle 5 must not increase patch interior face-vs-normal inversions (t5=$t5PatchInvInterior vs base=$basePatchInvInterior)",
                t5PatchInvInterior <= basePatchInvInterior
            )
            assertTrue(
                "$poseName All-5 must not increase patch interior face-vs-normal inversions (all5=$all5PatchInvInterior vs base=$basePatchInvInterior)",
                all5PatchInvInterior <= basePatchInvInterior
            )
            assertTrue(
                "$poseName Toggle 5 must not increase full-patch face-vs-normal inversions (t5=$t5PatchInvAll vs base=$basePatchInvAll)",
                t5PatchInvAll <= basePatchInvAll
            )
            assertTrue(
                "$poseName All-5 must not increase full-patch face-vs-normal inversions (all5=$all5PatchInvAll vs base=$basePatchInvAll)",
                all5PatchInvAll <= basePatchInvAll
            )
            assertEquals("$poseName Toggle 5 glans must preserve exact baseline triangle orientation", baseGlansInv, t5GlansInv)
            assertEquals("$poseName All-5 glans must preserve exact baseline triangle orientation", baseGlansInv, all5GlansInv)

            // Exact glans-to-floor sync (apex = floorZ - 0.008 + 0.042 = floorZ + 0.034)
            assertEquals(
                "$poseName Toggle 5 glans apex must track raised floorZ exactly (+0.034f)",
                t5FloorGlans + 0.034f,
                t5GlansTipZ,
                1e-4f
            )
            assertEquals("$poseName All-5 glans apex sync", all5FloorGlans + 0.034f, all5GlansTipZ, 1e-4f)

            // Measurable lift inside the actual upper vestibule without overshooting anterior commissure or outer labia majora
            val isSymmetric = basePose.leftOuter == basePose.rightOuter && basePose.leftInner == basePose.rightInner
            val probeVestX = if (isSymmetric) 0f else -0.09f
            val baseVestUpperZ = scene.sampleBodySurfaceZForTest(probeVestX, 0.254f, basePose, includeLithotomy = true)
            val t5VestUpperZ = scene.sampleBodySurfaceZForTest(probeVestX, 0.254f, t5OnlyPose, includeLithotomy = true)
            val upperVestLift = t5VestUpperZ - baseVestUpperZ
            assertTrue("$poseName Toggle 5 must not lower midline floor at glans (lift=$glansLift)", glansLift >= 0f)
            assertTrue("$poseName Toggle 5 must lift upper vestibule floor measurably (> +0.035 at x=$probeVestX), got $upperVestLift", upperVestLift > 0.035f)
            assertTrue("$poseName Toggle 5 must remain conservative (lift < +0.070 at x=$probeVestX), got $upperVestLift", upperVestLift < 0.070f)
            assertEquals("$poseName Toggle 5 must leave anterior commissure Z untouched", baseCommissureZ, t5CommissureZ, 1e-6f)
            if (poseName == "Rest(0/0)") {
                assertTrue(
                    "At rest, glans tip ($t5GlansTipZ) and hood apex ($t5HoodApexZ) must stay tucked below outer majora ($t5MajoraZ) & commissure ($t5CommissureZ)",
                    t5GlansTipZ < t5MajoraZ - 0.05f && t5HoodApexZ < t5MajoraZ - 0.03f && t5HoodApexZ < t5CommissureZ - 0.05f
                )
            }
            assertTrue(
                "$poseName Toggle 5 midline slope across [0.08..0.34] must stay smooth (maxT5Slope=$maxT5Slope <= maxBaseSlope=$maxBaseSlope + 0.15)",
                maxT5Slope <= maxBaseSlope + 0.15f
            )

            // All-5 arch & interaction invariants (preserving Toggles 1-4)
            assertEquals("$poseName All-5 arch inverted CCW triangles", 0, all5ArchRep.windingInvertedCcwCount)
            assertEquals("$poseName All-5 arch outer-skirt fold-over steps", 0, all5ArchRep.outerSkirtNonMonotonicSteps)
            assertEquals("$poseName All-5 arch inner-wall fold-over steps", 0, all5ArchRep.innerWallNonMonotonicSteps)
            assertEquals("$poseName All-5 arch midline crossings", 0, all5ArchRep.midlineCrossingVertices)
            assertEquals("$poseName All-5 arch glans interpenetrations", 0, all5ArchRep.glansInterpenetratingVertices)
            assertEquals("$poseName All-5 opaque arch below patch", 0, all5ArchRep.opaqueArchBelowPatchVertices)
            assertEquals("$poseName All-5 visible arch below patch", 0, all5ArchRep.visibleArchBelowPatchVertices)
            assertTrue("$poseName All-5 maxArchOvalR < 0.85, got ${all5ArchRep.maxArchOvalR}", all5ArchRep.maxArchOvalR < 0.85f)

            // C. Toggle 6 ONLY (Steps 1 & 2: floor taper + conservative hood crest height +0.014f)
            val t6OnlyPose = basePose.copy(betaV2BalancedUpperVulvaDepthWithCrest = true)
            scene.update(t6OnlyPose, includeLithotomyBody = true)
            assertValidMesh(scene.blossomVulvaPatchMesh)
            assertValidMesh(scene.glansMesh)
            assertValidMesh(scene.labiaHoodArchMesh)
            val t6PatchData = scene.blossomVulvaPatchMesh.vertexData.copyOf()
            val t6GlansData = scene.glansMesh.vertexData.copyOf()
            val t6ArchData = scene.labiaHoodArchMesh.vertexData.copyOf()
            val t6HoodApexZ = hoodCrestZ(scene.labiaHoodArchMesh)

            // Step 1 on patch & glans must be bit-identical between Toggle 5 and Toggle 6
            assertTrue(
                "$poseName Toggle 6 vulva patch must be bit-identical to Toggle 5 (Step 2 only adjusts hood crest)",
                t5PatchData.contentEquals(t6PatchData)
            )
            var t6ArchMatDiff = 0f
            var t6OuterSkirtLandingDiff = 0f
            for (r in 0 until scene.archRows) {
                for (c in 0 until scene.archCols) {
                    val v = r * scene.archCols + c
                    val b = v * stride
                    for (m in 6..9) {
                        val md = abs(t6ArchData[b + m] - baseArchData[b + m])
                        if (md > t6ArchMatDiff) t6ArchMatDiff = md
                    }
                    if (c == scene.archCols - 1) {
                        val dz = abs(t6ArchData[b + 2] - t5ArchData[b + 2])
                        if (dz > t6OuterSkirtLandingDiff) t6OuterSkirtLandingDiff = dz
                    }
                }
            }
            assertEquals("$poseName Toggle 6 must leave all arch mucosa/wetness/ao/pigment bitwise untouched", 0f, t6ArchMatDiff, 0f)
            assertEquals("$poseName Toggle 6 must leave outer skirt landing (u=1) bitwise identical to Toggle 5", 0f, t6OuterSkirtLandingDiff, 0f)

            // Verify Toggle 5 + Toggle 6 together never double-applies Step 1 (bit-identical to Toggle 6 alone)
            val t5AndT6Pose = basePose.copy(
                betaV2BalancedUpperVulvaDepth = true,
                betaV2BalancedUpperVulvaDepthWithCrest = true
            )
            scene.update(t5AndT6Pose, includeLithotomyBody = true)
            assertTrue("$poseName T5+T6 patch must be bit-identical to T6 alone", t6PatchData.contentEquals(scene.blossomVulvaPatchMesh.vertexData))
            assertTrue("$poseName T5+T6 glans must be bit-identical to T6 alone", t6GlansData.contentEquals(scene.glansMesh.vertexData))
            assertTrue("$poseName T5+T6 arch must be bit-identical to T6 alone", t6ArchData.contentEquals(scene.labiaHoodArchMesh.vertexData))

            // Verify Toggle 6 hood crest delta (+0.014f on top of Toggle 5) and rest tuck clearance
            val step2HoodDelta = t6HoodApexZ - t5HoodApexZ
            assertTrue(
                "$poseName Toggle 6 must add gentle hood crest lift (~+0.014f) on top of Step 1, got $step2HoodDelta",
                step2HoodDelta in 0.012f..0.016f
            )
            if (poseName == "Rest(0/0)") {
                assertTrue(
                    "At rest with Toggle 6, hood apex ($t6HoodApexZ) must stay tucked below outer majora ($t5MajoraZ) & commissure ($t5CommissureZ)",
                    t6HoodApexZ < t5MajoraZ - 0.02f && t6HoodApexZ < t5CommissureZ - 0.035f
                )
            }

            // D. All 6 Beta v2 toggles ON together (Toggles 1-4 + Steps 1 & 2)
            val all6Pose = all5Pose.copy(betaV2BalancedUpperVulvaDepthWithCrest = true)
            scene.update(all6Pose, includeLithotomyBody = true)
            assertValidMesh(scene.blossomVulvaPatchMesh)
            assertValidMesh(scene.glansMesh)
            assertValidMesh(scene.labiaHoodArchMesh)
            val all6ArchRep = inspectLabiaHoodArchDiagnostics(scene, all6Pose, "$poseName [All6BetaV2]")
            val all6HoodApexZ = hoodCrestZ(scene.labiaHoodArchMesh)
            val all6GlansInv = countInvertedCcwTriangles(scene.glansMesh)

            println(
                (
                    "T6_DIAG pose=$poseName hoodCrest(base=%.4f->t5=%.4f->t6=%.4f,step2=%+.4f,all6=%.4f) " +
                        "majoraZ=%.4f commZ=%.4f glansInvDelta=%d archAll6Inv=%d foldOut=%d foldIn=%d " +
                        "midCross=%d glansClip=%d belowVis=%d maxOvalR=%.4f"
                    ).format(
                        baseHoodApexZ, t5HoodApexZ, t6HoodApexZ, step2HoodDelta, all6HoodApexZ,
                        baseMajoraZ, baseCommissureZ, all6GlansInv - baseGlansInv,
                        all6ArchRep.windingInvertedCcwCount, all6ArchRep.outerSkirtNonMonotonicSteps,
                        all6ArchRep.innerWallNonMonotonicSteps, all6ArchRep.midlineCrossingVertices,
                        all6ArchRep.glansInterpenetratingVertices, all6ArchRep.visibleArchBelowPatchVertices,
                        all6ArchRep.maxArchOvalR
                    )
            )

            assertEquals("$poseName All-6 glans must preserve exact baseline triangle orientation", baseGlansInv, all6GlansInv)
            assertEquals("$poseName All-6 arch inverted CCW triangles", 0, all6ArchRep.windingInvertedCcwCount)
            assertEquals("$poseName All-6 arch outer-skirt fold-over steps", 0, all6ArchRep.outerSkirtNonMonotonicSteps)
            assertEquals("$poseName All-6 arch inner-wall fold-over steps", 0, all6ArchRep.innerWallNonMonotonicSteps)
            assertEquals("$poseName All-6 arch midline crossings", 0, all6ArchRep.midlineCrossingVertices)
            assertEquals("$poseName All-6 arch glans interpenetrations", 0, all6ArchRep.glansInterpenetratingVertices)
            assertEquals("$poseName All-6 opaque arch below patch", 0, all6ArchRep.opaqueArchBelowPatchVertices)
            assertEquals("$poseName All-6 visible arch below patch", 0, all6ArchRep.visibleArchBelowPatchVertices)
            assertTrue("$poseName All-6 maxArchOvalR < 0.85, got ${all6ArchRep.maxArchOvalR}", all6ArchRep.maxArchOvalR < 0.85f)
        }
    }

    @Test
    fun vulvaBetaV2Toggle7VestibularConcavityPreservesCanalDepthAndTopology() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val edgeR = scene.getVulvaPatchEdgeRForTest()

        fun countGeometricFlippedHeightfieldTriangles(
            mesh: DynamicGlMesh,
            vertexEdgeR: FloatArray,
            maxEdgeR: Float
        ): Int {
            val data = mesh.vertexData
            val idxBuf = mesh.indexBuffer
            idxBuf.position(0)
            var flipped = 0
            var t = 0
            while (t + 2 < mesh.indexCount) {
                val i0 = idxBuf.get(t).toInt() and 0xFFFF
                val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
                val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
                t += 3
                val triMaxR = max(vertexEdgeR[i0], max(vertexEdgeR[i1], vertexEdgeR[i2]))
                if (triMaxR >= maxEdgeR) continue
                val b0 = i0 * stride
                val b1 = i1 * stride
                val b2 = i2 * stride
                val e1x = data[b1] - data[b0]; val e1y = data[b1 + 1] - data[b0 + 1]
                val e2x = data[b2] - data[b0]; val e2y = data[b2 + 1] - data[b0 + 1]
                if (e1x * e2y - e1y * e2x <= 0f) flipped++
            }
            return flipped
        }

        val testPoses = listOf(
            "Rest(0/0)" to v13BaselineState(),
            "SelfSpread(0.74/0.78)" to v13BaselineState(
                leftOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                rightOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                leftInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                rightInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET
            ),
            "FullSpread(1.0/1.0)" to v13BaselineState(
                leftOuter = 1.0f, rightOuter = 1.0f, leftInner = 1.0f, rightInner = 1.0f
            ),
            "AsymSpread(L1/R0)" to v13BaselineState(
                leftOuter = 1.0f, leftInner = 1.0f, rightOuter = 0.0f, rightInner = 0.0f
            )
        )

        for ((poseName, basePose) in testPoses) {
            scene.update(basePose, includeLithotomyBody = true)
            val baseBodyData = scene.blossomBodyMesh.vertexData.copyOf()
            val basePatchData = scene.blossomVulvaPatchMesh.vertexData.copyOf()
            val baseGlansData = scene.glansMesh.vertexData.copyOf()
            val baseArchData = scene.labiaHoodArchMesh.vertexData.copyOf()

            // 1. Toggle 7 ON at concavity = 1.0f must be exact bit-identical to baseline OFF
            val t7AtOne = basePose.copy(
                betaV2AdjustableVestibularConcavity = true,
                betaV2VestibularConcavity = 1.0f
            )
            scene.update(t7AtOne, includeLithotomyBody = true)
            assertTrue(
                "$poseName Toggle 7 at concavity=1.0 must be bit-identical to baseline patch",
                basePatchData.contentEquals(scene.blossomVulvaPatchMesh.vertexData)
            )
            assertTrue(
                "$poseName Toggle 7 at concavity=1.0 must be bit-identical to baseline glans",
                baseGlansData.contentEquals(scene.glansMesh.vertexData)
            )
            assertTrue(
                "$poseName Toggle 7 at concavity=1.0 must be bit-identical to baseline arch",
                baseArchData.contentEquals(scene.labiaHoodArchMesh.vertexData)
            )

            // 2. Compare baseline vs default concavity=0.45 and flattest concavity=0.0
            val vestCenter = ((basePose.rightInner - basePose.leftInner) * 0.0775f +
                (basePose.rightOuter - basePose.leftOuter) * 0.029f)
            val introX = vestCenter * 0.45f
            val introY = -0.21f
            val baseIntroZ = scene.sampleBodySurfaceZForTest(introX, introY, basePose, includeLithotomy = true)
            // Sample peri-urethral / upper-mid vestibular floor outside the introitus funnel
            val vestMidX = vestCenter + 0.03f
            val vestMidY = 0.04f
            val baseVestMidZ = scene.sampleBodySurfaceZForTest(vestMidX, vestMidY, basePose, includeLithotomy = true)

            val t7MidPose = basePose.copy(
                betaV2AdjustableVestibularConcavity = true,
                betaV2VestibularConcavity = 0.45f
            )
            val t7FlatPose = basePose.copy(
                betaV2AdjustableVestibularConcavity = true,
                betaV2VestibularConcavity = 0.0f
            )

            val midIntroZ = scene.sampleBodySurfaceZForTest(introX, introY, t7MidPose, includeLithotomy = true)
            val flatIntroZ = scene.sampleBodySurfaceZForTest(introX, introY, t7FlatPose, includeLithotomy = true)
            val midVestZ = scene.sampleBodySurfaceZForTest(vestMidX, vestMidY, t7MidPose, includeLithotomy = true)
            val flatVestZ = scene.sampleBodySurfaceZForTest(vestMidX, vestMidY, t7FlatPose, includeLithotomy = true)

            // CRITICAL INVARIANT: Vaginal canal depth at introitus center must be strictly preserved!
            assertEquals(
                "$poseName Toggle 7 (concavity=0.45) must preserve exact vaginal canal depth at introitus",
                baseIntroZ,
                midIntroZ,
                1e-4f
            )
            assertEquals(
                "$poseName Toggle 7 (concavity=0.0) must preserve exact vaginal canal depth at introitus",
                baseIntroZ,
                flatIntroZ,
                1e-4f
            )

            // Surrounding vestibular basin must visibly lift monotonically as concavity decreases (1.0 -> 0.45 -> 0.0)
            assertTrue(
                "$poseName Toggle 7 must lift surrounding vestibular basin monotonically (base=$baseVestMidZ < mid=$midVestZ < flat=$flatVestZ)",
                midVestZ > baseVestMidZ + 0.020f && flatVestZ > midVestZ + 0.015f
            )

            // Verify body mesh, outer seam (edgeR >= 0.72), and material/AO channels remain untouched
            scene.update(t7MidPose, includeLithotomyBody = true)
            assertValidMesh(scene.blossomVulvaPatchMesh)
            assertValidMesh(scene.glansMesh)
            assertValidMesh(scene.labiaHoodArchMesh)
            assertTrue(
                "$poseName Toggle 7 must leave blossomBodyMesh bitwise untouched",
                baseBodyData.contentEquals(scene.blossomBodyMesh.vertexData)
            )
            val t7PatchData = scene.blossomVulvaPatchMesh.vertexData
            var maxSeamDiff = 0f
            var maxMatDiff = 0f
            for (v in 0 until scene.blossomVulvaPatchMesh.vertexCount) {
                val b = v * stride
                if (edgeR[v] >= 0.72f) {
                    val pd = max(
                        abs(t7PatchData[b] - basePatchData[b]),
                        max(abs(t7PatchData[b + 1] - basePatchData[b + 1]), abs(t7PatchData[b + 2] - basePatchData[b + 2]))
                    )
                    if (pd > maxSeamDiff) maxSeamDiff = pd
                }
                for (m in 6..9) {
                    val md = abs(t7PatchData[b + m] - basePatchData[b + m])
                    if (md > maxMatDiff) maxMatDiff = md
                }
            }
            assertEquals("$poseName Toggle 7 must leave outer patch seam (edgeR >= 0.72) bitwise untouched", 0f, maxSeamDiff, 0f)
            assertEquals("$poseName Toggle 7 must leave patch mucosa/wetness/ao/pigment bitwise untouched", 0f, maxMatDiff, 0f)
            assertEquals(
                "$poseName Toggle 7 must keep 0 geometric heightfield winding flips",
                0,
                countGeometricFlippedHeightfieldTriangles(scene.blossomVulvaPatchMesh, edgeR, 0.58f)
            )

            // 3. All 7 Beta v2 toggles ON together (including Toggle 7 at 0.35f) must keep 0 arch defects
            val all7Pose = basePose.copy(
                betaV2SeamOnlyPolyOffset = true,
                betaV2NonInvertingLabiaFold = true,
                betaV2AlignedSulcus = true,
                betaV2NaturalPelvicFinish = true,
                betaV2BalancedUpperVulvaDepth = true,
                betaV2BalancedUpperVulvaDepthWithCrest = true,
                betaV2AdjustableVestibularConcavity = true,
                betaV2VestibularConcavity = 0.35f
            )
            scene.update(all7Pose, includeLithotomyBody = true)
            assertValidMesh(scene.blossomVulvaPatchMesh)
            assertValidMesh(scene.glansMesh)
            assertValidMesh(scene.labiaHoodArchMesh)
            val all7ArchRep = inspectLabiaHoodArchDiagnostics(scene, all7Pose, "$poseName [All7BetaV2]")
            assertEquals("$poseName All-7 arch inverted CCW triangles", 0, all7ArchRep.windingInvertedCcwCount)
            assertEquals("$poseName All-7 arch outer-skirt fold-over steps", 0, all7ArchRep.outerSkirtNonMonotonicSteps)
            assertEquals("$poseName All-7 arch inner-wall fold-over steps", 0, all7ArchRep.innerWallNonMonotonicSteps)
            assertEquals("$poseName All-7 arch midline crossings", 0, all7ArchRep.midlineCrossingVertices)
            assertEquals("$poseName All-7 arch glans interpenetrations", 0, all7ArchRep.glansInterpenetratingVertices)
            assertEquals("$poseName All-7 visible arch below patch", 0, all7ArchRep.visibleArchBelowPatchVertices)
        }
    }

    @Test
    fun lifelikeBetaMicroBehaviorsPreserveInvariantsAndBaselineIdentity() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        // Baseline rest snapshots
        val restState = VulvaDeformationState()
        scene.update(restState, includeLithotomyBody = true)
        val restHead = scene.blossomHeadMesh.vertexData.copyOf()
        val restEyes = scene.blossomEyesMesh.vertexData.copyOf()
        val restHair = scene.blossomHairMesh.vertexData.copyOf()
        val restBody = scene.blossomBodyMesh.vertexData.copyOf()

        // 1. Eye microsaccades: rotate both eyeballs cleanly without disturbing head/hair/body
        scene.update(restState.copy(eyeGazeX = 0.75f, eyeGazeY = -0.50f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomEyesMesh)
        assertOutwardWindingMatchesNormals("blossomEyesMesh(gaze)", scene.blossomEyesMesh)
        assertTrue("Eye microsaccades must leave head mesh bitwise untouched", restHead.contentEquals(scene.blossomHeadMesh.vertexData))
        assertTrue("Eye microsaccades must leave hair mesh bitwise untouched", restHair.contentEquals(scene.blossomHairMesh.vertexData))
        var maxEyeMove = 0f
        for (v in 0 until scene.blossomEyesMesh.vertexCount) {
            val b = v * stride
            val d = sqrt(
                (scene.blossomEyesMesh.vertexData[b] - restEyes[b]).let { it * it } +
                    (scene.blossomEyesMesh.vertexData[b + 1] - restEyes[b + 1]).let { it * it } +
                    (scene.blossomEyesMesh.vertexData[b + 2] - restEyes[b + 2]).let { it * it }
            )
            if (d > maxEyeMove) maxEyeMove = d
        }
        assertTrue("Eye microsaccades must produce subtle bounded eyeball rotation (0.015..0.065), got $maxEyeMove", maxEyeMove in 0.015f..0.065f)

        // 2. Subtle facial expressions (warmth, brow, lipSoft): strictly localized above jaw rim (y > 5.88), never moving neck seam or body
        assertTrue("HeadRigCache smile morph must capture facial vertices", scene.blossomHeadRigCache.smileMorphIndices.size > 40)
        assertTrue("HeadRigCache brow morph must capture brow vertices", scene.blossomHeadRigCache.browMorphIndices.size > 30)
        assertTrue("HeadRigCache lipSoft morph must capture lip vertices", scene.blossomHeadRigCache.lipSoftMorphIndices.size > 20)

        scene.update(
            restState.copy(faceWarmth = 0.85f, faceBrow = 0.70f, faceLipSoft = 0.80f),
            includeLithotomyBody = true
        )
        assertValidMesh(scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(expressions)", scene.blossomHeadMesh)
        assertTrue("Facial expressions must leave body mesh bitwise untouched", restBody.contentEquals(scene.blossomBodyMesh.vertexData))
        var maxFaceDelta = 0f
        var maxNeckSeamDelta = 0f
        for (v in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = v * stride
            val restY = restHead[b + 1]
            val d = sqrt(
                (scene.blossomHeadMesh.vertexData[b] - restHead[b]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 1] - restHead[b + 1]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 2] - restHead[b + 2]).let { it * it }
            )
            if (d > maxFaceDelta) maxFaceDelta = d
            if (restY <= 5.80f && d > maxNeckSeamDelta) maxNeckSeamDelta = d
        }
        assertTrue("Facial expressions must be perceptible yet subtle (0.008..0.045), got $maxFaceDelta", maxFaceDelta in 0.008f..0.045f)
        assertEquals("Facial expressions must leave neck/submental seam (y <= 5.80) bitwise untouched", 0f, maxNeckSeamDelta, 0f)

        // 3. Gentle head repositioning (additive roll + turn + pitch + gaze + expressions combined) & exact reset back to 0f
        scene.update(
            restState.copy(
                headTurn = 0.18f,
                headPitch = 0.12f,
                headRollOffset = -0.38f,
                eyeGazeX = -0.4f,
                eyeGazeY = 0.3f,
                faceWarmth = 0.5f,
                eyeBlink = 0.5f
            ),
            includeLithotomyBody = true
        )
        assertValidMesh(scene.blossomHeadMesh)
        assertValidMesh(scene.blossomEyesMesh)
        assertValidMesh(scene.blossomHairMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(lifelikeCombined)", scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomEyesMesh(lifelikeCombined)", scene.blossomEyesMesh)
        assertOutwardWindingMatchesNormals("blossomHairMesh(lifelikeCombined)", scene.blossomHairMesh)

        scene.update(restState, includeLithotomyBody = true)
        assertTrue("Resetting all Lifelike head/face/eye channels must restore bit-identical head mesh", restHead.contentEquals(scene.blossomHeadMesh.vertexData))
        assertTrue("Resetting all Lifelike head/face/eye channels must restore bit-identical eyes mesh", restEyes.contentEquals(scene.blossomEyesMesh.vertexData))
        assertTrue("Resetting all Lifelike head/face/eye channels must restore bit-identical hair mesh", restHair.contentEquals(scene.blossomHairMesh.vertexData))

        // 4. Subtle shoulder relaxation vertex offset: zero when OFF, localized strictly to shoulders/deltoids (0.0 on breasts/neck/vulva/hands)
        val zeroSh = VulvaGlTextureView.evaluateVertexShoulderRelaxOffset(1.70f, 4.45f, -0.80f, 0f, 0f)
        assertEquals(0f, zeroSh[0], 0f)
        assertEquals(0f, zeroSh[1], 0f)
        assertEquals(0f, zeroSh[2], 0f)

        val rightShoulderOff = VulvaGlTextureView.evaluateVertexShoulderRelaxOffset(
            x = 1.72f, y = 4.50f, z = BlossomBodyBuilder.mattressSurfaceZ(4.50f) + 0.72f,
            shoulderRelaxL = 0.20f, shoulderRelaxR = 0.90f
        )
        val leftShoulderOff = VulvaGlTextureView.evaluateVertexShoulderRelaxOffset(
            x = -1.72f, y = 4.50f, z = BlossomBodyBuilder.mattressSurfaceZ(4.50f) + 0.72f,
            shoulderRelaxL = 0.20f, shoulderRelaxR = 0.90f
        )
        assertTrue("Active shoulder relaxation must gently drop shoulder downward (-Y < -0.03), got ${rightShoulderOff[1]}", rightShoulderOff[1] < -0.03f)
        assertTrue("Active shoulder relaxation must settle softly into cushion (-Z < -0.015), got ${rightShoulderOff[2]}", rightShoulderOff[2] < -0.015f)
        assertTrue("Asymmetric L/R shoulder relaxation must move right side further than left", abs(rightShoulderOff[1]) > abs(leftShoulderOff[1]) * 3f)

        // Verify zero spillover onto breasts, neck, vulva, and forearms
        val breastOff = VulvaGlTextureView.evaluateVertexShoulderRelaxOffset(0.70f, 3.80f, BlossomBodyBuilder.mattressSurfaceZ(3.80f) + 1.25f, 1f, 1f)
        val neckOff = VulvaGlTextureView.evaluateVertexShoulderRelaxOffset(0.25f, 5.10f, BlossomBodyBuilder.mattressSurfaceZ(5.10f) + 0.70f, 1f, 1f)
        val vulvaOff = VulvaGlTextureView.evaluateVertexShoulderRelaxOffset(0.15f, 0.0f, 0.20f, 1f, 1f)
        val forearmOff = VulvaGlTextureView.evaluateVertexShoulderRelaxOffset(2.15f, 1.50f, BlossomBodyBuilder.mattressSurfaceZ(1.50f) + 0.50f, 1f, 1f)
        assertEquals("Shoulder relaxation must NOT touch breasts", 0f, breastOff[1], 0f)
        assertEquals("Shoulder relaxation must NOT touch neck", 0f, neckOff[1], 0f)
        assertEquals("Shoulder relaxation must NOT touch vulva", 0f, vulvaOff[1], 0f)
        assertEquals("Shoulder relaxation must NOT touch forearms", 0f, forearmOff[1], 0f)

        // 5. Deeper breaths & sighs: exact identity on normal cycles (depthMul=1f), noticeably fuller peak on sigh cycles
        for (k in 0 until 32) {
            val ph = k / 32f
            val baseB = VulvaGlTextureView.evaluateBreathingDisplacement(ph, 0f)
            val normB = VulvaGlTextureView.evaluateBreathingDisplacementWithSigh(ph, 0f, isSighCycle = false, depthMultiplier = 1f)
            assertEquals(baseB.first, normB.first, 1e-6f)
            assertEquals(baseB.second, normB.second, 1e-6f)
        }
        var maxBaseChest = 0f
        var maxSighChest = 0f
        var maxBaseAbd = 0f
        var maxSighAbd = 0f
        for (k in 0 until 64) {
            val ph = k / 64f
            val b = VulvaGlTextureView.evaluateBreathingDisplacement(ph, 0f)
            val s = VulvaGlTextureView.evaluateBreathingDisplacementWithSigh(ph, 0f, isSighCycle = true, depthMultiplier = 1.72f)
            if (b.first > maxBaseChest) maxBaseChest = b.first
            if (s.first > maxSighChest) maxSighChest = s.first
            if (b.second > maxBaseAbd) maxBaseAbd = b.second
            if (s.second > maxSighAbd) maxSighAbd = s.second
        }
        assertTrue("Sigh cycle must deepen chest rise cleanly (> 1.6x baseline), got $maxSighChest vs $maxBaseChest", maxSighChest > maxBaseChest * 1.6f)
        assertTrue("Sigh cycle must deepen abdomen rise cleanly (> 1.5x baseline), got $maxSighAbd vs $maxBaseAbd", maxSighAbd > maxBaseAbd * 1.5f)
        assertTrue("Sigh cycle peak must remain anatomically bounded (< 0.22)", maxSighChest < 0.20f && maxSighAbd < 0.22f)
    }

    @Test
    fun lifelikePlusTab3BehaviorsPreserveInvariantsAndBaselineIdentity() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        // Baseline rest snapshots (default VulvaDeformationState has all Tab 3 channels == 0f)
        val restState = VulvaDeformationState()
        assertEquals(0f, restState.eyeBlinkRightOffset, 0f)
        assertEquals(0f, restState.eyelidGazeTrack, 0f)
        assertEquals(0f, restState.swallowPhase, 0f)
        assertEquals(0f, restState.nostrilBreath, 0f)
        assertEquals(0f, restState.livingWarmth, 0f)
        assertEquals(0.30f, restState.betaV2VestibularConcavity, 1e-6f)

        scene.update(restState, includeLithotomyBody = true)
        val restHead = scene.blossomHeadMesh.vertexData.copyOf()
        val restEyes = scene.blossomEyesMesh.vertexData.copyOf()
        val restHair = scene.blossomHairMesh.vertexData.copyOf()
        val restBody = scene.blossomBodyMesh.vertexData.copyOf()

        val rig = scene.blossomHeadRigCache
        assertTrue("blinkVertIsRight must match blinkVertIndices size", rig.blinkVertIsRight.size == rig.blinkVertIndices.size)
        assertTrue("blinkVertIsRight must include both left and right eyelid vertices", rig.blinkVertIsRight.any { it } && rig.blinkVertIsRight.any { !it })
        assertTrue("upperLidLiftIndices must capture upper eyelid vertices", rig.upperLidLiftIndices.size > 20)
        assertTrue("swallowJawIndices must capture submental/jaw vertices", rig.swallowJawIndices.size > 15)
        assertTrue("nostrilMorphIndices must capture nasal alar vertices", rig.nostrilMorphIndices.size > 10)
        assertTrue("throatNeckLocalIndices must capture anterior mid-throat vertices", rig.throatNeckLocalIndices.size > 15)

        // 1. Asymmetric blinking: right-only blink moves x > 0 lids only; left-only blink moves x < 0 lids only
        scene.update(restState.copy(eyeBlink = 0f, eyeBlinkRightOffset = 0.85f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(asymBlinkRight)", scene.blossomHeadMesh)
        var maxRightOnlyMoveL = 0f
        var maxRightOnlyMoveR = 0f
        for (v in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = v * stride
            val d = sqrt(
                (scene.blossomHeadMesh.vertexData[b] - restHead[b]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 1] - restHead[b + 1]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 2] - restHead[b + 2]).let { it * it }
            )
            if (restHead[b] < -0.02f && d > maxRightOnlyMoveL) maxRightOnlyMoveL = d
            if (restHead[b] > 0.02f && d > maxRightOnlyMoveR) maxRightOnlyMoveR = d
        }
        assertTrue("Right-only asymmetric blink must move right eyelid (> 0.015), got $maxRightOnlyMoveR", maxRightOnlyMoveR > 0.015f)
        assertEquals("Right-only asymmetric blink must leave left eyelid (x < -0.02) untouched", 0f, maxRightOnlyMoveL, 1e-6f)

        scene.update(restState.copy(eyeBlink = 0.85f, eyeBlinkRightOffset = -0.85f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(asymBlinkLeft)", scene.blossomHeadMesh)
        var maxLeftOnlyMoveL = 0f
        var maxLeftOnlyMoveR = 0f
        for (v in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = v * stride
            val d = sqrt(
                (scene.blossomHeadMesh.vertexData[b] - restHead[b]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 1] - restHead[b + 1]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 2] - restHead[b + 2]).let { it * it }
            )
            if (restHead[b] < -0.02f && d > maxLeftOnlyMoveL) maxLeftOnlyMoveL = d
            if (restHead[b] > 0.02f && d > maxLeftOnlyMoveR) maxLeftOnlyMoveR = d
        }
        assertTrue("Left-only asymmetric blink must move left eyelid (> 0.015), got $maxLeftOnlyMoveL", maxLeftOnlyMoveL > 0.015f)
        assertEquals("Left-only asymmetric blink must leave right eyelid (x > 0.02) untouched", 0f, maxLeftOnlyMoveR, 1e-6f)

        // 2. Eyelid tracking with vertical gaze (downward & upward) + 0f baseline identity
        scene.update(restState.copy(eyeGazeY = -0.55f, eyelidGazeTrack = 0f), includeLithotomyBody = true)
        assertTrue("With eyelidGazeTrack == 0f, vertical gaze must leave head mesh bitwise untouched", restHead.contentEquals(scene.blossomHeadMesh.vertexData))

        scene.update(restState.copy(eyeGazeY = -0.55f, eyelidGazeTrack = 1f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(eyelidTrackDown)", scene.blossomHeadMesh)
        var maxDownTrack = 0f
        for (v in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = v * stride
            val d = sqrt(
                (scene.blossomHeadMesh.vertexData[b] - restHead[b]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 1] - restHead[b + 1]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 2] - restHead[b + 2]).let { it * it }
            )
            if (d > maxDownTrack) maxDownTrack = d
        }
        assertTrue("Downward eyelid tracking must be subtle yet perceptible (0.004..0.040), got $maxDownTrack", maxDownTrack in 0.004f..0.040f)

        scene.update(restState.copy(eyeGazeY = 0.55f, eyelidGazeTrack = 1f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(eyelidTrackUp)", scene.blossomHeadMesh)
        var maxUpTrack = 0f
        for (v in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = v * stride
            val d = sqrt(
                (scene.blossomHeadMesh.vertexData[b] - restHead[b]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 1] - restHead[b + 1]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 2] - restHead[b + 2]).let { it * it }
            )
            if (d > maxUpTrack) maxUpTrack = d
        }
        assertTrue("Upward eyelid tracking must be subtle yet perceptible (0.002..0.025), got $maxUpTrack", maxUpTrack in 0.002f..0.025f)

        // 3. Subtle swallowing: localized strictly to anterior mid-throat + subtle submental jaw, leaving torso/vulva untouched
        for (phase in floatArrayOf(0.25f, 0.55f)) {
            scene.update(restState.copy(swallowPhase = phase), includeLithotomyBody = true)
            assertValidMesh(scene.blossomBodyMesh)
            assertValidMesh(scene.blossomHeadMesh)
            assertOutwardWindingMatchesNormals("blossomHeadMesh(swallow=$phase)", scene.blossomHeadMesh)
            var maxThroatMove = 0f
            var maxNonThroatBodyMove = 0f
            for (v in 0 until scene.blossomBodyMesh.vertexCount) {
                val b = v * stride
                val rx = restBody[b]
                val ry = restBody[b + 1]
                val d = sqrt(
                    (scene.blossomBodyMesh.vertexData[b] - rx).let { it * it } +
                        (scene.blossomBodyMesh.vertexData[b + 1] - ry).let { it * it } +
                        (scene.blossomBodyMesh.vertexData[b + 2] - restBody[b + 2]).let { it * it }
                )
                if (ry in 5.00f..5.78f && abs(rx) < 0.28f) {
                    if (d > maxThroatMove) maxThroatMove = d
                } else {
                    if (d > maxNonThroatBodyMove) maxNonThroatBodyMove = d
                }
            }
            assertTrue("Swallow phase=$phase must gently animate anterior throat (0.002..0.032), got $maxThroatMove", maxThroatMove in 0.002f..0.032f)
            assertEquals("Swallow phase=$phase must leave non-throat body (breasts/legs/vulva) untouched", 0f, maxNonThroatBodyMove, 1e-6f)
        }

        // Exact restoration at swallowPhase = 0f and 1f
        scene.update(restState.copy(swallowPhase = 1f), includeLithotomyBody = true)
        assertTrue("swallowPhase=1f must restore bit-identical body mesh", restBody.contentEquals(scene.blossomBodyMesh.vertexData))
        assertTrue("swallowPhase=1f must restore bit-identical head mesh", restHead.contentEquals(scene.blossomHeadMesh.vertexData))

        // 4. Breath-linked nasal alar softness: localized strictly to nose alar wings, zero body/neck-seam spillover
        scene.update(restState.copy(nostrilBreath = 1f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("blossomHeadMesh(nostrilBreath)", scene.blossomHeadMesh)
        assertTrue("Nostril breath flare must leave body mesh bitwise untouched", restBody.contentEquals(scene.blossomBodyMesh.vertexData))
        var maxNostrilMove = 0f
        var maxNostrilSeamMove = 0f
        for (v in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = v * stride
            val d = sqrt(
                (scene.blossomHeadMesh.vertexData[b] - restHead[b]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 1] - restHead[b + 1]).let { it * it } +
                    (scene.blossomHeadMesh.vertexData[b + 2] - restHead[b + 2]).let { it * it }
            )
            if (d > maxNostrilMove) maxNostrilMove = d
            if (restHead[b + 1] <= 5.80f && d > maxNostrilSeamMove) maxNostrilSeamMove = d
        }
        assertTrue("Nostril breath flare must be whisper-subtle (0.001..0.015), got $maxNostrilMove", maxNostrilMove in 0.001f..0.015f)
        assertEquals("Nostril breath flare must leave neck seam untouched", 0f, maxNostrilSeamMove, 0f)

        // Reset back to restState restores every single mesh bit-identically
        scene.update(restState, includeLithotomyBody = true)
        assertTrue("Resetting all Tab 3 channels must restore bit-identical head mesh", restHead.contentEquals(scene.blossomHeadMesh.vertexData))
        assertTrue("Resetting all Tab 3 channels must restore bit-identical eyes mesh", restEyes.contentEquals(scene.blossomEyesMesh.vertexData))
        assertTrue("Resetting all Tab 3 channels must restore bit-identical hair mesh", restHair.contentEquals(scene.blossomHairMesh.vertexData))
        assertTrue("Resetting all Tab 3 channels must restore bit-identical body mesh", restBody.contentEquals(scene.blossomBodyMesh.vertexData))

        // 5. Subtle capillary warmth localization (cheeks, ears, upper chest positive; legs/abdomen/vulva/mucosa strictly 0)
        val cheekWarmth = VulvaGlTextureView.evaluateLivingWarmthMask(x = 0.30f, y = 6.32f, nz = 0.85f)
        val earWarmth = VulvaGlTextureView.evaluateLivingWarmthMask(x = 0.64f, y = 6.34f, nz = 0.15f)
        val chestWarmth = VulvaGlTextureView.evaluateLivingWarmthMask(x = 0.35f, y = 4.35f, nz = 0.85f)
        val legWarmth = VulvaGlTextureView.evaluateLivingWarmthMask(x = 1.10f, y = -1.20f, nz = 0.90f)
        val abdomenWarmth = VulvaGlTextureView.evaluateLivingWarmthMask(x = 0.25f, y = 2.20f, nz = 0.90f)
        val vulvaWarmth = VulvaGlTextureView.evaluateLivingWarmthMask(x = 0.10f, y = 0.05f, nz = 0.90f, mucosa = 0.8f)
        val lipMucosaWarmth = VulvaGlTextureView.evaluateLivingWarmthMask(x = 0.30f, y = 6.32f, nz = 0.85f, mucosa = 1.0f)

        assertTrue("Cheek apple warmth must be strongly active (> 0.80), got $cheekWarmth", cheekWarmth > 0.80f)
        assertTrue("Ear warmth must be active (> 0.35), got $earWarmth", earWarmth > 0.35f)
        assertTrue("Upper chest warmth must be gently active (> 0.45), got $chestWarmth", chestWarmth > 0.45f)
        assertEquals("Leg skin must have 0 capillary warmth", 0f, legWarmth, 0f)
        assertEquals("Abdomen skin must have 0 capillary warmth", 0f, abdomenWarmth, 0f)
        assertEquals("Vulvar tissue must have 0 capillary warmth", 0f, vulvaWarmth, 0f)
        assertEquals("Full mucosa must have 0 capillary warmth", 0f, lipMucosaWarmth, 0f)
    }

    @Test
    fun vulvaBetaV3AnatomicalRealismPassPreservesInvariantsAndBaselineIdentity() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val edgeR = scene.getVulvaPatchEdgeRForTest()

        val testPoses = listOf(
            "Rest(0/0)" to VulvaDeformationState(),
            "SelfSpread(0.74/0.78)" to VulvaDeformationState(
                leftOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                rightOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                leftInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                rightInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET
            ),
            "FullSpread(1.0/1.0)" to VulvaDeformationState(
                leftOuter = 1.0f, rightOuter = 1.0f, leftInner = 1.0f, rightInner = 1.0f
            ),
            "HoodRetract(1.0)" to VulvaDeformationState(hoodRetract = 1.0f)
        )

        for ((poseName, basePose) in testPoses) {
            scene.update(basePose, includeLithotomyBody = true)
            val baseBody = scene.blossomBodyMesh.vertexData.copyOf()
            val basePatch = scene.blossomVulvaPatchMesh.vertexData.copyOf()
            val baseArch = scene.labiaHoodArchMesh.vertexData.copyOf()
            val baseGlans = scene.glansMesh.vertexData.copyOf()

            val baseArchAlpha = scene.labiaHoodArchAlphaData.copyOf()

            // Verify each individual Beta v3 toggle (at full 100% = 3x for 1..4) + all 6 combined preserve all topology invariants
            val variants = listOf(
                Triple("V3_Continuity_3x", true, basePose.copy(betaV3OrganicContinuity = true, betaV3ContinuityStrength = 1.0f)),
                Triple("V3_Asymmetry_3x", true, basePose.copy(betaV3AnatomicalAsymmetry = true, betaV3AsymmetryStrength = 1.0f)),
                Triple("V3_RegionalSurface_3x", true, basePose.copy(betaV3RegionalSurfaceDetail = true, betaV3SurfaceStrength = 1.0f)),
                Triple("V3_LabialVestibuleBlend", false, basePose.copy(betaV3LabialVestibuleBlend = true, betaV3LabialBlendStrength = 1.0f)),
                Triple("V3_IntricateLabiaFolds", true, basePose.copy(betaV3IntricateLabiaFolds = true, betaV3LabiaFoldsStrength = 1.0f)),
                Triple(
                    "V3_All6",
                    true,
                    basePose.copy(
                        betaV3OrganicContinuity = true,
                        betaV3ContinuityStrength = 1.0f,
                        betaV3SoftPigmentation = true,
                        betaV3PigmentStrength = 1.0f,
                        betaV3AnatomicalAsymmetry = true,
                        betaV3AsymmetryStrength = 1.0f,
                        betaV3RegionalSurfaceDetail = true,
                        betaV3SurfaceStrength = 1.0f,
                        betaV3LabialVestibuleBlend = true,
                        betaV3LabialBlendStrength = 1.0f,
                        betaV3IntricateLabiaFolds = true,
                        betaV3LabiaFoldsStrength = 1.0f
                    )
                )
            )

            for ((label, modifiesPatchInterior, variantPose) in variants) {
                scene.update(variantPose, includeLithotomyBody = true)
                assertValidMesh(scene.blossomVulvaPatchMesh)
                assertValidMesh(scene.labiaHoodArchMesh)
                assertValidMesh(scene.glansMesh)
                assertTrue(
                    "$poseName $label must leave blossomBodyMesh bitwise untouched",
                    baseBody.contentEquals(scene.blossomBodyMesh.vertexData)
                )

                // Outer patch seam (edgeR >= 0.90) must stay untouched to keep zero body-seam discontinuity
                val vPatch = scene.blossomVulvaPatchMesh.vertexData
                var maxOuterSeamDiff = 0f
                var maxInteriorDiff = 0f
                for (v in 0 until scene.blossomVulvaPatchMesh.vertexCount) {
                    val b = v * stride
                    val d = max(
                        abs(vPatch[b] - basePatch[b]),
                        max(abs(vPatch[b + 1] - basePatch[b + 1]), abs(vPatch[b + 2] - basePatch[b + 2]))
                    )
                    if (edgeR[v] >= 0.90f) {
                        if (d > maxOuterSeamDiff) maxOuterSeamDiff = d
                    } else if (d > maxInteriorDiff) {
                        maxInteriorDiff = d
                    }
                }
                assertEquals("$poseName $label outer patch seam must remain untouched", 0f, maxOuterSeamDiff, 1e-5f)
                if (modifiesPatchInterior) {
                    assertTrue("$poseName $label must produce measurable interior anatomical refinement (> 0.001)", maxInteriorDiff > 0.001f)
                }

                // Verify Option 5 measurably blends medial labial arch materials into vestibule & Option 6 adds arch fold relief + wetness/pigment variation
                val vArch = scene.labiaHoodArchMesh.vertexData
                var maxArchPosDiff = 0f
                var maxArchMatDiff = 0f
                for (v in 0 until scene.labiaHoodArchMesh.vertexCount) {
                    val b = v * stride
                    val pd = max(abs(vArch[b] - baseArch[b]), max(abs(vArch[b + 1] - baseArch[b + 1]), abs(vArch[b + 2] - baseArch[b + 2])))
                    val md = max(
                        max(abs(vArch[b + 6] - baseArch[b + 6]), abs(vArch[b + 7] - baseArch[b + 7])),
                        max(abs(vArch[b + 8] - baseArch[b + 8]), abs(vArch[b + 9] - baseArch[b + 9]))
                    )
                    if (pd > maxArchPosDiff) maxArchPosDiff = pd
                    if (md > maxArchMatDiff) maxArchMatDiff = md
                }
                if (label == "V3_LabialVestibuleBlend") {
                    assertTrue(
                        "$poseName Option 5 must measurably blend medial labial arch materials toward vestibule (> 0.04), got $maxArchMatDiff",
                        maxArchMatDiff > 0.04f
                    )
                }
                if (label == "V3_IntricateLabiaFolds") {
                    assertTrue(
                        "$poseName Option 6 must sculpt measurable wavy border & inner labial fold geometry (> 0.004), got $maxArchPosDiff",
                        maxArchPosDiff > 0.004f
                    )
                    assertTrue(
                        "$poseName Option 6 must vary inner labial wetness/pigmentation across folds (> 0.03), got $maxArchMatDiff",
                        maxArchMatDiff > 0.03f
                    )
                }

                val archRep = inspectLabiaHoodArchDiagnostics(scene, variantPose, "$poseName [$label]")
                assertEquals("$poseName $label arch inverted CCW triangles", 0, archRep.windingInvertedCcwCount)
                assertEquals("$poseName $label arch outer-skirt fold-over steps", 0, archRep.outerSkirtNonMonotonicSteps)
                assertEquals("$poseName $label arch inner-wall fold-over steps", 0, archRep.innerWallNonMonotonicSteps)
                assertEquals("$poseName $label arch midline crossings", 0, archRep.midlineCrossingVertices)
                assertEquals("$poseName $label arch glans interpenetrations", 0, archRep.glansInterpenetratingVertices)
                assertEquals("$poseName $label visible arch below patch", 0, archRep.visibleArchBelowPatchVertices)
                assertTrue("$poseName $label maxArchOvalR < 0.85, got ${archRep.maxArchOvalR}", archRep.maxArchOvalR < 0.85f)
            }

            // Verify Toggle 3 introduces measurable bilateral asymmetry delta relative to baseline on majora
            val probeMajoraX = 0.28f + basePose.rightOuter * 0.16f
            val asymPose = basePose.copy(betaV3AnatomicalAsymmetry = true, betaV3AsymmetryStrength = 1.0f)
            var maxAsymShift = 0f
            for (py in floatArrayOf(-0.10f, 0.04f, 0.18f)) {
                val baseDiff = scene.sampleBodySurfaceZForTest(-probeMajoraX, py, basePose, includeLithotomy = true) -
                    scene.sampleBodySurfaceZForTest(probeMajoraX, py, basePose, includeLithotomy = true)
                val asymDiff = scene.sampleBodySurfaceZForTest(-probeMajoraX, py, asymPose, includeLithotomy = true) -
                    scene.sampleBodySurfaceZForTest(probeMajoraX, py, asymPose, includeLithotomy = true)
                val shift = abs(asymDiff - baseDiff)
                if (shift > maxAsymShift) maxAsymShift = shift
            }
            assertTrue(
                "$poseName Toggle 3 must shift L/R majora asymmetry measurably (> 0.005 at 3x, x=±$probeMajoraX), got maxAsymShift=$maxAsymShift",
                maxAsymShift > 0.005f
            )

            // Verify turning all Beta v3 switches back OFF restores bit-identical live defaults
            scene.update(basePose, includeLithotomyBody = true)
            assertTrue("$poseName patch must revert bit-identically when Beta v3 is OFF", basePatch.contentEquals(scene.blossomVulvaPatchMesh.vertexData))
            assertTrue("$poseName arch must revert bit-identically when Beta v3 is OFF", baseArch.contentEquals(scene.labiaHoodArchMesh.vertexData))
            assertTrue("$poseName arch alpha must revert bit-identically when Beta v3 is OFF", baseArchAlpha.contentEquals(scene.labiaHoodArchAlphaData))
            assertTrue("$poseName glans must revert bit-identically when Beta v3 is OFF", baseGlans.contentEquals(scene.glansMesh.vertexData))
        }

        // Verify Vulva elastic resistance Beta slider (v21.0: 0% = OFF, default +50%, max +60%)
        assertEquals(0.50f, VULVA_ELASTIC_DEFAULT_RESISTANCE, 1e-6f)
        assertEquals(0.60f, VULVA_ELASTIC_MAX_RESISTANCE, 1e-6f)
        assertEquals("At 0% (OFF), pull scale must equal exact baseline 1.0", 1.0f, vulvaElasticPullScale(0f), 1e-6f)
        assertEquals("At 0% (OFF), max spread must equal exact baseline 1.0", 1.0f, vulvaElasticMaxSpread(0f), 1e-6f)
        assertEquals("At 0% (OFF), hood max must equal exact baseline 1.0", 1.0f, vulvaElasticHoodMax(0f), 1e-6f)
        assertEquals("At 0% (OFF), spring stiffness must equal baseline 200f", 200f, vulvaElasticSpringStiffness(0f), 1e-4f)
        assertEquals("At +40% (default), max spread must be reduced by 40% to 0.60", 0.60f, vulvaElasticMaxSpread(0.40f), 1e-5f)
        assertEquals("At +60% (max), max spread must be reduced by 60% to 0.40", 0.40f, vulvaElasticMaxSpread(0.60f), 1e-5f)
        assertTrue(
            "Elastic pull resistance must scale monotonically stiffer from 0% -> 20% -> 40% -> 60%",
            vulvaElasticPullScale(0.60f) < vulvaElasticPullScale(0.40f) &&
                vulvaElasticPullScale(0.40f) < vulvaElasticPullScale(0.20f) &&
                vulvaElasticPullScale(0.20f) < vulvaElasticPullScale(0f) &&
                vulvaElasticPullScale(0.40f) in 0.60f..0.72f &&
                vulvaElasticPullScale(0.60f) in 0.44f..0.55f
        )
        assertTrue(
            "Elastic recoil stiffness must increase monotonically up to +60%",
            vulvaElasticSpringStiffness(0.60f) > vulvaElasticSpringStiffness(0.40f) &&
                vulvaElasticSpringStiffness(0.40f) > vulvaElasticSpringStiffness(0.20f) &&
                vulvaElasticSpringStiffness(0.20f) > vulvaElasticSpringStiffness(0f)
        )
        // Progressive stretch resistance: outward drag near cap moves less than at rest, and never exceeds maxSpread
        val stepNearRest = vulvaElasticStep(0.05f, 0.20f, 0.60f, 0.40f) - 0.05f
        val stepNearCap = vulvaElasticStep(0.45f, 0.20f, 0.60f, 0.40f) - 0.45f
        assertTrue("Progressive elastic resistance should stiffen near cap ($stepNearCap < $stepNearRest)", stepNearCap < stepNearRest)
        assertTrue("Elastic step must never exceed max spread 0.60 at +40%", vulvaElasticStep(0.55f, 0.50f, 0.60f, 0.40f) <= 0.60f + 1e-6f)
        assertTrue("Elastic step must never exceed max spread 0.40 at +60%", vulvaElasticStep(0.35f, 0.50f, 0.40f, 0.60f) <= 0.40f + 1e-6f)
    }

    @Test
    fun breastSizeBoostScalesProportionallyExceptMostPetite() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        fun maxBreastApexZ(mesh: DynamicGlMesh): Float {
            val data = mesh.vertexData
            var best = Float.NEGATIVE_INFINITY
            for (v in 0 until mesh.vertexCount) {
                val z = data[v * stride + 2]
                if (z > best) best = z
            }
            return best
        }

        // 1. Most petite (0.0f and 0.06f) must remain 100% bit-identical even at max +50% breast boost
        for (petiteSize in floatArrayOf(0.0f, 0.06f)) {
            val baseState = VulvaDeformationState(breastSize = petiteSize, betaBreastSizeBoost = 0f)
            scene.update(baseState, includeLithotomyBody = true)
            val baseBreasts = scene.blossomBreastsMesh.vertexData.copyOf()

            scene.update(baseState.copy(betaBreastSizeBoost = 0.50f), includeLithotomyBody = true)
            assertTrue(
                "Breast size boost (+50%) must leave most petite size ($petiteSize) bitwise untouched",
                baseBreasts.contentEquals(scene.blossomBreastsMesh.vertexData)
            )
        }

        // 2. Medium (0.50f) and Full (1.00f) must scale monotonically from +15% up to +50% cleanly with valid outward normals
        scene.update(VulvaDeformationState(breastSize = 0.50f, betaBreastSizeBoost = 0f), includeLithotomyBody = true)
        var chestFloorZ = Float.NEGATIVE_INFINITY
        val bodyData0 = scene.blossomBodyMesh.vertexData
        for (v in 0 until scene.blossomBodyMesh.vertexCount) {
            val b = v * stride
            if (abs(abs(bodyData0[b]) - 0.72f) < 0.14f && abs(bodyData0[b + 1] - 3.75f) < 0.14f) {
                if (bodyData0[b + 2] > chestFloorZ) chestFloorZ = bodyData0[b + 2]
            }
        }
        for (sz in floatArrayOf(0.50f, 1.00f)) {
            val baseState = VulvaDeformationState(breastSize = sz, betaBreastSizeBoost = 0f)
            scene.update(baseState, includeLithotomyBody = true)
            val baseApexZ = maxBreastApexZ(scene.blossomBreastsMesh)
            val baseBody = scene.blossomBodyMesh.vertexData.copyOf()
            val baseProj = (baseApexZ - chestFloorZ).coerceAtLeast(0.05f)

            scene.update(baseState.copy(betaBreastSizeBoost = 0.15f), includeLithotomyBody = true)
            assertValidMesh(scene.blossomBreastsMesh)
            val apex15 = maxBreastApexZ(scene.blossomBreastsMesh)
            val gain15 = ((apex15 - chestFloorZ) - baseProj) / baseProj
            assertTrue(
                "Breast boost +15% at size=$sz must increase projection (+8%..+24%), got ${(gain15 * 100f).toInt()}%",
                gain15 in 0.08f..0.24f
            )

            scene.update(baseState.copy(betaBreastSizeBoost = 0.50f), includeLithotomyBody = true)
            assertValidMesh(scene.blossomBreastsMesh)
            assertOutwardWindingMatchesNormals("blossomBreastsMesh(breastBoost50=$sz)", scene.blossomBreastsMesh)
            val apex50 = maxBreastApexZ(scene.blossomBreastsMesh)
            val gain50 = ((apex50 - chestFloorZ) - baseProj) / baseProj
            assertTrue(
                "Breast boost +50% at size=$sz must increase projection substantially (+35%..+65%) and exceed +15%, got ${(gain50 * 100f).toInt()}%",
                gain50 in 0.35f..0.65f && gain50 > gain15 + 0.22f
            )
            assertTrue(
                "Breast boost +50% at size=$sz must leave blossomBodyMesh bitwise untouched",
                baseBody.contentEquals(scene.blossomBodyMesh.vertexData)
            )
        }
    }

    @Test
    fun heartRateAndCardioRespiratoryCouplingPhysiologicalInvariantsHold() {
        val restBpm = VulvaGlTextureView.RESTING_HEART_RATE_BPM
        val maxBpm = VulvaGlTextureView.MAX_HEART_RATE_BPM
        assertEquals(66f, restBpm, 1e-4f)
        assertEquals(142f, maxBpm, 1e-4f)

        // 1. Sustained regional stimulation drives BPM higher for clitoral glans (1.0) > breasts (0.54) > outer skin (0.20)
        fun simulateSustainedTouch(sensitivity: Float, seconds: Float): Float {
            var bpm = restBpm
            val dt = 0.05f
            val steps = (seconds / dt).toInt()
            for (i in 0 until steps) {
                bpm = VulvaGlTextureView.stepHeartRateBpm(
                    currentBpm = bpm,
                    dtSec = dt,
                    targetSensitivityGain = sensitivity,
                    activeDrive = 1.0f,
                    secondsSinceLastStim = 0f
                )
            }
            return bpm
        }

        // Version 19.0: heart-rate rise is 3× slower (takes 54s to match the prior 18s build)
        assertEquals(3.0f, VulvaGlTextureView.HEART_RATE_RISE_RATE_DIVISOR, 1e-5f)
        val bpmClit18 = simulateSustainedTouch(1.0f, 18f)
        val bpmClit = simulateSustainedTouch(1.0f, 54f)
        val bpmBreast = simulateSustainedTouch(0.54f, 54f)
        val bpmLeg = simulateSustainedTouch(0.20f, 54f)
        assertTrue(
            "3× slower rise must climb gently at 18s (expected < 128 BPM, got $bpmClit18) before reaching high BPM by 54s ($bpmClit)",
            bpmClit18 in (restBpm + 30f)..128f && bpmClit > bpmClit18 + 10f
        )
        assertTrue(
            "Clitoral stimulation BPM ($bpmClit) must exceed breast ($bpmBreast) > outer skin ($bpmLeg) > rest ($restBpm)",
            bpmClit > bpmBreast + 12f && bpmBreast > bpmLeg + 8f && bpmLeg > restBpm + 4f
        )
        assertTrue("BPM must stay strictly bounded <= $maxBpm, got $bpmClit", bpmClit <= maxBpm)

        // 2. Without touch, elevated BPM decays smoothly back toward resting 66 BPM
        var recovered40 = bpmClit
        for (i in 0 until 800) {
            recovered40 = VulvaGlTextureView.stepHeartRateBpm(
                currentBpm = recovered40,
                dtSec = 0.05f,
                targetSensitivityGain = 0f,
                activeDrive = 0f,
                secondsSinceLastStim = i * 0.05f
            )
        }
        assertTrue(
            "Elevated BPM ($bpmClit) must drop substantially after 40s rest (< 78 BPM), got $recovered40",
            recovered40 in restBpm..78f
        )
        var recovered80 = recovered40
        for (i in 800 until 1600) {
            recovered80 = VulvaGlTextureView.stepHeartRateBpm(
                currentBpm = recovered80,
                dtSec = 0.05f,
                targetSensitivityGain = 0f,
                activeDrive = 0f,
                secondsSinceLastStim = i * 0.05f
            )
        }
        assertTrue(
            "Elevated BPM ($bpmClit) must settle right back near resting $restBpm after 80s rest, got $recovered80",
            recovered80 in restBpm..(restBpm + 3f)
        )

        // 3. Breathing frequency scales monotonically with heart rate from resting 0.235 Hz up to 0.520 Hz
        val fRest = VulvaGlTextureView.breathFrequencyHzForHeartRate(restBpm)
        val fMid = VulvaGlTextureView.breathFrequencyHzForHeartRate(104f)
        val fPeak = VulvaGlTextureView.breathFrequencyHzForHeartRate(maxBpm)
        assertEquals(0.235f, fRest, 1e-4f)
        assertTrue("Breathing frequency must rise monotonically with BPM ($fRest < $fMid < $fPeak)", fRest < fMid && fMid < fPeak)
        assertEquals(0.520f, fPeak, 1e-3f)

        // 4. Breathing depth scales monotonically with heart rate while staying anatomically bounded
        fun peakDisplacements(bpm: Float): Pair<Float, Float> {
            var maxChest = 0f
            var maxAbd = 0f
            for (k in 0 until 64) {
                val ph = k / 64f
                val d = VulvaGlTextureView.evaluateHeartRateBreathingDisplacement(
                    phaseCycles = ph,
                    bpm = bpm,
                    stimulus = 0f,
                    isSighCycle = false,
                    depthMultiplier = 1f
                )
                if (d.first > maxChest) maxChest = d.first
                if (d.second > maxAbd) maxAbd = d.second
            }
            return maxChest to maxAbd
        }

        val (restChest, restAbd) = peakDisplacements(restBpm)
        val (midChest, midAbd) = peakDisplacements(104f)
        val (peakChest, peakAbd) = peakDisplacements(maxBpm)

        // At resting 66 BPM, matches baseline resting breathing cleanly
        var baseChest = 0f
        var baseAbd = 0f
        for (k in 0 until 64) {
            val b = VulvaGlTextureView.evaluateBreathingDisplacement(k / 64f, 0f)
            if (b.first > baseChest) baseChest = b.first
            if (b.second > baseAbd) baseAbd = b.second
        }
        assertEquals("At 66 BPM rest, chest peak must match baseline", baseChest, restChest, 1e-4f)
        assertEquals("At 66 BPM rest, abdomen peak must match baseline", baseAbd, restAbd, 1e-4f)

        assertTrue(
            "Chest breathing depth must increase monotonically with heart rate ($restChest < $midChest < $peakChest)",
            midChest > restChest * 1.15f && peakChest > midChest * 1.15f
        )
        assertTrue(
            "Abdomen breathing depth must increase monotonically with heart rate ($restAbd < $midAbd < $peakAbd)",
            midAbd > restAbd * 1.10f && peakAbd > midAbd * 1.10f
        )
        assertTrue(
            "Peak heart-rate breathing displacements must remain anatomically bounded (chest <= 0.195, abd <= 0.215), got ($peakChest, $peakAbd)",
            peakChest <= 0.195f && peakAbd <= 0.215f
        )
    }

    @Test
    fun version19MilestoneDefaultsToggleMemoryAndSelfSpreadElasticCapHold() {
        val session = BlossomViewportSessionState()

        // 1. Verify exact v19.0 Beta Lab defaults across Vulva, Life, and Life+
        assertTrue(session.betaV3OrganicContinuity)
        assertEquals(0.50f, session.betaV3ContinuityStrength, 1e-6f)
        assertTrue(session.betaV3SoftPigmentation)
        assertEquals(0.50f, session.betaV3PigmentStrength, 1e-6f)
        assertTrue(session.betaV3AnatomicalAsymmetry)
        assertEquals(0.85f, session.betaV3AsymmetryStrength, 1e-6f)
        assertTrue(session.betaV3RegionalSurfaceDetail)
        assertEquals(0.36f, session.betaV3SurfaceStrength, 1e-6f)
        assertTrue(session.betaV3LabialVestibuleBlend)
        assertEquals(0.75f, session.betaV3LabialBlendStrength, 1e-6f)
        assertTrue(session.betaV3IntricateLabiaFolds)
        assertEquals(0.65f, session.betaV3LabiaFoldsStrength, 1e-6f)
        assertTrue(session.betaVulvaElasticityEnabled)
        assertEquals(0.50f, session.betaVulvaElasticity, 1e-6f)
        assertEquals(0.50f, session.effectiveVulvaElasticity, 1e-6f)
        assertTrue("Beta Option #7 (Vestibule-labial patch alignment) must be ON by default in v23.0", session.betaV3MedialRootContinuity)
        assertEquals(0.75f, session.betaV3MedialRootStrength, 1e-6f)
        assertTrue(session.betaBreastSizeBoostEnabled)
        assertEquals(0.45f, session.betaBreastSizeBoost, 1e-6f)
        assertEquals(0.45f, session.effectiveBreastSizeBoost, 1e-6f)

        // All 6 Life toggles ON, all 7 Life+ realism toggles ON, Heart Rate HUD OFF
        assertTrue(session.betaLifeEyeMicrosaccades && session.betaLifeDoubleBlinking && session.betaLifeHeadRepositioning)
        assertTrue(session.betaLifeDeepBreathsSighs && session.betaLifeShoulderRelaxation && session.betaLifeFacialExpressions)
        assertTrue(session.betaLife2EyelidTracking && session.betaLife2AsymmetricBlinking && session.betaLife2GazeAnticipation)
        assertTrue(session.betaLife2SubtleSwallowing && session.betaLife2PosturalSettling)
        assertTrue(session.betaLife2BreathFaceCoupling && session.betaLife2LivingWarmth)
        assertFalse("Heart rate overlay must remain OFF by default", session.betaLife2ShowHeartRate)

        // 2. Verify toggle OFF -> ON preserves custom slider values (including after disableAllBetaToggles)
        session.betaV3AsymmetryStrength = 0.72f
        session.betaV3MedialRootStrength = 0.88f
        session.betaVulvaElasticity = 0.55f
        session.betaBreastSizeBoost = 0.32f

        session.betaV3AnatomicalAsymmetry = false
        session.betaV3MedialRootContinuity = true
        session.betaV3MedialRootContinuity = false
        session.betaVulvaElasticityEnabled = false
        session.betaBreastSizeBoostEnabled = false
        assertEquals(0f, session.effectiveVulvaElasticity, 1e-6f)
        assertEquals(0f, session.effectiveBreastSizeBoost, 1e-6f)

        session.betaV3AnatomicalAsymmetry = true
        session.betaV3MedialRootContinuity = true
        session.betaVulvaElasticityEnabled = true
        session.betaBreastSizeBoostEnabled = true
        assertEquals(0.72f, session.betaV3AsymmetryStrength, 1e-6f)
        assertEquals(0.88f, session.betaV3MedialRootStrength, 1e-6f)
        assertEquals(0.55f, session.effectiveVulvaElasticity, 1e-6f)
        assertEquals(0.32f, session.effectiveBreastSizeBoost, 1e-6f)

        session.disableAllBetaToggles()
        assertFalse(session.betaV3AnatomicalAsymmetry)
        assertFalse(session.betaV3MedialRootContinuity)
        assertFalse(session.betaVulvaElasticityEnabled)
        assertFalse(session.betaBreastSizeBoostEnabled)
        assertEquals(0.72f, session.betaV3AsymmetryStrength, 1e-6f)
        assertEquals(0.88f, session.betaV3MedialRootStrength, 1e-6f)
        assertEquals(0.55f, session.betaVulvaElasticity, 1e-6f)
        assertEquals(0.32f, session.betaBreastSizeBoost, 1e-6f)

        // 3. Verify default 50% elastic limit (0.50f max spread) & dynamic IK cap for Blossom's self-spread
        val defaultMaxSpread = vulvaElasticMaxSpread(VULVA_ELASTIC_DEFAULT_RESISTANCE)
        assertEquals(0.50f, defaultMaxSpread, 1e-6f)
        val effectiveSelfOuter = kotlin.math.min(BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET, defaultMaxSpread)
        val effectiveSelfInner = kotlin.math.min(BlossomArmIkRig.SELF_SPREAD_INNER_TARGET, defaultMaxSpread)
        assertEquals(0.50f, effectiveSelfOuter, 1e-6f)
        assertEquals(0.50f, effectiveSelfInner, 1e-6f)
        for (side in floatArrayOf(-1f, 1f)) {
            val sol = BlossomArmIkRig.solveSideArmIk(
                side = side,
                poseProgress = 1.0f,
                liveOuterSpread = effectiveSelfOuter
            )
            val l1 = (sol.elbow - sol.shoulder).len()
            val l2 = (sol.wrist - sol.elbow).len()
            assertEquals("Upper arm length must remain exact at v21.0 spread", BlossomArmIkRig.UPPER_ARM_LEN, l1, 1e-3f)
            assertEquals("Forearm length must remain exact at v21.0 spread", BlossomArmIkRig.FOREARM_LEN, l2, 1e-3f)
        }

        // 4. Verify the full default configuration produces zero mesh defects at rest & self-spread
        val scene = VulvaAnatomySceneMesh()
        val v21Rest = VulvaDeformationState(
            betaV3OrganicContinuity = true,
            betaV3ContinuityStrength = 0.50f,
            betaV3SoftPigmentation = true,
            betaV3PigmentStrength = 0.50f,
            betaV3AnatomicalAsymmetry = true,
            betaV3AsymmetryStrength = 0.85f,
            betaV3RegionalSurfaceDetail = true,
            betaV3SurfaceStrength = 0.36f,
            betaV3LabialVestibuleBlend = true,
            betaV3LabialBlendStrength = 0.75f,
            betaV3IntricateLabiaFolds = true,
            betaV3LabiaFoldsStrength = 0.65f,
            betaBreastSizeBoost = 0.45f
        )
        val v21Spread = v21Rest.copy(
            leftOuter = effectiveSelfOuter,
            rightOuter = effectiveSelfOuter,
            leftInner = effectiveSelfInner,
            rightInner = effectiveSelfInner,
            selfSpreadPose = 1.0f
        )
        for ((label, st) in listOf("v21Rest" to v21Rest, "v21Spread" to v21Spread)) {
            scene.update(st, includeLithotomyBody = true)
            assertValidMesh(scene.blossomVulvaPatchMesh)
            assertValidMesh(scene.labiaHoodArchMesh)
            assertValidMesh(scene.glansMesh)
            assertValidMesh(scene.blossomBreastsMesh)
            val diag = inspectLabiaHoodArchDiagnostics(scene, st, label)
            assertEquals("$label: 0 inverted CCW triangles", 0, diag.windingInvertedCcwCount)
            assertEquals("$label: 0 outer skirt fold-overs", 0, diag.outerSkirtNonMonotonicSteps)
            assertEquals("$label: 0 inner wall fold-overs", 0, diag.innerWallNonMonotonicSteps)
            assertEquals("$label: 0 midline crossings", 0, diag.midlineCrossingVertices)
            assertEquals("$label: 0 glans clips", 0, diag.glansInterpenetratingVertices)
            assertEquals("$label: 0 visible arch below patch", 0, diag.visibleArchBelowPatchVertices)
        }
    }

    @Test
    fun betaOption7MedialRootContinuityFixesRootMaterialWithoutAlteringGeometryOrOuterZones() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val baseState = VulvaDeformationState(
            leftOuter = 0.50f,
            rightOuter = 0.50f,
            leftInner = 0.50f,
            rightInner = 0.50f,
            betaV3OrganicContinuity = true,
            betaV3ContinuityStrength = 0.50f,
            betaV3SoftPigmentation = true,
            betaV3PigmentStrength = 0.50f,
            betaV3AnatomicalAsymmetry = true,
            betaV3AsymmetryStrength = 0.85f,
            betaV3RegionalSurfaceDetail = true,
            betaV3SurfaceStrength = 0.36f,
            betaV3LabialVestibuleBlend = true,
            betaV3LabialBlendStrength = 0.75f,
            betaV3IntricateLabiaFolds = true,
            betaV3LabiaFoldsStrength = 0.65f,
            betaV3MedialRootContinuity = false,
            betaV3MedialRootStrength = 0.75f
        )
        scene.update(baseState, includeLithotomyBody = true)
        val baseArch = scene.labiaHoodArchMesh.vertexData.copyOf()
        val basePatch = scene.blossomVulvaPatchMesh.vertexData.copyOf()
        val baseGlans = scene.glansMesh.vertexData.copyOf()

        val opt7State = baseState.copy(betaV3MedialRootContinuity = true, betaV3MedialRootStrength = 0.75f)
        scene.update(opt7State, includeLithotomyBody = true)
        val opt7Arch = scene.labiaHoodArchMesh.vertexData.copyOf()
        val opt7Patch = scene.blossomVulvaPatchMesh.vertexData.copyOf()

        // 1. Vulva patch geometry & normals (x, y, z, nx, ny, nz) must remain 100% bit-identical,
        //    and outer labia majora / mons / perineum skin (outside the labial attachment footprint) must have 0 pink bleed!
        var maxPatchMucLift = 0f
        var maxOuterSkinMucDiff = 0f
        for (i in 0 until scene.blossomVulvaPatchMesh.vertexCount) {
            val b = i * stride
            for (k in 0..5) {
                assertEquals("Patch vertex $i field $k must be bit-identical", basePatch[b + k], opt7Patch[b + k], 0f)
            }
            val px = opt7Patch[b]
            val py = opt7Patch[b + 1]
            val mucDiff = opt7Patch[b + 6] - basePatch[b + 6]
            if (mucDiff > maxPatchMucLift) maxPatchMucLift = mucDiff
            if (abs(px) >= 0.32f || py >= 0.34f || py <= -0.52f) {
                val absDiff = abs(mucDiff)
                if (absDiff > maxOuterSkinMucDiff) maxOuterSkinMucDiff = absDiff
            }
        }
        assertTrue(
            "Option 7 must genuinely lift underlying vulva-patch mucosa along the labial root (got maxPatchMucLift=$maxPatchMucLift)",
            maxPatchMucLift > 0.35f
        )
        assertEquals(
            "Option 7 must not bleed mucosa onto outer labia majora, mons, or perineum skin",
            0f,
            maxOuterSkinMucDiff,
            1e-5f
        )

        // 2. Verify exposed pale rows right at & medial to the visible labial root drop to 0 in BOTH Rest(0) and Self-Spread(0.50),
        //    and also verify Option 7 works cleanly even when Option 5 (betaV3LabialVestibuleBlend) is OFF!
        val pCols = BlossomSdfBodyBuilder.VULVA_PATCH_COLS
        val pRows = BlossomSdfBodyBuilder.VULVA_PATCH_ROWS
        for (testCase in listOf(
            "Rest" to baseState.copy(leftOuter = 0f, rightOuter = 0f, leftInner = 0f, rightInner = 0f, betaV3MedialRootContinuity = true, betaV3MedialRootStrength = 0.75f),
            "SelfSpread" to opt7State,
            "Opt5Off" to opt7State.copy(betaV3LabialVestibuleBlend = false)
        )) {
            val st = testCase.second
            scene.update(st, includeLithotomyBody = true)
            val pData = scene.blossomVulvaPatchMesh.vertexData
            val aData = scene.labiaHoodArchMesh.vertexData
            val aAlpha = scene.labiaHoodArchAlphaData
            fun sampleNearestPatch(tx: Float, ty: Float): FloatArray {
                var bestIdx = 0
                var bestD2 = Float.POSITIVE_INFINITY
                for (i in 0 until pCols * pRows) {
                    val b = i * stride
                    val dx = pData[b] - tx
                    val dy = pData[b + 1] - ty
                    val d2 = dx * dx + dy * dy
                    if (d2 < bestD2) {
                        bestD2 = d2
                        bestIdx = i
                    }
                }
                val b = bestIdx * stride
                return floatArrayOf(pData[b + 2], pData[b + 6])
            }

            var exposedPaleRows = 0
            var minRootPatchMuc = 1f
            var minRootArchMuc = 1f
            for (r in 0 until scene.archRows) {
                val arcS = (r.toFloat() / (scene.archRows - 1)) * 2f - 1f
                if (abs(arcS) < 0.18f || abs(arcS) > 0.90f) continue
                for (c in 0 until scene.archCols) {
                    val vIdx = r * scene.archCols + c
                    val b = vIdx * stride
                    val px = aData[b]
                    val py = aData[b + 1]
                    val pAtRoot = sampleNearestPatch(px, py)
                    if (aData[b + 2] >= pAtRoot[0] && aAlpha[vIdx] >= 0.50f) {
                        val pxMed = px - kotlin.math.sign(px) * 0.010f
                        val pMed = sampleNearestPatch(pxMed, py)
                        if (pAtRoot[1] < 0.70f || pMed[1] < 0.70f) exposedPaleRows++
                        if (pAtRoot[1] < minRootPatchMuc) minRootPatchMuc = pAtRoot[1]
                        if (aData[b + 6] < minRootArchMuc) minRootArchMuc = aData[b + 6]
                        break
                    }
                }
            }
            assertEquals("${testCase.first}: Option 7 must eliminate all exposed pale-skin medial root rows", 0, exposedPaleRows)
            assertTrue("${testCase.first}: minRootPatchMuc ($minRootPatchMuc) must stay >= 0.70", minRootPatchMuc >= 0.70f)
            assertTrue("${testCase.first}: minRootArchMuc ($minRootArchMuc) must stay >= 0.70", minRootArchMuc >= 0.70f)

            val diag = inspectLabiaHoodArchDiagnostics(scene, st, "Option7_${testCase.first}")
            assertEquals(0, diag.windingInvertedCcwCount)
            assertEquals(0, diag.outerSkirtNonMonotonicSteps)
            assertEquals(0, diag.innerWallNonMonotonicSteps)
            assertEquals(0, diag.midlineCrossingVertices)
            assertEquals(0, diag.glansInterpenetratingVertices)
            assertEquals(0, diag.visibleArchBelowPatchVertices)
        }

        // 3. Exact bit-identical reversion when turned OFF
        scene.update(baseState, includeLithotomyBody = true)
        assertTrue("Turning Option 7 OFF must restore arch bit-identically", baseArch.contentEquals(scene.labiaHoodArchMesh.vertexData))
        assertTrue("Turning Option 7 OFF must restore patch bit-identically", basePatch.contentEquals(scene.blossomVulvaPatchMesh.vertexData))
        assertTrue("Turning Option 7 OFF must restore glans bit-identically", baseGlans.contentEquals(scene.glansMesh.vertexData))
    }

    @Test
    fun peakExertionBackwardHeadPitchAndPostPeakRecoveryHoldInvariants() {
        // 1. Verify dramatic negative headPitch (-1f lean-back) with large erratic headTurn/roll produces valid outward meshes with hair above mattress
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        for (turnSign in floatArrayOf(-1f, 1f)) {
            val leanBackState = VulvaDeformationState(
                headPitch = -1.0f,
                headTurn = turnSign * 0.72f,
                headRollOffset = -turnSign * 0.17f
            )
            scene.update(leanBackState, includeLithotomyBody = true)
            assertValidMesh(scene.blossomHeadMesh)
            assertValidMesh(scene.blossomEyesMesh)
            assertValidMesh(scene.blossomHairMesh)
            assertValidMesh(scene.blossomBodyMesh)
            assertOutwardWindingMatchesNormals("Head(dramaticLeanBack_$turnSign)", scene.blossomHeadMesh)
            val hairData = scene.blossomHairMesh.vertexData
            for (i in 0 until scene.blossomHairMesh.vertexCount) {
                val y = hairData[i * stride + 1]
                val z = hairData[i * stride + 2]
                val minMatZ = BlossomBodyBuilder.mattressSurfaceZ(y) + 0.025f
                assertTrue("Hair vertex $i at y=$y must stay above mattress ($z >= $minMatZ) during dramatic lean-back", z >= minMatZ)
            }
        }

        // 2. Verify tight thigh-squeeze slam (-0.84f spread + -1.12f knee squeeze) pulls thighs & knees
        //    flush together with valid outward meshes and ZERO midline interpenetration across body, toes, and nails
        scene.update(VulvaDeformationState(leftLegSpread = 0f, rightLegSpread = 0f), includeLithotomyBody = true)
        val restBody = scene.blossomBodyMesh.vertexData.copyOf()
        val restToes = scene.blossomToesMesh.vertexData.copyOf()
        val restNails = scene.blossomNailsMesh.vertexData.copyOf()
        val restHead = scene.blossomHeadMesh.vertexData.copyOf()
        val restHands = scene.blossomHandsMesh.vertexData.copyOf()
        val restPalms = scene.blossomPalmsMesh.vertexData.copyOf()

        scene.update(VulvaDeformationState(leftLegSpread = -0.30f, rightLegSpread = -0.30f), includeLithotomyBody = true)
        val manualMinToeX = run {
            val td = scene.blossomToesMesh.vertexData
            var minPosX = Float.POSITIVE_INFINITY
            for (i in 0 until scene.blossomToesMesh.vertexCount) {
                val x = td[i * stride]
                if (x > 0f && x < minPosX) minPosX = x
            }
            minPosX
        }
        scene.update(
            VulvaDeformationState(
                leftLegSpread = com.example.ui.components.PEAK_LEG_SLAM_SHUT_TARGET,
                rightLegSpread = com.example.ui.components.PEAK_LEG_SLAM_SHUT_TARGET
            ),
            includeLithotomyBody = true
        )
        fun minRightInnerKneeX(bodyData: FloatArray, count: Int): Float {
            var minPosX = Float.POSITIVE_INFINITY
            for (i in 0 until count) {
                val b = i * stride
                val x = bodyData[b]
                val y = bodyData[b + 1]
                if (y in -2.5f..-1.2f && x > 0f && x < minPosX) minPosX = x
            }
            return minPosX
        }
        val rigidSlamKneeX = minRightInnerKneeX(scene.blossomBodyMesh.vertexData, scene.blossomBodyMesh.vertexCount)

        val slamSqueezeState = VulvaDeformationState(
            leftLegSpread = com.example.ui.components.PEAK_LEG_SLAM_SHUT_TARGET,
            rightLegSpread = com.example.ui.components.PEAK_LEG_SLAM_SHUT_TARGET,
            leftKneeShift = com.example.ui.components.PEAK_KNEE_SLAM_SQUEEZE,
            rightKneeShift = com.example.ui.components.PEAK_KNEE_SLAM_SQUEEZE
        )
        for (st in listOf(
            slamSqueezeState,
            VulvaDeformationState(
                leftLegSpread = BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD,
                rightLegSpread = BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD,
                leftKneeShift = -1.15f,
                rightKneeShift = -1.15f
            ),
            VulvaDeformationState(
                leftLegSpread = com.example.ui.components.PEAK_LEG_SLAM_SHUT_TARGET,
                rightLegSpread = com.example.ui.components.PEAK_PRE_EVENT_WIDE_STANCE,
                leftKneeShift = -1.05f,
                rightKneeShift = 0.80f
            )
        )) {
            scene.update(st, includeLithotomyBody = true)
            assertValidMesh(scene.blossomBodyMesh)
            assertValidMesh(scene.blossomToesMesh)
            assertValidMesh(scene.blossomNailsMesh)
            assertOutwardWindingMatchesNormals("Body(peakLegs_${st.leftLegSpread}_${st.leftKneeShift})", scene.blossomBodyMesh)
            val td = scene.blossomToesMesh.vertexData
            var minRightToeX = Float.POSITIVE_INFINITY
            var maxLeftToeX = Float.NEGATIVE_INFINITY
            for (i in 0 until scene.blossomToesMesh.vertexCount) {
                val x = td[i * stride]
                if (x >= 0f && x < minRightToeX) minRightToeX = x
                if (x < 0f && x > maxLeftToeX) maxLeftToeX = x
            }
            assertTrue("Right toes must not cross midline (minRightToeX=$minRightToeX)", minRightToeX > 0.02f)
            assertTrue("Left toes must not cross midline (maxLeftToeX=$maxLeftToeX)", maxLeftToeX < -0.02f)
            if (st === slamSqueezeState) {
                val squeezedKneeX = minRightInnerKneeX(scene.blossomBodyMesh.vertexData, scene.blossomBodyMesh.vertexCount)
                assertTrue(
                    "Slam-shut toes ($minRightToeX) must draw dramatically closer than manual -0.30f ($manualMinToeX)",
                    minRightToeX < manualMinToeX - 0.45f
                )
                assertTrue(
                    "Thigh-squeeze slam knee X ($squeezedKneeX) must press much closer than rigid spread alone ($rigidSlamKneeX) without crossing midline",
                    squeezedKneeX > 0.02f && squeezedKneeX < 0.14f && squeezedKneeX < rigidSlamKneeX - 0.25f
                )
            }
        }

        // 2b. Verify BONUS #1: Planted-foot knee/upper-leg shifting moves knees substantially while keeping feet 100% planted
        scene.update(
            VulvaDeformationState(leftLegSpread = 0f, rightLegSpread = 0f, leftKneeShift = -0.90f, rightKneeShift = 0.85f),
            includeLithotomyBody = true
        )
        assertValidMesh(scene.blossomBodyMesh)
        assertOutwardWindingMatchesNormals("Body(plantedFootKneeShift)", scene.blossomBodyMesh)
        var maxPlantedToeDelta = 0f
        for (i in 0 until scene.blossomToesMesh.vertexCount * stride) {
            val d = abs(scene.blossomToesMesh.vertexData[i] - restToes[i])
            if (d > maxPlantedToeDelta) maxPlantedToeDelta = d
        }
        assertEquals("Planted-foot knee shifting must leave toes/feet 100% planted", 0f, maxPlantedToeDelta, 1e-6f)
        var maxKneeDelta = 0f
        for (i in 0 until scene.blossomBodyMesh.vertexCount) {
            val b = i * stride
            if (restBody[b + 1] in -2.4f..-1.8f) {
                val dx = abs(scene.blossomBodyMesh.vertexData[b] - restBody[b])
                if (dx > maxKneeDelta) maxKneeDelta = dx
            }
        }
        assertTrue("Planted-foot knee shift must visibly displace knees/upper legs (maxKneeDelta=$maxKneeDelta)", maxKneeDelta > 0.35f)

        // 2c. Verify BONUS #2 (Mouth Opening) & BONUS #3 (Subtle Toe Wiggle, Finger Curl, Arm Shift) + exact 0f reversion
        scene.update(
            VulvaDeformationState(
                mouthOpen = 1.0f,
                leftToeWiggle = 0.78f,
                rightToeWiggle = -0.68f,
                leftFingerCurl = 0.80f,
                rightFingerCurl = 0.55f,
                leftArmShift = 0.60f,
                rightArmShift = -0.50f
            ),
            includeLithotomyBody = true
        )
        assertValidMesh(scene.blossomHeadMesh)
        assertValidMesh(scene.blossomBodyMesh)
        assertValidMesh(scene.blossomToesMesh)
        assertValidMesh(scene.blossomNailsMesh)
        assertValidMesh(scene.blossomHandsMesh)
        assertValidMesh(scene.blossomPalmsMesh)
        assertOutwardWindingMatchesNormals("Head(mouthOpen)", scene.blossomHeadMesh)
        assertOutwardWindingMatchesNormals("Hands(microCurlShift)", scene.blossomHandsMesh)

        var maxHeadMouthDy = 0f
        var minOpenMouthAo = 1f
        for (i in 0 until scene.blossomHeadMesh.vertexCount) {
            val b = i * stride
            val dy = abs(scene.blossomHeadMesh.vertexData[b + 1] - restHead[b + 1])
            if (dy > maxHeadMouthDy) maxHeadMouthDy = dy
            if (dy > 0.01f && scene.blossomHeadMesh.vertexData[b + 8] < minOpenMouthAo) {
                minOpenMouthAo = scene.blossomHeadMesh.vertexData[b + 8]
            }
        }
        assertTrue("Mouth opening must visibly part lips/jaw (maxHeadMouthDy=$maxHeadMouthDy)", maxHeadMouthDy > 0.045f)
        assertTrue("Mouth opening must shade inner oral cavity darker (minOpenMouthAo=$minOpenMouthAo)", minOpenMouthAo < 0.25f)

        var maxToeWiggleDisp = 0f
        for (i in 0 until scene.blossomToesMesh.vertexCount) {
            val b = i * stride
            val d = kotlin.math.hypot(
                scene.blossomToesMesh.vertexData[b + 1] - restToes[b + 1],
                scene.blossomToesMesh.vertexData[b + 2] - restToes[b + 2]
            )
            if (d > maxToeWiggleDisp) maxToeWiggleDisp = d
            assertTrue(
                "Wiggling toe $i must stay above mattress",
                scene.blossomToesMesh.vertexData[b + 2] >= BlossomBodyBuilder.mattressSurfaceZ(scene.blossomToesMesh.vertexData[b + 1]) + 0.01f
            )
        }
        assertTrue("Toe wiggle must visibly animate toes (maxToeWiggleDisp=$maxToeWiggleDisp)", maxToeWiggleDisp > 0.025f)

        var maxHandMicroDisp = 0f
        for (i in 0 until scene.blossomHandsMesh.vertexCount) {
            val b = i * stride
            val dx = scene.blossomHandsMesh.vertexData[b] - restHands[b]
            val dy = scene.blossomHandsMesh.vertexData[b + 1] - restHands[b + 1]
            val dz = scene.blossomHandsMesh.vertexData[b + 2] - restHands[b + 2]
            val d = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (d > maxHandMicroDisp) maxHandMicroDisp = d
        }
        assertTrue("Subtle arm shift + finger curl must visibly animate hands (maxHandMicroDisp=$maxHandMicroDisp)", maxHandMicroDisp > 0.05f)

        for (side in floatArrayOf(-1f, 1f)) {
            val sol = BlossomArmIkRig.solveSideArmIk(
                side = side,
                poseProgress = 0f,
                liveOuterSpread = 0f,
                armMicroShift = if (side < 0f) 0.60f else -0.50f,
                fingerMicroCurl = 0.75f
            )
            assertEquals("Upper arm length must remain exact during subtle arm shift", BlossomArmIkRig.UPPER_ARM_LEN, (sol.elbow - sol.shoulder).len(), 1e-3f)
            assertEquals("Forearm length must remain exact during subtle arm shift", BlossomArmIkRig.FOREARM_LEN, (sol.wrist - sol.elbow).len(), 1e-3f)
        }

        // Exact bit-identical reversion when all 9 expressive channels return to 0f
        scene.update(VulvaDeformationState(), includeLithotomyBody = true)
        assertTrue("Body must restore bit-identically when expressive peak channels return to 0", restBody.contentEquals(scene.blossomBodyMesh.vertexData))
        assertTrue("Toes must restore bit-identically when toe wiggle returns to 0", restToes.contentEquals(scene.blossomToesMesh.vertexData))
        assertTrue("All 20 nails must restore bit-identically when toe/finger channels return to 0", restNails.contentEquals(scene.blossomNailsMesh.vertexData))
        assertTrue("Head must restore bit-identically when mouthOpen returns to 0", restHead.contentEquals(scene.blossomHeadMesh.vertexData))
        assertTrue("Hands must restore bit-identically when arm/finger channels return to 0", restHands.contentEquals(scene.blossomHandsMesh.vertexData))
        assertTrue("Palms must restore bit-identically when arm/finger channels return to 0", restPalms.contentEquals(scene.blossomPalmsMesh.vertexData))

        // 3. Verify 136 BPM trigger, 8s pre-event autonomous 136->152 BPM climb, 16s peak, 4s eye closure,
        //    and desynchronized higher-frequency restless pre-event legs trending closer -> wide stance
        assertEquals(136f, com.example.ui.components.PEAK_EPISODE_TRIGGER_BPM, 1e-4f)
        assertEquals(8_000L, com.example.ui.components.PEAK_PRE_EVENT_DURATION_MS)
        assertEquals(152f, com.example.ui.components.PEAK_PRE_EVENT_TARGET_BPM, 1e-4f)
        assertEquals(16_000L, com.example.ui.components.PEAK_EPISODE_DURATION_MS)
        assertEquals(4_000L, com.example.ui.components.PEAK_EYELID_CLOSE_DURATION_MS)
        assertEquals(125f, com.example.ui.components.PEAK_RECOVERY_FLOOR_BPM, 1e-4f)
        assertEquals(136f, com.example.ui.components.computePreEventHeartRateBpm(136f, 0L), 1e-3f)
        assertEquals(144f, com.example.ui.components.computePreEventHeartRateBpm(136f, 4_000L), 1e-2f)
        assertEquals(152f, com.example.ui.components.computePreEventHeartRateBpm(136f, 8_000L), 1e-3f)

        var minPreLeft = 1f
        var minPreRight = 1f
        var earlySum = 0f
        var earlyCount = 0
        var lateSum = 0f
        var lateCount = 0
        var maxLRDiff = 0f
        var leftReversals = 0
        var prevL = 1.0f
        var prevDir = 0f
        for (step in 0..240) {
            val u = step / 240f
            val (l, r) = com.example.ui.components.computePreEventLegTargets(1.0f, 1.0f, u, dirSign = 1f)
            if (l < minPreLeft) minPreLeft = l
            if (r < minPreRight) minPreRight = r
            val diff = abs(l - r)
            if (diff > maxLRDiff) maxLRDiff = diff
            if (u in 0.14f..0.34f) { earlySum += (l + r) * 0.5f; earlyCount++ }
            if (u in 0.66f..0.92f) { lateSum += (l + r) * 0.5f; lateCount++ }
            if (step > 0 && u in 0.12f..0.84f) {
                val dl = l - prevL
                if (abs(dl) > 0.002f) {
                    val curDir = if (dl > 0f) 1f else -1f
                    if (prevDir != 0f && curDir != prevDir) leftReversals++
                    prevDir = curDir
                }
            }
            prevL = l
        }
        val earlyAvg = earlySum / earlyCount
        val lateAvg = lateSum / lateCount
        assertTrue("Pre-event legs from max wide must draw tighter than manual -0.30f (minL=$minPreLeft, minR=$minPreRight)", minPreLeft < -0.45f && minPreRight < -0.45f)
        assertTrue("Pre-event legs must trend from closer together ($earlyAvg) to further apart ($lateAvg)", lateAvg > earlyAvg + 0.45f)
        assertTrue("Pre-event L/R legs must move desynchronized (maxLRDiff=$maxLRDiff)", maxLRDiff > 0.25f)
        assertTrue("Pre-event legs must include higher-frequency restless movements (leftReversals=$leftReversals)", leftReversals >= 8)
        val (endL, endR) = com.example.ui.components.computePreEventLegTargets(1.0f, 1.0f, 1.0f, dirSign = 1f)
        assertEquals(com.example.ui.components.PEAK_PRE_EVENT_WIDE_STANCE, endL, 1e-3f)
        assertEquals(com.example.ui.components.PEAK_PRE_EVENT_WIDE_STANCE, endR, 1e-3f)
        val (endKneeL, endKneeR) = com.example.ui.components.computePreEventKneeShiftTargets(1.0f, dirSign = 1f)
        assertEquals(0f, endKneeL, 1e-4f)
        assertEquals(0f, endKneeR, 1e-4f)

        // 3b. Verify v24.0 refined post-squeeze leg pulses: smaller partial reopenings, staggered delayed
        //     knee returns, unequal L/R travel, two strong later surges (non-monotonic), and full
        //     mesh compatibility while "Ask Blossom to Spread" (selfSpreadPose = 1.0f) is active
        for (seed in longArrayOf(101L, 2024L, 98765L)) {
            val pulses = com.example.ui.components.buildLaterPeakLegPulses(kotlin.random.Random(seed))
            assertTrue("Must generate at least 5 varied post-squeeze pulses", pulses.size >= 5)
            val smallReopenCount = pulses.count { it.isSmallReopening }
            val strongSurgeCount = pulses.count { it.isStrongSurge }
            assertTrue("Must include smaller partial reopenings ($smallReopenCount)", smallReopenCount >= 2)
            assertTrue("Must include 2 strong later surges ($strongSurgeCount)", strongSurgeCount >= 2)
            // Verify strong later pulse is deeper than an earlier moderate pulse (not a predictable even fade)
            val firstSmallClose = pulses.first { it.isSmallReopening }.let { kotlin.math.min(it.closeKneeL, it.closeKneeR) }
            val lateStrongClose = pulses.last { it.isStrongSurge }.let { kotlin.math.min(it.closeKneeL, it.closeKneeR) }
            assertTrue("Strong later surge ($lateStrongClose) must squeeze deeper than smaller earlier pulse ($firstSmallClose)", lateStrongClose < firstSmallClose - 0.05f)
            // Verify staggered return timing & unequal left/right travel distances
            val delayedReturns = pulses.count { kotlin.math.abs(it.closeLagLMs - it.closeLagRMs) >= 120L }
            assertTrue("Must stagger knee return starts across later pulses ($delayedReturns)", delayedReturns >= 4)
            val asymOpenings = pulses.count { kotlin.math.abs(it.openKneeL - it.openKneeR) >= 0.14f }
            assertTrue("Left and right knees must cover unequal distances on reopenings ($asymOpenings)", asymOpenings >= 4)
            // Verify combined with active "Ask Blossom to Spread" (selfSpreadPose = 1f, outer/inner = 0.50f)
            val samplePulse = pulses[2] // Strong later surge
            scene.update(
                VulvaDeformationState(
                    leftOuter = 0.50f,
                    rightOuter = 0.50f,
                    leftInner = 0.50f,
                    rightInner = 0.50f,
                    selfSpreadPose = 1.0f,
                    leftLegSpread = samplePulse.closeSpreadL,
                    rightLegSpread = samplePulse.closeSpreadR,
                    leftKneeShift = samplePulse.closeKneeL,
                    rightKneeShift = samplePulse.closeKneeR
                ),
                includeLithotomyBody = true
            )
            assertValidMesh(scene.blossomBodyMesh)
            assertValidMesh(scene.blossomHandsMesh)
            assertValidMesh(scene.blossomPalmsMesh)
            assertOutwardWindingMatchesNormals("Body(spreadPose+strongLaterSurge_$seed)", scene.blossomBodyMesh)
        }

        // 4. Verify 2x post-peak heart rate recovery drops ~2x faster than normal AND stops cleanly at 125 BPM floor
        var bpmNormal = 141f
        var bpmFast = 141f
        for (step in 0 until 60) { // 6.0 seconds at 0.1s/step
            bpmNormal = VulvaGlTextureView.stepHeartRateBpm(
                currentBpm = bpmNormal,
                dtSec = 0.1f,
                targetSensitivityGain = 0f,
                activeDrive = 0f,
                secondsSinceLastStim = 5.0f,
                recoveryRateMultiplier = 1.0f,
                forceRecovery = false
            )
            bpmFast = VulvaGlTextureView.stepHeartRateBpm(
                currentBpm = bpmFast,
                dtSec = 0.1f,
                targetSensitivityGain = 1.0f,
                activeDrive = 1.0f,
                secondsSinceLastStim = 0.1f,
                recoveryRateMultiplier = 2.0f,
                forceRecovery = true
            )
        }
        val normalDrop = 141f - bpmNormal
        val fastDrop = 141f - bpmFast
        assertTrue(
            "6s 2x recovery drop ($fastDrop BPM) must be substantially larger than normal 6s drop ($normalDrop BPM)",
            fastDrop > normalDrop * 1.55f && fastDrop < normalDrop * 2.15f
        )

        var bpmPostPeak = 152f
        for (step in 0 until 120) { // 12 seconds of forced recovery with 125 BPM floor
            bpmPostPeak = VulvaGlTextureView.stepHeartRateBpm(
                currentBpm = bpmPostPeak,
                dtSec = 0.1f,
                targetSensitivityGain = 0f,
                activeDrive = 0f,
                secondsSinceLastStim = 5.0f,
                recoveryRateMultiplier = 2.0f,
                forceRecovery = true,
                recoveryFloorBpm = com.example.ui.components.PEAK_RECOVERY_FLOOR_BPM,
                maxBpm = VulvaGlTextureView.PEAK_MAX_HEART_RATE_BPM
            )
            assertTrue("Post-peak cooldown must never undershoot 125 BPM (got $bpmPostPeak)", bpmPostPeak >= 125f - 1e-4f)
        }
        assertEquals("Post-peak cooldown must settle cleanly at 125 BPM", 125f, bpmPostPeak, 0.05f)

        // 5. Verify peak breathing & forced powerful recovery sighs stay anatomically bounded
        var maxPeakChest = 0f
        var maxPeakAbd = 0f
        var maxSighChest = 0f
        var maxSighAbd = 0f
        for (k in 0 until 64) {
            val p = k / 64f
            val peakDisp = VulvaGlTextureView.evaluateHeartRateBreathingDisplacement(
                phaseCycles = p, bpm = 152f, stimulus = 1f, isSighCycle = false, depthMultiplier = 1.95f, peakMode = true
            )
            val sighDisp = VulvaGlTextureView.evaluateHeartRateBreathingDisplacement(
                phaseCycles = p, bpm = 132f, stimulus = 0f, isSighCycle = true, depthMultiplier = 2.0f, forcePowerfulSigh = true
            )
            if (peakDisp.first > maxPeakChest) maxPeakChest = peakDisp.first
            if (peakDisp.second > maxPeakAbd) maxPeakAbd = peakDisp.second
            if (sighDisp.first > maxSighChest) maxSighChest = sighDisp.first
            if (sighDisp.second > maxSighAbd) maxSighAbd = sighDisp.second
        }
        assertTrue("Peak heave must be visibly heavy and bounded ($maxPeakChest, $maxPeakAbd)", maxPeakChest in 0.20f..0.2421f && maxPeakAbd in 0.22f..0.2621f)
        assertTrue("Post-peak powerful sigh must be deep and bounded ($maxSighChest, $maxSighAbd)", maxSighChest in 0.16f..0.2201f && maxSighAbd in 0.18f..0.2401f)
    }

    @Test
    fun seamDBindPoseFixPreventsInnerThighCutoutWhilePreservingPelvicOpening() {
        val scene = VulvaAnatomySceneMesh()
        val body = scene.blossomBodyMesh
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val legCache = scene.blossomLegSkinCache
        val armCache = scene.blossomArmSkinCache

        val isLegVertex = BooleanArray(body.vertexCount)
        for (idx in legCache.bodyVertIndices) isLegVertex[idx] = true
        val isArmVertex = BooleanArray(body.vertexCount)
        for (idx in armCache.bodyVertIndices) isArmVertex[idx] = true

        assertEquals(
            "Bind-pose Seam D array must hold 4 floats per body vertex",
            body.vertexCount * 4,
            scene.blossomBodyBindHoleData.size
        )
        for (v in scene.blossomBodyBindHoleData) {
            assertTrue("Bind-pose Seam D attribute values must be finite", v.isFinite())
        }

        // Helper counting leg-owned vertices & leg-owned triangle fragment samples discarded by Legacy Posed vs Fixed Bind-Pose Seam D
        data class SeamDCounts(
            val legVertsDiscarded: Int,
            val legTriSamplesDiscarded: Int,
            val armVertsDiscarded: Int,
            val pelvicVertsDiscarded: Int,
            val pelvicTriSamplesDiscarded: Int
        )

        fun evaluateSceneSeamD(useBindPoseFix: Boolean, betaSmoothBodyHoleCutoff: Boolean): SeamDCounts {
            val bData = body.vertexData
            val bindData = scene.blossomBodyBindHoleData
            val idxBuf = body.indexBuffer

            var legVerts = 0
            var armVerts = 0
            var pelvicVerts = 0
            for (v in 0 until body.vertexCount) {
                val discarded = if (useBindPoseFix) {
                    val bd = v * 4
                    VulvaGlTextureView.evaluateBindPoseSeamDDiscard(
                        bindOvalX = bindData[bd],
                        bindOvalY = bindData[bd + 1],
                        bindZ = bindData[bd + 2],
                        bindNz = bindData[bd + 3],
                        betaSmoothBodyHoleCutoff = betaSmoothBodyHoleCutoff
                    )
                } else {
                    val b = v * stride
                    VulvaGlTextureView.evaluateLegacyPosedSeamDDiscard(
                        posedX = bData[b],
                        posedY = bData[b + 1],
                        posedZ = bData[b + 2],
                        normalZ = bData[b + 5],
                        betaSmoothBodyHoleCutoff = betaSmoothBodyHoleCutoff
                    )
                }
                if (discarded) {
                    when {
                        isLegVertex[v] -> legVerts++
                        isArmVertex[v] -> armVerts++
                        else -> pelvicVerts++
                    }
                }
            }

            // Sample triangle barycentric interiors (centroid + 3 edge midpoints) to match fragment-shader interpolation
            val bary = arrayOf(
                floatArrayOf(1f / 3f, 1f / 3f, 1f / 3f),
                floatArrayOf(0.5f, 0.5f, 0f),
                floatArrayOf(0.5f, 0f, 0.5f),
                floatArrayOf(0f, 0.5f, 0.5f)
            )
            var legSamples = 0
            var pelvicSamples = 0
            var t = 0
            while (t + 2 < body.indexCount) {
                val i0 = idxBuf.get(t).toInt() and 0xFFFF
                val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
                val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
                t += 3
                val triHasLeg = isLegVertex[i0] || isLegVertex[i1] || isLegVertex[i2]
                val triNoLimb = !triHasLeg && !isArmVertex[i0] && !isArmVertex[i1] && !isArmVertex[i2]
                if (!triHasLeg && !triNoLimb) continue

                for (w in bary) {
                    val w0 = w[0]; val w1 = w[1]; val w2 = w[2]
                    val discarded = if (useBindPoseFix) {
                        val d0 = i0 * 4; val d1 = i1 * 4; val d2 = i2 * 4
                        VulvaGlTextureView.evaluateBindPoseSeamDDiscard(
                            bindOvalX = bindData[d0] * w0 + bindData[d1] * w1 + bindData[d2] * w2,
                            bindOvalY = bindData[d0 + 1] * w0 + bindData[d1 + 1] * w1 + bindData[d2 + 1] * w2,
                            bindZ = bindData[d0 + 2] * w0 + bindData[d1 + 2] * w1 + bindData[d2 + 2] * w2,
                            bindNz = bindData[d0 + 3] * w0 + bindData[d1 + 3] * w1 + bindData[d2 + 3] * w2,
                            betaSmoothBodyHoleCutoff = betaSmoothBodyHoleCutoff
                        )
                    } else {
                        val b0 = i0 * stride; val b1 = i1 * stride; val b2 = i2 * stride
                        val nx = bData[b0 + 3] * w0 + bData[b1 + 3] * w1 + bData[b2 + 3] * w2
                        val ny = bData[b0 + 4] * w0 + bData[b1 + 4] * w1 + bData[b2 + 4] * w2
                        val nz = bData[b0 + 5] * w0 + bData[b1 + 5] * w1 + bData[b2 + 5] * w2
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
                        VulvaGlTextureView.evaluateLegacyPosedSeamDDiscard(
                            posedX = bData[b0] * w0 + bData[b1] * w1 + bData[b2] * w2,
                            posedY = bData[b0 + 1] * w0 + bData[b1 + 1] * w1 + bData[b2 + 1] * w2,
                            posedZ = bData[b0 + 2] * w0 + bData[b1 + 2] * w1 + bData[b2 + 2] * w2,
                            normalZ = nz / nLen,
                            betaSmoothBodyHoleCutoff = betaSmoothBodyHoleCutoff
                        )
                    }
                    if (discarded) {
                        if (triHasLeg) legSamples++ else pelvicSamples++
                    }
                }
            }
            return SeamDCounts(legVerts, legSamples, armVerts, pelvicVerts, pelvicSamples)
        }

        // 1. Verify exact Seam D parity at Rest (0/0) AND in "Ask Blossom to Spread" pose (selfSpreadPose = 1.0f)
        val restState = VulvaDeformationState(betaSmoothBodyHoleCutoff = true)
        scene.update(restState, includeLithotomyBody = true)
        val restVersionBeforePosing = scene.blossomBodyBindHoleVersion
        val restBindSnapshot = scene.blossomBodyBindHoleData.copyOf()
        val restLegacy = evaluateSceneSeamD(useBindPoseFix = false, betaSmoothBodyHoleCutoff = true)
        val restFixed = evaluateSceneSeamD(useBindPoseFix = true, betaSmoothBodyHoleCutoff = true)
        assertTrue("Rest pose must discard the anterior pelvic opening under Seam D (> 20 verts)", restFixed.pelvicVertsDiscarded > 20)
        assertEquals("Rest pose pelvic vertex discard count must match pre-fix Seam D identically", restLegacy.pelvicVertsDiscarded, restFixed.pelvicVertsDiscarded)
        assertEquals("Rest pose pelvic fragment-sample discard count must match pre-fix Seam D identically", restLegacy.pelvicTriSamplesDiscarded, restFixed.pelvicTriSamplesDiscarded)
        assertEquals("Rest pose must discard 0 leg vertices", 0, restFixed.legVertsDiscarded)
        assertEquals("Rest pose must discard 0 leg/bridge triangle samples", 0, restFixed.legTriSamplesDiscarded)

        val spreadState = VulvaDeformationState(
            leftOuter = 0.50f,
            rightOuter = 0.50f,
            leftInner = 0.50f,
            rightInner = 0.50f,
            selfSpreadPose = 1.0f,
            betaSmoothBodyHoleCutoff = true
        )
        scene.update(spreadState, includeLithotomyBody = true)
        val spreadLegacy = evaluateSceneSeamD(useBindPoseFix = false, betaSmoothBodyHoleCutoff = true)
        val spreadFixed = evaluateSceneSeamD(useBindPoseFix = true, betaSmoothBodyHoleCutoff = true)
        assertEquals("Spread pose pelvic vertex discard count must match pre-fix Seam D identically", spreadLegacy.pelvicVertsDiscarded, spreadFixed.pelvicVertsDiscarded)
        assertEquals("Spread pose pelvic fragment-sample discard count must match pre-fix Seam D identically", spreadLegacy.pelvicTriSamplesDiscarded, spreadFixed.pelvicTriSamplesDiscarded)
        assertEquals("Spread pose must discard 0 leg vertices", 0, spreadFixed.legVertsDiscarded)
        assertEquals("Spread pose must discard 0 arm vertices", 0, spreadFixed.armVertsDiscarded)

        // 2. Isolated comparison on the frozen initial peak squeeze (legSpread = -0.84, kneeShift = -1.12 on both sides)
        val initialSqueezeState = spreadState.copy(
            leftLegSpread = -0.84f,
            rightLegSpread = -0.84f,
            leftKneeShift = -1.12f,
            rightKneeShift = -1.12f,
            betaSmoothBodyHoleCutoff = true
        )
        scene.update(initialSqueezeState, includeLithotomyBody = true)
        val initLegacyOn = evaluateSceneSeamD(useBindPoseFix = false, betaSmoothBodyHoleCutoff = true)
        val initLegacyOff = evaluateSceneSeamD(useBindPoseFix = false, betaSmoothBodyHoleCutoff = false)
        val initFixedOn = evaluateSceneSeamD(useBindPoseFix = true, betaSmoothBodyHoleCutoff = true)

        println(
            "SEAM_D_DIAG initialSqueeze(-0.84/-1.12) " +
                "legacyOn(legVerts=${initLegacyOn.legVertsDiscarded}, legSamples=${initLegacyOn.legTriSamplesDiscarded}, pelvicVerts=${initLegacyOn.pelvicVertsDiscarded}) " +
                "legacyOff(legVerts=${initLegacyOff.legVertsDiscarded}) " +
                "fixedOn(legVerts=${initFixedOn.legVertsDiscarded}, legSamples=${initFixedOn.legTriSamplesDiscarded}, pelvicVerts=${initFixedOn.pelvicVertsDiscarded}, pelvicSamples=${initFixedOn.pelvicTriSamplesDiscarded})"
        )

        // Confirm isolated root cause: posed Seam D ON falsely punched a hole through the squeezed thigh; OFF did not
        assertTrue(
            "Isolated pre-fix check: legacy posed Seam D ON must reproduce thigh cutout on initial squeeze (got ${initLegacyOn.legVertsDiscarded} verts, ${initLegacyOn.legTriSamplesDiscarded} samples)",
            initLegacyOn.legVertsDiscarded > 0 && initLegacyOn.legTriSamplesDiscarded > 0
        )
        assertEquals("Isolated pre-fix check: Seam D OFF discards 0 thigh vertices", 0, initLegacyOff.legVertsDiscarded)
        // Confirm bind-pose fix completely eliminates thigh cutout (including groin bridge triangles) while keeping pelvic opening intact
        assertEquals("Fixed bind-pose Seam D ON must discard 0 thigh vertices on initial squeeze", 0, initFixedOn.legVertsDiscarded)
        assertEquals("Fixed bind-pose Seam D ON must discard 0 thigh/bridge triangle samples on initial squeeze", 0, initFixedOn.legTriSamplesDiscarded)
        assertEquals("Fixed bind-pose Seam D ON must discard 0 arm vertices on initial squeeze", 0, initFixedOn.armVertsDiscarded)
        assertEquals("Fixed bind-pose Seam D ON must preserve exact pelvic opening during initial squeeze", restFixed.pelvicVertsDiscarded, initFixedOn.pelvicVertsDiscarded)
        assertEquals("Fixed bind-pose Seam D ON must preserve exact pelvic fragment samples during initial squeeze", restFixed.pelvicTriSamplesDiscarded, initFixedOn.pelvicTriSamplesDiscarded)

        // 3. Isolated comparison on later asymmetric squeezes from buildLaterPeakLegPulses across multiple seeds
        var anyLaterPulseReproducedLegacyBug = false
        for (seed in longArrayOf(101L, 2024L, 98765L)) {
            val pulses = com.example.ui.components.buildLaterPeakLegPulses(kotlin.random.Random(seed))
            for ((idx, pulse) in pulses.withIndex()) {
                scene.update(
                    spreadState.copy(
                        leftLegSpread = pulse.closeSpreadL,
                        rightLegSpread = pulse.closeSpreadR,
                        leftKneeShift = pulse.closeKneeL,
                        rightKneeShift = pulse.closeKneeR
                    ),
                    includeLithotomyBody = true
                )
                val legLegacy = evaluateSceneSeamD(useBindPoseFix = false, betaSmoothBodyHoleCutoff = true)
                val legFixed = evaluateSceneSeamD(useBindPoseFix = true, betaSmoothBodyHoleCutoff = true)
                if (legLegacy.legVertsDiscarded > 0) anyLaterPulseReproducedLegacyBug = true
                if (pulse.isStrongSurge && seed == 101L) {
                    println(
                        "SEAM_D_DIAG laterAsymSurge(seed=$seed,idx=$idx,sp=${pulse.closeSpreadL}/${pulse.closeSpreadR},kn=${pulse.closeKneeL}/${pulse.closeKneeR}) " +
                            "legacyOn(legVerts=${legLegacy.legVertsDiscarded},legSamples=${legLegacy.legTriSamplesDiscarded}) " +
                            "fixedOn(legVerts=${legFixed.legVertsDiscarded},legSamples=${legFixed.legTriSamplesDiscarded})"
                    )
                }
                assertEquals("Fixed Seam D must discard 0 thigh verts on later pulse seed=$seed idx=$idx", 0, legFixed.legVertsDiscarded)
                assertEquals("Fixed Seam D must discard 0 thigh/bridge triangle samples on later pulse seed=$seed idx=$idx", 0, legFixed.legTriSamplesDiscarded)
                assertEquals("Fixed Seam D must preserve exact pelvic opening on later pulse seed=$seed idx=$idx", restFixed.pelvicVertsDiscarded, legFixed.pelvicVertsDiscarded)
            }
        }
        assertTrue("Isolated pre-fix check: later asymmetric strong squeezes must also trigger legacy posed Seam D cutout", anyLaterPulseReproducedLegacyBug)

        // 4. Verify bind-pose Seam D buffer stayed bitwise immutable across all leg/arm/spread poses (zero per-frame mutation/upload)
        assertEquals("Posing legs/arms must NOT increment blossomBodyBindHoleVersion", restVersionBeforePosing, scene.blossomBodyBindHoleVersion)
        assertTrue("Posing legs/arms must NOT mutate blossomBodyBindHoleData", restBindSnapshot.contentEquals(scene.blossomBodyBindHoleData))

        // 5. Verify toggling pelvis beta geometry while legs are squeezed keeps thighs 100% opaque & pelvic rim synced
        scene.update(initialSqueezeState.copy(betaSmoothVulvaSeam = false), includeLithotomyBody = true)
        val squeezedToggleOff = evaluateSceneSeamD(useBindPoseFix = true, betaSmoothBodyHoleCutoff = true)
        assertEquals("Toggling betaSmoothVulvaSeam OFF while squeezed must still discard 0 thigh verts", 0, squeezedToggleOff.legVertsDiscarded)
        assertEquals("Toggling betaSmoothVulvaSeam OFF while squeezed must still discard 0 thigh/bridge samples", 0, squeezedToggleOff.legTriSamplesDiscarded)
        scene.update(initialSqueezeState, includeLithotomyBody = true)
        val squeezedToggleRestored = evaluateSceneSeamD(useBindPoseFix = true, betaSmoothBodyHoleCutoff = true)
        assertEquals("Restoring betaSmoothVulvaSeam ON while squeezed must keep 0 thigh verts discarded", 0, squeezedToggleRestored.legVertsDiscarded)
        assertEquals("Restoring betaSmoothVulvaSeam ON while squeezed must restore exact pelvic opening", restFixed.pelvicVertsDiscarded, squeezedToggleRestored.pelvicVertsDiscarded)
    }

    @Test
    fun closedEyelidsFullyOccludeBlueIrisesAndPreserveOpenEyes() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val baseState = VulvaDeformationState()

        // Helper: convert world position back into head-local space (at headTurn=0, headPitch=0)
        // matching BlossomUpperBodyBuilder.headLocalToWorld / headWorldToLocal
        val headCenterY = 6.32f
        val headCenterZ = BlossomBodyBuilder.mattressSurfaceZ(6.32f) + 0.62f
        val headScale = 0.78f
        val pitchRad = (-13.0 * Math.PI / 180.0).toFloat()
        val cosP = kotlin.math.cos(pitchRad)
        val sinP = kotlin.math.sin(pitchRad)
        fun worldToHeadLocal(wx: Float, wy: Float, wz: Float): FloatArray {
            val dx = wx
            val dy = wy - headCenterY
            val dz = wz - headCenterZ
            val sy = dy * cosP + dz * sinP
            val sz = -dy * sinP + dz * cosP
            return floatArrayOf(dx / headScale, sy / headScale, sz / headScale)
        }

        // Compute front-most interpolated head-local Z on front-facing head triangles covering (qx, qy)
        fun sampleFrontHeadLocalZ(headMesh: DynamicGlMesh, qx: Float, qy: Float): Float {
            val hData = headMesh.vertexData
            val idxBuf = headMesh.indexBuffer
            idxBuf.position(0)
            var bestZ = Float.NEGATIVE_INFINITY
            var t = 0
            while (t + 2 < headMesh.indexCount) {
                val i0 = idxBuf.get(t).toInt() and 0xFFFF
                val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
                val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
                t += 3
                val b0 = i0 * stride
                val b1 = i1 * stride
                val b2 = i2 * stride
                // Quick world-Y filter to skip back/lower head
                if (hData[b0 + 1] < 5.95f && hData[b1 + 1] < 5.95f && hData[b2 + 1] < 5.95f) continue
                val p0 = worldToHeadLocal(hData[b0], hData[b0 + 1], hData[b0 + 2])
                val p1 = worldToHeadLocal(hData[b1], hData[b1 + 1], hData[b1 + 2])
                val p2 = worldToHeadLocal(hData[b2], hData[b2 + 1], hData[b2 + 2])
                if (p0[2] < 0.30f && p1[2] < 0.30f && p2[2] < 0.30f) continue
                val minX = minOf(p0[0], p1[0], p2[0])
                val maxX = maxOf(p0[0], p1[0], p2[0])
                if (qx < minX - 1e-4f || qx > maxX + 1e-4f) continue
                val minY = minOf(p0[1], p1[1], p2[1])
                val maxY = maxOf(p0[1], p1[1], p2[1])
                if (qy < minY - 1e-4f || qy > maxY + 1e-4f) continue

                // 2D barycentric test in head-local (lx, ly)
                val v0x = p1[0] - p0[0]; val v0y = p1[1] - p0[1]
                val v1x = p2[0] - p0[0]; val v1y = p2[1] - p0[1]
                val denom = v0x * v1y - v1x * v0y
                if (abs(denom) < 1e-8f) continue
                val vx = qx - p0[0]; val vy = qy - p0[1]
                val u = (vx * v1y - v1x * vy) / denom
                val v = (v0x * vy - vx * v0y) / denom
                val w = 1f - u - v
                if (u >= -1e-4f && v >= -1e-4f && w >= -1e-4f) {
                    val zInterp = w * p0[2] + u * p1[2] + v * p2[2]
                    if (zInterp > bestZ) bestZ = zInterp
                }
            }
            idxBuf.position(0)
            return bestZ
        }

        // 1. Open eyes (eyeBlink = 0f): blue iris vertices must sit strictly in front of recessed aperture
        scene.update(baseState.copy(eyeBlink = 0f), includeLithotomyBody = true)
        val openHeadSnapshot = scene.blossomHeadMesh.vertexData.copyOf()
        var openVisibleIrisCount = 0
        var totalIrisVerts = 0
        val eyesMesh = scene.blossomEyeMeshes.first()
        val eData = eyesMesh.vertexData
        for (i in 0 until eyesMesh.vertexCount) {
            val b = i * stride
            val irisR = eData[b + 6]
            if (irisR <= 0.48f) {
                totalIrisVerts++
                val lp = worldToHeadLocal(eData[b], eData[b + 1], eData[b + 2])
                val headZ = sampleFrontHeadLocalZ(scene.blossomHeadMesh, lp[0], lp[1])
                if (lp[2] > headZ + 0.01f) openVisibleIrisCount++
            }
        }
        assertTrue(
            "Open eyes must expose >80% of blue iris cleanly (visible=$openVisibleIrisCount/$totalIrisVerts)",
            openVisibleIrisCount > (totalIrisVerts * 0.80f).toInt() && totalIrisVerts > 100
        )

        // 2. Closed eyes (eyeBlink = 1f) across multiple gaze & peak facial morph combinations:
        //    EVERY iris & central-sclera vertex (irisR <= 0.65) must be strictly occluded behind the closed eyelid!
        val closedTestConfigs = listOf(
            baseState.copy(eyeBlink = 1.0f),
            baseState.copy(eyeBlink = 1.0f, mouthOpen = 0.94f, faceBrow = 0.55f, faceWarmth = 0.6f),
            baseState.copy(eyeBlink = 1.0f, eyeGazeX = 0.85f, eyeGazeY = -0.85f, eyelidGazeTrack = 1.0f),
            baseState.copy(eyeBlink = 1.0f, eyeGazeX = -0.85f, eyeGazeY = 0.85f, eyelidGazeTrack = 1.0f)
        )
        for ((cfgIdx, cfg) in closedTestConfigs.withIndex()) {
            scene.update(cfg, includeLithotomyBody = true)
            assertValidMesh(scene.blossomHeadMesh)
            assertOutwardWindingMatchesNormals("blossomHeadMesh(closedCfg=$cfgIdx)", scene.blossomHeadMesh)

            var exposedIrisCount = 0
            var minClearance = Float.POSITIVE_INFINITY
            val curEyes = scene.blossomEyeMeshes.first().vertexData
            for (i in 0 until eyesMesh.vertexCount) {
                val b = i * stride
                val irisR = curEyes[b + 6]
                if (irisR <= 0.65f) {
                    val lp = worldToHeadLocal(curEyes[b], curEyes[b + 1], curEyes[b + 2])
                    val eyelidZ = sampleFrontHeadLocalZ(scene.blossomHeadMesh, lp[0], lp[1])
                    val clearance = eyelidZ - lp[2]
                    if (clearance < minClearance) minClearance = clearance
                    if (clearance <= 0.006f) exposedIrisCount++
                }
            }
            assertEquals(
                "Closed eyelid (cfg=$cfgIdx) must fully occlude every iris/front-sclera vertex (minClearance=$minClearance)",
                0,
                exposedIrisCount
            )
            assertTrue(
                "Closed eyelid (cfg=$cfgIdx) must maintain safe Z clearance > 0.006 over iris, got $minClearance",
                minClearance > 0.006f
            )
        }

        // 3. Reopening to eyeBlink = 0f must restore open head geometry cleanly
        scene.update(baseState.copy(eyeBlink = 0f), includeLithotomyBody = true)
        var maxRestoreDiff = 0f
        for (i in openHeadSnapshot.indices) {
            val d = abs(scene.blossomHeadMesh.vertexData[i] - openHeadSnapshot[i])
            if (d > maxRestoreDiff) maxRestoreDiff = d
        }
        assertTrue("Reopening eyes must restore open head mesh (< 1e-5), got $maxRestoreDiff", maxRestoreDiff < 1e-5f)
    }

    @Test
    fun climaxVibrationPatternHasOneThirdFrequencyAndMaxAmplitude() {
        var totalOnMs = 0L
        var totalOffMs = 0L
        var onCount = 0
        var offCount = 0
        var minOnAmp = Int.MAX_VALUE
        var maxOnAmp = Int.MIN_VALUE

        for (seed in 1L..25L) {
            val rng = kotlin.random.Random(seed * 7919L)
            val chunk = com.example.ui.components.buildPeakVibrationChunk(rng, remainingMs = 12_000L)
            assertTrue("Peak vibration chunk must not be empty", chunk.timings.isNotEmpty())
            assertEquals("Timings and amplitudes arrays must match in length", chunk.timings.size, chunk.amplitudes.size)
            assertTrue("Chunk duration must fit within remainingMs", chunk.totalMs <= 12_000L)
            assertEquals("totalMs must equal sum of timings", chunk.timings.sum(), chunk.totalMs)

            for (i in chunk.timings.indices) {
                val dur = chunk.timings[i]
                val amp = chunk.amplitudes[i]
                if (i % 2 == 0) {
                    assertTrue("ON pulse duration must be in 84..159ms, got $dur", dur in 84L..159L)
                    assertTrue("ON pulse amplitude must be near hardware max (248..255), got $amp", amp in 248..255)
                    totalOnMs += dur
                    onCount++
                    if (amp < minOnAmp) minOnAmp = amp
                    if (amp > maxOnAmp) maxOnAmp = amp
                } else {
                    assertEquals("OFF gap amplitude must be 0", 0, amp)
                    assertTrue("OFF gap duration must be in 44..264ms, got $dur", dur in 44L..264L)
                    totalOffMs += dur
                    offCount++
                }
            }
        }

        val avgOnMs = totalOnMs.toFloat() / onCount
        val avgOffMs = totalOffMs.toFloat() / offCount
        val avgPeriodMs = avgOnMs + avgOffMs
        // Original fast cadence had ~75.5ms average period; 1/3 frequency is ~226ms period.
        assertTrue("Average climax pulse period must be ~3x slower (195..270ms), got $avgPeriodMs", avgPeriodMs in 195f..270f)
        assertTrue("Average ON pulse duration must allow full motor spin-up (105..140ms), got $avgOnMs", avgOnMs in 105f..140f)
        assertTrue("Peak amplitudes must stay in 248..255, got $minOnAmp..$maxOnAmp", minOnAmp >= 248 && maxOnAmp == 255)
    }

    @Test
    fun v27VisualRealismLayeredHairWispsAnd3DEyelashesGeometryAndWindingValid() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        assertValidMesh(scene.blossomHairWispsMesh)
        assertValidMesh(scene.blossomEyelashesMesh)
        assertOutwardWindingMatchesNormals("blossomHairWispsMesh", scene.blossomHairWispsMesh)
        assertOutwardWindingMatchesNormals("blossomEyelashesMesh", scene.blossomEyelashesMesh)
        assertTrue("blossomHairWispsMesh should contain 26 ribbons (>2000 verts)", scene.blossomHairWispsMesh.vertexCount > 2000)
        assertTrue("blossomEyelashesMesh should contain 4 strips (672 verts)", scene.blossomEyelashesMesh.vertexCount == 672)

        val restWisps = scene.blossomHairWispsMesh.vertexData.copyOf()
        val restLashes = scene.blossomEyelashesMesh.vertexData.copyOf()

        // Verify mattress floor clamp & valid normals across extreme head turn + pitch combinations
        for ((turn, pitch) in listOf(Pair(1f, 0f), Pair(-1f, 0f), Pair(0.6f, -0.9f), Pair(-0.7f, 0.8f))) {
            scene.update(
                VulvaDeformationState(headTurn = turn, headPitch = pitch, eyeBlink = 0.5f),
                includeLithotomyBody = true
            )
            assertValidMesh(scene.blossomHairWispsMesh)
            assertValidMesh(scene.blossomEyelashesMesh)

            val wData = scene.blossomHairWispsMesh.vertexData
            for (i in 0 until scene.blossomHairWispsMesh.vertexCount) {
                val b = i * stride
                val wy = wData[b + 1]
                val wz = wData[b + 2]
                val floorZ = BlossomBodyBuilder.mattressSurfaceZ(wy) + 0.03f
                assertTrue(
                    "Hair wisp vert $i at turn=$turn pitch=$pitch must stay above mattress (z=$wz >= $floorZ)",
                    wz >= floorZ - 1e-4f
                )
            }
        }

        // Verify exact bit-identical restoration when returning to neutral head pose
        scene.update(VulvaDeformationState(), includeLithotomyBody = true)
        var maxWispDiff = 0f
        for (i in restWisps.indices) {
            val d = abs(scene.blossomHairWispsMesh.vertexData[i] - restWisps[i])
            if (d > maxWispDiff) maxWispDiff = d
        }
        var maxLashDiff = 0f
        for (i in restLashes.indices) {
            val d = abs(scene.blossomEyelashesMesh.vertexData[i] - restLashes[i])
            if (d > maxLashDiff) maxLashDiff = d
        }
        assertTrue("Hair wisps must restore bit-identically at neutral pose (<1e-5), got $maxWispDiff", maxWispDiff < 1e-5f)
        assertTrue("3D eyelashes must restore bit-identically at neutral pose (<1e-5), got $maxLashDiff", maxLashDiff < 1e-5f)
    }

    @Test
    fun v27EyelashesTrackBlinkAndHeadTurnWithoutEyeballPenetration() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val openData = scene.blossomEyelashesMesh.vertexData.copyOf()

        // Full blink closure: upper lashes must sweep downward cleanly & every lash vertex must stay outside both eyeballs
        scene.update(VulvaDeformationState(eyeBlink = 1.0f), includeLithotomyBody = true)
        assertValidMesh(scene.blossomEyelashesMesh)
        assertOutwardWindingMatchesNormals("blossomEyelashesMesh(closed)", scene.blossomEyelashesMesh)

        val closedData = scene.blossomEyelashesMesh.vertexData.copyOf()
        val eyeRadius = 0.136f
        val eyeCenterY = 0.038f
        val eyeCenterZ = 0.528f
        var upperDropSum = 0f
        var upperCount = 0

        for (i in 0 until scene.blossomEyelashesMesh.vertexCount) {
            val b = i * stride
            val isUpper = closedData[b + 9] > 0.5f
            if (isUpper) {
                upperDropSum += (closedData[b + 1] - openData[b + 1])
                upperCount++
            }
            val local = BlossomUpperBodyBuilder.headWorldToLocal(closedData[b], closedData[b + 1], closedData[b + 2])
            val side = if (local[0] >= 0f) 1f else -1f
            val dx = local[0] - side * 0.256f
            val dy = local[1] - eyeCenterY
            val dz = local[2] - eyeCenterZ
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            assertTrue(
                "Closed eyelash vert $i must stay outside eyeball sphere (dist=$dist > $eyeRadius)",
                dist > eyeRadius + 0.008f
            )
        }
        val avgUpperDrop = upperDropSum / upperCount
        assertTrue("Upper lashes must sweep downward during full blink (avgDeltaY < -0.02), got $avgUpperDrop", avgUpperDrop < -0.02f)

        // Head turn rigidity check: average distance between eyelashes and eyes remains invariant under cervical rotation
        scene.update(VulvaDeformationState(eyeBlink = 0f, headTurn = 0f, headPitch = 0f), includeLithotomyBody = true)
        fun centroid(mesh: DynamicGlMesh): FloatArray {
            var sx = 0f; var sy = 0f; var sz = 0f
            val d = mesh.vertexData
            for (i in 0 until mesh.vertexCount) {
                val b = i * stride
                sx += d[b]; sy += d[b + 1]; sz += d[b + 2]
            }
            val inv = 1f / mesh.vertexCount
            return floatArrayOf(sx * inv, sy * inv, sz * inv)
        }
        val cEye0 = centroid(scene.blossomEyesMesh)
        val cLash0 = centroid(scene.blossomEyelashesMesh)
        val dist0 = sqrt(
            (cLash0[0] - cEye0[0]).let { it * it } +
                (cLash0[1] - cEye0[1]).let { it * it } +
                (cLash0[2] - cEye0[2]).let { it * it }
        )
        scene.update(VulvaDeformationState(eyeBlink = 0f, headTurn = 0.85f, headPitch = -0.55f), includeLithotomyBody = true)
        val cEye1 = centroid(scene.blossomEyesMesh)
        val cLash1 = centroid(scene.blossomEyelashesMesh)
        val dist1 = sqrt(
            (cLash1[0] - cEye1[0]).let { it * it } +
                (cLash1[1] - cEye1[1]).let { it * it } +
                (cLash1[2] - cEye1[2]).let { it * it }
        )
        assertTrue(
            "3D eyelashes must rotate rigidly with head/eyes (|dist1 - dist0| < 1e-3), got ${abs(dist1 - dist0)}",
            abs(dist1 - dist0) < 1e-3f
        )
    }

    @Test
    fun v27VisualRealismToggleDefaultsAndSessionStateMemory() {
        // Baseline VulvaDeformationState keeps all 5 v27.0 toggles OFF by default for test compatibility
        val baseline = VulvaDeformationState()
        assertFalse(baseline.betaRealismLayeredHair)
        assertFalse(baseline.betaRealismSkinDetail)
        assertFalse(baseline.betaRealismEyesAndLashes)
        assertFalse(baseline.betaRealismContactShadows)
        assertFalse(baseline.betaRealismDynamicCreases)

        // Interactive BlossomViewportSessionState keeps all 5 v27.0 visual realism upgrades OFF by default
        val session = com.example.ui.components.BlossomViewportSessionState()
        assertFalse(session.betaRealismLayeredHair)
        assertFalse(session.betaRealismSkinDetail)
        assertFalse(session.betaRealismEyesAndLashes)
        assertFalse(session.betaRealismContactShadows)
        assertFalse(session.betaRealismDynamicCreases)

        // Enabling each toggle and calling disableAllBetaToggles() cleanly disables all 5 v27.0 toggles
        session.betaRealismLayeredHair = true
        session.betaRealismSkinDetail = true
        session.betaRealismEyesAndLashes = true
        session.betaRealismContactShadows = true
        session.betaRealismDynamicCreases = true
        session.disableAllBetaToggles()
        assertFalse(session.betaRealismLayeredHair)
        assertFalse(session.betaRealismSkinDetail)
        assertFalse(session.betaRealismEyesAndLashes)
        assertFalse(session.betaRealismContactShadows)
        assertFalse(session.betaRealismDynamicCreases)
    }

    @Test
    fun v28Upgraded62LockLayeredHairWispsGeometryWindingFloorAndRigidityValid() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val wisps = scene.blossomHairWispsMesh

        // 62 sculpted multi-tier hair-lock & flyaway cards, 7 x 16 verts each = 6,944 verts
        assertEquals("Upgraded v28 hair wisps mesh must have 62 * 7 * 16 = 6944 verts", 62 * 7 * 16, wisps.vertexCount)
        assertValidMesh(wisps)
        assertOutwardWindingMatchesNormals("blossomHairWispsMesh(v28)", wisps)

        // Verify both volumetric lock cards (vMat.w >= 1.0) and airy flyaway strands (vMat.w < 1.0) are encoded
        var volLockVerts = 0
        var airyWispVerts = 0
        for (i in 0 until wisps.vertexCount) {
            val matW = wisps.vertexData[i * stride + 9]
            if (matW >= 1.0f) volLockVerts++ else airyWispVerts++
        }
        assertTrue("Upgraded hair wisps must include dense volumetric lock cards", volLockVerts > 3000)
        assertTrue("Upgraded hair wisps must include airy flyaway/wisp strands", airyWispVerts > 1500)

        // Verify head-turn rigidity & mattress clearance across extreme head turns
        fun centroid(mesh: DynamicGlMesh): FloatArray {
            var sx = 0f; var sy = 0f; var sz = 0f
            val d = mesh.vertexData
            for (i in 0 until mesh.vertexCount) {
                val b = i * stride
                sx += d[b]; sy += d[b + 1]; sz += d[b + 2]
            }
            val inv = 1f / mesh.vertexCount
            return floatArrayOf(sx * inv, sy * inv, sz * inv)
        }
        val c0 = centroid(wisps)
        scene.update(VulvaDeformationState(headTurn = 0.9f, headPitch = -0.5f), includeLithotomyBody = true)
        assertValidMesh(wisps)
        assertOutwardWindingMatchesNormals("blossomHairWispsMesh(v28_turned)", wisps)
        val c1 = centroid(wisps)
        val shift = sqrt(
            (c1[0] - c0[0]).let { it * it } +
                (c1[1] - c0[1]).let { it * it } +
                (c1[2] - c0[2]).let { it * it }
        )
        assertTrue("Upgraded hair wisps must follow head turn visibly (shift > 0.05), got $shift", shift > 0.05f)
        for (i in 0 until wisps.vertexCount) {
            val b = i * stride
            val wy = wisps.vertexData[b + 1]
            val wz = wisps.vertexData[b + 2]
            val floorZ = BlossomBodyBuilder.mattressSurfaceZ(wy) + 0.032f
            assertTrue("Hair card vert $i must stay above mattress ($wz >= $floorZ)", wz >= floorZ - 1e-4f)
        }
    }

    @Test
    fun v28Option8ClitoralHoodMonsBlendSoftensHoodWithoutAlteringGlansOrInnerLabia() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        val baseState = VulvaDeformationState(
            betaV3HoodMonsBlend = false,
            betaV3HoodMonsStrength = 0.85f
        )
        scene.update(baseState, includeLithotomyBody = true)
        val baseArch = scene.labiaHoodArchMesh.vertexData.copyOf()
        val baseGlans = scene.glansMesh.vertexData.copyOf()

        val blendedState = baseState.copy(betaV3HoodMonsBlend = true, betaV3HoodMonsStrength = 0.85f)
        scene.update(blendedState, includeLithotomyBody = true)
        assertValidMesh(scene.labiaHoodArchMesh)
        assertValidMesh(scene.blossomVulvaPatchMesh)
        assertValidMesh(scene.glansMesh)

        // Clitoral glans must remain bit-identical
        assertTrue(
            "Option #8 hood–mons blend must leave clitoral glans bit-identical",
            baseGlans.contentEquals(scene.glansMesh.vertexData)
        )

        // Dorsal clitoral hood crest (midRow, u ~ 0.20..0.48) should have markedly lower mucosa & pigment and lifted AO
        val cols = scene.archCols
        val midRow = scene.archRows / 2
        var hoodMucosaDropSum = 0f
        var hoodPigmentDropSum = 0f
        var hoodAoSum = 0f
        var hoodSamples = 0
        for (c in (cols * 20 / 100)..(cols * 48 / 100)) {
            val b = (midRow * cols + c) * stride
            hoodMucosaDropSum += (baseArch[b + 6] - scene.labiaHoodArchMesh.vertexData[b + 6])
            hoodAoSum += scene.labiaHoodArchMesh.vertexData[b + 8]
            hoodPigmentDropSum += (baseArch[b + 9] - scene.labiaHoodArchMesh.vertexData[b + 9])
            hoodSamples++
        }
        assertTrue(
            "Option #8 must reduce dorsal clitoral hood mucosa toward natural mons skin (avg drop > 0.08), got ${hoodMucosaDropSum / hoodSamples}",
            hoodMucosaDropSum / hoodSamples > 0.08f
        )
        assertTrue(
            "Option #8 must lighten dorsal clitoral hood pigment toward natural mons skin (avg drop > 0.06), got ${hoodPigmentDropSum / hoodSamples}",
            hoodPigmentDropSum / hoodSamples > 0.06f
        )
        assertTrue(
            "Option #8 must keep dorsal clitoral hood AO bright & harmonized with mons skin (avg >= 0.90), got ${hoodAoSum / hoodSamples}",
            hoodAoSum / hoodSamples >= 0.90f
        )

        // Lower inner labia rows (far from dorsal hood) should stay essentially untouched
        val lowerRow = scene.archRows / 6
        var maxLowerLabiaMatDiff = 0f
        for (c in 0 until cols) {
            val b = (lowerRow * cols + c) * stride
            for (m in 6..9) {
                val d = abs(scene.labiaHoodArchMesh.vertexData[b + m] - baseArch[b + m])
                if (d > maxLowerLabiaMatDiff) maxLowerLabiaMatDiff = d
            }
        }
        assertTrue(
            "Option #8 must preserve lower inner labia coloration (< 0.01 diff), got $maxLowerLabiaMatDiff",
            maxLowerLabiaMatDiff < 0.01f
        )

        // Reverting Option #8 back OFF restores exact baseline arch vertices
        scene.update(baseState, includeLithotomyBody = true)
        assertTrue(
            "Turning Option #8 back OFF must restore exact baseline arch mesh",
            baseArch.contentEquals(scene.labiaHoodArchMesh.vertexData)
        )
    }

    @Test
    fun v28NewBetaTogglesDefaultOffAndPreserveSliderMemory() {
        val baseline = VulvaDeformationState()
        assertFalse(baseline.betaV3HoodMonsBlend)
        assertEquals(0.75f, baseline.betaV3HoodMonsStrength, 1e-6f)
        assertFalse(baseline.betaRealismFacialMicro)
        assertFalse(baseline.betaRealismStudioLighting)
        assertFalse(baseline.betaRealismSurfaceMicro)

        val session = com.example.ui.components.BlossomViewportSessionState()
        assertFalse(session.betaV3HoodMonsBlend)
        assertEquals(0.75f, session.betaV3HoodMonsStrength, 1e-6f)
        assertFalse(session.betaRealismFacialMicro)
        assertFalse(session.betaRealismStudioLighting)
        assertFalse(session.betaRealismSurfaceMicro)

        session.betaV3HoodMonsBlend = true
        session.betaV3HoodMonsStrength = 0.42f
        session.betaRealismFacialMicro = true
        session.betaRealismStudioLighting = true
        session.betaRealismSurfaceMicro = true

        session.disableAllBetaToggles()
        assertFalse(session.betaV3HoodMonsBlend)
        assertEquals("Option #8 slider must preserve last value across disableAllBetaToggles", 0.42f, session.betaV3HoodMonsStrength, 1e-6f)
        assertFalse(session.betaRealismFacialMicro)
        assertFalse(session.betaRealismStudioLighting)
        assertFalse(session.betaRealismSurfaceMicro)
    }

    /**
     * Guards against reversed OpenGL triangle winding (which triggers `!gl_FrontFacing` normal flip
     * in the fragment shader and turns meshes dark/inverted). Verifies that > 95% of non-degenerate
     * triangles have geometric CCW face normal aligned with their stored vertex normals!
     */
    private fun assertOutwardWindingMatchesNormals(name: String, mesh: DynamicGlMesh) {
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val idxBuf = mesh.indexBuffer
        idxBuf.position(0)
        var aligned = 0
        var total = 0
        var t = 0
        while (t + 2 < mesh.indexCount) {
            val i0 = idxBuf.get(t).toInt() and 0xFFFF
            val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
            val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
            t += 3

            val b0 = i0 * stride
            val b1 = i1 * stride
            val b2 = i2 * stride

            val e1x = data[b1] - data[b0]
            val e1y = data[b1 + 1] - data[b0 + 1]
            val e1z = data[b1 + 2] - data[b0 + 2]
            val e2x = data[b2] - data[b0]
            val e2y = data[b2 + 1] - data[b0 + 1]
            val e2z = data[b2 + 2] - data[b0 + 2]

            val fx = e1y * e2z - e1z * e2y
            val fy = e1z * e2x - e1x * e2z
            val fz = e1x * e2y - e1y * e2x
            val fLen = sqrt(fx * fx + fy * fy + fz * fz)
            if (fLen < 1e-7f) continue

            val nx = (data[b0 + 3] + data[b1 + 3] + data[b2 + 3]) / 3f
            val ny = (data[b0 + 4] + data[b1 + 4] + data[b2 + 4]) / 3f
            val nz = (data[b0 + 5] + data[b1 + 5] + data[b2 + 5]) / 3f
            val dot = (fx * nx + fy * ny + fz * nz) / fLen
            total++
            if (dot > 0f) aligned++
        }
        val ratio = if (total > 0) aligned.toFloat() / total else 0f
        assertTrue(
            "$name must have outward CCW triangle winding matching stored normals (> 95%), got ${(ratio * 100f).toInt()}% ($aligned/$total)",
            ratio > 0.95f
        )
    }

    private fun assertValidMesh(mesh: DynamicGlMesh) {
        assertTrue("Mesh vertexCount must fit GL_UNSIGNED_SHORT (<= 65535), got ${mesh.vertexCount}", mesh.vertexCount <= 65535)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        for (i in 0 until mesh.vertexCount) {
            val base = i * stride
            for (k in 0 until stride) {
                val value = data[base + k]
                assertTrue("Vertex $i field $k must be finite, got $value", value.isFinite())
            }
            val nx = data[base + 3]
            val ny = data[base + 4]
            val nz = data[base + 5]
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            assertTrue("Vertex $i normal must be unit length (got $len)", len in 0.96f..1.04f)
        }
    }
}
