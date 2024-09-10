package com.example.settlement.payment.service;

import com.example.settlement.payment.entity.Payment;
import com.example.settlement.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * 모든 결제 내역 조회
     *
     * @return 모든 Payment 엔티티 리스트
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * ID로 결제 내역 조회
     *
     * @param id 결제의 고유 ID
     * @return Payment 엔티티 (존재하지 않을 경우 Optional.empty())
     */
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    /**
     * 새로운 결제 내역 저장
     *
     * @param payment 저장할 Payment 엔티티
     * @return 저장된 Payment 엔티티
     */
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    /**
     * 결제 내역 삭제
     *
     * @param id 삭제할 결제의 고유 ID
     */
    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
    }
}
