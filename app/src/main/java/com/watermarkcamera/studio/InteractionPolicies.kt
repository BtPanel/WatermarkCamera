package com.watermarkcamera.studio

enum class CameraControlSide { LEFT, RIGHT }

fun cameraSideControlOffset(side: CameraControlSide, distanceDp: Float): Float =
    if (side == CameraControlSide.LEFT) -distanceDp else distanceDp

enum class WatermarkGestureAction { SELECT_ONLY, MOVE, RESIZE }

fun watermarkGestureAction(
    locked: Boolean,
    resizeEdgeTouched: Boolean,
    longPressReached: Boolean
): WatermarkGestureAction = when {
    locked -> WatermarkGestureAction.SELECT_ONLY
    resizeEdgeTouched -> WatermarkGestureAction.RESIZE
    longPressReached -> WatermarkGestureAction.MOVE
    else -> WatermarkGestureAction.SELECT_ONLY
}

enum class ImageLoadStatus { IDLE, LOADING, READY, ERROR }

fun completedImageLoadStatus(bitmapAvailable: Boolean): ImageLoadStatus =
    if (bitmapAvailable) ImageLoadStatus.READY else ImageLoadStatus.ERROR

fun appBackDestination(currentPage: AppPage, templateReturnPage: AppPage): AppPage? = when (currentPage) {
    AppPage.CAMERA -> null
    AppPage.EDITOR -> AppPage.CAMERA
    AppPage.CROP -> AppPage.EDITOR
    AppPage.TEMPLATES -> templateReturnPage
    AppPage.SETTINGS -> AppPage.CAMERA
}

fun usesLightSystemBarIcons(page: AppPage, darkTheme: Boolean): Boolean =
    darkTheme || page == AppPage.CAMERA || page == AppPage.EDITOR || page == AppPage.CROP

fun shouldPreserveCameraWatermark(
    currentPage: AppPage,
    destination: AppPage,
    hasLayers: Boolean
): Boolean = currentPage == AppPage.EDITOR && destination == AppPage.CAMERA && hasLayers
