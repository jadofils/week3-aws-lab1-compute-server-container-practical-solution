# Onward Logistics: Delivering Order System Updates Safely

Week 3 Lab 1, by Jado Fils SEZIKEYE. A Java Spring Boot full-stack order application, released automatically to Amazon ECS on Fargate with zero-downtime blue/green deployments. All infrastructure is AWS CloudFormation deployed by Git sync.

## How a change reaches customers

```text
merge to app main → GitHub Actions (tests, OIDC, no keys) → ECR :release
  → EventBridge → CodePipeline → CodeDeploy blue/green (10% canary, auto-rollback)
  → Fargate tasks in private subnets behind one public ALB (no NAT, VPC endpoints only)
```

## Repository layout

| Folder | Becomes | Contents |
| --- | --- | --- |
| [`app/`](app/README.md) | application repository `onward-orders-app` | Spring Boot app (layered MVC), tests, Dockerfile, `taskdef.json`, `appspec.yaml`, GitHub Actions workflow |
| [`infrastructure/`](infrastructure/README.md) | infrastructure repository `onward-orders-infrastructure` | numbered CloudFormation stacks, GitSync deployment files, one-time bootstrap |
| [`docs/`](docs/README.md) | submission documents | discovery report, architecture diagram, decisions, cost, live-review checks |
| [`scripts/load/`](scripts/load) | review tooling | k6 and curl load tests, availability watcher |

The brief requires **separate application and infrastructure repositories**. Each of `app/` and `infrastructure/` is self-contained and written to be the root of its own repository: the workflow path, GitSync file paths and OIDC trust all assume that. This combined repository is the working copy they are published from.

## Start here

1. [Architecture](docs/architecture/README.md): what runs where and how a release flows.
2. [Infrastructure README](infrastructure/README.md): the stack reading order and the one-time setup.
3. [App README](app/README.md): local run, tests, and the delivery files.
4. [Live review checks](docs/demo/README.md): how to prove each client requirement.

## Run the app locally

```powershell
cd app
mvn spring-boot:run   # http://localhost:8080
mvn verify            # tests
```
