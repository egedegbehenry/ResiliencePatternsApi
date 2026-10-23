import http from 'k6/http';
import { check, sleep } from 'k6';
import { baseUrl, body, params, requireHealthyApp, correctnessThresholds } from './lib/common.js';

export const options = {
  scenarios: {
    baseline: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 5),
      duration: __ENV.DURATION || '10s',
    },
  },
  thresholds: {
    ...correctnessThresholds,
    'http_req_duration{name:hello}': [`p(95)<${Number(__ENV.BASELINE_P95_MS || 500)}`],
  },
};

export function setup() {
  requireHealthyApp();
}

export default function () {
  const response = http.get(`${baseUrl}/api/demo/hello`, params('hello'));
  check(response, {
    'hello returns 200': (r) => r.status === 200,
    'hello returns expected message': (r) => body(r).message === 'Resilience API is running successfully',
  });
  sleep(0.1);
}
