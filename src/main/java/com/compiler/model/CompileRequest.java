package com.compiler.model;

import lombok.Data;

@Data
public class CompileRequest {
    private String code;
    private String className;
    private String methodName;
    private Object[] args;
    private String language;
}