import http from 'k6/http';
import { fail } from 'k6';

export const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export function params(name, statuses = [200]) {
  return {
    tags: { name },
    responseCallback: http.expectedStatuses(...statuses),
    timeout: '5s',
    redirects: 0,
  };
}

export function body(response) {
  try {
    return JSON.parse(response.body);
  } catch (_) {
    return {};
  }
}

export function requireHealthyApp() {
  const response = http.get(`${baseUrl}/actuator/health`, params('health'));
  if (response.status !== 200 || body(response).status !== 'UP') {
    fail(`Application is not healthy at ${baseUrl}: HTTP ${response.status}`);
  }
}

export const correctnessThresholds = {
  checks: ['rate==1'],
  http_req_failed: ['rate==0'],
  http_reqs: ['count>0'],
};

export const singleRun = {
  executor: 'shared-iterations',
  vus: 1,
  iterations: 1,
  maxDuration: '60s',
};
