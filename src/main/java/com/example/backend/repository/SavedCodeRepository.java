package com.example.backend.repository;

import com.example.backend.model.SavedCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedCodeRepository extends JpaRepository<SavedCode, Long> {
    List<SavedCode> findByUserIdOrderBySavedAtDesc(Long userId);
}