package com.counhopig.ccalendar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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

private val PermissionBackground = Brush.verticalGradient(
    listOf(
        Color(0xFFF8FAFE),
        Color(0xFFE9EEF6),
        Color(0xFFDDE5F0)
    )
)
private val PermissionSurfaceLight = Color(0xFFF8FAFE)
private val PermissionText = Color(0xFF243044)
private val PermissionTextMuted = Color(0xFF738099)
private val PermissionAccent = Color(0xFF7C5CFF)
private val PermissionAccentDeep = Color(0xFF5C6BF2)
private val PermissionAccentSoft = Color(0xFFE2DEFF)

@Composable
fun PermissionsNotGrantedContent(
    onRequestPermissions: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    PermissionGateContent(
        badge = "授权未完成",
        title = "还差一步，才能使用日历",
        description = "cCalendar 需要日历权限来读取、创建和编辑系统日程。授权后，你的日程会在本机展示和管理。",
        primaryActionText = "重新请求权限",
        onPrimaryAction = onRequestPermissions,
        secondaryActionText = "打开系统设置",
        onSecondaryAction = onOpenSettings,
        footerText = "如果系统不再弹出授权窗口，请在设置中手动允许日历权限。"
    )
}

@Composable
fun PermissionExplanationScreen(
    onContinue: () -> Unit = {}
) {
    PermissionGateContent(
        badge = "本地日历权限",
        title = "让 cCalendar 接入你的日程",
        description = "授权后可以查看日历事件、添加新日程，并同步更新桌面小组件。所有日历数据只在本机使用。",
        primaryActionText = "继续并授权",
        onPrimaryAction = onContinue,
        secondaryActionText = null,
        onSecondaryAction = null,
        footerText = "你可以随时在系统设置中关闭权限。"
    )
}

@Composable
private fun PermissionGateContent(
    badge: String,
    title: String,
    description: String,
    primaryActionText: String,
    onPrimaryAction: () -> Unit,
    secondaryActionText: String?,
    onSecondaryAction: (() -> Unit)?,
    footerText: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PermissionBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PermissionTopBar(badge)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color.White.copy(alpha = 0.58f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    PermissionHeroIcon()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = title,
                            color = PermissionText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = description,
                            color = PermissionTextMuted,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }

                    PermissionBenefitRow(
                        icon = Icons.Default.CalendarMonth,
                        title = "读取日程",
                        description = "在月视图中显示系统日历事件。"
                    )
                    PermissionBenefitRow(
                        icon = Icons.Default.Edit,
                        title = "创建和编辑",
                        description = "直接在应用内新增、修改日程。"
                    )
                    PermissionBenefitRow(
                        icon = Icons.Default.Lock,
                        title = "本机处理",
                        description = "日历数据仅用于本地展示和小组件刷新。"
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PermissionAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(primaryActionText, fontWeight = FontWeight.SemiBold)
                }

                if (secondaryActionText != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PermissionText),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(secondaryActionText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(
                text = footerText,
                color = PermissionTextMuted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionTopBar(badge: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = "cCalendar",
                color = PermissionText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "日程清晰一点，生活轻一点",
                color = PermissionTextMuted,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.46f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.76f))
        ) {
            Text(
                text = badge,
                color = PermissionAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionHeroIcon() {
    Box(
        modifier = Modifier
            .size(78.dp)
            .background(
                Brush.verticalGradient(listOf(PermissionAccent, PermissionAccentDeep)),
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun PermissionBenefitRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PermissionSurfaceLight.copy(alpha = 0.68f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = PermissionAccentSoft
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PermissionAccent,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = PermissionText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = PermissionTextMuted,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 21.sp
            )
        }
    }
}
