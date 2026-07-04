---
title: 订单列表商品显示名称设计
date: 2026-07-04
status: approved
---

# 订单列表商品显示名称设计

## 1. 需求背景

当前订单列表接口返回的 `productIds` 字段仅包含商品ID列表，用户无法直接看到商品名称。需要在订单列表中显示完整的商品信息（包含ID和名称），提升用户体验。

## 2. 设计方案

### 2.1 架构概述

采用DTO模式，在查询时从ProductRepository中获取商品详情，将Order实体转换为包含商品信息的DTO返回给客户端。

**数据流向：**
1. 客户端请求订单列表 → `OrderController`
2. `OrderController` 调用 `OrderService.listAllWithProducts()`
3. `OrderService` 从 `OrderRepository` 获取订单列表
4. `OrderService` 从 `ProductRepository` 获取商品详情
5. `OrderService` 转换为 `OrderWithProductsDTO`
6. 返回给客户端

### 2.2 新增类

#### 2.2.1 ProductInfo

轻量级商品信息类，用于订单显示。

```java
package com.example.mall.order;

public class ProductInfo {
    private Long id;
    private String name;
    
    // getter/setter
}
```

#### 2.2.2 OrderWithProductsDTO

订单详情DTO，包含订单信息和商品列表。

```java
package com.example.mall.order;

import java.time.LocalDateTime;
import java.util.List;

public class OrderWithProductsDTO {
    private Long id;
    private String orderSn;
    private List<ProductInfo> products;
    private Integer totalAmount;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private Long userId;
    
    public static OrderWithProductsDTO fromOrder(Order order, List<Product> products);
}
```

### 2.3 修改类

#### 2.3.1 OrderService

- 注入 `ProductRepository`
- 新增方法：
  - `listAllWithProducts()` - 获取所有订单（含商品信息）
  - `listByStatusWithProducts(OrderStatus status)` - 按状态获取订单（含商品信息）
  - `findByIdWithProducts(Long id)` - 获取单个订单（含商品信息）
- 内部方法：
  - `convertToDTO(Order order)` - 将Order转换为OrderWithProductsDTO

#### 2.3.2 OrderController

- 修改 `listAll()` 返回 `OrderWithProductsDTO` 列表
- 修改 `getById()` 返回 `OrderWithProductsDTO`

### 2.4 错误处理

| 场景 | 处理方式 |
|------|----------|
| 商品ID不存在 | 显示占位名称："商品已下架或不存在" |
| 订单无商品 | 返回空列表 |

### 2.5 API响应示例

**修改前：**
```json
{
  "id": 1,
  "orderSn": "ORDER-20260201-001",
  "productIds": [1, 3, 6],
  "totalAmount": 23860,
  "status": "PENDING_PAYMENT"
}
```

**修改后：**
```json
{
  "id": 1,
  "orderSn": "ORDER-20260201-001",
  "products": [
    {"id": 1, "name": "新疆阿克苏苹果 5斤装"},
    {"id": 3, "name": "智利进口车厘子 500g"},
    {"id": 6, "name": "三只松鼠每日坚果 750g"}
  ],
  "totalAmount": 23860,
  "status": "PENDING_PAYMENT"
}
```

## 3. 实施计划

1. 创建 `ProductInfo` 类
2. 创建 `OrderWithProductsDTO` 类
3. 修改 `OrderService` 添加商品信息查询和DTO转换逻辑
4. 修改 `OrderController` 返回DTO
5. 测试验证

## 4. 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 商品被删除导致订单显示异常 | 低 | 使用占位名称处理 |
| 性能问题 | 低 | 内存查询，O(1)复杂度 |
