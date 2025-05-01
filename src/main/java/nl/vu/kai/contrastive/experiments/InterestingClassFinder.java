package nl.vu.kai.contrastive.experiments;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class InterestingClassFinder {

    public static Set<OWLEntity> findInterestingEntities(OWLOntology ontology, OWLDataFactory factory){

        OWLReasoner reasoner = new ReasonerFactory().createReasoner(ontology);

        Set<OWLEntity> result = new HashSet<>();

        for(OWLClass clazz:ontology.getClassesInSignature(Imports.INCLUDED)){
            if(!reasoner.getInstances(clazz).isEmpty() &&
                    !reasoner.getInstances(factory.getOWLObjectComplementOf(clazz)).isEmpty()) {
                System.out.println("Interesting class: "+clazz);
                result.add(clazz);
            }
        }
        for(OWLObjectProperty prp:ontology.getObjectPropertiesInSignature(Imports.INCLUDED)){
            if(ontology.individualsInSignature(Imports.INCLUDED)
                    .anyMatch(i -> !reasoner.getObjectPropertyValues(i,prp).isEmpty())
                && !reasoner.getInstances(factory.getOWLObjectAllValuesFrom(prp, factory.getOWLNothing())).isEmpty()) {
                System.out.println("Interesting property: "+prp);
                result.add(prp);
            }
        }
        System.out.println();
        if(result.isEmpty())
            System.out.println("Nothing found!");
        System.out.println();
        return result;
    }

    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology ont = man.loadOntologyFromOntologyDocument(new File(args[0]));
        findInterestingEntities(ont, man.getOWLDataFactory());
    }
}
