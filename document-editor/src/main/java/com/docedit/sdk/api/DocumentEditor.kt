package com.docedit.sdk.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.docedit.sdk.presentation.EditorActivity

/** Host entitlement information; basic page editing is available to every user. */
data class EditorConfig(val isPremium: Boolean = false)

object DocumentEditor {
    data class Input(
        val images: List<Uri> = emptyList(),
        val pdf: Uri? = null,
        val config: EditorConfig = EditorConfig(),
    ) {
        init {
            require(images.isNotEmpty() xor (pdf != null)) {
                "Provide either images or a PDF, but not both."
            }
        }
    }

    /** Output files are temporary. The host must copy them to persistent storage. */
    data class Result(
        val wasCancelled: Boolean = true,
        val images: List<Uri> = emptyList(),
        val pdf: Uri? = null,
        val thumbnail: Uri? = null,
        val pageCount: Int = 0,
    )

    fun registerLauncher(
        activity: ComponentActivity,
        onResult: (Result) -> Unit,
    ): ActivityResultLauncher<Input> = activity.registerForActivityResult(Contract(), onResult)

    class Contract : ActivityResultContract<Input, Result>() {
        override fun createIntent(context: Context, input: Input): Intent =
            Intent(context, EditorActivity::class.java).apply {
                putStringArrayListExtra(IMAGES, ArrayList(input.images.map(Uri::toString)))
                putExtra(PDF, input.pdf?.toString())
                putExtra(PREMIUM, input.config.isPremium)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Result {
            if (resultCode != Activity.RESULT_OK || intent == null) return Result()
            val pdf = intent.getStringExtra(PDF)?.let(Uri::parse) ?: return Result()
            val count = intent.getIntExtra(PAGE_COUNT, 0)
            if (count <= 0) return Result()
            return Result(
                wasCancelled = false,
                images = intent.getStringArrayListExtra(IMAGES).orEmpty().map(Uri::parse),
                pdf = pdf,
                thumbnail = intent.getStringExtra(THUMBNAIL)?.let(Uri::parse),
                pageCount = count,
            )
        }
    }

    internal fun readInput(intent: Intent): Input = Input(
        images = intent.getStringArrayListExtra(IMAGES).orEmpty().map(Uri::parse),
        pdf = intent.getStringExtra(PDF)?.let(Uri::parse),
        config = EditorConfig(intent.getBooleanExtra(PREMIUM, false)),
    )

    internal fun resultIntent(result: Result): Intent = Intent().apply {
        putStringArrayListExtra(IMAGES, ArrayList(result.images.map(Uri::toString)))
        putExtra(PDF, result.pdf?.toString())
        putExtra(THUMBNAIL, result.thumbnail?.toString())
        putExtra(PAGE_COUNT, result.pageCount)
    }

    private const val IMAGES = "com.docedit.sdk.images"
    private const val PDF = "com.docedit.sdk.pdf"
    private const val PREMIUM = "com.docedit.sdk.premium"
    private const val THUMBNAIL = "com.docedit.sdk.thumbnail"
    private const val PAGE_COUNT = "com.docedit.sdk.pageCount"
}
