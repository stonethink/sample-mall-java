# 春季满减促销系统 - 设计文档 PRD

> 文档版本：v1.0  
> 创建日期：2026-04-11  
> 关联需求：[spring-full-reduction-design.md](./spring-full-reduction-design.md)

---

## 1. 文档概述

### 1.1 文档目的

本文档是春季满减促销系统的**产品设计文档（PRD）**，用于指导开发团队进行技术实现。文档涵盖系统架构、数据模型、接口规范、业务规则等核心设计内容。

### 1.2 目标读者

- 后端开发工程师
- 前端开发工程师
- 测试工程师
- 产品经理

### 1.3 术语定义

| 术语 | 英文 | 说明 |
|------|------|------|
| 满减 | Full Reduction | 订单金额达到指定门槛后，减免固定金额 |
| 阶梯满减 | Tiered Reduction | 设置多个满减档位，消费金额越高优惠力度越大 |
| 全场通用 | Universal | 所有商品均参与活动 |
| 指定商品 | Specific Products | 仅特定商品列表中的商品参与活动 |
| 差额提醒 | Gap Hint | 提示用户距离下一档满减还差多少金额 |

---

## 2. 系统架构设计

### 2.1 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        管理后台 (Admin UI)                        │
│                     ┌─────────────────┐                         │
│                     │   admin.html    │                         │
│                     │  - 活动列表     │                         │
│                     │  - 活动表单     │                         │
│                     │  - 规则配置     │                         │
│                     └────────┬────────┘                         │
└──────────────────────────────┼──────────────────────────────────┘
                               │ HTTP
┌──────────────────────────────┼──────────────────────────────────┐
│                         REST API 层                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ /api/promotions │  │ /api/promotions │  │ /api/promotions │  │
│  │      CRUD       │  │ /{id}/rules     │  │ /calculate      │  │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘  │
│           │                    │                    │           │
│  ┌────────▼────────┐  ┌────────▼────────┐           │           │
│  │ Promotion       │  │ FullReduction   │           │           │
│  │ Controller      │  │ Rule Controller │           │           │
│  └────────┬────────┘  └─────────────────┘           │           │
└───────────┼─────────────────────────────────────────┼───────────┘
            │                                         │
┌───────────▼─────────────────────────────────────────▼───────────┐
│                         业务服务层                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ Promotion       │  │ FullReduction   │  │ Promotion       │  │
│  │ Service         │  │ Rule Service    │  │ Calculator      │  │
│  │ - CRUD          │  │ - CRUD          │  │ - 优惠计算      │  │
│  │ - 状态管理      │  │ - 批量设置      │  │ - 差额提醒      │  │
│  │ - 冲突校验      │  │ - 阶梯校验      │  │ - 规则匹配      │  │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘  │
└───────────┼────────────────────┼────────────────────┼───────────┘
            │                    │                    │
┌───────────▼────────────────────▼────────────────────▼───────────┐
│                         数据存储层                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ Promotion       │  │ FullReduction   │  │ Product         │  │
│  │ Repository      │  │ Rule Repository │  │ Repository      │  │
│  │ (ConcurrentHash │  │ (ConcurrentHash │  │ (ConcurrentHash │  │
│  │  Map + JSON)    │  │  Map + JSON)    │  │  Map + JSON)    │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 模块 | 职责 | 核心类 |
|------|------|--------|
| 促销活动管理 | 活动的生命周期管理 | Promotion, PromotionService, PromotionController |
| 满减规则管理 | 阶梯规则的配置与校验 | FullReductionRule, FullReductionRuleService |
| 优惠计算引擎 | 订单优惠金额计算 | PromotionCalculator |
| 订单集成 | 下单流程与优惠结合 | Order (扩展), OrderService (扩展) |
| 管理后台 | 运营操作界面 | admin.html (扩展) |

### 2.3 技术选型

| 层面 | 技术/方案 | 说明 |
|------|----------|------|
| 后端框架 | Spring Boot 2.7.5 | 与现有系统保持一致 |
| 编程语言 | Java 8 | 与现有系统保持一致 |
| 数据存储 | ConcurrentHashMap + JSON | 内存存储，保持轻量化 |
| API 文档 | Swagger 3.0.0 | 自动生成接口文档 |
| 前端 | 原生 HTML + JavaScript | 单页管理后台 |

