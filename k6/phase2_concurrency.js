// k6/config/phase2_concurrency.js

import http from 'k6/http';
import exec from 'k6/execution';
import {check} from 'k6';
import {CONCURRENCY_TARGET, ENV, TEST_USERS} from './config/env.js';
import {concurrencyOptions} from './config/options.js';
import {getAuthHeaders} from './helpers/auth.js';

export const options = concurrencyOptions;

// 좋아요 동시성
export function likeScenario() {
  // 시나리오 내 0,1,2... 순차 인덱스 → 유저 고유 배정
  const idx = exec.scenario.iterationInTest;
  const userId = TEST_USERS[idx % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  const res = http.post(
      `${ENV.BASE_URL}/reviews/${CONCURRENCY_TARGET.reviewId}/like`,
      null,
      {headers, tags: {name: 'POST /reviews/{id}/like'}}
  );
  if (res.status !== 200) {
    console.log(
        `[like 실패] idx=${idx} user=${userId} status=${res.status} body=${res.body}`);
  }
  check(res, {'[like] 200': (r) => r.status === 200});
}

// 댓글 동시성
export function commentScenario() {
  const idx = exec.scenario.iterationInTest;
  const userId = TEST_USERS[idx % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  const payload = JSON.stringify({
    reviewId: CONCURRENCY_TARGET.reviewId,
    userId: userId,
    content: `동시성 테스트 댓글 ${idx}`,
  });

  const res = http.post(
      `${ENV.BASE_URL}/comments`,
      payload,
      {headers, tags: {name: 'POST /comments'}}
  );
  check(res, {'[comment] 201': (r) => r.status === 200 || r.status === 201});
}

// ISBN
export function isbnScenario() {
  const userId = TEST_USERS[__ITER % TEST_USERS.length];
  const authHeaders = getAuthHeaders(userId, true);  // ← true! Content-Type 자동(멀티파트)

  const bookData = JSON.stringify({
    isbn: CONCURRENCY_TARGET.isbn,
    title: '동시성 테스트 도서',
    author: '테스트저자',
    description: '동시 등록 race condition 테스트',
    publisher: '테스트출판사',
    publishedDate: '2024-01-01',
  });

  const fd = {
    bookData: http.file(bookData, 'bookData.json', 'application/json'),
  };

  const res = http.post(`${ENV.BASE_URL}/books`, fd, {
    headers: authHeaders,
    tags: {name: 'POST /books (ISBN race)'},
  });

  if (res.status !== 201 && res.status !== 200 && res.status !== 409) {
    console.log(`[isbn 실패] status=${res.status}, body=${res.body}`);
  }
  check(res, {
    '[isbn] 성공/정상충돌': (r) => r.status === 201 || r.status === 200 || r.status
        === 409,
  });
}

// 1인 1리뷰 동시성
export function reviewScenario() {
  const userId = CONCURRENCY_TARGET.reviewRaceUserId;
  const headers = getAuthHeaders(userId);   // userId는 헤더로 전달

  const payload = JSON.stringify({
    bookId: CONCURRENCY_TARGET.reviewRaceBookId,
    rating: 5,
    content: `1인1리뷰 동시성 테스트 ${__ITER}`,
  });

  const res = http.post(
      `${ENV.BASE_URL}/reviews`,
      payload,
      {
        headers: {...headers, 'Content-Type': 'application/json'},
        tags: {name: 'POST /reviews (1인1리뷰 race)'},
      }
  );

  // 원인 파악용 임시 로그
  if (res.status !== 201 && res.status !== 200 && res.status !== 409) {
    console.log(`[review 실패] status=${res.status}, body=${res.body}`);
  }

  check(res, {
    '[review] 성공 또는 정상충돌(409)': (r) =>
        r.status === 201 || r.status === 200 || r.status === 409,
  });
}
