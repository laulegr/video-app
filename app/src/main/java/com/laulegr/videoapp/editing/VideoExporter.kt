package com.laulegr.videoapp.editing

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.laulegr.videoapp.model.FilterPreset
import com.laulegr.videoapp.model.TransitionType
import com.laulegr.videoapp.model.VideoClip
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface ExportOutcome {
    data class Success(val uri: Uri) : ExportOutcome
    data class Failure(val message: String) : ExportOutcome
}

/** Optional background music mixed under the clips' own audio. [volume] is 0f (silent) - 1f (full). */
data class MusicSettings(val uri: Uri, val volume: Float)

/**
 * Trims, speed-changes, fades and concatenates [VideoClip]s into a single MP4
 * via Media3 Transformer, applies a [FilterPreset] and optional caption
 * overlays, mixes in optional background [MusicSettings], then copies the
 * result into MediaStore so it shows up in the gallery and can be shared.
 *
 * Must be called from a thread with a Looper (the Transformer requirement) -
 * call it from a coroutine on Dispatchers.Main.
 */
class VideoExporter(private val context: Context) {

    suspend fun export(
        clips: List<VideoClip>,
        filter: FilterPreset,
        transition: TransitionType,
        music: MusicSettings?,
    ): ExportOutcome {
        if (clips.isEmpty()) return ExportOutcome.Failure("Keine Clips ausgewählt.")

        val outputFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.mp4")

        return try {
            runTransformer(clips, filter, transition, music, outputFile)
            val savedUri = saveToGallery(outputFile)
            ExportOutcome.Success(savedUri)
        } catch (t: Throwable) {
            ExportOutcome.Failure(t.message ?: "Export fehlgeschlagen.")
        } finally {
            outputFile.delete()
        }
    }

    private suspend fun runTransformer(
        clips: List<VideoClip>,
        filter: FilterPreset,
        transition: TransitionType,
        music: MusicSettings?,
        outputFile: File,
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val editedItems = clips.mapIndexed { index, clip ->
            clip.toEditedMediaItem(
                filter = filter,
                transition = transition,
                isFirstClip = index == 0,
                isLastClip = index == clips.lastIndex,
            )
        }
        val videoSequence = EditedMediaItemSequence.Builder(editedItems).build()

        val sequences = mutableListOf(videoSequence)
        if (music != null) {
            val totalDurationMs = clips.sumOf { it.effectiveDurationMs }
            sequences += buildMusicSequence(music, totalDurationMs)
        }

        val composition = Composition.Builder(sequences).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    if (continuation.isActive) continuation.resumeWithException(exportException)
                }
            })
            .build()

        transformer.start(composition, outputFile.absolutePath)

        continuation.invokeOnCancellation { transformer.cancel() }
    }

    private fun VideoClip.toEditedMediaItem(
        filter: FilterPreset,
        transition: TransitionType,
        isFirstClip: Boolean,
        isLastClip: Boolean,
    ): EditedMediaItem {
        val clippedItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(trimStartMs)
                    .setEndPositionMs(trimEndMs)
                    .build()
            )
            .build()

        val speedEffects = buildSpeedEffects(speed)
        val fade = FadeEffects.forClip(
            clipEffectiveDurationUs = effectiveDurationMs * 1_000L,
            isFirstClip = isFirstClip,
            isLastClip = isLastClip,
            transition = transition,
        )
        val captionOverlay = buildCaptionOverlay(captionText)

        val videoEffects = buildList<Effect> {
            addAll(FilterEffects.effectsFor(filter))
            speedEffects.videoEffect?.let(::add)
            fade?.let(::add)
            captionOverlay?.let(::add)
        }
        val audioProcessors = listOfNotNull(speedEffects.audioProcessor)

        return EditedMediaItem.Builder(clippedItem)
            .setEffects(Effects(audioProcessors, videoEffects))
            .build()
    }

    private fun buildMusicSequence(music: MusicSettings, totalDurationMs: Long): EditedMediaItemSequence {
        val musicItem = MediaItem.Builder()
            .setUri(music.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(totalDurationMs)
                    .build()
            )
            .build()

        val audioProcessors: List<AudioProcessor> = listOf(GainAudioProcessor(music.volume))

        val editedMusicItem = EditedMediaItem.Builder(musicItem)
            .setRemoveVideo(true)
            .setEffects(Effects(audioProcessors, emptyList()))
            .build()

        return EditedMediaItemSequence.Builder(listOf(editedMusicItem)).build()
    }

    private fun saveToGallery(file: File): Uri {
        val name = "ReelCut_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ReelCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Konnte Export nicht in der Galerie speichern.")

        resolver.openOutputStream(itemUri).use { out ->
            FileInputStream(file).use { input ->
                input.copyTo(out ?: throw IllegalStateException("Kein Output-Stream."))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }

        return itemUri
    }
}
