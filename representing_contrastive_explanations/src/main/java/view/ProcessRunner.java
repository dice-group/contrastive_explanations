package view;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessRunner {
    public static void runReasoner() {
        try {
            String base = "/Users/ashikmr/Desktop/contrastive_explanations";
            String jar = base + "/target/contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar";
            String classes = base + "/target/classes";
            String input = "/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/outputs/reasoner/family_json_input.json";

            String[] command = {
                    "java",
                    "-cp",
                    classes + ":" + jar,
                    "anonymized.contrastive.experiments.FactFoilFileWriter",
                    input
            };

            ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Error while generating explanations: ", e);
        }
    }

    public static void main (String[] args) {
        runReasoner();
    }

    public static String runGraphviz() {
        try {
            String venvPath = "/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/scripts/venv";

            // Step 1: Create virtual environment
            ProcessBuilder createVenv = new ProcessBuilder(
                    "python3", "-m", "venv", venvPath
            );
            createVenv.inheritIO();
            Process p1 = createVenv.start();
            p1.waitFor();

            new ProcessBuilder(venvPath + "/bin/pip", "install", "-r",
                    "/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/requirements.txt")
                    .inheritIO().start().waitFor();

            Process p2 = getProcess(venvPath);

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

    private static Process getProcess(String venvPath) throws IOException {
        String pythonExe = venvPath + "/bin/python";
        String input = "/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/outputs/reasoner/family_output.json";

        ProcessBuilder runScript = new ProcessBuilder(
                pythonExe,
                "/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/scripts/graph_representation.py",
                "--input-file", input
        );
        runScript.directory(new File("/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations"));

        runScript.redirectErrorStream(true);
        return runScript.start();
    }
}
