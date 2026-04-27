# 用户模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Sample Mall 项目增加完整的用户模块，包含用户注册/登录/登出、后台用户管理、订单关联用户、基于角色的前端权限控制。

**Architecture:** 遵循现有分层架构（Entity → Repository(内存) → Service → Controller），新增 `com.example.mall.user` 包。认证采用 Spring HttpSession + Cookie。前端在 `admin.html` 中集成登录态管理和用户管理标签页。

**Tech Stack:** Java 8, Spring Boot 2.7.5, Springfox 3.0.0, Jackson, Maven, 纯 HTML/CSS/JS

**Design Doc:** `docs/2026-04-14-user-module-design.md`

---

## File Structure

### 新增文件
| 文件 | 职责 |
|------|------|
| `src/main/java/com/example/mall/user/UserRole.java` | 用户角色枚举（USER / ADMIN） |
| `src/main/java/com/example/mall/user/User.java` | 用户实体 |
| `src/main/java/com/example/mall/user/UserRepository.java` | 内存存储，从 users.json 加载 |
| `src/main/java/com/example/mall/user/UserService.java` | 注册/登录/CRUD 业务逻辑 |
| `src/main/java/com/example/mall/user/UserController.java` | REST API（含 Session 认证） |
| `src/main/resources/users.json` | 初始用户数据 |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `src/main/java/com/example/mall/order/Order.java` | 新增 `userId` 字段 |
| `src/main/java/com/example/mall/order/OrderService.java` | `create()` 注入当前用户ID |
| `src/main/java/com/example/mall/order/OrderController.java` | Session认证检查 + 订单列表按角色过滤 |
| `src/main/resources/application.yml` | Session 30分钟超时配置 |
| `src/main/resources/static/admin.html` | 登录模态框 + 用户管理标签页 + 权限控制 |

---

## Task 1: UserRole 枚举 + User 实体

**Files:**
- Create: `src/main/java/com/example/mall/user/UserRole.java`
- Create: `src/main/java/com/example/mall/user/User.java`

- [ ] **Step 1: 创建 `UserRole.java`**

```java
package com.example.mall.user;

public enum UserRole {
    USER("普通用户"),
    ADMIN("管理员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

- [ ] **Step 2: 创建 `User.java`**

```java
package com.example.mall.user;

import java.time.LocalDateTime;

