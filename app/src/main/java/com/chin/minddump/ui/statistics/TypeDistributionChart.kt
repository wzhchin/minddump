package com.chin.minddump.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.chin.minddump.data.TypeCount
import com.chin.minddump.storage.EntryType
import com.chin.minddump.ui.theme.LocalExpressiveShapes

@Composable
fun TypeDistributionChart(
    data: List<TypeCount>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalExpressiveShapes.current
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val typeColors = mapOf(
        EntryType.TEXT to Color(0xFF6750A4),
        EntryType.PHOTO to Color(0xFF006780),
        EntryType.RECORDING to Color(0xFFA23F16),
        EntryType.VIDEO to Color(0xFFBA1A1A),
        EntryType.FILE to Color(0xFF7F747C),
        EntryType.UNKNOWN to Color(0xFF998D96),
    )

    val typeLabels = mapOf(
        EntryType.TEXT to "文字",
        EntryType.PHOTO to "拍照",
        EntryType.RECORDING to "录音",
        EntryType.VIDEO to "视频",
        EntryType.FILE to "文件",
        EntryType.UNKNOWN to "未知",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cardMedium as RoundedCornerShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isLoading || data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(120.dp),
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

        val total = data.sumOf { entry -> entry.count }.coerceAtLeast(1)

        // Donut chart
        Canvas(
            modifier = Modifier.size(140.dp),
        ) {
            val strokeWidth = 24.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f

            data.forEach { entry ->
                val sweepAngle = (entry.count.toFloat() / total) * 360f
                val color = typeColors[entry.type] ?: Color.Gray

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweepAngle
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            data.forEach { entry ->
                val color = typeColors[entry.type] ?: Color.Gray
                val label = typeLabels[entry.type] ?: entry.type.name

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color),
                    )
                    Text(
                        text = "$label ${entry.count}",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant,
                    )
                }
            }
        }
    }
}
