package com.watermarkcamera.studio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ProjectStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("watermark_projects", Context.MODE_PRIVATE)

    fun loadTemplates(): List<WatermarkTemplate> {
        val raw = prefs.getString("templates", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { templateFromJson(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun saveTemplate(template: WatermarkTemplate) {
        val items = loadTemplates().toMutableList()
        val index = items.indexOfFirst { it.id == template.id }
        if (index >= 0) items[index] = template.copy(updatedAt = System.currentTimeMillis())
        else items.add(0, template)
        saveTemplates(items)
    }

    fun deleteTemplate(id: String) = saveTemplates(loadTemplates().filterNot { it.id == id })

    fun loadCameraTemplateId(): String? = prefs.getString("cameraTemplateId", null)

    fun saveCameraTemplateId(id: String?) {
        prefs.edit().apply {
            if (id == null) remove("cameraTemplateId") else putString("cameraTemplateId", id)
        }.apply()
    }

    fun saveDraft(sourceUri: String?, layers: List<WatermarkLayer>, photoTransform: PhotoTransform) {
        val json = JSONObject().apply {
            put("sourceUri", sourceUri ?: JSONObject.NULL)
            put("layers", JSONArray().apply { layers.forEach { put(layerToJson(it)) } })
            put("photoTransform", JSONObject().apply {
                put("scale", photoTransform.scale.toDouble())
                put("offsetX", photoTransform.offsetX.toDouble())
                put("offsetY", photoTransform.offsetY.toDouble())
            })
        }
        prefs.edit().putString("draft", json.toString()).apply()
    }

    fun loadDraft(): DraftProject? {
        val raw = prefs.getString("draft", null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val layersJson = json.getJSONArray("layers")
            val layers = List(layersJson.length()) { layerFromJson(layersJson.getJSONObject(it)) }
                .filterNotNull()
            val transformJson = json.optJSONObject("photoTransform")
            DraftProject(
                sourceUri = json.optString("sourceUri").takeUnless { it.isBlank() || it == "null" },
                layers = layers,
                photoTransform = PhotoTransform(
                    scale = transformJson?.optDouble("scale", 1.0)?.toFloat() ?: 1f,
                    offsetX = transformJson?.optDouble("offsetX", 0.0)?.toFloat() ?: 0f,
                    offsetY = transformJson?.optDouble("offsetY", 0.0)?.toFloat() ?: 0f
                )
            )
        }.getOrNull()
    }

    fun clearDraft() = prefs.edit().remove("draft").apply()

    fun loadSettings() = AppSettings(
        amapApiKey = prefs.getString("amapApiKey", "").orEmpty(),
        defaultOpacity = prefs.getFloat("defaultOpacity", 1f),
        defaultFontSize = prefs.getFloat("defaultFontSize", 18f),
        defaultTimeFormat = prefs.getString("defaultTimeFormat", "yyyy-MM-dd  HH:mm").orEmpty(),
        exportQuality = prefs.getInt("exportQuality", 95),
        snapToCenter = prefs.getBoolean("snapToCenter", false),
        showGuides = prefs.getBoolean("showGuides", true),
        darkMode = prefs.getBoolean("darkMode", false)
    )

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("amapApiKey", settings.amapApiKey.trim())
            .putFloat("defaultOpacity", settings.defaultOpacity)
            .putFloat("defaultFontSize", settings.defaultFontSize)
            .putString("defaultTimeFormat", settings.defaultTimeFormat)
            .putInt("exportQuality", settings.exportQuality)
            .putBoolean("snapToCenter", settings.snapToCenter)
            .putBoolean("showGuides", settings.showGuides)
            .putBoolean("darkMode", settings.darkMode)
            .apply()
    }

    fun exportTemplate(template: WatermarkTemplate): String = templateToJson(template)
        .put("format", "watermark-camera-template")
        .put("version", 1)
        .toString(2)

    fun exportTemplatePackage(template: WatermarkTemplate, output: OutputStream) {
        val assets = linkedMapOf<String, ByteArray>()
        val packagedLayers = template.layers.map { layer ->
            var packaged = layer
            layer.imageUri?.let { uriValue ->
                val path = packageAssetPath("images", layer.id, uriValue, "img")
                assets[path] = readUriBytes(uriValue)
                packaged = packaged.copy(imageUri = "asset://$path")
            }
            layer.fontUri?.let { uriValue ->
                val path = packageAssetPath("fonts", layer.id, uriValue, "font")
                assets[path] = readUriBytes(uriValue)
                packaged = packaged.copy(fontUri = "asset://$path")
            }
            packaged
        }
        val manifest = templateToJson(template.copy(layers = packagedLayers))
            .put("format", "watermark-camera-template")
            .put("version", 2)
            .toString(2)
        require(manifest.toByteArray(Charsets.UTF_8).size <= 2 * 1024 * 1024) { "模板清单过大" }
        require(assets.values.sumOf { it.size.toLong() } <= 64L * 1024 * 1024) { "模板文件过大" }
        TemplatePackageArchive.write(output, manifest, assets)
    }

    fun importTemplate(raw: String): WatermarkTemplate {
        val json = JSONObject(raw)
        require(json.optString("format", "watermark-camera-template") == "watermark-camera-template") {
            "不是有效的水印模板文件"
        }
        return templateFromJson(json).copy(
            id = UUID.randomUUID().toString(),
            updatedAt = System.currentTimeMillis(),
            builtIn = false
        )
    }

    fun importTemplate(input: InputStream): WatermarkTemplate {
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input)
        buffered.mark(4)
        val signature = ByteArray(4)
        val signatureSize = buffered.read(signature)
        buffered.reset()
        val isZip = signatureSize >= 2 && signature[0] == 'P'.code.toByte() && signature[1] == 'K'.code.toByte()
        return if (isZip) importTemplatePackage(buffered) else {
            val raw = buffered.reader(Charsets.UTF_8).use { it.readTextLimited(2 * 1024 * 1024) }
            importTemplate(raw)
        }
    }

    private fun importTemplatePackage(input: InputStream): WatermarkTemplate {
        val contents = TemplatePackageArchive.read(input)
        val template = importTemplate(contents.manifest)
        val root = File(appContext.filesDir, "template_assets")
        val temporaryDir = File(root, ".${template.id}.tmp")
        val finalDir = File(root, template.id)
        temporaryDir.deleteRecursively()
        require(temporaryDir.mkdirs()) { "无法创建模板资源目录" }

        return runCatching {
            contents.assets.forEach { (entryName, bytes) ->
                val relative = entryName.removePrefix("assets/")
                val target = File(temporaryDir, relative)
                require(target.canonicalPath.startsWith(temporaryDir.canonicalPath + File.separator)) {
                    "模板资源路径无效"
                }
                val parent = requireNotNull(target.parentFile)
                require(parent.isDirectory || parent.mkdirs()) { "无法创建模板资源目录" }
                target.writeBytes(bytes)
            }
            require(!finalDir.exists() && temporaryDir.renameTo(finalDir)) { "无法保存模板资源" }

            template.copy(layers = template.layers.map { layer ->
                layer.copy(
                    imageUri = resolvePackagedAsset(layer.imageUri, contents.assets, finalDir),
                    fontUri = resolvePackagedAsset(layer.fontUri, contents.assets, finalDir)
                )
            })
        }.getOrElse { error ->
            temporaryDir.deleteRecursively()
            finalDir.deleteRecursively()
            throw error
        }
    }

    private fun resolvePackagedAsset(
        uriValue: String?,
        assets: Map<String, ByteArray>,
        finalDir: File
    ): String? {
        if (uriValue == null || !uriValue.startsWith("asset://")) return uriValue
        val entryName = uriValue.removePrefix("asset://")
        require(assets.containsKey(entryName)) { "模板缺少资源：$entryName" }
        val relative = entryName.removePrefix("assets/")
        val file = File(finalDir, relative)
        require(file.isFile) { "模板资源保存失败：$entryName" }
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        ).toString()
    }

    private fun packageAssetPath(group: String, layerId: String, uriValue: String, fallback: String): String {
        val extension = resolveExtension(Uri.parse(uriValue)).ifBlank { fallback }
        val safeId = layerId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "assets/$group/$safeId.$extension"
    }

    private fun resolveExtension(uri: Uri): String {
        val mimeExtension = appContext.contentResolver.getType(uri)
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        val displayName = runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
        val nameExtension = displayName?.substringAfterLast('.', "")
            ?: uri.lastPathSegment?.substringAfterLast('.', "").orEmpty()
        return sequenceOf(mimeExtension, nameExtension)
            .filterNotNull()
            .map { it.lowercase().filter(Char::isLetterOrDigit).take(8) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }

    private fun readUriBytes(uriValue: String): ByteArray {
        val input = appContext.contentResolver.openInputStream(Uri.parse(uriValue))
            ?: error("无法读取模板资源")
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                total += read
                require(total <= 32 * 1024 * 1024) { "模板中的单个资源过大" }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }

    private fun java.io.Reader.readTextLimited(maxChars: Int): String {
        val output = StringBuilder()
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (read == 0) continue
            require(output.length + read <= maxChars) { "模板文件过大" }
            output.append(buffer, 0, read)
        }
        return output.toString()
    }

    private fun saveTemplates(items: List<WatermarkTemplate>) {
        val array = JSONArray().apply { items.forEach { put(templateToJson(it)) } }
        prefs.edit().putString("templates", array.toString()).apply()
    }

    private fun templateToJson(template: WatermarkTemplate) = JSONObject().apply {
        put("id", template.id)
        put("name", template.name)
        put("updatedAt", template.updatedAt)
        put("category", template.category.name)
        put("builtIn", template.builtIn)
        put("layers", JSONArray().apply { template.layers.forEach { put(layerToJson(it)) } })
    }

    private fun templateFromJson(json: JSONObject): WatermarkTemplate {
        val layersJson = json.getJSONArray("layers")
        return WatermarkTemplate(
            id = json.getString("id"),
            name = json.getString("name"),
            updatedAt = json.optLong("updatedAt", 0L),
            layers = List(layersJson.length()) { layerFromJson(layersJson.getJSONObject(it)) }
                .filterNotNull(),
            category = runCatching { TemplateCategory.valueOf(json.optString("category")) }
                .getOrDefault(TemplateCategory.CUSTOM),
            builtIn = json.optBoolean("builtIn", false)
        )
    }

    private fun layerToJson(layer: WatermarkLayer) = JSONObject().apply {
        put("id", layer.id)
        put("kind", layer.kind.name)
        put("text", layer.text)
        put("imageUri", layer.imageUri ?: JSONObject.NULL)
        put("baseSize", layer.baseSize.toDouble())
        put("icon", layer.icon.name)
        put("horizontalAnchor", layer.horizontalAnchor.name)
        put("x", layer.x.toDouble())
        put("y", layer.y.toDouble())
        put("scaleX", layer.scaleX.toDouble())
        put("scaleY", layer.scaleY.toDouble())
        put("rotation", layer.rotation.toDouble())
        put("alpha", layer.alpha.toDouble())
        put("color", layer.color)
        put("fontSize", layer.fontSize.toDouble())
        put("bold", layer.bold)
        put("font", layer.font.name)
        put("fontUri", layer.fontUri ?: JSONObject.NULL)
        put("fontName", layer.fontName ?: JSONObject.NULL)
        put("maxLines", layer.maxLines)
        put("backgroundColor", layer.backgroundColor)
        put("locked", layer.locked)
        put("hidden", layer.hidden)
    }

    private fun layerFromJson(json: JSONObject): WatermarkLayer? {
        val kind = runCatching { LayerKind.valueOf(json.getString("kind")) }.getOrNull() ?: return null
        return WatermarkLayer(
        id = json.getString("id"),
        kind = kind,
        text = json.optString("text"),
        imageUri = json.optString("imageUri").takeUnless { it.isBlank() || it == "null" },
        baseSize = json.optDouble("baseSize", 120.0).toFloat(),
        icon = runCatching { BuiltInIcon.valueOf(json.optString("icon")) }.getOrDefault(BuiltInIcon.CAMERA),
        horizontalAnchor = runCatching {
            LayerHorizontalAnchor.valueOf(json.optString("horizontalAnchor"))
        }.getOrDefault(LayerHorizontalAnchor.CENTER),
        x = json.optDouble("x", 0.5).toFloat(),
        y = json.optDouble("y", 0.5).toFloat(),
        scaleX = json.optDouble("scaleX", json.optDouble("scale", 1.0)).toFloat(),
        scaleY = json.optDouble("scaleY", json.optDouble("scale", 1.0)).toFloat(),
        rotation = json.optDouble("rotation", 0.0).toFloat(),
        alpha = json.optDouble("alpha", 1.0).toFloat(),
        color = json.optInt("color", android.graphics.Color.WHITE),
        fontSize = json.optDouble("fontSize", 18.0).toFloat(),
        bold = json.optBoolean("bold", false),
        font = runCatching { WatermarkFont.valueOf(json.optString("font")) }.getOrDefault(WatermarkFont.DEFAULT),
        fontUri = json.optString("fontUri").takeUnless { it.isBlank() || it == "null" },
        fontName = json.optString("fontName").takeUnless { it.isBlank() || it == "null" },
        maxLines = json.optInt("maxLines", 1).coerceAtLeast(1),
        backgroundColor = json.optInt("backgroundColor", 0),
        locked = json.optBoolean("locked", false),
        hidden = json.optBoolean("hidden", false)
        )
    }
}
