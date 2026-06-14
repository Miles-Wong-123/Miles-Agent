# Miles-Agent

一个基于 `Spring Boot 3` + `LangChain4j` 的 AI Agent Demo，围绕“可对话、可记忆、可调用工具、可接入知识库、可流式输出”做了一次比较完整的后端实践。

这个项目不是纯聊天接口的拼装，而是把下面几类 Agent 能力串了起来：

- 大模型对话：基于 DashScope / Qwen 模型
- 会话记忆：基于 Redis 的多轮上下文记忆
- 工具调用：本地 Java Tool + 远程 MCP Tool
- 知识写入：支持把问答内容沉淀到 Markdown 文档和 pgvector
- 流式响应：支持前端逐段接收模型输出
- 基础监控：通过 Actuator + Micrometer + Prometheus 暴露模型调用指标

## 访问入口

项目里已经带了一个静态聊天页，启动后可直接访问：

- `http://localhost:10010/api/`

当前仓库还保留了几版前端原型：

- [`server/src/main/resources/static/index.html`](./server/src/main/resources/static/index.html)：当前默认聊天页
- [`server/src/main/resources/front/gpt.html`](./server/src/main/resources/front/gpt.html)：较早版本原型
- [`server/src/main/resources/front/qwen.html`](./server/src/main/resources/front/qwen.html)：另一版原型

## 项目定位

从代码和 git 历史看，这更像一个“个人 Agent 实验项目 / Demo 项目”，适合用来验证下面这些事情：

- LangChain4j 在 Spring Boot 中如何组装 AI Service
- 如何接入模型、Redis 记忆、MCP 工具和本地工具
- 如何把知识写入本地文档与向量库
- 如何做流式聊天接口
- 如何为模型请求补上基础监控

如果你之后要把它继续往产品化方向推进，这个仓库已经提供了一个不错的骨架。

## 功能一览

### 1. 对话能力

- `POST /api/chat`：同步对话
- `POST /api/streamChat`：流式对话
- 会话记忆按 `sessionId` 隔离，最近 20 条消息存入 Redis
- 系统提示词位于 [`server/src/main/resources/system-prompt/chat-bot.txt`](./server/src/main/resources/system-prompt/chat-bot.txt)

### 2. 工具调用

当前 Agent 注册了以下工具：

- `TimeTool`：获取上海时区当前时间
- `RagTool`：把问答写入 Markdown 文档并同步到向量库
- `EmailTool`：发送简单文本邮件
- `MCP Tool Provider`：通过远程 MCP 接入搜索工具

对应代码位置：

- [`server/src/main/java/com/miles/milesagent/ai/AiChatService.java`](./server/src/main/java/com/miles/milesagent/ai/AiChatService.java)
- [`server/src/main/java/com/miles/milesagent/tool`](./server/src/main/java/com/miles/milesagent/tool)
- [`server/src/main/java/com/miles/milesagent/config/McpToolConfig.java`](./server/src/main/java/com/miles/milesagent/config/McpToolConfig.java)

### 3. 知识库写入

- `POST /api/insert`：手动插入知识
- `RagTool`：允许 Agent 在合适时自动保存知识
- 文本会同时写入本地 Markdown 文档和 pgvector
- 文档默认目录由 `rag.docs-path` 配置决定

默认知识示例可见：

- [`server/src/main/resources/docs/MilesAgent.md`](./server/src/main/resources/docs/MilesAgent.md)

### 4. 监控能力

项目给模型调用挂了 `ChatModelListener`，会记录：

- 请求开始 / 成功 / 失败次数
- 模型响应耗时
- Token 使用量
- 用户 / 会话 / 模型维度标签

相关代码：

- [`server/src/main/java/com/miles/milesagent/Monitor/AiModelMonitorListener.java`](./server/src/main/java/com/miles/milesagent/Monitor/AiModelMonitorListener.java)
- [`server/src/main/java/com/miles/milesagent/Monitor/AiModelMetricsCollector.java`](./server/src/main/java/com/miles/milesagent/Monitor/AiModelMetricsCollector.java)

Prometheus 指标端点：

- `GET /api/actuator/prometheus`

## 当前实现状态

这里我按代码实际状态说明一下，避免 README 和代码不一致：

- 已接通：同步聊天、流式聊天、Redis 会话记忆、本地工具、MCP 搜索工具、知识写入、Prometheus 指标
- 已配置但当前未真正参与聊天召回：RAG 检索器 `ContentRetriever`
- 原因：`AiChatService` 里 `.contentRetriever(contentRetriever)` 目前是注释状态
- 这意味着当前项目更偏“可保存知识的 Agent”，而不是“已完整启用检索增强问答的 Agent”

如果你后面想恢复 RAG 检索，只需要重新接上这条配置，再结合实际数据调优召回阈值即可。

## 技术栈

