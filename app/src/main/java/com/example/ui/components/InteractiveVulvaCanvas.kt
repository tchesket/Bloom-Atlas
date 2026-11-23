package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.AnatomyPart
import com.example.data.model.SensitivityTier
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

data class VulvaToneStyle(
    val name: String,
    val skinHighlight: Color,
    val skinMid: Color,
    val skinShadow: Color,
    val creaseShadow: Color,
    val mucosaLight: Color,
    val mucosaMid: Color,
    val mucosaDeep: Color,
    val vestibuleDeep: Color,
    val glansHighlight: Color,
    val glansBody: Color
)

val VulvaToneStyles: List<VulvaToneStyle> = listOf(
    VulvaToneStyle(
        name = "Ivory Rose",
        skinHighlight = Color(0xFFFAE2D4),
        skinMid = Color(0xFFEEC1AA),
        skinShadow = Color(0xFFC9917A),
        creaseShadow = Color(0xFF915C4D),
        mucosaLight = Color(0xFFF3B0AF),
        mucosaMid = Color(0xFFDA7E84),
        mucosaDeep = Color(0xFFA74E5B),
        vestibuleDeep = Color(0xFF6B2231),
        glansHighlight = Color(0xFFFFD6DA),
        glansBody = Color(0xFFE38392)
    ),
    VulvaToneStyle(
        name = "Warm Peach",
        skinHighlight = Color(0xFFF3CEB4),
        skinMid = Color(0xFFDEA584),
        skinShadow = Color(0xFFB5775B),
        creaseShadow = Color(0xFF7D4838),
        mucosaLight = Color(0xFFE79C96),
        mucosaMid = Color(0xFFC86D6F),
        mucosaDeep = Color(0xFF933E48),
        vestibuleDeep = Color(0xFF5D1D28),
        glansHighlight = Color(0xFFFAC6C4),
        glansBody = Color(0xFFD6727A)
    ),
    VulvaToneStyle(
        name = "Golden Olive",
        skinHighlight = Color(0xFFE5BE98),
        skinMid = Color(0xFFC9966E),
        skinShadow = Color(0xFF9C6949),
        creaseShadow = Color(0xFF673E2B),
        mucosaLight = Color(0xFFD88D88),
        mucosaMid = Color(0xFFB35E62),
        mucosaDeep = Color(0xFF7F343D),
        vestibuleDeep = Color(0xFF4D1922),
        glansHighlight = Color(0xFFF1B8B4),
        glansBody = Color(0xFFC1646B)
    ),
    VulvaToneStyle(
        name = "Caramel Berry",
        skinHighlight = Color(0xFFD29D76),
        skinMid = Color(0xFFB07854),
        skinShadow = Color(0xFF835036),
        creaseShadow = Color(0xFF522D1F),
        mucosaLight = Color(0xFFC97E7B),
        mucosaMid = Color(0xFFA05158),
        mucosaDeep = Color(0xFF6F2B36),
        vestibuleDeep = Color(0xFF41141E),
        glansHighlight = Color(0xFFE3A1A0),
        glansBody = Color(0xFFAD555F)
    ),
    VulvaToneStyle(
        name = "Deep Chestnut",
        skinHighlight = Color(0xFFAE795B),
        skinMid = Color(0xFF89563D),
        skinShadow = Color(0xFF603725),
        creaseShadow = Color(0xFF391D14),
        mucosaLight = Color(0xFFB56E70),
        mucosaMid = Color(0xFF8B444D),
        mucosaDeep = Color(0xFF5C232F),
        vestibuleDeep = Color(0xFF34101A),
        glansHighlight = Color(0xFFD28E93),
        glansBody = Color(0xFF994A56)
    ),
    VulvaToneStyle(
        name = "Rich Espresso",
        skinHighlight = Color(0xFF855640),
        skinMid = Color(0xFF633B2A),
        skinShadow = Color(0xFF432419),
        creaseShadow = Color(0xFF26120C),
        mucosaLight = Color(0xFF9D5B62),
        mucosaMid = Color(0xFF763844),
        mucosaDeep = Color(0xFF4C1C29),
        vestibuleDeep = Color(0xFF2A0C15),
        glansHighlight = Color(0xFFBE7B84),
        glansBody = Color(0xFF863E4D)
    )
)

private data class SensoryRipple(
    val xNorm: Float,
    val yNorm: Float,
    val startNanos: Long,
    val tier: SensitivityTier
)

private data class PointerTrack(
    var lastPos: Offset,
    val startNorm: Offset
)

