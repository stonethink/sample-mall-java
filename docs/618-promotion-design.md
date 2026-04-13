# 618大促活动 - 需求设计方案

## 1. 需求背景

618年中大促是电商行业最重要的促销活动之一。本方案基于现有 Sample Mall 电商平台（商品+订单两大核心模块），设计一套完整的618促销活动功能，涵盖促销活动管理、满减优惠、限时特价、优惠券四大核心能力。

### 1.1 项目现状

| 维度 | 现状 |
|------|------|
| 技术栈 | Spring Boot 2.7.5 + Java 8 |
| 存储方式 | ConcurrentHashMap 内存存储 |
| 核心模块 | 商品(Product) + 订单(Order) |
| 促销能力 | 无 |
| 价格体系 | 商品单一定价（分为单位） |
| 前端 | admin.html 单页管理后台 |

### 1.2 设计目标

- 构建可扩展的促销活动管理体系
- 支持满减、限时特价、优惠券三种核心促销方式
- 与现有商品和订单模块无缝集成
- 提供管理后台促销活动管理界面
- 保持项目轻量化风格（内存存储）

---

## 2. 功能概述

### 2.1 功能模块全景图

```
618促销系统
├── 促销活动管理 (Promotion)
│   ├── 活动创建/编辑/删除
│   ├── 活动状态管理（未开始/进行中/已结束）
│   └── 活动商品关联
├── 满减优惠 (Full Reduction)
│   ├── 阶梯满减规则
│   ├── 订单金额计算
│   └── 满减优惠展示
├── 限时特价 (Flash Sale)
│   ├── 特价商品设置
│   ├── 活动时间控制
│   └── 限购数量控制
└── 优惠券 (Coupon)
    ├── 优惠券创建/发放
    ├── 优惠券核销
    └── 优惠券状态管理
```

### 2.2 功能优先级

| 优先级 | 功能模块 | 说明 |
|--------|----------|------|
| P0 | 促销活动管理 | 基础框架，其他功能依赖 |
| P0 | 满减优惠 | 618最核心的促销方式 |
| P1 | 限时特价 | 引流利器，提升转化 |
| P1 | 优惠券 | 用户触达，提升复购 |

---

## 3. 详细需求设计

### 3.1 促销活动管理 (Promotion)

#### 3.1.1 数据模型

```java
public class Promotion {
    private Long id;                    // 活动ID
    private String name;                // 活动名称，如"618年中大促"
    private String description;         // 活动描述
    private PromotionType type;         // 活动类型：FULL_REDUCTION/FLASH_SALE/COUPON
    private PromotionStatus status;     // 活动状态：NOT_STARTED/ACTIVE/ENDED
    private LocalDateTime startTime;    // 活动开始时间
    private LocalDateTime endTime;      // 活动结束时间
    private List<Long> productIds;      // 参与活动的商品ID列表（空=全场通用）
    private Boolean enabled;            // 是否启用
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}

public enum PromotionType {
    FULL_REDUCTION,  // 满减
    FLASH_SALE,      // 限时特价
    COUPON           // 优惠券
}

public enum PromotionStatus {
    NOT_STARTED,  // 未开始
    ACTIVE,       // 进行中
    ENDED         // 已结束
}
```

#### 3.1.2 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/promotions` | 查询所有促销活动 |
| GET | `/api/promotions/{id}` | 查询单个促销活动详情 |
| GET | `/api/promotions/active` | 查询当前生效的活动 |
| POST | `/api/promotions` | 创建促销活动 |
| PUT | `/api/promotions/{id}` | 更新促销活动 |
| DELETE | `/api/promotions/{id}` | 删除促销活动 |
| PUT | `/api/promotions/{id}/status` | 手动变更活动状态 |

#### 3.1.3 业务规则

1. 活动状态根据时间自动流转：
   - 当前时间 < startTime → NOT_STARTED
   - startTime ≤ 当前时间 ≤ endTime → ACTIVE
   - 当前时间 > endTime → ENDED
2. 同一商品不可同时参与两个相同类型的活动
3. 活动进行中不允许删除，只允许提前结束
4. enabled=false 时活动不生效，即使在活动时间内

