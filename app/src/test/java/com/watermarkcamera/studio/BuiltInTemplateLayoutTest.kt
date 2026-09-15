package com.watermarkcamera.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInTemplateLayoutTest {
    @Test
    fun `dingtalk template is returned without any layout changes`() {
        val layers = listOf(
            WatermarkLayer(id = "logo", kind = LayerKind.IMAGE, x = .12f, y = .08f),
            WatermarkLayer(id = "name", kind = LayerKind.TEXT, text = "张三", x = .96f, y = .94f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_dingtalk_checkin", layers, 1f)

        assertSame(layers, arranged)
    }

    @Test
    fun `text rows share a compact left aligned column`() {
        val layers = listOf(
            WatermarkLayer(id = "title", kind = LayerKind.TEXT, text = "工程记录", x = .4f, y = .68f, fontSize = 20f, bold = true),
            WatermarkLayer(id = "project", kind = LayerKind.TEXT, text = "项目", x = .2f, y = .77f, fontSize = 13f),
            WatermarkLayer(id = "time", kind = LayerKind.TIME, text = "yyyy-MM-dd", x = .3f, y = .92f, fontSize = 11f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_site_log", layers)
        val rows = arranged.sortedBy { it.y }

        rows.forEach {
            assertEquals(.09f, it.x, .0001f)
            assertEquals(LayerHorizontalAnchor.LEFT, it.horizontalAnchor)
        }
        assertEquals(.68f, rows.first().y, .0001f)
        assertTrue(rows[1].y - rows[0].y < .055f)
        assertTrue(rows[2].y - rows[1].y < .055f)
        assertTrue(rows.zipWithNext().all { (upper, lower) ->
            val requiredGap = (upper.fontSize / 2f + lower.fontSize / 2f + 4f) / 640f
            lower.y - upper.y >= requiredGap
        })
    }

    @Test
    fun `leading icon aligns with first row and scales to its text`() {
        val layers = listOf(
            WatermarkLayer(id = "check", kind = LayerKind.ICON, icon = BuiltInIcon.CHECK, x = .09f, y = .72f),
            WatermarkLayer(id = "title", kind = LayerKind.TEXT, text = "安全检查", x = .34f, y = .72f, fontSize = 20f, bold = true),
            WatermarkLayer(id = "result", kind = LayerKind.TEXT, text = "检查结果", x = .27f, y = .79f, fontSize = 14f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_safety_check", layers)
        val icon = arranged.single { it.kind == LayerKind.ICON }
        val title = arranged.single { it.id == "title" }
        val renderedIconSize = 48f * icon.uniformScale()

        assertEquals(.17f, title.x, .0001f)
        assertEquals(title.y, icon.y, .0001f)
        assertTrue(icon.x < title.x)
        assertTrue(renderedIconSize >= title.fontSize)
        assertTrue(renderedIconSize <= title.fontSize * 1.2f)
        assertEquals(icon.scaleX, icon.scaleY, .0001f)
    }

    @Test
    fun `icon follows the nearest semantic row`() {
        val layers = listOf(
            WatermarkLayer(id = "pin", kind = LayerKind.ICON, icon = BuiltInIcon.PIN, y = .93f),
            WatermarkLayer(id = "time", kind = LayerKind.TIME, text = "12:00", y = .80f, fontSize = 12f),
            WatermarkLayer(id = "place", kind = LayerKind.LOCATION, text = "上海市浦东新区", y = .93f, fontSize = 15f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_geo_note", layers)
        val icon = arranged.single { it.kind == LayerKind.ICON }
        val place = arranged.single { it.id == "place" }

        assertEquals(place.y, icon.y, .0001f)
        assertEquals(place.fontSize * 1.1f, 48f * icon.uniformScale(), .0001f)
    }

    @Test
    fun `row spacing stays visually stable across camera aspect ratios`() {
        val layers = listOf(
            WatermarkLayer(id = "title", kind = LayerKind.TEXT, text = "巡检记录", y = .70f, fontSize = 20f, bold = true),
            WatermarkLayer(id = "result", kind = LayerKind.TEXT, text = "检查结果：正常", y = .82f, fontSize = 13f),
            WatermarkLayer(id = "place", kind = LayerKind.LOCATION, text = "检查地点", y = .96f, fontSize = 11f)
        )
        val sixteenNine = BuiltInTemplateLayout.arrange("builtin_test", layers, 9f / 16f)
        val square = BuiltInTemplateLayout.arrange("builtin_test", layers, 1f)

        val sixteenNineGapInCanvasWidths = (sixteenNine[1].y - sixteenNine[0].y) / (9f / 16f)
        val squareGapInCanvasWidths = square[1].y - square[0].y

        assertEquals(sixteenNineGapInCanvasWidths, squareGapInCanvasWidths, .0001f)
        assertTrue(square[2].y <= .95f)
    }

    @Test
    fun `compact preview expands the lower watermark area but leaves dingtalk unchanged`() {
        assertEquals(.82f, BuiltInTemplateLayout.previewY("builtin_dingtalk_checkin", .82f), .0001f)

        val compactGap = BuiltInTemplateLayout.previewY("builtin_test", .86f) -
            BuiltInTemplateLayout.previewY("builtin_test", .82f)

        assertTrue(compactGap > .04f)
    }

    @Test
    fun `location is placed after other rows so wrapped text cannot cover them`() {
        val layers = listOf(
            WatermarkLayer(id = "title", kind = LayerKind.TEXT, text = "工程记录", y = .70f, fontSize = 20f),
            WatermarkLayer(id = "place", kind = LayerKind.LOCATION, text = "地点：很长的地址", y = .82f, fontSize = 12f),
            WatermarkLayer(id = "owner", kind = LayerKind.TEXT, text = "负责人：张三", y = .90f, fontSize = 12f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_site_log", layers)

        assertTrue(arranged.single { it.id == "place" }.y > arranged.single { it.id == "owner" }.y)
    }

    @Test
    fun `regular rows keep a slightly wider visual gap`() {
        val layers = listOf(
            WatermarkLayer(id = "time", kind = LayerKind.TIME, text = "2026-09-09 09:53", y = .80f, fontSize = 12f),
            WatermarkLayer(id = "place", kind = LayerKind.LOCATION, text = "地点：上海市黄浦区", y = .90f, fontSize = 12f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_site_log", layers)
        val centerGapDp = (arranged[1].y - arranged[0].y) /
            BuiltInTemplateLayout.DEFAULT_PORTRAIT_ASPECT * 360f
        val contentHalfHeightsDp = layers[0].fontSize / 2f + layers[1].fontSize / 2f

        assertTrue(centerGapDp - contentHalfHeightsDp >= 7f)
    }

    @Test
    fun `construction location matches the body text size while title stays unchanged`() {
        val layers = listOf(
            WatermarkLayer(id = "title", kind = LayerKind.TEXT, text = "施工现场", y = .73f, fontSize = 19f, bold = true),
            WatermarkLayer(id = "unit", kind = LayerKind.TEXT, text = "施工单位", y = .80f, fontSize = 13f),
            WatermarkLayer(id = "section", kind = LayerKind.TEXT, text = "施工部位", y = .85f, fontSize = 13f),
            WatermarkLayer(id = "place", kind = LayerKind.LOCATION, text = "地点：上海市黄浦区", y = .95f, fontSize = 11f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_build_day", layers)

        assertEquals(19f, arranged.single { it.id == "title" }.fontSize, .0001f)
        assertEquals(13f, arranged.single { it.id == "unit" }.fontSize, .0001f)
        assertEquals(13f, arranged.single { it.id == "section" }.fontSize, .0001f)
        assertEquals(13f, arranged.single { it.id == "place" }.fontSize, .0001f)
    }

    @Test
    fun `location follows nearby body size in every regular built in template`() {
        val layers = listOf(
            WatermarkLayer(id = "title", kind = LayerKind.TEXT, text = "工程记录", y = .69f, fontSize = 22f, bold = true),
            WatermarkLayer(id = "project", kind = LayerKind.TEXT, text = "项目", y = .76f, fontSize = 14f, bold = true),
            WatermarkLayer(id = "time", kind = LayerKind.TIME, text = "2026-09-09", y = .82f, fontSize = 13f),
            WatermarkLayer(id = "owner", kind = LayerKind.TEXT, text = "负责人", y = .92f, fontSize = 12f),
            WatermarkLayer(id = "place", kind = LayerKind.LOCATION, text = "地点：上海市黄浦区", y = .95f, fontSize = 11f)
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_site_log", layers)

        assertEquals(22f, arranged.single { it.id == "title" }.fontSize, .0001f)
        assertEquals(13f, arranged.single { it.id == "place" }.fontSize, .0001f)
    }

    @Test
    fun `three line location reserves its full rendered height`() {
        val layers = listOf(
            WatermarkLayer(id = "time", kind = LayerKind.TIME, text = "2026-09-09 09:53", y = .80f, fontSize = 12f),
            WatermarkLayer(
                id = "place",
                kind = LayerKind.LOCATION,
                text = "地点：上海市浦东新区世纪大道陆家嘴金融贸易区附近某某产业园区第一栋第二单元靠近地铁站出口，继续向东经过城市广场到达第三办公区南门",
                y = .90f,
                fontSize = 12f,
                maxLines = 3
            )
        )

        val arranged = BuiltInTemplateLayout.arrange("builtin_site_log", layers)
        val centerGapDp = (arranged[1].y - arranged[0].y) /
            BuiltInTemplateLayout.DEFAULT_PORTRAIT_ASPECT * 360f

        assertTrue(centerGapDp >= 31.5f)
    }
}
