# 商品促销规则维护功能设计文档

## 概述

为 Sample Mall 后台管理系统增加**商品促销规则维护**功能。管理员可在 `admin.html` 中创建、编辑、删除促销活动及其满减规则。本功能按分阶段策略交付：第一阶段实现促销活动 CRUD，第二阶段实现满减规则管理。

**范围**：仅后台规则维护（P0 + P1），不包含优惠计算引擎、订单关联、数据统计。

---

## 数据模型设计

采用与项目一致的内存存储风格（`ConcurrentHashMap` + `AtomicLong`）。

### 1. `Promotion` 实体

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | Long | 是 | 自增主键 |
| `name` | String | 是 | 活动名称，2-50 字符 |
| `description` | String | 否 | 活动描述，0-200 字符 |
| `type` | `PromotionType` | 是 | 活动类型，默认 `FULL_REDUCTION` |
| `status` | `PromotionStatus` | 是 | **查询时自动计算**，不持久化 |
| `preheatTime` | `LocalDateTime` | 否 | 预热开始时间，必须早于 `startTime` |
| `startTime` | `LocalDateTime` | 是 | 活动开始时间 |
| `endTime` | `LocalDateTime` | 是 | 活动结束时间，必须晚于 `startTime` |
| `productIds` | `List<Long>` | 否 | 参与商品 ID 列表，空数组 = 全场通用 |
| `enabled` | Boolean | 是 | 是否启用，默认 `true` |
| `priority` | Integer | 是 | 活动优先级，默认 0，数值越大优先级越高 |
| `createdAt` | `LocalDateTime` | 是 | 创建时间 |
| `updatedAt` | `LocalDateTime` | 是 | 更新时间 |

**枚举定义：**

```java
public enum PromotionType {
    FULL_REDUCTION  // 满减（预留扩展：FLASH_SALE, COUPON）
}

public enum PromotionStatus {
    NOT_STARTED,  // 未开始
    PREHEATING,   // 预热中（preheatTime <= now < startTime）
    ACTIVE,       // 进行中
    ENDED         // 已结束
}
```

### 2. `FullReductionRule` 实体

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | Long | 是 | 自增主键 |
| `promotionId` | Long | 是 | 关联促销活动 ID |
| `type` | `RuleType` | 是 | 规则类型：LADDER / PER_AMOUNT |
| `fullAmount` | Integer | 是 | 满足金额（分），必须 > 0 |
| `reductionAmount` | Integer | 是 | 减免金额（分），必须 > 0 且 < `fullAmount` |
| `level` | Integer | 是 | 阶梯等级，≥ 1，同一活动内唯一 |

**枚举定义：**

```java
public enum RuleType {
    LADDER,      // 阶梯满减：满100减10，满200减30（取最高档位）
    PER_AMOUNT   // 每满减：每满100减10，可叠加（200元减20）
}
```

**阶梯规则约束：**
- `level` 越大，`fullAmount` 必须越大
- 同一活动内 `level` 必须唯一

---

## 后端 API 设计

所有接口均为 **ADMIN 专属**。

### 第一阶段：促销活动管理（P0 核心）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/promotions` | 查询活动列表（支持分页、按状态筛选、按时间排序） |
| `GET` | `/api/promotions/{id}` | 查询活动详情（返回 Promotion 基本信息，不含规则） |
| `POST` | `/api/promotions` | 创建活动 |
| `PUT` | `/api/promotions/{id}` | 更新活动（可修改名称、时间、启用状态、商品范围等） |
| `DELETE` | `/api/promotions/{id}` | 删除活动（同时级联删除关联规则） |

### 第二阶段：满减规则管理（P1 + P0 批量规则）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/promotions/{id}/rules` | 查询某活动的所有规则（按 `level` 排序） |
| `POST` | `/api/promotions/{id}/rules` | 为活动添加单条规则 |
| `PUT` | `/api/promotions/{id}/rules/{ruleId}` | 修改单条规则 |
| `DELETE` | `/api/promotions/{id}/rules/{ruleId}` | 删除单条规则 |
| `POST` | `/api/promotions/{id}/rules/batch` | **批量覆盖设置**规则（删除旧规则，插入新规则列表） |
| `PUT` | `/api/promotions/{id}/enable` | 启用活动 |
| `PUT` | `/api/promotions/{id}/disable` | 禁用活动 |

### `status` 自动计算策略

查询时根据当前时间与 `preheatTime` / `startTime` / `endTime` 比对实时计算，不持久化存储：

```
now < preheatTime         → NOT_STARTED
preheatTime <= now < startTime → PREHEATING
startTime <= now <= endTime    → ACTIVE
now > endTime             → ENDED
```

---

