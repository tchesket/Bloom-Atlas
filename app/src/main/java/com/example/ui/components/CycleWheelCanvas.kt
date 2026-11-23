package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CyclePhase
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun CycleWheelCanvas(
    phases: List<CyclePhase>,
    selectedPhaseId: String,
    onSelectPhase: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = phases.firstOrNull { it.id == selectedPhaseId } ?: phases.first()
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    // 4 phase sweep allocations across a 28-day visual wheel
    val sweeps = listOf(64f, 102f, 44f, 150f) // Menstrual, Follicular, Ovulation, Luteal
    val starts = listOf(-90f, -26f, 76f, 120f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.18f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .pointerInput(phases) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val angleRad = atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                        var deg = (angleRad * 180.0 / PI).toFloat()
                        if (deg < -90f) deg += 360f
                        phases.forEachIndexed { idx, phase ->
                            val start = starts[idx]
                            val end = start + sweeps[idx]
                            if (deg in start..end) {
                                onSelectPhase(phase.id)
                            }
                        }
                    }
                }
        ) {
            val diameter = size.minDimension * 0.84f
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            phases.forEachIndexed { index, phase ->
                val isSelected = phase.id == selectedPhaseId
                val color = Color(phase.colorHex)
                val strokeWidth = if (isSelected) 34.dp.toPx() else 22.dp.toPx()

                drawArc(
                    color = color.copy(alpha = if (isSelected) 0.95f else 0.42f),
                    startAngle = starts[index] + 3f,
                    sweepAngle = sweeps[index] - 6f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center editorial medallion
            drawCircle(
                color = surface,
                radius = diameter * 0.34f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = onSurface.copy(alpha = 0.08f),
                radius = diameter * 0.34f,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = 2f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text(
                text = selected.daysRange,
                style = MaterialTheme.typography.labelMedium,
                color = Color(selected.colorHex),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = selected.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
