## ADDED Requirements

### Requirement: Shimmer loading skeleton
The system SHALL display animated shimmer placeholders while entry data is loading, replacing blank or static loading indicators.

#### Scenario: Initial load skeleton
- **WHEN** the entry list screen is opened and entries are being loaded from the database
- **THEN** 5 skeleton card placeholders SHALL appear with a shimmer animation (gradient sweep left→right, 1500ms cycle, primary color at 20% alpha), each matching the approximate size of a real entry card

#### Scenario: Skeleton type variation
- **WHEN** skeleton cards are displayed
- **THEN** each skeleton SHALL randomly vary between text-only layout and photo layout (with a gray rectangle placeholder for the image area) to simulate the mixed content types in the real list

#### Scenario: Skeleton to content transition
- **WHEN** entry data finishes loading
- **THEN** skeleton items SHALL crossfade to real content with a 300ms animation; no layout jump SHALL be visible

### Requirement: Empty state illustration
The system SHALL display a branded illustration and message when the entry list is empty.

#### Scenario: No entries (fresh install)
- **WHEN** the user opens the app for the first time and has no entries
- **THEN** the system SHALL display a centered illustration (a thought bubble icon with a subtle floating animation), a headline "开始记录你的想法", and a subtext "点击下方输入栏，写下第一条想法"

#### Scenario: No search results
- **WHEN** user performs a search that returns no results
- **THEN** the system SHALL display a different illustration (a magnifying glass icon), headline "未找到结果", and subtext "试试其他关键词"

#### Scenario: Empty state entrance animation
- **WHEN** an empty state illustration is displayed
- **THEN** the illustration SHALL fade in and float down from -20dp over 500ms with decelerate easing, followed by the text elements staggering in with 100ms delays

### Requirement: Brand gradient theme extensions
The system SHALL extend the theme system with branded gradient colors and animation constants accessible via CompositionLocal.

#### Scenario: Gradient color access
- **WHEN** a composable needs a gradient color
- **THEN** it SHALL access `LocalGradientColors.current` which provides `primaryGradient` (primary→tertiary), `cardGradient` (surfaceContainerLow→surfaceContainer), and `inputGradient` (surfaceContainerHigh→surfaceContainerHighest)

#### Scenario: Animation duration access
- **WHEN** a composable needs an animation duration
- **THEN** it SHALL access `LocalAnimationDuration.current` which provides `short` (150ms), `medium` (300ms), `long` (500ms)

#### Scenario: Motion curve access
- **WHEN** a composable needs an easing curve
- **THEN** it SHALL access `LocalMotionCurve.current` which provides `emphasize` (CubicBezierEasing(0.2f, 0f, 0f, 1f)), `standard` (CubicBezierEasing(0.2f, 0f, 0f, 1f)), `decelerate` (CubicBezierEasing(0f, 0f, 0f, 1f))

#### Scenario: Reduced motion override
- **WHEN** the device accessibility setting "Remove animations" is enabled
- **THEN** `LocalAnimationDuration` SHALL return 0 for all durations, and `LocalMotionCurve` SHALL return LinearEasing for all curves

### Requirement: Top app bar animation
The system SHALL animate the top app bar in response to scroll and contextual state changes.

#### Scenario: Scroll hide/show
- **WHEN** user scrolls down through the entry list
- **THEN** the top app bar SHALL collapse with a 250ms slide-up animation; when user scrolls back up, it SHALL reappear with a 250ms slide-down animation

#### Scenario: Search mode visual change
- **WHEN** search mode is activated
- **THEN** the top app bar background SHALL animate from transparent to surfaceContainer color over 200ms, and the title SHALL crossfade to a search text field

### Requirement: Snackbar animation
The system SHALL display snackbars with entrance and exit animations.

#### Scenario: Snackbar entrance
- **WHEN** a snackbar is triggered (e.g., after deleting an entry)
- **THEN** the snackbar SHALL slide up from the bottom of the screen with a 300ms decelerate animation

#### Scenario: Snackbar exit
- **WHEN** a snackbar is dismissed (timeout, swipe, or action)
- **THEN** the snackbar SHALL slide down and fade out with a 250ms animation
