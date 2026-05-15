package com.iwas.workload.enums;

public enum WorkloadLevel {
    AVAILABLE,      // utilization < 70%
    HEALTHY_BUSY,   // 70% <= utilization <= 100%
    OVERLOADED,     // utilization > 100%
    BLOCKED,        // allocatedEffortPercent = 0 (observer / no contracted hours)
    UNDEFINED       // 0-workday window, manager without allocation, or no capacity
}
