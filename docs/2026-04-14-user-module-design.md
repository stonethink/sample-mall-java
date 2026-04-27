# 用户模块设计文档

## 1. 概述

### 1.1 背景

当前 Sample Mall 项目已有商品、分类、订单三个模块，但缺乏用户体系。订单数据无法关联到具体用户，后台也没有用户管理能力。为了完善商城核心功能闭环，需要增加完整的用户模块。

### 1.2 目标

- 提供用户注册、登录、登出功能
- 提供后台用户管理（增删改查）
- 订单关联到具体用户
- 基于角色的权限控制（USER / ADMIN）

### 1.3 范围

本变更涉及：
- 后端：新增 `user` 包及用户分层代码，扩展 `Order` 实体
- 前端：`admin.html` 增加登录流程、用户管理标签页、角色权限控制
- 配置：`application.yml` Session 超时配置
- 数据：新增 `users.json` 初始数据

## 2. 数据模型设计

### 2.1 User 实体

```java
package com.example.mall.user;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;      // 唯一，登录账号
    private String password;      // 登录密码（本项目为示例，明文存储）
    private String nickname;      // 显示昵称
    private String phone;         // 手机号
    private String email;         // 邮箱
    private UserRole role;        // 角色
    private LocalDateTime createdAt;
    // getters / setters / constructors
}
```

**字段约束：**
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | 自增主键 |
| username | String | 唯一，非空，用于登录 |
| password | String | 非空，最小6位 |
| nickname | String | 可空，默认使用 username |
| phone | String | 可空 |
| email | String | 可空 |
| role | UserRole | 非空，默认 USER |
| createdAt | LocalDateTime | 自动填充 |

### 2.2 UserRole 枚举

```java
package com.example.mall.user;

public enum UserRole {
    USER("普通用户"),
    ADMIN("管理员");

    private final String description;
    // constructor / getter
}
```

### 2.3 Order 实体扩展

在现有 `Order` 中新增字段：

```java
private Long userId;  // 关联的用户ID，null 表示未关联（兼容历史数据）
```

## 3. 后端架构设计

### 3.1 模块结构

遵循现有模块分层模式：

```
com.example.mall.user
├── User.java              // 实体
├── UserRole.java          // 角色枚举
├── UserRepository.java    // 内存存储（ConcurrentHashMap）
├── UserService.java       // 业务逻辑
└── UserController.java    // REST API
```

### 3.2 UserRepository

- 使用 `ConcurrentHashMap<Long, User>` 作为内存存储
- 使用 `AtomicLong` 自增 ID
- 从 `users.json` 加载初始数据
- 提供 `findAll()`、`findById()`、`findByUsername()`、`save()`、`deleteById()` 方法

### 3.3 UserService

| 方法 | 说明 |
|------|------|
| `register(User user)` | 注册，校验用户名唯一，密码最小6位，默认 role=USER |
| `login(String username, String password)` | 登录验证，返回 User |
| `findById(Long id)` | 按ID查询 |
| `findAll()` | 查询全部 |
| `update(Long id, User updated)` | 更新用户信息 |
| `delete(Long id)` | 删除用户 |

### 3.4 UserController API

| 方法 | 端点 | 请求体 | 响应 | 权限 |
|------|------|--------|------|------|
| POST | `/api/users/register` | `{username, password, nickname?, phone?, email?}` | User（不含password） | 公开 |
| POST | `/api/users/login` | `{username, password}` | User（不含password）+ 创建Session | 公开 |
| POST | `/api/users/logout` | - | 200 OK + 销毁Session | 登录用户 |
| GET | `/api/users/me` | - | User（不含password） | 登录用户 |
| GET | `/api/users` | - | List&lt;User&gt; | ADMIN |
| GET | `/api/users/{id}` | - | User | ADMIN |
| PUT | `/api/users/{id}` | `{nickname?, phone?, email?, role?}` | User | ADMIN |
| DELETE | `/api/users/{id}` | - | 204 No Content | ADMIN |

### 3.5 Session 认证机制

采用 Spring `HttpSession` + Cookie（JSESSIONID）：

1. **登录**：验证成功后，`session.setAttribute("currentUser", user)`
2. **后续请求**：浏览器自动携带 JSESSIONID Cookie
3. **获取当前用户**：`session.getAttribute("currentUser")`
4. **登出**：`session.invalidate()`

**Session 配置（application.yml）：**
```yaml
server:
  servlet:
    session:
      timeout: 30m
```

