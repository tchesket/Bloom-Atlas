package com.example.ui.components

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-resolution upper-body anatomical & facial geometry builders for Blossom v4.1:
 *  1. 10 Proportionate Relaxed 3D Fingers & Thumbs (exact outward CCW winding on both hands)
 *  2. Live-Deformable Soft Rounded Medium Breasts (exact outward CCW winding -> exact skin-tone
 *     match with body, natural supine teardrop contour, soft feathered areolae, rounded 3D nipples)
 *  3. Sculptural Realistic Feminine Head & Face (96x88 bottom-to-top outward CCW mesh -> seamless
 *     skin lighting & neck join, calm serene almond blue eyes, soft eyebrows, dainty nose & lips)
 *  4. Glossy Natural Sapphire-Blue 3D Eyes (calm relaxed gaze, upper eyelids grazing upper iris)
 *  5. Stylish Layered Dark-Brown / Soft-Black Pixie Haircut (dimensional crown + 24 sweeping locks)
 */
internal object BlossomUpperBodyBuilder {

    // Classical adult female head (1.12x scale -> 2.10 shoulder-to-head canon) resting elevated on ergonomic pillow
    const val HEAD_CENTER_Y = 6.38f
    val HEAD_CENTER_Z: Float = BlossomBodyBuilder.mattressSurfaceZ(HEAD_CENTER_Y) + 1.24f
    private const val HEAD_SCALE = 1.12f
    private const val HEAD_PITCH_RAD = 0.14f
    private val COS_PITCH = cos(HEAD_PITCH_RAD)
    private val SIN_PITCH = sin(HEAD_PITCH_RAD)

    // True classical anatomical chest center aligned to slimmer feminine ribcage (64x60 center-clustered areola/nipple detail)
    const val BREAST_CENTER_AX = 0.80f
    const val BREAST_CENTER_Y = 3.96f
    const val BREAST_FOOTPRINT_R = 0.82f
    const val BREAST_COLS = 64
    const val BREAST_ROWS = 60

    private fun headLocalToWorld(lx: Float, ly: Float, lz: Float): FloatArray {
        val sx = lx * HEAD_SCALE
        val sy = ly * HEAD_SCALE
        val sz = lz * HEAD_SCALE
        val wy = HEAD_CENTER_Y + sy * COS_PITCH - sz * SIN_PITCH
        val wz = HEAD_CENTER_Z + sy * SIN_PITCH + sz * COS_PITCH
        return floatArrayOf(sx, wy, wz)
    }

    private fun headNormalToWorld(nx: Float, ny: Float, nz: Float): FloatArray {
        val wy = ny * COS_PITCH - nz * SIN_PITCH
        val wz = ny * SIN_PITCH + nz * COS_PITCH
        val len = sqrt(nx * nx + wy * wy + wz * wz).coerceAtLeast(1e-5f)
        return floatArrayOf(nx / len, wy / len, wz / len)
    }

    internal data class BlossomFingerSpec(
        val rootAx: Float,
        val rootY: Float,
        val rootZ: Float,
        val length: Float,
        val rLat: Float,
        val rZ: Float,
        val yawOut: Float,
        val archLift: Float,
        val tipCurlDown: Float,
        val isThumb: Boolean = false
    )

    internal val BLOSSOM_FINGER_SPECS: List<BlossomFingerSpec> by lazy {
        val zPalm = BlossomBodyBuilder.mattressSurfaceZ(0.36f) + 0.185f
        // Fuller, naturally arched feminine fingers anchored cleanly to lateral sculpted palms (zero hip merge!)
        listOf(
            BlossomFingerSpec(rootAx = 2.10f, rootY = 0.34f, rootZ = zPalm + 0.018f, length = 0.43f, rLat = 0.064f, rZ = 0.056f, yawOut = -0.24f, archLift = 0.042f, tipCurlDown = 0.048f, isThumb = true),
            BlossomFingerSpec(rootAx = 2.16f, rootY = 0.00f, rootZ = zPalm + 0.016f, length = 0.56f, rLat = 0.054f, rZ = 0.050f, yawOut = -0.02f, archLift = 0.054f, tipCurlDown = 0.062f),
            BlossomFingerSpec(rootAx = 2.27f, rootY = -0.03f, rootZ = zPalm + 0.022f, length = 0.62f, rLat = 0.056f, rZ = 0.052f, yawOut = 0.01f, archLift = 0.058f, tipCurlDown = 0.068f),
            BlossomFingerSpec(rootAx = 2.38f, rootY = 0.00f, rootZ = zPalm + 0.016f, length = 0.56f, rLat = 0.052f, rZ = 0.049f, yawOut = 0.04f, archLift = 0.052f, tipCurlDown = 0.062f),
            BlossomFingerSpec(rootAx = 2.48f, rootY = 0.05f, rootZ = zPalm + 0.006f, length = 0.44f, rLat = 0.046f, rZ = 0.044f, yawOut = 0.08f, archLift = 0.042f, tipCurlDown = 0.050f)
        )
    }

