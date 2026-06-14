# Vue 前端工程设计文档

- 状态：设计已确认，待落地
- 日期：2026-06-14
- 范围：为 Miles-Agent 后端补一个现代化的 Vue 前端，目标是"看起来好、写起来不别扭、扩展不折腾"
- 后续动作：用 OpenSpec 工作流（`/opsx:propose`）把本文转成可执行的 change

## 1. 目标与非目标

**目标**

- 替代 `server/src/main/resources/static/index.html` 这版手写 HTML 原型，提供一个工程化的现代前端
- 视觉走 Claude.ai 那种"暖底克制"风格，区别于 ChatGPT 默认那种产品感
- 架子搭全：Pinia + Vue Router + VueUse + Tailwind + shadcn-vue + TypeScript，后续加新页面（知识库、监控等）只增不改
- 流式输出 + 停止 + 重新生成 + Markdown / 代码块 + 主题切换，跟主流 LLM 聊天产品对齐

**非目标（v1 不做）**

- 多会话历史侧边栏（已确认"单会话"档位）
- 后端会话历史 API（前端只用 Redis 记忆里的当前会话）
- 工具调用过程的可视化卡片（后端目前没有 tool-event 流，这事要等后端先动）
- E2E / 视觉回归测试
- CI 流水线

## 2. 工程位置与构建集成

### 目录布局

```
Miles-Agent/
├── server/                                    # Spring Boot 后端（已迁移）
│   └── src/main/resources/static/             # ← 前端构建产物落点
├── web/                                       # 新建：Vue 前端工程
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   ├── src/
│   ├── public/
│   └── test/
└── docs/superpowers/specs/                    # 本文所在
```

### 开发期工作流

- `cd web && pnpm dev` 启 Vite，监听 5173
- `vite.config.ts` 配 proxy：`/api/*` → `http://localhost:10010`
- 浏览器访问 `http://localhost:5173`，所有 `/api/*` 请求透明转发到后端
- 后端按现有方式启动（VS Code F5 或 `bash start-miles-agent.sh`）

### 生产构建

- `cd web && pnpm build`，Vite 把产物输出到 `../server/src/main/resources/static/`（在 `vite.config.ts` 里通过 `build.outDir` 直接指过去，`emptyOutDir: true`）
- 这意味着每次 build 会清空 static 目录里所有内容，包括旧的 `ai.png` / `user.png` / `index.html`。设计上不再使用这些文件，保留它们是负担。`server/src/main/resources/front/` 下两个旧 HTML 原型（`gpt.html` / `qwen.html`）建议在前端落地后一并删除。
- Spring Boot `mvn package` 时静态资源已就位，jar 一并打包，部署仍然是单进程 10010

### 路由与后端 fallback

- 前端走 HTML5 history 模式（URL 干净，便于扩展）
- v1 只有 `/` 一条路由跑聊天页，但深链刷新（如 `/settings`）需要后端兜底
- 后端在 `AiChatController` 之外**新增一个 fallback controller**：所有非 `/api/<已知接口>` 的 GET 都返回 `index.html`，让 Vue Router 接管前端路由
- 这是本设计**唯一需要动后端的地方**（除了下文流式协议改动）

## 3. 技术栈

| 类别 | 选择 | 备注 |
| --- | --- | --- |
| 框架 | Vue 3 + `<script setup>` + Composition API | 现代默认 |
| 构建 | Vite 5 | 与 shadcn-vue 生态对齐 |
| 语言 | TypeScript（strict） | |
| 样式 | TailwindCSS v3 | shadcn-vue 依赖 |
| 组件库 | shadcn-vue | 提供高质量原子组件，复制进项目可改 |
| 图标 | lucide-vue-next | shadcn-vue 默认搭档 |
| 状态 | Pinia | 持久化用 `pinia-plugin-persistedstate` |
| 路由 | Vue Router 4（history 模式） | |
| 工具集 | VueUse | `useDark`、`useLocalStorage`、`useScroll` 等按需引入 |
| HTTP | 原生 `fetch` + `ReadableStream` | 不引 axios |
| Markdown | `markdown-it` | |
| 代码高亮 | `shiki` + `@shikijs/markdown-it` | VS Code 同款引擎 |
| 包管理 | pnpm | |
| Lint / 格式 | ESLint flat config + Prettier | |
| 单元测试 | Vitest + `@vue/test-utils` | |

**有意识的减法**

- 不引 axios：`fetch` + ReadableStream 已能覆盖同步与流式两个场景
- 不引 Google Fonts：避免代理 / 网络问题，用系统字体栈
- 不引 E2E 框架：手动验证清单覆盖 v1

