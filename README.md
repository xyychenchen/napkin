# ImageFlow

> 一个安卓端调用 Google Gemini Nano Banana 系列 API 作图的 App。
> 界面借鉴 Google Flow（flow.google.com）的暗色极简风格。

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android">
  <img alt="Min SDK" src="https://img.shields.io/badge/minSDK-26-blue">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green">
  <img alt="Version" src="https://img.shields.io/badge/version-0.1.0-FFB74D">
</p>

## 这是什么

ImageFlow 是一个调用 Gemini API 生成/编辑图片的安卓客户端。支持 Google 官方所有 Nano Banana 系列模型，所有官方文档参数都可以在 App 里调。

## 支持的模型

| 显示名 | Model ID | 定位 |
|---|---|---|
| Nano Banana (Legacy) | `gemini-2.5-flash-image` | 初代稳定版 |
| Nano Banana 2 Lite | `gemini-3.1-flash-lite-image` | 最快最便宜 |
| Nano Banana 2 | `gemini-3.1-flash-image` | 通用主力，支持 4K |
| Nano Banana Pro | `gemini-3-pro-image` | 最强质量 |

## 支持的官方参数

所有参数都来自 [Gemini API 官方图像生成文档](https://ai.google.dev/gemini-api/docs/image-generation)：

| 参数 | 可选值 | 说明 |
|---|---|---|
| `model` | 4 个 model id | 见上表 |
| `input.text` | 自由文本 | prompt |
| `input.image` | base64 + mime_type | 上传参考图（每个 model 数量上限不同） |
| `response_format.mime_type` | `image/png` / `image/jpeg` | 输出格式 |
| `response_format.aspect_ratio` | 1:1 / 3:2 / 2:3 / 3:4 / 4:3 / 4:5 / 5:4 / 9:16 / 16:9 / 21:9 | 10 种比例 |
| `response_format.image_size` | 0.5K / 1K / 2K / 4K | 按 model 联动限制 |
| `previous_interaction_id` | 上一轮 interaction id | 多轮编辑链 |
| `tools.google_search` | enable/disable | 让模型联网查实时信息 |

## 使用流程

1. 装好 APK，打开 App
2. 点右上角 ⚙️ 进设置页
3. 填入 Gemini API Key（从 https://aistudio.google.com/apikey 获取），点"测试连接"
4. 选择默认模型和默认参数
5. 回到主页，输入 prompt（可选：上传参考图）
6. 点"生成"，等几秒看结果
7. 历史记录在底部时间线，点缩略图进详情页
8. 详情页可以"基于此继续编辑"做多轮迭代

## 技术栈

- Kotlin 1.9.22 / Compose BOM 2024.02 / Material 3
- OkHttp 4.12.0（REST 调 Gemini Interactions API）
- Coil 3.0.4（图片加载）
- Room 2.6.1（生成历史）
- EncryptedSharedPreferences（API Key 加密存储）
- 强制暗色主题（Flow 风格）

## 项目结构

```
imageflow/
├── .github/workflows/android-build.yml  # CI: push Debug, tag 签名 Release
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/imageflow/app/
    │   ├── ImageFlowApplication.kt
    │   ├── MainActivity.kt
    │   ├── data/
    │   │   ├── ImageFlowDatabase.kt       # Room DB
    │   │   ├── Migrations.kt              # 升级迁移
    │   │   ├── entity/HistoryEntity.kt
    │   │   ├── dao/HistoryDao.kt
    │   │   ├── repo/GenerateRepository.kt # API + 文件 + 历史 三合一
    │   │   └── datastore/SettingsStore.kt # API Key 加密存储
    │   ├── network/
    │   │   ├── GeminiApi.kt               # Interactions API 封装
    │   │   └── GeminiModels.kt            # 4 个 model 定义 + 参数约束
    │   ├── ui/
    │   │   ├── theme/                     # 暗色 Flow 调色板
    │   │   ├── screen/
    │   │   │   ├── HomeScreen.kt          # 输入 + 画布 + 历史
    │   │   │   ├── SettingsScreen.kt      # API Key + 模型 + 参数
    │   │   │   └── HistoryDetailScreen.kt # 详情 + 多轮编辑
    │   │   └── component/
    │   ├── viewmodel/
    │   │   ├── GenerateViewModel.kt
    │   │   └── SettingsViewModel.kt
    │   └── util/TimeFormatter.kt
    └── res/
```

## ⚠️ 中国大陆使用须知

`generativelanguage.googleapis.com` 在中国大陆无法直连。使用 ImageFlow 需要保证手机能访问 Google 服务（自备代理）。App 本身不内置代理。

## License

MIT — 见 [LICENSE](LICENSE)
