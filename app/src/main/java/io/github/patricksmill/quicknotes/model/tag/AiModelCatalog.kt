package io.github.patricksmill.quicknotes.model.tag

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.JsonParser
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Curates model suggestions for the selected AI provider.
 *
 * The goal is to always show a sensible local shortlist and optionally enrich it with live API
 * results when the user chooses to refresh.
 */
class AiModelCatalog(context: Context) {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    data class ModelOption(
        val id: String,
        val label: String
    )

    fun curatedSuggestions(provider: TagSettingsManager.AiProvider): List<ModelOption> {
        return when (provider) {
            TagSettingsManager.AiProvider.OPENAI -> listOf(
                ModelOption("gpt-5-mini", "GPT-5 mini"),
                ModelOption("gpt-5.2", "GPT-5.2"),
                ModelOption("gpt-4.1", "GPT-4.1"),
                ModelOption("gpt-4o-mini", "GPT-4o mini")
            )

            TagSettingsManager.AiProvider.ANTHROPIC -> listOf(
                ModelOption("claude-sonnet-4-20250514", "Claude Sonnet 4"),
                ModelOption("claude-opus-4-1-20250805", "Claude Opus 4.1"),
                ModelOption("claude-3-7-sonnet-20250219", "Claude Sonnet 3.7"),
                ModelOption("claude-3-5-haiku-20241022", "Claude Haiku 3.5")
            )

            TagSettingsManager.AiProvider.GOOGLE -> listOf(
                ModelOption("gemini-2.5-flash", "Gemini 2.5 Flash"),
                ModelOption("gemini-2.5-pro", "Gemini 2.5 Pro"),
                ModelOption("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite"),
                ModelOption("gemini-2.0-flash-lite", "Gemini 2.0 Flash-Lite")
            )

            TagSettingsManager.AiProvider.CUSTOM -> emptyList()
        }
    }

    fun cachedSuggestions(provider: TagSettingsManager.AiProvider): List<ModelOption> {
        return readCachedSuggestions(provider)
    }

    fun saveCachedSuggestions(
        provider: TagSettingsManager.AiProvider,
        suggestions: List<ModelOption>
    ) {
        val normalized = normalizeSuggestions(suggestions)
        if (normalized.isEmpty()) {
            preferences.edit { remove(cacheKey(provider)) }
            return
        }
        preferences.edit { putString(cacheKey(provider), buildJson(normalized)) }
    }

    fun fetchSuggestions(
        provider: TagSettingsManager.AiProvider,
        endpoint: String,
        apiKey: String?
    ): List<ModelOption> {
        val effectiveKey = apiKey?.trim().orEmpty()
        val response = when (provider) {
            TagSettingsManager.AiProvider.OPENAI,
            TagSettingsManager.AiProvider.CUSTOM -> {
                require(effectiveKey.isNotBlank()) { "Missing API key for ${provider.displayName}" }
                getJson(
                    url = joinUrl(endpoint, "models"),
                    headers = mapOf("Authorization" to "Bearer $effectiveKey")
                )
            }

            TagSettingsManager.AiProvider.ANTHROPIC -> {
                require(effectiveKey.isNotBlank()) { "Missing API key for ${provider.displayName}" }
                getJson(
                    url = joinUrl(endpoint, "models"),
                    headers = mapOf(
                        "x-api-key" to effectiveKey,
                        "anthropic-version" to "2023-06-01"
                    )
                )
            }

            TagSettingsManager.AiProvider.GOOGLE -> {
                require(effectiveKey.isNotBlank()) { "Missing API key for ${provider.displayName}" }
                val url = Uri.parse(GOOGLE_MODELS_ENDPOINT)
                    .buildUpon()
                    .appendQueryParameter("key", effectiveKey)
                    .build()
                    .toString()
                getJson(url = url, headers = emptyMap())
            }
        }

        return when (provider) {
            TagSettingsManager.AiProvider.OPENAI,
            TagSettingsManager.AiProvider.CUSTOM -> parseOpenAiModels(response)

            TagSettingsManager.AiProvider.ANTHROPIC -> parseAnthropicModels(response)
            TagSettingsManager.AiProvider.GOOGLE -> parseGoogleModels(response)
        }
    }

    fun mergedSuggestions(
        provider: TagSettingsManager.AiProvider,
        currentModel: String,
        liveSuggestions: List<ModelOption>? = null
    ): List<ModelOption> {
        val ordered = linkedMapOf<String, ModelOption>()

        fun addAll(options: List<ModelOption>) {
            options.forEach { option ->
                val id = option.id.trim()
                if (id.isBlank()) return@forEach
                ordered.putIfAbsent(
                    id.lowercase(Locale.US),
                    ModelOption(id = id, label = option.label.trim().ifBlank { id })
                )
            }
        }

        addAll(curatedSuggestions(provider))
        addAll(if (liveSuggestions.isNullOrEmpty()) cachedSuggestions(provider) else liveSuggestions)

        val current = currentModel.trim()
        if (current.isNotBlank()) {
            ordered.putIfAbsent(
                current.lowercase(Locale.US),
                ModelOption(id = current, label = "Current selection: $current")
            )
        }

        val defaultModel = provider.defaultModel.trim()
        if (defaultModel.isNotBlank()) {
            ordered.putIfAbsent(
                defaultModel.lowercase(Locale.US),
                ModelOption(id = defaultModel, label = "Suggested default: $defaultModel")
            )
        }

        return ordered.values.toList()
    }

