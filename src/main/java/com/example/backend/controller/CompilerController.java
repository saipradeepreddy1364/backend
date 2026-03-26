package com.example.backend.controller;

import org.springframework.web.bind.annotation.*;
import com.example.backend.model.CodeRequest;

import java.io.*;
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

        String folderName = "temp_" + UUID.randomUUID();
        File folder = new File(folderName);
        folder.mkdir();

        try {
            File file = new File(folder, "Main.java");
            FileWriter writer = new FileWriter(file);
            writer.write(code);
            writer.close();

            // Compile
            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(folder)
                    .redirectErrorStream(true)
                    .start();

            String compileOutput = readStream(compile.getInputStream());
            compile.waitFor();

            if (!compileOutput.isEmpty()) {
                deleteFolder(folder);
                return "Compilation Error:\n" + compileOutput;
            }

            // Run with 10 second timeout
            Process run = new ProcessBuilder("java", "-cp", ".", "Main")
                    .directory(folder)
                    .redirectErrorStream(true)
                    .start();

            if (input != null && !input.isEmpty()) {
                BufferedWriter writerInput = new BufferedWriter(
                        new OutputStreamWriter(run.getOutputStream()));
                writerInput.write(input);
                writerInput.newLine();
                writerInput.flush();
                writerInput.close();
            }

            boolean finished = run.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                deleteFolder(folder);
                return "Error: Execution timed out (10 second limit exceeded).";
            }

            String output = readStream(run.getInputStream());
            deleteFolder(folder);

            return output.isEmpty() ? "(No output)" : output;

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        folder.delete();
    }
}