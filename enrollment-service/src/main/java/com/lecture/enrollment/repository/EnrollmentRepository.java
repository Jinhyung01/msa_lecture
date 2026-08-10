package com.lecture.enrollment.repository;

import com.lecture.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Enrollment> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, Enrollment.Status status);

    List<Enrollment> findAllByOrderByCreatedAtDesc();

    List<Enrollment> findByStatusOrderByCreatedAtDesc(Enrollment.Status status);

    boolean existsByUserIdAndCourseIdAndStatusIn(
            Long userId,
            Long courseId,
            List<Enrollment.Status> statuses);

    List<Enrollment> findByUserIdAndStatus(Long userId, Enrollment.Status status);
}
