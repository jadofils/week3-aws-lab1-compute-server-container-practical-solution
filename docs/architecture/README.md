# Architecture: Handling Sales Day Demand

Region `eu-west-1`, environment `dev`, stack `onward-dev-orders`. Every resource below is in [`infrastructure/template.yaml`](../../infrastructure/template.yaml).

**Editable diagram:** [`onward-sales-day-architecture.drawio`](onward-sales-day-architecture.drawio). Open it at [app.diagrams.net](https://app.diagrams.net) with *File → Open from → Device*, or use *Arrange → Insert → Advanced → From Text… → XML* to paste it into an existing diagram. It uses draw.io's built-in AWS 2021 icons: AWS Cloud → Region → VPC → Availability Zones → public and private subnets, the ALB, the Regional NAT, the Auto Scaling group, and numbered traffic and change flows. The Mermaid diagrams below show the same design in text form, so GitHub renders them.

## 1. Resources inside the VPC: public vs private

```mermaid
flowchart TB
  user([Merchants and customers]) -->|HTTP :80| igw

  subgraph vpc["VPC 10.0.0.0/16 · onward-dev-orders-vpc"]
    igw[Internet gateway]
    alb{{"Application Load Balancer · onward-dev-orders-alb<br/>PUBLIC · the only entry point"}}
    nat[["Regional NAT Gateway · onward-dev-orders-regional-nat<br/>outbound only · runs in each AZ in use"]]

    subgraph azA["Availability Zone A"]
      subgraph pubA["PUBLIC subnet 10.0.0.0/24"]
        albA[ALB node]
      end
      subgraph privA["PRIVATE subnet 10.0.10.0/24 · no public IPs"]
        ec2A["EC2 t3.micro · Amazon Linux 2023<br/>order app :8080"]
      end
    end

    subgraph azB["Availability Zone B"]
      subgraph pubB["PUBLIC subnet 10.0.1.0/24"]
        albB[ALB node]
      end
      subgraph privB["PRIVATE subnet 10.0.11.0/24 · no public IPs"]
        ec2B["EC2 t3.micro · Amazon Linux 2023<br/>order app :8080"]
        ec2C["extra servers on sales days<br/>(up to 6 in total)"]
      end
    end

    asg[/"Auto Scaling group onward-dev-orders-asg · min 2 · max 6<br/>target tracking: average CPU 50%"/]
  end

  igw --> alb
  alb --- albA & albB
  albA -->|":8080, server SG allows ALB SG only"| ec2A
  albB --> ec2B & ec2C
  asg -. launches / replaces .-> ec2A & ec2B & ec2C
  ec2A & ec2B & ec2C -->|outbound HTTPS/HTTP only| nat
  nat --> igw
  igw -->|security updates · Java · Systems Manager| internet([Internet: dnf repos, AWS APIs])
  ec2A & ec2B & ec2C -->|app JAR| s3gw[["S3 gateway endpoint (free)"]]
  s3gw --> bucket[("S3 release bucket (private)")]
  copier["Lambda release copier<br/>(deploy time only)"] -->|GitHub Release JAR| bucket
```

| | Public | Private |
| --- | --- | --- |
| Resources | Application Load Balancer, Regional NAT Gateway (public IPs), internet gateway | EC2 servers (Auto Scaling group) |
| Inbound from internet | ALB on port 80 only | **None.** No public IP, and the security group only admits port 8080 from the ALB security group |
| Outbound to internet | n/a | Through the NAT gateway only: security updates, Java, the app JAR from GitHub Releases, and Session Manager |
| Admin access | n/a | Session Manager over the outbound connection. No SSH or RDP port is open |

## 2. How a request flows

```mermaid
sequenceDiagram
  autonumber
  actor C as Customer
  participant ALB as Load balancer (public)
  participant S1 as Server in AZ A (private)
  participant S2 as Server in AZ B (private)
  C->>ALB: GET http://<alb-dns>/
  ALB->>S1: forward (round robin, healthy targets only)
  S1-->>C: page + "answered by i-0aaa · eu-west-1a"
  C->>ALB: next request (e.g. "Which server is answering?" poll)
  ALB->>S2: forward
  S2-->>C: "answered by i-0bbb · eu-west-1b"
  Note over ALB,S2: Health check GET /health every 15 s. Two failures remove a server from rotation.
```

## 3. How capacity changes on a sales day

```mermaid
flowchart LR
  spike["Demand rises<br/>(or UI 'Sales-day CPU spike')"] --> cpu["Average CPU > 50%<br/>1-minute metrics"]
  cpu --> tt["Target tracking alarm"] --> up["Auto Scaling raises desired count (max 6)"]
  up --> boot["New server boots: dnf updates + Java via NAT,<br/>download app release, start service"]
  boot --> hc["Passes /health"] --> rot["ALB adds it to rotation"]
  rot --> calm["Rush ends · CPU falls"] --> down["Target tracking scales in to 2"]
```

## 4. How a server gets what it needs

1. The launch template boots the latest **Amazon Linux 2023** AMI (resolved from the public SSM parameter) on `t3.micro`, with IMDSv2 required and an encrypted 8 GB gp3 disk.
2. The user data:
   - installs security updates and Java 21 through the NAT gateway;
   - copies `onward-orders.jar` for `AppVersion` from the **private S3 release bucket** through the **S3 gateway endpoint**, so this step needs no internet;
   - runs the app as a `systemd` service that restarts on failure;
   - waits for `/health`, then sends `cfn-signal`.

   The S3 copy is made once, at deploy time, by a small Lambda custom resource that downloads the GitHub Release. Launching or replacing a server therefore never depends on GitHub being available.
3. The Auto Scaling group uses **ELB health checks**, so a server whose app stops answering is replaced, not just one whose hardware fails.

## 5. How changes are delivered

```mermaid
flowchart LR
  appchg[Merge app change] --> gha["GitHub Actions:<br/>mvn verify, publish release vX.Y.Z"]
  infchg["Merge change to infrastructure/<br/>(e.g. AppVersion: vX.Y.Z)"] --> gs[CloudFormation Git sync]
  gs --> stack["Stack onward-dev-orders updated"]
  stack --> roll["Rolling update: one server at a time,<br/>min 2 in service, waits for cfn-signal"]
  gha -. release used by .-> roll
```

## 6. Failure behaviour

- **A server dies, or its app stops.** The ALB stops sending to it after three failed health checks (about 45 seconds). Auto Scaling terminates it and launches a replacement.
- **GitHub or the internet is unavailable.** New servers still install the app from the private S3 copy through the gateway endpoint.
- **A whole Availability Zone is lost.** The server in the other zone keeps answering through the ALB node in that zone. Auto Scaling launches replacements in the healthy zone, and the Regional NAT keeps serving the surviving zone.
- **Load drops after a sale.** Target tracking removes servers gradually, back to the minimum of 2.
