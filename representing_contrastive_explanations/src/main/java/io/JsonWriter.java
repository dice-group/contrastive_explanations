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

/**
 * Utility to create the JSON input file from the protege UI, given as input to the reasoner component.
 *
 * <p>This class builds a minimal configuration JSON containing the ontology path,
 * output destination, reasoner selection, output format, and a list of experiments
 * (each with a class expression, facts and foils).</p>
 */
public class JsonWriter {
    /**
     * Represents a single experiment entry in the generated input JSON.
     */
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

    /**
     * Top-level configuration object serialized to JSON.
     *
     * <p>Fields:
     * - ontology_input_file_path: path to the input OWL file
     * - output_file_path: base path where reasoner outputs will be written
     * - reasoner: identifier of the selected reasoner
     * - output_format: serialization format for outputs
     * - experiments: list of UserInput entries</p>
     */
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

    /**
     * Create the JSON input file used by the reasoner.
     *
     * @param fact a fact string entered by the user in the UI
     * @param foil a foil string entered by the user in the UI
     * @param query the class expression / query string entered by the user in the UI
     * @throws IOException if writing the JSON file fails
     */
    public static String createInputJsonFile(String fact, String foil, String query) throws IOException {
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

        // Ensure the inputs directory exists and write the JSON string to reasoner_input.json
        Path jsonPath = CommonUtil.createDirectory(INPUT_DIR).resolve(REASONER_INPUT_JSON);
        Files.writeString(jsonPath, jsonString, StandardCharsets.UTF_8);
        return jsonPath.toString();
    }

}

