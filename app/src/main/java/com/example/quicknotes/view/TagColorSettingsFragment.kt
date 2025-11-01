package com.example.quicknotes.view

// Removed NoteViewModel dependency; will access TagManager via ControllerActivity
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.quicknotes.model.tag.Tag
import com.example.quicknotes.model.tag.TagRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TagColorSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var colorResIds: IntArray
    private var colorNames: Array<String>? = null
    private var colorOptions: MutableList<TagRepository.ColorOption?>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val ctx = requireContext()
        val tagManager = (activity as? com.example.quicknotes.controller.ControllerActivity)?.onManageTags()
        colorOptions = tagManager?.availableColors
        val allTags = tagManager?.allTags ?: mutableSetOf()

        colorResIds = colorOptions?.mapNotNull { it?.resId }?.toIntArray() ?: intArrayOf()
        colorNames = colorOptions?.mapNotNull { it?.name }?.toTypedArray()

        val screen = preferenceManager.createPreferenceScreen(ctx)
        preferenceScreen = screen

        val sortedTags = allTags.sortedBy { it.name }
        for (tag in sortedTags) {
            screen.addPreference(createTagPreference(ctx, tag))
        }
    }

    private fun createTagPreference(ctx: Context, tag: Tag): Preference {
        val pref = Preference(ctx)
        pref.key = "tag_color_${tag.name}"
        pref.title = tag.name
        pref.summary = "Tap to change color"

        val color = ContextCompat.getColor(ctx, tag.colorResId)
        pref.icon = color.toDrawable()

        pref.setOnPreferenceClickListener {
            showColorPicker(tag.name, pref)
            true
        }
        return pref
    }

    private fun showColorPicker(tagName: String, pref: Preference) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select color for '$tagName'")
            .setItems(colorNames) { _, which ->
                val chosen = colorResIds[which]
                (activity as? com.example.quicknotes.controller.ControllerActivity)?.onManageTags()?.setTagColor(tagName, chosen)

                val color = ContextCompat.getColor(requireContext(), chosen)
                pref.icon = color.toDrawable()
            }
            .show()
    }
}