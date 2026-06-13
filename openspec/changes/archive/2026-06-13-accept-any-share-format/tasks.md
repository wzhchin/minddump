## 1. AndroidManifest & Activity 配置

- [x] 1.1 在 `AndroidManifest.xml` 中为 `MainActivity` 添加 `ACTION_SEND` intent-filter，MIME 类型 `*/*`
- [x] 1.2 在 `AndroidManifest.xml` 中为 `MainActivity` 添加 `ACTION_SEND_MULTIPLE` intent-filter，MIME 类型 `*/*`
- [x] 1.3 给 `MainActivity` 添加 `android:launchMode="singleTask"` 确保分享时复用已有实例

## 2. Share 数据模型

- [x] 2.1 在 `storage/` 包下创建 `ShareItem.kt`，定义 sealed class：`ShareItem.Text(content: String)` 和 `ShareItem.File(uri: Uri, fileName: String)`

## 3. ViewModel 分享处理逻辑

- [x] 3.1 在 `MindDumpViewModel` 中添加 `pendingShareItems: StateFlow<List<ShareItem>?>` 状态，默认 `null`
- [x] 3.2 实现 `handleShareIntent(intent: Intent)` 方法：解析 `ACTION_SEND` 提取 `EXTRA_TEXT` 或 `EXTRA_STREAM`，解析 `ACTION_SEND_MULTIPLE` 提取 URI 列表
- [x] 3.3 实现文件名解析辅助方法：通过 `ContentResolver.query()` 获取 `DISPLAY_NAME`，fallback 到 `lastPathSegment` → `"shared_file"`
- [x] 3.4 实现 `confirmShare(space: Space)` 方法：遍历 `pendingShareItems`，调用 `repository.saveTextEntry()` 或 `repository.importFile()`，完成后清空 pending 状态
- [x] 3.5 实现 `cancelShare()` 方法：清空 `pendingShareItems` 状态

## 4. MainActivity Intent 处理

- [x] 4.1 在 `MainActivity.onCreate()` 中检查 `intent?.action`，如果是 `ACTION_SEND` 或 `ACTION_SEND_MULTIPLE`，调用 `viewModel.handleShareIntent(intent)`
- [x] 4.2 覆写 `MainActivity.onNewIntent()`，调用 `viewModel.handleShareIntent(newIntent)`
- [x] 4.3 通过 `setContent {}` 中的 lambda 将 share intent 传递给 ViewModel

## 5. 空间选择 UI

- [x] 5.1 在 `MindDumpNavGraph` 或 `MainScreen` 中观察 `pendingShareItems`，非 `null` 时显示空间选择 Dialog
- [x] 5.2 Dialog 内容：显示分享摘要（"1 张图片" / "3 个文件" / "1 段文字"）+ "公开" 和 "私密" 两个按钮
- [x] 5.3 Dialog 取消/外部点击时调用 `viewModel.cancelShare()`
- [x] 5.4 选择空间后调用 `viewModel.confirmShare(space)` 并显示 Snackbar 确认

## 6. 边界情况处理

- [x] 6.1 `handleShareIntent` 中对 URI 读取添加 try-catch，失败时显示错误提示并跳过该项
- [x] 6.2 处理 Private 空间未解锁场景：选择 Private 时如果 `!repository.isSessionUnlocked()`，保存但不加密，UI 提示"未加密保存"
- [x] 6.3 确保分享处理在 IO 线程（`viewModelScope.launch(Dispatchers.IO)`），UI 更新切回 Main

## 7. 验证

- [x] 7.1 构建通过（`./gradlew assembleDebug`），Detekt/ktlint 无报错
- [ ] 7.2 手动测试：从浏览器分享文字 → 选择 MindDump → 选公开 → 条目出现在列表
- [ ] 7.3 手动测试：从相册分享图片 → 选择 MindDump → 选私密 → 文件保存并加密
- [ ] 7.4 手动测试：从文件管理器多选文件分享 → 批量保存成功
- [ ] 7.5 手动测试：分享时取消 Dialog → 无条目创建，UI 恢复正常
