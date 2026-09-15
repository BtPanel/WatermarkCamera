package com.watermarkcamera.studio

internal data class CameraViewportAspect(val width: Int, val height: Int)

internal class CameraBindingGuard {
    private var activeToken: Any? = null

    fun activate(token: Any) {
        activeToken = token
    }

    fun invalidate(token: Any) {
        if (activeToken === token) activeToken = null
    }

    fun isActive(token: Any): Boolean = activeToken === token
}

internal fun cameraViewportAspect(previewWidth: Int, previewHeight: Int): CameraViewportAspect {
    require(previewWidth > 0 && previewHeight > 0) { "Preview dimensions must be positive" }

    val divisor = greatestCommonDivisor(previewWidth, previewHeight)
    return CameraViewportAspect(previewWidth / divisor, previewHeight / divisor)
}

private tailrec fun greatestCommonDivisor(left: Int, right: Int): Int =
    if (right == 0) left else greatestCommonDivisor(right, left % right)
