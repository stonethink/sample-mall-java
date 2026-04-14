# 用户体验专家评审：订单状态管理 Spec

## 评审范围
- 评审日期：2026-04-14
- 评审文件：
  - `openspec/changes/add-order-status/proposal.md`
  - `openspec/changes/add-order-status/design.md`
  - `openspec/changes/add-order-status/specs/order-status-management/spec.md`
  - `openspec/changes/add-order-status/tasks.md`
- 参考实现风格：
  - `src/main/java/com/example/mall/order/OrderController.java`
  - `src/main/java/com/example/mall/order/Order.java`
  - 补充参考：`OrderService.java`、`OrderRepository.java`、`SwaggerConfig.java`

## P0 - 阻塞问题

### UX-01：新能力仅描述 `/api/orders`，未明确保留现有 `/order` 双路径风格
**问题描述**
现有订单控制器使用双路径映射：`/api/orders` 与 `/order`，且还保留 `/order/list` 风格列表接口。当前 proposal、design、spec、tasks 全部只描述了 `/api/orders/{id}/status` 与 `GET /api/orders?status=PAID`，没有明确 `/order/{id}/status`、`GET /order?status=PAID`、`GET /order/list?status=PAID` 是否也需要支持。

**影响分析**
- 现有 API 消费者如果依赖 `/order` 风格，无法从文档判断新能力是否可用。
- 实现者可能只为 `/api/orders` 做测试，导致别名路径能力缺失或行为不一致。
- 与当前控制器风格不一致，会直接拉低 API 可预期性。

**修改建议**
在 spec 和 design 中明确“新能力对两套历史路径都生效”，至少补充以下约束：
- `PUT /api/orders/{id}/status` 与 `PUT /order/{id}/status` 行为一致
- `GET /api/orders?status=PAID`、`GET /order?status=PAID`、`GET /order/list?status=PAID` 行为一致
- 如果团队决定只保留一套路由，也必须在 proposal 中明确迁移策略与弃用说明

**推荐 API 示例**
```http
PUT /order/1001/status
Content-Type: application/json

{
  "status": "PAID"
}
```

```http
GET /order/list?status=PAID
```

---

### UX-02：错误响应定义过于模糊，无法帮助开发者快速定位状态流转失败原因
**问题描述**
spec 只要求非法流转返回 HTTP 400 和“indicating the transition is not allowed”的错误消息；design 只提到抛出 `IllegalStateException`。这对 API 使用者不够友好，缺少当前状态、目标状态、允许的目标状态、订单 ID、错误码等关键上下文。

**影响分析**
- 开发者拿到 400 后仍需要翻文档或看服务端日志才能理解失败原因。
- 前端难以做精确提示，例如“当前订单已发货，只能确认收货”。
- 不利于 Swagger/OpenAPI 展示可消费的错误模型。

**修改建议**
为状态流转失败定义统一错误响应结构，并在 spec 中补充强制字段。建议至少包含：
- `code`
- `message`
- `orderId`
- `currentStatus`
- `targetStatus`
- `allowedTransitions`
- `path`
- `timestamp`

**推荐错误响应格式**
```json
{
  "code": "ORDER_STATUS_TRANSITION_NOT_ALLOWED",
  "message": "订单当前状态为 SHIPPED，不允许变更为 PAID",
  "orderId": 1001,
  "currentStatus": "SHIPPED",
  "targetStatus": "PAID",
  "allowedTransitions": ["COMPLETED"],
  "path": "/api/orders/1001/status",
  "timestamp": "2026-04-14T10:30:00"
}
```

同时补充以下异常场景：
- 请求体缺少 `status`
- `status` 不是合法枚举值
- 订单不存在

## P1 - 重要问题

### UX-03：通用更新端点与专用状态端点职责边界不清，容易引发歧义
**问题描述**
design 说明“将状态变更与订单编辑分离”，但 spec 没有明确 `PUT /api/orders/{id}` 是否允许携带 `status` 字段，也没有定义发现 `status` 时是忽略、报错还是按状态流转校验。

**影响分析**
- 客户端可能继续使用通用更新接口改状态，导致行为不一致。
- 如果实现时“静默忽略” `status`，开发者会误以为状态已更新成功。
- 如果实现时“直接覆盖” `status`，则专用状态端点失去意义，且绕过流转校验。

