package com.iwas.workload.enums;

/**
 * Dashboard "Workload" axis — pure backlog depth (volume of queued work vs the
 * member's weekly capacity), deadline-agnostic and order-independent.
 *
 * <p>Derived from {@code backlogDays = Σ remaining ÷ dailyCapacity}:
 * <ul>
 *   <li>{@code AVAILABLE}  — ≤ 5 workdays of queued work; room to take more.</li>
 *   <li>{@code BUSY}       — 5–10 workdays queued; be cautious adding work.</li>
 *   <li>{@code OVERLOADED} — &gt; 10 workdays queued.</li>
 *   <li>{@code BLOCKED}    — allocation 0% (observer): has no contracted capacity.</li>
 *   <li>{@code UNDEFINED}  — no allocation row to measure against (e.g. a manager lane).</li>
 * </ul>
 *
 * This is the deadline-blind counterpart to the deadline-risk signals
 * ({@code overdueTaskCount} / {@code predictedLateTaskCount}); the two are shown
 * side by side on the dashboard and never replace each other.
 */
public enum LoadLevel {
    AVAILABLE,
    BUSY,
    OVERLOADED,
    BLOCKED,
    UNDEFINED
}
