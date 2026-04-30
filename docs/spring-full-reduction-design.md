# 春季满减促销 - 需求设计方案

## 1. 需求背景

### 1.1 业务背景

春季是电商平台的传统促销旺季，平台计划推出"春季满减促销"活动，通过阶梯式满减优惠刺激用户消费，提升客单价和订单转化率。本次需求聚焦于**满减促销**这一核心能力，为平台搭建可复用的促销基础框架。


### 1.2 项目现状


| 维度     | 现状                        |
| ---------- | ----------------------------- |
| 技术栈   | Spring Boot 2.7.5 + Java 8  |
| 存储方式 | ConcurrentHashMap 内存存储  |
| 核心模块 | 商品(Product) + 订单(Order) |
| 促销能力 | 无                          |
| 价格体系 | 商品单一定价（分为单位）    |
| 前端     | admin.html 单页管理后台     |
| 数据加载 | JSON 文件初始化示例数据     |

### 1.3 设计目标

- 构建促销活动管理基础框架，为后续扩展其他促销类型（限时特价、优惠券等）奠定基础
- 实现阶梯满减规则的配置和计算能力
- 与现有商品和订单模块无缝集成，下单时自动应用满减优惠
- 提供管理后台促销管理界面
- 保持项目轻量化风格（内存存储、JSON 数据加载）

### 1.4 需求范围


| 范围   | 说明                                                   |
| -------- | -------------------------------------------------------- |
| 包含   | 促销活动管理、满减规则配置、订单满减计算、管理后台界面 |
| 不包含 | 限时特价、优惠券、拼团等其他促销方式（后续迭代）       |

---

## 2. 功能概述

### 2.1 功能模块图

```
春季满减促销系统
├── 促销活动管理 (Promotion)
│   ├── 活动创建/编辑/删除
│   ├── 活动状态管理（未开始/进行中/已结束）
│   ├── 活动启用/禁用
│   └── 活动商品关联（全场/指定商品）
├── 满减规则管理 (FullReductionRule)
│   ├── 阶梯满减规则配置
│   ├── 规则排序与校验
│   └── 规则CRUD操作
├── 订单优惠计算 (PromotionCalculator)
│   ├── 活动商品匹配
│   ├── 满减规则匹配（取最优档位）
│   ├── 优惠金额计算
│   └── 差额提醒
└── 管理后台 (Admin UI)
    ├── 促销活动列表/表单
    └── 满减规则配置面板
```

### 2.2 功能优先级


| 优先级 | 功能           | 说明                   |
| -------- | ---------------- | ------------------------ |
| P0     | 促销活动 CRUD  | 基础框架，满减规则依赖 |
| P0     | 满减规则 CRUD  | 核心业务能力           |
| P0     | 订单满减计算   | 业务闭环必需           |
| P1     | Order 模型扩展 | 订单中记录优惠明细     |
| P1     | 管理后台界面   | 运营操作入口           |
| P2     | 差额提醒接口   | 提升用户体验           |

---

## 3. 详细需求设计

### 3.1 促销活动管理 (Promotion)

#### 3.1.1 数据模型

```java
public class Promotion {
    private Long id;                    // 活动ID
    private String name;                // 活动名称，如"春季满减大促"
    private String description;         // 活动描述
    private PromotionType type;         // 活动类型：FULL_REDUCTION（本期仅满减）
    private PromotionStatus status;     // 活动状态：NOT_STARTED/ACTIVE/ENDED
    private LocalDateTime startTime;    // 活动开始时间
    private LocalDateTime endTime;      // 活动结束时间
    private List<Long> productIds;      // 参与活动的商品ID列表（空=全场通用）
    private Boolean enabled;            // 是否启用
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}

public enum PromotionType {
    FULL_REDUCTION  // 满减（本期唯一类型，预留枚举方便后续扩展）
}

public enum PromotionStatus {
    NOT_STARTED,  // 未开始
    ACTIVE,       // 进行中
    ENDED         // 已结束
}
```

#### 3.1.2 API 设计


| 方法   | 路径                           | 说明                                     |
| -------- | -------------------------------- | ------------------------------------------ |
| GET    | `/api/promotions`              | 查询所有促销活动（支持按状态筛选）       |
| GET    | `/api/promotions/{id}`         | 查询单个促销活动详情（含关联的满减规则） |
| GET    | `/api/promotions/active`       | 查询当前生效的活动列表                   |
| POST   | `/api/promotions`              | 创建促销活动                             |
| PUT    | `/api/promotions/{id}`         | 更新促销活动                             |
| DELETE | `/api/promotions/{id}`         | 删除促销活动                             |
| PUT    | `/api/promotions/{id}/enable`  | 启用活动                                 |
| PUT    | `/api/promotions/{id}/disable` | 禁用活动                                 |

