# 📈 FinSight

<img width="1136" height="505" alt="image" src="https://github.com/user-attachments/assets/a106a5d4-f7ba-4be0-be3b-daca917b7a2a" />
<img width="1917" height="324" alt="image" src="https://github.com/user-attachments/assets/45ebb257-5bb3-49f0-9f4d-1764d4f7d44a" />

## 프로젝트 소개

FinSight는 미국 경제 뉴스를 주기적으로 크롤링하고, LLM 챗봇 API를 통해 **SPY·QQQ** 같은 ETF와 **비트코인**, **Big7**(애플, 마이크로소프트, 엔비디아 등) 자산에 미치는 영향을 분석하는 서비스입니다.

등록한 자산에 긍정적인 영향을 주는 뉴스를 확인하고, 영어 뉴스를 한국어로 번역·요약해 빠르게 확인할 수 있도록 하는 것이 목표입니다.

## 기술 스택

### Backend
- **Java 21** · **Spring Boot 3.5**
- **모듈 구성**: `web` (REST API) · `core` (비즈니스 로직) · `batch` (배치 작업)
- Web: Spring Web, Security, JPA, Validation, Actuator, SpringDoc(OpenAPI)
- Core: JPA, Redis, JWT, QueryDSL, Bucket4j, Resilience4j, DJL(PyTorch), OpenNLP, Stanford CoreNLP, Spring Mail, Nurigo SMS 등
- Batch: Spring Batch, DJL

### Frontend
- **React 19** · **TypeScript** · **Vite** (Rolldown)
- 패키지 매니저: **pnpm**

## 프로젝트 구조

```
FinSight/
├── backend/          # Spring Boot 멀티 모듈
│   ├── web/          # REST API, Security, OpenAPI
│   ├── core/         # 공통 비즈니스 로직, ML/NLP, DB·캐시
│   └── batch/        # 뉴스 크롤링·처리 배치
├── frontend/         # React + Vite SPA
└── readme.md
```

## 실행 방법

### Backend
```bash
cd backend
./gradlew :web:bootRun
# 또는 특정 모듈 실행
./gradlew :core:bootRun
./gradlew :batch:bootRun
```

### Frontend
```bash
cd frontend
pnpm install
pnpm dev
```

빌드: `pnpm run build`

## 계획

- **1차**: 뉴스 스크래핑, AI 요약, 사용자 관심 종목 등록
- **2차**: 알림 기능, 속보·주요 뉴스 사용자 알림 전달
