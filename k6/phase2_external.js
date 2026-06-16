// k6/config/phase2_external.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {ENV, TEST_USERS} from './config/env.js';
import {getAuthHeaders} from './helpers/auth.js';
import {externalOptions} from './config/options.js';

export const options = externalOptions;

// 실제 이미지 파일을 바이너리로 로드
const ISBN_IMAGE = open('./assets/isbn.png', 'b');
const THUMB_IMAGE = open('./assets/thumb.png', 'b');

// Naver에서 실제 조회되는 ISBN
const FIXED_ISBN = '9791160263404';

function pickUser() {
  return TEST_USERS[exec.scenario.iterationInTest % TEST_USERS.length];
}

// Naver 도서 정보 조회
export function naverInfoScenario() {
  const headers = getAuthHeaders(pickUser());

  const res = http.get(
      `${ENV.BASE_URL}/books/info?isbn=${FIXED_ISBN}`,
      {headers, tags: {name: 'GET /books/info (Naver)'}}
  );
  check(res, {'info 200': (r) => r.status === 200});
  if (res.status !== 200) {
    console.log(`[naver info] status=${res.status} body=${res.body}`);
  }
  sleep(2); // 외부 rate limit 보호
}

// OCR ISBN 이미지 인식
export function ocrScenario() {
  const headers = getAuthHeaders(pickUser(), true); // 멀티파트

  const res = http.post(
      `${ENV.BASE_URL}/books/isbn/ocr`,
      {image: http.file(ISBN_IMAGE, 'isbn.png', 'image/png')},
      {headers, tags: {name: 'POST /books/isbn/ocr (OCR)'}}
  );
  check(res, {'ocr 응답': (r) => r.status === 200 || r.status === 400});
  if (res.status !== 200 && res.status !== 400) {
    console.log(`[ocr] status=${res.status} body=${res.body}`);
  }
  sleep(3); // OCR은 무거우므로 더 긴 간격
}

// 도서 등록 — 썸네일 없음
export function createNoThumbScenario() {
  const headers = getAuthHeaders(pickUser(), true);

  const bookData = JSON.stringify({
    isbn: `978${String(exec.scenario.iterationInTest).padStart(10, '0')}`, // 고유 ISBN
    title: `외부테스트 도서 ${exec.scenario.iterationInTest}`,
    author: '외부테스트',
    description: '썸네일 없는 등록 (서버+Naver 경로)',
    publisher: '테스트',
    publishedDate: '2024-01-01',
  });

  const res = http.post(
      `${ENV.BASE_URL}/books`,
      {bookData: http.file(bookData, 'bookData.json', 'application/json')},
      {headers, tags: {name: 'POST /books (썸네일 無)'}}
  );
  check(res, {
    'create 성공/충돌': (r) => r.status === 201 || r.status === 200 || r.status
        === 409
  });
  sleep(2);
}

// 도서 등록 — 썸네일 있음
export function createWithThumbScenario() {
  const headers = getAuthHeaders(pickUser(), true);

  const bookData = JSON.stringify({
    isbn: `979${String(exec.scenario.iterationInTest).padStart(10, '0')}`,
    title: `외부테스트 썸네일 ${exec.scenario.iterationInTest}`,
    author: '외부테스트',
    description: '썸네일 있는 등록 (서버+Naver+S3 업로드)',
    publisher: '테스트',
    publishedDate: '2024-01-01',
  });

  const res = http.post(
      `${ENV.BASE_URL}/books`,
      {
        bookData: http.file(bookData, 'bookData.json', 'application/json'),
        thumbnailImage: http.file(THUMB_IMAGE, 'thumb.png', 'image/png'),
      },
      {headers, tags: {name: 'POST /books (썸네일 有)'}}
  );
  check(res, {
    'create 성공/충돌': (r) => r.status === 201 || r.status === 200 || r.status
        === 409
  });
  sleep(2);
}