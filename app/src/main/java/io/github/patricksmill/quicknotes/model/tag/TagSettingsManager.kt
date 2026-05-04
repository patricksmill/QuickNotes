package io.github.patricksmill.quicknotes.model.tag

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * TagSettingsManager handles AI provider selection and the related credentials.
 */
class TagSettingsManager(ctx: Context) {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(ctx.applicationContext)

    enum class AiProvider(
        val storageKey: String,
        val displayName: String,
        val defaultEndpoint: String,
        val defaultModel: String
    ) {
        OPENAI(
            "openai",
            "OpenAI",
            "https://api.openai.com/v1",
            "gpt-5-mini"
        ),
        ANTHROPIC(
            "anthropic",
            "Claude",
            "https://api.anthropic.com/v1",
            "claude-sonnet-4-20250514"
        ),
        GOOGLE(
            "google",
            "Google",
            "https://generativelanguage.googleapis.com/v1beta/openai",
            "gemini-2.5-flash"
        ),
        CUSTOM(
            "custom",
            "Custom",
            "https://api.openai.com/v1",
            "gpt-5-mini"
        )
    }

    data class AiConfiguration(
        val provider: AiProvider,
        val apiKey: String?,
        val endpoint: String,
        val model: String
    )

    val selectedProvider: AiProvider
        get() {
            val stored = preferences.getString(PREF_AI_PROVIDER, null)
            return when {
                stored != null -> parseProvider(stored)
                legacyCustomEndpoint().isNotBlank() && legacyCustomEndpoint() != AiProvider.OPENAI.defaultEndpoint -> AiProvider.CUSTOM
                else -> AiProvider.OPENAI
            }
        }

    fun setSelectedProvider(provider: AiProvider) {
        preferences.edit { putString(PREF_AI_PROVIDER, provider.storageKey) }
    }

    val apiKey: String?
        get() = apiKeyFor(selectedProvider)

    fun apiKeyFor(provider: AiProvider): String? {
        val encrypted = preferences.getString(encryptedApiKeyKey(provider), null)
        if (!encrypted.isNullOrBlank()) {
            val decrypted = decrypt(encrypted)
            if (!decrypted.isNullOrBlank()) {
                return decrypted
            }
        }

        if (provider == AiProvider.OPENAI) {
            val legacy = preferences.getString(LEGACY_OPENAI_API_KEY, null)
            if (!legacy.isNullOrBlank()) {
                saveApiKey(provider, legacy)
                preferences.edit { remove(LEGACY_OPENAI_API_KEY) }
                return legacy
            }
        }

        if (provider == AiProvider.CUSTOM) {
            val openAiKey = apiKeyFor(AiProvider.OPENAI)
            if (!legacyCustomEndpoint().isBlank() && !openAiKey.isNullOrBlank()) {
                return openAiKey
            }
        }

        return null
    }

    fun saveApiKey(provider: AiProvider, apiKey: String) {
        val trimmed = apiKey.trim()
        val keyName = encryptedApiKeyKey(provider)

        if (trimmed.isBlank()) {
            preferences.edit { remove(keyName) }
            return
        }

        val enc = encrypt(trimmed)
        if (enc != null) {
            preferences.edit { putString(keyName, enc) }
        }
    }

    fun saveApiKey(value: String) {
        saveApiKey(selectedProvider, value)
    }

    val selectedAiEndpoint: String
        get() = endpointFor(selectedProvider)

    fun endpointFor(provider: AiProvider): String {
        return when (provider) {
            AiProvider.OPENAI -> AiProvider.OPENAI.defaultEndpoint
            AiProvider.ANTHROPIC -> AiProvider.ANTHROPIC.defaultEndpoint
            AiProvider.GOOGLE -> AiProvider.GOOGLE.defaultEndpoint
            AiProvider.CUSTOM -> {
                val endpoint = preferences.getString(CUSTOM_API_BASE_URL, null)
                    .orEmpty()
                    .trim()
                if (endpoint.isBlank()) legacyCustomEndpoint().ifBlank { AiProvider.CUSTOM.defaultEndpoint } else endpoint
            }
        }
    }

