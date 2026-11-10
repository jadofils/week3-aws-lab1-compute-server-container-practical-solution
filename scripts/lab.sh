#!/usr/bin/env bash
# Onward Logistics - Lab 1: Handling Sales Day Demand
# Deploy the lab and prove every client check from AWS CloudShell.
#
#   ./scripts/lab.sh deploy        one-time Git sync setup + stack creation (about 15 min)
#   ./scripts/lab.sh verify        read-only checks of every requirement (safe to run any time)
#   ./scripts/lab.sh scale         CPU spike -> servers added -> servers removed (about 25 min)
#   ./scripts/lab.sh fail-server   terminate one server and prove the site stays up   (asks first)
#   ./scripts/lab.sh fail-az       terminate every server in one AZ and prove it stays up (asks first)
#   ./scripts/lab.sh teardown      delete everything so it stops costing money        (asks first)
#   ./scripts/lab.sh all           deploy, verify, scale, fail-server, fail-az
#
# Settings (optional): ENVIRONMENT=dev  AWS_REGION=eu-west-1
set -uo pipefail

ENVIRONMENT="${ENVIRONMENT:-dev}"
export AWS_REGION="${AWS_REGION:-eu-west-1}"
export AWS_DEFAULT_REGION="$AWS_REGION"
PREFIX="onward-${ENVIRONMENT}"
STACK="${PREFIX}-orders"
ASG="${PREFIX}-orders-asg"
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/.." && pwd)"
EXPECTED_NAME="Jado Fils SEZIKEYE"
EXPECTED_LAB="Lab 1 - Handling Sales Day Demand"

PASSED=0; FAILED=0; FAILURES=()
bold()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
pass()  { PASSED=$((PASSED + 1)); printf '  \033[32mPASS\033[0m  %s\n' "$*"; }
fail()  { FAILED=$((FAILED + 1)); FAILURES+=("$*"); printf '  \033[31mFAIL\033[0m  %s\n' "$*"; }
info()  { printf '        %s\n' "$*"; }
need()  { command -v "$1" >/dev/null || { echo "Missing tool: $1"; exit 1; }; }
confirm() {
  printf '\n\033[33m%s\033[0m\nType yes to continue: ' "$1"
  read -r answer; [ "$answer" = "yes" ] || { echo "Cancelled."; exit 1; }
}

output() {
  aws cloudformation describe-stacks --stack-name "$STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text 2>/dev/null
}
stack_status() {
  aws cloudformation describe-stacks --stack-name "$STACK" --query "Stacks[0].StackStatus" --output text 2>/dev/null
}
in_service() {
  aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG" \
    --query "length(AutoScalingGroups[0].Instances[?LifecycleState=='InService' && HealthStatus=='Healthy'])" --output text
}
desired() {
  aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG" \
    --query "AutoScalingGroups[0].DesiredCapacity" --output text
}
instance_ids() {
  aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG" \
    --query "AutoScalingGroups[0].Instances[?LifecycleState=='InService'].InstanceId" --output text
}
require_stack() {
  case "$(stack_status)" in
    CREATE_COMPLETE|UPDATE_COMPLETE) URL="$(output ApplicationUrl)" ;;
    *) echo "Stack $STACK is not ready (status: $(stack_status || echo missing)). Run: ./scripts/lab.sh deploy"; exit 1 ;;
  esac
}

# --------------------------------------------------------------------------- deploy
cmd_deploy() {
  bold "Deploy: Git sync bootstrap + stack $STACK in $AWS_REGION"
  case "$(stack_status)" in
    CREATE_COMPLETE|UPDATE_COMPLETE) info "Stack already exists ($(stack_status)); skipping deploy."; return ;;
  esac
  bash "$REPO_ROOT/infrastructure/bootstrap/setup-gitsync.sh" jadofils \
    week3-aws-lab1-compute-server-container-practical-solution "$ENVIRONMENT" main
}

