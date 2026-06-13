## ADDED Requirements

### Requirement: Statistics screen with 4 modules
The system SHALL provide a statistics screen with 4 visual modules: time trend chart, type distribution chart, key metrics, and calendar heatmap.

#### Scenario: Screen accessible from top bar
- **WHEN** user taps the statistics icon in the main screen top bar
- **THEN** the app SHALL navigate to the statistics screen with a slide transition

#### Scenario: Back navigation
- **WHEN** user presses back or the top bar back arrow on the statistics screen
- **THEN** the app SHALL navigate back to the main screen

### Requirement: Time trend chart
The system SHALL display a line/bar chart showing entry counts over time.

#### Scenario: Default 7-day view
- **WHEN** the statistics screen opens
- **THEN** the trend chart SHALL show the last 7 days with entry counts per day

#### Scenario: Time range toggle
- **WHEN** user selects a different time range (7天 / 30天 / 90天)
- **THEN** the chart SHALL update to show entry counts for the selected range

#### Scenario: Empty days
- **WHEN** a day has zero entries
- **THEN** the chart SHALL still show that day with a zero-height bar or point at the baseline

### Requirement: Type distribution chart
The system SHALL display a chart showing the breakdown of entries by type (text, photo, audio, video, file).

#### Scenario: Pie/donut chart renders
- **WHEN** the statistics screen is displayed
- **THEN** a donut chart SHALL show the proportion of each entry type with distinct colors and labels

#### Scenario: Zero entries of a type
- **WHEN** an entry type has zero entries
- **THEN** that type SHALL NOT appear in the chart

#### Scenario: All entries same type
- **WHEN** all entries are of the same type
- **THEN** the chart SHALL show a full circle with that type's color and label

### Requirement: Key metrics dashboard
The system SHALL display key statistics as metric cards.

#### Scenario: Metrics displayed
- **WHEN** the statistics screen is shown
- **THEN** the following metrics SHALL be visible:
  - Total entries count
  - Current streak (consecutive days with at least 1 entry)
  - Longest streak
  - Most active hour (peak capture time)

#### Scenario: No entries yet
- **WHEN** the database has zero entries
- **THEN** all metrics SHALL show "—" or "0" gracefully

### Requirement: Calendar heatmap
The system SHALL display a calendar heatmap where each day cell is colored by entry count.

#### Scenario: Heatmap renders current month
- **WHEN** the statistics screen is displayed
- **THEN** a calendar grid SHALL show the current month with each day cell colored by entry count (0=empty, 1-2=light, 3-5=medium, 6+=dark)

#### Scenario: Month navigation
- **WHEN** user swipes or taps arrows
- **THEN** the heatmap SHALL navigate to the previous/next month

#### Scenario: Day tap shows detail
- **WHEN** user taps a day cell with entries
- **THEN** the system SHALL show a brief summary (entry count for that day)

### Requirement: Statistics data layer
The system SHALL provide statistics data through new DAO queries and a dedicated ViewModel.

#### Scenario: DAO aggregation queries
- **WHEN** the statistics screen requests data
- **THEN** the DAO SHALL execute SQL queries for:
  - Entry count grouped by dateFolder (trend + heatmap)
  - Entry count grouped by type (distribution)
  - Consecutive day streak calculation (metrics)
  - Hour-of-day distribution (peak hours)

#### Scenario: Reactive data updates
- **WHEN** new entries are added or deleted
- **THEN** the statistics screen SHALL update automatically via Room Flow

#### Scenario: Separate ViewModel
- **WHEN** the statistics screen is composed
- **THEN** it SHALL use its own `StatisticsViewModel` injected via Hilt, not the main `MindDumpViewModel`

### Requirement: Custom Canvas charts
All charts SHALL be drawn using Compose Canvas API without external charting libraries.

#### Scenario: No chart library dependency
- **WHEN** the app is built
- **THEN** the statistics screen SHALL NOT depend on any third-party chart library (Vico, MPAndroidChart, etc.)

#### Scenario: Charts respect theme colors
- **WHEN** charts are rendered
- **THEN** they SHALL use colors from `MaterialTheme.colorScheme` for bars, lines, and fills
