// k6/config/phase2_cache.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {ENV, TEST_USERS} from './config/env.js';
import {getAuthHeaders} from './helpers/auth.js';

export const options = {
  stages: [
    {duration: '30s', target: 50}, // 워밍업 + 캐시 적재(첫 미스)
    {duration: '2m', target: 50},  // 정상 부하 (캐시 히트 구간)
    {duration: '30s', target: 0},
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:books popular}': ['p(95)<1000'],
    'http_req_duration{name:users power}': ['p(95)<1000'],
    'http_req_duration{name:reviews popular}': ['p(95)<1000'],
  },
};

const PERIODS = ['DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME'];

export default function () {
  const userId = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const headers = getAuthHeaders(userId);
  const period = PERIODS[__ITER % PERIODS.length];

  // 인기 도서
  const booksRes = http.get(
      `${ENV.BASE_URL}/books/popular?period=${period}&direction=ASC&limit=10`,
      {headers, tags: {name: 'books popular'}}
  );
  check(booksRes, {'books popular 200': (r) => r.status === 200});
  sleep(0.3);

  // 파워 유저
  const usersRes = http.get(
      `${ENV.BASE_URL}/users/power?period=${period}&direction=ASC&limit=10`,
      {headers, tags: {name: 'users power'}}
  );
  check(usersRes, {'users power 200': (r) => r.status === 200});
  sleep(0.3);

  // 인기 리뷰
  const reviewsRes = http.get(
      `${ENV.BASE_URL}/reviews/popular?period=${period}&direction=ASC&limit=10`,
      {headers, tags: {name: 'reviews popular'}}
  );
  check(reviewsRes, {'reviews popular 200': (r) => r.status === 200});
  sleep(0.3);
}