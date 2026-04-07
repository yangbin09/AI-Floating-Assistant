package com.aisoul.assistant.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aisoul.assistant.model.ChatMessage
import com.aisoul.assistant.viewmodel.MainViewModel
import com.aisoul.assistant.viewmodel.ServiceStatus

@Composable
fun DashboardContent(
    viewModel: MainViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onNavigateToSoulMatch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Cards Section
        item {
            Text(
                text = "状态监控",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            StatusCardsRow(serviceStatus = uiState.serviceStatus)
        }

        // Quick Actions Section
        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "快速操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            QuickActionsRow(
                isServiceRunning = uiState.isFloatingServiceRunning,
                isTestingAi = uiState.isTestingAi,
                onStartService = onStartService,
                onStopService = onStopService,
                onTestAi = { viewModel.testAiConnection() }
            )
        }

        // AI Configuration Section
        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            ExpandableSection(
                title = "🤖 AI 助手配置",
                isExpanded = uiState.isAiExpanded,
                onToggle = { viewModel.toggleAiExpanded() }
            ) {
                AiConfigContent(
                    apiKeyMasked = uiState.apiKeyMasked,
                    selectedModel = uiState.selectedModel,
                    onApiKeySave = { viewModel.updateApiKey(it) },
                    onModelChange = { viewModel.updateModel(it) }
                )
            }
        }

        // Auto Reply Section
        item {
            ExpandableSection(
                title = "⚡ 自动回复配置",
                isExpanded = uiState.isAutoReplyExpanded,
                onToggle = { viewModel.toggleAutoReplyExpanded() }
            ) {
                AutoReplyConfigContent(
                    isEnabled = uiState.serviceStatus.isAutoReplyEnabled,
                    monitoredApps = uiState.serviceStatus.monitoredApps,
                    onToggle = { viewModel.toggleAutoReply() }
                )
            }
        }

        // Soul Automation Section
        item {
            SoulAutomationSection(onNavigateToSoulMatch = onNavigateToSoulMatch)
        }

        // Recent Logs Section
        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "📋 最近活动",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.recentMessages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "暂无活动记录",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.recentMessages.take(5)) { message ->
                MessageLogItem(message)
            }
        }

        // Test Result
        uiState.testResult?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("✅"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StatusCardsRow(serviceStatus: ServiceStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(
            title = "悬浮球",
            isOk = serviceStatus.isFloatingBallEnabled,
            modifier = Modifier.weight(1f)
        )
        StatusCard(
            title = "通知监听",
            isOk = serviceStatus.isNotificationListenerEnabled,
            modifier = Modifier.weight(1f)
        )
        StatusCard(
            title = "AI 配置",
            isOk = serviceStatus.isAiConfigured,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusCard(
    title: String,
    isOk: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isOk)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOk) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isOk) "正常" else "异常",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionsRow(
    isServiceRunning: Boolean,
    isTestingAi: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestAi: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = if (isServiceRunning) onStopService else onStartService,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isServiceRunning) Icons.Default.Settings else Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isServiceRunning) "停止服务" else "开启悬浮球")
        }

        Button(
            onClick = onTestAi,
            enabled = !isTestingAi,
            modifier = Modifier.weight(1f)
        ) {
            if (isTestingAi) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "测试 AI")
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AiConfigContent(
    apiKeyMasked: String,
    selectedModel: String,
    onApiKeySave: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKeyInput by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // API Key
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "API Key: ",
                style = MaterialTheme.typography.bodyMedium
            )
            if (apiKeyMasked.isNotEmpty() && !showApiKeyInput) {
                Text(
                    text = apiKeyMasked,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showApiKeyInput = true }) {
                    Text("修改")
                }
            } else {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入 DeepSeek API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                if (apiKeyInput.isNotEmpty()) {
                    TextButton(onClick = {
                        onApiKeySave(apiKeyInput)
                        showApiKeyInput = false
                        apiKeyInput = ""
                    }) {
                        Text("保存")
                    }
                }
            }
        }

        // Model Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { modelDropdownExpanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "模型: ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (selectedModel == "deepseek-chat") "DeepSeek Chat" else "DeepSeek Coder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            DropdownMenu(
                expanded = modelDropdownExpanded,
                onDismissRequest = { modelDropdownExpanded = false }
            ) {
                MainViewModel.SUPPORTED_MODELS.forEach { (modelId, modelName) ->
                    DropdownMenuItem(
                        text = { Text(modelName) },
                        onClick = {
                            onModelChange(modelId)
                            modelDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AutoReplyConfigContent(
    isEnabled: Boolean,
    monitoredApps: List<String>,
    onToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "自动回复总开关",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }

        Text(
            text = "已监听应用: ${monitoredApps.joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "提示: 首次使用需要在系统设置中开启通知监听权限",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun MessageLogItem(message: ChatMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isUser)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (message.isUser) "📩" else "🤖",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.senderName ?: (if (message.isUser) "用户" else "AI"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun SoulAutomationSection(
    onNavigateToSoulMatch: () -> Unit = {}
) {
    var isSoulExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSoulExpanded = !isSoulExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔮 Soul 自动化", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Beta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (isSoulExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = isSoulExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "全自动社交助手 - AI 自动帮你找人、聊天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 功能列表
                    SoulFeatureItem(
                        icon = "👤",
                        title = "自动找人",
                        description = "自动扫描匹配列表，分析用户好聊程度"
                    )
                    SoulFeatureItem(
                        icon = "💬",
                        title = "智能开场",
                        description = "基于对方兴趣生成个性化开场白"
                    )
                    SoulFeatureItem(
                        icon = "🤖",
                        title = "自动续聊",
                        description = "AI 分析上下文，生成多风格回复建议"
                    )
                    SoulFeatureItem(
                        icon = "🎭",
                        title = "人设切换",
                        description = "真诚温柔/幽默松弛/高情商等多种风格"
                    )

                    HorizontalDivider()

                    Text(
                        text = "⚠️ 使用提示",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "• 请确保 Soul 应用已开启\n• 首次使用需授权无障碍权限\n• 建议合理设置发送频率避免封号",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onNavigateToSoulMatch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开 Soul 匹配助手")
                    }
                }
            }
        }
    }
}

@Composable
private fun SoulFeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