---

## 3. 数据模型设计

### 3.1 实体关系图

```
┌─────────────────────┐         ┌─────────────────────────┐
│     Promotion       │         │   FullReductionRule     │
├─────────────────────┤         ├─────────────────────────┤
│ PK id: Long         │◄───────│ FK promotionId: Long    │
│    name: String     │  1 : N  │    fullAmount: Integer  │
│    description: Str │         │    reductionAmount: Int │
│    type: Enum       │         │    level: Integer       │
│    status: Enum     │         └─────────────────────────┘
│    startTime: Local │
│    endTime: Local   │
│    productIds: List │
│    enabled: Boolean │
└─────────────────────┘
         │
         │ 0/1 : 0/1
         ▼
┌─────────────────────┐
│       Order         │
├─────────────────────┤
│ PK id: Long         │
│    orderSn: String  │
│    productIds: List │
│    originalAmount   │
│    promotionDiscoun │
│    payAmount        │
│ FK promotionId      │
└─────────────────────┘
```

### 3.2 促销活动 (Promotion)

```java
public class Promotion {
    private Long id;                    // 活动ID，自增主键
    private String name;                // 活动名称，如"春季满减大促"
    private String description;         // 活动描述
    private PromotionType type;         // 活动类型：FULL_REDUCTION
    private PromotionStatus status;     // 活动状态：NOT_STARTED/ACTIVE/ENDED
    private LocalDateTime startTime;    // 活动开始时间
    private LocalDateTime endTime;      // 活动结束时间
    private List<Long> productIds;      // 参与活动的商品ID列表（空=全场通用）
    private Boolean enabled;            // 是否启用
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
```

**字段设计解析：**

| 字段 | 设计意图 |
|------|----------|
| `id` | 主键标识，用于唯一确定一个活动 |
| `name` | 运营展示用，如"春季满减大促" |
| `description` | 补充说明，便于运营人员理解活动详情 |
| `type` | **预留扩展**：本期只有满减，但用枚举方便后续加限时特价、优惠券等 |
| `status` | **自动计算**：根据时间自动流转（未开始/进行中/已结束），非手动设置 |
| `startTime/endTime` | 活动时间窗口，精确控制活动生效范围 |
| `productIds` | **灵活配置**：空数组=全场通用，有值=指定商品参与 |
| `enabled` | **独立开关**：即使时间在范围内，也可手动禁用活动 |
| `createdAt/updatedAt` | 审计字段，追踪记录变更 |

**字段约束：**

| 字段 | 类型 | 必填 | 约束说明 |
|------|------|------|----------|
| id | Long | 是 | 自增主键，唯一 |
| name | String | 是 | 长度 2-50 字符 |
| description | String | 否 | 长度 0-200 字符 |
| type | Enum | 是 | 默认 FULL_REDUCTION |
| status | Enum | 是 | 系统自动计算，不允许手动修改 |
| startTime | DateTime | 是 | 必须早于 endTime |
| endTime | DateTime | 是 | 必须晚于 startTime |
| productIds | List | 否 | 空数组表示全场通用 |
| enabled | Boolean | 是 | 默认 true |

**关键设计决策：**

**1. 为什么 `status` 要自动计算？**

手动维护状态容易出错（可能忘记改状态）。自动计算通过比对当前时间与起止时间，实时确定状态，更可靠。

**2. 为什么 `type` 用枚举而非写死？**

```java
// 本期
FULL_REDUCTION

// 后续可扩展
FLASH_SALE    // 限时特价
COUPON        // 优惠券
GROUP_BUY     // 拼团
```

**3. 为什么 `enabled` 和 `status` 并存？**

| 场景 | enabled | status | 是否生效 |
|------|---------|--------|----------|
| 活动进行中 | true | ACTIVE | ✅ 生效 |
| 活动进行中 | false | ACTIVE | ❌ 不生效（手动关闭）|
| 活动未开始 | true | NOT_STARTED | ⏳ 待生效 |