#### 3.1.3 业务规则

1. **状态自动流转**：
   - 当前时间 < startTime → `NOT_STARTED`
   - startTime <= 当前时间 <= endTime → `ACTIVE`
   - 当前时间 > endTime → `ENDED`
2. **活动冲突校验**：同一商品不可同时参与两个 FULL_REDUCTION 类型的活动（全场活动视为所有商品参与）
3. **删除保护**：活动状态为 ACTIVE 时不允许删除，只能先禁用
4. **启用控制**：`enabled=false` 时活动不生效，即使在活动时间范围内
5. **时间校验**：创建/编辑时，endTime 必须大于 startTime

#### 3.1.4 请求/响应示例

**创建促销活动 - 请求**

```json
POST /api/promotions
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

**创建促销活动 - 响应**

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

---

### 3.2 满减规则管理 (FullReductionRule)

#### 3.2.1 数据模型

```java
public class FullReductionRule {
    private Long id;                    // 规则ID
    private Long promotionId;           // 关联促销活动ID
    private Integer fullAmount;         // 满足金额（分）
    private Integer reductionAmount;    // 减免金额（分）
    private Integer level;              // 阶梯等级（1=最低档，数值越大优惠力度越大）
}
```

#### 3.2.2 API 设计


| 方法   | 路径                                  | 说明                                    |
| -------- | --------------------------------------- | ----------------------------------------- |
| GET    | `/api/promotions/{id}/rules`          | 查询活动下所有满减规则（按 level 升序） |
| POST   | `/api/promotions/{id}/rules`          | 为活动添加满减规则                      |
| PUT    | `/api/promotions/{id}/rules/{ruleId}` | 修改满减规则                            |
| DELETE | `/api/promotions/{id}/rules/{ruleId}` | 删除满减规则                            |
| POST   | `/api/promotions/{id}/rules/batch`    | 批量设置满减规则（覆盖原有规则）        |

#### 3.2.3 业务规则

1. **阶梯递增约束**：level 越大，fullAmount 和 reductionAmount 均必须大于低档位
2. **金额合理性校验**：reductionAmount 必须小于 fullAmount（减免金额不能超过满足金额）
3. **活动关联校验**：promotionId 对应的活动必须存在，且类型为 FULL_REDUCTION
4. **最少规则数**：一个满减活动至少需要配置 1 条规则
5. **活动进行中限制**：活动状态为 ACTIVE 时，允许新增规则但不允许删除已有规则（防止影响进行中的订单）

#### 3.2.4 请求/响应示例

**批量设置满减规则 - 请求**

```json
POST /api/promotions/1/rules/batch
[
    { "fullAmount": 10000, "reductionAmount": 1000, "level": 1 },
    { "fullAmount": 20000, "reductionAmount": 3000, "level": 2 },
    { "fullAmount": 30000, "reductionAmount": 6000, "level": 3 }
]
```

对应的业务含义：

- 满 100 元减 10 元
- 满 200 元减 30 元
- 满 300 元减 60 元

---

### 3.3 订单优惠计算 (PromotionCalculator)

#### 3.3.1 计算流程

```
1. 接收订单商品ID列表
2. 查询商品价格，计算商品原始总金额 (originalAmount)
3. 查询当前生效的满减活动（enabled=true 且 status=ACTIVE）
4. 筛选适用活动：
   a. 活动 productIds 为空 → 全场通用，所有商品参与
   b. 活动 productIds 非空 → 仅订单中匹配的商品金额参与计算
5. 根据参与金额匹配满减规则（取满足条件的最高档位）
6. 计算优惠金额 (promotionDiscount)
7. 计算最终支付金额：payAmount = originalAmount - promotionDiscount
8. payAmount 最小为 0
```

#### 3.3.2 API 设计


| 方法 | 路径                        | 说明                           |
| ------ | ----------------------------- | -------------------------------- |
| POST | `/api/promotions/calculate` | 计算订单满减优惠（下单前预览） |

**计算优惠 - 请求**

```json
POST /api/promotions/calculate
{
    "productIds": [1, 2, 3]
}
```

**计算优惠 - 响应**

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

#### 3.3.3 计算示例

```
促销活动：春季满减大促（全场通用）
规则：满100减10 | 满200减30 | 满300减60

场景1：订单金额 ¥80
  → 未命中任何规则
  → 实付 ¥80
  → 提示："再买20.00元可享满100减10优惠"

场景2：订单金额 ¥150
  → 命中"满100减10"
  → 实付 ¥140
  → 提示："再买50.00元可享满200减30优惠"

场景3：订单金额 ¥280
  → 命中"满200减30"
  → 实付 ¥250
  → 提示："再买20.00元可享满300减60优惠"