**修改建议**
在 spec 增加明确规则：
- `PUT /api/orders/{id}` 不允许修改 `status`
- 当请求体包含 `status` 时返回 HTTP 400
- 错误信息应指引使用专用端点 `PUT /api/orders/{id}/status`

**推荐错误响应格式**
```json
{
  "code": "ORDER_STATUS_UPDATE_REQUIRES_DEDICATED_ENDPOINT",
  "message": "status 字段只能通过 PUT /api/orders/{id}/status 修改",
  "path": "/api/orders/1001"
}
```

---

### UX-04：状态筛选与枚举输入规则不完整，参数可用性不足
**问题描述**
当前 spec 只给出 `GET /api/orders?status=PAID` 的成功场景，没有定义：
- 枚举值是否大小写敏感
- 非法值返回什么错误
- 是否返回允许值列表
- 空字符串、未知枚举、重复参数如何处理

**影响分析**
- 调用方难以一次性调用成功，尤其是前后端联调阶段。
- Swagger 文档难以准确表达参数约束。
- 依赖框架默认枚举解析时，可能直接返回不够友好的默认错误体。

**修改建议**
在 spec 中补充查询参数规则：
- `status` 使用大写枚举值
- 非法值返回 HTTP 400
- 错误体返回 `allowedValues`
- 示例中明确可用值列表

**推荐错误响应格式**
```json
{
  "code": "INVALID_ORDER_STATUS",
  "message": "无效的订单状态：paid",
  "parameter": "status",
  "providedValue": "paid",
  "allowedValues": ["PENDING_PAYMENT", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"]
}
```

---

### UX-05：OpenAPI/Swagger 展示要求缺失，文档消费者难以理解状态流转
**问题描述**
本次变更与“状态机”高度相关，但当前 design/spec/tasks 没有要求输出足够友好的 API 文档元素，例如：请求体模型名、枚举值说明、成功/失败示例、状态流转说明表。

**影响分析**
- Swagger 页面只能展示一个裸请求体 `{"status":"PAID"}`，上下文不足。
- 开发者需要反复切换 design/spec 才能理解哪些流转合法。
- 对示例项目的新手用户不够友好。

**修改建议**
在 design 与 tasks 中增加文档交付要求：
- 定义命名清晰的请求模型，如 `UpdateOrderStatusRequest`
- 为 `status` 参数和字段补充枚举说明
- 在 Swagger 中为 `PUT /orders/{id}/status` 增加 200/400/404 示例
- 在接口描述中附上简明流转表：`PENDING_PAYMENT -> PAID/CANCELLED` 等

**推荐文档示例**
```json
{
  "status": "SHIPPED"
}
```

并在接口说明中明确：
- `PENDING_PAYMENT` 可流转到：`PAID`、`CANCELLED`
- `PAID` 可流转到：`SHIPPED`、`CANCELLED`
- `SHIPPED` 可流转到：`COMPLETED`

---

### UX-06：错误处理实现路径未落地到任务层，容易退化为 Spring 默认错误页/默认错误体
**问题描述**
tasks 仅要求“非法转换返回 HTTP 400，订单不存在返回 HTTP 404”，但没有要求实现统一异常映射策略。结合现有代码风格，`OrderService.update()` 目前直接抛 `IllegalArgumentException`，如果状态变更也沿用这一模式，最终很可能得到框架默认错误响应，而不是对开发者友好的结构化错误。

**影响分析**
- 最终实现可能“符合状态码、不符合 DX”。
- 错误体字段随 Spring 默认行为变化，前端难以稳定消费。
- Swagger 也无法稳定描述错误模型。

**修改建议**
在 tasks 中新增明确任务：
- 定义结构化错误响应对象
- 在控制器或全局异常处理器中，将非法状态流转映射为 400，将不存在订单映射为 404
- 增加验证任务，检查错误体字段而不仅是状态码

**推荐错误响应格式**
```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "订单不存在，id=1001",
  "orderId": 1001,
  "path": "/api/orders/1001/status"
}
```

## P2 - 改进建议