## 4. 视觉设计指引

### 整体走向

Claude.ai 风格——暖色基调、极少分割线、大量留白、单一窄列。区别于 ChatGPT 的更"产品化"气质，目标是"读起来舒服"。

### 调色板（写入 `globals.css` 的 CSS 变量）

shadcn-vue `stone` 灰阶为基础，accent 用珊瑚赤陶橙（不照搬 Claude 色号、抄气质）。

```css
:root {
  --background: 48 33% 97%;     /* 暖米底，约 #faf9f5 */
  --foreground: 30 8% 15%;      /* 深暖灰文字，避免纯黑 */
  --muted: 40 20% 92%;          /* 用户消息卡片背景 */
  --muted-foreground: 30 6% 40%;
  --border: 40 15% 88%;
  --accent: 16 65% 55%;         /* 珊瑚赤陶橙 */
  --accent-foreground: 0 0% 100%;
  --radius: 0.75rem;
}

.dark {
  --background: 30 8% 12%;
  --foreground: 40 15% 92%;
  --muted: 30 6% 18%;
  --muted-foreground: 40 10% 65%;
  --border: 30 6% 22%;
  --accent: 16 60% 60%;
}
```

### 排版与节奏

- 字体栈：`-apple-system, ui-sans-serif, system-ui, "PingFang SC", "Microsoft YaHei", sans-serif`
- 等宽栈：`ui-monospace, "JetBrains Mono", Menlo, Consolas, monospace`
- 正文字号 16-17px、行高 1.7
- 内容列宽 `max-w-[720px]`，水平居中
- 消息上下 `py-8`，让段落"呼吸"

### 消息样式

- **AI 消息**：无气泡。左侧小 avatar（lucide `<Sparkles>`，accent 色，16×16），文字直接铺满列宽。代码块、表格、列表占整列宽，比塞气泡里好看
- **User 消息**：整宽轻量卡片（`bg-muted` + `rounded-2xl` + `px-5 py-4`），左对齐和 AI 平起平坐，**不**用右对齐胶囊气泡。这是 Claude 风格的关键决定之一
- 没有用户头像图标
- 鼠标悬停消息时浮现 `[复制]` `[重新生成]`，用 `text-muted-foreground` + `opacity-0 group-hover:opacity-100`

### 输入框

- 圆角矩形卡片，悬浮在底部（外圈 `pb-6`），`rounded-2xl` + `shadow-sm` + 细边框
- 内置 placeholder "Reply to Miles..." 之类的文案
- 右下角发送按钮：accent 色填充小圆按钮
- 流式中按钮变 `<Square>` 图标的停止按钮，颜色保持 accent
- textarea 自适应高度，最多 10 行后内部滚动

### 空态（首次进入页面）

居中显示欢迎语 + 三条点击即发送的示例 prompt：

1. *介绍一下 LangChain4j 的核心概念*（演示 RAG + 普通聊天）
2. *现在几点？*（演示 TimeTool）
3. *搜索一下 Spring Boot 3.5 的新特性*（演示 MCP 搜索工具）

### 键盘交互

- `Enter` 发送，`Shift+Enter` 换行
- `Cmd/Ctrl+Enter` 也发送（兼容习惯）
- `Cmd/Ctrl+K` 打开设置面板，`Esc` 关闭
- 流式过程中，用户手动滚上去时**暂停跟随**，回到底部按钮浮出（参考 Claude 实现）

## 5. 组件结构

```
App.vue
└─ <RouterView />
   └─ ChatView.vue  ← / 路由
      ├─ <AppHeader>
      │     ├─ 标题
      │     ├─ <ThemeToggle>
      │     └─ <Button @click="openSettings"> ⚙ </Button>
      ├─ <ChatList>                          # 监听 chatStore.messages
      │     ├─ <EmptyState v-if="messages.length === 0">
      │     ├─ v-for <ChatMessage>
      │     │     ├─ role="user"  → 卡片渲染
      │     │     └─ role="ai"    → <MarkdownView>（内含若干 <CodeBlock>）
      │     │           hover 时浮现 [复制] [重新生成]
      │     └─ <ScrollToBottomButton v-if="!followingTail">
      └─ <ChatInput>
            ├─ <Textarea>（auto-grow，最多 10 行）
            └─ <Button>（发送 / 停止 双态）

      <SettingsSheet v-model:open="...">     # 从右侧滑入
            ├─ userId 输入
            ├─ sessionId 显示 + "重置" 按钮
            ├─ 接口选择（Switch：流式 / 同步）
            ├─ 主题（Switch：浅 / 深 / 跟随系统）
            └─ "重置会话" 按钮（清 messages、换新 sessionId）
```

