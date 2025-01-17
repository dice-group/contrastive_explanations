package nl.vu.kai.contrastive;

import org.semanticweb.owlapi.model.*;

public class ContrastiveExplanationProblem {
    private final OWLOntology ontology;
    private final OWLClassExpression owlClassExpression;
    private final OWLNamedIndividual fact;
    private final OWLNamedIndividual foil;

    public ContrastiveExplanationProblem(OWLOntology ontology, OWLClassExpression owlClassExpression,
                                         OWLNamedIndividual fact, OWLNamedIndividual foil) {
        this.ontology = ontology;
        this.owlClassExpression = owlClassExpression;
        this.fact = fact;
        this.foil = foil;
     }

    public OWLOntology getOntology() {
        return ontology;
    }

    public OWLClassExpression getOwlClassExpression() {
        return owlClassExpression;
    }

    public OWLNamedIndividual getFact() {
        return fact;
    }

    public OWLNamedIndividual getFoil() {
        return foil;
    }
}
