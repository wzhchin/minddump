## MODIFIED Requirements

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

#### Scenario: Video entry shows a decoded thumbnail
- **WHEN** a video card is rendered
- **THEN** the card body shows a real decoded first-frame thumbnail of the video (not a blank or black region) in a large rounded media region with a play overlay
- **AND** the same decoded thumbnail is rendered for video members shown in a group media carousel

#### Scenario: Tapping a video opens the in-app player
- **WHEN** the user taps a video entry's cover (the thumbnail / play overlay) outside multi-select
- **THEN** the video opens in the app's full-screen in-app video player (not an external app)

#### Scenario: Generic file entry
- **WHEN** a generic file card is rendered
- **THEN** the card body shows a file chip with the file name
