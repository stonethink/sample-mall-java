# 测试专家评审：订单状态管理 Spec

## 评审范围
- 评审日期：2026-04-14
- 评审文档：
  - `openspec/changes/add-order-status/proposal.md`
  - `openspec/changes/add-order-status/design.md`
  - `openspec/changes/add-order-status/specs/order-status-management/spec.md`
  - `openspec/changes/add-order-status/tasks.md`
- 参考代码：
  - `src/main/java/com/example/mall/order/Order.java`
  - `src/main/java/com/example/mall/order/OrderController.java`
  - `src/main/java/com/example/mall/order/OrderService.java`
  - `src/main/java/com/example/mall/order/OrderRepository.java`
  - `src/main/resources/orders.json`

## 总体结论
当前 Spec 已覆盖主干正向流转与两个基础异常场景，适合作为第一版实现输入；但从测试视角看，**异常输入、错误响应契约、查询参数校验、并发语义、测试数据规格** 仍不够明确，11 个 WHEN/THEN 场景尚不足以支撑稳定的自动化测试设计。若直接按当前文档实现，较容易出现“功能能跑通，但自动化测试口径不一致、前端无法稳定排错、并发行为不可预期”的问题。

## P0 - 阻塞问题

### TE-01：400/404 错误响应体未形成明确契约，前后端及自动化测试无法稳定对齐
**问题描述**
- `spec.md` 仅说明非法流转返回 HTTP 400、订单不存在返回 HTTP 404，但没有规定响应体字段。
- 现有代码中订单接口的 404 目前是空响应；其他模块（如分类）才返回 `{"error": "..."}` 结构。若本变更不先定义契约，前端和测试脚本无法判断错误原因，只能依赖状态码。

**影响分析**
- 自动化测试无法断言错误语义，只能断言 HTTP 状态码，覆盖深度不足。
- 前端无法区分“订单不存在”“状态值非法”“状态流转非法”等不同错误。
- 实现阶段极易出现不同错误分支返回不同 JSON 结构的情况。

**修改建议**
在 `spec.md` 中新增统一错误响应要求，例如：
- 400/404 均返回 JSON 对象
- 至少包含：`error`、`message`
- 对状态变更错误，建议额外包含：`orderId`、`currentStatus`、`requestedStatus`

建议补充场景：

#### Scenario: Illegal transition returns structured error body
- **WHEN** an order is in `PAID` status
- **AND** a status change to `COMPLETED` is requested via `PUT /api/orders/{id}/status`
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL be a JSON object containing at least `error` and `message`
- **AND** the response body SHALL include `currentStatus` with value `PAID`
- **AND** the response body SHALL include `requestedStatus` with value `COMPLETED`

#### Scenario: Non-existent order returns structured 404 body
- **WHEN** a `PUT /api/orders/{id}/status` request is sent for a non-existent order ID
- **THEN** the system SHALL return HTTP 404
- **AND** the response body SHALL be a JSON object containing at least `error` and `message`
- **AND** the response body SHALL include the requested `orderId`

---

### TE-02：未覆盖非法状态值输入，异常路径定义不完整
**问题描述**
当前 Spec 只覆盖“状态合法但流转非法”的情况，没有覆盖“状态值本身非法”的情况，包括：
- 未知枚举值，如 `UNKNOWN`
- `null`
- 空字符串
- 大小写错误，如 `paid`
- 缺失 `status` 字段

**影响分析**
- 这是最常见的接口输入错误类型，真实调用和前端联调时高频出现。
- 当前文档未说明这些输入应返回 400 还是走到框架默认错误，容易导致实现依赖 Spring/Jackson 默认行为，响应体不稳定。
- 自动化测试无法形成统一断言口径。

**修改建议**
在 `spec.md` 中单独补充“invalid status payload/query parameter”相关 requirement 或 scenario，并在 `design.md` 指定校验责任归属（控制器反序列化失败 vs 业务校验失败）。

建议补充场景：

#### Scenario: Reject unknown status value in status update request
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with body `{"status": "UNKNOWN"}`
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status value is invalid

#### Scenario: Reject missing status field in status update request
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with an empty body or without the `status` field
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating `status` is required

