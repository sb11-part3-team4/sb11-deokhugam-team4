// k6/config/phase2_pagination.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {ENV, TEST_USERS} from './config/env.js';
import {paginationOptions} from './config/options.js';
import {getAuthHeaders} from './helpers/auth.js';

export const options = paginationOptions;

const BOOK_SORTS = ['title', 'publishedDate', 'rating', 'reviewCount'];
const REVIEW_SORTS = ['createdAt', 'rating'];

export default function () {
  const userId = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  // 도서 커서 페이지네이션
  const sort = BOOK_SORTS[__ITER % BOOK_SORTS.length];

  let res = http.get(
      `${ENV.BASE_URL}/books?orderBy=${sort}&direction=ASC&limit=50`,
      {headers, tags: {name: 'books page1'}}
  );
  check(res, {'book page1 200': (r) => r.status === 200});

  let body = null;
  try {
    body = res.json();
  } catch (e) {
    body = null;
  }

  // hasNext가 true이고 nextCursor가 있을 때만 루프
  for (let i = 0; i < 10 && body && body.hasNext === true && body.nextCursor;
      i++) {
    const cursor = encodeURIComponent(body.nextCursor);
    const after = body.nextAfter ? encodeURIComponent(body.nextAfter) : '';

    res = http.get(
        `${ENV.BASE_URL}/books?orderBy=${sort}&direction=ASC&limit=50&cursor=${cursor}&after=${after}`,
        {headers, tags: {name: 'books deep'}}  // ← 태그 단순화
    );
    const ok = check(res, {'book deep 200': (r) => r.status === 200});

    if (!ok) {
      console.error(`[book deep FAIL] sort=${sort} page=${i
      + 2} status=${res.status} body=${res.body ? res.body.substring(0, 200)
          : 'null'}`);
    }

    try {
      body = res.json();
    } catch (e) {
      body = null;
    }
    sleep(0.2);
  }

  sleep(0.3);

  // 리뷰 커서 페이지네이션
  const rsort = REVIEW_SORTS[__ITER % REVIEW_SORTS.length];

  res = http.get(
      `${ENV.BASE_URL}/reviews?orderBy=${rsort}&direction=DESC&limit=50`,
      {headers, tags: {name: 'reviews page1'}}
  );
  check(res, {
    'review page1 200': (r) => r.status === 200,
    'likedByMe 포함': (r) => {
      try {
        const b = r.json();
        return b && b.content && b.content.length > 0
            ? b.content[0].likedByMe !== undefined
            : true;
      } catch (e) {
        return false;
      }
    },
  });

  let rbody = null;
  try {
    rbody = res.json();
  } catch (e) {
    rbody = null;
  }

  for (let i = 0; i < 10 && rbody && rbody.hasNext === true && rbody.nextCursor;
      i++) {
    const cursor = encodeURIComponent(rbody.nextCursor);
    const after = rbody.nextAfter ? encodeURIComponent(rbody.nextAfter) : '';

    res = http.get(
        `${ENV.BASE_URL}/reviews?orderBy=${rsort}&direction=DESC&limit=50&cursor=${cursor}&after=${after}`,
        {headers, tags: {name: 'reviews deep'}}
    );
    check(res, {'review deep 200': (r) => r.status === 200});

    try {
      rbody = res.json();
    } catch (e) {
      rbody = null;
    }
    sleep(0.2);
  }

  sleep(0.5);
}