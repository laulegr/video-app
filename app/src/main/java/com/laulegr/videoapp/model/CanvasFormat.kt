package com.laulegr.videoapp.model

/**
 * Output frame format. Every clip is center-cropped to fill it, so mixed
 * portrait/landscape clips end up as one consistent video. [ORIGINAL] keeps
 * the source resolution untouched.
 */
enum class CanvasFormat(val label: String, val width: Int, val height: Int) {
    REEL("9:16 Reel", 1080, 1920),
    PORTRAIT("4:5 Post", 1080, 1350),
    SQUARE("1:1", 1080, 1080),
    ORIGINAL("Original", 0, 0),
}
