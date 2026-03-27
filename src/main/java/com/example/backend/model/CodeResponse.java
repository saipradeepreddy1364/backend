package com.example.backend.model;

public class CodeResponse {

    private String stdout;
    private String stderr;
    private String compileOutput;
    private int statusId;
    private String statusDescription;
    private String time;
    private long memory;

    // Full constructor
    public CodeResponse(String stdout, String stderr, String compileOutput,
                        int statusId, String statusDescription, String time, long memory) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.compileOutput = compileOutput;
        this.statusId = statusId;
        this.statusDescription = statusDescription;
        this.time = time;
        this.memory = memory;
    }

    // Overloaded constructor to match older 4-param calls
    public CodeResponse(String stdout, String stderr, String compileOutput, String statusDescription) {
        this(stdout, stderr, compileOutput, 4, statusDescription, "0.00", 0);
    }

    // Getters and Setters
    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout; }

    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr; }

    public String getCompileOutput() { return compileOutput; }
    public void setCompileOutput(String compileOutput) { this.compileOutput = compileOutput; }

    public int getStatusId() { return statusId; }
    public void setStatusId(int statusId) { this.statusId = statusId; }

    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public long getMemory() { return memory; }
    public void setMemory(long memory) { this.memory = memory; }
}