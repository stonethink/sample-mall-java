## MODIFIED Requirements

### Requirement: Product list returns category information with names
The product list API SHALL return category information including names and full paths, in addition to category IDs.

#### Scenario: List products with category info
- **WHEN** user requests product list
- **THEN** each product includes a "categories" field containing list of CategoryInfo objects
- **AND** each CategoryInfo contains id, name, and fullPath
- **AND** the original "categoryIds" field is preserved for backward compatibility

#### Scenario: Product with multiple categories shows all paths
- **WHEN** a product belongs to categories "食品生鲜 > 水果" and "进口商品"
- **THEN** the categories field contains both CategoryInfo objects with their respective full paths

#### Scenario: Product with no categories
- **WHEN** a product has no category associations
- **THEN** the categories field is an empty list
- **AND** the categoryIds field is null or empty

### Requirement: Product details include category information
The product detail API SHALL return category information with names and full paths.

#### Scenario: Get product details with categories
- **WHEN** user requests product details by ID
- **THEN** the response includes categories field with CategoryInfo list
- **AND** each CategoryInfo shows the full path from root to the category
