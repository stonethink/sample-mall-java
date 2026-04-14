## 1. Backend - Category Path Service

- [ ] 1.1 Create CategoryInfo DTO class with id, name, fullPath fields
- [ ] 1.2 Add buildCategoryPath() method in CategoryService to build full path
- [ ] 1.3 Add convertToCategoryInfoList() method to convert category IDs to CategoryInfo list
- [ ] 1.4 Add getCategoryPath() method to get full path for a single category

## 2. Backend - Product API Enhancement

- [ ] 2.1 Create ProductWithCategoriesDTO to include categories field
- [ ] 2.2 Update ProductService.listAll() to return products with category info
- [ ] 2.3 Update ProductService.listByCategory() to return products with category info
- [ ] 2.4 Update ProductService.findById() to return product with category info
- [ ] 2.5 Update ProductController to return enhanced product data

## 3. Frontend - Admin Display Update

- [ ] 3.1 Update admin.html product list to display category fullPath instead of IDs
- [ ] 3.2 Update product table rendering to show categories as readable text
- [ ] 3.3 Test category path display with single-level categories
- [ ] 3.4 Test category path display with multi-level categories

## 4. Testing & Verification

- [ ] 4.1 Test product list API returns categories with fullPath
- [ ] 4.2 Test product detail API returns categories with fullPath
- [ ] 4.3 Test backward compatibility (categoryIds field still present)
- [ ] 4.4 Test edge case: product with no categories
- [ ] 4.5 Test edge case: category with deep nesting (3+ levels)
