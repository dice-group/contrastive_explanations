import com.clarkparsia.owlapi.explanation.io.ConciseExplanationRenderer;
import com.clarkparsia.owlapiv3.OWL;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import io.PelletExplanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class AbductionReasoning {

    private static final String DEFAULT_OUTPUT_DIR = "output/"; // Customize the directory as needed

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
        AbductionReasoning app = new AbductionReasoning();
        app.run(ns, localOntologyPath, queryStr, individualWithExplan, individualWithoutExplan);

        // Record the end time
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + " milliseconds");

        // Close the scanner
        scanner.close();
    }

    public void run(String ns, String localOntologyPath, String queryStr, String individualWithExplan, String individualWithoutExplan) throws OWLOntologyCreationException, OWLException, IOException {
        PelletExplanation.setup();
        ConciseExplanationRenderer renderer = new ConciseExplanationRenderer();
        OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
        File file = new File(localOntologyPath);
        OWLOntology ontology = owlmanager.loadOntologyFromOntologyDocument(file);
        PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);
        PelletExplanation expGen = new PelletExplanation(reasoner);

        // Retrieve individuals from ontology
        OWLNamedIndividual owlIndividualWithExplan = getIndividualByName(ontology, individualWithExplan);
        OWLNamedIndividual owlIndividualWithoutExplan = getIndividualByName(ontology, individualWithoutExplan);

        OWLClassExpression query = parseQueryString(ns, queryStr);
        System.out.println("Query:--> " + query);

        // Get instances of the query
        NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(query, false);
        Set<OWLNamedIndividual> individuals = instances.getFlattened();
        // Process the individuals
        for (OWLNamedIndividual selectedIndividual : individuals) {
            if(selectedIndividual.equals(owlIndividualWithExplan)) {

                // Explain the classification of the selected individual
                Set<Set<OWLAxiom>> explanations = expGen.getInstanceExplanations(selectedIndividual, query, 10);
                Set<Set<OWLAxiom>> allCommonAxiomsSet = findCommonAxioms(explanations, ontology);
                System.out.println("allCommonAxiomsSet : "+allCommonAxiomsSet);
                System.out.println("allCommonAxiomsSet size: "+allCommonAxiomsSet.size());
                // Iterate over each element in commonAxioms
                for (Set<OWLAxiom> commonAxiomSet : allCommonAxiomsSet) {
                    // Apply abduction reasoning for each common axiom set
                    applyAbductionForIndividuals(ontology, explanations, owlIndividualWithExplan, owlIndividualWithoutExplan, commonAxiomSet);
                }
            }
        }
    }

    public static Set<Set<OWLAxiom>> findCommonAxioms(Set<Set<OWLAxiom>> explanationsSet, OWLOntology ontology) {
        Set<OWLAxiom> aBoxAxioms = ontology.getABoxAxioms(Imports.INCLUDED);
        Set<Set<OWLAxiom>> commonAxiomsSet = new HashSet<>();


        for (Set<OWLAxiom> explanationAxioms : explanationsSet) {
            Set<OWLAxiom> commonAxioms = new HashSet<>();

            for(OWLAxiom explanationAxiom : explanationAxioms){
            {
                if (aBoxAxioms.contains(explanationAxiom)) {
                    commonAxioms.add(explanationAxiom);
                }
            }
            }
            commonAxiomsSet.add(commonAxioms);
        }


        return commonAxiomsSet;
    }

    public void applyAbductionForIndividuals(OWLOntology ontology, Set<Set<OWLAxiom>> explanations, OWLNamedIndividual individualWithExplan, OWLNamedIndividual individualWithoutExplan, Set<OWLAxiom> commonAxioms) {

        OWLOntology ontology1= ontology;

        // Modify axioms for the first individual
        Set<OWLAxiom> axiomsForFirstIndividual = new HashSet<>();
       /* for (OWLAxiom axiom : commonAxioms) {
            replaceIndividualInAxiom(axiom, firstIndividual, secondIndividual, axiomsForFirstIndividual);
        }*/
        replaceIndividualInAxiom(commonAxioms, individualWithExplan, individualWithoutExplan, axiomsForFirstIndividual);

        // Check for missing axioms
        Set<OWLAxiom> missingAxioms = new HashSet<>();
        Set<OWLAxiom> aBoxAxioms = ontology.getABoxAxioms(Imports.INCLUDED);

        for (OWLAxiom axiom : axiomsForFirstIndividual) {
            if (!aBoxAxioms.contains(axiom)) {
                missingAxioms.add(axiom);
            }
        }

        // Add missing axioms to the ontology
        OWLOntologyManager manager = ontology1.getOWLOntologyManager();
       //manager.addAxioms(ontology1, missingAxioms);

        System.out.println("Missing axioms for " + individualWithoutExplan + ": " + missingAxioms);
    }

    private OWLNamedIndividual getIndividualByName(OWLOntology ontology, String individualName) {
        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            if (individual.getIRI().getFragment().equals(individualName)) {
                return individual;
            }
        }
        return null;
    }

    private void replaceIndividualInAxiom(Set<OWLAxiom> commonAxioms, OWLNamedIndividual original, OWLNamedIndividual replacement, Set<OWLAxiom> modifiedAxioms) {
        // Get the OWLDataFactory from the ontology manager
        OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = owlmanager.getOWLDataFactory();
        OWLNamedIndividual fixedVar = dataFactory.getOWLNamedIndividual(IRI.create("http://example.org/xyz"));

        // Iterate over the axioms in commonAxioms
        for (OWLAxiom axiom : commonAxioms) {
            // Visit the axiom and replace the individual where needed
            axiom.accept(new OWLObjectVisitorAdapter() {

                @Override
                public void visit(OWLClassAssertionAxiom axiom) {
                    if (axiom.getIndividual().equals(original)) {
                        OWLAxiom newAxiom = dataFactory.getOWLClassAssertionAxiom(axiom.getClassExpression(), replacement);
                        modifiedAxioms.add(newAxiom);
                    }
                }

                // Handle OWL Object Property Assertion Axiom
                @Override
                public void visit(OWLObjectPropertyAssertionAxiom axiom) {
                    if (axiom.getSubject().equals(original)) {
                        if (axiom.getObject().isNamed()) {
                            OWLNamedIndividual objectInd = axiom.getObject().asOWLNamedIndividual();

                            // Replace subject with replacement and object with fixedVar
                            OWLAxiom newAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), replacement, fixedVar);
                            modifiedAxioms.add(newAxiom);

                            // Now, look for the class assertion involving the object (ML in this case)
                            for (OWLAxiom classAxiom : commonAxioms) {
                                if (classAxiom instanceof OWLClassAssertionAxiom) {
                                    OWLClassAssertionAxiom classAssertionAxiom = (OWLClassAssertionAxiom) classAxiom;
                                    if (classAssertionAxiom.getIndividual().equals(objectInd)) {
                                        // If the object individual is of type "AI"
                                        // OWLClassAssertionAxiom newClassAssertionAxiom = dataFactory.getOWLClassAssertionAxiom(dataFactory.getOWLClass(IRI.create("http://www.semanticweb.org/CEX-Paper#AI")), fixedVar);
                                        OWLAxiom newAxi = dataFactory.getOWLClassAssertionAxiom(((OWLClassAssertionAxiom) classAxiom).getClassExpression(), fixedVar);
                                        modifiedAxioms.add(newAxi);
                                    }
                                }
                            }
                        }
                    }
                }



            });
        }
    }



  /*  public OWLClassExpression parseQueryString(String ns, String queryStr) {
        String str = ns + queryStr;
        return OWL.Class(str);
    }*/

    private OWLClassExpression parseQueryString(String ns, String queryStr) {
        // Remove outer parentheses if present
        queryStr = queryStr.trim();
        if (queryStr.startsWith("(") && queryStr.endsWith(")")) {
            queryStr = queryStr.substring(1, queryStr.length() - 1).trim();
        }

        // Handle OR at the top level
        List<String> orParts = splitAtTopLevel(queryStr, " or ");
        if (orParts.size() > 1) {
            List<OWLClassExpression> orExpressions = new ArrayList<>();
            for (String orPart : orParts) {
                orExpressions.add(parseQueryString(ns, orPart));
            }
            return OWL.or(orExpressions.toArray(new OWLClassExpression[0]));
        }

        // Handle AND at the top level
        List<String> andParts = splitAtTopLevel(queryStr, " and ");
        if (andParts.size() > 1) {
            List<OWLClassExpression> andExpressions = new ArrayList<>();
            for (String andPart : andParts) {
                andExpressions.add(parseQueryString(ns, andPart));
            }
            return OWL.and(andExpressions.toArray(new OWLClassExpression[0]));
        }

        // Handle NOT
        if (queryStr.startsWith("not ")) {
            String subQuery = queryStr.substring(4).trim();
            return OWL.not(parseQueryString(ns, subQuery));
        }

        // Handle existential quantification (some)
        if (queryStr.contains(" some ")) {
            List<String> subPartsList = splitAtTopLevel(queryStr, " some ");
            if (subPartsList.size() == 2) {
                OWLObjectProperty property = OWL.ObjectProperty(ns + subPartsList.get(0).trim());
                OWLClassExpression cls = parseQueryString(ns, subPartsList.get(1).trim());
                return OWL.some(property, cls);
            }
        }

        // Handle universal quantification (only)
        if (queryStr.contains(" only ")) {
            List<String> subPartsList = splitAtTopLevel(queryStr, " only ");
            if (subPartsList.size() == 2) {
                OWLObjectProperty property = OWL.ObjectProperty(ns + subPartsList.get(0).trim());
                OWLClassExpression cls = parseQueryString(ns, subPartsList.get(1).trim());
                return OWL.only(property, cls);
            }
        }

        // Handle cardinality restrictions (exactly, min, max)
        if (queryStr.contains(" exactly ")) {
            List<String> subPartsList = splitAtTopLevel(queryStr, " exactly ");
            if (subPartsList.size() == 2) {
                OWLObjectProperty property = OWL.ObjectProperty(ns + subPartsList.get(0).trim());
                int cardinality = Integer.parseInt(subPartsList.get(1).trim());
                return OWL.exactly(property, cardinality);
            }
        } else if (queryStr.contains(" min ")) {
            List<String> subPartsList = splitAtTopLevel(queryStr, " min ");
            if (subPartsList.size() == 2) {
                OWLObjectProperty property = OWL.ObjectProperty(ns + subPartsList.get(0).trim());
                int cardinality = Integer.parseInt(subPartsList.get(1).trim());
                return OWL.min(property, cardinality);
            }
        } else if (queryStr.contains(" max ")) {
            List<String> subPartsList = splitAtTopLevel(queryStr, " max ");
            if (subPartsList.size() == 2) {
                OWLObjectProperty property = OWL.ObjectProperty(ns + subPartsList.get(0).trim());
                int cardinality = Integer.parseInt(subPartsList.get(1).trim());
                return OWL.max(property, cardinality);
            }
        }

        // Handle simple class names
        return OWL.Class(ns + queryStr.trim());
    }

    // Helper method to split by a keyword at the top level, ignoring nested parentheses
    private List<String> splitAtTopLevel(String input, String delimiter) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int lastIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && input.startsWith(delimiter, i)) {
                result.add(input.substring(lastIndex, i).trim());
                lastIndex = i + delimiter.length();
                i += delimiter.length() - 1; // skip the delimiter
            }
        }
        result.add(input.substring (lastIndex).trim());
        return result;
    }

}

