package io.github.patricksmill.quicknotes.model.tag

import androidx.annotation.ColorRes
import java.util.Locale

@JvmRecord
data class Tag(@JvmField val name: String, @JvmField @field:ColorRes @param:ColorRes val colorResId: Int) {

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Tag) return false
        return name.equals(o.name, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return name.lowercase(Locale.getDefault()).hashCode()
    }
}

