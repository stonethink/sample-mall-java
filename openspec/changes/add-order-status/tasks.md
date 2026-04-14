## 1. 数据模型与枚举

- [ ] 1.1 创建 `OrderStatus` 枚举类（`src/main/java/com/example/mall/order/OrderStatus.java`），定义 `PENDING_PAYMENT`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED` 五种状态，并在枚举内实现合法状态转换校验方法 `canTransitionTo(OrderStatus target)`
- [ ] 1.2 在 `Order` 实体中新增 `status` 字段（类型 `OrderStatus`），添加 getter/setter，更新构造函数

## 2. 数据层

- [ ] 2.1 更新 `orders.json`，为所有现有订单数据添加 `status` 字段，混合使用不同状态值以便演示
- [ ] 2.2 在 `OrderRepository` 中新增 `findByStatus(OrderStatus status)` 方法，通过遍历内存存储按状态过滤订单

## 3. 业务逻辑层

- [ ] 3.1 在 `OrderService.create()` 中设置新订单的默认状态为 `PENDING_PAYMENT`
- [ ] 3.2 在 `OrderService` 中新增 `updateStatus(Long id, OrderStatus newStatus)` 方法，包含状态流转校验逻辑（调用 `canTransitionTo`），非法转换抛出 `IllegalStateException`
- [ ] 3.3 在 `OrderService` 中新增 `listByStatus(OrderStatus status)` 方法，调用 Repository 的按状态查询

## 4. API 层

- [ ] 4.1 在 `OrderController` 中新增 `PUT /api/orders/{id}/status` 端点，接收 `{"status": "PAID"}` 格式请求体，调用 Service 层状态变更方法，非法转换返回 HTTP 400，订单不存在返回 HTTP 404
- [ ] 4.2 修改 `OrderController` 的订单列表接口 `GET /api/orders`，增加可选的 `status` 查询参数，支持按状态筛选

## 5. 验证

- [ ] 5.1 启动应用，验证订单列表接口返回的订单包含 `status` 字段
- [ ] 5.2 验证创建新订单时默认状态为 `PENDING_PAYMENT`
- [ ] 5.3 验证状态流转 API 的正常流转和非法流转场景
- [ ] 5.4 验证按状态筛选订单列表功能
