package com.laulegr.videoapp.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

/**
 * One clip in the timeline. [trimStartMs]/[trimEndMs] mark the range of the
 * ORIGINAL file that is kept on export; [durationMs] is the original, untrimmed length.
 * [speed] is applied on export (0.25x-3x); [effectiveDurationMs] is the resulting
 * length in the final timeline after trim + speed, used to time transitions.
 * [transitionOut] is the transition at the cut after this clip (ignored on the last clip).
 */
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val durationMs: Long,
    val thumbnail: Bitmap? = null,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val speed: Float = 1f,
    val captionText: String? = null,
    val transitionOut: Transition = Transition.NONE,
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    val effectiveDurationMs: Long
        get() = (trimmedDurationMs / speed).toLong()
}

val SPEED_STEPS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f)
