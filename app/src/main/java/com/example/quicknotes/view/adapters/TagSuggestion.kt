package com.example.quicknotes.view.adapters

import com.example.quicknotes.model.tag.Tag

sealed class TagSuggestion {
	data class Existing(val tag: Tag) : TagSuggestion()
	data class Create(val query: String) : TagSuggestion()
}
