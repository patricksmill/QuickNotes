package io.github.patricksmill.quicknotes.model.tag

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseCreateParams
import io.github.patricksmill.quicknotes.model.note.Note
import java.util.concurrent.Executors

/**
 * AutoTaggingService handles automatic tag assignment for notes.
 * It supports both keyword-based and AI-powered tagging strategies.
 */
class AutoTaggingService(
    private val ctx: Context,
    private val tagRepository: TagRepository
) {
    /**
     * Functional interface for tag assignment callback.
     */
    interface TagAssignmentCallback {
        fun onTagAssigned(tagName: String)
    }

    /** Callback for AI tag suggestions (does not mutate the note).  */
    interface TagSuggestionsCallback {
        fun onSuggestions(suggestions: List<String>)
        fun onError(message: String) {}
    }

    /**
     * Assigns tags to a note based on keyword matching, up to the specified limit.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    fun performSimpleAutoTag(note: Note, limit: Int) {
        if (limit <= 0) return
        Log.d(TAG, "Starting simple auto-tag for note: ${note.title}, limit=$limit")
        val combined = extractTextContent(note)
        val words = extractWords(combined)
        val dictionary = loadKeywordTagMap(ctx)
        Log.d(TAG, "Simple auto-tag extracted words=${words.size}, dictionary size=${dictionary.size}")
        assignTagsFromDictionary(note, words, dictionary, limit)
    }

    /**
     * Uses AI to suggest and assign tags to a note, up to the specified limit.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     * @param apiKey The OpenAI API key
     * @param existingTags Set of existing tags to consider
     * @param callback Callback for individual tag assignments
     */
    fun performAiAutoTag(
        note: Note,
        limit: Int,
        apiKey: String,
        existingTags: Set<String>,
        callback: TagAssignmentCallback
    ) {
        val executor = Executors.newSingleThreadExecutor()
        val uiHandler = Handler(Looper.getMainLooper())

        executor.execute {
            try {
                val tagCsv = requestAiTags(note, limit, apiKey, existingTags)
                val tagNames = tagCsv.split("\\s*,\\s*".toRegex())
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
                
                if (tagNames.isNotEmpty()) {
                    Log.d(TAG, "AI suggested tags: $tagNames")
                    tagRepository.setTags(note, tagNames.map { it as String? }.toMutableList())
                    tagNames.forEach { tagName ->
                        uiHandler.post { callback.onTagAssigned(tagName) }
                    }
                    uiHandler.post {
                        Toast.makeText(
                            ctx,
                            "AI tagged: ${tagNames.joinToString(", ")}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                handleAiTaggingError(e, uiHandler)
            } finally {
                executor.shutdown()
            }
        }
    }

    /**
     * Produces AI tag suggestions without modifying the note.
     * Suggestions are delivered on the main thread.
     */
    fun performAiSuggest(
        note: Note,
        limit: Int,
        apiKey: String?,
        existingTags: Set<String>,
        callback: TagSuggestionsCallback
    ) {
        val executor = Executors.newSingleThreadExecutor()
        val uiHandler = Handler(Looper.getMainLooper())

        executor.execute {
            try {
                val tagCsv = requestAiTags(note, limit, apiKey, existingTags)
                val tagNames = tagCsv.split("\\s*,\\s*".toRegex())
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
                
                uiHandler.post { callback.onSuggestions(tagNames) }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown AI error"
                uiHandler.post { callback.onError(msg) }
            } finally {
                executor.shutdown()
            }
        }
    }

    /**
     * Extracts text content from note title and content.
     *
     * @param note The note to extract text from
     * @return Combined text content in lowercase
     */
    private fun extractTextContent(note: Note): String {
        return "${note.title} ${note.content}".lowercase()
    }

    /**
     * Extracts individual words from text content.
     *
     * @param text The text to extract words from
     * @return Set of unique words
     */
    private fun extractWords(text: String): Set<String> {
        return text.split("\\W+".toRegex())
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * Assigns tags based on keyword dictionary matching.
     *
     * @param note The note to tag
     * @param words Set of words from the note
     * @param dictionary Keyword to tag mapping
     * @param limit Maximum number of tags to assign
     */
    private fun assignTagsFromDictionary(
        note: Note,
        words: Set<String>,
        dictionary: Map<String, String>?,
        limit: Int
    ) {
        if (dictionary == null) return
        
        var count = 0
        val toAssign = mutableListOf<String>()
        
        for (word in words) {
            val tagName = dictionary[word]
            if (tagName != null && !hasTag(note, tagName)) {
                toAssign.add(tagName)
                count++
                if (count >= limit) {
                    break
                }
            }
        }
        
        if (toAssign.isNotEmpty()) {
            Log.d(TAG, "Simple auto-tag will assign: $toAssign")
            tagRepository.setTags(note, toAssign.map { it as String? }.toMutableList())
            Toast.makeText(
                ctx,
                "Auto-tagged ${note.title}: ${toAssign.joinToString(", ")}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Checks if a note already has a specific tag.
     *
     * @param note The note to check
     * @param tagName The tag name to look for
     * @return true if the note has the tag, false otherwise
     */
    private fun hasTag(note: Note, tagName: String): Boolean {
        return note.tags.any { it.name.equals(tagName, ignoreCase = true) }
    }

    /**
     * Requests AI-generated tags from OpenAI API.
     *
     * @param note The note to analyze
     * @param limit Maximum number of tags to request
     * @param apiKey OpenAI API key
     * @param existingTags Existing tags to consider
     * @return Comma-separated list of suggested tags
     */
    private fun requestAiTags(
        note: Note,
        limit: Int,
        apiKey: String?,
        existingTags: Set<String>
    ): String {
        val client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey.toString())
            .build()

        val prompt = buildAiPrompt(note, limit, existingTags)

        // Resolve model dynamically: if settings uses "auto", fetch first chat model from OpenAI; otherwise honor selected key
        val settings = TagSettingsManager(ctx)
        val key = settings.selectedAiModelKey

        val modelId = if (key.equals("auto", ignoreCase = true)) {
            try {
                // Fetch models and pick a viable chat model id
                val ids = fetchChatModelIds(client)
                if (ids.isEmpty()) "gpt-4o-mini" else ids[0]
            } catch (_: Exception) {
                "gpt-4o-mini"
            }
        } else {
            key
        }

        val params = ResponseCreateParams.builder()
            .input(prompt)
            .model(modelId)
            .build()

        val response = client.responses().create(params)
        return extractResponseText(response)
    }

    // Fetch list of available chat-capable model ids from OpenAI client
    private fun fetchChatModelIds(client: OpenAIClient): List<String> {
        val ids = mutableListOf<String>()
        try {
            // New OpenAI SDK: list models via client.models().list()
            val list = client.models().list()
            for (m in list.data()) {
                val id = m.id()
                if (id.startsWith("gpt-") || id.contains("chat")) {
                    ids.add(id)
                }
            }
        } catch (_: Exception) {
            // Ignore errors ;)
        }
        return ids
    }

    /**
     * Builds the prompt for AI tag generation.
     *
     * @param note The note to analyze
     * @param limit Maximum number of tags
     * @param existingTags Existing tags to consider
     * @return Formatted prompt string
     */
    private fun buildAiPrompt(
        note: Note,
        limit: Int,
        existingTags: Set<String>
    ): String {
        return buildString {
            appendLine("system: You are a tag suggestion assistant that outputs a comma-separated list of tag names.")
            appendLine("Use existing tags when appropriate. Do not explain or output anything other than the tag list.")
            appendLine("Existing tags: ${existingTags.joinToString(", ")}")
            appendLine("user: Extract up to $limit tags from the following text:")
            appendLine("Title: ${note.title}")
            appendLine("Content: ${note.content}")
        }
    }

    /**
     * Extracts text from OpenAI API response.
     *
     * @param response The API response
     * @return Extracted text content
     */
    private fun extractResponseText(response: Response): String {
        val sb = StringBuilder()
        for (item in response.output()) {
            if (!item.isMessage()) continue

            val msg = item.asMessage()
            for (content in msg.content()) {
                if (content.isOutputText()) {
                    sb.append(content.asOutputText().text())
                }
            }
            break
        }
        return sb.toString().trim()
    }

    /**
     * Handles errors during AI tagging.
     *
     * @param e The exception that occurred
     * @param uiHandler Handler for UI thread operations
     */
    private fun handleAiTaggingError(e: Exception, uiHandler: Handler) {
        e.printStackTrace()
        Log.e(TAG, "AI tagging error", e)
        uiHandler.post {
            Toast.makeText(
                ctx, "Auto-tag error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val TAG = "AutoTagging"
    }

    // Loads the keyword->tag map from assets/keyword_tags.json
    private fun loadKeywordTagMap(context: Context): Map<String, String> {
        return try {
            context.assets.open("keyword_tags.json").use { inputStream ->
                java.io.InputStreamReader(inputStream).use { reader ->
                    val gson = com.google.gson.Gson()
                    val listType = object : com.google.gson.reflect.TypeToken<List<KeywordTag>>() {}.type
                    val list: List<KeywordTag> = gson.fromJson(reader, listType)

                    list.mapNotNull { kt ->
                        if (kt.keyword != null && kt.tag != null) {
                            kt.keyword.lowercase() to kt.tag
                        } else null
                    }.toMap()
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private data class KeywordTag(
        val keyword: String? = null,
        val tag: String? = null
    )
}