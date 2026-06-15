## Context

Miles-Agent 当前已经有完整的聊天、RAG、Redis 记忆和前端页面，但所有能力默认对匿名用户开放，前端还通过固定或手填的 `userId` 来模拟身份。这种模式不适合继续扩展用户侧能力：聊天记录无法和真实用户绑定，知识写入缺少登录边界，后续也无法自然增加“我的会话”“我的资料”之类的功能。

这次改动要在现有基础设施上补齐最小可用的邮箱认证体系。项目已经具备 PostgreSQL、Redis 和邮件发送能力，因此可以在不引入额外中间件的前提下，把“注册、登录、会话、接口保护”整合进现有架构。

## Goals / Non-Goals

**Goals:**

- 支持邮箱验证码注册，并在注册成功后自动登录
- 支持邮箱 + 密码登录、登出、登录态探活
- 让聊天与知识写入等业务接口统一依赖登录态，不再信任前端传入的 `userId`
- 让前端具备完整的登录/注册流程、路由守卫和认证状态管理
- 使用项目已有 Redis 与 PostgreSQL 实现会话和用户数据持久化

**Non-Goals:**

- 忘记密码、重置密码、修改昵称、修改密码
- 邮箱发送限频、登录失败锁定、记住我长会话
- 第三方登录、角色权限体系、管理后台
- 引入完整 Spring Security 过滤器体系或 Flyway/Liquibase 迁移框架

## Decisions

### 1. 复用现有 PostgreSQL 与 Redis，不引入新基础设施

- 用户表直接落到当前 `miles-agent-pgvector` 使用的 PostgreSQL 数据库中
- 验证码存在 Redis，使用 TTL 自动过期
- Session 使用 Spring Session Data Redis 管理

这样做的原因是：当前项目已经依赖这两类基础设施，继续复用可以把变更集中在业务代码，而不是部署体系。

### 2. 使用 MyBatis + JDBC，而不是 JPA 或 MyBatis-Plus

- 登录相关 SQL 很简单，MyBatis 足够表达
- 用户明确偏好 MyBatis
- 不引入额外 ORM 抽象层，可以让建表、查询和异常分支更直接

备选方案是 JPA / MyBatis-Plus，但都会引入当前项目里还不存在的额外建模方式，收益不大。

### 3. 密码只引入 BCrypt，不引入完整 Spring Security

- 依赖 `spring-security-crypto` 获取 BCrypt
- 不引入 `spring-boot-starter-security`
- 鉴权通过自写 `HandlerInterceptor` 完成

这样既满足密码安全存储，又避免为了简单登录场景接入完整 Security 过滤器链和配置体系。

### 4. 登录态采用 HttpOnly cookie + Redis session

- 登录成功或注册成功后，把 `userId` 写入 `HttpSession`
- 浏览器通过 `credentials: 'include'` 自动携带 cookie
- Session 与 Redis TTL 统一按 30 分钟无操作过期

这个方案与现有 Spring MVC 架构天然兼容，也最适合浏览器端聊天应用。

### 5. 认证边界通过白名单拦截器控制

- `/api/auth/**`、`/api/actuator/**` 和静态资源请求白名单放行
- 其他业务接口默认要求 session 中存在 `userId`
- Controller 内不再信任 `ChatRequest.userId`

这让认证规则集中在一处管理，同时保持 Controller 逻辑简洁。

### 6. 前端认证与聊天状态分离

- 新增 `auth` store 负责 `fetchMe`、`login`、`register`、`logout`
- 现有 `chat` store 继续负责消息流转，但不再传真实用户身份
- 路由守卫根据认证态决定进入 `/login`、`/register` 还是 `/`

这样可以避免把登录流程揉进聊天 store，后续扩展用户相关页面时也更清晰。

## Risks / Trade-offs

- [Redis 不可用会同时影响验证码和会话] → 明确返回 503，不做本期降级，保持失败模式可见
- [登录后如果前端忘记携带 cookie，会持续收到 401] → 在 `auth` store 与 `api.ts` 中统一补 `credentials: 'include'`，避免分散遗漏
- [旧的 Redis 会话消息可能带着匿名时期的上下文] → 前端调试时建议更换 `sessionId`，并让后端只把 session 中真实用户写入监控上下文
- [数据库迁移没有引入专业框架] → 复用当前项目“启动期建表”的做法，在启动日志中明确输出建表结果；后续需求增大再引入 Flyway
- [自写拦截器比 Spring Security 少一层生态兜底] → 将职责限制在“是否已登录”这一件事，不在本期承担复杂授权逻辑

## Migration Plan

1. 在后端补齐依赖、数据源配置、用户表建表与 MyBatis mapper
2. 实现认证 API、验证码发送与会话写入逻辑
3. 加入统一鉴权拦截器，并让聊天/知识接口改为从 session 读取用户
4. 在前端增加登录/注册页面、认证 store、路由守卫，以及带 cookie 的请求调用
5. 手工验证注册、登录、登出、401 拦截、30 分钟会话与 Prometheus 白名单

回滚策略：

- 如果功能未上线，只需回退本次代码变更即可
- 如果已上线且需要快速回滚，可先去掉拦截器注册，恢复匿名访问；数据库中的 `users` 表保留不影响旧功能

## Open Questions

- 本期是否需要给登录 401 的前端自动跳转增加统一 toast 提示，目前设计为“路由守卫兜底 + 接口侧按需展示”
- 注册验证码是否需要“验证成功后短暂允许免重复输入 code”，当前设计仍要求 `register` 再带一次 `code`，实现更稳但 UX 略多一步隐藏校验
