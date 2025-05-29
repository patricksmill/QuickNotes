package com.example.quicknotes.model;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence provides methods for saving and loading notes and tag data
 * to and from JSON files in storage.
 */
public final class Persistence {
    private static final String NOTES_FILE_NAME = "notes.json";

    private static final String TAGS_FILE_NAME = "tag_lookup.json";

    /**
     * Loads notes from a JSON file into Java objects. If the file does not exist or an error occurs,
     * returns an empty list.
     *
     * @param ctx the application context (must not be null)
     * @return a list of notes previously saved, or an empty list if none
     */
    @NonNull
    public static List<Note> loadNotes(@NonNull Context ctx) {
        File file = new File(ctx.getFilesDir(), NOTES_FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<Note>>() {}.getType();
            List<Note> notes = new Gson().fromJson(reader, listType);
            return notes != null ? notes : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Saves a list of notes to a JSON file.
     *
     * @param ctx   the application context (must not be null)
     * @param notes the list of notes to save
     */
    public static void saveNotes(@NonNull Context ctx, @NonNull List<Note> notes) {
        File file = new File(ctx.getFilesDir(), NOTES_FILE_NAME);
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(notes, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the map of tags used for simple auto-tagging. If the file does not exist or an error occurs,
     * returns an empty map.
     *
     * @param ctx the application context (must not be null)
     * @return a map of tag names to color resource IDs
     */
    @NonNull
    public static Map<String, Integer> loadTagMap(@NonNull Context ctx) {
        File file = new File(ctx.getFilesDir(), TAGS_FILE_NAME);
        if (!file.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> map = new Gson().fromJson(reader, type);
            return map != null ? map : new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Saves the tag map to a JSON file.
     *
     * @param ctx the application context (must not be null)
     * @param map the tag map to save
     */
    public static void saveTagMap(@NonNull Context ctx, @NonNull Map<String, Integer> map) {
        File file = new File(ctx.getFilesDir(), TAGS_FILE_NAME);
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