    fun setAiEndpoint(endpoint: String) {
        saveCustomBaseUrl(endpoint)
    }

    fun saveCustomBaseUrl(endpoint: String) {
        val trimmed = endpoint.trim()
        if (trimmed.isBlank()) {
            preferences.edit { remove(CUSTOM_API_BASE_URL) }
        } else {
            preferences.edit { putString(CUSTOM_API_BASE_URL, trimmed) }
        }
    }

    val selectedAiModelKey: String
        get() = modelFor(selectedProvider)

    fun modelFor(provider: AiProvider): String {
        val stored = when (provider) {
            AiProvider.OPENAI -> preferences.getString(modelKey(provider), null)
                ?: preferences.getString(LEGACY_CUSTOM_MODEL, null)
            AiProvider.ANTHROPIC -> preferences.getString(modelKey(provider), null)
            AiProvider.GOOGLE -> preferences.getString(modelKey(provider), null)
            AiProvider.CUSTOM -> preferences.getString(CUSTOM_AI_MODEL, null)
                ?: preferences.getString(LEGACY_CUSTOM_MODEL, null)
        }

        val trimmed = stored.orEmpty().trim()
        return if (trimmed.isBlank() || trimmed.equals("auto", ignoreCase = true)) {
            provider.defaultModel
        } else {
            trimmed
        }
    }

    fun setAiModel(model: String) {
        saveModel(selectedProvider, model)
    }

    fun saveModel(provider: AiProvider, model: String) {
        val trimmed = model.trim()
        val key = if (provider == AiProvider.CUSTOM) CUSTOM_AI_MODEL else modelKey(provider)
        if (trimmed.isBlank()) {
            preferences.edit { remove(key) }
        } else {
            preferences.edit { putString(key, trimmed) }
        }
    }

    val isAiTaggingConfigured: Boolean
        get() = this.isAiMode && hasValidApiKey()

    fun hasValidApiKey(): Boolean {
        val k = this.apiKey
        return !k.isNullOrBlank()
    }

    val isAiMode: Boolean
        get() = preferences.getBoolean(PREF_AI_AUTO_TAG, DEFAULT_AI_MODE)

    fun setAiMode(enabled: Boolean) {
        preferences.edit { putBoolean(PREF_AI_AUTO_TAG, enabled) }
    }

    val autoTagLimit: Int
        get() = preferences.getInt(PREF_AUTO_TAG_LIMIT, DEFAULT_AUTO_TAG_LIMIT)

    fun setAutoTagLimit(limit: Int) {
        preferences.edit { putInt(PREF_AUTO_TAG_LIMIT, limit) }
    }

    val isAiConfirmationEnabled: Boolean
        get() = preferences.getBoolean(PREF_AI_CONFIRM, false)

