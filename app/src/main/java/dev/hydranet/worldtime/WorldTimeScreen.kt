package dev.hydranet.worldtime

import android.text.format.DateFormat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val SlotWidth = 48.dp
private val RowHeight = 76.dp
private val LabelWidth = 132.dp
private val HeaderHeight = 56.dp
private const val TimelineHours = 48L
private val DayGap = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldTimeScreen(
    zones: List<ZoneId>,
    homeZone: ZoneId,
    selected: Instant,
    followingNow: Boolean,
    onSelectTime: (Instant) -> Unit,
    onNow: () -> Unit,
    onZonesChange: (List<ZoneId>) -> Unit,
    onHomeZoneChange: (ZoneId) -> Unit,
    onShare: () -> Unit,
) {
    var showZonePicker by remember { mutableStateOf(false) }
    var timelineResetRequest by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val homeDateTime = selected.atZone(homeZone)

    fun resetToNow() {
        onNow()
        timelineResetRequest++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Time") },
                actions = {
                    IconButton(onClick = ::resetToNow) {
                        Icon(Icons.Outlined.Update, contentDescription = "Jump to now")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share selected times")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showZonePicker = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Add city") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = ::resetToNow,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                SelectedMomentCard(
                    homeDateTime = homeDateTime,
                    followingNow = followingNow,
                    onPickDate = {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                onSelectTime(WorldTimeData.onDate(
                                    selected, homeZone, LocalDate.of(year, month + 1, day),
                                ))
                            },
                            homeDateTime.year,
                            homeDateTime.monthValue - 1,
                            homeDateTime.dayOfMonth,
                        ).show()
                    },
                    onPickTime = {
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onSelectTime(WorldTimeData.onTime(
                                    selected, homeZone, LocalTime.of(hour, minute),
                                ))
                            },
                            homeDateTime.hour,
                            homeDateTime.minute,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    onNow = ::resetToNow,
                )
                Spacer(Modifier.height(12.dp))
                TimelineCard(
                    zones = zones,
                    homeZone = homeZone,
                    selected = selected,
                    followingNow = followingNow,
                    resetRequest = timelineResetRequest,
                    onSelectTime = onSelectTime,
                    onZonesChange = onZonesChange,
                    onHomeZoneChange = onHomeZoneChange,
                )
                Spacer(Modifier.height(12.dp))
                WorldDayNightCard(selected = selected, homeZone = homeZone)
                Spacer(Modifier.height(88.dp))
            }
        }
    }

    if (showZonePicker) {
        ZonePickerDialog(
            selected = selected,
            added = zones,
            onAdd = { zone ->
                onZonesChange(zones + zone)
                showZonePicker = false
            },
            onDismiss = { showZonePicker = false },
        )
    }

}

