# Onward orders infrastructure

This folder is the root of the **infrastructure repository** (`onward-orders-infrastructure`). Everything is AWS CloudFormation, deployed by **CloudFormation Git sync** from the `main` branch. Nothing is created in the console.

Region: `eu-west-1` ([why](../docs/decisions/README.md#d1-region-eu-west-1)). Naming: `onward-<env>-<purpose>`. Every taggable resource carries `Project=onward`, `Owner=Jado Fils SEZIKEYE` and `Environment=<env>`. There are no inline IAM policies.

## Layout

```text
bootstrap/
  0-gitsync-bootstrap.yaml   GitHub connection + GitSync role (deployed once by bootstrap.sh)
  bootstrap.sh               one-time setup: creates the GitSync configurations in order
templates/                   read in this order
  1-network.yaml             VPC, 2 public + 2 private subnets, no NAT, S3 gateway endpoint
  2-security-and-iam.yaml    security groups, task/CodeDeploy/CodePipeline/EventBridge roles, GitHub OIDC role
  3-vpc-endpoints.yaml       ECR API, ECR DKR and CloudWatch Logs interface endpoints
  4-registry-and-artifacts.yaml  ECR repository, versioned S3 pipeline bucket
  5-load-balancer.yaml       internet-facing ALB, prod :80 and test :9000 listeners, blue/green target groups
  6-ecs-service.yaml         Fargate cluster, task definition, service (CODE_DEPLOY controller), auto scaling
  7-delivery-pipeline.yaml   rollback alarms, CodeDeploy blue/green, CodePipeline, EventBridge trigger
  8-budget.yaml              monthly budget with email alerts
deployments/
  dev-<n>-<stack>.yaml       GitSync deployment file per stack: template path, parameters, tags
```

Stacks are independent GitSync stacks named `onward-<env>-<stack>`. They share IDs through exports named `onward-<env>-<stack>-<Value>`, so each template can be read on its own ([why not nested stacks](../docs/decisions/README.md#d5-one-gitsync-stack-per-template-linked-by-exports)).

## First-time setup (rebuild from scratch)

1. Sign in to the AWS CLI with short-lived admin credentials and run `export AWS_REGION=eu-west-1`.
2. Run `./bootstrap/bootstrap.sh <github-owner> onward-orders-infrastructure dev`. It:
   - deploys `0-gitsync-bootstrap.yaml`;
   - waits while you approve the GitHub connection once (AWS requires a person to approve this handshake);
   - links the repository and creates the GitSync configurations for stacks 1 to 8 in order.
3. When stack 2 finishes, the script prints `GitHubActionsRoleArn`. Set it with `AWS_REGION` as variables in the app repository, then push the app. The script waits for the first `release` image before it creates stack 6, because the ECS service needs an image to start.
4. Before stack 8, set a real `AlertEmail` in `deployments/dev-8-budget.yaml`.

After this, **every change is a merge**. Edit a template or deployment file, merge to `main`, and GitSync applies it.

## Changing things safely

- **Task size, scaling limits, off-hours scale-down:** `deployments/dev-6-ecs-service.yaml`. Task CPU and memory must also match `taskdef.json` in the app repository, which is the source of each release's task definition.
- **Do not edit the `TaskDefinition` resource in `6-ecs-service.yaml` after the first deployment.** CloudFormation cannot update a CODE_DEPLOY-controlled service's task definition; releases change it instead.
- **Release strategy:** `DeploymentConfigName` in `deployments/dev-7-delivery-pipeline.yaml`.
- **Preview a green release on the test listener:** set `TestTrafficCidr` to your IP/32 in `deployments/dev-2-security-and-iam.yaml`.
- **Tear down:** delete stacks 8 → 1, empty the pipeline bucket first, then the bootstrap stack.

## Checks before merging

```bash
pip install cfn-lint
cfn-lint --regions eu-west-1 -- templates/*.yaml bootstrap/*.yaml
```
