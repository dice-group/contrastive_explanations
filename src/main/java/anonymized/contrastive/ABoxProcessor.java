package anonymized.contrastive;

import anonymized.contrastive.helper.IndividualGenerator;
import anonymized.tools.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ABoxProcessor {

    private final IndividualGenerator individualGenerator;
    private final OWLDataFactory factory;

    public ABoxProcessor(IndividualGenerator individualGenerator, OWLDataFactory factory) {
        this.individualGenerator = individualGenerator;
        this.factory = factory;
    }

    // Process the ABox and generate new ABox entries based on the axiom type
    public Set<OWLAxiom> generateAbox2(
            Set<OWLAxiom> abox, Set<OWLNamedIndividual> individuals) {
        Set<OWLAxiom> abox2 = new HashSet<>();
        for (OWLAxiom axiom : abox) {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
                OWLClassExpression classExpression = classAssertion.getClassExpression();
                OWLNamedIndividual individual = classAssertion.getIndividual().asOWLNamedIndividual();

                // Check if the class expression is a named class
                for (OWLNamedIndividual ind : individuals) {
                    abox2.add(factory.getOWLClassAssertionAxiom(classExpression, individualGenerator.getIndividualForPair(individual, ind)));
                }

            } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom propertyAssertion = (OWLObjectPropertyAssertionAxiom) axiom;
                OWLObjectProperty property = propertyAssertion.getProperty().asOWLObjectProperty();
                OWLNamedIndividual subject = propertyAssertion.getSubject().asOWLNamedIndividual();
                OWLNamedIndividual object = propertyAssertion.getObject().asOWLNamedIndividual();

                for (OWLNamedIndividual ind1 : individuals) {
                    for (OWLNamedIndividual ind2 : individuals) {
                        // Add the OWLObjectProperty as the property in the pair
                        abox2.add(factory.getOWLObjectPropertyAssertionAxiom(
                                property,
                                individualGenerator.getIndividualForPair(subject, ind1),
                                individualGenerator.getIndividualForPair(object, ind2)
                        ));
                    }
                }
            }
        }
        return abox2;
    }

    public Set<OWLAxiom> generateABox3(Set<OWLAxiom> originalABox, Set<OWLAxiom> abox2) {
        Set<OWLAxiom> abox3 = new HashSet<>();
        for (OWLAxiom axiom : abox2) {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom ca = (OWLClassAssertionAxiom) axiom;
                OWLClassExpression classExpression = ca.getClassExpression();
                OWLNamedIndividual ind2 = individualGenerator
                        .getPairForIndividual((OWLNamedIndividual) ca.getIndividual())
                        .getValue();
                OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(classExpression, ind2);
                // Check if the original Abox contains the axiom and add the element to newAbox if true
                if (originalABox.contains(classAssertion)) {
                    abox3.add(ca);
                }
            } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom) axiom;
                OWLObjectPropertyExpression objectProperty = pa.getProperty();
                OWLNamedIndividual subject = individualGenerator.getPairForIndividual((OWLNamedIndividual) pa.getSubject()).getValue();
                OWLNamedIndividual object = individualGenerator.getPairForIndividual((OWLNamedIndividual) pa.getObject()).getValue();

                OWLObjectPropertyAssertionAxiom propertyAssertion = factory.getOWLObjectPropertyAssertionAxiom(objectProperty, subject, object);

                if (originalABox.contains(propertyAssertion)) {
                    abox3.add(pa);
                }

            }
        }
        return abox3;
    }

    private Set<OWLAxiom> transform2ABox(Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> abox, OWLDataFactory dataFactory) {
        Set<OWLAxiom> axioms = new HashSet<>();
        for (Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>> element : abox) {
            Object key = element.getKey();
            Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>> value = element.getValue();
            if (key instanceof OWLClassExpression) {
                assert value.getValue().size() == 1 : "The size of the value pair is not 1: " + value;
                assert value.getKey().size() == 1 : "The size of the key pair is not 1: " + value;
                List<OWLNamedIndividual> subject = value.getKey();
                List<OWLNamedIndividual> object = value.getValue();
                // Handle class assertions
                OWLClassExpression classExpression = (OWLClassExpression) key;
                OWLClassAssertionAxiom classAssertion1 = dataFactory.getOWLClassAssertionAxiom(classExpression, subject.get(0));
                OWLClassAssertionAxiom classAssertion2 = dataFactory.getOWLClassAssertionAxiom(classExpression, object.get(0));
                axioms.add(classAssertion1);
                axioms.add(classAssertion2);

            } else if (key instanceof OWLObjectProperty) {
                List<OWLNamedIndividual> subjects = value.getKey();
                List<OWLNamedIndividual> objects = value.getValue();
                // Handle object property assertions
                OWLObjectProperty property = (OWLObjectProperty) key;
                OWLObjectPropertyAssertionAxiom propertyAssertion1 =
                        dataFactory.getOWLObjectPropertyAssertionAxiom(property, subjects.get(0), subjects.get(1));
                axioms.add(propertyAssertion1);
                OWLObjectPropertyAssertionAxiom propertyAssertion2 =
                        dataFactory.getOWLObjectPropertyAssertionAxiom(property, objects.get(0), objects.get(1));
                axioms.add(propertyAssertion2);
            } else {
                throw new IllegalArgumentException("Unsupported key type: " + key.getClass().getName());
            }
        }
        return axioms;
    }

}
