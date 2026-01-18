package com.counhopig.ccalendar.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.counhopig.ccalendar.ui.model.Event
import java.time.LocalDate
import java.time.LocalTime

class EventViewModel : ViewModel() {
    private val _events = mutableStateListOf<Event>()
    val events: SnapshotStateList<Event> = _events
    
    // Removed sample events for system calendar integration

    fun loadSystemEvents(context: android.content.Context) {
        val repository = com.counhopig.ccalendar.data.SystemCalendarRepository(context)
        val systemEvents = repository.getSystemEvents()
        
        _events.clear()
        _events.addAll(systemEvents)
    }

    fun addEvent(event: Event) {
        _events.add(event.copy(id = System.currentTimeMillis()))
    }

    fun updateEvent(updatedEvent: Event) {
        val index = _events.indexOfFirst { it.id == updatedEvent.id }
        if (index != -1) {
            _events[index] = updatedEvent
        }
    }

    fun deleteEvent(eventId: Long) {
        _events.removeAll { it.id == eventId }
    }

    fun getEventsForDate(date: LocalDate): List<Event> {
        return _events.filter { it.date == date }
            .sortedWith(compareBy(
                { !it.isAllDay },
                { it.startTime ?: LocalTime.MIN }
            ))
    }

    fun getEventsForMonth(yearMonth: java.time.YearMonth): List<Event> {
        return _events.filter { 
            val eventDate = it.date
            eventDate.year == yearMonth.year && eventDate.month == yearMonth.month 
        }
    }
}