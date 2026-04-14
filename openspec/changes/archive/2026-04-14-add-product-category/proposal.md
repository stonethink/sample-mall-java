## Why

当前商品管理功能缺少分类体系，无法对商品进行有效组织和展示。随着商品数量增加，用户难以快速找到目标商品。添加商品分类功能可以建立商品层级结构，提升商品管理效率和用户体验。

## What Changes

- 新增商品分类数据模型（Category），支持多级分类（父子层级）
- 新增商品分类管理 API（增删改查、层级查询）
- 商品实体关联分类（多对多关系）
- 商品列表支持按分类筛选
- 管理后台新增分类管理页面

## Capabilities

### New Capabilities
- `category-management`: 商品分类的创建、编辑、删除、查询功能，支持无限层级
- `product-category-association`: 商品与分类的关联关系管理

### Modified Capabilities
- `product-management`: 商品管理增加分类筛选和分类信息展示

## Impact

- 数据库：新增 category 表和 product_category 关联表
- 后端 API：新增 CategoryController 及分类相关接口
- 前端：admin.html 新增分类管理模块
- 商品列表接口增加 categoryId 查询参数
