package abduction;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;

import java.util.HashSet;
import java.util.Set;

public class AbductionReasoning {
    public static Set<Set<OWLAxiom>> findCommonAxioms(Set<Set<OWLAxiom>> explanationsSet, OWLOntology ontology) {
        Set<OWLAxiom> aBoxAxioms = ontology.getABoxAxioms(Imports.INCLUDED);
        Set<Set<OWLAxiom>> commonAxiomsSet = new HashSet<>();


        for (Set<OWLAxiom> explanationAxioms : explanationsSet) {
            Set<OWLAxiom> commonAxioms = new HashSet<>();

            for (OWLAxiom explanationAxiom : explanationAxioms) {
                {
                    if (aBoxAxioms.contains(explanationAxiom)) {
                        commonAxioms.add(explanationAxiom);
                    }
                }
            }
            commonAxiomsSet.add(commonAxioms);
        }

        return commonAxiomsSet;
    }

    public void applyAbductionForIndividuals(OWLOntology ontology, Set<Set<OWLAxiom>> explanations, OWLNamedIndividual individualWithExplan, OWLNamedIndividual individualWithoutExplan, Set<OWLAxiom> commonAxioms) {

        OWLOntology ontology1 = ontology;

        // Modify axioms for the first individual
        Set<OWLAxiom> axiomsForFirstIndividual = new HashSet<>();
        replaceIndividualInAxiom(commonAxioms, individualWithExplan, individualWithoutExplan, axiomsForFirstIndividual);

        // Check for missing axioms
        Set<OWLAxiom> missingAxioms = new HashSet<>();
        Set<OWLAxiom> aBoxAxioms = ontology.getABoxAxioms(Imports.INCLUDED);

        for (OWLAxiom axiom : axiomsForFirstIndividual) {
            if (!aBoxAxioms.contains(axiom)) {
                missingAxioms.add(axiom);
            }
        }

        // Add missing axioms to the ontology
        OWLOntologyManager manager = ontology1.getOWLOntologyManager();

        System.out.println("Missing axioms for " + individualWithoutExplan + ": " + missingAxioms);
    }

    private void replaceIndividualInAxiom(Set<OWLAxiom> commonAxioms, OWLNamedIndividual original, OWLNamedIndividual replacement, Set<OWLAxiom> modifiedAxioms) {
        // Get the OWLDataFactory from the ontology manager
        OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = owlmanager.getOWLDataFactory();
        OWLNamedIndividual fixedVar = dataFactory.getOWLNamedIndividual(IRI.create("http://example.org/xyz"));

        // Iterate over the axioms in commonAxioms
        for (OWLAxiom axiom : commonAxioms) {
            // Visit the axiom and replace the individual where needed
            axiom.accept(new OWLObjectVisitorAdapter() {

                @Override
                public void visit(OWLClassAssertionAxiom axiom) {
                    if (axiom.getIndividual().equals(original)) {
                        OWLAxiom newAxiom = dataFactory.getOWLClassAssertionAxiom(axiom.getClassExpression(), replacement);
                        modifiedAxioms.add(newAxiom);
                    }
                }

                // Handle OWL Object Property Assertion Axiom
                @Override
                public void visit(OWLObjectPropertyAssertionAxiom axiom) {
                    if (axiom.getSubject().equals(original)) {
                        if (axiom.getObject().isNamed()) {
                            OWLNamedIndividual objectInd = axiom.getObject().asOWLNamedIndividual();

                            // Replace subject with replacement and object with fixedVar
                            OWLAxiom newAxiom = dataFactory.getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), replacement, fixedVar);
                            modifiedAxioms.add(newAxiom);

                            // Now, look for the class assertion involving the object (ML in this case)
                            for (OWLAxiom classAxiom : commonAxioms) {
                                if (classAxiom instanceof OWLClassAssertionAxiom) {
                                    OWLClassAssertionAxiom classAssertionAxiom = (OWLClassAssertionAxiom) classAxiom;
                                    if (classAssertionAxiom.getIndividual().equals(objectInd)) {
                                        // If the object individual is of type "AI"
                                        // OWLClassAssertionAxiom newClassAssertionAxiom = dataFactory.getOWLClassAssertionAxiom(dataFactory.getOWLClass(IRI.create("http://www.semanticweb.org/CEX-Paper#AI")), fixedVar);
                                        OWLAxiom newAxi = dataFactory.getOWLClassAssertionAxiom(((OWLClassAssertionAxiom) classAxiom).getClassExpression(), fixedVar);
                                        modifiedAxioms.add(newAxi);
                                    }
                                }
                            }
                        }
                    }
                }


            });
        }
    }
}