### UX-07：状态枚举对人类读者可理解，但对 API 文档消费者仍缺少“中文语义层”
**问题描述**
design 中已经给出中文名表格，但 spec 与 API 设计未说明这些中文语义如何传达给接口消费者。对于示例项目，直接把中文描述塞进核心响应模型不一定必要，但文档层至少应让调用方一眼看懂。

**影响分析**
- 新接手项目的前端或测试同学需要人工对照枚举含义。
- Swagger 页面如果只显示英文枚举，学习成本偏高。

**修改建议**
务实做法是：
- 核心 API 继续使用稳定英文枚举值，避免额外破坏兼容性
- 在 design/spec/Swagger 示例中同步给出中文说明
- 如后续前端确有展示需求，再考虑在专门 DTO 中增加 `statusLabel`，而不是立刻改基础模型

**推荐文档展示方式**
- `PENDING_PAYMENT`：待付款
- `PAID`：已付款
- `SHIPPED`：已发货
- `COMPLETED`：已完成
- `CANCELLED`：已取消

---

### UX-08：proposal 中“BREAKING”判断与 design 中“向后兼容”目标表述不一致，建议给出更清晰迁移口径
**问题描述**
proposal 认为“响应体新增 status 字段”属于 BREAKING；design 又把“保持向后兼容（新增字段，不删除已有字段）”列为目标。两份文档对兼容性的口径不一致。

**影响分析**
- 评审者与实现者对风险等级判断不一致。
- API 消费者无法判断自己是否必须立即调整。

**修改建议**
建议在 proposal 中改为更精确的表述：
- 对大多数宽松 JSON 消费者属于“加字段的兼容性增强”
- 对启用严格反序列化/白名单校验的客户端“可能造成兼容性风险”
- 增加一段迁移提醒，而不是只写 BREAKING

**推荐表述示例**
> 本次变更为响应体新增 `status` 字段。对大多数可忽略未知字段的客户端是兼容的；对严格校验响应结构的客户端，需评估并适配新增字段。

## 修改建议汇总

### spec.md 修改建议
- 在“Status change via dedicated API endpoint”部分补充双路径一致性要求，明确 `/api/orders` 与 `/order` 风格都支持状态变更与筛选。
- 新增场景：通用 `PUT /api/orders/{id}` 请求体包含 `status` 时返回 400，并提示使用专用状态端点。
- 新增场景：非法状态枚举值、缺失 `status` 字段、非法筛选参数时的结构化错误响应。
- 在非法流转场景中，补充返回字段要求：`currentStatus`、`targetStatus`、`allowedTransitions`。
- 在筛选场景中补充参数规则：大小写、允许值列表、错误示例。

### design.md 修改建议
- 在“Decision 3: API 设计”中补充路径兼容策略，明确是否同时支持 `/order` 与 `/order/list` 风格。
- 增加“错误响应设计”小节，定义统一错误码与错误体结构。
- 增加“更新接口边界”说明，明确通用更新接口禁止修改 `status`。
- 增加“API 文档策略”说明，要求 Swagger/OpenAPI 展示枚举说明、状态流转表和错误示例。
- 在兼容性段落中统一 proposal/design 的表述口径。

### proposal.md 修改建议
- 在 “What Changes” 或 “Impact” 中明确现有 `/order` 路径消费者的兼容策略。
- 将“BREAKING”表述改为更精确的“新增字段的兼容性风险说明”，避免与 design 目标冲突。
- 补充开发者影响说明：新增专用状态端点后，通用更新接口不再承担状态修改职责。
- 补充错误响应改进目标，强调帮助 API 消费者定位失败原因。

### tasks.md 修改建议
- 新增任务：定义 `UpdateOrderStatusRequest` 请求模型或等价的明确请求体结构。
- 新增任务：实现统一异常处理与结构化错误响应，而不是只校验状态码。
- 新增任务：为 Swagger/OpenAPI 增加请求示例、错误示例、枚举说明。
- 新增任务：验证 `/api/orders` 与 `/order` 两套路由在状态变更和筛选上的一致性。
- 新增任务：验证通用更新接口包含 `status` 时返回明确错误。
- 将现有验证任务从“只验证成功与失败场景”升级为“验证响应结构与错误字段完整性”。
