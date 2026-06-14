## ADDED Requirements

### Requirement: Pin/unpin action
The entry actions drawer SHALL include a pin/unpin toggle action for file entries (not comments). The action label SHALL reflect the current state ("置顶" when unpinned, "取消置顶" when pinned).

#### Scenario: Pin from the drawer
- **WHEN** the user opens the drawer for `2506-13-143022-f.md` and taps "置顶"
- **THEN** the file is renamed to `9999-2506-13-143022-f.md` and the drawer reflects the pinned state

#### Scenario: Unpin from the drawer
- **WHEN** the user opens the drawer for `9999-2506-13-143022-f.md` and taps "取消置顶"
- **THEN** the file is renamed to `2506-13-143022-f.md`

#### Scenario: Comment entry has no pin action
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** no pin/unpin action is shown

### Requirement: Set todo status action
The entry actions drawer SHALL include a "待办状态" (todo status) action that opens a chooser offering the status set (`TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`) plus a "无" (clear) option, for file entries (not comments).

#### Scenario: Open the status chooser
- **WHEN** the user taps "待办状态" in the drawer
- **THEN** a chooser presents TODO, DOING, WAIT, DONE, CANCEL, and a clear option, with the current status marked

#### Scenario: Pick a status
- **WHEN** the user picks DOING for `2506-13-143022-f.md`
- **THEN** the file is renamed to `2506-13-143022-DOING-f.md` and the chooser closes

#### Scenario: Clear the status
- **WHEN** the user picks the clear option for `2506-13-143022-DONE-f.md`
- **THEN** the file is renamed to `2506-13-143022-f.md`

#### Scenario: Comment entry has no status action
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** no todo-status action is shown
