package process;

import utils.CommonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static constants.ErrorMessageConstants.*;
import static constants.EnvConstants.*;
import static constants.PathConstants.*;

/**
 * Helper to invoke the packaged contrastive explanations reasoner JAR as an external
 * Java process using the provided input file path.
 */
public class ReasonerProcessRunner {

    private final String inputFilePath;

    public ReasonerProcessRunner(final String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    /**
     * Launch the reasoner as a separate JVM process.
     */
    public void runReasoner() {
        String output = "";
        int exit = -1;
        try {
            ProcessBuilder pb = getProcessBuilder(Path.of(this.inputFilePath));
            Process process = pb.start();
            
            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
             exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException(RUNTIME_ERROR_MESSAGE_4 + " exit=" + exit + "\n" + output);
            }
        } catch (Exception e) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_4 + " exit=" + exit + "\n" + output);
        }
    }

    /**
     * Build a ProcessBuilder configured to run the packaged reasoner JAR.
     *
     * @param inputFile resolved input file path passed to the reasoner main class
     * @return configured ProcessBuilder with stderr merged into stdout
     * @throws IOException if creation of the `bin` directory or path resolution fails
     */
    private ProcessBuilder getProcessBuilder(Path inputFile) throws IOException {
        // Ensure bin directory exists and locate the reasoner jar inside it
        Path jar = CommonUtil.createDirectory(BIN_DIR).resolve(RESOURCE_JAR);
        // Command to run the reasoner: java -cp <jar> <main_class> <input_file>
        String[] command = {
                "java",
                "-cp",
                jar.toAbsolutePath().toString(),
                REASONER_MAIN_CLASS,
                inputFile.toAbsolutePath().toString()
        };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb;
    }
}