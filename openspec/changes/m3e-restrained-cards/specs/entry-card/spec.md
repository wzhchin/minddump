## MODIFIED Requirements

### Requirement: Entry card surfaces use a calm, faithful Material 3 tonal treatment

An entry card SHALL render on a single calm tonal surface (e.g.
`surfaceContainerLow`) with a thin hairline border, a light two-layer elevation,
and the asymmetric rounded shape — relying on tonal elevation rather than a
gradient wash or heavy paint, so the feed reads as one quiet Material 3
Expressive surface. The card surface MAY carry a very faint wash of the entry's
type color so heterogeneous feeds read at a glance; the wash SHALL be subtle
enough that it never reads as a color block.

#### Scenario: Card shows tonal depth, not a gradient or flat tone
- **WHEN** any entry card is rendered in the feed
- **THEN** its surface is a calm tonal container (not a gradient wash, not a flat solid)
- **AND** it has a thin hairline border and a light two-layer elevation
- **AND** its corners use the asymmetric rounded shape (smaller on the bottom-start corner)

#### Scenario: Type color appears only as a faint wash
- **WHEN** photo, video, audio, and text entry cards are rendered together
- **THEN** each card's surface carries a very faint wash of its own type color
- **AND** the wash is subtle enough that the cards still read as a calm, near-neutral feed rather than a row of colored blocks

#### Scenario: Multi-select still works on the card
- **WHEN** the user enters multi-select mode
- **THEN** each card shows a selection affordance and tapping a card toggles its selection
- **AND** the card layout remains stable (no layout jump beyond the selection affordance)

### Requirement: Unified header above content with type dot, label, and indicator cluster

An entry card SHALL show the same single header row on every card type (text,
photo, video, recording, file): a small type-colored dot, a compact type label,
and a human-readable relative timestamp on the left, with a unified right-aligned
indicator cluster (pin indicator, todo status badge, and lock indicator when
applicable). The circular type-icon avatar SHALL NOT be rendered. The header
SHALL sit as a solid row on the card surface at the top of the card for all
types — it SHALL NOT float as an overlay over media, and no legibility scrim
SHALL be rendered.

#### Scenario: Header identifies type and time without an avatar
- **WHEN** an entry card is rendered
- **THEN** its header shows a small type-colored dot, a compact type label, and a relative timestamp (e.g. "刚刚", "5分钟前", "今天 14:30")
- **AND** it does NOT show a circular icon avatar

#### Scenario: All card types share one header placement
- **WHEN** text, recording, file, photo, and video entry cards are rendered together
- **THEN** every card's header sits as a solid row on the card surface at the top
- **AND** media (photo/video) cards do NOT overlay the header on the media and do NOT render a scrim

#### Scenario: Encrypted entries show a lock indicator
- **WHEN** an encrypted entry card is rendered (outside multi-select)
- **THEN** the header's right-aligned cluster shows a lock indicator

#### Scenario: Pin, status, and lock form one right-aligned cluster
- **WHEN** an entry card that is pinned, statused, and encrypted is rendered
- **THEN** the pin indicator, status badge, and lock indicator appear together as a single right-aligned cluster in the header

### Requirement: Multi-select affordance sits in the header dateline, never overlapping content

When the user is in multi-select mode, each card SHALL show a check affordance
as the first slot of the header dateline (in the place the type dot otherwise
occupies, hiding the type dot while multi-select is active), so the affordance is
in normal document flow and can never overlap the card body or footer. A selected
card SHALL additionally carry a primary-colored outline.

#### Scenario: Check appears in the header dateline during multi-select
- **WHEN** the user enters multi-select mode
- **THEN** each card's header dateline shows a check affordance as its first slot (replacing the type dot)
- **AND** the check does NOT overlap the card body, media, or footer

#### Scenario: Selected vs unselected check states
- **WHEN** a card is selected in multi-select mode
- **THEN** its check fills with the primary color and shows a check glyph
- **AND** the card carries a primary-colored outline
- **WHEN** a card is not selected
- **THEN** its check shows an outline ring with no fill

#### Scenario: Media cards show the check without overlap
- **WHEN** a photo or video card is rendered in multi-select mode
- **THEN** the check appears in the header dateline above the media
- **AND** it does NOT overlay or overlap the media

### Requirement: Media renders as an edge-to-edge body below the header

A photo or video entry card SHALL render its media edge-to-edge inside the body
zone, clipped to the body's top corners, sitting below the unified header. The
media SHALL NOT bleed under a floating header. Other entry types (text,
recording, file) keep their existing content semantics inside the unified body.

#### Scenario: Photo fills the card width below the header
- **WHEN** a photo entry card is rendered
- **THEN** the image fills the card width with no horizontal padding, below the header
- **AND** its top corners are clipped to the body's top-corner radii
- **AND** tapping the image opens the full-screen zoomable preview

#### Scenario: Video shows a decoded thumbnail as a hero below the header
- **WHEN** a video card is rendered
- **THEN** the decoded first-frame thumbnail fills the card width below the header with a play overlay, clipped to the body's top corners
- **AND** tapping the cover opens the in-app full-screen video player

#### Scenario: Text and file bodies are unchanged in semantics
- **WHEN** a text or file entry card is rendered
- **THEN** its body keeps its existing behavior (text collapsed with expand affordance; file as a document tile), laid out within the unified body inset

### Requirement: Recording entry renders as an audio affordance

A recording entry card SHALL render its body as an audio affordance: a waveform
visualization, a circular play control, and the recording's name and duration
— a recognizable audio control, not a generic file/document chip. Tapping the
play control SHALL hand the recording to the device's audio player (the existing
external-playback path), consistent with how recordings already open.

#### Scenario: Recording card shows a waveform and play control
- **WHEN** an audio recording card is rendered
- **THEN** its body shows a waveform of rounded bars tinted with the recording type accent
- **AND** it shows a circular play control and the recording's name and duration
- **AND** it does NOT show a generic file/document chip

#### Scenario: Play control opens playback
- **WHEN** the user taps the play control on a recording card (outside multi-select)
- **THEN** the recording opens via the existing external-audio-playback path (handed to the device player)

## ADDED Requirements

### Requirement: Tag and reminder footer is a unified wrapping chip row

An entry card's footer SHALL be a single wrapping row of tonal chips, using the
same footer treatment as the group card, that wraps to additional rows when
needed (not truncated with a "+N" affordance). The footer SHALL be omitted
entirely when the entry has no tags, no events, and no comments, and SHALL be
omitted during multi-select mode. Treatments stay tonal: tags as `#tag` chips,
the reminder as a bell chip with the soonest pending (or most-recent fired)
event, fired reminders visually de-emphasized, and an expandable comments chip.

#### Scenario: Many tags wrap to multiple rows
- **WHEN** an entry card carrying more chips than fit on one row is rendered
- **THEN** the chips wrap onto additional rows without truncation

#### Scenario: Plain card has no footer
- **WHEN** an entry card with no tags, no events, and no comments is rendered
- **THEN** no footer row SHALL be rendered

#### Scenario: Footer hidden in multi-select
- **WHEN** the user enters multi-select mode
- **THEN** entry cards SHALL NOT render the footer
