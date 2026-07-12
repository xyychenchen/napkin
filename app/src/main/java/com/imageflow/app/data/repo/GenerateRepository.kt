package com.imageflow.app.data.repo

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.imageflow.app.data.dao.HistoryDao
import com.imageflow.app.data.entity.HistoryEntity
import com.imageflow.app.network.GenerateRequest
import com.imageflow.app.network.GenerateResult
import com.imageflow.app.network.GeminiApi
import com.imageflow.app.network.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 生成仓库：把 API 调用 + 图片文件管理 + 历史记录 三件事串起来。
 */
class GenerateRepository(
    private val context: Context,
    private val historyDao: HistoryDao
) {
    /**
     * 读取输入 Uri 转成 base64。在 IO 线程做。
     */
    suspend fun uriToInputImage(uri: Uri): InputImage? = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/png"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            InputImage(mimeType = mimeType, base64Data = base64)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成图片，把结果存到本地，并写入历史记录。
     */
    suspend fun generate(
        apiKey: String,
        request: GenerateRequest,
        previousHistoryId: Long? = null
    ): GenerateResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val api = GeminiApi(apiKey)

        // 先在 DB 创建一条"进行中"的记录
        val inputPathsJson = JSONArray().apply {
            // 输入图片暂时没存到本地文件，只在历史里记 input 数量
        }.toString()
        val history = HistoryEntity(
            prompt = request.prompt,
            modelId = request.modelId,
            inputImagePaths = inputPathsJson,
            aspectRatio = request.aspectRatio,
            imageSize = request.imageSize,
            outputMimeType = request.outputMimeType,
            previousId = previousHistoryId,
            success = false
        )
        val historyId = historyDao.insert(history)

        val result = api.generate(request)
        val duration = System.currentTimeMillis() - startedAt

        if (result.success && result.outputImage != null) {
            // 把 base64 图片存到本地
            val outFile = saveImageToFile(result.outputImage.base64Data, result.outputImage.mimeType)
            val updated = history.copy(
                id = historyId,
                outputImagePath = outFile.absolutePath,
                outputMimeType = result.outputImage.mimeType,
                interactionId = result.interactionId,
                success = true,
                durationMs = duration
            )
            historyDao.update(updated)
        } else {
            // 失败也记录
            val updated = history.copy(
                id = historyId,
                success = false,
                errorMessage = result.errorMessage,
                durationMs = duration
            )
            historyDao.update(updated)
        }

        result.copy()
    }

    private fun saveImageToFile(base64: String, mimeType: String): File {
        val ext = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> "png"
        }
        val dir = File(context.filesDir, "generated").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    fun observeRecentHistory(): Flow<List<HistoryEntity>> = historyDao.observeRecent()

    suspend fun getHistory(id: Long): HistoryEntity? = historyDao.getById(id)

    fun observeHistory(id: Long): Flow<HistoryEntity?> = historyDao.observeById(id)

    suspend fun deleteHistory(id: Long) {
        val h = historyDao.getById(id) ?: return
        // 删本地图片文件
        h.outputImagePath?.let { File(it).delete() }
        historyDao.deleteById(id)
    }
}
