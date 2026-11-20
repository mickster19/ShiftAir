package com.shiftair.app.model

data class MediaItem(
    val id: Long,
    val title: String,
    val path: String,
    val uri: android.net.Uri,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val mimeType: String = ""
)

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)
