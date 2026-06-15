## 1. 后端依赖与基础设施接入

- [x] 1.1 在 `server/pom.xml` 中加入 MyBatis、JDBC/PostgreSQL、Spring Session Redis、Spring Security Crypto 依赖
- [x] 1.2 在 `server/src/main/resources/application.yml` 中补齐 `spring.datasource`、`spring.session`、`mybatis` 和 session cookie 相关配置
- [x] 1.3 新增启动期建表/建扩展逻辑，确保 `users` 表与 `pgcrypto` 扩展在 PostgreSQL 中可用
- [x] 1.4 验证 `cd server && ./mvnw -DskipTests compile` 可以通过，且应用启动时不再因数据源或会话配置报错

## 2. 认证后端主流程

- [x] 2.1 新增用户实体、认证 DTO、MyBatis mapper 接口与 `mapper/*.xml`，覆盖按邮箱查询与创建用户
- [x] 2.2 实现验证码生成、缓存、校验与删除逻辑，并接入现有 `JavaMailSender` 发送纯文本验证码邮件
- [x] 2.3 实现 `AuthService`，覆盖 `sendCode`、`verifyCode`、`register`、`login`、`logout`、`me`
- [x] 2.4 新增 `AuthController`，提供 `/api/auth/sendCode`、`/verifyCode`、`/register`、`/login`、`/logout`、`/me` 接口
- [x] 2.5 为注册与登录接入 BCrypt 哈希/比对，并统一登录失败返回 401 “邮箱或密码错误”

## 3. 鉴权拦截与业务接口收口

- [x] 3.1 新增 `AuthInterceptor` 并注册白名单规则，确保 `/api/auth/**`、`/api/actuator/**` 和静态资源放行，其余业务接口默认鉴权
- [x] 3.2 修改 `AiChatController`，从 `HttpSession` 读取真实登录用户并写入监控上下文，不再依赖请求体中的 `userId`
- [x] 3.3 评估并更新与聊天请求相关的 DTO / store 兼容逻辑，保证旧字段保留但不再作为真实身份来源
- [x] 3.4 手工验证未登录访问 `/api/chat`、`/api/streamChat`、`/api/insert` 会返回 401，`/api/actuator/prometheus` 仍可匿名访问

## 4. 前端认证体验

- [x] 4.1 新增 `web/src/stores/auth.ts`，实现 `fetchMe`、`sendCode`、`verifyCode`、`register`、`login`、`logout`
- [x] 4.2 在 `web/src/lib/api.ts` 及相关请求调用中统一加上 `credentials: 'include'`
- [x] 4.3 新增 `/login`、`/register` 路由与守卫逻辑，未登录用户跳转登录页，已登录用户阻止回到认证页
- [x] 4.4 实现 `LoginView.vue` 与 `RegisterView.vue`，其中注册页采用三步向导：发送验证码、校验验证码、填写昵称和密码
- [x] 4.5 在应用启动阶段接入 `fetchMe()` 探活，并根据认证态决定渲染聊天页还是登录页

## 5. 聊天页联动改造

- [x] 5.1 从 `settings` store 与聊天请求体中移除真实 `userId` 输入/传递逻辑，只保留 `sessionId`、主题和接口模式等设置
- [x] 5.2 在聊天页头部增加已登录用户昵称展示与“退出登录”入口
- [ ] 5.3 确保同步聊天、流式聊天、知识写入等前端调用在登录后可正常工作，未登录时能被认证流程正确拦回

## 6. 测试与验收

- [ ] 6.1 为 `AuthService` 编写后端测试，覆盖验证码校验、邮箱重复注册、注册成功、登录成功、登录失败等核心分支
- [ ] 6.2 为鉴权拦截编写后端集成测试，覆盖白名单放行与业务接口 401
- [ ] 6.3 为前端 `auth` store 和路由守卫补测试，覆盖 `fetchMe`、`login`、`register`、`logout` 与跳转分支
- [ ] 6.4 按设计文档手工跑通注册、登录、登出、30 分钟会话、匿名 401、Prometheus 白名单等验收场景
