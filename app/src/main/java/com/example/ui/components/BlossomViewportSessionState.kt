package com.example.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Scroll- and navigation-persistent session state for Blossom's 3D viewport (`InteractiveVulva3DCanvas`).
 *
 * **Version 19.0 Significant Pre-Graduation Milestone**:
 * - Hoisted above `AnatomyScreen`'s `LazyColumn` (via `AtlasViewModel`) so scrolling the viewport
 *   out of view and back never resets Blossom's active animations, self-spread pose, tissue/leg/head
 *   positions, camera framing, physiological heart rate, breathing phase, or Beta Lab settings.
 * - Promotes tuned v19.0 defaults across the Vulva, Life, and Life+ Beta Lab tabs while keeping
 *   every option independently toggleable prior to final graduation out of Beta.
 * - Preserves every slider's last value whenever its Beta toggle is turned OFF, restoring that exact
 *   value when toggled back ON.
 */
@Stable
class BlossomViewportSessionState {
    // --- Elastic tissue & positional body pose snapshots (synced to/from Compose Animatables) ---
    var leftOuterSpread: Float = 0f
    var rightOuterSpread: Float = 0f
    var leftInnerOpen: Float = 0f
    var rightInnerOpen: Float = 0f
    var hoodRetraction: Float = 0f
    var leftBreastX: Float = 0f
    var leftBreastY: Float = 0f
    var rightBreastX: Float = 0f
    var rightBreastY: Float = 0f
    var leftLegSpread: Float = 0f
    var rightLegSpread: Float = 0f
    var headTurn: Float = 0f
    var headPitch: Float = 0f
    var leftGluteX: Float = 0f
    var leftGluteY: Float = 0f
    var rightGluteX: Float = 0f
    var rightGluteY: Float = 0f
    var eyeBlink: Float = 0f
    var eyeBlinkR: Float = 0f

    // --- Lifelike & Lifelike+ live micro-behavior snapshots ---
    var eyeGazeX: Float = 0f
    var eyeGazeY: Float = 0f
    var idleHeadTurn: Float = 0f
    var idleHeadPitch: Float = 0f
    var idleHeadRoll: Float = 0f
    var faceWarmth: Float = 0f
    var faceBrow: Float = 0f
    var faceLipSoft: Float = 0f
    var shoulderRelaxL: Float = 0f
    var shoulderRelaxR: Float = 0f
    var anticipationGazeX: Float = 0f
    var anticipationGazeY: Float = 0f
    var swallowPhase: Float = 0f
    var settleHeadTurn: Float = 0f
    var settleHeadPitch: Float = 0f
    var settleHeadRoll: Float = 0f
    var settleShoulderL: Float = 0f
    var settleShoulderR: Float = 0f
    var settleLegL: Float = 0f
    var settleLegR: Float = 0f
    var nostrilBreath: Float = 0f
    var breathLipEase: Float = 0f
    var livingWarmth: Float = 0f

    internal val posturalSettleTriggerState = mutableIntStateOf(0)
    var posturalSettleTrigger: Int by posturalSettleTriggerState

    internal val lastManualHeadDragMsState = mutableLongStateOf(0L)
    var lastManualHeadDragMs: Long by lastManualHeadDragMsState

    // --- Self-spread pose & breathing cycle continuity ---
    internal val blossomSpreadPoseActiveState = mutableStateOf(false)
    var blossomSpreadPoseActive: Boolean by blossomSpreadPoseActiveState
    var selfSpreadPose: Float = 0f
    var breathPhase: Float = 0f

    // --- Physiology & heart-rate continuity ---
    internal val touchStimulusState = mutableFloatStateOf(0f)
    var touchStimulus: Float by touchStimulusState

    internal val lastStimulusTouchMsState = mutableLongStateOf(0L)
    var lastStimulusTouchMs: Long by lastStimulusTouchMsState

    internal val heartRateBpmState = mutableFloatStateOf(VulvaGlTextureView.RESTING_HEART_RATE_BPM)
    var heartRateBpm: Float by heartRateBpmState

    internal val heartRateSensitivityState = mutableFloatStateOf(0f)
    var heartRateSensitivity: Float by heartRateSensitivityState

    internal val heartRateDriveState = mutableFloatStateOf(0f)
    var heartRateDrive: Float by heartRateDriveState

    internal val lastHeartStimMsState = mutableLongStateOf(0L)
    var lastHeartStimMs: Long by lastHeartStimMsState

    // --- Peak 136 BPM -> 8s Pre-Event (136->152 BPM) -> 16s Peak -> 125 BPM Cooldown continuity ---
    internal val peakPreEventActiveState = mutableStateOf(false)
    var peakPreEventActive: Boolean by peakPreEventActiveState

