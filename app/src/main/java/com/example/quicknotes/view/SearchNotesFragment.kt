package com.example.quicknotes.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quicknotes.R
import com.example.quicknotes.databinding.FragmentSearchNotesBinding
import com.example.quicknotes.databinding.ListNoteBinding
import com.example.quicknotes.databinding.TagChipBinding
import com.example.quicknotes.model.Note
import com.example.quicknotes.model.Tag
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment responsible for displaying and managing the main notes view.
 * Provides functionality for searching, filtering, sorting, and managing notes.
 * Follows MVC pattern by delegating all business logic to the controller.
 */
class SearchNotesFragment : Fragment(), NotesUI {
    private var binding: FragmentSearchNotesBinding? = null
    private var listener: NotesUI.Listener? = null
    private val tagAdapter = TagListAdapter()
    private val notesListAdapter = NotesListAdapter()
    private val activeTagFilters: MutableSet<String> = mutableSetOf()
    private var baseNotes: MutableList<Note> = mutableListOf()
    private var currentSortBy = "date"

    private data class RenderNote(
        val source: Note,
        val id: String,
        val title: String,
        val content: String,
        val lastModified: Date,
        val tagNames: List<String>,
        val isPinned: Boolean,
        val isNotificationsEnabled: Boolean,
        val notificationDate: Date?
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchNotesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupSearchView()
        setupClickListeners()
        setupSwipeToDelete()
        refreshTagChips()
        listener?.let { updateView(it.onGetNotes()) }
    }

    private fun setupRecyclerViews() {
        binding?.let { binding ->
            binding.notesRecyclerView.adapter = notesListAdapter
            binding.notesRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.tagRecyclerView.adapter = tagAdapter
            binding.tagRecyclerView.layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun setupSearchView() {
        val searchView = binding?.searchBar ?: return
        searchView.setOnCloseListener {
            searchView.setQuery("", false)
            listener?.onSearchNotes("", title = true, content = true, tag = true)
            false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return handleSearch(query)
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return handleSearch(newText)
            }

            private fun handleSearch(query: String): Boolean {
                listener?.onSearchNotes(query, title = true, content = true, tag = true)
                return true
            }
        })
    }

    private fun setupClickListeners() {
        binding?.let { binding ->
            binding.sortButton.setOnClickListener { showSortDialog() }
            binding.manageTagsButton.setOnClickListener {
                val sheet = ManageTagsFragment()
                sheet.setListener(listener)
                sheet.show(parentFragmentManager, "ManageTags")
            }
            binding.addNoteFab.setOnClickListener { listener?.onNewNote() }
            binding.addNoteFab.setOnLongClickListener {
                listener?.let {
                    it.onAddDemoNotes()
                    updateView(it.onGetNotes())
                }
                true
            }
            binding.settingsButton.setOnClickListener { listener?.onOpenSettings() }
        }
    }

