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
import com.counhopig.ccalendar.ui.model.Calendar
import java.time.YearMonth
import java.time.ZoneId
import android.content.ContentValues
import java.util.TimeZone

class SystemCalendarRepository(private val context: Context) {

    fun getCalendars(): List<Calendar> {
        val calendars = mutableListOf<Calendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val colorIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else -1
                    val name = if (nameIdx != -1) it.getString(nameIdx) ?: "Unnamed" else "Unnamed"
                    val colorInt = if (colorIdx != -1) it.getInt(colorIdx) else 0
                    
                    if (id != -1L) {
                        calendars.add(Calendar(id, name, Color(colorInt)))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return calendars
    }

    fun getSystemEvents(calendarIds: Set<Long>, yearMonth: YearMonth): List<Event> {
        if (calendarIds.isEmpty()) {
            return emptyList()
        }

        val events = mutableListOf<Event>()
        
        val startOfMonth = yearMonth.minusMonths(1).atDay(1)
        val endOfMonth = yearMonth.plusMonths(1).atEndOfMonth()
        val startMillis = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endOfMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()


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
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_ID
        )
        
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
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


    fun addEvent(event: Event, calendarId: Long = 1): Long? {
        try {
            val startMillis = event.date.atTime(event.startTime ?: LocalTime.MIDNIGHT)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val endMillis = event.date.atTime(event.endTime ?: LocalTime.MIDNIGHT.plusHours(1))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment?.toLong()
        } catch (e: SecurityException) {
            e.printStackTrace()
            return null
        }
    }

    fun updateEvent(event: Event) {
        try {
            val startMillis = event.date.atTime(event.startTime ?: LocalTime.MIDNIGHT)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val endMillis = event.date.atTime(event.endTime ?: LocalTime.MIDNIGHT.plusHours(1))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
            }

            val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
            context.contentResolver.update(updateUri, values, null, null)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun deleteEvent(eventId: Long) {
        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(deleteUri, null, null)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
