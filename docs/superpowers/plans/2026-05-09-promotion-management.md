# 商品促销规则维护 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Sample Mall 后台增加商品促销规则维护功能，支持管理员 CRUD 促销活动和满减规则。

**Architecture:** 分阶段交付：第一阶段实现 Promotion CRUD，第二阶段实现 FullReductionRule CRUD。采用 TDD 开发模式（先写测试，再写实现）。保持现有内存存储架构和 Spring 分层模式。

**Tech Stack:** Spring Boot 2.7.5, Java 8, JUnit 5, Mockito, HTML/CSS/JS

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/java/com/example/mall/promotion/PromotionType.java` | 创建 | 活动类型枚举 |
| `src/main/java/com/example/mall/promotion/PromotionStatus.java` | 创建 | 活动状态枚举 |
| `src/main/java/com/example/mall/promotion/Promotion.java` | 创建 | 促销活动实体 |
| `src/main/java/com/example/mall/promotion/PromotionRepository.java` | 创建 | 内存存储 |
| `src/main/java/com/example/mall/promotion/PromotionService.java` | 创建 | 业务逻辑 |
| `src/main/java/com/example/mall/promotion/PromotionController.java` | 创建 | REST API |
| `src/test/java/com/example/mall/promotion/PromotionServiceTest.java` | 创建 | TDD 测试 |
| `src/main/java/com/example/mall/promotion/RuleType.java` | 创建 | 规则类型枚举 |
| `src/main/java/com/example/mall/promotion/FullReductionRule.java` | 创建 | 满减规则实体 |
| `src/main/java/com/example/mall/promotion/FullReductionRuleRepository.java` | 创建 | 内存存储 |
| `src/main/java/com/example/mall/promotion/FullReductionRuleService.java` | 创建 | 业务逻辑 |
| `src/test/java/com/example/mall/promotion/FullReductionRuleServiceTest.java` | 创建 | TDD 测试 |
| `src/main/resources/static/admin.html` | 修改 | 新增促销管理标签页 |

---

## 第一阶段：促销活动管理

### Task 1: Promotion 枚举和实体

**Files:**
- Create: `src/main/java/com/example/mall/promotion/PromotionType.java`
- Create: `src/main/java/com/example/mall/promotion/PromotionStatus.java`
- Create: `src/main/java/com/example/mall/promotion/Promotion.java`

- [ ] **Step 1: 创建 `PromotionType.java`**

```java
package com.example.mall.promotion;

public enum PromotionType {
    FULL_REDUCTION
}
```

- [ ] **Step 2: 创建 `PromotionStatus.java`**

```java
package com.example.mall.promotion;

