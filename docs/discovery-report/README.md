# Discovery Report: Delivering Order System Updates Safely

| Lab | Week 3 Lab 1 - Delivering Order System Updates Safely for Onward Logistics |
| --- | --- |
| Name | Jado Fils SEZIKEYE |
| Date | 9 November 2026 |
| Client | Onward Logistics (see the [Client Profile](../client-profile/README.md)) |
| Application stack | Java 21, Spring Boot, single full-stack container |
| AWS Region | `eu-west-1` (Ireland); see [decision D1](../decisions/README.md#d1-region-eu-west-1) |

## 1. What you found

### The client's need

Onward Logistics needs every approved improvement to its order system to reach merchants and customers automatically and safely, without outages, hand-run releases or exposed credentials, while the service absorbs peak days and costs little when quiet.

### Solutions that exist

| Solution | How it works | Trade off for this client |
| --- | --- | --- |
| Manual releases (today) | An engineer builds an image on a laptop, uploads it and updates the running service by hand. | **Strength:** no tooling to learn. **Weakness:** slow, error-prone, needs long-lived credentials on laptops, and nobody dares release on peak days. |
| Rolling update from a CI/CD pipeline | A pipeline builds the image and ECS replaces tasks a few at a time with the new version. | **Strength:** automated and simple. **Weakness:** old and new versions serve customers together, and rollback means a new deployment, which is slow while a bad version is live. |
| Blue/green (canary) from a CI/CD pipeline | A pipeline starts the new version beside the old one, tests it, then shifts traffic gradually; the old version stays ready for instant switch-back. | **Strength:** no downtime, bad releases are caught early and reversed automatically in seconds. **Weakness:** briefly runs two sets of tasks and has more moving parts to understand. |

### What happens with no solution

- **Money:** a release that goes wrong on a sales day stops order intake while engineers rebuild by hand. Delivery margins are thin, so lost orders and penalty refunds to merchants matter quickly.
- **Customer trust:** merchants see failed orders and riders get no jobs. Trust is the company's second priority.
- **Security and legal exposure:** hand releases need long-lived cloud keys on laptops and in scripts. The client's own audit already found too many people with too much access. A leaked key could expose customer data that falls under data-protection law in every market.
- **Operations:** releases stay monthly and risky instead of weekly, which blocks the "ship improvements faster" priority.

Documented example: in 2012 Knight Capital deployed new code by hand and missed one of eight servers. The inconsistent release ran for 45 minutes, caused about US$460 million in losses, and the company never recovered as an independent firm (SEC order, 2013). An automated, identical, reversible release process is exactly the control that was missing.

### Sources

- [SEC press release 2013-222: Knight Capital and the 2012 deployment failure](https://www.sec.gov/news/press-release/2013-222)
- [Amazon ECS blue/green deployments with CodeDeploy](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-type-bluegreen.html)
- [CodePipeline ECS blue/green deploy action](https://docs.aws.amazon.com/codepipeline/latest/userguide/action-reference-ECSbluegreen.html)
- [Starting a pipeline from an ECR image push (EventBridge)](https://docs.aws.amazon.com/codepipeline/latest/userguide/create-cwe-ecr-source.html)
- [ECR interface VPC endpoints and the S3 gateway endpoint for image layers](https://docs.aws.amazon.com/AmazonECR/latest/userguide/vpc-endpoints.html)
- [GitHub Actions OpenID Connect in AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
- [CloudFormation Git sync](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/git-sync.html)
- [AWS Pricing Calculator](https://calculator.aws/)

## 2. The business value of your solution

### Your chosen approach

A **blue/green canary release pipeline onto ECS Fargate**. Merging to `main` is the only manual step:

1. GitHub Actions tests and builds the container.
2. It signs in to AWS with a short-lived OIDC token and publishes the image.
3. AWS deploys the new version beside the current one and sends 10% of customers to it for five minutes.
4. If errors or failed health checks appear, AWS switches everyone back automatically. Otherwise it moves all traffic over and keeps the old version on standby for ten minutes.

The application runs in private subnets with no internet route; it reaches AWS services only through VPC endpoints, and customers reach it only through one load balancer address. This suits Onward better than a rolling update because a bad release reaches few customers and is reversed in seconds, which matters most on peak days. All infrastructure is CloudFormation deployed by GitSync, so the setup can be rebuilt from Git and read by a new engineer.

### Services you will use

| Service | The job it does | Why this service, used this way |
| --- | --- | --- |
| Amazon ECS on AWS Fargate | Runs the order container, at least two tasks across two Availability Zones. | No servers to patch for a small platform team; it replaces failed tasks automatically, and you pay per task-second. |
| Application Load Balancer | The single public address; production and test listeners; blue and green target groups. | Only health-checked tasks receive traffic, and CodeDeploy can switch traffic between versions. |
| Amazon VPC + VPC endpoints (ECR API, ECR DKR, CloudWatch Logs, S3 gateway) | Private subnets with no internet route; tasks reach AWS services on AWS's network. | Meets the Head of Security's rule. It is cheaper than a highly available NAT gateway and removes the path to the internet ([cost](../cost/README.md)). |
| Amazon ECR | Stores versioned images, scanned on push; old images expire. | Private registry reachable through endpoints; the push event starts releases. |
| GitHub Actions + IAM OIDC | Tests, builds and pushes the image; uploads the deployment bundle. | No long-lived AWS keys: the role trusts only the app repository's `main` branch. |
| Amazon EventBridge | Detects the `release` image push and starts the pipeline. | Event-driven, so there is no polling and no extra credentials. |
| AWS CodePipeline (V2) | Combines the image and the deployment bundle and hands them to CodeDeploy. | A visible, auditable release history. |
| AWS CodeDeploy (ECS blue/green) | Canary traffic shift, alarm-based automatic rollback, old version kept for instant switch-back. | Zero-downtime releases and fast reversal (CTO). |
| Application Auto Scaling | Target tracking on 50% CPU, between 2 and 6 tasks; optional night and weekend scale to 0. | Grows on busy days and shrinks after without anyone acting (Head of Operations). |
| Amazon CloudWatch | Container logs, alarms on server errors and unhealthy targets. | Evidence for audits, and the signals that trigger rollback. |
| AWS CloudFormation + Git sync | All of the above as code in Git, deployed on merge. | Nothing is clicked in the console, and the setup can be rebuilt from the repositories. |
| AWS Budgets | Monthly budget with 80% actual and 100% forecast email alerts. | Spend is visible from day one (client standard). |

### Who benefits, and how you will know

| Stakeholder | Benefit | How you would know |
| --- | --- | --- |
| Chief Technology Officer | Merging is the only release step; there is no downtime, and rollback is automatic. | A merge leads to a new version live in about 15 minutes with no manual action. A test with a bad release shows CodeDeploy rolling back by itself. Zero 5xx errors are seen at the load balancer during releases. |
| Head of Security | No stored keys; one public entry point; the application has no internet path. | No AWS access keys exist for the pipeline. The task subnets' route table has no `0.0.0.0/0` route. Tasks have no public IP, and the task security group admits only the load balancer. |
| Head of Operations | Survives a task or zone failure and scales automatically. | A stopped task is replaced within about a minute. Under load the desired count rises from 2 towards 6, then falls back. |
| Finance Lead | Known, bounded cost; idle time can cost almost nothing; the price of private connectivity is explained. | The [cost estimate](../cost/README.md) shows each line item. Budget alerts arrive at 80%. Off-hours scale-down or stack deletion is shown working. |
| Engineers (new joiners) | One readable path through numbered stacks and documentation. | A new engineer can say how a change reaches production after reading the [architecture](../architecture/README.md) page. |

### Limits and risks of your design

- **Orders are held in memory** for this demonstration. They disappear when a task is replaced and are not shared between tasks. Real persistence needs a database, which would be a separate decision and cost.
- **HTTP only** in the lab. Production needs an ACM certificate and an HTTPS listener on a real domain.
- **One Region.** A whole-Region outage is not covered.
- **The pipeline can run two versions at once** during the canary. Future breaking API or database changes must stay backward compatible for that window.
- **Interface endpoints charge per hour**, even when idle. Deleting idle environments, not only scaling tasks to zero, is what saves that money.
- **One manual approval is unavoidable:** the GitHub connection handshake when GitSync is first set up.
