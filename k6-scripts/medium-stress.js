import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { nextDaySlotIso } from './_slots.js';

/** Must match peak `target` VUs so each VU maps to one distinct identity. */
const USER_POOL = 50;

export const options = {
  setupTimeout: '4m',
  stages: [
    { duration: '15s', target: 50 },
    { duration: '30s', target: 50 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:4004';
/** Stay under gateway `/api/auth/**` limiter (~5 req/s replenish). */
const AUTH_PACING = 0.35;

export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  const users = [];

  for (let i = 0; i < USER_POOL; i++) {
    const username = `med_${randomString(10)}_${i}`;
    const password = 'Password123!';
    const body = JSON.stringify({
      name: username,
      email: `${username}@example.com`,
      password,
      registerDate: new Date().toISOString().split('T')[0],
    });

    http.post(`${BASE_URL}/api/auth/register`, body, { headers });
    sleep(AUTH_PACING);

    const loginRes = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ name: username, password }),
      { headers },
    );
    sleep(AUTH_PACING);

    let token = '';
    try {
      token = JSON.parse(loginRes.body).accessToken;
    } catch (e) {
      console.error(`medium-stress setup: login failed for ${username} status=${loginRes.status}`);
    }
    if (token) {
      users.push({ token, xUserId: username });
    }
  }

  if (users.length !== USER_POOL) {
    console.warn(`medium-stress setup: expected ${USER_POOL} users, got ${users.length}`);
  }

  const { start, end } = nextDaySlotIso();
  return { users, slotStart: start, slotEnd: end };
}

export default function (data) {
  if (!data.users.length) {
    console.error('medium-stress: no users from setup; aborting iteration');
    return;
  }
  const idx = (__VU - 1) % data.users.length;
  const user = data.users[idx];
  const params = {
    headers: {
      Authorization: `Bearer ${user.token}`,
      'Content-Type': 'application/json',
      'X-User-Id': user.xUserId,
    },
  };

  const qCon = `start=${encodeURIComponent(data.slotStart)}&end=${encodeURIComponent(data.slotEnd)}&serviceType=CONSULTATION`;
  const qVac = `start=${encodeURIComponent(data.slotStart)}&end=${encodeURIComponent(data.slotEnd)}&serviceType=VACCINATION`;

  check(http.get(`${BASE_URL}/api/appointments/doctor-options?${qCon}&page=0&size=20`, params), {
    'doctor-options consultation p0': (r) => r.status === 200,
  });
  check(http.get(`${BASE_URL}/api/appointments/doctor-options?${qCon}&page=1&size=20`, params), {
    'doctor-options consultation p1': (r) => r.status === 200,
  });
  check(http.get(`${BASE_URL}/api/appointments/doctor-options?${qVac}&page=0&size=20`, params), {
    'doctor-options vaccination p0': (r) => r.status === 200,
  });

  sleep(1);
}
