package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.AnatomyPart
import com.example.data.model.AnatomyViewType
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.MedicalPlateAsset
import com.example.data.model.SensitivityTier
import com.example.data.repository.WikimediaMediaRepository
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// Warm realistic medical-atlas watercolor/gouache illustration palette
private val PlatePaperLight = Color(0xFFFCF6F0)
private val PlatePaperEdge = Color(0xFFF3E5D8)
private val SkinWarmLight = Color(0xFFF2CCB5)
private val SkinWarmMid = Color(0xFFE0AB8E)
private val SkinWarmShadow = Color(0xFFB87B60)
private val MucosaRoseLight = Color(0xFFE79A92)
private val MucosaRoseMid = Color(0xFFCC716C)
private val MucosaRoseDeep = Color(0xFF964244)
private val VestibuleDeep = Color(0xFF6B2B30)
private val MuscleRedOuter = Color(0xFFC9605A)
private val MuscleRedDeep = Color(0xFF913638)
private val EndometriumPlush = Color(0xFFE6918A)
private val OvaryCream = Color(0xFFF4DFBE)
private val OvaryAmber = Color(0xFFD6A86E)
private val ErectilePlumLight = Color(0xFFC2729C)
private val ErectilePlumMid = Color(0xFF944771)
private val ErectilePlumDeep = Color(0xFF622649)
private val BladderGoldLight = Color(0xFFEBD1A0)
private val BladderGoldMid = Color(0xFFCFA566)
private val BoneIvory = Color(0xFFECE2D2)
private val BoneShadow = Color(0xFFCBBBA4)
private val PelvicMuscleSage = Color(0xFF7A9481)
private val PelvicMuscleDeep = Color(0xFF4A6655)
private val RectumTaupe = Color(0xFFC49E8E)
private val RectumDeep = Color(0xFF8C6558)
private val AtlasInk = Color(0xFF4B2926)

@Composable
fun InteractiveAnatomyCanvas(
    viewType: AnatomyViewType,
    parts: List<AnatomyPart>,
    selectedPartId: String,
    showLabels: Boolean,
    useMedicalPlate: Boolean,
    medicalPlate: MedicalPlateAsset?,
    hapticsEnabled: Boolean,
    touchExploreMode: Boolean,
    onSelectPart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val composeHaptic = LocalHapticFeedback.current
    val hapticsEngine = remember(context) { AnatomyHapticsEngine(context) }
    val imageLoader = remember(context) { WikimediaMediaRepository.getImageLoader(context) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    var activeTouchPoint by remember(viewType) { mutableStateOf<Offset?>(null) }

    val pulseTransition = rememberInfiniteTransition(label = "hotspot_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun selectPartWithHaptics(part: AnatomyPart, isDrag: Boolean) {
        onSelectPart(part.id)
        if (hapticsEnabled) {
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

    fun findNearestPart(offset: Offset, widthPx: Float, heightPx: Float): AnatomyPart? {
        if (widthPx <= 0f || heightPx <= 0f || parts.isEmpty()) return null
        val nx = (offset.x / widthPx).coerceIn(0f, 1f)
        val ny = (offset.y / heightPx).coerceIn(0f, 1f)
        val nearest = parts.minByOrNull { hypot(it.normX - nx, it.normY - ny) } ?: return null
        val dist = hypot(nearest.normX - nx, nearest.normY - ny)
        return if (dist <= 0.24f) nearest else null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = PlatePaperLight,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PlatePaperLight, PlatePaperEdge)
                        )
                    )
                    .pointerInput(parts, touchExploreMode, hapticsEnabled) {
                        detectTapGestures { tapOffset ->
                            val hit = findNearestPart(tapOffset, size.width.toFloat(), size.height.toFloat())
                            if (hit != null) {
                                activeTouchPoint = tapOffset
                                selectPartWithHaptics(hit, isDrag = false)
                            }
                        }
                    }
                    .then(
                        if (touchExploreMode) {
                            Modifier.pointerInput(parts, hapticsEnabled) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        activeTouchPoint = start
                                        val hit = findNearestPart(start, size.width.toFloat(), size.height.toFloat())
                                        if (hit != null) selectPartWithHaptics(hit, isDrag = true)
                                    },
                                    onDragEnd = { activeTouchPoint = null },
                                    onDragCancel = { activeTouchPoint = null },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val pos = change.position
                                        activeTouchPoint = pos
                                        val hit = findNearestPart(pos, size.width.toFloat(), size.height.toFloat())
                                        if (hit != null) selectPartWithHaptics(hit, isDrag = true)
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()
                val selectedPart = parts.firstOrNull { it.id == selectedPartId }

                // Base Layer: Authentic OpenStax/Wikimedia Medical Plate (with seamless local Canvas fallback)
                if (useMedicalPlate && medicalPlate != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(medicalPlate.imageUrl)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = "${viewType.title} medical textbook illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        loading = {
                            FallbackVectorPlateCanvas(viewType = viewType, selectedPartId = selectedPartId)
                        },
                        error = {
                            FallbackVectorPlateCanvas(viewType = viewType, selectedPartId = selectedPartId)
                        }
                    )
                } else {
                    FallbackVectorPlateCanvas(viewType = viewType, selectedPartId = selectedPartId)
                }

                // Overlay Canvas: Plate border, active sensory glow aura, and live Touch-Explore fingertip pulse
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val inset = size.minDimension * 0.025f
                    drawRoundRect(
                        color = AtlasInk.copy(alpha = 0.14f),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(width = 1.8f)
                    )

                    if (showLabels && selectedPart != null) {
                        val center = Offset(
                            selectedPart.normX * size.width,
                            selectedPart.normY * size.height
                        )
                        val auraColor = when (selectedPart.sensitivityTier) {
                            SensitivityTier.ULTRA -> Color(0xFFD93B5C)
                            SensitivityTier.HIGH -> primaryColor
                            SensitivityTier.MODERATE -> Color(0xFF3A6655)
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    auraColor.copy(alpha = 0.36f),
                                    auraColor.copy(alpha = 0.0f)
                                ),
                                center = center,
                                radius = 38.dp.toPx() * pulseScale * 0.80f
                            ),
                            radius = 38.dp.toPx() * pulseScale * 0.80f,
                            center = center
                        )
                    }

                    activeTouchPoint?.let { pt ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE6496C).copy(alpha = 0.42f),
                                    Color.Transparent
                                ),
                                center = pt,
                                radius = 44.dp.toPx()
                            ),
                            radius = 44.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Touch-Explore live instruction pill at top-center
                if (touchExploreMode) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryColor.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Glide finger across anatomy to feel nerve sensitivity",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                // Accessible 48dp touch hotspot pins (hidden by default unless Labels is toggled ON)
                if (showLabels) {
                    parts.forEach { part ->
                        val isSelected = part.id == selectedPartId
                        val xPx = (part.normX * widthPx).roundToInt()
                        val yPx = (part.normY * heightPx).roundToInt()

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(xPx - 24.dp.roundToPx(), yPx - 24.dp.roundToPx()) }
                                .size(48.dp)
                                .clip(CircleShape)
                                .testTag("anatomy_hotspot_${part.id}")
                                .semantics {
                                    contentDescription = "Inspect ${part.name}, marker ${part.number}, ${part.sensitivityTier.label}"
                                }
                                .clickable {
                                    activeTouchPoint = Offset(xPx.toFloat(), yPx.toFloat())
                                    selectPartWithHaptics(part, isDrag = false)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 30.dp else 24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> primaryColor
                                            part.sensitivityTier == SensitivityTier.ULTRA -> Color(0xFFFFEDF0).copy(alpha = 0.92f)
                                            else -> surface.copy(alpha = 0.90f)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.5.dp,
                                        color = if (isSelected) Color.White else AtlasInk.copy(alpha = 0.78f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = part.number.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else AtlasInk
                                )
                            }
                        }

                        if (isSelected) {
                            val labelOffsetY = if (part.normY > 0.78f) -46.dp else 28.dp
                            val clampedX = part.normX.coerceIn(0.26f, 0.74f)
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
                                color = AtlasInk.copy(alpha = 0.92f),
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

        if (useMedicalPlate && medicalPlate != null) {
            val sourcePrefix = if (viewType == AnatomyViewType.EXTERNAL_VULVA) {
                "Reference photo"
            } else {
                "Medical plate"
            }
            Text(
                text = "$sourcePrefix: ${medicalPlate.attribution.take(48)} · ${medicalPlate.license}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 6.dp)
            )
        }
    }
}

