## Why

当前的文件命名 `类型_时间戳.ext` 将元数据（类型）编码在文件名前缀中，冗余且不灵活。目录按天分组导致文件夹过多。文件系统与 Room 的职责边界模糊，关系数据（如评论）难以表达。需要重新设计文件组织形式，使其更简洁、人可读、可扩展。

## What Changes

- **新文件命名规范**：`{yymm-dd-HHMMSS}-f[-{originalName}].{extension}[.enc]`，类型靠扩展名推断，不再编码在文件名中
- **新目录结构**：按月分组 `YYYY-MM/`，取代按天分组
- **角色系统**：`f`（文件）、`n`（评论）、`g`（组目录），通过 role 区分文件职责
- **评论系统**：`{targetTs}-n-{commentTs}.md` 文件，通过时间戳前缀匹配关联到目标文件或组，支持多条评论
- **组系统**：`{ts}-g[-{name}]/` 目录，文件通过目录归属确定组关系，一个文件只属于一个组
- **长按 drawer**：取代当前的删除对话框，提供删除、重命名、多选、移动到组、移动到公共/私有操作
- **Room 角色明确化**：文件系统是唯一数据源，Room 只做索引缓存，可随时从磁盘重建
- **旧数据不迁移**：旧格式文件全部丢弃，不兼容

## Capabilities

### New Capabilities

- `file-naming-format`: 新的文件命名规范、目录结构、role 系统、扩展名类型推断
- `entry-relationships`: 评论（n-）和组（g-）的文件系统关联推导，孤儿评论处理
- `entry-actions-drawer`: 长按条目弹出的底部 drawer，包含删除、重命名、多选、移动到组、移动空间操作

### Modified Capabilities

_None — 全部是新能力_

## Impact

- **破坏性变更**：文件命名和目录结构完全不兼容旧格式，旧文件将被丢弃
- **文件**：`FileStorageEngine.kt`（完全重写）、`MindDumpEntry.kt`（重构）、`EntryEntity.kt` / `EntryDao.kt`（schema 变更）、`MindDumpRepository.kt`（重写）、`MindDumpDatabase.kt`（版本升级）
- **UI 文件**：`MainScreen.kt`（长按 drawer）、`MindDumpViewModel.kt`（新操作）、新增 drawer 组件
- **数据库迁移**：Room 版本升级，新增 `targetTimestamp`、`role` 字段，旧数据清空
- **无外部依赖变更**
