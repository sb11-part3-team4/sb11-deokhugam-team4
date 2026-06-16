// k6/config/phase2_search.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {ENV, TEST_USERS} from './config/env.js';
import {searchOptions} from './config/options.js';
import {getAuthHeaders} from './helpers/auth.js';

export const options = searchOptions;

const BOOK_KEYWORDS = ['테스트도서', '저자', '도서_1', '도서_2', '_10', '_20'];
const BOOK_SORTS = ['title', 'publishedDate', 'rating', 'reviewCount'];  // 정렬 4종
const REVIEW_KEYWORDS = ['감상', '시드', '내용', '도서'];

export default function () {
  const userId = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  // 도서 키워드 검색
  const kw = BOOK_KEYWORDS[__ITER % BOOK_KEYWORDS.length];
  const sort = BOOK_SORTS[__ITER % BOOK_SORTS.length];
  const bookSearchRes = http.get(
      `${ENV.BASE_URL}/books?keyword=${encodeURIComponent(
          kw)}&orderBy=${sort}&direction=DESC&limit=50`,
      {headers, tags: {name: 'GET /books?keyword'}}
  );
  check(bookSearchRes, {'[book search] 200': (r) => r.status === 200});
  sleep(0.5);

  // 도서 대량매칭 키워드 + 정렬
  const bookHeavyRes = http.get(
      `${ENV.BASE_URL}/books?keyword=${encodeURIComponent(
          '테스트도서')}&orderBy=rating&direction=DESC&limit=50`,
      {headers, tags: {name: 'GET /books?keyword(heavy)'}}
  );
  if (bookHeavyRes.status !== 200) {
    console.log(
        `[book heavy 실패] status=${bookHeavyRes.status}, body=${bookHeavyRes.body}`);
  }
  check(bookHeavyRes, {'[book heavy] 200': (r) => r.status === 200});
  sleep(0.5);

  // 리뷰 키워드 검색
  const rkw = REVIEW_KEYWORDS[__ITER % REVIEW_KEYWORDS.length];
  const rsort = (__ITER % 2 === 0) ? 'createdAt' : 'rating';
  const reviewSearchRes = http.get(
      `${ENV.BASE_URL}/reviews?keyword=${encodeURIComponent(
          rkw)}&orderBy=${rsort}&direction=DESC&limit=50`,
      {headers, tags: {name: 'GET /reviews?keyword'}}
  );
  check(reviewSearchRes, {'[review search] 200': (r) => r.status === 200});
  sleep(0.5);

  // 리뷰 keyword + userId 필터
  const reviewFilterRes = http.get(
      `${ENV.BASE_URL}/reviews?keyword=${encodeURIComponent(
          rkw)}&userId=${userId}&orderBy=createdAt&direction=DESC&limit=50`,
      {headers, tags: {name: 'GET /reviews?keyword+filter'}}
  );
  check(reviewFilterRes, {'[review filter] 200': (r) => r.status === 200});
  sleep(0.5);
}