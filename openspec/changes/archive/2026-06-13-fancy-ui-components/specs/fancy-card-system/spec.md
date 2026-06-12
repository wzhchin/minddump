## ADDED Requirements

### Requirement: Type-differentiated card visuals
The system SHALL render entry cards with distinct visual treatments based on entry type (text, photo, video, audio), so users can distinguish content at a glance.

#### Scenario: Text entry card
- **WHEN** a text-only entry is displayed in the list
- **THEN** the card SHALL show a 4dp-wide vertical gradient accent bar on the left edge (primary→tertiary color), the entry text, relative timestamp, and no media placeholder

#### Scenario: Photo entry card
- **WHEN** a photo entry is displayed in the list
- **THEN** the card SHALL show the photo thumbnail at the top (180dp height, rounded top corners), a bottom gradient overlay (transparent→surfaceContainerLowest), and text content below the image

#### Scenario: Audio entry card
- **WHEN** an audio entry is displayed in the list
- **THEN** the card SHALL show a waveform visualization decoration (5 animated bars) on the left, audio duration label, and text content; the waveform bars SHALL pulse when the entry is currently playing

#### Scenario: Video entry card
- **WHEN** a video entry is displayed in the list
- **THEN** the card SHALL show the video thumbnail with a centered play button overlay (circular, 48dp, semi-transparent surface), video duration badge in the bottom-right corner, and text content below

### Requirement: Card elevation and shadow animation
The system SHALL animate card elevation changes to create depth perception during interaction.

#### Scenario: Card idle state
- **WHEN** an entry card is in its default non-pressed state
- **THEN** the card SHALL have a shadow elevation of 1dp with tonal surface coloring

#### Scenario: Card pressed state
- **WHEN** user presses down on an entry card
- **THEN** the shadow elevation SHALL animate from 1dp to 4dp over 150ms, and the card SHALL scale to 0.97

#### Scenario: Card long-press state
- **WHEN** user long-presses an entry card for 500ms
- **THEN** the card SHALL elevate to 8dp with a subtle glow effect (primary color at 10% alpha behind the card)

### Requirement: Card content layout
The system SHALL arrange card content with consistent spacing, typography hierarchy, and truncation rules.

#### Scenario: Long text truncation
- **WHEN** an entry's text content exceeds 4 lines
- **THEN** the card SHALL display 3 lines of text followed by an ellipsis with a "展开" (expand) indicator

#### Scenario: Timestamp display
- **WHEN** an entry card is displayed
- **THEN** the card SHALL show a relative timestamp (e.g., "3分钟前", "昨天") using labelSmall typography in onSurfaceVariant color, aligned to the bottom-end of the card

#### Scenario: Encryption indicator
- **WHEN** the entry is stored encrypted
- **THEN** the card SHALL display a small lock icon (16dp) in the top-right corner with onSurfaceVariant alpha 0.5

### Requirement: Card staggered entrance animation
The system SHALL animate cards entering the viewport with a staggered animation to create a cascading visual effect.

#### Scenario: Scroll reveal
- **WHEN** an entry card scrolls into the visible area
- **THEN** the card SHALL animate in with a 200ms fadeIn + slideUp (20dp) transition using decelerate easing

#### Scenario: Batch insertion
- **WHEN** multiple entries are loaded at once (e.g., search results)
- **THEN** cards SHALL stagger their entrance animations by 30ms per item, up to a maximum of 10 items animated; remaining items appear instantly
