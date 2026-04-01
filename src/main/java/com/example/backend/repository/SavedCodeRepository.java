package com.example.backend.repository;

import com.example.backend.model.SavedCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedCodeRepository extends JpaRepository<SavedCode, Long> {
    List<SavedCode> findByUserEmailOrderBySavedAtDesc(String userEmail);
}
