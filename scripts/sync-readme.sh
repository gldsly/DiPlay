#!/usr/bin/env bash
# Rebuild README.md (English, what GitHub renders) and README.zh-CN.md (Simplified Chinese)
# as: this fork's header + upstream's own README text + this fork's differences.
#
# Why: both files keep upstream's own names, so an upstream README edit conflicts only in the header
# and the differences section instead of across the whole file. The middle is upstream text verbatim,
# so absorbing such a change is "run this script" instead of hand-merging prose:
#
#     git fetch origin
#     git merge origin/main          # README conflicts are fine, we overwrite them below
#     scripts/sync-readme.sh         # takes upstream's README text from origin/main
#     git add README.md README.zh-CN.md && git commit
#
# Usage: scripts/sync-readme.sh [upstream-ref]     (default: origin/main)
set -euo pipefail
cd "$(dirname "$0")/.."

UPSTREAM=${1:-origin/main}
git rev-parse --verify --quiet "$UPSTREAM" >/dev/null || {
    echo "sync-readme: unknown ref '$UPSTREAM' (run: git fetch origin)" >&2
    exit 1
}

EN_SRC="$UPSTREAM:README.md"
ZH_SRC="$UPSTREAM:README.zh-CN.md"

en_body=$(git show "$EN_SRC")
if git cat-file -e "$ZH_SRC" 2>/dev/null; then
    zh_body=$(git show "$ZH_SRC")
else
    echo "sync-readme: $ZH_SRC is gone upstream; falling back to the English text" >&2
    zh_body=$en_body
fi

# Drop upstream's own title line; this file supplies its own.
strip_title() { printf '%s\n' "$1" | sed '1{/^# DiPlay$/d;}'; }

read -r -d '' ZH_HEADER <<'ZH' || true
**简体中文** · [English](README.md)

> **个人 fork，由 AI 协助开发。** `gldsly/DiPlay` 跟随上游
> [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay)（它又基于
> [shilapi/xcertplay](https://github.com/shilapi/xcertplay)），并持续合并上游代码。同一功能两边
> 都有时，以上游实现为准，除非它对这台车明显更差。
>
> 本仓库的改动由 AI 助手与车主共同完成：AI 实现改动并说明依据，车主在真车上驾驶、测试并决策；
> 每条结论都先对着代码核实再下判断。问题反馈、发布与支持以上游为准，本 fork 不接收。
> **本仓库的任何产物都不是官方 DiPlay 发布。**

---
ZH

read -r -d '' EN_HEADER <<'EN' || true
**English** · [简体中文](README.zh-CN.md)

> **Personal fork, developed with an AI assistant.** `gldsly/DiPlay` tracks
> [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay), which is itself based on
> [shilapi/xcertplay](https://github.com/shilapi/xcertplay). Upstream releases are merged in as they
> land; where a feature exists on both sides, upstream's version wins unless it is demonstrably worse
> for this car.
>
> The work here is written by an AI assistant together with the car owner: the assistant implements
> changes and states its evidence, the owner drives the car, tests and decides. Findings are checked
> against the running code before they are reported or acted on. Upstream remains the authority for
> releases, issues and support, and this fork accepts neither. **Nothing here is an official DiPlay
> release.**

---
EN

read -r -d '' ZH_DIFFS <<'ZH' || true

## 本 fork 与上游的差异

本 fork 与上游是同一个应用：差异只有一组上游尚未包含的修复，加上从 fork 点之后合并进来的上游
新代码，其余部分与上游一致。

- **设置往返**：从设置页返回不再重建 CarPlay 会话；车机顶部/底部状态栏改为两个独立开关，两个界面
  遵循同一策略。
- **音乐缓冲**：新增 100 / 200 毫秒档，以及按实测到达间隔自适应的「自动」档。**默认自动**——车
  上实测中固定档仍会重缓冲，自动档不会。
- **音频容错**：音频启动失败会退避重试，而不是整场静音；音频焦点丢失后音量不再卡在压低值；
  音乐音量只有一个写入者。
- **连接恢复**：冷启动首次绑定会等待 Wi-Fi Direct 地址就绪（不再失败后多等 2 秒）；本次会话创建的
  Wi-Fi Direct 组（含系统命名）能被正确清理；VPN 服务被回收后可以重新绑定。
- **导航音频**：导航声道默认 15（本车实测值，上游默认自动路由）；「导航压低音乐」与「音频焦点」
  互斥，因为两者调整的是同一个音乐音量。
- **诊断**：导航输出被迫停止、日志过期回调、Activity 线程池泄漏现在都会明确记录或释放，不再静默。
- **日志级别**：音频、视频与接收的周期统计行属于 debug 级别，默认不写进日志——正常会话只记录状态变化、
  警告与错误。需要排查时可在「设置 → 诊断 → 记录周期统计」打开，此时日志约快四倍写满并轮转。

## 来源与致谢

基于 [xcertplay](https://github.com/shilapi/xcertplay)，GPL-3.0。首页/设置界面与网站改编自
[DiAuto](https://github.com/shihabal3amri/DiAuto)，AGPL-3.0，其许可证包含在 `docs/licenses` 中。
分发修改版时请保留这些声明。CarPlay 及其图标属于 Apple Inc.；本项目与 Apple、比亚迪均无关联或
背书关系。完整说明见[英文 README](README.md) 的 Source and credits 一节。
ZH

read -r -d '' EN_DIFFS <<'EN' || true

## Fork differences

This fork is the same app. The differences are a small set of fixes upstream does not have yet,
plus whatever upstream has released since the fork point; everything else is upstream code.

- **Settings round trip.** Leaving the settings screen no longer restarts the CarPlay session, and
  the head unit's top and bottom bars are separate switches that both screens follow.
- **Music buffer.** 100 ms and 200 ms join 300/500/1000, along with an auto window sized from the
  arrival gaps the link actually shows. Auto is the default: measured in the car, the fixed windows
  still rebuffered where auto did not.
- **Audio resilience.** A failed audio start retries with backoff instead of going silent for the
  rest of the session, and losing audio focus no longer leaves the music at ducked volume.
- **Connection recovery.** The first wireless bind after a cold start waits for the fresh Wi-Fi
  Direct address instead of failing and retrying two seconds later; a Wi-Fi Direct group this session
  created is cleaned up even when the framework named it; the VPN binding can be re-established
  after the framework drops the service.
- **Guidance audio.** Guidance keeps stream 15, the stream measured on the BYD head unit; upstream
  defaults to automatic routing there instead. Guidance ducking and audio focus are mutually
  exclusive, because both adjust the same music volume.
- **Diagnostics.** A guidance output that had to stop, the log-expiry callback and the activity's
  thread pools now say so instead of leaking quietly.
- **Log level.** The periodic audio, video and receive stats lines are debug-level and stay out of
  the log by default, so an ordinary session records state changes, warnings and errors only.
  Settings - Diagnostics - "Write periodic stats" brings them back for a problem report, and the log
  then fills and rotates about four times faster.
EN

# Upstream's own file names are kept, so an upstream README edit conflicts only in the header and the
# differences section; the middle stays upstream text verbatim.
printf '# DiPlay\n\n%s\n%s\n%s\n' "$EN_HEADER" "$(strip_title "$en_body")" "$EN_DIFFS" > README.md
printf '# DiPlay\n\n%s\n%s\n%s\n' "$ZH_HEADER" "$(strip_title "$zh_body")" "$ZH_DIFFS" > README.zh-CN.md

git --no-pager diff --stat README.md README.zh-CN.md