@Composable
private fun FallbackVectorPlateCanvas(
    viewType: AnatomyViewType,
    selectedPartId: String
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (viewType) {
            AnatomyViewType.EXTERNAL_VULVA -> drawIllustratedExternalVulva(selectedPartId)
            AnatomyViewType.INTERNAL_FRONT -> drawIllustratedInternalFront(selectedPartId)
            AnatomyViewType.CLITORAL_NETWORK -> drawIllustratedClitoralNetwork(selectedPartId)
            AnatomyViewType.PELVIC_SIDE -> drawIllustratedPelvicSide(selectedPartId)
        }
    }
}

private fun DrawScope.drawIllustratedExternalVulva(selectedId: String) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f

    // 1. Soft anatomical pelvic & inner thigh silhouette background
    val pelvicSilhouette = Path().apply {
        moveTo(w * 0.14f, h * 0.08f)
        cubicTo(w * 0.32f, h * 0.05f, w * 0.68f, h * 0.05f, w * 0.86f, h * 0.08f)
        cubicTo(w * 0.93f, h * 0.36f, w * 0.91f, h * 0.68f, w * 0.80f, h * 0.92f)
        cubicTo(w * 0.66f, h * 0.94f, w * 0.34f, h * 0.94f, w * 0.20f, h * 0.92f)
        cubicTo(w * 0.09f, h * 0.68f, w * 0.07f, h * 0.36f, w * 0.14f, h * 0.08f)
        close()
    }
    drawPath(
        pelvicSilhouette,
        brush = Brush.radialGradient(
            colors = listOf(SkinWarmLight, SkinWarmMid.copy(alpha = 0.86f), SkinWarmShadow.copy(alpha = 0.55f)),
            center = Offset(cx, h * 0.44f),
            radius = w * 0.56f
        )
    )

    // Subtle groin / thigh crease lines for anatomical realism
    val leftCrease = Path().apply {
        moveTo(w * 0.15f, h * 0.22f)
        cubicTo(w * 0.11f, h * 0.48f, w * 0.14f, h * 0.72f, w * 0.24f, h * 0.90f)
    }
    val rightCrease = Path().apply {
        moveTo(w * 0.85f, h * 0.22f)
        cubicTo(w * 0.89f, h * 0.48f, w * 0.86f, h * 0.72f, w * 0.76f, h * 0.90f)
    }
    drawPath(leftCrease, color = SkinWarmShadow.copy(alpha = 0.45f), style = Stroke(width = 3.2f, cap = StrokeCap.Round))
    drawPath(rightCrease, color = SkinWarmShadow.copy(alpha = 0.45f), style = Stroke(width = 3.2f, cap = StrokeCap.Round))

    // 2. Mons Pubis — softly mounded fatty cushion with subtle light highlight
    val monsPath = Path().apply {
        moveTo(w * 0.23f, h * 0.14f)
        cubicTo(w * 0.36f, h * 0.07f, w * 0.64f, h * 0.07f, w * 0.77f, h * 0.14f)
        cubicTo(w * 0.73f, h * 0.24f, w * 0.62f, h * 0.27f, cx, h * 0.24f)
        cubicTo(w * 0.38f, h * 0.27f, w * 0.27f, h * 0.24f, w * 0.23f, h * 0.14f)
        close()
    }
    drawPath(
        monsPath,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF9DEC9), SkinWarmLight, SkinWarmMid.copy(alpha = 0.65f)),
            center = Offset(cx, h * 0.14f),
            radius = w * 0.30f
        )
    )
    // Delicate follicular stippling on mons pubis
    val stippleCoords = listOf(
        0.38f to 0.12f, 0.44f to 0.11f, 0.50f to 0.10f, 0.56f to 0.11f, 0.62f to 0.12f,
        0.34f to 0.16f, 0.41f to 0.15f, 0.48f to 0.15f, 0.54f to 0.15f, 0.60f to 0.16f, 0.66f to 0.16f,
        0.39f to 0.19f, 0.46f to 0.18f, 0.54f to 0.18f, 0.61f to 0.19f
    )
    stippleCoords.forEach { (sx, sy) ->
        drawCircle(
            color = AtlasInk.copy(alpha = 0.18f),
            radius = 2.1f,
            center = Offset(w * sx, h * sy)
        )
    }

    // 3. Outer Labia (Labia Majora) — sculpted 3D plush cushions with radial highlights
    val leftMajora = Path().apply {
        moveTo(cx, h * 0.20f)
        cubicTo(w * 0.24f, h * 0.20f, w * 0.16f, h * 0.46f, w * 0.22f, h * 0.68f)
        cubicTo(w * 0.27f, h * 0.80f, w * 0.40f, h * 0.84f, cx, h * 0.82f)
        cubicTo(w * 0.38f, h * 0.77f, w * 0.31f, h * 0.62f, w * 0.31f, h * 0.47f)
        cubicTo(w * 0.31f, h * 0.33f, w * 0.38f, h * 0.24f, cx, h * 0.20f)
        close()
    }
    val rightMajora = Path().apply {
        moveTo(cx, h * 0.20f)
        cubicTo(w * 0.76f, h * 0.20f, w * 0.84f, h * 0.46f, w * 0.78f, h * 0.68f)
        cubicTo(w * 0.73f, h * 0.80f, w * 0.60f, h * 0.84f, cx, h * 0.82f)
        cubicTo(w * 0.62f, h * 0.77f, w * 0.69f, h * 0.62f, w * 0.69f, h * 0.47f)
        cubicTo(w * 0.69f, h * 0.33f, w * 0.62f, h * 0.24f, cx, h * 0.20f)
        close()
    }
    drawPath(
        leftMajora,
        brush = Brush.horizontalGradient(
            colors = listOf(SkinWarmShadow.copy(alpha = 0.82f), Color(0xFFF6D5C0), SkinWarmShadow.copy(alpha = 0.75f)),
            startX = w * 0.17f,
            endX = w * 0.44f
        )
    )
    drawPath(
        rightMajora,
        brush = Brush.horizontalGradient(
            colors = listOf(SkinWarmShadow.copy(alpha = 0.75f), Color(0xFFF6D5C0), SkinWarmShadow.copy(alpha = 0.82f)),
            startX = w * 0.56f,
            endX = w * 0.83f
        )
    )
    drawPath(leftMajora, color = AtlasInk.copy(alpha = 0.46f), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
    drawPath(rightMajora, color = AtlasInk.copy(alpha = 0.46f), style = Stroke(width = 2.4f, cap = StrokeCap.Round))

    // 4. Anatomical Vestibule (recessed inner mucosal valley)
    val vestibule = Path().apply {
        moveTo(cx, h * 0.24f)
        cubicTo(w * 0.36f, h * 0.28f, w * 0.32f, h * 0.56f, w * 0.40f, h * 0.76f)
        cubicTo(w * 0.45f, h * 0.81f, w * 0.55f, h * 0.81f, w * 0.60f, h * 0.76f)
        cubicTo(w * 0.68f, h * 0.56f, w * 0.64f, h * 0.28f, cx, h * 0.24f)
        close()
    }
    drawPath(
        vestibule,
        brush = Brush.radialGradient(
            colors = listOf(MucosaRoseLight, MucosaRoseMid, MucosaRoseDeep.copy(alpha = 0.88f)),
            center = Offset(cx, h * 0.52f),
            radius = w * 0.28f
        )
    )

    // 5. Inner Labia (Labia Minora) — realistic organic undulating folds meeting at hood & fourchette
    val innerLeft = Path().apply {
        moveTo(cx, h * 0.25f)
        cubicTo(w * 0.37f, h * 0.29f, w * 0.30f, h * 0.42f, w * 0.29f, h * 0.52f)
        cubicTo(w * 0.28f, h * 0.62f, w * 0.34f, h * 0.71f, w * 0.42f, h * 0.76f)
        cubicTo(w * 0.45f, h * 0.78f, w * 0.48f, h * 0.79f, cx, h * 0.79f)
        cubicTo(w * 0.43f, h * 0.75f, w * 0.39f, h * 0.65f, w * 0.39f, h * 0.53f)
        cubicTo(w * 0.39f, h * 0.41f, w * 0.43f, h * 0.34f, cx, h * 0.34f)
        close()
    }
    val innerRight = Path().apply {
        moveTo(cx, h * 0.25f)
        cubicTo(w * 0.64f, h * 0.29f, w * 0.72f, h * 0.41f, w * 0.74f, h * 0.53f)
        cubicTo(w * 0.75f, h * 0.63f, w * 0.67f, h * 0.72f, w * 0.58f, h * 0.76f)
        cubicTo(w * 0.55f, h * 0.78f, w * 0.52f, h * 0.79f, cx, h * 0.79f)
        cubicTo(w * 0.57f, h * 0.75f, w * 0.61f, h * 0.65f, w * 0.61f, h * 0.53f)
        cubicTo(w * 0.61f, h * 0.41f, w * 0.57f, h * 0.34f, cx, h * 0.34f)
        close()
    }
    drawPath(
        innerLeft,
        brush = Brush.horizontalGradient(
            colors = listOf(MucosaRoseDeep, MucosaRoseMid, MucosaRoseLight),
            startX = w * 0.28f,
            endX = w * 0.46f
        )
    )
    drawPath(
        innerRight,
        brush = Brush.horizontalGradient(
            colors = listOf(MucosaRoseLight, MucosaRoseMid, MucosaRoseDeep),
            startX = w * 0.54f,
            endX = w * 0.75f
        )
    )
    drawPath(innerLeft, color = AtlasInk.copy(alpha = 0.68f), style = Stroke(width = 2.3f, join = StrokeJoin.Round))
    drawPath(innerRight, color = AtlasInk.copy(alpha = 0.68f), style = Stroke(width = 2.3f, join = StrokeJoin.Round))

    // 6. Clitoral Hood (Prepuce) draping with soft shadow pocket over the glans
    val hoodShadow = Path().apply {
        moveTo(w * 0.41f, h * 0.35f)
        cubicTo(w * 0.44f, h * 0.27f, w * 0.56f, h * 0.27f, w * 0.59f, h * 0.35f)
        quadraticTo(cx, h * 0.39f, w * 0.41f, h * 0.35f)
        close()
    }
    drawPath(hoodShadow, color = VestibuleDeep.copy(alpha = 0.45f))

    // 7. Clitoral Glans — rounded jewel-like tissue with soft specular highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF7BEB8), MucosaRoseMid, MucosaRoseDeep),
            center = Offset(cx - w * 0.01f, h * 0.34f),
            radius = w * 0.042f
        ),
        radius = w * 0.036f,
        center = Offset(cx, h * 0.35f)
    )
    drawCircle(
        color = AtlasInk.copy(alpha = 0.65f),
        radius = w * 0.036f,
        center = Offset(cx, h * 0.35f),
        style = Stroke(width = 2.0f)
    )
    // Specular highlight dot
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = w * 0.009f,
        center = Offset(cx - w * 0.011f, h * 0.337f)
    )

    // Sculpted Hood arch sitting over the top of the glans
    val hoodDrape = Path().apply {
        moveTo(w * 0.40f, h * 0.34f)
        cubicTo(w * 0.42f, h * 0.22f, w * 0.58f, h * 0.22f, w * 0.60f, h * 0.34f)
        cubicTo(w * 0.55f, h * 0.29f, w * 0.45f, h * 0.29f, w * 0.40f, h * 0.34f)
        close()
    }
    drawPath(
        hoodDrape,
        brush = Brush.verticalGradient(
            colors = listOf(SkinWarmLight, MucosaRoseLight),
            startY = h * 0.23f,
            endY = h * 0.34f
        )
    )
    drawPath(hoodDrape, color = AtlasInk.copy(alpha = 0.70f), style = Stroke(width = 2.4f, join = StrokeJoin.Round))

    // Frenulum V-folds beneath the glans
    val frenulum = Path().apply {
        moveTo(w * 0.44f, h * 0.41f)
        quadraticTo(cx, h * 0.37f, w * 0.56f, h * 0.41f)
    }
    drawPath(frenulum, color = AtlasInk.copy(alpha = 0.42f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

    // 8. Urethral Opening (small anatomical rosette cushion)
    drawCircle(
        color = MucosaRoseDeep.copy(alpha = 0.32f),
        radius = w * 0.032f,
        center = Offset(cx, h * 0.50f)
    )
    drawCircle(
        color = VestibuleDeep,
        radius = w * 0.014f,
        center = Offset(cx, h * 0.50f)
    )

    // 9. Vaginal Opening (Introitus) with shadowed internal depth & subtle coronal fringe
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3E1419), VestibuleDeep, MucosaRoseDeep),
            center = Offset(cx, h * 0.66f),
            radius = w * 0.11f
        ),
        topLeft = Offset(cx - w * 0.064f, h * 0.57f),
        size = Size(w * 0.128f, h * 0.175f)
    )
    // Scalloped hymenal / coronal rim
    val corona = Path().apply {
        moveTo(cx - w * 0.060f, h * 0.65f)
        cubicTo(cx - w * 0.068f, h * 0.59f, cx - w * 0.025f, h * 0.56f, cx, h * 0.575f)
        cubicTo(cx + w * 0.025f, h * 0.56f, cx + w * 0.068f, h * 0.59f, cx + w * 0.060f, h * 0.65f)
        cubicTo(cx + w * 0.066f, h * 0.72f, cx + w * 0.030f, h * 0.75f, cx, h * 0.742f)
        cubicTo(cx - w * 0.030f, h * 0.75f, cx - w * 0.066f, h * 0.72f, cx - w * 0.060f, h * 0.65f)
        close()
    }
    drawPath(corona, color = MucosaRoseLight.copy(alpha = 0.80f), style = Stroke(width = 3.0f, join = StrokeJoin.Round))

    // 10. Perineum smooth muscular bridge
    val perineumHighlight = Path().apply {
        moveTo(w * 0.36f, h * 0.83f)
        quadraticTo(cx, h * 0.88f, w * 0.64f, h * 0.83f)
    }
    drawPath(perineumHighlight, color = SkinWarmShadow.copy(alpha = 0.55f), style = Stroke(width = 3.2f, cap = StrokeCap.Round))

    if (selectedId == "ext_mons") {
        drawPath(monsPath, color = Color(0xFFFFD8C2).copy(alpha = 0.35f))
    }
}

