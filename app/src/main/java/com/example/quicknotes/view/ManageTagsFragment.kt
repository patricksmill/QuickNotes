package com.example.quicknotes.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quicknotes.databinding.ItemManageTagBinding
import com.example.quicknotes.databinding.SheetManageTagsBinding
import com.example.quicknotes.model.tag.Tag
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Bottom sheet to manage tags (color, rename, delete) from the search screen.
 */
class ManageTagsFragment : BottomSheetDialogFragment() {
    private var binding: SheetManageTagsBinding? = null
    private var listener: NotesUI.Listener? = null
    private val adapter = TagListAdapter()

    fun setListener(l: NotesUI.Listener?) { listener = l }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SheetManageTagsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.recycler?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recycler?.adapter = adapter
        binding?.closeButton?.setOnClickListener { dismiss() }
        loadTags()
    }

    private fun loadTags() {
        val tags = (listener?.onGetAllTags() ?: mutableSetOf()).toList().sortedBy { it.name }
        adapter.updateData(tags)
    }

    private inner class TagListAdapter : RecyclerView.Adapter<TagListAdapter.VH>() {
        private var items: List<Tag> = emptyList()

        fun updateData(newData: List<Tag>) {
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = items.size
                override fun getNewListSize() = newData.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    items[oldItemPosition].name.equals(newData[newItemPosition].name, true)
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val a = items[oldItemPosition]; val b = newData[newItemPosition]
                    return a.name == b.name && a.colorResId == b.colorResId
                }
            })
            items = newData
            diff.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemManageTagBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(private val b: ItemManageTagBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(tag: Tag) {
                b.tagName.text = tag.name
                b.colorDot.background.setTint(ContextCompat.getColor(requireContext(), tag.colorResId))
                b.root.setOnClickListener { showActions(tag) }
            }
        }
    }

    private fun showActions(tag: Tag) {
        val actions = arrayOf("Change color", "Rename", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(tag.name)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showColorPicker(tag)
                    1 -> promptRename(tag)
                    2 -> confirmDelete(tag)
                }
            }
            .show()
    }

    private fun showColorPicker(tag: Tag) {
        val options = listener?.onGetAvailableColors()?.filterNotNull() ?: return
        val names = options.map { it.name }.toTypedArray()
        val res = options.map { it.resId }.toIntArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select color for '${tag.name}'")
            .setItems(names) { _, idx ->
                listener?.onSetTagColor(tag.name, res[idx])
                loadTags()
            }
            .show()
    }

    private fun promptRename(tag: Tag) {
        val input = android.widget.EditText(requireContext())
        input.setText(tag.name)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename tag")
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isNotEmpty() && !newName.equals(tag.name, true)) {
                    listener?.onRenameTag(tag.name, newName)
                    loadTags()
                }
            }
            .show()
    }

    private fun confirmDelete(tag: Tag) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete tag")
            .setMessage("Remove '${tag.name}' from all notes?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                listener?.onDeleteTag(tag.name)
                loadTags()
            }
            .show()
    }
}


