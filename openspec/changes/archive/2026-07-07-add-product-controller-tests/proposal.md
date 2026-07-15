## Why

ProductController 是电商系统商品管理的核心 API 层，但目前缺少单元测试覆盖。添加测试可以提高代码质量，确保商品 CRUD 操作的正确性，并为未来的重构提供安全保障。

## What Changes

- 创建 ProductControllerTest.java 测试文件
- 测试所有 API 端点：listAll、getById、create、update、delete、list、simpleList、search
- 使用 MockMvc 进行集成测试
- Mock ProductService 和相关依赖

## Capabilities

### New Capabilities
- `product-controller-tests`: 为 ProductController 提供完整的单元测试覆盖

### Modified Capabilities

## Impact

- `src/test/java/com/example/mall/product/ProductControllerTest.java`: 新建测试文件
- `src/main/java/com/example/mall/product/ProductController.java`: 被测试的目标类
