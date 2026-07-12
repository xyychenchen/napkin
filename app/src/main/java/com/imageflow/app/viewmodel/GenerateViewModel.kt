package com.imageflow.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imageflow.app.ImageFlowApplication
import com.imageflow.app.data.datastore.SettingsStore
import com.imageflow.app.data.entity.HistoryEntity
import com.imageflow.app.data.repo.GenerateRepository
import com.imageflow.app.network.GenerateRequest
import com.imageflow.app.network.GenerateResult
import com.imageflow.app.network.GeminiModel
import com.imageflow.app.network.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GenerateViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as ImageFlowApplication).database
    private val settings = SettingsStore(app)
    private val repo = GenerateRepository(app, db.historyDao())

    val recentHistory: StateFlow<List<HistoryEntity>> = repo.observeRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // === 当前编辑状态 ===
    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _inputImages = MutableStateFlow<List<InputImage>>(emptyList())
    val inputImages: StateFlow<List<InputImage>> = _inputImages.asStateFlow()

    private val _selectedModelId = MutableStateFlow(settings.getDefaultModelId())
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _aspectRatio = MutableStateFlow(settings.getDefaultAspectRatio())
    val aspectRatio: StateFlow<String> = _aspectRatio.asStateFlow()

    private val _imageSize = MutableStateFlow(settings.getDefaultImageSize())
    val imageSize: StateFlow<String> = _imageSize.asStateFlow()

    private val _outputMime = MutableStateFlow(settings.getDefaultOutputMime())
    val outputMime: StateFlow<String> = _outputMime.asStateFlow()

    private val _enableGoogleSearch = MutableStateFlow(settings.isGoogleSearchEnabled())
    val enableGoogleSearch: StateFlow<Boolean> = _enableGoogleSearch.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _lastResult = MutableStateFlow<HistoryEntity?>(null)
    val lastResult: StateFlow<HistoryEntity?> = _lastResult.asStateFlow()

    fun selectedModel(): GeminiModel = GeminiModel.byId(_selectedModelId.value)

    fun updatePrompt(value: String) { _prompt.value = value }

    fun selectModel(id: String) {
        _selectedModelId.value = id
        // 切换 model 时如果当前参数不被支持，自动重置成默认值
        val m = GeminiModel.byId(id)
        if (_aspectRatio.value !in m.supportedAspectRatios) {
            _aspectRatio.value = m.supportedAspectRatios.first()
        }
        if (_imageSize.value !in m.supportedImageSizes) {
            _imageSize.value = m.supportedImageSizes.first()
        }
    }

    fun setAspectRatio(value: String) { _aspectRatio.value = value }
    fun setImageSize(value: String) { _imageSize.value = value }
    fun setOutputMime(value: String) { _outputMime.value = value }
    fun toggleGoogleSearch(enabled: Boolean) { _enableGoogleSearch.value = enabled }

    /**
     * 把选中 Uri 加为输入图。会异步读 base64。
     */
    fun addInputImage(uri: Uri) {
        viewModelScope.launch {
            val img = repo.uriToInputImage(uri) ?: return@launch
            val current = _inputImages.value.toMutableList()
            val max = selectedModel().maxInputImages
            if (current.size >= max) {
                _lastError.value = "当前 model 最多支持 $max 张输入图"
                return@launch
            }
            current.add(img)
            _inputImages.value = current
        }
    }

    fun removeInputImage(index: Int) {
        val current = _inputImages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _inputImages.value = current
        }
    }

    fun clearInputs() {
        _prompt.value = ""
        _inputImages.value = emptyList()
        _lastError.value = null
    }

    /** 给 Compose 用的便捷方法 */
    fun observeHistoryFlow(id: Long): Flow<HistoryEntity?> = repo.observeHistory(id)

    suspend fun deleteHistory(id: Long) {
        repo.deleteHistory(id)
    }

    /**
     * 触发生成。
     */
    fun generate(previousHistoryId: Long? = null, previousInteractionId: String? = null) {
        if (_isGenerating.value) return
        if (_prompt.value.isBlank() && _inputImages.value.isEmpty()) {
            _lastError.value = "请输入 prompt 或上传图片"
            return
        }
        val apiKey = settings.getApiKey()
        if (apiKey.isBlank()) {
            _lastError.value = "请先在设置页填写 API Key"
            return
        }

        _isGenerating.value = true
        _lastError.value = null

        viewModelScope.launch {
            val request = GenerateRequest(
                modelId = _selectedModelId.value,
                prompt = _prompt.value,
                inputImagesBase64 = _inputImages.value,
                aspectRatio = _aspectRatio.value,
                imageSize = _imageSize.value,
                outputMimeType = _outputMime.value,
                previousInteractionId = previousInteractionId,
                enableGoogleSearch = _enableGoogleSearch.value
            )
            val result = repo.generate(apiKey, request, previousHistoryId)
            _isGenerating.value = false
            if (result.success) {
                // 拿最新一条历史作为结果
                _lastResult.value = repo.getHistory(repo.observeRecentHistory().let { flow ->
                    // 简单做法：直接查最近一条
                    recentHistory.value.firstOrNull()
                })
            } else {
                _lastError.value = result.errorMessage
            }
        }
    }
}
