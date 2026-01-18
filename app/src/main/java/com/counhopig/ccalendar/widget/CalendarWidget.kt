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
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
    
    val dateInfo = LocalDate.now()

    // Set header
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
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
