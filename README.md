# AI灵魂伴侣

<div align="center">

![Android](https://img.shields.io/badge/Android-36-green?style=flat-square&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat-square&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.00-blue?style=flat-square&logo=jetpackcompose)
![License](https://img.shields.io/badge/License-Apache%202.0-green?style=flat-square)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-green?style=flat-square&logo=githubactions)

**AI灵魂伴侣 - 运行在 Android 系统上的智能社交助手，自动读取 Soul 应用内容并生成回复建议**

[English](./README.md) | 简体中文

</div>

---

## 功能特性

### 核心功能

| 功能 | 描述 |
|------|------|
| 悬浮球 | 可拖动的悬浮球，松开自动贴边，点击展开设置界面 |
| 无障碍服务 | 自动读取 Soul 应用界面内容 |
| AI 智能回复 | 调用 DeepSeek API 生成智能回复建议 |
| 心动值追踪 | 记录与好友的心动值变化 |
| 匹配记录 | 保存历史匹配记录和聊天内容 |
| 确认模式 | AI 生成回复后需用户确认再发送 |

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
| 数据库 | Room | 2.6.1 |
| 网络 | OkHttp | 4.12.0 |
| 加密存储 | EncryptedSharedPreferences | 1.1.0-alpha06 |
| AI 集成 | DeepSeek API / Claude API | - |
| 最低 SDK | Android 11 | API 30 |
| 目标 SDK | Android 14 | API 35 |
| 编译工具 | Gradle | 9.2.1 |

### 架构图

```
┌──────────────────────────────────────────────────────────────┐
│                      AI灵魂伴侣                                │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────┐         ┌──────────────────────┐           │
│  │ MainActivity │────────→│ FloatingService      │           │
│  └──────────────┘         │ (前台服务)            │           │
│                            └──────────┬───────────┘           │
│                                       │                       │
│                            ┌──────────▼───────────┐         │
│                            │ ComposeView          │         │
│                            │ (悬浮层)              │         │
│                            └──────────┬───────────┘         │
│                                       │                       │
│     ┌─────────────────────────────────┼────────────────────┐ │
│     │                                 │                    │ │
│  ┌──▼──────────┐              ┌──────▼─────────┐         │ │
│  │ 4 Tab 导航  │              │ 悬浮球设置面板   │         │ │
│  │ (主屏幕)     │              │ (设置界面)       │         │ │
│  └─────────────┘              └──────┬─────────┘         │ │
│                                       │                    │ │
│                               ┌───────▼─────────┐        │ │
│                               │ MainViewModel   │        │ │
│                               │ (主界面逻辑)     │        │ │
│                               └───────┬─────────┘        │ │
│                                       │                    │ │
└───────────────────────────────────────┼────────────────────┘ │
                                        │                       │
┌───────────────────────────────────────▼────────────────────┐ │
│                         数据层                                 │ │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────┐  │ │
│  │ Room Database    │  │ DeepSeek API     │  │ ApiKey   │  │ │
│  │ (聊天/匹配记录)   │  │ (AI 回复生成)     │  │ Manager  │  │ │
│  └──────────────────┘  └──────────────────┘  └──────────┘  │ │
└───────────────────────────────────────────────────────────┘ │
│                                                                │
┌──────────────────────────────────────────────────────────────┐ │
│                      无障碍服务                                 │ │
│  ┌────────────────────────────────────────────────────────┐  │ │
│  │ SoulAccessibilityService                              │  │ │
│  │ - 读取 Soul 应用界面内容                               │  │ │
│  │ - 分析聊天阶段和用户关系                               │  │ │
│  │ - 自动化交互操作                                       │  │ │
│  └────────────────────────────────────────────────────────┘  │ │
└──────────────────────────────────────────────────────────────┘ │
```

---

## 项目结构

```
AI-Floating-Assistant/
├── app/
│   ├── src/main/
│   │   ├── java/com/aisoul/assistant/
│   │   │   │
│   │   │   ├── MainActivity.kt              # 主活动，权限请求
│   │   │   │
│   │   │   ├── FloatingService.kt           # 悬浮窗服务（前台服务）
│   │   │   │
│   │   │   ├── components/                  # UI 组件
│   │   │   │   ├── DashboardComponents.kt  # 主页仪表盘组件
│   │   │   │   └── MainScreen.kt           # 4 tab 主屏幕
│   │   │   │
│   │   │   ├── viewmodel/                   # ViewModel
│   │   │   │   └── MainViewModel.kt        # 主界面逻辑
│   │   │   │   └── ChatViewModel.kt        # 聊天逻辑
│   │   │   │
│   │   │   ├── model/                      # 数据模型
│   │   │   │   ├── SoulUser.kt             # Soul 用户模型
│   │   │   │   ├── ChatStage.kt           # 聊天阶段模型
│   │   │   │   └── Persona.kt             # AI 人设模型
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── local/                   # 本地存储
│   │   │   │   │   ├── dao/                # Room DAO
│   │   │   │   │   └── AppDatabase.kt      # Room Database
│   │   │   │   └── remote/                  # 远程 API
│   │   │   │       ├── DeepSeekApiClient.kt  # DeepSeek API 客户端
│   │   │   │       ├── ClaudeApiClient.kt   # Claude API 客户端
│   │   │   │       └── ApiKeyManager.kt    # API Key 加密管理
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── strategy/              # 业务策略
│   │   │   │   │   ├── ReplyGenerator.kt   # 回复生成器
│   │   │   │   │   ├── ChatStageAnalyzer.kt  # 聊天阶段分析
│   │   │   │   │   └── PersonaPromptBuilder.kt  # 人设提示词构建
│   │   │   │   └── usecase/               # 用例
│   │   │   │       └── GenerateAutoReplyUseCase.kt
│   │   │   │
│   │   │   ├── accessibility/              # 无障碍服务
│   │   │   │   ├── SoulAccessibilityService.kt  # Soul 无障碍服务
│   │   │   │   ├── SoulPageAnalyzer.kt    # 页面分析器
│   │   │   │   ├── SoulListAnalyzer.kt    # 列表分析器
│   │   │   │   ├── SoulChatAnalyzer.kt    # 聊天分析器
│   │   │   │   └── SoulProfileAnalyzer.kt # 个人资料分析器
│   │   │   │
│   │   │   └── ui/theme/                   # 主题配置
│   │   │       └── Theme.kt
│   │   │
│   │   └── res/                            # 资源文件
│   │
│   └── build.gradle.kts                    # 应用构建配置
│
├── .github/
│   └── workflows/
│       └── android.yml                      # CI/CD 流水线
│
├── gradle/                                 # Gradle 包装器
├── build.gradle.kts                        # 根构建配置
├── settings.gradle.kts                      # 项目设置
├── gradle.properties                        # Gradle 属性
└── README.md                               # 项目文档
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
6. 点击悬浮球进入主界面
7. 在设置中配置 DeepSeek API Key

---

## 配置 AI API Key

1. 获取 [DeepSeek API Key](https://platform.deepseek.com/)
2. 打开应用 → AI 版本 → API Key
3. 输入你的 API Key
4. 应用会自动加密保存

---

## 自动回复功能

### 工作流程

1. **无障碍服务**：通过 AccessibilityService 读取 Soul 应用界面内容
2. **内容分析**：分析当前页面类型（列表/聊天/个人资料）
3. **聊天阶段识别**：识别匹配中、聊天中、心动值变化等阶段
4. **AI 生成**：调用 DeepSeek API 生成符合人设的回复
5. **确认发送**：用户确认后通过无障碍服务执行点击发送

---

## CI/CD 流水线

项目使用 GitHub Actions 进行持续集成和部署：

### 流水线功能

- ✅ 每次 PR 和 push 自动构建
- ✅ 运行单元测试
- ✅ 生成 Debug APK
- ✅ 上传 APK 作为构建产物
- ✅ 生成 Release APK (通过手动触发)

### 触发方式

| 触发条件 | 构建类型 | 说明 |
|---------|---------|------|
| push to main | Debug | 自动构建并测试 |
| pull request | Debug | PR 构建验证 |
| tag v* | Release | 发布版本 |
| 手动触发 | Debug/Release | 可选择构建类型 |

### 下载构建产物

构建完成后，点击 Actions 标签页，选择对应的运行记录，在 Artifacts 部分下载 APK。

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
./gradlew testDebugUnitTest --tests "com.aisoul.assistant.ChatViewModelTest"

# 查看测试报告
open app/build/reports/tests/testDebugUnitTest/index.html
```

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
adb logcat -v time | grep aisoul

# 查看悬浮窗层级
adb shell dumpsys window windows | grep -A 10 "Window #"

# 重新安装并清除数据
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear com.aisoul.assistant
```

---

## 版本历史

### v2.0.0 (2026-04-07)

- ✅ 包名迁移 - 从 com.example.myapplication 迁移到 com.aisoul.assistant
- ✅ 应用名称 - 更名为 "AI灵魂伴侣"
- ✅ 应用图标 - 紫色聊天气泡图标
- ✅ 4 Tab 导航 - 悬浮球、AI版本、记录、工作台
- ✅ Soul 无障碍服务 - 自动读取 Soul 应用界面
- ✅ DeepSeek API 集成 - AI 智能自动回复
- ✅ 心动值追踪 - 记录与好友的心动值变化

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
- [DeepSeek API](https://platform.deepseek.com/) - AI 能力支持

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐**

</div>
