package com.watermarkcamera.studio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TemplatePackageArchiveTest {
    @Test
    fun `package round trip preserves manifest and assets`() {
        val output = ByteArrayOutputStream()
        val assets = linkedMapOf(
            "assets/images/logo.png" to byteArrayOf(1, 2, 3),
            "assets/fonts/custom.ttf" to byteArrayOf(4, 5, 6, 7)
        )

        TemplatePackageArchive.write(output, "{\"version\":2}", assets)
        val restored = TemplatePackageArchive.read(ByteArrayInputStream(output.toByteArray()))

        assertEquals("{\"version\":2}", restored.manifest)
        assertEquals(assets.keys, restored.assets.keys)
        assets.forEach { (name, bytes) -> assertArrayEquals(bytes, restored.assets.getValue(name)) }
    }

    @Test
    fun `package rejects entries outside archive root`() {
        val bytes = zipWithEntry("../outside.ttf", byteArrayOf(1))

        assertThrows(IllegalArgumentException::class.java) {
            TemplatePackageArchive.read(ByteArrayInputStream(bytes))
        }
    }

    @Test
    fun `package rejects duplicate entries`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("assets/logo.png"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("assets/./logo.png"))
            zip.write(byteArrayOf(2))
            zip.closeEntry()
        }

        assertThrows(IllegalArgumentException::class.java) {
            TemplatePackageArchive.read(ByteArrayInputStream(output.toByteArray()))
        }
    }

    @Test
    fun `package requires manifest`() {
        val bytes = zipWithEntry("assets/logo.png", byteArrayOf(1))

        assertThrows(IllegalArgumentException::class.java) {
            TemplatePackageArchive.read(ByteArrayInputStream(bytes))
        }
    }

    private fun zipWithEntry(name: String, bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
