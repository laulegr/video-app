package com.laulegr.videoapp.editing

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.SpeedChangeEffect

/** Video + audio side of a constant speed change, kept in sync so AV stays aligned. */
data class SpeedEffects(
    val videoEffect: SpeedChangeEffect?,
    val audioProcessor: AudioProcessor?,
)

fun buildSpeedEffects(speed: Float): SpeedEffects {
    if (speed == 1f) return SpeedEffects(videoEffect = null, audioProcessor = null)
    return SpeedEffects(
        videoEffect = SpeedChangeEffect(speed),
        audioProcessor = SonicAudioProcessor().apply { setSpeed(speed) },
    )
}
