package com.iwas.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class UserEffortRemainingResponse {

    private Long userId;
    private String userName;
    private LocalDate queryStart;
    private LocalDate queryEnd;

    private int peakAllocatedPercent;

    private int remainingPercent;

    private List<AllocationEntry> overlappingAllocations;

    /**
     * Upcoming dates when this user's capacity will increase (a project ends).
     * Always computed — intended for the quick note below the allocation form.
     */
    private List<FutureAvailabilityNote> futureAvailabilityNotes;

    /**
     * Full chronological allocation timeline as ordered segments.
     * Only populated when the request includes {@code detail=true}.
     * Null otherwise to keep the default response lightweight.
     */
    private List<TimelineSegment> allocationTimeline;

    // --- inner types ---

    @Data
    @Builder
    public static class AllocationEntry {
        private Long projectId;
        private String projectName;
        private String projectCode;
        private int allocatedPercent;
        private LocalDate projectStartDate;
        private LocalDate projectEndDate;
    }

    @Data
    @Builder
    public static class FutureAvailabilityNote {
        /** First day the extra capacity is available. */
        private LocalDate availableFrom;
        /** How many percent-points become free at this date. */
        private int additionalFreePercent;
        /** Cumulative remaining capacity from this date onward (before any new assignments). */
        private int cumulativeRemainingPercent;
        /** Projects that finish just before this date, releasing the capacity. */
        private List<ProjectRef> triggeringProjects;
    }

    @Data
    @Builder
    public static class TimelineSegment {
        /** First day of this segment. */
        private LocalDate from;
        /** Last day of this segment; null means open-ended. */
        private LocalDate to;
        private int allocatedPercent;
        private int remainingPercent;
        /** What changed at the start of this segment (which projects started or ended). */
        private ChangeSummary changeSummary;
    }

    @Data
    @Builder
    public static class ChangeSummary {
        /** Net delta at segment boundary: negative = capacity freed, positive = capacity consumed. */
        private int deltaPercent;
        private List<ProjectRef> startingProjects;
        private List<ProjectRef> endingProjects;
    }

    @Data
    @Builder
    public static class ProjectRef {
        private Long projectId;
        private String projectName;
        private String projectCode;
        private int allocatedPercent;
    }
}
