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
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * TagSettingsManager handles tag-related preferences and configuration settings.
 * It provides access to user preferences for AI tagging, API keys, and tagging limits.
 * Uses Android Keystore with javax.crypto.KeyGenerator for secure API key storage.
 */
class TagSettingsManager(ctx: Context) {
    private val preferences: SharedPreferences
    val apiKey: String?
        /**
         * Gets the OpenAI API key. Migrates from legacy plaintext if present.
         */
        get() {
            val enc =
                preferences.getString(PREF_API_KEY_ENCRYPTED, null)
            if (enc != null && !enc.trim { it <= ' ' }.isEmpty()) {
                val dec = decrypt(enc)
                if (dec != null && !dec.trim { it <= ' ' }.isEmpty()) return dec
            }
            val legacy =
                preferences.getString(PREF_API_KEY, null)
            if (legacy != null && !legacy.trim { it <= ' ' }.isEmpty()) {
                saveApiKey(legacy)
                preferences.edit { remove(PREF_API_KEY) }
                return legacy
            }
            return null
        }

    /** Saves the API key encrypted with AES/GCM in Android Keystore.  */
    fun saveApiKey(apiKey: String) {
        val trimmed = apiKey.trim { it <= ' ' }
        if (trimmed.isEmpty()) {
            preferences.edit { remove(PREF_API_KEY_ENCRYPTED) }
            return
        }
        val enc = encrypt(trimmed)
        if (enc != null) {
            preferences.edit { putString(PREF_API_KEY_ENCRYPTED, enc) }
        }
    }

    val isAiMode: Boolean
        get() = preferences.getBoolean(
            PREF_AI_AUTO_TAG,
            DEFAULT_AI_MODE
        )

    fun setAiMode(enabled: Boolean) {
        preferences.edit { putBoolean(PREF_AI_AUTO_TAG, enabled) }
    }
    val autoTagLimit: Int
        get() = preferences.getInt(
            PREF_AUTO_TAG_LIMIT,
            DEFAULT_AUTO_TAG_LIMIT
        )

    fun setAutoTagLimit(limit: Int) {
        preferences.edit { putInt(PREF_AUTO_TAG_LIMIT, limit) }
    }
    val selectedAiModelKey: String
        get() = preferences.getString(
            PREF_AI_MODEL,
            DEFAULT_AI_MODEL
        )!!

    val isAiTaggingConfigured: Boolean
        get() = this.isAiMode && hasValidApiKey()

    fun hasValidApiKey(): Boolean {
        val k = this.apiKey
        return k != null && !k.trim { it <= ' ' }.isEmpty()
    }

    val isAiConfirmationEnabled: Boolean
        get() = preferences.getBoolean(PREF_AI_CONFIRM, false)

    fun setAiConfirmationEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(PREF_AI_CONFIRM, enabled) }
    }

    init {
        val ctx1 = ctx.applicationContext
        this.preferences = PreferenceManager.getDefaultSharedPreferences(ctx1)
    }

    private val orCreateSecretKey: SecretKey?
        get() {
            try {
                val ks =
                    KeyStore.getInstance(ANDROID_KEYSTORE)
                ks.load(null)
                if (ks.containsAlias(KEY_ALIAS)) {
                    val entry = ks.getEntry(
                        KEY_ALIAS,
                        null
                    ) as KeyStore.SecretKeyEntry
                    return entry.secretKey
                }
                val keyGen: KeyGenerator = KeyGenerator.getInstance(
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
                return keyGen.generateKey()
            } catch (_: Exception) {
                return null
            }
        }

    private fun encrypt(plaintext: String): String? {
        try {
            val key = this.orCreateSecretKey
            if (key == null) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv // 12 bytes
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val out = ByteArray(12 + cipherBytes.size)
            System.arraycopy(iv, 0, out, 0, 12)
            System.arraycopy(cipherBytes, 0, out, 12, cipherBytes.size)
            return Base64.encodeToString(out, Base64.NO_WRAP)
        } catch (_: Exception) {
            return null
        }
    }

    private fun decrypt(base64: String?): String? {
        try {
            val key = this.orCreateSecretKey
            if (key == null) return null
            val all = Base64.decode(base64, Base64.NO_WRAP)
            if (all.size < 13) return null
            val iv = ByteArray(12)
            System.arraycopy(all, 0, iv, 0, 12)
            val cipherBytes = ByteArray(all.size - 12)
            System.arraycopy(all, 12, cipherBytes, 0, cipherBytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcm = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcm)
            val plain = cipher.doFinal(cipherBytes)
            return String(plain, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            return null
        }
    }

    companion object {
        private const val PREF_API_KEY = "openai_api_key" // legacy plaintext (migrated on read)
        private const val PREF_API_KEY_ENCRYPTED = "openai_api_key_encrypted"
        private const val PREF_AI_AUTO_TAG = "pref_ai_auto_tag"
        private const val PREF_AI_MODEL = "pref_ai_model"
        private const val PREF_AUTO_TAG_LIMIT = "auto_tag_limit"
        private const val PREF_AI_CONFIRM = "pref_ai_confirm"

        // Default values
        private const val DEFAULT_AUTO_TAG_LIMIT = 3
        private const val DEFAULT_AI_MODE = false
        private const val DEFAULT_AI_MODEL = "gpt-4o-mini"

        // ===== Encryption helpers (Android Keystore AES/GCM) =====
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "quicknotes_api_key_aes"
    }

    // Convenience wrapper to align with SettingsFragment usage
    fun setApiKey(value: String) {
        saveApiKey(value)
    }
}