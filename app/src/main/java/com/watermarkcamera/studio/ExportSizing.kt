package com.watermarkcamera.studio

import kotlin.math.roundToInt

enum class ExportAspect(val label: String, private val fixedRatio: Float?) {
    ORIGINAL("原始", null),
    SQUARE("1:1", 1f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    PORTRAIT_9_16("9:16", 9f / 16f);

    fun ratioFor(sourceWidth: Int, sourceHeight: Int): Float =
        fixedRatio ?: sourceWidth.toFloat() / sourceHeight.coerceAtLeast(1)
}

data class ExportDimensions(val width: Int, val height: Int)

data class ExportCropRect(val left: Int, val top: Int, val width: Int, val height: Int)

fun exportDimensionsFromWidth(width: Int, ratio: Float): ExportDimensions =
    ExportDimensions(width, (width / ratio).roundToInt().coerceAtLeast(1))

fun exportDimensionsFromHeight(height: Int, ratio: Float): ExportDimensions =
    ExportDimensions((height * ratio).roundToInt().coerceAtLeast(1), height)

fun exportDimensionsAfterWidthEdit(
    width: Int,
    currentHeight: Int,
    ratio: Float,
    ratioLocked: Boolean
): ExportDimensions = if (ratioLocked) {
    exportDimensionsFromWidth(width, ratio)
} else {
    ExportDimensions(width, currentHeight)
}

fun exportDimensionsAfterHeightEdit(
    currentWidth: Int,
    height: Int,
    ratio: Float,
    ratioLocked: Boolean
): ExportDimensions = if (ratioLocked) {
    exportDimensionsFromHeight(height, ratio)
} else {
    ExportDimensions(currentWidth, height)
}

fun exportDimensionsForShortEdge(shortEdge: Int, ratio: Float): ExportDimensions =
    if (ratio >= 1f) exportDimensionsFromHeight(shortEdge, ratio)
    else exportDimensionsFromWidth(shortEdge, ratio)

fun originalExportDimensions(
    sourceWidth: Int,
    sourceHeight: Int,
    aspect: ExportAspect
): ExportDimensions {
    val crop = centerExportCrop(sourceWidth, sourceHeight, aspect.ratioFor(sourceWidth, sourceHeight))
    return ExportDimensions(crop.width, crop.height)
}

fun centerExportCrop(sourceWidth: Int, sourceHeight: Int, targetRatio: Float): ExportCropRect {
    val sourceRatio = sourceWidth.toFloat() / sourceHeight.coerceAtLeast(1)
    return if (sourceRatio > targetRatio) {
        val width = (sourceHeight * targetRatio).roundToInt().coerceIn(1, sourceWidth)
        ExportCropRect((sourceWidth - width) / 2, 0, width, sourceHeight)
    } else {
        val height = (sourceWidth / targetRatio).roundToInt().coerceIn(1, sourceHeight)
        ExportCropRect(0, (sourceHeight - height) / 2, sourceWidth, height)
    }
}

fun isValidExportDimensions(dimensions: ExportDimensions): Boolean =
    dimensions.width in 1..8192 &&
        dimensions.height in 1..8192 &&
        dimensions.width.toLong() * dimensions.height <= 40_000_000L
