package com.counhopig.ccalendar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.counhopig.ccalendar.R
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
    
    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        loadDays()
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
    }

    override fun onDestroy() {}

    override fun getCount(): Int = days.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_day_cell)
        if (position >= days.size) return views 
        
        val text = days[position]
        views.setTextViewText(R.id.cell_day_text, text)
        
        if (text.isNotEmpty()) {
             val day = text.toInt()
             
             // Get the displayed month/year
             val displayedDate = CalendarWidget.loadDate(context, appWidgetId)
             val today = LocalDate.now()
             
             // Check if this cell represents "Today"
             if (today.year == displayedDate.year && 
                 today.month == displayedDate.month && 
                 today.dayOfMonth == day) {
                 
                 views.setTextColor(R.id.cell_day_text, Color.WHITE)
                 views.setInt(R.id.cell_day_text, "setBackgroundResource", R.drawable.widget_today_bg)
             } else {
                 views.setTextColor(R.id.cell_day_text, Color.parseColor("#EAF0FF"))
                 views.setInt(R.id.cell_day_text, "setBackgroundResource", 0)
             }
             
             // Fill in intent to open app on specific day
             val date = LocalDate.of(displayedDate.year, displayedDate.month, day)
             val fillInIntent = Intent().apply {
                 putExtra("SELECTED_DATE", date.toString())
             }
             views.setOnClickFillInIntent(R.id.cell_day_text, fillInIntent)
             
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
