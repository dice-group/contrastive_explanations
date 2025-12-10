package view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

        InputConfig(String ontology_input_file_path, List<Experiment> experiments) {
            this.ontology_input_file_path = ontology_input_file_path;
            this.output_file_path = "/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/outputs/reasoner/family_output";
            this.reasoner = "HERMIT";
            this.output_format = "json";
            this.experiments = experiments;
        }
    }

    public static void main(String[] args) throws IOException {
        ProcessRunner.runGraphviz();
    }

    public static void createInputJsonFile(File owlFile, String fact, String foil, String query) throws IOException {
        Experiment exp = new Experiment(
                query,
                Collections.singletonList(fact),
                Collections.singletonList(foil)
        );
        InputConfig config = new InputConfig(
                owlFile.getPath(),
                Collections.singletonList(exp)
        );
        // Serialize to JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(config);
        try (FileWriter writer = new FileWriter("/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/outputs/reasoner/family_json_input.json")) {
            writer.write(jsonString);
        } catch (IOException e) {
            throw e;
        }
    }
}

