package com.example.quicknotes.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.example.quicknotes.R;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ManageTags is responsible for managing tags associated with notes in the NoteLibrary.
 * It handles tag creation, color assignment, auto-tagging, and tag filtering.
 */
public class ManageTags {
    public Context ctx;
    private final List<ColorOption> availableColors;
    private final Map<String, Integer> colorMap;
    private final NoteLibrary noteLibrary;
    private final Random random = new Random();

    /**
     * Represents a color option for a tag.
     */
    public static class ColorOption {
        public final String name;
        @ColorRes
        public final int resId;

        public ColorOption(String name, int resId) {
            this.name = name;
            this.resId = resId;
        }
    }

    /**
     * Constructs a ManageTags instance for the given NoteLibrary.
     *
     * @param noteLibrary The NoteLibrary to manage tags for.
     */
    public ManageTags(@NonNull NoteLibrary noteLibrary) {
        this.noteLibrary = noteLibrary;
        this.ctx = noteLibrary.getContext();

        String[] names = ctx.getResources().getStringArray(R.array.tag_color_names);
        TypedArray ta = ctx.getResources().obtainTypedArray(R.array.tag_color_resources);
        try {
            List<ColorOption> opts = new ArrayList<>();
            for (int i = 0; i < ta.length() && i < names.length; i++) {
                int id = ta.getResourceId(i, 0);
                if (id != 0) {
                    opts.add(new ColorOption(names[i], id));
                }
            }
            this.availableColors = Collections.unmodifiableList(opts);
        } finally {
            ta.recycle();
        }

        colorMap = Persistence.loadTagMap(ctx);

        noteLibrary.getNotes().stream()
                .flatMap(n -> n.getTags().stream())
                .map(Tag::getName)
                .distinct()
                .forEach(this::getTagColorRes);

        Persistence.saveTagMap(ctx, colorMap);
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public String getApiKey() {
        return getPrefs().getString("user_api_key", null);
    }

    public boolean isAiMode() {
        return getPrefs().getBoolean("pref_ai_auto_tag", false);
    }

    public int getAutoTagLimit() {
        return getPrefs().getInt("auto_tag_limit", 5);
    }

    /**
     * Returns the list of available color options for tags.
     *
     * @return List of ColorOption
     */
    public List<ColorOption> getAvailableColors() {
        return availableColors;
    }

    /**
     * Gets the color resource ID for a given tag name, assigning a random color if not already assigned.
     *
     * @param tagName The name of the tag
     * @return The color resource ID
     */
    public int getTagColorRes(@NonNull String tagName) {
        String key = tagName.trim();
        Integer res = colorMap.get(key);
        if (res == null || res == 0) {
            res = availableColors.get(random.nextInt(availableColors.size())).resId;
            colorMap.put(key, res);
            Persistence.saveTagMap(ctx, colorMap);
        }
        return res;
    }

    /**
     * Sets the color resource ID for a given tag name.
     *
     * @param tagName The name of the tag
     * @param resId   The color resource ID
     */
    public void setTagColor(@NonNull String tagName, int resId) {
        if (tagName.trim().isEmpty()) return;
        colorMap.put(tagName.trim(), resId);
        Persistence.saveTagMap(ctx, colorMap);
    }

    /**
     * Sets a tag for the given note, creating the tag if necessary.
     *
     * @param note The note to tag
     * @param name The tag name
     */
    public void setTag(@NonNull Note note, @NonNull String name) {
        if (name.trim().isEmpty()) return;
        Tag tag = new Tag(name.trim(), getTagColorRes(name));
        note.setTag(tag);
        Persistence.saveNotes(ctx, noteLibrary.getNotes());
    }

    /**
     * Returns all tags used in the note library.
     *
     * @return Set of Tag objects
     */
    public Set<Tag> getAllTags() {
        Set<String> names = noteLibrary.getNotes().stream()
                .flatMap(n -> n.getTags().stream())
                .map(Tag::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return names.stream()
                .map(n -> new Tag(n, getTagColorRes(n)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Removes color assignments for tags that are no longer used in any note.
     */
    public void cleanupUnusedTags() {
        Set<String> used = noteLibrary.getNotes().stream()
                .flatMap(n -> n.getTags().stream())
                .map(Tag::getName)
                .collect(Collectors.toSet());
        if (colorMap.keySet().removeIf(k -> !used.contains(k))) {
            Persistence.saveTagMap(ctx, colorMap);
        }
    }

    /**
     * Assigns tags to a note based on keyword matching, up to the specified limit.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    public void simpleAutoTag(@NonNull Note note, int limit) {
        if (limit <= 0) return;
        String combined = (note.getTitle() + " " + note.getContent()).toLowerCase();
        Set<String> words = Arrays.stream(combined.split("\\W+"))
                .filter(w -> !w.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> dict = KeywordTagDictionary.loadTagMap(ctx);
        int count = 0;
        for (String word : words) {
            String tagName = dict.get(word);
            if (tagName != null) {
                boolean already = note.getTags().stream()
                        .anyMatch(t -> t.getName().equalsIgnoreCase(tagName));
                if (!already) {
                    setTag(note, tagName);
                    count++;
                }
                if (count >= limit) {
                    break;
                }
            }
        }
    }

    /**
     * Uses AI to suggest and assign tags to a note, up to the specified limit.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    public void aiAutoTag(@NonNull Note note, int limit) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Handler uiHandler = new Handler(Looper.getMainLooper());
        exec.execute(() -> {
            try {
                OpenAIClient client = OpenAIOkHttpClient.builder()
                        .apiKey(getApiKey())
                        .build();
                Set<String> existingTags = noteLibrary.getManageTags().getAllTags()
                        .stream()
                        .map(Tag::getName)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                String prompt =
                        "system: You are a tag suggestion assistant that outputs a comma-separated list of tag names.\n" +
                                "Use existing tags when appropriate. Do not explain or output anything other than the tag list.\n" +
                                "Existing tags: " + String.join(", ", existingTags) + "\n" +
                                "user: Extract up to " + limit + " tags from the following text:\n" +
                                "Title: " + note.getTitle() + "\n" +
                                "Content: " + note.getContent();
                ResponseCreateParams params = ResponseCreateParams.builder()
                        .input(prompt)
                        .model(ChatModel.GPT_4_1_NANO)
                        .build();
                Response response = client.responses().create(params);
                StringBuilder sb = new StringBuilder();
                for (ResponseOutputItem item : response.output()) {
                    if (!item.isMessage()) continue;
                    ResponseOutputMessage msg = item.asMessage();
                    for (ResponseOutputMessage.Content content : msg.content()) {
                        if (content.isOutputText()) {
                            sb.append(content.asOutputText().text());
                        }
                    }
                    break;
                }
                String tagCsv = sb.toString().trim();
                if (tagCsv.isEmpty()) return;
                String[] tagNames = tagCsv.split("\\s*,\\s*");
                for (String rawTag : tagNames) {
                    String tag = rawTag.trim();
                    if (tag.isEmpty()) continue;
                    uiHandler.post(() -> setTag(note, tag));
                }
            } catch (Exception e) {
                e.printStackTrace();
                uiHandler.post(() ->
                        Toast.makeText(ctx, "Auto‐tag error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            } finally {
                exec.shutdown();
            }
        });
    }

    /**
     * Filters notes by the given set of tag names.
     *
     * @param activeTagNames The set of tag names to filter by
     * @return List of notes containing at least one of the specified tags
     */
    public List<Note> filterNotesByTags(Set<String> activeTagNames) {
        if (activeTagNames == null || activeTagNames.isEmpty()) {
            return noteLibrary.getNotes();
        }
        List<Note> filtered = new ArrayList<>();
        for (Note note : noteLibrary.getNotes()) {
            for (Tag tag : note.getTags()) {
                if (activeTagNames.contains(tag.getName())) {
                    filtered.add(note);
                    break;
                }
            }
        }
        return filtered;
    }
}
