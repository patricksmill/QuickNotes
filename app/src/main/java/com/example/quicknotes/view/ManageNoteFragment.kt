package com.example.quicknotes.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.quicknotes.R
import com.example.quicknotes.databinding.FragmentManageNoteBinding
import com.example.quicknotes.model.Note
import com.example.quicknotes.view.adapters.TagSuggestion
import com.example.quicknotes.view.adapters.TagSuggestionAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.Date

/**
 * Fragment for managing note creation and editing.
 * Handles both new note creation and existing note modification.
 */
class ManageNoteFragment : BottomSheetDialogFragment(), NotesUI {
	private var binding: FragmentManageNoteBinding? = null
	private var listener: NotesUI.Listener? = null
	private var currentNote: Note? = null
	private var isNewNote = false
	private val selectedTagNames: MutableSet<String> = linkedSetOf()
	private var suggestionAdapter: TagSuggestionAdapter? = null
	private var tagsDirty: Boolean = false
	private val debounceHandler = Handler(Looper.getMainLooper())
	private var debounceRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManageNoteBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        @Suppress("DEPRECATION")
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        bindNoteFields()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        if (dialog is BottomSheetDialog) {
            val bottomSheet = dialog!!.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                val lp = sheet.layoutParams
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                sheet.layoutParams = lp
                behavior.isFitToContents = true
                behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

	/**
	 * Sets up all view listeners and event handlers.
	 */
	private fun setupListeners() {
		binding?.let { binding ->

			binding.saveButton.setOnClickListener { saveNote() }
			setupTagAutocomplete()
			
			val aiBtn = view?.findViewById<View>(R.id.aiSuggestButton)
			val aiProgress = view?.findViewById<View>(R.id.aiSuggestProgress)
			aiBtn?.setOnClickListener {
				suggestTagsWithLoading(aiBtn, aiProgress)
			}
			
			val cancel = view?.findViewById<View>(R.id.cancelButton)
			cancel?.setOnClickListener { dismiss() }
		}
	}

	private fun setupTagAutocomplete() {
		val input = binding?.tagInputView ?: return
		rebuildSuggestions("")
		input.setOnItemClickListener { parent, _, position, _ ->
			val item = suggestionAdapter?.getItem(position)
			when (item) {
				is TagSuggestion.Existing -> addTagChip(item.tag.name)
				is TagSuggestion.Create -> addTagChip(item.query)
				else -> {}
			}
			input.setText("")
		}
		input.setOnEditorActionListener { _, actionId, _ ->
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				val text = input.text?.toString()?.trim().orEmpty()
				if (text.isNotEmpty()) addTagChip(text)
				input.setText("")
				true
			} else false
		}
		input.addTextChangedListener(object : android.text.TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
				val q = s?.toString().orEmpty()
				debounceRunnable = Runnable { rebuildSuggestions(q) }
				debounceHandler.postDelayed(debounceRunnable!!, 200)
			}
			override fun afterTextChanged(s: android.text.Editable?) {}
		})
	}

	private fun rebuildSuggestions(query: String) {
		val input = binding?.tagInputView ?: return
		val tm = listener?.onManageTags()
		val all = tm?.allTags?.toList().orEmpty()
		val lower = query.lowercase()
		val filtered = all
			.filter { !selectedTagNames.contains(it.name) }
			.filter { lower.isEmpty() || it.name.lowercase().contains(lower) }
			.sortedBy { it.name }
			.map { TagSuggestion.Existing(it) }
			.toMutableList<TagSuggestion>()
		if (query.isNotBlank() && all.none { it.name.equals(query, ignoreCase = true) }) {
			filtered.add(0, TagSuggestion.Create(query))
		}
		if (suggestionAdapter == null) {
			suggestionAdapter = TagSuggestionAdapter(requireContext(), filtered)
			input.setAdapter(suggestionAdapter)
		} else {
			suggestionAdapter?.updateData(filtered)
		}
	}

	private fun addTagChip(name: String) {
		val trimmed = name.trim()
		if (trimmed.isEmpty() || selectedTagNames.contains(trimmed)) return
		selectedTagNames.add(trimmed)
		tagsDirty = true
		val chip = Chip(requireContext())
		chip.text = trimmed
		chip.isCloseIconVisible = true
		chip.setOnCloseIconClickListener {
			binding?.selectedTagsChipGroup?.removeView(chip)
			selectedTagNames.remove(trimmed)
			tagsDirty = true
			rebuildSuggestions(binding?.tagInputView?.text?.toString().orEmpty())
			persistSelectedTags()
		}
		chip.setOnClickListener {
			showTagActions(trimmed, chip)
		}
		// Try colorize from existing tags
		val colorRes = listener?.onManageTags()?.allTags?.firstOrNull { it.name.equals(trimmed, true) }?.colorResId
		if (colorRes != null) {
			chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
				ContextCompat.getColor(requireContext(), colorRes)
			)
		}
		binding?.selectedTagsChipGroup?.addView(chip)
		rebuildSuggestions("")
		persistSelectedTags()
	}

	private fun showTagActions(tagName: String, chip: Chip) {
		val actions = arrayOf("Change color", "Rename", "Delete")
		MaterialAlertDialogBuilder(requireContext())
			.setTitle(tagName)
			.setItems(actions) { _, which ->
				when (which) {
					0 -> showColorPickerForTag(tagName, chip)
					1 -> promptRenameTag(tagName, chip)
					2 -> confirmDeleteTag(tagName, chip)
				}
			}
			.show()
	}

	private fun showColorPickerForTag(tagName: String, chip: Chip) {
		val options = listener?.onGetAvailableColors()?.filterNotNull() ?: return
		val names = options.map { it.name }.toTypedArray()
		val resIds = options.map { it.resId }.toIntArray()
		MaterialAlertDialogBuilder(requireContext())
			.setTitle("Select color for '$tagName'")
			.setItems(names) { _, which ->
				val chosen = resIds[which]
				listener?.onSetTagColor(tagName, chosen)
				val color = ContextCompat.getColor(requireContext(), chosen)
				chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
				(activity as? com.example.quicknotes.controller.ControllerActivity)?.refreshNotesAndTags()
			}
			.show()
	}

	private fun promptRenameTag(oldName: String, chip: Chip) {
        val input = EditText(requireContext())
		input.setText(oldName)
        val hPad = resources.getDimensionPixelSize(R.dimen.spacing_lg)
        val vPad = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        input.setPadding(hPad, vPad, hPad, vPad)
        input.setSelection(input.text?.length ?: 0)
		MaterialAlertDialogBuilder(requireContext())
			.setTitle("Rename tag")
			.setView(input)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton("Rename") { _, _ ->
				val newName = input.text?.toString()?.trim().orEmpty()
				if (newName.isNotEmpty() && !newName.equals(oldName, true)) {
					listener?.onRenameTag(oldName, newName)
					selectedTagNames.remove(oldName)
					selectedTagNames.add(newName)
					chip.text = newName
					tagsDirty = true
					rebuildSuggestions(binding?.tagInputView?.text?.toString().orEmpty())
					// Re-resolve color for the new name
					listener?.onManageTags()?.allTags?.firstOrNull { it.name.equals(newName, true) }?.let { t ->
						chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
							ContextCompat.getColor(requireContext(), t.colorResId)
						)
					}
					persistSelectedTags()
				}
			}
			.show()
	}

	private fun confirmDeleteTag(tagName: String, chip: Chip) {
		MaterialAlertDialogBuilder(requireContext())
			.setTitle("Delete tag")
			.setMessage("Remove '$tagName' from all notes?")
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton("Delete") { _, _ ->
				listener?.onDeleteTag(tagName)
				binding?.selectedTagsChipGroup?.removeView(chip)
				selectedTagNames.remove(tagName)
				tagsDirty = true
				rebuildSuggestions(binding?.tagInputView?.text?.toString().orEmpty())
				persistSelectedTags()
			}
			.show()
	}

	private fun suggestTagsWithLoading(aiBtn: View?, progress: View?) {
		listener ?: return
		val aiConfigured = listener!!.onIsAiTaggingConfigured()
		if (!aiConfigured) {
			showError("AI tagging is not configured")
			return
		}

        val title = getText(binding!!.noteTitleText).trim()
        val content = getText(binding!!.noteContentText).trim()
        if (title.isEmpty() && content.isEmpty()) {
            showError("Enter a title or content first")
            return
        }

        val temp = Note(title, content, null)
        aiBtn?.isEnabled = false
        progress?.visibility = View.VISIBLE

        listener!!.onAiSuggestTags(temp, 5,
            { suggestions ->
                if (suggestions.isNullOrEmpty()) {
                    showError("No suggestions")
                } else {
                    showSuggestionDialog(suggestions.filterNotNull())
                }
                aiBtn?.isEnabled = true
                progress?.visibility = View.GONE
            },
            { err ->
                showError("Suggest failed: $err")
                aiBtn?.isEnabled = true
                progress?.visibility = View.GONE
            }
        )
    }

	private fun showSuggestionDialog(suggestions: List<String>) {
		val items = suggestions.toTypedArray()
		val checked = BooleanArray(items.size)
		MaterialAlertDialogBuilder(requireContext())
			.setTitle("AI tag suggestions")
			.setMultiChoiceItems(
				items,
				checked
			) { _, which, isChecked ->
				checked[which] = isChecked
			}
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton("Apply") { _, _ ->
				for (i in items.indices) {
					if (checked[i]) addTagChip(items[i])
				}
				persistSelectedTags()
			}
			.show()
	}

    /**
     * Handles saving the current note.
     */
    private fun saveNote() {
        listener ?: run {
            showError("Unable to save note at this time")
            return
        }

		val title = getText(binding!!.noteTitleText).trim()
		val content = getText(binding!!.noteContentText).trim()
		// Tags from chip selection

        if (title.isEmpty() || content.isEmpty()) {
            showError(getString(R.string.missing_item_field_error))
            return
        }

		currentNote!!.title = title
		currentNote!!.content = content
		if (tagsDirty) {
			persistSelectedTags()
		}

        // If AI confirmation is enabled and this is a new note, show suggestions dialog instead of auto-applying
        if (isNewNote && listener!!.onShouldConfirmAiSuggestions() && listener!!.onIsAiTaggingConfigured()) {
            // Do not navigate away yet; show suggestions first
            listener!!.onAiSuggestTags(currentNote!!, 5, 
                { suggestions ->
                    if (!suggestions.isNullOrEmpty()) showSuggestionDialog(suggestions.filterNotNull())
                }, 
                { }
            )
        }

        if (handleNotifications()) {
            listener!!.onSaveNote(currentNote!!, isNewNote)
            listener!!.onBrowseNotes()
            dismiss()
        }
    }

    /**
     * Handles notification settings validation and setup.
     */
    private fun handleNotifications(): Boolean {
        val enabled = binding!!.notificationsEnabled.isChecked
        var selectedDate: Date? = null

        if (enabled) {
            selectedDate = this.selectedDate
            if (listener != null && !listener!!.onValidateNotificationDate(selectedDate)) {
                showError("Cannot set notification for past date/time. Please select a future time.")
                return false
            }
        }

        listener?.onSetNotification(currentNote!!, enabled, selectedDate)
        return true
    }

    /**
     * Gets text from EditText widget safely.
     */
    private fun getText(editText: EditText): String {
        return editText.text?.toString() ?: ""
    }

    /**
     * Shows error message using Snackbar.
     */
    private fun showError(message: String) {
        Snackbar.make(binding?.root ?: return, message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Gets the selected date and time from the date and time pickers.
     */
    private val selectedDate: Date
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, binding!!.datePicker.year)
            cal.set(Calendar.MONTH, binding!!.datePicker.month)
            cal.set(Calendar.DAY_OF_MONTH, binding!!.datePicker.dayOfMonth)
            cal.set(Calendar.HOUR_OF_DAY, binding!!.timePicker.hour)
            cal.set(Calendar.MINUTE, binding!!.timePicker.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }

    /**
     * Sets the listener for UI events.
     */
    fun setListener(listener: NotesUI.Listener?) {
        this.listener = listener
    }

    /**
     * Binds note data to the UI fields.
     */
    private fun bindNoteFields() {
        currentNote ?: return

		binding?.let { binding ->
			binding.noteTitleText.setText(currentNote!!.title)
			binding.noteContentText.setText(currentNote!!.content)
			val initialTags = currentNote!!.tagNames.toList()
			selectedTagNames.clear()
			binding.selectedTagsChipGroup.removeAllViews()
			for (t in initialTags) addTagChip(t)
			binding.tagInputView.setText("")
			tagsDirty = false

            setupNotificationFields()
        }
    }

    /**
     * Sets up notification-related fields and listeners.
     */
    private fun setupNotificationFields() {
        val notificationsEnabled = currentNote!!.isNotificationsEnabled
        binding!!.notificationsEnabled.isChecked = notificationsEnabled
        updateNotificationVisibility(notificationsEnabled)

        binding!!.datePicker.minDate = System.currentTimeMillis()
        binding!!.notificationsEnabled.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationVisibility(isChecked)
            if (isChecked) setDefaultNotificationTime()
        }

        if (currentNote!!.notificationDate != null) {
            setDateTimeFromNote()
        } else if (notificationsEnabled) {
            setDefaultNotificationTime()
        }
    }

    /**
     * Updates visibility of notification date/time pickers.
     */
    private fun updateNotificationVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding!!.datePicker.visibility = visibility
        binding!!.timePicker.visibility = visibility
    }

    /**
     * Sets date/time pickers from the current note's notification date.
     */
    private fun setDateTimeFromNote() {
        val notificationDate = currentNote!!.notificationDate ?: return
        val cal = Calendar.getInstance()
        cal.time = notificationDate
        binding!!.datePicker.updateDate(
            cal.get(Calendar.YEAR), 
            cal.get(Calendar.MONTH), 
            cal.get(Calendar.DAY_OF_MONTH)
        )
        binding!!.timePicker.hour = cal.get(Calendar.HOUR_OF_DAY)
        binding!!.timePicker.minute = cal.get(Calendar.MINUTE)
    }

    /**
     * Sets the date and time pickers to a default time (1 hour from now).
     */
    private fun setDefaultNotificationTime() {
        val defaultTime = Calendar.getInstance()
        defaultTime.add(Calendar.HOUR_OF_DAY, 1)
        binding!!.datePicker.updateDate(
            defaultTime.get(Calendar.YEAR), 
            defaultTime.get(Calendar.MONTH), 
            defaultTime.get(Calendar.DAY_OF_MONTH)
        )
        binding!!.timePicker.hour = defaultTime.get(Calendar.HOUR_OF_DAY)
        binding!!.timePicker.minute = defaultTime.get(Calendar.MINUTE)
    }

	/**
	 * Sets the note to be edited in this fragment.
	 */
	fun setNoteToEdit(note: Note?) {
		this.currentNote = note
		this.isNewNote = currentNote?.let { it.title.isEmpty() && it.content.isEmpty() } ?: false
		binding?.let { bindNoteFields() }
	}

	private fun persistSelectedTags() {
		if (listener == null || currentNote == null) return
		if (!tagsDirty) return
		currentNote!!.tags.clear()
		val tagNames = selectedTagNames.map { it as String? }.toMutableList()
		listener!!.onSetTags(currentNote!!, tagNames)
		tagsDirty = false
	}
}