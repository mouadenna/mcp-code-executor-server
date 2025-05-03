/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.springframework.ai.mcp.sample.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class CodeExecutionService {

    private static final int TIMEOUT_SECONDS = 15;
    
    // Map of supported languages and their execution configurations
    private static final Map<String, LanguageConfig> SUPPORTED_LANGUAGES = new HashMap<>();
    
    static {
        SUPPORTED_LANGUAGES.put("python", new LanguageConfig("python", ".py", false));
        SUPPORTED_LANGUAGES.put("javascript", new LanguageConfig("node", ".js", false));
        SUPPORTED_LANGUAGES.put("typescript", new LanguageConfig(null, ".ts", true) {
            @Override
            public ProcessBuilder getProcessBuilder(String filePath) {
                // For TypeScript, we need to use ts-node or transpile to JS first
                String tsNodePath = System.getenv("TS_NODE_PATH");
                if (tsNodePath != null && !tsNodePath.isEmpty()) {
                    return new ProcessBuilder(tsNodePath, filePath);
                } else {
                    return new ProcessBuilder("npx", "ts-node", filePath);
                }
            }
        });
        SUPPORTED_LANGUAGES.put("java", new LanguageConfig(null, ".java", true) {
            @Override
            public ProcessBuilder getProcessBuilder(String filePath) {
                // For Java, we need to compile first then run
                // Extract class name from code (assumed to be same as filename)
                String className = new File(filePath).getName().replace(".java", "");
                String directory = new File(filePath).getParent();
                
                // Compile step handled separately in executeCode
                // Return process builder for running the compiled class
                return new ProcessBuilder("java", "-cp", directory, className);
            }
            
            @Override
            public boolean needsPreparation() {
                return true;
            }
            
            @Override
            public String prepareForExecution(String filePath) throws IOException, InterruptedException {
                // Compile the Java file
                ProcessBuilder compileBuilder = new ProcessBuilder("javac", filePath);
                Process compileProcess = compileBuilder.start();
                
                // Capture compilation errors
                StringBuilder compileOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        compileOutput.append(line).append("\n");
                    }
                }
                
                boolean compilationSuccessful = compileProcess.waitFor(30, TimeUnit.SECONDS) && 
                                               compileProcess.exitValue() == 0;
                
                if (!compilationSuccessful) {
                    return "Compilation failed: " + compileOutput.toString();
                }
                
                return null; // No errors
            }
        });
        SUPPORTED_LANGUAGES.put("cpp", new LanguageConfig(null, ".cpp", true) {
            @Override
            public ProcessBuilder getProcessBuilder(String filePath) {
                // For C++, we compile to an executable then run it
                String exePath = filePath.replace(".cpp", "");
                return new ProcessBuilder(exePath);
            }
            
            @Override
            public boolean needsPreparation() {
                return true;
            }
            
            @Override
            public String prepareForExecution(String filePath) throws IOException, InterruptedException {
                // Compile the C++ file
                String outputPath = filePath.replace(".cpp", "");
                List<String> compileCommand = new ArrayList<>();
                
                // Use g++ or clang++ based on availability
                String compiler = "g++";
                compileCommand.add(compiler);
                compileCommand.add("-o");
                compileCommand.add(outputPath);
                compileCommand.add(filePath);
                
                ProcessBuilder compileBuilder = new ProcessBuilder(compileCommand);
                compileBuilder.redirectErrorStream(true);
                Process compileProcess = compileBuilder.start();
                
                // Capture compilation output
                StringBuilder compileOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        compileOutput.append(line).append("\n");
                    }
                }
                
                boolean compilationSuccessful = compileProcess.waitFor(30, TimeUnit.SECONDS) && 
                                               compileProcess.exitValue() == 0;
                
                if (!compilationSuccessful) {
                    return "Compilation failed: " + compileOutput.toString();
                }
                
                // Make the compiled file executable on Unix-like systems
                File executableFile = new File(outputPath);
                executableFile.setExecutable(true);
                
                return null; // No errors
            }
        });
    }
    
    private static class LanguageConfig {
        String command;
        String extension;
        boolean needsCompilation;
        
        LanguageConfig(String command, String extension, boolean needsCompilation) {
            this.command = command;
            this.extension = extension;
            this.needsCompilation = needsCompilation;
        }
        
        public ProcessBuilder getProcessBuilder(String filePath) {
            return new ProcessBuilder(command, filePath);
        }
        
        public boolean needsPreparation() {
            return false;
        }
        
        public String prepareForExecution(String filePath) throws IOException, InterruptedException {
            return null; // No preparation needed by default
        }
    }

    /**
     * Executes code in the specified language
     * 
     * @param language The programming language to use (java, python, javascript, typescript, c++)
     * @param code The code to execute
     * @return The output of the executed code
     */
    @Tool(description = "Execute code in the specified language. Supported languages: java, python, javascript, typescript, c++.")
    public String executeCode(String language, String code) {
        try {
            // Normalize language name
            language = language.toLowerCase().trim();
            
            // Check if language is supported
            if (!SUPPORTED_LANGUAGES.containsKey(language)) {
                return "Unsupported language: " + language + ". Supported languages are: " + 
                       String.join(", ", SUPPORTED_LANGUAGES.keySet());
            }
            
            // Check if the code contains literal \n characters (backslash followed by n)
            // This typically happens when the client sends a string with escaped newlines
            if (code.contains("\\n")) {
                // Replace all occurrences of \n with actual newlines
                // Note: The replace method uses regex, so we need to escape the backslash
                code = code.replaceAll("\\\\n", "\n");
            }
            
            LanguageConfig config = SUPPORTED_LANGUAGES.get(language);
            
            // Create a temporary directory for the code files
            Path tempDir = Files.createTempDirectory("code_exec_");
            
            // Handle Java files specially, as they need class name matching filename
            String filename;
            if ("java".equals(language)) {
                // For Java, extract the public class name from the code
                filename = extractJavaClassName(code) + ".java";
            } else {
                filename = "code_" + UUID.randomUUID().toString() + config.extension;
            }
            
            // Create the code file
            Path filePath = tempDir.resolve(filename);
            File codeFile = filePath.toFile();
            
            try {
                // Write the code to the file
                Files.writeString(filePath, code);
                
                // Make the file executable if needed
                if ("bash".equals(language)) {
                    codeFile.setExecutable(true);
                }
                
                // Prepare for execution if needed (compilation)
                if (config.needsPreparation()) {
                    String prepResult = config.prepareForExecution(codeFile.getAbsolutePath());
                    if (prepResult != null) {
                        return prepResult; // Return compilation errors
                    }
                }
                
                // Get the appropriate process builder for this language
                ProcessBuilder processBuilder = config.getProcessBuilder(codeFile.getAbsolutePath());
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                
                // Capture output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                // Wait for process to complete with timeout
                boolean completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    return "Execution timed out after " + TIMEOUT_SECONDS + " seconds";
                }
                
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    return output.toString();
                } else {
                    return "Execution failed with exit code " + exitCode + ":\n" + output.toString();
                }
            } finally {
                // Clean up temp files
                deleteDirectory(tempDir.toFile());
            }
        } catch (Exception e) {
            return "Error executing code: " + e.getMessage();
        }
    }
    
    // Helper method to extract the class name from Java code
    private String extractJavaClassName(String code) {
        // Basic implementation - extracting the class name from "public class X"
        // Could be improved with proper parsing, this is simplified
        String className = "Main"; // Default name
        
        for (String line : code.split("\n")) {
            line = line.trim();
            if (line.contains("public class ")) {
                String[] parts = line.split("public class ");
                if (parts.length > 1) {
                    String afterClassKeyword = parts[1].trim();
                    parts = afterClassKeyword.split("[ {]"); // Split on space or opening brace
                    if (parts.length > 0) {
                        className = parts[0].trim();
                        break;
                    }
                }
            }
        }
        
        return className;
    }
    
    // Helper method to recursively delete a directory
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    public static void main(String[] args) {
        CodeExecutionService service = new CodeExecutionService();
        
        // Test Python
        System.out.println("=== Python Test ===");
        System.out.println(service.executeCode("python", "print('Hello, world!')\nprint(2 + 3)"));
        
        // Test JavaScript
        System.out.println("\n=== JavaScript Test ===");
        System.out.println(service.executeCode("javascript", "console.log('Hello from Node.js!');"));
            
        // Test Java (if JDK is available)
        System.out.println("\n=== Java Test ===");
        System.out.println(service.executeCode("java", 
            "public class HelloWorld {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello from Java!\");\n" +
            "    }\n" +
            "}"));
            
        // Test C++ (if compiler is available)
        System.out.println("\n=== C++ Test ===");
        System.out.println(service.executeCode("cpp",
            "#include <iostream>\n" +
            "int main() {\n" +
            "    std::cout << \"Hello from C++!\" << std::endl;\n" +
            "    return 0;\n" +
            "}"));
    }
} 