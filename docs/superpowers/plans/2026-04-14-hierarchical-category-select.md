---
change: hierarchical-category-select
design-doc: docs/superpowers/specs/2026-04-14-hierarchical-category-select-design.md
base-ref: 9ecccaf0763b40948c87294cb6ffcf1647e34e20
---

# 分类选择器层级显示 — 实施计划

## Task 1: 新增 renderCategoryOptions 辅助函数

**文件**: `src/main/resources/static/admin.html`

在 `loadCategoryFilter` 函数之前，添加辅助函数：

```javascript
function renderCategoryOptions(nodes, depth, selectedIds) {
    let html = '';
    nodes.forEach(node => {
        const prefix = depth > 0 ? '\u3000'.repeat(depth) + '├─ ' : '';
        const selected = selectedIds.includes(node.id) ? 'selected' : '';
        html += `<option value="${node.id}" ${selected}>${prefix}${node.name}</option>`;
        if (node.children && node.children.length > 0) {
            html += renderCategoryOptions(node.children, depth + 1, selectedIds);
        }
    });
    return html;
}
```

同时添加 `flattenCategoryTree` 辅助函数用于更新 `categoryNameMap`：

```javascript
function flattenCategoryTree(nodes) {
    nodes.forEach(node => {
        categoryNameMap[node.id] = node.name;
        if (node.children && node.children.length > 0) {
            flattenCategoryTree(node.children);
        }
    });
}
```

## Task 2: 修改 loadProductCategorySelect

**文件**: `src/main/resources/static/admin.html`

将现有实现改为调用 `/api/categories/tree`：

```javascript
async function loadProductCategorySelect(selectedIds = []) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/categories/tree`);
        const tree = await response.json();
        const select = document.getElementById('product-categories');
        select.innerHTML = renderCategoryOptions(tree, 0, selectedIds);
    } catch (error) {
        console.error('加载分类选择器失败:', error);
    }
}
```

## Task 3: 修改 loadCategoryFilter

**文件**: `src/main/resources/static/admin.html`

将现有实现改为调用 `/api/categories/tree`，保留"所有分类"默认选项，同时更新 `categoryNameMap`：

```javascript
async function loadCategoryFilter() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/categories/tree`);
        const tree = await response.json();
        
        // 更新名称映射
        categoryNameMap = {};
        flattenCategoryTree(tree);
        
        // 更新分类筛选下拉框（带层级）
        const filterSelect = document.getElementById('category-filter');
        filterSelect.innerHTML = '<option value="">所有分类</option>' + renderCategoryOptions(tree, 0, []);
    } catch (error) {
        console.error('加载分类失败:', error);
    }
}
```

## Task 4: 验证

- 启动应用，打开 admin.html
- 编辑商品：确认分类按层级缩进显示，已选分类正确回显
- 分类筛选：确认下拉框按层级显示
