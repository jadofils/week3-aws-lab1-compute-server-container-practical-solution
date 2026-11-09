#!/usr/bin/env bash
# Prints one line per request: time, HTTP status, which server and zone answered.
# Keep it running during scaling, server-failure and zone-failure tests to show no downtime.
# Usage: scripts/watch-availability.sh http://<alb-dns>
set -uo pipefail
URL="${1:?load balancer URL, e.g. http://onward-dev-orders-alb-123.eu-west-1.elb.amazonaws.com}"
ok=0; failed=0
trap 'echo; echo "ok=$ok failed=$failed"; exit' INT TERM
while true; do
  body=$(curl -s -m 3 -w ' %{http_code}' "$URL/health")
  code="${body##* }"
  server=$(printf '%s' "$body" | sed -n 's/.*"server":"\([^"]*\)".*/\1/p')
  zone=$(printf '%s' "$body" | sed -n 's/.*"availabilityZone":"\([^"]*\)".*/\1/p')
  if [ "$code" = "200" ]; then ok=$((ok+1)); else failed=$((failed+1)); fi
  printf '%s  %s  %-20s %-12s (ok=%d failed=%d)\n' "$(date +%T)" "$code" "${server:--}" "${zone:--}" "$ok" "$failed"
  sleep 0.5
done
