package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.data.repository.DiversityStudyGenerator
import kotlin.math.sin

/**
 * Renders a richly shaded, anatomical editorial illustration for the Illustrated Feed
 * and Interactive Spectrum Lab—depicting natural diversity in labia majora fullness,
 * asymmetric labia minora contours, scalloped/smooth edges, clitoral hood drape,
 * and authentic skin/mucosal color palettes.
 */
@Composable
fun DiversityStudyCanvas(
    innerLeftProjection: Float,
    innerRightProjection: Float,
    outerFullness: Float,
    hoodCoverage: Float,
    ruffleFactor: Float,
    verticalLength: Float,
    paletteIndex: Int,
    seed: Int,
    modifier: Modifier = Modifier
) {
    val palettes = DiversityStudyGenerator.naturalPalettes
    val palette = palettes[paletteIndex.coerceIn(0, palettes.lastIndex)]

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        val skinHighlight = lerp(palette.outerBase, Color.White, 0.24f)
        val skinShadow = lerp(palette.outerBase, palette.accentLine, 0.38f)
        val mucosaHighlight = lerp(palette.innerBase, Color.White, 0.22f)
        val mucosaDeep = lerp(palette.innerBase, palette.accentLine, 0.45f)

        // Warm archival paper plate backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(palette.cardTint, lerp(palette.cardTint, palette.outerBase, 0.28f)),
                center = Offset(cx, h * 0.48f),
                radius = size.maxDimension * 0.72f
            )
        )

        val topY = h * (0.11f + (1f - verticalLength) * 0.07f)
        val bottomY = h * (0.89f - (1f - verticalLength) * 0.07f)
        val spanY = bottomY - topY

        // Soft anatomical pelvic & inner thigh vignette framing
        val pelvicBase = Path().apply {
            moveTo(w * 0.16f, topY - h * 0.04f)
            cubicTo(w * 0.34f, topY - h * 0.07f, w * 0.66f, topY - h * 0.07f, w * 0.84f, topY - h * 0.04f)
            cubicTo(w * 0.90f, h * 0.40f, w * 0.88f, h * 0.72f, w * 0.76f, bottomY + h * 0.05f)
            cubicTo(w * 0.60f, bottomY + h * 0.07f, w * 0.40f, bottomY + h * 0.07f, w * 0.24f, bottomY + h * 0.05f)
            cubicTo(w * 0.12f, h * 0.72f, w * 0.10f, h * 0.40f, w * 0.16f, topY - h * 0.04f)
            close()
        }
        drawPath(
            pelvicBase,
            brush = Brush.radialGradient(
                colors = listOf(palette.outerBase.copy(alpha = 0.88f), skinShadow.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(cx, h * 0.48f),
                radius = w * 0.48f
            )
        )

        // Sculpted 3D Labia Majora cushions (left & right with realistic fullness shading)
        val outerSpread = w * (0.17f + outerFullness * 0.16f)
        val leftMajora = Path().apply {
            moveTo(cx, topY)
            cubicTo(
                cx - outerSpread * 1.05f, topY + spanY * 0.14f,
                cx - outerSpread * 1.15f, topY + spanY * 0.66f,
                cx - outerSpread * 0.42f, bottomY - spanY * 0.04f
            )
            quadraticTo(cx - outerSpread * 0.16f, bottomY + spanY * 0.02f, cx, bottomY)
            cubicTo(
                cx - outerSpread * 0.44f, bottomY - spanY * 0.18f,
                cx - outerSpread * 0.50f, topY + spanY * 0.34f,
                cx, topY
            )
            close()
        }
        val rightMajora = Path().apply {
            moveTo(cx, topY)
            cubicTo(
                cx + outerSpread * 1.05f, topY + spanY * 0.14f,
                cx + outerSpread * 1.15f, topY + spanY * 0.66f,
                cx + outerSpread * 0.42f, bottomY - spanY * 0.04f
            )
            quadraticTo(cx + outerSpread * 0.16f, bottomY + spanY * 0.02f, cx, bottomY)
            cubicTo(
                cx + outerSpread * 0.44f, bottomY - spanY * 0.18f,
                cx + outerSpread * 0.50f, topY + spanY * 0.34f,
                cx, topY
            )
            close()
        }

        drawPath(
            leftMajora,
            brush = Brush.horizontalGradient(
                colors = listOf(skinShadow.copy(alpha = 0.85f), skinHighlight, palette.outerBase, skinShadow.copy(alpha = 0.78f)),
                startX = cx - outerSpread * 1.12f,
                endX = cx
            )
        )
        drawPath(
            rightMajora,
            brush = Brush.horizontalGradient(
                colors = listOf(skinShadow.copy(alpha = 0.78f), palette.outerBase, skinHighlight, skinShadow.copy(alpha = 0.85f)),
                startX = cx,
                endX = cx + outerSpread * 1.12f
            )
        )
        drawPath(leftMajora, color = palette.accentLine.copy(alpha = 0.45f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
        drawPath(rightMajora, color = palette.accentLine.copy(alpha = 0.45f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))

        // Inner Vestibule recessed tissue depth
        val vestibuleW = w * (0.068f + (innerLeftProjection + innerRightProjection) * 0.032f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(mucosaDeep.copy(alpha = 0.82f), palette.innerBase.copy(alpha = 0.68f), Color.Transparent),
                center = Offset(cx, topY + spanY * 0.54f),
                radius = spanY * 0.36f
            ),
            topLeft = Offset(cx - vestibuleW, topY + spanY * 0.16f),
            size = Size(vestibuleW * 2f, spanY * 0.70f)
        )

        // Vaginal introitus subtle shadowed depth
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(palette.accentLine.copy(alpha = 0.75f), mucosaDeep.copy(alpha = 0.55f)),
                center = Offset(cx, topY + spanY * 0.66f),
                radius = w * 0.07f
            ),
            topLeft = Offset(cx - w * 0.042f, topY + spanY * 0.56f),
            size = Size(w * 0.084f, spanY * 0.18f)
        )

        // Inner Labia (Labia Minora) — realistic shaded organic asymmetric folds
        val innerTopY = topY + spanY * 0.13f
        val innerBottomY = bottomY - spanY * 0.11f
        val innerSpanY = innerBottomY - innerTopY

        fun buildShadedInnerFold(side: Float, projection: Float): Path {
            val maxReach = w * (0.036f + projection * 0.195f)
            val waveAmp = w * 0.022f * ruffleFactor * (0.45f + projection * 0.55f)
            val phase = (seed % 19) * 0.42f + if (side > 0f) 1.25f else 0f
            val steps = 20

            return Path().apply {
                moveTo(cx + side * (w * 0.012f), innerTopY)
                for (i in 1..steps) {
                    val t = i.toFloat() / steps
                    val y = innerTopY + innerSpanY * t
                    val envelope = sin(t * Math.PI).toFloat() * (1.12f - 0.28f * t)
                    val ruffleWave = sin((t * 3.8 * Math.PI + phase).toDouble()).toFloat() * waveAmp * envelope
                    val x = cx + side * (w * 0.016f + maxReach * envelope + ruffleWave)
                    lineTo(x, y)
                }
                cubicTo(
                    cx + side * w * 0.042f, innerBottomY - innerSpanY * 0.22f,
                    cx + side * w * 0.036f, innerTopY + innerSpanY * 0.28f,
                    cx + side * (w * 0.012f), innerTopY
                )
                close()
            }
        }

        val leftFold = buildShadedInnerFold(-1f, innerLeftProjection)
        val rightFold = buildShadedInnerFold(1f, innerRightProjection)

        drawPath(
            leftFold,
            brush = Brush.horizontalGradient(
                colors = listOf(mucosaDeep, palette.innerBase, mucosaHighlight),
                startX = cx - w * 0.23f,
                endX = cx
            )
        )
        drawPath(
            rightFold,
            brush = Brush.horizontalGradient(
                colors = listOf(mucosaHighlight, palette.innerBase, mucosaDeep),
                startX = cx,
                endX = cx + w * 0.23f
            )
        )
        drawPath(leftFold, color = palette.accentLine.copy(alpha = 0.68f), style = Stroke(width = 2.0f, join = StrokeJoin.Round))
        drawPath(rightFold, color = palette.accentLine.copy(alpha = 0.68f), style = Stroke(width = 2.0f, join = StrokeJoin.Round))

        // Clitoral Glans + Hood Shadow Pocket + Hood Drape
        val glansY = topY + spanY * 0.26f
        val glansRadius = w * 0.028f
        val hoodDrop = glansRadius * (0.15f + hoodCoverage * 1.70f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mucosaHighlight, palette.innerBase, mucosaDeep),
                center = Offset(cx - glansRadius * 0.25f, glansY - glansRadius * 0.25f),
                radius = glansRadius * 1.3f
            ),
            radius = glansRadius,
            center = Offset(cx, glansY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = glansRadius * 0.26f,
            center = Offset(cx - glansRadius * 0.28f, glansY - glansRadius * 0.28f)
        )

        val hoodPath = Path().apply {
            moveTo(cx - w * 0.072f, glansY + hoodDrop * 0.32f)
            cubicTo(
                cx - w * 0.060f, glansY - glansRadius * 2.3f,
                cx + w * 0.060f, glansY - glansRadius * 2.3f,
                cx + w * 0.072f, glansY + hoodDrop * 0.32f
            )
            quadraticTo(cx, glansY - glansRadius * 1.15f + hoodDrop, cx - w * 0.072f, glansY + hoodDrop * 0.32f)
            close()
        }
        drawPath(
            hoodPath,
            brush = Brush.verticalGradient(
                colors = listOf(skinHighlight, palette.outerBase, palette.innerBase.copy(alpha = 0.72f)),
                startY = glansY - glansRadius * 2.1f,
                endY = glansY + hoodDrop * 0.45f
            )
        )
        drawPath(hoodPath, color = palette.accentLine.copy(alpha = 0.65f), style = Stroke(width = 2.0f, join = StrokeJoin.Round))
    }
}
