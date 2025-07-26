/*
//package anonymized.contrastive.experiments;
//
//import anonymized.contrastive.ContrastiveExplanation;
//import anonymized.contrastive.ContrastiveExplanationGenerator;
//import anonymized.contrastive.ContrastiveExplanationProblem;
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.LoggerContext;
//import com.clarkparsia.owlapi.explanation.MyBlackBoxExplanation;
//import org.semanticweb.HermiT.ReasonerFactory;
//import org.semanticweb.elk.owlapi.ElkReasonerFactory;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
//import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
//
//import org.semanticweb.owlapi.model.*;
//import org.semanticweb.owlapi.model.parameters.Imports;
//import org.semanticweb.owlapi.reasoner.InferenceType;
//import org.semanticweb.owlapi.reasoner.OWLReasoner;
//import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
//import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
//import org.semanticweb.owlapi.util.DefaultPrefixManager;
//import org.semanticweb.owlapi.util.SimpleShortFormProvider;
//
//import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ManualFactFoilExperimenter {
//
//    public static int MAX_ONT_SIZE = 10000;
//
//    public enum ReasonerChoice { ELK, HERMIT }
//
//    public static ReasonerChoice reasoner = ReasonerChoice.ELK;
//
//    public static void main(String[] args) throws Exception {
//        if (args.length < 1) {
//            System.out.println("Usage: java ManualFactFoilExperimenter input.txt");
//            System.exit(1);
//        }
//
//        Map<String, String> config = parseInputFile(args[0]);
//
//        String ontologyPath = config.get("Ontology file path");
//        int maxIterations = Integer.parseInt(config.get("MaxIterations"));
//        String namespace = config.get("Name space");
//
//        List<String> factNames = Arrays.stream(config.getOrDefault("facts", "").split(","))
//                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
//
//        List<String> foilNames = Arrays.stream(config.getOrDefault("foils", "").split(","))
//                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
//
//        if (config.containsKey("Reasoner") && config.get("Reasoner").equalsIgnoreCase("HERMIT")) {
//            reasoner = ReasonerChoice.HERMIT;
//        }
//
//        boolean conflictMinimal = config.containsKey("conflict-minimal") &&
//                config.get("conflict-minimal").equalsIgnoreCase("true");
//
//        String classExprString = config.getOrDefault("class-expression", "").trim();
//
//        // Setup logging to OFF
//        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//        loggerContext.getLogger("org.semanticweb.owlapi").setLevel(Level.OFF);
//        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
//        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
//        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);
//
//        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
//
//        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//        OWLDataFactory factory = manager.getOWLDataFactory();
//
//        System.out.println("Parsing ontology...");
//        OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
//
//        if (ont.getAxiomCount() > MAX_ONT_SIZE) {
//            System.out.println("Ontology has more than " + MAX_ONT_SIZE + " axioms!");
//            System.exit(1);
//        }
//
//        OWLReasonerFactory reasonerFactory = (reasoner == ReasonerChoice.HERMIT)
//                ? new ReasonerFactory()
//                : new ElkReasonerFactory();
//
//        OWLReasoner r = reasonerFactory.createReasoner(ont);
//        r.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
//
//        Set<OWLNamedIndividual> allIndividuals = ont.individualsInSignature().collect(Collectors.toSet());
//
//        OWLClassExpression classExpr = null;
//        if (!classExprString.isEmpty()) {
//            // Parse class expression from string using Manchester parser
//            DefaultPrefixManager pm = new DefaultPrefixManager(null, null, namespace);
//            ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
//            parser.setStringToParse(classExprString);
//            parser.setDefaultOntology(ont);
//            parser.setOWLEntityChecker(new ShortFormEntityChecker(
//                    new BidirectionalShortFormProviderAdapter(
//                            manager,
//                            Collections.singleton(ont),
//                            new SimpleShortFormProvider()
//                    )
//            ));
//            classExpr = parser.parseClassExpression();
//            System.out.println("Parsed class expression: " + renderer.render(classExpr));
//        } else {
//            System.out.println("No class expression provided, will try to infer class from facts.");
//        }
//
//        System.out.println("STATS: common-size difference-size conflict-size num-fresh-individuals computation-time");
//
//        int iteration = 0;
//        outer:
//        for (String factName : factNames) {
//            OWLNamedIndividual fact = factory.getOWLNamedIndividual(IRI.create(namespace + factName));
//
//            OWLClassExpression factClassExpr = classExpr;
//
//            // If no class expression from input, infer class of fact
//            if (factClassExpr == null) {
//                OWLClass clazz = null;
//                for (OWLClass candidate : ont.classesInSignature().collect(Collectors.toSet())) {
//                    if (r.getInstances(candidate).getFlattened().contains(fact)) {
//                        clazz = candidate;
//                        break;
//                    }
//                }
//                if (clazz == null) {
//                    System.out.println("Could not determine class for fact individual: " + fact);
//                    continue;
//                }
//                factClassExpr = clazz;
//            }
//
//            for (String foilName : foilNames) {
//                if (iteration++ >= maxIterations) break outer;
//
//                OWLNamedIndividual foil = factory.getOWLNamedIndividual(IRI.create(namespace + foilName));
//
//                System.out.println("Using class expression: " + renderer.render(factClassExpr));
//                ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ont, factClassExpr, fact, foil);
//                System.out.println("CEP: " + cep.toString(renderer));
//
//                ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);
//                gen.useConflictMinimality(conflictMinimal);
//
//                long startTime = System.currentTimeMillis();
//                ContrastiveExplanation ce = gen.computeExplanation(cep);
//                System.out.println("CE: " + ce.toString(renderer));
//                long duration = System.currentTimeMillis() - startTime;
//
//                int commonSize = ce.getCommon().size();
//                int differenceSize = ce.getDifferent().size();
//                int conflictSize = ce.getConflict().size();
//                long freshIndividuals = ce.getFoilMapping()
//                        .values()
//                        .stream()
//                        .filter(x -> !allIndividuals.contains(x))
//                        .count();
//
//                System.out.println("STATS: " + commonSize + " " + differenceSize + " " + conflictSize + " " + freshIndividuals + " " + duration);
//            }
//        }
//    }
//
//    private static Map<String, String> parseInputFile(String path) throws Exception {
//        Map<String, String> config = new HashMap<>();
//        List<String> lines = Files.readAllLines(new File(path).toPath());
//        for (String line : lines) {
//            line = line.trim();
//            if (line.isEmpty() || line.startsWith("#")) continue;
//            String[] parts = line.split("=", 2);
//            if (parts.length == 2) {
//                config.put(parts[0].trim(), parts[1].trim());
//            }
//        }
//        return config;
//    }
//}

package anonymized.contrastive.experiments;

import anonymized.contrastive.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class ManualFactFoilExperimenter {

    public static int MAX_ONT_SIZE = 10000;

    public enum ReasonerChoice {ELK, HERMIT}

    public static ReasonerChoice reasoner = ReasonerChoice.ELK;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java ManualFactFoilExperimenter input.txt");
            System.exit(1);
        }

        Map<String, String> config = parseInputFile(args[0]);

        String ontologyPath = config.get("Ontology file path");
        int maxIterations = Integer.parseInt(config.get("MaxIterations"));
        String namespace = config.get("Name space");
        String classExprString = config.getOrDefault("class-expression", "").trim();

        List<String> factNames = Arrays.stream(config.getOrDefault("facts", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        List<String> foilNames = Arrays.stream(config.getOrDefault("foils", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        if (config.containsKey("Reasoner") && config.get("Reasoner").equalsIgnoreCase("HERMIT")) {
            reasoner = ReasonerChoice.HERMIT;
        }

        boolean conflictMinimal = config.containsKey("conflict-minimal") &&
                config.get("conflict-minimal").equalsIgnoreCase("true");

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("org.semanticweb.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        System.out.println("Parsing ontology...");
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));

        if (ontology.getAxiomCount() > MAX_ONT_SIZE) {
            System.out.println("Ontology has more than " + MAX_ONT_SIZE + " axioms!");
            System.exit(1);
        }

        OWLReasonerFactory reasonerFactory = (reasoner == ReasonerChoice.HERMIT)
                ? new ReasonerFactory()
                : new ElkReasonerFactory();

        OWLReasoner r = reasonerFactory.createReasoner(ontology);
        r.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        Set<OWLNamedIndividual> allIndividuals = ontology.individualsInSignature().collect(Collectors.toSet());

        OWLClassExpression classExpr = null;
        if (!classExprString.isEmpty()) {
            classExpr = ManchesterSyntaxParser(dataFactory, ontology, classExprString);
            System.out.println("Parsed class expression: " + renderer.render(classExpr));
        } else {
            System.out.println("No class expression provided, will try to infer class from facts.");
        }

        System.out.println("STATS: common-size difference-size conflict-size num-fresh-individuals computation-time");

        int iteration = 0;
        outer:
        for (String factName : factNames) {
            OWLNamedIndividual fact = dataFactory.getOWLNamedIndividual(IRI.create(namespace + factName));

            OWLClassExpression factClassExpr = classExpr;
            if (factClassExpr == null) {
                for (OWLClass candidate : ontology.classesInSignature().collect(Collectors.toSet())) {
                    if (r.getInstances(candidate).getFlattened().contains(fact)) {
                        factClassExpr = candidate;
                        break;
                    }
                }
                if (factClassExpr == null) {
                    System.out.println("Could not determine class for fact individual: " + fact);
                    continue;
                }
            }

            for (String foilName : foilNames) {
                if (iteration++ >= maxIterations) break outer;
                OWLNamedIndividual foil = dataFactory.getOWLNamedIndividual(IRI.create(namespace + foilName));

                System.out.println("Using class expression: " + renderer.render(factClassExpr));
                ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ontology, factClassExpr, fact, foil);
                System.out.println("CEP: " + cep.toString(renderer));
                System.out.println("CEP: " + cep);

                ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);
                gen.useConflictMinimality(conflictMinimal);

                long startTime = System.currentTimeMillis();
                ContrastiveExplanation ce = gen.computeExplanation(cep);
                System.out.println("CE: " + ce.toString(renderer));
                long duration = System.currentTimeMillis() - startTime;

                int commonSize = ce.getCommon().size();
                int differenceSize = ce.getDifferent().size();
                int conflictSize = ce.getConflict().size();
                long freshIndividuals = ce.getFoilMapping().values().stream()
                        .filter(x -> !allIndividuals.contains(x)).count();

                System.out.println("STATS: " + commonSize + " " + differenceSize + " " + conflictSize + " " + freshIndividuals + " " + duration);
            }
        }
    }


    private static OWLClassExpression ManchesterSyntaxParser(OWLDataFactory dataFactory, OWLOntology ontology, String classExprString) {

        // === Setup Short Form Provider and Bidirectional Provider for resolving names ===
        BidirectionalShortFormProvider shortFormProvider =
                new BidirectionalShortFormProviderAdapter(Collections.singleton(ontology), new SimpleShortFormProvider());

        // === Create OWLEntityChecker using short form provider ===
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(shortFormProvider);

        // === Create the Manchester Syntax Class Expression Parser with data factory and entity checker ===
        ManchesterOWLSyntaxClassExpressionParser parser =
                new ManchesterOWLSyntaxClassExpressionParser(dataFactory, entityChecker);

        // === Parse the expression ===
        OWLClassExpression classExpression = parser.parse(classExprString);
        return classExpression;

    }
    private static Map<String, String> parseInputFile(String path) throws Exception {
        Map<String, String> config = new HashMap<>();
        List<String> lines = Files.readAllLines(new File(path).toPath());
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                config.put(parts[0].trim(), parts[1].trim());
            }
        }
        return config;

    }
}

*/

