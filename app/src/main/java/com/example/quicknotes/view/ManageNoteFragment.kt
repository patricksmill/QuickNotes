package com.example.quicknotes.view

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import com.example.quicknotes.R
import com.example.quicknotes.databinding.FragmentManageNoteBinding
import com.example.quicknotes.model.Note
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
            binding.tagsInputLayout.setEndIconOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("How to enter tags")
                    .setMessage("Enter tags separated by commas, like:\n\nwork, urgent, meeting\n\nSpaces are optional but will be trimmed.")
                    .setPositiveButton("Got it", null)
                    .show()
            }

            binding.saveButton.setOnClickListener { saveNote() }
            
            val aiBtn = view?.findViewById<View>(R.id.aiSuggestButton)
            val aiProgress = view?.findViewById<View>(R.id.aiSuggestProgress)
            aiBtn?.setOnClickListener {
                suggestTagsWithLoading(aiBtn, aiProgress)
            }
            
            val cancel = view?.findViewById<View>(R.id.cancelButton)
            cancel?.setOnClickListener { dismiss() }
        }
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
                val chosen = mutableListOf<String?>()
                for (i in items.indices) {
                    if (checked[i]) chosen.add(items[i])
                }
                if (chosen.isNotEmpty() && listener != null) {
                    listener!!.onSetTags(currentNote!!, chosen)
                    bindNoteFields()
                }
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
        val tagsString = getText(binding!!.noteTagsText)

        if (title.isEmpty() || content.isEmpty()) {
            showError(getString(R.string.missing_item_field_error))
            return
        }

        val tagNames = tagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it as String? }
            .toMutableList()

        currentNote!!.title = title
        currentNote!!.content = content
        currentNote!!.tags.clear()
        listener!!.onSetTags(currentNote!!, tagNames)

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
            binding.noteTagsText.setText(
                TextUtils.join(", ", currentNote!!.tagNames)
            )

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
}