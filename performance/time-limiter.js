import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { baseUrl, body, params, requireHealthyApp, correctnessThresholds, singleRun } from './lib/common.js';

const timeouts = new Counter('expected_timeouts');

export const options = {
  scenarios: { timeLimit: singleRun },
  batch: 4,
  batchPerHost: 4,
  thresholds: {
    ...correctnessThresholds,
    expected_timeouts: ['count==4'],
    'http_req_duration{name:time-limit-slow}': ['p(95)<2000'],
  },
};

export function setup() {
  requireHealthyApp();
}

export default function () {
  const responses = http.batch(Array.from({ length: 4 }, () => ({
    method: 'GET',
    url: `${baseUrl}/api/demo/time-limit?delayMs=2000`,
    params: params('time-limit-slow', [504]),
  })));
  for (const response of responses) {
    timeouts.add(response.status === 504 ? 1 : 0);
    check(response, {
      'slow call returns explicit timeout': (r) =>
        r.status === 504 && body(r).message === 'Downstream service timed out',
    });
  }
  const recovered = http.get(`${baseUrl}/api/demo/time-limit?delayMs=0`, params('time-limit-recovery'));
  check(recovered, { 'fast call works after cancellations': (r) => r.status === 200 });
}
