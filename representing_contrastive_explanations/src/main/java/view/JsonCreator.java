package view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class JsonCreator {

    static class Experiment {
        String class_expression;
        List<String> facts;
        List<String> foils;

        Experiment(String class_expression, List<String> facts, List<String> foils) {
            this.class_expression = class_expression;
            this.facts = facts;
            this.foils = foils;
        }
    }

    static class InputConfig {
        String ontology_input_file_path;
        String output_file_path;
        String reasoner;
        String output_format;
        List<Experiment> experiments;

        InputConfig(String ontology_input_file_path, List<Experiment> experiments) throws IOException {
            this.ontology_input_file_path = ontology_input_file_path;
            this.output_file_path = PluginFiles.outputsDir().resolve("family_output").toString();
            this.reasoner = "HERMIT";
            this.output_format = "json";
            this.experiments = experiments;
        }
    }

    public static void createInputJsonFile(String fact, String foil, String query) throws IOException {
        Experiment exp = new Experiment(
                query,
                Collections.singletonList(fact),
                Collections.singletonList(foil)
        );
        InputConfig config = new InputConfig(
                PluginFiles.inputsDir().resolve("family.owl").toString(),
                Collections.singletonList(exp)
        );
        // Serialize to JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(config);
        Path jsonPath = PluginFiles.inputsDir().resolve("family_json_input.json");
        Files.writeString(jsonPath, jsonString, StandardCharsets.UTF_8);
    }

    public static class PluginFiles {
        private static final String APP_DIR = ".protege/contrastive-explanations";

        public static Path baseDir() throws IOException {
            Path dir = Paths.get(System.getProperty("user.home"), APP_DIR);
            Files.createDirectories(dir);
            return dir;
        }

        public static Path inputsDir() throws IOException {
            Path dir = baseDir().resolve("inputs");
            Files.createDirectories(dir);
            return dir;
        }

        public static Path binsDir() throws IOException {
            Path dir = baseDir().resolve("bin");
            Files.createDirectories(dir);
            return dir;
        }

        public static Path graphvizDir() throws IOException {
            Path dir = baseDir().resolve("graphviz-bundle");
            Files.createDirectories(dir);
            return dir;
        }

        public static Path outputsDir() throws IOException {
            Path dir = baseDir().resolve("outputs");
            Files.createDirectories(dir);
            return dir;
        }
    }

}

