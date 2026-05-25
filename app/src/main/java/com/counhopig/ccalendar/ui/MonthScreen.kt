package com.counhopig.ccalendar.ui

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counhopig.ccalendar.data.AppSettingsRepository
import com.counhopig.ccalendar.ui.model.Event
import com.counhopig.ccalendar.ui.theme.CCalendarTheme
import com.counhopig.ccalendar.ui.viewmodel.EventViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val NeoBackground = Color(0xFFE9EEF6)
private val NeoSurface = Color(0xFFE9EEF6)
private val NeoSurfaceLight = Color(0xFFF8FAFE)
private val NeoText = Color(0xFF243044)
private val NeoTextMuted = Color(0xFF738099)
private val NeoAccent = Color(0xFF7C5CFF)
private val NeoAccentSoft = Color(0xFFE2DEFF)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    modifier: Modifier = Modifier,
    viewModel: EventViewModel = viewModel(),
    initialSelectedDate: LocalDate? = null
) {
    val context = LocalContext.current
    val locale = remember { Locale.getDefault() }
    val today = remember { LocalDate.now() }
    val appSettingsRepository = remember { AppSettingsRepository(context) }
    var appSettings by remember { mutableStateOf(appSettingsRepository.getSettings()) }
    var selectedDate by remember { mutableStateOf(initialSelectedDate ?: today) }
    val scope = rememberCoroutineScope()

    val basePage = Int.MAX_VALUE / 2
    val initialMonthOffset = remember(initialSelectedDate) {
        initialSelectedDate?.let {
            ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(it)).toInt()
        } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = basePage + initialMonthOffset, pageCount = { Int.MAX_VALUE })

    val currentMonth = remember(pagerState.currentPage) {
        val monthsToAdd = pagerState.currentPage - basePage
        YearMonth.from(today).plusMonths(monthsToAdd.toLong())
    }
    var activeMonth by remember { mutableStateOf(currentMonth) }

    // Sheet states
    var showEventEditSheet by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }
    var showAppSettingsSheet by remember { mutableStateOf(false) }
    var showWidgetSettingsSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }
    val eventEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val calendarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val icsImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importIcs(context, it)
            showCalendarSheet = false
        }
    }

    if (showEventEditSheet) {
        EventEditSheet(
            event = eventToEdit,
            selectedDate = selectedDate,
            viewModel = viewModel,
            onDismiss = { showEventEditSheet = false },
            onSave = { event, calendarId ->
                if (eventToEdit == null) {
                    viewModel.addEvent(event, calendarId, context)
                } else {
                    viewModel.updateEvent(event, calendarId, context)
                }
                showEventEditSheet = false
            },
            onDelete = { id ->
                viewModel.deleteEvent(id, context)
                showEventEditSheet = false
            },
            sheetState = eventEditSheetState
        )
    }

    if (showCalendarSheet) {
        CalendarSelectionSheet(
            viewModel = viewModel,
            onDismiss = { showCalendarSheet = false },
            onImportIcs = { icsImporter.launch("text/calendar") },
            sheetState = calendarSheetState
        )
    }

    if (showAppSettingsSheet) {
        AppSettingsSheet(
            settings = appSettings,
            onSave = { settings ->
                appSettingsRepository.saveSettings(settings)
                appSettings = settings
                showAppSettingsSheet = false
            },
            onDismissRequest = { showAppSettingsSheet = false }
        )
    }

    if (showWidgetSettingsSheet) {
        WidgetSettingsSheet(
            viewModel = viewModel,
            onDismissRequest = { showWidgetSettingsSheet = false }
        )
    }

    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFFF6F8FC),
            NeoBackground,
            Color(0xFFDDE5F0)
        )
    )

    // Initial load
    LaunchedEffect(Unit) {
        viewModel.loadCalendars(context)
    }

    // Reload events when month or selection changes
    LaunchedEffect(activeMonth, viewModel.selectedCalendarIds) {
        viewModel.loadSystemEvents(context, activeMonth)
    }

    // Keep the agenda panel aligned with the month currently on screen.
    LaunchedEffect(currentMonth, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            activeMonth = currentMonth
            if (YearMonth.from(selectedDate) != currentMonth) {
                selectedDate = if (currentMonth == YearMonth.from(today)) {
                    today
                } else {
                    currentMonth.atDay(1)
                }
            }
        }
    }

    val onDateClick = { date: LocalDate ->
        selectedDate = date
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val onTodayClick = {
                    selectedDate = today
                    scope.launch {
                        pagerState.animateScrollToPage(basePage)
                    }
                    Unit
                }
                
                Header(
                    title = activeMonth.month.getDisplayName(TextStyle.FULL, locale),
                    subtitle = activeMonth.year.toString(),
                    onSettingsClick = { showCalendarSheet = true },
                    onAppSettingsClick = { showAppSettingsSheet = true },
                    onWidgetSettingsClick = { showWidgetSettingsSheet = true },
                    onTodayClick = onTodayClick
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val monthForPage = remember(page) {
                        val monthsToAdd = page - basePage
                        YearMonth.from(today).plusMonths(monthsToAdd.toLong())
                    }

                    MonthCard(
                        month = monthForPage,
                        today = today,
                        selectedDate = selectedDate,
                        locale = locale,
                        weekStartsOnMonday = appSettings.weekStartsOnMonday,
                        showAdjacentMonthDays = appSettings.showAdjacentMonthDays,
                        showEventDots = appSettings.showEventDots,
                        viewModel = viewModel,
                        onDateClick = onDateClick
                    )
                }

                AgendaCard(
                    selectedDate = selectedDate,
                    viewModel = viewModel,
                    use24HourTime = appSettings.use24HourTime,
                    onAddClick = {
                        eventToEdit = null
                        showEventEditSheet = true
                    },
                    onEventClick = { event ->
                        eventToEdit = event
                        showEventEditSheet = true
                    }
                )
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String,
    onSettingsClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onWidgetSettingsClick: () -> Unit,
    onTodayClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NeoText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = NeoTextMuted,
                fontSize = 14.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = NeoSurface,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
            modifier = Modifier
                .padding(end = 8.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onTodayClick)
        ) {
            Text(
                text = "今天",
                color = NeoAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeoSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = NeoText,
                    modifier = Modifier.size(21.dp)
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(NeoSurfaceLight)
            ) {
                DropdownMenuItem(
                    text = { Text("日历管理", color = NeoText) },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = NeoTextMuted) },
                    onClick = {
                        onSettingsClick()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("应用设置", color = NeoText) },
                    leadingIcon = { Icon(Icons.Default.Tune, null, tint = NeoTextMuted) },
                    onClick = {
                        onAppSettingsClick()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("小组件设置", color = NeoText) },
                    leadingIcon = { Icon(Icons.Default.Palette, null, tint = NeoTextMuted) },
                    onClick = {
                        onWidgetSettingsClick()
                        showMenu = false
                    }
                )
            }
        }
    }
}




