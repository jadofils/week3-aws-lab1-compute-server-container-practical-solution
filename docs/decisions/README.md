# Design decisions

Each decision gives the choice, the business reason, and the alternatives rejected. Owner: Jado Fils SEZIKEYE.

## D1. Region: eu-west-1
**Decision.** Deploy to `eu-west-1` (Ireland).
**Reason.**
- Every service used is available, including Regional NAT Gateway and CloudFormation Git sync.
- Prices are lower than in `af-south-1`.
- From West Africa, latency to western Europe is usually as low as or lower than to Cape Town, because the main submarine cables land in Europe.

**Rejected.**
- `af-south-1`: an opt-in Region with higher prices.
- `us-east-1`: further from customers.

## D2. One template and one deployment file
**Decision.** The whole environment is `infrastructure/template.yaml`, deployed by Git sync from `infrastructure/deployment.yaml`.
**Reason.** The brief asks to "deploy the template" with Git sync. Git sync maps one deployment file to one template. A single template has no cross-stack exports and no S3 upload step, and it can be read in one pass: it is split into five numbered sections.
**Rejected.** Nested stacks, because child templates must be uploaded to S3, which is a manual or extra step.

## D3. EC2 Auto Scaling with CPU target tracking at 50%, 2 to 6 servers
**Reason.**
- The CTO wants capacity to follow CPU automatically.
- Two servers is the minimum that survives losing a server or a zone.
- Six is about three times normal demand and caps spend.
- Target tracking both adds and removes servers, so no one tunes thresholds by hand.

**Rejected.**
- Step scaling: more alarms to tune.
- Scheduled scaling alone: misses unexpected surges.

## D4. Free Tier `t3.micro`, Amazon Linux 2023
**Reason.** It is required by the brief, and `t3.micro` (2 vCPU, 1 GB) runs the Java app with a 512 MB heap. AL2023 is AWS-supported and comes with `dnf` and the SSM agent. The AMI is resolved from the public SSM parameter, so new servers always get the latest image.

## D5. Regional NAT Gateway for outbound access
**Reason.** It is required by the brief. AWS runs it in every zone that has private workloads, so outbound access survives losing a zone, and it is one resource, one route and one route table. NAT allows only connections that start from inside, so servers stay unreachable from the internet.
**Rejected.**
- A single zonal NAT: not zone-resilient.
- Two zonal NATs: more to manage.
- A NAT instance: another server to patch.

## D6. No admin ports; Session Manager instead
**Reason.** It meets "nobody should be opening admin ports". Servers use the AWS managed `AmazonSSMManagedInstanceCore` policy and reach Systems Manager through the NAT gateway. There are no SSH keys to lose, and sessions are tied to IAM identities.
**Rejected.** A bastion host with SSH: a public server and keys to manage.

## D7. The application is installed at boot from the latest GitHub Release
**Reason.**
- "Installed and started automatically when a server launches" means the user data must fetch a tested build.
- GitHub Actions tests each change and publishes the JAR as a GitHub Release.
- Every new server downloads the **latest** release over HTTPS through the NAT gateway. No AWS credentials are needed, because the repository is public.
- There is no version pinning or rollback mechanism, which keeps the template small.

**Trade-off, stated honestly.** A server can only start while GitHub is reachable. `curl` retries for about a minute. If GitHub is down when Auto Scaling launches a server, that server fails its health check and is retried. The servers already running keep serving.

**Rejected.**
- A private S3 copy made at deploy time: more resources (bucket, Lambda, endpoint) than this lab needs.
- An upload pipeline with AWS credentials in GitHub: more secrets to manage.
- Building from source on each server: slow boots on `t3.micro`.
- A custom AMI: an extra image pipeline.

## D8. Load balancer health checks on `/health`, with ELB health type on the Auto Scaling group
**Reason.**
- A server whose app has stopped, even though EC2 reports it healthy, is removed from rotation after 3 failed checks (about 45 seconds) and then replaced.
- Three misses, not two, so a server that is only busy during a CPU spike isn't mistaken for a failed one.
- A 300-second grace period stops new servers being killed while they boot.

## D8b. No single point of failure
| If this fails | What keeps the service running |
| --- | --- |
| One server, or its app | Health checks pull it from the ALB, and Auto Scaling replaces it |
| One Availability Zone | ALB nodes, the Regional NAT and servers also run in the other AZ; replacements launch there |
| A server fails to boot | `cfn-signal` holds the stack until the app answers; at least 2 servers stay in service |
| Sudden demand | Target tracking adds servers up to 6, while the existing servers keep answering |

**Not covered (stated honestly):**
- Losing the whole Region.
- In-memory orders on a failed server, which are lost. Durable orders need a database, which is a separate decision.
- New servers need GitHub reachable to download the app (see D7). Running servers are not affected.

## D9. CreationPolicy and rolling UpdatePolicy with `cfn-signal`
**Reason.** The stack counts as created only when the app actually answers on the minimum number of servers. When the launch template changes (for example a new AMI or instance type), servers are replaced one at a time. At least two stay in service, and CloudFormation waits for each new server to signal healthy before continuing.

## D10. A spike trigger in the UI, bounded
**Reason.** The brief asks for a secure way to trigger a load spike through a UI or secure remote access.
- The UI trigger only burns CPU, for 30–600 seconds, one spike per server, and it can be disabled.
- Session Manager with `stress-ng` is the remote-access alternative.

## D11. Detailed (1-minute) monitoring
**Reason.** Scaling reacts within minutes instead of after 5-minute periods, which matters when sales-day orders triple within hours. It costs about $2.10 per server per month.

## D12. No inline policies; least privilege
**Reason.** This is a client standard and audit-friendly. The server role has one AWS managed policy. The Git sync role's IAM rights are limited to `onward-dev-orders-*` names.

## D13. HTTP for the lab
**Reason.** There is no domain or certificate for the lab. Production should add HTTPS with ACM and redirect HTTP to HTTPS.

## D13b. Load balancer access logs are off
**Reason.** ALB access logs must be written to an S3 bucket, and the lab design deliberately has no S3 bucket. Health, request counts and errors are still visible as CloudWatch load balancer metrics. Production should enable access logs to an encrypted S3 bucket with a retention policy, for audits.

## D13c. Servers can never get a public IP
**Reason.** The launch template sets `AssociatePublicIpAddress: false` on the server's network interface. It does not rely only on the private subnets' `MapPublicIpOnLaunch: false`, so a later subnet change cannot expose a server.

## D14. Orders in memory
**Reason.** The brief is about compute and availability, not data. Shared, durable orders would need a database, which is a separate client decision with its own cost.
