package com.watermarkcamera.studio

import android.graphics.Color
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

enum class LayerKind { TEXT, TIME, LOCATION, ICON, IMAGE }

enum class LayerHorizontalAnchor { LEFT, CENTER, RIGHT }

enum class WatermarkFont(val label: String) {
    DEFAULT("默认"),
    CONDENSED("窄体"),
    SERIF("衬线"),
    MONOSPACE("等宽"),
    CURSIVE("手写"),
    CUSTOM("自定义")
}

enum class BuiltInIcon(val label: String, val glyph: String) {
    CAMERA("相机", "CAM"),
    PIN("位置", "PIN"),
    CHECK("打卡", "OK"),
    STAR("收藏", "STAR"),
    DIVIDER("分隔线", "LINE")
}

data class WatermarkLayer(
    val id: String = UUID.randomUUID().toString(),
    val kind: LayerKind,
    val text: String = "",
    val imageUri: String? = null,
    val baseSize: Float = 120f,
    val icon: BuiltInIcon = BuiltInIcon.CAMERA,
    val horizontalAnchor: LayerHorizontalAnchor = LayerHorizontalAnchor.CENTER,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val color: Int = Color.WHITE,
    val fontSize: Float = 18f,
    val bold: Boolean = false,
    val font: WatermarkFont = WatermarkFont.DEFAULT,
    val fontUri: String? = null,
    val fontName: String? = null,
    val maxLines: Int = 1,
    val backgroundColor: Int = 0,
    val locked: Boolean = false,
    val hidden: Boolean = false
) : Serializable {
    fun uniformScale(): Float = sqrt(scaleX.coerceAtLeast(.01f) * scaleY.coerceAtLeast(.01f))

    fun displayText(now: Date = Date()): String = when (kind) {
        LayerKind.TIME -> runCatching {
            SimpleDateFormat(text.ifBlank { "yyyy-MM-dd  HH:mm" }, Locale.getDefault()).format(now)
        }.getOrElse { SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault()).format(now) }
        LayerKind.ICON -> icon.label
        else -> text
    }
}

data class WatermarkTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val layers: List<WatermarkLayer>,
    val updatedAt: Long = System.currentTimeMillis(),
    val category: TemplateCategory = TemplateCategory.CUSTOM,
    val builtIn: Boolean = false
)

data class PhotoTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) : Serializable {
    fun isIdentity(): Boolean = scale == 1f && offsetX == 0f && offsetY == 0f
}

data class DraftProject(
    val sourceUri: String?,
    val layers: List<WatermarkLayer>,
    val photoTransform: PhotoTransform = PhotoTransform()
)

enum class TemplateCategory(val label: String) {
    TIME_LOCATION("时间地点"),
    WORK("工程"),
    INSPECTION("巡检"),
    ATTENDANCE("外勤"),
    LIFE("生活"),
    CUSTOM("我的")
}

data class AppSettings(
    val amapApiKey: String = "",
    val defaultOpacity: Float = 1f,
    val defaultFontSize: Float = 18f,
    val defaultTimeFormat: String = "yyyy-MM-dd  HH:mm",
    val exportQuality: Int = 95,
    val snapToCenter: Boolean = false,
    val showGuides: Boolean = true,
    val darkMode: Boolean = false
)

enum class AppPage { CAMERA, EDITOR, CROP, TEMPLATES, SETTINGS }

enum class CropRatio(val label: String, val value: Float?) {
    FREE("自由", null),
    ORIGINAL("原图", 0f),
    SQUARE("1:1", 1f),
    THREE_FOUR("3:4", 3f / 4f),
    FOUR_THREE("4:3", 4f / 3f),
    NINE_SIXTEEN("9:16", 9f / 16f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}
