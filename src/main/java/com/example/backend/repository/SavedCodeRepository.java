package com.example.backend.repository;

import com.example.backend.model.SavedCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedCodeRepository extends JpaRepository<SavedCode, Long> {

    // All saved codes for a user, newest first
    List<SavedCode> findByUserEmailOrderBySavedAtDesc(String userEmail);

    // Count how many distinct problems a user has saved per category
    @Query("SELECT s.category, COUNT(DISTINCT s.problemId) FROM SavedCode s " +
           "WHERE s.userEmail = :email AND s.category IS NOT NULL " +
           "GROUP BY s.category")
    List<Object[]> countSolvedPerCategoryByEmail(@Param("email") String email);

    // Total distinct problems saved by a user
    @Query("SELECT COUNT(DISTINCT s.problemId) FROM SavedCode s " +
           "WHERE s.userEmail = :email AND s.problemId IS NOT NULL")
    long countDistinctProblemsByEmail(@Param("email") String email);

    // Find existing saved code for a specific problem + user (to upsert)
    Optional<SavedCode> findByUserEmailAndProblemId(String userEmail, Long problemId);
}