package com.chin.minddump.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.storage.TodoState
import com.chin.minddump.ui.BucketKind
import com.chin.minddump.ui.FeedFilter
import com.chin.minddump.ui.TimeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Which filter popover is currently open (at most one). */
private enum class FilterMenu { NONE, TIME, TODO }

/**
 * The three feed-filter actions (eventTime / todo / tag) for the top app bar,
 * each opening its own anchored dropdown. The tag action toggles the inline
 * faceted list (rendered separately by [FeedTagFacetList]); here it only flips
 * visibility via [onToggleTagList]. Each button shows a dot when its dimension
 * is active.
 */
@Composable
fun FeedFilterActions(
    filter: FeedFilter,
    onSetTime: (TimeFilter) -> Unit,
    onToggleTodo: (TodoState) -> Unit,
    onToggleTagList: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(FilterMenu.NONE) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // eventTime
        Box {
            FilterIconButton(
                icon = Icons.Filled.Schedule,
                desc = stringResource(R.string.filter_time),
                active = filter.timeActive,
                tint = tint,
                onClick = { menu = if (menu == FilterMenu.TIME) FilterMenu.NONE else FilterMenu.TIME },
            )
            DropdownMenu(expanded = menu == FilterMenu.TIME, onDismissRequest = { menu = FilterMenu.NONE }) {
                TimeMenuItems(
                    active = filter.time,
                    onPick = { onSetTime(it); menu = FilterMenu.NONE },
                )
            }
        }

        // todo
        Box {
            FilterIconButton(
                icon = Icons.Filled.TaskAlt,
                desc = stringResource(R.string.filter_todo),
                active = filter.todoActive,
                tint = tint,
                onClick = { menu = if (menu == FilterMenu.TODO) FilterMenu.NONE else FilterMenu.TODO },
            )
            DropdownMenu(expanded = menu == FilterMenu.TODO, onDismissRequest = { menu = FilterMenu.NONE }) {
                TodoState.entries.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(todoLabel(state)) },
                        trailingIcon = {
                            if (state in filter.todoStates) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = { onToggleTodo(state) },
                    )
                }
            }
        }

        // tag (toggles the inline list)
        FilterIconButton(
            icon = Icons.Filled.Label,
            desc = stringResource(R.string.filter_tag),
            active = filter.tagsActive,
            tint = tint,
            onClick = onToggleTagList,
        )
    }
}

@Composable
private fun TimeMenuItems(
    active: TimeFilter,
    onPick: (TimeFilter) -> Unit,
) {
    val buckets = listOf(
        BucketKind.TODAY to R.string.filter_time_today,
        BucketKind.NEXT_3_DAYS to R.string.filter_time_next_3_days,
        BucketKind.NEXT_7_DAYS to R.string.filter_time_next_7_days,
        BucketKind.PAST_7_DAYS to R.string.filter_time_past_7_days,
    )
    buckets.forEach { (kind, label) ->
        DropdownMenuItem(
            text = { Text(stringResource(label)) },
            trailingIcon = {
                if (active is TimeFilter.Bucket && active.kind == kind) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            },
            onClick = {
                // Re-tapping the active bucket deactivates; otherwise select it.
                val isSame = active is TimeFilter.Bucket && active.kind == kind
                onPick(if (isSame) TimeFilter.None else TimeFilter.Bucket(kind))
            },
        )
    }
    // Custom range entry.
    var showCustom by remember { mutableStateOf(false) }
    DropdownMenuItem(
        text = { Text(stringResource(R.string.filter_time_custom)) },
        trailingIcon = {
            if (active is TimeFilter.Range) Icon(Icons.Filled.Check, contentDescription = null)
        },
        onClick = { showCustom = true },
    )
    if (showCustom) {
        CustomRangePicker(
            initial = active as? TimeFilter.Range,
            onConfirm = { range ->
                showCustom = false
                onPick(range)
            },
            onDismiss = { showCustom = false },
        )
    }
}

/**
 * Two-step date picker (start then end) → inclusive [TimeFilter.Range]. Both
 * endpoints are required.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangePicker(
    initial: TimeFilter.Range?,
    onConfirm: (TimeFilter.Range) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickingEnd by remember { mutableStateOf(false) }
    val startState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.start?.toEpochMilli(),
    )
    val endState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.end?.toEpochMilli(),
    )

    val state = if (pickingEnd) endState else startState
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    val picked = state.selectedDateMillis ?: return@TextButton
                    if (!pickingEnd) {
                        pickingEnd = true
                    } else {
                        val start = (startState.selectedDateMillis ?: picked).toLocalDate()
                        val end = picked.toLocalDate()
                        // Normalize so start <= end.
                        val range = if (start <= end) TimeFilter.Range(start, end) else TimeFilter.Range(end, start)
                        onConfirm(range)
                    }
                },
            ) {
                Text(
                    stringResource(
                        if (pickingEnd) R.string.filter_custom_confirm else R.string.filter_custom_end,
                    ),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

private fun LocalDate.toEpochMilli(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

private fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

@Composable
private fun todoLabel(state: TodoState): String = when (state) {
    TodoState.NONE -> stringResource(R.string.status_none)
    TodoState.TODO -> stringResource(R.string.status_todo)
    TodoState.DOING -> stringResource(R.string.status_doing)
    TodoState.WAIT -> stringResource(R.string.status_wait)
    TodoState.DONE -> stringResource(R.string.status_done)
    TodoState.CANCEL -> stringResource(R.string.status_cancel)
}

/**
 * One filter icon button with a small dot overlay when [active].
 */
@Composable
private fun FilterIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    active: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    Box {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = desc, tint = tint)
        }
        if (active) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(6.dp),
            ) {}
        }
    }
}

/**
 * The inline faceted tag list shown above the entry list. [facetTags] are the
 * tags still available (computed by the ViewModel from time+todo-narrowed
 * entries); [selectedTags] are the active intersection selections. Tapping a
 * selected tag deselects; tapping an available tag selects (intersection).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FeedTagFacetList(
    facetTags: List<String>,
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLower = selectedTags.map { it.lowercase() }.toSet()
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        facetTags.forEach { tag ->
            val isSelected = tag.lowercase() in selectedLower
            FilterChip(
                selected = isSelected,
                onClick = { onToggleTag(tag) },
                label = { Text("#$tag") },
            )
        }
        if (selectedTags.isNotEmpty()) {
            TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(stringResource(R.string.filter_clear))
            }
        }
    }
}
