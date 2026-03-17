package com.compiler.repository;

import com.compiler.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@SuppressWarnings("null")
public class UserRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        user.setActive(rs.getBoolean("is_active"));
        user.setSubmissionCount(rs.getInt("submission_count"));
        return user;
    };
    
    public Optional<User> findById(String id) {
        try {
            return Optional.ofNullable(
                jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", rowMapper, id)
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    public void updateSubmissionCount(String userId) {
        jdbcTemplate.update(
            "UPDATE users SET submission_count = submission_count + 1 WHERE id = ?", 
            userId
        );
    }
}