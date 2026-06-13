package com.chin.minddump.storage

import android.net.Uri

/**
 * Represents a single item received via Android share intent.
 */
sealed class ShareItem {
    /**
     * Shared text content (e.g., from browser, notes app).
     */
    data class Text(val content: String) : ShareItem()

    /**
     * Shared file via content URI (e.g., image, video, document).
     */
    data class File(val uri: Uri, val fileName: String) : ShareItem()
}
