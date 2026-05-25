package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.counhopig.ccalendar.ui.model.Event
import com.counhopig.ccalendar.ui.viewmodel.EventViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import androidx.compose.material.icons.automirrored.filled.Subject
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.Duration

// 提醒选项数据类
data class ReminderOption(
    val id: Int,
    val minutes: Int,
    val displayName: String,
    val isCustom: Boolean = false
)

// 预定义提醒选项
val defaultReminderOptions = listOf(
    ReminderOption(0, 0, "不提醒"),
    ReminderOption(1, 5, "5分钟前"),
    ReminderOption(2, 15, "15分钟前"),
    ReminderOption(3, 30, "30分钟前"),
    ReminderOption(4, 60, "1小时前"),
    ReminderOption(5, 120, "2小时前"),
    ReminderOption(6, 1440, "1天前"),
    ReminderOption(7, 2880, "2天前"),
    ReminderOption(8, -1, "自定义...", isCustom = true)
)

// 辅助函数：获取显示文本
@Composable
fun getReminderDisplayText(minutes: Int, options: List<ReminderOption> = defaultReminderOptions): String {
    return options.find { it.minutes == minutes }?.displayName
        ?: if (minutes > 0) {
            // 自定义分钟数的显示
            when {
                minutes < 60 -> "${minutes}分钟前"
                minutes % 1440 == 0 -> "${minutes / 1440}天前"
                minutes % 60 == 0 -> "${minutes / 60}小时前"
                else -> "${minutes}分钟前"
            }
        } else {
            "不提醒"
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditSheet(
    event: Event?,
    selectedDate: LocalDate,
    viewModel: EventViewModel,
    onDismiss: () -> Unit,
    onSave: (Event, Long) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A2234),
        dragHandle = null
    ) {
        EventEditContent(
            event = event,
            selectedDate = selectedDate,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onSave = onSave,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditContent(
    event: Event?,
    selectedDate: LocalDate,
    viewModel: EventViewModel,
    onDismiss: () -> Unit,
    onSave: (Event, Long) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var isAllDay by remember { mutableStateOf(event?.isAllDay ?: false) }

    // Calendar state
    var selectedCalendarId by remember { mutableStateOf(event?.calendarId ?: viewModel.calendars.firstOrNull()?.id ?: 1L) }
    var showCalendarMenu by remember { mutableStateOf(false) }

    // Time & Date state
    var startDate by remember { mutableStateOf(event?.originalStartDate ?: selectedDate) }
    var endDate by remember { mutableStateOf(event?.originalEndDate ?: selectedDate) }
    var startTime by remember { mutableStateOf(event?.startTime ?: LocalTime.now().plusHours(1).withMinute(0)) }
    var endTime by remember { mutableStateOf(event?.endTime ?: LocalTime.now().plusHours(2).withMinute(0)) }

    // Dialog visibility states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    // Reminder state
    var selectedReminderMinutes by remember { mutableStateOf(event?.reminderMinutes ?: 0) }
    var showCustomReminderDialog by remember { mutableStateOf(false) }
    var customReminderValue by remember { mutableStateOf("") }
    var customReminderDate by remember { mutableStateOf(LocalDate.now()) }
    var customReminderTime by remember { mutableStateOf(LocalTime.now()) }

    val context = LocalContext.current
    val reminderOptions = defaultReminderOptions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .imePadding() // Handles keyboard
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                text = if (event == null) "添加日程" else "编辑日程",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            TextButton(
                onClick = {
                    val newEvent = Event(
                        id = event?.id ?: 0,
                        title = title.ifBlank { "无标题" },
                        description = description,
                        date = startDate, // The `date` field is used for sorting/display, use startDate
                        originalStartDate = startDate,
                        originalEndDate = endDate,
                        startTime = if (isAllDay) null else startTime,
                        endTime = if (isAllDay) null else endTime,
                        isAllDay = isAllDay,
                        color = Color.Transparent, // Color is managed by Calendar
                        calendarId = selectedCalendarId,
                        reminderMinutes = selectedReminderMinutes
                    )
                    onSave(newEvent, selectedCalendarId)
                },
                enabled = true
            ) {
                Text("保存", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Title ---
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("输入标题", color = Color.Gray, fontSize = 24.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
            )

            HorizontalDivider(color = Color(0xFF2C3549))

            // --- Calendar Selector ---
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCalendarMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentCalendar = viewModel.calendars.find { it.id == selectedCalendarId }
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFFB7C4E6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = currentCalendar?.name ?: "Default",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                DropdownMenu(
                    expanded = showCalendarMenu,
                    onDismissRequest = { showCalendarMenu = false }
                ) {
                    viewModel.calendars.forEach { calendar ->
                        DropdownMenuItem(
                            text = { Text(calendar.name) },
                            onClick = {
                                selectedCalendarId = calendar.id
                                showCalendarMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3549))

            // --- Reminder Selector ---
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReminderMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFB7C4E6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))

                    // 显示当前选择的提醒
                    val displayText = remember(selectedReminderMinutes, startDate, startTime, isAllDay) {
                        val option = reminderOptions.find { it.minutes == selectedReminderMinutes }
                        if (option != null) {
                            option.displayName
                        } else {
                            if (selectedReminderMinutes > 0) {
                                val baseTime = if (isAllDay) startDate.atStartOfDay() else LocalDateTime.of(startDate, startTime)
                                val reminderTime = baseTime.minusMinutes(selectedReminderMinutes.toLong())
                                reminderTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))
                            } else {
                                "不提醒"
                            }
                        }
                    }

                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                DropdownMenu(
                    expanded = showReminderMenu,
                    onDismissRequest = { showReminderMenu = false }
                ) {
                    reminderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    color = if (option.isCustom) MaterialTheme.colorScheme.primary else Color.Unspecified
                                )
                            },
                            onClick = {
                                if (option.isCustom) {
                                    // 自定义选项：打开对话框
                                    showReminderMenu = false
                                    // 初始化提醒时间逻辑
                                    val baseTime = if (isAllDay) startDate.atStartOfDay() else LocalDateTime.of(startDate, startTime)
                                    val currentDiff = if (selectedReminderMinutes > 0) selectedReminderMinutes else 0
                                    val reminderDateTime = baseTime.minusMinutes(currentDiff.toLong())
                                    customReminderDate = reminderDateTime.toLocalDate()
                                    customReminderTime = reminderDateTime.toLocalTime()
                                    showCustomReminderDialog = true
                                } else {
                                    // 预设选项
                                    selectedReminderMinutes = option.minutes
                                    showReminderMenu = false
                                }
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3549))

            // --- Time Section ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // All Day Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFFB7C4E6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "全天",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                // Date & Time Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = startDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { showStartDatePicker = true }
                    )
                    if (!isAllDay) {
                        Text(
                            text = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable { showStartTimePicker = true }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = endDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { showEndDatePicker = true }
                    )
                    if (!isAllDay) {
                        Text(
                            text = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable { showEndTimePicker = true }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3549))

            // --- Description ---
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Subject,
                    contentDescription = null,
                    tint = Color(0xFFB7C4E6),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 12.dp)
                )
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("添加说明", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFFEAF0FF),
                        unfocusedTextColor = Color(0xFFB7C4E6),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            // --- Delete Button (if editing) ---
            if (event != null && onDelete != null) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onDelete(event.id); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3549)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("删除日程", color = Color.Red, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(50.dp))
        }
    }

    // --- Custom Reminder Dialog ---
    if (showCustomReminderDialog) {
        AlertDialog(
            onDismissRequest = { showCustomReminderDialog = false },
            title = {
                Text(
                    text = "自定义提醒时间",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "选择具体的提醒时间",
                        color = Color(0xFFB7C4E6),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customReminderDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { showReminderDatePicker = true }
                                .padding(8.dp)
                        )
                        Text(
                            text = customReminderTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { showReminderTimePicker = true }
                                .padding(8.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reminderDateTime = LocalDateTime.of(customReminderDate, customReminderTime)
                        val eventDateTime = if (isAllDay) startDate.atStartOfDay() else LocalDateTime.of(startDate, startTime)
                        val minutes = Duration.between(reminderDateTime, eventDateTime).toMinutes()

                        if (minutes >= 0) {
                            selectedReminderMinutes = minutes.toInt()
                            showCustomReminderDialog = false
                        } else {
                            // The reminder time is later than the start time. This feature is not supported yet. You can add a reminder here.
                            // Due to the absence of Toast environment, this feature is not processed or set to 0.
                            selectedReminderMinutes = 0
                            showCustomReminderDialog = false
                        }
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderDialog = false }) {
                    Text("取消", color = Color(0xFFB7C4E6))
                }
            },
            containerColor = Color(0xFF1A2234),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // --- Reminder Date & Time Pickers ---
    if (showReminderDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customReminderDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        customReminderDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showReminderDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = customReminderTime.hour,
            initialMinute = customReminderTime.minute
        )
        TimePickerDialog(
            onDismissRequest = { showReminderTimePicker = false },
            onConfirm = {
                customReminderTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showReminderTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    // --- Date & Time Pickers ---

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        if (startDate.isAfter(endDate)) {
                            endDate = startDate
                        }
                    }
                    showStartDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newEndDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        // Allow end date to be same as start date or after
                        if (!newEndDate.isBefore(startDate)) {
                            endDate = newEndDate
                        }
                    }
                    showEndDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startTime.hour,
            initialMinute = startTime.minute
        )
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = {
                startTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showStartTimePicker = false
                if (startDate == endDate && startTime.isAfter(endTime)) {
                    endTime = startTime.plusHours(1)
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endTime.hour,
            initialMinute = endTime.minute
        )
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = {
                endTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
        text = { content() }
    )
}

// Preset Colors
val EventColors = listOf(
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

// Helper for contrast
fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return (0.2126f * r + 0.7152f * g + 0.0722f * b)
}