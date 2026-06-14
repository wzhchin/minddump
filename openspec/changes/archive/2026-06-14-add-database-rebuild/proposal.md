## Why

Room 数据库是磁盘文件的索引缓存层,理论上可随时从磁盘重建。但在实际使用中,索引可能因为 bug、异常退出、迁移残留等原因与磁盘状态不一致,而当前没有任何用户可见的手段强制重建——用户只能逐项操作或重装应用。需要一个明确的「重建数据库」入口,让用户在数据看起来异常时一键修复,且保证文件零损失。

## What Changes

- **设置入口**:在设置对话框中新增「重建数据库」按钮及说明文案
- **二次确认**:点击后弹出确认对话框,提示该操作不可撤销,避免误触
- **全量重建**:确认后清空 Room 表 + FTS 索引,从磁盘重新扫描 Public 与 Private 两个空间,再重建 FTS 索引
- **进度反馈**:重建期间显示 indeterminate 进度对话框(不可取消),完成后以 Snackbar 显示重建条目数
- **文件零改动**:重建只动数据库,磁盘文件完全不受影响

## Capabilities

### New Capabilities

- `database-rebuild`: 从磁盘全量重建 SQLite/FTS 索引的设置入口、确认流程、进度与结果反馈

### Modified Capabilities

_None — 这是一个独立的维护能力,不修改既有能力_

## Impact

- **数据**:`EntryDao` 新增 `clearAll()` / `rebuildFtsIndex()`;`MindDumpRepository` 新增 `rebuildDatabase()` / `countAllTotal()`
- **UI**:`SettingsDialog` 新增按钮 + 新增 `RebuildDatabaseDialog` 组件;`MainScreen` 接线 + Snackbar;`MindDumpViewModel` 新增状态与操作
- **文案**:`strings.xml` / `values-en/strings.xml` 各新增 7 条
- **无 Room schema 变更**(版本仍为 3,无需迁移)
- **无破坏性变更**:文件与现有数据完全兼容
