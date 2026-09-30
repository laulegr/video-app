package com.laulegr.videoapp.editing

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a single preview frame and the duration of a picked video via MediaMetadataRetriever. */
object VideoMetadata {

    data class Info(val durationMs: Long, val thumbnail: Bitmap?)

    suspend fun read(context: Context, uri: Uri): Info = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            Info(durationMs = durationMs, thumbnail = frame)
        } finally {
            retriever.release()
        }
    }
}
