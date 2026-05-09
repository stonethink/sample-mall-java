# 个人中心功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为用户模块扩展个人中心功能，支持已登录用户修改个人资料与密码，管理员重置用户密码。

**Architecture:** 在现有 `UserService`/`UserController` 中新增个人中心相关方法，`admin.html` 新增个人中心标签页和用户管理重置密码按钮，保持现有内存存储架构不变。

**Tech Stack:** Spring Boot 2.7.5, Java 8, HTML/CSS/JS, Session + Cookie 认证

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/java/com/example/mall/user/UserService.java` | 修改 | 新增 `updateProfile`, `changePassword`, `resetPassword` 业务方法 |
| `src/main/java/com/example/mall/user/UserController.java` | 修改 | 新增 `PUT /api/users/me`, `POST /api/users/me/password`, `POST /api/users/{id}/reset-password` 端点 |
| `src/main/resources/static/admin.html` | 修改 | 新增个人中心标签页 HTML/CSS/JS，调整标签页渲染，用户管理增加重置密码按钮 |

---

### Task 1: UserService — 新增个人中心业务逻辑

**Files:**
- Modify: `src/main/java/com/example/mall/user/UserService.java`

- [ ] **Step 1: 在 `UserService` 中新增 `updateProfile` 方法**

在 `delete` 方法之后，添加以下内容：

```java
    public User updateProfile(Long userId, User updated) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + userId));
        if (updated.getNickname() != null) {
            existing.setNickname(updated.getNickname());
        }
        if (updated.getPhone() != null) {
            existing.setPhone(updated.getPhone());
        }
        if (updated.getEmail() != null) {
            existing.setEmail(updated.getEmail());
        }
        return userRepository.save(existing);
    }
```

- [ ] **Step 2: 在 `UserService` 中新增 `changePassword` 方法**

在 `updateProfile` 之后，添加以下内容：

```java
    public User changePassword(Long userId, String oldPassword, String newPassword) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + userId));
        if (!existing.getPassword().equals(oldPassword)) {
            throw new IllegalArgumentException("旧密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        existing.setPassword(newPassword);
        return userRepository.save(existing);
    }
```

- [ ] **Step 3: 在 `UserService` 中新增 `resetPassword` 方法**

在 `changePassword` 之后，添加以下内容：

```java
    public User resetPassword(Long userId, String newPassword) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在, id=" + userId));
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        existing.setPassword(newPassword);
        return userRepository.save(existing);
    }
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/mall/user/UserService.java
git commit -m "feat(user): add updateProfile, changePassword, resetPassword methods"
```

---

### Task 2: UserController — 新增个人中心 API 端点

**Files:**
- Modify: `src/main/java/com/example/mall/user/UserController.java`

- [ ] **Step 1: 在 `UserController` 中新增 `updateProfile` 端点**

在 `getCurrentUser` 方法之后、`listAll` 方法之前，添加以下内容：

```java
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody User user, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return buildErrorResponse("UNAUTHORIZED", "请先登录");
        }
        try {
            User updated = userService.updateProfile(currentUser.getId(), user);
            session.setAttribute("currentUser", updated);
            return ResponseEntity.ok(maskPassword(updated));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("USER_NOT_FOUND", e.getMessage());
        }
    }
```

- [ ] **Step 2: 在 `UserController` 中新增 `changePassword` 端点**

在 `updateProfile` 之后，添加以下内容：

```java
    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return buildErrorResponse("UNAUTHORIZED", "请先登录");
        }
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return buildErrorResponse("INVALID_REQUEST", "旧密码和新密码不能为空");
        }
        try {
            User updated = userService.changePassword(currentUser.getId(), oldPassword, newPassword);
            session.setAttribute("currentUser", updated);
            return ResponseEntity.ok(maskPassword(updated));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("PASSWORD_ERROR", e.getMessage());
        }
    }
