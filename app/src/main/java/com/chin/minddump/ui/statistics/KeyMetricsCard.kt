package com.chin.minddump.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chin.minddump.ui.theme.LocalExpressiveShapes

@Composable
fun KeyMetricsCard(
    totalEntries: Int,
    currentStreak: Int,
    longestStreak: Int,
    peakHour: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalExpressiveShapes.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.cardMedium as RoundedCornerShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MetricItem(
                label = "总记录",
                value = if (isLoading) "—" else totalEntries.toString(),
            )
            MetricItem(
                label = "连续天数",
                value = if (isLoading) "—" else "$currentStreak 天",
            )
            MetricItem(
                label = "最长连续",
                value = if (isLoading) "—" else "$longestStreak 天",
            )
            MetricItem(
                label = "活跃时段",
                value = if (isLoading || peakHour < 0) "—" else "$peakHour:00",
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
