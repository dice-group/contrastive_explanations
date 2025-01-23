package nl.vu.kai.contrastive.experiments.classGeneration;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import tools.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates complex class expressions that are satisfied by at least one individual in the given ontology
 */
public abstract class ClassExpressionGenerator {

    protected final OWLOntology ontology;
    protected final OWLDataFactory factory;
    protected final OWLReasoner reasoner;

    protected final Random random = new Random(); // initialize with 0 to have reproducible results

    public ClassExpressionGenerator(OWLOntology ont, OWLDataFactory factory){
        this.ontology=ont;
        this.factory=factory;
        OWLReasonerFactory reasonerFactory = reasonerFactory();
        this.reasoner=reasonerFactory.createReasoner(ont);
    }

    protected abstract OWLReasonerFactory reasonerFactory();

    public abstract OWLClassExpression generateClassExpression(OWLNamedIndividual individual, int maxSize);

    public List<OWLClassExpression> generateClassExpressions(int number, int size) {

        List<OWLClassExpression> result =new LinkedList<>();

        List<OWLNamedIndividual> individuals = ontology.individualsInSignature(Imports.INCLUDED)
                .filter(x ->
                        ontology.classAssertionAxioms(x)
                                .findAny()
                                .isPresent()
                        || ontology.objectPropertyAssertionAxioms(x)
                                .findAny()
                                .isPresent())
                .collect(Collectors.toList());

        if(individuals.isEmpty())
            throw new IllegalArgumentException("No individual in this ontology satisfies anything!");

        for(int i = 0; i<number; i++){
            OWLNamedIndividual individual = Util.randomItem(individuals, random);
            result.add(generateClassExpression(individual,size));
        }

        return result;
    }

    protected int size(OWLClassExpression exp) {
        return exp.accept(new SizeVisitor());
    }

    private class SizeVisitor implements OWLClassExpressionVisitorEx<Integer> {
        @Override
        public Integer visit(OWLClass ce) {
            return 1;
        }
        @Override
        public Integer visit(OWLObjectSomeValuesFrom ce){
            return 1+ce.getFiller().accept(this);
        }
        public Integer visit(OWLObjectIntersectionOf ce) {
            return ce.operands()
                    .collect(Collectors.summingInt(x -> x.accept(this)));
        }
    }

}
