# Comet Design Handoff

- Change: hierarchical-category-select
- Phase: design
- Mode: compact
- Context hash: ceda94537a365513c8e9568dbdb31e2bfab2e3afed10b5029f7f23162c9e65fb

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/hierarchical-category-select/proposal.md

- Source: openspec/changes/hierarchical-category-select/proposal.md
- Lines: 1-26
- SHA256: c6a5f7dd4e73328516e2b81fbb992f52376c4ce4858e956a15ebc0534d1312be

```md
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
```

## openspec/changes/hierarchical-category-select/design.md

- Source: openspec/changes/hierarchical-category-select/design.md
- Lines: 1-58
- SHA256: 7ebc2e665b0c86f0ab39d7decf23dc6517c44b9d6af75e636e4a9f5e33947917

```md
## 方案概述

纯前端修改。将商品编辑和分类筛选下拉框改为调用 `/api/categories/tree` 接口，递归渲染带缩进的 `<option>` 元素。

## 架构决策

### 数据源选择

| 方案 | 描述 | 选择 |
|------|------|------|
| A: 前端自行构建树 | 调用 `/api/categories` 获取平铺列表，在前端按 parentId 构建树 | ✗ |
| B: 使用已有 tree 接口 | 调用 `/api/categories/tree` 直接获取树形 JSON | ✓ |

选择 B：后端已有完整的 `getCategoryTree()` 实现，直接复用避免重复逻辑。

### 层级显示方式

| 方案 | 描述 | 选择 |
|------|------|------|
| 1: 缩进前缀 | 用 `"──"` 或空格前缀表示层级深度 | ✓ |
| 2: optgroup 分组 | 用 HTML `<optgroup>` 将一级分类作为分组标签 | ✗ |
| 3: 自定义下拉组件 | 替换 `<select>` 为自定义树形组件 | ✗ |

选择方案 1：
- `<optgroup>` 仅支持一级分组，且 optgroup 本身不可选
- 自定义组件增加复杂度，不符合项目简洁风格
- 缩进前缀简单直观，兼容原生 `<select multiple>`

### 缩进策略

- 每级缩进使用 `"\u00A0\u00A0"` (2个不间断空格) + 前缀符号
- 一级分类：无前缀，显示原名称
- 二级分类：`"├─ 水果"`
- 三级及更深：`"│\u00A0\u00A0├─ 进口水果"`（递增缩进）

简化实现：统一用 `depth * "　"` (全角空格) 作为缩进，无需复杂树线符号。

## 数据流

```
/api/categories/tree
       │
       ▼
  [{id, name, parentId, children: [...]}]
       │
       ▼
  renderCategoryOptions(nodes, depth=0)
       │ 递归遍历
       ▼
  <option value="id">前缀 + name</option>
```

## 影响范围

仅修改 `src/main/resources/static/admin.html`：
1. `loadProductCategorySelect` — 商品编辑分类选择器
2. `loadCategoryFilter` — 商品列表分类筛选器
3. 新增辅助函数 `renderCategoryOptions(nodes, depth, selectedIds)`
```

## openspec/changes/hierarchical-category-select/tasks.md

- Source: openspec/changes/hierarchical-category-select/tasks.md
- Lines: 1-6
- SHA256: e4928a208256cc4b14d779d26cbd004c4717036af544cedf31682d31ad21b073

```md
## Tasks

- [ ] 新增 `renderCategoryOptions(nodes, depth, selectedIds)` 辅助函数，递归生成带缩进的 option HTML
- [ ] 修改 `loadProductCategorySelect` 函数，改为调用 `/api/categories/tree` 并使用 `renderCategoryOptions` 渲染
- [ ] 修改 `loadCategoryFilter` 函数，改为调用 `/api/categories/tree` 并使用 `renderCategoryOptions` 渲染（保留"所有分类"默认选项）
- [ ] 验证：编辑商品时分类按层级缩进显示，已选中分类正确回显
```

