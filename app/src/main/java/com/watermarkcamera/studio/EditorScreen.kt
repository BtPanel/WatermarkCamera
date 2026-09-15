package com.watermarkcamera.studio

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AlignHorizontalCenter
import androidx.compose.material.icons.outlined.AlignHorizontalLeft
import androidx.compose.material.icons.outlined.AlignHorizontalRight
import androidx.compose.material.icons.outlined.AlignVerticalBottom
import androidx.compose.material.icons.outlined.AlignVerticalCenter
import androidx.compose.material.icons.outlined.AlignVerticalTop
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToInt

private val EditorCanvasColor = Color(0xFF0B0E0D)
private val EditorChromeColor = Color(0xF21A201E)
private val EditorRaisedColor = Color(0xFF272E2B)
private val EditorContentColor = Color(0xFFF1F5F3)
private val EditorMutedColor = Color(0xFFAEB9B4)
private val EditorAccentColor = Color(0xFF72D9C1)

@Composable
fun EditorScreen(
    bitmap: Bitmap?,
    imageLoadStatus: ImageLoadStatus,
    photoTransform: PhotoTransform,
    onPhotoTransformChanged: (PhotoTransform) -> Unit,
    layers: List<WatermarkLayer>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    onLayersChanged: (List<WatermarkLayer>) -> Unit,
    onBack: () -> Unit,
    onRetryImage: () -> Unit,
    onChooseImage: () -> Unit,
    onCrop: () -> Unit,
    onPickLogo: () -> Unit,
    onPickFont: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onTemplates: () -> Unit,
    settings: AppSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenConfiguration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { screenConfiguration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { screenConfiguration.screenHeightDp.dp.toPx() }
    var propertyPanelOnRight by remember { mutableStateOf(true) }
    var propertyPanelOffsetX by remember { mutableFloatStateOf(0f) }
    var propertyPanelOffsetY by remember { mutableFloatStateOf(0f) }
    val undo = remember { mutableStateListOf<List<WatermarkLayer>>() }
    val redo = remember { mutableStateListOf<List<WatermarkLayer>>() }
    var showIcons by remember { mutableStateOf(false) }
    var showLocation by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showProperties by remember { mutableStateOf(false) }
    var showPhotoControls by remember { mutableStateOf(false) }
    val selected = layers.firstOrNull { it.id == selectedId }
    val currentPhotoTransform = rememberUpdatedState(photoTransform)
    val currentOnPhotoTransformChanged = rememberUpdatedState(onPhotoTransformChanged)

    LaunchedEffect(selectedId) {
        showProperties = selectedId != null
        if (selectedId != null) showPhotoControls = false
    }

    fun checkpoint() {
        val snapshot = layers.toList()
        if (undo.lastOrNull() != snapshot) {
            if (undo.size >= 50) undo.removeAt(0)
            undo += snapshot
        }
        redo.clear()
    }

    fun replace(layer: WatermarkLayer) {
        onLayersChanged(layers.map { if (it.id == layer.id) layer else it })
    }

    fun add(layer: WatermarkLayer) {
        checkpoint()
        onLayersChanged(layers + layer)
        onSelected(layer.id)
        showProperties = true
    }

    Box(Modifier.fillMaxSize().background(EditorCanvasColor)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 62.dp, bottom = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap == null) {
                if (imageLoadStatus == ImageLoadStatus.ERROR) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.BrokenImage,
                            contentDescription = null,
                            tint = EditorMutedColor,
                            modifier = Modifier.size(42.dp)
                        )
                        Text("无法读取这张照片", color = EditorContentColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRetryImage) {
                                Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                                Text("重试", Modifier.padding(start = 6.dp))
                            }
                            TextButton(onClick = onChooseImage) {
                                Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp))
                                Text("重新选择", Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                BoxWithConstraints(
                    Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val imageRatio = bitmap.width.toFloat() / bitmap.height
                    val availableRatio = constraints.maxWidth.toFloat() / constraints.maxHeight
                    val imageModifier = if (imageRatio > availableRatio) {
                        Modifier.fillMaxWidth().aspectRatio(imageRatio)
                    } else {
                        Modifier.height(maxHeight).aspectRatio(imageRatio)
                    }
                    Box(imageModifier.clip(RoundedCornerShape(2.dp)).pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onSelected(null)
                                showProperties = false
                            },
                            onDoubleTap = {
                                currentOnPhotoTransformChanged.value(PhotoTransform())
                            }
                        )
                    }) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "编辑照片",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = photoTransform.scale
                                    scaleY = photoTransform.scale
                                    translationX = photoTransform.offsetX * size.width
                                    translationY = photoTransform.offsetY * size.height
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val current = currentPhotoTransform.value
                                        currentOnPhotoTransformChanged.value(
                                            current.copy(
                                                scale = (current.scale * zoom).coerceIn(.05f, 6f),
                                                offsetX = (current.offsetX + pan.x / size.width).coerceIn(-1f, 1f),
                                                offsetY = (current.offsetY + pan.y / size.height).coerceIn(-1f, 1f)
                                            )
                                        )
                                    }
                                }
                        )
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val canvasWidthPx = constraints.maxWidth.toFloat()
                            val canvasHeightPx = constraints.maxHeight.toFloat()
                            if (settings.showGuides) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawLine(Color.White.copy(alpha = .28f), androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), 1.dp.toPx())
                                    drawLine(Color.White.copy(alpha = .28f), androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), 1.dp.toPx())
                                }
                            }
                            layers.forEach { layer ->
                                if (!layer.hidden) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        WatermarkLayerView(
                                            layer = layer,
                                            selected = layer.id == selectedId,
                                            canvasWidthPx = canvasWidthPx,
                                            canvasHeightPx = canvasHeightPx,
                                            onSelect = {
                                                showPhotoControls = false
                                                onSelected(layer.id)
                                                showProperties = true
                                            },
                                            onTransformStart = { checkpoint() },
                                             onTransform = { base, panX, panY, zoom, rotation ->
                                                 if (!base.locked) {
                                                     val rawX = (base.x + panX / canvasWidthPx).coerceIn(0f, 1f)
                                                     val rawY = (base.y + panY / canvasHeightPx).coerceIn(0f, 1f)
                                                     val nextX = if (settings.snapToCenter && kotlin.math.abs(rawX - .5f) < .025f) .5f else rawX
                                                     val nextY = if (settings.snapToCenter && kotlin.math.abs(rawY - .5f) < .025f) .5f else rawY
                                                     val minScale = if (base.kind == LayerKind.IMAGE) .05f else .15f
                                                     replace(base.copy(
                                                     x = nextX,
                                                     y = nextY,
                                                     scaleX = (base.scaleX * zoom).coerceIn(minScale, 6f),
                                                     scaleY = (base.scaleY * zoom).coerceIn(minScale, 6f),
                                                     rotation = base.rotation + rotation
                                                 )) }
                                             },
                                             onResize = { base, widthDelta, heightDelta, preserveAspect ->
                                                 if (!base.locked) {
                                                     replace(resizeWatermarkLayer(base, widthDelta, heightDelta, preserveAspect))
                                                 }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            message?.let {
                Surface(
                    Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = RoundedCornerShape(6.dp)
                ) { Text(it, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.inverseOnSurface) }
                LaunchedEffect(it) { delay(1800); message = null }
            }
        }

        EditorTopBar(
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 8.dp, vertical = 6.dp),
            canUndo = undo.isNotEmpty(),
            canRedo = redo.isNotEmpty(),
            canSave = bitmap != null,
            canExport = bitmap != null && !busy,
            onBack = onBack,
            onUndo = {
                if (undo.isNotEmpty()) {
                    redo += layers.toList()
                    onLayersChanged(undo.removeAt(undo.lastIndex))
                    onSelected(null)
                }
            },
            onRedo = {
                if (redo.isNotEmpty()) {
                    undo += layers.toList()
                    onLayersChanged(redo.removeAt(redo.lastIndex))
                    onSelected(null)
                }
            },
            onSave = { onSaveDraft(); message = "草稿已保存" },
            onExport = { showExport = true }
        )

        if (showProperties && selected != null) {
            FloatingLayerProperties(
                panelOnRight = propertyPanelOnRight,
                horizontalOffsetPx = propertyPanelOffsetX,
                verticalOffsetPx = propertyPanelOffsetY,
                onDrag = { deltaX, deltaY ->
                    propertyPanelOffsetX = (propertyPanelOffsetX + deltaX).coerceIn(-screenWidthPx * .75f, screenWidthPx * .75f)
                    propertyPanelOffsetY = (propertyPanelOffsetY + deltaY).coerceIn(-screenHeightPx * .15f, screenHeightPx * .07f)
                },
                onDragEnd = {
                    if (propertyPanelOnRight && propertyPanelOffsetX < -screenWidthPx * .25f) propertyPanelOnRight = false
                    if (!propertyPanelOnRight && propertyPanelOffsetX > screenWidthPx * .25f) propertyPanelOnRight = true
                    propertyPanelOffsetX = 0f
                },
                layer = selected,
                onClose = {
                    showProperties = false
                    onSelected(null)
                },
                onCheckpoint = { checkpoint() },
                onChange = ::replace,
                onPickFont = { onPickFont(selected.id) },
                onDuplicate = {
                    checkpoint()
                    val copy = selected.copy(
                        id = UUID.randomUUID().toString(),
                        x = (selected.x + .04f).coerceAtMost(1f),
                        y = (selected.y + .04f).coerceAtMost(1f)
                    )
                    onLayersChanged(layers + copy)
                    onSelected(copy.id)
                },
                onDelete = {
                    checkpoint()
                    onLayersChanged(layers.filterNot { it.id == selected.id })
                    onSelected(null)
                    showProperties = false
                },
                onMove = { delta ->
                    val index = layers.indexOfFirst { it.id == selected.id }
                    val target = (index + delta).coerceIn(0, layers.lastIndex)
                    if (target != index) {
                        checkpoint()
                        val copy = layers.toMutableList()
                        val item = copy.removeAt(index)
                        copy.add(target, item)
                        onLayersChanged(copy)
                    }
                }
            )
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = EditorChromeColor,
                tonalElevation = 4.dp
            ) {
                Column {
                    if (showPhotoControls) {
                        PhotoTransformBar(
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            transform = photoTransform,
                            onChange = onPhotoTransformChanged,
                            onClose = { showPhotoControls = false }
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorTool(Icons.Outlined.TextFormat, "文字") { add(WatermarkLayer(kind = LayerKind.TEXT, text = "修改文字", alpha = settings.defaultOpacity, fontSize = settings.defaultFontSize, backgroundColor = 0)) }
                        EditorTool(Icons.Outlined.Schedule, "时间") { add(WatermarkLayer(kind = LayerKind.TIME, text = settings.defaultTimeFormat, alpha = settings.defaultOpacity, fontSize = settings.defaultFontSize, backgroundColor = 0)) }
                        EditorTool(Icons.Outlined.EditLocationAlt, "位置") { showLocation = true }
                        EditorTool(Icons.Outlined.Image, "图片") { onPickLogo() }
                        EditorTool(Icons.Outlined.AddPhotoAlternate, "图标") { showIcons = true }
                        EditorTool(Icons.Outlined.ZoomOutMap, "调整照片", selected = showPhotoControls) {
                            onSelected(null)
                            showPhotoControls = !showPhotoControls
                        }
                        EditorTool(Icons.Outlined.Crop, "裁剪") { onCrop() }
                        EditorTool(Icons.Outlined.Layers, "模板") { onTemplates() }
                    }
                }
            }
        }
    }

    if (showIcons) IconPickerDialog(
        onDismiss = { showIcons = false },
        onPick = { showIcons = false; add(WatermarkLayer(kind = LayerKind.ICON, icon = it, backgroundColor = 0, alpha = settings.defaultOpacity)) }
    )
    if (showLocation) LocationPickerDialog(
        onDismiss = { showLocation = false },
        onPick = { value -> showLocation = false; add(WatermarkLayer(kind = LayerKind.LOCATION, text = value, alpha = settings.defaultOpacity, fontSize = settings.defaultFontSize, maxLines = 3)) }
    )
    if (showExport && bitmap != null) ExportDialog(
        initialQuality = settings.exportQuality,
        sourceWidth = bitmap.width,
        sourceHeight = bitmap.height,
        busy = busy,
        onDismiss = { if (!busy) showExport = false },
        onExport = { quality, dimensions, share ->
            busy = true
            scope.launch {
                val rendered = ImageProcessing.render(context, bitmap, layers, photoTransform, dimensions)
                val uri = ImageProcessing.saveToGallery(context, rendered, quality)
                rendered.recycle()
                busy = false
                showExport = false
                if (uri != null) {
                    message = "已保存到相册"
                    if (share) {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "分享照片"))
                    }
                } else message = "导出失败"
            }
        }
    )
}

