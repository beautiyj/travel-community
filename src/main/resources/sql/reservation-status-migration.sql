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
