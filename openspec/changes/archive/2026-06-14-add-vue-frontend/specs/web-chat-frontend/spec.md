## ADDED Requirements

### Requirement: 提供工程化的 Vue 前端工程

系统 SHALL 在仓库根的 `web/` 子目录下提供一个独立的 Vue 3 + Vite + TypeScript 工程，使用 pnpm 管理依赖。该工程 MUST 能独立 `pnpm install` / `pnpm dev` / `pnpm build` / `pnpm test` / `pnpm lint`。

#### Scenario: 开发模式启动
- **WHEN** 开发者在 `web/` 目录运行 `pnpm dev`
- **THEN** Vite 开发服务器监听 5173 端口，浏览器访问 `http://localhost:5173` 看到聊天页面
- **AND** 所有 `/api/*` 请求通过 Vite proxy 转发到 `http://localhost:10010`

#### Scenario: 生产构建落点
- **WHEN** 开发者在 `web/` 目录运行 `pnpm build`
- **THEN** 构建产物输出到 `../server/src/main/resources/static/` 并清空该目录原有内容
- **AND** Spring Boot `mvn package` 后生成的 jar 包含这些静态资源，部署仍然是单进程 10010

### Requirement: 同步聊天

系统 SHALL 允许用户提交一条 prompt 并接收完整的 AI 回复。

#### Scenario: 用户在同步模式下发送一条消息
- **WHEN** 用户在设置面板将"接口模式"切换为"同步"，在输入框键入 prompt 并按 Enter
- **THEN** 前端 POST `/api/chat` 请求体为 `{sessionId, userId, prompt}`
- **AND** 收到响应后整段文字一次性显示在 AI 消息位置

### Requirement: 流式聊天

系统 SHALL 默认使用流式接口，让用户逐字看到 AI 回复，并可中途停止。

#### Scenario: 用户发送消息触发流式输出
- **WHEN** 用户在默认模式下发送消息
- **THEN** 前端 POST `/api/streamChat` 请求体为 `{sessionId, userId, prompt}`
- **AND** 服务端响应 Content-Type 为 `text/plain;charset=UTF-8`
- **AND** 前端逐块读取响应字节并实时追加到 AI 消息内容
- **AND** UI 自动滚动到最新内容（除非用户已手动滚上去）

#### Scenario: 用户按停止按钮中断流式输出
- **WHEN** 流式输出过程中，用户点击发送按钮位置出现的停止按钮
- **THEN** 前端调用 AbortController abort，已收到的内容**保留**显示
- **AND** 该 AI 消息标记 `stoppedByUser`，UI 在内容末尾显示灰字"已停止"
- **AND** 用户可立即发送下一条消息

### Requirement: 后端流式接口产出 text/plain

系统 SHALL 让后端 `POST /api/streamChat` 接口以 `text/plain;charset=UTF-8` 协议产出 `Flux<String>` 内容，每个 String 片段直接作为字节追加到响应体。

#### Scenario: curl 探查响应头
- **WHEN** 调用方 `curl -i -X POST http://localhost:10010/api/streamChat -H 'Content-Type: application/json' -d '{...}'`
- **THEN** 响应头 `Content-Type: text/plain;charset=UTF-8`
- **AND** 响应体是无任何包装协议（无 `data:` 前缀、无 JSON 数组）的连续字符串字节流

### Requirement: 单消息重新生成

系统 SHALL 允许用户对最近一条 AI 消息发起"重新生成"，复用其之前的 user prompt。

#### Scenario: 用户点击重新生成
- **WHEN** 用户对最近一条 AI 消息点击"重新生成"按钮
- **THEN** 前端定位到该 AI 消息前面的 user 消息
- **AND** 把 AI 消息内容**就地清空**并重新进入流式接收
- **AND** **不**追加新的 user 消息到消息列表

### Requirement: Markdown 与代码块渲染

系统 SHALL 把 AI 消息渲染为 Markdown，代码块使用 shiki 引擎做语法高亮，且每个代码块带"复制"按钮。

#### Scenario: AI 回复中包含代码块
- **WHEN** AI 回复包含 ```` ```java\n...\n``` ```` 段落
- **THEN** UI 渲染该段落为高亮代码块（VS Code 同款 TextMate 引擎）
- **AND** 代码块右上角浮现"复制"按钮，点击后将代码原文写入剪贴板并显示一次性"已复制"反馈

### Requirement: 单消息复制

系统 SHALL 在 AI 消息悬停时浮现复制按钮，点击后将该消息的纯文本（去 markdown 标记）写入剪贴板。

#### Scenario: 用户复制 AI 消息
- **WHEN** 鼠标悬停在 AI 消息上 → 出现 [复制] 按钮 → 点击
- **THEN** 该消息的纯文本写入剪贴板
- **AND** 按钮显示一次性"已复制"反馈后恢复

### Requirement: 设置面板与持久化

系统 SHALL 提供一个从右侧滑入的设置面板，允许用户配置 userId、sessionId、接口模式（同步 / 流式）、主题，并将这些设置持久化到 localStorage。

#### Scenario: 修改设置后刷新页面
- **WHEN** 用户在设置面板中修改 userId 或主题，关闭面板
- **THEN** 设置写入 localStorage（key: `miles.settings`）
- **AND** 刷新页面后这些设置仍然生效

