// k6/config/options.js

// Phase 1: 스모크 테스트
export const smokeOptions = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],    // 에러 1% 미만
    http_req_duration: ['p(95)<2000'], // smoke는 기준 느슨하게 2초 이내로 측정
  },
};

// Phase 1: 베이스라인 테스트
export const baselineOptions = {
  stages: [
    {duration: '1m', target: 10}, // 워밍업
    {duration: '5m', target: 10}, // 기준선 측정 구간
    {duration: '1m', target: 0}, // 쿨다운
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:GET /books}': ['p(95)<500'],
    'http_req_duration{name:GET /books/{id}}': ['p(95)<300'],
    'http_req_duration{name:GET /reviews}': ['p(95)<500'],
    'http_req_duration{name:GET /reviews/{id}}': ['p(95)<300'],
    'http_req_duration{name:GET /comments}': ['p(95)<400'],
    'http_req_duration{name:GET /notifications}': ['p(95)<400'],
    'http_req_duration{name:GET /books/popular}': ['p(95)<800'],
    'http_req_duration{name:GET /reviews/popular}': ['p(95)<800'],
    'http_req_duration{name:GET /users/power}': ['p(95)<800'],
  }
};

// Phase 2: 로드 테스트
export const loadOptions = {
  stages: [
    {duration: '2m', target: 50},  // 0 → 50명 워밍업
    {duration: '5m', target: 50},  // 50명 유지
    {duration: '2m', target: 100}, // 50 → 100명 피크
    {duration: '5m', target: 100}, // 100명 유지
    {duration: '2m', target: 0},   // 쿨다운
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],   // 부하 상태이므로 1초까지 허용
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:GET /books}': ['p(95)<1000'],
    'http_req_duration{name:GET /books/{id}}': ['p(95)<800'],
    'http_req_duration{name:GET /books/popular}': ['p(95)<1000'],
    'http_req_duration{name:GET /reviews}': ['p(95)<1000'],
    'http_req_duration{name:GET /reviews/{id}}': ['p(95)<800'],
    'http_req_duration{name:GET /reviews/popular}': ['p(95)<1000'],
    'http_req_duration{name:GET /notifications}': ['p(95)<800'],
    'http_req_duration{name:GET /comments}': ['p(95)<1000'],
    'http_req_duration{name:GET /users/power}': ['p(95)<1000'],
  },
};

// Phase 2: 스트레스 테스트
export const stressOptions = {
  stages: [
    {duration: '2m', target: 100},  // 워밍업 (Load 피크 수준)
    {duration: '3m', target: 200},  // 200까지
    {duration: '3m', target: 300},  // 300까지
    {duration: '3m', target: 400},  // 400까지
    {duration: '3m', target: 0},    // 쿨다운
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],  // 느슨하게
    http_req_failed: ['rate<0.10'],     // 10%까지는 관찰 계속
  },
};

// Phase 2: 스파이크 테스트
export const spikeOptions = {
  stages: [
    {duration: '1m', target: 10},   // 평상시 (10 VU)
    {duration: '10s', target: 300}, // 급증 -> 10초 만에 300으로
    {duration: '2m', target: 300},  // 급증 유지
    {duration: '10s', target: 10},  // 급감
    {duration: '2m', target: 10},   // 회복 관찰
    {duration: '30s', target: 0},   // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],   // 급증이라 5%까지 허용
  },
};

// Phase 2: 인증 테스트 (회원가입 / 로그인 단독 측정)
export const authOptions = {
  scenarios: {
    signup_flow: {
      executor: 'ramping-vus',
      exec: 'signupScenario',
      startVUs: 0,
      stages: [
        {duration: '30s', target: 10},
        {duration: '2m', target: 10},
        {duration: '30s', target: 0},
      ],
      gracefulRampDown: '30s',
    },
    login_flow: {
      executor: 'ramping-vus',
      exec: 'loginScenario',
      startVUs: 0,
      stages: [
        {duration: '30s', target: 20},
        {duration: '2m', target: 20},
        {duration: '30s', target: 0},
      ],
      gracefulRampDown: '30s',
      startTime: '3m30s',  // 회원가입 끝난 뒤 로그인 측정 분리
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'],  // 회원가입 이메일 중복 등 일부 허용
    'http_req_duration{name:POST /users (signup)}': ['p(95)<1000'],
    'http_req_duration{name:POST /users/login}': ['p(95)<800'],
  },
};

// Phase 2: 동시성 테스트
export const concurrencyOptions = {
  scenarios: {
    like_storm: {
      executor: 'shared-iterations',
      vus: 10, iterations: 10, maxDuration: '30s',
      exec: 'likeScenario',
    },
    comment_storm: {
      executor: 'shared-iterations',
      vus: 10, iterations: 10, maxDuration: '30s',
      exec: 'commentScenario',
      startTime: '35s',
    },
    isbn_race: {
      executor: 'shared-iterations',
      vus: 10, iterations: 10, maxDuration: '30s',   // 같은 ISBN 10번 동시
      exec: 'isbnScenario',
      startTime: '70s',
    },
    review_race: {
      executor: 'shared-iterations',
      vus: 10, iterations: 10, maxDuration: '30s',
      exec: 'reviewScenario',
      startTime: '105s',
    },
  },
};

// Phase 2: 검색 성능 테스트
export const searchOptions = {
  stages: [
    {duration: '1m', target: 20},   // 워밍업
    {duration: '3m', target: 20},   // 검색 부하 유지
    {duration: '1m', target: 0},    // 쿨다운
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],   // 검색은 조회보다 느릴 수 있어 느슨하게
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:GET /books?keyword}': ['p(95)<2000'],
    'http_req_duration{name:GET /reviews?keyword}': ['p(95)<2000'],
  },
};

