#!/usr/bin/env bash
# One-time setup of CloudFormation GitSync for the Onward orders infrastructure.
# After this script, every infrastructure change is deployed by merging to the
# tracked branch; nobody runs CloudFormation by hand.
#
# Usage:  ./bootstrap/bootstrap.sh <github-owner> <infrastructure-repo-name> [environment] [branch]
# Needs:  AWS CLI v2 signed in with short-lived admin credentials (for example IAM Identity Center),
#         AWS_REGION set (we use eu-west-1).
set -euo pipefail

OWNER="${1:?GitHub owner, e.g. jadofils}"
INFRA_REPO="${2:?infrastructure repository name, e.g. onward-orders-infrastructure}"
ENVIRONMENT="${3:-dev}"
BRANCH="${4:-main}"
REGION="${AWS_REGION:?set AWS_REGION, e.g. export AWS_REGION=eu-west-1}"
PREFIX="onward-${ENVIRONMENT}"
HERE="$(cd "$(dirname "$0")" && pwd)"

# Stacks in dependency order: "<stack-name-suffix>:<deployment-file>".
STACKS=(
  "network:1-network"
  "security-and-iam:2-security-and-iam"
  "vpc-endpoints:3-vpc-endpoints"
  "registry-and-artifacts:4-registry-and-artifacts"
  "load-balancer:5-load-balancer"
  "ecs-service:6-ecs-service"
  "delivery-pipeline:7-delivery-pipeline"
  "budget:8-budget"
)

echo "==> 1/4 Deploying GitSync bootstrap stack ${PREFIX}-gitsync-bootstrap"
aws cloudformation deploy \
  --stack-name "${PREFIX}-gitsync-bootstrap" \
  --template-file "${HERE}/0-gitsync-bootstrap.yaml" \
  --parameter-overrides "Environment=${ENVIRONMENT}" \
  --capabilities CAPABILITY_NAMED_IAM \
  --tags Project=onward "Owner=Jado Fils SEZIKEYE" "Environment=${ENVIRONMENT}"

output() {
  aws cloudformation describe-stacks --stack-name "${PREFIX}-gitsync-bootstrap" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text
}
CONNECTION_ARN="$(output ConnectionArn)"
ROLE_ARN="$(output GitSyncRoleArn)"

echo "==> 2/4 Waiting for the GitHub connection to be approved"
echo "    Open: https://console.aws.amazon.com/codesuite/settings/connections?region=${REGION}"
echo "    Choose '${PREFIX}-github' > Update pending connection, and approve the GitHub app."
until [ "$(aws codeconnections get-connection --connection-arn "$CONNECTION_ARN" \
          --query Connection.ConnectionStatus --output text)" = "AVAILABLE" ]; do
  sleep 15
done

echo "==> 3/4 Linking ${OWNER}/${INFRA_REPO}"
LINK_ID="$(aws codeconnections list-repository-links \
  --query "RepositoryLinks[?OwnerId=='${OWNER}' && RepositoryName=='${INFRA_REPO}'].RepositoryLinkId | [0]" --output text)"
if [ -z "$LINK_ID" ] || [ "$LINK_ID" = "None" ]; then
  LINK_ID="$(aws codeconnections create-repository-link --connection-arn "$CONNECTION_ARN" \
    --owner-id "$OWNER" --repository-name "$INFRA_REPO" \
    --query RepositoryLinkInfo.RepositoryLinkId --output text)"
fi

wait_for_stack() {
  local name="$1"
  echo "    waiting for ${name}..."
  until aws cloudformation describe-stacks --stack-name "$name" >/dev/null 2>&1; do sleep 15; done
  aws cloudformation wait stack-create-complete --stack-name "$name" 2>/dev/null \
    || aws cloudformation wait stack-update-complete --stack-name "$name"
}

echo "==> 4/4 Creating GitSync configurations in dependency order"
for entry in "${STACKS[@]}"; do
  suffix="${entry%%:*}"; file="${entry##*:}"
  stack="${PREFIX}-${suffix}"

  if [ "$suffix" = "ecs-service" ]; then
    REPO_NAME="${PREFIX}-orders"
    echo "    The ECS service needs a 'release' image in ECR (${REPO_NAME})."
    echo "    Set the app repository variables (AWS_REGION, AWS_GITHUB_ACTIONS_ROLE_ARN, ENVIRONMENT)"
    echo "    and push to its main branch (or run the workflow manually). Waiting for the image..."
    until aws ecr describe-images --repository-name "$REPO_NAME" --image-ids imageTag=release >/dev/null 2>&1; do
      sleep 30
    done
  fi

  if aws codeconnections get-sync-configuration --resource-name "$stack" --sync-type CFN_STACK_SYNC >/dev/null 2>&1; then
    echo "    sync for ${stack} already exists"
  else
    aws codeconnections create-sync-configuration \
      --resource-name "$stack" \
      --sync-type CFN_STACK_SYNC \
      --branch "$BRANCH" \
      --config-file "deployments/${ENVIRONMENT}-${file}.yaml" \
      --repository-link-id "$LINK_ID" \
      --role-arn "$ROLE_ARN" \
      --publish-deployment-status ENABLED \
      --trigger-resource-update-on ANY_CHANGE >/dev/null
    echo "    created sync for ${stack}"
  fi
  wait_for_stack "$stack"

  if [ "$suffix" = "security-and-iam" ]; then
    echo "    GitHub Actions role for the app repository variable AWS_GITHUB_ACTIONS_ROLE_ARN:"
    aws cloudformation describe-stacks --stack-name "$stack" \
      --query "Stacks[0].Outputs[?OutputKey=='GitHubActionsRoleArn'].OutputValue" --output text
  fi
done

echo "==> Done. Application URL:"
aws cloudformation describe-stacks --stack-name "${PREFIX}-load-balancer" \
  --query "Stacks[0].Outputs[?OutputKey=='ApplicationUrl'].OutputValue" --output text
