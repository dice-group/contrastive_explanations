package ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class OntologyLoader {

    public OWLOntology  loadOntology(String localOntologyPath) throws OWLOntologyCreationException {
        OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();
        File file = new File(localOntologyPath);
        return owlManager.loadOntologyFromOntologyDocument(file);
    }

    public PelletReasoner getReasoner(OWLOntology owlOntology){
        return PelletReasonerFactory.getInstance().createReasoner(owlOntology);
    }

    public Set<OWLNamedIndividual> getIndividualsForQuery(PelletReasoner reasoner, OWLClassExpression query) {
        NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(query, false);
        Set<OWLNamedIndividual> individuals = instances.getFlattened();

        return individuals;
    }
    public OWLNamedIndividual getIndividualByName(OWLOntology ontology, String individualName) {
        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            if (individual.getIRI().getFragment().equals(individualName)) {
                return individual;
            }
        }
        return null;
    }
    public Set<OWLNamedIndividual> getAllIndividual(OWLOntology ontology) {
        Set<OWLNamedIndividual> individuals = new HashSet<>();

        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            individuals.add(individual);
        }
        return individuals;
    }
}
