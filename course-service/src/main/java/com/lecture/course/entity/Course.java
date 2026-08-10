package com.lecture.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 담당: 백엔드 A
 * 기존 Course(강좌) 테이블을 IT 리소스 카탈로그로 사용한다.
 * 명세서 5.2 courses — 리소스 카탈로그: 테이블/컬럼명은 유지하고 category Enum 값만 변경한다.
 */
@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 리소스명
    @Column(nullable = false)
    private String title;

    // 설명 및 제공 조건
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 예상 비용
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // 등록 관리자 ID (users 테이블 참조 - 직접 JOIN 없이 ID만 보관)
    @Column(nullable = false)
    private Long instructorId;

    // 제공 완료 횟수 (연관 리소스 정렬 기준)
    @Column(nullable = false)
    @Builder.Default
    private Integer enrollmentCount = 0;

    // 신청 가능 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * 리소스 카테고리 (통합 개발 구현 명세서 5.2 확정 Enum)
     */
    public enum Category {
        SERVER, CLOUD, LICENSE, DATA, ACCOUNT, NETWORK, SECURITY, PHYSICAL_DEVICE, OTHER
    }

    public enum Status {
        ACTIVE, INACTIVE
    }

    public void increaseEnrollmentCount() {
        this.enrollmentCount++;
    }

    public void update(String title, String description, Category category, BigDecimal price) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
    }

    public void changeStatus(Status status) {
        this.status = status;
    }
}
