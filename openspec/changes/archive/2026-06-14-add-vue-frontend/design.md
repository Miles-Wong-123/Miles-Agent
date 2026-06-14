## Context

Miles-Agent 后端是 Spring Boot 3 + LangChain4j 的 AI Agent，已经具备同步对话、流式对话、Redis 记忆、本地工具与 MCP 工具调用、知识写入、Prometheus 监控等能力。当前唯一的"前端"是 `server/src/main/resources/static/index.html` 一份手写 HTML，无构建、无类型、无组件化。这次要在仓库根的 `web/` 目录下建立一个工程化的 Vue 前端，长期承载所有用户可见交互。

仓库已经在上一轮完成了"前后端并列"的目录重构（后端在 `server/`，前端将进入 `web/`），并把开发者环境约束记录在 `docs/superpowers/specs/2026-06-14-vue-frontend-design.md`（本文是它的 OpenSpec 落地版）。该长文档对每个决策有详尽展开；本设计文档只保留**有歧义、需要解释、或有备选**的决定。

## Goals / Non-Goals

**Goals**

- 一个能让 v2 知识库页、监控页直接挂上来的可扩展前端架子（路由 + 状态库 + 测试都到位）
- Claude.ai 风格的视觉质感：暖米底 + 珊瑚赤陶橙 + 窄列 + 大量留白
- 流式对话的端到端可用：默认走流式、可中断、可重新生成
- 单进程部署模型不变：`mvn package` 后单 jar 包含前端产物，部署仍然只暴露 10010

**Non-Goals**

- 多会话历史侧边栏（v1 单会话）
- 后端会话历史 API（v1 不查 Redis，前端只感知"当前会话"）
- 工具调用过程的可视化卡片（依赖后端先输出 tool-event，本轮不动后端 streaming 协议结构）
- E2E、视觉回归、CI 流水线（手动验证清单 + 单元/组件测试覆盖 v1）

## Decisions

### D1：单一 capability `web-chat-frontend`，不拆 streaming-protocol / spa-fallback

**选择**：把 Vue 前端、后端 streaming `produces` 改动、SPA fallback controller 都纳入同一个 capability。

**理由**：后端两处改动（streaming 协议 + history fallback）单独存在没有意义——它们是为了让前端能跑而生的支撑。把它们独立成 capability 反而会割裂"用户拿到一个能跑的前端"这个目标。未来若有第二个前端（比如 mobile 客户端）复用同一份 streaming 协议，再把它独立成 `chat-streaming-protocol` capability。

**备选**：拆三个 capability（`web-chat-frontend` / `chat-streaming-protocol` / `spa-routing-fallback`）。被拒的原因如上。

### D2：流式协议用 `text/plain;charset=UTF-8`，不引入 NDJSON 或 SSE

**选择**：后端给 `streamChat` 加 `produces = "text/plain;charset=UTF-8"`，前端 fetch + `ReadableStream` + `TextDecoder`，**零解析**。

**理由**：

- v1 只需要传"模型 token 块"这一种数据，纯文本流是最贴合 `Flux<String>` 语义的协议
- 前端实现极短，不需要状态机或 SSE / NDJSON 解析器，bug 面小
- 后端改动只有一行（注解参数）

**备选 1：NDJSON**（每行一个 JSON 对象）。优势是未来要传"工具调用事件"时可平滑扩展。被拒的原因：v1 不需要结构化事件，引入 JSON 解析层增加复杂度且不会立刻产生收益。等 v2 做工具事件可视化时再切换协议（也是 v2 的明确演进路径）。

**备选 2：SSE**（`text/event-stream` + `data:` 前缀）。优势是有标准浏览器 API（EventSource）。被拒的原因：EventSource 不支持 POST 请求，必须用 fetch + 手写解析器，相比纯文本流多了协议解析开销且没有任何收益。

### D3：Pinia + Vue Router + VueUse 全装上，不只用 Composition API

**选择**：v1 单会话单页，但仍引入 Pinia（含 persistedstate）、Vue Router 4、VueUse。

**理由**：用户明确表态"架子搭全，以后扩展不折腾"。这三个依赖都是 Vue 生态标准组件，加进来的代价很小（包大小几 kB），换来的是：

- Pinia：DevTools 直接看到 chat / settings 状态，HMR 友好；将来 store 扩到 5+ 时不需要重构
- Vue Router：v2 加 `/knowledge`、`/monitoring` 时只是 push 一条路由
- VueUse：`useDark`、`useLocalStorage`、`useScroll` 等都是反复要写的逻辑

**备选**：纯 composables。被拒的原因：迁移成本随项目规模增加非线性增长，选用社区标准件可避免"DIY 抽象"陷阱。