    fun setAiConfirmationEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(PREF_AI_CONFIRM, enabled) }
    }

    init {
        // Ensure legacy plaintext keys are folded into encrypted storage early.
        apiKeyFor(AiProvider.OPENAI)
    }

    private fun encryptedApiKeyKey(provider: AiProvider): String {
        return when (provider) {
            AiProvider.OPENAI -> OPENAI_API_KEY_ENCRYPTED
            AiProvider.ANTHROPIC -> ANTHROPIC_API_KEY_ENCRYPTED
            AiProvider.GOOGLE -> GOOGLE_API_KEY_ENCRYPTED
            AiProvider.CUSTOM -> CUSTOM_API_KEY_ENCRYPTED
        }
    }

    private fun modelKey(provider: AiProvider): String {
        return when (provider) {
            AiProvider.OPENAI -> OPENAI_AI_MODEL
            AiProvider.ANTHROPIC -> ANTHROPIC_AI_MODEL
            AiProvider.GOOGLE -> GOOGLE_AI_MODEL
            AiProvider.CUSTOM -> CUSTOM_AI_MODEL
        }
    }

    private fun legacyCustomEndpoint(): String {
        return preferences.getString(LEGACY_CUSTOM_API_BASE_URL, null).orEmpty().trim()
    }

    private fun parseProvider(value: String): AiProvider {
        return providerForStorageKey(value)
    }

    private val orCreateSecretKey: SecretKey?
        get() {
            return try {
                val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
                ks.load(null)
                if (ks.containsAlias(KEY_ALIAS)) {
                    val entry = ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                    entry.secretKey
                } else {
                    val keyGen = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEYSTORE
                    )
                    val spec = KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                    keyGen.init(spec)
                    keyGen.generateKey()
                }
            } catch (_: Exception) {
                null
            }
        }

    private fun encrypt(plaintext: String): String? {
        return try {
            val key = orCreateSecretKey ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val out = ByteArray(12 + cipherBytes.size)
            System.arraycopy(iv, 0, out, 0, 12)
            System.arraycopy(cipherBytes, 0, out, 12, cipherBytes.size)
            Base64.encodeToString(out, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun decrypt(base64: String?): String? {
        return try {
            val key = orCreateSecretKey ?: return null
            val all = Base64.decode(base64, Base64.NO_WRAP)
            if (all.size < 13) return null
            val iv = ByteArray(12)
            System.arraycopy(all, 0, iv, 0, 12)
            val cipherBytes = ByteArray(all.size - 12)
            System.arraycopy(all, 12, cipherBytes, 0, cipherBytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun providerDisplayName(provider: AiProvider): String = provider.displayName

        fun providerForStorageKey(storageKey: String): AiProvider {
            return when (storageKey.lowercase(Locale.US)) {
                AiProvider.ANTHROPIC.storageKey -> AiProvider.ANTHROPIC
                AiProvider.GOOGLE.storageKey -> AiProvider.GOOGLE
                AiProvider.CUSTOM.storageKey -> AiProvider.CUSTOM
                else -> AiProvider.OPENAI
            }
        }

        private const val PREF_AI_PROVIDER = "pref_ai_provider"
        private const val PREF_AI_AUTO_TAG = "pref_ai_auto_tag"
        private const val PREF_AUTO_TAG_LIMIT = "auto_tag_limit"
        private const val PREF_AI_CONFIRM = "pref_ai_confirm"

        private const val OPENAI_API_KEY_ENCRYPTED = "openai_api_key_encrypted"
        private const val ANTHROPIC_API_KEY_ENCRYPTED = "anthropic_api_key_encrypted"
        private const val GOOGLE_API_KEY_ENCRYPTED = "google_api_key_encrypted"
        private const val CUSTOM_API_KEY_ENCRYPTED = "custom_api_key_encrypted"

        private const val OPENAI_AI_MODEL = "openai_ai_model"
        private const val ANTHROPIC_AI_MODEL = "anthropic_ai_model"
        private const val GOOGLE_AI_MODEL = "google_ai_model"
        private const val CUSTOM_AI_MODEL = "custom_ai_model"

        private const val CUSTOM_API_BASE_URL = "custom_api_base_url"

        private const val LEGACY_OPENAI_API_KEY = "openai_api_key"
        private const val LEGACY_CUSTOM_API_BASE_URL = "openai_api_base_url"
        private const val LEGACY_CUSTOM_MODEL = "pref_ai_model"

        private const val DEFAULT_AUTO_TAG_LIMIT = 3
        private const val DEFAULT_AI_MODE = false

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "quicknotes_api_key_aes"
    }

    fun currentConfiguration(): AiConfiguration {
        return AiConfiguration(
            provider = selectedProvider,
            apiKey = apiKey,
            endpoint = selectedAiEndpoint,
            model = selectedAiModelKey
        )
    }
}
