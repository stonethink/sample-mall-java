## ADDED Requirements

### Requirement: Build category full path
The system SHALL provide a service to build the full path of a category by traversing its parent chain.

#### Scenario: Build path for root category
- **WHEN** system builds path for a root category "食品生鲜"
- **THEN** the path is "食品生鲜"

#### Scenario: Build path for second-level category
- **WHEN** system builds path for category "水果" with parent "食品生鲜"
- **THEN** the path is "食品生鲜 > 水果"

#### Scenario: Build path for third-level category
- **WHEN** system builds path for category "进口水果" with parent chain "食品生鲜 > 水果"
- **THEN** the path is "食品生鲜 > 水果 > 进口水果"

### Requirement: Convert category IDs to category info list
The system SHALL convert a list of category IDs to a list of CategoryInfo objects containing id, name, and fullPath.

#### Scenario: Convert single category ID
- **WHEN** system converts category ID "cat-001"
- **THEN** it returns CategoryInfo with id="cat-001", name="食品生鲜", fullPath="食品生鲜"

#### Scenario: Convert multiple category IDs
- **WHEN** system converts category IDs ["cat-002", "cat-004"]
- **THEN** it returns list of CategoryInfo with correct names and paths

#### Scenario: Handle non-existent category ID
- **WHEN** system converts a non-existent category ID
- **THEN** it skips that ID and continues processing others
