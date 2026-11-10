# Cost: quiet day vs peak day (eu-west-1)

**[AWS Pricing Calculator estimate](https://calculator.aws/#/estimate?id=e196fec3f9bea1404644717d8585e2448b9f1083):** about **$131.47** for a quiet month and **$141.84** for a month with sales days. The per-service breakdown is in the [discovery report](../discovery-report/README.md#what-it-costs).

The per-day working below uses approximate on-demand list prices for `eu-west-1`. Re-check each line in the [AWS Pricing Calculator](https://calculator.aws/) before relying on them. Taxes are excluded. **Free Tier:** depending on the account's plan, `t3.micro` hours may be free or covered by credits. The figures below assume **no** Free Tier, so they are the worst case.

## Per-day estimate

| Line item | Price assumption | Quiet day (2 servers all day) | Peak day (2 servers for 14 h, 6 servers for 10 h) |
| --- | --- | --- | --- |
| EC2 `t3.micro` | $0.0114 per instance-hour | 48 h → **$0.55** | 28 h + 60 h = 88 h → **$1.00** |
| EBS gp3 8 GB per server | $0.088 per GB-month | **$0.05** | **$0.07** |
| EC2 detailed monitoring | 7 metrics × $0.30 per month, per server | **$0.14** | **$0.21** |
| Application Load Balancer, hourly | $0.0252 per hour | **$0.60** | **$0.60** |
| ALB capacity units (LCU) | $0.008 per LCU-hour (1 LCU quiet, about 3 LCU for 10 peak hours) | **$0.19** | **$0.35** |
| Regional NAT Gateway, hourly | $0.048 per hour **per active AZ** (2 AZs) | **$2.30** | **$2.30** |
| NAT data processing | $0.048 per GB (updates and downloads, about 1 GB quiet, 2 GB peak) | **$0.05** | **$0.10** |
| Public IPv4 addresses | $0.005 per hour each: 2 for the ALB, 2 for the NAT | **$0.48** | **$0.48** |
| **Total per day** | | **≈ $4.36** | **≈ $5.11** |

**Monthly (30 quiet days):** about **$131**. Each peak day adds about **$0.75**.

### What this tells the Finance Lead

- **A sales day costs only about 17% more than a quiet day**, because extra servers run only while they are needed.
- **The fixed costs dominate:** the NAT gateway, the load balancer and the public IPs come to about $3.60 of the $4.36 quiet day. They buy the security model (private servers, one entry point) and resilience to losing a zone.
- **Running six servers all month instead** would add 4 × $0.0114 × 24 h ≈ $1.09 per day, or about $33 per month, for capacity that sits idle on 29 days out of 30.

## Alternatives considered on cost and resilience

| Option | Cost vs chosen | Why rejected |
| --- | --- | --- |
| Fixed fleet of 6 servers | About +$33 per month | Pays for peak capacity all month, and still breaks if a sale exceeds the forecast. |
| One zonal NAT gateway in one AZ | About −$1.30 per day | If that AZ fails, servers in the other AZ lose updates, and the private route depends on a failed zone. Fails the AZ-loss requirement. |
| Two zonal NAT gateways (one per AZ) | About the same | Same resilience as Regional NAT, but more resources, routes and tables to manage. Regional NAT is one resource and is required by the brief. |
| NAT instance on EC2 | Cheaper | It is another server to patch, scale and make highly available, which is what the client wants to avoid. |
| Public IPs on servers, no NAT | Cheapest | Servers reachable from the internet: violates the Head of Security's rule. |
| Larger instances, no Auto Scaling | Higher, and not Free Tier | One big server is a single point of failure, and it still has to be sized for the peak. |

## Keeping cost under control

- The **budget alert** (`BudgetAlertEmail` in `deployment.yaml`) emails at 80% of $200 per month actual spend and at 100% forecast.
- **`MaxSize: 6`** caps the worst-case server spend.
- **Tear down after the review:** delete the stack (or remove its Git sync configuration). The whole environment can be rebuilt from the repository.
