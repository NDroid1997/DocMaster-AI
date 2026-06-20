package com.ndroid.docmasterai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String = "",
    val title: String = "",
    val filePath: String = "",
    val thumbnailPath: String? = null,
    val fileType: String = "pdf",
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val pageCount: Int = 1,
    val sourceType: String = "scan",
    val folderPath: String = "",
)

data class ScannedPage(
    val id: String = "",
    val imagePath: String = "",
    val enhancedImagePath: String? = null,
    val filterApplied: String = "original",
    val ocrText: String? = null,
    val pageNumber: Int = 0,
    val rotation: Int = 0,
)

data class OcrResult(
    val text: String = "",
    val language: String = "latin",
    val confidence: Float = 0f,
    val blocks: List<TextBlock> = emptyList(),
    val processingTimeMs: Long = 0,
)

data class TextBlock(
    val text: String = "",
    val boundingBox: List<Int> = emptyList(), // [left, top, right, bottom]
    val confidence: Float = 0f,
)

data class ConversionTask(
    val id: String = "",
    val sourceFile: String = "",
    val outputFile: String = "",
    val conversionType: ConversionType = ConversionType.IMAGE_TO_PDF,
    val progress: Float = 0f,
    val status: TaskStatus = TaskStatus.PENDING,
)

enum class ConversionType(val displayName: String) {
    PDF_TO_IMAGE("PDF → Image"),
    IMAGE_TO_PDF("Image → PDF"),
    PNG_TO_JPG("PNG → JPG"),
    JPG_TO_PNG("JPG → PNG"),
    WEBP_TO_JPG("WEBP → JPG"),
    PDF_TO_TEXT("PDF → Text"),
    IMAGE_TO_TEXT("Image → Text"),
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED
}

enum class ImageFilter(val displayName: String) {
    ORIGINAL("Original"),
    AUTO_ENHANCE("Auto Enhance"),
    BLACK_AND_WHITE("Black & White"),
    GRAYSCALE("Grayscale"),
    MAGIC_COLOR("Magic Color"),
    HIGH_CONTRAST("High Contrast"),
    SHARPEN("Sharpen"),
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
}

data class PdfOperation(
    val id: String = "",
    val type: PdfOperationType = PdfOperationType.MERGE,
    val sourceFiles: List<String> = emptyList(),
    val outputFile: String = "",
    val progress: Float = 0f,
    val status: TaskStatus = TaskStatus.PENDING,
)

enum class PdfOperationType(val displayName: String) {
    MERGE("Merge PDFs"),
    SPLIT("Split PDF"),
    ROTATE("Rotate Pages"),
    REORDER("Reorder Pages"),
    DELETE_PAGES("Delete Pages"),
    COMPRESS("Compress PDF"),
    WATERMARK("Add Watermark"),
    PASSWORD_PROTECT("Password Protect"),
    REMOVE_PASSWORD("Remove Password"),
}

data class ResizeOptions(
    val mode: ResizeMode = ResizeMode.PERCENTAGE,
    val width: Int = 0,
    val height: Int = 0,
    val percentage: Int = 100,
    val quality: Int = 90,
    val maintainAspectRatio: Boolean = true,
    val outputFormat: String = "jpg",
)

enum class ResizeMode(val displayName: String) {
    WIDTH("By Width"),
    HEIGHT("By Height"),
    PERCENTAGE("By Percentage"),
    CUSTOM("Custom Resolution"),
}