```

- [ ] **Step 3: 在 `UserController` 中新增 `resetPassword` 端点**

在 `changePassword` 之后，添加以下内容：

```java
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        String newPassword = request.get("newPassword");
        if (newPassword == null) {
            return buildErrorResponse("INVALID_REQUEST", "新密码不能为空");
        }
        try {
            User updated = userService.resetPassword(id, newPassword);
            return ResponseEntity.ok(maskPassword(updated));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("USER_NOT_FOUND", e.getMessage());
        }
    }
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/mall/user/UserController.java
git commit -m "feat(user): add personal center API endpoints - updateProfile, changePassword, resetPassword"
```

---

### Task 3: admin.html — 新增个人中心标签页 HTML 结构

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 在标签导航区后新增"个人中心"标签页内容**

在 `<!-- 用户管理 -->` 所在的标签内容区域之后（大约在第 521 行附近），找到最后一个 `</div>` 闭合标签，在其后添加：

```html
    <!-- 个人中心 -->
    <div id="profile" class="tab-content">
        <h2>个人中心</h2>
        
        <div class="profile-section">
            <h3>基本信息</h3>
            <div class="profile-readonly">
                <p><strong>用户名：</strong><span id="profile-username"></span></p>
                <p><strong>角色：</strong><span id="profile-role"></span></p>
                <p><strong>创建时间：</strong><span id="profile-createdAt"></span></p>
            </div>
            <form id="profile-form">
                <div class="form-row">
                    <div class="form-group">
                        <label>昵称</label>
                        <input type="text" id="profile-nickname" name="nickname">
                    </div>
                    <div class="form-group">
                        <label>手机号</label>
                        <input type="text" id="profile-phone" name="phone">
                    </div>
                    <div class="form-group">
                        <label>邮箱</label>
                        <input type="email" id="profile-email" name="email">
                    </div>
                </div>
                <button type="submit" class="btn btn-primary">保存修改</button>
            </form>
        </div>
        
        <div class="profile-section">
            <h3>修改密码</h3>
            <form id="password-form">
                <div class="form-row">
                    <div class="form-group">
                        <label>旧密码</label>
                        <input type="password" id="password-old" name="oldPassword" required>
                    </div>
                    <div class="form-group">
                        <label>新密码</label>
                        <input type="password" id="password-new" name="newPassword" required minlength="6">
                    </div>
                    <div class="form-group">
                        <label>确认新密码</label>
                        <input type="password" id="password-confirm" name="confirmPassword" required minlength="6">
                    </div>
                </div>
                <button type="submit" class="btn btn-primary">修改密码</button>
            </form>
        </div>
    </div>
