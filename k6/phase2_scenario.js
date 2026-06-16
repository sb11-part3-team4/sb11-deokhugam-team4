// k6/config/phase2_scenario.js

import http from 'k6/http';
import {check, group, sleep} from 'k6';
import exec from 'k6/execution';
import {ENV, TEST_USERS} from './config/env.js';
import {ScenarioOptions} from "./config/options.js";
import {getAuthHeaders} from './helpers/auth.js';

export const options = ScenarioOptions;

const PERIODS = ['DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME'];
const KEYWORDS = ['소설', '스프링', '원피스', '클린', '데이터'];

function pickUser() {
  // VU·반복마다 다른 유저가 돌도록 분산
  return TEST_USERS[(exec.scenario.iterationInTest) % TEST_USERS.length];
}

// 신규 방문자 읽기 흐름
export function browseFlow() {
  const userId = pickUser();
  const headers = getAuthHeaders(userId);

  group('browse', () => {
    // 1) 홈 = 인기 도서
    const period = PERIODS[exec.scenario.iterationInTest % PERIODS.length];
    let res = http.get(
        `${ENV.BASE_URL}/books/popular?period=${period}&direction=ASC&limit=10`,
        {headers, tags: {flow: 'browse', name: 'GET /books/popular'}}
    );
    check(res, {'popular 200': (r) => r.status === 200});
    sleep(1);

    // 2) 도서 목록 (제목 정렬 1페이지)
    res = http.get(
        `${ENV.BASE_URL}/books?orderBy=title&direction=ASC&limit=20`,
        {headers, tags: {flow: 'browse', name: 'GET /books'}}
    );
    check(res, {'books 200': (r) => r.status === 200});

    let firstBookId = null;
    try {
      const body = res.json();
      if (body.content && body.content.length > 0) {
        firstBookId = body.content[0].id;
      }
    } catch (e) {
    }
    sleep(1);

    // 3) 도서 상세
    if (firstBookId) {
      res = http.get(
          `${ENV.BASE_URL}/books/${firstBookId}`,
          {headers, tags: {flow: 'browse', name: 'GET /books/{id}'}}
      );
      check(res, {'book detail 200': (r) => r.status === 200});
      sleep(1);

      // 4) 그 책의 리뷰 목록
      res = http.get(
          `${ENV.BASE_URL}/reviews?bookId=${firstBookId}&orderBy=createdAt&direction=DESC&limit=20`,
          {headers, tags: {flow: 'browse', name: 'GET /reviews (by book)'}}
      );
      check(res, {'reviews 200': (r) => r.status === 200});

      // 5) 리뷰 상세 (있으면)
      let firstReviewId = null;
      try {
        const body = res.json();
        if (body.content && body.content.length > 0) {
          firstReviewId = body.content[0].id;
        }
      } catch (e) {
      }

      if (firstReviewId) {
        sleep(1);
        res = http.get(
            `${ENV.BASE_URL}/reviews/${firstReviewId}`,
            {headers, tags: {flow: 'browse', name: 'GET /reviews/{id}'}}
        );
        check(res, {'review detail 200': (r) => r.status === 200});
      }
    }
  });

  sleep(2); // 다음 방문까지 think time
}

// 대시보드 탐색 흐름
export function dashboardFlow() {
  const userId = pickUser();
  const headers = getAuthHeaders(userId);
  const period = PERIODS[exec.scenario.iterationInTest % PERIODS.length];

  group('dashboard', () => {
    let res = http.get(
        `${ENV.BASE_URL}/books/popular?period=${period}&direction=ASC&limit=10`,
        {headers, tags: {flow: 'dashboard', name: 'GET /books/popular'}}
    );
    check(res, {'popular books 200': (r) => r.status === 200});
    sleep(0.5);

    res = http.get(
        `${ENV.BASE_URL}/reviews/popular?period=${period}&direction=ASC&limit=10`,
        {headers, tags: {flow: 'dashboard', name: 'GET /reviews/popular'}}
    );
    check(res, {'popular reviews 200': (r) => r.status === 200});
    sleep(0.5);

    res = http.get(
        `${ENV.BASE_URL}/users/power?period=${period}&direction=ASC&limit=10`,
        {headers, tags: {flow: 'dashboard', name: 'GET /users/power'}}
    );
    check(res, {'power users 200': (r) => r.status === 200});
  });

  sleep(2);
}

// 활동 유저 쓰기 흐름
export function activeFlow() {
  const userId = pickUser();
  const headers = getAuthHeaders(userId);

  group('active', () => {
    // 1) 검색
    const kw = KEYWORDS[exec.scenario.iterationInTest % KEYWORDS.length];
    let res = http.get(
        `${ENV.BASE_URL}/books?keyword=${encodeURIComponent(
            kw)}&orderBy=title&direction=ASC&limit=20`,
        {headers, tags: {flow: 'active', name: 'GET /books?keyword'}}
    );
    check(res, {'search 200': (r) => r.status === 200});

    let bookId = null;
    try {
      const body = res.json();
      if (body.content && body.content.length > 0) {
        bookId = body.content[0].id;
      }
    } catch (e) { /* ignore */
    }
    sleep(1);

    if (bookId) {
      // 2) 상세
      res = http.get(
          `${ENV.BASE_URL}/books/${bookId}`,
          {headers, tags: {flow: 'active', name: 'GET /books/{id}'}}
      );
      check(res, {'detail 200': (r) => r.status === 200});
      sleep(1);

      // 3) 리뷰 작성
      const reviewPayload = JSON.stringify({
        bookId: bookId,
        rating: 4,
        content: `시나리오 활동 유저 리뷰 ${exec.scenario.iterationInTest}`,
      });
      res = http.post(
          `${ENV.BASE_URL}/reviews`,
          reviewPayload,
          {
            headers: {...headers, 'Content-Type': 'application/json'},
            tags: {flow: 'active', name: 'POST /reviews'}
          }
      );
      // 201 신규 or 409/400 중복 모두 정상으로 간주
      check(res, {
        'review write ok': (r) => r.status === 201 || r.status === 200
            || r.status === 409 || r.status === 400
      });
      sleep(1);
    }

    // 4) 리뷰 목록에서 하나 골라 좋아요 + 댓글
    res = http.get(
        `${ENV.BASE_URL}/reviews?orderBy=createdAt&direction=DESC&limit=20`,
        {headers, tags: {flow: 'active', name: 'GET /reviews'}}
    );
    let targetReviewId = null;
    try {
      const body = res.json();
      if (body.content && body.content.length > 0) {
        // 본인 리뷰가 아닌 것 우선
        const others = body.content.filter((r) => r.userId !== userId);
        targetReviewId = (others.length > 0 ? others[0] : body.content[0]).id;
      }
    } catch (e) { // ignore
    }

    if (targetReviewId) {
      sleep(1);
      // 5) 좋아요
      res = http.post(
          `${ENV.BASE_URL}/reviews/${targetReviewId}/like`,
          null,
          {headers, tags: {flow: 'active', name: 'POST /reviews/{id}/like'}}
      );
      check(res, {'like 200': (r) => r.status === 200});
      sleep(1);

      // 6) 댓글
      const commentPayload = JSON.stringify({
        reviewId: targetReviewId,
        userId: userId,
        content: `시나리오 댓글 ${exec.scenario.iterationInTest}`,
      });
      res = http.post(
          `${ENV.BASE_URL}/comments`,
          commentPayload,
          {headers, tags: {flow: 'active', name: 'POST /comments'}}
      );
      check(res, {'comment ok': (r) => r.status === 200 || r.status === 201});
    }
  });

  sleep(2);
}