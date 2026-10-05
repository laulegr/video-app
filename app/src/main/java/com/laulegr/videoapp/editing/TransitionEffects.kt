package com.laulegr.videoapp.editing

import android.graphics.Matrix
import androidx.media3.common.C
import androidx.media3.common.util.Size
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix
import com.laulegr.videoapp.model.Transition
import com.laulegr.videoapp.model.TransitionType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Timing of the (up to) two transitions touching one clip, in that clip's own
 * output time (after trim + speed): the incoming transition plays over the
 * first [inHalfUs], the outgoing one over the last [outHalfUs].
 */
class ClipTransitionWindows(
    private val clipDurationUs: Long,
    private val inType: TransitionType,
    private val inHalfUs: Long,
    private val outType: TransitionType,
    private val outHalfUs: Long,
) {
    val isActive: Boolean
        get() = (inType != TransitionType.NONE && inHalfUs > 0) ||
            (outType != TransitionType.NONE && outHalfUs > 0)

    /**
     * Which transition is running at [localUs], and how strongly: intensity 1 is
     * the peak right at the cut, 0 is back to the untouched clip.
     */
    fun phase(localUs: Long): Phase? {
        if (inType != TransitionType.NONE && inHalfUs > 0 && localUs < inHalfUs) {
            val p = (localUs.toFloat() / inHalfUs).coerceIn(0f, 1f)
            return Phase(inType, ease(inType, 1f - p), incoming = true)
        }
        val outStart = clipDurationUs - outHalfUs
        if (outType != TransitionType.NONE && outHalfUs > 0 && localUs > outStart) {
            val q = ((localUs - outStart).toFloat() / outHalfUs).coerceIn(0f, 1f)
            return Phase(outType, ease(outType, q), incoming = false)
        }
        return null
    }

    // x runs 0 -> 1 towards the cut on both sides, so motion is fastest at the
    // cut itself: accelerate into it, decelerate out of it.
    private fun ease(type: TransitionType, x: Float): Float = when (type) {
        TransitionType.FADE_BLACK, TransitionType.SHAKE, TransitionType.GLITCH -> x
        else -> x * x
    }

    companion object {
        fun forClip(clipDurationUs: Long, incoming: Transition?, outgoing: Transition?): ClipTransitionWindows {
            fun halfUs(t: Transition?): Long {
                if (t == null || t.type == TransitionType.NONE) return 0L
                return (t.durationMs * 1_000L / 2).coerceAtMost(clipDurationUs / 2)
            }
            return ClipTransitionWindows(
                clipDurationUs = clipDurationUs,
                inType = incoming?.type ?: TransitionType.NONE,
                inHalfUs = halfUs(incoming),
                outType = outgoing?.type ?: TransitionType.NONE,
                outHalfUs = halfUs(outgoing),
            )
        }
    }
}

data class Phase(val type: TransitionType, val intensity: Float, val incoming: Boolean)

/**
 * Media3 hands effects composition-wide timestamps (each clip's frames carry the
 * offset of everything before it), not per-clip ones. Effects are created per
 * clip and only ever see that clip's frames, in order, so the first timestamp
 * seen is where this clip starts.
 */
private class LocalClock {
    private var startUs = C.TIME_UNSET
    fun localUs(presentationTimeUs: Long): Long {
        if (startUs == C.TIME_UNSET) startUs = presentationTimeUs
        return presentationTimeUs - startUs
    }
}

