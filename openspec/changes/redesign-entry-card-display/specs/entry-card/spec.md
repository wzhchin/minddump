## Requirement: Entry card surfaces use a layered, depth-bearing treatment

An entry card SHALL render on a layered surface — a subtle gradient wash over the card body, a hairline border, a two-layer (tonal + ambient) elevation, and an asymmetric rounded shape — instead of a flat tone with a uniform border, so cards lift off the feed background with depth rather than a frame. The card surface MAY additionally carry a very faint wash of the entry's type color so heterogeneous feeds read at a glance; the wash SHALL be subtle enough that it never reads as a color block.

#### Scenario: Card shows gradient depth, not a flat tone
- **WHEN** any entry card is rendered in the feed
- **THEN** its surface is a subtle gradient (not a flat solid tone)
- **AND** it has a hairline border and a light two-layer elevation separating it from the background
- **AND** its corners use an asymmetric rounded shape (smaller on the bottom-start corner)

#### Scenario: Type color appears only as a faint wash
- **WHEN** photo, video, audio, and text entry cards are rendered together
- **THEN** each card's surface carries a very faint wash of its own type color
- **AND** the wash is subtle enough that the cards still read as a calm, near-neutral feed rather than a row of colored blocks

#### Scenario: Multi-select still works on the card
- **WHEN** the user enters multi-select mode
- **THEN** each card shows a selection affordance and tapping a card toggles its selection
- **AND** the card layout remains stable (no layout jump beyond the selection affordance)

## Requirement: Compact floating header with type dot and unified indicator cluster

An entry card SHALL show a single compact header row containing a small type-colored dot (not a circular icon avatar) and a human-readable relative timestamp on the left, with a unified right-aligned indicator cluster (pin indicator, todo status badge, and lock indicator when applicable). The circular type-icon avatar SHALL NOT be rendered. For photo and video entries the header SHALL float as a chip overlaid on the media with a legibility scrim; for other entry types the header SHALL sit as a solid row on the card surface at the top.

#### Scenario: Header identifies type and time without an avatar
- **WHEN** an entry card is rendered
- **THEN** its header shows a small type-colored dot and a relative timestamp (e.g. "刚刚", "5分钟前", "今天 14:30")
- **AND** it does NOT show a circular icon avatar

#### Scenario: Media entries get a floating header chip
- **WHEN** a photo or video entry card is rendered
- **THEN** the header floats as a chip overlaid on the media
- **AND** a legibility scrim sits behind the header so the timestamp and indicators remain readable over the image

#### Scenario: Non-media entries get a solid top header
- **WHEN** a text, recording, or file entry card is rendered
- **THEN** the header sits as a solid row on the card surface at the top (no overlay, no scrim)

#### Scenario: Encrypted entries show a lock indicator
- **WHEN** an encrypted entry card is rendered (outside multi-select)
- **THEN** the header's right-aligned cluster shows a lock indicator

#### Scenario: Pin, status, and lock form one right-aligned cluster
- **WHEN** an entry card that is pinned, statused, and encrypted is rendered
- **THEN** the pin indicator, status badge, and lock indicator appear together as a single right-aligned cluster in the header

## Requirement: Media entries render as an edge-to-edge hero

A photo or video entry card SHALL render its media edge-to-edge inside the card, clipped to the card's top corners, so the media is the visual anchor of the card and bleeds under the floating header chip. Other entry types (text, recording, file) are unaffected and keep their existing content layout.

#### Scenario: Photo fills the card width to its top corners
- **WHEN** a photo entry card is rendered
- **THEN** the image fills the card width with no horizontal padding
- **AND** its top corners are clipped to match the card's top-corner radii
- **AND** tapping the image opens the full-screen zoomable preview

#### Scenario: Video shows a decoded thumbnail as a hero
- **WHEN** a video card is rendered
- **THEN** the decoded first-frame thumbnail fills the card width with a play overlay, clipped to the card's top corners
- **AND** tapping the cover opens the in-app full-screen video player

#### Scenario: Text, recording, and file bodies are unchanged
- **WHEN** a text, recording, or file entry card is rendered
- **THEN** its body is laid out as before (text collapsed with expand affordance; recording/file as a document chip)

## Requirement: Comments render as a nested sub-surface

When an entry has comments, the in-card collapsed comments section SHALL render as a visually distinct nested sub-surface inside the card — a smaller rounded surface with a deeper tone, inset from the card edges — so the card reads with a clear top (identity) / middle (content) / nested (context) rhythm. The expand/collapse behavior and orphan-comment indicator are unchanged.

#### Scenario: Comments appear in a nested sub-surface
- **WHEN** an entry card with comments is rendered
- **THEN** the comments section appears as a smaller rounded sub-surface inset from the card edges, with a deeper tone than the card surface
- **AND** expanding/collapsing behaves as before

## Requirement: Tag and reminder meta wraps across rows

An entry card's tag and reminder footer SHALL render as a single wrapping row of tonal chips that wraps to additional rows when needed (not truncated with a "+N" affordance). The footer SHALL be omitted entirely when the entry has no tags and no events, and SHALL be omitted during multi-select mode. Treatments stay tonal and desaturated: tags as `#tag` chips, the reminder as a bell chip with the soonest pending (or most-recent fired) event, fired reminders visually de-emphasized.

#### Scenario: Many tags wrap to multiple rows
- **WHEN** an entry card carrying more tags than fit on one row is rendered
- **THEN** the tag chips wrap onto additional rows without truncation

#### Scenario: Plain card has no footer
- **WHEN** an entry card with no tags and no events is rendered
- **THEN** no footer row SHALL be rendered

#### Scenario: Footer hidden in multi-select
- **WHEN** the user enters multi-select mode
- **THEN** entry cards SHALL NOT render the tag/reminder footer
