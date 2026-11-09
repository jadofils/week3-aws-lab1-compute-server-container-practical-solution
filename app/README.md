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
- The UI is plain HTML/CSS/JavaScript served by Spring Boot from `src/main/resources/static/`, calling the REST API with AJAX (`fetch`). No server-side templating (Thymeleaf) is used.

## Project structure

The backend follows a layered MVC design with SOLID principles: controllers depend on service interfaces, services depend on repository and validator interfaces, and each class has one job.

```text
src/main/java/com/onward/orders/
├── OnwardOrdersApplication.java   # Spring Boot entry point
├── config/        # @ConfigurationProperties (app.info.*, app.orders.*)
├── controller/    # REST controllers: thin HTTP layer only (Order, Info, Health)
├── dto/
│   ├── request/   # Incoming JSON payloads (CreateOrderRequest)
│   └── response/  # Outgoing JSON payloads (OrderResponse, ServerInfoResponse, HealthResponse)
├── exception/     # Domain exceptions + @RestControllerAdvice returning ProblemDetail
├── mapper/        # Domain model -> response DTO conversion
├── model/         # Domain model (Order, OrderStatus)
├── repository/    # OrderRepository interface
│   └── impl/      # InMemoryOrderRepository (swap for a database later)
└── service/       # Service and validator interfaces
    └── impl/      # OrderServiceImpl, InfoServiceImpl, DefaultOrderValidator, HostnameServerIdentityService

src/main/resources/
├── application.properties   # Port, display metadata, order rules
└── static/                  # Front end (the "view")
    ├── index.html
    ├── css/styles.css
    └── js/
        ├── api.js    # AJAX calls to /api/* (data access)
        ├── view.js   # DOM rendering only
        └── app.js    # Controller: wires events -> api -> view
```

Display text (student name, lab name) and order limits are configured in `application.properties` instead of being hard-coded.

This is a learning demo, not a production order system. Its sample orders live only in memory on one server, are lost on restart, and are not shared across instances. Do not enter real customer information. A later production-style design would need shared durable storage and appropriate access controls.

## Tests

```powershell
mvn verify
```

The tests cover the validator rules, the in-memory repository limit, and the full HTTP contract: health, info (name and lab name), creating and listing orders, and error responses. They run in CI before any image is published.

## Delivery (how a merge reaches customers)

This folder is the root of the **application repository** (`onward-orders-app`).

| File | Purpose |
| --- | --- |
| `.github/workflows/build-and-deploy.yml` | On a push to `main`: run the tests, sign in to AWS with **GitHub OIDC** (no stored keys), upload the deployment bundle to S3, then push the image to ECR as `:<commit-sha>` and `:release`. |
| `Dockerfile` | Multi-stage build; runs as a non-root user; includes `curl` for the container health check. |
| `taskdef.json` | ECS task definition template. `${AWS_ACCOUNT_ID}`, `${AWS_REGION}` and `${ENVIRONMENT}` are filled in by the workflow. `<IMAGE1_NAME>` is filled in by CodePipeline. |
| `appspec.yaml` | Tells CodeDeploy which container and port receive traffic. `<TASK_DEFINITION>` is filled in by CodePipeline. |

Pushing the `release` tag triggers EventBridge → CodePipeline → CodeDeploy blue/green (10% canary for 5 minutes, automatic rollback on alarms).

Set these repository variables under *Settings → Secrets and variables → Actions → Variables*. None of them are secrets.

| Variable | Value |
| --- | --- |
| `AWS_REGION` | `eu-west-1` |
| `AWS_GITHUB_ACTIONS_ROLE_ARN` | Output `GitHubActionsRoleArn` of the `onward-dev-security-and-iam` stack |
| `ENVIRONMENT` | `dev` (default) |

Change task CPU and memory in `taskdef.json`, and keep `TaskCpu`/`TaskMemory` in the infrastructure's `6-ecs-service` deployment file the same.
