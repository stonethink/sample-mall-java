## Why

当前订单模型（`Order`）没有状态字段，无法表达订单在其生命周期中的不同阶段（如待付款、已付款、已发货、已完成、已取消等）。缺少状态管理导致业务流程无法闭环，后续的发货、退款、统计等功能均无法建立在状态流转基础上。增加订单状态管理是完善订单模块核心能力的必要一步。

## What Changes

- 为 `Order` 实体新增 `status` 字段，使用枚举类型 `OrderStatus` 表达状态值
- 定义标准订单状态枚举：`PENDING_PAYMENT`（待付款）、`PAID`（已付款）、`SHIPPED`（已发货）、`COMPLETED`（已完成）、`CANCELLED`（已取消）
- 新增订单状态流转 API，支持按业务规则推进订单状态（如确认付款、发货、确认收货、取消订单）
- 新增按状态筛选订单列表的查询能力
- 创建订单时默认状态为 `PENDING_PAYMENT`
- 更新 JSON 示例数据，为现有订单添加 `status` 字段
- **BREAKING**：`Order` 响应体新增 `status` 字段，已有的订单列表/详情 API 返回结构发生变化

## Capabilities

### New Capabilities
- `order-status-management`: 订单状态枚举定义、状态流转规则与校验、状态变更 API、按状态筛选订单

### Modified Capabilities
（无现有 spec 需要修改）

## Impact

- **代码**：`order` 包下的 `Order.java`、`OrderService.java`、`OrderController.java`、`OrderRepository.java` 均需修改；新增 `OrderStatus.java` 枚举类
- **API**：订单相关的所有 GET 接口返回体新增 `status` 字段；新增 `PUT /api/orders/{id}/status` 状态变更端点；`GET /api/orders` 支持 `status` 查询参数
- **数据**：`orders.json` 示例数据需增加 `status` 字段
- **前端**：`admin.html` 如需展示订单状态需同步调整（不在本次变更范围内，但需注意兼容性）
