# 变更记录

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)，格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [0.0.1] - 2026-09-26

首个版本。

### 新增

- 四象限底栏：肉体 / 知识 / 社会关系 / 持有物。点击展开、默认应用自动进入、再点收起且**保留已打开的子应用**
- 展开尺寸按容器内子应用数量自适应，不是固定 3 倍
- 子应用容器：`WebViewAssetLoader` 起本地源（不用 `file://`，那会被 CORS 掐掉 ES 模块），`documentStart` 注入宿主上下文
- 底栏高亮当前打开的子应用及其所属象限容器
- 中英双语。`values/` 是中文基准，`values-en/` 英文，未支持的语言回落到中文

### 说明

- 发布的是 **debug APK**。还没有签名密钥，release 变体产出的未签名包装了会被系统拒
- 构建依赖 `moechat-ai/msglist` 的产物，CI 里会一并构建并放到同级目录
