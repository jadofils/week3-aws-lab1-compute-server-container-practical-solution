# Onward order application

A single full-stack Spring Boot application. The REST backend and the browser UI are packaged in **one executable JAR**. The UI is plain HTML, CSS and JavaScript that calls the API with AJAX (`fetch`), with no server-side templates.

## What the page shows

- Your **full name** and the **lab name** ("Lab 1 - Handling Sales Day Demand").
- **Which server answered**: the EC2 instance ID, Availability Zone and private IP. These are read from the instance metadata service using IMDSv2. Off EC2, the app shows the host name instead.
- **Which server is answering?** This panel polls once a second through the load balancer and counts answers per server, so you can watch servers join and leave as capacity changes.
- **Sales-day CPU spike.** This panel starts a bounded CPU spike on the servers that receive the requests, which makes Auto Scaling add servers.
- Sample orders, held in memory on each server.

## API

| Method and path | Purpose |
| --- | --- |
| `GET /health` | Load balancer health check. Returns `{status, server, availabilityZone}`. |
| `GET /api/info` | Name, lab name, app name, server ID, zone and private IP |
| `GET /api/orders`, `POST /api/orders` | List or create sample orders |
| `POST /api/load/spike` `{"seconds": 420}` | Start a CPU spike on the answering server |
| `POST /api/load/stop`, `GET /api/load/status` | Stop a spike, or check its state |

**Why the spike trigger is safe to expose:**
- Its length is limited to 30–600 seconds.
- Each server runs at most one spike at a time.
- It takes no other input.
- It can be switched off with `LOAD_DEMO_ENABLED=false`.

The secure alternative with no UI is Systems Manager Session Manager. See the [live-review checks](../docs/demo/README.md).

## Structure (layered MVC, SOLID)

```text
src/main/java/com/onward/orders/
├── config/        @ConfigurationProperties: app.info.*, app.orders.*, app.identity.*, app.load.*
├── controller/    Thin REST controllers: Order, Info, Health, Load
├── dto/           request/ and response/ JSON shapes
├── exception/     BadRequestException family + @RestControllerAdvice returning ProblemDetail
├── mapper/        Domain model to response DTO
├── model/         Order, OrderStatus, ServerIdentity
├── repository/    OrderRepository + in-memory implementation
└── service/       Interfaces + impl/: OrderServiceImpl, InfoServiceImpl, DefaultOrderValidator,
                   Ec2MetadataServerIdentityService, CpuSpikeLoadGeneratorService
src/main/resources/static/   index.html, css/styles.css, js/api.js (AJAX) · view.js (DOM) · app.js (controller)
```

## Run and test

```powershell
mvn spring-boot:run   # http://localhost:8080
mvn verify            # 18 tests: validator, repository, CPU spike limits, IMDSv2 identity, full HTTP contract
```

## Release

`.github/workflows/release-app.yml` runs the tests on every change. On `main` it publishes `target/onward-orders.jar` as the GitHub Release `v<version from pom.xml>`. Each new version must have a new `<version>` in `pom.xml`.

Every server installs the **latest** release when it launches, from `https://github.com/<owner>/<repo>/releases/latest/download/onward-orders.jar`. Servers that are already running keep their version until they are replaced.
