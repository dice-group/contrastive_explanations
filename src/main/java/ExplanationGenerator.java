
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.PelletReasoner;
import openllet.owlapi.PelletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import java.util.Set;

public class ExplanationGenerator {

    public Set<Set<OWLAxiom>> generateExplanations(OWLOntology ontology, OWLNamedIndividual individual, OWLClassExpression query) {
        // Create the reasoner and load the ontology
        final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        // Create an clashExplanation generator
        final PelletExplanation expGen = new PelletExplanation(reasoner);
       return expGen.getInstanceExplanations(individual, query, 10);
    }
}
