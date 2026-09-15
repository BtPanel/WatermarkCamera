package com.watermarkcamera.studio

object TextLineBreaker {
    fun breakLines(
        text: String,
        maxWidth: Float,
        maxLines: Int,
        measureWidth: (String) -> Float
    ): List<String> {
        if (text.isEmpty()) return listOf("")
        if (maxWidth <= 0f || maxLines <= 1 || measureWidth(text) <= maxWidth) return listOf(text)

        val lines = mutableListOf<String>()
        var start = 0
        while (start < text.length && lines.size < maxLines) {
            if (lines.size == maxLines - 1) {
                lines += fitLastLine(text.substring(start), maxWidth, measureWidth)
                break
            }

            var end = start + 1
            var lastFit = start
            while (end <= text.length && measureWidth(text.substring(start, end)) <= maxWidth) {
                lastFit = end
                end++
            }
            if (lastFit == start) lastFit = start + 1
            lines += text.substring(start, lastFit)
            start = lastFit
        }
        return lines
    }

    private fun fitLastLine(text: String, maxWidth: Float, measureWidth: (String) -> Float): String {
        if (measureWidth(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0 && measureWidth(text.substring(0, end) + ellipsis) > maxWidth) end--
        return text.substring(0, end) + ellipsis
    }
}
