## ADDED Requirements

### Requirement: 12 haptic patterns
The system SHALL define 12 distinct haptic feedback patterns: Tick, Pop, Thud, Buildup, Success, Error, DragStart, DragEnd, Send, ScrollEdge, Selection, Cancel.

#### Scenario: Pattern definitions use Vibrator API
- **WHEN** a haptic pattern is performed
- **THEN** the system SHALL use Android's `VibrationEffect` predefined effects on API 29+ and `Vibrator` waveform patterns on API 26+, with `HapticFeedback` fallback for older devices

#### Scenario: Each pattern produces distinct tactile feedback
- **WHEN** two different patterns are performed in sequence
- **THEN** the user SHALL be able to distinguish them by feel (different intensity, duration, or waveform)

### Requirement: PremiumHaptics composable provider
The system SHALL provide a `rememberPremiumHaptics()` composable that returns a `PremiumHaptics` instance bound to the device's vibrator service.

#### Scenario: Haptics instance creation
- **WHEN** a composable calls `rememberPremiumHaptics()`
- **THEN** it SHALL return a cached instance that survives recomposition

#### Scenario: Device without vibrator
- **WHEN** the device has no vibrator hardware
- **THEN** all haptic calls SHALL be no-ops without crashing

### Requirement: Haptic patterns mapped to interactions
The system SHALL apply haptic feedback to the following interactions:

| Interaction | Pattern |
|---|---|
| Button tap, list item click | Tick |
| Toggle, confirm, space switch | Pop |
| Delete entry | Thud |
| Long-press detection | Buildup |
| Entry saved successfully | Success |
| Error states | Error |
| Drag/swipe start | DragStart |
| Drag/swipe end | DragEnd |
| Send message/note | Send |
| Scroll to list edge | ScrollEdge |
| Tab/segment change | Selection |
| Dismiss, cancel | Cancel |

#### Scenario: Send button pressed
- **WHEN** user taps the send button
- **THEN** the system SHALL perform the `Send` haptic pattern

#### Scenario: Entry deleted
- **WHEN** user confirms entry deletion
- **THEN** the system SHALL perform the `Thud` haptic pattern

#### Scenario: Space toggled
- **WHEN** user taps the space switch button
- **THEN** the system SHALL perform the `Pop` haptic pattern

### Requirement: Reduce motion disables haptics
The system SHALL respect the system's animation/transition scale settings.

#### Scenario: Animations disabled in system settings
- **WHEN** the user has disabled animations in Android accessibility settings
- **THEN** haptic feedback SHALL still function (haptics are not animations — they serve a different accessibility purpose)

#### Scenario: Vibrator unavailable
- **WHEN** the device vibrator service is unavailable or throws an exception
- **THEN** the system SHALL silently skip the haptic without crashing
