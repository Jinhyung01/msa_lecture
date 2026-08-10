package com.lecture.payment.repository;

import com.lecture.payment.entity.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByEnrollmentId(Long enrollmentId);

    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.Status status);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
