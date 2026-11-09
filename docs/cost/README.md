# Cost estimate: `dev` environment in eu-west-1

**Assumptions.** These are monthly figures for about 730 hours, using approximate on-demand list prices for eu-west-1 at the time of writing. Re-check every line in the [AWS Pricing Calculator](https://calculator.aws/) before relying on it. Taxes and free tier are excluded.

## Quiet month (always 2 tasks)

| Line item | Calculation | ≈ USD / month |
| --- | --- | --- |
| Fargate tasks (0.5 vCPU, 1 GB, x86) | 2 tasks × (0.5 × $0.04048 + 1 × $0.004445) × 730 h | 36.04 |
| Application Load Balancer, hourly | $0.0252 × 730 h | 18.40 |
| Application Load Balancer, LCUs | about 1 LCU × $0.008 × 730 h | 5.84 |
| Public IPv4 for the load balancer | 2 addresses × $0.005 × 730 h | 7.30 |
| **Interface endpoints** (ECR API, ECR DKR, Logs) | 3 endpoints × 2 AZs × $0.011 × 730 h | **48.18** |
| Interface endpoint data processing | about 10 GB × $0.01 | 0.10 |
| S3 gateway endpoint | no charge | 0.00 |
| CloudWatch Logs and alarms | about 2 GB ingested × $0.57 + 3 alarms × $0.10 | 1.44 |
| ECR storage | about 15 images × 0.15 GB × $0.10 | 0.23 |
| CodePipeline V2 | about 20 releases × 20 action-minutes × $0.002 | 0.80 |
| CodeDeploy (ECS), EventBridge (AWS events), Budgets (first 2) | no charge | 0.00 |
| **Total** | | **≈ 118** |

**Peak days.** Demand can reach three times normal, so up to 6 tasks run. Each extra task costs about $0.025 per hour. Ten peak hours a month at 6 tasks adds roughly $1. The 6-task maximum caps the worst case at about $108 per month for Fargate.

**Budget.** The `8-budget` stack sets $200 per month, which leaves headroom over the estimate. It emails at 80% of actual spend and at 100% of forecast spend.

## What the private connectivity costs (Finance Lead)

| Option | Monthly cost | Internet path from the app? | Highly available? |
| --- | --- | --- | --- |
| **Chosen: 3 interface endpoints + S3 gateway** | about $48 | **No** | Yes (one endpoint network interface per AZ) |
| One NAT gateway | about $35 + $0.048/GB + $3.65 IPv4 ≈ $39 | Yes | No (lose that AZ and outbound access fails) |
| Two NAT gateways (one per AZ) | about $78 + data | Yes | Yes |

The endpoints cost about 40% of the monthly bill. They are cheaper than a highly available NAT setup, and they are the only option that meets the security rule that the application has no path to the internet. A single NAT gateway would be about $9 cheaper, but it breaks that rule and is a single point of failure.

## Keeping idle environments cheap

| Lever | Saves | How |
| --- | --- | --- |
| Off-hours scale-down | about 76% of Fargate (128 of 168 hours a week), ≈ $27 per month | Set `EnableOffHoursScaleDown: 'true'` in `deployments/dev-6-ecs-service.yaml` and merge. Tasks go to 0 from 19:00 to 07:00 UTC on weekdays and over weekends. |
| Delete the environment when not in use | nearly all of it (load balancer, endpoints, IPv4) | Delete the GitSync stacks in reverse order (8 → 1). Recreate them later from Git with `bootstrap.sh`. ECR empties itself on delete; empty the pipeline bucket first. |
| Image lifecycle | ECR storage | Keeps only the last 15 images. |
| Log retention | CloudWatch storage | 14 days. |

## Alternatives rejected on cost

- **EC2 Auto Scaling group instead of Fargate.** It is cheaper per vCPU but adds patching work and does not meet the brief.
- **NAT gateway.** See the table above.
- **Always running for peak (6 tasks).** About $108 per month for Fargate, versus about $36 with scaling.
