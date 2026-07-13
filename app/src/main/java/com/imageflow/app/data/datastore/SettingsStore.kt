package com.imageflow.app.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * API Key + 用户设置的加密存储。
 *
 * API Key 是敏感凭证，绝不能明文存。这里用 EncryptedSharedPreferences：
 * - 基于 Android Keystore 生成的主密钥加密
 * - 文件落盘时整个 XML 都是密文
 * - root 设备理论上仍可被提取，但比明文安全得多
 *
 * 用户设置（默认 model、默认参数等）也存这里，避免再开一个 DataStore。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "imageflow_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // === API Key ===
    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""
    fun setApiKey(value: String) = prefs.edit().putString(KEY_API_KEY, value).apply()

    // === 默认 model ===
    fun getDefaultModelId(): String = prefs.getString(KEY_DEFAULT_MODEL, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID
    fun setDefaultModelId(value: String) = prefs.edit().putString(KEY_DEFAULT_MODEL, value).apply()

    // === 默认参数 ===
    fun getDefaultAspectRatio(): String = prefs.getString(KEY_ASPECT_RATIO, "1:1") ?: "1:1"
    fun setDefaultAspectRatio(value: String) = prefs.edit().putString(KEY_ASPECT_RATIO, value).apply()

    fun getDefaultImageSize(): String = prefs.getString(KEY_IMAGE_SIZE, "1K") ?: "1K"
    fun setDefaultImageSize(value: String) = prefs.edit().putString(KEY_IMAGE_SIZE, value).apply()

    fun getDefaultOutputMime(): String = prefs.getString(KEY_OUTPUT_MIME, "image/png") ?: "image/png"
    fun setDefaultOutputMime(value: String) = prefs.edit().putString(KEY_OUTPUT_MIME, value).apply()

    fun isGoogleSearchEnabled(): Boolean = prefs.getBoolean(KEY_GOOGLE_SEARCH, false)
    fun setGoogleSearchEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_GOOGLE_SEARCH, value).apply()

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEFAULT_MODEL = "default_model"
        private const val KEY_ASPECT_RATIO = "aspect_ratio"
        private const val KEY_IMAGE_SIZE = "image_size"
        private const val KEY_OUTPUT_MIME = "output_mime"
        private const val KEY_GOOGLE_SEARCH = "google_search"

        const val DEFAULT_MODEL_ID = "gemini-3.1-flash-image"
    }
}
