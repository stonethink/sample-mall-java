## Context

当前 ProductController 是商品管理的核心 API 层，提供 CRUD 操作。参照现有的 OrderControllerTest，我们将使用类似的测试模式——直接实例化 Controller 和 Service，使用 MockMvc 进行测试。

## Goals / Non-Goals

**Goals:**
- 为 ProductController 的所有主要端点提供单元测试覆盖
- 使用 MockMvc 模拟 HTTP 请求
- 测试正常路径和异常路径

**Non-Goals:**
- 不测试 ProductService 的业务逻辑（已有单独的 Service 测试）
- 不测试数据库持久化（使用内存 Repository）
- 不测试 CategoryService 的分类转换逻辑

## Decisions

### Decision 1: 测试策略

采用与 OrderControllerTest 相同的模式：
- 直接实例化 ProductRepository 和 CategoryRepository（内存实现）
- 创建真实的 CategoryService 和 ProductService
- 创建真实的 ProductController
- 使用 MockMvc 进行集成测试

**理由**：这种方式更接近真实场景，能够测试 Controller 层与 Service 层的集成，同时避免了复杂的 Mock 设置。

### Decision 2: 测试覆盖范围

重点测试以下端点：
- GET /api/products - listAll
- GET /api/products/{id} - getById（包括商品存在和不存在的情况）
- POST /api/products - create
- PUT /api/products/{id} - update
- DELETE /api/products/{id} - delete

**理由**：这些是商品管理的核心 CRUD 操作，覆盖了主要业务场景。

## Risks / Trade-offs

- **风险**: CategoryService 的依赖可能增加测试复杂度
  → **缓解**: 使用真实的 Repository 实现，避免 Mock 复杂性
- **风险**: 测试数据准备可能繁琐
  → **缓解**: 在 @BeforeEach 中初始化测试数据，复用现有模式