@Composable
fun InteractiveVulvaCanvas(
    parts: List<AnatomyPart>,
    selectedPartId: String,
    showLabels: Boolean,
    hapticsEnabled: Boolean,
    holdOpen: Boolean,
    resetTrigger: Int,
    toneIndex: Int,
    onSelectPart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val composeHaptic = LocalHapticFeedback.current
    val hapticsEngine = remember(context) { AnatomyHapticsEngine(context) }
    val coroutineScope = rememberCoroutineScope()

    val tone = VulvaToneStyles[toneIndex.coerceIn(0, VulvaToneStyles.lastIndex)]
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Five continuous biomechanical deformation channels (0f = undisturbed resting posture, 1f = comfortably parted)
    val leftOuterSpread = remember { Animatable(0f) }
    val rightOuterSpread = remember { Animatable(0f) }
    val leftInnerOpen = remember { Animatable(0f) }
    val rightInnerOpen = remember { Animatable(0f) }
    val hoodRetraction = remember { Animatable(0f) }

    val latestHoldOpen by rememberUpdatedState(holdOpen)
    val latestHapticsEnabled by rememberUpdatedState(hapticsEnabled)
    val latestParts by rememberUpdatedState(parts)
    val latestOnSelectPart by rememberUpdatedState(onSelectPart)

    var activePointerCount by remember { mutableStateOf(0) }
    val ripples = remember { mutableStateListOf<SensoryRipple>() }
    var frameNanos by remember { mutableLongStateOf(0L) }

    // Drive smooth ripple decay only while ripples exist
    LaunchedEffect(ripples.size > 0) {
        if (ripples.isEmpty()) return@LaunchedEffect
        while (ripples.isNotEmpty()) {
            withFrameNanos { now ->
                frameNanos = now
                ripples.removeAll { (now - it.startNanos) > 540_000_000L }
            }
        }
    }

    fun springBackAll() {
        val tissueSpring = spring<Float>(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessLow
        )
        coroutineScope.launch { leftOuterSpread.animateTo(0f, tissueSpring) }
        coroutineScope.launch { rightOuterSpread.animateTo(0f, tissueSpring) }
        coroutineScope.launch { leftInnerOpen.animateTo(0f, tissueSpring) }
        coroutineScope.launch { rightInnerOpen.animateTo(0f, tissueSpring) }
        coroutineScope.launch { hoodRetraction.animateTo(0f, tissueSpring) }
    }

    // Spring back when Reset is tapped or Hold Open is turned off while untouched
    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            springBackAll()
        }
    }
    LaunchedEffect(holdOpen) {
        if (!holdOpen && activePointerCount == 0) {
            springBackAll()
        }
    }

    fun triggerSensoryResponse(part: AnatomyPart, nx: Float, ny: Float, isDrag: Boolean) {
        latestOnSelectPart(part.id)
        val now = System.nanoTime()
        if (!isDrag || ripples.lastOrNull()?.let { now - it.startNanos > 110_000_000L } != false) {
            ripples.add(SensoryRipple(nx, ny, now, part.sensitivityTier))
            if (ripples.size > 10) ripples.removeAt(0)
        }
        if (latestHapticsEnabled) {
            if (isDrag) {
                hapticsEngine.triggerDragPulse(part.sensitivityTier)
            } else {
                hapticsEngine.triggerTap(part.sensitivityTier)
                if (part.sensitivityTier == SensitivityTier.ULTRA) {
                    composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .testTag("interactive_vulva_canvas"),
        shape = RoundedCornerShape(24.dp),
        color = tone.skinShadow,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription =
                        "Interactive anatomical vulva rendering. Spread outer and inner labia with one or two fingers, slide the clitoral hood upward, or glide across to explore sensitivity."
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val tracks = mutableMapOf<PointerId, PointerTrack>()
                        var lastHitPartId: String? = null
                        var lastDragFeedbackMs = 0L

                        while (true) {
                            val event = awaitPointerEvent()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val pressedChanges = event.changes.filter { it.pressed }
                            activePointerCount = pressedChanges.size

                            // Remove lifted pointers
                            val activeIds = pressedChanges.map { it.id }.toSet()
                            tracks.keys.retainAll(activeIds)

                            if (pressedChanges.isEmpty()) {
                                if (!latestHoldOpen) {
                                    springBackAll()
                                }
                                break
                            }

                            var dLeftOuter = 0f
                            var dRightOuter = 0f
                            var dLeftInner = 0f
                            var dRightInner = 0f
                            var dHood = 0f

                            pressedChanges.forEach { change ->
                                change.consume()
                                val pos = change.position
                                val nx = (pos.x / w).coerceIn(0f, 1f)
                                val ny = (pos.y / h).coerceIn(0f, 1f)
                                val existing = tracks[change.id]

                                if (existing == null) {
                                    tracks[change.id] = PointerTrack(
                                        lastPos = pos,
                                        startNorm = Offset(nx, ny)
                                    )
                                    val hit = hitTestDeformedVulva(
                                        nx = nx,
                                        ny = ny,
                                        leftOuter = leftOuterSpread.value,
                                        rightOuter = rightOuterSpread.value,
                                        leftInner = leftInnerOpen.value,
                                        rightInner = rightInnerOpen.value,
                                        hood = hoodRetraction.value,
                                        parts = latestParts
                                    )
                                    if (hit != null) {
                                        lastHitPartId = hit.id
                                        triggerSensoryResponse(hit, nx, ny, isDrag = false)
                                    }
                                } else {
                                    val dxNorm = (pos.x - existing.lastPos.x) / w
                                    val dyNorm = (pos.y - existing.lastPos.y) / h
                                    existing.lastPos = pos

                                    // 1. Clitoral hood retraction (upward drag near upper commissure / hood)
                                    val inHoodZone = existing.startNorm.x in 0.30f..0.70f &&
                                        existing.startNorm.y in 0.14f..0.44f
                                    if (inHoodZone && abs(dyNorm) > abs(dxNorm) * 0.55f) {
                                        dHood += (-dyNorm) * 4.4f
                                    }

                                    // 2. Left & Right Labia Parting (independent 1-finger or simultaneous 2-finger spread)
                                    val inLabiaVerticalSpan = existing.startNorm.y in 0.22f..0.86f
                                    if (inLabiaVerticalSpan) {
                                        val isLeftPointer = existing.startNorm.x < 0.50f
                                        if (isLeftPointer) {
                                            val outwardPull = -dxNorm // pulling left opens left side
                                            val startedNearInner = existing.startNorm.x in 0.36f..0.50f
                                            dLeftOuter += outwardPull * if (startedNearInner) 2.2f else 3.8f
                                            dLeftInner += outwardPull * if (startedNearInner) 4.4f else 2.9f
                                        } else {
                                            val outwardPull = dxNorm // pulling right opens right side
                                            val startedNearInner = existing.startNorm.x in 0.50f..0.64f
                                            dRightOuter += outwardPull * if (startedNearInner) 2.2f else 3.8f
                                            dRightInner += outwardPull * if (startedNearInner) 4.4f else 2.9f
                                        }
                                    }

                                    // 3. Live glide hit-testing & sensory feedback
                                    val nowMs = System.currentTimeMillis()
                                    val movedEnough = hypot(dxNorm, dyNorm) > 0.004f
                                    if (movedEnough && nowMs - lastDragFeedbackMs >= 85L) {
                                        val hit = hitTestDeformedVulva(
                                            nx = nx,
                                            ny = ny,
                                            leftOuter = leftOuterSpread.value,
                                            rightOuter = rightOuterSpread.value,
                                            leftInner = leftInnerOpen.value,
                                            rightInner = rightInnerOpen.value,
                                            hood = hoodRetraction.value,
                                            parts = latestParts
                                        )
                                        if (hit != null) {
                                            val changedPart = hit.id != lastHitPartId
                                            if (changedPart || nowMs - lastDragFeedbackMs >= 135L) {
                                                lastHitPartId = hit.id
                                                lastDragFeedbackMs = nowMs
                                                triggerSensoryResponse(hit, nx, ny, isDrag = true)
                                            }
                                        }
                                    }
                                }
                            }

                            // Apply live continuous deltas immediately
                            if (dLeftOuter != 0f || dRightOuter != 0f || dLeftInner != 0f || dRightInner != 0f || dHood != 0f) {
                                val nextLeftOuter = (leftOuterSpread.value + dLeftOuter).coerceIn(0f, 1f)
                                val nextRightOuter = (rightOuterSpread.value + dRightOuter).coerceIn(0f, 1f)
                                // Inner folds naturally accompany strong outer spread slightly, plus direct inner pull
                                val minLeftInnerFromOuter = (nextLeftOuter - 0.18f).coerceAtLeast(0f) * 0.72f
                                val minRightInnerFromOuter = (nextRightOuter - 0.18f).coerceAtLeast(0f) * 0.72f
                                val nextLeftInner = (leftInnerOpen.value + dLeftInner)
                                    .coerceAtLeast(minLeftInnerFromOuter)
                                    .coerceIn(0f, 1f)
                                val nextRightInner = (rightInnerOpen.value + dRightInner)
                                    .coerceAtLeast(minRightInnerFromOuter)
                                    .coerceIn(0f, 1f)
                                val nextHood = (hoodRetraction.value + dHood).coerceIn(0f, 1f)

                                coroutineScope.launch {
                                    leftOuterSpread.snapTo(nextLeftOuter)
                                    rightOuterSpread.snapTo(nextRightOuter)
                                    leftInnerOpen.snapTo(nextLeftInner)
                                    rightInnerOpen.snapTo(nextRightInner)
                                    hoodRetraction.snapTo(nextHood)
                                }
                            }
                        }
                    }
                }
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            val lOuter = leftOuterSpread.value
            val rOuter = rightOuterSpread.value
            val lInner = leftInnerOpen.value
            val rInner = rightInnerOpen.value
            val hood = hoodRetraction.value
            val nowNanos = frameNanos

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRealisticDeformableVulva(
                    tone = tone,
                    leftOuter = lOuter,
                    rightOuter = rOuter,
                    leftInner = lInner,
                    rightInner = rInner,
                    hoodRetract = hood
                )

                // Soft live fingertip sensory ripples
                ripples.forEach { ripple ->
                    val elapsed = ((nowNanos - ripple.startNanos).coerceAtLeast(0L)) / 540_000_000f
                    if (elapsed in 0f..1f) {
                        val center = Offset(ripple.xNorm * size.width, ripple.yNorm * size.height)
                        val maxRadius = when (ripple.tier) {
                            SensitivityTier.ULTRA -> 52.dp.toPx()
                            SensitivityTier.HIGH -> 42.dp.toPx()
                            SensitivityTier.MODERATE -> 34.dp.toPx()
                        }
                        val currentRadius = maxRadius * (0.28f + 0.72f * elapsed)
                        val alpha = (1f - elapsed) * (1f - elapsed) * 0.48f
                        val tint = when (ripple.tier) {
                            SensitivityTier.ULTRA -> Color(0xFFFFD1DC)
                            SensitivityTier.HIGH -> Color(0xFFFFE0D8)
                            SensitivityTier.MODERATE -> Color(0xFFFFF4E6)
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(tint.copy(alpha = alpha), Color.Transparent),
                                center = center,
                                radius = currentRadius
                            ),
                            radius = currentRadius,
                            center = center
                        )
                        drawCircle(
                            color = tint.copy(alpha = alpha * 0.85f),
                            radius = currentRadius * 0.62f,
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                }
            }

            // Optional Live-Tracking Anatomical Labels (strictly hidden unless Labels toggle is ON)
            if (showLabels) {
                val vestibuleExposure = ((lInner + rInner) * 0.5f).coerceIn(0f, 1f)

                parts.forEach { part ->
                    val livePos = computeDeformedPartNormalizedPos(
                        part = part,
                        leftOuter = lOuter,
                        rightOuter = rOuter,
                        leftInner = lInner,
                        rightInner = rInner,
                        hood = hood
                    )
                    // Deep landmarks smoothly appear as they are uncovered (or if explicitly selected)
                    val isSelected = part.id == selectedPartId
                    val visibilityAlpha = when (part.id) {
                        "ext_glans" -> ((hood - 0.12f) / 0.35f).coerceIn(if (isSelected) 0.92f else 0.25f, 1f)
                        "ext_urethra", "ext_introitus" -> ((vestibuleExposure - 0.12f) / 0.35f).coerceIn(if (isSelected) 0.92f else 0.25f, 1f)
                        else -> 1f
                    }

                    val xPx = (livePos.x * widthPx).roundToInt()
                    val yPx = (livePos.y * heightPx).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(xPx - 24.dp.roundToPx(), yPx - 24.dp.roundToPx()) }
                            .size(48.dp)
                            .clip(CircleShape)
                            .testTag("vulva_label_pin_${part.id}")
                            .semantics {
                                contentDescription = "Inspect ${part.name}, marker ${part.number}"
                            }
                            .clickable {
                                triggerSensoryResponse(part, livePos.x, livePos.y, isDrag = false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 28.dp else 22.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (isSelected) primaryColor else surfaceColor).copy(alpha = 0.92f * visibilityAlpha)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.2.dp,
                                    color = (if (isSelected) Color.White else tone.creaseShadow).copy(alpha = visibilityAlpha),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = part.number.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = (if (isSelected) Color.White else tone.creaseShadow).copy(alpha = visibilityAlpha)
                            )
                        }
                    }

                    if (isSelected) {
                        val labelOffsetY = if (livePos.y > 0.78f) -44.dp else 26.dp
                        val clampedX = livePos.x.coerceIn(0.24f, 0.76f)
                        Surface(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (clampedX * widthPx).roundToInt() - 74.dp.roundToPx(),
                                        yPx + labelOffsetY.roundToPx()
                                    )
                                }
                                .size(width = 148.dp, height = 28.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF341816).copy(alpha = 0.90f),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = part.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun computeDeformedPartNormalizedPos(
    part: AnatomyPart,
    leftOuter: Float,
    rightOuter: Float,
    leftInner: Float,
    rightInner: Float,
    hood: Float
): Offset {
    return when (part.id) {
        "ext_mons" -> Offset(0.50f, 0.12f)
        "ext_hood" -> Offset(0.50f, (0.285f - hood * 0.058f).coerceAtLeast(0.20f))
        "ext_glans" -> Offset(0.50f, 0.335f)
        "ext_labia_majora" -> Offset((0.28f - leftOuter * 0.105f).coerceAtLeast(0.12f), 0.50f)
        "ext_labia_minora" -> Offset((0.575f + rightInner * 0.135f + rightOuter * 0.04f).coerceAtMost(0.79f), 0.52f)
        "ext_urethra" -> Offset(0.50f, 0.495f)
        "ext_introitus" -> Offset(0.50f, 0.655f)
        "ext_perineum" -> Offset(0.50f, 0.875f)
        else -> Offset(part.normX, part.normY)
    }
}

private fun hitTestDeformedVulva(
    nx: Float,
    ny: Float,
    leftOuter: Float,
    rightOuter: Float,
    leftInner: Float,
    rightInner: Float,
    hood: Float,
    parts: List<AnatomyPart>
): AnatomyPart? {
    fun byId(id: String): AnatomyPart? = parts.firstOrNull { it.id == id }

    val vestibuleExposure = ((leftInner + rightInner) * 0.5f).coerceIn(0f, 1f)
    val cleftLeftEdge = 0.50f - (0.014f + leftInner * 0.125f + leftOuter * 0.035f)
    val cleftRightEdge = 0.50f + (0.014f + rightInner * 0.125f + rightOuter * 0.035f)
    val innerLeftOuterEdge = cleftLeftEdge - 0.068f
    val innerRightOuterEdge = cleftRightEdge + 0.068f

    return when {
        ny < 0.19f -> byId("ext_mons")
        ny > 0.80f -> byId("ext_perineum")
        // Clitoral glans (directly touchable once hood is retracted!) vs Clitoral hood
        abs(nx - 0.50f) < 0.095f && ny in 0.22f..0.39f -> {
            if (hood >= 0.22f && ny >= (0.31f - hood * 0.025f)) {
                byId("ext_glans")
            } else {
                byId("ext_hood")
            }
        }
        // Central vestibule & openings once inner labia are parted
        nx in cleftLeftEdge..cleftRightEdge && ny in 0.39f..0.77f -> {
            if (vestibuleExposure >= 0.16f) {
                when {
                    ny < 0.56f -> byId("ext_urethra")
                    else -> byId("ext_introitus")
                }
            } else {
                byId("ext_labia_minora")
            }
        }
        // Inner labia folds
        (nx in innerLeftOuterEdge..cleftLeftEdge || nx in cleftRightEdge..innerRightOuterEdge) &&
            ny in 0.30f..0.76f -> byId("ext_labia_minora")
        // Outer labia cushions
        else -> byId("ext_labia_majora")
    }
}

/**
 * High-realism studio-airbrushed anatomical renderer (outline-free, subsurface-lit, multi-pass
 * feathered shading) for a single, undisturbed vulva that deforms organically across 5 channels.
 */
private fun DrawScope.drawRealisticDeformableVulva(
    tone: VulvaToneStyle,
    leftOuter: Float,
    rightOuter: Float,
    leftInner: Float,
    rightInner: Float,
    hoodRetract: Float
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f

    val avgOuter = (leftOuter + rightOuter) * 0.5f
    val avgInner = (leftInner + rightInner) * 0.5f
    val cavityExposure = (avgInner * 0.80f + avgOuter * 0.20f).coerceIn(0f, 1f)
    // Warm subsurface hemoglobin bounce tint for realistic living skin terminators
    val sssWarmBounce = lerp(tone.creaseShadow, Color(0xFF9C2C36), 0.44f)

    // -------------------------------------------------------------------------
    // LAYER 1: Natural Pelvis, Inner Thigh Volume, Soft Inguinal Creases & Mons
    // -------------------------------------------------------------------------
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(tone.skinHighlight, tone.skinMid, tone.skinShadow),
            center = Offset(cx - w * 0.08f, h * 0.22f),
            radius = h * 0.96f
        )
    )

    // Soft organic dermal mottling / natural studio tonal warmth (prevents flat synthetic look)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(sssWarmBounce.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(cx + w * 0.22f, h * 0.62f),
            radius = w * 0.42f
        ),
        topLeft = Offset(cx - w * 0.16f, h * 0.28f),
        size = Size(w * 0.78f, h * 0.68f)
    )

    // Rounded lateral inner thighs with natural studio roll-off
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(tone.skinHighlight.copy(alpha = 0.70f), tone.skinMid.copy(alpha = 0.24f), Color.Transparent),
            center = Offset(w * 0.04f, h * 0.54f),
            radius = w * 0.42f
        ),
        topLeft = Offset(-w * 0.28f, h * 0.20f),
        size = Size(w * 0.58f, h * 0.78f)
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(tone.skinHighlight.copy(alpha = 0.52f), tone.skinMid.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(w * 0.96f, h * 0.54f),
            radius = w * 0.42f
        ),
        topLeft = Offset(w * 0.70f, h * 0.20f),
        size = Size(w * 0.58f, h * 0.78f)
    )

    // Multi-pass feathered inguinal (groin) crease troughs (NO crisp cartoon strokes!)
    val leftGroin = Path().apply {
        moveTo(w * 0.04f, h * 0.19f)
        cubicTo(
            w * (0.14f - leftOuter * 0.036f), h * 0.44f,
            w * (0.19f - leftOuter * 0.028f), h * 0.70f,
            w * 0.31f, h * 0.94f
        )
    }
    val rightGroin = Path().apply {
        moveTo(w * 0.96f, h * 0.19f)
        cubicTo(
            w * (0.86f + rightOuter * 0.036f), h * 0.44f,
            w * (0.81f + rightOuter * 0.028f), h * 0.70f,
            w * 0.69f, h * 0.94f
        )
    }
    listOf(0.11f to 0.06f, 0.065f to 0.10f, 0.032f to 0.14f, 0.014f to 0.16f).forEach { (widthFrac, alpha) ->
        drawPath(leftGroin, color = tone.creaseShadow.copy(alpha = alpha), style = Stroke(w * widthFrac, cap = StrokeCap.Round))
        drawPath(rightGroin, color = tone.creaseShadow.copy(alpha = alpha * 1.08f), style = Stroke(w * widthFrac, cap = StrokeCap.Round))
    }

    // Soft sculpted Mons Pubis rounded volume across the upper third
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                tone.skinHighlight.copy(alpha = 0.88f),
                tone.skinMid.copy(alpha = 0.42f),
                Color.Transparent
            ),
            center = Offset(cx - w * 0.05f, h * 0.10f),
            radius = w * 0.48f
        ),
        topLeft = Offset(w * 0.06f, -h * 0.08f),
        size = Size(w * 0.88f, h * 0.38f)
    )
    // Subtle natural follicular soft shadow stippling across upper mons
    for (i in 0..42) {
        val fx = ((i * 73 + 19) % 100) / 100f
        val fy = ((i * 41 + 11) % 100) / 100f
        val px = cx + (fx - 0.5f) * w * 0.56f
        val py = h * (0.02f + fy * 0.19f)
        val centerWeight = (1f - (abs(px - cx) / (w * 0.30f))).coerceIn(0f, 1f) *
            (1f - (py / (h * 0.22f))).coerceIn(0f, 1f)
        if (centerWeight > 0.08f) {
            drawCircle(
                color = tone.creaseShadow.copy(alpha = 0.09f * centerWeight),
                radius = w * 0.0045f,
                center = Offset(px, py)
            )
        }
    }

    // Warm, softly shadowed perineal bridge & delicate median raphe below posterior commissure
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(tone.skinMid.copy(alpha = 0.72f), tone.skinShadow.copy(alpha = 0.44f), Color.Transparent),
            center = Offset(cx, h * 0.88f),
            radius = w * 0.32f
        ),
        topLeft = Offset(cx - w * 0.30f, h * 0.77f),
        size = Size(w * 0.60f, h * 0.24f)
    )
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(tone.creaseShadow.copy(alpha = 0.20f), Color.Transparent),
            startY = h * 0.81f,
            endY = h * 0.96f
        ),
        start = Offset(cx, h * 0.81f),
        end = Offset(cx, h * 0.96f),
        strokeWidth = w * 0.012f,
        cap = StrokeCap.Round
    )

    // -------------------------------------------------------------------------
    // LAYER 2: Recessed Vestibular Basin, Urethral Papilla & Textured Introitus
    // -------------------------------------------------------------------------
    val vestLeftX = cx - w * (0.016f + leftInner * 0.145f + leftOuter * 0.042f)
    val vestRightX = cx + w * (0.016f + rightInner * 0.145f + rightOuter * 0.042f)

    val vestibulePath = Path().apply {
        moveTo(cx, h * 0.265f)
        cubicTo(
            vestLeftX - w * 0.026f, h * 0.36f,
            vestLeftX - w * 0.018f, h * 0.68f,
            cx, h * 0.795f
        )
        cubicTo(
            vestRightX + w * 0.018f, h * 0.68f,
            vestRightX + w * 0.026f, h * 0.36f,
            cx, h * 0.265f
        )
        close()
    }

    val cavityRimColor = lerp(tone.vestibuleDeep, tone.mucosaDeep, 0.22f + cavityExposure * 0.62f)
    val cavityCoreColor = lerp(tone.vestibuleDeep, tone.mucosaMid, 0.16f + cavityExposure * 0.68f)
    val cavityWarmHighlight = lerp(tone.mucosaMid, tone.mucosaLight, cavityExposure * 0.72f)
    drawPath(
        path = vestibulePath,
        brush = Brush.radialGradient(
            colors = listOf(cavityWarmHighlight, cavityCoreColor, cavityRimColor, tone.vestibuleDeep),
            center = Offset(cx - w * 0.01f, h * 0.50f),
            radius = h * (0.19f + cavityExposure * 0.16f)
        )
    )

    clipPath(vestibulePath) {
        // Subtle moist ambient vestibular sheen
        if (cavityExposure > 0.06f) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(tone.glansHighlight.copy(alpha = 0.26f * cavityExposure), Color.Transparent),
                    center = Offset(cx - w * 0.016f, h * 0.44f),
                    radius = w * (0.065f + cavityExposure * 0.09f)
                ),
                topLeft = Offset(cx - w * 0.16f, h * 0.34f),
                size = Size(w * 0.32f, h * 0.26f)
            )
        }

        // Urethral Papilla & Delicate Meatus Dimple
        val urethraAlpha = ((cavityExposure - 0.07f) / 0.40f).coerceIn(0f, 1f)
        if (urethraAlpha > 0.01f) {
            val uy = h * 0.492f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tone.mucosaLight.copy(alpha = 0.88f * urethraAlpha),
                        tone.mucosaMid.copy(alpha = 0.56f * urethraAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx - w * 0.005f, uy - h * 0.004f),
                    radius = w * 0.048f
                ),
                topLeft = Offset(cx - w * 0.048f, uy - h * 0.036f),
                size = Size(w * 0.096f, h * 0.072f)
            )
            // Soft recessed stellate urethral opening
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2E0B14).copy(alpha = 0.86f * urethraAlpha),
                        tone.vestibuleDeep.copy(alpha = 0.60f * urethraAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, uy),
                    radius = w * 0.014f
                ),
                topLeft = Offset(cx - w * 0.014f, uy - h * 0.014f),
                size = Size(w * 0.028f, h * 0.028f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.28f * urethraAlpha),
                radius = w * 0.005f,
                center = Offset(cx - w * 0.010f, uy - h * 0.011f)
            )
        }

        // Vaginal Introitus (layered recessed cavity + scalloped hymenal corona cushions + rugae)
        val introitusAlpha = ((cavityExposure - 0.04f) / 0.34f).coerceIn(0f, 1f)
        if (introitusAlpha > 0.01f) {
            val iy = h * 0.655f
            val introRx = w * (0.020f + cavityExposure * 0.070f)
            val introRy = h * (0.048f + cavityExposure * 0.048f)

            // Recessed vestibular perimeter shadow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tone.vestibuleDeep.copy(alpha = 0.94f * introitusAlpha),
                        tone.mucosaDeep.copy(alpha = 0.65f * introitusAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, iy),
                    radius = introRy * 1.48f
                ),
                topLeft = Offset(cx - introRx * 1.55f, iy - introRy * 1.32f),
                size = Size(introRx * 3.10f, introRy * 2.64f)
            )

            // Deep organic vaginal entrance
            val canalPath = Path().apply {
                moveTo(cx, iy - introRy)
                cubicTo(cx - introRx * 1.16f, iy - introRy * 0.52f, cx - introRx * 1.08f, iy + introRy * 0.64f, cx, iy + introRy)
                cubicTo(cx + introRx * 1.08f, iy + introRy * 0.64f, cx + introRx * 1.16f, iy - introRy * 0.52f, cx, iy - introRy)
                close()
            }
            drawPath(
                path = canalPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E050B).copy(alpha = 0.98f * introitusAlpha),
                        tone.vestibuleDeep.copy(alpha = 0.94f * introitusAlpha),
                        tone.mucosaDeep.copy(alpha = 0.72f * introitusAlpha)
                    ),
                    center = Offset(cx, iy - introRy * 0.06f),
                    radius = introRy * 1.18f
                )
            )

            // Soft sculpted hymenal corona mucosal petals around lower lateral rim (no harsh strokes!)
            val petalOffsets = listOf(
                Offset(-0.74f, 0.14f),
                Offset(-0.56f, 0.56f),
                Offset(-0.18f, 0.80f),
                Offset(0.24f, 0.78f),
                Offset(0.58f, 0.52f),
                Offset(0.74f, 0.10f)
            )
            petalOffsets.forEach { p ->
                val px = cx + p.x * introRx
                val py = iy + p.y * introRy
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tone.mucosaLight.copy(alpha = 0.74f * introitusAlpha),
                            tone.mucosaMid.copy(alpha = 0.46f * introitusAlpha),
                            Color.Transparent
                        ),
                        center = Offset(px - introRx * 0.08f, py - introRy * 0.06f),
                        radius = introRx * 0.44f
                    ),
                    topLeft = Offset(px - introRx * 0.38f, py - introRy * 0.24f),
                    size = Size(introRx * 0.76f, introRy * 0.48f)
                )
            }

            // Soft moist fourchette specular shimmer
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.26f * introitusAlpha), Color.Transparent),
                    center = Offset(cx, iy + introRy * 0.86f),
                    radius = introRx * 0.72f
                ),
                topLeft = Offset(cx - introRx * 0.65f, iy + introRy * 0.66f),
                size = Size(introRx * 1.30f, introRy * 0.36f)
            )
        }
    }

    // -------------------------------------------------------------------------
    // LAYER 3: Glistening Clitoral Glans & Soft Frenulum V-Cradle
    // -------------------------------------------------------------------------
    val glansCenterY = h * 0.330f
    val glansRx = w * (0.032f + hoodRetract * 0.011f)
    val glansRy = h * (0.034f + hoodRetract * 0.011f)

    // Softly shaded frenulum V-folds sweeping down into inner labia
    val frenulumLeft = Path().apply {
        moveTo(cx - glansRx * 0.32f, glansCenterY + glansRy * 0.62f)
        quadraticTo(
            cx - w * (0.028f + leftInner * 0.056f), glansCenterY + h * 0.054f,
            vestLeftX + w * 0.010f, h * 0.435f
        )
    }
    val frenulumRight = Path().apply {
        moveTo(cx + glansRx * 0.32f, glansCenterY + glansRy * 0.62f)
        quadraticTo(
            cx + w * (0.028f + rightInner * 0.056f), glansCenterY + h * 0.054f,
            vestRightX - w * 0.010f, h * 0.435f
        )
    }
    drawPath(frenulumLeft, color = tone.mucosaDeep.copy(alpha = 0.42f), style = Stroke(w * 0.024f, cap = StrokeCap.Round))
    drawPath(frenulumRight, color = tone.mucosaDeep.copy(alpha = 0.42f), style = Stroke(w * 0.024f, cap = StrokeCap.Round))
    drawPath(frenulumLeft, color = tone.mucosaLight.copy(alpha = 0.78f), style = Stroke(w * 0.012f, cap = StrokeCap.Round))
    drawPath(frenulumRight, color = tone.mucosaLight.copy(alpha = 0.78f), style = Stroke(w * 0.012f, cap = StrokeCap.Round))

    // Deep sculpted recess shadow behind glans
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(tone.vestibuleDeep.copy(alpha = 0.78f), Color.Transparent),
            center = Offset(cx, glansCenterY + glansRy * 0.24f),
            radius = glansRy * 1.55f
        ),
        topLeft = Offset(cx - glansRx * 1.55f, glansCenterY - glansRy * 1.20f),
        size = Size(glansRx * 3.10f, glansRy * 2.65f)
    )

    // 3D subsurface-lit pearl glans
    val glansLitCore = lerp(tone.glansBody, tone.glansHighlight, 0.42f + hoodRetract * 0.58f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(glansLitCore, tone.glansBody, tone.mucosaDeep),
            center = Offset(cx - glansRx * 0.28f, glansCenterY - glansRy * 0.26f),
            radius = glansRy * 1.30f
        ),
        topLeft = Offset(cx - glansRx, glansCenterY - glansRy),
        size = Size(glansRx * 2f, glansRy * 2f)
    )
    // Dual moist specular highlights (soft bloom + crisp wet highlight dot)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.24f + hoodRetract * 0.40f), Color.Transparent),
            center = Offset(cx - glansRx * 0.26f, glansCenterY - glansRy * 0.30f),
            radius = glansRx * 0.58f
        ),
        topLeft = Offset(cx - glansRx * 0.64f, glansCenterY - glansRy * 0.62f),
        size = Size(glansRx * 0.80f, glansRy * 0.58f)
    )

    // -------------------------------------------------------------------------
    // LAYER 4: Organic Ruffled Translucent Inner Labia (No Harsh Outlines!)
    // -------------------------------------------------------------------------
    drawDeformableInnerLabium(
        isLeft = true,
        tone = tone,
        sssWarmBounce = sssWarmBounce,
        innerOpen = leftInner,
        outerSpread = leftOuter,
        hoodRetract = hoodRetract
    )
    drawDeformableInnerLabium(
        isLeft = false,
        tone = tone,
        sssWarmBounce = sssWarmBounce,
        innerOpen = rightInner,
        outerSpread = rightOuter,
        hoodRetract = hoodRetract
    )

    // -------------------------------------------------------------------------
    // LAYER 5: Sculpted Retractable Clitoral Hood (Seamless Skin-to-Mucosa)
    // -------------------------------------------------------------------------
    val hoodLift = hoodRetract * h * 0.072f
    val hoodSpread = (leftInner + rightInner) * 0.5f * w * 0.036f
    val hoodArchBottomY = h * 0.354f - hoodLift
    val hoodTopY = h * 0.195f - hoodLift * 0.22f

    // Multi-pass soft cast shadow from hood arch onto the glans underneath
    val hoodShadowAlpha = (0.54f * (1f - hoodRetract * 0.68f)).coerceAtLeast(0.10f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(tone.vestibuleDeep.copy(alpha = hoodShadowAlpha), Color.Transparent),
            center = Offset(cx, hoodArchBottomY + h * 0.008f),
            radius = w * (0.088f + hoodSpread)
        ),
        topLeft = Offset(cx - w * 0.09f - hoodSpread, hoodArchBottomY - h * 0.024f),
        size = Size(w * 0.18f + hoodSpread * 2f, h * 0.064f)
    )

    val hoodPath = Path().apply {
        moveTo(cx, hoodTopY)
        cubicTo(
            cx - w * 0.082f - hoodSpread, hoodTopY + h * 0.028f,
            cx - w * 0.094f - hoodSpread, hoodArchBottomY - h * 0.016f,
            cx - w * 0.068f - hoodSpread, hoodArchBottomY + h * 0.015f
        )
        quadraticTo(
            cx, hoodArchBottomY - h * (0.028f + hoodRetract * 0.015f),
            cx + w * 0.068f + hoodSpread, hoodArchBottomY + h * 0.015f
        )
        cubicTo(
            cx + w * 0.094f + hoodSpread, hoodArchBottomY - h * 0.016f,
            cx + w * 0.082f + hoodSpread, hoodTopY + h * 0.028f,
            cx, hoodTopY
        )
        close()
    }
    drawPath(
        path = hoodPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                tone.skinMid,
                lerp(tone.skinMid, tone.mucosaMid, 0.45f),
                lerp(tone.mucosaMid, tone.mucosaDeep, 0.55f)
            ),
            startY = hoodTopY,
            endY = hoodArchBottomY + h * 0.018f
        )
    )
    // Soft dome specular across the hood arch
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                lerp(tone.skinHighlight, tone.glansHighlight, 0.35f).copy(alpha = 0.68f),
                Color.Transparent
            ),
            center = Offset(cx - w * 0.016f, (hoodTopY + hoodArchBottomY) * 0.48f),
            radius = w * 0.078f
        ),
        topLeft = Offset(cx - w * 0.072f, hoodTopY + h * 0.012f),
        size = Size(w * 0.144f, (hoodArchBottomY - hoodTopY).coerceAtLeast(h * 0.04f))
    )
    // Natural retracted horizontal soft skin-fold shadow when lifted
    if (hoodRetract > 0.10f) {
        val foldY = hoodTopY + h * 0.032f
        val foldPath = Path().apply {
            moveTo(cx - w * 0.072f, foldY + h * 0.012f)
            quadraticTo(cx, foldY - h * 0.012f, cx + w * 0.072f, foldY + h * 0.012f)
        }
        drawPath(foldPath, color = tone.creaseShadow.copy(alpha = 0.14f * hoodRetract), style = Stroke(w * 0.026f, cap = StrokeCap.Round))
        drawPath(foldPath, color = tone.creaseShadow.copy(alpha = 0.24f * hoodRetract), style = Stroke(w * 0.010f, cap = StrokeCap.Round))
    }

    // -------------------------------------------------------------------------
    // LAYER 6: Seamless Feathered Plump Outer Labia Cushions (Labia Majora)
    // -------------------------------------------------------------------------
    drawDeformableOuterLabium(
        isLeft = true,
        tone = tone,
        sssWarmBounce = sssWarmBounce,
        outerSpread = leftOuter,
        innerOpen = leftInner
    )
    drawDeformableOuterLabium(
        isLeft = false,
        tone = tone,
        sssWarmBounce = sssWarmBounce,
        outerSpread = rightOuter,
        innerOpen = rightInner
    )

    // Soft, feathered resting central pudendal cleft shadow (disappears naturally as labia part)
    val restClosedFactor = (1f - (avgOuter * 1.45f + avgInner * 1.25f)).coerceIn(0f, 1f)
    if (restClosedFactor > 0.03f) {
        val cleftLine = Path().apply {
            moveTo(cx, h * 0.250f)
            cubicTo(cx - w * 0.005f, h * 0.42f, cx + w * 0.005f, h * 0.62f, cx, h * 0.785f)
        }
        drawPath(cleftLine, color = sssWarmBounce.copy(alpha = 0.24f * restClosedFactor), style = Stroke(w * 0.044f, cap = StrokeCap.Round))
        drawPath(cleftLine, color = tone.creaseShadow.copy(alpha = 0.36f * restClosedFactor), style = Stroke(w * 0.016f, cap = StrokeCap.Round))
        drawPath(cleftLine, color = tone.vestibuleDeep.copy(alpha = 0.48f * restClosedFactor), style = Stroke(w * 0.005f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawDeformableInnerLabium(
    isLeft: Boolean,
    tone: VulvaToneStyle,
    sssWarmBounce: Color,
    innerOpen: Float,
    outerSpread: Float,
    hoodRetract: Float
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f
    val dir = if (isLeft) -1f else 1f

    val innerShift = w * (0.008f + innerOpen * 0.142f + outerSpread * 0.040f)
    val foldWidth = w * (0.060f + innerOpen * 0.050f)
    val topAnchorY = h * (0.292f - hoodRetract * 0.025f)

    // Gentle organic asymmetric multi-lobe scalloping
    val lobe1 = if (isLeft) w * 0.014f else w * 0.018f
    val lobe2 = if (isLeft) w * 0.008f else w * 0.012f

    val medialXTop = cx + dir * (w * 0.020f + innerShift * 0.45f)
    val medialXMid = cx + dir * innerShift
    val outerPeak1 = cx + dir * (innerShift + foldWidth * 0.98f + lobe1)
    val outerPeak2 = cx + dir * (innerShift + foldWidth * 0.86f + lobe2)

    val foldPath = Path().apply {
        moveTo(medialXTop, topAnchorY)
        cubicTo(
            outerPeak1, h * 0.36f,
            outerPeak1 - dir * lobe2 * 0.5f, h * 0.45f,
            outerPeak2 + dir * lobe1 * 0.45f, h * 0.54f
        )
        cubicTo(
            outerPeak2, h * 0.63f,
            cx + dir * (innerShift + foldWidth * 0.38f), h * 0.725f,
            cx + dir * w * 0.006f, h * 0.785f
        )
        cubicTo(
            medialXMid + dir * w * 0.004f, h * 0.69f,
            medialXMid - dir * w * 0.006f, h * 0.49f,
            medialXTop, topAnchorY
        )
        close()
    }

    // Soft subsurface cast shadow underneath the fold (feathered, never a hard ink stroke)
    drawPath(foldPath, color = tone.vestibuleDeep.copy(alpha = 0.14f), style = Stroke(w * 0.032f, cap = StrokeCap.Round))
    drawPath(foldPath, color = sssWarmBounce.copy(alpha = 0.22f), style = Stroke(w * 0.014f, cap = StrokeCap.Round))

    // Rich translucent rose-mauve mucosal body with natural outer-edge pigmentation gradient
    val minX = minOf(medialXMid, outerPeak1) - w * 0.02f
    val maxX = maxOf(medialXMid, outerPeak1) + w * 0.02f
    val edgePigment = lerp(tone.mucosaDeep, tone.creaseShadow, 0.36f)
    drawPath(
        path = foldPath,
        brush = Brush.horizontalGradient(
            colors = if (isLeft) {
                listOf(edgePigment, tone.mucosaDeep, tone.mucosaMid, tone.mucosaLight)
            } else {
                listOf(tone.mucosaLight, tone.mucosaMid, tone.mucosaDeep, edgePigment)
            },
            startX = minX,
            endX = maxX
        )
    )

    // Moist curved mucosal specular sheen along the rolling fold surface
    clipPath(foldPath) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    tone.glansHighlight.copy(alpha = 0.34f + innerOpen * 0.26f),
                    tone.mucosaLight.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(cx + dir * (innerShift + foldWidth * 0.48f), h * 0.48f),
                radius = w * 0.075f
            ),
            topLeft = Offset(cx + dir * (innerShift + foldWidth * 0.48f) - w * 0.055f, h * 0.33f),
            size = Size(w * 0.11f, h * 0.38f)
        )
    }
}

