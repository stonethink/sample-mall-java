## 1. 数据模型与枚举

- [ ] 1.1 创建 `OrderStatus` 枚举类（`src/main/java/com/example/mall/order/OrderStatus.java`），定义 `PENDING_PAYMENT`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED` 五种状态，并在枚举内实现合法状态转换校验方法 `canTransitionTo(OrderStatus target)`
- [ ] 1.2 在 `Order` 实体中新增 `status` 字段（类型 `OrderStatus`），添加 getter/setter，更新构造函数
- [ ] 1.3 定义结构化错误响应类（如 `OrderStatusErrorResponse`），包含 `error`、`message`、`orderId`、`currentStatus`、`requestedStatus`、`allowedTransitions` 等字段

## 2. 数据层

- [ ] 2.1 更新 `orders.json`，为所有现有订单数据添加 `status` 字段，确保五种状态每种至少有一条订单数据，便于演示和测试
- [ ] 2.2 在 `OrderRepository` 中新增 `findByStatus(OrderStatus status)` 方法，通过遍历内存存储按状态过滤订单
- [ ] 2.3 在 `OrderRepository` 数据加载逻辑中增加兼容性处理：对缺失 `status` 字段的订单，默认设置为 `PENDING_PAYMENT`

## 3. 业务逻辑层

- [ ] 3.1 在 `OrderService.create()` 中设置新订单的默认状态为 `PENDING_PAYMENT`，确保客户端传入的 `status` 字段被忽略
- [ ] 3.2 在 `OrderService` 中新增 `updateStatus(Long id, OrderStatus newStatus)` 方法，包含状态流转校验逻辑（调用 `canTransitionTo`），非法转换抛出带上下文的业务异常（包含当前状态、请求状态、允许转换列表）
- [ ] 3.3 在 `OrderService` 中新增 `listByStatus(OrderStatus status)` 方法，调用 Repository 的按状态查询
- [ ] 3.4 在 `OrderService.update()` 通用更新方法中增加处理：忽略请求体中的 `status` 字段，确保状态不会被通用接口修改

## 4. API 层

- [ ] 4.1 在 `OrderController` 中新增 `PUT /api/orders/{id}/status` 端点（同时支持 `PUT /order/{id}/status`），接收 `{"status": "PAID"}` 格式请求体，调用 Service 层状态变更方法，非法转换返回 HTTP 400，订单不存在返回 HTTP 404
- [ ] 4.2 修改 `OrderController` 的订单列表接口 `GET /api/orders`（同时支持 `GET /order` 和 `GET /order/list`），增加可选的 `status` 查询参数，支持按状态筛选
- [ ] 4.3 实现统一异常处理：将状态流转异常映射为结构化 400 响应，将订单不存在异常映射为结构化 404 响应，包含错误码、消息、订单ID、当前状态、请求状态、允许转换列表等字段
- [ ] 4.4 实现无效输入校验：对非法状态值（如 `UNKNOWN`、空字符串、大小写错误如 `paid`）、缺失 `status` 字段的请求返回 HTTP 400 和结构化错误响应
- [ ] 4.5 实现无效查询参数校验：对非法 `status` 查询参数（如 `UNKNOWN`、空字符串）返回 HTTP 400 和结构化错误响应

## 5. 验证

### 5.1 基础功能验证
- [ ] 5.1.1 启动应用，验证订单列表接口返回的订单包含 `status` 字段
- [ ] 5.1.2 验证创建新订单时默认状态为 `PENDING_PAYMENT`
- [ ] 5.1.3 验证创建订单时传入 `status` 字段被忽略，仍为 `PENDING_PAYMENT`
- [ ] 5.1.4 验证按状态筛选订单列表功能正常
- [ ] 5.1.5 验证筛选无匹配订单时返回 HTTP 200 和空数组 `[]`

### 5.2 状态流转验证
- [ ] 5.2.1 验证正常流转路径：`PENDING_PAYMENT→PAID`、`PENDING_PAYMENT→CANCELLED`、`PAID→SHIPPED`、`PAID→CANCELLED`、`SHIPPED→COMPLETED`
- [ ] 5.2.2 验证非法流转被拒绝：`COMPLETED→PENDING_PAYMENT`、`CANCELLED→any`、`PENDING_PAYMENT→SHIPPED`、`PAID→COMPLETED`、`SHIPPED→CANCELLED`
- [ ] 5.2.3 验证同状态更新被拒绝（如 `PAID→PAID`）
- [ ] 5.2.4 验证非法流转返回的结构化错误响应包含 `error`、`message`、`orderId`、`currentStatus`、`requestedStatus`、`allowedTransitions`

### 5.3 无效输入验证
- [ ] 5.3.1 验证未知状态值（如 `UNKNOWN`）返回 HTTP 400 和结构化错误响应
- [ ] 5.3.2 验证缺失 `status` 字段返回 HTTP 400 和结构化错误响应
- [ ] 5.3.3 验证大小写错误状态值（如 `paid`）返回 HTTP 400 和结构化错误响应
- [ ] 5.3.4 验证空字符串状态值返回 HTTP 400 和结构化错误响应
- [ ] 5.3.5 验证非法查询参数（如 `?status=UNKNOWN`、`?status=`）返回 HTTP 400 和结构化错误响应

### 5.4 边界与兼容性验证
- [ ] 5.4.1 验证订单不存在时返回 HTTP 404 和结构化错误响应（包含 `error`、`message`、`orderId`）
- [ ] 5.4.2 验证 `PUT /api/orders/{id}` 通用更新接口不会修改 `status` 字段
- [ ] 5.4.3 验证 `/api/orders` 和 `/order` 两套路径风格在状态变更和筛选上行为一致
- [ ] 5.4.4 验证缺失 `status` 字段的历史订单数据被默认处理为 `PENDING_PAYMENT`

### 5.5 测试数据检查
- [ ] 5.5.1 确认 `orders.json` 中五种状态（`PENDING_PAYMENT`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED`）每种至少有一条订单数据
- [ ] 5.5.2 确认测试数据包含可用于合法流转验证的订单（如至少一条 `PENDING_PAYMENT` 用于测试付款，一条 `PAID` 用于测试发货等）
- [ ] 5.5.3 检查 Swagger/OpenAPI 文档是否正确展示状态枚举说明和错误响应示例
