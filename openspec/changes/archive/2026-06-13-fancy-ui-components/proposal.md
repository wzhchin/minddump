## Why

MindDump 的 UI 架构虽然采用了 Material 3，但视觉体验偏"标准模板"——卡片、输入框、按钮都是开箱即用的 M3 组件，缺乏品牌辨识度和情感化设计。作为一个私密"大脑倾倒"工具，界面应该让人感觉精致、沉浸、有趣，而不是像一个技术 demo。用户反馈"不够 fancy"，说明需要在动画、微交互、视觉层次和品牌化组件上做系统性提升。

## What Changes

- **重新设计 Entry 卡片**：增加渐变、阴影层次、微动画（hover/press 反馈）、时间线视觉连接
- **升级 InputBar**：添加打字机动画、发送反馈、录音状态脉冲动画、展开/收起动画
- **品牌化主题增强**：自定义渐变色、玻璃拟态（glassmorphism）效果、动态背景
- **过渡动画体系**：页面切换、列表项出现/消失、搜索展开/收起的共享元素过渡
- **加载与空状态**：骨架屏（skeleton/shimmer）、自定义插图空状态
- **手势微交互**：滑动删除（swipe-to-dismiss）、卡片长按涟漪效果、拖拽排序反馈

## Capabilities

### New Capabilities
- `fancy-animations`: 动画与微交互体系——页面过渡、列表动画、手势反馈、脉冲/涟漪效果
- `fancy-card-system`: 高级卡片设计系统——渐变、玻璃拟态、时间线布局、entry 类型视觉差异化
- `fancy-input-bar`: 沉浸式输入体验——打字机效果、录音脉冲、发送反馈、展开动画
- `fancy-visual-polish`: 视觉打磨层——骨架屏、shimmer 加载、空状态插图、品牌化主题增强

### Modified Capabilities
<!-- 无已有 spec 需要修改 -->

## Impact

- **ui/components/**: EntryItem、InputBar、SpaceSwitchButton 需要大幅重写或增强
- **ui/theme/**: 新增渐变定义、扩展 Color/Typography/Shape、添加 CompositionLocal 供动画系统消费
- **ui/screens/**: MainScreen、CameraScreen、FullscreenEditScreen 需集成新过渡动画
- **依赖**: 可能新增 `accompanist` 动画库或使用 Compose 1.7+ 内置动画 API
- **性能**: 动画需注意 60fps，使用 `derivedStateOf`、`remember` 和 `Animatable` 避免重组
