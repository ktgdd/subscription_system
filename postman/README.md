# Postman Collection for Subscription Management API

## Overview
This Postman collection contains all API endpoints for the Subscription Management Service.

## Setup

### 1. Import Collection
- Open Postman
- Click "Import" button
- Select `Subscription_API.postman_collection.json`

### 2. Configure Environment Variables
The collection uses the following variables:
- `baseUrl`: Base URL of the API (default: `http://localhost:8080`)
- `adminToken`: JWT token with ADMIN role
- `userToken`: JWT token with USER role

### 3. Generate JWT Tokens
You need to generate JWT tokens with the following claims:
- `userId`: User ID (Long)
- `role`: Either "ADMIN" or "USER"
- `requestId`: Optional request ID for idempotency

#### Example JWT Payload (Admin):
```json
{
  "userId": "1",
  "role": "ADMIN",
  "requestId": "req-123",
  "iat": 1234567890,
  "exp": 1234654290
}
```

#### Example JWT Payload (User):
```json
{
  "userId": "100",
  "role": "USER",
  "requestId": "req-456",
  "iat": 1234567890,
  "exp": 1234654290
}
```

You can use online JWT generators or create a simple utility to generate tokens with your secret key.

## API Endpoints

### Health Check
- `GET /health` - Basic health check

### Subscription Plans (Admin Only)
- `GET /api/subscription-plans` - Get all plans (optionally filtered by accountId)
- `GET /api/subscription-plans/{id}` - Get plan by ID
- `POST /api/subscription-plans` - Create new plan
- `PUT /api/subscription-plans/{id}` - Update plan
- `DELETE /api/subscription-plans/{id}` - Delete plan (soft delete)

### User Subscriptions
- `POST /api/subscriptions` - Subscribe to a plan
- `POST /api/subscriptions/{id}/extend` - Extend existing subscription
- `GET /api/subscriptions` - Get all user subscriptions
- `GET /api/subscriptions/{id}` - Get subscription by ID

### Actuator Endpoints
- `GET /actuator/health` - Detailed health check
- `GET /actuator/metrics` - List all metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Testing Scenarios

### 1. Admin Flow
1. Set `adminToken` variable with admin JWT
2. Create subscription plan
3. Get all plans
4. Update plan
5. Get plan by ID

### 2. User Flow
1. Set `userToken` variable with user JWT
2. Get all plans (should fail - admin only)
3. Subscribe to a plan
4. Get user subscriptions
5. Extend subscription
6. Get subscription by ID

### 3. Idempotency Testing
1. Make the same subscribe request twice with same JWT (same requestId)
2. Second request should fail with duplicate request error

### 4. Rate Limiting
1. Make multiple rapid requests
2. After 100 requests per minute, should get 429 Too Many Requests

## Notes
- All admin endpoints require `Authorization: Bearer <adminToken>`
- All user subscription endpoints require `Authorization: Bearer <userToken>`
- Idempotency is handled automatically using userId, accountId, durationTypeId, and requestId
- Payment processing is asynchronous - subscription status will be "INITIATED" initially
- Use `X-Correlation-ID` header for distributed tracing

