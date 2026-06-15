## ADDED Requirements

### Requirement: 用户可以通过邮箱验证码完成注册

系统 SHALL 允许未注册用户通过邮箱验证码完成注册。注册流程 MUST 包含发送验证码、校验验证码、提交昵称与密码三个阶段，并在注册成功后自动建立登录态。

#### Scenario: 未注册邮箱请求验证码
- **WHEN** 用户向 `POST /api/auth/sendCode` 提交一个未注册且格式合法的邮箱地址
- **THEN** 系统发送一封包含 6 位数字验证码的邮件到该邮箱
- **AND** 系统为该邮箱写入一个 5 分钟有效的验证码记录

#### Scenario: 已注册邮箱请求验证码
- **WHEN** 用户向 `POST /api/auth/sendCode` 提交一个已注册邮箱
- **THEN** 系统返回 400
- **AND** 响应明确说明该邮箱已注册

#### Scenario: 用户校验错误或过期验证码
- **WHEN** 用户向 `POST /api/auth/verifyCode` 或 `POST /api/auth/register` 提交错误或已过期的验证码
- **THEN** 系统返回 400
- **AND** 不创建用户账号

#### Scenario: 用户完成注册
- **WHEN** 用户向 `POST /api/auth/register` 提交合法邮箱、正确验证码、合法昵称和长度不少于 8 位的密码
- **THEN** 系统创建一个新用户
- **AND** 系统为该用户建立登录态
- **AND** 响应返回该用户的 `userId`、`email` 和 `nickname`

### Requirement: 用户可以通过邮箱和密码登录

系统 SHALL 允许已注册用户通过邮箱和密码登录，并在失败时返回统一的认证失败文案，避免暴露“邮箱是否存在”的差异。

#### Scenario: 用户登录成功
- **WHEN** 用户向 `POST /api/auth/login` 提交已注册邮箱和正确密码
- **THEN** 系统建立登录态
- **AND** 响应返回该用户的 `userId`、`email` 和 `nickname`

#### Scenario: 用户登录失败
- **WHEN** 用户向 `POST /api/auth/login` 提交不存在的邮箱或错误密码
- **THEN** 系统返回 401
- **AND** 响应使用统一的“邮箱或密码错误”文案

### Requirement: 系统通过会话 cookie 维持登录态

系统 SHALL 通过 HttpOnly cookie 维持登录态，并允许前端查询当前登录用户与主动登出。

#### Scenario: 已登录用户查询当前身份
- **WHEN** 已登录用户访问 `GET /api/auth/me`
- **THEN** 系统返回该用户的 `userId`、`email` 和 `nickname`

#### Scenario: 未登录用户查询当前身份
- **WHEN** 未登录用户访问 `GET /api/auth/me`
- **THEN** 系统返回 401

#### Scenario: 用户主动退出登录
- **WHEN** 已登录用户访问 `POST /api/auth/logout`
- **THEN** 系统使当前登录态失效
- **AND** 该用户随后访问 `GET /api/auth/me` 会收到 401

### Requirement: 业务接口必须要求已登录

系统 SHALL 对聊天与知识写入等业务接口强制执行登录校验，未登录请求不得再通过请求体中的 `userId` 模拟身份。

#### Scenario: 未登录访问聊天接口
- **WHEN** 未登录用户访问 `POST /api/chat`、`POST /api/streamChat` 或 `POST /api/insert`
- **THEN** 系统返回 401

#### Scenario: 已登录访问业务接口
- **WHEN** 已登录用户访问 `POST /api/chat`、`POST /api/streamChat` 或 `POST /api/insert`
- **THEN** 系统允许请求继续执行
- **AND** 系统使用当前登录用户作为真实业务身份

#### Scenario: 白名单接口保持匿名可访问
- **WHEN** 未登录用户访问 `POST /api/auth/sendCode` 或 `GET /api/actuator/prometheus`
- **THEN** 系统允许请求继续执行
