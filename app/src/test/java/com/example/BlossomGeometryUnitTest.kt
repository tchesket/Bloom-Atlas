package com.example

import com.example.ui.components.BlossomBodyBuilder
import com.example.ui.components.BlossomSdfBodyBuilder
import com.example.ui.components.BlossomUpperBodyBuilder
import com.example.ui.components.DynamicGlMesh
import com.example.ui.components.VulvaAnatomySceneMesh
import com.example.ui.components.VulvaDeformationState
import com.example.ui.components.VulvaGlTextureView
import com.example.ui.components.computeBlinkIntervalRangeMs
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
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
                val err = abs(BlossomSdfBodyBuilder.evaluateBodySdf(x, y, z))
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
            "Clitoral hood and inner labia should stay tucked within pelvic profile (archMaxZ <= bellyMaxZ + 0.01), got archMaxZ=$archMaxZ vs bellyMaxZ=$bellyMaxZ",
            archMaxZ <= bellyMaxZ + 0.01f
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

        // 4. Downward head pitch: headPitch = 1f nods anterior face gently downward; negative pitch clamps safely to 0f
        scene.update(VulvaDeformationState(headPitch = -0.8f), includeLithotomyBody = true)
        var maxNegPitchDiff = 0f
        for (i in restHeadCopy.indices) {
            val d = abs(scene.blossomHeadMesh.vertexData[i] - restHeadCopy[i])
            if (d > maxNegPitchDiff) maxNegPitchDiff = d
        }
        assertTrue("Negative headPitch must clamp cleanly to resting 0f (< 1e-4), got $maxNegPitchDiff", maxNegPitchDiff < 1e-4f)

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
