package com.example.quicknotes.view.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.quicknotes.R

class TagSuggestionAdapter(
	context: Context,
	private var items: MutableList<TagSuggestion>
) : ArrayAdapter<TagSuggestion>(context, 0, items) {

	fun updateData(newItems: List<TagSuggestion>) {
		items.clear()
		items.addAll(newItems)
		notifyDataSetChanged()
	}

	override fun getCount(): Int = items.size
	override fun getItem(position: Int): TagSuggestion? = items.getOrNull(position)

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_tag_suggestion, parent, false)
		val colorDot = view.findViewById<View>(R.id.colorDot)
		val text = view.findViewById<TextView>(R.id.suggestionText)

		when (val item = items[position]) {
			is TagSuggestion.Existing -> {
				text.text = item.tag.name
				colorDot.visibility = View.VISIBLE
				val color = ContextCompat.getColor(context, item.tag.colorResId)
				colorDot.background.setTint(color)
			}
			is TagSuggestion.Create -> {
				text.text = context.getString(R.string.create_tag_label, item.query)
				colorDot.visibility = View.INVISIBLE
			}
		}
		return view
	}
}