private fun DrawScope.drawDeformableOuterLabium(
    isLeft: Boolean,
    tone: VulvaToneStyle,
    sssWarmBounce: Color,
    outerSpread: Float,
    innerOpen: Float
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f
    val dir = if (isLeft) -1f else 1f

    val spreadPx = w * (outerSpread * 0.175f + innerOpen * 0.028f)
    val medialEdgeX = cx + dir * (w * 0.020f + spreadPx)
    val lateralEdgeX = cx + dir * (w * 0.305f + spreadPx * 0.42f)
    val crestX = (medialEdgeX + lateralEdgeX) * 0.50f

    val cushionPath = Path().apply {
        moveTo(cx + dir * w * 0.048f, h * 0.172f)
        cubicTo(
            lateralEdgeX + dir * w * 0.032f, h * 0.27f,
            lateralEdgeX + dir * w * 0.016f, h * 0.64f,
            cx + dir * (w * 0.115f + spreadPx * 0.22f), h * 0.838f
        )
        quadraticTo(
            cx + dir * (w * 0.038f + spreadPx * 0.15f), h * 0.858f,
            cx + dir * (w * 0.016f + spreadPx * 0.25f), h * 0.795f
        )
        cubicTo(
            medialEdgeX + dir * w * 0.012f, h * 0.66f,
            medialEdgeX + dir * w * 0.016f, h * 0.36f,
            cx + dir * (w * 0.024f + spreadPx * 0.25f), h * 0.242f
        )
        quadraticTo(
            cx + dir * w * 0.022f, h * 0.200f,
            cx + dir * w * 0.048f, h * 0.172f
        )
        close()
    }

    // Feathered soft ambient occlusion & warm subsurface shadow under the medial curve (no hard edge!)
    val aoStrength = (1f - outerSpread * 0.36f).coerceIn(0.45f, 1f)
    drawPath(cushionPath, color = tone.creaseShadow.copy(alpha = 0.12f * aoStrength), style = Stroke(w * 0.056f, cap = StrokeCap.Round))
    drawPath(cushionPath, color = sssWarmBounce.copy(alpha = 0.20f * aoStrength), style = Stroke(w * 0.026f, cap = StrokeCap.Round))

    // 3D subsurface dermal fill rolling smoothly from lit crest into shadowed lateral & medial slopes
    drawPath(
        path = cushionPath,
        brush = Brush.radialGradient(
            colors = listOf(tone.skinHighlight, tone.skinMid, tone.skinShadow),
            center = Offset(
                crestX - if (isLeft) w * 0.024f else w * 0.038f,
                h * 0.43f
            ),
            radius = h * 0.40f
        )
    )

    // Soft satin-skin studio crest highlight along plump vertical fullness
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                tone.skinHighlight.copy(alpha = if (isLeft) 0.64f else 0.50f),
                Color.Transparent
            ),
            center = Offset(crestX - w * 0.015f, h * 0.44f),
            radius = w * 0.17f
        ),
        topLeft = Offset(crestX - w * 0.12f, h * 0.23f),
        size = Size(w * 0.24f, h * 0.48f)
    )

    // Subtle warm natural medial margin feathering (soft cutaneous-to-mucosal transition)
    val medialBorder = Path().apply {
        moveTo(cx + dir * (w * 0.024f + spreadPx * 0.25f), h * 0.255f)
        cubicTo(
            medialEdgeX + dir * w * 0.016f, h * 0.37f,
            medialEdgeX + dir * w * 0.012f, h * 0.65f,
            cx + dir * (w * 0.018f + spreadPx * 0.25f), h * 0.79f
        )
    }
    drawPath(medialBorder, color = sssWarmBounce.copy(alpha = 0.22f), style = Stroke(w * 0.022f, cap = StrokeCap.Round))
}
