# Entry Card

## Purpose

Define how individual entries (text, photo, recording, video, file) are rendered as expressive cards in the MindDump feed, including visual presentation, type indicators, content rendering, and interaction patterns.

## Requirements

### Requirement: Single entry renders as an expressive card
The system SHALL render each entry (text, photo, recording, video, file) as a card with a calm surface tone, large rounded corners, light tonal elevation, and a subtle border, rather than as a chat bubble.

#### Scenario: Entry appears as a card
- **WHEN** any entry is shown in the feed
- **THEN** it is presented as a card with a rounded-rectangle shape and a quiet surface tone
- **AND** it has a visible-but-subtle border and a light elevation that separates it from the background
- **AND** it does not resemble a left/right-aligned chat bubble

#### Scenario: Multi-select still works on the card
- **WHEN** the user enters multi-select mode
- **THEN** each card shows a selection affordance and tapping a card toggles its selection
- **AND** the card layout remains stable (no layout jump beyond the selection affordance)

### Requirement: Type-icon header with relative timestamp
The system SHALL show, at the top of every entry card, a circular icon avatar colored by entry type alongside a relative timestamp, so the user can scan heterogeneous content (text/photo/audio/video/file) at a glance.

#### Scenario: Header identifies entry type and time
- **WHEN** an entry card is rendered
- **THEN** its header shows an icon whose shape and color indicate the entry type (text, photo, recording, video, file)
- **AND** it shows a human-readable relative timestamp (e.g. "刚刚", "5分钟前", "今天 14:30")

#### Scenario: Encrypted entries show a lock indicator
- **WHEN** an encrypted entry card is rendered (outside multi-select)
- **THEN** the header shows a lock indicator signaling the entry is encrypted

### Requirement: Per-type card content
The system SHALL render the card body according to entry type, with media types given a generous, cinematic media region.

#### Scenario: Text entry
- **WHEN** a text entry card is rendered
- **THEN** the card body shows the text, collapsed to a few lines by default with an expand affordance when long

#### Scenario: Photo entry
- **WHEN** a photo entry card is rendered
- **THEN** the card body shows the image in a large rounded media region that fills the card width
- **AND** tapping the image opens the full-screen zoomable preview

#### Scenario: Recording entry
- **WHEN** an audio recording card is rendered
- **THEN** the card body shows a document/media chip representing the recording

#### Scenario: Video entry
- **WHEN** a video card is rendered
- **THEN** the card body shows the video thumbnail in a large rounded media region with a play overlay

#### Scenario: Generic file entry
- **WHEN** a generic file card is rendered
- **THEN** the card body shows a file chip with the file name

### Requirement: Card interactions preserved
The system SHALL preserve the existing tap and long-press interactions on the card (open entry, open action menu), with tactile feedback.

#### Scenario: Tap opens the entry
- **WHEN** the user taps an entry card (outside multi-select)
- **THEN** the entry opens for viewing/editing as before

#### Scenario: Long-press opens the action menu
- **WHEN** the user long-presses an entry card
- **THEN** the entry action menu opens as before
- **AND** a buildup tactile feedback is emitted