#### Scenario: Reject case-mismatched status value
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with body `{"status": "paid"}`
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status value is invalid

---

## P1 - 重要问题

### TE-03：状态转换覆盖不足，11 个场景不能证明“所有非法组合都被拒绝”
**问题描述**
当前文档只列出 5 个合法流转场景，以及 2 个非法场景示例：
- `COMPLETED -> PENDING_PAYMENT`
- `CANCELLED -> any`

但未覆盖或未明确以下高风险非法组合：
- `PENDING_PAYMENT -> SHIPPED`
- `PENDING_PAYMENT -> COMPLETED`
- `PAID -> PENDING_PAYMENT`
- `PAID -> COMPLETED`
- `SHIPPED -> PAID`
- `SHIPPED -> CANCELLED`
- 已完成订单再次变更为其他任意状态
- 同状态更新，如 `PAID -> PAID`

**影响分析**
- 需求文字说“enforce valid state transitions”，但测试无法只靠两个反例证明全矩阵受控。
- 实现者可能只针对示例写 `if/else`，遗漏某些非法组合。
- 自动化测试难以从 Spec 直接推导完整状态机断言。

**修改建议**
不一定要把全部 20+ 组合逐条写成独立场景，但至少应在 `design.md` 或 `spec.md` 中明确：
- 仅允许 5 条合法路径
- 除合法路径外，其余全部状态变更请求均返回 400
- 明确同状态更新是否允许

建议补充场景：

#### Scenario: Reject unsupported transition from PENDING_PAYMENT to SHIPPED
- **WHEN** an order is in `PENDING_PAYMENT` status
- **AND** a status change to `SHIPPED` is requested
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** return an error message indicating the transition is not allowed

#### Scenario: Reject unsupported transition from SHIPPED to CANCELLED
- **WHEN** an order is in `SHIPPED` status
- **AND** a status change to `CANCELLED` is requested
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** return an error message indicating the transition is not allowed

#### Scenario: Define same-status update behavior
- **WHEN** an order is in `PAID` status
- **AND** a status change to `PAID` is requested
- **THEN** the system SHALL either return HTTP 200 without changing the resource or reject the request with HTTP 400
- **AND** the chosen behavior SHALL be defined consistently in both the specification and implementation

---

### TE-04：筛选接口缺少无效查询参数场景，边界与异常输入未定义
**问题描述**
`spec.md` 仅定义：
- `GET /api/orders?status=PAID` 返回筛选结果
- `GET /api/orders` 无参数返回全部

但未定义以下情况：
- `status=UNKNOWN`
- `status=paid`
- `status=` 空字符串
- `status` 多值或重复参数

**影响分析**
- 查询型接口同样存在输入校验需求，且与更新接口应保持一致。
- 若未定义，可能有人实现为“忽略非法过滤条件并返回全部订单”，这会掩盖调用错误。
- 自动化测试无法明确预期行为。

**修改建议**
在 `spec.md` 中补充非法筛选参数场景，并在 `design.md` 中说明查询参数解析与错误返回策略。

建议补充场景：

#### Scenario: Reject invalid status filter value
- **WHEN** a `GET /api/orders?status=UNKNOWN` request is sent
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status filter is invalid

#### Scenario: Reject empty status filter value
- **WHEN** a `GET /api/orders?status=` request is sent
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status filter is invalid

---

### TE-05：并发修改同一订单状态的语义未定义，存在竞态测试空白
**问题描述**
`design.md` 提到“内存 Map 的 put 是原子的，当前场景下风险可控”，但状态更新实际是“读取当前状态 -> 校验 -> 写回”的复合操作，并非天然原子。两个请求同时修改同一订单时，可能同时基于旧状态完成校验。

示例：订单初始为 `PAID`，两个请求并发执行：
- 请求 A：`PAID -> SHIPPED`
- 请求 B：`PAID -> CANCELLED`

若两者都在读取旧值后完成校验，就可能都返回成功，最终状态取决于最后一次写入。

**影响分析**
- 与“状态流转规则严格受控”的目标存在冲突。
- 自动化测试无法判断预期：是允许最后写入覆盖，还是要求只有一个成功。
- 后续若前端增加“确认付款/发货”按钮，偶发竞态会很难排查。

