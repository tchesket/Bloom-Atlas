package com.example.ui.components

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Clinical staging constants, shared reclining table surface curve, and draped exam cushion
 * builder (with integrated ergonomic headrest bolster) for Blossom's full head-to-toe figure.
 */
internal object BlossomBodyBuilder {

    const val BLOSSOM_MILESTONE_TAG = "Blossom Beta v1"

    const val CUSHION_COLS = 64
    const val CUSHION_ROWS = 96

    // Balanced anatomical perineal elevation above table (-1.14): vulva sits naturally nestled near mattress & between thighs
    const val MATTRESS_TOP_Z = -1.14f

    /**
     * C1-continuous clinical reclining exam table surface height Z(y).
     * Matches v3.1 identically for y <= 3.00 so the calibrated lower body is unchanged,
     * then transitions smoothly into a gently inclined upper-back support plane.
     */
    fun mattressSurfaceZ(y: Float): Float {
        if (y <= 0.72f) return MATTRESS_TOP_Z
        val yRec = y - 0.72f
        if (y <= 3.00f) {
            return MATTRESS_TOP_Z - 0.16f * yRec - 0.014f * yRec * yRec
        }
        val zAt3 = MATTRESS_TOP_Z - 0.16f * 2.28f - 0.014f * 2.28f * 2.28f
        val dy = y - 3.00f
        // Exact C1 analytic integral of slope(t) = -0.06 - 0.16384 * exp(-0.42 * t)
        return zAt3 - 0.06f * dy - (0.16384f / 0.42f) * (1f - exp(-0.42f * dy))
    }

