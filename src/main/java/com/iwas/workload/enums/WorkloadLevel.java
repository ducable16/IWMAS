package com.iwas.workload.enums;

public enum WorkloadLevel {
    AVAILABLE,      // utilization < 70%
    HEALTHY_BUSY,   // 70% <= utilization <= 100%
    OVERLOADED      // utilization > 100%
}
