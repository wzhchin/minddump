## Context

MindDump 采用双存储架构:文件系统是唯一数据源,Room + FTS 是派生索引。已有的
`reconcileWithDisk(space)` 做的是增量对账(插入新增、删除孤儿、更新过期行),但它不会
清掉索引里「磁盘已无、但快照里仍有」之外的累积漂移,也无法修复 FTS 索引本身的损坏。
缺少一个「推倒重来」的用户入口。

## Goals / Non-Goals

**Goals:**
- 提供用户可见的「重建数据库」入口,作为数据异常时的自助修复手段
- 保证文件零损失——重建只动索引,不碰磁盘文件
- 给出明确的进度与结果反馈,避免用户误以为卡死

**Non-Goals:**
- 不改变 Room schema(版本仍为 3),不引入迁移
- 不做选择性重建(只能全量重建两个空间)
- 不改变 `reconcileWithDisk` 的既有语义

## Decisions

### Decision 1:重建 = 清空 + 全量 re-scan + FTS rebuild

`rebuildDatabase()` 顺序执行:`clearAll()` → `reconcileWithDisk(PUBLIC)` →
`reconcileWithDisk(PRIVATE)` → `rebuildFtsIndex()`。

- `reconcileWithDisk` 在空表上等价于全量插入(无孤儿、无更新,只有新增分支)。
- 显式 `rebuildFtsIndex()`(`INSERT INTO entries_fts(entries_fts) VALUES('rebuild')`)
  保证 FTS 与内容表严格一致,覆盖增量对账可能漏掉的 FTS 漂移。

**替代方案**:直接 `fallbackToDestructiveMigration` 式删除整个 db 文件重建。
被否决——会丢失 Room 的内部配置/连接,且需要重启 DB 实例;清表重建更轻量、
风险更低。

### Decision 2:FTS rebuild 用 `entries_fts` 的特殊命令

FTS4 content-table 模式下,`INSERT INTO <fts>(<fts>) VALUES('rebuild')` 是官方
重建命令,从 contentEntity(`entries`)重新填充。与 `MIGRATION_2_3` 中使用的命令一致。

### Decision 3:确认 + 进度用两个状态驱动一个对话框

`UiState` 加 `showRebuildDatabaseDialog`(是否显示对话框)与 `isRebuildingDatabase`
(是否在执行中)。`RebuildDatabaseDialog` 根据 `running` 切换渲染:运行时显示
不可取消的 indeterminate 转圈,否则显示确认/取消按钮。执行在
`viewModelScope` 的 IO 协程,完成后 `refreshForCurrentScope()` 刷新当前列表。

### Decision 4:不依赖会话密码

`reconcileWithDisk` 只通过 `.enc` 扩展名判断加密状态、从不解密,因此重建
Private 空间不需要 session 密码,任何解锁状态下都能安全执行。

## Risks / Trade-offs

- **大库重建耗时**:条目极多时清表 + 全量扫描 + FTS 重建可能耗时数秒,
  故用 indeterminate 转圈而非具体进度(实现成本与收益不匹配)。
- **重建中崩溃**:若重建中途异常,表会被部分重建——但 `reconcileWithDisk`
  是幂等的,用户再次重建即可恢复,文件从未受损。
