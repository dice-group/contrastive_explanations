package openllet.owlapi.explanation;

import helper.ExplanationHelper;
import org.semanticweb.owlapi.model.*;
import java.util.*;

public class ExplanationEngine {

    public ContrastiveExplanation computeExplanation(ContrastiveExplanationProblem problem) throws OWLOntologyCreationException {
        // Step 1: Use ExplanationHelper to get relevant axioms and individuals
        Set<OWLAxiom> relevantAxioms = ExplanationHelper.getRelevantAxioms(problem);
        Set<OWLNamedIndividual> relevantIndividuals = ExplanationHelper.getRelevantIndividuals(problem);

        // Step 2: Compute ABox2 and ABox3 (stubbed for now)
        Set<OWLAxiom> abox2 = new HashSet<>(); // TODO: Compute ABox2
        Set<OWLAxiom> abox3 = new HashSet<>(); // TODO: Compute ABox3

        // Step 3: Create "specialAxiom"
        OWLAxiom specialAxiom = problem.getOntology().getOWLOntologyManager()
                .getOWLDataFactory().getOWLClassAssertionAxiom(problem.getOwlClassExpression(), problem.getFact());

        // Step 4: Construct overApproximationOntology
        OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
        OWLOntology overApproximationOntology = manager.createOntology();
        manager.addAxioms(overApproximationOntology, relevantAxioms);
        manager.addAxioms(overApproximationOntology, abox2);

        // Step 5: Compute first explanation
        Set<OWLAxiom> different = computeExplanation(overApproximationOntology, specialAxiom, relevantAxioms, abox3);

        // Step 6: Update overApproximationOntology
        manager.removeAxioms(overApproximationOntology, abox2);
        manager.addAxioms(overApproximationOntology, abox3);
        manager.addAxioms(overApproximationOntology, different);

        // Step 7: Compute second explanation
        Set<OWLAxiom> common = computeExplanation(overApproximationOntology, specialAxiom, relevantAxioms, different);

        // Step 8: Return ContrastiveExplanation
        return new ContrastiveExplanation(common, different, null, null, null);
    }

    private Set<OWLAxiom> computeExplanation(OWLOntology ontology, OWLAxiom axiom,
                                             Set<OWLAxiom> fixedSet, Set<OWLAxiom> additionalSet) {
        // TODO: Implement explanation logic using reasoning APIs
        return new HashSet<>();
    }
}

