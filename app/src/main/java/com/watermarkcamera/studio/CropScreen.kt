package com.watermarkcamera.studio

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropRotate
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

private enum class CropDragMode {
    MOVE,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

@Composable
fun CropScreen(bitmap: Bitmap, onCancel: () -> Unit, onApply: (Bitmap) -> Unit) {
    var working by remember(bitmap) { mutableStateOf(bitmap) }
    var ratio by remember { mutableStateOf(CropRatio.FREE) }
    var crop by remember(working) { mutableStateOf(RectF(.08f, .08f, .92f, .92f)) }
    val currentCrop = rememberUpdatedState(crop)
    val currentRatio = rememberUpdatedState(ratio)
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var dragMode by remember { mutableStateOf(CropDragMode.MOVE) }

    Column(Modifier.fillMaxSize().background(Color(0xFF111315))) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, "取消", tint = Color.White) }
            Text("裁剪", color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = {
                working = ImageProcessing.rotate90(working)
                crop = RectF(.08f, .08f, .92f, .92f)
            }) { Icon(Icons.Outlined.CropRotate, "旋转", tint = Color.White) }
            IconButton(onClick = { onApply(ImageProcessing.crop(working, crop)) }) {
                Icon(Icons.Outlined.Done, "应用裁剪", tint = Color(0xFF78D7C1))
            }
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().onSizeChanged { boxSize = it }
                .pointerInput(working, boxSize) {
                    detectDragGestures(
                        onDragStart = { point ->
                            val image = fittedImageRect(boxSize, working.width, working.height)
                            val rect = cropToScreen(currentCrop.value, image)
                            val cornerThreshold = 30.dp.toPx()
                            val edgeThreshold = 24.dp.toPx()
                            val corners = listOf(
                                CropDragMode.TOP_LEFT to rect.topLeft,
                                CropDragMode.TOP_RIGHT to rect.topRight,
                                CropDragMode.BOTTOM_LEFT to rect.bottomLeft,
                                CropDragMode.BOTTOM_RIGHT to rect.bottomRight
                            )
                            val corner = corners.minBy { (_, p) -> (p - point).getDistance() }
                            dragMode = if ((corner.second - point).getDistance() <= cornerThreshold) {
                                corner.first
                            } else {
                                val distances = listOf(
                                    CropDragMode.LEFT to abs(point.x - rect.left),
                                    CropDragMode.TOP to abs(point.y - rect.top),
                                    CropDragMode.RIGHT to abs(point.x - rect.right),
                                    CropDragMode.BOTTOM to abs(point.y - rect.bottom)
                                )
                                distances.minBy { it.second }.takeIf { it.second <= edgeThreshold }?.first
                                    ?: CropDragMode.MOVE
                            }
                        }
                    ) { change, amount ->
                        change.consume()
                        val image = fittedImageRect(boxSize, working.width, working.height)
                        if (image.width <= 0f || image.height <= 0f) return@detectDragGestures
                        val dx = amount.x / image.width
                        val dy = amount.y / image.height
                        crop = updateCrop(currentCrop.value, dragMode, dx, dy, currentRatio.value.value, working.width, working.height)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                bitmap = working.asImageBitmap(),
                contentDescription = "待裁剪照片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Canvas(Modifier.fillMaxSize()) {
                val image = fittedImageRect(IntSize(size.width.toInt(), size.height.toInt()), working.width, working.height)
                val rect = cropToScreen(crop, image)
                val shade = Color.Black.copy(alpha = .58f)
                drawRect(shade, topLeft = image.topLeft, size = Size(image.width, rect.top - image.top))
                drawRect(shade, topLeft = Offset(image.left, rect.bottom), size = Size(image.width, image.bottom - rect.bottom))
                drawRect(shade, topLeft = Offset(image.left, rect.top), size = Size(rect.left - image.left, rect.height))
                drawRect(shade, topLeft = Offset(rect.right, rect.top), size = Size(image.right - rect.right, rect.height))
                drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(2.dp.toPx()))
                drawLine(Color.White.copy(.55f), Offset(rect.left + rect.width / 3, rect.top), Offset(rect.left + rect.width / 3, rect.bottom), 1.dp.toPx())
                drawLine(Color.White.copy(.55f), Offset(rect.left + rect.width * 2 / 3, rect.top), Offset(rect.left + rect.width * 2 / 3, rect.bottom), 1.dp.toPx())
                drawLine(Color.White.copy(.55f), Offset(rect.left, rect.top + rect.height / 3), Offset(rect.right, rect.top + rect.height / 3), 1.dp.toPx())
                drawLine(Color.White.copy(.55f), Offset(rect.left, rect.top + rect.height * 2 / 3), Offset(rect.right, rect.top + rect.height * 2 / 3), 1.dp.toPx())
                val handleColor = Color(0xFF60A5FA)
                listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach {
                    drawCircle(Color.White, 7.dp.toPx(), it)
                    drawCircle(handleColor, 7.dp.toPx(), it, style = Stroke(1.5.dp.toPx()))
                }
                listOf(
                    Offset(rect.center.x, rect.top),
                    Offset(rect.center.x, rect.bottom),
                    Offset(rect.left, rect.center.y),
                    Offset(rect.right, rect.center.y)
                ).forEach {
                    val topLeft = Offset(it.x - 6.dp.toPx(), it.y - 3.dp.toPx())
                    val handleSize = Size(12.dp.toPx(), 6.dp.toPx())
                    drawRect(Color.White, topLeft, handleSize)
                    drawRect(handleColor, topLeft, handleSize, style = Stroke(1.5.dp.toPx()))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropRatio.entries.forEach { item ->
                FilterChip(
                    selected = ratio == item,
                    onClick = {
                        ratio = item
                        crop = cropForRatio(item.value, working.width, working.height)
                    },
                    label = { Text(item.label) }
                )
            }
        }
    }
}

