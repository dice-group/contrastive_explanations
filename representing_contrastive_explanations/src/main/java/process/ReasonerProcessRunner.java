package process;

import utils.CommonUtil;

import java.io.*;
import java.nio.file.Path;

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
        try {
            ProcessBuilder pb = getProcessBuilder(Path.of(this.inputFilePath));
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException(RUNTIME_ERROR_MESSAGE_4 + exit);
            }
        } catch (Exception e) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_4, e);
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
