package com.imageflow.app.network

/**
 * Gemini 官方支持的图像生成 model 列表。
 * 数据来源：https://ai.google.dev/gemini-api/docs/image-generation (2026-07 抓取)
 *
 * 注意：每个 model 支持的 aspectRatio 和 imageSize 不同，UI 需要根据所选 model 联动限制。
 */
data class GeminiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val maxInputImages: Int,
    val supportedAspectRatios: List<String>,
    val supportedImageSizes: List<String>
) {
    companion object {
        /**
         * 官方 4 个 Nano Banana 系列图像生成模型。
         */
        val ALL: List<GeminiModel> = listOf(
            GeminiModel(
                id = "gemini-2.5-flash-image",
                displayName = "Nano Banana (Legacy)",
                description = "Gemini 2.5 Flash Image。Nano Banana 系列的初代，稳定可靠但官方建议迁移到 2 Lite。",
                maxInputImages = 3,
                supportedAspectRatios = listOf("1:1"),
                supportedImageSizes = listOf("1K")
            ),
            GeminiModel(
                id = "gemini-3.1-flash-lite-image",
                displayName = "Nano Banana 2 Lite",
                description = "Gemini 3.1 Flash Lite Image。最快最便宜，适合高并发场景。仅支持 1K。",
                maxInputImages = 14,
                supportedAspectRatios = listOf("1:1", "3:2", "2:3", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9"),
                supportedImageSizes = listOf("1K")
            ),
            GeminiModel(
                id = "gemini-3.1-flash-image",
                displayName = "Nano Banana 2",
                description = "Gemini 3.1 Flash Image。通用主力，支持 4K 输出，平衡速度与质量。",
                maxInputImages = 14,
                supportedAspectRatios = listOf("1:1", "3:2", "2:3", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9"),
                supportedImageSizes = listOf("0.5K", "1K", "2K", "4K")
            ),
            GeminiModel(
                id = "gemini-3-pro-image",
                displayName = "Nano Banana Pro",
                description = "Gemini 3 Pro Image。最强质量，世界知识最丰富，适合复杂视觉任务。",
                maxInputImages = 6,
                supportedAspectRatios = listOf("1:1", "3:2", "2:3", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9"),
                supportedImageSizes = listOf("1K", "2K", "4K")
            )
        )

        fun byId(id: String): GeminiModel = ALL.firstOrNull { it.id == id } ?: ALL[2]
    }
}

/**
 * 一次生成的请求参数。对应官方 Interactions API 的请求体字段。
 *
 * @param modelId              模型 id
 * @param prompt               文字指令
 * @param inputImagesBase64    输入图片列表（base64 编码 + mime type）
 * @param aspectRatio          输出图比例（必须在该 model 的 supportedAspectRatios 内）
 * @param imageSize            输出图分辨率（必须在该 model 的 supportedImageSizes 内）
 * @param outputMimeType       "image/jpeg" 或 "image/png"
 * @param previousInteractionId 多轮编辑时上一轮的 interaction id
 * @param enableGoogleSearch   是否启用 google_search 工具（让模型联网查实时信息）
 */
data class GenerateRequest(
    val modelId: String,
    val prompt: String,
    val inputImagesBase64: List<InputImage>,
    val aspectRatio: String,
    val imageSize: String,
    val outputMimeType: String,
    val previousInteractionId: String? = null,
    val enableGoogleSearch: Boolean = false
)

data class InputImage(
    val mimeType: String,
    val base64Data: String
)

/**
 * API 返回的生成结果。
 */
data class GenerateResult(
    val success: Boolean,
    val outputImage: OutputImage? = null,
    val interactionId: String? = null,
    val errorMessage: String? = null
)

data class OutputImage(
    val mimeType: String,
    val base64Data: String
)
