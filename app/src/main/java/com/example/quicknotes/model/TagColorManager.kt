package com.example.quicknotes.model

import android.content.Context
import androidx.annotation.ColorRes
import com.example.quicknotes.R
import java.util.Collections
import java.util.Random

/**
 * TagColorManager handles color assignment and management for tags.
 * It manages available colors and assigns colors to tag names persistently.
 */
class TagColorManager(ctx: Context) {
    private val ctx: Context = ctx.applicationContext

    /**
     * Returns the list of available color options for tags.
     *
     * @return List of ColorOption
     */
    @JvmField
    val availableColors: MutableList<ColorOption?>
    private val colorMap: MutableMap<String?, Int?>
    private val random = Random()

    /**
     * Represents a color option for a tag.
     */
    @JvmRecord
    data class ColorOption(@JvmField val name: String, @JvmField @field:ColorRes val resId: Int)

    /**
     * Constructs a TagColorManager instance.
     *
     * @param ctx The application context
     */
    init {
        this.availableColors = loadAvailableColors()
        this.colorMap = Persistence.loadTagMap(ctx)
    }

    /**
     * Loads available color options from resources.
     *
     * @return List of available color options
     */
    private fun loadAvailableColors(): MutableList<ColorOption?> {
        val names = ctx.resources.getStringArray(R.array.tag_color_names)
        val ta = ctx.resources.obtainTypedArray(R.array.tag_color_resources)
        try {
            val opts: MutableList<ColorOption?> = ArrayList()
            var i = 0
            while (i < ta.length() && i < names.size) {
                val id = ta.getResourceId(i, 0)
                if (id != 0) {
                    opts.add(ColorOption(names[i], id))
                }
                i++
            }
            return Collections.unmodifiableList<ColorOption?>(opts)
        } finally {
            ta.recycle()
        }
    }

    /**
     * Gets the color resource ID for a given tag name, assigning a random color if not already assigned.
     *
     * @param tagName The name of the tag
     * @return The color resource ID
     */
    fun getTagColorRes(tagName: String): Int {
        val key = tagName.trim { it <= ' ' }
        var res = colorMap[key]
        if (res == null || res == 0) {
            res = assignRandomColor()
            colorMap.put(key, res)
            saveColorMap()
        }
        return res
    }

    /**
     * Sets the color resource ID for a given tag name.
     *
     * @param tagName The name of the tag
     * @param resId   The color resource ID
     */
    fun setTagColor(tagName: String, resId: Int) {
        if (tagName.trim { it <= ' ' }.isEmpty()) return
        colorMap.put(tagName.trim { it <= ' ' }, resId)
        saveColorMap()
    }

    /**
     * Removes color assignments for tags that are no longer used.
     *
     * @param usedTagNames Set of tag names currently in use
     */
    fun cleanupUnusedColors(usedTagNames: MutableSet<String?>) {
        if (colorMap.keys.removeIf { k: String? -> !usedTagNames.contains(k) }) {
            saveColorMap()
        }
    }

    /**
     * Assigns a random color from available colors.
     *
     * @return Random color resource ID
     */
    private fun assignRandomColor(): Int {
        return availableColors[random.nextInt(availableColors.size)]!!.resId
    }

    /**
     * Persists the color map to storage.
     */
    private fun saveColorMap() {
        Persistence.saveTagMap(ctx, colorMap)
    }
}