// k6/config/phase1_baseline.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {ENV, TEST_USERS} from './config/env.js';
import {baselineOptions} from './config/options.js';
import {getAuthHeaders} from './helpers/auth.js';
import {firstId} from './helpers/data.js';

export const options = baselineOptions;

export default function () {
  // 캐시 편향 방지를 위해 VU마다 다른 유저 사용
  const userId = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  // 도서 목록 조회
  const booksRes = http.get(
      `${ENV.BASE_URL}/books?orderBy=rating&direction=DESC&limit=20`,
      {headers, tags: {name: 'GET /books'}}
  );
  check(booksRes, {
    '[GET /books] 200': (r) => r.status === 200,
    '[GET /books] content 존재': (r) => r.json('content') !== undefined,
  });
  sleep(0.5);

  // 도서 상세 + 리뷰 + 댓글
  const bookId = firstId(booksRes);
  if (bookId) {
    // 도서 상세
    const bookRes = http.get(
        `${ENV.BASE_URL}/books/${bookId}`,
        {headers, tags: {name: 'GET /books/{id}'}}
    );
    check(bookRes, {'[GET /books/{id}] 200': (r) => r.status === 200});
    sleep(0.3);

    // 리뷰 목록
    const reviewsRes = http.get(
        `${ENV.BASE_URL}/reviews?bookId=${bookId}&orderBy=createdAt&direction=DESC&limit=10`,
        {headers, tags: {name: 'GET /reviews'}}
    );
    check(reviewsRes, {'[GET /reviews] 200': (r) => r.status === 200});
    sleep(0.3);

    // 댓글 목록
    const reviewId = firstId(reviewsRes);
    if (reviewId) {
      const commentsRes = http.get(
          `${ENV.BASE_URL}/comments?reviewId=${reviewId}&limit=10`,
          {headers, tags: {name: 'GET /comments'}}
      );
      check(commentsRes, {'[GET /comments] 200': (r) => r.status === 200});
      sleep(0.3);

      // 리뷰 상세
      const reviewDetailRes = http.get(
          `${ENV.BASE_URL}/reviews/${reviewId}`,
          {headers, tags: {name: 'GET /reviews/{id}'}}
      );
      check(reviewDetailRes,
          {'[GET /reviews/{id}] 200': (r) => r.status === 200});
      sleep(0.3);
    }
  }

  // 대시보드 3종
  const popularBooksRes = http.get(
      `${ENV.BASE_URL}/books/popular?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /books/popular'}}
  );
  check(popularBooksRes, {'[GET /books/popular] 200': (r) => r.status === 200});
  sleep(0.3);

  const popularReviewsRes = http.get(
      `${ENV.BASE_URL}/reviews/popular?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /reviews/popular'}}
  );
  check(popularReviewsRes,
      {'[GET /reviews/popular] 200': (r) => r.status === 200});
  sleep(0.3);

  const powerUsersRes = http.get(
      `${ENV.BASE_URL}/users/power?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /users/power'}}
  );
  check(powerUsersRes, {'[GET /users/power] 200': (r) => r.status === 200});
  sleep(0.3);

  // 알림 목록
  const notiRes = http.get(
      `${ENV.BASE_URL}/notifications?userId=${userId}&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /notifications'}}
  );
  check(notiRes, {'[GET /notifications] 200': (r) => r.status === 200});
  sleep(0.5);
}