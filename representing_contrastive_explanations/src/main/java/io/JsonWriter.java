package io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import utils.CommonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static constants.EnvConstants.*;
import static constants.PathConstants.*;

public class JsonWriter {

    static class UserInput {
        String class_expression;
        List<String> facts;
        List<String> foils;

        UserInput(String class_expression, List<String> facts, List<String> foils) {
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
        List<UserInput> experiments;

        InputConfig(String ontology_input_file_path, List<UserInput> experiments) throws IOException {
            this.ontology_input_file_path = ontology_input_file_path;
            this.output_file_path = CommonUtil.createDirectory(OUTPUT_DIR).resolve(REASONER_OUTPUT_JSON).toString();
            this.reasoner = HERMIT_REASONER;
            this.output_format = JSON_OUTPUT_FORMAT;
            this.experiments = experiments;
        }
    }

    public static void createInputJsonFile(String fact, String foil, String query) throws IOException {
        UserInput exp = new UserInput(
                query,
                Collections.singletonList(fact),
                Collections.singletonList(foil)
        );
        InputConfig config = new InputConfig(
                CommonUtil.createDirectory(INPUT_DIR).resolve(INPUT_OWL_FILE).toString(),
                Collections.singletonList(exp)
        );
        // Serialize to JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(config);
        Path jsonPath = CommonUtil.createDirectory(INPUT_DIR).resolve(REASONER_INPUT_JSON);
        Files.writeString(jsonPath, jsonString, StandardCharsets.UTF_8);
    }

}