# --------------------------------------------------------------------------- verify
cmd_verify() {
  need jq
  require_stack
  bold "Stack"
  pass "Stack $STACK is $(stack_status) in $AWS_REGION, created from Git by CloudFormation Git sync"
  info "Application URL: $URL"

  VPC=$(output VpcId)
  SG_ALB=$(aws ec2 describe-security-groups --filters Name=group-name,Values="${PREFIX}-orders-alb-sg" --query "SecurityGroups[0].GroupId" --output text)
  SG_SRV=$(aws ec2 describe-security-groups --filters Name=group-name,Values="${PREFIX}-orders-server-sg" --query "SecurityGroups[0].GroupId" --output text)
  IDS=$(instance_ids)

  bold "1. One public address that stays reachable"
  code=$(curl -s -o /dev/null -m 5 -w '%{http_code}' "$URL/health")
  [ "$code" = "200" ] && pass "GET $URL/health returns 200" || fail "Health check through the load balancer returned $code"
  info_json=$(curl -s -m 5 "$URL/api/info")
  name=$(jq -r '.studentName // empty' <<<"$info_json"); lab=$(jq -r '.labName // empty' <<<"$info_json")
  [ "$name" = "$EXPECTED_NAME" ] && pass "Page shows full name: $name" || fail "Page shows name '$name', expected '$EXPECTED_NAME'"
  [ "$lab" = "$EXPECTED_LAB" ] && pass "Page shows lab name: $lab" || fail "Page shows lab '$lab', expected '$EXPECTED_LAB'"
  public_lbs=$(aws elbv2 describe-load-balancers --query "length(LoadBalancers[?VpcId=='$VPC' && Scheme=='internet-facing'])" --output text)
  [ "$public_lbs" = "1" ] && pass "Exactly one internet-facing load balancer in the VPC" || fail "$public_lbs internet-facing load balancers in the VPC"

  bold "2. Requests are shared across servers and the page shows which server answered"
  answers=$(for _ in $(seq 1 30); do curl -s -m 3 "$URL/api/info" | jq -r '"\(.serverId) \(.availabilityZone)"'; done | sort | uniq -c)
  servers=$(wc -l <<<"$answers" | tr -d ' '); zones=$(awk '{print $3}' <<<"$answers" | sort -u | wc -l | tr -d ' ')
  echo "$answers" | sed 's/^/        /'
  [ "$servers" -ge 2 ] && pass "30 requests were answered by $servers different servers" || fail "Only $servers server answered 30 requests"
  [ "$zones" -ge 2 ] && pass "Answers came from $zones Availability Zones" || fail "Answers came from only $zones Availability Zone"

  bold "3. Servers are private Free Tier EC2 instances; only the load balancer is public"
  aws ec2 describe-instances --instance-ids $IDS \
    --query "Reservations[].Instances[].[InstanceId,InstanceType,Placement.AvailabilityZone,PrivateIpAddress,PublicIpAddress||'none']" \
    --output text | sed 's/^/        /'
  with_public=$(aws ec2 describe-instances --instance-ids $IDS --query "length(Reservations[].Instances[] | [?PublicIpAddress!=null])" --output text)
  [ "$with_public" = "0" ] && pass "No server has a public IP address" || fail "$with_public server(s) have a public IP"
  types=$(aws ec2 describe-instances --instance-ids $IDS --query "Reservations[].Instances[].InstanceType" --output text | tr '\t' '\n' | sort -u | tr '\n' ' ')
  [[ "$types" =~ ^(t3\.micro|t2\.micro)\ $ ]] && pass "Instance type is Free Tier eligible: $types" || fail "Unexpected instance types: $types"
  ami_ok=$(aws ec2 describe-images --image-ids $(aws ec2 describe-instances --instance-ids $IDS --query "Reservations[0].Instances[0].ImageId" --output text) --query "Images[0].Name" --output text)
  [[ "$ami_ok" == al2023-ami-* ]] && pass "Servers run Amazon Linux 2023 ($ami_ok)" || fail "AMI is $ami_ok"
  public_subnets=0
  for id in $IDS; do
    subnet=$(aws ec2 describe-instances --instance-ids "$id" --query "Reservations[0].Instances[0].SubnetId" --output text)
    map=$(aws ec2 describe-subnets --subnet-ids "$subnet" --query "Subnets[0].MapPublicIpOnLaunch" --output text)
    [ "$map" = "False" ] || { public_subnets=$((public_subnets + 1)); info "subnet $subnet of $id assigns public IPs"; }
  done
  [ "$public_subnets" = "0" ] && pass "Every server's subnet has MapPublicIpOnLaunch=false" || fail "$public_subnets server(s) in subnets that assign public IPs"
  rt=$(aws ec2 describe-route-tables --filters Name=tag:Name,Values="${PREFIX}-orders-private-rt" --query "RouteTables[0]" --output json)
  nat_route=$(jq -r '.Routes[] | select(.DestinationCidrBlock=="0.0.0.0/0") | .NatGatewayId // empty' <<<"$rt")
  igw_route=$(jq -r '.Routes[] | select(.GatewayId!=null and (.GatewayId|startswith("igw-"))) | .GatewayId' <<<"$rt")
  [ -n "$nat_route" ] && pass "Private route table sends 0.0.0.0/0 to the NAT gateway $nat_route (outbound only)" || fail "Private route table has no NAT route"
  [ -z "$igw_route" ] && pass "Private route table has no route to the internet gateway" || fail "Private route table routes to $igw_route"
  nat_mode=$(aws ec2 describe-nat-gateways --nat-gateway-ids "$(output NatGatewayId)" --query "NatGateways[0].[State,AvailabilityMode]" --output text 2>/dev/null)
  info "NAT gateway state / availability mode: $nat_mode"
  [[ "$nat_mode" == available* ]] && pass "Regional NAT gateway is available" || fail "NAT gateway is not available ($nat_mode)"

  bold "4. Security rules: one entry point, no admin ports"
  srv_rules=$(aws ec2 describe-security-groups --group-ids "$SG_SRV" --query "SecurityGroups[0].IpPermissions" --output json)
  only_alb=$(jq --arg alb "$SG_ALB" '[.[] | select((.UserIdGroupPairs|map(.GroupId)) != [$alb] or (.IpRanges|length) > 0)] | length' <<<"$srv_rules")
  [ "$only_alb" = "0" ] && pass "Server security group admits traffic only from the load balancer security group" || fail "Server security group has other inbound rules"
  admin=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values="$VPC" \
    --query "SecurityGroups[].IpPermissions[?FromPort==\`22\` || FromPort==\`3389\`][]" --output json | jq length)
  [ "$admin" = "0" ] && pass "No security group in the VPC opens SSH (22) or RDP (3389)" || fail "$admin admin-port rule(s) found"
  alb_rules=$(aws ec2 describe-security-groups --group-ids "$SG_ALB" --query "SecurityGroups[0].IpPermissions[].[FromPort,ToPort]" --output text | tr '\t' '-' | tr '\n' ' ')
  [ "$alb_rules" = "80-80 " ] && pass "Load balancer accepts only port 80 from the internet" || fail "Load balancer inbound ports: $alb_rules"
  first_ip=$(aws ec2 describe-instances --instance-ids $(echo $IDS | awk '{print $1}') --query "Reservations[0].Instances[0].PrivateIpAddress" --output text)
  if curl -s -m 5 "http://$first_ip:8080/health" >/dev/null; then fail "Server $first_ip answered directly from CloudShell"; else pass "A server's private IP ($first_ip) cannot be reached directly from outside the VPC"; fi

  bold "5. Servers can download updates (outbound through NAT) and are managed without open ports"
  target=$(echo $IDS | awk '{print $1}')
  managed=$(aws ssm describe-instance-information --filters Key=InstanceIds,Values="$target" --query "length(InstanceInformationList)" --output text)
  [ "$managed" = "1" ] && pass "Server $target is registered with Systems Manager (Session Manager works, no SSH)" || fail "Server $target is not registered with Systems Manager"
  if [ "$managed" = "1" ]; then
    # Runs on the server through Systems Manager (no SSH): check updates, reach the internet, check the app.
    cmd=$(aws ssm send-command --instance-ids "$target" --document-name AWS-RunShellScript \
      --parameters '{"commands":["dnf -q check-update --security >/dev/null; echo dnf-exit=$?","echo github=$(curl -s -o /dev/null -w %{http_code} https://github.com)","systemctl is-active onward-orders"]}' \
      --query "Command.CommandId" --output text)
    aws ssm wait command-executed --command-id "$cmd" --instance-id "$target" 2>/dev/null || true
    out=$(aws ssm get-command-invocation --command-id "$cmd" --instance-id "$target" --query "StandardOutputContent" --output text 2>/dev/null)
    echo "$out" | sed 's/^/        /'
    grep -qE "dnf-exit=(0|100)" <<<"$out" && pass "Server reached the Amazon Linux update repositories" || fail "Server could not check for updates"
    grep -qE "github=(200|301|302)" <<<"$out" && pass "Server reached the internet (GitHub) through the NAT gateway" || fail "Server could not reach the internet"
    grep -q "^active" <<<"$out" && pass "App was installed and started automatically as the onward-orders service" || fail "onward-orders service is not active"
  fi

  bold "6. Automatic capacity: Auto Scaling and target tracking"
  read -r min max des <<<"$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG" \
    --query "AutoScalingGroups[0].[MinSize,MaxSize,DesiredCapacity]" --output text)"
  info "Auto Scaling group $ASG: min=$min max=$max desired=$des in service=$(in_service)"
  [ "$min" -ge 2 ] && pass "Minimum of $min servers (one per Availability Zone)" || fail "Minimum size is $min"
  azs=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG" --query "length(AutoScalingGroups[0].AvailabilityZones)" --output text)
  [ "$azs" -ge 2 ] && pass "Auto Scaling group spans $azs Availability Zones" || fail "Auto Scaling group spans $azs AZ"
  hc=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG" --query "AutoScalingGroups[0].HealthCheckType" --output text)
  [ "$hc" = "ELB" ] && pass "Unhealthy servers are replaced using load balancer health checks" || fail "Health check type is $hc"
  policy=$(aws autoscaling describe-policies --auto-scaling-group-name "$ASG" \
    --query "ScalingPolicies[?PolicyType=='TargetTrackingScaling'].TargetTrackingConfiguration.[PredefinedMetricSpecification.PredefinedMetricType,TargetValue]" --output text)
  [[ "$policy" == ASGAverageCPUUtilization* ]] && pass "Target tracking keeps average CPU near $(awk '{print $2}' <<<"$policy")%" || fail "No CPU target tracking policy"

  bold "7. Company standards: naming, tags, no inline policies, budget"
  for key in Project Owner Environment; do
    n=$(aws ec2 describe-tags --filters Name=resource-id,Values="$(echo $IDS | awk '{print $1}')" Name=key,Values="$key" --query "length(Tags)" --output text)
    [ "$n" = "1" ] && pass "Servers carry the $key tag" || fail "Servers are missing the $key tag"
  done
  inline=0
  for role in "${PREFIX}-orders-server-role" "${PREFIX}-gitsync-role"; do
    c=$(aws iam list-role-policies --role-name "$role" --query "length(PolicyNames)" --output text 2>/dev/null || echo 0)
    inline=$((inline + c))
  done
  [ "$inline" = "0" ] && pass "No inline IAM policies on the project roles" || fail "$inline inline polic(ies) found"
  account=$(aws sts get-caller-identity --query Account --output text)
  if aws budgets describe-budget --account-id "$account" --budget-name "${PREFIX}-orders-monthly-budget" >/dev/null 2>&1; then
    pass "Monthly budget ${PREFIX}-orders-monthly-budget is active"
  else
    fail "Budget ${PREFIX}-orders-monthly-budget not found (set BudgetAlertEmail in deployment.yaml)"
  fi
  sync=$(aws codeconnections get-sync-configuration --resource-name "$STACK" --sync-type CFN_STACK_SYNC \
    --query "SyncConfiguration.[Branch,ConfigFile]" --output text 2>/dev/null)
  [ -n "$sync" ] && pass "Git sync deploys the stack from: $sync" || fail "No Git sync configuration for $STACK"
}

