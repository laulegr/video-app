package com.laulegr.videoapp.ai

import com.laulegr.videoapp.model.FilterPreset
import com.laulegr.videoapp.model.Transition

/**
 * A suggested edit: per-clip trim length, filter, speed and the transition
 * used at every cut. Returned by a [TemplateEngine] and applied to the
 * current project when the user taps it.
 */
data class EditTemplate(
    val id: String,
    val title: String,
    val description: String,
    val perClipDurationMs: Long?,
    val filter: FilterPreset,
    val speed: Float = 1f,
    val transition: Transition = Transition.NONE,
)
