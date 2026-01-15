package process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static constants.ErrorMessageConstants.RUNTIME_ERROR_MESSAGE_5;
import static constants.ErrorMessageConstants.RUNTIME_ERROR_MESSAGE_6;
import static constants.PathConstants.*;
import static utils.CommonUtil.getTempFile;

/**
 * Helper to create a Python virtual environment and install Python dependencies.
 *
 * <p>This class encapsulates two responsibilities:
 * - createVenvAndInstallRequirements: create an isolated venv and install packages,
 * - installRequirements: call `pip install -r requirements.txt` inside the venv.</p>
 */
public class PythonProcessRunner {
    /**
     * Create a temporary Python virtual environment and install the required packages.
     *
     * @return Path to the created virtual environment root directory
     * @throws IOException on filesystem I/O errors
     * @throws InterruptedException if the subprocess is interrupted while waiting
     */
    public Path createVenvAndInstallRequirements() throws IOException, InterruptedException {
        // Create a unique temporary directory to host the virtual environment
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
        // Install requirements from a temporary requirements file (returned by getTempFile)
        installRequirements(venvDir, getTempFile(REQUIREMENTS_TXT));
        return venvDir;
    }

    /**
     * Install Python packages into the provided virtual environment using pip.
     *
     * <p>This method resolves the Python executable under the venv's `bin` directory and
     * runs `python -m pip install -r <requirementsFile>`.</p>
     *
     * @param venvDir virtual environment root directory
     * @param requirementsFile path to the requirements file to install
     * @throws IOException on filesystem/process I/O errors
     * @throws InterruptedException if the subprocess is interrupted while waiting
     */
    private void installRequirements(Path venvDir, Path requirementsFile)
            throws IOException, InterruptedException {
        Path pythonExe = venvDir.resolve(BIN_DIR).resolve(PYTHON);
        // Build the command: <venv>/bin/python -m pip install -r <requirementsFile>
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
