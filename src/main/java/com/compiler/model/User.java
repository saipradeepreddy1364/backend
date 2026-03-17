package com.compiler.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private String id;
    private String email;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
    private int submissionCount;
}