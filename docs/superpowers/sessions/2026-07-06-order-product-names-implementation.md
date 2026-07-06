# 订单列表商品名称显示功能实现

## 概述

本会话完成了订单列表中显示商品名称的功能实现，包括后端 API 改造、前端页面更新以及相关问题的排查与修复。

---

## 技术方案

### 1. DTO 设计

创建了 `OrderWithProductsDTO`，在原 `Order` 实体基础上增加 `products` 字段，用于传输商品详情列表。

**ProductInfo.java** - 轻量级商品信息类：
```java
public class ProductInfo {
    private Long id;
    private String name;
    // Getters and setters
}
```

**OrderWithProductsDTO.java** - 订单增强DTO：
```java
public class OrderWithProductsDTO {
    private Long id;
    private String orderSn;
    private List<ProductInfo> products;
    private List<Long> productIds;  // 保持向后兼容
    private Integer totalAmount;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private Long userId;
    
    public static OrderWithProductsDTO fromOrder(Order order, List<ProductInfo> products) {
        // 映射逻辑
    }
}
```

### 2. Service 层改造

在 `OrderService` 中注入 `ProductRepository`，实现商品名称查询逻辑：

```java
private OrderWithProductsDTO convertToDTO(Order order) {
    List<ProductInfo> productInfos = new ArrayList<>();
    if (order.getProductIds() != null && productRepository != null) {
        for (Long productId : order.getProductIds()) {
            ProductInfo info = new ProductInfo();
            info.setId(productId);
            Optional<Product> product = productRepository.findById(productId);
            info.setName(product.map(Product::getName).orElse("商品已下架或不存在"));
            productInfos.add(info);
        }
    }
    return OrderWithProductsDTO.fromOrder(order, productInfos);
}
```

### 3. Controller 层更新

修改 `OrderController` 的 `listAll()` 和 `getById()` 方法，返回 `OrderWithProductsDTO` 类型。

---

## 问题排查与修复

### 问题1：Spring 构造函数注入错误

**错误信息**：`No default constructor found` for `OrderService`

**修复方案**：为构造函数添加 `@Autowired` 注解

```java
@Autowired
public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
    this.orderRepository = orderRepository;
    this.productRepository = productRepository;
}
```

### 问题2：端口 8080 被占用

**修复方案**：终止占用端口的进程并重启应用

### 问题3：前端报错 - productIds 未定义

**根本原因**：前端 `admin.html` 使用 `order.productIds.join(', ')`，但 `OrderWithProductsDTO` 缺少该字段

**修复方案**：在 `OrderWithProductsDTO` 中添加 `productIds` 字段保持向后兼容

---

## 前端同步修改

### 修改文件：admin.html

**1. 表格表头更新**（3处）：
- 将 "商品ID" 改为 "商品名称"

**2. 表格内容渲染更新**（第1799行）：
```javascript
// 修改前
<td>${order.productIds.join(', ')}</td>

// 修改后
<td>${order.products && order.products.length > 0 ? order.products.map(p => p.name).join(', ') : '-'}</td>
```

---

## API 响应示例

```json
{
    "id": 1,
    "orderSn": "ORDER-20260201-001",
    "products": [
        {"id": 1, "name": "青岛经典啤酒 5瓶装"},
        {"id": 3, "name": "赣南脐橙果冻橙 500g"},
        {"id": 6, "name": "三只松鼠坚果礼盒 750g"}
    ],
    "productIds": [1, 3, 6],
    "totalAmount": 23860,
    "createdAt": "2026-02-01T09:30:00",
    "status": "PENDING_PAYMENT",
    "userId": null
}
```

---

## 修改的文件清单

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/java/com/example/mall/order/ProductInfo.java` | 新建 - 商品信息DTO |
| `src/main/java/com/example/mall/order/OrderWithProductsDTO.java` | 新建 - 订单增强DTO |
| `src/main/java/com/example/mall/order/OrderService.java` | 修改 - 添加商品名称查询逻辑 |
| `src/main/java/com/example/mall/order/OrderController.java` | 修改 - 返回 OrderWithProductsDTO |
| `src/main/resources/static/admin.html` | 修改 - 显示商品名称 |

---

## Git 提交记录

```
d219aa2 fix(order): OrderWithProductsDTO 添加 productIds 字段保持前端兼容性
cdeb786 fix(admin): 订单列表改为显示商品名称
```

---

## 功能验证

API 测试结果显示订单列表成功返回商品名称：

| ID | 订单号 | 商品名称 |
|----|--------|---------|
| 1 | ORDER-20260201-001 | 青岛经典啤酒 5瓶装, 赣南脐橙果冻橙 500g, 三只松鼠坚果礼盒 750g |
| 2 | ORDER-20260201-002 | 烟台红富士苹果 2-3斤, 海南香蕉 3斤装 |
| ... | ... | ... |

---

## 总结

本会话成功实现了订单列表显示商品名称的功能，采用 DTO 模式避免修改原有实体，通过 Service 层关联查询获取商品信息，并同步更新前端页面展示商品名称而非商品ID。过程中排查并修复了构造函数注入、端口占用、前端兼容性等问题，确保功能完整可用。
