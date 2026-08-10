package com.lecture.course.repository;

import com.lecture.course.entity.Course;
import org.springframework.data.jpa.domain.Specification;

/**
 * GET /api/courses 필터 규칙 (명세서 8. API-02):
 * - category 생략 시 전체
 * - status 생략 시 ACTIVE
 * - keyword는 title과 description 검색
 */
public class CourseSpecification {

    private CourseSpecification() {
    }

    public static Specification<Course> hasCategory(Course.Category category) {
        return (root, query, cb) ->
                category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Course> hasStatus(Course.Status status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Course> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
        };
    }
}
