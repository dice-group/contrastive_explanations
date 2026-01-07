package graphviz;

import process.PythonProcessRunner;
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
import static io.ResourceExtractor.extractGraphvizBundle;
import static utils.CommonUtil.getTempFile;

public class GraphvizService {

    private final Path venvDir;
    private final Path gvRoot;

    public GraphvizService(final Path venvDir, final Path gvRoot) {
        this.venvDir = venvDir;
        this.gvRoot = gvRoot;
    }

    public Path convertDotToPng(String dot) throws Throwable {
        Path pngFile = CommonUtil.createDirectory(OUTPUT_DIR).resolve(GRAPHS_PNG_FILE);
        try (Writer w = Files.newBufferedWriter(pngFile.resolveSibling(GRAPH_DOT_FILE))) {
            w.write(dot);
        } catch (IOException e) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_2, e);
        }
        Path dotBinary = this.gvRoot.resolve(BIN_DIR).resolve(DOT_BINARY);

        Path dotFile = CommonUtil.createDirectory(OUTPUT_DIR).resolve(GRAPH_DOT_FILE);
        ProcessBuilder pb = new ProcessBuilder(
                dotBinary.toAbsolutePath().toString(),
                "-Tpng",
                dotFile.toAbsolutePath().toString(),
                "-o",
                pngFile.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put(DYLD_LIBRARY_PATH, this.gvRoot.resolve(LIB_DIR).toString());
        env.put(GVBINDIR, this.gvRoot.resolve(GRAPHVIZ_LIB_DIR).toString());
        env.put(GVPLUGIN_PATH, this.gvRoot.resolve(GRAPHVIZ_LIB_DIR).toString());
        env.put(PATH, this.gvRoot.resolve(BIN_DIR) + ":" + env.getOrDefault(PATH, ""));
        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException(RUNTIME_ERROR_MESSAGE_1 + exit);
        }
        return pngFile;
    }

    public String graphvizRunner() {
        try {
            Path pythonExe = this.venvDir.resolve(BIN_DIR).resolve(PYTHON);
            Path pythonScriptPath = getTempFile(SCRIPT + "/" + GRAPH_REPRESENTATION_CLASS);
            Path inputFile = CommonUtil.createDirectory(OUTPUT_DIR).resolve(REASONER_OUTPUT_JSON + ".json");
            ProcessBuilder runScript = new ProcessBuilder(
                    pythonExe.toString(),
                    pythonScriptPath.toString(),
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int exitCode = p2.waitFor();
            if (exitCode != 0) throw new RuntimeException(RUNTIME_ERROR_MESSAGE_7);
            return output.toString().trim();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Throwable {
        String dot =
                "digraph {\n" +
                        "\tgraph [bgcolor=lightyellow fontcolor=black fontsize=20 label=\"Query: married some (married some (hasSibling some Parent)) | fact=F10F179 | foil=F9F169\" rankdir=LR splines=true]\n" +
                        "\tsubgraph cluster_fact {\n" +
                        "\t\tcolor=lightyellow fontcolor=black fontsize=16 label=\"\" labelloc=t style=filled\n" +
                        "\t\tF10F179 -> F10M180 [label=married]\n" +
                        "\t\tF10M180 -> F10F179 [label=married]\n" +
                        "\t\tF10F179 -> F10M173 [label=hasSibling]\n" +
                        "\t\tF10M173 -> Father [label=Type]\n" +
                        "\t}\n" +
                        "\tsubgraph cluster_foil_common {\n" +
                        "\t\tcolor=lightyellow fontcolor=black fontsize=16 label=\"\" labelloc=t style=filled\n" +
                        "\t\tF9F169 -> F9M170 [label=married]\n" +
                        "\t}\n" +
                        "\tsubgraph cluster_foil_diff {\n" +
                        "\t\tcolor=lightyellow fontcolor=black fontsize=16 labelloc=t style=filled\n" +
                        "\t\tF9M170 -> F9M170 [label=married color=\"#ff0000\" penwidth=2 style=dashed]\n" +
                        "\t\tF9M170 -> __C1 [label=hasSibling color=\"#ff0000\" penwidth=2 style=dashed]\n" +
                        "\t\t__C1 -> Father [label=Type color=\"#ff0000\" penwidth=2 style=dashed]\n" +
                        "\t}\n" +
                        "\tF10F179 [fillcolor=\"#3399FF\" style=filled]\n" +
                        "\tF10M180 [fillcolor=\"#3399FF\" style=filled]\n" +
                        "\tF10M173 [fillcolor=\"#3399FF\" style=filled]\n" +
                        "\tFather [fillcolor=lightyellow style=filled]\n" +
                        "\tF9F169 [fillcolor=\"#6AC780\" style=filled]\n" +
                        "\tF9M170 [fillcolor=\"#6AC780\" style=filled]\n" +
                        "\t__C1 [fillcolor=\"#6AC780\" style=filled]\n" +
                        "\tlegend [label=<\n" +
                        "            <TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">\n" +
                        "              <TR><TD COLSPAN=\"2\"><B>Legend</B></TD></TR>\n" +
                        "              <TR><TD BGCOLOR=\"#3399FF\"></TD><TD>Fact node</TD></TR>\n" +
                        "              <TR><TD BGCOLOR=\"#6AC780\"></TD><TD>Foil node</TD></TR>\n" +
                        "              <TR><TD BGCOLOR=\"lightyellow\"></TD><TD>Common node</TD></TR>\n" +
                        "              <TR><TD><FONT COLOR=\"#ff0000\"><I>--------</I></FONT></TD><TD>Missing edge in foil</TD></TR>\n" +
                        "            </TABLE>\n" +
                        "        > shape=none]\n" +
                        "\n" +
                        "\n" +
                        "}\n";
        PythonProcessRunner pythonProcess = new PythonProcessRunner();
        Path venvDir = pythonProcess.createVenvAndInstallRequirements();
        Path gvRoot = extractGraphvizBundle();

        GraphvizService graphvizService = new GraphvizService(venvDir, gvRoot);
        graphvizService.convertDotToPng(dot);
    }

}
