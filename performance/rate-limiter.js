import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { baseUrl, body, params, requireHealthyApp, correctnessThresholds, singleRun } from './lib/common.js';

const rejected = new Counter('rate_limit_rejections');
const accepted = new Counter('rate_limit_acceptances');

export const options = {
  scenarios: { rateLimit: singleRun },
  thresholds: {
    ...correctnessThresholds,
    rate_limit_rejections: ['count>0'],
    rate_limit_acceptances: ['count>0'],
  },
};

export function setup() {
  requireHealthyApp();
}

export default function () {
  const requestParams = params('rate-limit', [200, 429]);
  for (let i = 0; i < 20; i++) {
    const response = http.get(`${baseUrl}/api/demo/rate-limit`, requestParams);
    accepted.add(response.status === 200 ? 1 : 0);
    rejected.add(response.status === 429 ? 1 : 0);
    check(response, {
      'rate limiter returns success or explicit rejection': (r) =>
        (r.status === 200 && body(r).message === 'Simulated downstream service responded successfully') ||
        (r.status === 429 && body(r).message === 'Rate limit exceeded; try again later'),
      '429 includes Retry-After': (r) => r.status !== 429 || r.headers['Retry-After'] === '10',
      '429 is returned promptly': (r) => r.status !== 429 || r.timings.duration < 1000,
    });
  }
  // Wait longer than one complete refresh period before checking recovery.
  sleep(11);
  const recovered = http.get(`${baseUrl}/api/demo/rate-limit`, params('rate-limit-recovery'));
  check(recovered, { 'rate limiter recovers after refresh': (r) => r.status === 200 });
}
