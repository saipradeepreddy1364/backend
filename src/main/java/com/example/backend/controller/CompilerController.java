package com.example.backend.controller;

import com.example.backend.model.CodeRequest;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/compiler")
@CrossOrigin(origins = "https://compiler-two-sigma.vercel.app")
public class CompilerController {

    @PostMapping("/run")
    public String runCode(@RequestBody CodeRequest request) {
        String code = request.getCode();
        String input = request.getInput();

        if (code == null || code.isBlank()) {
            return "Error: No code provided.";
        }

        // Create a temporary folder
        Path folder;
        try {
            folder = Files.createTempDirectory("compiler_" + UUID.randomUUID());
        } catch (IOException e) {
            return "Error creating temporary folder: " + e.getMessage();
        }

        File javaFile = folder.resolve("Main.java").toFile();

        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(code);
        } catch (IOException e) {
            deleteFolder(folder.toFile());
            return "Error writing code file: " + e.getMessage();
        }

        try {
            // Compile
            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(folder.toFile())
                    .redirectErrorStream(true)
                    .start();

            String compileOutput = readStream(compile.getInputStream());
            compile.waitFor();

            if (!compileOutput.isEmpty()) {
                deleteFolder(folder.toFile());
                return "Compilation Error:\n" + compileOutput;
            }

            // Run with optional input and 10-second timeout
            Process run = new ProcessBuilder("java", "-cp", ".", "Main")
                    .directory(folder.toFile())
                    .redirectErrorStream(true)
                    .start();

            if (input != null && !input.isBlank()) {
                try (BufferedWriter writerInput = new BufferedWriter(
                        new OutputStreamWriter(run.getOutputStream()))) {
                    writerInput.write(input);
                    writerInput.newLine();
                    writerInput.flush();
                }
            }

            boolean finished = run.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                deleteFolder(folder.toFile());
                return "Error: Execution timed out (10 second limit exceeded).";
            }

            String output = readStream(run.getInputStream());
            deleteFolder(folder.toFile());

            return output.isBlank() ? "(No output)" : output;

        } catch (Exception e) {
            deleteFolder(folder.toFile());
            return "Error: " + e.getMessage();
        }
    }

    // Helper to read InputStream fully
    private String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
    }

    // Recursive folder deletion
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