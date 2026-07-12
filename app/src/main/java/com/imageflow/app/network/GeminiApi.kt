package com.imageflow.app.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gemini Interactions API 客户端。
 *
 * 官方文档：https://ai.google.dev/gemini-api/docs/image-generation
 * 端点：POST https://generativelanguage.googleapis.com/v1beta/interactions
 *
 * 请求头：x-goog-api-key: <API_KEY>
 *
 * 请求体（核心字段，全部按官方文档实现）：
 * {
 *   "model": "gemini-3.1-flash-image",
 *   "input": [
 *     {"type": "text", "text": "..."},
 *     {"type": "image", "mime_type": "image/png", "data": "<base64>"}
 *   ],
 *   "response_format": {
 *     "type": "image",
 *     "mime_type": "image/png",
 *     "aspect_ratio": "1:1",
 *     "image_size": "1K"
 *   },
 *   "previous_interaction_id": "<id>",  // 可选
 *   "tools": [{"type": "google_search", "search_types": ["web_search", "image_search"]}]  // 可选
 * }
 *
 * 响应体：
 * {
 *   "id": "<interaction_id>",
 *   "output": [
 *     {"type": "text", "text": "..."},
 *     {"type": "image", "mime_type": "image/png", "data": "<base64>"}
 *   ]
 * }
 */
class GeminiApi(
    private val apiKey: String,
    private val baseUrl: String = BASE_URL,
    private val timeoutSeconds: Long = 120
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    /**
     * 调用一次生成。返回 [GenerateResult]。
     * 在 IO 线程执行，调用方不需要切线程。
     */
    suspend fun generate(request: GenerateRequest): GenerateResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GenerateResult(
                success = false,
                errorMessage = "API Key 未设置，请去设置页填写"
            )
        }

        val body = buildRequestBody(request)
        val mediaType = "application/json".toMediaTypeOrNull()
        val req = Request.Builder()
            .url(baseUrl)
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(mediaType))
            .build()

        val response = withTimeoutOrNull(timeoutSeconds * 1000) {
            try {
                client.newCall(req).execute()
            } catch (e: Exception) {
                return@withTimeoutOrNull null
            }
        } ?: return@withContext GenerateResult(
            success = false,
            errorMessage = "请求超时（${timeoutSeconds}s），请检查网络或代理"
        )

        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            response.close()
            return@withContext GenerateResult(
                success = false,
                errorMessage = parseHttpError(response.code(), errBody)
            )
        }

        val respStr = response.body?.string() ?: ""
        response.close()
        parseSuccessResponse(respStr)
    }

    private fun buildRequestBody(request: GenerateRequest): String {
        val json = JSONObject()

        json.put("model", request.modelId)

        // input 数组
        val input = JSONArray()
        input.put(JSONObject().apply {
            put("type", "text")
            put("text", request.prompt)
        })
        request.inputImagesBase64.forEach { img ->
            input.put(JSONObject().apply {
                put("type", "image")
                put("mime_type", img.mimeType)
                put("data", img.base64Data)
            })
        }
        json.put("input", input)

        // response_format
        json.put("response_format", JSONObject().apply {
            put("type", "image")
            put("mime_type", request.outputMimeType)
            put("aspect_ratio", request.aspectRatio)
            put("image_size", request.imageSize)
        })

        // previous_interaction_id（多轮编辑）
        request.previousInteractionId?.let { json.put("previous_interaction_id", it) }

        // tools（google_search）
        if (request.enableGoogleSearch) {
            json.put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "google_search")
                    put("search_types", JSONArray().apply {
                        put("web_search")
                        put("image_search")
                    })
                })
            })
        }

        return json.toString()
    }

    private fun parseSuccessResponse(respStr: String): GenerateResult {
        return try {
            val json = JSONObject(respStr)
            val interactionId = json.optString("id", null)

            val output = json.optJSONArray("output") ?: JSONArray()
            var imageFound: OutputImage? = null

            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                if (item.optString("type") == "image") {
                    imageFound = OutputImage(
                        mimeType = item.optString("mime_type", "image/png"),
                        base64Data = item.optString("data", "")
                    )
                    break
                }
            }

            if (imageFound == null || imageFound.base64Data.isBlank()) {
                GenerateResult(
                    success = false,
                    interactionId = interactionId,
                    errorMessage = "API 返回成功但未包含图像数据"
                )
            } else {
                GenerateResult(
                    success = true,
                    outputImage = imageFound,
                    interactionId = interactionId
                )
            }
        } catch (e: Exception) {
            GenerateResult(
                success = false,
                errorMessage = "解析响应失败：${e.message}"
            )
        }
    }

    private fun parseHttpError(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val err = json.optJSONObject("error")
            val message = err?.optString("message", "") ?: body
            "HTTP $code: $message"
        } catch (e: Exception) {
            "HTTP $code: $body"
        }
    }

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
    }
}
