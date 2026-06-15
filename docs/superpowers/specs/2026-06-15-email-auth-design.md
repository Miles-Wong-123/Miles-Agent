# 邮箱注册登录设计文档

- 状态：设计已确认，待落地
- 日期：2026-06-15
- 范围：为 Miles-Agent 增加“邮箱注册 + 邮箱登录”能力，并把后端所有业务接口收敛到“必须登录”
- 后续动作：用 writing-plans skill 把本文转成可执行的实施计划

## 1. 目标与非目标

**目标**

- 用户必须先注册并登录，才能使用 `/api/chat`、`/api/streamChat`、`/api/insert` 等聊天与知识能力
- 注册流程：输入邮箱 → 邮箱收到 6 位验证码 → 校验通过后再要求设置密码（两次输入一致）+ 填写昵称 → 自动登录
- 登录流程：邮箱 + 密码 → 登录态以 HttpOnly cookie 维持
- 现有聊天接口的 `userId` 自动绑定到登录用户，前端不再传 `'guest'`

**非目标（本期不做）**

- 忘记密码 / 重置密码（用户不需要时手动改 DB）
- 验证码发送限频与同一邮箱发送频率限制
- “记住我” 长会话延长 cookie 有效期
- 登录失败次数锁定
- 第三方 / 社交登录（用户已明确“只能邮箱登录”）
- 角色与权限体系
- 用户资料编辑页（昵称注册后不开放修改入口）
- 邮件模板美化（纯文本即可）

## 2. 关键决策与理由

| 决策点 | 选型 | 理由 |
|---|---|---|
| 数据库 | 复用现有 PostgreSQL 实例 `miles-agent-pgvector`（数据库名 `infinitechat`） | 已在跑，不引新基础设施；MyBatis 连 PG 与连 MySQL 写法 99% 一致 |
| ORM | MyBatis（用户偏好） | 不引 MyBatis-Plus，避免再增依赖；登录功能 SQL 简单 |
| 密码哈希 | BCrypt（仅引入 `spring-security-crypto` 这个 ~80KB 小包） | 不引入完整 Spring Security 框架 |
| 登录态 | Spring Session + Redis + HttpOnly cookie | 项目已有 Redis；cookie HttpOnly 抗 XSS；项目 CORS 已 `allowCredentials(true)` |
| 鉴权拦截 | 自写 `HandlerInterceptor`，按白名单放行 | 比 Spring Security 配置量小一个数量级 |
| 验证码存储 | Redis，key=`verify:email:{email}`，TTL 5 分钟 | 不进 DB；过期自然丢弃 |
| 数据库迁移 | 启动时执行 `users.sql`（与 `EmbeddingStoreConfig` 已有“启动建表”惯例一致） | 不引入 Flyway/Liquibase |
| 自动登录 | 注册成功直接写 session | 体验上等价于注册即登录，少一次密码输入 |

## 3. 数据模型

### 3.1 `users` 表（PostgreSQL）

```sql
CREATE TABLE IF NOT EXISTS users (
  id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(254) NOT NULL UNIQUE,
  nickname      VARCHAR(32)  NOT NULL,
  password_hash VARCHAR(72)  NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
```

字段约束：

- `email`：UNIQUE。用 RFC 5321 上限 254 个字符。
- `nickname`：1–32 字符（应用层校验），允许中英文混合，**允许重名**。
- `password_hash`：BCrypt 输出固定 60 字节，长度上限留 72 以兼容前缀变化。
- `id`：UUID v4。注意需要 PostgreSQL 启用 `pgcrypto` 扩展或在应用层生成 UUID。本项目已启用 pgvector 扩展机制，启动脚本里追加 `CREATE EXTENSION IF NOT EXISTS pgcrypto;`。

### 3.2 Redis 临时数据

| Key 模板 | 值 | TTL | 用途 |
|---|---|---|---|
| `verify:email:{email}` | 6 位数字字符串 | 300 秒 | 注册时的邮箱验证码 |
| `spring:session:sessions:{id}` 等 | Spring Session 自身管理 | 默认 1800 秒（30 分钟无操作） | 登录态 |

## 4. 后端 API

所有接口前缀沿用现有 `server.servlet.context-path=/api`。

### 4.1 鉴权接口（白名单，未登录可访问）

