package nl.vu.kai.contrastive.experiments.classGeneration;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import nl.vu.kai.tools.Util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ELClassExpressionGenerator extends ClassExpressionGenerator {

    public static double chanceForClass = 0.5;

    public ELClassExpressionGenerator(OWLOntology ont, OWLDataFactory factory){
        super(ont,factory);
    }

    @Override
    protected OWLReasonerFactory reasonerFactory() {
        return new ElkReasonerFactory();
    }

    @Override
    public OWLClassExpression generateClassExpression(OWLNamedIndividual individual, int size) {
        Set<OWLClassExpression> atoms = new HashSet<>();
        int remainingSize = size;
        while(remainingSize>0) {
            OWLClassExpression nextAtom = generateAtom(individual, remainingSize);
            atoms.add(nextAtom);
            remainingSize-=size(nextAtom);
        }
        if(atoms.size()==0)
            throw new AssertionError("!");
        assert atoms.size()>0;
        if(atoms.size()==1)
            return atoms.iterator().next();
        else
            return factory.getOWLObjectIntersectionOf(atoms);

    }

    public OWLClassExpression generateAtom(OWLNamedIndividual individual, int maxSize) {

        if(maxSize<1)
            throw new IllegalArgumentException("Cannot generate class expression of non-positive size!");

        if(maxSize==1
                || !ontology.objectPropertyAssertionAxioms(individual)
                .findAny()
                .isPresent()
                || random.nextDouble()<chanceForClass
        ){
            OWLClass clazz = Util.randomItem(reasoner.types(individual), random);

            return clazz;
        } else {
            assert maxSize>=2;

            OWLObjectPropertyAssertionAxiom pa =
                    Util.randomItem(
                            ontology.getObjectPropertyAssertionAxioms(individual)
                                    .stream()
                                    .filter(x -> x.getObject().isNamed()),
                            random);
            OWLClassExpression successorExpression =
                    generateClassExpression(pa.getObject().asOWLNamedIndividual(), maxSize-1);
            return factory.getOWLObjectSomeValuesFrom(pa.getProperty(), successorExpression);
        }
    }
}
