// k6/config/phase1_smoke.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {ENV, TEST_USERS} from './config/env.js';
import {smokeOptions} from './config/options.js';
import {getAuthHeaders} from './helpers/auth.js';

export const options = smokeOptions;

export default function () {
  // VU 번호에 따라 유저 순환
  const userId = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  // 도서 목록 조회
  const booksRes = http.get(
      `${ENV.BASE_URL}/books?orderBy=rating&limit=10`,
      {headers, tags: {name: 'GET /books'}}
  );
  check(booksRes, {
    '[GET /books] status 200': (r) => r.status === 200,
    '[GET /books] content 존재': (r) => r.json('content') !== undefined,
  });
  sleep(1);

  // 도서 상세 + 리뷰 조회
  const content = booksRes.json('content');
  if (content && content.length > 0) {
    const bookId = content[0].id;

    // 도서 상세
    const bookRes = http.get(
        `${ENV.BASE_URL}/books/${bookId}`,
        {headers, tags: {name: 'GET /books/{id}'}}
    );
    check(bookRes, {
      '[GET /books/{id}] status 200': (r) => r.status === 200,
    });
    sleep(1);

    // 리뷰 목록
    const reviewsRes = http.get(
        `${ENV.BASE_URL}/reviews?bookId=${bookId}&orderBy=createdAt&limit=10`,
        {headers, tags: {name: 'GET /reviews'}}
    );
    check(reviewsRes, {
      '[GET /reviews] status 200': (r) => r.status === 200,
    });
    sleep(1);

    // 댓글 목록
    const reviewContent = reviewsRes.json('content');
    if (reviewContent && reviewContent.length > 0) {
      const reviewId = reviewContent[0].id;
      const commentsRes = http.get(
          `${ENV.BASE_URL}/comments?reviewId=${reviewId}&limit=10`,
          {headers, tags: {name: 'GET /comments'}}
      );
      check(commentsRes, {
        '[GET /comments] status 200': (r) => r.status === 200,
      });
      sleep(1);
    }
  }

  // 인기 도서
  const popularBooksRes = http.get(
      `${ENV.BASE_URL}/books/popular?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /books/popular'}}
  );
  check(popularBooksRes, {
    '[GET /books/popular] status 200': (r) => r.status === 200,
  });
  sleep(1);

  // 인기 리뷰
  const popularReviewsRes = http.get(
      `${ENV.BASE_URL}/reviews/popular?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /reviews/popular'}}
  );
  check(popularReviewsRes, {
    '[GET /reviews/popular] status 200': (r) => r.status === 200,
  });
  sleep(1);

  // 파워 유저
  const powerUsersRes = http.get(
      `${ENV.BASE_URL}/users/power?period=DAILY&direction=DESC&limit=10`,
      {headers, tags: {name: 'GET /users/power'}}
  );
  check(powerUsersRes, {
    '[GET /users/power] status 200': (r) => r.status === 200,
  });
  sleep(1);

  // 알림 목록
  const notiRes = http.get(
      `${ENV.BASE_URL}/notifications?userId=${userId}&limit=10`,
      {headers, tags: {name: 'GET /notifications'}}
  );
  check(notiRes, {
    '[GET /notifications] status 200': (r) => r.status === 200,
  });
  sleep(1);
}