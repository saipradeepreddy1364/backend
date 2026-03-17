package com.compiler.compiler;

import com.compiler.model.ExecutionResult;
import org.springframework.stereotype.Component;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Component
public class JavaCompilerEngine {
    
    private final JavaCompiler compiler;
    private final DiagnosticCollector<JavaFileObject> diagnostics;
    
    public JavaCompilerEngine() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.diagnostics = new DiagnosticCollector<>();
    }
    
    public CompilationResult compileAndExecute(String code, String className, String methodName, 
                                                Object[] args, long timeoutSeconds) {
        CompilationResult result = new CompilationResult();
        long startTime = System.currentTimeMillis();
        
        try {
            // Compile
            boolean compiled = compile(code, className, result);
            result.setCompilationTime(System.currentTimeMillis() - startTime);
            
            if (!compiled) {
                return result;
            }
            
            // Execute
            long execStartTime = System.currentTimeMillis();
            ExecutionResult execResult = execute(className, methodName, args, timeoutSeconds);
            result.setOutput(execResult.getOutput());
            result.setError(execResult.getError());
            result.setSuccess(execResult.isSuccess());
            result.setExecutionTime(System.currentTimeMillis() - execStartTime);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("Error: " + e.getMessage());
        }
        
        return result;
    }
    
    private boolean compile(String code, String className, CompilationResult result) {
        try {
            JavaFileObject file = new InMemoryJavaFileObject(className, code);
            Iterable<? extends JavaFileObject> files = Collections.singletonList(file);
            
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, null, diagnostics, null, null, files);
            
            boolean success = task.call();
            
            StringBuilder errors = new StringBuilder();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                errors.append(diagnostic.getMessage(null))
                      .append(" at line ").append(diagnostic.getLineNumber())
                      .append("\n");
            }
            
            result.setSuccess(success);
            result.setError(errors.toString());
            return success;
            
        } catch (Exception e) {
            result.setError("Compilation error: " + e.getMessage());
            return false;
        }
    }
    
    private ExecutionResult execute(String className, String methodName, Object[] args, long timeout) {
        ExecutionResult result = new ExecutionResult();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            System.setOut(new PrintStream(baos));
            System.setErr(new PrintStream(baos));
            
            Future<?> future = executor.submit(() -> {
                try {
                    Class<?> clazz = Class.forName(className);
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Method method = clazz.getMethod(methodName, getParameterTypes(args));
                    method.invoke(instance, args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            future.get(timeout, TimeUnit.SECONDS);
            result.setSuccess(true);
            result.setOutput(baos.toString());
            
        } catch (TimeoutException e) {
            result.setError("Execution timed out after " + timeout + " seconds");
        } catch (Exception e) {
            result.setError("Execution error: " + e.getMessage());
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
            executor.shutdownNow();
        }
        
        return result;
    }
    
    private Class<?>[] getParameterTypes(Object[] args) {
        if (args == null) return new Class<?>[0];
        return Arrays.stream(args)
                    .map(Object::getClass)
                    .toArray(Class<?>[]::new);
    }
    
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;
        
        InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), 
                  Kind.SOURCE);
            this.code = code;
        }
        
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}