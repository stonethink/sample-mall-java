## Why

后台管理员需要将订单数据导出为 Excel 文件，便于进行数据分析、财务对账和线下存档。目前订单列表仅通过 API 返回 JSON 数据，无法直接生成可读的表格文件。添加 Excel 导出功能可以提升运营效率，满足日常数据导出需求。

## What Changes

- 在 OrderController 中新增 `GET /api/orders/export` 端点，按权限和状态筛选后导出为 Excel 文件
- 引入 Apache POI 依赖用于生成 `.xlsx` 格式的 Excel 文件
- 导出权限规则：管理员可导出全部订单（支持按状态筛选），普通用户仅导出自己的订单
- Excel 文件包含完整订单列：id、orderSn、商品名称（逗号分隔）、productIds、totalAmount、createdAt、status、userId
- 新增 Excel 导出工具类或服务方法，将 `OrderWithProductsDTO` 列表转换为 Excel 二进制流

## Capabilities

### New Capabilities
- `order-export`: 提供订单列表导出为 Excel 文件的能力，支持按订单状态筛选和管理员/普通用户权限区分

### Modified Capabilities

- (无规格级别行为变更)

## Impact

- `pom.xml`: 新增 Apache POI 依赖（poi-ooxml）
- `src/main/java/com/example/mall/order/OrderController.java`: 新增 `/export` 端点
- `src/main/java/com/example/mall/order/OrderService.java`: 新增或扩展导出相关方法
- 新增导出相关的辅助类（如 Excel 写入工具）