| 方法 | 路径 | 入参 | 成功返回 | 失败返回 |
|---|---|---|---|---|
| POST | `/api/auth/sendCode` | `{email}` | `{ok:true}` | 400（邮箱格式不合法 / 已注册） |
| POST | `/api/auth/verifyCode` | `{email, code}` | `{ok:true}` | 400（验证码错误或过期） |
| POST | `/api/auth/register` | `{email, code, nickname, password}` | `{userId, email, nickname}` | 400（验证码失效 / 邮箱已注册 / 昵称非法 / 密码 < 8 位） |
| POST | `/api/auth/login` | `{email, password}` | `{userId, email, nickname}` | 401（邮箱不存在或密码错误，统一文案） |
| POST | `/api/auth/logout` | 无 | `{ok:true}` | — |
| GET  | `/api/auth/me` | 无 | `{userId, email, nickname}`（已登录）；401（未登录） | — |

### 4.2 接口语义细节

- **`sendCode`**：先查 `users` 是否已注册该邮箱；已注册返回 400 “邮箱已注册”；未注册则生成 6 位数字验证码，覆盖写入 Redis（同一邮箱重发会刷新 5 分钟有效期），调用 `JavaMailSender` 发送纯文本邮件。
- **`verifyCode`**：仅做“是否匹配且未过期”的校验，**不消费**验证码。失败返回 400。这一步存在的意义是注册向导第 2 步给用户即时反馈，不必等填完密码才知道验证码错。
- **`register`**：再次校验验证码并立即从 Redis 删除。然后 BCrypt 哈希密码 → INSERT users → 写 session（`session.setAttribute("userId", uuid)`）。
- **`login`**：查 users.email → BCrypt verify → 写 session。失败统一返回 401 “邮箱或密码错误”，不区分“邮箱不存在”与“密码错”，避免邮箱枚举。
- **`logout`**：`request.getSession().invalidate()`，浏览器侧 cookie 由 Spring Session 清除指令处理。
- **`me`**：未登录返回 401，前端拿来做“启动时探活”，决定渲染登录页还是 chat 页。

### 4.3 密码与昵称校验

- 密码长度 ≥ 8 字符；不强制大小写、数字、特殊字符（YAGNI）。两次输入一致由前端校验，后端只接收一次密码。
- 昵称：trim 后长度在 [1, 32]，不允许空字符串与纯空白。

## 5. 鉴权机制

### 5.1 依赖

`server/pom.xml` 新增：

- `mybatis-spring-boot-starter`（最新稳定版）
- `org.postgresql:postgresql`（JDBC 驱动）
- `org.springframework.boot:spring-boot-starter-jdbc`（数据源 / 事务，MyBatis starter 通常会传递；显式声明便于阅读）
- `org.springframework.session:spring-session-data-redis`
- `org.springframework.security:spring-security-crypto`（仅 BCrypt）

显式不引：`spring-boot-starter-security`、`spring-boot-starter-data-jpa`。

### 5.2 配置

`application.yml` 新增：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${pgvector.host}:${pgvector.port}/${pgvector.database}
    username: ${pgvector.user}
    password: ${pgvector.password}
    driver-class-name: org.postgresql.Driver
  session:
    store-type: redis
    timeout: 30m
mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

### 5.3 拦截器

新增 `AuthInterceptor implements HandlerInterceptor`，注册到 `WebMvcConfigurer`：

- **白名单**：`/auth/**`、`/actuator/**`（Prometheus 抓取）、SPA fallback 命中的静态资源（`SpaFallbackController` 已处理）
- **拦截路径**：除白名单外的全部 `/**`
- **判断逻辑**：`session.getAttribute("userId")` 为 null 时直接 `response.sendError(401)`，不再走 controller
- **职责单一**：拦截器只做 401 拦截，不写 `MonitorContextHolder`；监控上下文仍由各 Controller 自行写入

### 5.4 Cookie / Session 时长

`application.yml` 显式配置：

```yaml
server:
  servlet:
    session:
      timeout: 30m
      cookie:
        http-only: true
        same-site: lax
        max-age: 30m
```

效果：cookie 与 Redis session 同时 30 分钟过期；用户在 30 分钟内任意一次请求都会刷新到期时间；关闭浏览器再打开仍在 30 分钟内可继续使用。

### 5.5 与现有接口的衔接

- `AiChatController` 的 `/chat`、`/streamChat`、`/insert` 不再从 `ChatRequest` 读 `userId`；改为从 `HttpSession` 读真实用户 UUID 并写入 `MonitorContextHolder`（替换原本从 `ChatRequest` 取值的写法）。
- `ChatRequest.userId` 字段保留但忽略（不破坏 DTO 兼容，老前端调用不报错）。
- `sessionId` 仍由前端生成（一个聊天话题一个），现在归属真实用户。

## 6. 前端流程

### 6.1 路由

`web/src/router/index.ts` 新增：

```ts
{ path: '/login',    name: 'login',    component: () => import('@/views/LoginView.vue') }
{ path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue') }
```

### 6.2 守卫

`router.beforeEach`：

