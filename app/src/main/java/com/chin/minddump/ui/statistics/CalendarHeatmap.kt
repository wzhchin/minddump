package com.chin.minddump.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.chin.minddump.data.DayCount
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarHeatmap(
    dayCounts: List<DayCount>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalExpressiveShapes.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // Build a map of date -> count for quick lookup
    val countMap = remember(dayCounts) {
        dayCounts.associate { it.monthFolder to it.count }
    }

    val monthLabel = remember(currentMonth) {
        val formatter = DateTimeFormatter.ofPattern("yyyy年M月")
        currentMonth.format(formatter)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cardMedium as RoundedCornerShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { currentMonth = currentMonth.minusMonths(1) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = { currentMonth = currentMonth.plusMonths(1) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("加载中...", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
            }
            return@Column
        }

        // Day-of-week headers
        val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Calendar grid
        val firstDay = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        // Monday=1, Sunday=7 → convert to 0-indexed starting from Monday
        val startOffset = (firstDay.dayOfWeek.value - 1) // 0=Mon, 6=Sun
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        val cellSize = 36.dp
        val cellSpacing = 4.dp

        Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNumber)
                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val count = countMap[dateStr] ?: 0
                            val today = date == LocalDate.now()

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(heatmapColor(count, primaryColor, surfaceVariant, today)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (count > 0 || today) {
                                        Color.White
                                    } else {
                                        onSurfaceVariant
                                    },
                                )
                            }
                        } else {
                            Box(modifier = Modifier.size(cellSize))
                        }
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("少", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            repeat(4) { level ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when (level) {
                                0 -> surfaceVariant
                                1 -> primaryColor.copy(alpha = 0.3f)
                                2 -> primaryColor.copy(alpha = 0.6f)
                                else -> primaryColor
                            },
                        ),
                )
            }
            Text("多", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
        }
    }
}

private fun heatmapColor(count: Int, primary: Color, surfaceVariant: Color, isToday: Boolean): Color {
    if (isToday && count == 0) return primary.copy(alpha = 0.4f)
    return when {
        count == 0 -> surfaceVariant
        count <= 2 -> primary.copy(alpha = 0.25f)
        count <= 5 -> primary.copy(alpha = 0.5f)
        else -> primary
    }
}
