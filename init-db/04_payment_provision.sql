-- 백엔드 C: payments 테이블을 "제공 작업(Provision)" 모델로 확장한다.
-- 기존 테이블과 데이터는 삭제하지 않고 컬럼만 추가/수정한다.
-- (다른 담당자의 02/03 SQL과 파일을 분리해 충돌을 피한다.)

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS enrollment_id BIGINT NULL COMMENT '신청 ID' AFTER course_id,
    ADD COLUMN IF NOT EXISTS manager_id BIGINT NULL COMMENT '처리 관리자 ID' AFTER status,
    ADD COLUMN IF NOT EXISTS manager_memo VARCHAR(500) NULL COMMENT '관리자 메모' AFTER manager_id,
    ADD COLUMN IF NOT EXISTS result_memo VARCHAR(1000) NULL COMMENT '제공 결과' AFTER manager_memo,
    ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(500) NULL COMMENT '반려 사유' AFTER result_memo,
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500) NULL COMMENT '취소 사유' AFTER reject_reason,
    ADD COLUMN IF NOT EXISTS provided_at DATETIME(6) NULL COMMENT '제공 완료일' AFTER cancel_reason;

ALTER TABLE payments
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
        COMMENT 'REQUESTED | ACCEPTED | PROVISIONING | PROVIDED | REJECTED | CANCELLED';

ALTER TABLE payments
    ADD UNIQUE INDEX IF NOT EXISTS uq_payments_enrollment_id (enrollment_id);
