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
    private var lastAppliedDeformation: VulvaDeformationState? = null

    @Volatile
    private var lastAppliedLegsMode: Boolean? = null

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
    }

    private val sessionLock = Any()
    private var activeSession: RenderSession? = null

    private var aPosLoc = -1
    private var aNormalLoc = -1
    private var aMatLoc = -1

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

    private var breathPhase = 0f
    private var smoothedStimulus = 0f
    private var lastBreathTimeNs = 0L

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
        val thread = HandlerThread("VulvaGlRenderThread", Process.THREAD_PRIORITY_LESS_FAVORABLE + 3)
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
            // Build heavy procedural SDF anatomy meshes on a follow-up tick after the first clear buffer
            // has already been swapped to SurfaceTexture so SurfaceSyncGroup settles immediately.
            handler.post {
                if (session.generation != renderGeneration || session.teardownPosted ||
                    session.eglSurface == EGL14.EGL_NO_SURFACE
                ) return@post
                sceneMesh = VulvaAnatomySceneMesh()
                lastAppliedDeformation = VulvaDeformationState()
                lastAppliedLegsMode = null
                lastBreathTimeNs = System.nanoTime()
                isGlReady = true
                if (isHostVisible && !isHostPaused && session.generation == renderGeneration && !session.teardownPosted) {
                    drawFrameInternal(session)
                }
            }
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
        if (!EGL14.eglSwapBuffers(display, eglSurf)) return false

        val program = compileProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) return false
        session.programId = program

        aPosLoc = GLES20.glGetAttribLocation(program, "aPos")
        aNormalLoc = GLES20.glGetAttribLocation(program, "aNormal")
        aMatLoc = GLES20.glGetAttribLocation(program, "aMat")

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
        if (legsMode) {
            breathPhase = (breathPhase + dtSec * breathFrequencyHz(smoothedStimulus)) % 1.0f
        }
        val breathDisp = if (legsMode) evaluateBreathingDisplacement(breathPhase, smoothedStimulus) else Pair(0f, 0f)
        GLES20.glUniform1f(uBreathChestLoc, breathDisp.first)
        GLES20.glUniform1f(uBreathAbdomenLoc, breathDisp.second)

        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glEnableVertexAttribArray(aMatLoc)

        fun drawMeshList(meshes: List<DynamicGlMesh>, materialType: Float) {
            GLES20.glUniform1f(uMaterialTypeLoc, materialType)
            meshes.forEach { glMesh ->
                val allowPubic = (materialType < 0.5f) &&
                    (glMesh === mesh.blossomBodyMesh || glMesh === mesh.blossomVulvaPatchMesh || glMesh === mesh.pelvisMesh)
                GLES20.glUniform1f(uAllowPubicHairLoc, if (allowPubic) 1.0f else 0.0f)
                val allowBreath = legsMode && (materialType < 0.5f) &&
                    (glMesh === mesh.blossomBodyMesh || glMesh === mesh.blossomBreastsMesh)
                GLES20.glUniform1f(uAllowBreathingLoc, if (allowBreath) 1.0f else 0.0f)

                val vb = glMesh.vertexBuffer
                vb.position(0)
                GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, DynamicGlMesh.STRIDE_BYTES, vb)
                vb.position(3)
                GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, DynamicGlMesh.STRIDE_BYTES, vb)
                vb.position(6)
                GLES20.glVertexAttribPointer(aMatLoc, 4, GLES20.GL_FLOAT, false, DynamicGlMesh.STRIDE_BYTES, vb)

                glMesh.indexBuffer.position(0)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, glMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, glMesh.indexBuffer)
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

        GLES20.glDisableVertexAttribArray(aPosLoc)
        GLES20.glDisableVertexAttribArray(aNormalLoc)
        GLES20.glDisableVertexAttribArray(aMatLoc)

        if (session.generation != renderGeneration || session.teardownPosted || !isGlReady) return
        val swapped = EGL14.eglSwapBuffers(session.eglDisplay, session.eglSurface)
        if (!swapped) {
            val eglErr = EGL14.eglGetError()
            if (eglErr == EGL14.EGL_BAD_SURFACE || eglErr == EGL14.EGL_CONTEXT_LOST) {
                isGlReady = false
            }
            return
        }

        if (legsMode && isGlReady && isHostVisible && !isHostPaused &&
            session.generation == renderGeneration && !session.teardownPosted && !session.breathTickerScheduled
        ) {
            val frameMs = ((System.nanoTime() - frameStartNs) / 1_000_000L).coerceAtLeast(0L)
            // Adaptive idle pacing: smooth ~15-30 FPS breathing while preserving generous RenderThread headroom
            val nextDelayMs = maxOf(33L, frameMs * 2L).coerceAtMost(70L)
            session.breathTickerScheduled = true
            session.handler.postDelayed(breathTicker, nextDelayMs)
        }
    }

    private fun releaseGlInternal(session: RenderSession) {
        if (session.generation == renderGeneration) {
            isGlReady = false
        }
        if (session.programId != 0) {
            GLES20.glDeleteProgram(session.programId)
            session.programId = 0
        }
        if (session.eglDisplay != EGL14.EGL_NO_DISPLAY) {
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
         * Abdominal/diaphragmatic swell slightly leads upper thoracic/ribcage rise for lifelike supine breathing.
         */
        internal fun evaluateBreathingDisplacement(phaseCycles: Float, stimulus: Float): Pair<Float, Float> {
            val s = stimulus.coerceIn(0f, 1f)
            val depthScale = 1.0f + 0.24f * s
            val abdomenWave = evaluateBreathWave(phaseCycles)
            val chestWave = evaluateBreathWave(phaseCycles - 0.07f)
            // Gently noticeable calm supine chest & diaphragmatic rise-and-fall
            val chestRise = chestWave * 0.080f * depthScale
            val abdomenRise = abdomenWave * 0.096f * depthScale
            return Pair(chestRise, abdomenRise)
        }

        private const val VERTEX_SHADER = """
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec4 aMat;

            uniform mat4 uMvp;
            uniform mat4 uModel;
            uniform float uBreathChest;
            uniform float uBreathAbdomen;
            uniform float uAllowBreathing;

            varying vec3 vWorldPos;
            varying vec3 vRestPos;
            varying vec3 vNormal;
            varying vec4 vMat;

            void main() {
                vec3 pos = aPos;
                vec3 nrm = normalize(aNormal);

                if (uAllowBreathing > 0.5) {
                    // Strictly isolate natural breathing lift to anterior torso, ribcage, breasts & abdomen
                    // (back remains pinned to exam cushion; arms, hands, head & vulva stay 100% undisturbed)
                    float ax = abs(pos.x);
                    float latMask = 1.0 - smoothstep(0.96, 1.48, ax);
                    float frontMask = smoothstep(-0.10, 0.34, nrm.z) * smoothstep(-0.34, 0.14, pos.z);
                    float torsoMask = latMask * frontMask;

                    // Soft diaphragmatic abdominal rise (y ~ 0.9..3.4, centered near soft mid-abdomen)
                    float abdEnv = smoothstep(0.86, 1.58, pos.y) * (1.0 - smoothstep(2.48, 3.46, pos.y));
                    // Gentle thoracic & breast rise (y ~ 2.3..4.8, moving ribcage & breasts in exact lockstep)
                    float chestEnv = smoothstep(2.26, 3.18, pos.y) * (1.0 - smoothstep(4.24, 4.86, pos.y));

                    float liftZ = (uBreathAbdomen * abdEnv + uBreathChest * chestEnv) * torsoMask;
                    float liftY = (uBreathChest * chestEnv * 0.34 - uBreathAbdomen * abdEnv * 0.08) * torsoMask;
                    float latRib = (uBreathChest * chestEnv * 0.25 + uBreathAbdomen * abdEnv * 0.18) *
                                   smoothstep(0.14, 0.78, ax) * torsoMask * sign(pos.x);

                    pos += vec3(latRib, liftY, liftZ);
                    // Matching subtle normal tilt so studio lighting visibly breathes across rising chest & belly
                    nrm = normalize(nrm + vec3(latRib * 0.72, (uBreathAbdomen * abdEnv - uBreathChest * chestEnv) * torsoMask * 0.58, 0.0));
                }

                vec4 worldPos = uModel * vec4(pos, 1.0);
                vWorldPos = worldPos.xyz;
                vRestPos = (uModel * vec4(aPos, 1.0)).xyz;
                vNormal = nrm;
                vMat = aMat;
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
        private const val FRAGMENT_SHADER = """
            precision highp float;

            varying vec3 vWorldPos;
            varying vec3 vRestPos;
            varying vec3 vNormal;
            varying vec4 vMat;

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

                // -----------------------------------------------------------------
                // PASS A: LIVING SKIN & MUCOSA WITH RGB SUBSURFACE SCATTERING (SSS)
                // -----------------------------------------------------------------
                float mucosa = clamp(vMat.x, 0.0, 1.0);
                float wetness = clamp(vMat.y, 0.0, 1.0);
                float ao = clamp(vMat.z, 0.06, 1.0);
                float pigment = clamp(vMat.w, 0.0, 1.0);

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
                float faceVelvet = smoothstep(5.25, 5.85, vRestPos.y);
                float isHeadRegion = smoothstep(5.40, 5.62, vRestPos.y);
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
                // Skin-harmonized pigmentation across body and pelvic skin
                vec3 pigmentedSkin = mix(warmBlush, mix(uSkinShadow * 0.93, uMucosaBase * 0.90, 0.44), pigment * (0.38 + pelvicSkinZone * 0.14) * (1.0 - isHeadRegion * 0.65));
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
                vec3 mucosaVar = mix(baseMucosaTone * 0.93, baseMucosaTone * 1.07, nMid * 0.45 + nHemo * 0.35 + 0.15);
                vec3 mucosaRich = mix(mucosaVar, uMucosaDeep, (1.0 - ao) * (0.50 - 0.24 * isHeadRegion - 0.28 * isChestRegion));
                // Natural darker pigmentation concentrated subtly along the labia minora free edge (strictly pelvic region)
                float isPelvicMucosa = 1.0 - smoothstep(1.2, 2.0, vRestPos.y);
                vec3 mucosaRimmed = mix(mucosaRich, mix(uMucosaDeep * 0.86, uSkinShadow * 0.82, 0.48), pigment * 0.34 * isPelvicMucosa);
                // Keep facial skin (low mucosa) 100% matched to neck/chest brightness and neutral hue!
                float faceLipActive = smoothstep(0.06, 0.22, mucosa);
                vec3 faceHarmonizedSkin = mix(pigmentedSkin, warmBlush * vec3(1.01, 1.01, 1.00), isHeadRegion * (1.0 - faceLipActive) * 0.45);
                vec3 albedo = mix(faceHarmonizedSkin, mucosaRimmed, mucosa);

                // Hair-color-matched sculpted eyebrows (pigment ~0.18..0.56) & crisp dark eyelashes (pigment > 0.68) — zero stray dots!
                float browMaskFace = isHeadRegion * smoothstep(0.16, 0.52, pigment) * (1.0 - smoothstep(0.08, 0.18, mucosa));
                float lashMaskFace = isHeadRegion * smoothstep(0.68, 0.90, pigment) * (1.0 - smoothstep(0.08, 0.18, mucosa));
                float browMicroHair = smoothstep(0.28, 0.78, vnoise3(vRestPos * vec3(135.0, 210.0, 120.0)));
                vec3 browTone = mix(uHairBase * 0.92, uSkinShadow * 0.44, 0.24) * mix(0.86, 1.08, browMicroHair);
                vec3 lashTone = mix(uHairBase * 0.38, vec3(0.06, 0.045, 0.045), 0.56);
                albedo = mix(albedo, browTone, browMaskFace * 0.72);
                albedo = mix(albedo, lashTone, lashMaskFace * 0.86);

                // Customizable pubic hair (0.0 = Cleanly Shaven, 0.45 = Natural default, 1.0 = Very Bushy)
                // Organic inverted-teardrop density gradient tapering smoothly onto upper labia majora (zero hard rectangular edge!)
                float ax = abs(vRestPos.x);
                float hairAmt = clamp(uPubicHair, 0.0, 1.0);
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
                float hairCoverage = clamp(rawCoverage * hairGrow, 0.0, 0.64 + 0.32 * hairBush);

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
                vec3 Hback = normalize(-Lkey + N * 0.48);
                float thinTransmit = pow(clamp(dot(V, Hback), 0.0, 1.0), 2.8) * (0.10 + 0.36 * mucosa + 0.05 * jointSss);
                float grazingTrans = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 2.3) * (0.14 + 0.34 * mucosa + 0.08 * jointSss);

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
                float faceMatteDamp = 1.0 - smoothstep(5.45, 5.95, vRestPos.y) * 0.36;
                float pelvicMatteDamp = 1.0 - pelvicSkinZone * 0.42;
                float areolaMatteDamp = 1.0 - dryPigmentMask * 0.56;
                float oilBreakup = mix(0.79, 1.19, nMicro * 0.65 + nMelanin * 0.35) * hairSpecDamp * jointSpecSoften * faceMatteDamp * pelvicMatteDamp * areolaMatteDamp;
                float microRough = mix(-3.0, 3.2, nMid * 0.55 + nMicro * 0.45);
                float broadExp = max(12.0, mix(18.0, 29.0, clamp(wetness + naturalSheen * 0.22, 0.0, 1.0)) + microRough * (1.0 - mucosa));
                float broadAmp = mix(0.070, 0.148, wetness) * (0.88 + 0.24 * naturalSheen);
                float broadSpec = pow(NdotH1, broadExp) * broadAmp * oilBreakup;
                // Secondary tight crisp highlight strictly reserved for naturally moist inner mucosa & clitoral glans
                float moistMask = smoothstep(0.075, 0.18, wetness);
                float wetSharpSpec = (pow(NdotH1, mix(78.0, 162.0, wetness)) * wetness * 0.54 +
                                      pow(NdotH2, 92.0) * wetness * 0.15) * moistMask * faceMatteDamp * areolaMatteDamp;

                // Soft studio rim light + subtle matte-velvet peach-fuzz Fresnel wrap across dry skin silhouettes
                float viewRim = clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0);
                float fresnel = pow(viewRim, 3.4);
                float rimLight = fresnel * (0.26 + 0.24 * mucosa) * (0.45 + 0.55 * max(dot(N, Lrim), 0.0));
                float velvetWrap = pow(viewRim, 2.2) * (1.0 - mucosa) * (1.0 - hairCoverage * 0.80) * 0.052;

                // Colored Hemoglobin AO Bounce: prevents crevices from looking muddy gray & softens hip/lip/body contrast
                float fullAo = clamp(ao * microAo, 0.06, 1.0);
                vec3 aoExponents = mix(vec3(0.76, 1.00, 1.18), vec3(0.68, 0.90, 1.06), jointSss * 0.65);
                vec3 coloredAo = pow(vec3(fullAo), aoExponents);

                // Balanced portrait illumination so Blossom's face matches her neck & chest brightness naturally
                float headFillLift = isHeadRegion * (1.0 - browMaskFace * 0.7 - lashMaskFace * 0.85) * 0.055;
                vec3 ambient = albedo * (0.19 + headFillLift + 0.08 * (N.y * 0.5 + 0.5));
                vec3 keyColor = vec3(1.03, 0.96, 0.92);
                vec3 fillColor = vec3(0.98, 0.93, 0.90);
                float faceSssTrim = 1.0 - isHeadRegion * (1.0 - mucosa) * 0.30;

                vec3 lit = ambient * coloredAo +
                           albedo * (diffKeyRGB * 0.78 * keyColor + diffFillRGB * (0.22 + isHeadRegion * 0.08) * fillColor) * (vec3(0.28) + 0.72 * coloredAo) +
                           sssTint * (terminator * 0.35 * faceSssTrim + grazingTrans * 0.19 + thinTransmit * (0.22 + 0.20 * uLightMode)) * coloredAo +
                           vec3(1.0, 0.96, 0.94) * (broadSpec + wetSharpSpec) * (0.30 + 0.70 * fullAo) +
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

                gl_FragColor = vec4(mapped, 1.0);
            }
        """
    }
}
