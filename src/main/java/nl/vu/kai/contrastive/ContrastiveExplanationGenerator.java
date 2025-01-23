package nl.vu.kai.contrastive;

import com.clarkparsia.owlapi.explanation.MyBlackBoxExplanation;
import nl.vu.kai.contrastive.conflicts.ConflictHandler;
import nl.vu.kai.contrastive.experiments.ExperimenterWithClasses;
import nl.vu.kai.contrastive.helper.RelevantScopeFinder;
import nl.vu.kai.contrastive.helper.IndividualGenerator;
//import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.HermiT.ReasonerFactory;
//import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.*;


public class ContrastiveExplanationGenerator {

    private final IndividualGenerator individualGenerator;

    private final ABoxProcessor aboxProcessor;

    private final OWLDataFactory factory;

    public ContrastiveExplanationGenerator(OWLDataFactory factory) {
        this.factory=factory;
        individualGenerator =new IndividualGenerator(factory);
        aboxProcessor=new ABoxProcessor(individualGenerator, factory);
    }

    public ContrastiveExplanation computeExplanation(ContrastiveExplanationProblem problem) throws OWLOntologyCreationException {

        // Step 0: make TBox "conflict-save"
        ConflictHandler conflictHandler = new ConflictHandler(problem.getOntology(), factory);
        conflictHandler.makeTBoxConflictSave();

        // Step 1: Use ExplanationHelper to get relevant axioms and individuals
        Set<OWLAxiom> relevantAxioms = RelevantScopeFinder.getRelevantAxioms(problem);
        Set<OWLNamedIndividual> relevantIndividuals = RelevantScopeFinder.getRelevantIndividuals(problem, relevantAxioms, factory);
        Set<OWLAxiom> module = RelevantScopeFinder.getModule(problem, relevantIndividuals);

        // Step 2: Compute ABox2 and ABox3 (stubbed for now)
        Set<OWLAxiom> abox2 = aboxProcessor.generateAbox2(relevantAxioms,relevantIndividuals);
        Set<OWLAxiom> abox3 = aboxProcessor.generateABox3(module,abox2);

        System.out.println("Size ABox2: "+abox2.size());
        System.out.println("Size ABox3: "+abox3.size());

        /*System.out.println("ABox 3:");
        abox3.forEach(System.out::println);
        System.out.println();*/
        System.out.println("Generated ABoxes");

        OWLNamedIndividual combinedIndividual = individualGenerator.getIndividualForPair(problem.getFact(),problem.getFoil());

        // Step 3: Create "specialAxiom"
        OWLAxiom specialAxiom = factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), combinedIndividual);

        // Step 4: Construct overApproximationOntology
        OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
        OWLOntology overApproximationOntology = manager.createOntology();
        manager.addAxioms(overApproximationOntology, module);//problem.getOntology().getAxioms());
        manager.addAxioms(overApproximationOntology, abox2);

        /*System.out.println("Overapproximation: ");
        overApproximationOntology.axioms().forEach(System.out::println);
        System.out.println();*/

        Set<OWLAxiom> flexibleSet = new HashSet<>();
        flexibleSet.addAll(abox2);
        flexibleSet.removeAll(abox3);

        System.out.println("OverApproximated ontology size: "+overApproximationOntology.getAxiomCount());
        System.out.println("Flexible: "+flexibleSet.size());

        // Step 5: Compute first explanation
        Set<OWLAxiom> different = computeExplanation(overApproximationOntology, specialAxiom, flexibleSet);

        System.out.println("Computed first justification");

        // Step 6: Update overApproximationOntology
        manager.removeAxioms(overApproximationOntology, flexibleSet);
        manager.addAxioms(overApproximationOntology, abox3);
        manager.addAxioms(overApproximationOntology, different);

        flexibleSet = abox3;


        System.out.println("OverApproximated ontology size: "+overApproximationOntology.getAxiomCount());
        System.out.println("Flexible: "+flexibleSet.size());

        // Step 7: Compute second explanation
        Set<OWLAxiom> common = computeExplanation(overApproximationOntology, specialAxiom, flexibleSet);

        System.out.println("Computed second justification");

        // Step 8: extract mappings and conflict

        ContrastiveExplanation result = extractMappings(common,different);
        result = conflictHandler.addConflict(result);
        conflictHandler.restoreOntology();

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

        //ElkReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasonerFactory reasonerFactory = ExperimenterWithClasses.reasoner== ExperimenterWithClasses.ReasonerChoice.ELK ?
                new ElkReasonerFactory() :
                new ReasonerFactory();

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