---

### 3.2 满减优惠 (Full Reduction)

#### 3.2.1 数据模型

```java
public class FullReductionRule {
    private Long id;                    // 规则ID
    private Long promotionId;           // 关联促销活动ID
    private Integer fullAmount;         // 满足金额（分）
    private Integer reductionAmount;    // 减免金额（分）
    private Integer level;              // 阶梯等级（1=最低档，越大越优惠）
}
```

#### 3.2.2 功能需求

**FR-1: 阶梯满减规则配置**
- 支持为一个促销活动配置多档满减规则
- 示例规则：
  - 满100元减10元
  - 满200元减30元
  - 满300元减60元
- 规则按满足金额从低到高排序

**FR-2: 满减金额计算**
- 用户下单时，根据订单中参与活动商品的总金额，匹配最优的满减规则
- 取满足条件的最高档位满减
- 计算公式：实付金额 = 商品总额 - 满减金额

**FR-3: 满减信息展示**
- 商品详情/列表页展示"满X减Y"标签
- 购物结算时展示满减优惠明细
- 差额提醒："再买XX元可享满减优惠"

#### 3.2.3 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/promotions/{id}/rules` | 查询活动满减规则 |
| POST | `/api/promotions/{id}/rules` | 添加满减规则 |
| PUT | `/api/promotions/{id}/rules/{ruleId}` | 修改满减规则 |
| DELETE | `/api/promotions/{id}/rules/{ruleId}` | 删除满减规则 |
| POST | `/api/promotions/calculate` | 计算订单优惠金额 |

#### 3.2.4 计算示例

```
促销活动：618满减大促
规则：满100减10 | 满200减30 | 满300减60

场景1：订单金额 ¥150
  → 命中"满100减10"
  → 实付 ¥140

场景2：订单金额 ¥280
  → 命中"满200减30"
  → 实付 ¥250

场景3：订单金额 ¥350
  → 命中"满300减60"
  → 实付 ¥290
```

---

### 3.3 限时特价 (Flash Sale)

#### 3.3.1 数据模型

```java
public class FlashSaleItem {
    private Long id;                    // ID
    private Long promotionId;           // 关联促销活动ID
    private Long productId;             // 商品ID
    private Integer flashPrice;         // 特价价格（分）
    private Integer originalPrice;      // 原价（分），冗余存储
    private Integer stockLimit;         // 活动限量库存
    private Integer soldCount;          // 已售数量
    private Integer perUserLimit;       // 每人限购数量
}
```

#### 3.3.2 功能需求

**FS-1: 特价商品设置**
- 管理员可为活动商品设置特价
- 特价必须低于原价
- 支持设置活动限量库存
- 支持设置每人限购数量

**FS-2: 限时特价展示**
- 商品列表展示特价标签和原价划线价
- 展示倒计时（距活动结束）
- 展示剩余库存数量和销售进度

**FS-3: 特价库存控制**
- 下单时扣减活动库存（非商品总库存）
- 活动库存售罄后恢复原价
- 每人限购校验

#### 3.3.3 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/flash-sales` | 查询所有限时特价商品 |
| GET | `/api/flash-sales/active` | 查询进行中的特价商品 |
| GET | `/api/flash-sales/{id}` | 查询特价商品详情 |
| POST | `/api/flash-sales` | 创建限时特价 |
| PUT | `/api/flash-sales/{id}` | 修改限时特价 |
| DELETE | `/api/flash-sales/{id}` | 删除限时特价 |

---

### 3.4 优惠券 (Coupon)

#### 3.4.1 数据模型

