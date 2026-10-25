-- =============================================================================
-- finsight_comments: 테이블·컬럼 설명
-- -----------------------------------------------------------------------------
-- 목적
--   - 테이블 설명을 한곳에 모으고 데이터베이스에도 다시 기록한다
-- 대상
--   - Flyway 가 만드는 테이블과, 나중에 덧붙인 컬럼
-- 주의
--   - 컬럼 설명은 컬럼 정의의 COMMENT 에 있다. 여기서 컬럼 타입을 다시 바꾸지 않는다
--   - 아래 ALTER 는 테이블 설명만 갱신한다
-- =============================================================================

-- -----------------------------------------------------------------------------
-- editor_assets
-- 테이블: 에디터 업로드 자산 메타데이터 (바이너리는 외부 스토리지)
--   id: 자산 UUID (클라이언트 또는 서버 생성)
--   object_key: 버킷 내 객체 키(경로 포함)
--   bucket_name: 저장소 버킷 이름
--   original_file_name: 사용자가 올린 원본 파일명
--   content_type: MIME 타입 (예: image/png)
--   file_size: 파일 크기(바이트)
--   uploaded_at: 업로드 완료 시각(밀리초)
-- -----------------------------------------------------------------------------
ALTER TABLE editor_assets COMMENT = '에디터 업로드 자산 메타데이터 (바이너리는 외부 스토리지)';

-- -----------------------------------------------------------------------------
-- ulink_item
-- 테이블: 통합링크 항목
--   id: 업무 키(예: ULK 접두 + 타임스탬프)
--   domain_id: 사이트·테넌트 구분
--   section_code: 통합링크 구분(메뉴/푸터 등)
--   link_group: 같은 구역 내 그룹명
--   link_name: 링크 표시명
--   link_url: 이동 URL
--   link_target: 링크 타겟
--   description: 비고·설명
--   created_at: 등록 시각
--   updated_at: 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE ulink_item COMMENT = '통합링크 항목';

-- -----------------------------------------------------------------------------
-- popup_item
-- 테이블: 레이어 팝업 항목
--   id: 업무 키(예: POP + 타임스탬프)
--   domain_id: 사이트·테넌트 구분
--   title: 팝업 제목
--   file_url: 첨부·연결 파일 URL
--   link_target: 링크 타겟(_blank 등)
--   img_path: 이미지 경로 또는 URL
--   file_name: 표시용 파일명
--   vertical_pos: 세로 위치(픽셀 등, 화면 규약)
--   width_pos: 가로 위치
--   vertical_size: 세로 크기
--   width_size: 가로 크기
--   notice_begin: 노출 시작(문자열, 운영 규약)
--   notice_end: 노출 종료
--   stop_today_hide: Y: 오늘 하루 보지 않기 적용
--   notice_active: Y/N 게시(노출) 활성
--   created_at: 등록 시각
--   updated_at: 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE popup_item COMMENT = '레이어 팝업 항목';

-- -----------------------------------------------------------------------------
-- mainimg_item
-- 테이블: 메인 이미지(히어로) 항목
--   id: 업무 키(예: IMG + 타임스탬프)
--   domain_id: 사이트·테넌트 구분
--   image_name: 슬롯·이미지 표시명
--   image: 이미지 URL 또는 경로
--   image_file: 업로드 파일 경로·식별자
--   description: 부가 설명
--   reflect_yn: Y/N 화면 반영(노출) 여부
--   created_at: 등록 시각
--   updated_at: 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE mainimg_item COMMENT = '메인 이미지(히어로) 항목';

-- -----------------------------------------------------------------------------
-- email_verifications
-- 테이블: 이메일 OTP 인증 챌린지
--   id: 이메일 인증 챌린지 PK
--   purpose: 인증 용도 코드 (회원가입/아이디찾기/비밀번호찾기 등)
--   purpose_label: 인증 용도 표시명
--   email: 인증 대상 이메일
--   challenge_id: 챌린지 토큰(UUID). 프론트 verify URL 경로에 사용
--   code_hash: 인증코드 해시 (평문 저장 금지)
--   verified_code: 검증에 성공한 코드(감사/디버그용, 선택)
--   last_entered_code: 마지막 입력 코드(실패 추적)
--   request_ip: 인증 요청 IP
--   request_location: IP 기반 대략 위치
--   requested_at: 인증 요청 시각
--   expires_at: 코드 만료 시각
--   verified_at: 코드 검증 성공 시각
--   consumed_at: 가입/재설정 등 최종 사용 시각
--   attempt_count: 코드 입력 시도 횟수
--   status: 챌린지 상태 (PENDING/VERIFIED/EXPIRED/CONSUMED 등)
--   created_at: 레코드 생성 시각
--   updated_at: 레코드 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE email_verifications COMMENT = '이메일 OTP 인증 챌린지';

