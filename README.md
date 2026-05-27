# CRM Backend (Sales Management)

Interview-ready Spring Boot REST API.

## What it includes
- `Customers` CRUD
- `Inventory Items` CRUD
- `Sales` creation that reduces inventory (simple sales-tracking + inventory handling)
- Optional basic Salesforce integration endpoint: `GET /api/salesforce/accounts`

## Prerequisites
- Java 17
- Maven
- MySQL 8+

## Configure database
Set environment variables (or edit `src/main/resources/application.properties`):
- `DB_URL` (default: `jdbc:mysql://localhost:3306/crm`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default: empty)

## Configure Salesforce (optional)
Set:
- `SALESFORCE_INSTANCE_URL` (e.g. `https://yourDomain.my.salesforce.com`)
- `SALESFORCE_ACCESS_TOKEN` (a valid Bearer token)
- `SALESFORCE_API_VERSION` (default: `59.0`)

If these are not set, `/api/salesforce/accounts` returns `501 Not Implemented`.

## Run locally
From `crm-backend/`:
```bash
mvn spring-boot:run
```

API base: `http://localhost:8080/api`

