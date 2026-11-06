package com.ndroid.docmasterai

import com.docedit.sdk.api.DocumentEditor
import com.docedit.sdk.api.EditorConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentEditorTest {
    @Test(expected = IllegalArgumentException::class)
    fun emptyEditorInputIsRejected() {
        DocumentEditor.Input()
    }

    @Test
    fun defaultResultIsCancelledWithoutOutput() {
        val result = DocumentEditor.Result()
        assertTrue(result.wasCancelled)
        assertTrue(result.images.isEmpty())
        assertNull(result.pdf)
    }

    @Test
    fun premiumAccessMustBeExplicit() {
        assertFalse(EditorConfig().isPremium)
        assertTrue(EditorConfig(isPremium = true).isPremium)
    }
}
