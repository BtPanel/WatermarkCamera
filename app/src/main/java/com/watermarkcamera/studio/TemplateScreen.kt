package com.watermarkcamera.studio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class TemplateFilter(val label: String) {
    ALL("全部"),
    FEATURED("内置"),
    TIME_LOCATION("时间地点"),
    WORK("工程"),
    INSPECTION("巡检"),
    ATTENDANCE("外勤"),
    LIFE("生活"),
    MINE("我的")
}

@Composable
fun TemplateScreen(
    store: ProjectStore,
    currentLayers: List<WatermarkLayer>,
    canSave: Boolean,
    onBack: () -> Unit,
    onApply: (WatermarkTemplate) -> Unit
) {
    var userTemplates by remember { mutableStateOf(store.loadTemplates()) }
    var filter by remember { mutableStateOf(TemplateFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<WatermarkTemplate?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WatermarkTemplate?>(null) }
    var exportTarget by remember { mutableStateOf<WatermarkTemplate?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates = BuiltInTemplates.all + userTemplates
    val filteredTemplates = templates.filter { template ->
        val categoryMatches = when (filter) {
            TemplateFilter.ALL -> true
            TemplateFilter.FEATURED -> template.builtIn
            TemplateFilter.TIME_LOCATION -> template.category == TemplateCategory.TIME_LOCATION
            TemplateFilter.WORK -> template.category == TemplateCategory.WORK
            TemplateFilter.INSPECTION -> template.category == TemplateCategory.INSPECTION
            TemplateFilter.ATTENDANCE -> template.category == TemplateCategory.ATTENDANCE
            TemplateFilter.LIFE -> template.category == TemplateCategory.LIFE
            TemplateFilter.MINE -> !template.builtIn
        }
        categoryMatches && (query.isBlank() || template.name.contains(query.trim(), ignoreCase = true))
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val template = exportTarget
        exportTarget = null
        if (uri != null && template != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.buffered()?.use { output ->
                            store.exportTemplatePackage(template, output)
                        } ?: error("无法写入文件")
                    }
                }.onSuccess { message = "完整模板包已保存到手机存储" }
                    .onFailure { message = it.message ?: "模板导出失败" }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.buffered()?.use(store::importTemplate)
                            ?: error("无法读取文件")
                    }
                }.onSuccess {
                    store.saveTemplate(it)
                    userTemplates = store.loadTemplates()
                    filter = TemplateFilter.MINE
                    message = "已导入模板“${it.name}”"
                }.onFailure { message = it.message ?: "模板导入失败" }
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "返回") }
                Column(Modifier.weight(1f)) {
                    Text("水印模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1) }
                }
                IconButton(onClick = {
                    importLauncher.launch(arrayOf("application/zip", "application/json", "text/plain", "application/octet-stream"))
                }) {
                    Icon(Icons.Outlined.FileOpen, "从存储导入模板")
                }
                IconButton(onClick = { showNew = true }, enabled = canSave && currentLayers.isNotEmpty()) {
                    Icon(Icons.Outlined.Add, "新建模板")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                singleLine = true,
                label = { Text("搜索模板") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "清除搜索") }
                }
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TemplateFilter.entries.forEach { item ->
                    FilterChip(
                        modifier = Modifier.height(36.dp),
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.label, maxLines = 1) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (filteredTemplates.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有匹配的模板", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
                        TemplateItem(
                            template = template,
                            canUpdate = canSave && currentLayers.isNotEmpty(),
                            onApply = { onApply(template) },
                            onCopy = {
                                val copy = template.copy(
                                    id = UUID.randomUUID().toString(),
                                    name = "${template.name} 副本",
                                    layers = template.layers.map { it.copy(id = UUID.randomUUID().toString()) },
                                    updatedAt = System.currentTimeMillis(),
                                    builtIn = false
                                )
                                store.saveTemplate(copy)
                                userTemplates = store.loadTemplates()
                                filter = TemplateFilter.MINE
                                message = "已复制到我的模板"
                            },
                            onExport = {
                                exportTarget = template
                                val safeName = template.name.replace(Regex("[\\/:*?\"<>|]"), "_")
                                exportLauncher.launch("$safeName.watermark-template.zip")
                            },
                            onRename = { editTarget = template },
                            onDelete = { deleteTarget = template },
                            onUpdate = {
                                store.saveTemplate(template.copy(layers = currentLayers.toList()))
                                userTemplates = store.loadTemplates()
                                message = "模板内容已更新"
                            }
                        )
                    }
                    item { Box(Modifier.height(4.dp)) }
                }
            }
        }
    }

    if (showNew) NameDialog(
        title = "新建模板",
        initial = "我的水印 ${userTemplates.size + 1}",
        onDismiss = { showNew = false },
        onSave = { name ->
            store.saveTemplate(
                WatermarkTemplate(
                    name = name,
                    layers = currentLayers.toList(),
                    category = TemplateCategory.CUSTOM
                )
            )
            userTemplates = store.loadTemplates()
            filter = TemplateFilter.MINE
            showNew = false
        }
    )
    editTarget?.let { template ->
        NameDialog(
            title = "重命名模板",
            initial = template.name,
            onDismiss = { editTarget = null },
            onSave = { name ->
                store.saveTemplate(template.copy(name = name))
                userTemplates = store.loadTemplates()
                editTarget = null
            }
        )
    }
    deleteTarget?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除模板") },
            text = { Text("确定删除“${template.name}”吗？") },
            confirmButton = {
                Button(onClick = {
                    store.deleteTemplate(template.id)
                    userTemplates = store.loadTemplates()
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun TemplateItem(
    template: WatermarkTemplate,
    canUpdate: Boolean,
    onApply: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        TemplatePreview(template, Modifier.fillMaxWidth().aspectRatio(2.5f))
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(template.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (template.builtIn) "内置 · ${template.category.label} · ${template.layers.size} 个图层"
                        else "${template.category.label} · ${template.layers.size} 个图层 · ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(template.updatedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (!template.builtIn) {
                    IconButton(onClick = onExport, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Download, "导出到存储") }
                    IconButton(onClick = onRename, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Edit, "重命名") }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Delete, "删除") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (template.builtIn) {
                    OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.ContentCopy, null)
                        Text("复制到我的", Modifier.padding(start = 6.dp))
                    }
                } else {
                    OutlinedButton(onClick = onUpdate, enabled = canUpdate, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Refresh, null)
                        Text("更新内容", Modifier.padding(start = 6.dp))
                    }
                }
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("使用模板") }
            }
        }
    }
}

