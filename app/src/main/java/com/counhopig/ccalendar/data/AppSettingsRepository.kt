package com.counhopig.ccalendar.data

import android.content.Context

data class AppSettings(
    val weekStartsOnMonday: Boolean = true,
    val showAdjacentMonthDays: Boolean = true,
    val showEventDots: Boolean = true,
    val use24HourTime: Boolean = true,
    val defaultReminderMinutes: Int = 0
)

class AppSettingsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getSettings(): AppSettings {
        return AppSettings(
            weekStartsOnMonday = prefs.getBoolean("week_starts_on_monday", true),
            showAdjacentMonthDays = prefs.getBoolean("show_adjacent_month_days", true),
            showEventDots = prefs.getBoolean("show_event_dots", true),
            use24HourTime = prefs.getBoolean("use_24_hour_time", true),
            defaultReminderMinutes = prefs.getInt("default_reminder_minutes", 0)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putBoolean("week_starts_on_monday", settings.weekStartsOnMonday)
            .putBoolean("show_adjacent_month_days", settings.showAdjacentMonthDays)
            .putBoolean("show_event_dots", settings.showEventDots)
            .putBoolean("use_24_hour_time", settings.use24HourTime)
            .putInt("default_reminder_minutes", settings.defaultReminderMinutes)
            .apply()
    }
}