```java
public class CouponTemplate {
    private Long id;                    // 优惠券模板ID
    private Long promotionId;           // 关联促销活动ID
    private String name;                // 优惠券名称
    private CouponType couponType;      // 类型：FIXED/PERCENTAGE
    private Integer discountValue;      // 优惠值（FIXED=金额分, PERCENTAGE=折扣百分比）
    private Integer minAmount;          // 使用门槛金额（分），0=无门槛
    private Integer maxDiscount;        // 最大优惠金额（分），用于折扣券封顶
    private Integer totalCount;         // 发行总量
    private Integer remainCount;        // 剩余数量
    private Integer perUserLimit;       // 每人限领数量
    private LocalDateTime validStart;   // 有效期开始
    private LocalDateTime validEnd;     // 有效期结束
    private List<Long> applicableProductIds;  // 适用商品ID列表（空=全场通用）
}

public class UserCoupon {
    private Long id;                    // ID
    private Long templateId;            // 优惠券模板ID
    private String couponCode;          // 优惠券码（唯一）
    private String userId;              // 用户标识（简化处理）
    private CouponStatus status;        // 状态：UNUSED/USED/EXPIRED
    private LocalDateTime receivedAt;   // 领取时间
    private LocalDateTime usedAt;       // 使用时间
    private Long orderId;               // 使用的订单ID
}

public enum CouponType {
    FIXED,       // 固定金额券：满100减20
    PERCENTAGE   // 折扣券：打8折
}

public enum CouponStatus {
    UNUSED,   // 未使用
    USED,     // 已使用
    EXPIRED   // 已过期
}
```

#### 3.4.2 功能需求

**CP-1: 优惠券模板管理**
- 创建优惠券模板（名称、类型、面值、门槛、数量、有效期）
- 支持固定金额券和折扣百分比券
- 设置发行总量和每人限领数量

**CP-2: 优惠券发放与领取**
- 用户领取优惠券，生成唯一优惠券码
- 校验领取资格（剩余数量、每人限领）
- 记录领取时间

**CP-3: 优惠券核销**
- 下单时选择可用优惠券
- 校验优惠券有效性（状态、有效期、门槛金额、适用商品）
- 核销优惠券并关联订单
- 计算优惠金额

**CP-4: 优惠券状态管理**
- 过期自动失效
- 订单取消后优惠券退回

#### 3.4.3 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/coupons/templates` | 查询所有优惠券模板 |
| POST | `/api/coupons/templates` | 创建优惠券模板 |
| PUT | `/api/coupons/templates/{id}` | 修改优惠券模板 |
| DELETE | `/api/coupons/templates/{id}` | 删除优惠券模板 |
| POST | `/api/coupons/receive/{templateId}` | 用户领取优惠券 |
| GET | `/api/coupons/user/{userId}` | 查询用户优惠券列表 |
| POST | `/api/coupons/verify` | 验证优惠券是否可用 |
| POST | `/api/coupons/use` | 核销优惠券 |

#### 3.4.4 优惠券示例

```
优惠券1：618通用满减券
  类型：固定金额
  面值：¥20
  门槛：满¥100可用
  适用范围：全场通用
  数量：1000张

优惠券2：水果品类8折券
  类型：折扣百分比
  折扣：80%（8折）
  最大优惠：¥50
  门槛：满¥50可用
  适用范围：水果品类商品
  数量：500张

优惠券3：新人无门槛券
  类型：固定金额
  面值：¥10
  门槛：无门槛
  适用范围：全场通用
  数量：2000张
```

---

## 4. 订单优惠计算集成

### 4.1 Order 模型扩展

```java
public class Order {
    // 现有字段保留
    private Long id;
    private String orderSn;
    private List<Long> productIds;
    private Integer totalAmount;
    private LocalDateTime createdAt;
    
    // === 新增字段 ===
    private Integer originalAmount;     // 商品原始总金额（分）
    private Integer promotionDiscount;  // 满减优惠金额（分）
    private Integer couponDiscount;     // 优惠券抵扣金额（分）
    private Integer flashSaleDiscount;  // 限时特价优惠金额（分）
    private Integer payAmount;          // 最终支付金额（分）
    private Long couponId;              // 使用的优惠券ID
    private Long promotionId;           // 命中的促销活动ID
}
```

### 4.2 优惠计算流程

