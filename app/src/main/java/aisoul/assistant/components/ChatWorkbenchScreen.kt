package com.aisoul.assistant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisoul.assistant.model.ChatMessage
import com.aisoul.assistant.model.ChatStage
import com.aisoul.assistant.model.Persona
import com.aisoul.assistant.model.SoulUser
import com.aisoul.assistant.viewmodel.ChatWorkbenchViewModel

/**
 * 聊天工作台页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWorkbenchScreen(
    viewModel: ChatWorkbenchViewModel,
    onNavigateBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val suggestedReplies by viewModel.suggestedReplies.collectAsState()

    var showPersonaSelector by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentUser) {
        // 当用户变化时，加载聊天历史
        uiState.currentUser?.let { user ->
            viewModel.loadChatHistory(user.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.currentUser?.name ?: "聊天工作台",
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.currentUser?.let { user ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "推荐指数: ${user.matchScore}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (uiState.chatStage != ChatStage.UNKNOWN) {
                                    Text(
                                        text = " · ${uiState.chatStage.displayName()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showPersonaSelector = true }) {
                        Icon(Icons.Default.Person, contentDescription = "切换人设")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 聊天消息区域
            ChatMessageList(
                messages = uiState.chatHistory,
                modifier = Modifier.weight(1f)
            )

            // 建议回复区域
            if (suggestedReplies.isNotEmpty() || uiState.isGenerating) {
                SuggestedRepliesSection(
                    replies = suggestedReplies,
                    selectedReply = uiState.selectedReply,
                    isGenerating = uiState.isGenerating,
                    isRewriting = uiState.isRewriting,
                    onReplyClick = { viewModel.selectReply(it) },
                    onSendClick = {
                        viewModel.sendSelectedReply()
                        uiState.selectedReply?.let { onSendMessage(it) }
                    },
                    onRewriteClick = { reply, style ->
                        viewModel.rewriteReply(reply, style)
                    }
                )
            }

            // 人设显示
            PersonaBar(
                persona = uiState.selectedPersona,
                onSwitchClick = { showPersonaSelector = true }
            )
        }

        // 人设选择弹窗
        if (showPersonaSelector) {
            PersonaSelectorDialog(
                personas = uiState.availablePersonas,
                selectedPersona = uiState.selectedPersona,
                onPersonaSelect = {
                    viewModel.switchPersona(it)
                    showPersonaSelector = false
                },
                onDismiss = { showPersonaSelector = false }
            )
        }
    }
}

@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (messages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无聊天记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "发送消息后 AI 会生成回复建议",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        items(messages, key = { it.id }) { message ->
            ChatBubbleItem(message = message)
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("我", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SuggestedRepliesSection(
    replies: List<String>,
    selectedReply: String?,
    isGenerating: Boolean,
    isRewriting: Boolean,
    onReplyClick: (String) -> Unit,
    onSendClick: (String) -> Unit,
    onRewriteClick: (String, com.aisoul.assistant.domain.strategy.RewriteStyle) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI 建议回复",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isGenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在生成回复...")
                }
            } else {
                replies.forEach { reply ->
                    SuggestedReplyItem(
                        reply = reply,
                        isSelected = reply == selectedReply,
                        isRewriting = isRewriting,
                        onClick = { onReplyClick(reply) },
                        onSend = { onSendClick(reply) },
                        onRewrite = { style -> onRewriteClick(reply, style) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SuggestedReplyItem(
    reply: String,
    isSelected: Boolean,
    isRewriting: Boolean,
    onClick: () -> Unit,
    onSend: () -> Unit,
    onRewrite: (com.aisoul.assistant.domain.strategy.RewriteStyle) -> Unit
) {
    var showRewriteMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reply,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // 改写按钮
            Box {
                IconButton(
                    onClick = { showRewriteMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isRewriting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "改写",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                DropdownMenu(
                    expanded = showRewriteMenu,
                    onDismissRequest = { showRewriteMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("更幽默") },
                        onClick = {
                            onRewrite(com.aisoul.assistant.domain.strategy.RewriteStyle.MORE_HUMOROUS)
                            showRewriteMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("更温柔") },
                        onClick = {
                            onRewrite(com.aisoul.assistant.domain.strategy.RewriteStyle.MORE_GENTLE)
                            showRewriteMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("更推进") },
                        onClick = {
                            onRewrite(com.aisoul.assistant.domain.strategy.RewriteStyle.MORE_AGGRESSIVE)
                            showRewriteMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("更真诚") },
                        onClick = {
                            onRewrite(com.aisoul.assistant.domain.strategy.RewriteStyle.MORE_SINCERE)
                            showRewriteMenu = false
                        }
                    )
                }
            }

            // 发送按钮
            FilledTonalButton(
                onClick = onSend,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("发送", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PersonaBar(
    persona: Persona,
    onSwitchClick: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .clickable { onSwitchClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = persona.icon,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前人设",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = persona.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "切换",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PersonaSelectorDialog(
    personas: List<Persona>,
    selectedPersona: Persona,
    onPersonaSelect: (Persona) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择聊天人设") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(personas) { persona ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (persona.id == selectedPersona.id)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPersonaSelect(persona) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = persona.icon,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = persona.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = persona.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
