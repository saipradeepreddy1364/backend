package com.example.backend.controller;

import com.example.backend.model.CodeRequest;
import com.example.backend.model.CodeResponse;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/compiler")
@CrossOrigin(origins = "*")
public class CompilerController {

    // Reusable thread pool for reading streams concurrently
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/run")
    public CodeResponse runCode(@RequestBody CodeRequest request) {
        String code = request.getCode();
        String input = request.getInput();

        if (code == null || code.isBlank()) {
            return new CodeResponse("Error: No code provided.", null, null, "Error");
        }

        Path folder = null;
        try {
            // Create unique temp directory for this execution
            folder = Files.createTempDirectory("compiler_" + UUID.randomUUID());
            File javaFile = folder.resolve("Main.java").toFile();

            try (FileWriter writer = new FileWriter(javaFile)) {
                writer.write(code);
            }

            // ─── STEP 1: COMPILE ───────────────────────────────────────────
            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(folder.toFile())
                    .redirectErrorStream(true)
                    .start();

            // Read compile output concurrently (prevents buffer deadlock)
            Future<String> compileFuture = executor.submit(() -> readStream(compile.getInputStream()));

            boolean compileFinished = compile.waitFor(15, TimeUnit.SECONDS);
            if (!compileFinished) {
                compile.destroyForcibly();
                return new CodeResponse("Error: Compilation timed out.", null, null, "Timeout");
            }

            String compileOutput = compileFuture.get(5, TimeUnit.SECONDS);

            if (compile.exitValue() != 0) {
                return new CodeResponse("Compilation Error:\n" + compileOutput, null, null, "Compilation Error");
            }

            // ─── STEP 2: RUN ───────────────────────────────────────────────
            Process run = new ProcessBuilder("java", "-cp", ".", "Main")
                    .directory(folder.toFile())
                    .redirectErrorStream(true) // merge stderr into stdout
                    .start();

            // ✅ FIX: Read output CONCURRENTLY while process runs
            Future<String> outputFuture = executor.submit(() -> readStream(run.getInputStream()));

            // Send stdin input to the process (if any)
            if (input != null && !input.isBlank()) {
                try (BufferedWriter inputWriter = new BufferedWriter(
                        new OutputStreamWriter(run.getOutputStream()))) {
                    inputWriter.write(input);
                    inputWriter.newLine();
                    inputWriter.flush();
                }
            } else {
                // Close stdin so Scanner-based programs don't hang waiting for input
                run.getOutputStream().close();
            }

            // Wait up to 10 seconds for the process to finish
            boolean finished = run.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                return new CodeResponse("Error: Execution timed out (10s).", null, null, "Timeout");
            }

            // ✅ Now safely get the output (already read concurrently)
            String output = outputFuture.get(5, TimeUnit.SECONDS);

            // Filter out Render/JVM environment noise
            String filteredOutput = output.lines()
                    .filter(line -> !line.startsWith("Picked up JAVA_TOOL_OPTIONS"))
                    .collect(java.util.stream.Collectors.joining("\n"));

            return new CodeResponse(
                    filteredOutput.trim().isEmpty() ? "No output" : filteredOutput.trim(),
                    null, null, "Success");

        } catch (TimeoutException e) {
            return new CodeResponse("Error: Output reading timed out.", null, null, "Timeout");
        } catch (Exception e) {
            return new CodeResponse("Backend Error: " + e.getMessage(), null, null, "Error");
        } finally {
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