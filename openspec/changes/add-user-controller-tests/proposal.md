## Why

UserController 是电商系统用户管理的核心 API 层，提供注册、登录、用户管理等关键功能，但目前缺少单元测试覆盖。添加测试可以确保用户认证和管理功能的正确性，提高代码质量。

## What Changes

- 创建 UserControllerTest.java 测试文件
- 测试核心 API 端点：register、login、getCurrentUser、listAll、getById、update、delete
- 使用 MockMvc 进行集成测试，Mock HttpSession 模拟登录状态
- Mock UserService 和相关依赖

## Capabilities

### New Capabilities
- `user-controller-tests`: 为 UserController 提供完整的单元测试覆盖

### Modified Capabilities

## Impact

- `src/test/java/com/example/mall/user/UserControllerTest.java`: 新建测试文件
- `src/main/java/com/example/mall/user/UserController.java`: 被测试的目标类
