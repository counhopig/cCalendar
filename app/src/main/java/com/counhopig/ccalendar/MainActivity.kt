package com.counhopig.ccalendar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
    private var showExplanation by mutableStateOf(true)

    private fun checkPermissions() {
        val hasReadPermission = checkSelfPermission(Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val newHasPermissions = hasReadPermission && hasWritePermission
        if (newHasPermissions && !hasPermissions) {
            showExplanation = false
        }
        hasPermissions = newHasPermissions
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()

        // Parse SELECTED_DATE from intent if present
        var selectedDate: LocalDate? = null
        val dateStr = intent.getStringExtra("SELECTED_DATE")
        if (!dateStr.isNullOrEmpty()) {
            try {
                selectedDate = LocalDate.parse(dateStr)
            } catch (e: Exception) {
                // Ignore malformed date string
            }
        }

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
                          MonthScreen(initialSelectedDate = selectedDate)
                      } else {
                         if (showExplanation) {
                             PermissionExplanationScreen(
                                 onContinue = {
                                     showExplanation = false
                                     permissionsLauncher.launch(
                                         arrayOf(
                                             Manifest.permission.READ_CALENDAR,
                                             Manifest.permission.WRITE_CALENDAR
                                         )
                                     )
                                 }
                             )
                         } else {
                             PermissionsNotGrantedContent(
                                 onRequestPermissions = {
                                     permissionsLauncher.launch(
                                         arrayOf(
                                             Manifest.permission.READ_CALENDAR,
                                             Manifest.permission.WRITE_CALENDAR
                                         )
                                     )
                                 },
                                 onOpenSettings = {
                                     val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                         data = android.net.Uri.fromParts("package", packageName, null)
                                         addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                     }
                                     startActivity(intent)
                                 }
                             )
                         }
                     }
                }
            }
        }


    }
}

@Composable
fun PermissionsNotGrantedContent(
    onRequestPermissions: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Calendar permissions are required to use this app.")
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(onClick = onRequestPermissions) {
                    Text("请求权限")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onOpenSettings) {
                    Text("打开设置")
                }
            }
        }
    }
}

@Composable
fun PermissionExplanationScreen(
    onContinue: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text = "欢迎使用日历应用", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "为了提供完整的日历功能，我们需要访问您的日历权限。这将允许您查看、创建和编辑日历事件。")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "我们尊重您的隐私，您的日历数据仅用于本地显示，不会上传到任何服务器。")
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onContinue) {
                Text("继续并授权")
            }
        }
    }
}