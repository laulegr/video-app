package com.laulegr.videoapp.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

/**
 * One clip in the timeline. [trimStartMs]/[trimEndMs] mark the range of the
 * ORIGINAL file that is kept on export; [durationMs] is the original, untrimmed length.
 */
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val durationMs: Long,
    val thumbnail: Bitmap? = null,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

enum class TransitionType(val label: String, val available: Boolean) {
    CUT("Schnitt", available = true),
    CROSSFADE("Überblendung (bald)", available = false),
}
