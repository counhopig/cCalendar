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
            
            // In a widget, loading all system events efficiently is key.
            // We'll trust the repository to load a range.
            // For better performance, we should ideally filter by the displayed month.
            // The current simple repository loads year +/- 1.
            val allEvents = repository.getSystemEvents()
            
            eventsMap.clear()
            allEvents.forEach { event ->
                val list = eventsMap.getOrPut(event.date) { mutableListOf() }
                (list as MutableList).add(event)
            }
        }
    }

    private fun loadDays() {
        days.clear()
        
        // Load the saved date for this widget, or default to now
        val dateInfo = CalendarWidget.loadDate(context, appWidgetId)
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
             val displayedDate = CalendarWidget.loadDate(context, appWidgetId)
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
             val dailyEvents = eventsMap[cellDate]?.take(3)
             
             if (!dailyEvents.isNullOrEmpty()) {
                 // Slot 1
                 if (dailyEvents.isNotEmpty()) {
                     val e1 = dailyEvents[0]
                     views.setViewVisibility(R.id.cell_event_1, View.VISIBLE)
                     views.setTextViewText(R.id.cell_event_1_text, e1.title)
                     
                     // Convert Color to int argb
                     val c = e1.color.toArgb()
                     views.setInt(R.id.cell_event_1_color, "setBackgroundColor", c)
                 }
                 
                 // Slot 2 or More
                 if (dailyEvents.size > 1) {
                     if (dailyEvents.size == 2) {
                        val e2 = dailyEvents[1]
                        views.setViewVisibility(R.id.cell_event_2, View.VISIBLE)
                        views.setTextViewText(R.id.cell_event_2_text, e2.title)
                        val c = e2.color.toArgb()
                        views.setInt(R.id.cell_event_2_color, "setBackgroundColor", c)
                     } else {
                         // More than 2 events, show dots
                         views.setViewVisibility(R.id.cell_more_text, View.VISIBLE)
                     }
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
