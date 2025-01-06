import javafx.util.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class AboxProcessor1 {

    // Process the ABox and generate new ABox entries based on the axiom type
    public Set<String> generateNewAbox(Set<OWLAxiom> abox, Set<OWLNamedIndividual> individuals) {
        Set<String> newAbox = new HashSet<>();

        for (OWLAxiom axiom : abox) {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
                OWLClassExpression classExpression = classAssertion.getClassExpression();
                OWLNamedIndividual individual = classAssertion.getIndividual().asOWLNamedIndividual();

                // Check if the class expression is a named class
                if (!classExpression.isAnonymous()) {
                    OWLClass owlClass = classExpression.asOWLClass();

                   /* for (OWLNamedIndividual ind : individuals) {
                        newAbox.add(owlClass.getIRI().getFragment() + "(" + individual.getIRI().getFragment() + "," + ind.getIRI().getFragment() + ")");
                    }*/
                    for (OWLNamedIndividual ind : individuals) {
                        newAbox.add(owlClass + "(" + individual+ "," + ind + ")");
                    }
                } else {
                    // Handle anonymous class expressions if needed
                    // For now, just print a message or skip
                    System.out.println("Skipping anonymous class expression: " + classExpression);
                }
            } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom propertyAssertion = (OWLObjectPropertyAssertionAxiom) axiom;
                OWLObjectProperty property = propertyAssertion.getProperty().asOWLObjectProperty();
                OWLNamedIndividual subject = propertyAssertion.getSubject().asOWLNamedIndividual();
                OWLNamedIndividual object = propertyAssertion.getObject().asOWLNamedIndividual();
                for (OWLNamedIndividual ind1 : individuals) {
                    for (OWLNamedIndividual ind2 : individuals) /*{
                        newAbox.add(property.getIRI().getFragment() + "(["
                                + subject.getIRI().getFragment() + "," + ind1.getIRI().getFragment() + "], ["
                                + object.getIRI().getFragment() + "," + ind2.getIRI().getFragment() + "])");
                    }*/
                    {
                        newAbox.add(property+ "(["
                                + subject + "," + ind1+ "], ["
                                + object+ "," + ind2 + "])");
                    }
                }
            }
        }
        return newAbox;
    }

    public Set<String> generateNewAbox1(Set<OWLAxiom> abox, Set<OWLNamedIndividual> individuals) {
        Set<String> newAbox = new HashSet<>();

        for (OWLAxiom axiom : abox) {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
                OWLClassExpression classExpression = classAssertion.getClassExpression();
                OWLNamedIndividual individual = classAssertion.getIndividual().asOWLNamedIndividual();

                // Check if the class expression is a named class
                if (!classExpression.isAnonymous()) {
                    OWLClass owlClass = classExpression.asOWLClass();

                   /* for (OWLNamedIndividual ind : individuals) {
                        newAbox.add(owlClass.getIRI().getFragment() + "(" + individual.getIRI().getFragment() + "," + ind.getIRI().getFragment() + ")");
                    }*/
                    for (OWLNamedIndividual ind : individuals) {
                        newAbox.add(owlClass + "(" + individual+ "," + ind + ")");
                    }
                } else {
                    // Handle anonymous class expressions if needed
                    // For now, just print a message or skip
                    System.out.println("Skipping anonymous class expression: " + classExpression);
                }
            } else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
                OWLObjectPropertyAssertionAxiom propertyAssertion = (OWLObjectPropertyAssertionAxiom) axiom;
                OWLObjectProperty property = propertyAssertion.getProperty().asOWLObjectProperty();
                OWLNamedIndividual subject = propertyAssertion.getSubject().asOWLNamedIndividual();
                OWLNamedIndividual object = propertyAssertion.getObject().asOWLNamedIndividual();
                for (OWLNamedIndividual ind1 : individuals) {
                    for (OWLNamedIndividual ind2 : individuals) /*{
                        newAbox.add(property.getIRI().getFragment() + "(["
                                + subject.getIRI().getFragment() + "," + ind1.getIRI().getFragment() + "], ["
                                + object.getIRI().getFragment() + "," + ind2.getIRI().getFragment() + "])");
                    }*/
                    {
                        newAbox.add(property+ "(["
                                + subject + "," + ind1+ "], ["
                                + object+ "," + ind2 + "])");
                    }
                }
            }
        }
        return newAbox;
    }

    public Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> generateNewAbox2(Set<OWLAxiom> abox, Set<OWLNamedIndividual> individuals) {
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

    // New method to process nnnbox3 and create a new ABox
    public Set<OWLAxiom> createNewAboxFromNnnbox3_2(
            Set<OWLAxiom> originalAbox,
            Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> nnnbox3,
            OWLDataFactory dataFactory) {
            Set<OWLAxiom> newAbox = new HashSet<>();
            for (Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>> element : nnnbox3) {
            Object firstElement = element.getKey();
            Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>> pair = element.getValue();
            if (firstElement instanceof OWLClassExpression) {
                OWLClassExpression classExpression = (OWLClassExpression) firstElement;
                if (pair.getValue().size() == 1) {
                    OWLNamedIndividual individual = pair.getValue().get(0);
                    OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(classExpression, individual);
                    if (originalAbox.contains(classAssertion)) {
                        newAbox.add(classAssertion);
                    }
                }
            } else if (firstElement instanceof OWLObjectProperty) {
                OWLObjectProperty objectProperty = (OWLObjectProperty) firstElement;

                if (pair.getValue().size() == 2) {
                    //OWLNamedIndividual subject = pair.getValue().get(0);
                    OWLNamedIndividual subject = pair.getKey().get(1);
                    //OWLNamedIndividual object = pair.getValue().get(1);
                    OWLNamedIndividual object = pair.getValue().get(1);
                    OWLObjectPropertyAssertionAxiom propertyAssertion = dataFactory.getOWLObjectPropertyAssertionAxiom(objectProperty, subject, object);

                    if (originalAbox.contains(propertyAssertion)) {
                        newAbox.add(propertyAssertion);
                    }
                }
            }
        }
        return newAbox;
    }
    public Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> createNewAboxFromNnnbox3(
            Set<OWLAxiom> originalAbox,
            Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> nnnbox3,
            OWLDataFactory dataFactory) {
        Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> newAbox = new HashSet<>();
        for (Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>> element : nnnbox3) {
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
    // Process the ABox and generate new ABox entries based on the axiom type
    /*public Map<OWLAxiom, Set<OWLAxiom>> generateNewAbox2(Set<OWLAxiom> abox, Set<OWLNamedIndividual> individuals, OWLDataFactory dataFactory) {
        Map<OWLAxiom, Set<OWLAxiom>> newAboxMap = new HashMap<>();

        for (OWLAxiom axiom : abox) {
            Set<OWLAxiom> newAxioms = new HashSet<>();

            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
                OWLClassExpression classExpression = classAssertion.getClassExpression();
                OWLNamedIndividual individual = classAssertion.getIndividual().asOWLNamedIndividual();

                if (!classExpression.isAnonymous()) {
                    OWLClass owlClass = classExpression.asOWLClass();
                    for (OWLNamedIndividual ind : individuals) {
                        OWLClassAssertionAxiom newAxiom = dataFactory.getOWLClassAssertionAxiom(owlClass, ind);
                        newAxioms.add(newAxiom);
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
                        OWLObjectPropertyAssertionAxiom newAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(
                                property,
                                dataFactory.getOWLObjectOneOf(subject, ind1),
                                dataFactory.getOWLObjectOneOf(object, ind2)
                        );
                        newAxioms.add(newAxiom);
                    }
                }
            }

            newAboxMap.put(axiom, newAxioms);
        }

        return newAboxMap;
    }*/
}
