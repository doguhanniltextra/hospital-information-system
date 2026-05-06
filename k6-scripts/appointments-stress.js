import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  scenarios: {
    constant_request_rate: {
      executor: 'constant-arrival-rate',
      rate: 100, // 100 requests per second
      timeUnit: '1s',
      duration: '30s', // Keep it to 30 seconds to simulate a quick burst 
      preAllocatedVUs: 100, // Pre-allocate enough VUs to handle the rate
      maxVUs: 500, // Hard limit to avoid blowing up memory locally
    },
  },
  setupTimeout: '5m',
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'], 
  },
};

const BASE_URL = 'http://localhost:4004';

export function setup() {
  const adminName = `admin_${randomString(8)}`;
  const password = 'Password123!';
  const headers = { 'Content-Type': 'application/json' };

  // 1. Register & Login
  http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
    name: adminName, email: `${adminName}@example.com`, password: password, registerDate: new Date().toISOString().split('T')[0]
  }), { headers });

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ name: adminName, password: password }), { headers });
  const token = JSON.parse(loginRes.body).accessToken;
  
  const authHeaders = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };

  // 2. Create 50 Patients
  const patientIds = [];
  for (let i = 0; i < 50; i++) {
    const patientRes = http.post(`${BASE_URL}/api/patients`, JSON.stringify({
        name: `John Doe ${randomString(5)}`,
        email: `${randomString(8)}@example.com`,
        address: "123 Health Street",
        dateOfBirth: "1980-01-01",
        registeredDate: new Date().toISOString().split('T')[0]
    }), { headers: authHeaders });

    if(patientRes.status === 200) {
        patientIds.push(JSON.parse(patientRes.body).id); 
    }
  }

  // 3. Create 50 Doctors
  const doctorIds = [];
  for (let i = 0; i < 50; i++) {
    const doctorRes = http.post(`${BASE_URL}/api/doctors`, JSON.stringify({
        name: `Dr. Smith ${randomString(5)}`,
        email: `${randomString(8)}@example.com`,
        number: "1234567890",
        specialization: "Cardiologist",
        yearsOfExperience: 10,
        hospitalName: "General Hospital",
        department: "Cardiology",
        licenseNumber: 999123 + i,
        available: true,
        patientCount: 0,
        getMaximumPatient: false
    }), { headers: authHeaders });

    if(doctorRes.status === 200) {
        doctorIds.push(JSON.parse(doctorRes.body).id); 
    }
  }

  if(patientIds.length === 0 || doctorIds.length === 0) {
    console.warn(`Failed to grab IDs! Patients=${patientIds.length}, Doctors=${doctorIds.length}`);
  }

  return { token, patientIds, doctorIds };
}

export default function (data) {
  if(!data.patientIds || data.patientIds.length === 0 || !data.doctorIds || data.doctorIds.length === 0) return; 

  // Randomly select a patient and a doctor
  const pIndex = Math.floor(Math.random() * data.patientIds.length);
  const dIndex = Math.floor(Math.random() * data.doctorIds.length);
  const selectedPatientId = data.patientIds[pIndex];
  const selectedDoctorId = data.doctorIds[dIndex];

  const url = `${BASE_URL}/api/appointments/`;
  const payload = JSON.stringify({
      patientId: selectedPatientId,
      doctorId: selectedDoctorId,
      serviceDate: "2026-10-10 10:00",
      serviceDateEnd: "2026-10-10 11:00",
      serviceType: "CONSULTATION",
      amount: 150.00,
      paymentStatus: false,
      paymentType: "CREDIT"
  });

  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json'
    }
  };

  const res = http.post(url, payload, params);

  check(res, {
    'appointment created 200': (r) => r.status === 200,
  });

}
