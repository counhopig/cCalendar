package com.counhopig.ccalendar.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import com.counhopig.ccalendar.ui.model.Event
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.ZoneId 

class SystemCalendarRepository(private val context: Context) {

    fun getSystemEvents(): List<Event> {
        val events = mutableListOf<Event>()
        
        // Define range: +/- 1 year from now
        val now = Instant.now()
        val startMillis = now.minusSeconds(86400 * 365).toEpochMilli()
        val endMillis = now.plusSeconds(86400 * 365).toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR
        )

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val colorIdx = it.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
                val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)

                while (it.moveToNext()) {
                    val title = if (titleIdx != -1) it.getString(titleIdx) ?: "No Title" else "No Title"
                    val description = if (descIdx != -1) it.getString(descIdx) ?: "" else ""
                    val begin = if (beginIdx != -1) it.getLong(beginIdx) else 0L
                    val end = if (endIdx != -1) it.getLong(endIdx) else 0L
                    val allDay = if (allDayIdx != -1) it.getInt(allDayIdx) == 1 else false
                    val colorInt = if (colorIdx != -1) it.getInt(colorIdx) else 0xFF7C5CFF.toInt()
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L

                    val displayColor = if (colorInt != 0) Color(colorInt) else Color(0xFF7C5CFF)

                    // All-day events are stored in UTC, normal events in local time (or respective timezone)
                    val zoneId = if (allDay) ZoneId.of("UTC") else ZoneId.systemDefault()
                    val startZoned = Instant.ofEpochMilli(begin).atZone(zoneId)
                    val endZoned = Instant.ofEpochMilli(end).atZone(zoneId)
                    
                    val startDate = startZoned.toLocalDate()
                    var endDate = endZoned.toLocalDate()
                    
                    val endTimeCheck = endZoned.toLocalTime()
                    
                    // For allDay events, the end date is exclusive (e.g. starts Jan 18 00:00, ends Jan 19 00:00 = 1 day).
                    // For normal events, if it ends at midnight, it is also exclusive of that ending day.
                    if (allDay || (endTimeCheck == LocalTime.MIDNIGHT && !startDate.isEqual(endDate))) {
                        endDate = endDate.minusDays(1)
                    }

                    // Expand multi-day events
                    var currentDate = startDate
                    while (!currentDate.isAfter(endDate)) {
                        val startTime = if (allDay) null else startZoned.toLocalTime()
                        val endTime = if (allDay) null else endZoned.toLocalTime()

                        events.add(Event(
                            id = id, 
                            title = if (startDate == endDate) title else "$title", 
                            description = description,
                            date = currentDate,
                            startTime = startTime,
                            endTime = endTime,
                            isAllDay = allDay,
                            color = displayColor
                        ))
                        currentDate = currentDate.plusDays(1)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }
}
