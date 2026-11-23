package com.example

import com.example.ui.components.BlossomSdfBodyBuilder
import com.example.ui.components.BlossomUpperBodyBuilder
import com.example.ui.components.DynamicGlMesh
import com.example.ui.components.VulvaAnatomySceneMesh
import com.example.ui.components.VulvaDeformationState
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
        assertTrue("Hands mesh should contain 10 sculpted fingers (2240 verts), got ${hands.vertexCount}", hands.vertexCount == 2240)
        assertTrue("Middle fingertips should reach naturally beside upper thighs (handsMinY < -0.55), got $handsMinY", handsMinY < -0.55f)
        assertTrue("Relaxed hands should rest cleanly beside hips without clipping (handsMaxAbsX > 2.30), got $handsMaxAbsX", handsMaxAbsX > 2.30f)

        // Verify clean air-gap between hip and wrist/palm (ZERO hip-merge!), smooth wrist-to-palm arch, & realistic arm fullness
        val hipHandGapSdf = BlossomSdfBodyBuilder.evaluateBodySdf(1.88f, 0.45f, -0.95f)
        assertTrue("Air gap between hip and palm must remain open outside body (SDF > 0.04), got $hipHandGapSdf", hipHandGapSdf > 0.04f)
        val wristTopZ = BlossomSdfBodyBuilder.sampleBodySurfacePoint(2.24f, 0.82f).z
        val palmTopZ = BlossomSdfBodyBuilder.sampleBodySurfacePoint(2.32f, 0.36f).z
        assertTrue(
            "Wrist-to-palm dorsal transition must be smooth with no wrist bump or pancake drop (|wristTopZ=$wristTopZ - palmTopZ=$palmTopZ| < 0.06)",
            abs(wristTopZ - palmTopZ) < 0.06f
        )
        // Measure forearm cross-section width at mid-forearm (y ~ 1.85) & upper arm fullness (y ~ 3.55)
        var midForearmMinX = Float.POSITIVE_INFINITY
        var midForearmMaxX = Float.NEGATIVE_INFINITY
        var midUpperArmMaxX = Float.NEGATIVE_INFINITY
        for (i in 0 until body.vertexCount) {
            val ax = abs(bData[i * stride])
            val y = bData[i * stride + 1]
            if (y in 1.75f..1.95f && ax > 1.55f) {
                if (ax < midForearmMinX) midForearmMinX = ax
                if (ax > midForearmMaxX) midForearmMaxX = ax
            }
            if (y in 3.45f..3.65f && ax > midUpperArmMaxX) {
                midUpperArmMaxX = ax
            }
        }
        val midForearmWidth = midForearmMaxX - midForearmMinX
        assertTrue(
            "Mid-forearm must have realistic non-spindly width (> 0.56), got $midForearmWidth ($midForearmMinX..$midForearmMaxX)",
            midForearmWidth > 0.56f
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
        scene.update(VulvaDeformationState(breastSize = 0.0f), includeLithotomyBody = true)
        val petiteApexZ = scene.blossomBreastsMesh.vertexData[leftApexIdx + 2]
        assertTrue(
            "Breast size slider must visibly scale volume (fullApexZ=$fullApexZ > restApexZ=$restApexZ > petiteApexZ=$petiteApexZ)",
            fullApexZ > restApexZ + 0.10f && restApexZ > petiteApexZ + 0.10f
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
        scene.update(VulvaDeformationState(), includeLithotomyBody = true)

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
