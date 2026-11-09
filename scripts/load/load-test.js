// k6 load test that drives ECS target-tracking scale-out, then lets it scale back in.
// Usage: k6 run -e URL=http://<alb-dns> scripts/load/load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.URL;
if (!BASE) {
  throw new Error('Set the load balancer address: k6 run -e URL=http://<alb-dns> load-test.js');
}

export const options = {
  stages: [
    { duration: '2m', target: 50 },   // ramp up
    { duration: '8m', target: 150 },  // sustained peak (about 3x normal) - watch tasks scale out
    { duration: '1m', target: 0 },    // stop - watch tasks scale back in over the next minutes
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],   // under 1% errors while scaling
  },
};

export default function () {
  const create = http.post(`${BASE}/api/orders`, JSON.stringify({
    customerName: `load-${__VU}`,
    item: 'Parcel delivery',
    quantity: 1 + (__ITER % 5),
  }), { headers: { 'Content-Type': 'application/json' } });
  check(create, { 'order created': (r) => r.status === 200 });

  const list = http.get(`${BASE}/api/orders`);
  check(list, { 'orders listed': (r) => r.status === 200 });

  sleep(0.2);
}
