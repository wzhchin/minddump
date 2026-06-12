## ADDED Requirements

### Requirement: Page transition animations
The system SHALL animate transitions between screens using shared element transitions when navigating from entry list to fullscreen edit, and crossfade for other screen changes.

#### Scenario: Navigate from list to fullscreen edit
- **WHEN** user taps an entry card in the main list
- **THEN** the entry card content SHALL animate into the fullscreen edit screen using shared element transition, with a 400ms duration and emphasize easing

#### Scenario: Navigate from camera back to main
- **WHEN** user navigates back from camera screen to main screen
- **THEN** the screen SHALL crossfade with a 300ms duration and standard easing

#### Scenario: Reduced motion accessibility
- **WHEN** the device has "Remove animations" accessibility setting enabled
- **THEN** all page transitions SHALL be instant (0ms duration) with no motion

### Requirement: List item appearance animations
The system SHALL animate list items entering the visible area with a staggered fade-in and slide-up effect.

#### Scenario: Initial list load
- **WHEN** the entry list is first displayed with items
- **THEN** each item SHALL fade in (alpha 0→1) and slide up (offset 40dp→0) with a 300ms duration, staggered by 50ms per item, using decelerate easing

#### Scenario: New entry added to list
- **WHEN** a new entry is added at the top of the list
- **THEN** the new item SHALL animate in with fadeIn + expandVertically (250ms, emphasize easing) and existing items SHALL shift down with animateItemPlacement

#### Scenario: Entry deleted from list
- **WHEN** an entry is removed from the list
- **THEN** the item SHALL animate out with fadeOut + shrinkVertically (250ms, emphasize easing) and remaining items SHALL smoothly close the gap

### Requirement: Touch feedback animations
The system SHALL provide haptic-inspired visual feedback on interactive elements when pressed.

#### Scenario: Card press feedback
- **WHEN** user presses down on an entry card
- **THEN** the card SHALL scale to 0.97 with a 100ms animation and elevate shadow; on release, it SHALL spring back to 1.0

#### Scenario: Button press ripple
- **WHEN** user presses any icon button
- **THEN** the button SHALL show a bounded ripple effect matching the M3 theme color

### Requirement: Swipe-to-dismiss gesture
The system SHALL allow users to swipe an entry card horizontally to trigger deletion.

#### Scenario: Swipe right to delete
- **WHEN** user swipes an entry card to the right beyond 200dp threshold
- **THEN** the card SHALL animate off-screen, a red background with delete icon SHALL be revealed, and the entry SHALL be deleted with a 300ms shrinkVertically animation

#### Scenario: Partial swipe cancellation
- **WHEN** user swipes less than 200dp and releases
- **THEN** the card SHALL spring back to its original position with a 200ms animation

#### Scenario: Swipe with undo
- **WHEN** an entry is deleted via swipe
- **THEN** a Snackbar with "撤销" action SHALL appear for 5 seconds; if user taps "撤销", the entry SHALL be restored and animate back into the list

### Requirement: Search bar expand/collapse animation
The system SHALL animate the search bar expanding from a collapsed icon-only state to a full-width text input.

#### Scenario: Search bar expand
- **WHEN** user taps the search icon
- **THEN** the search bar SHALL expand with a 300ms container transform animation (width, corner radius, elevation change) using standard easing

#### Scenario: Search bar collapse
- **WHEN** user taps the back/clear button on an empty search bar
- **THEN** the search bar SHALL collapse back to the icon-only state with a 250ms animation
