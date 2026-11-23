package com.example

import com.example.ui.components.BlossomArmIkRig
import com.example.ui.components.BlossomSdfBodyBuilder
import com.example.ui.components.DynamicGlMesh
import com.example.ui.components.VulvaAnatomySceneMesh
import com.example.ui.components.VulvaDeformationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class BlossomArmIkRigTest {

    @Test
    fun analyticalTwoBoneIkPreservesExactBoneLengthsAcrossFullArc() {
        val samples = listOf(0.0f, 0.20f, 0.40f, 0.55f, 0.68f, 0.85f, 1.0f)
        for (side in listOf(-1f, 1f)) {
            for (p in samples) {
                val outer = if (p > 0.68f) BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET * ((p - 0.68f) / 0.32f) else 0f
                val sol = BlossomArmIkRig.solveSideArmIk(side, p, outer)
                val upperLen = (sol.elbow - sol.shoulder).len()
                val forearmLen = (sol.wrist - sol.elbow).len()
                assertTrue(
                    "Upper arm length must remain exact at side=$side p=$p: expected ${BlossomArmIkRig.UPPER_ARM_LEN}, got $upperLen",
                    abs(upperLen - BlossomArmIkRig.UPPER_ARM_LEN) < 0.002f
                )
                assertTrue(
                    "Forearm length must remain exact at side=$side p=$p: expected ${BlossomArmIkRig.FOREARM_LEN}, got $forearmLen",
                    abs(forearmLen - BlossomArmIkRig.FOREARM_LEN) < 0.002f
                )
                // Orthonormal right-handed hand frame (det = +1)
                val det = sol.poseHand.axisX.cross(sol.poseHand.axisY).dot(sol.poseHand.axisZ)
                assertTrue(
                    "Pose hand frame must be strictly right-handed (det ~= +1) at side=$side p=$p, got $det",
                    det in 0.99f..1.01f
                )
            }
        }
    }

    @Test
    fun armAndLegSkinCachesAreStrictlyDisjoint() {
        val scene = VulvaAnatomySceneMesh()
        val legSet = scene.blossomLegSkinCache.bodyVertIndices.toHashSet()
        val armSet = scene.blossomArmSkinCache.bodyVertIndices.toHashSet()
        assertTrue("ArmSkinCache should cover arm/forearm/palm vertices (> 600), got ${armSet.size}", armSet.size > 600)
        val overlap = legSet.intersect(armSet)
        assertEquals("ArmSkinCache and LegSkinCache must share 0 body vertices", 0, overlap.size)
    }

    @Test
    fun wristArcClearsAnteriorHipAndFingertipsAlignWithOpenedOuterLabia() {
        for (side in listOf(-1f, 1f)) {
            // Mid-arc hip clearance check
            val midSol = BlossomArmIkRig.solveSideArmIk(side, 0.35f, 0f)
            val hipSdf = BlossomSdfBodyBuilder.evaluateBodySdf(midSol.wrist.x, midSol.wrist.y, midSol.wrist.z)
            assertTrue(
                "Mid-arc wrist must clear anterior hip cleanly (SDF > 0.15) on side=$side, got $hipSdf at ${midSol.wrist}",
                hipSdf > 0.15f
            )

            // Final held pose fingertip contact alignment beside opened outer labia majora
            val indexTip = BlossomArmIkRig.computeFingertipWorldPosition(
                side = side,
                fingerIndex = 1,
                poseProgress = 1.0f,
                liveOuterSpread = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET
            )
            val middleTip = BlossomArmIkRig.computeFingertipWorldPosition(
                side = side,
                fingerIndex = 2,
                poseProgress = 1.0f,
                liveOuterSpread = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET
            )
            val thumbTip = BlossomArmIkRig.computeFingertipWorldPosition(
                side = side,
                fingerIndex = 0,
                poseProgress = 1.0f,
                liveOuterSpread = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET
            )
            val pinkyTip = BlossomArmIkRig.computeFingertipWorldPosition(
                side = side,
                fingerIndex = 4,
                poseProgress = 1.0f,
                liveOuterSpread = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET
            )

            val indexAx = indexTip.x * side
            val middleAx = middleTip.x * side
            assertTrue(
                "Index fingertip must reach opened outer labium on side=$side (ax in 0.22..0.72), got ax=$indexAx, tip=$indexTip",
                indexAx in 0.22f..0.72f
            )
            assertTrue(
                "Middle fingertip must reach opened outer labium on side=$side (ax in 0.22..0.72), got ax=$middleAx, tip=$middleTip",
                middleAx in 0.22f..0.72f
            )
            assertTrue(
                "Index & middle fingertips must sit cleanly in vulva vertical span (y in -0.38..0.38), got indexY=${indexTip.y}, middleY=${middleTip.y}",
                indexTip.y in -0.38f..0.38f && middleTip.y in -0.38f..0.38f
            )
            assertTrue(
                "Index & middle fingertips must sit gently on anterior labial surface (z in 0.04..0.52), got indexZ=${indexTip.z}, middleZ=${middleTip.z}",
                indexTip.z in 0.04f..0.52f && middleTip.z in 0.04f..0.52f
            )
            assertTrue(
                "Thumb should stay softly above/superior to middle tip on side=$side (thumbY=${thumbTip.y} > middleY=${middleTip.y})",
                thumbTip.y > middleTip.y + 0.25f
            )
            assertTrue(
                "Curled pinky should tuck neatly back toward palm out of vestibule sightline (pinkyY=${pinkyTip.y} > middleY=${middleTip.y} + 0.25)",
                pinkyTip.y > middleTip.y + 0.25f
            )
        }
    }

    @Test
    fun allTenFingersAndPalmsClearAnteriorThighsWithZeroClipping() {
        val scene = VulvaAnatomySceneMesh()
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val poses = listOf(
            0.68f to 0.0f,
            0.85f to BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET * 0.5f,
            1.00f to BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET
        )
        for ((p, outer) in poses) {
            scene.update(
                VulvaDeformationState(
                    leftOuter = outer,
                    rightOuter = outer,
                    leftInner = outer,
                    rightInner = outer,
                    selfSpreadPose = p
                ),
                includeLithotomyBody = true
            )

            // 1. Check EVERY vertex of all 10 articulated fingers against Blossom's torso/pelvis/thigh SDF
            val hData = scene.blossomHandsMesh.vertexData
            var minFingerSdf = Float.POSITIVE_INFINITY
            var worstFingerInfo = ""
            for (i in 0 until scene.blossomHandsMesh.vertexCount) {
                val x = hData[i * stride]
                val y = hData[i * stride + 1]
                val z = hData[i * stride + 2]
                val sdf = BlossomSdfBodyBuilder.evaluateBodySdf(x, y, z)
                if (sdf < minFingerSdf) {
                    minFingerSdf = sdf
                    val fingerIdx = (i / (16 * 18)) % 5
                    val sideLabel = if (i < 16 * 18 * 5) "L" else "R"
                    worstFingerInfo = "$sideLabel finger#$fingerIdx at (${"%.2f".format(x)}, ${"%.2f".format(y)}, ${"%.2f".format(z)})"
                }
            }
            assertTrue(
                "ALL 10 fingers must stay strictly outside thighs & body (minFingerSdf > 0.005) at p=$p, got $minFingerSdf ($worstFingerInfo)",
                minFingerSdf > 0.005f
            )

            // 2. Check EVERY posed palm vertex in blossomBodyMesh (wHand > 0.75) AND parametric blossomPalmsMesh clears thighs & abdomen
            val bData = scene.blossomBodyMesh.vertexData
            val armCache = scene.blossomArmSkinCache
            var minPalmVertSdf = Float.POSITIVE_INFINITY
            for (k in armCache.bodyVertIndices.indices) {
                if (armCache.wHand[k] > 0.75f) {
                    val idx = armCache.bodyVertIndices[k] * stride
                    val sdf = BlossomSdfBodyBuilder.evaluateBodySdf(bData[idx], bData[idx + 1], bData[idx + 2])
                    if (sdf < minPalmVertSdf) minPalmVertSdf = sdf
                }
            }
            val pData = scene.blossomPalmsMesh.vertexData
            for (i in 0 until scene.blossomPalmsMesh.vertexCount) {
                val idx = i * stride
                val sdf = BlossomSdfBodyBuilder.evaluateBodySdf(pData[idx], pData[idx + 1], pData[idx + 2])
                if (sdf < minPalmVertSdf) minPalmVertSdf = sdf
            }
            assertTrue(
                "ALL posed palm vertices must stay strictly clear of thighs & body (minPalmVertSdf > 0.005) at p=$p, got $minPalmVertSdf",
                minPalmVertSdf > 0.005f
            )

            // 3. Check wrist & mid-palm centers stay cleanly above abdomen/groin/thigh
            for (side in listOf(-1f, 1f)) {
                val sol = BlossomArmIkRig.solveSideArmIk(side, p, outer)
                val wristSdf = BlossomSdfBodyBuilder.evaluateBodySdf(sol.wrist.x, sol.wrist.y, sol.wrist.z)
                val palmMid = sol.wrist + sol.poseHand.axisY * 0.45f
                val palmSdf = BlossomSdfBodyBuilder.evaluateBodySdf(palmMid.x, palmMid.y, palmMid.z)
                assertTrue(
                    "Wrist must stay cleanly above lower abdomen/hip (SDF > 0.10) at side=$side p=$p, got $wristSdf at ${sol.wrist}",
                    wristSdf > 0.10f
                )
                assertTrue(
                    "Palm center must stay cleanly above groin/thigh (SDF > 0.08) at side=$side p=$p, got $palmSdf at $palmMid",
                    palmSdf > 0.08f
                )
            }
        }
    }

    @Test
    fun posedBodyHandsAndNailsMaintainFiniteUnitNormalsOutwardWindingAndRestIdentity() {
        val scene = VulvaAnatomySceneMesh()
        scene.update(VulvaDeformationState(selfSpreadPose = 0f), includeLithotomyBody = true)
        val restBodyCopy = scene.blossomBodyMesh.vertexData.copyOf()
        val restPalmsCopy = scene.blossomPalmsMesh.vertexData.copyOf()
        val restHandsCopy = scene.blossomHandsMesh.vertexData.copyOf()
        val restNailsCopy = scene.blossomNailsMesh.vertexData.copyOf()

        val testStates = listOf(
            VulvaDeformationState(selfSpreadPose = 0.50f, leftLegSpread = 0.80f, rightLegSpread = 0.80f),
            VulvaDeformationState(
                leftOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                rightOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                leftInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                rightInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                leftLegSpread = 0.80f,
                rightLegSpread = 0.80f,
                selfSpreadPose = 1.0f
            )
        )

        testStates.forEach { state ->
            scene.update(state, includeLithotomyBody = true)
            assertValidMesh(scene.blossomBodyMesh)
            assertValidMesh(scene.blossomPalmsMesh)
            assertValidMesh(scene.blossomHandsMesh)
            assertValidMesh(scene.blossomNailsMesh)
            assertOutwardWinding("blossomPalmsMesh(p=${state.selfSpreadPose})", scene.blossomPalmsMesh)
            assertOutwardWinding("blossomHandsMesh(p=${state.selfSpreadPose})", scene.blossomHandsMesh)
            assertOutwardWinding("blossomNailsMesh(p=${state.selfSpreadPose})", scene.blossomNailsMesh)
        }

        // Returning to p=0 with legs reset must restore exact rest coordinates
        scene.update(VulvaDeformationState(selfSpreadPose = 0f), includeLithotomyBody = true)
        var maxBodyDiff = 0f
        for (i in restBodyCopy.indices) {
            val d = abs(scene.blossomBodyMesh.vertexData[i] - restBodyCopy[i])
            if (d > maxBodyDiff) maxBodyDiff = d
        }
        var maxPalmsDiff = 0f
        for (i in restPalmsCopy.indices) {
            val d = abs(scene.blossomPalmsMesh.vertexData[i] - restPalmsCopy[i])
            if (d > maxPalmsDiff) maxPalmsDiff = d
        }
        var maxHandsDiff = 0f
        for (i in restHandsCopy.indices) {
            val d = abs(scene.blossomHandsMesh.vertexData[i] - restHandsCopy[i])
            if (d > maxHandsDiff) maxHandsDiff = d
        }
        var maxNailsDiff = 0f
        for (i in restNailsCopy.indices) {
            val d = abs(scene.blossomNailsMesh.vertexData[i] - restNailsCopy[i])
            if (d > maxNailsDiff) maxNailsDiff = d
        }
        assertTrue("Rest body identity must hold (< 1e-4), got $maxBodyDiff", maxBodyDiff < 1e-4f)
        assertTrue("Rest palms identity must hold (< 1e-4), got $maxPalmsDiff", maxPalmsDiff < 1e-4f)
        assertTrue("Rest hands identity must hold (< 1e-4), got $maxHandsDiff", maxHandsDiff < 1e-4f)
        assertTrue("Rest nails identity must hold (< 1e-4), got $maxNailsDiff", maxNailsDiff < 1e-4f)
    }

    @Test
    fun spreadPoseHasZeroUpperArmToTableSkinSpikes() {
        val scene = VulvaAnatomySceneMesh()
        scene.update(VulvaDeformationState(selfSpreadPose = 0f), includeLithotomyBody = true)
        val restBody = scene.blossomBodyMesh.vertexData.copyOf()

        scene.update(
            VulvaDeformationState(
                leftOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                rightOuter = BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET,
                leftInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                rightInner = BlossomArmIkRig.SELF_SPREAD_INNER_TARGET,
                selfSpreadPose = 1.0f
            ),
            includeLithotomyBody = true
        )

        val posedBody = scene.blossomBodyMesh.vertexData
        val idxBuf = scene.blossomBodyMesh.indexBuffer
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        idxBuf.position(0)

        var maxStretchRatio = 0f
        var maxEdgeLen = 0f
        var worstInfo = ""

        fun checkEdge(a: Int, b: Int) {
            val ba = a * stride
            val bb = b * stride
            val rdx = restBody[bb] - restBody[ba]
            val rdy = restBody[bb + 1] - restBody[ba + 1]
            val rdz = restBody[bb + 2] - restBody[ba + 2]
            val restLen = sqrt(rdx * rdx + rdy * rdy + rdz * rdz)
            if (restLen < 0.015f) return

            val pdx = posedBody[bb] - posedBody[ba]
            val pdy = posedBody[bb + 1] - posedBody[ba + 1]
            val pdz = posedBody[bb + 2] - posedBody[ba + 2]
            val posedLen = sqrt(pdx * pdx + pdy * pdy + pdz * pdz)
            val ratio = posedLen / restLen
            if (ratio > maxStretchRatio) {
                maxStretchRatio = ratio
                maxEdgeLen = posedLen
                worstInfo = "verts($a,$b) rest=(${"%.2f".format(restBody[ba])},${"%.2f".format(restBody[ba + 1])},${"%.2f".format(restBody[ba + 2])}) restLen=${"%.3f".format(restLen)} posedLen=${"%.3f".format(posedLen)}"
            }
        }

        var t = 0
        while (t + 2 < scene.blossomBodyMesh.indexCount) {
            val i0 = idxBuf.get(t).toInt() and 0xFFFF
            val i1 = idxBuf.get(t + 1).toInt() and 0xFFFF
            val i2 = idxBuf.get(t + 2).toInt() and 0xFFFF
            t += 3
            checkEdge(i0, i1)
            checkEdge(i1, i2)
            checkEdge(i2, i0)
        }

        assertTrue(
            "Spread pose must have ZERO arm-to-table spikes (maxEdgeStretchRatio < 2.60 and maxEdgeLen < 0.30), got ratio=$maxStretchRatio len=$maxEdgeLen ($worstInfo)",
            maxStretchRatio < 2.60f && maxEdgeLen < 0.30f
        )
    }

    private fun assertValidMesh(mesh: DynamicGlMesh) {
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        for (i in 0 until mesh.vertexCount) {
            val base = i * stride
            for (k in 0 until stride) {
                assertTrue("Vertex $i field $k must be finite", data[base + k].isFinite())
            }
            val nx = data[base + 3]
            val ny = data[base + 4]
            val nz = data[base + 5]
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            assertTrue("Vertex $i normal must be unit length, got $len", len in 0.96f..1.04f)
        }
    }

    private fun assertOutwardWinding(name: String, mesh: DynamicGlMesh) {
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
            "$name must have outward CCW winding (> 95%), got ${(ratio * 100f).toInt()}% ($aligned/$total)",
            ratio > 0.95f
        )
    }
}
