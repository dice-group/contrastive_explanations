package anonymized.contrastive.experiments;

import anonymized.contrastive.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ManualFactFoilExperimenter {

    public enum ReasonerChoice { ELK, HERMIT }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java ManualFactFoilExperimenter input.json");
            System.exit(1);
        }

        // Load and parse JSON config
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(args[0]));

        String ontologyPath = root.get("ontology_file_path").asText();

        // Read reasoner or use default
        ReasonerChoice reasoner = ReasonerChoice.ELK;
        if (root.has("reasoner")) {
            String reasonerStr = root.get("reasoner").asText().trim().toUpperCase();
            switch (reasonerStr) {
                case "HERMIT":
                    reasoner = ReasonerChoice.HERMIT;
                    break;
                case "ELK":
                    reasoner = ReasonerChoice.ELK;
                    break;
                default:
                    System.out.println("Warning: Unsupported reasoner '" + reasonerStr + "'. Defaulting to ELK.");
            }
        }

        // Read conflict-minimal flag or use default true
        boolean conflictMinimal = root.has("conflict_minimal") && root.get("conflict_minimal").asBoolean(true);

        // Logging suppression
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("org.semanticweb.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);

        // Ontology loading
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        System.out.println("Parsing ontology...");
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
        System.out.println("Detected namespace: " + getOntologyBaseIRI(ontology));

        OWLReasonerFactory reasonerFactory = switch (reasoner) {
            case HERMIT -> new ReasonerFactory();
            case ELK -> new ElkReasonerFactory();
            // Need implemenation for Pellet
        };

        OWLReasoner r = reasonerFactory.createReasoner(ontology);
        r.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
        Set<OWLNamedIndividual> allIndividuals = ontology.individualsInSignature().collect(Collectors.toSet());

        // Run all experiments
        JsonNode experiments = root.get("experiments");
        if (experiments == null || !experiments.isArray()) {
            System.out.println("Error: 'experiments' must be a list in JSON.");
            System.exit(1);
        }

        for (JsonNode exp : experiments) {
            String classExprStr = exp.get("class_expression").asText();

            OWLClassExpression classExpr = parseManchesterSyntax(dataFactory, ontology, classExprStr);
            System.out.println("\nParsed class expression: " + renderer.render(classExpr));

            List<String> facts = new ArrayList<>();
            exp.get("facts").forEach(f -> facts.add(f.asText().trim()));
            List<String> foils = new ArrayList<>();
            exp.get("foils").forEach(f -> foils.add(f.asText().trim()));

            for (String factName : facts) {
                OWLNamedIndividual fact = dataFactory.getOWLNamedIndividual(IRI.create(getOntologyBaseIRI(ontology) + factName));

                for (String foilName : foils) {
                    OWLNamedIndividual foil = dataFactory.getOWLNamedIndividual(IRI.create(getOntologyBaseIRI(ontology) + foilName));

                    System.out.println("\nFact: " + factName + " | Foil: " + foilName);
                    //System.out.println("Using Manchester: " + renderer.render(classExpr));

                    ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ontology, classExpr, fact, foil);
                    ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);
                    gen.useConflictMinimality(conflictMinimal);

                    long startTime = System.currentTimeMillis();
                    ContrastiveExplanation ce = gen.computeExplanation(cep);
                    long duration = System.currentTimeMillis() - startTime;

                    System.out.println("CE: " + ce.toString(renderer));

                    int commonSize = ce.getCommon().size();
                    int differenceSize = ce.getDifferent().size();
                    int conflictSize = ce.getConflict().size();
                    long freshIndividuals = ce.getFoilMapping().values().stream()
                            .filter(x -> !allIndividuals.contains(x)).count();

                    System.out.println("STATS: " + commonSize + " " + differenceSize + " " +
                            conflictSize + " " + freshIndividuals + " " + duration);
                }
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