### 目录结构

```
web/src/
├── main.ts                  # 装 Pinia、Router、persistedstate 插件
├── App.vue                  # <RouterView />
├── router/
│   └── index.ts             # 路由表，v1 只一条 /
├── stores/
│   ├── chat.ts              # useChatStore
│   └── settings.ts          # useSettingsStore（持久化）
├── views/
│   └── ChatView.vue
├── components/
│   ├── ui/                  # shadcn-vue 复制进来的原子组件
│   └── chat/                # 业务组件（详见上方组件树）
├── composables/
│   ├── useStreaming.ts      # fetch + ReadableStream → store
│   ├── useTheme.ts          # 包 VueUse useDark + 持久化
│   └── useAutoScroll.ts     # 自动滚动 + 暂停跟随
├── lib/
│   ├── api.ts               # chat / streamChat 两个函数
│   └── markdown.ts          # markdown-it + shiki 装配
└── styles/
    └── globals.css
```

## 6. 数据流与状态结构

### Store 形态

```ts
// stores/chat.ts
interface Message {
  id: string                  // uuid
  role: 'user' | 'ai'
  content: string
  streaming?: boolean
  error?: string              // 请求失败时设
  stoppedByUser?: boolean
}

interface ChatState {
  messages: Message[]
  isStreaming: boolean
  abortController: AbortController | null
}

// actions
send(prompt: string): Promise<void>
stop(): void
regenerate(aiMessageId: string): Promise<void>
resetSession(): void           // 清 messages、换新 sessionId
```

```ts
// stores/settings.ts（持久化到 localStorage，key: miles.settings）
interface SettingsState {
  userId: number               // 默认 2001
  sessionId: string            // 首次加载自动 uuidv4
  useStreaming: boolean        // 默认 true
  theme: 'light' | 'dark' | 'system'  // 默认 system
}
```

### 发送一条消息的时序

```
用户在 <ChatInput> 按 Enter
   │
   ▼
chatStore.send(prompt)
   ├─ 推一条 user message 进 messages
   ├─ 推一条空的 ai message 占位（streaming: true）
   ├─ new AbortController()，存到 store
   ├─ isStreaming = true
   │
   ├─ if (settings.useStreaming):
   │     api.streamChat({ sessionId, userId, prompt }, signal, onChunk)
   │     onChunk(delta) → ai.content += delta（响应式，UI 自动滚动）
   │
   └─ else:
         api.chat(...) → ai.content = 整段返回
   ▼
finally: isStreaming = false, abortController = null, ai.streaming = false
```

### 重新生成

`chatStore.regenerate(aiMessageId)` → 找到这条 AI 消息前面那条 user message → 把 AI 这条**就地清空** content → 复用 send 流程但**不**追加新 user 消息。

⚠️ **已知限制**：后端 Redis 记忆里会同时留下旧的失败回答 + 新的回答两轮，因为目前没有"删除最后一轮"的接口。v1 接受这个限制。v2 加 `DELETE /api/memory/last` 接口后再清。

## 7. 流式协议

### 后端配合改动

给 `AiChatController.streamChat` 的 `@PostMapping` 加 `produces = "text/plain;charset=UTF-8"`：

```java
@PostMapping(value = "/streamChat", produces = "text/plain;charset=UTF-8")
public Flux<String> streamChat(@RequestBody ChatRequest chatRequest) { ... }
```

效果：Spring 把 `Flux<String>` 直接序列化为连续字节流，每个 String 是一个模型 token 块。Content-Type 明确为 `text/plain`，前端不需要任何解析。

### 前端实现

```ts
// lib/api.ts
async function streamChat(
  payload: ChatRequest,
  signal: AbortSignal,
  onChunk: (delta: string) => void
) {
  const res = await fetch('/api/streamChat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal,
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  if (!res.body) throw new Error('no body')

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    onChunk(decoder.decode(value, { stream: true }))
  }
}
```

### 同步接口

`api.chat(payload, signal)` 直接 `await fetch(...).then(r => r.text())`，没有特殊处理。

## 8. 错误处理

| 场景 | 表现 | 处理 |
| --- | --- | --- |
| 后端不可达（fetch TypeError、502、连接被拒） | AI 占位消息红字提示 + 重试按钮 | `ai.error = "无法连接后端"`，保留消息允许重试 |
| 后端 4xx / 5xx | 解析 body 错误文案 | `ai.error = body`，UI 同上 |
| 流式中断（fetch 中途断流） | 已收到内容**保留**，灰字"连接中断" | catch 后 `ai.streaming = false`，加 `ai.error = "stream interrupted"` |
| 用户主动停止 | 已收到内容保留，灰字"已停止" | 不进 error 流，标 `ai.stoppedByUser = true` |
| 空 / 纯空白 prompt | 发送按钮 disabled | 在 `<ChatInput>` 层拦截，不进 store |
| prompt 超长（暂定 8000 字） | 发送按钮 disabled + 字符计数告警 | 同上 |

