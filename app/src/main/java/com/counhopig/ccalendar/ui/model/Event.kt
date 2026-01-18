package com.counhopig.ccalendar.ui.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime

data class Event(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val color: Color = Color(0xFF7C5CFF),
    val isAllDay: Boolean = false,
    val reminderMinutes: Int = 0 // 0表示无提醒
) {
    val hasTime: Boolean
        get() = startTime != null || !isAllDay
}