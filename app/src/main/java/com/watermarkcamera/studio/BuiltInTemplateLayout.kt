package com.watermarkcamera.studio

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

object BuiltInTemplateLayout {
    const val DINGTALK_TEMPLATE_ID = "builtin_dingtalk_checkin"
    const val DEFAULT_PORTRAIT_ASPECT = 3f / 4f

    private const val REFERENCE_WIDTH_DP = 360f
    private const val ICON_BASE_SIZE_DP = 48f
    private const val REGULAR_ROW_GAP_DP = 7f
    private const val TITLE_ROW_GAP_DP = 7f
    private const val LOCATION_CHARACTERS_PER_LINE = 22f
    const val LOCATION_LINE_HEIGHT_MULTIPLIER = 1.15f
    private val textKinds = setOf(LayerKind.TEXT, LayerKind.TIME, LayerKind.LOCATION)

    fun arrange(
        templateId: String,
        layers: List<WatermarkLayer>,
        portraitAspectRatio: Float = DEFAULT_PORTRAIT_ASPECT
    ): List<WatermarkLayer> {
        if (templateId == DINGTALK_TEMPLATE_ID) return layers

        val normalizedLayers = normalizeLocationTextSize(layers)
        val orderedRows = normalizedLayers
            .filter { it.kind in textKinds }
            .sortedWith(compareBy<WatermarkLayer> { it.kind == LayerKind.LOCATION }.thenBy { it.y })
        if (orderedRows.isEmpty()) return layers

        val aspect = portraitAspectRatio.coerceIn(.5f, 1f)
        val hasLeadingIcon = normalizedLayers.any { it.kind == LayerKind.ICON }
        val textColumnX = if (hasLeadingIcon) .17f else .09f
        val rowY = LinkedHashMap<String, Float>(orderedRows.size)
        var currentY = orderedRows.first().y
        rowY[orderedRows.first().id] = currentY

        orderedRows.zipWithNext().forEachIndexed { index, (upper, lower) ->
            val extraGapDp = if (index == 0 && upper.bold) TITLE_ROW_GAP_DP else REGULAR_ROW_GAP_DP
            val centerGapDp = rowHeightDp(upper) / 2f + rowHeightDp(lower) / 2f + extraGapDp
            currentY += centerGapDp / REFERENCE_WIDTH_DP * aspect
            rowY[lower.id] = currentY
        }

        val lastRow = orderedRows.last()
        val bottomEdge = rowY.getValue(lastRow.id) + rowHeightDp(lastRow) / 2f / REFERENCE_WIDTH_DP * aspect
        val upwardShift = max(0f, bottomEdge - .95f)
        val iconTargets = normalizedLayers.filter { it.kind == LayerKind.ICON }.associate { icon ->
            icon.id to orderedRows.minBy { row -> abs(row.y - icon.y) }
        }

        return normalizedLayers.map { layer ->
            when {
                layer.kind in textKinds -> layer.copy(
                    x = textColumnX,
                    y = rowY.getValue(layer.id) - upwardShift,
                    horizontalAnchor = LayerHorizontalAnchor.LEFT,
                    maxLines = if (layer.kind == LayerKind.LOCATION) max(3, layer.maxLines) else layer.maxLines
                )
                layer.kind == LayerKind.ICON -> {
                    val target = iconTargets.getValue(layer.id)
                    val iconScale = (target.fontSize * 1.1f / ICON_BASE_SIZE_DP).coerceIn(.22f, .55f)
                    layer.copy(
                        x = .10f,
                        y = rowY.getValue(target.id) - upwardShift,
                        scaleX = iconScale,
                        scaleY = iconScale
                    )
                }
                else -> layer
            }
        }
    }

    fun previewY(templateId: String, layerY: Float): Float {
        if (templateId == DINGTALK_TEMPLATE_ID) return layerY
        return ((layerY - .55f) / .45f).coerceIn(.06f, .94f)
    }

    private fun rowHeightDp(layer: WatermarkLayer): Float {
        if (layer.kind != LayerKind.LOCATION) return layer.fontSize

        val estimatedLines = ceil(layer.text.length / LOCATION_CHARACTERS_PER_LINE)
            .toInt()
            .coerceIn(1, max(1, layer.maxLines))
        return layer.fontSize * estimatedLines * LOCATION_LINE_HEIGHT_MULTIPLIER
    }

    private fun normalizeLocationTextSize(layers: List<WatermarkLayer>): List<WatermarkLayer> {
        val bodyFontSize = layers
            .filter { it.kind in textKinds && it.kind != LayerKind.LOCATION && !it.bold }
            .maxOfOrNull { it.fontSize }
            ?: return layers
        return layers.map { layer ->
            if (layer.kind == LayerKind.LOCATION) layer.copy(fontSize = bodyFontSize) else layer
        }
    }
}
