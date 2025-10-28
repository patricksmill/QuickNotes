package com.example.quicknotes.model;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import com.example.quicknotes.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * TagColorManager handles color assignment and management for tags.
 * It manages available colors and assigns colors to tag names persistently.
 */
public class TagColorManager {
    private final Context ctx;
    private final List<ColorOption> availableColors;
    private final Map<String, Integer> colorMap;
    private final Random random = new Random();

    /**
         * Represents a color option for a tag.
         */
        public record ColorOption(String name, @ColorRes int resId) {
            public ColorOption(String name, int resId) {
                this.name = name;
                this.resId = resId;
            }
        }

    /**
     * Constructs a TagColorManager instance.
     *
     * @param ctx The application context
     */
    public TagColorManager(@NonNull Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.availableColors = loadAvailableColors();
        this.colorMap = Persistence.loadTagMap(ctx);
    }

    /**
     * Loads available color options from resources.
     *
     * @return List of available color options
     */
    private List<ColorOption> loadAvailableColors() {
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
            return Collections.unmodifiableList(opts);
        } finally {
            ta.recycle();
        }
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
            res = assignRandomColor();
            colorMap.put(key, res);
            saveColorMap();
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
        saveColorMap();
    }

    /**
     * Removes color assignments for tags that are no longer used.
     *
     * @param usedTagNames Set of tag names currently in use
     */
    public void cleanupUnusedColors(@NonNull java.util.Set<String> usedTagNames) {
        if (colorMap.keySet().removeIf(k -> !usedTagNames.contains(k))) {
            saveColorMap();
        }
    }

    /**
     * Assigns a random color from available colors.
     *
     * @return Random color resource ID
     */
    private int assignRandomColor() {
        return availableColors.get(random.nextInt(availableColors.size())).resId;
    }

    /**
     * Persists the color map to storage.
     */
    private void saveColorMap() {
        Persistence.saveTagMap(ctx, colorMap);
    }
} 