#### Scenario: 首次进入页面时初始化 sessionId
- **WHEN** 用户首次访问页面，localStorage 中没有 `sessionId`
- **THEN** 系统自动生成一个 uuidv4 作为 sessionId 并持久化

### Requirement: 主题切换

系统 SHALL 支持浅色 / 深色 / 跟随系统三种主题模式，浅色与深色对应不同的 CSS 变量。

#### Scenario: 切换主题
- **WHEN** 用户在 header 点击主题按钮，或在设置面板切换主题模式
- **THEN** `<html>` 元素的 `dark` class 立即添加或移除
- **AND** 整个 UI 的背景色、文字色、accent 色立即按对应变量重新着色
- **AND** 所选主题模式持久化到 localStorage

### Requirement: 重置会话

系统 SHALL 允许用户通过设置面板的"重置会话"按钮清空当前对话并开始新会话。

#### Scenario: 用户点击重置会话
- **WHEN** 用户在设置面板点击"重置会话"
- **THEN** chat store 中的 messages 数组清空
- **AND** sessionId 替换为新生成的 uuidv4 并持久化
- **AND** 后端 Redis 中旧 sessionId 的记忆**保持不动**（自然过期或被覆盖）

### Requirement: 空态示例 prompt

系统 SHALL 在消息列表为空时，居中显示欢迎语和三条点击即发送的示例 prompt。

#### Scenario: 首次进入页面看到空态
- **WHEN** 用户首次进入聊天页面，messages 数组为空
- **THEN** UI 显示欢迎语和 3 条示例 prompt
- **AND** 点击任一示例 prompt 直接触发 `chatStore.send(prompt)`，等同于用户键入并发送

### Requirement: 错误的内联展示

系统 SHALL 把与具体消息相关的错误（连接失败、4xx/5xx、流式中断）以红字内联在该消息上展示，并提供"重试"操作。

#### Scenario: 后端不可达
- **WHEN** 用户发送消息时后端无法连接（fetch 抛 TypeError 或返回 502 / 连接被拒）
- **THEN** 该 AI 消息内容区显示红字提示"无法连接后端"
- **AND** 显示"重试"按钮，点击后重新发起同一请求

#### Scenario: 流式中途断开
- **WHEN** 流式接收过程中连接中断（非用户主动停止）
- **THEN** 已收到内容**保留**显示
- **AND** 内容末尾追加灰字"连接中断"
- **AND** 显示"重试"按钮

### Requirement: 全局错误兜底

系统 SHALL 通过 `app.config.errorHandler` 和 `unhandledrejection` 监听器捕获未处理的异常，以 toast 形式提示用户。

#### Scenario: 渲染层抛出异常
- **WHEN** 某个 Vue 组件在渲染或生命周期中抛出异常
- **THEN** Vue 全局 errorHandler 捕获 → console.error 记录 → 屏幕右下角弹出 toast 显示错误概要

### Requirement: 单页应用的后端 fallback

系统 SHALL 让 Spring Boot 后端对所有非 `/api/<已知接口>` 路径的 GET 请求返回 `index.html`，使前端 Vue Router 的 history 模式深链刷新可用。

#### Scenario: 用户刷新非根路由
- **WHEN** 用户在浏览器地址栏访问 `http://localhost:10010/api/anything-not-known`
- **THEN** Spring Boot 返回 200 + `index.html` 内容
- **AND** 前端 Vue Router 接管路由，渲染对应页面或 404 视图（v1 仅 `/` 一条路由，其他路径走前端 not-found）

### Requirement: 客户端输入校验

系统 SHALL 在用户提交前阻止非法输入：空 / 纯空白 prompt 不允许发送，超长 prompt（>8000 字）不允许发送。

#### Scenario: 用户尝试发送空 prompt
- **WHEN** 输入框为空或只包含空白字符
- **THEN** 发送按钮处于 disabled 状态
- **AND** 按 Enter 不会触发任何请求

#### Scenario: 用户输入超过 8000 字
- **WHEN** 输入字符数超过 8000
- **THEN** 发送按钮 disabled
- **AND** 输入框附近显示字符计数告警

### Requirement: 视觉走 Claude 风

系统 SHALL 使用如下视觉规范：暖米底（基于 stone 灰阶 + `--background: 48 33% 97%`）+ 珊瑚赤陶橙 accent（`--accent: 16 65% 55%`）；窄列居中（`max-w-[720px]`）；正文 16-17px 字号、行高 1.7；AI 消息无气泡，User 消息整宽轻量卡片。

#### Scenario: 浅色主题下浏览
- **WHEN** 在浅色主题下加载页面
- **THEN** 整页背景为暖米色（接近 #faf9f5），不是纯白
- **AND** AI 消息以纯文本铺满 720px 居中列宽，左侧带 16×16 的 accent 色 Sparkles 图标
- **AND** User 消息以 `bg-muted` + `rounded-2xl` 卡片整宽展示，**不**右对齐胶囊气泡

### Requirement: 删除旧前端原型

系统 SHALL 在前端工程落地后，移除 `server/src/main/resources/front/` 目录下的旧 HTML 原型与配套图片资源。

#### Scenario: 落地后检查仓库
- **WHEN** 前端工程已 build 并产生 `server/src/main/resources/static/index.html` 等新资源
- **THEN** `server/src/main/resources/front/gpt.html`、`qwen.html`、`ai.png`、`user.png` 已被删除
- **AND** 仓库中没有任何代码引用上述路径
