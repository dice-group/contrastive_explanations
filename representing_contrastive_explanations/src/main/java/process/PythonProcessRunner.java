package process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static constants.ErrorMessageConstants.RUNTIME_ERROR_MESSAGE_5;
import static constants.ErrorMessageConstants.RUNTIME_ERROR_MESSAGE_6;
import static constants.PathConstants.*;
import static utils.CommonUtil.getTempFile;

public class PythonProcessRunner {

    public Path createVenvAndInstallRequirements() throws IOException, InterruptedException {
        Path venvDir = Files.createTempDirectory("contrastive_venv_");
        ProcessBuilder pb = new ProcessBuilder(
                "python3", "-m", "venv", venvDir.toString()
        );
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_6 + exit);
        }
        installRequirements(venvDir, getTempFile(REQUIREMENTS_TXT));
        return venvDir;
    }

    private void installRequirements(Path venvDir, Path requirementsFile)
            throws IOException, InterruptedException {
        Path pythonExe = venvDir.resolve(BIN_DIR).resolve(PYTHON);

        ProcessBuilder pb = new ProcessBuilder(
                pythonExe.toString(),
                "-m", "pip", "install",
                "-r", requirementsFile.toString()
        );
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_5 + exit);
        }
    }

}
