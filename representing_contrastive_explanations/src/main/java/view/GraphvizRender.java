package view;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static view.Util.extractGraphvizBundle;


public class GraphvizRender {

    public static Path toPng(String dot) throws Throwable {
        Path pngFile = JsonCreator.PluginFiles.outputsDir().resolve("graphs.png");
        try (Writer w = Files.newBufferedWriter(pngFile.resolveSibling("graph.dot"))) {
            w.write(dot);
        }

        Path gvRoot = extractGraphvizBundle();
        Path dotBinary = gvRoot.resolve("bin/dot");

        Path dotFile = JsonCreator.PluginFiles.outputsDir().resolve("graph.dot");
        ProcessBuilder pb = new ProcessBuilder(
                dotBinary.toAbsolutePath().toString(),
                "-Tpng",
                dotFile.toAbsolutePath().toString(),
                "-o",
                pngFile.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        env.put("DYLD_LIBRARY_PATH", gvRoot.resolve("lib").toString());
        env.put("GVBINDIR", gvRoot.resolve("lib/graphviz").toString());
        env.put("GVPLUGIN_PATH", gvRoot.resolve("lib/graphviz").toString());
        env.put("PATH", gvRoot.resolve("bin") + ":" + env.getOrDefault("PATH", ""));
        Process process = pb.start();

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            r.lines().forEach(System.out::println);
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("dot failed with exit code " + exit);
        }
        return pngFile;
    }

    public static void main (String[] args) throws Throwable {
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
        toPng(dot);
    }
}


