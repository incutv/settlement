package com.example.settlement.dailySettlement.service;

import com.example.settlement.dailySettlement.entity.Settlement;
import com.example.settlement.dailySettlement.repository.SettlementRepository;
import com.example.settlement.payment.entity.Payment;
import com.example.settlement.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SettlementScheduledTasks {
    private final PaymentRepository paymentRepository;

    private final SettlementRepository settlementRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SettlementScheduledTasks(PaymentRepository paymentRepository, SettlementRepository settlementRepository, JdbcTemplate jdbcTemplate) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    // 매일 오전 3시 실행
    //@Scheduled(cron = "0 * * * * ?")
    //@SchedulerLock(name = "ScheduledTask_run")
    public void dailySettlement() {
        // 어제의 날짜를 가져옴
        LocalDate yesterday = LocalDate.now().minusDays(1);
        // 어제의 시작 시각 설정 (2024-09-05 00:00:00)
        LocalDateTime startDate = yesterday.atStartOfDay();
        // 어제의 끝 시각 설정 (2024-09-05 23:59:59)
        LocalDateTime endDate = yesterday.atTime(LocalTime.of(23, 59, 59));

        // 해당 기간 동안의 결제 내역 조회 및 집계
        Map<Long, BigDecimal> settlementMap = getSettlementMap(startDate, endDate);

        long beforeTime1 = System.currentTimeMillis();
        processSettlements(settlementMap, yesterday);
        long afterTime1 = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
        long diffTime1 = afterTime1 - beforeTime1; // 두 개의 실행 시간
        log.info("실행 시간(ms): " + diffTime1); // 세컨드(초 단위 변환)


        long beforeTime2 = System.currentTimeMillis();
        // Settlement 리스트 생성
        List<Settlement> settlements = settlementMap.entrySet().stream()
                .map(entry -> {
                    Settlement settlement = new Settlement();
                    settlement.setPartnerId(entry.getKey());
                    settlement.setTotalAmount(entry.getValue());
                    settlement.setStatus("completed");
                    settlement.setPaymentDate(yesterday);
                    settlement.setCreatedAt(LocalDateTime.now());
                    settlement.setUpdatedAt(LocalDateTime.now());
                    return settlement;
                })
                .collect(Collectors.toList());

        // JDBC Batch Insert를 통해 Settlement 엔티티들을 한 번에 저장
        batchInsertSettlements(settlements);
        long afterTime2 = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
        long diffTime2 = afterTime2 - beforeTime2; // 두 개의 실행 시간
        log.info("실행 시간(ms): " + diffTime2); // 세컨드(초 단위 변환)


        System.out.println("크론 표현식을 사용해 특정 시간에 실행되는 작업");
    }

    private void batchInsertSettlements(List<Settlement> settlements) {
        String sql = "INSERT INTO settlements (partner_id, total_amount, status, payment_date, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Settlement settlement = settlements.get(i);
                ps.setLong(1, settlement.getPartnerId());
                ps.setBigDecimal(2, settlement.getTotalAmount());
                ps.setString(3, settlement.getStatus());
                ps.setObject(4, settlement.getPaymentDate());
                ps.setObject(5, settlement.getCreatedAt());
                ps.setObject(6, settlement.getUpdatedAt());
            }
            @Override
            public int getBatchSize() {
                return settlements.size();
            }
        });
    }

    private Map<Long, BigDecimal> getSettlementMap(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> paymentList = paymentRepository.findByPaymentDateBetween(startDate, endDate);
        return paymentList.stream()
                .collect(Collectors.groupingBy(
                        Payment::getPartnerId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Payment::getPaymentAmount,
                                BigDecimal::add
                        )
                ));
    }

    private void processSettlements(Map<Long, BigDecimal> settlementMap, LocalDate paymentDate) {
        // ForkJoinPool 커스텀 하여 처리 : CPU 코어 수 + 여유 스레드 수
        ForkJoinPool customThreadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        try {
            customThreadPool.submit(() ->
                    settlementMap.entrySet().parallelStream()
                            .forEach(entry -> {
                                Settlement settlement = Settlement.create(entry.getKey(), entry.getValue(), paymentDate);
                                settlementRepository.save(settlement);
                            })
            ).get(); // get()을 호출하여 작업이 완료될 때까지 기다림
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            customThreadPool.shutdown();  // 작업이 끝나면 풀을 종료
        }
    }
}
