package graphviz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import utils.CommonUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static constants.ErrorMessageConstants.*;
import static constants.EnvConstants.*;
import static constants.EnvConstants.PATH;
import static constants.PathConstants.*;
import static utils.CommonUtil.isMacOS;

/**
 * Service responsible for invoking Graphviz and to produce
 * binary dot and convert the dot to graph images.
 */
public class GraphvizService {

    private final Path gvRoot;

    /**
     * Construct the service with the Graphviz bundle root path.
     *
     * @param gvRoot root path to the Graphviz distribution/bundle
     */
    public GraphvizService(final Path gvRoot) {
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
        String dotBinaryName = DOT_BINARY_WINDOWS;
        if (isMacOS()) {
            dotBinaryName = DOT_BINARY;
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
        if (isMacOS()) {
            env.put(DYLD_LIBRARY_PATH, this.gvRoot.resolve(LIB_DIR).toString());
            env.put(GVBINDIR, this.gvRoot.resolve(GRAPHVIZ_LIB_DIR).toString());
            env.put(GVPLUGIN_PATH, this.gvRoot.resolve(GRAPHVIZ_LIB_DIR).toString());
        } else {
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
     * Simple DTO representing a block in the reasoner output JSON.
     */
    public static class Block {
        public String namespace;
        public String class_expression;
        public List<Result> results;
    }

    /**
     * DTO for individual explanation results.
     * */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        public String fact;
        public String foil;
        public List<String> common;
        public List<String> different;
        public Map<String, String> fact_mapping;
        public Map<String, String> foil_mapping;
        public List<String> conflicts;
    }

    /**
     * Read the reasoner JSON output and build a single DOT string for all blocks.
     *
     * @return DOT content generated from the JSON file
     */
    public String getDotFromJson() throws Exception {
        Path inputFile = CommonUtil.createDirectory(OUTPUT_DIR).resolve(REASONER_OUTPUT_JSON + ".json");
        StringBuilder dot = new StringBuilder();
        ObjectMapper om = new ObjectMapper();
        // Deserialize JSON array into a list of Block objects
        List<Block> blocks = om.readValue(
                new File(String.valueOf(inputFile)),
                om.getTypeFactory().constructCollectionType(List.class, Block.class)
        );
        for (Block block : blocks) {
            String classExpr = block.class_expression;
            List<Result> results = block.results;
            for (Result res : results) {
                dot.append(buildDot(classExpr, res));
            }
        }
        return dot.toString();
    }

    /**
     * Build a DOT subgraph for a single explanation result.\
     *
     * @param classExpr the queried class expression (for title)
     * @param result    explanation result holding axioms and mappings
     * @return DOT for the result
     */
    private String buildDot(String classExpr, Result result) {
        String factId = result.fact;
        String foilId = result.foil;

        List<String> commonAxioms = result.common;
        List<String> differentAxioms = result.different;

        Map<String, String> factMapping = result.fact_mapping;
        Map<String, String> foilMapping = result.foil_mapping;

        // Build edges with mappings applied
        List<Edge> factEdges = new ArrayList<>();
        factEdges.addAll(applyMappingToEdges(commonAxioms, factMapping));
        factEdges.addAll(applyMappingToEdges(differentAxioms, factMapping));

        List<Edge> foilCommonEdges = applyMappingToEdges(commonAxioms, foilMapping);
        List<Edge> foilDiffEdges = applyMappingToEdges(differentAxioms, foilMapping);

        String title = "\"Query: " + classExpr + " | fact=" + factId + " | foil=" + foilId + "\"";

        // Track nodes and flags
        Map<String, NodeFlags> nodeFlags = new LinkedHashMap<>();

        // DOT builder
        StringBuilder sb = new StringBuilder(16_384);
        sb.append("digraph {\n");
        sb.append("\tgraph [bgcolor=lightyellow fontcolor=black fontsize=20 label=").append(title).append(" rankdir=LR splines=true]\n");

        // --- FACT cluster ---
        sb.append("\tsubgraph cluster_fact {\n")
                .append("color=lightyellow fontcolor=black fontsize=16 label=\"\" labelloc=t style=filled\n");
        for (Edge e : factEdges) {
            sb.append("\t").append(e.subj).append(" -> ").append(e.obj)
                    .append(" [label=").append(e.rel).append("]\n");
            flag(nodeFlags, e.subj, true, false);
            flag(nodeFlags, e.obj, true, false);
        }
        sb.append("  }\n");

        // --- FOIL common cluster ---
        sb.append("subgraph cluster_foil_common {\n")
                .append("color=lightyellow fontcolor=black fontsize=16 label=\"\" labelloc=t style=filled\n");
        for (Edge e : foilCommonEdges) {
            sb.append("\t").append(e.subj).append(" -> ").append(e.obj)
                    .append(" [label=").append(e.rel).append("]\n");
            flag(nodeFlags, e.subj, false, true);
            flag(nodeFlags, e.obj, false, true);
        }
        sb.append("  }\n");

        // --- FOIL different cluster (dashed red) ---
        sb.append("subgraph cluster_foil_diff {\n")
                .append("color=lightyellow fontcolor=black fontsize=16 labelloc=t style=filled\n");
        for (Edge e : foilDiffEdges) {
            sb.append("\t").append(e.subj).append(" -> ").append(e.obj)
                    .append(" [label=").append(e.rel)
                    .append(" color=\"#ff0000\" penwidth=2 style=dashed]\n");
            flag(nodeFlags, e.subj, false, true);
            flag(nodeFlags, e.obj, false, true);
        }
        sb.append("  }\n");

        // Apply node coloring after edges are in
        for (Map.Entry<String, NodeFlags> entry : nodeFlags.entrySet()) {
            String node = entry.getKey();
            NodeFlags flags = entry.getValue();
            String fill = colorForNode(flags.fact, flags.foil);

            sb.append("\t").append(node)
                    .append(" [fillcolor=").append("\"").append(fill).append("\"")
                    .append(" style=filled").append("]\n");
        }
        sb.append("  legend [label=<\n")
                .append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">\n")
                .append("<TR><TD COLSPAN=\"2\"><B>Legend</B></TD></TR>\n")
                .append("<TR><TD BGCOLOR=\"#3399FF\"></TD><TD>Fact node</TD></TR>\n")
                .append("<TR><TD BGCOLOR=\"#6AC780\"></TD><TD>Foil node</TD></TR>\n")
                .append("<TR><TD><FONT COLOR=\"#ff0000\"><I>--------</I></FONT></TD><TD>Missing edge in foil</TD></TR>\n")
                .append("</TABLE>\n")
                .append("  >").append(" shape=none]");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Convert a list of axiom strings into Edge objects applying a node mapping.
     *
     * @param axiomStrings raw axiom strings (may contain nulls/empty entries)
     * @param mapping      mapping from original node identifiers to display identifiers
     * @return list of Edge records representing the mapped edges
     */
    private List<Edge> applyMappingToEdges(List<String> axiomStrings, Map<String, String> mapping) {
        List<Edge> edges = new ArrayList<>();
        for (String stmt : axiomStrings) {
            if (stmt == null) continue;
            String s = stmt.trim();
            if (s.isEmpty()) continue;
            String[] parts = s.split("\\s+");
            if (parts.length != 3) continue;
            String subj = parts[0];
            String rel = parts[1];
            String obj = parts[2];
            String mappedSubj = mapping.getOrDefault(subj, subj);
            String mappedObj = mapping.getOrDefault(obj, obj);
            edges.add(new Edge(mappedSubj, rel, mappedObj));
        }
        return edges;
    }

    /**
     * Mark node flags indicating presence in fact and/or foil clusters.
     *
     * @param nodeFlags map being updated
     * @param node      node identifier
     * @param isFact    true if node appears in the fact cluster
     * @param isFoil    true if node appears in the foil cluster
     */
    private void flag(Map<String, NodeFlags> nodeFlags, String node, boolean isFact, boolean isFoil) {
        NodeFlags nf = nodeFlags.computeIfAbsent(node, k -> new NodeFlags());
        if (isFact) nf.fact = true;
        if (isFoil) nf.foil = true;
    }

    /**
     * Choose a fill color based on node presence flags.
     *
     * @param fact whether the node is in the fact cluster
     * @param foil whether the node is in the foil cluster
     * @return color string suitable for DOT fillcolor attribute
     */
    private String colorForNode(boolean fact, boolean foil) {
        if (fact && foil) return "lightyellow";  // present in both
        if (fact) return "#3399FF";              // fact-only
        if (foil) return "#6AC780";              // foil-only
        return "lightyellow";
    }

    // Simple holder for node presence flags
    private static class NodeFlags {
        boolean fact;
        boolean foil;
    }

    // Simple DTO representing a graph edge
    private static class Edge {
        String subj;
        String rel;
        String obj;
        Edge(String subj, String rel, String obj) {
            this.subj = subj;
            this.rel = rel;
            this.obj = obj;
        }
    }

}
