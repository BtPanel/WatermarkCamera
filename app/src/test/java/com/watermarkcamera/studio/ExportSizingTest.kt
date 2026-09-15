package com.watermarkcamera.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportSizingTest {
    @Test
    fun `original aspect keeps the imported image dimensions`() {
        assertEquals(
            ExportDimensions(4000, 3000),
            originalExportDimensions(4000, 3000, ExportAspect.ORIGINAL)
        )
    }

    @Test
    fun `portrait export dimensions remain locked to nine by sixteen`() {
        assertEquals(
            ExportDimensions(1080, 1920),
            exportDimensionsFromWidth(1080, ExportAspect.PORTRAIT_9_16.ratioFor(4000, 3000))
        )
        assertEquals(
            ExportDimensions(1080, 1920),
            exportDimensionsFromHeight(1920, ExportAspect.PORTRAIT_9_16.ratioFor(4000, 3000))
        )
    }

    @Test
    fun `four k preset uses the short edge and selected ratio`() {
        assertEquals(
            ExportDimensions(3840, 2160),
            exportDimensionsForShortEdge(2160, ExportAspect.LANDSCAPE_16_9.ratioFor(4000, 3000))
        )
    }

    @Test
    fun `aspect change uses the largest centered crop without upscaling`() {
        assertEquals(
            ExportDimensions(1688, 3000),
            originalExportDimensions(4000, 3000, ExportAspect.PORTRAIT_9_16)
        )
        assertEquals(
            ExportCropRect(left = 1156, top = 0, width = 1688, height = 3000),
            centerExportCrop(4000, 3000, ExportAspect.PORTRAIT_9_16.ratioFor(4000, 3000))
        )
    }

    @Test
    fun `unsafe export dimensions are rejected`() {
        assertTrue(isValidExportDimensions(ExportDimensions(7680, 4320)))
        assertFalse(isValidExportDimensions(ExportDimensions(9000, 4320)))
        assertFalse(isValidExportDimensions(ExportDimensions(8000, 6000)))
        assertFalse(isValidExportDimensions(ExportDimensions(0, 1080)))
    }

    @Test
    fun `locked width edit recalculates height`() {
        assertEquals(
            ExportDimensions(1080, 1920),
            exportDimensionsAfterWidthEdit(
                width = 1080,
                currentHeight = 1440,
                ratio = 9f / 16f,
                ratioLocked = true
            )
        )
    }

    @Test
    fun `unlocked width edit preserves independently entered height`() {
        assertEquals(
            ExportDimensions(1080, 1440),
            exportDimensionsAfterWidthEdit(
                width = 1080,
                currentHeight = 1440,
                ratio = 9f / 16f,
                ratioLocked = false
            )
        )
        assertEquals(
            ExportDimensions(1080, 1600),
            exportDimensionsAfterHeightEdit(
                currentWidth = 1080,
                height = 1600,
                ratio = 9f / 16f,
                ratioLocked = false
            )
        )
    }
}