@Composable
private fun EditorTopBar(
    modifier: Modifier = Modifier,
    canUndo: Boolean,
    canRedo: Boolean,
    canSave: Boolean,
    canExport: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = EditorChromeColor,
        tonalElevation = 4.dp
    ) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorChromeButton(Icons.Outlined.ArrowBack, "返回", onClick = onBack)
            Text(
                "水印编辑",
                Modifier.weight(1f).padding(start = 2.dp),
                color = EditorContentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            EditorChromeButton(Icons.Outlined.Undo, "撤销", enabled = canUndo, onClick = onUndo)
            EditorChromeButton(Icons.Outlined.Redo, "重做", enabled = canRedo, onClick = onRedo)
            EditorChromeButton(Icons.Outlined.Save, "保存草稿", enabled = canSave, onClick = onSave)
            Button(
                onClick = onExport,
                enabled = canExport,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EditorAccentColor,
                    contentColor = Color(0xFF062E27),
                    disabledContainerColor = EditorRaisedColor,
                    disabledContentColor = EditorMutedColor.copy(alpha = .5f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("导出", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EditorChromeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    tint: Color = EditorContentColor,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            description,
            Modifier.size(21.dp),
            tint = if (enabled) tint else EditorMutedColor.copy(alpha = .38f)
        )
    }
}

@Composable
private fun PhotoTransformBar(
    modifier: Modifier = Modifier,
    transform: PhotoTransform,
    onChange: (PhotoTransform) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorChromeButton(Icons.Outlined.Close, "关闭照片调整", onClick = onClose)
        Icon(Icons.Outlined.ZoomOutMap, null, Modifier.size(20.dp), tint = EditorMutedColor)
        Slider(
            value = transform.scale,
            onValueChange = { onChange(transform.copy(scale = it)) },
            valueRange = .05f..6f,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = EditorAccentColor,
                activeTrackColor = EditorAccentColor,
                inactiveTrackColor = EditorMutedColor.copy(alpha = .28f)
            )
        )
        Text("${(transform.scale * 100).toInt()}%", color = EditorContentColor, style = MaterialTheme.typography.labelMedium)
        EditorChromeButton(Icons.Outlined.Refresh, "重置照片") { onChange(PhotoTransform()) }
    }
}

