package com.compiler.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CodeSubmission {
    private String id;
    private String userId;
    private String code;
    private String language;
    private String className;
    private String methodName;
    private String input;
    private String output;
    private String error;
    private boolean success;
    private long compilationTime;
    private long executionTime;
    private LocalDateTime createdAt;
}