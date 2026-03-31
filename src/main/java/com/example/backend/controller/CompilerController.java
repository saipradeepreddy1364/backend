package com.example.backend.controller;

import com.example.backend.model.CodeRequest;
import com.example.backend.model.CodeResponse;
import org.springframework.web.bind.annotation.*;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/compiler")
@CrossOrigin(origins = "*")
public class CompilerController {

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
            // Create temp folder
            folder = Files.createTempDirectory("compiler_" + UUID.randomUUID());
            Path javaFile = folder.resolve("Main.java");
            Files.writeString(javaFile, code);

            // STEP 1: IN-MEMORY COMPILE using javax.tools (no new JVM process!)
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            if (compiler == null) {
                return fallbackProcessCompiler(folder, input);
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager =
                         compiler.getStandardFileManager(diagnostics, null, null)) {

                Iterable<? extends JavaFileObject> compilationUnits =
                        fileManager.getJavaFileObjects(javaFile.toFile());

                JavaCompiler.CompilationTask task = compiler.getTask(
                        null, fileManager, diagnostics, null, null, compilationUnits);

                boolean success = task.call();

                if (!success) {
                    StringBuilder errors = new StringBuilder("Compilation Error:\n");
                    for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                        if (d.getKind() == Diagnostic.Kind.ERROR) {
                            errors.append("Line ").append(d.getLineNumber())
                                  .append(": ").append(d.getMessage(null)).append("\n");
                        }
                    }
                    return new CodeResponse(errors.toString().trim(), null, null, "Compilation Error");
                }
            }

            // STEP 2: RUN via ProcessBuilder (1 JVM startup instead of 2)
            Process run = new ProcessBuilder("java", "-cp", folder.toString(), "Main")
                    .redirectErrorStream(true)
                    .start();

            // Read output concurrently (prevents pipe buffer deadlock)
            Future<String> outputFuture = executor.submit(() -> readStream(run.getInputStream()));

            // Send stdin input
            if (input != null && !input.isBlank()) {
                try (BufferedWriter inputWriter = new BufferedWriter(
                        new OutputStreamWriter(run.getOutputStream()))) {
                    inputWriter.write(input);
                    inputWriter.newLine();
                    inputWriter.flush();
                }
            } else {
                run.getOutputStream().close(); // close stdin so Scanner doesn't hang
            }

            boolean finished = run.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                return new CodeResponse("Error: Execution timed out (10s).", null, null, "Timeout");
            }

            String output = outputFuture.get(5, TimeUnit.SECONDS);

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
            if (folder != null) deleteFolder(folder.toFile());
        }
    }

    /**
     * Fallback if javax.tools is unavailable (JRE-only environment).
     * Uses ProcessBuilder with all deadlock fixes applied.
     */
    private CodeResponse fallbackProcessCompiler(Path folder, String input) throws Exception {
        Process compile = new ProcessBuilder("javac", "Main.java")
                .directory(folder.toFile())
                .redirectErrorStream(true)
                .start();

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

        Process run = new ProcessBuilder("java", "-cp", ".", "Main")
                .directory(folder.toFile())
                .redirectErrorStream(true)
                .start();

        Future<String> outputFuture = executor.submit(() -> readStream(run.getInputStream()));

        if (input != null && !input.isBlank()) {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(run.getOutputStream()))) {
                w.write(input); w.newLine(); w.flush();
            }
        } else {
            run.getOutputStream().close();
        }

        boolean finished = run.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            run.destroyForcibly();
            return new CodeResponse("Error: Execution timed out.", null, null, "Timeout");
        }

        String output = outputFuture.get(5, TimeUnit.SECONDS);
        String filtered = output.lines()
                .filter(l -> !l.startsWith("Picked up JAVA_TOOL_OPTIONS"))
                .collect(java.util.stream.Collectors.joining("\n"));
        return new CodeResponse(filtered.trim().isEmpty() ? "No output" : filtered.trim(), null, null, "Success");
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
            if (files != null) { for (File f : files) deleteFolder(f); }
        }
        folder.delete();
    }
}