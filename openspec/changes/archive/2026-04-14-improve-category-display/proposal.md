## Why

当前商品列表中商品类别显示为分类ID（如 cat-001），对用户不友好。管理员无法直观了解商品所属的分类，尤其是多级分类场景下，需要显示完整的分类路径（如：食品生鲜 > 水果 > 进口水果）才能清晰表达商品分类归属。

## What Changes

- 后端 API 返回商品数据时，将 categoryIds 转换为包含名称和完整路径的分类信息
- 新增 CategoryPathDTO 包含分类ID、名称和完整路径（如：食品生鲜 > 水果）
- 前端商品列表显示分类名称而非ID，多级分类显示完整路径
- 商品详情页同步更新分类显示方式

## Capabilities

### New Capabilities
- `category-path-rendering`: 分类路径渲染服务，提供分类ID到完整路径名称的转换

### Modified Capabilities
- `product-management`: 商品列表和详情接口返回的分类信息从ID列表改为包含名称和路径的结构化数据

## Impact

- 后端：ProductController 和 ProductService 需要调整返回数据结构
- 前端：admin.html 商品列表和表单中的分类显示逻辑需要调整
- API 响应格式变化，但向后兼容（新增字段，不删除原有 categoryIds）
