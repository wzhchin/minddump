## Requirement: Group card uses the entry card's layered surface and compact header

A group card SHALL render on the same layered surface as entry cards (subtle gradient wash, hairline border, two-layer elevation, asymmetric rounded shape) so the feed reads as one cohesive surface. Its header SHALL use the same compact treatment: a small dot (primary-colored) before the group name, with pin indicator, status badge, and member count forming the right-aligned cluster. The circular folder-icon avatar SHALL NOT be rendered. The media carousel, type-chip row, and empty-state wording are unchanged.

#### Scenario: Group card matches entry card surface
- **WHEN** a group card is rendered alongside entry cards
- **THEN** the group card uses the same gradient surface, hairline border, elevation, and asymmetric rounded shape as the entry cards
- **AND** it does NOT carry a per-type color wash (the surface gradient alone applies)

#### Scenario: Group card header uses a dot, not a folder avatar
- **WHEN** a group card is rendered
- **THEN** its header shows a small dot before the group name (not a circular folder-icon avatar)
- **AND** the pin indicator, status badge, and member count appear as a right-aligned cluster

#### Scenario: Carousel and type chips are unchanged
- **WHEN** a group card with media members is rendered
- **THEN** the media carousel preview and its tap/browse behavior are unchanged
- **AND** the type-stat row and empty-group indication are unchanged