    // -------------------------------------------------------------------------
    // 1. 10 PROPORTIONATE RELAXED 3D FINGERS & THUMBS (OUTWARD CCW BOTH HANDS)
    // -------------------------------------------------------------------------
    fun buildBlossomHandsMesh(): DynamicGlMesh {
        val floorZ = BlossomBodyBuilder.MATTRESS_TOP_Z + 0.015f
        val fingerSpecs = BLOSSOM_FINGER_SPECS

        val cols = 16
        val rows = 14
        val vertsPerFinger = cols * rows
        val trisPerFinger = (cols - 1) * (rows - 1) * 6
        val totalFingers = fingerSpecs.size * 2

        val mesh = DynamicGlMesh(vertsPerFinger * totalFingers, trisPerFinger * totalFingers)
        val indices = ShortArray(trisPerFinger * totalFingers)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        var fingerIdx = 0
        var idxOut = 0
        val sides = floatArrayOf(-1f, +1f)

        for (side in sides) {
            for (spec in fingerSpecs) {
                val baseVert = fingerIdx * vertsPerFinger
                for (r in 0 until rows - 1) {
                    for (c in 0 until cols - 1) {
                        val i0 = (baseVert + r * cols + c).toShort()
                        val i1 = (baseVert + r * cols + c + 1).toShort()
                        val i2 = (baseVert + (r + 1) * cols + c).toShort()
                        val i3 = (baseVert + (r + 1) * cols + c + 1).toShort()
                        // Strictly outward CCW winding on BOTH left (+1) and right (-1) hands
                        if (side > 0f) {
                            indices[idxOut++] = i0; indices[idxOut++] = i1; indices[idxOut++] = i2
                            indices[idxOut++] = i1; indices[idxOut++] = i3; indices[idxOut++] = i2
                        } else {
                            indices[idxOut++] = i0; indices[idxOut++] = i2; indices[idxOut++] = i1
                            indices[idxOut++] = i1; indices[idxOut++] = i2; indices[idxOut++] = i3
                        }
                    }
                }

                val fwdX = side * sin(spec.yawOut)
                val fwdY = -cos(spec.yawOut)
                val latX = side * cos(spec.yawOut)
                val latY = sin(spec.yawOut)

                for (r in 0 until rows) {
                    val s = r.toFloat() / (rows - 1)
                    val tipT = smoothStep(0.84f, 1.0f, s)
                    val tipAngle = tipT * (PI.toFloat() * 0.5f)
                    val tipScale = cos(tipAngle).coerceAtLeast(0.025f)

                    val rootFlare = 1f + 0.16f * (1f - smoothStep(0f, 0.24f, s))
                    val distalTaper = 1f - 0.22f * smoothStep(0.12f, 0.90f, s)
                    val pipJoint = 1f + 0.045f * exp(-((s - 0.36f) / 0.09f).let { it * it })
                    val dipJoint = 1f + 0.032f * exp(-((s - 0.70f) / 0.09f).let { it * it })
                    val radScale = rootFlare * distalTaper * pipJoint * dipJoint * tipScale

                    val along = -0.115f + s * (spec.length + 0.115f)
                    val curlZ = spec.archLift * sin(s * PI.toFloat()) - spec.tipCurlDown * smoothStep(0.32f, 1.0f, s)
                    val medialCurve = if (spec.isThumb) {
                        0.024f * smoothStep(0.25f, 1.0f, s)
                    } else {
                        -0.014f * smoothStep(0.30f, 1.0f, s)
                    }

                    val cx = side * spec.rootAx + fwdX * along + latX * medialCurve
                    val cy = spec.rootY + fwdY * along + latY * medialCurve
                    val cz = spec.rootZ + curlZ

                    for (c in 0 until cols) {
                        val u = c.toFloat() / (cols - 1)
                        val phi = u * (2f * PI.toFloat())
                        val cosP = cos(phi)
                        val sinP = sin(phi)

                        val nailMask = smoothBell(s, 0.76f, 0.95f, 0.05f) * smoothStep(0.45f, 0.92f, sinP) *
                            (1f - smoothStep(0.55f, 0.88f, abs(cosP)))
                        val nailIndent = -0.003f * nailMask

                        val localLat = spec.rLat * radScale * cosP
                        var localZ = spec.rZ * radScale * sinP + nailIndent
                        if (sinP < 0f) localZ *= 0.88f

                        val px = cx + latX * localLat
                        val py = cy + latY * localLat
                        val pz = (cz + localZ).coerceAtLeast(floorZ)

                        val radialW = cos(tipAngle)
                        val fwdW = sin(tipAngle)
                        var nx = latX * (cosP * radialW / spec.rLat) + fwdX * (fwdW / spec.rLat)
                        var ny = latY * (cosP * radialW / spec.rLat) + fwdY * (fwdW / spec.rLat)
                        var nz = sinP * radialW / spec.rZ
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen

                        // Seamless normal fusion into unified SDF palm at knuckle root
                        val rootBlend = 1f - smoothStep(0f, 0.26f, s)
                        if (rootBlend > 0.001f) {
                            val sdfNorm = BlossomSdfBodyBuilder.evaluateBodyNormal(px, py, pz, eps = 0.016f)
                            val mx = nx * (1f - rootBlend) + sdfNorm[0] * rootBlend
                            val my = ny * (1f - rootBlend) + sdfNorm[1] * rootBlend
                            val mz = nz * (1f - rootBlend) + sdfNorm[2] * rootBlend
                            val mLen = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                            nx = mx / mLen; ny = my / mLen; nz = mz / mLen
                        }

                        val interdigitalAo = 1f - abs(cosP) * 0.20f * (1f - smoothStep(0.08f, 0.62f, s))
                        val padAo = 1f - smoothStep(0.10f, 0.85f, -sinP) * 0.28f
                        val ao = (interdigitalAo * padAo).coerceIn(0.42f, 1f)
                        val wetness = (0.055f + nailMask * 0.24f).coerceIn(0f, 0.45f)
                        val pigment = (0.05f + smoothStep(0.68f, 0.96f, s) * 0.06f + nailMask * 0.14f).coerceIn(0f, 1f)

                        val vIdx = (baseVert + r * cols + c) * stride
                        data[vIdx] = px; data[vIdx + 1] = py; data[vIdx + 2] = pz
                        data[vIdx + 3] = nx; data[vIdx + 4] = ny; data[vIdx + 5] = nz
                        data[vIdx + 6] = nailMask * 0.16f
                        data[vIdx + 7] = wetness
                        data[vIdx + 8] = ao
                        data[vIdx + 9] = pigment
                    }
                }
                fingerIdx++
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return mesh
    }

    // -------------------------------------------------------------------------
    // 1B. 20 SCULPTED 3D KERATIN FINGERNAILS (10) & TOENAILS (10) — OUTWARD CCW!
    //     Returns Pair(nailsMesh, toeNailStartVert) so toenails track leg spread!
    // -------------------------------------------------------------------------
    fun buildBlossomNailsMesh(): Pair<DynamicGlMesh, Int> {
        val nailCols = 10
        val nailRows = 8
        val vertsPerNail = nailCols * nailRows
        val trisPerNail = (nailCols - 1) * (nailRows - 1) * 6

        val fingerSpecs = BLOSSOM_FINGER_SPECS
        val toeSpecs = BlossomSdfBodyBuilder.BLOSSOM_TOE_SPECS
        val totalNails = (fingerSpecs.size + toeSpecs.size) * 2
        val toeNailStartVert = fingerSpecs.size * 2 * vertsPerNail

        val mesh = DynamicGlMesh(vertsPerNail * totalNails, trisPerNail * totalNails)
        val indices = ShortArray(trisPerNail * totalNails)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val sides = floatArrayOf(-1f, +1f)

        var nailIdx = 0
        var idxOut = 0

        fun emitNailIndices(baseVert: Int, side: Float) {
            for (r in 0 until nailRows - 1) {
                for (c in 0 until nailCols - 1) {
                    val i0 = (baseVert + r * nailCols + c).toShort()
                    val i1 = (baseVert + r * nailCols + c + 1).toShort()
                    val i2 = (baseVert + (r + 1) * nailCols + c).toShort()
                    val i3 = (baseVert + (r + 1) * nailCols + c + 1).toShort()
                    // d/dc = +lat, d/dr = +fwd -> side=+1 needs fwd x lat (+Z); side=-1 needs lat x fwd (+Z)
                    if (side > 0f) {
                        indices[idxOut++] = i0; indices[idxOut++] = i2; indices[idxOut++] = i1
                        indices[idxOut++] = i1; indices[idxOut++] = i2; indices[idxOut++] = i3
                    } else {
                        indices[idxOut++] = i0; indices[idxOut++] = i1; indices[idxOut++] = i2
                        indices[idxOut++] = i1; indices[idxOut++] = i3; indices[idxOut++] = i2
                    }
                }
            }
        }

        // Part 1: 10 Curved Almond Fingernails seated flush on dorsal distal phalanges
        for (side in sides) {
            for (spec in fingerSpecs) {
                val baseVert = nailIdx * vertsPerNail
                emitNailIndices(baseVert, side)

                val fwdX = side * sin(spec.yawOut)
                val fwdY = -cos(spec.yawOut)
                val latX = side * cos(spec.yawOut)
                val latY = sin(spec.yawOut)

                for (r in 0 until nailRows) {
                    val v = r.toFloat() / (nailRows - 1)
                    val s = 0.72f + v * 0.245f

                    val tipT = smoothStep(0.84f, 1.0f, s)
                    val tipAngle = tipT * (PI.toFloat() * 0.5f)
                    val tipScale = cos(tipAngle).coerceAtLeast(0.025f)
                    val rootFlare = 1f + 0.16f * (1f - smoothStep(0f, 0.24f, s))
                    val distalTaper = 1f - 0.22f * smoothStep(0.12f, 0.90f, s)
                    val pipJoint = 1f + 0.045f * exp(-((s - 0.36f) / 0.09f).let { it * it })
                    val dipJoint = 1f + 0.032f * exp(-((s - 0.70f) / 0.09f).let { it * it })
                    val radScale = rootFlare * distalTaper * pipJoint * dipJoint * tipScale

                    val along = -0.115f + s * (spec.length + 0.115f)
                    val curlZ = spec.archLift * sin(s * PI.toFloat()) - spec.tipCurlDown * smoothStep(0.32f, 1.0f, s)
                    val medialCurve = if (spec.isThumb) {
                        0.024f * smoothStep(0.25f, 1.0f, s)
                    } else {
                        -0.014f * smoothStep(0.30f, 1.0f, s)
                    }

                    val cx = side * spec.rootAx + fwdX * along + latX * medialCurve
                    val cy = spec.rootY + fwdY * along + latY * medialCurve
                    val cz = spec.rootZ + curlZ

                    // Graceful rounded cuticle & almond free edge profile
                    val vCentered = v * 2f - 1f
                    val almondWidth = sqrt((1f - vCentered * vCentered * 0.48f).coerceAtLeast(0.22f))

                    for (c in 0 until nailCols) {
                        val u = c.toFloat() / (nailCols - 1)
                        val uCentered = u * 2f - 1f
                        val phi = (PI.toFloat() * 0.5f) - uCentered * (0.32f * PI.toFloat()) * almondWidth
                        val cosP = cos(phi)
                        val sinP = sin(phi)

                        val radialW = cos(tipAngle * 0.65f)
                        val fwdW = sin(tipAngle * 0.65f)
                        var nx = latX * (cosP * radialW / spec.rLat) + fwdX * (fwdW / spec.rLat)
                        var ny = latY * (cosP * radialW / spec.rLat) + fwdY * (fwdW / spec.rLat)
                        var nz = sinP * radialW / spec.rZ
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen

                        // Raised smooth natural keratin plate sitting cleanly atop the finger tip
                        val edgeBevel = sin(v * PI.toFloat()).coerceAtLeast(0.15f) * cos(uCentered * PI.toFloat() * 0.46f)
                        val plateLift = 0.0055f * (0.45f + 0.55f * edgeBevel)
                        val localLat = spec.rLat * radScale * cosP
                        val localZ = spec.rZ * radScale * sinP

                        val px = cx + latX * localLat + nx * plateLift
                        val py = cy + latY * localLat + ny * plateLift
                        val pz = cz + localZ + nz * plateLift

                        val nailAo = (0.86f + 0.14f * cos(uCentered * PI.toFloat() * 0.45f) -
                            0.14f * (1f - smoothStep(0f, 0.20f, v))).coerceIn(0.62f, 1f)

                        val vIdx = (baseVert + r * nailCols + c) * stride
                        data[vIdx] = px; data[vIdx + 1] = py; data[vIdx + 2] = pz
                        data[vIdx + 3] = nx; data[vIdx + 4] = ny; data[vIdx + 5] = nz
                        data[vIdx + 6] = v
                        data[vIdx + 7] = u
                        data[vIdx + 8] = nailAo
                        data[vIdx + 9] = 0.0f // fingernail
                    }
                }
                nailIdx++
            }
        }

        // Part 2: 10 Sculpted Natural Toenails seated flush on dorsal distal toes
        for (side in sides) {
            for (spec in toeSpecs) {
                val baseVert = nailIdx * vertsPerNail
                emitNailIndices(baseVert, side)

                val fwdX = side * sin(spec.yawOut)
                val fwdY = -cos(spec.yawOut)
                val latX = side * cos(spec.yawOut)
                val latY = sin(spec.yawOut)

                for (r in 0 until nailRows) {
                    val v = r.toFloat() / (nailRows - 1)
                    val s = 0.56f + v * 0.34f

                    val tipT = smoothStep(0.80f, 1.0f, s)
                    val tipAngle = tipT * (PI.toFloat() * 0.5f)
                    val tipScale = cos(tipAngle).coerceAtLeast(0.025f)
                    val rootFlare = 1f + 0.14f * (1f - smoothStep(0.0f, 0.22f, s))
                    val phalanxNeck = 1f - 0.09f * exp(-((s - 0.38f) / 0.18f).let { it * it })
                    val distalPad = 1f + 0.09f * exp(-((s - 0.74f) / 0.16f).let { it * it })
                    val radScale = rootFlare * phalanxNeck * distalPad * tipScale

                    val along = -0.095f + s * (spec.length + 0.095f)
                    val curlZ = spec.archLift * sin(s * PI.toFloat()) - 0.024f * smoothStep(0.42f, 0.92f, s)
                    val medialCurve = -0.014f * smoothStep(0.30f, 1.0f, s) * (if (spec.rLat < 0.048f) 1f else 0.22f)

                    val cx = side * spec.rootAx + fwdX * along + latX * medialCurve
                    val cy = spec.rootY + fwdY * along + latY * medialCurve
                    val cz = spec.rootZ + curlZ

                    val vCentered = v * 2f - 1f
                    val toePlateWidth = sqrt((1f - vCentered * vCentered * 0.28f).coerceAtLeast(0.35f))

                    for (c in 0 until nailCols) {
                        val u = c.toFloat() / (nailCols - 1)
                        val uCentered = u * 2f - 1f
                        val phi = (PI.toFloat() * 0.5f) - uCentered * (0.33f * PI.toFloat()) * toePlateWidth
                        val cosP = cos(phi)
                        val sinP = sin(phi)

                        val radialW = cos(tipAngle * 0.55f)
                        val fwdW = sin(tipAngle * 0.55f)
                        var nx = latX * (cosP * radialW / spec.rLat) + fwdX * (fwdW / spec.rLat)
                        var ny = latY * (cosP * radialW / spec.rLat) + fwdY * (fwdW / spec.rLat)
                        var nz = sinP * radialW / spec.rZ
                        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                        nx /= nLen; ny /= nLen; nz /= nLen

                        val edgeBevel = sin(v * PI.toFloat()).coerceAtLeast(0.18f) * cos(uCentered * PI.toFloat() * 0.45f)
                        val plateLift = 0.0062f * (0.48f + 0.52f * edgeBevel)
                        val localLat = spec.rLat * radScale * cosP
                        val localZ = spec.rZ * radScale * sinP

                        val px = cx + latX * localLat + nx * plateLift
                        val py = cy + latY * localLat + ny * plateLift
                        val pz = cz + localZ + nz * plateLift

                        val nailAo = (0.85f + 0.15f * cos(uCentered * PI.toFloat() * 0.45f) -
                            0.14f * (1f - smoothStep(0f, 0.22f, v))).coerceIn(0.60f, 1f)

                        val vIdx = (baseVert + r * nailCols + c) * stride
                        data[vIdx] = px; data[vIdx + 1] = py; data[vIdx + 2] = pz
                        data[vIdx + 3] = nx; data[vIdx + 4] = ny; data[vIdx + 5] = nz
                        data[vIdx + 6] = v
                        data[vIdx + 7] = u
                        data[vIdx + 8] = nailAo
                        data[vIdx + 9] = 1.0f // toenail
                    }
                }
                nailIdx++
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return Pair(mesh, toeNailStartVert)
    }

    // -------------------------------------------------------------------------
    // 2. LIVE-DEFORMABLE SOFT ROUNDED MEDIUM BREASTS (EXACT OUTWARD CCW WINDING!)
    // -------------------------------------------------------------------------
    class BreastSideCache(val side: Float, val cols: Int, val rows: Int) {
        val count = cols * rows
        val baseX = FloatArray(count)
        val baseY = FloatArray(count)
        val baseZ = FloatArray(count)
        val baseNx = FloatArray(count)
        val baseNy = FloatArray(count)
        val baseNz = FloatArray(count)
        val baseAo = FloatArray(count)
        val basePigment = FloatArray(count)
        val localEx = FloatArray(count)
        val localEy = FloatArray(count)
        val edgeR = FloatArray(count)
    }

    class BlossomBreastsCache(
        val left: BreastSideCache,
        val right: BreastSideCache
    )

    fun buildBreastsCache(): BlossomBreastsCache {
        fun sampleSide(side: Float): BreastSideCache {
            val cache = BreastSideCache(side, BREAST_COLS, BREAST_ROWS)
            val cx = side * BREAST_CENTER_AX
            val cy = BREAST_CENTER_Y
            val rx = BREAST_FOOTPRINT_R
            val ry = BREAST_FOOTPRINT_R

            for (r in 0 until BREAST_ROWS) {
                val vRaw = (r.toFloat() / (BREAST_ROWS - 1)) * 2f - 1f
                // Tight radial center clustering (~1mm vertex spacing across areola & nipple papilla!)
                val v = 0.44f * vRaw + 0.56f * vRaw * vRaw * vRaw
                for (c in 0 until BREAST_COLS) {
                    val uRaw = (c.toFloat() / (BREAST_COLS - 1)) * 2f - 1f
                    val u = 0.44f * uRaw + 0.56f * uRaw * uRaw * uRaw

                    val ex = u * sqrt((1f - 0.5f * v * v).coerceAtLeast(0.0001f))
                    val ey = v * sqrt((1f - 0.5f * u * u).coerceAtLeast(0.0001f))
                    val edgeR = sqrt(ex * ex + ey * ey).coerceIn(0f, 1f)

                    val x = cx + ex * rx
                    val y = cy + ey * ry
                    val sample = BlossomSdfBodyBuilder.sampleBodySurfacePoint(x, y)

                    val idx = r * BREAST_COLS + c
                    cache.baseX[idx] = x
                    cache.baseY[idx] = y
                    cache.baseZ[idx] = sample.z
                    cache.baseNx[idx] = sample.nx
                    cache.baseNy[idx] = sample.ny
                    cache.baseNz[idx] = sample.nz
                    cache.baseAo[idx] = sample.ao
                    cache.basePigment[idx] = sample.pigment
                    cache.localEx[idx] = ex
                    cache.localEy[idx] = ey
                    cache.edgeR[idx] = edgeR
                }
            }
            return cache
        }
        return BlossomBreastsCache(left = sampleSide(-1f), right = sampleSide(+1f))
    }

    /**
     * Exact CCW outward winding (i0, i1, i2), (i1, i3, i2) so gl_FrontFacing == true across
     * the entire breast surface and lighting/skin color matches the torso 100% seamlessly!
     */
    fun buildBreastsIndices(): ShortArray {
        val cols = BREAST_COLS
        val rows = BREAST_ROWS
        val vertsPerSide = cols * rows
        val trisPerSide = (cols - 1) * (rows - 1) * 6
        val indices = ShortArray(trisPerSide * 2)
        var out = 0
        for (sideIdx in 0..1) {
            val base = sideIdx * vertsPerSide
            for (r in 0 until rows - 1) {
                for (c in 0 until cols - 1) {
                    val i0 = (base + r * cols + c).toShort()
                    val i1 = (base + r * cols + c + 1).toShort()
                    val i2 = (base + (r + 1) * cols + c).toShort()
                    val i3 = (base + (r + 1) * cols + c + 1).toShort()
                    indices[out++] = i0; indices[out++] = i1; indices[out++] = i2
                    indices[out++] = i1; indices[out++] = i3; indices[out++] = i2
                }
            }
        }
        return indices
    }

    /**
     * Real-time 60 FPS update of Blossom's soft rounded medium anatomical breasts with interactive 2D
     * soft-tissue mobility, exact torso skin-color match, softly feathered areolae, and rounded 3D nipples.
     */
    fun updateBlossomBreastsMesh(
        mesh: DynamicGlMesh,
        cache: BlossomBreastsCache,
        state: VulvaDeformationState,
        computeNormals: (FloatArray, Int, Int, Boolean, Boolean) -> Unit
    ) {
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX
        val sideBuf = FloatArray(cache.left.count * stride)

        val sizeNorm = state.breastSize.coerceIn(0f, 1f)
        // Smooth anatomical breast volume scaling: 0.0 (Petite 0.58x) -> 0.5 (Medium 1.00x) -> 1.0 (Full 1.48x)
        val sizeFactor = 0.58f + sizeNorm * 0.90f
        val domePower = 1.64 - sizeNorm * 0.30

        fun updateSide(sideCache: BreastSideCache, shiftX: Float, shiftY: Float, sideOffsetVerts: Int) {
            val side = sideCache.side
            val sx = shiftX.coerceIn(-1f, 1f) * (0.20f + 0.09f * sizeNorm)
            val sy = shiftY.coerceIn(-1f, 1f) * (0.18f + 0.08f * sizeNorm)
            val medialGather = (-sx * side).coerceIn(-0.16f, 0.22f)
            val liftFullness = (sy * 0.18f).coerceIn(-0.04f, 0.05f)

            // Natural supine apex settles softly laterally & slightly inferiorly under gravity (slightly fuller drape when larger)
            val apexEx = side * (0.11f + 0.04f * sizeNorm)
            val apexEy = -(0.055f + 0.030f * sizeNorm)

            for (i in 0 until sideCache.count) {
                val ex = sideCache.localEx[i]
                val ey = sideCache.localEy[i]
                val edgeR = sideCache.edgeR[i]

                // Ultra-smooth wide collar envelope melting outer boundary seamlessly into the slim ribcage
                val collarFade = 1f - smoothStep(0.68f, 0.96f, edgeR)
                val softShiftW = (1f - smoothStep(0.0f, 0.92f, edgeR)).let { t -> t * t * (3f - 2f * t) }

                // Natural supine teardrop breast contour: gentle linear upper slope -> plush rounded lower/lateral fullness
                val dxRel = ex - apexEx
                val dyRel = ey - apexEy
                val dxA = dxRel / (if (dxRel * side > 0f) 0.92f else 0.84f)
                val dyA = dyRel / (if (dyRel > 0f) 0.96f else 0.78f)
                val rDome = sqrt(dxA * dxA + dyA * dyA).coerceIn(0f, 1f)
                val cosHem = cos(rDome * (PI.toFloat() * 0.5f)).coerceAtLeast(0f)
                val roundedDome = Math.pow(cosHem.toDouble(), domePower).toFloat() * collarFade
                // Soften upper pole gently toward clavicles so breasts flow seamlessly out of the chest wall
                val upperSlopeSoften = 1f - (0.26f - 0.06f * sizeNorm) * smoothStep(0.08f, 0.86f, ey)
                val lowerPlush = 1f + (0.08f + 0.08f * sizeNorm) * smoothStep(-0.08f, -0.58f, ey) * (1f - smoothStep(0.62f, 0.92f, edgeR))
                val moundHeight = (0.46f * sizeFactor + medialGather * 0.18f + liftFullness) * roundedDome * upperSlopeSoften * lowerPlush

                // High-resolution anatomical Areola (~4.2 cm organic disc + subtle Montgomery tubercles) & realistic cylindrical-domed Nipple Papilla
                val relWx = dxRel * BREAST_FOOTPRINT_R
                val relWy = dyRel * BREAST_FOOTPRINT_R
                val distApexWorld = sqrt(relWx * relWx + relWy * relWy)
                val ang = kotlin.math.atan2(relWy, relWx)
                val organicEdge = 1f + 0.032f * sin(ang * 3f + side * 1.1f) + 0.020f * cos(ang * 5f - side * 0.7f)
                val distOrg = distApexWorld / organicEdge

                // Soft organically feathered areola pigment & subtle cushioned elevation
                val areolaScale = 0.88f + 0.24f * sizeNorm
                val areolaMask = (1f - smoothStep(0.155f * areolaScale, 0.232f * areolaScale, distOrg)) * collarFade
                val areolaCushion = 0.010f * (1f - smoothStep(0.04f, 0.215f * areolaScale, distOrg)) * collarFade
                // Subtle tactile Montgomery gland micro-pebbling around outer areola ring (r ~ 0.10..0.18)
                val montRing = smoothBell(distApexWorld, 0.095f * areolaScale, 0.185f * areolaScale, 0.025f)
                val montWave = (sin(ang * 9f + side * 0.9f) * cos(distApexWorld * 62f)).coerceAtLeast(0f)
                val montBump = 0.0038f * montRing * montWave * montWave * collarFade

                // Crisp natural peri-nipple basal groove separating papilla cleanly from surrounding areola
                val basalGroove = -0.0075f * exp(-((distApexWorld - 0.078f) / 0.016f).let { it * it }) * collarFade

                // Realistic cylindrical-domed 3D nipple papilla (broad soft rounded top, NEVER a pointy cone!)
                val nipR = 0.068f
                val nipT = (distApexWorld / nipR).coerceIn(0f, 1.4f)
                val nippleDome = if (nipT < 1f) {
                    val flatCrown = Math.pow((1.0 - Math.pow(nipT.toDouble(), 3.2)), 0.46).toFloat()
                    // Micro organic lobular tip texture (breaks up synthetic CAD perfection)
                    val tipOrganic = 1f + 0.025f * sin(ang * 4f + 0.5f) * smoothStep(0.15f, 0.65f, nipT)
                    0.062f * flatCrown * tipOrganic * collarFade
                } else {
                    0f
                }
                val nippleMask = (1f - smoothStep(0.056f, 0.080f, distApexWorld)) * collarFade

                val collarBias = 0.0024f * smoothStep(0.74f, 0.88f, edgeR) * (1f - smoothStep(0.92f, 0.96f, edgeR)) -
                    0.0036f * smoothStep(0.95f, 1.0f, edgeR)
                val totalLift = moundHeight + areolaCushion + montBump + basalGroove + nippleDome + collarBias

                // Blend chest-wall outward normal with anterior +Z and subtle lateral supine drape
                val nxB = sideCache.baseNx[i] * 0.34f + side * (0.09f + 0.05f * sizeNorm) * softShiftW
                val nyB = sideCache.baseNy[i] * 0.22f - (0.04f + 0.03f * sizeNorm) * softShiftW
                val nzB = (sideCache.baseNz[i] * 0.56f + 0.44f).coerceAtLeast(0.50f)
                val dirLen = sqrt(nxB * nxB + nyB * nyB + nzB * nzB).coerceAtLeast(1e-5f)

                val px = sideCache.baseX[i] + (nxB / dirLen) * totalLift + sx * softShiftW
                val py = sideCache.baseY[i] + (nyB / dirLen) * totalLift + sy * softShiftW
                val pz = sideCache.baseZ[i] + (nzB / dirLen) * totalLift +
                    (medialGather * 0.07f - abs(sx) * 0.04f) * softShiftW

                // Natural inframammary crease AO & subtle basal nipple shadow ring
                val infraMask = exp(-(((ey + 0.72f) / 0.18f).let { it * it } + (ex / 0.64f).let { it * it })) * collarFade
                val nippleRimAo = exp(-((distApexWorld - 0.076f) / 0.017f).let { it * it }) * 0.19f * collarFade
                val areolaRimSoft = (0.04f * montRing * (1f - montWave)) * collarFade
                val ao = (sideCache.baseAo[i] * (1f - (0.11f + 0.11f * sizeNorm) * infraMask - nippleRimAo - areolaRimSoft)).coerceIn(0.24f, 1f)

                // Realistic velvety-matte warm dusty-rose / cocoa-rose areola & deeper papilla (ZERO plastic gloss!)
                val mucosa = (areolaMask * 0.42f + nippleMask * 0.22f).coerceIn(0f, 0.66f)
                val wetness = (0.042f - areolaMask * 0.008f + nippleMask * 0.022f).coerceIn(0.025f, 0.08f)
                val pigment = (sideCache.basePigment[i] + areolaMask * 0.54f + nippleMask * 0.28f).coerceIn(0f, 0.96f)

                val idx = i * stride
                sideBuf[idx] = px; sideBuf[idx + 1] = py; sideBuf[idx + 2] = pz
                sideBuf[idx + 6] = mucosa
                sideBuf[idx + 7] = wetness
                sideBuf[idx + 8] = ao
                sideBuf[idx + 9] = pigment
            }

            computeNormals(sideBuf, sideCache.cols, sideCache.rows, false, false)

            // Exact outer-collar normal fusion into unified SDF ribcage -> ZERO seam!
            for (i in 0 until sideCache.count) {
                val edgeR = sideCache.edgeR[i]
                val nBlend = smoothStep(0.68f, 0.94f, edgeR)
                val idx = i * stride
                if (nBlend > 0.001f) {
                    val mx = sideBuf[idx + 3] * (1f - nBlend) + sideCache.baseNx[i] * nBlend
                    val my = sideBuf[idx + 4] * (1f - nBlend) + sideCache.baseNy[i] * nBlend
                    val mz = sideBuf[idx + 5] * (1f - nBlend) + sideCache.baseNz[i] * nBlend
                    val len = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                    sideBuf[idx + 3] = mx / len
                    sideBuf[idx + 4] = my / len
                    sideBuf[idx + 5] = mz / len
                }
            }

            System.arraycopy(sideBuf, 0, data, sideOffsetVerts * stride, sideBuf.size)
        }

        updateSide(cache.left, state.leftBreastX, state.leftBreastY, 0)
        updateSide(cache.right, state.rightBreastX, state.rightBreastY, cache.left.count)
        mesh.commitVertices()
    }

    // -------------------------------------------------------------------------
    // 3. SCULPTURAL REALISTIC FEMININE HEAD & FACE (96x88, OUTWARD CCW!)
    //    theta runs BOTTOM-TO-TOP (PI -> 0) so d/dc=+X, d/dr=+Y => +Z OUTWARD!
    // -------------------------------------------------------------------------
    fun buildBlossomHeadMesh(
        buildIndices: (Int, Int) -> ShortArray,
        computeNormals: (FloatArray, Int, Int, Boolean, Boolean) -> Unit
    ): DynamicGlMesh {
        val cols = 96
        val rows = 88
        val mesh = DynamicGlMesh(cols * rows, (cols - 1) * (rows - 1) * 6)
        mesh.setIndices(buildIndices(cols, rows))

        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        for (r in 0 until rows) {
            // Run v from 0 (neck/chin bottom, theta = PI) UP to 1 (crown top, theta = 0) -> strictly +Y!
            val vRaw = 1f - (r.toFloat() / (rows - 1))
            val vCentered = vRaw * 2f - 1f
            val vCluster = 0.72f * vCentered + 0.28f * vCentered * vCentered * vCentered
            val theta = ((vCluster + 1f) * 0.5f).coerceIn(0.004f, 0.996f) * PI.toFloat()
            val sinT = sin(theta)
            val cosT = cos(theta)

            for (c in 0 until cols) {
                // Cluster horizontal resolution tightly across anterior face (phi near 0)
                val uRaw = (c.toFloat() / (cols - 1)) * 2f - 1f
                val uCluster = 0.58f * uRaw + 0.42f * uRaw * uRaw * uRaw
                val phi = uCluster * PI.toFloat()
                val sinP = sin(phi)
                val cosP = cos(phi)

                // Classical adult female cranial-facial envelope (crisp feminine jawline & soft rounded chin!)
                val rawY = cosT * 0.96f
                val frontFace = smoothStep(-0.12f, 0.48f, cosP)

                // Graceful zygomatic arch, subtle feminine jaw angle, and soft clean taper into chin (ZERO melted chin!)
                val lowerFaceT = smoothStep(0.02f, -0.88f, rawY)
                val jawAngle = 1f + 0.042f * exp(-((rawY + 0.46f) / 0.16f).let { it * it }) * smoothStep(0.32f, 0.80f, abs(sinP))
                val jawTaper = 1f - 0.36f * lowerFaceT * lowerFaceT * (0.48f + 0.52f * frontFace)
                val chinRound = 1f - 0.16f * smoothStep(-0.68f, -0.94f, rawY) * frontFace
                val templeHollow = 1f - 0.065f * exp(-((rawY - 0.24f) / 0.22f).let { it * it }) * smoothStep(0.36f, 0.86f, abs(sinP))
                val craniumBackFull = 1f + 0.08f * exp(-((rawY - 0.16f) / 0.48f).let { it * it }) * smoothStep(0.10f, 0.80f, -cosP)

                var lx = sinT * sinP * 0.695f * jawAngle * jawTaper * chinRound * templeHollow
                var ly = rawY
                var lz = sinT * cosP * 0.79f * craniumBackFull

                // Natural facial plane across forehead & crisp mandibular/chin projection (chin projects cleanly ahead of throat!)
                if (cosP > 0f) {
                    val foreheadPlane = -0.026f * smoothStep(0.28f, 0.72f, ly) * cosP * cosP
                    val lowerSlope = -0.046f * smoothStep(0.04f, -0.70f, ly) * cosP * cosP
                    val chinForwardSupport = 0.088f * exp(-((rawY + 0.79f) / 0.14f).let { it * it }) * cosP * cosP
                    lz += foreheadPlane + lowerSlope + chinForwardSupport
                }

                // Tuck STRICTLY underneath the jaw & skull base (theta > 0.895*PI) so chin & jawline remain 100% crisp!
                val submentalT = smoothStep(0.895f * PI.toFloat(), 0.992f * PI.toFloat(), theta)
                if (submentalT > 0.001f) {
                    val targetNeckX = sinP * 0.31f
                    val targetNeckY = -0.84f - 0.16f * smoothStep(0.92f * PI.toFloat(), 1.0f * PI.toFloat(), theta)
                    val targetNeckZ = -0.26f + cosP * 0.31f
                    lx = lx * (1f - submentalT) + targetNeckX * submentalT
                    ly = ly * (1f - submentalT) + targetNeckY * submentalT
                    lz = lz * (1f - submentalT) + targetNeckZ * submentalT
                }

                val ax = abs(lx)
                var dzFace = 0f
                var faceMucosa = 0f
                var faceWetness = 0.042f
                var faceAo = 1f
                var facePigment = 0.025f

                if (cosP > 0.14f && submentalT < 0.45f) {
                    val fW = smoothStep(0.14f, 0.54f, cosP) * (1f - submentalT)

                    // 1. Real Cranial & Facial Bony/Soft-Tissue Structure (malar cheekbones, subtle brow bone, glabella, crisp chin)
                    val zygomaCheek = 0.052f * exp(-(((ax - 0.36f) / 0.19f).let { it * it } + ((ly + 0.12f) / 0.16f).let { it * it }))
                    val buccalSoftHollow = -0.018f * exp(-(((ax - 0.35f) / 0.14f).let { it * it } + ((ly + 0.42f) / 0.16f).let { it * it }))
                    val chinForm = 0.056f * exp(-((ax / 0.165f).let { it * it } + ((ly + 0.78f) / 0.125f).let { it * it }))
                    val jawRimDef = 0.020f * exp(-(((ax - 0.34f) / 0.16f).let { it * it } + ((ly + 0.68f) / 0.10f).let { it * it }))
                    val browArchY = 0.168f + 0.024f * exp(-((ax - 0.285f) / 0.11f).let { it * it }) - 0.032f * smoothStep(0.28f, 0.43f, ax)
                    val browBone = 0.032f * exp(-(((ax - 0.24f) / 0.22f).let { it * it } + ((ly - browArchY) / 0.085f).let { it * it }))
                    val glabellaBridge = 0.016f * exp(-((ax / 0.10f).let { it * it } + ((ly - 0.14f) / 0.08f).let { it * it }))
                    val tearTrough = -0.014f * exp(-(((ax - 0.22f) / 0.12f).let { it * it } + ((ly + 0.075f) / 0.045f).let { it * it }))
                    val nasolabialSoft = 0.014f * exp(-(((ax - 0.22f) / 0.09f).let { it * it } + ((ly + 0.36f) / 0.13f).let { it * it }))
                    dzFace += (zygomaCheek + buccalSoftHollow + chinForm + jawRimDef + browBone + glabellaBridge + tearTrough + nasolabialSoft) * fW

                    // 2. Deeper-Set Natural Human Eye Sockets & Sculpted 3D Almond Eyelids (Rule of Fifths: ax = 0.246, ly = +0.022)
                    val exEye = (ax - 0.246f) / 0.118f
                    val eyEye = (ly - 0.022f) / 0.072f
                    val orbitR2 = exEye * exEye * 0.54f + eyEye * eyEye * 0.48f
                    val orbitHollow = -0.082f * exp(-orbitR2 * 1.48f)

                    // Calm natural resting almond aperture with gentle lateral lift and soft inner canthus
                    val eyeU = ((ax - 0.246f) / 0.094f).coerceIn(-1.25f, 1.25f)
                    val eyeSpan = (1f - eyeU * eyeU).coerceAtLeast(0f)
                    val upperLidY = 0.022f + (0.027f + 0.004f * eyeU) * sqrt(eyeSpan)
                    val lowerLidY = 0.022f - (0.018f - 0.002f * eyeU) * sqrt(eyeSpan)
                    val insideAperture = if (abs(eyeU) < 0.98f) {
                        smoothStep(lowerLidY - 0.005f, lowerLidY + 0.005f, ly) *
                            (1f - smoothStep(upperLidY - 0.005f, upperLidY + 0.005f, ly))
                    } else 0f

                    val apertureRecess = -0.106f * insideAperture
                    val upperLidFold = 0.032f * exp(-((ly - (upperLidY + 0.013f)) / 0.018f).let { it * it }) * smoothStep(1.08f, 0.66f, abs(eyeU))
                    val upperCrease = -0.015f * exp(-((ly - (upperLidY + 0.036f)) / 0.013f).let { it * it }) * smoothStep(1.02f, 0.58f, abs(eyeU))
                    val lowerLidMargin = 0.016f * exp(-((ly - (lowerLidY - 0.011f)) / 0.016f).let { it * it }) * smoothStep(1.04f, 0.66f, abs(eyeU))
                    dzFace += (orbitHollow + apertureRecess + upperLidFold + upperCrease + lowerLidMargin) * fW

                    // Natural fine hair-textured eyebrows & soft subtle lashline (NEVER painted cartoon lines!)
                    val lashLine = exp(-((ly - (upperLidY + 0.002f)) / 0.0075f).let { it * it }) * smoothStep(1.00f, 0.72f, abs(eyeU))
                    val lowerLash = exp(-((ly - (lowerLidY - 0.002f)) / 0.007f).let { it * it }) * smoothStep(0.88f, 0.52f, abs(eyeU)) * 0.16f
                    val browThickness = 0.0155f * (1f - 0.42f * smoothStep(0.24f, 0.41f, ax))
                    val browHairGrain = 0.72f + 0.28f * sin(ax * 190f + ly * 75f)
                    val browMask = exp(-((ly - browArchY) / browThickness).let { it * it }) *
                        smoothStep(0.105f, 0.165f, ax) * (1f - smoothStep(0.35f, 0.43f, ax)) * browHairGrain

                    // 3. Sculptural Realistic Nose (nasion dip -> slender straight dorsum -> dainty rounded tip -> soft alae & columella)
                    val nasionDip = -0.016f * exp(-((ax / 0.065f).let { it * it } + ((ly - 0.045f) / 0.048f).let { it * it }))
                    val bridgeW = 0.046f + 0.020f * smoothStep(0.03f, -0.24f, ly)
                    val bridgeSpan = smoothBell(ly, -0.255f, 0.045f, 0.06f)
                    val bridgeProfile = 0.054f + 0.104f * smoothStep(0.03f, -0.25f, ly)
                    val noseBridge = bridgeProfile * exp(-((ax / bridgeW).let { it * it })) * bridgeSpan
                    val noseTip = 0.122f * exp(-((ax / 0.064f).let { it * it } + ((ly + 0.255f) / 0.064f).let { it * it }))
                    val noseAla = 0.050f * exp(-(((ax - 0.084f) / 0.044f).let { it * it } + ((ly + 0.282f) / 0.042f).let { it * it }))
                    val alarCrease = -0.012f * exp(-(((ax - 0.120f) / 0.020f).let { it * it } + ((ly + 0.278f) / 0.048f).let { it * it }))
                    val nostrilCut = -0.030f * exp(-(((ax - 0.052f) / 0.022f).let { it * it } + ((ly + 0.308f) / 0.018f).let { it * it }))
                    val philtrum = -0.010f * exp(-((ax / 0.030f).let { it * it })) * smoothBell(ly, -0.43f, -0.32f, 0.030f)
                    dzFace += (nasionDip + noseBridge + noseTip + noseAla + alarCrease + nostrilCut + philtrum) * fW

                    // 4. Natural, Understated, Calm Closed Human Lips (ZERO puffy doll lips, ZERO shiny lipstick!)
                    val lipU = (ax / 0.168f).coerceIn(0f, 1.35f)
                    val lipSpan = smoothStep(1.04f, 0.58f, lipU)
                    val gentleCornerCurve = 0.009f * lipU * lipU
                    val cupidDip = -0.008f * exp(-((ax / 0.030f).let { it * it })) +
                        0.007f * exp(-(((ax - 0.046f) / 0.034f).let { it * it }))
                    val lipPartY = -0.495f + gentleCornerCurve
                    val upperLipY = -0.471f + cupidDip + gentleCornerCurve * 0.40f
                    val lowerLipY = -0.526f + gentleCornerCurve * 0.65f

                    val upperLipVol = 0.022f * exp(-((ly - upperLipY) / 0.022f).let { it * it }) * lipSpan
                    val lowerLipVol = 0.028f * exp(-((ly - lowerLipY) / 0.028f).let { it * it }) * smoothStep(0.94f, 0.45f, lipU)
                    val lipPartGroove = -0.011f * exp(-((ly - lipPartY) / 0.008f).let { it * it }) * smoothStep(1.00f, 0.56f, lipU)
                    val cornerDimple = -0.010f * exp(-(((ax - 0.172f) / 0.024f).let { it * it } + ((ly + 0.488f) / 0.024f).let { it * it }))
                    val mentolabialDip = -0.016f * exp(-((ax / 0.13f).let { it * it } + ((ly + 0.625f) / 0.048f).let { it * it }))
                    dzFace += (upperLipVol + lowerLipVol + lipPartGroove + cornerDimple + mentolabialDip) * fW

                    // Natural muted rose lip tint + ultra-subtle natural cheekbone & nose-bridge living warmth (matte-satin!)
                    val lipColorMask = ((exp(-((ly - upperLipY) / 0.023f).let { it * it }) +
                        exp(-((ly - lowerLipY) / 0.029f).let { it * it })) * lipSpan).coerceIn(0f, 1f)
                    val cheekWarmth = 0.042f * exp(-(((ax - 0.36f) / 0.18f).let { it * it } + ((ly + 0.12f) / 0.16f).let { it * it }))

                    faceMucosa = (lipColorMask * 0.22f + cheekWarmth).coerceIn(0f, 0.28f)
                    faceWetness = (0.036f + lipColorMask * 0.044f).coerceIn(0.032f, 0.088f)
                    val nostrilAo = exp(-(((ax - 0.052f) / 0.026f).let { it * it } + ((ly + 0.308f) / 0.020f).let { it * it })) * 0.44f
                    val lipLineAo = exp(-((ly - lipPartY) / 0.009f).let { it * it }) * lipSpan * 0.26f
                    val creaseAo = exp(-((ly - (upperLidY + 0.034f)) / 0.015f).let { it * it }) * smoothStep(1.00f, 0.56f, abs(eyeU)) * 0.22f
                    val orbitShadowAo = exp(-orbitR2 * 1.85f) * 0.10f
                    val browDarkAo = browMask * 0.34f
                    val lashDarkAo = (lashLine * 0.50f + lowerLash * 0.16f).coerceIn(0f, 0.54f)
                    faceAo = (1f - nostrilAo - lipLineAo - creaseAo - orbitShadowAo - browDarkAo - lashDarkAo).coerceIn(0.22f, 1f)
                    facePigment = (0.025f + browMask * 0.44f + lashLine * 0.56f + lipColorMask * 0.14f).coerceIn(0f, 0.78f)
                }

                // 5. Delicate Sculpted Ears on lateral temples
                val earRegion = smoothStep(0.72f, 0.96f, abs(sinP)) * smoothBell(ly, -0.24f, 0.12f, 0.08f) * (1f - submentalT)
                if (earRegion > 0.001f) {
                    val earLat = 0.060f * earRegion * exp(-((cosP + 0.16f) / 0.28f).let { it * it })
                    lx += (if (sinP >= 0f) 1f else -1f) * earLat
                    lz -= 0.018f * earRegion
                    faceAo *= (1f - 0.20f * earRegion * exp(-((cosP + 0.10f) / 0.18f).let { it * it }))
                }

                // Soft natural ambient occlusion strictly underneath the submental jaw plane
                if (submentalT > 0.001f) {
                    faceAo *= (1f - 0.24f * submentalT)
                }

                lz += dzFace

                val worldPt = headLocalToWorld(lx, ly, lz)
                val idx = (r * cols + c) * stride
                data[idx] = worldPt[0]
                data[idx + 1] = worldPt[1]
                data[idx + 2] = worldPt[2]
                data[idx + 6] = faceMucosa
                data[idx + 7] = faceWetness
                data[idx + 8] = faceAo
                data[idx + 9] = facePigment
            }
        }

        // Bottom-to-top v parameterization means tx x bx = (+X) x (+Y) = +Z OUTWARD!
        computeNormals(data, cols, rows, false, true)

        // Blend ONLY the tucked hidden neck-socket collar (theta > 0.93*PI) into SDF neck normals so chin stays 100% crisp!
        for (r in 0 until rows) {
            val vRaw = 1f - (r.toFloat() / (rows - 1))
            val vCentered = vRaw * 2f - 1f
            val vCluster = 0.72f * vCentered + 0.28f * vCentered * vCentered * vCentered
            val theta = ((vCluster + 1f) * 0.5f).coerceIn(0.004f, 0.996f) * PI.toFloat()
            val nBlend = smoothStep(0.93f * PI.toFloat(), 0.99f * PI.toFloat(), theta)
            if (nBlend > 0.001f) {
                for (c in 0 until cols) {
                    val idx = (r * cols + c) * stride
                    val sdfNorm = BlossomSdfBodyBuilder.evaluateBodyNormal(data[idx], data[idx + 1], data[idx + 2], eps = 0.018f)
                    val mx = data[idx + 3] * (1f - nBlend) + sdfNorm[0] * nBlend
                    val my = data[idx + 4] * (1f - nBlend) + sdfNorm[1] * nBlend
                    val mz = data[idx + 5] * (1f - nBlend) + sdfNorm[2] * nBlend
                    val len = sqrt(mx * mx + my * my + mz * mz).coerceAtLeast(1e-5f)
                    data[idx + 3] = mx / len
                    data[idx + 4] = my / len
                    data[idx + 5] = mz / len
                }
            }
        }

        mesh.commitVertices()
        return mesh
    }

    // -------------------------------------------------------------------------
    // 4. NATURAL DEEPER-SET SAPPHIRE-BLUE 3D EYES (SEATED CALMLY BEHIND EYELIDS)
    // -------------------------------------------------------------------------
    fun buildBlossomEyesMesh(): DynamicGlMesh {
        val cols = 28
        val rows = 20
        val vertsPerEye = cols * rows
        val trisPerEye = (cols - 1) * (rows - 1) * 6

        val mesh = DynamicGlMesh(vertsPerEye * 2, trisPerEye * 2)
        val indices = ShortArray(trisPerEye * 2)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        val eyeRadius = 0.098f
        val eyeCenterY = 0.024f
        val eyeCenterZ = 0.576f
        val sides = floatArrayOf(-1f, +1f)

        var outIdx = 0
        for ((eyeIndex, side) in sides.withIndex()) {
            val baseVert = eyeIndex * vertsPerEye
            for (r in 0 until rows - 1) {
                for (c in 0 until cols - 1) {
                    val i0 = (baseVert + r * cols + c).toShort()
                    val i1 = (baseVert + r * cols + c + 1).toShort()
                    val i2 = (baseVert + (r + 1) * cols + c).toShort()
                    val i3 = (baseVert + (r + 1) * cols + c + 1).toShort()
                    // Verified strictly CCW outward: d/dr(+X) x d/dc(+Y) = +Z
                    indices[outIdx++] = i0; indices[outIdx++] = i2; indices[outIdx++] = i1
                    indices[outIdx++] = i1; indices[outIdx++] = i2; indices[outIdx++] = i3
                }
            }

            val cx = side * 0.246f
            for (r in 0 until rows) {
                val v = r.toFloat() / (rows - 1)
                val alpha = v * (0.66f * PI.toFloat())
                val sinA = sin(alpha)
                val cosA = cos(alpha)

                for (c in 0 until cols) {
                    val u = c.toFloat() / (cols - 1)
                    val phi = u * (2f * PI.toFloat())
                    val cosP = cos(phi)
                    val sinP = sin(phi)

                    val lnx = sinA * cosP
                    val lny = sinA * sinP
                    val lnz = cosA

                    val lx = cx + lnx * eyeRadius
                    val ly = eyeCenterY + lny * eyeRadius
                    val lz = eyeCenterZ + lnz * eyeRadius

                    val wPt = headLocalToWorld(lx, ly, lz)
                    val wN = headNormalToWorld(lnx, lny, lnz)

                    val irisR = sinA
                    // Soft natural upper eyelid cast shadow across upper sclera & top of iris
                    val upperLidShadow = (1f - smoothStep(-0.08f, 0.44f, lny) * 0.52f).coerceIn(0.38f, 1f)

                    val idx = (baseVert + r * cols + c) * stride
                    data[idx] = wPt[0]; data[idx + 1] = wPt[1]; data[idx + 2] = wPt[2]
                    data[idx + 3] = wN[0]; data[idx + 4] = wN[1]; data[idx + 5] = wN[2]
                    data[idx + 6] = irisR
                    data[idx + 7] = phi / (2f * PI.toFloat())
                    data[idx + 8] = upperLidShadow
                    data[idx + 9] = lny * 0.5f + 0.5f
                }
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return mesh
    }

    // -------------------------------------------------------------------------
    // 5. SOFT CHIN-TO-COLLARBONE LAYERED DARK-BROWN / SOFT-BLACK BOB (CAP + 34 LOCKS)
    // -------------------------------------------------------------------------
    fun buildBlossomHairMesh(): DynamicGlMesh {
        val capCols = 52
        val capRows = 38
        val capVerts = capCols * capRows
        val capIndices = (capCols - 1) * (capRows - 1) * 6

        data class LockSpec(
            val p0: FloatArray,
            val p1: FloatArray,
            val p2: FloatArray,
            val halfW: Float,
            val thick: Float,
            val shadeBias: Float
        )

        val locks = listOf(
            // 8 Soft asymmetrical side-swept feathered bangs framing forehead & temples
            LockSpec(floatArrayOf(0.24f, 0.80f, 0.45f), floatArrayOf(-0.04f, 0.64f, 0.71f), floatArrayOf(-0.36f, 0.30f, 0.68f), 0.145f, 0.044f, 0.18f),
            LockSpec(floatArrayOf(0.16f, 0.78f, 0.51f), floatArrayOf(-0.14f, 0.56f, 0.72f), floatArrayOf(-0.48f, 0.20f, 0.58f), 0.135f, 0.040f, 0.12f),
            LockSpec(floatArrayOf(0.06f, 0.76f, 0.55f), floatArrayOf(-0.24f, 0.48f, 0.69f), floatArrayOf(-0.58f, 0.08f, 0.48f), 0.125f, 0.038f, 0.20f),
            LockSpec(floatArrayOf(0.30f, 0.82f, 0.39f), floatArrayOf(0.06f, 0.64f, 0.70f), floatArrayOf(-0.18f, 0.36f, 0.73f), 0.125f, 0.038f, 0.08f),
            LockSpec(floatArrayOf(0.20f, 0.88f, 0.33f), floatArrayOf(-0.18f, 0.72f, 0.65f), floatArrayOf(-0.56f, 0.30f, 0.50f), 0.155f, 0.046f, 0.24f),
            LockSpec(floatArrayOf(-0.04f, 0.84f, 0.45f), floatArrayOf(-0.40f, 0.54f, 0.60f), floatArrayOf(-0.70f, 0.04f, 0.36f), 0.135f, 0.040f, 0.14f),
            LockSpec(floatArrayOf(-0.18f, 0.78f, 0.46f), floatArrayOf(-0.56f, 0.36f, 0.48f), floatArrayOf(-0.76f, -0.18f, 0.26f), 0.125f, 0.038f, 0.16f),
            LockSpec(floatArrayOf(0.10f, 0.94f, 0.28f), floatArrayOf(-0.28f, 0.80f, 0.55f), floatArrayOf(-0.62f, 0.40f, 0.42f), 0.155f, 0.046f, 0.22f),

            // 10 Longer face-framing layered bob side locks sweeping past ears, cheekbones & chin (left & right)
            LockSpec(floatArrayOf(-0.34f, 0.78f, 0.32f), floatArrayOf(-0.76f, 0.16f, 0.32f), floatArrayOf(-0.72f, -0.84f, 0.16f), 0.145f, 0.042f, 0.16f),
            LockSpec(floatArrayOf(-0.42f, 0.72f, 0.16f), floatArrayOf(-0.82f, 0.02f, 0.14f), floatArrayOf(-0.80f, -1.04f, -0.02f), 0.155f, 0.044f, 0.12f),
            LockSpec(floatArrayOf(-0.38f, 0.76f, 0.24f), floatArrayOf(-0.78f, 0.06f, 0.24f), floatArrayOf(-0.68f, -0.92f, 0.08f), 0.130f, 0.038f, 0.22f),
            LockSpec(floatArrayOf(-0.26f, 0.86f, 0.12f), floatArrayOf(-0.78f, 0.24f, 0.16f), floatArrayOf(-0.84f, -0.78f, -0.10f), 0.150f, 0.044f, 0.18f),
            LockSpec(floatArrayOf(-0.46f, 0.60f, 0.02f), floatArrayOf(-0.84f, -0.14f, -0.04f), floatArrayOf(-0.86f, -1.16f, -0.22f), 0.160f, 0.046f, 0.10f),

            LockSpec(floatArrayOf(0.32f, 0.78f, 0.34f), floatArrayOf(0.75f, 0.18f, 0.34f), floatArrayOf(0.72f, -0.84f, 0.16f), 0.145f, 0.042f, 0.16f),
            LockSpec(floatArrayOf(0.40f, 0.72f, 0.18f), floatArrayOf(0.82f, 0.02f, 0.14f), floatArrayOf(0.80f, -1.04f, -0.02f), 0.155f, 0.044f, 0.12f),
            LockSpec(floatArrayOf(0.28f, 0.76f, 0.42f), floatArrayOf(0.68f, 0.24f, 0.44f), floatArrayOf(0.68f, -0.76f, 0.22f), 0.130f, 0.038f, 0.22f),
            LockSpec(floatArrayOf(0.26f, 0.88f, 0.16f), floatArrayOf(0.78f, 0.24f, 0.18f), floatArrayOf(0.84f, -0.78f, -0.08f), 0.150f, 0.044f, 0.18f),
            LockSpec(floatArrayOf(0.46f, 0.60f, 0.02f), floatArrayOf(0.84f, -0.14f, -0.04f), floatArrayOf(0.86f, -1.16f, -0.22f), 0.160f, 0.046f, 0.10f),

            // 10 Lush pillow-draping & shoulder-grazing lower bob locks fanning softly beside her neck & upper shoulders
            LockSpec(floatArrayOf(-0.56f, 0.26f, -0.18f), floatArrayOf(-0.90f, -0.46f, -0.26f), floatArrayOf(-1.02f, -1.36f, -0.34f), 0.175f, 0.046f, 0.14f),
            LockSpec(floatArrayOf(-0.48f, 0.14f, -0.34f), floatArrayOf(-0.84f, -0.62f, -0.42f), floatArrayOf(-1.08f, -1.52f, -0.46f), 0.185f, 0.046f, 0.08f),
            LockSpec(floatArrayOf(-0.36f, 0.02f, -0.48f), floatArrayOf(-0.72f, -0.74f, -0.54f), floatArrayOf(-0.96f, -1.62f, -0.54f), 0.180f, 0.044f, 0.12f),
            LockSpec(floatArrayOf(-0.54f, 0.42f, -0.08f), floatArrayOf(-0.88f, -0.26f, -0.18f), floatArrayOf(-0.98f, -1.24f, -0.28f), 0.165f, 0.044f, 0.20f),
            LockSpec(floatArrayOf(-0.62f, 0.04f, 0.04f), floatArrayOf(-0.80f, -0.58f, 0.04f), floatArrayOf(-0.84f, -1.32f, -0.12f), 0.145f, 0.040f, 0.18f),

            LockSpec(floatArrayOf(0.56f, 0.26f, -0.18f), floatArrayOf(0.90f, -0.46f, -0.26f), floatArrayOf(1.02f, -1.36f, -0.34f), 0.175f, 0.046f, 0.14f),
            LockSpec(floatArrayOf(0.48f, 0.14f, -0.34f), floatArrayOf(0.84f, -0.62f, -0.42f), floatArrayOf(1.08f, -1.52f, -0.46f), 0.185f, 0.046f, 0.08f),
            LockSpec(floatArrayOf(0.36f, 0.02f, -0.48f), floatArrayOf(0.72f, -0.74f, -0.54f), floatArrayOf(0.96f, -1.62f, -0.54f), 0.180f, 0.044f, 0.12f),
            LockSpec(floatArrayOf(0.54f, 0.42f, -0.08f), floatArrayOf(0.88f, -0.26f, -0.18f), floatArrayOf(0.98f, -1.24f, -0.28f), 0.165f, 0.044f, 0.20f),
            LockSpec(floatArrayOf(0.62f, 0.04f, 0.04f), floatArrayOf(0.80f, -0.58f, 0.04f), floatArrayOf(0.84f, -1.32f, -0.12f), 0.145f, 0.040f, 0.18f),

            // 6 Layered dimensional crown volume locks (gives natural glossy silhouette from every angle)
            LockSpec(floatArrayOf(0.14f, 1.01f, 0.08f), floatArrayOf(-0.22f, 0.92f, 0.40f), floatArrayOf(-0.58f, 0.54f, 0.44f), 0.165f, 0.050f, 0.24f),
            LockSpec(floatArrayOf(0.24f, 0.99f, 0.02f), floatArrayOf(0.56f, 0.78f, 0.24f), floatArrayOf(0.78f, 0.30f, 0.20f), 0.160f, 0.048f, 0.18f),
            LockSpec(floatArrayOf(-0.08f, 1.01f, -0.08f), floatArrayOf(-0.52f, 0.76f, 0.16f), floatArrayOf(-0.78f, 0.20f, 0.10f), 0.160f, 0.048f, 0.14f),
            LockSpec(floatArrayOf(0.06f, 1.04f, -0.14f), floatArrayOf(-0.36f, 0.90f, -0.04f), floatArrayOf(-0.72f, 0.40f, -0.12f), 0.165f, 0.048f, 0.20f),
            LockSpec(floatArrayOf(0.12f, 1.02f, -0.16f), floatArrayOf(0.48f, 0.84f, -0.04f), floatArrayOf(0.76f, 0.34f, -0.12f), 0.160f, 0.046f, 0.16f),
            LockSpec(floatArrayOf(-0.02f, 1.04f, 0.14f), floatArrayOf(0.28f, 0.94f, 0.38f), floatArrayOf(0.62f, 0.56f, 0.42f), 0.155f, 0.046f, 0.22f)
        )

        val lockCols = 8
        val lockRows = 14
        val vertsPerLock = lockCols * lockRows
        val trisPerLock = (lockCols - 1) * (lockRows - 1) * 6

        val totalVerts = capVerts + locks.size * vertsPerLock
        val totalIndices = capIndices + locks.size * trisPerLock
        val mesh = DynamicGlMesh(totalVerts, totalIndices)
        val indices = ShortArray(totalIndices)
        val data = mesh.vertexData
        val stride = DynamicGlMesh.FLOATS_PER_VERTEX

        var idxOut = 0
        for (r in 0 until capRows - 1) {
            for (c in 0 until capCols - 1) {
                val i0 = (r * capCols + c).toShort()
                val i1 = (r * capCols + c + 1).toShort()
                val i2 = ((r + 1) * capCols + c).toShort()
                val i3 = ((r + 1) * capCols + c + 1).toShort()
                indices[idxOut++] = i0; indices[idxOut++] = i2; indices[idxOut++] = i1
                indices[idxOut++] = i1; indices[idxOut++] = i2; indices[idxOut++] = i3
            }
        }

        // Part A: Layered Bob Crown & Pillow-Draping Nape Volume Cap
        for (r in 0 until capRows) {
            val v = r.toFloat() / (capRows - 1)
            for (c in 0 until capCols) {
                val u = (c.toFloat() / (capCols - 1)) * 2f - 1f
                val phi = u * PI.toFloat()
                val sinP = sin(phi)
                val cosP = cos(phi)

                val frontMask = smoothStep(-0.15f, 0.65f, cosP)
                val backMask = smoothStep(0.10f, 0.80f, -cosP)
                val sideMask = smoothStep(0.32f, 0.88f, abs(sinP))
                // Soft asymmetrical side-part curve across forehead so front bangs look feathered instead of straight-across blunt
                val partLift = 0.075f * smoothStep(-0.18f, 0.58f, sinP) * frontMask
                val maxTheta = 0.62f * PI.toFloat() -
                    (0.285f + partLift) * PI.toFloat() * frontMask +
                    0.24f * PI.toFloat() * backMask +
                    0.16f * PI.toFloat() * sideMask * (1f - frontMask)

                val theta = (0.008f + v * (maxTheta / PI.toFloat())).coerceAtMost(0.86f) * PI.toFloat()
                val sinT = sin(theta)
                val cosT = cos(theta)

                val crownVolume = 0.135f * (1f - smoothStep(0.28f, 0.92f, v)) + 0.055f * smoothStep(0.20f, 0.85f, v)
                val edgeTaper = 1f - 0.66f * smoothStep(0.84f, 1.0f, v)
                val strandRib = 0.014f * sin(phi * 18f + v * 5.2f) + 0.008f * cos(phi * 31f - v * 7.5f)
                val loft = (crownVolume * edgeTaper + strandRib * (1f - smoothStep(0.86f, 1.0f, v))).coerceAtLeast(0.014f)

                val craniumBack = 1f + 0.10f * backMask * sinT
                // Gentle lateral fan-out at lower nape so longer back hair rests softly onto the headrest pillow
                val pillowFan = 0.18f * smoothStep(0.58f, 0.96f, v) * (backMask * 0.7f + sideMask * 0.5f) * (1f - frontMask)
                val lx = sinT * sinP * (0.71f + loft + pillowFan)
                val ly = cosT * (0.98f + loft * 0.95f) - 0.24f * smoothStep(0.65f, 1.0f, v) * (backMask + sideMask * 0.6f) * (1f - frontMask)
                val lz = sinT * cosP * (0.80f * craniumBack + loft)

                val wPt = headLocalToWorld(lx, ly, lz)
                val lnx = sinT * sinP + 0.12f * cos(phi * 18f + v * 5.2f) * cosP
                val lny = cosT
                val lnz = sinT * cosP - 0.12f * cos(phi * 18f + v * 5.2f) * sinP
                val wN = headNormalToWorld(lnx, lny, lnz)

                val strandShade = 0.5f + 0.5f * sin(phi * 24f + v * 6.0f)
                val ao = (0.76f + 0.24f * (1f - smoothStep(0.62f, 1.0f, v)) * (0.75f + 0.25f * strandShade)).coerceIn(0.42f, 1f)

                val idx = (r * capCols + c) * stride
                data[idx] = wPt[0]; data[idx + 1] = wPt[1]; data[idx + 2] = wPt[2]
                data[idx + 3] = wN[0]; data[idx + 4] = wN[1]; data[idx + 5] = wN[2]
                data[idx + 6] = v
                data[idx + 7] = strandShade
                data[idx + 8] = ao
                data[idx + 9] = 0.08f
            }
        }

        // Part B: 24 Layered Curved Pixie Hair Locks (verified outward CCW winding!)
        for ((lockIdx, lock) in locks.withIndex()) {
            val baseVert = capVerts + lockIdx * vertsPerLock
            for (r in 0 until lockRows - 1) {
                for (c in 0 until lockCols - 1) {
                    val i0 = (baseVert + r * lockCols + c).toShort()
                    val i1 = (baseVert + r * lockCols + c + 1).toShort()
                    val i2 = (baseVert + (r + 1) * lockCols + c).toShort()
                    val i3 = (baseVert + (r + 1) * lockCols + c + 1).toShort()
                    // B = T x O => d/dc(B) x d/dr(T) = B x T = +O (OUTWARD CCW!)
                    indices[idxOut++] = i0; indices[idxOut++] = i1; indices[idxOut++] = i2
                    indices[idxOut++] = i1; indices[idxOut++] = i3; indices[idxOut++] = i2
                }
            }

            for (r in 0 until lockRows) {
                val t = r.toFloat() / (lockRows - 1)
                val omt = 1f - t
                val cx = omt * omt * lock.p0[0] + 2f * omt * t * lock.p1[0] + t * t * lock.p2[0]
                val cy = omt * omt * lock.p0[1] + 2f * omt * t * lock.p1[1] + t * t * lock.p2[1]
                val cz = omt * omt * lock.p0[2] + 2f * omt * t * lock.p1[2] + t * t * lock.p2[2]

                var tx = 2f * omt * (lock.p1[0] - lock.p0[0]) + 2f * t * (lock.p2[0] - lock.p1[0])
                var ty = 2f * omt * (lock.p1[1] - lock.p0[1]) + 2f * t * (lock.p2[1] - lock.p1[1])
                var tz = 2f * omt * (lock.p1[2] - lock.p0[2]) + 2f * t * (lock.p2[2] - lock.p1[2])
                val tLen = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(1e-5f)
                tx /= tLen; ty /= tLen; tz /= tLen

                val drapeW = smoothStep(-0.15f, -0.85f, cy)
                var ox = cx * (1f - 0.45f * drapeW)
                var oy = (cy - 0.12f) * (1f - drapeW) + 0.06f * drapeW
                var oz = cz * (1f - drapeW) + 0.92f * drapeW
                val oDotT = ox * tx + oy * ty + oz * tz
                ox -= tx * oDotT; oy -= ty * oDotT; oz -= tz * oDotT
                val oLen = sqrt(ox * ox + oy * oy + oz * oz).coerceAtLeast(1e-5f)
                ox /= oLen; oy /= oLen; oz /= oLen

                val bx = ty * oz - tz * oy
                val by = tz * ox - tx * oz
                val bz = tx * oy - ty * ox

                val widthEnv = sin(t * PI.toFloat()).let { s ->
                    (0.60f * (1f - t) + 0.66f * s).coerceIn(0.04f, 1.0f) * (1f - smoothStep(0.72f, 1.0f, t) * 0.90f)
                }
                val curW = lock.halfW * widthEnv
                val curThick = lock.thick * (0.48f + 0.52f * sin(t * PI.toFloat()))

                for (c in 0 until lockCols) {
                    val u = (c.toFloat() / (lockCols - 1)) * 2f - 1f
                    val arch = cos(u * (PI.toFloat() * 0.5f))
                    val lx = cx + bx * (u * curW) + ox * (arch * curThick)
                    val ly = cy + by * (u * curW) + oy * (arch * curThick)
                    val lz = cz + bz * (u * curW) + oz * (arch * curThick)

                    val lnx = ox * (0.85f * arch + 0.15f) + bx * (u * 0.52f)
                    val lny = oy * (0.85f * arch + 0.15f) + by * (u * 0.52f)
                    val lnz = oz * (0.85f * arch + 0.15f) + bz * (u * 0.52f)

                    val wPt = headLocalToWorld(lx, ly, lz)
                    val minHairZ = BlossomBodyBuilder.mattressSurfaceZ(wPt[1]) + 0.03f
                    if (wPt[2] < minHairZ) wPt[2] = minHairZ
                    val wN = headNormalToWorld(lnx, lny, lnz)

                    val idx = (baseVert + r * lockCols + c) * stride
                    data[idx] = wPt[0]; data[idx + 1] = wPt[1]; data[idx + 2] = wPt[2]
                    data[idx + 3] = wN[0]; data[idx + 4] = wN[1]; data[idx + 5] = wN[2]
                    data[idx + 6] = t
                    data[idx + 7] = 0.5f + 0.5f * cos(u * PI.toFloat())
                    data[idx + 8] = (0.72f + 0.28f * arch).coerceIn(0.55f, 1f)
                    data[idx + 9] = lock.shadeBias
                }
            }
        }

        mesh.setIndices(indices)
        mesh.commitVertices()
        return mesh
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun smoothBell(x: Float, low: Float, high: Float, feather: Float): Float {
        return smoothStep(low - feather, low + feather, x) * (1f - smoothStep(high - feather, high + feather, x))
    }
}
