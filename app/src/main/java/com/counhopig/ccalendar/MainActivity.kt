package com.counhopig.ccalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.counhopig.ccalendar.ui.theme.CCalendarTheme
import com.counhopig.ccalendar.ui.MonthScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CCalendarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MonthScreen()
                }
            }
        }
    }
}