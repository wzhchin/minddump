## Context

MindDump 使用双存储架构：文件系统是唯一数据源，Room 是索引缓存层。当前文件名格式 `类型_HHmmssSSS.ext` 将类型硬编码在前缀中，按天分目录。重构目标：人可读的文件名、按月分组、支持评论和组的关联关系、role 前缀系统为未来扩展预留空间。

## Goals / Non-Goals

**Goals:**
- 建立新的文件命名和目录规范
- 通过 role + 扩展名推断类型，消除类型前缀
- 实现评论（n-）和组（g-）的关联关系
- 长按 drawer 替代删除对话框
- 明确 Room 为纯索引层

**Non-Goals:**
- 旧格式文件迁移（全部丢弃）
- 加密算法变更（保持 AES-256-CBC）
- UI 大改版（主列表保持现有气泡风格，评论/组做增量）
- 实时同步 / 云备份

## Decisions

### Decision 1：时间戳格式 `yymm-dd-HHMMSS`

完整的人可读格式，自包含不依赖目录上下文。例如 `2506-13-143022` = 2025年6月13日 14:30:22。

解析规则：`{ts}-f[-{originalName}].{extension}` 中，取第一个 `-f`、`-n-`、`-g` 之前的部分作为时间戳。

**替代方案**：纯数字时间戳 `HHmmssSSS`。被否决——人不可读，文件拷出后丢失上下文。

### Decision 2：Role 系统

文件名中 `f` / `n` / `g` 作为 role 标识：

```
{ts}-f[-{originalName}].{extension}    → 文件
{targetTs}-n-{commentTs}.md            → 评论
{ts}-g[-{name}]/                       → 组目录
```

解析：`split("-")` 后，第二个含 role 的段即为角色。未来可扩展新 role（如 `t-` 模板、`b-` 备份）。

**替代方案**：用 sidecar `.meta` JSON 文件存元数据。被否决——文件数翻倍，用户手动管理时看到一堆 `.meta` 文件不直觉。

### Decision 3：目录结构按月分组

```
{workDir}/{Public|Private}/YYYY-MM/
```

月内文件量可控（每天几十条，一个月几百条），减少目录碎片。

**替代方案**：保持按天分组。被否决——目录过多，且时间戳已自包含日期信息。

### Decision 4：关联关系纯靠文件系统推导

- 评论归属：扫描同目录，找前缀为 `targetTs` 且 role 为 `f` 或 `g` 的条目
- 组归属：文件在 `g-` 目录下即属于该组
- Room 存 `targetTimestamp` 和 `groupPath` 字段，但仅为查询性能，reconcile 时从磁盘重建

孤儿评论（目标文件已删）照常在 Room 索引，UI 照常展示。

**替代方案**：Room 存 `parentId` 外键。被否决——违反"文件系统是唯一数据源"原则，Room 只是索引。

### Decision 5：组用目录实现

组是一个以 `{ts}-g[-{name}]` 命名的子目录，文件移动到组目录下即归属该组。

- 一个文件只属于一个组
- 组可以匿名（`{ts}-g/`）或命名（`{ts}-g-travel/`）
- 组可以有评论（时间戳与组目录时间戳匹配）
- "移动到组"操作即物理移动文件

**替代方案**：文件名编码组信息（`{ts}-f-g{groupTs}.ext`）。被否决——解析复杂，文件改组要重命名。

### Decision 6：长按 drawer

长按条目弹出底部 drawer（BottomSheetScaffold / ModalBottomSheet），包含：
- 删除
- 重命名（修改 `-f-` 后的部分，即 originalName）
- 多选（进入多选模式）
- 移动到组（弹出已有组列表 + 新建组选项）
- 移动到公共/私有

**替代方案**：保持 AlertDialog 删除确认。被否决——功能不够，无法扩展。

## Risks / Trade-offs

- **[破坏性格式变更]** → 旧文件不兼容，但有明确前提：尚未上生产，测试数据可丢弃
- **[评论关联依赖文件名匹配]** → 如果用户手动重命名目标文件，评论变孤儿。缓解：重命名时提示用户影响
- **[组 = 物理目录，移动有 IO 开销]** → 大文件移动可能慢。缓解：使用 `File.renameTo()` 而非拷贝（同文件系统下是原子操作）
- **[按月目录文件数可能较大]** → 一个月几百条文件在单个目录下，`listFiles()` 性能可接受
- **[Room schema 变更]** → 需要 Migration，旧数据清空（`fallbackToDestructiveMigration`）
