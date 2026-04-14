## MODIFIED Requirements

### Requirement: Product list supports category filter
The product list API SHALL support filtering by category ID.

#### Scenario: Filter products by category
- **WHEN** user requests product list with categoryId parameter "cat-001"
- **THEN** system returns only products associated with that category

#### Scenario: Filter with non-existent category
- **WHEN** user requests product list with invalid categoryId
- **THEN** system returns empty list

### Requirement: Product details include categories
The product details SHALL include associated category information.

#### Scenario: Get product with categories
- **WHEN** user requests product details for product with categories
- **THEN** system returns product data including categoryIds and category names
