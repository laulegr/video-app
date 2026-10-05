package com.laulegr.videoapp.ai

import com.laulegr.videoapp.model.FilterPreset
import com.laulegr.videoapp.model.Transition
import com.laulegr.videoapp.model.TransitionType
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

        // Many short clips -> fast-paced montage with quick zoom punches.
        if (clips.size >= 4 || avgMs < 4_000) {
            suggestions += EditTemplate(
                id = "quick_cuts",
                title = "Quick Cuts",
                description = "Kurze, knackige Schnitte (${clips.size} Clips) mit Zoom-Punch – schneller Reel-Rhythmus.",
                perClipDurationMs = 1_500L,
                filter = FilterPreset.VIBRANT,
                transition = Transition(TransitionType.ZOOM_IN, 250L),
            )
        }

        // Few, longer clips -> highlight reel, keep more of each clip.
        if (clips.size <= 3 && avgMs >= 4_000) {
            suggestions += EditTemplate(
                id = "highlight",
                title = "Highlight",
                description = "Längere Ausschnitte je Clip, weiche Wischer – ruhig, aber nicht langweilig.",
                perClipDurationMs = (avgMs * 0.6).toLong().coerceAtLeast(2_000L),
                filter = FilterPreset.WARM,
                transition = Transition(TransitionType.SWIPE_LEFT, 400L),
            )
        }

        if (clips.size >= 2) {
            suggestions += EditTemplate(
                id = "cinematic",
                title = "Cinematic",
                description = "Moody-Look mit kurzer Schwarzblende – wirkt wie ein kleiner Film.",
                perClipDurationMs = (avgMs * 0.7).toLong().coerceAtLeast(2_000L),
                filter = FilterPreset.MOODY,
                transition = Transition(TransitionType.FADE_BLACK, 500L),
            )
        }

        suggestions += EditTemplate(
            id = "retro",
            title = "Retro Vibes",
            description = "Vintage-Look mit Blitz-Übergängen – wie eine alte Filmkamera.",
            perClipDurationMs = null,
            filter = FilterPreset.VINTAGE,
            transition = Transition(TransitionType.FLASH_WHITE, 300L),
        )

        suggestions += EditTemplate(
            id = "hype",
            title = "Hype Reel",
            description = "Neon, 1.25x Tempo, Glitch zwischen den Clips – maximal Energie.",
            perClipDurationMs = 1_200L,
            filter = FilterPreset.NEON,
            speed = 1.25f,
            transition = Transition(TransitionType.GLITCH, 300L),
        )

        suggestions += EditTemplate(
            id = "spin",
            title = "Spin Edit",
            description = "Schnelle Dreh-Übergänge, satte Farben – der klassische Trend-Edit.",
            perClipDurationMs = 2_000L,
            filter = FilterPreset.VIBRANT,
            transition = Transition(TransitionType.SPIN, 300L),
        )

        // Always offer a neutral, untouched option.
        suggestions += EditTemplate(
            id = "clean",
            title = "Clean Cut",
            description = "Original-Länge der Clips, kein Filter, harte Schnitte – nur zusammengefügt.",
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

    private val actionLabels = setOf("Vehicle", "Sport", "Sports equipment", "Outdoor recreation", "Bicycle", "Extreme sport")
    private val portraitLabels = setOf("Person", "Face", "Selfie", "Smile", "Portrait photography")
    private val foodLabels = setOf("Food", "Dish", "Cuisine", "Meal", "Drink")
    private val natureLabels = setOf("Mountain", "Sky", "Nature", "Cloud", "Landscape", "Water", "Lake", "Horizon", "Hill")

    fun suggestFrom(labels: List<String>, clipCount: Int): EditTemplate? {
        val topLabels = labels.take(6).toSet()

        return when {
            topLabels.any { it in actionLabels } -> EditTemplate(
                id = "ai_action",
                title = "Action (KI)",
                description = "Erkannt: Bewegung/Sport im Material – 1s-Schnitte, Speed-up, Wackel-Übergänge.",
                perClipDurationMs = 1_000L,
                filter = FilterPreset.NEON,
                speed = 1.15f,
                transition = Transition(TransitionType.SHAKE, 300L),
            )

            topLabels.any { it in natureLabels } -> EditTemplate(
                id = "ai_nature",
                title = "Nature (KI)",
                description = "Erkannt: Landschaft/Natur im Material – kühler Look, Wischer wie ein Kameraschwenk.",
                perClipDurationMs = null,
                filter = FilterPreset.COOL,
                transition = Transition(TransitionType.SWIPE_LEFT, 400L),
            )

            topLabels.any { it in portraitLabels } -> EditTemplate(
                id = "ai_portrait",
                title = "Portrait (KI)",
                description = "Erkannt: Personen/Gesichter im Material – warmer Hautton, sanfte Blitz-Übergänge.",
                perClipDurationMs = null,
                filter = FilterPreset.WARM,
                transition = Transition(TransitionType.FLASH_WHITE, 300L),
            )

            topLabels.any { it in foodLabels } -> EditTemplate(
                id = "ai_food",
                title = "Food (KI)",
                description = "Erkannt: Essen/Getränke im Material – sattere Farben, Zoom-Punch.",
                perClipDurationMs = 2_000L,
                filter = FilterPreset.VIBRANT,
                transition = Transition(TransitionType.ZOOM_IN, 300L),
            )

            else -> null
        }
    }
}