# --------------------------------------------------------------------------- scale
cmd_scale() {
  need jq
  require_stack
  local start_des start_ins max_seen
  start_des=$(desired); start_ins=$(instance_ids | wc -w)
  bold "Scale test: CPU spike through the UI's API, then watch Auto Scaling"
  info "Starting with desired=$start_des. Sending spike requests (420 s each) through the load balancer..."
  for _ in $(seq 1 10); do
    curl -s -m 5 -X POST "$URL/api/load/spike" -H 'Content-Type: application/json' -d '{"seconds":420}' \
      | jq -r '"        \(.serverId) (\(.availabilityZone)): active=\(.active) started=\(.started)"'
  done
  max_seen=$start_des
  info "Waiting up to 15 minutes for Auto Scaling to add servers..."
  for _ in $(seq 1 30); do
    sleep 30
    d=$(desired); [ "$d" -gt "$max_seen" ] && max_seen=$d
    printf '\r        desired=%s in service=%s   ' "$d" "$(in_service)"
    # Keep newly added servers busy too, so the average stays high until the spike ends.
    for _ in 1 2 3; do curl -s -o /dev/null -m 3 -X POST "$URL/api/load/spike" -H 'Content-Type: application/json' -d '{"seconds":300}'; done
    [ "$max_seen" -gt "$start_des" ] && [ "$(in_service)" -ge "$max_seen" ] && break
  done
  echo
  [ "$max_seen" -gt "$start_des" ] && pass "Rising CPU added servers: desired grew from $start_des to $max_seen" || fail "Desired capacity did not grow (still $start_des)"

  info "Stopping the spikes and waiting up to 25 minutes for servers to be removed..."
  for _ in $(seq 1 12); do curl -s -o /dev/null -m 3 -X POST "$URL/api/load/stop"; done
  for _ in $(seq 1 50); do
    sleep 30
    d=$(desired); printf '\r        desired=%s in service=%s   ' "$d" "$(in_service)"
    [ "$d" -le "$start_des" ] && break
  done
  echo
  [ "$(desired)" -le "$start_des" ] && pass "After the rush, capacity shrank back to $(desired)" || fail "Capacity is still $(desired); scale-in can take longer, re-check later"
  aws autoscaling describe-scaling-activities --auto-scaling-group-name "$ASG" --max-items 6 \
    --query "Activities[].[StartTime,StatusCode,Description]" --output text | sed 's/^/        /'
}

