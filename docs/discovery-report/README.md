# Discovery Report: Handling Sales Day Demand

| Lab | Lab 1 - Handling Sales Day Demand for the Onward Logistics Order System |
| --- | --- |
| Name | Jado Fils SEZIKEYE |
| Date | 9 November 2026 |
| Client | Onward Logistics (see the [Client Profile](../client-profile/README.md)) |
| Application stack | Java 21, Spring Boot, single full-stack application |
| AWS Region | `eu-west-1` (Ireland); see [decision D1](../decisions/README.md#d1-region-eu-west-1) |

## 1. What you found

### The client's need

Onward Logistics needs its order system to stay fast and available when sales-day orders triple within hours, to recover on its own from server or data-centre failures, and to stop paying for the extra capacity once the rush is over.

### Solutions that exist

| Solution | How it works | Trade off for this client |
| --- | --- | --- |
| Fixed fleet sized for the peak | Run enough servers for the busiest sales day, all month. | **Strength:** simple and predictable. **Weakness:** pays for about three times the needed capacity on ordinary days, and still fails if a sale beats the forecast. |
| Manual scaling | An engineer adds servers before or during a sale and removes them afterwards. | **Strength:** no new tooling. **Weakness:** someone has to watch and act mid-event, it reacts late, it is error-prone, and leadership explicitly does not want it. |
| EC2 Auto Scaling behind a load balancer | A load balancer spreads requests over healthy servers in several zones. A policy adds or removes servers to keep average CPU near a target. | **Strength:** grows and shrinks automatically, and replaces failed servers. **Weakness:** needs sensible limits and health checks, and new servers take a few minutes to boot. |

### What happens with no solution

- **Lost revenue at the worst moment.** Slow pages and errors during the peak mean abandoned orders. Last year's sales day already cost Onward merchants and customer trust.
- **Merchant and customer trust.** Merchants choose a delivery partner they can rely on during their own sales. Repeated failures push them to competitors, and the company's first priority is to "grow without breaking".
- **Wasted money or night-time firefighting.** The alternatives are paying for peak capacity all month (thin margins) or engineers resizing servers by hand during the event.
- **Security exposure.** Patching or fixing servers in a hurry tempts teams to open admin ports or give servers public addresses.

**Documented example.** On Black Friday 2018, J.Crew's website failed under demand for about five hours. Shoppers could not add items to their carts or pay. Estimates of lost sales were about $700,000 (experts quoted by Business Insider) to $775,000 (LovetheSales.com, reported by Retail TouchPoints), and the company had to extend its sale to compensate. Walmart, Lowe's, Best Buy and others had similar outages the same weekend.

### Sources

- [Business Insider: J.Crew's website is crashing as customers try to shop Black Friday deals (2018)](https://www.businessinsider.in/j-crews-website-is-crashing-as-customers-try-to-shop-black-friday-deals/articleshow/66776787.cms)
- [Retail TouchPoints: Site outages plague Walmart, J.Crew, Lowe's, Best Buy, Office Depot during Black Friday weekend](https://retailtouchpoints.com/features/news-briefs/site-outages-plague-walmart-j-crew-lowe-s-best-buy-office-depot-during-black-friday-weekend)
- [Amazon EC2 Auto Scaling target tracking policies](https://docs.aws.amazon.com/autoscaling/ec2/userguide/as-scaling-target-tracking.html)
- [Elastic Load Balancing health checks for target groups](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/target-group-health-checks.html)
- [NAT gateways (including Regional NAT gateways)](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-nat-gateway.html)
- [AWS Systems Manager Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [CloudFormation Git sync](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/git-sync.html)
- [AWS Pricing Calculator](https://calculator.aws/)

## 2. The business value of your solution

### Your chosen approach

**EC2 Auto Scaling across two Availability Zones, behind one internet-facing Application Load Balancer, with CPU target tracking.**

- **Normal day:** two Free Tier `t3.micro` servers run, one in each zone.
- **Sales day:** when average CPU goes above 50%, Auto Scaling adds servers, up to six (about three times normal capacity). It removes them again when CPU falls.
- **Failures:** a server that fails its health check is replaced automatically. Because there is always a server in each zone, losing one zone does not stop the service.
- **Security:**
  - Servers sit in private subnets with no public addresses and no admin ports.
  - They reach the internet for updates only outbound, through a Regional NAT Gateway.
  - Engineers use Session Manager instead of SSH.

This suits Onward better than a fixed fleet because it pays for peak capacity only during the peak. It suits them better than manual scaling because nobody has to act during the sale.

### Services you will use

| Service | The job it does | Why this service, used this way |
| --- | --- | --- |
| Amazon VPC (2 public + 2 private subnets in 2 AZs) | Network boundary: public subnets for the load balancer, private subnets for servers. | Enforces the "one public entry point" rule and spreads risk across two data centres. |
| Application Load Balancer | The single public address. Shares requests round robin across healthy servers and health-checks `/health` every 15 seconds. | Customers never see an unhealthy server, and the address never changes as servers come and go. |
| Amazon EC2 (Free Tier `t3.micro`, Amazon Linux 2023) | Runs the order app. A launch template installs and starts it automatically at boot. | The required compute; Free Tier keeps the quiet-day cost low; every server is identical. |
| EC2 Auto Scaling + target tracking | Keeps 2–6 servers, holds average CPU near 50%, and replaces unhealthy servers. | Grows on sales days and shrinks afterwards with nobody adjusting anything (CTO, Head of Operations). |
| Regional NAT Gateway | Outbound-only internet for the private servers: security updates, Java, the app download and Session Manager. | One gateway that AWS runs in each zone in use, so updates keep working if a zone fails, and inbound connections are impossible. |
| AWS Systems Manager Session Manager | Shell access to servers without SSH keys or open ports, through the outbound connection. | Meets "nobody should be opening admin ports to fix them". Sessions are logged with IAM identity. |
| AWS IAM (role + AWS managed policy) | Gives servers only the Session Manager permission. | Least privilege, no inline policies (client standard). |
| Amazon CloudWatch (detailed monitoring) | 1-minute CPU metrics that drive the scaling alarms. | Scaling reacts within minutes, not after 5-minute periods. |
| AWS CloudFormation + Git sync | The whole environment in one template, deployed from Git on every merge. | Nothing is clicked in the console, every change is reviewed, and it can be rebuilt from the repository. |
| AWS Budgets | Monthly budget with email alerts at 80% (actual) and 100% (forecast). | Spend is visible from day one (client standard). |
| GitHub Actions + GitHub Releases | Tests the app and publishes the versioned JAR. | No laptop builds, and every server runs a known, tested version. |

### Who benefits, and how you will know

| Stakeholder | Benefit | How you would know |
| --- | --- | --- |
| Chief Technology Officer | Sales-day load is absorbed automatically, and extra capacity is removed afterwards. | During a CPU spike the group's desired capacity rises from 2 towards 6, then falls back to 2. The scaling history shows each change with no human action. |
| Head of Security | One public entry point. Servers are unreachable from the internet, and no admin ports are open. | Instances have no public IP. The server security group allows only port 8080 from the load balancer. Session Manager works with port 22 closed. |
| Head of Operations | Failed servers and lost zones are handled automatically, and the answering server is visible. | Terminate a server: the page keeps answering and a replacement appears. The "which server is answering" panel shows servers joining and leaving. |
| Finance Lead | Pays for peak capacity only during the peak, with costs explained. | The [cost page](../cost/README.md) compares a quiet day and a peak day, line by line. Budget alerts arrive at 80% of the limit. |
| Merchants and customers | Fast, reliable ordering during sales. | No load balancer 5xx errors during the spike test; response times stay steady while servers are added. |

### Limits and risks of your design

- **Scaling takes minutes.** A new server needs about 2–4 minutes to boot and pass health checks. A very sudden surge is absorbed by the two running servers until the new ones arrive. Raising `MinSize` before a known sale (or adding a scheduled action) closes that gap.
- **Orders are in memory per server** in this demonstration. They are lost when a server is replaced and are not shared. Real orders need a shared database, which is a separate decision and cost.
- **HTTP only** for the lab. Production needs an HTTPS listener with an ACM certificate.
- **The NAT gateway is a fixed hourly cost** even on quiet days, and it is the largest line item after the load balancer.
- **Region failure is not covered.** The design survives losing one zone, not the whole Region.
- **New servers download the app from GitHub.** If GitHub is unreachable when Auto Scaling launches a server, that server can't start until GitHub is back. Servers already running are not affected.
- **Free Tier depends on the account.** Eligibility for `t3.micro` hours depends on the account's Free Tier plan; outside it, the normal hourly price applies.
