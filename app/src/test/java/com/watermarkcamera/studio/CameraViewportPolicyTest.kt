package com.watermarkcamera.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraViewportPolicyTest {
    @Test
    fun `portrait preview dimensions remain portrait for camera crop`() {
        assertEquals(CameraViewportAspect(3, 4), cameraViewportAspect(1200, 1600))
        assertEquals(CameraViewportAspect(9, 16), cameraViewportAspect(1080, 1920))
    }

    @Test
    fun `camera crop ratio is reduced without changing orientation`() {
        assertEquals(CameraViewportAspect(3, 4), cameraViewportAspect(1080, 1440))
        assertEquals(CameraViewportAspect(300, 559), cameraViewportAspect(1200, 2236))
        assertEquals(CameraViewportAspect(1, 1), cameraViewportAspect(1200, 1200))
    }

    @Test
    fun `stale camera binding cannot replace the active binding`() {
        val guard = CameraBindingGuard()
        val oldBinding = Any()
        val newBinding = Any()

        guard.activate(oldBinding)
        guard.activate(newBinding)

        assertEquals(false, guard.isActive(oldBinding))
        assertEquals(true, guard.isActive(newBinding))

        guard.invalidate(oldBinding)
        assertEquals(true, guard.isActive(newBinding))

        guard.invalidate(newBinding)
        assertEquals(false, guard.isActive(newBinding))
    }
}