    private fun setupSwipeToDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = notesListAdapter.listNotes[position]
                    listener?.onDeleteNote(item.source)
                    Snackbar.make(
                        binding?.root ?: return,
                        "Note deleted",
                        Snackbar.LENGTH_LONG
                    ).setAction("Undo") {
                        listener?.onUndoDelete()
                    }.show()
                }
            }
        }).attachToRecyclerView(binding?.notesRecyclerView)
    }

    fun setListener(listener: NotesUI.Listener?) {
        this.listener = listener
    }

    fun updateView(notes: List<Note>) {
        baseNotes.clear()
        baseNotes.addAll(notes)
        refreshTagChips()
        displayNotes()
    }

    private fun refreshTagChips() {
        val tags = listener?.onGetAllTags() ?: mutableSetOf()
        tagAdapter.updateData(tags)
    }

    private fun displayNotes() {
        var notes = baseNotes.toMutableList()

        // Apply tag filtering
        if (activeTagFilters.isNotEmpty()) {
            notes = notes.filter { note ->
                note.tags.any { tag ->
                    activeTagFilters.contains(tag.name)
                }
            }.toMutableList()
        }

        // Sort notes: pinned first, then by current sort
        notes.sortWith { a, b ->
            if (a.isPinned != b.isPinned) {
                if (a.isPinned) -1 else 1
            } else {
                when (currentSortBy) {
                    "title" -> a.title.compareTo(b.title, ignoreCase = true)
                    else -> b.lastModified.compareTo(a.lastModified)
                }
            }
        }

        notesListAdapter.updateData(notes)
        val isEmpty = notes.isEmpty()
        binding?.let { binding ->
            binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.notesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun setupPins(pinIcon: ImageView, note: Note) {
        pinIcon.isSelected = note.isPinned
        pinIcon.setOnClickListener { view ->
            listener ?: return@setOnClickListener
            listener!!.onTogglePin(note)
            view.isSelected = note.isPinned
            Snackbar.make(
                binding?.root ?: return@setOnClickListener,
                "Note ${if (note.isPinned) "pinned" else "unpinned"}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showSortDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Notes")
            .setItems(
                arrayOf("Sort by Date", "Sort by Title")
            ) { _, which ->
                currentSortBy = if (which == 1) "title" else "date"
                displayNotes()
            }
            .show()
    }

    private inner class TagListAdapter : RecyclerView.Adapter<TagListAdapter.ViewHolder>() {
        private var listTags: MutableList<Tag> = mutableListOf()
        private val selectedTags: MutableSet<String> = mutableSetOf()

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                TagChipBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(listTags[position], selectedTags.contains(listTags[position].name))
        }

        override fun getItemCount(): Int = listTags.size

        override fun getItemId(position: Int): Long = listTags[position].name.hashCode().toLong()

        fun updateData(tags: Collection<Tag>) {
            val newList = tags.toMutableList()
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = listTags.size
                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return listTags[oldItemPosition].name == newList[newItemPosition].name
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val a = listTags[oldItemPosition]
                    val b = newList[newItemPosition]
                    return a.name == b.name && a.colorResId == b.colorResId
                }
            })
            this.listTags = newList
            diff.dispatchUpdatesTo(this)
        }

        fun findTagPositionByName(name: String): Int {
            return listTags.indexOfFirst { it.name == name }
        }

        inner class ViewHolder(private val binding: TagChipBinding) : RecyclerView.ViewHolder(
            binding.root
        ) {
            fun bind(tag: Tag, isSelected: Boolean) {
                val chip = binding.tagChip
                chip.text = tag.name
                chip.isChecked = isSelected
                val baseColor = ContextCompat.getColor(requireContext(), tag.colorResId)
                chip.chipBackgroundColor = ColorStateList.valueOf(baseColor)
                
                // Choose readable text color based on luminance
                val textColor = if (ColorUtils.calculateLuminance(baseColor) > 0.5) {
                    ContextCompat.getColor(requireContext(), android.R.color.black)
                } else {
                    ContextCompat.getColor(requireContext(), android.R.color.white)
                }
                chip.setTextColor(textColor)
                chip.chipStrokeColor = ColorStateList.valueOf(
                    ColorUtils.blendARGB(baseColor, -0x1000000, 0.2f)
                )
                chip.chipStrokeWidth = resources.getDimensionPixelSize(R.dimen.chip_stroke_width).toFloat()
                
                chip.setOnClickListener {
                    val previouslySelected = selectedTags.firstOrNull()
                    selectedTags.clear()
                    if (!isSelected) selectedTags.add(tag.name)

                    activeTagFilters.clear()
                    activeTagFilters.addAll(selectedTags)
                    displayNotes()

                    // Notify only the two chips that changed selection state
                    previouslySelected?.let { prev ->
                        val prevPos = findTagPositionByName(prev)
                        if (prevPos >= 0) notifyItemChanged(prevPos)
                    }
                    val currPos = bindingAdapterPosition
                    if (currPos != RecyclerView.NO_POSITION) notifyItemChanged(currPos)
                }
            }
        }
    }

    private inner class NotesListAdapter : RecyclerView.Adapter<NotesListAdapter.ViewHolder>() {
        var listNotes: MutableList<RenderNote> = mutableListOf()

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                ListNoteBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(listNotes[position])
        }

        override fun getItemCount(): Int = listNotes.size

        override fun getItemId(position: Int): Long = listNotes[position].id.hashCode().toLong()

        fun updateData(notes: List<Note>) {
            val newList = notes.map { n ->
                RenderNote(
                    source = n,
                    id = n.id,
                    title = n.title,
                    content = n.content,
                    lastModified = n.lastModified,
                    tagNames = n.tagNames.toList(),
                    isPinned = n.isPinned,
                    isNotificationsEnabled = n.isNotificationsEnabled,
                    notificationDate = n.notificationDate
                )
            }.toMutableList()
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = listNotes.size
                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return listNotes[oldItemPosition].id == newList[newItemPosition].id
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val a = listNotes[oldItemPosition]
                    val b = newList[newItemPosition]
                    if (a.isPinned != b.isPinned) return false
                    if (a.title != b.title) return false
                    if (a.content != b.content) return false
                    if (a.lastModified != b.lastModified) return false
                    if (a.isNotificationsEnabled != b.isNotificationsEnabled) return false
                    if (a.notificationDate != b.notificationDate) return false

                    val at = a.tagNames.toMutableList().apply { sort() }
                    val bt = b.tagNames.toMutableList().apply { sort() }
                    return at == bt
                }
            })
            this.listNotes = newList
            diff.dispatchUpdatesTo(this)
        }

        inner class ViewHolder(private val binding: ListNoteBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(note: RenderNote) {
                binding.noteNameText.text = note.title
                binding.noteContentText.text = note.content
                binding.noteDateText.text = DATE_FORMAT.format(note.lastModified)
                binding.noteTagsText.text = TextUtils.join(", ", note.tagNames)
                setupPins(binding.notePinIcon, note.source)
                
                val showNotif = listener?.onShouldShowNotificationIcon(note.source) == true
                if (showNotif) {
                    binding.noteTagsText.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_bell, 0, 0, 0
                    )
                } else {
                    binding.noteTagsText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
                
                binding.root.setOnClickListener {
                    listener?.onManageNotes(note.source)
                }
            }
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    }
}