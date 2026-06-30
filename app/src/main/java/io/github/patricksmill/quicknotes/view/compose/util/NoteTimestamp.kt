package io.github.patricksmill.quicknotes.view.compose.util

import android.icu.text.RelativeDateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private val DATE_ONLY_FORMAT = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

fun formatNoteTimestamp(date: Date): String {
    val nowMs = System.currentTimeMillis()
    val diffMs = date.time - nowMs
    val absMs = abs(diffMs)

    val weekMs = TimeUnit.DAYS.toMillis(7)
    if (absMs >= weekMs) {
        return DATE_ONLY_FORMAT.format(date)
    }

    val fmt = RelativeDateTimeFormatter.getInstance(Locale.getDefault())

    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    if (days != 0L) return fmt.format(days.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY)

    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    if (hours != 0L) return fmt.format(hours.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.HOUR)

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    if (minutes != 0L) return fmt.format(minutes.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.MINUTE)

    val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs)
    return fmt.format(seconds.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.SECOND)
}
