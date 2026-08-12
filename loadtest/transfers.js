/**
 * k6 load script: register/login, open two accounts, then hammer transfers.
 *
 * Run (with the API up on :8080):
 *   k6 run loadtest/transfers.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const transferDuration = new Trend('transfer_duration');

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    transfer_duration: ['p(95)<2000'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const suffix = `${Date.now()}_${Math.floor(Math.random() * 1e6)}`;
  const username = `load_${suffix}`;
  const password = 'Password123';

  let res = http.post(
    `${BASE}/api/v1/auth/register`,
    JSON.stringify({
      username,
      email: `${username}@finova.local`,
      fullName: 'Load Tester',
      password,
    }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'register 201': (r) => r.status === 201 });

  res = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  const login = res.json();
  const token = login.accessToken;

  const headers = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  const a1 = http.post(
    `${BASE}/api/v1/accounts`,
    JSON.stringify({ accountType: 'SAVINGS', initialDeposit: 100000 }),
    { headers },
  ).json();
  const a2 = http.post(
    `${BASE}/api/v1/accounts`,
    JSON.stringify({ accountType: 'CURRENT', initialDeposit: 0 }),
    { headers },
  ).json();

  return { token, from: a1.id, to: a2.id };
}

export default function (data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };
  const start = Date.now();
  const res = http.post(
    `${BASE}/api/v1/transfers`,
    JSON.stringify({
      fromAccountId: data.from,
      toAccountId: data.to,
      amount: 1.0,
      description: 'k6 load',
    }),
    { headers },
  );
  transferDuration.add(Date.now() - start);
  check(res, {
    'transfer ok or insufficient': (r) => r.status === 201 || r.status === 422,
  });
  sleep(0.2);
}