@Composable
private fun TemplatePreview(template: WatermarkTemplate, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier.background(Color(0xFF505758))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF62696A), size = androidx.compose.ui.geometry.Size(size.width, size.height * .38f))
            drawLine(Color.White.copy(alpha = .16f), androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = .16f), androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), 1.dp.toPx())
        }
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        template.layers.filterNot { it.hidden }.forEach { layer ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val layerModifier = Modifier.graphicsLayer {
                    val scale = layer.uniformScale() * .7f
                    val previewY = BuiltInTemplateLayout.previewY(template.id, layer.y)
                    val anchorOffset = when (layer.horizontalAnchor) {
                        LayerHorizontalAnchor.LEFT -> size.width * scale / 2f
                        LayerHorizontalAnchor.CENTER -> 0f
                        LayerHorizontalAnchor.RIGHT -> -size.width * scale / 2f
                    }
                    translationX = (layer.x - .5f) * widthPx + anchorOffset
                    translationY = (previewY - .5f) * heightPx
                    rotationZ = layer.rotation
                    scaleX = scale
                    scaleY = scale
                    alpha = layer.alpha
                }
                when (layer.kind) {
                    LayerKind.ICON -> Icon(
                        imageVector = when (layer.icon) {
                            BuiltInIcon.CAMERA -> Icons.Filled.PhotoCamera
                            BuiltInIcon.PIN -> Icons.Filled.LocationOn
                            BuiltInIcon.CHECK -> Icons.Filled.CheckCircle
                            BuiltInIcon.STAR -> Icons.Filled.Star
                            BuiltInIcon.DIVIDER -> Icons.Outlined.Remove
                        },
                        contentDescription = null,
                        tint = Color(layer.color),
                        modifier = layerModifier.size(22.dp)
                    )
                    LayerKind.IMAGE -> {
                        val bitmap by produceState<android.graphics.Bitmap?>(null, layer.imageUri) {
                            value = layer.imageUri?.let { ImageProcessing.loadBitmap(context, Uri.parse(it), 512) }
                        }
                        bitmap?.let {
                            val ratio = it.width.toFloat() / it.height
                            val baseSize = (layer.baseSize * .7f).coerceIn(3f, 168f)
                            val imageModifier = if (ratio >= 1f) {
                                layerModifier.size(width = baseSize.dp, height = (baseSize / ratio).dp)
                            } else {
                                layerModifier.size(width = (baseSize * ratio).dp, height = baseSize.dp)
                            }
                            Image(it.asImageBitmap(), null, imageModifier, contentScale = ContentScale.FillBounds)
                        } ?: Icon(Icons.Outlined.Image, null, tint = Color.White, modifier = layerModifier.size(20.dp))
                    }
                    LayerKind.LOCATION -> {
                        val bitmap by produceState<android.graphics.Bitmap?>(null, layer.imageUri) {
                            value = layer.imageUri?.let { ImageProcessing.loadBitmap(context, Uri.parse(it), 512) }
                        }
                        val fontFamily = remember(layer.font, layer.fontUri, layer.bold) {
                            androidx.compose.ui.text.font.FontFamily(FontSupport.resolve(context, layer))
                        }
                        LocationWatermarkContent(layer, bitmap, fontFamily, layerModifier, contentScale = .55f)
                    }
                    else -> Text(
                        text = layer.displayText(),
                        color = Color(layer.color),
                        fontSize = (layer.fontSize * .55f).coerceIn(7f, 17f).sp,
                        fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        letterSpacing = 0.sp,
                        modifier = layerModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("模板名称") }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