@Composable
private fun WatermarkLayerView(
    layer: WatermarkLayer,
    selected: Boolean,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    onSelect: () -> Unit,
    onTransformStart: () -> Unit,
    onTransform: (WatermarkLayer, Float, Float, Float, Float) -> Unit,
    onResize: (WatermarkLayer, Float, Float, Boolean) -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val canvasScale = min(canvasWidthPx, canvasHeightPx) / (360f * density)
    val currentLayer = rememberUpdatedState(layer)
    val currentSelected = rememberUpdatedState(selected)
    val currentTransform by rememberUpdatedState(onTransform)
    val currentTransformStart by rememberUpdatedState(onTransformStart)
    val currentResize by rememberUpdatedState(onResize)
    val bitmap by produceState<Bitmap?>(initialValue = null, layer.imageUri) {
        value = layer.imageUri?.let { ImageProcessing.loadBitmap(context, Uri.parse(it), 1024) }
    }
    val textFontFamily = remember(layer.font, layer.fontUri, layer.bold) {
        FontFamily(FontSupport.resolve(context, layer))
    }
    val border = if (selected) Modifier.border(1.dp, Color(0xFF4A90E2), RoundedCornerShape(2.dp)) else Modifier
    Box(
        Modifier.graphicsLayer {
            val horizontalScale = layer.scaleX * canvasScale
            val verticalScale = layer.scaleY * canvasScale
            val anchorOffset = when (layer.horizontalAnchor) {
                LayerHorizontalAnchor.LEFT -> size.width * horizontalScale / 2f
                LayerHorizontalAnchor.CENTER -> 0f
                LayerHorizontalAnchor.RIGHT -> -size.width * horizontalScale / 2f
            }
            translationX = (layer.x - .5f) * canvasWidthPx + anchorOffset
            translationY = (layer.y - .5f) * canvasHeightPx
            scaleX = horizontalScale
            scaleY = verticalScale
            rotationZ = layer.rotation
            alpha = layer.alpha
        }.then(border)
            .pointerInput(layer.id, layer.locked) {
                awaitEachGesture {
                    var accumulatedRotation = 0f
                    var accumulatedZoom = 1f
                    var accumulatedCanvasPan = Offset.Zero
                    var accumulatedResizePan = Offset.Zero
                    var transforming = false
                    val touchSlop = viewConfiguration.touchSlop
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureLayer = currentLayer.value
                    val edgeX = min(8.dp.toPx(), size.width * .16f)
                    val edgeY = min(8.dp.toPx(), size.height * .16f)
                    val resizeX = if (currentSelected.value && !gestureLayer.locked) when {
                        down.position.x <= edgeX -> -1f
                        down.position.x >= size.width - edgeX -> 1f
                        else -> 0f
                    } else 0f
                    val resizeY = if (currentSelected.value && !gestureLayer.locked) when {
                        down.position.y <= edgeY -> -1f
                        down.position.y >= size.height - edgeY -> 1f
                        else -> 0f
                    } else 0f
                    val resizeEdgeTouched = resizeX != 0f || resizeY != 0f
                    var action = watermarkGestureAction(gestureLayer.locked, resizeEdgeTouched, false)
                    down.consume()
                    onSelect()

                    if (action == WatermarkGestureAction.SELECT_ONLY && !gestureLayer.locked) {
                        var canceledBeforeLongPress = false
                        val completedBeforeTimeout = withTimeoutOrNull(350L) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed || change.isConsumed ||
                                    (change.position - down.position).getDistance() > touchSlop
                                ) {
                                    canceledBeforeLongPress = true
                                    return@withTimeoutOrNull true
                                }
                            }
                        }
                        if (!canceledBeforeLongPress && completedBeforeTimeout == null) {
                            action = watermarkGestureAction(false, false, true)
                            transforming = true
                            currentTransformStart()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    if (action == WatermarkGestureAction.SELECT_ONLY) return@awaitEachGesture

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val rotationChange = event.calculateRotation()
                            val panChange = event.calculatePan()
                            val resizing = action == WatermarkGestureAction.RESIZE && event.changes.count { it.pressed } <= 1
                            if (resizing) {
                                accumulatedResizePan += panChange
                            } else {
                                accumulatedZoom *= zoomChange
                                accumulatedRotation += rotationChange
                                accumulatedCanvasPan += panChange.toCanvasPan(currentLayer.value)
                            }

                            if (!transforming) {
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - accumulatedZoom) * centroidSize
                                val rotationMotion = abs(accumulatedRotation * PI.toFloat() * centroidSize / 180f)
                                val panMotion = if (resizing) accumulatedResizePan.getDistance() else accumulatedCanvasPan.getDistance()
                                if (max(max(zoomMotion, rotationMotion), panMotion) > touchSlop) {
                                    transforming = true
                                    currentTransformStart()
                                }
                            }

                            if (transforming) {
                                if (resizing) {
                                    currentResize(
                                        gestureLayer,
                                        resizeX * accumulatedResizePan.x / canvasWidthPx,
                                        resizeY * accumulatedResizePan.y / canvasHeightPx,
                                        resizeX != 0f && resizeY != 0f
                                    )
                                } else {
                                    currentTransform(
                                        gestureLayer,
                                        accumulatedCanvasPan.x,
                                        accumulatedCanvasPan.y,
                                        accumulatedZoom,
                                        accumulatedRotation
                                    )
                                }
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) change.consume()
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.padding(1.dp), contentAlignment = Alignment.Center) {
            when (layer.kind) {
                LayerKind.IMAGE -> bitmap?.let {
                    val ratio = it.width.toFloat() / it.height
                    val baseSize = layer.baseSize.coerceIn(4f, 240f)
                    val imageModifier = if (ratio >= 1f) {
                        Modifier.size(width = baseSize.dp, height = (baseSize / ratio).dp)
                    } else {
                        Modifier.size(width = (baseSize * ratio).dp, height = baseSize.dp)
                    }
                    Image(it.asImageBitmap(), "图片水印", imageModifier, contentScale = ContentScale.FillBounds)
                } ?: Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                LayerKind.ICON -> Icon(
                    imageVector = when (layer.icon) {
                        BuiltInIcon.CAMERA -> Icons.Filled.PhotoCamera
                        BuiltInIcon.PIN -> Icons.Filled.LocationOn
                        BuiltInIcon.CHECK -> Icons.Filled.CheckCircle
                        BuiltInIcon.STAR -> Icons.Filled.Star
                        BuiltInIcon.DIVIDER -> Icons.Outlined.Remove
                    },
                    contentDescription = layer.icon.label,
                    tint = Color(layer.color),
                    modifier = Modifier.size(48.dp)
                )
                LayerKind.LOCATION -> LocationWatermarkContent(layer, bitmap, textFontFamily)
                else -> Text(
                    text = layer.displayText(),
                    color = Color(layer.color),
                    fontSize = layer.fontSize.sp,
                    fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = textFontFamily,
                    lineHeight = layer.fontSize.sp,
                    letterSpacing = 0.sp,
                    modifier = if (layer.horizontalAnchor == LayerHorizontalAnchor.RIGHT) {
                        Modifier
                    } else {
                        Modifier.padding(horizontal = 2.dp)
                    }
                )
            }
        }
        if (selected && layer.locked) Icon(Icons.Outlined.Lock, "已锁定", Modifier.align(Alignment.TopEnd).size(18.dp), tint = Color(0xFF78D7C1))
    }
}

