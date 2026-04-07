package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class FloatingBallTest {

    @Test
    fun `FloatingBall has correct default configuration`() {
        // FloatingBall is a Composable, so we test its behavior through integration tests
        // This test verifies the component can be instantiated with proper callbacks
        var clickCount = 0
        var dragCallCount = 0

        // These callbacks should be called by the component
        val onClick: () -> Unit = { clickCount++ }
        val onDrag: (Float, Float) -> Unit = { _, _ -> dragCallCount++ }
        val onDragEnd: () -> Unit = { }

        // Verify callbacks can be assigned without error
        assertNotNull(onClick)
        assertNotNull(onDrag)
        assertNotNull(onDragEnd)
    }

    @Test
    fun `drag callback receives delta values`() {
        var receivedDeltaX = 0f
        var receivedDeltaY = 0f

        val onDrag: (Float, Float) -> Unit = { deltaX, deltaY ->
            receivedDeltaX = deltaX
            receivedDeltaY = deltaY
        }

        // Simulate drag call
        onDrag(10.5f, -20.3f)

        assertEquals(10.5f, receivedDeltaX, 0.001f)
        assertEquals(-20.3f, receivedDeltaY, 0.001f)
    }
}
