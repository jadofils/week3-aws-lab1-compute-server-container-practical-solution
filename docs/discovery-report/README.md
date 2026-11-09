# Lab 1 Discovery Report

Handling Sales Day Demand for the Onward Logistics Order System

| Report detail | Value |
| --- | --- |
| Name | Jado Fils SEZIKEYE |
| Date | 9 November 2026 |
| Client | Onward Logistics |
| Assigned application language | Java |

This report frames the sales-day reliability and cost problem before implementation. The proposed direction is a private, multi-AZ EC2 Auto Scaling design behind one public Application Load Balancer. We will complete the quiet-day and peak-day cost comparison in the AWS Pricing Calculator after the resource inventory and usage assumptions are agreed.

# 1. What we found

## The client's need

Onward Logistics needs its order system to remain responsive when sales-day demand triples, recover automatically from server or Availability Zone failure, keep application servers private, and avoid paying for peak capacity during quiet periods.

## Solutions that exist

| Solution | How it works | Trade-off for Onward Logistics |
| --- | --- | --- |
| Fixed EC2 capacity sized for peak | Keep a fixed fleet large enough for the busiest expected day. | Simple to understand, but idle capacity costs money on ordinary days; sizing for normal demand risks slow pages and errors at peaks. |
| EC2 Auto Scaling behind an Application Load Balancer | Spread requests across healthy EC2 targets and add or remove instances as a CloudWatch utilization metric changes. | Fits the required EC2 design and can balance resilience with cost. Requires careful minimum, maximum, health-check, and scaling settings. |
| Managed serverless or container platform | Run the application on a managed compute service that adjusts capacity without managing individual EC2 servers. | Can reduce server administration, but changes the assigned EC2 learning objective and would require reworking the application and deployment design. |

## What happens if the problem stays unsolved

The lab scenario records that last year’s peak caused slow pages and errors, costing merchants and customer trust. If the system remains unchanged, another surge could delay or fail order submission when demand is highest. Staff may have to intervene during the event, while keeping a permanently oversized fleet would spend money during quiet periods. The client profile also identifies customer trust, thin delivery margins, and audit readiness as business priorities.

# 2. Business value of the proposed solution

## Chosen approach

Use an internet-facing Application Load Balancer across at least two Availability Zones, with an Auto Scaling group of Amazon Linux EC2 instances in private subnets. Instances launch from a CloudFormation launch template that installs and starts the single full-stack app automatically. A target-tracking policy based on average CPU utilization is the initial scaling candidate; the final target and group limits will be set after the workload and demonstration plan are defined. Load-balancer health checks and Auto Scaling health replacement will route around failed instances and restore capacity. A Regional NAT Gateway will provide outbound internet access as required by the lab. Systems Manager Session Manager is the candidate for secure administration without opening inbound admin ports.

CloudFormation will define the infrastructure and be deployed from a version-controlled repository through CloudFormation GitSync. Resource names will follow onward-dev-purpose (or the selected environment), and supported resources will carry Project=onward, Owner=Jado Fils SEZIKEYE, and Environment tags. IAM will use roles with attached managed policies; the template will contain no inline policies.

## AWS services and their jobs

| Service | Job in the design | Why it fits |
| --- | --- | --- |
| Amazon VPC, subnets, route tables, Internet Gateway | Separate public load-balancer subnets from private application subnets across AZs. | Enforces the public-entry/private-server boundary. |
| Regional NAT Gateway | Allow private EC2 instances to reach the internet for operating-system or application updates. | Meets the mandatory outbound-access design while keeping instances without public IPs. |
| Application Load Balancer | Provide one public application entry and send requests to healthy EC2 targets. | Spreads traffic and supports target health checks and server-identification demonstrations. |
| Amazon EC2 and EC2 Auto Scaling | Run the full-stack app and change instance count with demand; replace unhealthy capacity. | Required compute model; supports automatic scale-out and scale-in. |
| Amazon CloudWatch | Provide EC2 metrics, scaling signals, and application/instance logs. | Makes scaling and operational behavior observable. |
| AWS Systems Manager Session Manager | Provide controlled remote administration without public SSH/RDP access. | Supports the security requirement to avoid opening admin ports. |
| AWS Identity and Access Management | Give instances and deployment services only required permissions through roles. | Supports least privilege and the no-inline-policy rule. |
| AWS CloudFormation with GitSync | Define and deploy the infrastructure from version control. | Makes the environment repeatable and reviewable as required. |

## Who benefits and how we will know

| Stakeholder | Benefit | Observable evidence |
| --- | --- | --- |
| Chief Technology Officer | Capacity responds to a traffic surge without manual resizing, then returns toward the normal level. | Auto Scaling group desired capacity rises during load and falls after load; application remains reachable. |
| Head of Security | Customers enter through the load balancer; application instances have no direct public reachability or open admin ports. | Subnets, public-IP settings, security-group rules, and a direct-instance reachability check. |
| Head of Operations | Failed instances are replaced, requests use healthy targets across AZs, and responders can identify the instance serving a request. | Target health, Auto Scaling activity history, AZ placement, logs, and server identity shown by the app. |
| Finance Lead | Capacity is not held at peak size all month, and costs are compared for quiet and peak usage. | AWS Pricing Calculator scenarios with explicit resource counts, hours, and data assumptions. |

## Cost discovery and design limits

Cost discovery is a required next step after the complete resource list is defined. We will enter the selected AWS Region, instance type and count, load-balancer hours and traffic, Regional NAT Gateway hours and data processing, storage, monitoring, and expected run duration into the AWS Pricing Calculator. We will compare a quiet-day case with a peak-day case and record the assumptions and alternatives. EC2 Free Tier eligibility must be checked against the selected account, Region, and current offer; it does not make every supporting resource free.

The estimate will depend on workload duration, data transfer, NAT processing, and the final Region. CPU may not represent every source of application demand, so the scaling metric must be checked during the load demonstration. The design assumes the demonstration application can run on more than one instance without local-only order state; durable shared order storage is not specified in the supplied lab brief and must be confirmed before adding database resources or estimating their cost. A Regional NAT Gateway is mandated, so its cost and outbound dependency remain part of the design.

## Sources

- Lab 1 problem description supplied with the course materials.
- [Client Profile: Onward Logistics](../client-profile/README.md).
- [Discovery Report Template](template.md).
- [Target tracking scaling policies for Amazon EC2 Auto Scaling](https://docs.aws.amazon.com/autoscaling/ec2/userguide/as-scaling-target-tracking.html).
- [Health checks for Application Load Balancer target groups](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/target-group-health-checks.html).
- [How Elastic Load Balancing works](https://docs.aws.amazon.com/elasticloadbalancing/latest/userguide/how-elastic-load-balancing-works.html).
- [Regional NAT gateways for automatic multi-AZ expansion](https://docs.aws.amazon.com/vpc/latest/userguide/nat-gateways-regional.html).

The editable completed report is in [`../source/completed-discovery-report.docx`](../source/completed-discovery-report.docx).
