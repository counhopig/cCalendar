package com.counhopig.ccalendar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.counhopig.ccalendar.R
import com.counhopig.ccalendar.data.SystemCalendarRepository
import com.counhopig.ccalendar.data.WidgetSettingsRepository
import com.counhopig.ccalendar.ui.model.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

private data class WidgetDayCell(
    val date: LocalDate,
    val isTargetMonth: Boolean
)

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CalendarRemoteViewsFactory(this.applicationContext, intent)
    }
}

class CalendarRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private val monthOffset = intent.getIntExtra("MONTH_OFFSET", 0)
    private val days = mutableListOf<WidgetDayCell>()
    
    // Store daily events in a map
    private val eventsMap = mutableMapOf<LocalDate, List<Event>>()
    private val repository = SystemCalendarRepository(context)
    private val settingsRepo = WidgetSettingsRepository(context)
    private var fontColor = android.graphics.Color.parseColor("#EAF0FF") // Default
    
    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        // Reload settings
        val settings = settingsRepo.getSettings()
        fontColor = settings.fontColor
        
        loadDays()
        loadEvents()
    }

    private fun loadEvents() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) 
            == PackageManager.PERMISSION_GRANTED) {
            
            val calendars = repository.getCalendars()
            val calendarIds = calendars.map { it.id }.toSet()
            val yearMonth = YearMonth.now().plusMonths(monthOffset.toLong())

            val allEvents = repository.getSystemEvents(calendarIds, yearMonth)
            
            // Sort events to keep consistent slots, prioritizing multi-day events
            // Build map of event ID to occurrence count
            val eventIdToCount = allEvents.groupBy { it.id }.mapValues { it.value.size }
            
            val sortedEvents = allEvents.sortedWith(compareByDescending<Event> { eventIdToCount[it.id] ?: 0 }
                .thenBy { !it.isAllDay } // All day first
                .thenBy { it.startTime ?: LocalTime.MIN }
                .thenBy { it.title }
            )

            eventsMap.clear()
            sortedEvents.forEach { event ->
                val list = eventsMap.getOrPut(event.date) { mutableListOf() }
                (list as MutableList).add(event)
            }
        }
    }

    private fun loadDays() {
        days.clear()
        
        // Show month based on offset
        val month = YearMonth.now().plusMonths(monthOffset.toLong())
        
        val first = month.atDay(1)
        
        // Sunday=7, Monday=1 ... Saturday=6
        // Grid starts at Sunday.
        // If first is Sunday(7), offset = 0.
        // If first is Monday(1), offset = 1.
        val offset = first.dayOfWeek.value % 7
        val firstVisibleDate = first.minusDays(offset.toLong())

        repeat(42) { index ->
            val cellDate = firstVisibleDate.plusDays(index.toLong())
            days.add(WidgetDayCell(cellDate, YearMonth.from(cellDate) == month))
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = days.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_day_cell)
        if (position >= days.size) return views 
        
        val cell = days[position]
        val cellDate = cell.date
        val text = cellDate.dayOfMonth.toString()
        views.setTextViewText(R.id.cell_day_text, text)
        
        // Clear previous states
        views.setViewVisibility(R.id.cell_event_1, View.GONE)
        views.setViewVisibility(R.id.cell_event_2, View.GONE)
        views.setViewVisibility(R.id.cell_more_text, View.GONE)
        
        val today = LocalDate.now()
             
        // Check if this cell represents "Today"
        if (today == cellDate) {
            views.setTextColor(R.id.cell_day_text, Color.WHITE) // Keep Today White for visibility on highlight
            views.setInt(R.id.cell_day_text, "setBackgroundResource", R.drawable.widget_today_bg)
        } else {
            views.setTextColor(
                R.id.cell_day_text,
                if (cell.isTargetMonth) fontColor else fontColor.withAlpha(0.36f)
            )
            views.setInt(R.id.cell_day_text, "setBackgroundResource", 0)
        }
             
        // --- Load Events ---
        val allDailyEvents = if (cell.isTargetMonth) eventsMap[cellDate] ?: emptyList() else emptyList()
             
        if (allDailyEvents.isNotEmpty()) {
            // Function to setup event view
            fun setupEventView(event: Event, textId: Int, bgId: Int, containerId: Int) {
                views.setViewVisibility(containerId, View.VISIBLE)
                views.setTextViewText(textId, event.title)
                     
                // Check neighbors for connection (visual shape)
                val prevDay = cellDate.minusDays(1)
                val nextDay = cellDate.plusDays(1)
                     
                val prevEvents = eventsMap[prevDay] ?: emptyList()
                val nextEvents = eventsMap[nextDay] ?: emptyList()
                     
                val continuesLeft = prevEvents.any { it.id == event.id }
                val continuesRight = nextEvents.any { it.id == event.id }
                     
                // Text logic: Show title only on start or if disconnected from left
                val showText = !continuesLeft
                views.setTextViewText(textId, if (showText) event.title else "")
                     
                val bgRes = when {
                    continuesLeft && continuesRight -> R.drawable.shape_event_item_middle
                    continuesLeft && !continuesRight -> R.drawable.shape_event_item_end
                    !continuesLeft && continuesRight -> R.drawable.shape_event_item_start
                    else -> R.drawable.shape_event_item_single
                }
                     
                views.setImageViewResource(bgId, bgRes)
                val c = event.color.toArgb()
                views.setInt(bgId, "setColorFilter", c)

                // If middle/end, maybe hide text or keep it?
                // Usually repeating text is fine, but maybe redundant if it's very short.
                // The requirement is "connected color block".
                // Text color - ensure contrast. If color is dark, text white. If light, text black.
                // Simple heuristic: default white text for calendar colors which are usually distinct.
            }

            setupEventView(allDailyEvents[0], R.id.cell_event_1_text, R.id.cell_event_1_bg, R.id.cell_event_1)
                 
            if (allDailyEvents.size > 1) {
                setupEventView(allDailyEvents[1], R.id.cell_event_2_text, R.id.cell_event_2_bg, R.id.cell_event_2)
            }

            if (allDailyEvents.size > 2) {
                views.setViewVisibility(R.id.cell_more_text, View.VISIBLE)
                val moreCount = allDailyEvents.size - 2
                views.setTextViewText(R.id.cell_more_text, "+$moreCount")
            }
        }

        // Fill in intent to open app on specific day
        val fillInIntent = Intent().apply {
            putExtra("SELECTED_DATE", cellDate.toString())
        }
        views.setOnClickFillInIntent(R.id.cell_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}

private fun Int.withAlpha(alpha: Float): Int {
    val coercedAlpha = (Color.alpha(this) * alpha).toInt().coerceIn(0, 255)
    return Color.argb(coercedAlpha, Color.red(this), Color.green(this), Color.blue(this))
}
