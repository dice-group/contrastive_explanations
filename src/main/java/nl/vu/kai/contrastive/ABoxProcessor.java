package nl.vu.kai.contrastive;

import nl.vu.kai.contrastive.helper.IndividualGenerator;
import org.semanticweb.owlapi.model.*;
import tools.Pair;
import java.util.*;

public class ABoxProcessor {

    private final IndividualGenerator individualGenerator;
    private final OWLDataFactory factory;

    public ABoxProcessor(IndividualGenerator individualGenerator, OWLDataFactory factory){
        this.individualGenerator=individualGenerator;
        this.factory=factory;
    }

    /*
        ABox 1: the original ABox
        ABox 2: for every axiom A(a)/r(a,b) in ABox 1, and all individuals x1,x2, we add A([a,x1])/r([a,x1],[b,x2])
        ABox 3: contains all axioms A([a,x])/r([a,x1],[b,x2]) from ABox 2 for which A(x)/r(x1,x2) is in original abox
     */
    // Process the ABox and generate new ABox entries based on the axiom type
    //private Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>>
    public Set<OWLAxiom> generateAbox2(
            Set<OWLAxiom> abox, Set<OWLNamedIndividual> individuals) {
        // Set to store either OWLClass or OWLObjectProperty with two lists of OWLNamedIndividual
        //Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> newAbox = new HashSet<>();

        Set<OWLAxiom> abox2 = new HashSet<>();

        for (OWLAxiom axiom : abox) {
            if (axiom instanceof OWLClassAssertionAxiom) {
                OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
                OWLClassExpression classExpression = classAssertion.getClassExpression();
                OWLNamedIndividual individual = classAssertion.getIndividual().asOWLNamedIndividual();

                // Check if the class expression is a named class
                for (OWLNamedIndividual ind : individuals) {
                    abox2.add(factory.getOWLClassAssertionAxiom(classExpression,individualGenerator.getIndividualForPair(individual,ind)));
                    //ewAbox.add(new Pair<>(classExpression, new Pair<>(Arrays.asList(individual, ind), Collections.singletonList(ind))));
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
                                individualGenerator.getIndividualForPair(subject,ind1),
                                individualGenerator.getIndividualForPair(object,ind2)
                                ));
                        //newAbox.add(new Pair<>(property, new Pair<>(Arrays.asList(subject, ind1), Arrays.asList(object, ind2))));
                    }
                }
            }
        }
        return abox2;
    }

    /*public Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> generateNewAbox3(
            Set<OWLAxiom> originalAbox,
            Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> abox2,
            OWLDataFactory dataFactory) {
     */
    public Set<OWLAxiom> generateABox3(Set<OWLAxiom> originalABox, Set<OWLAxiom> abox2) {
        //Set<Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>>> newAbox = new HashSet<>();
        Set<OWLAxiom> abox3 = new HashSet<>();
        for (//Pair<Object, Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>>> element : abox2
            OWLAxiom axiom: abox2) {
            //Object firstElement = element.getKey();
            //Pair<List<OWLNamedIndividual>, List<OWLNamedIndividual>> internalpair = element.getValue();
            if (axiom instanceof OWLClassAssertionAxiom) {
                    //firstElement instanceof OWLClassExpression) {
                OWLClassAssertionAxiom ca = (OWLClassAssertionAxiom)axiom;
                OWLClassExpression classExpression = ca.getClassExpression();//(OWLClassExpression) firstElement;
                // Ensure that the size of the value is exactly 1
                //assert internalpair.getValue().size() == 1 : "The size of the value pair is not 1: " + internalpair;
                //OWLNamedIndividual individual = internalpair.getValue().get(0);
                OWLNamedIndividual ind2 = individualGenerator
                        .getPairForIndividual((OWLNamedIndividual) ca.getIndividual())
                        .getValue();
                OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(classExpression, ind2);
                //System.out.println("Candidate for ABox3: "+classAssertion);
                // Check if the original Abox contains the axiom and add the element to newAbox if true
                if (originalABox.contains(classAssertion)) {
                    //System.out.println(" -- adding that fucker");
                    abox3.add(ca);
                }
            } else if (axiom instanceof OWLObjectPropertyAssertionAxiom){//firstElement instanceof OWLObjectProperty) {
                OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom) axiom;
                OWLObjectPropertyExpression objectProperty = pa.getProperty();//(OWLObjectProperty) firstElement;

                //if (internalpair.getValue().size() == 2) {
                   // OWLNamedIndividual subject = internalpair.getKey().get(1);
                   // OWLNamedIndividual object = internalpair.getValue().get(1);

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

    //Return  Set<OWLAxiom> as A(a)/r(a,x1),r(b,x2) for all existing axioms A([a,x])/r([a,x1],[b,x2])
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
