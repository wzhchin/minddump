## Context

MindDump 是一个加密的"brain dump"应用，目前只支持应用内创建条目（文字、拍照、录音、录像）。`MindDumpRepository` 已有 `importFile(uri, fileName)` 和 `saveTextEntry(space, content)` 方法，`FileStorageEngine` 也已支持从 content URI 复制文件。但 `AndroidManifest.xml` 中没有声明任何 `ACTION_SEND` intent-filter，`MainActivity` 也没有处理 incoming share intent 的逻辑。

当前 `MainActivity` 是 single-activity 架构，使用 Hilt 注入、Compose UI、`MindDumpViewModel` 管理状态。UI 通过 `currentSpace: StateFlow<Space>` 跟踪当前所在空间（Public/Private）。

## Goals / Non-Goals

**Goals:**
- 用户从任何 Android 应用通过系统 Share sheet 分享内容到 MindDump
- 接受任何 MIME 类型：纯文本、图片、视频、音频、文档、任意文件
- 支持单文件和 `ACTION_SEND_MULTIPLE` 多文件批量分享
- 分享时弹出空间选择（Public / Private），选择后自动保存
- 保存完成后显示简短确认（Snackbar 或 Toast），然后自动关闭 activity（如果是独立的分享流程）
- 复用已有的 `importFile()` / `saveTextEntry()` 方法，最小化新代码

**Non-Goals:**
- 不做分享前的内容预览/编辑（MVP 直接保存）
- 不支持分享到特定日期文件夹（总是保存到"今天"）
- 不做"分享后打开 MindDump 编辑"的流程
- 不处理 Private 空间未解锁时的密码输入——未解锁时默认保存到 Public

## Decisions

### 1. Intent-filter 策略：使用 `*/*` 通配

**选择**: 在 `ACTION_SEND` 和 `ACTION_SEND_MULTIPLE` 的 intent-filter 中使用 `android:mimeType="*/*"`

**原因**: 用户希望接受任何格式、任何应用的分享。使用 `*/*` 让 MindDump 出现在所有 Share sheet 中，无需逐一列举 MIME 类型。

**替代方案**: 声明多个具体 MIME 类型（`image/*`, `video/*`, `text/plain`, `application/*`）——更精确但无法覆盖所有未知文件类型，且 Android manifest 中不支持 `application/*` 这种子通配方式，需要逐一列出。

### 2. 架构：直接在 ViewModel 中处理，不新建独立组件

**选择**: 在 `MindDumpViewModel` 中添加 `handleShareIntent(intent: Intent)` 方法，由 `MainActivity.onNewIntent()` 和 `onCreate()` 调用。

**原因**:
- ViewModel 已经持有 `MindDumpRepository` 引用和 `currentSpace` 状态
- 分享流程简单（解析 → 存空间选择 → 保存），不需要独立 UseCase
- 保持与现有架构一致（所有业务逻辑在 ViewModel）

**替代方案**: 创建独立的 `ShareHandler` 类——增加了不必要的间接层，这个逻辑不会在多个地方复用。

### 3. 空间选择：Compose Dialog overlay

**选择**: 在 ViewModel 中添加 `pendingShareItems: StateFlow<List<ShareItem>?>` 状态。当有分享内容时，UI 层弹出 Material 3 Dialog，让用户选择 Public 或 Private，选择后触发保存。

**原因**:
- 符合 Compose 单向数据流模式
- Dialog 是最轻量的交互方式，不需要新 screen 或 navigation route
- 用户需要明确选择空间（因为加密需求），不能静默默认

### 4. 分享内容数据模型：sealed class ShareItem

**选择**:
```kotlin
sealed class ShareItem {
    data class Text(val content: String) : ShareItem()
    data class File(val uri: Uri, val fileName: String) : ShareItem()
}
```

**原因**: 分享内容只有两种——纯文本或文件 URI。sealed class 提供穷举安全性。

### 5. Activity 启动模式：使用 `singleTask` (或 `singleTop`)

**选择**: 给 `MainActivity` 添加 `android:launchMode="singleTask"`，确保分享时不会创建多个 activity 实例。通过 `onNewIntent()` 处理后续分享。

**原因**: 分享 intent 可能从其他应用发起，如果 activity 已在后台，`singleTask` 确保复用现有实例并通过 `onNewIntent()` 传递 intent，避免堆栈混乱。

**替代方案**: `singleTop` —— 如果 activity 在栈顶则复用，否则创建新实例。但分享场景下更希望始终复用。考虑到 MindDump 是单 activity 应用，`singleTask` 更安全。

### 6. 文件名提取：ContentResolver query

**选择**: 通过 `ContentResolver.query(uri, projection, ...)` 获取 `OpenableColumns.DISPLAY_NAME` 作为原始文件名。失败时 fallback 到 `uri.lastPathSegment` 或 `"shared_file"`。

**原因**: 分享过来的 content URI 没有有意义的路径，需要从 ContentResolver 查询原始文件名以保持可读性。

## Risks / Trade-offs

- **[大文件分享]** → 分享大文件时 `importFile()` 在 IO 线程复制，不会阻塞 UI。但无进度显示——MVP 阶段可接受。
- **[Private 空间未解锁]** → 如果用户选择 Private 但密码未设置/未解锁，当前 fallback 为直接保存（不加密）。需要在 UI 层做提示。
- **[权限问题]** → content URI 由发送方通过 `FLAG_GRANT_READ_URI_PERMISSION` 授权，通常不需要额外权限。但某些设备/应用可能不正确设置 flag，导致读取失败——需要 try-catch 和用户提示。
- **[intent-filter 过于宽泛]** → `*/*` 会让 MindDump 出现在所有分享场景，包括不太相关的（如分享联系人）。这是设计目标（接受任何格式），但可能降低 Share sheet 中的可见性优先级。
- **[singleTask 的Activity管理]** → 当用户从分享打开一个已经在后台的activity实例时，之前的返回栈状态会被清除。这对于 MindDump 这种单 activity app 影响不大。
