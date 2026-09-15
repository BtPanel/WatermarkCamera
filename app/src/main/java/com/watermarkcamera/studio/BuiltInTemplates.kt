package com.watermarkcamera.studio

import android.graphics.Color

object BuiltInTemplates {
    private val white = Color.WHITE
    private val mint = Color.rgb(76, 220, 190)
    private val yellow = Color.rgb(255, 213, 79)
    private val red = Color.rgb(255, 112, 112)
    private val cyan = Color.rgb(112, 205, 255)
    private const val DINGTALK_LOGO =
        "android.resource://com.watermarkcamera.studio/drawable/dingtalk_camera_logo"
    private const val DINGTALK_LOCATION =
        "android.resource://com.watermarkcamera.studio/drawable/dingtalk_camera_location"
    private const val DINGTALK_USER =
        "android.resource://com.watermarkcamera.studio/drawable/dingtalk_camera_user"
    private const val DINGTALK_DIVIDER =
        "android.resource://com.watermarkcamera.studio/drawable/dingtalk_camera_divider"

    val all: List<WatermarkTemplate> = listOf(
        template("builtin_minimal_time", "极简时间", TemplateCategory.TIME_LOCATION,
            time("date", "yyyy.MM.dd", .20f, .82f, 25f, white, true),
            time("clock", "HH:mm", .15f, .88f, 17f, mint, true),
            location("place", "点击设置位置", .26f, .93f, 12f, white)
        ),
        template("builtin_geo_note", "地点记录", TemplateCategory.TIME_LOCATION,
            icon("pin", BuiltInIcon.PIN, .09f, .93f, mint),
            time("time", "yyyy-MM-dd  HH:mm:ss", .28f, .82f, 12f, white),
            text("coord", "经纬度：点击修改", .24f, .87f, 11f, cyan),
            location("place", "点击设置详细地址", .38f, .93f, 15f, white, true)
        ),
        template("builtin_site_log", "工程日志", TemplateCategory.WORK,
            text("title", "工程记录", .18f, .69f, 22f, yellow, true),
            text("project", "项目：点击修改", .25f, .76f, 14f, white, true),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .82f, 13f, white),
            location("place", "地点：点击设置", .25f, .87f, 12f, white),
            text("owner", "负责人：点击修改", .25f, .92f, 12f, mint)
        ),
        template("builtin_build_day", "施工记录", TemplateCategory.WORK,
            icon("camera", BuiltInIcon.CAMERA, .09f, .73f, yellow),
            text("title", "施工现场", .34f, .73f, 19f, white, true),
            text("unit", "施工单位：点击修改", .27f, .80f, 13f, white),
            text("section", "施工部位：点击修改", .27f, .85f, 13f, white),
            time("time", "yyyy-MM-dd  HH:mm", .26f, .90f, 12f, yellow),
            location("place", "点击设置位置", .26f, .95f, 13f, white)
        ),
        template("builtin_safety_check", "安全检查", TemplateCategory.INSPECTION,
            icon("check", BuiltInIcon.CHECK, .09f, .72f, mint),
            text("title", "安全检查", .34f, .72f, 20f, white, true),
            text("result", "检查结果：正常", .27f, .79f, 14f, mint, true),
            text("inspector", "检查人：点击修改", .27f, .85f, 12f, white),
            time("time", "yyyy-MM-dd  HH:mm", .26f, .90f, 12f, white),
            location("place", "点击设置检查地点", .27f, .95f, 11f, white)
        ),
        template("builtin_equipment_check", "设备巡检", TemplateCategory.INSPECTION,
            text("title", "设备巡检记录", .25f, .70f, 20f, cyan, true),
            text("device", "设备编号：点击修改", .27f, .77f, 13f, white),
            text("status", "运行状态：正常", .23f, .82f, 13f, mint, true),
            text("person", "巡检人员：点击修改", .27f, .87f, 12f, white),
            time("time", "yyyy-MM-dd  HH:mm:ss", .27f, .92f, 11f, white)
        ),
        template("builtin_field_clock", "外勤打卡", TemplateCategory.ATTENDANCE,
            icon("check", BuiltInIcon.CHECK, .09f, .78f, mint),
            text("title", "外勤打卡", .34f, .78f, 21f, white, true),
            time("time", "HH:mm:ss", .20f, .85f, 18f, yellow, true),
            time("date", "yyyy年MM月dd日", .25f, .90f, 12f, white),
            location("place", "点击设置打卡地点", .28f, .95f, 11f, white)
        ),
        template("builtin_dingtalk_checkin", "钉钉签到", TemplateCategory.ATTENDANCE,
            image("divider", DINGTALK_DIVIDER, dingX(50.5f), dingY(119f), dingSize(179f)),
            time("clock", "HH:mm", dingX(162f), dingY(68f), 22.2f, white, font = WatermarkFont.CONDENSED),
            time("date", "yyyy.MM.dd", dingX(176f), dingY(140f), 11.17f, white),
            time("weekday", "EEEE", dingX(334f), dingY(139.5f), 11f, white),
            image("dingtalk_logo", DINGTALK_LOGO, dingX(142.5f), dingY(187.5f), dingSize(119.2f)),
            image("user_icon", DINGTALK_USER, dingX(1048.5f), dingY(2112f), dingSize(51.2f)),
            text("user_name", "张三", dingX(1153f), dingY(2111.5f), 10.5f, white)
                .copy(horizontalAnchor = LayerHorizontalAnchor.RIGHT),
            location("place", "上海环球金融中心 · 上海市", dingX(1153f), dingY(2174.5f), 11.15f, white)
                .copy(
                    imageUri = DINGTALK_LOCATION,
                    baseSize = dingSize(51.2f),
                    horizontalAnchor = LayerHorizontalAnchor.RIGHT
                )
        ),
        template("builtin_visit", "到访记录", TemplateCategory.ATTENDANCE,
            text("title", "客户到访", .20f, .74f, 20f, cyan, true),
            text("customer", "客户：点击修改", .24f, .81f, 13f, white),
            text("visitor", "拜访人：点击修改", .25f, .86f, 13f, white),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .91f, 12f, yellow),
            location("place", "点击设置到访地址", .27f, .96f, 11f, white)
        ),
        template("builtin_weather_geo", "天气经纬度", TemplateCategory.TIME_LOCATION,
            time("time", "yyyy-MM-dd  HH:mm:ss", .28f, .76f, 15f, white, true),
            text("weather", "天气：点击修改", .22f, .82f, 12f, cyan),
            text("altitude", "海拔：等待定位", .22f, .87f, 11f, mint),
            text("coord", "经纬度：等待定位", .25f, .92f, 10f, white),
            location("place", "点击设置详细地址", .31f, .96f, 12f, white)
        ),
        template("builtin_progress", "施工进度", TemplateCategory.WORK,
            text("title", "施工进度记录", .25f, .66f, 20f, yellow, true),
            text("project", "项目名称：点击修改", .28f, .73f, 13f, white, true),
            text("area", "施工区域：点击修改", .27f, .78f, 12f, white),
            text("progress", "当前进度：点击修改", .27f, .83f, 12f, mint),
            text("person", "记录人：点击修改", .24f, .88f, 11f, white),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .92f, 11f, yellow),
            location("place", "点击设置项目地址", .26f, .96f, 10f, white)
        ),
        template("builtin_acceptance", "工程验收", TemplateCategory.WORK,
            icon("check", BuiltInIcon.CHECK, .09f, .68f, mint),
            text("title", "工程验收记录", .40f, .68f, 19f, white, true),
            text("project", "项目：点击修改", .24f, .75f, 13f, white),
            text("item", "验收项：点击修改", .25f, .80f, 12f, white),
            text("result", "验收结果：合格", .24f, .85f, 13f, mint, true),
            text("person", "验收人：点击修改", .25f, .90f, 11f, white),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .95f, 11f, yellow)
        ),
        template("builtin_supervision", "监理旁站", TemplateCategory.WORK,
            text("title", "监理旁站记录", .25f, .68f, 19f, cyan, true),
            text("project", "工程名称：点击修改", .27f, .75f, 12f, white),
            text("process", "施工工序：点击修改", .27f, .80f, 12f, white),
            text("unit", "施工单位：点击修改", .27f, .85f, 12f, white),
            text("supervisor", "旁站监理：点击修改", .27f, .90f, 11f, mint),
            location("place", "点击设置旁站地点", .27f, .95f, 10f, white)
        ),
        template("builtin_rectification", "隐患整改", TemplateCategory.INSPECTION,
            text("title", "安全隐患整改", .25f, .66f, 20f, red, true),
            text("issue", "隐患内容：点击修改", .27f, .73f, 12f, white),
            text("action", "整改措施：点击修改", .27f, .78f, 12f, white),
            text("result", "复查结果：已完成", .25f, .83f, 12f, mint, true),
            text("person", "责任人：点击修改", .24f, .88f, 11f, white),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .92f, 11f, yellow),
            location("place", "点击设置整改地点", .27f, .96f, 10f, white)
        ),
        template("builtin_material", "物资进场", TemplateCategory.WORK,
            icon("camera", BuiltInIcon.CAMERA, .09f, .68f, yellow),
            text("title", "物资进场验收", .40f, .68f, 18f, white, true),
            text("material", "物资名称：点击修改", .27f, .75f, 12f, white),
            text("quantity", "规格数量：点击修改", .26f, .80f, 12f, white),
            text("supplier", "供应单位：点击修改", .27f, .85f, 12f, white),
            text("result", "验收状态：合格", .24f, .90f, 12f, mint),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .95f, 11f, yellow)
        ),
        template("builtin_training", "班前教育", TemplateCategory.WORK,
            text("title", "班前安全教育", .24f, .69f, 20f, yellow, true),
            text("team", "班组：点击修改", .22f, .76f, 13f, white),
            text("topic", "教育主题：点击修改", .26f, .81f, 12f, white),
            text("speaker", "讲解人：点击修改", .24f, .86f, 12f, mint),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .91f, 11f, white),
            location("place", "点击设置教育地点", .27f, .96f, 10f, white)
        ),
        template("builtin_maintenance", "设备维保", TemplateCategory.INSPECTION,
            text("title", "设备维护保养", .24f, .68f, 19f, cyan, true),
            text("device", "设备名称：点击修改", .27f, .75f, 12f, white),
            text("number", "设备编号：点击修改", .27f, .80f, 12f, white),
            text("content", "维保内容：点击修改", .27f, .85f, 12f, white),
            text("person", "维保人员：点击修改", .27f, .90f, 11f, mint),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .95f, 11f, yellow)
        ),
        template("builtin_property", "物业巡查", TemplateCategory.INSPECTION,
            icon("check", BuiltInIcon.CHECK, .09f, .70f, mint),
            text("title", "物业巡查记录", .40f, .70f, 18f, white, true),
            text("area", "巡查区域：点击修改", .27f, .77f, 12f, white),
            text("item", "巡查项目：点击修改", .27f, .82f, 12f, white),
            text("result", "巡查结果：正常", .25f, .87f, 12f, mint),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .92f, 11f, yellow),
            location("place", "点击设置巡查地点", .27f, .96f, 10f, white)
        ),
        template("builtin_fire", "消防检查", TemplateCategory.INSPECTION,
            text("title", "消防设施检查", .24f, .68f, 20f, red, true),
            text("area", "检查区域：点击修改", .27f, .75f, 12f, white),
            text("facility", "设施名称：点击修改", .27f, .80f, 12f, white),
            text("result", "检查结果：正常", .25f, .85f, 12f, mint, true),
            text("person", "检查人员：点击修改", .27f, .90f, 11f, white),
            location("place", "点击设置检查地点", .27f, .95f, 10f, white)
        ),
        template("builtin_utilities", "水电巡检", TemplateCategory.INSPECTION,
            text("title", "水电巡检记录", .24f, .69f, 19f, cyan, true),
            text("meter", "表计编号：点击修改", .26f, .76f, 12f, white),
            text("reading", "当前读数：点击修改", .26f, .81f, 13f, yellow),
            text("status", "运行状态：正常", .24f, .86f, 12f, mint),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .91f, 11f, white),
            location("place", "点击设置巡检地点", .27f, .96f, 10f, white)
        ),
        template("builtin_store", "门店巡检", TemplateCategory.INSPECTION,
            text("title", "门店巡检", .18f, .70f, 20f, yellow, true),
            text("store", "门店名称：点击修改", .27f, .77f, 13f, white),
            text("item", "巡检项目：点击修改", .27f, .82f, 12f, white),
            text("result", "巡检结果：正常", .25f, .87f, 12f, mint),
            text("person", "巡检人：点击修改", .24f, .92f, 11f, white),
            location("place", "点击设置门店地址", .27f, .96f, 10f, white)
        ),
        template("builtin_attendance_detail", "考勤打卡", TemplateCategory.ATTENDANCE,
            time("clock", "HH:mm:ss", .20f, .73f, 25f, yellow, true, WatermarkFont.MONOSPACE),
            time("date", "yyyy年MM月dd日", .25f, .80f, 12f, white),
            text("team", "部门/班组：点击修改", .27f, .85f, 12f, white),
            text("person", "姓名：点击修改", .23f, .90f, 12f, mint),
            location("place", "点击设置打卡地点", .28f, .96f, 10f, white)
        ),
        template("builtin_delivery", "物流签收", TemplateCategory.ATTENDANCE,
            icon("check", BuiltInIcon.CHECK, .09f, .70f, mint),
            text("title", "物流签收记录", .40f, .70f, 18f, white, true),
            text("order", "单号：点击修改", .24f, .77f, 12f, white),
            text("receiver", "签收人：点击修改", .25f, .82f, 12f, white),
            text("remark", "备注：点击修改", .24f, .87f, 12f, white),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .92f, 11f, yellow),
            location("place", "点击设置签收地址", .27f, .96f, 10f, white)
        ),
        template("builtin_meeting", "会议记录", TemplateCategory.ATTENDANCE,
            text("title", "现场会议记录", .24f, .70f, 19f, cyan, true),
            text("topic", "会议主题：点击修改", .27f, .77f, 12f, white),
            text("host", "主持人：点击修改", .24f, .82f, 12f, white),
            text("note", "纪要：点击修改", .23f, .87f, 12f, white),
            time("time", "yyyy-MM-dd  HH:mm", .25f, .92f, 11f, yellow),
            location("place", "点击设置会议地点", .27f, .96f, 10f, white)
        ),
        template("builtin_travel", "旅行足迹", TemplateCategory.LIFE,
            icon("star", BuiltInIcon.STAR, .09f, .79f, yellow),
            text("title", "旅途记录", .34f, .79f, 22f, white, true, WatermarkFont.SERIF),
            location("place", "点击设置城市", .24f, .86f, 14f, mint, true),
            time("date", "yyyy.MM.dd", .20f, .92f, 12f, white)
        ),
        template("builtin_daily", "日常片刻", TemplateCategory.LIFE,
            time("clock", "HH:mm", .14f, .80f, 26f, white, true, WatermarkFont.MONOSPACE),
            time("date", "yyyy / MM / dd", .21f, .87f, 12f, yellow, font = WatermarkFont.MONOSPACE),
            text("note", "记录此刻", .18f, .93f, 13f, white, font = WatermarkFont.SERIF)
        )
    )

    private fun template(
        id: String,
        name: String,
        category: TemplateCategory,
        vararg layers: WatermarkLayer
    ): WatermarkTemplate {
        val alignedLayers = BuiltInTemplateLayout.arrange(id, layers.toList())
        return WatermarkTemplate(
            id = id,
            name = name,
            layers = alignedLayers,
            updatedAt = 0L,
            category = category,
            builtIn = true
        )
    }

    private fun text(
        id: String,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        font: WatermarkFont = WatermarkFont.DEFAULT
    ) = WatermarkLayer(
        id = id,
        kind = LayerKind.TEXT,
        text = value,
        x = x,
        y = y,
        color = color,
        fontSize = size,
        bold = bold,
        font = font,
        backgroundColor = 0
    )

    private fun time(
        id: String,
        format: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        font: WatermarkFont = WatermarkFont.DEFAULT
    ) = text(id, format, x, y, size, color, bold, font).copy(kind = LayerKind.TIME)

    private fun location(
        id: String,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = text(id, value, x, y, size, color, bold).copy(kind = LayerKind.LOCATION)

    private fun icon(id: String, value: BuiltInIcon, x: Float, y: Float, color: Int) = WatermarkLayer(
        id = id,
        kind = LayerKind.ICON,
        icon = value,
        x = x,
        y = y,
        color = color,
        backgroundColor = 0
    )

    private fun image(
        id: String,
        uri: String,
        x: Float,
        y: Float,
        baseSize: Float
    ) = WatermarkLayer(
        id = id,
        kind = LayerKind.IMAGE,
        imageUri = uri,
        baseSize = baseSize,
        x = x,
        y = y,
        backgroundColor = 0
    )

    private fun dingX(pixels: Float) = pixels / 1200f

    private fun dingY(pixels: Float) = pixels / 2236f

    private fun dingSize(pixels: Float) = pixels * 360f / 1200f

    fun forPortraitAspect(template: WatermarkTemplate, portraitAspectRatio: Float): WatermarkTemplate {
        if (!template.builtIn || template.id == BuiltInTemplateLayout.DINGTALK_TEMPLATE_ID) return template
        return template.copy(
            layers = BuiltInTemplateLayout.arrange(template.id, template.layers, portraitAspectRatio)
        )
    }
}
