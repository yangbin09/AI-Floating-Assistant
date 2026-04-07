package com.aisoul.assistant.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisoul.assistant.model.AutomationScenario
import com.aisoul.assistant.model.Persona
import com.aisoul.assistant.viewmodel.AutomationSettingsViewModel

/**
 * 自动化设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationSettingsScreen(
    viewModel: AutomationSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddBlacklistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自动化设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 全局开关
            item {
                GlobalSwitchCard(
                    isEnabled = uiState.settings.isEnabled,
                    onToggle = { viewModel.updateEnabled(it) }
                )
            }

            // 功能开关
            item {
                FeaturesCard(
                    settings = uiState.settings,
                    onAutoFindChange = { viewModel.updateAutoFind(it) },
                    onAutoOpenerChange = { viewModel.updateAutoOpener(it) },
                    onAutoReplyChange = { viewModel.updateAutoReply(it) },
                    onAutoSendChange = { viewModel.updateAutoSend(it) }
                )
            }

            // 场景预设
            item {
                ScenarioCard(
                    availableScenarios = uiState.availableScenarios,
                    currentSettings = uiState.settings,
                    onApplyScenario = { viewModel.applyScenario(it) }
                )
            }

            // 频率控制
            item {
                FrequencyCard(
                    maxMessagesPerHour = uiState.settings.maxMessagesPerHour,
                    minInterval = uiState.settings.minIntervalSeconds,
                    maxInterval = uiState.settings.maxIntervalSeconds,
                    onUpdateMaxPerHour = { viewModel.updateMaxMessagesPerHour(it) },
                    onUpdateInterval = { min, max -> viewModel.updateInterval(min, max) }
                )
            }

            // 人设选择
            item {
                PersonaCard(
                    availablePersonas = uiState.availablePersonas,
                    selectedPersonaId = uiState.settings.selectedPersonaId,
                    onPersonaSelect = { viewModel.updatePersona(it) }
                )
            }

            // 黑名单关键词
            item {
                BlacklistCard(
                    keywords = uiState.settings.blacklistedKeywords,
                    onAddKeyword = { viewModel.addBlacklistKeyword(it) },
                    onRemoveKeyword = { viewModel.removeBlacklistKeyword(it) },
                    onShowAddDialog = { showAddBlacklistDialog = true }
                )
            }

            // 说明
            item {
                InfoCard()
            }
        }
    }

    // 添加关键词弹窗
    if (showAddBlacklistDialog) {
        AddBlacklistDialog(
            onAdd = { keyword ->
                viewModel.addBlacklistKeyword(keyword)
                showAddBlacklistDialog = false
            },
            onDismiss = { showAddBlacklistDialog = false }
        )
    }
}

@Composable
private fun GlobalSwitchCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isEnabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = null,
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "自动化引擎",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEnabled) "运行中" else "已停止",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun FeaturesCard(
    settings: com.aisoul.assistant.model.AutomationSettings,
    onAutoFindChange: (Boolean) -> Unit,
    onAutoOpenerChange: (Boolean) -> Unit,
    onAutoReplyChange: (Boolean) -> Unit,
    onAutoSendChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "功能开关",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            FeatureSwitch(
                title = "自动找人",
                description = "自动扫描并进入匹配列表",
                isEnabled = settings.autoFindEnabled,
                onToggle = onAutoFindChange
            )

            FeatureSwitch(
                title = "自动开场",
                description = "检测到用户后自动生成开场白",
                isEnabled = settings.autoOpenerEnabled,
                onToggle = onAutoOpenerChange
            )

            FeatureSwitch(
                title = "回复建议",
                description = "收到消息后生成 AI 回复建议",
                isEnabled = settings.autoReplyEnabled,
                onToggle = onAutoReplyChange
            )

            FeatureSwitch(
                title = "自动发送",
                description = "自动发送最佳回复（需谨慎）",
                isEnabled = settings.autoSendEnabled,
                onToggle = onAutoSendChange
            )
        }
    }
}

@Composable
private fun FeatureSwitch(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun ScenarioCard(
    availableScenarios: List<AutomationScenario>,
    currentSettings: com.aisoul.assistant.model.AutomationSettings,
    onApplyScenario: (AutomationScenario) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "场景预设",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableScenarios) { scenario ->
                    val isSelected = currentSettings.maxMessagesPerHour == scenario.settings.maxMessagesPerHour

                    FilterChip(
                        selected = isSelected,
                        onClick = { onApplyScenario(scenario) },
                        label = { Text(scenario.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val currentScenario = availableScenarios.find {
                it.settings.maxMessagesPerHour == currentSettings.maxMessagesPerHour
            }

            Text(
                text = currentScenario?.description ?: "自定义设置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun FrequencyCard(
    maxMessagesPerHour: Int,
    minInterval: Int,
    maxInterval: Int,
    onUpdateMaxPerHour: (Int) -> Unit,
    onUpdateInterval: (Int, Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "频率控制",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 每小时最大消息数
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每小时最多发送",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$maxMessagesPerHour 条",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = maxMessagesPerHour.toFloat(),
                onValueChange = { onUpdateMaxPerHour(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 发送间隔
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "发送间隔",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${minInterval}-${maxInterval} 秒",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            RangeSlider(
                value = minInterval.toFloat()..maxInterval.toFloat(),
                onValueChange = { range ->
                    onUpdateInterval(range.start.toInt(), range.endInclusive.toInt())
                },
                valueRange = 1f..120f
            )
        }
    }
}

@Composable
private fun PersonaCard(
    availablePersonas: List<Persona>,
    selectedPersonaId: String,
    onPersonaSelect: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "聊天人设",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availablePersonas) { persona ->
                    val isSelected = persona.id == selectedPersonaId

                    FilterChip(
                        selected = isSelected,
                        onClick = { onPersonaSelect(persona.id) },
                        label = { Text("${persona.icon} ${persona.name}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val selectedPersona = availablePersonas.find { it.id == selectedPersonaId }
            Text(
                text = selectedPersona?.description ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun BlacklistCard(
    keywords: List<String>,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onShowAddDialog: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "黑名单关键词",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShowAddDialog) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }

            Text(
                text = "包含这些关键词时跳过自动回复",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (keywords.isEmpty()) {
                Text(
                    text = "暂无黑名单关键词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(keywords) { keyword ->
                        InputChip(
                            selected = false,
                            onClick = { },
                            label = { Text(keyword) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onRemoveKeyword(keyword) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "使用提示",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = """
                        1. 请确保 Soul 应用已开启并登录
                        2. 建议从"保守模式"开始测试
                        3. 自动发送功能可能违反平台规则，使用需谨慎
                        4. 合理设置发送频率，避免账号风险
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun AddBlacklistDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加黑名单关键词") },
        text = {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("关键词") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(keyword) },
                enabled = keyword.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
