package com.compiler.model;

import lombok.Data;

@Data
public class ExecutionResult {
    private boolean success;
    private String output;
    private String error;
    private long executionTime;
}