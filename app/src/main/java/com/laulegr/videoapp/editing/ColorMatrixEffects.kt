package com.laulegr.videoapp.editing

import androidx.media3.effect.RgbMatrix

/**
 * Plain luminance matrix (Rec. 601 weights), stored COLUMN-MAJOR as Media3's
 * GL pipeline expects (column j = how much output channels r,g,b,a receive
 * from input channel j). Getting this transposed is an easy mistake - a
 * row-major grayscale matrix read as column-major computes
 * out_r=0.299*(r+g+b), out_g=0.587*(r+g+b), out_b=0.114*(r+g+b) instead of
 * three equal channels, which rendered as a strong green/yellow false-color
 * cast rather than gray during testing.
 */
object GrayscaleMatrix : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = floatArrayOf(
        0.299f, 0.299f, 0.299f, 0f,
        0.587f, 0.587f, 0.587f, 0f,
        0.114f, 0.114f, 0.114f, 0f,
        0f, 0f, 0f, 1f,
    )
}

/** Classic sepia tone matrix (standard coefficients), column-major - see [GrayscaleMatrix]. */
object SepiaMatrix : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = floatArrayOf(
        0.393f, 0.349f, 0.272f, 0f,
        0.769f, 0.686f, 0.534f, 0f,
        0.189f, 0.168f, 0.131f, 0f,
        0f, 0f, 0f, 1f,
    )
}
