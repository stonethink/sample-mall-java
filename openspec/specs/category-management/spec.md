## ADDED Requirements

### Requirement: Create category
The system SHALL allow creating a new product category with name and optional parent category.

#### Scenario: Create root category
- **WHEN** admin submits a category with name "数码产品" and no parentId
- **THEN** system creates the category with auto-generated ID
- **AND** the category is stored in categories.json

#### Scenario: Create sub-category
- **WHEN** admin submits a category with name "手机" and parentId pointing to "数码产品"
- **THEN** system creates the category as a child of "数码产品"

#### Scenario: Create category with duplicate name
- **WHEN** admin submits a category with name that already exists under the same parent
- **THEN** system returns error "分类名称已存在"

### Requirement: Update category
The system SHALL allow updating category name and parent.

#### Scenario: Update category name
- **WHEN** admin updates category name from "手机" to "智能手机"
- **THEN** system updates the category name
- **AND** all products associated with this category remain linked

#### Scenario: Move category to another parent
- **WHEN** admin changes parentId of "手机" from "数码产品" to "通讯设备"
- **THEN** system updates the parent relationship
- **AND** the category becomes a child of "通讯设备"

#### Scenario: Prevent circular reference
- **WHEN** admin tries to set parentId to one of the category's descendants
- **THEN** system returns error "不能将分类设置为其子分类的后代"

### Requirement: Delete category
The system SHALL allow deleting a category only if it has no sub-categories and no associated products.

#### Scenario: Delete leaf category
- **WHEN** admin deletes a category with no children and no products
- **THEN** system removes the category from categories.json

#### Scenario: Prevent deleting category with children
- **WHEN** admin tries to delete a category that has sub-categories
- **THEN** system returns error "请先删除子分类"

#### Scenario: Prevent deleting category with products
- **WHEN** admin tries to delete a category that has associated products
- **THEN** system returns error "该分类下存在商品，无法删除"

### Requirement: Query categories
The system SHALL support querying categories in various formats.

#### Scenario: List all categories
- **WHEN** user requests all categories
- **THEN** system returns flat list of all categories

#### Scenario: Get category tree
- **WHEN** user requests category tree
- **THEN** system returns hierarchical structure with children nested under parents

#### Scenario: Get category by ID
- **WHEN** user requests category by specific ID
- **THEN** system returns the category details including parent info if exists

#### Scenario: Get sub-categories
- **WHEN** user requests sub-categories of a specific parent ID
- **THEN** system returns all direct children of that category

### Requirement: Category hierarchy validation
The system SHALL enforce maximum hierarchy depth.

#### Scenario: Create category exceeding max depth
- **WHEN** admin tries to create a category at level 6 (max is 5)
- **THEN** system returns error "分类层级不能超过5级"
