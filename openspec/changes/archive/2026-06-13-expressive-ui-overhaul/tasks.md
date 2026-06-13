## 1. Foundation — Font & Theme

- [x] 1.1 Copy `google_sans_flex.ttf` from `~/repos/LastChat/app/src/main/res/font/` to `app/src/main/res/font/google_sans_flex.ttf`
- [x] 1.2 Create `ui/theme/Type.kt` — define `GoogleSansFlexExpressive` FontFamily with ROND=100, weight ladder (Light→ExtraBold), wider width for display/headline. Create `createTypography()` function with full M3 scale. Include API 26 fallback.
- [x] 1.3 Update `ui/Theme.kt` — remove inline `AppTypography`, import from `Type.kt`, pass to `MaterialTheme(typography = ...)`
- [x] 1.4 Create `ui/theme/Shape.kt` — define `ExpressiveShapes` data class (cardLarge=28dp, cardMedium=24dp, cardSmall=16dp, buttonPill=CircleShape, buttonRounded=20dp, buttonSquared=12dp, inputField=20dp, chip=12dp). Add `LocalExpressiveShapes` CompositionLocal.
- [x] 1.5 Update `ui/Theme.kt` — provide `LocalExpressiveShapes` in CompositionLocalProvider block

## 2. Foundation — Haptics & Press Feedback

- [x] 2.1 Create `ui/theme/PremiumHaptics.kt` — `HapticPattern` sealed class with 12 patterns (Tick, Pop, Thud, Buildup, Success, Error, DragStart, DragEnd, Send, ScrollEdge, Selection, Cancel). `PremiumHaptics` class with `perform(pattern)` using Vibrator API. `rememberPremiumHaptics()` composable.
- [x] 2.2 Update `ui/theme/FancyAnimations.kt` — change `animatePressScale()` default to 0.92f. Add `noRippleClickable()` modifier extension that combines `clickable(indication=null)` + press scale + haptics.
- [x] 2.3 Update `ui/theme/FancyAnimations.kt` — add `asymmetricCardShape()` helper returning `RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)` for entry cards

## 3. Entry Card Redesign

- [x] 3.1 Rewrite `ui/EntryItem.kt` — new unified layout: top row with circular type-icon avatar + relative timestamp, content area below per entry type
- [x] 3.2 Add type icon mapping — TEXT→Edit, PHOTO→Image/Camera, RECORDING→Mic, VIDEO→Videocam, FILE→AttachFile, UNKNOWN→Help
- [x] 3.3 Add relative timestamp formatting — "刚刚" / "X分钟前" / "X小时前" / "昨天 HH:mm" / "MM月DD日 HH:mm"
- [x] 3.4 Apply asymmetric corners to entry cards using shape tokens
- [x] 3.5 Wire `noRippleClickable()` + haptics (Tick on tap, Buildup on long-press) to entry cards
- [x] 3.6 Update photo entry — Coil thumbnail with 4:3 aspect ratio, rounded corners (optical nesting)
- [x] 3.7 Update audio entry — content preview + animated waveform bars
- [x] 3.8 Update video entry — thumbnail + centered play overlay
- [x] 3.9 Update file entry — file name + file type icon
- [x] 3.10 Update `ui/SkeletonAndEmptyStates.kt` — use new card shape tokens for skeleton items

## 4. Top Bar Redesign

- [x] 4.1 Update `ui/MainScreen.kt` — remove `CenterAlignedTopAppBar` title entirely (no app name, no space name)
- [x] 4.2 Update top bar actions — show exactly 3 floating icon buttons: Search, Statistics (bar chart icon), Settings
- [x] 4.3 Apply `noRippleClickable()` + haptics (Tick) to all top bar icon buttons
- [x] 4.4 Apply shape tokens to search field (inputField=20dp or pill shape)

## 5. Input Bar & Components Update

- [x] 5.1 Update `ui/InputBar.kt` — apply shape tokens to text field, action buttons; wire haptics (Send on submit, Pop on record toggle, Tick on camera/import)
- [x] 5.2 Update `ui/SpaceSwitchButton.kt` — wire Pop haptic on space toggle
- [x] 5.3 Update all dialog components (`ui/components/*.kt`) — apply shape tokens, wire Tick haptic on confirm/cancel buttons

## 6. Statistics — Data Layer

- [x] 6.1 Add DAO queries in `data/EntryDao.kt` — `getEntryCountByDay(space, limit)`: Flow of date→count pairs; `getEntryCountByType(space)`: Flow of type→count pairs; `getStreakInfo(space)`: Flow of current+longest streak; `getHourlyDistribution(space)`: Flow of hour→count pairs
- [x] 6.2 Create `ui/statistics/StatisticsUiState.kt` — data classes for trend data, type distribution, metrics, heatmap data
- [x] 6.3 Create `ui/statistics/StatisticsViewModel.kt` — Hilt-injected, collects DAO flows, computes metrics, exposes StateFlow<StatisticsUiState>
- [x] 6.4 Add Hilt module binding for StatisticsViewModel if needed

## 7. Statistics — UI

- [x] 7.1 Create `ui/statistics/StatisticsScreen.kt` — Scaffold with top bar (back arrow + "统计" title), scrollable content with 4 sections
- [x] 7.2 Create `ui/statistics/TrendChart.kt` — Canvas-drawn bar chart with 7/30/90 day toggle, using MaterialTheme colors
- [x] 7.3 Create `ui/statistics/TypeDistributionChart.kt` — Canvas-drawn donut chart with type colors and labels
- [x] 7.4 Create `ui/statistics/KeyMetricsCard.kt` — metric cards (total entries, current streak, longest streak, peak hour)
- [x] 7.5 Create `ui/statistics/CalendarHeatmap.kt` — Canvas-drawn month grid with color intensity by count, month navigation
- [x] 7.6 Update `ui/MindDumpNavGraph.kt` — add `Screen.Statistics("statistics")` route, wire `StatisticsScreen` composable, pass `onNavigateToStatistics` callback to MainScreen

## 8. Polish & Verification

- [x] 8.1 Run Detekt + ktlint on all new/modified files
- [x] 8.2 Build and run app on device/emulator — verify all screens render correctly
- [ ] 8.3 Test each entry type card layout (text, photo, audio, video, file)
- [ ] 8.4 Test haptics on physical device (emulator doesn't vibrate)
- [ ] 8.5 Test statistics screen with empty database (zero entries)
- [ ] 8.6 Test statistics screen with populated data
- [ ] 8.7 Verify reduce-motion mode: animations collapse, haptics still work
- [ ] 8.8 Verify dark theme renders correctly on all new components
