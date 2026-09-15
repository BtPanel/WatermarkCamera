package com.watermarkcamera.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextLineBreakerTest {
    @Test
    fun `long chinese location wraps without changing its content`() {
        val text = "上海市浦东新区世纪大道一百号环球金融中心"

        val lines = TextLineBreaker.breakLines(
            text = text,
            maxWidth = 10f,
            maxLines = 3,
            measureWidth = { it.length.toFloat() }
        )

        assertEquals(text, lines.joinToString(""))
        assertEquals(2, lines.size)
        assertTrue(lines.all { it.length <= 10 })
    }

    @Test
    fun `short location remains on one line`() {
        val lines = TextLineBreaker.breakLines(
            text = "上海市浦东新区",
            maxWidth = 20f,
            maxLines = 3,
            measureWidth = { it.length.toFloat() }
        )

        assertEquals(listOf("上海市浦东新区"), lines)
    }
}