package anonymized.contrastive.experiments;

import anonymized.contrastive.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class ManualFactFoilExperimenter {

    public static int MAX_ONT_SIZE = 10000;

    public enum ReasonerChoice { ELK, HERMIT }

    public static ReasonerChoice reasoner = ReasonerChoice.ELK;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java ManualFactFoilExperimenter input.txt");
            System.exit(1);
        }

        Map<String, String> config = parseInputFile(args[0]);

        String ontologyPath = config.get("Ontology file path");
        int maxIterations = Integer.parseInt(config.get("MaxIterations"));
        String classExprString = config.getOrDefault("class-expression", "").trim();

        List<String> factNames = Arrays.stream(config.getOrDefault("facts", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        List<String> foilNames = Arrays.stream(config.getOrDefault("foils", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        if (config.containsKey("Reasoner") && config.get("Reasoner").equalsIgnoreCase("HERMIT")) {
            reasoner = ReasonerChoice.HERMIT;
        }

        boolean conflictMinimal = config.containsKey("conflict-minimal") &&
                config.get("conflict-minimal").equalsIgnoreCase("true");

        // Turn off OWL/ELK logging
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("org.semanticweb.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        System.out.println("Parsing ontology...");
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));

        if (ontology.getAxiomCount() > MAX_ONT_SIZE) {
            System.out.println("Ontology has more than " + MAX_ONT_SIZE + " axioms!");
            System.exit(1);
        }

        // Automatically extract namespace
        String namespace = getOntologyBaseIRI(ontology);
        System.out.println("Detected namespace: " + namespace);

        OWLReasonerFactory reasonerFactory = (reasoner == ReasonerChoice.HERMIT)
                ? new ReasonerFactory()
                : new ElkReasonerFactory();

        OWLReasoner r = reasonerFactory.createReasoner(ontology);
        r.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        Set<OWLNamedIndividual> allIndividuals = ontology.individualsInSignature().collect(Collectors.toSet());

        OWLClassExpression classExpr = null;
        if (!classExprString.isEmpty()) {
            classExpr = parseManchesterSyntax(dataFactory, ontology, classExprString);
            System.out.println("Parsed class expression: " + renderer.render(classExpr));
        } else {
            System.out.println("No class expression provided, will try to infer class from facts.");
        }

        System.out.println("STATS: common-size difference-size conflict-size num-fresh-individuals computation-time");

        int iteration = 0;
        outer:
        for (String factName : factNames) {
            OWLNamedIndividual fact = dataFactory.getOWLNamedIndividual(IRI.create(namespace + factName));
            OWLClassExpression factClassExpr = classExpr;

            if (factClassExpr == null) {
                for (OWLClass candidate : ontology.classesInSignature().collect(Collectors.toSet())) {
                    if (r.getInstances(candidate).getFlattened().contains(fact)) {
                        factClassExpr = candidate;
                        break;
                    }
                }
                if (factClassExpr == null) {
                    System.out.println("Could not determine class for fact individual: " + fact);
                    continue;
                }
            }

            for (String foilName : foilNames) {
                if (iteration++ >= maxIterations) break outer;

                OWLNamedIndividual foil = dataFactory.getOWLNamedIndividual(IRI.create(namespace + foilName));

                System.out.println("Using Menchester expression: " + renderer.render(factClassExpr));
                System.out.println("Using class expression: " + factClassExpr);
                ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ontology, factClassExpr, fact, foil);
                System.out.println("CEP: " + cep.toString(renderer));

                ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);
                gen.useConflictMinimality(conflictMinimal);

                long startTime = System.currentTimeMillis();
                ContrastiveExplanation ce = gen.computeExplanation(cep);
                System.out.println("CE: " + ce.toString(renderer));
                long duration = System.currentTimeMillis() - startTime;

                int commonSize = ce.getCommon().size();
                int differenceSize = ce.getDifferent().size();
                int conflictSize = ce.getConflict().size();
                long freshIndividuals = ce.getFoilMapping().values().stream()
                        .filter(x -> !allIndividuals.contains(x)).count();

                System.out.println("STATS: " + commonSize + " " + differenceSize + " " + conflictSize + " " + freshIndividuals + " " + duration);
            }
        }
    }

    private static OWLClassExpression parseManchesterSyntax(OWLDataFactory dataFactory, OWLOntology ontology, String classExprString) {
        BidirectionalShortFormProvider shortFormProvider =
                new BidirectionalShortFormProviderAdapter(Collections.singleton(ontology), new SimpleShortFormProvider());
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(shortFormProvider);
        ManchesterOWLSyntaxClassExpressionParser parser =
                new ManchesterOWLSyntaxClassExpressionParser(dataFactory, entityChecker);
        return parser.parse(classExprString);
    }

    private static Map<String, String> parseInputFile(String path) throws Exception {
        Map<String, String> config = new HashMap<>();
        List<String> lines = Files.readAllLines(new File(path).toPath());
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                config.put(parts[0].trim(), parts[1].trim());
            }
        }
        return config;
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
}
