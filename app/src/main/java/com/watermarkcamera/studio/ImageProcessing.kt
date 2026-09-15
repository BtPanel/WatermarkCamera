package com.watermarkcamera.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageProcessing {
    suspend fun loadBitmap(context: Context, uri: Uri, maxSide: Int = 4096): Bitmap? = withContext(Dispatchers.IO) {
        decodePlatformBitmap(context, uri, maxSide)
    }

    private fun decodePlatformBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, uri, maxSide)
        } else {
            decodeWithBitmapFactory(context, uri, maxSide)
        }
    }.getOrNull()

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(context: Context, uri: Uri, maxSide: Int): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val sourceMaxSide = max(info.size.width, info.size.height)
            if (sourceMaxSide > maxSide) {
                val ratio = maxSide.toFloat() / sourceMaxSide
                decoder.setTargetSize(
                    max(1, (info.size.width * ratio).roundToInt()),
                    max(1, (info.size.height * ratio).roundToInt())
                )
            }
        }

    private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        var sample = 1
        while (max(options.outWidth, options.outHeight) / sample > maxSide) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } ?: return null

        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
        return if (matrix.isIdentity) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, matrix, true
        ).also { if (it !== decoded) decoded.recycle() }
    }

    fun crop(bitmap: Bitmap, normalized: RectF): Bitmap {
        val left = (normalized.left.coerceIn(0f, 0.99f) * bitmap.width).toInt()
        val top = (normalized.top.coerceIn(0f, 0.99f) * bitmap.height).toInt()
        val right = (normalized.right.coerceIn(0.01f, 1f) * bitmap.width).toInt()
        val bottom = (normalized.bottom.coerceIn(0.01f, 1f) * bitmap.height).toInt()
        return Bitmap.createBitmap(bitmap, left, top, max(1, right - left), max(1, bottom - top))
    }

    fun rotate90(bitmap: Bitmap): Bitmap = Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f) }, true
    )

    fun applyPhotoTransform(source: Bitmap, transform: PhotoTransform): Bitmap {
        if (transform.isIdentity()) return source.copy(Bitmap.Config.ARGB_8888, true)
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        drawPhoto(Canvas(result), source, transform)
        return result
    }

    suspend fun render(
        context: Context,
        source: Bitmap,
        layers: List<WatermarkLayer>,
        photoTransform: PhotoTransform = PhotoTransform(),
        outputDimensions: ExportDimensions = ExportDimensions(source.width, source.height)
    ): Bitmap =
        withContext(Dispatchers.Default) {
            require(isValidExportDimensions(outputDimensions)) { "Invalid export dimensions" }
            val result = Bitmap.createBitmap(outputDimensions.width, outputDimensions.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val outputRatio = outputDimensions.width.toFloat() / outputDimensions.height
            val crop = centerExportCrop(source.width, source.height, outputRatio)
            val scale = max(
                outputDimensions.width.toFloat() / crop.width,
                outputDimensions.height.toFloat() / crop.height
            )
            canvas.save()
            canvas.translate(
                (outputDimensions.width - crop.width * scale) / 2f,
                (outputDimensions.height - crop.height * scale) / 2f
            )
            canvas.scale(scale, scale)
            canvas.translate(-crop.left.toFloat(), -crop.top.toFloat())
            drawPhoto(canvas, source, photoTransform)
            val unit = min(source.width, source.height) / 360f
            layers.filterNot { it.hidden }.forEach { layer ->
                canvas.save()
                val centerX = layer.x * source.width
                val centerY = layer.y * source.height
                canvas.translate(centerX, centerY)
                canvas.rotate(layer.rotation)
                canvas.scale(layer.scaleX, layer.scaleY)
                when (layer.kind) {
                    LayerKind.IMAGE -> drawImageLayer(context, canvas, layer, unit)
                    LayerKind.ICON -> drawIconLayer(canvas, layer, unit)
                    else -> drawTextLayer(context, canvas, layer, unit)
                }
                canvas.restore()
            }
            canvas.restore()
            result
        }

    private fun drawPhoto(canvas: Canvas, source: Bitmap, transform: PhotoTransform) {
        canvas.drawColor(Color.BLACK)
        canvas.save()
        canvas.translate(
            source.width / 2f + transform.offsetX * source.width,
            source.height / 2f + transform.offsetY * source.height
        )
        canvas.scale(transform.scale, transform.scale)
        canvas.drawBitmap(
            source,
            -source.width / 2f,
            -source.height / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        canvas.restore()
    }

    private fun drawTextLayer(context: Context, canvas: Canvas, layer: WatermarkLayer, unit: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.color
            alpha = (layer.alpha * 255).toInt()
            textSize = layer.fontSize * unit
            typeface = FontSupport.resolve(context, layer)
        }
        val value = layer.displayText()
        val leadingBitmap = if (layer.kind == LayerKind.LOCATION) {
            layer.imageUri?.let(Uri::parse)?.let { decodePlatformBitmap(context, it, 512) }
        } else {
            null
        }
        val maxSize = layer.baseSize * unit
        val imageRatio = leadingBitmap?.let { min(maxSize / it.width, maxSize / it.height) }
        val imageWidth = if (leadingBitmap != null && imageRatio != null) leadingBitmap.width * imageRatio else 0f
        val imageHeight = if (leadingBitmap != null && imageRatio != null) leadingBitmap.height * imageRatio else 0f
        val gap = if (leadingBitmap != null) 3.3f * unit else 0f

        if (layer.kind == LayerKind.LOCATION && layer.maxLines > 1) {
            val availableCanvasWidth = when (layer.horizontalAnchor) {
                LayerHorizontalAnchor.LEFT -> (1f - layer.x) * canvas.width
                LayerHorizontalAnchor.CENTER -> 2f * min(layer.x, 1f - layer.x) * canvas.width
                LayerHorizontalAnchor.RIGHT -> layer.x * canvas.width
            }
            val availableTextWidth = (
                availableCanvasWidth / layer.scaleX.coerceAtLeast(.15f) - imageWidth - gap - 8f * unit
            ).coerceAtLeast(24f * unit)
            val lines = TextLineBreaker.breakLines(
                text = value,
                maxWidth = availableTextWidth,
                maxLines = layer.maxLines,
                measureWidth = paint::measureText
            )
            val metrics = paint.fontMetrics
            val lineHeight = paint.textSize * BuiltInTemplateLayout.LOCATION_LINE_HEIGHT_MULTIPLIER
            val textWidth = lines.maxOfOrNull(paint::measureText) ?: 0f
            val textHeight = lines.size * lineHeight
            val contentWidth = imageWidth + gap + textWidth
            val contentHeight = max(imageHeight, textHeight)
            val left = when (layer.horizontalAnchor) {
                LayerHorizontalAnchor.LEFT -> 0f
                LayerHorizontalAnchor.CENTER -> -contentWidth / 2f
                LayerHorizontalAnchor.RIGHT -> -contentWidth
            }
            val top = -contentHeight / 2f
            if (leadingBitmap != null) {
                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (layer.alpha * 255).toInt()
                }
                canvas.drawBitmap(
                    leadingBitmap,
                    null,
                    RectF(left, top, left + imageWidth, top + imageHeight),
                    imagePaint
                )
            }
            paint.textAlign = Paint.Align.LEFT
            val textX = left + imageWidth + gap
            val firstBaseline = top - metrics.ascent
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, textX, firstBaseline + index * lineHeight, paint)
            }
            leadingBitmap?.recycle()
            return
        }

        if (layer.kind == LayerKind.LOCATION) {
            val availableCanvasWidth = when (layer.horizontalAnchor) {
                LayerHorizontalAnchor.LEFT -> (1f - layer.x) * canvas.width
                LayerHorizontalAnchor.CENTER -> 2f * min(layer.x, 1f - layer.x) * canvas.width
                LayerHorizontalAnchor.RIGHT -> layer.x * canvas.width
            }
            val availableTextWidth = (
                availableCanvasWidth / layer.scaleX.coerceAtLeast(.15f) - imageWidth - gap - 8f * unit
            ).coerceAtLeast(24f * unit)
            val measuredWidth = paint.measureText(value)
            if (measuredWidth > availableTextWidth && measuredWidth > 0f) {
                paint.textSize = (paint.textSize * availableTextWidth / measuredWidth).coerceAtLeast(6f * unit)
            }
        }

        val metrics = paint.fontMetrics
        val baseline = -(metrics.ascent + metrics.descent) / 2f
        if (leadingBitmap == null) {
            paint.textAlign = when (layer.horizontalAnchor) {
                LayerHorizontalAnchor.LEFT -> Paint.Align.LEFT
                LayerHorizontalAnchor.CENTER -> Paint.Align.CENTER
                LayerHorizontalAnchor.RIGHT -> Paint.Align.RIGHT
            }
            canvas.drawText(value, 0f, baseline, paint)
            return
        }

        val textWidth = paint.measureText(value)
        val contentWidth = imageWidth + gap + textWidth
        val left = when (layer.horizontalAnchor) {
            LayerHorizontalAnchor.LEFT -> 0f
            LayerHorizontalAnchor.CENTER -> -contentWidth / 2f
            LayerHorizontalAnchor.RIGHT -> -contentWidth
        }
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (layer.alpha * 255).toInt()
        }
        canvas.drawBitmap(
            leadingBitmap,
            null,
            RectF(left, -imageHeight / 2f, left + imageWidth, imageHeight / 2f),
            imagePaint
        )
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(value, left + imageWidth + gap, baseline, paint)
        leadingBitmap.recycle()
    }

    private fun drawImageLayer(context: Context, canvas: Canvas, layer: WatermarkLayer, unit: Float) {
        val uri = layer.imageUri?.let(Uri::parse) ?: return
        val bitmap = decodePlatformBitmap(context, uri, 2048) ?: return
        val maxSize = layer.baseSize * unit
        val ratio = min(maxSize / bitmap.width, maxSize / bitmap.height)
        val w = bitmap.width * ratio
        val h = bitmap.height * ratio
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (layer.alpha * 255).toInt()
        }
        canvas.drawBitmap(bitmap, null, RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), paint)
        bitmap.recycle()
    }

    private fun drawIconLayer(canvas: Canvas, layer: WatermarkLayer, unit: Float) {
        val size = 48f * unit
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.color
            alpha = (layer.alpha * 255).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f * unit
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        when (layer.icon) {
            BuiltInIcon.CAMERA -> {
                canvas.drawRoundRect(RectF(-size / 2, -size * .32f, size / 2, size * .32f), 5f * unit, 5f * unit, stroke)
                canvas.drawCircle(0f, 0f, size * .18f, stroke)
                canvas.drawLine(-size * .25f, -size * .32f, -size * .15f, -size * .45f, stroke)
                canvas.drawLine(-size * .15f, -size * .45f, size * .06f, -size * .45f, stroke)
            }
            BuiltInIcon.PIN -> {
                canvas.drawCircle(0f, -size * .12f, size * .24f, stroke)
                canvas.drawCircle(0f, -size * .12f, size * .07f, stroke)
                canvas.drawLine(-size * .17f, size * .06f, 0f, size * .42f, stroke)
                canvas.drawLine(size * .17f, size * .06f, 0f, size * .42f, stroke)
            }
            BuiltInIcon.CHECK -> {
                canvas.drawCircle(0f, 0f, size * .42f, stroke)
                canvas.drawLine(-size * .2f, 0f, -size * .04f, size * .16f, stroke)
                canvas.drawLine(-size * .04f, size * .16f, size * .24f, -size * .16f, stroke)
            }
            BuiltInIcon.STAR -> {
                val path = android.graphics.Path()
                for (i in 0 until 10) {
                    val radius = if (i % 2 == 0) size * .45f else size * .2f
                    val angle = Math.toRadians((-90 + i * 36).toDouble())
                    val x = (kotlin.math.cos(angle) * radius).toFloat()
                    val y = (kotlin.math.sin(angle) * radius).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, stroke)
            }
            BuiltInIcon.DIVIDER -> {
                canvas.drawLine(-size / 2f, 0f, size / 2f, 0f, stroke)
            }
        }
    }

    suspend fun saveToGallery(context: Context, bitmap: Bitmap, quality: Int = 95): Uri? = withContext(Dispatchers.IO) {
        val name = "Watermark_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WatermarkCameraStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext null
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            uri
        }.getOrElse {
            context.contentResolver.delete(uri, null, null)
            null
        }
    }

    fun saveDraftBitmap(context: Context, bitmap: Bitmap): Uri {
        val folder = File(context.filesDir, "drafts").apply { mkdirs() }
        val file = File(folder, "current.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        return Uri.fromFile(file)
    }
}
