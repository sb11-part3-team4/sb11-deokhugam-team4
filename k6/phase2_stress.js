// k6/config/phase2_stress.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {ENV, TEST_USERS} from './config/env.js';
import {stressOptions} from './config/options.js';
import {getAuthHeaders} from './helpers/auth.js';
import {firstId} from './helpers/data.js';

export const options = stressOptions;

export default function () {
  const userId = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  // 도서 목록 조회
  const booksRes = http.get(
      `${ENV.BASE_URL}/books?orderBy=rating&direction=DESC&limit=20`,
      {headers, tags: {name: 'GET /books'}}
  );
  check(booksRes, {'[GET /books] 200': (r) => r.status === 200});
  sleep(0.3);

  // 도서 상세 + 리뷰 + 댓글
  const bookId = firstId(booksRes);
  if (bookId) {
    const bookRes = http.get(
        `${ENV.BASE_URL}/books/${bookId}`,
        {headers, tags: {name: 'GET /books/{id}'}}
    );
    check(bookRes, {'[GET /books/{id}] 200': (r) => r.status === 200});
    sleep(0.2);

    const reviewsRes = http.get(
        `${ENV.BASE_URL}/reviews?bookId=${bookId}&orderBy=createdAt&direction=DESC&limit=10`,
        {headers, tags: {name: 'GET /reviews'}}
    );
    check(reviewsRes, {'[GET /reviews] 200': (r) => r.status === 200});
    sleep(0.2);

    const reviewId = firstId(reviewsRes);
    if (reviewId) {
      const commentsRes = http.get(
          `${ENV.BASE_URL}/comments?reviewId=${reviewId}&limit=10`,
          {headers, tags: {name: 'GET /comments'}}
      );
      check(commentsRes, {'[GET /comments] 200': (r) => r.status === 200});
      sleep(0.2);

      const reviewDetailRes = http.get(
          `${ENV.BASE_URL}/reviews/${reviewId}`,
          {headers, tags: {name: 'GET /reviews/{id}'}}
      );
      check(reviewDetailRes,
          {'[GET /reviews/{id}] 200': (r) => r.status === 200});
      sleep(0.2);
    }
  }

  // 대시보드 3종
  const popularBooksRes = http.get(
      `${ENV.BASE_URL}/books/popular?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /books/popular'}}
  );
  check(popularBooksRes, {'[GET /books/popular] 200': (r) => r.status === 200});
  sleep(0.2);

  const popularReviewsRes = http.get(
      `${ENV.BASE_URL}/reviews/popular?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /reviews/popular'}}
  );
  check(popularReviewsRes,
      {'[GET /reviews/popular] 200': (r) => r.status === 200});
  sleep(0.2);

  const powerUsersRes = http.get(
      `${ENV.BASE_URL}/users/power?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /users/power'}}
  );
  check(powerUsersRes, {'[GET /users/power] 200': (r) => r.status === 200});
  sleep(0.2);

  // 알림 목록
  const notiRes = http.get(
      `${ENV.BASE_URL}/notifications?userId=${userId}&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /notifications'}}
  );
  check(notiRes, {'[GET /notifications] 200': (r) => r.status === 200});
  sleep(0.3);
}