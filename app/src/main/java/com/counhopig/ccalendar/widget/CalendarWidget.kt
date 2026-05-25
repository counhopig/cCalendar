package com.counhopig.ccalendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.RemoteViews
import com.counhopig.ccalendar.MainActivity
import com.counhopig.ccalendar.R
import com.counhopig.ccalendar.data.WidgetSettingsRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class CalendarWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_PREV_MONTH = "com.counhopig.ccalendar.widget.action.PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.counhopig.ccalendar.widget.action.NEXT_MONTH"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val ACTION_PROVIDER_CHANGED = "android.intent.action.PROVIDER_CHANGED"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_PREV_MONTH, ACTION_NEXT_MONTH -> {
                val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val repository = WidgetSettingsRepository(context)
                    val currentOffset = repository.getMonthOffset(appWidgetId)
                    val newOffset = when (intent.action) {
                        ACTION_PREV_MONTH -> currentOffset - 1
                        ACTION_NEXT_MONTH -> currentOffset + 1
                        else -> currentOffset
                    }
                    repository.setMonthOffset(appWidgetId, newOffset)
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_PROVIDER_CHANGED -> {
                // Refresh all widgets when system calendar changes
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, CalendarWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val repository = WidgetSettingsRepository(context)
    val settings = repository.getSettings()

    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
    
    // Get month offset for this widget
    val monthOffset = repository.getMonthOffset(appWidgetId)
    val targetMonth = YearMonth.now().plusMonths(monthOffset.toLong())
    
    // --- 1. Apply Background ---
    // Generate bitmap for background with corners and transparency
    val resources = context.resources
    // Assume a standard widget size or just a large enough bitmap that will scale fitXY.
    // However, fitXY for rounded corners might distort corners if aspect ratio doesn't match.
    // Best is to use a 9-patch or simple solid color with SDK-supported rounded corners.
    // Since we want ANY color, 9-patch is hard.
    // But since API 31, we can use system_app_widget_background_radius naturally.
    // For manual radius control, we create a Bitmap.
    // Warning: Creating very large bitmaps in AppWidgetProvider can crash (TransactionTooLargeException).
    // A 4x4 widget on high DPI can be large.
    // We can just create a small bitmap and let it stretch, but corners will stretch.
    // Better: Create a bitmap of size e.g. 400x400 (sufficient for average widget aspect) or try to adhere to widget options.
    // A 9-patch created at runtime? No support for run-time 9-patch creation easily.
    // Let's create a bitmap of size 1000x1000 so corners look decent?
    // Or just use `setImageViewBitmap` with a modest size. 
    // Since fitXY scales everything, the corner radius will also scale.
    // To keep corner radius constant (e.g. 16dp), we shouldn't scale.
    // But widget size is variable.
    // The "Proper" way is usually to use `GradientDrawable` shape and `setTint` but standard RemoteViews doesn't support changing GradientDrawable radius dynamically (only color via setTint).
    // Wait! `RemoteViews` on S+ supports `setViewLayoutParam( ... setBackgroundResource ... )`? No.
    // However, most modern widgets use `R.drawable.widget_background` (shape) and rely on `system_app_widget_background_radius` for consistency.
    // The user wants CUSTOM radius.
    // Let's accept that corners might stretch a bit if we assume 1:1, or we just generate a bitmap that is "big enough" to look like a solid rect.
    // But scaling distorts corners.
    // If we use `ImageView` with `src` set to a specific color, we can't clip it easily.
    // Let's rely on standard practice: Use a shape drawable. If user wants custom radius, we can define a few shapes (small, medium, large) and swap `setBackgroundResource`.
    // But user asked for slider.
    // Okay, simple approach: Create a Bitmap.
    // To minimize distortion, we can read actual widget size from options?
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
    // Convert dp to px
    val density = resources.displayMetrics.density
    val widthPx = ((if (minWidth > 0) minWidth else 300) * density).toInt()
    val heightPx = ((if (minHeight > 0) minHeight else 300) * density).toInt()
    
    // Safeguard size
    val safeW = widthPx.coerceIn(100, 1200)
    val safeH = heightPx.coerceIn(100, 1200)

    val bgBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bgBitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        color = repository.getEffectiveBackgroundColor(settings)
        style = Paint.Style.FILL
    }
    // Radius in px
    val radiusPx = settings.cornerRadius * density
    canvas.drawRoundRect(RectF(0f, 0f, safeW.toFloat(), safeH.toFloat()), radiusPx, radiusPx, paint)
    
    views.setImageViewBitmap(R.id.widget_background_image, bgBitmap)


    // --- 2. Apply Text Colors ---
    val fontColor = settings.fontColor
    views.setTextColor(R.id.widget_header, fontColor)
    views.setTextColor(R.id.widget_header_sun, fontColor)
    views.setTextColor(R.id.widget_header_mon, fontColor)
    views.setTextColor(R.id.widget_header_tue, fontColor)
    views.setTextColor(R.id.widget_header_wed, fontColor)
    views.setTextColor(R.id.widget_header_thu, fontColor)
    views.setTextColor(R.id.widget_header_fri, fontColor)
    views.setTextColor(R.id.widget_header_sat, fontColor)
    views.setTextColor(R.id.widget_prev_month, fontColor)
    views.setTextColor(R.id.widget_next_month, fontColor)


    // Set header with target month
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    val formattedDate = targetMonth.format(formatter)
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

    // Previous month button
    val prevIntent = Intent(context, CalendarWidget::class.java).apply {
        action = CalendarWidget.ACTION_PREV_MONTH
        putExtra(CalendarWidget.EXTRA_WIDGET_ID, appWidgetId)
    }
    val prevPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId * 10 + 1,
        prevIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_prev_month, prevPendingIntent)

    // Next month button
    val nextIntent = Intent(context, CalendarWidget::class.java).apply {
        action = CalendarWidget.ACTION_NEXT_MONTH
        putExtra(CalendarWidget.EXTRA_WIDGET_ID, appWidgetId)
    }
    val nextPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId * 10 + 2,
        nextIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_next_month, nextPendingIntent)

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
        putExtra("FONT_COLOR", fontColor) // Pass settings to service
        putExtra("MONTH_OFFSET", monthOffset) // Pass month offset to service
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }
    views.setRemoteAdapter(R.id.widget_grid_view, intent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid_view)
}
