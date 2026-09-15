package com.watermarkcamera.studio

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class TemplatePackageContents(
    val manifest: String,
    val assets: Map<String, ByteArray>
)

object TemplatePackageArchive {
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val MAX_ENTRIES = 128
    private const val MAX_ENTRY_BYTES = 32 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 64 * 1024 * 1024

    fun write(output: OutputStream, manifest: String, assets: Map<String, ByteArray>) {
        require(assets.size + 1 <= MAX_ENTRIES) { "模板资源数量过多" }
        ZipOutputStream(output).use { zip ->
            writeEntry(zip, MANIFEST_ENTRY, manifest.toByteArray(Charsets.UTF_8))
            assets.forEach { (rawName, bytes) ->
                val name = canonicalEntryName(rawName)
                require(name.startsWith("assets/") && name.length > "assets/".length) { "模板资源路径无效" }
                require(bytes.size <= MAX_ENTRY_BYTES) { "模板中的单个资源过大" }
                writeEntry(zip, name, bytes)
            }
        }
    }

    fun read(input: InputStream): TemplatePackageContents {
        var manifest: String? = null
        var totalBytes = 0L
        var entryCount = 0
        val seen = mutableSetOf<String>()
        val assets = linkedMapOf<String, ByteArray>()

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "模板资源数量过多" }
                val name = canonicalEntryName(entry.name)
                require(seen.add(name)) { "模板包含重复资源" }
                require(name == MANIFEST_ENTRY || name.startsWith("assets/")) { "模板包含未知文件" }

                val bytes = readEntry(zip) { count ->
                    totalBytes += count
                    require(totalBytes <= MAX_TOTAL_BYTES) { "模板文件过大" }
                }
                if (name == MANIFEST_ENTRY) {
                    require(manifest == null) { "模板包含重复清单" }
                    manifest = bytes.toString(Charsets.UTF_8)
                } else {
                    assets[name] = bytes
                }
                zip.closeEntry()
            }
        }

        return TemplatePackageContents(
            manifest = requireNotNull(manifest) { "模板缺少 manifest.json" },
            assets = assets
        )
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun readEntry(zip: ZipInputStream, onBytesRead: (Int) -> Unit): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var entryBytes = 0
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            entryBytes += read
            require(entryBytes <= MAX_ENTRY_BYTES) { "模板中的单个资源过大" }
            onBytesRead(read)
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun canonicalEntryName(rawName: String): String {
        require(rawName.isNotBlank() && !rawName.startsWith('/') && !rawName.startsWith('\\')) {
            "模板资源路径无效"
        }
        val normalized = rawName.replace('\\', '/')
        val parts = mutableListOf<String>()
        normalized.split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> throw IllegalArgumentException("模板资源路径无效")
                else -> parts += part
            }
        }
        require(parts.isNotEmpty()) { "模板资源路径无效" }
        return parts.joinToString("/")
    }
}
