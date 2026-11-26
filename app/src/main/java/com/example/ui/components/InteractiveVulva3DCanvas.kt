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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Vulva soft-tissue elasticity Beta slider helpers (Version 21.0):
 *  - `0f` = OFF / exact pre-elastic baseline
 *  - `0.50f` = v21.0 default (+50% elastic resistance -> 0.50 max spread, matching Blossom's self-spread IK cap)
 *  - `0.60f` = maximum (+60% elastic resistance -> 0.40 max spread)
 */
internal const val VULVA_ELASTIC_DEFAULT_RESISTANCE = 0.50f
internal const val VULVA_ELASTIC_MAX_RESISTANCE = 0.60f
internal const val VULVA_ELASTIC_CURVE_REF = 0.40f

/**
 * Peak physiological sequence timings (v23.0):
 *  - Trigger at 136 BPM -> 8-second pre-event (autonomous 136->152 BPM climb, continuous ramping
 *    vibration, green slowly pulsating HR HUD, desynchronized higher-frequency restless leg &
 *    planted-foot knee movement trending from closer together -> wide stance at t = 8s, plus subtle
 *    building toe/finger/arm tension)
 *  - Followed by 16-second dramatic peak episode (tight thigh-squeeze slam -> smooth part open to
 *    default 0f -> 40%-slower legs mixing planted-foot knee shifting with whole-leg sweeps & shakes,
 *    60%-slower dramatic head arcs, 4s eyelid closure -> rapid irregular blinking, mouth opening
 *    right at start with organic breath modulation, subtle toe wiggling + finger curling + arm
 *    shifting, and high-intensity high-frequency irregular pulsing vibration)
 *  - Followed by post-peak recovery sighs and fast HR cooldown stopping cleanly at 125 BPM.
 */
internal const val PEAK_EPISODE_TRIGGER_BPM = 136f
internal const val PEAK_PRE_EVENT_DURATION_MS = 8_000L
internal const val PEAK_PRE_EVENT_TARGET_BPM = 152f
internal const val PEAK_EPISODE_DURATION_MS = 16_000L
internal const val PEAK_EYELID_CLOSE_DURATION_MS = 4_000L
internal const val PEAK_LEG_SLAM_SHUT_TARGET = -0.84f
internal const val PEAK_KNEE_SLAM_SQUEEZE = -1.12f
internal const val PEAK_LEG_PART_OPEN_DURATION_MS = 1_250
internal const val PEAK_PRE_EVENT_WIDE_STANCE = 0.94f
internal const val PEAK_RECOVERY_FLOOR_BPM = 125f
internal const val PEAK_FAST_RECOVERY_DURATION_MS = 6_000L
internal const val PEAK_POST_125_HOLD_DURATION_MS = 15_000L

/** Autonomous heart rate ramp during the 8-second pre-event delay (136 -> 152 BPM regardless of touch). */
internal fun computePreEventHeartRateBpm(startBpm: Float, elapsedMs: Long): Float {
    val start = startBpm.coerceIn(PEAK_EPISODE_TRIGGER_BPM, PEAK_PRE_EVENT_TARGET_BPM)
    val u = (elapsedMs.toFloat() / PEAK_PRE_EVENT_DURATION_MS.toFloat()).coerceIn(0f, 1f)
    return (start + (PEAK_PRE_EVENT_TARGET_BPM - start) * u)
        .coerceIn(PEAK_EPISODE_TRIGGER_BPM, PEAK_PRE_EVENT_TARGET_BPM)
}

/**
 * Evaluates desynchronized, higher-frequency restless pre-event leg spread targets
 * `(leftLegSpread, rightLegSpread)` across `u in 0..1` (0..8s).
 *
 * Unlike a uniform mirror-locked sway, left and right legs move at irrational frequency ratios and
 * offset phases with layered higher-frequency restless quivers/hitches growing in amplitude over
 * time, while following a clear macro trend from closer together (drawing tighter than manual
 * `-0.30f` even from `+1.0f`) progressively outward into a wide stance (`+0.94f, +0.94f`) at `u = 1`.
 */
internal fun computePreEventLegTargets(
    startLeft: Float,
    startRight: Float,
    uRaw: Float,
    dirSign: Float = 1f
): Pair<Float, Float> {
    val u = uRaw.coerceIn(0f, 1f)
    val tau = (2.0 * Math.PI).toFloat()

    // Macro stance trend: draw legs closer together early (~ -0.46f) then gradually open wider
    // across u = 0.20..0.84 before opening cleanly into wide stance (+0.94f) at u = 1.0
    val drawCloser = smoothStepPose(0.00f, 0.22f, u)
    val openTrend = smoothStepPose(0.24f, 0.86f, u)
    val macroBase = (-0.46f) + (0.48f - (-0.46f)) * openTrend

    // Growing restlessness envelope (tapers cleanly to 0 into the final wide stance)
    val wideBlend = smoothStepPose(0.82f, 1.00f, u)
    val restlessEnv = (0.20f + 0.80f * smoothStepPose(0.06f, 0.78f, u)) * (1f - wideBlend)

    // Left leg: independent slow shift + mid restless wave + high-frequency irregular quiver (~1.4-2.5 Hz)
    val lSlow = sin(tau * (1.85f * u + 0.35f * u * u) + 0.35f) * 0.24f
    val lMid = sin(tau * (4.70f * u) + 1.10f) * 0.14f
    val lHigh = (sin(tau * (11.8f * u) + 0.65f) * 0.085f +
        sin(tau * (19.3f * u) + 2.15f) * 0.055f) * (0.65f + 0.35f * sin(tau * 2.9f * u))
    val offsetL = (lSlow + lMid + lHigh) * restlessEnv * dirSign

    // Right leg: irrational frequency ratios & phase offsets so L and R are genuinely desynchronized
    val rSlow = sin(tau * (2.53f * u + 0.42f * u * u) + 2.15f) * 0.25f
    val rMid = sin(tau * (6.15f * u) + 3.40f) * 0.14f
    val rHigh = (sin(tau * (14.6f * u) + 2.75f) * 0.085f +
        sin(tau * (23.7f * u) + 0.90f) * 0.055f) * (0.65f + 0.35f * sin(tau * 3.6f * u + 1.4f))
    val offsetR = (rSlow + rMid + rHigh) * restlessEnv * (-dirSign)

    val rawL = (macroBase + offsetL).coerceIn(BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD, 1f)
    val rawR = (macroBase + offsetR).coerceIn(BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD, 1f)

    // Seamless blend in from starting leg placement over the first ~14% (~1.1s)
    val blendIn = smoothStepPose(0f, 0.14f, u) * (0.35f + 0.65f * drawCloser)
    val easedL = startLeft + (rawL - startLeft) * blendIn
    val easedR = startRight + (rawR - startRight) * blendIn

    val finalL = (easedL + (PEAK_PRE_EVENT_WIDE_STANCE - easedL) * wideBlend)
        .coerceIn(BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD, 1f)
    val finalR = (easedR + (PEAK_PRE_EVENT_WIDE_STANCE - easedR) * wideBlend)
        .coerceIn(BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD, 1f)
    return Pair(finalL, finalR)
}

/**
 * Evaluates independent planted-foot knee/upper-leg shift targets `(leftKneeShift, rightKneeShift)`
 * across `u in 0..1` during the 8-second pre-event: knees fidget and press subtly inward early,
 * weave independently with higher-frequency restless micro-shifts, and taper cleanly back to `0f`
 * right at `u = 1.0` so the wide stance precedes the thigh-squeeze slam cleanly.
 */
internal fun computePreEventKneeShiftTargets(
    uRaw: Float,
    dirSign: Float = 1f
): Pair<Float, Float> {
    val u = uRaw.coerceIn(0f, 1f)
    val tau = (2.0 * Math.PI).toFloat()
    val envIn = smoothStepPose(0.03f, 0.18f, u)
    val envOut = 1f - smoothStepPose(0.82f, 1.00f, u)
    val env = envIn * envOut
    val earlyPress = -0.34f * (1f - smoothStepPose(0.18f, 0.62f, u))

    val kL = earlyPress + dirSign * (
        sin(tau * (2.95f * u) + 0.55f) * 0.34f +
            sin(tau * (9.40f * u) + 1.80f) * 0.16f +
            sin(tau * (16.7f * u) + 0.40f) * 0.08f
        )
    val kR = earlyPress - dirSign * (
        sin(tau * (3.73f * u) + 2.40f) * 0.34f +
            sin(tau * (11.3f * u) + 0.70f) * 0.16f +
            sin(tau * (20.1f * u) + 2.90f) * 0.08f
        )
    return Pair(
        (kL * env).coerceIn(-0.92f, 0.85f),
        (kR * env).coerceIn(-0.92f, 0.85f)
    )
}

internal fun vulvaElasticPullScale(resistance: Float): Float {
    val r = resistance.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
    return 1f - r * 0.85f
}

internal fun vulvaElasticMaxSpread(resistance: Float): Float {
    val r = resistance.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
    return 1f - r
}

internal fun vulvaElasticHoodMax(resistance: Float): Float {
    val r = resistance.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
    return 1f - r * 0.70f
}

internal fun vulvaElasticSpringStiffness(resistance: Float): Float {
    val r = resistance.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
    return Spring.StiffnessLow * (1f + r * 1.25f)
}

internal fun vulvaElasticStep(current: Float, delta: Float, maxLimit: Float, resistance: Float): Float {
    val r = resistance.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
    val resist = if (delta > 0f && r > 0.0001f) {
        val normR = (r / VULVA_ELASTIC_CURVE_REF).coerceIn(0f, 1.50f)
        val frac = (current / maxLimit.coerceAtLeast(0.05f)).coerceIn(0f, 1f)
        (1f - (0.32f * normR) * frac).coerceAtLeast(0.42f)
    } else {
        1f
    }
    return (current + delta * resist).coerceIn(0f, maxLimit)
}

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
    modifier: Modifier = Modifier,
    sessionState: BlossomViewportSessionState = remember { BlossomViewportSessionState() }
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
    // All Animatables restore their exact values from sessionState on scroll re-entry (v19.0)
    val leftOuterSpread = remember(sessionState) { Animatable(sessionState.leftOuterSpread) }
    val rightOuterSpread = remember(sessionState) { Animatable(sessionState.rightOuterSpread) }
    val leftInnerOpen = remember(sessionState) { Animatable(sessionState.leftInnerOpen) }
    val rightInnerOpen = remember(sessionState) { Animatable(sessionState.rightInnerOpen) }
    val hoodRetraction = remember(sessionState) { Animatable(sessionState.hoodRetraction) }
    val leftBreastX = remember(sessionState) { Animatable(sessionState.leftBreastX) }
    val leftBreastY = remember(sessionState) { Animatable(sessionState.leftBreastY) }
    val rightBreastX = remember(sessionState) { Animatable(sessionState.rightBreastX) }
    val rightBreastY = remember(sessionState) { Animatable(sessionState.rightBreastY) }
    // Persistent leg-spread & head-turn/nod channels (stay in place until moved again or explicitly reset)
    val leftLegSpread = remember(sessionState) { Animatable(sessionState.leftLegSpread) }
    val rightLegSpread = remember(sessionState) { Animatable(sessionState.rightLegSpread) }
    val headTurn = remember(sessionState) { Animatable(sessionState.headTurn) }
    val headPitch = remember(sessionState) { Animatable(sessionState.headPitch) }
    // Subtle elastic buttock soft-tissue channels + natural interval eyelid blink
    val leftGluteX = remember(sessionState) { Animatable(sessionState.leftGluteX) }
    val leftGluteY = remember(sessionState) { Animatable(sessionState.leftGluteY) }
    val rightGluteX = remember(sessionState) { Animatable(sessionState.rightGluteX) }
    val rightGluteY = remember(sessionState) { Animatable(sessionState.rightGluteY) }
    val eyeBlink = remember(sessionState) { Animatable(sessionState.eyeBlink) }
    val eyeBlinkR = remember(sessionState) { Animatable(sessionState.eyeBlinkR) }
    // Lifelike Beta (Tab 2) live micro-behavior channels
    val eyeGazeX = remember(sessionState) { Animatable(sessionState.eyeGazeX) }
    val eyeGazeY = remember(sessionState) { Animatable(sessionState.eyeGazeY) }
    val idleHeadTurn = remember(sessionState) { Animatable(sessionState.idleHeadTurn) }
    val idleHeadPitch = remember(sessionState) { Animatable(sessionState.idleHeadPitch) }
    val idleHeadRoll = remember(sessionState) { Animatable(sessionState.idleHeadRoll) }
    val faceWarmth = remember(sessionState) { Animatable(sessionState.faceWarmth) }
    val faceBrow = remember(sessionState) { Animatable(sessionState.faceBrow) }
    val faceLipSoft = remember(sessionState) { Animatable(sessionState.faceLipSoft) }
    val shoulderRelaxL = remember(sessionState) { Animatable(sessionState.shoulderRelaxL) }
    val shoulderRelaxR = remember(sessionState) { Animatable(sessionState.shoulderRelaxR) }
    // Lifelike+ Beta (Tab 3) live micro-behavior channels (all strictly 0f when their respective toggles are OFF)
    val anticipationGazeX = remember(sessionState) { Animatable(sessionState.anticipationGazeX) }
    val anticipationGazeY = remember(sessionState) { Animatable(sessionState.anticipationGazeY) }
    val swallowPhase = remember(sessionState) { Animatable(sessionState.swallowPhase) }
    val settleHeadTurn = remember(sessionState) { Animatable(sessionState.settleHeadTurn) }
    val settleHeadPitch = remember(sessionState) { Animatable(sessionState.settleHeadPitch) }
    val settleHeadRoll = remember(sessionState) { Animatable(sessionState.settleHeadRoll) }
    val settleShoulderL = remember(sessionState) { Animatable(sessionState.settleShoulderL) }
    val settleShoulderR = remember(sessionState) { Animatable(sessionState.settleShoulderR) }
    val settleLegL = remember(sessionState) { Animatable(sessionState.settleLegL) }
    val settleLegR = remember(sessionState) { Animatable(sessionState.settleLegR) }
    val nostrilBreath = remember(sessionState) { Animatable(sessionState.nostrilBreath) }
    val breathLipEase = remember(sessionState) { Animatable(sessionState.breathLipEase) }
    val livingWarmth = remember(sessionState) { Animatable(sessionState.livingWarmth) }
    var posturalSettleTrigger by sessionState.posturalSettleTriggerState
    var lastManualHeadDragMs by sessionState.lastManualHeadDragMsState
    // Unified touch-engagement stimulus & realistic cardio-respiratory heart rate (66..142 BPM, 3× slower rise in v19.0)
    var touchStimulus by sessionState.touchStimulusState
    var lastStimulusTouchMs by sessionState.lastStimulusTouchMsState
    var heartRateBpm by sessionState.heartRateBpmState
    var heartRateSensitivity by sessionState.heartRateSensitivityState
    var heartRateDrive by sessionState.heartRateDriveState
    var lastHeartStimMs by sessionState.lastHeartStimMsState
    val heartPulseScale = remember { Animatable(1f) }
    val heartPillPulseScale = remember { Animatable(1f) }
    // Peak 136 BPM -> 7s Pre-Event -> 12s Peak -> 125 BPM Cooldown channels (persisted across scroll)
    var peakPreEventActive by sessionState.peakPreEventActiveState
    var peakPreEventStartMs by sessionState.peakPreEventStartMsState
    var peakPreEventStartBpm by sessionState.peakPreEventStartBpmState
    var peakEpisodeActive by sessionState.peakEpisodeActiveState
    var peakEpisodeEndMs by sessionState.peakEpisodeEndMsState
    var peakPostCooldownActive by sessionState.peakPostCooldownActiveState
    var peakRecoveryFastEndMs by sessionState.peakRecoveryFastEndMsState
    var peakRecoveryHoldUntilMs by sessionState.peakRecoveryHoldUntilMsState
    var peakRefractoryUntilMs by sessionState.peakRefractoryUntilMsState
    var postPeakSighToken by sessionState.postPeakSighTokenState
    var postPeakSighCount by sessionState.postPeakSighCountState
    val peakHeadPitch = remember(sessionState) { Animatable(sessionState.peakHeadPitch) }
    val peakHeadTurn = remember(sessionState) { Animatable(sessionState.peakHeadTurn) }
    val peakHeadRoll = remember(sessionState) { Animatable(sessionState.peakHeadRoll) }
    val peakLegShakeL = remember(sessionState) { Animatable(sessionState.peakLegShakeL) }
    val peakLegShakeR = remember(sessionState) { Animatable(sessionState.peakLegShakeR) }
    val peakKneeShiftL = remember(sessionState) { Animatable(sessionState.peakKneeShiftL) }
    val peakKneeShiftR = remember(sessionState) { Animatable(sessionState.peakKneeShiftR) }
    val peakToeWiggleL = remember(sessionState) { Animatable(sessionState.peakToeWiggleL) }
    val peakToeWiggleR = remember(sessionState) { Animatable(sessionState.peakToeWiggleR) }
    val peakArmShiftL = remember(sessionState) { Animatable(sessionState.peakArmShiftL) }
    val peakArmShiftR = remember(sessionState) { Animatable(sessionState.peakArmShiftR) }
    val peakFingerCurlL = remember(sessionState) { Animatable(sessionState.peakFingerCurlL) }
    val peakFingerCurlR = remember(sessionState) { Animatable(sessionState.peakFingerCurlR) }
    val peakMouthOpen = remember(sessionState) { Animatable(sessionState.peakMouthOpen) }
    // Animated self-spread hand/arm IK pose (0 = relaxed at sides, 1 = hands holding labia open)
    var blossomSpreadPoseActive by sessionState.blossomSpreadPoseActiveState
    val selfSpreadPose = remember(sessionState) { Animatable(sessionState.selfSpreadPose) }

    // Live Blossom appearance customization: Hair color (default Chestnut Brown), Breast size (default Medium), Pubic hair (default Natural 0.45)
    var hairStyleIndex by sessionState.hairStyleIndexState
    var breastSize by sessionState.breastSizeState
    var pubicHairAmount by sessionState.pubicHairAmountState
    val hairStyle = BlossomHairStyles[hairStyleIndex.coerceIn(0, BlossomHairStyles.lastIndex)]

    // Smooth 3D camera orbit, zoom, target framing, lighting & Blossom full-body state (persisted across scroll disposal)
    var isOrbitMode by sessionState.isOrbitModeState
    var dramaticSideLight by sessionState.dramaticSideLightState
    var showExperimentalLegs by sessionState.showExperimentalLegsState
    // Buried compatibility toggle: restores exact pre-Beta v1 vulva, seam, neck, and hair appearance
    var showAdvancedRendering by sessionState.showAdvancedRenderingState
    var legacyVulvaMode by sessionState.legacyVulvaModeState
    // Floating always-on-top 3-Tab Beta Lab overlay (v19.0 Pre-Graduation Milestone defaults):
    //  Tab 0 (Vulva): all 6 anatomical realism v3 options + elasticity (40%, max 60%) + breast boost (45%) ON by default
    //  Tab 1 (Life):  all 6 promoted ON by default
    //  Tab 2 (Life+): all 7 subtle realism features ON by default; Heart Rate HUD remains OFF by default
    var showBetaMenu by sessionState.showBetaMenuState
    var betaMenuTab by sessionState.betaMenuTabState
    var betaV3OrganicContinuity by sessionState.betaV3OrganicContinuityState
    var betaV3ContinuityStrength by sessionState.betaV3ContinuityStrengthState
    var betaV3SoftPigmentation by sessionState.betaV3SoftPigmentationState
    var betaV3PigmentStrength by sessionState.betaV3PigmentStrengthState
    var betaV3AnatomicalAsymmetry by sessionState.betaV3AnatomicalAsymmetryState
    var betaV3AsymmetryStrength by sessionState.betaV3AsymmetryStrengthState
    var betaV3RegionalSurfaceDetail by sessionState.betaV3RegionalSurfaceDetailState
    var betaV3SurfaceStrength by sessionState.betaV3SurfaceStrengthState
    var betaV3LabialVestibuleBlend by sessionState.betaV3LabialVestibuleBlendState
    var betaV3LabialBlendStrength by sessionState.betaV3LabialBlendStrengthState
    var betaV3IntricateLabiaFolds by sessionState.betaV3IntricateLabiaFoldsState
    var betaV3LabiaFoldsStrength by sessionState.betaV3LabiaFoldsStrengthState
    var betaV3MedialRootContinuity by sessionState.betaV3MedialRootContinuityState
    var betaV3MedialRootStrength by sessionState.betaV3MedialRootStrengthState
    var betaVulvaElasticityEnabled by sessionState.betaVulvaElasticityEnabledState
    var betaVulvaElasticity by sessionState.betaVulvaElasticityState
    var betaBreastSizeBoostEnabled by sessionState.betaBreastSizeBoostEnabledState
    var betaBreastSizeBoost by sessionState.betaBreastSizeBoostState
    // Lifelike Beta (Tab 2) toggles (promoted ON by default)
    var betaLifeEyeMicrosaccades by sessionState.betaLifeEyeMicrosaccadesState
    var betaLifeDoubleBlinking by sessionState.betaLifeDoubleBlinkingState
    var betaLifeHeadRepositioning by sessionState.betaLifeHeadRepositioningState
    var betaLifeDeepBreathsSighs by sessionState.betaLifeDeepBreathsSighsState
    var betaLifeShoulderRelaxation by sessionState.betaLifeShoulderRelaxationState
    var betaLifeFacialExpressions by sessionState.betaLifeFacialExpressionsState
    // Lifelike+ Beta (Tab 3) next-gen realism toggles (all 7 ON by default; Heart Rate HUD OFF by default)
    var betaLife2EyelidTracking by sessionState.betaLife2EyelidTrackingState
    var betaLife2AsymmetricBlinking by sessionState.betaLife2AsymmetricBlinkingState
    var betaLife2GazeAnticipation by sessionState.betaLife2GazeAnticipationState
    var betaLife2SubtleSwallowing by sessionState.betaLife2SubtleSwallowingState
    var betaLife2PosturalSettling by sessionState.betaLife2PosturalSettlingState
    var betaLife2BreathFaceCoupling by sessionState.betaLife2BreathFaceCouplingState
    var betaLife2LivingWarmth by sessionState.betaLife2LivingWarmthState
    var betaLife2ShowHeartRate by sessionState.betaLife2ShowHeartRateState

    var activePresetOrdinal by sessionState.activePresetOrdinalState
    val activePreset: CameraAnglePreset? = activePresetOrdinal?.let { CameraAnglePreset.entries.getOrNull(it) }
    val camYaw = remember(sessionState) { Animatable(sessionState.camYaw) }
    val camPitch = remember(sessionState) { Animatable(sessionState.camPitch) }
    val camZoom = remember(sessionState) { Animatable(sessionState.camZoom) }
    val camTargetY = remember(sessionState) { Animatable(sessionState.camTargetY) }
    val camTargetZ = remember(sessionState) { Animatable(sessionState.camTargetZ) }

    fun persistAnimatablesToSession() {
        sessionState.leftOuterSpread = leftOuterSpread.value
        sessionState.rightOuterSpread = rightOuterSpread.value
        sessionState.leftInnerOpen = leftInnerOpen.value
        sessionState.rightInnerOpen = rightInnerOpen.value
        sessionState.hoodRetraction = hoodRetraction.value
        sessionState.leftBreastX = leftBreastX.value
        sessionState.leftBreastY = leftBreastY.value
        sessionState.rightBreastX = rightBreastX.value
        sessionState.rightBreastY = rightBreastY.value
        sessionState.leftLegSpread = leftLegSpread.value
        sessionState.rightLegSpread = rightLegSpread.value
        sessionState.headTurn = headTurn.value
        sessionState.headPitch = headPitch.value
        sessionState.leftGluteX = leftGluteX.value
        sessionState.leftGluteY = leftGluteY.value
        sessionState.rightGluteX = rightGluteX.value
        sessionState.rightGluteY = rightGluteY.value
        sessionState.eyeBlink = eyeBlink.value
        sessionState.eyeBlinkR = eyeBlinkR.value
        sessionState.eyeGazeX = eyeGazeX.value
        sessionState.eyeGazeY = eyeGazeY.value
        sessionState.idleHeadTurn = idleHeadTurn.value
        sessionState.idleHeadPitch = idleHeadPitch.value
        sessionState.idleHeadRoll = idleHeadRoll.value
        sessionState.faceWarmth = faceWarmth.value
        sessionState.faceBrow = faceBrow.value
        sessionState.faceLipSoft = faceLipSoft.value
        sessionState.shoulderRelaxL = shoulderRelaxL.value
        sessionState.shoulderRelaxR = shoulderRelaxR.value
        sessionState.anticipationGazeX = anticipationGazeX.value
        sessionState.anticipationGazeY = anticipationGazeY.value
        sessionState.swallowPhase = swallowPhase.value
        sessionState.settleHeadTurn = settleHeadTurn.value
        sessionState.settleHeadPitch = settleHeadPitch.value
        sessionState.settleHeadRoll = settleHeadRoll.value
        sessionState.settleShoulderL = settleShoulderL.value
        sessionState.settleShoulderR = settleShoulderR.value
        sessionState.settleLegL = settleLegL.value
        sessionState.settleLegR = settleLegR.value
        sessionState.nostrilBreath = nostrilBreath.value
        sessionState.breathLipEase = breathLipEase.value
        sessionState.livingWarmth = livingWarmth.value
        sessionState.peakHeadPitch = peakHeadPitch.value
        sessionState.peakHeadTurn = peakHeadTurn.value
        sessionState.peakHeadRoll = peakHeadRoll.value
        sessionState.peakLegShakeL = peakLegShakeL.value
        sessionState.peakLegShakeR = peakLegShakeR.value
        sessionState.peakKneeShiftL = peakKneeShiftL.value
        sessionState.peakKneeShiftR = peakKneeShiftR.value
        sessionState.peakToeWiggleL = peakToeWiggleL.value
        sessionState.peakToeWiggleR = peakToeWiggleR.value
        sessionState.peakArmShiftL = peakArmShiftL.value
        sessionState.peakArmShiftR = peakArmShiftR.value
        sessionState.peakFingerCurlL = peakFingerCurlL.value
        sessionState.peakFingerCurlR = peakFingerCurlR.value
        sessionState.peakMouthOpen = peakMouthOpen.value
        sessionState.selfSpreadPose = selfSpreadPose.value
        sessionState.camYaw = camYaw.value
        sessionState.camPitch = camPitch.value
        sessionState.camZoom = camZoom.value
        sessionState.camTargetY = camTargetY.value
        sessionState.camTargetZ = camTargetZ.value
    }

    SideEffect { persistAnimatablesToSession() }
    DisposableEffect(sessionState) {
        onDispose {
            hapticsEngine.cancel()
            persistAnimatablesToSession()
        }
    }

    // Live 3D skin sensory glow under fingertip
    var touchGlowX by remember { mutableFloatStateOf(0.5f) }
    var touchGlowY by remember { mutableFloatStateOf(0.5f) }
    var touchGlowColor by remember { mutableStateOf(Color(0xFFFFD1DC)) }
    val touchGlowIntensity = remember { Animatable(0f) }

    val activeVulvaElasticity = sessionState.effectiveVulvaElasticity
    val activeBreastSizeBoost = sessionState.effectiveBreastSizeBoost

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
    val latestBetaVulvaElasticity by rememberUpdatedState(activeVulvaElasticity)
    val latestBetaBreastSizeBoost by rememberUpdatedState(activeBreastSizeBoost)
    val latestPeakPreEventActive by rememberUpdatedState(peakPreEventActive)
    val latestPeakEpisodeActive by rememberUpdatedState(peakEpisodeActive)

    fun registerPhysiologicalStimulus(level: Float, isDragMotion: Boolean = false) {
        val clamped = level.coerceIn(0f, 1f)
        val nowMs = System.currentTimeMillis()
        lastStimulusTouchMs = nowMs
        touchStimulus = max(touchStimulus, clamped)
        // During the 7s autonomous pre-event, 12s peak episode, or post-peak drop down to 125 BPM,
        // let the physiological sequence control heart rate cleanly without stray touch jumps
        if (peakPreEventActive || peakEpisodeActive || peakPostCooldownActive) return
        lastHeartStimMs = nowMs
        // Weight sensitivity smoothly upward toward the most sensitive active contact zone
        heartRateSensitivity = max(heartRateSensitivity * 0.86f, clamped)
        heartRateDrive = max(heartRateDrive, if (isDragMotion) 1.0f else 0.58f)
        if (!isDragMotion) {
            // Subtle immediate autonomic pulse bump on tap/contact, scaled 3× slower for v19.0
            val tapCeiling = VulvaGlTextureView.RESTING_HEART_RATE_BPM +
                (VulvaGlTextureView.MAX_HEART_RATE_BPM - VulvaGlTextureView.RESTING_HEART_RATE_BPM) * (0.18f + 0.82f * clamped)
            val tapBump = (0.6f + 2.4f * clamped) / VulvaGlTextureView.HEART_RATE_RISE_RATE_DIVISOR
            if (heartRateBpm < tapCeiling) {
                heartRateBpm = (heartRateBpm + tapBump).coerceAtMost(tapCeiling)
            }
        }
    }

    fun springBackTissue(resetLegs: Boolean = false) {
        val elasticRes = latestBetaVulvaElasticity.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
        val activeMaxSpread = vulvaElasticMaxSpread(elasticRes)
        val tissueSpring = spring<Float>(
            dampingRatio = 0.76f + 0.06f * (elasticRes / VULVA_ELASTIC_CURVE_REF).coerceIn(0f, 1.5f),
            stiffness = vulvaElasticSpringStiffness(elasticRes)
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
        // (capped to the v19.0 40% elastic limit or any higher active elasticity resistance)
        val outerRestTarget = if (latestBlossomSpreadPoseActive && !resetLegs) {
            min(BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET, activeMaxSpread)
        } else {
            0f
        }
        val innerRestTarget = if (latestBlossomSpreadPoseActive && !resetLegs) {
            min(BlossomArmIkRig.SELF_SPREAD_INNER_TARGET, activeMaxSpread)
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
            peakPreEventActive = false
            peakEpisodeActive = false
            peakPostCooldownActive = false
            peakRecoveryFastEndMs = 0L
            peakRecoveryHoldUntilMs = 0L
            hapticsEngine.cancel()
            coroutineScope.launch { peakHeadPitch.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakHeadTurn.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakHeadRoll.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakLegShakeL.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakLegShakeR.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakKneeShiftL.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakKneeShiftR.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakToeWiggleL.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakToeWiggleR.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakArmShiftL.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakArmShiftR.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakFingerCurlL.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakFingerCurlR.animateTo(0f, positionalSpring) }
            coroutineScope.launch { peakMouthOpen.animateTo(0f, positionalSpring) }
        }
    }

    fun animateToPreset(preset: CameraAnglePreset, legsEnabled: Boolean = showExperimentalLegs) {
        activePresetOrdinal = preset.ordinal
        val target = preset.resolveCamera(legsEnabled)
        val camSpring = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        coroutineScope.launch { camYaw.animateTo(target.yawDeg, camSpring) }
        coroutineScope.launch { camPitch.animateTo(target.pitchDeg, camSpring) }
        coroutineScope.launch { camZoom.animateTo(target.zoom, camSpring) }
        coroutineScope.launch { camTargetY.animateTo(target.targetY, camSpring) }
        coroutineScope.launch { camTargetZ.animateTo(target.targetZ, camSpring) }
    }

    LaunchedEffect(blossomSpreadPoseActive) {
        if (blossomSpreadPoseActive && selfSpreadPose.value < 0.999f) {
            val startPose = selfSpreadPose.value
            val remaining = (1f - startPose).coerceIn(0.05f, 1f)
            val durationMs = (2500f * remaining).roundToInt().coerceAtLeast(280)
            val startSpreadT = smoothStepPose(0.66f, 1.00f, startPose)
            val activeMaxSpread = vulvaElasticMaxSpread(latestBetaVulvaElasticity)
            val selfOuterCap = min(BlossomArmIkRig.SELF_SPREAD_OUTER_TARGET, activeMaxSpread)
            val selfInnerCap = min(BlossomArmIkRig.SELF_SPREAD_INNER_TARGET, activeMaxSpread)
            val startLO = leftOuterSpread.value
            val startRO = rightOuterSpread.value
            val startLI = leftInnerOpen.value
            val startRI = rightInnerOpen.value
            val targetLO = max(startLO.coerceAtMost(activeMaxSpread), selfOuterCap)
            val targetRO = max(startRO.coerceAtMost(activeMaxSpread), selfOuterCap)
            val targetLI = max(startLI.coerceAtMost(activeMaxSpread), selfInnerCap)
            val targetRI = max(startRI.coerceAtMost(activeMaxSpread), selfInnerCap)

            selfSpreadPose.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
            ) {
                val spreadT = smoothStepPose(0.66f, 1.00f, value)
                if (spreadT > 0.0001f && activePointerCount == 0) {
                    val spreadFrac = if (startSpreadT >= 0.999f) {
                        1f
                    } else {
                        ((spreadT - startSpreadT) / (1f - startSpreadT)).coerceIn(0f, 1f)
                    }
                    val lo = startLO + (targetLO - startLO) * spreadFrac
                    val ro = startRO + (targetRO - startRO) * spreadFrac
                    val li = startLI + (targetLI - startLI) * spreadFrac
                    val ri = startRI + (targetRI - startRI) * spreadFrac
                    coroutineScope.launch {
                        leftOuterSpread.snapTo(lo)
                        rightOuterSpread.snapTo(ro)
                        leftInnerOpen.snapTo(li)
                        rightInnerOpen.snapTo(ri)
                    }
                }
            }
        } else if (!blossomSpreadPoseActive && selfSpreadPose.value > 0.001f) {
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
        if (resetTrigger > sessionState.lastHandledResetTrigger) {
            sessionState.lastHandledResetTrigger = resetTrigger
            blossomSpreadPoseActive = false
            springBackTissue(resetLegs = true)
        }
    }
    LaunchedEffect(holdOpen) {
        val prevHoldOpen = sessionState.lastObservedHoldOpen
        sessionState.lastObservedHoldOpen = holdOpen
        if (prevHoldOpen == true && !holdOpen && activePointerCount == 0) {
            springBackTissue(resetLegs = false)
        }
    }

    // Smoothly integrate physiological heart rate and drive the 3-phase 136 BPM peak sequence:
    //   Phase 1 (7s pre-event): triggered at 136 BPM -> autonomous HR climb 136 -> 152 BPM
    //   Phase 2 (12s peak):     sustained at 152 BPM with dramatic choreography + irregular pulsing vibration
    //   Phase 3 (cooldown):     2-3 powerful sighs + 2x HR recovery stopping cleanly at 125 BPM
    LaunchedEffect(showExperimentalLegs) {
        if (!showExperimentalLegs) {
            touchStimulus = 0f
            heartRateBpm = VulvaGlTextureView.RESTING_HEART_RATE_BPM
            heartRateSensitivity = 0f
            heartRateDrive = 0f
            peakPreEventActive = false
            peakEpisodeActive = false
            peakPostCooldownActive = false
            peakRecoveryFastEndMs = 0L
            peakRecoveryHoldUntilMs = 0L
            hapticsEngine.cancel()
            return@LaunchedEffect
        }
        val peakRng = kotlin.random.Random(System.currentTimeMillis() xor 0x3D9E51A7L)
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

            // Phase 1 Trigger: enter 7-second pre-event delay immediately upon hitting 136 BPM
            if (!peakPreEventActive && !peakEpisodeActive && !peakPostCooldownActive &&
                heartRateBpm >= PEAK_EPISODE_TRIGGER_BPM && nowMs >= peakRefractoryUntilMs
            ) {
                peakPreEventActive = true
                peakPreEventStartMs = nowMs
                peakPreEventStartBpm = heartRateBpm.coerceIn(PEAK_EPISODE_TRIGGER_BPM, PEAK_PRE_EVENT_TARGET_BPM)
                peakRecoveryHoldUntilMs = 0L
            }

            // Phase 1 -> Phase 2 Transition: after 7s pre-event, start the 12-second peak event at 152 BPM
            if (peakPreEventActive && nowMs - peakPreEventStartMs >= PEAK_PRE_EVENT_DURATION_MS) {
                peakPreEventActive = false
                peakEpisodeActive = true
                peakEpisodeEndMs = nowMs + PEAK_EPISODE_DURATION_MS
                heartRateBpm = PEAK_PRE_EVENT_TARGET_BPM
            }

            // Phase 2 -> Phase 3 Transition: end 12-second peak event -> recovery sighs + cooldown ONLY down to 125 BPM
            if (peakEpisodeActive && nowMs >= peakEpisodeEndMs) {
                peakEpisodeActive = false
                peakPostCooldownActive = true
                postPeakSighCount = 2 + peakRng.nextInt(2)
                postPeakSighToken++
                peakRecoveryFastEndMs = nowMs + PEAK_FAST_RECOVERY_DURATION_MS
                peakRefractoryUntilMs = nowMs + 4_000L
                heartRateDrive = 0f
                heartRateSensitivity = 0f
                lastHeartStimMs = nowMs - 3_000L
            }

            val secsSinceHeartStim = ((nowMs - lastHeartStimMs).coerceAtLeast(0L)) / 1000f
            if (secsSinceHeartStim > 0.28f && heartRateDrive > 0f) {
                heartRateDrive = (heartRateDrive - dtSec * 2.2f).coerceAtLeast(0f)
            }
            if (secsSinceHeartStim > 1.2f && heartRateSensitivity > 0f) {
                heartRateSensitivity = (heartRateSensitivity - dtSec * 0.18f).coerceAtLeast(0f)
            }

            heartRateBpm = when {
                peakPreEventActive -> {
                    // Autonomous steady climb from 136 -> 152 BPM across the 7-second pre-event, regardless of touch
                    computePreEventHeartRateBpm(peakPreEventStartBpm, nowMs - peakPreEventStartMs)
                }
                peakEpisodeActive -> {
                    // Sustained at peak 152 BPM throughout the 12-second main event
                    PEAK_PRE_EVENT_TARGET_BPM
                }
                peakPostCooldownActive -> {
                    val cooledBpm = VulvaGlTextureView.stepHeartRateBpm(
                        currentBpm = heartRateBpm,
                        dtSec = dtSec,
                        targetSensitivityGain = 0f,
                        activeDrive = 0f,
                        secondsSinceLastStim = 5.0f,
                        recoveryRateMultiplier = 2.0f,
                        forceRecovery = true,
                        recoveryFloorBpm = PEAK_RECOVERY_FLOOR_BPM,
                        maxBpm = VulvaGlTextureView.PEAK_MAX_HEART_RATE_BPM
                    )
                    if (cooledBpm <= PEAK_RECOVERY_FLOOR_BPM + 0.05f || nowMs >= peakRecoveryFastEndMs) {
                        peakPostCooldownActive = false
                        peakRecoveryFastEndMs = 0L
                        peakRecoveryHoldUntilMs = nowMs + PEAK_POST_125_HOLD_DURATION_MS
                        peakRefractoryUntilMs = nowMs + 900L
                        lastHeartStimMs = nowMs
                        max(cooledBpm, PEAK_RECOVERY_FLOOR_BPM)
                    } else {
                        cooledBpm
                    }
                }
                else -> {
                    val holdFloor = if (nowMs < peakRecoveryHoldUntilMs) {
                        PEAK_RECOVERY_FLOOR_BPM
                    } else {
                        VulvaGlTextureView.RESTING_HEART_RATE_BPM
                    }
                    VulvaGlTextureView.stepHeartRateBpm(
                        currentBpm = heartRateBpm,
                        dtSec = dtSec,
                        targetSensitivityGain = heartRateSensitivity,
                        activeDrive = heartRateDrive,
                        secondsSinceLastStim = secsSinceHeartStim,
                        recoveryRateMultiplier = 1.0f,
                        forceRecovery = false,
                        recoveryFloorBpm = holdFloor,
                        maxBpm = VulvaGlTextureView.MAX_HEART_RATE_BPM
                    )
                }
            }
        }
    }

    // Phase 1 — 8-Second Pre-Event Choreography (136 -> 152 BPM):
    //  - Continuous phone vibration ramping smoothly from low intensity to full high intensity
    //  - Desynchronized, higher-frequency restless leg & planted-foot knee movement tending from
    //    closer together -> wider stance at t = 8s, plus subtle building toe/finger/arm tension
    LaunchedEffect(showExperimentalLegs, peakPreEventActive) {
        if (!showExperimentalLegs || !peakPreEventActive) {
            if (!latestPeakEpisodeActive) {
                hapticsEngine.cancel()
                val relaxSpec = tween<Float>(durationMillis = 480, easing = FastOutSlowInEasing)
                coroutineScope.launch { peakKneeShiftL.animateTo(0f, relaxSpec) }
                coroutineScope.launch { peakKneeShiftR.animateTo(0f, relaxSpec) }
                coroutineScope.launch { peakToeWiggleL.animateTo(0f, relaxSpec) }
                coroutineScope.launch { peakToeWiggleR.animateTo(0f, relaxSpec) }
                coroutineScope.launch { peakArmShiftL.animateTo(0f, relaxSpec) }
                coroutineScope.launch { peakArmShiftR.animateTo(0f, relaxSpec) }
                coroutineScope.launch { peakFingerCurlL.animateTo(0f, relaxSpec) }
                coroutineScope.launch { peakFingerCurlR.animateTo(0f, relaxSpec) }
            }
            return@LaunchedEffect
        }
        val startMs = peakPreEventStartMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        val endMs = startMs + PEAK_PRE_EVENT_DURATION_MS
        val preRng = kotlin.random.Random(startMs xor 0x2F4A6C8EL)
        val startL = leftLegSpread.value
        val startR = rightLegSpread.value
        val dirSign = when {
            startL - startR > 0.15f -> 1f
            startR - startL > 0.15f -> -1f
            else -> if (preRng.nextBoolean()) 1f else -1f
        }

        // Clear any leftover micro leg-settle offsets cleanly
        launch { settleLegL.animateTo(0f, tween(180)) }
        launch { settleLegR.animateTo(0f, tween(180)) }
        launch { peakLegShakeL.animateTo(0f, tween(180)) }
        launch { peakLegShakeR.animateTo(0f, tween(180)) }

        // Continuous vibration smoothly ramping from low -> high intensity across the 8 seconds
        val vibrationJob = launch {
            try {
                while (System.currentTimeMillis() < endMs) {
                    val now = System.currentTimeMillis()
                    val remaining = (endMs - now).coerceAtLeast(0L)
                    if (remaining <= 20L) break
                    val progress = ((now - startMs).toFloat() / PEAK_PRE_EVENT_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                    if (latestHapticsEnabled) {
                        val sliceMs = min(240L, remaining + 60L).coerceAtLeast(70L)
                        hapticsEngine.playContinuousIntensity(progress, durationMs = sliceMs)
                    } else {
                        hapticsEngine.cancel()
                    }
                    delay(min(155L, remaining))
                }
            } finally {
                if (!latestPeakEpisodeActive) {
                    hapticsEngine.cancel()
                }
            }
        }

        // Desynchronized restless legs + planted-foot knee fidgets + building subtle toe/finger/arm tension
        val swayJob = launch {
            val tau = (2.0 * Math.PI).toFloat()
            while (true) {
                val now = System.currentTimeMillis()
                val u = ((now - startMs).toFloat() / PEAK_PRE_EVENT_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                val (targetL, targetR) = computePreEventLegTargets(startL, startR, u, dirSign)
                val (kneeL, kneeR) = computePreEventKneeShiftTargets(u, dirSign)
                leftLegSpread.snapTo(targetL)
                rightLegSpread.snapTo(targetR)
                peakKneeShiftL.snapTo(kneeL)
                peakKneeShiftR.snapTo(kneeR)

                // Subtle building toe wiggling, finger curling, and arm tension across the 8s build-up
                val buildEnv = smoothStepPose(0.10f, 0.88f, u)
                peakToeWiggleL.snapTo((sin(tau * 6.4f * u + 0.5f) * 0.34f + sin(tau * 11.2f * u) * 0.14f) * buildEnv)
                peakToeWiggleR.snapTo((sin(tau * 7.9f * u + 2.2f) * 0.34f + sin(tau * 13.5f * u + 1.1f) * 0.14f) * buildEnv)
                peakFingerCurlL.snapTo(((0.22f + 0.20f * sin(tau * 2.3f * u + 0.4f)) * buildEnv).coerceIn(0f, 0.55f))
                peakFingerCurlR.snapTo(((0.22f + 0.20f * sin(tau * 2.9f * u + 2.1f)) * buildEnv).coerceIn(0f, 0.55f))
                peakArmShiftL.snapTo((sin(tau * 1.45f * u + 0.2f) * 0.26f) * buildEnv)
                peakArmShiftR.snapTo((sin(tau * 1.88f * u + 2.3f) * 0.26f) * buildEnv)

                if (now >= endMs) break
                delay(16L)
            }
        }

        vibrationJob.join()
        swayJob.join()
    }

    // Phase 2 — 16-Second Dramatic Peak Event Choreography (starts right after 8s pre-event at 152 BPM):
    //  - Vibration: switches from continuous to high-intensity, high-frequency, slightly irregular pulsing
    //  - Legs (40% slower = 1.67x durations): slam thighs tightly squeezed together (-0.84f spread +
    //    -1.12f knee squeeze) -> part open slowly to default (0f) over ~1.25s -> mix planted-foot
    //    knee/upper-leg shifting (feet stay planted while knees shift) with independent whole-leg
    //    sweeps and irregular shaking for the remainder of the 16s
    //  - Head (60% slower = 2.5x durations): deep downward dip, then slow dramatic lean-back with
    //    large irregular side-to-side swings & rolls throughout the 16s
    //  - Eyelids: close tightly for 4 full seconds, then open back up into dramatic irregular blinking
    //  - Mouth: opens smoothly right at start of the 16s with organic breath-gasp modulation
    //  - Extremities: subtle independent toe wiggling, finger curling, and gentle arm shifting
    var wasPeakEpisodeActive by remember(sessionState) { mutableStateOf(sessionState.peakEpisodeActive) }
    LaunchedEffect(showExperimentalLegs, peakEpisodeActive) {
        if (!showExperimentalLegs) {
            hapticsEngine.cancel()
            peakHeadPitch.snapTo(0f)
            peakHeadTurn.snapTo(0f)
            peakHeadRoll.snapTo(0f)
            peakLegShakeL.snapTo(0f)
            peakLegShakeR.snapTo(0f)
            peakKneeShiftL.snapTo(0f)
            peakKneeShiftR.snapTo(0f)
            peakToeWiggleL.snapTo(0f)
            peakToeWiggleR.snapTo(0f)
            peakArmShiftL.snapTo(0f)
            peakArmShiftR.snapTo(0f)
            peakFingerCurlL.snapTo(0f)
            peakFingerCurlR.snapTo(0f)
            peakMouthOpen.snapTo(0f)
            wasPeakEpisodeActive = false
            return@LaunchedEffect
        }
        if (!peakEpisodeActive) {
            if (!latestPeakPreEventActive) {
                hapticsEngine.cancel()
            }
            val headReturnSpec = tween<Float>(durationMillis = 1250, easing = FastOutSlowInEasing)
            val shakeStopSpec = tween<Float>(durationMillis = 560, easing = FastOutSlowInEasing)
            val mouthCloseSpec = tween<Float>(durationMillis = 780, easing = FastOutSlowInEasing)
            val extremRelaxSpec = tween<Float>(durationMillis = 980, easing = FastOutSlowInEasing)
            coroutineScope.launch { peakHeadPitch.animateTo(0f, headReturnSpec) }
            coroutineScope.launch { peakHeadTurn.animateTo(0f, headReturnSpec) }
            coroutineScope.launch { peakHeadRoll.animateTo(0f, headReturnSpec) }
            coroutineScope.launch { peakLegShakeL.animateTo(0f, shakeStopSpec) }
            coroutineScope.launch { peakLegShakeR.animateTo(0f, shakeStopSpec) }
            coroutineScope.launch { peakMouthOpen.animateTo(0f, mouthCloseSpec) }
            if (!latestPeakPreEventActive) {
                coroutineScope.launch { peakKneeShiftL.animateTo(0f, extremRelaxSpec) }
                coroutineScope.launch { peakKneeShiftR.animateTo(0f, extremRelaxSpec) }
                coroutineScope.launch { peakToeWiggleL.animateTo(0f, extremRelaxSpec) }
                coroutineScope.launch { peakToeWiggleR.animateTo(0f, extremRelaxSpec) }
                coroutineScope.launch { peakArmShiftL.animateTo(0f, extremRelaxSpec) }
                coroutineScope.launch { peakArmShiftR.animateTo(0f, extremRelaxSpec) }
                coroutineScope.launch { peakFingerCurlL.animateTo(0f, extremRelaxSpec) }
                coroutineScope.launch { peakFingerCurlR.animateTo(0f, extremRelaxSpec) }
            }
            if (wasPeakEpisodeActive) {
                wasPeakEpisodeActive = false
                // Ensure eyelids open cleanly and legs relax smoothly back toward comfortable default stance
                val eyeOpenSpec = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
                val legRelaxSpec = tween<Float>(durationMillis = 1950, easing = FastOutSlowInEasing)
                coroutineScope.launch { eyeBlink.animateTo(0f, eyeOpenSpec) }
                coroutineScope.launch { eyeBlinkR.animateTo(0f, eyeOpenSpec) }
                coroutineScope.launch { leftLegSpread.animateTo(0f, legRelaxSpec) }
                coroutineScope.launch { rightLegSpread.animateTo(0f, legRelaxSpec) }
            }
            return@LaunchedEffect
        }

        wasPeakEpisodeActive = true
        val startMs = System.currentTimeMillis()
        val endMs = peakEpisodeEndMs.coerceAtLeast(startMs + 1_000L)
        val headRng = kotlin.random.Random(startMs xor 0x41B2C3D4L)
        val legLRng = kotlin.random.Random(startMs xor 0x19F8E7D6L)
        val legRRng = kotlin.random.Random(startMs xor 0x72A4B6C8L)
        val legDriftLRng = kotlin.random.Random(startMs xor 0x3A17F429L)
        val legDriftRRng = kotlin.random.Random(startMs xor 0x6E82C51BL)
        val blinkRng = kotlin.random.Random(startMs xor 0x55C3E1A9L)
        val vibRng = kotlin.random.Random(startMs xor 0x8D3F1E27L)
        val mouthRng = kotlin.random.Random(startMs xor 0x29A7C4F1L)
        val extremLRng = kotlin.random.Random(startMs xor 0x61C2E3F4L)
        val extremRRng = kotlin.random.Random(startMs xor 0x94D5A6B7L)

        // 0. High-intensity, high-frequency, slightly irregular pulsing phone vibration for the full 16s
        val pulseVibrationJob = launch {
            try {
                while (System.currentTimeMillis() < endMs) {
                    val remaining = (endMs - System.currentTimeMillis()).coerceAtLeast(0L)
                    if (remaining <= 30L) break
                    if (!latestHapticsEnabled) {
                        hapticsEngine.cancel()
                        delay(min(100L, remaining))
                        continue
                    }
                    val pulseCount = 6 + vibRng.nextInt(5)
                    val timingsList = ArrayList<Long>(pulseCount * 2)
                    val ampsList = ArrayList<Int>(pulseCount * 2)
                    var chunkMs = 0L
                    for (p in 0 until pulseCount) {
                        val onMs = (26 + vibRng.nextInt(28)).toLong()
                        val offMs = when {
                            vibRng.nextFloat() < 0.22f -> (14 + vibRng.nextInt(14)).toLong()
                            vibRng.nextFloat() < 0.16f -> (58 + vibRng.nextInt(32)).toLong()
                            else -> (22 + vibRng.nextInt(26)).toLong()
                        }
                        if (chunkMs + onMs > remaining) break
                        val amp = 212 + vibRng.nextInt(44)
                        timingsList.add(onMs)
                        ampsList.add(amp)
                        chunkMs += onMs
                        if (chunkMs + offMs <= remaining) {
                            timingsList.add(offMs)
                            ampsList.add(0)
                            chunkMs += offMs
                        }
                    }
                    if (timingsList.isEmpty()) break
                    hapticsEngine.playIrregularPeakWaveform(
                        timings = timingsList.toLongArray(),
                        amplitudes = ampsList.toIntArray()
                    )
                    delay((chunkMs - 10L).coerceAtLeast(25L))
                }
            } finally {
                hapticsEngine.cancel()
            }
        }

        // 1. Legs (40% slower = 1.67x durations):
        //    Slam thighs tightly squeezed together -> slowly part open to default (0f) ->
        //    mix planted-foot knee/upper-leg shifting with independent whole-leg sweeps & shaking
        val legsJob = launch {
            val slamDurL = 290
            val slamDurR = 320
            val jCloseL = launch { leftLegSpread.animateTo(PEAK_LEG_SLAM_SHUT_TARGET, tween(slamDurL, easing = FastOutSlowInEasing)) }
            val jCloseR = launch { rightLegSpread.animateTo(PEAK_LEG_SLAM_SHUT_TARGET, tween(slamDurR, easing = FastOutSlowInEasing)) }
            val jSqueezeL = launch { peakKneeShiftL.animateTo(PEAK_KNEE_SLAM_SQUEEZE, tween(slamDurL + 25, easing = FastOutSlowInEasing)) }
            val jSqueezeR = launch { peakKneeShiftR.animateTo(PEAK_KNEE_SLAM_SQUEEZE, tween(slamDurR + 25, easing = FastOutSlowInEasing)) }
            val jClearL = launch { settleLegL.animateTo(0f, tween(140)) }
            val jClearR = launch { settleLegR.animateTo(0f, tween(140)) }
            jCloseL.join(); jCloseR.join(); jSqueezeL.join(); jSqueezeR.join(); jClearL.join(); jClearR.join()

            // Brief tense thigh-clinch hold (~260ms) before parting open
            delay(260L)

            // Slowly part open to default position (0f) over ~1.25 seconds
            if (System.currentTimeMillis() < endMs) {
                val partDur = min(PEAK_LEG_PART_OPEN_DURATION_MS.toLong(), (endMs - System.currentTimeMillis()).coerceAtLeast(220L)).toInt()
                val jOpenL = launch { leftLegSpread.animateTo(0f, tween(partDur, easing = FastOutSlowInEasing)) }
                val jOpenR = launch { rightLegSpread.animateTo(0f, tween(partDur, easing = FastOutSlowInEasing)) }
                val jKneeOpenL = launch { peakKneeShiftL.animateTo(0f, tween(partDur, easing = FastOutSlowInEasing)) }
                val jKneeOpenR = launch { peakKneeShiftR.animateTo(0f, tween(partDur, easing = FastOutSlowInEasing)) }
                jOpenL.join(); jOpenR.join(); jKneeOpenL.join(); jKneeOpenR.join()
            }

            var leftFootPlanted = false
            var rightFootPlanted = false

            // Independent Left Leg: alternates between Planted-Foot Knee Shifting and Whole-Leg Sweeps (40% slower)
            val leftDriftJob = launch {
                var sideSign = if (legDriftLRng.nextBoolean()) 1f else -1f
                var firstPlantedPass = true
                while (System.currentTimeMillis() < endMs) {
                    val usePlantedKneeMode = firstPlantedPass || legDriftLRng.nextFloat() < 0.50f
                    firstPlantedPass = false
                    if (usePlantedKneeMode) {
                        // Plant foot steadily while knee & upper leg shift back and forth (including inward thigh press)
                        leftFootPlanted = true
                        val plantedFootSpread = (-0.18f + legDriftLRng.nextFloat() * 0.56f)
                        leftLegSpread.animateTo(plantedFootSpread, tween(420 + legDriftLRng.nextInt(260), easing = FastOutSlowInEasing))
                        val kneeCycles = 2 + legDriftLRng.nextInt(3)
                        for (c in 0 until kneeCycles) {
                            if (System.currentTimeMillis() >= endMs) break
                            sideSign = -sideSign
                            val kneeTarget = if (sideSign < 0f) {
                                -(0.45f + legDriftLRng.nextFloat() * 0.62f) // deep inward thigh-squeeze press (-0.45..-1.07)
                            } else {
                                0.32f + legDriftLRng.nextFloat() * 0.54f // outward knee roll (+0.32..+0.86)
                            }
                            val kneeDur = 420 + legDriftLRng.nextInt(460)
                            peakKneeShiftL.animateTo(kneeTarget, tween(kneeDur, easing = FastOutSlowInEasing))
                            if (legDriftLRng.nextFloat() < 0.34f && System.currentTimeMillis() < endMs) {
                                delay((95 + legDriftLRng.nextInt(185)).toLong())
                            }
                        }
                        leftFootPlanted = false
                    } else {
                        // Whole-leg spread sweep (40% slower: 400..1100ms) with organic knee follow-through
                        sideSign = if (legDriftLRng.nextFloat() < 0.72f) -sideSign else sideSign
                        val rawTarget = if (sideSign > 0f) {
                            0.18f + legDriftLRng.nextFloat() * 0.64f
                        } else {
                            -(0.18f + legDriftLRng.nextFloat() * 0.58f)
                        }
                        val kneeFollow = if (sideSign < 0f) {
                            -(0.32f + legDriftLRng.nextFloat() * 0.68f) // natural inward thigh clinch
                        } else {
                            -0.12f + legDriftLRng.nextFloat() * 0.48f
                        }
                        val moveDur = 400 + legDriftLRng.nextInt(700)
                        val jSpread = launch { leftLegSpread.animateTo(rawTarget, tween(moveDur, easing = FastOutSlowInEasing)) }
                        val jKnee = launch { peakKneeShiftL.animateTo(kneeFollow, tween(moveDur + 60, easing = FastOutSlowInEasing)) }
                        jSpread.join(); jKnee.join()
                        if (legDriftLRng.nextFloat() < 0.36f && System.currentTimeMillis() < endMs) {
                            delay((95 + legDriftLRng.nextInt(230)).toLong())
                        }
                    }
                }
            }

            // Independent Right Leg: staggered phase alternating Planted-Foot Knee Shifting and Whole-Leg Sweeps (40% slower)
            val rightDriftJob = launch {
                delay((185 + legDriftRRng.nextInt(240)).toLong())
                var sideSign = if (legDriftRRng.nextBoolean()) 1f else -1f
                while (System.currentTimeMillis() < endMs) {
                    val usePlantedKneeMode = legDriftRRng.nextFloat() < 0.50f
                    if (usePlantedKneeMode) {
                        rightFootPlanted = true
                        val plantedFootSpread = (-0.18f + legDriftRRng.nextFloat() * 0.56f)
                        rightLegSpread.animateTo(plantedFootSpread, tween(410 + legDriftRRng.nextInt(270), easing = FastOutSlowInEasing))
                        val kneeCycles = 2 + legDriftRRng.nextInt(3)
                        for (c in 0 until kneeCycles) {
                            if (System.currentTimeMillis() >= endMs) break
                            sideSign = -sideSign
                            val kneeTarget = if (sideSign < 0f) {
                                -(0.45f + legDriftRRng.nextFloat() * 0.62f)
                            } else {
                                0.32f + legDriftRRng.nextFloat() * 0.54f
                            }
                            val kneeDur = 410 + legDriftRRng.nextInt(480)
                            peakKneeShiftR.animateTo(kneeTarget, tween(kneeDur, easing = FastOutSlowInEasing))
                            if (legDriftRRng.nextFloat() < 0.34f && System.currentTimeMillis() < endMs) {
                                delay((95 + legDriftRRng.nextInt(190)).toLong())
                            }
                        }
                        rightFootPlanted = false
                    } else {
                        sideSign = if (legDriftRRng.nextFloat() < 0.72f) -sideSign else sideSign
                        val rawTarget = if (sideSign > 0f) {
                            0.18f + legDriftRRng.nextFloat() * 0.64f
                        } else {
                            -(0.18f + legDriftRRng.nextFloat() * 0.58f)
                        }
                        val kneeFollow = if (sideSign < 0f) {
                            -(0.32f + legDriftRRng.nextFloat() * 0.68f)
                        } else {
                            -0.12f + legDriftRRng.nextFloat() * 0.48f
                        }
                        val moveDur = 390 + legDriftRRng.nextInt(710)
                        val jSpread = launch { rightLegSpread.animateTo(rawTarget, tween(moveDur, easing = FastOutSlowInEasing)) }
                        val jKnee = launch { peakKneeShiftR.animateTo(kneeFollow, tween(moveDur + 60, easing = FastOutSlowInEasing)) }
                        jSpread.join(); jKnee.join()
                        if (legDriftRRng.nextFloat() < 0.36f && System.currentTimeMillis() < endMs) {
                            delay((90 + legDriftRRng.nextInt(235)).toLong())
                        }
                    }
                }
            }

            // Independent leg tremor/shakes (40% slower: 75..205ms; attenuated while foot is planted!)
            val leftShakeJob = launch {
                while (System.currentTimeMillis() < endMs) {
                    val plantedScale = if (leftFootPlanted) 0.14f else 1.0f
                    val burstDir = if (legLRng.nextFloat() < 0.56f) 1f else -1f
                    val burstAmp = burstDir * (0.090f + legLRng.nextFloat() * 0.155f) * plantedScale
                    val upDur = 76 + legLRng.nextInt(120)
                    val downDur = 86 + legLRng.nextInt(135)
                    peakLegShakeL.animateTo(burstAmp, tween(upDur, easing = FastOutSlowInEasing))
                    val rebound = -burstDir * (0.042f + legLRng.nextFloat() * 0.105f) * plantedScale
                    peakLegShakeL.animateTo(rebound, tween(downDur, easing = FastOutSlowInEasing))
                    if (legLRng.nextFloat() < 0.26f) {
                        delay((58 + legLRng.nextInt(140)).toLong())
                    }
                }
            }
            val rightShakeJob = launch {
                delay((54 + legRRng.nextInt(108)).toLong())
                while (System.currentTimeMillis() < endMs) {
                    val plantedScale = if (rightFootPlanted) 0.14f else 1.0f
                    val burstDir = if (legRRng.nextFloat() < 0.56f) 1f else -1f
                    val burstAmp = burstDir * (0.090f + legRRng.nextFloat() * 0.155f) * plantedScale
                    val upDur = 74 + legRRng.nextInt(124)
                    val downDur = 84 + legRRng.nextInt(140)
                    peakLegShakeR.animateTo(burstAmp, tween(upDur, easing = FastOutSlowInEasing))
                    val rebound = -burstDir * (0.042f + legRRng.nextFloat() * 0.105f) * plantedScale
                    peakLegShakeR.animateTo(rebound, tween(downDur, easing = FastOutSlowInEasing))
                    if (legRRng.nextFloat() < 0.26f) {
                        delay((54 + legRRng.nextInt(148)).toLong())
                    }
                }
            }
            leftDriftJob.join()
            rightDriftJob.join()
            leftShakeJob.join()
            rightShakeJob.join()
        }

        // 2. Head (60% slower = 2.5x durations): deep downward dip, then slow dramatic lean-back with
        //    large organic pitch heaves, wide irregular side-to-side head turns, and pronounced rolls
        val headPitchJob = launch {
            launch { headPitch.animateTo(0f, tween(360, easing = FastOutSlowInEasing)) }
            launch { idleHeadPitch.animateTo(0f, tween(360, easing = FastOutSlowInEasing)) }
            // Dip head down (+1.0f) as thighs squeeze shut (2.5x slower than 210ms)
            peakHeadPitch.animateTo(1.0f, tween(520, easing = FastOutSlowInEasing))
            delay(950L)
            if (System.currentTimeMillis() < endMs) {
                // Throw head deeply back (-1.0f) in 920ms, then execute slow, expressive pitch arcs
                peakHeadPitch.animateTo(-1.0f, tween(920, easing = FastOutSlowInEasing))
                while (System.currentTimeMillis() < endMs) {
                    val heaveTarget = if (headRng.nextFloat() < 0.34f) {
                        -(0.42f + headRng.nextFloat() * 0.26f)
                    } else {
                        -(0.82f + headRng.nextFloat() * 0.18f)
                    }
                    peakHeadPitch.animateTo(heaveTarget, tween(440 + headRng.nextInt(580), easing = FastOutSlowInEasing))
                    if (headRng.nextFloat() < 0.28f && System.currentTimeMillis() < endMs) {
                        delay((105 + headRng.nextInt(235)).toLong())
                    }
                }
            }
        }
        val headSideJob = launch {
            var sideSign = if (headRng.nextBoolean()) 1f else -1f
            while (System.currentTimeMillis() < endMs) {
                sideSign = if (headRng.nextFloat() < 0.74f) -sideSign else sideSign
                val turnTarget = sideSign * (0.28f + headRng.nextFloat() * 0.44f)
                val rollTarget = -sideSign * (0.070f + headRng.nextFloat() * 0.100f)
                val moveDur = 280 + headRng.nextInt(380) // 60% slower (2.5x of 110..265ms)
                val jT = launch { peakHeadTurn.animateTo(turnTarget, tween(moveDur, easing = FastOutSlowInEasing)) }
                val jR = launch { peakHeadRoll.animateTo(rollTarget, tween(moveDur + 45, easing = FastOutSlowInEasing)) }
                jT.join(); jR.join()
                if (headRng.nextFloat() < 0.32f && System.currentTimeMillis() < endMs) {
                    val jitterTurn = turnTarget * (0.58f + headRng.nextFloat() * 0.26f)
                    peakHeadTurn.animateTo(jitterTurn, tween(195 + headRng.nextInt(135), easing = FastOutSlowInEasing))
                }
                delay((75 + headRng.nextInt(185)).toLong())
            }
        }

        // 3. Eyelids: close tightly for 4 full seconds at start, then open back up into dramatic
        //    rapid irregular peak blinking & flutter bursts for the remaining ~12 seconds
        val peakBlinkJob = launch {
            suspend fun runPeakBlink(amp: Float, closeMs: Int, holdMs: Long, openMs: Int) {
                val asym = latestBetaLife2AsymmetricBlinking
                if (!asym) {
                    eyeBlink.animateTo(amp, tween(closeMs, easing = FastOutSlowInEasing))
                    delay(holdMs)
                    eyeBlink.animateTo(0f, tween(openMs, easing = FastOutSlowInEasing))
                    if (eyeBlinkR.value > 0.001f) eyeBlinkR.snapTo(0f)
                    return
                }
                val leadLeft = blinkRng.nextFloat() < 0.54f
                val staggerMs = (8 + blinkRng.nextInt(12)).toLong()
                val jL = launch {
                    if (!leadLeft) delay(staggerMs)
                    eyeBlink.animateTo(amp, tween(closeMs, easing = FastOutSlowInEasing))
                    delay(holdMs)
                    eyeBlink.animateTo(0f, tween(openMs, easing = FastOutSlowInEasing))
                }
                val jR = launch {
                    if (leadLeft) delay(staggerMs)
                    eyeBlinkR.animateTo(amp, tween(closeMs, easing = FastOutSlowInEasing))
                    delay(holdMs)
                    eyeBlinkR.animateTo(0f, tween(openMs, easing = FastOutSlowInEasing))
                }
                jL.join(); jR.join()
            }

            // Sustained 4-second full eyelid closure right at start of the 16s peak event
            val closeL = launch { eyeBlink.animateTo(1f, tween(115, easing = FastOutSlowInEasing)) }
            val closeR = launch { eyeBlinkR.animateTo(1f, tween(122, easing = FastOutSlowInEasing)) }
            closeL.join(); closeR.join()
            val fourSecHold = PEAK_EYELID_CLOSE_DURATION_MS.coerceAtMost((endMs - System.currentTimeMillis()).coerceAtLeast(250L))
            delay(fourSecHold)
            val openL = launch { eyeBlink.animateTo(0f, tween(230, easing = FastOutSlowInEasing)) }
            val openR = launch { eyeBlinkR.animateTo(0f, tween(245, easing = FastOutSlowInEasing)) }
            openL.join(); openR.join()

            var extraHoldCount = 0
            while (System.currentTimeMillis() < endMs) {
                if (extraHoldCount < 2 && blinkRng.nextFloat() < 0.18f) {
                    extraHoldCount++
                    val shutMs = (680 + blinkRng.nextInt(580)).toLong()
                    val clampedShutMs = shutMs.coerceAtMost((endMs - System.currentTimeMillis()).coerceAtLeast(180L))
                    runPeakBlink(amp = 1f, closeMs = 70, holdMs = clampedShutMs, openMs = 155)
                } else {
                    val closeMs = 46 + blinkRng.nextInt(22)
                    val holdMs = (14 + blinkRng.nextInt(18)).toLong()
                    val openMs = 82 + blinkRng.nextInt(36)
                    runPeakBlink(1f, closeMs, holdMs, openMs)
                    if (blinkRng.nextFloat() < 0.80f && System.currentTimeMillis() < endMs) {
                        delay((60 + blinkRng.nextInt(55)).toLong())
                        runPeakBlink(0.94f + blinkRng.nextFloat() * 0.06f, 44 + blinkRng.nextInt(18), 14L + blinkRng.nextInt(16), 82 + blinkRng.nextInt(32))
                        if (blinkRng.nextFloat() < 0.35f && System.currentTimeMillis() < endMs) {
                            delay((55 + blinkRng.nextInt(45)).toLong())
                            runPeakBlink(0.88f + blinkRng.nextFloat() * 0.12f, 44 + blinkRng.nextInt(16), 14L + blinkRng.nextInt(14), 84 + blinkRng.nextInt(30))
                        }
                    }
                }
                delay((135 + blinkRng.nextInt(225)).toLong())
            }
        }

        // 4. Mouth (Bonus #2): smoothly opens right at the start of the 16-second event and modulates
        //    organically with breath/gasp waves before softening cleanly at the end
        val mouthJob = launch {
            peakMouthOpen.animateTo(0.94f, tween(340, easing = FastOutSlowInEasing))
            delay(480L)
            while (System.currentTimeMillis() < endMs) {
                val remainFrac = ((endMs - System.currentTimeMillis()).toFloat() / PEAK_EPISODE_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                val targetOpen = if (remainFrac < 0.12f) {
                    0.35f + mouthRng.nextFloat() * 0.25f
                } else {
                    0.62f + mouthRng.nextFloat() * 0.38f
                }
                peakMouthOpen.animateTo(targetOpen, tween(540 + mouthRng.nextInt(560), easing = FastOutSlowInEasing))
                if (mouthRng.nextFloat() < 0.30f && System.currentTimeMillis() < endMs) {
                    delay((140 + mouthRng.nextInt(260)).toLong())
                }
            }
        }

        // 5. Subtle Extremities (Bonus #3): independent toe wiggling, subtle finger curling, and gentle arm shifting
        val extremitiesJob = launch {
            val toesLJob = launch {
                while (System.currentTimeMillis() < endMs) {
                    val target = -0.68f + extremLRng.nextFloat() * 1.46f // -0.68..+0.78
                    peakToeWiggleL.animateTo(target, tween(290 + extremLRng.nextInt(380), easing = FastOutSlowInEasing))
                    if (extremLRng.nextFloat() < 0.32f && System.currentTimeMillis() < endMs) {
                        delay((90 + extremLRng.nextInt(170)).toLong())
                    }
                }
            }
            val toesRJob = launch {
                delay((120 + extremRRng.nextInt(180)).toLong())
                while (System.currentTimeMillis() < endMs) {
                    val target = -0.68f + extremRRng.nextFloat() * 1.46f
                    peakToeWiggleR.animateTo(target, tween(280 + extremRRng.nextInt(400), easing = FastOutSlowInEasing))
                    if (extremRRng.nextFloat() < 0.32f && System.currentTimeMillis() < endMs) {
                        delay((85 + extremRRng.nextInt(175)).toLong())
                    }
                }
            }
            val fingersLJob = launch {
                while (System.currentTimeMillis() < endMs) {
                    val target = 0.18f + extremLRng.nextFloat() * 0.66f // subtle curl/grip 0.18..0.84
                    peakFingerCurlL.animateTo(target, tween(460 + extremLRng.nextInt(520), easing = FastOutSlowInEasing))
                    if (extremLRng.nextFloat() < 0.35f && System.currentTimeMillis() < endMs) {
                        delay((150 + extremLRng.nextInt(240)).toLong())
                    }
                }
            }
            val fingersRJob = launch {
                delay((175 + extremRRng.nextInt(220)).toLong())
                while (System.currentTimeMillis() < endMs) {
                    val target = 0.18f + extremRRng.nextFloat() * 0.66f
                    peakFingerCurlR.animateTo(target, tween(450 + extremRRng.nextInt(540), easing = FastOutSlowInEasing))
                    if (extremRRng.nextFloat() < 0.35f && System.currentTimeMillis() < endMs) {
                        delay((140 + extremRRng.nextInt(250)).toLong())
                    }
                }
            }
            val armsLJob = launch {
                while (System.currentTimeMillis() < endMs) {
                    val target = -0.52f + extremLRng.nextFloat() * 1.14f // subtle arm shift -0.52..+0.62
                    peakArmShiftL.animateTo(target, tween(760 + extremLRng.nextInt(720), easing = FastOutSlowInEasing))
                    if (extremLRng.nextFloat() < 0.40f && System.currentTimeMillis() < endMs) {
                        delay((210 + extremLRng.nextInt(320)).toLong())
                    }
                }
            }
            val armsRJob = launch {
                delay((260 + extremRRng.nextInt(320)).toLong())
                while (System.currentTimeMillis() < endMs) {
                    val target = -0.52f + extremRRng.nextFloat() * 1.14f
                    peakArmShiftR.animateTo(target, tween(740 + extremRRng.nextInt(750), easing = FastOutSlowInEasing))
                    if (extremRRng.nextFloat() < 0.40f && System.currentTimeMillis() < endMs) {
                        delay((200 + extremRRng.nextInt(330)).toLong())
                    }
                }
            }
            toesLJob.join(); toesRJob.join()
            fingersLJob.join(); fingersRJob.join()
            armsLJob.join(); armsRJob.join()
        }

        pulseVibrationJob.join()
        legsJob.join()
        headPitchJob.join()
        headSideJob.join()
        peakBlinkJob.join()
        mouthJob.join()
        extremitiesJob.join()
    }

    // Beat-synchronized pulse for the optional top-left Heart Rate HUD badge, switching to a slow,
    // glowing green pulsation across the entire pill during the 7-second pre-event delay
    LaunchedEffect(showExperimentalLegs, betaLife2ShowHeartRate, peakPreEventActive) {
        if (!showExperimentalLegs || !betaLife2ShowHeartRate) {
            heartPulseScale.snapTo(1f)
            heartPillPulseScale.snapTo(1f)
            return@LaunchedEffect
        }
        if (peakPreEventActive) {
            while (true) {
                val upPill = launch { heartPillPulseScale.animateTo(1.13f, tween(680, easing = FastOutSlowInEasing)) }
                val upHeart = launch { heartPulseScale.animateTo(1.24f, tween(680, easing = FastOutSlowInEasing)) }
                upPill.join(); upHeart.join()
                val downPill = launch { heartPillPulseScale.animateTo(0.98f, tween(680, easing = FastOutSlowInEasing)) }
                val downHeart = launch { heartPulseScale.animateTo(0.96f, tween(680, easing = FastOutSlowInEasing)) }
                downPill.join(); downHeart.join()
            }
        }
        heartPillPulseScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        while (true) {
            val bpm = latestHeartRateBpm.coerceIn(
                VulvaGlTextureView.RESTING_HEART_RATE_BPM,
                VulvaGlTextureView.PEAK_MAX_HEART_RATE_BPM
            )
            val beatPeriodMs = (60_000f / bpm).roundToInt().coerceIn(360, 1050)
            val hrNorm = VulvaGlTextureView.heartRateExertionNorm(bpm)
            val systoleScale = 1.12f + 0.09f * hrNorm
            val systoleMs = (beatPeriodMs * 0.18f).roundToInt().coerceIn(70, 160)
            val diastoleMs = (beatPeriodMs * 0.28f).roundToInt().coerceIn(105, 240)
            heartPulseScale.animateTo(systoleScale, tween(systoleMs, easing = FastOutSlowInEasing))
            heartPulseScale.animateTo(1.0f, tween(diastoleMs, easing = FastOutSlowInEasing))
            val pauseMs = (beatPeriodMs - systoleMs - diastoleMs).coerceAtLeast(80).toLong()
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
            // v21.0: 50% tighter L/R asymmetric stagger (8–19ms instead of 16–38ms)
            val staggerMs = (8 + rng.nextInt(12)).toLong()
            val closeJitter = (rng.nextInt(11) - 5)
            val openJitter = (rng.nextInt(13) - 6)
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
            // Yield cleanly to the dedicated 10-second peak blink choreography while active
            if (latestPeakEpisodeActive) {
                delay(120L)
                continue
            }
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
                if (latestPeakEpisodeActive) break
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
            if (!faceCouldBeVisible || recentlyDragged || latestPeakEpisodeActive) {
                delay(350L)
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
        if (!showExperimentalLegs || !betaLife2PosturalSettling || posturalSettleTrigger <= sessionState.lastHandledPosturalSettleTrigger) {
            return@LaunchedEffect
        }
        sessionState.lastHandledPosturalSettleTrigger = posturalSettleTrigger
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

    val activeLegMinSpread = if (
        peakPreEventActive || peakEpisodeActive ||
        leftLegSpread.value < BlossomSdfBodyBuilder.MANUAL_MIN_LEG_SPREAD ||
        rightLegSpread.value < BlossomSdfBodyBuilder.MANUAL_MIN_LEG_SPREAD
    ) {
        BlossomSdfBodyBuilder.PEAK_MIN_LEG_SPREAD
    } else {
        BlossomSdfBodyBuilder.MANUAL_MIN_LEG_SPREAD
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
        leftLegSpread = (leftLegSpread.value + settleLegL.value + peakLegShakeL.value).coerceIn(activeLegMinSpread, 1f),
        rightLegSpread = (rightLegSpread.value + settleLegR.value + peakLegShakeR.value).coerceIn(activeLegMinSpread, 1f),
        selfSpreadPose = selfSpreadPose.value,
        headTurn = (headTurn.value + idleHeadTurn.value + settleHeadTurn.value + peakHeadTurn.value).coerceIn(-1f, 1f),
        headPitch = (headPitch.value + idleHeadPitch.value + settleHeadPitch.value + peakHeadPitch.value).coerceIn(-1f, 1f),
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
        betaV3LabialVestibuleBlend = betaV3LabialVestibuleBlend,
        betaV3LabialBlendStrength = betaV3LabialBlendStrength,
        betaV3IntricateLabiaFolds = betaV3IntricateLabiaFolds,
        betaV3LabiaFoldsStrength = betaV3LabiaFoldsStrength,
        betaV3MedialRootContinuity = betaV3MedialRootContinuity,
        betaV3MedialRootStrength = betaV3MedialRootStrength,
        betaBreastSizeBoost = activeBreastSizeBoost,
        eyeGazeX = (eyeGazeX.value + anticipationGazeX.value).coerceIn(-1f, 1f),
        eyeGazeY = (eyeGazeY.value + anticipationGazeY.value).coerceIn(-1f, 1f),
        headRollOffset = idleHeadRoll.value + settleHeadRoll.value + peakHeadRoll.value,
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
        livingWarmth = livingWarmth.value,
        leftKneeShift = peakKneeShiftL.value,
        rightKneeShift = peakKneeShiftR.value,
        leftToeWiggle = peakToeWiggleL.value,
        rightToeWiggle = peakToeWiggleR.value,
        leftArmShift = peakArmShiftL.value,
        rightArmShift = peakArmShiftR.value,
        leftFingerCurl = peakFingerCurlL.value,
        rightFingerCurl = peakFingerCurlR.value,
        mouthOpen = peakMouthOpen.value
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
                            Lifecycle.Event.ON_PAUSE -> {
                                hapticsEngine.cancel()
                                glViewRef?.setRenderingPaused(true)
                            }
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
                        ).apply {
                            breathPhase = sessionState.breathPhase
                        }.also { glViewRef = it }
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
                        glView.peakBreathActive = peakEpisodeActive
                        glView.postPeakSighToken = postPeakSighToken
                        glView.postPeakSighCount = postPeakSighCount
                        glView.requestFrame()
                    },
                    onRelease = { glView ->
                        sessionState.breathPhase = glView.breathPhase
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
                                        activePresetOrdinal = null
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
                                            betaBreastSizeBoost = latestBetaBreastSizeBoost,
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

                                                        val elasticRes = latestBetaVulvaElasticity.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
                                                        val pullScale = vulvaElasticPullScale(elasticRes)
                                                        val hoodPullScale = 1f - elasticRes * 0.65f
                                                        val inHoodZone = existing.startNorm.x in 0.28f..0.72f &&
                                                            existing.startNorm.y in 0.14f..0.46f
                                                        if (inHoodZone && abs(dyNorm) > abs(dxNorm) * 0.55f) {
                                                            dHood += (-dyNorm) * 4.5f * hoodPullScale
                                                        }

                                                        val inLabiaSpan = existing.startNorm.y in 0.18f..0.88f
                                                        if (inLabiaSpan) {
                                                            if (existing.startNorm.x < 0.50f) {
                                                                val pull = -dxNorm
                                                                val nearInner = existing.startNorm.x in 0.36f..0.50f
                                                                dLeftOuter += pull * (if (nearInner) 2.2f else 3.9f) * pullScale
                                                                dLeftInner += pull * (if (nearInner) 4.5f else 3.0f) * pullScale
                                                            } else {
                                                                val pull = dxNorm
                                                                val nearInner = existing.startNorm.x in 0.50f..0.64f
                                                                dRightOuter += pull * (if (nearInner) 2.2f else 3.9f) * pullScale
                                                                dRightInner += pull * (if (nearInner) 4.5f else 3.0f) * pullScale
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
                                            val elasticRes = latestBetaVulvaElasticity.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
                                            val maxSpread = vulvaElasticMaxSpread(elasticRes)
                                            val maxHood = vulvaElasticHoodMax(elasticRes)
                                            val nextLO = vulvaElasticStep(leftOuterSpread.value, dLeftOuter, maxSpread, elasticRes)
                                            val nextRO = vulvaElasticStep(rightOuterSpread.value, dRightOuter, maxSpread, elasticRes)
                                            val minLI = (nextLO - 0.16f).coerceAtLeast(0f) * 0.72f
                                            val minRI = (nextRO - 0.16f).coerceAtLeast(0f) * 0.72f
                                            val nextLI = vulvaElasticStep(leftInnerOpen.value, dLeftInner, maxSpread, elasticRes)
                                                .coerceAtLeast(minLI)
                                            val nextRI = vulvaElasticStep(rightInnerOpen.value, dRightInner, maxSpread, elasticRes)
                                                .coerceAtLeast(minRI)
                                            val nextH = vulvaElasticStep(hoodRetraction.value, dHood, maxHood, elasticRes)

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

                                        if ((dLeftLeg != 0f || dRightLeg != 0f) && !latestPeakPreEventActive && !latestPeakEpisodeActive) {
                                            val nextLL = (leftLegSpread.value + dLeftLeg).coerceIn(BlossomSdfBodyBuilder.MANUAL_MIN_LEG_SPREAD, 1f)
                                            val nextRL = (rightLegSpread.value + dRightLeg).coerceIn(BlossomSdfBodyBuilder.MANUAL_MIN_LEG_SPREAD, 1f)
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
                    val preGreenPulseFrac = ((heartPillPulseScale.value - 0.98f) / 0.15f).coerceIn(0f, 1f)
                    val heartTint = if (peakPreEventActive) {
                        Color(
                            red = 0.20f + 0.14f * preGreenPulseFrac,
                            green = 0.88f + 0.12f * preGreenPulseFrac,
                            blue = 0.48f + 0.16f * preGreenPulseFrac,
                            alpha = 1.0f
                        )
                    } else {
                        Color(
                            red = 1.0f,
                            green = (0.48f - 0.18f * hrNorm).coerceIn(0.24f, 0.55f),
                            blue = (0.56f - 0.24f * hrNorm).coerceIn(0.26f, 0.60f),
                            alpha = 1.0f
                        )
                    }
                    val pillBackground = if (peakPreEventActive) {
                        Color(0xFF0E2B1C).copy(alpha = 0.84f + 0.10f * preGreenPulseFrac)
                    } else {
                        Color(0xFF1D1416).copy(alpha = 0.80f)
                    }
                    val pillBorderColor = if (peakPreEventActive) {
                        heartTint.copy(alpha = 0.55f + 0.42f * preGreenPulseFrac)
                    } else {
                        heartTint.copy(alpha = 0.32f + 0.38f * hrNorm)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            modifier = Modifier
                                .scale(heartPillPulseScale.value)
                                .testTag("heart_rate_hud_pill")
                                .semantics {
                                    contentDescription = "Heart rate $bpmInt beats per minute"
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = pillBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (peakPreEventActive) 1.5.dp else 1.dp,
                                color = pillBorderColor
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
                                    color = if (peakPreEventActive) Color(0xFFE6FFF1) else Color.White
                                )
                                Text(
                                    text = "BPM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (peakPreEventActive) Color(0xFF8CFFB8) else Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }

                        val sequenceRunning = peakPreEventActive || peakEpisodeActive
                        Surface(
                            onClick = {
                                if (latestHapticsEnabled) {
                                    composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                peakPreEventActive = false
                                peakEpisodeActive = false
                                peakPostCooldownActive = false
                                peakRecoveryFastEndMs = 0L
                                peakRecoveryHoldUntilMs = 0L
                                peakRefractoryUntilMs = 0L
                                heartRateBpm = PEAK_EPISODE_TRIGGER_BPM
                            },
                            modifier = Modifier
                                .testTag("trigger_peak_hr_button")
                                .semantics {
                                    contentDescription = "Set heart rate to 136 BPM to test peak sequence"
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (sequenceRunning) {
                                Color(0xFF103322).copy(alpha = 0.84f)
                            } else {
                                Color(0xFF26181C).copy(alpha = 0.82f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (sequenceRunning) {
                                    Color(0xFF56F09B).copy(alpha = 0.62f)
                                } else {
                                    Color(0xFFFFB74D).copy(alpha = 0.52f)
                                }
                            ),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = if (sequenceRunning) Color(0xFF69F0AE) else Color(0xFFFFCA68),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (sequenceRunning) "Restart 136" else "Trigger 136",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sequenceRunning) Color(0xFFD9FFE8) else Color(0xFFFFE6B8)
                                )
                            }
                        }
                    }
                }

                // Always-on-top Floating Hamburger Beta Menu (Vulva + Life + Life+)
                val activeVulvaBetaCount = listOf(
                    betaV3OrganicContinuity,
                    betaV3SoftPigmentation,
                    betaV3AnatomicalAsymmetry,
                    betaV3RegionalSurfaceDetail,
                    betaV3LabialVestibuleBlend,
                    betaV3IntricateLabiaFolds,
                    betaV3MedialRootContinuity,
                    betaVulvaElasticityEnabled && betaVulvaElasticity > 0.005f,
                    betaBreastSizeBoostEnabled && betaBreastSizeBoost > 0.005f
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

                // Translucent Compact Floating 3-Tab Beta Panel (v19.0: 60% height, ~30% narrower, higher translucency)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBetaMenu,
                    enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.95f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.96f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 58.dp, end = 8.dp, start = 12.dp, bottom = 8.dp)
                ) {
                    val panelScroll = rememberScrollState()
                    // Lock vertical scroll & fling strictly inside the Beta Lab panel so hitting top or
                    // bottom stops cleanly and never scrolls the parent AnatomyScreen LazyColumn.
                    val betaPanelScrollBlocker = remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset = available

                            override suspend fun onPostFling(
                                consumed: Velocity,
                                available: Velocity
                            ): Velocity = available
                        }
                    }
                    val headerScrollAbsorber = rememberScrollableState { 0f }
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 222.dp)
                            .heightIn(max = 200.dp)
                            .nestedScroll(betaPanelScrollBlocker)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                            .testTag("beta_floating_panel"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        ),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp)
                        ) {
                            // Non-scrolling header + 3-tab strip (absorbs vertical drags so parent page stays still)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scrollable(
                                        state = headerScrollAbsorber,
                                        orientation = Orientation.Vertical
                                    )
                            ) {
                                // Compact header + quick Reset All (turns toggles OFF while preserving last slider values)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Beta Lab",
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (totalActiveBetaCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    sessionState.disableAllBetaToggles()
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
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.80f))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "All off",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
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
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    BetaMenuTabPill(
                                        label = if (activeVulvaBetaCount > 0) "Vulva $activeVulvaBetaCount" else "Vulva",
                                        selected = betaMenuTab == 0,
                                        tag = "beta_tab_vulva",
                                        onClick = { betaMenuTab = 0 },
                                        modifier = Modifier.weight(1f)
                                    )
                                    BetaMenuTabPill(
                                        label = if (activeLifelikeBetaCount > 0) "Life $activeLifelikeBetaCount" else "Life",
                                        selected = betaMenuTab == 1,
                                        tag = "beta_tab_lifelike",
                                        onClick = { betaMenuTab = 1 },
                                        modifier = Modifier.weight(1f)
                                    )
                                    BetaMenuTabPill(
                                        label = if (activeLifelike2BetaCount > 0) "Life+ $activeLifelike2BetaCount" else "Life+",
                                        selected = betaMenuTab == 2,
                                        tag = "beta_tab_lifelike_plus",
                                        onClick = { betaMenuTab = 2 },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(panelScroll)
                            ) {
                                if (betaMenuTab == 0) {
                                    Text(
                                        text = "v21.0 tuned defaults (toggles preserve last slider value)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                            label = "Continuity strength (3×)",
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
                                            label = "Pigmentation blend (3×)",
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
                                            label = "Asymmetry strength (3×)",
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
                                            label = "Surface detail (3×)",
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
                                        title = "5. Labial-vestibule color blend",
                                        subtitle = "Smoothly melts the inner labial base into the vulvar vestibule color & tone",
                                        checked = betaV3LabialVestibuleBlend,
                                        tag = "toggle_beta_v3_labial_vestibule_blend",
                                        compact = true,
                                        onToggle = {
                                            betaV3LabialVestibuleBlend = !betaV3LabialVestibuleBlend
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3LabialVestibuleBlend) {
                                        BetaStrengthSliderRow(
                                            label = "Base blend strength",
                                            value = betaV3LabialBlendStrength,
                                            tag = "slider_beta_v3_labial_blend_strength",
                                            onValueChange = { betaV3LabialBlendStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "6. Intricate labial folds & border",
                                        subtitle = "Wavy asymmetric border with tangled inner folds, varied wetness & rosy tint",
                                        checked = betaV3IntricateLabiaFolds,
                                        tag = "toggle_beta_v3_intricate_labia_folds",
                                        compact = true,
                                        onToggle = {
                                            betaV3IntricateLabiaFolds = !betaV3IntricateLabiaFolds
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3IntricateLabiaFolds) {
                                        BetaStrengthSliderRow(
                                            label = "Folds & border strength",
                                            value = betaV3LabiaFoldsStrength,
                                            tag = "slider_beta_v3_labia_folds_strength",
                                            onValueChange = { betaV3LabiaFoldsStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "7. Vestibule-labial patch alignment",
                                        subtitle = "Aligns underlying mucosal patch to inner labia to remove pale skin bands",
                                        checked = betaV3MedialRootContinuity,
                                        tag = "toggle_beta_v3_medial_root_continuity",
                                        compact = true,
                                        onToggle = {
                                            betaV3MedialRootContinuity = !betaV3MedialRootContinuity
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaV3MedialRootContinuity) {
                                        BetaStrengthSliderRow(
                                            label = "Patch alignment strength",
                                            value = betaV3MedialRootStrength,
                                            tag = "slider_beta_v3_medial_root_strength",
                                            onValueChange = { betaV3MedialRootStrength = it }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Vulva elastic resistance",
                                        subtitle = "Firms labial pull resistance & lowers max spread up to 60%",
                                        checked = betaVulvaElasticityEnabled,
                                        tag = "toggle_beta_vulva_elasticity",
                                        compact = true,
                                        onToggle = {
                                            betaVulvaElasticityEnabled = !betaVulvaElasticityEnabled
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaVulvaElasticityEnabled) {
                                        val elasticPct = (betaVulvaElasticity.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE) * 100f).roundToInt()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Resistance (max 60%)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "+$elasticPct%",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Slider(
                                                value = betaVulvaElasticity.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE),
                                                onValueChange = { betaVulvaElasticity = it },
                                                valueRange = 0f..VULVA_ELASTIC_MAX_RESISTANCE,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("slider_beta_vulva_elasticity")
                                                    .semantics {
                                                        contentDescription = "Vulva elastic resistance, $elasticPct percent"
                                                    }
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                    )
                                    VulvaBetaToggleRow(
                                        title = "Breast size boost",
                                        subtitle = "Boosts breast size up to +50% except the most petite setting",
                                        checked = betaBreastSizeBoostEnabled,
                                        tag = "toggle_beta_breast_size_boost",
                                        compact = true,
                                        onToggle = {
                                            betaBreastSizeBoostEnabled = !betaBreastSizeBoostEnabled
                                            if (hapticsEnabled) hapticsEngine.triggerTap(SensitivityTier.MODERATE)
                                        }
                                    )
                                    AnimatedVisibility(visible = betaBreastSizeBoostEnabled) {
                                        val boostPct = (betaBreastSizeBoost.coerceIn(0f, 0.50f) * 100f).roundToInt()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Boost amount (max 50%)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "+$boostPct%",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Slider(
                                                value = betaBreastSizeBoost.coerceIn(0f, 0.50f),
                                                onValueChange = { betaBreastSizeBoost = it },
                                                valueRange = 0f..0.50f,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("slider_beta_breast_size_boost")
                                                    .semantics {
                                                        contentDescription = "Breast size boost, $boostPct percent"
                                                    }
                                            )
                                        }
                                    }
                                } else if (betaMenuTab == 1) {
                                    Text(
                                        text = "Subtle, irregular micro-behaviors (ON by default)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        text = "Next-gen realism (all ON by default except Heart Rate HUD)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        subtitle = "Slight natural timing difference (8–19ms) between left and right eyelids",
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
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(tag)
            .semantics {
                contentDescription = "$label tab${if (selected) ", selected" else ""}"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 11.sp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
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
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 3.dp else 8.dp)
            .testTag(tag)
            .semantics {
                contentDescription = "$title, ${if (checked) "enabled" else "disabled"}. $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.5.sp)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp)
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = if (compact) Modifier.scale(0.76f) else Modifier
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
            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
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

