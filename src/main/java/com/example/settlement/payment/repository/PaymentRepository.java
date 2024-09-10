package com.example.settlement.payment.repository;

import com.example.settlement.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    // 기본 CRUD 메서드는 JpaRepository가 제공
}
