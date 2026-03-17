package com.compiler;

import com.compiler.compiler.CompilationResult;
import com.compiler.compiler.JavaCompilerEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CompilerServiceTest {
    
    @Autowired
    private JavaCompilerEngine compilerEngine;
    
    @Test
    public void testSimpleCompilation() {
        String code = "public class Main { public void main() { System.out.println(\"Hello, World!\"); } }";
        CompilationResult result = compilerEngine.compileAndExecute(
            code, 
            "Main", 
            "main", 
            new Object[]{}, 
            5
        );
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput());
    }
}