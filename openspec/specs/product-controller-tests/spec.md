# product-controller-tests Specification

## Purpose
TBD - created by archiving change add-product-controller-tests. Update Purpose after archive.
## Requirements
### Requirement: 测试 listAll 端点

ProductController SHALL 提供 GET /api/products 端点，能够返回所有商品列表，包含分类信息。

#### Scenario: listAll 返回商品列表
- **WHEN** 客户端发送 GET /api/products 请求
- **THEN** 返回 HTTP 200 状态码
- **AND** 返回商品列表数组

### Requirement: 测试 getById 端点

ProductController SHALL 提供 GET /api/products/{id} 端点，能够根据ID返回单个商品。

#### Scenario: getById 返回存在的商品
- **WHEN** 客户端发送 GET /api/products/{id} 请求，且商品存在
- **THEN** 返回 HTTP 200 状态码
- **AND** 返回包含商品详情的 ProductWithCategoriesDTO

#### Scenario: getById 返回不存在的商品
- **WHEN** 客户端发送 GET /api/products/{id} 请求，且商品不存在
- **THEN** 返回 HTTP 404 状态码

### Requirement: 测试 create 端点

ProductController SHALL 提供 POST /api/products 端点，能够创建新商品。

#### Scenario: create 创建新商品成功
- **WHEN** 客户端发送 POST /api/products 请求，包含有效的商品数据
- **THEN** 返回 HTTP 201 状态码
- **AND** 返回创建的商品信息

### Requirement: 测试 update 端点

ProductController SHALL 提供 PUT /api/products/{id} 端点，能够更新商品信息。

#### Scenario: update 更新商品成功
- **WHEN** 客户端发送 PUT /api/products/{id} 请求，包含有效的更新数据
- **THEN** 返回 HTTP 200 状态码
- **AND** 返回更新后的商品信息

### Requirement: 测试 delete 端点

ProductController SHALL 提供 DELETE /api/products/{id} 端点，能够删除商品。

#### Scenario: delete 删除商品成功
- **WHEN** 客户端发送 DELETE /api/products/{id} 请求
- **THEN** 返回 HTTP 204 状态码

