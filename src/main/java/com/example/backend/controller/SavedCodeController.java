// src/main/java/com/example/backend/controller/SavedCodeController.java
package com.example.backend.controller;

import com.example.backend.model.SavedCode;
import com.example.backend.repository.SavedCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/codes")
@CrossOrigin(origins = "*")
public class SavedCodeController {

    @Autowired
    private SavedCodeRepository savedCodeRepository;

    // ── Save a code ─────────────────────────────────────────────────────────
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveCode(
            @RequestBody SavedCode savedCode) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Upsert: if a code already exists for this user + problem, update it
            if (savedCode.getProblemId() != null && savedCode.getUserEmail() != null) {
                Optional<SavedCode> existing = savedCodeRepository
                        .findByUserEmailAndProblemId(savedCode.getUserEmail(), savedCode.getProblemId());
                existing.ifPresent(e -> savedCode.setId(e.getId()));
            }
            SavedCode saved = savedCodeRepository.save(savedCode);
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("message", "Code saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to save code: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ── Get all saved codes for a user ──────────────────────────────────────
    @GetMapping("/user/{email}")
    public ResponseEntity<Map<String, Object>> getSavedCodes(
            @PathVariable String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SavedCode> codes = savedCodeRepository.findByUserEmailOrderBySavedAtDesc(email);
            response.put("success", true);
            response.put("codes", codes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch codes: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ── Delete a saved code ─────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCode(@PathVariable long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!savedCodeRepository.existsById(id)) {
                response.put("success", false);
                response.put("message", "Code not found");
                return ResponseEntity.status(404).body(response);
            }
            savedCodeRepository.deleteById(id);
            response.put("success", true);
            response.put("message", "Code deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete code: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ── Analytics / Stats endpoint ──────────────────────────────────────────
    @GetMapping("/stats/{email}")
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SavedCode> codes = savedCodeRepository.findByUserEmailOrderBySavedAtDesc(email);

            // ── total distinct problems saved ─────────────────────────────
            long total = savedCodeRepository.countDistinctProblemsByEmail(email);

            // ── byCategory: use repository query to count distinct problems ─
            List<Object[]> catRows = savedCodeRepository.countSolvedPerCategoryByEmail(email);
            Map<String, Long> byCategory = new LinkedHashMap<>();
            for (Object[] row : catRows) {
                String cat = (String) row[0];
                // Cast via Number to avoid @NonNull Long unboxing warning (Java 16778128)
                long count = ((Number) row[1]).longValue();
                if (cat != null && !cat.isBlank()) {
                    byCategory.put(cat, count);
                }
            }

            // ── byLanguage ────────────────────────────────────────────────
            Map<String, Long> byLanguage = codes.stream()
                    .filter(c -> c.getLanguage() != null && !c.getLanguage().isBlank())
                    .collect(Collectors.groupingBy(SavedCode::getLanguage, Collectors.counting()));

            // ── compilerRuns ──────────────────────────────────────────────
            // Explicitly call .longValue() to avoid @NonNull Long unboxing warning
            long compilerRuns = codes.stream()
                    .mapToLong(c -> {
                        Long runs = c.getCompilerRuns();
                        return (runs != null) ? runs.longValue() : 0L;
                    })
                    .sum();

            // ── streakDays ────────────────────────────────────────────────
            Set<LocalDate> saveDates = codes.stream()
                    .filter(c -> c.getSavedAt() != null)
                    .map(c -> c.getSavedAt().toLocalDate())
                    .collect(Collectors.toSet());

            List<LocalDate> sortedDates = saveDates.stream()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            int streakDays = 0;
            if (!sortedDates.isEmpty()) {
                LocalDate today = LocalDate.now();
                LocalDate mostRecent = sortedDates.get(0);
                // Only start counting if the user saved today or yesterday
                if (mostRecent.equals(today) || mostRecent.equals(today.minusDays(1))) {
                    LocalDate expected = mostRecent;
                    for (LocalDate d : sortedDates) {
                        if (d.equals(expected)) {
                            streakDays++;
                            expected = expected.minusDays(1);
                        } else {
                            break;
                        }
                    }
                }
            }

            // ── Build response ────────────────────────────────────────────
            response.put("success", true);
            response.put("total", total);
            response.put("byCategory", byCategory);
            response.put("byLanguage", byLanguage);
            response.put("compilerRuns", compilerRuns);
            response.put("streakDays", streakDays);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch stats: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}