-- =========================================================
-- 예약/결제 상태값 한글 -> 영어 마이그레이션
-- 적용일: 2026-07-21
--
-- 배경: 팀 DDL은 영어 컨벤션(status DEFAULT 'PENDING')인데 코드가 한글 값을
--       저장하고 있어 관리자 파트 조회와 어긋났음. enum 도입과 함께 영어로 통일.
--
-- ⚠️ 코드를 pull 한 뒤 반드시 각자 DB에서 1회 실행할 것.
--    실행하지 않으면 기존 행을 읽을 때 enum 변환 실패로 예외가 발생함.
--
-- ⚠️ MySQL Workbench에서 Error 1175(safe update mode)가 나면
--    아래 SET 구문까지 포함해 전체를 한 번에 실행할 것.
--    (Preferences를 바꿀 필요 없음 — 세션 단위로만 껐다 켠다)
-- =========================================================

SET SQL_SAFE_UPDATES = 0;

UPDATE RESERVATION SET status = 'PENDING'          WHERE status = '예약중';
UPDATE RESERVATION SET status = 'PAID'             WHERE status = '예약완료';
UPDATE RESERVATION SET status = 'CANCEL_REQUESTED' WHERE status = '취소요청';
UPDATE RESERVATION SET status = 'CANCELED'         WHERE status = '예약취소';
UPDATE RESERVATION SET status = 'EXPIRED'          WHERE status = '예약만료';

UPDATE PAYMENT SET payment_status = 'DONE'     WHERE payment_status = '결제완료';
UPDATE PAYMENT SET payment_status = 'CANCELED' WHERE payment_status = '결제취소';
UPDATE PAYMENT SET payment_status = 'FAILED'   WHERE payment_status = '결제실패';

SET SQL_SAFE_UPDATES = 1;   -- 안전장치 원복 (반드시 다시 켤 것)

-- 확인용: 아래 두 쿼리 결과에 한글 값이 남아 있으면 안 됨
-- SELECT status, COUNT(*) FROM RESERVATION GROUP BY status;
-- SELECT payment_status, COUNT(*) FROM PAYMENT GROUP BY payment_status;


-- =========================================================
-- 취소 흐름 컬럼 (이미 적용돼 있으면 실행 불필요)
-- 팀원 DB에 없을 수 있어 함께 안내용으로 남겨둠
-- =========================================================
-- ALTER TABLE RESERVATION ADD COLUMN cancel_reason VARCHAR(100) NULL;
-- ALTER TABLE RESERVATION ADD COLUMN cancel_requested_at DATETIME NULL;


-- =========================================================
-- 예약 파트 추가 컬럼 (2026-07-27 ~ 07-29 추가분 모음)
-- ⚠️ 이 컬럼들이 없으면 예약 조회·생성이 전부 실패한다(매퍼가 그대로 참조).
--    코드 pull 후 각자 DB에서 1회 실행할 것.
--
-- order_id / payment_type / pg_tid : 결제 정합성 보정 배치용.
--   결제 준비 시 발급한 orderId·시도 PG·(카카오)tid를 기록해, 콜백 유실 시
--   배치가 PG에 실제 결제 여부를 조회할 수 있게 한다.
-- check_out_date : 숙박 기간 예약용. visit_date를 체크인으로 쓰고 체크아웃을
--   이 컬럼에 담는다. 숙박만 값이 있고 맛집/관광지는 NULL(당일 방문) —
--   기존 단일날짜 예약은 1박으로 간주된다.
-- =========================================================
ALTER TABLE RESERVATION
  ADD COLUMN order_id       VARCHAR(100) NULL AFTER status,
  ADD COLUMN payment_type   TINYINT      NULL AFTER order_id,
  ADD COLUMN pg_tid         VARCHAR(60)  NULL AFTER payment_type,
  ADD COLUMN check_out_date DATE         NULL;


-- =========================================================
-- place_type — 로컬 테스트 전용 (PLACE에서 아직 못 받아옴)
-- PLACE.place_type이 아직 tinyint(값 1뿐)라 tour/food 구분이 안 돼서,
-- 임시로 RESERVATION에 저장해 폼 파라미터로 테스트 중이었다.
-- 2026-07-29: PLACE.place_type이 영어값(tour/food/stay)으로 팀 구두 확정되어
-- 아래에서 이 컬럼을 없애고 PLACE 조회 방식으로 교체한다.
-- =========================================================
-- (구) ALTER TABLE RESERVATION ADD COLUMN place_type VARCHAR(20) NULL;


-- =========================================================
-- PLACE.place_type 영어값 전환 + RESERVATION.place_type 제거 (2026-07-29)
--
-- ⚠️ PLACE.place_type은 예약 파트 소유가 아니다. 이 ALTER는 컬럼 타입만 바꾸며,
--    place_id 1~3(예약 테스트 허브가 쓰는 값)만 tour/food/stay로 맞춰둔다.
--    그 외 기존 행은 숫자값이 문자열로 그대로 캐스팅된 상태로 남으므로,
--    실제 콘텐츠 재분류는 PLACE 담당 팀원이 별도로 처리해야 한다.
--    (business/tour/community 쪽 코드가 place_type을 Integer로 읽는 곳이 있다면
--     이 ALTER 이후 타입 불일치로 깨질 수 있음 — 그쪽 코드는 함께 고치지 않았다.)
--
-- 무료예약(0원) 테스트 플래그는 PLACE.place_type과 무관하게 결제 흐름에서
-- freeTest 파라미터로 별도 처리한다(place_type엔 'free' 값이 없음).
-- =========================================================
ALTER TABLE PLACE MODIFY place_type VARCHAR(10) NOT NULL;

UPDATE PLACE SET place_type = 'stay' WHERE place_id = 1;
UPDATE PLACE SET place_type = 'food' WHERE place_id = 2;
UPDATE PLACE SET place_type = 'tour' WHERE place_id = 3;

ALTER TABLE RESERVATION DROP COLUMN place_type;
