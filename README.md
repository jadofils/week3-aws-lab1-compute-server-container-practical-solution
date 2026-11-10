# Onward Logistics: Handling Sales Day Demand

Lab 1 (Week 3, Compute & Delivery on AWS), by Jado Fils SEZIKEYE.

The Onward order system is a single full-stack Java application. It runs on Free Tier Amazon Linux EC2 servers in private subnets behind one public Application Load Balancer. On sales days the server count grows automatically with CPU load, and it shrinks again afterwards. Everything is defined in **one CloudFormation template** and deployed by **CloudFormation Git sync** from **one deployment file**.

```text
customers ──HTTP──▶ ALB (public subnets, 2 AZs)
                     └─▶ Auto Scaling group: 2–6 × t3.micro (private subnets, 2 AZs, no public IPs)
                           └─ outbound only ─▶ Regional NAT Gateway ─▶ internet (updates, app download)
```

## Repository layout

| Path | What it is |
| --- | --- |
| [`infrastructure/template.yaml`](infrastructure/template.yaml) | The whole environment: VPC, Regional NAT, ALB, launch template, Auto Scaling and budget |
| [`infrastructure/deployment.yaml`](infrastructure/deployment.yaml) | The Git sync deployment file: which template, which parameter values, which tags |
| [`infrastructure/bootstrap/`](infrastructure/bootstrap) | One-time Git sync setup (the GitHub connection and the sync role) |
| [`app/`](app/README.md) | Spring Boot full-stack app: layered MVC backend, HTML/CSS/JS UI using AJAX, tests |
| [`.github/workflows/release-app.yml`](.github/workflows/release-app.yml) | Tests the app and publishes the JAR as a GitHub Release, which the servers download at launch |
| [`docs/`](docs/README.md) | Discovery report, architecture diagram, cost, decisions, live-review checks |
| [`scripts/lab.sh`](scripts/lab.sh) | Run in AWS CloudShell: `deploy`, `verify` (PASS/FAIL for every requirement), `scale`, `fail-server`, `fail-az`, `teardown` |
| [`scripts/watch-availability.sh`](scripts/watch-availability.sh) | Prints which server answered each request, so downtime is visible during failure tests |

## How it fits together

1. **Merge app code to `main`.** GitHub Actions runs the tests and publishes `onward-orders.jar` as the release `v<version>`.
2. **Merge to `infrastructure/`.** Git sync updates the CloudFormation stack `onward-dev-orders`.
3. **Each server boots from the launch template.** Through the NAT gateway:
   - it installs security updates and Java;
   - it downloads the latest release JAR from GitHub;
   - it starts the app as a `systemd` service;
   - it signals CloudFormation once `/health` answers.
4. **CPU rises above 50%.** Target tracking adds servers, up to 6. When the load drops, it removes them again, down to 2.

Start with the [architecture](docs/architecture/README.md), then read [`template.yaml`](infrastructure/template.yaml) top to bottom. It is split into five numbered sections.

## Run the app locally

```powershell
cd app
mvn spring-boot:run    # http://localhost:8080
mvn verify             # tests
```
