package com.roamtrip.auth;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class EmailMessage implements Serializable {
    private String to;
    private String subject;
    private String template;
    private String token;
}