-- -----------------------------------------------------------------------------
-- email_logs
-- 테이블: 메일 발송 이력
--   id: 메일 발송 이력 PK
--   recipient: 수신 이메일 주소
--   subject: 메일 제목
--   template_type: 템플릿/발송 유형 코드 (예: verification-code)
--   purpose: 발송 용도 enum (VERIFICATION_SIGNUP, NEWS_ALERT 등)
--   purpose_label: 발송 용도 한글 라벨
--   status: 발송 상태 (SENT, FAILED 등)
--   from_address: 발신 이메일 주소(SMTP username)
--   user_id: 관련 사용자 ID (비로그인이면 NULL 가능)
--   actor_type: 요청 주체 (ANONYMOUS, USER, SYSTEM, ADMIN)
--   actor_user_id: 실제 조작한 사용자 ID(관리자/본인 등)
--   request_ip: 요청 클라이언트 IP (비로그인 추적용)
--   request_location: IP 기반 대략 위치 문자열
--   user_agent: 요청 User-Agent
--   body_preview: 본문 미리보기(HTML 제거, OTP/인증코드 마스킹)
--   error_message: 발송 실패 시 오류 메시지
--   related_ref: 연관 참조키 (예: email verification challengeId)
--   sent_at: SMTP 전송 성공 시각
--   delivered_at: 수신 서버 전달 시각(확장용)
--   opened_at: 열람 시각(확장용)
--   clicked_at: 링크 클릭 시각(확장용)
--   bounced_at: 반송 시각(확장용)
--   bounce_reason: 반송 사유(확장용)
--   created_at: 이력 생성 시각
--   updated_at: 이력 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE email_logs COMMENT = '메일 발송 이력';

-- -----------------------------------------------------------------------------
-- email_templates
-- 테이블: 이메일 HTML 템플릿 저장소
--   id: 이메일 템플릿 PK
--   name: 템플릿 고유 이름 (예: verification-code, welcome)
--   subject: 메일 제목 템플릿 ({{appName}} 등 변수 가능)
--   text_content: 텍스트 본문 템플릿(선택)
--   active: 1=사용, 0=비활성(발송 시 폴백)
--   created_at: 템플릿 생성 시각
--   updated_at: 템플릿 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE email_templates COMMENT = '이메일 HTML 템플릿 저장소';

-- -----------------------------------------------------------------------------
-- welcome_email_jobs
-- 테이블: 회원가입 축하 메일 발송 큐. 가입 후 2시간 이내
--   id: 축하 메일 작업 PK
--   user_id: 대상 사용자 ID (users.id)
--   registered_at: 회원가입 시각 (발송 윈도우 기준점)
--   deadline_at: 발송 마감 시각 (registered_at + 2시간)
--   scheduled_at: 다음 발송 시도 예정 시각
--   status: 작업 상태 (PENDING|SENT|EXPIRED|FAILED)
--   attempt_count: 발송 시도 횟수
--   last_error: 마지막 실패 사유
--   sent_at: 발송 성공 시각
--   created_at: 작업 생성 시각
--   updated_at: 작업 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE welcome_email_jobs COMMENT = '회원가입 축하 메일 발송 큐. 가입 후 2시간 이내';

-- -----------------------------------------------------------------------------
-- live_vod_favorites
-- 테이블: LIVE/VOD 즐겨찾기. 사용자·영상당 1건
--   id: 즐겨찾기 PK
--   video_id: YouTube videoId
--   user_email: 즐겨찾기한 사용자 이메일
--   created_at: 즐겨찾기 등록 시각
-- -----------------------------------------------------------------------------
ALTER TABLE live_vod_favorites COMMENT = 'LIVE/VOD 즐겨찾기. 사용자·영상당 1건';

-- -----------------------------------------------------------------------------
-- live_vod_comments
-- 테이블: LIVE/VOD 댓글·대댓글. parent_id로 1단계 답글
--   id: 댓글 PK
--   video_id: YouTube videoId
--   user_email: 작성자 이메일
--   author_nickname: 작성자 닉네임(표시용)
--   content: 댓글 본문
--   parent_id: 부모 댓글 ID (NULL이면 최상위 댓글)
--   created_at: 작성 시각
--   updated_at: 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE live_vod_comments COMMENT = 'LIVE/VOD 댓글·대댓글. parent_id로 1단계 답글';