@Composable
private fun MonthCard(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    locale: Locale,
    weekStartsOnMonday: Boolean,
    showAdjacentMonthDays: Boolean,
    showEventDots: Boolean,
    viewModel: EventViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        color = NeoSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeekHeader(locale, weekStartsOnMonday)
            MonthGrid(
                month = month,
                today = today,
                selectedDate = selectedDate,
                weekStartsOnMonday = weekStartsOnMonday,
                showAdjacentMonthDays = showAdjacentMonthDays,
                showEventDots = showEventDots,
                viewModel = viewModel,
                onDateClick = onDateClick
            )
        }
    }
}

@Composable
private fun WeekHeader(locale: Locale, weekStartsOnMonday: Boolean) {
    val dayOrder = if (weekStartsOnMonday) {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    } else {
        listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        dayOrder.forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                color = NeoTextMuted,
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
    weekStartsOnMonday: Boolean,
    showAdjacentMonthDays: Boolean,
    showEventDots: Boolean,
    viewModel: EventViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val firstDayOffset = if (weekStartsOnMonday) {
        (first.dayOfWeek.value + 6) % 7
    } else {
        first.dayOfWeek.value % 7
    }
    val firstVisibleDate = first.minusDays(firstDayOffset.toLong())

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val cellDate = firstVisibleDate.plusDays(cellIndex.toLong())
                    val inMonth = YearMonth.from(cellDate) == month
                    val isToday = cellDate == today
                    val isSelected = cellDate == selectedDate

                    DayCell(
                        text = if (inMonth || showAdjacentMonthDays) cellDate.dayOfMonth.toString() else "",
                        isToday = isToday,
                        isSelected = isSelected,
                        isInMonth = inMonth,
                        eventColors = if (inMonth && showEventDots) {
                            viewModel.getEventsForDate(cellDate).map { it.color }.distinct().take(3)
                        } else {
                            emptyList()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = inMonth,
                        onClick = { onDateClick(cellDate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    text: String,
    isToday: Boolean,
    isSelected: Boolean,
    isInMonth: Boolean,
    eventColors: List<Color> = emptyList(),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val targetBg = when {
        isSelected -> NeoAccent
        isToday -> NeoAccentSoft
        else -> Color.Transparent
    }
    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "dayBackground"
    )

    val targetTextColor = when {
        isSelected -> Color.White
        isToday -> NeoAccent
        isInMonth -> NeoText
        else -> NeoTextMuted.copy(alpha = 0.45f)
    }
    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "dayTextColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isToday && !isSelected) 1.dp else 0.dp,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "dayBorderWidth"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .background(bg)
            .border(
                width = borderWidth,
                color = if (isToday && !isSelected) Color.White.copy(alpha = 0.75f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
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
            if (eventColors.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    eventColors.forEach { eventColor ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) Color.White else eventColor)
                                .padding(horizontal = 1.dp)
                        )
                        Spacer(Modifier.width(2.dp))
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
    use24HourTime: Boolean,
    onAddClick: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        color = NeoSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = if (selectedDate == LocalDate.now()) "今日日程" else "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日"
                Column {
                    Text(
                        text = titleText,
                        color = NeoText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        color = NeoTextMuted,
                        fontSize = 12.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeoAccent)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(10.dp))

            val events = viewModel.getEventsForDate(selectedDate)

            if (events.isEmpty()) {
                Surface(
                    color = NeoSurface,
                    shadowElevation = 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "暂无日程，今天可以轻松一点。",
                        color = NeoTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        EventItem(
                            event = event,
                            use24HourTime = use24HourTime,
                            onClick = { onEventClick(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(
    event: com.counhopig.ccalendar.ui.model.Event,
    use24HourTime: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeoSurface)
            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // 颜色指示条
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(5.dp, 34.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                .background(event.color)
        )
        Spacer(Modifier.width(12.dp))
        
        Column(
            modifier = androidx.compose.ui.Modifier.weight(1f)
        ) {
            Text(
                text = event.title,
                color = NeoText,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    color = NeoTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (event.hasTime) {
                val timeText = buildString {
                    if (event.startTime != null) {
                        append(event.startTime.formatForDisplay(use24HourTime))
                        if (event.endTime != null) {
                            append(" - ")
                            append(event.endTime.formatForDisplay(use24HourTime))
                        }
                    } else if (!event.isAllDay) {
                        append("全天")
                    }
                }
                if (timeText.isNotEmpty()) {
                    Text(
                        text = timeText,
                        color = NeoAccent,
                        fontSize = 11.sp
                    )
                }
            }
        }
        
        if (event.reminderMinutes > 0) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "提醒",
                tint = NeoTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun java.time.LocalTime.formatForDisplay(use24HourTime: Boolean): String {
    return if (use24HourTime) {
        "%02d:%02d".format(hour, minute)
    } else {
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val period = if (hour < 12) "上午" else "下午"
        "$period %d:%02d".format(hour12, minute)
    }
}



@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
private fun MonthScreenPreview() {
    CCalendarTheme {
        MonthScreen(viewModel = EventViewModel(), initialSelectedDate = null)
    }
}