/** Zoom / spin / swipe / shake / glitch-jitter, applied in normalized device coordinates. */
class TransitionGeometry(private val windows: ClipTransitionWindows) : MatrixTransformation {
    private val clock = LocalClock()
    private var aspect = 9f / 16f

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        aspect = inputWidth.toFloat() / inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val phase = windows.phase(clock.localUs(presentationTimeUs)) ?: return Matrix()
        val k = phase.intensity
        val sign = if (phase.incoming) 1f else -1f
        return when (phase.type) {
            TransitionType.ZOOM_IN -> transform(scale = 1f + 0.8f * k)
            TransitionType.ZOOM_OUT ->
                if (phase.incoming) transform(scale = 1f + 0.6f * k) else transform(scale = 1f - 0.45f * k)
            TransitionType.SPIN -> {
                val degrees = -sign * 90f * k
                transform(scale = coverScale(degrees), rotationDegrees = degrees)
            }
            TransitionType.SWIPE_LEFT -> swipe(tx = sign * SWIPE_DISTANCE * k)
            TransitionType.SWIPE_RIGHT -> swipe(tx = -sign * SWIPE_DISTANCE * k)
            TransitionType.SWIPE_UP -> swipe(ty = -sign * SWIPE_DISTANCE * k)
            TransitionType.SHAKE -> {
                val rnd = Random(presentationTimeUs / FRAME_BUCKET_US)
                val amplitude = 0.08f * k
                transform(
                    scale = 1f + 2.5f * amplitude,
                    rotationDegrees = 3f * k * rnd.nextSigned(),
                    tx = amplitude * rnd.nextSigned(),
                    ty = amplitude * rnd.nextSigned(),
                )
            }
            TransitionType.GLITCH -> {
                // Blocky horizontal jumps, changing every other frame.
                val rnd = Random(presentationTimeUs / (2 * FRAME_BUCKET_US))
                val tx = 0.07f * k * rnd.nextSigned()
                transform(scale = 1f + abs(tx) + 0.01f, tx = tx)
            }
            TransitionType.NONE, TransitionType.FADE_BLACK, TransitionType.FLASH_WHITE -> Matrix()
        }
    }

    // Zoomed just enough that the shifted frame still covers the whole picture.
    private fun swipe(tx: Float = 0f, ty: Float = 0f) =
        transform(scale = 1f + abs(tx) + abs(ty), tx = tx, ty = ty)

    // Smallest zoom at which the rotated frame still covers the canvas (no black corners).
    private fun coverScale(degrees: Float): Float {
        val rad = Math.toRadians(degrees.toDouble())
        return (abs(cos(rad)) + max(aspect, 1f / aspect) * abs(sin(rad))).toFloat()
    }

    private fun transform(
        scale: Float = 1f,
        rotationDegrees: Float = 0f,
        tx: Float = 0f,
        ty: Float = 0f,
    ): Matrix = Matrix().apply {
        if (rotationDegrees != 0f) {
            // NDC squashes x and y to the same [-1, 1] range; rotate in real
            // pixel proportions so the picture doesn't shear.
            postScale(aspect, 1f)
            postRotate(rotationDegrees)
            postScale(1f / aspect, 1f)
        }
        postScale(scale, scale)
        postTranslate(tx, ty)
    }

    private companion object {
        const val SWIPE_DISTANCE = 0.6f
        const val FRAME_BUCKET_US = 33_333L
    }
}

/** Dip to black / flash to white / glitch color flicker. */
class TransitionColor(private val windows: ClipTransitionWindows) : RgbMatrix {
    private val clock = LocalClock()

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val phase = windows.phase(clock.localUs(presentationTimeUs)) ?: return IDENTITY
        val k = phase.intensity
        return when (phase.type) {
            TransitionType.FADE_BLACK -> scaleAndLift(scale = 1f - k, lift = 0f)
            TransitionType.FLASH_WHITE -> scaleAndLift(scale = 1f - k, lift = k)
            // The shrinking outgoing clip also dims, so it reads as falling away into black.
            TransitionType.ZOOM_OUT -> if (phase.incoming) IDENTITY else scaleAndLift(1f - 0.7f * k, 0f)
            TransitionType.GLITCH -> glitch(k, presentationTimeUs)
            else -> IDENTITY
        }
    }

    private fun glitch(k: Float, presentationTimeUs: Long): FloatArray {
        val rnd = Random(presentationTimeUs / 50_000L)
        if (rnd.nextFloat() > 0.25f + 0.75f * k) return IDENTITY
        return when (rnd.nextInt(3)) {
            0 -> colorMatrix( // magenta push
                floatArrayOf(1.3f, 0f, 0f, 0f, 0.6f, 0f, 0f, 0f, 1.3f),
                lift = floatArrayOf(0.08f, 0f, 0.08f),
            )
            1 -> colorMatrix( // cyan push
                floatArrayOf(0.5f, 0f, 0f, 0f, 1.2f, 0f, 0f, 0f, 1.3f),
                lift = floatArrayOf(0f, 0.05f, 0.08f),
            )
            else -> colorMatrix( // channel swap: r<-g, g<-b, b<-r
                floatArrayOf(0f, 1f, 0f, 0f, 0f, 1f, 1f, 0f, 0f),
                lift = floatArrayOf(0f, 0f, 0f),
            )
        }
    }

    private companion object {
        val IDENTITY = colorMatrix(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f), floatArrayOf(0f, 0f, 0f))

        fun scaleAndLift(scale: Float, lift: Float) = colorMatrix(
            floatArrayOf(scale, 0f, 0f, 0f, scale, 0f, 0f, 0f, scale),
            floatArrayOf(lift, lift, lift),
        )

        /**
         * [rows] is a plain row-major 3x3 (out_r = rows[0..2] · (r,g,b), ...) plus a
         * per-channel [lift]; Media3 wants a 4x4 column-major array with the lift
         * in the last column (indices 12-14, as in its own Brightness effect).
         */
        fun colorMatrix(rows: FloatArray, lift: FloatArray) = floatArrayOf(
            rows[0], rows[3], rows[6], 0f,
            rows[1], rows[4], rows[7], 0f,
            rows[2], rows[5], rows[8], 0f,
            lift[0], lift[1], lift[2], 1f,
        )
    }
}

private fun Random.nextSigned(): Float = nextFloat() * 2f - 1f