private fun Offset.toCanvasPan(layer: WatermarkLayer): Offset {
    val radians = Math.toRadians(layer.rotation.toDouble())
    val scaledX = x * layer.scaleX
    val scaledY = y * layer.scaleY
    val cosine = cos(radians).toFloat()
    val sine = sin(radians).toFloat()
    return Offset(
        x = scaledX * cosine - scaledY * sine,
        y = scaledX * sine + scaledY * cosine
    )
}

@Composable
private fun CompactLayerBar(
    layer: WatermarkLayer,
    onChange: (WatermarkLayer) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).background(EditorRaisedColor).padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onChange(layer.copy(locked = !layer.locked)) }) {
            Icon(if (layer.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, if (layer.locked) "解锁" else "锁定", tint = EditorContentColor)
        }
        IconButton(onClick = { onChange(layer.copy(hidden = !layer.hidden)) }) {
            Icon(if (layer.hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (layer.hidden) "显示" else "隐藏", tint = EditorContentColor)
        }
        IconButton(onClick = { onMove(-1) }) { Icon(Icons.Outlined.KeyboardArrowDown, "下移一层", tint = EditorContentColor) }
        IconButton(onClick = { onMove(1) }) { Icon(Icons.Outlined.KeyboardArrowUp, "上移一层", tint = EditorContentColor) }
        IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, "复制", tint = EditorContentColor) }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "删除", tint = Color(0xFFFFB4A2)) }
    }
}

