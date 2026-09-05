-- =============================================================================
-- V19: 데모 계정 system2 → guest 이름 정리
-- -----------------------------------------------------------------------------
-- 목적
--   - users.username / users.nickname 이 'system2' 인 행을 'guest' 로 바꾼다.
--   - 로그인 화면·헤더에 보이던 예전 데모 아이디를 guest 로 통일한다.
--
-- username
--   - system2 → guest
--   - 이미 username='guest' 행이 있으면 UNIQUE 충돌을 피하기 위해
--     guest 가 없을 때만 username 을 변경한다.
--
-- nickname
--   - nickname='system2' 이면 guest 로 변경한다.
--   - username 이 system2 로 남은 경우(이미 guest username 존재)에도
--     표시용 nickname 만 guest 로 맞춘다.
--
-- 연관
--   - AuthenticationService.resolveLoginUser 에서 system2 로그인 입력을
--     guest 로 해석한다.
--   - 프론트 LoginForm 기본 아이디는 guest.
-- =============================================================================

UPDATE users
SET nickname = 'guest'
WHERE nickname = 'system2';

UPDATE users
SET username = 'guest'
WHERE username = 'system2'
  AND NOT EXISTS (
      SELECT 1
      FROM (SELECT id FROM users WHERE username = 'guest') AS existing_guest
  );

UPDATE users
SET nickname = 'guest'
WHERE username = 'system2'
  AND (nickname IS NULL OR nickname = '' OR nickname = 'system2');
