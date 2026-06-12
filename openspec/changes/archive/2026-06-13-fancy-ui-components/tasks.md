## 1. Theme Foundation

- [x] 1.1 Create `GradientColors` data class and `LocalGradientColors` CompositionLocal with primaryGradient, cardGradient, inputGradient color pairs
- [x] 1.2 Create `AnimationDuration` data class and `LocalAnimationDuration` CompositionLocal (short=150ms, medium=300ms, long=500ms)
- [x] 1.3 Create `MotionCurve` data class and `LocalMotionCurve` CompositionLocal (emphasize, standard, decelerate easing curves)
- [x] 1.4 Implement reduced-motion detection via `AccessibilityManager` that overrides durations to 0 and curves to LinearEasing
- [x] 1.5 Wire all new CompositionLocals into the existing `MindDumpTheme` composable with light/dark variants

## 2. Animation Infrastructure

- [x] 2.1 Create `AnimatePressScale` Modifier factory that scales to 0.97 on press and springs back on release (100ms)
- [x] 2.2 Create `ShimmerEffect` Modifier using `drawWithContent` + `Brush.linearGradient` with 1500ms sweep cycle
- [x] 2.3 Create `PulseAnimation` composable using `infiniteTransition` for recording indicator (scale 1.0→1.3, 800ms loop)
- [x] 2.4 Create `StaggeredEntrance` composable that delays `AnimatedVisibility` by index * 30ms (max 10 items)
- [x] 2.5 Create `SwipeToDismissBox` composable with 200dp threshold, red delete background, and spring-back on partial swipe

## 3. Card System Overhaul

- [x] 3.1 Refactor `EntryItem` to support type-differentiated visuals: text (gradient accent bar), photo (image + overlay), audio (waveform bars), video (thumbnail + play overlay)
- [x] 3.2 Add `Modifier.graphicsLayer` shadow elevation animation (1dp→4dp) on press with `animateFloatAsState`
- [x] 3.3 Implement text truncation with 3-line limit + "展开" expand indicator
- [x] 3.4 Add lock icon (16dp, onSurfaceVariant 50% alpha) in top-right corner for encrypted entries
- [x] 3.5 Add scroll-reveal entrance animation per card using `AnimatedVisibility(fadeIn + slideInVertically, 200ms)`
- [x] 3.6 Implement staggered batch entrance with `StaggeredEntrance` for search results / initial load

## 4. Input Bar Enhancement

- [x] 4.1 Define `InputBarState` sealed interface (Collapsed, Expanded, Recording, Sending) with state properties
- [x] 4.2 Implement `updateTransition`-driven layout changes: text field expand/collapse, action buttons reveal/hide (300ms)
- [x] 4.3 Add `PulseAnimation` for recording state indicator with ripple effect (every 1200ms)
- [x] 4.4 Implement send feedback: circular progress → checkmark scale-in (200ms) on the send button
- [x] 4.5 Add typing bounce animation on send button (scale 1.0→1.05→1.0, 100ms) per character typed
- [x] 4.6 Implement space switch button 180° Y-axis rotation animation (400ms) using `graphicsLayer { rotationY }`
- [x] 4.7 Wire recording state into existing `AudioRecorder` flow so UI reflects actual recording lifecycle

## 5. List & Navigation Animations

- [x] 5.1 Add `SharedTransitionLayout` wrapper in main navigation host for list↔fullscreen-edit shared element
- [x] 5.2 Apply `Modifier.sharedElement()` to entry card and fullscreen edit content with 400ms emphasize transition
- [x] 5.3 Implement screen crossfade (300ms) for camera↔main navigation using `AnimatedNavHost` or `Crossfade`
- [x] 5.4 Add `animateItemPlacement()` to LazyColumn items for smooth position shifts on insert/delete
- [x] 5.5 Implement delete animation: `AnimatedVisibility(fadeOut + shrinkVertically, 250ms)` + Snackbar with "撤销"
- [x] 5.6 Wire undo action in Snackbar to restore deleted entry and animate it back into list

## 6. Search Bar Animation

- [x] 6.1 Refactor search UI into collapsed (icon-only) ↔ expanded (full text field) states
- [x] 6.2 Implement container transform animation (width, corner radius, elevation) with 300ms standard easing
- [x] 6.3 Add top app bar scroll hide/show with `TopAppBarScrollBehavior` (250ms slide animation)
- [x] 6.4 Animate top app bar background color transition on search mode activation (200ms)

## 7. Loading & Empty States

- [x] 7.1 Create `EntrySkeletonCard` composable matching text/photo card layouts with `ShimmerEffect` modifier
- [x] 7.2 Create `SkeletonEntryList` composable showing 5 varied skeleton cards during loading
- [x] 7.3 Implement crossfade transition from skeleton list to real content (300ms, no layout jump)
- [x] 7.4 Create empty state composable with thought-bubble icon + floating animation (translateY oscillation, 2000ms)
- [x] 7.5 Create no-search-results empty state with magnifying glass icon
- [x] 7.6 Add staggered entrance animation for empty state elements (icon → headline → subtext, 100ms delays)

## 8. Polish & Testing

- [x] 8.1 Verify all animations respect reduced-motion accessibility setting on API 29+
- [ ] 8.2 Profile animation performance with Android Studio Profiler — ensure 60fps, no jank on mid-range devices
- [x] 8.3 Run Detekt + ktlint on all new/modified files and fix violations
- [ ] 8.4 Add Compose UI tests for key animation states (InputBarState transitions, swipe-to-dismiss, skeleton→content)
- [ ] 8.5 Manual QA: verify light/dark theme, dynamic colors, and all entry types render correctly with new animations