**修改建议**
考虑到这是示例级内存项目，建议二选一并写入文档：
1. **务实方案**：明确并发一致性不是本次保证范围，仅保证单请求场景；tasks 中增加手工并发验证说明。
2. **更稳妥方案**：在服务层对单订单状态更新加同步控制，并规定并发下仅允许一个请求成功。

建议补充场景：

#### Scenario: Concurrent status updates on the same order are handled deterministically
- **WHEN** two requests try to update the status of the same order at the same time from `PAID`
- **AND** one request changes the status to `SHIPPED`
- **AND** the other request changes the status to `CANCELLED`
- **THEN** the system SHALL define a deterministic outcome
- **AND** the specification SHALL state whether only one request succeeds or whether last-write-wins is accepted for this demo project

---

### TE-06：创建订单与通用更新接口对 `status` 字段的处理未说清，存在绕过规则风险
**问题描述**
Spec 已说明“创建订单默认状态为 `PENDING_PAYMENT`”且状态更新应走独立端点，但未定义：
- `POST /api/orders` 请求体若显式传入 `status`，系统是忽略还是拒绝？
- `PUT /api/orders/{id}` 通用更新接口若带 `status` 字段，系统是忽略还是拒绝？

从现有代码看，通用更新接口目前不会更新 `status`，但 Spec 没写清楚，测试无法据此形成确定断言。

**影响分析**
- 若未来有人扩展 `update()` 时顺手复制字段，可能绕过状态机规则。
- 接口边界不清会导致前端误用通用更新接口改状态。
- 自动化测试无法覆盖“禁止旁路修改状态”的要求。

**修改建议**
在 `spec.md` 或 `design.md` 中明确：
- 创建订单时客户端提供的 `status` SHALL be ignored 或 SHALL be rejected with 400
- 通用订单更新接口 SHALL NOT modify `status`

建议补充场景：

#### Scenario: Create order ignores client-supplied status
- **WHEN** a new order is created via `POST /api/orders`
- **AND** the request body contains `"status": "COMPLETED"`
- **THEN** the created order's status SHALL be `PENDING_PAYMENT`

#### Scenario: General order update endpoint does not change status
- **WHEN** an existing order is updated via `PUT /api/orders/{id}`
- **AND** the request body contains a `status` field different from the current order status
- **THEN** the system SHALL NOT change the order status via this endpoint
- **AND** the specification SHALL define whether the `status` field is ignored or rejected

---

## P2 - 改进建议

### TE-07：tasks.md 的测试数据规格过于粗粒度，难以直接支撑自动化与回归测试
**问题描述**
当前 `tasks.md` 只写了“混合使用不同状态值以便演示”“验证正常流转和非法流转场景”，但没有明确最小测试数据集与验证矩阵。

**影响分析**
- 实现者可能只准备少量样例，导致某些状态无法稳定复现。
- 自动化测试难以做到可重复、可追踪。
- 评审要求中的边界、异常、并发测试没有落到任务层，后续容易被遗漏。

**修改建议**
将 `tasks.md` 的验证部分扩充为更细粒度任务，至少明确：
- `orders.json` 中至少 1 条订单覆盖每个状态
- 至少 1 条 `PENDING_PAYMENT`、1 条 `PAID`、1 条 `SHIPPED` 用于验证 5 条合法流转路径与非法流转示例
- 增加无效状态值、缺失 `status`、空字符串、大小写错误、非法筛选参数、非存在订单 ID 的验证任务
- 若并发不纳入实现范围，也要增加“文档化并发限制”的任务

可追加任务示例：
- [ ] 5.5 验证 `PUT /api/orders/{id}/status` 在 `status` 缺失、非法枚举值、大小写错误时返回 HTTP 400 与统一错误响应体
- [ ] 5.6 验证 `GET /api/orders` 在非法 `status` 查询参数下返回 HTTP 400 与统一错误响应体
- [ ] 5.7 准备覆盖五种状态的固定测试数据，并记录每条样例数据的用途
- [ ] 5.8 说明并发更新同一订单时的预期行为，并执行一次并发验证或明确标注为非保证范围

---

