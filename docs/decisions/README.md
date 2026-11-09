# Design decisions

Each decision gives the choice, the business reason and the alternatives rejected. Owner: Jado Fils SEZIKEYE.

## D1. Region: eu-west-1
**Decision.** Deploy to `eu-west-1` (Ireland).
**Reason.**
- Every service in the design is available there, including CloudFormation Git sync and CodeDeploy for ECS.
- Prices are lower than in `af-south-1`.
- From West Africa, latency to western Europe is usually as low as or lower than to Cape Town, because the main submarine cables run to Europe.

**Rejected.**
- `af-south-1`: an opt-in Region with higher prices.
- `us-east-1`: further from customers.

**Revisit** if data-residency law in a market requires in-country or in-continent hosting.

## D2. ECS on Fargate with at least 2 tasks across 2 Availability Zones
**Reason.** There are no servers to patch for a small platform team. A failed task is replaced automatically, and if one zone is lost the other zone keeps serving.
**Rejected.**
- EC2 Auto Scaling: more operational work, and the brief requires Fargate.
- A single task: no protection against a failure.

## D3. VPC endpoints instead of a NAT gateway
**Reason.** The Head of Security requires that the application has no path to the internet. The endpoints used are ECR API, ECR DKR, CloudWatch Logs and an S3 gateway endpoint for image layers. They are cheaper than highly available NAT ([cost](../cost/README.md)).
**Rejected.** A NAT gateway: it provides an internet path and costs more when made highly available.

## D4. The application security group allows HTTPS egress to `0.0.0.0/0`
**Reason.**
- The private route table has no internet route, so the only reachable targets are the VPC endpoints.
- Fargate pulls image layers from S3 through the gateway endpoint. Its Region-specific prefix-list ID cannot be referenced portably in CloudFormation.
- The S3 endpoint policy limits access to the ECR layer bucket.

**Rejected.** Hard-coding the S3 prefix-list ID, which ties the templates to one Region.

## D5. One GitSync stack per template, linked by exports
**Reason.** GitSync deploys a template file straight from Git. Nested stacks would need child templates uploaded to S3 by hand or by an extra pipeline, which breaks "nothing manual" and "rebuild from the repository". Numbered stacks also give a new engineer a clear reading order.
**Rejected.**
- Nested stacks loaded from S3.
- One very large template, which is harder to read.

## D6. Deployment bundle through S3, triggered by the ECR push
**Reason.**
- GitHub Actions uploads `taskdef.json` and `appspec.yaml` to S3, then pushes the image.
- The ECR push of the `release` tag is the EventBridge event that starts CodePipeline, which is the brief's required trigger.
- The pipeline needs no GitHub connection of its own, and the order of uploads guarantees the descriptors match the image.

**Rejected.** A CodeConnections GitHub source in the pipeline: a second trigger path and another connection to manage.

## D7. Moving `release` tag on a MUTABLE repository
**Reason.** The ECR source action follows one tag. Every image is also pushed with its immutable commit SHA, so any release can be traced to a commit.
**Rejected.** IMMUTABLE tags, which cannot express "the current release".

## D8. Canary 10% for 5 minutes, automatic rollback on alarms, old version kept 10 minutes
**Reason.**
- A bad release reaches only a few customers before the alarms catch it: target 5xx errors, or unhealthy blue or green targets.
- Rollback is automatic.
- After success, the old task set stays for 10 minutes so an engineer can still roll back instantly.

**Rejected.**
- All-at-once: everyone is exposed.
- Rolling ECS update: slower to reverse.

## D9. Scaling on CPU target tracking at 50%, between 2 and 6 tasks
**Reason.** It handles about three times normal demand with no one adjusting anything. The maximum caps spend, and the minimum keeps one task per zone.
**Rejected.** Request-count scaling: that metric is tied to one target group, and the active group swaps on every blue/green release.

## D10. Test listener closed by default
**Reason.** Customers have one public entry point (port 80). Engineers can open port 9000 to their own IP through `TestTrafficCidr` while previewing a green release.

## D11. GitHub OIDC role limited to one repository and branch
**Reason.** No long-lived keys exist anywhere. The role trusts only `repo:jadofils/onward-orders-app:ref:refs/heads/main`. It can only push to one ECR repository and write to `deploy/*` in the pipeline bucket.

## D12. No inline IAM policies
**Reason.** This is a client standard and audit-friendly. Every permission is an AWS managed policy or a named, versionable `AWS::IAM::ManagedPolicy`.

## D13. HTTP for the lab, HTTPS for production
**Reason.** There is no domain or certificate for the lab. Production should add an ACM certificate, an HTTPS listener, and a redirect from HTTP to HTTPS.

## D14. In-memory orders
**Reason.** The brief asks for a demo full-stack app and no database. Persistence is a separate client decision with its own cost; see the discovery report's limits section.