这样实现了**时间控制**和**人工干预**的双重机制。

**4. 为什么 `productIds` 用 List 而非关联表？**

- 项目采用 **内存存储**（ConcurrentHashMap）
- 简化设计，避免复杂关联
- JSON 序列化友好，直接存 `[1,2,3]`

**整体设计理念：**

1. **开闭原则** - `type` 枚举预留扩展点
2. **单一职责** - 状态自动计算，不依赖人工维护
3. **灵活性** - `enabled` 开关 + `productIds` 配置满足不同场景
4. **轻量化** - 与项目整体风格一致（内存存储、JSON 数据）

**枚举定义：**

```java
public enum PromotionType {
    FULL_REDUCTION  // 满减（预留扩展：FLASH_SALE, COUPON）
}

public enum PromotionStatus {
    NOT_STARTED,  // 未开始
    ACTIVE,       // 进行中
    ENDED         // 已结束
}
```

### 3.3 满减规则 (FullReductionRule)

```java
public class FullReductionRule {
    private Long id;                    // 规则ID，自增主键
    private Long promotionId;           // 关联促销活动ID
    private Integer fullAmount;         // 满足金额（分）
    private Integer reductionAmount;    // 减免金额（分）
    private Integer level;              // 阶梯等级（1=最低档）
}
```

**字段约束：**

| 字段 | 类型 | 必填 | 约束说明 |
|------|------|------|----------|
| id | Long | 是 | 自增主键 |
| promotionId | Long | 是 | 外键，关联 Promotion |
| fullAmount | Integer | 是 | 必须 > 0，单位：分 |
| reductionAmount | Integer | 是 | 必须 > 0 且 < fullAmount |
| level | Integer | 是 | 必须 >= 1，同一活动内唯一 |

**阶梯规则约束：**

- level 越大，fullAmount 必须越大
- level 越大，reductionAmount 必须越大
- 同一活动内 level 不允许重复

### 3.4 订单扩展 (Order)

```java
public class Order {
    // === 现有字段（保留）===
    private Long id;
    private String orderSn;
    private List<Long> productIds;
    private Integer totalAmount;        // 兼容字段，值 = payAmount
    private LocalDateTime createdAt;
    
    // === 新增字段 ===
    private Integer originalAmount;     // 商品原始总金额（分）
    private Integer promotionDiscount;  // 满减优惠金额（分）
    private Integer payAmount;          // 最终支付金额（分）
    private Long promotionId;           // 命中的满减活动ID
}
```

**金额计算逻辑：**

```
payAmount = originalAmount - promotionDiscount
payAmount >= 0（最小为0）
totalAmount = payAmount（兼容现有接口）
```

---

## 4. 接口规范

### 4.1 REST API 清单

#### 促销活动管理接口

| 方法 | 路径 | 功能 | 优先级 |
|------|------|------|--------|
| GET | `/api/promotions` | 查询所有活动（支持状态筛选） | P0 |
| GET | `/api/promotions/{id}` | 查询活动详情（含规则） | P0 |
| GET | `/api/promotions/active` | 查询当前生效活动 | P0 |
| POST | `/api/promotions` | 创建活动 | P0 |
| PUT | `/api/promotions/{id}` | 更新活动 | P0 |
| DELETE | `/api/promotions/{id}` | 删除活动 | P0 |
| PUT | `/api/promotions/{id}/enable` | 启用活动 | P1 |
| PUT | `/api/promotions/{id}/disable` | 禁用活动 | P1 |

#### 满减规则管理接口

| 方法 | 路径 | 功能 | 优先级 |
|------|------|------|--------|
| GET | `/api/promotions/{id}/rules` | 查询活动规则 | P0 |
| POST | `/api/promotions/{id}/rules` | 添加单条规则 | P1 |
| PUT | `/api/promotions/{id}/rules/{ruleId}` | 修改规则 | P1 |
| DELETE | `/api/promotions/{id}/rules/{ruleId}` | 删除规则 | P1 |
| POST | `/api/promotions/{id}/rules/batch` | 批量设置规则 | P0 |

#### 优惠计算接口

| 方法 | 路径 | 功能 | 优先级 |
|------|------|------|--------|
| POST | `/api/promotions/calculate` | 计算订单优惠 | P0 |

