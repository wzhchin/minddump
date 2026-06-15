## ADDED Requirements

### Requirement: Tapping a video cover opens a full-screen in-app player
The system SHALL open a full-screen, in-app video player when the user taps a video cover (an entry card's video thumbnail or a video tile in a group media carousel), so playback stays inside the app rather than handing off to an external viewer.

#### Scenario: Open from a video entry card
- **WHEN** the user taps the video cover on a video entry card (outside multi-select)
- **THEN** a full-screen in-app player opens and begins playing that video

#### Scenario: Open from a group carousel video tile
- **WHEN** the user taps a video tile in a group media carousel
- **THEN** a full-screen in-app player opens and begins playing that video

### Requirement: Player provides standard transport controls
The in-app player SHALL present standard transport controls (play/pause, seek/scrub, and a progress indicator) over the video, auto-hidden when idle, so the user can control playback.

#### Scenario: Controls are available and dismissible
- **WHEN** the player is open and playing
- **THEN** transport controls are available to pause, resume, and seek
- **AND** the controls hide when idle and reappear on tap

### Requirement: Player releases media resources on close
The system SHALL fully release the underlying media player and its resources whenever the player view is dismissed (back, tap-outside, or navigating away), so no media resources or battery drain persist after the player closes.

#### Scenario: Closing the player releases resources
- **WHEN** the user dismisses the full-screen player
- **THEN** the media player is released immediately and playback stops
- **AND** reopening a video later creates a fresh player rather than resuming a leaked one
