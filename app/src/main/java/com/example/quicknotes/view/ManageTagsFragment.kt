package com.example.quicknotes.view

import android.R
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.quicknotes.controller.ControllerActivity
import com.example.quicknotes.model.Tag
import com.example.quicknotes.model.TagColorManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * ManageTagsFragment provides a dedicated screen to view and manage all tags:
 * rename, delete, merge, and change colors.
 */
class ManageTagsFragment : PreferenceFragmentCompat(), NotesUI {
    private var listener: NotesUI.Listener? = null
    private lateinit var colorResIds: IntArray
    private var colorNames: Array<String>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is ControllerActivity) {
            setListener(activity as NotesUI.Listener?)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val ctx = requireContext()
        if (activity is ControllerActivity) {
            setListener(activity as NotesUI.Listener?)
        }

        val colorOptions =
            if (listener != null) listener!!.onGetAvailableColors() else TagColorManager(ctx).availableColors
        val allTags = if (listener != null) listener!!.onGetAllTags() else mutableSetOf()

        // Use mapNotNull to safely handle nullable elements and create non-null collections.
        colorResIds = colorOptions?.mapNotNull { it?.resId }?.toIntArray() ?: intArrayOf()
        colorNames = colorOptions?.mapNotNull { it?.name }?.toTypedArray()

        val screen = preferenceManager.createPreferenceScreen(ctx)
        preferenceScreen = screen

        // Filter out null tags before processing.
        val validTags = allTags.toList()
        if (validTags.isEmpty()) {
            val noTagsPref = Preference(ctx)
            noTagsPref.title = "No tags available"
            noTagsPref.summary = "Create tags by editing notes"
            noTagsPref.isSelectable = false
            screen.addPreference(noTagsPref)
        } else {
            // Sort non-null tags alphabetically.
            val tagList: List<Tag> = validTags.sortedBy { it.name }
            for (tag in tagList) {
                screen.addPreference(createTagPreference(ctx, tag))
            }
        }

        val mergePref = Preference(ctx)
        mergePref.title = "Merge Tags"
        mergePref.summary = "Combine multiple tags into one"
        mergePref.setOnPreferenceClickListener {
            showMergeDialog()
            true
        }
        screen.addPreference(mergePref)
    }

    private fun createTagPreference(ctx: Context, tag: Tag): Preference {
        val pref = Preference(ctx)
        val tagName = tag.name
        pref.key = "manage_tag_$tagName"
        pref.title = tagName
        val currentColorRes = tag.colorResId
        val currentColor = ContextCompat.getColor(ctx, currentColorRes)
        pref.icon = currentColor.toDrawable()
        pref.isIconSpaceReserved = true
        pref.summary = "Tap to rename, delete, or change color"
        pref.setOnPreferenceClickListener {
            showTagOptionsDialog(tagName, pref)
            true
        }
        return pref
    }

    private fun showTagOptionsDialog(tagName: String, pref: Preference) {
        val options = arrayOf("Rename", "Delete", "Change Color")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage '$tagName'")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(tagName)
                    1 -> showDeleteConfirm(tagName)
                    2 -> showColorPicker(tagName, pref)
                }
            }
            .show()
    }

    private fun showRenameDialog(oldName: String) {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(oldName)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Tag")
            .setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val newName = input.text?.toString()?.trim() ?: ""
                if (newName.isNotEmpty() && listener != null) {
                    listener!!.onRenameTag(oldName, newName)
                    refreshScreen()
                }
            }
            .show()
    }

    private fun showDeleteConfirm(tagName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Tag")
            .setMessage("Remove tag '$tagName' from all notes?")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (listener != null) {
                    listener!!.onDeleteTag(tagName)
                    refreshScreen()
                }
            }
            .show()
    }

    private fun showColorPicker(tagName: String, pref: Preference) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select color for '$tagName'")
            .setItems(colorNames) { _, which ->
                val chosen = colorResIds[which]
                if (listener != null) {
                    listener!!.onSetTagColor(tagName, chosen)
                    val color = ContextCompat.getColor(requireContext(), chosen)
                    pref.icon = color.toDrawable()
                }
            }
            .show()
    }

    private fun showMergeDialog() {
        if (listener == null) return
        val allTags = listener!!.onGetAllTags()
        val validTags = allTags.toList()
        if (validTags.isEmpty()) return

        val names: List<String> = validTags.map { it.name }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        val items = names.toTypedArray()
        val checked = BooleanArray(items.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select tags to merge")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Next") { _, _ ->
                val selected = items.filterIndexed { index, _ -> checked[index] }
                if (selected.isEmpty()) return@setPositiveButton
                showMergeTargetDialog(selected)
            }
            .show()
    }

    private fun showMergeTargetDialog(sources: List<String>) {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Merge into tag")
            .setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Merge") { _, _ ->
                val target = input.text?.toString()?.trim() ?: ""
                if (target.isEmpty() || listener == null) return@setPositiveButton
                // De-duplicate sources and remove target if present
                val unique = sources.toMutableSet()
                unique.removeIf { s -> s.equals(target, ignoreCase = true) }
                if (unique.isEmpty()) return@setPositiveButton
                listener!!.onMergeTags(ArrayList(unique), target)
                refreshScreen()
            }
            .show()
    }

    private fun refreshScreen() {
        // Rebuild preferences to reflect latest tag set/colors
        onCreatePreferences(null, null)
    }

    private fun setListener(listener: NotesUI.Listener?) {
        this.listener = listener
    }
}