    private fun normalizeSuggestions(suggestions: List<ModelOption>): List<ModelOption> {
        return suggestions
            .mapNotNull { option ->
                val id = option.id.trim()
                if (id.isBlank()) return@mapNotNull null
                ModelOption(id = id, label = option.label.trim().ifBlank { id })
            }
            .distinctBy { it.id.lowercase(Locale.US) }
            .sortedBy { it.label.lowercase(Locale.US) }
    }

    private fun buildJson(suggestions: List<ModelOption>): String {
        return suggestions.joinToString(prefix = "[", postfix = "]") { option ->
            """{"id":"${escapeJson(option.id)}","label":"${escapeJson(option.label)}"}"""
        }
    }

    private fun escapeJson(value: String): String {
        return buildString(value.length) {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }

    private fun readCachedSuggestions(provider: TagSettingsManager.AiProvider): List<ModelOption> {
        val raw = preferences.getString(cacheKey(provider), null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return try {
            val parsed = JsonParser.parseString(raw).asJsonArray
            parsed.mapNotNull { element ->
                val obj = element.asJsonObject
                val id = obj.get("id")?.asString.orEmpty().trim()
                if (id.isBlank()) return@mapNotNull null
                val label = obj.get("label")?.asString.orEmpty().trim().ifBlank { id }
                ModelOption(id = id, label = label)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseOpenAiModels(response: com.google.gson.JsonObject): List<ModelOption> {
        val data = response.getAsJsonArray("data") ?: return emptyList()
        return data.mapNotNull { element ->
            val obj = element.asJsonObject
            val id = obj.get("id")?.asString.orEmpty().trim()
            if (id.isBlank()) null else ModelOption(id = id, label = prettifyModelLabel(id))
        }
    }

    private fun parseAnthropicModels(response: com.google.gson.JsonObject): List<ModelOption> {
        val data = response.getAsJsonArray("data") ?: return emptyList()
        return data.mapNotNull { element ->
            val obj = element.asJsonObject
            val id = obj.get("id")?.asString.orEmpty().trim()
            if (id.isBlank()) return@mapNotNull null
            val label = obj.get("display_name")?.asString.orEmpty().trim().ifBlank { prettifyModelLabel(id) }
            ModelOption(id = id, label = label)
        }
    }

    private fun parseGoogleModels(response: com.google.gson.JsonObject): List<ModelOption> {
        val models = response.getAsJsonArray("models") ?: return emptyList()
        return models.mapNotNull { element ->
            val obj = element.asJsonObject
            val name = obj.get("name")?.asString.orEmpty().trim()
            val id = name.substringAfterLast('/').ifBlank { obj.get("id")?.asString.orEmpty().trim() }
            if (id.isBlank() || !id.startsWith("gemini-", ignoreCase = true)) return@mapNotNull null
            val label = obj.get("displayName")?.asString.orEmpty().trim().ifBlank { prettifyModelLabel(id) }
            ModelOption(id = id, label = label)
        }
    }

    private fun prettifyModelLabel(id: String): String {
        return when {
            id.startsWith("gpt-", ignoreCase = true) ->
                id.replace("gpt-", "GPT-").replace("-mini", " mini").replace("-nano", " nano")

            id.startsWith("claude-", ignoreCase = true) ->
                id.replace("claude-", "Claude ").replace("-", " ")

            id.startsWith("gemini-", ignoreCase = true) ->
                id.replace("gemini-", "Gemini ").replace("-", " ")

            else -> id
        }
    }

    private fun getJson(url: String, headers: Map<String, String>): com.google.gson.JsonObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 30_000
            doInput = true
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        try {
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code ${connection.responseMessage}: $body")
            }
            return JsonParser.parseString(body).asJsonObject
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        if (stream == null) return ""
        return InputStreamReader(stream, StandardCharsets.UTF_8).use { it.readText() }
    }

    private fun joinUrl(baseUrl: String, path: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        val suffix = path.trimStart('/')
        return "$trimmed/$suffix"
    }

    companion object {
        fun cacheKey(provider: TagSettingsManager.AiProvider): String {
            return when (provider) {
                TagSettingsManager.AiProvider.OPENAI -> OPENAI_CACHE_KEY
                TagSettingsManager.AiProvider.ANTHROPIC -> ANTHROPIC_CACHE_KEY
                TagSettingsManager.AiProvider.GOOGLE -> GOOGLE_CACHE_KEY
                TagSettingsManager.AiProvider.CUSTOM -> CUSTOM_CACHE_KEY
            }
        }

        private const val GOOGLE_MODELS_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models"

        private const val OPENAI_CACHE_KEY = "ai_model_suggestions_openai"
        private const val ANTHROPIC_CACHE_KEY = "ai_model_suggestions_anthropic"
        private const val GOOGLE_CACHE_KEY = "ai_model_suggestions_google"
        private const val CUSTOM_CACHE_KEY = "ai_model_suggestions_custom"
    }
}
