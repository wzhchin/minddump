## Why

文件保存和数据库缓存层存在多处数据完整性缺陷：文件路径无唯一约束导致重复条目、加密流程中途 crash 会泄漏明文、`refreshFromDisk` 只加不删造成幽灵条目、秒级时间戳碰撞会覆盖文件。这些问题在正常使用中不容易触发，但在快速操作、crash 恢复、磁盘手动修改等场景下会导致数据不一致甚至隐私泄漏。

## What Changes

- `EntryEntity.filePath` 加唯一索引，`insert` 时 `OnConflictStrategy.REPLACE` 真正按路径去重
- 加密流程改为原子化：先验证加密文件完好再删明文原文
- `importFile` 加空输入和空结果检查，拒绝创建幽灵条目
- `refreshFromDisk` 改为双向 reconcile：新增、删除幽灵、更新过时 `contentPreview`
- 文件名时间戳从秒级 (`HHmmss`) 改为毫秒级 (`HHmmssSSS`)，并加碰撞检测 + 自增序号兜底
- `registerMediaFile` 在加密前确定 `EntryType`，避免从加密后文件名推断类型
- `entries` 表加统计查询覆盖索引 `(space, dateFolder)` 和 `(space, type)`

## Capabilities

### New Capabilities
- `disk-reconcile`: 双向磁盘-数据库同步，检测新增/删除/变更条目并保持 Room 索引与文件系统一致
- `atomic-encryption`: 原子化加密写入流程，crash-safe，不泄漏明文

### Modified Capabilities
（无已有 spec 需修改）

## Impact

- **`data/EntryEntity.kt`**: 加 `@Index(unique = true)`，需要 Room DB version bump + `fallbackToDestructiveMigration` 或 migration
- **`data/EntryDao.kt`**: `refreshFromDisk` 逻辑迁移到 repository 层的 reconcile 方法，新增 delete-orphan、update-stale 查询
- **`data/MindDumpRepository.kt`**: 重写 `refreshFromDisk`、`encryptFile`、`importFile`、`registerMediaFile`
- **`storage/FileStorageEngine.kt`**: 时间戳格式改为 `HHmmssSSS`，所有 `get*File()` 加碰撞检测
- **`data/MindDumpDatabase.kt`**: version 1 → 2，加 migration
- **`storage/MindDumpEntry.kt`**: `extractTimestamp` 适配新时间戳格式（如果存在）
