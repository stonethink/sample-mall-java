## ADDED Requirements

### Requirement: Order has a status field
Each order SHALL have a `status` field representing its current lifecycle stage. The status SHALL be one of: `PENDING_PAYMENT`, `PAID`, `SHIPPED`, `COMPLETED`, `CANCELLED`.

#### Scenario: New order defaults to PENDING_PAYMENT
- **WHEN** a new order is created via `POST /api/orders`
- **THEN** the order's status SHALL be `PENDING_PAYMENT`
- **AND** the status field SHALL be included in the response body

#### Scenario: Order response includes status
- **WHEN** an order is retrieved via `GET /api/orders/{id}` or listed via `GET /api/orders`
- **THEN** each order object SHALL include the `status` field with its current value

### Requirement: Order status transitions follow defined rules
The system SHALL enforce valid state transitions and reject illegal ones.

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

### Requirement: Status change via dedicated API endpoint
The system SHALL provide a dedicated endpoint for changing order status, separate from the general order update endpoint.

#### Scenario: Update order status
- **WHEN** a `PUT /api/orders/{id}/status` request is sent with body `{"status": "PAID"}`
- **THEN** the system SHALL validate the transition and update the order status
- **AND** return the updated order with HTTP 200

#### Scenario: Status change for non-existent order
- **WHEN** a `PUT /api/orders/{id}/status` request is sent for a non-existent order ID
- **THEN** the system SHALL return HTTP 404

### Requirement: Filter orders by status
The system SHALL support filtering the order list by status.

#### Scenario: List orders filtered by status
- **WHEN** a `GET /api/orders?status=PAID` request is sent
- **THEN** the system SHALL return only orders with `PAID` status

#### Scenario: List all orders without filter
- **WHEN** a `GET /api/orders` request is sent without a `status` parameter
- **THEN** the system SHALL return all orders regardless of status
