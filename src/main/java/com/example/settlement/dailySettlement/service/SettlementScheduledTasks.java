package com.example.settlement.dailySettlement.service;

import com.example.settlement.dailySettlement.entity.Settlement;
import com.example.settlement.dailySettlement.repository.SettlementRepository;
import com.example.settlement.payment.entity.Payment;
import com.example.settlement.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Component
public class SettlementScheduledTasks {
    private final PaymentRepository paymentRepository;

    private final SettlementRepository settlementRepository;

    @Autowired
    public SettlementScheduledTasks(PaymentRepository paymentRepository, SettlementRepository settlementRepository) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
    }
    // 매일 오전 3시 실행
    @Scheduled(cron = "0 * * * * ?")
    public void dailySettlement() {
        // 어제의 날짜를 가져옴
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 어제의 시작 시각 설정 (2024-09-05 00:00:00)
        LocalDateTime startDate = yesterday.atStartOfDay();

        // 어제의 끝 시각 설정 (2024-09-05 23:59:59)
        LocalDateTime endDate = yesterday.atTime(LocalTime.of(23, 59, 59));

        List<Payment> paymentList = paymentRepository.findByPaymentDateBetween(startDate, endDate);

        // 파트너 ID별로 결제 금액을 집계
        Map<Long, BigDecimal> settlementMap = paymentList.stream()
                .collect(Collectors.groupingBy(
                        Payment::getPartnerId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Payment::getPaymentAmount,
                                BigDecimal::add
                        )
                ));


        // ForkJoinPool 커스텀 하여 처리 : CPU 코어 수 + 여유 스레드 수
        ForkJoinPool customThreadPool = new ForkJoinPool(10);  // 10개 스레드로 구성된 풀

        try {
            customThreadPool.submit(() ->
                    settlementMap.entrySet().parallelStream()
                            .forEach(entry -> {
                                Settlement settlement = new Settlement();
                                settlement.setPartnerId(entry.getKey());
                                settlement.setTotalAmount(entry.getValue());
                                settlement.setStatus("completed");
                                settlement.setPaymentDate(yesterday);
                                settlement.setCreatedAt(LocalDateTime.now());
                                settlement.setUpdatedAt(LocalDateTime.now());
                                settlementRepository.save(settlement);
                            })
            ).get(); // get()을 호출하여 작업이 완료될 때까지 기다림
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            customThreadPool.shutdown();  // 작업이 끝나면 풀을 종료
        }

        System.out.println("크론 표현식을 사용해 특정 시간에 실행되는 작업");
    }
}
