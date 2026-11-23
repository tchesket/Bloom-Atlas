package com.example.ui.components

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Clinical staging constants & draped exam table cushion builder for Blossom.
 * Blossom's unified seamless 3D lower body (torso, hips, buttocks, legs, feet) and
 * ultra-high-resolution deformable vulva patch live in [BlossomSdfBodyBuilder].
 */
internal object BlossomBodyBuilder {

    const val BLOSSOM_MILESTONE_TAG = "Blossom v3.0"

    const val CUSHION_COLS = 64
    const val CUSHION_ROWS = 64

    // Balanced anatomical perineal elevation above table (-1.14): vulva sits naturally nestled near mattress & between thighs
    const val MATTRESS_TOP_Z = -1.14f

    // -------------------------------------------------------------------------
    // DARK DRAPED CLINICAL EXAM TABLE MATTRESS (CLOSED 3D RECLINING CUSHION)
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
            // y spans past resting toes (-6.40) up behind reclining waist (+4.10)
            val y = -6.40f + v * 10.50f

            val seatHalfW = 2.80f - 0.36f * smoothStep(0.60f, 3.80f, y)
            val topEndT = smoothStep(0.94f, 1.0f, v)
            val botEndT = 1f - smoothStep(0.0f, 0.06f, v)
            val endScale = min(
                cos(topEndT * (PI.toFloat() * 0.5f)).coerceAtLeast(0.03f),
                cos(botEndT * (PI.toFloat() * 0.5f)).coerceAtLeast(0.03f)
            )
            val halfW = seatHalfW * endScale
            val halfThick = 0.28f * endScale

            val yRecline = max(0f, y - 0.72f)
            val centerZ = (MATTRESS_TOP_Z - halfThick) - 0.16f * yRecline - 0.014f * yRecline * yRecline

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

                val buttockIndent = 0.085f * exp(-(((ax - 0.80f) / 0.86f).let { it * it } + ((y + 0.68f) / 0.80f).let { it * it })) * isTopFace
                val backIndent = 0.065f * exp(-((ax / 1.18f).let { it * it } + ((y - 1.60f) / 1.50f).let { it * it })) * isTopFace
                val footIndent = 0.048f * exp(-(((ax - 1.84f) / 0.48f).let { it * it } + ((y + 5.02f) / 0.96f).let { it * it })) * isTopFace

                val drapeFold = (0.022f * sin(y * 3.1f + ax * 1.8f) + 0.012f * cos(y * 7.2f - x * 2.4f)) *
                    smoothStep(1.35f, 2.50f, ax)

                val z = centerZ + halfThick * flatZ - buttockIndent - backIndent - footIndent + drapeFold

                val buttockShadow = exp(-(((ax - 0.78f) / 0.96f).let { it * it } + ((y + 0.68f) / 0.88f).let { it * it })) * 0.54f
                val waistShadow = exp(-((ax / 1.32f).let { it * it } + ((y - 1.60f) / 1.60f).let { it * it })) * 0.44f
                val footShadow = exp(-(((ax - 1.84f) / 0.56f).let { it * it } + ((y + 5.02f) / 1.08f).let { it * it })) * 0.58f
                val undersideAo = 1f - 0.48f * smoothStep(0.10f, 0.85f, -cosT)
                val ao = ((1f - max(buttockShadow, max(waistShadow, footShadow)) * isTopFace) * undersideAo).coerceIn(0.16f, 1f)

                val idx = (r * cols + c) * stride
                data[idx] = x
                data[idx + 1] = y
                data[idx + 2] = z
                data[idx + 6] = 0f
                data[idx + 7] = 0f
                data[idx + 8] = ao
                data[idx + 9] = (0.5f + drapeFold * 14f).coerceIn(0f, 1f)
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
