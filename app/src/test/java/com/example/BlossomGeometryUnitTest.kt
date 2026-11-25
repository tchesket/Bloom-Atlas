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
