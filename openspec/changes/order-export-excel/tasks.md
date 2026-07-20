## 1. 依赖配置

- [ ] 1.1 在 pom.xml 中添加 Apache POI 依赖（poi-ooxml）

## 2. Excel 导出工具

- [ ] 2.1 创建 OrderExcelExporter 工具类，接收 `List<OrderWithProductsDTO>` 并生成 `.xlsx` 格式的 ByteArrayOutputStream

## 3. Service 层扩展

- [ ] 3.1 在 OrderService 中添加订单查询与权限过滤的整合方法，供导出端点使用

## 4. Controller 导出端点

- [ ] 4.1 在 OrderController 中新增 `GET /api/orders/export` 端点，处理权限校验、调用 Service 和 Excel 导出、返回文件下载响应

## 5. 测试

- [ ] 5.1 单元测试 OrderExcelExporter 工具类
- [ ] 5.2 集成测试 OrderController 导出端点（权限、状态筛选、文件格式验证）