private enum class LayerPropertyPanel { APPEARANCE, TRANSFORM, LAYER }

@Composable
fun FloatingLayerProperties(
    panelOnRight: Boolean,
    horizontalOffsetPx: Float = 0f,
    verticalOffsetPx: Float = 0f,
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    layer: WatermarkLayer,
    onClose: () -> Unit,
    onCheckpoint: () -> Unit,
    onChange: (WatermarkLayer) -> Unit,
    onPickFont: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onMove: ((Int) -> Unit)? = null
) {
    var activePanel by remember(layer.id) { mutableStateOf<LayerPropertyPanel?>(null) }
    var showCustomColor by remember(layer.id) { mutableStateOf(false) }
    var showLocationSearch by remember(layer.id) { mutableStateOf(false) }
    var showTextEditor by remember(layer.id) { mutableStateOf(false) }
    var originalText by remember(layer.id) { mutableStateOf(layer.text) }
    val supportsTextFormatting = layer.kind == LayerKind.TEXT || layer.kind == LayerKind.LOCATION || layer.kind == LayerKind.TIME
    val supportsTextInput = supportsTextFormatting
    val minimumScale = if (layer.kind == LayerKind.IMAGE) .05f else .15f
    val uniformScale = layer.uniformScale().coerceIn(minimumScale, 4f)

    fun toggle(panel: LayerPropertyPanel) {
        activePanel = if (activePanel == panel) null else panel
    }

    @Composable
    fun PropertyFlyout() {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = EditorChromeColor,
            tonalElevation = 10.dp
        ) {
            Column(
                Modifier
                    .width(236.dp)
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            when (activePanel) {
                LayerPropertyPanel.APPEARANCE -> {
                    PropertySliderRow(
                        icon = Icons.Outlined.Opacity,
                        description = "透明度",
                        displayValue = "${(layer.alpha * 100).toInt()}%",
                        value = layer.alpha,
                        valueRange = 0f..1f,
                        onCheckpoint = onCheckpoint,
                        onValueChange = { onChange(layer.copy(alpha = it)) }
                    )
                    if (supportsTextFormatting) {
                        PropertySliderRow(
                            icon = Icons.Outlined.FormatSize,
                            description = "字号",
                            displayValue = layer.fontSize.toInt().toString(),
                            value = layer.fontSize,
                            valueRange = 8f..48f,
                            onCheckpoint = onCheckpoint,
                            onValueChange = { onChange(layer.copy(fontSize = it)) }
                        )
                    }
                    if (supportsTextFormatting) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp))
                            .background(EditorRaisedColor).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onCheckpoint(); onChange(layer.copy(bold = !layer.bold)) }) {
                            Icon(
                                Icons.Outlined.FormatBold,
                                "粗体",
                                tint = if (layer.bold) EditorAccentColor else EditorContentColor
                            )
                        }
                        WatermarkFont.entries.filterNot { it == WatermarkFont.CUSTOM }.forEach { font ->
                            TextButton(
                                onClick = {
                                    onCheckpoint()
                                    onChange(layer.copy(font = font, fontUri = null, fontName = null))
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = EditorContentColor)
                            ) {
                                if (layer.font == font) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = EditorAccentColor)
                                Text(font.label, maxLines = 1)
                            }
                        }
                        TextButton(onClick = onPickFont, colors = ButtonDefaults.textButtonColors(contentColor = EditorContentColor)) {
                            Icon(Icons.Outlined.FontDownload, "导入字体")
                            Text(if (layer.font == WatermarkFont.CUSTOM) layer.fontName ?: "重新导入" else "导入", maxLines = 1)
                        }
                    }
                    }
                    if (layer.kind != LayerKind.IMAGE) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp))
                            .background(EditorRaisedColor).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf(0xFFFFFFFF, 0xFF101214, 0xFFFFD54F, 0xFFEF5350, 0xFF42A5F5, 0xFF66BB6A)
                        colors.forEach { color ->
                            IconButton(onClick = { onCheckpoint(); onChange(layer.copy(color = color.toInt())) }) {
                                Box(
                                    Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(color))
                                        .border(
                                            if (layer.color == color.toInt()) 3.dp else 1.dp,
                                            if (layer.color == color.toInt()) EditorAccentColor else EditorMutedColor,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                        IconButton(onClick = { showCustomColor = true }) {
                            Icon(Icons.Outlined.Palette, "自定义颜色", tint = EditorAccentColor)
                        }
                    }
                    }
                }
                LayerPropertyPanel.TRANSFORM -> {
                    PropertySliderRow(
                        icon = Icons.Outlined.ZoomOutMap,
                        description = "等比大小",
                        displayValue = "${(uniformScale * 100).toInt()}%",
                        value = uniformScale,
                        valueRange = minimumScale..4f,
                        onCheckpoint = onCheckpoint,
                        onValueChange = { onChange(layer.copy(scaleX = it, scaleY = it)) }
                    )
                    PropertySliderRow(
                        icon = Icons.Outlined.RotateRight,
                        description = "旋转",
                        displayValue = "${layer.rotation.toInt()}°",
                        value = layer.rotation,
                        valueRange = -180f..180f,
                        onCheckpoint = onCheckpoint,
                        onValueChange = { onChange(layer.copy(rotation = it)) }
                    )
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(EditorRaisedColor),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            onCheckpoint()
                            onChange(layer.copy(x = .1f, horizontalAnchor = LayerHorizontalAnchor.LEFT))
                        }) { Icon(Icons.Outlined.AlignHorizontalLeft, "靠左", tint = EditorContentColor) }
                        IconButton(onClick = {
                            onCheckpoint()
                            onChange(layer.copy(x = .5f, horizontalAnchor = LayerHorizontalAnchor.CENTER))
                        }) { Icon(Icons.Outlined.AlignHorizontalCenter, "水平居中", tint = EditorContentColor) }
                        IconButton(onClick = {
                            onCheckpoint()
                            onChange(layer.copy(x = .9f, horizontalAnchor = LayerHorizontalAnchor.RIGHT))
                        }) { Icon(Icons.Outlined.AlignHorizontalRight, "靠右", tint = EditorContentColor) }
                        IconButton(onClick = { onCheckpoint(); onChange(layer.copy(y = .1f)) }) { Icon(Icons.Outlined.AlignVerticalTop, "靠上", tint = EditorContentColor) }
                        IconButton(onClick = { onCheckpoint(); onChange(layer.copy(y = .5f)) }) { Icon(Icons.Outlined.AlignVerticalCenter, "垂直居中", tint = EditorContentColor) }
                        IconButton(onClick = { onCheckpoint(); onChange(layer.copy(y = .9f)) }) { Icon(Icons.Outlined.AlignVerticalBottom, "靠下", tint = EditorContentColor) }
                    }
                }
                LayerPropertyPanel.LAYER -> if (onDuplicate != null && onDelete != null && onMove != null) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(EditorRaisedColor),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onCheckpoint(); onChange(layer.copy(locked = !layer.locked)) }) {
                            Icon(if (layer.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, if (layer.locked) "解锁" else "锁定", tint = EditorContentColor)
                        }
                        IconButton(onClick = { onCheckpoint(); onChange(layer.copy(hidden = !layer.hidden)) }) {
                            Icon(if (layer.hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (layer.hidden) "显示" else "隐藏", tint = EditorContentColor)
                        }
                        IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, "复制", tint = EditorContentColor) }
                    }
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(EditorRaisedColor),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onMove(-1) }) { Icon(Icons.Outlined.KeyboardArrowDown, "下移一层", tint = EditorContentColor) }
                        IconButton(onClick = { onMove(1) }) { Icon(Icons.Outlined.KeyboardArrowUp, "上移一层", tint = EditorContentColor) }
                        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "删除", tint = Color(0xFFFFB4A2)) }
                    }
                }
                null -> Unit
            }
            }
        }
    }

    @Composable
    fun ToolRail() {
        Surface(
            modifier = Modifier.pointerInput(panelOnRight) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
            shape = RoundedCornerShape(8.dp),
            color = EditorChromeColor,
            tonalElevation = 10.dp
        ) {
            Column(
                Modifier.width(56.dp).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(width = 44.dp, height = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(width = 22.dp, height = 3.dp)
                            .background(EditorMutedColor.copy(alpha = .72f), RoundedCornerShape(2.dp))
                    )
                }
                EditorChromeButton(Icons.Outlined.Close, "关闭详细属性", onClick = onClose)
                if (supportsTextInput) {
                    PropertyMenuButton(Icons.Outlined.TextFormat, "内容", showTextEditor) {
                        originalText = layer.text
                        onCheckpoint()
                        activePanel = null
                        showTextEditor = true
                    }
                }
                if (layer.kind == LayerKind.LOCATION) {
                    EditorChromeButton(Icons.Outlined.EditLocationAlt, "搜索位置") { showLocationSearch = true }
                }
                if (layer.kind == LayerKind.IMAGE) {
                    PropertyMenuButton(Icons.Outlined.Palette, "外观", activePanel == LayerPropertyPanel.APPEARANCE) {
                        toggle(LayerPropertyPanel.APPEARANCE)
                    }
                } else {
                    PropertyMenuButton(Icons.Outlined.Palette, "外观", activePanel == LayerPropertyPanel.APPEARANCE, Color(layer.color)) {
                        toggle(LayerPropertyPanel.APPEARANCE)
                    }
                }
                PropertyMenuButton(Icons.Outlined.ZoomOutMap, "大小、旋转与对齐", activePanel == LayerPropertyPanel.TRANSFORM) {
                    toggle(LayerPropertyPanel.TRANSFORM)
                }
                if (onDuplicate != null && onDelete != null && onMove != null) {
                    PropertyMenuButton(Icons.Outlined.Layers, "图层操作", activePanel == LayerPropertyPanel.LAYER) {
                        toggle(LayerPropertyPanel.LAYER)
                    }
                }
                if (onDelete != null) {
                    EditorChromeButton(Icons.Outlined.Delete, "删除水印", tint = Color(0xFFFFB4A2), onClick = onDelete)
                }
            }
        }
    }

    Popup(
        alignment = if (panelOnRight) Alignment.CenterEnd else Alignment.CenterStart,
        offset = IntOffset(horizontalOffsetPx.roundToInt(), verticalOffsetPx.roundToInt()),
        onDismissRequest = {
            if (activePanel != null) activePanel = null else onClose()
        },
        properties = PopupProperties(focusable = true)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (panelOnRight && activePanel != null) PropertyFlyout()
            ToolRail()
            if (!panelOnRight && activePanel != null) PropertyFlyout()
        }
    }

    if (showTextEditor && supportsTextInput) {
        WatermarkTextEditDialog(
            value = layer.text,
            label = when (layer.kind) {
                LayerKind.LOCATION -> "位置文字"
                LayerKind.TIME -> "时间格式"
                else -> "水印文字"
            },
            onValueChange = { onChange(layer.copy(text = it)) },
            onConfirm = { showTextEditor = false },
            onDismiss = {
                onChange(layer.copy(text = originalText))
                showTextEditor = false
            }
        )
    }

    if (showCustomColor) {
        CustomColorDialog(
            initialColor = layer.color,
            onDismiss = { showCustomColor = false },
            onApply = { color ->
                showCustomColor = false
                onCheckpoint()
                onChange(layer.copy(color = color))
            }
        )
    }
    if (showLocationSearch) {
        LocationPickerDialog(
            initialText = layer.text,
            confirmLabel = "应用",
            onDismiss = { showLocationSearch = false },
            onPick = { value ->
                showLocationSearch = false
                onCheckpoint()
                onChange(layer.copy(text = value))
            }
        )
    }
}

