# ExpertMatch Authorization Guide

This guide explains how authorization is implemented in ExpertMatch and how to test the service with authentication.

## Table of Contents

1. [Authorization Overview](#authorization-overview)
2. [Spring Gateway Integration](#spring-gateway-integration)
3. [Header-Based Authentication](#header-based-authentication)
4. [Role-Based Access Control](#role-based-access-control)
5. [Testing with Headers](#testing-with-headers)
6. [Testing Scenarios](#testing-scenarios)
7. [Troubleshooting](#troubleshooting)

---

## Authorization Overview

ExpertMatch uses **header-based authentication** where **Spring Gateway** handles authentication and authorization, then
populates user information in HTTP headers. The implementation follows these principles:

- **Gateway-Based Security**: Spring Gateway validates JWT tokens and handles authentication
- **Header Propagation**: User information is passed via HTTP headers
- **Stateless Service**: ExpertMatch service is stateless and trusts headers from Spring Gateway
- **Role-Based Access Control (RBAC)**: Access is controlled by roles in headers

### Security Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Client    │────────▶│ Spring       │────────▶│ ExpertMatch │
│  (Frontend) │  JWT    │ Gateway      │ Headers │   Service   │
│             │  Token  │              │         │             │
└─────────────┘         └──────────────┘         └─────────────┘
                              │
                              │ Validates JWT and populates:
                              │ - X-User-Id: User identifier
                              │ - X-User-Roles: Comma-separated roles
                              │ - X-User-Email: User email
```

---

## Spring Gateway Integration

### Architecture

ExpertMatch Service is designed to be deployed **behind Spring Gateway**, which:

1. **Validates JWT Tokens**: Spring Gateway validates JWT tokens from clients
2. **Extracts User Information**: Extracts user ID, roles, and email from JWT claims
3. **Populates Headers**: Adds user information to HTTP headers before forwarding to ExpertMatch Service
4. **Handles Authorization**: Can enforce role-based access at the gateway level

### Gateway Configuration

Spring Gateway should be configured to:

- Validate JWT tokens from OAuth2 authorization server
- Extract user information from JWT claims:

      - User ID from `sub` claim
    - Roles from `authorities` claim (array)
    - Email from `email` claim (optional)
- Add headers to requests:

      - `X-User-Id`: User identifier
    - `X-User-Roles`: Comma-separated list of roles (e.g., "ROLE_USER,ROLE_ADMIN")
    - `X-User-Email`: User email address (optional)

---

## Header-Based Authentication

### Required Headers

ExpertMatch Service expects the following headers (populated by Spring Gateway):

| Header         | Required | Description                   | Example                |
|----------------|----------|-------------------------------|------------------------|
| `X-User-Id`    | Yes      | User identifier               | `user-123`             |
| `X-User-Roles` | No       | Comma-separated list of roles | `ROLE_USER,ROLE_ADMIN` |
| `X-User-Email` | No       | User email address            | `user@example.com`     |

**Note**: These headers are now documented in the OpenAPI specification and will appear in Swagger UI for all endpoints.
See the [OpenAPI Documentation](#openapi-documentation) section for details.

### Header Format

**X-User-Id**:

- Single string value
- Example: `user-123`

**X-User-Roles**:

- Comma-separated list of roles
- No spaces around commas (or spaces are trimmed)
- Example: `ROLE_USER,ROLE_ADMIN` or `ROLE_USER, ROLE_ADMIN`

**X-User-Email**:

- Email address string
- Example: `user@example.com`

### Service Configuration

Header names can be configured in `application.yml`:

```yaml
expertmatch:
  security:
    headers:
      user-id: ${EXPERTMATCH_SECURITY_HEADER_USER_ID:X-User-Id}
      user-roles: ${EXPERTMATCH_SECURITY_HEADER_USER_ROLES:X-User-Roles}
      user-email: ${EXPERTMATCH_SECURITY_HEADER_USER_EMAIL:X-User-Email}
```

### Fallback Behavior

If headers are missing (e.g., in local development without Spring Gateway), the service will:

- Use `"anonymous-user"` as the default user ID
- Return empty list for roles
- Return `null` for email

This allows local development and testing without requiring Spring Gateway.

---

## Role-Based Access Control

### Endpoint Access Matrix

| Endpoint               | Public | Authenticated | ADMIN Role                       |
|------------------------|--------|---------------|----------------------------------|
| `/actuator/health`     | ✅      | -             | -                                |
| `/actuator/info`       | ✅      | -             | -                                |
| `/api/v1/query/**`     | -      | ✅             | -                                |
| `/api/v1/chats/**`     | -      | ✅             | -                                |
| `/api/v1/test-data/**` | -      | -             | ✅ Required (enforced by Gateway) |
| `/api/v1/health`       | -      | ✅             | -                                |
| `/api/v1/metrics`      | -      | ✅             | -                                |
| `/mcp/**`              | -      | ✅             | -                                |

### Role Format

Roles in the `X-User-Roles` header:

- **Format**: `ROLE_<ROLE_NAME>` (e.g., `ROLE_ADMIN`, `ROLE_USER`)
- **Separator**: Comma (`,`)
- **Example**: `ROLE_USER,ROLE_ADMIN`

### Common Roles

- `ROLE_USER` or `ROLE_EXPERT_MATCH_USER`: Standard user, can query and manage chats
- `ROLE_ADMIN` or `ROLE_EXPERT_MATCH_ADMIN`: Admin user, can access ingestion endpoints
- `ROLE_EXPERT_MATCH_TESTER`: Tester role (for test data generation, disabled in production)

**Note**: Authorization is primarily handled by Spring Gateway. ExpertMatch Service trusts the headers provided by the
gateway.

---

## Testing with Headers

### Using cURL

**Basic Request**:
```bash
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: user-123" \
  -H "X-User-Roles: ROLE_USER" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Looking for experts in Java, Spring Boot, and AWS"
  }'
```

**With Multiple Roles**:
```bash
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: admin-456" \
  -H "X-User-Roles: ROLE_USER,ROLE_ADMIN" \
  -H "X-User-Email: admin@example.com" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Looking for experts in Java"
  }'
```

**With Environment Variables**:

```bash
export USER_ID="user-123"
export USER_ROLES="ROLE_USER"
export USER_EMAIL="user@example.com"

curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: $USER_ID" \
  -H "X-User-Roles: $USER_ROLES" \
  -H "X-User-Email: $USER_EMAIL" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Looking for experts in Java"
  }'
```

### Option 3: Using HTTPie

```bash
http POST http://localhost:8093/api/v1/query \
  X-User-Id:user-123 \
  X-User-Roles:"ROLE_USER" \
  query="Looking for experts in Java, Spring Boot, and AWS"
```

### Option 4: Using Swagger UI

Swagger UI allows you to set headers manually:

1. Open Swagger UI: `http://localhost:8093/swagger-ui.html`
2. Click "Authorize" button (if available) or set headers manually
3. For each request, you can add headers:

     - `X-User-Id`: Your user ID
    - `X-User-Roles`: Your roles (comma-separated)
    - `X-User-Email`: Your email (optional)

---

## Testing Scenarios

### Scenario 1: Regular User Testing

**Header Requirements**:

- `X-User-Id`: Any user ID
- `X-User-Roles`: `ROLE_USER` or `ROLE_EXPERT_MATCH_USER`

**Allowed Endpoints**:

- ✅ All query endpoints
- ✅ All chat management endpoints
- ✅ System endpoints (health, metrics)
- ✅ MCP server endpoints
- ❌ Ingestion endpoints (requires ADMIN role, enforced by Gateway)

**Test Request**:
```bash
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: user-123" \
  -H "X-User-Roles: ROLE_USER" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Looking for experts in Java"
  }'
```

### Scenario 2: Admin User Testing

**Header Requirements**:

- `X-User-Id`: Any user ID
- `X-User-Roles`: `ROLE_ADMIN` or `ROLE_EXPERT_MATCH_ADMIN` (or both)

**Allowed Endpoints**:

- ✅ All regular user endpoints
- ✅ All ingestion endpoints (if Gateway allows)

**Test Request**:
```bash
curl -X POST "http://localhost:8093/api/v1/test-data?size=small" \
  -H "X-User-Id: admin-456" \
  -H "X-User-Roles: ROLE_ADMIN"
```

### Scenario 3: Missing Headers (Fallback)

**Behavior**: Service falls back to `"anonymous-user"` if `X-User-Id` is missing

**Test Request**:
```bash
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Test query"
  }'
```

**Result**: Request succeeds with `userId = "anonymous-user"` (useful for local development)

### Scenario 4: Multiple Roles

**Header Requirements**:

- `X-User-Id`: Any user ID
- `X-User-Roles`: `ROLE_USER,ROLE_ADMIN` (comma-separated)

**Test Request**:
```bash
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: power-user-789" \
  -H "X-User-Roles: ROLE_USER,ROLE_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Test query"
  }'
```

---

## Troubleshooting

### Issue: Headers Not Recognized

**Possible Causes**:

1. Header names don't match configuration
2. Headers are missing from request
3. Spring Gateway not configured correctly

**Solutions**:

1. Verify header names match configuration (default: `X-User-Id`, `X-User-Roles`, `X-User-Email`)
2. Check request includes headers
3. Verify Spring Gateway is populating headers correctly

**Debug Steps**:
```bash
# Check headers in request
curl -v -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: user-123" \
  -H "X-User-Roles: ROLE_USER" \
  -H "Content-Type: application/json" \
  -d '{"query": "Test"}'
```

### Issue: Roles Not Parsed Correctly

**Possible Causes**:

1. Incorrect format in `X-User-Roles` header
2. Extra spaces or formatting issues

**Solutions**:

1. Ensure roles are comma-separated: `ROLE_USER,ROLE_ADMIN`
2. Spaces around commas are acceptable (will be trimmed)
3. Verify role format includes `ROLE_` prefix

**Debug Steps**:
```bash
# Test with different role formats
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: user-123" \
  -H "X-User-Roles: ROLE_USER,ROLE_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"query": "Test"}'
```

### Issue: Service Uses "anonymous-user"

**Possible Causes**:

1. `X-User-Id` header is missing
2. Header value is empty or blank

**Solutions**:

1. Ensure `X-User-Id` header is present in request
2. Verify header value is not empty
3. Check Spring Gateway is forwarding headers correctly

**Note**: This is expected behavior for local development without Spring Gateway. In production, Spring Gateway should
always populate the header.

---

## Security Best Practices

### Development

1. **Local Development**: Headers can be set manually for testing
2. **Fallback Behavior**: Service gracefully handles missing headers (uses "anonymous-user")
3. **Test Utilities**: Use `TestHeaderHelper` in tests to set headers

### Production

1. **Always Deploy Behind Spring Gateway**: Never expose ExpertMatch Service directly
2. **Gateway Validation**: Spring Gateway must validate JWT tokens before forwarding
3. **Header Trust**: Service trusts headers from Spring Gateway (ensure gateway is secure)
4. **HTTPS**: Use HTTPS for all API communication
5. **Header Validation**: Consider adding header validation in production (optional)

---

## Quick Reference

### Environment Variables

```bash
# Header configuration (optional, defaults shown)
export EXPERTMATCH_SECURITY_HEADER_USER_ID="X-User-Id"
export EXPERTMATCH_SECURITY_HEADER_USER_ROLES="X-User-Roles"
export EXPERTMATCH_SECURITY_HEADER_USER_EMAIL="X-User-Email"
```

### cURL Examples

**Authenticated Request**:
```bash
curl -H "X-User-Id: user-123" \
  -H "X-User-Roles: ROLE_USER" \
  http://localhost:8093/api/v1/chats
```

**Admin Request**:
```bash
curl -X POST \
  -H "X-User-Id: admin-456" \
  -H "X-User-Roles: ROLE_ADMIN" \
  "http://localhost:8093/api/v1/test-data?size=small"
```

**Request with Email**:

```bash
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: user-123" \
  -H "X-User-Roles: ROLE_USER" \
  -H "X-User-Email: user@example.com" \
  -H "Content-Type: application/json" \
  -d '{"query": "Test query"}'
```

### API Client Setup

Configure your API client to include the following headers in requests:

- `X-User-Id`: Your user ID
- `X-User-Roles`: Your roles (comma-separated)
- `X-User-Email`: Your email (optional)

---

## OpenAPI Documentation

The ExpertMatch API is fully documented in the OpenAPI specification, including:

- **User authentication headers** (`X-User-Id`, `X-User-Roles`, `X-User-Email`) documented as parameters for all
  endpoints
- Request/response schemas
- Request/response examples
- Validation rules
- Error response schemas

### Accessing OpenAPI Documentation

- **Swagger UI**: http://localhost:8093/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8093/api/v1/openapi.json

In Swagger UI, you can:

- See all required and optional headers in the Parameters section for each endpoint
- Test endpoints directly with header values
- View example values for headers
- Generate client code with proper header handling

All endpoints that require user identification now have the user headers documented in the Parameters section, making it
easy to see which headers are required or optional.

---

## Additional Resources

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [API Endpoints Documentation](API_ENDPOINTS.md)

---

## Summary

ExpertMatch uses header-based authentication where Spring Gateway handles security:

- **Authentication**: Spring Gateway validates JWT tokens
- **Header Propagation**: User information passed via HTTP headers (`X-User-Id`, `X-User-Roles`, `X-User-Email`)
- **Service Trust**: ExpertMatch Service trusts headers from Spring Gateway
- **Fallback**: Service uses "anonymous-user" if headers are missing (for local development)
- **Testing**: Set headers manually in your API client, cURL, or Swagger UI

For production, always deploy ExpertMatch Service behind Spring Gateway with proper JWT validation and header
population.