## 后端业务逻辑设计

### 1. `PromotionRepository`

- 使用 `ConcurrentHashMap<Long, Promotion>` + `AtomicLong`
- 从 `promotions.json` 加载初始数据（如有）
- 关键方法：`findAll()`, `findById(Long)`, `save(Promotion)`, `deleteById(Long)`, `findByStatus(PromotionStatus)`

### 2. `FullReductionRuleRepository`

- 使用 `ConcurrentHashMap<Long, FullReductionRule>` + `AtomicLong`
- 关键方法：`findByPromotionId(Long)`, `findById(Long)`, `save(FullReductionRule)`, `deleteById(Long)`, `deleteByPromotionId(Long)`（级联删除）

### 3. `PromotionService`

- **`create(Promotion)`** — 校验 `name` 长度（2-50）、`endTime > startTime`、`preheatTime < startTime`（如有），自动填充 `createdAt` / `updatedAt`，默认 `enabled=true`
- **`update(Long id, Promotion)`** — 查找并更新允许修改的字段，更新 `updatedAt`
- **`delete(Long id)`** — 删除活动，同时调用 `FullReductionRuleRepository.deleteByPromotionId(id)` 级联删除规则
- **`calculateStatus(Promotion)`** — 根据当前时间与 `preheatTime` / `startTime` / `endTime` 计算并返回 `PromotionStatus`
- **`enable(Long id)` / `disable(Long id)`** — 切换 `enabled` 字段

### 4. `FullReductionRuleService`

- **`findByPromotionId(Long promotionId)`** — 按 `level` 升序返回规则列表
- **`addRule(Long promotionId, FullReductionRule)`** — 校验 `fullAmount > 0`、`reductionAmount > 0` 且 `< fullAmount`、`level >= 1` 且同一活动内唯一
- **`updateRule(Long ruleId, FullReductionRule)`** — 修改规则并校验
- **`deleteRule(Long ruleId)`** — 删除单条规则
- **`batchSetRules(Long promotionId, List<FullReductionRule>)`** — 删除该活动所有旧规则，批量插入新规则，并校验：
  - `level` 唯一
  - `level` 越大，`fullAmount` 越大
  - 每条规则的 `reductionAmount < fullAmount`

---

## 前端设计

在 `admin.html` 中新增**"促销管理"标签页**（仅 ADMIN 可见，与"用户管理"同级）。

### 第一阶段：活动列表与表单

**活动列表区域**
- 表格字段：ID、活动名称、类型、状态（自动计算 + 颜色标签）、开始时间、结束时间、启用状态、操作
- 操作按钮：编辑、禁用/启用、删除
- 顶部工具栏："+ 新增活动"按钮、状态筛选下拉框（全部/未开始/进行中/已结束）、刷新按钮

**活动表单模态框**
- 字段：活动名称、活动描述、类型（下拉，目前仅 FULL_REDUCTION）、预热时间、开始时间、结束时间、参与商品（多选下拉或 ID 输入）、启用状态（开关）、优先级（数字输入）
- 前端基础校验：名称 2-50 字符、结束时间 > 开始时间

### 第二阶段：规则配置

**规则配置区域**
- 嵌入在活动详情/编辑模态框中的下半部分
- 表格字段：档位（level）、规则类型（阶梯/每满）、满足金额、减免金额
- 操作：增删改规则行
- **批量保存按钮**：调用 `/api/promotions/{id}/rules/batch` 一次性覆盖所有规则
- 前端校验：满足金额递增、减免金额 < 满足金额

---

## 安全、权限与分阶段策略

### 权限控制

- 所有 `/api/promotions/**` 接口仅允许 ADMIN 访问
- 前端"促销管理"标签页仅在 `renderTabsByRole` 中 `isAdmin` 时渲染

### 错误处理

- 统一返回 `{ error: "ERROR_CODE", message: "描述" }`
- 新增错误码：
  - `INVALID_TIME_RANGE` — 时间范围设置错误
  - `DUPLICATE_LEVEL` — 阶梯档位重复
  - `INVALID_RULE_AMOUNT` — 减免金额不小于满足金额
  - `PROMOTION_NOT_FOUND` — 活动不存在

### 分阶段实现策略

| 阶段 | 交付内容 | 验收标准 |
|------|---------|---------|
| **第一阶段** | `Promotion` 实体/枚举/仓库/服务/控制器 + `admin.html` 活动列表/表单 | 管理员可完整 CRUD 活动，时间校验生效，状态自动计算正确 |
| **第二阶段** | `FullReductionRule` 实体/枚举/仓库/服务 + 规则配置 UI + 批量设置接口 | 管理员可为活动配置阶梯/每满规则，前端校验正确，级联删除正常 |