### 4.2 接口详细规范

#### 4.2.1 创建促销活动

**请求：**

```http
POST /api/promotions
Content-Type: application/json

{
    "name": "春季满减大促",
    "description": "春暖花开，全场满100减10，满200减30，满300减60",
    "type": "FULL_REDUCTION",
    "startTime": "2026-03-20T00:00:00",
    "endTime": "2026-04-20T23:59:59",
    "productIds": [],
    "enabled": true
}
```

**响应（成功 201）：**

```json
{
    "id": 1,
    "name": "春季满减大促",
    "description": "春暖花开，全场满100减10，满200减30，满300减60",
    "type": "FULL_REDUCTION",
    "status": "ACTIVE",
    "startTime": "2026-03-20T00:00:00",
    "endTime": "2026-04-20T23:59:59",
    "productIds": [],
    "enabled": true,
    "createdAt": "2026-03-18T10:30:00",
    "updatedAt": "2026-03-18T10:30:00"
}
```

**响应（失败 400）：**

```json
{
    "code": 400,
    "message": "活动时间设置错误：结束时间必须晚于开始时间"
}
```

#### 4.2.2 批量设置满减规则

**请求：**

```http
POST /api/promotions/1/rules/batch
Content-Type: application/json

[
    { "fullAmount": 10000, "reductionAmount": 1000, "level": 1 },
    { "fullAmount": 20000, "reductionAmount": 3000, "level": 2 },
    { "fullAmount": 30000, "reductionAmount": 6000, "level": 3 }
]
```

**业务含义：**

| level | fullAmount | reductionAmount | 业务含义 |
|-------|------------|-----------------|----------|
| 1 | 10000 | 1000 | 满 100 元减 10 元 |
| 2 | 20000 | 3000 | 满 200 元减 30 元 |
| 3 | 30000 | 6000 | 满 300 元减 60 元 |

**响应（成功 200）：**

```json
{
    "code": 200,
    "message": "规则设置成功",
    "data": {
        "promotionId": 1,
        "rulesCount": 3,
        "rules": [
            { "id": 1, "fullAmount": 10000, "reductionAmount": 1000, "level": 1 },
            { "id": 2, "fullAmount": 20000, "reductionAmount": 3000, "level": 2 },
            { "id": 3, "fullAmount": 30000, "reductionAmount": 6000, "level": 3 }
        ]
    }
}
```

#### 4.2.3 计算订单优惠

**请求：**

```http
POST /api/promotions/calculate
Content-Type: application/json

{
    "productIds": [1, 2, 3]
}
```

**响应（命中优惠 200）：**

```json
{
    "originalAmount": 25000,
    "promotionId": 1,
    "promotionName": "春季满减大促",
    "matchedRule": {
        "fullAmount": 20000,
        "reductionAmount": 3000,
        "level": 2
    },
    "promotionDiscount": 3000,
    "payAmount": 22000,
    "nextRule": {
        "fullAmount": 30000,
        "reductionAmount": 6000,
        "gap": 5000,
        "hint": "再买50.00元可享满300减60优惠"
    }
}
```

**响应（未命中优惠 200）：**

```json
{
    "originalAmount": 8000,
    "promotionId": null,
    "promotionName": null,
    "matchedRule": null,
    "promotionDiscount": 0,
    "payAmount": 8000,
    "nextRule": {
        "fullAmount": 10000,
        "reductionAmount": 1000,
        "gap": 2000,
        "hint": "再买20.00元可享满100减10优惠"
    }
}
```

### 4.3 状态码规范

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 查询、更新成功 |
| 201 | 创建成功 | 创建活动、规则成功 |
| 204 | 无内容 | 删除成功 |
| 400 | 请求参数错误 | 参数校验失败、业务规则冲突 |
| 404 | 资源不存在 | 活动/规则ID不存在 |
| 409 | 资源冲突 | 活动冲突、规则冲突 |
| 500 | 服务器内部错误 | 系统异常 |

---

## 5. 业务规则设计

### 5.1 活动状态流转