```
1. 获取订单商品列表和数量
2. 计算商品原始总金额 (originalAmount)
3. 检查限时特价 → 替换特价商品的价格 → 计算flashSaleDiscount
4. 基于特价后金额，匹配满减规则 → 计算promotionDiscount
5. 校验并应用优惠券 → 计算couponDiscount
6. 计算最终支付金额：
   payAmount = originalAmount - flashSaleDiscount - promotionDiscount - couponDiscount
7. payAmount 最小为0（不会出现负数）
```

### 4.3 优惠叠加规则

| 组合 | 是否允许 | 说明 |
|------|----------|------|
| 满减 + 限时特价 | 是 | 特价商品参与满减计算 |
| 满减 + 优惠券 | 是 | 先满减再抵扣优惠券 |
| 限时特价 + 优惠券 | 是 | 特价后的金额使用优惠券 |
| 三者叠加 | 是 | 按 特价→满减→优惠券 顺序计算 |

---

## 5. 项目结构设计

### 5.1 新增包结构

```
src/main/java/com/example/mall/
├── product/                       # 现有
├── order/                         # 现有（扩展）
└── promotion/                     # 新增：促销领域
    ├── Promotion.java             # 促销活动实体
    ├── PromotionType.java         # 活动类型枚举
    ├── PromotionStatus.java       # 活动状态枚举
    ├── PromotionController.java   # 促销活动API
    ├── PromotionService.java      # 促销活动业务逻辑
    ├── PromotionRepository.java   # 促销活动数据存储
    ├── FullReductionRule.java     # 满减规则实体
    ├── FlashSaleItem.java         # 限时特价实体
    ├── CouponTemplate.java        # 优惠券模板实体
    ├── CouponType.java            # 优惠券类型枚举
    ├── UserCoupon.java            # 用户优惠券实体
    ├── CouponStatus.java          # 优惠券状态枚举
    ├── CouponController.java      # 优惠券API
    ├── CouponService.java         # 优惠券业务逻辑
    ├── CouponRepository.java      # 优惠券数据存储
    ├── FlashSaleController.java   # 限时特价API
    ├── FlashSaleService.java      # 限时特价业务逻辑
    ├── FlashSaleRepository.java   # 限时特价数据存储
    └── PromotionCalculator.java   # 优惠计算引擎
```

### 5.2 新增示例数据

```
src/main/resources/
├── promotions.json                # 促销活动示例数据
├── flash-sales.json               # 限时特价示例数据
└── coupons.json                   # 优惠券模板示例数据
```

### 5.3 前端扩展

在 `admin.html` 中新增以下Tab页面：
- **促销活动** - 活动列表、创建/编辑活动
- **满减规则** - 规则配置管理
- **限时特价** - 特价商品管理
- **优惠券** - 优惠券模板管理、发放记录

---

## 6. 示例数据设计

### 6.1 促销活动示例

```json
[
  {
    "id": 1,
    "name": "618全场满减大促",
    "description": "618年中大促，全场满100减10，满200减30，满300减60",
    "type": "FULL_REDUCTION",
    "status": "ACTIVE",
    "startTime": "2026-06-01T00:00:00",
    "endTime": "2026-06-18T23:59:59",
    "productIds": [],
    "enabled": true
  },
  {
    "id": 2,
    "name": "618水果限时特价",
    "description": "精选水果限时特价，低至5折",
    "type": "FLASH_SALE",
    "status": "ACTIVE",
    "startTime": "2026-06-16T10:00:00",
    "endTime": "2026-06-18T22:00:00",
    "productIds": [1, 2, 3, 4, 5],
    "enabled": true
  },
  {
    "id": 3,
    "name": "618优惠券雨",
    "description": "618海量优惠券限时发放",
    "type": "COUPON",
    "status": "ACTIVE",
    "startTime": "2026-06-10T00:00:00",
    "endTime": "2026-06-18T23:59:59",
    "productIds": [],
    "enabled": true
  }
]
```

### 6.2 满减规则示例

```json
[
  { "id": 1, "promotionId": 1, "fullAmount": 10000, "reductionAmount": 1000, "level": 1 },
  { "id": 2, "promotionId": 1, "fullAmount": 20000, "reductionAmount": 3000, "level": 2 },
  { "id": 3, "promotionId": 1, "fullAmount": 30000, "reductionAmount": 6000, "level": 3 }
]
```

