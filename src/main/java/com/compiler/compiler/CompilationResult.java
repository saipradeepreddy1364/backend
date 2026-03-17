package com.compiler.compiler;

import lombok.Data;

@Data
public class CompilationResult {
    private boolean success;
    private String output;
    private String error;
    private byte[] bytecode;
    private long compilationTime;
    private long executionTime;
}