package com.laulegr.videoapp.model

/**
 * Look presets applied to the whole export. Backed by Media3 effects in
 * [com.laulegr.videoapp.editing.FilterEffects]. Grayscale/sepia use our own
 * column-major [com.laulegr.videoapp.editing.GrayscaleMatrix]/
 * [com.laulegr.videoapp.editing.SepiaMatrix] - see the HDR-mode comment in
 * VideoExporter for the on-device color bugs these presets flushed out and
 * how they were root-caused.
 */
enum class FilterPreset(val label: String) {
    NONE("Original"),
    VIBRANT("Vibrant"),
    NOIR("Noir"),
    SEPIA("Sepia"),
    WARM("Warm"),
    COOL("Cool"),
    MOODY("Moody"),
    NEON("Neon"),
    DREAMY("Dreamy"),
    VINTAGE("Vintage"),
}
