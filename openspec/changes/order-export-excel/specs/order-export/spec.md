## ADDED Requirements

### Requirement: 管理员可按状态导出全部订单

The system SHALL allow admin users to export all orders as an Excel file.

#### Scenario: 管理员导出全部订单（无筛选）
- **WHEN** an admin user sends `GET /api/orders/export` with no `status` parameter
- **THEN** the system returns an `.xlsx` file containing all orders

#### Scenario: 管理员导出按状态筛选的订单
- **WHEN** an admin user sends `GET /api/orders/export?status=PAID`
- **THEN** the system returns an `.xlsx` file containing only orders with status `PAID`

### Requirement: 普通用户仅能导出自己的订单

The system SHALL allow non-admin users to export only their own orders as an Excel file.

#### Scenario: 普通用户导出自己的订单
- **WHEN** a logged-in non-admin user sends `GET /api/orders/export` with no status filter
- **THEN** the system returns an `.xlsx` file containing only orders belonging to this user

#### Scenario: 普通用户导出按状态筛选的订单
- **WHEN** a logged-in non-admin user sends `GET /api/orders/export?status=SHIPPED`
- **THEN** the system returns an `.xlsx` file containing only this user's orders with status `SHIPPED`

### Requirement: 未登录用户返回 401

The system SHALL reject unauthenticated export requests.

#### Scenario: 未登录用户尝试导出
- **WHEN** an unauthenticated user sends `GET /api/orders/export`
- **THEN** the system returns HTTP 401 Unauthorized with error message

### Requirement: Excel 文件包含完整订单字段

The exported Excel file SHALL contain the following columns: 订单ID, 订单号, 商品名称, 商品ID列表, 总金额(分), 创建时间, 状态, 用户ID.

#### Scenario: 验证 Excel 列头正确
- **WHEN** any user exports orders
- **THEN** the returned `.xlsx` file's header row contains the specified column names

#### Scenario: 验证 Excel 数据行正确
- **WHEN** any user exports orders
- **THEN** each row in the `.xlsx` file contains the correct order data mapped to the corresponding column