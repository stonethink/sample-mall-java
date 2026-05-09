# 用户模块 — 个人中心功能设计文档

## 概述

在用户模块已完整实现的基础上，扩展**个人中心**功能，使已登录用户能够自主修改个人资料（昵称、手机号、邮箱）和密码，同时支持管理员重置任意用户的密码。

## 后端 API 设计

在现有 `UserController.java` 基础上新增 3 个端点：

| 方法 | 端点 | 权限 | 请求体 | 说明 |
|------|------|------|--------|------|
| `PUT` | `/api/users/me` | 登录用户 | `{ nickname, phone, email }` | 更新当前用户的昵称、手机号、邮箱 |
| `POST` | `/api/users/me/password` | 登录用户 | `{ oldPassword, newPassword }` | 修改自己的密码，需验证旧密码 |
| `POST` | `/api/users/{id}/reset-password` | ADMIN | `{ newPassword }` | 管理员重置任意用户密码，无需旧密码 |

现有 `PUT /api/users/{id}` 保持为 ADMIN 专属，用于修改其他用户的资料和角色。

## 后端业务逻辑设计

在 `UserService.java` 中新增 3 个方法：

### 1. `updateProfile(Long userId, User updated)`
- 根据 `userId` 查找用户，仅更新昵称、手机号、邮箱
- 不更新用户名、密码、角色
- 返回更新后的用户

### 2. `changePassword(Long userId, String oldPassword, String newPassword)`
- 根据 `userId` 查找用户
- 验证 `oldPassword` 是否与当前密码匹配，不匹配则抛出异常
- 校验 `newPassword` 长度不少于 6 位
- 更新密码并保存

### 3. `resetPassword(Long userId, String newPassword)`
- 根据 `userId` 查找用户
- 校验 `newPassword` 长度不少于 6 位
- 直接重置密码（无需旧密码，仅管理员调用）
- 返回重置后的用户

`UserRepository` 无需改动，现有 `findById` + `save` 已满足需求。

## 前端设计

在 `admin.html` 现有标签页体系中，**新增"个人中心"标签页**，所有登录用户均可见。

### 个人中心标签页布局

分为上下两个区块：

**上半区 — 基本信息**
- 只读字段（文本展示）：用户名、角色（管理员/普通用户）、创建时间
- 可编辑表单：
  - 昵称（input）
  - 手机号（input）
  - 邮箱（input）
  - **保存按钮** — 调用 `PUT /api/users/me`

**下半区 — 修改密码**
- 表单字段：
  - 旧密码（password input）
  - 新密码（password input）
  - 确认新密码（password input）
- **修改按钮** — 调用 `POST /api/users/me/password`
- 前端校验：新密码与确认密码必须一致，且不少于 6 位

### 用户管理列表增强（ADMIN 专属）

在用户管理表格的"操作"列中，现有"编辑"和"删除"按钮旁，**新增"重置密码"按钮**：
- 点击后弹出模态框，内含：
  - 新密码输入框
  - 确认新密码输入框
- **确认按钮** — 调用 `POST /api/users/{id}/reset-password`
- 前端校验：两次输入一致，且不少于 6 位

### 标签页渲染逻辑调整

`renderTabsByRole()` 函数调整：
- 所有登录用户：显示"商品管理"、"订单管理"、"分类管理"、"**个人中心**"
- ADMIN 额外显示："用户管理"

## 安全与权限设计

### 接口权限矩阵

| 端点 | 最低权限 | 额外校验 |
|------|---------|---------|
| `PUT /api/users/me` | 登录用户 | 只能修改自己的资料 |
| `POST /api/users/me/password` | 登录用户 | 必须提供正确的旧密码 |
| `POST /api/users/{id}/reset-password` | ADMIN | 无旧密码校验，直接重置 |

### 密码策略
- 新密码长度不少于 **6 位**
- 旧密码错误时返回 `400 BAD_REQUEST`，错误码 `PASSWORD_ERROR`
- 密码明文传输（当前项目无 HTTPS，后续如需增强可加盐哈希）

### Session 一致性
- `PUT /api/users/me` 更新成功后，需要同步更新 `session.setAttribute("currentUser", updatedUser)`
- 否则前端 `currentUser` 缓存与 Session 不一致，页面刷新后显示旧数据

### 前端安全
- 所有请求保持 `credentials: 'include'`
- 密码输入框使用 `type="password"` 隐藏输入
- 前端校验新密码与确认密码一致性，减少无效请求

### 错误处理
- 统一返回格式：`{ error: "ERROR_CODE", message: "描述" }`
- 新增错误码：
  - `PASSWORD_ERROR` — 旧密码错误
  - `PASSWORD_MISMATCH` — 新密码与确认密码不一致（前端主要校验）
  - `PASSWORD_TOO_SHORT` — 密码少于 6 位
