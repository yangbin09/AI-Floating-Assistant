package com.example.myapplication

import com.example.myapplication.model.ChatMessage
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ChatMessageTest {

    @Test
    fun `ChatMessage creation with default values`() {
        val message = ChatMessage(content = "Hello", isUser = true)

        assertEquals("Hello", message.content)
        assertTrue(message.isUser)
        assertFalse(message.isUser.not())
        assertTrue(message.id.isNotEmpty())
        assertTrue(message.timestamp > 0)
    }

    @Test
    fun `ChatMessage with custom id and timestamp`() {
        val customId = "test-id-123"
        val customTimestamp = 1234567890L

        val message = ChatMessage(
            id = customId,
            content = "Test message",
            isUser = false,
            timestamp = customTimestamp
        )

        assertEquals(customId, message.id)
        assertEquals("Test message", message.content)
        assertFalse(message.isUser)
        assertEquals(customTimestamp, message.timestamp)
    }

    @Test
    fun `ChatMessage copy creates new instance with updated content`() {
        val original = ChatMessage(
            content = "Original",
            isUser = true
        )

        val copied = original.copy(content = "Updated")

        assertEquals("Original", original.content)
        assertEquals("Updated", copied.content)
        assertEquals(original.id, copied.id)
        assertEquals(original.isUser, copied.isUser)
    }

    @Test
    fun `ChatMessage id uniqueness`() {
        val messages = (1..100).map {
            ChatMessage(content = "Message $it", isUser = it % 2 == 0)
        }

        val ids = messages.map { it.id }
        assertEquals(100, ids.toSet().size)
    }

    @Test
    fun `ChatMessage timestamp is current time`() {
        val beforeCreation = System.currentTimeMillis()
        val message = ChatMessage(content = "Test", isUser = false)
        val afterCreation = System.currentTimeMillis()

        assertTrue(message.timestamp in beforeCreation..afterCreation)
    }
}
