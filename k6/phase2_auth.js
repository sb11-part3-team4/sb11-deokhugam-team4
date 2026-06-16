// k6/config/phase2_auth.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {ENV} from './config/env.js';
import {authOptions} from './config/options.js';

export const options = authOptions;

// 로그인 측정용 계정을 미리 1개 생성하고 자격증명을 반환
export function setup() {
  const email = `logintest_${Date.now()}@deokhugam.com`;
  const password = 'deokhugam1!';

  const res = http.post(
      `${ENV.BASE_URL}/users`,
      JSON.stringify({
        email: email,
        nickname: `로그인테스트`,
        password: password,
      }),
      {headers: {'Content-Type': 'application/json'}}
  );

  const ok = res.status === 201 || res.status === 200;
  console.log(
      `[setup] 로그인 계정 생성 ${ok ? '성공' : '실패'}: ${email} (status=${res.status})`);
  if (!ok) {
    console.log(`[setup] body=${res.body}`);
  }

  return {loginEmail: email, loginPassword: password};
}

// 회원가입
export function signupScenario() {
  const unique = `${__VU}_${exec.scenario.iterationInTest}_${Date.now()}`;

  const payload = JSON.stringify({
    email: `loadtest_${unique}@deokhugam.com`,
    nickname: `부하_${exec.scenario.iterationInTest}`.substring(0, 20),
    password: 'deokhugam1!',
  });

  const res = http.post(
      `${ENV.BASE_URL}/users`,
      payload,
      {
        headers: {'Content-Type': 'application/json'},
        tags: {name: 'POST /users (signup)'}
      }
  );

  const ok = check(res, {
    'signup 성공 또는 중복': (r) => r.status === 201 || r.status === 200 || r.status
        === 409,
  });
  if (!ok) {
    console.log(`[signup 실패] status=${res.status} body=${res.body}`);
  }

  sleep(1);
}

// 로그인
export function loginScenario(data) {
  const payload = JSON.stringify({
    email: data.loginEmail,
    password: data.loginPassword,
  });

  const res = http.post(
      `${ENV.BASE_URL}/users/login`,
      payload,
      {
        headers: {'Content-Type': 'application/json'},
        tags: {name: 'POST /users/login'}
      }
  );

  const ok = check(res, {
    'login 200': (r) => r.status === 200,
  });
  if (!ok) {
    console.log(
        `[login 실패] email=${data.loginEmail} status=${res.status} body=${res.body}`);
  }

  sleep(1);
}