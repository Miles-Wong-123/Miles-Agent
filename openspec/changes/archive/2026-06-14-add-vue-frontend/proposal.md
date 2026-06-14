## Why

后端 Agent 已经跑通，但当前的前端是 `server/src/main/resources/static/index.html` 那版手写 HTML——单文件、无构建、无类型、视觉糙。我们要让 Miles-Agent 长期演进成一个像样的产品，前端必须先工程化，否则做任何新页面（知识库、监控等）都会越来越别扭。同时，Claude.ai 那种"暖底克制"的设计走向能立刻把产品质感拉上一档。

## What Changes

- **新建 `web/` 子目录**，里面是完整的 Vue 3 + Vite + TypeScript 工程，技术栈：Tailwind v3 + shadcn-vue + Pinia + Vue Router 4 + VueUse + pnpm
- **聊天 UI 走 Claude 风**：暖米底 + 珊瑚赤陶橙 accent；AI 消息无气泡、User 消息整宽轻量卡片；窄列居中（720px）；空态带 3 条示例 prompt
- **流式输出 + 停止按钮 + 单消息重新生成 + Markdown/shiki 代码高亮 + 复制 + 设置面板 + 主题切换**
- **生产构建直接覆盖 `server/src/main/resources/static/`**：Vite `build.outDir` 指过去 + `emptyOutDir: true`，单 jar 打包部署不变
- **BREAKING（前端可见）**：旧的 `static/index.html`、`static/ai.png`、`static/user.png` 在前端落地后被覆盖；`server/src/main/resources/front/` 下两个旧 HTML 原型与 png 一并删除
- **后端配合改 1 处**：`AiChatController.streamChat` 的 `@PostMapping` 加 `produces = "text/plain;charset=UTF-8"`，让 `Flux<String>` 走纯文本流，前端无需协议解析
- **后端新增 1 处**：一个 fallback controller，把所有非 `/api/<已知接口>` 的 GET 请求返回 `index.html`，让 Vue Router 的 history 模式深链刷新可用
- **不做（v1 非目标）**：多会话历史侧边栏、后端会话历史 API、工具调用过程可视化、E2E 测试、CI 流水线

## Capabilities

### New Capabilities

- `web-chat-frontend`: Vue 工程化前端聊天体验，包括 UI 结构、状态管理、流式接收、错误处理、设置面板、主题切换；以及为支撑 SPA 所需的后端最小改动（streaming `produces` + history fallback）

### Modified Capabilities

（无——这是首次为该项目沉淀 spec，没有既有 capability 需要修改）

## Impact

**新增**

- `web/` 整套前端工程（package.json、vite/tailwind/ts 配置、源码、单元 + 组件测试）
- 后端 fallback controller（在 `server/src/main/java/com/miles/milesagent/controller/` 下新建）

**修改**

- `server/src/main/java/com/miles/milesagent/controller/AiChatController.java`：`streamChat` 方法加 `produces = "text/plain;charset=UTF-8"`
- `server/src/main/resources/static/`：构建产物落点，旧 `index.html` / `ai.png` / `user.png` 被 Vite 清空
- `README.md`：补 `web/` 工程说明与启动方式

**删除**

- `server/src/main/resources/front/gpt.html`
- `server/src/main/resources/front/qwen.html`
- `server/src/main/resources/front/ai.png`
- `server/src/main/resources/front/user.png`

**依赖与工具链**

- 引入 pnpm 作为前端包管理器（开发机需安装）
- 前端运行时依赖：Vue 3、Vue Router 4、Pinia + persistedstate、VueUse、TailwindCSS v3、shadcn-vue 复制进来的组件、lucide-vue-next、markdown-it、shiki、pinia-plugin-persistedstate
- 前端开发依赖：Vite 5、TypeScript、Vitest、@vue/test-utils、ESLint flat config、Prettier、vue-tsc
