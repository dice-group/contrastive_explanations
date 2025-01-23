package nl.vu.kai.contrastive.helper;

import nl.vu.kai.contrastive.ContrastiveExplanation;
import org.semanticweb.owlapi.model.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContrastiveExplanationInstatiator {
    private final OWLDataFactory factory;

    public ContrastiveExplanationInstatiator(OWLDataFactory factory){
        this.factory=factory;
    }

    /**
     * Instantiate the axioms in the difference for the foil
     */
    public Stream<OWLAxiom> instantiateFoilDifference(ContrastiveExplanation ce){
        return ce.getDifferent()
                .stream()
                .map(x -> instantiate(x, ce.getFoilMapping()));
    }

    public OWLAxiom instantiate(OWLAxiom axiom, Map<OWLNamedIndividual,OWLNamedIndividual> mapping){
        if(axiom instanceof OWLClassAssertionAxiom){
            OWLClassAssertionAxiom ca = (OWLClassAssertionAxiom)axiom;
            return factory.getOWLClassAssertionAxiom(
                        ca.getClassExpression(),
                        apply(mapping, ca.getIndividual()));
        } else if(axiom instanceof OWLObjectPropertyAssertionAxiom){
            OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom) axiom;
            return factory.getOWLObjectPropertyAssertionAxiom(
                    pa.getProperty(),
                    apply(mapping, pa.getSubject()),
                    apply(mapping, pa.getObject()));
        } else
            throw new IllegalArgumentException("Should have been ABox: "+axiom);
    }
    public OWLIndividual apply(Map<OWLNamedIndividual,OWLNamedIndividual> mapping, OWLIndividual individual) {
        if(individual.isAnonymous())
            return individual;
        else
            return mapping.getOrDefault(individual, individual.asOWLNamedIndividual());
    }
}
