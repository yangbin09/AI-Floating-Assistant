package com.aisoul.assistant

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aisoul.assistant.components.AutomationSettingsScreen
import com.aisoul.assistant.components.ChatPanel
import com.aisoul.assistant.components.ChatWorkbenchScreen
import com.aisoul.assistant.components.SoulMatchScreen
import com.aisoul.assistant.model.SoulUser
import com.aisoul.assistant.viewmodel.AutomationSettingsViewModel
import com.aisoul.assistant.viewmodel.ChatViewModel
import com.aisoul.assistant.viewmodel.ChatWorkbenchViewModel
import com.aisoul.assistant.viewmodel.MainViewModel
import com.aisoul.assistant.viewmodel.SoulMatchViewModel

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
    mainViewModel: MainViewModel,
    onStartFloatingService: () -> Unit = {},
    onStopFloatingService: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSoulMatchScreen by remember { mutableStateOf(false) }

    val chatWorkbenchViewModel: ChatWorkbenchViewModel = viewModel()
    val soulMatchViewModel: SoulMatchViewModel = viewModel()

    // Show SoulMatchScreen when requested
    if (showSoulMatchScreen) {
        SoulMatchScreen(
            viewModel = soulMatchViewModel,
            onNavigateToChatWorkbench = { user ->
                chatWorkbenchViewModel.setCurrentUser(user)
                selectedTabIndex = 3
                showSoulMatchScreen = false
            },
            onNavigateBack = { showSoulMatchScreen = false }
        )
        return
    }

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
                    viewModel = mainViewModel,
                    onStartService = onStartFloatingService,
                    onStopService = onStopFloatingService,
                    onNavigateToSoulMatch = { showSoulMatchScreen = true }
                )
                1 -> AiTabContent(
                    onNavigateToSoulMatch = { showSoulMatchScreen = true }
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
    viewModel: MainViewModel,
    onStartService: () -> Unit = {},
    onStopService: () -> Unit = {},
    onNavigateToSoulMatch: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                    TextButton(onClick = { onStopService() }) {
                        Text("停止")
                    }
                } else {
                    Button(onClick = { onStartService() }) {
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
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                    Toast.makeText(context, "请在列表中找到 Soul 无障碍服务并开启", Toast.LENGTH_LONG).show()
                }) {
                    Text("授权")
                }
            }
        }

        // Soul匹配助手入口
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigateToSoulMatch()
                        Toast.makeText(context, "正在打开 Soul 匹配助手...", Toast.LENGTH_SHORT).show()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Soul 匹配助手",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "AI 智能匹配推荐，生成个性化开场白",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
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
    onNavigateToSoulMatch: () -> Unit
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
            0 -> SoulMatchTab(
                onNavigateToSoulMatch = onNavigateToSoulMatch
            )
            1 -> AutomationSettingsContent()
        }
    }
}

@Composable
private fun SoulMatchTab(
    onNavigateToSoulMatch: () -> Unit
) {
    val context = LocalContext.current

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
            onClick = {
                Toast.makeText(context, "正在打开 Soul 匹配助手...", Toast.LENGTH_SHORT).show()
                onNavigateToSoulMatch()
            }
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
    val context = LocalContext.current
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
                    .clickable {
                        Toast.makeText(context, "查看与 ${record.name} 的匹配详情", Toast.LENGTH_SHORT).show()
                        onNavigateToWorkbench(SoulUser(id = record.name, name = record.name, avatarUrl = null))
                    }
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
                            .clickable {
                                Toast.makeText(context, "查看 ${record.name} 的资料", Toast.LENGTH_SHORT).show()
                            },
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
    val context = LocalContext.current
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
                    .clickable {
                        Toast.makeText(context, "打开与 ${chat.name} 的聊天", Toast.LENGTH_SHORT).show()
                    }
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
                            .clickable {
                                Toast.makeText(context, "查看 ${chat.name} 的资料", Toast.LENGTH_SHORT).show()
                            },
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
