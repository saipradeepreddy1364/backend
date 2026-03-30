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
@CrossOrigin(origins = "*") // Allows your Vercel frontend to connect
public class CompilerController {

    @PostMapping("/run")
    public CodeResponse runCode(@RequestBody CodeRequest request) {
        String code = request.getCode();
        // Updated: Using getInput() which will map to 'stdin' from frontend via @JsonAlias
        String input = request.getInput();

        if (code == null || code.isBlank()) {
            return new CodeResponse("Error: No code provided.", null, null, "Error");
        }

        Path folder = null;
        try {
            // Create a unique temporary directory for this execution
            folder = Files.createTempDirectory("compiler_" + UUID.randomUUID());
            File javaFile = folder.resolve("Main.java").toFile();

            try (FileWriter writer = new FileWriter(javaFile)) {
                writer.write(code);
            }

            // --- STEP 1: COMPILE ---
            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(folder.toFile())
                    .redirectErrorStream(true)
                    .start();

            String compileOutput = readStream(compile.getInputStream());
            compile.waitFor();

            if (compile.exitValue() != 0) {
                return new CodeResponse("Compilation Error:\n" + compileOutput, null, null, "Compilation Error");
            }

            // --- STEP 2: RUN ---
            Process run = new ProcessBuilder("java", "-cp", ".", "Main")
                    .directory(folder.toFile())
                    .redirectErrorStream(true)
                    .start();

            // Send input (stdin) to the running process
            if (input != null && !input.isBlank()) {
                try (BufferedWriter writerInput = new BufferedWriter(new OutputStreamWriter(run.getOutputStream()))) {
                    writerInput.write(input);
                    writerInput.newLine(); // Ensure the line is submitted
                    writerInput.flush();
                }
            }

            // Wait for 10 seconds max
            boolean finished = run.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                return new CodeResponse("Error: Execution timed out (10s).", null, null, "Timeout");
            }

            String output = readStream(run.getInputStream());

            // Filter out JAVA_TOOL_OPTIONS line added by Render
            String filteredOutput = output.lines()
                    .filter(line -> !line.startsWith("Picked up JAVA_TOOL_OPTIONS"))
                    .collect(java.util.stream.Collectors.joining("\n"));

            // Return 'No output' if the string is truly empty
            return new CodeResponse(filteredOutput.trim().isEmpty() ? "No output" : filteredOutput, null, null, "Success");

        } catch (Exception e) {
            return new CodeResponse("Backend Error: " + e.getMessage(), null, null, "Error");
        } finally {
            // Cleanup: Always delete the temporary folder
            if (folder != null) {
                deleteFolder(folder.toFile());
            }
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
        return output.toString();
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