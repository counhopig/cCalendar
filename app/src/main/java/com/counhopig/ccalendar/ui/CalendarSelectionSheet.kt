package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.counhopig.ccalendar.ui.model.Calendar
import com.counhopig.ccalendar.ui.viewmodel.EventViewModel

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.*

private val NeoBackground = Color(0xFFE9EEF6)
private val NeoSurface = Color(0xFFE9EEF6)
private val NeoText = Color(0xFF243044)
private val NeoTextMuted = Color(0xFF738099)
private val NeoAccent = Color(0xFF7C5CFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSelectionSheet(
    viewModel: EventViewModel,
    onDismiss: () -> Unit,
    onImportIcs: () -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeoBackground,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val groupedCalendars = remember(viewModel.calendars.toList()) {
                viewModel.calendars
                    .groupBy { it.accountKey }
                    .toList()
                    .sortedBy { it.first.label }
            }
            val selectedCount = viewModel.selectedCalendarIds.size
            val totalCount = viewModel.calendars.size

            CalendarManagerHeader(
                selectedCount = selectedCount,
                totalCount = totalCount,
                onSelectAll = { viewModel.setCalendarSelection(viewModel.calendars.map { it.id }.toSet()) },
                onClear = { viewModel.setCalendarSelection(emptySet()) }
            )
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedCalendars, key = { it.first.label }) { (account, calendars) ->
                    AccountCalendarGroup(
                        account = account,
                        calendars = calendars,
                        selectedIds = viewModel.selectedCalendarIds,
                        onSelectAccount = {
                            viewModel.setCalendarSelection(viewModel.selectedCalendarIds + calendars.map { it.id })
                        },
                        onClearAccount = {
                            viewModel.setCalendarSelection(viewModel.selectedCalendarIds - calendars.map { it.id }.toSet())
                        },
                        onToggle = { id -> viewModel.toggleCalendarSelection(id) },
                        onColorChange = { id, color -> viewModel.updateCalendarColor(id, color) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFD0D8E6))

            Button(
                onClick = onImportIcs,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = NeoAccent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("从 ICS 文件导入", color = Color.White)
            }
        }
    }
}

private data class CalendarAccount(
    val name: String,
    val type: String
) {
    val label: String
        get() = name.ifBlank {
            when {
                type.contains("local", ignoreCase = true) -> "本机日历"
                type.isBlank() -> "其他账号"
                else -> type.substringAfterLast('.')
            }
        }

    val subtitle: String
        get() = when {
            type.isBlank() -> "系统日历"
            type.contains("google", ignoreCase = true) -> "Google"
            type.contains("exchange", ignoreCase = true) -> "Exchange"
            type.contains("local", ignoreCase = true) -> "本机"
            else -> type.substringAfterLast('.')
        }
}

private val Calendar.accountKey: CalendarAccount
    get() = CalendarAccount(accountName, accountType)

@Composable
private fun CalendarManagerHeader(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "日历管理",
            color = NeoText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = NeoSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$selectedCount / $totalCount 个日历已显示", color = NeoText, fontWeight = FontWeight.SemiBold)
                    Text("按账号管理显示、隐藏和颜色", color = NeoTextMuted, fontSize = 12.sp)
                }
                CompactAction("全选", Icons.Default.Checklist, onSelectAll)
                CompactAction("清空", Icons.Default.VisibilityOff, onClear)
            }
        }
    }
}

@Composable
private fun CompactAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.48f))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NeoAccent, modifier = Modifier.size(16.dp))
        Text(text, color = NeoAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountCalendarGroup(
    account: CalendarAccount,
    calendars: List<Calendar>,
    selectedIds: Set<Long>,
    onSelectAccount: () -> Unit,
    onClearAccount: () -> Unit,
    onToggle: (Long) -> Unit,
    onColorChange: (Long, Color) -> Unit
) {
    val selectedInGroup = calendars.count { selectedIds.contains(it.id) }

    Surface(
        color = NeoSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.58f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NeoAccent)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.label, color = NeoText, fontWeight = FontWeight.Bold)
                    Text("${account.subtitle} · $selectedInGroup/${calendars.size} 已显示", color = NeoTextMuted, fontSize = 12.sp)
                }
                CompactAction("显示", Icons.Default.Checklist, onSelectAccount)
                CompactAction("隐藏", Icons.Default.VisibilityOff, onClearAccount)
            }

            calendars.forEachIndexed { index, calendar ->
                CalendarRow(
                    calendar = calendar,
                    isSelected = selectedIds.contains(calendar.id),
                    onToggle = { onToggle(calendar.id) },
                    onColorChange = { color -> onColorChange(calendar.id, color) }
                )
                if (index != calendars.lastIndex) {
                    HorizontalDivider(color = Color(0xFFD0D8E6).copy(alpha = 0.65f))
                }
            }
        }
    }
}

// Preset Colors available for calendars
val CalendarColors = listOf(
    Color(0xFFEF5350), // Tomato
    Color(0xFFF4511E), // Tangerine
    Color(0xFFF09300), // Pumpkin
    Color(0xFFE4C441), // Banana
    Color(0xFF7CB342), // Basil
    Color(0xFF0B8043), // Sage
    Color(0xFF039BE5), // Peacock
    Color(0xFF3F51B5), // Blueberry
    Color(0xFF7986CB), // Lavender
    Color(0xFF8E24AA), // Grape
    Color(0xFFD81B60), // Flamingo
    Color(0xFF616161)  // Graphite
)

@Composable
private fun CalendarRow(
    calendar: Calendar,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onColorChange: (Color) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(24.dp) // Slightly larger to be touchable
                    .clip(RoundedCornerShape(4.dp))
                    .background(calendar.color)
                    .clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = NeoSurface
            ) {
                 CalendarColors.chunked(4).forEach { rowColors ->
                     Row(modifier = Modifier.padding(8.dp)) {
                         rowColors.forEach { color ->
                             Box(
                                 modifier = Modifier
                                     .padding(4.dp)
                                     .size(32.dp)
                                     .clip(androidx.compose.foundation.shape.CircleShape)
                                     .background(color)
                                     .clickable {
                                         onColorChange(color)
                                         expanded = false
                                     }
                             )
                         }
                     }
                 }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        Text(
            text = calendar.name,
            color = NeoText,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = NeoAccent,
                uncheckedColor = NeoTextMuted
            )
        )
    }
}
