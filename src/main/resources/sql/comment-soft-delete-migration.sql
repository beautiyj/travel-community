-- =========================================================
-- 댓글 소프트 삭제 마이그레이션
-- 적용일: 2026-07-28
--
-- 배경: 원댓글을 삭제하면 fk_comment_parent 의 ON DELETE CASCADE 로 인해
--       딸린 대댓글까지 함께 물리 삭제되던 문제. 원댓글은 이제 물리 삭제하지
--       않고 deleted_at 만 채우는 소프트 삭제로 전환하여 대댓글을 보존함
--       (member.deleted_at 과 동일한 패턴).
--
-- ⚠️ 코드를 pull 한 뒤 반드시 각자 DB에서 1회 실행할 것.
--    실행하지 않으면 deleteComment 호출 시 존재하지 않는 컬럼 오류가 발생함.
-- =========================================================

ALTER TABLE comment ADD COLUMN deleted_at DATETIME NULL AFTER updated_at;
