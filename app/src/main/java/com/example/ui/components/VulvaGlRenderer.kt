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
import android.view.TextureView
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
    var touchGlow: VulvaTouchGlow = VulvaTouchGlow()

    @Volatile
    var dramaticSideLight: Boolean = false

    @Volatile
    var showExperimentalLegs: Boolean = true

    @Volatile
    private var lastAppliedDeformation: VulvaDeformationState? = null

    @Volatile
    private var lastAppliedLegsMode: Boolean? = null

    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var programId: Int = 0
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

    private var surfaceWidth = 1
    private var surfaceHeight = 1
    private var isGlReady = false

    private var sceneMesh: VulvaAnatomySceneMesh? = null

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    @Volatile
    private var renderScheduled = false

    init {
        isOpaque = true
        surfaceTextureListener = this
    }

    fun requestFrame() {
        val handler = renderHandler ?: return
        if (!renderScheduled) {
            renderScheduled = true
            handler.post {
                renderScheduled = false
                drawFrameInternal()
            }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        startRenderThread(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        renderHandler?.post {
            if (isGlReady) {
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
                drawFrameInternal()
            }
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun startRenderThread(surface: SurfaceTexture) {
        stopRenderThread()
        val thread = HandlerThread("VulvaGlRenderThread")
        thread.start()
        renderThread = thread
        val handler = Handler(thread.looper)
        renderHandler = handler
        handler.post {
            val ok = initEglAndShaders(surface)
            if (ok) {
                isGlReady = true
                drawFrameInternal()
            } else {
                post { onGlError() }
            }
        }
    }

    private fun stopRenderThread() {
        val handler = renderHandler
        val thread = renderThread
        renderHandler = null
        renderThread = null
        if (handler != null && thread != null) {
            handler.post {
                releaseGlInternal()
                thread.quitSafely()
            }
        }
    }

    private fun initEglAndShaders(surface: SurfaceTexture): Boolean {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

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
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
            numConfigs[0] <= 0 || configs[0] == null
        ) {
            return false
        }
        val config = configs[0]!!

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) return false

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return false

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false

        programId = compileProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (programId == 0) return false

        aPosLoc = GLES20.glGetAttribLocation(programId, "aPos")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
        aMatLoc = GLES20.glGetAttribLocation(programId, "aMat")

        uMvpLoc = GLES20.glGetUniformLocation(programId, "uMvp")
        uModelLoc = GLES20.glGetUniformLocation(programId, "uModel")
        uCamPosLoc = GLES20.glGetUniformLocation(programId, "uCamPos")
        uSkinBaseLoc = GLES20.glGetUniformLocation(programId, "uSkinBase")
        uSkinShadowLoc = GLES20.glGetUniformLocation(programId, "uSkinShadow")
        uMucosaBaseLoc = GLES20.glGetUniformLocation(programId, "uMucosaBase")
        uMucosaDeepLoc = GLES20.glGetUniformLocation(programId, "uMucosaDeep")
        uTouchPosLoc = GLES20.glGetUniformLocation(programId, "uTouchPos")
        uTouchColorLoc = GLES20.glGetUniformLocation(programId, "uTouchColor")
        uTouchIntensityLoc = GLES20.glGetUniformLocation(programId, "uTouchIntensity")
        uStretchLoc = GLES20.glGetUniformLocation(programId, "uStretch")
        uLightModeLoc = GLES20.glGetUniformLocation(programId, "uLightMode")
        uMaterialTypeLoc = GLES20.glGetUniformLocation(programId, "uMaterialType")
        uHairBaseLoc = GLES20.glGetUniformLocation(programId, "uHairBase")
        uHairHighlightLoc = GLES20.glGetUniformLocation(programId, "uHairHighlight")
        uHairSheenLoc = GLES20.glGetUniformLocation(programId, "uHairSheen")

        sceneMesh = VulvaAnatomySceneMesh()
        lastAppliedDeformation = VulvaDeformationState()

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        return true
    }

    private fun drawFrameInternal() {
        if (!isGlReady || eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return

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

        GLES20.glUseProgram(programId)
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

        val touchWorldX = (glow.normX - 0.5f) * 1.60f
        val touchWorldY = (0.5f - glow.normY) * 1.90f
        GLES20.glUniform2f(uTouchPosLoc, touchWorldX, touchWorldY)
        GLES20.glUniform3f(uTouchColorLoc, glow.color.red, glow.color.green, glow.color.blue)
        GLES20.glUniform1f(uTouchIntensityLoc, glow.intensity)

        val avgStretch = ((currentDeform.leftOuter + currentDeform.rightOuter +
            currentDeform.leftInner + currentDeform.rightInner) * 0.25f).coerceIn(0f, 1f)
        GLES20.glUniform1f(uStretchLoc, avgStretch)
        GLES20.glUniform1f(uLightModeLoc, if (dramaticSideLight) 1.0f else 0.0f)

        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glEnableVertexAttribArray(aNormalLoc)
        GLES20.glEnableVertexAttribArray(aMatLoc)

        fun drawMeshList(meshes: List<DynamicGlMesh>, materialType: Float) {
            GLES20.glUniform1f(uMaterialTypeLoc, materialType)
            meshes.forEach { glMesh ->
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
            drawMeshList(mesh.blossomNailMeshes, materialType = 4.0f)
            drawMeshList(mesh.blossomEyeMeshes, materialType = 2.0f)
            drawMeshList(mesh.blossomHairMeshes, materialType = 3.0f)
        } else {
            drawMeshList(mesh.coreMeshes, materialType = 0.0f)
        }

        GLES20.glDisableVertexAttribArray(aPosLoc)
        GLES20.glDisableVertexAttribArray(aNormalLoc)
        GLES20.glDisableVertexAttribArray(aMatLoc)

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun releaseGlInternal() {
        isGlReady = false
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
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

        private const val VERTEX_SHADER = """
            attribute vec3 aPos;
            attribute vec3 aNormal;
            attribute vec4 aMat;

            uniform mat4 uMvp;
            uniform mat4 uModel;

            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec4 vMat;

            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(aNormal);
                vMat = aMat;
                gl_Position = uMvp * vec4(aPos, 1.0);
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
                float pores = vnoise3(p * 78.0) * 0.50 + vnoise3(p * 165.0) * 0.32;
                float fineGrain = vnoise3(p * 290.0) * 0.18;
                float creaseWave = sin(p.y * 58.0 + vnoise3(p * 15.0) * 4.2 + p.x * 22.0);
                float fineLabialStriae = sin(p.y * 132.0 + vnoise3(p * 28.0) * 3.4 - abs(p.x) * 46.0);
                float creaseRidges = ((0.5 + 0.5 * creaseWave) * 0.72 + (0.5 + 0.5 * fineLabialStriae) * 0.28) *
                                     (0.24 + 0.76 * pigment) * (1.0 - stretch * 0.38);
                float mucosaRipple = vnoise3(p * 38.0) * 0.30 + vnoise3(p * 96.0) * 0.14;
                return mix(pores + fineGrain + creaseRidges * 0.44, mucosaRipple + creaseRidges * 0.26, mucosa);
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
                // PASS E: NATURAL GLOSSY KERATIN 3D FINGERNAILS & TOENAILS
                // -----------------------------------------------------------------
                if (uMaterialType > 3.5) {
                    float nailV = clamp(vMat.x, 0.0, 1.0); // 0=cuticle -> 1=free tip
                    float nailU = clamp(vMat.y, 0.0, 1.0); // 0=left fold -> 1=right fold
                    float nailAo = clamp(vMat.z, 0.55, 1.0);
                    float isToe = vMat.w;

                    float uMid = abs(nailU - 0.5) * 2.0;
                    // Natural warm translucent pink nail bed harmonized with Blossom's skin tone
                    vec3 nailBed = mix(uSkinBase * vec3(1.06, 0.89, 0.90), uMucosaBase * vec3(1.10, 0.96, 0.96), 0.34);
                    // Delicate pale-ivory lunula half-moon near proximal cuticle
                    float lunulaCurve = 0.24 * (1.0 - 0.65 * uMid * uMid) * (1.0 - 0.30 * isToe);
                    float lunulaMask = (1.0 - smoothstep(lunulaCurve - 0.06, lunulaCurve + 0.05, nailV)) * smoothstep(0.0, 0.08, nailV);
                    // Crisp translucent pale-ivory free edge tip
                    float tipCurve = 0.79 - 0.06 * uMid * uMid + 0.04 * isToe;
                    float freeEdge = smoothstep(tipCurve - 0.05, tipCurve + 0.06, nailV);
                    vec3 ivoryKeratin = vec3(0.95, 0.91, 0.87);

                    vec3 nailAlbedo = mix(nailBed, mix(nailBed, ivoryKeratin, 0.52), lunulaMask);
                    nailAlbedo = mix(nailAlbedo, ivoryKeratin, freeEdge * 0.78);
                    // Subtle lateral nail-fold & cuticle warmth
                    float edgeShadow = smoothstep(0.68, 1.0, uMid) * 0.16 + (1.0 - smoothstep(0.0, 0.14, nailV)) * 0.14;
                    nailAlbedo *= (1.0 - edgeShadow);

                    float dKeyN = clamp((dot(N, Lkey) + 0.26) / 1.26, 0.0, 1.0);
                    float dFillN = clamp((dot(N, Lfill) + 0.26) / 1.26, 0.0, 1.0);
                    vec3 HkeyN = normalize(Lkey + V);
                    vec3 HfillN = normalize(Lfill + V);
                    // Clean glossy studio keratin specular highlight
                    float spec1 = pow(max(dot(N, HkeyN), 0.0), 86.0) * 0.58;
                    float spec2 = pow(max(dot(N, HfillN), 0.0), 42.0) * 0.22;
                    float nailFresnel = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 3.0) * 0.18;

                    vec3 nailLit = nailAlbedo * (0.28 + dKeyN * 0.66 + dFillN * 0.22) * nailAo +
                                   vec3(1.0, 0.98, 0.97) * (spec1 + spec2 + nailFresnel);
                    vec3 nailMapped = nailLit / (nailLit + vec3(0.74));
                    nailMapped = pow(clamp(nailMapped * 1.28, 0.0, 1.0), vec3(1.01));
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

                    float fineStrands = vnoise3(vWorldPos * vec3(42.0, 26.0, 42.0)) * 0.65 +
                                        vnoise3(vWorldPos * vec3(95.0, 52.0, 95.0)) * 0.35;
                    vec3 hairAlbedo = mix(uHairBase, uHairHighlight, clamp(strandRib * 0.42 + fineStrands * 0.36 + lockBias * 0.54, 0.0, 1.0));

                    float dKeyH = clamp((dot(N, Lkey) + 0.26) / 1.26, 0.0, 1.0);
                    float dFillH = clamp((dot(N, Lfill) + 0.28) / 1.28, 0.0, 1.0);
                    vec3 HkeyH = normalize(Lkey + V);
                    vec3 HfillH = normalize(Lfill + V);

                    // Soft dual-lobe studio hair sheen (primary silk + secondary warm strand highlight)
                    float primarySheen = pow(max(dot(N, HkeyH), 0.0), 28.0) * (0.22 + 0.16 * fineStrands);
                    float secondarySheen = pow(max(dot(N, HfillH), 0.0), 14.0) * 0.12;
                    float rimSheen = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 2.6) * 0.18 * clamp(dot(N, Lrim) * 0.5 + 0.5, 0.2, 1.0);

                    vec3 hairLit = hairAlbedo * (0.26 + dKeyH * 0.74 + dFillH * 0.24) * hairAo +
                                   uHairSheen * primarySheen * hairAo +
                                   uHairHighlight * secondarySheen * 1.15 * hairAo +
                                   uHairSheen * 0.58 * rimSheen * hairAo;
                    vec3 hairMapped = hairLit / (hairLit + vec3(0.72));
                    hairMapped = pow(clamp(hairMapped * 1.26, 0.0, 1.0), vec3(1.01));
                    gl_FragColor = vec4(hairMapped, 1.0);
                    return;
                }

                // -----------------------------------------------------------------
                // PASS C: LUMINOUS SAPPHIRE-BLUE ANATOMICAL EYES (SCLERA + IRIS + CORNEA)
                // -----------------------------------------------------------------
                if (uMaterialType > 1.5) {
                    float irisR = vMat.x;
                    float irisAngle = vMat.y * 6.2831853;
                    float lidAo = clamp(vMat.z, 0.40, 1.0);

                    // Warm natural sclera softening gently near the corners (never stark neon white!)
                    float cornerWarm = smoothstep(0.42, 0.90, irisR);
                    vec3 scleraCol = mix(vec3(0.86, 0.83, 0.81), vec3(0.79, 0.69, 0.67), cornerWarm * 0.44);

                    // Procedural radial iris stroma fibers + natural sapphire/slate-blue gradient + crisp limbal ring
                    float fiber = 0.5 + 0.5 * sin(irisAngle * 24.0 + vnoise3(vWorldPos * 85.0) * 5.0);
                    float fineFiber = 0.5 + 0.5 * cos(irisAngle * 46.0 - irisR * 18.0);
                    vec3 deepSapphire = vec3(0.08, 0.21, 0.38);
                    vec3 brightCerulean = vec3(0.22, 0.47, 0.68);
                    vec3 innerGold = vec3(0.36, 0.45, 0.42);
                    float irisGrad = smoothstep(0.14, 0.32, irisR);
                    vec3 irisCol = mix(brightCerulean, deepSapphire, irisGrad * 0.72);
                    irisCol = mix(irisCol, brightCerulean * 1.16, fiber * 0.34 + fineFiber * 0.16);
                    irisCol = mix(irisCol, innerGold, (1.0 - smoothstep(0.13, 0.22, irisR)) * 0.28);

                    // Natural dark navy limbal ring at outer iris border (r ~ 0.34..0.38)
                    float limbalRing = exp(-pow((irisR - 0.36) / 0.028, 2.0));
                    irisCol = mix(irisCol, vec3(0.04, 0.09, 0.17), limbalRing * 0.78);

                    float inIris = 1.0 - smoothstep(0.355, 0.390, irisR);
                    float inPupil = 1.0 - smoothstep(0.135, 0.168, irisR);

                    vec3 eyeAlbedo = mix(scleraCol, irisCol, inIris);
                    eyeAlbedo = mix(eyeAlbedo, vec3(0.015, 0.016, 0.022), inPupil);

                    float dKeyE = clamp((dot(N, Lkey) + 0.28) / 1.28, 0.0, 1.0);
                    float dFillE = clamp((dot(N, Lfill) + 0.24) / 1.24, 0.0, 1.0);
                    // Subtle lower-iris internal light scatter makes blue eyes sparkle naturally
                    float irisGlow = inIris * (1.0 - inPupil) * smoothstep(0.25, 0.85, 1.0 - vMat.w) * 0.22;

                    vec3 HkeyE = normalize(Lkey + V);
                    vec3 HfillE = normalize(Lfill + V);
                    float corneaGlint1 = pow(max(dot(N, HkeyE), 0.0), 140.0) * 1.15;
                    float corneaGlint2 = pow(max(dot(N, HfillE), 0.0), 90.0) * 0.35;

                    vec3 eyeLit = eyeAlbedo * (0.28 + dKeyE * 0.66 + dFillE * 0.22 + irisGlow) * lidAo +
                                  vec3(1.0, 0.99, 0.98) * (corneaGlint1 + corneaGlint2);
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

                // Multi-octave 3D procedural skin albedo mottling (zero UV seams across unified SDF body & vulva!)
                float nMacro = vnoise3(vWorldPos * 3.8);
                float nMid = vnoise3(vWorldPos * 14.5);
                float nMicro = vnoise3(vWorldPos * 62.0);

                // Analytical finite-difference 3D micro-normal bump for crisp pore, dermal & labial-crease relief
                float eps = 0.0038;
                float hC = skinDetailHeight(vWorldPos, mucosa, pigment, uStretch);
                float hX = skinDetailHeight(vWorldPos + vec3(eps, 0.0, 0.0), mucosa, pigment, uStretch);
                float hY = skinDetailHeight(vWorldPos + vec3(0.0, eps, 0.0), mucosa, pigment, uStretch);

                float bumpGain = mix(0.140, 0.058, mucosa) + pigment * 0.056;
                vec3 microGrad = vec3(hC - hX, hC - hY, 0.0) * (bumpGain / eps * 0.0056);
                N = normalize(N + microGrad);

                // Subtle cavity micro-occlusion inside pores & delicate labial folds
                float microAo = mix(0.88, 1.05, hC);

                // Harmonized organic skin & mucosa albedo — keeps body, areolae, face, outer labia & inner lips living & cohesive
                vec3 skinMottle = mix(uSkinBase * 0.94, uSkinBase * 1.05, nMacro * 0.62 + nMid * 0.38);
                vec3 warmBlush = mix(skinMottle, skinMottle * vec3(1.05, 0.94, 0.92), 0.25 + nMid * 0.25);
                // Velvety-matte dry pigmented skin (areolae, nipples, brows) receives authentic warm dusty-rose/cocoa depth
                float dryPigmentMask = smoothstep(0.28, 0.72, pigment) * (1.0 - smoothstep(0.08, 0.18, wetness));
                float areolaFineGrain = vnoise3(vWorldPos * 140.0) * 0.55 + vnoise3(vWorldPos * 260.0) * 0.45;
                vec3 areolaTone = mix(uSkinShadow * vec3(0.92, 0.76, 0.74), uMucosaBase * vec3(0.84, 0.68, 0.66), 0.54);
                areolaTone *= mix(0.92, 1.06, areolaFineGrain);
                vec3 pigmentedSkin = mix(warmBlush, mix(uSkinShadow * 0.94, uMucosaBase * 0.92, 0.42), pigment * 0.38);
                pigmentedSkin = mix(pigmentedSkin, areolaTone, dryPigmentMask * 0.52);

                // Soft velvety vascular harmonization between surrounding skin and labial mucosa
                vec3 harmonizedMucosa = mix(uSkinBase * vec3(1.03, 0.88, 0.88), uMucosaBase, 0.64);
                vec3 mucosaVar = mix(harmonizedMucosa * 0.92, harmonizedMucosa * 1.10, nMid * 0.55 + 0.28);
                vec3 mucosaRich = mix(mucosaVar, uMucosaDeep, (1.0 - ao) * 0.50);
                vec3 mucosaRimmed = mix(mucosaRich, mix(uMucosaDeep * 0.94, uSkinShadow * 0.92, 0.46), pigment * 0.22);
                vec3 albedo = mix(pigmentedSkin, mucosaRimmed, mucosa * (1.0 - dryPigmentMask * 0.45));

                // Soft, naturally feathered curved mons pubis hair arch (strictly on anterior pelvic surface!)
                float ax = abs(vWorldPos.x);
                float anteriorMask = smoothstep(-0.18, 0.04, vWorldPos.z) * (1.0 - smoothstep(0.55, 0.85, ax));
                float edgeNoise = (nMid - 0.5) * 0.12;
                float monsTopArch = 0.84 - ax * ax * 0.62 + edgeNoise;
                float monsTopFade = 1.0 - smoothstep(monsTopArch - 0.18, monsTopArch + 0.06, vWorldPos.y);
                float monsBotArch = 0.50 + 0.08 * smoothstep(0.0, 0.22, ax) + edgeNoise * 0.4;
                float monsBotFade = smoothstep(monsBotArch - 0.05, monsBotArch + 0.11, vWorldPos.y);
                float monsWidthLimit = 0.25 + 0.26 * smoothstep(0.46, 0.74, vWorldPos.y) + edgeNoise * 0.6;
                float monsSideFade = 1.0 - smoothstep(monsWidthLimit * 0.45, monsWidthLimit, ax);
                float monsHairPatch = monsTopFade * monsBotFade * monsSideFade * (1.0 - mucosa) * anteriorMask;

                float upperLabiaHair = smoothstep(0.19, 0.28, ax) * (1.0 - smoothstep(0.32, 0.43, ax)) *
                                       smoothstep(0.08, 0.46, vWorldPos.y) * (1.0 - smoothstep(0.48, 0.68, vWorldPos.y)) *
                                       (1.0 - mucosa) * anteriorMask * 0.26;
                float totalHairMask = clamp(monsHairPatch + upperLabiaHair, 0.0, 1.0);

                float curl1 = vnoise3(vWorldPos * 18.0) * 2.6;
                float curl2 = vnoise3(vWorldPos * 42.0) * 1.2;
                vec3 hairCoord = vec3(
                    vWorldPos.x * 74.0 + curl1 + curl2,
                    vWorldPos.y * 32.0 + ax * 20.0 - curl1 * 0.60,
                    vWorldPos.z * 28.0
                );
                float hairSoftShadow = smoothstep(0.28, 0.78, vnoise3(hairCoord * 0.52));
                float hairFineStrands = smoothstep(0.52, 0.88, vnoise3(hairCoord * 1.45));
                float hairCoverage = clamp(totalHairMask * (hairSoftShadow * 0.36 + hairFineStrands * 0.48), 0.0, 0.62);

                vec3 hairColor = mix(uSkinShadow * 0.34, uHairBase * 0.85, 0.60);
                albedo = mix(albedo, hairColor, hairCoverage * 0.66);

                float NdotL1 = dot(N, Lkey);
                float NdotL2 = dot(N, Lfill);

                // SPATIAL HIP, INGUINAL & GLUTEAL JOINT SSS UNIFICATION ZONE:
                // Softens dermal wrap scattering & terminator bounce across abdomen, pelvis, buttocks & upper thighs!
                float hipJointX = smoothstep(0.28, 0.56, ax) * (1.0 - smoothstep(1.38, 1.92, ax));
                float hipJointY = smoothstep(-1.42, -0.92, vWorldPos.y) * (1.0 - smoothstep(0.72, 1.24, vWorldPos.y));
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

                // Dual-lobe micro-faceted specular BRDF (dampened softly over mons hair & hip-joint transitions)
                vec3 H1 = normalize(Lkey + V);
                vec3 H2 = normalize(Lfill + V);
                float NdotH1 = max(dot(N, H1), 0.0);
                float NdotH2 = max(dot(N, H2), 0.0);

                float hairSpecDamp = 1.0 - hairCoverage * 0.65;
                float jointSpecSoften = 1.0 - jointSss * 0.22;
                // Natural matte-velvet portrait finish on face & dry pigmented areolae (eliminates synthetic vinyl/doll shine!)
                float faceMatteDamp = 1.0 - smoothstep(5.45, 5.95, vWorldPos.y) * 0.38;
                float areolaMatteDamp = 1.0 - dryPigmentMask * 0.34;
                float oilBreakup = mix(0.76, 1.22, nMicro) * hairSpecDamp * jointSpecSoften * faceMatteDamp * areolaMatteDamp;
                float broadSpec = pow(NdotH1, mix(22.0, 32.0, wetness)) * mix(0.09, 0.16, wetness) * oilBreakup;
                float wetSharpSpec = (pow(NdotH1, mix(70.0, 152.0, wetness)) * wetness * 0.54 +
                                      pow(NdotH2, 84.0) * wetness * 0.16) * faceMatteDamp * areolaMatteDamp;

                // Soft studio rim light sculpting Blossom's thighs, knees, hips, outer labia & hood
                float fresnel = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 3.4);
                float rimLight = fresnel * (0.28 + 0.24 * mucosa) * (0.45 + 0.55 * max(dot(N, Lrim), 0.0));

                // Colored Hemoglobin AO Bounce: prevents crevices from looking muddy gray & softens hip/lip/body contrast
                float fullAo = clamp(ao * microAo, 0.06, 1.0);
                vec3 aoExponents = mix(vec3(0.76, 1.00, 1.18), vec3(0.68, 0.90, 1.06), jointSss * 0.65);
                vec3 coloredAo = pow(vec3(fullAo), aoExponents);

                vec3 ambient = albedo * (0.19 + 0.08 * (N.y * 0.5 + 0.5));
                vec3 keyColor = vec3(1.03, 0.96, 0.92);
                vec3 fillColor = vec3(0.98, 0.93, 0.90);

                vec3 lit = ambient * coloredAo +
                           albedo * (diffKeyRGB * 0.78 * keyColor + diffFillRGB * 0.22 * fillColor) * (vec3(0.28) + 0.72 * coloredAo) +
                           sssTint * (terminator * 0.34 + grazingTrans * 0.20 + thinTransmit * (0.22 + 0.20 * uLightMode)) * coloredAo +
                           vec3(1.0, 0.96, 0.94) * (broadSpec + wetSharpSpec) * (0.28 + 0.72 * fullAo) +
                           mix(uSkinBase, sssTint, 0.46) * rimLight;

                // Soft warm subsurface glow right under active fingertip touch
                if (uTouchIntensity > 0.005) {
                    float dTouch = length(vWorldPos.xy - uTouchPos);
                    float glowMask = exp(-dTouch * dTouch * 14.0) * uTouchIntensity;
                    lit += uTouchColor * glowMask * 0.28;
                }

                // Balanced photographic filmic tone mapping (keeps skin warm, luminous & organic across all 6 tones)
                vec3 mapped = lit / (lit + vec3(0.76));
                mapped = pow(clamp(mapped * 1.32, 0.0, 1.0), vec3(1.01));

                gl_FragColor = vec4(mapped, 1.0);
            }
        """
    }
}
