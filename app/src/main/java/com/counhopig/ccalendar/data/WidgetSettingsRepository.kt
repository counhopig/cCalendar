package com.counhopig.ccalendar.data

import android.content.Context
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class WidgetSettings(
    @ColorInt val fontColor: Int = Color(0xFFEAF0FF).toArgb(),
    @ColorInt val backgroundColor: Int = Color(0xFF0B1220).toArgb(),
    val backgroundTransparency: Float = 1.0f, // 0.0 to 1.0. Applied to backgroundColor alpha.
    val cornerRadius: Int = 16 // dp
)

class WidgetSettingsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)

    fun getSettings(): WidgetSettings {
        val fontColor = prefs.getInt("font_color", Color(0xFFEAF0FF).toArgb())
        val bgColor = prefs.getInt("background_color", Color(0xFF0B1220).toArgb())
        val bgAlpha = prefs.getFloat("background_alpha", 1.0f)
        val radius = prefs.getInt("corner_radius", 16)
        return WidgetSettings(fontColor, bgColor, bgAlpha, radius)
    }

    fun saveSettings(settings: WidgetSettings) {
        prefs.edit().apply {
            putInt("font_color", settings.fontColor)
            putInt("background_color", settings.backgroundColor)
            putFloat("background_alpha", settings.backgroundTransparency)
            putInt("corner_radius", settings.cornerRadius)
            apply()
        }
    }
    
    // Helper to get effective background color with alpha
    fun getEffectiveBackgroundColor(settings: WidgetSettings): Int {
        val color = Color(settings.backgroundColor)
        return color.copy(alpha = settings.backgroundTransparency).toArgb()
    }
}
