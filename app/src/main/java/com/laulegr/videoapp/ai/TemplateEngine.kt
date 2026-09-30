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

/**
 * v2: refines the v1 heuristic with on-device content labels from
 * [ContentLabeler]. Produces at most one extra, more specific suggestion -
 * it never replaces the fast, always-available heuristic results.
 */
object ContentAwareTemplateEngine {

    private val actionLabels = setOf("Vehicle", "Sport", "Sports equipment", "Outdoor", "Water", "Bicycle")
    private val portraitLabels = setOf("Person", "Face", "Selfie", "Smile", "Portrait photography")
    private val foodLabels = setOf("Food", "Dish", "Cuisine", "Meal", "Drink")

    fun suggestFrom(labels: List<String>, clipCount: Int): EditTemplate? {
        val topLabels = labels.take(5).toSet()

        return when {
            topLabels.any { it in actionLabels } -> EditTemplate(
                id = "ai_action",
                title = "Action (KI)",
                description = "Erkannt: Bewegung/Sport im Material – knackige 1s-Schnitte, leichter Speed-up, knalliger Filter.",
                perClipDurationMs = 1_000L,
                filter = FilterPreset.VIBRANT,
                speed = 1.15f,
            )

            topLabels.any { it in portraitLabels } -> EditTemplate(
                id = "ai_portrait",
                title = "Portrait (KI)",
                description = "Erkannt: Personen/Gesichter im Material – ruhigere Schnitte, warmer Hautton-Filter.",
                perClipDurationMs = null,
                filter = FilterPreset.WARM,
            )

            topLabels.any { it in foodLabels } -> EditTemplate(
                id = "ai_food",
                title = "Food (KI)",
                description = "Erkannt: Essen/Getränke im Material – sattere Farben, mittleres Tempo.",
                perClipDurationMs = 2_000L,
                filter = FilterPreset.VIBRANT,
            )

            else -> null
        }
    }
}
