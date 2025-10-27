package com.example.quicknotes.model

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStreamReader

object KeywordTagDictionary {
    private const val TAG = "KeywordTagDictionary"
    private const val FILE_NAME = "keyword_tags.json"
    
    // Create Gson instance once instead of each call
    private val gson = Gson()

    /**
     * Loads the tag map from a JSON file.
     * @param ctx the context of the application
     * @return a map of keywords to tags, or an empty map if the file does not exist
     */
    fun loadTagMap(ctx: Context): Map<String, String> {
        return try {
            ctx.assets.open(FILE_NAME).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val listType = object : TypeToken<List<KeywordTag>>() {}.type
                    val list: List<KeywordTag> = gson.fromJson(reader, listType)

                    list.mapNotNull { kt ->
                        if (kt.keyword != null && kt.tag != null) {
                            kt.keyword.lowercase() to kt.tag
                        } else null
                    }.toMap()
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Could not read $FILE_NAME from assets", e)
            emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $FILE_NAME", e)
            emptyMap()
        }
    }

    private data class KeywordTag(
        val keyword: String? = null,
        val tag: String? = null
    )
}