// k6/config/phase2_soak.js

import http from 'k6/http';
import {check, group, sleep} from 'k6';
import exec from 'k6/execution';
import {ENV, TEST_USERS} from './config/env.js';
import {getAuthHeaders} from './helpers/auth.js';
import {soakOptions} from './config/options.js';

export const options = soakOptions;

const PERIODS = ['DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME'];
const KEYWORDS = ['소설', '스프링', '원피스', '클린', '데이터'];

// 에러 발생 시에만 로그를 찍는 헬퍼 함수
function logIfError(res, apiName) {
  if (res.status !== 200 && res.status !== 201) {
    console.error(
        `[ERROR] ${apiName} | Status: ${res.status} | Body: ${res.body}`);
  }
}

function pickUser() {
  return TEST_USERS[exec.scenario.iterationInTest % TEST_USERS.length];
}

// 안전한 JSON 파싱
function safeJson(res) {
  try {
    return res.json();
  } catch (e) {
    return null;
  }
}

// 읽기 위주 흐름 (대부분의 VU)
export function readFlow() {
  const userId = pickUser();
  const headers = getAuthHeaders(userId);

  group('read', () => {
    // 인기 도서
    const period = PERIODS[exec.scenario.iterationInTest % PERIODS.length];
    let res = http.get(
        `${ENV.BASE_URL}/books/popular?period=${period}&direction=ASC&limit=10`,
        {headers, tags: {flow: 'read', name: 'GET /books/popular'}}
    );
    check(res, {'popular 200': (r) => r.status === 200});
    logIfError(res, 'GET /books/popular'); // 👈 여기에 추가!
    sleep(1);

    // 도서 목록
    res = http.get(
        `${ENV.BASE_URL}/books?orderBy=title&direction=ASC&limit=20`,
        {headers, tags: {flow: 'read', name: 'GET /books'}}
    );
    check(res, {'books 200': (r) => r.status === 200});
    logIfError(res, 'GET /books'); // 👈 여기에 추가!

    let bookId = null;
    const body = safeJson(res);
    if (body && body.content && body.content.length
        > 0) {
      bookId = body.content[0].id;
    }
    sleep(1);

    // 도서 상세 + 리뷰 목록
    if (bookId) {
      res = http.get(
          `${ENV.BASE_URL}/books/${bookId}`,
          {headers, tags: {flow: 'read', name: 'GET /books/{id}'}}
      );
      check(res, {'detail 200': (r) => r.status === 200});
      logIfError(res, 'GET /books/{id}'); // 👈 여기에 추가!
      sleep(1);

      res = http.get(
          `${ENV.BASE_URL}/reviews?bookId=${bookId}&orderBy=createdAt&direction=DESC&limit=20`,
          {headers, tags: {flow: 'read', name: 'GET /reviews (by book)'}}
      );
      check(res, {'reviews 200': (r) => r.status === 200});
      logIfError(res, 'GET /reviews'); // 👈 여기에 추가!
    }

    // 알림 조회
    sleep(1);
    res = http.get(
        `${ENV.BASE_URL}/notifications?userId=${userId}&limit=20`,
        {headers, tags: {flow: 'read', name: 'GET /notifications'}}
    );
    check(res, {'notifications 200': (r) => r.status === 200});
    logIfError(res, 'GET /notifications'); // 👈 여기에 추가!
  });

  sleep(2);
}

//소량 쓰기 흐름
export function writeFlow() {
  const userId = pickUser();
  const headers = getAuthHeaders(userId);

  group('write', () => {
    // 검색 → 리뷰 목록 → 좋아요 → 댓글
    const kw = KEYWORDS[exec.scenario.iterationInTest % KEYWORDS.length];
    let res = http.get(
        `${ENV.BASE_URL}/books?keyword=${encodeURIComponent(
            kw)}&orderBy=title&direction=ASC&limit=20`,
        {headers, tags: {flow: 'write', name: 'GET /books?keyword'}}
    );
    check(res, {'search 200': (r) => r.status === 200});
    logIfError(res, 'GET /books?keyword'); // 👈 여기에 추가!
    sleep(1);

    res = http.get(
        `${ENV.BASE_URL}/reviews?orderBy=createdAt&direction=DESC&limit=20`,
        {headers, tags: {flow: 'write', name: 'GET /reviews'}}
    );
    let targetReviewId = null;
    const body = safeJson(res);
    if (body && body.content && body.content.length > 0) {
      const others = body.content.filter((r) => r.userId !== userId);
      targetReviewId = (others.length > 0 ? others[0] : body.content[0]).id;
    }

    if (targetReviewId) {
      sleep(1);
      // 좋아요 (토글이라 다음 반복에서 취소되며 누적 안 됨)
      res = http.post(
          `${ENV.BASE_URL}/reviews/${targetReviewId}/like`,
          null,
          {headers, tags: {flow: 'write', name: 'POST /reviews/{id}/like'}}
      );
      check(res, {'like 200': (r) => r.status === 200});
      logIfError(res, 'POST /reviews/{id}/like');
      sleep(1);

      // 댓글
      const payload = JSON.stringify({
        reviewId: targetReviewId,
        userId: userId,
        content: `soak 댓글 ${exec.scenario.iterationInTest}`,
      });
      res = http.post(
          `${ENV.BASE_URL}/comments`,
          payload,
          {headers, tags: {flow: 'write', name: 'POST /comments'}}
      );
      check(res, {'comment ok': (r) => r.status === 200 || r.status === 201});
      logIfError(res, 'POST /comments');
    }
  });

  sleep(3);
}