- Java 17
- Spring Boot 3.5.9
- LangChain4j 1.1.x
- DashScope / Qwen
- Redis
- PostgreSQL + pgvector
- Spring Mail
- Reactor
- Spring Boot Actuator
- Micrometer + Prometheus

## 核心结构

```text
server/src/main/java/com/miles/milesagent
├── ai/                  # Agent 接口定义与装配
├── config/              # 模型、RAG、Redis、MCP 等配置
├── controller/          # 对外 HTTP 接口
├── guardrail/           # 输入防护
├── Monitor/             # 模型调用监控
├── model/dto/           # 请求 DTO
└── tool/                # Agent 可调用工具

server/src/main/resources
├── docs/                # 本地知识文档
├── static/              # 默认静态聊天页
├── front/               # 早期前端原型
└── system-prompt/       # 系统提示词
```

## 接口说明

### 1. 同步聊天

`POST /api/chat`

请求示例：

```json
{
  "sessionId": 1001,
  "userId": 2001,
  "prompt": "你好，介绍一下你自己"
}
```

### 2. 流式聊天

`POST /api/streamChat`

请求体与 `/api/chat` 一致，返回 `Flux<String>`。

### 3. 插入知识

`POST /api/insert`

请求示例：

```json
{
  "question": "这个项目是做什么的？",
  "answer": "这是一个基于 Spring Boot 和 LangChain4j 的 AI Agent Demo。",
  "sourceName": "MilesAgent.md"
}
```

## 快速启动

### 1. 准备依赖

你至少需要准备：

- JDK 17
- Maven
- Redis
- PostgreSQL + pgvector 扩展
- DashScope API Key
- BigModel API Key

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

也可以直接通过环境变量覆盖，例如：

```bash
export DASHSCOPE_API_KEY=your_key
export BIGMODEL_API_KEY=your_key
export DEV_REDIS_HOST=localhost
export DEV_REDIS_PORT=6380
export DEV_PGVECTOR_HOST=localhost
export DEV_PGVECTOR_PORT=5432
```

### 3. 启动项目

```bash
cd server
./mvnw spring-boot:run
```

或者直接在仓库根目录执行 `bash start-miles-agent.sh`，它会顺带启动 pgvector、Redis 容器后再拉起服务。

默认地址：

- 服务首页：`http://localhost:10010/api/`
- 健康检查：`http://localhost:10010/api/actuator/health`
- Prometheus：`http://localhost:10010/api/actuator/prometheus`

## 关键实现说明

### Agent 组装

[`AiChatService`](./server/src/main/java/com/miles/milesagent/ai/AiChatService.java) 是整个项目的核心装配点，负责把这些能力组装到一个 `AiChat` Agent 上：

- `ChatModel`
- `StreamingChatModel`
- Redis `MessageWindowChatMemory`
- 本地工具 `TimeTool` / `RagTool` / `EmailTool`
- 远程 `McpToolProvider`

### 输入防护

[`SafeInputGuardrail`](./server/src/main/java/com/miles/milesagent/guardrail/SafeInputGuardrail.java) 对用户输入做了一个很轻量的敏感词拦截，属于演示级 guardrail。

### 向量存储

[`EmbeddingStoreConfig`](./server/src/main/java/com/miles/milesagent/config/EmbeddingStoreConfig.java) 当前使用 pgvector，并设置了：

- `createTable(true)`
- `dropTableFirst(true)`

这对本地演示很方便，但也意味着每次启动可能重建表，不适合生产环境长期保留数据。

## Git 历史速览

从 git 历史看，这个项目的主干开发集中在 `2026-04-25`：

1. `89a7bb6` `first commit`
2. `24eab7e` `Add Miles Agent implementation`
3. `9ec9f7c` `Update README.md`
4. `8d15acb` `Update README and application config`
5. `2672c8c` `Add streaming chat static page and rename artifact`

这也和现在仓库的形态基本一致：先完成 Agent 主体，再补配置说明，最后增加流式聊天静态页。

## 目前的不足

如果按“继续维护”的视角看，这个项目还有几个比较明显的改进点：

- RAG 检索链路已配置但未接入实际聊天
- 测试覆盖很少，目前只有一个 `contextLoads()` 测试
- 敏感词防护较基础，仍是演示级实现
- `pgvector` 配置里的 `dropTableFirst(true)` 不适合长期数据保留
- README 之前几乎为空，项目说明主要散落在代码注释中

## 适合继续演进的方向

- 接通 `contentRetriever`，完成真正可用的 RAG 问答
- 为工具调用和知识写入补集成测试
- 把监控面板与 Prometheus / Grafana 联动起来
- 为前端聊天页补上更完整的会话管理和错误提示
- 把邮件、搜索、知识库等能力拆成更稳定的工具策略

## License

仓库里目前还没有明确的开源许可证配置。如果你准备公开发布到 GitHub，建议补一个 `LICENSE` 文件。
