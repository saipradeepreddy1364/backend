package com.example.backend.controller;

import com.example.backend.model.CodeRequest;
import com.example.backend.model.CodeResponse;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/compiler")
// UPDATED: Use * to allow all origins or put your exact Vercel URL
@CrossOrigin(origins = "*") 
public class CompilerController {

    @PostMapping("/run")
    public CodeResponse runCode(@RequestBody CodeRequest request) {
        String code = request.getCode();
        // UPDATED: Your frontend sends 'stdin', ensure your CodeRequest model matches or check both
        String input = request.getInput() != null ? request.getInput() : request.getStdin();

        if (code == null || code.isBlank()) {
            return new CodeResponse("Error: No code provided.", null, null, "Error");
        }

        Path folder = null;
        try {
            folder = Files.createTempDirectory("compiler_" + UUID.randomUUID());
            File javaFile = folder.resolve("Main.java").toFile();
            try (FileWriter writer = new FileWriter(javaFile)) {
                writer.write(code);
            }

            // 1. COMPILE
            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(folder.toFile())
                    .redirectErrorStream(true)
                    .start();

            String compileOutput = readStream(compile.getInputStream());
            compile.waitFor();

            if (compile.exitValue() != 0) { // Check exit code instead of just output length
                return new CodeResponse("Compilation Error:\n" + compileOutput, null, null, "Compilation Error");
            }

            // 2. RUN
            Process run = new ProcessBuilder("java", "-cp", ".", "Main")
                    .directory(folder.toFile())
                    .redirectErrorStream(true)
                    .start();

            // Provide input to the running process
            if (input != null && !input.isBlank()) {
                try (BufferedWriter writerInput = new BufferedWriter(new OutputStreamWriter(run.getOutputStream()))) {
                    writerInput.write(input);
                    writerInput.flush();
                }
            }

            boolean finished = run.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                return new CodeResponse("Error: Execution timed out.", null, null, "Timeout");
            }

            String output = readStream(run.getInputStream());
            return new CodeResponse(output.isEmpty() ? "No output" : output, null, null, "Success");

        } catch (Exception e) {
            return new CodeResponse("Error: " + e.getMessage(), null, null, "Error");
        } finally {
            if (folder != null) deleteFolder(folder.toFile());
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString().trim(); // Use trim to remove trailing newlines
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) deleteFolder(f);
            }
        }
        folder.delete();
    }
}
