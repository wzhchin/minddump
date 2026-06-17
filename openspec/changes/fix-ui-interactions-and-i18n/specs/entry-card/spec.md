## MODIFIED Requirements

### Requirement: Type-icon header with relative timestamp
The system SHALL show, at the top of every entry card, a circular icon avatar colored by entry type alongside a relative timestamp, so the user can scan heterogeneous content (text/photo/audio/video/file) at a glance.

> i18n is out of scope this round; the relative-timestamp wording stays zh-CN. Only the lock-indicator source-of-truth is changed here.

#### Scenario: Header identifies entry type and time
- **WHEN** an entry card is rendered
- **THEN** its header shows an icon whose shape and color indicate the entry type (text, photo, recording, video, file)
- **AND** it shows a human-readable relative timestamp (e.g. "刚刚", "5分钟前", "今天 14:30")

#### Scenario: Encrypted entries show a lock indicator from a parsed property
- **WHEN** an encrypted entry card is rendered (outside multi-select)
- **THEN** the header shows a lock indicator signaling the entry is encrypted
- **AND** the indicator reflects a parsed encryption property, not a filename substring guess

### Requirement: Per-type card content
The system SHALL render the card body according to entry type, with media types given a generous, cinematic media region. Long text bodies SHALL collapse to a few lines with a working, animated expand/collapse affordance.

#### Scenario: Text entry collapses with a working expand affordance
- **WHEN** a text entry whose body exceeds the collapse threshold is rendered
- **THEN** the card body shows the text collapsed to a few lines
- **AND** it shows an expand affordance that, when tapped, expands the body to full height with an animated transition
- **AND** while expanded the affordance shows a collapse label (收起)
- **AND** tapping the expand/collapse affordance toggles the body only and does NOT navigate into the entry

#### Scenario: Short text entry shows no expand affordance
- **WHEN** a text entry whose body is at or below the collapse threshold is rendered
- **THEN** the card body shows the full text with no expand affordance

#### Scenario: Text preview does not flash the file name
- **WHEN** a text entry card is rendered and its content is still loading
- **THEN** the body shows a loading/empty placeholder rather than the file name

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

### Requirement: Type-icon colors use non-semantic tokens
The system SHALL color the type-icon avatar per entry type using theme tokens that are NOT reserved for error/warning semantics for non-error categories. In particular, the video type SHALL NOT use the error color token.

#### Scenario: Video category color is not the error token
- **WHEN** a video entry card header is rendered
- **THEN** the type-icon avatar uses a non-error category color
- **AND** no entry-type avatar in a non-error context uses the error color token

## ADDED Requirements

### Requirement: Feed list uses idiomatic item animation
The system SHALL animate feed list items using the LazyColumn item-scoped animation primitive so that inserts, moves, and removals animate fluidly, without re-firing an entrance animation merely because an item scrolled into view, and without dead animation specifications.

#### Scenario: Items animate on reorder and insert
- **WHEN** entries are inserted, reordered (e.g. by editing an entry which moves it to the top), or removed
- **THEN** the affected list items animate to their new positions
- **AND** items that merely scroll into view do not replay an entrance animation

#### Scenario: No dead animation specifications
- **WHEN** the feed list is rendered
- **THEN** every declared animation specification corresponds to a transition that actually occurs (no permanently-true visibility wrapper with an unreachable exit specification)