-- -----------------------------------------------------------------------------
-- live_vod_reactions
-- 테이블: LIVE/VOD 영상 좋아요·싫어요
--   id: 반응 PK
--   video_id: YouTube videoId
--   user_email: 반응한 사용자 이메일
--   reaction_type: LIKE 또는 DISLIKE
--   created_at: 등록 시각
--   updated_at: 변경 시각
-- -----------------------------------------------------------------------------
ALTER TABLE live_vod_reactions COMMENT = 'LIVE/VOD 영상 좋아요·싫어요';

-- -----------------------------------------------------------------------------
-- live_vod_comment_reactions
-- 테이블: LIVE/VOD 댓글·대댓글 좋아요·싫어요
--   id: 댓글 반응 PK
--   comment_id: live_vod_comments.id
--   user_email: 반응한 사용자 이메일
--   reaction_type: LIKE 또는 DISLIKE
--   created_at: 등록 시각
--   updated_at: 변경 시각
-- -----------------------------------------------------------------------------
ALTER TABLE live_vod_comment_reactions COMMENT = 'LIVE/VOD 댓글·대댓글 좋아요·싫어요';

-- -----------------------------------------------------------------------------
-- board_moderation_run
-- 테이블: 게시판 신고 과다 숨김 실행 이력 (수동·배치)
--   id: 모더레이션 실행 이력 PK
--   triggered_by: 실행 경로 (MANUAL: 관리자 수동, BATCH: 스케줄 배치)
--   actor_email: 수동 실행 관리자 이메일 (BATCH면 NULL 가능)
--   report_threshold: 적용한 신고 건수 임계값 (이상이면 숨김 대상)
--   hidden_count: 이번 실행에서 HIDDEN으로 바꾼 게시글 건수
--   created_at: 실행(이력 저장) 시각
-- -----------------------------------------------------------------------------
ALTER TABLE board_moderation_run COMMENT = '게시판 신고 과다 숨김 실행 이력 (수동·배치)';

-- -----------------------------------------------------------------------------
-- inbox_notification
-- 테이블: 사용자 인앱 알림함
--   id: 인앱 알림 PK
--   recipient_user_id: 수신 사용자 ID
--   category: 카테고리 (YOUTUBE|NEWS|COMMENT|QNA|WATCHLIST|ADMIN)
--   actor_user_id: 행위자 사용자 ID (시스템이면 NULL)
--   actor_name: 행위자 표시명
--   actor_avatar_url: 행위자 아바타 URL
--   title: 알림 제목/본문 요약
--   body: 부가 스니펫(댓글 미리보기 등)
--   link_url: 클릭 시 이동 경로
--   ref_type: 연관 리소스 타입 (BOARD|NEWS|COMMENT|MEDIA 등)
--   ref_id: 연관 리소스 ID
--   is_read: 읽음 여부 (0:미읽음, 1:읽음)
--   read_at: 읽은 시각
--   deleted: 소프트 삭제 (0:활성, 1:삭제)
--   created_at: 생성 시각
--   updated_at: 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE inbox_notification COMMENT = '사용자 인앱 알림함';

-- -----------------------------------------------------------------------------
-- inbox_settings
-- 테이블: 인앱 알림 카테고리 수신 설정
--   user_id: 사용자 ID (PK)
--   youtube_enabled: 유튜브(미디어) 알림 수신
--   news_enabled: 뉴스 알림 수신
--   comment_enabled: 댓글 알림 수신
--   qna_enabled: QnA 알림 수신
--   created_at: 생성 시각
--   updated_at: 수정 시각
-- -----------------------------------------------------------------------------
ALTER TABLE inbox_settings COMMENT = '인앱 알림 카테고리 수신 설정';

-- -----------------------------------------------------------------------------
-- sms_settings
-- 테이블: SMS 발송 설정 (관리자 싱글톤)
--   id: 설정 PK (싱글톤 1행, 고정값 1)
--   enabled: SMS 마스터 스위치 (0이면 전체 자동발송 스킵)
--   news_alert_enabled: 뉴스 알림 자동 SMS 허용
--   otp_enabled: OTP SMS 발송 허용
--   account_recovery_enabled: 계정 복구 SMS 발송 허용
--   system_alert_enabled: 시스템 알림 SMS 발송 허용
--   notification_enabled: 일반 알림 SMS 발송 허용
--   created_at: 생성 시각
-- -----------------------------------------------------------------------------
ALTER TABLE sms_settings COMMENT = 'SMS 발송 설정 (관리자 싱글톤)';

