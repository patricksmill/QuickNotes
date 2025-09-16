package com.example.quicknotes.model;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

/**
 * TagSettingsManager handles tag-related preferences and configuration settings.
 * It provides access to user preferences for AI tagging, API keys, and tagging limits.
 */
public class TagSettingsManager {
    private final SharedPreferences preferences;
    private SharedPreferences securePreferences;
    private static final String PREF_API_KEY = "openai_api_key";
    private static final String PREF_AI_AUTO_TAG = "pref_ai_auto_tag";
    private static final String PREF_AUTO_TAG_LIMIT = "auto_tag_limit";
    
    // Default values
    private static final int DEFAULT_AUTO_TAG_LIMIT = 3;
    private static final boolean DEFAULT_AI_MODE = false;

    /**
     * Constructs a TagSettingsManager instance.
     *
     * @param ctx The application context
     */
    public TagSettingsManager(@NonNull Context ctx) {
        Context ctx1 = ctx.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(ctx1);
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            this.securePreferences = EncryptedSharedPreferences.create(
                    "quicknotes_secure_prefs",
                    masterKeyAlias,
                    ctx1,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            this.securePreferences = this.preferences; // fallback
        }
    }

    /**
     * Gets the OpenAI API key from user preferences.
     *
     * @return The API key, or null if not set
     */
    public String getApiKey() {
        return securePreferences.getString(PREF_API_KEY, null);
    }



    /**
     * Checks if AI-powered auto-tagging is enabled.
     *
     * @return true if AI mode is enabled, false for keyword-based tagging
     */
    public boolean isAiMode() {
        return preferences.getBoolean(PREF_AI_AUTO_TAG, DEFAULT_AI_MODE);
    }


    /**
     * Gets the maximum number of tags to assign during auto-tagging.
     *
     * @return The auto-tag limit
     */
    public int getAutoTagLimit() {
        return preferences.getInt(PREF_AUTO_TAG_LIMIT, DEFAULT_AUTO_TAG_LIMIT);
    }


    /**
     * Checks if AI tagging is properly configured (has API key and is enabled).
     *
     * @return true if AI tagging can be used
     */
    public boolean isAiTaggingConfigured() {
        return isAiMode() && hasValidApiKey();
    }

    /**
     * Checks if a valid API key is configured.
     *
     * @return true if API key is set and non-empty
     */
    public boolean hasValidApiKey() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

} 