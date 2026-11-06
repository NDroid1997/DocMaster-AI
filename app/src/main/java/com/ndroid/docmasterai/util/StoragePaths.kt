package com.ndroid.docmasterai.util

/** Shared download locations for conversions, PDF exports, and resized images. */
object StoragePaths {
    const val DOWNLOAD_SUBDIR = "DocMasterAi"

    // Android's canonical directory name, not the localized UI label "Downloads".
    fun downloadsRelativePath(): String =
        "Download/$DOWNLOAD_SUBDIR"

    fun downloadsUserVisiblePath(fileName: String): String =
        "${downloadsRelativePath()}/$fileName"
}

