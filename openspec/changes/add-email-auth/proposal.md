## Why

Miles-Agent 目前仍允许匿名用户直接访问聊天与知识写入接口，这让用户身份、会话归属和后续功能扩展都不稳定。既然项目已经有 Redis、PostgreSQL 和邮件能力，现在补上邮箱注册登录并把业务接口收敛到“必须登录”，是把 Demo 往可持续产品形态推进的必要一步。

## What Changes

- 新增邮箱注册流程：发送 6 位验证码、校验验证码、设置昵称与密码、注册成功后自动登录
- 新增邮箱密码登录、登出、当前登录用户探活接口，登录态通过 HttpOnly cookie + Redis session 维持
- 为后端增加统一鉴权拦截，未登录用户不能访问 `/api/chat`、`/api/streamChat`、`/api/insert` 等业务接口
- 新增 `users` 用户表及启动期建表逻辑，密码改为 BCrypt 哈希存储
- 前端新增 `/login`、`/register` 页面、路由守卫、认证 store，以及登录后的昵称展示与退出登录入口
- **BREAKING**：聊天接口请求体不再依赖前端传入 `guest`/自定义 `userId` 作为真实用户身份；后端统一从 session 读取登录用户
- **BREAKING**：所有业务接口默认要求已登录，匿名直接访问会返回 401

## Capabilities

### New Capabilities

- `email-authentication`: 邮箱验证码注册、邮箱密码登录、基于 HttpOnly cookie 的会话维持，以及对业务接口的登录态保护

### Modified Capabilities

- `web-chat-frontend`: 前端聊天体验改为基于认证态访问，新增登录/注册流程、路由守卫、认证 store，并移除前端手工维护 `guest` userId 的行为

## Impact

- 后端新增认证控制器、服务层、MyBatis mapper、用户表建表 SQL、Redis session 配置和鉴权拦截器
- 后端业务接口会从 `HttpSession` 读取真实用户身份，监控上下文同步改为使用登录用户
- 前端新增登录/注册视图、认证状态管理、带 cookie 的接口调用与路由守卫
- 新增依赖：MyBatis、JDBC/PostgreSQL、Spring Session Redis、Spring Security Crypto（BCrypt）
- 运行要求变更：未登录用户不能直接使用聊天与知识接口；已有前端调试方式需要先登录或携带 cookie
