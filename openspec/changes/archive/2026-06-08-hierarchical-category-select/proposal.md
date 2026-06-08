## Why

编辑商品时，分类选择器以扁平列表展示所有分类，无法区分一级分类和子分类的层级关系。当分类数量增多时，用户难以快速定位目标分类。需要按层级缩进显示分类，提升编辑效率。

## What Changes

- 商品编辑模态框中的分类多选下拉框改为按树形层级展示
- 利用已有的 `/api/categories/tree` 接口获取树形结构数据
- 子分类通过缩进前缀（如 `── `）体现层级关系
- 分类筛选下拉框同步改为层级展示，保持体验一致

## Capabilities

### New Capabilities

（无新增能力）

### Modified Capabilities

- `product-category-association`: 前端分类选择器的展示方式从扁平列表改为层级展示

## Impact

- **前端**：`admin.html` 中的 `loadProductCategorySelect` 和 `loadCategoryFilter` 函数
- **后端**：无修改（已有 `/api/categories/tree` 接口）
- **兼容性**：纯 UI 展示优化，不影响数据结构和 API 契约
