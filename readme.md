# FinSight

<img width="1136" height="505" alt="image" src="https://github.com/user-attachments/assets/a106a5d4-f7ba-4be0-be3b-daca917b7a2a" />
<img width="1917" height="324" alt="image" src="https://github.com/user-attachments/assets/45ebb257-5bb3-49f0-9f4d-1764d4f7d44a" />

미국 경제/시장 뉴스를 수집하고, 분석/가공한 뒤 사용자 맞춤 피드와 커뮤니티 기능으로 제공하는 서비스입니다.

## 프로젝트 구성

### Backend (Spring Boot 멀티모듈)

- `backend/web`: 외부에 노출되는 REST API 서버, 인증/인가, 게시판/뉴스/알림 API
- `backend/core`: 공통 도메인, 데이터 접근, 보안/토큰, 외부 API/AI 연동 로직
- `backend/batch`: 스케줄러 기반 뉴스 수집/가공, 알림/계정 관리 배치 작업

### Frontend (Next.js)

- `frontend`: 사용자 웹 UI
- App Router 기반 Next.js 프로젝트 (`next dev`, `next build`, `next start`)

## 기술 스택

- Backend: Java 21, Spring Boot 3.5.4, Spring Security, JPA, Redis, Flyway, Batch, WebSocket, QueryDSL
- AI/NLP: OpenAI API 연동, DJL(PyTorch), OpenNLP, Stanford CoreNLP
- Infra/기타: MySQL, H2(로컬/테스트), Micrometer/Actuator, Jasypt, Spring Mail, Solapi
- Frontend: Next.js 15, React 18, TypeScript, ESLint, TailwindCSS 4

## 디렉터리 구조

```text
FinSight/
├─ backend/
│  ├─ web/
│  ├─ core/
│  └─ batch/
├─ frontend/
└─ readme.md
```

## 사전 요구사항

- JDK 21
- Node.js 20+
- pnpm
- (선택) MySQL / Redis / SMTP / 외부 API 키

## 로컬 실행

### 1) Backend

```bash
cd backend
./gradlew :core:bootRun
./gradlew :web:bootRun
./gradlew :batch:bootRun
```

- 기본 프로필
  - `core`: `core-local`
  - `web`: `local`
  - `batch`: `batch-local`
- 기본 포트
  - `core`: `8081` (`SERVER_PORT` 미지정 시)
  - `web`: `8080`
  - `batch`: 웹 비활성(`spring.main.web-application-type=none`)

### 2) Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

- 개발 서버: `http://localhost:3000`
- 프로덕션 빌드: `pnpm build`
- 프로덕션 실행: `pnpm start`

## 환경변수 (주요)

실행 환경에 따라 아래 값을 설정해서 사용합니다.

### Backend 공통

- DB: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- JWT: `JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_EXPIRATION_PERIOD`, `JWT_REFRESH_EXPIRATION_PERIOD`
- 암호화: `ENCRYPT_KEY`

### 외부 연동

- 뉴스/AI: `MARKETAUX_API_KEY`, `OPENAI_API_KEY`, `OPENAI_API_URL`, `OPENAI_MODEL`
- YouTube: `YOUTUBE_API_KEY`, `YOUTUBE_API_URL`, `YOUTUBE_MAX_RESULTS_PER_SOURCE`
- 메일: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- 알림: `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`, `SOLAPI_FROM_NUMBER`
- 소셜: `KAKAO_CLIENT_ID`, `KAKAO_REDIRECT_URI`

### Frontend

- `FINSIGHT_API_BASE_URL` (예: `http://localhost:8080`)
- `FINSIGHT_API_PROXY_TIMEOUT_MS`

## 배치 작업 개요

`backend/batch`는 스케줄러를 통해 주기 작업을 수행합니다.

- 뉴스 수집/가공 스케줄
- YouTube import 및 AI enrichment 스케줄
- 사용자 상태/비밀번호 만료 점검
- 알림 발송/정리 작업

## API 문서

SpringDoc(OpenAPI) UI는 `web` 모듈 실행 후 확인할 수 있습니다.

- 일반적인 경로: `/swagger-ui/index.html`
- 환경/보안 설정에 따라 경로/접근 정책은 달라질 수 있습니다.
