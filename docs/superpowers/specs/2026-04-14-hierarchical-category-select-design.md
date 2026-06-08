---
comet_change: hierarchical-category-select
role: technical-design
canonical_spec: openspec
---

# 分类选择器层级显示 — 技术设计

## 概述

将商品编辑和分类筛选下拉框从扁平列表改为按层级缩进展示，利用已有 `/api/categories/tree` 接口。

## 技术方案

### 核心实现

新增辅助函数 `renderCategoryOptions(nodes, depth, selectedIds)`：

```javascript
function renderCategoryOptions(nodes, depth, selectedIds) {
    let html = '';
    nodes.forEach(node => {
        const prefix = depth > 0 ? '　'.repeat(depth) + '├─ ' : '';
        const selected = selectedIds.includes(node.id) ? 'selected' : '';
        html += `<option value="${node.id}" ${selected}>${prefix}${node.name}</option>`;
        if (node.children && node.children.length > 0) {
            html += renderCategoryOptions(node.children, depth + 1, selectedIds);
        }
    });
    return html;
}
```

### 修改点

1. **loadProductCategorySelect**：改为 `fetch('/api/categories/tree')` + `renderCategoryOptions`
2. **loadCategoryFilter**：改为 `fetch('/api/categories/tree')` + `renderCategoryOptions`（保留"所有分类"默认 option）
3. **categoryNameMap**：保留兼容，tree 遍历时同时构建 map

### 缩进策略

| 层级 | 前缀 | 示例 |
|------|------|------|
| 0（根） | 无 | `食品生鲜` |
| 1 | `　├─ ` | `　├─ 水果` |
| 2 | `　　├─ ` | `　　├─ 进口水果` |

使用全角空格 `\u3000` 作为缩进单位（确保 `<option>` 内等宽对齐）。

## 风险评估

- **风险极低**：后端零改动，纯 UI 展示变更
- **回退方案**：恢复 `loadProductCategorySelect` 和 `loadCategoryFilter` 原实现

## 测试策略

- 手动验证：打开商品编辑模态框，确认分类按层级缩进显示
- 手动验证：已选分类正确回显（selected 状态）
- 手动验证：分类筛选下拉框同样按层级显示
