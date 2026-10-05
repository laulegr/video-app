package com.laulegr.videoapp.editing

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.SpeedChangeEffect

/**
 * Video + audio side of a constant speed change, kept in sync so AV stays aligned.
 *
 * Deliberately not EditedMediaItem.Builder.setSpeed: tested on-device with two
 * 1.2s trims at 1.25x, setSpeed dropped extra frames at each clip's end AND
 * started the next clip's video right after them instead of at its computed
 * offset - picture ran ~130ms ahead of the audio from clip 2 on, growing with
 * every clip. These effects keep every clip's video starting exactly where
 * its audio does.
 */
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
