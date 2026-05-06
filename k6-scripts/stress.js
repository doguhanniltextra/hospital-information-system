import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { nextDaySlotIso } from './_slots.js';

// Base API URL
const BASE_URL = 'http://localhost:4004';

// Test setup runs ONCE at the beginning of the test
export function setup() {
  const username = `testuser_${randomString(8)}`;
  const email = `${username}@example.com`;
  const password = 'Password123!';

  // 1. Register a user
  const registerPayload = JSON.stringify({
    name: username,
    email: email,
    password: password,
    registerDate: new Date().toISOString().split('T')[0]
  });

  const headers = { 'Content-Type': 'application/json' };
  
  const registerRes = http.post(`${BASE_URL}/api/auth/register`, registerPayload, { headers });
  
  // If user already exists or similar issue, try to login anyway.
  
  // 2. Login
  const loginPayload = JSON.stringify({
    name: username,
    password: password
  });

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, { headers });
  
  // Extract token
  let token = 'placeholder_token';
  try {
     const body = JSON.parse(loginRes.body);
     token = body.accessToken;
  } catch (e) {
     console.error("Failed to parse token from login response", loginRes.body);
  }

  const { start, end } = nextDaySlotIso();
  return { token, slotStart: start, slotEnd: end };
}

// User Journey
export default function (data) {
  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json'
    }
  };

  const qCon = `start=${encodeURIComponent(data.slotStart)}&end=${encodeURIComponent(data.slotEnd)}&serviceType=CONSULTATION`;
  const qVac = `start=${encodeURIComponent(data.slotStart)}&end=${encodeURIComponent(data.slotEnd)}&serviceType=VACCINATION`;

  check(http.get(`${BASE_URL}/api/appointments/doctor-options?${qCon}&page=0&size=20`, params), { 'doctor-options consultation p0': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/api/appointments/doctor-options?${qCon}&page=1&size=20`, params), { 'doctor-options consultation p1': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/api/appointments/doctor-options?${qVac}&page=0&size=20`, params), { 'doctor-options vaccination p0': (r) => r.status === 200 });

  // Small sleep to simulate realistic user action
  sleep(1);
}
