package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AnatomyPart
import com.example.data.model.SensitivityTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class CameraAnglePreset(
    val label: String,
    val yawDeg: Float,
    val pitchDeg: Float,
    val zoom: Float
) {
    EXAM_FRONT("Exam View", 0f, 5f, 1.02f),
    FULL_LEGS("Wide View", 0f, 6f, 0.72f),
    UPPER_BODY("Chest & Face", 0f, 8f, 0.34f),
    FACE_CLOSE("Face", 0f, 4f, 0.72f),
    CLOSE_UP("Macro", 0f, 3f, 1.56f),
    LEFT_34("3/4 Left", -30f, 10f, 0.92f),
    RIGHT_34("3/4 Right", 30f, 10f, 0.92f),
    LOW_ANGLE("Low Angle", 0f, -24f, 1.02f);

    fun resolveCamera(blossomBodyEnabled: Boolean): VulvaCamera3D {
        if (!blossomBodyEnabled) {
            return VulvaCamera3D(yawDeg = yawDeg, pitchDeg = pitchDeg, zoom = zoom)
        }
        return when (this) {
            EXAM_FRONT -> VulvaCamera3D(yawDeg = 0f, pitchDeg = 5f, zoom = 1.02f, targetY = 0.02f, targetZ = 0.08f)
            FULL_LEGS -> VulvaCamera3D(yawDeg = 0f, pitchDeg = 13f, zoom = 0.140f, targetY = 1.05f, targetZ = -0.62f)
            UPPER_BODY -> VulvaCamera3D(yawDeg = 0f, pitchDeg = 6f, zoom = 0.37f, targetY = 4.54f, targetZ = -0.82f)
            FACE_CLOSE -> VulvaCamera3D(yawDeg = 0f, pitchDeg = 4f, zoom = 0.80f, targetY = 6.40f, targetZ = -0.82f)
            CLOSE_UP -> VulvaCamera3D(yawDeg = 0f, pitchDeg = 3f, zoom = 1.56f, targetY = 0.04f, targetZ = 0.08f)
            LEFT_34 -> VulvaCamera3D(yawDeg = -46f, pitchDeg = 17f, zoom = 0.160f, targetY = 1.05f, targetZ = -0.62f)
            RIGHT_34 -> VulvaCamera3D(yawDeg = 46f, pitchDeg = 17f, zoom = 0.160f, targetY = 1.05f, targetZ = -0.62f)
            LOW_ANGLE -> VulvaCamera3D(yawDeg = 0f, pitchDeg = -28f, zoom = 0.32f, targetY = -0.24f, targetZ = 0.04f)
        }
    }
}

private enum class TouchTargetZone {
    VULVA,
    LEFT_BREAST,
    RIGHT_BREAST,
    LEFT_LEG,
    RIGHT_LEG
}

private data class Pointer3DTrack(
    var lastPos: Offset,
    val startNorm: Offset,
    val zone: TouchTargetZone = TouchTargetZone.VULVA
)