-- -----------------------------------------------------------------------------
-- sms_send_log
-- 테이블: SMS 발송 이력 (성공·실패·스킵)
--   id: SMS 발송 이력 PK
--   to_phone: 수신 전화번호
--   from_phone: 발신 전화번호
--   content_preview: 본문 미리보기 (OTP는 [REDACTED])
--   status: 발송 결과 (SENT|FAILED|SKIPPED)
--   external_message_id: Solapi(외부) 메시지 ID
--   error_message: 실패 또는 스킵 사유
--   recipient_user_id: 수신 사용자 ID (회원 매핑 시)
--   actor_user_id: 발송 요청자(관리자) 사용자 ID (수동 발송 시)
--   created_at: 발송(기록) 시각
-- -----------------------------------------------------------------------------
ALTER TABLE sms_send_log COMMENT = 'SMS 발송 이력 (성공·실패·스킵)';

-- -----------------------------------------------------------------------------
-- news_reactions
-- 테이블: 뉴스 좋아요·싫어요
--   id: 반응 PK
--   news_id: 뉴스 ID
--   user_email: 반응한 사용자 이메일
--   reaction_type: LIKE 또는 DISLIKE
--   created_at: 등록 시각
--   updated_at: 변경 시각
-- -----------------------------------------------------------------------------
ALTER TABLE news_reactions COMMENT = '뉴스 좋아요·싫어요';

-- -----------------------------------------------------------------------------
-- portfolio_asset
-- 테이블: 사용자 포트폴리오 자산
--   id: 자산 PK
--   user_id: 소유 사용자 ID
--   asset_name: 자산 이름
--   asset_kind: ASSET 또는 LIABILITY
--   asset_category: STOCK_ETF|DEPOSIT|PENSION|CASH|OTHER|LIABILITY
--   valuation_amount: 평가금액(원)
--   profit_rate: 손익률(%), 없으면 NULL
--   created_at: 등록 시각
--   updated_at: 수정 시각
--   custodian: 계좌 또는 보관처
--   acquisition_amount: 매수원금 또는 취득가
--   quantity: 보유 수량
--   unit_price: 현재 단가
--   memo: 메모
-- -----------------------------------------------------------------------------
ALTER TABLE portfolio_asset COMMENT = '사용자 포트폴리오 자산';

-- -----------------------------------------------------------------------------
-- portfolio_snapshot
-- 테이블: 사용자 포트폴리오 월별 순자산 기록
--   id: 월별 기록 PK
--   user_id: 소유 사용자 ID
--   snapshot_month: 기록 월
--   total_assets: 기록 시점 총자산(원)
--   total_liabilities: 기록 시점 총부채(원)
--   net_worth: 기록 시점 순자산(원)
--   recorded_at: 기록 시각
--   memo: 이번 달 메모
--   record_status: 기록 상태
-- -----------------------------------------------------------------------------
ALTER TABLE portfolio_snapshot COMMENT = '사용자 포트폴리오 월별 순자산 기록';

-- -----------------------------------------------------------------------------
-- portfolio_share
-- 테이블: 커뮤니티에 공개하는 포트폴리오 공유 카드
--   id: 공유 카드 PK
--   user_id: 공유한 사용자 ID. 더미는 0
--   author_name: 사용자명
--   message: 공유 설명
--   visibility: PUBLIC, FOLLOWERS, PRIVATE
--   goal_label: 목표 이름
--   amount_label: 카드에 보이는 금액
--   progress_percent: 목표 진행률 0~100
--   month_rate_label: 이번 달 증감률
--   goal_rate_label: 목표 달성률
--   show_asset: 자산 칩 표시
--   show_debt: 부채 칩 표시
--   cheer_count: 응원 수
--   created_at: 공유 시각
--   moderation_status: OPEN, WARN, BLIND, REMOVED
--   public_detail: 공개 Snapshot JSON. 계좌·보관처는 제외
-- -----------------------------------------------------------------------------
ALTER TABLE portfolio_share COMMENT = '커뮤니티에 공개하는 포트폴리오 공유 카드';

-- -----------------------------------------------------------------------------
-- portfolio_share_goal
-- 테이블: 이번 주 인기 목표
--   id: 인기 목표 PK
--   goal_label: 목표 이름
--   participant_count: 참여 인원
--   sort_order: 표시 순서
-- -----------------------------------------------------------------------------
ALTER TABLE portfolio_share_goal COMMENT = '이번 주 인기 목표';