public enum PromotionStatus {
    NOT_STARTED,
    PREHEATING,
    ACTIVE,
    ENDED
}
```

- [ ] **Step 3: 创建 `Promotion.java`**

```java
package com.example.mall.promotion;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class Promotion {
    private Long id;
    private String name;
    private String description;
    private PromotionType type;
    private PromotionStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime preheatTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private List<Long> productIds;
    private Boolean enabled;
    private Integer priority;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PromotionType getType() { return type; }
    public void setType(PromotionType type) { this.type = type; }
    public PromotionStatus getStatus() { return status; }
    public void setStatus(PromotionStatus status) { this.status = status; }
    public LocalDateTime getPreheatTime() { return preheatTime; }
    public void setPreheatTime(LocalDateTime preheatTime) { this.preheatTime = preheatTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/mall/promotion/
git commit -m "feat(promotion): add Promotion entity, PromotionType and PromotionStatus enums"
```

---

### Task 2: PromotionRepository

**Files:**
- Create: `src/main/java/com/example/mall/promotion/PromotionRepository.java`

- [ ] **Step 1: 创建 `PromotionRepository.java`**

```java
package com.example.mall.promotion;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class PromotionRepository {

    private final ConcurrentHashMap<Long, Promotion> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Promotion> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Promotion> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Promotion save(Promotion promotion) {
        if (promotion.getId() == null) {
            promotion.setId(idGenerator.getAndIncrement());
        }
        if (promotion.getCreatedAt() == null) {
            promotion.setCreatedAt(LocalDateTime.now());
        }
        promotion.setUpdatedAt(LocalDateTime.now());
        store.put(promotion.getId(), promotion);
        return promotion;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public List<Promotion> findByStatus(PromotionStatus status) {
        return store.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/mall/promotion/PromotionRepository.java
git commit -m "feat(promotion): add PromotionRepository with in-memory storage"
```

---

### Task 3: PromotionService (TDD)

**Files:**
- Create: `src/test/java/com/example/mall/promotion/PromotionServiceTest.java`
- Create: `src/main/java/com/example/mall/promotion/PromotionService.java`

- [ ] **Step 1: 创建测试目录并编写失败测试**

```bash
mkdir -p src/test/java/com/example/mall/promotion
```

创建 `src/test/java/com/example/mall/promotion/PromotionServiceTest.java`：

```java
package com.example.mall.promotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PromotionServiceTest {

    private PromotionService promotionService;
    private PromotionRepository promotionRepository;

    @BeforeEach
    void setUp() {
        promotionRepository = new PromotionRepository();
        promotionService = new PromotionService(promotionRepository);
    }

    @Test
    void create_shouldSetDefaultValues() {
        Promotion promotion = new Promotion();
        promotion.setName("春季大促");
        promotion.setStartTime(LocalDateTime.now().plusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        Promotion created = promotionService.create(promotion);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertEquals(Boolean.TRUE, created.getEnabled());
        assertEquals(PromotionType.FULL_REDUCTION, created.getType());
    }

    @Test
    void create_shouldThrowWhenNameTooShort() {
        Promotion promotion = new Promotion();
        promotion.setName("A");
        promotion.setStartTime(LocalDateTime.now().plusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promotionService.create(promotion));
        assertEquals("活动名称长度必须在2-50个字符之间", exception.getMessage());
    }

    @Test
    void create_shouldThrowWhenEndTimeBeforeStartTime() {
        Promotion promotion = new Promotion();
        promotion.setName("春季大促");
        promotion.setStartTime(LocalDateTime.now().plusDays(7));
        promotion.setEndTime(LocalDateTime.now().plusDays(1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> promotionService.create(promotion));
        assertEquals("结束时间必须晚于开始时间", exception.getMessage());
    }

    @Test
    void calculateStatus_shouldReturnNotStarted() {
        Promotion promotion = new Promotion();
        promotion.setPreheatTime(LocalDateTime.now().plusDays(1));
        promotion.setStartTime(LocalDateTime.now().plusDays(2));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.NOT_STARTED, status);
    }

    @Test
    void calculateStatus_shouldReturnActive() {
        Promotion promotion = new Promotion();
        promotion.setStartTime(LocalDateTime.now().minusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.ACTIVE, status);
    }

    @Test
    void calculateStatus_shouldReturnEnded() {
        Promotion promotion = new Promotion();
        promotion.setStartTime(LocalDateTime.now().minusDays(7));
        promotion.setEndTime(LocalDateTime.now().minusDays(1));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.ENDED, status);
    }

    @Test
    void calculateStatus_shouldReturnPreheating() {
        Promotion promotion = new Promotion();
        promotion.setPreheatTime(LocalDateTime.now().minusDays(1));
        promotion.setStartTime(LocalDateTime.now().plusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));

        PromotionStatus status = promotionService.calculateStatus(promotion);

        assertEquals(PromotionStatus.PREHEATING, status);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=PromotionServiceTest`
Expected: BUILD FAILURE / 6 tests fail (class not found / method not found)

- [ ] **Step 3: 创建 `PromotionService.java` 使测试通过**

```java
package com.example.mall.promotion;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public Promotion create(Promotion promotion) {
        if (promotion.getName() == null || promotion.getName().length() < 2 || promotion.getName().length() > 50) {
            throw new IllegalArgumentException("活动名称长度必须在2-50个字符之间");
        }
        if (promotion.getStartTime() == null || promotion.getEndTime() == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        if (!promotion.getEndTime().isAfter(promotion.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (promotion.getPreheatTime() != null && !promotion.getPreheatTime().isBefore(promotion.getStartTime())) {
            throw new IllegalArgumentException("预热时间必须早于开始时间");
        }
        if (promotion.getType() == null) {
            promotion.setType(PromotionType.FULL_REDUCTION);
        }
        if (promotion.getEnabled() == null) {
            promotion.setEnabled(true);
        }
        if (promotion.getPriority() == null) {
            promotion.setPriority(0);
        }
        return promotionRepository.save(promotion);
    }

    public Optional<Promotion> findById(Long id) {
        return promotionRepository.findById(id);
    }

    public List<Promotion> findAll() {
        return promotionRepository.findAll();
    }

    public Promotion update(Long id, Promotion updated) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在, id=" + id));
        if (updated.getName() != null) {
            if (updated.getName().length() < 2 || updated.getName().length() > 50) {
                throw new IllegalArgumentException("活动名称长度必须在2-50个字符之间");
            }
            existing.setName(updated.getName());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }
        if (updated.getPreheatTime() != null) {
            existing.setPreheatTime(updated.getPreheatTime());
        }
        if (updated.getStartTime() != null) {
            existing.setStartTime(updated.getStartTime());
        }
        if (updated.getEndTime() != null) {
            existing.setEndTime(updated.getEndTime());
        }
        if (updated.getProductIds() != null) {
            existing.setProductIds(updated.getProductIds());
        }
        if (updated.getEnabled() != null) {
            existing.setEnabled(updated.getEnabled());
        }
        if (updated.getPriority() != null) {
            existing.setPriority(updated.getPriority());
        }
        if (updated.getType() != null) {
            existing.setType(updated.getType());
        }
        return promotionRepository.save(existing);
    }

    public void delete(Long id) {
        promotionRepository.deleteById(id);
    }

    public PromotionStatus calculateStatus(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = promotion.getStartTime();
        LocalDateTime endTime = promotion.getEndTime();
        LocalDateTime preheatTime = promotion.getPreheatTime();

        if (now.isAfter(endTime)) {
            return PromotionStatus.ENDED;
        }
        if (preheatTime != null && !now.isBefore(preheatTime) && now.isBefore(startTime)) {
            return PromotionStatus.PREHEATING;
        }
        if (!now.isBefore(startTime) && !now.isAfter(endTime)) {
            return PromotionStatus.ACTIVE;
        }
        return PromotionStatus.NOT_STARTED;
    }

    public Promotion enable(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在, id=" + id));
        promotion.setEnabled(true);
        return promotionRepository.save(promotion);
    }

    public Promotion disable(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在, id=" + id));
        promotion.setEnabled(false);
        return promotionRepository.save(promotion);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=PromotionServiceTest`
Expected: 6 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/mall/promotion/PromotionService.java src/test/java/com/example/mall/promotion/PromotionServiceTest.java
git commit -m "feat(promotion): add PromotionService with TDD tests"
```

---

### Task 4: PromotionController

**Files:**
- Create: `src/main/java/com/example/mall/promotion/PromotionController.java`

- [ ] **Step 1: 创建 `PromotionController.java`**

```java
package com.example.mall.promotion;

import com.example.mall.user.User;
import com.example.mall.user.UserRole;
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
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ResponseEntity<?> listAll(HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        List<Promotion> promotions = promotionService.findAll();
        promotions.forEach(p -> p.setStatus(promotionService.calculateStatus(p)));
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        return promotionService.findById(id)
                .map(p -> {
                    p.setStatus(promotionService.calculateStatus(p));
                    return ResponseEntity.ok(p);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Promotion promotion, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion created = promotionService.create(promotion);
            created.setStatus(promotionService.calculateStatus(created));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("INVALID_REQUEST", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Promotion promotion, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion updated = promotionService.update(id, promotion);
            updated.setStatus(promotionService.calculateStatus(updated));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("PROMOTION_NOT_FOUND", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enable(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion promotion = promotionService.enable(id);
            promotion.setStatus(promotionService.calculateStatus(promotion));
            return ResponseEntity.ok(promotion);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("PROMOTION_NOT_FOUND", e.getMessage());
        }
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disable(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            Promotion promotion = promotionService.disable(id);
            promotion.setStatus(promotionService.calculateStatus(promotion));
            return ResponseEntity.ok(promotion);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("PROMOTION_NOT_FOUND", e.getMessage());
        }
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
git add src/main/java/com/example/mall/promotion/PromotionController.java
git commit -m "feat(promotion): add PromotionController with ADMIN-only access"
```

---

### Task 5: admin.html — 第一阶段 UI（促销管理标签页）

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 在 CSS 区域添加促销管理样式**

在 `</style>` 之前添加：

```css
        .status-badge {
            display: inline-block;
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 500;
        }
        .status-not_started { background: #e5e7eb; color: #374151; }
        .status-preheating { background: #fef3c7; color: #92400e; }
        .status-active { background: #d1fae5; color: #065f46; }
        .status-ended { background: #fee2e2; color: #991b1b; }
```

- [ ] **Step 2: 在标签内容区域添加促销管理标签页 HTML**

在 `<!-- 用户管理 -->` 标签页之前插入：

```html
        <!-- 促销管理 -->
        <div id="promotions-tab" class="tab-content">
            <div class="toolbar">
                <button class="btn btn-primary" onclick="showPromotionModal()">+ 新增活动</button>
                <select id="promotion-status-filter" class="category-select" onchange="loadPromotions()">
                    <option value="">全部状态</option>
                    <option value="NOT_STARTED">未开始</option>
                    <option value="PREHEATING">预热中</option>
                    <option value="ACTIVE">进行中</option>
                    <option value="ENDED">已结束</option>
                </select>
                <button class="btn" onclick="loadPromotions()">刷新</button>
            </div>

            <div id="promotions-loading" class="loading">加载中...</div>
            
            <table id="promotions-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>活动名称</th>
                        <th>类型</th>
                        <th>状态</th>
                        <th>开始时间</th>
                        <th>结束时间</th>
                        <th>启用</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody id="promotions-tbody">
                </tbody>
            </table>
        </div>
```

- [ ] **Step 3: 在 body 末尾添加活动表单模态框**

在 `</div>`（容器闭合）之前插入：

```html
    <!-- 活动模态框 -->
    <div id="promotion-modal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3 id="promotion-modal-title">新增活动</h3>
                <button class="modal-close" onclick="closePromotionModal()">&times;</button>
            </div>
            <form id="promotion-form">
                <input type="hidden" id="promotion-id">
                <div class="form-group">
                    <label>活动名称 *</label>
                    <input type="text" id="promotion-name" required minlength="2" maxlength="50">
                </div>
                <div class="form-group">
                    <label>活动描述</label>
                    <input type="text" id="promotion-description" maxlength="200">
                </div>
                <div class="form-group">
                    <label>类型</label>
                    <select id="promotion-type" class="category-select">
                        <option value="FULL_REDUCTION">满减</option>
                    </select>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>预热时间</label>
                        <input type="datetime-local" id="promotion-preheatTime">
                    </div>
                    <div class="form-group">
                        <label>开始时间 *</label>
                        <input type="datetime-local" id="promotion-startTime" required>
                    </div>
                    <div class="form-group">
                        <label>结束时间 *</label>
                        <input type="datetime-local" id="promotion-endTime" required>
                    </div>
                </div>
                <div class="form-group">
                    <label>参与商品ID（逗号分隔，留空=全场通用）</label>
                    <input type="text" id="promotion-productIds" placeholder="例如: 1,2,3">
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>优先级</label>
                        <input type="number" id="promotion-priority" value="0" min="0">
                    </div>
                    <div class="form-group">
                        <label>启用状态</label>
                        <select id="promotion-enabled" class="category-select">
                            <option value="true">启用</option>
                            <option value="false">禁用</option>
                        </select>
                    </div>
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">保存</button>
                    <button type="button" class="btn" onclick="closePromotionModal()">取消</button>
                </div>
            </form>
        </div>
    </div>
```

- [ ] **Step 4: 修改 `renderTabsByRole` 添加"促销管理"标签**

```javascript
            if (isAdmin) {
                tabsHtml += `<button class="tab-btn" onclick="switchTab('users')">用户管理</button>`;
                tabsHtml += `<button class="tab-btn" onclick="switchTab('promotions')">促销管理</button>`;
            }
```

- [ ] **Step 5: 修改 `switchTab` 添加 `promotions` 分支**

```javascript
            } else if (tab === 'promotions') {
                loadPromotions();
            }
```

- [ ] **Step 6: 在 `showApp()` 中绑定促销表单事件**

```javascript
            document.getElementById('promotion-form').addEventListener('submit', handlePromotionSubmit);
```

- [ ] **Step 7: 在 `// ==================== 用户管理 ====================` 之前添加促销管理 JS 函数**

```javascript
        // ==================== 促销管理 ====================

        async function loadPromotions() {
            const loading = document.getElementById('promotions-loading');
            const tbody = document.getElementById('promotions-tbody');
            const statusFilter = document.getElementById('promotion-status-filter').value;
            
            loading.classList.add('active');
            
            try {
                const response = await fetch(`${API_BASE_URL}/api/promotions`, {
                    credentials: 'include'
                });
                let promotions = await response.json();
                
                if (statusFilter) {
                    promotions = promotions.filter(p => p.status === statusFilter);
                }
                
                if (promotions.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="8" class="empty-state">暂无活动数据</td></tr>';
                } else {
                    tbody.innerHTML = promotions.map(p => {
                        const statusClass = 'status-' + p.status.toLowerCase();
                        const statusText = {
                            'NOT_STARTED': '未开始',
                            'PREHEATING': '预热中',
                            'ACTIVE': '进行中',
                            'ENDED': '已结束'
                        }[p.status] || p.status;
                        return `
                            <tr>
                                <td>${p.id}</td>
                                <td>${p.name}</td>
                                <td>${p.type === 'FULL_REDUCTION' ? '满减' : p.type}</td>
                                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                                <td>${new Date(p.startTime).toLocaleString('zh-CN')}</td>
                                <td>${new Date(p.endTime).toLocaleString('zh-CN')}</td>
                                <td>${p.enabled ? '✅' : '❌'}</td>
                                <td>
                                    <div class="action-btns">
                                        <button class="btn" onclick="editPromotion(${p.id}, '${p.name}', '${p.description || ''}', '${p.type}', '${p.preheatTime || ''}', '${p.startTime}', '${p.endTime}', '${(p.productIds || []).join(',')}', ${p.priority}, ${p.enabled})">编辑</button>
                                        <button class="btn" onclick="togglePromotion(${p.id}, ${p.enabled})">${p.enabled ? '禁用' : '启用'}</button>
                                        <button class="btn btn-danger" onclick="deletePromotion(${p.id})">删除</button>
                                    </div>
                                </td>
                            </tr>
                        `;
                    }).join('');
                }
            } catch (error) {
                showToast('加载活动列表失败: ' + error.message, 'error');
                tbody.innerHTML = '<tr><td colspan="8" class="empty-state">加载失败</td></tr>';
            } finally {
                loading.classList.remove('active');
            }
        }

        function showPromotionModal(promotion = null) {
            const modal = document.getElementById('promotion-modal');
            const title = document.getElementById('promotion-modal-title');
            
            if (promotion) {
                title.textContent = '编辑活动';
                document.getElementById('promotion-id').value = promotion.id;
                document.getElementById('promotion-name').value = promotion.name;
                document.getElementById('promotion-description').value = promotion.description || '';
                document.getElementById('promotion-type').value = promotion.type;
                document.getElementById('promotion-preheatTime').value = promotion.preheatTime ? new Date(promotion.preheatTime).toISOString().slice(0, 16) : '';
                document.getElementById('promotion-startTime').value = new Date(promotion.startTime).toISOString().slice(0, 16);
                document.getElementById('promotion-endTime').value = new Date(promotion.endTime).toISOString().slice(0, 16);
                document.getElementById('promotion-productIds').value = (promotion.productIds || []).join(',');
                document.getElementById('promotion-priority').value = promotion.priority;
                document.getElementById('promotion-enabled').value = String(promotion.enabled);
            } else {
                title.textContent = '新增活动';
                document.getElementById('promotion-form').reset();
                document.getElementById('promotion-id').value = '';
                document.getElementById('promotion-type').value = 'FULL_REDUCTION';
                document.getElementById('promotion-enabled').value = 'true';
                document.getElementById('promotion-priority').value = '0';
            }
            modal.classList.add('active');
        }

        function closePromotionModal() {
            document.getElementById('promotion-modal').classList.remove('active');
        }

        function editPromotion(id, name, description, type, preheatTime, startTime, endTime, productIds, priority, enabled) {
            showPromotionModal({ id, name, description, type, preheatTime, startTime, endTime, productIds: productIds ? productIds.split(',').map(Number) : [], priority, enabled });
        }

        async function handlePromotionSubmit(e) {
            e.preventDefault();
            const id = document.getElementById('promotion-id').value;
            const productIdsStr = document.getElementById('promotion-productIds').value;
            const data = {
                name: document.getElementById('promotion-name').value,
                description: document.getElementById('promotion-description').value,
                type: document.getElementById('promotion-type').value,
                preheatTime: document.getElementById('promotion-preheatTime').value || null,
                startTime: document.getElementById('promotion-startTime').value,
                endTime: document.getElementById('promotion-endTime').value,
                productIds: productIdsStr ? productIdsStr.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n)) : [],
                priority: parseInt(document.getElementById('promotion-priority').value) || 0,
                enabled: document.getElementById('promotion-enabled').value === 'true'
            };
            
            try {
                const url = id ? `${API_BASE_URL}/api/promotions/${id}` : `${API_BASE_URL}/api/promotions`;
                const method = id ? 'PUT' : 'POST';
                const response = await fetch(url, {
                    method,
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify(data)
                });
                const result = await response.json();
                if (response.ok) {
                    showToast(id ? '活动更新成功' : '活动创建成功', 'success');
                    closePromotionModal();
                    loadPromotions();
                } else {
                    showToast(result.message || '操作失败', 'error');
                }
            } catch (error) {
                showToast('操作失败: ' + error.message, 'error');
            }
        }

        async function togglePromotion(id, currentlyEnabled) {
            const action = currentlyEnabled ? 'disable' : 'enable';
            try {
                const response = await fetch(`${API_BASE_URL}/api/promotions/${id}/${action}`, {
                    method: 'PUT',
                    credentials: 'include'
                });
                if (response.ok) {
                    showToast(currentlyEnabled ? '活动已禁用' : '活动已启用', 'success');
                    loadPromotions();
                } else {
                    const result = await response.json();
                    showToast(result.message || '操作失败', 'error');
                }
            } catch (error) {
                showToast('操作失败: ' + error.message, 'error');
            }
        }

        async function deletePromotion(id) {
            if (!confirm('确定要删除这个活动吗? 关联的规则也将被删除。')) return;
            
            try {
                const response = await fetch(`${API_BASE_URL}/api/promotions/${id}`, {
                    method: 'DELETE',
                    credentials: 'include'
                });
                if (response.ok) {
                    showToast('活动删除成功', 'success');
                    loadPromotions();
                } else {
                    const result = await response.json();
                    showToast(result.message || '删除失败', 'error');
                }
            } catch (error) {
                showToast('删除失败: ' + error.message, 'error');
            }
        }
```

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add promotion management tab with activity list and form"
```

---

### Task 6: 第一阶段编译验证与运行测试

- [ ] **Step 1: 编译项目**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行单元测试**

Run: `mvn test -Dtest=PromotionServiceTest`
Expected: 6 tests PASSED

- [ ] **Step 3: 启动应用**

Run: `mvn spring-boot:run`
Expected: Started MallBackendApplication

- [ ] **Step 4: 手动验证**

1. 打开 `http://localhost:8080/admin.html`
2. 使用 `admin` / `admin` 登录
3. 切换到"促销管理"标签页
4. 创建活动：名称"测试活动"、开始时间明天、结束时间一周后
5. 编辑活动名称
6. 禁用/启用活动
7. 删除活动

- [ ] **Step 5: Commit**

```bash
git commit --allow-empty -m "test: phase 1 promotion management verified"
```

---

## 第二阶段：满减规则管理

### Task 7: RuleType 枚举和 FullReductionRule 实体

**Files:**
- Create: `src/main/java/com/example/mall/promotion/RuleType.java`
- Create: `src/main/java/com/example/mall/promotion/FullReductionRule.java`

- [ ] **Step 1: 创建 `RuleType.java`**

```java
package com.example.mall.promotion;

public enum RuleType {
    LADDER,
    PER_AMOUNT
}
```

- [ ] **Step 2: 创建 `FullReductionRule.java`**

```java
package com.example.mall.promotion;

public class FullReductionRule {
    private Long id;
    private Long promotionId;
    private RuleType type;
    private Integer fullAmount;
    private Integer reductionAmount;
    private Integer level;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }
    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }
    public Integer getFullAmount() { return fullAmount; }
    public void setFullAmount(Integer fullAmount) { this.fullAmount = fullAmount; }
    public Integer getReductionAmount() { return reductionAmount; }
    public void setReductionAmount(Integer reductionAmount) { this.reductionAmount = reductionAmount; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/mall/promotion/RuleType.java src/main/java/com/example/mall/promotion/FullReductionRule.java
git commit -m "feat(promotion): add FullReductionRule entity and RuleType enum"
```

---

### Task 8: FullReductionRuleRepository

**Files:**
- Create: `src/main/java/com/example/mall/promotion/FullReductionRuleRepository.java`

- [ ] **Step 1: 创建 `FullReductionRuleRepository.java`**

```java
package com.example.mall.promotion;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class FullReductionRuleRepository {

    private final ConcurrentHashMap<Long, FullReductionRule> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<FullReductionRule> findByPromotionId(Long promotionId) {
        return store.values().stream()
                .filter(r -> r.getPromotionId().equals(promotionId))
                .sorted((a, b) -> Integer.compare(a.getLevel(), b.getLevel()))
                .collect(Collectors.toList());
    }

    public Optional<FullReductionRule> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public FullReductionRule save(FullReductionRule rule) {
        if (rule.getId() == null) {
            rule.setId(idGenerator.getAndIncrement());
        }
        store.put(rule.getId(), rule);
        return rule;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public void deleteByPromotionId(Long promotionId) {
        List<Long> idsToDelete = store.values().stream()
                .filter(r -> r.getPromotionId().equals(promotionId))
                .map(FullReductionRule::getId)
                .collect(Collectors.toList());
        idsToDelete.forEach(store::remove);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/mall/promotion/FullReductionRuleRepository.java
git commit -m "feat(promotion): add FullReductionRuleRepository"
```

---

### Task 9: FullReductionRuleService (TDD)

**Files:**
- Create: `src/test/java/com/example/mall/promotion/FullReductionRuleServiceTest.java`
- Create: `src/main/java/com/example/mall/promotion/FullReductionRuleService.java`

- [ ] **Step 1: 编写失败测试**

创建 `src/test/java/com/example/mall/promotion/FullReductionRuleServiceTest.java`：

```java
package com.example.mall.promotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FullReductionRuleServiceTest {

    private FullReductionRuleService ruleService;
    private FullReductionRuleRepository ruleRepository;

    @BeforeEach
    void setUp() {
        ruleRepository = new FullReductionRuleRepository();
        ruleService = new FullReductionRuleService(ruleRepository);
    }

    @Test
    void addRule_shouldSucceedWithValidData() {
        FullReductionRule rule = new FullReductionRule();
        rule.setPromotionId(1L);
        rule.setType(RuleType.LADDER);
        rule.setFullAmount(10000);
        rule.setReductionAmount(1000);
        rule.setLevel(1);

        FullReductionRule created = ruleService.addRule(1L, rule);

        assertNotNull(created.getId());
        assertEquals(1L, created.getPromotionId());
    }

    @Test
    void addRule_shouldThrowWhenFullAmountNotPositive() {
        FullReductionRule rule = new FullReductionRule();
        rule.setPromotionId(1L);
        rule.setFullAmount(0);
        rule.setReductionAmount(1000);
        rule.setLevel(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ruleService.addRule(1L, rule));
        assertEquals("满足金额必须大于0", exception.getMessage());
    }

    @Test
    void addRule_shouldThrowWhenReductionNotLessThanFull() {
        FullReductionRule rule = new FullReductionRule();
        rule.setPromotionId(1L);
        rule.setFullAmount(1000);
        rule.setReductionAmount(1000);
        rule.setLevel(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ruleService.addRule(1L, rule));
        assertEquals("减免金额必须小于满足金额", exception.getMessage());
    }

    @Test
    void batchSetRules_shouldDeleteOldAndInsertNew() {
        FullReductionRule oldRule = new FullReductionRule();
        oldRule.setPromotionId(1L);
        oldRule.setFullAmount(5000);
        oldRule.setReductionAmount(500);
        oldRule.setLevel(1);
        ruleService.addRule(1L, oldRule);

        FullReductionRule newRule = new FullReductionRule();
        newRule.setPromotionId(1L);
        newRule.setFullAmount(10000);
        newRule.setReductionAmount(1000);
        newRule.setLevel(1);

        ruleService.batchSetRules(1L, Arrays.asList(newRule));

        List<FullReductionRule> rules = ruleService.findByPromotionId(1L);
        assertEquals(1, rules.size());
        assertEquals(10000, rules.get(0).getFullAmount());
    }

    @Test
    void batchSetRules_shouldThrowWhenLevelNotIncreasing() {
        FullReductionRule rule1 = new FullReductionRule();
        rule1.setPromotionId(1L);
        rule1.setFullAmount(20000);
        rule1.setReductionAmount(2000);
        rule1.setLevel(2);

        FullReductionRule rule2 = new FullReductionRule();
        rule2.setPromotionId(1L);
        rule2.setFullAmount(10000);
        rule2.setReductionAmount(1000);
        rule2.setLevel(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ruleService.batchSetRules(1L, Arrays.asList(rule1, rule2)));
        assertTrue(exception.getMessage().contains("level 越大，fullAmount 必须越大"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=FullReductionRuleServiceTest`
Expected: BUILD FAILURE / 5 tests fail

- [ ] **Step 3: 创建 `FullReductionRuleService.java` 使测试通过**

```java
package com.example.mall.promotion;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FullReductionRuleService {

    private final FullReductionRuleRepository ruleRepository;

    public FullReductionRuleService(FullReductionRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<FullReductionRule> findByPromotionId(Long promotionId) {
        return ruleRepository.findByPromotionId(promotionId);
    }

    public FullReductionRule addRule(Long promotionId, FullReductionRule rule) {
        rule.setPromotionId(promotionId);
        validateRule(rule);
        validateLevelUnique(promotionId, rule.getLevel(), null);
        return ruleRepository.save(rule);
    }

    public FullReductionRule updateRule(Long ruleId, FullReductionRule rule) {
        FullReductionRule existing = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在, id=" + ruleId));
        validateRule(rule);
        validateLevelUnique(existing.getPromotionId(), rule.getLevel(), ruleId);
        existing.setType(rule.getType());
        existing.setFullAmount(rule.getFullAmount());
        existing.setReductionAmount(rule.getReductionAmount());
        existing.setLevel(rule.getLevel());
        return ruleRepository.save(existing);
    }

    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    public void batchSetRules(Long promotionId, List<FullReductionRule> rules) {
        ruleRepository.deleteByPromotionId(promotionId);
        
        Set<Integer> levels = new HashSet<>();
        FullReductionRule prev = null;
        
        List<FullReductionRule> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(FullReductionRule::getLevel))
                .collect(java.util.stream.Collectors.toList());
        
        for (FullReductionRule rule : sortedRules) {
            rule.setPromotionId(promotionId);
            validateRule(rule);
            if (!levels.add(rule.getLevel())) {
                throw new IllegalArgumentException("阶梯档位 level=" + rule.getLevel() + " 重复");
            }
            if (prev != null && rule.getFullAmount() <= prev.getFullAmount()) {
                throw new IllegalArgumentException("level 越大，fullAmount 必须越大");
            }
            prev = rule;
        }
        
        for (FullReductionRule rule : sortedRules) {
            ruleRepository.save(rule);
        }
    }

    private void validateRule(FullReductionRule rule) {
        if (rule.getFullAmount() == null || rule.getFullAmount() <= 0) {
            throw new IllegalArgumentException("满足金额必须大于0");
        }
        if (rule.getReductionAmount() == null || rule.getReductionAmount() <= 0) {
            throw new IllegalArgumentException("减免金额必须大于0");
        }
        if (rule.getReductionAmount() >= rule.getFullAmount()) {
            throw new IllegalArgumentException("减免金额必须小于满足金额");
        }
        if (rule.getLevel() == null || rule.getLevel() < 1) {
            throw new IllegalArgumentException("阶梯等级必须大于等于1");
        }
    }

    private void validateLevelUnique(Long promotionId, Integer level, Long excludeRuleId) {
        List<FullReductionRule> existingRules = ruleRepository.findByPromotionId(promotionId);
        for (FullReductionRule r : existingRules) {
            if (r.getLevel().equals(level) && !r.getId().equals(excludeRuleId)) {
                throw new IllegalArgumentException("阶梯档位 level=" + level + " 已存在");
            }
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=FullReductionRuleServiceTest`
Expected: 5 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/mall/promotion/FullReductionRuleService.java src/test/java/com/example/mall/promotion/FullReductionRuleServiceTest.java
git commit -m "feat(promotion): add FullReductionRuleService with TDD tests"
```

---

### Task 10: PromotionController 补充规则端点 + 级联删除

**Files:**
- Modify: `src/main/java/com/example/mall/promotion/PromotionController.java`

- [ ] **Step 1: 在 `PromotionController` 中注入 `FullReductionRuleService` 并添加规则端点**

修改构造函数为：

```java
    private final PromotionService promotionService;
    private final FullReductionRuleService ruleService;

    public PromotionController(PromotionService promotionService, FullReductionRuleService ruleService) {
        this.promotionService = promotionService;
        this.ruleService = ruleService;
    }
```

在 `disable` 方法之后添加规则端点：

```java
    @GetMapping("/{id}/rules")
    public ResponseEntity<?> listRules(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        List<FullReductionRule> rules = ruleService.findByPromotionId(id);
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/{id}/rules/batch")
    public ResponseEntity<?> batchSetRules(@PathVariable Long id, @RequestBody List<FullReductionRule> rules, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        try {
            ruleService.batchSetRules(id, rules);
            return ResponseEntity.ok(ruleService.findByPromotionId(id));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("INVALID_RULE", e.getMessage());
        }
    }
```

修改 `delete` 方法为级联删除：

```java
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return buildErrorResponse("FORBIDDEN", "权限不足，需要管理员角色");
        }
        ruleService.findByPromotionId(id).forEach(r -> ruleService.deleteRule(r.getId()));
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/mall/promotion/PromotionController.java
git commit -m "feat(promotion): add rule endpoints and cascade delete in PromotionController"
```

---

### Task 11: admin.html — 第二阶段 UI（规则配置）

**Files:**
- Modify: `src/main/resources/static/admin.html`

- [ ] **Step 1: 在活动编辑模态框中添加规则配置区域**

在 `promotion-form` 的 `form-actions` 之前添加：

```html
                <div id="promotion-rules-section" style="display: none; margin-top: 24px; padding-top: 24px; border-top: 1px solid #e5e7eb;">
                    <h4 style="margin-bottom: 16px;">满减规则配置</h4>
                    <table id="rules-table" style="width: 100%; margin-bottom: 16px;">
                        <thead>
                            <tr>
                                <th>档位</th>
                                <th>类型</th>
                                <th>满足金额（分）</th>
                                <th>减免金额（分）</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="rules-tbody">
                        </tbody>
                    </table>
                    <button type="button" class="btn" onclick="addRuleRow()">+ 添加规则</button>
                </div>
```

- [ ] **Step 2: 修改 `editPromotion` 函数显示规则区域并加载规则**

修改 `showPromotionModal` 为：

```javascript
        function showPromotionModal(promotion = null) {
            const modal = document.getElementById('promotion-modal');
            const title = document.getElementById('promotion-modal-title');
            const rulesSection = document.getElementById('promotion-rules-section');
            
            if (promotion) {
                title.textContent = '编辑活动';
                document.getElementById('promotion-id').value = promotion.id;
                document.getElementById('promotion-name').value = promotion.name;
                document.getElementById('promotion-description').value = promotion.description || '';
                document.getElementById('promotion-type').value = promotion.type;
                document.getElementById('promotion-preheatTime').value = promotion.preheatTime ? new Date(promotion.preheatTime).toISOString().slice(0, 16) : '';
                document.getElementById('promotion-startTime').value = new Date(promotion.startTime).toISOString().slice(0, 16);
                document.getElementById('promotion-endTime').value = new Date(promotion.endTime).toISOString().slice(0, 16);
                document.getElementById('promotion-productIds').value = (promotion.productIds || []).join(',');
                document.getElementById('promotion-priority').value = promotion.priority;
                document.getElementById('promotion-enabled').value = String(promotion.enabled);
                rulesSection.style.display = 'block';
                loadPromotionRules(promotion.id);
            } else {
                title.textContent = '新增活动';
                document.getElementById('promotion-form').reset();
                document.getElementById('promotion-id').value = '';
                document.getElementById('promotion-type').value = 'FULL_REDUCTION';
                document.getElementById('promotion-enabled').value = 'true';
                document.getElementById('promotion-priority').value = '0';
                rulesSection.style.display = 'none';
                document.getElementById('rules-tbody').innerHTML = '';
            }
            modal.classList.add('active');
        }
```

- [ ] **Step 3: 在促销管理 JS 区域添加规则相关函数**

在 `deletePromotion` 函数之后添加：

```javascript
        async function loadPromotionRules(promotionId) {
            const tbody = document.getElementById('rules-tbody');
            try {
                const response = await fetch(`${API_BASE_URL}/api/promotions/${promotionId}/rules`, {
                    credentials: 'include'
                });
                const rules = await response.json();
                if (rules.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="5" class="empty-state">暂无规则</td></tr>';
                } else {
                    tbody.innerHTML = rules.map((r, index) => `
                        <tr data-index="${index}">
                            <td><input type="number" class="rule-level" value="${r.level}" min="1" style="width: 60px;"></td>
                            <td>
                                <select class="rule-type" style="width: 100px;">
                                    <option value="LADDER" ${r.type === 'LADDER' ? 'selected' : ''}>阶梯</option>
                                    <option value="PER_AMOUNT" ${r.type === 'PER_AMOUNT' ? 'selected' : ''}>每满</option>
                                </select>
                            </td>
                            <td><input type="number" class="rule-full" value="${r.fullAmount}" min="1" style="width: 100px;"></td>
                            <td><input type="number" class="rule-reduction" value="${r.reductionAmount}" min="1" style="width: 100px;"></td>
                            <td><button type="button" class="btn btn-danger" onclick="removeRuleRow(this)">删除</button></td>
                        </tr>
                    `).join('');
                }
            } catch (error) {
                tbody.innerHTML = '<tr><td colspan="5" class="empty-state">加载失败</td></tr>';
            }
        }

        function addRuleRow() {
            const tbody = document.getElementById('rules-tbody');
            const emptyRow = tbody.querySelector('.empty-state');
            if (emptyRow) emptyRow.parentElement.remove();
            
            const row = document.createElement('tr');
            row.innerHTML = `
                <td><input type="number" class="rule-level" min="1" style="width: 60px;"></td>
                <td>
                    <select class="rule-type" style="width: 100px;">
                        <option value="LADDER">阶梯</option>
                        <option value="PER_AMOUNT">每满</option>
                    </select>
                </td>
                <td><input type="number" class="rule-full" min="1" style="width: 100px;"></td>
                <td><input type="number" class="rule-reduction" min="1" style="width: 100px;"></td>
                <td><button type="button" class="btn btn-danger" onclick="removeRuleRow(this)">删除</button></td>
            `;
            tbody.appendChild(row);
        }

        function removeRuleRow(btn) {
            btn.closest('tr').remove();
            const tbody = document.getElementById('rules-tbody');
            if (tbody.children.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="empty-state">暂无规则</td></tr>';
            }
        }
```

- [ ] **Step 4: 修改 `handlePromotionSubmit` 在编辑时同时保存规则**

在 `handlePromotionSubmit` 的成功处理部分，如果 `id` 存在，添加规则保存逻辑：

```javascript
                if (response.ok) {
                    showToast(id ? '活动更新成功' : '活动创建成功', 'success');
                    if (id) {
                        await savePromotionRules(id);
                    }
                    closePromotionModal();
                    loadPromotions();
                }
```

添加 `savePromotionRules` 函数：

```javascript
        async function savePromotionRules(promotionId) {
            const rows = document.querySelectorAll('#rules-tbody tr');
            const rules = [];
            for (const row of rows) {
                const level = parseInt(row.querySelector('.rule-level').value);
                const type = row.querySelector('.rule-type').value;
                const fullAmount = parseInt(row.querySelector('.rule-full').value);
                const reductionAmount = parseInt(row.querySelector('.rule-reduction').value);
                if (isNaN(level) || isNaN(fullAmount) || isNaN(reductionAmount)) continue;
                rules.push({ promotionId, type, fullAmount, reductionAmount, level });
            }
            
            if (rules.length > 0) {
                try {
                    const response = await fetch(`${API_BASE_URL}/api/promotions/${promotionId}/rules/batch`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                        body: JSON.stringify(rules)
                    });
                    if (!response.ok) {
                        const result = await response.json();
                        showToast(result.message || '规则保存失败', 'error');
                    }
                } catch (error) {
                    showToast('规则保存失败: ' + error.message, 'error');
                }
            }
        }
```

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat(admin): add promotion rule configuration UI in modal"
```

---

### Task 12: 第二阶段编译验证与运行测试

- [ ] **Step 1: 编译项目**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行所有单元测试**

Run: `mvn test`
Expected: PromotionServiceTest (6 tests) + FullReductionRuleServiceTest (5 tests) = 11 tests PASSED

- [ ] **Step 3: 启动应用**

Run: `mvn spring-boot:run`
Expected: Started MallBackendApplication

- [ ] **Step 4: 手动验证**

1. 打开 `http://localhost:8080/admin.html`
2. 使用 `admin` / `admin` 登录
3. 创建活动，然后在编辑界面配置满减规则：
   - 档位 1：满 10000 分减 1000 分（阶梯）
   - 档位 2：满 20000 分减 3000 分（阶梯）
4. 保存后重新编辑，确认规则正确加载
5. 测试错误校验：减免金额 ≥ 满足金额时应报错
6. 删除活动，确认规则级联删除

- [ ] **Step 5: Commit**

```bash
git commit --allow-empty -m "test: phase 2 promotion rule management verified"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- ✅ Promotion 实体/枚举 → Task 1
- ✅ PromotionRepository → Task 2
- ✅ PromotionService + TDD → Task 3
- ✅ PromotionController 基本端点 → Task 4
- ✅ admin.html 活动列表/表单 → Task 5
- ✅ FullReductionRule 实体/枚举 → Task 7
- ✅ FullReductionRuleRepository → Task 8
- ✅ FullReductionRuleService + TDD → Task 9
- ✅ PromotionController 规则端点 + 级联删除 → Task 10
- ✅ admin.html 规则配置 → Task 11
- ✅ 编译验证与测试 → Task 6, 12

**2. Placeholder scan:**
- ✅ 无 TBD、TODO、"implement later"
- ✅ 所有代码块包含完整实现
- ✅ 无模糊描述

**3. Type consistency:**
- ✅ `PromotionService` 构造函数参数在 Task 3 和 Task 4 中一致
- ✅ `PromotionController` 构造函数在 Task 4 和 Task 10 中一致（增加 `FullReductionRuleService`）
- ✅ API 路径与 spec 一致：`/api/promotions`, `/api/promotions/{id}/rules/batch`
