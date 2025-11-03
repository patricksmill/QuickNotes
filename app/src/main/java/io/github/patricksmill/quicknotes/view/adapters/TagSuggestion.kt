package io.github.patricksmill.quicknotes.view.adapters

import io.github.patricksmill.quicknotes.model.tag.Tag

sealed class TagSuggestion {
	data class Existing(val tag: Tag) : TagSuggestion()
	data class Create(val query: String) : TagSuggestion()
}