### D4：HTML5 history 模式 + 后端 fallback，不退化到 hash 模式

**选择**：Vue Router 用 history 模式，后端新增 fallback controller 把所有非 `/api/<已知>` GET 返回 `index.html`。

**理由**：URL 干净（`/settings` 而不是 `/api/#/settings`），符合"长期产品"定位；后端 fallback 只是一个一次性 controller，写一次受用所有未来路由。

**备选**：hash 模式。被拒的原因：URL 丑且不可分享。

### D5：构建产物直接覆盖 `server/src/main/resources/static/`，不放在 `web/dist/` 再手动拷贝

**选择**：`vite.config.ts` 里 `build.outDir: '../server/src/main/resources/static'` + `emptyOutDir: true`。

**理由**：单一真实来源——前端构建即等于后端静态资源更新。少一道拷贝步骤，少一处可能不同步的地方。`mvn package` 之前先 `pnpm build` 即可。

**风险**：旧的 `static/index.html`、`static/ai.png`、`static/user.png` 会被清空。这是预期行为（旧前端原型本就被替代），会通过 git 删除记录在案。

**备选**：两步构建（`web/dist` → 拷贝到 `static/`）。被拒的原因：多一步、多一个潜在的"忘记拷贝"故障点。

### D6：UI 走 Claude.ai 风（暖米底 + 珊瑚赤陶橙），不抄 ChatGPT

**选择**：shadcn-vue `stone` 灰阶 + 自定义 `--accent: 16 65% 55%`；AI 消息无气泡，User 消息整宽轻量卡片；窄列 720px 居中。

**理由**：用户明确表达偏好。Claude 风格也更适合"读起来舒服"的 Agent 输出（长段 markdown + 代码块）。

**视觉规范的执行细节**完整落在 `docs/superpowers/specs/2026-06-14-vue-frontend-design.md` 第 4 节，本设计不重复。

### D7：v1 不引入 E2E / 视觉回归测试

**选择**：测试覆盖 = Vitest 单元 + 组件测试 + `vue-tsc` 类型 + ESLint。E2E 留给 v2。

**理由**：E2E 跑通需要真后端 + DashScope key + Redis/pgvector，CI 成本高、稳定性差；v1 的设计稳定性也不够，视觉回归此时引入会产生大量噪声。手动验证清单（spec 中"手动验证清单"）能覆盖 v1。

## Risks / Trade-offs

- **重新生成会让 Redis 记忆"脏"一轮** → v2 加 `DELETE /api/memory/last` 接口清理；v1 接受
- **`emptyOutDir: true` 会删除 `static/` 现存文件** → 通过 git 历史可追溯；删除前在 commit message 中说明
- **shiki 是异步 highlighter** → 首次代码块渲染有几十 ms 延迟 → 用 `<CodeBlock>` 组件加 skeleton 占位
- **fetch 在 Safari < 14.1 不支持 ReadableStream** → 不在支持范围（用户开发用 macOS 现代 Chrome/Safari，不为旧版本兼容）
- **shell 代理（`ALL_PROXY`）会拦截 curl localhost** → 只影响开发期 curl，不影响浏览器；开发文档中提示用 `--noproxy '*'`

## Migration Plan

部署是单进程 jar，不存在数据迁移。**前端首次落地的步骤**：

1. 后端先打补丁（`streamChat` 加 `produces`，新增 fallback controller），单测通过、`./mvnw compile` 通过
2. `web/` 工程脚手架完整落地、`pnpm build` 能产出到 `server/src/main/resources/static/`
3. `mvn spring-boot:run` 起后端，浏览器访问 `http://localhost:10010/api/` 验证手动验证清单 9 项
4. 通过后删除 `server/src/main/resources/front/` 的旧 HTML 原型与 png
5. 更新 `README.md`，提交单一 commit

**回滚**：

- 前端构建出问题：`git revert` 整个 commit，`static/` 恢复到旧 `index.html` 原型
- 后端 streaming 改动出问题：单独 revert `AiChatController` 那一行 `produces` 即可（前端会拿到 JSON 整段而非流式，明显的退化但不影响其他功能）

## Open Questions

- **`useStreaming` 默认值是否暴露给用户**？v1 设计是默认 true 且设置面板可切。如果切到同步会失去流式体验；考虑过把这个开关藏起来，最终决定**保留**——它对调试后端非常有用，且对用户透明（默认就是流式）。
- **是否需要在 `start-miles-agent.sh` 中集成 `pnpm dev` 启动**？v1 不集成——前后端开发节奏不一定同步，强绑会限制灵活性。开发期手动 `pnpm dev` 与 `bash start-miles-agent.sh` 分开跑。
