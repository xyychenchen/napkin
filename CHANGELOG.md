# Changelog

All notable changes to ImageFlow will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned for v0.2.0
- 多轮编辑链可视化（树形历史）
- 图片导出/分享到其他 App
- 批量生成（同 prompt 多 seed）
- 自定义 prompt 模板

## [0.1.5] - 2026-07-12

### Fixed
- 修复 Release 构建 R8 缺类错误（续）：Coil 3 是 Kotlin Multiplatform 项目，部分 expect/actual 类（如 `coil3.PlatformContext`）R8 找不到 actual 实现。proguard-rules.pro 加 `-dontwarn coil3.**` + 完整 keep。同时预防性加上 bouncycastle / conscrypt / openjsse 的 dontwarn。

## [0.1.4] - 2026-07-12

### Fixed
- 修复 Release 构建 R8 缺类错误：`security-crypto` 传递依赖 `tink` 引用了 `com.google.errorprone.annotations.*` 但该库未在 runtime classpath。在 `proguard-rules.pro` 加 `-dontwarn com.google.errorprone.annotations.**`

## [0.1.3] - 2026-07-12

### Fixed
- 修复 Gradle 配置错误：`KEYSTORE_PATH` 环境变量为空字符串时 `file("")` 报"path may not be null or empty"。改成 `isNotEmpty()` 判断，让 main 分支构建（无签名）也能跑通。

## [0.1.2] - 2026-07-12

### Fixed
- 修复 `response.code()` 编译错误（OkHttp 4 已改为 val `response.code`）

## [0.1.1] - 2026-07-12

### Fixed
- 修复编译错误：OkHttp 4 的 `response.body()` 已改为 val `response.body`
- 修复编译错误：`HistoryDetailScreen` 删除操作需在 coroutine 里调 suspend 函数
- 修复编译错误：`GenerateViewModel` 缺 `Flow` import
- 修复 `GenerateViewModel.generate()` 拿 lastResult 时类型不匹配

## [0.1.0] - 2026-07-12

### Added
- 项目骨架：Kotlin + Jetpack Compose + Material 3，强制暗色主题（Flow 风格）
- 4 个 Gemini Nano Banana 系列模型可选：
  - **Nano Banana (Legacy)** — `gemini-2.5-flash-image`，老版本稳定
  - **Nano Banana 2 Lite** — `gemini-3.1-flash-lite-image`，最快最便宜
  - **Nano Banana 2** — `gemini-3.1-flash-image`，通用主力，支持 4K
  - **Nano Banana Pro** — `gemini-3-pro-image`，最强质量
- 调用官方 Interactions API（`/v1beta/interactions`），完整支持官方文档参数：
  - `model` — 4 个 model id 可选
  - `input` — text + 多张 image（base64），按 model 限制输入图数量
  - `response_format.type` — 固定 `image`
  - `response_format.mime_type` — `image/png` 或 `image/jpeg`
  - `response_format.aspect_ratio` — 10 种比例（1:1 / 3:2 / 2:3 / 3:4 / 4:3 / 4:5 / 5:4 / 9:16 / 16:9 / 21:9）
  - `response_format.image_size` — 0.5K / 1K / 2K / 4K（按 model 联动限制）
  - `previous_interaction_id` — 多轮编辑（详情页"基于此继续编辑"按钮触发）
  - `tools.google_search` — 让模型联网查实时信息
- API Key 用 EncryptedSharedPreferences 加密存储（基于 Android Keystore）
- 设置页：API Key 输入 + 测试连接 + 4 个 model 选择 + 全参数配置面板
- 主界面：上方画布显示最新生成 + 输入区 + 历史时间线（最近 20 条缩略图）
- 参数弹窗（底部 ModalBottomSheet）：model + 宽高比 + 分辨率 + 输出格式 + Google 搜索开关
- 切换 model 时自动重置不支持的参数到默认值
- 历史详情页：大图 + 元数据 + prompt + 基于此继续编辑 / 删除
- Room 数据库存储生成历史（prompt / 参数 / interactionId / 耗时 / 成功失败）
- 生成图片自动保存到 app 内部存储 `filesDir/generated/`
- GitHub Actions CI：push 构建 Debug APK，tag 自动签名 + 发 Release
- 复用 v0.1.4 期间生成的 keystore（Napkin2026!）做 release 签名

### Tech Stack
- Kotlin 1.9.22 / AGP 8.2.2 / Gradle 8.5 / JDK 17
- Compose BOM 2024.02 / Material 3
- Room 2.6.1 + KSP
- OkHttp 4.12.0（调 Gemini API）
- Coil 3.0.4（图片加载，Compose 原生）
- EncryptedSharedPreferences 1.1.0-alpha06（API Key 加密）
- Navigation Compose 2.7.7

### Known Limitations
- 仅本地存储，无云同步
- 中国大陆访问 `generativelanguage.googleapis.com` 需要代理，App 不内置代理
- 不支持图片附件到历史记录的 input（输入图只存 base64，关闭 App 后输入图丢失，但输出图永久保存）
- 不支持多 Interaction 链式历史的可视化
