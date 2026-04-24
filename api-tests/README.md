# Patient Management System - API Tests

This directory contains `.http` files compatible with **IntelliJ IDEA**, **VS Code (REST Client extension)**, and other modern IDEs. They provide a quick way to test and interact with the microservices.

## 🚀 How to use

1. **Gateway Port**: All requests target the API Gateway on port `4004` via the `@host` variable.
2. **Authentication**: 
   - Start with `auth.http` to register/login.
   - Most requests require a `Bearer Token`. The `.http` files are configured to use variables like `{{authToken}}`.
3. **Variables**: You can define environment variables in your IDE or replace placeholders like `{{patientId}}`, `{{doctorId}}` with actual values from previous responses.

## 📁 File Structure

- `auth.http`: User registration, login, token refresh, and logout.
- `patient.http`: CRUD operations for patients and viewing medical records/lab results.
- `doctor.http`: Searching doctors and checking availability/shifts.
- `appointment.http`: Creating and managing appointments.
- `support.http`: Lab catalog, orders, results, and Inventory/Stock management.
- `admission.http`: Patient admission and discharge workflows.

## 🛠 Prerequisites

- Microservices must be running (e.g., via `docker-compose up`).
- API Gateway must be active on port `4004`.