private fun fittedImageRect(size: IntSize, imageWidth: Int, imageHeight: Int): Rect {
    if (size.width == 0 || size.height == 0) return Rect.Zero
    val scale = min(size.width.toFloat() / imageWidth, size.height.toFloat() / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return Rect((size.width - width) / 2f, (size.height - height) / 2f, (size.width + width) / 2f, (size.height + height) / 2f)
}

private fun cropToScreen(crop: RectF, image: Rect) = Rect(
    image.left + crop.left * image.width,
    image.top + crop.top * image.height,
    image.left + crop.right * image.width,
    image.top + crop.bottom * image.height
)

private fun cropForRatio(ratio: Float?, width: Int, height: Int): RectF {
    if (ratio == null || ratio == 0f) return RectF(.05f, .05f, .95f, .95f)
    val sourceRatio = width.toFloat() / height
    return if (ratio > sourceRatio) {
        val normalizedHeight = .9f * sourceRatio / ratio
        RectF(.05f, .5f - normalizedHeight / 2, .95f, .5f + normalizedHeight / 2)
    } else {
        val normalizedWidth = .9f * ratio / sourceRatio
        RectF(.5f - normalizedWidth / 2, .05f, .5f + normalizedWidth / 2, .95f)
    }
}

private fun updateCrop(
    old: RectF,
    mode: CropDragMode,
    dx: Float,
    dy: Float,
    ratio: Float?,
    imageWidth: Int,
    imageHeight: Int
): RectF {
    val minSize = .08f
    var left = old.left
    var top = old.top
    var right = old.right
    var bottom = old.bottom
    when (mode) {
        CropDragMode.MOVE -> {
            val moveX = dx.coerceIn(-left, 1f - right)
            val moveY = dy.coerceIn(-top, 1f - bottom)
            left += moveX; right += moveX; top += moveY; bottom += moveY
        }
        CropDragMode.LEFT -> left = (left + dx).coerceAtMost(right - minSize)
        CropDragMode.TOP -> top = (top + dy).coerceAtMost(bottom - minSize)
        CropDragMode.RIGHT -> right = (right + dx).coerceAtLeast(left + minSize)
        CropDragMode.BOTTOM -> bottom = (bottom + dy).coerceAtLeast(top + minSize)
        CropDragMode.TOP_LEFT -> { left = (left + dx).coerceAtMost(right - minSize); top = (top + dy).coerceAtMost(bottom - minSize) }
        CropDragMode.TOP_RIGHT -> { right = (right + dx).coerceAtLeast(left + minSize); top = (top + dy).coerceAtMost(bottom - minSize) }
        CropDragMode.BOTTOM_LEFT -> { left = (left + dx).coerceAtMost(right - minSize); bottom = (bottom + dy).coerceAtLeast(top + minSize) }
        CropDragMode.BOTTOM_RIGHT -> { right = (right + dx).coerceAtLeast(left + minSize); bottom = (bottom + dy).coerceAtLeast(top + minSize) }
    }
    left = left.coerceIn(0f, 1f); right = right.coerceIn(0f, 1f)
    top = top.coerceIn(0f, 1f); bottom = bottom.coerceIn(0f, 1f)
    if (ratio != null && ratio > 0f && mode != CropDragMode.MOVE) {
        val desiredNormalizedRatio = ratio * imageHeight / imageWidth
        if (mode == CropDragMode.TOP || mode == CropDragMode.BOTTOM) {
            val newHeight = bottom - top
            val newWidth = (newHeight * desiredNormalizedRatio).coerceAtLeast(minSize)
            val centerX = (left + right) / 2f
            left = (centerX - newWidth / 2f).coerceAtLeast(0f)
            right = (centerX + newWidth / 2f).coerceAtMost(1f)
        } else {
            val newWidth = right - left
            val newHeight = (newWidth / desiredNormalizedRatio).coerceAtLeast(minSize)
            if (mode == CropDragMode.TOP_LEFT || mode == CropDragMode.TOP_RIGHT) top = (bottom - newHeight).coerceAtLeast(0f)
            else if (mode == CropDragMode.BOTTOM_LEFT || mode == CropDragMode.BOTTOM_RIGHT) bottom = (top + newHeight).coerceAtMost(1f)
            else {
                val centerY = (top + bottom) / 2f
                top = (centerY - newHeight / 2f).coerceAtLeast(0f)
                bottom = (centerY + newHeight / 2f).coerceAtMost(1f)
            }
        }
    }
    return RectF(left, top, right, bottom)
}
