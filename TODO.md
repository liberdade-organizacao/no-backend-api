# Plan for Implementing User Input Validation

This document outlines the step-by-step plan to introduce user input validation into the BaaS API. The goal is to ensure that all incoming requests (payloads and headers) conform to the specifications defined in `docs/api.md` before they are processed by the business logic layer.

## Overview
The implementation will focus on validating:
1.  **Request Payloads (JSON/MsgPack):** Ensuring required keys are present and have the correct types (e.g., string, bytes).
2.  **Request Headers:** Ensuring required authentication and context headers (e.g., `x-client-auth-key`, `x-user-auth-key`, `x-filename`) are present when required by the endpoint.
3.  **Query Parameters:** Validating parameters passed via the URL.

## Implementation Strategy
To maintain a clear separation of concerns and make the implementation manageable for less capable models, we will use a middleware-like approach or a dedicated validation utility.

### Step 1: Define Validation Schema and Utilities
- [ ] Create a new namespace `br.bsb.liberdade.baas.validation` to house validation logic.
- [ ] Implement a generic validation function that takes a schema (as a map) and the input data (params) and returns either the valid data or a collection of error messages.
- [ ] Define helper functions to validate specific types (e.g., `validate-string`, `validate-email`, `validate-presence`).

### Step 2: Identify Endpoints and Requirements
- [ ] Create a mapping of all routes in `src/br/bsb/liberdade/baas/api.clj` to their expected payload/header requirements based on `docs/api.md`.
- [ ] Document these requirements clearly to serve as the "source of truth" for the validation implementation.

### Step 3: Implement Validation for Individual Handlers
- [ ] For each handler in `src/br/bsb/liberdade/baas/api.clj`:
    - [ ] Identify where the payload is extracted (e.g., `boilerplate-in`).
    - [ ] Insert a call to the validation utility immediately after extraction.
    - [ ] If validation fails, use `boilerplate-out` to return a `400 Bad Request` with a descriptive error message.

### Step 4: Implement Header and Query Param Validation
- [ ] Extend the validation utility or create new validators specifically for `req` headers and `query-string` parameters.
- [ ] Apply these validators to the relevant handlers (e.g., `list-apps`, `upload-user-file`).

### Step 5: Verification and Testing
- [ ] Create or update tests in `test/br/bsb/liberdade/baas/api_test.clj` to include:
    - [ ] Positive test cases: Valid requests pass through.
    - [ ] Negative test cases: Missing required keys, incorrect types, and missing required headers return `400 Bad Request`.
- [ ] Run the existing test suite to ensure no regressions were introduced.

## Error Response Format
All validation errors should return a consistent JSON/MsgPack structure:
```json
{
  "error": "Validation Failed",
  "details": {
    "email": "must be a valid email address",
    "auth_key": "is required"
  }
}
```
