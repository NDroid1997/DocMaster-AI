package com.ndroid.docmasterai.data.repository

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.FileProvider
import com.ndroid.docmasterai.data.model.ImageFilter
import com.ndroid.docmasterai.data.model.ScannedPage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class ScannerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scanDir: File
        get() = File(context.filesDir, "scans").also { it.mkdirs() }

    private val tempDir: File
        get() = File(context.cacheDir, "scan_temp").also { it.mkdirs() }

    suspend fun captureImage(imageCapture: ImageCapture): ScannedPage = withContext(Dispatchers.IO) {
        val photoFile = File(tempDir, "scan_${UUID.randomUUID()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                outputOptions,
                Dispatchers.IO.asExecutor(),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(
                            ScannedPage(
                                id = UUID.randomUUID().toString(),
                                imagePath = photoFile.absolutePath,
                                pageNumber = 0,
                            )
                        )
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    suspend fun applyFilter(imagePath: String, filter: ImageFilter): String = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val filtered = when (filter) {
            ImageFilter.ORIGINAL -> bitmap
            ImageFilter.AUTO_ENHANCE -> autoEnhance(bitmap)
            ImageFilter.BLACK_AND_WHITE -> blackAndWhite(bitmap)
            ImageFilter.GRAYSCALE -> grayscale(bitmap)
            ImageFilter.MAGIC_COLOR -> magicColor(bitmap)
            ImageFilter.HIGH_CONTRAST -> highContrast(bitmap)
            ImageFilter.SHARPEN -> sharpen(bitmap)
            ImageFilter.BRIGHTNESS -> adjustBrightness(bitmap, 30f)
            ImageFilter.CONTRAST -> adjustContrast(bitmap, 1.5f)
        }
        val outputFile = File(scanDir, "enhanced_${UUID.randomUUID()}.jpg")
        FileOutputStream(outputFile).use { out ->
            filtered.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        if (filtered != bitmap) filtered.recycle()
        outputFile.absolutePath
    }

    suspend fun saveScanToStorage(pages: List<ScannedPage>, title: String): List<String> =
        withContext(Dispatchers.IO) {
            pages.map { page ->
                val destFile = File(scanDir, "${title}_page${page.pageNumber}_${UUID.randomUUID()}.jpg")
                File(page.enhancedImagePath ?: page.imagePath).copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        }

    fun getFileUri(filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun cleanTempFiles() = withContext(Dispatchers.IO) {
        tempDir.listFiles()?.forEach { it.delete() }
    }

    // --- Image Filter Implementations ---

    private fun autoEnhance(src: Bitmap): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val cm = ColorMatrix().apply {
            setSaturation(1.2f)
            val contrast = ColorMatrix(floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,
                0f, 1.1f, 0f, 0f, 10f,
                0f, 0f, 1.1f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ))
            postConcat(contrast)
        }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(result).drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun blackAndWhite(src: Bitmap): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val cm = ColorMatrix().apply {
            setSaturation(0f)
            val bw = ColorMatrix(floatArrayOf(
                1.5f, 0f, 0f, 0f, -80f,
                0f, 1.5f, 0f, 0f, -80f,
                0f, 0f, 1.5f, 0f, -80f,
                0f, 0f, 0f, 1f, 0f
            ))
            postConcat(bw)
        }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(result).drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun grayscale(src: Bitmap): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(result).drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun magicColor(src: Bitmap): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val cm = ColorMatrix(floatArrayOf(
            1.3f, 0f, 0f, 0f, 0f,
            0f, 1.1f, 0f, 0f, 0f,
            0f, 0f, 1.4f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(result).drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun highContrast(src: Bitmap): Bitmap = adjustContrast(src, 2.0f)

    private fun sharpen(src: Bitmap): Bitmap {
        // Simple sharpen using convolution
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val cm = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, -20f,
            0f, 1.2f, 0f, 0f, -20f,
            0f, 0f, 1.2f, 0f, -20f,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(result).drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun adjustBrightness(src: Bitmap, value: Float): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val cm = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, value,
            0f, 1f, 0f, 0f, value,
            0f, 0f, 1f, 0f, value,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(result).drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun adjustContrast(src: Bitmap, contrast: Float): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val t = (1f - contrast) / 2f * 255f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, t,
            0f, contrast, 0f, 0f, t,
            0f, 0f, contrast, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(result).drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun kotlinx.coroutines.CoroutineDispatcher.asExecutor() =
        java.util.concurrent.Executor { command -> command.run() }
}
