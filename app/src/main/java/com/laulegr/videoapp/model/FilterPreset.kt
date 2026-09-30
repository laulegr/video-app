package com.laulegr.videoapp.model

/**
 * Look presets applied to the whole export. Backed by Media3 effects in
 * [com.laulegr.videoapp.editing.FilterEffects].
 */
enum class FilterPreset(val label: String) {
    NONE("Original"),
    VIBRANT("Vibrant"),
    MONO("Schwarz-Weiß"),
    WARM("Warm"),
    MOODY("Moody"),
}