// Phase 2: 페이지네이션 테스트
export const paginationOptions = {
  stages: [
    {duration: '1m', target: 20},
    {duration: '3m', target: 20},
    {duration: '1m', target: 0},
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:books page1}': ['p(95)<1000'],
    'http_req_duration{name:books deep}': ['p(95)<1000'],
  },
};

// Phase 2: Redis 캐시 테스트
export const cacheOptions = {
  stages: [
    {duration: '30s', target: 50}, // 워밍업 + 캐시 적재
    {duration: '2m', target: 50},  // 정상 부하: 캐시 히트 구간
    {duration: '30s', target: 0},
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:books popular}': ['p(95)<1000'],
    'http_req_duration{name:users power}': ['p(95)<1000'],
  },
};

export const ScenarioOptions = {
  scenarios: {
    browse_flow: {
      executor: 'ramping-vus',
      exec: 'browseFlow',
      startVUs: 0,
      stages: [
        {duration: '1m', target: 35},
        {duration: '3m', target: 35},
        {duration: '1m', target: 0},
      ],
      gracefulRampDown: '30s',
    },
    dashboard_flow: {
      executor: 'ramping-vus',
      exec: 'dashboardFlow',
      startVUs: 0,
      stages: [
        {duration: '1m', target: 10},
        {duration: '3m', target: 10},
        {duration: '1m', target: 0},
      ],
      gracefulRampDown: '30s',
    },
    active_flow: {
      executor: 'ramping-vus',
      exec: 'activeFlow',
      startVUs: 0,
      stages: [
        {duration: '1m', target: 5},
        {duration: '3m', target: 5},
        {duration: '1m', target: 0},
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'], // 쓰기 흐름의 정상 충돌 고려해 5%
    'http_req_duration{flow:browse}': ['p(95)<800'],    // 읽기 흐름은 더 엄격하게
    'http_req_duration{flow:dashboard}': ['p(95)<800'],
    'http_req_duration{flow:active}': ['p(95)<1500'],     // 쓰기 흐름은 여유
  },
};

// Phase 3: Soak 테스트 (장시간 안정성)
export const soakOptions = {
  scenarios: {
    read_soak: {
      executor: 'constant-vus',
      exec: 'readFlow',
      vus: 25,
      duration: '4h',
    },
    write_soak: {
      executor: 'constant-vus',
      exec: 'writeFlow',
      vus: 5,
      duration: '4h',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{flow:read}': ['p(95)<800'],
    'http_req_duration{flow:write}': ['p(95)<1500'],
  },
};

export const externalOptions = {
  scenarios: {
    naver_info: {
      executor: 'constant-vus',
      exec: 'naverInfoScenario',
      vus: 3,
      duration: '2m',
    },
    ocr: {
      executor: 'constant-vus',
      exec: 'ocrScenario',
      vus: 2,
      duration: '2m',
      startTime: '2m30s',   // naver 끝난 뒤 외부 부하 분리
    },
    create_no_thumb: {
      executor: 'constant-vus',
      exec: 'createNoThumbScenario',
      vus: 2,
      duration: '2m',
      startTime: '5m',
    },
    create_with_thumb: {
      executor: 'constant-vus',
      exec: 'createWithThumbScenario',
      vus: 2,
      duration: '2m',
      startTime: '7m30s',
    },
  },
  thresholds: {
    // 외부 의존이라 느릴 수 있어 기준 느슨하게 (관찰 목적)
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.10'],   // 외부 API rate limit 등 고려
    'http_req_duration{name:GET /books/info (Naver)}': ['p(95)<3000'],
    'http_req_duration{name:POST /books/isbn/ocr (OCR)}': ['p(95)<5000'],
    'http_req_duration{name:POST /books (썸네일 無)}': ['p(95)<3000'],
    'http_req_duration{name:POST /books (썸네일 有)}': ['p(95)<5000'],
  },
};

// Phase 3: 배치 병행 테스트
export const batchConcurrentOptions = {
  stages: [
    {duration: '2m', target: 30},   // 배치 실행 전 (기준선)
    {duration: '6m', target: 30},   // 정각 배치 실행 구간 포함 유지
    {duration: '2m', target: 0},    // 배치 실행 후
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{target:ranking}': ['p(95)<1000'], // 배치가 갱신하는 랭킹 조회
    'http_req_duration{target:normal}': ['p(95)<800'],  // 배치와 무관한 일반 조회 대조군
  },
};