### 3.6 订单关联用户

**OrderController.create() 修改：**
- 从 Session 获取当前登录用户
- 将 `userId` 设置到新建订单中
- 未登录时 `userId` 为 `null`（兼容历史数据）

**OrderController.listAll() 权限控制：**
- `ADMIN`：返回所有订单
- `USER`：仅返回 `userId == currentUser.id` 的订单
- 未登录：返回空列表或 401

## 4. 前端交互设计

### 4.1 登录流程

页面加载时调用 `GET /api/users/me`：
- 返回 200：已登录，显示管理界面
- 返回 401：未登录，显示登录模态框

登录模态框：
- 输入用户名、密码
- 调用 `POST /api/users/login`
- 成功：关闭模态框，加载管理界面
- 失败：提示错误信息

### 4.2 顶部用户信息

header 区域右侧显示：
```
当前用户: admin (管理员)  [退出登录]
```

点击"退出登录"调用 `POST /api/users/logout`，成功后刷新页面回到登录态。

### 4.3 标签页权限控制

| 标签页 | ADMIN | USER |
|--------|-------|------|
| 商品管理 | ✅ | ✅ |
| 分类管理 | ✅ | ✅ |
| 订单管理 | ✅ 全部订单 | ✅ 仅自己的订单 |
| 用户管理 | ✅ | ❌ 隐藏 |

**实现方式：** 前端根据 `/api/users/me` 返回的 `role` 动态渲染标签页。

### 4.4 用户管理标签页

与现有管理模式保持一致：
- 表格展示：ID、用户名、昵称、手机号、邮箱、角色、注册时间、操作
- 操作：编辑、删除
- 新增用户按钮 + 模态框
- 角色列使用 badge 标签区分（ADMIN 蓝色 / USER 灰色）

### 4.5 订单列表增强

订单表格增加"下单用户"列：
- ADMIN：显示用户昵称
- USER：不显示此列（因为自己看自己的）

## 5. 安全与错误处理

### 5.1 密码策略
- 最小长度 6 位
- 注册时校验用户名唯一性
- 响应中绝不返回 password 字段

### 5.2 权限控制

ADMIN 专属接口统一检查：
```java
User currentUser = (User) session.getAttribute("currentUser");
if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(...);
}
```

### 5.3 错误响应格式

延续现有项目风格：

**未认证：**
```json
{
  "error": "UNAUTHORIZED",
  "message": "请先登录"
}
```

**权限不足：**
```json
{
  "error": "FORBIDDEN",
  "message": "权限不足，需要管理员角色"
}
```

**用户名已存在：**
```json
{
  "error": "USERNAME_EXISTS",
  "message": "用户名已被注册"
}
```

## 6. 初始数据

### 6.1 users.json

```json
[
  {
    "id": 1,
    "username": "admin",
    "password": "admin",
    "nickname": "管理员",
    "phone": "13800138000",
    "email": "admin@mall.com",
    "role": "ADMIN",
    "createdAt": "2026-04-01T10:00:00"
  },
  {
    "id": 2,
    "username": "user1",
    "password": "123456",
    "nickname": "测试用户",
    "phone": "13900139000",
    "email": "user1@mall.com",
    "role": "USER",
    "createdAt": "2026-04-01T10:00:00"
  }
]
```

## 7. 受影响文件清单

### 7.1 新增文件
- `src/main/java/com/example/mall/user/User.java`
- `src/main/java/com/example/mall/user/UserRole.java`
- `src/main/java/com/example/mall/user/UserRepository.java`
- `src/main/java/com/example/mall/user/UserService.java`
- `src/main/java/com/example/mall/user/UserController.java`
- `src/main/resources/users.json`
- `docs/2026-04-14-user-module-design.md`

### 7.2 修改文件
- `src/main/java/com/example/mall/order/Order.java` — 新增 userId 字段
- `src/main/java/com/example/mall/order/OrderController.java` — 关联用户 + 权限过滤
- `src/main/java/com/example/mall/order/OrderService.java` — 适配 userId
- `src/main/resources/application.yml` — Session 超时配置
- `src/main/resources/static/admin.html` — 登录 + 用户管理 + 权限控制

## 8. 兼容性说明

- **历史订单**：`userId` 为 `null`，ADMIN 仍可正常查看
- **公开 API**：注册、登录接口无需认证
- **Swagger 文档**：自动包含新增接口（Springfox 扫描 `com.example.mall` 包）
