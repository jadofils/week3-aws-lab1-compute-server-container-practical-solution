#!/usr/bin/env bash
# One-time setup of CloudFormation Git sync for the order system.
# After this, every infrastructure change is a merge to main; nobody runs CloudFormation by hand.
#
# Usage:  AWS_REGION=eu-west-1 ./infrastructure/bootstrap/setup-gitsync.sh [github-owner] [repository] [environment] [branch]
# Needs:  AWS CLI v2 signed in with short-lived admin credentials (for example IAM Identity Center),
#         and the app release (v0.1.0) already published by the GitHub Actions workflow.
set -euo pipefail

OWNER="${1:-jadofils}"
REPO="${2:-week3-aws-lab1-compute-server-container-practical-solution}"
ENVIRONMENT="${3:-dev}"
BRANCH="${4:-main}"
REGION="${AWS_REGION:?set AWS_REGION, e.g. export AWS_REGION=eu-west-1}"
PREFIX="onward-${ENVIRONMENT}"
STACK="${PREFIX}-orders"
HERE="$(cd "$(dirname "$0")" && pwd)"

echo "==> 1/4 Deploying the Git sync bootstrap stack (${PREFIX}-gitsync-bootstrap)"
aws cloudformation deploy \
  --stack-name "${PREFIX}-gitsync-bootstrap" \
  --template-file "${HERE}/gitsync-bootstrap.yaml" \
  --parameter-overrides "Environment=${ENVIRONMENT}" \
  --capabilities CAPABILITY_NAMED_IAM \
  --tags Project=onward "Owner=Jado Fils SEZIKEYE" "Environment=${ENVIRONMENT}"

output() {
  aws cloudformation describe-stacks --stack-name "${PREFIX}-gitsync-bootstrap" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text
}
CONNECTION_ARN="$(output ConnectionArn)"
ROLE_ARN="$(output GitSyncRoleArn)"

echo "==> 2/4 Approve the GitHub connection (one click, required by AWS)"
echo "    https://console.aws.amazon.com/codesuite/settings/connections?region=${REGION}"
echo "    Choose '${PREFIX}-github' > Update pending connection > approve the GitHub app."
until [ "$(aws codeconnections get-connection --connection-arn "$CONNECTION_ARN" \
          --query Connection.ConnectionStatus --output text)" = "AVAILABLE" ]; do
  sleep 15
done

echo "==> 3/4 Linking ${OWNER}/${REPO}"
LINK_ID="$(aws codeconnections list-repository-links \
  --query "RepositoryLinks[?OwnerId=='${OWNER}' && RepositoryName=='${REPO}'].RepositoryLinkId | [0]" --output text)"
if [ -z "$LINK_ID" ] || [ "$LINK_ID" = "None" ]; then
  LINK_ID="$(aws codeconnections create-repository-link --connection-arn "$CONNECTION_ARN" \
    --owner-id "$OWNER" --repository-name "$REPO" \
    --query RepositoryLinkInfo.RepositoryLinkId --output text)"
fi

echo "==> 4/4 Creating the Git sync configuration for stack ${STACK}"
if aws codeconnections get-sync-configuration --resource-name "$STACK" --sync-type CFN_STACK_SYNC >/dev/null 2>&1; then
  echo "    already configured"
else
  aws codeconnections create-sync-configuration \
    --resource-name "$STACK" \
    --sync-type CFN_STACK_SYNC \
    --branch "$BRANCH" \
    --config-file infrastructure/deployment.yaml \
    --repository-link-id "$LINK_ID" \
    --role-arn "$ROLE_ARN" \
    --publish-deployment-status ENABLED \
    --trigger-resource-update-on FILE_CHANGE >/dev/null
fi

echo "    Waiting for ${STACK} (about 10 minutes: NAT gateway, load balancer, servers booting)..."
until aws cloudformation describe-stacks --stack-name "$STACK" >/dev/null 2>&1; do sleep 15; done
aws cloudformation wait stack-create-complete --stack-name "$STACK" 2>/dev/null \
  || aws cloudformation wait stack-update-complete --stack-name "$STACK"

echo "==> Done. Application URL:"
aws cloudformation describe-stacks --stack-name "$STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='ApplicationUrl'].OutputValue" --output text
