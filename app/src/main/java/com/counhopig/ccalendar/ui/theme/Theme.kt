package com.counhopig.ccalendar.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8D7BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF7DD3FC),
    tertiary = Color(0xFFF0ABFC),
    background = Color(0xFF08111F),
    onBackground = Color(0xFFEAF0FF),
    surface = Color(0xFF0F1B33),
    onSurface = Color(0xFFEAF0FF),
    surfaceVariant = Color(0xFF17233B),
    onSurfaceVariant = Color(0xFFB7C4E6),
    outline = Color(0xFF3B4761)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6F55F2),
    onPrimary = Color.White,
    secondary = Color(0xFF5A6D8C),
    tertiary = Color(0xFFB56DDA),
    background = Color(0xFFE9EEF6),
    onBackground = Color(0xFF243044),
    surface = Color(0xFFE9EEF6),
    onSurface = Color(0xFF243044),
    surfaceVariant = Color(0xFFDDE5F0),
    onSurfaceVariant = Color(0xFF738099),
    outline = Color(0xFFD0D8E6)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun CCalendarTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
