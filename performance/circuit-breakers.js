import http from 'k6/http';
import { check, sleep } from 'k6';
import { baseUrl, body, params, requireHealthyApp, correctnessThresholds, singleRun } from './lib/common.js';

export const options = {
  scenarios: { isolation: singleRun },
  thresholds: correctnessThresholds,
};

export function setup() {
  requireHealthyApp();
}

function openCircuit(service) {
  for (let i = 0; i < 5; i++) {
    const response = http.get(`${baseUrl}/api/demo/${service}?fail=true`, params(`${service}-failure`, [503]));
    check(response, {
      'simulated failure or already-open circuit returns 503': (r) => r.status === 503 &&
        ['Simulated downstream service is unavailable', 'Circuit breaker is open; try again later'].includes(body(r).message),
    });
  }
  const blocked = http.get(`${baseUrl}/api/demo/${service}?fail=false`, params(`${service}-blocked`, [503]));
  check(blocked, {
    'open circuit blocks a healthy request': (r) =>
      r.status === 503 && body(r).message === 'Circuit breaker is open; try again later',
  });
}

function healthy(service) {
  const response = http.get(`${baseUrl}/api/demo/${service}?fail=false`, params(`${service}-healthy`));
  check(response, {
    'healthy downstream returns its own service name': (r) =>
      r.status === 200 && body(r).service === `${service}Service`,
  });
}

export default function () {
  openCircuit('inventory');
  healthy('payment');
  sleep(11);
  healthy('inventory');
  healthy('inventory');
  openCircuit('payment');
  healthy('inventory');
  const original = http.get(`${baseUrl}/api/demo/circuit-breaker?fail=false`, params('original-demo'));
  check(original, { 'original demo remains independent': (r) => r.status === 200 });
}
