package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.*

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
        containerColor = Color(0xFF0F1B33),
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
                .padding(horizontal = 16.dp, vertical = 8.dp) // Adjusted padding
        ) {
            Text(
                text = "选择日历",
                color = Color(0xFFEAF0FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.calendars) { calendar ->
                    CalendarRow(
                        calendar = calendar,
                        isSelected = viewModel.selectedCalendarIds.contains(calendar.id),
                        onToggle = { viewModel.toggleCalendarSelection(calendar.id) },
                        onColorChange = { color -> viewModel.updateCalendarColor(calendar.id, color) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2C3549))

            Button(
                onClick = onImportIcs,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151F3A))
            ) {
                Text("从 ICS 文件导入", color = Color(0xFFEAF0FF))
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
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
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
                containerColor = Color(0xFF1A2234)
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
            color = Color(0xFFEAF0FF),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
                .clickable(onClick = onToggle) // Click text to toggle visibility
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF7C5CFF),
                uncheckedColor = Color(0xFFB7C4E6)
            )
        )
    }
}
