package com.laulegr.videoapp.editing

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Scales 16-bit PCM sample amplitude by [gain] (0f = silent, 1f = unchanged).
 * Used to set the background-music volume relative to the clips' own audio.
 */
class GainAudioProcessor(private val gain: Float) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        return if (gain != 1f && inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            inputAudioFormat
        } else {
            AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        val shortsIn = inputBuffer.asShortBuffer()
        val shortsOut = outputBuffer.asShortBuffer()
        while (shortsIn.hasRemaining()) {
            val scaled = (shortsIn.get() * gain).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            shortsOut.put(scaled.toShort())
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.position(remaining)
        outputBuffer.flip()
    }
}
