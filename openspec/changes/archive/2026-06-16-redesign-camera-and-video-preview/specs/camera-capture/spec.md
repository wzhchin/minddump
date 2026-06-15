## ADDED Requirements

### Requirement: Capture screen follows the expressive theme system
The capture screen SHALL use the app's M3 Expressive design tokens — expressive shape tokens, premium haptics, and motion durations/curves — for all of its controls and state transitions, so it is visually and tactilely consistent with every other screen rather than using raw default components with hardcoded dimensions.

#### Scenario: Controls use expressive shapes and haptics
- **WHEN** the user opens the capture screen and interacts with any control (mode toggle, shutter, switch camera, flash)
- **THEN** each control is rendered with the expressive shape tokens (e.g. pill/rounded button shapes) instead of raw default chip/button styling
- **AND** each interaction emits the premium haptic feedback pattern used elsewhere in the app
- **AND** state transitions (idle → recording, switching camera) animate using the shared motion duration and curve tokens

### Requirement: Photo and video mode selection
The capture screen SHALL let the user choose between Photo and Video mode via a segmented control, and SHALL render the shutter control appropriately for the active mode.

#### Scenario: Photo mode is the default
- **WHEN** the user opens the capture screen
- **THEN** Photo mode is active and the shutter shows a capture affordance

#### Scenario: Switching to video mode
- **WHEN** the user selects the Video segment
- **THEN** the shutter becomes a record affordance
- **AND** selecting Photo again cancels any in-progress recording state

### Requirement: Video recording shows an elapsed timer and recording state
The capture screen SHALL, while a video is recording, display a live elapsed timer and a distinct recording visual state on the shutter, so the user can see that recording is in progress and how long it has lasted.

#### Scenario: Timer counts up while recording
- **WHEN** the user starts a video recording
- **THEN** the shutter switches to a stop affordance with a recording indicator (e.g. a red ring)
- **AND** an elapsed timer in `mm:ss` format is displayed and increments for as long as recording continues

#### Scenario: Timer stops on stop
- **WHEN** the user stops the recording
- **THEN** the timer stops, the capture is finalized, and the screen returns to the feed

### Requirement: Capture screen chrome
The capture screen SHALL provide a top bar with a close affordance and a flash toggle, plus a camera-switch affordance, so the user can dismiss the screen and toggle camera-facing and flash without hunting for controls.

#### Scenario: Closing the capture screen
- **WHEN** the user taps close
- **THEN** the camera preview is released and the user returns to the feed

#### Scenario: Switching facing camera
- **WHEN** the user taps the switch-camera affordance
- **THEN** the preview rebinds to the opposite camera (front/back)