```

- [ ] **Step 2: 在 CSS 区域新增个人中心样式**

在 `<style>` 标签内（建议放在 `.login-modal` 样式之后），添加：

```css
        .profile-section {
            margin-bottom: 32px;
            padding-bottom: 24px;
            border-bottom: 1px solid #e5e7eb;
        }
        .profile-section:last-child {
            border-bottom: none;
            margin-bottom: 0;
        }
        .profile-section h3 {
            font-size: 18px;
            margin-bottom: 16px;
            color: #1a1a1a;
        }
        .profile-readonly {
            background: #f9fafb;
            padding: 16px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .profile-readonly p {
            margin-bottom: 8px;
            color: #4b5563;
        }
        .profile-readonly p:last-child {
            margin-bottom: 0;
        }
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add personal center tab HTML and CSS"
```

---

### Task 4: admin.html — 调整标签页渲染逻辑

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 修改 `renderTabsByRole` 函数，为所有登录用户添加"个人中心"标签**

将现有的 `renderTabsByRole` 函数替换为：

```javascript
        function renderTabsByRole() {
            const tabsContainer = document.querySelector('.tabs');
            const isAdmin = currentUser && currentUser.role === 'ADMIN';

            let tabsHtml = `
                <button class="tab-btn active" onclick="switchTab('products')">商品管理</button>
                <button class="tab-btn" onclick="switchTab('categories')">分类管理</button>
                <button class="tab-btn" onclick="switchTab('orders')">订单管理</button>
                <button class="tab-btn" onclick="switchTab('profile')">个人中心</button>
            `;
            if (isAdmin) {
                tabsHtml += `<button class="tab-btn" onclick="switchTab('users')">用户管理</button>`;
            }
            tabsContainer.innerHTML = tabsHtml;
        }
```

- [ ] **Step 2: 修改 `switchTab` 函数，在个人中心切换时加载数据**

在现有的 `switchTab` 函数中，找到处理各标签页的逻辑，在 `users` 分支之前添加 `profile` 分支：

```javascript
            } else if (tabName === 'profile') {
                loadProfile();
```

（具体插入位置：在 `users` 分支同级，如果 `switchTab` 使用 if-else if 链式结构，则在 `users` 的 else if 之前插入。）

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add profile tab to renderTabsByRole and switchTab"
```

---

### Task 5: admin.html — 新增个人中心 JavaScript 逻辑

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 在 `showApp()` 中绑定个人中心表单事件**

在 `showApp()` 函数内（第 751 行附近，在 `product-form` 和 `order-form` 的 `addEventListener` 之后），添加：

```javascript
            document.getElementById('profile-form').addEventListener('submit', handleProfileSubmit);
            document.getElementById('password-form').addEventListener('submit', handlePasswordSubmit);
```

- [ ] **Step 2: 在 `// ==================== 用户管理 ====================` 之前，添加个人中心函数**

```javascript
        // ==================== 个人中心 ====================

        async function loadProfile() {
            if (!currentUser) return;
            document.getElementById('profile-username').textContent = currentUser.username;
            document.getElementById('profile-role').textContent = currentUser.role === 'ADMIN' ? '管理员' : '普通用户';
            document.getElementById('profile-createdAt').textContent = new Date(currentUser.createdAt).toLocaleString('zh-CN');
            document.getElementById('profile-nickname').value = currentUser.nickname || '';
            document.getElementById('profile-phone').value = currentUser.phone || '';
            document.getElementById('profile-email').value = currentUser.email || '';
        }

        async function handleProfileSubmit(e) {
            e.preventDefault();
            const data = {
                nickname: document.getElementById('profile-nickname').value,
                phone: document.getElementById('profile-phone').value,
                email: document.getElementById('profile-email').value
            };
            try {
                const response = await fetch(`${API_BASE_URL}/api/users/me`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(data)
                });
                const result = await response.json();
                if (response.ok) {
                    currentUser = result;
                    document.getElementById('current-user-display').textContent =
                        `当前用户: ${currentUser.nickname || currentUser.username} (${currentUser.role === 'ADMIN' ? '管理员' : '普通用户'})`;
                    showToast('资料更新成功', 'success');
                } else {
                    showToast(result.message || '更新失败', 'error');
                }
            } catch (error) {
                showToast('更新失败: ' + error.message, 'error');
            }
        }

        async function handlePasswordSubmit(e) {
            e.preventDefault();
            const oldPassword = document.getElementById('password-old').value;
            const newPassword = document.getElementById('password-new').value;
            const confirmPassword = document.getElementById('password-confirm').value;
            
            if (newPassword !== confirmPassword) {
                showToast('新密码与确认密码不一致', 'error');
                return;
            }
            if (newPassword.length < 6) {
                showToast('新密码长度不能少于6位', 'error');
                return;
            }
            
            try {
                const response = await fetch(`${API_BASE_URL}/api/users/me/password`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ oldPassword, newPassword })
                });
                const result = await response.json();
                if (response.ok) {
                    showToast('密码修改成功', 'success');
                    document.getElementById('password-form').reset();
                } else {
                    showToast(result.message || '密码修改失败', 'error');
                }
            } catch (error) {
                showToast('密码修改失败: ' + error.message, 'error');
            }
        }
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add personal center JavaScript logic"
```

---

### Task 6: admin.html — 用户管理列表增加重置密码功能

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 在 `loadUsers()` 的用户列表操作中，新增"重置密码"按钮**

将现有的操作列按钮替换为包含"重置密码"的版本：

```javascript
                                    <div class="action-btns">
                                        <button class="btn" onclick="editUser(${user.id}, '${user.username}', '${user.nickname || ''}', '${user.phone || ''}', '${user.email || ''}', '${user.role}')">编辑</button>
                                        <button class="btn" onclick="showResetPasswordModal(${user.id}, '${user.username}')">重置密码</button>
                                        <button class="btn btn-danger" onclick="deleteUser(${user.id})">删除</button>
                                    </div>
```

- [ ] **Step 2: 在 HTML body 末尾（所有模态框之后），添加重置密码模态框**

```html
    <!-- 重置密码模态框 -->
    <div id="reset-password-modal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3 id="reset-password-title">重置密码</h3>
                <button class="modal-close" onclick="closeResetPasswordModal()">&times;</button>
            </div>
            <form id="reset-password-form">
                <input type="hidden" id="reset-password-user-id">
                <div class="form-group">
                    <label>新密码</label>
                    <input type="password" id="reset-password-new" required minlength="6">
                </div>
                <div class="form-group">
                    <label>确认新密码</label>
                    <input type="password" id="reset-password-confirm" required minlength="6">
                </div>
                <div class="form-actions">
                    <button type="button" class="btn" onclick="closeResetPasswordModal()">取消</button>
                    <button type="submit" class="btn btn-primary">确认重置</button>
                </div>
            </form>
        </div>
    </div>
```

- [ ] **Step 3: 在用户管理 JavaScript 区域末尾，添加重置密码相关函数**

```javascript
        function showResetPasswordModal(userId, username) {
            document.getElementById('reset-password-user-id').value = userId;
            document.getElementById('reset-password-title').textContent = `重置密码 - ${username}`;
            document.getElementById('reset-password-modal').classList.add('active');
            document.getElementById('reset-password-new').value = '';
            document.getElementById('reset-password-confirm').value = '';
        }

        function closeResetPasswordModal() {
            document.getElementById('reset-password-modal').classList.remove('active');
        }

        document.getElementById('reset-password-form').addEventListener('submit', async function(e) {
            e.preventDefault();
            const userId = document.getElementById('reset-password-user-id').value;
            const newPassword = document.getElementById('reset-password-new').value;
            const confirmPassword = document.getElementById('reset-password-confirm').value;
            
            if (newPassword !== confirmPassword) {
                showToast('两次输入的密码不一致', 'error');
                return;
            }
            if (newPassword.length < 6) {
                showToast('密码长度不能少于6位', 'error');
                return;
            }
            
            try {
                const response = await fetch(`${API_BASE_URL}/api/users/${userId}/reset-password`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ newPassword })
                });
                const result = await response.json();
                if (response.ok) {
                    showToast('密码重置成功', 'success');
                    closeResetPasswordModal();
                } else {
                    showToast(result.message || '重置失败', 'error');
                }
            } catch (error) {
                showToast('重置失败: ' + error.message, 'error');
            }
        });
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add admin reset-password button and modal in user management"
```

---

### Task 7: 编译验证与运行测试

- [ ] **Step 1: 编译项目**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 启动应用**

Run: `mvn spring-boot:run`
Expected: Started MallBackendApplication in X.XXX seconds

- [ ] **Step 3: 手动验证**

1. 打开浏览器访问 `http://localhost:8080/admin.html`
2. 使用 `user1` / `123456` 登录
3. 切换到"个人中心"标签页
4. 修改昵称和手机号，点击保存，确认资料更新成功
5. 修改密码（旧密码 `123456`，新密码 `1234567`），确认密码修改成功
6. 退出登录，使用新密码重新登录验证
7. 使用 `admin` / `admin` 登录
8. 进入"用户管理"，对 `user1` 点击"重置密码"
9. 输入新密码并确认，验证重置成功
10. 退出登录，使用重置后的密码以 `user1` 登录验证

- [ ] **Step 4: Commit**

```bash
git commit --allow-empty -m "test: personal center feature verified"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- ✅ `PUT /api/users/me` → Task 1 + Task 2
- ✅ `POST /api/users/me/password` → Task 1 + Task 2
- ✅ `POST /api/users/{id}/reset-password` → Task 1 + Task 2
- ✅ 个人中心标签页 → Task 3 + Task 4 + Task 5
- ✅ 用户管理重置密码 → Task 6
- ✅ 标签页渲染调整 → Task 4
- ✅ Session 一致性 → Task 2 (session.setAttribute)
- ✅ 前端校验 → Task 5 + Task 6

**2. Placeholder scan:**
- ✅ 无 TBD、TODO、"implement later"
- ✅ 无 "add appropriate error handling" 等模糊描述
- ✅ 所有代码块包含完整实现

**3. Type consistency:**
- ✅ `changePassword` 参数顺序在 Task 1 和 Task 2 中一致：`(Long userId, String oldPassword, String newPassword)`
- ✅ `resetPassword` 参数顺序一致：`(Long userId, String newPassword)`
- ✅ `updateProfile` 参数一致：`(Long userId, User updated)`
- ✅ API 路径一致：`/api/users/me`, `/api/users/me/password`, `/api/users/{id}/reset-password`
