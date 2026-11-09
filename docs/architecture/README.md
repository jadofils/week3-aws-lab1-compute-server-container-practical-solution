# Architecture: Onward orders on ECS Fargate with blue/green delivery

Region `eu-west-1`, environment `dev`. Every resource below is defined in the [infrastructure templates](../../infrastructure/README.md) and deployed by CloudFormation Git sync.

## 1. Resources inside the VPC

```mermaid
flowchart TB
  user([Merchants and customers]) -->|HTTP :80| alb

  subgraph vpc["VPC 10.0.0.0/16 · onward-dev-orders-vpc"]
    igw[Internet gateway]
    subgraph azA["Availability Zone A"]
      subgraph pubA["Public subnet 10.0.0.0/24"]
        albA[ALB node]
      end
      subgraph privA["Private subnet 10.0.10.0/24 · no internet route"]
        taskA[Fargate task<br/>onward-orders :8080]
        vpceA[Endpoint ENIs<br/>ECR API · ECR DKR · Logs]
      end
    end
    subgraph azB["Availability Zone B"]
      subgraph pubB["Public subnet 10.0.1.0/24"]
        albB[ALB node]
      end
      subgraph privB["Private subnet 10.0.11.0/24 · no internet route"]
        taskB[Fargate task<br/>onward-orders :8080]
        vpceB[Endpoint ENIs<br/>ECR API · ECR DKR · Logs]
      end
    end
    alb{{"Application Load Balancer · onward-dev-orders-alb<br/>:80 production → blue · :9000 test → green"}}
    s3gw[(S3 gateway endpoint<br/>on private route table)]
  end

  igw --- alb
  alb --- albA & albB
  albA -->|app SG allows ALB SG only| taskA
  albB --> taskB
  taskA & taskB -->|HTTPS 443| vpceA & vpceB
  taskA & taskB -->|image layers| s3gw
  vpceA & vpceB -.AWS network.-> aws[(ECR · CloudWatch Logs)]
  s3gw -.AWS network.-> s3[(S3 ECR layer bucket)]
```

| Security rule | Where it is enforced |
| --- | --- |
| One public entry point | Only the ALB is in public subnets. Tasks have `AssignPublicIp: DISABLED`. |
| No internet path for the application | The private route table has only the `local` route and the S3 gateway route. There is no NAT gateway. |
| Tasks accept only load-balancer traffic | The app security group allows port 8080 from the ALB security group only. |
| Endpoints accept only task traffic | The endpoint security group allows port 443 from the app security group only. |

## 2. Delivery pipeline: how a change reaches customers

```mermaid
sequenceDiagram
  autonumber
  actor Dev as Engineer
  participant GH as GitHub (app repo)
  participant GHA as GitHub Actions
  participant STS as AWS STS (OIDC)
  participant S3 as S3 pipeline bucket
  participant ECR as Amazon ECR
  participant EB as EventBridge
  participant CP as CodePipeline
  participant CD as CodeDeploy
  participant ECS as ECS service
  participant ALB as ALB

  Dev->>GH: merge pull request to main (the only manual step)
  GH->>GHA: workflow build-and-deploy.yml
  GHA->>GHA: mvn verify (tests)
  GHA->>STS: OIDC token → short-lived role credentials
  GHA->>S3: upload deploy/orders-deploy.zip (taskdef.json + appspec.yaml)
  GHA->>ECR: push image :<commit-sha> and :release
  ECR-->>EB: "ECR Image Action" PUSH, tag release
  EB->>CP: StartPipelineExecution
  CP->>CP: Source = image detail + bundle
  CP->>CD: CodeDeployToECS (new task definition revision)
  CD->>ECS: start green task set (private subnets)
  CD->>ALB: test listener :9000 → green, health checks pass
  CD->>ALB: production :80 shifts 10% → green, waits 5 min
  alt alarms fire (5xx or unhealthy targets)
    CD->>ALB: shift 100% back to blue (automatic rollback)
  else healthy
    CD->>ALB: shift 100% → green
    CD->>ECS: keep blue 10 min for instant rollback, then terminate
  end
```

## 3. Infrastructure delivery

```mermaid
flowchart LR
  eng[Engineer merges to<br/>infrastructure repo main] --> gs[CloudFormation Git sync]
  gs --> s1[1 network] --> s2[2 security and IAM] --> s3[3 VPC endpoints] --> s4[4 registry and artifacts]
  s4 --> s5[5 load balancer] --> s6[6 ECS service] --> s7[7 delivery pipeline] --> s8[8 budget]
```

Each numbered stack has its own deployment file in `infrastructure/deployments/`. Stacks share values through CloudFormation exports named `onward-<env>-<stack>-<Value>`. A change to a template or its deployment file is deployed when it is merged.

## 4. Scaling and failure behaviour

- **Container failure.** The ALB stops sending to an unhealthy task, and ECS replaces it to keep the desired count.
- **Availability Zone loss.** The other zone's task keeps serving behind the ALB. ECS starts replacements in the healthy zone, and the endpoints exist in both zones.
- **Busy days.** CPU target tracking at 50% raises the task count from 2 up to 6, then removes tasks once load drops (120-second scale-in cooldown).
- **Idle.** An optional schedule scales to 0 tasks at night and on weekends. Deleting the stacks removes the remaining hourly cost; see the [cost estimate](../cost/README.md).
- **Data.** Demo orders are in memory per task. They are lost when a task is replaced and are not shared between tasks.
