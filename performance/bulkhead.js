import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { baseUrl, body, params, requireHealthyApp, correctnessThresholds, singleRun } from './lib/common.js';

const rejected = new Counter('bulkhead_rejections');

export const options = {
  scenarios: { bulkhead: singleRun },
  batch: 6,
  batchPerHost: 6,
  thresholds: { ...correctnessThresholds, bulkhead_rejections: ['count>0'] },
};

export function setup() {
  requireHealthyApp();
}

export default function () {
  const requests = Array.from({ length: 6 }, () => ({
    method: 'GET',
    url: `${baseUrl}/api/demo/bulkhead?delayMs=3000`,
    params: params('bulkhead-overlap', [200, 503]),
  }));
  const responses = http.batch(requests);
  const acceptedCount = responses.filter((r) => r.status === 200).length;
  check(acceptedCount, { 'exactly two overlapping calls accepted': (n) => n === 2 });
  for (const response of responses) {
    rejected.add(response.status === 503 ? 1 : 0);
    check(response, {
      'bulkhead response matches success or full capacity': (r) =>
        (r.status === 200 && body(r).message === 'Simulated downstream service responded successfully') ||
        (r.status === 503 && body(r).message === 'Bulkhead is full; try again later'),
      'bulkhead rejects promptly': (r) => r.status !== 503 || r.timings.duration < 1000,
    });
  }
  const recovered = http.get(`${baseUrl}/api/demo/bulkhead?delayMs=0`, params('bulkhead-recovery'));
  check(recovered, { 'bulkhead releases capacity': (r) => r.status === 200 });
}
