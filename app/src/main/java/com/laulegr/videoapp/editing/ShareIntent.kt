package com.laulegr.videoapp.editing

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the system share sheet for the exported MP4 (Instagram will show up as a
 * target automatically since the video already sits in MediaStore). No hard
 * dependency on Instagram's own intent contract, which keeps this working even
 * if that app isn't installed or changes its API.
 */
fun shareVideo(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Video teilen"))
}