    internal val peakPreEventStartMsState = mutableLongStateOf(0L)
    var peakPreEventStartMs: Long by peakPreEventStartMsState

    internal val peakPreEventStartBpmState = mutableFloatStateOf(PEAK_EPISODE_TRIGGER_BPM)
    var peakPreEventStartBpm: Float by peakPreEventStartBpmState

    internal val peakEpisodeActiveState = mutableStateOf(false)
    var peakEpisodeActive: Boolean by peakEpisodeActiveState

    internal val peakEpisodeEndMsState = mutableLongStateOf(0L)
    var peakEpisodeEndMs: Long by peakEpisodeEndMsState

    internal val peakPostCooldownActiveState = mutableStateOf(false)
    var peakPostCooldownActive: Boolean by peakPostCooldownActiveState

    internal val peakRecoveryFastEndMsState = mutableLongStateOf(0L)
    var peakRecoveryFastEndMs: Long by peakRecoveryFastEndMsState

    internal val peakRecoveryHoldUntilMsState = mutableLongStateOf(0L)
    var peakRecoveryHoldUntilMs: Long by peakRecoveryHoldUntilMsState

    internal val peakRefractoryUntilMsState = mutableLongStateOf(0L)
    var peakRefractoryUntilMs: Long by peakRefractoryUntilMsState

    internal val postPeakSighTokenState = mutableIntStateOf(0)
    var postPeakSighToken: Int by postPeakSighTokenState

    internal val postPeakSighCountState = mutableIntStateOf(2)
    var postPeakSighCount: Int by postPeakSighCountState

    var peakHeadPitch: Float = 0f
    var peakHeadTurn: Float = 0f
    var peakHeadRoll: Float = 0f
    var peakLegShakeL: Float = 0f
    var peakLegShakeR: Float = 0f
    var peakKneeShiftL: Float = 0f
    var peakKneeShiftR: Float = 0f
    var peakToeWiggleL: Float = 0f
    var peakToeWiggleR: Float = 0f
    var peakArmShiftL: Float = 0f
    var peakArmShiftR: Float = 0f
    var peakFingerCurlL: Float = 0f
    var peakFingerCurlR: Float = 0f
    var peakMouthOpen: Float = 0f

    // --- Appearance, lighting & viewport mode ---
    internal val hairStyleIndexState = mutableIntStateOf(2)
    var hairStyleIndex: Int by hairStyleIndexState

    internal val breastSizeState = mutableFloatStateOf(0.50f)
    var breastSize: Float by breastSizeState

    internal val pubicHairAmountState = mutableFloatStateOf(0.45f)
    var pubicHairAmount: Float by pubicHairAmountState

    internal val isOrbitModeState = mutableStateOf(false)
    var isOrbitMode: Boolean by isOrbitModeState

    internal val dramaticSideLightState = mutableStateOf(false)
    var dramaticSideLight: Boolean by dramaticSideLightState

    internal val showExperimentalLegsState = mutableStateOf(true)
    var showExperimentalLegs: Boolean by showExperimentalLegsState

    internal val showAdvancedRenderingState = mutableStateOf(false)
    var showAdvancedRendering: Boolean by showAdvancedRenderingState

    internal val legacyVulvaModeState = mutableStateOf(false)
    var legacyVulvaMode: Boolean by legacyVulvaModeState

    // --- Camera state snapshot (defaults to Exam Front full-body framing) ---
    internal val activePresetOrdinalState = mutableStateOf<Int?>(0)
    var activePresetOrdinal: Int? by activePresetOrdinalState
    var camYaw: Float = 0f
    var camPitch: Float = 5f
    var camZoom: Float = 1.02f
    var camTargetY: Float = 0.02f
    var camTargetZ: Float = 0.08f

    // --- Effect re-entry guards (prevents scroll-back from re-triggering past Reset / HoldOpen / Settle) ---
    var lastHandledResetTrigger: Int = 0
    var lastObservedHoldOpen: Boolean? = null
    var lastHandledPosturalSettleTrigger: Int = 0

    // --- Floating 3-Tab Beta Lab UI state ---
    internal val showBetaMenuState = mutableStateOf(false)
    var showBetaMenu: Boolean by showBetaMenuState

    internal val betaMenuTabState = mutableIntStateOf(0) // 0 = Vulva, 1 = Life, 2 = Life+
    var betaMenuTab: Int by betaMenuTabState

    // --- Tab 0 (Vulva): v19.0 Pre-Graduation Milestone defaults (all enabled with tuned strengths) ---
    // Each slider value is preserved when its toggle is turned OFF and restored when turned back ON.
    internal val betaV3OrganicContinuityState = mutableStateOf(true)
    var betaV3OrganicContinuity: Boolean by betaV3OrganicContinuityState
    internal val betaV3ContinuityStrengthState = mutableFloatStateOf(0.50f)
    var betaV3ContinuityStrength: Float by betaV3ContinuityStrengthState

