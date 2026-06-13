package com.chin.minddump.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.chin.minddump.data.DayCount
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TrendChart(
    data: List<DayCount>,
    selectedRange: TrendRange,
    onRangeSelected: (TrendRange) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalExpressiveShapes.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val barGradient = Brush.verticalGradient(
        colors = listOf(
            primaryColor,
            primaryColor.copy(alpha = 0.6f),
        ),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cardMedium as RoundedCornerShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        // Range toggle chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            TrendRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) primaryColor.copy(alpha = 0.15f) else surfaceVariant,
                        ).clickable { onRangeSelected(range) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = range.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) primaryColor else onSurfaceVariant,
                    )
                }
            }
        }

        if (isLoading || data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isLoading) "加载中..." else "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant,
                )
            }
            return@Column
        }

        // Bar chart
        val maxCount = data.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val barCount = data.size
            if (barCount == 0) return@Canvas

            val totalWidth = size.width
            val barSpacing = 4.dp.toPx()
            val barWidth = (totalWidth - barSpacing * (barCount - 1)) / barCount
            val chartHeight = size.height - 20.dp.toPx() // space for labels

            data.forEachIndexed { index, dayCount ->
                val barHeight = (dayCount.count.toFloat() / maxCount) * chartHeight
                val x = index * (barWidth + barSpacing)
                val y = chartHeight - barHeight

                drawRoundRect(
                    brush = barGradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }

        // Date labels (first and last)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            data.firstOrNull()?.dateFolder?.let {
                Text(formatShortDate(it), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            }
            data.lastOrNull()?.dateFolder?.let {
                Text(formatShortDate(it), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            }
        }
    }
}

private fun formatShortDate(dateStr: String): String = try {
    val date = LocalDate.parse(dateStr)
    val formatter = DateTimeFormatter.ofPattern("M/d")
    date.format(formatter)
} catch (_: Exception) {
    dateStr
}
