package process;

import utils.CommonUtil;

import java.io.*;
import java.nio.file.Path;

import static constants.ErrorMessageConstants.*;
import static constants.EnvConstants.*;
import static constants.PathConstants.*;

public class ReasonerProcessRunner {

    private final String inputFilePath;

    public ReasonerProcessRunner(final String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

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

    private ProcessBuilder getProcessBuilder(Path inputFile) throws IOException {
        Path jar = CommonUtil.createDirectory(BIN_DIR).resolve(RESOURCE_JAR);
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
