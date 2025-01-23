package nl.vu.kai.contrastive.experiments.helpers;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Find appropriate candidates for foils
 */
public class FoilCandidateFinder {
    private final OWLOntology ontology;

    private final Strategy strategy;

    private OWLReasoner owlReasoner;

    public enum Strategy {
        CommonPropertyAssertion, CommonClass
    }

    public FoilCandidateFinder(OWLOntology ontology, Strategy strategy){
        this.ontology=ontology;
        this.strategy=strategy;
    }

    public void setReasoner(OWLReasoner reasoner) {
        this.owlReasoner=reasoner;
    }

    public Stream<OWLNamedIndividual> foilCandidates(OWLNamedIndividual fact) {
        switch (strategy) {
            case CommonPropertyAssertion:
                assert owlReasoner!=null;
                return commonPropertyAssertion(fact);
            case CommonClass:
                return commonClass(fact);
            default:
                throw new IllegalArgumentException("Unexpected strategy: "+strategy);
        }
    }

    private Stream<OWLNamedIndividual> commonClass(OWLNamedIndividual fact) {
        return owlReasoner.types(fact)
                .filter(x -> !x.isTopEntity())
                .flatMap(x -> owlReasoner.instances(x));
    }

    private Stream<OWLNamedIndividual> commonPropertyAssertion(OWLNamedIndividual fact) {
        Set<OWLObjectPropertyExpression> properties = ontology.objectPropertyAssertionAxioms(fact)
                .map(OWLObjectPropertyAssertionAxiom::getProperty)
                .collect(Collectors.toSet());
        return ontology.individualsInSignature(Imports.INCLUDED)
                .filter(x -> !x.equals(fact))
                .filter( x->
                        ontology.objectPropertyAssertionAxioms(x)
                                .map(OWLObjectPropertyAssertionAxiom::getProperty)
                                .anyMatch(properties::contains)
                        );
    }
}
