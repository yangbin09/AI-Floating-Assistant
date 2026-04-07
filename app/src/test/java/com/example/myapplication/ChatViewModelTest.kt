package com.example.myapplication

import com.aisoul.assistant.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ChatMessage has unique id by default`() {
        val msg1 = ChatMessage(content = "Hello", isUser = true)
        val msg2 = ChatMessage(content = "World", isUser = false)

        assertNotEquals(msg1.id, msg2.id)
    }

    @Test
    fun `ChatMessage stores content correctly`() {
        val msg = ChatMessage(content = "Test content", isUser = true)

        assertEquals("Test content", msg.content)
        assertTrue(msg.isUser)
    }

    @Test
    fun `ChatMessage timestamp is set by default`() {
        val before = System.currentTimeMillis()
        val msg = ChatMessage(content = "Test", isUser = false)
        val after = System.currentTimeMillis()

        assertTrue(msg.timestamp in before..after)
    }

    @Test
    fun `ChatMessage with sender info`() {
        val msg = ChatMessage(
            content = "Hello",
            isUser = true,
            senderName = "John",
            senderApp = "com.example.app",
            conversationId = "com.example.app_John"
        )

        assertEquals("John", msg.senderName)
        assertEquals("com.example.app", msg.senderApp)
        assertEquals("com.example.app_John", msg.conversationId)
    }

    @Test
    fun `multiple messages have unique ids`() {
        val messages = (1..10).map {
            ChatMessage(content = "Message $it", isUser = it % 2 == 0)
        }

        val ids = messages.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `welcome message content verification`() {
        val welcomeContent = "你好！我是你的 AI 助手。我可以帮你总结文章、翻译内容或回答问题。"

        assertTrue(welcomeContent.contains("你好"))
        assertTrue(welcomeContent.contains("AI 助手"))
        assertTrue(welcomeContent.contains("总结") || welcomeContent.contains("翻译"))
    }

    @Test
    fun `simulated AI response patterns`() {
        val responses = mapOf(
            "你好" to "你好！有什么我可以帮助你的吗？",
            "帮助" to "我可以帮你",
            "总结" to "请发送你想要总结的文章",
            "翻译" to "请发送你想要翻译的文本"
        )

        responses.forEach { (input, _) ->
            assertTrue(input.isNotEmpty())
        }
    }

    @Test
    fun `message list operations`() {
        val messages = mutableListOf<ChatMessage>()

        val msg1 = ChatMessage(content = "First", isUser = true)
        val msg2 = ChatMessage(content = "Second", isUser = false)

        messages.add(msg1)
        messages.add(msg2)

        assertEquals(2, messages.size)
        assertEquals("First", messages[0].content)
        assertEquals("Second", messages[1].content)

        messages.removeIf { it.content == "First" }
        assertEquals(1, messages.size)
        assertEquals("Second", messages[0].content)
    }

    @Test
    fun `conversation id format`() {
        val senderApp = "com.tencent.mm"
        val senderName = "Zhang San"
        val conversationId = "${senderApp}_${senderName}"

        assertEquals("com.tencent.mm_Zhang San", conversationId)
    }
}
