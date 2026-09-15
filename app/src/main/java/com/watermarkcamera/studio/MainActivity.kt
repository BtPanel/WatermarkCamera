package com.watermarkcamera.studio

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        setContent {
            val context = LocalContext.current
            val store = remember { ProjectStore(context) }
            var settings by remember { mutableStateOf(store.loadSettings()) }
            var page by rememberSaveable { mutableStateOf(AppPage.CAMERA) }
            var cameraShootingState by rememberSaveable {
                mutableStateOf(
                    CameraShootingState(
                        aspect = if (store.loadCameraTemplateId() == BuiltInTemplateLayout.DINGTALK_TEMPLATE_ID) {
                            CameraAspect.DINGTALK
                        } else {
                            CameraAspect.FOUR_THREE
                        }
                    )
                )
            }
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
            var imageLoadStatus by remember { mutableStateOf(ImageLoadStatus.IDLE) }
            var imageLoadRevision by remember { mutableIntStateOf(0) }
            var photoTransform by rememberSaveable { mutableStateOf(PhotoTransform()) }
            var sourceUri by rememberSaveable { mutableStateOf<String?>(null) }
            val layers = rememberSaveable(
                saver = Saver(
                    save = { ArrayList(it) },
                    restore = { it.toMutableStateList() }
                )
            ) { mutableStateListOf<WatermarkLayer>() }
            var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
            var pendingLogoPicker by rememberSaveable { mutableStateOf(false) }
            var pendingFontLayerId by rememberSaveable { mutableStateOf<String?>(null) }
            var pendingTemplate by rememberSaveable { mutableStateOf(false) }
            var templateReturnPage by rememberSaveable { mutableStateOf(AppPage.CAMERA) }
            var templateRevision by remember { mutableIntStateOf(0) }
            val availableTemplates = remember(templateRevision, page) {
                BuiltInTemplates.all + store.loadTemplates()
            }

            BackHandler(enabled = page != AppPage.CAMERA) {
                appBackDestination(page, templateReturnPage)?.let { destination ->
                    if (shouldPreserveCameraWatermark(page, destination, layers.isNotEmpty())) {
                        pendingTemplate = true
                    }
                    page = destination
                }
            }

            fun handlePickedImage(uri: Uri?) {
                if (uri != null) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    if (pendingLogoPicker) {
                        val layer = WatermarkLayer(kind = LayerKind.IMAGE, imageUri = uri.toString(), alpha = settings.defaultOpacity)
                        layers += layer
                        selectedId = layer.id
                        pendingLogoPicker = false
                    } else {
                        sourceUri = uri.toString()
                        photoTransform = PhotoTransform()
                        if (!pendingTemplate) layers.clear()
                        pendingTemplate = false
                        selectedId = null
                        page = AppPage.EDITOR
                    }
                } else {
                    pendingLogoPicker = false
                }
            }

            val imageFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), ::handlePickedImage)
            val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                pendingLogoPicker = false
                handlePickedImage(uri)
            }

            val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                val layerId = pendingFontLayerId
                pendingFontLayerId = null
                if (uri != null && layerId != null) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val displayName = runCatching {
                        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                    }.getOrNull()
                    val index = layers.indexOfFirst { it.id == layerId }
                    if (index >= 0) {
                        layers[index] = layers[index].copy(
                            font = WatermarkFont.CUSTOM,
                            fontUri = uri.toString(),
                            fontName = displayName ?: "自定义字体"
                        )
                    }
                }
            }

            LaunchedEffect(sourceUri, imageLoadRevision) {
                val uri = sourceUri?.let(Uri::parse)
                if (uri == null) {
                    bitmap = null
                    imageLoadStatus = ImageLoadStatus.IDLE
                    return@LaunchedEffect
                }
                bitmap = null
                imageLoadStatus = ImageLoadStatus.LOADING
                val loaded = ImageProcessing.loadBitmap(context, uri)
                bitmap = loaded
                imageLoadStatus = completedImageLoadStatus(loaded != null)
            }

            StudioTheme(darkTheme = settings.darkMode) {
                val lightSystemBarIcons = usesLightSystemBarIcons(page, settings.darkMode)
                SideEffect {
                    val transparent = android.graphics.Color.TRANSPARENT
                    val systemBarStyle = if (lightSystemBarIcons) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    }
                    enableEdgeToEdge(
                        statusBarStyle = systemBarStyle,
                        navigationBarStyle = systemBarStyle
                    )
                }
                val immersivePage = page == AppPage.CAMERA || page == AppPage.EDITOR || page == AppPage.CROP
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (immersivePage) androidx.compose.ui.graphics.Color(0xFF111315)
                            else MaterialTheme.colorScheme.background
                        )
                ) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        when (page) {
                    AppPage.CAMERA -> CameraScreen(
                        previewLayers = layers,
                        templates = availableTemplates,
                        initialTemplateId = store.loadCameraTemplateId(),
                        settings = settings,
                        hasDraft = store.loadDraft() != null,
                        shootingState = cameraShootingState,
                        onShootingStateChanged = { cameraShootingState = it },
                        onPreviewLayersChanged = { updated ->
                            layers.clear()
                            layers.addAll(updated)
                            selectedId = null
                            pendingTemplate = updated.isNotEmpty()
                        },
                        onTemplateChanged = { store.saveCameraTemplateId(it) },
                        onSaveTemplate = { name, templateLayers ->
                            store.saveTemplate(
                                WatermarkTemplate(
                                    name = name,
                                    layers = templateLayers,
                                    category = TemplateCategory.CUSTOM
                                )
                            )
                            templateRevision++
                        },
                        onPickFont = { layerId ->
                            pendingFontLayerId = layerId
                            fontPicker.launch(arrayOf(
                                "font/*",
                                "application/x-font-ttf",
                                "application/x-font-opentype",
                                "application/vnd.ms-opentype",
                                "application/octet-stream"
                            ))
                        },
                        onBack = { finish() },
                        onCaptured = { uri ->
                            sourceUri = uri.toString()
                            photoTransform = PhotoTransform()
                            pendingTemplate = layers.isNotEmpty()
                            selectedId = null
                            page = AppPage.EDITOR
                        },
                        onGallery = {
                            pendingLogoPicker = false
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onContinueDraft = {
                            store.loadDraft()?.let { draft ->
                                sourceUri = draft.sourceUri
                                layers.clear()
                                layers.addAll(draft.layers)
                                photoTransform = draft.photoTransform
                                selectedId = null
                                page = AppPage.EDITOR
                            }
                        },
                        onTemplates = {
                            templateReturnPage = AppPage.CAMERA
                            page = AppPage.TEMPLATES
                        },
                        onSettings = { page = AppPage.SETTINGS }
                    )
                    AppPage.EDITOR -> EditorScreen(
                        bitmap = bitmap,
                        imageLoadStatus = imageLoadStatus,
                        photoTransform = photoTransform,
                        onPhotoTransformChanged = { photoTransform = it },
                        layers = layers,
                        selectedId = selectedId,
                        onSelected = { selectedId = it },
                        onLayersChanged = { updated ->
                            layers.clear()
                            layers.addAll(updated)
                        },
                        onBack = {
                            pendingTemplate = layers.isNotEmpty()
                            page = AppPage.CAMERA
                        },
                        onRetryImage = { imageLoadRevision++ },
                        onChooseImage = {
                            pendingLogoPicker = false
                            pendingTemplate = layers.isNotEmpty()
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onCrop = {
                            if (!photoTransform.isIdentity()) {
                                bitmap = bitmap?.let { ImageProcessing.applyPhotoTransform(it, photoTransform) }
                                photoTransform = PhotoTransform()
                            }
                            page = AppPage.CROP
                        },
                        onPickLogo = {
                            pendingLogoPicker = true
                            imageFilePicker.launch(arrayOf("image/*"))
                        },
                        onPickFont = { layerId ->
                            pendingFontLayerId = layerId
                            fontPicker.launch(arrayOf(
                                "font/*",
                                "application/x-font-ttf",
                                "application/x-font-opentype",
                                "application/vnd.ms-opentype",
                                "application/octet-stream"
                            ))
                        },
                        onSaveDraft = {
                            bitmap?.let {
                                val draftUri = ImageProcessing.saveDraftBitmap(context, it)
                                sourceUri = draftUri.toString()
                                store.saveDraft(sourceUri, layers, photoTransform)
                            }
                        },
                        onTemplates = {
                            templateReturnPage = AppPage.EDITOR
                            page = AppPage.TEMPLATES
                        },
                        settings = settings
                    )
                    AppPage.CROP -> bitmap?.let { source ->
                        CropScreen(
                            bitmap = source,
                            onCancel = { page = AppPage.EDITOR },
                            onApply = { cropped ->
                                bitmap = cropped
                                photoTransform = PhotoTransform()
                                sourceUri = ImageProcessing.saveDraftBitmap(context, cropped).toString()
                                page = AppPage.EDITOR
                            }
                        )
                    } ?: run { page = AppPage.CAMERA }
                    AppPage.TEMPLATES -> TemplateScreen(
                        store = store,
                        currentLayers = layers,
                        canSave = layers.isNotEmpty(),
                        onBack = { page = templateReturnPage },
                        onApply = { template ->
                            val portraitAspect = if (templateReturnPage == AppPage.EDITOR && bitmap != null) {
                                bitmap!!.width.toFloat() / bitmap!!.height
                            } else {
                                BuiltInTemplateLayout.DEFAULT_PORTRAIT_ASPECT
                            }
                            val appliedTemplate = BuiltInTemplates.forPortraitAspect(template, portraitAspect)
                            layers.clear()
                            layers.addAll(appliedTemplate.layers.map { it.copy(id = java.util.UUID.randomUUID().toString()) })
                            store.saveCameraTemplateId(template.id)
                            if (templateReturnPage == AppPage.CAMERA) {
                                val targetAspect = when {
                                    template.id == BuiltInTemplateLayout.DINGTALK_TEMPLATE_ID -> CameraAspect.DINGTALK
                                    cameraShootingState.aspect == CameraAspect.DINGTALK -> CameraAspect.FOUR_THREE
                                    else -> cameraShootingState.aspect
                                }
                                cameraShootingState = cameraShootingState.copy(aspect = targetAspect)
                            }
                            selectedId = null
                            pendingTemplate = templateReturnPage == AppPage.CAMERA
                            page = templateReturnPage
                        }
                    )
                            AppPage.SETTINGS -> SettingsScreen(
                                settings = settings,
                                onChange = {
                                    settings = it
                                    store.saveSettings(it)
                                },
                                onBack = { page = AppPage.CAMERA }
                            )
                        }
                    }
                }
            }
        }
    }
}
