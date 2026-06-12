## Context

MindDump 当前 UI 基于 Material 3 组件库，使用 NiA（Now in Android）设计体系。主题系统已有 dynamic color、自定义 Color/Typography/Shape、BackgroundTheme 和 TintTheme 扩展。但组件层面全部使用 M3 默认样式，缺乏品牌辨识度和情感化设计。主要组件包括 EntryItem（卡片）、InputBar（输入栏）、SpaceSwitchButton（空间切换）。

当前动画仅限：InputBar 的 AnimatedContent（200ms fade）和 SpaceSwitchButton 的 animateColorAsState（300ms）。无列表入场动画、无手势交互、无过渡动画。

## Goals / Non-Goals

**Goals:**
- 建立品牌化的动画与微交互体系，让 app 感觉"精致"和"活泼"
- 卡片系统视觉差异化（文本、照片、视频、音频各有独特视觉语言）
- 输入体验沉浸化，录音/发送有明确的视觉反馈
- 加载状态用骨架屏替代空白，空状态有品牌化插图
- 所有动画保持 60fps，不引入 jank

**Non-Goals:**
- 不重做信息架构或导航结构
- 不引入新的第三方动画库（如 Lottie），优先使用 Compose 原生 API
- 不改变数据层或加密逻辑
- 不做自定义 View 系统，全部基于 Composable
- 不做主题编辑器或用户自定义主题功能

## Decisions

### D1: 动画基础设施——基于 Compose Animation API

**选择**: 使用 `Compose Animation`（`AnimatedVisibility`、`AnimatedContent`、`updateTransition`、`Animatable`、`infiniteTransition`）+ `SharedTransitionLayout`（Compose 1.7+）。

**替代方案**:
- Accompanist Animation: 已被 Compose 官方吸收，不需要额外依赖
- Lottie: 过重，需要 JSON 资源文件，不适合微交互场景

**理由**: Compose 原生动画 API 完全覆盖需求，无额外依赖，与 M3 组件深度集成。SharedTransitionLayout 提供共享元素过渡。

### D2: 卡片视觉体系——渐变 + 微阴影 + 类型化装饰

**选择**: 每种 entry 类型使用独特的视觉线索：
- 文本: 左侧 4dp 渐变色条（primary→tertiary）+ subtle press scale animation
- 照片: 图片占位带 parallax 微偏移 + 底部渐变遮罩覆盖文字
- 音频: 波形装饰条 + 脉冲播放指示器
- 视频: 照片样式 + 播放按钮覆盖层

卡片统一使用 `CardDefaults.outlinedCardElevation()` 配合 `Modifier.graphicsLayer { shadowElevation }` 实现动态阴影。

**替代方案**:
- 玻璃拟态（Glassmorphism）: 在 API 29+ 上 `RenderEffect.createBlurEffect` 可用但性能不可控，且与 M3 design language 不一致
- 纯圆角大卡片: 过于常见，缺乏辨识度

### D3: 输入栏增强——状态机驱动的动画系统

**选择**: 用 `sealed interface InputBarState`（Collapsed、Expanded、Recording、Sending）驱动 `updateTransition`，每个状态定义不同的布局、颜色、图标动画。

录音状态使用 `infiniteTransition.animateFloat` 实现脉冲效果（scale + alpha 循环）。

**替代方案**:
- MotionLayout: 过于复杂，且处于实验阶段
- 纯 AnimatedVisibility 组合: 状态多时难以协调

### D4: 列表动画——`LazyColumn` + `AnimatedVisibility` + `SharedTransitionLayout`

**选择**: 使用 `LazyColumn` 的 `animateItemPlacement()` modifier 实现列表项位移动画。新项目出现使用 `AnimatedVisibility（fadeIn + expandVertically）`。删除使用 `AnimatedVisibility（fadeOut + shrinkVertically）`。

共享元素过渡（点击卡片进入全屏编辑）使用 `SharedTransitionLayout` + `Modifier.sharedElement()`。

**替代方案**:
- 自定义 ItemAnimator: Compose 不需要，LazyColumn 内置支持
- 第三方库（Epoxy, Groupie）: 不适用于 Compose

### D5: 骨架屏——自定义 Shimmer 效果

**选择**: 基于 `LinearGradient` + `translateAnimation` 的自定义 Shimmer Composable，使用 `Modifier.drawWithContent` 和 `Brush.linearGradient` 实现。

**替代方案**:
- Accompanist Placeholder: 已废弃，功能被 Compose 内置 placeholder 取代
- Compose Material 3 Placeholder: `Modifier.placeholder()` 可用但视觉自定义受限

**理由**: 自定义 Shimmer 可以完全匹配品牌色和动画曲线。

### D6: 主题扩展——新增 CompositionLocal

**选择**: 在现有 theme 系统中新增：
- `LocalGradientColors`: 提供品牌渐变色对（primaryGradient, cardGradient, inputGradient）
- `LocalAnimationDuration`: 统一动画时长常量（short=150ms, medium=300ms, long=500ms）
- `LocalMotionCurve`: 统一缓动曲线（emphasize, standard, decelerate）

**理由**: 通过 CompositionLocal 确保动画一致性，便于主题切换时统一更新。

## Risks / Trade-offs

- **[性能] 列表动画在大量条目时可能导致 jank** → 使用 `key` + `contentType` 优化 LazyColumn，动画仅在可见项上运行，使用 `derivedStateOf` 避免不必要重组
- **[兼容性] SharedTransitionLayout 需要 Compose 1.7+** → 项目已使用 Compose BOM 2024+，确保版本兼容；如果不可用，降级为 `Crossfade`
- **[复杂度] 状态机驱动 InputBar 增加测试成本** → 每个状态独立测试，使用 `StateFlow<InputBarState>` 便于 ViewModel 单元测试
- **[过度动画] 用户可能觉得动画过多/太慢** → 所有动画时长通过 `LocalAnimationDuration` 控制，提供"减少动画"开关（尊重 `Settings.System.ACCELEROMETER_ROTATION` 或 `AccessibilityManager`）
