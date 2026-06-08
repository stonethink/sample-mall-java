## ADDED Requirements

### Requirement: Order has a status field
Each order SHALL have a `status` field representing its current lifecycle stage. The status SHALL be one of: `PENDING_PAYMENT`, `PAID`, `SHIPPED`, `COMPLETED`, `CANCELLED`.

#### Scenario: New order defaults to PENDING_PAYMENT
- **WHEN** a new order is created via `POST /api/orders`
- **THEN** the order's status SHALL be `PENDING_PAYMENT`
- **AND** the status field SHALL be included in the response body

#### Scenario: Create order ignores client-supplied status
- **WHEN** a new order is created via `POST /api/orders`
- **AND** the request body contains a `status` field with any value (e.g., `"status": "COMPLETED"`)
- **THEN** the system SHALL ignore the client-supplied status value
- **AND** the created order's status SHALL be `PENDING_PAYMENT`

#### Scenario: Order response includes status
- **WHEN** an order is retrieved via `GET /api/orders/{id}` or listed via `GET /api/orders`
- **THEN** each order object SHALL include the `status` field with its current value

### Requirement: Order status transitions follow defined rules
The system SHALL enforce valid state transitions and reject illegal ones.

**Valid transitions:**
- `PENDING_PAYMENT` → `PAID`
- `PENDING_PAYMENT` → `CANCELLED`
- `PAID` → `SHIPPED`
- `PAID` → `CANCELLED`
- `SHIPPED` → `COMPLETED`

**All other transitions SHALL be rejected with HTTP 400.**

#### Scenario: Confirm payment
- **WHEN** an order is in `PENDING_PAYMENT` status
- **AND** a status change to `PAID` is requested
- **THEN** the order status SHALL be updated to `PAID`

#### Scenario: Ship a paid order
- **WHEN** an order is in `PAID` status
- **AND** a status change to `SHIPPED` is requested
- **THEN** the order status SHALL be updated to `SHIPPED`

#### Scenario: Complete a shipped order
- **WHEN** an order is in `SHIPPED` status
- **AND** a status change to `COMPLETED` is requested
- **THEN** the order status SHALL be updated to `COMPLETED`

#### Scenario: Cancel a pending payment order
- **WHEN** an order is in `PENDING_PAYMENT` status
- **AND** a status change to `CANCELLED` is requested
- **THEN** the order status SHALL be updated to `CANCELLED`

#### Scenario: Cancel a paid order
- **WHEN** an order is in `PAID` status
- **AND** a status change to `CANCELLED` is requested
- **THEN** the order status SHALL be updated to `CANCELLED`

#### Scenario: Reject illegal status transition
- **WHEN** an order is in `COMPLETED` status
- **AND** a status change to `PENDING_PAYMENT` is requested
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** return an error message indicating the transition is not allowed

#### Scenario: Reject transition from CANCELLED
- **WHEN** an order is in `CANCELLED` status
- **AND** any status change is requested
- **THEN** the system SHALL reject the request with HTTP 400

#### Scenario: Reject unsupported transition from PENDING_PAYMENT to SHIPPED
- **WHEN** an order is in `PENDING_PAYMENT` status
- **AND** a status change to `SHIPPED` is requested
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the transition is not allowed

#### Scenario: Reject unsupported transition from SHIPPED to CANCELLED
- **WHEN** an order is in `SHIPPED` status
- **AND** a status change to `CANCELLED` is requested
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the transition is not allowed

#### Scenario: Reject same-status update
- **WHEN** an order is in `PAID` status
- **AND** a status change to `PAID` is requested
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the transition is not allowed

### Requirement: Status change via dedicated API endpoint
The system SHALL provide a dedicated endpoint for changing order status, separate from the general order update endpoint.

**Note:** The general order update endpoint `PUT /api/orders/{id}` SHALL NOT modify the order status. If the request body contains a `status` field, the system SHALL either ignore it or reject the request with HTTP 400.

#### Scenario: Update order status
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with body `{"status": "PAID"}`
- **THEN** the system SHALL validate the transition and update the order status
- **AND** return the updated order with HTTP 200

#### Scenario: Status change for non-existent order
- **WHEN** a `PUT /api/orders/{id}/status` request is sent for a non-existent order ID
- **THEN** the system SHALL return HTTP 404
- **AND** the response body SHALL be a JSON object containing at least `error` and `message` fields
- **AND** the response body SHALL include the requested `orderId`

#### Scenario: Reject unknown status value in status update request
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with body `{"status": "UNKNOWN"}`
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status value is invalid
- **AND** the response body SHALL include `allowedValues` listing all valid status values

#### Scenario: Reject missing status field in status update request
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with an empty body or without the `status` field
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating `status` is required

#### Scenario: Reject case-mismatched status value
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with body `{"status": "paid"}` (lowercase)
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status value is invalid

### Requirement: Error response format for status operations
The system SHALL return structured error responses for all 400 and 404 errors related to status operations.

#### Scenario: Illegal transition returns structured error body
- **WHEN** an order is in `PAID` status
- **AND** a status change to `COMPLETED` is requested via `PUT /api/orders/{id}/status`
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL be a JSON object containing at least `error`, `message`, `orderId`, `currentStatus`, and `requestedStatus`
- **AND** the response body SHOULD include `allowedTransitions` listing valid target statuses from the current state

**Example error response for illegal transition:**
```json
{
  "error": "INVALID_STATUS_TRANSITION",
  "message": "Cannot transition from PAID to COMPLETED",
  "orderId": 1001,
  "currentStatus": "PAID",
  "requestedStatus": "COMPLETED",
  "allowedTransitions": ["SHIPPED", "CANCELLED"]
}
```

### Requirement: Filter orders by status
The system SHALL support filtering the order list by status.

#### Scenario: List orders filtered by status
- **WHEN** a `GET /api/orders?status=PAID` request is sent
- **THEN** the system SHALL return only orders with `PAID` status

#### Scenario: List all orders without filter
- **WHEN** a `GET /api/orders` request is sent without a `status` parameter
- **THEN** the system SHALL return all orders regardless of status

#### Scenario: Empty result when filtering by status
- **WHEN** a `GET /api/orders?status=PAID` request is sent
- **AND** no orders have `PAID` status
- **THEN** the system SHALL return HTTP 200
- **AND** the response body SHALL be an empty array `[]`

#### Scenario: Reject invalid status filter value
- **WHEN** a `GET /api/orders?status=UNKNOWN` request is sent
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status filter is invalid

#### Scenario: Reject empty status filter value
- **WHEN** a `GET /api/orders?status=` request is sent with an empty value
- **THEN** the system SHALL reject the request with HTTP 400
- **AND** the response body SHALL contain an error message indicating the status filter is invalid

### Requirement: Backward compatibility for orders without status
The system SHALL handle orders that do not have a `status` field (legacy data) gracefully.

#### Scenario: Legacy orders default to PENDING_PAYMENT
- **WHEN** an order without a `status` field is loaded from data source
- **THEN** the system SHALL treat the order as having `PENDING_PAYMENT` status
- **AND** the order SHALL be included in `GET /api/orders?status=PENDING_PAYMENT` results
