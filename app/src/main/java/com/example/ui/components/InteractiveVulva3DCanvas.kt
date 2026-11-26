package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.draw.scale
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
    val eyeBlinkR = remember { Animatable(0f) }
    // Lifelike Beta (Tab 2) live micro-behavior channels
    val eyeGazeX = remember { Animatable(0f) }
    val eyeGazeY = remember { Animatable(0f) }
    val idleHeadTurn = remember { Animatable(0f) }
    val idleHeadPitch = remember { Animatable(0f) }
    val idleHeadRoll = remember { Animatable(0f) }
    val faceWarmth = remember { Animatable(0f) }
    val faceBrow = remember { Animatable(0f) }
    val faceLipSoft = remember { Animatable(0f) }
    val shoulderRelaxL = remember { Animatable(0f) }
    val shoulderRelaxR = remember { Animatable(0f) }
    // Lifelike+ Beta (Tab 3) live micro-behavior channels (all strictly 0f when their respective toggles are OFF)
    val anticipationGazeX = remember { Animatable(0f) }
    val anticipationGazeY = remember { Animatable(0f) }
    val swallowPhase = remember { Animatable(0f) }
    val settleHeadTurn = remember { Animatable(0f) }
    val settleHeadPitch = remember { Animatable(0f) }
    val settleHeadRoll = remember { Animatable(0f) }
    val settleShoulderL = remember { Animatable(0f) }
    val settleShoulderR = remember { Animatable(0f) }
    val settleLegL = remember { Animatable(0f) }
    val settleLegR = remember { Animatable(0f) }
    val nostrilBreath = remember { Animatable(0f) }
    val breathLipEase = remember { Animatable(0f) }
    val livingWarmth = remember { Animatable(0f) }
    var posturalSettleTrigger by remember { mutableIntStateOf(0) }
    var lastManualHeadDragMs by remember { mutableStateOf(0L) }
    // Unified touch-engagement stimulus & realistic cardio-respiratory heart rate (66..142 BPM)
    var touchStimulus by remember { mutableFloatStateOf(0f) }
    var lastStimulusTouchMs by remember { mutableStateOf(0L) }
    var heartRateBpm by remember { mutableFloatStateOf(VulvaGlTextureView.RESTING_HEART_RATE_BPM) }
    var heartRateSensitivity by remember { mutableFloatStateOf(0f) }
    var heartRateDrive by remember { mutableFloatStateOf(0f) }
    var lastHeartStimMs by remember { mutableStateOf(0L) }
    val heartPulseScale = remember { Animatable(1f) }
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
    // Floating always-on-top 3-Tab Beta Lab overlay:
    //  Tab 0 (Vulva): Beta v2 is now baked live; holds 5 independent Beta v3 anatomical realism toggles + Breast Size Boost (all OFF by default)
    //  Tab 1 (Life):  all 6 promoted ON by default
    //  Tab 2 (Life+): 7 subtle realism features + Heart Rate HUD toggle (all OFF by default)
    var showBetaMenu by remember { mutableStateOf(false) }
    var betaMenuTab by remember { mutableIntStateOf(0) } // 0 = Vulva, 1 = Life, 2 = Life+
    var betaV3OrganicContinuity by remember { mutableStateOf(false) }
    var betaV3ContinuityStrength by remember { mutableFloatStateOf(0.65f) }
    var betaV3SoftPigmentation by remember { mutableStateOf(false) }
    var betaV3PigmentStrength by remember { mutableFloatStateOf(0.70f) }
    var betaV3AnatomicalAsymmetry by remember { mutableStateOf(false) }
    var betaV3AsymmetryStrength by remember { mutableFloatStateOf(0.65f) }
    var betaV3RegionalSurfaceDetail by remember { mutableStateOf(false) }
    var betaV3SurfaceStrength by remember { mutableFloatStateOf(0.65f) }
    var betaV3NaturalLighting by remember { mutableStateOf(false) }
    var betaV3LightingStrength by remember { mutableFloatStateOf(0.70f) }
    var betaBreastSizeBoost by remember { mutableStateOf(false) }
    // Lifelike Beta (Tab 2) toggles (promoted ON by default)
    var betaLifeEyeMicrosaccades by remember { mutableStateOf(true) }
    var betaLifeDoubleBlinking by remember { mutableStateOf(true) }
    var betaLifeHeadRepositioning by remember { mutableStateOf(true) }
    var betaLifeDeepBreathsSighs by remember { mutableStateOf(true) }
    var betaLifeShoulderRelaxation by remember { mutableStateOf(true) }
    var betaLifeFacialExpressions by remember { mutableStateOf(true) }
    // Lifelike+ Beta (Tab 3) next-gen realism toggles (all OFF by default!)
    var betaLife2EyelidTracking by remember { mutableStateOf(false) }
    var betaLife2AsymmetricBlinking by remember { mutableStateOf(false) }
    var betaLife2GazeAnticipation by remember { mutableStateOf(false) }
    var betaLife2SubtleSwallowing by remember { mutableStateOf(false) }
    var betaLife2PosturalSettling by remember { mutableStateOf(false) }
    var betaLife2BreathFaceCoupling by remember { mutableStateOf(false) }
    var betaLife2LivingWarmth by remember { mutableStateOf(false) }
    var betaLife2ShowHeartRate by remember { mutableStateOf(false) }
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
    val latestHeartRateBpm by rememberUpdatedState(heartRateBpm)
    val latestBetaLifeDoubleBlinking by rememberUpdatedState(betaLifeDoubleBlinking)
    val latestBetaLife2AsymmetricBlinking by rememberUpdatedState(betaLife2AsymmetricBlinking)
    val latestBetaLife2GazeAnticipation by rememberUpdatedState(betaLife2GazeAnticipation)
    val latestBetaLife2PosturalSettling by rememberUpdatedState(betaLife2PosturalSettling)

    fun registerPhysiologicalStimulus(level: Float, isDragMotion: Boolean = false) {
        val clamped = level.coerceIn(0f, 1f)
        val nowMs = System.currentTimeMillis()
        lastStimulusTouchMs = nowMs
        touchStimulus = max(touchStimulus, clamped)
        lastHeartStimMs = nowMs
        // Weight sensitivity smoothly upward toward the most sensitive active contact zone
        heartRateSensitivity = max(heartRateSensitivity * 0.86f, clamped)
        heartRateDrive = max(heartRateDrive, if (isDragMotion) 1.0f else 0.58f)
        if (!isDragMotion) {
            // Subtle immediate autonomic pulse bump on tap/contact scaled by zone sensitivity
            val tapCeiling = VulvaGlTextureView.RESTING_HEART_RATE_BPM +
                (VulvaGlTextureView.MAX_HEART_RATE_BPM - VulvaGlTextureView.RESTING_HEART_RATE_BPM) * (0.18f + 0.82f * clamped)
            val tapBump = 0.6f + 2.4f * clamped
            if (heartRateBpm < tapCeiling) {
                heartRateBpm = (heartRateBpm + tapBump).coerceAtMost(tapCeiling)
            }
        }
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
            coroutineScope.launch { idleHeadTurn.animateTo(0f, positionalSpring) }
            coroutineScope.launch { idleHeadPitch.animateTo(0f, positionalSpring) }
            coroutineScope.launch { idleHeadRoll.animateTo(0f, positionalSpring) }
            coroutineScope.launch { settleHeadTurn.animateTo(0f, positionalSpring) }
            coroutineScope.launch { settleHeadPitch.animateTo(0f, positionalSpring) }
            coroutineScope.launch { settleHeadRoll.animateTo(0f, positionalSpring) }
            coroutineScope.launch { settleLegL.animateTo(0f, positionalSpring) }
            coroutineScope.launch { settleLegR.animateTo(0f, positionalSpring) }
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
            if (latestBetaLife2PosturalSettling) posturalSettleTrigger++
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

    // Smoothly integrate physiological heart rate & settle touch-engagement stimulus back to calm baseline
    LaunchedEffect(showExperimentalLegs) {
        if (!showExperimentalLegs) {
            touchStimulus = 0f
            heartRateBpm = VulvaGlTextureView.RESTING_HEART_RATE_BPM
            heartRateSensitivity = 0f
            heartRateDrive = 0f
            return@LaunchedEffect
        }
        var lastTickMs = System.currentTimeMillis()
        while (true) {
            delay(65L)
            val nowMs = System.currentTimeMillis()
            val dtSec = ((nowMs - lastTickMs) / 1000f).coerceIn(0.01f, 0.25f)
            lastTickMs = nowMs

            val currentStim = touchStimulus
            if (currentStim > 0.001f) {
                val idleMs = nowMs - lastStimulusTouchMs
                if (idleMs > 950L) {
                    touchStimulus = (currentStim - 0.019f).coerceAtLeast(0f)
                }
            }

            val secsSinceHeartStim = ((nowMs - lastHeartStimMs).coerceAtLeast(0L)) / 1000f
            if (secsSinceHeartStim > 0.28f && heartRateDrive > 0f) {
                heartRateDrive = (heartRateDrive - dtSec * 2.2f).coerceAtLeast(0f)
            }
            if (secsSinceHeartStim > 1.2f && heartRateSensitivity > 0f) {
                heartRateSensitivity = (heartRateSensitivity - dtSec * 0.18f).coerceAtLeast(0f)
            }
            heartRateBpm = VulvaGlTextureView.stepHeartRateBpm(
                currentBpm = heartRateBpm,
                dtSec = dtSec,
                targetSensitivityGain = heartRateSensitivity,
                activeDrive = heartRateDrive,
                secondsSinceLastStim = secsSinceHeartStim
            )
        }
    }

    // Gentle beat-synchronized pulse for the optional top-left Heart Rate HUD badge
    LaunchedEffect(showExperimentalLegs, betaLife2ShowHeartRate) {
        if (!showExperimentalLegs || !betaLife2ShowHeartRate) {
            heartPulseScale.snapTo(1f)
            return@LaunchedEffect
        }
        while (true) {
            val bpm = latestHeartRateBpm.coerceIn(
                VulvaGlTextureView.RESTING_HEART_RATE_BPM,
                VulvaGlTextureView.MAX_HEART_RATE_BPM
            )
            val beatPeriodMs = (60_000f / bpm).roundToInt().coerceIn(380, 1050)
            val hrNorm = VulvaGlTextureView.heartRateExertionNorm(bpm)
            val systoleScale = 1.12f + 0.08f * hrNorm
            val systoleMs = (beatPeriodMs * 0.18f).roundToInt().coerceIn(75, 160)
            val diastoleMs = (beatPeriodMs * 0.28f).roundToInt().coerceIn(110, 240)
            heartPulseScale.animateTo(systoleScale, tween(systoleMs, easing = FastOutSlowInEasing))
            heartPulseScale.animateTo(1.0f, tween(diastoleMs, easing = FastOutSlowInEasing))
            val pauseMs = (beatPeriodMs - systoleMs - diastoleMs).coerceAtLeast(90).toLong()
            delay(pauseMs)
        }
    }

    // Natural involuntary blinking: calm randomized intervals at rest (~3.4–6.2s), smoothly becoming
    // more frequent during breast/vulva touch (~1.8–3.2s) and especially clitoral engagement (~1.1–2.1s).
    // Supports Tab 2 "Occasional double blinking" and Tab 3 "Asymmetric blinking" (16–38ms L/R micro-offset).
    LaunchedEffect(showExperimentalLegs) {
        if (!showExperimentalLegs) {
            eyeBlink.snapTo(0f)
            eyeBlinkR.snapTo(0f)
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis())

        suspend fun runSingleBlink(
            amp: Float,
            closeMs: Int,
            holdMs: Long,
            openMs: Int,
            asymmetric: Boolean
        ) {
            if (!asymmetric) {
                eyeBlink.animateTo(amp, tween(durationMillis = closeMs, easing = FastOutSlowInEasing))
                delay(holdMs)
                eyeBlink.animateTo(0f, tween(durationMillis = openMs, easing = FastOutSlowInEasing))
                if (eyeBlinkR.value > 0.001f) eyeBlinkR.snapTo(0f)
                return
            }
            val leadLeft = rng.nextFloat() < 0.54f
            val staggerMs = (16 + rng.nextInt(23)).toLong()
            val closeJitter = (rng.nextInt(19) - 9)
            val openJitter = (rng.nextInt(25) - 12)
            val trailAmp = (amp * (0.95f + rng.nextFloat() * 0.05f)).coerceIn(0f, 1f)
            val leftJob = coroutineScope.launch {
                if (!leadLeft) delay(staggerMs)
                val cMs = (closeMs + if (leadLeft) 0 else closeJitter).coerceAtLeast(48)
                val oMs = (openMs + if (leadLeft) 0 else openJitter).coerceAtLeast(90)
                val a = if (leadLeft) amp else trailAmp
                eyeBlink.animateTo(a, tween(durationMillis = cMs, easing = FastOutSlowInEasing))
                delay(holdMs)
                eyeBlink.animateTo(0f, tween(durationMillis = oMs, easing = FastOutSlowInEasing))
            }
            val rightJob = coroutineScope.launch {
                if (leadLeft) delay(staggerMs)
                val cMs = (closeMs + if (!leadLeft) 0 else closeJitter).coerceAtLeast(48)
                val oMs = (openMs + if (!leadLeft) 0 else openJitter).coerceAtLeast(90)
                val a = if (!leadLeft) amp else trailAmp
                eyeBlinkR.animateTo(a, tween(durationMillis = cMs, easing = FastOutSlowInEasing))
                delay(holdMs)
                eyeBlinkR.animateTo(0f, tween(durationMillis = oMs, easing = FastOutSlowInEasing))
            }
            leftJob.join()
            rightJob.join()
        }

        delay(1600L)
        while (true) {
            // Skip animating 60 FPS eyelid blinks when zoomed tightly onto the pelvis (face at y ~ 6.35 is off-screen)
            val faceCouldBeVisible = camZoom.value <= 0.62f || camTargetY.value >= 2.2f
            if (faceCouldBeVisible) {
                val stimAtBlink = latestTouchStimulus.coerceIn(0f, 1f)
                val enhancedDoubleBlink = latestBetaLifeDoubleBlinking
                val asymBlink = latestBetaLife2AsymmetricBlinking
                if (!enhancedDoubleBlink) {
                    val closeMs = (75 + (stimAtBlink * 18f)).roundToInt()
                    val holdMs = (28 + (stimAtBlink * 16f)).roundToInt().toLong()
                    val openMs = (135 + (stimAtBlink * 22f)).roundToInt()
                    runSingleBlink(1f, closeMs, holdMs, openMs, asymBlink)

                    val doubleBlinkProb = 0.16f + 0.16f * stimAtBlink
                    if (rng.nextFloat() < doubleBlinkProb) {
                        delay(145L)
                        runSingleBlink(1f, 70, 22L, 130, asymBlink)
                    }
                } else {
                    val isSoftHalfFlutter = rng.nextFloat() < 0.18f
                    if (isSoftHalfFlutter) {
                        val amp = 0.44f + rng.nextFloat() * 0.20f
                        runSingleBlink(
                            amp = amp,
                            closeMs = 95 + rng.nextInt(40),
                            holdMs = 24L + rng.nextInt(26),
                            openMs = 155 + rng.nextInt(55),
                            asymmetric = asymBlink
                        )
                    } else {
                        val closeMs = (68 + rng.nextInt(30) + (stimAtBlink * 16f)).roundToInt()
                        val holdMs = (22 + rng.nextInt(32) + (stimAtBlink * 14f)).roundToInt().toLong()
                        val openMs = (124 + rng.nextInt(52) + (stimAtBlink * 20f)).roundToInt()
                        runSingleBlink(1f, closeMs, holdMs, openMs, asymBlink)

                        val doubleBlinkProb = 0.44f + 0.16f * stimAtBlink
                        if (rng.nextFloat() < doubleBlinkProb) {
                            val gapMs = 95L + rng.nextInt(130)
                            delay(gapMs)
                            val secondAmp = 0.86f + rng.nextFloat() * 0.14f
                            runSingleBlink(
                                amp = secondAmp,
                                closeMs = 64 + rng.nextInt(22),
                                holdMs = 18L + rng.nextInt(22),
                                openMs = 120 + rng.nextInt(45),
                                asymmetric = asymBlink
                            )
                        }
                    }
                }
            } else if (eyeBlink.value > 0.001f || eyeBlinkR.value > 0.001f) {
                eyeBlink.snapTo(0f)
                eyeBlinkR.snapTo(0f)
            }

            // Slice wait into 120ms steps so newly starting breast/vulva/clitoris touches shorten the wait immediately
            val jitter = rng.nextFloat()
            val cadenceSpread = if (latestBetaLifeDoubleBlinking) 0.72f + rng.nextFloat() * 0.50f else 1.0f
            var elapsedWaitMs = 0L
            while (true) {
                val range = computeBlinkIntervalRangeMs(latestTouchStimulus)
                val baseWaitMs = range.first + ((range.last - range.first) * jitter).toLong()
                val targetWaitMs = (baseWaitMs * cadenceSpread).toLong().coerceAtLeast(850L)
                if (elapsedWaitMs >= targetWaitMs) break
                delay(120L)
                elapsedWaitMs += 120L
            }
        }
    }

    // Lifelike Beta 1: Subtle irregular eye microsaccades + occasional soft gaze shifts
    LaunchedEffect(showExperimentalLegs, betaLifeEyeMicrosaccades) {
        if (!showExperimentalLegs || !betaLifeEyeMicrosaccades) {
            if (!showExperimentalLegs) {
                eyeGazeX.snapTo(0f)
                eyeGazeY.snapTo(0f)
            } else {
                coroutineScope.launch { eyeGazeX.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                coroutineScope.launch { eyeGazeY.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
            }
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis() xor 0x1A2B3C4DL)
        var anchorX = 0f
        var anchorY = 0f
        delay(450L)
        while (true) {
            val faceCouldBeVisible = camZoom.value <= 0.62f || camTargetY.value >= 2.2f
            if (!faceCouldBeVisible) {
                if (abs(eyeGazeX.value) > 0.001f || abs(eyeGazeY.value) > 0.001f) {
                    eyeGazeX.snapTo(0f)
                    eyeGazeY.snapTo(0f)
                }
                delay(360L)
                continue
            }
            val roll = rng.nextFloat()
            val targetX: Float
            val targetY: Float
            val durMs: Int
            val holdMs: Long
            if (roll < 0.66f) {
                // Fine physiological microsaccade dart around current fixation anchor
                targetX = (anchorX + (rng.nextFloat() * 2f - 1f) * 0.24f).coerceIn(-0.78f, 0.78f)
                targetY = (anchorY + (rng.nextFloat() * 2f - 1f) * 0.18f).coerceIn(-0.56f, 0.56f)
                durMs = 55 + rng.nextInt(55)
                holdMs = (380 + rng.nextInt(1280)).toLong()
            } else if (roll < 0.86f) {
                // Gentle unhurried gaze drift toward a new natural fixation point
                anchorX = (rng.nextFloat() * 2f - 1f) * 0.64f
                anchorY = (rng.nextFloat() * 2f - 1f) * 0.44f
                targetX = anchorX
                targetY = anchorY
                durMs = 135 + rng.nextInt(145)
                holdMs = (1150 + rng.nextInt(2400)).toLong()
            } else {
                // Soft return toward calm central eye contact
                anchorX = (rng.nextFloat() * 2f - 1f) * 0.10f
                anchorY = (rng.nextFloat() * 2f - 1f) * 0.08f
                targetX = anchorX
                targetY = anchorY
                durMs = 150 + rng.nextInt(130)
                holdMs = (1300 + rng.nextInt(2600)).toLong()
            }
            val spec = tween<Float>(durationMillis = durMs, easing = FastOutSlowInEasing)
            val jobX = launch { eyeGazeX.animateTo(targetX, spec) }
            val jobY = launch { eyeGazeY.animateTo(targetY, spec) }
            jobX.join()
            jobY.join()
            delay(holdMs)
        }
    }

    // Lifelike Beta 3: Gentle, irregular head repositioning on the pillow (additive to manual drags)
    LaunchedEffect(showExperimentalLegs, betaLifeHeadRepositioning) {
        if (!showExperimentalLegs || !betaLifeHeadRepositioning) {
            if (!showExperimentalLegs) {
                idleHeadTurn.snapTo(0f)
                idleHeadPitch.snapTo(0f)
                idleHeadRoll.snapTo(0f)
            } else {
                val resetSpec = tween<Float>(850, easing = FastOutSlowInEasing)
                coroutineScope.launch { idleHeadTurn.animateTo(0f, resetSpec) }
                coroutineScope.launch { idleHeadPitch.animateTo(0f, resetSpec) }
                coroutineScope.launch { idleHeadRoll.animateTo(0f, resetSpec) }
            }
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis() xor 0x5E6F7A8BL)
        delay(950L)
        while (true) {
            val faceCouldBeVisible = camZoom.value <= 0.62f || camTargetY.value >= 2.2f
            val recentlyDragged = (System.currentTimeMillis() - lastManualHeadDragMs) < 4200L
            if (!faceCouldBeVisible || recentlyDragged) {
                delay(450L)
                continue
            }
            val settleBack = rng.nextFloat() < 0.30f
            val nextTurn = if (settleBack) {
                (rng.nextFloat() * 2f - 1f) * 0.04f
            } else {
                (rng.nextFloat() * 2f - 1f) * 0.22f
            }
            val nextPitch = if (settleBack) {
                rng.nextFloat() * 0.03f
            } else {
                rng.nextFloat() * 0.14f
            }
            val nextRoll = if (settleBack) {
                (rng.nextFloat() * 2f - 1f) * 0.08f
            } else {
                (rng.nextFloat() * 2f - 1f) * 0.42f
            }
            val durMs = 1650 + rng.nextInt(1550)
            // Tab 3 Lifelike+ Gaze Anticipation: eyes saccade toward the new fixation angle ~180–280ms
            // before the head glides to follow, then gently counter-rotate (VOR) as the head arrives.
            val useGazeLead = latestBetaLife2GazeAnticipation
            if (useGazeLead) {
                val deltaTurn = nextTurn - idleHeadTurn.value
                val deltaPitch = nextPitch - idleHeadPitch.value
                val leadX = (deltaTurn * 2.15f).coerceIn(-0.56f, 0.56f)
                val leadY = (-deltaPitch * 1.90f).coerceIn(-0.34f, 0.34f)
                if (abs(leadX) > 0.05f || abs(leadY) > 0.04f) {
                    val saccDur = 95 + rng.nextInt(50)
                    val jLx = launch { anticipationGazeX.animateTo(leadX, tween(saccDur, easing = FastOutSlowInEasing)) }
                    val jLy = launch { anticipationGazeY.animateTo(leadY, tween(saccDur, easing = FastOutSlowInEasing)) }
                    jLx.join()
                    jLy.join()
                    delay((175 + rng.nextInt(115)).toLong())
                }
            }
            val spec = tween<Float>(durationMillis = durMs, easing = FastOutSlowInEasing)
            val jt = launch { idleHeadTurn.animateTo(nextTurn, spec) }
            val jp = launch { idleHeadPitch.animateTo(nextPitch, spec) }
            val jr = launch { idleHeadRoll.animateTo(nextRoll, spec) }
            val jVorX = if (useGazeLead) {
                launch {
                    delay((durMs * 0.12f).roundToInt().toLong())
                    anticipationGazeX.animateTo(0f, tween((durMs * 0.72f).roundToInt().coerceAtLeast(320), easing = FastOutSlowInEasing))
                }
            } else null
            val jVorY = if (useGazeLead) {
                launch {
                    delay((durMs * 0.12f).roundToInt().toLong())
                    anticipationGazeY.animateTo(0f, tween((durMs * 0.72f).roundToInt().coerceAtLeast(320), easing = FastOutSlowInEasing))
                }
            } else null
            jt.join()
            jp.join()
            jr.join()
            jVorX?.join()
            jVorY?.join()

            val restMs = (3800 + rng.nextInt(6800)).toLong()
            delay(restMs)
        }
    }

    // Ensure Gaze Anticipation resets cleanly to 0f immediately when toggled OFF
    LaunchedEffect(showExperimentalLegs, betaLife2GazeAnticipation) {
        if (!showExperimentalLegs || !betaLife2GazeAnticipation) {
            if (!showExperimentalLegs) {
                anticipationGazeX.snapTo(0f)
                anticipationGazeY.snapTo(0f)
            } else {
                coroutineScope.launch { anticipationGazeX.animateTo(0f, tween(240, easing = FastOutSlowInEasing)) }
                coroutineScope.launch { anticipationGazeY.animateTo(0f, tween(240, easing = FastOutSlowInEasing)) }
            }
        }
    }

    // Tab 3 Lifelike+: Subtle occasional swallowing (delicate anterior throat & submental ease every ~14–28s)
    LaunchedEffect(showExperimentalLegs, betaLife2SubtleSwallowing) {
        if (!showExperimentalLegs || !betaLife2SubtleSwallowing) {
            swallowPhase.snapTo(0f)
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis() xor 0x2B4C6D8EL)
        // Initial brief beat so user can observe a calm swallow soon after enabling, then irregular natural spacing
        delay((2200 + rng.nextInt(2000)).toLong())
        while (true) {
            val neckCouldBeVisible = camZoom.value <= 0.72f || camTargetY.value >= 2.0f
            if (neckCouldBeVisible) {
                val durMs = 1100 + rng.nextInt(280)
                swallowPhase.snapTo(0.001f)
                swallowPhase.animateTo(1f, tween(durationMillis = durMs, easing = FastOutSlowInEasing))
                swallowPhase.snapTo(0f)
            }
            delay((13500 + rng.nextInt(14500)).toLong())
        }
    }

    // Tab 3 Lifelike+: Postural settling (small, coordinated cushion, shoulder & head micro-settle after larger movements)
    LaunchedEffect(showExperimentalLegs, betaLife2PosturalSettling) {
        if (!showExperimentalLegs || !betaLife2PosturalSettling) {
            if (!showExperimentalLegs) {
                settleHeadTurn.snapTo(0f)
                settleHeadPitch.snapTo(0f)
                settleHeadRoll.snapTo(0f)
                settleShoulderL.snapTo(0f)
                settleShoulderR.snapTo(0f)
                settleLegL.snapTo(0f)
                settleLegR.snapTo(0f)
            } else {
                val rSpec = tween<Float>(620, easing = FastOutSlowInEasing)
                coroutineScope.launch { settleHeadTurn.animateTo(0f, rSpec) }
                coroutineScope.launch { settleHeadPitch.animateTo(0f, rSpec) }
                coroutineScope.launch { settleHeadRoll.animateTo(0f, rSpec) }
                coroutineScope.launch { settleShoulderL.animateTo(0f, rSpec) }
                coroutineScope.launch { settleShoulderR.animateTo(0f, rSpec) }
                coroutineScope.launch { settleLegL.animateTo(0f, rSpec) }
                coroutineScope.launch { settleLegR.animateTo(0f, rSpec) }
            }
        }
    }
    LaunchedEffect(showExperimentalLegs, betaLife2PosturalSettling, posturalSettleTrigger) {
        if (!showExperimentalLegs || !betaLife2PosturalSettling || posturalSettleTrigger <= 0) return@LaunchedEffect
        val rng = kotlin.random.Random(System.currentTimeMillis() xor posturalSettleTrigger.toLong())
        delay((340 + rng.nextInt(320)).toLong())
        if (activePointerCount > 0) return@LaunchedEffect

        val durMs = 950 + rng.nextInt(650)
        val settleSpec = tween<Float>(durationMillis = durMs, easing = FastOutSlowInEasing)
        val targetShL = 0.12f + rng.nextFloat() * 0.14f
        val targetShR = 0.10f + rng.nextFloat() * 0.14f
        val targetHT = (rng.nextFloat() * 2f - 1f) * 0.036f
        val targetHP = rng.nextFloat() * 0.028f
        val targetHR = (rng.nextFloat() * 2f - 1f) * 0.028f
        val targetLegL = if (leftLegSpread.value > 0.08f) (rng.nextFloat() * 2f - 1f) * 0.022f else 0f
        val targetLegR = if (rightLegSpread.value > 0.08f) (rng.nextFloat() * 2f - 1f) * 0.022f else 0f

        val j1 = launch { settleShoulderL.animateTo(targetShL, settleSpec) }
        val j2 = launch {
            delay((70 + rng.nextInt(140)).toLong())
            settleShoulderR.animateTo(targetShR, settleSpec)
        }
        val j3 = launch { settleHeadTurn.animateTo(targetHT, settleSpec) }
        val j4 = launch { settleHeadPitch.animateTo(targetHP, settleSpec) }
        val j5 = launch { settleHeadRoll.animateTo(targetHR, settleSpec) }
        val j6 = launch { settleLegL.animateTo(targetLegL, settleSpec) }
        val j7 = launch { settleLegR.animateTo(targetLegR, settleSpec) }
        j1.join(); j2.join(); j3.join(); j4.join(); j5.join(); j6.join(); j7.join()

        delay((900 + rng.nextInt(900)).toLong())
        if (activePointerCount > 0) return@LaunchedEffect
        val easeSpec = tween<Float>(durationMillis = 1400 + rng.nextInt(700), easing = FastOutSlowInEasing)
        launch { settleShoulderL.animateTo(0f, easeSpec) }
        launch { settleShoulderR.animateTo(0f, easeSpec) }
        launch { settleHeadTurn.animateTo(targetHT * 0.35f, easeSpec) }
        launch { settleHeadPitch.animateTo(0f, easeSpec) }
        launch { settleHeadRoll.animateTo(targetHR * 0.30f, easeSpec) }
        launch { settleLegL.animateTo(targetLegL * 0.45f, easeSpec) }
        launch { settleLegR.animateTo(targetLegR * 0.45f, easeSpec) }
    }

    // Tab 3 Lifelike+: Breath-linked nostril & lip softness (whisper-soft alar flare & relaxed exhale lip ease)
    LaunchedEffect(showExperimentalLegs, betaLife2BreathFaceCoupling) {
        if (!showExperimentalLegs || !betaLife2BreathFaceCoupling) {
            if (!showExperimentalLegs) {
                nostrilBreath.snapTo(0f)
                breathLipEase.snapTo(0f)
            } else {
                val rSpec = tween<Float>(450, easing = FastOutSlowInEasing)
                coroutineScope.launch { nostrilBreath.animateTo(0f, rSpec) }
                coroutineScope.launch { breathLipEase.animateTo(0f, rSpec) }
            }
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis() xor 0x6A4F2C19L)
        while (true) {
            val faceCouldBeVisible = camZoom.value <= 0.62f || camTargetY.value >= 2.2f
            if (!faceCouldBeVisible) {
                delay(420L)
                continue
            }
            val stim = max(
                latestTouchStimulus.coerceIn(0f, 1f),
                VulvaGlTextureView.heartRateExertionNorm(latestHeartRateBpm)
            )
            val deepBeat = rng.nextFloat() < (0.24f + stim * 0.18f)
            val inhaleMs = ((if (deepBeat) 1950 else 1480) + rng.nextInt(380) - (stim * 620f).roundToInt()).coerceAtLeast(620)
            val exhaleMs = ((if (deepBeat) 2450 else 1920) + rng.nextInt(460) - (stim * 740f).roundToInt()).coerceAtLeast(720)
            val peakNostril = ((if (deepBeat) 0.84f else 0.48f) + stim * 0.34f + rng.nextFloat() * 0.12f).coerceIn(0.32f, 1.0f)
            val peakLip = ((if (deepBeat) 0.24f else 0.11f) + stim * 0.26f).coerceIn(0f, 0.52f)

            val jInN = launch { nostrilBreath.animateTo(peakNostril, tween(inhaleMs, easing = FastOutSlowInEasing)) }
            val jInL = launch { breathLipEase.animateTo(peakLip * 0.35f, tween(inhaleMs, easing = FastOutSlowInEasing)) }
            jInN.join(); jInL.join()

            val jExN = launch { nostrilBreath.animateTo(0.06f, tween(exhaleMs, easing = FastOutSlowInEasing)) }
            val jExL = launch {
                breathLipEase.animateTo(peakLip, tween((exhaleMs * 0.42f).roundToInt(), easing = FastOutSlowInEasing))
                breathLipEase.animateTo(0f, tween((exhaleMs * 0.58f).roundToInt(), easing = FastOutSlowInEasing))
            }
            jExN.join(); jExL.join()
            delay(((280 + rng.nextInt(420)) * (1f - 0.65f * stim)).roundToInt().coerceAtLeast(80).toLong())
        }
    }

    // Tab 3 Lifelike+: Subtle capillary warmth across cheeks, ears & upper chest (calm organic ebb + heart-rate response)
    LaunchedEffect(showExperimentalLegs, betaLife2LivingWarmth) {
        if (!showExperimentalLegs || !betaLife2LivingWarmth) {
            if (!showExperimentalLegs) {
                livingWarmth.snapTo(0f)
            } else {
                livingWarmth.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
            }
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis() xor 0x4D2A8F1EL)
        while (true) {
            val stim = max(
                latestTouchStimulus.coerceIn(0f, 1f),
                VulvaGlTextureView.heartRateExertionNorm(latestHeartRateBpm)
            )
            val targetWarmth = (0.24f + rng.nextFloat() * 0.14f + stim * 0.56f).coerceIn(0.20f, 0.96f)
            val durMs = if (stim > 0.2f) 1100 + rng.nextInt(500) else 2400 + rng.nextInt(1800)
            livingWarmth.animateTo(targetWarmth, tween(durMs, easing = FastOutSlowInEasing))
            delay((600 + rng.nextInt(900)).toLong())
        }
    }

    // Lifelike Beta 5: Subtle, irregular asymmetric shoulder relaxation / release
    LaunchedEffect(showExperimentalLegs, betaLifeShoulderRelaxation) {
        if (!showExperimentalLegs || !betaLifeShoulderRelaxation) {
            if (!showExperimentalLegs) {
                shoulderRelaxL.snapTo(0f)
                shoulderRelaxR.snapTo(0f)
            } else {
                val resetSpec = tween<Float>(750, easing = FastOutSlowInEasing)
                coroutineScope.launch { shoulderRelaxL.animateTo(0f, resetSpec) }
                coroutineScope.launch { shoulderRelaxR.animateTo(0f, resetSpec) }
            }
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis() xor 0x3C4D5E6FL)
        delay(800L)
        while (true) {
            val shouldersCouldBeVisible = camZoom.value <= 0.72f || camTargetY.value >= 1.6f
            if (!shouldersCouldBeVisible) {
                delay(420L)
                continue
            }
            // Occasional subtle pre-release breath lift before letting the shoulders melt downward
            if (rng.nextFloat() < 0.38f) {
                val liftL = -(0.06f + rng.nextFloat() * 0.12f)
                val liftR = -(0.06f + rng.nextFloat() * 0.12f)
                val liftDur = 750 + rng.nextInt(550)
                val jl = launch { shoulderRelaxL.animateTo(liftL, tween(liftDur, easing = FastOutSlowInEasing)) }
                val jr = launch { shoulderRelaxR.animateTo(liftR, tween(liftDur + rng.nextInt(180), easing = FastOutSlowInEasing)) }
                jl.join()
                jr.join()
                delay((180 + rng.nextInt(260)).toLong())
            }
            // Irregular asymmetric downward drop & cushion settle
            val baseDrop = 0.42f + rng.nextFloat() * 0.48f
            val targetL = (baseDrop + (rng.nextFloat() * 2f - 1f) * 0.14f).coerceIn(0.24f, 0.96f)
            val targetR = (baseDrop + (rng.nextFloat() * 2f - 1f) * 0.14f).coerceIn(0.24f, 0.96f)
            val dropDur = 1350 + rng.nextInt(1150)
            val staggerMs = (90 + rng.nextInt(240)).toLong()
            val leftFirst = rng.nextBoolean()
            val jA = launch {
                if (!leftFirst) delay(staggerMs)
                shoulderRelaxL.animateTo(targetL, tween(dropDur, easing = FastOutSlowInEasing))
            }
            val jB = launch {
                if (leftFirst) delay(staggerMs)
                shoulderRelaxR.animateTo(targetR, tween(dropDur + rng.nextInt(220), easing = FastOutSlowInEasing))
            }
            jA.join()
            jB.join()
            delay((2200 + rng.nextInt(4200)).toLong())

            // Gently drift back toward a varied calm baseline tension (not rigidly 0)
            val settleBase = 0.04f + rng.nextFloat() * 0.24f
            val settleDur = 1600 + rng.nextInt(1300)
            val jS1 = launch { shoulderRelaxL.animateTo((settleBase + rng.nextFloat() * 0.08f).coerceAtMost(0.34f), tween(settleDur, easing = FastOutSlowInEasing)) }
            val jS2 = launch { shoulderRelaxR.animateTo((settleBase + rng.nextFloat() * 0.08f).coerceAtMost(0.34f), tween(settleDur + rng.nextInt(240), easing = FastOutSlowInEasing)) }
            jS1.join()
            jS2.join()
            delay((2600 + rng.nextInt(5400)).toLong())
        }
    }

    // Lifelike Beta 6: Subtle, irregular serene facial expression micro-shifts
    LaunchedEffect(showExperimentalLegs, betaLifeFacialExpressions) {
        if (!showExperimentalLegs || !betaLifeFacialExpressions) {
            if (!showExperimentalLegs) {
                faceWarmth.snapTo(0f)
                faceBrow.snapTo(0f)
                faceLipSoft.snapTo(0f)
            } else {
                val resetSpec = tween<Float>(680, easing = FastOutSlowInEasing)
                coroutineScope.launch { faceWarmth.animateTo(0f, resetSpec) }
                coroutineScope.launch { faceBrow.animateTo(0f, resetSpec) }
                coroutineScope.launch { faceLipSoft.animateTo(0f, resetSpec) }
            }
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.currentTimeMillis() xor 0x7A8B9C0DL)
        delay(650L)
        while (true) {
            val faceCouldBeVisible = camZoom.value <= 0.62f || camTargetY.value >= 2.2f
            if (!faceCouldBeVisible) {
                delay(420L)
                continue
            }
            val stim = latestTouchStimulus.coerceIn(0f, 1f)
            val mood = rng.nextFloat()
            val tWarmth: Float
            val tBrow: Float
            val tLip: Float
            when {
                mood < 0.34f -> {
                    // Quiet, soft micro-smile warmth
                    tWarmth = (0.34f + rng.nextFloat() * 0.38f + stim * 0.14f).coerceIn(0f, 0.90f)
                    tBrow = (rng.nextFloat() * 0.22f - 0.04f).coerceIn(-0.25f, 0.30f)
                    tLip = (0.08f + rng.nextFloat() * 0.24f).coerceIn(0f, 0.55f)
                }
                mood < 0.58f -> {
                    // Gentle, soft brow curiosity / quiet attentiveness
                    tWarmth = (0.08f + rng.nextFloat() * 0.22f).coerceIn(0f, 0.45f)
                    tBrow = (0.28f + rng.nextFloat() * 0.38f).coerceIn(0f, 0.72f)
                    tLip = (0.04f + rng.nextFloat() * 0.16f).coerceIn(0f, 0.35f)
                }
                mood < 0.80f -> {
                    // Deep calm relaxed exhale ease
                    tWarmth = (0.14f + rng.nextFloat() * 0.24f).coerceIn(0f, 0.50f)
                    tBrow = (-0.22f + rng.nextFloat() * 0.26f).coerceIn(-0.30f, 0.12f)
                    tLip = (0.32f + rng.nextFloat() * 0.42f + stim * 0.16f).coerceIn(0f, 0.88f)
                }
                else -> {
                    // Quiet near-neutral serene rest
                    tWarmth = rng.nextFloat() * 0.12f
                    tBrow = (rng.nextFloat() * 2f - 1f) * 0.08f
                    tLip = rng.nextFloat() * 0.10f
                }
            }
            val durMs = 1150 + rng.nextInt(1350)
            val jW = launch { faceWarmth.animateTo(tWarmth, tween(durMs, easing = FastOutSlowInEasing)) }
            val jB = launch { faceBrow.animateTo(tBrow, tween(durMs + rng.nextInt(260), easing = FastOutSlowInEasing)) }
            val jL = launch { faceLipSoft.animateTo(tLip, tween(durMs + rng.nextInt(220), easing = FastOutSlowInEasing)) }
            jW.join()
            jB.join()
            jL.join()

            delay((2800 + rng.nextInt(5200)).toLong())
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
        leftLegSpread = (leftLegSpread.value + settleLegL.value).coerceIn(-0.30f, 1f),
        rightLegSpread = (rightLegSpread.value + settleLegR.value).coerceIn(-0.30f, 1f),
        selfSpreadPose = selfSpreadPose.value,
        headTurn = (headTurn.value + idleHeadTurn.value + settleHeadTurn.value).coerceIn(-1f, 1f),
        headPitch = (headPitch.value + idleHeadPitch.value + settleHeadPitch.value).coerceIn(0f, 1f),
        eyeBlink = eyeBlink.value,
        leftGluteX = leftGluteX.value,
        leftGluteY = leftGluteY.value,
        rightGluteX = rightGluteX.value,
        rightGluteY = rightGluteY.value,
        legacyVulvaMode = legacyVulvaMode,
        betaV3OrganicContinuity = betaV3OrganicContinuity,
        betaV3ContinuityStrength = betaV3ContinuityStrength,
        betaV3SoftPigmentation = betaV3SoftPigmentation,
        betaV3PigmentStrength = betaV3PigmentStrength,
        betaV3AnatomicalAsymmetry = betaV3AnatomicalAsymmetry,
        betaV3AsymmetryStrength = betaV3AsymmetryStrength,
        betaV3RegionalSurfaceDetail = betaV3RegionalSurfaceDetail,
        betaV3SurfaceStrength = betaV3SurfaceStrength,
        betaV3NaturalLighting = betaV3NaturalLighting,
        betaV3LightingStrength = betaV3LightingStrength,
        betaBreastSizeBoost = betaBreastSizeBoost,
        eyeGazeX = (eyeGazeX.value + anticipationGazeX.value).coerceIn(-1f, 1f),
        eyeGazeY = (eyeGazeY.value + anticipationGazeY.value).coerceIn(-1f, 1f),
        headRollOffset = idleHeadRoll.value + settleHeadRoll.value,
        faceWarmth = faceWarmth.value,
        faceBrow = faceBrow.value,
        faceLipSoft = (faceLipSoft.value + breathLipEase.value).coerceIn(0f, 1f),
        betaLifeDeepBreathsSighs = betaLifeDeepBreathsSighs,
        shoulderRelaxL = (shoulderRelaxL.value + settleShoulderL.value).coerceIn(-0.25f, 1.0f),
        shoulderRelaxR = (shoulderRelaxR.value + settleShoulderR.value).coerceIn(-0.25f, 1.0f),
        eyeBlinkRightOffset = if (betaLife2AsymmetricBlinking) (eyeBlinkR.value - eyeBlink.value) else 0f,
        eyelidGazeTrack = if (betaLife2EyelidTracking) 1f else 0f,
        swallowPhase = swallowPhase.value,
        nostrilBreath = nostrilBreath.value,
        livingWarmth = livingWarmth.value
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
                        glView.heartRateBpm = heartRateBpm
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
                                var touchedSettleableZone = false

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
                                        if (!latestOrbitMode && touchedSettleableZone && latestBetaLife2PosturalSettling) {
                                            posturalSettleTrigger++
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
                                            betaBreastSizeBoost = betaBreastSizeBoost,
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
                                                    touchedSettleableZone = true
                                                }
                                                if (zone == TouchTargetZone.LEFT_LEG ||
                                                    zone == TouchTargetZone.RIGHT_LEG ||
                                                    zone == TouchTargetZone.HEAD
                                                ) {
                                                    touchedSettleableZone = true
                                                }

                                                when (zone) {
                                                    TouchTargetZone.NONE -> {
                                                        // Cleanly ignore touches on arms, hands, torso, or background
                                                    }
                                                    TouchTargetZone.LEFT_BREAST,
                                                    TouchTargetZone.RIGHT_BREAST -> {
                                                        registerPhysiologicalStimulus(0.54f, isDragMotion = false)
                                                        if (latestHapticsEnabled) {
                                                            hapticsEngine.triggerTap(SensitivityTier.HIGH)
                                                        }
                                                    }
                                                    TouchTargetZone.LEFT_GLUTE,
                                                    TouchTargetZone.RIGHT_GLUTE -> {
                                                        registerPhysiologicalStimulus(0.26f, isDragMotion = false)
                                                        if (latestHapticsEnabled) {
                                                            hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                                        }
                                                    }
                                                    TouchTargetZone.LEFT_LEG,
                                                    TouchTargetZone.RIGHT_LEG,
                                                    TouchTargetZone.HEAD -> {
                                                        registerPhysiologicalStimulus(0.18f, isDragMotion = false)
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
                                                        val vulvaSens = when {
                                                            hit?.id == "ext_glans" -> 1.00f
                                                            hit?.id == "ext_hood" || hit?.sensitivityTier == SensitivityTier.ULTRA -> 0.86f
                                                            hit?.sensitivityTier == SensitivityTier.HIGH -> 0.68f
                                                            else -> 0.44f
                                                        }
                                                        registerPhysiologicalStimulus(vulvaSens, isDragMotion = false)
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
                                                            registerPhysiologicalStimulus(0.58f, isDragMotion = true)
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
                                                            registerPhysiologicalStimulus(0.58f, isDragMotion = true)
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
                                                        if (abs(rawDx) > 0.003f) {
                                                            registerPhysiologicalStimulus(0.20f, isDragMotion = true)
                                                        }
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
                                                        if (abs(rawDx) > 0.003f) {
                                                            registerPhysiologicalStimulus(0.20f, isDragMotion = true)
                                                        }
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
                                                        if (hypot(rawDx, rawDy) > 0.003f) {
                                                            registerPhysiologicalStimulus(0.28f, isDragMotion = true)
                                                        }
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
                                                        if (hypot(rawDx, rawDy) > 0.003f) {
                                                            registerPhysiologicalStimulus(0.28f, isDragMotion = true)
                                                        }
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
                                                            val dragSens = when {
                                                                dragHit?.id == "ext_glans" -> 1.00f
                                                                inHoodZone || dragHit?.id == "ext_hood" || dragHit?.sensitivityTier == SensitivityTier.ULTRA -> 0.88f
                                                                dragHit?.sensitivityTier == SensitivityTier.HIGH -> 0.70f
                                                                else -> 0.46f
                                                            }
                                                            registerPhysiologicalStimulus(dragSens, isDragMotion = true)
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
                                            lastManualHeadDragMs = System.currentTimeMillis()
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

                // Optional Live Heart Rate (BPM) HUD Pill anchored top-left over the 3D viewport
                androidx.compose.animation.AnimatedVisibility(
                    visible = showExperimentalLegs && betaLife2ShowHeartRate,
                    enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.92f),
                    exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    val bpmInt = heartRateBpm.roundToInt()
                    val hrNorm = VulvaGlTextureView.heartRateExertionNorm(heartRateBpm)
                    val heartTint = Color(
                        red = 1.0f,
                        green = (0.48f - 0.18f * hrNorm).coerceIn(0.24f, 0.55f),
                        blue = (0.56f - 0.24f * hrNorm).coerceIn(0.26f, 0.60f),
                        alpha = 1.0f
                    )
                    Surface(
                        modifier = Modifier
                            .testTag("heart_rate_hud_pill")
                            .semantics {
                                contentDescription = "Heart rate $bpmInt beats per minute"
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1D1416).copy(alpha = 0.80f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = heartTint.copy(alpha = 0.32f + 0.38f * hrNorm)
                        ),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = heartTint,
                                modifier = Modifier
                                    .size(15.dp)
                                    .scale(heartPulseScale.value)
                            )
                            Text(
                                text = "$bpmInt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "BPM",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                }

                // Always-on-top Floating Hamburger Beta Menu (Vulva + Life + Life+)
                val activeVulvaBetaCount = listOf(
                    betaV3OrganicContinuity,
                    betaV3SoftPigmentation,
                    betaV3AnatomicalAsymmetry,
                    betaV3RegionalSurfaceDetail,
                    betaV3NaturalLighting,
                    betaBreastSizeBoost
                ).count { it }
                val activeLifelikeBetaCount = listOf(
                    betaLifeEyeMicrosaccades,
                    betaLifeDoubleBlinking,
                    betaLifeHeadRepositioning,
                    betaLifeDeepBreathsSighs,
                    betaLifeShoulderRelaxation,
                    betaLifeFacialExpressions
                ).count { it }
                val activeLifelike2BetaCount = listOf(
                    betaLife2EyelidTracking,
                    betaLife2AsymmetricBlinking,
                    betaLife2GazeAnticipation,
                    betaLife2SubtleSwallowing,
                    betaLife2PosturalSettling,
                    betaLife2BreathFaceCoupling,
                    betaLife2LivingWarmth,
                    betaLife2ShowHeartRate
                ).count { it }
                val totalActiveBetaCount = activeVulvaBetaCount + activeLifelikeBetaCount + activeLifelike2BetaCount

                // Floating Hamburger Button anchored top-right over the 3D viewport (48dp touch target)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D1416).copy(alpha = if (showBetaMenu) 0.92f else 0.78f))
                        .border(
                            width = 1.dp,
                            color = if (totalActiveBetaCount > 0) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            } else {
                                Color.White.copy(alpha = 0.22f)
                            },
                            shape = CircleShape
                        )
                        .clickable {
                            showBetaMenu = !showBetaMenu
                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                        }
                        .testTag("beta_hamburger_button")
                        .semantics {
                            contentDescription = if (showBetaMenu) {
                                "Close Beta menu overlay"
                            } else if (totalActiveBetaCount > 0) {
                                "Open Beta menu overlay, $totalActiveBetaCount beta features active"
                            } else {
                                "Open Beta menu overlay"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showBetaMenu) Icons.Filled.Close else Icons.Filled.Menu,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    if (totalActiveBetaCount > 0 && !showBetaMenu) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 5.dp, end = 5.dp)
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = totalActiveBetaCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Translucent Compact Floating 3-Tab Beta Panel (touches outside still orbit/touch Blossom live)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBetaMenu,
                    enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.95f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.96f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 8.dp, start = 12.dp, bottom = 10.dp)
                ) {
                    val panelScroll = rememberScrollState()
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 316.dp)
                            .heightIn(max = 332.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                            .testTag("beta_floating_panel"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.89f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f)
                        ),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 8.dp)
                        ) {
                            // Header + quick Reset All
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Beta Lab",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (totalActiveBetaCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                betaV3OrganicContinuity = false
                                                betaV3ContinuityStrength = 0.65f
                                                betaV3SoftPigmentation = false
                                                betaV3PigmentStrength = 0.70f
                                                betaV3AnatomicalAsymmetry = false
                                                betaV3AsymmetryStrength = 0.65f
                                                betaV3RegionalSurfaceDetail = false
                                                betaV3SurfaceStrength = 0.65f
                                                betaV3NaturalLighting = false
                                                betaV3LightingStrength = 0.70f
                                                betaBreastSizeBoost = false
                                                betaLifeEyeMicrosaccades = false
                                                betaLifeDoubleBlinking = false
                                                betaLifeHeadRepositioning = false
                                                betaLifeDeepBreathsSighs = false
                                                betaLifeShoulderRelaxation = false
                                                betaLifeFacialExpressions = false
                                                betaLife2EyelidTracking = false
                                                betaLife2AsymmetricBlinking = false
                                                betaLife2GazeAnticipation = false
                                                betaLife2SubtleSwallowing = false
                                                betaLife2PosturalSettling = false
                                                betaLife2BreathFaceCoupling = false
                                                betaLife2LivingWarmth = false
                                                betaLife2ShowHeartRate = false
                                                if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                            }
                                            .testTag("beta_reset_all_button")
                                            .semantics {
                                                contentDescription = "Turn off all active beta toggles"
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f))
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Reset all",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // 3-Tab Selector: Vulva | Life | Life+
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BetaMenuTabPill(
                                    label = if (activeVulvaBetaCount > 0) "Vulva ($activeVulvaBetaCount)" else "Vulva",
                                    selected = betaMenuTab == 0,
                                    tag = "beta_tab_vulva",
                                    onClick = { betaMenuTab = 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                BetaMenuTabPill(
                                    label = if (activeLifelikeBetaCount > 0) "Life ($activeLifelikeBetaCount)" else "Life",
                                    selected = betaMenuTab == 1,
                                    tag = "beta_tab_lifelike",
                                    onClick = { betaMenuTab = 1 },
                                    modifier = Modifier.weight(1f)
                                )
                                BetaMenuTabPill(
                                    label = if (activeLifelike2BetaCount > 0) "Life+ ($activeLifelike2BetaCount)" else "Life+",
                                    selected = betaMenuTab == 2,
                                    tag = "beta_tab_lifelike_plus",
                                    onClick = { betaMenuTab = 2 },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(panelScroll)
                            ) {
                                if (betaMenuTab == 0) {
                                    Text(
                                        text = "Anatomical realism pass v3 & proportion options (OFF by default)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "1. Organic tissue continuity",
                                        subtitle = "Softens labial cushion slopes, commissure bridges & sulcus transitions",
                                        checked = betaV3OrganicContinuity,
                                        tag = "toggle_beta_v3_organic_continuity",
                                        compact = true,
                                        onToggle = {
                                            betaV3OrganicContinuity = !betaV3OrganicContinuity
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3OrganicContinuity) {
                                        BetaStrengthSliderRow(
                                            label = "Continuity strength",
                                            value = betaV3ContinuityStrength,
                                            tag = "slider_beta_v3_continuity_strength",
                                            onValueChange = { betaV3ContinuityStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "2. Soft spatial pigmentation",
                                        subtitle = "Organically varies mucocutaneous edges & subtle vascular/melanin tones",
                                        checked = betaV3SoftPigmentation,
                                        tag = "toggle_beta_v3_soft_pigmentation",
                                        compact = true,
                                        onToggle = {
                                            betaV3SoftPigmentation = !betaV3SoftPigmentation
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3SoftPigmentation) {
                                        BetaStrengthSliderRow(
                                            label = "Pigmentation blend",
                                            value = betaV3PigmentStrength,
                                            tag = "slider_beta_v3_pigment_strength",
                                            onValueChange = { betaV3PigmentStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "3. Subtle anatomical asymmetry",
                                        subtitle = "Natural bilateral fullness, lobe contour & fold variation",
                                        checked = betaV3AnatomicalAsymmetry,
                                        tag = "toggle_beta_v3_anatomical_asymmetry",
                                        compact = true,
                                        onToggle = {
                                            betaV3AnatomicalAsymmetry = !betaV3AnatomicalAsymmetry
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3AnatomicalAsymmetry) {
                                        BetaStrengthSliderRow(
                                            label = "Asymmetry strength",
                                            value = betaV3AsymmetryStrength,
                                            tag = "slider_beta_v3_asymmetry_strength",
                                            onValueChange = { betaV3AsymmetryStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "4. Regional curvature & material",
                                        subtitle = "Differentiates outer skin grain, transitional satin, moist mucosa & glans",
                                        checked = betaV3RegionalSurfaceDetail,
                                        tag = "toggle_beta_v3_regional_surface",
                                        compact = true,
                                        onToggle = {
                                            betaV3RegionalSurfaceDetail = !betaV3RegionalSurfaceDetail
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3RegionalSurfaceDetail) {
                                        BetaStrengthSliderRow(
                                            label = "Surface detail strength",
                                            value = betaV3SurfaceStrength,
                                            tag = "slider_beta_v3_surface_strength",
                                            onValueChange = { betaV3SurfaceStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "5. Natural lighting & shadows",
                                        subtitle = "Softens pelvic shadow penumbras with warm cavity bounce while keeping landmarks crisp",
                                        checked = betaV3NaturalLighting,
                                        tag = "toggle_beta_v3_natural_lighting",
                                        compact = true,
                                        onToggle = {
                                            betaV3NaturalLighting = !betaV3NaturalLighting
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3NaturalLighting) {
                                        BetaStrengthSliderRow(
                                            label = "Lighting softness",
                                            value = betaV3LightingStrength,
                                            tag = "slider_beta_v3_lighting_strength",
                                            onValueChange = { betaV3LightingStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Breast size boost (+15%)",
                                        subtitle = "Increases breast size ~15% across slider settings except the most petite",
                                        checked = betaBreastSizeBoost,
                                        tag = "toggle_beta_breast_size_boost",
                                        compact = true,
                                        onToggle = {
                                            betaBreastSizeBoost = !betaBreastSizeBoost
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                } else if (betaMenuTab == 1) {
                                    Text(
                                        text = "Subtle, irregular micro-behaviors (promoted ON by default)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Eye microsaccades",
                                        subtitle = "Tiny irregular eye movements and occasional soft gaze shifts",
                                        checked = betaLifeEyeMicrosaccades,
                                        tag = "toggle_beta_life_eye_microsaccades",
                                        compact = true,
                                        onToggle = {
                                            betaLifeEyeMicrosaccades = !betaLifeEyeMicrosaccades
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Occasional double blinking",
                                        subtitle = "Irregular blink rhythm with asymmetric double-blinks and soft flutters",
                                        checked = betaLifeDoubleBlinking,
                                        tag = "toggle_beta_life_double_blinking",
                                        compact = true,
                                        onToggle = {
                                            betaLifeDoubleBlinking = !betaLifeDoubleBlinking
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Gentle head repositioning",
                                        subtitle = "Infrequent, unhurried pillow head shifts and subtle tilt",
                                        checked = betaLifeHeadRepositioning,
                                        tag = "toggle_beta_life_head_repositioning",
                                        compact = true,
                                        onToggle = {
                                            betaLifeHeadRepositioning = !betaLifeHeadRepositioning
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Deeper breaths & sighs",
                                        subtitle = "Irregular breath variation with occasional slower deep inhales and sighs",
                                        checked = betaLifeDeepBreathsSighs,
                                        tag = "toggle_beta_life_deep_breaths_sighs",
                                        compact = true,
                                        onToggle = {
                                            betaLifeDeepBreathsSighs = !betaLifeDeepBreathsSighs
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Subtle shoulder relaxation",
                                        subtitle = "Occasional gentle, slightly asymmetric shoulder drop and softening",
                                        checked = betaLifeShoulderRelaxation,
                                        tag = "toggle_beta_life_shoulder_relaxation",
                                        compact = true,
                                        onToggle = {
                                            betaLifeShoulderRelaxation = !betaLifeShoulderRelaxation
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Subtle facial expressions",
                                        subtitle = "Quiet micro-smile warmth, gentle brow softening, and relaxed lip ease",
                                        checked = betaLifeFacialExpressions,
                                        tag = "toggle_beta_life_facial_expressions",
                                        compact = true,
                                        onToggle = {
                                            betaLifeFacialExpressions = !betaLifeFacialExpressions
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                } else {
                                    Text(
                                        text = "Next-gen subtle realism tests (all OFF by default)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Eyelid tracking",
                                        subtitle = "Subtle upper eyelid follow accompanying vertical gaze shifts",
                                        checked = betaLife2EyelidTracking,
                                        tag = "toggle_beta_life2_eyelid_tracking",
                                        compact = true,
                                        onToggle = {
                                            betaLife2EyelidTracking = !betaLife2EyelidTracking
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Asymmetric blinking",
                                        subtitle = "Slight natural timing difference (16–38ms) between left and right eyelids",
                                        checked = betaLife2AsymmetricBlinking,
                                        tag = "toggle_beta_life2_asymmetric_blinking",
                                        compact = true,
                                        onToggle = {
                                            betaLife2AsymmetricBlinking = !betaLife2AsymmetricBlinking
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Gaze anticipation",
                                        subtitle = "Eyes glance toward a new fixation point shortly before the head follows",
                                        checked = betaLife2GazeAnticipation,
                                        tag = "toggle_beta_life2_gaze_anticipation",
                                        compact = true,
                                        onToggle = {
                                            betaLife2GazeAnticipation = !betaLife2GazeAnticipation
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Subtle swallowing",
                                        subtitle = "Occasional gentle throat and submental jaw swallow wave",
                                        checked = betaLife2SubtleSwallowing,
                                        tag = "toggle_beta_life2_subtle_swallowing",
                                        compact = true,
                                        onToggle = {
                                            betaLife2SubtleSwallowing = !betaLife2SubtleSwallowing
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Postural settling",
                                        subtitle = "Small, coordinated cushion, shoulder & head ease after larger movements",
                                        checked = betaLife2PosturalSettling,
                                        tag = "toggle_beta_life2_postural_settling",
                                        compact = true,
                                        onToggle = {
                                            betaLife2PosturalSettling = !betaLife2PosturalSettling
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Breath-linked face softness",
                                        subtitle = "Whisper-soft nostril flare and relaxed lip ease synced to breathing",
                                        checked = betaLife2BreathFaceCoupling,
                                        tag = "toggle_beta_life2_breath_face_coupling",
                                        compact = true,
                                        onToggle = {
                                            betaLife2BreathFaceCoupling = !betaLife2BreathFaceCoupling
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Subtle capillary warmth",
                                        subtitle = "Natural living rosy warmth across cheeks & upper chest responding gently to touch",
                                        checked = betaLife2LivingWarmth,
                                        tag = "toggle_beta_life2_living_warmth",
                                        compact = true,
                                        onToggle = {
                                            betaLife2LivingWarmth = !betaLife2LivingWarmth
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Show heart rate (BPM)",
                                        subtitle = "Displays Blossom's live stimulation-linked heart rate in the top-left corner",
                                        checked = betaLife2ShowHeartRate,
                                        tag = "toggle_beta_life2_show_heart_rate",
                                        compact = true,
                                        onToggle = {
                                            betaLife2ShowHeartRate = !betaLife2ShowHeartRate
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
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
private fun BetaMenuTabPill(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag(tag)
            .semantics {
                contentDescription = "$label tab${if (selected) ", selected" else ""}"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}

@Composable
private fun VulvaBetaToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    compact: Boolean = false,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 48.dp else 56.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = if (compact) 12.dp else 14.dp, vertical = if (compact) 6.dp else 8.dp)
            .testTag(tag)
            .semantics {
                contentDescription = "$title, ${if (checked) "enabled" else "disabled"}. $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
private fun BetaStrengthSliderRow(
    label: String,
    value: Float,
    tag: String,
    onValueChange: (Float) -> Unit
) {
    val pct = (value.coerceIn(0f, 1f) * 100f).roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .semantics {
                    contentDescription = "$label, $pct percent"
                }
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

