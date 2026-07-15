## Context

当前 UserController 是用户管理的核心 API 层，提供注册、登录、用户管理等关键功能。参照现有的 OrderControllerTest 和 ProductControllerTest，我们将使用类似的测试模式——直接实例化 Controller 和 Service，使用 MockMvc 进行测试。UserController 的特殊之处在于大部分端点依赖 HttpSession 进行身份认证。

## Goals / Non-Goals

**Goals:**
- 为 UserController 的核心端点提供单元测试覆盖
- 使用 MockMvc 模拟 HTTP 请求和 HttpSession
- 测试正常路径和异常路径（认证失败、参数错误等）

**Non-Goals:**
- 不测试 UserService 的业务逻辑（单独的 Service 测试）
- 不测试数据库持久化（使用内存 Repository）
- 不测试密码加密逻辑

## Decisions

### Decision 1: 测试策略

采用与 ProductControllerTest 相同的模式：
- 直接实例化 UserRepository（内存实现）
- 创建真实的 UserService 和 UserController
- 使用 MockMvc 进行集成测试
- 通过 MockHttpSession 模拟登录状态

**理由**：这种方式更接近真实场景，能够测试 Controller 层与 Service 层的集成，同时避免了复杂的 Mock 设置。

### Decision 2: 测试覆盖范围

重点测试以下端点：
- POST /api/users/register - 用户注册
- POST /api/users/login - 用户登录
- GET /api/users/me - 获取当前用户（需要登录）
- GET /api/users - 获取用户列表（需要管理员）
- GET /api/users/{id} - 获取单个用户（需要管理员）
- DELETE /api/users/{id} - 删除用户（需要管理员）

**理由**：这些是用户管理的核心功能，覆盖了认证、授权和 CRUD 操作。

## Risks / Trade-offs

- **风险**: HttpSession 的模拟可能增加测试复杂度
  → **缓解**: 使用 MockMvc 的 session() 方法设置会话属性
- **风险**: 测试数据准备可能繁琐
  → **缓解**: 在 @BeforeEach 中初始化测试数据，复用现有模式
