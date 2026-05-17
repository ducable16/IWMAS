package com.iwas.recommendation.enums;

/**
 * Leaf criteria in the AHP hierarchy used by the assignee recommender.
 *
 * <p>The hierarchy is:
 * <pre>
 *                       GOAL
 *                         │
 *       ┌─────────────────┼─────────────────┐
 *     SKILL           WORKLOAD          ON_TIME
 *       │
 *   ┌───┴───┐
 * COVERAGE  LEVEL
 * </pre>
 *
 * <p>The parent {@code SKILL} node is not represented in this enum because it is an
 * internal aggregate node — its weight is derived as {@code SKILL_COVERAGE + SKILL_LEVEL}.
 * Only leaf criteria participate in the final TOPSIS weight vector.
 */
public enum AhpCriterion {
    SKILL_COVERAGE,
    SKILL_LEVEL,
    WORKLOAD,
    ON_TIME
}
