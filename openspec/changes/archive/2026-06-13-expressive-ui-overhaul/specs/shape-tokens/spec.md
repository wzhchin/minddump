## ADDED Requirements

### Requirement: Shape token hierarchy
The system SHALL define the following shape tokens via a `ExpressiveShapes` data class:

| Token | Corner radius |
|---|---|
| cardLarge | 28dp |
| cardMedium | 24dp |
| cardSmall | 16dp |
| buttonPill | 50% (CircleShape) |
| buttonRounded | 20dp |
| buttonSquared | 12dp |
| inputField | 20dp |
| chip | 12dp |

#### Scenario: Tokens accessible via CompositionLocal
- **WHEN** a composable accesses `LocalExpressiveShapes.current`
- **THEN** it SHALL receive the full `ExpressiveShapes` data class with all tokens defined

#### Scenario: Tokens used instead of hardcoded values
- **WHEN** a UI component needs a corner radius
- **THEN** it SHALL reference the appropriate token from `LocalExpressiveShapes` instead of hardcoding a dp value

### Requirement: Asymmetric entry card corners
The system SHALL support asymmetric corner shapes for entry list items.

#### Scenario: Entry card shape
- **WHEN** an entry card is rendered in the list
- **THEN** it SHALL use corners of 20dp on three sides and 6dp on the bottom-left, creating a visual stack effect

#### Scenario: Optical nesting
- **WHEN** a nested element (e.g., thumbnail, badge) sits inside a card with 20dp outer corners
- **THEN** the inner element SHALL use a reduced corner radius based on padding distance (outer - padding ≈ inner)

### Requirement: Press scale feedback with no ripple
The system SHALL replace Material ripple indications with scale-based press feedback.

#### Scenario: Press scale animation
- **WHEN** user presses an interactive element using `animatePressScale()`
- **THEN** the element SHALL scale to 0.92f with spring physics (dampingRatio=0.6f, stiffness=400f) and spring back to 1.0f on release

#### Scenario: No ripple indication
- **WHEN** user presses any interactive element using `noRippleClickable()`
- **THEN** no Material ripple SHALL appear; only the scale animation and haptic feedback SHALL indicate the press

#### Scenario: Combined scale + elevation on press
- **WHEN** user presses a card or elevated element
- **THEN** the element SHALL simultaneously scale down and animate shadow elevation for a depth effect
