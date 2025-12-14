package view;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ProcessRunner {
    public static void runReasoner() throws IOException {
        createJar();
        try {
            Path inputFile = JsonCreator.PluginFiles.inputsDir().resolve("family_json_input.json");
            ProcessBuilder pb = getProcessBuilder(inputFile);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Error while generating explanations: ", e);
        }
    }
    private static ProcessBuilder getProcessBuilder(Path inputFile) throws IOException {
        Path jar = JsonCreator.PluginFiles.binsDir().resolve("contrastive-explanations-runner.jar");
        String[] command = {
                "java",
                "-cp",
                jar.toAbsolutePath().toString(),
                "anonymized.contrastive.experiments.FactFoilFileWriter",
                inputFile.toAbsolutePath().toString()
        };

//        String base = "/Users/ashikmr/Desktop/contrastive_explanations";
//        String jar = base + "/target/contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar";
//        String classes = base + "/target/classes";
//        String[] command = {
//                "java",
//                "-cp",
//                classes + ":" + jar,
//                "anonymized.contrastive.experiments.FactFoilFileWriter",
//                inputFile.toString()
//        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb;
    }

    public static void main(String[] args) throws Throwable {
//        createJar();
//        JsonCreator.createInputJsonFile(
//                "F3F46",
//                "F7F118",
//                "Parent and (hasChild some (hasParent some Parent))"
//        );
        runReasoner();
//        GraphvizRender.toPng(ProcessRunner.runGraphviz());

    }

    private static void installRequirements(Path venvDir, Path requirementsFile)
            throws IOException, InterruptedException {
        Path pythonExe = venvDir.resolve("bin").resolve("python");

        ProcessBuilder pb = new ProcessBuilder(
                pythonExe.toString(),
                "-m", "pip", "install",
                "-r", requirementsFile.toString()
        );
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("pip install failed, exit code: " + exit);
        }
    }

    private static Path createVenv() throws IOException, InterruptedException {
        Path venvDir = Files.createTempDirectory("contrastive_venv_");
        ProcessBuilder pb = new ProcessBuilder(
                "python3", "-m", "venv", venvDir.toString()
        );
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Failed to create venv, exit code: " + exit);
        }
        return venvDir;
    }

    public static String runGraphviz() {
        try {
            Path venvDir = createVenv();
            Path reqFile = getTempFile("requirements.txt");
            installRequirements(venvDir, reqFile);
            Path pythonExe = venvDir.resolve("bin").resolve("python");
            Path scriptPath = getTempFile("scripts/graph_representation.py");
            Path inputFile = JsonCreator.PluginFiles.outputsDir().resolve("family_output.json");

            ProcessBuilder runScript = new ProcessBuilder(
                    pythonExe.toString(),
                    scriptPath.toString(),
                    "--input-file", inputFile.toString()
            );
            runScript.redirectErrorStream(true);
            Process p2 = runScript.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = p2.waitFor();
            System.out.println("Script exited with code: " + exitCode);
            if (exitCode != 0) System.err.println("The Python script failed to execute correctly.");
            return output.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException("Error while generating graphviz image: ", e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getTempFile(String resourcePath) throws IOException {
        try (InputStream in = ProcessRunner.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            Path tempFile = Files.createTempFile("contrastive_", "_" + Paths.get(resourcePath).getFileName());
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }

    public static void createJar() throws IOException {
        Path binDir = JsonCreator.PluginFiles.baseDir().resolve("bin");
        Files.createDirectories(binDir);
        Path outJar = binDir.resolve("contrastive-explanations-runner.jar");
        try (InputStream in = ProcessRunner.class.getClassLoader()
                .getResourceAsStream("lib/contrastive-explanations-reasoner.jar")) {
            if (in == null) {
                throw new FileNotFoundException("Resource lib/factfoil-runner.jar not found inside plugin");
            }
            Files.copy(in, outJar, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