```
                    ┌─────────────┐
                    │  创建活动    │
                    └──────┬──────┘
                           │
                           ▼
              ┌────────────────────────┐
    ┌────────►│      NOT_STARTED       │◄────────┐
    │         │        未开始           │         │
    │         └───────────┬────────────┘         │
    │                     │ startTime <= now      │
    │                     ▼                       │
    │         ┌────────────────────────┐         │
    │  endTime│       ACTIVE           │         │
    │  < now  │       进行中           │         │
    │         └───────────┬────────────┘         │
    │                     │ endTime < now         │
    │                     ▼                       │
    │         ┌────────────────────────┐         │
    └─────────┤        ENDED           ├─────────┘
              │        已结束           │
              └────────────────────────┘
```

**状态计算规则：**

```java
if (now < startTime) {
    status = NOT_STARTED;
} else if (now >= startTime && now <= endTime) {
    status = ACTIVE;
} else {
    status = ENDED;
}
```

### 5.2 活动冲突校验规则

**冲突场景：**

| 场景 | 新活动 | 已有活动 | 是否冲突 |
|------|--------|----------|----------|
| 1 | 全场通用 | 全场通用 | 冲突 |
| 2 | 全场通用 | 指定商品A | 冲突 |
| 3 | 指定商品A | 全场通用 | 冲突 |
| 4 | 指定商品A | 指定商品A | 冲突 |
| 5 | 指定商品A | 指定商品B（A≠B） | 不冲突 |

**校验时机：**

- 创建活动时
- 更新活动时间或商品范围时
- 启用禁用的活动时

### 5.3 满减规则阶梯约束

**约束条件：**

```
规则列表按 level 升序排列：R1, R2, R3, ..., Rn

必须满足：
1. R1.fullAmount < R2.fullAmount < ... < Rn.fullAmount
2. R1.reductionAmount < R2.reductionAmount < ... < Rn.reductionAmount
3. ∀i, Ri.reductionAmount < Ri.fullAmount
```

**示例：**

```
✓ 合法规则：
  level=1: 满100减10
  level=2: 满200减30
  level=3: 满300减60

✗ 非法规则（reductionAmount 不递增）：
  level=1: 满100减20
  level=2: 满200减15  ← 错误：20 > 15

✗ 非法规则（reductionAmount >= fullAmount）：
  level=1: 满100减100  ← 错误：必须小于
```

### 5.4 优惠计算规则

**计算流程：**

```
输入：订单商品ID列表 [pid1, pid2, ...]

Step 1: 计算原始金额
  originalAmount = Σ(商品单价 × 数量)

Step 2: 查询生效活动
  activePromotions = 查询 enabled=true 且 status=ACTIVE 的活动

Step 3: 匹配适用活动
  for each promotion in activePromotions:
    if promotion.productIds 为空:
      applicableAmount = originalAmount
    else:
      applicableAmount = Σ(订单中属于 productIds 的商品金额)
    
    if applicableAmount >= 最低档位满金额:
      该活动适用

Step 4: 选择最优活动（取优惠金额最大）
  如果多个活动适用，选择 reductionAmount 最大的

Step 5: 计算优惠金额
  promotionDiscount = 匹配档位的 reductionAmount
  payAmount = originalAmount - promotionDiscount
  payAmount = max(0, payAmount)

Step 6: 计算差额提醒
  nextRule = 当前匹配档位的下一档
  if nextRule 存在:
    gap = nextRule.fullAmount - applicableAmount
    hint = "再买{gap}元可享满{nextRule.fullAmount/100}减{nextRule.reductionAmount/100}优惠"
```

**计算示例：**

```
活动：春季满减大促（全场通用）
规则：满100减10 | 满200减30 | 满300减60

场景1：订单金额 ¥80
  → 未命中任何规则
  → 实付 ¥80
  → 提示："再买20.00元可享满100减10优惠"

场景2：订单金额 ¥150
  → 命中"满100减10"
  → 实付 ¥140
  → 提示："再买50.00元可享满200减30优惠"

场景3：订单金额 ¥350
  → 命中"满300减60"
  → 实付 ¥290
  → 无更高档位提示
```

---

## 6. 管理后台设计

### 6.1 页面结构

