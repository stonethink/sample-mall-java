## ADDED Requirements

### Requirement: 测试 register 端点

UserController SHALL 提供 POST /api/users/register 端点，能够注册新用户。

#### Scenario: register 注册新用户成功
- **WHEN** 客户端发送 POST /api/users/register 请求，包含有效的用户数据
- **THEN** 返回 HTTP 201 状态码
- **AND** 返回注册的用户信息（密码被脱敏）

#### Scenario: register 用户名已存在
- **WHEN** 客户端发送 POST /api/users/register 请求，用户名已被注册
- **THEN** 返回 HTTP 400 状态码
- **AND** 返回错误信息 "USERNAME_EXISTS"

### Requirement: 测试 login 端点

UserController SHALL 提供 POST /api/users/login 端点，能够验证用户登录。

#### Scenario: login 登录成功
- **WHEN** 客户端发送 POST /api/users/login 请求，包含正确的用户名和密码
- **THEN** 返回 HTTP 200 状态码
- **AND** 返回用户信息（密码被脱敏）

#### Scenario: login 用户名或密码错误
- **WHEN** 客户端发送 POST /api/users/login 请求，用户名或密码错误
- **THEN** 返回 HTTP 400 状态码
- **AND** 返回错误信息 "UNAUTHORIZED"

### Requirement: 测试 getCurrentUser 端点

UserController SHALL 提供 GET /api/users/me 端点，能够获取当前登录用户信息。

#### Scenario: getCurrentUser 已登录
- **WHEN** 客户端发送 GET /api/users/me 请求，且已登录
- **THEN** 返回 HTTP 200 状态码
- **AND** 返回当前用户信息

#### Scenario: getCurrentUser 未登录
- **WHEN** 客户端发送 GET /api/users/me 请求，且未登录
- **THEN** 返回 HTTP 400 状态码
- **AND** 返回错误信息 "UNAUTHORIZED"

### Requirement: 测试 listAll 端点

UserController SHALL 提供 GET /api/users 端点，管理员能够获取所有用户列表。

#### Scenario: listAll 管理员访问
- **WHEN** 管理员用户发送 GET /api/users 请求
- **THEN** 返回 HTTP 200 状态码
- **AND** 返回用户列表

#### Scenario: listAll 非管理员访问
- **WHEN** 非管理员用户发送 GET /api/users 请求
- **THEN** 返回 HTTP 400 状态码
- **AND** 返回错误信息 "FORBIDDEN"

### Requirement: 测试 delete 端点

UserController SHALL 提供 DELETE /api/users/{id} 端点，管理员能够删除用户。

#### Scenario: delete 管理员删除用户
- **WHEN** 管理员用户发送 DELETE /api/users/{id} 请求
- **THEN** 返回 HTTP 204 状态码
