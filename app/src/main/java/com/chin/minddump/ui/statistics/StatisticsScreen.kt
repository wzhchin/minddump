package com.chin.minddump.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.rememberPremiumHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val trendRange by viewModel.trendRange.collectAsState()
    val haptics = rememberPremiumHaptics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section 1: Key Metrics
            item {
                Text(
                    text = "概览",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                KeyMetricsCard(
                    totalEntries = uiState.totalEntries,
                    currentStreak = uiState.currentStreak,
                    longestStreak = uiState.longestStreak,
                    peakHour = uiState.peakHour,
                    isLoading = uiState.isLoading,
                )
            }

            // Section 2: Trend Chart
            item {
                Text(
                    text = "记录趋势",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                TrendChart(
                    data = uiState.trendData,
                    selectedRange = trendRange,
                    onRangeSelected = { viewModel.setTrendRange(it) },
                    isLoading = uiState.isLoading,
                )
            }

            // Section 3: Type Distribution
            item {
                Text(
                    text = "类型分布",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                TypeDistributionChart(
                    data = uiState.typeDistribution,
                    isLoading = uiState.isLoading,
                )
            }

            // Section 4: Calendar Heatmap
            item {
                Text(
                    text = "日历",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                CalendarHeatmap(
                    dayCounts = uiState.trendData,
                    isLoading = uiState.isLoading,
                )
            }
        }
    }
}
