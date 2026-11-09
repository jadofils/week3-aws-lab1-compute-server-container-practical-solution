#!/usr/bin/env bash
# Prints one line per request so downtime is visible during releases and failure tests.
# Usage: scripts/load/watch-availability.sh http://<alb-dns>
set -uo pipefail
URL="${1:?load balancer URL}"
ok=0; failed=0
trap 'echo; echo "ok=$ok failed=$failed"; exit' INT TERM
while true; do
  body=$(curl -s -m 3 -w ' %{http_code}' "$URL/health")
  code="${body##* }"
  server=$(printf '%s' "$body" | sed -n 's/.*"server":"\([^"]*\)".*/\1/p')
  if [ "$code" = "200" ]; then ok=$((ok+1)); else failed=$((failed+1)); fi
  printf '%s  %s  %s  (ok=%d failed=%d)\n' "$(date +%T)" "$code" "${server:-?}" "$ok" "$failed"
  sleep 0.5
done
