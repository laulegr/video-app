package com.laulegr.videoapp.editing

import android.text.SpannableString
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay

/** Builds a lower-third caption overlay for a clip, or null if it has no caption. */
fun buildCaptionOverlay(captionText: String?): OverlayEffect? {
    val text = captionText?.trim()
    if (text.isNullOrEmpty()) return null

    val settings = StaticOverlaySettings.Builder()
        .setBackgroundFrameAnchor(0f, -0.75f)
        .setOverlayFrameAnchor(0f, 0f)
        .build()

    val overlay = TextOverlay.createStaticTextOverlay(SpannableString(text), settings)
    return OverlayEffect(listOf(overlay))
}