-- -----------------------------------------------------------------------------
-- portfolio_share_reaction
-- 테이블: 포트폴리오 공유 좋아요·싫어요
--   id: 반응 PK
--   share_id: portfolio_share.id
--   user_id: 반응한 사용자 ID
--   reaction_type: LIKE 또는 DISLIKE
--   created_at: 반응 시각
-- -----------------------------------------------------------------------------
ALTER TABLE portfolio_share_reaction COMMENT = '포트폴리오 공유 좋아요·싫어요';

-- -----------------------------------------------------------------------------
-- portfolio_share_report
-- 테이블: 포트폴리오 공유 신고
--   id: 신고 PK
--   share_id: portfolio_share.id
--   user_id: 신고한 사용자 ID
--   created_at: 신고 시각
--   reason: 신고 사유. 오픈채팅 URL, 과장 수익문구, 리딩방 패턴, 투자 사기, 개인정보, 광고
--   note: 회원이 입력한 신고 사유
-- -----------------------------------------------------------------------------
ALTER TABLE portfolio_share_report COMMENT = '포트폴리오 공유 신고';

-- -----------------------------------------------------------------------------
-- portfolio_share_comment
-- 테이블: 포트폴리오 공유 댓글
--   id: 댓글 PK
--   share_id: portfolio_share.id
--   user_id: 작성 사용자 ID
--   author_name: 작성 당시 이름
--   content: 댓글 본문
--   created_at: 작성 시각
-- -----------------------------------------------------------------------------
ALTER TABLE portfolio_share_comment COMMENT = '포트폴리오 공유 댓글';

-- -----------------------------------------------------------------------------
-- news_crawl_incident
-- 테이블: 제공처별 뉴스 수집 장애 사건
--   id: 장애 사건 PK
--   provider: 뉴스 제공처. MARKETAUX, ALPHA_VANTAGE, YAHOO_FINANCE
--   status: OPEN, ANALYZED, REJECTED, APPROVED, RUNNING, SUCCEEDED, FAILED
--   cause: CONNECTION, TIMEOUT, HTTP_401, HTTP_403, HTTP_404, HTTP_429, HTTP_5XX, PARSE, UNEXPECTED
--   http_status: HTTP 상태 코드. 연결 실패나 타임아웃이면 NULL
--   retry_after_seconds: 429 Retry-After 초. 숫자 헤더가 아니면 NULL
--   message: 원인 예외 요약. 쿼리의 API 키는 마스킹
--   retryable: 서버 정책상 재수집 허용. 1이면 허용
--   occurred_at: 장애를 관측한 시각
--   analyzed_at: 규칙 분류를 마친 시각
--   model_note: 모델 서술. 모델 실패 시 규칙 분류만 유지한다는 문구
--   approved_by: 승인자 계정
--   approved_at: 승인 시각
--   fencing_token: 실행 선점 토큰. 재선점하면 증가
-- -----------------------------------------------------------------------------
ALTER TABLE news_crawl_incident COMMENT = '제공처별 뉴스 수집 장애 사건';

-- -----------------------------------------------------------------------------
-- news_crawl_attempt
-- 테이블: 장애 사건별 재수집 시도
--   id: 실행 시도 PK
--   incident_id: news_crawl_incident.id
--   status: RUNNING, SUCCEEDED, FAILED, SKIPPED
--   executor: 재수집을 실행한 계정
--   started_at: 시도 시작 시각
--   finished_at: 시도 종료 시각
--   saved_count: 이번 시도에서 저장한 뉴스 수
--   duplicate_count: 이미 있어 건너뛴 뉴스 수
--   result_message: 저장 확인 또는 실패 사유
-- -----------------------------------------------------------------------------
ALTER TABLE news_crawl_attempt COMMENT = '장애 사건별 재수집 시도';

-- -----------------------------------------------------------------------------
-- news_crawl_provider_lock
-- 테이블: 제공처별 뉴스 수집 실행 잠금
--   provider: 잠금 대상 제공처. PK
--   owner: 잠금 소유자. batch- 또는 incident- 로 시작
--   fencing_token: 현재 소유자의 펜싱 토큰. 만료 후 재선점하면 증가
--   expires_at: 잠금 만료 시각. 지나면 다른 소유자가 선점
-- -----------------------------------------------------------------------------
ALTER TABLE news_crawl_provider_lock COMMENT = '제공처별 뉴스 수집 실행 잠금';

-- -----------------------------------------------------------------------------
-- news
--   news_provider: 뉴스 제공자
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- boards
--   highlighted: 공지 강조 여부
-- -----------------------------------------------------------------------------
