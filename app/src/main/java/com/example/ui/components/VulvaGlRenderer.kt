package com.example.ui.components

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.view.TextureView
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.IdentityHashMap
import kotlin.math.cos
import kotlin.math.sin

data class VulvaCamera3D(
    val yawDeg: Float = 0f,
    val pitchDeg: Float = 5f,
    val zoom: Float = 1.0f,
    val targetY: Float = 0.02f,
    val targetZ: Float = 0.08f
)

data class VulvaTouchGlow(
    val normX: Float = 0.5f,
    val normY: Float = 0.5f,
    val intensity: Float = 0f,
    val color: Color = Color(0xFFFFD1DC)
)

data class BlossomHairStyle(
    val name: String,
    val baseColor: Color,
    val highlightColor: Color,
    val sheenColor: Color
)

val BlossomHairStyles = listOf(
    BlossomHairStyle(
        name = "Jet Black",
        baseColor = Color(0xFF161314),
        highlightColor = Color(0xFF2D2527),
        sheenColor = Color(0xFFAAB0C2)
    ),
    BlossomHairStyle(
        name = "Dark Espresso",
        baseColor = Color(0xFF231814),
        highlightColor = Color(0xFF452E22),
        sheenColor = Color(0xFFC2A492)
    ),
    BlossomHairStyle(
        name = "Chestnut Brown",
        baseColor = Color(0xFF3E2417),
        highlightColor = Color(0xFF7A4B2E),
        sheenColor = Color(0xFFDFB898)
    ),
    BlossomHairStyle(
        name = "Warm Auburn",
        baseColor = Color(0xFF5A2516),
        highlightColor = Color(0xFFA24A28),
        sheenColor = Color(0xFFF0BE96)
    ),
    BlossomHairStyle(
        name = "Honey Blonde",
        baseColor = Color(0xFF8B6739),
        highlightColor = Color(0xFFD5AE6E),
        sheenColor = Color(0xFFFFECC8)
    ),
    BlossomHairStyle(
        name = "Platinum Blonde",
        baseColor = Color(0xFFB4A38B),
        highlightColor = Color(0xFFE7DBC6),
        sheenColor = Color(0xFFFFFAF0)
    )
)

/**
 * TextureView-backed OpenGL ES 2.0 renderer using EGL14.
 * Features multi-octave procedural dermal pore & labial crease normal mapping,
 * dual-scatter skin Subsurface Scattering (SSS), micro-specular oil breakup,
 * wet dual-lobe mucosal BRDF, and studio gynecological exam lighting.
 */
