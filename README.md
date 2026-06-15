# Miles-Agent

一个基于 `Spring Boot 3` + `LangChain4j` 的 AI Agent Demo，围绕“可对话、可记忆、可调用工具、可接入知识库、可流式输出”做了一次比较完整的后端实践。现在项目已经补上了邮箱注册登录、Redis Session 和业务接口鉴权，整体更接近一个可持续演进的 Agent 应用骨架。

## 功能一览

- 大模型对话：基于 DashScope / Qwen 模型
- 会话记忆：基于 Redis 的多轮上下文记忆
- 工具调用：本地 Java Tool + 远程 MCP Tool
- 知识写入：支持把问答内容沉淀到 Markdown 文档和 pgvector
- 流式响应：支持前端逐段接收模型输出
- 邮箱认证：支持验证码注册、邮箱密码登录、Redis Session 登录态
- 接口鉴权：聊天、流式聊天、知识写入等业务接口默认要求登录
- 基础监控：通过 Actuator + Micrometer + Prometheus 暴露模型调用指标

## 访问入口

仓库现在前后端并列，前端是仓库根的 `web/` 目录（Vue 3 + Vite + TypeScript）。生产构建产物会落到 `server/src/main/resources/static/`，由 Spring Boot 单进程一并提供。

- `http://localhost:10010/api/`：前端页面入口
- `http://localhost:10010/api/actuator/health`：健康检查
- `http://localhost:10010/api/actuator/prometheus`：Prometheus 指标

首次打开 `http://localhost:10010/api/` 时：

- 未登录：前端会自动跳到 `/login`
- 没有账号：可以进入 `/register`，完成“邮箱 -> 验证码 -> 昵称和密码”三步注册
- 已登录：直接进入聊天页

## 项目定位

从代码和 git 历史看，这更像一个“个人 Agent 实验项目 / Demo 项目”，适合用来验证下面这些事情：

- LangChain4j 在 Spring Boot 中如何组装 AI Service
- 如何接入模型、Redis 记忆、MCP 工具和本地工具
- 如何把知识写入本地文档与向量库
- 如何做流式聊天接口
- 如何给 Agent 应用补上最小可用的邮箱认证和会话体系
- 如何为模型请求补上基础监控

## 功能说明

### 1. 认证能力

- `POST /api/auth/sendCode`：发送注册验证码
- `POST /api/auth/verifyCode`：校验验证码
- `POST /api/auth/register`：注册并自动登录
- `POST /api/auth/login`：邮箱密码登录
- `POST /api/auth/logout`：退出登录
- `GET /api/auth/me`：获取当前登录用户

认证实现特点：

- 登录态通过 `HttpOnly Cookie + Redis Session` 维护
- 默认 30 分钟无操作过期
- 前端所有认证后请求统一带 `credentials: 'include'`
- 后端不再信任前端传入的 `userId`

### 2. 对话能力

- `POST /api/chat`：同步对话
- `POST /api/streamChat`：流式对话
- 会话记忆按 `sessionId` 隔离，最近 20 条消息存入 Redis
- 系统提示词位于 [`server/src/main/resources/system-prompt/chat-bot.txt`](./server/src/main/resources/system-prompt/chat-bot.txt)

注意：

- 这两个接口现在默认要求登录
- 请求体里只需要传 `sessionId` 和 `prompt`

### 3. 工具调用

当前 Agent 注册了以下工具：

- `TimeTool`：获取上海时区当前时间
- `RagTool`：把问答写入 Markdown 文档并同步到向量库
- `EmailTool`：发送简单文本邮件
- `MCP Tool Provider`：通过远程 MCP 接入搜索工具

对应代码位置：

- [`server/src/main/java/com/miles/milesagent/ai/AiChatService.java`](./server/src/main/java/com/miles/milesagent/ai/AiChatService.java)
- [`server/src/main/java/com/miles/milesagent/tool`](./server/src/main/java/com/miles/milesagent/tool)
- [`server/src/main/java/com/miles/milesagent/config/McpToolConfig.java`](./server/src/main/java/com/miles/milesagent/config/McpToolConfig.java)

