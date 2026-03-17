package com.compiler.repository;

import com.compiler.model.CodeSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class SubmissionRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<CodeSubmission> rowMapper = (rs, rowNum) -> {
        CodeSubmission sub = new CodeSubmission();
        sub.setId(rs.getString("id"));
        sub.setUserId(rs.getString("user_id"));
        sub.setCode(rs.getString("code"));
        sub.setLanguage(rs.getString("language"));
        sub.setClassName(rs.getString("class_name"));
        sub.setMethodName(rs.getString("method_name"));
        sub.setInput(rs.getString("input"));
        sub.setOutput(rs.getString("output"));
        sub.setError(rs.getString("error"));
        sub.setSuccess(rs.getBoolean("success"));
        sub.setCompilationTime(rs.getLong("compilation_time"));
        sub.setExecutionTime(rs.getLong("execution_time"));
        sub.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return sub;
    };
    
    public void save(CodeSubmission submission) {
        jdbcTemplate.update(
            "INSERT INTO code_submissions VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            submission.getId(), submission.getUserId(), submission.getCode(),
            submission.getLanguage(), submission.getClassName(), submission.getMethodName(),
            submission.getInput(), submission.getOutput(), submission.getError(),
            submission.isSuccess(), submission.getCompilationTime(),
            submission.getExecutionTime(), submission.getCreatedAt()
        );
    }
    
    public List<CodeSubmission> findByUserId(String userId) {
        return jdbcTemplate.query(
            "SELECT * FROM code_submissions WHERE user_id = ? ORDER BY created_at DESC",
            rowMapper, userId
        );
    }
    
    public Optional<CodeSubmission> findById(String id) {
        try {
            return Optional.ofNullable(
                jdbcTemplate.queryForObject("SELECT * FROM code_submissions WHERE id = ?", rowMapper, id)
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}