### TE-08：Spec 可测试性总体较好，但建议把“状态机矩阵”显式化，降低用例设计歧义
**问题描述**
当前场景能覆盖主路径，但测试人员仍需自行从文字中推导完整矩阵。对示例项目来说，增加一张简单状态矩阵比继续堆叠场景更高效。

**影响分析**
- 用例设计人员需要二次解释需求。
- 评审者与实现者对“哪些状态允许转换”可能理解不一致。

**修改建议**
在 `design.md` 中补一张状态转换矩阵，标注 Allowed / Rejected；`spec.md` 中保留代表性场景即可。这样最适合本项目这种小型、内存存储、无复杂工作流引擎的场景。

## 修改建议汇总

### spec.md 修改建议（需给出具体的新增/修改场景内容）
1. 在“Order status transitions follow defined rules”下补充总则：**除设计文档明确允许的 5 条流转外，其他任意状态变更请求一律返回 HTTP 400**。
2. 新增非法输入场景：
   - `UNKNOWN` 状态值
   - 缺失 `status`
   - 空字符串
   - 大小写错误如 `paid`
3. 新增查询参数异常场景：
   - `GET /api/orders?status=UNKNOWN`
   - `GET /api/orders?status=`
4. 新增统一错误响应体场景，明确 400/404 至少返回 `error`、`message`；状态流转错误建议返回 `currentStatus`、`requestedStatus`、`orderId`。
5. 新增同状态更新语义场景，明确 `PAID -> PAID` 是 200 幂等还是 400 非法请求。
6. 新增“禁止旁路改状态”场景：
   - 创建订单携带 `status` 时的行为
   - 通用更新接口携带 `status` 时的行为
7. 新增并发语义场景，哪怕只声明“本示例项目不保证并发一致性”，也应显式写明。

### design.md 修改建议
1. 在 API 设计部分补充错误响应体结构示例，例如：
   - 400：`{"error":"INVALID_STATUS_TRANSITION","message":"...","orderId":1,"currentStatus":"PAID","requestedStatus":"COMPLETED"}`
   - 404：`{"error":"ORDER_NOT_FOUND","message":"...","orderId":999}`
2. 在“状态流转规则”部分补充：
   - 除列出的 5 条允许路径外，其余全部拒绝
   - 同状态更新的明确处理策略
3. 在“API 设计”部分明确：
   - `POST /api/orders` 若传入 `status` 的处理方式
   - `PUT /api/orders/{id}` 不允许修改 `status`
   - `GET /api/orders?status=...` 非法参数返回 400
4. 在“Risks / Trade-offs”中不要只写“put 是原子的”，应明确复合读写存在竞态；如果选择不处理并发，需要明确写成已知限制。
5. 建议补一张状态机矩阵，方便测试直接转化为用例。

### proposal.md 修改建议
1. 在 “What Changes” 中增加“统一错误响应定义”和“非法输入校验”的变更项，避免需求目标只覆盖 happy path。
2. 在 “Impact” 中补充：
   - 状态更新与状态筛选接口的 400/404 错误体将新增稳定 JSON 结构
   - 自动化测试与前端联调将依赖该契约
3. 若决定不处理并发一致性，也建议在 proposal 中简要注明“示例项目仅保证单请求状态流转校验，不承诺强并发一致性”，避免预期过高。

### tasks.md 修改建议
1. 在 API 层任务中增加：
   - 非法状态值输入校验
   - 缺失 `status` 校验
   - 非法筛选参数校验
   - 统一错误响应体实现
2. 在验证任务中细化为可执行的检查项，而不是笼统一句“验证正常流转和非法流转场景”。
3. 明确测试数据规格：
   - `orders.json` 至少覆盖五种状态各 1 条
   - 至少保留可用于合法流转与非法流转验证的固定订单样本
4. 增加并发相关任务：
   - 若实现并发保护，则验证同一订单并发更新仅一个成功
   - 若不实现，则在任务中显式记录限制并做最小化验证
5. 建议新增以下任务：
   - [ ] 定义并实现订单状态变更接口统一错误响应结构
   - [ ] 验证非法状态值、空值、大小写错误、非法筛选参数的 400 行为
   - [ ] 验证非存在订单 ID 的 404 错误体
   - [ ] 明确并验证并发更新同一订单的预期行为