    internal val betaV3SoftPigmentationState = mutableStateOf(true)
    var betaV3SoftPigmentation: Boolean by betaV3SoftPigmentationState
    internal val betaV3PigmentStrengthState = mutableFloatStateOf(0.50f)
    var betaV3PigmentStrength: Float by betaV3PigmentStrengthState

    internal val betaV3AnatomicalAsymmetryState = mutableStateOf(true)
    var betaV3AnatomicalAsymmetry: Boolean by betaV3AnatomicalAsymmetryState
    internal val betaV3AsymmetryStrengthState = mutableFloatStateOf(0.85f)
    var betaV3AsymmetryStrength: Float by betaV3AsymmetryStrengthState

    internal val betaV3RegionalSurfaceDetailState = mutableStateOf(true)
    var betaV3RegionalSurfaceDetail: Boolean by betaV3RegionalSurfaceDetailState
    internal val betaV3SurfaceStrengthState = mutableFloatStateOf(0.36f)
    var betaV3SurfaceStrength: Float by betaV3SurfaceStrengthState

    internal val betaV3LabialVestibuleBlendState = mutableStateOf(true)
    var betaV3LabialVestibuleBlend: Boolean by betaV3LabialVestibuleBlendState
    internal val betaV3LabialBlendStrengthState = mutableFloatStateOf(0.75f)
    var betaV3LabialBlendStrength: Float by betaV3LabialBlendStrengthState

    internal val betaV3IntricateLabiaFoldsState = mutableStateOf(true)
    var betaV3IntricateLabiaFolds: Boolean by betaV3IntricateLabiaFoldsState
    internal val betaV3LabiaFoldsStrengthState = mutableFloatStateOf(0.65f)
    var betaV3LabiaFoldsStrength: Float by betaV3LabiaFoldsStrengthState

    // Beta Option #7 (v22.0->v23.0): Vestibule-Labial Patch Alignment — ON by default (75% strength)
    internal val betaV3MedialRootContinuityState = mutableStateOf(true)
    var betaV3MedialRootContinuity: Boolean by betaV3MedialRootContinuityState
    internal val betaV3MedialRootStrengthState = mutableFloatStateOf(0.75f)
    var betaV3MedialRootStrength: Float by betaV3MedialRootStrengthState

    // Beta Option #8 (v28.0): Clitoral Hood–Mons Skin Blend — OFF by default (remembers 75% strength)
    internal val betaV3HoodMonsBlendState = mutableStateOf(false)
    var betaV3HoodMonsBlend: Boolean by betaV3HoodMonsBlendState
    internal val betaV3HoodMonsStrengthState = mutableFloatStateOf(0.75f)
    var betaV3HoodMonsStrength: Float by betaV3HoodMonsStrengthState

    internal val betaVulvaElasticityEnabledState = mutableStateOf(true)
    var betaVulvaElasticityEnabled: Boolean by betaVulvaElasticityEnabledState
    internal val betaVulvaElasticityState = mutableFloatStateOf(VULVA_ELASTIC_DEFAULT_RESISTANCE)
    var betaVulvaElasticity: Float by betaVulvaElasticityState

    internal val betaBreastSizeBoostEnabledState = mutableStateOf(true)
    var betaBreastSizeBoostEnabled: Boolean by betaBreastSizeBoostEnabledState
    internal val betaBreastSizeBoostState = mutableFloatStateOf(0.45f)
    var betaBreastSizeBoost: Float by betaBreastSizeBoostState

    // --- Tab 1 (Life): all 6 promoted ON by default ---
    internal val betaLifeEyeMicrosaccadesState = mutableStateOf(true)
    var betaLifeEyeMicrosaccades: Boolean by betaLifeEyeMicrosaccadesState

    internal val betaLifeDoubleBlinkingState = mutableStateOf(true)
    var betaLifeDoubleBlinking: Boolean by betaLifeDoubleBlinkingState

    internal val betaLifeHeadRepositioningState = mutableStateOf(true)
    var betaLifeHeadRepositioning: Boolean by betaLifeHeadRepositioningState

    internal val betaLifeDeepBreathsSighsState = mutableStateOf(true)
    var betaLifeDeepBreathsSighs: Boolean by betaLifeDeepBreathsSighsState

    internal val betaLifeShoulderRelaxationState = mutableStateOf(true)
    var betaLifeShoulderRelaxation: Boolean by betaLifeShoulderRelaxationState