### 全局兜底

- `app.config.errorHandler` 接 Vue 渲染层异常 → console.error + Sonner toast
- `window.addEventListener('unhandledrejection')` 兜底未捕获 promise → toast

### Toast vs 内联错误

- 流相关错误（消息发不出去、流断了）→ 内联在消息上
- 全局/系统错误（设置保存失败、未捕获异常）→ Toast

### 重试策略

v1 **不做自动重试**。流断了 → 显示重试按钮 → 用户点。理由：自动判断"什么错值得重试"复杂，且要避免重复发出用户没说完的 prompt。

## 9. 测试策略

### 测什么

| 类型 | 工具 | 范围 |
| --- | --- | --- |
| 单元 | Vitest | `lib/api.ts` 的请求拼装与流式解析；`stores/chat.ts` 的 send / stop / regenerate 状态变迁；`lib/markdown.ts` 关键 case |
| 组件 | Vitest + `@vue/test-utils` | `<ChatInput>` 键盘行为（Enter 发送、Shift+Enter 换行、空消息禁用）；`<ChatMessage>` 角色分支渲染 |
| 类型 | `vue-tsc --noEmit` | TS strict |
| Lint | ESLint flat + Prettier | `pnpm lint` |

### 不测

- E2E（依赖真后端 + DashScope key + Redis/pgvector，成本太高）
- 视觉回归（设计未稳定）
- 后端（不在本 spec 范围）

### 手动验证清单（v1 上线前过一遍）

1. 流式发一条消息，能逐字看到
2. 流式中按停止，已收到内容保留 + 灰字"已停止"
3. 后端关掉发一条，红字错误 + 重试按钮可用
4. 重新生成：AI 消息就地刷新
5. 切深/浅主题，刷新后保持
6. 刷新页面后 sessionId 不变（Redis 记忆延续）
7. 设置面板里"重置会话"，新 sessionId 生效，记忆清零
8. 浏览器最大/最小宽度下布局不崩
9. 长代码块 + 长表格能水平滚动而不撑破容器

## 10. 已知限制与未来工作

| # | 限制 | v2 怎么办 |
| --- | --- | --- |
| 1 | 重新生成会让 Redis 记忆"脏"一轮 | 后端加 `DELETE /api/memory/last` |
| 2 | 没有多会话历史 | 后端加 `GET /api/sessions` + `GET /api/sessions/:id/messages`，前端加侧边栏 |
| 3 | 工具调用过程不可视 | 后端给 streamChat 增加 NDJSON 输出（同时支持 `text/plain` 和 `application/x-ndjson`），前端按 Accept 头切换解析 |
| 4 | 没有 CI | 加 GitHub Actions（`pnpm test` + `pnpm build` + `pnpm lint`） |
| 5 | 没有 E2E 覆盖 | 引 Playwright，用真后端 + 测试桩 DashScope |

## 11. 影响清单

**新增**

- `web/` 前端工程整套（package.json、Vite/Tailwind/TS 配置、源码、测试）
- 后端一个 fallback controller（处理非 `/api/<已知>` GET → index.html）

**修改**

- `server/src/main/java/com/miles/milesagent/controller/AiChatController.java` 的 `streamChat`：加 `produces = "text/plain;charset=UTF-8"`
- `server/src/main/resources/static/`：构建时被 Vite 覆盖，旧 `index.html` / `ai.png` / `user.png` 全部消失
- `README.md`：补 `web/` 工程说明 + 启动方式
- `start-miles-agent.sh`：可选——v1 暂不集成前端 dev server 启动，开发期手动 `pnpm dev`

**删除**

- `server/src/main/resources/front/gpt.html`
- `server/src/main/resources/front/qwen.html`
- `server/src/main/resources/front/ai.png`
- `server/src/main/resources/front/user.png`

## 12. 落地路径

本设计**不在这一轮直接执行**。下一步走 OpenSpec 工作流：

1. 用 `/opsx:propose` 把本文转成具体的 change（`openspec/changes/<change-id>/`）
2. OpenSpec 会生成 proposal、tasks、design 三件套
3. 用 `/opsx:apply` 按 tasks 实施
4. 完成后 `/opsx:archive` 归档

实施过程中如果发现设计需要调整，回来更新本文档，再同步到 OpenSpec change。


