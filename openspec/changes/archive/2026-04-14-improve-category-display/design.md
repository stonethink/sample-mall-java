## Context

当前商品分类功能已实现，商品列表中显示的是分类ID（如 cat-001）。管理员在查看商品时无法直观了解商品所属的分类名称，特别是多级分类场景下，无法快速识别商品的完整分类路径。

现有数据结构：
- Product.categoryIds: List<String> - 仅包含分类ID
- Category: 包含 id, name, parentId

## Goals / Non-Goals

**Goals:**
- 商品列表显示分类名称而非ID
- 多级分类显示完整路径（如：食品生鲜 > 水果 > 进口水果）
- 保持 API 向后兼容（保留 categoryIds 字段）
- 优化前端显示，减少前端计算逻辑

**Non-Goals:**
- 不修改数据存储结构
- 不删除原有的 categoryIds 字段
- 不引入缓存机制（当前数据量小）

## Decisions

### 1. 后端构建分类路径
**选择：** 在服务端将 categoryIds 转换为包含名称和路径的分类信息
**理由：**
- 减少前端计算复杂度
- 避免前端多次请求分类数据
- 统一路径构建逻辑，确保一致性

**替代方案：** 前端根据 categoryIds 查询分类名称并构建路径 → 增加前端复杂度，需要额外请求

### 2. 数据结构：新增 CategoryInfo DTO
**选择：** 创建 CategoryInfo 类包含 id、name、fullPath
```java
class CategoryInfo {
    String id;      // cat-001
    String name;    // 进口水果
    String fullPath; // 食品生鲜 > 水果 > 进口水果
}
```
**理由：**
- 结构清晰，包含完整信息
- 前端可直接使用 fullPath 显示
- 保留 id 用于编辑时回显

### 3. API 响应格式
**选择：** Product 响应中新增 categories 字段（CategoryInfo 列表），保留原有的 categoryIds
**理由：**
- 向后兼容，不影响现有客户端
- 新客户端可以使用更友好的 categories 字段

### 4. 路径构建算法
**选择：** 使用递归或迭代方式，根据 parentId 链构建完整路径
**算法思路：**
```
for each categoryId:
    path = []
    current = findCategory(categoryId)
    while current != null:
        path.add(0, current.name)
        current = findCategory(current.parentId)
    fullPath = String.join(" > ", path)
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 分类层级过深导致路径过长 | 限制显示层级或截断显示 |
| 频繁查询分类数据影响性能 | 当前数据量小，如有性能问题后续可引入缓存 |
| 分类名称变更后路径不一致 | 实时计算路径，不存储，确保始终最新 |

## Migration Plan

1. 创建 CategoryInfo DTO 类
2. 在 CategoryService 中添加 buildCategoryPath() 方法
3. 修改 ProductService，在返回商品数据时转换分类信息
4. 更新前端 admin.html，使用 categories 字段显示分类路径
5. 测试验证分类路径显示正确

## Open Questions

- 分类路径分隔符使用 " > " 还是 " / "？（当前选择 " > "，更直观）
- 是否需要限制路径显示的最大长度？（当前不做限制，数据量小）
