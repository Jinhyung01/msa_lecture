package com.lecture.course.repository;

import com.lecture.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // 강사(등록 관리자)별 리소스 조회 - 수정 시 소유권 확인용
    List<Course> findByInstructorId(Long instructorId);

    // 연관 리소스 후보 조회: 카테고리 목록 중 하나 + 상태 + 제외 ID 목록
    List<Course> findByCategoryInAndStatusAndIdNotIn(
            List<Course.Category> categories,
            Course.Status status,
            List<Long> excludeIds
    );

    // 제외 ID가 없을 때 (진행/제공 이력이 아예 없는 신규 사용자 케이스 대비)
    List<Course> findByCategoryInAndStatus(
            List<Course.Category> categories,
            Course.Status status
    );
}
