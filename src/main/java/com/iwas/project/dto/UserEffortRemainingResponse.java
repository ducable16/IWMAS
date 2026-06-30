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

    private List<FutureAvailabilityNote> futureAvailabilityNotes;

    private List<TimelineSegment> allocationTimeline;


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
        private LocalDate availableFrom;
        private int additionalFreePercent;
        private int cumulativeRemainingPercent;
        private List<ProjectRef> triggeringProjects;
    }

    @Data
    @Builder
    public static class TimelineSegment {
        private LocalDate from;
        private LocalDate to;
        private int allocatedPercent;
        private int remainingPercent;
        private ChangeSummary changeSummary;
    }

    @Data
    @Builder
    public static class ChangeSummary {
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
