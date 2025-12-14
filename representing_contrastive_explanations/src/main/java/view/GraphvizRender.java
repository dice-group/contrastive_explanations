package view;

import guru.nidi.graphviz.engine.*;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;


public class GraphvizRender {
    public static Path toPng(String dot) throws Throwable {
        Path outputFile = JsonCreator.PluginFiles.outputsDir().resolve("graphs.png");
        try (Writer w = Files.newBufferedWriter(outputFile.resolveSibling("graph.dot"))) {
            w.write(dot);
        }
        try {
            Graphviz.fromString(dot)
                    .render(Format.PNG)
                    .toFile(outputFile.toFile());
        } catch (Exception e) {
            throw e.getCause();
        }
        return outputFile;
    }
}


