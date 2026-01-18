package com.counhopig.ccalendar.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.counhopig.ccalendar.R
import java.time.LocalDate
import java.time.YearMonth

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CalendarRemoteViewsFactory(this.applicationContext)
    }
}

class CalendarRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val days = mutableListOf<String>()
    
    override fun onCreate() {
        loadDays()
    }

    override fun onDataSetChanged() {
        loadDays()
    }

    private fun loadDays() {
        days.clear()
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val first = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        
        // Monday=1 ... Sunday=7
        // Grid starts Monday.
        // Offset = dayOfWeek - 1.
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
             val today = LocalDate.now()
             if (day == today.dayOfMonth) {
                 views.setTextColor(R.id.cell_day_text, Color.WHITE)
                 // Simple indication of today: bold color
             } else {
                 views.setTextColor(R.id.cell_day_text, Color.parseColor("#EAF0FF"))
             }
        } else {
             views.setTextColor(R.id.cell_day_text, Color.TRANSPARENT)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
