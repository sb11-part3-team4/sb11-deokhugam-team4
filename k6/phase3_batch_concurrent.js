// k6/config/phase3_batch_concurrency.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {ENV, TEST_USERS} from './config/env.js';
import {getAuthHeaders} from './helpers/auth.js';
import {batchConcurrentOptions} from './config/options.js';

export const options = batchConcurrentOptions;

const PERIODS = ['DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME'];

function pickUser() {
  return TEST_USERS[exec.scenario.iterationInTest % TEST_USERS.length];
}

function safeJson(res) {
  try {
    return res.json();
  } catch (e) {
    return null;
  }
}

export default function () {
  const userId = pickUser();
  const headers = getAuthHeaders(userId);
  const period = PERIODS[exec.scenario.iterationInTest % PERIODS.length];

  // 배치가 갱신하는 랭킹 조회 (target:ranking)
  // 도서 랭킹
  let res = http.get(
      `${ENV.BASE_URL}/books/popular?period=${period}&direction=ASC&limit=10`,
      {headers, tags: {name: 'GET /books/popular', target: 'ranking'}}
  );
  check(res, {'popular books 200': (r) => r.status === 200});
  sleep(0.3);

  // 파워유저 랭킹
  res = http.get(
      `${ENV.BASE_URL}/users/power?period=${period}&direction=ASC&limit=10`,
      {headers, tags: {name: 'GET /users/power', target: 'ranking'}}
  );
  check(res, {'power users 200': (r) => r.status === 200});
  sleep(0.3);

  // 인기 리뷰 랭킹
  res = http.get(
      `${ENV.BASE_URL}/reviews/popular?period=${period}&direction=ASC&limit=10`,
      {headers, tags: {name: 'GET /reviews/popular', target: 'ranking'}}
  );
  check(res, {'popular reviews 200': (r) => r.status === 200});
  sleep(0.3);

  // 배치와 무관한 일반 조회 (대조군, target:normal)
  res = http.get(
      `${ENV.BASE_URL}/books?orderBy=title&direction=ASC&limit=20`,
      {headers, tags: {name: 'GET /books', target: 'normal'}}
  );
  check(res, {'books 200': (r) => r.status === 200});

  let bookId = null;
  const body = safeJson(res);
  if (body && body.content && body.content.length
      > 0) {
    bookId = body.content[0].id;
  }
  sleep(0.3);

  if (bookId) {
    res = http.get(
        `${ENV.BASE_URL}/reviews?bookId=${bookId}&orderBy=createdAt&direction=DESC&limit=20`,
        {headers, tags: {name: 'GET /reviews', target: 'normal'}}
    );
    check(res, {'reviews 200': (r) => r.status === 200});
  }

  sleep(1);
}