package nl.vu.kai.contrastive.experiments.helpers;

import nl.vu.kai.tools.Pair;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.*;

public class InterestingIndividualPairFinders {
    public static Map<OWLNamedIndividual,Set<OWLNamedIndividual>>
    findInterestingIndividuals(OWLOntology ontology) {

        Map<OWLNamedIndividual, Set<OWLNamedIndividual>> result = new HashMap();

        Set<OWLAxiom> removed = new HashSet<>();
        ontology.axioms(AxiomType.DIFFERENT_INDIVIDUALS).forEach(removed::add);
        ontology.removeAxioms(removed);

        OWLReasoner reasoner = new ReasonerFactory().createReasoner(ontology);
        for(OWLNamedIndividual ind:ontology.getIndividualsInSignature(Imports.INCLUDED)){
            Set<OWLNamedIndividual> diff = reasoner.getDifferentIndividuals(ind).getFlattened();
            if(!diff.isEmpty()){
                System.out.println("Found different individuals!");
                result.put(ind,diff);
            }
        }

        ontology.addAxioms(removed);

        return result;
    }

    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology ont = man.loadOntologyFromOntologyDocument(new File(args[0]));

        findInterestingIndividuals(ont);
    }
}
