package io.github.patricksmill.quicknotes.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.patricksmill.quicknotes.model.note.Note
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * Persistence provides methods for saving and loading notes and tag data
 * to and from JSON files in storage.
 */
object Persistence {
    private const val NOTES_FILE_NAME = "notes.json"

    private const val TAGS_FILE_NAME = "tag_lookup.json"

    /**
     * Loads notes from a JSON file into Java objects. If the file does not exist or an error occurs,
     * returns an empty list.
     *
     * @param ctx the application context (must not be null)
     * @return a list of notes previously saved, or an empty list if none
     */
    fun loadNotes(ctx: Context): MutableList<Note> {
        val file = File(ctx.filesDir, NOTES_FILE_NAME)
        if (!file.exists()) return mutableListOf()
        try {
            FileReader(file).use { reader ->
                val listType = object : TypeToken<MutableList<Note>>() {}.type
                val notes = Gson().fromJson<MutableList<Note>>(reader, listType)
                return notes ?: mutableListOf()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return mutableListOf()
        }
    }

    /**
     * Saves a list of notes to a JSON file.
     *
     * @param ctx   the application context (must not be null)
     * @param notes the list of notes to save
     */
    fun saveNotes(ctx: Context, notes: List<Note>) {
        val file = File(ctx.filesDir, NOTES_FILE_NAME)
        try {
            FileWriter(file).use { writer ->
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.toJson(notes, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the map of tags used for simple auto-tagging. If the file does not exist or an error occurs,
     * returns an empty map.
     *
     * @param ctx the application context (must not be null)
     * @return a map of tag names to color resource IDs
     */
    fun loadTagMap(ctx: Context): MutableMap<String?, Int?> {
        val file = File(ctx.filesDir, TAGS_FILE_NAME)
        if (!file.exists()) return HashMap()
        try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableMap<String?, Int?>?>() {}.type
                val map = Gson().fromJson<MutableMap<String?, Int?>?>(reader, type)
                return map ?: HashMap()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return HashMap()
        }
    }

    /**
     * Saves the tag map to a JSON file.
     *
     * @param ctx the application context (must not be null)
     * @param map the tag map to save
     */
    fun saveTagMap(ctx: Context, map: MutableMap<String?, Int?>) {
        val file = File(ctx.filesDir, TAGS_FILE_NAME)
        try {
            FileWriter(file).use { writer ->
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.toJson(map, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