class VulvaGlTextureView(
    context: Context,
    private val onGlError: () -> Unit
) : TextureView(context), TextureView.SurfaceTextureListener {

    @Volatile
    var deformationState: VulvaDeformationState = VulvaDeformationState()

    @Volatile
    var cameraState: VulvaCamera3D = VulvaCamera3D()

    @Volatile
    var toneStyle: VulvaToneStyle = VulvaToneStyles[1]

    @Volatile
    var hairStyle: BlossomHairStyle = BlossomHairStyles[2]

    @Volatile
    var pubicHairAmount: Float = 0.45f

    @Volatile
    var touchGlow: VulvaTouchGlow = VulvaTouchGlow()

    @Volatile
    var dramaticSideLight: Boolean = false

    @Volatile
    var showExperimentalLegs: Boolean = true

    @Volatile
    var touchStimulus: Float = 0f

    @Volatile
    var heartRateBpm: Float = RESTING_HEART_RATE_BPM

    @Volatile
    var peakBreathActive: Boolean = false

    @Volatile
    var postPeakSighToken: Int = 0

    @Volatile
    var postPeakSighCount: Int = 2

    @Volatile
    private var lastAppliedDeformation: VulvaDeformationState? = null

    @Volatile
    private var lastAppliedLegsMode: Boolean? = null

    private class GlMeshVbo(
        val vboId: Int,
        val iboId: Int,
        var uploadedVertexVersion: Int = -1,
        var uploadedIndexVersion: Int = -1
    )

    private class RenderSession(
        val generation: Int,
        val surface: SurfaceTexture,
        val thread: HandlerThread,
        val handler: Handler
    ) {
        var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        var programId: Int = 0
        var glReleased: Boolean = false
        @Volatile
        var teardownPosted: Boolean = false
        var surfaceToRelease: SurfaceTexture? = null
        @Volatile
        var renderScheduled: Boolean = false
        var breathTickerScheduled: Boolean = false
        val meshVbos: IdentityHashMap<DynamicGlMesh, GlMeshVbo> = IdentityHashMap()
        var archAlphaVboId: Int = 0
        var archAlphaVersion: Int = -1
        var archCcwIboId: Int = 0
        var archCoreIboId: Int = 0
        var archSkirtIboId: Int = 0
        var archSplitVersion: Int = -1
        var vulvaPatchInteriorIboId: Int = 0
        var vulvaPatchSeamIboId: Int = 0
        var bodyBindHoleVboId: Int = 0
        var bodyBindHoleVersion: Int = -1
    }

    private val sessionLock = Any()
    private var activeSession: RenderSession? = null

    private var aPosLoc = -1
    private var aNormalLoc = -1
    private var aMatLoc = -1
    private var aArchAlphaLoc = -1
    private var aBodyBindHoleLoc = -1

    private var uMvpLoc = -1
    private var uModelLoc = -1
    private var uCamPosLoc = -1
    private var uSkinBaseLoc = -1
    private var uSkinShadowLoc = -1
    private var uMucosaBaseLoc = -1
    private var uMucosaDeepLoc = -1
    private var uTouchPosLoc = -1
    private var uTouchColorLoc = -1
    private var uTouchIntensityLoc = -1
    private var uStretchLoc = -1
    private var uLightModeLoc = -1
    private var uMaterialTypeLoc = -1
    private var uHairBaseLoc = -1
    private var uHairHighlightLoc = -1
    private var uHairSheenLoc = -1
    private var uPubicHairLoc = -1
    private var uAllowPubicHairLoc = -1
    private var uBreathChestLoc = -1
    private var uBreathAbdomenLoc = -1
    private var uAllowBreathingLoc = -1
    private var uBetaSmoothNeckSeamLoc = -1
    private var uBetaSoftHairLoc = -1
    private var uBetaSmoothBodyHoleLoc = -1
    private var uSmoothBodyHoleOvalRLoc = -1
    private var uArchAlphaPassLoc = -1
    private var uBetaWarmTransitionZoneLoc = -1
    private var uBetaRefineInnerLabiaLoc = -1
    private var uBetaV2NaturalPelvicFinishLoc = -1
    private var uShoulderRelaxLLoc = -1
    private var uShoulderRelaxRLoc = -1
    private var uBetaLifeWarmthLoc = -1
    private var uBetaV3SoftPigmentationLoc = -1
    private var uBetaV3RegionalMaterialLoc = -1
    private var uBetaV3LabialVestibuleBlendLoc = -1
    private var uBetaV3IntricateLabiaFoldsLoc = -1
    private var uBetaV3MedialRootContinuityLoc = -1
    private var uIsLabiaArchMeshLoc = -1

    private var surfaceWidth = 1
    private var surfaceHeight = 1
    @Volatile
    private var isGlReady = false

    @Volatile
    private var isHostVisible = true

    @Volatile
    private var isHostPaused = false

    @Volatile
    private var renderGeneration = 0

    @Volatile
    var breathPhase: Float = 0f
    private var smoothedStimulus = 0f
    private var lastBreathTimeNs = 0L
    private val breathRng = kotlin.random.Random(System.nanoTime())
    private var lastDeepBreathEnabled = false
    private var breathCycleFreqMul = 1.0f
    private var breathCycleDepthMul = 1.0f
    private var breathCycleIsSigh = false
    private var cyclesUntilNextSigh = 2
    private var lastPeakBreathActive = false
    private var peakCycleFreqHz = 0.78f
    private var peakCycleDepthMul = 1.48f
    private var lastHandledPostPeakSighToken = 0
    private var pendingPostPeakSighs = 0
    private var inPostPeakSigh = false

    private val breathTicker = Runnable {
        val session = synchronized(sessionLock) {
            val s = activeSession
            if (s != null && !s.teardownPosted && !s.glReleased) s else null
        } ?: return@Runnable
        session.breathTickerScheduled = false
        if (isGlReady && isHostVisible && !isHostPaused && showExperimentalLegs) {
            drawFrameInternal(session)
        }
    }

    private var sceneMesh: VulvaAnatomySceneMesh? = null

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    init {
        isOpaque = true
        surfaceTextureListener = this
    }

    private fun currentActiveSession(): RenderSession? = synchronized(sessionLock) {
        val s = activeSession
        if (s != null && !s.teardownPosted && !s.glReleased) s else null
    }

    fun requestFrame() {
        val session = currentActiveSession() ?: return
        if (isHostPaused || !isHostVisible) return
        if (!session.renderScheduled) {
            session.renderScheduled = true
            session.handler.post {
                session.renderScheduled = false
                if (session.generation != renderGeneration || session.teardownPosted) return@post
                if (session.breathTickerScheduled) {
                    session.handler.removeCallbacks(breathTicker)
                    session.breathTickerScheduled = false
                }
                drawFrameInternal(session)
            }
        }
    }

    fun setRenderingPaused(paused: Boolean) {
        if (isHostPaused == paused) return
        isHostPaused = paused
        val session = currentActiveSession() ?: return
        if (paused) {
            session.handler.removeCallbacks(breathTicker)
            session.breathTickerScheduled = false
        } else if (isGlReady && isHostVisible) {
            lastBreathTimeNs = System.nanoTime()
            requestFrame()
        }
    }

    fun shutdown() {
        requestSessionTeardown(surfaceToRelease = null)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateHostVisibility(visibility == View.VISIBLE && this.visibility == View.VISIBLE)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateHostVisibility(windowVisibility == View.VISIBLE && this.visibility == View.VISIBLE && visibility == View.VISIBLE)
    }

    override fun onDetachedFromWindow() {
        updateHostVisibility(false)
        requestSessionTeardown(surfaceToRelease = null)
        super.onDetachedFromWindow()
    }

    private fun updateHostVisibility(visible: Boolean) {
        if (isHostVisible == visible) return
        isHostVisible = visible
        val session = currentActiveSession() ?: return
        if (!visible) {
            session.handler.removeCallbacks(breathTicker)
            session.breathTickerScheduled = false
        } else if (isGlReady && !isHostPaused) {
            lastBreathTimeNs = System.nanoTime()
            requestFrame()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        isHostVisible = windowVisibility == View.VISIBLE && visibility == View.VISIBLE
        startRenderThread(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        val session = currentActiveSession() ?: return
        session.handler.post {
            if (isGlReady && session.generation == renderGeneration && !session.teardownPosted) {
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
                drawFrameInternal(session)
            }
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // Keep SurfaceTexture alive until the GL thread finishes any in-flight frame and calls
        // eglDestroySurface(), preventing Surface queueBuffer(-19 / ENODEV) & EGL_BAD_SURFACE!
        val deferReleaseToGlThread = requestSessionTeardown(surfaceToRelease = surface)
        return !deferReleaseToGlThread
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun startRenderThread(surface: SurfaceTexture) {
        requestSessionTeardown(surfaceToRelease = null)
        // Run below UI/HWUI RenderThread priority so Compose window compositing & fence waits are never starved
        val thread = HandlerThread("VulvaGlRenderThread", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        val handler = Handler(thread.looper)
        val session = synchronized(sessionLock) {
            val generation = ++renderGeneration
            RenderSession(generation, surface, thread, handler).also {
                activeSession = it
            }
        }
        handler.post {
            if (session.generation != renderGeneration || session.teardownPosted) return@post
            val ok = initEglAndShaders(session)
            if (!ok || session.generation != renderGeneration || session.teardownPosted) {
                if (!ok && session.generation == renderGeneration && !session.teardownPosted) {
                    post { onGlError() }
                }
                return@post
            }
            // Brief yield after the first clear buffer swap so HWUI finishes initial frame composition
            // before heavy procedural SDF anatomy meshes are generated.
            handler.postDelayed({
                if (session.generation != renderGeneration || session.teardownPosted ||
                    session.eglSurface == EGL14.EGL_NO_SURFACE
                ) return@postDelayed
                sceneMesh = VulvaAnatomySceneMesh()
                lastAppliedDeformation = VulvaDeformationState()
                lastAppliedLegsMode = true
                lastBreathTimeNs = System.nanoTime()
                isGlReady = true
                if (isHostVisible && !isHostPaused && session.generation == renderGeneration && !session.teardownPosted) {
                    drawFrameInternal(session)
                }
            }, 60L)
        }
    }

    /**
     * Initiates safe asynchronous EGL teardown for the current session.
     * Returns true if [surfaceToRelease] ownership was handed to the GL thread (caller must return
     * `false` from `onSurfaceTextureDestroyed` so Android does not destroy the surface prematurely).
     */
    private fun requestSessionTeardown(surfaceToRelease: SurfaceTexture?): Boolean {
        val sessionToTearDown: RenderSession?
        val shouldPostTeardown: Boolean
        val deferredRelease: Boolean

        synchronized(sessionLock) {
            val s = activeSession
            if (s == null || s.glReleased) {
                if (s != null && s.glReleased) activeSession = null
                return false
            }
            if (surfaceToRelease != null && s.surface === surfaceToRelease) {
                s.surfaceToRelease = surfaceToRelease
                deferredRelease = true
            } else {
                deferredRelease = false
            }
            renderGeneration++
            isGlReady = false
            if (!s.teardownPosted) {
                s.teardownPosted = true
                sessionToTearDown = s
                shouldPostTeardown = true
            } else {
                sessionToTearDown = null
                shouldPostTeardown = false
            }
        }

        if (shouldPostTeardown && sessionToTearDown != null) {
            val handler = sessionToTearDown.handler
            handler.removeCallbacksAndMessages(null)
            handler.post {
                sessionToTearDown.breathTickerScheduled = false
                sessionToTearDown.renderScheduled = false
                releaseGlInternal(sessionToTearDown)
                val toRelease = synchronized(sessionLock) {
                    sessionToTearDown.glReleased = true
                    val surf = sessionToTearDown.surfaceToRelease
                    sessionToTearDown.surfaceToRelease = null
                    if (activeSession === sessionToTearDown) {
                        activeSession = null
                    }
                    surf
                }
                if (toRelease != null) {
                    try {
                        toRelease.release()
                    } catch (_: Throwable) {
                        // Surface already released
                    }
                }
                sessionToTearDown.thread.quitSafely()
            }
        }
        return deferredRelease
    }

    private fun initEglAndShaders(session: RenderSession): Boolean {
        if (session.generation != renderGeneration || session.teardownPosted) return false
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return false
        session.eglDisplay = display

        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return false

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
            numConfigs[0] <= 0 || configs[0] == null
        ) {
            return false
        }
        val config = configs[0]!!

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (context == EGL14.EGL_NO_CONTEXT) return false
        session.eglContext = context

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurf = EGL14.eglCreateWindowSurface(display, config, session.surface, surfaceAttribs, 0)
        if (eglSurf == EGL14.EGL_NO_SURFACE) return false
        session.eglSurface = eglSurf

        if (!EGL14.eglMakeCurrent(display, eglSurf, eglSurf, context)) return false

        // Immediately clear and swap one dark-backdrop frame so SurfaceTexture / SurfaceSyncGroup
        // receives a ready buffer right away before shader compilation and SDF mesh generation.
        if (session.generation != renderGeneration || session.teardownPosted) return false
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClearColor(0.11f, 0.085f, 0.085f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glFinish()
        if (!EGL14.eglSwapBuffers(display, eglSurf)) return false

        val program = compileProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) return false
        session.programId = program

        aPosLoc = GLES20.glGetAttribLocation(program, "aPos")
        aNormalLoc = GLES20.glGetAttribLocation(program, "aNormal")
        aMatLoc = GLES20.glGetAttribLocation(program, "aMat")
        aArchAlphaLoc = GLES20.glGetAttribLocation(program, "aArchAlpha")
        aBodyBindHoleLoc = GLES20.glGetAttribLocation(program, "aBodyBindHole")

        uMvpLoc = GLES20.glGetUniformLocation(program, "uMvp")
        uModelLoc = GLES20.glGetUniformLocation(program, "uModel")
        uCamPosLoc = GLES20.glGetUniformLocation(program, "uCamPos")
        uSkinBaseLoc = GLES20.glGetUniformLocation(program, "uSkinBase")
        uSkinShadowLoc = GLES20.glGetUniformLocation(program, "uSkinShadow")
        uMucosaBaseLoc = GLES20.glGetUniformLocation(program, "uMucosaBase")
        uMucosaDeepLoc = GLES20.glGetUniformLocation(program, "uMucosaDeep")
        uTouchPosLoc = GLES20.glGetUniformLocation(program, "uTouchPos")
        uTouchColorLoc = GLES20.glGetUniformLocation(program, "uTouchColor")
        uTouchIntensityLoc = GLES20.glGetUniformLocation(program, "uTouchIntensity")
        uStretchLoc = GLES20.glGetUniformLocation(program, "uStretch")
        uLightModeLoc = GLES20.glGetUniformLocation(program, "uLightMode")
        uMaterialTypeLoc = GLES20.glGetUniformLocation(program, "uMaterialType")
        uHairBaseLoc = GLES20.glGetUniformLocation(program, "uHairBase")
        uHairHighlightLoc = GLES20.glGetUniformLocation(program, "uHairHighlight")
        uHairSheenLoc = GLES20.glGetUniformLocation(program, "uHairSheen")
        uPubicHairLoc = GLES20.glGetUniformLocation(program, "uPubicHair")
        uAllowPubicHairLoc = GLES20.glGetUniformLocation(program, "uAllowPubicHair")
        uBreathChestLoc = GLES20.glGetUniformLocation(program, "uBreathChest")
        uBreathAbdomenLoc = GLES20.glGetUniformLocation(program, "uBreathAbdomen")
        uAllowBreathingLoc = GLES20.glGetUniformLocation(program, "uAllowBreathing")
        uBetaSmoothNeckSeamLoc = GLES20.glGetUniformLocation(program, "uBetaSmoothNeckSeam")
        uBetaSoftHairLoc = GLES20.glGetUniformLocation(program, "uBetaSoftHair")
        uBetaSmoothBodyHoleLoc = GLES20.glGetUniformLocation(program, "uBetaSmoothBodyHole")
        uSmoothBodyHoleOvalRLoc = GLES20.glGetUniformLocation(program, "uSmoothBodyHoleOvalR")
        uArchAlphaPassLoc = GLES20.glGetUniformLocation(program, "uArchAlphaPass")
        uBetaWarmTransitionZoneLoc = GLES20.glGetUniformLocation(program, "uBetaWarmTransitionZone")
        uBetaRefineInnerLabiaLoc = GLES20.glGetUniformLocation(program, "uBetaRefineInnerLabia")
        uBetaV2NaturalPelvicFinishLoc = GLES20.glGetUniformLocation(program, "uBetaV2NaturalPelvicFinish")
        uShoulderRelaxLLoc = GLES20.glGetUniformLocation(program, "uShoulderRelaxL")
        uShoulderRelaxRLoc = GLES20.glGetUniformLocation(program, "uShoulderRelaxR")
        uBetaLifeWarmthLoc = GLES20.glGetUniformLocation(program, "uBetaLifeWarmth")
        uBetaV3SoftPigmentationLoc = GLES20.glGetUniformLocation(program, "uBetaV3SoftPigmentation")
        uBetaV3RegionalMaterialLoc = GLES20.glGetUniformLocation(program, "uBetaV3RegionalMaterial")
        uBetaV3LabialVestibuleBlendLoc = GLES20.glGetUniformLocation(program, "uBetaV3LabialVestibuleBlend")
        uBetaV3IntricateLabiaFoldsLoc = GLES20.glGetUniformLocation(program, "uBetaV3IntricateLabiaFolds")
        uBetaV3MedialRootContinuityLoc = GLES20.glGetUniformLocation(program, "uBetaV3MedialRootContinuity")
        uIsLabiaArchMeshLoc = GLES20.glGetUniformLocation(program, "uIsLabiaArchMesh")

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        return true
    }

    private fun drawFrameInternal(session: RenderSession) {
        if (session.generation != renderGeneration || session.teardownPosted ||
            !isGlReady || !isHostVisible || isHostPaused ||
            session.eglDisplay == EGL14.EGL_NO_DISPLAY || session.eglSurface == EGL14.EGL_NO_SURFACE
        ) return

        val frameStartNs = System.nanoTime()
        val mesh = sceneMesh ?: return
        val currentDeform = deformationState
        val legsMode = showExperimentalLegs
        if (currentDeform != lastAppliedDeformation || legsMode != lastAppliedLegsMode) {
            mesh.update(currentDeform, includeLithotomyBody = legsMode)
            lastAppliedDeformation = currentDeform
            lastAppliedLegsMode = legsMode
        }

        val tone = toneStyle
        val cam = cameraState
        val glow = touchGlow

        // Deep warm clinical-studio dark backdrop so sculpted 3D skin & thighs pop with rich rim lighting
        GLES20.glClearColor(0.11f, 0.085f, 0.085f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat().coerceAtLeast(1f)
        val camPos = computeMatrices(cam, aspect, modelMatrix, viewMatrix, projMatrix, vpMatrix, mvpMatrix)

        GLES20.glUseProgram(session.programId)
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(uCamPosLoc, camPos[0], camPos[1], camPos[2])

        GLES20.glUniform3f(uSkinBaseLoc, tone.skinMid.red, tone.skinMid.green, tone.skinMid.blue)
        GLES20.glUniform3f(uSkinShadowLoc, tone.skinShadow.red, tone.skinShadow.green, tone.skinShadow.blue)
        GLES20.glUniform3f(uMucosaBaseLoc, tone.mucosaMid.red, tone.mucosaMid.green, tone.mucosaMid.blue)
        GLES20.glUniform3f(uMucosaDeepLoc, tone.vestibuleDeep.red, tone.vestibuleDeep.green, tone.vestibuleDeep.blue)
        val hair = hairStyle
        GLES20.glUniform3f(uHairBaseLoc, hair.baseColor.red, hair.baseColor.green, hair.baseColor.blue)
        GLES20.glUniform3f(uHairHighlightLoc, hair.highlightColor.red, hair.highlightColor.green, hair.highlightColor.blue)
        GLES20.glUniform3f(uHairSheenLoc, hair.sheenColor.red, hair.sheenColor.green, hair.sheenColor.blue)
        GLES20.glUniform1f(uPubicHairLoc, pubicHairAmount.coerceIn(0f, 1f))

        val touchWorldX = (glow.normX - 0.5f) * 1.60f
        val touchWorldY = (0.5f - glow.normY) * 1.90f
        GLES20.glUniform2f(uTouchPosLoc, touchWorldX, touchWorldY)
        GLES20.glUniform3f(uTouchColorLoc, glow.color.red, glow.color.green, glow.color.blue)
        GLES20.glUniform1f(uTouchIntensityLoc, glow.intensity)

        val avgStretch = ((currentDeform.leftOuter + currentDeform.rightOuter +
            currentDeform.leftInner + currentDeform.rightInner) * 0.25f).coerceIn(0f, 1f)
        GLES20.glUniform1f(uStretchLoc, avgStretch)
        GLES20.glUniform1f(uLightModeLoc, if (dramaticSideLight) 1.0f else 0.0f)
        GLES20.glUniform1f(uBetaSmoothNeckSeamLoc, if (currentDeform.betaSmoothNeckSeam) 1.0f else 0.0f)
        GLES20.glUniform1f(uBetaSoftHairLoc, if (currentDeform.betaSoftHair) 1.0f else 0.0f)
        GLES20.glUniform1f(uSmoothBodyHoleOvalRLoc, SEAM_D_HOLE_OVAL_R)

        // Advance natural periodic chest & abdominal breathing phase (true physiological speed regardless of idle frame pacing)
        val nowNs = System.nanoTime()
        val dtSec = if (lastBreathTimeNs > 0L) {
            ((nowNs - lastBreathTimeNs) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.22f)
        } else {
            0f
        }
        lastBreathTimeNs = nowNs
        val targetStim = touchStimulus.coerceIn(0f, 1f)
        val stimRate = if (targetStim > smoothedStimulus) 3.2f else 0.55f
        smoothedStimulus = (smoothedStimulus + (targetStim - smoothedStimulus) * (dtSec * stimRate).coerceIn(0f, 1f))
            .coerceIn(0f, 1f)
        val liveBpm = heartRateBpm.coerceIn(RESTING_HEART_RATE_BPM, PEAK_MAX_HEART_RATE_BPM)
        val liveHrNorm = heartRateExertionNorm(liveBpm)
        val deepBreathsEnabled = legsMode && currentDeform.betaLifeDeepBreathsSighs
        if (deepBreathsEnabled != lastDeepBreathEnabled) {
            lastDeepBreathEnabled = deepBreathsEnabled
            if (deepBreathsEnabled) {
                // Start with natural irregular variation and trigger an unhurried sigh within 1-2 breaths
                breathCycleFreqMul = 0.92f + breathRng.nextFloat() * 0.16f
                breathCycleDepthMul = 0.94f + breathRng.nextFloat() * 0.18f
                breathCycleIsSigh = false
                cyclesUntilNextSigh = 1 + breathRng.nextInt(2)
            } else {
                breathCycleFreqMul = 1.0f
                breathCycleDepthMul = 1.0f
                breathCycleIsSigh = false
                cyclesUntilNextSigh = 2
            }
        }
        val activeBreathFreq = breathFrequencyHzForHeartRate(liveBpm, smoothedStimulus)
        val peakActiveNow = legsMode && peakBreathActive
        if (peakActiveNow != lastPeakBreathActive) {
            lastPeakBreathActive = peakActiveNow
            if (peakActiveNow) {
                pendingPostPeakSighs = 0
                inPostPeakSigh = false
                breathCycleIsSigh = false
                peakCycleFreqHz = 0.72f + breathRng.nextFloat() * 0.44f
                peakCycleDepthMul = 1.42f + breathRng.nextFloat() * 0.62f
            }
        }
        val sighTokenNow = postPeakSighToken
        if (sighTokenNow != lastHandledPostPeakSighToken) {
            lastHandledPostPeakSighToken = sighTokenNow
            if (legsMode && !peakActiveNow) {
                pendingPostPeakSighs = postPeakSighCount.coerceIn(2, 3)
                inPostPeakSigh = true
                breathPhase = 0f
                breathCycleIsSigh = true
                breathCycleFreqMul = 0.48f + breathRng.nextFloat() * 0.08f
                breathCycleDepthMul = 1.82f + breathRng.nextFloat() * 0.18f
            }
        }
        if (legsMode) {
            when {
                peakActiveNow -> {
                    // Peak 12s exertion episode: dramatic, fast, heavily irregular breath speed & fullness
                    val nextPhase = breathPhase + dtSec * peakCycleFreqHz
                    if (nextPhase >= 1.0f) {
                        breathPhase = nextPhase % 1.0f
                        peakCycleFreqHz = 0.62f + breathRng.nextFloat() * 0.56f
                        peakCycleDepthMul = 1.38f + breathRng.nextFloat() * 0.68f
                    } else {
                        breathPhase = nextPhase
                    }
                }
                inPostPeakSigh -> {
                    // Post-peak afterglow: 2 or 3 powerful, slow recovery sighs (~0.18–0.22 Hz each)
                    val sighFreqHz = (0.205f * (0.88f + 0.24f * (breathCycleFreqMul - 0.48f) / 0.08f)).coerceIn(0.16f, 0.24f)
                    val nextPhase = breathPhase + dtSec * sighFreqHz
                    if (nextPhase >= 1.0f) {
                        breathPhase = nextPhase % 1.0f
                        pendingPostPeakSighs--
                        if (pendingPostPeakSighs > 0) {
                            breathCycleIsSigh = true
                            breathCycleFreqMul = 0.48f + breathRng.nextFloat() * 0.08f
                            breathCycleDepthMul = 1.78f + breathRng.nextFloat() * 0.22f
                        } else {
                            inPostPeakSigh = false
                            breathCycleIsSigh = false
                            breathCycleFreqMul = 0.92f + breathRng.nextFloat() * 0.16f
                            breathCycleDepthMul = 0.94f + breathRng.nextFloat() * 0.18f
                            cyclesUntilNextSigh = 3 + breathRng.nextInt(4)
                        }
                    } else {
                        breathPhase = nextPhase
                    }
                }
                !deepBreathsEnabled -> {
                    breathPhase = (breathPhase + dtSec * activeBreathFreq) % 1.0f
                }
                else -> {
                    // During elevated heart rate, taper random slow-sigh intervals toward steadier rapid exertion breaths
                    val effCycleFreqMul = 1.0f + (breathCycleFreqMul - 1.0f) * (1.0f - 0.65f * liveHrNorm)
                    val nextPhase = breathPhase + dtSec * activeBreathFreq * effCycleFreqMul
                    if (nextPhase >= 1.0f) {
                        breathPhase = nextPhase % 1.0f
                        cyclesUntilNextSigh--
                        if (cyclesUntilNextSigh <= 0 && liveHrNorm < 0.55f) {
                            breathCycleIsSigh = true
                            breathCycleFreqMul = 0.64f + breathRng.nextFloat() * 0.12f
                            breathCycleDepthMul = 1.56f + breathRng.nextFloat() * 0.34f
                            cyclesUntilNextSigh = 4 + breathRng.nextInt(5)
                        } else {
                            breathCycleIsSigh = false
                            breathCycleFreqMul = 0.89f + breathRng.nextFloat() * 0.22f
                            breathCycleDepthMul = 0.86f + breathRng.nextFloat() * 0.28f
                            if (cyclesUntilNextSigh <= 0) cyclesUntilNextSigh = 3 + breathRng.nextInt(3)
                        }
                    } else {
                        breathPhase = nextPhase
                    }
                }
            }
        }
        val breathDisp = when {
            !legsMode -> Pair(0f, 0f)
            peakActiveNow -> evaluateHeartRateBreathingDisplacement(
                phaseCycles = breathPhase,
                bpm = liveBpm,
                stimulus = 1f,
                isSighCycle = false,
                depthMultiplier = peakCycleDepthMul,
                peakMode = true
            )
            inPostPeakSigh -> evaluateHeartRateBreathingDisplacement(
                phaseCycles = breathPhase,
                bpm = liveBpm,
                stimulus = smoothedStimulus,
                isSighCycle = true,
                depthMultiplier = breathCycleDepthMul,
                forcePowerfulSigh = true
            )
            !deepBreathsEnabled -> evaluateHeartRateBreathingDisplacement(
                phaseCycles = breathPhase,
                bpm = liveBpm,
                stimulus = smoothedStimulus,
                isSighCycle = false,
                depthMultiplier = 1f
            )
            else -> evaluateHeartRateBreathingDisplacement(
                phaseCycles = breathPhase,
                bpm = liveBpm,
                stimulus = smoothedStimulus,
                isSighCycle = breathCycleIsSigh && liveHrNorm < 0.55f,
                depthMultiplier = breathCycleDepthMul
            )
        }
        GLES20.glUniform1f(uBreathChestLoc, breathDisp.first)
        GLES20.glUniform1f(uBreathAbdomenLoc, breathDisp.second)
        GLES20.glUniform1f(uShoulderRelaxLLoc, if (legsMode) currentDeform.shoulderRelaxL else 0f)
        GLES20.glUniform1f(uShoulderRelaxRLoc, if (legsMode) currentDeform.shoulderRelaxR else 0f)
        GLES20.glUniform1f(uBetaLifeWarmthLoc, if (legsMode) currentDeform.livingWarmth.coerceIn(0f, 1f) else 0f)
        GLES20.glUniform1f(
            uBetaV3SoftPigmentationLoc,
            if (currentDeform.betaV3SoftPigmentation) currentDeform.betaV3PigmentStrength.coerceIn(0f, 1f) * 3.0f else 0f
        )
        GLES20.glUniform1f(
            uBetaV3RegionalMaterialLoc,
            if (currentDeform.betaV3RegionalSurfaceDetail) currentDeform.betaV3SurfaceStrength.coerceIn(0f, 1f) * 3.0f else 0f
        )
        GLES20.glUniform1f(
            uBetaV3LabialVestibuleBlendLoc,
            if (currentDeform.betaV3LabialVestibuleBlend) currentDeform.betaV3LabialBlendStrength.coerceIn(0f, 1f) else 0f
        )
        GLES20.glUniform1f(
            uBetaV3IntricateLabiaFoldsLoc,
            if (currentDeform.betaV3IntricateLabiaFolds) currentDeform.betaV3LabiaFoldsStrength.coerceIn(0f, 1f) else 0f
        )
        GLES20.glUniform1f(
            uBetaV3MedialRootContinuityLoc,
            if (currentDeform.betaV3MedialRootContinuity) currentDeform.betaV3MedialRootStrength.coerceIn(0f, 1f) else 0f
        )

        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glEnableVertexAttribArray(aMatLoc)

        fun isAabbVisible(
            minX: Float, maxX: Float,
            minY: Float, maxY: Float,
            minZ: Float, maxZ: Float,
            margin: Float
        ): Boolean {
            val x0 = minX - margin; val x1 = maxX + margin
            val y0 = minY - margin; val y1 = maxY + margin
            val z0 = minZ - margin; val z1 = maxZ + margin
            val m = mvpMatrix
            var outLeft = true
            var outRight = true
            var outBottom = true
            var outTop = true
            var outNear = true
            var outFar = true
            for (c in 0 until 8) {
                val px = if ((c and 1) == 0) x0 else x1
                val py = if ((c and 2) == 0) y0 else y1
                val pz = if ((c and 4) == 0) z0 else z1
                val cx = m[0] * px + m[4] * py + m[8] * pz + m[12]
                val cy = m[1] * px + m[5] * py + m[9] * pz + m[13]
                val cz = m[2] * px + m[6] * py + m[10] * pz + m[14]
                val cw = m[3] * px + m[7] * py + m[11] * pz + m[15]
                if (cw <= 0f) return true
                if (cx >= -cw) outLeft = false
                if (cx <= cw) outRight = false
                if (cy >= -cw) outBottom = false
                if (cy <= cw) outTop = false
                if (cz >= -cw) outNear = false
                if (cz <= cw) outFar = false
            }
            return !(outLeft || outRight || outBottom || outTop || outNear || outFar)
        }

        fun ensureMeshBuffers(glMesh: DynamicGlMesh): GlMeshVbo {
            var entry = session.meshVbos[glMesh]
            if (entry == null) {
                val ids = IntArray(2)
                GLES20.glGenBuffers(2, ids, 0)
                entry = GlMeshVbo(vboId = ids[0], iboId = ids[1])
                session.meshVbos[glMesh] = entry
            }
            if (entry.uploadedVertexVersion != glMesh.vertexVersion) {
                val vb = glMesh.vertexBuffer
                vb.position(0)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entry.vboId)
                if (entry.uploadedVertexVersion < 0) {
                    GLES20.glBufferData(
                        GLES20.GL_ARRAY_BUFFER,
                        glMesh.vertexData.size * 4,
                        vb,
                        GLES20.GL_DYNAMIC_DRAW
                    )
                } else {
                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, glMesh.vertexData.size * 4, vb)
                }
                entry.uploadedVertexVersion = glMesh.vertexVersion
            }
            if (entry.uploadedIndexVersion != glMesh.indexVersion) {
                val ib = glMesh.indexBuffer
                ib.position(0)
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, entry.iboId)
                GLES20.glBufferData(
                    GLES20.GL_ELEMENT_ARRAY_BUFFER,
                    glMesh.indexCount * 2,
                    ib,
                    GLES20.GL_STATIC_DRAW
                )
                entry.uploadedIndexVersion = glMesh.indexVersion
            }
            return entry
        }

        fun ensureArchExtraBuffers() {
            if (session.archCcwIboId == 0) {
                val ids = IntArray(4)
                GLES20.glGenBuffers(4, ids, 0)
                session.archAlphaVboId = ids[0]
                session.archCcwIboId = ids[1]
                session.archCoreIboId = ids[2]
                session.archSkirtIboId = ids[3]

                val ccwBuf = mesh.labiaHoodArchCcwIndexBuffer
                ccwBuf.position(0)
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.archCcwIboId)
                GLES20.glBufferData(
                    GLES20.GL_ELEMENT_ARRAY_BUFFER,
                    mesh.labiaHoodArchMesh.indexCount * 2,
                    ccwBuf,
                    GLES20.GL_STATIC_DRAW
                )
            }
            if (session.archAlphaVersion != mesh.labiaHoodArchAlphaVersion) {
                val alphaBuf = mesh.labiaHoodArchAlphaBuffer
                alphaBuf.position(0)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, session.archAlphaVboId)
                val alphaBytes = mesh.labiaHoodArchMesh.vertexCount * 4
                if (session.archAlphaVersion < 0) {
                    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, alphaBytes, alphaBuf, GLES20.GL_DYNAMIC_DRAW)
                } else {
                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, alphaBytes, alphaBuf)
                }
                session.archAlphaVersion = mesh.labiaHoodArchAlphaVersion
            }
            if (session.archSplitVersion != mesh.labiaHoodArchSplitVersion) {
                if (mesh.labiaHoodArchCoreIndexCount > 0) {
                    val coreBuf = mesh.labiaHoodArchCoreIndexBuffer
                    coreBuf.position(0)
                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.archCoreIboId)
                    GLES20.glBufferData(
                        GLES20.GL_ELEMENT_ARRAY_BUFFER,
                        mesh.labiaHoodArchCoreIndexCount * 2,
                        coreBuf,
                        GLES20.GL_DYNAMIC_DRAW
                    )
                }
                if (mesh.labiaHoodArchSkirtIndexCount > 0) {
                    val skirtBuf = mesh.labiaHoodArchSkirtIndexBuffer
                    skirtBuf.position(0)
                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.archSkirtIboId)
                    GLES20.glBufferData(
                        GLES20.GL_ELEMENT_ARRAY_BUFFER,
                        mesh.labiaHoodArchSkirtIndexCount * 2,
                        skirtBuf,
                        GLES20.GL_DYNAMIC_DRAW
                    )
                }
                session.archSplitVersion = mesh.labiaHoodArchSplitVersion
            }
        }

        fun ensureVulvaPatchSplitBuffers() {
            if (session.vulvaPatchInteriorIboId == 0) {
                val ids = IntArray(2)
                GLES20.glGenBuffers(2, ids, 0)
                session.vulvaPatchInteriorIboId = ids[0]
                session.vulvaPatchSeamIboId = ids[1]

                if (mesh.vulvaPatchInteriorIndexCount > 0) {
                    val intBuf = mesh.vulvaPatchInteriorIndexBuffer
                    intBuf.position(0)
                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.vulvaPatchInteriorIboId)
                    GLES20.glBufferData(
                        GLES20.GL_ELEMENT_ARRAY_BUFFER,
                        mesh.vulvaPatchInteriorIndexCount * 2,
                        intBuf,
                        GLES20.GL_STATIC_DRAW
                    )
                }
                if (mesh.vulvaPatchSeamIndexCount > 0) {
                    val seamBuf = mesh.vulvaPatchSeamIndexBuffer
                    seamBuf.position(0)
                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.vulvaPatchSeamIboId)
                    GLES20.glBufferData(
                        GLES20.GL_ELEMENT_ARRAY_BUFFER,
                        mesh.vulvaPatchSeamIndexCount * 2,
                        seamBuf,
                        GLES20.GL_STATIC_DRAW
                    )
                }
            }
        }

        fun ensureBodyBindHoleBuffer() {
            if (session.bodyBindHoleVboId == 0) {
                val ids = IntArray(1)
                GLES20.glGenBuffers(1, ids, 0)
                session.bodyBindHoleVboId = ids[0]
            }
            if (session.bodyBindHoleVersion != mesh.blossomBodyBindHoleVersion) {
                val buf = mesh.blossomBodyBindHoleBuffer
                buf.position(0)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, session.bodyBindHoleVboId)
                val bytes = mesh.blossomBodyBindHoleData.size * 4
                if (session.bodyBindHoleVersion < 0) {
                    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, bytes, buf, GLES20.GL_STATIC_DRAW)
                } else {
                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, bytes, buf)
                }
                session.bodyBindHoleVersion = mesh.blossomBodyBindHoleVersion
            }
        }

        fun drawMeshList(meshes: List<DynamicGlMesh>, materialType: Float) {
            GLES20.glUniform1f(uMaterialTypeLoc, materialType)
            meshes.forEach { glMesh ->
                // Exact conservative clip-space AABB frustum culling (skips off-screen head/hair/breasts/toes in close-ups)
                if (!isAabbVisible(glMesh.minX, glMesh.maxX, glMesh.minY, glMesh.maxY, glMesh.minZ, glMesh.maxZ, margin = 0.28f)) {
                    return@forEach
                }

                val allowPubic = (materialType < 0.5f) &&
                    (glMesh === mesh.blossomBodyMesh || glMesh === mesh.blossomVulvaPatchMesh || glMesh === mesh.pelvisMesh)
                GLES20.glUniform1f(uAllowPubicHairLoc, if (allowPubic) 1.0f else 0.0f)
                val allowBreath = legsMode && (materialType < 0.5f) &&
                    (glMesh === mesh.blossomBodyMesh || glMesh === mesh.blossomBreastsMesh)
                GLES20.glUniform1f(uAllowBreathingLoc, if (allowBreath) 1.0f else 0.0f)
                val useSmoothBodyHole = legsMode && materialType < 0.5f &&
                    currentDeform.betaSmoothBodyHoleCutoff && glMesh === mesh.blossomBodyMesh
                GLES20.glUniform1f(uBetaSmoothBodyHoleLoc, if (useSmoothBodyHole) 1.0f else 0.0f)
                val useNaturalPelvicFinish = currentDeform.betaV2NaturalPelvicFinish && materialType < 0.5f &&
                    (glMesh === mesh.blossomBodyMesh ||
                        glMesh === mesh.blossomVulvaPatchMesh ||
                        glMesh === mesh.pelvisMesh ||
                        glMesh === mesh.labiaHoodArchMesh ||
                        glMesh === mesh.glansMesh)
                GLES20.glUniform1f(uBetaV2NaturalPelvicFinishLoc, if (useNaturalPelvicFinish) 1.0f else 0.0f)

                val vboEntry = ensureMeshBuffers(glMesh)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboEntry.vboId)
                GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, DynamicGlMesh.STRIDE_BYTES, 0)
                GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, DynamicGlMesh.STRIDE_BYTES, 12)
                GLES20.glVertexAttribPointer(aMatLoc, 4, GLES20.GL_FLOAT, false, DynamicGlMesh.STRIDE_BYTES, 24)

                val isArchMesh = materialType < 0.5f && glMesh === mesh.labiaHoodArchMesh
                GLES20.glUniform1f(uIsLabiaArchMeshLoc, if (isArchMesh) 1.0f else 0.0f)
                val useArchWarmFix = isArchMesh && currentDeform.betaWarmTransitionZone
                GLES20.glUniform1f(uBetaWarmTransitionZoneLoc, if (useArchWarmFix) 1.0f else 0.0f)

                val useInnerLabiaRefine = isArchMesh && currentDeform.betaRefineInnerLabia
                GLES20.glUniform1f(uBetaRefineInnerLabiaLoc, if (useInnerLabiaRefine) 1.0f else 0.0f)

                val useArchSoftEdge = isArchMesh && currentDeform.betaSoftArchEdge
                if (isArchMesh && (useArchWarmFix || useArchSoftEdge)) {
                    ensureArchExtraBuffers()
                }
                if (aArchAlphaLoc >= 0) {
                    if (useArchSoftEdge) {
                        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, session.archAlphaVboId)
                        GLES20.glEnableVertexAttribArray(aArchAlphaLoc)
                        GLES20.glVertexAttribPointer(aArchAlphaLoc, 1, GLES20.GL_FLOAT, false, 4, 0)
                    } else {
                        GLES20.glDisableVertexAttribArray(aArchAlphaLoc)
                        GLES20.glVertexAttrib1f(aArchAlphaLoc, 1.0f)
                    }
                }
                if (aBodyBindHoleLoc >= 0) {
                    if (useSmoothBodyHole) {
                        ensureBodyBindHoleBuffer()
                        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, session.bodyBindHoleVboId)
                        GLES20.glEnableVertexAttribArray(aBodyBindHoleLoc)
                        GLES20.glVertexAttribPointer(aBodyBindHoleLoc, 4, GLES20.GL_FLOAT, false, 16, 0)
                    } else {
                        GLES20.glDisableVertexAttribArray(aBodyBindHoleLoc)
                        GLES20.glVertexAttrib4f(aBodyBindHoleLoc, 2.0f, 2.0f, -2.0f, 0.0f)
                    }
                }

                val useSeamAPolyOffset = legsMode && currentDeform.betaSmoothVulvaSeam &&
                    glMesh === mesh.blossomVulvaPatchMesh
                val useSeamOnlyPolyOffset = useSeamAPolyOffset && currentDeform.betaV2SeamOnlyPolyOffset
                if (useSeamOnlyPolyOffset) {
                    // Vulva Beta v2 Toggle 1: draw interior patch triangles (edgeR < 0.88) WITHOUT polygon
                    // offset so oblique views never pull the recessed patch forward through labiaHoodArchMesh
                    // or glansMesh; apply Seam A polygon offset strictly to the outer seam ring (edgeR >= 0.88).
                    ensureVulvaPatchSplitBuffers()
                    GLES20.glUniform1f(uArchAlphaPassLoc, 0.0f)
                    if (mesh.vulvaPatchInteriorIndexCount > 0) {
                        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.vulvaPatchInteriorIboId)
                        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.vulvaPatchInteriorIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                    }
                    if (mesh.vulvaPatchSeamIndexCount > 0) {
                        GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)
                        GLES20.glPolygonOffset(SEAM_A_PATCH_POLY_OFFSET_FACTOR, SEAM_A_PATCH_POLY_OFFSET_UNITS)
                        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.vulvaPatchSeamIboId)
                        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.vulvaPatchSeamIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                        GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)
                    }
                } else {
                    if (useSeamAPolyOffset) {
                        GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)
                        GLES20.glPolygonOffset(SEAM_A_PATCH_POLY_OFFSET_FACTOR, SEAM_A_PATCH_POLY_OFFSET_UNITS)
                    }
                    if (useArchSoftEdge) {
                        // Pass 1: Opaque interior core (writes depth for clean self-occlusion)
                        GLES20.glUniform1f(uArchAlphaPassLoc, 1.0f)
                        if (useArchWarmFix) {
                            if (mesh.labiaHoodArchCoreIndexCount > 0) {
                                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.archCoreIboId)
                                GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.labiaHoodArchCoreIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                            }
                        } else {
                            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboEntry.iboId)
                            GLES20.glDrawElements(GLES20.GL_TRIANGLES, glMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                        }

                        // Pass 2: Feathered outer ~20% skirt alpha-blended cleanly onto underlying vulva patch
                        GLES20.glUniform1f(uArchAlphaPassLoc, 2.0f)
                        GLES20.glEnable(GLES20.GL_BLEND)
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                        GLES20.glDepthMask(false)
                        if (useArchWarmFix) {
                            if (mesh.labiaHoodArchSkirtIndexCount > 0) {
                                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, session.archSkirtIboId)
                                GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.labiaHoodArchSkirtIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                            }
                        } else {
                            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboEntry.iboId)
                            GLES20.glDrawElements(GLES20.GL_TRIANGLES, glMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                        }
                        GLES20.glDepthMask(true)
                        GLES20.glDisable(GLES20.GL_BLEND)
                        GLES20.glUniform1f(uArchAlphaPassLoc, 0.0f)
                    } else {
                        GLES20.glUniform1f(uArchAlphaPassLoc, 0.0f)
                        val iboId = if (useArchWarmFix) session.archCcwIboId else vboEntry.iboId
                        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)
                        GLES20.glDrawElements(GLES20.GL_TRIANGLES, glMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                    }
                    if (useSeamAPolyOffset) {
                        GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)
                    }
                }
            }
        }

        if (legsMode) {
            drawMeshList(mesh.blossomDrapeMeshes, materialType = 1.0f)
            drawMeshList(mesh.blossomSkinMeshes, materialType = 0.0f)
            // Pull 3D keratin nail plates cleanly forward in 16-bit depth buffer to prevent any finger-skin Z-fighting
            GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)
            GLES20.glPolygonOffset(-2.0f, -2.0f)
            drawMeshList(mesh.blossomNailMeshes, materialType = 4.0f)
            GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)
            drawMeshList(mesh.blossomEyeMeshes, materialType = 2.0f)
            drawMeshList(mesh.blossomHairMeshes, materialType = 3.0f)
        } else {
            drawMeshList(mesh.coreMeshes, materialType = 0.0f)
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisableVertexAttribArray(aPosLoc)
        GLES20.glDisableVertexAttribArray(aNormalLoc)
        GLES20.glDisableVertexAttribArray(aMatLoc)
        if (aArchAlphaLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aArchAlphaLoc)
        }
        if (aBodyBindHoleLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aBodyBindHoleLoc)
        }

        if (session.generation != renderGeneration || session.teardownPosted || !isGlReady) return
        // Complete GPU rasterization on this background GL thread before handing the buffer to SurfaceTexture
        // so HWUI's EglManager::fenceWait never stalls on queued GPU work, and frameMs reflects real GPU time.
        GLES20.glFinish()
        val swapped = EGL14.eglSwapBuffers(session.eglDisplay, session.eglSurface)
        if (!swapped) {
            val eglErr = EGL14.eglGetError()
            if (eglErr == EGL14.EGL_BAD_SURFACE || eglErr == EGL14.EGL_CONTEXT_LOST) {
                isGlReady = false
            }
            return
        }

        // Only schedule idle breathing ticks when the breathing abdomen/chest region is visible in the viewport
        val breathingRegionVisible = legsMode && isAabbVisible(-1.50f, 1.50f, 1.08f, 4.86f, -0.50f, 1.25f, margin = 0.10f)
        if (breathingRegionVisible && isGlReady && isHostVisible && !isHostPaused &&
            session.generation == renderGeneration && !session.teardownPosted && !session.breathTickerScheduled
        ) {
            val frameMs = ((System.nanoTime() - frameStartNs) / 1_000_000L).coerceAtLeast(0L)
            // GPU-aware adaptive idle pacing: smooth calm breathing while preserving generous HWUI headroom
            val nextDelayMs = maxOf(66L, frameMs * 2L).coerceAtMost(220L)
            session.breathTickerScheduled = true
            session.handler.postDelayed(breathTicker, nextDelayMs)
        }
    }

    private fun releaseGlInternal(session: RenderSession) {
        if (session.generation == renderGeneration) {
            isGlReady = false
        }
        if (session.eglDisplay != EGL14.EGL_NO_DISPLAY && session.eglContext != EGL14.EGL_NO_CONTEXT) {
            if (session.meshVbos.isNotEmpty()) {
                val ids = IntArray(session.meshVbos.size * 2)
                var k = 0
                session.meshVbos.values.forEach { entry ->
                    ids[k++] = entry.vboId
                    ids[k++] = entry.iboId
                }
                GLES20.glDeleteBuffers(k, ids, 0)
                session.meshVbos.clear()
            }
            if (session.archCcwIboId != 0) {
                val extras = intArrayOf(
                    session.archAlphaVboId,
                    session.archCcwIboId,
                    session.archCoreIboId,
                    session.archSkirtIboId
                )
                GLES20.glDeleteBuffers(extras.size, extras, 0)
                session.archAlphaVboId = 0
                session.archCcwIboId = 0
                session.archCoreIboId = 0
                session.archSkirtIboId = 0
            }
            if (session.vulvaPatchInteriorIboId != 0) {
                val patchSplitIds = intArrayOf(
                    session.vulvaPatchInteriorIboId,
                    session.vulvaPatchSeamIboId
                )
                GLES20.glDeleteBuffers(patchSplitIds.size, patchSplitIds, 0)
                session.vulvaPatchInteriorIboId = 0
                session.vulvaPatchSeamIboId = 0
            }
            if (session.bodyBindHoleVboId != 0) {
                val bindHoleIds = intArrayOf(session.bodyBindHoleVboId)
                GLES20.glDeleteBuffers(1, bindHoleIds, 0)
                session.bodyBindHoleVboId = 0
                session.bodyBindHoleVersion = -1
            }
            if (session.programId != 0) {
                GLES20.glDeleteProgram(session.programId)
                session.programId = 0
            }
            EGL14.eglMakeCurrent(
                session.eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            if (session.eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(session.eglDisplay, session.eglSurface)
                session.eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (session.eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(session.eglDisplay, session.eglContext)
                session.eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglTerminate(session.eglDisplay)
            session.eglDisplay = EGL14.EGL_NO_DISPLAY
        } else {
            session.meshVbos.clear()
            session.programId = 0
        }
    }

    private fun compileProgram(vsSrc: String, fsSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
        if (vs == 0) return 0
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
        if (fs == 0) {
            GLES20.glDeleteShader(vs)
            return 0
        }
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        internal const val SEAM_A_PATCH_POLY_OFFSET_FACTOR = -0.75f
        internal const val SEAM_A_PATCH_POLY_OFFSET_UNITS = -1.0f
        internal const val SEAM_D_HOLE_OVAL_R = 0.93f

        /**
         * Pre-fix Seam D predicate mirror (evaluated on posed aPos/vRestPos and posed normalZ).
         * Kept solely for isolated regression tests proving posed thigh cutouts before the fix.
         */
        internal fun evaluateLegacyPosedSeamDDiscard(
            posedX: Float,
            posedY: Float,
            posedZ: Float,
            normalZ: Float,
            betaSmoothBodyHoleCutoff: Boolean,
            holeOvalRThreshold: Float = SEAM_D_HOLE_OVAL_R
        ): Boolean {
            if (!betaSmoothBodyHoleCutoff) return false
            val ox = posedX / BlossomSdfBodyBuilder.PATCH_RX
            val oy = (posedY - BlossomSdfBodyBuilder.PATCH_CENTER_Y) / BlossomSdfBodyBuilder.PATCH_RY
            val holeOvalR = kotlin.math.sqrt(ox * ox + oy * oy)
            return posedZ > -0.92f && normalZ > 0.04f && holeOvalR < holeOvalRThreshold
        }

        /**
         * Exact CPU mirror of the fixed Seam D fragment discard predicate in FRAGMENT_SHADER_PART_2,
         * evaluated strictly from unposed bind-pose pelvic coordinates (aBodyBindHole / vBodyBindHole).
         */
        internal fun evaluateBindPoseSeamDDiscard(
            bindOvalX: Float,
            bindOvalY: Float,
            bindZ: Float,
            bindNz: Float,
            betaSmoothBodyHoleCutoff: Boolean,
            holeOvalRThreshold: Float = SEAM_D_HOLE_OVAL_R
        ): Boolean {
            if (!betaSmoothBodyHoleCutoff) return false
            val holeOvalR = kotlin.math.sqrt(bindOvalX * bindOvalX + bindOvalY * bindOvalY)
            return bindZ > -0.92f && bindNz > 0.04f && holeOvalR < holeOvalRThreshold
        }

        fun computeMatrices(
            cam: VulvaCamera3D,
            aspect: Float,
            model: FloatArray,
            view: FloatArray,
            proj: FloatArray,
            vp: FloatArray,
            mvp: FloatArray
        ): FloatArray {
            Matrix.setIdentityM(model, 0)
            // Expanded zoom range (0.10 = Blossom 1:1 Head-to-Toe Full Body + Exam Table, 1.95 = Macro Close-Up)
            val radius = (2.82f / cam.zoom.coerceIn(0.10f, 1.95f))
            val yawRad = Math.toRadians(cam.yawDeg.toDouble()).toFloat()
            val pitchRad = Math.toRadians(cam.pitchDeg.toDouble()).toFloat()

            val eyeX = radius * cos(pitchRad) * sin(yawRad)
            val eyeY = cam.targetY + radius * sin(pitchRad)
            val eyeZ = cam.targetZ + radius * cos(pitchRad) * cos(yawRad)

            Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, 0f, cam.targetY, cam.targetZ, 0f, 1f, 0f)
            Matrix.perspectiveM(proj, 0, 40f, aspect, 0.18f, 85f)
            Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
            return floatArrayOf(eyeX, eyeY, eyeZ)
        }

        fun projectPointToNormalizedScreen(
            worldPoint: FloatArray,
            cam: VulvaCamera3D,
            aspect: Float
        ): Offset {
            val model = FloatArray(16)
            val view = FloatArray(16)
            val proj = FloatArray(16)
            val vp = FloatArray(16)
            val mvp = FloatArray(16)
            computeMatrices(cam, aspect, model, view, proj, vp, mvp)

            val input = floatArrayOf(worldPoint[0], worldPoint[1], worldPoint[2], 1f)
            val clip = FloatArray(4)
            Matrix.multiplyMV(clip, 0, mvp, 0, input, 0)
            val w = clip[3].coerceAtLeast(0.0001f)
            val ndcX = clip[0] / w
            val ndcY = clip[1] / w
            return Offset(
                x = ((ndcX + 1f) * 0.5f).coerceIn(0.05f, 0.95f),
                y = ((1f - ndcY) * 0.5f).coerceIn(0.05f, 0.95f)
            )
        }

        /**
         * Natural respiratory frequency: calm ~14.1 breaths/min at rest (0.235 Hz),
         * gently quickening up to ~20.7 breaths/min (0.345 Hz) during tactile engagement.
         */
        internal fun breathFrequencyHz(stimulus: Float): Float {
            val s = stimulus.coerceIn(0f, 1f)
            return 0.235f + 0.110f * s
        }

        /**
         * Asymmetric physiological respiratory waveform in [0..1]:
         * smooth inhale (0.00..0.40), relaxed slower exhale (0.40..0.84), brief calm resting pause (0.84..1.00).
         */
        internal fun evaluateBreathWave(cycle01: Float): Float {
            val t = ((cycle01 % 1f) + 1f) % 1f
            return when {
                t < 0.40f -> {
                    val u = t / 0.40f
                    (0.5f - 0.5f * cos(u * Math.PI.toFloat())).coerceIn(0f, 1f)
                }
                t < 0.84f -> {
                    val u = (t - 0.40f) / 0.44f
                    (0.5f + 0.5f * cos(u * Math.PI.toFloat())).coerceIn(0f, 1f)
                }
                else -> 0f
            }
        }

        /**
         * Computes subtle synchronized (chestRise, abdomenRise) world-space respiratory lift.
         * Abdominal/diaphragmatic swell gently leads thoracic ribcage expansion in cohesive lockstep.
         */
        internal fun evaluateBreathingDisplacement(phaseCycles: Float, stimulus: Float): Pair<Float, Float> {
            val s = stimulus.coerceIn(0f, 1f)
            val depthScale = 1.0f + 0.24f * s
            val abdomenWave = evaluateBreathWave(phaseCycles)
            val chestWave = evaluateBreathWave(phaseCycles - 0.04f)
            // Gently noticeable calm supine ribcage & diaphragmatic expansion in lockstep
            val chestRise = chestWave * 0.072f * depthScale
            val abdomenRise = abdomenWave * 0.096f * depthScale
            return Pair(chestRise, abdomenRise)
        }

        /**
         * Lifelike Beta irregular deep-breath / sigh waveform in [0..1].
         * Normal cycles use the exact physiological curve; sigh cycles take a fuller, slightly
         * longer inhale (0.00..0.44), hold a delicate beat at the crest (0.44..0.50), and release
         * in a slow, eased exhale (0.50..0.93) before a soft resting pause.
         */
        internal fun evaluateSighBreathWave(cycle01: Float, isSighCycle: Boolean): Float {
            if (!isSighCycle) return evaluateBreathWave(cycle01)
            val t = ((cycle01 % 1f) + 1f) % 1f
            return when {
                t < 0.44f -> {
                    val u = t / 0.44f
                    (0.5f - 0.5f * cos(u * Math.PI.toFloat())).coerceIn(0f, 1f)
                }
                t < 0.50f -> {
                    val u = (t - 0.44f) / 0.06f
                    (1f - 0.035f * u * u).coerceIn(0f, 1f)
                }
                t < 0.93f -> {
                    val u = (t - 0.50f) / 0.43f
                    val ease = (0.5f + 0.5f * cos(u * Math.PI.toFloat())).coerceIn(0f, 1f)
                    (0.965f * ease * (0.76f + 0.24f * ease)).coerceIn(0f, 1f)
                }
                else -> 0f
            }
        }

        internal fun evaluateBreathingDisplacementWithSigh(
            phaseCycles: Float,
            stimulus: Float,
            isSighCycle: Boolean,
            depthMultiplier: Float
        ): Pair<Float, Float> {
            val s = stimulus.coerceIn(0f, 1f)
            val depthScale = (1.0f + 0.24f * s) * depthMultiplier.coerceIn(0.75f, 2.0f)
            val abdomenWave = evaluateSighBreathWave(phaseCycles, isSighCycle)
            val chestLag = if (isSighCycle) 0.028f else 0.04f
            val chestWave = evaluateSighBreathWave(phaseCycles - chestLag, isSighCycle)
            // During a deep sigh, the upper ribcage participates a touch more visibly relative to calm rest
            val chestBoost = if (isSighCycle) 1.12f else 1.0f
            val chestRise = chestWave * 0.072f * depthScale * chestBoost
            val abdomenRise = abdomenWave * 0.096f * depthScale
            return Pair(chestRise, abdomenRise)
        }

        const val RESTING_HEART_RATE_BPM = 66f
        const val MAX_HEART_RATE_BPM = 142f
        const val PEAK_MAX_HEART_RATE_BPM = 152f

        /**
         * Version 19.0 sympathetic cardiac ramp divisor: slows Blossom's heart-rate rise by 3×
         * so arousal/cardiac elevation builds gradually and organically over sustained contact.
         */
        internal const val HEART_RATE_RISE_RATE_DIVISOR = 3.0f

        internal fun heartRateExertionNorm(bpm: Float): Float =
            ((bpm - RESTING_HEART_RATE_BPM) / (MAX_HEART_RATE_BPM - RESTING_HEART_RATE_BPM)).coerceIn(0f, 1f)

        /**
         * Physiological heart-rate dynamics: rises smoothly (at the v19.0 3× slower build rate)
         * toward a sensitivity-weighted ceiling during active touch/manipulation (highest for
         * clitoral glans/hood, moderate for inner labia and breasts, gentle for outer regions)
         * and recovers exponentially back to `recoveryFloorBpm` (resting 66 BPM by default, or
         * 125 BPM during post-peak cooldown) after a brief sympathetic inertia beat once stimulation stops.
         */
        internal fun stepHeartRateBpm(
            currentBpm: Float,
            dtSec: Float,
            targetSensitivityGain: Float,
            activeDrive: Float,
            secondsSinceLastStim: Float,
            recoveryRateMultiplier: Float = 1f,
            forceRecovery: Boolean = false,
            recoveryFloorBpm: Float = RESTING_HEART_RATE_BPM,
            maxBpm: Float = MAX_HEART_RATE_BPM
        ): Float {
            val safeMax = kotlin.math.max(maxBpm, MAX_HEART_RATE_BPM)
            val dt = dtSec.coerceIn(0f, 0.25f)
            if (dt <= 0f) return currentBpm.coerceIn(RESTING_HEART_RATE_BPM, safeMax)
            val sens = targetSensitivityGain.coerceIn(0f, 1f)
            val drive = activeDrive.coerceIn(0f, 1f)
            if (!forceRecovery && drive > 0.01f && sens > 0.01f) {
                val ceilingBpm = RESTING_HEART_RATE_BPM +
                    (MAX_HEART_RATE_BPM - RESTING_HEART_RATE_BPM) * (0.18f + 0.82f * sens)
                if (currentBpm < ceilingBpm) {
                    val riseRate = ((0.055f + 0.215f * sens) * (0.35f + 0.65f * drive)) / HEART_RATE_RISE_RATE_DIVISOR
                    val next = currentBpm + (ceilingBpm - currentBpm) * (1f - kotlin.math.exp(-dt * riseRate))
                    return next.coerceIn(RESTING_HEART_RATE_BPM, safeMax)
                }
                // Gentle settle if currently above this lower-sensitivity zone's ceiling
                val driftRate = 0.025f
                return (currentBpm + (ceilingBpm - currentBpm) * (1f - kotlin.math.exp(-dt * driftRate)))
                    .coerceIn(RESTING_HEART_RATE_BPM, safeMax)
            }
            // Brief ~1.2s sympathetic inertia after touch stops, then smooth parasympathetic recovery
            // (post-peak cooldown passes recoveryRateMultiplier = 2.0f, forceRecovery = true, recoveryFloorBpm = 125f)
            val floorBpm = recoveryFloorBpm.coerceIn(RESTING_HEART_RATE_BPM, safeMax)
            if (currentBpm <= floorBpm) return currentBpm.coerceIn(RESTING_HEART_RATE_BPM, safeMax)
            val recoveryRamp = if (forceRecovery) 1f else ((secondsSinceLastStim - 1.1f) / 1.4f).coerceIn(0.10f, 1f)
            val tauSec = 17.0f / recoveryRateMultiplier.coerceAtLeast(0.1f)
            val decayAlpha = 1f - kotlin.math.exp(-dt * recoveryRamp / tauSec)
            val next = currentBpm + (RESTING_HEART_RATE_BPM - currentBpm) * decayAlpha
            return if (next - floorBpm < 0.05f) floorBpm else next
        }

        /**
         * Cardio-respiratory frequency coupling:
         *  - Calm rest (66 BPM): 0.235 Hz (~14.1 breaths/min, matches rest baseline)
         *  - Moderate exertion (~104 BPM): ~0.358 Hz (~21.5 breaths/min)
         *  - Peak exertion (142 BPM): ~0.520 Hz (~31.2 breaths/min, unmistakably rapid breathing)
         */
        internal fun breathFrequencyHzForHeartRate(bpm: Float, stimulus: Float = 0f): Float {
            val hrNorm = heartRateExertionNorm(bpm)
            val s = stimulus.coerceIn(0f, 1f)
            val restFreq = breathFrequencyHz(s)
            val hrFreq = 0.235f + 0.285f * hrNorm * (0.72f + 0.28f * hrNorm)
            return kotlin.math.max(restFreq, hrFreq)
        }

        /**
         * Exertion-modulated respiratory waveform: at calm rest (hrNorm == 0) reproduces
         * evaluateBreathWave bit-identically; as heart rate climbs, the resting pause shortens
         * smoothly and inhale/exhale deepen into continuous heavy breathing.
         */
        internal fun evaluateExertionBreathWave(cycle01: Float, hrNorm: Float): Float {
            val h = hrNorm.coerceIn(0f, 1f)
            if (h <= 1e-4f) return evaluateBreathWave(cycle01)
            val t = ((cycle01 % 1f) + 1f) % 1f
            val inhaleEnd = 0.40f + 0.05f * h
            val exhaleEnd = 0.84f + 0.11f * h
            return when {
                t < inhaleEnd -> {
                    val u = t / inhaleEnd
                    (0.5f - 0.5f * cos(u * Math.PI.toFloat())).coerceIn(0f, 1f)
                }
                t < exhaleEnd -> {
                    val u = (t - inhaleEnd) / (exhaleEnd - inhaleEnd)
                    (0.5f + 0.5f * cos(u * Math.PI.toFloat())).coerceIn(0f, 1f)
                }
                else -> 0f
            }
        }

        /**
         * Heart-rate-coupled chest & abdominal respiratory displacement:
         * scales tidal depth and thoracic accessory effort smoothly with heart rate while keeping
         * peak ribcage lift anatomically bounded.
         */
        internal fun evaluateHeartRateBreathingDisplacement(
            phaseCycles: Float,
            bpm: Float,
            stimulus: Float = 0f,
            isSighCycle: Boolean = false,
            depthMultiplier: Float = 1f,
            peakMode: Boolean = false,
            forcePowerfulSigh: Boolean = false
        ): Pair<Float, Float> {
            val hrNorm = heartRateExertionNorm(bpm)
            if (hrNorm <= 1e-4f && !peakMode && !forcePowerfulSigh) {
                return if (!isSighCycle && kotlin.math.abs(depthMultiplier - 1f) <= 1e-4f) {
                    evaluateBreathingDisplacement(phaseCycles, stimulus)
                } else {
                    evaluateBreathingDisplacementWithSigh(phaseCycles, stimulus, isSighCycle, depthMultiplier)
                }
            }
            val s = stimulus.coerceIn(0f, 1f)
            val baseMul = depthMultiplier.coerceIn(0.75f, 2.05f)
            val effCycleMul = if (peakMode || forcePowerfulSigh) {
                baseMul
            } else {
                1f + (baseMul - 1f) * (1f - 0.65f * hrNorm)
            }
            val exertionDepth = when {
                forcePowerfulSigh -> (1.0f + 0.16f * hrNorm) * effCycleMul
                peakMode -> (1.0f + 0.72f * kotlin.math.max(hrNorm, 0.90f)) * effCycleMul
                else -> kotlin.math.max(1.0f + 0.24f * s, 1.0f + 0.78f * hrNorm) * effCycleMul
            }
            val useSighWave = isSighCycle || forcePowerfulSigh
            val abdomenWave = if (useSighWave) {
                evaluateSighBreathWave(phaseCycles, true)
            } else {
                evaluateExertionBreathWave(phaseCycles, if (peakMode) 1f else hrNorm)
            }
            val chestLag = (if (useSighWave) 0.028f else 0.04f) * (1f - 0.55f * (if (peakMode) 1f else hrNorm))
            val chestWave = if (useSighWave) {
                evaluateSighBreathWave(phaseCycles - chestLag, true)
            } else {
                evaluateExertionBreathWave(phaseCycles - chestLag, if (peakMode) 1f else hrNorm)
            }
            // High heart rate recruits stronger upper-thoracic (chest) breathing effort
            val chestBoost = when {
                forcePowerfulSigh -> 1.24f
                peakMode -> 1.32f
                else -> (if (isSighCycle) 1.12f else 1.0f) * (1.0f + 0.28f * hrNorm)
            }
            val maxChest = when {
                peakMode -> 0.242f
                forcePowerfulSigh -> 0.220f
                else -> 0.195f
            }
            val maxAbdomen = when {
                peakMode -> 0.262f
                forcePowerfulSigh -> 0.240f
                else -> 0.215f
            }
            val chestRise = (chestWave * 0.072f * exertionDepth * chestBoost).coerceAtMost(maxChest)
            val abdomenRise = (abdomenWave * 0.096f * exertionDepth).coerceAtMost(maxAbdomen)
            return Pair(chestRise, abdomenRise)
        }

        /**
         * Exact CPU mirror of the VERTEX_SHADER respiratory displacement used by unit tests
         * to verify thoracic ribcage expansion and zero differential breast Z-stretching.
         */
        internal fun evaluateVertexBreathingOffset(
            x: Float,
            y: Float,
            z: Float,
            nx: Float,
            ny: Float,
            nz: Float,
            breathChest: Float,
            breathAbdomen: Float
        ): FloatArray {
            fun glSmoothStep(e0: Float, e1: Float, v: Float): Float {
                val t = ((v - e0) / (e1 - e0)).coerceIn(0f, 1f)
                return t * t * (3f - 2f * t)
            }
            val nLen = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
            val nrmZ = nz / nLen
            val ax = kotlin.math.abs(x)
            val sgnX = when {
                x > 0f -> 1f
                x < 0f -> -1f
                else -> 0f
            }
            val zAboveMat = z - BlossomBodyBuilder.mattressSurfaceZ(y)

            val abdLatMask = 1f - glSmoothStep(0.96f, 1.48f, ax)
            val abdFrontMask = glSmoothStep(-0.10f, 0.34f, nrmZ) * glSmoothStep(0.68f, 1.18f, zAboveMat)
            val abdMask = abdLatMask * abdFrontMask
            val abdEnv = glSmoothStep(0.86f, 1.58f, y) * (1f - glSmoothStep(2.42f, 3.02f, y))

            val chestLatMask = 1f - glSmoothStep(1.26f, 1.52f, ax)
            val chestDepthMask = glSmoothStep(0.18f, 0.96f, zAboveMat)
            val chestMask = chestLatMask * chestDepthMask
            val chestEnv = glSmoothStep(2.15f, 3.05f, y) * (1f - glSmoothStep(4.32f, 4.92f, y))
            val ribLatMask = glSmoothStep(0.22f, 0.98f, ax) * (1f - glSmoothStep(1.30f, 1.52f, ax)) *
                glSmoothStep(0.20f, 0.72f, zAboveMat)

            val liftZ = breathAbdomen * abdEnv * abdMask + breathChest * chestEnv * chestMask
            val liftY = breathChest * chestEnv * chestMask * 0.24f - breathAbdomen * abdEnv * abdMask * 0.08f
            val latRib = (breathChest * chestEnv * ribLatMask * 0.36f +
                breathAbdomen * abdEnv * abdMask * glSmoothStep(0.14f, 0.78f, ax) * 0.18f) * sgnX
            return floatArrayOf(latRib, liftY, liftZ)
        }

        /**
         * Exact CPU mirror of the VERTEX_SHADER Lifelike Beta subtle shoulder relaxation offset.
         * Strictly localized to the trapezius ridge (y > 4.56, ax > 0.62) and deltoid/upper-arm
         * shoulder cap (ax > 1.40, y in 3.45..5.18) so breasts, neck, head, forearms & vulva are
         * 100% untouched (0.0f displacement).
         */
        internal fun evaluateVertexShoulderRelaxOffset(
            x: Float,
            y: Float,
            z: Float,
            shoulderRelaxL: Float,
            shoulderRelaxR: Float
        ): FloatArray {
            if (kotlin.math.abs(shoulderRelaxL) <= 1e-4f && kotlin.math.abs(shoulderRelaxR) <= 1e-4f) {
                return floatArrayOf(0f, 0f, 0f)
            }
            fun glSmoothStep(e0: Float, e1: Float, v: Float): Float {
                val t = ((v - e0) / (e1 - e0)).coerceIn(0f, 1f)
                return t * t * (3f - 2f * t)
            }
            val ax = kotlin.math.abs(x)
            val sgnX = when {
                x > 0f -> 1f
                x < 0f -> -1f
                else -> 0f
            }
            val zAboveMat = z - BlossomBodyBuilder.mattressSurfaceZ(y)
            val trapMask = glSmoothStep(0.62f, 1.08f, ax) * (1f - glSmoothStep(1.86f, 2.30f, ax)) *
                glSmoothStep(4.56f, 4.82f, y) * (1f - glSmoothStep(4.98f, 5.22f, y))
            val deltoidMask = glSmoothStep(1.40f, 1.66f, ax) * (1f - glSmoothStep(2.16f, 2.52f, ax)) *
                glSmoothStep(3.45f, 4.08f, y) * (1f - glSmoothStep(4.86f, 5.18f, y))
            val zMask = glSmoothStep(0.14f, 0.56f, zAboveMat)
            val shMask = (trapMask * 0.76f + deltoidMask).coerceIn(0f, 1f) * zMask
            val sideRelax = if (x < 0f) shoulderRelaxL else shoulderRelaxR
            val dx = sideRelax * 0.014f * shMask * sgnX
            val dy = -sideRelax * 0.068f * shMask
            val dz = -sideRelax * 0.036f * shMask
            return floatArrayOf(dx, dy, dz)
        }

        /**
         * Exact CPU mirror of the FRAGMENT_SHADER Tab 3 Lifelike+ subtle capillary warmth mask
         * (localized strictly to cheek apples, ears/temples, and upper chest/décolletage — 0.0 on
         * legs, forearms, abdomen, and pelvic/vulvar tissue).
         */
        internal fun evaluateLivingWarmthMask(
            x: Float,
            y: Float,
            nz: Float,
            mucosa: Float = 0f,
            dryPigmentMask: Float = 0f
        ): Float {
            fun glSmoothStep(e0: Float, e1: Float, v: Float): Float {
                val t = ((v - e0) / (e1 - e0)).coerceIn(0f, 1f)
                return t * t * (3f - 2f * t)
            }
            val ax = kotlin.math.abs(x)
            val isHead = glSmoothStep(5.40f, 5.62f, y)
            val cheekDx = (ax - 0.30f) / 0.16f
            val cheekDy = (y - 6.32f) / 0.17f
            val cheekFlush = isHead * kotlin.math.exp(-(cheekDx * cheekDx + cheekDy * cheekDy)) *
                glSmoothStep(0.10f, 0.55f, nz)
            val earDy = (y - 6.34f) / 0.22f
            val earFlush = isHead * glSmoothStep(0.44f, 0.62f, ax) * kotlin.math.exp(-(earDy * earDy)) * 0.45f
            val chestFlush = glSmoothStep(3.45f, 4.15f, y) * (1f - glSmoothStep(4.78f, 5.25f, y)) *
                (1f - glSmoothStep(0.75f, 1.38f, ax)) * glSmoothStep(0.15f, 0.65f, nz) *
                (1f - dryPigmentMask * 0.75f) * 0.62f
            return ((cheekFlush + earFlush + chestFlush) * (1f - mucosa.coerceIn(0f, 1f))).coerceIn(0f, 1f)
        }

        private const val VERTEX_SHADER = """
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec4 aMat;
            attribute float aArchAlpha;
            attribute vec4 aBodyBindHole;

            uniform mat4 uMvp;
            uniform mat4 uModel;
            uniform float uBreathChest;
            uniform float uBreathAbdomen;
            uniform float uAllowBreathing;
            uniform float uShoulderRelaxL;
            uniform float uShoulderRelaxR;

            varying vec3 vWorldPos;
            varying vec3 vRestPos;
            varying vec3 vNormal;
            varying vec4 vMat;
            varying float vArchAlpha;
            varying vec4 vBodyBindHole;

            float mattressZ(float y) {
                if (y <= 0.72) return -1.14;
                float yRec = y - 0.72;
                if (y <= 3.00) {
                    return -1.14 - 0.16 * yRec - 0.014 * yRec * yRec;
                }
                float dy = y - 3.00;
                return -1.577574 - 0.06 * dy - 0.390095 * (1.0 - exp(-0.42 * dy));
            }

            void main() {
                vec3 pos = aPos;
                vec3 nrm = normalize(aNormal);

                if (uAllowBreathing > 0.5) {
                    // Strictly isolate natural breathing expansion to torso, ribcage, breasts & abdomen
                    // (back remains pinned to exam cushion; arms, hands, head & vulva stay 100% undisturbed)
                    float ax = abs(pos.x);
                    float zAboveMat = pos.z - mattressZ(pos.y);

                    // 1. Soft diaphragmatic abdominal rise (y ~ 0.9..3.0, handing off cleanly to ribcage below breasts)
                    float abdLatMask = 1.0 - smoothstep(0.96, 1.48, ax);
                    float abdFrontMask = smoothstep(-0.10, 0.34, nrm.z) * smoothstep(0.68, 1.18, zAboveMat);
                    float abdMask = abdLatMask * abdFrontMask;
                    float abdEnv = smoothstep(0.86, 1.58, pos.y) * (1.0 - smoothstep(2.42, 3.02, pos.y));

                    // 2. True thoracic ribcage expansion (y ~ 2.2..4.9): depth mask saturates cleanly to 1.0
                    //    AT the anterior chest wall beneath the breasts (zAboveMat >= 0.96, independent of nrm.z!)
                    //    so the entire ribcage expands anteriorly (+Z), superiorly (+Y pump-handle) & laterally
                    //    (±X bucket-handle) while the breasts ride rigidly on the chest with ZERO Z-stretching!
                    float chestLatMask = 1.0 - smoothstep(1.26, 1.52, ax);
                    float chestDepthMask = smoothstep(0.18, 0.96, zAboveMat);
                    float chestMask = chestLatMask * chestDepthMask;
                    float chestEnv = smoothstep(2.15, 3.05, pos.y) * (1.0 - smoothstep(4.32, 4.92, pos.y));
                    float ribLatMask = smoothstep(0.22, 0.98, ax) * (1.0 - smoothstep(1.30, 1.52, ax)) *
                                       smoothstep(0.20, 0.72, zAboveMat);

                    float liftZ = uBreathAbdomen * abdEnv * abdMask + uBreathChest * chestEnv * chestMask;
                    float liftY = uBreathChest * chestEnv * chestMask * 0.24 - uBreathAbdomen * abdEnv * abdMask * 0.08;
                    float latRib = (uBreathChest * chestEnv * ribLatMask * 0.36 +
                                    uBreathAbdomen * abdEnv * abdMask * smoothstep(0.14, 0.78, ax) * 0.18) * sign(pos.x);

                    pos += vec3(latRib, liftY, liftZ);
                    // Subtle matching normal breath on abdomen & ribcage without warping breast specular shape
                    nrm = normalize(nrm + vec3(
                        latRib * 0.48,
                        uBreathAbdomen * abdEnv * abdMask * 0.58 - uBreathChest * chestEnv * chestMask * 0.24,
                        0.0
                    ));

                    // Lifelike Beta: Subtle asymmetric shoulder / trapezius / deltoid relaxation (0.0 when disabled)
                    if (abs(uShoulderRelaxL) > 0.0001 || abs(uShoulderRelaxR) > 0.0001) {
                        float trapMask = smoothstep(0.62, 1.08, ax) * (1.0 - smoothstep(1.86, 2.30, ax)) *
                                         smoothstep(4.56, 4.82, aPos.y) * (1.0 - smoothstep(4.98, 5.22, aPos.y));
                        float deltoidMask = smoothstep(1.40, 1.66, ax) * (1.0 - smoothstep(2.16, 2.52, ax)) *
                                            smoothstep(3.45, 4.08, aPos.y) * (1.0 - smoothstep(4.86, 5.18, aPos.y));
                        float shZMask = smoothstep(0.14, 0.56, zAboveMat);
                        float shMask = clamp(trapMask * 0.76 + deltoidMask, 0.0, 1.0) * shZMask;
                        float sideRelax = (aPos.x < 0.0) ? uShoulderRelaxL : uShoulderRelaxR;
                        pos += vec3(
                            sideRelax * 0.014 * shMask * sign(aPos.x),
                            -sideRelax * 0.068 * shMask,
                            -sideRelax * 0.036 * shMask
                        );
                    }
                }

                vec4 worldPos = uModel * vec4(pos, 1.0);
                vWorldPos = worldPos.xyz;
                vRestPos = (uModel * vec4(aPos, 1.0)).xyz;
                vNormal = nrm;
                vMat = aMat;
                vArchAlpha = aArchAlpha;
                vBodyBindHole = aBodyBindHole;
                gl_Position = uMvp * vec4(pos, 1.0);
            }
        """

        /**
         * Photorealistic Skin, Wet Mucosa & Draped Clinical Linen Shader:
         *  - True RGB-Wavelength Pre-Integrated Subsurface Scattering (independent R/G/B
         *    dermal diffusion profiles + thin-tissue forward/back translucency)
         *  - Colored hemoglobin ambient crevice bounce (softens skin-to-lip transitions)
         *  - 4-frequency 3D object-space procedural dermal pores & stretch-responsive creases
         *  - Dual-lobe wet mucosal specular BRDF for glistening vestibule & clitoral glans
         *  - Dedicated matte charcoal-plum woven clinical linen pass for Blossom's exam cushion
         */
        private val FRAGMENT_SHADER: String by lazy { FRAGMENT_SHADER_PART_1 + FRAGMENT_SHADER_PART_2 }

        private const val FRAGMENT_SHADER_PART_1 = """
            precision highp float;

            varying vec3 vWorldPos;
            varying vec3 vRestPos;
            varying vec3 vNormal;
            varying vec4 vMat;
            varying float vArchAlpha;
            varying vec4 vBodyBindHole;

            uniform vec3 uCamPos;
            uniform vec3 uSkinBase;
            uniform vec3 uSkinShadow;
            uniform vec3 uMucosaBase;
            uniform vec3 uMucosaDeep;
            uniform vec2 uTouchPos;
            uniform vec3 uTouchColor;
            uniform float uTouchIntensity;
            uniform float uStretch;
            uniform float uLightMode;
            uniform float uMaterialType;
            uniform vec3 uHairBase;
            uniform vec3 uHairHighlight;
            uniform vec3 uHairSheen;
            uniform float uPubicHair;
            uniform float uAllowPubicHair;
            uniform float uBetaSmoothNeckSeam;
            uniform float uBetaSoftHair;
            uniform float uBetaSmoothBodyHole;
            uniform float uSmoothBodyHoleOvalR;
            uniform float uArchAlphaPass;
            uniform float uBetaWarmTransitionZone;
            uniform float uBetaRefineInnerLabia;
            uniform float uBetaV2NaturalPelvicFinish;
            uniform float uBetaLifeWarmth;
            uniform float uBetaV3SoftPigmentation;
            uniform float uBetaV3RegionalMaterial;
            uniform float uBetaV3LabialVestibuleBlend;
            uniform float uBetaV3IntricateLabiaFolds;
            uniform float uBetaV3MedialRootContinuity;
            uniform float uIsLabiaArchMesh;

            float hash31(vec3 p) {
                p = fract(p * vec3(0.1031, 0.11369, 0.13787));
                p += dot(p, p.yzx + 19.19);
                return fract((p.x + p.y) * p.z);
            }

            float vnoise3(vec3 p) {
                vec3 i = floor(p);
                vec3 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float n000 = hash31(i + vec3(0.0, 0.0, 0.0));
                float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
                float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
                float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
                float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
                float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
                float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
                float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
                float nx00 = mix(n000, n100, f.x);
                float nx10 = mix(n010, n110, f.x);
                float nx01 = mix(n001, n101, f.x);
                float nx11 = mix(n011, n111, f.x);
                float nxy0 = mix(nx00, nx10, f.y);
                float nxy1 = mix(nx01, nx11, f.y);
                return mix(nxy0, nxy1, f.z);
            }

            // Procedural multi-scale skin, velvety mucosal & fine labial crease micro-heightfield in true 3D object space
            float skinDetailHeight(vec3 p, float mucosa, float pigment, float stretch) {
                float pores = vnoise3(p * 78.0) * 0.46 + vnoise3(p * 165.0) * 0.30;
                float fineWeave = vnoise3(p * vec3(195.0, 145.0, 195.0)) * 0.12 + vnoise3(p * 300.0) * 0.12;
                float creaseWave = sin(p.y * 58.0 + vnoise3(p * 15.0) * 4.2 + p.x * 22.0);
                float fineLabialStriae = sin(p.y * 132.0 + vnoise3(p * 28.0) * 3.4 - abs(p.x) * 46.0);
                float creaseRidges = ((0.5 + 0.5 * creaseWave) * 0.72 + (0.5 + 0.5 * fineLabialStriae) * 0.28) *
                                     (0.24 + 0.76 * pigment) * (1.0 - stretch * 0.38);
                float mucosaRipple = vnoise3(p * 38.0) * 0.30 + vnoise3(p * 96.0) * 0.14;
                return mix(pores + fineWeave + creaseRidges * 0.44, mucosaRipple + creaseRidges * 0.26, mucosa);
            }

            // Beta v3 Option 6: multi-scale domain-warped tangled folds & delicate plicae for inner labia minora
            // Uses asymmetric signed coordinates so left and right inner labia form unique organic fold networks.
            float labiaTangleHeight(vec3 p) {
                vec3 asymP = vec3(p.x * 1.14 + 0.37, p.y * 1.06 - 0.19, p.z * 1.10);
                vec3 warp1 = vec3(
                    vnoise3(asymP * 11.5 + vec3(1.3, -2.1, 0.7)) - 0.5,
                    vnoise3(asymP * 11.5 + vec3(-2.7, 1.9, -1.1)) - 0.5,
                    vnoise3(asymP * 11.5 + vec3(0.6, 2.4, 2.8)) - 0.5
                );
                vec3 q = asymP + warp1 * 0.135;
                float ridgeA = 1.0 - abs(sin(q.y * 30.0 + q.x * 42.0 + warp1.z * 3.8));
                float ridgeB = 1.0 - abs(sin(q.y * 23.0 - q.x * 52.0 + warp1.x * 4.4 + 1.25));
                vec3 q2 = q * 1.95 + warp1 * 0.28;
                float plicae = 0.5 + 0.5 * sin(q2.y * 34.0 + q2.x * 26.0 + vnoise3(q2 * 14.0) * 4.2);
                float lobules = vnoise3(q * 36.0 + vec3(2.1, -1.4, 0.8));
                float microCrepe = vnoise3(q * 74.0 + vec3(-1.3, 3.2, -0.9));
                float primary = pow(clamp(ridgeA * 0.54 + ridgeB * 0.46, 0.0, 1.0), 1.35);
                return primary * 0.54 + plicae * 0.24 + lobules * 0.14 + microCrepe * 0.08;
            }

            void main() {
                vec3 N = normalize(vNormal);
                if (!gl_FrontFacing) {
                    N = -N;
                }
                vec3 V = normalize(uCamPos - vWorldPos);

                // Shared Studio 3-Point Lighting Rig (Exam Overhead vs Dramatic Raking Side Light)
                vec3 LkeyExam = normalize(vec3(-0.34, 0.54, 0.77));
                vec3 LkeySide = normalize(vec3(-0.76, 0.34, 0.55));
                vec3 Lkey = normalize(mix(LkeyExam, LkeySide, uLightMode));
                vec3 Lfill = normalize(vec3(0.62, -0.22, 0.74));
                vec3 Lrim = normalize(vec3(0.42, 0.28, -0.84));

                // -----------------------------------------------------------------
                // PASS E: NATURAL WARM TRANSLUCENT PINK KERATIN 3D FINGERNAILS & TOENAILS
                // -----------------------------------------------------------------
                if (uMaterialType > 3.5) {
                    float nailV = clamp(vMat.x, 0.0, 1.0); // 0=cuticle -> 1=free tip
                    float nailU = clamp(vMat.y, 0.0, 1.0); // 0=left fold -> 1=right fold
                    float nailAo = clamp(vMat.z, 0.74, 1.0);
                    float isToe = vMat.w;

                    float uMid = abs(nailU - 0.5) * 2.0;
                    // Luminous warm rosy-pink translucent keratin over living capillary nailbed (harmonized with skin SSS — ZERO gray/silver!)
                    vec3 warmPinkTint = vec3(0.99, 0.56, 0.52);
                    vec3 nailBed = mix(uSkinBase * vec3(1.08, 0.80, 0.74), warmPinkTint, 0.44);
                    // Delicate warm peach-cream lunula near cuticle & sheer warm cream free tip (never cool/white/silver!)
                    float lunulaCurve = 0.22 * (1.0 - 0.68 * uMid * uMid) * (1.0 - 0.30 * isToe);
                    float lunulaMask = (1.0 - smoothstep(lunulaCurve - 0.05, lunulaCurve + 0.05, nailV)) * smoothstep(0.0, 0.07, nailV);
                    float tipCurve = 0.83 - 0.05 * uMid * uMid + 0.04 * isToe;
                    float freeEdge = smoothstep(tipCurve - 0.05, tipCurve + 0.06, nailV);
                    vec3 warmCream = vec3(1.00, 0.90, 0.83);

                    vec3 nailAlbedo = mix(nailBed, mix(nailBed, warmCream, 0.26), lunulaMask);
                    nailAlbedo = mix(nailAlbedo, warmCream, freeEdge * 0.28);
                    // Gentle warm lateral cuticle framing (very soft so nail perimeter never reads dark or muddy)
                    float edgeShadow = smoothstep(0.76, 1.0, uMid) * 0.05 + (1.0 - smoothstep(0.0, 0.10, nailV)) * 0.04;
                    nailAlbedo *= (1.0 - edgeShadow);

                    // Living RGB subsurface-scattered nailbed lighting matching Pass A finger skin warmth & brightness
                    float nDotKey = dot(N, Lkey);
                    float nDotFill = dot(N, Lfill);
                    vec3 sssNailKey = vec3(0.62, 0.32, 0.16);
                    vec3 sssNailFill = vec3(0.46, 0.24, 0.13);
                    vec3 diffKeyNail = pow(max(clamp((vec3(nDotKey) + sssNailKey) / (vec3(1.0) + sssNailKey), 0.0, 1.0), vec3(0.0001)), vec3(0.90, 1.04, 1.18));
                    vec3 diffFillNail = pow(max(clamp((vec3(nDotFill) + sssNailFill) / (vec3(1.0) + sssNailFill), 0.0, 1.0), vec3(0.0001)), vec3(0.94, 1.04, 1.16));
                    vec3 nailColorAo = pow(vec3(nailAo), vec3(0.76, 1.00, 1.18));
                    vec3 nailSssGlow = mix(uSkinBase * vec3(1.06, 0.56, 0.48), vec3(0.94, 0.42, 0.36), 0.45);
                    float nailTerminator = exp(-abs(nDotKey - 0.02) * 3.4) * 0.26;

                    vec3 HkeyN = normalize(Lkey + V);
                    vec3 HfillN = normalize(Lfill + V);
                    // Tight, natural healthy keratin gloss (warm cream tint, zero broad metallic/silver Fresnel!)
                    float spec1 = pow(max(dot(N, HkeyN), 0.0), 108.0) * 0.22;
                    float spec2 = pow(max(dot(N, HfillN), 0.0), 52.0) * 0.08;
                    float nailFresnel = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 4.0) * 0.035;

                    vec3 nailLit = nailAlbedo * (0.28 + 0.06 * (N.y * 0.5 + 0.5)) * nailColorAo +
                                   nailAlbedo * (diffKeyNail * 0.80 * vec3(1.03, 0.96, 0.92) + diffFillNail * 0.24 * vec3(0.98, 0.93, 0.90)) * (vec3(0.32) + 0.68 * nailColorAo) +
                                   nailSssGlow * nailTerminator * nailColorAo +
                                   vec3(1.0, 0.92, 0.87) * (spec1 + spec2 + nailFresnel);
                    vec3 nailMapped = nailLit / (nailLit + vec3(0.76));
                    nailMapped = pow(clamp(nailMapped * 1.32, 0.0, 1.0), vec3(1.01));
                    float nailLuma = dot(nailMapped, vec3(0.299, 0.587, 0.114));
                    nailMapped = clamp(mix(vec3(nailLuma), nailMapped, 1.05), 0.0, 1.0);
                    gl_FragColor = vec4(nailMapped, 1.0);
                    return;
                }

                // -----------------------------------------------------------------
                // PASS D: CUSTOMIZABLE SILKY LAYERED BOB HAIR (ANISOTROPIC SILK)
                // -----------------------------------------------------------------
                if (uMaterialType > 2.5) {
                    float strandFlow = vMat.x;
                    float strandRib = vMat.y;
                    float hairAo = clamp(vMat.z, 0.35, 1.0);
                    float lockBias = vMat.w;

                    if (uBetaSoftHair > 0.5) {
                        // Beta 6: Exact baseline hair silhouette + fine strand-direction normal detail,
                        // subtle strand-to-strand highlight/color variation & silky broken-up dual sheen
                        vec3 hp = vRestPos;
                        float hEps = 0.0042;
                        vec3 strandFreq1 = vec3(124.0, 22.0, 124.0);
                        vec3 strandFreq2 = vec3(235.0, 42.0, 235.0);
                        float sC = vnoise3(hp * strandFreq1) * 0.62 + vnoise3(hp * strandFreq2) * 0.38;
                        float sX = vnoise3((hp + vec3(hEps, 0.0, 0.0)) * strandFreq1) * 0.62 +
                                   vnoise3((hp + vec3(hEps, 0.0, 0.0)) * strandFreq2) * 0.38;
                        float sY = vnoise3((hp + vec3(0.0, hEps, 0.0)) * strandFreq1) * 0.62 +
                                   vnoise3((hp + vec3(0.0, hEps, 0.0)) * strandFreq2) * 0.38;
                        float sZ = vnoise3((hp + vec3(0.0, 0.0, hEps)) * strandFreq1) * 0.62 +
                                   vnoise3((hp + vec3(0.0, 0.0, hEps)) * strandFreq2) * 0.38;
                        vec3 sRawGrad = vec3(sC - sX, sC - sY, sC - sZ);
                        vec3 sTanGrad = sRawGrad - N * dot(sRawGrad, N);
                        vec3 Nhair = normalize(N + sTanGrad * 0.58);

                        float lockToneVar = vnoise3(hp * vec3(38.0, 8.5, 38.0));
                        float fineStrandTone = sC;
                        float lowlightMask = smoothstep(0.56, 0.22, lockToneVar * 0.58 + fineStrandTone * 0.42);
                        float highlightMask = smoothstep(0.52, 0.88, lockToneVar * 0.45 + fineStrandTone * 0.55);
                        float silkyBandB = 0.5 + 0.5 * sin(strandFlow * 18.0 + fineStrandTone * 5.2 + lockToneVar * 2.8);
                        float tipSoft = smoothstep(0.74, 1.0, strandFlow);

                        vec3 hairAlbedo = mix(uHairBase, uHairHighlight, clamp(strandRib * 0.22 + fineStrandTone * 0.44 + lockBias * 0.48, 0.0, 1.0));
                        hairAlbedo = mix(hairAlbedo, uHairBase * 0.76, lowlightMask * 0.34);
                        hairAlbedo = mix(hairAlbedo, mix(uHairHighlight, uHairSheen, 0.22), highlightMask * 0.28);
                        hairAlbedo *= mix(0.83, 1.07, smoothstep(0.0, 0.34, strandFlow) * (0.76 + 0.24 * silkyBandB));
                        hairAlbedo = mix(hairAlbedo, hairAlbedo * 1.04, tipSoft * 0.35);

                        vec3 flowRef = abs(Nhair.y) < 0.94 ? vec3(0.0, -1.0, 0.0) : vec3(1.0, 0.0, 0.0);
                        vec3 Tstrand = normalize(flowRef - Nhair * dot(flowRef, Nhair));
                        float dKeyH = clamp((dot(Nhair, Lkey) + 0.28) / 1.28, 0.0, 1.0);
                        float dFillH = clamp((dot(Nhair, Lfill) + 0.30) / 1.30, 0.0, 1.0);
                        vec3 HkeyH = normalize(Lkey + V);
                        vec3 HfillH = normalize(Lfill + V);

                        vec3 Nsheen1 = normalize(Nhair + Tstrand * (-0.08 + (fineStrandTone - 0.5) * 0.10));
                        vec3 Nsheen2 = normalize(Nhair + Tstrand * (0.11 + (lockToneVar - 0.5) * 0.08));
                        float strandBreakup = 0.52 + 0.48 * smoothstep(0.24, 0.84, fineStrandTone);
                        float primarySheen = pow(max(dot(Nsheen1, HkeyH), 0.0), 46.0) * (0.20 + 0.26 * strandBreakup) * (1.0 - tipSoft * 0.22);
                        float secondarySheen = (pow(max(dot(Nsheen2, HkeyH), 0.0), 18.0) * 0.13 +
                                                pow(max(dot(Nsheen2, HfillH), 0.0), 18.0) * 0.10) * (0.72 + 0.28 * lockToneVar);
                        float rimSheen = pow(clamp(1.0 - max(dot(Nhair, V), 0.0), 0.0, 1.0), 2.6) * 0.18 * clamp(dot(Nhair, Lrim) * 0.5 + 0.5, 0.2, 1.0);
                        float strandAo = clamp(hairAo * mix(0.88, 1.06, fineStrandTone), 0.32, 1.0);

                        vec3 hairLit = hairAlbedo * (0.26 + dKeyH * 0.72 + dFillH * 0.25) * strandAo +
                                       uHairSheen * primarySheen * strandAo +
                                       uHairHighlight * secondarySheen * 1.15 * strandAo +
                                       uHairSheen * 0.50 * rimSheen * strandAo;
                        vec3 hairMapped = hairLit / (hairLit + vec3(0.72));
                        hairMapped = pow(clamp(hairMapped * 1.26, 0.0, 1.0), vec3(1.01));
                        gl_FragColor = vec4(hairMapped, 1.0);
                        return;
                    }

                    // Fine silky longitudinal micro-strands so hair reads soft & natural instead of sculpted clay
                    float fineStrands = vnoise3(vWorldPos * vec3(56.0, 18.0, 56.0)) * 0.58 +
                                        vnoise3(vWorldPos * vec3(128.0, 34.0, 128.0)) * 0.42;
                    float silkyBand = 0.5 + 0.5 * sin(strandFlow * 16.0 + fineStrands * 4.5);
                    vec3 hairAlbedo = mix(uHairBase, uHairHighlight, clamp(strandRib * 0.28 + fineStrands * 0.44 + lockBias * 0.42, 0.0, 1.0));
                    // Subtle darker root warmth & lighter sun-kissed mid-shaft depth
                    hairAlbedo *= mix(0.84, 1.06, smoothstep(0.0, 0.35, strandFlow) * (0.78 + 0.22 * silkyBand));

                    float dKeyH = clamp((dot(N, Lkey) + 0.28) / 1.28, 0.0, 1.0);
                    float dFillH = clamp((dot(N, Lfill) + 0.30) / 1.30, 0.0, 1.0);
                    vec3 HkeyH = normalize(Lkey + V);
                    vec3 HfillH = normalize(Lfill + V);

                    // Soft dual-lobe studio hair sheen (primary silk + secondary warm strand highlight)
                    float primarySheen = pow(max(dot(N, HkeyH), 0.0), 34.0) * (0.20 + 0.22 * fineStrands);
                    float secondarySheen = pow(max(dot(N, HfillH), 0.0), 16.0) * 0.11;
                    float rimSheen = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 2.5) * 0.19 * clamp(dot(N, Lrim) * 0.5 + 0.5, 0.2, 1.0);

                    vec3 hairLit = hairAlbedo * (0.27 + dKeyH * 0.72 + dFillH * 0.25) * hairAo +
                                   uHairSheen * primarySheen * hairAo +
                                   uHairHighlight * secondarySheen * 1.12 * hairAo +
                                   uHairSheen * 0.55 * rimSheen * hairAo;
                    vec3 hairMapped = hairLit / (hairLit + vec3(0.72));
                    hairMapped = pow(clamp(hairMapped * 1.26, 0.0, 1.0), vec3(1.01));
                    gl_FragColor = vec4(hairMapped, 1.0);
                    return;
                }

                // -----------------------------------------------------------------
                // PASS C: NATURAL SLATE-BLUE ANATOMICAL EYES (VISIBLE SCLERA + REALISTIC IRIS)
                // -----------------------------------------------------------------
                if (uMaterialType > 1.5) {
                    float irisR = vMat.x;
                    float irisAngle = vMat.y * 6.2831853;
                    float lidAo = clamp(vMat.z, 0.40, 1.0);

                    // Natural warm off-white sclera softening gently near medial & lateral corners
                    float cornerWarm = smoothstep(0.36, 0.88, irisR);
                    vec3 scleraCol = mix(vec3(0.89, 0.86, 0.83), vec3(0.80, 0.71, 0.68), cornerWarm * 0.42);

                    // Smaller human-proportioned iris with softer muted slate-blue stroma & delicate radial fibers
                    float fiber = 0.5 + 0.5 * sin(irisAngle * 24.0 + vnoise3(vWorldPos * 85.0) * 5.0);
                    float fineFiber = 0.5 + 0.5 * cos(irisAngle * 46.0 - irisR * 18.0);
                    vec3 deepSlate = vec3(0.11, 0.19, 0.28);
                    vec3 softSlateBlue = vec3(0.26, 0.38, 0.49);
                    vec3 innerHazelGray = vec3(0.38, 0.40, 0.35);
                    float irisGrad = smoothstep(0.11, 0.26, irisR);
                    vec3 irisCol = mix(softSlateBlue, deepSlate, irisGrad * 0.68);
                    irisCol = mix(irisCol, softSlateBlue * 1.12, fiber * 0.28 + fineFiber * 0.14);
                    irisCol = mix(irisCol, innerHazelGray, (1.0 - smoothstep(0.10, 0.18, irisR)) * 0.34);

                    // Soft natural slate-navy limbal ring at realistic outer iris border (r ~ 0.29..0.31)
                    float limbalRing = exp(-pow((irisR - 0.298) / 0.022, 2.0));
                    irisCol = mix(irisCol, vec3(0.07, 0.12, 0.18), limbalRing * 0.60);

                    float inIris = 1.0 - smoothstep(0.286, 0.314, irisR);
                    float inPupil = 1.0 - smoothstep(0.112, 0.138, irisR);

                    vec3 eyeAlbedo = mix(scleraCol, irisCol, inIris);
                    eyeAlbedo = mix(eyeAlbedo, vec3(0.018, 0.019, 0.024), inPupil);

                    float dKeyE = clamp((dot(N, Lkey) + 0.28) / 1.28, 0.0, 1.0);
                    float dFillE = clamp((dot(N, Lfill) + 0.24) / 1.24, 0.0, 1.0);
                    // Gentle natural lower-iris scatter (lifelike without doll-like glow)
                    float irisGlow = inIris * (1.0 - inPupil) * smoothstep(0.24, 0.86, 1.0 - vMat.w) * 0.18;

                    vec3 HkeyE = normalize(Lkey + V);
                    vec3 HfillE = normalize(Lfill + V);
                    // Realistic corneal dome bulge sized to the natural iris for crisp, life-like catchlights
                    float corneaZone = 1.0 - smoothstep(0.24, 0.36, irisR);
                    vec3 corneaOffset = vec3(cos(irisAngle), sin(irisAngle), 0.14) * (irisR * 0.20 * corneaZone);
                    vec3 Ncornea = normalize(N + corneaOffset);
                    float corneaGlint1 = pow(max(dot(Ncornea, HkeyE), 0.0), 156.0) * 1.15;
                    float corneaGlint2 = pow(max(dot(Ncornea, HfillE), 0.0), 96.0) * 0.34;
                    // Delicate lower-lid tear-meniscus moisture sheen along the bottom scleral margin
                    float tearLine = smoothstep(0.56, 0.78, 1.0 - vMat.w) * (1.0 - inIris) * 0.14 * pow(max(dot(N, HkeyE), 0.0), 48.0);

                    vec3 eyeLit = eyeAlbedo * (0.28 + dKeyE * 0.66 + dFillE * 0.22 + irisGlow) * lidAo +
                                  vec3(1.0, 0.99, 0.98) * (corneaGlint1 + corneaGlint2 + tearLine);
                    vec3 eyeMapped = eyeLit / (eyeLit + vec3(0.70));
                    eyeMapped = pow(clamp(eyeMapped * 1.22, 0.0, 1.0), vec3(1.01));
                    gl_FragColor = vec4(eyeMapped, 1.0);
                    return;
                }

                // -----------------------------------------------------------------
                // PASS B: SUBTLE DARK DRAPED CLINICAL EXAM CUSHION (MATTE LINEN)
                // -----------------------------------------------------------------
                if (uMaterialType > 0.5) {
                    float clothAo = clamp(vMat.z, 0.14, 1.0);
                    float foldShade = clamp(vMat.w, 0.0, 1.0);
                    float weaveX = 0.5 + 0.5 * sin(vWorldPos.x * 155.0 + vWorldPos.z * 40.0);
                    float weaveY = 0.5 + 0.5 * sin(vWorldPos.y * 155.0 + vWorldPos.z * 40.0);
                    float weave = mix(0.92, 1.08, weaveX * 0.5 + weaveY * 0.5);
                    float linenMottle = vnoise3(vWorldPos * 9.5);

                    vec3 drapeBase = vec3(0.155, 0.132, 0.148) * weave * mix(0.90, 1.12, linenMottle * 0.65 + foldShade * 0.35);
                    float dKey = clamp((dot(N, Lkey) + 0.18) / 1.18, 0.0, 1.0);
                    float dFill = clamp((dot(N, Lfill) + 0.22) / 1.22, 0.0, 1.0);
                    // Soft matte fabric grazing sheen (micro-fiber velvet scatter, zero glossy specular)
                    float clothSheen = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 2.8) * 0.11;

                    vec3 drapeLit = drapeBase * (0.24 + dKey * 0.66 + dFill * 0.18) * clothAo +
                                    vec3(0.24, 0.20, 0.23) * clothSheen * clothAo;
                    vec3 drapeMapped = drapeLit / (drapeLit + vec3(0.74));
                    drapeMapped = pow(clamp(drapeMapped * 1.28, 0.0, 1.0), vec3(1.02));
                    gl_FragColor = vec4(drapeMapped, 1.0);
                    return;
                }
        """

        private const val FRAGMENT_SHADER_PART_2 = """

                // -----------------------------------------------------------------
                // PASS A: LIVING SKIN & MUCOSA WITH RGB SUBSURFACE SCATTERING (SSS)
                // -----------------------------------------------------------------
                // Standard Beta v1 Seam D: clip anterior body mesh hole along a smooth per-fragment ovalR curve
                // instead of coarse Marching Cubes voxel stair-steps (disabled in Legacy Vulva mode).
                // Evaluated strictly in unposed bind-pose anterior pelvic coordinates (vBodyBindHole) so
                // posed limbs (e.g., strongly squeezed inner thighs passing in front of the pelvis) stay 100% opaque.
                if (uBetaSmoothBodyHole > 0.5) {
                    float holeOvalR = length(vBodyBindHole.xy);
                    if (vBodyBindHole.z > -0.92 && vBodyBindHole.w > 0.04 && holeOvalR < uSmoothBodyHoleOvalR) {
                        discard;
                    }
                }
                float mucosa = clamp(vMat.x, 0.0, 1.0);
                float wetness = clamp(vMat.y, 0.0, 1.0);
                float ao = clamp(vMat.z, 0.06, 1.0);
                float pigment = clamp(vMat.w, 0.0, 1.0);

                // Standard Beta v1 Seam E: two-pass alpha split for labiaHoodArchMesh outer ~20% skirt
                // (Pass 1 = opaque core writing depth; Pass 2 = depth-tested alpha-blended feathered perimeter).
                float archAlpha = clamp(vArchAlpha, 0.0, 1.0);
                if (uArchAlphaPass > 0.5 && uArchAlphaPass < 1.5 && archAlpha < 0.995) {
                    discard;
                }
                if (uArchAlphaPass > 1.5 && (archAlpha >= 0.995 || archAlpha < 0.004)) {
                    discard;
                }

                // Multi-octave 3D procedural skin albedo mottling + subtle living dual-chromophore (hemoglobin/melanin) variation
                // Sampled in undisplaced vRestPos so procedural pores & pubic hair NEVER swim during breathing!
                float nMacro = vnoise3(vRestPos * 3.8);
                float nMid = vnoise3(vRestPos * 14.5);
                float nMicro = vnoise3(vRestPos * 62.0);
                float nHemo = vnoise3(vRestPos * 7.8 + vec3(4.3, -2.7, 1.9));
                float nMelanin = vnoise3(vRestPos * 23.0 + vec3(-3.1, 5.4, 2.2));

                // True 3D tangent-projected micro-normal bump (consistent velvety grain across front, 3/4 & profile views!)
                float eps = 0.0034;
                float hC = skinDetailHeight(vRestPos, mucosa, pigment, uStretch);
                float hX = skinDetailHeight(vRestPos + vec3(eps, 0.0, 0.0), mucosa, pigment, uStretch);
                float hY = skinDetailHeight(vRestPos + vec3(0.0, eps, 0.0), mucosa, pigment, uStretch);
                float hZ = skinDetailHeight(vRestPos + vec3(0.0, 0.0, eps), mucosa, pigment, uStretch);

                // Finer, velvety portrait skin micro-normal on face & chest (prevents coarse orange-peel pores!)
                float isHeadRegion = smoothstep(5.40, 5.62, vRestPos.y);
                float submentalDown = (1.0 - smoothstep(5.66, 5.92, vRestPos.y)) * smoothstep(0.06, -0.44, N.y);
                float faceVelvet = mix(
                    smoothstep(5.25, 5.85, vRestPos.y),
                    smoothstep(4.55, 5.92, vRestPos.y) * (1.0 - submentalDown * 0.85),
                    uBetaSmoothNeckSeam
                );
                float neckSkinBlend = mix(
                    isHeadRegion,
                    smoothstep(4.48, 5.92, vRestPos.y) * (1.0 - submentalDown),
                    uBetaSmoothNeckSeam
                );
                float submentalFeatherZone = uBetaSmoothNeckSeam * submentalDown *
                    smoothstep(4.78, 5.38, vRestPos.y) * (1.0 - smoothstep(5.80, 5.92, vRestPos.y)) *
                    (1.0 - smoothstep(0.42, 0.74, abs(vRestPos.x)));
                float pelvicSkinZone = smoothstep(-1.32, -0.92, vRestPos.y) *
                                       (1.0 - smoothstep(0.92, 1.32, vRestPos.y)) *
                                       (1.0 - smoothstep(0.68, 1.08, abs(vRestPos.x))) *
                                       (1.0 - mucosa);
                float isChestRegion = smoothstep(2.65, 3.10, vRestPos.y) * (1.0 - isHeadRegion);
                float dryPigmentMask = smoothstep(0.14, 0.48, pigment) * (1.0 - smoothstep(0.036, 0.052, wetness)) * isChestRegion;
                float nippleTipMask = smoothstep(0.58, 0.88, pigment) * (1.0 - smoothstep(0.036, 0.052, wetness)) * isChestRegion;
                float bumpGain = (mix(0.128, 0.052, mucosa) + pigment * 0.044 + pelvicSkinZone * 0.042) * (1.0 - faceVelvet * 0.52);
                vec3 rawGrad = vec3(hC - hX, hC - hY, hC - hZ);
                vec3 tanGrad = rawGrad - N * dot(rawGrad, N);
                vec3 microGrad = tanGrad * (bumpGain / eps * 0.0056);
                float camDist = length(uCamPos - vWorldPos);
                // Subtle natural areolar creping & Montgomery tubercle micro-normal detail in chest close-up views
                if (dryPigmentMask > 0.01 && camDist < 5.2) {
                    float closeFadeA = 1.0 - smoothstep(3.8, 5.2, camDist);
                    float aEps = 0.0024;
                    vec3 aP = vRestPos;
                    float aC = vnoise3(aP * 115.0) * 0.55 + vnoise3(aP * 220.0) * 0.30 + smoothstep(0.68, 0.88, vnoise3(aP * 68.0)) * 0.15;
                    float aX = vnoise3((aP + vec3(aEps, 0.0, 0.0)) * 115.0) * 0.55 + vnoise3((aP + vec3(aEps, 0.0, 0.0)) * 220.0) * 0.30 + smoothstep(0.68, 0.88, vnoise3((aP + vec3(aEps, 0.0, 0.0)) * 68.0)) * 0.15;
                    float aY = vnoise3((aP + vec3(0.0, aEps, 0.0)) * 115.0) * 0.55 + vnoise3((aP + vec3(0.0, aEps, 0.0)) * 220.0) * 0.30 + smoothstep(0.68, 0.88, vnoise3((aP + vec3(0.0, aEps, 0.0)) * 68.0)) * 0.15;
                    float aZ = vnoise3((aP + vec3(0.0, 0.0, aEps)) * 115.0) * 0.55 + vnoise3((aP + vec3(0.0, 0.0, aEps)) * 220.0) * 0.30 + smoothstep(0.68, 0.88, vnoise3((aP + vec3(0.0, 0.0, aEps)) * 68.0)) * 0.15;
                    vec3 aRaw = vec3(aC - aX, aC - aY, aC - aZ);
                    vec3 aTan = aRaw - N * dot(aRaw, N);
                    microGrad += aTan * (dryPigmentMask * (0.28 + nippleTipMask * 0.14) * closeFadeA);
                }
                // Fine dermal skin-line & pore micro-normal relief across majora, mons & perineum in close-up views
                if (pelvicSkinZone > 0.01 && camDist < 4.6) {
                    float closeFadeP = 1.0 - smoothstep(3.4, 4.6, camDist);
                    float dEps = 0.0026;
                    vec3 pA = vRestPos;
                    float dC = vnoise3(pA * 142.0) * 0.52 + vnoise3(pA * vec3(105.0, 215.0, 140.0)) * 0.30 +
                               (0.5 + 0.5 * sin(pA.x * 165.0 + vnoise3(pA * 34.0) * 4.5 + pA.y * 52.0)) * 0.18;
                    vec3 pAx = pA + vec3(dEps, 0.0, 0.0);
                    float dX = vnoise3(pAx * 142.0) * 0.52 + vnoise3(pAx * vec3(105.0, 215.0, 140.0)) * 0.30 +
                               (0.5 + 0.5 * sin(pAx.x * 165.0 + vnoise3(pAx * 34.0) * 4.5 + pAx.y * 52.0)) * 0.18;
                    vec3 pAy = pA + vec3(0.0, dEps, 0.0);
                    float dY = vnoise3(pAy * 142.0) * 0.52 + vnoise3(pAy * vec3(105.0, 215.0, 140.0)) * 0.30 +
                               (0.5 + 0.5 * sin(pAy.x * 165.0 + vnoise3(pAy * 34.0) * 4.5 + pAy.y * 52.0)) * 0.18;
                    vec3 pAz = pA + vec3(0.0, 0.0, dEps);
                    float dZ = vnoise3(pAz * 142.0) * 0.52 + vnoise3(pAz * vec3(105.0, 215.0, 140.0)) * 0.30 +
                               (0.5 + 0.5 * sin(pAz.x * 165.0 + vnoise3(pAz * 34.0) * 4.5 + pAz.y * 52.0)) * 0.18;
                    vec3 dRaw = vec3(dC - dX, dC - dY, dC - dZ);
                    vec3 dTan = dRaw - N * dot(dRaw, N);
                    microGrad += dTan * (pelvicSkinZone * 0.24 * closeFadeP);
                }
                // Beta v3 Toggle 4 (Regional Curvature & Material Differentiation):
                // Differentiate micro-normal grain by anatomical zone (fine Langer's lines on outer majora/mons,
                // silky-smooth attenuation on mucosal vestibule & clitoral glans — strictly pelvic-scoped!).
                float pelvicRegionMask = smoothstep(-1.22, -0.84, vRestPos.y) *
                                         (1.0 - smoothstep(0.84, 1.22, vRestPos.y)) *
                                         (1.0 - smoothstep(0.66, 1.02, abs(vRestPos.x)));
                float v3MatStr = uBetaV3RegionalMaterial * pelvicRegionMask;
                float v3ZoneOuter = pelvicRegionMask * (1.0 - smoothstep(0.04, 0.24, mucosa));
                float v3ZoneTrans = pelvicRegionMask * smoothstep(0.04, 0.26, mucosa) * (1.0 - smoothstep(0.42, 0.72, mucosa));
                float v3ZoneMucosa = pelvicRegionMask * smoothstep(0.45, 0.85, mucosa) * (1.0 - smoothstep(0.86, 0.94, wetness));
                float v3ZoneGlans = pelvicRegionMask * smoothstep(0.86, 0.94, wetness) * smoothstep(0.85, 0.98, mucosa);
                if (v3MatStr > 0.001) {
                    // Damp coarse pore normal bump on smooth non-keratinized mucosa & pearl glans (safely floored at 3x!)
                    float v3PoreDamp = clamp(1.0 - (v3ZoneMucosa * 0.42 + v3ZoneGlans * 0.56) * min(uBetaV3RegionalMaterial, 1.65), 0.12, 1.0);
                    microGrad *= v3PoreDamp;
                    // Add delicate diagonal Langer's tension lines on dry outer cutaneous pelvic skin in close-ups
                    if (v3ZoneOuter > 0.01 && camDist < 4.8) {
                        float closeL = 1.0 - smoothstep(3.4, 4.8, camDist);
                        float sideSign = (vRestPos.x >= 0.0) ? 1.0 : -1.0;
                        float lPhase = (vRestPos.x * 118.0 + sideSign * vRestPos.y * 54.0) + vnoise3(vRestPos * 18.0) * 3.2;
                        float lLine = sin(lPhase) * 0.5 + sin(lPhase * 1.92 + 1.1) * 0.25;
                        vec3 lDir = normalize(vec3(1.0, sideSign * 0.45, 0.0));
                        vec3 lTan = lDir - N * dot(lDir, N);
                        microGrad += lTan * (lLine * 0.026 * v3ZoneOuter * uBetaV3RegionalMaterial * closeL);
                    }
                }
                // Beta v3 Option 6 (Intricate Labial Folds & Wavy Organic Border):
                // Sculpt rich, asymmetric tangled folds & delicate plicae across the inner labia minora,
                // plus soft organic border relief along the wavy labial-vestibular transition.
                float v3FoldStr = uBetaV3IntricateLabiaFolds * pelvicRegionMask;
                float innerLabiaFoldsMask = uIsLabiaArchMesh * smoothstep(0.14, 0.52, mucosa) * smoothstep(0.08, 0.52, archAlpha);
                float tangleHeightC = 0.5;
                float tangleActive = 0.0;
                if (v3FoldStr > 0.001) {
                    float mucEdgeBand = 4.0 * mucosa * (1.0 - mucosa);
                    float borderReliefMask = (1.0 - uIsLabiaArchMesh) * mucEdgeBand * smoothstep(0.16, 0.62, mucosa) * 0.42;
                    tangleActive = clamp(innerLabiaFoldsMask + borderReliefMask, 0.0, 1.0) * v3FoldStr;
                    if (tangleActive > 0.004) {
                        float fEps = 0.0032;
                        tangleHeightC = labiaTangleHeight(vRestPos);
                        float fX = labiaTangleHeight(vRestPos + vec3(fEps, 0.0, 0.0));
                        float fY = labiaTangleHeight(vRestPos + vec3(0.0, fEps, 0.0));
                        float fZ = labiaTangleHeight(vRestPos + vec3(0.0, 0.0, fEps));
                        vec3 fRaw = vec3(tangleHeightC - fX, tangleHeightC - fY, tangleHeightC - fZ);
                        vec3 fTan = fRaw - N * dot(fRaw, N);
                        microGrad += fTan * (tangleActive * (0.29 / fEps * 0.0052));
                        // Organic moisture variation across inner labial folds: dewy pooling in crevices, satin on raised ridges
                        float wetPocket = vnoise3(vRestPos * vec3(13.0, 17.0, 13.0) + vec3(2.4, -1.7, 0.6));
                        float creasePool = clamp(1.0 - tangleHeightC * 1.45, -0.35, 0.65);
                        float wetDelta = (creasePool * 0.34 + (wetPocket - 0.46) * 0.42) * innerLabiaFoldsMask * v3FoldStr;
                        wetness = clamp(wetness + wetDelta, 0.02, 0.98);
                    }
                }
                N = normalize(N + microGrad);

                // Subtle cavity micro-occlusion inside pores & delicate labial folds (softer on facial skin)
                float microAo = mix(mix(0.895, 0.958, faceVelvet), 1.032, hC);

                // Harmonized organic skin & mucosa albedo — keeps body, areolae, face, outer labia & inner lips living & cohesive
                vec3 skinMottle = mix(uSkinBase * 0.94, uSkinBase * 1.05, nMacro * 0.60 + nMid * 0.40);
                // Delicate living capillary & melanin micro-variation (slightly enriched vascular warmth across pelvic skin)
                float chromoStrength = (1.0 - faceVelvet * 0.45) * (1.0 - mucosa);
                vec3 hemoShift = mix(vec3(0.986, 1.004, 1.007), vec3(1.030, 0.978, 0.976), nHemo);
                float melaninShift = mix(0.974, 1.024, nMelanin);
                skinMottle *= mix(vec3(1.0), hemoShift * melaninShift, chromoStrength * (0.82 + pelvicSkinZone * 0.28));
                vec3 warmBlush = mix(skinMottle, skinMottle * vec3(1.05, 0.94, 0.92), 0.25 + nMid * 0.25 + pelvicSkinZone * 0.08 * nHemo);
                // Seam F (betaWarmTransitionZone): labiaHoodArchMesh now uses outward CCW winding (gl_FrontFacing = true),
                // plus a gentle warm-hue safeguard on its low-mucosa outer skin skirt so it converges
                // cleanly into surrounding skin without lilac shift while keeping labia interior colors untouched.
                float labiaInnerMask = uBetaRefineInnerLabia * smoothstep(0.06, 0.32, mucosa);
                float archWarmLower = mix(0.18, 0.10, uBetaRefineInnerLabia);
                float archWarmUpper = mix(0.48, 0.28, uBetaRefineInnerLabia);
                float archWarmMask = uBetaWarmTransitionZone * (1.0 - smoothstep(archWarmLower, archWarmUpper, mucosa));
                float ringSoftAo = mix(ao, mix(ao, 1.0, 0.28), archWarmMask);

                // Beta v3 Toggle 2 (Soft Spatially Varying Pigmentation, scaled 3x: 0..3.0):
                // Organic low-frequency domain warp of mucocutaneous & pigment transition zones plus subtle
                // vascular/melanin gradients strictly inside the pelvic region (0.0 when OFF -> bit-identical).
                float v3PigStr = uBetaV3SoftPigmentation * pelvicRegionMask;
                float pigmentColor = pigment;
                float mucosaColor = mucosa;
                float v3OrgWarp = 0.0;
                if (v3PigStr > 0.001) {
                    v3OrgWarp = (vnoise3(vRestPos * 5.8 + vec3(1.7, -2.3, 0.9)) - 0.5) * 0.62 + (nHemo - 0.5) * 0.38;
                    float v3OrgFine = vnoise3(vRestPos * 16.5 + vec3(-2.4, 3.1, 1.5)) - 0.5;
                    float mucBand = 4.0 * mucosa * (1.0 - mucosa);
                    mucosaColor = clamp(mucosa + v3OrgWarp * 0.26 * mucBand * v3PigStr, 0.0, 1.0);
                    float pigMod = clamp(1.0 + (v3OrgWarp * 0.44 + v3OrgFine * 0.22) * v3PigStr, 0.15, 2.25);
                    pigmentColor = clamp(pigment * pigMod, 0.0, 1.0);
                }
                // Beta v3 Option 6: wavy, irregular, asymmetric labial-vestibular color boundary modulation
                if (v3FoldStr > 0.001) {
                    float mucBandF = 4.0 * mucosaColor * (1.0 - mucosaColor);
                    float borderMeander =
                        (vnoise3(vec3(vRestPos.x * 9.5 + 0.72, vRestPos.y * 12.8 - 0.35, vRestPos.z * 9.0)) - 0.5) * 0.65 +
                        (vnoise3(vec3(vRestPos.x * 24.0 - 1.4, vRestPos.y * 29.0 + 0.9, vRestPos.z * 21.0)) - 0.5) * 0.35;
                    mucosaColor = clamp(mucosaColor + borderMeander * 0.54 * mucBandF * v3FoldStr, 0.0, 1.0);
                    pigmentColor = clamp(pigmentColor + borderMeander * 0.22 * innerLabiaFoldsMask * v3FoldStr, 0.0, 1.0);
                }

                // Skin-harmonized pigmentation across body and pelvic skin
                vec3 defaultPigmentTarget = mix(uSkinShadow * 0.93, uMucosaBase * 0.90, 0.44);
                vec3 warmRingPigmentTarget = mix(uSkinBase * vec3(0.96, 0.87, 0.80), uSkinShadow * vec3(1.03, 0.95, 0.88), 0.55);
                vec3 pigmentTarget = mix(defaultPigmentTarget, warmRingPigmentTarget, archWarmMask);
                float pigmentWeight = mix(0.38 + pelvicSkinZone * 0.14, 0.30, archWarmMask) * (1.0 - neckSkinBlend * 0.65);
                vec3 pigmentedSkin = mix(warmBlush, pigmentTarget, pigmentColor * pigmentWeight);
                // Darker, more naturally visible v6.3 areola & nipple pigmentation (warm dusty rose-terracotta adapting smoothly to skin tone)
                vec3 warmRoseDeep = mix(uMucosaBase * 0.68, uMucosaDeep * 1.15, 0.35);
                vec3 areolaBaseTone = mix(uSkinShadow * vec3(0.74, 0.58, 0.54), warmRoseDeep, 0.42);
                vec3 areolaMottle = mix(areolaBaseTone * 0.92, areolaBaseTone * 1.08, nMid * 0.55 + nMelanin * 0.45);
                vec3 nippleDeepTone = areolaBaseTone * vec3(0.84, 0.78, 0.78);
                vec3 areolaFinalTone = mix(areolaMottle, nippleDeepTone, nippleTipMask * 0.65);
                pigmentedSkin = mix(pigmentedSkin, areolaFinalTone, dryPigmentMask * 0.80);

                // Soft velvety vascular harmonization between surrounding skin and labial / facial lip mucosa
                vec3 harmonizedMucosa = mix(uSkinBase * vec3(1.03, 0.88, 0.88), uMucosaBase, 0.64);
                // Soft, natural skin-harmonized muted rose facial lip tone (never artificial dark/injected!)
                vec3 facialLipTone = mix(uSkinBase * vec3(1.02, 0.81, 0.81), uMucosaBase * vec3(1.04, 0.88, 0.90), 0.52);
                vec3 baseMucosaTone = mix(harmonizedMucosa, facialLipTone, isHeadRegion);
                float mucosaVarLo = mix(0.93, 0.97, labiaInnerMask);
                float mucosaVarHi = mix(1.07, 1.03, labiaInnerMask);
                vec3 mucosaVar = mix(baseMucosaTone * mucosaVarLo, baseMucosaTone * mucosaVarHi, nMid * 0.45 + nHemo * 0.35 + 0.15);
                vec3 mucosaRich = mix(mucosaVar, uMucosaDeep, (1.0 - ringSoftAo) * (0.50 - 0.24 * isHeadRegion - 0.28 * isChestRegion) * (1.0 - archWarmMask * 0.65));
                // Natural darker pigmentation concentrated subtly along the labia minora free edge (strictly pelvic region)
                float isPelvicMucosa = 1.0 - smoothstep(1.2, 2.0, vRestPos.y);
                vec3 defaultRimTarget = mix(uMucosaDeep * 0.86, uSkinShadow * 0.82, 0.48);
                vec3 warmRimTarget = mix(uSkinBase * vec3(0.98, 0.80, 0.74), uSkinShadow * vec3(1.05, 0.82, 0.75), 0.45);
                vec3 rimTarget = mix(defaultRimTarget, warmRimTarget, archWarmMask);
                vec3 mucosaRimmed = mix(mucosaRich, rimTarget, pigmentColor * 0.34 * isPelvicMucosa * (1.0 - archWarmMask * 0.35));
                vec3 warmTransitionMucosa = mix(warmBlush * vec3(1.02, 0.87, 0.79), uSkinShadow * vec3(1.06, 0.86, 0.77), 0.35);
                mucosaRimmed = mix(mucosaRimmed, warmTransitionMucosa, archWarmMask * 0.55);
                // Keep facial skin (low mucosa) 100% matched to neck/chest brightness and neutral hue!
                float faceLipActive = smoothstep(0.06, 0.22, mucosa);
                vec3 faceHarmonizedSkin = mix(pigmentedSkin, warmBlush * vec3(1.01, 1.01, 1.00), neckSkinBlend * (1.0 - faceLipActive) * 0.45);
                // Beta 5: gently warm-feather the downward-facing submental band under the chin into upper-neck tone (zero change on face y >= 5.92)
                faceHarmonizedSkin = mix(faceHarmonizedSkin, faceHarmonizedSkin * vec3(0.955, 0.918, 0.902), submentalFeatherZone * (1.0 - faceLipActive) * 0.48);
                vec3 albedo = mix(faceHarmonizedSkin, mucosaRimmed, mucosaColor);
                // Beta v3 Option 5 (Smooth Labial-Vestibule Base Color Blend):
                // Attenuate labia-arch-only darkening/browning strictly where the medial base meets the vestibule,
                // and harmonize both sides of the junction into the exact same warm mucosal rose tone.
                float v3BlendStr = uBetaV3LabialVestibuleBlend * pelvicRegionMask;
                float v3MedRootStr = uBetaV3MedialRootContinuity * pelvicRegionMask;
                float vestBaseMask = uIsLabiaArchMesh *
                    smoothstep(0.42, 0.88, mucosaColor) *
                    (1.0 - smoothstep(0.16, 0.52, pigmentColor));
                // Beta v3 Option 7 (v22.0): when the underlying patch is labia-aligned, ensure the medial
                // junction on labiaHoodArchMesh sheds arch-only darkening cleanly (even when Option 5 is OFF)
                // AND taper seamFShadeFade smoothly on the low-mucosa outer cutaneous skirt so it merges into
                // blossomVulvaPatchMesh with zero outer brightness step!
                float seamFOuterSkirtFade = mix(1.0, smoothstep(0.12, 0.34, mucosaColor), v3MedRootStr);
                // Seam F (uBetaWarmTransitionZone): deepen the newly front-lit labiaHoodArchMesh by ~1 warm skin-tone
                // shade (proportional across light and dark skin tones), fading cleanly to 0 along the outer Seam E skirt.
                float seamFShadeFade = smoothstep(0.0, 0.55, archAlpha) * smoothstep(0.06, 0.28, mucosaColor + pigmentColor * 0.65);
                seamFShadeFade *= seamFOuterSkirtFade;
                seamFShadeFade *= (1.0 - vestBaseMask * max(v3BlendStr, v3MedRootStr) * 0.94);
                vec3 seamFShadeTarget = mix(albedo * vec3(0.84, 0.77, 0.72), uSkinShadow * vec3(0.96, 0.90, 0.86), 0.24 * (1.0 - mucosaColor));
                albedo = mix(albedo, seamFShadeTarget, uBetaWarmTransitionZone * seamFShadeFade);
                // 3G (uBetaRefineInnerLabia): very faint natural warm rose-brown tissue tint on inner labia,
                // strongest along the rolled free edges (high pigment) and fading smoothly to 0 before the outer skirt.
                float innerLabiaFade = smoothstep(0.05, 0.42, mucosaColor);
                float freeEdgeWeight = smoothstep(0.14, 0.56, pigmentColor) * smoothstep(0.06, 0.34, mucosaColor);
                float roseBrownStrength = uBetaRefineInnerLabia * clamp(innerLabiaFade * 0.11 + freeEdgeWeight * 0.19, 0.0, 0.28);
                roseBrownStrength *= (1.0 - vestBaseMask * max(v3BlendStr, v3MedRootStr) * 0.92);
                vec3 warmRoseBrown = mix(uSkinShadow * vec3(0.84, 0.64, 0.56), uMucosaDeep * vec3(0.96, 0.78, 0.70), 0.38);
                albedo = mix(albedo, warmRoseBrown, roseBrownStrength);
                float junctionHarmonyStr = max(v3BlendStr, v3MedRootStr * 0.88);
                if (junctionHarmonyStr > 0.001) {
                    vec3 vestibuleHarmonyTone = mix(mucosaVar, uMucosaDeep, (1.0 - clamp(ao, 0.48, 1.0)) * 0.26);
                    albedo = mix(albedo, vestibuleHarmonyTone, vestBaseMask * junctionHarmonyStr * 0.84);
                    // Gently harmonize the outer vestibule floor margin on the patch side so both sides meet seamlessly
                    float vestOuterRim = (1.0 - uIsLabiaArchMesh) *
                        smoothstep(0.38, 0.88, mucosaColor) *
                        (1.0 - smoothstep(0.84, 0.97, wetness)) *
                        smoothstep(0.24, 0.68, ao);
                    albedo = mix(albedo, vestibuleHarmonyTone, vestOuterRim * junctionHarmonyStr * 0.34);
                }
                if (v3PigStr > 0.001) {
                    // Organic subcutaneous vascular transition & gentle non-uniform melanin tone modulation (safely clamped at 3x)
                    float mucBand = 4.0 * mucosaColor * (1.0 - mucosaColor);
                    vec3 vascularDrift = mix(albedo * vec3(1.03, 0.95, 0.94), mix(uSkinShadow, uMucosaBase, 0.45), 0.24);
                    albedo = mix(albedo, vascularDrift, clamp(mucBand * v3PigStr * (0.20 + 0.18 * nHemo), 0.0, 0.82));
                    vec3 melaninDrift = mix(albedo * vec3(0.955, 0.935, 0.925), albedo * vec3(1.035, 1.015, 1.005), clamp(0.5 + v3OrgWarp * 1.15, 0.0, 1.0));
                    albedo = mix(albedo, melaninDrift, clamp(v3PigStr * (0.30 + 0.20 * pigmentColor), 0.0, 0.85));
                }
                // Beta v3 Option 6: variegated light/rosy pigmentation & delicate tonal mottling across tangled inner labial folds
                if (v3FoldStr > 0.001 && innerLabiaFoldsMask > 0.005) {
                    float pigPatch = vnoise3(vRestPos * vec3(16.0, 21.0, 14.0) + vec3(-1.9, 2.6, 1.1));
                    float ridgeLight = smoothstep(0.46, 0.84, tangleHeightC);
                    float creaseRosy = 1.0 - smoothstep(0.18, 0.56, tangleHeightC);
                    vec3 lightTranslucentPink = mix(albedo * vec3(1.07, 0.99, 0.99), mix(uSkinBase, uMucosaBase, 0.56) * vec3(1.06, 0.95, 0.96), 0.34);
                    vec3 rosyCreaseTone = mix(albedo * vec3(0.96, 0.85, 0.86), uMucosaDeep * vec3(1.08, 0.94, 0.94), 0.28);
                    vec3 dappledPigTone = mix(albedo * vec3(0.93, 0.87, 0.85), albedo * vec3(1.05, 0.97, 0.96), pigPatch);
                    float fWeight = innerLabiaFoldsMask * v3FoldStr;
                    albedo = mix(albedo, lightTranslucentPink, ridgeLight * fWeight * 0.42);
                    albedo = mix(albedo, rosyCreaseTone, creaseRosy * fWeight * 0.38);
                    albedo = mix(albedo, dappledPigTone, fWeight * 0.36);
                }

                // Tab 3 Lifelike+: Subtle living capillary warmth on cheeks, ears & upper chest (0.0 when OFF -> bit-identical)
                if (uBetaLifeWarmth > 0.001) {
                    float axW = abs(vRestPos.x);
                    float cheekFlush = isHeadRegion *
                        exp(-pow((axW - 0.30) / 0.16, 2.0) - pow((vRestPos.y - 6.32) / 0.17, 2.0)) *
                        smoothstep(0.10, 0.55, N.z);
                    float earFlush = isHeadRegion *
                        smoothstep(0.44, 0.62, axW) * exp(-pow((vRestPos.y - 6.34) / 0.22, 2.0)) * 0.45;
                    float chestFlush = smoothstep(3.45, 4.15, vRestPos.y) * (1.0 - smoothstep(4.78, 5.25, vRestPos.y)) *
                        (1.0 - smoothstep(0.75, 1.38, axW)) * smoothstep(0.15, 0.65, N.z) *
                        (1.0 - dryPigmentMask * 0.75) * 0.62;
                    float warmthMask = clamp((cheekFlush + earFlush + chestFlush) * (1.0 - mucosa), 0.0, 1.0);
                    float flushMix = clamp(warmthMask * uBetaLifeWarmth * 0.38, 0.0, 0.24);
                    vec3 capillaryTone = albedo * vec3(1.045, 0.915, 0.905);
                    albedo = mix(albedo, capillaryTone, flushMix);
                }

                // Hair-color-matched sculpted eyebrows (pigment ~0.18..0.56) & crisp dark eyelashes (pigment > 0.68) — zero stray dots!
                // Fast-path guard: skip 3D brow hair noise on all non-facial/non-brow fragments (bit-for-bit identical result).
                float browMaskFace = isHeadRegion * smoothstep(0.16, 0.52, pigment) * (1.0 - smoothstep(0.08, 0.18, mucosa));
                float lashMaskFace = isHeadRegion * smoothstep(0.68, 0.90, pigment) * (1.0 - smoothstep(0.08, 0.18, mucosa));
                if (browMaskFace > 0.001 || lashMaskFace > 0.001) {
                    float browMicroHair = smoothstep(0.28, 0.78, vnoise3(vRestPos * vec3(135.0, 210.0, 120.0)));
                    vec3 browTone = mix(uHairBase * 0.92, uSkinShadow * 0.44, 0.24) * mix(0.86, 1.08, browMicroHair);
                    vec3 lashTone = mix(uHairBase * 0.38, vec3(0.06, 0.045, 0.045), 0.56);
                    albedo = mix(albedo, browTone, browMaskFace * 0.72);
                    albedo = mix(albedo, lashTone, lashMaskFace * 0.86);
                }

                // Customizable pubic hair (0.0 = Cleanly Shaven, 0.45 = Natural default, 1.0 = Very Bushy)
                // Organic inverted-teardrop density gradient tapering smoothly onto upper labia majora (zero hard rectangular edge!)
                // Fast-path guard: skip 6-10 3D noise evaluations outside pelvic hair region or when cleanly shaven.
                float ax = abs(vRestPos.x);
                float hairCoverage = 0.0;
                float hairAmt = clamp(uPubicHair, 0.0, 1.0);
                if (uAllowPubicHair > 0.5 && hairAmt > 0.015) {
                    float hairGrow = clamp(hairAmt / 0.45, 0.0, 1.0);
                    float hairBush = clamp((hairAmt - 0.45) / 0.55, 0.0, 1.0);

                    // Strictly isolate pubic hair to pelvic skin meshes & pelvic surface Z (ZERO bleeding onto posed hands/fingers!)
                    float pelvicZBand = smoothstep(-0.18, 0.04, vRestPos.z) * (1.0 - smoothstep(0.42, 0.54, vRestPos.z));
                    float anteriorMask = uAllowPubicHair * pelvicZBand * (1.0 - smoothstep(0.54 + 0.16 * hairBush, 0.86 + 0.18 * hairBush, ax));
                    float edgeNoise = (nMid - 0.5) * (0.14 + 0.06 * hairBush) + (nMicro - 0.5) * 0.045;

                    // Softly arched upper mons border + organic V-tapered lower contour flowing smoothly onto upper labia majora
                    float monsTopArch = (0.84 + 0.22 * hairBush) - ax * ax * (0.58 - 0.16 * hairBush) + edgeNoise;
                    float monsTopFade = 1.0 - smoothstep(monsTopArch - 0.22, monsTopArch + 0.07 + 0.06 * hairBush, vRestPos.y);

                    // Curved lower density taper: gently clears the central anterior cleft while tapering organically down the outer labia majora
                    float cleftArchY = (0.42 - 0.10 * hairBush) - smoothstep(0.08, 0.32, ax) * (0.36 + 0.24 * hairBush) + edgeNoise * 0.55;
                    float lowerTaperFade = smoothstep(cleftArchY - 0.16, cleftArchY + 0.22, vRestPos.y);
                    float verticalDensity = smoothstep(0.02 - 0.34 * hairBush, 0.64 - 0.10 * hairBush, vRestPos.y + edgeNoise * 0.45);

                    // Organic tapered width narrowing naturally from the mons triangle down along the outer crowns of the labia majora
                    float yWidthT = smoothstep(-0.12 - 0.24 * hairBush, 0.72, vRestPos.y);
                    float widthLimit = (0.28 + 0.11 * hairBush) + (0.24 + 0.13 * hairBush) * yWidthT + edgeNoise * 0.55;
                    float sideTaperFade = 1.0 - smoothstep(widthLimit * (0.40 - 0.08 * hairBush), widthLimit, ax);

                    // Keep inner cleft & mucosal vestibule naturally hair-free with a soft feathered margin
                    float cleftInnerClear = smoothstep(0.14 - 0.03 * hairBush, 0.26 - 0.04 * hairBush, ax + smoothstep(0.38, 0.62, vRestPos.y) * 0.26);
                    float upperLabiaTaper = monsTopFade * lowerTaperFade * sideTaperFade * cleftInnerClear *
                                            mix(0.32, 1.0, verticalDensity) * (1.0 - mucosa) * anteriorMask;
                    float totalHairMask = clamp(upperLabiaTaper * (0.88 + 0.26 * hairBush), 0.0, 1.0) * smoothstep(0.015, 0.14, hairAmt);

                    if (totalHairMask > 0.0) {
                        // Natural downward-and-slightly-inward hair growth direction + multi-scale strand-thickness variation
                        float flowInward = sign(vRestPos.x) * (0.26 * smoothstep(0.30, 0.78, vRestPos.y) - 0.12 * (1.0 - smoothstep(0.10, 0.45, vRestPos.y)));
                        float curl1 = vnoise3(vRestPos * 19.0) * 2.5;
                        float curl2 = vnoise3(vRestPos * 46.0) * 1.15;
                        vec3 hairCoord = vec3(
                            (vRestPos.x + vRestPos.y * flowInward) * 76.0 + curl1 + curl2,
                            vRestPos.y * 28.0 + ax * 18.0 - curl1 * 0.55,
                            vRestPos.z * 28.0
                        );
                        // Multi-thickness strand hierarchy: fine wispy border hairs, medium curly shafts, and denser core curls
                        float fineWisps = smoothstep(0.56 - 0.14 * hairBush, 0.90 - 0.10 * hairBush, vnoise3(hairCoord * 1.95 + vec3(1.4, -0.8, 0.5)));
                        float mediumStrands = smoothstep(0.48 - 0.18 * hairBush, 0.86 - 0.14 * hairBush, vnoise3(hairCoord * 1.28));
                        float coarseCore = smoothstep(0.30 - 0.12 * hairBush, 0.76 - 0.16 * hairBush, vnoise3(hairCoord * 0.62)) * smoothstep(0.18, 0.68, totalHairMask);
                        float hairDenseLayer = smoothstep(0.32, 0.72, vnoise3(hairCoord * 0.92 + vec3(3.1, -1.7, 2.4))) * hairBush * 0.40 * smoothstep(0.22, 0.72, totalHairMask);
                        float rawCoverage = totalHairMask * (fineWisps * 0.34 + mediumStrands * (0.44 + 0.20 * hairBush) + coarseCore * (0.34 + 0.22 * hairBush) + hairDenseLayer);
                        hairCoverage = clamp(rawCoverage * hairGrow, 0.0, 0.64 + 0.32 * hairBush);

                        // Subtle 3D coarse curly-hair relief when fuller so pubic hair catches realistic studio depth
                        if (hairCoverage > 0.04) {
                            float reliefGain = hairCoverage * (0.09 + 0.20 * hairBush);
                            float hcX = vnoise3((hairCoord + vec3(0.45, 0.0, 0.0)) * 1.15) - vnoise3((hairCoord - vec3(0.45, 0.0, 0.0)) * 1.15);
                            float hcY = vnoise3((hairCoord + vec3(0.0, 0.45, 0.0)) * 1.15) - vnoise3((hairCoord - vec3(0.0, 0.45, 0.0)) * 1.15);
                            N = normalize(N + vec3(hcX, hcY, 0.0) * reliefGain);
                        }

                        vec3 hairColor = mix(uSkinShadow * 0.34, uHairBase * 0.85, 0.60);
                        // Subtle warm strand variation inside fuller bush so it never looks like a flat dark decal
                        vec3 bushyHairColor = mix(hairColor * 0.84, mix(uHairBase, uHairHighlight, 0.28), (mediumStrands * 0.6 + fineWisps * 0.4) * (0.20 + 0.35 * hairBush));
                        albedo = mix(albedo, bushyHairColor, hairCoverage * (0.64 + 0.28 * hairBush));
                    }
                }

                float NdotL1 = dot(N, Lkey);
                float NdotL2 = dot(N, Lfill);

                // SPATIAL HIP, INGUINAL & GLUTEAL JOINT SSS UNIFICATION ZONE:
                // Softens dermal wrap scattering & terminator bounce across abdomen, pelvis, buttocks & upper thighs!
                float hipJointX = smoothstep(0.28, 0.56, ax) * (1.0 - smoothstep(1.38, 1.92, ax));
                float hipJointY = smoothstep(-1.42, -0.92, vRestPos.y) * (1.0 - smoothstep(0.72, 1.24, vRestPos.y));
                float jointSss = hipJointX * hipJointY * (1.0 - mucosa);

                // TRUE RGB-WAVELENGTH PRE-INTEGRATED SUBSURFACE SCATTERING (SSS):
                // Red (~650nm) wraps deepest through hemoglobin dermis; Green medium; Blue tightest!
                vec3 sssWrapKey = mix(vec3(0.64, 0.30, 0.14), vec3(0.86, 0.46, 0.22), mucosa) +
                                  vec3(0.24, 0.14, 0.08) * jointSss;
                vec3 sssWrapFill = mix(vec3(0.46, 0.24, 0.12), vec3(0.66, 0.36, 0.18), mucosa) +
                                   vec3(0.18, 0.10, 0.06) * jointSss;
                vec3 diffKeyRGB = clamp((vec3(NdotL1) + sssWrapKey) / (vec3(1.0) + sssWrapKey), 0.0, 1.0);
                vec3 diffFillRGB = clamp((vec3(NdotL2) + sssWrapFill) / (vec3(1.0) + sssWrapFill), 0.0, 1.0);
                diffKeyRGB = pow(max(diffKeyRGB, vec3(0.0001)), vec3(0.90, 1.05, 1.22));
                diffFillRGB = pow(max(diffFillRGB, vec3(0.0001)), vec3(0.94, 1.04, 1.16));

                // Subcutaneous dermis hemoglobin glow across shadow terminators, hip joints & thin labial edges
                vec3 sssShallow = mix(uSkinBase * vec3(1.08, 0.62, 0.50), vec3(0.90, 0.40, 0.28), 0.50);
                vec3 sssDeep = vec3(0.86, 0.18, 0.20);
                vec3 sssTint = mix(sssShallow, sssDeep, 0.36 + 0.54 * mucosa);
                float terminator = exp(-abs(NdotL1 - 0.02) * 3.6) * (0.28 + 0.34 * mucosa + 0.14 * jointSss);

                // Thin-tissue forward/back translucency (illuminates thin labia minora & hood margins!)
                // Vulva Beta v2 Toggle 4: strictly pelvic-scoped satin-velvet finish reducing waxy rim/specular polish
                // while preserving natural pinpoint mucosal moisture (bit-identical to v13.0 when uniform == 0.0).
                float pelvicFinishMask = uBetaV2NaturalPelvicFinish *
                    smoothstep(-1.18, -0.82, vRestPos.y) *
                    (1.0 - smoothstep(0.82, 1.18, vRestPos.y)) *
                    (1.0 - smoothstep(0.62, 0.96, ax));
                float nonArchPelvicMucosa = pelvicFinishMask * mucosa * (1.0 - labiaInnerMask);
                float pelvicCutaneousFinish = pelvicFinishMask * (1.0 - mucosa);

                vec3 Hback = normalize(-Lkey + N * 0.48);
                float thinTransmit = pow(clamp(dot(V, Hback), 0.0, 1.0), 2.8) * (0.10 + 0.36 * mucosa + 0.05 * jointSss);
                float grazingTrans = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 2.3) * (0.14 + 0.34 * mucosa + 0.08 * jointSss) *
                    (1.0 - pelvicFinishMask * 0.28);

                // Dual-lobe matte-velvet specular BRDF with gentle regional roughness variation (ZERO oily gloss or sparkle!)
                vec3 H1 = normalize(Lkey + V);
                vec3 H2 = normalize(Lfill + V);
                float NdotH1 = max(dot(N, H1), 0.0);
                float NdotH2 = max(dot(N, H2), 0.0);

                // Gentle anatomical sheen zones: shoulders/collarbones, cheekbones/nose bridge, and limbs catch slightly softer natural sheen
                float shoulderClavZone = smoothstep(4.25, 4.65, vRestPos.y) * (1.0 - smoothstep(4.85, 5.20, vRestPos.y));
                float cheekNoseZone = isHeadRegion * smoothstep(0.35, 0.85, N.z) * (1.0 - smoothstep(0.50, 0.72, ax));
                float limbZone = clamp(smoothstep(1.35, 1.95, ax) + smoothstep(-0.35, -1.65, vRestPos.y), 0.0, 1.0);
                float naturalSheen = clamp(shoulderClavZone * 0.75 + cheekNoseZone * 0.50 + limbZone * 0.55, 0.0, 1.0) * (1.0 - mucosa);

                float hairSpecDamp = 1.0 - hairCoverage * 0.65;
                float jointSpecSoften = 1.0 - jointSss * 0.22;
                // Natural matte-velvet finish on face, outer pelvic skin (majora/mons/perineum), and dry pigmented areolae
                float faceMatteBlend = mix(
                    smoothstep(5.45, 5.95, vRestPos.y),
                    smoothstep(4.62, 5.95, vRestPos.y) * (1.0 - submentalDown * 0.65),
                    uBetaSmoothNeckSeam
                );
                float faceMatteDamp = 1.0 - faceMatteBlend * 0.36;
                float pelvicMatteDamp = 1.0 - pelvicSkinZone * 0.42;
                float areolaMatteDamp = 1.0 - dryPigmentMask * 0.56;
                float oilBreakupBase = hairSpecDamp * jointSpecSoften * faceMatteDamp * pelvicMatteDamp * areolaMatteDamp;
                float oilBreakup = mix(0.79, 1.19, nMicro * 0.65 + nMelanin * 0.35) * oilBreakupBase;
                float labiaOilBreakup = mix(oilBreakup, 0.92 * oilBreakupBase, 0.65 * labiaInnerMask);
                float microRough = mix(-3.0, 3.2, nMid * 0.55 + nMicro * 0.45);
                // Beta v3 Toggle 4 (Regional Material Differentiation, scaled 3x: 0..3.0): separate dry matte-satin outer skin,
                // pliable transitional sulcus/hood satin, hydrated inner mucosa, and luminous pearl glans!
                float v3MatBounded = min(uBetaV3RegionalMaterial, 3.0);
                float v3OuterMatte = clamp(1.0 - v3ZoneOuter * v3MatBounded * 0.24, 0.24, 1.0);
                float v3TransSatin = 1.0 + v3ZoneTrans * v3MatBounded * 0.14;
                float v3HydrationVar = mix(0.86, 1.18, nMid * 0.55 + nHemo * 0.45);
                float v3MucosaSheen = 1.0 + v3ZoneMucosa * v3MatBounded * (0.28 * v3HydrationVar);
                float v3GlansPearl = 1.0 + v3ZoneGlans * v3MatBounded * 0.24;
                float broadExp = max(8.0, mix(18.0, 29.0, clamp(wetness + naturalSheen * 0.22, 0.0, 1.0)) +
                    microRough * (1.0 - mucosa) + 22.0 * labiaInnerMask - 3.0 * pelvicCutaneousFinish -
                    3.6 * v3ZoneOuter * v3MatBounded + 5.5 * v3ZoneTrans * v3MatBounded);
                float pelvicBroadTrim = (1.0 - pelvicCutaneousFinish * 0.28) * (1.0 - nonArchPelvicMucosa * 0.26) * (1.0 - pelvicFinishMask * labiaInnerMask * 0.12);
                float broadAmp = mix(0.070, 0.148, wetness) * (0.88 + 0.24 * naturalSheen) * (1.0 - 0.56 * labiaInnerMask) * pelvicBroadTrim * v3OuterMatte * v3TransSatin;
                float broadSpec = pow(NdotH1, broadExp) * broadAmp * labiaOilBreakup;
                // Secondary tight crisp highlight strictly reserved for naturally moist inner mucosa & clitoral glans
                float moistMask = smoothstep(0.075, 0.18, wetness);
                float wetKeyExp = mix(78.0, 162.0, wetness) + 86.0 * labiaInnerMask + 32.0 * nonArchPelvicMucosa +
                    22.0 * v3ZoneMucosa * v3MatBounded + 32.0 * v3ZoneGlans * v3MatBounded;
                float wetFillExp = 92.0 + 74.0 * labiaInnerMask + 24.0 * nonArchPelvicMucosa;
                float wetSpecGain = (1.0 - 0.56 * labiaInnerMask) * (1.0 - nonArchPelvicMucosa * 0.34) * (1.0 - pelvicFinishMask * labiaInnerMask * 0.12) *
                    clamp(1.0 - v3ZoneTrans * v3MatBounded * 0.24, 0.25, 1.0) * v3MucosaSheen * v3GlansPearl;
                float wetSharpSpec = (pow(NdotH1, wetKeyExp) * wetness * 0.54 +
                                      pow(NdotH2, wetFillExp) * wetness * 0.15) * wetSpecGain * moistMask * faceMatteDamp * areolaMatteDamp;

                // Soft studio rim light + subtle matte-velvet peach-fuzz Fresnel wrap across dry skin silhouettes
                float viewRim = clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0);
                float fresnel = pow(viewRim, 3.4);
                float rimLight = fresnel * (0.26 + 0.24 * mucosa) * (0.45 + 0.55 * max(dot(N, Lrim), 0.0)) *
                    (1.0 - pelvicFinishMask * 0.42);
                float velvetWrap = pow(viewRim, 2.2) * (1.0 - mucosa) * (1.0 - hairCoverage * 0.80) * (0.052 + 0.018 * v3ZoneOuter * v3MatBounded);

                // Colored Hemoglobin AO Bounce: prevents crevices from looking muddy gray & softens hip/lip/body contrast
                float featheredAo = mix(ringSoftAo, min(ringSoftAo, 0.87), submentalFeatherZone * 0.42);
                featheredAo = mix(featheredAo, min(featheredAo, 0.93), labiaInnerMask * 0.45);
                if (v3BlendStr > 0.001) {
                    // Ease medial junction AO shadow so the labial base melts organically into the vestibule
                    featheredAo = mix(featheredAo, max(featheredAo, 0.72), vestBaseMask * v3BlendStr * 0.65);
                }
                float tangleCreaseAo = (tangleActive > 0.004)
                    ? mix(1.0 - 0.26 * tangleActive, 1.04, clamp(tangleHeightC * 1.15, 0.0, 1.0))
                    : 1.0;
                float fullAo = clamp(featheredAo * microAo * tangleCreaseAo, 0.06, 1.0);
                vec3 aoExponents = mix(vec3(0.76, 1.00, 1.18), vec3(0.68, 0.90, 1.06), jointSss * 0.65);
                aoExponents = mix(aoExponents, vec3(0.68, 0.96, 1.22), archWarmMask * 0.60);
                vec3 coloredAo = pow(vec3(fullAo), aoExponents);

                // Balanced portrait illumination so Blossom's face matches her neck & chest brightness naturally
                float headFillLift = neckSkinBlend * (1.0 - browMaskFace * 0.7 - lashMaskFace * 0.85) * 0.055;
                vec3 ambient = albedo * (0.19 + headFillLift + 0.08 * (N.y * 0.5 + 0.5));
                vec3 keyColor = vec3(1.03, 0.96, 0.92);
                vec3 fillColor = vec3(0.98, 0.93, 0.90);
                float faceSssTrim = 1.0 - neckSkinBlend * (1.0 - mucosa) * 0.30;
                vec3 specColor = mix(
                    vec3(1.0, 0.96, 0.94),
                    mix(albedo * 1.35 + vec3(0.18, 0.13, 0.12), vec3(1.0, 0.91, 0.87), 0.52),
                    0.42 * labiaInnerMask
                );

                vec3 lit = ambient * coloredAo +
                           albedo * (diffKeyRGB * 0.78 * keyColor + diffFillRGB * (0.22 + neckSkinBlend * 0.08) * fillColor) * (vec3(0.28) + 0.72 * coloredAo) +
                           sssTint * (terminator * 0.35 * faceSssTrim + grazingTrans * 0.19 + thinTransmit * (0.22 + 0.20 * uLightMode)) * coloredAo +
                           specColor * (broadSpec + wetSharpSpec) * (0.30 + 0.70 * fullAo) +
                           mix(uSkinBase, vec3(1.0, 0.96, 0.93), 0.28) * velvetWrap * (0.45 + 0.55 * fullAo) +
                           mix(uSkinBase, sssTint, 0.46) * rimLight;

                // Soft warm subsurface glow right under active fingertip touch
                if (uTouchIntensity > 0.005) {
                    float dTouch = length(vWorldPos.xy - uTouchPos);
                    float glowMask = exp(-dTouch * dTouch * 14.0) * uTouchIntensity;
                    lit += uTouchColor * glowMask * 0.28;
                }

                // Balanced photographic filmic tone mapping with gentle warm highlight saturation preservation
                vec3 mapped = lit / (lit + vec3(0.76));
                mapped = pow(clamp(mapped * 1.32, 0.0, 1.0), vec3(1.01));
                float mappedLuma = dot(mapped, vec3(0.299, 0.587, 0.114));
                mapped = clamp(mix(vec3(mappedLuma), mapped, 1.03), 0.0, 1.0);

                float outAlpha = (uArchAlphaPass > 1.5) ? archAlpha : 1.0;
                gl_FragColor = vec4(mapped, outAlpha);
            }
        """
    }
}
