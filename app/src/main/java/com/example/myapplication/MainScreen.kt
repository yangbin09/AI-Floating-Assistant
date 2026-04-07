package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.components.AutomationSettingsScreen
import com.example.myapplication.components.ChatPanel
import com.example.myapplication.components.ChatWorkbenchScreen
import com.example.myapplication.model.SoulUser
import com.example.myapplication.viewmodel.AutomationSettingsViewModel
import com.example.myapplication.viewmodel.ChatViewModel
import com.example.myapplication.viewmodel.ChatWorkbenchViewModel
import com.example.myapplication.viewmodel.MainViewModel

data class TabItem(
    val label: String,
    val icon: ImageVector
)

val tabItems = listOf(
    TabItem("悬浮球", Icons.Default.Circle),
    TabItem("AI", Icons.Default.Psychology),
    TabItem("记录", Icons.Default.History),
    TabItem("工作台", Icons.Default.WorkOutline)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val chatViewModel: ChatViewModel = viewModel()
    val chatWorkbenchViewModel: ChatWorkbenchViewModel = viewModel()
    val automationSettingsViewModel: AutomationSettingsViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabItems[selectedTabIndex].label) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTabIndex) {
                0 -> FloatingBallSettingsTab(
                    viewModel = mainViewModel
                )
                1 -> AiTabContent(
                    onNavigateToWorkbench = { user ->
                        chatWorkbenchViewModel.setCurrentUser(user)
                        selectedTabIndex = 3
                    }
                )
                2 -> RecordsTabContent(
                    onNavigateToWorkbench = { user ->
                        chatWorkbenchViewModel.setCurrentUser(user)
                        selectedTabIndex = 3
                    }
                )
                3 -> ChatWorkbenchScreen(
                    viewModel = chatWorkbenchViewModel,
                    onNavigateBack = { selectedTabIndex = 0 },
                    onSendMessage = { }
                )
            }
        }
    }
}

@Composable
private fun FloatingBallSettingsTab(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 服务状态卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isFloatingServiceRunning)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (uiState.isFloatingServiceRunning)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "悬浮球服务",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (uiState.isFloatingServiceRunning) "运行中" else "未启动",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (uiState.isFloatingServiceRunning) {
                    TextButton(onClick = { }) {
                        Text("停止")
                    }
                } else {
                    Button(onClick = { }) {
                        Text("启动")
                    }
                }
            }
        }

        // 通知监听状态
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = if (uiState.serviceStatus.isNotificationListenerEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "通知监听权限",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (uiState.serviceStatus.isNotificationListenerEnabled)
                            "已授权"
                        else
                            "未授权",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 无障碍服务状态
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "无障碍服务 (Soul)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "用于自动读取 Soul 界面",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Button(onClick = { }) {
                    Text("授权")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "提示：首次使用需要在系统设置中开启悬浮球和通知监听权限",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun AiTabContent(
    onNavigateToWorkbench: (SoulUser) -> Unit
) {
    // AI Tab - Soul 自动化功能
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Soul 匹配") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("自动化设置") }
            )
        }

        when (selectedSubTab) {
            0 -> SoulMatchTab(onNavigateToWorkbench = onNavigateToWorkbench)
            1 -> AutomationSettingsContent()
        }
    }
}

@Composable
private fun SoulMatchTab(
    onNavigateToWorkbench: (SoulUser) -> Unit
) {
    // TODO: 调用 Soul 无障碍服务扫描
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Soul 智能匹配",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI 自动扫描匹配列表\n分析用户好聊程度\n生成个性化开场白",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* 启动扫描 */ }
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始扫描")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "请确保 Soul 应用已开启并处于匹配列表页面",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutomationSettingsContent() {
    val automationSettingsViewModel: AutomationSettingsViewModel = viewModel()
    val uiState by automationSettingsViewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 全局开关
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI 自动化引擎",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (uiState.settings.isEnabled) "运行中" else "已停止",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = uiState.settings.isEnabled,
                        onCheckedChange = { automationSettingsViewModel.updateEnabled(it) }
                    )
                }
            }
        }

        // 功能开关
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "功能开关",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自动回复建议")
                        Switch(
                            checked = uiState.settings.autoReplyEnabled,
                            onCheckedChange = { automationSettingsViewModel.updateAutoReply(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自动发送（需确认）")
                        Switch(
                            checked = uiState.settings.autoSendEnabled,
                            onCheckedChange = { automationSettingsViewModel.updateAutoSend(it) }
                        )
                    }
                }
            }
        }

        // 发送频率
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "发送频率",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "每小时最多 ${uiState.settings.maxMessagesPerHour} 条",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.settings.maxMessagesPerHour.toFloat(),
                        onValueChange = {
                            automationSettingsViewModel.updateMaxMessagesPerHour(it.toInt())
                        },
                        valueRange = 1f..30f
                    )
                }
            }
        }

        // 人设选择
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "聊天人设",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availablePersonas.forEach { persona ->
                            FilterChip(
                                selected = persona.id == uiState.settings.selectedPersonaId,
                                onClick = {
                                    automationSettingsViewModel.updatePersona(persona.id)
                                },
                                label = { Text("${persona.icon} ${persona.name}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsTabContent(
    onNavigateToWorkbench: (SoulUser) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("匹配记录") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("聊天记录") }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("心动值") }
            )
        }

        when (selectedSubTab) {
            0 -> MatchRecordsTab(onNavigateToWorkbench = onNavigateToWorkbench)
            1 -> ChatRecordsTab()
            2 -> HeartRecordsTab()
        }
    }
}

@Composable
private fun MatchRecordsTab(
    onNavigateToWorkbench: (SoulUser) -> Unit
) {
    // 模拟匹配记录数据
    val mockRecords = remember {
        listOf(
            MatchRecord("小美", "22♀", "音乐、旅行", 85, "2小时前"),
            MatchRecord("小林", "24♂", "电影、游戏", 72, "昨天"),
            MatchRecord("小红", "23♀", "美食、摄影", 90, "3天前"),
            MatchRecord("小张", "25♂", "运动、读书", 65, "上周")
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(mockRecords) { record ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = record.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = record.ageGender,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = record.tags,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${record.matchScore}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = record.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRecordsTab() {
    val mockChats = remember {
        listOf(
            ChatRecord("小美", "最后一条消息...", "刚刚"),
            ChatRecord("小林", "好的没问题", "10分钟前"),
            ChatRecord("小红", "明天见～", "昨天")
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mockChats) { chat ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💬", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = chat.lastMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = chat.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun HeartRecordsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "心动值统计",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "今日收到: 12 次心动",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "本周收到: 48 次心动",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "本月收到: 156 次心动",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "累计匹配: 328 人",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

data class MatchRecord(
    val name: String,
    val ageGender: String,
    val tags: String,
    val matchScore: Int,
    val time: String
)

data class ChatRecord(
    val name: String,
    val lastMessage: String,
    val time: String
)
