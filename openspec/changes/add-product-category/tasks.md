## 1. Backend - Category Entity & Repository

- [x] 1.1 Create Category.java entity with id, name, parentId fields
- [x] 1.2 Create CategoryRepository.java for JSON file operations
- [x] 1.3 Create categories.json data file with default categories

## 2. Backend - Category Service

- [x] 2.1 Implement createCategory() with duplicate name validation
- [x] 2.2 Implement updateCategory() with circular reference check
- [x] 2.3 Implement deleteCategory() with children and product check
- [x] 2.4 Implement getAllCategories() and getCategoryTree()
- [x] 2.5 Implement getCategoryById() and getSubCategories()
- [x] 2.6 Add max depth validation (5 levels)

## 3. Backend - Category Controller

- [x] 3.1 Create CategoryController.java with @RestController
- [x] 3.2 Implement POST /api/categories endpoint
- [x] 3.3 Implement PUT /api/categories/{id} endpoint
- [x] 3.4 Implement DELETE /api/categories/{id} endpoint
- [x] 3.5 Implement GET /api/categories endpoint (flat list)
- [x] 3.6 Implement GET /api/categories/tree endpoint (hierarchical)
- [x] 3.7 Implement GET /api/categories/{id} endpoint
- [x] 3.8 Implement GET /api/categories/{id}/children endpoint

## 4. Backend - Product-Category Association

- [x] 4.1 Update Product.java to add categoryIds field (List<String>)
- [x] 4.2 Update ProductService.createProduct() to handle categoryIds
- [x] 4.3 Update ProductService.updateProduct() to handle categoryIds
- [x] 4.4 Update ProductService.getProducts() to support categoryId filter
- [x] 4.5 Add category validation when associating with products

## 5. Frontend - Admin Category Management

- [x] 5.1 Add Category Management section to admin.html
- [x] 5.2 Implement category tree display component
- [x] 5.3 Implement create category form (name, parent selection)
- [x] 5.4 Implement edit category form
- [x] 5.5 Implement delete category with confirmation
- [x] 5.6 Add category selector in product form

## 6. Frontend - Product Category Filter

- [x] 6.1 Add category filter dropdown to product list
- [x] 6.2 Update product list to show category labels
- [x] 6.3 Update product detail view to show categories

## 7. Testing & Verification

- [x] 7.1 Test create root category
- [x] 7.2 Test create sub-category
- [x] 7.3 Test duplicate name validation
- [x] 7.4 Test circular reference prevention
- [x] 7.5 Test delete with children prevention
- [x] 7.6 Test delete with products prevention
- [x] 7.7 Test max depth validation
- [x] 7.8 Test product-category association
- [x] 7.9 Test product filter by category
