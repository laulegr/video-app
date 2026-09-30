package com.laulegr.videoapp.ai

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.laulegr.videoapp.model.VideoClip
import kotlinx.coroutines.tasks.await

/**
 * On-device scene/object labels (ML Kit Image Labeling, runs locally - no
 * network, no API key) for each clip's first frame. Used by
 * [ContentAwareTemplateEngine] to pick a smarter suggestion than the pure
 * duration/count heuristic in [HeuristicTemplateEngine] can.
 */
object ContentLabeler {

    suspend fun dominantLabels(clips: List<VideoClip>, minConfidence: Float = 0.6f): List<String> {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val counts = mutableMapOf<String, Int>()

        for (clip in clips) {
            val bitmap = clip.thumbnail ?: continue
            val labels = runCatching {
                labeler.process(InputImage.fromBitmap(bitmap, 0)).await()
            }.getOrNull() ?: continue

            labels.filter { it.confidence >= minConfidence }
                .forEach { label -> counts[label.text] = (counts[label.text] ?: 0) + 1 }
        }

        return counts.entries.sortedByDescending { it.value }.map { it.key }
    }
}
