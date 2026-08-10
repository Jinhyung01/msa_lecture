package com.lecture.course.service;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.exception.ForbiddenException;
import com.lecture.course.exception.ResourceNotFoundException;
import com.lecture.course.repository.CourseRepository;
import com.lecture.course.repository.CourseSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 담당: 백엔드 A
 * 리소스 카탈로그(Course) CRUD + 연관 리소스 후보 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * 리소스 등록 (API-04) - 관리자만 가능, 권한 체크는 Controller에서 X-User-Role로 수행
     */
    @Transactional
    public CourseDto.CourseResponse createCourse(CourseDto.SaveRequest request, Long instructorId) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .instructorId(instructorId)
                .enrollmentCount(0)
                .status(Course.Status.ACTIVE)
                .build();

        return CourseDto.CourseResponse.from(courseRepository.save(course));
    }

    /**
     * 리소스 수정 (API-05) - 본인이 등록한 리소스만 수정 가능
     */
    @Transactional
    public CourseDto.CourseResponse updateCourse(Long id, CourseDto.SaveRequest request, Long instructorId) {
        Course course = findCourseById(id);
        if (!course.getInstructorId().equals(instructorId)) {
            throw new ForbiddenException("본인이 등록한 리소스만 수정할 수 있습니다: " + id);
        }
        course.update(request.getTitle(), request.getDescription(), request.getCategory(), request.getPrice());
        return CourseDto.CourseResponse.from(course);
    }

    /**
     * 신청 가능 상태 변경 (ACTIVE/INACTIVE)
     */
    @Transactional
    public CourseDto.CourseResponse changeStatus(Long id, Course.Status status) {
        Course course = findCourseById(id);
        course.changeStatus(status);
        return CourseDto.CourseResponse.from(course);
    }

    /**
     * 리소스 단건 조회 (API-03)
     */
    public CourseDto.CourseResponse getCourse(Long id) {
        return CourseDto.CourseResponse.from(findCourseById(id));
    }

    /**
     * 리소스 목록 조회 (API-02)
     * category 생략 시 전체, status 생략 시 ACTIVE, keyword는 title+description 검색
     */
    public List<CourseDto.CourseResponse> getCourses(Course.Category category, Course.Status status, String keyword) {
        Course.Status effectiveStatus = (status != null) ? status : Course.Status.ACTIVE;

        Specification<Course> spec = Specification
                .where(CourseSpecification.hasCategory(category))
                .and(CourseSpecification.hasStatus(effectiveStatus))
                .and(CourseSpecification.hasKeyword(keyword));

        return courseRepository.findAll(spec).stream()
                .map(CourseDto.CourseResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 리소스 존재 여부 확인 (Enrollment Service → Course Service 내부 호출용)
     */
    public boolean existsCourse(Long id) {
        return courseRepository.existsById(id);
    }

    /**
     * 제공 완료 횟수 증가 (Enrollment Service가 resource.provided 소비 후 호출)
     * 중복 이벤트에 대한 멱등 처리는 Enrollment Service가 processed_events로 보장한다.
     */
    @Transactional
    public void increaseEnrollmentCount(Long courseId) {
        Course course = findCourseById(courseId);
        course.increaseEnrollmentCount();
    }

    /**
     * 연관 리소스 후보 조회 (API-18, Recommend Service 내부 호출용)
     * categories 중 하나에 속하고 ACTIVE이며 excludeIds에 없는 리소스를,
     * 제공 완료 횟수(enrollmentCount) 내림차순으로 반환한다.
     */
    public List<CourseDto.CourseResponse> getRecommendCandidates(
            List<Course.Category> categories, List<Long> excludeIds) {

        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }

        List<Course> courses = (excludeIds == null || excludeIds.isEmpty())
                ? courseRepository.findByCategoryInAndStatus(categories, Course.Status.ACTIVE)
                : courseRepository.findByCategoryInAndStatusAndIdNotIn(categories, Course.Status.ACTIVE, excludeIds);

        return courses.stream()
                .sorted((a, b) -> b.getEnrollmentCount() - a.getEnrollmentCount())
                .map(CourseDto.CourseResponse::from)
                .collect(Collectors.toList());
    }

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("리소스를 찾을 수 없습니다: " + id));
    }
}