### 4. 知识库写入

- `POST /api/insert`：手动插入知识
- `RagTool`：允许 Agent 在合适时自动保存知识
- 文本会同时写入本地 Markdown 文档和 pgvector
- 文档默认目录由 `rag.docs-path` 配置决定

默认知识示例可见：

- [`server/src/main/resources/docs/MilesAgent.md`](./server/src/main/resources/docs/MilesAgent.md)

### 5. 监控能力

项目给模型调用挂了 `ChatModelListener`，会记录：

- 请求开始 / 成功 / 失败次数
- 模型响应耗时
- Token 使用量
- 用户 / 会话 / 模型维度标签

相关代码：

- [`server/src/main/java/com/miles/milesagent/Monitor/AiModelMonitorListener.java`](./server/src/main/java/com/miles/milesagent/Monitor/AiModelMonitorListener.java)
- [`server/src/main/java/com/miles/milesagent/Monitor/AiModelMetricsCollector.java`](./server/src/main/java/com/miles/milesagent/Monitor/AiModelMetricsCollector.java)

## 当前实现状态

- 已接通：邮箱认证、同步聊天、流式聊天、Redis 会话记忆、本地工具、MCP 搜索工具、知识写入、Prometheus 指标
- 已配置但当前未真正参与聊天召回：RAG 检索器 `ContentRetriever`
- 原因：`AiChatService` 里 `.contentRetriever(contentRetriever)` 目前是注释状态
- 这意味着当前项目更偏“可保存知识的 Agent”，而不是“已完整启用检索增强问答的 Agent”

## 技术栈

- Java 17
- Spring Boot 3.5.9
- LangChain4j 1.1.x
- DashScope / Qwen
- Redis
- PostgreSQL + pgvector
- MyBatis
- Spring Session Data Redis
- Spring Mail
- Reactor
- Spring Boot Actuator
- Micrometer + Prometheus
- Vue 3 + Vite + TypeScript + Pinia

## 核心结构

```text
server/src/main/java/com/miles/milesagent
├── ai/                  # Agent 接口定义与装配
├── auth/                # 用户模型、DTO、Mapper、认证服务
├── config/              # 模型、RAG、Redis、MCP、鉴权等配置
├── controller/          # 对外 HTTP 接口（含 SPA fallback）
├── guardrail/           # 输入防护
├── Monitor/             # 模型调用监控
├── model/dto/           # 聊天请求 DTO
└── tool/                # Agent 可调用工具

server/src/main/resources
├── docs/                # 本地知识文档
├── mapper/              # MyBatis XML
├── static/              # 前端构建产物（build 后自动覆盖）
└── system-prompt/       # 系统提示词

web/
├── src/
│   ├── components/      # 业务组件 + UI 套件
│   ├── lib/             # api 客户端、类型、markdown 渲染
│   ├── router/          # Vue Router 4，history 模式
│   ├── stores/          # Pinia: auth / chat / settings
│   └── views/           # Chat / Login / Register
└── test/                # Vitest 测试
```

## 接口示例

### 1. 注册与登录

发送验证码：

```bash
curl -i -X POST http://127.0.0.1:10010/api/auth/sendCode \
  -H "Content-Type: application/json" \
  -d '{"email":"your-email@example.com"}'
```

注册：

```bash
curl -i -c /tmp/miles-agent.cookie -b /tmp/miles-agent.cookie \
  -X POST http://127.0.0.1:10010/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "code": "123456",
    "nickname": "Miles User",
    "password": "Pass12345"
  }'
```

已有账号直接登录：

```bash
curl -i -c /tmp/miles-agent.cookie -b /tmp/miles-agent.cookie \
  -X POST http://127.0.0.1:10010/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "password": "Pass12345"
  }'
```

### 2. 同步聊天

```bash
curl -i -b /tmp/miles-agent.cookie \
  -X POST http://127.0.0.1:10010/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-1001",
    "prompt": "你好，介绍一下你自己"
  }'
```

