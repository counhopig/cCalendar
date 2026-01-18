package com.counhopig.ccalendar.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import com.counhopig.ccalendar.ui.model.Event
import java.time.Instant
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

                    // Handle color. If 0 (black/transparent), use default. 
                    // Android calendar colors are typically valid ARGB.
                    // If opaque alpha is missing, we might want to ensure it.
                    // But usually DISPLAY_COLOR is a full int.
                    val displayColor = if (colorInt != 0) Color(colorInt) else Color(0xFF7C5CFF)

                    val startInstant = Instant.ofEpochMilli(begin)
                    val endInstant = Instant.ofEpochMilli(end)
                    val zoneId = ZoneId.systemDefault()
                    
                    val startDate = startInstant.atZone(zoneId).toLocalDate()
                    
                    val startTime = if (allDay) null else startInstant.atZone(zoneId).toLocalTime()
                    val endTime = if (allDay) null else endInstant.atZone(zoneId).toLocalTime()

                    events.add(Event(
                        id = id, 
                        title = title,
                        description = description,
                        date = startDate,
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = allDay,
                        color = displayColor
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }
}
