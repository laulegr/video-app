package com.laulegr.videoapp.ai

import com.laulegr.videoapp.model.FilterPreset

/**
 * A suggested edit: per-clip trim lengths plus a global filter. Returned by a
 * [TemplateEngine] and applied to the current project when the user taps it.
 */
data class EditTemplate(
    val id: String,
    val title: String,
    val description: String,
    val perClipDurationMs: Long?,
    val filter: FilterPreset,
    val speed: Float = 1f,
)
