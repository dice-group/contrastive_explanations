import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import io.PelletExplanation;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import java.util.Set;

public class ExplanationGenerator {

    public Set<Set<OWLAxiom>> generateExplanations(OWLOntology ontology, OWLNamedIndividual individual, OWLClassExpression query) {
        PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);
        PelletExplanation expGen = new PelletExplanation(reasoner);
       return expGen.getInstanceExplanations(individual, query, 10);
    }
}
