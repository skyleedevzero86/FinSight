# FinSight

<img width="1889" height="922" alt="image" src="https://github.com/user-attachments/assets/85211cf6-0c1e-4aa5-918a-87dd447f746b" />
<img width="1882" height="922" alt="image" src="https://github.com/user-attachments/assets/5259b901-159d-4228-9381-ab8c17c79cdd" />
<img width="1884" height="910" alt="image" src="https://github.com/user-attachments/assets/f03b2d6d-344e-4c4c-b006-4ebeae5bbbfd" />
<img width="1886" height="924" alt="image" src="https://github.com/user-attachments/assets/0fa1822a-f465-47a7-882e-41cddd6bb7a8" />

경제/시장 뉴스를 수집·AI 분석하고, 커뮤니티·미디어·알림과 함께 제공하는 서비스입니다.

## 프로젝트 구성

| 경로 | 역할 |
| --- | --- |
| `backend/web` | 외부 REST API (포트 `8080`), JWT, Swagger, Spring AI MCP |
| `backend/core` | 도메인·JPA·보안·외부 API/AI (라이브러리, 단독 시 `8081`) |
| `backend/batch` | 뉴스/YouTube/알림/계정 스케줄 배치 (웹 없음) |
| `frontend` | Next.js 15 App Router 사용자·관리자 UI (`3000`) |
| `docker-compose.yml` | 로컬 데이터 스토어만 (MySQL · Redis · MinIO) |
| `infra/docker-compose.yml` | 앱 포함 Compose (web · batch · frontend + 위 스토어) |
| `infra/k8s` | k3s/Kustomize 매니페스트 (Compose와 분리, 네임스페이스 `finsight-k8s`) |
| `docs/setup` | 로컬 AI(Ollama · DJL · Hugging Face) 설치 가이드 |

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.5.4, Spring Security, JPA, QueryDSL, Redis, Flyway, Batch, WebSocket, Resilience4j, Micrometer/Actuator, Jasypt, Spring AI MCP Server
- **AI/NLP**: OpenAI, Ollama/Llama, Hugging Face Inference, DJL(PyTorch CPU), OpenNLP
- **Infra**: MySQL 8.4, Redis 7, MinIO/S3(에디터 에셋), H2(테스트). 클러스터 배포는 `infra/k8s` (k3s `local-path`)
- **알림**: Spring Mail, Solapi(SMS), FCM, Slack/Webhook 등
- **Frontend**: Next.js 15, React 19, TypeScript, Tailwind CSS 4, ESLint

## Backend 패키지 구조 (`core`)

도메인 패키지는 CRUD·서비스·어댑터, `global`은 횡단 관심사(설정·유틸)를 둡니다.

