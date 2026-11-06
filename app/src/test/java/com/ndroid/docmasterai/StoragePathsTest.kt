package com.ndroid.docmasterai

import com.ndroid.docmasterai.util.StoragePaths
import org.junit.Assert.assertEquals
import org.junit.Test

class StoragePathsTest {
    @Test
    fun relativePathMatchesExistingDownloadFolder() {
        assertEquals("DocMasterAi", StoragePaths.DOWNLOAD_SUBDIR)
        assertEquals("Download/DocMasterAi", StoragePaths.downloadsRelativePath())
    }

    @Test
    fun visiblePathPreservesFileName() {
        assertEquals(
            "Download/DocMasterAi/My document.pdf",
            StoragePaths.downloadsUserVisiblePath("My document.pdf"),
        )
    }
}
