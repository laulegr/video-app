package com.laulegr.videoapp.ai

import com.laulegr.videoapp.model.FilterPreset
import com.laulegr.videoapp.model.VideoClip

/**
 * Suggests edit templates for the clips currently on the timeline.
 *
 * v1 ([HeuristicTemplateEngine]) is pure on-device logic based on clip count
 * and length — no network, no model weights, works instantly and offline.
 * It is deliberately behind this interface so a smarter engine (on-device
 * ML Kit classification of clip content, or a cloud call that looks at
 * actual frames/audio) can be swapped in later without touching the UI or
 * [com.laulegr.videoapp.ui.editor.EditorViewModel].
 */
interface TemplateEngine {
    fun suggest(clips: List<VideoClip>): List<EditTemplate>
}

class HeuristicTemplateEngine : TemplateEngine {

    override fun suggest(clips: List<VideoClip>): List<EditTemplate> {
        if (clips.isEmpty()) return emptyList()

        val totalMs = clips.sumOf { it.durationMs }
        val avgMs = totalMs / clips.size
        val suggestions = mutableListOf<EditTemplate>()

        // Many short clips -> fast-paced montage, hard cuts, punchy filter.
        if (clips.size >= 4 || avgMs < 4_000) {
            suggestions += EditTemplate(
                id = "quick_cuts",
                title = "Quick Cuts",
                description = "Kurze, knackige Schnitte (${clips.size} Clips) für einen schnellen Reel-Rhythmus.",
                perClipDurationMs = 1_500L,
                filter = FilterPreset.VIBRANT,
            )
        }

        // Few, longer clips -> highlight reel, keep more of each clip.
        if (clips.size <= 3 && avgMs >= 4_000) {
            suggestions += EditTemplate(
                id = "highlight",
                title = "Highlight",
                description = "Längere Ausschnitte je Clip, ruhigerer Schnitt für ein Highlight-Reel.",
                perClipDurationMs = (avgMs * 0.6).toLong().coerceAtLeast(2_000L),
                filter = FilterPreset.WARM,
            )
        }

        // Always offer a neutral, untouched option.
        suggestions += EditTemplate(
            id = "clean",
            title = "Clean Cut",
            description = "Original-Länge der Clips, kein Filter – nur zusammengefügt.",
            perClipDurationMs = null,
            filter = FilterPreset.NONE,
        )

        return suggestions
    }
}
