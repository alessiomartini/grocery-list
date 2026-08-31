package com.alessiomartini.dispensa.ui.expiry

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How many days ahead an item counts as "expiring soon" and gets highlighted. */
const val EXPIRY_WARNING_WINDOW_DAYS = 3L

enum class ExpiryUrgency { EXPIRED, TODAY, SOON, LATER }

data class ExpiryStatus(val urgency: ExpiryUrgency, val daysUntil: Long)

fun expiryStatusOf(date: LocalDate, today: LocalDate = LocalDate.now()): ExpiryStatus {
    val days = ChronoUnit.DAYS.between(today, date)
    val urgency = when {
        days < 0 -> ExpiryUrgency.EXPIRED
        days == 0L -> ExpiryUrgency.TODAY
        days <= EXPIRY_WARNING_WINDOW_DAYS -> ExpiryUrgency.SOON
        else -> ExpiryUrgency.LATER
    }
    return ExpiryStatus(urgency, days)
}

fun colorForUrgency(urgency: ExpiryUrgency): Color = when (urgency) {
    ExpiryUrgency.EXPIRED -> Color(0xFFC62828)
    ExpiryUrgency.TODAY -> Color(0xFFC62828)
    ExpiryUrgency.SOON -> Color(0xFFF57C00)
    ExpiryUrgency.LATER -> Color(0xFF2E7D32)
}
