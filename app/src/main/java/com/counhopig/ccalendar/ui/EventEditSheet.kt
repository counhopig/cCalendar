package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.counhopig.ccalendar.data.AppSettingsRepository
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

private val NeoBackground = Color(0xFFE9EEF6)
private val NeoSurface = Color(0xFFE9EEF6)
private val NeoText = Color(0xFF243044)
private val NeoTextMuted = Color(0xFF738099)
private val NeoAccent = Color(0xFF7C5CFF)
private val NeoDivider = Color(0xFFD0D8E6)

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
        containerColor = NeoBackground,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.statusBarsPadding(),
                color = NeoTextMuted
            )
        }
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

    val context = LocalContext.current
    val appSettingsRepository = remember { AppSettingsRepository(context) }

    // Reminder state
    var selectedReminderMinutes by remember {
        mutableStateOf(event?.reminderMinutes ?: appSettingsRepository.getSettings().defaultReminderMinutes)
    }
    var showCustomReminderDialog by remember { mutableStateOf(false) }
    var customReminderValue by remember { mutableStateOf("") }
    var customReminderDate by remember { mutableStateOf(LocalDate.now()) }
    var customReminderTime by remember { mutableStateOf(LocalTime.now()) }
    val reminderOptions = defaultReminderOptions

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Spacer(Modifier.size(48.dp))
            Text(
                text = if (event == null) "添加日程" else "编辑日程",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeoText
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
                placeholder = { Text("输入标题", color = NeoTextMuted, fontSize = 24.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = NeoText,
                    unfocusedTextColor = NeoText,
                    cursorColor = NeoAccent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = NeoText)
            )

            HorizontalDivider(color = NeoDivider)

            // --- Calendar Selector ---
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showCalendarMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentCalendar = viewModel.calendars.find { it.id == selectedCalendarId }
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = NeoTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = currentCalendar?.name ?: "Default",
                        color = NeoText,
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

            HorizontalDivider(color = NeoDivider)

            // --- Reminder Selector ---
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showReminderMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = NeoTextMuted,
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
                        color = NeoText,
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
                                    color = if (option.isCustom) NeoAccent else NeoText
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

            HorizontalDivider(color = NeoDivider)

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
                        tint = NeoTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "全天",
                        color = NeoText,
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
                        color = NeoText,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showStartDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    if (!isAllDay) {
                        Text(
                            text = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = NeoText,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showStartTimePicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
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
                        color = NeoText,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showEndDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    if (!isAllDay) {
                        Text(
                            text = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = NeoText,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showEndTimePicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = NeoDivider)

            // --- Description ---
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Subject,
                    contentDescription = null,
                    tint = NeoTextMuted,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 12.dp)
                )
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("添加说明", color = NeoTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = NeoText,
                        unfocusedTextColor = NeoTextMuted,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeoSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
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
                    color = NeoText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "选择具体的提醒时间",
                        color = NeoTextMuted,
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
                            color = NeoText,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showReminderDatePicker = true }
                                .padding(8.dp)
                        )
                        Text(
                            text = customReminderTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = NeoText,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
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
                    Text("确定", color = NeoAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderDialog = false }) {
                    Text("取消", color = NeoTextMuted)
                }
            },
            containerColor = NeoSurface,
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
        WheelTimePickerDialog(
            initialTime = customReminderTime,
            onDismissRequest = { showReminderTimePicker = false },
            onConfirm = { selectedTime ->
                customReminderTime = selectedTime
                showReminderTimePicker = false
            }
        )
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
        WheelTimePickerDialog(
            initialTime = startTime,
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { selectedTime ->
                startTime = selectedTime
                showStartTimePicker = false
                if (startDate == endDate && startTime.isAfter(endTime)) {
                    endTime = startTime.plusHours(1)
                }
            }
        )
    }

    if (showEndTimePicker) {
        WheelTimePickerDialog(
            initialTime = endTime,
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = { selectedTime ->
                endTime = selectedTime
                showEndTimePicker = false
            }
        )
    }
}

@Composable
fun WheelTimePickerDialog(
    initialTime: LocalTime,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var selectedHour by remember(initialTime) { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember(initialTime) { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "选择时间",
                color = NeoText,
                fontWeight = FontWeight.SemiBold
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(selectedHour, selectedMinute)) }) {
                Text("确定", color = NeoAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消", color = NeoTextMuted)
            }
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelNumberPicker(
                    value = selectedHour,
                    range = 0..23,
                    suffix = "时",
                    onValueChange = { selectedHour = it }
                )
                Text(
                    text = ":",
                    color = NeoText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                WheelNumberPicker(
                    value = selectedMinute,
                    range = 0..59,
                    suffix = "分",
                    onValueChange = { selectedMinute = it }
                )
            }
        },
        containerColor = NeoSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun WheelNumberPicker(
    value: Int,
    range: IntRange,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    val values = remember(range) { range.toList() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, values.lastIndex)
    )

    LaunchedEffect(value) {
        val index = (value - range.first).coerceIn(0, values.lastIndex)
        if (listState.firstVisibleItemIndex != index) {
            listState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val selected = (range.first + listState.firstVisibleItemIndex)
                    .coerceIn(range.first, range.last)
                if (selected != value) {
                    onValueChange(selected)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .width(112.dp)
            .height(176.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.86f), RoundedCornerShape(14.dp))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(values) { number ->
                val selected = number == value
                Text(
                    text = "%02d%s".format(number, suffix),
                    color = if (selected) NeoText else NeoTextMuted.copy(alpha = 0.56f),
                    fontSize = if (selected) 24.sp else 18.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onValueChange(number) }
                        .wrapContentHeight(Alignment.CenterVertically),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
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
