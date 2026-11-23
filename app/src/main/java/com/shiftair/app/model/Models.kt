package com.shiftair.app.model

import android.net.Uri
import java.io.File

data class MediaItemModel(
    val id: Long,
    val title: String,
    val subtitle: String,
    val uri: Uri,
    val durationMs: Long,
    val sizeBytes: Long,
    val isVideo: Boolean
)

data class FileItemModel(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
)
