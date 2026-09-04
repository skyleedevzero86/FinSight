# FinSight

<img width="1889" height="922" alt="image" src="https://github.com/user-attachments/assets/85211cf6-0c1e-4aa5-918a-87dd447f746b" />
<img width="1882" height="922" alt="image" src="https://github.com/user-attachments/assets/5259b901-159d-4228-9381-ab8c17c79cdd" />
<img width="1884" height="910" alt="image" src="https://github.com/user-attachments/assets/f03b2d6d-344e-4c4c-b006-4ebeae5bbbfd" />
<img width="1886" height="924" alt="image" src="https://github.com/user-attachments/assets/0fa1822a-f465-47a7-882e-41cddd6bb7a8" />

경제/시장 뉴스를 수집·AI 분석하고, 커뮤니티·미디어·알림과 함께 제공하는 서비스입니다.

## 프로젝트 구성

| 경로                             | 역할                                                     |
| -------------------------------- | -------------------------------------------------------- |
| `backend/web`                    | 외부 REST API (포트 `8080`), JWT, Swagger                |
| `backend/core`                   | 도메인·JPA·보안·외부 API/AI (라이브러리, 단독 시 `8081`) |
| `backend/batch`                  | 뉴스/YouTube/알림/계정 스케줄 배치 (웹 없음)             |
| `frontend`                       | Next.js 15 App Router 사용자·관리자 UI (`3000`)          |
| `docker/` / `docker-compose.yml` | MySQL · Redis · MinIO 로컬 인프라                        |

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.5.4, Spring Security, JPA, QueryDSL, Redis, Flyway, Batch, WebSocket, Resilience4j, Micrometer/Actuator, Jasypt
- **AI/NLP**: OpenAI, Ollama/Llama, DJL(PyTorch), OpenNLP 등
- **Infra**: MySQL 8, Redis 7, MinIO/S3(에디터 에셋), H2(테스트)
- **알림**: Spring Mail, Solapi(SMS), FCM, Slack/Webhook 등
- **Frontend**: Next.js 15, React 18, TypeScript, Tailwind CSS 4, ESLint

## Backend 패키지 구조 (`core`)

도메인 패키지는 CRUD·서비스·어댑터, `global`은 횡단 관심사(설정·유틸)를 둡니다.

```text
com.sleekydz86.finsight.core/
├─ auth / board / comment / news / media / notification / user / …
│    └─ (도메인, port, adapter, service)
└─ global/
   ├─ config/          # Redis, Security, Web, Cache, DB, Async 등 공통 설정
   ├─ aspect / exception / dto / security / logging / …
   └─ …
```

- 공통 Spring 설정 → `global.config`
- 도메인 전용 properties → 해당 도메인
- `web` / `batch` 모듈 전용 config → 각 모듈에 유지

## Frontend 주요 경로

| 경로                                                 | 설명                         |
| ---------------------------------------------------- | ---------------------------- |
| `/`, `/news`, `/live-vod`, `/economy-pick`           | 메인·뉴스·LIVE/VOD·경제 Pick |
| `/login`, `/signup`, `/find-email`, `/find-password` | 인증·계정 복구               |
| `/auth/{google,kakao,naver}/callback`                | 소셜 로그인 콜백             |
| `/community/free`, `/qna`, `/notice`                 | 커뮤니티 CRUD·댓글·반응      |
| `/my`, `/my/posts`, `/my/favorites`, `/my/history`   | 마이페이지                   |
| `/admin/users`, `/admin/email-logs`                  | 관리자                       |

Next.js가 `/api/v1/*`를 백엔드(`FINSIGHT_API_BASE_URL`, 기본 `http://localhost:8080`)로 프록시합니다.

## 디렉터리 구조

```text
FinSight/
├─ backend/
│  ├─ web/
│  ├─ core/
│  └─ batch/
├─ frontend/
├─ docker/
├─ docker-compose.yml
└─ readme.md
```

## 사전 요구사항

- JDK 25
- Node.js 20+ (또는 Bun)
- (권장) Docker — MySQL / Redis / MinIO
- (선택) 외부 API 키: MarketAux, OpenAI, YouTube, 메일/Solapi/FCM, OAuth 클라이언트

## 로컬 인프라

```bash
docker compose up -d
```

| 서비스 | 접속                                                        |
| ------ | ----------------------------------------------------------- |
| MySQL  | `localhost:3306` / DB `finsight` / 사용자·비번 compose 참고 |
| Redis  | `localhost:9379` (컨테이너 6379, 비밀번호 compose 참고)     |
| MinIO  | API `9000`, 콘솔 `9001`                                     |

## 로컬 실행

### 1) Backend

```bash
cd backend
./gradlew :web:bootRun
./gradlew :batch:bootRun
```

Windows(PowerShell):

```powershell
cd backend
.\gradlew.bat :web:bootRun
.\gradlew.bat :batch:bootRun
```

| 모듈    | 프로필        | 포트           |
| ------- | ------------- | -------------- |
| `web`   | `local`       | `8080`         |
| `core`  | `core-local`  | 단독 시 `8081` |
| `batch` | `batch-local` | 웹 없음        |

일반 API 개발은 `:web:bootRun`만으로 충분합니다. 뉴스·YouTube 수집은 `:batch:bootRun`이 필요합니다.

### 2) Frontend

이 저장소는 `bun.lock`을 사용합니다. (npm/pnpm도 가능)

```bash
cd frontend
bun install
bun run dev
```

- 개발: `http://localhost:3000`
- 빌드: `bun run build` → `bun run start`

## 환경변수

### Backend

- DB: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- JWT: `JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_EXPIRATION_PERIOD`, `JWT_REFRESH_EXPIRATION_PERIOD`
- 암호화: `ENCRYPT_KEY`
- 뉴스/AI: `MARKETAUX_API_KEY`, `OPENAI_API_KEY`, `OPENAI_API_URL`, `OPENAI_MODEL`
- YouTube: `YOUTUBE_API_KEY`, `YOUTUBE_API_URL`, `YOUTUBE_MAX_RESULTS_PER_SOURCE`
- 메일: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- SMS: `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`, `SOLAPI_FROM_NUMBER`
- 소셜: `GOOGLE_*`, `KAKAO_CLIENT_ID`, `KAKAO_REDIRECT_URI`, `NAVER_*` 등

### Frontend

- `FINSIGHT_API_BASE_URL` (예: `http://localhost:8080`)
- `FINSIGHT_API_PROXY_TIMEOUT_MS`

## 배치 작업

`backend/batch` 스케줄러:

- 뉴스 수집·AI/감정 분석
- YouTube import · AI enrichment
- 게시글 모더레이션(신고 과다 숨김 등)
- 사용자 상태·비밀번호 만료 점검
- 알림 발송·정리

## API 문서

`web` 기동 후:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `/api/v1/health`, Actuator

환경·보안 설정에 따라 경로/접근 정책이 달라질 수 있습니다.
