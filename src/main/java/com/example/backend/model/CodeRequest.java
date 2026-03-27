package com.example.backend.model;

public class CodeRequest {

    private String code;  // the source code to compile/run
    private String input; // optional input for the program

    public CodeRequest() {}

    public CodeRequest(String code, String input) {
        this.code = code;
        this.input = input;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}