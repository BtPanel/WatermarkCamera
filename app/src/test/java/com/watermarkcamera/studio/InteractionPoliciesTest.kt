package com.watermarkcamera.studio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class InteractionPoliciesTest {
    @Test
    fun `completed image decode without bitmap is an error`() {
        assertEquals(ImageLoadStatus.ERROR, completedImageLoadStatus(bitmapAvailable = false))
    }

    @Test
    fun `completed image decode with bitmap is ready`() {
        assertEquals(ImageLoadStatus.READY, completedImageLoadStatus(bitmapAvailable = true))
    }

    @Test
    fun `watermark layers survive saved state serialization`() {
        val layer = WatermarkLayer(
            id = "layer-1",
            kind = LayerKind.IMAGE,
            imageUri = "content://photos/1",
            x = .24f,
            y = .73f,
            font = WatermarkFont.CUSTOM,
            fontUri = "content://fonts/1"
        )

        assertEquals(layer, serializeRoundTrip(layer))
    }

    @Test
    fun `photo transform survives saved state serialization`() {
        val transform = PhotoTransform(scale = 2.25f, offsetX = .12f, offsetY = -.08f)

        assertEquals(transform, serializeRoundTrip(transform))
    }

    @Test
    fun `side controls are equally spaced around shutter`() {
        val left = cameraSideControlOffset(CameraControlSide.LEFT, 72f)
        val right = cameraSideControlOffset(CameraControlSide.RIGHT, 72f)

        assertEquals(-72f, left, 0f)
        assertEquals(72f, right, 0f)
        assertEquals(0f, left + right, 0f)
    }

    @Test
    fun `tap selects watermark without moving it`() {
        assertEquals(
            WatermarkGestureAction.SELECT_ONLY,
            watermarkGestureAction(
                locked = false,
                resizeEdgeTouched = false,
                longPressReached = false
            )
        )
    }

    @Test
    fun `long press inside watermark enables movement`() {
        assertEquals(
            WatermarkGestureAction.MOVE,
            watermarkGestureAction(
                locked = false,
                resizeEdgeTouched = false,
                longPressReached = true
            )
        )
    }

    @Test
    fun `selected edge keeps resize behavior without waiting for long press`() {
        assertEquals(
            WatermarkGestureAction.RESIZE,
            watermarkGestureAction(
                locked = false,
                resizeEdgeTouched = true,
                longPressReached = false
            )
        )
    }

    @Test
    fun `locked watermark cannot move or resize`() {
        assertEquals(
            WatermarkGestureAction.SELECT_ONLY,
            watermarkGestureAction(
                locked = true,
                resizeEdgeTouched = false,
                longPressReached = true
            )
        )
        assertEquals(
            WatermarkGestureAction.SELECT_ONLY,
            watermarkGestureAction(
                locked = true,
                resizeEdgeTouched = true,
                longPressReached = true
            )
        )
    }

    @Test
    fun `system back returns from work pages without exiting the camera`() {
        assertEquals(AppPage.CAMERA, appBackDestination(AppPage.EDITOR, AppPage.CAMERA))
        assertEquals(AppPage.EDITOR, appBackDestination(AppPage.CROP, AppPage.CAMERA))
        assertEquals(AppPage.CAMERA, appBackDestination(AppPage.SETTINGS, AppPage.CAMERA))
        assertEquals(AppPage.EDITOR, appBackDestination(AppPage.TEMPLATES, AppPage.EDITOR))
    }

    @Test
    fun `camera page owns the app exit confirmation`() {
        assertEquals(null, appBackDestination(AppPage.CAMERA, AppPage.CAMERA))
    }

    @Test
    fun `immersive work pages use light system bar icons`() {
        assertEquals(true, usesLightSystemBarIcons(AppPage.CAMERA, darkTheme = false))
        assertEquals(true, usesLightSystemBarIcons(AppPage.EDITOR, darkTheme = false))
        assertEquals(true, usesLightSystemBarIcons(AppPage.CROP, darkTheme = false))
    }

    @Test
    fun `standard pages match system bar icons to the theme`() {
        assertEquals(false, usesLightSystemBarIcons(AppPage.TEMPLATES, darkTheme = false))
        assertEquals(false, usesLightSystemBarIcons(AppPage.SETTINGS, darkTheme = false))
        assertEquals(true, usesLightSystemBarIcons(AppPage.TEMPLATES, darkTheme = true))
        assertEquals(true, usesLightSystemBarIcons(AppPage.SETTINGS, darkTheme = true))
    }

    @Test
    fun `camera shooting state survives saved state serialization`() {
        val state = CameraShootingState(
            lens = CameraLens.FRONT,
            flashEnabled = true,
            aspect = CameraAspect.SIXTEEN_NINE,
            timerSeconds = 5,
            zoomRatio = 2.4f
        )

        assertEquals(state, serializeRoundTrip(state))
    }

    @Test
    fun `returning from editor preserves the current watermark for gallery import`() {
        assertEquals(
            true,
            shouldPreserveCameraWatermark(
                currentPage = AppPage.EDITOR,
                destination = AppPage.CAMERA,
                hasLayers = true
            )
        )
        assertEquals(
            false,
            shouldPreserveCameraWatermark(
                currentPage = AppPage.EDITOR,
                destination = AppPage.CAMERA,
                hasLayers = false
            )
        )
    }

    private fun serializeRoundTrip(value: Any): Any {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(value) }
            output.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
    }
}
