package com.chin.minddump.ui.statistics

import com.chin.minddump.data.DayCount
import com.chin.minddump.data.HourCount
import com.chin.minddump.data.TypeCount

/**
 * UI state for the statistics screen.
 */
data class StatisticsUiState(
    val trendData: List<DayCount> = emptyList(),
    val typeDistribution: List<TypeCount> = emptyList(),
    val hourlyDistribution: List<HourCount> = emptyList(),
    val totalEntries: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val peakHour: Int = -1,
    val isLoading: Boolean = true,
)

/**
 * Time range options for the trend chart.
 */
enum class TrendRange(val days: Int, val label: String) {
    WEEK(7, "7天"),
    MONTH(30, "30天"),
    QUARTER(90, "90天"),
}
