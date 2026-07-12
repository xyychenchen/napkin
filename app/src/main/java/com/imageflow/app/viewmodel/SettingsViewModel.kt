package com.imageflow.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imageflow.app.ImageFlowApplication
import com.imageflow.app.data.datastore.SettingsStore
import com.imageflow.app.network.GeminiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)

    private val _apiKey = MutableStateFlow(settings.getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _defaultModelId = MutableStateFlow(settings.getDefaultModelId())
    val defaultModelId: StateFlow<String> = _defaultModelId.asStateFlow()

    private val _aspectRatio = MutableStateFlow(settings.getDefaultAspectRatio())
    val aspectRatio: StateFlow<String> = _aspectRatio.asStateFlow()

    private val _imageSize = MutableStateFlow(settings.getDefaultImageSize())
    val imageSize: StateFlow<String> = _imageSize.asStateFlow()

    private val _outputMime = MutableStateFlow(settings.getDefaultOutputMime())
    val outputMime: StateFlow<String> = _outputMime.asStateFlow()

    private val _googleSearch = MutableStateFlow(settings.isGoogleSearchEnabled())
    val googleSearch: StateFlow<Boolean> = _googleSearch.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    val models: List<GeminiModel> = GeminiModel.ALL

    fun updateApiKey(value: String) {
        _apiKey.value = value
        settings.setApiKey(value)
    }

    fun updateDefaultModel(id: String) {
        _defaultModelId.value = id
        settings.setDefaultModelId(id)
    }

    fun updateAspectRatio(value: String) {
        _aspectRatio.value = value
        settings.setDefaultAspectRatio(value)
    }

    fun updateImageSize(value: String) {
        _imageSize.value = value
        settings.setDefaultImageSize(value)
    }

    fun updateOutputMime(value: String) {
        _outputMime.value = value
        settings.setDefaultOutputMime(value)
    }

    fun updateGoogleSearch(enabled: Boolean) {
        _googleSearch.value = enabled
        settings.setGoogleSearchEnabled(enabled)
    }

    /**
     * 测试 API Key 是否有效。
     * 用一个最便宜的请求（list models）验证。
     */
    fun testApiKey() {
        if (_testing.value) return
        val key = _apiKey.value.trim()
        if (key.isBlank()) {
            _testResult.value = "API Key 为空"
            return
        }
        _testing.value = true
        _testResult.value = null

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                    val req = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body()?.string() ?: ""
                            val json = JSONObject(body)
                            val models = json.optJSONArray("models")
                            "✅ 连接成功，可用模型数：${models?.length() ?: 0}"
                        } else {
                            val body = resp.body()?.string() ?: ""
                            val msg = try {
                                JSONObject(body).optJSONObject("error")?.optString("message", "") ?: body
                            } catch (e: Exception) { body }
                            "❌ HTTP ${resp.code()}: $msg"
                        }
                    }
                } catch (e: Exception) {
                    "❌ 网络错误：${e.message}"
                }
            }
            _testResult.value = result
            _testing.value = false
        }
    }
}
