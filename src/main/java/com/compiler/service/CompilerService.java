package com.compiler.service;

import com.compiler.compiler.CompilationResult;
import com.compiler.compiler.JavaCompilerEngine;
import com.compiler.model.CodeSubmission;
import com.compiler.repository.SubmissionRepository;
import com.compiler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CompilerService {
    
    @Autowired
    private JavaCompilerEngine compilerEngine;
    
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${compiler.timeout.seconds:10}")
    private long timeoutSeconds;
    
    public CompilationResult compileAndExecute(String code, String userId, 
                                               String className, String methodName, 
                                               Object[] args) {
        
        CompilationResult result = compilerEngine.compileAndExecute(
            code, className, methodName, args, timeoutSeconds);
        
        CodeSubmission submission = new CodeSubmission();
        submission.setId(UUID.randomUUID().toString());
        submission.setUserId(userId);
        submission.setCode(code);
        submission.setLanguage("java");
        submission.setClassName(className);
        submission.setMethodName(methodName);
        submission.setInput(args != null ? String.join(", ", java.util.Arrays.toString(args)) : "");
        submission.setOutput(result.getOutput());
        submission.setError(result.getError());
        submission.setSuccess(result.isSuccess());
        submission.setCompilationTime(result.getCompilationTime());
        submission.setExecutionTime(result.getExecutionTime());
        submission.setCreatedAt(LocalDateTime.now());
        
        submissionRepository.save(submission);
        userRepository.updateSubmissionCount(userId);
        
        return result;
    }
    
    public List<CodeSubmission> getUserSubmissions(String userId) {
        return submissionRepository.findByUserId(userId);
    }
    
    public CodeSubmission getSubmission(String id) {
        return submissionRepository.findById(id).orElse(null);
    }
}