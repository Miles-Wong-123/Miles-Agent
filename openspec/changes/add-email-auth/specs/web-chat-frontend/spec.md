## MODIFIED Requirements

### Requirement: 同步聊天

系统 SHALL 允许已登录用户提交一条 prompt 并接收完整的 AI 回复。

#### Scenario: 用户在同步模式下发送一条消息
- **WHEN** 已登录用户在设置面板将"接口模式"切换为"同步"，在输入框键入 prompt 并按 Enter
- **THEN** 前端 POST `/api/chat` 请求体为 `{sessionId, prompt}`
- **AND** 该请求 MUST 携带浏览器当前登录态 cookie
- **AND** 收到响应后整段文字一次性显示在 AI 消息位置

### Requirement: 流式聊天

系统 SHALL 默认使用流式接口，让已登录用户逐字看到 AI 回复，并可中途停止。

#### Scenario: 用户发送消息触发流式输出
- **WHEN** 已登录用户在默认模式下发送消息
- **THEN** 前端 POST `/api/streamChat` 请求体为 `{sessionId, prompt}`
- **AND** 该请求 MUST 携带浏览器当前登录态 cookie
- **AND** 服务端响应 Content-Type 为 `text/plain;charset=UTF-8`
- **AND** 前端逐块读取响应字节并实时追加到 AI 消息内容
- **AND** UI 自动滚动到最新内容（除非用户已手动滚上去）

#### Scenario: 用户按停止按钮中断流式输出
- **WHEN** 流式输出过程中，用户点击发送按钮位置出现的停止按钮
- **THEN** 前端调用 AbortController abort，已收到的内容**保留**显示
- **AND** 该 AI 消息标记 `stoppedByUser`，UI 在内容末尾显示灰字"已停止"
- **AND** 用户可立即发送下一条消息

### Requirement: 设置面板与持久化

系统 SHALL 提供一个从右侧滑入的设置面板，允许用户配置 `sessionId`、接口模式（同步 / 流式）、主题，并将这些设置持久化到 localStorage。

#### Scenario: 修改设置后刷新页面
- **WHEN** 用户在设置面板中修改主题或接口模式，关闭面板
- **THEN** 设置写入 localStorage（key: `miles.settings`）
- **AND** 刷新页面后这些设置仍然生效

#### Scenario: 首次进入页面时初始化 sessionId
- **WHEN** 用户首次访问页面，localStorage 中没有 `sessionId`
- **THEN** 系统自动生成一个 uuidv4 作为 sessionId 并持久化

## ADDED Requirements

### Requirement: 前端必须通过登录与注册流程进入聊天页

系统 SHALL 提供 `/login` 与 `/register` 页面，并在未认证时阻止用户直接进入聊天页面。

#### Scenario: 未登录用户访问聊天页
- **WHEN** 未认证用户访问 `/` 或其他受保护的前端路由
- **THEN** 前端把用户重定向到 `/login`

#### Scenario: 已登录用户访问认证页
- **WHEN** 已认证用户访问 `/login` 或 `/register`
- **THEN** 前端把用户重定向到 `/`

### Requirement: 前端支持三步注册向导

系统 SHALL 在注册页提供单页三步向导，依次完成邮箱验证码发送、验证码校验、昵称与密码设置。

#### Scenario: 用户完成三步注册
- **WHEN** 用户在注册页依次完成发送验证码、校验验证码、填写昵称和密码并提交
- **THEN** 前端完成注册请求
- **AND** 注册成功后跳转到聊天页

#### Scenario: 注册时验证码失效
- **WHEN** 用户在最后一步提交注册时收到“验证码错误或过期”响应
- **THEN** 前端在当前页面展示错误
- **AND** 注册向导允许用户返回验证码步骤重新输入

### Requirement: 前端支持登录探活与退出登录

系统 SHALL 在应用启动时探测当前登录态，在已登录时显示用户昵称，并提供退出登录入口。

#### Scenario: 应用启动时探测登录态
- **WHEN** 前端应用初始化
- **THEN** 前端请求 `GET /api/auth/me`
- **AND** 根据结果决定渲染聊天页还是登录页

#### Scenario: 已登录用户退出
- **WHEN** 已登录用户在页面头部点击“退出登录”
- **THEN** 前端调用 `POST /api/auth/logout`
- **AND** 本地认证状态被清空
- **AND** 页面跳转到 `/login`
