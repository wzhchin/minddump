## ADDED Requirements

### Requirement: Long text bodies expand and collapse in place
Within an entry card (not the fullscreen editor), a long text body SHALL collapse to a few lines and expose a working, animated expand/collapse affordance. Toggling it SHALL change only the body's presentation and SHALL NOT open the fullscreen editor or navigate away.

#### Scenario: Expand a long text body in the card
- **WHEN** the user taps the expand affordance on a collapsed long-text body in an entry card
- **THEN** the body animates to its full height showing all the text
- **AND** the affordance changes to a collapse label (收起)

#### Scenario: Collapse an expanded text body in the card
- **WHEN** the user taps the collapse affordance on an expanded long-text body
- **THEN** the body animates back to the collapsed few-line view
- **AND** the affordance changes back to the expand label (展开)

#### Scenario: Expand toggle does not open the editor
- **WHEN** the user taps the expand/collapse affordance
- **THEN** the fullscreen editor does not open and the entry is not navigated into
- **AND** only the in-card body height changes