场景4：订单金额 ¥350
  → 命中"满300减60"
  → 实付 ¥290
  → 无更高档位提示

场景5：指定商品活动，订单含活动商品¥120 + 非活动商品¥80
  → 仅活动商品金额 ¥120 参与计算
  → 命中"满100减10"
  → 实付 ¥190（120 - 10 + 80）
```

---

### 3.4 Order 模型扩展

#### 3.4.1 新增字段

```java
public class Order {
    // === 现有字段保留 ===
    private Long id;
    private String orderSn;
    private List<Long> productIds;
    private Integer totalAmount;        // 保留兼容，值等于 payAmount
    private LocalDateTime createdAt;

    // === 新增字段 ===
    private Integer originalAmount;     // 商品原始总金额（分）
    private Integer promotionDiscount;  // 满减优惠金额（分），无优惠则为0
    private Integer payAmount;          // 最终支付金额（分）
    private Long promotionId;           // 命中的满减活动ID，无则为null
}
```

#### 3.4.2 下单流程变更

```
原流程：
  创建订单 → 保存订单

新流程：
  创建订单 → 计算商品原始总金额 → 匹配满减活动并计算优惠 → 填充优惠字段 → 保存订单
```

#### 3.4.3 兼容性说明

- `totalAmount` 字段保留，其值设为 `payAmount`（即优惠后的实付金额），保证现有接口兼容
- 未命中任何活动时：`originalAmount = payAmount = totalAmount`，`promotionDiscount = 0`，`promotionId = null`

---

### 3.5 管理后台界面

#### 3.5.1 页面结构

在 `admin.html` 中新增 **促销管理** Tab，包含以下内容：

**活动列表区**

- 表格展示所有促销活动：名称、类型、状态、时间范围、启用状态
- 支持按状态筛选（全部/未开始/进行中/已结束）
- 操作按钮：编辑、删除、启用/禁用
- 新建活动按钮

**活动表单区（新建/编辑）**

- 活动名称（文本输入）
- 活动描述（文本输入）
- 活动时间范围（开始时间 - 结束时间）
- 适用商品（全场通用 / 指定商品多选）
- 是否启用（开关）

**满减规则配置区**

- 选中活动后展示关联的满减规则列表
- 支持动态添加/删除规则行
- 每行包含：满足金额、减免金额、档位等级
- 一键保存按钮（批量提交规则）

---

## 4. 项目结构设计

### 4.1 新增包结构

```
src/main/java/com/example/mall/
├── product/                        # 现有，无变更
├── order/                          # 现有，扩展 Order 模型和 OrderService
│   ├── Order.java                  # 扩展：新增 originalAmount/promotionDiscount/payAmount/promotionId 字段
│   └── OrderService.java           # 扩展：下单时调用优惠计算
└── promotion/                      # 新增：促销领域
    ├── Promotion.java              # 促销活动实体
    ├── PromotionType.java          # 活动类型枚举
    ├── PromotionStatus.java        # 活动状态枚举
    ├── PromotionController.java    # 促销活动 REST API
    ├── PromotionService.java       # 促销活动业务逻辑
    ├── PromotionRepository.java    # 促销活动数据存储（ConcurrentHashMap）
    ├── FullReductionRule.java      # 满减规则实体
    ├── FullReductionRuleRepository.java  # 满减规则数据存储
    └── PromotionCalculator.java    # 满减优惠计算引擎
```

### 4.2 新增资源文件

```
src/main/resources/
├── promotions.json                 # 促销活动示例数据
└── full-reduction-rules.json       # 满减规则示例数据
```

### 4.3 依赖关系

```
PromotionController
  └── PromotionService
        ├── PromotionRepository
        ├── FullReductionRuleRepository
        └── PromotionCalculator
              └── ProductService（查询商品价格）

OrderService（扩展）
  └── PromotionCalculator（下单时计算优惠）
