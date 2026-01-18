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

    init {
        // 添加一些示例事件
        addSampleEvents()
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

    private fun addSampleEvents() {
        val today = LocalDate.now()
        
        // 今天的事件
        addEvent(Event(
            title = "团队会议",
            description = "每周项目进度同步",
            date = today,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 30),
            color = androidx.compose.ui.graphics.Color(0xFF7C5CFF),
            reminderMinutes = 15
        ))

        addEvent(Event(
            title = "健身房",
            description = "力量训练",
            date = today,
            startTime = LocalTime.of(18, 0),
            endTime = LocalTime.of(19, 30),
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            isAllDay = false
        ))

        // 明天的全天事件
        addEvent(Event(
            title = "生日派对",
            description = "朋友的生日庆祝",
            date = today.plusDays(1),
            isAllDay = true,
            color = androidx.compose.ui.graphics.Color(0xFFFF9800)
        ))

        // 后天的事件
        addEvent(Event(
            title = "牙医预约",
            description = "定期检查",
            date = today.plusDays(2),
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 15),
            color = androidx.compose.ui.graphics.Color(0xFF2196F3),
            reminderMinutes = 60
        ))

        // 上周的事件（演示用）
        addEvent(Event(
            title = "项目交付",
            description = "完成最终版本",
            date = today.minusDays(7),
            isAllDay = true,
            color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
        ))
    }
}