package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.counhopig.ccalendar.ui.model.Event
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditSheet(
    event: Event?,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
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
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var isAllDay by remember { mutableStateOf(event?.isAllDay ?: false) }
    
    // Time & Date state
    var date by remember { mutableStateOf(event?.date ?: selectedDate) }
    var startTime by remember { mutableStateOf(event?.startTime ?: LocalTime.now().plusHours(1).withMinute(0)) }
    var endTime by remember { mutableStateOf(event?.endTime ?: LocalTime.now().plusHours(2).withMinute(0)) }
    
    // Color state
    var selectedColor by remember { mutableStateOf(event?.color ?: EventColors.first()) }
    
    // Dialog visibility states
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

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
                        date = date,
                        startTime = if (isAllDay) null else startTime,
                        endTime = if (isAllDay) null else endTime,
                        isAllDay = isAllDay,
                        color = selectedColor
                    )
                    onSave(newEvent)
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
                        .padding(start = 40.dp) // Indent to align with text above
                        .clickable { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE")),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                if (!isAllDay) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { showStartTimePicker = true }
                                .padding(vertical = 8.dp)
                        )
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_today), // Just an arrow fallback or similar
                            contentDescription = "to",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp).background(Color.Transparent) // Placeholder
                        )
                        Text(
                            text = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { showEndTimePicker = true }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3549))

            // --- Color Section ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = Color(0xFFB7C4E6), // Icon color
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("颜色", color = Color.White, fontSize = 16.sp)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EventColors.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) 
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (color.luminance() > 0.5) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3549))

            // --- Description ---
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Subject,
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

    // --- Pickers ---

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
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
                // Auto-adjust end time if needed
                if (startTime.isAfter(endTime)) {
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

