# Project TODOs

## Widget Experience (High Priority)
- [x] **Fix Widget Navigation**:
  - `MainActivity` ignores the `SELECTED_DATE` intent. Needs to handle start-up intent to jump to the correct date.
  - **完成** (2026-02-01): 已修复，`MainActivity` 现在解析 `SELECTED_DATE` 参数并传递给 `MonthScreen`

- [x] **Widget Month Browsing**:
  - The widget is stuck on the current month. Needs "Next/Prev Month" buttons and store the offset in Preferences.
  - **完成** (2026-02-01): 添加了月份导航按钮，在 `WidgetSettingsRepository` 中存储偏移量，实现月份切换逻辑

- [x] **Data Synchronization**:
  - Add `android.intent.action.PROVIDER_CHANGED` receiver to `AndroidManifest` to refresh widget when system calendar changes.
  - **完成** (2026-02-01): 已在 `AndroidManifest.xml` 中添加 `PROVIDER_CHANGED` 接收器，并在 `CalendarWidget` 中处理变化事件

- [x] **Visual Bugs**:
  - Multi-day event indicators break if the event falls out of the top 2 slots. Needs smarter sorting/prioritization for Widget display.
  - **完成** (2026-02-01): 优化事件排序算法，优先显示多日事件，按事件出现天数降序排序

- [x] **Localization**:
  - Remove hardcoded `Locale.CHINA` date formatting in `CalendarWidget.kt`.
  - **完成** (2026-02-01): 已替换为 `Locale.getDefault()`，支持系统区域设置

## App Usability
- [x] **Navigation**:
  - Add a "Back to Today" button in `MonthScreen`. Currently difficult to return after scrolling.
  - **完成** (2026-02-01): 在下拉菜单中添加"返回今天"选项，点击后跳转回当前月份并选中今天日期

- [x] **Onboarding**:
  - Improve permission request flow with an explanation screen before the system dialog.
  - **完成** (2026-02-01): 添加 `PermissionExplanationScreen` 解释屏幕，优化权限请求流程

## 新增建议功能
- [ ] **性能优化**:
  - 优化小部件背景生成，避免每次更新都创建新位图
  - 添加事件缓存机制，减少日历查询频率

- [ ] **用户体验改进**:
  - 添加小部件配置向导，引导用户设置颜色和透明度
  - 支持更多日期格式选项

- [ ] **测试覆盖**:
  - 添加小部件功能单元测试
  - 添加UI自动化测试