### 6.3 限时特价示例

```json
[
  {
    "id": 1,
    "promotionId": 2,
    "productId": 1,
    "flashPrice": 2490,
    "originalPrice": 4980,
    "stockLimit": 100,
    "soldCount": 35,
    "perUserLimit": 2
  },
  {
    "id": 2,
    "promotionId": 2,
    "productId": 2,
    "flashPrice": 9900,
    "originalPrice": 15800,
    "stockLimit": 50,
    "soldCount": 22,
    "perUserLimit": 1
  }
]
```

### 6.4 优惠券模板示例

```json
[
  {
    "id": 1,
    "promotionId": 3,
    "name": "618满100减20通用券",
    "couponType": "FIXED",
    "discountValue": 2000,
    "minAmount": 10000,
    "maxDiscount": 2000,
    "totalCount": 1000,
    "remainCount": 680,
    "perUserLimit": 3,
    "validStart": "2026-06-10T00:00:00",
    "validEnd": "2026-06-18T23:59:59",
    "applicableProductIds": []
  },
  {
    "id": 2,
    "promotionId": 3,
    "name": "618水果8折券",
    "couponType": "PERCENTAGE",
    "discountValue": 80,
    "minAmount": 5000,
    "maxDiscount": 5000,
    "totalCount": 500,
    "remainCount": 320,
    "perUserLimit": 1,
    "validStart": "2026-06-10T00:00:00",
    "validEnd": "2026-06-18T23:59:59",
    "applicableProductIds": [1, 2, 3, 4, 5]
  }
]
```

---

## 7. 非功能性需求

### 7.1 性能要求

- 优惠计算响应时间 < 100ms
- 优惠券领取支持并发（ConcurrentHashMap 保证线程安全）
- 限时特价库存扣减使用原子操作

### 7.2 数据一致性

- 优惠券核销与订单创建需保证原子性
- 限时特价库存扣减需防止超卖
- 活动状态变更需同步更新相关优惠券/特价

### 7.3 扩展性考虑

- 促销类型使用枚举，未来可扩展更多类型（如拼团、秒杀）
- 优惠计算引擎使用策略模式，便于增加新的优惠计算规则
- 数据层接口化，未来可平滑替换为数据库实现

---

## 8. 实施步骤建议

### 第一步：基础框架搭建
- 创建 promotion 包结构
- 实现 Promotion 实体、Repository、Service、Controller
- 加载促销活动示例数据

### 第二步：满减功能实现
- 实现 FullReductionRule 实体和存储
- 实现满减规则 CRUD API
- 实现满减金额计算逻辑
- 扩展 Order 模型，集成优惠计算

### 第三步：限时特价实现
- 实现 FlashSaleItem 实体和存储
- 实现限时特价 CRUD API
- 实现特价库存控制逻辑
- 限时特价商品展示集成

### 第四步：优惠券系统实现
- 实现 CouponTemplate 和 UserCoupon 实体
- 实现优惠券模板管理 API
- 实现优惠券领取和核销 API
- 实现优惠券有效期管理

### 第五步：集成与优化
- 实现 PromotionCalculator 优惠计算引擎
- 集成三种优惠的叠加计算
- 扩展 admin.html 管理后台
- 编写 Swagger 文档注解

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| 满减 (Full Reduction) | 订单金额满足一定条件后减免指定金额 |
| 限时特价 (Flash Sale) | 在限定时间内以低于原价的价格出售商品 |
| 优惠券 (Coupon) | 可抵扣订单金额的虚拟券 |
| 阶梯满减 | 设置多个满减档位，金额越高优惠越大 |
| 核销 | 使用优惠券并使其失效的过程 |
| 门槛金额 | 使用优惠券的最低订单金额要求 |

### B. 价格单位说明

本系统所有金额字段均以**分**为单位存储（与现有系统保持一致）：
- 存储值 4980 = 实际金额 ¥49.80
- 存储值 10000 = 实际金额 ¥100.00
- 前端展示时需除以100转换为元