```text
com.sleekydz86.finsight.core/
├─ auth / board / comment / news / media / notification / user
├─ editor / inbox / health / history / cms
├─ popup / mainimg / ulink
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

| 경로 | 설명 |
| --- | --- |
| `/`, `/news`, `/search`, `/live-vod`, `/economy-pick` | 메인·뉴스·검색·LIVE/VOD·경제 Pick |
| `/live-vod/watch/[videoId]` | VOD 시청 |
| `/login`, `/signup`, `/find-email`, `/find-password` | 인증·계정 복구 |
| `/auth/{google,kakao,naver}/callback` | 소셜 로그인 콜백 |
| `/verify/[token]` | 이메일 인증 |
| `/community`, `/community/free`, `/qna`, `/notice` | 커뮤니티 CRUD·댓글·반응 |
| `/myinfo`, `/myinfo/posts`, `/myinfo/favorites`, `/myinfo/history`, `/myinfo/portfolio`, `/myinfo/userinfo` | 마이페이지 |
| `/admin/users`, `/stats`, `/health`, `/email-logs`, `/sms` | 관리자 (회원·통계·헬스·메일·SMS) |
| `/admin/moderation`, `/notifications`, `/popup`, `/mainimg`, `/ulink` | 관리자 (모더레이션·알림·팝업·메인이미지·링크) |
| `/terms`, `/privacy`, `/viewer-rights`, `/youth-policy` | 약관·정책 |

`/myinfo`, `/admin`, 커뮤니티 글쓰기·수정은 세션 쿠키가 없으면 `/login`으로 보냅니다.

Next.js가 `/api/v1/*`를 백엔드(`FINSIGHT_API_BASE_URL`, 기본 `http://localhost:8080`)로 프록시합니다.

## 디렉터리 구조

```text
FinSight/
├─ backend/
│  ├─ web/
│  ├─ core/
│  └─ batch/
├─ frontend/
├─ infra/
│  ├─ docker-compose.yml
│  └─ k8s/                 # Kustomize, 상세는 infra/k8s/README.md
├─ docs/
│  ├─ setup/               # 로컬 AI 설치
│  └─ cassandra-ai/        # 별도 DART/KOSDAQ 랩 (FinSight 앱과 분리)
├─ docker-compose.yml      # MySQL · Redis · MinIO 만
└─ readme.md
```

## 사전 요구사항

- JDK 21 (Gradle toolchain)
- Node.js 20+ (Bun 또는 pnpm)
- (권장) Docker — MySQL / Redis / MinIO
- (선택) 외부 API 키: MarketAux, OpenAI, YouTube, 메일/Solapi/FCM, OAuth 클라이언트
- (선택) 로컬 Llama/감성 모델: [docs/setup/README.md](docs/setup/README.md)

## 로컬 인프라

데이터 스토어만 띄울 때 (앱은 Gradle/Next로 따로 실행):

```bash
docker compose up -d
```

| 서비스 | 접속 |
| --- | --- |
| MySQL | `localhost:3306` / DB `finsight` / 사용자·비번 compose 참고 |
| Redis | `localhost:9379` (컨테이너 6379, 비밀번호 compose 참고) |
| MinIO | API `9000`, 콘솔 `9001` |

웹·배치·프론트까지 한 스택으로 띄울 때 (루트 `.env` 필요):

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

이 Compose는 루트 `docker-compose.yml`과 볼륨 이름(`finsight_finsight-*-data`)을 공유합니다. k8s 매니페스트는 이 스택과 분리되어 있으며, 적용 방법은 [infra/k8s/README.md](infra/k8s/README.md)를 따릅니다.

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

| 모듈 | 프로필 | 포트 |
| --- | --- | --- |
| `web` | `local` | `8080` |
| `core` | `core-local` | 단독 시 `8081` |
| `batch` | `batch-local` | 웹 없음 |

일반 API 개발은 `:web:bootRun`만으로 충분합니다. 뉴스·YouTube 수집은 `:batch:bootRun`이 필요합니다.

### 2) Frontend

`frontend/bun.lock`과 `frontend/pnpm-lock.yaml`이 있습니다. 둘 중 하나만 쓰면 됩니다.

```bash
cd frontend
bun install
bun run dev
```

```bash
cd frontend
pnpm install
pnpm run dev
```

- 개발: `http://localhost:3000`
- 빌드: `bun run build` 또는 `pnpm run build` → `start`
- 캐시 문제 시: `pnpm run dev:clean` (`clean` 후 dev 재시작)

## 환경변수

키 목록의 기준은 루트 `.env.example`입니다. 값은 커밋하지 않습니다.

### Backend

- DB: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` (`MYSQL_*`는 Compose용)
- Redis: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- JWT: `JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_EXPIRATION_PERIOD`, `JWT_REFRESH_EXPIRATION_PERIOD`
- 암호화: `ENCRYPT_KEY`
- 에디터 스토리지: `FINSIGHT_EDITOR_MINIO_ENABLED`, `FINSIGHT_EDITOR_MINIO_ENDPOINT`, `FINSIGHT_EDITOR_MINIO_ACCESS_KEY`, `FINSIGHT_EDITOR_MINIO_SECRET_KEY`, `FINSIGHT_EDITOR_MINIO_BUCKET`
- 뉴스/AI: `MARKETAUX_API_KEY`, `OPENAI_API_KEY`, `OPENAI_API_URL`, `OPENAI_MODEL`, `AI_DEFAULT_MODEL`, `AI_FALLBACK_MODEL`
- 로컬 AI: `OLLAMA_ENABLED`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `LLAMA_PROVIDER`, `HF_TOKEN`, `DJL_ENABLED`, `DJL_MODEL_NAME`
- MCP: `MCP_SERVER_ENABLED`
- YouTube: `YOUTUBE_API_KEY`, `YOUTUBE_API_URL`, `YOUTUBE_MAX_RESULTS_PER_SOURCE`
- 메일: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- SMS: `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`, `SOLAPI_FROM_NUMBER`
- 소셜: `GOOGLE_*`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`, `NAVER_*`
- 프론트 URL·관리자: `APP_FRONTEND_URL`, `FINSIGHT_ADMIN_EMAILS`, `FINSIGHT_ADMIN_USERNAMES`

### Frontend

- `FINSIGHT_API_BASE_URL` (예: `http://localhost:8080`, Compose 안에서는 `http://web:8080`)
- `FINSIGHT_API_PROXY_TIMEOUT_MS`

## 배치 작업

`backend/batch` 스케줄러:

- 뉴스 수집·AI/감정 분석 (`NewsScrapScheduler`)
- YouTube import · AI enrichment (`YoutubeImportScheduler`, `YoutubeAiEnrichmentScheduler`)
- 게시글 모더레이션 (`BoardModerationScheduler`)
- 사용자 상태·비밀번호 만료 (`UserScheduler`, `PasswordExpirationScheduler`)
- 알림 발송·정리 (`NotificationScheduler`)

## Kubernetes

Compose와 분리된 k3s 매니페스트입니다. MySQL·MinIO는 StatefulSet+PVC, Redis는 PVC 없는 캐시, web/frontend는 Deployment, batch는 CronJob입니다. Ingress는 frontend와 web만 노출합니다.

```powershell
cd infra/k8s
copy 01-secrets.example.yaml secrets.yaml
kubectl apply -k .
```

절차·스토리지·S3 전환 메모: [infra/k8s/README.md](infra/k8s/README.md)

## 로컬 AI

Ollama·DJL·Hugging Face 캐시를 D: 드라이브 `tools/`에 두는 절차는 [docs/setup/README.md](docs/setup/README.md)에 있습니다. Python/CUDA 수동 설치는 필요 없고, DJL PyTorch는 Gradle `pytorch-native-cpu`를 사용합니다.

## API 문서

`web` 기동 후:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `/api/v1/health`, Actuator readiness `/actuator/health/readiness`

환경·보안 설정에 따라 경로/접근 정책이 달라질 수 있습니다.

## 아키텍처

### (운영서버)

<img width="1672" height="941" alt="image" src="https://github.com/user-attachments/assets/e97fa7c9-2ca3-4bac-8186-e431e7682de1" />

### (로컬)

<img width="1672" height="941" alt="image" src="https://github.com/user-attachments/assets/18a38f1d-c507-4ee6-8dc6-dae050046d10" />
