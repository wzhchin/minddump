## MODIFIED Requirements

### Requirement: Unified color baseline
The system SHALL derive all app colors (including any legacy declarative color resources) from the theme seed, eliminating the previous inconsistency between green declarative colors and purple Compose-theme colors. Category/entry-type indicator colors SHALL be derived from the theme scheme and SHALL NOT reuse semantic tokens (error/warning) for non-semantic category coloring.

#### Scenario: Colors are consistent with the seed
- **WHEN** the app renders any surface, icon, or control
- **THEN** its color is consistent with the active theme derived from the seed and palette style
- **AND** no two uncoordinated color palettes (e.g. green XML vs purple Compose) coexist

#### Scenario: Category colors do not reuse the error token
- **WHEN** the app renders an entry-type indicator for a non-error category (e.g. video)
- **THEN** its color is a theme-derived category color, not the error/warning color token
- **AND** the error color token is reserved for genuine error/urgent states (e.g. recording-in-progress, destructive confirmations)
