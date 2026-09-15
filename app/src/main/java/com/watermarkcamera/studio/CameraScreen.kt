package com.watermarkcamera.studio

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Rational
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.Serializable
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class CameraAspect(
    val label: String,
    val landscapeRatio: Float,
    val cameraRatio: Int
) {
    FOUR_THREE("4:3", 4f / 3f, androidx.camera.core.AspectRatio.RATIO_4_3),
    SIXTEEN_NINE("16:9", 16f / 9f, androidx.camera.core.AspectRatio.RATIO_16_9),
    DINGTALK("钉钉", 2236f / 1200f, androidx.camera.core.AspectRatio.RATIO_16_9),
    SQUARE("1:1", 1f, androidx.camera.core.AspectRatio.RATIO_4_3);

    fun next(): CameraAspect = entries[(ordinal + 1) % entries.size]
}

enum class CameraLens { BACK, FRONT }

data class CameraShootingState(
    val lens: CameraLens = CameraLens.BACK,
    val flashEnabled: Boolean = false,
    val aspect: CameraAspect = CameraAspect.FOUR_THREE,
    val timerSeconds: Int = 0,
    val zoomRatio: Float = 1f
) : Serializable

@Composable
fun CameraScreen(
    previewLayers: List<WatermarkLayer>,
    templates: List<WatermarkTemplate>,
    initialTemplateId: String?,
    settings: AppSettings,
    hasDraft: Boolean,
    shootingState: CameraShootingState,
    onShootingStateChanged: (CameraShootingState) -> Unit,
    onPreviewLayersChanged: (List<WatermarkLayer>) -> Unit,
    onTemplateChanged: (String?) -> Unit,
    onSaveTemplate: (String, List<WatermarkLayer>) -> Unit,
    onPickFont: (String) -> Unit,
    onBack: () -> Unit,
    onCaptured: (Uri) -> Unit,
    onGallery: () -> Unit,
    onContinueDraft: () -> Unit,
    onTemplates: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val lensFacing = if (shootingState.lens == CameraLens.BACK) {
        CameraSelector.LENS_FACING_BACK
    } else {
        CameraSelector.LENS_FACING_FRONT
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraBindingGuard = remember { CameraBindingGuard() }
    val flashEnabled = shootingState.flashEnabled
    val aspect = shootingState.aspect
    val timerSeconds = shootingState.timerSeconds
    var countdown by remember { mutableIntStateOf(0) }
    var capturing by remember { mutableStateOf(false) }
    val zoomRatio = shootingState.zoomRatio
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var showZoomControls by remember { mutableStateOf(false) }
    var showTemplateMenu by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }
    var showExitConfirm by remember { mutableStateOf(false) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    val screenConfiguration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { screenConfiguration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { screenConfiguration.screenHeightDp.dp.toPx() }
    var propertyPanelOnRight by remember { mutableStateOf(true) }
    var propertyPanelOffsetX by remember { mutableFloatStateOf(0f) }
    var propertyPanelOffsetY by remember { mutableFloatStateOf(0f) }
    var currentPlace by remember { mutableStateOf<CurrentPlace?>(null) }
    var locationAttempted by remember { mutableStateOf(false) }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var activeTemplateId by remember {
        mutableStateOf(
            if (previewLayers.isEmpty()) templates.firstOrNull { it.id == initialTemplateId }?.id
            else initialTemplateId ?: "current"
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val controlRotation = rememberCameraControlRotation()
    val currentCameraState = androidx.compose.runtime.rememberUpdatedState(camera)
    val currentZoomRatioState = androidx.compose.runtime.rememberUpdatedState(zoomRatio)
    val currentMinZoomState = androidx.compose.runtime.rememberUpdatedState(minZoom)
    val currentShootingState = androidx.compose.runtime.rememberUpdatedState(shootingState)
    val currentOnShootingStateChanged = androidx.compose.runtime.rememberUpdatedState(onShootingStateChanged)

    fun updateShootingState(transform: (CameraShootingState) -> CameraShootingState) {
        currentOnShootingStateChanged.value(transform(currentShootingState.value))
    }

    fun layersForTemplate(template: WatermarkTemplate, targetAspect: CameraAspect): List<WatermarkLayer> {
        val adapted = BuiltInTemplates.forPortraitAspect(template, 1f / targetAspect.landscapeRatio)
        val layers = adapted.layers.map { it.copy(id = java.util.UUID.randomUUID().toString()) }
        return currentPlace?.let(layers::withCurrentPlace) ?: layers
    }

    fun markTemplateModified() {
        activeTemplateId = "current"
        onTemplateChanged(null)
    }
    val currentMaxZoomState = androidx.compose.runtime.rememberUpdatedState(maxZoom)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    BackHandler { showExitConfirm = true }
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("确认退出") },
            text = { Text("确定退出水印相机吗？") },
            confirmButton = {
                Button(onClick = {
                    showExitConfirm = false
                    onBack()
                }) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("取消") }
            }
        )
    }
    if (showSaveTemplate) {
        AlertDialog(
            onDismissRequest = { showSaveTemplate = false },
            title = { Text("保存为新模板") },
            text = {
                OutlinedTextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = { Text("模板名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newTemplateName.trim()
                        if (name.isNotEmpty()) {
                            onSaveTemplate(name, previewLayers.toList())
                            showSaveTemplate = false
                            error = "已保存新模板“$name”"
                        }
                    },
                    enabled = newTemplateName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplate = false }) { Text("取消") }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(granted) {
        if (granted && !locationGranted) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
    LaunchedEffect(granted, locationGranted) {
        if (granted && locationGranted && !locationAttempted) {
            locationAttempted = true
            SystemLocationService.locateOnce(context).onSuccess { systemPlace ->
                var resolved = systemPlace
                val amapAccepted = context.getSharedPreferences("privacy", 0)
                    .getBoolean("amap_accepted", false)
                if (resolved.address.isBlank() && amapAccepted && AmapService.hasApiKey(context)) {
                    AmapService.setPrivacyConsent(context, true)
                    AmapService.locateOnce(context).getOrNull()?.let { amapPlace ->
                        resolved = resolved.copy(address = amapPlace.watermarkText)
                    }
                }
                currentPlace = resolved
            }
        }
    }
    LaunchedEffect(currentPlace, previewLayers) {
        val place = currentPlace ?: return@LaunchedEffect
        val locationUpdated = previewLayers.withCurrentPlace(place)
        val activeTemplate = templates.firstOrNull { it.id == activeTemplateId }
        val updated = if (
            activeTemplate?.builtIn == true &&
            activeTemplate.id != BuiltInTemplateLayout.DINGTALK_TEMPLATE_ID
        ) {
            BuiltInTemplateLayout.arrange(
                templateId = activeTemplate.id,
                layers = locationUpdated,
                portraitAspectRatio = 1f / aspect.landscapeRatio
            )
        } else {
            locationUpdated
        }
        if (updated != previewLayers) onPreviewLayersChanged(updated)
    }
    LaunchedEffect(initialTemplateId, templates) {
        if (previewLayers.isEmpty() && initialTemplateId != null) {
            templates.firstOrNull { it.id == initialTemplateId }?.let { template ->
                activeTemplateId = template.id
                onPreviewLayersChanged(layersForTemplate(template, aspect))
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({ runCatching { providerFuture.get().unbindAll() } }, ContextCompat.getMainExecutor(context))
        }
    }

    if (!granted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("允许相机权限") }
            IconButton(onClick = { showExitConfirm = true }, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Icon(Icons.Outlined.ArrowBack, "返回")
            }
        }
        return
    }

    fun takePhoto() {
        if (capturing) return
        capturing = true
        scope.launch {
            if (timerSeconds > 0) {
                for (remaining in timerSeconds downTo 1) {
                    countdown = remaining
                    delay(1000)
                }
                countdown = 0
            }
            val capture = imageCapture
            if (capture == null) {
                capturing = false
                return@launch
            }
            val folder = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(folder, "photo_${System.currentTimeMillis()}.jpg")
            val options = ImageCapture.OutputFileOptions.Builder(file).build()
            capture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    ContextCompat.getMainExecutor(context).execute {
                        capturing = false
                        onCaptured(uri)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute {
                        capturing = false
                        error = exception.message ?: "拍照失败"
                    }
                }
            })
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(top = 56.dp, bottom = 104.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val portraitRatio = 1f / aspect.landscapeRatio
            val availableRatio = constraints.maxWidth.toFloat() / constraints.maxHeight
            val previewModifier = if (portraitRatio > availableRatio) {
                Modifier.fillMaxWidth().aspectRatio(portraitRatio)
            } else {
                Modifier.fillMaxHeight().aspectRatio(portraitRatio)
            }
            val availableWidth = constraints.maxWidth
            val availableHeight = constraints.maxHeight
            Box(previewModifier.clip(RoundedCornerShape(2.dp))) {
                key(lensFacing, aspect, availableWidth, availableHeight) {
                    val bindingToken = remember { Any() }
                    DisposableEffect(bindingToken) {
                        cameraBindingGuard.activate(bindingToken)
                        imageCapture = null
                        camera = null
                        onDispose {
                            cameraBindingGuard.invalidate(bindingToken)
                            imageCapture = null
                            camera = null
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                var pinchGesture = false
                                var gestureZoom = 1f
                                var downX = 0f
                                var downY = 0f
                                val scaleDetector = android.view.ScaleGestureDetector(
                                    ctx,
                                    object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                        override fun onScaleBegin(detector: android.view.ScaleGestureDetector): Boolean {
                                            pinchGesture = true
                                            gestureZoom = currentCameraState.value?.cameraInfo?.zoomState?.value?.zoomRatio
                                                ?: currentZoomRatioState.value
                                            return currentMaxZoomState.value > currentMinZoomState.value
                                        }

                                        override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                                            val nextZoom = (gestureZoom * detector.scaleFactor)
                                                .coerceIn(currentMinZoomState.value, currentMaxZoomState.value)
                                            gestureZoom = nextZoom
                                            updateShootingState { it.copy(zoomRatio = nextZoom) }
                                            currentCameraState.value?.cameraControl?.setZoomRatio(nextZoom)
                                            return true
                                        }
                                    }
                                )
                                setOnTouchListener { view, event ->
                                    scaleDetector.onTouchEvent(event)
                                    when (event.actionMasked) {
                                        android.view.MotionEvent.ACTION_DOWN -> {
                                            pinchGesture = false
                                            downX = event.x
                                            downY = event.y
                                        }
                                        android.view.MotionEvent.ACTION_POINTER_DOWN -> pinchGesture = true
                                        android.view.MotionEvent.ACTION_UP -> if (!pinchGesture &&
                                            kotlin.math.hypot(event.x - downX, event.y - downY) <= 16f * resources.displayMetrics.density
                                        ) {
                                        val point = meteringPointFactory.createPoint(event.x, event.y)
                                        currentCameraState.value?.cameraControl?.startFocusAndMetering(
                                            FocusMeteringAction.Builder(point)
                                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                                .build()
                                        )
                                        view.performClick()
                                    }
                                    }
                                    true
                                }
                                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                doOnLayout { previewView ->
                                    providerFuture.addListener(
                                        cameraProviderListener@{
                                        if (!cameraBindingGuard.isActive(bindingToken) || !previewView.isAttachedToWindow) {
                                            return@cameraProviderListener
                                        }
                                        runCatching {
                                            val provider = providerFuture.get()
                                            val targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                                            val viewportAspect = cameraViewportAspect(previewView.width, previewView.height)
                                            val viewPort = ViewPort.Builder(
                                                Rational(viewportAspect.width, viewportAspect.height),
                                                targetRotation
                                            )
                                                .setScaleType(ViewPort.FILL_CENTER)
                                                .build()
                                            val preview = Preview.Builder()
                                                .setTargetAspectRatio(aspect.cameraRatio)
                                                .setTargetRotation(targetRotation)
                                                .build()
                                                .also { it.surfaceProvider = surfaceProvider }
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                .setTargetAspectRatio(aspect.cameraRatio)
                                                .setTargetRotation(targetRotation)
                                                .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                                                .build()
                                            val useCases = UseCaseGroup.Builder()
                                                .setViewPort(viewPort)
                                                .addUseCase(preview)
                                                .addUseCase(capture)
                                                .build()
                                            provider.unbindAll()
                                            val boundCamera = provider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                                                useCases
                                            )
                                            if (!cameraBindingGuard.isActive(bindingToken)) {
                                                provider.unbind(preview, capture)
                                                return@runCatching
                                            }
                                            camera = boundCamera
                                            imageCapture = capture
                                            val zoomState = boundCamera.cameraInfo.zoomState.value
                                            minZoom = zoomState?.minZoomRatio ?: 1f
                                            maxZoom = minOf(zoomState?.maxZoomRatio ?: 1f, 8f)
                                            val restoredZoom = currentShootingState.value.zoomRatio.coerceIn(minZoom, maxZoom)
                                            boundCamera.cameraControl.setZoomRatio(restoredZoom)
                                            if (restoredZoom != currentShootingState.value.zoomRatio) {
                                                updateShootingState { it.copy(zoomRatio = restoredZoom) }
                                            }
                                        }.onFailure {
                                            if (cameraBindingGuard.isActive(bindingToken)) {
                                                error = it.message ?: "无法启动相机"
                                            }
                                        }
                                        },
                                        ContextCompat.getMainExecutor(ctx)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CameraWatermarkPreview(
                    layers = previewLayers,
                    showGuides = settings.showGuides,
                    selectedId = selectedLayerId,
                    onSelected = {
                        showZoomControls = false
                        selectedLayerId = it
                    },
                    onLayerMove = { id, dx, dy ->
                        markTemplateModified()
                        onPreviewLayersChanged(previewLayers.map { layer ->
                            if (layer.id == id && !layer.locked) {
                                layer.copy(
                                    x = (layer.x + dx).coerceIn(0f, 1f),
                                    y = (layer.y + dy).coerceIn(0f, 1f)
                                )
                            } else {
                                layer
                            }
                        })
                    },
                    onLayerResize = { base, widthDelta, heightDelta, preserveAspect ->
                        markTemplateModified()
                        val updated = resizeWatermarkLayer(base, widthDelta, heightDelta, preserveAspect)
                        onPreviewLayersChanged(previewLayers.map { if (it.id == base.id) updated else it })
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Color(0xD90B0D0E),
                    RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraRoundButton(onClick = { showExitConfirm = true }, rotation = controlRotation) { Icon(Icons.Outlined.ArrowBack, "返回", tint = Color.White) }
            CameraRoundButton(onClick = {
                val nextAspect = aspect.next()
                imageCapture = null
                camera = null
                updateShootingState { it.copy(aspect = nextAspect) }
                selectedLayerId = null
                templates.firstOrNull { it.id == activeTemplateId }
                    ?.takeIf { it.builtIn && it.id != BuiltInTemplateLayout.DINGTALK_TEMPLATE_ID }
                    ?.let { onPreviewLayersChanged(layersForTemplate(it, nextAspect)) }
            }, rotation = controlRotation) {
                Column(
                    Modifier.size(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.AspectRatio, "拍摄比例 ${aspect.label}", tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(aspect.label, color = Color.White, fontSize = 9.sp, lineHeight = 10.sp)
                }
            }
            CameraRoundButton(onClick = {
                val nextTimer = when (timerSeconds) { 0 -> 3; 3 -> 5; 5 -> 10; else -> 0 }
                updateShootingState { it.copy(timerSeconds = nextTimer) }
            }, rotation = controlRotation) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Timer, "倒计时 ${timerSeconds}秒", tint = Color.White, modifier = Modifier.size(24.dp))
                    if (timerSeconds > 0) {
                        Text(
                            text = "${timerSeconds}s",
                            color = Color.Black,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color(0xFFFFC857), RoundedCornerShape(5.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            CameraRoundButton(
                onClick = {
                    val enabled = !flashEnabled
                    updateShootingState { it.copy(flashEnabled = enabled) }
                    imageCapture?.flashMode = if (enabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                },
                enabled = camera?.cameraInfo?.hasFlashUnit() != false,
                rotation = controlRotation
            ) {
                Icon(if (flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, "闪光灯", tint = Color.White)
            }
            CameraRoundButton(onClick = {
                imageCapture = null
                camera = null
                updateShootingState {
                    it.copy(
                        lens = if (it.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK,
                        flashEnabled = false
                    )
                }
            }, rotation = controlRotation) {
                Icon(Icons.Outlined.Cameraswitch, "切换镜头", tint = Color.White)
            }
        }

        val selectedLayer = previewLayers.firstOrNull { it.id == selectedLayerId }
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .background(Color(0xFF0B0D0E))
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraUtilityButton(
                    icon = Icons.Outlined.PhotoLibrary,
                    description = "打开系统相册",
                    rotation = controlRotation,
                    onClick = onGallery
                )
            }
            if (maxZoom > minZoom) {
                CameraZoomButton(
                    zoomRatio = zoomRatio,
                    rotation = controlRotation,
                    onClick = {
                        selectedLayerId = null
                        showZoomControls = true
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = cameraSideControlOffset(CameraControlSide.LEFT, 72f).dp)
                )
            }
            IconButton(
                onClick = ::takePhoto,
                enabled = !capturing,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
                    .background(Color(0xFF0B0D0E), CircleShape)
                    .border(3.dp, Color.White, CircleShape)
            ) {
                Box(
                    Modifier
                        .size(62.dp)
                        .background(if (capturing) Color(0xFF707776) else Color(0xFF20A58B), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = .22f), CircleShape)
                )
            }
            if (templates.isNotEmpty() || previewLayers.isNotEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset(x = cameraSideControlOffset(CameraControlSide.RIGHT, 72f).dp)
                ) {
                        CameraUtilityButton(
                            icon = Icons.Outlined.Layers,
                            description = "选择水印模板",
                            rotation = controlRotation,
                            selected = showTemplateMenu,
                            onClick = { showTemplateMenu = true }
                        )
                        DropdownMenu(
                            expanded = showTemplateMenu,
                            onDismissRequest = { showTemplateMenu = false },
                            modifier = Modifier.heightIn(max = 360.dp),
                            shape = RoundedCornerShape(8.dp),
                            containerColor = Color(0xFF202423)
                        ) {
                            if (hasDraft) {
                                CameraMenuItem(
                                    label = "继续上次编辑",
                                    icon = Icons.Outlined.History,
                                    onClick = {
                                        showTemplateMenu = false
                                        onContinueDraft()
                                    }
                                )
                            }
                            CameraMenuItem(
                                label = "管理模板",
                                icon = Icons.Outlined.Layers,
                                onClick = {
                                    showTemplateMenu = false
                                    onTemplates()
                                }
                            )
                            if (previewLayers.isNotEmpty()) {
                                CameraMenuItem(
                                    label = "保存为新模板",
                                    icon = Icons.Outlined.Save,
                                    onClick = {
                                        showTemplateMenu = false
                                        newTemplateName = "我的水印 ${templates.count { !it.builtIn } + 1}"
                                        showSaveTemplate = true
                                    }
                                )
                            }
                            CameraMenuItem(
                                label = "无水印",
                                icon = Icons.Outlined.Remove,
                                selected = activeTemplateId == null,
                                onClick = {
                                    showTemplateMenu = false
                                    activeTemplateId = null
                                    selectedLayerId = null
                                    onTemplateChanged(null)
                                    onPreviewLayersChanged(emptyList())
                                }
                            )
                            if (previewLayers.isNotEmpty() && activeTemplateId == "current") {
                                CameraMenuItem(
                                    label = "当前模板",
                                    icon = Icons.Outlined.Layers,
                                    selected = true,
                                    onClick = { showTemplateMenu = false }
                                )
                            }
                            templates.forEach { template ->
                                CameraMenuItem(
                                    label = template.name,
                                    icon = Icons.Outlined.Layers,
                                    selected = activeTemplateId == template.id,
                                    onClick = {
                                        showTemplateMenu = false
                                        activeTemplateId = template.id
                                        selectedLayerId = null
                                        onTemplateChanged(template.id)
                                        val targetAspect = when {
                                            template.id == BuiltInTemplateLayout.DINGTALK_TEMPLATE_ID -> CameraAspect.DINGTALK
                                            aspect == CameraAspect.DINGTALK -> CameraAspect.FOUR_THREE
                                            else -> aspect
                                        }
                                        updateShootingState { it.copy(aspect = targetAspect) }
                                        onPreviewLayersChanged(layersForTemplate(template, targetAspect))
                                    }
                                )
                            }
                        }
                }
            }
            CameraUtilityButton(
                Icons.Outlined.Settings,
                "设置",
                controlRotation,
                onClick = onSettings,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (selectedLayer == null && maxZoom > minZoom && showZoomControls) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { showZoomControls = false }
                    }
            )
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, bottom = 112.dp)
                    .height(52.dp)
                    .background(Color(0xE6202423), RoundedCornerShape(26.dp))
                    .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.ZoomOut, "缩小", tint = Color.White, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = controlRotation })
                Slider(
                    value = zoomRatio,
                    onValueChange = {
                        updateShootingState { state -> state.copy(zoomRatio = it) }
                        camera?.cameraControl?.setZoomRatio(it)
                    },
                    valueRange = minZoom..maxZoom,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Icon(Icons.Outlined.ZoomIn, "放大", tint = Color.White, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = controlRotation })
                TextButton(onClick = { showZoomControls = false }) {
                    Text(String.format(Locale.getDefault(), "%.1fx", zoomRatio), color = Color.White)
                }
            }
        }

        selectedLayer?.let { layer ->
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
                layer = layer,
                onClose = { selectedLayerId = null },
                onCheckpoint = {},
                onChange = { updated ->
                    markTemplateModified()
                    onPreviewLayersChanged(previewLayers.map { if (it.id == updated.id) updated else it })
                },
                onPickFont = { onPickFont(layer.id) },
                onDuplicate = {
                    val copy = layer.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        x = (layer.x + .04f).coerceAtMost(1f),
                        y = (layer.y + .04f).coerceAtMost(1f)
                    )
                    markTemplateModified()
                    selectedLayerId = copy.id
                    onPreviewLayersChanged(previewLayers + copy)
                },
                onDelete = {
                    markTemplateModified()
                    selectedLayerId = null
                    onPreviewLayersChanged(previewLayers.filterNot { it.id == layer.id })
                },
                onMove = { delta ->
                    val index = previewLayers.indexOfFirst { it.id == layer.id }
                    val target = (index + delta).coerceIn(0, previewLayers.lastIndex)
                    if (index >= 0 && target != index) {
                        val reordered = previewLayers.toMutableList()
                        reordered.add(target, reordered.removeAt(index))
                        markTemplateModified()
                        onPreviewLayersChanged(reordered)
                    }
                }
            )
        }

        if (countdown > 0) {
            Text(
                countdown.toString(),
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = .35f), CircleShape).padding(22.dp)
            )
        }
        error?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = .7f), RoundedCornerShape(6.dp)).padding(12.dp)
            )
            LaunchedEffect(it) { delay(2200); error = null }
        }
    }
}

