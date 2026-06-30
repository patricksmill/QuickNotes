package io.github.patricksmill.quicknotes.model.tag

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TagTest {
    @Test
    fun equalsIsCaseInsensitiveOnName() {
        val lower = Tag("work", 0x7f060001)
        val upper = Tag("WORK", 0x7f060002)

        assertEquals(lower, upper)
    }

    @Test
    fun hashCodeIsCaseInsensitiveOnName() {
        val lower = Tag("work", 0x7f060001)
        val upper = Tag("WORK", 0x7f060002)

        assertEquals(lower.hashCode(), upper.hashCode())
    }

    @Test
    fun differentNamesAreNotEqual() {
        val first = Tag("work", 0x7f060001)
        val second = Tag("personal", 0x7f060001)

        assertNotEquals(first, second)
    }
}
