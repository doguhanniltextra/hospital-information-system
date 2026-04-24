# Security and Compliance Specification

This document outlines the security architecture, threat mitigation strategies, and identity management protocols implemented across the HIS microservices.

## 1. Identity Linking and Authorization

The system utilizes an **Explicit Identity Linking** model to bridge the gap between the `auth-service` (Authentication) and the domain verticals (Authorization).

### Architectural Pattern
- **authUserId**: Every user in the `auth-service` has a unique UUID.
- **Domain Link**: Domain entities (e.g., `Patient`, `Doctor`) store this `authUserId` as a surrogate key.
- **Ownership Verification**: Before performing sensitive operations (e.g., viewing lab results, modifying appointments), services verify that the `authUserId` extracted from the JWT matches the `authUserId` stored on the target domain aggregate.

### Logic Flow
1. User authenticates via `auth-service` and receives a JWT containing their `userId` (as the subject) and `roles`.
2. The user requests a resource (e.g., `/api/patients/me/results`).
3. The `patient-management` service extracts the `userId` from the JWT.
4. It queries the database for a `Patient` record where `authUserId == extractedUserId`.
5. If no link exists or the IDs mismatch, a `403 Forbidden` is returned.

## 2. Privilege Escalation Mitigation

To prevent unauthorized role assignments, the system enforces a strict boundary between public and administrative operations.

### Hardened Registration Flow
- **Public Registration**: The `RegisterRequestDto` has been sanitized to remove the `roles` field. All self-registrations via the public endpoint are hardcoded to the `PATIENT` role at the service layer.
- **Admin-Controlled Mapping**: Role assignments for `DOCTOR`, `RECEPTIONIST`, or `ADMIN` must be performed through an internal, secured administrative interface that requires `ADMIN` privileges.

## 3. Data Exposure Prevention

### Endpoint Sanitization
- **UUID Enumeration**: Sensitive validation endpoints (e.g., `appointment-service/validateIds`) are restricted to `ADMIN` and `RECEPTIONIST` roles to prevent malicious actors from enumerating valid entity IDs.
- **Lab Data Access**: Lab result access is strictly filtered by the `authUserId` link, ensuring patients can only access their own clinical data.

## 4. Standardized Security Configuration

Every service implements a consistent `SecurityConfig` utilizing a standardized `JwtAuthFilter`. 
- **Statelessness**: No session state is maintained on the server; all authorization context is derived from the JWT.
- **CORS/CSRF**: Strictly configured to prevent unauthorized cross-origin requests in web environments.
