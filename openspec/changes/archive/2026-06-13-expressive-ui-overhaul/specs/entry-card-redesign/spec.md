## ADDED Requirements

### Requirement: Unified card layout with header row
All entry types SHALL use a unified card layout: a header row with type icon avatar and timestamp, followed by a content area below.

#### Scenario: Header row renders type icon and timestamp
- **WHEN** any entry card is displayed
- **THEN** the top of the card SHALL show a circular avatar containing the entry type icon, followed by a relative timestamp (e.g., "今天 14:32")

#### Scenario: Type icons per entry type
- **WHEN** an entry card is rendered
- **THEN** the avatar SHALL display the icon matching the entry type:
  - TEXT: edit/note icon
  - PHOTO: camera/image icon
  - RECORDING: mic icon
  - VIDEO: videocam icon
  - FILE: attachment/file icon
  - UNKNOWN: help/question icon

#### Scenario: Relative timestamp formatting
- **WHEN** the entry timestamp is displayed
- **THEN** it SHALL show a relative format: "刚刚" (<1min), "X分钟前" (<60min), "X小时前" (<24h), "昨天 HH:mm" (yesterday), "MM月DD日 HH:mm" (older)

### Requirement: Content area per entry type
The content area below the header SHALL render differently based on entry type.

#### Scenario: Text entry content
- **WHEN** a TEXT entry is displayed
- **THEN** the content area SHALL show the text content with `bodyMedium` style, max 3 lines with ellipsis

#### Scenario: Photo entry content
- **WHEN** a PHOTO entry is displayed
- **THEN** the content area SHALL show an image thumbnail loaded via Coil, filling the card width with 4:3 aspect ratio and rounded corners

#### Scenario: Audio entry content
- **WHEN** a RECORDING entry is displayed
- **THEN** the content area SHALL show the content preview text (if any) plus an animated waveform indicator with a play icon

#### Scenario: Video entry content
- **WHEN** a VIDEO entry is displayed
- **THEN** the content area SHALL show a video thumbnail with a centered play overlay icon

#### Scenario: File entry content
- **WHEN** a FILE entry is displayed
- **THEN** the content area SHALL show the file name with a file type icon

### Requirement: Asymmetric corners on entry cards
Entry cards SHALL use asymmetric corners for visual rhythm in the list.

#### Scenario: Card shape
- **WHEN** an entry card is rendered
- **THEN** it SHALL use `RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)` — 6dp on bottom-left

### Requirement: Minimal top bar with floating icons
The main screen top bar SHALL NOT display any title text. It SHALL only show floating icon buttons.

#### Scenario: No title displayed
- **WHEN** the main screen is displayed
- **THEN** the top bar SHALL NOT show the app name, space name, or any text title

#### Scenario: Floating icon buttons
- **WHEN** the main screen top bar is visible
- **THEN** it SHALL show exactly 3 icon buttons at the top-right: Search (magnifying glass), Statistics (bar chart), Settings (gear)

#### Scenario: Search expands inline
- **WHEN** user taps the Search icon
- **THEN** the search field SHALL expand in place of the icon buttons, with a close/clear action

### Requirement: Entry card interactions
Entry cards SHALL support tap and long-press with scale + haptic feedback.

#### Scenario: Tap to open
- **WHEN** user taps an entry card
- **THEN** the card SHALL scale to 0.92f, perform `Tick` haptic, and open the file

#### Scenario: Long-press to delete
- **WHEN** user long-presses an entry card
- **THEN** the card SHALL perform `Buildup` haptic, then show the delete confirmation dialog
