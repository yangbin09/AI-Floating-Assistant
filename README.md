# AI Floating Assistant - AI 悬浮助手

<div align="center">

![Android](https://img.shields.io/badge/Android-36-green?style=flat-square&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat-square&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.00-blue?style=flat-square&logo=jetpackcompose)
![License](https://img.shields.io/badge/License-Apache%202.0-green?style=flat-square)

**一个运行在 Android 系统上的 AI 悬浮球助手应用**

[English](./README.md) | 简体中文

</div>

---

## 目录

- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [核心模块](#核心模块)
- [测试说明](#测试说明)
- [开发指南](#开发指南)
- [版本历史](#版本历史)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 功能特性

### 核心功能

| 功能 | 描述 |
|------|------|
| 悬浮球 | 可拖动的悬浮球，松开自动贴边 |
| 聊天面板 | 点击展开/收起聊天界面 |
| AI 回复 | 模拟 AI 回复，支持打字机效果 |
| 后台运行 | 前台服务支持，进程保活 |
| 权限管理 | Overlay 悬浮窗权限管理 |

### 悬浮球交互

```
┌─────────────────────────────────────────┐
│                                         │
│   ┌───┐                                │
│   │AI │  ← 拖动悬浮球                   │
│   └───┘                                │
│                                         │
│   手指滑动 → 松手自动贴边                │
│   单击 → 展开聊天面板                    │
│                                         │
└─────────────────────────────────────────┘
```

### 聊天面板

```
┌─────────────────────────────────────────┐
│  AI 助手                            [X] │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ 你好！我是你的 AI 助手。           │   │
│  │ 我可以帮你总结文章、翻译内容...    │   │
│  └─────────────────────────────────┘   │
│                                         │
│           ┌─────────────────────────────────┐   │
│           │ 用户消息                           │   │
│           └─────────────────────────────────┘   │
│                                         │
│                                         │
├─────────────────────────────────────────┤
│  [输入你想问的问题...]            [发送] │
└─────────────────────────────────────────┘
```

---

## 技术架构

### 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0.21 |
| UI 框架 | Jetpack Compose | BOM 2024.09.00 |
| 设计风格 | Material Design 3 | - |
| 架构模式 | MVVM | - |
| 状态管理 | StateFlow | - |
| 生命周期 | Lifecycle | 2.6.1 |
| 编译工具 | Gradle | 9.2.1 |
| 最低 SDK | Android 11 | API 30 |
| 目标 SDK | Android 14 | API 36 |

### 架构图

```
┌──────────────────────────────────────────────────────┐
│                    应用层 (Application)                │
├──────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────────┐         ┌──────────────────────┐    │
│  │ MainActivity │────────→│ FloatingService      │    │
│  └──────────────┘         │ (前台服务)            │    │
│                            └──────────┬───────────┘    │
│                                       │                │
│                            ┌──────────▼───────────┐    │
│                            │ ComposeView          │    │
│                            │ (悬浮层)              │    │
│                            └──────────┬───────────┘    │
│                                       │                │
│     ┌──────────────────────────────────┼────────────┐  │
│     │                                  │            │  │
│  ┌──▼──────────┐              ┌────────▼─────────┐ │  │
│  │FloatingBall │              │ ChatPanel         │ │  │
│  │(悬浮球组件)  │              │ (聊天面板)        │ │  │
│  └─────────────┘              └────────┬─────────┘ │  │
│                                        │            │  │
│                                 ┌──────▼─────────┐  │  │
│                                 │ ChatViewModel   │  │  │
│                                 │ (业务逻辑)      │  │  │
│                                 └─────────────────┘  │  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 关键设计

1. **MVVM 架构** - UI 与业务逻辑分离
2. **前台服务** - 确保应用在后台持续运行
3. **Compose 声明式 UI** - 高效渲染悬浮层
4. **StateFlow** - 响应式状态管理
5. **错误处理** - 完善的异常捕获与恢复

---

## 项目结构

```
day01/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/myapplication/
│   │   │   │   │
│   │   │   │   ├── MainActivity.kt          # 主活动
│   │   │   │   │
│   │   │   │   ├── FloatingService.kt       # 悬浮窗服务 ⭐
│   │   │   │   │
│   │   │   │   ├── components/              # UI 组件
│   │   │   │   │   ├── FloatingBall.kt      # 悬浮球
│   │   │   │   │   └── ChatPanel.kt         # 聊天面板
│   │   │   │   │
│   │   │   │   ├── viewmodel/               # ViewModel
│   │   │   │   │   └── ChatViewModel.kt     # 聊天逻辑
│   │   │   │   │
│   │   │   │   ├── model/                   # 数据模型
│   │   │   │   │   └── ChatMessage.kt       # 消息模型
│   │   │   │   │
│   │   │   │   └── ui/theme/               # 主题配置
│   │   │   │       ├── Theme.kt
│   │   │   │       ├── Color.kt
│   │   │   │       └── Type.kt
│   │   │   │
│   │   │   └── res/                         # 资源文件
│   │   │       ├── drawable/
│   │   │       ├── values/
│   │   │       └── xml/
│   │   │
│   │   ├── test/                            # 单元测试
│   │   │   └── java/.../
│   │   │       ├── ChatMessageTest.kt
│   │   │       ├── ChatViewModelTest.kt
│   │   │       └── FloatingBallTest.kt
│   │   │
│   │   └── androidTest/                      # 仪器测试
│   │       └── java/.../
│   │           └── ExampleInstrumentedTest.kt
│   │
│   └── build.gradle.kts                     # 应用构建配置
│
├── gradle/                                  # Gradle 包装器
│   └── wrapper/
│
├── build.gradle.kts                         # 根构建配置
├── settings.gradle.kts                      # 项目设置
├── gradle.properties                        # Gradle 属性
└── README.md                                # 项目文档
```

---

## 快速开始

### 环境要求

- Android Studio Hedgehog (2024.1.1) 或更高版本
- JDK 17+
- Android SDK API 30+
- Kotlin 2.0+

### 克隆项目

```bash
git clone https://github.com/yangbin09/AI-Floating-Assistant.git
cd AI-Floating-Assistant
```

### 导入 Android Studio

1. 打开 Android Studio
2. 选择 `File → Open`
3. 选择项目根目录 `day01`
4. 等待 Gradle 同步完成

### 构建 APK

#### 命令行构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

#### Android Studio 构建

1. 选择 `Build → Generate Signed Bundle / APK`
2. 选择 `Android App Bundle` 或 `APK`
3. 配置签名（可选）
4. 点击 `Finish`

### 安装运行

```bash
# 通过 adb 安装
adb install app/build/outputs/apk/debug/app-debug.apk

# 或者拖拽 APK 到模拟器/设备
```

### 首次使用

1. 安装并打开应用
2. 点击「开启 AI 助手」按钮
3. 系统会请求「显示在其他应用上层」权限
4. 授权后，悬浮球将出现在屏幕上
5. 拖动悬浮球到合适位置
6. 点击悬浮球展开聊天面板

---

## 核心模块

### FloatingService

悬浮窗核心服务，负责：

- 创建和管理悬浮球视图
- 处理 Overlay 权限
- 管理聊天面板的显示/隐藏
- 前台服务通知

```kotlin
// 主要职责
class FloatingService : Service() {
    fun onCreate()           // 初始化窗口管理器、启动前台服务
    fun showFloatingBall()   // 创建悬浮球视图
    fun toggleChatPanel()     // 切换聊天面板显示状态
    fun snapToEdge()         // 悬浮球贴边逻辑
    fun onDestroy()          // 清理资源
}
```

### FloatingBall

悬浮球可组合组件，支持：

- 拖动移动
- 点击展开聊天
- 自动贴边
- 无障碍支持

```kotlin
@Composable
fun FloatingBall(
    onClick: () -> Unit,           // 点击回调
    onDrag: (deltaX, deltaY) -> Unit,  // 拖动回调
    onDragEnd: () -> Unit          // 拖动结束回调
)
```

### ChatPanel

聊天面板组件，包含：

- 消息列表（LazyColumn）
- 输入框
- 发送按钮
- 自动滚动
- AI 回复模拟

```kotlin
@Composable
fun ChatPanel(
    onClose: () -> Unit,           // 关闭回调
    viewModel: ChatViewModel       // 视图模型
)
```

### ChatViewModel

聊天业务逻辑：

- 消息状态管理
- AI 回复逻辑
- 打字机效果
- 输入验证

```kotlin
class ChatViewModel : ViewModel() {
    fun updateInputText(text: String)  // 更新输入
    fun sendMessage()                   // 发送消息
}
```

### ChatMessage

消息数据模型：

```kotlin
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,           // 消息内容
    val isUser: Boolean,           // 是否用户消息
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 测试说明

### 测试覆盖

| 测试类 | 测试数量 | 覆盖范围 |
|--------|---------|---------|
| ChatMessageTest | 6 | 模型创建、复制、ID 唯一性、时间戳 |
| ChatViewModelTest | 9 | 欢迎消息、输入更新、发送逻辑、并发保护 |
| FloatingBallTest | 2 | 回调函数验证 |

**总计：17 个单元测试**

### 运行测试

```bash
# 运行所有单元测试
./gradlew testDebugUnitTest

# 运行特定测试类
./gradlew testDebugUnitTest --tests "com.example.myapplication.ChatViewModelTest"

# 查看测试报告
open app/build/reports/tests/testDebugUnitTest/index.html
```

### 测试报告

测试结果位于：`app/build/reports/tests/testDebugUnitTest/index.html`

---

## 开发指南

### 添加新功能

1. **创建 ViewModel 处理业务逻辑**
   ```kotlin
   class NewFeatureViewModel : ViewModel() {
       // 业务逻辑
   }
   ```

2. **创建 Composable 组件**
   ```kotlin
   @Composable
   fun NewFeaturePanel(
       viewModel: NewFeatureViewModel = viewModel()
   ) {
       // UI 实现
   }
   ```

3. **在 FloatingService 中集成**
   ```kotlin
   private fun showNewFeature() {
       // 创建视图并添加到窗口管理器
   }
   ```

### 代码规范

- 使用 Kotlin 编码规范
- Compose 函数以 `PascalCase` 命名
- ViewModel 函数以 `camelCase` 命名
- 添加适当的注释和文档

### 调试技巧

```bash
# 查看详细日志
adb logcat -v time | grep myapplication

# 查看悬浮窗层级
adb shell dumpsys window windows | grep -A 10 "Window #"

# 重新安装并清除数据
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear com.example.myapplication
```

---

## 版本历史

### v1.0.0 (2026-04-07)

- ✅ 悬浮球功能实现
- ✅ 聊天面板 UI
- ✅ 模拟 AI 回复
- ✅ MVVM 架构重构
- ✅ 单元测试覆盖
- ✅ GitHub 仓库创建

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交 Issue

- 描述清晰的问题或建议
- 提供复现步骤（如适用）
- 附上日志或截图

### 提交 PR

1. Fork 本仓库
2. 创建特性分支 `git checkout -b feature/AmazingFeature`
3. 提交更改 `git commit -m 'Add AmazingFeature'`
4. 推送分支 `git push origin feature/AmazingFeature`
5. 创建 Pull Request

---

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。

```
Copyright 2026 Yang Bin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 联系方式

- **作者**：Yang Bin
- **邮箱**：yangbin199@email.com
- **GitHub**：[@yangbin09](https://github.com/yangbin09)

---

## 致谢

- [Jetpack Compose](https://developer.android.com/compose) - 现代 Android UI 工具包
- [Material Design 3](https://m3.material.io/) - 设计系统
- [Android Developers](https://developer.android.com/) - 开发文档

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐**

</div>
