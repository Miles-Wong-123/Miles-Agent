## 1. 后端配合改动

- [x] 1.1 给 `AiChatController.streamChat` 的 `@PostMapping` 加 `produces = "text/plain;charset=UTF-8"`
- [x] 1.2 新增 `SpaFallbackController`：`@GetMapping` 匹配所有非 `/api/<已知接口>` 路径，返回 `classpath:/static/index.html` 的内容（Content-Type: text/html）
- [x] 1.3 `cd server && ./mvnw -DskipTests compile` 通过
- [x] 1.4 `bash start-miles-agent.sh` 起后端，curl 验证 `/api/streamChat` 响应头 Content-Type 为 `text/plain;charset=UTF-8`
- [x] 1.5 curl 验证 `/api/anything-not-known` 返回 200 + index.html 内容
- [x] 1.6 把后端 `sessionId` / `userId` 类型从 `Long` 放宽到 `String`（实现期发现的设计缺口；前端用 uuidv4 字符串）：`ChatRequest`、`AiChat` 接口（`@MemoryId String sessionId`）、`MonitorContext`、`AiModelMonitorListener` 中相关用法；`./mvnw -DskipTests compile` 通过；curl 用字符串 sessionId 调 `/api/streamChat` 不再被 Jackson 拦截

## 2. 前端工程脚手架

- [x] 2.1 在仓库根执行 `pnpm create vite@latest web -- --template vue-ts`，初始化 `web/` 工程
- [x] 2.2 删掉 Vite 模板自带的示例代码（HelloWorld.vue、`<style>` 中的样板等），保留 `index.html`、`main.ts`、`App.vue` 骨架
- [x] 2.3 配置 `vite.config.ts`：dev server 5173、proxy `/api` → `http://localhost:10010`、`build.outDir: '../server/src/main/resources/static'`、`emptyOutDir: true`
- [x] 2.4 配置 `tsconfig.json` 为 strict 模式，路径别名 `@/* → src/*`
- [x] 2.5 安装并配置 TailwindCSS v3（`tailwind.config.ts` + postcss + `globals.css` 引入）
- [x] 2.6 安装 `shadcn-vue` CLI 并 `init`（生成 `components.json`、配置 `tailwind.config.ts` 中 shadcn 主题）
- [x] 2.7 配置 ESLint flat config + Prettier，添加 `pnpm lint` / `pnpm format` 脚本
- [x] 2.8 配置 Vitest + `@vue/test-utils` + happy-dom，`vitest.config.ts` 与 vite 共享 alias，添加 `pnpm test` 脚本
- [x] 2.9 添加 `pnpm typecheck` 脚本（`vue-tsc --noEmit`）
- [x] 2.10 验证：`pnpm dev` 能启 5173、`pnpm build` 能输出到 `server/src/main/resources/static/`、`pnpm test` 能跑空套件

## 3. 设计系统与基础组件

- [x] 3.1 在 `src/styles/globals.css` 写入 Claude 风 CSS 变量（浅色 + 深色两套），注入 `:root` / `.dark`
- [x] 3.2 配置 `tailwind.config.ts` 把 shadcn 默认主题颜色映射到上述 CSS 变量；设置字体栈、`max-w-[720px]` 等扩展
- [x] 3.3 用 `npx shadcn-vue@latest add` 引入：`button`、`textarea`、`sheet`、`switch`、`tooltip`、`sonner`（按需追加）
- [x] 3.4 安装 `lucide-vue-next`，在 `App.vue` 中验证图标渲染

## 4. 核心状态层与工具

- [x] 4.1 安装并装载 Pinia（含 `pinia-plugin-persistedstate`）到 `main.ts`
- [x] 4.2 安装 VueUse
- [x] 4.3 安装 Vue Router 4，创建 `router/index.ts`，注册 `/` → `ChatView`，history 模式
- [x] 4.4 实现 `stores/settings.ts`：`userId`、`sessionId`（首次自动 uuidv4）、`useStreaming`、`theme`，持久化到 localStorage（key `miles.settings`）
- [x] 4.5 实现 `stores/chat.ts`：state（`messages`、`isStreaming`、`abortController`）+ actions（`send`、`stop`、`regenerate`、`resetSession`）
- [x] 4.6 实现 `lib/api.ts`：`chat(payload, signal)` 同步、`streamChat(payload, signal, onChunk)` 流式（fetch + ReadableStream + TextDecoder）
- [x] 4.7 实现 `composables/useTheme.ts`：包 VueUse `useDark`，与 settings store 同步
- [x] 4.8 实现 `composables/useAutoScroll.ts`：流式中跟随底部，用户手动滚上去暂停跟随，提供"回到底部"标志位

## 5. 业务组件

