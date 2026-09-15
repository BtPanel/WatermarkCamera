package com.watermarkcamera.studio

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconResourcesTest {
    private val resources = File("src/main/res")

    @Test
    fun `all launcher icon resources use the approved signal red palette`() {
        val iconFiles = listOf(
            resources.resolve("drawable/app_icon.xml"),
            resources.resolve("drawable/ic_launcher_foreground.xml"),
            resources.resolve("mipmap-anydpi/ic_launcher.xml"),
            resources.resolve("values/colors.xml"),
        )
        val combined = iconFiles.joinToString("\n") { it.readText() }

        assertTrue(combined.contains("#E43D36"))
        assertTrue(combined.contains("#FFFFFF"))
        assertTrue(combined.contains("#FFC247"))
        listOf("#0B6158", "#074C45", "#72D9C1", "#5FE0C0").forEach { oldColor ->
            assertFalse("old launcher color remains: $oldColor", combined.contains(oldColor))
        }
    }

    @Test
    fun `adaptive and legacy icons contain the coordinate cut geometry`() {
        val foreground = resources.resolve("drawable/ic_launcher_foreground.xml").readText()
        val legacy = resources.resolve("mipmap-anydpi/ic_launcher.xml").readText()
        val appIcon = resources.resolve("drawable/app_icon.xml").readText()

        listOf(foreground, legacy, appIcon).forEach { icon ->
            assertTrue(icon.contains("android:fillType=\"evenOdd\""))
            assertTrue(icon.contains("L64,88"))
            assertTrue(icon.contains("A5,5"))
        }
    }

    @Test
    fun `adaptive foreground renders at about sixty percent of the icon`() {
        val foreground = resources.resolve("drawable/ic_launcher_foreground.xml").readText()
        val scale = Regex("""android:scaleX="([^"]+)"""")
            .find(foreground)!!
            .groupValues[1]
            .toDouble()
        val translate = Regex("""android:translateX="([^"]+)"""")
            .find(foreground)!!
            .groupValues[1]
            .toDouble()

        // Calibrated from the emulator: scale 0.70 rendered as 90 px inside a 124 px icon.
        val launcherScaleFactor = (90.0 / 124.0) / 0.70
        val renderedRatio = scale * launcherScaleFactor

        assertTrue("rendered ratio was $renderedRatio", renderedRatio in 0.58..0.62)
        assertEquals(54.0, scale * 50.0 + translate, 0.01)
        assertTrue(foreground.contains("android:scaleY=\"$scale\""))
        assertTrue(foreground.contains("android:translateY=\"$translate\""))
    }
}
