## Context

订单模块目前通过 `OrderController.listAll()` 提供 JSON 格式的订单列表查询，支持按状态筛选和权限隔离（管理员查看全部，普通用户仅查看自己的）。运营人员需要将订单数据导出为 Excel 文件以便进行数据分析、财务对账等线下操作。

项目为 Spring Boot 2.7 + Java 8，无现有 Excel 处理依赖。

## Goals / Non-Goals

**Goals:**
- 新增 `GET /api/orders/export` 端点，返回 `.xlsx` 格式的 Excel 文件下载
- 支持按状态筛选（与 `listAll` 相同的 `status` 查询参数）
- 复用现有权限逻辑：管理员导出全部，普通用户仅导出自己的订单
- Excel 包含订单完整字段：id, orderSn, 商品名, productIds, totalAmount, createdAt, status, userId

**Non-Goals:**
- 不引入异步导出（当前数据量小，同步导出即可）
- 不修改现有的 JSON 查询接口行为
- 不添加自定义 Excel 模板/样式定制（使用默认表格样式）
- 不涉及前端下载 UI（仅提供后端 API）

## Decisions

1. **选择 Apache POI 作为 Excel 库**
   - 理由：业界标准的 Java Excel 处理库，支持 `.xlsx` 格式，社区成熟稳定
   - 替代方案：EasyExcel（阿里开源的简化封装）— 功能更简但需额外依赖，当前项目为 Java 8，POI 兼容性更可靠

2. **导出为独立端点而非参数开关**
   - 理由：`?export=true` 方式会耦合 JSON 和二进制两种响应格式，导致 Controller 方法职责不清晰。独立端点更符合 REST 设计原则

3. **Excel 写入逻辑放在 OrderService 层**
   - 理由：Service 层已有 `listAllWithProducts()` 和 `listByStatusWithProducts()` 方法，导出可直接复用这些方法获取 DTO 列表。将 Excel 生成逻辑封装为单独的辅助类，在 Controller 中调用

4. **直接返回文件流而非先写磁盘**
   - 理由：项目数据量小，使用 `ByteArrayOutputStream` 在内存中生成 Excel 后直接通过 `ResponseEntity<byte[]>` 返回，避免临时文件管理

## Risks / Trade-offs

- [内存] 大数据量导出可能导致 OOM → 当前订单数据量小，暂不处理；后续可改为流式写入或异步导出
- [依赖] 引入 Apache POI 增加约 2MB JAR 体积 → 对电商后台项目可接受
- [兼容性] POI 4.x+ 要求 Java 8+，当前项目 JDK 1.8 兼容