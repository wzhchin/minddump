## Context

MindDump 的数据层采用「文件系统为 source of truth，Room 为索引缓存」的架构。`FileStorageEngine` 负责磁盘 I/O，`MindDumpRepository` 协调文件写入与 Room 插入，`EntryDao` 提供 Flow-based 查询。

当前架构的问题集中在**一致性**和**安全性**两个维度：

1. **一致性**：Room 与文件系统之间没有双向同步，`refreshFromDisk` 只做增量插入，磁盘删除不会反映到 Room；`filePath` 无唯一约束，重复条目可以存在。
2. **安全性**：加密流程是「写明文 → 加密 → 删明文」三步，crash 在中间步骤会泄漏明文。
3. **健壮性**：秒级时间戳在快速操作下碰撞，`importFile` 静默吞掉空输入。

Constraints：
- Min SDK 29，`MANAGE_EXTERNAL_STORAGE` 已用于文件系统访问。
- Room version 1，需要 bump 到 2 并提供 migration。
- 加密向后兼容：已有的 `.enc` 文件格式不能变。

## Goals / Non-Goals

**Goals:**
- Room `filePath` 唯一索引，杜绝重复条目
- 加密流程 crash-safe：任何时刻中断都不泄漏明文
- 双向磁盘-DB reconcile：新增、删除、变更都能同步
- 文件名时间戳不碰撞
- `importFile` 拒绝空输入
- 统计查询走索引覆盖扫描

**Non-Goals:**
- 不引入远程同步/云端备份
- 不改变加密算法（保持 AES-256-GCM）
- 不改变 `.enc` 文件格式（向后兼容）
- 不引入 WAL 模式以外的 Room 配置
- 不做 `MindDumpEntry` 的 `File` 对象池优化（P2，后续单独处理）

## Decisions

### D1: Room migration 而非 destructive migration

**Decision**: 从 version 1 → 2，手写 `Migration(1, 2)` 添加索引。

**Rationale**: 用户已有数据。`fallbackToDestructiveMigration()` 会丢数据，对日记应用不可接受。

**Migration 内容**:
```sql
CREATE UNIQUE INDEX index_entries_filePath ON entries(filePath);
CREATE INDEX index_entries_space_dateFolder ON entries(space, dateFolder);
CREATE INDEX index_entries_space_type ON entries(space, type);
```

**Alternative considered**: 用 `@Fts4` 的 `content` 参数关联主表——不必要，FTS 已经在用，只是索引缺失。

### D2: reconcile 策略 — 全量 diff in memory

**Decision**: `reconcileWithDisk(space)` 一次性把磁盘条目和 Room 条目全量拉到内存做 diff。

**Rationale**: 个人日记 app 数据量级（几千条），全量 diff 简单可靠。不需要增量标记或文件系统 watcher。

**Diff 逻辑**:
```
diskPaths = scanEntries().map { it.file.absolutePath }.toSet()
dbEntries = dao.getAllSnapshot(space)  // suspend fun，返回 List 不是 Flow

dbPaths = dbEntries.map { it.filePath }.toSet()

toInsert = diskEntries.filter { it.file.absolutePath !in dbPaths }  // 磁盘有 DB 没有
toDelete = dbEntries.filter { it.filePath !in diskPaths }            // DB 有磁盘没有
toUpdate = dbEntries.filter { it.filePath in diskPaths }             // 两边都有，检查 lastModified
```

`toUpdate` 中，如果文件的 `lastModified` 与 Room 记录不同，重新读取 `contentPreview` 和 `lastModified`。

**Alternative considered**: 只做增量（只加不删/不改）——现状，不够。增量 + tombstone——复杂度不值得。

### D3: 原子加密 — verify-then-delete

**Decision**: 加密后、删明文前，验证加密文件存在且非空。

```kotlin
private fun encryptFile(source: File, password: String): File {
    val encrypted = File(source.parent, source.name + ".enc")
    cryptoEngine.encryptFile(source, encrypted, password)
    check(encrypted.exists() && encrypted.length() > 0) {
        "Encryption failed: output file missing or empty"
    }
    source.delete()
    return encrypted
}
```

**Rationale**: 完整的 atomic rename 在 Android 外部存储上不可靠（`File.renameTo()` 跨文件系统会失败）。verify-then-delete 是在当前架构内最简单的 crash-safe 改进。

**Alternative considered**: 先写 temp 再 rename——`FileStorageEngine` 的 `workDir` 可能在不同文件系统上，`renameTo` 跨 FS 会失败，需要 `copyTo + delete`，本质上和现在一样。verify-then-delete 是同等安全性下的最简方案。

### D4: 时间戳毫秒化 + 碰撞自增

**Decision**: 格式从 `HHmmss` 改为 `HHmmssSSS`（毫秒级）。如果碰撞，追加 `_1`, `_2`, ... 后缀。

```kotlin
private fun uniqueFile(dir: File, prefix: String, ext: String): File {
    val baseName = "${prefix}_${nowTimestampStr()}"
    var file = File(dir, "$baseName.$ext")
    var seq = 1
    while (file.exists()) {
        file = File(dir, "${baseName}_$seq.$ext")
        seq++
    }
    return file
}
```

**Rationale**: 毫秒级把碰撞概率降到极低；while-loop 兜底处理极端情况。

**Alternative considered**: UUID 文件名——不可读，丢失时间信息。保留中文前缀 + 时间戳更符合用户在文件管理器中浏览的需求。

**注意**: `extractTimestamp` 需要适配新格式。当前逻辑是 `split("_")[1]`，毫秒格式下仍然正确（取到 `HHmmssSSS`），但 display 层如果按 `HHmmss` 格式化需要截取前 6 位。

### D5: `importFile` fail-fast

**Decision**: `openInputStream` 返回 null 时抛 `IOException`，写入后检查文件非空。

**Rationale**: 让调用方（ViewModel）能捕获异常并展示错误提示，而不是创建幽灵条目。

## Risks / Trade-offs

- **Migration 失败风险**: 如果用户已有重复 `filePath` 的条目，`CREATE UNIQUE INDEX` 会失败。→ **Mitigation**: migration 中先 `DELETE FROM entries WHERE id NOT IN (SELECT MIN(id) FROM entries GROUP BY filePath)` 去重，再建索引。
- **全量 reconcile 内存占用**: 几千条 EntryEntity 内存占用约几百 KB，可接受。如果未来条目超过 10 万级需要改为分批。→ **Mitigation**: 目前不加限制，未来如果需要可以加 `LIMIT` 分页 reconcile。
- **时间戳格式变更是向前兼容的**: 新文件名更长，但 `extractTimestamp` 不需要改（`split("_")[1]` 仍然取到时间戳部分）。旧文件名格式（6 位）不会被误读。
- **`check()` 抛异常**: `encryptFile` 中的 `check` 在加密失败时抛 `IllegalStateException`。→ **Mitigation**: 调用方已经在 `withContext(Dispatchers.IO)` 中，异常会传播到 ViewModel 层。需要在 ViewModel 加 try-catch 展示错误。
