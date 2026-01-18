package com.counhopig.ccalendar.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counhopig.ccalendar.data.SystemCalendarRepository
import com.counhopig.ccalendar.ui.model.Calendar as AppCalendar
import com.counhopig.ccalendar.ui.model.Event as AppEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.Parameter
import java.time.LocalDateTime
import java.time.ZonedDateTime

class EventViewModel : ViewModel() {
    private val _events = mutableStateListOf<AppEvent>()
    val events: SnapshotStateList<AppEvent> = _events
    
    private val _calendars = mutableStateListOf<AppCalendar>()
    val calendars: SnapshotStateList<AppCalendar> = _calendars
    
    var selectedCalendarIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    private lateinit var repository: SystemCalendarRepository

    private fun initializeRepository(context: Context) {
        if (!::repository.isInitialized) {
            repository = SystemCalendarRepository(context)
        }
    }
    
    fun loadCalendars(context: Context) {
        initializeRepository(context)
        val systemCalendars = repository.getCalendars()
        _calendars.clear()
        _calendars.addAll(systemCalendars)
        // Default to all calendars selected
        selectedCalendarIds = systemCalendars.map { it.id }.toSet()
    }

    fun toggleCalendarSelection(id: Long) {
        selectedCalendarIds = if (selectedCalendarIds.contains(id)) {
            selectedCalendarIds - id
        } else {
            selectedCalendarIds + id
        }
    }

    fun loadSystemEvents(context: Context, yearMonth: YearMonth) {
        initializeRepository(context)
        val systemEvents = repository.getSystemEvents(selectedCalendarIds, yearMonth)
        
        _events.clear()
        _events.addAll(systemEvents)
    }

    fun addEvent(event: AppEvent, context: Context) {
        initializeRepository(context)
        repository.addEvent(event)
        // Refresh events from source to get the new event with its system ID
        loadSystemEvents(context, YearMonth.from(event.date))
    }

    fun updateEvent(updatedEvent: AppEvent, context: Context) {
        initializeRepository(context)
        repository.updateEvent(updatedEvent)
        loadSystemEvents(context, YearMonth.from(updatedEvent.date))
    }

    fun deleteEvent(eventId: Long, context: Context) {
        initializeRepository(context)
        repository.deleteEvent(eventId)
        loadSystemEvents(context, YearMonth.now()) // Refresh current month as we don't know the event's date
    }
    
    fun importIcs(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val builder = CalendarBuilder()
                        val calendar = builder.build(inputStream)
                        @Suppress("UNCHECKED_CAST")
                        val events = calendar.getComponents<VEvent>("VEVENT")
                        for (component in events) {
                            val dtStartProp = component.getProperty<DtStart<*>>("DTSTART").orElse(null)
                            
                            if (dtStartProp != null) {
                                val dateObj = dtStartProp.date
                                val valueParam = dtStartProp.getParameter<Parameter>("VALUE").orElse(null)
                                val isAllDay = valueParam?.value == "DATE"

                                val startDate: LocalDate
                                val startTime: LocalTime?
                                
                                if (dateObj is LocalDate) {
                                    startDate = dateObj
                                    startTime = null
                                } else if (dateObj is LocalDateTime) {
                                    startDate = dateObj.toLocalDate()
                                    startTime = dateObj.toLocalTime()
                                } else if (dateObj is ZonedDateTime) {
                                    startDate = dateObj.toLocalDate()
                                    startTime = dateObj.toLocalTime()
                                } else if (dateObj is java.util.Date) {
                                     val zdt = dateObj.toInstant().atZone(ZoneId.systemDefault())
                                     startDate = zdt.toLocalDate()
                                     startTime = zdt.toLocalTime()
                                } else {
                                    startDate = LocalDate.now()
                                    startTime = null
                                }
                                
                                val summary = component.getProperty<Property>("SUMMARY").orElse(null)?.value ?: "No Title"
                                val description = component.getProperty<Property>("DESCRIPTION").orElse(null)?.value ?: ""

                                val appEvent = AppEvent(
                                    title = summary,
                                    description = description,
                                    date = startDate,
                                    startTime = startTime,
                                    endTime = null,
                                    isAllDay = isAllDay,
                                )
                                // Add to primary calendar by default
                                repository.addEvent(appEvent, calendarId = 1)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Refresh events from source after import
            loadSystemEvents(context, YearMonth.now())
        }
    }

    fun getEventsForDate(date: LocalDate): List<AppEvent> {
        return _events.filter { it.date == date }
            .sortedWith(compareBy(
                { !it.isAllDay },
                { it.startTime ?: LocalTime.MIN }
            ))
    }

    fun getEventsForMonth(yearMonth: java.time.YearMonth): List<AppEvent> {
        return _events.filter { 
            val eventDate = it.date
            eventDate.year == yearMonth.year && eventDate.month == yearMonth.month 
        }
    }
}