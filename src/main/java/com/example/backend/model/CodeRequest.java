package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CodeRequest {
    private String code;

    // This tells Jackson to fill 'input' even if the JSON says 'stdin'
    @JsonAlias({"stdin", "input"})
    private String input;

    public CodeRequest() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
}
