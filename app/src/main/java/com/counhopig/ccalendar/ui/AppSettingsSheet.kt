package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.counhopig.ccalendar.data.AppSettings

private val NeoBackground = Color(0xFFE9EEF6)
private val NeoSurface = Color(0xFFE9EEF6)
private val NeoText = Color(0xFF243044)
private val NeoTextMuted = Color(0xFF738099)
private val NeoAccent = Color(0xFF7C5CFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsSheet(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var draft by remember(settings) { mutableStateOf(settings) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = NeoBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NeoTextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "应用设置",
                color = NeoText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SettingsSection(title = "日历显示") {
                SettingRow(title = "一周开始于", description = "调整月视图和星期栏顺序") {
                    SegmentedChoice(
                        leftText = "周一",
                        rightText = "周日",
                        leftSelected = draft.weekStartsOnMonday,
                        onLeftClick = { draft = draft.copy(weekStartsOnMonday = true) },
                        onRightClick = { draft = draft.copy(weekStartsOnMonday = false) }
                    )
                }
                SettingSwitchRow(
                    title = "显示相邻月份日期",
                    description = "在月视图首尾显示上月/下月日期",
                    checked = draft.showAdjacentMonthDays,
                    onCheckedChange = { draft = draft.copy(showAdjacentMonthDays = it) }
                )
                SettingSwitchRow(
                    title = "显示日程颜色点",
                    description = "在日期下方显示当天日程提示",
                    checked = draft.showEventDots,
                    onCheckedChange = { draft = draft.copy(showEventDots = it) }
                )
            }

            SettingsSection(title = "时间与日程") {
                SettingSwitchRow(
                    title = "24 小时制",
                    description = "关闭后使用上午/下午时间显示",
                    checked = draft.use24HourTime,
                    onCheckedChange = { draft = draft.copy(use24HourTime = it) }
                )
                SettingRow(title = "默认提醒", description = "新建日程时自动选择") {
                    ReminderDropdown(
                        selectedMinutes = draft.defaultReminderMinutes,
                        onSelected = { draft = draft.copy(defaultReminderMinutes = it) }
                    )
                }
            }

            Button(
                onClick = { onSave(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = NeoAccent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("保存设置", color = Color.White)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = NeoTextMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            color = NeoSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(title = title, description = description) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = NeoText, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = NeoTextMuted, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

@Composable
private fun SegmentedChoice(
    leftText: String,
    rightText: String,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.46f), RoundedCornerShape(999.dp))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
            .padding(3.dp)
    ) {
        SegmentButton(leftText, leftSelected, onLeftClick)
        SegmentButton(rightText, !leftSelected, onRightClick)
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (selected) Color.White else NeoTextMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) NeoAccent else Color.Transparent, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ReminderDropdown(
    selectedMinutes: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = defaultReminderOptions.filterNot { it.isCustom }

    Column {
        Text(
            text = getReminderDisplayText(selectedMinutes),
            color = NeoAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.46f), RoundedCornerShape(999.dp))
                .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName, color = NeoText) },
                    onClick = {
                        onSelected(option.minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}