private fun DrawScope.drawIllustratedInternalFront(selectedId: String) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f

    // 1. Broad Ligaments (translucent supportive connective sheets)
    val broadLigament = Path().apply {
        moveTo(w * 0.14f, h * 0.25f)
        cubicTo(w * 0.22f, h * 0.44f, w * 0.30f, h * 0.52f, w * 0.42f, h * 0.54f)
        lineTo(w * 0.58f, h * 0.54f)
        cubicTo(w * 0.70f, h * 0.52f, w * 0.78f, h * 0.44f, w * 0.86f, h * 0.25f)
        cubicTo(w * 0.76f, h * 0.20f, w * 0.24f, h * 0.20f, w * 0.14f, h * 0.25f)
        close()
    }
    drawPath(broadLigament, color = MucosaRoseLight.copy(alpha = 0.18f))

    // 2. Pelvic Floor Muscle Hammock (striated levator ani)
    val pelvicBand = Path().apply {
        moveTo(w * 0.11f, h * 0.70f)
        cubicTo(w * 0.26f, h * 0.84f, w * 0.38f, h * 0.86f, cx, h * 0.86f)
        cubicTo(w * 0.62f, h * 0.86f, w * 0.74f, h * 0.84f, w * 0.89f, h * 0.70f)
        cubicTo(w * 0.77f, h * 0.89f, w * 0.64f, h * 0.92f, cx, h * 0.92f)
        cubicTo(w * 0.36f, h * 0.92f, w * 0.23f, h * 0.89f, w * 0.11f, h * 0.70f)
        close()
    }
    drawPath(
        pelvicBand,
        brush = Brush.verticalGradient(
            colors = listOf(PelvicMuscleSage.copy(alpha = 0.85f), PelvicMuscleDeep.copy(alpha = 0.90f)),
            startY = h * 0.70f,
            endY = h * 0.92f
        )
    )
    drawPath(pelvicBand, color = PelvicMuscleDeep, style = Stroke(width = 2.4f))

    // 3. Fallopian Tubes (anatomically widening isthmus -> ampulla -> fimbriae)
    fun drawFallopianSide(sign: Float) {
        val tubeOuter = Path().apply {
            moveTo(cx + sign * w * 0.13f, h * 0.21f)
            cubicTo(
                cx + sign * w * 0.25f, h * 0.12f,
                cx + sign * w * 0.38f, h * 0.15f,
                cx + sign * w * 0.39f, h * 0.28f
            )
            cubicTo(
                cx + sign * w * 0.36f, h * 0.30f,
                cx + sign * w * 0.32f, h * 0.28f,
                cx + sign * w * 0.32f, h * 0.24f
            )
            cubicTo(
                cx + sign * w * 0.31f, h * 0.19f,
                cx + sign * w * 0.22f, h * 0.18f,
                cx + sign * w * 0.14f, h * 0.25f
            )
            close()
        }
        drawPath(
            tubeOuter,
            brush = Brush.verticalGradient(
                colors = listOf(MucosaRoseLight, MuscleRedOuter),
                startY = h * 0.13f,
                endY = h * 0.30f
            )
        )
        drawPath(tubeOuter, color = AtlasInk.copy(alpha = 0.65f), style = Stroke(width = 2.2f, join = StrokeJoin.Round))

        // Sweeping finger-like Fimbriae cradling the ovary
        val baseX = cx + sign * w * 0.365f
        val baseY = h * 0.285f
        for (i in -2..2) {
            val angle = (90 + i * 24) * PI / 180.0
            val tipX = baseX + (cos(angle) * w * 0.048f * sign).toFloat()
            val tipY = baseY + (sin(angle) * h * 0.055f).toFloat()
            val finger = Path().apply {
                moveTo(baseX, baseY)
                quadraticTo((baseX + tipX) * 0.5f + sign * 6f, (baseY + tipY) * 0.5f, tipX, tipY)
            }
            drawPath(finger, color = MuscleRedOuter, style = Stroke(width = 5.5f, cap = StrokeCap.Round))
            drawPath(finger, color = AtlasInk.copy(alpha = 0.5f), style = Stroke(width = 1.6f, cap = StrokeCap.Round))
        }

        // Ovarian ligament connecting uterus to ovary
        drawLine(
            color = MucosaRoseMid.copy(alpha = 0.72f),
            start = Offset(cx + sign * w * 0.13f, h * 0.32f),
            end = Offset(cx + sign * w * 0.24f, h * 0.34f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }
    drawFallopianSide(-1f)
    drawFallopianSide(1f)

    // 4. Ovaries (almond shaded glands with internal cutaway follicles on right)
    fun drawOvary(centerX: Float, showFollicles: Boolean) {
        val ovW = w * 0.135f
        val ovH = h * 0.088f
        val tl = Offset(centerX - ovW / 2f, h * 0.34f - ovH / 2f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF5E1), OvaryCream, OvaryAmber),
                center = Offset(centerX - ovW * 0.15f, h * 0.33f),
                radius = ovW * 0.72f
            ),
            topLeft = tl,
            size = Size(ovW, ovH)
        )
        drawOval(
            color = AtlasInk.copy(alpha = 0.62f),
            topLeft = tl,
            size = Size(ovW, ovH),
            style = Stroke(width = 2.3f)
        )
        if (showFollicles) {
            drawCircle(color = Color(0xFFFFFAF0), radius = w * 0.015f, center = Offset(centerX - w * 0.025f, h * 0.335f))
            drawCircle(color = OvaryAmber, radius = w * 0.015f, center = Offset(centerX - w * 0.025f, h * 0.335f), style = Stroke(width = 1.5f))
            drawCircle(color = Color(0xFFFFFAF0), radius = w * 0.019f, center = Offset(centerX + w * 0.018f, h * 0.345f))
            drawCircle(color = MuscleRedOuter.copy(alpha = 0.7f), radius = w * 0.019f, center = Offset(centerX + w * 0.018f, h * 0.345f), style = Stroke(width = 1.6f))
            drawCircle(color = Color(0xFFFFFAF0), radius = w * 0.011f, center = Offset(centerX + w * 0.002f, h * 0.318f))
        }
    }
    drawOvary(w * 0.20f, showFollicles = false)
    drawOvary(w * 0.80f, showFollicles = true)

    // 5. Uterus (layered muscular myometrium + plush endometrial cutaway)
    val uterusOuter = Path().apply {
        moveTo(cx, h * 0.12f)
        cubicTo(w * 0.67f, h * 0.12f, w * 0.70f, h * 0.25f, w * 0.63f, h * 0.38f)
        cubicTo(w * 0.59f, h * 0.45f, w * 0.57f, h * 0.49f, w * 0.57f, h * 0.54f)
        lineTo(w * 0.43f, h * 0.54f)
        cubicTo(w * 0.43f, h * 0.49f, w * 0.41f, h * 0.45f, w * 0.37f, h * 0.38f)
        cubicTo(w * 0.30f, h * 0.25f, w * 0.33f, h * 0.12f, cx, h * 0.12f)
        close()
    }
    drawPath(
        uterusOuter,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE07D76), MuscleRedOuter, MuscleRedDeep),
            center = Offset(cx, h * 0.24f),
            radius = w * 0.30f
        )
    )
    drawPath(uterusOuter, color = AtlasInk.copy(alpha = 0.75f), style = Stroke(width = 3.0f, join = StrokeJoin.Round))

    // Cutaway Endometrial lining layer
    val endometrium = Path().apply {
        moveTo(w * 0.39f, h * 0.21f)
        cubicTo(w * 0.45f, h * 0.19f, w * 0.55f, h * 0.19f, w * 0.61f, h * 0.21f)
        cubicTo(w * 0.59f, h * 0.32f, w * 0.54f, h * 0.40f, w * 0.525f, h * 0.52f)
        lineTo(w * 0.475f, h * 0.52f)
        cubicTo(w * 0.46f, h * 0.40f, w * 0.41f, h * 0.32f, w * 0.39f, h * 0.21f)
        close()
    }
    drawPath(endometrium, color = EndometriumPlush)

    // Deep uterine cavity shadow
    val cavity = Path().apply {
        moveTo(w * 0.42f, h * 0.235f)
        cubicTo(w * 0.46f, h * 0.225f, w * 0.54f, h * 0.225f, w * 0.58f, h * 0.235f)
        cubicTo(w * 0.55f, h * 0.33f, w * 0.52f, h * 0.41f, cx, h * 0.49f)
        cubicTo(w * 0.48f, h * 0.41f, w * 0.45f, h * 0.33f, w * 0.42f, h * 0.235f)
        close()
    }
    drawPath(
        cavity,
        brush = Brush.verticalGradient(
            colors = listOf(VestibuleDeep.copy(alpha = 0.85f), MucosaRoseDeep),
            startY = h * 0.23f,
            endY = h * 0.49f
        )
    )

    // 6. Vaginal Canal with lateral fornices & 3D shaded muscular walls
    val canalOuter = Path().apply {
        moveTo(w * 0.39f, h * 0.51f)
        cubicTo(w * 0.37f, h * 0.65f, w * 0.38f, h * 0.78f, w * 0.41f, h * 0.89f)
        lineTo(w * 0.59f, h * 0.89f)
        cubicTo(w * 0.62f, h * 0.78f, w * 0.63f, h * 0.65f, w * 0.61f, h * 0.51f)
        cubicTo(w * 0.59f, h * 0.48f, w * 0.56f, h * 0.48f, w * 0.55f, h * 0.52f)
        lineTo(w * 0.45f, h * 0.52f)
        cubicTo(w * 0.44f, h * 0.48f, w * 0.41f, h * 0.48f, w * 0.39f, h * 0.51f)
        close()
    }
    drawPath(
        canalOuter,
        brush = Brush.horizontalGradient(
            colors = listOf(MuscleRedOuter, MucosaRoseLight, MucosaRoseMid, MucosaRoseLight, MuscleRedOuter),
            startX = w * 0.38f,
            endX = w * 0.62f
        )
    )
    drawPath(canalOuter, color = AtlasInk.copy(alpha = 0.70f), style = Stroke(width = 2.6f, join = StrokeJoin.Round))

    // Sculpted Cervix lips dipping gently into upper vaginal fornices
    val cervix = Path().apply {
        moveTo(w * 0.42f, h * 0.49f)
        cubicTo(w * 0.42f, h * 0.56f, w * 0.48f, h * 0.56f, cx, h * 0.545f)
        cubicTo(w * 0.52f, h * 0.56f, w * 0.58f, h * 0.56f, w * 0.58f, h * 0.49f)
        close()
    }
    drawPath(
        cervix,
        brush = Brush.verticalGradient(
            colors = listOf(MucosaRoseMid, MucosaRoseLight),
            startY = h * 0.49f,
            endY = h * 0.56f
        )
    )
    drawPath(cervix, color = AtlasInk.copy(alpha = 0.72f), style = Stroke(width = 2.2f))
    // Endocervical canal slit
    drawLine(
        color = VestibuleDeep,
        start = Offset(cx, h * 0.48f),
        end = Offset(cx, h * 0.545f),
        strokeWidth = 3.2f,
        cap = StrokeCap.Round
    )

    // Realistic 3D-shaded pleated vaginal rugae ridges
    for (i in 0..6) {
        val y = h * (0.585f + i * 0.044f)
        val ridgeShadow = Path().apply {
            moveTo(w * 0.425f, y + 3f)
            cubicTo(w * 0.47f, y + 8f, w * 0.53f, y + 8f, w * 0.575f, y + 3f)
        }
        val ridgeHighlight = Path().apply {
            moveTo(w * 0.425f, y)
            cubicTo(w * 0.47f, y + 5f, w * 0.53f, y + 5f, w * 0.575f, y)
        }
        drawPath(ridgeShadow, color = MucosaRoseDeep.copy(alpha = 0.50f), style = Stroke(width = 3.0f, cap = StrokeCap.Round))
        drawPath(ridgeHighlight, color = Color(0xFFFFD8D2).copy(alpha = 0.72f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
    }

    if (selectedId == "int_uterus") {
        drawPath(uterusOuter, color = Color.White.copy(alpha = 0.14f))
    }
}

private fun DrawScope.drawIllustratedClitoralNetwork(selectedId: String) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f

    // 1. Ghosted Pubic Arch Bone reference behind the clitoris
    val pubicArch = Path().apply {
        moveTo(w * 0.14f, h * 0.52f)
        cubicTo(w * 0.24f, h * 0.22f, w * 0.36f, h * 0.15f, cx, h * 0.15f)
        cubicTo(w * 0.64f, h * 0.15f, w * 0.76f, h * 0.22f, w * 0.86f, h * 0.52f)
        cubicTo(w * 0.75f, h * 0.32f, w * 0.62f, h * 0.26f, cx, h * 0.26f)
        cubicTo(w * 0.38f, h * 0.26f, w * 0.25f, h * 0.32f, w * 0.14f, h * 0.52f)
        close()
    }
    drawPath(pubicArch, color = BoneIvory.copy(alpha = 0.78f))
    drawPath(pubicArch, color = BoneShadow, style = Stroke(width = 2.0f))

    // 2. Central Reference: Urethra + Vaginal Canal surrounded by Urethral Sponge (CUV / G-Zone)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF3C6C0), MucosaRoseLight.copy(alpha = 0.72f), MucosaRoseMid.copy(alpha = 0.38f)),
            center = Offset(cx, h * 0.64f),
            radius = w * 0.16f
        ),
        topLeft = Offset(cx - w * 0.135f, h * 0.44f),
        size = Size(w * 0.27f, h * 0.36f)
    )

    // Vaginal canal lumen
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(VestibuleDeep.copy(alpha = 0.72f), MucosaRoseDeep.copy(alpha = 0.85f)),
            center = Offset(cx, h * 0.65f),
            radius = w * 0.09f
        ),
        topLeft = Offset(cx - w * 0.082f, h * 0.54f),
        size = Size(w * 0.164f, h * 0.23f)
    )
    drawOval(
        color = AtlasInk.copy(alpha = 0.55f),
        topLeft = Offset(cx - w * 0.082f, h * 0.54f),
        size = Size(w * 0.164f, h * 0.23f),
        style = Stroke(width = 2.2f)
    )

    // Urethra opening above vaginal canal
    drawCircle(
        color = BladderGoldMid.copy(alpha = 0.85f),
        radius = w * 0.028f,
        center = Offset(cx, h * 0.47f)
    )
    drawCircle(
        color = AtlasInk.copy(alpha = 0.65f),
        radius = w * 0.012f,
        center = Offset(cx, h * 0.47f)
    )

    // 3. Clitoral Crura (sweeping internal erectile wings along pubic rami)
    val leftCrus = Path().apply {
        moveTo(cx - w * 0.032f, h * 0.26f)
        cubicTo(w * 0.30f, h * 0.27f, w * 0.13f, h * 0.44f, w * 0.16f, h * 0.74f)
        cubicTo(w * 0.18f, h * 0.80f, w * 0.24f, h * 0.77f, w * 0.25f, h * 0.70f)
        cubicTo(w * 0.26f, h * 0.54f, w * 0.32f, h * 0.38f, cx - w * 0.045f, h * 0.34f)
        close()
    }
    val rightCrus = Path().apply {
        moveTo(cx + w * 0.032f, h * 0.26f)
        cubicTo(w * 0.70f, h * 0.27f, w * 0.87f, h * 0.44f, w * 0.84f, h * 0.74f)
        cubicTo(w * 0.82f, h * 0.80f, w * 0.76f, h * 0.77f, w * 0.75f, h * 0.70f)
        cubicTo(w * 0.74f, h * 0.54f, w * 0.68f, h * 0.38f, cx + w * 0.045f, h * 0.34f)
        close()
    }
    val crusBrushLeft = Brush.linearGradient(
        colors = listOf(ErectilePlumLight, ErectilePlumMid, ErectilePlumDeep),
        start = Offset(cx, h * 0.26f),
        end = Offset(w * 0.16f, h * 0.76f)
    )
    val crusBrushRight = Brush.linearGradient(
        colors = listOf(ErectilePlumLight, ErectilePlumMid, ErectilePlumDeep),
        start = Offset(cx, h * 0.26f),
        end = Offset(w * 0.84f, h * 0.76f)
    )
    drawPath(leftCrus, brush = crusBrushLeft)
    drawPath(rightCrus, brush = crusBrushRight)
    drawPath(leftCrus, color = ErectilePlumDeep, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
    drawPath(rightCrus, color = ErectilePlumDeep, style = Stroke(width = 2.6f, join = StrokeJoin.Round))

    // 4. Vestibular Bulbs (plush spongy vascular teardrops flanking the vaginal canal)
    val leftBulb = Path().apply {
        moveTo(cx - w * 0.025f, h * 0.36f)
        cubicTo(w * 0.35f, h * 0.40f, w * 0.26f, h * 0.60f, w * 0.31f, h * 0.77f)
        cubicTo(w * 0.34f, h * 0.84f, w * 0.42f, h * 0.82f, w * 0.42f, h * 0.73f)
        cubicTo(w * 0.42f, h * 0.60f, w * 0.42f, h * 0.46f, cx - w * 0.015f, h * 0.40f)
        close()
    }
    val rightBulb = Path().apply {
        moveTo(cx + w * 0.025f, h * 0.36f)
        cubicTo(w * 0.65f, h * 0.40f, w * 0.74f, h * 0.60f, w * 0.69f, h * 0.77f)
        cubicTo(w * 0.66f, h * 0.84f, w * 0.58f, h * 0.82f, w * 0.58f, h * 0.73f)
        cubicTo(w * 0.58f, h * 0.60f, w * 0.58f, h * 0.46f, cx + w * 0.015f, h * 0.40f)
        close()
    }
    drawPath(
        leftBulb,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE996A8), MuscleRedOuter, ErectilePlumMid),
            center = Offset(w * 0.35f, h * 0.62f),
            radius = w * 0.18f
        )
    )
    drawPath(
        rightBulb,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE996A8), MuscleRedOuter, ErectilePlumMid),
            center = Offset(w * 0.65f, h * 0.62f),
            radius = w * 0.18f
        )
    )
    drawPath(leftBulb, color = AtlasInk.copy(alpha = 0.72f), style = Stroke(width = 2.5f, join = StrokeJoin.Round))
    drawPath(rightBulb, color = AtlasInk.copy(alpha = 0.72f), style = Stroke(width = 2.5f, join = StrokeJoin.Round))

    // Subtle cavernous vascular texture highlights inside bulbs
    listOf(0.35f to 0.56f, 0.34f to 0.66f, 0.37f to 0.73f, 0.65f to 0.56f, 0.66f to 0.66f, 0.63f to 0.73f).forEach { (bx, by) ->
        drawCircle(
            color = Color.White.copy(alpha = 0.24f),
            radius = w * 0.014f,
            center = Offset(w * bx, h * by)
        )
    }

    // 5. Clitoral Body / Shaft & Glans crown
    val shaft = Path().apply {
        moveTo(cx - w * 0.052f, h * 0.35f)
        cubicTo(cx - w * 0.058f, h * 0.24f, cx - w * 0.044f, h * 0.17f, cx, h * 0.17f)
        cubicTo(cx + w * 0.044f, h * 0.17f, cx + w * 0.058f, h * 0.24f, cx + w * 0.052f, h * 0.35f)
        close()
    }
    drawPath(
        shaft,
        brush = Brush.horizontalGradient(
            colors = listOf(ErectilePlumDeep, ErectilePlumLight, ErectilePlumDeep),
            startX = cx - w * 0.055f,
            endX = cx + w * 0.055f
        )
    )
    drawPath(shaft, color = AtlasInk.copy(alpha = 0.75f), style = Stroke(width = 2.4f))

    // Glans pearl at apex
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD0DC), ErectilePlumLight, ErectilePlumDeep),
            center = Offset(cx - w * 0.012f, h * 0.20f),
            radius = w * 0.048f
        ),
        radius = w * 0.040f,
        center = Offset(cx, h * 0.21f)
    )
    drawCircle(color = AtlasInk.copy(alpha = 0.75f), radius = w * 0.040f, center = Offset(cx, h * 0.21f), style = Stroke(width = 2.2f))

    // Delicate dorsal nerve filaments (golden fine lines)
    val nerveLeft = Path().apply {
        moveTo(cx - w * 0.015f, h * 0.22f)
        cubicTo(w * 0.42f, h * 0.30f, w * 0.28f, h * 0.38f, w * 0.22f, h * 0.56f)
    }
    val nerveRight = Path().apply {
        moveTo(cx + w * 0.015f, h * 0.22f)
        cubicTo(w * 0.58f, h * 0.30f, w * 0.72f, h * 0.38f, w * 0.78f, h * 0.56f)
    }
    drawPath(nerveLeft, color = Color(0xFFFFE08A).copy(alpha = 0.75f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
    drawPath(nerveRight, color = Color(0xFFFFE08A).copy(alpha = 0.75f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

    if (selectedId == "clit_urethral_sponge") {
        drawCircle(color = Color.White.copy(alpha = 0.20f), radius = w * 0.11f, center = Offset(cx, h * 0.62f))
    }
}

private fun DrawScope.drawIllustratedPelvicSide(selectedId: String) {
    val w = size.width
    val h = size.height

    // 1. Anatomical Pelvic Wall Silhouette (sagittal abdominal/pelvic contour)
    val bodyCrossSection = Path().apply {
        moveTo(w * 0.16f, h * 0.08f)
        cubicTo(w * 0.13f, h * 0.32f, w * 0.10f, h * 0.56f, w * 0.19f, h * 0.78f)
        cubicTo(w * 0.36f, h * 0.88f, w * 0.64f, h * 0.90f, w * 0.86f, h * 0.84f)
        cubicTo(w * 0.91f, h * 0.58f, w * 0.91f, h * 0.30f, w * 0.86f, h * 0.08f)
        close()
    }
    drawPath(
        bodyCrossSection,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFF7E7DC), Color(0xFFEDD4C4)),
            startY = h * 0.08f,
            endY = h * 0.90f
        )
    )

    // 2. Pubic Bone (Symphysis) — front-left ivory cross-section
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFAF4EB), BoneIvory, BoneShadow),
            center = Offset(w * 0.19f, h * 0.57f),
            radius = w * 0.08f
        ),
        topLeft = Offset(w * 0.14f, h * 0.50f),
        size = Size(w * 0.095f, h * 0.145f)
    )
    drawOval(
        color = AtlasInk.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.14f, h * 0.50f),
        size = Size(w * 0.095f, h * 0.145f),
        style = Stroke(width = 2.0f)
    )

    // 3. Curved Sacrum & Coccyx (Tailbone) — back-right segmented ivory silhouette
    val sacrum = Path().apply {
        moveTo(w * 0.83f, h * 0.16f)
        cubicTo(w * 0.89f, h * 0.34f, w * 0.88f, h * 0.54f, w * 0.79f, h * 0.67f)
        cubicTo(w * 0.77f, h * 0.64f, w * 0.81f, h * 0.48f, w * 0.77f, h * 0.22f)
        close()
    }
    drawPath(sacrum, color = BoneIvory)
    drawPath(sacrum, color = BoneShadow, style = Stroke(width = 2.2f))

    // 4. Rectum & Anal Canal (curving along sacrum posteriorly)
    val rectum = Path().apply {
        moveTo(w * 0.73f, h * 0.22f)
        cubicTo(w * 0.71f, h * 0.40f, w * 0.64f, h * 0.57f, w * 0.62f, h * 0.82f)
        lineTo(w * 0.72f, h * 0.83f)
        cubicTo(w * 0.75f, h * 0.61f, w * 0.80f, h * 0.42f, w * 0.81f, h * 0.24f)
        close()
    }
    drawPath(
        rectum,
        brush = Brush.horizontalGradient(
            colors = listOf(RectumDeep, RectumTaupe, RectumDeep),
            startX = w * 0.62f,
            endX = w * 0.81f
        )
    )
    drawPath(rectum, color = AtlasInk.copy(alpha = 0.65f), style = Stroke(width = 2.4f))

    // 5. Urinary Bladder & Short Urethra (front-left behind pubic bone)
    val bladder = Path().apply {
        moveTo(w * 0.23f, h * 0.44f)
        cubicTo(w * 0.24f, h * 0.33f, w * 0.41f, h * 0.34f, w * 0.42f, h * 0.46f)
        cubicTo(w * 0.41f, h * 0.55f, w * 0.33f, h * 0.57f, w * 0.30f, h * 0.72f)
        lineTo(w * 0.255f, h * 0.72f)
        cubicTo(w * 0.27f, h * 0.58f, w * 0.23f, h * 0.53f, w * 0.23f, h * 0.44f)
        close()
    }
    drawPath(
        bladder,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFDF0D2), BladderGoldLight, BladderGoldMid),
            center = Offset(w * 0.32f, h * 0.44f),
            radius = w * 0.14f
        )
    )
    drawPath(bladder, color = AtlasInk.copy(alpha = 0.70f), style = Stroke(width = 2.5f, join = StrokeJoin.Round))

    // 6. Clitoral Glans & Internal Shaft curving beneath the pubic bone
    val clitSide = Path().apply {
        moveTo(w * 0.22f, h * 0.71f)
        cubicTo(w * 0.20f, h * 0.65f, w * 0.22f, h * 0.61f, w * 0.26f, h * 0.58f)
        cubicTo(w * 0.27f, h * 0.61f, w * 0.24f, h * 0.66f, w * 0.25f, h * 0.71f)
        close()
    }
    drawPath(clitSide, color = ErectilePlumMid)
    drawPath(clitSide, color = AtlasInk.copy(alpha = 0.65f), style = Stroke(width = 1.8f))

    // 7. Anteverted Uterus tilting forward over bladder with endometrial cutaway
    val uterusSide = Path().apply {
        moveTo(w * 0.46f, h * 0.49f)
        cubicTo(w * 0.35f, h * 0.34f, w * 0.38f, h * 0.16f, w * 0.55f, h * 0.18f)
        cubicTo(w * 0.69f, h * 0.20f, w * 0.67f, h * 0.40f, w * 0.55f, h * 0.51f)
        close()
    }
    drawPath(
        uterusSide,
        brush = Brush.radialGradient(
            colors = listOf(EndometriumPlush, MuscleRedOuter, MuscleRedDeep),
            center = Offset(w * 0.51f, h * 0.28f),
            radius = w * 0.20f
        )
    )
    drawPath(uterusSide, color = AtlasInk.copy(alpha = 0.78f), style = Stroke(width = 2.8f, join = StrokeJoin.Round))

    // Internal uterine cavity curve
    val uterusCavity = Path().apply {
        moveTo(w * 0.47f, h * 0.28f)
        cubicTo(w * 0.53f, h * 0.26f, w * 0.58f, h * 0.33f, w * 0.51f, h * 0.48f)
        cubicTo(w * 0.49f, h * 0.39f, w * 0.45f, h * 0.32f, w * 0.47f, h * 0.28f)
        close()
    }
    drawPath(uterusCavity, color = VestibuleDeep.copy(alpha = 0.78f))

    // 8. Angled Vaginal Canal sloping backward (~45 degrees) with fornices & rugae
    val vaginaSide = Path().apply {
        moveTo(w * 0.44f, h * 0.47f)
        cubicTo(w * 0.41f, h * 0.58f, w * 0.37f, h * 0.69f, w * 0.34f, h * 0.79f)
        lineTo(w * 0.45f, h * 0.81f)
        cubicTo(w * 0.49f, h * 0.70f, w * 0.54f, h * 0.59f, w * 0.58f, h * 0.48f)
        cubicTo(w * 0.56f, h * 0.45f, w * 0.53f, h * 0.46f, w * 0.51f, h * 0.49f)
        lineTo(w * 0.46f, h * 0.49f)
        close()
    }
    drawPath(
        vaginaSide,
        brush = Brush.horizontalGradient(
            colors = listOf(MucosaRoseDeep, MucosaRoseLight, MucosaRoseMid),
            startX = w * 0.34f,
            endX = w * 0.58f
        )
    )
    drawPath(vaginaSide, color = AtlasInk.copy(alpha = 0.75f), style = Stroke(width = 2.5f, join = StrokeJoin.Round))

    // Internal vaginal lumen shadow line
    val lumenLine = Path().apply {
        moveTo(w * 0.50f, h * 0.50f)
        cubicTo(w * 0.46f, h * 0.60f, w * 0.42f, h * 0.70f, w * 0.395f, h * 0.795f)
    }
    drawPath(lumenLine, color = VestibuleDeep.copy(alpha = 0.65f), style = Stroke(width = 4.0f, cap = StrokeCap.Round))

    // 9. Pelvic Floor & Perineal Body Anchor Muscles along the base
    val pelvicBase = Path().apply {
        moveTo(w * 0.21f, h * 0.76f)
        cubicTo(w * 0.36f, h * 0.85f, w * 0.58f, h * 0.87f, w * 0.76f, h * 0.82f)
    }
    drawPath(pelvicBase, color = PelvicMuscleDeep.copy(alpha = 0.78f), style = Stroke(width = 8.5f, cap = StrokeCap.Round))
    drawPath(pelvicBase, color = PelvicMuscleSage.copy(alpha = 0.72f), style = Stroke(width = 3.5f, cap = StrokeCap.Round))

    if (selectedId == "side_uterus") {
        drawPath(uterusSide, color = Color.White.copy(alpha = 0.14f))
    }
}