- [x] 5.1 实现 `lib/markdown.ts`：`markdown-it` + `@shikijs/markdown-it`（VS Code 同款引擎），导出 `renderMarkdown(text: string): string`
- [x] 5.2 实现 `<MarkdownView>`：接受 `text` prop，渲染 markdown HTML（`v-html` + 容器内 Tailwind prose 样式）
- [x] 5.3 实现 `<CodeBlock>`：包装 shiki 渲染结果，右上角带"复制"按钮（写剪贴板 + 一次性反馈）
- [x] 5.4 实现 `<ChatMessage>`：根据 role 分支——user 整宽 `bg-muted` 卡片；ai 无气泡 + 左侧 Sparkles 图标 + 内嵌 `<MarkdownView>`；hover 时浮现 `[复制] [重新生成]`
- [x] 5.5 实现 `<ChatList>`：v-for 渲染 messages、空态分支、集成 `useAutoScroll`、回到底部浮动按钮
- [x] 5.6 实现 `<EmptyState>`：欢迎语 + 3 条示例 prompt（点击直接 send）
- [x] 5.7 实现 `<ChatInput>`：textarea auto-grow（最多 10 行）、Enter / Shift+Enter / Cmd+Enter 行为、字符上限 8000 校验、发送/停止双态按钮（accent 色小圆按钮）
- [x] 5.8 实现 `<AppHeader>`：标题 + ThemeToggle + 设置按钮（打开 SettingsSheet）
- [x] 5.9 实现 `<ThemeToggle>`：单按钮切换浅/深，集成 `useTheme`
- [x] 5.10 实现 `<SettingsSheet>`：从右侧滑入，含 userId 输入、sessionId 显示 + 重置、流式开关、主题三选一、"重置会话"按钮，全部双向绑定到 settings store
- [x] 5.11 实现 `views/ChatView.vue`：组装 AppHeader、ChatList、ChatInput、SettingsSheet
- [x] 5.12 在 `App.vue` 中放 `<RouterView />` 与全局 `<Sonner />` toast 容器
- [x] 5.13 在 `main.ts` 注册 `app.config.errorHandler` 和 `unhandledrejection` 监听器，错误转为 toast

## 6. 单元与组件测试

- [x] 6.1 `test/lib/api.test.ts`：mock fetch 验证 chat / streamChat 请求体拼装；用 `ReadableStream` mock 流式响应，验证 onChunk 调用次数与拼接结果
- [x] 6.2 `test/stores/chat.test.ts`：覆盖 send（成功 / 失败 / abort）、stop、regenerate（无前置 user 消息时的 no-op）、resetSession 状态变迁
- [x] 6.3 `test/lib/markdown.test.ts`：纯文本、代码块（含 java / ts / 未知语言）、表格、列表的渲染快照
- [x] 6.4 `test/components/ChatInput.test.ts`：Enter 发送、Shift+Enter 换行、空消息禁用、超长消息禁用 + 字符计数告警
- [x] 6.5 `test/components/ChatMessage.test.ts`：user 角色渲染卡片、ai 角色渲染 MarkdownView、`stoppedByUser` 显示灰字"已停止"、`error` 显示红字 + 重试按钮
- [x] 6.6 `pnpm test` 全部通过；`pnpm typecheck` 无错误；`pnpm lint` 无 error

## 7. 构建与部署联调

- [x] 7.1 `cd web && pnpm build` 输出到 `server/src/main/resources/static/`，`server/src/main/resources/static/index.html` 存在且引用了 hash 化的 JS/CSS 资源
- [x] 7.2 `cd server && ./mvnw -DskipTests package` 生成 jar；用 `unzip -l target/*.jar | grep static` 验证静态资源已嵌入
- [x] 7.3 `bash start-miles-agent.sh` 启动，浏览器访问 `http://localhost:10010/api/` 看到新 UI
- [x] 7.4 浏览器访问 `http://localhost:10010/api/some-deep-link`，确认拿到 index.html 而不是 404

## 8. 手动验证清单

- [x] 8.1 流式发一条消息，能逐字看到
- [x] 8.2 流式中按停止，已收到内容保留 + 灰字"已停止"
- [x] 8.3 后端关掉发一条，红字错误 + 重试按钮可用，重试在后端起来后能成功
- [x] 8.4 对最后一条 AI 消息点重新生成，原内容就地清空、新内容流入
- [x] 8.5 切深/浅主题，刷新后保持
- [x] 8.6 刷新页面后 sessionId 不变，发送新消息能延续上一轮上下文（Redis 记忆）
- [x] 8.7 设置面板"重置会话"，新 sessionId 生效，发送消息时模型不知道上一轮内容
- [x] 8.8 浏览器最大宽度（>1920）和最小宽度（375）下布局不崩
- [x] 8.9 长代码块（>120 列）和长表格能水平滚动而不撑破容器
- [x] 8.10 点击空态的示例 prompt 能直接发出请求
- [x] 8.11 单消息复制按钮工作正常（Markdown 转纯文本写入剪贴板）

## 9. 清理与文档

- [x] 9.1 `git rm` 删除 `server/src/main/resources/front/gpt.html`、`qwen.html`、`ai.png`、`user.png`
- [x] 9.2 全仓搜索确认没有代码引用上述被删路径
- [x] 9.3 更新 `README.md`：在"核心结构"章节补 `web/` 子树、在"快速启动"章节补"前端开发：`cd web && pnpm install && pnpm dev`"、在"访问入口"补对新前端的说明（旧的几条静态页链接删掉）
- [x] 9.4 在 `.gitignore` 中追加 `web/node_modules/`、`web/dist/`（如果有）、`web/.vite/`
- [x] 9.5 给本次落地写 commit message 草稿（`feat: add Vue 3 frontend in web/ with Claude-style chat UI`）；多处改动建议拆 2-3 个 commit（后端配合 / 前端工程 / 清理与文档）由用户决定
