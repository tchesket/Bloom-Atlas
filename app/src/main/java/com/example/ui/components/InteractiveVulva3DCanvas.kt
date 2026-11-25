package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AnatomyPart
import com.example.data.model.SensitivityTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
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

private data class Pointer3DTrack(
    var lastPos: Offset,
    val startNorm: Offset,
    val zone: TouchTargetZone = TouchTargetZone.NONE,
    val vulvaFrame: ProjectedVulvaFrame? = null
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
    // Persistent leg-spread & head-turn/nod channels (stay in place until moved again or explicitly reset)
    val leftLegSpread = remember { Animatable(0f) }
    val rightLegSpread = remember { Animatable(0f) }
    val headTurn = remember { Animatable(0f) }
    val headPitch = remember { Animatable(0f) }
    // Subtle elastic buttock soft-tissue channels + natural interval eyelid blink
    val leftGluteX = remember { Animatable(0f) }
    val leftGluteY = remember { Animatable(0f) }
    val rightGluteX = remember { Animatable(0f) }
    val rightGluteY = remember { Animatable(0f) }
    val eyeBlink = remember { Animatable(0f) }
    // Unified touch-engagement stimulus (0 = calm rest, ~0.55 = breast/vulva touch, 1.0 = clitoris/hood touch)
    var touchStimulus by remember { mutableFloatStateOf(0f) }
    var lastStimulusTouchMs by remember { mutableStateOf(0L) }
    // Animated self-spread hand/arm IK pose (0 = relaxed at sides, 1 = hands holding labia open)
    var blossomSpreadPoseActive by remember { mutableStateOf(false) }
    val selfSpreadPose = remember { Animatable(0f) }

    // Live Blossom appearance customization: Hair color (default Chestnut Brown), Breast size (default Medium), Pubic hair (default Natural 0.45)
    var hairStyleIndex by remember { mutableIntStateOf(2) }
    var breastSize by remember { mutableFloatStateOf(0.50f) }
    var pubicHairAmount by remember { mutableFloatStateOf(0.45f) }
    val hairStyle = BlossomHairStyles[hairStyleIndex.coerceIn(0, BlossomHairStyles.lastIndex)]

    // Smooth 3D camera orbit, zoom, target framing, lighting & Blossom full-body state (default ON, Exam View)
    var isOrbitMode by remember { mutableStateOf(false) }
    var dramaticSideLight by remember { mutableStateOf(false) }
    var showExperimentalLegs by remember { mutableStateOf(true) }
    // Buried compatibility toggle: restores exact pre-Beta v1 vulva, seam, neck, and hair appearance
    var showAdvancedRendering by remember { mutableStateOf(false) }
    var legacyVulvaMode by remember { mutableStateOf(false) }
    // Collapsed-by-default Vulva Beta v2 experimental toggles (all OFF by default to preserve Version 13.0)
    var showVulvaBetaV2 by remember { mutableStateOf(false) }
    var betaV2SeamOnlyPolyOffset by remember { mutableStateOf(false) }
    var betaV2NonInvertingLabiaFold by remember { mutableStateOf(false) }
    var betaV2AlignedSulcus by remember { mutableStateOf(false) }
    var betaV2NaturalPelvicFinish by remember { mutableStateOf(false) }
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
    val latestBlossomSpreadPoseActive by rememberUpdatedState(blossomSpreadPoseActive)
    val latestTouchStimulus by rememberUpdatedState(touchStimulus)

    fun registerPhysiologicalStimulus(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        lastStimulusTouchMs = System.currentTimeMillis()
        touchStimulus = max(touchStimulus, clamped)
    }

    fun springBackTissue(resetLegs: Boolean = false) {
        val tissueSpring = spring<Float>(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessLow
        )
        val breastSpring = spring<Float>(
            dampingRatio = 0.50f,
            stiffness = Spring.StiffnessMediumLow
        )
        val gluteSpring = spring<Float>(
            dampingRatio = 0.58f,
            stiffness = Spring.StiffnessMedium
        )
        // While Blossom holds the self-spread pose, releasing manual touches returns labia to her held pose
        val outerRestTarget = if (latestBlossomSpreadPoseActive && !resetLegs) {
            BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET
        } else {
            0f
        }
        val innerRestTarget = if (latestBlossomSpreadPoseActive && !resetLegs) {
            BlossomArmIkRig.SELF_SPREAD_INNER_TARGET
        } else {
            0f
        }
        coroutineScope.launch { leftOuterSpread.animateTo(outerRestTarget, tissueSpring) }
        coroutineScope.launch { rightOuterSpread.animateTo(outerRestTarget, tissueSpring) }
        coroutineScope.launch { leftInnerOpen.animateTo(innerRestTarget, tissueSpring) }
        coroutineScope.launch { rightInnerOpen.animateTo(innerRestTarget, tissueSpring) }
        coroutineScope.launch { hoodRetraction.animateTo(0f, tissueSpring) }
        coroutineScope.launch { leftBreastX.animateTo(0f, breastSpring) }
        coroutineScope.launch { leftBreastY.animateTo(0f, breastSpring) }
        coroutineScope.launch { rightBreastX.animateTo(0f, breastSpring) }
        coroutineScope.launch { rightBreastY.animateTo(0f, breastSpring) }
        coroutineScope.launch { leftGluteX.animateTo(0f, gluteSpring) }
        coroutineScope.launch { leftGluteY.animateTo(0f, gluteSpring) }
        coroutineScope.launch { rightGluteX.animateTo(0f, gluteSpring) }
        coroutineScope.launch { rightGluteY.animateTo(0f, gluteSpring) }
        // Only reset legs & head pose when explicitly requested (e.g., Reset button) — they are positional, not elastic
        if (resetLegs) {
            val positionalSpring = spring<Float>(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessVeryLow
            )
            coroutineScope.launch { leftLegSpread.animateTo(0f, positionalSpring) }
            coroutineScope.launch { rightLegSpread.animateTo(0f, positionalSpring) }
            coroutineScope.launch { headTurn.animateTo(0f, positionalSpring) }
            coroutineScope.launch { headPitch.animateTo(0f, positionalSpring) }
        }
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

    LaunchedEffect(blossomSpreadPoseActive) {
        if (blossomSpreadPoseActive) {
            val remaining = (1f - selfSpreadPose.value).coerceIn(0.05f, 1f)
            val durationMs = (2500f * remaining).roundToInt().coerceAtLeast(280)
            val startLO = leftOuterSpread.value
            val startRO = rightOuterSpread.value
            val startLI = leftInnerOpen.value
            val startRI = rightInnerOpen.value
            val targetLO = max(startLO, BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET)
            val targetRO = max(startRO, BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET)
            val targetLI = max(startLI, BlossomArmIkRig.SELF_SPREAD_INNER_TARGET)
            val targetRI = max(startRI, BlossomArmIkRig.SELF_SPREAD_INNER_TARGET)

            selfSpreadPose.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
            ) {
                val spreadT = smoothStepPose(0.66f, 1.00f, value)
                if (spreadT > 0.0001f && activePointerCount == 0) {
                    val lo = startLO + (targetLO - startLO) * spreadT
                    val ro = startRO + (targetRO - startRO) * spreadT
                    val li = startLI + (targetLI - startLI) * spreadT
                    val ri = startRI + (targetRI - startRI) * spreadT
                    coroutineScope.launch {
                        leftOuterSpread.snapTo(lo)
                        rightOuterSpread.snapTo(ro)
                        leftInnerOpen.snapTo(li)
                        rightInnerOpen.snapTo(ri)
                    }
                }
            }
        } else if (selfSpreadPose.value > 0.001f) {
            val remaining = selfSpreadPose.value.coerceIn(0.05f, 1f)
            val durationMs = (2215f * remaining).roundToInt().coerceAtLeast(280)
            val startLO = leftOuterSpread.value
            val startRO = rightOuterSpread.value
            val startLI = leftInnerOpen.value
            val startRI = rightInnerOpen.value
            val startSpreadT = smoothStepPose(0.66f, 1.00f, selfSpreadPose.value)

            selfSpreadPose.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
            ) {
                if (!latestHoldOpen && activePointerCount == 0 && startSpreadT > 0.001f) {
                    val closeFrac = (smoothStepPose(0.66f, 1.00f, value) / startSpreadT).coerceIn(0f, 1f)
                    coroutineScope.launch {
                        leftOuterSpread.snapTo(startLO * closeFrac)
                        rightOuterSpread.snapTo(startRO * closeFrac)
                        leftInnerOpen.snapTo(startLI * closeFrac)
                        rightInnerOpen.snapTo(startRI * closeFrac)
                    }
                }
            }
        }
    }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            blossomSpreadPoseActive = false
            springBackTissue(resetLegs = true)
        }
    }
    LaunchedEffect(holdOpen) {
        if (!holdOpen && activePointerCount == 0) {
            springBackTissue(resetLegs = false)
        }
    }

    // Smoothly settle touch-engagement stimulus back to calm baseline ~3.5s after tactile contact ends
    LaunchedEffect(showExperimentalLegs) {
        if (!showExperimentalLegs) {
            touchStimulus = 0f
            return@LaunchedEffect
        }
        while (true) {
            delay(100L)
            val current = touchStimulus
            if (current > 0.001f) {
                val idleMs = System.currentTimeMillis() - lastStimulusTouchMs
                if (idleMs > 950L) {
                    val next = (current - 0.030f).coerceAtLeast(0f)
                    touchStimulus = next
                }
            }
        }
    }

    // Natural involuntary blinking: calm randomized intervals at rest (~3.4–6.2s), smoothly becoming
    // more frequent during breast/vulva touch (~1.8–3.2s) and especially clitoral engagement (~1.1–2.1s)
    LaunchedEffect(showExperimentalLegs) {
        if (!showExperimentalLegs) {
            eyeBlink.snapTo(0f)
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis())
        delay(1600L)
        while (true) {
            // Skip animating 60 FPS eyelid blinks when zoomed tightly onto the pelvis (face at y ~ 6.35 is off-screen)
            val faceCouldBeVisible = camZoom.value <= 0.62f || camTargetY.value >= 2.2f
            if (faceCouldBeVisible) {
                val stimAtBlink = latestTouchStimulus.coerceIn(0f, 1f)
                val closeMs = (75 + (stimAtBlink * 18f)).roundToInt()
                val holdMs = (28 + (stimAtBlink * 16f)).roundToInt().toLong()
                val openMs = (135 + (stimAtBlink * 22f)).roundToInt()
                eyeBlink.animateTo(1f, tween(durationMillis = closeMs, easing = FastOutSlowInEasing))
                delay(holdMs)
                eyeBlink.animateTo(0f, tween(durationMillis = openMs, easing = FastOutSlowInEasing))

                val doubleBlinkProb = 0.16f + 0.16f * stimAtBlink
                if (rng.nextFloat() < doubleBlinkProb) {
                    delay(145L)
                    eyeBlink.animateTo(1f, tween(durationMillis = 70, easing = FastOutSlowInEasing))
                    delay(22L)
                    eyeBlink.animateTo(0f, tween(durationMillis = 130, easing = FastOutSlowInEasing))
                }
            } else if (eyeBlink.value > 0.001f) {
                eyeBlink.snapTo(0f)
            }

            // Slice wait into 120ms steps so newly starting breast/vulva/clitoris touches shorten the wait immediately
            val jitter = rng.nextFloat()
            var elapsedWaitMs = 0L
            while (true) {
                val range = computeBlinkIntervalRangeMs(latestTouchStimulus)
                val targetWaitMs = range.first + ((range.last - range.first) * jitter).toLong()
                if (elapsedWaitMs >= targetWaitMs) break
                delay(120L)
                elapsedWaitMs += 120L
            }
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
        rightLegSpread = rightLegSpread.value,
        selfSpreadPose = selfSpreadPose.value,
        headTurn = headTurn.value,
        headPitch = headPitch.value,
        eyeBlink = eyeBlink.value,
        leftGluteX = leftGluteX.value,
        leftGluteY = leftGluteY.value,
        rightGluteX = rightGluteX.value,
        rightGluteY = rightGluteY.value,
        legacyVulvaMode = legacyVulvaMode,
        betaV2SeamOnlyPolyOffset = betaV2SeamOnlyPolyOffset,
        betaV2NonInvertingLabiaFold = betaV2NonInvertingLabiaFold,
        betaV2AlignedSulcus = betaV2AlignedSulcus,
        betaV2NaturalPelvicFinish = betaV2NaturalPelvicFinish
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

                val lifecycleOwner = LocalLifecycleOwner.current
                var glViewRef by remember { mutableStateOf<VulvaGlTextureView?>(null) }

                DisposableEffect(lifecycleOwner, glViewRef) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> glViewRef?.setRenderingPaused(true)
                            Lifecycle.Event.ON_RESUME -> glViewRef?.setRenderingPaused(false)
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        VulvaGlTextureView(
                            context = ctx,
                            onGlError = { glUnavailable = true }
                        ).also { glViewRef = it }
                    },
                    update = { glView ->
                        glViewRef = glView
                        glView.deformationState = currentDeform
                        glView.cameraState = currentCam
                        glView.toneStyle = tone
                        glView.hairStyle = hairStyle
                        glView.pubicHairAmount = pubicHairAmount
                        glView.touchGlow = currentGlow
                        glView.dramaticSideLight = dramaticSideLight
                        glView.showExperimentalLegs = showExperimentalLegs
                        glView.touchStimulus = touchStimulus
                        glView.requestFrame()
                    },
                    onRelease = { glView ->
                        if (glViewRef === glView) glViewRef = null
                        glView.shutdown()
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
                                var touchedElasticZone = false

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
                                        if (!latestOrbitMode && !latestHoldOpen && touchedElasticZone) {
                                            springBackTissue(resetLegs = false)
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
                                                val nextYaw = (camYaw.value - dx * 130f).coerceIn(-maxYaw, maxYaw)
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
                                        var dHeadTurn = 0f
                                        var dHeadPitch = 0f
                                        var dLeftGluteX = 0f
                                        var dLeftGluteY = 0f
                                        var dRightGluteX = 0f
                                        var dRightGluteY = 0f

                                        val zoomScale = camZoom.value.coerceIn(0.10f, 1.90f)
                                        val liveCam = VulvaCamera3D(
                                            yawDeg = camYaw.value,
                                            pitchDeg = camPitch.value,
                                            zoom = camZoom.value,
                                            targetY = camTargetY.value,
                                            targetZ = camTargetZ.value
                                        )
                                        val aspectLive = w / h
                                        val liveDeform = VulvaDeformationState(
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
                                            rightLegSpread = rightLegSpread.value,
                                            selfSpreadPose = selfSpreadPose.value,
                                            headTurn = headTurn.value,
                                            headPitch = headPitch.value,
                                            eyeBlink = eyeBlink.value,
                                            leftGluteX = leftGluteX.value,
                                            leftGluteY = leftGluteY.value,
                                            rightGluteX = rightGluteX.value,
                                            rightGluteY = rightGluteY.value
                                        )

                                        pressed.forEach { change ->
                                            change.consume()
                                            val pos = change.position
                                            val rawNx = (pos.x / w).coerceIn(0f, 1f)
                                            val rawNy = (pos.y / h).coerceIn(0f, 1f)
                                            val rawScreen = Offset(rawNx, rawNy)
                                            val existing = tracks[change.id]

                                            if (existing == null) {
                                                val hitResult = BlossomTouchHitResolver.resolveTouchZone(
                                                    rawScreenNorm = rawScreen,
                                                    cam = liveCam,
                                                    aspect = aspectLive,
                                                    deform = liveDeform,
                                                    showFullBody = latestShowFullBody
                                                )
                                                val zone = hitResult.zone
                                                val startLocal = hitResult.vulvaLocalNorm ?: rawScreen
                                                tracks[change.id] = Pointer3DTrack(
                                                    lastPos = pos,
                                                    startNorm = startLocal,
                                                    zone = zone,
                                                    vulvaFrame = hitResult.vulvaFrame
                                                )

                                                if (zone == TouchTargetZone.VULVA ||
                                                    zone == TouchTargetZone.LEFT_BREAST ||
                                                    zone == TouchTargetZone.RIGHT_BREAST ||
                                                    zone == TouchTargetZone.LEFT_GLUTE ||
                                                    zone == TouchTargetZone.RIGHT_GLUTE
                                                ) {
                                                    touchedElasticZone = true
                                                }

                                                when (zone) {
                                                    TouchTargetZone.NONE -> {
                                                        // Cleanly ignore touches on arms, hands, torso, or background
                                                    }
                                                    TouchTargetZone.LEFT_BREAST,
                                                    TouchTargetZone.RIGHT_BREAST -> {
                                                        registerPhysiologicalStimulus(0.55f)
                                                        if (latestHapticsEnabled) {
                                                            hapticsEngine.triggerTap(SensitivityTier.HIGH)
                                                        }
                                                    }
                                                    TouchTargetZone.LEFT_LEG,
                                                    TouchTargetZone.RIGHT_LEG,
                                                    TouchTargetZone.LEFT_GLUTE,
                                                    TouchTargetZone.RIGHT_GLUTE,
                                                    TouchTargetZone.HEAD -> {
                                                        if (latestHapticsEnabled) {
                                                            hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                                        }
                                                    }
                                                    TouchTargetZone.VULVA -> {
                                                        val hit = hitTest3DVulva(
                                                            nx = startLocal.x,
                                                            ny = startLocal.y,
                                                            deform = liveDeform,
                                                            parts = latestParts
                                                        )
                                                        val isClitoralHit = hit?.id == "ext_glans" || hit?.id == "ext_hood" ||
                                                            hit?.sensitivityTier == SensitivityTier.ULTRA
                                                        registerPhysiologicalStimulus(if (isClitoralHit) 1.0f else 0.62f)
                                                        if (hit != null) {
                                                            lastHitPartId = hit.id
                                                            trigger3DSensory(hit, startLocal.x, startLocal.y, isDrag = false)
                                                        }
                                                    }
                                                }
                                            } else {
                                                val rawDx = (pos.x - existing.lastPos.x) / w
                                                val rawDy = (pos.y - existing.lastPos.y) / h
                                                existing.lastPos = pos

                                                when (existing.zone) {
                                                    TouchTargetZone.NONE -> {
                                                        // Ignored non-interactive touch: zero tissue stretch, zero haptics
                                                    }
                                                    TouchTargetZone.LEFT_BREAST -> {
                                                        val breastGain = (1.65f / zoomScale.coerceAtLeast(0.22f)).coerceIn(2.6f, 7.5f)
                                                        dLeftBreastX += rawDx * breastGain
                                                        dLeftBreastY += -rawDy * breastGain
                                                        if (hypot(rawDx, rawDy) > 0.002f) {
                                                            registerPhysiologicalStimulus(0.58f)
                                                        }
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
                                                        if (hypot(rawDx, rawDy) > 0.002f) {
                                                            registerPhysiologicalStimulus(0.58f)
                                                        }
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
                                                    TouchTargetZone.LEFT_GLUTE -> {
                                                        val gluteGain = (1.50f / zoomScale.coerceAtLeast(0.20f)).coerceIn(2.4f, 6.8f)
                                                        dLeftGluteX += rawDx * gluteGain
                                                        dLeftGluteY += -rawDy * gluteGain
                                                        val nowMs = System.currentTimeMillis()
                                                        if (hypot(rawDx, rawDy) > 0.006f && nowMs - lastDragFeedbackMs >= 125L) {
                                                            lastDragFeedbackMs = nowMs
                                                            if (latestHapticsEnabled) {
                                                                hapticsEngine.triggerDragPulse(SensitivityTier.MODERATE)
                                                            }
                                                        }
                                                    }
                                                    TouchTargetZone.RIGHT_GLUTE -> {
                                                        val gluteGain = (1.50f / zoomScale.coerceAtLeast(0.20f)).coerceIn(2.4f, 6.8f)
                                                        dRightGluteX += rawDx * gluteGain
                                                        dRightGluteY += -rawDy * gluteGain
                                                        val nowMs = System.currentTimeMillis()
                                                        if (hypot(rawDx, rawDy) > 0.006f && nowMs - lastDragFeedbackMs >= 125L) {
                                                            lastDragFeedbackMs = nowMs
                                                            if (latestHapticsEnabled) {
                                                                hapticsEngine.triggerDragPulse(SensitivityTier.MODERATE)
                                                            }
                                                        }
                                                    }
                                                    TouchTargetZone.HEAD -> {
                                                        val headGain = (1.95f / zoomScale.coerceAtLeast(0.24f)).coerceIn(2.6f, 7.2f)
                                                        val headPitchGain = (1.65f / zoomScale.coerceAtLeast(0.24f)).coerceIn(2.2f, 6.2f)
                                                        dHeadTurn += rawDx * headGain
                                                        dHeadPitch += rawDy * headPitchGain
                                                        val nowMs = System.currentTimeMillis()
                                                        if (hypot(rawDx, rawDy) > 0.005f && nowMs - lastDragFeedbackMs >= 120L) {
                                                            lastDragFeedbackMs = nowMs
                                                            if (latestHapticsEnabled) {
                                                                hapticsEngine.triggerDragPulse(SensitivityTier.MODERATE)
                                                            }
                                                        }
                                                    }
                                                    TouchTargetZone.VULVA -> {
                                                        val vFrame = existing.vulvaFrame
                                                        val localDelta = vFrame?.screenDeltaToLocalDelta(rawDx, rawDy)
                                                            ?: Offset(rawDx / zoomScale, rawDy / zoomScale)
                                                        val dxNorm = localDelta.x
                                                        val dyNorm = localDelta.y
                                                        val curLocal = vFrame?.screenToLocalNorm(rawScreen) ?: existing.startNorm

                                                        val inHoodZone = existing.startNorm.x in 0.28f..0.72f &&
                                                            existing.startNorm.y in 0.14f..0.46f
                                                        if (inHoodZone && abs(dyNorm) > abs(dxNorm) * 0.55f) {
                                                            dHood += (-dyNorm) * 4.5f
                                                        }

                                                        val inLabiaSpan = existing.startNorm.y in 0.18f..0.88f
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
                                                        if (hypot(dxNorm, dyNorm) > 0.003f) {
                                                            val dragHit = hitTest3DVulva(
                                                                nx = curLocal.x,
                                                                ny = curLocal.y,
                                                                deform = liveDeform,
                                                                parts = latestParts
                                                            )
                                                            val isClitDrag = inHoodZone || dragHit?.id == "ext_glans" ||
                                                                dragHit?.id == "ext_hood" || dragHit?.sensitivityTier == SensitivityTier.ULTRA
                                                            registerPhysiologicalStimulus(if (isClitDrag) 1.0f else 0.64f)
                                                            if (hypot(dxNorm, dyNorm) > 0.004f && nowMs - lastDragFeedbackMs >= 85L &&
                                                                dragHit != null && (dragHit.id != lastHitPartId || nowMs - lastDragFeedbackMs >= 135L)
                                                            ) {
                                                                lastHitPartId = dragHit.id
                                                                lastDragFeedbackMs = nowMs
                                                                trigger3DSensory(dragHit, curLocal.x, curLocal.y, isDrag = true)
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

                                        if (dLeftGluteX != 0f || dLeftGluteY != 0f || dRightGluteX != 0f || dRightGluteY != 0f) {
                                            val nextLGX = (leftGluteX.value + dLeftGluteX).coerceIn(-1f, 1f)
                                            val nextLGY = (leftGluteY.value + dLeftGluteY).coerceIn(-1f, 1f)
                                            val nextRGX = (rightGluteX.value + dRightGluteX).coerceIn(-1f, 1f)
                                            val nextRGY = (rightGluteY.value + dRightGluteY).coerceIn(-1f, 1f)
                                            coroutineScope.launch {
                                                leftGluteX.snapTo(nextLGX)
                                                leftGluteY.snapTo(nextLGY)
                                                rightGluteX.snapTo(nextRGX)
                                                rightGluteY.snapTo(nextRGY)
                                            }
                                        }

                                        if (dHeadTurn != 0f || dHeadPitch != 0f) {
                                            val nextHead = (headTurn.value + dHeadTurn).coerceIn(-1f, 1f)
                                            val nextPitch = (headPitch.value + dHeadPitch).coerceIn(0f, 1f)
                                            coroutineScope.launch {
                                                headTurn.snapTo(nextHead)
                                                headPitch.snapTo(nextPitch)
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

        // Row 1: 3D Motion, Pose & Studio Controls (separate from Camera Views)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("controls_action_row")
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

            if (showExperimentalLegs) {
                item {
                    FilterChip(
                        selected = blossomSpreadPoseActive,
                        onClick = {
                            blossomSpreadPoseActive = !blossomSpreadPoseActive
                            if (hapticsEnabled) {
                                hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                            }
                        },
                        modifier = Modifier.testTag("toggle_blossom_self_spread"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.PanTool,
                                contentDescription = if (blossomSpreadPoseActive) {
                                    "Release Blossom's hands back to resting position"
                                } else {
                                    "Ask Blossom to gently spread her labia open with her hands"
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (blossomSpreadPoseActive) "Release Pose" else "Ask Blossom to Spread",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
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
                            blossomSpreadPoseActive = false
                            coroutineScope.launch { selfSpreadPose.snapTo(0f) }
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
                    label = { Text(if (showExperimentalLegs) "Full Body: On" else "Full Body: Off") }
                )
            }
        }

        // Row 2: Dedicated Camera View Presets Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("camera_views_row")
        ) {
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
        }

        Text(
            text = if (isOrbitMode) {
                "${BlossomBodyBuilder.BLOSSOM_MILESTONE_TAG} · Orbit 3D: drag to rotate, or pinch & slide two fingers to zoom/pan head-to-toe."
            } else {
                "${BlossomBodyBuilder.BLOSSOM_MILESTONE_TAG} · Touch labia, hood, breasts, or glutes; drag legs or face to turn/nod (they stay put), or ask Blossom to spread."
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

        // Live Pubic Hair Slider (Shaven -> Trimmed -> Natural [default] -> Full -> Very Bushy)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val hairDensityLabel = when {
                pubicHairAmount < 0.06f -> "Shaven"
                pubicHairAmount < 0.28f -> "Trimmed"
                pubicHairAmount < 0.62f -> "Natural"
                pubicHairAmount < 0.84f -> "Full"
                else -> "Very Bushy"
            }
            Text(
                text = "Pubic hair · $hairDensityLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = pubicHairAmount,
                onValueChange = { pubicHairAmount = it },
                valueRange = 0f..1f,
                modifier = Modifier
                    .weight(1f)
                    .testTag("blossom_pubic_hair_slider")
                    .semantics {
                        contentDescription = "Adjust pubic hair amount, currently $hairDensityLabel"
                    }
            )
        }

        // Collapsed-by-default Vulva Beta v2 experimental section (4 independent toggles, all OFF by default)
        val activeBetaV2Count = listOf(
            betaV2SeamOnlyPolyOffset,
            betaV2NonInvertingLabiaFold,
            betaV2AlignedSulcus,
            betaV2NaturalPelvicFinish
        ).count { it }
        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showVulvaBetaV2 = !showVulvaBetaV2 }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .testTag("vulva_beta_v2_toggle")
                    .semantics {
                        contentDescription = if (showVulvaBetaV2) {
                            "Collapse Vulva Beta v2 experimental options"
                        } else {
                            "Expand Vulva Beta v2 experimental options"
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (activeBetaV2Count > 0) {
                        "Vulva Beta v2 · $activeBetaV2Count active"
                    } else {
                        "Vulva Beta v2"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (showVulvaBetaV2) Icons.Filled.KeyboardArrowUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = showVulvaBetaV2) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        VulvaBetaToggleRow(
                            title = "1. Seam-only depth offset",
                            subtitle = "Restricts patch polygon offset to the outer seam ring and keeps lower tips above the patch",
                            checked = betaV2SeamOnlyPolyOffset,
                            tag = "toggle_beta_v2_seam_poly_offset",
                            onToggle = {
                                betaV2SeamOnlyPolyOffset = !betaV2SeamOnlyPolyOffset
                                if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        VulvaBetaToggleRow(
                            title = "2. Non-inverting fold geometry",
                            subtitle = "Expands outer labial skirt dynamically when parted and prevents midline/glans clipping",
                            checked = betaV2NonInvertingLabiaFold,
                            tag = "toggle_beta_v2_non_inverting_fold",
                            onToggle = {
                                betaV2NonInvertingLabiaFold = !betaV2NonInvertingLabiaFold
                                if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        VulvaBetaToggleRow(
                            title = "3. Aligned interlabial sulcus",
                            subtitle = "Shifts and softens the interlabial groove cleanly between inner and outer labia",
                            checked = betaV2AlignedSulcus,
                            tag = "toggle_beta_v2_aligned_sulcus",
                            onToggle = {
                                betaV2AlignedSulcus = !betaV2AlignedSulcus
                                if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        VulvaBetaToggleRow(
                            title = "4. Natural satin-velvet finish",
                            subtitle = "Damps grazing rim glare and broad specular polish on pelvic folds only",
                            checked = betaV2NaturalPelvicFinish,
                            tag = "toggle_beta_v2_natural_pelvic_finish",
                            onToggle = {
                                betaV2NaturalPelvicFinish = !betaV2NaturalPelvicFinish
                                if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                            }
                        )
                    }
                }
            }
        }

        // Discreet, collapsed-by-default Advanced rendering disclosure holding the Legacy Vulva fallback option
        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showAdvancedRendering = !showAdvancedRendering }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .testTag("advanced_rendering_toggle")
                    .semantics {
                        contentDescription = if (showAdvancedRendering) {
                            "Collapse advanced rendering options"
                        } else {
                            "Expand advanced rendering options"
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (legacyVulvaMode) "Advanced rendering · Legacy active" else "Advanced rendering",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (showAdvancedRendering) Icons.Filled.KeyboardArrowUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = showAdvancedRendering) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                legacyVulvaMode = !legacyVulvaMode
                                if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("toggle_legacy_vulva")
                            .semantics {
                                contentDescription = "Legacy vulva, ${if (legacyVulvaMode) "enabled" else "disabled"}. Use the original pre-Beta v1 appearance."
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Legacy vulva",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use original pre-Beta v1 vulva, seam, neck, and hair appearance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = legacyVulvaMode,
                            onCheckedChange = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VulvaBetaToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(tag)
            .semantics {
                contentDescription = "$title, ${if (checked) "enabled" else "disabled"}. $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
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

private fun smoothStepPose(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Computes randomized blink wait range (min..max ms) from touch engagement stimulus [0..1]:
 *  - Rest (0.0): calm ~3.4–6.2s
 *  - Breasts / Vulva (~0.55–0.65): subtly more frequent ~1.8–3.2s
 *  - Clitoris / Clitoral Hood (1.0): noticeably more frequent ~1.1–2.1s
 */
internal fun computeBlinkIntervalRangeMs(stimulus: Float): LongRange {
    val s = stimulus.coerceIn(0f, 1f)
    val minMs = (3400f - 2300f * s).roundToInt().toLong()
    val maxMs = (6200f - 4100f * s).roundToInt().toLong()
    return minMs..maxMs
}

