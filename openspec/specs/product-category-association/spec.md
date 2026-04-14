## ADDED Requirements

### Requirement: Associate product with categories
The system SHALL allow associating a product with one or more categories.

#### Scenario: Create product with categories
- **WHEN** admin creates a product with categoryIds ["cat-001", "cat-002"]
- **THEN** system stores the product with the category associations
- **AND** the product appears in both categories

#### Scenario: Update product categories
- **WHEN** admin updates product to have categoryIds ["cat-003"]
- **THEN** system updates the category associations
- **AND** the product is removed from previous categories and added to new one

#### Scenario: Remove all categories from product
- **WHEN** admin updates product with empty categoryIds []
- **THEN** system removes all category associations from the product

### Requirement: Validate category association
The system SHALL validate that associated categories exist.

#### Scenario: Associate with non-existent category
- **WHEN** admin tries to associate product with categoryId "non-existent"
- **THEN** system returns error "分类不存在"

### Requirement: Query products by category
The system SHALL support querying products by category.

#### Scenario: List products in category
- **WHEN** user requests products with categoryId "cat-001"
- **THEN** system returns all products associated with that category

#### Scenario: Include category info in product
- **WHEN** user requests product details
- **THEN** system returns product with associated category names and IDs
