import abduction.AbductionReasoning;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import io.PelletExplanation;
import ontology.OntologyLoader;
import ontology.QueryParser;
import org.semanticweb.owlapi.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class AbductionManager {
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

        // Create an instance of the AbductionReasoner class and run the explanation generation
        AbductionManager app = new AbductionManager();
        app.run(ns, localOntologyPath, queryStr, individualWithExplan, individualWithoutExplan);

        // Record the end time
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + " milliseconds");

        // Close the scanner
        scanner.close();
    }

    public void run(String ns, String localOntologyPath, String queryStr, String individualWithExplan, String individualWithoutExplan) throws OWLOntologyCreationException, OWLException, IOException {
        PelletExplanation.setup();
        OntologyLoader ontologyLoader = new OntologyLoader();
        QueryParser queryParser = new QueryParser();
        AbductionReasoning abductionReasoning = new AbductionReasoning();
        OWLOntology ontology = ontologyLoader.loadOntology(localOntologyPath);
        PelletReasoner reasoner = ontologyLoader.getReasoner(ontology);
        PelletExplanation expGen = new PelletExplanation(reasoner);

        // Retrieve individuals from ontology
        OWLNamedIndividual owlIndividualWithExplan = ontologyLoader.getIndividualByName(ontology, individualWithExplan);
        OWLNamedIndividual owlIndividualWithoutExplan = ontologyLoader.getIndividualByName(ontology, individualWithoutExplan);
        OWLClassExpression query = queryParser.parseQueryString(ns, queryStr);

        System.out.println("Query:--> " + query);

        // Get instances of the query
        Set<OWLNamedIndividual> individuals = ontologyLoader.getIndividualsForQuery(reasoner,query);
        // Process the individuals
        for (OWLNamedIndividual selectedIndividual : individuals) {
            if (selectedIndividual.equals(owlIndividualWithExplan)) {

                // Explain the classification of the selected individual
                Set<Set<OWLAxiom>> explanations = expGen.getInstanceExplanations(selectedIndividual, query, 10);
                Set<Set<OWLAxiom>> allCommonAxiomsSet = abductionReasoning.findCommonAxioms(explanations, ontology);
                System.out.println("allCommonAxiomsSet : " + allCommonAxiomsSet);
                System.out.println("allCommonAxiomsSet size: " + allCommonAxiomsSet.size());
                // Iterate over each element in commonAxioms
                for (Set<OWLAxiom> commonAxiomSet : allCommonAxiomsSet) {
                    // Apply abduction reasoning for each common axiom set
                    abductionReasoning.applyAbductionForIndividuals(ontology, explanations, owlIndividualWithExplan, owlIndividualWithoutExplan, commonAxiomSet);
                }
            }
        }
    }

}

