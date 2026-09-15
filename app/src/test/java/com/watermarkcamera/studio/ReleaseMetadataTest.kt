package com.watermarkcamera.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseMetadataTest {
    @Test
    fun `release metadata is version one point zero`() {
        assertEquals("1.0", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)
    }
}
