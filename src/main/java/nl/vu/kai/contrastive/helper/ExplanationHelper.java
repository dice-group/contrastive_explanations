package nl.vu.kai.contrastive.helper;

import nl.vu.kai.contrastive.ContrastiveExplanationProblem;
import org.semanticweb.owlapi.model.*;
import java.util.Set;

public class ExplanationHelper {

    public static Set<OWLAxiom> getRelevantAxioms(ContrastiveExplanationProblem problem) {
        return problem.getOntology().getAxioms();
    }

    public static Set<OWLNamedIndividual> getRelevantIndividuals(ContrastiveExplanationProblem problem) {
        return problem.getOntology().getIndividualsInSignature();
    }
}

