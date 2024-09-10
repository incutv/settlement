package com.example.settlement.dailySettlement.repository;

import com.example.settlement.dailySettlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    // 기본 CRUD 메서드는 JpaRepository가 자동으로 제공

    // 필요시 커스텀 쿼리 메서드를 추가할 수 있음
}
