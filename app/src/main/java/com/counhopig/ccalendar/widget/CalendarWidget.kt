package com.counhopig.ccalendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.counhopig.ccalendar.MainActivity
import com.counhopig.ccalendar.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_PREVIOUS_MONTH || action == ACTION_NEXT_MONTH) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val current = loadDate(context, appWidgetId)
                val newDate = if (action == ACTION_PREVIOUS_MONTH) {
                    current.minusMonths(1)
                } else {
                    current.plusMonths(1)
                }
                saveDate(context, appWidgetId, newDate)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid_view)
            }
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deleteDate(context, appWidgetId)
        }
    }

    companion object {
        const val ACTION_PREVIOUS_MONTH = "com.counhopig.ccalendar.ACTION_PREVIOUS_MONTH"
        const val ACTION_NEXT_MONTH = "com.counhopig.ccalendar.ACTION_NEXT_MONTH"
        
        private const val PREFS_NAME = "com.counhopig.ccalendar.widget.CalendarWidget"
        private const val PREF_PREFIX_KEY = "appwidget_"

        internal fun saveDate(context: Context, appWidgetId: Int, date: LocalDate) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_year", date.year)
            prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_month", date.monthValue)
            prefs.apply()
        }

        internal fun loadDate(context: Context, appWidgetId: Int): LocalDate {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            val year = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_year", -1)
            val month = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_month", -1)
            return if (year != -1 && month != -1) {
                LocalDate.of(year, month, 1)
            } else {
                LocalDate.now()
            }
        }
        
        internal fun deleteDate(context: Context, appWidgetId: Int) {
             val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
             prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_year")
             prefs.remove(PREF_PREFIX_KEY + appWidgetId + "_month")
             prefs.apply()
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
    
    val dateInfo = CalendarWidget.loadDate(context, appWidgetId)

    // Set header
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    
    // Capitalize the month name because some Locales might be lowercase
    val formattedDate = dateInfo.format(formatter)
    views.setTextViewText(R.id.widget_header, formattedDate)

    // Click header to open app
    val appIntent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        appIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_header, pendingIntent)

    // Previous button
    val prevIntent = Intent(context, CalendarWidget::class.java).apply {
        action = CalendarWidget.ACTION_PREVIOUS_MONTH
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val prevPending = PendingIntent.getBroadcast(
        context, 
        appWidgetId, // Use ID as rc to keep distinct per widget
        prevIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

    // Next button
    val nextIntent = Intent(context, CalendarWidget::class.java).apply {
        action = CalendarWidget.ACTION_NEXT_MONTH
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val nextPending = PendingIntent.getBroadcast(
        context, 
        appWidgetId + 10000, 
        nextIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

    // Template intent for the grid items
    val dayIntent = Intent(context, MainActivity::class.java)
    val dayPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId, 
        dayIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    views.setPendingIntentTemplate(R.id.widget_grid_view, dayPendingIntent)

    // Set up GridView
    val intent = Intent(context, CalendarWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }
    views.setRemoteAdapter(R.id.widget_grid_view, intent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid_view)
}