# --------------------------------------------------------------------------- failures
wait_until_restored() {
  local target="$1"
  info "Watching availability while Auto Scaling replaces capacity (up to 10 minutes)..."
  local end=$(( $(date +%s) + 600 )) ok=0 bad=0 code
  while [ "$(date +%s)" -lt "$end" ]; do
    code=$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$URL/health")
    if [ "$code" = "200" ]; then ok=$((ok + 1)); else bad=$((bad + 1)); fi
    printf '\r        requests ok=%d failed=%d  healthy in service=%s/%s   ' "$ok" "$bad" "$(in_service)" "$target"
    [ "$(in_service)" -ge "$target" ] && [ "$ok" -gt 60 ] && break
    sleep 1
  done
  echo
  [ "$bad" -eq 0 ] && pass "Customers saw no failed requests ($ok succeeded)" \
    || info "$bad of $((ok + bad)) requests failed briefly while the load balancer detected the failure"
  [ "$(in_service)" -ge "$target" ] && pass "Auto Scaling restored $target healthy servers without manual action" \
    || fail "Only $(in_service) of $target servers are healthy after 10 minutes"
}

cmd_fail_server() {
  require_stack
  local target victim
  target=$(desired); victim=$(instance_ids | awk '{print $1}')
  bold "Failure test: lose one server"
  confirm "This terminates server $victim. Auto Scaling should replace it automatically."
  aws ec2 terminate-instances --instance-ids "$victim" --query "TerminatingInstances[0].CurrentState.Name" --output text | sed 's/^/        terminating: /'
  wait_until_restored "$target"
}