- 未认证用户访问 `/login` 或 `/register` 之外的任意路径 → 重定向 `/login`
- 已认证用户访问 `/login` 或 `/register` → 重定向 `/`
- 认证状态来自 Pinia `auth` store；首次加载时 `App.vue` 的 `onMounted` 调一次 `auth.fetchMe()`（拿 401 视为未登录）

### 6.3 状态

`web/src/stores/auth.ts`（新增）：

```ts
state: { user: { userId, email, nickname } | null, ready: boolean }
actions: fetchMe(), sendCode(email), verifyCode(email, code), register(...), login(...), logout()
```

`fetch` 调用统一带 `credentials: 'include'`，让浏览器自动处理 `SESSION` cookie。**现有 `web/src/lib/api.ts` 中的 `chat()` 与 `streamChat()` 也需补上 `credentials: 'include'`**，否则登录后访问聊天接口仍会被 401 拒绝。

### 6.4 注册页（`RegisterView.vue`）

单页 3 步向导，**不跳路由**，只是组件内 `step` 状态：

1. 邮箱输入框 + “发送验证码”按钮 → 调 `sendCode`
2. 验证码 6 位输入框 + “下一步”按钮 → 调 `verifyCode`（仅校验）
3. 昵称 + 密码 + 确认密码 → 提交调 `register`，成功后路由跳 `/`

任意一步出错就地展示错误文案；用户可随时退到上一步重填。

### 6.5 登录页（`LoginView.vue`）

单一表单：邮箱 + 密码 + 提交。下方放“没有账号？立刻注册”链接跳 `/register`。

### 6.6 头像区与登出

`AppHeader.vue` 右上角加：

- 已登录时显示昵称 + 下拉菜单
- 菜单里只有“退出登录”项，调 `auth.logout()` → 跳 `/login`

### 6.7 store 改动

- `web/src/stores/settings.ts` 删除写死的 `userId: 'guest'` 与对应 state；`useStreaming` / `theme` 保持原样
- `web/src/stores/chat.ts` 中 `runAssistant` 不再传 `userId`；后端从 session 读

## 7. 错误与边界情况

| 场景 | 行为 |
|---|---|
| 邮件发送失败（SMTP 异常） | `sendCode` 返回 500 + 通用文案；不删除 Redis 里已写入的码（下次用户重发会覆盖） |
| 用户填完密码才提交时验证码已过期 | `register` 返回 400，前端把向导退到第 2 步 |
| 同一邮箱并发注册 | 数据库 `email UNIQUE` 兜底；INSERT 冲突时返回 400 “邮箱已注册” |
| Redis 不可用 | 验证码无法发送、session 无法读写；接口返回 503；不做降级 |
| Cookie 被禁用 | 登录后 `/api/auth/me` 持续返回 401；前端把这种现象包成“登录态丢失，请重试”提示 |
| 用户已登录后访问 `/api/auth/login` | 直接覆盖现有 session；不视为错误 |

## 8. 验收标准

- 全新数据库环境下，启动后第一次访问 `http://localhost:10010/api/` 自动跳到 `/login`
- 用未注册邮箱跑一遍“发送验证码 → 输入验证码 → 设置密码 → 自动跳到 chat 页 → 能正常对话”
- 关闭浏览器再打开 30 分钟内仍处于登录态；超过 30 分钟无操作再打开会跳回登录页
- 退出登录后再访问 `/`、`/api/chat` 都不再可达
- `users` 表里能看到 `password_hash` 是 `$2a$` 开头的 BCrypt 串，**绝不**是明文
- 同一邮箱注册第二次返回 400 “邮箱已注册”
- 直接 `curl /api/chat` 不带 cookie 返回 401
- `/api/actuator/prometheus` 不需要登录仍可访问

## 9. 测试策略

- **后端**：为 `AuthService` 写单元测试覆盖“验证码生成/校验、密码哈希/比对、注册/登录主流程、邮箱重复”。`AuthInterceptor` 写一份 MockMvc 集成测试覆盖白名单与 401 行为。
- **前端**：Vitest 覆盖 `auth` store 的 `login` / `register` / `logout` / `fetchMe`，以及路由守卫的跳转分支。组件测试覆盖注册向导的步骤切换与错误回退。
- **手工**：按第 8 节验收标准跑一遍端到端。

## 10. 后续可扩展项（不在本期）

- 忘记密码（复用同一套验证码机制 + `POST /api/auth/resetPassword`）
- 验证码限频（Redis 计数 + Lua 脚本，60 秒一次 / 1 小时上限）
- 登录失败锁定
- “记住我”延长 cookie
- 用户资料页（改昵称 / 改密码）
- OAuth2 第三方登录
