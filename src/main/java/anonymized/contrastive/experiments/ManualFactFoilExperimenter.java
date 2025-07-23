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
        OWLDataFactory factory = manager.getOWLDataFactory();

        System.out.println("Parsing ontology...");
        OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));

        if (ont.getAxiomCount() > MAX_ONT_SIZE) {
            System.out.println("Ontology has more than " + MAX_ONT_SIZE + " axioms!");
            System.exit(1);
        }

        OWLReasonerFactory reasonerFactory = (reasoner == ReasonerChoice.HERMIT)
                ? new ReasonerFactory()
                : new ElkReasonerFactory();

        OWLReasoner r = reasonerFactory.createReasoner(ont);
        r.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        Set<OWLNamedIndividual> allIndividuals = ont.individualsInSignature().collect(Collectors.toSet());

        OWLClassExpression classExpr = null;
        if (!classExprString.isEmpty()) {
            classExpr = parseQueryString(factory, namespace, classExprString);
            System.out.println("Parsed class expression: " + renderer.render(classExpr));
        } else {
            System.out.println("No class expression provided, will try to infer class from facts.");
        }

        System.out.println("STATS: common-size difference-size conflict-size num-fresh-individuals computation-time");

        int iteration = 0;
        outer:
        for (String factName : factNames) {
            OWLNamedIndividual fact = factory.getOWLNamedIndividual(IRI.create(namespace + factName));

            OWLClassExpression factClassExpr = classExpr;
            if (factClassExpr == null) {
                for (OWLClass candidate : ont.classesInSignature().collect(Collectors.toSet())) {
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
                OWLNamedIndividual foil = factory.getOWLNamedIndividual(IRI.create(namespace + foilName));

                System.out.println("Using class expression: " + renderer.render(factClassExpr));
                ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ont, factClassExpr, fact, foil);
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

    private static OWLClassExpression parseQueryString(OWLDataFactory df, String ns, String queryStr) {
        queryStr = queryStr.trim();
        if (queryStr.startsWith("(") && queryStr.endsWith(")")) {
            queryStr = queryStr.substring(1, queryStr.length() - 1).trim();
        }

        List<String> orParts = splitAtTopLevel(queryStr, " or ");
        if (orParts.size() > 1) {
            List<OWLClassExpression> orExpressions = new ArrayList<>();
            for (String part : orParts) {
                orExpressions.add(parseQueryString(df, ns, part));
            }
            return df.getOWLObjectUnionOf(orExpressions);
        }

        List<String> andParts = splitAtTopLevel(queryStr, " and ");
        if (andParts.size() > 1) {
            List<OWLClassExpression> andExpressions = new ArrayList<>();
            for (String part : andParts) {
                andExpressions.add(parseQueryString(df, ns, part));
            }
            return df.getOWLObjectIntersectionOf(andExpressions);
        }

        if (queryStr.startsWith("not ")) {
            return df.getOWLObjectComplementOf(parseQueryString(df, ns, queryStr.substring(4)));
        }

        if (queryStr.contains(" some ")) {
            List<String> parts = splitAtTopLevel(queryStr, " some ");
            return df.getOWLObjectSomeValuesFrom(
                    df.getOWLObjectProperty(IRI.create(ns + parts.get(0).trim())),
                    parseQueryString(df, ns, parts.get(1).trim()));
        }

        if (queryStr.contains(" only ")) {
            List<String> parts = splitAtTopLevel(queryStr, " only ");
            return df.getOWLObjectAllValuesFrom(
                    df.getOWLObjectProperty(IRI.create(ns + parts.get(0).trim())),
                    parseQueryString(df, ns, parts.get(1).trim()));
        }

        if (queryStr.contains(" exactly ")) {
            List<String> parts = splitAtTopLevel(queryStr, " exactly ");
            return df.getOWLObjectExactCardinality(
                    Integer.parseInt(parts.get(1).trim()),
                    df.getOWLObjectProperty(IRI.create(ns + parts.get(0).trim())));
        }

        if (queryStr.contains(" min ")) {
            List<String> parts = splitAtTopLevel(queryStr, " min ");
            return df.getOWLObjectMinCardinality(
                    Integer.parseInt(parts.get(1).trim()),
                    df.getOWLObjectProperty(IRI.create(ns + parts.get(0).trim())));
        }

        if (queryStr.contains(" max ")) {
            List<String> parts = splitAtTopLevel(queryStr, " max ");
            return df.getOWLObjectMaxCardinality(
                    Integer.parseInt(parts.get(1).trim()),
                    df.getOWLObjectProperty(IRI.create(ns + parts.get(0).trim())));
        }

        return df.getOWLClass(IRI.create(ns + queryStr.trim()));
    }

    private static List<String> splitAtTopLevel(String input, String delimiter) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int lastIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && input.startsWith(delimiter, i)) {
                result.add(input.substring(lastIndex, i).trim());
                lastIndex = i + delimiter.length();
                i += delimiter.length() - 1;
            }
        }
        result.add(input.substring(lastIndex).trim());
        return result;
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

