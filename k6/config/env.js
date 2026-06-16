// k6/config/env.js

export const ENV = {
  BASE_URL: __ENV.BASE_URL || 'http://localhost:8080/api',
};

// curl로 조회한 deokhugam-perf 내 실제 시드 유저
const DEFAULT_USERS = [
  '2e56150c-1826-49aa-9d2a-9f0ce44d1c47',
  '390afed4-b2b6-4191-98ad-9c9919a5908c',
  '543b039c-0e11-4ab3-aefd-2a20ee684e99',
  'a88656bb-7b95-4605-b9bd-f596eae545ee',
  'df40d4fb-2619-4dc3-b8c3-33abd060ddc1',
  '8fff8b26-5512-406f-8de7-aef282ece327',
  '67fdb7f1-9b58-457c-80bd-e1a101f18fad',
  '533658f1-6d28-45d7-8a89-bee749817489',
  'b3d5d67a-ee0f-448b-afd2-afc243329b38',
  '6e246fdc-41d2-45a3-b530-2dab99b912b6',
];

export const TEST_USERS = __ENV.USERS
    ? __ENV.USERS.split(',').map((s) => s.trim()).filter((s) => s.length > 0)
    : DEFAULT_USERS;

// 동시성 테스트
export const CONCURRENCY_TARGET = {
  reviewId: 'f3b039f1-ccb9-4fcb-8817-94640e277492',         // 좋아요·댓글 대상
  isbn: '9788900000000',                                    // ISBN 중복 테스트
  reviewRaceBookId: '00001917-de8f-43be-9c1d-606ae9ca269f', // 1인1리뷰 대상 책
  reviewRaceUserId: '390afed4-b2b6-4191-98ad-9c9919a5908c', // 1인1리뷰 대상 유저
};

// 로그인 테스트용 경신 계정
export const KYOUNGSIN_USER = '9c156297-0f12-476a-afe7-e6e1fad089a3';