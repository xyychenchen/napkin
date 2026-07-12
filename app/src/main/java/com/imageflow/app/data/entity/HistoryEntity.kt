package com.imageflow.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次生成的完整记录。
 *
 * @param prompt          用户输入的 prompt
 * @param modelId         使用的 model id（如 "gemini-3.1-flash-image"）
 * @param inputImagePaths 输入图片的本地路径列表（JSON 数组字符串），可能为空（纯文生图）
 * @param outputImagePath 生成图片的本地路径
 * @param outputMimeType  生成图片的 mime type ("image/jpeg" / "image/png")
 * @param aspectRatio     实际使用的 aspect ratio
 * @param imageSize       实际使用的 image size ("1K" / "2K" 等)
 * @param previousId      多轮编辑时的上一条 history id（链式编辑）
 * @param interactionId   Gemini API 返回的 interaction id（用于后续接续编辑）
 * @param success         是否成功
 * @param errorMessage    失败时的错误信息
 * @param durationMs      API 调用耗时（毫秒）
 * @param createdAt       创建时间（毫秒）
 *
 * schema v1。后续字段添加走 [com.imageflow.app.data.Migrations]。
 */
@Entity(
    tableName = "history",
    indices = [Index("createdAt"), Index("previousId"), Index("modelId")]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val prompt: String,
    val modelId: String,
    val inputImagePaths: String = "[]",
    val outputImagePath: String? = null,
    val outputMimeType: String? = null,
    val aspectRatio: String? = null,
    val imageSize: String? = null,
    val previousId: Long? = null,
    val interactionId: String? = null,
    val success: Boolean = false,
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
