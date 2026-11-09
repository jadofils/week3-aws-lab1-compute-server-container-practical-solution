# Lab 1 Discovery Report

**Handling sales-day demand for the Onward Logistics order system**

| Detail | Value |
| --- | --- |
| Name | Jado Fils SEZIKEYE |
| Client | Onward Logistics |
| Application language | Java (Spring Boot, full-stack web application) |
| Date | 9 November 2026 |

## What we found

The current order application runs as one full-stack container. Sales-day demand can reach three times normal demand. The client needs the service to stay available through a container or Availability Zone failure, scale with demand, keep the application private, and make a code merge the only manual release step. The rollback path should be quick and understandable to a new engineer.

The infrastructure and deployment must be repeatable as code. CloudFormation GitSync deploys infrastructure changes. GitHub Actions builds and publishes the application image to Amazon ECR using GitHub OIDC. An ECR image-push event starts CodePipeline, which deploys to Amazon ECS on AWS Fargate with AWS CodeDeploy blue/green traffic shifting. The app and infrastructure ultimately belong in separate GitHub repositories.

## Options considered

| Option | Benefits | Trade-offs |
| --- | --- | --- |
| Fixed container capacity sized for peak | Simple capacity behavior. | Pays for peak capacity during quiet periods and can still be undersized if demand exceeds the forecast. |
| ECS Fargate with target tracking and blue/green deployment | Managed container compute, automatic capacity adjustment, and controlled traffic shifting with rollback support. | Requires clear task scaling targets, deployment alarms, and capacity/cost assumptions. |
| EC2 Auto Scaling | Familiar server-based scaling and control over hosts. | Adds host patching and administration and does not match the lab's required Fargate design. |

## Recommended design and business value

Use an internet-facing Application Load Balancer across two or more Availability Zones. It is the only public application entry point. It forwards to an ECS Fargate service whose tasks run in private subnets with public IP assignment disabled. Configure ECS target tracking to scale task count, with a minimum of two tasks across Availability Zones and bounded maximum capacity. Use an ECS deployment controller of `CODE_DEPLOY`, two target groups, production and test listeners, and CodeDeploy blue/green deployments with health checks and automatic rollback alarms.

There is no NAT gateway and no general internet route from task subnets. Private connectivity uses VPC endpoints: ECR API and Docker Registry interface endpoints, an S3 gateway endpoint for image layers, and CloudWatch Logs interface endpoint for task logs. Add Secrets Manager or Systems Manager endpoints only if the chosen application configuration actually uses those services. ECR image publishing occurs from GitHub Actions using OIDC; it does not require task subnets to reach GitHub.

CloudFormation defines VPC, subnets, endpoints, security groups, ALB, ECR, ECS, IAM roles, scaling, CodeDeploy, CodePipeline, EventBridge, and monitoring. CloudFormation GitSync tracks the infrastructure repository. Do not create infrastructure manually in the console. IAM access is granted through roles and attached managed policies; no inline policies are used. Apply `Project=onward`, `Owner=Jado Fils SEZIKEYE`, and `Environment=<environment>` tags where supported, and use names following `onward-<environment>-<purpose>`.

| Need | Evidence for review |
| --- | --- |
| Single public entry and private app | ALB public DNS works; task ENIs have no public IP and task security group accepts app traffic only from the ALB security group. |
| Resilience and automatic scaling | Tasks are placed across AZs; ECS service desired count changes under load; stopped task is replaced; ALB health checks remove unhealthy targets. |
| Safe releases and fast rollback | CodeDeploy shifts traffic between target groups; deployment alarms or failed health checks trigger rollback. |
| AWS-only runtime connectivity | Task subnet route tables have no NAT/default internet route; ECR and logs access uses VPC endpoints. |
| Repeatable infrastructure | GitSync stack deployment records and the version-controlled CloudFormation source. |
| Low idle spend | ECS scales to its configured minimum; compare quiet and peak task-hours, ALB, endpoints, logs, ECR storage, and data transfer. |

## Resource inventory for AWS Pricing Calculator

Complete the calculator only after choosing Region, run duration, task sizing, and load assumptions. Model at least a quiet case and a three-times-demand peak case.

| Resource | Inputs to decide and price |
| --- | --- |
| ECS Fargate tasks | vCPU and memory per task, minimum/desired/maximum task count, Linux architecture, task-hours in quiet and peak periods. |
| Application Load Balancer | Hours running, processed bytes or LCUs, number of listeners and target groups. |
| VPC endpoints | ECR API endpoint-hours, ECR DKR endpoint-hours, CloudWatch Logs endpoint-hours, endpoint data processing; S3 gateway endpoint has no hourly charge but review regional transfer charges. |
| Amazon ECR | Stored image GB-months, image push/pull volume, lifecycle retention. |
| CloudWatch | Log ingestion and retention, metrics, alarms, dashboards if used. |
| CodePipeline / CodeDeploy / EventBridge | Pipeline executions, deployment frequency, pipeline type and applicable service pricing for the selected Region. |
| VPC / data transfer | Public IPv4 addresses used by the ALB, inter-AZ data transfer, internet data transfer delivered by the ALB. |
| CloudFormation GitSync and GitHub Actions | Confirm service charges, runner type/minutes, and any GitHub plan limits separately from AWS resource estimate. |

Do not price a NAT gateway: the design requirement is VPC endpoints instead. Do not add a database to this lab demo without a confirmed persistence requirement. The current app stores sample orders in process memory, so data is temporary and not shared across tasks; production persistence needs a separate client decision and estimate.

## Repository and deliverables status

The current Git repository combines app and infrastructure work for learning. The client deliverable calls for separate app and infrastructure repositories; create/publish those repositories and move the corresponding folders as a follow-up before claiming that requirement complete. Link the completed report, both repositories, the architecture diagram, and the demonstration recording or review in the final hand-in.

## References

- Lab 1 problem description and client profile supplied with the course.
- [ECR interface VPC endpoints and S3 image-layer endpoint](https://docs.aws.amazon.com/AmazonECR/latest/userguide/vpc-endpoints.html)
- [ECS private VPC connectivity](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/networking-connecting-vpc.html)
- [ECS blue/green deployment with CodeDeploy](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-type-bluegreen.html)
- [CodePipeline ECS blue/green action](https://docs.aws.amazon.com/codepipeline/latest/userguide/action-reference-ECSbluegreen.html)
- [ECR image push event as a CodePipeline source](https://docs.aws.amazon.com/codepipeline/latest/userguide/create-cwe-ecr-source.html)
- [GitHub Actions OIDC in AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
- [CloudFormation GitSync concepts](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/git-sync-concepts-terms.html)
