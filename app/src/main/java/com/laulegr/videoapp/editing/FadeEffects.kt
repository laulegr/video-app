package com.laulegr.videoapp.editing

import androidx.media3.effect.RgbMatrix

/**
 * Fades RGB towards black at the start and/or end of a clip's OWN timeline
 * (presentationTimeUs is relative to that clip's trimmed+speed-adjusted stream,
 * starting at 0 - see Media3's RgbMatrix contract). Used to fake a "crossfade"
 * transition between clips: fade the outgoing clip to black, fade the incoming
 * clip in from black. A true cross-dissolve would need multi-sequence video
 * compositing, which is still experimental in Media3 - this is the robust,
 * well-supported alternative.
 */
class FadeToBlackMatrix(
    private val clipDurationUs: Long,
    private val fadeInUs: Long,
    private val fadeOutUs: Long,
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val fadeInFactor = if (fadeInUs > 0) {
            (presentationTimeUs.toFloat() / fadeInUs).coerceIn(0f, 1f)
        } else 1f

        val timeUntilEnd = clipDurationUs - presentationTimeUs
        val fadeOutFactor = if (fadeOutUs > 0) {
            (timeUntilEnd.toFloat() / fadeOutUs).coerceIn(0f, 1f)
        } else 1f

        val scale = minOf(fadeInFactor, fadeOutFactor)
        return floatArrayOf(
            scale, 0f, 0f, 0f,
            0f, scale, 0f, 0f,
            0f, 0f, scale, 0f,
            0f, 0f, 0f, 1f,
        )
    }
}

object FadeEffects {
    const val FADE_DURATION_US = 400_000L

    fun forClip(
        clipEffectiveDurationUs: Long,
        isFirstClip: Boolean,
        isLastClip: Boolean,
        transition: com.laulegr.videoapp.model.TransitionType,
    ): RgbMatrix? {
        if (transition != com.laulegr.videoapp.model.TransitionType.FADE_TO_BLACK) return null

        val fadeIn = if (isFirstClip) 0L else FADE_DURATION_US.coerceAtMost(clipEffectiveDurationUs / 2)
        val fadeOut = if (isLastClip) 0L else FADE_DURATION_US.coerceAtMost(clipEffectiveDurationUs / 2)
        if (fadeIn == 0L && fadeOut == 0L) return null

        return FadeToBlackMatrix(clipEffectiveDurationUs, fadeIn, fadeOut)
    }
}
