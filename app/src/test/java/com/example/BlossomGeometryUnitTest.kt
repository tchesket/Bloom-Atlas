package com.example

import com.example.ui.components.BlossomSdfBodyBuilder
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
                hoodRetract = 1f
            )
        )

        states.forEach { state ->
            scene.update(state, includeLithotomyBody = false)
            scene.coreMeshes.forEach { assertValidMesh(it) }

            scene.update(state, includeLithotomyBody = true)
            scene.blossomSkinMeshes.forEach { assertValidMesh(it) }
            scene.blossomDrapeMeshes.forEach { assertValidMesh(it) }
        }
    }

    @Test
    fun unifiedSdfBodyHasTrueHumanProportionsFlushVulvaPatchAndSafeVertexBudget() {
        val scene = VulvaAnatomySceneMesh()
        scene.update(VulvaDeformationState(), includeLithotomyBody = true)

        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val body = scene.blossomBodyMesh

        // 0. Ensure unified SDF body fits comfortably inside GL_UNSIGNED_SHORT (65,535) with rich resolution (> 6,000 verts)
        assertTrue(
            "Unified SDF body vertex count should be in 6000..55000, got ${body.vertexCount}",
            body.vertexCount in 6000..55000
        )

        var hipMaxX = 0f
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

            if (y in -0.6f..0.8f && x > hipMaxX) hipMaxX = x
            if (y > bodyMaxY) bodyMaxY = y
            if (y < bodyMinY) bodyMinY = y
            if (y in -2.5f..-1.2f && z > kneeMaxZ) kneeMaxZ = z
            if (y < -4.2f && z < footMinZ) footMinZ = z
            if (y in 1.20f..2.60f && x < 0.40f) {
                if (z < bellyMinZ) bellyMinZ = z
                if (z > bellyMaxZ) bellyMaxZ = z
            }
        }

        // 1. Verify realistic 1:1 proportions across the single unified watertight body
        assertTrue("Hip half-width should be realistic 1:1 scale (> 1.60), got $hipMaxX", hipMaxX > 1.60f)
        assertTrue("Torso should extend through full belly and waist (maxY > 3.20), got $bodyMaxY", bodyMaxY > 3.20f)
        assertTrue("Feet should extend naturally to full leg length (minY < -5.20), got $bodyMinY", bodyMinY < -5.20f)
        assertTrue("Reclining abdomen should have full rounded non-flattened depth (> 1.35), got ${bellyMaxZ - bellyMinZ}", (bellyMaxZ - bellyMinZ) > 1.35f)
        assertTrue("Bent knees should rise clearly upward (+Z > 1.95), got $kneeMaxZ", kneeMaxZ > 1.95f)
        assertTrue("Foot soles should rest flush on exam mattress (minZ in -1.20..-1.06), got $footMinZ", footMinZ in -1.20f..-1.06f)

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

    private fun assertValidMesh(mesh: DynamicGlMesh) {
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