    // -------------------------------------------------------------------------
    // FULL-LENGTH DARK DRAPED CLINICAL EXAM TABLE MATTRESS + HEADREST PILLOW
    // -------------------------------------------------------------------------
    fun buildExamCushionMesh(
        buildIndices: (Int, Int) -> ShortArray,
        computeNormals: (FloatArray, Int, Int, Boolean, Boolean) -> Unit
    ): DynamicGlMesh {
        val cols = CUSHION_COLS
        val rows = CUSHION_ROWS
        val mesh = DynamicGlMesh(cols * rows, (cols - 1) * (rows - 1) * 6)
        mesh.setIndices(buildIndices(cols, rows))

        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        for (r in 0 until rows) {
            val v = r.toFloat() / (rows - 1)
            // y spans past resting toes (-6.40) cleanly past the crown of Blossom's head (+8.55)
            val y = -6.40f + v * 14.95f

            // Generous clinical exam mattress width supporting comfortably spread feet, reclining arms, shoulders, and headrest
            val lowerSpreadExtra = 0.42f * (1f - smoothStep(-0.80f, 1.20f, y))
            val seatHalfW = (2.94f + lowerSpreadExtra) - 0.12f * smoothStep(0.60f, 5.00f, y) - 0.24f * smoothStep(5.80f, 8.10f, y)
            val topEndT = smoothStep(0.955f, 1.0f, v)
            val botEndT = 1f - smoothStep(0.0f, 0.045f, v)
            val endScale = min(
                cos(topEndT * (PI.toFloat() * 0.5f)).coerceAtLeast(0.03f),
                cos(botEndT * (PI.toFloat() * 0.5f)).coerceAtLeast(0.03f)
            )
            val halfW = seatHalfW * endScale
            val halfThick = 0.28f * endScale

            val surfaceZ = mattressSurfaceZ(y)
            val centerZ = surfaceZ - halfThick

            for (c in 0 until cols) {
                val u = (c.toFloat() / (cols - 1)) * 2f - 1f
                val theta = u * PI.toFloat()
                val sinT = sin(theta)
                val cosT = cos(theta)

                val flatX = if (sinT >= 0f) {
                    Math.pow(sinT.toDouble(), 0.68).toFloat()
                } else {
                    -Math.pow((-sinT).toDouble(), 0.68).toFloat()
                }
                val flatZ = if (cosT >= 0f) {
                    Math.pow(cosT.toDouble(), 0.46).toFloat()
                } else {
                    -Math.pow((-cosT).toDouble(), 0.46).toFloat()
                }

                val x = halfW * flatX
                val ax = abs(x)
                val isTopFace = smoothStep(-0.10f, 0.45f, cosT)

                // Soft weight indentations beneath buttocks, back, shoulders, hands & feet
                val buttockIndent = 0.085f * exp(-(((ax - 0.80f) / 0.86f).let { it * it } + ((y + 0.68f) / 0.80f).let { it * it })) * isTopFace
                val backIndent = 0.065f * exp(-((ax / 1.24f).let { it * it } + ((y - 2.30f) / 1.95f).let { it * it })) * isTopFace
                val shoulderIndent = 0.056f * exp(-(((ax - 1.05f) / 0.90f).let { it * it } + ((y - 4.70f) / 0.75f).let { it * it })) * isTopFace
                val footIndent = 0.048f * exp(-(((ax - 1.84f) / 0.48f).let { it * it } + ((y + 5.02f) / 0.96f).let { it * it })) * isTopFace

                // Integrated soft ergonomic clinical headrest pillow gently elevating & cradling Blossom's head
                val pillowX = ax / 1.50f
                val pillowY = (y - 6.56f) / 0.90f
                val pillowBase = exp(-(pillowX * pillowX * pillowX * pillowX * 0.72f + pillowY * pillowY * 1.38f)) * isTopFace
                val headCup = exp(-((ax / 0.68f).let { it * it } + ((y - 6.54f) / 0.64f).let { it * it })) * isTopFace
                val headrestLift = (0.58f * pillowBase - 0.14f * headCup).coerceAtLeast(0f)

                val drapeFold = (0.020f * sin(y * 2.9f + ax * 1.8f) + 0.011f * cos(y * 6.8f - x * 2.4f)) *
                    smoothStep(1.45f, 2.55f, ax)

                val z = centerZ + halfThick * flatZ - buttockIndent - backIndent - shoulderIndent - footIndent +
                    headrestLift + drapeFold

                val buttockShadow = exp(-(((ax - 0.78f) / 0.96f).let { it * it } + ((y + 0.68f) / 0.88f).let { it * it })) * 0.54f
                val waistShadow = exp(-((ax / 1.38f).let { it * it } + ((y - 2.30f) / 2.00f).let { it * it })) * 0.42f
                val shoulderShadow = exp(-((ax / 1.72f).let { it * it } + ((y - 4.75f) / 0.85f).let { it * it })) * 0.46f
                val armShadow = exp(-(((ax - 2.08f) / 0.52f).let { it * it } + ((y - 2.20f) / 2.55f).let { it * it })) * 0.42f
                val headShadow = exp(-((ax / 0.84f).let { it * it } + ((y - 6.76f) / 0.80f).let { it * it })) * 0.52f
                val footShadow = exp(-(((ax - 1.84f) / 0.56f).let { it * it } + ((y + 5.02f) / 1.08f).let { it * it })) * 0.58f
                val maxContactShadow = max(
                    max(buttockShadow, waistShadow),
                    max(max(shoulderShadow, armShadow), max(headShadow, footShadow))
                )
                val undersideAo = 1f - 0.48f * smoothStep(0.10f, 0.85f, -cosT)
                val ao = ((1f - maxContactShadow * isTopFace) * undersideAo).coerceIn(0.16f, 1f)

                val idx = (r * cols + c) * stride
                data[idx] = x
                data[idx + 1] = y
                data[idx + 2] = z
                data[idx + 6] = 0f
                data[idx + 7] = 0f
                data[idx + 8] = ao
                data[idx + 9] = (0.5f + drapeFold * 14f + pillowBase * 0.16f).coerceIn(0f, 1f)
            }
        }

        computeNormals(data, cols, rows, false, true)
        mesh.commitVertices()
        return mesh
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
