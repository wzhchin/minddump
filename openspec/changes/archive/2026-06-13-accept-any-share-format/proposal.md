## Why

MindDump 目前只能通过应用内相机、录音和文本输入来创建条目。用户在浏览其他应用时经常想快速保存内容（图片、链接、文件、文字），但必须先切换到 MindDump 再手动操作。Android 的 Share sheet 是系统级的"发送到任何应用"机制——接入后用户可以从**任何应用**直接分享**任何格式**的内容到 MindDump，实现真正的"brain dump"零摩擦捕获。

## What Changes

- 在 `AndroidManifest.xml` 中为 `MainActivity` 添加 `ACTION_SEND` 和 `ACTION_SEND_MULTIPLE` intent-filter，接受 `*/*` MIME 类型（即任何格式）
- 新增 `ShareHandler` 组件，负责解析 incoming share intent、提取 text / URI / 多文件数据
- 支持的分享内容类型：
  - 纯文本（包括链接、笔记、剪贴板内容）
  - 单个文件（图片、视频、音频、PDF、任意文件）
  - 多个文件（批量导入）
- 复用已有的 `MindDumpRepository.saveTextEntry()` 和 `importFile()` 方法
- 添加空间选择 UI（Public / Private），让用户决定分享内容存放位置

## Capabilities

### New Capabilities
- `share-receiver`: 接收来自任何 Android 应用的分享内容（文本、单文件、多文件），解析 intent 并保存为 MindDump 条目

### Modified Capabilities
<!-- 无现有 specs -->

## Impact

- **AndroidManifest.xml**: 添加 intent-filter 声明
- **MainActivity.kt**: 添加 `onNewIntent()` 处理，启动分享流程
- **新增 `ShareHandler`**: intent 解析 + 数据提取逻辑
- **UI 层**: 新增分享确认/空间选择对话框（Compose overlay 或 dialog）
- **MindDumpRepository**: 无变更（复用现有 `importFile` / `saveTextEntry`）
- **FileStorageEngine**: 无变更（已有 `importFile(uri)` 支持）
- **用户体验**: 用户从任何应用"分享"→ 选择 MindDump → 弹出空间选择 → 完成
