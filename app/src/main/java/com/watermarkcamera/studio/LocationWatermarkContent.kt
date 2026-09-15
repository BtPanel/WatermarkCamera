package com.watermarkcamera.studio

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LocationWatermarkContent(
    layer: WatermarkLayer,
    leadingBitmap: Bitmap?,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    contentScale: Float = 1f
) {
    val text = layer.displayText()
    val requestedFontSize = layer.fontSize * contentScale
    val maxTextWidth = (304f * contentScale).dp
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val naturalTextWidth = remember(text, requestedFontSize, fontFamily, layer.bold) {
        textMeasurer.measure(
            text = AnnotatedString(text),
            style = TextStyle(
                fontSize = requestedFontSize.sp,
                fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = fontFamily,
                letterSpacing = 0.sp
            ),
            maxLines = 1,
            softWrap = false
        ).size.width
    }
    val maxTextWidthPx = with(density) { maxTextWidth.toPx() }
    val fittedFontSize = if (layer.maxLines == 1 && naturalTextWidth > maxTextWidthPx && naturalTextWidth > 0) {
        (requestedFontSize * maxTextWidthPx / naturalTextWidth).coerceAtLeast(6f * contentScale)
    } else {
        requestedFontSize
    }

    Row(
        modifier = if (layer.horizontalAnchor == LayerHorizontalAnchor.LEFT) {
            modifier.padding(horizontal = (2f * contentScale).dp)
        } else {
            modifier
        },
        horizontalArrangement = Arrangement.spacedBy((3.3f * contentScale).dp),
        verticalAlignment = if (layer.maxLines > 1) Alignment.Top else Alignment.CenterVertically
    ) {
        leadingBitmap?.let { bitmap ->
            val ratio = bitmap.width.toFloat() / bitmap.height
            val baseSize = (layer.baseSize * contentScale).coerceIn(2f, 240f)
            val imageModifier = if (ratio >= 1f) {
                Modifier.size(width = baseSize.dp, height = (baseSize / ratio).dp)
            } else {
                Modifier.size(width = (baseSize * ratio).dp, height = baseSize.dp)
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "定位图标",
                modifier = imageModifier,
                contentScale = ContentScale.FillBounds
            )
        }
        Text(
            text = text,
            color = Color(layer.color),
            fontSize = fittedFontSize.sp,
            fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = fontFamily,
            lineHeight = (fittedFontSize * BuiltInTemplateLayout.LOCATION_LINE_HEIGHT_MULTIPLIER).sp,
            letterSpacing = 0.sp,
            maxLines = layer.maxLines,
            softWrap = layer.maxLines > 1,
            overflow = if (layer.maxLines > 1) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = Modifier.widthIn(max = maxTextWidth)
        )
    }
}
