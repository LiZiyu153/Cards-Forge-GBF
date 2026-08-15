# Forge 碧蓝幻想（GBF）自定义卡牌脚本项目

本项目是基于开源万智牌游戏引擎 **Card-Forge**（[github.com/Card-Forge/forge](https://github.com/Card-Forge/forge)，GPL-3.0 协议）的**自定义卡牌脚本项目**，为 Forge 游戏添加了一套完整的《碧蓝幻想》（Granblue Fantasy，代码 `GBF`）主题系列：包括卡牌脚本、衍生物脚本、系列定义与配套的测试/回归工具。

> ⚠️ 本项目仅包含**卡牌脚本与工程配套**（`.txt` 脚本、测试、工具），不包含 Forge 引擎本体。请自行获取 Forge 游戏安装目录，并将本仓库内容放入对应位置使用。

## 内容统计

| 内容 | 数量 |
|---|---|
| GBF 卡牌脚本 | 136 张（`res/cardsfolder/<首字母>/`） |
| GBF 衍生物脚本 | 5 个（`res/tokenscripts/`） |
| 系列定义 | `res/editions/Granblue Fantasy.txt` |

## 目录结构

| 路径 | 说明 |
|---|---|
| `res/cardsfolder/<letter>/` | 卡牌脚本（每卡一个 `.txt`，按卡名首字母分目录） |
| `res/tokenscripts/` | 本系列引用的衍生物脚本（5 个） |
| `res/editions/Granblue Fantasy.txt` | 系列定义：metadata + `[cards]` + `[tokens]` |
| `res/lists/TypeLists.txt` | 注册了本系列自定义种族（Draph / Erune / Harvin / Primal / Basara）的类型表 |
| `res/blockdata/blocks.txt`、`res/blockdata/fantasyblocks.txt` | 轮抓/环境接入：注册了 `Granblue Fantasy` 系列（各 1 行） |
| `tests/` | 无头解析/行为测试基建（Java，详见 `tests/README.md`） |
| `tools/` | 一键回归 gate（`run_all_tests.cmd`）与反模式 lint（`lint_gbf.py`） |
| `LICENSE.txt` | GPL-3.0 协议全文 |

## 使用方法

### 安装卡牌

1. 准备一个 Forge 游戏安装目录（本系列基于 `forge-installer-2.0.13` 开发）。
2. 将本仓库的 `res/` 内容合并进 Forge 安装目录的 `res/`（覆盖/合并同名目录）：
   - `cardsfolder/` → 卡牌脚本
   - `tokenscripts/` → 衍生物脚本
   - `editions/Granblue Fantasy.txt` → 系列定义
   - `lists/TypeLists.txt` → 自定义种族注册（**注意：这是整个官方类型表 + 本系列追加项，合并时请保留本仓库版本**）
   - `blockdata/blocks.txt` 与 `blockdata/fantasyblocks.txt` → 轮抓/环境接入（合并时追加本仓库中的 `Granblue Fantasy` 行即可）
3. 启动 Forge，即可在牌库编辑器/系列列表中找到 `Granblue Fantasy` 系列。

> 本系列轮抓使用 UNH 作为基本地牌集，已按此注册。

### 运行测试

开发环境需使用 Forge 安装目录的 fat jar 作 classpath（详见 `tests/README.md`）。一键回归：

```bat
tools\run_all_tests.cmd
```

- `-Lint`：只跑反模式 lint + 静态验证（快速档）
- `-Quick`：只跑解析测试
- `-Card "卡名"`：只测单卡

## 开发约定（新增卡牌流程）

1. 在 `res/cardsfolder/<首字母>/` 编写卡牌脚本（Forge 脚本语法）。
2. 在 `res/editions/Granblue Fantasy.txt` 的 `[cards]` 注册卡牌（新衍生物同步注册 `[tokens]`）。
3. 运行 `tools\run_all_tests.cmd` 全量回归（反模式 lint + 编译 + 全量解析 + 行为测试 + 静态验证），全部通过后提交。

## 许可证

本项目遵循 **GPL-3.0** 协议（见 `LICENSE.txt`），与上游 Card-Forge 引擎一致。使用、修改与分发请遵守 GPL-3.0 条款。

## 致谢

- [Card-Forge / forge](https://github.com/Card-Forge/forge) —— 上游开源万智牌引擎（GPL-3.0）
- 卡牌设计灵感来自《碧蓝幻想》（Granblue Fantasy）系列
