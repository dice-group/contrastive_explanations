package graphviz;

import process.PythonProcessRunner;
import process.ReasonerProcessRunner;
import utils.CommonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static constants.ErrorMessageConstants.*;
import static constants.EnvConstants.*;
import static constants.EnvConstants.PATH;
import static constants.PathConstants.*;
import static io.JsonWriter.createInputJsonFile;
import static io.ResourceExtractor.extractGraphvizBundle;
import static utils.CommonUtil.getTempFile;
import static utils.CommonUtil.isMacOS;

/**
 * Service responsible for invoking Graphviz and a helper Python script to produce
 * binary dot and convert the dot to graph images.
 *
 * <p>This class:
 * - writes DOT text to a file and invokes the Graphviz `dot` binary to produce a PNG,
 * - runs a Python script from a virtual environment to generate graph representation.</p>
 */

public class GraphvizService {

    private final Path venvDir;
    private final Path gvRoot;

    public GraphvizService(final Path venvDir, final Path gvRoot) {
        this.venvDir = venvDir;
        this.gvRoot = gvRoot;
    }

    /**
     * Convert the provided DOT graph string into a PNG file.
     *
     * @param dot DOT format graph content
     * @return Path to the generated PNG file
     */

    public Path convertDotToPng(String dot) throws Throwable {
        // Ensure outputs directory and compute PNG file path
        Path pngFile = CommonUtil.createDirectory(OUTPUT_DIR).resolve(GRAPHS_PNG_FILE);
        try (Writer w = Files.newBufferedWriter(pngFile.resolveSibling(GRAPH_DOT_FILE))) {
            w.write(dot);
        } catch (IOException e) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_2, e);
        }
        // Locate the dot binary inside the Graphviz bundle
        String dotBinaryName = "dot.exe";
        if(isMacOS()){
            dotBinaryName = "dot";
        }
        Path dotBinary = this.gvRoot.resolve(BIN_DIR).resolve(dotBinaryName);
        Path dotFile = CommonUtil.createDirectory(OUTPUT_DIR).resolve(GRAPH_DOT_FILE);

        // Build the process to run: dot -Tpng input.dot -o output.png
        ProcessBuilder pb = new ProcessBuilder(
                dotBinary.toAbsolutePath().toString(),
                "-Tpng",
                dotFile.toAbsolutePath().toString(),
                "-o",
                pngFile.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        // Configure environment required by Graphviz
        Map<String, String> env = pb.environment();
        if(isMacOS()){
            env.put(DYLD_LIBRARY_PATH, this.gvRoot.resolve(LIB_DIR).toString());
            env.put(GVBINDIR, this.gvRoot.resolve(GRAPHVIZ_LIB_DIR).toString());
            env.put(GVPLUGIN_PATH, this.gvRoot.resolve(GRAPHVIZ_LIB_DIR).toString());
        }else {
            env.put(DYLD_LIBRARY_PATH, this.gvRoot.resolve(BIN_DIR).toString());
            env.put(GVBINDIR, this.gvRoot.resolve(BIN_DIR).toString());
            env.put(GVPLUGIN_PATH, this.gvRoot.resolve(BIN_DIR).toString());
        }
        env.put(PATH, this.gvRoot.resolve(BIN_DIR) + ":" + env.getOrDefault(PATH, ""));
        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_1 + exit);
        }
        return pngFile;
    }

    /**
     * Run the graph representation Python script inside the configured virtual environment
     * and return the script output as a dot string.
     *
     * @return Output produced by the Python graph representation script: dot
     */
    public String graphvizRunner() {
        try {
            // Resolve Python executable inside the virtual environment
            Path pythonExe = this.venvDir.resolve("Scripts").resolve("python.exe");;
            if(isMacOS()){
                pythonExe = this.venvDir.resolve(BIN_DIR).resolve(PYTHON);
            }
            Path pythonScriptPath = getTempFile(SCRIPT + "/" + GRAPH_REPRESENTATION_CLASS);
            Path inputFile = CommonUtil.createDirectory(OUTPUT_DIR).resolve(REASONER_OUTPUT_JSON + ".json");

            // Build process: python script --input-file <path>
            ProcessBuilder runScript = new ProcessBuilder(
                    pythonExe.toString(),
                    pythonScriptPath.toString(),
                    "--input-file", inputFile.toString()
            );
            runScript.redirectErrorStream(true);
            Process p2 = runScript.start();
            StringBuilder dotOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    dotOutput.append(line).append("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int exitCode = p2.waitFor();
            if (exitCode != 0) throw new RuntimeException(RUNTIME_ERROR_MESSAGE_7);
            return dotOutput.toString().trim();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Throwable {
        // createInputJsonFile("abc", "def", "ghi");
        String jsonInputFilePath = String.valueOf(CommonUtil.createDirectory(INPUT_DIR).resolve(REASONER_INPUT_JSON));
        ReasonerProcessRunner processRunner = new ReasonerProcessRunner(jsonInputFilePath);
        processRunner.runReasoner();
    }
}
