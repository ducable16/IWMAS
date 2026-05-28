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
public class UserIndexEvent implements Serializable {

    public enum Op { UPSERT, DELETE }

    private Op op;
    private Long userId;
    private String email;
    private String fullName;
    private String position;
    private String avatarId;
    private String role;
}
