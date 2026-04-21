package com.iwas.workload.dto;

import com.iwas.workload.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BurnoutLogResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private LocalDateTime evaluatedAt;
    private Integer riskScore;
    private RiskLevel riskLevel;
    private Integer overdueTaskCount;
    private BigDecimal capacityUsedAvg;
    private Boolean isAlertSent;
    private String note;
}