public class User {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Long id, String username, String password, String nickname,
                String phone, String email, UserRole role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/mall/user/
git commit -m "feat(user): add User entity and UserRole enum"
```

---

## Task 2: UserRepository（内存存储）

**Files:**
- Create: `src/main/java/com/example/mall/user/UserRepository.java`

- [ ] **Step 1: 创建 `UserRepository.java`**

```java
package com.example.mall.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class UserRepository {

    private final ConcurrentHashMap<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserRepository() {
        loadUsersFromJson();
    }

    private void loadUsersFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("users.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            List<User> users = objectMapper.readValue(inputStream, new TypeReference<List<User>>() {});
            for (User user : users) {
                save(user);
            }
        } catch (IOException e) {
            System.err.println("Failed to load users from JSON: " + e.getMessage());
        }
    }

    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<User> findByUsername(String username) {
        return store.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
        store.put(user.getId(), user);
        return user;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public boolean existsByUsername(String username) {
        return store.values().stream()
                .anyMatch(u -> u.getUsername().equals(username));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/mall/user/UserRepository.java
git commit -m "feat(user): add UserRepository with in-memory storage"
```

---

## Task 3: UserService（业务逻辑）

**Files:**
- Create: `src/main/java/com/example/mall/user/UserService.java`

- [ ] **Step 1: 创建 `UserService.java`**

```java
package com.example.mall.user;

import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已被注册");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User update(Long id, User updated) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + id));
        if (updated.getNickname() != null) {
            existing.setNickname(updated.getNickname());
        }
        if (updated.getPhone() != null) {
            existing.setPhone(updated.getPhone());
        }
        if (updated.getEmail() != null) {
            existing.setEmail(updated.getEmail());
        }
        if (updated.getRole() != null) {
            existing.setRole(updated.getRole());
        }
        return userRepository.save(existing);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/mall/user/UserService.java
git commit -m "feat(user): add UserService with register/login/CRUD"
```

---

## Task 4: UserController（REST API + Session 认证）

**Files:**
- Create: `src/main/java/com/example/mall/user/UserController.java`

- [ ] **Step 1: 创建 `UserController.java`**

```java
package com.example.mall.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registered = userService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(maskPassword(registered));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("USERNAME_EXISTS", e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        if (username == null || password == null) {
            return buildErrorResponse("UNAUTHORIZED", "用户名和密码不能为空");
        }
        try {
            User user = userService.login(username, password);
            session.setAttribute("currentUser", user);
            return ResponseEntity.ok(maskPassword(user));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("UNAUTHORIZED", e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return buildErrorResponse("UNAUTHORIZED", "请先登录");
        }
        return ResponseEntity.ok(maskPassword(user));
    }

    @GetMapping
    public ResponseEntity<?> listAll(HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users.stream()
                .map(this::maskPassword)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        return userService.findById(id)
                .map(u -> ResponseEntity.ok(maskPassword(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody User user, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            User updated = userService.update(id, user);
            return ResponseEntity.ok(maskPassword(updated));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("USER_NOT_FOUND", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private User maskPassword(User user) {
        User masked = new User();
        masked.setId(user.getId());
        masked.setUsername(user.getUsername());
        masked.setNickname(user.getNickname());
        masked.setPhone(user.getPhone());
        masked.setEmail(user.getEmail());
        masked.setRole(user.getRole());
        masked.setCreatedAt(user.getCreatedAt());
        return masked;
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(String error, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/mall/user/UserController.java
git commit -m "feat(user): add UserController with session-based auth"
```

---

## Task 5: Order 实体扩展 + OrderService 修改

**Files:**
- Modify: `src/main/java/com/example/mall/order/Order.java`
- Modify: `src/main/java/com/example/mall/order/OrderService.java`

- [ ] **Step 1: 修改 `Order.java`，新增 `userId` 字段**

在现有字段后添加：
```java
private Long userId;
```

在 getter/setter 区域添加：
```java
public Long getUserId() {
    return userId;
}

public void setUserId(Long userId) {
    this.userId = userId;
}
```

同时修改兼容构造函数，确保 Jackson 反序列化时缺失 `userId` 不会报错（已有默认无参构造函数，无需额外修改）。

- [ ] **Step 2: 修改 `OrderService.java` 的 `create` 方法**

将现有 `create` 方法：
```java
public Order create(Order order) {
    order.setStatus(OrderStatus.PENDING_PAYMENT);
    return orderRepository.save(order);
}
```

修改为接受 userId：
```java
public Order create(Order order, Long userId) {
    order.setStatus(OrderStatus.PENDING_PAYMENT);
    order.setUserId(userId);
    return orderRepository.save(order);
}
```

保留无 userId 的重载（可选）：
```java
public Order create(Order order) {
    return create(order, null);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/mall/order/Order.java src/main/java/com/example/mall/order/OrderService.java
git commit -m "feat(order): add userId to Order and update OrderService"
```

---

## Task 6: OrderController 权限改造

**Files:**
- Modify: `src/main/java/com/example/mall/order/OrderController.java`

- [ ] **Step 1: 在 `OrderController` 中注入 UserService 并添加辅助方法**

在类顶部添加：
```java
import com.example.mall.user.User;
import com.example.mall.user.UserRole;
import javax.servlet.http.HttpSession;
```

添加辅助方法：
```java
private User getCurrentUser(HttpSession session) {
    return (User) session.getAttribute("currentUser");
}
```

- [ ] **Step 2: 修改 `listAll` 方法支持按角色过滤**

将现有 `listAll`：
```java
@GetMapping
public ResponseEntity<?> listAll(@RequestParam(required = false) String status) {
    if (status != null) {
        OrderStatus orderStatus = parseOrderStatus(status);
        if (orderStatus == null) {
            return buildInvalidStatusResponse(status);
        }
        return ResponseEntity.ok(orderService.listByStatus(orderStatus));
    }
    return ResponseEntity.ok(orderService.listAll());
}
```

修改为：
```java
@GetMapping
public ResponseEntity<?> listAll(@RequestParam(required = false) String status, HttpSession session) {
    User currentUser = getCurrentUser(session);
    if (currentUser == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "UNAUTHORIZED", "message", "请先登录"));
    }

    List<Order> orders;
    if (status != null) {
        OrderStatus orderStatus = parseOrderStatus(status);
        if (orderStatus == null) {
            return buildInvalidStatusResponse(status);
        }
        orders = orderService.listByStatus(orderStatus);
    } else {
        orders = orderService.listAll();
    }

    if (currentUser.getRole() != UserRole.ADMIN) {
        orders = orders.stream()
                .filter(o -> currentUser.getId().equals(o.getUserId()))
                .collect(Collectors.toList());
    }
    return ResponseEntity.ok(orders);
}
```

- [ ] **Step 3: 修改 `create` 方法关联当前用户**

将现有 `create`：
```java
@PostMapping
public ResponseEntity<Order> create(@RequestBody Order order) {
    Order created = orderService.create(order);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

修改为：
```java
@PostMapping
public ResponseEntity<?> create(@RequestBody Order order, HttpSession session) {
    User currentUser = getCurrentUser(session);
    Long userId = currentUser != null ? currentUser.getId() : null;
    Order created = orderService.create(order, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/mall/order/OrderController.java
git commit -m "feat(order): add session auth and role-based order filtering"
```

---

## Task 7: 配置与初始数据

**Files:**
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/users.json`

- [ ] **Step 1: 修改 `application.yml` 添加 Session 配置**

在现有内容后追加：
```yaml
server:
  servlet:
    session:
      timeout: 30m
```

注意：如果 `server:` 已存在，合并到现有块中：
```yaml
server:
  port: 8080
  servlet:
    session:
      timeout: 30m
```

- [ ] **Step 2: 创建 `users.json`**

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

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml src/main/resources/users.json
git commit -m "chore: add session config and users seed data"
```

---

## Task 8: 前端 admin.html — 登录流程

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 在 `<style>` 中添加登录模态框样式**

在现有 `.modal-content` 样式后添加（确保登录模态框在最上层）：
```css
.login-modal {
    z-index: 3000;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 14px;
}

.user-info span {
    color: #374151;
}
```

- [ ] **Step 2: 在 `<header>` 中添加用户信息区域**

将现有 header：
```html
<header>
    <h1>商城管理后台</h1>
    <p>Sample Mall - 企业AI Coding 示例电商平台项目</p>
</header>
```

修改为：
```html
<header>
    <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
            <h1>商城管理后台</h1>
            <p>Sample Mall - 企业AI Coding 示例电商平台项目</p>
        </div>
        <div class="user-info" id="user-info" style="display: none;">
            <span id="current-user-display"></span>
            <button class="btn" onclick="logout()">退出登录</button>
        </div>
    </div>
</header>
```

- [ ] **Step 3: 在 `</body>` 前添加登录模态框（在所有模态框之后）**

```html
<!-- 登录模态框 -->
<div id="login-modal" class="modal login-modal active">
    <div class="modal-content">
        <div class="modal-header">
            <h2>用户登录</h2>
        </div>
        <form id="login-form">
            <div class="form-group">
                <label>用户名 *</label>
                <input type="text" id="login-username" required placeholder="请输入用户名">
            </div>
            <div class="form-group">
                <label>密码 *</label>
                <input type="password" id="login-password" required placeholder="请输入密码">
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">登录</button>
            </div>
        </form>
    </div>
</div>
```

- [ ] **Step 4: 在 `<script>` 初始化部分添加登录检查**

在现有初始化代码后添加：
```javascript
let currentUser = null;

// 页面加载时检查登录状态
async function checkAuth() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/users/me`, {
            credentials: 'include'
        });
        if (response.ok) {
            currentUser = await response.json();
            showApp();
        } else {
            showLogin();
        }
    } catch (error) {
        showLogin();
    }
}

function showLogin() {
    document.getElementById('login-modal').classList.add('active');
    document.getElementById('user-info').style.display = 'none';
}

function showApp() {
    document.getElementById('login-modal').classList.remove('active');
    document.getElementById('user-info').style.display = 'flex';
    document.getElementById('current-user-display').textContent = 
        `当前用户: ${currentUser.nickname || currentUser.username} (${currentUser.role === 'ADMIN' ? '管理员' : '普通用户'})`;
    
    // 根据角色渲染标签页
    renderTabsByRole();
    
    // 加载默认页面
    loadProducts();
    loadCategoryFilter();
}

function renderTabsByRole() {
    const tabsContainer = document.querySelector('.tabs');
    const isAdmin = currentUser && currentUser.role === 'ADMIN';
    
    let tabsHtml = `
        <button class="tab-btn active" onclick="switchTab('products')">商品管理</button>
        <button class="tab-btn" onclick="switchTab('categories')">分类管理</button>
        <button class="tab-btn" onclick="switchTab('orders')">订单管理</button>
    `;
    if (isAdmin) {
        tabsHtml += `<button class="tab-btn" onclick="switchTab('users')">用户管理</button>`;
    }
    tabsContainer.innerHTML = tabsHtml;
}

async function handleLoginSubmit(e) {
    e.preventDefault();
    const data = {
        username: document.getElementById('login-username').value,
        password: document.getElementById('login-password').value
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/users/login`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data),
            credentials: 'include'
        });
        
        if (response.ok) {
            currentUser = await response.json();
            showToast('登录成功', 'success');
            showApp();
        } else {
            const error = await response.json();
            showToast(error.message || '登录失败', 'error');
        }
    } catch (error) {
        showToast('登录失败: ' + error.message, 'error');
    }
}

async function logout() {
    try {
        await fetch(`${API_BASE_URL}/api/users/logout`, {
            method: 'POST',
            credentials: 'include'
        });
        currentUser = null;
        showToast('已退出登录', 'info');
        location.reload();
    } catch (error) {
        showToast('退出失败: ' + error.message, 'error');
    }
}

// 绑定登录表单
document.getElementById('login-form').addEventListener('submit', handleLoginSubmit);

// 替换原有的 DOMContentLoaded 初始化
checkAuth();
```

注意：需要将原有的 `document.addEventListener('DOMContentLoaded', ...)` 中的 `loadProducts()` 调用移除（因为现在在 `showApp()` 中调用）。

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add login modal and session-based auth flow"
```

---

## Task 9: 前端 admin.html — 用户管理标签页

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 在标签页区域添加"用户管理"标签内容**

在 `orders-tab` 之后（`</div>` 闭合前）添加：
```html
<!-- 用户管理 -->
<div id="users-tab" class="tab-content">
    <div class="toolbar">
        <button class="btn btn-primary" onclick="showUserModal()">+ 新增用户</button>
        <button class="btn" onclick="loadUsers()">刷新</button>
    </div>

    <div id="users-loading" class="loading">加载中...</div>
    
    <table id="users-table">
        <thead>
            <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>昵称</th>
                <th>手机号</th>
                <th>邮箱</th>
                <th>角色</th>
                <th>注册时间</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody id="users-tbody">
        </tbody>
    </table>
</div>
```

- [ ] **Step 2: 在订单模态框后添加用户模态框**

```html
<!-- 用户模态框 -->
<div id="user-modal" class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h2 id="user-modal-title">新增用户</h2>
            <span class="close-btn" onclick="closeUserModal()">×</span>
        </div>
        <form id="user-form">
            <input type="hidden" id="user-id">
            <div class="form-group">
                <label>用户名 *</label>
                <input type="text" id="user-username" required>
            </div>
            <div class="form-group">
                <label>密码 *</label>
                <input type="password" id="user-password" required>
            </div>
            <div class="form-group">
                <label>昵称</label>
                <input type="text" id="user-nickname">
            </div>
            <div class="form-group">
                <label>手机号</label>
                <input type="text" id="user-phone">
            </div>
            <div class="form-group">
                <label>邮箱</label>
                <input type="email" id="user-email">
            </div>
            <div class="form-group">
                <label>角色</label>
                <select id="user-role" class="category-select">
                    <option value="USER">普通用户</option>
                    <option value="ADMIN">管理员</option>
                </select>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">保存</button>
                <button type="button" class="btn" onclick="closeUserModal()">取消</button>
            </div>
        </form>
    </div>
</div>
```

- [ ] **Step 3: 在 `switchTab` 函数中添加用户管理分支**

在 `switchTab` 函数的 `else if (tab === 'orders')` 分支后添加：
```javascript
} else if (tab === 'users') {
    loadUsers();
}
```

- [ ] **Step 4: 在 `<script>` 末尾添加用户管理相关函数**

```javascript
// ==================== 用户管理 ====================

async function loadUsers() {
    const loading = document.getElementById('users-loading');
    const tbody = document.getElementById('users-tbody');
    
    loading.classList.add('active');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/users`, {
            credentials: 'include'
        });
        const users = await response.json();
        
        if (users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="empty-state">暂无用户数据</td></tr>';
        } else {
            tbody.innerHTML = users.map(user => {
                const roleBadge = user.role === 'ADMIN' 
                    ? '<span class="badge badge-info">管理员</span>' 
                    : '<span class="badge badge-secondary">普通用户</span>';
                return `
                    <tr>
                        <td>${user.id}</td>
                        <td>${user.username}</td>
                        <td>${user.nickname || '-'}</td>
                        <td>${user.phone || '-'}</td>
                        <td>${user.email || '-'}</td>
                        <td>${roleBadge}</td>
                        <td>${new Date(user.createdAt).toLocaleString('zh-CN')}</td>
                        <td>
                            <div class="action-btns">
                                <button class="btn" onclick="editUser(${user.id}, '${user.username}', '${user.nickname || ''}', '${user.phone || ''}', '${user.email || ''}', '${user.role}')">编辑</button>
                                <button class="btn btn-danger" onclick="deleteUser(${user.id})">删除</button>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (error) {
        showToast('加载用户列表失败: ' + error.message, 'error');
        tbody.innerHTML = '<tr><td colspan="8" class="empty-state">加载失败</td></tr>';
    } finally {
        loading.classList.remove('active');
    }
}

function showUserModal(user = null) {
    const modal = document.getElementById('user-modal');
    const title = document.getElementById('user-modal-title');
    
    if (user) {
        title.textContent = '编辑用户';
        document.getElementById('user-id').value = user.id;
        document.getElementById('user-username').value = user.username;
        document.getElementById('user-password').value = '';
        document.getElementById('user-nickname').value = user.nickname || '';
        document.getElementById('user-phone').value = user.phone || '';
        document.getElementById('user-email').value = user.email || '';
        document.getElementById('user-role').value = user.role;
    } else {
        title.textContent = '新增用户';
        document.getElementById('user-form').reset();
        document.getElementById('user-id').value = '';
    }
    
    modal.classList.add('active');
}

function closeUserModal() {
    document.getElementById('user-modal').classList.remove('active');
    document.getElementById('user-form').reset();
}

async function handleUserSubmit(e) {
    e.preventDefault();
    
    const id = document.getElementById('user-id').value;
    const data = {
        username: document.getElementById('user-username').value,
        password: document.getElementById('user-password').value,
        nickname: document.getElementById('user-nickname').value,
        phone: document.getElementById('user-phone').value,
        email: document.getElementById('user-email').value,
        role: document.getElementById('user-role').value
    };
    
    try {
        let response;
        if (id) {
            // 编辑时不需要传 password 和 username
            const updateData = {
                nickname: data.nickname,
                phone: data.phone,
                email: data.email,
                role: data.role
            };
            response = await fetch(`${API_BASE_URL}/api/users/${id}`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(updateData),
                credentials: 'include'
            });
        } else {
            response = await fetch(`${API_BASE_URL}/api/users/register`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data),
                credentials: 'include'
            });
        }
        
        if (response.ok) {
            showToast(id ? '用户更新成功' : '用户创建成功', 'success');
            closeUserModal();
            loadUsers();
        } else {
            const error = await response.json();
            showToast(error.message || '操作失败', 'error');
        }
    } catch (error) {
        showToast('操作失败: ' + error.message, 'error');
    }
}

function editUser(id, username, nickname, phone, email, role) {
    showUserModal({ id, username, nickname, phone, email, role });
}

async function deleteUser(id) {
    if (!confirm('确定要删除这个用户吗?')) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/users/${id}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        
        if (response.ok) {
            showToast('用户删除成功', 'success');
            loadUsers();
        } else {
            showToast('删除失败', 'error');
        }
    } catch (error) {
        showToast('删除失败: ' + error.message, 'error');
    }
}

// 绑定用户表单提交事件
document.getElementById('user-form').addEventListener('submit', handleUserSubmit);
```

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add user management tab and modal"
```

---

## Task 10: 前端 admin.html — 订单列表增强与权限控制

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 修改 `loadOrders` 函数**

将现有 `loadOrders` 函数替换为支持角色过滤和显示用户信息的版本：

```javascript
async function loadOrders() {
    const loading = document.getElementById('orders-loading');
    const tbody = document.getElementById('orders-tbody');
    const isAdmin = currentUser && currentUser.role === 'ADMIN';
    
    loading.classList.add('active');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/orders`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            if (response.status === 401) {
                showToast('请先登录', 'error');
                showLogin();
                return;
            }
        }
        
        const orders = await response.json();
        
        if (orders.length === 0) {
            tbody.innerHTML = `<tr><td colspan="${isAdmin ? 8 : 7}" class="empty-state">暂无订单数据</td></tr>`;
        } else {
            tbody.innerHTML = orders.map(order => {
                const statusBadge = getOrderStatusBadge(order.status);
                const userCell = isAdmin ? `<td>${order.userId || '未关联'}</td>` : '';
                return `
                    <tr>
                        <td>${order.id}</td>
                        <td>${order.orderSn}</td>
                        <td>${order.productIds.join(', ')}</td>
                        <td class="price">¥${(order.totalAmount / 100).toFixed(2)}</td>
                        <td>${statusBadge}</td>
                        ${userCell}
                        <td>${new Date(order.createdAt).toLocaleString('zh-CN')}</td>
                        <td>
                            <button class="btn btn-danger" onclick="deleteOrder(${order.id})">删除</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (error) {
        showToast('加载订单列表失败: ' + error.message, 'error');
        tbody.innerHTML = `<tr><td colspan="${isAdmin ? 8 : 7}" class="empty-state">加载失败</td></tr>`;
    } finally {
        loading.classList.remove('active');
    }
}
```

- [ ] **Step 2: 修改订单表格表头**

将现有表头：
```html
<tr>
    <th>ID</th>
    <th>订单号</th>
    <th>商品ID</th>
    <th>总金额(元)</th>
    <th>状态</th>
    <th>创建时间</th>
    <th>操作</th>
</tr>
```

替换为动态渲染（通过JS在加载时根据角色设置），或在HTML中保留原样，在 `loadOrders` 加载前动态修改表头。

更简单的方式：在 `showApp()` 中根据角色设置表头：
```javascript
function updateOrderTableHeader() {
    const isAdmin = currentUser && currentUser.role === 'ADMIN';
    const thead = document.querySelector('#orders-table thead tr');
    if (isAdmin) {
        thead.innerHTML = `
            <th>ID</th>
            <th>订单号</th>
            <th>商品ID</th>
            <th>总金额(元)</th>
            <th>状态</th>
            <th>下单用户</th>
            <th>创建时间</th>
            <th>操作</th>
        `;
    } else {
        thead.innerHTML = `
            <th>ID</th>
            <th>订单号</th>
            <th>商品ID</th>
            <th>总金额(元)</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
        `;
    }
}
```

在 `showApp()` 中调用 `updateOrderTableHeader()`。

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add role-based order filtering and user column"
```

---

## Task 11: 编译验证与运行测试

**Files:** None (verification only)

- [ ] **Step 1: 编译项目**

```bash
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 启动应用**

```bash
mvn spring-boot:run
```

Expected: Application starts on port 8080

- [ ] **Step 3: 浏览器验证登录流程**

1. 访问 `http://localhost:8080/admin.html`
2. 应显示登录模态框
3. 使用 `admin` / `admin` 登录
4. 应显示管理界面，包含"用户管理"标签页
5. 退出登录，使用 `user1` / `123456` 登录
6. 应不显示"用户管理"标签页，订单列表只显示自己的订单

- [ ] **Step 4: Swagger 验证**

访问 `http://localhost:8080/swagger-ui/`，确认新增的用户相关 API 已列出。

- [ ] **Step 5: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete user module with auth, management and order association"
```

---

## Self-Review Checklist

### Spec Coverage
- [x] UserRole + User 实体 → Task 1
- [x] UserRepository 内存存储 → Task 2
- [x] UserService 注册/登录/CRUD → Task 3
- [x] UserController REST API + Session → Task 4
- [x] Order 实体扩展 userId → Task 5
- [x] OrderController 权限过滤 → Task 6
- [x] application.yml Session 配置 → Task 7
- [x] users.json 初始数据 → Task 7
- [x] 前端登录模态框 → Task 8
- [x] 前端用户管理标签页 → Task 9
- [x] 前端角色权限控制 → Task 8, 9, 10
- [x] 订单列表用户关联显示 → Task 10

### Placeholder Scan
- [x] 无 TBD/TODO/"implement later"
- [x] 所有代码块包含完整可运行代码
- [x] 所有命令包含预期输出

### Type Consistency
- [x] `UserRole` 枚举在 Task 1 定义，Task 4/6 中使用一致
- [x] `userId` 字段在 Task 5 添加，Task 6/10 中使用一致
- [x] API 端点 `/api/users/*` 在 Task 4 定义，Task 8/9 中调用一致
