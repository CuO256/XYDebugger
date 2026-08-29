# XYDebugger

LSPosed 模块，为**小猿口算**（`com.fenbi.android.leo`）的口算 PK 提供**全对模式**与**自动答题**。

口算 PK 的判题方式是「原生手写识别 + 前端字符串比对」：把答案手写在答题区，原生识别出文字后，前端用 `question.answers.includes(recognizeResult)` 判断对错。本模块拦截并改造这条判题链路，让任意识别结果都判对，并自动在答题区画随机笔画触发识别。

## 功能

- **全对模式**：注入控制脚本覆写 `Array.prototype.includes`，识别结果恒判对，支持分数约分等场景。
- **自动答题**：定时在答题 canvas 上模拟 `MouseEvent` 笔画（随机起点/方向/贝塞尔弯曲/抖动，模拟真实手速），触发原生手写识别，延迟可调。
- **实时控制面板**：纯代码构建的悬浮面板叠加在小猿当前 Activity 上，无需任何悬浮窗权限；配置读写小猿自身 `SharedPreferences`，改动实时生效。

## 工作原理

```
取题(含标准答案 answers[])
   ↓
手写识别: 前端把 {strokes, expectedResult, ruleType} 交给原生 "recognize"
   ↓
前端判题: answers.includes(识别结果)   ← 本模块的注入点
   ↓
CORRECT → 加分/下一题   FAULT → 重答
```

注入方式（双通道，必中正在使用的 WebView）：

1. **shouldInterceptRequest 拼接**：判题 JS（`Oral-legacy.*.js` / `index-legacy.*.js` 等运行时按需加载的 chunk）走 WebView 请求加载时，把控制脚本拼到 JS 内容前面一起返回，控制脚本与判题逻辑必然在同一 JS 上下文执行。
2. **evaluateJavascript 追加**：当小猿在活跃 WebView 上执行判题相关 JS 时，把控制脚本前缀追加进去同一次执行；同时 `onPageFinished` 后主动注入。

控制脚本（`AUTO_ANSWER_JS` / `buildJsFilePrefix`）通过覆写 `includes` 与定时派发 `MouseEvent` 实现全对与自动答题，所有逻辑包在 `try/catch`，异常不影响判题模块自身执行。

## 使用方法

0. **版本要求**：仅推荐使用小猿口算 **3.140.1**，其余版本未做测试。
   [下载 3.140.1](https://m.wandoujia.com/apps/7695003/history_v31400199)

1. 在 LSPosed 中启用本模块，勾选作用域 **小猿口算**（`com.fenbi.android.leo`，模块内已预置）。
2. **强制停止小猿口算并重新打开**（模块改动需冷启动才生效）。
3. 小猿进程内会显示 "XY Debugger" 控制面板：
   - **全对模式**：任意识别都判对
   - **自动答题**：定时画笔画触发识别，延迟滑块 + 预设档位（0.5/1/1.5/2/3s）
   - 头部可拖拽定位、点击折叠
   - 配置即改即生效，持久化到小猿进程 prefs

## 构建

源码自带 Xposed API stub（`de.robv.android.xposed.*`），**build.gradle 无需添加 Xposed/LSPosed 依赖**，可直接编译(需手动在删除apk内的xposed相关类)

- 环境要求：compileSdk 33 / minSdk 24 / targetSdk 33，AGP 8.0.0

## 项目结构

```
app/src/main/
├── assets/xposed_init                 # 模块入口声明
├── AndroidManifest.xml                # xposed 元数据 + INTERNET 权限（判题 JS 注入下载）
├── java/
│   ├── de/robv/android/xposed/        # Xposed API stub（编译期占位）
│   └── xiaoyuan/debugger/
│       ├── OralPkJudgeHook.java       # 模块入口：注入控制脚本 + hook shouldInterceptRequest
│       ├── InAppOverlay.java          # 小猿进程内控制面板（纯代码构建）
│       ├── Config.java                # 配置模型 + SharedPreferences 持久化
│       └── MainActivity.java          # 模块应用说明页
└── res/                               # 应用图标、主题、说明页布局
```

## 免责声明

本模块仅供学习研究 Xposed / WebView 注入技术使用，请勿用于违反平台规则的作弊行为。使用造成的一切后果由使用者自行承担。
