package com.watermarkcamera.studio

import kotlin.math.abs

fun resizeWatermarkLayer(
    base: WatermarkLayer,
    widthDelta: Float,
    heightDelta: Float,
    preserveAspect: Boolean
): WatermarkLayer {
    val minScale = if (base.kind == LayerKind.IMAGE) .05f else .15f
    if (preserveAspect) {
        val scaleDelta = if (abs(widthDelta) >= abs(heightDelta)) widthDelta else heightDelta
        val currentScale = base.uniformScale()
        val nextScale = (currentScale + scaleDelta * 3f).coerceIn(minScale, 6f)
        val ratio = nextScale / currentScale.coerceAtLeast(.01f)
        return base.copy(
            scaleX = (base.scaleX * ratio).coerceIn(minScale, 6f),
            scaleY = (base.scaleY * ratio).coerceIn(minScale, 6f)
        )
    }
    return base.copy(
        scaleX = if (widthDelta != 0f) (base.scaleX + widthDelta * 3f).coerceIn(minScale, 6f) else base.scaleX,
        scaleY = if (heightDelta != 0f) (base.scaleY + heightDelta * 3f).coerceIn(minScale, 6f) else base.scaleY
    )
}
