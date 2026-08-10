-- 담당: 백엔드 A (user-service / course-service / recommend-service)
-- 사내 IT 리소스 플랫폼 전환: courses 테이블 카테고리 값 변경 문서화
-- category 컬럼은 이미 VARCHAR(50)이라 스키마 변경은 없다.
-- 값 범위만 다음 Enum으로 바뀐다: SERVER, CLOUD, LICENSE, DATA, ACCOUNT, NETWORK, SECURITY, PHYSICAL_DEVICE, OTHER
-- 기존 테이블/데이터는 삭제하지 않는다.

ALTER TABLE courses
    MODIFY COLUMN category VARCHAR(50) NOT NULL
    COMMENT 'SERVER|CLOUD|LICENSE|DATA|ACCOUNT|NETWORK|SECURITY|PHYSICAL_DEVICE|OTHER';

ALTER TABLE courses
    MODIFY COLUMN title VARCHAR(255) NOT NULL COMMENT '리소스명';

ALTER TABLE courses
    MODIFY COLUMN description TEXT COMMENT '설명 및 제공 조건';

ALTER TABLE courses
    MODIFY COLUMN price DECIMAL(10,2) NOT NULL COMMENT '예상 비용';

ALTER TABLE courses
    MODIFY COLUMN enrollment_count INT NOT NULL DEFAULT 0 COMMENT '제공 완료 횟수';

ALTER TABLE courses
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE';

ALTER TABLE users
    MODIFY COLUMN role VARCHAR(20) NOT NULL COMMENT 'STUDENT(신청자) | INSTRUCTOR(리소스 관리자)';
