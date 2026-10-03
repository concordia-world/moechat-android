# moechat-android

[![CI](https://github.com/concordia-world/moechat-android/actions/workflows/ci.yml/badge.svg)](https://github.com/concordia-world/moechat-android/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/concordia-world/moechat-android?color=blue)](https://github.com/concordia-world/moechat-android/releases)

moechat 的 Android 宿主。

## 架构

**骨架原生，子应用 Web。**

- **宿主（本仓库）**：Kotlin + Compose 实现骨架——顶栏、底栏、四个象限容器、主内容区
- **子应用**：Web 应用，跑在宿主的 WebView 里，各自独立成仓库。当前接了 [`msglist`](https://github.com/concordia-world/msglist)（第二象限）

「五端统一交互风格」靠**同一套设计规范**约束各端原生实现，而不是靠共享代码。

## 已实现

- [x] 顶栏：主体图标、可输入的时空地址栏、后退 / 前进 / 刷新
- [x] 底栏：四个象限容器（肉体 / 知识 / 社会关系 / 持有物）
- [x] 点击容器展开，尺寸按容器内子应用数量自适应（**不是固定 3 倍**）
- [x] 点击容器时该象限的默认应用自动进入；再点收起时**保留**已打开的子应用
- [x] 底栏高亮当前打开的子应用及其所属象限容器
- [x] 子应用容器（WebView）＋ 本地源 ＋ 宿主↔子应用通信协议
- [x] 中英双语
- [ ] 主体切换面板
- [ ] 时空历史导航（后退 / 前进 / 刷新的语义）

## 构建

**前置：`msglist` 必须放在本仓库的父目录下，且已构建。**

```
<父目录>/
├── moechat-android/     ← 本仓库
└── msglist/dist/        ← 必须有
```

`app/build.gradle.kts` 里的 `syncSubApps` 会从 `../../msglist/dist` 拷贝子应用产物到
`build/generated/assets/`（已被 `.gitignore` 覆盖，所以仓库里不留第二份副本）。
**没有它就直接构建失败**，并打印该跑的命令——不会静默产出一个没有子应用的包。

```sh
# 1. 先构建子应用
cd ../msglist && npm install && npm run build

# 2. 再构建宿主
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 子应用怎么被加载

走 `WebViewAssetLoader` 起的 `https://appassets.androidplatform.net`。

**不能用 `file://`**：Android WebView 在 file:// 下会因 CORS 拒绝 ES 模块，
子应用的 `<script type="module">` 直接不执行。

宿主上下文与回调桥在 `documentStart` 注入。Android 的 `@JavascriptInterface` 只能收 String，
所以 stringify 补在 JS 层——这样**五端对子应用暴露的是同一份契约**：

```js
window.moechat                                  // 只读快照：subjectId / subjectName / spacetime / theme / locale
window.moechatHost.post(type, payloadObject)    // 回传
```

## 多语言

`res/values/` 是**中文基准**（任何未支持的语言回落到它），`res/values-en/` 是英文。
子应用标题用 `@StringRes Int` 而非 `String`，所以漏改的调用点编译器会点名。

传给子应用的 `locale` 是 `Locale.getDefault().toLanguageTag()`，即系统偏好语言的
原样 BCP-47 标签。**宿主不是子应用的语言权威**——子应用按同一份规范各自兜底。

## 发布

推 `v*` tag 即触发 `release.yml`，产出 APK 挂到 Release 上。

**产物是 debug APK**：还没有签名密钥，release 变体产出的未签名包装了会被系统拒。

## 图标

自适应图标（API 26+）。**本项目 `minSdk = 26`，所以只会走这一条路径**，
不再提供各密度的传统 PNG 图标。

```
app/src/main/res/
├── mipmap-anydpi-v26/ic_launcher.xml      自适应图标定义
├── drawable/ic_launcher_background.xml    底色（纯白）
└── mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png
```

**前景层是「从白卡里抠出来的美术」，不是整张卡片。** 两条理由：

- 卡片本身就是白的，放进自适应图标会和背景层重复
- 卡片自带内边距，直接缩放会让美术比实际需要的更小

抠图 = 从图像边框做「近白像素」的连通填充（白卡连通到边框，气泡里的眼白被气泡包围、够不到），
再做一轮宽松阈值的二次填充吃掉抗锯齿边。

**源图不在本仓库**，在 [`moechat-spec`](https://github.com/concordia-world/moechat-spec)
的 `brand/logo/icon/`。
