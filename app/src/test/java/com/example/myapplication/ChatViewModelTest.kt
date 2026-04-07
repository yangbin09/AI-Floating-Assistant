package com.example.myapplication

import com.example.myapplication.model.ChatMessage
import com.example.myapplication.viewmodel.ChatViewModel
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

    private lateinit var viewModel: ChatViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has welcome message`() {
        val state = viewModel.uiState.value

        assertTrue("Should have at least one message", state.messages.isNotEmpty())
        assertFalse("Welcome message should be from AI", state.messages.first().isUser)
        assertTrue("Welcome message should mention AI assistant",
            state.messages.first().content.contains("AI 助手"))
    }

    @Test
    fun `updateInputText updates the input field`() {
        viewModel.updateInputText("Hello World")

        assertEquals("Hello World", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendMessage with blank input does nothing`() {
        viewModel.updateInputText("   ")
        viewModel.sendMessage()

        // Should still only have welcome message
        assertEquals("Should only have welcome message", 1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sendMessage with valid input adds user message`() {
        viewModel.updateInputText("Test message")
        viewModel.sendMessage()

        val messages = viewModel.uiState.value.messages
        val userMessage = messages.find { it.isUser }

        assertNotNull("Should have a user message", userMessage)
        assertEquals("Test message", userMessage?.content)
    }

    @Test
    fun `sendMessage clears input field`() {
        viewModel.updateInputText("Test message")
        viewModel.sendMessage()

        assertEquals("Input should be cleared", "", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendMessage while AI is typing does nothing`() = runTest {
        viewModel.updateInputText("First message")
        viewModel.sendMessage()

        // Advance dispatcher just enough to let the coroutine start and set isAiTyping = true
        // But not enough to complete the coroutine (which takes 1000ms delay + streaming delays)
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        // Verify AI is now typing
        assertTrue("AI should be typing", viewModel.uiState.value.isAiTyping)

        // Now try to send another while AI is typing
        viewModel.updateInputText("Second message")
        viewModel.sendMessage()

        // Should still have only 1 user message since AI is still typing
        val userMessages = viewModel.uiState.value.messages.filter { it.isUser }
        assertEquals("Should only have one user message", 1, userMessages.size)
    }

    @Test
    fun `messages have unique ids`() = runTest {
        viewModel.updateInputText("Message 1")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateInputText("Message 2")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        val ids = messages.map { it.id }
        assertEquals("All message IDs should be unique",
            ids.size, ids.toSet().size)
    }

    @Test
    fun `welcome message content is correct`() {
        val welcomeMessage = viewModel.uiState.value.messages.first()

        assertTrue(welcomeMessage.content.contains("你好"))
        assertTrue(welcomeMessage.content.contains("AI 助手"))
        assertFalse(welcomeMessage.isUser)
    }

    @Test
    fun `inputText state persists after multiple updates`() {
        viewModel.updateInputText("First")
        viewModel.updateInputText("Second")
        viewModel.updateInputText("Third")

        assertEquals("Third", viewModel.uiState.value.inputText)
    }
}
