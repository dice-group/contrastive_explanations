package nl.vu.kai.contrastive;

import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * This corresponds to what is called commonality in the paper, e.g. q_com
     */
    public Set<OWLAxiom> getCommon() {
        return common;
    }

    /**
     * This corresponds to what is called difference in the paper, e.g. q_diff
     */
    public Set<OWLAxiom> getDifferent() {
        return different;
    }

    /**
     * this is how we implemented the fact evidence vector, as a mapping from variables to individual names
     */
    public Map<OWLNamedIndividual, OWLNamedIndividual> getFactMapping() {
        return factMapping;
    }

    /**
     * this is how we implemented the foil evidence vector, as a mapping from variables to individual names
     */
    public Map<OWLNamedIndividual, OWLNamedIndividual> getFoilMapping() {
        return foilMapping;
    }

    /**
     * this is what is called conflict set in the paper, i.e. \mathcal{C}
     */
    public Set<OWLAxiom> getConflict() {
        return conflict;
    }

    public String toString(){
        return "Common: "+common+"\n"+
                "Different: "+different+"\n"+
                "Fact mapping: "+factMapping+"\n"+
                "Foil mapping: "+foilMapping+"\n"+
                "Conflicts: "+conflict;
    }

    public String toString(ManchesterOWLSyntaxOWLObjectRendererImpl renderer) {
        return "Common: "+common.stream()
                    .map(renderer::render)
                    .collect(Collectors.joining(", "))+"\n"+
                "Different: "+different.stream()
                    .map(renderer::render)
                    .collect(Collectors.joining(", "))+"\n"+
                "Fact mapping: "+factMapping.entrySet()
                    .stream()
                    .map(x -> renderer.render(x.getKey())+"->"+renderer.render(x.getValue()))
                    .collect(Collectors.joining(", "))+"\n"+
                "Foil mapping: "+foilMapping.entrySet()
                    .stream()
                    .map(x -> renderer.render(x.getKey())+"->"+renderer.render(x.getValue()))
                    .collect(Collectors.joining(", "))+"\n"+
                "Conflicts: "+conflict.stream()
                    .map(renderer::render)
                .collect(Collectors.joining(", "))+"\n";
    }
}

