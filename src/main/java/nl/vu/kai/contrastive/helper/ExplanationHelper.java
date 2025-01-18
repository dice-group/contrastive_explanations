package nl.vu.kai.contrastive.helper;

import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.modularity.OntologySegmenter;
import nl.vu.kai.contrastive.ContrastiveExplanationProblem;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExplanationHelper {

    public static Set<OWLAxiom> getRelevantAxioms(ContrastiveExplanationProblem problem) {
        OWLOntology ontology = problem.getOntology();
        Set<OWLEntity> signature = problem.getOwlClassExpression().signature().collect(Collectors.toSet());
        signature.add(problem.getFact());

        OntologySegmenter moduleExtractor =
                new SyntacticLocalityModuleExtractor(ontology.getOWLOntologyManager(), ontology, ModuleType.STAR);

        Set<OWLAxiom> result = moduleExtractor.extract(signature);

        return result;
    }

    public static Set<OWLNamedIndividual> getRelevantIndividuals(
            ContrastiveExplanationProblem problem, Set<OWLAxiom> axioms) {

        int distance = getMaxDistance(problem.getFact(), axioms);

        Set<OWLEntity> signature = axioms
                .stream()
                .flatMap(x -> x.signature())
                .collect(Collectors.toSet());

        Set<OWLNamedIndividual> result = withinDistance(problem.getOntology(), problem.getFoil(), distance, signature);

        return result;
    }

    private static Set<OWLNamedIndividual> withinDistance(
            OWLOntology ontology,
            OWLNamedIndividual foil,
            int maxDistance,
            Set<OWLEntity> signature) {

        Set<OWLNamedIndividual> collected = new HashSet<>();

        Set<OWLAxiom> axioms = ontology.aboxAxioms(Imports.INCLUDED)
                .filter(x -> x
                        .signature()
                        .allMatch(signature::contains))
                .collect(Collectors.toSet());
        Map<OWLIndividual, Integer> distances = new HashMap<>();
        distances.put(foil, 0);
        boolean change = true;
        while(change){
            change = false;
            for(OWLAxiom axiom : axioms){
                if(axiom instanceof OWLObjectPropertyAssertionAxiom) {
                    OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom) axiom;

                    if(distances.containsKey(pa.getSubject()) && distances.get(pa.getSubject())<maxDistance){
                        if(pa.getObject().isNamed())
                            collected.add(pa.getObject().asOWLNamedIndividual());
                        int dist = distances.get(pa.getSubject())+1;
                        if(!distances.containsKey(pa.getObject()) || distances.get(pa.getObject())>dist) {
                            change = true;
                            if (pa.getObject().isNamed())
                                distances.put(pa.getObject(), dist);
                        }
                    }
                    if(distances.containsKey(pa.getObject()) && distances.get(pa.getObject())<maxDistance){
                        if(pa.getSubject().isNamed())
                            collected.add(pa.getSubject().asOWLNamedIndividual());
                        int dist = distances.get(pa.getObject())+1;
                        if(!distances.containsKey(pa.getSubject()) || distances.get(pa.getSubject())>dist) {
                            change = true;
                            if (pa.getSubject().isNamed())
                                distances.put(pa.getSubject(), dist);
                        }
                    }
                }

            }
        }

        return collected;
    }

    /**
     * Collect the largest path distance to any individual in the axiom set,
     * where the path distance is defined as the length of the shortest path to that individual
     */
    private static int getMaxDistance(OWLNamedIndividual fact, Set<OWLAxiom> axioms) {
        Map<OWLIndividual, Integer> distances = new HashMap<>();
        distances.put(fact, 0);
        boolean change = true;
        while(change){
            change = false;
            for(OWLAxiom axiom : axioms){
                if(axiom instanceof OWLObjectPropertyAssertionAxiom) {
                    OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom) axiom;

                    if(distances.containsKey(pa.getSubject())){
                        int dist = distances.get(pa.getSubject())+1;
                        if(!distances.containsKey(pa.getObject()) || distances.get(pa.getObject())>dist) {
                            change = true;
                            if (pa.getObject().isNamed())
                                distances.put(pa.getObject(), dist);
                        }
                    }
                    if(distances.containsKey(pa.getObject())){
                        int dist = distances.get(pa.getObject())+1;
                        if(!distances.containsKey(pa.getSubject()) || distances.get(pa.getSubject())>dist) {
                            change = true;
                            if (pa.getSubject().isNamed())
                                distances.put(pa.getSubject(), dist);
                        }
                    }
                }

            }
        }
        int maxDistance = 0;
        for(int dist: distances.values())
            maxDistance=Math.max(maxDistance,dist);
        return maxDistance;
    }
}