@Composable
private fun CameraRoundButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    rotation: Float = 0f,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }, contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun CameraUtilityButton(
    icon: ImageVector,
    description: String,
    rotation: Float,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .background(if (selected) Color(0xFF176B5B) else Color(0xFF242827), CircleShape)
            .border(1.dp, Color.White.copy(alpha = .12f), CircleShape)
    ) {
        Icon(
            icon,
            description,
            tint = if (selected) Color(0xFF9CF0DA) else Color.White,
            modifier = Modifier.size(23.dp).graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun CameraZoomButton(
    zoomRatio: Float,
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .background(Color(0xFF242827), CircleShape)
            .border(1.dp, Color.White.copy(alpha = .12f), CircleShape)
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%.1fx", zoomRatio),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun CameraMenuItem(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = Color(0xFFF0F4F2),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                icon,
                null,
                tint = if (selected) Color(0xFF70D8C0) else Color(0xFFBFC8C3),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (selected) {
            { Icon(Icons.Filled.Check, null, tint = Color(0xFF70D8C0), modifier = Modifier.size(18.dp)) }
        } else null,
        onClick = onClick
    )
}

@Composable
private fun rememberCameraControlRotation(): Float {
    val context = LocalContext.current
    var rotation by remember { mutableFloatStateOf(0f) }
    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                rotation = when (orientation) {
                    in 45..134 -> -90f
                    in 135..224 -> 180f
                    in 225..314 -> 90f
                    else -> 0f
                }
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    return rotation
}

@Composable
private fun CameraWatermarkPreview(
    layers: List<WatermarkLayer>,
    showGuides: Boolean,
    selectedId: String?,
    onSelected: (String) -> Unit,
    onLayerMove: (String, Float, Float) -> Unit,
    onLayerResize: (WatermarkLayer, Float, Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var timeTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timeTick++
        }
    }
    BoxWithConstraints(modifier) {
        val canvasWidthPx = constraints.maxWidth.toFloat()
        val canvasHeightPx = constraints.maxHeight.toFloat()
        if (showGuides) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(Color.White.copy(alpha = .3f), androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), 1.dp.toPx())
                drawLine(Color.White.copy(alpha = .3f), androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), 1.dp.toPx())
            }
        }
        layers.filterNot { it.hidden }.forEach { layer ->
            key(layer.id, timeTick.takeIf { layer.kind == LayerKind.TIME }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CameraWatermarkLayer(
                        layer = layer,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                        selected = layer.id == selectedId,
                        onClick = { onSelected(layer.id) },
                        onMove = { dx, dy -> onLayerMove(layer.id, dx, dy) },
                        onResize = onLayerResize
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraWatermarkLayer(
    layer: WatermarkLayer,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    selected: Boolean,
    onClick: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (WatermarkLayer, Float, Float, Boolean) -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val canvasScale = minOf(canvasWidthPx, canvasHeightPx) / (360f * density)
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, layer.imageUri) {
        value = layer.imageUri?.let { ImageProcessing.loadBitmap(context, Uri.parse(it), 1024) }
    }
    val currentLayer = androidx.compose.runtime.rememberUpdatedState(layer)
    val currentOnClick = androidx.compose.runtime.rememberUpdatedState(onClick)
    val currentOnMove = androidx.compose.runtime.rememberUpdatedState(onMove)
    val currentOnResize = androidx.compose.runtime.rememberUpdatedState(onResize)
    val fontFamily = remember(layer.font, layer.fontUri, layer.bold) { FontFamily(FontSupport.resolve(context, layer)) }
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
        }.pointerInput(layer.id, layer.locked, selected) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val gestureLayer = currentLayer.value
                val edgeX = minOf(10.dp.toPx(), size.width * .18f)
                val edgeY = minOf(10.dp.toPx(), size.height * .18f)
                val resizeX = if (selected && !gestureLayer.locked) when {
                    down.position.x <= edgeX -> -1f
                    down.position.x >= size.width - edgeX -> 1f
                    else -> 0f
                } else 0f
                val resizeY = if (selected && !gestureLayer.locked) when {
                    down.position.y <= edgeY -> -1f
                    down.position.y >= size.height - edgeY -> 1f
                    else -> 0f
                } else 0f
                val resizeEdgeTouched = resizeX != 0f || resizeY != 0f
                var action = watermarkGestureAction(gestureLayer.locked, resizeEdgeTouched, false)
                var accumulatedResizePan = androidx.compose.ui.geometry.Offset.Zero
                down.consume()
                currentOnClick.value()

                if (action == WatermarkGestureAction.SELECT_ONLY && !gestureLayer.locked) {
                    var canceled = false
                    val completedBeforeTimeout = withTimeoutOrNull(350L) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed ||
                                (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                            ) {
                                canceled = true
                                return@withTimeoutOrNull true
                            }
                        }
                    }
                    if (!canceled && completedBeforeTimeout == null) {
                        action = watermarkGestureAction(false, false, true)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                if (action != WatermarkGestureAction.SELECT_ONLY) {
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        if (action == WatermarkGestureAction.RESIZE) {
                            accumulatedResizePan += pan
                            currentOnResize.value(
                                gestureLayer,
                                resizeX * accumulatedResizePan.x / canvasWidthPx,
                                resizeY * accumulatedResizePan.y / canvasHeightPx,
                                resizeX != 0f && resizeY != 0f
                            )
                        } else {
                            val canvasPan = pan.toCameraCanvasPan(currentLayer.value)
                            currentOnMove.value(
                                canvasPan.x / canvasWidthPx,
                                canvasPan.y / canvasHeightPx
                            )
                        }
                        event.changes.forEach { change ->
                            if (change.positionChanged()) change.consume()
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
        }.then(
            if (selected) Modifier.border(1.dp, Color(0xFF4A90E2), RoundedCornerShape(2.dp))
            else Modifier
        ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
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
            }
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
                modifier = Modifier.size(48.dp)
            )
            LayerKind.LOCATION -> LocationWatermarkContent(layer, bitmap, fontFamily)
            else -> Text(
                text = layer.displayText(),
                color = Color(layer.color),
                fontSize = layer.fontSize.sp,
                fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = fontFamily,
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
}

private fun androidx.compose.ui.geometry.Offset.toCameraCanvasPan(layer: WatermarkLayer): androidx.compose.ui.geometry.Offset {
    val radians = Math.toRadians(layer.rotation.toDouble())
    val scaledX = x * layer.scaleX
    val scaledY = y * layer.scaleY
    val cosine = kotlin.math.cos(radians).toFloat()
    val sine = kotlin.math.sin(radians).toFloat()
    return androidx.compose.ui.geometry.Offset(
        x = scaledX * cosine - scaledY * sine,
        y = scaledX * sine + scaledY * cosine
    )
}
