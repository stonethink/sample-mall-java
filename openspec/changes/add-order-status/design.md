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

**完整状态转换矩阵：**

| 当前状态 | 允许的目标状态 | 说明 |
|----------|----------------|------|
| `PENDING_PAYMENT` | `PAID`, `CANCELLED` | 待付款订单可确认付款或取消 |
| `PAID` | `SHIPPED`, `CANCELLED` | 已付款订单可发货或取消（注：取消代表订单终止，退款不在本次范围） |
| `SHIPPED` | `COMPLETED` | 已发货订单只能确认收货 |
| `COMPLETED` | （无） | 已完成订单不允许任何状态变更 |
| `CANCELLED` | （无） | 已取消订单不允许任何状态变更 |

**所有未列出的转换组合均视为非法**，将返回 HTTP 400。

非法跳转（如 `COMPLETED → PENDING_PAYMENT`）将抛出 `IllegalStateException`。

**替代方案**：不做流转校验，允许任意状态变更。选择校验方案是为了保证数据一致性，防止误操作。

### Decision 3: API 设计

状态变更使用独立端点 `PUT /api/orders/{id}/status`，请求体为 `{"status": "PAID"}`。

**理由**：将状态变更与订单编辑（`PUT /api/orders/{id}`）分离，语义更清晰，也便于后续加权限控制。

**通用更新接口边界**：
- `PUT /api/orders/{id}` 通用更新接口**不得修改** `status` 字段
- 如果请求体包含 `status` 字段，系统将**静默忽略**该字段（不返回错误，但也不生效）
- 状态变更**必须**通过专用端点 `PUT /api/orders/{id}/status` 进行

**理由**：防止绕过状态流转校验，确保所有状态变更都经过状态机规则检查。

按状态筛选通过查询参数实现：`GET /api/orders?status=PAID`。

**路径兼容性**：新能力同时支持 `/api/orders` 和 `/order` 两套路径风格（与现有控制器保持一致），包括：
- `PUT /api/orders/{id}/status` 和 `PUT /order/{id}/status`
- `GET /api/orders?status=PAID`、`GET /order?status=PAID` 和 `GET /order/list?status=PAID`

### Decision 4: 数据层处理

- `Order` 实体新增 `status` 字段（类型 `OrderStatus`）
- `OrderRepository` 新增 `findByStatus(OrderStatus status)` 方法，遍历内存存储过滤
- `orders.json` 为每条数据添加 `status` 字段，混合使用不同状态以便演示
- JSON 反序列化时 Jackson 自动将字符串映射为枚举值

**历史数据兼容策略**：
- 对于缺失 `status` 字段的旧数据，在加载时默认映射为 `PENDING_PAYMENT`
- 实现方式：在 Repository 加载数据时进行兜底处理，确保所有订单对象都有有效的状态值

### Decision 5: 错误响应设计

定义统一的结构化错误响应格式，便于前端和 API 消费者定位问题：

**HTTP 400 - 非法状态流转：**
```json
{
  "error": "INVALID_STATUS_TRANSITION",
  "message": "Cannot transition from PAID to COMPLETED",
  "orderId": 1001,
  "currentStatus": "PAID",
  "requestedStatus": "COMPLETED",
  "allowedTransitions": ["SHIPPED", "CANCELLED"]
}
```

**HTTP 400 - 无效状态值：**
```json
{
  "error": "INVALID_ORDER_STATUS",
  "message": "Invalid status value: paid",
  "parameter": "status",
  "providedValue": "paid",
  "allowedValues": ["PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"]
}
```

**HTTP 404 - 订单不存在：**
```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order not found with id: 1001",
  "orderId": 1001
}
```

**实现策略**：
- 在 `OrderService` 中抛出带上下文的业务异常
- 在 `OrderController` 或全局异常处理器中捕获并转换为结构化响应

## Risks / Trade-offs

- **[内存存储无事务]** → 状态变更涉及"读取→校验→写入"复合操作，非原子性；存在竞态条件风险。考虑到这是示例项目，当前仅保证单请求场景下的状态校验，**不承诺强并发一致性**
- **[枚举扩展需改代码]** → 后续新增状态（如退款中）需修改枚举类并重新部署；当前阶段可接受，后续如有频繁变更再考虑配置化
- **[无鉴权控制]** → 状态变更 API 无权限校验，任何调用者均可操作；当前项目无认证体系，暂不处理，但预留了按角色扩展的接口设计
- **[兼容性变更]** → 订单响应体新增 `status` 字段，对大多数可忽略未知字段的客户端是兼容的；对严格校验响应结构的客户端，需评估并适配新增字段

**关于 `PAID → CANCELLED` 的说明**：
本次变更保留 `PAID → CANCELLED` 转换路径，代表"示例级人工取消，订单终止"。真实的退款流程不在本次范围内，如需完整的退款/售后能力，后续应单独设计，不直接混入主订单五态模型。

**关于超时自动取消**：
当前版本仅支持人工取消，超时未付款订单不会自动关闭。如需自动取消能力，后续需引入定时任务机制。
