package com.counhopig.ccalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.counhopig.ccalendar.ui.theme.CCalendarTheme
import com.counhopig.ccalendar.ui.viewmodel.EventViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp 
import kotlin.math.absoluteValue
import com.counhopig.ccalendar.ui.model.Event
import java.time.LocalTime



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    modifier: Modifier = Modifier,
    viewModel: EventViewModel = viewModel()
) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    
    val currentMonth = remember(pagerState.currentPage) {
        val monthsToAdd = pagerState.currentPage - initialPage
        YearMonth.from(today).plusMonths(monthsToAdd.toLong())
    }

    // Sheet state
    var showSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSheet) {
        EventEditSheet(
            event = eventToEdit,
            selectedDate = selectedDate,
            onDismiss = { showSheet = false },
            onSave = { event -> 
                if (eventToEdit == null) {
                    viewModel.addEvent(event)
                } else {
                    viewModel.updateEvent(event)
                }
                showSheet = false
            },
            onDelete = { id -> viewModel.deleteEvent(id) },
            sheetState = sheetState
        )
    }

    val bg = Brush.verticalGradient(

        listOf(
            Color(0xFF0B1220),
            Color(0xFF0B1220),
            Color(0xFF060A12)
        )
    )

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadSystemEvents(context)
        }
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.READ_CALENDAR
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadSystemEvents(context)
        } else {
            launcher.launch(permission)
        }
    }

    val onDateClick = { date: LocalDate ->
        selectedDate = date
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header(
                title = currentMonth.month.getDisplayName(TextStyle.FULL, locale),
                subtitle = currentMonth.year.toString()
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val monthForPage = remember(page) {
                    val monthsToAdd = page - initialPage
                    YearMonth.from(today).plusMonths(monthsToAdd.toLong())
                }
                
                Box(
                    modifier = Modifier.graphicsLayer {
                        // Calculate the absolute offset for the current page from the
                        // scroll position. We use the absolute value which allows us to mirror
                        // any effects for both directions
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                        // We animate the alpha, between 50% and 100%
                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        
                        // Scale slightly
                        scaleX = lerp(
                            start = 0.9f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        scaleY = scaleX
                    }
                ) {
                    MonthCard(
                        month = monthForPage,
                        today = today,
                        selectedDate = selectedDate,
                        locale = locale,
                        viewModel = viewModel,
                        onDateClick = onDateClick
                    )
                }
            }

            AgendaCard(
                selectedDate = selectedDate, 
                viewModel = viewModel,
                onAddClick = {
                    eventToEdit = null
                    showSheet = true
                },
                onEventClick = { event ->
                    eventToEdit = event
                    showSheet = true
                }
            )
        }
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFEAF0FF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFFB7C4E6),
                fontSize = 14.sp
            )
        }
    }
}




@Composable
private fun MonthCard(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    locale: Locale,
    viewModel: EventViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0F1B33)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeekHeader(locale)
            MonthGrid(
                month = month,
                today = today,
                selectedDate = selectedDate,
                viewModel = viewModel,
                onDateClick = onDateClick
            )
        }
    }
}

@Composable
private fun WeekHeader(locale: Locale) {
    val dayOrder = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        dayOrder.forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                color = Color(0xFFB7C4E6),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    viewModel: EventViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val mondayBasedIndex = ((first.dayOfWeek.value + 6) % 7) // Monday=0

    var day = 1

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val inMonth = cellIndex >= mondayBasedIndex && day <= daysInMonth

                    val cellDate = if (inMonth) month.atDay(day) else null
                    val isToday = cellDate == today
                    val isSelected = cellDate == selectedDate

                    DayCell(
                        text = if (inMonth) day.toString() else "",
                        isToday = isToday,
                        isSelected = isSelected,
                        isInMonth = inMonth,
                        eventCount = if (cellDate != null) viewModel.getEventsForDate(cellDate).size else 0,
                        modifier = Modifier.weight(1f),
                        onClick = { if (cellDate != null) onDateClick(cellDate) }
                    )

                    if (inMonth) day++
                }
            }
            if (day > daysInMonth) break
        }
    }
}

@Composable
private fun DayCell(
    text: String,
    isToday: Boolean,
    isSelected: Boolean,
    isInMonth: Boolean,
    eventCount: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val bg = when {
        isSelected -> Color(0xFF7C5CFF)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        isToday -> Color(0xFF7C5CFF)
        isInMonth -> Color(0xFFEAF0FF)
        else -> Color(0xFFB7C4E6)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (eventCount > 0) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(minOf(eventCount, 3)) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) Color.White else Color(0xFF7C5CFF))
                                .padding(horizontal = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaCard(
    selectedDate: LocalDate, 
    viewModel: EventViewModel,
    onAddClick: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0F1B33)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = if (selectedDate == LocalDate.now()) "今日日程" else "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日"
                Text(
                    text = titleText,
                    color = Color(0xFFEAF0FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF151F3A))
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color(0xFFEAF0FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(6.dp))

            val events = viewModel.getEventsForDate(selectedDate)

            if (events.isEmpty()) {
                Text(
                    text = "${selectedDate} · 暂无日程",
                    color = Color(0xFFB7C4E6),
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        EventItem(event = event, onClick = { onEventClick(event) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(event: com.counhopig.ccalendar.ui.model.Event, onClick: () -> Unit) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // 颜色指示条
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(4.dp, 20.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                .background(event.color)
        )
        Spacer(Modifier.width(12.dp))
        
        Column(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            Text(
                text = event.title,
                color = androidx.compose.ui.graphics.Color(0xFFEAF0FF),
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    color = androidx.compose.ui.graphics.Color(0xFFB7C4E6),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (event.hasTime) {
                val timeText = buildString {
                    if (event.startTime != null) {
                        append(event.startTime.toString().substring(0, 5))
                        if (event.endTime != null) {
                            append(" - ")
                            append(event.endTime.toString().substring(0, 5))
                        }
                    } else if (!event.isAllDay) {
                        append("全天")
                    }
                }
                if (timeText.isNotEmpty()) {
                    Text(
                        text = timeText,
                        color = androidx.compose.ui.graphics.Color(0xFF7C5CFF),
                        fontSize = 11.sp
                    )
                }
            }
        }
        
        if (event.reminderMinutes > 0) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "提醒",
                tint = Color(0xFFB7C4E6),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun MonthScreenPreview() {
    CCalendarTheme {
        MonthScreen(viewModel = EventViewModel())
    }
}
