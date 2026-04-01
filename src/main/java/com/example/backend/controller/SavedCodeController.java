package com.example.backend.controller;

import com.example.backend.model.SavedCode;
import com.example.backend.repository.SavedCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/codes")
public class SavedCodeController {

    @Autowired
    private SavedCodeRepository savedCodeRepository;

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveCode(@RequestBody SavedCode savedCode) {
        Map<String, Object> response = new HashMap<>();

        if (savedCode.getUserEmail() == null || savedCode.getUserEmail().isBlank()) {
            response.put("success", false);
            response.put("message", "userEmail is required. Please login first.");
            return ResponseEntity.badRequest().body(response);
        }

        SavedCode saved = savedCodeRepository.save(savedCode);
        response.put("success", true);
        response.put("message", "Code saved successfully!");
        response.put("id", saved.getId());
        response.put("savedAt", saved.getSavedAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userEmail}")
    public ResponseEntity<List<SavedCode>> getCodesByUser(@PathVariable @NonNull String userEmail) {
        List<SavedCode> codes = savedCodeRepository.findByUserEmailOrderBySavedAtDesc(userEmail);
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCodeById(@PathVariable @NonNull Long id) {
        Map<String, Object> response = new HashMap<>();
        return savedCodeRepository.findById(id)
                .map(code -> {
                    response.put("success", true);
                    response.put("data", code);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("message", "Code not found.");
                    return ResponseEntity.status(404).body(response);
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCode(@PathVariable @NonNull Long id) {
        Map<String, Object> response = new HashMap<>();

        if (!savedCodeRepository.existsById(id)) {
            response.put("success", false);
            response.put("message", "Code not found.");
            return ResponseEntity.status(404).body(response);
        }

        savedCodeRepository.deleteById(id);
        response.put("success", true);
        response.put("message", "Code deleted successfully.");
        return ResponseEntity.ok(response);
    }
}