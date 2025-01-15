import openllet.owlapi.OpenlletReasoner;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class OntologyLoader {

    public OWLOntology loadOntology(String localOntologyPath) throws OWLOntologyCreationException {
        OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();
        File file = new File(localOntologyPath);
        return owlManager.loadOntologyFromOntologyDocument(file);
    }



    public Set<OWLNamedIndividual> getIndividualsForQuery(OpenlletReasoner reasoner, OWLClassExpression query) {
        NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(query, false);
        Set<OWLNamedIndividual> individuals = instances.getFlattened();

        return individuals;
    }
    public Set<OWLNamedIndividual> getAllIndividual(OWLOntology ontology) {
        Set<OWLNamedIndividual> individuals = new HashSet<>();

        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            individuals.add(individual);
        }
        return individuals;
    }
}
