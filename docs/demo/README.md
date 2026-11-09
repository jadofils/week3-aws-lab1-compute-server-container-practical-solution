# Live review: proving each client check

Run these with AWS CLI v2 signed in with short-lived credentials, `AWS_REGION=eu-west-1`, from the infrastructure repository root. Set the shared values once:

```bash
export AWS_REGION=eu-west-1 ENV=dev
export CLUSTER=onward-$ENV-orders-cluster SERVICE=onward-$ENV-orders-service
export URL=$(aws cloudformation describe-stacks --stack-name onward-$ENV-load-balancer \
  --query "Stacks[0].Outputs[?OutputKey=='ApplicationUrl'].OutputValue" --output text)
```

## 1. One public address; containers in private subnets with no internet route
```bash
curl -s $URL/api/info                       # name and lab name, via the only public address
aws ecs list-tasks --cluster $CLUSTER --service-name $SERVICE --query taskArns --output text \
  | xargs aws ecs describe-tasks --cluster $CLUSTER --tasks \
  --query "tasks[].attachments[].details[?name=='privateIPv4Address'||name=='subnetId'].value"
aws ec2 describe-route-tables --filters Name=tag:Name,Values=onward-$ENV-orders-private-rt \
  --query "RouteTables[].Routes[]"         # only 'local' and the S3 prefix-list route; no 0.0.0.0/0
aws ec2 describe-nat-gateways --filter Name=state,Values=available --query "length(NatGateways)"   # 0
```

## 2. A push to the app repository results in a new version, with no manual steps
Change some visible text in the app, merge to `main`, then watch:
```bash
aws codepipeline list-pipeline-executions --pipeline-name onward-$ENV-orders-pipeline --max-items 1
aws deploy list-deployments --application-name onward-$ENV-orders --deployment-group-name onward-$ENV-orders-dg --max-items 1
```

## 3. No downtime during a release, and a bad release can be reversed
- **No downtime.** During the release, run `scripts/load/watch-availability.sh $URL`. It prints the HTTP status every half second, and should show no non-200 responses.
- **Automatic reversal.** Merge a change that makes `/health` return 500. The green tasks never become healthy, the alarms fire, and CodeDeploy rolls back with status `Stopped` / `Failed`. Customers stay on blue.
- **Manual reversal.** While the old version is kept (10 minutes after a release), run:
  ```bash
  aws deploy stop-deployment --deployment-id <id> --auto-rollback-enabled
  ```
  After that window, run `git revert` on the bad commit and merge. The pipeline redeploys the previous code.

## 4. No long-lived credentials in the repositories or pipelines
```bash
git grep -nE "AKIA[0-9A-Z]{16}|aws_secret_access_key" || echo "no keys"
aws iam list-users --query "Users[?starts_with(UserName,'onward')]"   # [] - no pipeline IAM users
```
Show the workflow's `id-token: write` permission and the role's trust policy, which is limited to `repo:<owner>/onward-orders-app:ref:refs/heads/main`.

## 5. Adds containers under load and removes them afterwards
```bash
k6 run -e URL=$URL scripts/load/load-test.js        # or: scripts/load/generate-load.sh $URL 600
watch -n 15 "aws ecs describe-services --cluster $CLUSTER --services $SERVICE --query 'services[0].[desiredCount,runningCount]'"
aws application-autoscaling describe-scaling-activities --service-namespace ecs --max-items 5
```

## 6. Keeps working if a container fails or an Availability Zone is lost
```bash
scripts/load/watch-availability.sh $URL &       # keep this running
TASK=$(aws ecs list-tasks --cluster $CLUSTER --service-name $SERVICE --query 'taskArns[0]' --output text)
aws ecs stop-task --cluster $CLUSTER --task $TASK --reason "resilience demo"
```
To simulate losing a zone, stop every task in one subnet. Alternatively, use AWS Fault Injection Service's `aws:network:disrupt-connectivity` action on one private subnet. The other zone keeps serving, and ECS replaces the lost tasks.

## 7. A new engineer can follow the setup
Walk through the [README](../../README.md), then the [architecture](../architecture/README.md), the numbered templates, and the [decisions](../decisions/README.md).

## 8. Cost, including private connectivity, and rejected alternatives
See the [cost estimate](../cost/README.md) and the [decisions](../decisions/README.md).

## 9. Rebuild from the repositories
Delete the stacks in reverse order, then run `bootstrap/bootstrap.sh <owner> onward-orders-infrastructure`. Push the app once when prompted. The whole environment comes back from Git.