@Composable
private fun WatermarkTextEditDialog(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改水印内容") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                label = { Text(label) }
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("完成") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun PropertyMenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    iconTint: Color = EditorContentColor,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).then(
            if (selected) Modifier.background(EditorAccentColor.copy(alpha = .18f), RoundedCornerShape(6.dp))
            else Modifier
        )
    ) {
        Icon(icon, description, Modifier.size(22.dp), tint = if (selected) EditorAccentColor else iconTint)
    }
}

@Composable
private fun PropertySliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    displayValue: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onCheckpoint: () -> Unit,
    onValueChange: (Float) -> Unit
) {
    var changing by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(EditorRaisedColor).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, description, Modifier.size(22.dp), tint = EditorMutedColor)
        Slider(
            value = value,
            onValueChange = {
                if (!changing) {
                    changing = true
                    onCheckpoint()
                }
                onValueChange(it)
            },
            onValueChangeFinished = { changing = false },
            valueRange = valueRange,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            colors = SliderDefaults.colors(
                thumbColor = EditorAccentColor,
                activeTrackColor = EditorAccentColor,
                inactiveTrackColor = EditorMutedColor.copy(alpha = .28f)
            )
        )
        Box(Modifier.size(width = 54.dp, height = 32.dp), contentAlignment = Alignment.Center) {
            Text(displayValue, color = EditorContentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun CustomColorDialog(initialColor: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    var red by remember(initialColor) { mutableIntStateOf(android.graphics.Color.red(initialColor)) }
    var green by remember(initialColor) { mutableIntStateOf(android.graphics.Color.green(initialColor)) }
    var blue by remember(initialColor) { mutableIntStateOf(android.graphics.Color.blue(initialColor)) }
    var hex by remember(initialColor) { mutableStateOf(String.format("%06X", initialColor and 0xFFFFFF)) }

    fun selectColor(color: Int) {
        red = android.graphics.Color.red(color)
        green = android.graphics.Color.green(color)
        blue = android.graphics.Color.blue(color)
        hex = String.format("%06X", color and 0xFFFFFF)
    }

    fun updateChannels(nextRed: Int = red, nextGreen: Int = green, nextBlue: Int = blue) {
        red = nextRed
        green = nextGreen
        blue = nextBlue
        hex = String.format("%02X%02X%02X", red, green, blue)
    }
    val parsed = hex.takeIf { it.length == 6 }?.let {
        runCatching { android.graphics.Color.rgb(red, green, blue) }.getOrNull()
    }
    val previewColor = Color(android.graphics.Color.rgb(red, green, blue))
    val presets = listOf(
        0xFFFFFFFF, 0xFF121416, 0xFFEF4444, 0xFFF97316, 0xFFFACC15,
        0xFF22C55E, 0xFF14B8A6, 0xFF3B82F6, 0xFF8B5CF6, 0xFFEC4899
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Palette, null) },
        title = { Text("选择颜色") },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(6.dp))
                        .background(previewColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "#${String.format("%02X%02X%02X", red, green, blue)}",
                        color = if (red * .299f + green * .587f + blue * .114f > 150f) Color.Black else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text("常用颜色", style = MaterialTheme.typography.labelLarge)
                presets.chunked(5).forEach { rowColors ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        rowColors.forEach { color ->
                            val colorInt = color.toInt()
                            val selected = parsed == colorInt
                            Box(
                                Modifier.size(42.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(color))
                                    .border(
                                        if (selected) 3.dp else 1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { selectColor(colorInt) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        "已选择",
                                        tint = if (android.graphics.Color.luminance(colorInt) > .5f) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Text("RGB", style = MaterialTheme.typography.labelLarge)
                ColorChannelRow("R", red, Color(0xFFE53935)) { updateChannels(nextRed = it) }
                ColorChannelRow("G", green, Color(0xFF2EAD62)) { updateChannels(nextGreen = it) }
                ColorChannelRow("B", blue, Color(0xFF3384E8)) { updateChannels(nextBlue = it) }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(6).uppercase()
                        hex = cleaned
                        if (cleaned.length == 6) {
                            runCatching { cleaned.toLong(16).toInt() }.getOrNull()?.let { value ->
                                red = (value shr 16) and 0xFF
                                green = (value shr 8) and 0xFF
                                blue = value and 0xFF
                            }
                        }
                    },
                    singleLine = true,
                    label = { Text("HEX") },
                    prefix = { Text("#") },
                    supportingText = { Text("输入 6 位十六进制颜色") },
                    isError = parsed == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { parsed?.let(onApply) }, enabled = parsed != null) { Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ColorChannelRow(label: String, value: Int, color: Color, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.size(width = 22.dp, height = 24.dp), fontWeight = FontWeight.SemiBold)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
            steps = 254,
            colors = SliderDefaults.colors(activeTrackColor = color, thumbColor = color),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Text(value.toString(), modifier = Modifier.size(width = 34.dp, height = 24.dp))
    }
}

@Composable
private fun EditorTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                if (selected) EditorAccentColor.copy(alpha = .18f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
    ) {
        Icon(
            icon,
            label,
            Modifier.size(22.dp),
            tint = if (selected) EditorAccentColor else EditorContentColor
        )
    }
}

@Composable
private fun IconPickerDialog(onDismiss: () -> Unit, onPick: (BuiltInIcon) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BuiltInIcon.entries.forEach { item ->
                    IconButton(onClick = { onPick(item) }) {
                        Icon(
                            when (item) {
                                BuiltInIcon.CAMERA -> Icons.Filled.PhotoCamera
                                BuiltInIcon.PIN -> Icons.Filled.LocationOn
                                BuiltInIcon.CHECK -> Icons.Filled.CheckCircle
                                BuiltInIcon.STAR -> Icons.Filled.Star
                                BuiltInIcon.DIVIDER -> Icons.Outlined.Remove
                            }, item.label
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private enum class ExportPreset(val label: String, val shortEdge: Int?) {
    ORIGINAL("原始", null),
    HD("1080P", 1080),
    TWO_K("2K", 1440),
    FOUR_K("4K", 2160)
}

@Composable
private fun ExportDialog(
    initialQuality: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    busy: Boolean,
    onDismiss: () -> Unit,
    onExport: (Int, ExportDimensions, Boolean) -> Unit
) {
    var quality by remember { mutableStateOf(initialQuality.toFloat()) }
    var aspect by remember(sourceWidth, sourceHeight) { mutableStateOf(ExportAspect.ORIGINAL) }
    var ratioLocked by remember(sourceWidth, sourceHeight) { mutableStateOf(true) }
    var selectedPreset by remember(sourceWidth, sourceHeight) { mutableStateOf<ExportPreset?>(ExportPreset.ORIGINAL) }
    val initialDimensions = remember(sourceWidth, sourceHeight) {
        originalExportDimensions(sourceWidth, sourceHeight, ExportAspect.ORIGINAL)
    }
    var widthText by remember(sourceWidth, sourceHeight) { mutableStateOf(initialDimensions.width.toString()) }
    var heightText by remember(sourceWidth, sourceHeight) { mutableStateOf(initialDimensions.height.toString()) }

    fun setDimensions(dimensions: ExportDimensions) {
        widthText = dimensions.width.toString()
        heightText = dimensions.height.toString()
    }

    fun dimensionsFor(newAspect: ExportAspect, preset: ExportPreset?): ExportDimensions {
        val ratio = newAspect.ratioFor(sourceWidth, sourceHeight)
        return when {
            preset == ExportPreset.ORIGINAL -> originalExportDimensions(sourceWidth, sourceHeight, newAspect)
            preset?.shortEdge != null -> exportDimensionsForShortEdge(preset.shortEdge, ratio)
            else -> exportDimensionsFromWidth(widthText.toIntOrNull() ?: sourceWidth, ratio)
        }
    }

    val dimensions = widthText.toIntOrNull()?.let { width ->
        heightText.toIntOrNull()?.let { height -> ExportDimensions(width, height) }
    }
    val validDimensions = dimensions?.takeIf(::isValidExportDimensions)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出照片") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("画面比例", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExportAspect.entries.forEach { item ->
                        FilterChip(
                            selected = aspect == item,
                            onClick = {
                                aspect = item
                                ratioLocked = true
                                setDimensions(dimensionsFor(item, selectedPreset))
                            },
                            label = { Text(item.label, maxLines = 1) },
                            enabled = !busy,
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Text("输出分辨率", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExportPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                ratioLocked = true
                                setDimensions(dimensionsFor(aspect, preset))
                            },
                            label = { Text(preset.label, maxLines = 1) },
                            enabled = !busy,
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { value ->
                            widthText = value.filter(Char::isDigit).take(4)
                            selectedPreset = null
                            widthText.toIntOrNull()?.let { width ->
                                val updated = exportDimensionsAfterWidthEdit(
                                    width = width,
                                    currentHeight = heightText.toIntOrNull() ?: 1,
                                    ratio = aspect.ratioFor(sourceWidth, sourceHeight),
                                    ratioLocked = ratioLocked
                                )
                                if (ratioLocked) heightText = updated.height.toString()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("宽度 px") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(
                        onClick = {
                            ratioLocked = !ratioLocked
                            if (ratioLocked) {
                                widthText.toIntOrNull()?.let { width ->
                                    setDimensions(
                                        exportDimensionsFromWidth(
                                            width,
                                            aspect.ratioFor(sourceWidth, sourceHeight)
                                        )
                                    )
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (ratioLocked) Icons.Outlined.Link else Icons.Outlined.LinkOff,
                            if (ratioLocked) "解除宽高比例" else "锁定宽高比例",
                            tint = if (ratioLocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { value ->
                            heightText = value.filter(Char::isDigit).take(4)
                            selectedPreset = null
                            heightText.toIntOrNull()?.let { height ->
                                val updated = exportDimensionsAfterHeightEdit(
                                    currentWidth = widthText.toIntOrNull() ?: 1,
                                    height = height,
                                    ratio = aspect.ratioFor(sourceWidth, sourceHeight),
                                    ratioLocked = ratioLocked
                                )
                                if (ratioLocked) widthText = updated.width.toString()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("高度 px") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Text(
                    when {
                        !ratioLocked -> "已解除比例锁定，可分别输入宽度和高度"
                        aspect == ExportAspect.ORIGINAL -> "保持当前画面比例"
                        else -> "比例变化时将从画面中央裁切"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dimensions != null && validDimensions == null) {
                    Text(
                        "单边最大 8192 px，且总像素不能超过 4000 万",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text("JPEG 质量 ${quality.toInt()}%")
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 60f..100f, enabled = !busy)
                if (busy) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp)); Text("正在生成…", Modifier.padding(start = 10.dp))
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { validDimensions?.let { onExport(quality.toInt(), it, true) } },
                    enabled = !busy && validDimensions != null
                ) { Text("保存并分享") }
                Button(
                    onClick = { validDimensions?.let { onExport(quality.toInt(), it, false) } },
                    enabled = !busy && validDimensions != null
                ) { Text("保存") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } }
    )
}
