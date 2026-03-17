package com.compiler.controller;

import com.compiler.compiler.CompilationResult;
import com.compiler.model.CodeSubmission;
import com.compiler.model.CompileRequest;
import com.compiler.service.CompilerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compiler")
@CrossOrigin(origins = "${frontend.url}")
public class CompilerController {
    
    @Autowired
    private CompilerService compilerService;
    
    @Value("${frontend.url}")
    private String frontendUrl;
    
    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody CompileRequest request) {
        try {
            String userId = "test-user"; // In real app, get from authentication
            
            CompilationResult result = compilerService.compileAndExecute(
                request.getCode(), userId,
                request.getClassName() != null ? request.getClassName() : "Main",
                request.getMethodName() != null ? request.getMethodName() : "main",
                request.getArgs() != null ? request.getArgs() : new Object[0]
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("output", result.getOutput());
            response.put("error", result.getError());
            response.put("compilationTime", result.getCompilationTime());
            response.put("executionTime", result.getExecutionTime());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/submissions")
    public List<CodeSubmission> getSubmissions() {
        return compilerService.getUserSubmissions("test-user");
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        status.put("frontendUrl", frontendUrl);
        return status;
    }
}