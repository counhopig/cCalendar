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
import com.counhopig.ccalendar.ui.model.Event
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

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
    private val days = mutableListOf<String>()
    
    // Store daily events in a map
    private val eventsMap = mutableMapOf<LocalDate, List<Event>>()
    private val repository = SystemCalendarRepository(context)
    
    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        loadDays()
        loadEvents()
    }

    private fun loadEvents() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) 
            == PackageManager.PERMISSION_GRANTED) {
            
            val allEvents = repository.getSystemEvents()
            
            // Sort events to keep consistent slots
            // Sorting strategy: AllDay first, then longer duration first, then start time, then title, then ID.
            // But we need duration. Event doesn't have duration directly exposed in my Event class.
            // Let's rely on StartTime. AllDay events have null startTime, treated as very start.
            // But to connect bars, stability is most important. 
            // If we sort by Title or ID, it's stable across days.
            // Let's sort by Title for visual consistency of same-named events.
            val sortedEvents = allEvents.sortedWith(compareBy(
                { !it.isAllDay }, // All day first (true < false is false... wait. isAllDay=true => !isAllDay=false. false < true. So AllDay first)
                { it.startTime ?: LocalTime.MIN },
                { it.title }
            ))

            eventsMap.clear()
            sortedEvents.forEach { event ->
                val list = eventsMap.getOrPut(event.date) { mutableListOf() }
                (list as MutableList).add(event)
            }
        }
    }

    private fun loadDays() {
        days.clear()
        
        // Always show current month
        val dateInfo = LocalDate.now()
        val month = YearMonth.from(dateInfo)
        
        val first = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        
        // Monday=1 ... Sunday=7
        // Offset = dayOfWeek - 1. (Mon->0, Tue->1...)
        val offset = first.dayOfWeek.value - 1

        for (i in 0 until offset) {
            days.add("")
        }
        for (i in 1..daysInMonth) {
            days.add(i.toString())
        }
        
        // Fill remaining cells to keep grid stable if needed
        val remaining = 42 - days.size // 6 rows * 7
        if (remaining > 0) {
             for (i in 0 until remaining) {
                 days.add("") // Or next month's days
             }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = days.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_day_cell)
        if (position >= days.size) return views 
        
        val text = days[position]
        views.setTextViewText(R.id.cell_day_text, text)
        
        // Clear previous states
        views.setViewVisibility(R.id.cell_event_1, View.GONE)
        views.setViewVisibility(R.id.cell_event_2, View.GONE)
        views.setViewVisibility(R.id.cell_more_text, View.GONE)
        
        if (text.isNotEmpty()) {
             val day = try { text.toInt() } catch(e: Exception) { -1 }
             if (day == -1) return views

             // Get the displayed month/year
             val displayedDate = LocalDate.now()
             val cellDate = LocalDate.of(displayedDate.year, displayedDate.month, day)
             val today = LocalDate.now()
             
             // Check if this cell represents "Today"
             if (today == cellDate) {
                 views.setTextColor(R.id.cell_day_text, Color.WHITE)
                 views.setInt(R.id.cell_day_text, "setBackgroundResource", R.drawable.widget_today_bg)
             } else {
                 views.setTextColor(R.id.cell_day_text, Color.parseColor("#EAF0FF"))
                 views.setInt(R.id.cell_day_text, "setBackgroundResource", 0)
             }
             
             // --- Load Events ---
             val dailyEvents = eventsMap[cellDate]?.take(2) // Only show top 2 to fit
             val allDailyEvents = eventsMap[cellDate] ?: emptyList()
             
             if (allDailyEvents.isNotEmpty()) {
                 // Function to setup event view
                 fun setupEventView(event: Event, bgParams: Pair<Int, Int>, textId: Int, bgId: Int, containerId: Int) {
                     views.setViewVisibility(containerId, View.VISIBLE)
                     views.setTextViewText(textId, event.title)
                     
                     // Check neighbors for connection
                     // We need to check if there is an event with SAME ID on prev/next day
                     val prevDay = cellDate.minusDays(1)
                     val nextDay = cellDate.plusDays(1)
                     
                     val prevEvents = eventsMap[prevDay] ?: emptyList()
                     val nextEvents = eventsMap[nextDay] ?: emptyList()
                     
                     val continuesLeft = prevEvents.any { it.id == event.id }
                     val continuesRight = nextEvents.any { it.id == event.id }
                     
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

                 if (allDailyEvents.isNotEmpty()) {
                     setupEventView(allDailyEvents[0], 0 to 0, R.id.cell_event_1_text, R.id.cell_event_1_bg, R.id.cell_event_1)
                 }
                 
                 if (allDailyEvents.size > 1) {
                     setupEventView(allDailyEvents[1], 0 to 0, R.id.cell_event_2_text, R.id.cell_event_2_bg, R.id.cell_event_2)
                 }

                 if (allDailyEvents.size > 2) {
                     views.setViewVisibility(R.id.cell_more_text, View.VISIBLE)
                 }
             }

             // Fill in intent to open app on specific day
             val dateStr = cellDate.toString()
             val fillInIntent = Intent().apply {
                 putExtra("SELECTED_DATE", dateStr)
             }
             views.setOnClickFillInIntent(R.id.cell_root, fillInIntent)
             
        } else {
             views.setTextColor(R.id.cell_day_text, Color.TRANSPARENT)
             views.setInt(R.id.cell_day_text, "setBackgroundResource", 0)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
