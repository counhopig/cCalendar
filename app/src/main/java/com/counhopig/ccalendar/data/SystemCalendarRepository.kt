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

class SystemCalendarRepository(val context: Context) {

    fun updateCalendarColor(calendarId: Long, colorInt: Int) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Calendars.CALENDAR_COLOR, colorInt)
            }
            val updateUri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId)
            // Note: Updating Calendars usually requires appending ACCOUNT_NAME and ACCOUNT_TYPE as query parameters
            // to function as a Sync Adapter, OR the provider might allow local color changes.
            // If this fails, it's a limitation of Android Calendar Provider for non-sync-adapter apps.
            // But usually CALENDAR_COLOR is editable.
            
            // However, modifying CALENDAR_COLOR indicates a sync change.
            // Let's try appending CallerIsSyncAdapter to permit changes if necessary, but standard apps shouldn't use it.
            // We just try a bold update first.
            context.contentResolver.update(updateUri, values, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

                    // Filter out problematic calendars that effectively don't work or are system artifacts
                    if (name == "calendar_displayname_xiaomi" || name == "calendar_displayname_birthday") {
                        continue
                    }
                    
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
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.HAS_ALARM
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
                val calendarIdIdx = it.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
                val hasAlarmIdx = it.getColumnIndex(CalendarContract.Instances.HAS_ALARM)

                while (it.moveToNext()) {
                    val title = if (titleIdx != -1) it.getString(titleIdx) ?: "No Title" else "No Title"
                    val description = if (descIdx != -1) it.getString(descIdx) ?: "" else ""
                    val begin = if (beginIdx != -1) it.getLong(beginIdx) else 0L
                    val end = if (endIdx != -1) it.getLong(endIdx) else 0L
                    val allDay = if (allDayIdx != -1) it.getInt(allDayIdx) == 1 else false
                    val colorInt = if (colorIdx != -1) it.getInt(colorIdx) else 0xFF7C5CFF.toInt()
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val calendarId = if (calendarIdIdx != -1) it.getLong(calendarIdIdx) else 1L
                    val hasAlarm = if (hasAlarmIdx != -1) it.getInt(hasAlarmIdx) == 1 else false

                    var reminderMinutes = 0
                    if (hasAlarm) {
                        val reminderCursor = context.contentResolver.query(
                            CalendarContract.Reminders.CONTENT_URI,
                            arrayOf(CalendarContract.Reminders.MINUTES),
                            "${CalendarContract.Reminders.EVENT_ID} = ?",
                            arrayOf(id.toString()),
                            null
                        )
                        reminderCursor?.use {
                            if (it.moveToFirst()) {
                                val minutesIdx = it.getColumnIndex(CalendarContract.Reminders.MINUTES)
                                if (minutesIdx != -1) {
                                    reminderMinutes = it.getInt(minutesIdx)
                                }
                            }
                        }
                    }

                    val displayColor = if (colorInt != 0) Color(colorInt) else Color(0xFF7C5CFF)

                    // All-day events are stored in UTC, normal events in local time (or respective timezone)
                    val zoneId = if (allDay) ZoneId.of("UTC") else ZoneId.systemDefault()
                    val startZoned = Instant.ofEpochMilli(begin).atZone(zoneId)
                    val endZoned = Instant.ofEpochMilli(end).atZone(zoneId)
                    
                    val startDate = startZoned.toLocalDate()
                    var endDate = endZoned.toLocalDate()
                    
                    val endTimeCheck = endZoned.toLocalTime()
                    
                    if (allDay || (endTimeCheck == LocalTime.MIDNIGHT && !startDate.isEqual(endDate))) {
                        endDate = endDate.minusDays(1)
                    }

                    var currentDate = startDate
                    while (!currentDate.isAfter(endDate)) {
                        val startTime = if (allDay) null else startZoned.toLocalTime()
                        val endTime = if (allDay) null else endZoned.toLocalTime()

                        events.add(Event(
                            id = id,
                            calendarId = calendarId,
                            title = if (startDate == endDate) title else "$title", 
                            description = description,
                            originalStartDate = startDate,
                            originalEndDate = endDate,
                            date = currentDate,
                            startTime = startTime,
                            endTime = endTime,
                            isAllDay = allDay,
                            color = displayColor,
                            reminderMinutes = reminderMinutes
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
            val startMillis: Long
            val endMillis: Long
            val eventTimezone: String

            if (event.isAllDay) {
                // All-day events: UTC midnight using java.util.Calendar to be safe
                val utc = TimeZone.getTimeZone("UTC")
                val cal = java.util.Calendar.getInstance(utc)
                cal.set(event.originalStartDate.year, event.originalStartDate.monthValue - 1, event.originalStartDate.dayOfMonth, 0, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                startMillis = cal.timeInMillis
                
                // End date is start date + duration (in days)
                // For safety, let's calculate end millis based on originalEndDate + 1 day
                cal.set(event.originalEndDate.year, event.originalEndDate.monthValue - 1, event.originalEndDate.dayOfMonth, 0, 0, 0)
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                endMillis = cal.timeInMillis
                
                eventTimezone = "UTC"
            } else {
                startMillis = event.originalStartDate.atTime(event.startTime ?: LocalTime.MIDNIGHT)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMillis = event.originalEndDate.atTime(event.endTime ?: LocalTime.MIDNIGHT.plusHours(1))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                eventTimezone = TimeZone.getDefault().id
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, eventTimezone)
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
                put(CalendarContract.Events.HAS_ALARM, if (event.reminderMinutes > 0) 1 else 0)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLong()

            if (eventId != null && event.reminderMinutes > 0) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, event.reminderMinutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
            
            return eventId
        } catch (e: SecurityException) {
            e.printStackTrace()
            return null
        }
    }

    fun updateEvent(event: Event) {
        try {
            val startMillis: Long
            val endMillis: Long
            
            if (event.isAllDay) {
                // All-day events: UTC midnight using java.util.Calendar to be safe
                val utc = TimeZone.getTimeZone("UTC")
                val cal = java.util.Calendar.getInstance(utc)
                cal.set(event.originalStartDate.year, event.originalStartDate.monthValue - 1, event.originalStartDate.dayOfMonth, 0, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                startMillis = cal.timeInMillis
                
                cal.set(event.originalEndDate.year, event.originalEndDate.monthValue - 1, event.originalEndDate.dayOfMonth, 0, 0, 0)
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                endMillis = cal.timeInMillis
            } else {
                startMillis = event.originalStartDate.atTime(event.startTime ?: LocalTime.MIDNIGHT)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                endMillis = event.originalEndDate.atTime(event.endTime ?: LocalTime.MIDNIGHT.plusHours(1))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
                put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
                put(CalendarContract.Events.HAS_ALARM, if (event.reminderMinutes > 0) 1 else 0)
                // If switching between all-day and normal, timezone update is crucial
                if(event.isAllDay) {
                     put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                } else {
                     put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
            }

            val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
            context.contentResolver.update(updateUri, values, null, null)

            // Remove old reminders and add new one
            context.contentResolver.delete(CalendarContract.Reminders.CONTENT_URI, "${CalendarContract.Reminders.EVENT_ID} = ?", arrayOf(event.id.toString()))
            if (event.reminderMinutes > 0) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, event.id)
                    put(CalendarContract.Reminders.MINUTES, event.reminderMinutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun deleteEvent(eventId: Long) {
        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(deleteUri, null, null)
            // Reminders are deleted automatically by the provider
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
