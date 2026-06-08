## Tasks

- [x] 新增 `renderCategoryOptions(nodes, depth, selectedIds)` 辅助函数，递归生成带缩进的 option HTML
- [x] 修改 `loadProductCategorySelect` 函数，改为调用 `/api/categories/tree` 并使用 `renderCategoryOptions` 渲染
- [x] 修改 `loadCategoryFilter` 函数，改为调用 `/api/categories/tree` 并使用 `renderCategoryOptions` 渲染（保留"所有分类"默认选项）
- [x] 验证：编辑商品时分类按层级缩进显示，已选中分类正确回显
