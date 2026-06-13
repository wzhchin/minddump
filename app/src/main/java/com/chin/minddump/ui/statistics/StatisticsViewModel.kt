package com.chin.minddump.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chin.minddump.data.DayCount
import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.storage.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel
    @Inject
    constructor(
        private val repository: MindDumpRepository,
    ) : ViewModel() {

        private val space = Space.PUBLIC

        private val _trendRange = MutableStateFlow(TrendRange.WEEK)
        val trendRange: StateFlow<TrendRange> = _trendRange.asStateFlow()

        val uiState: StateFlow<StatisticsUiState> = combine(
            _trendRange,
            repository.getEntryCountByDay(space, 90),
            repository.getEntryCountByType(space),
            repository.getHourlyDistribution(space),
            repository.countFlow(space),
        ) { range, dayCounts, typeCounts, hourCounts, total ->
            val filteredTrend = filterTrendByRange(dayCounts, range)
            val streaks = computeStreaks(dayCounts)
            val peakHour = hourCounts.maxByOrNull { it.count }?.hour ?: -1

            StatisticsUiState(
                trendData = filteredTrend,
                typeDistribution = typeCounts,
                hourlyDistribution = hourCounts,
                totalEntries = total,
                currentStreak = streaks.first,
                longestStreak = streaks.second,
                peakHour = peakHour,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState(),
        )

        fun setTrendRange(range: TrendRange) {
            _trendRange.value = range
        }

        private fun filterTrendByRange(
            dayCounts: List<DayCount>,
            range: TrendRange,
        ): List<DayCount> {
            val cutoff = LocalDate.now().minusDays(range.days.toLong())
            return dayCounts
                .filter {
                    try {
                        // monthFolder is YYYY-MM, parse as year-month for comparison
                        val yearMonth = java.time.YearMonth.parse(it.monthFolder)
                        val firstOfMonth = yearMonth.atDay(1)
                        firstOfMonth >= cutoff
                    } catch (_: Exception) {
                        false
                    }
                }.map { DayCount(it.monthFolder, it.count) }
        }

        /**
         * Compute current streak and longest streak from month counts.
         * Streak = consecutive months with at least 1 entry.
         */
        private fun computeStreaks(dayCounts: List<DayCount>): Pair<Int, Int> {
            val monthsWithEntries = dayCounts
                .filter { it.count > 0 }
                .mapNotNull {
                    try {
                        java.time.YearMonth.parse(it.monthFolder)
                    } catch (_: Exception) {
                        null
                    }
                }.sortedDescending()
                .toSet()

            if (monthsWithEntries.isEmpty()) return Pair(0, 0)

            // Current streak: count backwards from current month
            var currentStreak = 0
            var checkMonth = java.time.YearMonth.now()
            while (checkMonth in monthsWithEntries) {
                currentStreak++
                checkMonth = checkMonth.minusMonths(1)
            }

            // Longest streak: find max consecutive run
            val sortedMonths = monthsWithEntries.sorted()
            var longestStreak = 1
            var runLength = 1
            for (i in 1 until sortedMonths.size) {
                if (java.time.temporal.ChronoUnit.MONTHS
                        .between(sortedMonths[i - 1], sortedMonths[i]) == 1L) {
                    runLength++
                    longestStreak = maxOf(longestStreak, runLength)
                } else {
                    runLength = 1
                }
            }

            return Pair(currentStreak, longestStreak)
        }
    }