```
admin.html
├── 商品管理 Tab
├── 订单管理 Tab
└── 促销管理 Tab [新增]
    ├── 活动列表区
    │   ├── 筛选栏（状态筛选）
    │   ├── 活动表格
    │   └── 操作按钮（新建/编辑/删除/启用/禁用）
    ├── 活动表单区（弹窗）
    │   ├── 活动名称输入
    │   ├── 活动描述输入
    │   ├── 时间范围选择
    │   ├── 适用商品选择（全场/指定）
    │   └── 启用状态开关
    └── 规则配置区（活动选中后展示）
        ├── 规则列表（动态表格）
        ├── 添加规则按钮
        └── 保存规则按钮
```

### 6.2 活动列表字段

| 字段 | 说明 | 排序 |
|------|------|------|
| 活动名称 | 显示名称 | - |
| 活动类型 | 显示"满减" | - |
| 活动状态 | 未开始/进行中/已结束（带颜色标签） | - |
| 时间范围 | 开始时间 ~ 结束时间 | - |
| 启用状态 | 启用/禁用（开关） | - |
| 操作 | 编辑/删除/启用/禁用 | - |

### 6.3 规则配置交互

**动态规则表格：**

| 档位等级 | 满足金额（元） | 减免金额（元） | 操作 |
|----------|----------------|----------------|------|
| 1 | 100 | 10 | 删除 |
| 2 | 200 | 30 | 删除 |
| 3 | 300 | 60 | 删除 |
| - | [输入] | [输入] | 添加 |

**前端校验：**

- 满足金额必须 > 0
- 减免金额必须 > 0 且 < 满足金额
- 档位必须递增
- 至少配置 1 条规则

---

## 7. 实施计划

### 7.1 开发阶段

| 阶段 | 任务 | 交付物 | 预估工时 |
|------|------|--------|----------|
| **Phase 1** | 促销基础框架 | Promotion 实体、Repository、Service、Controller | 1 天 |
| **Phase 2** | 满减规则管理 | FullReductionRule 实体、Repository、规则校验 | 1 天 |
| **Phase 3** | 优惠计算引擎 | PromotionCalculator、计算接口 | 0.5 天 |
| **Phase 4** | 订单集成 | Order 扩展、下单流程改造 | 0.5 天 |
| **Phase 5** | 管理后台 | admin.html 促销管理界面 | 1 天 |
| **Phase 6** | 联调测试 | 端到端测试、Bug修复 | 1 天 |

### 7.2 依赖关系

```
Phase 1: Promotion基础框架
    │
    ▼
Phase 2: 满减规则管理
    │
    ▼
Phase 3: 优惠计算引擎 ◄────── Phase 1 (PromotionService)
    │                           Phase 2 (FullReductionRuleService)
    ▼
Phase 4: 订单集成 ◄────────── Phase 3 (PromotionCalculator)
    │
    ▼
Phase 5: 管理后台 ◄────────── Phase 1,2 (所有API)
    │
    ▼
Phase 6: 联调测试
```

### 7.3 验收标准

| 验收项 | 验收标准 |
|--------|----------|
| 功能完整性 | 所有P0功能正常可用 |
| 接口规范 | 符合Swagger文档定义 |
| 数据准确性 | 优惠计算结果与预期一致 |
| 兼容性 | 现有订单接口正常返回 |
| 性能 | 优惠计算响应 < 50ms |

---

## 8. 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 活动冲突校验遗漏 | 同一商品参与多个活动 | 增加单元测试覆盖所有冲突场景 |
| 金额计算精度问题 | 优惠金额错误 | 统一使用Integer（分）存储，避免浮点数 |
| 并发修改规则 | 数据不一致 | ConcurrentHashMap保证存储线程安全 |
| 订单数据兼容 | 历史订单字段缺失 | totalAmount字段保留并正确赋值 |

---

## 9. 附录

### 9.1 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-04-11 | 初始版本 | - |

### 9.2 参考文档

- [spring-full-reduction-design.md](./spring-full-reduction-design.md) - 需求设计方案
- 现有系统代码库

### 9.3 相关资源

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **管理后台**: http://localhost:8080/admin.html