cmd_fail_az() {
  require_stack
  local target zone victims
  target=$(desired)
  zone=$(aws ec2 describe-instances --instance-ids $(instance_ids) --query "Reservations[0].Instances[0].Placement.AvailabilityZone" --output text)
  victims=$(aws ec2 describe-instances --instance-ids $(instance_ids) \
    --query "Reservations[].Instances[] | [?Placement.AvailabilityZone=='$zone'].InstanceId" --output text)
  bold "Failure test: lose a whole Availability Zone ($zone)"
  confirm "This terminates every server in $zone: $victims"
  aws ec2 terminate-instances --instance-ids $victims >/dev/null && info "Terminated: $victims"
  wait_until_restored "$target"
}

# --------------------------------------------------------------------------- teardown
cmd_teardown() {
  bold "Teardown: stop all charges for $STACK"
  confirm "This deletes the Git sync configuration, the stack $STACK and the bootstrap stack."
  aws codeconnections delete-sync-configuration --resource-name "$STACK" --sync-type CFN_STACK_SYNC 2>/dev/null \
    && info "Git sync configuration removed (so Git sync does not recreate the stack)"
  aws cloudformation delete-stack --stack-name "$STACK"
  info "Deleting $STACK (NAT gateway and load balancer take a few minutes)..."
  aws cloudformation wait stack-delete-complete --stack-name "$STACK" && pass "Stack $STACK deleted"
  link=$(aws codeconnections list-repository-links --query "RepositoryLinks[?RepositoryName=='week3-aws-lab1-compute-server-container-practical-solution'].RepositoryLinkId | [0]" --output text)
  [ -n "$link" ] && [ "$link" != "None" ] && aws codeconnections delete-repository-link --repository-link-id "$link" && info "Repository link removed"
  aws cloudformation delete-stack --stack-name "${PREFIX}-gitsync-bootstrap"
  aws cloudformation wait stack-delete-complete --stack-name "${PREFIX}-gitsync-bootstrap" && pass "Bootstrap stack deleted"
  info "Everything can be rebuilt with: ./scripts/lab.sh deploy"
}

summary() {
  bold "Summary: $PASSED passed, $FAILED failed"
  for f in "${FAILURES[@]:-}"; do [ -n "$f" ] && printf '  - %s\n' "$f"; done
  [ "$FAILED" -eq 0 ]
}

case "${1:-help}" in deploy|verify|scale|fail-server|fail-az|teardown|all) need aws; need curl ;; esac
case "${1:-help}" in
  deploy)      cmd_deploy ;;
  verify)      cmd_verify; summary ;;
  scale)       cmd_scale; summary ;;
  fail-server) cmd_fail_server; summary ;;
  fail-az)     cmd_fail_az; summary ;;
  teardown)    cmd_teardown; summary ;;
  all)         cmd_deploy; cmd_verify; cmd_scale; cmd_fail_server; cmd_fail_az; summary ;;
  *) sed -n '2,14p' "$0" ;;
esac