```

---

## 5. 示例数据设计

### 5.1 促销活动示例 (promotions.json)

```json
[
    {
        "id": 1,
        "name": "春季满减大促",
        "description": "春暖花开，全场满100减10，满200减30，满300减60",
        "type": "FULL_REDUCTION",
        "startTime": "2026-03-20T00:00:00",
        "endTime": "2026-04-30T23:59:59",
        "productIds": [],
        "enabled": true
    },
    {
        "id": 2,
        "name": "春季生鲜专区满减",
        "description": "生鲜商品专享，满50减5，满100减15",
        "type": "FULL_REDUCTION",
        "startTime": "2026-04-01T00:00:00",
        "endTime": "2026-04-15T23:59:59",
        "productIds": [1, 2, 3],
        "enabled": true
    },
    {
        "id": 3,
        "name": "五一预热满减",
        "description": "五一黄金周预热，全场满150减20，满300减50",
        "type": "FULL_REDUCTION",
        "startTime": "2026-04-25T00:00:00",
        "endTime": "2026-05-05T23:59:59",
        "productIds": [],
        "enabled": false
    }
]
```

### 5.2 满减规则示例 (full-reduction-rules.json)

```json
[
    { "id": 1, "promotionId": 1, "fullAmount": 10000, "reductionAmount": 1000, "level": 1 },
    { "id": 2, "promotionId": 1, "fullAmount": 20000, "reductionAmount": 3000, "level": 2 },
    { "id": 3, "promotionId": 1, "fullAmount": 30000, "reductionAmount": 6000, "level": 3 },
    { "id": 4, "promotionId": 2, "fullAmount": 5000,  "reductionAmount": 500,  "level": 1 },
    { "id": 5, "promotionId": 2, "fullAmount": 10000, "reductionAmount": 1500, "level": 2 },
    { "id": 6, "promotionId": 3, "fullAmount": 15000, "reductionAmount": 2000, "level": 1 },
    { "id": 7, "promotionId": 3, "fullAmount": 30000, "reductionAmount": 5000, "level": 2 }
]
```

---

## 6. 非功能性需求

### 6.1 性能要求

- 满减优惠计算响应时间 < 50ms（内存操作，应远低于此值）
- 活动状态查询支持按需计算，无需定时任务

### 6.2 数据一致性

- 下单时满减计算与订单保存需保证原子性（单线程内即可，ConcurrentHashMap 保证存储线程安全）
- 活动禁用后，已生成的订单优惠记录不受影响

### 6.3 扩展性考虑

- `PromotionType` 使用枚举设计，后续可新增 `FLASH_SALE`、`COUPON` 等类型
- `PromotionCalculator` 使用策略模式，不同促销类型对应不同计算策略，便于扩展
- Repository 层接口化设计，后续可替换为数据库实现
- Order 模型预留了 `promotionDiscount` 等字段，后续增加优惠券时仅需新增对应字段

---

## 7. 实施步骤

### 第一步：促销基础框架搭建

- 创建 `promotion` 包结构
- 实现 `Promotion`、`PromotionType`、`PromotionStatus` 实体/枚举
- 实现 `PromotionRepository`（ConcurrentHashMap 存储 + JSON 数据加载）
- 实现 `PromotionService`（CRUD + 状态自动计算 + 活动冲突校验）
- 实现 `PromotionController`（REST API）

### 第二步：满减规则管理

- 实现 `FullReductionRule` 实体
- 实现 `FullReductionRuleRepository`（ConcurrentHashMap 存储 + JSON 数据加载）
- 在 `PromotionService` 中增加满减规则 CRUD 逻辑和校验
- 暴露规则管理 API（含批量设置接口）

### 第三步：满减计算引擎

- 实现 `PromotionCalculator`
- 实现活动商品匹配逻辑
- 实现阶梯满减规则匹配与金额计算
- 实现差额提醒（nextRule + gap + hint）
- 暴露 `/api/promotions/calculate` 接口

### 第四步：订单集成

- 扩展 `Order` 实体，新增 `originalAmount`、`promotionDiscount`、`payAmount`、`promotionId` 字段
- 修改 `OrderService.create()`，在下单时自动调用 `PromotionCalculator` 计算优惠
- 确保 `totalAmount` 兼容（赋值为 payAmount）

### 第五步：管理后台界面

- 在 `admin.html` 新增"促销管理"Tab
- 实现活动列表展示（含状态筛选）
- 实现活动新建/编辑表单
- 实现满减规则动态配置面板
- 前后端联调

---

## 附录

### A. 术语表


| 术语                  | 说明                                       |
| ----------------------- | -------------------------------------------- |
| 满减 (Full Reduction) | 订单金额达到指定门槛后，减免固定金额       |
| 阶梯满减              | 设置多个满减档位，消费金额越高优惠力度越大 |
| 全场通用              | productIds 为空，表示所有商品均参与活动    |
| 指定商品              | productIds 非空，仅列表中的商品参与活动    |
| 差额提醒              | 提示用户距离下一档满减还差多少金额         |

### B. 价格单位说明

本系统所有金额字段均以**分**为单位存储（与现有系统保持一致）：

- 存储值 10000 = 实际金额 ¥100.00
- 存储值 3000 = 实际金额 ¥30.00
- 前端展示时需除以 100 转换为元

### C. 与 618 大促方案的关系

本方案是 618 大促整体方案中**促销活动管理 + 满减优惠**部分的独立落地版本：

- 复用 618 方案中的 `Promotion`、`PromotionType`、`PromotionStatus`、`FullReductionRule` 数据模型设计
- 复用 618 方案中的 API 路径规范和包结构设计
- 后续 618 大促的限时特价、优惠券功能可在本次搭建的基础框架上继续扩展
