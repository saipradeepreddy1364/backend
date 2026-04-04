// src/main/java/com/example/backend/model/SavedCode.java
package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_codes")
public class SavedCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The ID of the problem from your problems list (used for upsert & distinct count)
    private Long problemId;

    // The problem title (e.g. "Two Sum")
    private String title;

    // The full source code the user wrote
    @Column(columnDefinition = "TEXT")
    private String code;

    // Custom input the user provided in the editor
    @Column(columnDefinition = "TEXT")
    private String input;

    // The email of the user who saved this code
    private String userEmail;

    // Programming language: "Python", "Java", "C++", etc.
    private String language;

    // Category / topic of the problem: "Arrays", "Graphs", etc.
    // Powers the byCategory breakdown in analytics.
    private String category;

    // How many times the user ran this code in the compiler.
    // Increment on each /compile call and persist here.
    private Long compilerRuns;

    // Timestamp set automatically before first insert
    private LocalDateTime savedAt;

    @PrePersist
    public void prePersist() {
        this.savedAt = LocalDateTime.now();
        if (this.compilerRuns == null) {
            this.compilerRuns = 0L;
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getCompilerRuns() { return compilerRuns; }
    public void setCompilerRuns(Long compilerRuns) { this.compilerRuns = compilerRuns; }

    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}