    internal val betaLifeFacialExpressionsState = mutableStateOf(true)
    var betaLifeFacialExpressions: Boolean by betaLifeFacialExpressionsState

    // --- Tab 2 (Life+): v19.0 promotes all 7 realism features ON by default; Heart Rate HUD stays OFF ---
    internal val betaLife2EyelidTrackingState = mutableStateOf(true)
    var betaLife2EyelidTracking: Boolean by betaLife2EyelidTrackingState

    internal val betaLife2AsymmetricBlinkingState = mutableStateOf(true)
    var betaLife2AsymmetricBlinking: Boolean by betaLife2AsymmetricBlinkingState

    internal val betaLife2GazeAnticipationState = mutableStateOf(true)
    var betaLife2GazeAnticipation: Boolean by betaLife2GazeAnticipationState

    internal val betaLife2SubtleSwallowingState = mutableStateOf(true)
    var betaLife2SubtleSwallowing: Boolean by betaLife2SubtleSwallowingState

    internal val betaLife2PosturalSettlingState = mutableStateOf(true)
    var betaLife2PosturalSettling: Boolean by betaLife2PosturalSettlingState

    internal val betaLife2BreathFaceCouplingState = mutableStateOf(true)
    var betaLife2BreathFaceCoupling: Boolean by betaLife2BreathFaceCouplingState

    internal val betaLife2LivingWarmthState = mutableStateOf(true)
    var betaLife2LivingWarmth: Boolean by betaLife2LivingWarmthState

    internal val betaLife2ShowHeartRateState = mutableStateOf(false)
    var betaLife2ShowHeartRate: Boolean by betaLife2ShowHeartRateState

    // --- Tab 2 (Life+) v27.0 Photorealistic Visual Realism Beta toggles (all 5 OFF by default) ---
    internal val betaRealismLayeredHairState = mutableStateOf(false)
    var betaRealismLayeredHair: Boolean by betaRealismLayeredHairState

    internal val betaRealismSkinDetailState = mutableStateOf(false)
    var betaRealismSkinDetail: Boolean by betaRealismSkinDetailState

    internal val betaRealismEyesAndLashesState = mutableStateOf(false)
    var betaRealismEyesAndLashes: Boolean by betaRealismEyesAndLashesState

    internal val betaRealismContactShadowsState = mutableStateOf(false)
    var betaRealismContactShadows: Boolean by betaRealismContactShadowsState

    internal val betaRealismDynamicCreasesState = mutableStateOf(false)
    var betaRealismDynamicCreases: Boolean by betaRealismDynamicCreasesState

    // --- Tab 2 (Life+) v28.0 Additional Photorealistic Realism Beta toggles (all OFF by default) ---
    internal val betaRealismFacialMicroState = mutableStateOf(false)
    var betaRealismFacialMicro: Boolean by betaRealismFacialMicroState

    internal val betaRealismStudioLightingState = mutableStateOf(false)
    var betaRealismStudioLighting: Boolean by betaRealismStudioLightingState

    internal val betaRealismSurfaceMicroState = mutableStateOf(false)
    var betaRealismSurfaceMicro: Boolean by betaRealismSurfaceMicroState

    /** Effective vulva elastic resistance (0f when toggled OFF while preserving last slider value). */
    val effectiveVulvaElasticity: Float
        get() = if (betaVulvaElasticityEnabled) {
            betaVulvaElasticity.coerceIn(0f, VULVA_ELASTIC_MAX_RESISTANCE)
        } else {
            0f
        }

    /** Effective breast size boost (0f when toggled OFF while preserving last slider value). */
    val effectiveBreastSizeBoost: Float
        get() = if (betaBreastSizeBoostEnabled) {
            betaBreastSizeBoost.coerceIn(0f, 0.50f)
        } else {
            0f
        }

    /**
     * Turns OFF every Beta Lab toggle across all 3 tabs while intentionally preserving each
     * slider's last configured value so re-enabling any toggle restores the user's prior tuning.
     */
    fun disableAllBetaToggles() {
        betaV3OrganicContinuity = false
        betaV3SoftPigmentation = false
        betaV3AnatomicalAsymmetry = false
        betaV3RegionalSurfaceDetail = false
        betaV3LabialVestibuleBlend = false
        betaV3IntricateLabiaFolds = false
        betaV3MedialRootContinuity = false
        betaV3HoodMonsBlend = false
        betaVulvaElasticityEnabled = false
        betaBreastSizeBoostEnabled = false
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
        betaRealismLayeredHair = false
        betaRealismSkinDetail = false
        betaRealismEyesAndLashes = false
        betaRealismContactShadows = false
        betaRealismDynamicCreases = false
        betaRealismFacialMicro = false
        betaRealismStudioLighting = false
        betaRealismSurfaceMicro = false
    }
}
