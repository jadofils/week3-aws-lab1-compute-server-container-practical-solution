# Live review: proving each client check

## Deploy with Git sync (one time, in the console, Region eu-west-1)

1. **Bootstrap.** Go to *CloudFormation → Create stack → With new resources*.
   - Upload `infrastructure/bootstrap/gitsync-bootstrap.yaml` and name the stack `onward-dev-gitsync-bootstrap`.
   - Acknowledge the IAM capability and create the stack.
   - It creates the GitHub connection `onward-dev-github` and the role `onward-dev-gitsync-role`.
2. **Approve the connection.** Go to *Developer Tools → Settings → Connections*.
   - Choose `onward-dev-github` → *Update pending connection*.
   - Authorize the GitHub app for this repository. The status becomes *Available*.
3. **Create the stack from Git.** Go to *CloudFormation → Create stack → With new resources → Sync from Git*.
   - Stack name: `onward-dev-orders`.
   - Provider: GitHub, connection `onward-dev-github`, repository `week3-aws-lab1-compute-server-container-practical-solution`, branch `main`.
   - Deployment file path: `infrastructure/deployment.yaml`.
   - IAM role: existing `onward-dev-gitsync-role`.
   - Create. Git sync builds the stack in about 10–15 minutes. After that, every merge to `main` that changes the template or the deployment file updates it.

## Step by step checks

Use AWS CLI v2 with short-lived credentials, or AWS CloudShell. First set:

```bash
export AWS_REGION=eu-west-1 STACK=onward-dev-orders ASG=onward-dev-orders-asg
export URL=$(aws cloudformation describe-stacks --stack-name $STACK \
  --query "Stacks[0].Outputs[?OutputKey=='ApplicationUrl'].OutputValue" --output text)
echo $URL
```

## 1. One public address that stays reachable as servers come and go
- Open `$URL` in a browser. It shows your name, the lab name and **"This page was answered by i-… (eu-west-1a, 10.0.10.x)"**.
- During checks 3 to 5, keep this running in a second terminal. It should print only `200`:
  ```bash
  while true; do curl -s -m 3 -o /dev/null -w "%{http_code} " "$URL/health"; sleep 1; done
  ```

## 2. Requests are shared across servers, and the page shows which server answered
- In the UI, click **Start watching** under "Which server is answering?". Both servers appear, in different zones, with similar answer counts.
- From a terminal:
  ```bash
  for i in $(seq 1 10); do curl -s $URL/health; echo; done
  ```

## 3. Rising demand adds servers, and the number shrinks again afterwards
1. In the UI, **Sales-day CPU spike**: 420 seconds, 8 requests → **Start spike**. The results show the spike running on both servers.
2. Watch the server list in the UI. Average CPU passes 50%, the desired capacity rises, and new instance IDs appear in the watcher within about 3 to 5 minutes. Click **Start spike** again so the new servers join the load. To follow the count from a terminal:
   ```bash
   watch -n 15 "aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names $ASG \
     --query 'AutoScalingGroups[0].[DesiredCapacity,length(Instances)]' --output text"
   ```
3. After the spike ends, or after **Stop spikes**, CPU falls and target tracking removes servers, back to 2 after roughly 10 to 15 minutes.
4. Show the evidence:
   ```bash
   aws autoscaling describe-scaling-activities --auto-scaling-group-name $ASG --max-items 6 \
     --query "Activities[].[StartTime,Description]" --output table
   ```

**Secure remote-access alternative** (no open ports):
```bash
aws ssm start-session --target <instance-id>
sudo dnf -y install stress-ng && stress-ng --cpu 0 --timeout 420s
```

## 4. A failed server is replaced without manual action
```bash
ID=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names $ASG \
  --query "AutoScalingGroups[0].Instances[0].InstanceId" --output text)
aws ec2 terminate-instances --instance-ids $ID
```
Alternatively, stop only the app with `sudo systemctl stop onward-orders` in a Session Manager session. The ALB health check fails and Auto Scaling replaces the server.

The watcher keeps getting `200` from the other server. The scaling history shows the replacement, and a new instance ID appears.

## 5. The application keeps working if one Availability Zone is lost
To simulate losing a zone, terminate every server in one AZ:
```bash
aws ec2 describe-instances --filters "Name=tag:aws:autoscaling:groupName,Values=$ASG" \
  "Name=availability-zone,Values=${AWS_REGION}a" "Name=instance-state-name,Values=running" \
  --query "Reservations[].Instances[].InstanceId" --output text | xargs aws ec2 terminate-instances --instance-ids
```
The page keeps answering from the zone-B servers, and Auto Scaling launches replacements. For a fuller test, AWS Fault Injection Service's `aws:network:disrupt-connectivity` action can cut a whole subnet.

## 6. Only the load balancer is public; servers can still download updates
```bash
# Servers have no public IP
aws ec2 describe-instances --filters "Name=tag:aws:autoscaling:groupName,Values=$ASG" \
  --query "Reservations[].Instances[].[InstanceId,PrivateIpAddress,PublicIpAddress]" --output table
# The server security group admits only the ALB security group on 8080, and has no port 22
aws ec2 describe-security-groups --filters Name=group-name,Values=onward-dev-orders-server-sg \
  --query "SecurityGroups[0].IpPermissions"
```
Show the private route table: `0.0.0.0/0 → nat-…`, with no route to the internet gateway. Then, in a Session Manager session, prove outbound access works:
```bash
sudo dnf check-update --security | tail -3
curl -sI https://github.com | head -1
```
Trying to reach a server's private IP from your laptop fails, because there is no route.

## 7. What it costs and which alternatives were rejected
Walk through the [cost page](../cost/README.md) (quiet vs peak day) and the [decisions](../decisions/README.md).

## 8. The whole setup can be rebuilt from the repository
Delete the stack (in the console, first turn off its Git sync under the stack's *Git sync* tab), then repeat step 3 of the deploy section above. Everything comes back from `template.yaml`, `deployment.yaml` and the GitHub Release.

## After the review: stop the charges
1. In *CloudFormation → onward-dev-orders → Git sync*, disconnect Git sync, so it doesn't recreate the stack.
2. Delete the stack `onward-dev-orders`. The NAT gateway and load balancer take a few minutes.
3. Optionally, delete `onward-dev-gitsync-bootstrap`.
