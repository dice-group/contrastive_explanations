package nl.vu.kai.contrastive.old;


import nl.vu.kai.contrastive.helper.ABoxProcessor;
import nl.vu.kai.contrastive.helper.IndividualGenerator;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import tools.Pair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;

public class AbductionManager {
    private OntologyLoader ontologyLoader;
    private QueryParser queryParser;
    private ExplanationGenerator explanationGenerator;
    private IndividualGenerator individualPairGenerator;
    private ABoxProcessor aboxProcessor;

    public AbductionManager() {
        ontologyLoader = new OntologyLoader();
        queryParser = new QueryParser();
        explanationGenerator = new ExplanationGenerator();
        OWLDataFactory fact = OWLManager.getOWLDataFactory();
        individualPairGenerator = new IndividualGenerator(fact);
        aboxProcessor = new ABoxProcessor(individualPairGenerator, fact);
    }

    // Main entry point of the application
    public static void main(String[] args) throws OWLOntologyCreationException, IOException, OWLException {

        // Record the start time
        long startTime = System.currentTimeMillis();

        // Create a scanner for console input
        Scanner scanner = new Scanner(System.in);

        // Prompt the user for the input file path
        System.out.print("Enter the path to the input file: ");
        String inputPath = scanner.nextLine();

        // Read the input file
        String inputContent = new String(Files.readAllBytes(Paths.get(inputPath)));
        String[] lines = inputContent.split("\n");

        // Read ontology path, namespace, query, and individual names from the input
        String localOntologyPath = lines[0].split("=")[1].trim();
        String ns = lines[1].split("=")[1].trim();
        String queryStr = lines[2].split("=")[1].trim();
        String individualWithExplan = lines[3].split("=")[1].trim();
        String individualWithoutExplan = lines[4].split("=")[1].trim();

        // Create an instance of the nl.vu.kai.contrastive.old.AbductionManager class and run the explanation generation
        AbductionManager app = new AbductionManager();
        app.run(ns, localOntologyPath, queryStr, individualWithExplan, individualWithoutExplan);

        // Record the end time
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + " milliseconds");

        // Close the scanner
        scanner.close();
    }

    public void run(String ns, String localOntologyPath, String queryStr, String individualWithExplan, String individualWithoutExplan) throws OWLOntologyCreationException, OWLException, IOException {
        // Load ontology and parse the query
        OWLOntology ontology = ontologyLoader.loadOntology(localOntologyPath);
        OWLClassExpression query = queryParser.parseQueryString(ns, queryStr);
        // Create the reasoner and load the ontology
        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        // Create an clashExplanation generator
        final PelletExplanation expGen = new PelletExplanation(reasoner);

     /*   PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);

        PelletExplanation expGen = new PelletExplanation(reasoner);*/


        // Get all individuals from the ontology
        Set<OWLNamedIndividual> allIndividuals = ontologyLoader.getAllIndividual(ontology);
        System.out.println("All individual avilable in the ontology: " + allIndividuals);

        Set<Pair<OWLNamedIndividual, OWLNamedIndividual>> individualPairs = individualPairGenerator.generatePairs(allIndividuals);
        System.out.println("Individual pairs: " + individualPairs);

        Set<OWLAxiom> originalAbox = ontology.getABoxAxioms(Imports.INCLUDED);
        System.out.println("All originalAbox contents of ontology: " + originalAbox);
/*
        Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> abox2 = aboxProcessor.innerGenerateNewAbox2(originalAbox, allIndividuals);
        System.out.println("New ABox abox2  contents size: " + abox2.size());
        System.out.println("New ABox abox2  contents: " + abox2);

        Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> abox3 = aboxProcessor.generateNewAbox3(originalAbox, abox2, ontology.getOWLOntologyManager().getOWLDataFactory());
        System.out.println("New ABox3  contents size: " + abox3.size());
        System.out.println("New ABox3  contents: " + abox3);

        Set<OWLAxiom> owlAxiomSet = aboxProcessor.transform2ABox(abox3, ontology.getOWLOntologyManager().getOWLDataFactory());
        System.out.println("New owlAxiomSet  contents size: " + owlAxiomSet.size());
        System.out.println("New owlAxiomSet  contents: " + owlAxiomSet);*/
    }
}
