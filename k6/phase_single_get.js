// k6/config/phase_single_get.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {ENV, TEST_USERS} from './config/env.js';
import {baselineOptions} from './config/options.js';
import {getAuthHeaders} from './helpers/auth.js';

export const options = baselineOptions;

const COMMENT_ID = '8543084a-0a69-4836-ba63-c0c5a801f9ab';

export default function () {
  const userId = TEST_USERS[(__VU - 1) % TEST_USERS.length];
  const headers = getAuthHeaders(userId);

  // 사용자 정보 단건 조회
  const userRes = http.get(
      `${ENV.BASE_URL}/users/${userId}`,
      {headers, tags: {name: 'GET /users/{id}'}}
  );
  check(userRes, {'[user] 200': (r) => r.status === 200});
  sleep(0.5);

  // 댓글 상세 단건 조회
  const commentRes = http.get(
      `${ENV.BASE_URL}/comments/${COMMENT_ID}`,
      {headers, tags: {name: 'GET /comments/{id}'}}
  );
  check(commentRes, {'[comment] 200': (r) => r.status === 200});
  sleep(0.5);
}