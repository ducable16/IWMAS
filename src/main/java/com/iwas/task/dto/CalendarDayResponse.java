package com.iwas.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CalendarDayResponse {
    private LocalDate date;
    private List<TaskResponse> tasks;
    private int count;
}