@Composable
fun InteractiveVulva3DCanvas(
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
    val isPreviewOrTest = LocalInspectionMode.current ||
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
    var glUnavailable by remember { mutableStateOf(false) }

    if (isPreviewOrTest || glUnavailable) {
        InteractiveVulvaCanvas(
            parts = parts,
            selectedPartId = selectedPartId,
            showLabels = showLabels,
            hapticsEnabled = hapticsEnabled,
            holdOpen = holdOpen,
            resetTrigger = resetTrigger,
            toneIndex = toneIndex,
            onSelectPart = onSelectPart,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val composeHaptic = LocalHapticFeedback.current
    val hapticsEngine = remember(context) { AnatomyHapticsEngine(context) }
    val coroutineScope = rememberCoroutineScope()

    val tone = VulvaToneStyles[toneIndex.coerceIn(0, VulvaToneStyles.lastIndex)]
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    // 5 live elastic pelvic channels + 4 breast mobility + 2 interactive leg-spread channels
    val leftOuterSpread = remember { Animatable(0f) }
    val rightOuterSpread = remember { Animatable(0f) }
    val leftInnerOpen = remember { Animatable(0f) }
    val rightInnerOpen = remember { Animatable(0f) }
    val hoodRetraction = remember { Animatable(0f) }
    val leftBreastX = remember { Animatable(0f) }
    val leftBreastY = remember { Animatable(0f) }
    val rightBreastX = remember { Animatable(0f) }
    val rightBreastY = remember { Animatable(0f) }
    val leftLegSpread = remember { Animatable(0f) }
    val rightLegSpread = remember { Animatable(0f) }

    // Live Blossom appearance customization: Hair color (default Chestnut Brown) & Breast size (default Medium)
    var hairStyleIndex by remember { mutableIntStateOf(2) }
    var breastSize by remember { mutableFloatStateOf(0.50f) }
    val hairStyle = BlossomHairStyles[hairStyleIndex.coerceIn(0, BlossomHairStyles.lastIndex)]

    // Smooth 3D camera orbit, zoom, target framing, lighting & Blossom full-body Beta state (default ON, Exam View)
    var isOrbitMode by remember { mutableStateOf(false) }
    var dramaticSideLight by remember { mutableStateOf(false) }
    var showExperimentalLegs by remember { mutableStateOf(true) }
    var activePreset by remember { mutableStateOf<CameraAnglePreset?>(CameraAnglePreset.EXAM_FRONT) }
    val initialCam = remember { CameraAnglePreset.EXAM_FRONT.resolveCamera(blossomBodyEnabled = true) }
    val camYaw = remember { Animatable(initialCam.yawDeg) }
    val camPitch = remember { Animatable(initialCam.pitchDeg) }
    val camZoom = remember { Animatable(initialCam.zoom) }
    val camTargetY = remember { Animatable(initialCam.targetY) }
    val camTargetZ = remember { Animatable(initialCam.targetZ) }

    // Live 3D skin sensory glow under fingertip
    var touchGlowX by remember { mutableFloatStateOf(0.5f) }
    var touchGlowY by remember { mutableFloatStateOf(0.5f) }
    var touchGlowColor by remember { mutableStateOf(Color(0xFFFFD1DC)) }
    val touchGlowIntensity = remember { Animatable(0f) }

    var activePointerCount by remember { mutableStateOf(0) }
    val latestHoldOpen by rememberUpdatedState(holdOpen)
    val latestHapticsEnabled by rememberUpdatedState(hapticsEnabled)
    val latestParts by rememberUpdatedState(parts)
    val latestOnSelectPart by rememberUpdatedState(onSelectPart)
    val latestOrbitMode by rememberUpdatedState(isOrbitMode)
    val latestShowFullBody by rememberUpdatedState(showExperimentalLegs)

    fun springBackTissue() {
        val tissueSpring = spring<Float>(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessLow
        )
        val breastSpring = spring<Float>(
            dampingRatio = 0.50f,
            stiffness = Spring.StiffnessMediumLow
        )
        val legSpring = spring<Float>(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessVeryLow
        )
        coroutineScope.launch { leftOuterSpread.animateTo(0f, tissueSpring) }
        coroutineScope.launch { rightOuterSpread.animateTo(0f, tissueSpring) }
        coroutineScope.launch { leftInnerOpen.animateTo(0f, tissueSpring) }
        coroutineScope.launch { rightInnerOpen.animateTo(0f, tissueSpring) }
        coroutineScope.launch { hoodRetraction.animateTo(0f, tissueSpring) }
        coroutineScope.launch { leftBreastX.animateTo(0f, breastSpring) }
        coroutineScope.launch { leftBreastY.animateTo(0f, breastSpring) }
        coroutineScope.launch { rightBreastX.animateTo(0f, breastSpring) }
        coroutineScope.launch { rightBreastY.animateTo(0f, breastSpring) }
        coroutineScope.launch { leftLegSpread.animateTo(0f, legSpring) }
        coroutineScope.launch { rightLegSpread.animateTo(0f, legSpring) }
    }

    fun animateToPreset(preset: CameraAnglePreset, legsEnabled: Boolean = showExperimentalLegs) {
        activePreset = preset
        val target = preset.resolveCamera(legsEnabled)
        val camSpring = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        coroutineScope.launch { camYaw.animateTo(target.yawDeg, camSpring) }
        coroutineScope.launch { camPitch.animateTo(target.pitchDeg, camSpring) }
        coroutineScope.launch { camZoom.animateTo(target.zoom, camSpring) }
        coroutineScope.launch { camTargetY.animateTo(target.targetY, camSpring) }
        coroutineScope.launch { camTargetZ.animateTo(target.targetZ, camSpring) }
    }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            springBackTissue()
        }
    }
    LaunchedEffect(holdOpen) {
        if (!holdOpen && activePointerCount == 0) {
            springBackTissue()
        }
    }

    fun trigger3DSensory(part: AnatomyPart, nx: Float, ny: Float, isDrag: Boolean) {
        latestOnSelectPart(part.id)
        touchGlowX = nx
        touchGlowY = ny
        touchGlowColor = when (part.sensitivityTier) {
            SensitivityTier.ULTRA -> Color(0xFFFFA8C2)
            SensitivityTier.HIGH -> Color(0xFFFFC7BC)
            SensitivityTier.MODERATE -> Color(0xFFFFE5CC)
        }
        coroutineScope.launch {
            touchGlowIntensity.snapTo(if (part.sensitivityTier == SensitivityTier.ULTRA) 1.0f else 0.72f)
            delay(90L)
            touchGlowIntensity.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow))
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

    val currentDeform = VulvaDeformationState(
        leftOuter = leftOuterSpread.value,
        rightOuter = rightOuterSpread.value,
        leftInner = leftInnerOpen.value,
        rightInner = rightInnerOpen.value,
        hoodRetract = hoodRetraction.value,
        leftBreastX = leftBreastX.value,
        leftBreastY = leftBreastY.value,
        rightBreastX = rightBreastX.value,
        rightBreastY = rightBreastY.value,
        breastSize = breastSize,
        leftLegSpread = leftLegSpread.value,
        rightLegSpread = rightLegSpread.value
    )
    val currentCam = VulvaCamera3D(
        yawDeg = camYaw.value,
        pitchDeg = camPitch.value,
        zoom = camZoom.value,
        targetY = camTargetY.value,
        targetZ = camTargetZ.value
    )
    val currentGlow = VulvaTouchGlow(
        normX = touchGlowX,
        normY = touchGlowY,
        intensity = touchGlowIntensity.value,
        color = touchGlowColor
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.86f)
                .testTag("interactive_vulva_3d_canvas"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1616),
            tonalElevation = 2.dp,
            shadowElevation = 3.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val aspect = widthPx / heightPx

                AndroidView(
                    factory = { ctx ->
                        VulvaGlTextureView(
                            context = ctx,
                            onGlError = { glUnavailable = true }
                        )
                    },
                    update = { glView ->
                        glView.deformationState = currentDeform
                        glView.cameraState = currentCam
                        glView.toneStyle = tone
                        glView.hairStyle = hairStyle
                        glView.touchGlow = currentGlow
                        glView.dramaticSideLight = dramaticSideLight
                        glView.showExperimentalLegs = showExperimentalLegs
                        glView.requestFrame()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = if (isOrbitMode) {
                                "3D gynecological vulva simulator in Orbit mode. Drag to rotate in 3D or pinch to zoom out to full legs or close-up."
                            } else {
                                "3D gynecological vulva simulator in Touch Tissue mode. Spread outer and inner labia with one or two fingers, lift the clitoral hood upward, or glide across to explore sensitivity."
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val tracks = mutableMapOf<PointerId, Pointer3DTrack>()
                                var lastHitPartId: String? = null
                                var lastDragFeedbackMs = 0L
                                var lastPinchDist = -1f
                                var lastPinchMidY = -1f

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val pressed = event.changes.filter { it.pressed }
                                    activePointerCount = pressed.size

                                    val activeIds = pressed.map { it.id }.toSet()
                                    tracks.keys.retainAll(activeIds)

                                    if (pressed.isEmpty()) {
                                        lastPinchDist = -1f
                                        lastPinchMidY = -1f
                                        if (!latestOrbitMode && !latestHoldOpen) {
                                            springBackTissue()
                                        }
                                        break
                                    }

                                    if (latestOrbitMode) {
                                        activePreset = null
                                        val minZoom = if (latestShowFullBody) 0.10f else 0.42f
                                        val maxYaw = if (latestShowFullBody) 82f else 54f
                                        val minPitch = if (latestShowFullBody) -36f else -36f
                                        val maxPitch = if (latestShowFullBody) 58f else 42f
                                        if (pressed.size >= 2) {
                                            pressed.forEach { it.consume() }
                                            val p0 = pressed[0].position
                                            val p1 = pressed[1].position
                                            val dist = hypot(p0.x - p1.x, p0.y - p1.y)
                                            val midY = (p0.y + p1.y) * 0.5f
                                            if (lastPinchDist > 1f) {
                                                val scaleRatio = dist / lastPinchDist
                                                val nextZoom = (camZoom.value * scaleRatio).coerceIn(minZoom, 1.90f)
                                                coroutineScope.launch { camZoom.snapTo(nextZoom) }
                                            }
                                            if (latestShowFullBody && lastPinchMidY >= 0f) {
                                                val panDy = (midY - lastPinchMidY) / h
                                                val panSpan = (2.1f / camZoom.value.coerceIn(0.10f, 1.50f)).coerceIn(1.2f, 11.0f)
                                                val nextTargetY = (camTargetY.value + panDy * panSpan).coerceIn(-3.2f, 8.2f)
                                                val nextTargetZ = BlossomBodyBuilder.mattressSurfaceZ(nextTargetY) + 0.92f
                                                coroutineScope.launch {
                                                    camTargetY.snapTo(nextTargetY)
                                                    camTargetZ.snapTo(nextTargetZ.coerceIn(-1.45f, 0.14f))
                                                }
                                            }
                                            lastPinchDist = dist
                                            lastPinchMidY = midY
                                        } else {
                                            lastPinchDist = -1f
                                            lastPinchMidY = -1f
                                            val ch = pressed[0]
                                            ch.consume()
                                            val prev = tracks[ch.id]
                                            if (prev == null) {
                                                tracks[ch.id] = Pointer3DTrack(ch.position, Offset(ch.position.x / w, ch.position.y / h))
                                            } else {
                                                val dx = (ch.position.x - prev.lastPos.x) / w
                                                val dy = (ch.position.y - prev.lastPos.y) / h
                                                prev.lastPos = ch.position
                                                val nextYaw = (camYaw.value + dx * 130f).coerceIn(-maxYaw, maxYaw)
                                                val nextPitch = (camPitch.value + dy * 100f).coerceIn(minPitch, maxPitch)
                                                coroutineScope.launch {
                                                    camYaw.snapTo(nextYaw)
                                                    camPitch.snapTo(nextPitch)
                                                }
                                            }
                                        }
                                    } else {
                                        var dLeftOuter = 0f
                                        var dRightOuter = 0f
                                        var dLeftInner = 0f
                                        var dRightInner = 0f
                                        var dHood = 0f
                                        var dLeftBreastX = 0f
                                        var dLeftBreastY = 0f
                                        var dRightBreastX = 0f
                                        var dRightBreastY = 0f
                                        var dLeftLeg = 0f
                                        var dRightLeg = 0f

                                        val zoomScale = camZoom.value.coerceIn(0.10f, 1.90f)
                                        val liveCam = VulvaCamera3D(
                                            yawDeg = camYaw.value,
                                            pitchDeg = camPitch.value,
                                            zoom = camZoom.value,
                                            targetY = camTargetY.value,
                                            targetZ = camTargetZ.value
                                        )
                                        val aspectLive = w / h
                                        val chestZ = BlossomBodyBuilder.mattressSurfaceZ(BlossomUpperBodyBuilder.BREAST_CENTER_Y) + 1.56f
                                        val leftBreastScreen = VulvaGlTextureView.projectPointToNormalizedScreen(
                                            floatArrayOf(-BlossomUpperBodyBuilder.BREAST_CENTER_AX, BlossomUpperBodyBuilder.BREAST_CENTER_Y, chestZ),
                                            liveCam,
                                            aspectLive
                                        )
                                        val rightBreastScreen = VulvaGlTextureView.projectPointToNormalizedScreen(
                                            floatArrayOf(BlossomUpperBodyBuilder.BREAST_CENTER_AX, BlossomUpperBodyBuilder.BREAST_CENTER_Y, chestZ),
                                            liveCam,
                                            aspectLive
                                        )
                                        val breastHitRadius = (0.34f * zoomScale).coerceIn(0.065f, 0.28f)

                                        // Project Blossom's knees, mid-thighs & calves for natural direct side-to-side leg spreading
                                        val lKneeW = BlossomSdfBodyBuilder.transformLegPoint(-1.80f, -1.86f, 1.74f, -1f, leftLegSpread.value)
                                        val rKneeW = BlossomSdfBodyBuilder.transformLegPoint(1.80f, -1.86f, 1.74f, +1f, rightLegSpread.value)
                                        val lThighW = BlossomSdfBodyBuilder.transformLegPoint(-1.46f, -0.82f, 0.78f, -1f, leftLegSpread.value, 0.78f)
                                        val rThighW = BlossomSdfBodyBuilder.transformLegPoint(1.46f, -0.82f, 0.78f, +1f, rightLegSpread.value, 0.78f)
                                        val lCalfW = BlossomSdfBodyBuilder.transformLegPoint(-1.74f, -3.05f, 0.40f, -1f, leftLegSpread.value)
                                        val rCalfW = BlossomSdfBodyBuilder.transformLegPoint(1.74f, -3.05f, 0.40f, +1f, rightLegSpread.value)

                                        val lKneeS = VulvaGlTextureView.projectPointToNormalizedScreen(lKneeW, liveCam, aspectLive)
                                        val rKneeS = VulvaGlTextureView.projectPointToNormalizedScreen(rKneeW, liveCam, aspectLive)
                                        val lThighS = VulvaGlTextureView.projectPointToNormalizedScreen(lThighW, liveCam, aspectLive)
                                        val rThighS = VulvaGlTextureView.projectPointToNormalizedScreen(rThighW, liveCam, aspectLive)
                                        val lCalfS = VulvaGlTextureView.projectPointToNormalizedScreen(lCalfW, liveCam, aspectLive)
                                        val rCalfS = VulvaGlTextureView.projectPointToNormalizedScreen(rCalfW, liveCam, aspectLive)
                                        val legHitRadius = (0.40f * zoomScale).coerceIn(0.11f, 0.28f)

                                        pressed.forEach { change ->
                                            change.consume()
                                            val pos = change.position
                                            val rawNx = (pos.x / w).coerceIn(0f, 1f)
                                            val rawNy = (pos.y / h).coerceIn(0f, 1f)
                                            val nx = (0.5f + (rawNx - 0.5f) / zoomScale).coerceIn(0f, 1f)
                                            val ny = (0.5f + (rawNy - 0.5f) / zoomScale).coerceIn(0f, 1f)
                                            val existing = tracks[change.id]

                                            if (existing == null) {
                                                val distLB = hypot(rawNx - leftBreastScreen.x, rawNy - leftBreastScreen.y)
                                                val distRB = hypot(rawNx - rightBreastScreen.x, rawNy - rightBreastScreen.y)
                                                val distLL = minOf(
                                                    hypot(rawNx - lKneeS.x, rawNy - lKneeS.y),
                                                    hypot(rawNx - lThighS.x, rawNy - lThighS.y),
                                                    hypot(rawNx - lCalfS.x, rawNy - lCalfS.y)
                                                )
                                                val distRL = minOf(
                                                    hypot(rawNx - rKneeS.x, rawNy - rKneeS.y),
                                                    hypot(rawNx - rThighS.x, rawNy - rThighS.y),
                                                    hypot(rawNx - rCalfS.x, rawNy - rCalfS.y)
                                                )
                                                val allowLeftLeg = zoomScale < 0.78f || rawNx < 0.28f || rawNy > 0.72f
                                                val allowRightLeg = zoomScale < 0.78f || rawNx > 0.72f || rawNy > 0.72f

                                                val zone = when {
                                                    latestShowFullBody && distLB < breastHitRadius && distLB <= distRB -> TouchTargetZone.LEFT_BREAST
                                                    latestShowFullBody && distRB < breastHitRadius -> TouchTargetZone.RIGHT_BREAST
                                                    latestShowFullBody && allowLeftLeg && distLL < legHitRadius && distLL <= distRL -> TouchTargetZone.LEFT_LEG
                                                    latestShowFullBody && allowRightLeg && distRL < legHitRadius -> TouchTargetZone.RIGHT_LEG
                                                    else -> TouchTargetZone.VULVA
                                                }
                                                tracks[change.id] = Pointer3DTrack(pos, Offset(nx, ny), zone)

                                                if (zone != TouchTargetZone.VULVA) {
                                                    if (latestHapticsEnabled) {
                                                        val tier = if (zone == TouchTargetZone.LEFT_LEG || zone == TouchTargetZone.RIGHT_LEG) {
                                                            SensitivityTier.MODERATE
                                                        } else {
                                                            SensitivityTier.HIGH
                                                        }
                                                        hapticsEngine.triggerTap(tier)
                                                    }
                                                } else {
                                                    val hit = hitTest3DVulva(
                                                        nx = nx,
                                                        ny = ny,
                                                        deform = VulvaDeformationState(
                                                            leftOuterSpread.value,
                                                            rightOuterSpread.value,
                                                            leftInnerOpen.value,
                                                            rightInnerOpen.value,
                                                            hoodRetraction.value
                                                        ),
                                                        parts = latestParts
                                                    )
                                                    if (hit != null) {
                                                        lastHitPartId = hit.id
                                                        trigger3DSensory(hit, nx, ny, isDrag = false)
                                                    }
                                                }
                                            } else {
                                                val rawDx = (pos.x - existing.lastPos.x) / w
                                                val rawDy = (pos.y - existing.lastPos.y) / h
                                                val dxNorm = rawDx / zoomScale
                                                val dyNorm = rawDy / zoomScale
                                                existing.lastPos = pos

                                                when (existing.zone) {
                                                    TouchTargetZone.LEFT_BREAST -> {
                                                        val breastGain = (1.65f / zoomScale.coerceAtLeast(0.22f)).coerceIn(2.6f, 7.5f)
                                                        dLeftBreastX += rawDx * breastGain
                                                        dLeftBreastY += -rawDy * breastGain
                                                        val nowMs = System.currentTimeMillis()
                                                        if (hypot(rawDx, rawDy) > 0.005f && nowMs - lastDragFeedbackMs >= 110L) {
                                                            lastDragFeedbackMs = nowMs
                                                            if (latestHapticsEnabled) {
                                                                hapticsEngine.triggerDragPulse(SensitivityTier.MODERATE)
                                                            }
                                                        }
                                                    }
                                                    TouchTargetZone.RIGHT_BREAST -> {
                                                        val breastGain = (1.65f / zoomScale.coerceAtLeast(0.22f)).coerceIn(2.6f, 7.5f)
                                                        dRightBreastX += rawDx * breastGain
                                                        dRightBreastY += -rawDy * breastGain
                                                        val nowMs = System.currentTimeMillis()
                                                        if (hypot(rawDx, rawDy) > 0.005f && nowMs - lastDragFeedbackMs >= 110L) {
                                                            lastDragFeedbackMs = nowMs
                                                            if (latestHapticsEnabled) {
                                                                hapticsEngine.triggerDragPulse(SensitivityTier.MODERATE)
                                                            }
                                                        }
                                                    }
                                                    TouchTargetZone.LEFT_LEG -> {
                                                        val legGain = (1.55f / zoomScale.coerceAtLeast(0.16f)).coerceIn(2.4f, 8.0f)
                                                        dLeftLeg += (-rawDx) * legGain
                                                        val nowMs = System.currentTimeMillis()
                                                        if (abs(rawDx) > 0.005f && nowMs - lastDragFeedbackMs >= 120L) {
                                                            lastDragFeedbackMs = nowMs
                                                            if (latestHapticsEnabled) {
                                                                hapticsEngine.triggerDragPulse(SensitivityTier.MODERATE)
                                                            }
                                                        }
                                                    }
                                                    TouchTargetZone.RIGHT_LEG -> {
                                                        val legGain = (1.55f / zoomScale.coerceAtLeast(0.16f)).coerceIn(2.4f, 8.0f)
                                                        dRightLeg += rawDx * legGain
                                                        val nowMs = System.currentTimeMillis()
                                                        if (abs(rawDx) > 0.005f && nowMs - lastDragFeedbackMs >= 120L) {
                                                            lastDragFeedbackMs = nowMs
                                                            if (latestHapticsEnabled) {
                                                                hapticsEngine.triggerDragPulse(SensitivityTier.MODERATE)
                                                            }
                                                        }
                                                    }
                                                    TouchTargetZone.VULVA -> {
                                                        val inHoodZone = existing.startNorm.x in 0.30f..0.70f &&
                                                            existing.startNorm.y in 0.14f..0.44f
                                                        if (inHoodZone && abs(dyNorm) > abs(dxNorm) * 0.55f) {
                                                            dHood += (-dyNorm) * 4.5f
                                                        }

                                                        val inLabiaSpan = existing.startNorm.y in 0.20f..0.86f
                                                        if (inLabiaSpan) {
                                                            if (existing.startNorm.x < 0.50f) {
                                                                val pull = -dxNorm
                                                                val nearInner = existing.startNorm.x in 0.36f..0.50f
                                                                dLeftOuter += pull * if (nearInner) 2.2f else 3.9f
                                                                dLeftInner += pull * if (nearInner) 4.5f else 3.0f
                                                            } else {
                                                                val pull = dxNorm
                                                                val nearInner = existing.startNorm.x in 0.50f..0.64f
                                                                dRightOuter += pull * if (nearInner) 2.2f else 3.9f
                                                                dRightInner += pull * if (nearInner) 4.5f else 3.0f
                                                            }
                                                        }

                                                        val nowMs = System.currentTimeMillis()
                                                        if (hypot(dxNorm, dyNorm) > 0.004f && nowMs - lastDragFeedbackMs >= 85L) {
                                                            val hit = hitTest3DVulva(
                                                                nx = nx,
                                                                ny = ny,
                                                                deform = VulvaDeformationState(
                                                                    leftOuterSpread.value,
                                                                    rightOuterSpread.value,
                                                                    leftInnerOpen.value,
                                                                    rightInnerOpen.value,
                                                                    hoodRetraction.value
                                                                ),
                                                                parts = latestParts
                                                            )
                                                            if (hit != null && (hit.id != lastHitPartId || nowMs - lastDragFeedbackMs >= 135L)) {
                                                                lastHitPartId = hit.id
                                                                lastDragFeedbackMs = nowMs
                                                                trigger3DSensory(hit, nx, ny, isDrag = true)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (dLeftOuter != 0f || dRightOuter != 0f || dLeftInner != 0f || dRightInner != 0f || dHood != 0f) {
                                            val nextLO = (leftOuterSpread.value + dLeftOuter).coerceIn(0f, 1f)
                                            val nextRO = (rightOuterSpread.value + dRightOuter).coerceIn(0f, 1f)
                                            val minLI = (nextLO - 0.16f).coerceAtLeast(0f) * 0.72f
                                            val minRI = (nextRO - 0.16f).coerceAtLeast(0f) * 0.72f
                                            val nextLI = (leftInnerOpen.value + dLeftInner).coerceAtLeast(minLI).coerceIn(0f, 1f)
                                            val nextRI = (rightInnerOpen.value + dRightInner).coerceAtLeast(minRI).coerceIn(0f, 1f)
                                            val nextH = (hoodRetraction.value + dHood).coerceIn(0f, 1f)

                                            coroutineScope.launch {
                                                leftOuterSpread.snapTo(nextLO)
                                                rightOuterSpread.snapTo(nextRO)
                                                leftInnerOpen.snapTo(nextLI)
                                                rightInnerOpen.snapTo(nextRI)
                                                hoodRetraction.snapTo(nextH)
                                            }
                                        }

                                        if (dLeftBreastX != 0f || dLeftBreastY != 0f || dRightBreastX != 0f || dRightBreastY != 0f) {
                                            val nextLBX = (leftBreastX.value + dLeftBreastX).coerceIn(-1f, 1f)
                                            val nextLBY = (leftBreastY.value + dLeftBreastY).coerceIn(-1f, 1f)
                                            val nextRBX = (rightBreastX.value + dRightBreastX).coerceIn(-1f, 1f)
                                            val nextRBY = (rightBreastY.value + dRightBreastY).coerceIn(-1f, 1f)
                                            coroutineScope.launch {
                                                leftBreastX.snapTo(nextLBX)
                                                leftBreastY.snapTo(nextLBY)
                                                rightBreastX.snapTo(nextRBX)
                                                rightBreastY.snapTo(nextRBY)
                                            }
                                        }

                                        if (dLeftLeg != 0f || dRightLeg != 0f) {
                                            val nextLL = (leftLegSpread.value + dLeftLeg).coerceIn(-0.30f, 1f)
                                            val nextRL = (rightLegSpread.value + dRightLeg).coerceIn(-0.30f, 1f)
                                            coroutineScope.launch {
                                                leftLegSpread.snapTo(nextLL)
                                                rightLegSpread.snapTo(nextRL)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                )

                // Optional 3D-Projected Anatomical Labels (hidden unless Labels toggle is ON)
                if (showLabels) {
                    val vestibuleExposure = ((currentDeform.leftInner + currentDeform.rightInner) * 0.5f).coerceIn(0f, 1f)

                    parts.forEach { part ->
                        val worldPt = VulvaAnatomySceneMesh.computeLandmark3D(part.id, currentDeform)
                        val screenNorm = VulvaGlTextureView.projectPointToNormalizedScreen(worldPt, currentCam, aspect)
                        val isSelected = part.id == selectedPartId
                        val visibilityAlpha = when (part.id) {
                            "ext_glans" -> (0.84f + currentDeform.hoodRetract * 0.16f).coerceIn(0.84f, 1f)
                            "ext_urethra", "ext_introitus" -> ((vestibuleExposure - 0.12f) / 0.35f).coerceIn(if (isSelected) 0.92f else 0.25f, 1f)
                            else -> 1f
                        }

                        val xPx = (screenNorm.x * widthPx).roundToInt()
                        val yPx = (screenNorm.y * heightPx).roundToInt()

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(xPx - 24.dp.roundToPx(), yPx - 24.dp.roundToPx()) }
                                .size(48.dp)
                                .clip(CircleShape)
                                .testTag("vulva_3d_pin_${part.id}")
                                .semantics {
                                    contentDescription = "Inspect ${part.name}, marker ${part.number}"
                                }
                                .clickable {
                                    trigger3DSensory(part, screenNorm.x, screenNorm.y, isDrag = false)
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
                            val labelOffsetY = if (screenNorm.y > 0.78f) -44.dp else 26.dp
                            val clampedX = screenNorm.x.coerceIn(0.24f, 0.76f)
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

        // Compact 3D Simulator Camera, Angle Presets & Studio Lighting Strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = isOrbitMode,
                    onClick = { isOrbitMode = !isOrbitMode },
                    modifier = Modifier.testTag("toggle_3d_orbit_mode"),
                    leadingIcon = {
                        Icon(
                            imageVector = if (isOrbitMode) Icons.Filled.ViewInAr else Icons.Filled.TouchApp,
                            contentDescription = "Switch between touching tissue and orbiting the 3D camera",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (isOrbitMode) "Orbit 3D: On" else "Orbit 3D",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }

            val visiblePresets = if (showExperimentalLegs) {
                CameraAnglePreset.entries
            } else {
                CameraAnglePreset.entries.filter { it != CameraAnglePreset.UPPER_BODY && it != CameraAnglePreset.FACE_CLOSE }
            }
            items(visiblePresets) { preset ->
                val selected = activePreset == preset
                val chipLabel = if (preset == CameraAnglePreset.FULL_LEGS && showExperimentalLegs) {
                    "Full Body"
                } else {
                    preset.label
                }
                FilterChip(
                    selected = selected,
                    onClick = { animateToPreset(preset) },
                    modifier = Modifier.testTag("camera_preset_${preset.name.lowercase()}"),
                    label = { Text(chipLabel) }
                )
            }

            item {
                FilterChip(
                    selected = dramaticSideLight,
                    onClick = { dramaticSideLight = !dramaticSideLight },
                    modifier = Modifier.testTag("toggle_3d_side_light"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = "Switch studio lighting angle",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(if (dramaticSideLight) "Side Light" else "Exam Light") }
                )
            }

            item {
                FilterChip(
                    selected = showExperimentalLegs,
                    onClick = {
                        val nextLegs = !showExperimentalLegs
                        showExperimentalLegs = nextLegs
                        if (nextLegs) {
                            animateToPreset(CameraAnglePreset.FULL_LEGS, legsEnabled = true)
                        } else {
                            animateToPreset(CameraAnglePreset.EXAM_FRONT, legsEnabled = false)
                        }
                    },
                    modifier = Modifier.testTag("toggle_3d_experimental_legs"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = "Toggle Blossom's full reclining lower body and draped exam cushion",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(if (showExperimentalLegs) "Blossom Body (Beta): On" else "Blossom Body (Beta): Off") }
                )
            }
        }

        Text(
            text = if (isOrbitMode) {
                "${BlossomBodyBuilder.BLOSSOM_MILESTONE_TAG} · Orbit 3D: drag to rotate, or pinch & slide two fingers to zoom/pan head-to-toe."
            } else {
                "${BlossomBodyBuilder.BLOSSOM_MILESTONE_TAG} · Interactive 3D: move labia, lift hood, shift breasts, or gently spread legs side-to-side."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        if (showExperimentalLegs) {
            // Hair Color Swatch Selector (defaults to warm Chestnut Brown)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hair color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(BlossomHairStyles) { index, style ->
                        val selected = index == hairStyleIndex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .testTag("blossom_hair_swatch_$index")
                                .semantics {
                                    contentDescription = "Select ${style.name} hair color"
                                }
                                .clickable {
                                    hairStyleIndex = index
                                    if (hapticsEnabled) {
                                        hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(style.baseColor)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(style.highlightColor)
                                )
                            }
                        }
                    }
                }
            }

            // Live Breast Size Slider (Petite -> Medium -> Full)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sizeLabel = when {
                    breastSize < 0.30f -> "Petite"
                    breastSize > 0.72f -> "Full"
                    else -> "Medium"
                }
                Text(
                    text = "Breast size · $sizeLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = breastSize,
                    onValueChange = { breastSize = it },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("blossom_breast_size_slider")
                        .semantics {
                            contentDescription = "Adjust breast size, currently $sizeLabel"
                        }
                )
            }
        }
    }
}

private fun hitTest3DVulva(
    nx: Float,
    ny: Float,
    deform: VulvaDeformationState,
    parts: List<AnatomyPart>
): AnatomyPart? {
    fun byId(id: String): AnatomyPart? = parts.firstOrNull { it.id == id }

    val vestibuleExposure = ((deform.leftInner + deform.rightInner) * 0.5f).coerceIn(0f, 1f)
    val cleftLeftEdge = 0.50f - (0.015f + deform.leftInner * 0.125f + deform.leftOuter * 0.035f)
    val cleftRightEdge = 0.50f + (0.015f + deform.rightInner * 0.125f + deform.rightOuter * 0.035f)
    val innerLeftOuterEdge = cleftLeftEdge - 0.068f
    val innerRightOuterEdge = cleftRightEdge + 0.068f

    return when {
        ny < 0.19f -> byId("ext_mons")
        ny > 0.80f -> byId("ext_perineum")
        abs(nx - 0.50f) < 0.095f && ny in 0.22f..0.40f -> {
            val hoodLowerEdgeY = 0.345f - deform.hoodRetract * 0.065f
            if (ny >= hoodLowerEdgeY) {
                byId("ext_glans")
            } else {
                byId("ext_hood")
            }
        }
        nx in cleftLeftEdge..cleftRightEdge && ny in 0.39f..0.77f -> {
            if (vestibuleExposure >= 0.16f) {
                if (ny < 0.56f) byId("ext_urethra") else byId("ext_introitus")
            } else {
                byId("ext_labia_minora")
            }
        }
        (nx in innerLeftOuterEdge..cleftLeftEdge || nx in cleftRightEdge..innerRightOuterEdge) &&
            ny in 0.30f..0.76f -> byId("ext_labia_minora")
        else -> byId("ext_labia_majora")
    }
}
