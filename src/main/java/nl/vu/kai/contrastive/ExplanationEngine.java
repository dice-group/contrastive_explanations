package nl.vu.kai.contrastive;

import com.clarkparsia.owlapi.explanation.MyBlackBoxExplanation;
import nl.vu.kai.contrastive.helper.ExplanationHelper;
import nl.vu.kai.contrastive.helper.IndividualGenerator;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.MyExplanation;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import tools.Pair;

import java.util.*;


public class ExplanationEngine {

    private final IndividualGenerator individualGenerator;

    private final ABoxProcessor aboxProcessor;

    private final OWLDataFactory factory;

    public ExplanationEngine(OWLDataFactory factory) {
        this.factory=factory;
        individualGenerator =new IndividualGenerator(factory);
        aboxProcessor=new ABoxProcessor(individualGenerator, factory);
    }

    public ContrastiveExplanation computeExplanation(ContrastiveExplanationProblem problem) throws OWLOntologyCreationException {
        // Step 1: Use ExplanationHelper to get relevant axioms and individuals
        Set<OWLAxiom> relevantAxioms = ExplanationHelper.getRelevantAxioms(problem);
        Set<OWLNamedIndividual> relevantIndividuals = ExplanationHelper.getRelevantIndividuals(problem);

        // Step 2: Compute ABox2 and ABox3 (stubbed for now)
        Set<OWLAxiom> abox2 = aboxProcessor.generateAbox2(relevantAxioms,relevantIndividuals);
        Set<OWLAxiom> abox3 = aboxProcessor.generateABox3(relevantAxioms,abox2);

        /*System.out.println("ABox 3:");
        abox3.forEach(System.out::println);
        System.out.println();*/

        OWLNamedIndividual combinedIndividual = individualGenerator.getIndividualForPair(problem.getFact(),problem.getFoil());

        // Step 3: Create "specialAxiom"
        OWLAxiom specialAxiom = factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), combinedIndividual);

        // Step 4: Construct overApproximationOntology
        OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
        OWLOntology overApproximationOntology = manager.createOntology();
        manager.addAxioms(overApproximationOntology, problem.getOntology().getAxioms());
        manager.addAxioms(overApproximationOntology, abox2);

        /*System.out.println("Overapproximation: ");
        overApproximationOntology.axioms().forEach(System.out::println);
        System.out.println();*/

        Set<OWLAxiom> flexibleSet = new HashSet<>();
        flexibleSet.addAll(abox2);
        flexibleSet.removeAll(abox3);

        // Step 5: Compute first explanation
        Set<OWLAxiom> different = computeExplanation(overApproximationOntology, specialAxiom, flexibleSet);

        // Step 6: Update overApproximationOntology
        manager.removeAxioms(overApproximationOntology, flexibleSet);
        manager.addAxioms(overApproximationOntology, abox3);
        manager.addAxioms(overApproximationOntology, different);

        flexibleSet = abox3;

        // Step 7: Compute second explanation
        Set<OWLAxiom> common = computeExplanation(overApproximationOntology, specialAxiom, flexibleSet);

        ContrastiveExplanation result = extractMappings(common,different);

        // Step 8: Return ContrastiveExplanation
        return result;
    }

    private ContrastiveExplanation extractMappings(Set<OWLAxiom> common, Set<OWLAxiom> different) {
        Map<OWLNamedIndividual,OWLNamedIndividual> factMap = new HashMap<>();
        Map<OWLNamedIndividual,OWLNamedIndividual> foilMap = new HashMap<>();
        for(OWLAxiom axiom:common) {
            for (OWLNamedIndividual ind : axiom.getIndividualsInSignature()) {
                factMap.put(ind, individualGenerator.getPairForIndividual(ind).getKey());
                foilMap.put(ind, individualGenerator.getPairForIndividual(ind).getValue());
            }
        }

        for(OWLAxiom axiom:different) {
            for (OWLNamedIndividual ind : axiom.getIndividualsInSignature()) {
                factMap.put(ind, individualGenerator.getPairForIndividual(ind).getKey());
                foilMap.put(ind, individualGenerator.getPairForIndividual(ind).getValue());
            }
        }
        return new ContrastiveExplanation(common,different,factMap,foilMap,Collections.emptySet());
    }

    private Set<OWLAxiom> computeExplanation(OWLOntology ontology, OWLAxiom axiom,
                                             Set<OWLAxiom> flexibleSet) {
        Set<OWLAxiom> fixedSet = new HashSet<>(ontology.getAxioms());
        fixedSet.removeAll(flexibleSet);
        //MyExplanation expl = new MyExplanation(ontology,fixedSet);
        //Set<OWLAxiom> result = expl.getEntailmentExplanation(axiom);

        ReasonerFactory reasonerFactory = new ReasonerFactory();

        MyBlackBoxExplanation expl = new MyBlackBoxExplanation(ontology, reasonerFactory, reasonerFactory.createReasoner(ontology));
        expl.setStaticPart(fixedSet);
        Set<OWLAxiom> result = expl.getExplanation(asUnsat(axiom));


        result.removeAll(fixedSet);
        return result;
    }

    private OWLClassExpression asUnsat(OWLAxiom axiom) {
        if(axiom instanceof OWLClassAssertionAxiom) {
            OWLClassAssertionAxiom ca = (OWLClassAssertionAxiom)axiom;
            return factory.getOWLObjectIntersectionOf(factory.getOWLObjectOneOf(ca.getIndividual()), factory.getOWLObjectComplementOf(ca.getClassExpression()));
        } else
            throw new AssertionError("Not implemented!");
    }
}

