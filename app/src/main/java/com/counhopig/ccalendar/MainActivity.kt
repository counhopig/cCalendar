package com.counhopig.ccalendar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.counhopig.ccalendar.ui.MonthScreen
import com.counhopig.ccalendar.ui.theme.CCalendarTheme

class MainActivity : ComponentActivity() {

    private var hasPermissions by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    hasPermissions = true
                }
            }

        setContent {
            CCalendarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (hasPermissions) {
                        MonthScreen()
                    } else {
                        PermissionsNotGrantedContent()
                    }
                }
            }
        }

        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )
    }
}

@Composable
fun PermissionsNotGrantedContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Calendar permissions are required to use this app.")
    }
}