@Composable
private fun SelectedMomentCard(
    homeDateTime: ZonedDateTime,
    followingNow: Boolean,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onNow: () -> Unit,
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val timeFormat = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SELECTED MOMENT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (followingNow) {
                    Box(
                        Modifier.size(8.dp).clip(RoundedCornerShape(100))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.clickable(onClick = onPickTime),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = timeFormat.format(homeDateTime),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Set exact time",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${WorldTimeData.cityName(homeDateTime.zone)} · ${WorldTimeData.offsetLabel(homeDateTime.zone, homeDateTime.toInstant())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onPickDate,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(dateFormat.format(homeDateTime), maxLines = 1)
                }
                OutlinedButton(onClick = onNow, contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Now")
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(
    zones: List<ZoneId>,
    homeZone: ZoneId,
    selected: Instant,
    followingNow: Boolean,
    resetRequest: Int,
    onSelectTime: (Instant) -> Unit,
    onZonesChange: (List<ZoneId>) -> Unit,
    onHomeZoneChange: (ZoneId) -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val start = WorldTimeData.timelineStart(selected, homeZone)
    val slots = remember(start) { List(TimelineHours.toInt()) { start.plus(Duration.ofHours(it.toLong())) } }
    val selectedIndex = WorldTimeData.timelineHourIndex(start, selected)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val rowHeightPx = with(density) { RowHeight.toPx() }
    var draggingZoneId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragSourceIndex by remember { mutableStateOf(-1) }
    var dragTargetIndex by remember { mutableStateOf(-1) }
    var dragSnapshot by remember { mutableStateOf<List<ZoneId>?>(null) }
    val displayedZones = dragSnapshot ?: zones

    fun rowTranslation(index: Int): Float = when {
        index == dragSourceIndex -> dragOffsetY
        dragSourceIndex < dragTargetIndex && index in (dragSourceIndex + 1)..dragTargetIndex ->
            -rowHeightPx
        dragSourceIndex > dragTargetIndex && index in dragTargetIndex..<dragSourceIndex ->
            rowHeightPx
        else -> 0f
    }

    fun finishDrag(commit: Boolean) {
        val snapshot = dragSnapshot
        if (commit && snapshot != null && dragSourceIndex in snapshot.indices &&
            dragTargetIndex in snapshot.indices && dragSourceIndex != dragTargetIndex
        ) {
            onZonesChange(snapshot.toMutableList().apply {
                add(dragTargetIndex, removeAt(dragSourceIndex))
            })
        }
        draggingZoneId = null
        dragSnapshot = null
        dragSourceIndex = -1
        dragTargetIndex = -1
        dragOffsetY = 0f
    }
    LaunchedEffect(start, selectedIndex, resetRequest) {
        val target = with(density) { (SlotWidth * (selectedIndex - 2).coerceAtLeast(0)).roundToPx() }
        scrollState.scrollTo(target)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("TIME ZONES", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row {
                Column(Modifier.width(LabelWidth)) {
                    Box(Modifier.height(HeaderHeight).fillMaxWidth().padding(start = 12.dp),
                        contentAlignment = Alignment.CenterStart) {
                        Text("CITY / TIME", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    displayedZones.forEachIndexed { index, zone ->
                        key(zone.id) {
                            val currentIndex = zones.indexOf(zone)
                            ZoneLabel(
                                zone = zone,
                                homeZone = homeZone,
                                selected = selected,
                                homeDate = selected.atZone(homeZone).toLocalDate(),
                                is24Hour = is24Hour,
                                isDragging = draggingZoneId == zone.id,
                                dragInProgress = dragSnapshot != null,
                                translationY = rowTranslation(index),
                                showDivider = index < displayedZones.lastIndex,
                                canRemove = zones.size > 1,
                                onSetHome = { onHomeZoneChange(zone) },
                                onRemove = { onZonesChange(zones.filterNot { it == zone }) },
                                onDragStart = {
                                    dragSnapshot = zones
                                    draggingZoneId = zone.id
                                    dragSourceIndex = index
                                    dragTargetIndex = index
                                    dragOffsetY = 0f
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { deltaY ->
                                    val snapshot = dragSnapshot
                                    if (snapshot != null && dragSourceIndex in snapshot.indices) {
                                        val minimum = -dragSourceIndex * rowHeightPx
                                        val maximum = (snapshot.lastIndex - dragSourceIndex) * rowHeightPx
                                        dragOffsetY = (dragOffsetY + deltaY).coerceIn(minimum, maximum)
                                        val center = (dragSourceIndex + 0.5f) * rowHeightPx + dragOffsetY
                                        dragTargetIndex = (center / rowHeightPx).toInt()
                                            .coerceIn(0, snapshot.lastIndex)
                                    }
                                },
                                onDragEnd = { finishDrag(commit = true) },
                                onDragCancel = { finishDrag(commit = false) },
                                onMoveUp = {
                                    if (currentIndex > 0) onZonesChange(zones.toMutableList().apply {
                                        add(currentIndex - 1, removeAt(currentIndex))
                                    })
                                },
                                onMoveDown = {
                                    if (currentIndex in 0..<zones.lastIndex) onZonesChange(zones.toMutableList().apply {
                                        add(currentIndex + 1, removeAt(currentIndex))
                                    })
                                },
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1f)
                        .height(HeaderHeight + RowHeight * zones.size)
                        .horizontalScroll(scrollState),
                ) {
                    Column {
                        Row {
                            slots.forEachIndexed { index, instant ->
                                HeaderSlot(
                                    instant = instant,
                                    previous = slots.getOrNull(index - 1),
                                    homeZone = homeZone,
                                    selected = index == selectedIndex,
                                    followingNow = followingNow,
                                    onClick = { onSelectTime(instant) },
                                )
                            }
                        }
                        displayedZones.forEachIndexed { index, zone ->
                            key(zone.id) {
                                TimelineZoneRow(
                                    slots = slots,
                                    zone = zone,
                                    is24Hour = is24Hour,
                                    isDragging = draggingZoneId == zone.id,
                                    dragInProgress = dragSnapshot != null,
                                    translationY = rowTranslation(index),
                                    onSelectTime = onSelectTime,
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.offset(x = SlotWidth * selectedIndex)
                            .width(SlotWidth).fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(9.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneLabel(
    zone: ZoneId,
    homeZone: ZoneId,
    selected: Instant,
    homeDate: LocalDate,
    is24Hour: Boolean,
    isDragging: Boolean,
    dragInProgress: Boolean,
    translationY: Float,
    showDivider: Boolean,
    canRemove: Boolean,
    onSetHome: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val currentDragStart by rememberUpdatedState(onDragStart)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    val currentDragCancel by rememberUpdatedState(onDragCancel)
    val settledTranslationY by key(dragInProgress) {
        animateFloatAsState(
            targetValue = if (isDragging) 0f else translationY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "City row placement",
        )
    }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val local = selected.atZone(zone)
    val dayDifference = ChronoUnit.DAYS.between(homeDate, local.toLocalDate())
    val dayLabel = when (dayDifference) {
        -1L -> "Yesterday"
        0L -> "Today"
        1L -> "Tomorrow"
        else -> DateTimeFormatter.ofPattern("MMM d").format(local)
    }
    val timeFormat = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
    }

    Row(
        modifier = Modifier.width(LabelWidth).height(RowHeight)
            .pointerInput(zone.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentDrag(dragAmount.y)
                    },
                    onDragEnd = { currentDragEnd() },
                    onDragCancel = { currentDragCancel() },
                )
            }
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                this.translationY = if (isDragging) translationY else settledTranslationY
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                shape = RoundedCornerShape(8.dp)
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Move ${WorldTimeData.cityName(zone)} up") {
                        onMoveUp()
                        true
                    },
                    CustomAccessibilityAction("Move ${WorldTimeData.cityName(zone)} down") {
                        onMoveDown()
                        true
                    },
                )
            }
            .background(if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceContainer)
            .drawBehind {
                if (showDivider) {
                    val stroke = 1.dp.toPx()
                    val y = size.height - stroke / 2
                    drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), stroke)
                }
            }
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (zone == homeZone) "⌂ ${WorldTimeData.cityName(zone)}" else WorldTimeData.cityName(zone),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeFormat.format(local),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(
                text = "${WorldTimeData.offsetLabel(zone, selected).removeSuffix(":00")} · $dayLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.MoreVert,
                    contentDescription = "Options for ${WorldTimeData.cityName(zone)}",
                    modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Set as home") },
                    enabled = zone != homeZone,
                    onClick = {
                        menuOpen = false
                        onSetHome()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove city") },
                    enabled = canRemove,
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderSlot(
    instant: Instant,
    previous: Instant?,
    homeZone: ZoneId,
    selected: Boolean,
    followingNow: Boolean,
    onClick: () -> Unit,
) {
    val local = instant.atZone(homeZone)
    val newDay = previous != null &&
        previous.atZone(homeZone).toLocalDate() != local.toLocalDate()
    val colors = MaterialTheme.colorScheme
    val dayBackground = if (local.toLocalDate().toEpochDay() % 2L == 0L) {
        colors.surfaceContainer
    } else {
        colors.surfaceContainerHigh
    }
    Box(
        modifier = Modifier.width(SlotWidth).height(HeaderHeight)
            .background(dayBackground)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (selected && followingNow) "NOW"
                    else if (newDay) DateTimeFormatter.ofPattern("EEE").format(local).uppercase()
                    else "",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = if (selected) colors.primary else colors.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = if (newDay && !(selected && followingNow)) {
                    DateTimeFormatter.ofPattern("MMM d").format(local).uppercase()
                } else {
                    local.hour.toString().padStart(2, '0')
                },
                style = MaterialTheme.typography.labelLarge,
                fontSize = if (newDay && !(selected && followingNow)) 11.sp else 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = colors.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TimelineZoneRow(
    slots: List<Instant>,
    zone: ZoneId,
    is24Hour: Boolean,
    isDragging: Boolean,
    dragInProgress: Boolean,
    translationY: Float,
    onSelectTime: (Instant) -> Unit,
) {
    val settledTranslationY by key(dragInProgress) {
        animateFloatAsState(
            targetValue = if (isDragging) 0f else translationY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "Timeline row placement",
        )
    }

    Row(
        modifier = Modifier
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                this.translationY = if (isDragging) translationY else settledTranslationY
            },
    ) {
        slots.forEachIndexed { index, instant ->
            TimelineSlot(
                instant = instant,
                previous = slots.getOrNull(index - 1),
                next = slots.getOrNull(index + 1),
                zone = zone,
                is24Hour = is24Hour,
                onClick = { onSelectTime(instant) },
            )
        }
    }
}

@Composable
private fun TimelineSlot(
    instant: Instant,
    previous: Instant?,
    next: Instant?,
    zone: ZoneId,
    is24Hour: Boolean,
    onClick: () -> Unit,
) {
    val local = instant.atZone(zone)
    val newDay = previous != null &&
        previous.atZone(zone).toLocalDate() != local.toLocalDate()
    val previousLocal = previous?.atZone(zone)
    val nextLocal = next?.atZone(zone)
    val roundedStart = previousLocal == null || previousLocal.toLocalDate() != local.toLocalDate()
    val roundedEnd = nextLocal == null || nextLocal.toLocalDate() != local.toLocalDate()
    val shape = RoundedCornerShape(
        topStart = if (roundedStart) 8.dp else 0.dp,
        bottomStart = if (roundedStart) 8.dp else 0.dp,
        topEnd = if (roundedEnd) 8.dp else 0.dp,
        bottomEnd = if (roundedEnd) 8.dp else 0.dp,
    )
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = colors.timelineBandColors(local.hour)
    val hour = if (is24Hour) local.hour.toString().padStart(2, '0')
        else ((local.hour + 11) % 12 + 1).toString()
    val time = if (local.minute == 0) hour else "$hour:${local.minute.toString().padStart(2, '0')}"

    Box(
        modifier = Modifier.width(SlotWidth).height(RowHeight)
            .background(colors.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height(42.dp).align(Alignment.Center)
                .padding(start = if (newDay) DayGap else 0.dp)
                .background(background, shape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (newDay) {
                Text(
                    text = DateTimeFormatter.ofPattern("EEE").format(local).uppercase(),
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = colors.timelineDetailColor(foreground),
                    maxLines = 1,
                )
                Text(
                    text = DateTimeFormatter.ofPattern("MMM d").format(local).uppercase(),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = time,
                    fontSize = if (local.minute == 0) 16.sp else 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = foreground,
                    maxLines = 1,
                )
                if (!is24Hour) {
                    Text(
                        text = if (local.hour < 12) "am" else "pm",
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        color = colors.timelineDetailColor(foreground),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZonePickerDialog(
    selected: Instant,
    added: List<ZoneId>,
    onAdd: (ZoneId) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, added) {
        WorldTimeData.availableZones.filter { zone ->
            zone !in added && (
                query.isBlank() || zone.id.contains(query.trim(), ignoreCase = true) ||
                    WorldTimeData.cityName(zone).contains(query.trim(), ignoreCase = true)
                )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a time zone") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("City or time zone") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {{
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }} else null,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${results.size} time zones",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (results.isEmpty()) {
                    Text("No matching time zones", modifier = Modifier.padding(12.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(results, key = { it.id }) { zone ->
                            ListItem(
                                headlineContent = { Text(WorldTimeData.cityName(zone)) },
                                supportingContent = {
                                    Text("${WorldTimeData.regionName(zone)} · ${WorldTimeData.offsetLabel(zone, selected)}",
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                modifier = Modifier.clickable { onAdd(zone) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun WorldTimeScreenPreview() {
    WorldTimeTheme {
        WorldTimeScreen(
            zones = listOf(ZoneId.of("America/Los_Angeles"), ZoneId.of("America/New_York"),
                ZoneId.of("Europe/London")),
            homeZone = ZoneId.of("America/Los_Angeles"),
            selected = Instant.parse("2026-09-18T19:26:00Z"),
            followingNow = false,
            onSelectTime = {},
            onNow = {},
            onZonesChange = {},
            onHomeZoneChange = {},
            onShare = {},
        )
    }
}
