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
