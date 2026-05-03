package com.iwas.common.mesaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectIndexEvent implements Serializable {

    public enum Op { UPSERT, DELETE }

    private Op op;
    private Long projectId;
    private String name;
    private String code;
    private String status;
    private Long managerId;
}
