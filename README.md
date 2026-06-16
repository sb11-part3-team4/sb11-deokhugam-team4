<img width="680" height="178" alt="image" src="https://github.com/user-attachments/assets/1106b1ba-aed4-448f-9300-f5d93f5684a2" />


- 책 읽는 즐거움을 공유하고, 지식과 감상을 나누는 책 덕후들을 위한 커뮤니티 서비스입니다.

---

## 프로젝트 목표

- 인기 도서·리뷰·파워 유저 점수 산출 등 주기적 대용량 데이터 연산을 Spring Batch로 안정적으로 처리합니다.
- 유지보수성을 고려하여 객체지향 설계 원칙에 따라 개발하고, TDD 사이클을 준수하며 테스트 커버리지 80% 이상을 유지합니다.
- GitHub Actions와 AWS ECS를 활용한 CI/CD 파이프라인을 구성하여 빌드·테스트·배포를 자동화합니다.

### CODECOV 배지

![codecov|112](https://codecov.io/gh/sb11-part3-team4/sb11-deokhugam-team4/branch/main/graph/badge.svg)


---

## 기술 스택

| 분류 | 사용 도구 및 버전 |
| --- | --- |
| Language | Java 17 (OpenJDK) |
| Framework | Spring Boot 3.5.14 |
| Database | PostgreSQL (AWS RDS), H2 (Test) |
| Cache | Redis (Local), Upstash Redis (Prod) |
| Storage | AWS S3 |
| ORM | Spring Data JPA |
| Build Tool | Gradle 8.14.4 |
| Scheduling | Spring Scheduler, Spring Batch |
| Monitoring | Spring Boot Actuator |
| API Docs | springdoc-openapi (Swagger) |
| CI/CD | GitHub Actions |
| Deployment | AWS EC2, Docker, AWS ECS, AWS ECR |
| Test | JUnit 5, JaCoCo, Codecov |
| External API | Naver Book API, OCR Space API |

---

## 팀원 소개 및 역할 분담

| 이름 | 담당 업무 |
| --- | --- |
| 김지성 | 사용자 API 전체 — 회원가입, 로그인, 조회, 수정, 삭제, 파워 유저 조회 |
| 김관희 | 리뷰 API 전체 — 등록, 조회, 수정, 삭제, 좋아요, 인기 리뷰 조회 |
| 이경신 | 도서 API — 생성, 수정, OCR 기반 ISBN 인식 / AWS S3 업로더 연동 / 성능 테스트 / 전역 예외 처리 고도화 / 통합 보고서 작성 |
| 정재현 | 댓글 API 전체 — 등록, 조회, 수정, 삭제 / 고아 데이터 배치 삭제 처리 |
| 최광호 | 알림 API 전체 — 목록 조회, 읽음 처리, 전체 읽음 처리 / 발표 자료 제작 |
| 최정윤 (팀장) | 프로젝트 초기 세팅 및 공용 코드 작성 / CI·CD 파이프라인 구축 / 도서 API — 단일 조회, Naver Book API 기반 ISBN 조회, 목록 조회, 삭제, 인기 도서 조회 / 배치 모니터링 메트릭 구현 |

[팀원 소개 및 역할 분담](팀원-소개-및-역할-분담)

---

## 프로젝트 아키텍처

<img width="1051" height="646" alt="image" src="https://github.com/user-attachments/assets/815c82fc-2499-4e49-b543-3a1c6c93cfcc" />


## ERD

<img width="853" height="717" alt="image" src="https://github.com/user-attachments/assets/848d3f95-4eb9-49b9-80ca-9200a449724c" />


## 배포 파이프라인 플로우차트

<img width="1658" height="293" alt="image" src="https://github.com/user-attachments/assets/0abfe854-e702-4151-a012-70f7a0326fbb" />


## USE CASE

[위키-유즈 케이스](USE-CASE)


## 트러블 슈팅

[위키-트러블 슈팅](트러블슈팅)
