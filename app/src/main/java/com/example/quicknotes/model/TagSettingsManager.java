package com.example.quicknotes.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

/**
 * TagSettingsManager handles tag-related preferences and configuration settings.
 * It provides access to user preferences for AI tagging, API keys, and tagging limits.
 * Uses Android Keystore with javax.crypto.KeyGenerator for secure API key storage.
 */
public class TagSettingsManager {
    private final SharedPreferences preferences;
    private static final String PREF_API_KEY = "openai_api_key"; // legacy plaintext (migrated on read)
    private static final String PREF_API_KEY_ENCRYPTED = "openai_api_key_encrypted";
    private static final String PREF_AI_AUTO_TAG = "pref_ai_auto_tag";
    private static final String PREF_AI_MODEL = "pref_ai_model";
    private static final String PREF_AUTO_TAG_LIMIT = "auto_tag_limit";
    
    // Default values
    private static final int DEFAULT_AUTO_TAG_LIMIT = 3;
    private static final boolean DEFAULT_AI_MODE = false;
    private static final String DEFAULT_AI_MODEL = "GPT_4_1_NANO";

    public TagSettingsManager(@NonNull Context ctx) {
        Context ctx1 = ctx.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(ctx1);
    }

    /**
     * Gets the OpenAI API key. Migrates from legacy plaintext if present.
     */
    public String getApiKey() {
        String enc = preferences.getString(PREF_API_KEY_ENCRYPTED, null);
        if (enc != null && !enc.trim().isEmpty()) {
            String dec = decrypt(enc);
            if (dec != null && !dec.trim().isEmpty()) return dec;
        }
        String legacy = preferences.getString(PREF_API_KEY, null);
        if (legacy != null && !legacy.trim().isEmpty()) {
            saveApiKey(legacy);
            preferences.edit().remove(PREF_API_KEY).apply();
            return legacy;
        }
        return null;
    }

    /** Saves the API key encrypted with AES/GCM in Android Keystore. */
    public void saveApiKey(@NonNull String apiKey) {
        String trimmed = apiKey.trim();
        if (trimmed.isEmpty()) {
            preferences.edit().remove(PREF_API_KEY_ENCRYPTED).apply();
            return;
        }
        String enc = encrypt(trimmed);
        if (enc != null) {
            preferences.edit().putString(PREF_API_KEY_ENCRYPTED, enc).apply();
        }
    }

    public boolean isAiMode() { return preferences.getBoolean(PREF_AI_AUTO_TAG, DEFAULT_AI_MODE); }
    public int getAutoTagLimit() { return preferences.getInt(PREF_AUTO_TAG_LIMIT, DEFAULT_AUTO_TAG_LIMIT); }
    public String getSelectedAiModelKey() { return preferences.getString(PREF_AI_MODEL, DEFAULT_AI_MODEL); }
    public boolean isAiTaggingConfigured() { return isAiMode() && hasValidApiKey(); }
    public boolean hasValidApiKey() { String k = getApiKey(); return k != null && !k.trim().isEmpty(); }

    // ===== Encryption helpers (Android Keystore AES/GCM) =====
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "quicknotes_api_key_aes";

    private SecretKey getOrCreateSecretKey() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null);
                return entry.getSecretKey();
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();
            keyGen.init(spec);
            return keyGen.generateKey();
        } catch (Exception e) {
            return null;
        }
    }

    private String encrypt(String plaintext) {
        try {
            SecretKey key = getOrCreateSecretKey();
            if (key == null) return null;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV(); // 12 bytes
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[12 + cipherBytes.length];
            System.arraycopy(iv, 0, out, 0, 12);
            System.arraycopy(cipherBytes, 0, out, 12, cipherBytes.length);
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    private String decrypt(String base64) {
        try {
            SecretKey key = getOrCreateSecretKey();
            if (key == null) return null;
            byte[] all = Base64.decode(base64, Base64.NO_WRAP);
            if (all.length < 13) return null;
            byte[] iv = new byte[12];
            System.arraycopy(all, 0, iv, 0, 12);
            byte[] cipherBytes = new byte[all.length - 12];
            System.arraycopy(all, 12, cipherBytes, 0, cipherBytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcm);
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
} 