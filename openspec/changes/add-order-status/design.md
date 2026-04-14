## Context

当前 `Order` 实体仅包含 `id`、`orderSn`、`productIds`、`totalAmount`、`createdAt` 五个字段，没有状态概念。所有订单在系统中没有生命周期区分，无法支撑后续的支付确认、发货、签收等业务流程。项目采用内存存储（`ConcurrentHashMap`），无持久化数据库，JSON 文件作为初始数据源。

## Goals / Non-Goals

**Goals:**
- 为订单增加状态枚举字段，覆盖基本电商订单生命周期
- 定义清晰的状态流转规则，防止非法状态跳转
- 提供状态变更 API 端点
- 支持按状态筛选订单列表
- 保持与现有 API 的向后兼容（新增字段，不删除已有字段）

**Non-Goals:**
- 不涉及支付系统对接或真实支付流程
- 不涉及物流系统对接
- 不涉及前端 `admin.html` 页面的改造
- 不涉及订单状态变更的事件通知或消息推送
- 不涉及订单超时自动取消等定时任务

## Decisions

### Decision 1: 状态枚举设计

使用 Java 枚举 `OrderStatus` 定义五种状态：

| 枚举值 | 中文名 | 说明 |
|--------|--------|------|
| `PENDING_PAYMENT` | 待付款 | 订单创建后的初始状态 |
| `PAID` | 已付款 | 用户完成支付 |
| `SHIPPED` | 已发货 | 商家已发货 |
| `COMPLETED` | 已完成 | 用户确认收货 |
| `CANCELLED` | 已取消 | 订单被取消 |

**替代方案**：使用字符串常量或整型编码。枚举类型更安全、可读性更好，且 Jackson 默认支持枚举序列化/反序列化。

### Decision 2: 状态流转规则

采用有限状态机思路，合法的状态转换路径：

```
PENDING_PAYMENT → PAID        （确认付款）
PENDING_PAYMENT → CANCELLED   （取消订单）
PAID → SHIPPED                （确认发货）
PAID → CANCELLED              （取消订单）
SHIPPED → COMPLETED           （确认收货）
```

非法跳转（如 `COMPLETED → PENDING_PAYMENT`）将抛出 `IllegalStateException`。

**替代方案**：不做流转校验，允许任意状态变更。选择校验方案是为了保证数据一致性，防止误操作。

### Decision 3: API 设计

状态变更使用独立端点 `PUT /api/orders/{id}/status`，请求体为 `{"status": "PAID"}`。

**理由**：将状态变更与订单编辑（`PUT /api/orders/{id}`）分离，语义更清晰，也便于后续加权限控制。

按状态筛选通过查询参数实现：`GET /api/orders?status=PAID`。

### Decision 4: 数据层处理

- `Order` 实体新增 `status` 字段（类型 `OrderStatus`）
- `OrderRepository` 新增 `findByStatus(OrderStatus status)` 方法，遍历内存存储过滤
- `orders.json` 为每条数据添加 `status` 字段，混合使用不同状态以便演示
- JSON 反序列化时 Jackson 自动将字符串映射为枚举值

## Risks / Trade-offs

- **[内存存储无事务]** → 状态变更是单步操作，内存 Map 的 put 是原子的，当前场景下风险可控
- **[枚举扩展需改代码]** → 后续新增状态（如退款中）需修改枚举类并重新部署；当前阶段可接受，后续如有频繁变更再考虑配置化
- **[无鉴权控制]** → 状态变更 API 无权限校验，任何调用者均可操作；当前项目无认证体系，暂不处理
- **[BREAKING 变更]** → 订单响应体新增 `status` 字段，前端如有强类型约束需适配
