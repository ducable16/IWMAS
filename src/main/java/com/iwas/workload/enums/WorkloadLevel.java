package com.iwas.workload.enums;

/**
 * Workload v3 risk badge — answers the PM's question "do I need to act on
 * this member?". Driven by the schedule simulation, not by raw utilisation:
 * a member at 95% with every deadline met is fine; a member at 60% with a
 * task the simulation predicts will slip is not.
 *
 * Severity order (worst first): OVERDUE > WILL_SLIP > TIGHT > HEALTHY > AVAILABLE.
 */
public enum WorkloadLevel {
    OVERDUE,    // has at least one task already past its due date
    WILL_SLIP,  // no overdue tasks, but the simulation predicts >=1 task will miss its deadline
    TIGHT,      // all deadlines met, but near-term tightness is high (>= 85%)
    HEALTHY,    // all deadlines met, moderate near-term tightness
    AVAILABLE,  // low near-term tightness (< 50%) — can absorb more work
    BLOCKED,    // allocatedEffortPercent = 0 (observer / no contracted hours)
    UNDEFINED   // no allocation row (manager-without-row) or no resolvable capacity
}
