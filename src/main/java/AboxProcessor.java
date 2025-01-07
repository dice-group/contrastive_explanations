import tools.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.*;

public class AboxProcessor {

    /*
        ABox 1: the original ABox
        ABox 2: for every axiom A(a)/r(a,b) in ABox 1, and all individuals x1,x2, we add A([a,x1])/r([a,x1],[b,x2])
        ABox 3: contains all axioms A([a,x])/r([a,x1],[a,x2]) from ABox 2 for which A(x)/r(x1,x2) is in ABox 2
     */


    // Process the ABox and generate new ABox entries based on the axiom type
    public Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> generateNewAbox2(
            Set<OWLAxiom> abox, Set<OWLNamedIndividual> individuals) {
        // Set to store either OWLClass or OWLObjectProperty with two lists of OWLNamedIndividual
        Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> newAbox = new HashSet<>();

        for (OWLAxiom axiom : abox) {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
                OWLClassExpression classExpression = classAssertion.getClassExpression();
                OWLNamedIndividual individual = classAssertion.getIndividual().asOWLNamedIndividual();

                // Check if the class expression is a named class
                if (!classExpression.isAnonymous()) {
                    OWLClass owlClass = classExpression.asOWLClass();

                    for (OWLNamedIndividual ind : individuals) {
                        // Add the OWLClass as the property in the pair
                        newAbox.add(new Pair<>(owlClass, new Pair<>(Collections.singletonList(individual), Collections.singletonList(ind))));
                    }
                } else {
                    System.out.println("Skipping anonymous class expression: " + classExpression);
                }
            } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom propertyAssertion = (OWLObjectPropertyAssertionAxiom) axiom;
                OWLObjectProperty property = propertyAssertion.getProperty().asOWLObjectProperty();
                OWLNamedIndividual subject = propertyAssertion.getSubject().asOWLNamedIndividual();
                OWLNamedIndividual object = propertyAssertion.getObject().asOWLNamedIndividual();

                for (OWLNamedIndividual ind1 : individuals) {
                    for (OWLNamedIndividual ind2 : individuals) {
                        // Add the OWLObjectProperty as the property in the pair
                        newAbox.add(new Pair<>(property, new Pair<>(Arrays.asList(subject, ind1), Arrays.asList(object, ind2))));
                    }
                }
            }
        }
        return newAbox;
    }


    public Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> generateNewAbox3(
            Set<OWLAxiom> originalAbox,
            Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> abox2,
            OWLDataFactory dataFactory) {
        Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> newAbox = new HashSet<>();
        for (Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>> element : abox2) {
            Object firstElement = element.getKey();
            Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>> pair = element.getValue();
            if (firstElement instanceof OWLClassExpression) {
                OWLClassExpression classExpression = (OWLClassExpression) firstElement;
                if (pair.getValue().size() == 1) {
                    OWLNamedIndividual individual = pair.getValue().get(0);
                    OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(classExpression, individual);
                    if (originalAbox.contains(classAssertion)) {
                        newAbox.add(element);
                    }
                }
            } else if (firstElement instanceof OWLObjectProperty) {
                OWLObjectProperty objectProperty = (OWLObjectProperty) firstElement;

                if (pair.getValue().size() == 2) {
                    OWLNamedIndividual subject = pair.getKey().get(1);
                    OWLNamedIndividual object = pair.getValue().get(1);
                    OWLObjectPropertyAssertionAxiom propertyAssertion = dataFactory.getOWLObjectPropertyAssertionAxiom(objectProperty, subject, object);

                    if (originalAbox.contains(propertyAssertion)) {
                        newAbox.add(element);
                    }
                }
            }
        }
        return newAbox;
    }

    public Set<OWLAxiom> transform2ABox(Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> abox2) {
        throw new AssertionError("not implemented yet!");
    }
}
