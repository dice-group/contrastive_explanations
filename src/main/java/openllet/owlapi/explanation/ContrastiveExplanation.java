package openllet.owlapi.explanation;

import org.semanticweb.owlapi.model.*;
import java.util.*;

public class ContrastiveExplanation {
    private final Set<OWLAxiom> common;
    private final Set<OWLAxiom> different;
    private final Map<OWLNamedIndividual, OWLNamedIndividual> factMapping;
    private final Map<OWLNamedIndividual, OWLNamedIndividual> foilMapping;
    private final Set<OWLAxiom> conflict;

    public ContrastiveExplanation(Set<OWLAxiom> common, Set<OWLAxiom> different,
                                  Map<OWLNamedIndividual, OWLNamedIndividual> factMapping,
                                  Map<OWLNamedIndividual, OWLNamedIndividual> foilMapping,
                                  Set<OWLAxiom> conflict) {
        this.common = common;
        this.different = different;
        this.factMapping = factMapping;
        this.foilMapping = foilMapping;
        this.conflict = conflict;
    }

    public Set<OWLAxiom> getCommon() {
        return common;
    }

    public Set<OWLAxiom> getDifferent() {
        return different;
    }

    public Map<OWLNamedIndividual, OWLNamedIndividual> getFactMapping() {
        return factMapping;
    }

    public Map<OWLNamedIndividual, OWLNamedIndividual> getFoilMapping() {
        return foilMapping;
    }

    public Set<OWLAxiom> getConflict() {
        return conflict;
    }
}

