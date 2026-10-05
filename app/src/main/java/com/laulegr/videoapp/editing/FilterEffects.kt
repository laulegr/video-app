package com.laulegr.videoapp.editing

import androidx.media3.common.Effect
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import com.laulegr.videoapp.model.FilterPreset

/**
 * Maps a [FilterPreset] to the Media3 GL effects applied at export time.
 * Kept separate from [VideoExporter] so new looks can be added here only.
 */
object FilterEffects {

    fun effectsFor(preset: FilterPreset): List<Effect> = when (preset) {
        FilterPreset.NONE -> emptyList()

        FilterPreset.VIBRANT -> listOf(
            Contrast(0.15f),
            HslAdjustment.Builder().adjustSaturation(0.35f).build(),
        )

        FilterPreset.NOIR -> listOf(
            GrayscaleMatrix,
            Contrast(0.3f),
        )

        FilterPreset.SEPIA -> listOf(
            SepiaMatrix,
            Contrast(0.05f),
        )

        FilterPreset.WARM -> listOf(
            HslAdjustment.Builder().adjustHue(-8f).adjustSaturation(0.15f).build(),
            Brightness(0.05f),
        )

        FilterPreset.COOL -> listOf(
            HslAdjustment.Builder().adjustHue(14f).adjustSaturation(-0.1f).build(),
            Contrast(0.05f),
        )

        FilterPreset.MOODY -> listOf(
            Contrast(0.25f),
            HslAdjustment.Builder().adjustSaturation(-0.25f).build(),
            Brightness(-0.05f),
        )

        FilterPreset.NEON -> listOf(
            HslAdjustment.Builder().adjustSaturation(0.55f).build(),
            Contrast(0.3f),
        )

        FilterPreset.DREAMY -> listOf(
            HslAdjustment.Builder().adjustSaturation(-0.15f).adjustLightness(0.05f).build(),
            Contrast(-0.15f),
            Brightness(0.08f),
        )

        FilterPreset.VINTAGE -> listOf(
            HslAdjustment.Builder().adjustHue(6f).adjustSaturation(-0.3f).build(),
            Contrast(-0.1f),
            Brightness(0.06f),
        )
    }
}
