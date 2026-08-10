-- Backend B: Enrollment Service 리소스 신청 모델 마이그레이션
-- 기존 데이터와 테이블을 삭제하지 않고 신청 필드를 확장한다.

ALTER TABLE enrollments DROP INDEX IF EXISTS uq_user_course;
ALTER TABLE enrollments DROP INDEX IF EXISTS UKg1muiskd02x66lpy6fqcj6b9q;

ALTER TABLE enrollments
    ADD COLUMN IF NOT EXISTS payment_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS reason VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS quantity INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS desired_date DATE NULL,
    ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS last_event_id VARCHAR(100) NULL;

UPDATE enrollments
SET status = 'REQUESTED'
WHERE status = 'PENDING';

UPDATE enrollments
SET status = 'PROVIDED'
WHERE status = 'ACTIVE';

UPDATE enrollments
SET reason = '기존 교육 플랫폼에서 전환된 신청'
WHERE reason IS NULL OR TRIM(reason) = '';

ALTER TABLE enrollments
    MODIFY COLUMN reason VARCHAR(500) NOT NULL,
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED';

ALTER TABLE enrollments
    ADD UNIQUE INDEX IF NOT EXISTS uq_enrollment_payment_id (payment_id),
    ADD INDEX IF NOT EXISTS idx_enrollment_user_created (user_id, created_at),
    ADD INDEX IF NOT EXISTS idx_enrollment_status (status);
