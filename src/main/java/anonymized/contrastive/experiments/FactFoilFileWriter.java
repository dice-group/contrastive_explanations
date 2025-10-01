//package anonymized.contrastive.experiments;
//
//import anonymized.contrastive.*;
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.LoggerContext;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.semanticweb.HermiT.ReasonerFactory;
//import org.semanticweb.elk.owlapi.ElkReasonerFactory;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.expression.OWLEntityChecker;
//import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
//import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
//import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
//import org.semanticweb.owlapi.model.*;
//import org.semanticweb.owlapi.reasoner.*;
//import org.semanticweb.owlapi.util.*;
//
//import java.io.File;
//import java.io.PrintWriter;
//import java.util.*;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
//public class FactFoilFileWriter {
//
//    public enum ReasonerChoice { ELK, HERMIT }
//
//    public static void main(String[] args) throws Exception {
//        if (args.length < 1) {
//            System.err.println("Usage: java FactFoilFileWriter input.json");
//            System.exit(1);
//        }
//
//        String inputJson = args[0];
//        String outputFile = "E:\\Workspace_Dice\\DataSource\\family_output.txt";
//
//        try (PrintWriter writer = new PrintWriter(new File(outputFile))) {
//            Consumer<String> out = writer::println;
//
//            // Load JSON config
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode root = mapper.readTree(new File(inputJson));
//            String ontologyPath = root.get("ontology_file_path").asText();
//
//            // Reasoner selection
//            ReasonerChoice reasoner = ReasonerChoice.ELK;
//            if (root.has("reasoner")) {
//                String reasonerStr = root.get("reasoner").asText().trim().toUpperCase();
//                switch (reasonerStr) {
//                    case "HERMIT" -> reasoner = ReasonerChoice.HERMIT;
//                    case "ELK" -> reasoner = ReasonerChoice.ELK;
//                    default -> out.accept("Warning: Unsupported reasoner '" + reasonerStr + "'. Defaulting to ELK.");
//                }
//            }
//
//            // Disable verbose logging
//            LoggerContext loggerContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
//            loggerContext.getLogger("org.semanticweb.owlapi").setLevel(Level.OFF);
//            loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
//            loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
//            loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);
//
//            ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
//            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//            OWLDataFactory dataFactory = manager.getOWLDataFactory();
//
//            out.accept("Parsing ontology...");
//            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
//            out.accept("Detected namespace: " + getOntologyBaseIRI(ontology));
//
//            OWLReasonerFactory reasonerFactory = switch (reasoner) {
//                case HERMIT -> new ReasonerFactory();
//                case ELK -> new ElkReasonerFactory();
//            };
//
//            OWLReasoner r = reasonerFactory.createReasoner(ontology);
//            r.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
//            Set<OWLNamedIndividual> allIndividuals = ontology.individualsInSignature().collect(Collectors.toSet());
//
//            JsonNode experiments = root.get("experiments");
//            if (experiments == null || !experiments.isArray()) {
//                out.accept("Error: 'experiments' must be a list in JSON.");
//                System.exit(1);
//            }
//
//            for (JsonNode exp : experiments) {
//                String classExprStr = exp.get("class_expression").asText();
//                OWLClassExpression classExpr = parseManchesterSyntax(dataFactory, ontology, classExprStr);
//                out.accept("\nParsed class expression: " + renderer.render(classExpr));
//
//                List<String> facts = new ArrayList<>();
//                exp.get("facts").forEach(f -> facts.add(f.asText().trim()));
//                List<String> foils = new ArrayList<>();
//                exp.get("foils").forEach(f -> foils.add(f.asText().trim()));
//
//                for (String factName : facts) {
//                    OWLNamedIndividual fact = dataFactory.getOWLNamedIndividual(
//                            IRI.create(getOntologyBaseIRI(ontology) + factName));
//
//                    for (String foilName : foils) {
//                        OWLNamedIndividual foil = dataFactory.getOWLNamedIndividual(
//                                IRI.create(getOntologyBaseIRI(ontology) + foilName));
//
//                        out.accept("\nFact: " + factName + " | Foil: " + foilName);
//
//                        ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ontology, classExpr, fact, foil);
//                        ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);
//
//                        long startTime = System.currentTimeMillis();
//                        ContrastiveExplanation ce = gen.computeExplanation(cep);
//                        long duration = System.currentTimeMillis() - startTime;
//
//                        // Print structured output
//                        printStructuredCE(writer, ce, duration);
//                        // Add separator for clarity
//                        writer.println("====================================================\n");
//                    }
//                }
//            }
//
//            out.accept("\nFinished all experiments. Output saved to " + outputFile);
//        }
//    }
//
//    // ==================== Helper Methods ====================
//
//    private static OWLClassExpression parseManchesterSyntax(OWLDataFactory dataFactory, OWLOntology ontology, String classExprString) {
//        BidirectionalShortFormProvider shortFormProvider =
//                new BidirectionalShortFormProviderAdapter(Collections.singleton(ontology), new SimpleShortFormProvider());
//        OWLEntityChecker entityChecker = new ShortFormEntityChecker(shortFormProvider);
//        ManchesterOWLSyntaxClassExpressionParser parser =
//                new ManchesterOWLSyntaxClassExpressionParser(dataFactory, entityChecker);
//        return parser.parse(classExprString);
//    }
//
//    private static String getOntologyBaseIRI(OWLOntology ontology) {
//        return ontology.axioms(AxiomType.DECLARATION)
//                .map(ax -> ax.getSignature().stream().findFirst())
//                .filter(Optional::isPresent)
//                .map(opt -> opt.get().getIRI().toString())
//                .map(iriStr -> {
//                    int lastHash = iriStr.lastIndexOf('#');
//                    int lastSlash = iriStr.lastIndexOf('/');
//                    int splitPoint = Math.max(lastHash, lastSlash);
//                    return (splitPoint != -1) ? iriStr.substring(0, splitPoint + 1) : iriStr;
//                })
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Could not infer ontology base IRI"));
//    }
//
//    private static void printStructuredCE(PrintWriter out, ContrastiveExplanation ce, long duration) {
//        // Substitute placeholders with actual individual names
//        List<String> commonResolved = substituteMappings(ce.getCommon(), ce.getFactMapping());
//        List<String> differentResolved = substituteMappings(ce.getDifferent(), ce.getFactMapping());
//
//        out.println("COMMON:");
//        commonResolved.forEach(s -> out.println(" - " + s));
//
//        out.println("DIFFERENT:");
//        differentResolved.forEach(s -> out.println(" - " + s));
//
//        out.println("FACT MAPPING:");
//        ce.getFactMapping().forEach((k, v) -> out.println(" " + k + " -> " + v.getIRI().getShortForm()));
//
//        out.println("FOIL MAPPING:");
//        ce.getFoilMapping().forEach((k, v) -> out.println(" " + k + " -> " + v.getIRI().getShortForm()));
//
//        if (!ce.getConflict().isEmpty()) {
//            out.println("CONFLICTS:");
//            ce.getConflict().forEach(s -> out.println(" - " + s));
//        }
//
//        out.println("STATS:");
//        out.println(" Common facts: " + ce.getCommon().size());
//        out.println(" Differences: " + ce.getDifferent().size());
//        out.println(" Conflicts: " + ce.getConflict().size());
//        out.println(" Runtime (ms): " + duration);
//        out.println();
//    }
//
//    private static List<String> substituteMappings(Set<OWLAxiom> axioms, Map<OWLNamedIndividual, OWLNamedIndividual> mapping) {
//        List<String> result = new ArrayList<>();
//        for (OWLAxiom ax : axioms) {
//            String axStr = ax.toString(); // Or use Manchester renderer if you prefer
//            for (Map.Entry<OWLNamedIndividual, OWLNamedIndividual> e : mapping.entrySet()) {
//                // Replace fact individual names with their short forms
//                axStr = axStr.replace(e.getKey().getIRI().getShortForm(), e.getValue().getIRI().getShortForm());
//            }
//            result.add(axStr);
//        }
//        return result;
//    }
//
//}
//


