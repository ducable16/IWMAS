package com.iwas.workload.repository;

import com.iwas.workload.entity.BurnoutLog;
import com.iwas.workload.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BurnoutLogRepository extends JpaRepository<BurnoutLog, Long> {

    @Query("SELECT b FROM BurnoutLog b WHERE b.userId = :userId ORDER BY b.evaluatedAt DESC")
    List<BurnoutLog> findByUserIdOrderByEvaluatedAtDesc(Long userId);

    @Query("SELECT b FROM BurnoutLog b WHERE b.riskLevel IN :levels AND b.isAlertSent = false ORDER BY b.riskScore DESC")
    List<BurnoutLog> findUnsentAlerts(List<RiskLevel> levels);
}
