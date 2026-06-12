## ADDED Requirements

### Requirement: Multi-state input bar with animated transitions
The system SHALL display an input bar that smoothly transitions between visual states: Collapsed, Expanded, Recording, and Sending.

#### Scenario: Collapsed to Expanded
- **WHEN** user taps the collapsed input bar
- **THEN** the input bar SHALL expand from a single-line prompt to a multi-line text field (max 4 lines) with a 300ms container transform animation, revealing action buttons (camera, microphone) below the text field

#### Scenario: Expanded to Recording
- **WHEN** user taps the microphone button
- **THEN** the input bar SHALL transition to recording state over 300ms: text field collapses, a waveform visualization appears, a pulsing red recording indicator animates continuously, and a timer counts elapsed seconds

#### Scenario: Recording to Collapsed
- **WHEN** user stops recording (tap stop button)
- **THEN** the input bar SHALL animate back to Collapsed state with a 250ms crossfade, the recording indicator fades out, and the audio entry appears at the top of the list

#### Scenario: Sending feedback
- **WHEN** user submits text and the entry is being saved
- **THEN** the send button SHALL show a circular progress indicator for up to 500ms, then transform into a checkmark icon with a 200ms scale-in animation before resetting

### Requirement: Recording pulse animation
The system SHALL display a continuous pulse animation on the recording indicator to provide clear visual feedback that audio is being captured.

#### Scenario: Recording active
- **WHEN** audio recording is in progress
- **THEN** a red circular indicator SHALL pulse with a scale animation between 1.0x and 1.3x over 800ms (repeating), and a ripple effect SHALL emanate outward from the indicator every 1200ms

#### Scenario: Recording paused
- **WHEN** audio recording is paused
- **THEN** the pulse animation SHALL stop and the indicator SHALL remain visible but static at 1.0x scale with 60% alpha, indicating a paused state

### Requirement: Typing feedback animation
The system SHALL provide subtle visual feedback as the user types.

#### Scenario: Character typed
- **WHEN** user types a character in the text field
- **THEN** the send button SHALL perform a subtle scale bounce (1.0→1.05→1.0) over 100ms if the text field is non-empty

#### Scenario: Text cleared
- **WHEN** user deletes all text from the input field
- **THEN** the send button SHALL fade to disabled state with a 150ms alpha animation and the microphone/camera buttons SHALL reappear with a slide-in from the right

### Requirement: Action button animations
The system SHALL animate the action buttons (camera, microphone, send) contextually based on input bar state.

#### Scenario: Camera button press
- **WHEN** user taps the camera button
- **THEN** the button SHALL scale down to 0.9 over 80ms, then scale back to 1.0 over 120ms with an overshoot curve, before navigating to the camera screen

#### Scenario: Microphone button hover
- **WHEN** user long-presses the microphone button without releasing
- **THEN** the button SHALL grow to 1.2x scale over 300ms with a growing ripple, and transition to recording state if held for 500ms

#### Scenario: Space switch animation
- **WHEN** user taps the space switch button in the input bar area
- **THEN** the button SHALL perform a 180° Y-axis rotation over 400ms, changing its icon and color to reflect the new space (public/private)
