#!/usr/bin/env bash
# Fallback load generator using only curl, for machines without k6.
# Usage: scripts/load/generate-load.sh http://<alb-dns> [seconds=600] [parallel=40]
set -euo pipefail
URL="${1:?load balancer URL, e.g. http://onward-dev-orders-alb-123.eu-west-1.elb.amazonaws.com}"
DURATION="${2:-600}"
PARALLEL="${3:-40}"
END=$(( $(date +%s) + DURATION ))

echo "Sending load to $URL for ${DURATION}s with $PARALLEL parallel workers..."
worker() {
  while [ "$(date +%s)" -lt "$END" ]; do
    curl -s -o /dev/null -X POST "$URL/api/orders" -H 'Content-Type: application/json' \
      -d '{"customerName":"load","item":"Parcel delivery","quantity":1}'
    curl -s -o /dev/null "$URL/api/orders"
  done
}
for _ in $(seq 1 "$PARALLEL"); do worker & done
wait
echo "Load finished. Tasks scale back in after the CPU average stays below target."