### 3. 流式聊天

```bash
curl -N -b /tmp/miles-agent.cookie \
  -X POST http://127.0.0.1:10010/api/streamChat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-1002",
    "prompt": "请用一句话介绍这个项目"
  }'
```

### 4. 插入知识

```bash
curl -i -b /tmp/miles-agent.cookie \
  -X POST http://127.0.0.1:10010/api/insert \
  -H "Content-Type: application/json" \
  -d '{
    "question": "这个项目是做什么的？",
    "answer": "这是一个基于 Spring Boot 和 LangChain4j 的 AI Agent Demo。",
    "sourceName": "MilesAgent.md"
  }'
```

## 快速启动

### 1. 准备依赖

你至少需要准备：

- JDK 17
- Node.js
- pnpm
- Docker

还需要准备：

- Redis
- PostgreSQL + pgvector 扩展
- DashScope API Key
- BigModel API Key
- 发件邮箱 SMTP 配置

### 2. 准备本地配置

复制示例配置：

```bash
cp server/src/main/resources/application-local.example.yml server/src/main/resources/application-local.yml
```

然后补充你自己的：

- Redis 连接
- pgvector 连接
- DashScope API Key
- BigModel API Key
- 邮件账号与授权码

相关配置文件：

- [`server/src/main/resources/application.yml`](./server/src/main/resources/application.yml)
- [`server/src/main/resources/application-dev.yml`](./server/src/main/resources/application-dev.yml)
- [`server/src/main/resources/application-prod.yml`](./server/src/main/resources/application-prod.yml)
- [`server/src/main/resources/application-local.example.yml`](./server/src/main/resources/application-local.example.yml)

### 3. 启动项目

推荐直接在仓库根目录执行：

```bash
./start-miles-agent.sh
```

这个脚本会：

- 启动 `miles-agent-pgvector`
- 启动 `miles-agent-redis`
- 等待 `5432` 和 `6380` 端口就绪
- 自动构建 `web/` 前端静态资源
- 进入 `server/` 后执行 `./mvnw spring-boot:run`

如果本地还没有安装前端依赖，脚本会直接提示你先执行：

```bash
cd web && pnpm install
```

停止服务：

```bash
./stop-miles-agent.sh
```

如果你只想单独启动后端：

```bash
cd server
./mvnw spring-boot:run
```

前端开发模式：

```bash
cd web
pnpm install
pnpm dev
pnpm test
pnpm typecheck
pnpm lint
pnpm build
```

部署仍然是单 jar：先 `cd web && pnpm build`，再 `cd server && ./mvnw -DskipTests package`，前端资源会自动嵌入到 `target/miles-agent-*.jar`。

## 首次使用流程

1. 启动项目后，打开 `http://127.0.0.1:10010/api/`
2. 没有账号就进入注册页
3. 输入邮箱，接收验证码
4. 校验验证码后填写昵称和密码
5. 注册成功后自动登录并进入聊天页
6. 后续接口调试如果走 `curl`，记得带上登录 cookie

## 常见排查

### 1. 服务有没有起来

健康检查：

```bash
curl http://127.0.0.1:10010/api/actuator/health
```

正常会返回包含 `"status":"UP"` 的 JSON。

### 2. 容器是不是在线

```bash
docker ps
```

正常应该能看到：

- `miles-agent-pgvector`
- `miles-agent-redis`

### 3. 为什么 `curl /api/chat` 返回 401

这是正常现象。现在聊天、流式聊天、知识写入都要求登录。

解决方式：

- 先在浏览器里登录
- 或先调用 `/api/auth/login`，把 cookie 保存到本地文件后，再带 cookie 调业务接口

### 4. 为什么 `start-miles-agent.sh` 一直不返回

这也是正常现象。

原因：

- 脚本最后以前台方式运行 `./mvnw spring-boot:run`
- 当前终端会持续输出后端日志

处理方式：

- 保持这个窗口不动
- 另开一个新终端做 `curl` 测试或前端调试