package anonymized.contrastive.experiments;

import anonymized.contrastive.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class FactFoilFileWriter {

    public enum ReasonerChoice { ELK, HERMIT }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java FactFoilFileWriter input.json");
            System.exit(1);
        }

        String inputJson = args[0];
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(inputJson));

        String ontologyPath = root.get("ontology_input_file_path").asText();
        String outputBasePath = root.has("output_file_path") ? root.get("output_file_path").asText() :
                "E:/Workspace_Dice/DataSource/family_output";
        String outputFormat = root.has("output_format") ? root.get("output_format").asText().trim().toLowerCase() : "text";

        // Append proper extension
        String outputFile = outputBasePath + (outputFormat.equals("json") ? ".json" : ".txt");

        // Disable verbose logging
        LoggerContext loggerContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("org.semanticweb.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
        String namespace = getOntologyBaseIRI(ontology);

        // Reasoner selection
        ReasonerChoice reasoner = ReasonerChoice.ELK;
        if (root.has("reasoner")) {
            String reasonerStr = root.get("reasoner").asText().trim().toUpperCase();
            switch (reasonerStr) {
                case "HERMIT" -> reasoner = ReasonerChoice.HERMIT;
                case "ELK" -> reasoner = ReasonerChoice.ELK;
            }
        }

        OWLReasonerFactory reasonerFactory = switch (reasoner) {
            case HERMIT -> new ReasonerFactory();
            case ELK -> new ElkReasonerFactory();
        };

        OWLReasoner r = reasonerFactory.createReasoner(ontology);
        r.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        JsonNode experiments = root.get("experiments");
        if (experiments == null || !experiments.isArray()) {
            System.err.println("Error: 'experiments' must be a list in JSON.");
            System.exit(1);
        }

        ArrayNode jsonOutput = mapper.createArrayNode();

        try (PrintWriter writer = new PrintWriter(new File(outputFile))) {
            for (JsonNode exp : experiments) {
                String classExprStr = exp.get("class_expression").asText();
                OWLClassExpression classExpr = parseManchesterSyntax(dataFactory, ontology, classExprStr);

                List<String> facts = new ArrayList<>();
                exp.get("facts").forEach(f -> facts.add(f.asText().trim()));
                List<String> foils = new ArrayList<>();
                exp.get("foils").forEach(f -> foils.add(f.asText().trim()));

                if (outputFormat.equals("json")) {
                    ObjectNode classExprNode = mapper.createObjectNode();
                    classExprNode.put("namespace", namespace);
                    classExprNode.put("class_expression", classExprStr);
                    ArrayNode resultsArray = mapper.createArrayNode();

                    for (String factName : facts) {
                        OWLNamedIndividual fact = dataFactory.getOWLNamedIndividual(IRI.create(namespace + factName));
                        for (String foilName : foils) {
                            OWLNamedIndividual foil = dataFactory.getOWLNamedIndividual(IRI.create(namespace + foilName));

                            ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ontology, classExpr, fact, foil);
                            ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);

                            long startTime = System.currentTimeMillis();
                            ContrastiveExplanation ce = gen.computeExplanation(cep);
                            long duration = System.currentTimeMillis() - startTime;

                            ObjectNode ceNode = mapper.createObjectNode();
                            ceNode.put("fact", factName);
                            ceNode.put("foil", foilName);

                            ceNode.putPOJO("common", substituteMappings(ce.getCommon(), ce.getFactMapping()));
                            ceNode.putPOJO("different", substituteMappings(ce.getDifferent(), ce.getFactMapping()));

                            Map<String, String> factMap = ce.getFactMapping().entrySet().stream()
                                    .collect(Collectors.toMap(e -> e.getKey().getIRI().getShortForm(),
                                            e -> e.getValue().getIRI().getShortForm()));
                            Map<String, String> foilMap = ce.getFoilMapping().entrySet().stream()
                                    .collect(Collectors.toMap(e -> e.getKey().getIRI().getShortForm(),
                                            e -> e.getValue().getIRI().getShortForm()));

                            ceNode.putPOJO("fact_mapping", factMap);
                            ceNode.putPOJO("foil_mapping", foilMap);
                            ceNode.putPOJO("conflicts", ce.getConflict());

                            ObjectNode stats = mapper.createObjectNode();
                            stats.put("common", ce.getCommon().size());
                            stats.put("different", ce.getDifferent().size());
                            stats.put("conflicts", ce.getConflict().size());
                            stats.put("runtime_ms", duration);
                            ceNode.set("stats", stats);

                            resultsArray.add(ceNode);
                        }
                    }
                    classExprNode.set("results", resultsArray);
                    jsonOutput.add(classExprNode);

                } else {
                    for (String factName : facts) {
                        OWLNamedIndividual fact = dataFactory.getOWLNamedIndividual(IRI.create(namespace + factName));
                        for (String foilName : foils) {
                            OWLNamedIndividual foil = dataFactory.getOWLNamedIndividual(IRI.create(namespace + foilName));

                            ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ontology, classExpr, fact, foil);
                            ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);

                            long startTime = System.currentTimeMillis();
                            ContrastiveExplanation ce = gen.computeExplanation(cep);
                            long duration = System.currentTimeMillis() - startTime;

                            printStructuredCE(writer, namespace, classExprStr, factName, foilName, ce, duration);
                        }
                    }
                }
            }

            if (outputFormat.equals("json")) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFile), jsonOutput);
            }
        }

        System.out.println("Finished all experiments. Output saved to " + outputFile);
    }

    // ==================== Helper Methods ====================

    private static OWLClassExpression parseManchesterSyntax(OWLDataFactory dataFactory, OWLOntology ontology, String classExprString) {
        BidirectionalShortFormProvider shortFormProvider =
                new BidirectionalShortFormProviderAdapter(Collections.singleton(ontology), new SimpleShortFormProvider());
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(shortFormProvider);
        ManchesterOWLSyntaxClassExpressionParser parser =
                new ManchesterOWLSyntaxClassExpressionParser(dataFactory, entityChecker);
        return parser.parse(classExprString);
    }

    private static String getOntologyBaseIRI(OWLOntology ontology) {
        return ontology.axioms(AxiomType.DECLARATION)
                .map(ax -> ax.getSignature().stream().findFirst())
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getIRI().toString())
                .map(iriStr -> {
                    int lastHash = iriStr.lastIndexOf('#');
                    int lastSlash = iriStr.lastIndexOf('/');
                    int splitPoint = Math.max(lastHash, lastSlash);
                    return (splitPoint != -1) ? iriStr.substring(0, splitPoint + 1) : iriStr;
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not infer ontology base IRI"));
    }

    private static void printStructuredCE(PrintWriter out, String namespace, String classExpression, String factName, String foilName, ContrastiveExplanation ce, long duration) {
        out.println("NAMESPACE: " + namespace);
        out.println("CLASS EXPRESSION: " + classExpression);
        out.println("FACT: " + factName);
        out.println("FOIL: " + foilName);
        out.println();

        List<String> commonResolved = substituteMappings(ce.getCommon(), ce.getFactMapping());
        List<String> differentResolved = substituteMappings(ce.getDifferent(), ce.getFactMapping());

        out.println("COMMON:");
        commonResolved.forEach(s -> out.println(" - " + s));

        out.println("DIFFERENT:");
        differentResolved.forEach(s -> out.println(" - " + s));

        out.println("FACT MAPPING:");
        ce.getFactMapping().forEach((k, v) -> out.println(" " + k + " -> " + v.getIRI().getShortForm()));

        out.println("FOIL MAPPING:");
        ce.getFoilMapping().forEach((k, v) -> out.println(" " + k + " -> " + v.getIRI().getShortForm()));

        if (!ce.getConflict().isEmpty()) {
            out.println("CONFLICTS:");
            ce.getConflict().forEach(s -> out.println(" - " + s));
        }

        out.println("STATS:");
        out.println(" Common facts: " + ce.getCommon().size());
        out.println(" Differences: " + ce.getDifferent().size());
        out.println(" Conflicts: " + ce.getConflict().size());
        out.println(" Runtime (ms): " + duration);
        out.println("\n====================================================\n");
    }

    private static List<String> substituteMappings(Set<OWLAxiom> axioms, Map<OWLNamedIndividual, OWLNamedIndividual> mapping) {
        List<String> result = new ArrayList<>();
        for (OWLAxiom ax : axioms) {
            String axStr = ax.toString();
            for (Map.Entry<OWLNamedIndividual, OWLNamedIndividual> e : mapping.entrySet()) {
                axStr = axStr.replace(e.getKey().getIRI().getShortForm(), e.getValue().getIRI().getShortForm());
            }
            result.add(axStr);
        }
        return result;
    }
}
