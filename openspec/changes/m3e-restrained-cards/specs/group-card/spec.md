## MODIFIED Requirements

### Requirement: Group card matches the entry card's unified scaffold and tonal surface

A group card SHALL render on the same calm tonal surface as entry cards (thin
hairline border, light two-layer elevation, asymmetric rounded shape, no gradient
wash) so the feed reads as one cohesive Material 3 Expressive surface. It SHALL
use the same unified three-zone scaffold (header · body · footer) and the same
header/footer placement as entry cards: a header row with a small dot, a compact
label, and a relative timestamp on the left and a right-aligned cluster (pin
indicator, status badge), a body with the group title and a member/last-updated
byline, and a footer of type-count chips. The group card SHALL NOT carry a
per-type color wash (the surface tone alone applies). The circular folder-icon
avatar SHALL NOT be rendered. The media carousel behavior and empty-state wording
are unchanged.

#### Scenario: Group card matches entry card surface and scaffold
- **WHEN** a group card is rendered alongside entry cards
- **THEN** the group card uses the same calm tonal surface, hairline border, elevation, and asymmetric rounded shape as the entry cards
- **AND** its header sits as a solid row at the top with the same left-aligned dateline (dot · label · time) and right-aligned cluster as the entry cards
- **AND** its footer uses the same wrapping chip treatment as the entry cards

#### Scenario: Group card header uses a dot, not a folder avatar
- **WHEN** a group card is rendered
- **THEN** its header shows a small dot before the group name (not a circular folder-icon avatar)
- **AND** the pin indicator and status badge appear as a right-aligned cluster

#### Scenario: Member count appears in the body byline
- **WHEN** a group card with members is rendered
- **THEN** the body shows the group title and a byline that includes the member count and last-updated time
- **AND** the type-count chips appear in the footer

#### Scenario: Carousel and empty-state are unchanged
- **WHEN** a group card with media members is rendered
- **THEN** the media carousel preview and its tap/browse behavior are unchanged
- **WHEN** a group card with no members is rendered
- **THEN** the type-stat footer shows a localized empty indication (e.g. "空分组")
