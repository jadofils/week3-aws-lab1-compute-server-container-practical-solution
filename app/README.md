# Onward Orders Demo

A small Java Spring Boot application for Lab 1. The browser UI and REST backend are packaged in the same executable JAR.

## Local run

Requirements: JDK 21 and Maven 3.6.3 or later.

From the `app` directory:

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080`.

## What it demonstrates

- `GET /api/info` returns the student name, lab name, app name, and server identifier.
- `GET /health` is a simple health-check endpoint for the future load balancer target group.
- `GET /api/orders` lists recent sample orders held by this application process.
- `POST /api/orders` accepts JSON with `customerName`, `item`, and `quantity`, then returns an order ID, `RECEIVED` status, timestamp, and serving host.
- The UI is served from `src/main/resources/static/index.html` by Spring Boot.

This is a learning demo, not a production order system. Its sample orders live only in memory on one server, are lost on restart, and are not shared across instances. Do not enter real customer information. A later production-style design would need shared durable storage and appropriate access controls.
