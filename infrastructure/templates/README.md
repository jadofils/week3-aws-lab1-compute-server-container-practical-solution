# CloudFormation template status

The numbered CloudFormation files currently in this directory are an earlier EC2 and NAT Gateway draft. They do **not** meet Lab 1's required ECS Fargate, private VPC endpoint, ECR, EventBridge, CodePipeline, or CodeDeploy blue/green requirements. Do not deploy that draft or treat it as the proposed architecture.

The replacement stack flow must be implemented before deployment:

1. `0-artifact-store-bootstrap.yaml` — optional bootstrap only for packaging nested templates; it must not store a manually managed application JAR.
2. `1-main-root-stack.yaml` — shared project parameters/tags and nested stack wiring.
3. `2-network.yaml` — VPC, two public ALB subnets, two private task subnets, no NAT, no general internet route, and ECR API/DKR, S3 gateway, and CloudWatch Logs endpoints.
4. `3-security-and-iam.yaml` — ALB/task/endpoint security groups, ECS task execution and task roles, and GitHub Actions OIDC role. Attach managed policies; do not add inline role policies.
5. `4-load-balancer.yaml` — internet-facing ALB, production and test listeners, and blue/green IP target groups.
6. `5-compute-and-autoscaling.yaml` — ECR repository, ECS cluster, Fargate task definition/service, `CODE_DEPLOY` deployment controller, and ECS target-tracking scaling.
7. `6-operations-and-monitoring.yaml` — CodeDeploy application/deployment group, CodePipeline, EventBridge ECR push rule, CloudWatch alarms/log group, and permissions for pipeline/deployment invocation.

CloudFormation GitSync must track the infrastructure repository's configured branch, template, and deployment file. Nested templates must be made available to CloudFormation by a repeatable code-based publishing mechanism, and each update must touch a tracked GitSync input. Do not assume that changing an S3 object alone triggers GitSync.

The root parameter list should include repository/connection identifiers, AWS Region assumptions, image repository name, environment, task CPU/memory, desired/minimum/maximum tasks, and scaling target. Keep task sizing and run-time assumptions open for AWS Pricing Calculator work. This repository is not yet split: the app repository must contain the workflow, Dockerfile, `taskdef.json`, and `appspec.yaml`; the infrastructure repository must contain these CloudFormation templates and GitSync deployment definition.

## Current app deployment artifacts

- [`app/Dockerfile`](../../app/Dockerfile) builds and runs the Java Spring Boot full-stack app as a non-root container.
- [`app/taskdef.json`](../../app/taskdef.json) is a CodePipeline task definition substitution template.
- [`app/appspec.yaml`](../../app/appspec.yaml) directs CodeDeploy to update the ECS service target.
- [`app/build-and-push-ecr.yml`](../../app/build-and-push-ecr.yml) is the GitHub Actions workflow source; move it to `.github/workflows/build-and-push-ecr.yml` at the root of the separate app repository and configure the three repository variables documented in that file.

The infrastructure templates remain an implementation task; architecture and discovery docs describe the required target design, not a claim that it is already deployable.
