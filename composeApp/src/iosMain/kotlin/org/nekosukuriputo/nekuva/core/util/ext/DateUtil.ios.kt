package org.nekosukuriputo.nekuva.core.util.ext

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import org.nekosukuriputo.nekuva.ioscompat.System

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatEpochToDateString(epochMillis: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(epochMillis))

actual fun daysAgo(epochMillis: Long): Long {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.until(LocalDate.now(), ChronoUnit.DAYS)
}
